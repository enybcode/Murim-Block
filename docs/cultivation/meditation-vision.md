# Meditation V3: closed eyes and spiritual vision (design proposal)

Status: validated by Enzo on 30 Oct 2026 (playable mockup `meditation-vision-demo.html`, artifact
9CuKAErzZ7vkfNFoStRdgU) and in the game. Numbers are tunable placeholders. It replaced the breathing QTE.

## Decisions taken (Enzo, 30 Oct)

- The breathing mini-game is removed.
- The Qi gain multiplier now comes from **Calm**: it rises the longer the player meditates undisturbed.
- While cultivating the player closes their eyes. A mortal sees only Qi motes; higher realms see blocks as blue
  holographic outlines and mobs as stars.
- The speedster time slow (Wally West) is **not** part of meditation: it belongs to the combat redo, later.

## What stays

Key C, the Shenfa lotus pose, the end conditions (moving, attacking, taking damage, Sneak), "no hostile within
8 blocks" to start, Qi smoke (`QiAura`), the stage breakthrough, the Advance button, Meridian pills, the inner world.

## What went (30 Oct)

`BreathTrack`, `BreathingHud`, `BreathResult`, payload `Breath`, Harmony and combo, the clips `lotus_still`,
`breath` and `deep` (one's own body now breathes on the 4 s loop like everyone's), their GUI shots and tests. The
vignette texture stays: the trial's dive uses it.

## Calm (server)

- `calm` 0-1, lives only while meditating, reset to 0 when meditation ends.
- Grows by 1/120 per second (full after 2 min) **while the eyes are closed**. Eyes open: frozen.
- A hostile mob within 16 blocks: Calm stops growing and drops by 1/30 per second. The vision is how the player
  sees it coming.
- Gain = base × (1 + 2 × calm), so ×1 to ×3 (the QTE gave up to ×5: gauge sizes may need retuning).
- Tiers keep their names: Steady Breath (calm ≥ 0.25), Flowing Qi (≥ 0.5), Still Heart (= 1). Reaching a tier plays
  one soft chime and briefly shows its name.
- HUD: one discreet ring near the crosshair filling with Calm, tinted in the tier colour. Nothing else.

## Closed eyes (client)

- The eyes close by themselves when the sit animation ends (fade to black over 1 s); while seated the meditation key
  (C, placeholder) opens and closes them (V is the lock-on). They reopen during the stage breakthrough cinematic and
  the trial's dive, and close again after.
- The black is drawn over the world, under the HUD (Calm ring, chat, System messages stay readable).
- Qi motes drift around in the dark, denser with the Qi held (same log scale as `QiAura`). The own Qi smoke is
  under the dark; others see it as before.
- Inside the inner world: same rules.

## Spiritual vision by realm

| Realm | Blocks | Mobs and players |
|---|---|---|
| Mortal | nothing, only Qi motes | nothing |
| Third-Rate Warrior | outlines within 6 blocks, faint, flicker | stars within 12 blocks |
| Second-Rate Warrior | 10 blocks, steady | 20 blocks |
| First-Rate Warrior | 16 blocks, sharper edges | 32 blocks, players' Qi shown as a halo |
| Peak Master | 24 blocks | 48 blocks |
| Transcendent | 32 blocks, nearly solid | 64 blocks, sees through Qi concealment (later) |

- **Blocks:** blue holographic outlines on the edges of faces exposed to air only, not every block edge. They pulse
  slowly on the 4 s breathing loop. The mesh is rebuilt when the player meditates or a block changes nearby, never
  every frame.
- **Stars:** one billboard per living entity.
  - size from the bounding box (chicken small, Iron Golem big),
  - colour from hostility: white to blue for passive, yellow for neutral or hostile but not targeting, red for a
    mob targeting the player (`Enemy` + current target),
  - twinkle speed from strength: max health and attack damage, log scale.
- **Fast things:** projectiles and very fast entities leave only a smeared afterimage (Photon trail). Later, speed
  techniques used near a meditator show the same way.
- **PvP:** the vision goes through walls, so it costs nothing only while seated in lotus. If it ever becomes usable
  standing, it must cost Qi per second.

## In the code (30 Oct)

- Server: `Cultivation` keeps `calm`, `eyesClosed` and `disturbed` while meditating (reset at start and stop);
  `CultivationRules.calmAfter`, `calmTier`, `meditationGain(calm)`; `Meditation` checks hostiles within 16 blocks
  each second. Payload `Eyes(boolean)` (client to server), network protocol `4`; the snapshot carries the Calm.
- `client/cultivation/vision/ClosedEyes`: own eyes, auto-close after 24 ticks, fade, hand and crosshair hidden.
  `autoClose = false` keeps them open for dev tours that film the world (`InnerWorldTour`, `MeditationChecks`).
- `VisionMesh`: exposed faces and their edges in a sphere of the block reach (at most 20 blocks up and down),
  scanned a slice a tick (8 for the first mesh), rescanned every 2 s or when the eye moves. Blocks with a collision
  shape and liquids count; air and plants do not. Outlines are whole cubes (a slab shows as a full block).
- `SpiritVision` (`RenderLevelStageEvent.AFTER_WEATHER`: `AFTER_LEVEL` comes after the camera rotation is popped):
  the dark veil, the outlines in two line passes (soft wide, thin bright), faint faces from Peak Master, stars as
  camera-facing quads, afterimages of anything moving more than 0.45 block a tick and of projectiles.
  `VisionRenderTypes`: no depth test, additive. `VisionRules`: the numbers, unit tested (`VisionRulesTest`).
- Star temper on the client: players are cultivators; a `Mob.isAggressive()` mob is targeting; `Enemy` hostile;
  `NeutralMob` (wolf, Iron Golem…) neutral; the rest passive.
- `client/cultivation/MeditationHud`: Qi motes in the dark (count from `QiAura.power`), the Calm ring with a red dot
  while a hostile is within 16 blocks, the tier banner and chime, the stage gauge, the key prompt.
- Check: `gradlew runGuiShots` (`murim_meditation`, `murim_calm_still_heart`, the vision with a cow, a wolf and an
  Iron Golem in each F5 camera: `murim_vision_first_rate` (front), `murim_vision_first_person`, `murim_vision_back`;
  then `murim_meditation_full` and the pose shots with open eyes). Stars are capped at 5 % of their distance to the
  camera, so a mob beside the F5 camera does not blind the view.
