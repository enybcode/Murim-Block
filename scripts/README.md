# Scripts

Outils de développement. Aucun n'est livré dans le jar du mod.

| Dossier | Rôle | Commande |
|---|---|---|
| `cloud/` | Configuration de Gradle et vérification pour les environnements distants | `scripts/cloud/verify.sh` |
| `worldgen/` | Génère les données des biomes Murim et dessine les textures de la flore et du Jeton de la Secte | voir ci-dessous |
| `concepts/` | Rendus voxel des structures, dans un Blockbench isolé | voir ci-dessous |
| `showcase/` | Montage de la vidéo de présentation | voir ci-dessous |
| `animation/` | Animations de joueur Murim pour Epic Fight (mise à terre, portage, finish) et GIF | voir ci-dessous |

## worldgen

- `node scripts/worldgen/generate-nature.cjs` : régénère tous les JSON des biomes, c'est-à-dire :
  - les états de blocs et les modèles ;
  - le loot, les recettes et les tags ;
  - les arbres et les éléments de génération ;
  - les biomes eux-mêmes.

  Il fusionne la langue au lieu de l'écraser. `vanilla/plains_features.json` donne l'ordre vanilla des étapes de génération.
- `java scripts/worldgen/NatureTextures.java src/main/resources/assets/murimblock/textures/block` : les 72 textures de la flore et des roches.
- `java scripts/worldgen/TokenTextures.java src/main/resources/assets/murimblock/textures/item` : les 32 images du Jeton de la Secte.

## concepts

Ces scripts demandent un Blockbench isolé : `Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<profil vide>`.

- `node scripts/concepts/render-concepts.cjs` : les maquettes des structures de chaque biome, dans `docs/biomes/images/structures-*`.
- `node scripts/concepts/render-proposals.cjs` : les quatre propositions de structures, dans `docs/structures/images`.
- `st-runtime.js` est le kit voxel commun : matériaux, toits chinois, salles, pavillons, arbres, personnages.

## showcase

1. `gradlew -PgameTests runShowcase` filme la vidéo dans le vrai jeu : une image par tick, en 594×1056, dans `build/showcase-run/frames`.
2. `node scripts/showcase/make-video.cjs` (Blockbench isolé sur le port 9232) monte la vidéo. Il ajoute la musique originale synthétisée, le titre et les sous-titres, puis produit `build/showcase-run/video/` :
   - la version principale en 1080×1920 ;
   - une version légère pour Discord ;
   - la musique en WAV.

## animation

- `node scripts/animation/knockdown-animations.cjs [--preview build/anim-preview]` régénère les animations de la mise à terre dans `assets/murimblock/animmodels/animations/biped/murim` :
  - à terre, ramper ;
  - porter (debout, en marchant), être porté ;
  - le finish générique.

  Les poses sont écrites en rotations d'articulations (`biped.cjs` lit le squelette d'Epic Fight dans son jar, depuis le cache Gradle). `--preview` dessine chaque pose en fil de fer, de face et de profil.
