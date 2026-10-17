# Le Dragon d'Or (concept)

> Demande d'Enzo du 16/10/2026 : « un modèle de dragon géant doré, un mob légendaire unique, comme un dieu qui se
> baladerait sur la carte » ; « il doit être majestueux, le mob ultime », et une forme humaine qui soit « l'entité
> au-dessus de tout ». **En jeu sous sa forme humaine** (assis dans le Sanctuaire de la Falaise des Nuages) ; la forme
> de dragon n'est encore qu'une maquette.

## Ce qui est décidé

- **Construction** : de gros cubes (environ 230), les détails peints dans la texture (carreaux de 64 × 64), pas des
  milliers de petits blocs. Enzo a écarté une version en voxels (trop chargée), des lames en soleil autour de la tête
  (« trop goofy ») et des écailles qui faisaient des briques.
- **Silhouette** : un grand S vertical (tête en haut, corps qui serpente, queue recourbée en touffe), tête massive en
  plaques superposées, gueule ouverte et crocs, grands bois ivoire à trois ramifications, crinière épaisse en grosses
  mèches, longues moustaches fines, quatre pattes griffues avec un bracelet de crinière, écailles de poisson arrondies
  vers le bas, larges plaques de ventre sur le devant.
- **Couleurs : or blanc et jade** (choisies parmi or impérial, or blanc et jade, noir et or, rouge et or) : écailles
  blanc-doré, ventre vert d'eau, crinière jade, bois ivoire. Yeux violets pour l'instant.
- **Taille** : environ 40 blocs de haut dressé, 50 de long en vol (un joueur fait 1,8 bloc).
- **Poses étudiées** : dressé en S, enroulé au sol la tête levée, en vol à l'horizontale.

- **Le modèle** : `scripts/concepts/golden-dragon.cjs` (géométrie Bedrock et atlas 512 × 512). C'est notre propre
  dessin, construit à la manière des dragons en gros cubes qu'Enzo a montrés ; on ne copie aucun modèle trouvé en ligne
  (ni un personnage d'une autre œuvre), pour les droits.

## Sa forme humaine, en jeu (16/10/2026)

Demande d'Enzo : « pour l'instant tu le laisses en forme humaine dans la première structure qu'on a faite ».

- **Entité** `murimblock:golden_dragon_sage` (`GoldenDragonSageEntity`), nommée **The Golden Dragon** ; œuf « Golden Dragon Spawn Egg ».
- **Modèle** sur le squelette humain d'Epic Fight, comme Sensei Yun (`scripts/animation/golden-dragon-sage-3d.cjs` : maillage
  `animmodels/entity/golden_dragon_sage`, texture, projet Blockbench) :
  - couronne d'or sur un chignon piqué de jade, longs cheveux blancs ;
  - grande barbe blanche, yeux jade lumineux ;
  - robe jade profond bordée d'or blanc, un dragon d'or brodé dans le dos, col de fourrure blanche ;
  - ceinture or blanc à fermoir d'or, pendentif de jade blanc.
- **Où** : dans la salle principale du **Sanctuaire de la Falaise des Nuages**, sur le tapis rouge devant l'autel, face
  aux portes (`SanctuaryBuilder.sageSeat`).
  - Placé une seule fois par monde (`SanctuaryPlacement.placeSage`, sauvegardé) : à la fin de la construction d'un
    nouveau sanctuaire.
  - Pour un sanctuaire construit avant, il est placé la première fois qu'un joueur s'approche à moins de 96 blocs.
- **Comportement pour l'instant** :
  - assis en lotus (la méditation des joueurs) ;
  - aucun dégât ne le touche (sauf `/kill`), on ne peut pas le pousser et il ne quitte pas sa place ;
  - il se tourne vers un joueur qui approche et revient face aux portes quand il est seul ;
  - clic droit : ses dialogues (ci-dessous).
- **Vérifié** :
  - GameTests `GoldenDragonSageGameTests` (invulnérable, immobile, placé une seule fois sur sa place) ;
  - tour `runBiomeTour` (en monde neuf, présent à sa place, photo `sanctuary-sage.png`).

## Dialogues et talismans de passage (17/10/2026)

Demande d'Enzo : « qu'il ait plusieurs dialogues et qu'il propose de créer des objets consommables à l'unité pour
changer de dimension, qui coûteraient très cher » ; destinations Nether, End et retour au Murim, retour inclus,
**32 yuanbao d'or** l'objet.

