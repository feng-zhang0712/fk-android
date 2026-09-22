package com.fk.core.background

import android.content.Context

/**
 * Background — WorkManager scheduling façade (refresh / processing / short in-process work).
 *
 * Conceptually aligned with iOS `FKCoreKit` BackgroundTask; Android Jetpack WorkManager.
 */
object Background {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Builds the default [BackgroundTaskManager] implementation. */
  fun create(
    context: Context,
    configuration: BackgroundTaskConfiguration = BackgroundTaskConfiguration(),
  ): BackgroundTaskManager =
    BackgroundTaskManager(
      context = context,
      configuration = configuration,
    )

  /** Builds an in-memory mock for tests and samples. */
  fun mock(): MockBackgroundTaskScheduler = MockBackgroundTaskScheduler()
}
