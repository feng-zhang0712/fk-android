package com.fk.core.background

import kotlinx.coroutines.flow.Flow

/**
 * Pluggable contract for deferred background task registration and scheduling.
 *
 * Conceptually aligned with iOS `FKBackgroundTaskScheduling`; Android engine is WorkManager.
 */
interface BackgroundTaskScheduling {
  /**
   * Registers an app-refresh handler.
   *
   * Replaces an existing handler when the kind matches; throws
   * [BackgroundTaskError.DuplicateRegistration] when the kind conflicts.
   */
  fun registerAppRefresh(
    identifier: String,
    handler: BackgroundTaskHandler,
  )

  /**
   * Registers a processing handler.
   *
   * Replaces an existing handler when the kind matches; throws
   * [BackgroundTaskError.DuplicateRegistration] when the kind conflicts.
   */
  fun registerProcessing(
    identifier: String,
    handler: BackgroundTaskHandler,
  )

  /**
   * Enqueues a unique refresh worker for [request].
   *
   * @throws BackgroundTaskError.NotInstalled / [BackgroundTaskError.UnregisteredIdentifier]
   */
  suspend fun scheduleAppRefresh(request: BackgroundAppRefreshRequest)

  /**
   * Enqueues a unique processing worker for [request].
   *
   * @throws BackgroundTaskError.NotInstalled / [BackgroundTaskError.UnregisteredIdentifier]
   */
  suspend fun scheduleProcessing(request: BackgroundProcessingRequest)

  /**
   * Cancels pending / running unique work for [identifier].
   *
   * @throws BackgroundTaskError.NotInstalled / [BackgroundTaskError.UnregisteredIdentifier]
   */
  suspend fun cancelScheduledTask(identifier: String)

  /**
   * Observes WorkManager state for the unique work named [identifier].
   *
   * Emits `null` when no work infos exist yet.
   */
  fun observeWorkInfo(identifier: String): Flow<BackgroundWorkInfo?>

  /** Pending / running unique-work summaries (debug / sample). */
  suspend fun pendingSummaries(): List<BackgroundTaskPendingSummary>
}
