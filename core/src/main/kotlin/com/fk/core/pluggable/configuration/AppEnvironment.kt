package com.fk.core.pluggable.configuration

/**
 * Deployment environment for build-time or runtime configuration.
 */
enum class AppEnvironment {
  Development,
  Staging,
  Production,
  ;

  /** Whether verbose logging and debug menus are typically enabled. */
  val isDebuggable: Boolean
    get() = this != Production
}

/** Supplies environment-specific endpoints and flags. */
interface AppEnvironmentProvider {
  /** Active deployment environment. */
  val environment: AppEnvironment

  /** API base URL for the active environment. */
  val apiBaseUrl: String

  /** Optional web base URL (H5, marketing pages). */
  val webBaseUrl: String?
}

/** Boolean or multivariate feature flags (local defaults + remote overrides). */
interface FeatureFlagProvider {
  /** Returns whether a flag is enabled; `false` when unknown unless documented otherwise. */
  fun isEnabled(key: String): Boolean

  /** Optional string payload for multivariate flags. */
  fun stringValue(key: String): String?
}

/** Fetches remote configuration (Firebase Remote Config, internal CMS, etc.). */
interface RemoteConfigProvider {
  /** Activates latest remote values. */
  suspend fun fetch()

  /** String config value, or `null` when undefined. */
  fun string(key: String): String?

  /** Boolean config value when defined. */
  fun bool(key: String): Boolean?
}
