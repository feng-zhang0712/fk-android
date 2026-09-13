package com.fk.core.pluggable

/**
 * Pluggable — narrow, swappable contracts for app infrastructure.
 *
 * Feature modules depend on these interfaces instead of concrete networking,
 * storage, session, or logging SDKs. Host apps (or later `:core` packages such
 * as `network` / `storage`) supply implementations at composition time.
 *
 * This package ships **contracts, shared value types, a composition bag, and
 * lightweight mocks**. Heavy adapters (OkHttp, DataStore, …) belong in their
 * dedicated packages.
 *
 * Conceptually aligned with iOS `FKCoreKit` Pluggable — Android-shaped APIs.
 */
object Pluggable {
  /**
   * Contract revision. Bump only when public interfaces change in a breaking way.
   */
  const val CONTRACT_VERSION: Int = 1
}
