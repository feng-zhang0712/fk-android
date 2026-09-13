package com.fk.core.file

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/**
 * OkHttp helpers for Range downloads and multipart uploads.
 */
internal class FileTransferEngine(
  configuration: FileTransferConfiguration,
  client: OkHttpClient? = null,
) {
  private val http: OkHttpClient = client ?: OkHttpClient.Builder()
    .connectTimeout(configuration.connectTimeoutMs, TimeUnit.MILLISECONDS)
    .readTimeout(configuration.readTimeoutMs, TimeUnit.MILLISECONDS)
    .writeTimeout(configuration.writeTimeoutMs, TimeUnit.MILLISECONDS)
    .build()

  /**
   * Downloads [url] into [partialFile], appending from [startOffset].
   *
   * @return Absolute completed byte count written to the partial file.
   */
  fun downloadToPartial(
    url: String,
    partialFile: File,
    startOffset: Long,
    headers: Map<String, String>,
    callHolder: CallHolder,
    onProgress: (completed: Long, total: Long) -> Unit,
  ): Long {
    val requestBuilder = Request.Builder().url(url).get()
    if (startOffset > 0L) {
      requestBuilder.header("Range", "bytes=$startOffset-")
    }
    headers.forEach { (k, v) -> requestBuilder.header(k, v) }
    val call = http.newCall(requestBuilder.build())
    callHolder.call = call
    call.execute().use { response ->
      if (!response.isSuccessful && response.code != 206) {
        throw FileTransferError.TransferFailed("HTTP ${response.code}")
      }
      val body = response.body ?: throw FileTransferError.InvalidResponse("empty body")
      val contentLength = body.contentLength()
      val restartFromScratch = startOffset > 0L && response.code == 200
      val total = when {
        contentLength < 0L -> -1L
        startOffset > 0L && response.code == 206 -> startOffset + contentLength
        restartFromScratch -> contentLength
        else -> contentLength
      }
      val writeOffset = if (restartFromScratch) 0L else startOffset
      if (writeOffset == 0L && partialFile.exists()) {
        partialFile.delete()
      }

      RandomAccessFile(partialFile, "rw").use { raf ->
        raf.seek(writeOffset)
        val buffer = ByteArray(DEFAULT_BUFFER)
        var completed = writeOffset
        body.byteStream().use { input ->
          while (true) {
            if (callHolder.cancelled) throw InterruptedIO()
            val read = input.read(buffer)
            if (read < 0) break
            raf.write(buffer, 0, read)
            completed += read
            onProgress(completed, total)
          }
        }
        return completed
      }
    }
  }

  fun uploadMultipart(
    request: UploadRequest,
    callHolder: CallHolder,
    onProgress: (completed: Long, total: Long) -> Unit,
  ): UploadResult {
    for (part in request.files) {
      if (!part.file.exists()) {
        throw FileTransferError.FileNotFound(part.file.absolutePath)
      }
    }

    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
    request.formFields.forEach { (key, value) ->
      multipart.addFormDataPart(key, value)
    }
    request.files.forEach { part ->
      val media = part.mimeType.toMediaTypeOrNull()
      multipart.addFormDataPart(
        part.fieldName,
        part.fileName,
        part.file.asRequestBody(media),
      )
    }
    val countingBody = CountingRequestBody(multipart.build(), onProgress)
    val builder = Request.Builder()
      .url(request.url)
      .method(request.method.uppercase(), countingBody)
    request.headers.forEach { (k, v) -> builder.header(k, v) }

    val call = http.newCall(builder.build())
    callHolder.call = call
    call.execute().use { response ->
      val bytes = response.body?.bytes() ?: ByteArray(0)
      if (!response.isSuccessful) {
        throw FileTransferError.TransferFailed("HTTP ${response.code}")
      }
      return UploadResult(
        taskId = -1,
        responseBody = bytes,
        statusCode = response.code,
      )
    }
  }

  class CallHolder {
    @Volatile
    var call: okhttp3.Call? = null

    @Volatile
    var cancelled: Boolean = false

    fun cancel() {
      cancelled = true
      call?.cancel()
    }
  }

  private class InterruptedIO : IOException("transfer interrupted")

  private class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (completed: Long, total: Long) -> Unit,
  ) : RequestBody() {
    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
      val total = contentLength()
      var completed = 0L
      val forwarding = object : ForwardingSink(sink) {
        override fun write(source: Buffer, byteCount: Long) {
          super.write(source, byteCount)
          completed += byteCount
          onProgress(completed, total)
        }
      }.buffer()
      delegate.writeTo(forwarding)
      forwarding.flush()
    }
  }

  companion object {
    private const val DEFAULT_BUFFER: Int = 8 * 1024
  }
}
