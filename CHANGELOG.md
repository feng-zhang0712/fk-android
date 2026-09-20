# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
for library coordinates (`com.fk.android:*`). Versions in the **0.x** line may include
breaking API changes without a major bump.

## [0.1.2] - 2026-09-20

### Added

#### `:ui`

- **`FkRating`** star rating control (`com.fk.ui.rating`) with read-only and interactive modes
- Whole / half / custom step snapping, tap and drag selection, optional caption, haptics, and TalkBack range semantics
- Partial fill via draw clip (correct half-star rendering); bundled star vectors; custom painter support
- Convenience APIs: `FkRatingReadOnly`, `FkRatingInteractive`
- Sample hub under UI → Rating covering public Rating APIs

#### `:business`

- Filter appearance model (`FilterAppearance`, pill styles, panel height policies, equal-width strip)

### Changed

- Filter tab chevron defaults to stroke chevrons (`fk_ic_chevron_down` / `_up`); removed solid triangle assets
- Filter strip uses Surface token and normal-weight titles; panel section weights normalized
- Sync module / package `VERSION` markers with `FK_VERSION_NAME` **0.1.2**

## [0.1.1] - 2026-09-14

### Added

- JitPack remote install from Git tags (`jitpack.yml`, JDK 17, `publishLibrariesToMavenLocal`)
- JitPack-aware Maven `groupId` so multi-module transitive POMs resolve for consumers
- Docs: document Git-tag / JitPack install as the open-source path (SPM-like)

### Changed

- Recommend JitPack coordinates for host apps; keep `com.fk.android` for local / GitHub Packages / future Maven Central

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
