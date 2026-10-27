# What Epic Fight does (study, 27 Oct 2026)

Read from Epic Fight's 1.21.1 source (github.com/Antikythera-Studios/epicfight, branch `1.21.1`, versions 21.13
to 21.17), its CHANGELOG, README and wiki (epicfight-docs.readthedocs.io). For ideas only: its code is GPL and its
assets are All Rights Reserved; nothing is copied into Shenfa. Not verified: the 1.20.1 (20.x) changelog, the FOV
during attacks, third-party addons (WOM, Indestructible, EFM), PvP rules.

## First person

- The whole body is drawn in first person from the skeleton's pose (`FirstPersonRenderer`), head, cape, elytra and
  stuck arrows hidden; client option `first_person_model` (on by default), `enable_player_vanilla_model` to keep
  vanilla.
- Per-animation "pov" sub-files: a camera transform track, per-part visibility, root in `CAMERA` or `WORLD` space,
  view limits (min/max X and Y rotation), an optional separate first-person animation, sync frame. Only the bow and
  crossbow ship with one.
- The animation can move the camera (`enable_pov_action`), blended on transitions.
- It turns the "First-person Model" mod off when active. Fixes in 21.13: first person broken with shader packs,
  camera not changing while aiming.
- Players complain the first-person animations look bad (issue #2063); an addon forces vanilla first person outside
  combat items.

## Combat

- Attack phases `start, antic, preDelay, contact, recovery, end`, several per animation, each with its hand,
  joint/collider pairs and properties (damage, armor negation, impact, max strikes, stun type, swing and hit sounds,
  hit priority, particle). The phase windows set entity states: movement locked (start→recovery), turning locked
  (antic→end), attacking (preDelay→contact), combos allowed again from recovery, skills not before contact.
- Swept hit detection between the previous and current pose; colliders `OBB`, `MultiOBB`, `Line`, `MultiLine`,
  `Plane` with an interpolation count, center and size (datapack).
- 8-tick input buffer for attack, dodge, guard and the weapon skill.
- Combo list per weapon whose last two entries are the dash attack (after some sprint ticks) and the air attack.
- Stun types `NONE, SHORT, LONG, HOLD, KNOCKDOWN, NEUTRALIZE, FALL`; duration from the attacker's impact against the
  target's stun armor.
- Attributes: stamina (15, regen 1 after 30 ticks), armor negation, max strikes, weight, stun armor (armor gives
  weight and stun armor to players since 21.15.7), offhand variants.
- Guard, impact guard, parry (8-tick window, anti-spam), cost = penalty × impact, penalty growing per block.
- Dodge (roll, step), knockdown wake-up; movement skills (double jump, leap); no roll in the air since 21.17.1.
- Weapon innate skills charged by dealing damage; passive skills limited by a gamerule; skill books, or an official
  skill-tree addon.
- Weapon categories with one-hand, two-hand and dual styles chosen by the off-hand item; mirrored animations.
- Mob patches as datapacks: model, armature, faction, attributes, default motions, stun animations, and a
  `combat_behavior` list (weight, interruptible, looping, cooldown, conditions on distance, angle, health, chance).
  Bosses (the Ender Dragon) have phases.
- Mounted attacks, bow and crossbow animations, AI changes for ranged mobs; grappling animations exist.
- Two modes, vanilla and combat, switched by a key or automatically by held item (lists of combat and mining items),
  gamerules for the default mode and switching.

## Feel

- No hit stop, no camera shake, no slow motion in the core mod: a clear gap.
- Weapon trails from animation sub-files (resource packs can change them).
- Hit particles (cut, blunt), blood (option), ground slam particles, afterimages; per-weapon hit and swing sounds.
- Lock-on: range 20 (5 to 25), quick shift by a mouse flick, auto retarget after a kill; third-person camera modes
  with offsets; 8-direction movement (21.14.2); turns itself off with Shoulder Surfing or Better Third Person.
- Transitions, layers with priorities, joint masks, link animations; root motion properties (vertical movement,
  gravity off windows, fixed move distance, destination provider, camera sync).
- Cloth physics: a position-based cloth simulator (~1900 lines) for the player's cape and meshes declaring cloth.
- IK (FABRIK) only for the Ender Dragon; no foot IK for players; no footstep events; ragdoll only by an addon.

## Data and modding

- Weapons and armors as datapacks (attributes per style, colliders), overridable since 21.17.1, with tags; an
  in-game datapack editor.
- Animations as JSON exported by a Blender add-on (the tutorial targets Blender 2.79); custom animations off on
  servers by default.
- 3D armor by resource pack; emote wheel (21.15.1); event API (lock-on, camera, dodge, attack phase end), camera
  API, KubeJS skills.

## Known weaknesses

- First-person animations disliked; shader problems (Iris first-person rotation, armor glint lost in combat mode,
  translucent armor with Bliss); resource packs that change player or mob models break (Fresh Animations);
  FPS drops (model loading, Sodium with ImmediatelyFast), answered with persistent mapped buffers and compute
  shaders; incompatible with OptiFine and Controllable; many mixin conflicts.
