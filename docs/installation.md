# Installation & Consumption

How host Android apps depend on **fk-android**.

Library version is defined once in [`gradle.properties`](../gradle.properties) as `FK_VERSION_NAME` (currently **0.1.0**). Maven `groupId` is `FK_GROUP_ID` = **`com.fk.android`**.

**Recommended production path:** publish AARs to a Maven repository, then consume them with Gradle coordinates.

```text
:business  →  :ui  →  :core

Host app typically depends on the highest module it needs:
  implementation("com.fk.android:business:<version>")  // pulls ui + core via api()
  // or only:
  implementation("com.fk.android:ui:<version>")
  implementation("com.fk.android:core:<version>")
```

`:sample` is a demo app only — **not** published.

---

## Artifacts

| Gradle module | Maven coordinate | Contents |
|---------------|------------------|----------|
| `:core` | `com.fk.android:core` | Foundation (network, storage, pluggable, …) |
| `:ui` | `com.fk.android:ui` | Theme + Compose UI kits |
| `:business` | `com.fk.android:business` | Comment, filter, cell rows |

---

## Option A — Maven coordinate (recommended for apps)

Use this once artifacts are published (Maven Central, company Nexus/Artifactory, or GitHub Packages).

### 1. Repository

**Maven Central** (when published there):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}
```

**GitHub Packages:**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.pkg.github.com/feng-zhang0712/fk-android")
      credentials {
        username = providers.gradleProperty("gpr.user").orNull
          ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.key").orNull
          ?: System.getenv("GITHUB_TOKEN")
      }
    }
  }
}
```

Host `~/.gradle/gradle.properties` (or CI secrets):

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PAT
```

The PAT needs at least `read:packages` (and `write:packages` when publishing).

### 2. Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
  // Prefer the top module you need — api() edges pull transitive modules.
  implementation("com.fk.android:business:0.1.0")

  // Or pick layers explicitly:
  // implementation("com.fk.android:core:0.1.0")
  // implementation("com.fk.android:ui:0.1.0")
}
```

Host apps still need their own Compose / Material3 BOM and `FkTheme` usage as documented by each package README.

---

## Option B — `mavenLocal()` (local install)

Useful while developing fk-android and verifying a host app without publishing remotely.

### 1. Publish from this repo

```bash
./scripts/publish-local.sh
# equivalent:
# ./gradlew :core:publishToMavenLocal :ui:publishToMavenLocal :business:publishToMavenLocal
```

Artifacts land under `~/.m2/repository/com/fk/android/`.

### 2. Consume from the host app

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    mavenLocal()
  }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
  implementation("com.fk.android:business:0.1.0")
}
```

Bump `FK_VERSION_NAME` before publishing if you need to invalidate a cached local version.

---

## Option C — Composite build / source (monorepo or sibling checkout)

When the host app and fk-android live side by side (or in one repo):

### Composite build (`includeBuild`)

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

```kotlin
// host app/build.gradle.kts
dependencies {
  implementation("com.fk.android:business") // resolved to included build
}
```

### Multi-module `project()` (same Gradle build)

```kotlin
// settings.gradle.kts
include(":core")
include(":ui")
include(":business")
// point projectDir at the fk-android modules if needed
```

```kotlin
dependencies {
  implementation(project(":business"))
}
```

---

## Option D — JitPack (GitHub → AAR)

Convenient for early integration from a public Git tag. Prefer Options A/B for production once coordinates are stable.

1. Use a release tag such as `0.1.0` (no `v` prefix).
2. In the host app:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}
```

JitPack coordinates follow the GitHub path (not `com.fk.android`). Prefer `publishToMavenLocal` or a real Maven host until a dedicated JitPack module layout is documented.

---

## Versioning & release

- **0.x** = public API may still change; pin exact versions in apps.
- Bump `FK_VERSION_NAME` in `gradle.properties` for each release.
- Tag Git with the same version string (**no `v` prefix**), e.g. `0.1.0`, on `main` after merging from `develop`.
- Publish AARs (`./scripts/publish-local.sh` or `./scripts/publish-github-packages.sh`), then announce coordinates + [CHANGELOG.md](../CHANGELOG.md).
- Keep `:sample` green:

```bash
./gradlew :business:assembleRelease :sample:assembleDebug
```

See [releasing.md](releasing.md) for the full release checklist.

---

## ProGuard / R8

Each library ships `consumer-rules.pro`. Host apps using R8 full mode should keep those consumer rules (Gradle merges them automatically for `implementation` dependencies).

---

## See also

- Root [README.md](../README.md) — modules overview
- [releasing.md](releasing.md) — publish & tag workflow
- Per-package `README.md` under `core/`, `ui/`, `business/` sources
