package com.fk.core.security

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES encrypt/decrypt (CBC / ECB + PKCS5/PKCS7 padding) via JCA.
 *
 * Key length must be 16 / 24 / 32 bytes. CBC requires a 16-byte IV.
 */
class AesCryptor(
  private val utils: SecurityUtils = SecurityUtils(),
) {
  /** Generates a cryptographically strong AES key of [keyBytes] length. */
  fun generateKey(keyBytes: Int = 32): ByteArray {
    if (keyBytes != 16 && keyBytes != 24 && keyBytes != 32) {
      throw SecurityException.InvalidKey("AES key length must be 16, 24, or 32 bytes")
    }
    return utils.randomBytes(keyBytes)
  }

  /** Generates a 16-byte IV for CBC. */
  fun generateIv(): ByteArray = utils.randomBytes(AES_BLOCK_SIZE)

  /** Encrypts [plain] with [key] (and [iv] when [mode] is [AesMode.Cbc]). */
  fun encrypt(
    plain: ByteArray,
    key: ByteArray,
    iv: ByteArray? = null,
    mode: AesMode = AesMode.Cbc,
  ): ByteArray = crypt(Cipher.ENCRYPT_MODE, plain, key, iv, mode)

  /** Decrypts [cipher] with [key] (and [iv] when [mode] is [AesMode.Cbc]). */
  fun decrypt(
    cipher: ByteArray,
    key: ByteArray,
    iv: ByteArray? = null,
    mode: AesMode = AesMode.Cbc,
  ): ByteArray = crypt(Cipher.DECRYPT_MODE, cipher, key, iv, mode)

  /** Encrypts UTF-8 [text] and returns Base64 ciphertext. */
  fun encryptToBase64(
    text: String,
    key: ByteArray,
    iv: ByteArray? = null,
    mode: AesMode = AesMode.Cbc,
  ): String = SecurityCodec.toBase64(encrypt(text.toByteArray(Charsets.UTF_8), key, iv, mode))

  /** Decrypts Base64 [ciphertext] to a UTF-8 string. */
  fun decryptFromBase64(
    ciphertext: String,
    key: ByteArray,
    iv: ByteArray? = null,
    mode: AesMode = AesMode.Cbc,
  ): String {
    val plain = decrypt(SecurityCodec.fromBase64(ciphertext), key, iv, mode)
    return plain.toString(Charsets.UTF_8)
  }

  private fun crypt(
    opmode: Int,
    input: ByteArray,
    key: ByteArray,
    iv: ByteArray?,
    mode: AesMode,
  ): ByteArray {
    validate(key, iv, mode)
    return try {
      val transformation = when (mode) {
        AesMode.Cbc -> "AES/CBC/PKCS5Padding"
        AesMode.Ecb -> "AES/ECB/PKCS5Padding"
      }
      val cipher = Cipher.getInstance(transformation)
      val secret = SecretKeySpec(key, "AES")
      when (mode) {
        AesMode.Cbc -> cipher.init(opmode, secret, IvParameterSpec(iv!!))
        AesMode.Ecb -> cipher.init(opmode, secret)
      }
      cipher.doFinal(input)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  private fun validate(key: ByteArray, iv: ByteArray?, mode: AesMode) {
    if (key.size != 16 && key.size != 24 && key.size != 32) {
      throw SecurityException.InvalidKey("AES key length must be 16, 24, or 32 bytes")
    }
    if (mode == AesMode.Cbc) {
      if (iv == null || iv.size != AES_BLOCK_SIZE) {
        throw SecurityException.InvalidKey("CBC requires a 16-byte IV")
      }
    }
  }

  private companion object {
    const val AES_BLOCK_SIZE = 16
  }
}
