# Les plantes du Murim

> **État : en jeu le 18/10/2026** (tranche 1 des plantes et de l'alchimie, d'après la proposition
> [plantes-murim-proposition.md](plantes-murim-proposition.md) et les choix d'Enzo). Le chaudron d'alchimie est la tranche
> suivante : pour l'instant, les deux pilules se fabriquent avec les herbes récoltées à la table de craft.

Décisions d'Enzo (17-18/10/2026) :

- les plantes d'abord, le chaudron ensuite (dans le monde, avec un écran de feu) ; **pas de plafond** aux pilules, seule la
  toxicité freine ;
- chaque plante a un **modèle 3D**, en voxels à 16 texels par bloc, inspirés des bons packs de fleurs 3D (Weskerson's
  Nature, Actually 3D Flowers & Plants), et **une icône pixel art dans l'inventaire** ;
- les plantes **s'animent selon leur rareté** (« plus ou moins important en fonction de sa rareté ») ; seules celles du
  jardin et les rares spécimens spirituels sauvages bougent (une plante sauvage ordinaire est un bloc figé : des
  milliers à l'écran, sans coût) ;
- 13 herbes, puis **30 fleurs** de plus, et **ce qui les fait pousser** : graines, bulbe, bouture, rhizome ou spores.

## Les 43 plantes

Liste et ordre : `world/plant/PlantCatalog.java`, écrit par `scripts/plants/plants-data.cjs` (**l'ordre est sauvegardé
dans les mondes : on ajoute à la fin, on ne déplace jamais**).

- **Herbes (13)** : chrysanthème, pousse de bambou, astragale, laîche du Kunlun, fougère karstique, argousier, lotus,
  cistanche, rhododendron, lotus des neiges (déjà dans les biomes) ; ginseng, lingzhi, goji (nouveaux). Récoltées, elles
  donnent leur **herbe** (`HerbItem`), avec sa **maturité** (mature, maturité spirituelle) et sa **pureté** (%).
- **Fleurs (30)**, six par région : pivoine, orchidée cymbidium, camélia, platycodon, lys araignée rouge, lys blanc
  (centre) ; pavot bleu de l'Himalaya, edelweiss, gentiane, myosotis des cimes, saxifrage des neiges, tulipe du Tianshan
  (nord) ; chandelle du désert, iris des sables, œillet de Chine, coquelicot, câprier, harmale (ouest) ; hortensia,
  gardénia, hibiscus, balsamine, hédychie, iris des marais (sud) ; nénuphar, volubilis, hémérocalle, jasmin, adénophore
  (est) ; **udumbara**, la fleur de légende qui ne s'ouvre qu'une fois tous les trois mille ans (partout, très rare).
- **Ce qui les fait pousser (43)** : un sachet de graines teint de la couleur de la fleur pour la plupart ; un bulbe (lys,
  tulipe, lys araignée, orchidée) ; une bouture (les arbustes) ; un rhizome (bambou, iris, hémérocalle, hédychie,
  nénuphar, chandelle du désert) ; des spores (fougère, lingzhi).

## Dans le monde

- **Sauvages** : les 33 nouvelles plantes poussent par touffes dans les biomes de leur région (modificateurs de biomes
  `data/murimblock/neoforge/biome_modifier/plants_*.json`, plus ou moins rares). Les 10 herbes déjà présentes gardent leurs
  touffes. Cueillie, une herbe donne son herbe (et parfois de quoi la replanter) ; aux cisailles, le bloc lui-même. Une
  fleur donne la fleur (posable) et parfois sa graine.
- **Spécimens spirituels** : très rarement, une herbe pousse déjà à maturité spirituelle dans la nature : elle bouge, elle
  brille, des particules montent ; sa récolte est double et très pure.

## Au jardin

- On plante ce qui fait pousser la plante **sur son sol** (terre ; sable ou terre sèche ; terre humide ou vase près de
  l'eau ; roche, gravier ou terre de montagne), ou **sur l'eau calme** (lotus, nénuphar). Un plant (`PlantCropBlock`, un
  seul bloc pour toutes les espèces) a **quatre stades** : semis, jeune, mature, maturité spirituelle.
- Il pousse s'il a de la **lumière**, la bonne **chaleur** (froid pour le nord, chaud pour les terres sèches) et de
  **l'eau** à moins de 4 blocs (pour les plantes humides et d'eau). Une condition faible le ralentit environ trois fois ;
  une condition absente l'arrête. Commune : mature en 25 minutes environ ; rare : environ une heure et demie.
- **Maturité spirituelle** : seulement sur une terre Murim riche en Qi (deux fois plus vite sur un lieu de puissance), en
  bonnes conditions, et sans plus de 8 plants à moins de 4 blocs (le Qi se partage).
- La **pureté** monte à chaque stade poussé en bonnes conditions (50 au semis, au moins 90 à la maturité spirituelle) ; la
  poudre d'os hâte jusqu'à mature, sans pureté, jamais jusqu'au spirituel.
- **Récolter** : clic droit main vide sur un plant mature. Fleurs, baies, feuilles : la plante reste et repart de jeune.
  Racines et pousses : elle est arrachée. Accroupi + clic droit : le plant dit son stade, sa lumière, sa chaleur, son eau
  et sa pureté.

## Les animations, selon la rareté

| Rareté | Mouvement |
|---|---|
| Commune | chaque tige et branche se balance au vent à son rythme, les têtes hochent |
| Peu commune | + les grappes de baies frémissent, les feuilles flottantes dansent |
| Rare | + la fleur respire, une partie luit en pulsant |
| Très rare | + le bord luit plus fort, des grains de Qi montent en permanence |

## Fabrication et vérifications

- Modèles, animations, textures : `scripts/plants/` (`plant-kit.cjs`, `chrysanthemum.cjs`, `herb-models.cjs`,
  `flowers.cjs`), icônes (`herb-icons.cjs`, `flowers.cjs`, `seed-icons.cjs`), tout le reste (`plants-data.cjs`). Les blocs
  sauvages figés sont **cuits** depuis les mêmes modèles GeckoLib (rotations autorisées par Minecraft seulement).
- Visionneuse et films : `scripts/plants/viewer/index.html` (three.js, lit les modèles et animations du jeu).
- Tests : `PlantGrowthTest` (règles), `PlantGameTests` (semer sur son sol, pousser à la lumière et pas dans le noir,
  récolter gardé ou arraché, herbe sauvage, nénuphar sur l'eau). Rendu en vrai client : `gradlew -PgameTests runGardenTour`.

## Plus tard

- Le chaudron d'alchimie (dans le monde, écran de feu) et ses recettes, qui utilisent maturité et pureté.
- Le carnet d'herboriste, les variantes de couleur des autres fleurs, les étapes modélisées une à une pour toutes.
