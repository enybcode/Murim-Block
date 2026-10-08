# GitHub Workflow

- Work in this IntelliJ project, not the separate MCreator workspace.
- Use `test` for changes awaiting the user's in-game validation.
- For each completed change, run appropriate checks, commit the relevant files, and push `origin/test`.
- Keep unrelated user changes and local reference files out of the commit.
- Open or update a pull request from `test` to `main` and report its link.
- `main` is the stable, validated branch. Passing automated tests is not user approval.
- Merge the pull request into `main` only after the user explicitly validates the change.
- Keep `test` after merging so it remains available for the next change.
- Never force-push or discard existing work to synchronize these branches.

# In-Game Text

- Use English for all player-facing mod text, including GUI labels, status names, keybinds and command feedback.
- Keep translatable labels in `assets/murimblock/lang/en_us.json`; do not reintroduce French UI strings.
- Keep serialized IDs and saved player data stable when changing display text.
- Keep the manuscript font scoped to Murimblock screens rather than replacing Minecraft's global font.

# Verification And Cloud

- Read `docs/README.md` for current documentation and `docs/CLOUD_IMPORT.md` for cloud setup.
- Local checks: `./gradlew build compileGameTestJava`, then `./gradlew -PgameTests runGameTestServer` (use `gradlew.bat` on Windows).
- Cloud checks: `bash scripts/cloud/verify.sh`; use `bash scripts/cloud/gradle.sh` for subsequent Gradle commands so the writable cache and managed Java proxy remain configured.
- Preserve the platform's proxy and CA trust. Never disable TLS verification or bypass a blocked domain.
- Keep dev-only tests in `src/gameTest`; never enable `-PgameTests` for a normal player client or package the harness in the release jar.
- Cloud server checks do not replace graphical client checks or two-client multiplayer validation.
- Never upload local worlds, IDE state, caches, credentials or `.local` reference files as repository source.
