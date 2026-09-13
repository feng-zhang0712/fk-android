package com.fk.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Fires a pending local notification when an [AlarmManager] alarm elapses.
 */
class LocalNotificationAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != LocalNotificationPoster.ACTION_FIRE) return
    val identifier = intent.getStringExtra(LocalNotificationPoster.EXTRA_REQUEST_ID) ?: return
    val appContext = context.applicationContext
    val store = LocalNotificationStore(appContext)
    val pending = store.get(identifier) ?: return
    val categories = LocalNotificationCategoryRegistry.snapshot()

    try {
      LocalNotificationPoster.post(appContext, pending.request, categories)
    } catch (_: SecurityException) {
      store.remove(identifier)
      return
    }

    when (val trigger = pending.request.trigger) {
      is LocalNotificationTrigger.TimeInterval -> {
        if (trigger.repeats) {
          val next = System.currentTimeMillis() + trigger.delayMs
          store.upsert(pending.request, next)
          LocalNotificationAlarms.schedule(appContext, pending.request.identifier, next)
        } else {
          store.remove(identifier)
        }
      }
      is LocalNotificationTrigger.AtEpochMs -> {
        if (trigger.repeats) {
          val interval = trigger.repeatIntervalMs
          if (interval == null) {
            store.remove(identifier)
            return
          }
          val next = System.currentTimeMillis() + interval
          store.upsert(pending.request, next)
          LocalNotificationAlarms.schedule(appContext, pending.request.identifier, next)
        } else {
          store.remove(identifier)
        }
      }
      LocalNotificationTrigger.Immediate -> store.remove(identifier)
    }
  }
}

/**
 * Restores alarm-backed pending notifications after device boot.
 */
class LocalNotificationBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    val appContext = context.applicationContext
    val store = LocalNotificationStore(appContext)
    val now = System.currentTimeMillis()
    for (entry in store.all()) {
      val fireAt = entry.nextFireEpochMs.coerceAtLeast(now + 1_000L)
      LocalNotificationAlarms.schedule(appContext, entry.request.identifier, fireAt)
    }
  }
}

/**
 * Delivers category action taps to an optional in-process handler.
 */
class LocalNotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != LocalNotificationPoster.ACTION_CATEGORY_ACTION) return
    val requestId = intent.getStringExtra(LocalNotificationPoster.EXTRA_REQUEST_ID) ?: return
    val actionId = intent.getStringExtra(LocalNotificationPoster.EXTRA_ACTION_ID) ?: return
    val userInfo = intent.extras
      ?.keySet()
      ?.filter { it.startsWith(LocalNotificationPoster.EXTRA_USER_INFO_PREFIX) }
      ?.associate { key ->
        key.removePrefix(LocalNotificationPoster.EXTRA_USER_INFO_PREFIX) to
          (intent.getStringExtra(key) ?: "")
      }
      .orEmpty()

    LocalNotificationResponseHub.dispatch(
      LocalNotificationResponse(
        requestIdentifier = requestId,
        actionIdentifier = actionId,
        userInfo = userInfo,
        isDefaultAction = false,
      ),
    )
  }
}

internal object LocalNotificationAlarms {
  /** Returns `false` when [AlarmManager] is unavailable. */
  fun schedule(context: Context, identifier: String, triggerAtEpochMs: Long): Boolean {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
    val pending = pendingIntent(context, identifier)
    val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
    if (canExact) {
      alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtEpochMs,
        pending,
      )
    } else {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtEpochMs,
        pending,
      )
    }
    return true
  }

  fun cancel(context: Context, identifier: String) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    alarmManager.cancel(pendingIntent(context, identifier))
  }

  private fun pendingIntent(context: Context, identifier: String): PendingIntent {
    val intent = Intent(context, LocalNotificationAlarmReceiver::class.java).apply {
      action = LocalNotificationPoster.ACTION_FIRE
      putExtra(LocalNotificationPoster.EXTRA_REQUEST_ID, identifier)
    }
    return PendingIntent.getBroadcast(
      context,
      LocalNotificationPoster.notificationId(identifier),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}

internal object LocalNotificationCategoryRegistry {
  private val lock = Any()
  private val categories = linkedMapOf<String, LocalNotificationCategory>()

  fun register(list: List<LocalNotificationCategory>) {
    synchronized(lock) {
      list.forEach { categories[it.identifier] = it }
    }
  }

  fun snapshot(): Map<String, LocalNotificationCategory> =
    synchronized(lock) { categories.toMap() }
}

/**
 * User interaction response for a delivered local notification.
 */
data class LocalNotificationResponse(
  val requestIdentifier: String,
  val actionIdentifier: String,
  val userInfo: Map<String, String>,
  val isDefaultAction: Boolean,
)

fun interface LocalNotificationResponseHandler {
  fun onResponse(response: LocalNotificationResponse)
}

internal object LocalNotificationResponseHub {
  @Volatile
  private var handler: LocalNotificationResponseHandler? = null

  fun setHandler(handler: LocalNotificationResponseHandler?) {
    this.handler = handler
  }

  fun dispatch(response: LocalNotificationResponse) {
    handler?.onResponse(response)
  }
}
