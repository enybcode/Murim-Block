# Sword Guard: First Live Slice

Minecraft 1.21.1 / NeoForge 21.1.248. Only swords change in this delivery.
This is not the finished combat replacement or animated blade collision.

## Controls

- Enable Combat Mode with its configured key, V by default.
- Hold a sword in the main hand and leave the offhand empty.
- Hold the configured Use Item key, right mouse button by default, to guard.
- Release it to stop protection. Opening a screen or losing window focus also
  releases the input. A server-side timeout handles a missing release/refresh.
- Left click attacks normally. An accepted sword attack suspends guard for ten
  ticks; held guard resumes afterwards. Impact and guard-break recovery lock
  sword attacks until their own expiry.
- Right click is reserved for guarding in this eligible Combat Mode state,
  including when aiming at an interactable block. Leave Combat Mode to use it
  normally, or use an offhand item, which disables the sword override entirely.
- Axes, bows, crossbows, tridents, empty hands and other items keep vanilla behavior.

## Current Rules

The basic sword guard covers a 120-degree horizontal frontal cone. It cancels
direct player and mob melee damage, not projectiles, falls, explosions, magic
or shield-bypassing sources. It requires current equipment, a live input lease,
the same dimension and line of sight. Equipment is checked at incoming damage,
not just on the next tick. Holding a sword without guarding grants no protection.

A successful interception costs `max(4, incoming damage * 4)` Qi and prevents
health loss. Insufficient Qi exhausts the remainder, breaks the guard for fourteen
ticks and leaves the hit for vanilla damage/armor processing. The remaining Qi
does not buy a partial block. Releasing/repressing cannot erase guard-break recovery.
An impact uses four ticks of recoil; releasing removes protection but does not
erase that short recovery. Guard slows movement to 35 percent and prevents
sprinting. Guard/recovery and Qi charging are mutually exclusive.

The server accepts only a held/released intent, not client-chosen damage,
positions, Qi cost, targets or action timing. Held input refreshes every ten
client ticks and expires after thirty server ticks. Death, clone, logout,
dimension change, leaving Combat Mode, invalid equipment and server shutdown
clean up temporary state. Guard state is not saved to player data.

An accepted sword hit still uses vanilla timing and damage in this slice.
There is no delayed second hit, automatic mutual-target clash or new combo system.
The local `SwordExchange` approximation is no longer registered or called.
The shared trajectory/contact core is preserved for the later authoritative
attack replacement; it is not running alongside vanilla attacks.

## Weapon Categories And Martial Arts

`WeaponCategory` classifies the item, using sword/axe tags with class fallback,
and separates ranged weapons, tridents and unarmed combat. Classification alone
does not install custom behavior. `CombatProfile` identifies rules by both
weapon category and martial-art ID; material does not define a fighting style.

Only `murimblock:basic_sword` is enabled. Its guard geometry, Qi cost, movement,
impact, break and attack-recovery settings are profile values, not global rules
for every future weapon. Unknown arts and incompatible categories have no profile.
There is no learned-art selection, art progression or persistent active-art field
yet. The next art system must resolve the player's server-owned learned/equipped
art and compatible weapon category to matching actions and animation resources.

Different sword arts can then use different preparation, cuts, stance, guard,
combos and Qi costs while sharing contact/server validation. Those authored
actions are future work, not additional playable styles in this delivery.

## Visuals

The unsaved action attachment synchronizes to the owner and tracking players.
In third person, a temporary vanilla BLOCK arm pose moves the skin's sword arm;
this is not yet a custom full-body martial animation. Observers use the accepted
action, not the owner's private Combat Mode flag. First person renders the vanilla
hand once with a guard tilt and brief impact/break recoil. Sounds and sparse CRIT
particles give feedback. No additional HUD bar or third-party assets are added.

The one access-transformer entry exposes the vanilla first-person hand renderer.
The old attack-strength counter exposure is removed because damage is not deferred.

## Verification

Run unit tests/build:

```powershell
.\gradlew.bat cleanTest test build
```

Run dev-only tests on an isolated headless Minecraft GameTest server:

```powershell
.\gradlew.bat -PcombatGameTests runGameTestServer
```

The tests in `src/gameTest` are only loaded with that Gradle property and are
excluded from the distributable jar. The test world is under `build/game-test-run`.
The suite checks actual health loss, Qi spending, rear attacks, guard exhaustion,
release, movement cleanup, weapon swaps, offhand/mode exclusions, environmental
damage, Qi charging, attack/guard exclusivity and the input timeout.

- [x] Final expanded unit suite and production build: 209 local tests, no failures/errors/skips.
- [x] Dev-only server GameTests: all 14 required scenarios passed.
- [x] Independent clean staged Git tree: 194 tests/build and the same 14 server scenarios passed.
- [x] Production jar excludes dev-only test classes; the isolated jar also excludes unpublished prototype classes.
- [ ] Human F5/first-person and two-client latency validation before `main`.

The isolated export contains exactly the delivery selected for Git, excluding
unrelated Qi particle edits, reference folders and unused local prototype files.
Its smaller test count excludes fifteen old prototype tests. The delivered suite
adds twenty tests to the previously published 174, plus fourteen server scenarios.
GitHub runs both the build and server suite; current status is reported on
[PR #2](https://github.com/enybcode/Murim-Block/pull/2). `main` requires user approval.

The server fixtures explicitly negotiate mock NeoForge channels and wait out
vanilla login invulnerability before checking damage. The test arena is installed
before structures are spawned. These harness corrections avoid interpreting
spawn protection or an unconfigured connection as a successful guard.

Automated server tests do not establish visual quality, client input ordering or
network feel. In Minecraft, check classic/slim skins, armor, both main hands,
guard release, repeated blocks, sprinting, air swings/mining, GUI/focus changes,
death, switching slots, food/shield use, PvP settings and leaving Combat Mode.
Full-body animation, timed parry, swept animated blade contacts and mob AI changes
remain pending. The controls and future profiles rely on the installed NeoForge
API; see its [payload documentation](https://docs.neoforged.net/docs/1.21.1/networking/payload/)
and [GameTest documentation](https://docs.neoforged.net/docs/1.21.1/misc/gametest/).
