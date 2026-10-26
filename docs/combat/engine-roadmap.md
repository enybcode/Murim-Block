# Combat engine roadmap

Goal: a combat engine of Epic Fight's level or better, written by us from scratch. Epic Fight is studied for
ideas only: its code is GPL and its assets are All Rights Reserved, so nothing is copied.

Status: steps 0 to 2 done (26 Oct 2026). The first combat system was removed the same day (commit `b27eaf3d`).

## Why Epic Fight feels good (what we must match)

1. The whole body moves: a real skeleton with a skinned mesh, so legs, hips and torso carry each blow and
   elbows and knees bend cleanly.
2. Animations are made by eye in Blender, not typed as angles.
3. Movement comes from the animation (root motion): a step in a strike is a real step, no sliding.
4. Nothing pops: every change of animation blends from the current pose; layers with joint masks mix
   walking and attacking.
5. Short, readable timings (windup, active, recovery) and an 8-tick input buffer.
6. Every blow has a visible result: the target is stunned with a reaction animation scaled by impact.
7. Decisions cost a resource: guard penalty growing with each block, guard break, dodge cost.
8. Camera and lock-on keep the player in control.

## Rules for every step

- Body first: a strike starts from the feet, then weight, hips, torso; the arm comes last.
- Data-driven: skeletons, clips, moves and weapons are files reloaded with `/reload` or resource reload.
- Server authority with client prediction for the local player.
- Each step ends with: unit tests for the pure code, an automated in-game test, GIFs (front, side, three
  quarters, first person when it applies), Enzo's approval, then commit and push.
- Pure math (poses, colliders, timelines) lives in classes without Minecraft types so it can be unit tested.
- No step starts before the previous one is approved, unless the doc says the steps are independent.

## Decisions

| # | Question | Recommended | Status |
|---|----------|-------------|--------|
| D1 | Player model | Our own skeleton and skinned mesh loaded from glTF 2.0 (Blender exports it natively). PAL is no longer used for combat; `AGENTS.md` stack is updated. | accepted 26 Oct |
| D2 | Who animates | Mixed: Claude makes first versions with Blender scripts (headless, renders GIFs), Enzo fixes by eye in Blender. | accepted 26 Oct |
| D3 | Mobs | Humanoid mobs (zombies, bandits, masters) use the same skeleton, moves and reactions; GeckoLib stays for non-humanoid creatures. | accepted 26 Oct |
| D4 | Combat mode key | B toggles combat mode (left click strikes, never mines). | accepted 26 Oct |
| D5 | Dodge invulnerability | Melee and projectiles, not explosions. | accepted 26 Oct |
| D6 | Resource | Qi replaces Epic Fight's stamina; everything special costs Qi. | accepted 26 Oct |

## Milestones

| Milestone | Steps | What Enzo can see |
|-----------|-------|-------------------|
| M1 Body | 0 to 3 | The player drawn by our engine, any skin, a guard pose with clean elbows and knees. |
| M2 Motion | 4 to 8 | Idle, walk, run, jump in our engine, smooth blends, first person, synced in multiplayer. |
| M3 Fists | 9 to 11 | Full boxing slice: stance, chain, dash and air strikes, charged strike, hit reactions, guard, parry, dodge. |
| M4 Feel | 12 to 13 | Shoulder camera, lock-on, trails, sparks, sounds, hit-stop, camera shake. |
| M5 World | 14 to 16 | Humanoid mobs fighting with the engine, swords and spears, tools and performance pass. |

---

## Step 0: Foundations and tooling

Goal: everything needed to build and check the engine before any feature.

Build:
- Packages: `com.murimblock.anim` (skeleton, mesh, clips, animator, pure math), `com.murimblock.anim.client`
  (rendering), `com.murimblock.combat` (fight logic), `com.murimblock.combat.client`.
- Blender pipeline in `tools/blender/` (outside the jar): a script that builds the reference rig
  `murim_biped.blend`, an export script to glTF (`.gltf` + `.bin`) with fixed settings, and a render script that
  films a clip from front, side and three quarters into PNG frames. Check Blender is installed on both PCs;
  document the setup in `docs/two-pcs.md`.
