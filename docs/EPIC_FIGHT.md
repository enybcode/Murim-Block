# Epic Fight Integration

## Pinned Environment

- Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
- Epic Fight **21.17.3.1**, published Modrinth artifact **8HHhJt6i**.
- Exact required dependency on client and server, installed by Gradle for IntelliJ.
- Murimblock network protocol **7** requires matching client/server builds.
- No extracted Epic Fight classes, textures, meshes or animations in Murim's jar.

The published binary and sources jar were inspected, not only the README or an
unreleased branch. Ignored references live in `.local/epicfight`. Its embedded
dependencies remain upstream-owned; this is not a Fabric or newer-API port.

## Engine And Murim Responsibilities

Epic Fight owns animation, rendering, attack clock/colliders, damage, basic combos,
guard, supported entity patches and attack synchronization. Murim owns Qi,
cultivation, rewards, screens, basic-action/content policy and its public addon API.
`EpicFightBridge` reads the actual player patch; `CombatData` is an unsaved HUD/event
mirror, not another combat authority. Epic Fight persists its actual mode/skills.
Logout clears only the mirror. Mode switches respect the engine gamerule/events.

Supported vanilla mob/boss patches remain active; this is not sword-only combat.
Outside combat mode Epic Fight still allows its vanilla-mode attacks. No new
vanilla-combat removal, boss rewrite, style engine or blade clash is claimed here.

Qi charging cannot start during an action/held skill. Casts while charging are
rejected server-side. Movement lock, regeneration, existing rewards and save IDs
remain unchanged. No attack, guard or dodge cost was transferred to Qi.

## Basic Combat, Not Native Progression

`EpicFightCombatDefaults` enforces the basic **Guard** and **Roll** in their slots
on the server. They require no book or Techniques interaction. Changes use native
owner/observer sync packets; repeated enforcement does not reset unchanged skills.
Compatible weapons, ground requirements, action/interrupt rules and directional
guard are still enforced by the engine.

Native basic attack/combo and knockdown recovery remain. Other native skills are
cleared from equipped slots and denied by the cast hook, including native passive,
mover, identity and weapon-innate progression. Skills from other namespaces are
not stripped, leaving room for future Murim integrations. This is not a complete
anti-cheat audit of every upstream packet; saved learned IDs remain inert, not
globally erased from player NBT.

Techniques is **empty**, reserved for Murim. The native skill library, scrolling,
learning/equip controls, `EpicFightSkillService` and `TechniqueChangePayload` have
been removed. Martial styles/four configurable M1 slots are still future work.

## Content Retirement And Existing Worlds

**Back up existing worlds before testing. Removed items are not recoverable without
a backup.** Content retirement is scoped to item namespace `epicfight`:

- Creative parent/search contents: use `BuildCreativeModeTabContentsEvent.remove`,
  not mutation of its read-only collection views. Empty native tabs are not useful
  catalogues; no engine items are exposed by rebuilt tabs/search.
- Recipes: a reload mixin removes native recipe IDs and foreign recipes explicitly
  producing a native item through the standard `result` formats. Unknown custom
  recipe formats are not indiscriminately deleted.
- Loot: native bonus book registration/table injections are disabled. An additive
  NeoForge global loot modifier removes engine item stacks while preserving other
  drops. Its list uses `replace: false`, retaining other mods' modifiers.
- Legacy player inventory/ender chest: cleaned on login and every twenty server
  player ticks; open slots and cursor contents are cleaned too.
- Containers: cleaned when opened, not by scanning or rewriting the whole world.
- Native item entities: rejected on server world join, including direct boss drops.
- Living equipment: native stacks removed; vanilla/Murim stacks are untouched.
- Native item use/attacks: denied while legacy/given stacks await cleanup.
- Stray patch: keep superclass skeleton combat setup, skip only native robe
  assignment that would overwrite a vanilla helmet/chest/legs.

Registry IDs/classes remain loaded for save, engine capability and network safety.
An operator can still reference them with `/give`; this is content suppression,
not unregistration. Closed/unloaded chests, nested item NBT and offline player
files are not exhaustively migrated. JEI/EMI integration is not supplied. Third-party
custom loot/recipe formats and ordering need specific compatibility tests; a mod
could reinsert content after our filter. No unrelated save migration is performed.

## Interface And Controls

- Defaults: **K** Profile, **V** combat mode, **R** charge Qi; guard right mouse,
  dodge Left Alt, attack left mouse, lock-on G. User bindings may differ.
- One Murim category with **ten** visible controls: three Murim controls, attack,
  guard, dodge and four target-lock controls. Eight native presentation rows are
  hidden; native menu/mode/debug/emote/innate/mover shortcuts are disabled including
  reset defaults. Engine input objects remain registered; unrelated bindings stay.
- Native book/skill screens defensively route to the empty Techniques tab. Other
  native presentation/config/editor screens route to Info / Combat Settings.
  Outside a world they route to vanilla Options, not a player-dependent screen.
- Settings retain camera type, automatic perspective, first-person camera/body,
  lock-on snapping, blood and vanilla keybindings. Advanced native editors, emote
  and cosmetic tools are not recreated.
- Four native HUD layers are canceled: stamina, skills, weapon innate and charging.
  Native skill tiles, health/target indicators and the yellow stamina bar are absent.
  The compact blue Qi replacement of XP remains; actual XP data is untouched.
  A small charge indicator is an extension point for a future permitted chargeable
  action, not a replacement yellow endurance system.
