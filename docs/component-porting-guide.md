# Component Porting Guide

Guidance for what to encapsulate in **fk-android** (`:core` / `:ui` / `:business`) versus what to leave to Android / Jetpack / Material.

**Sources analyzed**

- iOS [FKKit](../../FKKit) — `FKCoreKit` + `FKUIKit`
- iOS [FKBusinessKit](../../FKBusinessKit) — Base, TabBarFilter, CellKit, CommentKit

**Principles**

1. If Android / Jetpack / Material 3 already provides a mature, standard solution that apps normally use directly → **do not wrap**.
2. Encapsulate when multi-app projects need a **shared contract**, consistent behavior, or product-level orchestration that SDKs do not give out of the box.
3. Port **concepts and models**, not UIKit APIs or ViewController inheritance trees.
4. Prefer thin façades over reinventing OkHttp, DataStore, WorkManager, BiometricPrompt, Coil, Media3, etc.
5. Skip Apple-only capabilities (FairPlay, AirPlay, SharePlay, CarPlay, ATT, Photos Limited, Quick Look, SF Symbols as primary icon source, `BGTaskScheduler` semantics).

**Target stack (reference)**

OkHttp · DataStore · WorkManager · BiometricPrompt · Media3 · Coil · Jetpack Compose · Material 3 · kotlinx.serialization · Coroutines

**Module dependency (hard rule)**

```text
:business → :ui → :core
:sample   → :business   (demo only; not published)
```

Do not invent reverse dependencies (`:core` must not depend on `:ui`).

---

## Decision summary

| Decision | Meaning |
|----------|---------|
| **Encapsulate** | Implement (or thin-façade) in fk-android |
| **Skip** | Use platform / Material / host app code; no library module |
| **Optional** | Only if a product vertical needs it |
| **Apple-only** | Do not port |

---

## Recommended encapsulation order

Work **top to bottom**. Finish a phase’s **verify** gate before starting the next. Prefer one feature branch per row (or small coherent group), branched from `develop` (for example `feature/pluggable`, `feature/network`).

### Priority legend

| Priority | Meaning |
|----------|---------|
| **P0** | Foundation — blocks almost everything else |
| **P1** | High reuse across apps; do soon after P0 |
| **P2** | Important UX chrome; after design tokens exist |
| **P3** | Business composites; after list / form primitives |
| **P4** | Optional / vertical / media — only when product needs it |

### Phase A — `:core` contracts & data plane (P0)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| A1 | `pluggable` | Pluggable | Protocol seams: networking, storage, session, routing, analytics, logging, media, config. No heavy implementations yet — interfaces + composition root hooks. | Sample can bind mock implementations |
| A2 | `network` | Network | OkHttp-based client façade: endpoints, interceptors, retry, error model, optional SSL pin / cache policy | Smoke request in `:sample` or unit test |
| A3 | `storage` | Storage | Key-value + typed storage on DataStore; encrypted path via Keystore strategy | Round-trip read/write |
| A4 | `logging` | Logger | Levels, structured fields, optional file sink | Logs visible from sample |
| A5 | `mapping` | ModelMapping | kotlinx.serialization conventions + business envelope helpers | Encode/decode sample DTO |

**Why this order:** Pluggable first so Network/Storage/Logger plug into seams instead of becoming hard-wired singletons.

### Phase B — `:core` security, locale, device façades (P1)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| B1 | `security` | Security | Hash / AES / RSA / HMAC / masking / random; Keystore-backed key storage | Encrypt/decrypt round-trip |
| B2 | `i18n` | I18n | Runtime locale switch, typed keys, formatters (beyond static `res/values`) | Switch locale in sample |
| B3 | `permissions` | Permissions | Unified check/request façade over Android runtime permissions | Request one permission end-to-end |
| B4 | `biometric` | BiometricAuth | Thin BiometricPrompt façade (capability / policy / errors) | Prompt on device/emulator with biometrics |
| B5 | `background` | BackgroundTask | WorkManager scheduling contract (refresh / processing) | Enqueue + observe one worker |
| B6 | `notification` | LocalNotification | Channels + schedule/cancel model over NotificationManager | Schedule a local notification |

**Thin / defer inside Phase B**

| Package | Note |
|---------|------|
| `async` | Only if shared debounce/throttle APIs are needed across modules |
| `datetime` | Only if Moment-style shared formatters are needed for API parity |