- Reuse `scripts/frames-to-gif.java` for GIFs.
- An in-game test harness (`client/dev/EngineTest`) driven by a system property and a Gradle run config, as
  before: scripted steps, captures, a pass/fail report in the log.

Done when: the Blender scripts run headless on this PC, produce a rig file and a test GIF; the harness runs
an empty test and exits.

Done (26 Oct 2026): Blender 5.2.3 headless on this PC; `tools/blender/` (`murim_biped.py`, `build.py`,
`export.sh`, `render.py`, README); GIFs with `scripts/frames-to-gif.java`; `client/dev/EngineTest` with the
`runEngineTest` Gradle run (folder `run/engine-test`). The second PC needs Blender 5.2 too (`docs/two-pcs.md`).

## Step 1: Skeleton

Goal: one humanoid skeleton shared by players and humanoid mobs.

Joints: Epic Fight's biped layout, copied at Enzo's request on 26 Oct 2026 (structure only, rebuilt by us; their
files are All Rights Reserved), positions rounded to whole pixels (bind pose in Minecraft pixels, 1/16 block):
`Root` (pelvis, y 12, carries root motion) → `Torso` → `Chest` → `Head`; `Chest` → `Shoulder_L/R` (collarbone)
→ `Arm_L/R` (upper arm) → `Hand_L/R` (forearm and hand) → `Tool_L/R` (grip); `Arm_L/R` → `Elbow_L/R` (helper);
`Root` → `Thigh_L/R` → `Leg_L/R` (shin); `Thigh_L/R` → `Knee_L/R` (helper). 20 joints, left side on +X.
Helpers turn half as far as the forearm or shin (`Biped.settleHelpers`), so the hinge of a bent limb stays filled.

Build:
- `Joint` (name, index, parent, inverse bind matrix), `Armature` (joints in parent-first order, lookup by
  name), `JointTransform` (translation, rotation quaternion, scale), `Pose` (one transform per joint),
  `Pose.toModelMatrices()` (local to model space).
- Pose math: interpolate (lerp translation, slerp rotation), overwrite with a mask, add (additive layers),
  mirror left/right.

Done when: unit tests cover parent chains, slerp, mirror and masks; the skeleton from the glTF file matches the
code's expectations (joint names and hierarchy checked at load, clear error otherwise).

