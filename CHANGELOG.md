# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
for library coordinates (`com.fk.android:*`). Versions in the **0.x** line may include
breaking API changes without a major bump.

## [0.1.0] - 2026-09-14

First public release of the fk-android library modules.

### Added

#### `:core` (`com.fk.android:core`)

- Pluggable DI contracts and sample mocks
- OkHttp `ApiClient` network façade
- DataStore key-value storage with optional Keystore encryption
- Logging sinks (Logcat / file) with structured fields
- JSON mapping helpers and business envelope utilities
- Security / crypto façade with Keystore-backed keys
- Runtime i18n helpers
- Permissions, biometric, notification, background (WorkManager), file transfer, image loading, and app infra façades

#### `:ui` (`com.fk.android:ui`)

- Design tokens and `FkTheme` (Material 3 bridge)
- Empty / loading / error overlays
- Skeleton placeholders
- Toast / HUD / snackbar presenter
- Lazy list refresh and load-more orchestration
- Text fields (formatting, validation, OTP, counted area)
- Bottom and center sheet façades
- Widgets: avatar, chip façade, tag, status pill
- Lean step indicator and timeline (`flow`)

#### `:business` (`com.fk.android:business`)

- Comment kit (Standard / Compact presets, composer session, drafts)
- Tab-strip filter host with multi-panel selection
- Selective business list-row Compose kit

#### Tooling & docs

- Maven Publish for `:core`, `:ui`, `:business` (`publishToMavenLocal` / GitHub Packages)
- Installation guide (`docs/installation.md`) and release checklist (`docs/releasing.md`)
- Local and GitHub Packages publish scripts under `scripts/`
- Sample catalog app (`:sample`) covering library demos

### Notes

- `:sample` is not published.
- Pin exact versions in host apps; treat **0.1.0** as an initial 0.x surface, not an API freeze.
