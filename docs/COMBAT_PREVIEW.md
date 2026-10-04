# Sword Animation Preview

First implementation slice following `COMBAT_REWORK_PLAN.md`, 2026-10-04.
Candidate for `test`, not user-approved for `main`.

## Scope

This is a server-triggered visual preview, **not a new gameplay attack**.
The original clip turns the body, moves the torso, head, both arms and legs,
and rotates the held sword's item socket. It lasts 24 ticks (1.2 seconds at
20 TPS): preparation 0-8, illustrative cut 8-12, recovery 12-24. The shared
`AttackTimeline` describes those phases, but no contact or damage code uses it.
There is no root translation, movement lock, custom camera or new HUD.

Vanilla attacks, current sword guard and its existing Qi costs, V/R/K controls,
equipment categories, cultivation, rewards, saves and the addon API stay in place.
No Epic Fight code, animation, rig, model or shader has been imported.

## Development Setup And In-Game Test

Minecraft 1.21.1, NeoForge 21.1.248 and Java 21 are the qualified project versions.
PAL is pinned to `com.zigythebird.playeranim:PlayerAnimationLibNeo:1.1.6+mc.1.21.1`.
Its mod ID is `player_animation_library`; both client and server require that
exact version. This is a rendering-library dependency, not Epic Fight's engine.

1. Reload the Gradle project in IntelliJ after pulling `test`, then restart `runClient`.
2. Open a test world with cheats, or join the same-build dedicated server as an operator (permission level 2).
3. Hold a sword in the main hand with an empty offhand. Finish attacking, guarding or charging. Combat Mode need not be on.
4. Press F5 to observe the skin, then run `/combat preview`. Repeat from the front, back and side.
5. `/combat preview stop` ends it early. Normal attacking, item/block interaction, guard or charging also stops the preview without blocking the requested action.

