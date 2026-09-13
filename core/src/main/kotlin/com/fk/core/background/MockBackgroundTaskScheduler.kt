package com.fk.core.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory [BackgroundTaskScheduling] + [BackgroundWorkExtending] for tests and samples.
 *
 * Conceptually aligned with iOS `FKMockBackgroundTaskScheduler`.
 */
class MockBackgroundTaskScheduler : BackgroundTaskScheduling, BackgroundWorkExtending {

  private data class Entry(
    val kind: BackgroundTaskKind,
    val handler: BackgroundTaskHandler,
  )

  private val lock = Any()
  private val registry = linkedMapOf<String, Entry>()
  private val refresh = mutableListOf<BackgroundAppRefreshRequest>()
  private val processing = mutableListOf<BackgroundProcessingRequest>()
  private val workStates = ConcurrentHashMap<String, MutableStateFlow<BackgroundWorkInfo?>>()
  private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @Volatile
  private var installed: Boolean = false

  /** Optional override invoked by [simulateLaunch] instead of the registered handler. */
  @Volatile
  var simulateHandler: (suspend (String) -> Boolean)? = null

  /** Submitted app refresh requests in schedule order. */
  fun scheduledRefresh(): List<BackgroundAppRefreshRequest> =
    synchronized(lock) { refresh.toList() }

  /** Submitted processing requests in schedule order. */
  fun scheduledProcessing(): List<BackgroundProcessingRequest> =
    synchronized(lock) { processing.toList() }

  /** Marks installation complete without validating registrations. */
  fun markInstalled() {
    installed = true
  }

  /**
   * Validates registrations and marks installation complete.
   *
   * @throws BackgroundTaskError.UnregisteredIdentifier when a descriptor has no handler.
   */
  fun installRegistrations(registrations: List<BackgroundTaskRegistration>) {
    synchronized(lock) {
      for (registration in registrations) {
        val entry = registry[registration.identifier]
          ?: throw BackgroundTaskError.UnregisteredIdentifier(registration.identifier)
        if (entry.kind != registration.kind) {
          throw BackgroundTaskError.UnregisteredIdentifier(registration.identifier)
        }
      }
      installed = true
    }
  }

  override fun registerAppRefresh(
    identifier: String,
    handler: BackgroundTaskHandler,
  ) {
    register(identifier, BackgroundTaskKind.AppRefresh, handler)
  }

  override fun registerProcessing(
    identifier: String,
    handler: BackgroundTaskHandler,
  ) {
    register(identifier, BackgroundTaskKind.Processing, handler)
  }

  override suspend fun scheduleAppRefresh(request: BackgroundAppRefreshRequest) {
    ensureCanSchedule(request.identifier, BackgroundTaskKind.AppRefresh)
    synchronized(lock) { refresh += request }
    emitState(
      BackgroundWorkInfo(
        identifier = request.identifier,
        state = BackgroundWorkState.Enqueued,
      ),
    )
  }

  override suspend fun scheduleProcessing(request: BackgroundProcessingRequest) {
    ensureCanSchedule(request.identifier, BackgroundTaskKind.Processing)
    synchronized(lock) { processing += request }
    emitState(
      BackgroundWorkInfo(
        identifier = request.identifier,
        state = BackgroundWorkState.Enqueued,
      ),
    )
  }

  override suspend fun cancelScheduledTask(identifier: String) {
    synchronized(lock) {
      if (!installed) throw BackgroundTaskError.NotInstalled()
      if (registry[identifier] == null) {
        throw BackgroundTaskError.UnregisteredIdentifier(identifier)
      }
      refresh.removeAll { it.identifier == identifier }
      processing.removeAll { it.identifier == identifier }
    }
    emitState(
      BackgroundWorkInfo(
        identifier = identifier,
        state = BackgroundWorkState.Cancelled,
      ),
    )
  }

  override fun observeWorkInfo(identifier: String): Flow<BackgroundWorkInfo?> =
    stateFlow(identifier)

  override suspend fun pendingSummaries(): List<BackgroundTaskPendingSummary> =
    synchronized(lock) {
      val fromRefresh = refresh.map {
        BackgroundTaskPendingSummary(
          identifier = it.identifier,
          kind = BackgroundTaskKind.AppRefresh,
          earliestBeginEpochMs = it.earliestBeginEpochMs,
          state = BackgroundWorkState.Enqueued,
        )
      }
      val fromProcessing = processing.map {
        BackgroundTaskPendingSummary(
          identifier = it.identifier,
          kind = BackgroundTaskKind.Processing,
          earliestBeginEpochMs = it.earliestBeginEpochMs,
          requiresNetworkConnectivity = it.requiresNetworkConnectivity,
          requiresCharging = it.requiresCharging,
          state = BackgroundWorkState.Enqueued,
        )
      }
      fromRefresh + fromProcessing
    }

  override fun beginBackgroundWork(
    name: String?,
    work: suspend () -> Unit,
  ): BackgroundWorkToken {
    val job = workScope.launch {
      work()
    }
    return BackgroundWorkToken(job)
  }

  /**
   * Manually invokes a registered handler as if WorkManager launched the task.
   *
   * @param simulateExpiration When true, marks the handle expired before the handler runs.
   */
  suspend fun simulateLaunch(
    identifier: String,
    simulateExpiration: Boolean = false,
  ): Boolean {
    val entry = synchronized(lock) { registry[identifier] }
    val override = simulateHandler
    val handle = BackgroundTaskHandle(identifier)
    if (simulateExpiration) {
      handle.markExpired()
    }

    emitState(
      BackgroundWorkInfo(
        identifier = identifier,
        state = BackgroundWorkState.Running,
      ),
    )

    val success = when {
      override != null -> override(identifier) && !handle.isExpired
      entry != null -> {
        val result = entry.handler.invoke(handle)
        if (!handle.isCompleted) handle.complete(result)
        (handle.lastSuccess ?: result) && !handle.isExpired
      }
      else -> false
    }

    synchronized(lock) {
      refresh.removeAll { it.identifier == identifier }
      processing.removeAll { it.identifier == identifier }
    }

    emitState(
      BackgroundWorkInfo(
        identifier = identifier,
        state = if (success) BackgroundWorkState.Succeeded else BackgroundWorkState.Failed,
      ),
    )
    return success
  }

  private fun register(
    identifier: String,
    kind: BackgroundTaskKind,
    handler: BackgroundTaskHandler,
  ) {
    synchronized(lock) {
      val existing = registry[identifier]
      if (existing != null && existing.kind != kind) {
        throw BackgroundTaskError.DuplicateRegistration(identifier)
      }
      registry[identifier] = Entry(kind, handler)
    }
  }

  private fun ensureCanSchedule(
    identifier: String,
    expectedKind: BackgroundTaskKind,
  ) {
    synchronized(lock) {
      if (!installed) throw BackgroundTaskError.NotInstalled()
      val entry = registry[identifier]
        ?: throw BackgroundTaskError.UnregisteredIdentifier(identifier)
      if (entry.kind != expectedKind) {
        throw BackgroundTaskError.UnregisteredIdentifier(identifier)
      }
    }
  }

  private fun stateFlow(identifier: String): MutableStateFlow<BackgroundWorkInfo?> =
    workStates.getOrPut(identifier) { MutableStateFlow(null) }

  private fun emitState(info: BackgroundWorkInfo) {
    stateFlow(info.identifier).value = info
  }
}
