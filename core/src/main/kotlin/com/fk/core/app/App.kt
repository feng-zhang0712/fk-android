package com.fk.core.app

import android.content.Context
import com.fk.core.pluggable.core.AppLifecycleObserver
import com.fk.core.pluggable.mock.MockAppLifecycleObserver

/**
 * App infra — version, deeplink, lifecycle, analytics sink, startup tasks.
 *
 * Conceptually aligned with iOS `FKCoreKit` BusinessKit (non-UI).
 * Named package hub [App] (factory); UI Base VC patterns are intentionally out of scope.
 */
object App {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Builds production defaults bound to [context]. */
  fun create(
    context: Context,
    configuration: AppConfiguration = AppConfiguration(),
    lifecycle: AppLifecycleObserver? = null,
  ): AppServices {
    val appContext = context.applicationContext
    val info = AndroidAppInfo(
      context = appContext,
      channel = configuration.channel,
      environmentOverride = configuration.environment,
    )
    val analytics = BufferedAnalyticsTracker(configuration).also {
      it.setCommonParametersProvider { info.commonParameters() }
    }
    return AppServices(
      info = info,
      versionChecker = DefaultVersionChecker(info),
      deeplinks = DefaultDeeplinkRouter(),
      analytics = analytics,
      startup = StartupTaskManager(),
      lifecycle = lifecycle ?: ProcessLifecycleAppObserver(),
    )
  }

  /** In-memory collaborators for tests and offline samples. */
  fun mock(
    metadata: AppMetadata = AppMetadata(
      packageName = "com.fk.sample",
      versionName = "0.1.0",
      versionCode = 1L,
      channel = "mock",
      environment = AppBuildEnvironment.Debug,
    ),
    lifecycle: AppLifecycleObserver = MockAppLifecycleObserver(),
  ): AppServices {
    val info = object : AppInfo {
      override val packageName: String = metadata.packageName
      override val versionName: String = metadata.versionName
      override val versionCode: Long = metadata.versionCode
      override val sdkInt: Int = android.os.Build.VERSION.SDK_INT
      override val deviceModel: String = "mock"
      override val channel: String = metadata.channel
      override val environment: AppBuildEnvironment = metadata.environment
    }
    val analytics = BufferedAnalyticsTracker().also {
      it.setCommonParametersProvider { info.commonParameters() }
    }
    return AppServices(
      info = info,
      versionChecker = DefaultVersionChecker(info),
      deeplinks = DefaultDeeplinkRouter(),
      analytics = analytics,
      startup = StartupTaskManager(),
      lifecycle = lifecycle,
    )
  }
}
