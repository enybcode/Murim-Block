# La Lance du Seigneur Conquérant (霸王枪, *Overlord Spear*)

> **État : en jeu sur `test` depuis le 18/10/2026** (conception d'Enzo, « on va reprendre ça »), à valider en jeu. Voir « Ce qui est en jeu » à la fin.
> Arme : la Lance de la Ruine (`MurimItems.SPEAR_OF_RUIN`). Demandes d'Enzo : « un art martial intermédiaire à la lance », « qu'il ait une certaine
> puissance destructrice » ; source : « après avoir monté la technique de la lance du boss de l'arène, maîtrise 50 ».
> Noms en jeu en anglais.

Inspirée de la lance de Xiang Yu, le « Seigneur conquérant » de la Chine ancienne : on ne pique pas, on **pulvérise**.
C'est un art intermédiaire : il s'apprend à **Qi Condensation**.

## Où le trouver

Quand la **maîtrise de la Lance du Pic Céleste** (l'art du capitaine de l'arène) atteint **50**, le joueur reçoit le
manuel de la Lance du Conquérant.

## Passif « Conqueror's Weight »

- Chaque coup qui touche ajoute une charge de **Domination** (jusqu'à 5).
- À 5 charges, le M1 suivant devient un **coup de bélier** : la lance s'enfonce, une onde de choc fend le sol devant soi
  et repousse tout sur 4 blocs.
- Ce coup fait **×1,8** dégâts et **brise la garde**.

## Les 5 techniques

| # | Nom | Effet |
|---|---|---|
| 1 | **Tyrant's Thrust** | Estoc chargé (maintenu). La pointe libère un rayon de Qi qui perce **8 blocs** en ligne droite et traverse tout le monde. |
| 2 | **Earth-Splitting Sweep** | Balayage à 360° frappé au sol. Le sol se fissure en anneau, les ennemis sont projetés en l'air. |
| 3 | **Falling Mountain** | Saut très haut, puis on plante la lance en retombant. Cratère de 5 blocs, onde de choc, les blocs volent puis reviennent. |
| 4 | **Overlord's Charge** | Charge de 10 blocs qui embarque tous les ennemis sur la pointe, puis les écrase contre le premier mur ou les relâche au bout. |
| 5 | **Hundred Battles Rampage** | Rafale de 12 estocs en 2 secondes, chacun avec une petite explosion de Qi. Le dernier coup éjecte. |

- **Secret « Breaking the Cauldrons »** : utiliser Tyrant's Thrust sous **20 % de vie**. Le rayon devient énorme
  (3 blocs de large) et fait **×2**.
- **Ultime « Conqueror Descends »** : une lance de Qi géante (×20) tombe du ciel sur la zone visée. Explosion de
  10 blocs, cratère temporaire, tout est projeté.
- **Finish** : il soulève l'adversaire à terre au bout de la lance, puis le plante dans le sol au milieu d'une onde de
  choc.

## Effets visuels (spectaculaires)

- un modèle GeckoLib pour le rayon de Qi et pour la lance géante ;
- des fissures au sol et des blocs qui volent puis reviennent (la mécanique existe déjà avec le Poing Brise-Montagne) ;
- de la poussière, des ondes ;
- des lumières Photon rouge sang et or.

## Arme dédiée

La **Lance de la Ruine** (*Spear of Ruin*, choisie par Enzo parmi trois concepts) : acier noir fendu de veines de Qi qui
brillent dans le noir, trois griffes d'or, crinière rouge, au moveset de lance d'Epic Fight. On peut aussi jouer l'art
avec n'importe quelle lance.

## Chiffres de départ (à régler en jeu)

Pour coder, comme les autres arts intermédiaires ; les dégâts suivent l'arme et le Qi (`TechniquePower`), sans one-shot
entre joueurs (`BurstGuard`).

| Technique | Qi | Recharge | Dégâts (× l'attaque) |
|---|---|---|---|
| Tyrant's Thrust | 35 | 8 s | ×1,5 sur la ligne |
| Earth-Splitting Sweep | 50 | 12 s | ×1,2 dans 4 blocs |
| Falling Mountain | 60 | 15 s | ×1,8 au centre, ×1,0 dans 5 blocs |
| Overlord's Charge | 55 | 14 s | ×1,0 en route, ×1,6 contre un mur |
| Hundred Battles Rampage | 75 | 22 s | 12 × 0,35, le dernier ×1,0 |
| Conqueror Descends (ultime) | 180 | 4 min | ×3,0 au centre, ×1,5 au bord |

## Ce qui est en jeu (18/10/2026)

| Élément | Code | Vérifié par |
|---|---|---|
| L'art, ses 5 techniques, l'ultime, le secret (sans quêtes : le rouleau enseigne tout sauf le secret) | `MartialArts` (`OVERLORD_SPEAR`) | `OverlordSpearGameTests` (9 tests) |
| Le rouleau donné à la maîtrise 50 de la Lance du Pic Céleste, une fois ; lisible dès Qi Condensation | `OverlordSpear.celestialMastery`, `MartialProgress.addMastery` | idem |
| Le passif (charges de Domination, bélier ×1,8, garde brisée, onde de 4 blocs), Tyrant's Thrust tenu (barre de combat) et son rayon, le secret, le balayage, la montagne, la charge, la furie, l'ultime, le finish | `martial/OverlordSpear` | idem, et `gradlew -PgameTests runOverlordTour` (film en vrai client) |
| Les animations (lance à deux mains, d'après les poses de la Lance du Pic Céleste) | `scripts/animation/overlord-spear.cjs`, `MurimAnimations.buildOverlordSpear` | idem |
| Le rayon de Qi et la lance géante (modèles GeckoLib qui brillent) | `martial/OverlordQiEntity`, `client/OverlordQiRenderer`, `scripts/animation/overlord-fx.cjs` | idem |
| Les flashs rouge sang et or (Photon), protocole réseau 30 | `martial/OverlordFx`, `client/OverlordFxClient`, `OverlordFlashPayload` | idem |
| Le terrain cassé (fissures, cratères, mur percé) se reconstruit toujours | `combat/ShatteredGround` | idem |
| Les icônes (famille noir et rouge sang), le rouleau, l'aura (braises), le guide du Dragon d'Or (4 gouttes, 18 yuanbao) | `TechniqueIcons.java`, `ScrollTexture.java`, `AuraClient`, `GoldenDragonShop` | `SageShopGameTests` |
| La Lance de la Ruine | `MurimItems.SPEAR_OF_RUIN`, `scripts/animation/ruin-spear-model.cjs` | `runRuinSpearTour` |

Pas encore vu : le finish entre deux vrais joueurs, l'équilibrage en vrai combat.
