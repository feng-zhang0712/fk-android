# fk-android

[![Android](https://img.shields.io/badge/Android-minSdk%2024-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/version-0.1.2-orange.svg)](CHANGELOG.md)
[![JitPack](https://jitpack.io/v/feng-zhang0712/fk-android.svg)](https://jitpack.io/#feng-zhang0712/fk-android)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Android component libraries for shared app infrastructure and Compose UI.

| Module | Remote install (JitPack) | Role |
|--------|--------------------------|------|
| `:core` | `com.github.feng-zhang0712.fk-android:core` | Foundation: pluggable contracts, network, storage, security, logging, … |
| `:ui` | `com.github.feng-zhang0712.fk-android:ui` | Design tokens + high-value Compose UI (not every Material control), including Rating |
| `:business` | `com.github.feng-zhang0712.fk-android:business` | Business composites (comment, filter, selective list rows) |
| `:sample` | — (app) | Local demo / compile smoke check |

**Naming:** short module names. Open-source apps install from a **Git tag via JitPack** (same idea as SPM from Git). Maven Central coordinates `com.fk.android:*` are planned — see [docs/releasing.md](docs/releasing.md).

Current library version / Git tag: **`0.1.2`**. See [CHANGELOG.md](CHANGELOG.md).

## Code style

- **Indentation:** 2 spaces (no tabs) for Kotlin, Gradle Kotlin DSL, XML, TOML, and Markdown.
- Configured via root [`.editorconfig`](.editorconfig) (Android Studio / IntelliJ / VS Code / Cursor honor it when EditorConfig is enabled).
- Prefer the project `.editorconfig` over personal IDE defaults when contributing.

## Requirements

- JDK 17+
- Android SDK (compile / target **36**, **minSdk 24**)
- Android Studio Ladybug+ (or compatible AGP 8.8)

`minSdk 24` (Android 7.0) keeps the shared library usable on virtually all active Android devices. Raise only if a future component requires newer platform APIs.

## Project layout

```text
fk-android/
├── core/                 # foundation library
├── ui/                   # Compose UI library (depends on :core)
├── business/             # business UI library (depends on :ui)
├── sample/               # demo application
├── docs/                 # installation & release docs
├── scripts/              # publish helpers
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

### `:core` packages

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

### `:ui` packages

| Package | Intent |
|---------|--------|
| `theme` | Design tokens + `FkTheme` |
| `empty` | Empty / error / loading overlays |
| `skeleton` | Skeleton placeholders |
| `toast` | Toast / HUD / snackbar |
| `list` | List orchestration (refresh, paging chrome) |
| `textfield` | Formatted / validated inputs |
| `sheet` | Product-level sheets when Material is not enough |
| `widget` | Avatar, chip façade, tag, status pill |
| `flow` | Step indicator + timeline |

### `:business` packages

| Package | Intent |
|---------|--------|
| `comment` | Comment list + composer |
| `filter` | Tab / multi-panel filter UX |
| `cell` | Selective business row patterns (models + Compose) |

## Build

```bash
./gradlew :core:assembleRelease :ui:assembleRelease :business:assembleRelease
./gradlew :sample:assembleDebug
```

## Installation (consume in an Android app)

Install from a **GitHub release tag** via [JitPack](https://jitpack.io) (closest to iOS SPM-from-Git).

**Full guide:** **[docs/installation.md](docs/installation.md)**  
**Release / tag checklist:** **[docs/releasing.md](docs/releasing.md)**

```kotlin
// settings.gradle.kts — repositories
maven { url = uri("https://jitpack.io") }

// app/build.gradle.kts — dependency (version = Git tag)
implementation("com.github.feng-zhang0712.fk-android:business:0.1.2")
```

### Same Gradle build (monorepo)

```kotlin
dependencies {
  implementation(project(":business")) // pulls :ui and :core
}
```

## Design notes

- Prefer **Jetpack + Material 3** for commodity controls; only wrap what multi-app projects need for consistency.
- Use Compose Navigation / app scaffolds in host apps rather than library “base” activity shells.
- English only for public APIs, comments, and docs.

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
- Tag stable releases with semantic versions **without** a `v` prefix (for example: `0.1.2`), on `main` after merging from `develop`. JitPack serves that tag to remote consumers.

## License

MIT — see [LICENSE](LICENSE).
