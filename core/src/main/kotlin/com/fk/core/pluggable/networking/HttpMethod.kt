package com.fk.core.pluggable.networking

/**
 * HTTP methods supported by [ApiRequest] and [ApiClient].
 */
enum class HttpMethod(val wireValue: String) {
  Get("GET"),
  Post("POST"),
  Put("PUT"),
  Patch("PATCH"),
  Delete("DELETE"),
  Head("HEAD"),
}
