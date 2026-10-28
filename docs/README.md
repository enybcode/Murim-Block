# Murimblock V2 Docs

One file per feature or decision, added as the work goes.

- [System GUI](gui/system-gui.md): the player's menu (Profile, Techniques, Cultivation, Info), its look and code.
- [Cultivation](cultivation/cultivation.md): Qi data, stages, meditation and meridians (first slice, proposal).
- [Inner world](cultivation/inner-world.md): each player's private island, entered from meditation from Third-rate on.
- [Mortal trial](cultivation/mortal-trial.md): the way out of the mortal realm, fifteen zombies on a graveyard island.
- [Shenfa to-do](combat/shenfa-todo.md): what is left for the animation engine and combat, in order (the current list).
- [Shenfa roadmap and record](combat/engine-roadmap.md): the original step plan and how each done step was built.
- [Combat system](combat/combat-system.md): keys, the fists, rules, blows, guard and parry, mobs, feel (first version).
- [Epic Fight study](combat/epic-fight-study.md): what Epic Fight does in first person, combat and feel (27 Oct 2026).
- [Two PCs](two-pcs.md): how the two PCs stay in sync through GitHub, phone access, and the second PC's setup prompt.
- [Playtest checklist](playtest/tests-en-jeu.pdf): what Enzo tests in game, to tick and annotate (in French; source
  `tests-en-jeu.html`, printed to PDF with Chrome headless).

## Repository layout

- `src/main/java/com/murimblock/`: `shenfa/` the animation engine (`shenfa/client/` draws players), `combat/` and
  `client/combat/` the fight (server rules, client keys and feel), `cultivation/`, `innerworld/`, `registry/`,
  `client/gui/` the System screen, `client/dev/` the scripted in-game checks (off unless a dev run sets them), `mixin/`.
- `src/main/resources/assets/murimblock/`: `skeletal/` the biped and its clip sets (glTF and sidecars), `lang/`,
  textures, GeckoLib models, particles, `player_animations/` (PAL).
- `tools/`: `blender/` builds the biped and the clips, `textures/` paints original textures and models (Node),
  `gifs.sh` and `frames-to-gif.java` turn recorded frames into GIFs.
- `docs/`: this index; `playtest/` the in-game checklist.
