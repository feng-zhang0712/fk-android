package com.fk.core.security

import java.security.SecureRandom

/**
 * Secure random helpers and PII masking utilities.
 */
class SecurityUtils(
  private val random: SecureRandom = SecureRandom(),
) {
  /** Cryptographically strong random bytes. */
  fun randomBytes(count: Int): ByteArray {
    if (count <= 0) throw SecurityException.InvalidInput("count must be > 0")
    val bytes = ByteArray(count)
    random.nextBytes(bytes)
    return bytes
  }

  /** Random string drawn from [alphabet] (unbiased [SecureRandom.nextInt]). */
  fun randomString(
    length: Int,
    alphabet: String = DEFAULT_ALPHABET,
  ): String {
    if (length <= 0) throw SecurityException.InvalidInput("length must be > 0")
    if (alphabet.isEmpty()) throw SecurityException.InvalidInput("alphabet must not be empty")
    val chars = alphabet.toCharArray()
    return buildString(length) {
      repeat(length) {
        append(chars[random.nextInt(chars.size)])
      }
    }
  }

  /** Masks a phone-like digit string: `138****5678`. */
  fun maskPhone(value: String): String {
    val digits = value.filter { it.isDigit() }
    if (digits.length < 7) return value
    return digits.take(3) + "****" + digits.takeLast(4)
  }

  /** Masks an ID-like string keeping 3 prefix/suffix characters. */
  fun maskIdCard(value: String): String {
    val trimmed = value.trim()
    if (trimmed.length < 8) return value
    val middle = "*".repeat(trimmed.length - 6)
    return trimmed.take(3) + middle + trimmed.takeLast(3)
  }

  /** Masks an email local-part. */
  fun maskEmail(value: String): String {
    val at = value.indexOf('@')
    if (at <= 0 || at == value.lastIndex) return value
    val name = value.substring(0, at)
    val domain = value.substring(at + 1)
    val maskedName = when {
      name.length <= 2 -> name.take(1) + "*"
      else -> name.first() + "*".repeat(name.length - 2) + name.last()
    }
    return "$maskedName@$domain"
  }

  /**
   * Overwrites [bytes] in place with zeros (best-effort; GC may retain copies).
   */
  fun secureWipe(bytes: ByteArray) {
    bytes.fill(0)
  }

  companion object {
    const val DEFAULT_ALPHABET: String =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  }
}
