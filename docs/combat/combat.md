# Bare-handed combat

Our own melee, no Epic Fight. Slice 1, step 1 (25 Oct 2026): one strike, the jab, with placeholder animation.
Guard, dash, combo, lock-on and our damage model come in the next steps.

## Choices taken for step 1 (Enzo may change them)

- Left click with an empty main hand: strike when aiming at a creature or the air, mine when aiming at a block.
  With an item in hand, vanilla is untouched.
- Damage goes through `hurt` with a player-attack source, so vanilla armor still applies after our damage.

## Moves

One JSON file per strike in `data/<namespace>/combat_moves/` (`jab.json`). Times in ticks:

| Field | Meaning |
|---|---|
| `animation` | PAL animation id, in `assets/<namespace>/player_animations/` |
| `phases` | `windup` before the blow lands, `active` ticks it can hit, `recovery` before the next action |
| `reach`, `angle` | blocks from the eyes, full width of the cone in degrees |
| `damage`, `knockback` | damage before armor; knockback replaces vanilla's 0.4 |
| `step` | blocks stepped forward on the first tick (on the ground only) |
| `move_speed` | multiplier on walking input during the move |
| `hit_stop` | ticks the striker freezes on impact |

`/reload` reads them again and sends them to every client. A world data pack overrides the built-in file; editing
`src/main/resources` needs `gradlew processResources` before `/reload`.

## How it works

- `combat/StrikeClock`: the timeline (idle, attack, guard, dash, stunned), pure Java, unit tested (`StrikeClockTest`).
- `combat/Fighter`: clock + move; a non-saved attachment on the server, a single instance for the local player.
- Client prediction (`client/combat/ClientCombat`): the click starts the animation, the step and the local hit test at
  once, and sends `StrikeRequest`. The server (`combat/CombatServer`) refuses if not bare-handed, meditating or still
  busy (3 ticks of slack for lag), and the client then stops the animation.
- Hits: on each active tick the server sweeps the cone from last tick's view to the current one (`StrikeReach`),
  living entities only, not through blocks, each target once per strike. It resets the target's hurt delay so one
  strike is exactly one damage roll, and no vanilla attack cooldown applies.
- Impact: sound, crit particles, a short camera shake for striker and target, and hit-stop (the animation and the
  timeline pause). The striker plays it from its own prediction; others get `Impact` from the server.
- Combat animations play on their own PAL layer (`murimblock:combat`, above the lotus and getting-up layer).

## Checks

`gradlew runCombatTest` (`run/combat-test`) clicks like a player at a still husk: predicted start, step, one damage
roll per strike even clicked twice, no cooldown, vanilla kept with an item, mining kept on blocks. PASS/FAIL in the
log, then frames in `screenshots/combat/` (`fp_*` first person, `side_*` from the side). GIF with
`java scripts/frames-to-gif.java <frames dir> run/gifs/<name>.gif`.

Known for step 3: in first person the arm barely shows (PAL third-person model arms); the jab animation is a
placeholder.
