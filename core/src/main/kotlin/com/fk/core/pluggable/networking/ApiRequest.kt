package com.fk.core.pluggable.networking

/**
 * Transport-neutral description of an HTTP API call.
 *
 * Higher-level clients map endpoint types into this value before invoking
 * [ApiClient]. Relative [url] strings are resolved by the client against its
 * configured base URL.
 *
 * @property url Absolute or relative URL string.
 * @property method HTTP method.
 * @property headers Outbound headers.
 * @property body Raw request body; `null` for bodyless requests.
 * @property timeoutMs Optional per-request timeout in milliseconds.
 */
data class ApiRequest(
  val url: String,
  val method: HttpMethod = HttpMethod.Get,
  val headers: Map<String, String> = emptyMap(),
  val body: ByteArray? = null,
  val timeoutMs: Long? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ApiRequest) return false
    return url == other.url &&
      method == other.method &&
      headers == other.headers &&
      body.contentEquals(other.body) &&
      timeoutMs == other.timeoutMs
  }

  override fun hashCode(): Int {
    var result = url.hashCode()
    result = 31 * result + method.hashCode()
    result = 31 * result + headers.hashCode()
    result = 31 * result + (body?.contentHashCode() ?: 0)
    result = 31 * result + (timeoutMs?.hashCode() ?: 0)
    return result
  }
}

/**
 * Result of a successful transport-level API call (before business envelope parsing).
 *
 * @property data Response payload bytes.
 * @property statusCode HTTP status when known; `null` for non-HTTP transports.
 * @property headers Response headers when known.
 */
data class ApiResponse(
  val data: ByteArray,
  val statusCode: Int? = null,
  val headers: Map<String, String> = emptyMap(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ApiResponse) return false
    return data.contentEquals(other.data) &&
      statusCode == other.statusCode &&
      headers == other.headers
  }

  override fun hashCode(): Int {
    var result = data.contentHashCode()
    result = 31 * result + (statusCode ?: 0)
    result = 31 * result + headers.hashCode()
    return result
  }
}
