package com.fk.core.background

/**
 * Pluggable contract for short-lived in-process work after the app backgrounds.
 *
 * Conceptually aligned with iOS `FKBackgroundWorkExtending` / `beginBackgroundTask`.
 * Android does **not** grant an extra execution budget; prefer [BackgroundTaskScheduling]
 * for deferred work that must survive process death.
 */
interface BackgroundWorkExtending {
  /**
   * Starts [work] asynchronously and returns a [BackgroundWorkToken] immediately.
   *
   * Call [BackgroundWorkToken.end] when finished (idempotent). The token becomes invalid
   * if the coroutine completes or is cancelled.
   */
  fun beginBackgroundWork(
    name: String? = null,
    work: suspend () -> Unit,
  ): BackgroundWorkToken
}
