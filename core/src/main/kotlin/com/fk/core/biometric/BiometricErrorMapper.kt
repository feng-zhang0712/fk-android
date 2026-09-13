package com.fk.core.biometric

import androidx.biometric.BiometricPrompt

/**
 * Maps [BiometricPrompt] error codes to [BiometricError].
 */
internal object BiometricErrorMapper {
  fun fromPromptError(errorCode: Int, errString: CharSequence?): BiometricError =
    when (errorCode) {
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      -> BiometricError.BiometryNotAvailable
      BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricError.BiometryNotEnrolled
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      -> BiometricError.BiometryLockout
      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> BiometricError.PasscodeNotSet
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_USER_CANCELED,
      -> BiometricError.UserCancelled
      BiometricPrompt.ERROR_CANCELED -> BiometricError.SystemCancelled
      BiometricPrompt.ERROR_NO_SPACE,
      BiometricPrompt.ERROR_TIMEOUT,
      BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
      BiometricPrompt.ERROR_VENDOR,
      -> BiometricError.AuthenticationFailed
      else -> BiometricError.Underlying(errorCode, errString?.toString())
    }
}
