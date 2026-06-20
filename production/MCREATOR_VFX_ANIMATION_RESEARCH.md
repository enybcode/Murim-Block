# MCreator VFX And Animation Research

This file is research only. It does not add particles, items, entities, Java code, or resources to the playable mod.

## Local Project Reality

- MCreator version: 2026.1
- Generator: `neoforge-1.21.8`
- NeoForge version in `build.gradle`: `21.8.31`
- Current visual effects already used:
  - `AuraMeditation` spawns vanilla `SOUL` and `SOUL_FIRE_FLAME` particles.
  - `KillAddQi` spawns vanilla `LAVA` particles.
  - `Crab` currently has no custom animations in its MCreator element.
- Current safe rule: keep concrete mod changes out of `src/main` and `elements` until explicitly approved.

## What MCreator Can Do Safely

MCreator has a Particle mod element. Good use cases:

- qi aura sparks;
- cultivation mist;
- sword slash traces;
- impact bursts;
- demonic smoke;
- talisman glow dust;
- breakthrough shockwave fragments.

Useful particle settings:

- animated tiled texture;
- render type `Lit` for glowing particles;
- scale;
- angular velocity and angular acceleration;
- gravity, including negative gravity for rising aura;
- max age and age variation;
- collision;
- always-show flag, but use carefully for performance.

MCreator procedures can spawn particles from triggers such as player tick, right click, entity hurt, entity death, block right click, and similar event hooks. This is enough for many first-pass "wow" effects if we generate clean particle presets instead of random clouds.

## When MCreator Is Not Enough

Use Java / NeoForge helper code when we need:

- exact rings, cones, spirals, arcs, beams, and trails;
- client-only camera shake;
- custom particle movement beyond simple speed/gravity;
- synchronized ability phases;
- reusable VFX presets shared by many techniques;
- dynamic color, size, lifetime, or sprite selection;
- higher-quality performance throttling.

NeoForge supports registering real particle types, particle descriptions under `assets/<namespace>/particles`, textures under `assets/<namespace>/textures/particle`, and client providers for custom particle behavior.

## Light Strategy

There are two different meanings of "light":

- glowing particle: visual brightness only, safe and easy;
- real world light: actual block/entity light affecting nearby blocks, harder and risky.

Recommended order:

1. Use MCreator `Lit` particles and emissive-looking textures first.
2. For temporary illumination, test vanilla light blocks only in isolated prototypes.
3. Use dynamic-light APIs only if we deliberately accept an external dependency.

For Murim Block, most techniques should fake light with lit particles, colored sprites, overlays, and sound. True dynamic lights should be reserved for rare ultimate abilities.

## Animation Strategy

### Built-In / Safer Path

- Use MCreator's Java entity animation system where possible.
- Keep Blockbench model files in `production/blockbench`.
- Export only after review.
- Prefer readable animation names:
  - `idle`
  - `walk`
  - `attack_bite`
  - `cast_start`
  - `cast_loop`
  - `cast_release`
  - `hit_react`
  - `death`

### Plugin Path

Nerdy's Better Animations supports MCreator 2025.3 and 2026.1. It adds procedure blocks to trigger entity and item animations directly, plus texture swapping. This looks more aligned with the current workspace than old GeckoLib-only workflows.

GeckoLib can still be valuable for advanced animated entities/items/armor, but it adds an exported mod dependency and requires correct Blockbench GeckoLib format exports. Also, the current local Blockbench setup showed a GeckoLib Animation Utils compatibility warning, so this path needs a compatibility test before committing to it.

## Recommended Murim VFX Preset Library

Do not add these to the mod yet. These are named presets I can generate later.

### Qi Aura: `qi_aura_blue`

- Trigger: player tick while aura active.
- Shape: low-density rising spiral around torso.
- Particles: lit blue/white custom particles or vanilla soul variants.
- Performance rule: spawn every 2-4 ticks, not every tick at high count.

### Cultivation Breakthrough: `breakthrough_ring`

- Trigger: breakthrough event.
- Shape: expanding horizontal ring + vertical column.
- Needs: custom Java helper or carefully generated MCreator procedure.
- Add screen shake only after base effect is stable.

### Sword Technique Slash: `sword_arc`

- Trigger: technique key/release or hit frame.
- Shape: forward crescent, short lifetime, high readability.
- Needs: custom particle coordinates, probably Java helper.

### Impact Burst: `qi_impact`

- Trigger: entity hurt by technique.
- Shape: small flash, radial dust, knockback direction line.
- Can start in MCreator procedures.

### Demonic Shield Pulse: `demonic_guard_pulse`

- Trigger: block/parry/use.
- Shape: red-black ring close to player, short smoke aftertrail.
- Combine with shield texture/model state later.

## Safe Implementation Plan

1. Research-only docs and generators.
2. Create `production/vfx/*.json` preset specs, not imported into MCreator.
3. Add toolkit commands that generate preview-only particle coordinate plans.
4. When approved, import one tiny test particle/procedure into a branch.
5. Compile with `gradlew compileJava`.
6. Test in MCreator run client.
7. Only then repeat for real techniques.

## Candidate APIs / Plugins

- MCreator native Particle element: safest.
- MCreator procedures: safest for triggering existing particles.
- Nerdy's Better Animations: promising for triggerable entity/item animations on MCreator 2026.1.
- GeckoLib: powerful but heavier; test compatibility first.
- NeoForge custom particles: best for high-quality reusable VFX helpers.
- Dynamic-light APIs: optional later; avoid until the mod foundation is stable.

## Sources

- MCreator particle element: https://mcreator.net/wiki/how-make-particle
- Nerdy's Better Animations: https://mcreator.net/plugin/119117/entity-texture-swapper
- GeckoLib MCreator workflow: https://mcreator.net/forum/93274/tutorial-how-use-nerdys-geckolib-plugin-40-20224
- MCreator OBJ note: https://mcreator.net/wiki/custom-obj-models-blocks-and-items
- NeoForge particles: https://docs.neoforged.net/docs/resources/client/particles/
- NeoForge client particle rendering: https://docs.neoforged.net/docs/rendering/particles/
- NeoForge models: https://docs.neoforged.net/docs/resources/client/models/
- NeoForge mob effects: https://docs.neoforged.net/docs/items/mobeffects/
