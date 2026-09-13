package com.fk.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Builds and posts notifications; ensures channels exist.
 */
internal object LocalNotificationPoster {

  fun ensureChannel(context: Context, channel: LocalNotificationChannel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val existing = manager.getNotificationChannel(channel.id)
    if (existing != null) return
    val androidChannel = NotificationChannel(
      channel.id,
      channel.name,
      channel.importance.toAndroidImportance(),
    ).apply {
      description = channel.description
    }
    manager.createNotificationChannel(androidChannel)
  }

  fun post(
    context: Context,
    request: LocalNotificationRequest,
    categories: Map<String, LocalNotificationCategory>,
  ) {
    val appContext = context.applicationContext
    val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
      ?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_REQUEST_ID, request.identifier)
        request.content.userInfo.forEach { (key, value) ->
          putExtra(EXTRA_USER_INFO_PREFIX + key, value)
        }
      }

    val contentIntent = launchIntent?.let {
      PendingIntent.getActivity(
        appContext,
        notificationId(request.identifier),
        it,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }

    val smallIcon = appContext.applicationInfo.icon.takeIf { it != 0 }
      ?: android.R.drawable.stat_notify_chat

    val builder = NotificationCompat.Builder(appContext, request.channelId)
      .setSmallIcon(smallIcon)
      .setContentTitle(request.content.title)
      .setContentText(request.content.body)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(
            buildString {
              request.content.subtitle?.let { append(it).append('\n') }
              append(request.content.body)
            },
          ),
      )
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(contentIntent)

    when (request.content.sound) {
      LocalNotificationSound.Default -> builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
      LocalNotificationSound.None -> builder.setSilent(true)
    }

    request.content.groupKey?.let { builder.setGroup(it) }

    val categoryId = request.categoryIdentifier
    if (categoryId != null) {
      val category = categories[categoryId]
      category?.actions?.forEach { categoryAction ->
        val actionIntent = Intent(appContext, LocalNotificationActionReceiver::class.java).apply {
          this.action = ACTION_CATEGORY_ACTION
          putExtra(EXTRA_REQUEST_ID, request.identifier)
          putExtra(EXTRA_ACTION_ID, categoryAction.identifier)
          request.content.userInfo.forEach { (key, value) ->
            putExtra(EXTRA_USER_INFO_PREFIX + key, value)
          }
        }
        val pending = PendingIntent.getBroadcast(
          appContext,
          (request.identifier + categoryAction.identifier).hashCode(),
          actionIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(0, categoryAction.title, pending)
      }
    }

    NotificationManagerCompat.from(appContext)
      .notify(request.identifier, notificationId(request.identifier), builder.build())
  }

  fun cancelDelivered(context: Context, identifier: String) {
    NotificationManagerCompat.from(context.applicationContext)
      .cancel(identifier, notificationId(identifier))
  }

  fun cancelAllDelivered(context: Context) {
    NotificationManagerCompat.from(context.applicationContext).cancelAll()
  }

  fun notificationId(identifier: String): Int = identifier.hashCode()

  const val EXTRA_REQUEST_ID: String = "fk.local_notification.request_id"
  const val EXTRA_ACTION_ID: String = "fk.local_notification.action_id"
  const val EXTRA_USER_INFO_PREFIX: String = "fk.local_notification.user_info."
  const val ACTION_FIRE: String = "com.fk.core.notification.ACTION_FIRE"
  const val ACTION_CATEGORY_ACTION: String = "com.fk.core.notification.ACTION_CATEGORY_ACTION"
}

private fun LocalNotificationImportance.toAndroidImportance(): Int =
  when (this) {
    LocalNotificationImportance.Min -> NotificationManager.IMPORTANCE_MIN
    LocalNotificationImportance.Low -> NotificationManager.IMPORTANCE_LOW
    LocalNotificationImportance.Default -> NotificationManager.IMPORTANCE_DEFAULT
    LocalNotificationImportance.High -> NotificationManager.IMPORTANCE_HIGH
    LocalNotificationImportance.Max -> NotificationManager.IMPORTANCE_MAX
  }
