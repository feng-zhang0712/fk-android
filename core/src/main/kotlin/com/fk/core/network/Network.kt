package com.fk.core.network

/**
 * Network — OkHttp-backed HTTP transport for fk-android.
 *
 * Implements [com.fk.core.pluggable.networking.ApiClient] so feature modules
 * stay on Pluggable contracts while the host wires a concrete client.
 *
 * Conceptually aligned with iOS `FKCoreKit` Network; Android-shaped APIs.
 */
object Network {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}
