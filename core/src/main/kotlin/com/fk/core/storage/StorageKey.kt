package com.fk.core.storage

/**
 * Namespace-aware logical key to reduce collisions between features or apps.
 *
 * Prefer a shared enum of keys per module over scattering raw strings.
 *
 * ```kotlin
 * enum class AppStorageKey(override val rawValue: String) : StorageKey {
 *   AuthToken("auth.token"),
 *   ;
 *   override val namespace: String get() = "com.example.app"
 * }
 * store.put(AppStorageKey.AuthToken.fullKey, token, String.serializer())
 * ```
 */
interface StorageKey {
  /** Short identifier unique within [namespace]. */
  val rawValue: String

  /** Stable prefix (bundle id, team id, or feature id). */
  val namespace: String

  /** Canonical string passed to storage: `"<namespace>.<rawValue>"`. */
  val fullKey: String
    get() = "$namespace.$rawValue"
}

/**
 * Ad-hoc [StorageKey] when a dynamic or one-off key is enough.
 */
data class StringStorageKey(
  override val namespace: String,
  override val rawValue: String,
) : StorageKey
