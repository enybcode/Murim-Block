#!/usr/bin/env bash
# The two-client check of the Azure Cloud Sword Art (MpTourServer, MpTourClient): a dedicated server, then two real
# clients (MurimA attacks, MurimB takes it and guards), each following the phases the server announces. Prints the
# three reports; exits non-zero if any failed. Development only (-PgameTests). Usage: bash scripts/mp/run-mp-tour.sh
set -u
cd "$(dirname "$0")/../.."
GRADLE=./gradlew
[ -f gradlew.bat ] && [ "$(uname -o 2>/dev/null)" = "Msys" ] && GRADLE=./gradlew.bat
OUT=build/mp-run
mkdir -p "$OUT"
"$GRADLE" -PgameTests compileJava compileGameTestJava --console=plain -q || exit 1

"$GRADLE" -PgameTests runMpServer --console=plain > "$OUT/server.out" 2>&1 &
SERVER=$!
# Wait for the server to be up (at most 5 minutes).
for i in $(seq 1 300); do
  grep -q 'Done (' "$OUT/server/logs/latest.log" 2>/dev/null && break
  sleep 1
done
grep -q 'Done (' "$OUT/server/logs/latest.log" 2>/dev/null || { echo "server did not start"; kill $SERVER; exit 1; }

"$GRADLE" -PgameTests runMpClientA --console=plain > "$OUT/a.out" 2>&1 &
A=$!
sleep 20
"$GRADLE" -PgameTests runMpClientB --console=plain > "$OUT/b.out" 2>&1 &
B=$!

wait $A; wait $B; wait $SERVER
status=0
for r in "$OUT/server/mp-tour-server.txt" "$OUT/a/mp-tour-client.txt" "$OUT/b/mp-tour-client.txt"; do
  echo "== $r"
  if [ -f "$r" ]; then cat "$r"; grep -q '^PASSED' "$r" || status=1; else echo "(missing)"; status=1; fi
done
exit $status