### Phase C — `:core` files, images, app infra (P1)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| C1 | `file` | FileManager (transfers) | Resumable download/upload, queue, transfer persistence | Transfer a file with pause/resume path |
| C2 | `image` | ImageLoader (contract) | `ImageLoading` interface; Coil adapter as default impl | Load remote image via contract |
| C3 | `app` | BusinessKit (non-UI) | Version, deeplink parse/route hooks, lifecycle, analytics sink, startup tasks | Deeplink parse + version string in sample |

### Phase D — `:ui` design system & feedback (P2)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| D1 | `theme` | Theme | Color / typography / spacing / shape tokens; `FkTheme` | Sample themed screen |
| D2 | `empty` | EmptyState | Loading / empty / error overlays | Toggle states in sample |
| D3 | `skeleton` | Skeleton | Shimmer / placeholder patterns | Skeleton on a list placeholder |
| D4 | `toast` | Toast / HUD / Snackbar | Unified queue + HUD + snackbar styling | Enqueue multiple toasts |

**Do not** implement Material clones (Button, Alert, Checkbox, …) in this phase.

### Phase E — `:ui` forms & lists (P2)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| E1 | `list` | ListKit + Refresh | Refresh, load-more chrome, empty/skeleton orchestration helpers for Lazy lists | Sample list with pull-to-refresh + empty |
| E2 | `textfield` | TextField (form) | Formatting, validation, OTP, counters — not a basic TextField wrapper | OTP + validated field demos |
| E3 | `sheet` | SheetPresentationController | **Only if** product needs multi-detent / anchored sheet beyond ModalBottomSheet | Optional sample |

### Phase F — `:business` composites (P3)

| Order | Package | iOS source | Scope | Verify |
|------:|---------|------------|-------|--------|
| F1 | `comment` | CommentKit | Models + contracts + Compose list/composer (no networking) | Full comment demo in `:sample` |
| F2 | `filter` | TabBarFilter | Multi-panel filter UX (hierarchy / dual / tags / list) | Filter demo in `:sample` |
| F3 | `cell` | CellKit (selective) | **Per vertical** item models + Compose rows — never dump all iOS cells | Only rows your apps need |

### Phase G — optional media & extras (P4)

| Order | Area | When |
|------:|------|------|
| G1 | Media3 player orchestration | Shared offline / resume / feed pool required |
| G2 | WebView JS bridge | Multiple apps share the same bridge contract |
| G3 | Widgets (Avatar / Chip / StatusPill) | Brand needs exceed Material defaults |
| G4 | QR helpers | Many apps need one shared API (otherwise use ML Kit directly) |

### Suggested milestone map

```text
Milestone 0  Scaffold          ✅ (repo, modules, this guide)
Milestone 1  Core data plane   Phase A
Milestone 2  Core façades      Phase B + C
Milestone 3  UI foundation     Phase D
Milestone 4  UI lists/forms    Phase E
Milestone 5  Business kits     Phase F1–F2
Milestone 6  Vertical cells    Phase F3 (as needed)
Milestone 7  Media / extras    Phase G (as needed)
```

### Order rationale (short)

1. **Contracts before implementations** — Pluggable prevents hard-wired globals.
2. **Network / storage / log / map together** — every feature needs them.
3. **Security & permissions next** — many flows depend on them early.
4. **Theme before UI widgets** — Empty/Skeleton/Toast must consume tokens.
5. **List + TextField before business** — Comment/Filter compose those primitives.
6. **Business before optional media** — most apps need comments/filters sooner than a custom player stack.

---

## 1. FKCoreKit → `:core`

### Encapsulate

