# Player animation method (31 Oct 2026)

Enzo adopted for the player's moves the method svasco used for his own combat mod: analyse reference clips he likes
(Roblox battlegrounds games, Epic Fight, others), give the key frames, what he likes and not, the timings and the feel
wanted, then film our version from the same angle and compare it side by side with the reference. Only the feel is
copied, never the animation.

Why: our first M1 were posed from memory and took four rounds of "too fast" and "too slow"; a measured reference
gives the timings from the start, and the side by side replaces taste arguments with a visible gap.

What his video showed (fists against a husk, third person): four to five M1 in about 1.2 s with a heavier last one,
every strike moving the whole body (step, hip turn, raised knee); white trails on the fists that linger; blood on the
ground at each blow; the foe launched in the air, followed, slammed down in a dust cloud and struck on the ground.
Much of the weight comes from that layer, not from the poses alone.

How: the procedure is `.claude/skills/reference-animation/SKILL.md`; the tools are `tools/reference/sheet.sh` (one
picture per tick of a clip) and `tools/reference/side-by-side.sh` (both films at 20 fps). Both need ffmpeg. The clips
stay in `run/references/` (not committed); the notes and timings go in `docs/combat/references/`.

Limits: the references are flashier than Minecraft's look, so their rhythm and weight are taken, not their effects;
our clips are posed by hand in Blender, so matching a reference still takes a few rounds.
