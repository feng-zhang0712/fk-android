# Releasing

Checklist for publishing **fk-android** library modules.

## Coordinates

| Property | Location | Example |
|----------|----------|---------|
| `FK_GROUP_ID` | `gradle.properties` | `com.fk.android` |
| `FK_VERSION_NAME` | `gradle.properties` | `0.1.0` |

Published artifacts:

- `com.fk.android:core`
- `com.fk.android:ui`
- `com.fk.android:business`

Git tags use the **same** version string as `FK_VERSION_NAME` and **must not** use a `v` prefix (e.g. `0.1.0`, not `v0.1.0`).

## Pre-release

1. Work lands on `develop` via focused PRs.
2. Bump `FK_VERSION_NAME` if this is not already the target version.
3. Update [CHANGELOG.md](../CHANGELOG.md) for the release.
4. Verify builds:

```bash
./gradlew :core:assembleRelease :ui:assembleRelease :business:assembleRelease :sample:assembleDebug
```

5. Smoke-test `:sample` on a device or emulator for the verticals you care about.

## Publish locally

```bash
./scripts/publish-local.sh
```

Installs AARs + sources JARs into `~/.m2/repository/com/fk/android/`.

## Publish to GitHub Packages

Requires a GitHub PAT with `write:packages` (and `read:packages`).

```bash
export GITHUB_ACTOR=YOUR_GITHUB_USERNAME
export GITHUB_TOKEN=YOUR_GITHUB_PAT
./scripts/publish-github-packages.sh
```

Or set `gpr.user` / `gpr.key` in `~/.gradle/gradle.properties`.

CI: pushing an annotated tag matching `N.N.N` runs [`.github/workflows/publish.yml`](../.github/workflows/publish.yml).

## Git: merge, tag, push

```bash
# On develop — commit release prep, then:
git checkout main
git pull origin main
git merge develop
git tag -a 0.1.0 -m "Release 0.1.0"
git push origin main
git push origin 0.1.0
git checkout develop
git merge main   # optional: keep develop aligned
git push origin develop
```

## Post-release

1. Confirm packages appear under the GitHub repo **Packages** tab (if using GitHub Packages).
2. Announce coordinates + changelog to host-app teams.
3. Open `develop` for the next `FK_VERSION_NAME` bump when work resumes.
