package com.fk.core.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Android notification channel importance (mapped to [android.app.NotificationManager] constants).
 */
enum class LocalNotificationImportance {
  Min,
  Low,
  Default,
  High,
  Max,
}

/**
 * Channel descriptor ensured before posting (Android O+).
 *
 * @property id Stable channel id referenced by [LocalNotificationRequest.channelId].
 * @property name User-visible channel name.
 * @property importance Delivery importance.
 * @property description Optional channel description in system settings.
 */
data class LocalNotificationChannel(
  val id: String,
  val name: String,
  val importance: LocalNotificationImportance = LocalNotificationImportance.Default,
  val description: String? = null,
)

/** Sound played when the notification is delivered. */
@Serializable
enum class LocalNotificationSound {
  /** System default notification sound. */
  Default,

  /** Silent notification. */
  None,
}

/**
 * Local notification content.
 *
 * Conceptually aligned with iOS `FKLocalNotificationContent` (subset mapped to Android).
 */
@Serializable
data class LocalNotificationContent(
  val title: String,
  val body: String,
  val subtitle: String? = null,
  val sound: LocalNotificationSound = LocalNotificationSound.Default,
  val userInfo: Map<String, String> = emptyMap(),
  /** Group / thread key for notification bundling. */
  val groupKey: String? = null,
)

/**
 * Typed local notification trigger.
 *
 * Conceptually aligned with iOS `FKLocalNotificationTrigger`.
 */
@Serializable
sealed class LocalNotificationTrigger {
  /** Delivers immediately via [android.app.NotificationManager.notify]. */
  @Serializable
  @SerialName("immediate")
  data object Immediate : LocalNotificationTrigger()

  /**
   * Fires after [delayMs]; when [repeats] is true, reschedules with the same delay after delivery.
   *
   * Repeating intervals below 60_000 ms are rejected (matches iOS ≥ 60s rule for repeating).
   */
  @Serializable
  @SerialName("interval")
  data class TimeInterval(
    val delayMs: Long,
    val repeats: Boolean = false,
  ) : LocalNotificationTrigger()

  /**
   * Fires at [epochMs].
   *
   * When [repeats] is true, [repeatIntervalMs] must be ≥ 60_000 and is used to schedule the next fire.
   */
  @Serializable
  @SerialName("at")
  data class AtEpochMs(
    val epochMs: Long,
    val repeats: Boolean = false,
    val repeatIntervalMs: Long? = null,
  ) : LocalNotificationTrigger()
}

/**
 * Input model for scheduling a local notification.
 *
 * Re-scheduling with the same [identifier] replaces any existing pending request.
 */
@Serializable
data class LocalNotificationRequest(
  val identifier: String,
  val content: LocalNotificationContent,
  val trigger: LocalNotificationTrigger,
  val channelId: String,
  val categoryIdentifier: String? = null,
)

/** Summary of a pending (alarm-backed) local notification. */
data class LocalNotificationPendingSummary(
  val identifier: String,
  val content: LocalNotificationContent,
  val triggerDescription: String,
  val channelId: String,
  val nextFireEpochMs: Long?,
)

/** Action button attached to a [LocalNotificationCategory]. */
data class LocalNotificationAction(
  val identifier: String,
  val title: String,
)

/**
 * Category grouping custom action buttons.
 *
 * Registered categories are applied when the notification is posted.
 */
data class LocalNotificationCategory(
  val identifier: String,
  val actions: List<LocalNotificationAction> = emptyList(),
)

/** Standard `userInfo` keys for local notification payloads. */
object LocalNotificationUserInfoKey {
  const val DEEPLINK_URL: String = "fk.deeplink.url"
  const val ROUTE_ID: String = "fk.route.id"
  const val ANALYTICS_EVENT: String = "fk.analytics.event"
}

/**
 * Runtime knobs for [LocalNotificationManager].
 *
 * @property logSchedulingFailures Emits Logcat lines when scheduling fails.
 * @property defaultChannel Created at manager init (and reused when [LocalNotificationRequest.channelId] matches).
 */
data class LocalNotificationConfiguration(
  val logSchedulingFailures: Boolean = true,
  val defaultChannel: LocalNotificationChannel? = LocalNotificationChannel(
    id = DEFAULT_CHANNEL_ID,
    name = "General",
    importance = LocalNotificationImportance.Default,
  ),
) {
  companion object {
    const val DEFAULT_CHANNEL_ID: String = "fk.general"
  }
}

/**
 * Stable error taxonomy for local notification operations.
 *
 * Conceptually aligned with iOS `FKLocalNotificationError`.
 */
sealed class LocalNotificationError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  /** Notification permission / channel disabled; request via Permissions first. */
  class NotAuthorized :
    LocalNotificationError("Local notifications are not authorized")

  /** Trigger parameters are invalid. */
  class InvalidTrigger(detail: String) :
    LocalNotificationError("Invalid local notification trigger: $detail")

  /** Content parameters are invalid. */
  class InvalidContent(detail: String) :
    LocalNotificationError("Invalid local notification content: $detail")

  /** Underlying platform failure. */
  class SystemError(detail: String, cause: Throwable? = null) :
    LocalNotificationError("Local notification system error: $detail", cause)
}