- **Dialogues** (`GoldenDragonSageDialogue`, pages de `DialogueService` comme le vieux maître du monde intérieur) :
  - premières paroles à la première rencontre, puis une salutation du jour (trois, une par jour) ;
  - menu : qui il est (et la rumeur du dragon d'or), ce lieu, comment devenir plus fort (deux pages), les autres
    mondes, ses marchandises, adieu ;
  - les autres mondes mènent à ses talismans, puis à son rouleau de marchandises (ci-dessous).
  - Les textes (`dialogue.murimblock.golden_dragon_sage.*`) sont à faire relire par la session « lore ».
- **Talismans de passage** (`CrossingTalismanItem`) : `nether_crossing_talisman`, `end_crossing_talisman`,
  `murim_crossing_talisman` (rareté épique, empilables par 16 ; icônes `scripts/animation/crossing-talismans.cjs` :
  bandes de papier taoïstes, rouge et or, noire et violet, pâle et jade).
  - Tenu 3 secondes, il emmène dans son monde et retient l'endroit exact d'où l'on part (il brille alors) ; tenu à
    nouveau, il y ramène, puis tombe en cendres. Dans son propre monde, sans avoir servi, il ne fait rien.
  - Arrivées : le Nether à la position divisée par 8, sur un sol sûr (sinon une petite plateforme d'obsidienne) ; l'End
    sur sa plateforme d'obsidienne d'arrivée ; le Murim au lit ou à l'ancre du joueur s'il en a un dans l'Overworld,
    sinon au point d'apparition du monde.
- **Vérifié** : GameTests `CrossingTalismanGameTests` (dialogue jusqu'au rouleau, refus sans or, prix pris sur deux
  piles ; aller au Nether sur un sol sûr, retour au point exact, talisman en cendres).

## Rouleau de marchandises et guides des arts (17/10/2026)

Demande d'Enzo : « il peut aussi donner des livres pour guider le joueur vers les arts martiaux, un par art, vendus
selon le degré de difficulté », et un écran de vente « beau, en mode le rouleau dans Kung Fu Panda ». Maquette
validée le 17/10 (« parfait »), icônes des livres refaites en pixel art à sa demande.

- **Écran** (`client/ScrollShopScreen`, textures peintes par `scripts/animation/ScrollShopArt.java`) : rouleau
  suspendu qui se déroule depuis son rouleau du haut (12 ticks), brocart rouge à frise dorée, papier de riz vieilli
  avec montagnes dans la brume et dragon d'or pâle, sceau 龍 ; titre « Treasures of the Golden Dragon » ; une ligne par
  marchandise : icône, nom, difficulté en gouttes d'encre rouge, prix en lingots d'or, sceau rond 購 pour acheter (vif au
  survol, gris si la bourse ne suffit pas) ; bourse en bas ; molette pour faire défiler (6 lignes visibles). Police
  manuscrite, mise à l'échelle si l'écran est petit.
- **Vente** (`mob/GoldenDragonShop`, paquets `ScrollShopPayloads`, protocole réseau **26**) : seulement depuis un
  rouleau ouvert, le Dragon à moins de 8 blocs ; prix vérifié et pris côté serveur.

| Ligne | Difficulté | Prix (yuanbao d'or) |
|---|---|---|
| Guide : Long Fist | 1 | 3 |
| Guide : Golden Bell | 1 | 3 |
| Guide : Traveller's Staff | 1 | 3 |
| Guide : Celestial Peak Spear | 2 | 6 |
| Guide : Azure Cloud Sword | 3 | 10 |
| Guide : Mountain-Shattering Fist | 4 | 16 |
| Guide : Seven Stars Sword | 4 | 20 |
| Guide : Majin Fist | 4 | 20 |
| Guide : Purple Heaven Thunder Saber | 4 | 24 |
| Sanguine Art | — | « Not yet written » (personne ne garde cet art : pas en vente) |
| Talismans Nether, End, Murim | — | 32 chacun |

- **Guides** (`item/MartialGuideItem`, `<art>_guide`, icônes 16×16 en pixel art : livre fermé de la couleur de l'art,
  emblème doré poing, cloche, bâton, lance, épée, montagne, étoile, soleil ou éclair) : lu, il s'ouvre en livre de 3 pages (l'art, qui le garde
  et comment l'obtenir, le chemin) et écrit dans le chat la direction et la distance du lieu le plus proche : avant-poste
  de l'Alliance (Poing Long, Bâton : marchand), arène de l'Alliance (capitaine), temple de la Mer de Bambous (Sensei Yun ; le moine de la Cloche d'Or),
  grand camp de bandits (le Seigneur de guerre puis le Fils du Démon, qui porte les rouleaux du Poing Brise-Montagne et du Poing du Majin), sommet du Pic Cueille-Étoiles (Sept Étoiles). Le guide du Sabre du Tonnerre ne mène à aucun lieu (le manuel est
  donné par la foudre, à un paratonnerre sous l'orage, dès Qi Crystallization) : il écrit les conditions à la place. N'enseigne rien, jamais
  consommé ; recharge de 5 s.
- Les pages (`guide.murimblock.*`) sont à faire relire par la session « lore ».
- **Vérifié** : GameTests `SageShopGameTests` (tous les arts sauf le Sanguinaire ont leur guide, prix croissants avec la
  difficulté, ligne sanguinaire invendable, refus sans rouleau ouvert, de loin, hors liste ; achat ; guide lu et gardé),
  tour en vrai client `runSageShopTour` (déroulé, achat par le sceau, défilement, échelle 2, pages du guide).

## À décider

- Ce qu'il fait de plus sous forme humaine (dialogue, épreuve, aura), et ce qui le fait se changer en dragon.
- Son **comportement** : vol lent au-dessus de la carte, repos enroulé sur un sommet, unique par monde, neutre sauf
  attaqué, combat de boss, récompense, réapparition.
- Les **animations** (ondulation en vol), l'**intégration** en jeu (GeckoLib ou Epic Fight, segments du corps animés),
  les yeux (violets ou or blanc).

## Note technique

Blockbench affiche la géométrie Bedrock en miroir sur x : la rotation qui couche l'axe z d'un cube le long d'une
direction d est `[asin(dy/|d|), atan2(dx, dz), 0]` (`along` dans le script, vérifiée sur un modèle de test).
