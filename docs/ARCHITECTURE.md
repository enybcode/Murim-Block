# Murimblock Architecture

Murimblock is a Minecraft 1.21.1 NeoForge mod built around server-authoritative gameplay systems and a small public addon API.

## Package Map

`com.murimblock`

- `Murimblock`: mod entry point and NeoForge event registration.
- `api`: public Java API for addons.
- `api.qi`: supported Qi addon contract.
- `api.cultivation`: supported Cultivation addon contract.
- `api.combat`: supported Combat addon contract and combat mode change event.
- `combat`: actual Epic Fight mode bridge, unsaved HUD mirror and addon events.
- `integration.epicfight`: pinned engine boundary, basic actions and native content policy.
- `mob`: entity registry and summon-only TrainingOpponent prototype; no natural spawns.
- `qi`: Qi implementation, player data, reward calculation, attachments, server events.
- `qi.charge`: Qi charging gameplay state and charge VFX tuning helpers.
- `cultivation`: Cultivation implementation, progression table, attachments and commands.
- `network`: internal packets. Addons should not depend on these packets.
- `command`: debug/admin commands.
- `client`: client-only input, HUD, FOV and foreground effects.

## Public API Boundary

Only `com.murimblock.api.*` is considered addon-facing.

Implementation packages such as `com.murimblock.qi`, `com.murimblock.cultivation`, `com.murimblock.network`, `com.murimblock.command`, and `com.murimblock.client` are not compatibility contracts. Addons may compile against them during early experiments, but they should expect breaking changes while Murimblock is in the `0.x` series.

## Server And Client Separation

Gameplay authority lives on the logical server:

- Qi mutation uses `ServerPlayer`.
- Qi charging state is validated server-side by `QiChargeService`.
- Combat mode is toggled and validated server-side by `CombatService`.
- Mob kill Qi rewards are calculated and applied server-side by `QiRewardManager`.
- Attachments are registered through NeoForge and stored per player.

Client-only classes remain in `com.murimblock.client`:

- `QiChargeClientHandler`: key input and client movement input lock preview.
- `QiChargeClientEffects`: first-person foreground particles.
- `QiChargeFovHandler`: charge FOV transition.
- `MurimblockKeyMappings`: key registration.
- `CombatModeClientHandler`: combat toggle key input.
- `client.gui.MurimProfileScreen` / `MurimProfileLayout`: adaptive Clear Manuscript pages, native pixel geometry and scoped bitmap font.
- `TrainingOpponentRenderer`: client registration for the provisional Zombie appearance; Epic Fight preset owns animated rendering.
- `client.hud.CombatQiHud`: redraws the vanilla experience bar background with a blue Qi progress sprite while Combat Mode is active, without numeric Qi text.

Server code must not import `Minecraft`, `ClientLevel`, `GuiGraphics`, `Camera`, or `KeyMapping`.

## Qi

Main classes:

- `QiData`: immutable Qi and Qi Max data with invariants.
- `QiAttachments`: internal NeoForge attachment registration.
- `QiService`: internal implementation service for reading and mutating Qi.
- `QiEvents`: login, regeneration tick and kill reward hooks.
- `QiRewardManager`: centralized mob kill reward calculation and award logic.
- `QiKillTracker`: temporary anti-farm kill history.
- `QiBossProgress`: persistent first boss victory state.
- `QiFormat`: shared numeric formatting utility.

Addon entry point:

```java
double qi = MurimblockApi.qi().getQi(player);
MurimblockApi.qi().addQi(serverPlayer, 25.0);
```

## Qi Charging

Qi charging is split into clear concerns:

- `QiChargeState`: small state object.
- `QiChargeService`: server-side state and movement locks.
- `QiChargeEvents`: lifecycle hooks and world particles.
- `QiChargeParticleEffects`: server particle spawning calculations.
- `QiChargeVisuals`: visual constants and interpolation helpers.
- `QiChargeStatePayload`: client to server intent packet.

The client sends intent only. The server decides whether charging is valid.

## Combat

Combat Mode reads Epic Fight's actual player mode. The engine owns attacks,
animation, damage, guard, collision and persistence. Murimblock maintains an
unsaved mirror only for attachment synchronization and the existing addon event.

Main classes:

- `CombatData`: immutable temporary combat state.
- `CombatAttachments`: internal unsaved NeoForge attachment registration.
- `CombatService`: server-authoritative reads, set and toggle operations.
- `CombatEvents`: clears stale mirrors without overwriting Epic Fight's saved mode.
- `EpicFightBridge`: engine mode, activity checks and Qi-charge cast exclusion.
- `EpicFightStaminaMixin`: common-side stamina compatibility shim; no stamina
  depletion or limits, without changing Qi, skill cooldowns or saved engine IDs.
- `EpicFightCombatDefaults`: basic guard/roll without books; remove equipped native
  progression and special skills, preserving other namespaces and engine attack/wakeup.
