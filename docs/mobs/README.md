# Mob looks

Vanilla mobs keep their id, spawns, drops and behaviour; we give them a new look: a model in relief on the biped's
skeleton (`tools/blender/mobs/<name>.py`, built by `tools/blender/mob.py`), a texture painted in code, and motion.
`SkeletalMobs` maps each mob type to its look; `SkeletalMobRenderer` draws it (glowing parts, worn armor from the
biped's armor parts, held items) and falls back to vanilla's renderer when `shenfaMobs = false` in
`config/murimblock-client.toml`. The engine test group `mobs` checks it in game.

| Mob | Look | Sheet | State |
|---|---|---|---|
| Zombie | Disciple déchu | `disciple-dechu.png`, model `disciple-dechu-model-v1.png`, clips `disciple-dechu-clips-v1.png` | In game (28 Oct 2026) with its own clips (`skeletal/animations/disciple`): slumped idle, dragging walk, reaching chase, half guard when aggressive, strike (every third swing the double blow), flinch, death. |

Not yet: the lunge (its clip is ready; it needs the server to make the zombie leap) and the attacks chosen by the
server (today vanilla's zombie decides when to hit and the client picks the clip), cloth on the rope ends
and robe skirts, baby zombies' larger head (they are drawn as a half-size adult).
