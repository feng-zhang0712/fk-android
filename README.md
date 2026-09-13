# fk-android

Android component libraries corresponding to iOS [FKKit](../FKKit) / [FKBusinessKit](../FKBusinessKit).

| Module | Maven artifact | Maps to (iOS) | Role |
|--------|----------------|---------------|------|
| `:core` | `com.fk.android:core` | FKCoreKit | Foundation: pluggable contracts, network, storage, security, logging, … |
| `:ui` | `com.fk.android:ui` | FKUIKit | Design tokens + high-value Compose UI (not every Material control) |
| `:business` | `com.fk.android:business` | FKBusinessKit | Business composites (comment, filter, selective list rows) |
| `:sample` | — (app) | FKKitExamples | Local demo / compile smoke check |

**Naming:** short module names; brand lives in Maven `groupId` (`com.fk.android`), not in every artifact prefix.

## Requirements

- JDK 17+
- Android SDK (compile / target **36**, min **24**)
- Android Studio Ladybug+ (or compatible AGP 8.8)

## Project layout

```text
fk-android/
├── core/                 # foundation library
├── ui/                   # Compose UI library (depends on :core)
├── business/             # business UI library (depends on :ui)
├── sample/               # demo application
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

### `:core` packages (planned)

| Package | Intent |
|---------|--------|
| `pluggable` | DI / replaceable contracts |
| `network` | HTTP client façade (OkHttp) |
| `storage` | Key-value / typed storage |
| `security` | Crypto + Keystore helpers |
| `logging` | Structured logging |
| `mapping` | JSON / model mapping |
| `i18n` | Runtime locale helpers |
| `async` | Debounce / throttle utilities |
| `datetime` | Shared date/time formatting |
| `permissions` | Runtime permission façade |
| `file` | Transfer / resume I/O |
| `image` | Image loading contract |
| `notification` | Local notification scheduling |
| `biometric` | BiometricPrompt façade |
| `background` | WorkManager façade |
| `app` | Version / deeplink / lifecycle / analytics hooks |

### `:ui` packages (planned)

| Package | Intent |
|---------|--------|
| `theme` | Design tokens + `FkTheme` |
| `empty` | Empty / error / loading overlays |
| `skeleton` | Skeleton placeholders |
| `toast` | Toast / HUD / snackbar queue |
| `list` | List orchestration (refresh, paging chrome) |
| `textfield` | Formatted / validated inputs |
| `sheet` | Product-level sheets when Material is not enough |

### `:business` packages (planned)

| Package | Intent |
|---------|--------|
| `comment` | Comment list + composer contracts |
| `filter` | Tab / multi-panel filter UX |
| `cell` | Selective business row patterns (models + Compose) |

## Build

```bash
./gradlew :core:assembleRelease :ui:assembleRelease :business:assembleRelease
./gradlew :sample:assembleDebug
```

## Consume (local)

```kotlin
dependencies {
    implementation(project(":business")) // pulls :ui and :core
    // or:
    implementation(project(":core"))
    implementation(project(":ui"))
}
```

## Design notes

- Prefer **Jetpack + Material 3** for commodity controls; only wrap what multi-app projects need for consistency.
- Do **not** port iOS `Base` view-controller shells; use Compose Navigation / app scaffolds in host apps.
- English only for public APIs, comments, and docs (aligned with FKKit).

## License

MIT — see [LICENSE](LICENSE).
