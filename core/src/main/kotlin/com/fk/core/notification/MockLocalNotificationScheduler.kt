package com.fk.core.notification

/**
 * In-memory [LocalNotificationScheduling] for tests and samples.
 *
 * Conceptually aligned with iOS `FKMockLocalNotificationScheduler`.
 */
class MockLocalNotificationScheduler(
  var authorizationGranted: Boolean = true,
) : LocalNotificationScheduling {

  private val lock = Any()
  private val pending = linkedMapOf<String, LocalNotificationRequest>()
  private val delivered = linkedMapOf<String, LocalNotificationRequest>()

  /** Error thrown on the next schedule attempt when set. */
  @Volatile
  var shouldThrow: LocalNotificationError? = null

  /** Optional response handler for [simulateResponse]. */
  @Volatile
  var responseHandler: LocalNotificationResponseHandler? = null

  /** Scheduled requests keyed by identifier (most recent schedule wins). */
  fun scheduled(): List<LocalNotificationRequest> =
    synchronized(lock) { pending.values.toList() }

  fun delivered(): List<LocalNotificationRequest> =
    synchronized(lock) { delivered.values.toList() }

  override suspend fun schedule(request: LocalNotificationRequest) {
    shouldThrow?.let { throw it }
    if (!authorizationGranted) throw LocalNotificationError.NotAuthorized()
    LocalNotificationValidator.validate(request)
    synchronized(lock) {
      pending[request.identifier] = request
      if (request.trigger is LocalNotificationTrigger.Immediate) {
        delivered[request.identifier] = request
        pending.remove(request.identifier)
      }
    }
  }

  override suspend fun cancelPending(identifier: String) {
    synchronized(lock) { pending.remove(identifier) }
  }

  override suspend fun cancelAllPending() {
    synchronized(lock) { pending.clear() }
  }

  override suspend fun removeDelivered(identifier: String) {
    synchronized(lock) { delivered.remove(identifier) }
  }

  override suspend fun removeAllDelivered() {
    synchronized(lock) { delivered.clear() }
  }

  /** Moves a pending request into delivered (simulates alarm fire). */
  fun simulateDelivery(identifier: String) {
    synchronized(lock) {
      val request = pending.remove(identifier) ?: return
      delivered[identifier] = request
    }
  }

  /** Simulates a user tap / action on a scheduled or delivered notification. */
  fun simulateResponse(
    requestIdentifier: String,
    actionIdentifier: String = "default",
    isDefaultAction: Boolean = true,
  ) {
    val snapshot = synchronized(lock) {
      pending[requestIdentifier] ?: delivered[requestIdentifier]
    } ?: return
    responseHandler?.onResponse(
      LocalNotificationResponse(
        requestIdentifier = requestIdentifier,
        actionIdentifier = actionIdentifier,
        userInfo = snapshot.content.userInfo,
        isDefaultAction = isDefaultAction,
      ),
    )
  }
}
