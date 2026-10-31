# TGCraft study (31 Oct 2026)

What svasco's Tokyo Ghoul mod (`github.com/semanavasco/tgcraft`, NeoForge 1.21.1, read at commit 953ada1) does for
combat feel, and what to take. Enzo was given access by its author. The repository has no licence: ideas only, no
code or assets copied. Its own art pipeline lists ideas it took from Murimblock (sources in the repo, in-game tours,
one engine per body).

## How it is built

- Player animations: Player Animation Library (Blockbench / GeckoLib JSON written by Claude, edited by hand). No
  elbows or knees (Bendable Cuboids has no 1.21.1 NeoForge build): straight limbs, a forearm faked by moving the arm.
  Shenfa already beats this.
- Every attack is a move in data (wind-up, active, recovery, reach, arc, targets, damage, parry window) played by one
  runner; extras are registered behaviours (`hit_stop`, `launcher`, `wind_lines`, `air_hold`, sounds).
- Method: a reference video, the developer's verdicts logged in the docs with dates ("stiff", "a teleport", "both
  stuck"), each feature reworked one to three times after testing, checked by screenshot tours from several angles.

## What makes it dynamic

| Layer | What it does |
|---|---|
| Combat movement set | Idle, walk, run, jump, fall, landing redone anime-like. Poses are keys; the life is code (`MovementMotion`): arms swing against the legs, chest twists against the hips, the body rolls over each foot, leans into speed and turns on springs that overshoot, sinks and rebounds on landing, breathes standing. |
| Move style | Hips lead, arms trail and overshoot, dip in the wind-up then rise or lunge, settle past the end. Fists: jab 10 ticks (wind-up 4), cross 12 (5), hook 16 (7), uppercut 20. Moves start and end near the combat idle. |
| Moves by body state | A press picks a chain by state: after a dash (palms), jumping back (leaning kick), forward (flying kick), up (high kick), sprinting (superman punch), in the air (air chain). |
| Hit stop by weight | 1 tick a flurry hit, 2 a sweep, 3 a thrust, 4 a slam; the target hangs where struck, shaking, then flies off. Fists had none at first: it felt odd until the body moves were wound up harder. |
| Wind lines | White ribbons from hands and feet, only where a limb swings fast against the body; on dashes every limb. |
| Hit bursts | Pixel frames, one a tick, where the blow lands (white; gold and bigger for a parry), stuck to the struck body. |
| Impact frames | On heavy finishers: black and white negative, then black and white, zoom blur; cut, not faded; client option. |
| Ground | Rubble ring heaved up by slams (drawn only), pixel dust in three greys, blood stains that fade. |
| Dodge and guard | Afterimages on a dodge that avoids a blow; a hexagon barrier where a blow meets the guard. |
| Air combos | A launcher opens a jump window; the jump is assisted to reach the target; air hits keep both up; a slam finisher with hit stop, impact frames and shake. |
| Sounds | Generated from noise (own), a swing and a hit sound per move, three takes each. |

## For Murimblock

We already have hit stop, trails, sparks, camera shake, reaction clips and elbows and knees. Missing, by value for
the "dynamic" feel: a living combat movement set, hit stop by weight with the target hanging, wind lines only on
fast limbs, impact frames and ground rubble on finishers, moves by body state, air combos. Keep the Murim taste:
rhythm and weight, not a flood of effects.