Gradle automatically adds PAL to development runs. For a normal launcher or
server, install the matching [PAL NeoForge release](https://modrinth.com/mod/player-animation-library/version/yjxtkvnD)
alongside Murimblock. PAL already contains its Molang and Javassist dependencies;
do not add separate jars for them. It is **not** bundled in Murimblock's jar.
Client/server protocol is now version 3, rejecting old Murimblock protocol-2 peers.
Do not use a gameplay resource pack to replace this clip while qualifying the
baseline; optional replacement clips must keep the tested ID/duration/bones.

## Modules And Contracts

| File / Symbol | Responsibility |
| --- | --- |
| `combat/preview/PreviewDefinition` | Resource ID, shared phase timeline and right/left item socket names |
| `combat/preview/PreviewState` | Immutable unsaved generation/start/sample/duration/playing snapshot; bounded codec |
| `combat/preview/PreviewPlayback` | Local tick mapping, late seek, duplicate/stale rejection, terminal stop and local expiry |
| `combat/preview/CombatPreviewService` | Server validation, copied weapon/slot/dimension/arm binding, start/cancel/expiry and tracking refresh |
| `combat/preview/CombatPreviewEvents` | Concrete attack/use/interact events, death/logout/clone/dimension, tracking and server lifecycle |
| `combat/CombatAttachments.PLAYER_PREVIEW` | NeoForge synchronization to the owner and entity trackers; no disk serializer or copy-on-death |
| `client/animation/CombatPreviewClientEvents` | Register one PAL layer per client player, priority 800, CLIENT distribution only |
| `client/animation/PalPreviewController` | Isolated PAL rendering backend, absolute-age seek, mirrored pose, resource reload/missing-clip handling |
| `assets/murimblock/player_animations/sword_preview.json` | Original numeric Bedrock-format keyframes; nonlooping; zero root motion |
| `command/CombatCommands`, `en_us.json` | Operator-only preview commands and translated English feedback |
| `MeleeCombatService.requestGuard`, `QiChargeService.startCharging` | Small cancellation hooks; no changed guard/charging cost or eligibility |

The snapshot is addressed by NeoForge's entity attachment protocol; its actor
identity/lifetime is the entity, not a global client UUID map. Reliable ordered
S2C attachment packets and the synced attachment registry are used instead of
adding another parallel packet transport. A client cannot submit a preview age,
target, pose or damage through this system. NeoForge's initial entity attachment
sync supplies newly tracking observers; `StartTracking` refreshes the sample age.

Active snapshots refresh at most every four server ticks, plus start/stop and
tracking changes. At receipt, the renderer anchors the accepted server age to
the player's simulation tick count, not wall-clock time or level day time.
Identical snapshots do not reset the anchor. Same-generation corrections never
rewind. Each frame samples that age plus one partial tick, without independent
PAL tick advancement. Off-screen rendering does not pause or restart the action.
A new generation resets playback; stop is terminal for the old generation.

This is **not latency-compensated final combat synchronization**: packet transit,
stalling and animation interpolation may still offset two clients. No RTT
estimate or network rollback is claimed. A bounded server clock/pose sampler
must be qualified before this timeline authorizes real contacts or damage.

Server acceptance requires an alive, nonspectating sword user, empty offhand,
no item use, active melee state, charging, swing, riding, sleeping, swimming or
elytra flight. The preview does not reserve or lock the player. A copied stack,
selected slot, dimension and main arm bind each action. Changes cancel it;
switching to an identical sword in a different slot cannot reuse it.
Interaction/attack/use events cancel without canceling the historical event.
Swinging at air and incompatible state/equipment changes are checked by the
server tick, so some interruptions take up to one server tick.
Finish, death, logout, clone, dimension change and server stop clean temporary
state/bindings. The preview is not serialized into player saves.

PAL operates on Minecraft's existing skin/model, including slim/classic and
armor render layers. Mirroring reads the animated player's actual main arm,
including remote players. First-person mode is `NONE`: this lot leaves vanilla
first-person hands and camera in place rather than adding an unqualified camera.
Missing clips produce a bounded warning and no pose; normal combat still works.
Resource reload replaces the loaded clip at the current age, not at zero.

The declared socket names are an extension contract, **not** a verified server
world-space blade trajectory. A later lot must bake the complete transform chain,
units, handedness and weapon offset for a shared server/client pose sampler.
Do not use the old instantaneous capsule resolver to pretend this preview hits.

## Dependency And Asset Provenance

The clip was authored specifically for Murimblock by Codex at the user's request,
as original numeric keyframes on the standard cubic player-bone API. It is not
an Epic Fight export or a retargeted external clip. It inherits the project's
current asset licensing; it does not change Murimblock's overall license.

PAL's source audit used branch 1.21.1 commit
`10e019f89fa25d0cd6f50fb8768586a969106911`; runtime qualification uses the actual
publisher's NeoForge binary, not a source-built guess. SHA-256 observed:

- PAL: `3cab280cadd766fb0b92810bad74b0505ddac54d8d9d6c09d4dc05dc5ca8cb47`.
- Embedded `mochafloats-4.1.0.jar`: `cda96e88268736147c8bd32239fb95bd79035500f2ee3b7a26029e1c232fe94a`.
- Embedded `javassist-3.30.2-GA.jar`: `55a8a955e6b04b5ef0ed8591384d34b09174f248d5ef0c2e956f63c9f3bafded`.

The separately published Molang 4.1.0 binary had a different hash. Therefore
`extractPalRuntime` extracts PAL's exact nested jars **under build only** for
compile/headless tests. `implementation` is nontransitive, preventing a second
core or a different Molang/Netty runtime. Production continues to let PAL load
its own nested dependencies. Extracted libraries are not copied into Murimblock.
These hashes record the observed qualification, not enforced dependency verification.

- [PAL MIT license](https://github.com/PlayerAnimationLibrary/PlayerAnimationLibrary/blob/10e019f89fa25d0cd6f50fb8768586a969106911/LICENSE): ZigyTheBird; its inherited files also include KosmX and GeckoLib MIT notices.
- [MochaFloats MIT license](https://github.com/PlayerAnimationLibrary/mochafloats/blob/main/license.txt): Unnamed Team, fork used by PAL. No Mocha source is copied here.
- [Javassist licensing](https://github.com/jboss-javassist/javassist/blob/rel_3_30_2_ga/License.html): upstream alternative MPL/LGPL/Apache terms. No Javassist code is copied or shaded here.
- [PAL API documentation](https://docs.zigythebird.com/pal/intro/) and the audited implementations of `PlayerAnimationRegisterEvent`, `PlayerAnimResources`, `AnimationController`, `MirrorModifier` and `FirstPersonMode` informed the adapter.
- NeoForge 21.1.248 `AttachmentType`, `AttachmentSyncHandler`, `AttachmentSync` and concrete interaction events were inspected from the installed sources.

Keep upstream notices/licenses with their artifacts. Before any future bundling
or copying, ship the full applicable notices and re-audit transitive licenses;
do not infer asset permission from PAL's MIT code or Epic Fight's GPL code.
Epic Fight reuse blockers remain documented in `COMBAT_REWORK_PLAN.md`.

## Verification And Remaining Gates

Automated coverage added:

- Snapshot bounds, timing, exclusive expiry, generation overflow and codec roundtrips.
- Late observer seek, duplicate/stale snapshots, no rewind/restart, terminal stop, partial-tick limits and frozen clock.
- Real PAL loader and headless controller sampling: finite poses, matching 24-tick duration, return to neutral, no looping, mirrored arms/weapon sockets and no scripted effects.
- Operator permission 2 for preview, preserving public legacy combat commands.
- Eight new GameTests: no health/Qi/movement/durability change; normal attack still applies exactly once; preview refusal; guard/charge interruption; slot/item-use interruption; late tracking age; death/dimension/no persistence; clone/logout.

Executed in the IntelliJ checkout: fresh `test build --rerun-tasks --no-build-cache`,
224 tests passed (including 15 unrelated local prototype tests). All 22 required
dedicated-server GameTests passed, including the 14 existing sword guard tests.
The PAL mod and nested dependencies were actually discovered on that server;
the client backend was not loaded there. Two existing keybind deprecation
warnings remain outside this lot.

The isolated Git-selected source export also passed a fresh build: 209 tests
across 31 suites, zero failures/errors/skips, and all 22 required GameTests.
Its production jar contains the original clip and dependency inventory, but
no GameTest/JUnit classes, unused local combat prototypes or embedded PAL code.
`prepareClientRun` was regenerated successfully for IntelliJ; it is a setup
check, not a rendered client/F5 test. Local reference files and the pre-existing
Qi particle edit were excluded and their code hashes preserved.

CI now requires the server's positive `All ... required tests passed` result,
because a caught mod-loading failure can exit zero without executing GameTests.
Do not equate a zero Gradle exit code with a completed server suite.

Still **not validated**: F5 visual quality, first-person regression, slim/classic
armor alignment, GUI player rendering, real two-client networking, latency/TPS/FPS
behavior and a server-side world-space socket. Headless pose tests do not render
Minecraft meshes and synthetic tracking/lifecycle events are not real network,
respawn or portal sessions. No claim of final combat readiness or zero bugs.

Manual gate before promotion to `main`:

1. Capture front/back/side F5 clips, classic/slim skins, both main arms, with and without armor. Check held-sword alignment, no twisting artifacts and return to walking/idle.
2. Repeat in first person, moving/jumping and from the Profile GUI. Check camera/hands, guard, R charging and normal interactions are unchanged.
3. Two real clients on a dedicated server: owner and observer trigger/repeat; enter tracking range mid-clip; both hands and armor; test latency and low TPS. Confirm no age-zero restart on snapshots or tracker entry.
4. Swap slots/weapons/offhand, attack, block/use items, guard, charge, die/respawn, relog and change dimension while previewing. Verify no residual pose or state.
5. Verify old save data, Qi/cultivation rewards and GUI controls remain unchanged. Explicit user approval is required before merging.

After those gates, the next lot is one sword move with server-authoritative
windup/contact/recovery and one allowed impact, removing vanilla damage only
for that custom route. Styles, four configurable M1s, mob patches and physical
weapon clashes remain later milestones.
