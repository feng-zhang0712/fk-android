package com.fk.core.file

import kotlinx.serialization.Serializable
import java.io.File

/** Transfer direction. */
@Serializable
enum class TransferKind {
  Download,
  Upload,
}

/** Transfer execution state. */
@Serializable
enum class TransferState {
  Queued,
  Running,
  Paused,
  Completed,
  Cancelled,
  Failed,
}

/** Progress snapshot delivered to callers. */
data class TransferProgress(
  val taskId: Int,
  val progress: Double,
  val completedBytes: Long,
  val totalBytes: Long,
)

/**
 * Download request.
 *
 * @property sourceUrl Remote HTTP(S) URL.
 * @property destinationDirectory Directory that will receive the file (created if needed).
 * @property fileName Optional override; defaults to the URL last path segment or `download.bin`.
 * @property headers Extra request headers (e.g. Authorization).
 */
data class DownloadRequest(
  val sourceUrl: String,
  val destinationDirectory: File,
  val fileName: String? = null,
  val headers: Map<String, String> = emptyMap(),
)

/** Successful download result. */
data class DownloadResult(
  val taskId: Int,
  val file: File,
  val sourceUrl: String,
)

/**
 * Multipart upload request.
 *
 * @property url Destination endpoint.
 * @property files File parts.
 * @property formFields Optional text fields.
 * @property headers Extra request headers (e.g. Authorization).
 * @property method HTTP method; defaults to POST.
 */
data class UploadRequest(
  val url: String,
  val files: List<UploadFile>,
  val formFields: Map<String, String> = emptyMap(),
  val headers: Map<String, String> = emptyMap(),
  val method: String = "POST",
)

/** One multipart file part. */
data class UploadFile(
  val fieldName: String,
  val file: File,
  val fileName: String = file.name,
  val mimeType: String = mimeTypeFor(file.extension),
)

/** Successful upload result. */
data class UploadResult(
  val taskId: Int,
  val responseBody: ByteArray,
  val statusCode: Int,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UploadResult) return false
    return taskId == other.taskId &&
      statusCode == other.statusCode &&
      responseBody.contentEquals(other.responseBody)
  }

  override fun hashCode(): Int {
    var result = taskId
    result = 31 * result + responseBody.contentHashCode()
    result = 31 * result + statusCode
    return result
  }
}

/** Persisted transfer snapshot for cold-start inspection / resume of downloads. */
@Serializable
data class PersistedTransfer(
  val id: Int,
  val kind: TransferKind,
  val state: TransferState,
  val sourceUrl: String,
  val destinationPath: String? = null,
  val partialPath: String? = null,
  val completedBytes: Long = 0L,
  val totalBytes: Long = -1L,
  val updatedAtEpochMs: Long,
)

/** Live task snapshot for observation. */
data class TransferSnapshot(
  val id: Int,
  val kind: TransferKind,
  val state: TransferState,
  val sourceUrl: String,
  val destinationPath: String? = null,
  val completedBytes: Long = 0L,
  val totalBytes: Long = -1L,
  val errorMessage: String? = null,
)

/**
 * Runtime knobs for [FileTransferManager].
 *
 * @property maxConcurrentTransfers Cap on simultaneously running transfers.
 * @property workingDirectoryName Folder under cache used for `.partial` download files.
 * @property persistenceKey SharedPreferences key for transfer snapshots.
 * @property connectTimeoutMs OkHttp connect timeout.
 * @property readTimeoutMs OkHttp read timeout.
 * @property writeTimeoutMs OkHttp write timeout.
 */
data class FileTransferConfiguration(
  val maxConcurrentTransfers: Int = 2,
  val workingDirectoryName: String = "FkFileTransfers",
  val persistenceKey: String = "com.fk.core.file.transfers",
  val connectTimeoutMs: Long = 30_000L,
  val readTimeoutMs: Long = 60_000L,
  val writeTimeoutMs: Long = 60_000L,
)

/**
 * Stable error taxonomy for file transfer operations.
 *
 * Conceptually aligned with iOS `FKFileManagerError` transfer cases.
 */
sealed class FileTransferError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  class InvalidUrl(value: String) :
    FileTransferError("Invalid transfer URL: $value")

  class TransferFailed(detail: String, cause: Throwable? = null) :
    FileTransferError("Transfer failed: $detail", cause)

  class InvalidResponse(detail: String = "invalid response") :
    FileTransferError("Invalid transfer response: $detail")

  class FileNotFound(path: String) :
    FileTransferError("File not found: $path")

  class TaskNotFound(taskId: Int) :
    FileTransferError("Transfer task not found: $taskId")

  class Unknown(detail: String, cause: Throwable? = null) :
    FileTransferError(detail, cause)
}

internal fun mimeTypeFor(extension: String): String =
  when (extension.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "pdf" -> "application/pdf"
    "txt" -> "text/plain"
    "json" -> "application/json"
    "zip" -> "application/zip"
    "mp4" -> "video/mp4"
    "mp3" -> "audio/mpeg"
    else -> "application/octet-stream"
  }
