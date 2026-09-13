package com.fk.core.storage

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fk.core.pluggable.storage.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Preferences DataStore implementation of Pluggable [KeyValueStore].
 *
 * Values are stored as Base64 strings under `[keyPrefix] + logicalKey`. The sync
 * [KeyValueStore] surface bridges DataStore's Flow API via [runBlocking] — call
 * from a background thread when doing bulk I/O on the main thread matters.
 *
 * @param dataStore Backing Preferences store (see [Storage.preferencesDataStore]).
 * @param keyPrefix Physical key prefix so [clearOwned] / [ownedKeys] never wipe
 *   unrelated preferences that may share the same file.
 */
class DataStoreKeyValueStore(
  private val dataStore: DataStore<Preferences>,
  private val keyPrefix: String = Storage.DEFAULT_KEY_PREFIX,
) : KeyValueStore {
  init {
    require(keyPrefix.isNotEmpty()) { "keyPrefix must not be empty" }
  }

  override fun getBytes(key: String): ByteArray? {
    val logical = requireLogicalKey(key)
    return runCatchingDataStore {
      val encoded = dataStore.data.first()[stringPreferencesKey(physicalKey(logical))]
        ?: return@runCatchingDataStore null
      Base64.decode(encoded, Base64.NO_WRAP)
    }
  }

  override fun putBytes(key: String, value: ByteArray?) {
    val logical = requireLogicalKey(key)
    if (value == null) {
      remove(logical)
      return
    }
    val encoded = Base64.encodeToString(value, Base64.NO_WRAP)
    runCatchingDataStore {
      dataStore.edit { prefs ->
        prefs[stringPreferencesKey(physicalKey(logical))] = encoded
      }
    }
  }

  override fun remove(key: String) {
    val logical = requireLogicalKey(key)
    runCatchingDataStore {
      dataStore.edit { prefs ->
        prefs.remove(stringPreferencesKey(physicalKey(logical)))
      }
    }
  }

  override fun contains(key: String): Boolean {
    val logical = requireLogicalKey(key)
    return runCatchingDataStore {
      dataStore.data.first().contains(stringPreferencesKey(physicalKey(logical)))
    }
  }

  /**
   * Logical keys owned by this instance (prefix stripped), sorted.
   */
  fun ownedKeys(): List<String> = runCatchingDataStore {
    dataStore.data.first().asMap().keys
      .map { it.name }
      .mapNotNull { physical -> logicalKeyOrNull(physical) }
      .sorted()
  }

  /**
   * Removes every preference key that starts with [keyPrefix].
   */
  fun clearOwned() {
    runCatchingDataStore {
      dataStore.edit { prefs ->
        val toRemove = prefs.asMap().keys.filter { it.name.startsWith(keyPrefix) }
        toRemove.forEach { prefs.remove(it) }
      }
    }
  }

  private fun physicalKey(logical: String): String = keyPrefix + logical

  private fun logicalKeyOrNull(physical: String): String? {
    if (!physical.startsWith(keyPrefix)) return null
    return physical.removePrefix(keyPrefix)
  }

  private fun requireLogicalKey(key: String): String {
    if (key.isBlank()) throw StorageException.InvalidKey(key)
    return key
  }

  private fun <T> runCatchingDataStore(block: suspend () -> T): T {
    return try {
      // IO dispatcher avoids Main-thread deadlocks with DataStore's internal actor.
      runBlocking(Dispatchers.IO) { block() }
    } catch (e: StorageException) {
      throw e
    } catch (e: Exception) {
      throw StorageException.DataStoreFailure(e)
    }
  }
}
