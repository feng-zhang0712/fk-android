# Pluggable (`com.fk.core.pluggable`)

DI / replaceable contracts for app infrastructure. Phase **A1** in the root
[component guide](../../../../../../../../docs/component-guide.md).

## Layout

| Package | Role |
|---------|------|
| `com.fk.core.pluggable` | `Pluggable`, `PluggableServices` |
| `…core` | Observation token, app lifecycle |
| `…networking` | API client, interceptors, credentials, reachability |
| `…storage` | Key-value + typed store |
| `…session` | User session |
| `…configuration` | Environment, feature flags, remote config |
| `…logging` | Pluggable logger levels |
| `…mock` | In-memory / Logcat stubs for samples and tests |

## Usage

```kotlin
val storage = DefaultTypedStore(InMemoryKeyValueStore())
val session = MockUserSession(initiallyAuthenticated = true, initialUserId = "u-1")
val services = PluggableServices(
  apiClient = MockApiClient(),
  storage = storage,
  session = session,
  sessionObserver = session,
  environment = MockAppEnvironmentProvider(),
  featureFlags = MockFeatureFlagProvider(mapOf("new_home" to true)),
  logger = MockPluggableLogger(),
  reachability = MockReachability(),
)
```

Prefer injecting individual interfaces into feature modules over passing
`PluggableServices` everywhere.

Concrete adapters: OkHttp → [`network`](../network/README.md); DataStore-backed
storage arrives in the later `storage` package. Mocks remain for samples/tests.
