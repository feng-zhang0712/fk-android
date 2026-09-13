package com.fk.core.pluggable.mock

import com.fk.core.pluggable.core.AppLifecycleObserver
import com.fk.core.pluggable.core.AppLifecycleState
import com.fk.core.pluggable.core.ObservationToken
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/** Mutable [AppLifecycleObserver] for samples and tests. */
class MockAppLifecycleObserver(
  initial: AppLifecycleState = AppLifecycleState.Active,
) : AppLifecycleObserver {
  private val current = AtomicReference(initial)
  private val listeners = CopyOnWriteArrayList<(AppLifecycleState) -> Unit>()

  override val state: AppLifecycleState
    get() = current.get()

  /** Updates state and notifies observers. */
  fun setState(value: AppLifecycleState) {
    current.set(value)
    listeners.forEach { it(value) }
  }

  override fun observe(handler: (AppLifecycleState) -> Unit): ObservationToken {
    listeners += handler
    return ObservationToken { listeners -= handler }
  }
}
