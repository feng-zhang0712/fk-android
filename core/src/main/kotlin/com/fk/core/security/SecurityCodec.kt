package com.fk.core.security

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Base64 / HEX / URL helpers used across Security services.
 */
object SecurityCodec {
  /** Encodes [bytes] as Base64 (NO_WRAP). */
  fun toBase64(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.NO_WRAP)

  /** Decodes Base64 [text]. */
  fun fromBase64(text: String): ByteArray {
    return try {
      Base64.decode(text, Base64.NO_WRAP)
    } catch (e: IllegalArgumentException) {
      throw SecurityException.InvalidInput("Invalid Base64", e)
    }
  }

  /** Encodes [bytes] as HEX. */
  fun toHex(bytes: ByteArray, uppercase: Boolean = false): String {
    val hexChars = if (uppercase) HEX_UPPER else HEX_LOWER
    val out = CharArray(bytes.size * 2)
    var i = 0
    for (b in bytes) {
      val v = b.toInt() and 0xFF
      out[i++] = hexChars[v ushr 4]
      out[i++] = hexChars[v and 0x0F]
    }
    return String(out)
  }

  /** Decodes a HEX string (odd length rejected). */
  fun fromHex(hex: String): ByteArray {
    val cleaned = hex.trim().replace(" ", "")
    if (cleaned.length % 2 != 0) {
      throw SecurityException.InvalidInput("HEX length must be even")
    }
    return try {
      ByteArray(cleaned.length / 2) { index ->
        cleaned.substring(index * 2, index * 2 + 2).toInt(16).toByte()
      }
    } catch (e: Exception) {
      throw SecurityException.InvalidInput("Invalid HEX", e)
    }
  }

  /** application/x-www-form-urlencoded encode. */
  fun urlEncode(text: String): String =
    URLEncoder.encode(text, Charsets.UTF_8.name())

  /** application/x-www-form-urlencoded decode. */
  fun urlDecode(text: String): String =
    URLDecoder.decode(text, Charsets.UTF_8.name())

  private val HEX_LOWER = "0123456789abcdef".toCharArray()
  private val HEX_UPPER = "0123456789ABCDEF".toCharArray()
}
