# Biometric (`com.fk.core.biometric`)

Thin [BiometricPrompt](https://developer.android.com/reference/androidx/biometric/BiometricPrompt) façade: capability probe, policy mapping, and typed errors. Phase **B4**.

## Layout

| Type | Role |
|------|------|
| `Biometric` | Package marker + `create` / `mock` factories |
| `BiometricAuthenticating` | Pluggable contract |
| `BiometricAuth` | Default Jetpack implementation |
| `MockBiometricAuthenticator` | Tests / samples |
| `BiometricCapability` / `BiometricPolicy` / `BiometryType` | Readiness + policy models |
| `BiometricError` | Unified failure taxonomy |
| `BiometricAuthOptions` / `BiometricAuthConfiguration` | Per-call / default knobs |
| `authenticateIfAvailable` | Probe-then-authenticate helper |

## Usage

```kotlin
val biometric = Biometric.create(context)
val capability = biometric.capability(BiometricPolicy.BiometricsOrDeviceCredential)

if (capability.canAuthenticate) {
  try {
    biometric.authenticate(
      activity = activity, // FragmentActivity
      reason = BiometricReason.unlockApp(),
      policy = BiometricPolicy.BiometricsOrDeviceCredential,
    )
  } catch (e: BiometricError.UserCancelled) {
    // silent exit
  } catch (e: BiometricError) {
    // guide user (not enrolled, lockout, …)
  }
}
```

`MainActivity` (or host) must be a [FragmentActivity] — required by `BiometricPrompt`.

## Notes

- No Keystore `CryptoObject` binding in this phase (matches iOS public API: auth gate only).
- Apple-only cases (Optic ID, Watch) are not ported.
- Device-credential policies omit the negative button (Android platform rule).
- Use `openBiometricSettings()` when capability reports not enrolled / no credential.
- `BiometricCapability.isDeviceCredentialSet` is probed separately from the active policy.
