#!/usr/bin/env bash
# Publish :core, :ui, and :business to GitHub Packages.
#
# Auth (first match wins per Gradle credentials):
#   - gpr.user / gpr.key in ~/.gradle/gradle.properties
#   - GITHUB_ACTOR / GITHUB_TOKEN environment variables
#
# PAT scopes: write:packages, read:packages (and repo access if the package is private).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VERSION="$(grep -E '^FK_VERSION_NAME=' gradle.properties | cut -d= -f2-)"
GROUP="$(grep -E '^FK_GROUP_ID=' gradle.properties | cut -d= -f2-)"

if [[ -z "${GITHUB_TOKEN:-}" && -z "${gpr_key:-}" ]]; then
  if ! grep -qE '^gpr\.key=' "${HOME}/.gradle/gradle.properties" 2>/dev/null; then
    echo "error: set GITHUB_TOKEN (and GITHUB_ACTOR) or gpr.user/gpr.key before publishing." >&2
    exit 1
  fi
fi

echo "==> Publishing ${GROUP}:{core,ui,business}:${VERSION} to GitHub Packages"
./gradlew \
  :core:publishReleasePublicationToGitHubPackagesRepository \
  :ui:publishReleasePublicationToGitHubPackagesRepository \
  :business:publishReleasePublicationToGitHubPackagesRepository \
  "$@"

echo "==> Done. Consumers need the GitHub Packages Maven repo + credentials (see docs/installation.md)."
