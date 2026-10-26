# Bare-handed combat

Our own melee, no Epic Fight, modelled on a video Enzo showed on 26 Oct 2026 (combat mode on a key, both arms in
first person, jab / cross / hook, charged strike, parry, projectiles sent back, a style rank). Slice 1, step 1:
combat mode and the bare-handed M1 chain from the mockup (jab, cross, hook, uppercut). The full plan is in `todo.md`.

## Decisions (Enzo, 26 Oct 2026)

- Combat mode on **B**. In it the left click always strikes and never mines; out of it, vanilla. It needs a held item
  with a fighting style (for now only empty hands) and ends by itself without one, in meditation, on death.
- Dash invulnerability covers blows and arrows, not explosions.
- Vanilla armor applies after our damage (damage goes through `hurt` with a player-attack source).
- Charged strike, projectiles sent back and the other special actions cost Qi.

## Fighting styles

One JSON file per style in `data/<namespace>/combat_movesets/` (`bare_hands.json`). Swords and spears will be new
styles and moves on the same engine, without code.

| Field | Meaning |
|---|---|
| `held` | what the main hand holds: `"empty"`, an item id, or `"#"` and an item tag |
| `chain` | the left-click chain (M1), move ids in order |
| `chain_window` | ticks after a move ends in which the next click goes on with the chain (default 10) |
| `priority` | when several styles match an item, the highest wins (ties: the smaller id) |

Combat mode is allowed while the main hand has a style; holding something without one ends it. Changing to another
style stops the move in progress. The server only accepts a strike from the chain of the style in hand.

## Moves

One JSON file per strike in `data/<namespace>/combat_moves/` (`jab.json`). Times in ticks:

| Field | Meaning |
|---|---|
| `animation` | PAL animation id, in `assets/<namespace>/player_animations/` |
| `phases` | `windup` before the blow lands, `active` ticks it can hit, `recovery` before the next action |
| `hitbox` | where it hits, around the eyes: `{"type": "box", "start", "length", "width", "height"}` along the view (up and down too), or `{"type": "arc", "radius", "height", "from", "to"}` swept in degrees of the view (negative to the left) over the active ticks |
| `damage`, `knockback` | damage before armor; knockback replaces vanilla's 0.4 |
| `lift` | upward push given to the target (the uppercut), default 0 |
| `step` | blocks stepped forward on the first tick (on the ground only) |
| `move_speed` | multiplier on walking input during the move |
| `hit_stop` | ticks the striker freezes on impact |
| `armor_ticks` | first ticks of the move in which a blow taken does not cut it (default 0) |
| `first_person` | `left` and `right`: keys `{tick, fist: [x, y, z]}`, the fist in camera space (blocks, x right, y up, -z ahead) |

`/reload` reads them again and sends them to every client. A world data pack overrides the built-in file; editing
`src/main/resources` needs `gradlew processResources` before `/reload`.

## How it works

- `combat/StrikeClock`: the timeline (idle, attack, guard, dash, stunned), pure Java, unit tested (`StrikeClockTest`).
- `combat/Fighter`: clock + move; a non-saved attachment on the server, a single instance for the local player.
- Client prediction (`client/combat/ClientCombat`): the click starts the animation, the step and the local hit test at
  once, and sends `StrikeRequest`. The server (`combat/CombatServer`) refuses out of combat mode, a move outside the style in hand, or still
  busy (3 ticks of slack for lag), and the client then stops the animation.
- Hits (`StrikeReach`, `HitGeometry`): on each active tick the server tests the move's hit shape against every living
  box nearby (a box with the separating axis test, an arc cut into convex slices), following the view from the last
  tick to this one, not through blocks, each target once per strike. It resets the target's hurt delay so one
  strike is exactly one damage roll, and no vanilla attack cooldown applies. The geometry is unit tested
  (`HitGeometryTest`).
- Hitbox view: `/murim debug hitboxes on|off` (operators). Our own strike's shape is drawn around us, grey while it
  winds up, red while it can hit, dark once spent (an arc: the whole swing in grey, the slice being swept in red);
  living boxes nearby in white, the ones just struck in yellow.