- `EpicFightContentPolicy`: creative/recipe/loot/legacy-item retirement scoped to
  `epicfight`, without unregistering IDs or changing vanilla item content.
- `RemoveEpicFightLoot`, `EpicFightContentRegistries`: additive NeoForge global loot filter.
- `EpicFightLootMixin`: suppress native bonus book loot construction/injection.
- `EpicFightRecipesMixin`: recipe reload filter for native recipes/explicit native outputs.
- `EpicFightStrayEquipmentMixin`: keep skeleton combat initialization but skip the
  native robe assignment that would overwrite a Stray's vanilla equipment.
- `CombatCommands`: `/combat check`, `/combat on`, `/combat off`, `/combat toggle`.
- `CombatModeTogglePayload`: client to server toggle request with no client-chosen state.
- `CombatModeClientHandler`: sends toggle requests when the configurable key is pressed.

Flow:

```text
client key press
network toggle payload
server CombatService -> EpicFightBridge -> Epic Fight PlayerPatch mode
actual state -> CombatData mirror + CombatModeChangedEvent
MurimblockApi.combat() reads actual PlayerPatch mode
```

Epic Fight 21.17.3.1 is installed as a required dependency. See `docs/EPIC_FIGHT.md`.
Techniques is empty and reserved for Murim progression. No native book-learning
payload or skill service remains. Martial styles and four configurable M1 moves
are not implemented by this integration and need a later design.

Combat Mode activates these HUD replacements:

- vanilla `experience_bar` and `experience_level` layers are cancelled while combat mode is active;
- `CombatQiHud` redraws the vanilla experience bar background at the vanilla coordinates;
- the progress sprite keeps the vanilla experience bar dimensions and shape, but is recolored blue and filled from `Qi / Qi Max`;
- no numeric Qi value is rendered in the HUD;
- player XP values are not modified, and the vanilla XP level is only hidden visually while combat mode is active.
- `EpicFightHud` cancels only the four native Epic Fight HUD layers, without
  native skill tiles or stamina. A compact charge indicator remains an extension
  point for a future permitted chargeable action; normal basic actions do not use it.
- `EpicFightGuiAdapter` routes native screens into Murimblock; outside a world
  it opens vanilla Options instead of a player-dependent profile.
- `EpicFightControls` regroups native input objects and filters eight redundant
  presentation rows. Three access-transformed fields affect presentation only.

Addon entry point:

```java
boolean inCombat = MurimblockApi.combat().isInCombatMode(player);
```

## Cultivation

Main classes:

- `CultivationData`: immutable player cultivation state.
- `CultivationAttachments`: internal NeoForge attachment registration.
- `CultivationService`: implementation service for state reads and progression mutations.
- `CultivationProgression`: current hardcoded progression table.
- `CultivationRealm`, `CultivationStage`, `BreakthroughType`: domain values.
- `CultivationEvents`: login initialization.
- `CultivationCommands`: debug/admin commands.

Addon entry point:

```java
CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);
boolean ready = MurimblockApi.cultivation().canAttemptBreakthrough(player);
```

## Networking

Current packets:

- Client to server: `QiChargeStatePayload`, sent when the local charge key state changes.
- Client to server: `CombatModeTogglePayload`, sent once per consumed combat key press.
- Server to client: Qi attachment sync for the owning player through `QiAttachments`.
- Server to client: Combat attachment sync for the owning player through `CombatAttachments`.

Network version 7 requires matching client/server builds. The Murimblock mirror
is not persisted; Epic Fight saves its own mode. Qi/cultivation IDs are unchanged.
Epic Fight carries its own attack/animation/skill synchronization packets.

Dev-only server tests live in `src/gameTest`, are enabled with
`-PgameTests`, and are not packaged in the mod jar. The GameTest server uses
`build/game-test-run`, not the user's development world.

Addons should not use Murimblock internal packet classes to read or mutate Qi or combat mode. They should use `com.murimblock.api`.

## Data And Configuration Direction

`MurimEntities.TYPES` is connected to the mod bus but has no entries. Preparation
does not add a fake NPC, renderer, spawn rule or world migration. Future mob entity
code/attributes/AI belongs under `mob`; rendering stays client-only. Epic Fight
mob patches and animation registration must use the pinned API, not a second
damage loop. Inactive examples and the designer handoff are documented in
`MOB_CREATION_GUIDE.md`. Unknown entity rewards currently fall back to the existing
Qi reward rules; each new mob must be deliberately balanced there.

Current gameplay values are still Java constants or Java tables. This is acceptable for the current young codebase, but the intended direction is:

- balance values that designers change often should move toward data files;
- global server toggles and multipliers should move toward server config;
- public addon integrations should go through `com.murimblock.api` and future data maps or datapack data.

See `docs/DATA_DRIVEN.md` and `docs/QI_REWARDS.md`.
