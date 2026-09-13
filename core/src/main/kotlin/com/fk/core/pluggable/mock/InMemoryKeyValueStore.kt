package com.fk.core.pluggable.mock

import com.fk.core.pluggable.storage.KeyValueStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory [KeyValueStore] for samples and unit tests.
 */
class InMemoryKeyValueStore : KeyValueStore {
  private val values = ConcurrentHashMap<String, ByteArray>()

  override fun getBytes(key: String): ByteArray? = values[key]?.copyOf()

  override fun putBytes(key: String, value: ByteArray?) {
    if (value == null) {
      values.remove(key)
    } else {
      values[key] = value.copyOf()
    }
  }

  override fun remove(key: String) {
    values.remove(key)
  }

  override fun contains(key: String): Boolean = values.containsKey(key)
}
