# Shenfa to-do (rewritten 27 Oct 2026)

Shenfa is the animation engine (`com.murimblock.shenfa`); combat builds on it. This list replaces the step plan of
`engine-roadmap.md`, which stays as the record of what was built and how. `epic-fight-study.md` holds what Epic
Fight does; "EF" below marks an idea taken from it, "beyond EF" what it lacks.

## Done

- Skeleton (Epic Fight's 20-joint layout), skinned body and armor from glTF (Blender), CPU skinning, player
  renderer matching vanilla (pixel-compared from six sides, every vanilla player state, item poses, vanilla layers,
  armor with its own model).
- Clips, animation player, animator (layers by priority, joint masks by motion, eased transitions with no pop,
  additive layers, mirroring), living motions read from the body, first clips (motions, fists set).
- Walking: planted feet (two-bone IK, soft near full stretch), cadence measured from the clip, legs turned toward
  the motion, backward playback, stairs, slabs, ledges, turning in place. Beyond EF (no foot IK there).
- First person: the body drawn from the same pose, vanilla hands outside combat, our arms in combat, camera at the
  head. Same idea as EF's first-person model.
- Tools: Blender scripts, the engine test (pixel compares, real-player tour with joint and foot measures).

## 1. Engine, before combat (in this order)

1. Events and root motion (old step 6).
   - Timed events in the sidecar JSON: sound, particle, Photon effect, camera shake, footstep, server callback; a
     side for each (client, server, both).
   - Footstep events from planted feet (FootPlanter knows when a foot lands): step sounds per block. Beyond EF.
   - Root motion like EF's properties: vertical movement, gravity-off windows, fixed move distance, destination
     provider (toward a target, capped), facing provider, camera sync; walls stop it (`Entity.move`).
   - Server authority, the local player predicted and corrected smoothly.
2. First person, second pass (EF's "pov").
   - Per-clip first-person settings in the sidecar: a camera track, part visibility, root in camera or world space,
     view limits while the clip plays, an optional first-person-only clip.
   - The combat stance raised in view (the fists sit low at rest), without changing what others see.
   - Bow and crossbow aiming seen as others see them (EF ships pov clips for these only).
   - Turn the "First-person Model" mod off while ours is on, as EF does; check with Iris shaders (EF's known bug).
3. Network sync (old step 8): clip ids as small integers, play/stop/motion payloads, late joiners get the state,
   prediction with server confirm, a lag simulator, duel test at 0 and 150 ms.
4. Cloth (EF has a position-based cloth simulator for capes): capes, robe tails, sashes and hair ribbons of murim
   outfits hanging and swinging with the body. Beyond EF for outfits.
5. Walking leftovers: stairs and slabs still turn a leg up to about 78° for a frame or two; the knee peaks 35 to
   55° a frame on the flat; the boxing shuffle slides (fixed by combat movement speeds).
6. Performance and compatibility (old step 16), with EF's known weaknesses as the checklist:
   - 50 animated bodies in view measured; skinning cached when nothing moves; GPU skinning if the CPU cost shows.
   - Iris and shader packs (first-person rotation, armor glint, translucent armor), Sodium, resource packs that
     change player models (fall back to vanilla), OptiFine declared incompatible if it breaks.
   - Fewer and softer mixins (we use events and reflection so far).
   - The lotus pose and the trial's getting up (BentLimbsLayer) move to Shenfa.

## 2. Combat (to redesign with Enzo before any code)

Combat and weapons will be reworked in depth. The pieces below are the reference to choose from; nothing here is
decided.

- Modes: vanilla and combat (D4: key B), and EF's automatic switch by held item (combat items, mining items).
- Strikes as phases (EF): `start, windup, active, recovery, end`, several per clip, each with its hand, colliders
  and properties; the windows lock movement, turning, combos, skills, item use; super armor and invulnerable
  windows (ours).
- Hits: swept colliders (box, multi-box, line for spears, plane), interpolated between poses, one hit per target per
  phase, max targets, line of sight; the server evaluates poses from the same clips.
- Input: an 8-tick buffer (EF), combo chains with dash and air variants, charged strikes (Qi).
- Reactions: EF's stun types plus ours (launch, directional reactions by the side hit); stun time from impact
  against stun armor; knockback along the blow.
- Defense: guard with growing penalty and guard break, parry window (EF: 8 ticks), dodge step and roll with
  invulnerability (D5), knockdown wake-up; Qi as the cost (D6).
- Attributes: impact, armor negation, max strikes, weight and stun armor from armor (EF), tied to cultivation.
- Techniques: weapon innate skills charged by landing blows (EF) become murim techniques; passives and a tree tied
  to cultivation rather than skill books.
- Grabs and throws (EF has grappling animations): clinch and throws for the fist style.
- Mobs on Shenfa (D3): EF-style data patches with a `combat_behavior` list (weights, cooldowns, interruptible,
  conditions on distance, angle, health, chance); bosses with phases (the Grave Warden).
- Vanilla mobs (Enzo, 27 Oct 2026): redo every vanilla mob's attack animations, as Epic Fight does, on Shenfa.
  - Humanoids on the biped skeleton (D3): zombie, husk, drowned, zombie villager, zombified piglin, skeleton,
    stray, wither skeleton, piglin, piglin brute, vindicator, pillager, evoker, witch, illusioner.
  - The others need their own skeletons in Shenfa (D3 said GeckoLib for them; to decide again): spider, cave
    spider, creeper, enderman (biped proportions, own skeleton), iron golem, ravager, hoglin, zoglin, blaze, slime
    and magma cube, phantom, guardian, warden, wolf, polar bear, goat, bee, and the bosses (Ender Dragon, Wither).
  - Each one: idle, walk, run, its attacks as phases with colliders, hit reactions, death; its combat behavior as
    data (EF's mob patches). Check against EF's own list (`epicfight_mobpatch`) which ones it covers.
- Mounted strikes, bow and crossbow, spears and swords after fists.
- Data: moves, weapons and mob patches as datapacks reloaded with `/reload`; an in-game editor later.

## 3. Feel (where Shenfa should beat EF)

- Hit stop: attacker and target freeze 2 to 5 ticks scaled by impact, synced. Beyond EF (absent from its core).
- Camera shake scaled by impact, a short flash on heavy blows, slow motion on a finishing blow. Beyond EF.
- Trails along fists and weapons from clip events (EF), hit sparks by impact (cut, blunt), ground slam, blood as an
  option, afterimages for qinggong and fast techniques (EF has afterimages).
- Sounds per weapon and impact, swing sounds per phase (EF), footsteps (beyond EF).
- Camera: over-the-shoulder third person, lock-on (EF: range 20, quick shift with a mouse flick, retarget after a
  kill), 8-direction movement, off when Shoulder Surfing or Better Third Person is present (EF).
- Tuning pass with Enzo in game.

## 4. Tools and data

- Clips from Blender as glTF (done; EF's exporter targets Blender 2.79): Enzo edits the clips in Blender, export
  scripts keep the sidecars.
- A dev screen to play and scrub clips in game (EF has a datapack editor).
- An emote wheel later (EF 21.15), an event API for other mods.
