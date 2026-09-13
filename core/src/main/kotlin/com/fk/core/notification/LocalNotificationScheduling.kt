package com.fk.core.notification

/**
 * Pluggable contract for local notification scheduling.
 *
 * Conceptually aligned with iOS `FKLocalNotificationScheduling`.
 * Request notification permission via [com.fk.core.permissions.Permissions] before scheduling —
 * this contract never prompts.
 */
interface LocalNotificationScheduling {
  /** Schedules a single local notification request. */
  suspend fun schedule(request: LocalNotificationRequest)

  /** Schedules multiple requests; throws on the first failure. */
  suspend fun schedule(requests: List<LocalNotificationRequest>) {
    for (request in requests) {
      schedule(request)
    }
  }

  /** Cancels a pending notification by identifier. */
  suspend fun cancelPending(identifier: String)

  /** Cancels multiple pending notifications by identifier. */
  suspend fun cancelPending(identifiers: List<String>) {
    for (identifier in identifiers) {
      cancelPending(identifier)
    }
  }

  /** Cancels all pending local notifications managed by this façade. */
  suspend fun cancelAllPending()

  /** Removes a delivered notification from the shade by identifier. */
  suspend fun removeDelivered(identifier: String)

  /** Removes multiple delivered notifications by identifier. */
  suspend fun removeDelivered(identifiers: List<String>) {
    for (identifier in identifiers) {
      removeDelivered(identifier)
    }
  }

  /** Removes all delivered notifications for this app. */
  suspend fun removeAllDelivered()
}
