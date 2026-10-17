# Antre du Roi Crabe : le Puits aux racines

> Demande d'Enzo (15/10/2026) : « une grande arène dans une grotte, un boss secret qui donne un art martial unique, une
> seule arène par monde ; si on le tue on drop le parchemin de son art ; dans le marécage (le Jiangnan Waterlands),
> immense, avec un bateau au fond à moitié immergé dans la boue ; il aura de très grosses attaques de zone ». Le boss
> fait 4 blocs de haut : bonne hauteur sous plafond. Bien éclairé, pour voir le combat.
> Maquette retenue : **C, le Puits aux racines** (trois proposées : la Fosse engloutie, la Nef de la jonque, le Puits).

## Où

- **Une seule par monde**, construite au premier démarrage, après le sanctuaire (`CrabLairPlacement`, sauvegarde
  `murimblock_crab_lair`).
- Sous le cœur du premier **Jiangnan Waterlands** trouvé, à plus de 800 blocs du point d'apparition. La recherche se
  fait par anneaux d'échantillons, tous les 32 blocs, jusqu'à 6 000 blocs de son point de départ.
- Sur la carte Murim, le Jiangnan n'existe que dans la **région Est**. La recherche part donc du point d'apparition s'il
  est dans l'Est, sinon à 3 000 blocs à l'intérieur de l'Est, sur le chemin qui y mène depuis le point d'apparition.
- Pas de Jiangnan trouvé : pas d'antre dans ce monde (c'est noté une fois, la recherche ne recommence pas).
- Pas dans les mondes plats ou de test.
- Rien ne le signale en surface, sinon le puits lui-même : un trou cerclé de racines de mangrove et de boue au milieu
  du marais.

## Forme (`CrabLairBuilder`)

- **Grotte ronde de 88 blocs** de large, son sol 46 blocs sous le marais.
- **Hauteur sous plafond** : 42 blocs au centre, plus de 25 blocs sur toute la zone de combat. Les stalactites ne
  descendent jamais à moins de 22 blocs du sol, et les racines à moins de 20.
- **Parois** : deepslate, tuf, deepslate pavé et mousse, bruitées, sur deux blocs d'épaisseur pour sceller la grotte.
- **Sol** : boue, racines boueuses de mangrove dans les creux, flaques d'eau.
- **Le puits** : 8 blocs de rayon, ouvert jusqu'au marais.
  - Des racines de mangrove pendent dedans, avec des racines suspendues au bout.
  - Des vignes sur ses parois permettent de remonter.
  - Dessous, une **mare de 4 blocs de profondeur** : on peut sauter depuis la surface.
- **La jonque**, brisée en deux contre la paroi ouest :
  - la poupe est couchée et à moitié enfoncée dans la boue ;
  - la proue est dressée hors de la boue ;
  - coque en chêne noir avec un liseré de mangrove, pont en épicéa ;
  - château arrière en calcite, poteaux de mangrove écorcée, toit de tuiles de deepslate ;
  - trois mâts, voiles de laine blanche déchirées, lanternes.
- **Dix nids** sur le pourtour : os, terre cuite rouge et brune.
- **Lumière**, pour bien voir le combat :
  - froglights ocre incrustées dans les parois, sur deux anneaux ;
  - lanternes pendues à des chaînes au plafond ;
  - shroomlights dans les nids ;
  - lichen lumineux sur les parois ;
  - blocs de lumière invisibles de niveau 14 au-dessus de tout le sol, si bien qu'aucun autre monstre n'y apparaît.

## Occupants

- Le **Roi Crabe**, à côté de la mare, face au puits.
- Quatre **crabes cuirassés** autour de lui.
- Tous sont persistants.
- Son rouleau d'art (l'art du crabe, pas encore fait) sera dans son butin (`loot_table/entities/crab_king.json`, session
  « armes »).

## Maquette (`scripts/concepts/crab-cave.js`)

- `node scripts/concepts/render-crab-cave.cjs [--only=c_open,c_inside] [--port=]` rend les vues voxel (1 voxel =
  1 bloc) dans un Blockbench isolé ;
- propositions A (`a_open`, `a_inside`, `a_surface`), B (`b_*`) et C (`c_*`).

## Vérifications

- **`CrabLairGameTests`** construit l'antre entier dans la roche, loin des autres tests, puis vérifie :
  - la hauteur libre au-dessus de l'arène ;
  - le sol, le puits ouvert et la mare profonde ;
  - la jonque ;
  - le roi et ses 4 crabes ;
  - au moins 10 de lumière partout sur le sol.
- **`gradlew -PgameTests runCrabLairTour`** : dans un vrai monde Murim, le jeu place l'antre lui-même, puis la visite
  vérifie qu'il est sous le Jiangnan et que le roi y est, et prend six vues (`build/crab-lair-tour-run/screenshots`).
