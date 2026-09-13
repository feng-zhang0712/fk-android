package com.fk.core.security

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Message digests for strings, bytes, and files (HEX lowercase output).
 */
class Hasher {
  /** Digests UTF-8 [text] and returns a lowercase HEX string. */
  fun hashString(text: String, algorithm: HashAlgorithm = HashAlgorithm.Sha256): String =
    hashBytes(text.toByteArray(Charsets.UTF_8), algorithm)

  /** Digests [bytes] and returns a lowercase HEX string. */
  fun hashBytes(bytes: ByteArray, algorithm: HashAlgorithm = HashAlgorithm.Sha256): String {
    return try {
      val digest = MessageDigest.getInstance(algorithm.jcaName).digest(bytes)
      SecurityCodec.toHex(digest, uppercase = false)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  /**
   * Streaming file digest (1 MiB chunks). Prefer SHA-256+ for integrity.
   */
  fun hashFile(file: File, algorithm: HashAlgorithm = HashAlgorithm.Sha256): String {
    if (!file.isFile) {
      throw SecurityException.InvalidInput("Not a readable file: ${file.path}")
    }
    return try {
      val digest = MessageDigest.getInstance(algorithm.jcaName)
      val buffer = ByteArray(1024 * 1024)
      FileInputStream(file).use { input ->
        while (true) {
          val read = input.read(buffer)
          if (read <= 0) break
          digest.update(buffer, 0, read)
        }
      }
      SecurityCodec.toHex(digest.digest(), uppercase = false)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }
}
