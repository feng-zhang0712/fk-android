package com.fk.core.pluggable.core

/**
 * High-level application lifecycle states for pluggable observers.
 *
 * Host apps typically map [androidx.lifecycle.Lifecycle] / ProcessLifecycleOwner
 * into these values.
 */
enum class AppLifecycleState {
  Active,
  Inactive,
  Background,
  Terminated,
}

/**
 * Observes app lifecycle transitions without coupling feature modules to
 * Activity or ProcessLifecycleOwner directly.
 */
interface AppLifecycleObserver {
  /** Current lifecycle state. */
  val state: AppLifecycleState

  /**
   * Registers a state-change handler.
   *
   * @return Token that cancels the subscription when [ObservationToken.cancel] is called.
   */
  fun observe(handler: (AppLifecycleState) -> Unit): ObservationToken
}
