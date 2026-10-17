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

## À faire, dans l'ordre

1. La surcouche des deux formes en jeu, à partir du modèle fusionné.
2. La barre de rage dans le HUD.
3. La transformation et le retour.
4. Les M1 de chaque forme.
5. Les 8 techniques et leurs 8 versions du démon, avec leurs chiffres (alignés sur
   [../technique-balance.md](../technique-balance.md) et le Sanguine Art).
6. Le parchemin, et l'endroit où on le trouve.

## Encore à décider par Enzo

- Le nom de l'art (proposé : *Devouring Demon Art*) et de la barre (proposé : *Fury*).
- Le royaume requis (proposé : Foundation Establishment).
- Où trouver le parchemin.
- La transformation : automatique quand la barre est pleine (proposé), ou déclenchée par le joueur.
