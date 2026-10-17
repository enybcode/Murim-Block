# Blood Dao — ninja combat test, 1.10.1

The user requested a modest increase in reaction and attack speed plus a 15% heal on a
successful Blood Cloud Step Slash. Existing body, clothing, dao mesh and damage are retained.
The scope remains ninja-only, without drops or player techniques.

## Reaction and tempo

Empty-target checks no longer consume the vanilla melee goal's 20-tick path-check cooldown.
Acquisition uses the existing nearest-player goal with a slightly more frequent scan (interval
3 instead of 5). Navigation speed, guard radius, creative/spectator exclusions and pursuit rules
are unchanged. Existing saved ninjas use the new timings without respawning.

| Action | Previous | Current |
| --- | --- | --- |
| M1 preparation | 6 ticks / 0.30 s | 5 ticks / 0.25 s |
| M1 impact | 9 ticks / 0.45 s | 8 ticks / 0.40 s |
| M1 animation ends | 22 ticks / 1.10 s | 19 ticks / 0.95 s |
| M1 minimum interval | 26 ticks / 1.30 s | 23 ticks / 1.15 s |
| Cloud Step preparation | 12 ticks / 0.60 s | 10 ticks / 0.50 s |
| Cloud Step animation ends | 32 ticks / 1.60 s | 28 ticks / 1.40 s |
| Cloud Step minimum interval | 120 ticks / 6.00 s | 110 ticks / 5.50 s |

The same authored poses are retimed. Hand, blade, ribbon, pulse and scarf use the corresponding
new phases. Cloud Step still covers five blocks over four movement ticks. Each controller retains
its own cooldown and cannot interrupt an active attack. Cloud Step takes priority when ready;
M1 fills its cooldown against a nearby enemy.

## Blood Cloud Step Slash

- 6 damage per distinct enemy; maximum four successfully injured enemies per dash.
- First confirmed health loss restores 15% of the ninja's maximum health, once per cast.
- With the normal 24 max health this restores 3.6 HP (1.8 hearts); at 40 max health it restores 6 HP.
- Healing is bounded by maximum health. Extra targets never grant extra healing, even if the first
  heal was capped because the ninja was nearly full. Missing, blocking or immunity grants no heal.
- A fixed telegraphed line permits sidestepping. Quarter-block checks stop at walls, ledges,
  liquids, unloaded chunks and the 28-block guard boundary. Allied Blood Ninjas are excluded.
- Black afterimages, dark-red ribbon and converging blood motes mark the cut and successful healing.

## Basic diagonal cut

- Existing four-point attack attribute, one damage attempt, no movement burst and no healing.
- At impact the selected target must remain hostile, visible, within 2.5 blocks and within the
  committed 110-degree forward arc. Disarming cancels the action.
- Shoulder-led diagonal cut, articulated elbow, quaternion wrist and tassel follow-through.
- Dark-red blade ribbon; 15 falling, non-emissive droplets only after confirmed health loss.

## Validation and implementation

BloodStepAttack owns dash movement and the per-cast healing flag. BloodDaoM1Attack owns melee
impact validation. BloodNinja owns targeting and synced kind/start/facing. DaoAttackMotion and
DaoM1Motion supply the retimed poses; ReferenceNinjaModel and AnimatedBloodDaoLayer share the hand
transforms. BloodStepEffects and BloodDropletParticle supply the existing visual effects.

GameTests cover timing, percentage scaling, multi-target healing cap, overheal, dodge, walls,
immunity, disarm, cooldown coexistence, idle-target reaction, save/load, patrol and unchanged drops.
The optional graphical tours run in separate disposable development worlds. From the repository root:
`./gradlew -PgameTests runBloodCloudStepTour` and
`./gradlew -PgameTests runBloodDaoM1Tour` (use `.\gradlew.bat` on Windows).
GameTests and graphical tour classes must never be packaged in the player JAR.

Minecraft 1.21.1, NeoForge 21.1.248 and Java 21 are unchanged. The standalone test addon does not
modify Azure Cloud Sword or Long Fist. The dao model author credit remains in the original asset.
