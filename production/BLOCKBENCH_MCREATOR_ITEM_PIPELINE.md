# Blockbench -> MCreator Item Pipeline

## Best Workflow For Swords And Objects

For normal weapons, tools, pills, manuals and artifacts, use **Java Block/Item** in Blockbench. MCreator's own guide maps item models to the Java Block/Item workspace for Forge/Fabric-style mods.

Use this by default for:

- swords;
- sabers;
- staffs;
- manuals/books;
- pills;
- talismans;
- small artifacts.

Use OBJ only for more complex static shapes that JSON cannot express cleanly. Use GeckoLib or animated-item plugins only when the item itself needs animation.

## Rules That Avoid Broken Purple/Black Models

- Keep asset ids lowercase with underscores.
- Use square textures: 16x16, 32x32, 64x64, etc.
- Avoid non-standard item rotations; Minecraft item JSON supports limited angles.
- Use valid texture paths and copy the texture into the mod assets.
- Preview all perspectives in Blockbench's Display tab before import.

## Current Murim Block Pattern

`MurimSword` is currently a simple 2D handheld item:

```txt
src/main/resources/assets/murim_block/models/item/murim_sword.json
parent: item/handheld
texture: murim_block:item/murimsword2
```

`EpeeEntrainement` already uses a custom 3D model:

```txt
elements/EpeeEntrainement.mod.json
renderType: 1
customModelName: wooden_sword_debutant:default
```

So the practical target for generated swords is:

```txt
src/main/resources/assets/murim_block/models/custom/<asset_id>.json
src/main/resources/assets/murim_block/textures/item/<asset_id>.png
customModelName: <asset_id>:default
```

## Commands

Create an item production sheet:

```powershell
tools\murim.ps1 new-item "Jade Qi Sword"
```

Generate a starter sword model:

```powershell
tools\murim.ps1 gen-sword-model "Jade Qi Sword" --kind jade
```

Generate variants:

```powershell
tools\murim.ps1 gen-sword-model "Training Sword" --kind training
tools\murim.ps1 gen-sword-model "Demonic Fang Sword" --kind demonic
```

The generated files stay under `production/items/generated/` until they are checked visually in Blockbench.

## Import Loop

1. Generate model and placeholder texture.
2. Open model JSON in Blockbench.
3. Tune silhouette, UVs, texture and Display tab.
4. Copy final model to `src/main/resources/assets/murim_block/models/custom/`.
5. Copy texture to `src/main/resources/assets/murim_block/textures/item/`.
6. In MCreator, set item render type/custom model.
7. Regenerate code if needed.
8. Run:

```powershell
.\gradlew.bat compileJava
```

9. Test in game.
