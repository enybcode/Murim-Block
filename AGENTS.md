# Branch Colleuhu95 (read first)

- This branch belongs to Colleuhu95, a new contributor creating his own martial technique. Read `docs/COLLEUHU95.md`.
- On this branch, its rules replace the GitHub Workflow below: commit and push only to `origin/Colleuhu95`; never push,
  merge or open work directly on `test` or `main`. When the technique is ready, open a pull request `Colleuhu95` → `test`
  for Enzo (the project owner) to review.

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

# Multiple Coding Agents

- Codex and Claude work against the same repository rules, but do not share chat history automatically.
- In a shared checkout, allow only one agent to edit, stage, commit or change branches at a time. The other can review in read-only mode.
- Before editing, inspect the current branch and working tree again; preserve unfinished changes from the user or another agent.
- For simultaneous implementation, use separate Git worktrees only when requested by the user; never assume uncommitted changes are copied into a worktree.
- Transfer context with a concise handoff: task, branch, commit, changed files, checks actually run, and remaining limitations. Never transfer credentials or local worlds.
- Read `docs/AI_COLLABORATION.md` for the Claude setup and initial read-only verification.
