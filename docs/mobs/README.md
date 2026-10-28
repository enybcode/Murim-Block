# Mob looks

Vanilla mobs keep their id, spawns, drops and behaviour; we give them a new look: a model in relief on the biped's
skeleton (`tools/blender/mobs/<name>.py`, built by `tools/blender/mob.py`), a texture painted in code, and motion.
`SkeletalMobs` maps each mob type to its look; `SkeletalMobRenderer` draws it (glowing parts, worn armor from the
biped's armor parts, held items) and falls back to vanilla's renderer when `shenfaMobs = false` in
`config/murimblock-client.toml`. The engine test group `mobs` checks it in game.

| Mob | Look | Sheet | State |
|---|---|---|---|
| Zombie | Disciple déchu | `disciple-dechu.png`, armed moves storyboards `boards/` (validated, clips validated 28 Oct 2026), model `disciple-dechu-model-v1.png`, clips `disciple-dechu-clips-v1.png` | In game (28 Oct 2026) with its own clips (`skeletal/animations/disciple`): slumped idle, dragging walk, reaching chase, half guard when aggressive, flinch, death. It fights by `DiscipleFighting` (server): waits in guard 3 blocks off, steps in about every two seconds and picks the haymaker, the double blow (always after two blocked blows, else 1 in 4) or the lunge (when its prey backs away, at most every 5 s); each blow lands at its clip's impact through guard, parry and dodge; in a crowd of three it only strikes. Armed (`DiscipleFighting.Weapon`), the same moves its weapon's way with their own guard: sword (cut, cut and backhand, lunging thrust), axe (overhead chop, sweep then chop, charge), any tool held like a staff (sweep, two pokes, charge); a weapon reaches half a block further. |
| Husk | Disciple des sables | `disciple-sables.png` | Design sheet to validate (28 Oct 2026); the zombie's skeleton and clips, look only. |
| Drowned | Disciple noyé | `disciple-noye.png` | Design sheet to validate (28 Oct 2026); the zombie's clips, plus two new clips later (fork throw, swim), storyboard first. |
| Zombie villager | Paysan possédé | `paysan-possede.png` | Design sheet to validate (28 Oct 2026); the zombie's skeleton and clips, look only. |
| Zombified piglin | Sanglier du culte | `sanglier-du-culte.png` | Design sheet to validate (28 Oct 2026); the zombie's skeleton and armed sword clips, look only. |
| Skeleton | Archer impérial | `archer-imperial.png`, model `archer-imperial-model-v1.png` | In game (28 Oct 2026): model validated; moves with the biped's motions and aims its bow like vanilla's skeleton. Own clips and moves (aimed shot, volley, bow bash): next, storyboard first. |

Cloth (`MobCloth`, pieces listed by `mob.py` in `skeletal/mobs/<name>.json`): the rope ends and the robe's front and back
panels hang from the belt and swing with the body, off the legs.

The zombie family side by side: `zombie-family.png`. Sheets are drawn by `sheet.js` from `zombie-family.js` and printed
to PNG with headless Chrome at 1600 x 800.
