# Crab King (蟹王)

Boss de l'art du crabe (15/10/2026, demande d'Enzo) : un artiste martial que la maladie des hommes-crabes a pris tout
entier. Il gardera une structure qu'Enzo construira ; en attendant, pas d'apparition dans le monde : œuf
(`Crab King Spawn Egg`) ou `/summon murimblock:crab_king`. Il ne disparaît jamais.

Il lâchera le rouleau de l'art du crabe, et se battra avec ses techniques, quand l'art sera fait (les techniques sont
à choisir avec Enzo). Son allure suit le concept retenu, stade 3 « Roi crabe »
(`scripts/concepts/crab-art-pixel.cjs`, `docs/combat/styles/images/crab-pixel-*`).

![De face](images/crab_king-front.png) ![De dos](images/crab_king-back.png)

## Allure

Deux fois plus grand qu'un homme (15/10, demande d'Enzo) : 4 blocs de haut pour le corps, la carapace et ses pointes
au-dessus (boîte 1,2 × 3,9). Modèle 3D sur le squelette biped d'Epic Fight : 557 cubes peints texel
par texel (deux texels par pixel), tournés dans tous les sens pour que rien ne fasse gros bloc.

- chair à vif rouge sombre, veines noires, plaques striées sur le torse et le ventre, pustules ocre ;
- carapace de croûte brune en dôme à paliers sur le dos et sur la tête comme une capuche, bosselée et hérissée, son
  rebord déchiqueté au-dessus du visage ;
- yeux bleus sur pédoncules sous le rebord ; le visage creusé et sombre, mandibules à la bouche ;
- épaules prises dans la croûte ; avant-bras enflés et longues pinces courbes jusqu'au sol, la droite plus grosse. Le
  doigt extérieur est sur l'avant-bras (`Hand_*`), le doigt intérieur sur la main (`Tool_*`) : tourner la main ouvre la pince ;
- une seconde paire de bras fins à petites pinces ; genoux croûtés, pieds griffus ;
- ce qui reste de sa robe : une ceinture sombre et son pendentif de jade, un ourlet déchiré sur les cuisses.

## Combat

- 260 PV, armure 14 (robustesse 4), recul résisté à 90 %, armure de stun 14, attaque 11 (ses pinces). Lent (0,23).
- Toutes ses attaques sont lentes à venir et **parables** :
  - **Fauchage** (`crab_king_sweep`, ×1,0, plusieurs cibles) : la grosse pince droite, ouverte, armée à droite puis
    balayée à plat devant lui, en claquant (≤ 5 blocs) ;
  - **Claquement** (`crab_king_snap`, ×1,3, étourdit) : les deux pinces levées grandes ouvertes, puis une fente, les deux
    lancées en avant et refermées (2 à 5,8 blocs) ;
  - **Écrasement** (`crab_king_slam`, ×1,5, met à terre) : les deux pinces levées très haut, puis abattues au sol devant
    lui ; à 0,74 s, onde de choc de 4,6 blocs autour du point d'impact (0,6 × l'attaque, projette, épargne ses crabes).
    Un seul coup : qui vient de prendre ou de parer les pinces n'est pas touché par l'onde. Recharge 8 s.
- **Prise dans la grosse pince** (15/10, demande d'Enzo ; `crab_king_grab` puis `crab_king_grab_hold`, 1,5 à 5 blocs,
  recharge 11 s) : il arme la pince droite grande ouverte et se fend ; à 0,55 s elle se referme sur ce qui est devant lui
  (0,3 × l'attaque). Ce qu'elle attrape est soulevé haut devant lui, serré trois fois (0,25 × l'attaque chaque fois),
  levé au-dessus de sa tête puis abattu au sol devant lui à 1,62 s de la prise (0,8 × l'attaque, mis à terre). Une garde
  normale ne l'arrête pas ; une **parade parfaite** au moment où la pince se ferme la fait rater ; on peut l'esquiver.
  **Tenu, un joueur ne peut rien faire** (16/10, demande d'Enzo) : ni bouger, ni tourner, ni attaquer, parer, rouler,
  utiliser une compétence, un objet ou une technique Murim, ni méditer, ni changer d'objet en main ; il se débat
  (`crab_king_held` : bras plaqués, jambes qui battent, tête renversée). Il reste libre de regarder autour de lui.
  Interrompu (étourdi, tué, déchargé), il lâche ce qu'il tient : sa victime est libérée par un court étourdissement. Pas de boîte d'Epic Fight : la prise est celle de `CrabKingEntity`.
- **Rage** : sous la moitié de sa vie, une seule fois (sauvegardé), il rugit (`crab_king_roar`) et deux crabes cuirassés
  sortent du sol à ses côtés, sur sa cible.
- Un crabe : il respire sous l'eau (tag `minecraft:can_breathe_under_water`) et va plus vite dans l'eau.
- Touché, il appelle les crabes et hommes-crabes autour de lui. Barre de boss rouge pour les joueurs proches.
- Au repos, son idle (`crab_king_idle`) : voûté, pieds écartés, les pinces basses en avant ; respiration lente, regards
  à gauche et à droite, deux claquements de pinces.
- Sa marche à lui, lourde (`crab_king_walk`, deux pas en 1,6 s, aussi pour la poursuite) : chaque pied planté, le corps
  qui s'enfonce dessus, le poids qui passe d'un côté à l'autre, les épaules qui tournent contre les hanches, les pinces
  basses qui se balancent. Chaque pas s'entend (pas de ravageur, grave) et soulève un peu le sol.
- Butin (provisoire, avant le rouleau) : 2 à 4 morues ; tué par un joueur, 4 à 8 taels d'argent et un trident une fois
  sur dix. 60 XP, 30 Qi (+100 à la première victoire, +30 Qi Max). Le sens du Qi lit en lui Condensation du Qi, stade tardif.

## Fichiers

- `scripts/animation/crab-king-3d.cjs` : le modèle (mesh `animmodels/entity/crab_king.json`, atlas
  `textures/entity/crab_king.png`, projet `art/blockbench/crab_king.bbmodel`, ses cubes tournés y sont droits) ;
- `scripts/animation/crab-king-moves.cjs` : ses animations (`animations/biped/murim/crab_king_*.json`, projet
  `art/blockbench/crab_king_moves.bbmodel`) ;
- `scripts/animation/mesh-preview.cjs crab_king` : rendu du mesh tel qu'écrit, au repos (`images/crab_king-*.png`) ;
- Java : `mob/CrabKingEntity`, `integration/epicfight/CrabKingPatch`, `CrabKingAnimations`, `client/CrabKingRenderer` ;
  butin `loot_table/entities/crab_king.json`.

## Vérification

- `CrabKingGameTests` : boss à lui (patch, idle, marche lourde, 4 blocs de haut, 260 PV, armure, ne disparaît pas,
  respire sous l'eau, barre de boss) ; rage (deux crabes, rugissement, une fois) ; onde de choc ; la prise (attrape,
  soulève, abat au sol, blesse), la prise dans le vide, un joueur tenu qui ne peut rien faire (entravé, verrous
  d'Epic Fight, technique refusée, attaque annulée) et libéré après l'abattage ou à la mort du boss ; il se bat avec ses pinces et touche.
- En vrai client : `MURIM_TOUR_ONLY=king gradlew -PgameTests runWeaponTour` (debout de chaque côté, idle, marche, ses trois
  attaques, sa prise sur un zombie, son rugissement, sa mort).
