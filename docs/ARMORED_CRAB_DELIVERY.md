# Armored Crab: Local Audit And Test Delivery

Checked 12 October 2026 in the IntelliJ checkout on `test`. Minecraft 1.21.1,
NeoForge 21.1.248, Java 21, Epic Fight 21.17.3.1. No dependency migration.

## Implemented Locally

- `mob/ArmoredCrabEntity.java`: hostile shore creature, 10 health, 3 attack damage,
  movement speed 0.25, follow range 16. No vanilla melee attack goal.
- `mob/MurimEntities.java`: stable `armored_crab` ID, 1.1 x 0.6 collision box,
  engine attributes and on-ground placement rules.
- `integration/epicfight/ArmoredCrabArmature.java`, `ArmoredCrabEpicFight.java`,
  `ArmoredCrabPatch.java`: 28-joint rig, idle/walk/right attack/left attack/hurt/death,
  synchronized animation attacks and chasing behavior. One engine attack goal.
- `client/ArmoredCrabRenderer.java`: client-only engine skinned renderer and texture;
  the empty vanilla model provides the engine's living-renderer hook.
- `item/MurimItems.java`: spawn egg and Murimblock creative tab, English labels.
- `assets/murimblock/animmodels`: one combined mesh/armature resource and six clips.
  Durations: idle 3 s, walk 0.75 s, right attack 1.25 s, left attack 22/24 s,
  hurt 0.5 s, death 1.625 s. Export rounding is checked with tolerance 0.0001 s.
- Right contact window 0.50-0.62 s; left 0.34-0.44 s. Engine handles damage,
  not a second vanilla melee goal. Collider reach is intentionally extended;
  its alignment with the visible claws still needs graphical validation.
- Biome modifier: beach, snowy beach, stony shore, river, swamp, mangrove swamp;
  weight 20, groups 3-5. Natural placement requires water within the local search
  volume and valid ground, rejects Peaceful, and does not require darkness.
- Death: 1-3 XP, base Qi reward 3 under existing reward/anti-farm rules. Loot (14 Oct): a cod one time in two (cooked if
  it burns, Looting adds up to one), and a trident one time in a hundred when a player kills it (Looting: 2, 3, 4 %).

These resources are for the original crab, not the separate infected humanoid
Blockbench experiments. Local model/reference files are not distributed.

## Cleanup

`EpicFightMobSupport.humanoidAttributes` now reuses `creatureAttributes` for shared
engine attributes. Offhand attributes remain humanoid-only; behavior is unchanged.

SHA-256 comparison of `src`, `docs` and `scripts` found no removable duplicate
implementation files. Two empty loot tables are identical but belong to different
entity IDs. The training mob preset and inactive zombie example have identical
content but different roles. They are retained, not deleted merely for equality.
No worlds, caches, local art versions or user reference directories were deleted.

## Checks Actually Run

- `gradlew.bat build compileGameTestJava`: passed, 130 unit tests, zero failures.
  Includes new `ArmoredCrabAssetsTest`: joint/track uniqueness, known joint names,
  ordered finite frame times, 16-element finite matrices, clip durations, texture
  decoding, egg model and English labels. Existing distribution test checks the
  real release JAR and excludes dev harness/Blockbench files.
- `gradlew.bat -PgameTests runGameTestServer`: 34 required tests passed, including
  seven crab checks: rig/goals, rewards/loot/XP, placement, creative egg, right and
  left damage windows with one contact, and AI attack selection.
- `gradlew.bat -PgameTests runVisualSmoke`: passed in a real graphical client.
  Covers the compact GUI pages/settings, interactions and existing training mob.
  This harness does NOT capture the crab; it is not crab visual approval.
- `git diff --check`: passed before staging.

The local menu changes are included: compact 320 x 200 layout, bookmark tabs,
English manuscript text and corresponding layout/screen/capture checks.
Unrelated whitespace change in `QiChargeClientEffects.java` is left local.

## Remaining Validation

- In-game crab mesh/texture, all clips, claw-to-collider alignment and resource reload.
- Natural population over time in each biome/day/night; server tests validate the
  placement predicate, not a statistically observed natural spawn distribution.
- Two clients: spawning, tracking, attack timing, death and dimension transitions.
- Test observer/chunk cleanup on exceptional asynchronous GameTest failures merits
  future hardening; the successful test run is not proof of every failure path.
- Cloud checks were not run from this local Windows task.

Push to `origin/test` is authorized for this delivery. Do not merge `main` until
the user explicitly validates the change. Automated passes are not approval.