| iOS module | Android package (planned) | Why |
|------------|---------------------------|-----|
| **Pluggable** | `com.fk.core.pluggable` | Highest priority. Shared DI / replaceable contracts (network, storage, session, routing, analytics, …). |
| **Network** | `com.fk.core.network` | OkHttp is the engine; still wrap a unified client (interceptors, retry, SSL pinning policy, cache, error model) so apps do not drift. |
| **Storage** | `com.fk.core.storage` | DataStore / SharedPreferences exist; wrap unified key-value / typed storage + encrypted storage strategy (Keystore). |
| **Security** | `com.fk.core.security` | JCA + Keystore exist; unified façade for AES / RSA / HMAC / masking / random keeps compliance and cross-app consistency. |
| **ModelMapping** | `com.fk.core.mapping` | Shared serialization conventions and business envelopes (kotlinx.serialization / Moshi patterns). |
| **Logger** | `com.fk.core.logging` | Levels, structured fields, optional file persistence, crash hooks. |
| **I18n** (runtime) | `com.fk.core.i18n` | Resources cover static strings; wrap **in-app locale switch**, remote dictionaries, typed keys. |
| **FileManager** (transfers) | `com.fk.core.file` | Resumable upload/download, transfer queue, persistence — not provided turnkey by the platform. |
| **ImageLoader** (contract) | `com.fk.core.image` | Implement with Coil; expose an `ImageLoading` interface aligned with Pluggable media. |
| **BusinessKit** (non-UI) | `com.fk.core.app` | Version, deeplink, lifecycle, analytics hooks, startup tasks — multi-app infrastructure. |
| **Permissions** (façade) | `com.fk.core.permissions` | Fragmented runtime APIs; unified check / request surface is high value. |
| **BiometricAuth** | `com.fk.core.biometric` | BiometricPrompt exists; thin façade for capability / policy / error mapping. |
| **BackgroundTask** | `com.fk.core.background` | Implement with WorkManager; unify refresh / processing task scheduling contracts. |
| **LocalNotification** | `com.fk.core.notification` | NotificationManager exists; unify scheduling models and channel strategy. |

### Thin / optional encapsulate

| iOS module | Android package | Note |
|------------|-----------------|------|
| **Async** | `com.fk.core.async` | Coroutines are standard; only wrap shared debounce / throttle / task-group semantics if needed across apps. |
| **DateTime** | `com.fk.core.datetime` | `java.time` is enough; optional Moment-style format / relative-time helpers for API parity. |

### Skip

| iOS module | Why |
|------------|-----|
| **Extension** (UIKit) | No Android equivalent; use Compose / View helpers in host apps or `:ui` as needed. |
| **Extension** (bulk Foundation utilities) | Kotlin stdlib / AndroidX cover most cases; do **not** port the whole surface — add only a few proven shared helpers if repeated. |
| **QRCode** (generate / parse) | ML Kit / ZXing are mature; call from the app or a tiny optional helper later if many apps need the same API. |
| **Appearance**-style geometry micro-helpers | Use dp / Compose modifiers. |
| **Quick Look** preview datasource | Apple-only. |

### Apple-only (do not port)

FairPlay · AirPlay · SharePlay · CarPlay · ATT (`appTracking`) · Photos library limited / add-only nuances · exact `BGTaskScheduler` semantics · SF Symbols as the primary icon pipeline · Quick Look.

---

## 2. FKUIKit → `:ui`

Material 3 and Compose already cover most commodity controls. Encapsulate **design system + high-value composition**, not Button / Checkbox clones.

### Encapsulate

| iOS module | Android package (planned) | Why |
|------------|---------------------------|-----|
| **Theme** | `com.fk.ui.theme` | Design tokens (color / type / spacing / shadow) and component defaults — required for multi-app consistency. |
| **EmptyState** | `com.fk.ui.empty` | No standard Material “loading / empty / error” overlay kit; list screens need it constantly. |
| **Skeleton** | `com.fk.ui.skeleton` | No platform skeleton standard; shared shimmer / placeholder is valuable. |
| **Toast / HUD / Snackbar queue** | `com.fk.ui.toast` | Snackbar / Toast exist; unify queue, HUD, and styling. |
| **ListKit** (orchestration pattern) | `com.fk.ui.list` | Diff + sections + refresh + empty/skeleton orchestration — wrap the **scenario**, not `UITableView` APIs. |
| **TextField** (form capabilities) | `com.fk.ui.textfield` | Skip basic TextField; wrap formatting, validation, OTP, counters. |
| **Sheet** (product-level) | `com.fk.ui.sheet` | ModalBottomSheet covers basics; wrap only if you need iOS-parity multi-detent / anchor / keyboard behavior as a product API. |
| **Refresh** | fold into `com.fk.ui.list` | SwipeRefresh exists; do not ship a large standalone refresh library. |

### Optional

| iOS module | When |
|------------|------|
| **Player / Core** (media orchestration) | Only if products need shared offline, resume, QoE, feed player pool on top of Media3. Drop FairPlay / AirPlay / SharePlay / CarPlay. |
| **WebView** JS bridge | Only if multiple apps share the same bridge contract; otherwise use WebView / Custom Tabs directly. |
| **Widgets** (Avatar, Chip, StatusPill, …) | Only when brand customization exceeds Material Chip / simple Compose; otherwise keep in host apps or thin theme wrappers. |

