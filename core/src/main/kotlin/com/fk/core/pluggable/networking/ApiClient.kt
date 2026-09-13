package com.fk.core.pluggable.networking

/**
 * Executes HTTP API requests using the host app's networking stack.
 *
 * Production: OkHttp / Ktor (see future `:core` `network` package).
 * Tests / samples: [com.fk.core.pluggable.mock.MockApiClient].
 */
fun interface ApiClient {
  /**
   * Performs a request and returns raw response data.
   *
   * @throws Exception Transport, TLS, or client configuration failures.
   */
  suspend fun perform(request: ApiRequest): ApiResponse
}

/** Mutates outbound [ApiRequest] values before dispatch (auth headers, tracing, …). */
fun interface RequestInterceptor {
  /** @return Request ready for transport. */
  fun intercept(request: ApiRequest): ApiRequest
}

/** Mutates inbound response data before decoding or business handling. */
fun interface ResponseInterceptor {
  /** @return Body bytes for downstream consumers. */
  fun intercept(data: ByteArray, statusCode: Int?, headers: Map<String, String>): ByteArray
}

/** Signs outbound requests to satisfy backend authentication policies. */
fun interface RequestSigner {
  fun sign(request: ApiRequest): ApiRequest
}

/**
 * Read/write credential storage used by auth interceptors and refresh flows.
 *
 * Production typically wraps EncryptedSharedPreferences or Keystore-backed storage.
 */
interface CredentialStore {
  var accessToken: String?
  var refreshToken: String?
}

/** Refreshes an expired access token when the backend returns HTTP 401. */
fun interface TokenRefresher {
  /**
   * @param refreshToken Current refresh token from [CredentialStore].
   * @return New access token string.
   */
  suspend fun refreshAccessToken(refreshToken: String?): String
}

/** Reports whether the device currently has usable network connectivity. */
interface Reachability {
  val isReachable: Boolean
}
