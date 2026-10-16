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
  - clic droit : une de quatre phrases, à tour de rôle (`message.murimblock.golden_dragon_sage.0` à `.3`), à faire
    relire par la session « lore ».
- **Vérifié** :
  - GameTests `GoldenDragonSageGameTests` (invulnérable, immobile, placé une seule fois sur sa place) ;
  - tour `runBiomeTour` (en monde neuf, présent à sa place, photo `sanctuary-sage.png`).

## À décider

- Ce qu'il fait de plus sous forme humaine (dialogue, épreuve, aura), et ce qui le fait se changer en dragon.
- Son **comportement** : vol lent au-dessus de la carte, repos enroulé sur un sommet, unique par monde, neutre sauf
  attaqué, combat de boss, récompense, réapparition.
- Les **animations** (ondulation en vol), l'**intégration** en jeu (GeckoLib ou Epic Fight, segments du corps animés),
  les yeux (violets ou or blanc).

## Note technique

Blockbench affiche la géométrie Bedrock en miroir sur x : la rotation qui couche l'axe z d'un cube le long d'une
direction d est `[asin(dy/|d|), atan2(dx, dz), 0]` (`along` dans le script, vérifiée sur un modèle de test).