### Skip (use Material / platform)

| iOS module | Android counterpart |
|------------|---------------------|
| **Button** | Material Button |
| **ActionSheet / Alert** | ModalBottomSheet + AlertDialog |
| **Badge** | Badge / BadgedBox |
| **Divider** | HorizontalDivider |
| **SelectionControl** | Checkbox / RadioButton |
| **ProgressBar** | Linear / Circular ProgressIndicator |
| **RatingControl** | Simple custom or leave to app |
| **SearchBar** | SearchBar / DockedSearchBar |
| **TabBar** | TabRow / TabLayout |
| **PagingController** | Tab + HorizontalPager / ViewPager2 |
| **Carousel** | HorizontalPager |
| **ImageView** | Coil + AsyncImage |
| **PhotoPicker** | Photo Picker / Activity Result APIs |
| **QR scanner UI** | CameraX + ML Kit |
| **BlurView** | RenderEffect; imperfect parity — do not force a library |
| **CornerShadow / Appearance** | Shape + elevation / modifiers |
| **Keyboard toolkit** | WindowInsets / IME — iOS-shaped; do not port |
| **Callout / ExpandableText / Marquee / IconView** | Write in the screen; not a library system by default |
| **MediaGallery** | Host app or existing viewer libraries |
| **SearchViewController** | Compose a search screen from SearchBar + list + empty/skeleton |

---

## 3. FKBusinessKit → `:business`

This iOS package is UIKit business chrome. **Do not port ViewController / Cell base classes.** Port contracts, models, and Compose UX.

### Encapsulate

| iOS module | Android package (planned) | Why |
|------------|---------------------------|-----|
| **CommentKit** | `com.fk.business.comment` | Clear feature boundary: list / reply / optimistic like / composer contracts, no networking. Strong parity candidate. |
| **TabBarFilter** | `com.fk.business.filter` | High-value filter UX (hierarchy, dual column, tags, single list). Reimplement UI in Compose; reuse selection models conceptually. |

### Optional (by product vertical)

| iOS module | Android package | Guidance |
|------------|-----------------|----------|
| **CellKit** | `com.fk.business.cell` | **Do not** port 20+ cells wholesale. Pick rows by domain (commerce, social, user list, …). Ship **item models + Compose row patterns**, not `UITableViewCell` clones. Prefer CommentKit over display-only comment thread cells when actions + composer matter. |

### Skip

| iOS module | Why |
|------------|-----|
| **Base** (`FKBaseViewController`, …) | Android uses Activity / Fragment / Compose Navigation + app Scaffold. Learn overlay / refresh **patterns** from iOS docs; do not create a Base VC hierarchy. |
| **Icons / asset catalog** | Ship Android drawables / vectors under resources as needed; no UIImage API port. |

---

## 4. What “done” means for a new component

Before merging a new public API into fk-android:

1. Confirm it is **Encapsulate** or an approved **Optional** in this guide (or update this guide with rationale).
2. Prefer extending an existing package over adding a new top-level concept.
3. Follow the **Recommended encapsulation order** unless you document why a later phase is pulled forward.
4. Public types documented in English; no Chinese in library sources.
5. Sample coverage under `:sample` for every public capability worth demoing.
6. `./gradlew :module:assembleRelease` succeeds; no speculative dependencies (Hilt, Media3, CameraX, …) until a component needs them.
7. Open the PR against **`develop`** (see root README → Branching & Collaboration).

---

## 5. Quick checklist

**Do encapsulate**

Pluggable · Network façade · Storage abstraction · Security façade · Model mapping · Logger · Runtime I18n · Permissions · Biometric / Background / LocalNotification façades · File transfers · ImageLoading contract · App infra (non-UI BusinessKit) · Theme · EmptyState · Skeleton · Toast queue · List orchestration · Form TextField enhancements · CommentKit · TabBarFilter · (optional) Player orchestration · (optional, selective) CellKit rows

**Do not encapsulate**

Material commodity controls (Button, Alert, basic Sheet, Badge, Divider, Checkbox, Radio, Progress, Search, Tab, Pager, Carousel) · PhotoPicker · QR · Blur · Keyboard toolkit · bulk Extensions · Base VC system · whole CellKit UI dump · Apple-only features

---

*Last updated: 2026-09-13 — phases A–G ordered for fk-android implementation.*
