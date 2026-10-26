# Two PCs

Enzo develops on two Windows PCs, rarely on at the same time. Each has its own checkout of `main-V2`; GitHub
(`enybcode/Murim-Block`) is the link between them. Claude sessions, Claude's memory and the `run/` folder (test
worlds, game options) stay on each PC; everything that matters is in the repo (`AGENTS.md`, `CLAUDE.md`, `docs/`).

## Every session

- Start: `git pull --ff-only origin main-V2` before any edit. A SessionStart hook in `.claude/settings.json` does it
  for every Claude session opened in the project (both PCs, since the file is committed); `CLAUDE.md` asks for it too.
- End: every verified change is already pushed to `origin/main-V2` (project rule). Nothing stays local.
- If a pull is refused because both PCs committed, rebase the local commits on `origin/main-V2`, never force-push.

## Phone

New sessions connect to Remote Control (Code tab setting "Connect new sessions to Remote Control", on), so they
show on claude.ai/code and in the Claude mobile app while their PC is on and the app open. "Keep computer awake
while Claude works" is on too. A PC that is off or asleep cannot be reached from the phone.

## Setting up the second PC

Paste the prompt below into a new Claude Code session (Claude desktop app, Code tab), opened in the `Documents`
folder of the second PC. Enzo signs in to GitHub himself when Git asks, on the first push.

```text
Set up this PC to develop the Minecraft mod Murimblock V2, like my other PC. Reply in French. Steps:

1. Check that Git, Java 21 and Blender 5.2 are installed (`git --version`, `java -version`,
   `C:\Program Files\Blender Foundation\Blender 5.2\blender.exe`). If one is missing, tell me and install it with
   winget once I say yes (Git.Git, EclipseAdoptium.Temurin.21.JDK, BlenderFoundation.Blender). Blender builds the
   combat engine's models (`tools/blender/README.md`).
2. Clone https://github.com/enybcode/Murim-Block.git, branch main-V2, into %USERPROFILE%\Documents\MurimblockV2
   (`git clone -b main-V2 ...`). Set the repo's git identity: user.name "enzo bertrand",
   user.email "enzo.berbertrand@gmail.com".
3. In that folder run `gradlew.bat build` (the first run downloads Minecraft and NeoForge, it can take a while; run
   it in the background). Never run `gradlew --stop`. Report whether it passed.
4. Create my personal rules file %USERPROFILE%\.claude\CLAUDE.md (if it exists, show it to me before changing it)
   with exactly this content:

   # Personal Rules (all projects)

   ## Language

   - Reply in French; keep code, commits and in-game text in English unless a project says otherwise.

   ## Spend few tokens

   - Short answers: the result, what was verified, what is left. No long recaps or lists of options I did not ask for.
   - Read only what the task needs: search first (Grep/Glob), then read the relevant lines, not whole large files.
     Do not re-read a file you just wrote or edited.
   - Filter command output: `tail`, `grep` or `-q` on builds and tests; never dump full logs into the conversation.
   - Delegate broad searches or very verbose work (long logs, many files) to a subagent and keep only its conclusion.
   - Do not rerun a full test suite when a targeted check proves the change; run the full suite before pushing.
   - Project CLAUDE.md files stay short (under ~60 lines). History and feature details go in `docs/`, long
     repeatable procedures in `.claude/skills/<name>/SKILL.md`, folder-specific pitfalls in `.claude/rules/` with `paths:`.
     Never `@import` long documents into a CLAUDE.md.
   - When I switch to an unrelated task in a long conversation, suggest starting a new session.
   - Before installing a third-party skill or plugin, show me what it contains (hooks, MCP servers, scripts) and wait for my OK.

5. Turn on the Code tab settings "Connect new sessions to Remote Control" and "Keep computer awake while Claude
   works", and turn Remote Control on for this session, so I can follow sessions from my phone.
6. Tell me in a few lines what is ready, and that from now on I should open my Murimblock sessions in
   %USERPROFILE%\Documents\MurimblockV2 (its CLAUDE.md and AGENTS.md explain the project and the two-PC routine).
```
