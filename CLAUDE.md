@AGENTS.md

## Claude

- Check `git status --short --branch` before editing; one agent edits this checkout at a time.
- Enzo works on two PCs, rarely on at the same time, synced through GitHub (`docs/two-pcs.md`). At the start of a
  session, before any edit: `git pull --ff-only origin main`. Never leave work unpushed at the end of a turn.
- Keep this file and `AGENTS.md` short: they load every turn. Put history and feature details in `docs/`,
  long repeatable procedures in `.claude/skills/<name>/SKILL.md`, and folder-specific pitfalls in
  `.claude/rules/*.md` with `paths:`.
- Build the CurseForge tester zip (`run/release/`) only when Enzo explicitly asks for it, never on your own.
