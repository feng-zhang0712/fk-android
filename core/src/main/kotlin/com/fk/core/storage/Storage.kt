package com.fk.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.fk.core.pluggable.storage.DefaultTypedStore
import com.fk.core.pluggable.storage.TypedStore

/**
 * Storage — DataStore-backed key-value persistence for fk-android.
 *
 * Implements Pluggable [KeyValueStore] / [TypedStore] so feature modules stay on
 * contracts while the host wires a concrete store. Encrypted values use
 * Android Keystore AES-GCM via [EncryptedKeyValueStore].
 *
 * Conceptually aligned with iOS `FKCoreKit` Storage; Android-shaped APIs.
 */
object Storage {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Default Preferences DataStore file name (no extension). */
  const val DEFAULT_DATASTORE_NAME: String = "fk_storage"

  /** Default logical-key prefix written into Preferences. */
  const val DEFAULT_KEY_PREFIX: String = "fk.storage."

  /** Default Android Keystore alias for [EncryptedKeyValueStore]. */
  const val DEFAULT_KEYSTORE_ALIAS: String = "fk.storage.aes"

  private val dataStoreLock = Any()
  private val dataStores = mutableMapOf<String, DataStore<Preferences>>()

  /**
   * Creates (or reuses) a Preferences [DataStore] under the app's files directory.
   *
   * DataStore forbids multiple active instances for the same file; callers with the
   * same [context] package + [name] share one instance.
   *
   * @param context Any context; [Context.getApplicationContext] is used.
   * @param name File name without extension (maps to `datastore/<name>.preferences_pb`).
   */
  fun preferencesDataStore(
    context: Context,
    name: String = DEFAULT_DATASTORE_NAME,
  ): DataStore<Preferences> {
    val appContext = context.applicationContext
    val cacheKey = "${appContext.packageName}:$name"
    synchronized(dataStoreLock) {
      return dataStores.getOrPut(cacheKey) {
        PreferenceDataStoreFactory.create(
          produceFile = { appContext.preferencesDataStoreFile(name) },
        )
      }
    }
  }

  /**
   * Builds a [DataStoreKeyValueStore] with the default DataStore file and key prefix.
   */
  fun keyValueStore(
    context: Context,
    name: String = DEFAULT_DATASTORE_NAME,
    keyPrefix: String = DEFAULT_KEY_PREFIX,
  ): DataStoreKeyValueStore = DataStoreKeyValueStore(
    dataStore = preferencesDataStore(context, name),
    keyPrefix = keyPrefix,
  )

  /**
   * Builds a [TypedStore] over [keyValueStore] (JSON via [DefaultTypedStore]).
   */
  fun typedStore(
    context: Context,
    name: String = DEFAULT_DATASTORE_NAME,
    keyPrefix: String = DEFAULT_KEY_PREFIX,
  ): TypedStore = DefaultTypedStore(keyValueStore(context, name, keyPrefix))

  /**
   * Builds an encrypted [KeyValueStore]: plaintext keys, Keystore AES-GCM ciphertext values.
   *
   * Prefer a dedicated DataStore [name] so encrypted and plain preferences do not
   * share one file accidentally.
   */
  fun encryptedKeyValueStore(
    context: Context,
    name: String = "fk_storage_secure",
    keyPrefix: String = DEFAULT_KEY_PREFIX,
    keystoreAlias: String = DEFAULT_KEYSTORE_ALIAS,
  ): EncryptedKeyValueStore = EncryptedKeyValueStore(
    delegate = keyValueStore(context, name, keyPrefix),
    keystoreAlias = keystoreAlias,
  )

  /**
   * Builds an encrypted [TypedStore] over [encryptedKeyValueStore] (JSON via [DefaultTypedStore]).
   *
   * Suitable for tokens and small secrets.
   */
  fun encryptedTypedStore(
    context: Context,
    name: String = "fk_storage_secure",
    keyPrefix: String = DEFAULT_KEY_PREFIX,
    keystoreAlias: String = DEFAULT_KEYSTORE_ALIAS,
  ): TypedStore = DefaultTypedStore(
    encryptedKeyValueStore(context, name, keyPrefix, keystoreAlias),
  )
}
