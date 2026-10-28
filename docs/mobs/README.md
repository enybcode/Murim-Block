# Mob looks

Vanilla mobs keep their id, spawns, drops and behaviour; we give them a new look: a model in relief on the biped's
skeleton (`tools/blender/mobs/<name>.py`, built by `tools/blender/mob.py`), a texture painted in code, and motion.
`SkeletalMobs` maps each mob type to its look; `SkeletalMobRenderer` draws it (glowing parts, worn armor from the
biped's armor parts, held items) and falls back to vanilla's renderer when `shenfaMobs = false` in
`config/murimblock-client.toml`. The engine test group `mobs` checks it in game.

| Mob | Look | Sheet | State |
|---|---|---|---|
| Zombie | Disciple déchu | `disciple-dechu.png`, model `disciple-dechu-model-v1.png` | In game (28 Oct 2026). Moves with the biped's living motions; reaches and swings like vanilla's zombie. Own clips and attacks: next. |

Not yet: each mob's own clips (idle stooped, dragging walk, the three attacks of its sheet), cloth on the rope ends
and robe skirts, baby zombies' larger head (they are drawn as a half-size adult).