- Impact: sound, crit particles, a short camera shake for striker and target, and hit-stop (the animation and the
  timeline pause). The striker plays it from its own prediction; others get `Impact` from the server.
- Combat animations play on their own PAL layer (`murimblock:combat`), and only through `client/combat/CombatAnimator`:
  one action per player, a new one cuts the current one only when it ranks as high or higher (stunned > dash >
  guard > attack, `FighterState`), with a 2-tick fade; hit-stop pauses it. Nothing plays, and no strike starts,
  while the cultivation layer (lotus, getting up) is busy. The server follows the same order (`StrikeClock.canEnter`).
- Combat mode: B sends `ToggleCombatMode`; the server decides and answers `CombatMode` (with the reason when it ends
  it itself). In the mode the client takes the attack clicks before vanilla (`ClientTickEvent.Pre`) and cancels every
  vanilla attack trigger, held button included; the server also cancels vanilla attacks and block hits. A crimson
  emblem sits right of the hotbar.
- A blow taken (`CombatServer.blowTaken`): in combat mode, damage from a creature or a projectile stops the move
  in progress and the chain, and nothing can start for 8 ticks (`FLINCH_TICKS`); fire, poison, falls and hunger
  cut nothing, and a move's `armor_ticks` shrug blows off. The server sends `Flinch` to the fighter and those
  watching: the predicted strike stops and `combat_flinch` plays above any strike.
- The chain (`ChainTracker`, unit tested): a click within the style's `chain_window` after a move ends throws the next
  move, after the last one the first again; later, or after a blow taken, it starts over. A click during a move is
  kept and thrown the tick the move allows (one click ahead); a click while flinching is lost. The client sends the
  chain index; the server accepts the next one (window widened by the lag slack) or a fresh start at the first.
- The bare-handed chain: jab 2/2/5 ticks, 3 damage; cross 3/2/6, 3.9, box 3.3 long; hook 4/2/7, 4.5, arc of 2.7
  from 70 degrees left to 20 right; uppercut 5/2/10, 6, lift 0.55. First-person keys come from the mockup's
  skeleton, rescaled to our camera; third-person animations `combat_jab`, `_cross`, `_hook`, `_uppercut`.
- First person (`client/combat/FirstPersonArms`): during a strike both arms are drawn from the skin in place of the
  vanilla hand. Each hangs from a fixed shoulder and reaches to its fist, eased between the move's keys; hit-stop
  holds them still.

## Checks

`gradlew runCombatTest` (`run/combat-test`) presses B and clicks like a player at a still husk: vanilla punch out of
the mode, B on and off, a test sword style matched by tag, predicted start, step, one damage
roll per strike even clicked twice, no cooldown, an item ends the mode, no mining even held, a blow taken, the
hitbox edges with the hitbox view on (ahead, too far, to the side, behind, behind a wall, a test hook arc; shots
`hitbox_jab_*`, `hitbox_hook_*`). PASS/FAIL in the
log, then frames in `screenshots/combat/` (`fp_*` first person, `side_*` from the side). GIF with
`java scripts/frames-to-gif.java <frames dir> run/gifs/<name>.gif`.

The chain checks: four clicks in a row throw the four moves and deal their damage (17.1 after armor), the uppercut
pushes up, a long pause starts over, a click in the window goes on, a blow taken starts over; then the chain filmed
in first person, from the side and from the front (`chain_*`). A husk without AI never moves, so the test reads the
uppercut's upward push rather than a flight.

Known: the animations are first versions, to tune by eye in game.

`/reload` is checked by `runCombatTest` too: a world data pack makes the jab hit for 5, then removing it brings 3 back.

Over the network: `gradlew runCombatDuelServer`, then `runCombatDuelAttacker` and `runCombatDuelDefender` together
(`run/combat-duel/`, the server folder needs `eula.txt` and `online-mode=false`). `Dev` turns combat mode on and
strikes `Dev2`; each client checks what crossed the network (moves and styles sent, health lost, the strike and the
blow seen by the defender), logs PASS/FAIL and quits; the server stops when both have left. The duel clients never
pause when their window is in the back: closing the pause menu would release every key, the test's clicks included.
