package com.fk.core.background

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * Default [BackgroundTaskScheduling] + [BackgroundWorkExtending] implementation on WorkManager.
 *
 * Conceptually aligned with iOS `FKBackgroundTaskManager` (refresh / processing / short work),
 * without Apple `BGTaskScheduler` semantics.
 *
 * Call [installRegistrations] once after registering handlers (typically at application start).
 * Re-register handlers on every cold start before WorkManager may execute pending work.
 */
class BackgroundTaskManager(
  context: Context,
  private val configuration: BackgroundTaskConfiguration = BackgroundTaskConfiguration(),
  private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) : BackgroundTaskScheduling, BackgroundWorkExtending {

  private data class ScheduleMeta(
    val kind: BackgroundTaskKind,
    val earliestBeginEpochMs: Long?,
    val requiresNetwork: Boolean,
    val requiresCharging: Boolean,
  )

  private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val scheduleMeta = ConcurrentHashMap<String, ScheduleMeta>()

  /**
   * Validates that every registration has a matching handler and marks installation complete.
   *
   * @throws BackgroundTaskError.AlreadyInstalled / [BackgroundTaskError.UnregisteredIdentifier]
   */
  fun installRegistrations(registrations: List<BackgroundTaskRegistration>) {
    BackgroundTaskRegistry.install(registrations, configuration.allowsMultipleInstall)
    log("Installed ${registrations.size} background task registration(s)")
  }

  override fun registerAppRefresh(
    identifier: String,
    handler: BackgroundTaskHandler,
  ) {
    BackgroundTaskRegistry.register(identifier, BackgroundTaskKind.AppRefresh, handler)
  }

  override fun registerProcessing(
    identifier: String,
    handler: BackgroundTaskHandler,
  ) {
    BackgroundTaskRegistry.register(identifier, BackgroundTaskKind.Processing, handler)
  }

  override suspend fun scheduleAppRefresh(request: BackgroundAppRefreshRequest) {
    BackgroundTaskRegistry.ensureInstalled()
    BackgroundTaskRegistry.require(request.identifier, BackgroundTaskKind.AppRefresh)
    enqueue(
      identifier = request.identifier,
      kind = BackgroundTaskKind.AppRefresh,
      earliestBeginEpochMs = request.earliestBeginEpochMs,
      requiresNetwork = false,
      requiresCharging = false,
    )
    log("Scheduled app refresh '${request.identifier}'")
  }

  override suspend fun scheduleProcessing(request: BackgroundProcessingRequest) {
    BackgroundTaskRegistry.ensureInstalled()
    BackgroundTaskRegistry.require(request.identifier, BackgroundTaskKind.Processing)
    enqueue(
      identifier = request.identifier,
      kind = BackgroundTaskKind.Processing,
      earliestBeginEpochMs = request.earliestBeginEpochMs,
      requiresNetwork = request.requiresNetworkConnectivity,
      requiresCharging = request.requiresCharging,
    )
    log("Scheduled processing '${request.identifier}'")
  }

  override suspend fun cancelScheduledTask(identifier: String) {
    BackgroundTaskRegistry.ensureInstalled()
    if (BackgroundTaskRegistry.get(identifier) == null) {
      throw BackgroundTaskError.UnregisteredIdentifier(identifier)
    }
    try {
      workManager.cancelUniqueWork(identifier).await()
      scheduleMeta.remove(identifier)
    } catch (t: Throwable) {
      throw BackgroundTaskError.SchedulingFailed(t)
    }
    log("Cancelled scheduled task '$identifier'")
  }

  override fun observeWorkInfo(identifier: String): Flow<BackgroundWorkInfo?> =
    workManager.getWorkInfosForUniqueWorkFlow(identifier).map { infos ->
      infos.firstOrNull()?.toBackgroundWorkInfo(identifier)
    }

  override suspend fun pendingSummaries(): List<BackgroundTaskPendingSummary> {
    val kinds = BackgroundTaskRegistry.snapshotKinds()
    val summaries = mutableListOf<BackgroundTaskPendingSummary>()
    for ((identifier, kind) in kinds) {
      val infos = try {
        workManager.getWorkInfosForUniqueWork(identifier).awaitFuture()
      } catch (_: Throwable) {
        emptyList()
      }
      val meta = scheduleMeta[identifier]
      for (info in infos) {
        if (info.state.isFinished) continue
        summaries += BackgroundTaskPendingSummary(
          identifier = identifier,
          kind = meta?.kind ?: kind,
          earliestBeginEpochMs = meta?.earliestBeginEpochMs,
          requiresNetworkConnectivity = meta?.requiresNetwork ?: false,
          requiresCharging = meta?.requiresCharging ?: false,
          state = info.state.toBackgroundWorkState(),
        )
      }
    }
    return summaries
  }

  override fun beginBackgroundWork(
    name: String?,
    work: suspend () -> Unit,
  ): BackgroundWorkToken {
    val label = name?.takeIf { it.isNotBlank() } ?: "fk-background-work"
    val job = workScope.launch {
      try {
        work()
      } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
      } catch (t: Throwable) {
        log("beginBackgroundWork '$label' failed: ${t.message}")
      }
    }
    return BackgroundWorkToken(job)
  }

  private suspend fun enqueue(
    identifier: String,
    kind: BackgroundTaskKind,
    earliestBeginEpochMs: Long?,
    requiresNetwork: Boolean,
    requiresCharging: Boolean,
  ) {
    val delayMs = earliestBeginEpochMs?.let { max(0L, it - System.currentTimeMillis()) } ?: 0L
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(
        if (requiresNetwork) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED,
      )
      .setRequiresCharging(requiresCharging)
      .build()

    val request = OneTimeWorkRequestBuilder<BackgroundCoroutineWorker>()
      .setConstraints(constraints)
      .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
      .setInputData(
        workDataOf(
          BackgroundCoroutineWorker.KEY_IDENTIFIER to identifier,
          BackgroundCoroutineWorker.KEY_LOG to configuration.logScheduling,
        ),
      )
      .addTag(TAG_PREFIX + identifier)
      .build()

    try {
      workManager.enqueueUniqueWork(
        identifier,
        ExistingWorkPolicy.REPLACE,
        request,
      ).await()
      scheduleMeta[identifier] = ScheduleMeta(
        kind = kind,
        earliestBeginEpochMs = earliestBeginEpochMs,
        requiresNetwork = requiresNetwork,
        requiresCharging = requiresCharging,
      )
    } catch (t: Throwable) {
      throw BackgroundTaskError.SchedulingFailed(t)
    }
  }

  private fun log(message: String) {
    if (configuration.logScheduling) {
      Log.d(BackgroundCoroutineWorker.TAG, message)
    }
  }

  companion object {
    private const val TAG_PREFIX: String = "fk.background."
  }
}

