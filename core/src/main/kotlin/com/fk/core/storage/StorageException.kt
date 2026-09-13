package com.fk.core.storage

import java.io.IOException

/**
 * Persistence failures thrown by Storage backends.
 *
 * Missing keys return `null` from [com.fk.core.pluggable.storage.KeyValueStore]
 * / [com.fk.core.pluggable.storage.TypedStore] reads — they are not exceptions.
 */
sealed class StorageException(
  message: String,
  cause: Throwable? = null,
) : IOException(message, cause) {
  /** Logical key is blank or otherwise unusable. */
  class InvalidKey(key: String) : StorageException("Invalid storage key: '$key'")

  /** Preferences DataStore read/write failed. */
  class DataStoreFailure(cause: Throwable) : StorageException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "DataStore failure",
    cause = cause,
  )

  /** Android Keystore key creation or lookup failed. */
  class KeystoreFailure(cause: Throwable) : StorageException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Keystore failure",
    cause = cause,
  )

  /** Ciphertext could not be decrypted (corrupt blob or wrong key alias). */
  class DecryptionFailed(cause: Throwable? = null) : StorageException(
    message = cause?.message?.takeIf { it.isNotBlank() } ?: "Decryption failed",
    cause = cause,
  )

  /** Plaintext could not be encrypted. */
  class EncryptionFailed(cause: Throwable) : StorageException(
    message = cause.message?.takeIf { it.isNotBlank() } ?: "Encryption failed",
    cause = cause,
  )
}
