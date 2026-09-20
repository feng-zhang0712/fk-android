package com.fk.core.security

import android.content.Context

/**
 * Security — hash, AES, RSA, HMAC, masking, random, and Keystore-backed key storage.
 *
 * Conceptually aligned with iOS `FKCoreKit` Security; Android-shaped JCA / Keystore APIs.
 */
object Security {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.2"

  /** Default Android Keystore alias used to wrap secrets in [AndroidSecretKeyStore]. */
  const val DEFAULT_WRAP_ALIAS: String = "fk.security.wrap"

  /** Builds a façade without a [SecretKeyStore] (no [Context] required). */
  fun create(): FkSecurity = FkSecurity()

  /**
   * Builds a façade that can persist raw key bytes via [AndroidSecretKeyStore].
   */
  fun create(context: Context, prefsName: String = "fk_security_keys"): FkSecurity {
    val utils = SecurityUtils()
    return FkSecurity(
      utils = utils,
      aes = AesCryptor(utils),
      secretKeyStore = AndroidSecretKeyStore(
        context = context,
        preferencesName = prefsName,
      ),
    )
  }
}

/**
 * Convenience bag of security services for host wiring / samples.
 *
 * Prefer injecting individual collaborators into feature modules when possible.
 *
 * @param utils Shared by [aes] when constructed via defaults / [Security.create].
 */
class FkSecurity(
  val utils: SecurityUtils = SecurityUtils(),
  val hasher: Hasher = Hasher(),
  val aes: AesCryptor = AesCryptor(utils),
  val rsa: RsaCryptor = RsaCryptor(),
  val hmac: HmacSigner = HmacSigner(),
  val codec: SecurityCodec = SecurityCodec,
  val secretKeyStore: SecretKeyStore? = null,
)
