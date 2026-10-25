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
| `stageQi` | Qi gathered toward the next stage |
| `qi` / `maxQi` | Qi spent by techniques later; regenerates slowly, faster while meditating |
| `meridians` | number opened, always in the Qi-flow order (0-13) |

Saved IDs stay stable once released. Commands (operators only) set each field for tests.

## Stage gauge

- Gauge to fill: `100 × 1.25^(stage-1) × 3^realm` (Body Tempering 1 = 100, stage 9 ≈ 600; Qi Condensation 1 = 300).
- Full gauge: the Cultivation page shows **Advance** (enabled only while meditating). Pressing it: stage + 1, gauge
  reset, `maxQi` + 10 % (realm bonus to decide), a [!] System message.
- Stage breakthrough (`client/cultivation/StageBreakthrough`, mockup "Spiral Breakthrough" chosen by Enzo, 25 Oct,
  artifact 6x4Qok5t6ZRtcKrXVinTpz): the System closes and, in first person, the camera steps behind the player for
  3.6 s. Qi specks gather in a ring at the feet (0-0.8 s), two strands spiral up to above the head with a rising
  beacon sound (0.6-1.8 s), the seated body lifts a little like a deep breath, then the spiral bursts into falling
  specks (`qi_fall`, one in eight gold) and a ring of Qi smoke, with a chime one pentatonic step higher per stage
  (note-block chime and bell, amethyst shimmer). A **BREAKTHROUGH** System window slides down for 2.5 s: realm,
  stage, Max Qi bonus, next gauge (stage 9: "the trial awaits"). The breathing lane pauses meanwhile. Everyone
  tracking the player sees and hears it (payload `stage_breakthrough`); the window and camera are the player's own.
  Checked by `runInnerWorldTour` (shots `murim_breakthrough_spiral`, `_burst`, `_window`).
- Stage 9 full: the button reads **Trial required** (disabled) and lists the missing meridians
  (`MERIDIANS_FOR_TRIAL`). The mortal realm's trial: [mortal-trial.md](mortal-trial.md); the realms above: to do.

## Meditation

- Key (placeholder C): sit in lotus (PAL animation), camera free, player rooted. Moving, attacking or taking damage
  ends it. Not usable in the air, in water or with a hostile mob closer than 8 blocks.
- Base gain: 2 stage Qi per second, and `qi` refills 5× faster.
- Breathing QTE (V2, validated playable mockup "Breathing V2", artifact Kanzz5Vni8Jhd3CDPrxaNm, 24 Oct): marks
  scroll left into a frame. ▲ inhale and ▼ exhale: press the key as the mark enters the frame. Bars are holds: keep
  the key down from the bar's start, release at its end. Beats are short / normal / long (1.0 / 1.6 / 2.6 s), up to
  25 % shorter at full Harmony, in random patterns (calm, box breathing, quick, deep, sip, stagger; the hard ones
  need some Harmony).
- Judging: perfect ±0.07 s (+6 % Harmony), good ±0.16 s (+4 %), miss -8 % and the combo ends. Combo tiers at 5, 10
  and 20 breaths: Steady Breath x1.2, Flowing Qi x1.5, Still Heart x2. Gain = base × (1 + 1.5 × Harmony) × tier,
  so up to × 5. No penalty beyond that in this slice (Qi deviation could come with the trials).

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
- Attachment `murimblock:cultivation`, saved and kept on death. Network protocol `2`: sync, pose, toggle, advance, breath.
- Key C (placeholder) starts meditation; seated cross-legged, knees bent out, shins crossed, hands on the knees
  (Enzo's reference photo, 23 Oct). Legs have no knee in Minecraft or PAL: `MeditationLegsLayer` hides them and draws
  a thigh and a shin per leg from the skin. Rest of the pose
  in `player_animations/meditate_lotus.json`, played through PAL and
  sent to everyone tracking the player. The Cultivation page has Advance and Stop/Meditate; Profile reads the real Qi.
- Breathing QTE: `client/cultivation/BreathTrack` (patterns and judging, plain Java, unit tested) and
  `BreathingHud` (lane, frame, Combo and Harmony boxes, effects). Presses come from the input events, so they are
  timed to the frame. The client judges and sends `Breath(BreathResult)`; the server accepts one per 8 ticks and keeps
  Harmony and its own combo for the gain (`CultivationRules.meditationGain(harmony, combo)`). Sneak stands up.
- Effects: hits flash the frame, send a rectangular wave out (perfect: two, gold, plus sparks) and climb a
  pentatonic bell; misses shake the lane, redden the screen edges and thud (note block bass). Harmony tints the
  edges in the tier colour (`textures/gui/breath_vignette.png`, drawn by `scripts/breath-vignette.cjs`), and above
  50 % Qi motes drift to the frame. The world brightens a little on the inhale. Sounds are vanilla sound events.
- Qi smoke `client/cultivation/QiAura` + `QiParticle`, `QiSparkParticle` (types `murimblock:qi`, `qi_ring`,
  `qi_spark`, options `QiParticleOptions` = power 0-1): cyan puffs and near-white twinkling specks leave the body of
  every meditating player, rise and fade (puffs swell and fray); a good breath blows a flat ring of them out.
  Everything scales with the Qi held, log scale: power = ln(1+qi/100) / ln(101), so 100 Qi ≈ 0.15 (a puff every
  ~4 ticks, small and faint), 1000 ≈ 0.52, 10 000 = 1 (1.5 puffs + 1 speck a tick, ring of 20). Watchers learn the
  Qi from `Pose` and `MeditatorQi` (every second). `gradlew runGuiShots` adds `murim_meditation_pose_strong`
  (10 000 Qi). Original textures: `textures/particle/qi_smoke_0..5.png` (16x16, thinning frames, Java noise
  script) and `qi_spark_0..2.png` (8x8).
- Commands: `/murim cultivation get|fill <player>`, `/murim cultivation set <player> <realm> <stage> <meridians>`,
  `/murim cultivation qi <player> <amount>` (Qi and max Qi), `/murim prepare` (test setup: all meridians, full
  gauge, 10 000 Qi, clear day, hostiles within 32 blocks removed).
- Pill icon drawn by `scripts/pill-icon.cjs`. Meridian bonuses are shown but not applied yet.
- Check: `gradlew runGuiShots` also seats the player in lotus on a platform and shoots the HUD states
  (`murim_meditation`, `murim_breath_good`, `murim_breath_miss`, `murim_breath_full`).