private fun WorkInfo.toBackgroundWorkInfo(identifier: String): BackgroundWorkInfo =
  BackgroundWorkInfo(
    identifier = identifier,
    state = state.toBackgroundWorkState(),
    workId = id.toString(),
    attemptCount = runAttemptCount,
  )

private fun WorkInfo.State.toBackgroundWorkState(): BackgroundWorkState =
  when (this) {
    WorkInfo.State.ENQUEUED -> BackgroundWorkState.Enqueued
    WorkInfo.State.RUNNING -> BackgroundWorkState.Running
    WorkInfo.State.SUCCEEDED -> BackgroundWorkState.Succeeded
    WorkInfo.State.FAILED -> BackgroundWorkState.Failed
    WorkInfo.State.BLOCKED -> BackgroundWorkState.Blocked
    WorkInfo.State.CANCELLED -> BackgroundWorkState.Cancelled
  }

/** Await a Guava [ListenableFuture] without pulling kotlinx-coroutines-guava. */
private suspend fun <T> ListenableFuture<T>.awaitFuture(): T =
  suspendCancellableCoroutine { cont ->
    addListener(
      {
        try {
          cont.resume(get())
        } catch (e: ExecutionException) {
          cont.resumeWithException(e.cause ?: e)
        } catch (t: Throwable) {
          cont.resumeWithException(t)
        }
      },
      { it.run() },
    )
    cont.invokeOnCancellation { cancel(true) }
  }
