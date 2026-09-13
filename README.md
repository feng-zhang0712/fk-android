# fk-android

[![Android](https://img.shields.io/badge/Android-minSdk%2024-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Android component libraries corresponding to iOS [FKKit](../FKKit) / [FKBusinessKit](../FKBusinessKit).

| Module | Maven artifact | Maps to (iOS) | Role |
|--------|----------------|---------------|------|
| `:core` | `com.fk.android:core` | FKCoreKit | Foundation: pluggable contracts, network, storage, security, logging, … |
| `:ui` | `com.fk.android:ui` | FKUIKit | Design tokens + high-value Compose UI (not every Material control) |
| `:business` | `com.fk.android:business` | FKBusinessKit | Business composites (comment, filter, selective list rows) |
| `:sample` | — (app) | FKKitExamples | Local demo / compile smoke check |

**Naming:** short module names; brand lives in Maven `groupId` (`com.fk.android`), not in every artifact prefix.


## Code style

- **Indentation:** 2 spaces (no tabs) for Kotlin, Gradle Kotlin DSL, XML, TOML, and Markdown.
- Configured via root [`.editorconfig`](.editorconfig) (Android Studio / IntelliJ / VS Code / Cursor honor it when EditorConfig is enabled).
- Prefer the project `.editorconfig` over personal IDE defaults when contributing.

## Requirements

- JDK 17+
- Android SDK (compile / target **36**, **minSdk 24**)
- Android Studio Ladybug+ (or compatible AGP 8.8)

`minSdk 24` (Android 7.0) is intentional for a shared library: broader device reach than matching iOS 15’s calendar year (≈ API 31), while still covering virtually all active Android devices. Raise only if a future component requires newer platform APIs.

## Project layout

```text
fk-android/
├── core/                 # foundation library
├── ui/                   # Compose UI library (depends on :core)
├── business/             # business UI library (depends on :ui)
├── sample/               # demo application
├── docs/                 # component guide and design notes
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

## Component guide

Before adding components, read **[docs/component-guide.md](docs/component-guide.md)** — what to encapsulate from FKKit / FKBusinessKit vs what to leave to Android / Material.

## Design notes

- Prefer **Jetpack + Material 3** for commodity controls; only wrap what multi-app projects need for consistency.
- Do **not** port iOS `Base` view-controller shells; use Compose Navigation / app scaffolds in host apps.
- English only for public APIs, comments, and docs (aligned with FKKit).


## Contributing

Pull requests are welcome. Open PRs against **`develop`**, keep changes focused, and ensure library modules assemble successfully.

## Support

File bug reports and feature requests in [GitHub Issues](https://github.com/feng-zhang0712/fk-android/issues).

## Security

Please report security vulnerabilities through [GitHub private security advisories](https://github.com/feng-zhang0712/fk-android/security/advisories/new) instead of public issues.

## Branching & Collaboration (Recommended)

- Use **`develop`** as the integration branch.
- Create feature branches from `develop` (for example: `feature/network`, `feature/theme`).
- Keep **`main`** aligned with stable / release snapshots.
- Keep commits focused and use clear conventional-style messages.
- Follow this commit format:
  - `<type>(<scope>): <subject>`
  - Example: `feat(network): add OkHttp client façade with interceptors`
- Recommended commit types:
  - `feat`: new feature
  - `fix`: bug fix
  - `refactor`: internal refactor without behavior change
  - `perf`: performance improvement
  - `docs`: documentation updates
  - `test`: tests added or updated
  - `build`: build/dependency/tooling changes
  - `chore`: maintenance tasks
- Commit message rules:
  - Use present tense and imperative mood (`add`, `fix`, `refactor`).
  - Keep the subject concise (recommended ≤ 72 characters).
  - Reference module scope whenever possible (for example: `network`, `theme`, `comment`, `core`, `ui`, `business`, `sample`, `docs`).
  - Add a body when context is needed (why, impact, migration notes).
- Open pull requests into `develop` with:
  - change summary
  - test/verification notes
  - migration notes when APIs change
- Tag stable releases with semantic versions (for example: `0.1.0`), then merge release work back into `develop`.

## License

MIT — see [LICENSE](LICENSE).
