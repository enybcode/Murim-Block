# L'Art du Démon Dévoreur (噬魔功, *Shì Mó Gōng*)

> **État : conception validée par Enzo le 18/10/2026 (« parfait »), rien en jeu.** Inspirations : la kagune de Kaneki
> (Tokyo Ghoul), le haki des rois (One Piece), la furie de Kyubi contre Pain (Naruto). Ce sont des idées, pas des copies.
> Art de haut niveau, 8 techniques. Les noms en jeu sont en anglais.
>
> Demandes d'Enzo qui fixent la conception :
> - « à 0 de rage on a les 4 kagune et à 100 on ressemble à cette image (même système que les cristaux de sang : ça va
>   sur le skin, pas une armure), donc ça change même la kagune » ;
> - « 8 attaques sans rage, et elles changent avec la barre pleine ».

## Les deux formes (maquettes faites)

- **Forme normale** (0 à 99 de rage) : **quatre tentacules** rouges à écailles qui sortent du bas du dos.
- **Forme du démon** (à 100 de rage) : **deux scolopendres** violettes à joints rouges, articulées segment par segment,
  avec des crochets sur chaque segment, et un **demi-masque** violet sur le visage.
- Les deux sont une **surcouche sur le skin**, attachée au squelette d'Epic Fight, comme les cristaux de sang du Sanguine
  Art : ni armure, ni objet. Le skin et l'équipement du joueur restent visibles.
- **Le modèle** : `art/blockbench/kagune_forms.bbmodel` (`node scripts/concepts/kagune-forms.cjs`), qui fusionne
  `kagune_preview.bbmodel` et `kakuja_preview.bbmodel` (`node scripts/concepts/kagune-preview.cjs [--form=kakuja]`). Il
  contient trois animations :
  - `idle_hunger` : les tentacules au repos ;
  - `transform` : il se courbe, les tentacules rentrent dans le dos, les scolopendres jaillissent segment par segment, il
    rejette la tête en arrière et le masque apparaît (2,6 s) ;
  - `idle_demon` : les scolopendres au repos.
- **Vérifié** : à chaque image des deux idles, aucun bloc de kagune ne passe sous le sol ni dans le corps. La
  transformation n'est pas encore vérifiée.
- **En jeu** (`/kagune off|hunger|transform|demon`, opérateur) : `martial/KaguneState`, `client/KaguneOverlay`, physique
  de queue `client/KaguneTail`. Après la transformation, il est **à quatre pattes** (`demon-beast-gait.cjs`, tête levée) :
  hitbox de 0,9 × 1,15 bloc (yeux à 0,95), **+40 % de vitesse** (`KaguneState.Beast`). Chaque chaîne de la kagune est
  relevée à sa racine, image par image, pour rester à 0,28 bloc au-dessus du sol (`KaguneModel.keepAboveGround`).
  Vérifié en vrai client : `MURIM_KAGUNE=1 MURIM_KAGUNE_FILM=beast gradlew -PgameTests runWeaponTour`.

## Comment ça marche

- **La touche de l'art** sort la kagune (0,5 s) ou la range. Rangée, l'art ne fait rien. Sortie, les M1 sont faits par
  les tentacules, les mains vides, à 4 blocs.
- **Les 8 techniques ne coûtent pas de rage** : on peut toutes les lancer à tout moment (avec du Qi).
- **La barre de rage** (rouge, à côté de celle du Qi, sur 100) **monte** quand les M1 et les techniques touchent, et
  quand on reçoit des coups. Elle redescend hors combat. Plus elle monte, plus la kagune s'agite.
- **Barre pleine : la transformation.** Les tentacules deviennent les scolopendres, et **les 8 techniques changent**, sur
  les mêmes touches, ainsi que les M1. Il gagne en dégâts et en résistance, mais ne peut plus parer.
- **La forme du démon** dure tant qu'il reste de la rage : la barre descend, chaque coup qui touche en rend un peu. À 0,
  il revient à la forme normale, avec un court épuisement.

