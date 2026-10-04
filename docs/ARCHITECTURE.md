# Murimblock Architecture

Murimblock is a Minecraft 1.21.1 NeoForge mod built around server-authoritative gameplay systems and a small public addon API.

## Package Map

`com.murimblock`

- `Murimblock`: mod entry point and NeoForge event registration.
- `api`: public Java API for addons.
- `api.qi`: supported Qi addon contract.
- `api.cultivation`: supported Cultivation addon contract.
- `api.combat`: supported Combat addon contract and combat mode change event.
- `combat`: combat mode, weapon/art profiles, temporary sword guard state and server services.
- `combat.attack`: shared trajectory and contact core; not connected to live attacks yet.
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
- Sword guard eligibility, Qi costs, damage interception and guard break are validated by `MeleeCombatService`.
- Mob kill Qi rewards are calculated and applied server-side by `QiRewardManager`.
- Attachments are registered through NeoForge and stored per player.

Client-only classes remain in `com.murimblock.client`:

- `QiChargeClientHandler`: key input and client movement input lock preview.
- `QiChargeClientEffects`: first-person foreground particles.
- `QiChargeFovHandler`: charge FOV transition.
- `MurimblockKeyMappings`: key registration.
- `CombatModeClientHandler`: combat toggle key input.
- `SwordCombatClientHandler`: guard intent, vanilla arm poses and hand recoil.
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

Combat Mode is the temporary normal/combat toggle. A separate unsaved melee
attachment represents guard, attack recovery, impact and guard-break actions.
The clash enum ID remains reserved but this live slice generates no clashes.

Main classes:

- `CombatData`: immutable temporary combat state.
- `CombatAttachments`: internal unsaved NeoForge attachment registration.
- `CombatService`: server-authoritative reads, set and toggle operations.
- `CombatEvents`: login, logout and clone reset behavior.
- `CombatCommands`: `/combat check`, `/combat on`, `/combat off`, `/combat toggle`.
- `CombatModeTogglePayload`: client to server toggle request with no client-chosen state.
- `CombatModeClientHandler`: sends toggle requests when the configurable key is pressed.
- `WeaponCategory`: classifies weapons without modifying unsupported families.
- `CombatProfile` / `CombatProfiles`: rules identified by weapon category and martial-art ID.
- `MeleeCombatService`: sword guard input leases and server-authoritative damage interception.
- `GuardRequest`: held-input timeout and copied slot/weapon binding.

Flow:

```text
client key press
network toggle payload
server CombatService
CombatData attachment sync
MurimblockApi.combat()
```

Only `basic_sword` is enabled. A frontal sword guard cancels direct melee damage
at a Qi cost. An attack suspends guard during a short recovery but its hit stays
vanilla, without deferral or a second damage owner. Axes, ranged weapons, unarmed
combat and other categories have no installed custom behavior. There is no
learned-art selection or save-data change yet. A future art resolver will select
compatible profiles and authored attacks rather than modify every sword globally.
The old mutual-target clash shortcut is no longer registered or used. The shared
blade-contact core remains separate until animation and swept collision are ready.
All eligibility and damage decisions stay on the server; the client sends only
guard intent. The existing Combat addon API is unchanged.
See `docs/SWORD_COMBAT.md` for controls, restrictions and the validation plan.
No technique bar, combo system or hotbar replacement exists yet.

Combat Mode currently activates one HUD replacement:

- vanilla `experience_bar` and `experience_level` layers are cancelled while combat mode is active;
- `CombatQiHud` redraws the vanilla experience bar background at the vanilla coordinates;
- the progress sprite keeps the vanilla experience bar dimensions and shape, but is recolored blue and filled from `Qi / Qi Max`;
- no numeric Qi value is rendered in the HUD;
- player XP values are not modified, and the vanilla XP level is only hidden visually while combat mode is active.

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
- Client to server: `GuardStatePayload`, sent on guard input changes with a held-input refresh.
- Server to client: Qi attachment sync for the owning player through `QiAttachments`.
- Server to client: Combat attachment sync for the owning player through `CombatAttachments`.
- Server to client: temporary melee action sync to the owner and tracking players.

Guard input refreshes every ten client ticks while held and expires after thirty
server ticks without a refresh. Release removes protection immediately; equipment,
dimension and player lifecycle checks prevent stale guard ownership. Network
version 2 includes the guard channel and requires matching client/server builds.

Dev-only server tests live in `src/gameTest`, are enabled with
`-PcombatGameTests`, and are not packaged in the mod jar. The GameTest server uses
`build/game-test-run`, not the user's development world.

Addons should not use Murimblock internal packet classes to read or mutate Qi or combat mode. They should use `com.murimblock.api`.

## Data And Configuration Direction

Current gameplay values are still Java constants or Java tables. This is acceptable for the current young codebase, but the intended direction is:

- balance values that designers change often should move toward data files;
- global server toggles and multipliers should move toward server config;
- public addon integrations should go through `com.murimblock.api` and future data maps or datapack data.

See `docs/DATA_DRIVEN.md` and `docs/QI_REWARDS.md`.
