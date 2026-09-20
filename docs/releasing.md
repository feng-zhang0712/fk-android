# Releasing

Checklist for publishing **fk-android** so apps can install it **from a Git tag** (JitPack), like an iOS SPM package URL + version tag.

## Coordinates

| Context | Example |
|---------|---------|
| Git tag / `FK_VERSION_NAME` | `0.1.2` (no `v` prefix) |
| JitPack (open source, recommended now) | `com.github.feng-zhang0712.fk-android:business:0.1.2` |
| Maven Central (planned) | `com.fk.android:business:0.1.2` |
| GitHub Packages | `com.fk.android:business:0.1.2` (auth often required to download) |

Published modules: `core`, `ui`, `business`. `:sample` is never published.

## Pre-release

1. Land work on `develop`.
2. Bump `FK_VERSION_NAME` in `gradle.properties`.
3. Update [CHANGELOG.md](../CHANGELOG.md).
4. Verify:

```bash
./gradlew :core:assembleRelease :ui:assembleRelease :business:assembleRelease :sample:assembleDebug
```

5. Smoke-test `:sample` if the release touches user-facing UI.

## Ship a remote-installable tag (JitPack)

JitPack builds AARs **from the GitHub tag**. You do **not** upload AARs manually.

```bash
# After release commit is on main:
git tag -a 0.1.2 -m "Release 0.1.2"
git push origin main
git push origin 0.1.2
```

Then open [https://jitpack.io/#feng-zhang0712/fk-android](https://jitpack.io/#feng-zhang0712/fk-android), select the tag, and wait for a green build (or let the first consumer sync trigger it).

Config used by JitPack: [`jitpack.yml`](../jitpack.yml) (JDK 17 + `publishLibrariesToMavenLocal`).

Consumers follow [installation.md](installation.md).

## Optional — GitHub Packages

```bash
export GITHUB_ACTOR=YOUR_GITHUB_USERNAME
export GITHUB_TOKEN=YOUR_GITHUB_PAT   # write:packages
./scripts/publish-github-packages.sh
```

CI: [`.github/workflows/publish.yml`](../.github/workflows/publish.yml) runs on `N.N.N` tags.

Prefer JitPack for public open-source apps (no download token).

## Optional — local author check

```bash
./scripts/publish-local.sh
```

## Planned — Maven Central

Goal: consumers use `implementation("com.fk.android:business:<version>")` with only `mavenCentral()`.

Requires (one-time, on your Sonatype / Central Portal account):

1. Register at [Maven Central Portal](https://central.sonatype.com/) and claim namespace `com.fk.android` (domain or GitHub proof).
2. Create a publishing user token; store as CI secrets (never commit).
3. Add GPG signing + Central publish plugin / portal upload to this repo.
4. Publish from a tagged release; verify the artifact page on Central.

Until that is wired, **JitPack is the supported remote install path.**

## Git flow reminder

```bash
git checkout main
git pull origin main
git merge develop
git tag -a 0.1.2 -m "Release 0.1.2"
git push origin main
git push origin 0.1.2
git checkout develop
```

## Post-release

1. Confirm JitPack build is green for the new tag.
2. Point host apps (e.g. sactrain) at the new tag coordinate.
3. Announce changelog.
