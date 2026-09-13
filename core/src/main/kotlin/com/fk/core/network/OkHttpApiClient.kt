package com.fk.core.network

import com.fk.core.pluggable.networking.ApiClient
import com.fk.core.pluggable.networking.ApiRequest
import com.fk.core.pluggable.networking.ApiResponse
import com.fk.core.pluggable.networking.HttpMethod
import com.fk.core.pluggable.networking.Reachability
import com.fk.core.pluggable.networking.RequestInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * OkHttp implementation of Pluggable [ApiClient].
 *
 * @param client Shared OkHttp client (timeouts, interceptors, connection pool).
 * @param config Base URL and default headers.
 * @param requestInterceptors Pluggable interceptors applied before building the OkHttp request.
 * @param reachability Optional preflight connectivity check.
 */
class OkHttpApiClient(
  private val client: OkHttpClient,
  private val config: NetworkConfig = NetworkConfig(),
  private val requestInterceptors: List<RequestInterceptor> = emptyList(),
  private val reachability: Reachability? = null,
) : ApiClient {
  /**
   * Convenience constructor that builds the OkHttp client from [config].
   */
  constructor(
    config: NetworkConfig,
    requestInterceptors: List<RequestInterceptor> = emptyList(),
    reachability: Reachability? = null,
    additionalOkHttpInterceptors: List<okhttp3.Interceptor> = emptyList(),
  ) : this(
    client = config.buildClient(additionalOkHttpInterceptors),
    config = config,
    requestInterceptors = requestInterceptors,
    reachability = reachability,
  )

  override suspend fun perform(request: ApiRequest): ApiResponse {
    if (reachability?.isReachable == false) {
      throw NetworkException.Offline()
    }

    var pending = request
    for (interceptor in requestInterceptors) {
      pending = interceptor.intercept(pending)
    }

    val httpUrl = resolveUrl(pending.url)
      ?: throw NetworkException.InvalidUrl(pending.url)

    val builder = Request.Builder().url(httpUrl)
    config.defaultHeaders.forEach { (key, value) -> builder.header(key, value) }
    pending.headers.forEach { (key, value) -> builder.header(key, value) }

    val mediaType = headerValue(pending.headers, "Content-Type")?.toMediaTypeOrNull()
    val body = pending.body?.toRequestBody(
      mediaType ?: "application/octet-stream".toMediaTypeOrNull(),
    )

    when (pending.method) {
      HttpMethod.Get -> builder.get()
      HttpMethod.Head -> builder.head()
      HttpMethod.Post -> builder.post(body ?: ByteArray(0).toRequestBody(null))
      HttpMethod.Put -> builder.put(body ?: ByteArray(0).toRequestBody(null))
      HttpMethod.Patch -> builder.patch(body ?: ByteArray(0).toRequestBody(null))
      HttpMethod.Delete -> {
        if (body != null) builder.delete(body) else builder.delete()
      }
    }

    val okRequest = builder.build()
    // Per-request timeout clones the client (separate connection pool). Prefer
    // NetworkConfig timeouts for steady-state traffic; use ApiRequest.timeoutMs sparingly.
    val callClient = pending.timeoutMs?.let { timeoutMs ->
      client.newBuilder()
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()
    } ?: client

    return execute(callClient.newCall(okRequest))
  }

  private fun resolveUrl(raw: String): HttpUrl? {
    raw.toHttpUrlOrNull()?.let { return it }
    val base = config.baseUrl?.trimEnd('/') ?: return null
    val path = if (raw.startsWith("/")) raw else "/$raw"
    return "$base$path".toHttpUrlOrNull()
  }

  private suspend fun execute(call: Call): ApiResponse =
    suspendCancellableCoroutine { continuation ->
      continuation.invokeOnCancellation { call.cancel() }
      call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          if (continuation.isCancelled) return
          val mapped = when {
            call.isCanceled() -> NetworkException.Cancelled()
            else -> NetworkException.Transport(e)
          }
          continuation.resumeWithException(mapped)
        }

        override fun onResponse(call: Call, response: Response) {
          response.use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            val headers = resp.headers.toMap()
            if (continuation.isCancelled) return
            continuation.resume(
              ApiResponse(
                data = bytes,
                statusCode = resp.code,
                headers = headers,
              ),
            )
          }
        }
      })
    }

  private fun Headers.toMap(): Map<String, String> {
    val result = LinkedHashMap<String, String>(size)
    for (i in 0 until size) {
      result[name(i)] = value(i)
    }
    return result
  }

  private fun headerValue(headers: Map<String, String>, name: String): String? {
    val target = name.lowercase()
    return headers.entries.firstOrNull { it.key.lowercase() == target }?.value
  }
}
