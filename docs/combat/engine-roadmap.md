# Combat engine roadmap

Goal: a combat engine of Epic Fight's level or better, written by us from scratch. Epic Fight is studied for
ideas only: its code is GPL and its assets are All Rights Reserved, so nothing is copied.

The animation engine is named **Shenfa** (身法, "the art of the body", the wuxia word for a fighter's mastery of
movement and stance), package `com.murimblock.shenfa`: skeleton, skinned mesh, clips, animator, foot planting and
the player renderer. Combat, weapons and moves build on it.

Status: steps 0 to 8 done with combat and feel in a first version (27 Oct 2026). On 28 Oct the techniques, the
blade moveset and every mob on Shenfa were taken out, to be redone with Enzo (they are in git history before that
day). The to-do list is `shenfa-todo.md`; this file stays as the record of each step. The first combat system was
removed on 26 Oct (commit `b27eaf3d`).

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
| D3 | Mobs | Every mob on Shenfa: humanoids (zombies, bandits, masters) on the biped skeleton with the same moves and reactions, the others (spiders, golems, bosses...) on skeletons of their own. GeckoLib stays only for non-combat models. | accepted 26 Oct, widened 27 Oct to every mob |
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
- Packages: `com.murimblock.shenfa` (skeleton, mesh, clips, animator, pure math), `com.murimblock.shenfa.client`
  (rendering), `com.murimblock.combat` (fight logic), `com.murimblock.combat.client`.
- Blender pipeline in `tools/blender/` (outside the jar): a script that builds the reference rig
  `murim_biped.blend`, an export script to glTF (`.gltf` + `.bin`) with fixed settings, and a render script that
  films a clip from front, side and three quarters into PNG frames. Check Blender is installed on both PCs;
  document the setup in `docs/two-pcs.md`.
- Reuse `tools/frames-to-gif.java` for GIFs.
- An in-game test harness (`client/dev/EngineTest`) driven by a system property and a Gradle run config, as
  before: scripted steps, captures, a pass/fail report in the log.

Done when: the Blender scripts run headless on this PC, produce a rig file and a test GIF; the harness runs
an empty test and exits.

Done (26 Oct 2026): Blender 5.2.3 headless on this PC; `tools/blender/` (`murim_biped.py`, `build.py`,
`export.sh`, `render.py`, README); GIFs with `tools/frames-to-gif.java`; `client/dev/EngineTest` with the
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

Done (26 Oct 2026): `com.murimblock.shenfa`: `JointTransform`, `Joint`, `Armature`, `JointMask`, `Pose`, `Biped`
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
vanilla boxes, weights, UVs, hinges, refusals), `shenfa/client/SkeletalModels` (reload listener for
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

Step 3a done (26 Oct 2026): `shenfa/client/SkeletalPlayerRenderer` cancels `RenderPlayerEvent.Pre` and draws the
body and outer layer (CPU skinning in `shenfa/Skinning`, triangles sent as quads, `entityCutoutNoCull`, ghost and
glowing cases), held items at `Tool_*` (offset from vanilla's hand placement), the vanilla name tag (through
reflection: the methods are protected and overridden as protected, so an access transformer breaks vanilla),
head look, death fall, entity scale. `shenfa/client/SkeletalPlayers` decides who is drawn (`/skeletal on|off`,
`/skeletal pose <name>`, dev only) and the pose: `BipedPoses.stand` copies vanilla's standing pose (arm sway,
an arm raised 18° when it holds an item) until the idle clip of step 5. `Arm_*` moved to vanilla's pivot (x ±5).
`runEngineTest` (`SkeletalRenderChecks`): six default skins (wide, slim, outer layers, sword, shield, bow,
torch) shot by vanilla then by us from six sides: at most 0.3 % of pixels differ (only the slim arms, which
vanilla pivots half a pixel lower), the bottom faces' UVs match vanilla; test poses shot; about 30 µs per
player.

