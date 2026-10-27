# Shenfa to-do (updated 27 Oct 2026, evening)

Shenfa is the animation engine (`com.murimblock.shenfa`); combat (`com.murimblock.combat`, `combat-system.md`) builds
on it. `engine-roadmap.md` is the record of what was built and how; `epic-fight-study.md` holds what Epic Fight does.
"EF" marks an idea taken from it, "beyond EF" what it lacks.

## Done

- Skeleton, skinned body and armor from glTF, CPU skinning; player renderer matching vanilla in every state.
- Clips, animation player, animator (layers by priority, masks by motion, eased transitions, additive layers, additive
  composite layers, mirroring, an outside base pose); clips from Blender (glTF) or keyed in JSON on a model's parts.
- Walking with planted feet; first person with the body, the camera at the head.
- 1.1 Events and root motion: clip events (sound, particle, footstep, shake, callback), footsteps from planted feet,
  root motion for the local player (and for mobs on the server).
- 1.2 Any skeleton: every mob is driven by Shenfa (`MobShenfa`, a renderer mixin): humanoids and illagers on the
  biped (they play the player's clips), every other model on a skeleton of its own parts (`PartRig`).
- 1.3 First person, second pass (part): the fists raised in one's own view in combat; ours steps aside when the
  "First-person Model" mod is loaded.
- 1.4 Network: server-side clip library (from the mod's files), play/stop of any layer for any body, combat mode,
  hits, the local player's moves predicted and corrected by the server.
- 1.5 Cloth: the cape as position-based cloth (beyond vanilla's board), test cape `/skeletal cape test`.
- 1.7 Performance (part): 50 animated players at 25 to 35 µs each to draw, 113 fps (vanilla's players: 108 to 117).
- Combat (EF-inspired, first version): combat mode, fists and blade movesets with combos, dash and air strikes,
  techniques for Qi, dodges with invulnerability, swept colliders, damage with cultivation, staggers by impact against
  stun armor, hit stop, guard for Qi with guard break, parry, 8-tick buffer, gap closing toward the target.
- Mobs: 24 vanilla mobs patched; 18 checked in game landing their Shenfa attack at the clip's window.
- Feel: hit stop, camera shake, flash, sparks and sounds by result, trails on fists and blades, lock-on (flick to
  switch, retarget after a kill), Qi bar in combat mode.
- Checks: 94 unit tests; in-game groups render, clips, motions, vanilla, player, events, mobs, combat, film, perf,
  cloth: 156 checks pass in one run; contact sheets of the fighting clips and of 18 mob attacks
  (`run/engine-test/screenshots/film`). A dedicated server starts with the mod.

## Left (in order)

1. Tuning with Enzo in game: every number of `combat-system.md`, the clips' poses (drafts keyed blind, D2: Enzo fixes
   them in Blender), the reaches, the Qi costs (mortals have 100 Qi and gain 0.5 a second).
2. First person: per-clip first-person settings (camera track, part visibility), bow and crossbow seen as others see
   them, a check with Iris shaders, turning the First-person Model mod off instead of stepping aside.
3. Network: a second client and a lag simulator (the duel test at 0 and 150 ms was not run: one client only here).
4. Cloth: robes, sashes and hair ribbons once murim outfits exist; collisions with the arms.
5. Walking leftovers: stairs still turn a leg up to about 78° for a frame; the knee peaks 35 to 55° a frame on the
   flat.
6. Compatibility: Iris, Sodium, resource packs changing mob models (vanilla's model is driven, so they should work),
   OptiFine; the lotus pose and the trial's getting up (PAL, BentLimbsLayer) move to Shenfa.
7. Combat, more: grabs and throws for the fists, spears, bows and crossbows, mounted strikes, techniques tied to
   cultivation and the martial arts of the System screen, guard and dodges for mobs, boss phases (Ender Dragon,
   Wither), the creeper, blaze, slime, phantom and guardian patched, datapack movesets with an editor.
8. Feel, more: slow motion on a finishing blow, an over-the-shoulder camera, hit sparks as Photon effects.
9. Tools: a dev screen to play and scrub clips, an emote wheel, an event API for other mods.
