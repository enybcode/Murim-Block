# Marchand de l'Alliance (盟商) — *Alliance Merchant*

Marchand de l'Alliance orthodoxe, demandé par Enzo le 15/10/2026. Il commerce en **taëls d'argent** (*Silver Tael*,
银两), la monnaie du Murim ajoutée avec lui. Pas encore placé dans le monde : il attend le marché de la porte de la
Citadelle du Pic céleste (`docs/factions/README.md`). D'ici là : œuf (`Alliance Merchant Spawn Egg`) ou
`/summon murimblock:alliance_merchant`.

## Allure

Modèle 3D sur le squelette biped d'Epic Fight (`scripts/animation/merchant-3d.cjs`), **tout en tissu** : ses
marchandises et une éventuelle armure sont des items à part.

- bonnet de gaze noire à perle dorée, deux pans courts derrière ;
- visage en **16 x 16** (deux fois la résolution d'une tête Minecraft) sur une fine plaque devant la tête
  (`scripts/animation/hd-face.cjs`, pièce de densité 2 dans `mesh-kit.cjs`) : yeux plissés de marchand rusé, sourire,
  longue moustache fine, joues rosées ;
- robe de soie azur brochée de petites pièces d'or, bordée d'or pâle, croisée sur une sous-robe blanche ; larges
  manches ; ceinture d'or pâle sur sa bedaine, nœud devant, bourse rouge à la hanche gauche ; jupe de robe fendue,
  pantalon indigo, chaussons noirs ;
- visage vivant (`MobFaces`, 10 atlas) : clignements, regards, marmonnement ; frappé, il grimace.

## Comportement

- **Neutre et pacifique** : il se promène autour de son étal (6 blocs), ne disparaît jamais, n'a aucune arme.
- **Clic droit** : ouvre l'écran d'échange de Minecraft. Pendant l'échange il s'arrête et regarde le joueur ; l'échange
  se ferme si le joueur s'éloigne de plus de 8 blocs.
- **Frappé** : il ferme l'échange et s'enfuit (panique). 30 PV.
- **Stock** : chaque échange est limité, il se réapprovisionne toutes les demi-journées (12 000 ticks).
- Tué par un joueur : 1 à 3 taëls (+ Butin).

## Échanges

Il vend (prix en taëls) :

| Objet | Prix | Stock |
|---|---|---|
| Azure Peak Spear | 24 | 2 |
| Snow Plum Jian | 24 | 2 |
| Rattan Shield | 12 | 2 |
| Qi Gathering Pill | 6 | 8 |
| Realm Breaking Pill | 32 | 2 |
| Potion de soin | 4 | 6 |
| Pomme dorée | 8 | 4 |
| 4 pains | 1 | 16 |
| Long Fist Scroll (art libre) | 40 | 1 |

Il achète (contre des taëls) :

| Butin | Taëls | Stock |
|---|---|---|
| Bandit Dao | 3 | 8 |
| Grande hache du Chef Bandit | 12 | 2 |
| Chaque pièce d'armure du Chef Bandit | 8 | 2 |
| Trident | 10 | 2 |
| 6 cuirs | 1 | 12 |
| 8 morues | 1 | 12 |

Les arts de faction ne se vendent pas : ils se gagnent.

## Taëls d'argent

Icône : un yuanbao, lingot d'argent en forme de barque (`scripts/animation/silver-tael-icon.cjs`). Lâchés aux joueurs
seulement (+ Butin) :

| Mob | Taëls |
|---|---|
| Bandit des montagnes | 0 à 2 |
| Homme-crabe | 0 à 1 |
| Chef Bandit | 6 à 10 |
| Sensei Yun | 15 à 20 |
| Marchand de l'Alliance | 1 à 3 |

## Vérifications

`AllianceMerchantGameTests` : échanges tous en taëls, lance à 24 et dao racheté 3 ; clic droit ouvre l'écran, achat
réel de la lance avec 24 taëls, un coup ferme l'échange ; butin du marchand et taëls des bandits. Portrait en vrai client :
`gradlew -PgameTests runMugshotTour` (`alliance-merchant-front.png`, `-side.png`).
