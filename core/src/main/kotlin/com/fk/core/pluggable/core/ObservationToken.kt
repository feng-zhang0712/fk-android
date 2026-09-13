package com.fk.core.pluggable.core

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cancellation handle returned by observable pluggable services.
 *
 * Retain the token for the lifetime of the subscription; call [cancel] (or let
 * the token be GC'd after cancel-on-close patterns) to unregister.
 */
class ObservationToken(
  private val onCancel: () -> Unit,
) {
  private val cancelled = AtomicBoolean(false)

  /** Unregisters the observer if not already cancelled. */
  fun cancel() {
    if (cancelled.compareAndSet(false, true)) {
      onCancel()
    }
  }
}
