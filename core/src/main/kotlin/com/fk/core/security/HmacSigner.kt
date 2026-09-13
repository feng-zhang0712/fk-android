package com.fk.core.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC and deterministic request-parameter signing.
 */
class HmacSigner {
  /** Returns raw HMAC bytes. */
  fun hmac(
    data: ByteArray,
    key: ByteArray,
    algorithm: HmacAlgorithm = HmacAlgorithm.Sha256,
  ): ByteArray {
    if (key.isEmpty()) throw SecurityException.InvalidKey("HMAC key must not be empty")
    return try {
      val mac = Mac.getInstance(algorithm.jcaName)
      mac.init(SecretKeySpec(key, algorithm.jcaName))
      mac.doFinal(data)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  /** Returns lowercase HEX HMAC. */
  fun hmacHex(
    data: ByteArray,
    key: ByteArray,
    algorithm: HmacAlgorithm = HmacAlgorithm.Sha256,
  ): String = SecurityCodec.toHex(hmac(data, key, algorithm), uppercase = false)

  /**
   * Signs query-like parameters: sorted `key=value` joined by `&`, then HMAC-HEX.
   *
   * Values are stringified with [Any.toString]; nested structures are not expanded.
   */
  fun signParameters(
    parameters: Map<String, Any?>,
    secret: String,
    algorithm: HmacAlgorithm = HmacAlgorithm.Sha256,
  ): String {
    val canonical = parameters.entries
      .sortedBy { it.key }
      .joinToString(separator = "&") { (key, value) ->
        "$key=${value?.toString().orEmpty()}"
      }
    return hmacHex(canonical.toByteArray(Charsets.UTF_8), secret.toByteArray(Charsets.UTF_8), algorithm)
  }

  /** Constant-time HEX compare of [signatureHex] against [signParameters]. */
  fun verifyParameters(
    parameters: Map<String, Any?>,
    secret: String,
    signatureHex: String,
    algorithm: HmacAlgorithm = HmacAlgorithm.Sha256,
  ): Boolean {
    val expected = signParameters(parameters, secret, algorithm)
    return constantTimeEquals(expected, signatureHex.lowercase())
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }
}
