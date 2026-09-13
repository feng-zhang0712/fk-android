package com.fk.core.biometric

import androidx.fragment.app.FragmentActivity

/**
 * Pluggable biometric authentication contract.
 *
 * Conceptually aligned with iOS `FKBiometricAuthenticating`.
 */
interface BiometricAuthenticating {
  /** Probes readiness for [policy] without showing UI. */
  fun capability(policy: BiometricPolicy): BiometricCapability

  /** Probes readiness using the implementation default policy. */
  fun capability(): BiometricCapability

  /**
   * Presents the system biometric / credential prompt.
   *
   * @throws BiometricError on failure or cancellation.
   */
  suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    policy: BiometricPolicy,
    options: BiometricAuthOptions = BiometricAuthOptions(),
  )

  /** Authenticates using the implementation default policy. */
  suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    options: BiometricAuthOptions = BiometricAuthOptions(),
  )

  /** Cancels an in-flight prompt; pending [authenticate] fails with [BiometricError.AppCancelled]. */
  fun cancelAuthentication()
}
