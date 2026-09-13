package com.fk.core.notification

/**
 * Shared request validation for [LocalNotificationManager] and [MockLocalNotificationScheduler].
 */
internal object LocalNotificationValidator {
  private const val MIN_REPEAT_MS: Long = 60_000L

  fun validate(request: LocalNotificationRequest) {
    if (request.identifier.isBlank()) {
      throw LocalNotificationError.InvalidContent("identifier must not be blank")
    }
    if (request.channelId.isBlank()) {
      throw LocalNotificationError.InvalidContent("channelId must not be blank")
    }
    if (request.content.title.isBlank() && request.content.body.isBlank()) {
      throw LocalNotificationError.InvalidContent("title and body must not both be blank")
    }
    when (val trigger = request.trigger) {
      LocalNotificationTrigger.Immediate -> Unit
      is LocalNotificationTrigger.TimeInterval -> {
        if (trigger.delayMs < 0L) {
          throw LocalNotificationError.InvalidTrigger("delayMs must be ≥ 0")
        }
        if (trigger.repeats && trigger.delayMs < MIN_REPEAT_MS) {
          throw LocalNotificationError.InvalidTrigger(
            "repeating timeInterval must be ≥ ${MIN_REPEAT_MS}ms",
          )
        }
      }
      is LocalNotificationTrigger.AtEpochMs -> {
        if (trigger.repeats) {
          val interval = trigger.repeatIntervalMs
            ?: throw LocalNotificationError.InvalidTrigger(
              "repeatIntervalMs required when repeats=true",
            )
          if (interval < MIN_REPEAT_MS) {
            throw LocalNotificationError.InvalidTrigger(
              "repeatIntervalMs must be ≥ ${MIN_REPEAT_MS}ms",
            )
          }
        }
      }
    }
  }
}
