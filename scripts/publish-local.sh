#!/usr/bin/env bash
# Publish :core, :ui, and :business to the local Maven repository (~/.m2).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

VERSION="$(grep -E '^FK_VERSION_NAME=' gradle.properties | cut -d= -f2-)"
GROUP="$(grep -E '^FK_GROUP_ID=' gradle.properties | cut -d= -f2-)"

echo "==> Publishing ${GROUP}:{core,ui,business}:${VERSION} to mavenLocal()"
./gradlew \
  :core:publishToMavenLocal \
  :ui:publishToMavenLocal \
  :business:publishToMavenLocal \
  "$@"

echo "==> Done. Artifacts under ~/.m2/repository/${GROUP//.//}/"
echo "    Add mavenLocal() to the host app repositories, then:"
echo "    implementation(\"${GROUP}:business:${VERSION}\")"
