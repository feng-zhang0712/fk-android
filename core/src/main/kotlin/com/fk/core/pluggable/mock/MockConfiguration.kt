package com.fk.core.pluggable.mock

import com.fk.core.pluggable.configuration.AppEnvironment
import com.fk.core.pluggable.configuration.AppEnvironmentProvider
import com.fk.core.pluggable.configuration.FeatureFlagProvider
import com.fk.core.pluggable.configuration.RemoteConfigProvider
import com.fk.core.pluggable.networking.Reachability
import java.util.concurrent.ConcurrentHashMap

/** Fixed [AppEnvironmentProvider] for samples. */
class MockAppEnvironmentProvider(
  override val environment: AppEnvironment = AppEnvironment.Development,
  override val apiBaseUrl: String = "https://api.example.com",
  override val webBaseUrl: String? = "https://www.example.com",
) : AppEnvironmentProvider

/** In-memory [FeatureFlagProvider]. */
class MockFeatureFlagProvider(
  private val flags: Map<String, Boolean> = emptyMap(),
  private val strings: Map<String, String> = emptyMap(),
) : FeatureFlagProvider {
  override fun isEnabled(key: String): Boolean = flags[key] == true
  override fun stringValue(key: String): String? = strings[key]
}

/** In-memory [RemoteConfigProvider]. */
class MockRemoteConfigProvider : RemoteConfigProvider {
  private val strings = ConcurrentHashMap<String, String>()
  private val bools = ConcurrentHashMap<String, Boolean>()

  fun putString(key: String, value: String) {
    strings[key] = value
  }

  fun putBool(key: String, value: Boolean) {
    bools[key] = value
  }

  override suspend fun fetch() = Unit

  override fun string(key: String): String? = strings[key]

  override fun bool(key: String): Boolean? = bools[key]
}

/** Fixed [Reachability] for samples. */
class MockReachability(
  override val isReachable: Boolean = true,
) : Reachability
