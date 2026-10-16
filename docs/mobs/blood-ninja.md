# La secte du sang : le Blood Ninja et ses cultistes

> Fusionné dans Murimblock le 16/10/2026 (demande d'Enzo : « ça fait partie du mod »). C'était l'addon séparé Blood Sect
> Decor 1.10.1 de Colleuhu95 ; son code est maintenant dans `com.murimblock.bloodsect`, ses ressources sous `murimblock`.
> Le détail du combat du dao est dans [blood-dao.md](blood-dao.md) ; la base souterraine dans
> [art/structures/blood-cult](../../art/structures/blood-cult/README.md).

## Ce qui est en jeu

- **Blood Ninja** (`murimblock:blood_ninja`) : 24 PV, garde un poste, patrouille autour, poursuit les joueurs en survie
  puis revient ; ignore les joueurs créatifs et spectateurs. 6 points d'expérience. Pas d'apparition naturelle : œuf
  (`murimblock:blood_ninja_spawn_egg`) ou `/summon murimblock:blood_ninja`.
- **Blood Dao** (`murimblock:blood_dao`), réservé au ninja : M1 diagonal animé (4 dégâts, impact à 0,40 s, au moins 1,15 s
  entre deux), **Blood Cloud Step Slash** (préparation 0,50 s, ruée de 5 blocs, 6 dégâts par cible, 4 cibles au plus,
  recharge 5,50 s ; la première cible blessée rend 15 % des PV max). Le dao ne tombe pas à la mort.
- **Sang sombre** (`murimblock:dark_blood`, seau `murimblock:dark_blood_bucket`) : le liquide des bassins et cascades de la
  base, distinct du sang de l'Art Sanguin (`murimblock:blood`). Particule `murimblock:blood_droplet`.
- **Apparence : les cultistes de sang** (16/10/2026). Chaque ninja tire à l'apparition un rang parmi 5 (acolyte, disciple,
  zélote, lieur de sang, ancien) et une armure légère parmi 4, ou aucune (épaulière unique, brigandine, calottes de fer,
  lamelles), sauvegardés (`Outfit`, `Armour`). Les faces de notre modèle sont dessinées sur les parties du modèle du ninja
  (`ReferenceNinjaModel`, `CultistMesh`, `CultistArmourLayer`) : ses poses et ses attaques sont celles d'origine.
  Modèles : `scripts/animation/blood-cultist-3d.cjs`, `blood-cult-armor.cjs` (Blockbench `art/blockbench/blood_cultist*.bbmodel`,
  `blood_cult_armor_*.bbmodel`) ; fichiers de jeu : `blood-cultist-game.cjs`.

## Tests

- GameTests : `com.murimblock.bloodsect` (dao, M1, sang sombre, ninja, apparence), gabarits `blood_sect_*`.
- Vrai client : `gradlew -PgameTests runBloodCloudStepTour`, `runBloodDaoM1Tour`, `runCultistLookTour`.
- Sources de travail de Colleuhu95 (modèles de référence, poses) : `art/blood-sect/`.

## Reste à faire

- Intégration aux règles Murimblock/Epic Fight (parade, Qi, factions, apparitions, butin), boss du culte.
- Les armures des cultistes ne sont pas encore des objets portables ; les pans de tissu ne bougent pas.
- Ancien nom de l'addon dans les mondes de test : `bloodsect:*` n'est pas repris (nouvel espace de noms).
