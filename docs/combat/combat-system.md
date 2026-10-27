# Combat on Shenfa (first version, 27 Oct 2026)

Built after Epic Fight's model (`epic-fight-study.md`), with Shenfa's clips. Code: `com.murimblock.combat` (common:
rules, server, payloads), `com.murimblock.client.combat` (keys, prediction, feel, lock-on, trails, HUD), mixins
`MobStrikeMixin` (common) and `LivingEntityRendererMixin` (client). All numbers are first values, to tune in game.

## Keys

| Key | Default | Does |
| --- | --- | --- |
| Combat Mode | B | toggles combat mode (D4); the HUD shows the Qi bar |
| Attack | left click | in combat mode: the next strike (no mining, no vanilla swing) |
| Guard | right click (held) | in combat mode: raises the guard (no item use) |
| Dodge | Left Alt | a dodge the way the movement keys point (back with none held) |
| Technique | R | the moveset's technique, paid with Qi |
| Lock On | V | locks the camera on the foe nearest the crosshair |

## Movesets (`Moveset`)

- Fists (bare hands, or any item that is not a blade): combo jab, cross, hook, uppercut; dash strike `fists/step_in`
  (sprinting); air strike `fists/hammer` (once per jump); technique `fists/palm` (25 Qi, super armor, knockdown).
- Blade (sword or axe in the main hand): combo `blade/slash_1..3`; dash `blade/thrust`; air `blade/drop_cut`;
  technique `blade/whirl` (30 Qi, a full turn, up to 8 targets).
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
  pose; a box in front of the body for mobs on their own skeleton); each body is hit once per phase, up to its max.
- Player damage: attack damage attribute (the weapon) times the phase's, times 1 + 0.5 per realm + 0.05 per stage,
  enchantments applied, weapon durability used. Mobs deal vanilla's own blow (damage, effects) when the window lands.
- Stagger: the phase's stun (short, long, knockdown, launch) weakened by the target's stun armor (golem 6, ravager 6,
  warden 12, players armor / 8); too weak a blow does not stagger. A stagger breaks the move the target played.
- Hit stop: both bodies freeze 2 to 6 ticks by impact (beyond Epic Fight).
- Guard (held, facing the blow, also against arrows): blocks for 2 x impact x penalty Qi, the penalty growing 0.25 per
  blocked blow (reset after 2 s); out of Qi the guard breaks (half damage, long stagger, Qi emptied).
- Parry: a blow landing within 8 ticks after raising the guard (after 10 ticks down) is parried: no damage, the
  attacker staggers.

## Mobs (`MobPatches`, `MobStrikeMixin`)

When vanilla makes a patched mob strike (`doHurtTarget`, from its goals or brain), the mob plays one of its attacks
instead (by weight and distance) and the blow comes at the clip's window. Humanoids (zombie, husk, drowned, zombie
villager, skeleton, stray, wither skeleton, zombified piglin, piglin, piglin brute, vindicator, enderman) use biped
clips (`humanoid/claw`, `lunge`, `punch`, `overhead`, and the blade's cuts when armed). Others use clips keyed on
their own parts (`mob_animations/*.json`): spider and cave spider (bite, pounce), iron golem (smash, swipe), ravager
(bite, ram), hoglin and zoglin (toss), wolf (bite, leap), polar bear (swipe, maul), warden (smash, swipe), vex
(stab), silverfish and endermite (bite). Unpatched mobs (creeper, blaze, slime, phantom, guardian, the bosses...)
keep vanilla's attacks and still reel from blows (`common/flinch|stagger|knockdown`).

## Feel (`CombatEffects`, `Trails`, `LockOn`, `CombatHud`)

Sparks by result (blunt, cut, guard, parry flash, guard break), sounds (vanilla's), camera shake for the one hit
(less for the striker), a white flash on heavy blows and broken guards, ribbons behind fists and blades in the hit
window, reactions on the target's own skeleton, lock-on with flick switching and retargeting after a kill.