Done (26 Oct 2026): `com.murimblock.anim`: `JointTransform`, `Joint`, `Armature`, `JointMask`, `Pose`, `Biped`
(Epic Fight's 20-joint layout; `Biped.check` compares a loaded skeleton). Unit tests in `ArmatureTest` and
`PoseTest`; JOML 1.10.5 and Gson 2.10.1 added to the test classpath.

## Step 2: Mesh and glTF loader

Goal: load the player body as a skinned mesh made in Blender.

Mesh rules (made in Blender on the rig of step 0):
- Same boxes as the vanilla player, same UVs on the 64x64 skin layout, so every skin works unchanged.
- Two variants: wide arms (Steve) and slim arms (Alex), chosen from the player's model type.
- The outer layer (hat, jacket, sleeves, pants) is a second mesh inflated like vanilla (0.5 px for the hat, 0.25 px for the rest), rendered
  translucent, with the same weights.
- Elbows and knees, Epic Fight's way: the limb is two rigid blocks; at the joint a hinge, flat at rest, joins
  them: a strip on the side the joint opens on (back of the elbow, front of the knee) from the upper block's edge
  to the helper's edge to the lower block's edge, plus two triangles on each side from the joint's axis. The
  hinge shows the texel row just below the joint. No stretching anywhere; the sleeve does the same.
- At most 4 joint influences per vertex, weights summing to 1.

Build:
- `GltfLoader`: reads `.gltf` JSON and `.bin` buffers from resources: nodes, the skin (joints, inverse bind
  matrices), mesh primitives (`POSITION`, `NORMAL`, `TEXCOORD_0`, `JOINTS_0`, `WEIGHTS_0`) and animations
  (step 4). Supports the accessor types Blender writes; refuses others with a clear message.
- `SkinnedMesh`: parts by name (body, head, arms, legs, outer layers) so parts can be hidden.
- Files: `assets/murimblock/skeletal/biped.gltf`, `biped_slim.gltf`.

Done when: unit tests load a small fixture file; a validation reports bad weights, missing joints or UVs outside
the skin.

Done (26 Oct 2026): `GltfLoader`, `SkinnedMesh`, `SkinnedModel` (pure, tested on the shipped files: skeleton,
vanilla boxes, weights, UVs, hinges, refusals), `anim/client/SkeletalModels` (reload listener for
`assets/*/skeletal/*.gltf`), files exported by `tools/blender/export.sh`. The body's bottom faces' UV orientation
is to check against vanilla in step 3.

## Step 3: Skinned renderer (third person)

Goal: the player drawn by our engine, identical to vanilla at rest, clean when bent.

Build:
- Linear blend skinning on the CPU each frame into the `VertexConsumer` (positions and normals transformed),
  with light, overlay (red hurt flash) and the player's skin texture. Opaque pass for the base, translucent
  pass for the outer layer.
- In combat mode (later: whenever a skeletal animation plays), cancel the vanilla player render
  (`RenderPlayerEvent.Pre`) and render ours. Outside it, vanilla is untouched.
- Held items rendered at `tool_l/r` with the vanilla item renderer.
- Armor: an armor mesh per slot (helmet, chestplate, leggings, boots) on the same skeleton, UVs on the vanilla
  armor texture layout, with dyed leather, trims and enchantment glint.
- Also handled: cape, elytra, arrows and bee stingers stuck in the body, name tag, shadow, invisibility,
  spectator, glowing outline, sneaking height, sleeping, riding.
- A debug skeleton view: joints drawn as small axes.

Done when:
- Side by side GIF: vanilla player and our player in the same rest pose, 3 different skins (wide, slim, skin
  with transparent outer parts): no visible difference.
- A test pose with elbows and knees at 150°: no hole, no corner poking out, texture not stretched, sleeve in one
  piece (GIF from front, side and close-up).
- Cost under 0.2 ms per player per frame (measured with 20 players in view).

## Step 4: Clips and the animation player

Goal: play animations made in Blender.

Build:
- Clips come from glTF animations (one file per clip set, e.g. `skeletal/animations/fists.gltf`), turned at
  load into `Clip`: per-joint tracks of translation, rotation and scale with `LINEAR`, `STEP` and
  `CUBICSPLINE` interpolation, sampled at any time (partial ticks included).
- A sidecar JSON per clip set (`skeletal/animations/fists.json`) gives each clip's id, loop flag, default
  transition time, joint mask, events (step 6) and root motion settings.
- `AnimationPlayer`: current clip, elapsed time, speed, loop, end callback.
- Dev command `/murim anim play <clip> [speed]` and `/murim anim speed <factor>` (slow motion for checks).
- Clips reload with F3+T without restarting.

Done when: unit tests sample known keyframes; a clip made in Blender plays in game and matches Blender's render
(GIF next to GIF).

## Step 5: Animator: layers, masks, transitions

Goal: several animations at once, and no pop ever.

Build:
- Base layer driven by living motions: idle, walk, run, sneak, jump, fall, swim, climb, ride, and combat
  variants (combat idle stance, combat walk with feet in stance).
- Composite layers by priority: `LOWEST`, `LOW`, `MIDDLE`, `HIGH`, `HIGHEST`. A layer only controls the joints
  its clip's mask leaves on, and the mask can depend on the living motion (for example a guard clip takes the
  arms only while walking, the whole body while standing).
- Transitions: when a clip starts, the current pose is captured and blended into the new clip's first frames
  over its transition time (default 0.08 s for strikes, 0.15 s for motions). A layer turned off fades out the
  same way.
- Additive layers (beyond Epic Fight): small motions added on top of any pose: breathing, a flinch while
  running, recoil on block.
- Procedural head look: head and neck follow the camera pitch and yaw within limits, on top of the clips.
- Mirroring: a clip can play mirrored (left and right swapped), so one animation serves both sides.

Done when: automated test switches between every pair of motions and strikes and measures the largest joint
rotation change between two frames: never above a threshold (no pop); GIF of walking while guarding.

## Step 6: Events and root motion

Goal: animations that move the body in the world and trigger effects at exact times.

