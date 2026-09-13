package com.fk.core.biometric

/**
 * Passcode / device-credential fallback policy for biometric prompts.
 *
 * Conceptually aligned with iOS `FKBiometricPolicy`.
 */
enum class BiometricPolicy {
  /** Biometrics only (no device credential button / fallback). */
  BiometricsOnly,

  /** Biometrics, with optional device PIN / pattern / password. */
  BiometricsOrDeviceCredential,

  /** Device credential only (PIN / pattern / password). */
  DeviceCredential,
}

/**
 * Detected biometric modality on the device (best-effort via PackageManager features).
 */
enum class BiometryType {
  None,
  Fingerprint,
  Face,
  Iris,
}

/**
 * Silent readiness snapshot from [BiometricManager] (never shows UI).
 */
data class BiometricCapability(
  val canAuthenticate: Boolean,
  val biometryType: BiometryType,
  val isBiometryEnrolled: Boolean,
  /** Whether a device PIN / pattern / password can be used (separate probe). */
  val isDeviceCredentialSet: Boolean,
  val evaluatedPolicy: BiometricPolicy,
  val probeError: BiometricError? = null,
)

/**
 * Per-call overrides for [BiometricAuthenticating.authenticate].
 */
data class BiometricAuthOptions(
  val policy: BiometricPolicy? = null,
  val allowDeviceCredential: Boolean = true,
  val title: String? = null,
  val subtitle: String? = null,
  val negativeButtonText: String? = null,
)

/**
 * Long-lived defaults for [BiometricAuth].
 */
data class BiometricAuthConfiguration(
  val defaultPolicy: BiometricPolicy = BiometricPolicy.BiometricsOrDeviceCredential,
  val defaultNegativeButtonText: String = "Cancel",
)

/**
 * Common localized-reason helpers (hosts may still pass custom strings).
 */
object BiometricReason {
  fun unlockApp(): String = "Unlock to continue"
  fun confirmAction(): String = "Confirm it is you"
}

/**
 * Unified biometric failure taxonomy.
 *
 * Conceptually aligned with iOS `FKBiometricError` (Apple-only cases omitted).
 */
sealed class BiometricError(message: String? = null) : Exception(message) {
  data object BiometryNotAvailable : BiometricError("Biometry not available")
  data object BiometryNotEnrolled : BiometricError("Biometry not enrolled")
  data object BiometryLockout : BiometricError("Biometry locked out")
  data object PasscodeNotSet : BiometricError("Device credential not set")
  data object AuthenticationFailed : BiometricError("Authentication failed")
  data object UserCancelled : BiometricError("User cancelled")
  data object UserFallback : BiometricError("User chose fallback")
  data object SystemCancelled : BiometricError("System cancelled")
  data object AppCancelled : BiometricError("App cancelled")
  data object NotInteractive : BiometricError("Not interactive")
  data object InvalidReason : BiometricError("Invalid reason")
  data object AuthenticationInProgress : BiometricError("Authentication already in progress")
  data class Underlying(val code: Int, val detail: String? = null) :
    BiometricError(detail ?: "Underlying biometric error: $code")
}
