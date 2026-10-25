# Mortal trial (The Rising Dead)

Status: in the game since 25 Oct 2026. Mockup "Graveyard Inner World" validated by Enzo the same day
(artifact 4cL5qNBhdTUcZqgc42rZNn): the trial out of the mortal realm is a graveyard island at night and waves of
zombies, no NPC. The Heart Demon stays the idea for the realms above.

## Decisions (Enzo, 25 Oct)

- BEGIN TRIAL opens the graveyard island; 5 waves of zombies.
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
    (PAL animation `player_animations/trial_rise.json`, 1.8 s, earth thrown up), then back to first person and the
    title **SURVIVE DEATH** (dark red) with "Mortal Trial · five waves of the dead". The first wave waits 7 s.
- Waves: 3, 5, 7, 9, then 9 and the **Grave Warden** (1.6× size, 50 health, 6 damage) out of the mausoleum.
  7 s of calm before the first (the arrival plays meanwhile), 4 s between waves. Zombies climb out of the grave mounds farthest from the player,
  with earth thrown up; they hunt the player (follow range 64). Trial zombies have 14 health, drop nothing, give
  no experience, never burn or turn into drowned, stay in Peaceful (where they do no damage) and are never saved.
- Counter: one slim strip at the top center (made smaller at Enzo's request): "WAVE n/5" and "x/y left" (or the next
  wave's countdown), progress along its bottom edge. Payload `trial_status`.
- Win: back to the body, realm + 1 at stage 1 with an empty gauge and the stage Max Qi bonus, the spiral
  breakthrough and its window, and "[!] X broke through to Third-Rate Warrior!" to the whole server.
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

`gradlew runInnerWorldTour` ends with the trial: the BEGIN TRIAL page, arrival, wave 1 out of the graves, a lost
trial (−30 %, zombies gone), then five cleared waves and Third-Rate Warrior. Shots `murim_trial_page`, `murim_trial_dive`, `murim_trial_rise_1`, `_rise_2`, `murim_trial_title`,
`murim_trial_arrival`, `murim_trial_island`, `murim_trial_won`. Unit tests: `CultivationTest` (realm breakthrough,
scatter). Not yet played by hand with real fights.