- `node scripts/animation/azure-cloud-animations.cjs [--preview build/anim-preview]` : les animations de l'Art de l'Épée du Nuage Azur (pour l'instant, Cloud Step Slash). `pose.cjs` regroupe l'écriture des poses, commune aux deux scripts.
- `java scripts/worldgen/ManualTexture.java src/main/resources/assets/murimblock/textures/item` : la texture du Manuel de l'Épée du Nuage Azur.
- `java scripts/worldgen/TechniqueIcons.java src/main/resources/assets/murimblock/textures/gui/technique` : les icônes 32x32 des techniques (barre de combat, onglet Techniques).
- `java scripts/worldgen/BanditDao.java src/main/resources/assets/murimblock/textures/item` : la texture 16x16 du Dao de bandit (icône d'inventaire, au sol, en cadre).
- `node scripts/animation/bandit-dao-model.cjs` : le Dao de bandit en 3D tenu en main, tranchant en bas (`models/item/bandit_dao_body.json`,
  sa houppe seule `bandit_dao_tassel.json`, les transformations de la main `bandit_dao_held.json`, la texture `bandit_dao_3d.png`,
  `models/item/bandit_dao.json` en transformations séparées) et sa source Blockbench `art/blockbench/bandit_dao.bbmodel`.
  `client/BanditDaoRenderer` dessine la lame puis la houppe, balancée par un pendule (`TasselPendulum`).
- `node scripts/animation/bandit-3d.cjs` : le bandit en 3D sur le squelette biped d'Epic Fight (col croisé, ceinture nouée et ses pans, jupe de robe,
  manches, bandes, sandales ; bandeau, chignon, épaulière, gourde, foulard, capuche de loup, turban, voile, masque selon la tenue) :
  le mesh `animmodels/entity/mountain_bandit.json`, ses cinq atlas `textures/entity/mountain_bandit/3d/` et le projet
  Blockbench `art/blockbench/mountain_bandit.bbmodel`.
- `node scripts/animation/bandit-chief-3d.cjs` : le Bandit Chief en 3D (mesh `animmodels/entity/bandit_chief.json`, atlas
  `textures/entity/bandit_chief.png`, projet Blockbench) ; `node scripts/animation/chief-axe-model.cjs` : sa grande hache ;
  `node scripts/animation/chief-axe.cjs` : ses trois attaques (`animations/biped/murim/chief_*.json`). Voir `docs/mobs/bandit-chief.md`.
  Les modèles 3D de mobs partagent `scripts/animation/mesh-kit.cjs`.
- `node scripts/animation/mob-idles.cjs` : les idles du bandit et du chef (5 s en boucle : respiration, appui, regards ; le bandit
  regarde le fil de son dao, le chef roule la nuque, hache sur l'épaule), écrits avec des rotations par quaternions
  (`pose.cjs` writeSmooth). Les visages bougent par texture : 10 expressions par atlas (clignement, regards, marmonnement,
  colère, cri), peintes par `bandit-3d.cjs` et `bandit-chief-3d.cjs`, choisies à chaque tick par `client/MobFaces`.
- `node scripts/animation/mob-animations-preview.cjs` : toutes les animations du bandit et du chef sur leurs modèles 3D, arme en main,
  en projets Blockbench à regarder (`build/blockbench/*_animations.bbmodel`, onglet Animate). Les mouvements d'Epic Fight y sont lus
  dans son jar : ces projets restent dans `build/`, jamais dans le dépôt.
- `java scripts/worldgen/BanditSkins.java src/main/resources/assets/murimblock/textures/entity/mountain_bandit` : les cinq tenues du bandit en skins 64x64 (rendu vanilla de secours, sans Epic Fight).
- `java scripts/animation/FramesToGif.java <dossier> <sortie.gif> [délai ms] [largeur] [x,y,l,h]` assemble des images en GIF ; `--sheet` fait une planche contact.

Les autres vérifications en vrai client sont des tâches Gradle :
- `gradlew -PgameTests runVisualSmoke`
- `gradlew -PgameTests runBiomeTour` : les 7 biomes et le sanctuaire dans un monde neuf.
- `gradlew -PgameTests runKnockdownTour` : la mise à terre, le portage et le finish avec deux joueurs factices.
- `gradlew -PgameTests runMartialTour` : l'Art de l'Épée du Nuage Azur (manuel, onglet Techniques, Cloud Step Slash au clavier).
- `gradlew -PgameTests runParryTour` : la garde Murim (parade parfaite et normale, coups et vraies flèches).
- `bash scripts/mp/run-mp-tour.sh` : **deux vrais clients** sur un serveur dédié (hors ligne, vol interdit) : MurimA attaque, MurimB encaisse puis garde, chacun avec ses vraies touches, phases annoncées par le serveur dans le chat (`MpTourServer`, `MpTourClient`). Rapports : `build/mp-run/server/mp-tour-server.txt`, `build/mp-run/a|b/mp-tour-client.txt`.
