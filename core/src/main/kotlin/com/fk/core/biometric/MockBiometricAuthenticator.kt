package com.fk.core.biometric

import androidx.fragment.app.FragmentActivity

/**
 * Test / sample double for [BiometricAuthenticating].
 *
 * Conceptually aligned with iOS `FKMockBiometricAuthenticator`.
 */
class MockBiometricAuthenticator(
  var capabilityResult: BiometricCapability = BiometricCapability(
    canAuthenticate = true,
    biometryType = BiometryType.Fingerprint,
    isBiometryEnrolled = true,
    isDeviceCredentialSet = true,
    evaluatedPolicy = BiometricPolicy.BiometricsOrDeviceCredential,
  ),
  var authenticateOutcome: Result<Unit> = Result.success(Unit),
) : BiometricAuthenticating {
  override fun capability(policy: BiometricPolicy): BiometricCapability =
    capabilityResult.copy(evaluatedPolicy = policy)

  override fun capability(): BiometricCapability = capabilityResult

  override suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    policy: BiometricPolicy,
    options: BiometricAuthOptions,
  ) {
    if (reason.trim().isEmpty()) throw BiometricError.InvalidReason
    authenticateOutcome.getOrElse { throw it }
  }

  override suspend fun authenticate(
    activity: FragmentActivity,
    reason: String,
    options: BiometricAuthOptions,
  ) {
    authenticate(activity, reason, options.policy ?: capabilityResult.evaluatedPolicy, options)
  }

  override fun cancelAuthentication() = Unit
}
