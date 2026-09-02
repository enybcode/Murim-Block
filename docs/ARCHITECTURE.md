# Murimblock Architecture

Murimblock is a Minecraft 1.21.1 NeoForge mod built around server-authoritative gameplay systems and a small public addon API.

## Package Map

`com.murimblock`

- `Murimblock`: mod entry point and NeoForge event registration.
- `api`: public Java API for addons.
- `api.qi`: supported Qi addon contract.
- `api.cultivation`: supported Cultivation addon contract.
- `api.combat`: supported Combat addon contract and combat mode change event.
- `combat`: temporary combat mode implementation, attachment and server service.
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
- `QiDebugOverlay`: temporary Qi HUD.
- `MurimblockKeyMappings`: key registration.
- `CombatModeClientHandler`: combat toggle key input.
- `client.hud.CombatQiHud`: replaces the vanilla experience layer with a Qi bar while Combat Mode is active.

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

Combat mode currently represents only one temporary state: normal or combat.

Main classes:

- `CombatData`: immutable temporary combat state.
- `CombatAttachments`: internal unsaved NeoForge attachment registration.
- `CombatService`: server-authoritative reads, set and toggle operations.
- `CombatEvents`: login, logout and clone reset behavior.
- `CombatCommands`: `/combat check`, `/combat on`, `/combat off`, `/combat toggle`.
- `CombatModeTogglePayload`: client to server toggle request with no client-chosen state.
- `CombatModeClientHandler`: sends toggle requests when the configurable key is pressed.

Flow:

```text
client key press
network toggle payload
server CombatService
CombatData attachment sync
MurimblockApi.combat()
```

No HUD, technique bar, damage system, combo system or hotbar replacement exists yet.

Combat Mode currently activates one HUD replacement:

- vanilla `experience_bar` and `experience_level` layers are cancelled while combat mode is active;
- `CombatQiHud` draws a blue Qi bar at the vanilla experience bar location;
- player XP values are not modified.

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

Addons should not use Murimblock internal packet classes to read or mutate Qi or combat mode. They should use `com.murimblock.api`.

## Data And Configuration Direction

Current gameplay values are still Java constants or Java tables. This is acceptable for the current young codebase, but the intended direction is:

- balance values that designers change often should move toward data files;
- global server toggles and multipliers should move toward server config;
- public addon integrations should go through `com.murimblock.api` and future data maps or datapack data.

See `docs/DATA_DRIVEN.md` and `docs/QI_REWARDS.md`.
