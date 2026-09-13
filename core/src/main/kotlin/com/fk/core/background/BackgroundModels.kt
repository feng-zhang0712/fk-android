package com.fk.core.background

/**
 * Background task kind — maps conceptually to iOS `BGAppRefreshTask` / `BGProcessingTask`.
 *
 * On Android both kinds enqueue WorkManager work; [Processing] may attach network / charging constraints.
 */
enum class BackgroundTaskKind {
  /** Lightweight deferred refresh (config sync, small payloads). */
  AppRefresh,

  /** Heavier deferred work; constraints are set at schedule time. */
  Processing,
}

/**
 * Descriptor installed at app bootstrap via [BackgroundTaskManager.installRegistrations].
 *
 * @property identifier Unique work name (also used as WorkManager unique work id).
 * @property kind Refresh vs processing registration.
 */
data class BackgroundTaskRegistration(
  val identifier: String,
  val kind: BackgroundTaskKind,
)

/**
 * Request to schedule an app-refresh style worker.
 *
 * @property identifier Must match a registered [BackgroundTaskKind.AppRefresh] handler.
 * @property earliestBeginEpochMs Earliest start time (epoch millis); `null` = as soon as allowed.
 */
data class BackgroundAppRefreshRequest(
  val identifier: String,
  val earliestBeginEpochMs: Long? = null,
)

/**
 * Request to schedule a processing-style worker.
 *
 * @property identifier Must match a registered [BackgroundTaskKind.Processing] handler.
 * @property earliestBeginEpochMs Earliest start time (epoch millis); `null` = as soon as allowed.
 * @property requiresNetworkConnectivity When true, waits for a connected network.
 * @property requiresCharging When true, waits for the device to be charging (power analog of iOS external power).
 */
data class BackgroundProcessingRequest(
  val identifier: String,
  val earliestBeginEpochMs: Long? = null,
  val requiresNetworkConnectivity: Boolean = false,
  val requiresCharging: Boolean = false,
)

/**
 * Runtime knobs for [BackgroundTaskManager].
 *
 * @property allowsMultipleInstall When false, a second [BackgroundTaskManager.installRegistrations] throws.
 * @property logScheduling Emits simple Logcat lines for schedule / cancel / complete when true.
 */
data class BackgroundTaskConfiguration(
  val allowsMultipleInstall: Boolean = false,
  val logScheduling: Boolean = false,
)

/**
 * Readable summary of a pending / running unique work request (debug / sample).
 */
data class BackgroundTaskPendingSummary(
  val identifier: String,
  val kind: BackgroundTaskKind,
  val earliestBeginEpochMs: Long?,
  val requiresNetworkConnectivity: Boolean = false,
  val requiresCharging: Boolean = false,
  val state: BackgroundWorkState = BackgroundWorkState.Enqueued,
)

/** Coarse WorkManager state for observation and summaries. */
enum class BackgroundWorkState {
  Enqueued,
  Running,
  Succeeded,
  Failed,
  Blocked,
  Cancelled,
}

/**
 * Snapshot of unique work for [identifier].
 *
 * @property workId WorkManager UUID string when available.
 */
data class BackgroundWorkInfo(
  val identifier: String,
  val state: BackgroundWorkState,
  val workId: String? = null,
  val attemptCount: Int = 0,
)

/** Handler invoked when WorkManager runs a registered task. Returns `true` on success. */
fun interface BackgroundTaskHandler {
  suspend fun invoke(handle: BackgroundTaskHandle): Boolean
}

/**
 * Stable error taxonomy for background scheduling.
 *
 * Conceptually aligned with iOS `FKBackgroundTaskError` (without plist-permit cases).
 */
sealed class BackgroundTaskError(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  /** Schedule / cancel for an identifier that was never registered. */
  class UnregisteredIdentifier(val identifier: String) :
    BackgroundTaskError("Unregistered background task identifier: $identifier")

  /** Register attempted with a conflicting kind for an existing identifier. */
  class DuplicateRegistration(val identifier: String) :
    BackgroundTaskError("Conflicting background task registration: $identifier")

  /** [BackgroundTaskManager.installRegistrations] was called more than once. */
  class AlreadyInstalled :
    BackgroundTaskError("Background task registrations are already installed")

  /** Schedule / cancel before [BackgroundTaskManager.installRegistrations]. */
  class NotInstalled :
    BackgroundTaskError("Background task registrations are not installed")

  /** WorkManager enqueue / cancel failed. */
  class SchedulingFailed(cause: Throwable? = null) :
    BackgroundTaskError("Background task scheduling failed", cause)
}
