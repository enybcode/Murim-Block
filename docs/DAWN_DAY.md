# Epic Fight: Dawn Day — animations only

Decision by Enzo on 15/10/2026: use Dawn Day for its animations, "as with Epic Fight". We keep only the animations and remove everything else: its HUD, its keys, and its place in the controls menu.

## What it is

- [Epic Fight: Dawn Day](https://modrinth.com/mod/epic-fight-dawn-day) (`epicfight_dd`, by Nexo) is an Epic Fight add-on. Its animation engine is Epic Fight itself, which we already use.
- It provides 575 animation files:
  - 320 combat;
  - 102 skill;
  - 91 movement;
  - 34 death;
  - 23 execution.
- The weapon movesets include halberd, battle staff, saber, florett, iron fist, sickle, knife, hook claws, dual daggers and light greatsword.
- **Licence:** All Rights Reserved. The author allows other projects to use its animations "as long as this mod is a dependency". As with Epic Fight, **nothing is copied into our jar**: we reference the installed mod.

## Integration

- **Dependency.** It is a required dependency, pinned like Epic Fight:
  - `dawn_day_version=21.17.1.2.1`;
  - Modrinth artifact `kCZCK2dK` (`gradle.properties`, `build.gradle`, `neoforge.mods.toml`).
  - It loads alongside Epic Fight 21.17.3.1.
- **Items retired.** `EpicFightContentPolicy` treats the `epicfight_dd` namespace like `epicfight`. This retires:
  - its about 20 weapons, skill books, gems, materials and potions (vanilla potion items holding its potion types);
  - them from the creative menu (its tab, once empty, disappears), from inventories, chests, loot, mob equipment and recipes.
  - Its skills (Redemption, Iron Will, weapon innates and the Wing stance) are only reachable through those items.
- **Key.** Its only key, *Switch Stance* (J), is unbound and hidden in `EpicFightControls`, like the native Epic Fight keys. Nothing of Dawn Day remains in the Controls screen.
- **HUD and settings.** It has no HUD of its own outside its retired skills, and no configuration screen (only an `epicfight_dd-common.toml` file).

## What it still changes, to decide

The mod keeps two behaviours, through a mixin on `PlayerPatch`:
- **Player death:** players die with its "expressive" death animation.
- **Players knocked down by a `KNOCKDOWN` blow** (Crescent, Leaping Slam, etc.) fall with its animation instead of Epic Fight's.

Its custom death animations also apply to humanoid mobs that use Epic Fight's death animation (bandits, Chief, Sensei, Heart Demon); this is `mob_death_anim` in its config.

To be kept or undone as Enzo prefers. Undoing them takes our own mixin of higher priority, or a config default.

## Known noise in the log

None of this blocks loading or our tests. It comes from Dawn Day itself:
- a few optional animations need Combat Evolution;
- one recipe (`herb_sickle`) is broken;
- one mixin targets the absent Weapons of Minecraft.

## Checks (15/10/2026)

- `./gradlew build compileGameTestJava`: passing.
- `runGameTestServer`: 207 tests passing, including `dawnDayContentIsRetiredButLoaded`.
- `runVisualSmoke` in a real client: passing. It checks that the controls have one Murim category with 13 useful keys and no Dawn Day key, and that no native item is present.

## Using its animations

They are read by Epic Fight like ours, under `epicfight_dd:biped/...`, for example `epicfight_dd:biped/combat/halberd_auto1`. Their accessors are in `net.dawn_day.gameasset.animation.DawnDayAnimations`.

The Alliance's planned weapons map well onto them:
- halberd → guandao;
- battle staff → monk's staff;
- florett or saber → jian.
