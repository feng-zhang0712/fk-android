package com.fk.core.pluggable.core

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cancellation handle returned by observable pluggable services.
 *
 * Retain the token for the lifetime of the subscription and call [cancel]
 * (or [close]) to unregister. Tokens are **not** cancelled automatically by GC.
 */
class ObservationToken(
  private val onCancel: () -> Unit,
) : AutoCloseable {
  private val cancelled = AtomicBoolean(false)

  /** Unregisters the observer if not already cancelled. */
  fun cancel() {
    if (cancelled.compareAndSet(false, true)) {
      onCancel()
    }
  }

  /** Same as [cancel]; enables `use { }` and try-with-resources style cleanup. */
  override fun close() = cancel()
}
