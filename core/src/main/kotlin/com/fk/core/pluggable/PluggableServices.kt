package com.fk.core.pluggable

import com.fk.core.pluggable.configuration.AppEnvironmentProvider
import com.fk.core.pluggable.configuration.FeatureFlagProvider
import com.fk.core.pluggable.configuration.RemoteConfigProvider
import com.fk.core.pluggable.core.AppLifecycleObserver
import com.fk.core.pluggable.logging.PluggableLogger
import com.fk.core.pluggable.networking.ApiClient
import com.fk.core.pluggable.networking.CredentialStore
import com.fk.core.pluggable.networking.Reachability
import com.fk.core.pluggable.session.UserSession
import com.fk.core.pluggable.session.UserSessionObserver
import com.fk.core.pluggable.storage.TypedStore

/**
 * Optional composition-root bag of common Pluggable dependencies.
 *
 * Prefer injecting **individual** interfaces into feature modules rather than
 * passing this container everywhere. Useful for samples and small apps.
 *
 * Nullable fields are intentional — wire only what the host needs.
 */
data class PluggableServices(
  val apiClient: ApiClient? = null,
  val storage: TypedStore? = null,
  val session: UserSession? = null,
  val sessionObserver: UserSessionObserver? = null,
  val environment: AppEnvironmentProvider? = null,
  val featureFlags: FeatureFlagProvider? = null,
  val remoteConfig: RemoteConfigProvider? = null,
  val logger: PluggableLogger? = null,
  val reachability: Reachability? = null,
  val credentials: CredentialStore? = null,
  val appLifecycle: AppLifecycleObserver? = null,
)
