package com.fk.core.biometric

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager

/**
 * Maps [BiometricPolicy] to [BiometricManager] authenticator flags and probes capability.
 */
internal object BiometricCapabilityProbe {
  internal const val ANY_BIOMETRIC: Int =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.BIOMETRIC_WEAK

  fun authenticators(
    policy: BiometricPolicy,
    allowDeviceCredential: Boolean,
  ): Int =
    when (policy) {
      BiometricPolicy.BiometricsOnly -> ANY_BIOMETRIC
      BiometricPolicy.BiometricsOrDeviceCredential ->
        if (allowDeviceCredential) {
          ANY_BIOMETRIC or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
          ANY_BIOMETRIC
        }
      BiometricPolicy.DeviceCredential -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

  fun probe(
    context: Context,
    policy: BiometricPolicy,
    allowDeviceCredential: Boolean = true,
  ): BiometricCapability {
    val authenticators = authenticators(policy, allowDeviceCredential)
    val manager = BiometricManager.from(context)
    val code = manager.canAuthenticate(authenticators)
    val credentialCode = manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    val biometryType = detectBiometryType(context)
    val error = mapCanAuthenticateCode(code)
    val enrolled = when (code) {
      BiometricManager.BIOMETRIC_SUCCESS -> true
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
      else -> false
    }
    return BiometricCapability(
      canAuthenticate = code == BiometricManager.BIOMETRIC_SUCCESS,
      biometryType = biometryType,
      isBiometryEnrolled = enrolled,
      isDeviceCredentialSet = credentialCode == BiometricManager.BIOMETRIC_SUCCESS,
      evaluatedPolicy = policy,
      probeError = error,
    )
  }

  fun detectBiometryType(context: Context): BiometryType {
    val pm = context.packageManager
    return when {
      pm.hasSystemFeature(PackageManager.FEATURE_FACE) -> BiometryType.Face
      pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) -> BiometryType.Fingerprint
      pm.hasSystemFeature(PackageManager.FEATURE_IRIS) -> BiometryType.Iris
      else -> BiometryType.None
    }
  }

  fun mapCanAuthenticateCode(code: Int): BiometricError? =
    when (code) {
      BiometricManager.BIOMETRIC_SUCCESS -> null
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
      BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
      -> BiometricError.BiometryNotAvailable
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricError.BiometryNotEnrolled
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricError.PasscodeNotSet
      else -> BiometricError.Underlying(code)
    }
}
