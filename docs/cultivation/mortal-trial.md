# Mortal trial (The Rising Dead)

Status: in the game since 25 Oct 2026. Mockup "Graveyard Inner World" validated by Enzo the same day
(artifact 4cL5qNBhdTUcZqgc42rZNn): the trial out of the mortal realm is a graveyard island at night and waves of
zombies, no NPC. The Heart Demon stays the idea for the realms above.

## Decisions (Enzo, 25 Oct)

- BEGIN TRIAL opens the graveyard island; 5 waves of zombies. Changed the same day: one wave of 15 in all, arriving
  little by little, the Grave Warden as the 15th; it must be possible for a mortal.
- Victory: Third-Rate Warrior (Qi Condensation stage 1). Defeat: 30 % of the Qi lost.

## How it works

- Ready: Body Tempering stage 9, full gauge, the realm's meridians open (4), meditating. Then System (K) >
  Cultivation > **BEGIN TRIAL** (the card reads "The Rising Dead"). The System closes, the body stays seated as for
  the inner world, the spirit arrives south of the arena facing north.
- Entry (Enzo, 25 Oct: dive no. 1 of four proposals, plus getting up and a title; `client/innerworld/TrialEntry`):
  - Dive, 3 s, still seated: Qi specks are drawn in from all around to the dantian (seen by those near, payload
    `trial_dive`); for the player the screen darkens from the edges to black over a slowing Warden heartbeat, the
    breathing lane hidden. Standing up or being struck cancels it, at no cost.
  - Arrival: a low bell, the screen opens on the graveyard; seen from behind, the player lies face down and gets up
    (seen three quarters from the side: the body turns 55° from the camera; Shenfa clip `trial/rise`, 3.2 s, from the
    storyboard `docs/mobs/boards/epreuve.png` validated on 30 Oct, after the physiotherapists' way up from the floor:
    lying face down while the screen opens, pushing up onto hands and knees, one foot forward, upright on one knee,
    driving up through the front leg with earth thrown up, a look around), then back to first person and the
    title **SURVIVE DEATH** (dark red) with "Mortal Trial · fifteen of the dead". The dead rise 7 s after arriving.
- The wave: 15 dead in all. Two climb out of the graves when it starts (7 s after arriving, the arrival plays
  meanwhile), then one every 3 s while fewer than 4 stand, always from graves away from the player, earth thrown up.
  Since 30 Oct the dead are drawn on Shenfa as Disciples déchus and fight as they do (`DiscipleFighting`). Each
  climbs out of its grave (clip `disciple/climb`, 40 ticks: a hand through the earth, the grip on the edge, the haul,
  a knee, up and hunched), held still meanwhile (`TrialZombie.emerging`).
  The 15th is the **Grave Warden** (1.6x size, 60 health, 8 damage; doubled on 29 Oct at Enzo's request), out of the mausoleum once the 14 others are down.
  It looks like a zombie gone wrong (Enzo, 25 Oct: "a bit mutant, do not change too much"). On Shenfa since 30 Oct
  (design sheet `docs/mobs/gardien-tombes.png`; `tools/blender/mobs/warden.py`, `skeletal/mobs/warden.gltf`): grey-green
  rotting skin with sores, red eyes glowing in sunken sockets, a torn mouth and swollen cheek, bare ribs through a
  torn shirt, a hunch and three bone spikes down the spine, a bone knot and spur on the right shoulder, a swollen
  right forearm, claws, bare clawed feet under ragged trousers. It ducks out of the mausoleum, takes a heavy step,
  unfolds and roars (clip `disciple/warden_entrance`, 60 ticks), then plays the Disciple's clips and blows at 0.85
  of their pace (`TrialZombie.WARDEN_PACE`): its step, its roar and each blow shake the camera of a player within
  14 blocks and throw up earth. With Shenfa mobs off, the old look is drawn (`TrialZombieRenderer`, texture
  `tools/textures/grave-warden.cjs`, growths `GraveWardenMutationLayer`). `runRiseFilm` ends with shots of it
  from the front, three quarters, the back and at night.
  Trial zombies are softer than vanilla ones (10 health, 2 damage, a little slower), hunt the player (follow range 64),
  drop nothing, give no experience, never burn or turn into drowned, stay in Peaceful (where they do no damage) and
  are never saved. The spirit arrives with full health and food.
- Counter: one slim strip at the top center (made smaller at Enzo's request): "RISING DEAD" and "x/15 left" (or the
  countdown before they rise), progress along its bottom edge. Payload `trial_status`.
- Win (the 15th falls): back to the body, realm + 1 at stage 1 with an empty gauge and the stage Max Qi bonus, the spiral
  breakthrough and its window, and "[!] You broke through to Third-Rate Warrior." to the player alone: nothing about
  a trial is ever told to the server (Enzo, 25 Oct: it stays secret).
- Loss: anything that ends the trip before the last zombie falls (a lethal blow, nobody dies there; falling off;
  RETURN TO BODY; a blow on the body; logging out). The trial's zombies vanish and 30 % of the gathered stage Qi
  scatters (`Cultivation.scatter`), so the gauge must be refilled before trying again.
- Runs live in memory (`MortalTrial`), cleared when the server stops.

## The island (`TrialIsland`, version 1)

Dimension `murimblock:mortal_trial` (always midnight, overworld sky: stars and moon; same void generator as the inner
world), at the same coordinates as the player's own island, built on the first trial (`InnerIslands` saves
`trial_built`). Same footprint as the inner island: podzol, coarse dirt and dead grass; a polished blackstone arena
with deepslate spokes, a bone and blackstone taiji and six crying obsidian cracks; soul lantern posts; about 45 graves
(headstones of walls, chiseled bricks or slabs, coarse-dirt mounds, lit candles); eight dead dark-oak trees; a broken
iron fence; a dark gate with hanging soul lanterns and cobwebs; a stone mausoleum on the north zone (candle altar, a
skull); a black pool; floating islets. The client adds a close blue-grey fog and falling ash (`TrialHud`). The inner
world's guards apply: nothing breaks, nothing is placed.

## Check

`gradlew runInnerWorldTour` ends with the trial: the BEGIN TRIAL page, arrival, the first two out of the graves, a lost
trial (−30 %, zombies gone), then all 15 put down and Third-Rate Warrior. Shots `murim_trial_page`, `murim_trial_dive`, `murim_trial_rise_1`, `_rise_2`, `murim_trial_title`,
`murim_trial_arrival`, `murim_trial_island`, `murim_trial_won`. Unit tests: `CultivationTest` (realm breakthrough,
scatter). `gradlew runEngineTest -PengineGroups=trial [-PengineGifs=true]`: one of the dead climbs out and then
comes, the Warden comes out and fights at its pace, the player gets up (GIFs `trial_climb`, `trial_warden`,
`trial_rise`). Not yet played by hand with real fights.
The tour also films the entry, one shot per tick in `screenshots/film/`; make the GIF (kept local, out of the
repository) with `java tools/frames-to-gif.java run/inner-world-tour/screenshots/film run/gifs/murim_trial_entry.gif`.

To tune the getting up alone: `gradlew runRiseFilm` (`run/rise-film`) films it three quarters from the side, one shot per tick, in
`screenshots/rise/`. The getting up is drawn by Shenfa only (Shenfa players off: no animation). PAL, which played
it until 30 Oct, is out of the mod.
