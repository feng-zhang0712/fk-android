package com.fk.core.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fk.core.pluggable.core.AppLifecycleObserver
import com.fk.core.pluggable.core.AppLifecycleState
import com.fk.core.pluggable.core.ObservationToken
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridges [ProcessLifecycleOwner] into Pluggable [AppLifecycleObserver].
 *
 * Conceptually aligned with iOS `FKBusinessLifecycleObserver` → Pluggable adapter.
 *
 * Call [close] when the host no longer needs the bridge (e.g. process-scoped singleton
 * replacement) so the ProcessLifecycle observer is removed.
 */
class ProcessLifecycleAppObserver(
  private val lifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : AppLifecycleObserver, AutoCloseable {
  private val current = AtomicReference(map(lifecycleOwner.lifecycle.currentState))
  private val listeners = CopyOnWriteArrayList<(AppLifecycleState) -> Unit>()
  private val closed = AtomicBoolean(false)

  private val observer = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = publish(AppLifecycleState.Inactive)
    override fun onResume(owner: LifecycleOwner) = publish(AppLifecycleState.Active)
    override fun onPause(owner: LifecycleOwner) = publish(AppLifecycleState.Inactive)
    override fun onStop(owner: LifecycleOwner) = publish(AppLifecycleState.Background)
    override fun onDestroy(owner: LifecycleOwner) = publish(AppLifecycleState.Terminated)
  }

  init {
    lifecycleOwner.lifecycle.addObserver(observer)
  }

  override val state: AppLifecycleState
    get() = current.get()

  override fun observe(handler: (AppLifecycleState) -> Unit): ObservationToken {
    listeners += handler
    handler(state)
    return ObservationToken { listeners -= handler }
  }

  override fun close() {
    if (!closed.compareAndSet(false, true)) return
    lifecycleOwner.lifecycle.removeObserver(observer)
    listeners.clear()
  }

  private fun publish(value: AppLifecycleState) {
    val previous = current.getAndSet(value)
    if (previous == value) return
    listeners.forEach { it(value) }
  }

  companion object {
    fun map(state: Lifecycle.State): AppLifecycleState =
      when {
        state.isAtLeast(Lifecycle.State.RESUMED) -> AppLifecycleState.Active
        state.isAtLeast(Lifecycle.State.STARTED) -> AppLifecycleState.Inactive
        state.isAtLeast(Lifecycle.State.CREATED) -> AppLifecycleState.Background
        else -> AppLifecycleState.Terminated
      }
  }
}