Step 3b done (26 Oct 2026): armor meshes in the biped files (vanilla's armor boxes, 64x32 UVs, left limbs
mirrored, inflated 1 / 0.5, rigid with hinges) drawn by `shenfa/client/SkeletalArmor` like vanilla's armor layer
(material layers, dye, trims, glint); `shenfa/client/VanillaLayers` runs the renderer's other layers (cape, elytra,
arrows, stingers, head items, parrots, other mods' layers) on the vanilla model with its parts moved to follow
our joints, the cape and elytra hanging from `Chest`; `BipedPoses.stand` approximates vanilla's crouch;
`/skeletal joints on` shows the bones alone. Checks: armored lineup within 0.4 % of vanilla from four sides;
twenty players at about 55 µs each. Left for later: armor with its own model (other mods) is not drawn; the crouch
is close but not exact (5 to 9 % of pixels differ, the crouch clip of step 5 replaces it); vanilla keeps drawing
players who sleep, glide, swim, ride or spectate.

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

Step 4 done (27 Oct 2026): `shenfa/Clip` (channels per joint and path, `LINEAR`, `STEP`, `CUBICSPLINE`, values are
whole local transforms as in glTF), `shenfa/ClipLoader` (glTF animations matched to joints by node name, the
armature object folded into the root as for the model; sidecar `loop`, `transition`, `mask` = joints with their
children; unknown clips, settings or joints refused), `shenfa/Animation` (clip and settings), `shenfa/AnimationPlayer`
(ticks of 1/20 s times speed and slow motion, partial ticks, loop without a jump, end callback, seek).
`shenfa/client/SkeletalClips` loads `skeletal/animations/<set>.gltf` + `<set>.json` with the resource packs; ids are
`murimblock:<set>/<clip>`. The commands live under `/skeletal anim play|stop|speed` (client side) instead of
`/murim anim`, since `/murim` is a server command and the two would clash. Blender: `tools/blender/murim_clips.py`
keys clip sets (20 fps), `export_clips.py` exports them (every frame sampled, the helper constraint baked);
the `test` set has `bounce` (boxing bounce in guard, legs carrying it) and `wave`. Checks: unit tests sample
hand-made and Blender-exported keyframes; `runEngineTest` plays, loops, slows down and reloads the clips and films
them frame by frame from the three views of `render.py`. Until step 5, one player drives every skeletal player.

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

Step 5 done (27 Oct 2026): `shenfa/Animator` (base layer, composite layers `LOWEST` to `HIGHEST`, additive layers;
a layer that changes clip blends from the pose it showed, eased over the new clip's transition; a layer fades in
and out by weight; each joint's weight follows the mask of the current motion, so a guard can take the whole body
standing and only the arms walking, without a pop; mirroring; `SteadyBlend` keeps each joint turning the same way
when two rotations are near half a turn apart, which made a raised arm flip in one frame), `shenfa/LivingMotion`
(with fallbacks), `shenfa/MotionSet` (`skeletal/motions/biped.json`: clips per motion, combat clips first in combat
mode, additive clips), sidecar settings `masks`, `additive`, `move_speed`. `shenfa/client/PlayerAnimators`: one
animator per player, reading the body every tick (ride, swim, climb, jump, fall, sneak, walk, run, idle) and
playing walk, run, sneak walk and climb at the body's real speed. The renderer lays a swimmer down like vanilla and
draws riders and swimmers. Clips (`tools/blender/murim_clips.py`): `biped` (idle, walk, run, sneak, sneak_walk,
jump, fall, climb, ride, swim, additive breathe) and `fists` (stance, stance_walk, guard, jab, legs first). Dev
commands: `/skeletal motion <motion|auto>`, `/skeletal combat on|off`, `/skeletal anim play <clip> [speed]
[priority] [mirrored]`, `/skeletal pose animated|stand`. Checks: `AnimatorTest` switches every pair of the 12 base
clips at three points of their cycle, and the guard and jab over each, at 60 frames per second: no frame moves
more than the clips themselves plus an eased blend of the gap (a cut is caught); `runEngineTest` reads a test
body standing, walking, running, sneaking and falling, plays every motion, and films the guard over walking and a
jab from the stance. The head look is applied after the animator (no limits yet). The clips are first versions for
Enzo to fix in Blender.

