@AGENTS.md

## Claude

- Check `git status --short --branch` before editing; one agent edits this checkout at a time.
- Keep this file and `AGENTS.md` short: they load every turn. Put history and feature details in `docs/`,
  long repeatable procedures in `.claude/skills/<name>/SKILL.md`, and folder-specific pitfalls in
  `.claude/rules/*.md` with `paths:`.
