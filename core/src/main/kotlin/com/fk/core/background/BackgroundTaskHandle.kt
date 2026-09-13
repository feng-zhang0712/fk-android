package com.fk.core.background

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Execution context for one background handler invocation.
 *
 * Conceptually aligned with iOS `FKBackgroundTaskHandle`.
 *
 * @property identifier Registered task id for this run.
 */
class BackgroundTaskHandle internal constructor(
  val identifier: String,
  private val expiredProbe: () -> Boolean = { false },
) {
  private val completed = AtomicBoolean(false)
  private val expiredFlag = AtomicBoolean(false)

  @Volatile
  private var _lastSuccess: Boolean? = null

  /** Whether the system (or mock) has asked the handler to stop. */
  val isExpired: Boolean
    get() = expiredFlag.get() || expiredProbe()

  /** Last success passed to [complete], if any. */
  internal val lastSuccess: Boolean?
    get() = _lastSuccess

  /**
   * Marks the handle complete. Safe to call once; subsequent calls are ignored.
   *
   * WorkManager still uses the handler return value when [complete] was not called.
   */
  fun complete(success: Boolean) {
    if (completed.compareAndSet(false, true)) {
      _lastSuccess = success
    }
  }

  internal fun markExpired() {
    expiredFlag.set(true)
  }

  internal val isCompleted: Boolean
    get() = completed.get()
}
