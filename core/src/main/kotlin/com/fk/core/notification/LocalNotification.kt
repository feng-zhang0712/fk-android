package com.fk.core.notification

import android.content.Context

/**
 * LocalNotification — NotificationManager + AlarmManager façade (channels / schedule / cancel).
 *
 * Conceptually aligned with iOS `FKCoreKit` LocalNotification; Android-shaped APIs.
 */
object LocalNotification {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Builds the default [LocalNotificationManager] implementation. */
  fun create(
    context: Context,
    configuration: LocalNotificationConfiguration = LocalNotificationConfiguration(),
  ): LocalNotificationManager =
    LocalNotificationManager(
      context = context,
      configuration = configuration,
    )

  /** Builds an in-memory mock for tests and samples. */
  fun mock(
    authorizationGranted: Boolean = true,
  ): MockLocalNotificationScheduler =
    MockLocalNotificationScheduler(authorizationGranted = authorizationGranted)
}
