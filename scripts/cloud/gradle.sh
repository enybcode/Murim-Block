#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"
cd "$root"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$root/.local/cloud-gradle}"
mkdir -p "$GRADLE_USER_HOME"
if [[ ! -w "$GRADLE_USER_HOME" ]]; then
  printf '%s\n' 'GRADLE_USER_HOME must be writable; no system cache will be modified.' >&2
  exit 1
fi
java_version="$(java -version 2>&1)"
if [[ ! "$java_version" =~ version\ \"21(\.|\") ]]; then
  printf '%s\n' 'Select a Java 21 JDK before running the cloud build.' >&2
  exit 1
fi
node scripts/cloud/configure-gradle.cjs "$GRADLE_USER_HOME"
# Keep inherited proxy variables, CA trust and JAVA_TOOL_OPTIONS. Never disable TLS.
exec bash ./gradlew "$@"
