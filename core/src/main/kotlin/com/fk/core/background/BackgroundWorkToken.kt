package com.fk.core.background

import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Token returned synchronously from [BackgroundWorkExtending.beginBackgroundWork].
 *
 * Conceptually aligned with iOS `FKBackgroundWorkToken`.
 */
class BackgroundWorkToken internal constructor(
  private val job: Job,
) {
  private val ended = AtomicBoolean(false)

  /** Whether the underlying job is still active. */
  val isValid: Boolean
    get() = !ended.get() && job.isActive

  /** Ends the in-process work. Idempotent; cancels the job if still running. */
  fun end() {
    if (!ended.compareAndSet(false, true)) return
    if (job.isActive) job.cancel()
  }
}
