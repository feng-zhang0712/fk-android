package com.fk.core.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default [LocalNotificationScheduling] implementation over NotificationManager + AlarmManager.
 *
 * Conceptually aligned with iOS `FKLocalNotificationManager`.
 * Never prompts for permission — use [com.fk.core.permissions.Permissions] first.
 */
class LocalNotificationManager(
  context: Context,
  private val configuration: LocalNotificationConfiguration = LocalNotificationConfiguration(),
) : LocalNotificationScheduling {

  private val appContext = context.applicationContext
  private val store = LocalNotificationStore(appContext)

  init {
    configuration.defaultChannel?.let { ensureChannel(it) }
  }

  /** Creates the notification channel if it does not already exist (no-op below Android O). */
  fun ensureChannel(channel: LocalNotificationChannel) {
    LocalNotificationPoster.ensureChannel(appContext, channel)
  }

  /** Registers categories whose actions are attached when notifications are posted. */
  fun registerCategories(categories: List<LocalNotificationCategory>) {
    LocalNotificationCategoryRegistry.register(categories)
  }

  /** Optional handler for category action taps (invoked in-process when the receiver runs). */
  fun setResponseHandler(handler: LocalNotificationResponseHandler?) {
    LocalNotificationResponseHub.setHandler(handler)
  }

  /**
   * Returns whether scheduling is allowed without prompting.
   *
   * Checks [NotificationManagerCompat.areNotificationsEnabled] and, on API 33+,
   * [Manifest.permission.POST_NOTIFICATIONS].
   */
  fun canScheduleNotifications(): Boolean {
    if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
      return false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return true
  }

  /** Returns summaries of alarm-backed pending requests managed by this façade. */
  suspend fun pendingRequests(): List<LocalNotificationPendingSummary> =
    withContext(Dispatchers.IO) {
      store.all().map { entry ->
        LocalNotificationPendingSummary(
          identifier = entry.request.identifier,
          content = entry.request.content,
          triggerDescription = entry.request.trigger.describe(),
          channelId = entry.request.channelId,
          nextFireEpochMs = entry.nextFireEpochMs,
        )
      }
    }

  override suspend fun schedule(request: LocalNotificationRequest) {
    withContext(Dispatchers.IO) {
      LocalNotificationValidator.validate(request)
      if (!canScheduleNotifications()) {
        logFailure(LocalNotificationError.NotAuthorized())
        throw LocalNotificationError.NotAuthorized()
      }

      ensureChannelForRequest(request.channelId)

      when (val trigger = request.trigger) {
        LocalNotificationTrigger.Immediate -> {
          LocalNotificationAlarms.cancel(appContext, request.identifier)
          store.remove(request.identifier)
          try {
            LocalNotificationPoster.post(
              appContext,
              request,
              LocalNotificationCategoryRegistry.snapshot(),
            )
          } catch (e: SecurityException) {
            val error = LocalNotificationError.NotAuthorized()
            logFailure(error)
            throw error
          }
        }
        is LocalNotificationTrigger.TimeInterval -> {
          val fireAt = System.currentTimeMillis() + trigger.delayMs
          scheduleAlarm(request, fireAt)
        }
        is LocalNotificationTrigger.AtEpochMs -> {
          val fireAt = trigger.epochMs.coerceAtLeast(System.currentTimeMillis())
          scheduleAlarm(request, fireAt)
        }
      }
    }
  }

  override suspend fun cancelPending(identifier: String) {
    withContext(Dispatchers.IO) {
      LocalNotificationAlarms.cancel(appContext, identifier)
      store.remove(identifier)
    }
  }

  override suspend fun cancelAllPending() {
    withContext(Dispatchers.IO) {
      store.all().forEach { entry ->
        LocalNotificationAlarms.cancel(appContext, entry.request.identifier)
      }
      store.clear()
    }
  }

  override suspend fun removeDelivered(identifier: String) {
    withContext(Dispatchers.Main.immediate) {
      LocalNotificationPoster.cancelDelivered(appContext, identifier)
    }
  }

  override suspend fun removeAllDelivered() {
    withContext(Dispatchers.Main.immediate) {
      LocalNotificationPoster.cancelAllDelivered(appContext)
    }
  }

  private fun scheduleAlarm(request: LocalNotificationRequest, fireAt: Long) {
    LocalNotificationAlarms.cancel(appContext, request.identifier)
    store.upsert(request, fireAt)
    if (!LocalNotificationAlarms.schedule(appContext, request.identifier, fireAt)) {
      store.remove(request.identifier)
      val error = LocalNotificationError.SystemError("AlarmManager unavailable")
      logFailure(error)
      throw error
    }
  }

  private fun ensureChannelForRequest(channelId: String) {
    if (channelExists(channelId)) return
    val default = configuration.defaultChannel
    val channel = if (default != null && default.id == channelId) {
      default
    } else {
      LocalNotificationChannel(
        id = channelId,
        name = channelId,
        importance = LocalNotificationImportance.Default,
      )
    }
    ensureChannel(channel)
  }

  private fun channelExists(channelId: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val manager = appContext.getSystemService(android.app.NotificationManager::class.java)
      ?: return false
    return manager.getNotificationChannel(channelId) != null
  }

  private fun logFailure(error: LocalNotificationError) {
    if (configuration.logSchedulingFailures) {
      Log.d(TAG, "Local notification scheduling failed: ${error.message}")
    }
  }

  companion object {
    private const val TAG: String = "FkLocalNotification"
  }
}

private fun LocalNotificationTrigger.describe(): String =
  when (this) {
    LocalNotificationTrigger.Immediate -> "immediate"
    is LocalNotificationTrigger.TimeInterval ->
      "timeInterval(delayMs=$delayMs, repeats=$repeats)"
    is LocalNotificationTrigger.AtEpochMs ->
      "atEpochMs(epochMs=$epochMs, repeats=$repeats, repeatIntervalMs=$repeatIntervalMs)"
  }
