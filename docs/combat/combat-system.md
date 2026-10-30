# Combat on Shenfa (first version, 27 Oct 2026; trimmed 28 Oct)

Built after Epic Fight's model (`epic-fight-study.md`), with Shenfa's clips. Code: `com.murimblock.combat` (common:
rules, server, payloads), `com.murimblock.client.combat` (keys, prediction, feel, lock-on, trails, HUD). All numbers
are first values, to tune in game.

On 28 Oct Enzo had the technique key (R, `fists/palm`, `blade/whirl`), the blade moveset and every mob on Shenfa
(rigs, mob clips, `MobPatches`, the strike and renderer mixins) taken out: weapons and techniques will be redone to
his design, and mobs designed with him. They are in git history before that day if a piece is wanted back.

## Keys

| Key | Default | Does |
| --- | --- | --- |
| Combat Mode | B | toggles combat mode (D4); the combat HUD slides in (see below) |
| Attack | left click | in combat mode: the next strike (no mining, no vanilla swing) |
| Guard | right click (held) | in combat mode: raises the guard (no item use) |
| Dodge | Left Alt | a dodge the way the movement keys point (back with none held) |
| Lock On | V | locks the camera on the foe nearest the crosshair |

## Moves (`Moveset`)

- Fists, whatever is held: combo jab, cross, hook, uppercut; dash strike `fists/step_in` (sprinting); air strike
  `fists/hammer` (once per jump).
- Dodges (`combat/dodge_*`): forward roll, back, left, right; 6 Qi; invulnerable to melee and projectiles in their
  window (D5); root motion carries the body.

## Rules (`CombatState`, `CombatMoves`, shared by server and client)

- A strike locks walking until its recovery; from the recovery the next strike may start (combo), or a dodge.
- The next combo strike continues the chain if started in the recovery or within 8 ticks after the strike ends.
- An input that cannot start yet waits 8 ticks in the buffer (Epic Fight's buffer).
- The client predicts its own moves with the same rules and starts them on the key press; the server decides and
  sends a correction (`Self`) when it disagrees.
- A strike turns the body to its target (locked foe, crosshair, or nearest foe ahead) and closes the gap during the
  windup, at most 1.5 blocks (Epic Fight's destination provider).

## Blows (`CombatServer`, `Colliders`)

- Clip sidecars carry `attack` data: phases (windup, active window, recovery, hand, colliders on joints, damage,
  impact, stun, knockback, kind) and invulnerable or super-armor windows (`AttackData`, read by `ClipLoader`).
- The server samples the clip at a few moments between two ticks and sweeps the colliders (joint boxes in the biped's
  pose); each body is hit once per phase, up to its max.
- Damage: attack damage attribute (the weapon) times the phase's, times 1 + 0.5 per realm + 0.05 per stage,
  enchantments applied, weapon durability used.
- Stagger: the phase's stun (short, long, knockdown, launch) weakened by the target's stun armor (players: armor / 8;
  mobs: by size, so a golem barely reels); too weak a blow does not stagger. A staggered mob stands still; a
  stagger breaks the move a player played.
- Hit stop: both bodies freeze 2 to 6 ticks by impact (beyond Epic Fight).
- Guard (held, facing the blow, also against arrows and vanilla mobs' blows): blocks for 2 x impact x penalty Qi, the
  penalty growing 0.25 per blocked blow (reset after 2 s); out of Qi the guard breaks (half damage, long stagger, Qi
  emptied).
- Parry: a blow landing within 8 ticks after raising the guard (after 10 ticks down) is parried: no damage, the
  attacker staggers.

## Mobs

Not on Shenfa: they move and attack as vanilla made them. Our blows hurt and stagger them (no reaction clip), and
their blows meet our guard, parry and dodges.

## Feel (`CombatEffects`, `Trails`, `LockOn`, `CombatHud`)

Sparks by result (blunt, cut, guard, parry flash, guard break), sounds (vanilla's), camera shake for the one hit
(less for the striker), a white flash on heavy blows and broken guards, ribbons behind fists in the hit window,
reactions on players' skeletons, lock-on with flick switching and retargeting after a kill.

## Combat HUD (`CombatHud`, `LockOn`)

Direction J3 chosen by Enzo on 30 Oct 2026 (`docs/gui/boards/combat-bar-j3.png`; its second board `combat-bar-j3-2.png`
proposes the whole screen, guard, parry, dodge, combo and the ultimate cast; the rejected ones are
`combat-bar*.png` beside it). On 30 Oct Enzo kept the placement but not the look: `combat-bar-j3-look.png` redraws it in
the System menu's style in three looks (A window, B seals, C list), to choose. The active art is chosen in K, Techniques
(no combat key for it). The vanilla hotbar stays. In combat mode a System block slides in from the bottom right:

- The Qi as segments of 10 Qi (a round ten past 200 max Qi, so at most 20), the started one half full, numbers above.
- Under it, the active art's techniques in boxes on keys 1 to 3 and its ultimate, gold, on 4. A box goes pink when its
  Qi is lacking and shows a padlock with the mastery it waits for. Boxes show the technique's initials until each has
  its icon.
- The lock (choice B): a turning pink diamond above the locked foe and a thin health bar facing the camera, the
  health just lost white while it melts.

Waiting for real techniques (`MartialArtsData` is still the menu's placeholder): keys 1-4 cast nothing yet, so the
aim (cost segments blinking), cast (spent segments flash), cooldown (dark box and seconds) and ultimate charge (gold
filling while fighting) states of the board are not drawn yet. Checked by the shot `combat_hud_lock` in the
`combat` engine test group.