## Les 8 techniques (validées)

| # | Forme normale (les tentacules) | Forme du démon (les scolopendres) |
|---|---|---|
| 1 | **Lash** (Le Fouet) : les 4 tentacules fouettent en éventail | **Centipede Bite** (La Morsure) : une scolopendre fond sur la cible et la mord |
| 2 | **Impaling Thorns** (Les Pieux) : des tentacules jaillissent du sol sous la cible | **Burrow** (Le Fouissage) : les scolopendres plongent sous terre et ressortent sous les ennemis |
| 3 | **Grapple** (Le Grappin) : il s'accroche à un mur ou tire un ennemi à lui | **Constrict** (L'Enlacement) : une scolopendre s'enroule autour d'un ennemi et le broie |
| 4 | **Cage** (La Cage) : les tentacules enferment la cible | **Spiral** (La Spirale) : les deux scolopendres tournent autour de lui et broient tout à 6 blocs |
| 5 | **Cocoon** (Le Cocon) : il se protège dans les tentacules et renvoie les coups | **Carapace** (La Carapace) : les scolopendres s'enroulent autour de lui, puis se déroulent d'un coup |
| 6 | **Sovereign's Will** (La Volonté du Souverain), haki : les plus faibles tombent à genoux | **Demon's Roar** (Le Rugissement du Démon), haki : à genoux jusqu'à un royaume en dessous, sur 30 blocs |
| 7 | **Black Lightning Mantle** (Le Manteau d'éclairs noirs), haki dans la kagune : les coups portent plus loin et traversent la garde | **Black Storm** (L'Orage noir) : les scolopendres se plantent dans le sol et lancent des éclairs autour |
| 8 | **Burst** (L'Éclatement), l'ultime : une explosion de chair autour de lui | **Demon's Maw** (La Bouche du Démon), l'ultime : les deux scolopendres forment une gueule qui crache une sphère qui explose |

**Le haki** (techniques 6 et 7) agit selon la différence de royaume, lue comme la perception du Qi : deux royaumes en
dessous, la cible tombe à genoux ; un royaume, elle est écrasée (ralentie, affaiblie) ; à royaume égal, elle est seulement
oppressée ; plus fort que le joueur, rien. Les effets : le temps se fige une demi-seconde, des éclairs noirs et rouges,
l'écran de la victime se décolore et l'on entend un battement de cœur.

## En jeu (18/10/2026, sur `test`)

- **L'art** : `MartialArts.DEVOURING_DEMON` (royaume proposé : Foundation Establishment, `DEVOURING_DEMON_REALM`), mains
  nues, passif `devouring_hunger` (les coups à mains nues d'Epic Fight pour l'instant). Logique serveur :
  `martial/DevouringDemon` ; état synchronisé : `martial/KaguneState` (forme, rage, coup de kagune en cours).
- **La kagune sort toute seule** en mode combat, art principal, mains vides (pas de touche pour l'instant) ; une arme en
  main ou la sortie du mode combat la range, et la rage est perdue.
- **La rage (Fury)**, sur 100 : +4 par coup porté, +3 par coup reçu (une fois par tick) ; après 6 s sans rien, −2 par
  seconde. À 100, **transformation automatique** (2,6 s, aucune technique pendant), puis forme du démon à quatre pattes :
  dégâts ×1,25, dégâts reçus ×0,75, −3 de rage par seconde, +2 par coup porté. À 0 : retour aux tentacules, essoufflé
  4 s (lenteur II, faiblesse). La forme du démon tient hors combat jusqu'à épuisement ; un K.O. ou une arme la finit.
- **Le HUD**, combiné à la barre de combat (`CombatBarClient.furyRing`) : un anneau de sang autour du dantian, qui monte
  des deux côtés depuis le bas et se ferme en haut à 100 ; au-delà de 75 il bat comme un cœur ; en forme du démon il
  devient violet veiné de rouge, qui coule, et le cerclage d'or du dantian passe au cramoisi ; les icônes des
  techniques montrent leur version du démon.
- **Lash** (160 Qi, 5 s) : les quatre tentacules se dressent derrière ses épaules, ramassées comme des queues de
  scorpion, puis se projettent l'une après l'autre, de droite à gauche, chacune vers son point de l'arc devant lui
  (4,5 blocs, 150°), et s'abattent dessus ; coup ×1,3 sur chaque ennemi, repoussé. Corps : `demon_lash`. Revue par Enzo
  le 18/10 (la morsure validée telle quelle).
- **Centipede Bite** (la même touche en forme du démon) : une scolopendre se dresse comme un cobra, puis frappe en arc et
  retombe sur l'ennemi regardé (7 blocs, cône de 22°), coup ×2,2, ralenti, attiré ; il dévore un cinquième de la
  blessure en vie. Corps : `demon_bite`, à quatre pattes.
- **Le mouvement des chaînes** (`client/KaguneStrikes`, `KaguneModel.aim`) est calculé dans le jeu, par-dessus l'idle,
  pour viser là où il regarde (espace modèle : devant −z, sa droite +x, vérifié en jeu) ; la morsure fait un arc dont la
  corde vaut la distance de la cible : la tête retombe dessus.
- **Le style** (Enzo, 18/10/2026 : « le joueur ne doit presque rien faire, la kagune doit s'étendre et frapper, pas
  trop de particules à part à l'impact ») : le corps bouge à peine (`demon_lash`, `demon_bite`) ; la kagune se ramasse,
  courte, puis s'étire (ses joints s'écartent, ses segments s'allongent : jusqu'à ×1,45 pour les tentacules, ×1,35
  pour la scolopendre qui va chercher une cible lointaine). Rien pendant le coup ; à l'impact seulement
  (`client/KaguneImpactFx`, `network/KaguneImpactPayload`, protocole 31) : une lumière Photon rouge (et violette pour la
  morsure), une gerbe de sang qui part à l'opposé du frappeur et retombe au sol, la vue secouée (fort pour la victime,
  léger pour le frappeur) et un arrêt sur le coup (hit-stop) de la kagune.
- **Chiffres** : +5 de « kagune » sur la main nue des techniques (`TechniquePower.DEMON_TRAINING`).
- **Tests** : `DevouringDemonGameTests` (la kagune sort, la rage monte, transformation, retour, Lash dans l'arc, morsure
  sur la cible regardée). Film en vrai client (HUD, Lash, transformation, morsure ; jeu ralenti à 5 ticks/s pour que
  chaque tick ait son image) : `MURIM_KAGUNE=1 MURIM_KAGUNE_FILM=art gradlew -PgameTests runWeaponTour`.
- **Icônes** : `scripts/worldgen/TechniqueIcons.java`, famille « demon ».

## À faire, dans l'ordre

1. ~~La surcouche des deux formes~~, ~~la barre de rage dans le HUD~~, ~~la transformation et le retour~~ : faits.
2. Les 7 autres techniques et leurs versions du démon (Impaling Thorns / Burrow, Grapple / Constrict, Cage / Spiral,
   Cocoon / Carapace, Sovereign's Will / Demon's Roar, Black Lightning Mantle / Black Storm, Burst / Demon's Maw).
3. Les M1 de chaque forme (par les tentacules, à 4 blocs) ; pour l'instant ceux d'Epic Fight à mains nues.
4. La forme du démon ne doit plus pouvoir parer (pas encore bloqué).
5. Le parchemin, et l'endroit où on le trouve.

## Encore à décider par Enzo

- Le nom de l'art (proposé : *Devouring Demon Art*) et de la barre (proposé : *Fury*).
- Le royaume requis (proposé : Foundation Establishment).
- Où trouver le parchemin.
- La transformation : automatique quand la barre est pleine (fait ainsi, à confirmer), ou déclenchée par le joueur.
- La kagune : sortie d'elle-même en mode combat (fait ainsi, à confirmer), ou par une touche.
