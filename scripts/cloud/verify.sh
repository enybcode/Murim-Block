#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"
cd "$root"
bash scripts/cloud/setup.sh
mkdir -p build/cloud-verification
bash scripts/cloud/gradle.sh --no-daemon --max-workers=2 -PgameTests runGameTestServer 2>&1 \
  | tee build/cloud-verification/gametest-output.log
grep -Eq 'All [1-9][0-9]* required tests passed' build/cloud-verification/gametest-output.log
