package com.fk.core.pluggable.storage

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json

/**
 * Binary key-value storage boundary (DataStore, SharedPreferences, file, or memory).
 */
interface KeyValueStore {
  /** Reads raw bytes for [key], or `null` when missing. */
  fun getBytes(key: String): ByteArray?

  /** Writes or deletes data for [key]. Pass `null` to remove the entry. */
  fun putBytes(key: String, value: ByteArray?)

  /** Removes the value for [key] if present. */
  fun remove(key: String)

  /** Whether a value exists for [key]. */
  fun contains(key: String): Boolean
}

/**
 * Typed storage built on [KeyValueStore] using kotlinx.serialization.
 *
 * Prefer injecting this into feature modules instead of a concrete DataStore type.
 */
interface TypedStore : KeyValueStore {
  /**
   * Decodes a value when present.
   *
   * @param deserializer Strategy for [T] (e.g. `serializer()`).
   */
  fun <T> get(key: String, deserializer: DeserializationStrategy<T>): T?

  /**
   * Encodes and stores a value.
   *
   * @param serializer Strategy for [T].
   */
  fun <T> put(key: String, value: T, serializer: SerializationStrategy<T>)
}

/**
 * Default JSON helpers for [TypedStore] adapters.
 */
object PluggableJson {
  /** Shared lenient JSON format for pluggable typed storage. */
  val Default: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
  }
}

/**
 * Default [TypedStore] implementation over a [KeyValueStore] + [StringFormat].
 */
class DefaultTypedStore(
  private val keyValueStore: KeyValueStore,
  private val format: StringFormat = PluggableJson.Default,
) : TypedStore, KeyValueStore by keyValueStore {
  override fun <T> get(key: String, deserializer: DeserializationStrategy<T>): T? {
    val bytes = getBytes(key) ?: return null
    return format.decodeFromString(deserializer, bytes.decodeToString())
  }

  override fun <T> put(key: String, value: T, serializer: SerializationStrategy<T>) {
    val encoded = format.encodeToString(serializer, value)
    putBytes(key, encoded.encodeToByteArray())
  }
}
