#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"
cd "$root"
node --test scripts/cloud/configure-gradle.test.cjs
exec bash scripts/cloud/gradle.sh --no-daemon --max-workers=2 build compileGameTestJava
