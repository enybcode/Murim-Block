# Combat Rework: Implementation And Verification

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21.
This document distinguishes implemented code from planned gameplay and visual checks.

## Current Delivery

Step 1 adds an isolated, shared attack/contact core. It is not connected to
Minecraft input, damage, AI, rendering or networking yet. It changes no controls,
HUD, skins, save data or live combat. An earlier local sword-combat prototype
remains separate and is not part of this delivery.

The intended result is an original Murim/Wuxia combat system with animated
cubic player models, readable attack preparation, directional guard, timed
parry and blade clashes. Third-person animation must move the actual player
model with its skin and armor, not only the held weapon. Observers must see
the same accepted action. Non-boss mobs will be adapted by behavior/model
family, not by applying a player animation to every entity.

## Step 1: Added Code

Package: `com.murimblock.combat.attack`.

| Type | Responsibility |
| --- | --- |
| `AttackTimeline` | Windup, contact and recovery durations; clips update intervals to active contact time. |
| `BladePose` | Hilt and tip positions; interpolation and local-to-world yaw conversion. |
| `BladeTrajectory` | Immutable, ordered baked endpoint keyframes and blade radius. |
| `AttackDefinition` | Namespaced action/animation identities with a timeline and trajectory. |
| `BladeGeometry` | Instantaneous blade capsule contact using Minecraft's existing JOML dependency. |
| `AttackContactResolver` | Deterministic ordering and single consumption of already validated contacts. |

