---
name: reference-animation
description: Make or redo a player combat animation from reference clips Enzo likes (Roblox battlegrounds, Epic Fight,
  others), measured frame by frame and compared side by side with our in-game film. Use for any player move, M1 or
  skill, and when Enzo sends a combat video as reference.
---

# Player animation from references

The method svasco used for his combat (31 Oct 2026, `docs/combat/animation-method.md`): copy the feel of a clip,
never its animation. Enzo picks the clips; the work is measured, not guessed.

## 1. Gather

- Put the clips in `run/references/<move>/` (out of git: never commit someone else's footage).
- Write `docs/combat/references/<move>.md`: each clip (game, where to find it, seconds used), what Enzo likes, what he
  does not, the feel aimed for. Ask him for these if he did not say.

## 2. Measure

- `sh tools/reference/sheet.sh <clip> <start> <seconds> run/references/<move>/sheet.png [crop w:h:x:y]`: one picture
  per game tick. Crop around the body: frames are cheap to read only when small and few.
- For each strike, note in ticks: windup, active (contact), hold on impact, recovery, gap to the next strike, and
  what moves first (feet, hips, shoulder, arm, wrist). Note the dynamic layer too (below).
- The timing table goes in the reference note.

## 3. Storyboard

- Board in `docs/mobs/boards/` as usual (`board.js`), but its timings are the measured ones, and each panel names the
  reference tick it comes from. Enzo validates it before any animation (CLAUDE.md rule).

## 4. Animate

- Clips in `tools/blender/murim_clips.py`, phases in the set's sidecar json, then export (`docs/combat/` and the
  fists work of 31 Oct for the commands).

## 5. Compare

- Film the move in game from the reference's angle (third person behind, or a side view: `GifRecorder` scenes),
  `gradlew runEngineTest -PengineGroups=combat -PengineGifs=true`.
- `sh tools/reference/side-by-side.sh <clip> <start> <seconds> run/engine-test/screenshots/gif/<scene> <out.gif>`,
  both starting on the guard before the first strike. Fix timings tick by tick until they line up; send Enzo the
  side by side (SendUserFile), not a description.

## The dynamic layer (check every move)

A strike reads strong when all of these agree, not from the arm alone:

- The whole body: a step or a weight shift, hips then shoulder then arm then wrist; the head and free hand follow.
- Anticipation readable in 1 to 3 ticks, contact held (hit stop, `CombatEffects`), follow-through past the target.
- Trail on the striking hand or blade (`Trails`), sparks and sound at contact, camera shake (`CameraShake`).
- The target reacts to match the blow: flinch, stagger, knockback, launch, knockdown (`combat/` reaction clips).
- Ground effects for heavy blows: dust, cracks, debris (Photon).

Keep the Murim taste: take the rhythm and weight of the reference, not a flood of effects.
