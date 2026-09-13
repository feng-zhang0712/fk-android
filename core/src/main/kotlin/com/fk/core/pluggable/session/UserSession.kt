package com.fk.core.pluggable.session

import com.fk.core.pluggable.core.ObservationToken

/**
 * Authenticated user session boundary for feature modules.
 *
 * Features depend on this interface instead of a concrete login manager singleton.
 */
interface UserSession {
  /** Whether the user is signed in with a valid session. */
  val isAuthenticated: Boolean

  /** Stable user identifier when authenticated. */
  val userId: String?

  /**
   * Clears local session state (tokens, profile cache).
   * Does not define UI navigation.
   */
  fun signOut()
}

/**
 * Observes session changes (login, logout, account switch).
 */
interface UserSessionObserver {
  /**
   * Adds a handler invoked when authentication state changes.
   *
   * @param handler Receives `true` when signed in.
   */
  fun observeAuthenticationChange(handler: (Boolean) -> Unit): ObservationToken
}