No new runtime dependency is installed. Segment distance uses
[JOML Intersectiond](https://joml-ci.github.io/JOML/apidocs/org/joml/Intersectiond.html),
rather than a second geometry implementation.

The closest points are used to compute squared separation explicitly: bundled
JOML 1.10.5 returns a dot product instead of squared separation when both
segments degenerate into points. Regression tests cover this at the origin
and near the Minecraft world border.

### Timing And Authoring Contract

- Durations and keyframe ages are server ticks; fractional values are permitted
  for sampled motion. Phase ranges have an inclusive start and exclusive end.
- Contact lasts at least one tick. Windup and recovery may be empty.
- Trajectories start at age zero and cover the entire attack timeline. Keys are
  strictly ordered and copied. Sampling outside the range clamps to an endpoint;
  the runtime must check the phase before requesting an impact.
- Geometry uses blocks, not pixels. Local +Y is up and +Z is forward. At yaw zero,
  local +X matches world +X. Minecraft yaw +90 faces world -X.
- Linear endpoint interpolation is baked motion, not a rigid sword rotation or
  skeletal animation. A future exporter must bake enough keys from the original
  authored movement and explicitly convert Blockbench axes, scale and pivots.
- `AttackDefinition.animation` is an identity, not proof that an animation file
  exists or has loaded. Playback and resource validation belong to the client adapter.

### Contact Resolution Contract

Each attack has an entity UUID and a nonnegative action sequence. This works
for both players and mobs without giving either a separate damage policy.

Candidates use a common absolute server clock, including fractional tick times;
they must not be sorted using unrelated per-actor animation ages. The future
runtime owns eligibility, reach, walls, contact generation and authoritative
validation. A client cannot submit an arbitrary accepted body hit or clash.

1. Resolve contacts chronologically, regardless of input list order.
2. At exactly equal times, obstruction takes precedence over blade clash,
   which takes precedence over body hit.
3. The first accepted impact consumes the strike. A clash consumes both strikes.
4. A late clash cannot undo a body hit or stop an opponent's still-valid strike.
5. Consuming two clashing strikes gives neither victim immunity to a third attacker.
6. Retain consumed/cancelled IDs across runtime updates, then retire them when
   their actions end. Do not accumulate an unbounded global history.

Canonical identities break otherwise indistinguishable same-time ties, including
three blades meeting. This is deterministic, not a multi-body physics simulation.
The first slice has one impact per strike, not area damage or multi-target cuts.
An explicit policy and tests are required before adding those features.

### Deliberate Limits

`BladeGeometry` tests capsules at one instant. It does not sweep a moving blade
between ticks. The runtime still needs bounded swept contact generation,
body volumes, block collision checks and contact-time calculation. A skipped
timeline phase is preserved by interval clipping, but this alone cannot detect
a blade that passed through something between two samples.

The resolver accepts fixtures as well as real contacts: supplying a `BodyHit`
does not establish that a target was actually reached. No damage is applied
and no guard, parry, recoil or AI decision is performed by these classes.

## Verification Log

- [x] Inspect the existing attack, damage and rendering hooks and preserve local edits.
- [x] Add the shared action, motion and contact contracts without installing gameplay hooks.
- [x] Add focused unit tests and a pure-core composition test.
- [x] Run the expanded test suite and build after this change.
- [x] Verify an exported clean Git snapshot, independently of the unfinished local prototype.
- [ ] Publish the completed lot on `test` and open a review toward `main`.
- [ ] Validate animation, gameplay and multiplayer in Minecraft after their adapters exist.

Tests cover half-open phases, skipped intervals, invalid data, immutable inputs,
trajectory interpolation, Minecraft yaw, finite blade endpoints, grazing/missing
capsules, world-border coordinates, wall/clash/body ordering, duplicate prevention,
cross-update consumption, cancellation and deterministic ties.

The composition test feeds sampled geometry into the resolver: intersecting
active blades clash, separated simultaneous blades do not, and inactive phases
produce no candidate. Its body contacts are fixtures. It is not a GameTest,
network test or proof of visually correct in-game weapon contact.

Local IntelliJ verification: `gradlew.bat cleanTest test build` succeeded with
165 tests, zero failures/errors/skips, including 36 new core tests. This local
count also includes tests from the unfinished prototype.

Isolated verification: export the committed baseline and copy only this lot's
source/tests into it, then run `gradlew.bat test build`. All 143 tests passed
with zero failures/errors/skips. All 11 new source/test files were SHA-256
checked against the IntelliJ checkout. The two compile warnings concern an
existing deprecated NeoForge event-bus annotation, not this change.

## Next Steps

| Step | Implementation | Acceptance gate | Status |
| --- | --- | --- | --- |
| 1. Shared core | Timeline, baked blade motion and contact resolution. | Unit tests, build and clean snapshot. | Passed; no live gameplay integration yet. |
| 2. Player animation slice | Pin compatible Player Animation Library; author original stance, cut, guard and clash recoil; connect full player rig. | Actual F5/first-person/observer playback, matching debug blade, no duplicate rendering. | Not started. |
| 3. Armed zombie slice | Server actions, swept blade/body/block contacts, controlled damage bridge and zombie AI/model adapter. | Player/zombie and player/player exchanges, one damage owner, visible preparation and recoil. | Not started. |
| 4. Defense/network | Directional block, timed parry, guard break, action synchronization and interruption. | Server/client agreement, lifecycle and latency checks. | Not started. |
| 5. Mob families | Armed, unarmed, ranged and special-attack adapters with an entity audit. | Each entity's behavior, model, equipment, drops and exclusions checked. | Not started. |
| 6. Cleanup/release | Remove superseded paths and finish regression/performance checks. | Existing worlds, dedicated server, multiplayer and user approval. | Not started. |

Player Animation Library is a candidate, not an installed dependency. Verify
the pinned Minecraft 1.21.1 artifact and its own API before using current
[PAL documentation](https://docs.zigythebird.com/pal/). Author original animations;
do not copy Epic Fight or Better Combat assets without permission.

### Replacing Existing Combat

There must be one owner per managed attack. When the new slice is connected,
replace the local prototype's fixed-delay/mutual-target `SwordExchange` route;
do not run it alongside the trajectory resolver. Suppress the corresponding
vanilla attack/swing execution for managed actions only.

For the first mob, preserve navigation and target selection but replace its
melee execution/cooldown with server-requested actions. Prevent an independent
vanilla `doHurtTarget` from firing while the custom strike owns that exchange.
Unsupported actors keep vanilla behavior during rollout.

At a validated body contact, verify armor, enchantments, critical conditions,
knockback, weapon durability, statistics, kill credit and hooks. Avoid blindly
calling vanilla `Player.attack`, which can add an unintended second area sweep.

Retire supported manual arm/hand render paths when animation playback replaces
them. Remove access-transformer entries only when their last actual use is gone.
Do not delete the entire combat system before the replacement slice passes.

Wither and Ender Dragon are excluded. Leave Warden and Elder Guardian AI and
animations unchanged until their treatment is explicitly decided. Passive mobs
do not automatically acquire a sword guard. Projectile, magic, explosion and
environmental damage require their own policies rather than universal blocking.

### Visual And Gameplay Check Loop

1. Inspect editable original animation sources and key poses at normal/slow speed.
2. Run a well-lit Minecraft arena with debug weapon/contact volumes.
3. Observe consecutive motion in first person, F5 and another player's view.
4. Match the visible blade to contact timing; check hand attachment, clipping,
   windup, guard, recoil and recovery without effects hiding the motion.
5. Repeat with classic/slim skins, armor, both main hands, movement, crouching,
   jumping, different FOVs and window sizes.
6. Exercise weapon changes, item use, mining, combat toggle, Qi charging, death,
   logout, dimension changes and reload to catch stale actions/poses.
7. Verify a dedicated server and two clients with controlled latency, then profile
   multiple nearby fighters. Correct, rebuild and observe again.

A screenshot checks a pose, not an entire transition. A passing build checks
compilation, not visual quality. Keep each unobserved check explicitly pending.

## GitHub Workflow

Commit only this completed lot on `test`; preserve unfinished prototype edits,
the user's Qi effects edit and local reference folders. Push and open/update
a pull request toward `main`. Automated success does not authorize merging:
`main` changes only after explicit user validation.