Build:
- Timed events in the sidecar JSON: sound, particle, Photon effect, camera shake, footstep, and server
  callbacks (for example "open the hit window"). Each event says its side: client, server or both.
- Root motion: the `root` joint's translation track is removed from the drawn pose and applied to the entity
  each tick as `Entity.move(MoverType.SELF, delta)`, rotated by the facing at the start of the clip, so walls and
  edges stop it. Options per clip: vertical movement, gravity off windows, move only during given windows,
  stop the entity's velocity at start, and an optional pull toward the locked target capped in distance.
- Sync: the server is the authority; the local player predicts and is corrected smoothly when the server
  differs by more than a small distance.

Done when: a step-forward clip moves the player exactly the distance animated (test measures it), stops against
a wall, and looks the same on another client (duel test).

## Step 7: First person

Goal: what the player sees matches what others see.

Build:
- Arms (and the body and legs when looking down: beyond Epic Fight's arms-only fallback) drawn from the same
  pose, with the camera placed at the head joint and smoothed so strikes do not shake the view too much.
- Per clip optional first-person adjustments (arm offset or a camera track) only when a strike reads badly.
- Held items in hand, correct with any FOV setting.

Done when: GIFs in first person of idle, walk, each strike; no arm clipping into the camera at FOV 30 to 110.

## Step 8: Network sync

Goal: everyone sees the same animations at the same moment.

Build:
- Clip ids mapped to small integers, synced on join and after reload.
- Payloads: play (entity, clip, layer, start time, speed, mirrored), stop layer, living motion changes.
  Sent to players tracking the entity; a player arriving late gets the current state.
- The local player plays its own strikes at once; the server confirms or refuses (refused: blend back to the
  stance).
- A dev option that delays packets to simulate lag.

Done when: duel test with 0 and 150 ms of simulated lag: both screens show the same clip within one tick
of offset, no snapping.

## Step 9: Combat core

Goal: strikes as timed phases, hit detection on the real animated body, data-driven weapons.

Build:
- Phases per clip, possibly several per clip (a double strike): `windup` start, `active` start, `active` end,
  `recovery` start, `end`. Each phase gives the hand, the collider and its joint, and properties.
- State timeline driven by phases: movement locked, turning locked, chain allowed (from recovery), skill
  allowed, item use allowed, super armor (blows taken do not interrupt), invulnerable. One `FighterState`
  class read by input, movement and damage code.
- Input: left click in combat mode; an 8-tick buffer retried each tick; chain counter reset after 16 ticks
  idle or when another action plays; variants chosen by context: dash strike (sprinting at least 8 ticks), air
  strike, charged strike (hold, costs Qi).
- The server evaluates poses too (no rendering), from the same clip files, so hit detection follows the true
  animation.
- Colliders: oriented boxes attached to a joint with an offset and size, several boxes per weapon, a line
  collider for spears. Each tick the collider is swept between the previous and the current pose with N
  interpolated samples (N from data) so fast strikes never skip a target.
- Hit rules: one hit per target per phase, a maximum of targets per phase, nearest first, line of sight,
  team and PvP rules, the target's hurt delay skipped.
- Damage: our damage source carries damage, armor negation, impact, stun type, knockback and Qi cost; vanilla
  armor applies after our damage.
- Hit-stop (beyond Epic Fight): attacker and target freeze 2 to 5 ticks scaled by impact, synced on all
  clients.
- Data: `data/murimblock/combat/moves/*.json` (clip, phases, per-phase properties) and
  `data/murimblock/combat/weapons/*.json` (items or tags matched, category, style one hand, two hands or dual,
  chain list, dash, air and charged moves, guard clip, colliders, impact, armor negation, max targets),
  synced to clients and reloaded with `/reload`.
- Hitbox debug view: colliders and their sweep drawn in game, targets touched highlighted.

Done when: unit tests for phases, timeline, colliders and sweeps; automated tests: each strike hits a target in
front, misses one out of reach, behind or behind a wall; chain order and reset; dash and air variants; a fast
strike at a moving target is not skipped; duel test with lag.

## Step 10: Hit reactions

Goal: every blow shows on the target.

Build:
- Stun types: `NONE` (additive flinch only), `SHORT`, `LONG`, `KNOCKDOWN` (falls, gets up; can get up with a
  dodge), `LAUNCH` (airborne, for uppercuts; beyond Epic Fight), `NEUTRALIZE` (guard broken), `FALL`.
- Stun time from the attacker's impact against the target's stun resistance (armor weight and cultivation);
  the formula lives in one class with unit tests.
- Reaction clips chosen by stun type and by the direction of the blow (front, back, left, right: beyond Epic
  Fight, which ignores the direction).
- Knockback along the blow's direction, strength from data.
- A stunned fighter's move and chain stop, except during super armor.

Done when: automated test hits a target from four sides with each stun type and checks the clip played and the
stun time; GIFs of each reaction.

## Step 11: Defense and Qi

Goal: choices with a cost.

Build:
- Qi as the resource: costs per action in data; spending pauses Qi regeneration for a short delay (hooked to
  the cultivation Qi, not a new bar).
- Guard (held key): frontal 180°; does not block projectiles, explosions, fire or magic unless a technique says
  so; cost = penalty × impact, the penalty grows with each block and resets after 40 ticks without blocking or
  when the guard holder lands a blow; out of Qi → guard break (`NEUTRALIZE`); guard hit clips per weapon; slower
  walk and no sprint while guarding.
- Parry (beyond Epic Fight's base guard): the first 4 ticks of a guard deflect at no cost and stun the attacker.
- Dodge: step in 4 directions with their own clips, and a roll; invulnerability window from data (melee and
  projectiles, not explosions, per D5); can cancel a strike's recovery; ground only.

Done when: automated tests for front and back blocks, penalty growth, guard break, parry window timing, dodge
invulnerability against a melee blow, an arrow and an explosion; GIFs.

## Step 12: Camera and targeting

Goal: Epic Fight's camera control.

Build:
- Over-the-shoulder third-person camera with its own rotation, pulled in by walls (rays from the eyes).
- Movement relative to the camera; the body turns toward the movement or the target with a speed limit.
- Lock-on key: the target nearest the screen center within range and sight; switch target with a mouse flick;
  released on death, out of sight or range; strikes face the target; the target outlined.
- All of it in the client config, off by default for players who prefer vanilla.

Done when: GIFs of lock-on, switching, release; strikes land on a target circling the player.

## Step 13: Feel

Goal: blows that feel heavy.

Build:
- Trails along fists and weapons from clip events (Photon), hit sparks by impact, sounds per weapon and impact,
  camera shake scaled by impact, a short screen flash on heavy blows.
- Tuning pass on all values with Enzo, in game.

Done when: Enzo approves the fist slice in game.

## Step 14: Humanoid mobs

Goal: mobs fight with the same engine (per D3).

Build:
- Zombies, husks, skeletons, bandits and masters use the skeleton, the moves and the reactions.
- AI goals: approach to range, strike with chains, guard, dodge, keep distance with ranged weapons; difficulty
  values in data.
- The mortal trial's undead and the Grave Warden move to the engine.

Done when: a duel against a bandit mob plays out with chains, guards and dodges; GIFs.

## Step 15: Weapons and content

Goal: the fist slice at Epic Fight's level, then other weapons.

Build:
- Fists: boxing stance, jab, cross, hook, uppercut chain, dash strike, air strike, charged strike, guard,
  parry, dodges, reactions. Each clip passes the checklist below.
- Then sword and spear movesets with their colliders.
- Clip checklist: stance with one foot forward and knees bent; weight shift before the strike; hips turn
  before the shoulders; anticipation visible; contact frame clear; follow-through; return to stance.

Done when: every clip passes the checklist on GIFs and Enzo approves in game.

## Step 16: Performance and compatibility

Build and check:
- 50 players or mobs on the engine in view: frame cost measured, skinning cached when nothing moves.
- Shader packs (Iris) and mods that add player render layers: fall back to vanilla rendering when a conflict is
  detected, with a config switch.
- The lotus pose and the trial's getting up (now drawn by `BentLimbsLayer`) move to the engine.

Done when: numbers written in this doc, no crash with the tested mods.

## Beyond Epic Fight (summary)

Directional hit reactions, launch stun, hit-stop, additive layers, a parry window in the base guard, four-way
dodge clips, body visible in first person, Qi tied to cultivation.
