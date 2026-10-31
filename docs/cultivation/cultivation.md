# Cultivation (design, first slice)

Status: validated by Enzo on 23 Oct 2026; the foundation is in the game (below). Numbers are tunable placeholders.

## Decisions taken (Enzo, 23 Oct)

- First slice = the Qi foundation: saved server data, sync, meditation, stage-up; the Cultivation page shows real
  values. The breakthrough trial between realms comes in the next slice.
- Stages 1-9 of a realm: when the stage gauge is full, the player presses a button to rise one stage.
- Meridians open with items (pills), not automatically.
- Meditation: the player sits in lotus and plays a breathing mini-game that speeds up the gain.

## Player data (server, saved attachment, synced to its owner)

| Field | Meaning |
|---|---|
| `realm` | index in the realm ladder (0 = Mortal / Body Tempering) |
| `stage` | 1-9 inside the realm |
| `maxQi` | the progress: grows with meditation, step by step, up to the stage's peak |
| `stageQi` | derived: max Qi past the stage's floor (saved as `stage_qi` for old saves) |
| `qi` | Qi spent by dodges, guards and techniques; refills as a share of max Qi |
| `meridians` | number opened, always in the Qi-flow order (0-13) |

Saved IDs stay stable once released. Commands (operators only) set each field for tests.

## Max Qi and its peaks

Since 31 Oct (Enzo): cultivating is growing the reservoir. Meditation raises max Qi little by little; each stage has
a **peak** where it stops growing until the player advances. There is no stage gauge apart from max Qi, and nothing
resets: max Qi only goes on from where it is. Qi, the fuel, is spent in fights and refills on its own.

- A stage adds `100 × 1.25^(stage-1) × 3^realm` max Qi (Body Tempering 1 = 100, stage 9 ≈ 600; Qi Condensation 1
  = 300). Start 100; peaks: 200, 325, 481… Body Tempering 9 peaks at ≈ 2680, the floor of Qi Condensation 1.
- Peak reached: the Cultivation page shows **Advance** (enabled only while meditating). Pressing it: stage + 1, max
  Qi unchanged, growing again toward the next peak, a [!] System message.
- Meant to be very long (Enzo, 31 Oct): 0.02 max Qi a second, × 3 at full Calm. Body Tempering 1 ≈ 30 min of sitting,
  stage 9 ≈ 2 h 45, the whole realm ≈ 12-13 h; each realm three times the one before (Qi Condensation ≈ 36 h).
  Killing and remedies will be other ways to gain max Qi (to design).
- Qi refills 0.5 % of max Qi a second, 2.5 % while meditating (empty to full in 200 s, 40 s seated).
- Saves from before 31 Oct come back with their old gauge turned into max Qi (floor + stage Qi).

## Kills and death (`KillQi`, from V1 on Enzo's request, 31 Oct)

- A kill gives the mob's Qi (refills Qi) and 5 % of it as max Qi, which stops at the stage's peak like meditation's.
  Qi by kind, V1's table: zombie 4, skeleton 6, creeper and witch 18, enderman 15, iron golem 45, Warden 400,
  Wither 300, dragon 500; any other mob 1; the mod's own entities 0; nothing in the inner world.
- Farming: over 10 kills of one kind within 15 min the reward is 75 %, over 20 it is half (in memory only).
- A boss's first defeat adds max Qi once (saved list `defeated`): Elder Guardian 100, Warden 250, Wither 400, dragon 600.
- The player's death costs 10 % of max Qi for good, never below the stage's floor.
- Feedback in the action bar (`+4 Qi · +0.2 Max Qi`); the death's loss in red in the chat.
- Stage breakthrough (`client/cultivation/StageBreakthrough`, mockup "Spiral Breakthrough" chosen by Enzo, 25 Oct,
  artifact 6x4Qok5t6ZRtcKrXVinTpz): the System closes and, in first person, the camera steps behind the player for
  3.6 s. Qi specks gather in a ring at the feet (0-0.8 s), two strands spiral up to above the head with a rising
  beacon sound (0.6-1.8 s), the seated body lifts a little like a deep breath, then the spiral bursts into falling
  specks (`qi_fall`, one in eight gold) and a ring of Qi smoke, with a chime one pentatonic step higher per stage
  (note-block chime and bell, amethyst shimmer). A **BREAKTHROUGH** System window slides down for 2.5 s: realm,
  stage, max Qi, next peak (stage 9: "the trial awaits"). The breathing lane pauses meanwhile. Everyone
  tracking the player sees and hears it (payload `stage_breakthrough`); the window and camera are the player's own.
  Checked by `runInnerWorldTour` (shots `murim_breakthrough_spiral`, `_burst`, `_window`).
