# Shenfa to-do (rewritten 27 Oct 2026)

Shenfa is the animation engine (`com.murimblock.shenfa`); combat builds on it. This list is the plan;
`engine-roadmap.md` stays as the record of what was built and how, `epic-fight-study.md` holds what Epic Fight does.
"EF" marks an idea taken from it, "beyond EF" what it lacks.

## Done

- Skeleton (EF's 20-joint layout), skinned body and armor from glTF (Blender), CPU skinning.
- Player renderer matching vanilla: pixel-compared from six sides, every vanilla state (sleeping, elytra, riptide,
  riding, swimming, climbing, spectator), item poses and the attack swing, vanilla layers, armor with its own model.
- Clips, animation player, animator: layers by priority, joint masks by motion, eased transitions with no pop or
  flip, additive layers, mirroring; living motions read from the body.
- Walking: planted feet (two-bone IK, soft near full stretch), cadence measured from the clip, legs turned toward
  the motion, backward playback, stairs, slabs, ledges, turning in place. Slide down from 2.6 to 0.05 blocks a
  second. Beyond EF (no foot IK there).
- First person: the body drawn from the same pose, vanilla hands outside combat, our arms in combat, camera at the
  head; hands never nearer than 0.33 block at any field of view.
- Tools: Blender scripts (clips from keyframes, export, render), engine test with pixel compares and the
  real-player tour (joint turns, foot slide, ground offset). 84 unit tests, 91 in-game checks.

## Decisions (details in `engine-roadmap.md`)

- D1 our own skeleton and glTF; D2 Claude drafts clips in Blender, Enzo fixes by eye; D4 key B for combat mode;
  D5 dodge dodges melee and projectiles, not explosions; D6 Qi replaces stamina.
- D3 widened (27 Oct): every mob on Shenfa, non-humanoids on skeletons of their own; GeckoLib only for non-combat
  models.

## Open checks

- Enzo plays the walk and first person by hand (only scripted runs so far).
- Armor from a real mod with its own model (only our test model so far).

## 1. Engine, before combat (in this order)

1. Events and root motion. Done 27 Oct: clip events (sound, particle at a joint, footstep, camera shake, server
   callback, each client/server/both), step sounds from planted feet with vanilla's muted, root motion for the
   local player (ground or vertical, gravity-off windows, walls stop it), the `fists/step_in` clip. Left, with
   combat or network:
   - Photon effects as an event type; server events and callbacks run once the server plays clips (step 4).
   - Root motion toward a target (capped), facing and camera sync (with lock-on); the server decides and the local
     player is predicted and corrected (step 4).
2. Any skeleton, not only the biped (needed by D3).
   - Loader, clips and animator already take any armature. To widen: the renderer (players only today), a mob
     renderer on Shenfa, FootPlanter and LegIk for any number of legs (spider, wolf), motions per skeleton.
   - First test: one humanoid mob (zombie) on the biped, one non-humanoid (spider) on its own skeleton.
3. First person, second pass (EF's "pov").
   - Per-clip first-person settings: camera track, part visibility, root in camera or world space, view limits,
     an optional first-person-only clip.
   - Combat stance raised in view (the fists sit low at rest) without changing what others see.
   - Bow and crossbow aiming seen as others see them.
   - Turn the "First-person Model" mod off while ours is on; check with Iris (EF's known bug).
4. Network sync: clip ids as small integers, play/stop/motion payloads, late joiners get the state, prediction with
   server confirm, a lag simulator, a duel test at 0 and 150 ms.
5. Cloth (EF has position-based cloth for capes): capes, robe tails, sashes, hair ribbons. Beyond EF for outfits.
6. Walking leftovers: stairs and slabs still turn a leg up to about 78° for a frame or two; the knee peaks 35 to 55°
   for a frame on the flat; the boxing shuffle slides (set with the combat movement speeds).
7. Performance and compatibility, with EF's weaknesses as the checklist.
   - 50 animated bodies in view measured; skinning cached when nothing moves; GPU skinning if the CPU cost shows.
   - Iris and shader packs (first-person rotation, armor glint, translucent armor), Sodium, resource packs that
     change player or mob models (fall back to vanilla), OptiFine declared incompatible if it breaks.
   - Few, soft mixins (events and reflection so far).
   - The lotus pose and the trial's getting up (BentLimbsLayer) move from PAL to Shenfa.

## 2. Combat (to redesign with Enzo before any code)

Combat and weapons will be reworked in depth; the pieces below are the menu, nothing is decided.

- Modes: vanilla and combat (D4), EF's automatic switch by held item.
- Strikes as phases (EF): start, windup, active, recovery, end, several per clip, each with its hand, colliders and
  properties; windows that lock movement, turning, combos, skills, item use; super armor and invulnerable windows.
- Hits: swept colliders (box, multi-box, line for spears, plane) between poses, one hit per target per phase, max
  targets, line of sight; the server evaluates poses from the same clips.
- Input: 8-tick buffer (EF), combo chains with dash and air variants, charged strikes (Qi).
- Reactions: EF's stun types plus launch and reactions by the side hit; stun time from impact against stun armor;
  knockback along the blow.
- Defense: guard with growing penalty and guard break, parry window (EF: 8 ticks), dodge step and roll (D5),
  knockdown wake-up; Qi as the cost (D6).
- Attributes: impact, armor negation, max strikes, weight and stun armor from armor, tied to cultivation.
- Techniques: murim techniques charged by landing blows (EF's innate skills), passives and a tree tied to
  cultivation rather than skill books.
- Grabs and throws (EF has grappling): clinch and throws for the fist style.
- Weapons after fists: swords, spears, bow and crossbow, mounted strikes.
- Data: moves, weapons and mob patches as datapacks reloaded with `/reload`; an in-game editor later.

## 3. Mobs on Shenfa (after the combat design)

- Every vanilla mob's animations redone, attacks included, as EF does (Enzo, 27 Oct): idle, walk, run, attacks as
  phases with colliders, hit reactions, death; combat behavior as data (EF's mob patches: weights, cooldowns,
  interruptible, conditions on distance, angle, health, chance). Check EF's `epicfight_mobpatch` list.
  - Biped skeleton: zombie, husk, drowned, zombie villager, zombified piglin, skeleton, stray, wither skeleton,
    piglin, piglin brute, vindicator, pillager, evoker, witch, illusioner.
  - Skeletons of their own: spider, cave spider, creeper, enderman, iron golem, ravager, hoglin, zoglin, blaze,
    slime, magma cube, phantom, guardian, warden, wolf, polar bear, goat, bee; bosses Ender Dragon and Wither
    with phases.
- Our own mobs (bandits, masters, beasts, the Grave Warden boss) on the same system.

## 4. Feel (where Shenfa should beat EF)

- Hit stop: attacker and target freeze 2 to 5 ticks by impact, synced. Beyond EF.
- Camera shake by impact, a short flash on heavy blows, slow motion on a finishing blow. Beyond EF.
- Trails on fists and weapons from clip events (EF), hit sparks by impact type, ground slam, blood as an option,
  afterimages for qinggong and fast techniques.
- Sounds per weapon and impact, swing sounds per phase (EF), footsteps (beyond EF).
- Camera: over-the-shoulder third person, lock-on (EF: range 20, flick to switch, retarget after a kill),
  8-direction movement; off when Shoulder Surfing or Better Third Person is present.
- Tuning pass with Enzo in game.

## 5. Tools and data

- Clips from Blender as glTF (done): Enzo edits in Blender, export scripts keep the sidecars.
- A dev screen to play and scrub clips in game (EF has a datapack editor).
- An emote wheel later, an event API for other mods.
