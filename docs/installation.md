# Installation & Consumption

How host Android apps depend on **fk-android** from a **remote Git release** (same idea as installing an iOS package from a Git tag).

Library version is `FK_VERSION_NAME` in [`gradle.properties`](../gradle.properties) (currently **0.1.1**). Git tags use the **same** string (**no `v` prefix**), e.g. `0.1.1`.

```text
:business  →  :ui  →  :core

Prefer depending on the highest module you need:
  implementation("…:business:<version>")  // pulls ui + core via api()
```

`:sample` is a demo app only — **not** published.

---

## iOS SPM → Android (mental model)

| iOS | Android (this repo) |
|-----|---------------------|
| Add package from GitHub URL | Add the **JitPack** Maven repository |
| Resolve a **Git tag** | Use that **tag** as the dependency version |
| Link a product | `implementation("group:artifact:tag")` |

Open-source path today: **GitHub tag → [JitPack](https://jitpack.io) builds AARs → your app downloads them.**  
No local install required. Anyone with network access can depend on the library.

Longer term, the same artifacts can also be published to **Maven Central** under `com.fk.android` (see [releasing.md](releasing.md)). Until then, use JitPack coordinates below.

---

## Artifacts

| Gradle module | JitPack coordinate (Git tag install) | Future Maven Central |
|---------------|--------------------------------------|----------------------|
| `:core` | `com.github.feng-zhang0712.fk-android:core` | `com.fk.android:core` |
| `:ui` | `com.github.feng-zhang0712.fk-android:ui` | `com.fk.android:ui` |
| `:business` | `com.github.feng-zhang0712.fk-android:business` | `com.fk.android:business` |

---

## Recommended — install from Git tag (JitPack)

Closest to “Add Package Dependency” from a Git repo on iOS.

### 1. Add the JitPack repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}
```

### 2. Add the dependency

```kotlin
// app (or feature) build.gradle.kts
dependencies {
  // Version = Git tag on https://github.com/feng-zhang0712/fk-android
  implementation("com.github.feng-zhang0712.fk-android:business:0.1.1")

  // Or layer explicitly:
  // implementation("com.github.feng-zhang0712.fk-android:core:0.1.1")
  // implementation("com.github.feng-zhang0712.fk-android:ui:0.1.1")
}
```

### 3. First sync tip

The **first** resolve of a new tag may take a few minutes while JitPack builds from source. Check build status:

- Repo lookup: [https://jitpack.io/#feng-zhang0712/fk-android](https://jitpack.io/#feng-zhang0712/fk-android)
- Direct log (example): `https://jitpack.io/com/github/feng-zhang0712/fk-android/business/0.1.1/build.log`

Host apps still need their own Compose / Material3 BOM. Wrap UI from `:ui` / `:business` in `FkTheme` as described in package READMEs.

---

## Alternative — Maven Central (`com.fk.android`)

When Central publishing is enabled (see [releasing.md](releasing.md)):

```kotlin
repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation("com.fk.android:business:0.1.1")
}
```

No JitPack repo needed. Prefer this once available — stable coordinates for open source.

---

## Alternative — GitHub Packages

Works for GitHub-hosted packages but **often requires a GitHub token even to download**, which is awkward for public open-source consumers. Prefer JitPack or Maven Central for public apps.

```kotlin
maven {
  url = uri("https://maven.pkg.github.com/feng-zhang0712/fk-android")
  credentials {
    username = providers.gradleProperty("gpr.user").orNull
      ?: System.getenv("GITHUB_ACTOR")
    password = providers.gradleProperty("gpr.key").orNull
      ?: System.getenv("GITHUB_TOKEN")
  }
}
```

```kotlin
implementation("com.fk.android:business:0.1.1")
```

---

## Maintainer / CI only — local `mavenLocal()`

For library authors debugging publish output on one machine (not for app teams):

```bash
./scripts/publish-local.sh
```

Then temporarily add `mavenLocal()` in the host app. Prefer JitPack for normal integration.

---

## Composite build / source (monorepo)

When the host app and this repo live side by side during development:

```kotlin
// host settings.gradle.kts
includeBuild("../fk-android") {
  dependencySubstitution {
    substitute(module("com.fk.android:business")).using(project(":business"))
    substitute(module("com.fk.android:ui")).using(project(":ui"))
    substitute(module("com.fk.android:core")).using(project(":core"))
  }
}
```

---

## Versioning

- **0.x** = public API may still change; pin exact versions in apps.
- Bump `FK_VERSION_NAME`, update [CHANGELOG.md](../CHANGELOG.md), merge to `main`, tag **without** `v` (e.g. `0.1.1`).
- JitPack builds from that tag automatically on first consumer request (or when you open the JitPack page and click **Get** / **Look up**).

See [releasing.md](releasing.md).

---

## ProGuard / R8

Each library ships `consumer-rules.pro`. Gradle merges consumer rules for `implementation` dependencies automatically.

---

## See also

- Root [README.md](../README.md)
- [releasing.md](releasing.md)
- Per-package `README.md` under `core/`, `ui/`, `business/`
