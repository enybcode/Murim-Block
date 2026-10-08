# Murimblock

Minecraft Murim / Wuxia mod. This repository is the IntelliJ development project;
the separate MCreator workspace is not the source of truth.

Changes awaiting in-game approval live on **test**. **main** is the stable,
user-validated branch. Automated checks do not authorize a merge.

## Development environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Gradle Wrapper 9.2.1

Epic Fight 21.17.3.1 is required on the client and server, pinned to Modrinth artifact
`8HHhJt6i`. Gradle installs it for IntelliJ runs. Players must install that same
NeoForge / Minecraft 1.21.1 release alongside Murimblock.
See [Epic Fight integration](docs/EPIC_FIGHT.md) for the integration boundary and
validation checklist. Epic Fight is a dependency, not an engine copied into this
repository. Its API and animations are used through the installed mod.

Open this folder in IntelliJ, select a Java 21 Gradle JVM, reload Gradle and use
the generated client/server configurations or the wrapper tasks below. Restart
the game after updating Java classes, mixins or dependency versions. Client and
server must use the same Murimblock build (network protocol **7**).

## Common tasks

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat -PgameTests runGameTestServer
```

## Current systems

- Qi and Qi Max player data.
- Passive and active Qi regeneration.
- Configurable keybind for charging Qi.
- Qi charging movement lock, FOV and particle effects.
- Compact blue Qi HUD in Epic Fight combat mode; no native skill icons.
- Stamina disabled for player combat: no yellow endurance bar or stamina exhaustion.
- Cultivation realms, stages and breakthrough checks.
- Server-side Qi rewards for mob kills with anti-farm and boss first victories.
- Actual Epic Fight mode bridge; the engine owns combat, animation, collision and guard.
- Native screens replaced by Murimblock; combat camera settings remain available.
- Techniques is deliberately empty: no Epic Fight library, learning or equipping UI.
- Basic guard and roll are server-assigned combat actions available without books.
- Native passive upgrades, movement skills and weapon special attacks cannot cast
  and are removed from equipped slots; other namespaces are not stripped.
- Epic Fight items removed from creative tabs/search, recipes and generated loot.
- Legacy engine items removed from loaded player inventories, ender chests, opened
  containers and dropped/equipped items, without unregistering save IDs.
- One Murimblock keybind category with ten useful bindings instead of four blocks;
  native menu, special-attack and mover shortcuts are disabled and hidden.
- English in-game text and a dedicated bitmap font for the Murim profile GUI.
- Four distinct GUI pages with the 3D player shown only on Profile.
- Nine cultivation realms and four stages, with server-side breakthrough checks.
- Admin/debug commands for Qi, Qi Max, cultivation, rewards and combat mode.
- Saved Qi/cultivation/boss progress, with public Qi/cultivation/combat addon APIs.
- Development-only unit, real server combat and real client rendering checks.
- Registered empty entity registry for future Murim mobs; no placeholder entity
  or new natural spawn is added by this preparation.

### What Is Not Implemented

Martial styles, four configurable M1 moves, custom Murim mobs, their animation
packs and physical blade clashes are future work. The empty Techniques tab is
not a finished style system. Epic Fight's supported vanilla mob/boss patches
remain active; outside combat mode its vanilla-mode attacks still exist.

### Existing Worlds And Removed Content

**Back up an existing world before trying this build.** Engine items are actually
removed, not merely hidden. Their registry IDs remain available so existing saves
can load. Unopened/unloaded containers and items nested inside another item's NBT
are not globally migrated. A privileged `/give` can still reference the registered
IDs, but held items are unusable and loaded inventories are cleaned periodically.
Vanilla and Murimblock items, Qi values and cultivation save IDs are not targeted.
JEI/EMI catalog integration is not implemented or tested here.

No new Qi cost was added for attacks, guard or dodge. Disabling stamina does not
remove directional guard rules, animation restrictions or unrelated resources.
See the integration document before upgrading Epic Fight or adding combat addons.

## Ajouter Un Mob Et Ses Animations

Le guide complet est [MOB_CREATION_GUIDE.md](docs/MOB_CREATION_GUIDE.md).
La [fiche a remplir](docs/examples/mobs/MOB_REQUEST_TEMPLATE.md) ne demande pas de
Java : tu fournis le design et les ressources disponibles, je realise le code,
les fichiers techniques, l'integration Epic Fight et les tests.

### Ce Que Tu Me Donnes

| Element | Contenu attendu |
| --- | --- |
| Identite | Nom anglais, role Murim, mob vanilla a adapter OU nouveau mob, boss ou non. |
| Apparence | References face/profil/dos, taille, arme, skin ou PNG, fichier source du modele si disponible. |
| Comportement | Hostile/neutre/allie, cibles, faction, vitesse, distance de poursuite, reaction aux obstacles. |
| Combat | Liste des coups, enchainement, preparation/contact/recuperation, garde, esquive, interruptions. |
| Animations | Projet source, rig, noms des clips, FPS, exports et petites videos ; sinon demandes de mouvements. |
| Monde | Biomes/structures, rarete, conditions d'apparition, drops, XP et recompense Qi souhaitee. |
| Droits | Auteur, provenance et licence autorisant modification et redistribution des ressources. |

Les chiffres inconnus peuvent rester `A proposer`. Une image seule suffit pour
commencer une fiche et un prototype, pas pour promettre un mob final anime.
Pour un humanoide aux proportions Minecraft, une texture de skin peut suffire
au premier prototype ; un corps non standard demande un modele et un rig adaptes.

### Comment Je L'Integre

1. Verifier les ressources et choisir une variante vanilla ou une vraie entite Murim.
2. Enregistrer le type dans `mob/MurimEntities.java`, les attributs et l'IA serveur.
3. Ajouter un rendu client et une texture Minecraft lisible, sans modifier tous les mobs.
4. Raccorder un patch Epic Fight compatible, d'abord aux animations de la dependance.
5. Tester un seul coup : une animation, une fenetre de contact, un degat serveur.
6. Ajouter progressivement combos, garde et animations originales validees sur le rig.
7. Activer spawn/drops/recompenses seulement apres validation du prototype invoque.
8. Tester sauvegarde, mort, dimensions et deux clients, publier sur `test`, attendre
   ta validation avant le passage sur `main`.

### Creer Les Animations

Commencer par **un petit export aller-retour**, pas par vingt animations.
Blockbench convient au modele pixel-art et aux textures. Un `.bbmodel`, une
animation vanilla ou GeckoLib ne devient pas automatiquement une animation
skeletale Epic Fight. Pour ce moteur, preparer un mesh skinned et un rig compatible,
puis exporter avec Blender et l'exporteur Epic Fight apres verification des versions.

Fournir le `.blend`, les textures, l'export JSON, les versions Blender/exporteur,
une video a FPS connu et les instants de preparation/contact/recuperation. Je
realise les declarations d'animation, colliders, IA et synchronisation ; tu n'as
pas a ecrire ces fichiers de code. Les phases de degats ne se deduisent pas
automatiquement de la video ou des keyframes.

Les exemples dans `docs/examples/mobs` sont **inactifs**. Le preset zombie ne
convient qu'a une vraie classe Zombie compatible. Le fragment de comportement
n'est pas un mob complet et ne doit pas etre copie seul dans les ressources.
Voir le guide pour les chemins exacts, les limites et le controle des exports.

## Repository Layout

- `src/main`: shipped mod code and resources.
- `src/test`: unit and resource checks.
- `src/gameTest`: development-only foundation server tests.
- `docs`: maintained technical documentation and GUI demo.
- `.local/gui`: local GUI references and previous asset kits, excluded from Git.
- `build`, `.gradle`, `run`: generated output, caches and local development worlds.

Keep local references out of `src` and commits. Never delete development worlds
as part of source cleanup.

## GUI Preview

`docs/gui/tab-layout-demo.html` previews the four integrated tab layouts in English.
It shares the manuscript font with the mod and uses example values and an
illustrative player. The Minecraft GUI reads synchronized player data and actual
configured keybindings. The integrated appearance was approved by Enzo on
2026-10-03.

## Developer documentation

- `docs/ARCHITECTURE.md`: project architecture and package responsibilities.
- `docs/ADDON_API.md`: public Java API currently available to addons.
- `docs/ADDON_GUIDE.md`: how addon developers should depend on Murimblock.
- `docs/DATA_DRIVEN.md`: planned direction for datapacks, data maps and config.
- `docs/QI_REWARDS.md`: current mob Qi reward balance table.
- `docs/EPIC_FIGHT.md`: selected combat engine, current boundary and integration checks.
- `docs/MOB_CREATION_GUIDE.md`: detailed French mob/animation handoff and implementation guide.
- `docs/examples/mobs`: inactive examples and a non-code mob request form.
- `docs/GIT_WORKFLOW.md`: publication on `test` and user-approved promotion to `main`.