- Manuscript font stays scoped to Murim screens. Player-facing text is English.
  Profile alone shows the 3D player. Existing approved pixel-art assets are retained.

Three access-transformed fields are used for controls presentation. A client-only
mixin suppresses the transient upstream version banner; mod-list/version credits
remain. The old pinned wooden-sword capability override resolves upstream sound/
particle IDs without copied assets; recheck it on dependency updates.

## Stamina Policy

The common-side `EpicFightStaminaMixin` targets `PlayerPatch.getStamina`,
`hasStamina` and `setStamina` on both client and server. Reads return the existing
maximum; finite nonnegative costs are affordable and writes cannot deplete it.
Saved attribute/data IDs remain. This is not a larger stamina pool or faster regen.

No exhaustion-based guard break and no health fallback from insufficient stamina.
Directional guard, airborne dodge rejection, cooldown/weapon-charge predicates and
explicit unrelated health resources remain. No stamina-only upgrades are exposed
as Murim techniques. The policy is version-pinned, not an upstream API guarantee.

## Mob Preparation

`MurimEntities` is a registered **empty** DeferredRegister. No fake mob, natural
spawn or placeholder asset is introduced. See the French
[mob/animation guide](MOB_CREATION_GUIDE.md) and
[request template](examples/mobs/MOB_REQUEST_TEMPLATE.md).

The inactive behavior fragment is deserialized by the actual pinned engine in a
server GameTest. This verifies its schema/animation IDs, not a rendered new mob.
The zombie preset only suits a compatible Zombie-derived entity. Mob creation
still needs a chosen design, legal resources, rig, attributes/AI/renderer, combat
integration and validation before enabling natural spawns.

## Verification

```powershell
.\gradlew.bat build compileGameTestJava
.\gradlew.bat -PgameTests runGameTestServer
.\gradlew.bat -PgameTests runVisualSmoke
.\gradlew.bat prepareClientRun
```

The last task restores normal IntelliJ preparation without dev-only harness classes.
Server tests use `build/game-test-run`. The visual harness requires an existing
disposable copied world at `build/client-smoke-run/saves/murim-smoke`; never overwrite
an active world or use a user save for cleanup tests. Reports/screenshots are local
build output, not release assets. The published jar excludes both harnesses.

### Checks Recorded On 2026-10-08

- 114 unit checks, zero failures/errors.
- 19 required real server GameTests: one animated sword damage contact, six vanilla
  sword tiers, default bookless guard/roll, rejected special cast, native recipe
  removal with vanilla recipe preservation, registered global loot cleanup, legacy
  inventory/chest/cursor cleanup, rejected book use/drop and preserved Stray armor.
- Existing mode/Qi/cultivation, charging movement/lifecycle/save tests remain.
  Actual grounded dodge and front-only guard still spend no stamina or Qi.
- Real client checks: native screen routes, empty Techniques (five navigation
  widgets only), synchronized guard/roll, one category with ten controls, rebuilt
  creative/search tabs without engine items and common-side stamina policy.
- Captures cover four tabs, settings, controls, HUD, small window and actual creative
  inventory at 427x240 and 320x240 GUI sizes. A final passed marker, not process exit
  alone, confirms the opt-in client run completed.

Automated tests do not prove feel, all addon compatibility or a network latency
scenario. Embedded server connections exercise real logic but are not two remote
players. Before merge, user validation must cover:

1. Restart/reload IntelliJ, Techniques empty, creative/search content absent, vanilla
   crafting/loot unchanged and legacy cleanup on a disposable world copy.
2. Third-person combos, guard front/back, dodge, target lock, rebinding/Reset All.
3. Tools/shield/bow/building outside combat; Qi charging and interruptions.
4. Relog, death/respawn, dimensions and equipment changes mid-action.
5. Two clients on a dedicated server: owner/observer animation, single damage,
   late tracking, disconnect and reconnect, default guard/roll synchronization.

Keep `main` unchanged until explicit in-game approval. No absence-of-bugs promise.

## Upgrade Checklist And Sources

Recheck engine skill/container APIs, namespaces, native screen/HUD names, input
fields and gamerules. Also recheck all common mixin signatures: PlayerPatch stamina,
Minecraft RecipeManager.apply, EpicFightLootTables book methods and StrayPatch
onJoinWorld/setItemSlot. Required mixin targets intentionally fail loudly if the
pinned implementation changes; do not upgrade the jar without testing both sides.

Known upstream diagnostics can mention missing subtitles, `epicfight:air_slash`,
optional WaveyCapes integration and the optional Epic Fight web service connection.
Removing gameplay items does not remove dependency resources/classes/services or
guarantee a silent upstream log. Those are not reported as fixed in this change.

- [Pinned published release](https://modrinth.com/mod/epic-fight/version/21.17.3.1-mc1.21.1-neoforge).
- [Official sources](https://github.com/Antikythera-Studios/epicfight/tree/1.21.1).
- [API setup](https://epicfight-docs.readthedocs.io/API/Starting/).
- [Asset terms](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS).
- [NeoForge loot documentation](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/).

Engine code and animation assets have separate terms: GPL code versus reserved
assets. Referencing an installed animation is not permission to redistribute its
export. No such asset extraction/copy is performed by this integration.
