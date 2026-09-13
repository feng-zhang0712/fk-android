package com.fk.core.security

/** Supported hash algorithms (MD5/SHA-1 are legacy-compatible only). */
enum class HashAlgorithm(val jcaName: String) {
  Md5("MD5"),
  Sha1("SHA-1"),
  Sha256("SHA-256"),
  Sha512("SHA-512"),
}

/** AES block cipher mode. */
enum class AesMode {
  /** CBC requires a 16-byte IV. */
  Cbc,

  /** ECB ignores IV (interop only; prefer [Cbc]). */
  Ecb,
}

/** HMAC digest algorithms. */
enum class HmacAlgorithm(val jcaName: String) {
  Sha256("HmacSHA256"),
  Sha512("HmacSHA512"),
}

/** RSA encryption padding schemes. */
enum class RsaEncryptionPadding {
  /** RSAES-PKCS1-v1_5. */
  Pkcs1,

  /** RSA-OAEP with SHA-256. */
  OaepSha256,
}

/** RSA signature schemes. */
enum class RsaSignatureAlgorithm(val jcaName: String) {
  Pkcs1Sha256("SHA256withRSA"),
  Pkcs1Sha512("SHA512withRSA"),
}

/**
 * RSA key pair container (SPKI public / PKCS#8 private encodings).
 */
data class RsaKeyPair(
  val publicKeyDer: ByteArray,
  val privateKeyDer: ByteArray,
  val tag: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RsaKeyPair) return false
    return tag == other.tag &&
      publicKeyDer.contentEquals(other.publicKeyDer) &&
      privateKeyDer.contentEquals(other.privateKeyDer)
  }

  override fun hashCode(): Int {
    var result = tag.hashCode()
    result = 31 * result + publicKeyDer.contentHashCode()
    result = 31 * result + privateKeyDer.contentHashCode()
    return result
  }
}
