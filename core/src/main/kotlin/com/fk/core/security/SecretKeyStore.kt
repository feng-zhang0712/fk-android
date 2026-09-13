package com.fk.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores raw secret key bytes (e.g. AES keys) for later retrieval.
 */
interface SecretKeyStore {
  /** Persists [keyBytes] under [alias], replacing any existing value. */
  fun put(alias: String, keyBytes: ByteArray)

  /** Returns stored bytes or throws [SecurityException.KeyNotFound]. */
  fun get(alias: String): ByteArray

  /** Removes [alias] if present. */
  fun remove(alias: String)

  /** Whether [alias] exists. */
  fun contains(alias: String): Boolean
}

/**
 * [SecretKeyStore] that wraps values with Android Keystore AES-GCM and keeps
 * ciphertext in a private [SharedPreferences] file.
 *
 * Blob layout: `IV (12 bytes) || ciphertext+tag`, Base64-encoded in prefs.
 */
class AndroidSecretKeyStore(
  context: Context,
  preferencesName: String = "fk_security_keys",
  private val wrapAlias: String = Security.DEFAULT_WRAP_ALIAS,
) : SecretKeyStore {
  init {
    require(wrapAlias.isNotBlank()) { "wrapAlias must not be blank" }
  }

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

  private val wrapCipher = KeystoreWrapCipher(wrapAlias)

  override fun put(alias: String, keyBytes: ByteArray) {
    requireAlias(alias)
    if (keyBytes.isEmpty()) throw SecurityException.InvalidInput("keyBytes must not be empty")
    val sealed = wrapCipher.encrypt(keyBytes)
    val encoded = Base64.encodeToString(sealed, Base64.NO_WRAP)
    val ok = prefs.edit().putString(alias, encoded).commit()
    if (!ok) {
      throw SecurityException.KeystoreFailed(IllegalStateException("SharedPreferences commit failed"))
    }
  }

  override fun get(alias: String): ByteArray {
    requireAlias(alias)
    val encoded = prefs.getString(alias, null)
      ?: throw SecurityException.KeyNotFound(alias)
    return try {
      val sealed = Base64.decode(encoded, Base64.NO_WRAP)
      wrapCipher.decrypt(sealed)
    } catch (e: SecurityException) {
      throw e
    } catch (e: Exception) {
      throw SecurityException.KeystoreFailed(e)
    }
  }

  override fun remove(alias: String) {
    requireAlias(alias)
    prefs.edit().remove(alias).commit()
  }

  override fun contains(alias: String): Boolean {
    requireAlias(alias)
    return prefs.contains(alias)
  }

  private fun requireAlias(alias: String) {
    if (alias.isBlank()) throw SecurityException.InvalidInput("alias must not be blank")
  }
}

/**
 * AES-GCM wrap key living in AndroidKeyStore.
 */
internal class KeystoreWrapCipher(
  private val keyAlias: String,
) {
  private val lock = Any()

  @Volatile
  private var cachedKey: SecretKey? = null

  fun encrypt(plain: ByteArray): ByteArray {
    val secretKey = getOrCreateKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    return cipher.iv + cipher.doFinal(plain)
  }

  fun decrypt(blob: ByteArray): ByteArray {
    if (blob.size <= IV_SIZE) {
      throw SecurityException.KeystoreFailed(IllegalArgumentException("Ciphertext too short"))
    }
    val iv = blob.copyOfRange(0, IV_SIZE)
    val ciphertext = blob.copyOfRange(IV_SIZE, blob.size)
    val secretKey = getOrCreateKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
    return cipher.doFinal(ciphertext)
  }

  private fun getOrCreateKey(): SecretKey {
    cachedKey?.let { return it }
    synchronized(lock) {
      cachedKey?.let { return it }
      try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
          return existing.secretKey.also { cachedKey = it }
        }
        val keyGenerator = KeyGenerator.getInstance(
          KeyProperties.KEY_ALGORITHM_AES,
          ANDROID_KEYSTORE,
        )
        keyGenerator.init(
          KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build(),
        )
        return keyGenerator.generateKey().also { cachedKey = it }
      } catch (e: Exception) {
        throw SecurityException.KeystoreFailed(e)
      }
    }
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val IV_SIZE = 12
    const val TAG_BITS = 128
  }
}