- Stage 9 at its peak: the button reads **Trial required** (disabled) and lists the missing meridians
  (`MERIDIANS_FOR_TRIAL`). The mortal realm's trial: [mortal-trial.md](mortal-trial.md); the realms above: to do.

## Meditation

- Key (placeholder C): sit in lotus (Shenfa, see below), camera free, player rooted. Moving, attacking or taking damage
  ends it. Not usable in the air, in water or with a hostile mob closer than 8 blocks.
- Base gain: 0.02 max Qi per second, and `qi` refills 5× faster.
- Since 30 Oct (V3, validated by Enzo): no mini-game. The eyes close once seated and the Calm multiplies the gain,
  up to × 3; closed eyes show the spiritual vision. Rules and code: [meditation-vision.md](meditation-vision.md).
  The breathing QTE (V2, 24 Oct) is gone.

## Meridians

- One item, **Meridian-Opening Pill**, opens the next meridian in order when eaten while meditating; it fails
  (pill kept) if the realm is too low for that meridian. Per-meridian pills are possible later if Enzo prefers.
- Each open meridian gives its Qi bonus (texts already in `en_us.json`, values to set).
- Source of the pill: dev command now; alchemy, loot or merchants later (to decide).

## Not in this slice

Breakthrough trial, Qi from combat, pills that add Qi, Qi costs of techniques, realm names beyond Core Formation.

## In the code (23 Oct)

- `cultivation/`: `CultivationRules` (numbers), `Cultivation` (player data, plain Java, unit tested), `CultivationCodecs`
  (save + network), `Meditation` (server rules), `CultivationEvents`, `CultivationCommands`, `MeridianPillItem`.
- Attachment `murimblock:cultivation`, saved and kept on death. Network protocol `4`: sync, pose, toggle, advance, eyes.
- Key C (placeholder) starts meditation; seated cross-legged, knees bent out, shins crossed, hands on the knees
  (Enzo's reference photo, 23 Oct). Drawn by Shenfa since 29 Oct (storyboards `docs/mobs/boards/meditation.png`
  and `meditation_fp.png`): `client/cultivation/LotusBody` plays `skeletal/animations/meditation.gltf` (sit 24 ticks,
  lotus, breakthrough 72, stand 16, 1.5x after a hit) for every player the server says meditates; the lotus breathes
  on a 4 s loop. While seated (`PlayerAnimators.bodyHeld`) the feet are not planted, vanilla arm work and held items are
  left out, and the first-person eye follows the head down to about 0.95 block. A breakthrough lifts the body by its
  clip; the third-person camera glides back over 12 ticks and in again. With Shenfa players off, no lotus is drawn.
  Clips in `tools/blender/murim_clips.py` (`SETS["meditation"]`); check: `runEngineTest -PengineGroups=meditation`. The Cultivation page has Advance and Stop/Meditate; Profile reads the real Qi.
- Closed eyes, Calm and vision: see [meditation-vision.md](meditation-vision.md#in-the-code-30-oct).
- Qi smoke `client/cultivation/QiAura` + `QiParticle`, `QiSparkParticle` (types `murimblock:qi`, `qi_ring`,
  `qi_spark`, options `QiParticleOptions` = power 0-1): cyan puffs and near-white twinkling specks leave the body of
  every meditating player, rise and fade (puffs swell and fray); a stage breakthrough blows a flat ring of them out.
  Everything scales with the Qi held, log scale: power = ln(1+qi/100) / ln(101), so 100 Qi ≈ 0.15 (a puff every
  ~4 ticks, small and faint), 1000 ≈ 0.52, 10 000 = 1 (1.5 puffs + 1 speck a tick, ring of 20). Watchers learn the
  Qi from `Pose` and `MeditatorQi` (every second). `gradlew runGuiShots` adds `murim_meditation_pose_strong`
  (10 000 Qi). Original textures: `textures/particle/qi_smoke_0..5.png` (16x16, thinning frames, Java noise
  script) and `qi_spark_0..2.png` (8x8).
- Commands: `/murim cultivation get|fill <player>`, `/murim cultivation set <player> <realm> <stage> <meridians>`,
  `/murim cultivation qi <player> <amount>` (Qi and max Qi), `/murim prepare` (test setup: all meridians, full
  gauge, 10 000 Qi, clear day, hostiles within 32 blocks removed).
- Pill icon drawn by `tools/textures/pill-icon.cjs`. Meridian bonuses are shown but not applied yet.
- Check: `gradlew runGuiShots` also seats the player in lotus on a platform and shoots the HUD states
  (`murim_meditation`, `murim_calm_still_heart`, `murim_vision_first_rate`, `murim_meditation_full`).
