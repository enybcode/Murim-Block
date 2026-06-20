# Blockbench -> MCreator Animation Pipeline

## Viable Paths

### Path A: Built-in MCreator animations

Use this first. MCreator 2026.1 can work with Java entity/block animation workflows without needing GeckoLib for every case. This is the best default for Murim Block because it keeps the mod lighter and easier to maintain.

Use when:

- The mob needs idle/walk/attack/death style animations.
- We can accept MCreator's animation UI and generated Java model code.
- The animation does not need advanced stacking or complex procedure-triggered blending.

### Path B: Nerdy's Better Animations

Use this when procedures need to trigger exact animations at exact moments. The plugin page says it supports MCreator `2026.1` and adds procedure blocks for triggerable entity/item animations.

Use when:

- A technique or mob attack must play a specific animation on command.
- We need item animations or player-arm rendering behavior.
- We want texture swapping or controlled animation playback from procedures.

### Path C: GeckoLib

Use only if the built-in workflow is not enough. It is powerful, but it adds dependency and workflow complexity.

Use when:

- We need advanced keyframe animation, smooth stacking, controllers, or complex item/entity/block animation.
- We accept the library dependency and extra integration work.

## Current Local Reality

- Blockbench is installed at `C:\Users\enzob\AppData\Local\Programs\Blockbench\Blockbench.exe`.
- Blockbench plugins found include `animated_java`, `animation_utils`, `animation_sliders`, and `minecraft_entity_wizard`.
- `C:\Users\enzob\Documents\Steel-Fanged Raikans.bbmodel` is a complete `modded_entity` model with useful bones.
- `C:\Users\enzob\Documents\Pied Steal fang.bbmodel` is a partial foot/leg model, not a complete mob.
- Current `.bbmodel` files contain no animations yet.

## Codex-Friendly Workflow

1. Inspect model:

```powershell
tools\murim.ps1 bbmodel-info "C:\Users\enzob\Documents\Steel-Fanged Raikans.bbmodel"
```

2. Generate starter animations:

```powershell
tools\murim.ps1 gen-raikan-animations
```

3. Open the model in Blockbench and import/preview the generated JSON:

```txt
production/blockbench/steel_fanged_raikan.animations.json
```

4. Tune in Blockbench:

- fix bone rotation axes if needed;
- adjust timing;
- add personality;
- export using the format MCreator accepts for the chosen workflow.

5. Import/select animations in MCreator entity settings.

6. Compile:

```powershell
.\gradlew.bat compileJava
```

7. Test in game and commit once the mob/animation actually works.

## Quality Rules

- A generated animation is only a draft.
- Every mob needs at least: idle, walk, attack, hurt/knockdown/death.
- Every attack animation needs gameplay timing: wind-up, damage frame, recovery.
- Every animation must be checked in Blockbench visually before MCreator import.
- Every MCreator import must be followed by `compileJava`.
