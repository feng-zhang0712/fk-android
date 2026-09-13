package com.fk.core.security

import java.io.IOException

/**
 * Failures thrown by Security helpers.
 */
sealed class SecurityException(
  message: String,
  cause: Throwable? = null,
) : IOException(message, cause) {
  /** Input bytes / strings are unusable for the requested operation. */
  class InvalidInput(detail: String, cause: Throwable? = null) : SecurityException(detail, cause)

  /** Key or IV missing / wrong length. */
  class InvalidKey(detail: String) : SecurityException(detail)

  /** JCA cipher / digest / signature failure. */
  class CryptoFailed(cause: Throwable) : SecurityException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Crypto failed",
    cause = cause,
  )

  /** Android Keystore or preference persistence failure. */
  class KeystoreFailed(cause: Throwable) : SecurityException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Keystore failed",
    cause = cause,
  )

  /** Requested secret alias is missing. */
  class KeyNotFound(alias: String) : SecurityException("Key not found: $alias")
}
