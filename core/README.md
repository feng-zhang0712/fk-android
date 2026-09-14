# :core

Foundation library (`com.fk.android:core`). Non-UI infrastructure for shared apps.

## Packages

| Package | Status | Notes |
|---------|--------|-------|
| `pluggable` | Available | DI contracts + mocks |
| `network` | Available | OkHttp `ApiClient` façade |
| `storage` | Available | DataStore + optional Keystore encryption |
| `logging` | Available | Logcat / file sinks |
| `mapping` | Available | JSON conventions + envelopes |
| `security` | Available | Crypto + Keystore-backed keys |
| `i18n` | Available | Runtime locale helpers |
| `permissions` | Available | Runtime permission façade |
| `biometric` | Available | BiometricPrompt façade |
| `notification` | Available | Local notification façade |
| `background` | Available | WorkManager façade |
| `file` | Available | Resumable OkHttp transfer |
| `image` | Available | ImageLoading + Coil adapter |
| `app` | Available | Version / deeplink / startup hooks |

Public entry: `com.fk.core.FkCore`.

Installation: see [docs/installation.md](../docs/installation.md).