Step 5 redone for vanilla parity (27 Oct 2026), so nothing vanilla does is lost before combat:
`shenfa/client/VanillaPoses` runs vanilla's own player model every frame and lays its arm work on our joints: the
item poses (bow, crossbow charging and charged, shield, trident, spyglass, goat horn, brush, and poses other mods
add through NeoForge's `IArmPoseTransformer`) take the arm, both for two-handed ones, forearm straight, faded in
over 0.08 s; the attack swing is added as the change it makes to each arm and to the chest's turn, so it plays over
any clip; a held item raises the arm 18° as in vanilla. The renderer copies every body turn of vanilla's
`setupRotations`: asleep in a bed, elytra flight (laid along it, turned toward the motion, head back 45°), riptide
spin, upside down (Dinnerbone and Grumm), shaking when frozen, a seated rider's body following its mount with the
head within 85°; spectators show their head only. Armor with a model of its own (other mods, e.g. GeckoLib armor)
is drawn by vanilla's armor layer on the vanilla model following our joints; a spyglass in use is held at the eye
as vanilla does; slim arms hold items half a pixel in as vanilla does. New motions: `sleep`, `glide`, `spin`.
Checks: `VanillaChecks` compares with vanilla pixel by pixel (in vanilla's `stand` pose) eleven arm poses (0.13 to
0.15 % of pixels differ), the bodies (asleep, gliding, upside down, frozen, in a boat, custom helmet: under
0.03 %) and a riptide spin alone (0.65 %, the spin turns 75° a tick so two shots differ), then shoots the same with
the animator. Test players now get distinct UUIDs (two with the same UUID were silently dropped by the level).
Still vanilla's choice, not ours: babies are not scaled down (players are never babies).

Real-player tour (27 Oct 2026, `PlayerChecks`, `gradlew runEngineTest -PengineGroups=player`): the local player is
played through the real keys (walk, run, stop, sneak, jumps, a 40-block fall, a ladder, swimming, a boat, a bed at
night, an elytra flight, a riptide, bow, crossbow, shield, sword swings while walking, trident, spyglass, eating, the
stance, the guard while walking, jabs), filmed by the third-person camera held at a fixed angle, with a probe giving
each part's largest joint turn per frame at 60 fps (frames stalled by saving a shot are left out). Every motion is
read right. Found and fixed the same day:
- An item pose whose arm goes half a turn (trident raised: 76° in one frame) took the other way round mid-fade:
  `VanillaPoses` now uses the animator's `SteadyBlend` and fades over 0.15 s (trident 27°, spyglass 16°).
- Getting on a ladder swung both arms up in 0.15 s (33° a frame): the climb transition is 0.25 s (21°).
- Feet slid: Blender's eased keys stopped the planted foot at each key; step cycles are now keyed linearly
  (`murim_clips.LINEAR`), the walk lands on both feet and has a longer stride. The tour measures the planted foot's
  slide: run 0.5 blocks/s (at 5.6), sneak walk 0.7 (at 1.3), walk 1.5 to 1.8 (at 4.3). Walking at Minecraft's
  4.3 blocks a second (a human jog) with legs on the ground cannot cover the distance; planting the feet needs
  foot IK (later). The boxing shuffle slides almost fully (4.2 at 4.3): combat mode will walk slower (step 9).
- The wing that seemed to cut through the chest in elytra flight is vanilla's too (the far wing seen edge on):
  the tour now shoots every other elytra frame with vanilla to compare.
- The test world was shared between check groups and runs: the tour now builds 300 blocks away, and the first
  step brings the player over the test area (on a fresh world the test players were dropped in unloaded chunks).

Walking (27 Oct 2026). Minecraft walks a body at 4.3 blocks a second with 12 px legs: a foot on the ground covers
at most about 0.85 block a step, so either the steps are quick or the feet slide (vanilla slides). Now:
- `shenfa/FootPlanter` (with `shenfa/LegIk`, two-bone IK): a sole the clip puts on the ground while it goes back under
  the body keeps its place in the world, the thigh and knee reaching for it as the body moves on; it lets go when
  the clip lifts it (off the ground and coming forward) or the leg is straight, easing back into the clip over
  0.12 s; it plants again only after a lift (no jump), stands exactly on the ground, and a foot in the air never goes
  through it. `shenfa/client/FootPlanting` runs it for each player walking, running or sneak-walking on the ground.
- `move_speed: "auto"` (`shenfa/Strides`): a step cycle's ground speed is measured from its own feet at load, so the
  cadence always matches the body (walk 2.25, run 4.62, sneak walk 0.66 blocks a second at speed 1).
- The walk steps 0.9 block (about 4.6 steps a second at 4.3 blocks a second), lands on both feet and keeps the
  swinging foot 1.2 px up until it lands (a reaching key); step cycles are keyed linearly.
- Going sideways or back, the hips and legs turn toward the motion (at most 80°, smoothed) while the chest faces
  ahead; going back the cycle plays backward (`AnimationPlayer` takes a negative speed).
Measured on the real player (planted foot's slide): walk 0.2 blocks a second (was 1.6 to 2.6), run 0.2 to 0.3
(was 0.6), sneak walk 0.4 (was 0.7), walking back 0.9 (was 8.5), sideways 0.7 (was 2.9); `WalkSlideTest` checks
walk, run and sneak walk under 0.3 offline. Left: the boxing shuffle still slides (combat is being reworked); a knee
turns up to 30° a frame, the quick cadence's own speed plus the let-go.

Feet on the ground (27 Oct 2026): `FootPlanter` now has three modes. `STEP` (walking, running, sneak-walking) as
above; `STAND` (still, sneaking still): both feet stay put while the body turns, and a foot that drifted 2.5 px or
whose body turned 35° takes a 0.2 s step lifted on an arc, one foot at a time; `OFF` (in the air, swimming,
riding...): the clip alone. Each foot stands on the block surface under it (`FootPlanting.ground`: the collision
shape at the foot, a surface covered by a block above is buried, not ground): a swinging foot looks 16 px ahead and
rises onto a step before it reaches it, a foot's height changes at most 90 px a second, the hips sink up to 9 px
walking (4 standing: on a ledge the lower foot hangs a little) and always stay 6 px above the highest foot, and the
drawn body catches up over a moment when Minecraft lifts it up a stair at once or drops it down one (`lag`). A
planted foot out of reach is pulled in toward the hip, and eases there; a foot letting go keeps its height in the
world. `LegIk` turns the thigh about the knee's hinge first (a signed angle, no flip), and folds the knee at most
160°. The tour adds stairs up and down, slabs, a slab edge and turning in place, and measures how far the foot on the
ground stands off its surface. Measured: walking 0.01 blocks a second of slide (was 0.2), sideways 0.08, back 0.4,
stairs down 1.0, slabs 0.5, turning in place 0, a slab edge 0; feet within 0.05 px of their surface on average.
Rechecked the same day (the tour logs every frame where a joint turns faster than 40° at 60 fps, with the feet's
state), causes found and fixed: a leg near full stretch or full fold made the knee snap (`LegIk` eases the distance
there, soft IK); a foot let go held back then snapped (it is now handed back to the clip as the clip lifts it); the
hips squatted for a rear foot left on the lower step (only a foot under or ahead of the body makes them sink, a
step's catch-up is capped at 3 px up and 6 down); a step up or a slab was read as a jump, and a short drop off a
step too (`PlayerAnimators`); a jump dragged the legs to the ground (a foot let go in the air rises with the body);
landing folded the legs (in the air feet just follow the clip); the ground lookup returned a surface buried under a
stair; an arm flipped on the ladder (the animator's steady blend forgets the last blend's ways round); the elytra
head snapped back 45° (it tilts over four ticks). Now on the real player: on the flat a knee peaks at 35 to 55° a
frame (the clip alone 22), stairs and slabs at 50 to 78° for a frame or two per climb, everything else under 47°.
`StairsTest`, `FootPlanterTest` and `WalkSlideTest` check this offline.
Matching vanilla or right as is: idle, sneak, jump, fall and landing, boat, bed, bow, crossbow, shield, eating, the
stance, the guard while walking, jabs; the sword swing's 48° a frame is vanilla's own speed (its formula gives 46°
in the first frame).

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

Step 6 done (27 Oct 2026), but for the server share: `ClipEvent`, `RootMotion`, `shenfa/client/ClipEvents`,
`Footsteps`, `CameraShake`. Sidecar `events` (sound, particle at a joint, footstep, shake, callback; client, server or
both) fire once each as the player passes them, forward, backward, across a loop, at any speed; a layer fading out
fires no more, a mirrored layer swaps their side. Footsteps: each foot planted by `FootPlanter` (a stride, or a step
while standing, softer) plays the step sound of the block under it, and vanilla's distance-counted step sounds are
muted for a body drawn with its feet planting (sneaking makes none, as in vanilla); a body out of view keeps
vanilla's. Root motion (`"root_motion": true` or `{vertical, no_gravity}`): the root's travel is taken out of the drawn
pose and moves the local player through `Entity.move` along its body's facing, so walls stop it; while it plays the
feet follow the clip, not the ground. First clip: `fists/step_in`, a jab stepping in 0.7 block. Checks (`events`
group, real player): 9 steps in 1.75 s of walking, every vanilla step muted; step_in travels 0.70 blocks (clip 0.70)
facing south, east and mirrored, its sound and two footsteps in order; a wall stops it at 7.70; a looping clip's
particle and shake fire once and shake the camera. Left: the server side (events, prediction, other clients) with
step 8; Photon effects; root motion toward a target and camera sync with combat; the boxing shuffle plants only 4
steps in 1.75 s (it slides).


## Step 7: First person

Goal: what the player sees matches what others see.

Build:
- Arms (and the body and legs when looking down: beyond Epic Fight's arms-only fallback) drawn from the same
  pose, with the camera placed at the head joint and smoothed so strikes do not shake the view too much.
- Per clip optional first-person adjustments (arm offset or a camera track) only when a strike reads badly.
- Held items in hand, correct with any FOV setting.

Done when: GIFs in first person of idle, walk, each strike; no arm clipping into the camera at FOV 30 to 110.

Step 7 done (27 Oct 2026): `shenfa/client/FirstPerson`. In first person the local player's body is drawn in the world
(after the entities, through the same renderer) from the pose others see, the head, hat, helmet, head items, vanilla
layers and name tag left out, so looking down shows the body and the steps (planted feet included) at any field of
view; looking down the body steps back under the eye (up to 0.2 block) so the chest's top does not fill the view.
Vanilla's first-person hands stay for ordinary play (mining, placing, eating, every item pose); in combat mode, or
while a Shenfa layer plays, our arms and held items replace them (`RenderHandEvent` cancelled), so the stance, the
guard and strikes are seen as others see them, without the arms swapping between strikes. The camera follows the
head's eye (above the neck, the neck's own turn left out), smoothed and at most 0.35 block from vanilla's, moved in
the field-of-view event (after the camera is placed, before the world). `/skeletal firstperson on|off`. Checks (tour,
real player): looking down, walking looking down, walking with a sword (vanilla hands), the guard walking, jabs at FOV
70, 30 and 110; the hands never come nearer than 0.33 block to the eye (the near plane is 0.05). Not done: per-clip
first-person adjustments (to add when a strike reads badly); in combat the fists sit low in the view at rest.

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

Steps 6b to 13 done in a first version (27 Oct 2026, evening), details in `combat-system.md`:
- Mobs on Shenfa (`shenfa/client/mob`): a mixin right after vanilla's `setupAnim` lays Shenfa's layers on the
  mob's own pose and writes them into its model parts, then puts the parts back after the draw (vanilla sets only
  some values each frame; without that a spider drifted away). Humanoids and illagers ride the biped (`HumanoidRig`,
  the player's clips, a rigid limb pointed from its pivot to the hand or foot); any other model gets a skeleton of
  its parts (`PartRig`, vanilla's own space) and clips keyed on them in JSON (`ClipLoader.loadKeyframes`). The
  model is still vanilla's, so armor, held items, glowing eyes and resource packs keep working.
- Network: clip ids go as text (clients may have more clips than the server); `ShenfaLibrary` reads the clips from
  the mod's files on the server; play and stop any layer of any body; combat mode; the local player predicts its
  moves with the server's own rules and the server corrects it. Not done: integer ids, the lag simulator, the duel
  test (one client only here).
- Combat, hits, reactions, defense, camera and feel: `combat-system.md`. Mixins: `LivingEntityRendererMixin`
  (client), `MobStrikeMixin` (common, `doHurtTarget` of `Mob`, iron golem, hoglin, zoglin, ravager, warden).
- Checks: groups `mobs` (37 vanilla mobs rigged, a contact sheet of 18 attacks), `combat` (rules, combo, damage,
  stagger, hit stop, dodge, guard, parry, 18 mobs landing their attacks), `film` (contact sheets of every fighting
  clip), `perf` (50 players: 35 µs each, 113 fps against 108 with vanilla's), `cloth`. A dedicated server starts with
  the mod (no client class on the server).

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
- The trial's getting up (now drawn by `BentLimbsLayer`) moves to the engine (the lotus did on 29 Oct 2026).

Done when: numbers written in this doc, no crash with the tested mods.

## Beyond Epic Fight (summary)

Directional hit reactions, launch stun, hit-stop, additive layers, a parry window in the base guard, four-way
dodge clips, body visible in first person, Qi tied to cultivation.
