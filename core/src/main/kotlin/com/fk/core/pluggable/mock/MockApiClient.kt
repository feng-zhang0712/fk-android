package com.fk.core.pluggable.mock

import com.fk.core.pluggable.networking.ApiClient
import com.fk.core.pluggable.networking.ApiRequest
import com.fk.core.pluggable.networking.ApiResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Canned [ApiClient] for samples and tests.
 */
class MockApiClient : ApiClient {
  private val responses = ConcurrentHashMap<String, Result<ApiResponse>>()
  @Volatile
  private var defaultResponse: Result<ApiResponse>? = null

  /** Registers a canned response for an exact URL string key. */
  fun setResponse(url: String, result: Result<ApiResponse>) {
    responses[url] = result
  }

  /** Fallback when no URL-specific stub exists. */
  fun setDefaultResponse(result: Result<ApiResponse>) {
    defaultResponse = result
  }

  override suspend fun perform(request: ApiRequest): ApiResponse {
    val result = responses[request.url] ?: defaultResponse
    return when {
      result == null -> ApiResponse(data = ByteArray(0), statusCode = 204)
      result.isSuccess -> result.getOrThrow()
      else -> throw result.exceptionOrNull()!!
    }
  }
}
