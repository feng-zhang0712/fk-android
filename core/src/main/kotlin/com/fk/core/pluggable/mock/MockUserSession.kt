package com.fk.core.pluggable.mock

import com.fk.core.pluggable.core.ObservationToken
import com.fk.core.pluggable.session.UserSession
import com.fk.core.pluggable.session.UserSessionObserver
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Mutable in-memory [UserSession] + [UserSessionObserver] for samples and tests.
 */
class MockUserSession(
  initiallyAuthenticated: Boolean = false,
  initialUserId: String? = null,
) : UserSession, UserSessionObserver {
  private val authenticated = AtomicReference(initiallyAuthenticated)
  private val id = AtomicReference(initialUserId)
  private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()

  override val isAuthenticated: Boolean
    get() = authenticated.get()

  override val userId: String?
    get() = id.get()

  /** Updates authentication state and notifies observers. */
  fun setAuthenticated(value: Boolean, userId: String? = if (value) id.get() else null) {
    authenticated.set(value)
    id.set(if (value) userId else null)
    listeners.forEach { it(value) }
  }

  override fun signOut() {
    setAuthenticated(false, null)
  }

  override fun observeAuthenticationChange(handler: (Boolean) -> Unit): ObservationToken {
    listeners += handler
    return ObservationToken { listeners -= handler }
  }
}
