package com.fk.core.security

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * RSA key generation, encrypt/decrypt, and sign/verify.
 *
 * Public keys use X.509 / SPKI DER; private keys use PKCS#8 DER.
 */
class RsaCryptor {
  /**
   * Generates an RSA key pair.
   *
   * @param keySizeBits 2048 / 3072 / 4096.
   * @param tag Caller-defined label stored on [RsaKeyPair.tag].
   */
  fun generateKeyPair(keySizeBits: Int = 2048, tag: String = "fk.rsa"): RsaKeyPair {
    if (keySizeBits != 2048 && keySizeBits != 3072 && keySizeBits != 4096) {
      throw SecurityException.InvalidKey("RSA key size must be 2048, 3072, or 4096")
    }
    return try {
      val generator = KeyPairGenerator.getInstance("RSA")
      generator.initialize(keySizeBits)
      val pair = generator.generateKeyPair()
      RsaKeyPair(
        publicKeyDer = pair.public.encoded,
        privateKeyDer = pair.private.encoded,
        tag = tag,
      )
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  fun encrypt(
    plain: ByteArray,
    publicKeyDer: ByteArray,
    padding: RsaEncryptionPadding = RsaEncryptionPadding.OaepSha256,
  ): ByteArray {
    return try {
      val cipher = cipherFor(padding)
      val publicKey = decodePublic(publicKeyDer)
      when (padding) {
        RsaEncryptionPadding.OaepSha256 ->
          cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams())
        RsaEncryptionPadding.Pkcs1 ->
          cipher.init(Cipher.ENCRYPT_MODE, publicKey)
      }
      cipher.doFinal(plain)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  fun decrypt(
    cipherBytes: ByteArray,
    privateKeyDer: ByteArray,
    padding: RsaEncryptionPadding = RsaEncryptionPadding.OaepSha256,
  ): ByteArray {
    return try {
      val cipher = cipherFor(padding)
      val privateKey = decodePrivate(privateKeyDer)
      when (padding) {
        RsaEncryptionPadding.OaepSha256 ->
          cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams())
        RsaEncryptionPadding.Pkcs1 ->
          cipher.init(Cipher.DECRYPT_MODE, privateKey)
      }
      cipher.doFinal(cipherBytes)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  fun sign(
    data: ByteArray,
    privateKeyDer: ByteArray,
    algorithm: RsaSignatureAlgorithm = RsaSignatureAlgorithm.Pkcs1Sha256,
  ): ByteArray {
    return try {
      val signature = Signature.getInstance(algorithm.jcaName)
      signature.initSign(decodePrivate(privateKeyDer))
      signature.update(data)
      signature.sign()
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  fun verify(
    signatureBytes: ByteArray,
    data: ByteArray,
    publicKeyDer: ByteArray,
    algorithm: RsaSignatureAlgorithm = RsaSignatureAlgorithm.Pkcs1Sha256,
  ): Boolean {
    return try {
      val signature = Signature.getInstance(algorithm.jcaName)
      signature.initVerify(decodePublic(publicKeyDer))
      signature.update(data)
      signature.verify(signatureBytes)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.CryptoFailed(e)
    }
  }

  private fun cipherFor(padding: RsaEncryptionPadding): Cipher = when (padding) {
    RsaEncryptionPadding.Pkcs1 -> Cipher.getInstance("RSA/ECB/PKCS1Padding")
    RsaEncryptionPadding.OaepSha256 -> Cipher.getInstance("RSA/ECB/OAEPPadding")
  }

  private fun oaepParams(): OAEPParameterSpec =
    OAEPParameterSpec(
      "SHA-256",
      "MGF1",
      MGF1ParameterSpec.SHA256,
      PSource.PSpecified.DEFAULT,
    )

  private fun decodePublic(der: ByteArray): PublicKey =
    KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))

  private fun decodePrivate(der: ByteArray): PrivateKey =
    KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
}
