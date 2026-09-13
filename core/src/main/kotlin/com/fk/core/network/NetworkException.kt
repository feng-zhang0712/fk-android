package com.fk.core.network

import java.io.IOException

/**
 * Transport-level failures thrown by [OkHttpApiClient].
 *
 * Non-2xx HTTP responses are returned as [com.fk.core.pluggable.networking.ApiResponse]
 * (callers inspect [com.fk.core.pluggable.networking.ApiResponse.statusCode]), not as
 * exceptions. Business / envelope errors belong in higher layers (mapping).
 */
sealed class NetworkException(
  message: String,
  cause: Throwable? = null,
) : IOException(message, cause) {
  /** The request URL could not be resolved (missing baseUrl for a relative path, etc.). */
  class InvalidUrl(url: String) : NetworkException("Invalid URL: $url")

  /** No network connectivity when a reachability check fails before dispatch. */
  class Offline : NetworkException("Device appears offline")

  /** The call was cancelled. */
  class Cancelled : NetworkException("Request cancelled")

  /** Socket / TLS / protocol failure without an HTTP status. */
  class Transport(cause: Throwable) : NetworkException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Transport failure",
    cause = cause,
  )
}
