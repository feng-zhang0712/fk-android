package com.fk.core.biometric

import android.content.Context

/**
 * Biometric — thin BiometricPrompt façade (capability / policy / errors).
 *
 * Conceptually aligned with iOS `FKCoreKit` BiometricAuth; Android Jetpack APIs.
 */
object Biometric {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.0"

  /** Builds the default [BiometricAuth] implementation. */
  fun create(
    context: Context,
    configuration: BiometricAuthConfiguration = BiometricAuthConfiguration(),
  ): BiometricAuth = BiometricAuth(context, configuration)

  /** Builds a configurable mock for tests and samples. */
  fun mock(
    capability: BiometricCapability = BiometricCapability(
      canAuthenticate = true,
      biometryType = BiometryType.Fingerprint,
      isBiometryEnrolled = true,
      isDeviceCredentialSet = true,
      evaluatedPolicy = BiometricPolicy.BiometricsOrDeviceCredential,
    ),
    authenticateOutcome: Result<Unit> = Result.success(Unit),
  ): MockBiometricAuthenticator =
    MockBiometricAuthenticator(
      capabilityResult = capability,
      authenticateOutcome = authenticateOutcome,
    )
}
