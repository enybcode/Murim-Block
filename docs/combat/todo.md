# Combat to-do (slice 1: bare hands)

Decided 26 Oct 2026: combat mode on B; dash invulnerable to blows and arrows, not explosions; vanilla armor after
our damage; charged strike, projectiles sent back and special actions cost Qi.

## Still to decide
- [ ] The full damage model: player HP by realm, mob damage, "weak against strong" in PvP.
- [ ] How much Qi each action costs, and what happens with too little Qi.
- [ ] Keys: the in-game guide lists V for Guard and G for Sense Qi, the plan had right click for guard and G for
      lock-on.

## Step 1: engine, combat mode, first person (done 26 Oct 2026)
- [x] Moves in JSON (phases, reach, angle, damage, knockback, step, slowdown, hit-stop, first-person poses),
      reloaded by `/reload`.
- [x] Fighter states: idle, attack, guard, dash, stunned.
- [x] Combat mode on B: the click always strikes, never mines; vanilla outside; ends with an item in hand or in
      meditation; message and emblem by the hotbar.
- [x] Strike predicted on the client, checked by the server; hits swept between ticks, each target once.
- [x] Hit-stop, camera shake, sound, particles; slowed while striking, small step forward.
- [x] Both arms in first person during a strike (left jab, right guard), vanilla hand at rest.
- [ ] Enzo tries it in game.

## Foundations for weapons (styles done 26 Oct 2026)
- [x] Fighting styles in JSON (`combat_movesets`): what is held, the M1 chain, chain window, priority.
- [x] Combat mode and strikes follow the style of the held item; a test sword style works by item tag.
- [x] One owner for combat animations with fixed priorities (stunned, dash, guard, attack); none over the lotus or
      the getting up.
- [x] A blow taken cancels the move and the chain: 8 ticks of flinch, not for fire or poison, `armor_ticks` ready.
- [x] Hit shapes per move (box, arc) with a debug view around the player (`/murim debug hitboxes on`).

## Step 2: combo and charged strike
- [ ] Jab, cross, hook, fists alternating; one click buffered; the chain drops after a short pause.
- [ ] Hold the click to charge: visible charge, more damage and knockback, "FULL CHARGE"; costs Qi.
- [ ] Real third-person animations (combo, charge, guard) with bent elbows and knees.
- [ ] First-person poses for each move.
- [ ] Check: GIFs from the side, the front and first person.

## Step 3: defense
- [ ] Guard, front only, with its stance.
- [ ] Normal parry: half damage, no flinch.
- [ ] Perfect parry: 10 ticks, once per raise, attacker staggered.
- [ ] Send projectiles back: a parried arrow flies back at the shooter, a ghast fireball struck in time flies back
      ("RETURN TO SENDER"); costs Qi.
- [ ] Dash on left Alt: 2 blocks, short invulnerability (blows and arrows), 0.6 s cooldown, on the ground only,
      no momentum kept, never into the void or through a wall; costs Qi.
- [ ] Trial zombies on the engine, with a visible windup so they can be parried.

## Step 4: combat feedback on screen
- [ ] Damage numbers over the target, with an option to hide them.
- [ ] Style rank top right (wuxia ranks or D to SSS), a gauge that drains when the hits stop.
- [ ] Under it: hits, total damage, the last moves (JAB, CROSS, HOOK, PARRY...).
- [ ] A puff of particles when the target dies.

## Step 5: lock-on
- [ ] A key to lock or release the nearest enemy (16 blocks), with a mark over it; wheel to switch.
- [ ] Camera follows smoothly, with an option to turn it off.
- [ ] Strikes, dashes and returned projectiles aimed at the target.

## Step 6: our damage
- [ ] `CombatRules`: move base x realm and stage bonus x combo multiplier x charge.
- [ ] Unit tests of the formulas.

## Step 7: links with the rest
- [ ] Qi spent by combat actions, from the cultivation Qi.
- [ ] A future Qi technique can cut a combo.
- [ ] Keys can be rebound (Alt+Tab, other mods).

## Step 8: checks and doc
- [ ] In-game tour with a test attacker on controlled timing: combo, charge, perfect and normal parry to the tick,
      arrow and fireball sent back, dash through a blow, lock-on.
- [ ] Simulated lag, two clients for PvP.
- [ ] Final doc and GIFs.
