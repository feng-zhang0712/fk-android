package com.fk.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.fk.core.pluggable.storage.KeyValueStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [KeyValueStore] decorator that encrypts values with AES-GCM using a key in
 * the Android Keystore.
 *
 * Keys remain plaintext (optionally prefixed by the delegate). Suitable for
 * tokens and small secrets layered on [DataStoreKeyValueStore] or any other
 * [KeyValueStore].
 *
 * Blob layout: `IV (12 bytes) || ciphertext+tag`.
 *
 * @param delegate Backing store for ciphertext bytes.
 * @param keystoreAlias Android Keystore secret-key alias (created on first use).
 */
class EncryptedKeyValueStore(
  private val delegate: KeyValueStore,
  private val keystoreAlias: String = Storage.DEFAULT_KEYSTORE_ALIAS,
) : KeyValueStore {
  init {
    require(keystoreAlias.isNotBlank()) { "keystoreAlias must not be blank" }
  }

  private val cipher = KeystoreAesGcmCipher(keystoreAlias)

  override fun getBytes(key: String): ByteArray? {
    val blob = delegate.getBytes(key) ?: return null
    return try {
      cipher.decrypt(blob)
    } catch (e: StorageException) {
      throw e
    } catch (e: Exception) {
      throw StorageException.DecryptionFailed(e)
    }
  }

  override fun putBytes(key: String, value: ByteArray?) {
    if (value == null) {
      delegate.putBytes(key, null)
      return
    }
    val encrypted = try {
      cipher.encrypt(value)
    } catch (e: StorageException) {
      throw e
    } catch (e: Exception) {
      throw StorageException.EncryptionFailed(e)
    }
    delegate.putBytes(key, encrypted)
  }

  override fun remove(key: String) = delegate.remove(key)

  override fun contains(key: String): Boolean = delegate.contains(key)
}

/**
 * AES/GCM encryptor backed by AndroidKeyStore.
 *
 * Caches the [SecretKey] after first load/create. Concurrent first-use is
 * serialized so two threads cannot both call [KeyGenerator.generateKey].
 */
internal class KeystoreAesGcmCipher(
  private val keyAlias: String,
) {
  private val lock = Any()

  @Volatile
  private var cachedKey: SecretKey? = null

  fun encrypt(plain: ByteArray): ByteArray {
    val secretKey = getOrCreateKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val iv = cipher.iv
    val ciphertext = cipher.doFinal(plain)
    return iv + ciphertext
  }

  fun decrypt(blob: ByteArray): ByteArray {
    if (blob.size <= IV_SIZE) {
      throw StorageException.DecryptionFailed()
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
            .setKeySize(KEY_SIZE_BITS)
            .build(),
        )
        return keyGenerator.generateKey().also { cachedKey = it }
      } catch (e: Exception) {
        throw StorageException.KeystoreFailure(e)
      }
    }
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val IV_SIZE = 12
    const val TAG_BITS = 128
    const val KEY_SIZE_BITS = 256
  }
}
