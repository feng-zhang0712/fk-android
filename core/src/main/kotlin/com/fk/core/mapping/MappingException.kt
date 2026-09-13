package com.fk.core.mapping

import java.io.IOException

/**
 * Mapping / envelope failures thrown by [JsonMapper] and related helpers.
 */
sealed class MappingException(
  message: String,
  cause: Throwable? = null,
) : IOException(message, cause) {
  /** Bytes are not valid JSON. */
  class InvalidJson(cause: Throwable? = null) : MappingException(
    message = cause?.message?.takeIf { it.isNotBlank() } ?: "Invalid JSON",
    cause = cause,
  )

  /** kotlinx.serialization decode failed. */
  class DecodingFailed(cause: Throwable) : MappingException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Decoding failed",
    cause = cause,
  )

  /** kotlinx.serialization encode failed. */
  class EncodingFailed(cause: Throwable) : MappingException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Encoding failed",
    cause = cause,
  )

  /** Expected envelope key / path was missing. */
  class KeyNotFound(path: String) : MappingException("Key not found: $path")

  /** Envelope reported a non-success business code (or success flag = false). */
  class BusinessFailure(
    val code: Int,
    val businessMessage: String?,
  ) : MappingException(
    message = buildString {
      append("Business failure code=")
      append(code)
      if (!businessMessage.isNullOrBlank()) {
        append(": ")
        append(businessMessage)
      }
    },
  )
}
