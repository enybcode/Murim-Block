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
| Misty Bamboo Daoist Temple Pendant | 128 (64 + 64) | 1 |
| Yellow River Bandit Den Pendant | 128 (64 + 64) | 1 |
| Alliance Arena Pendant | 128 (64 + 64) | 1 |

### Les pendentifs (demande d'Enzo du 15/10)

- **Leur rôle.** Ce sont les marchandises les plus chères du marchand. Comme le Jeton de la Secte, chaque pendentif a une aiguille, mais elle pointe vers la structure **de son type la plus proche** (`LocatorPendantItem`).
- **Leurs structures.** Seules les structures importantes ont un pendentif : celles qui ont un boss, ou des PNJ à quêtes (aucune pour l'instant). Aujourd'hui :
  - le temple de la Mer de Bambous (Sensei Yun) ;
  - le repaire des bandits du Fleuve Jaune (le Chef Bandit) ;
  - l'arène de l'Alliance (le capitaine de la Garde du Pic).
- **Leur dessin rappelle le lieu** (`scripts/worldgen/PendantTextures.java`, 32 positions d'aiguille chacun, comme une boussole) :
  - **Temple** : un disque de jade cerclé de bambou à nœuds, un nuage azur gravé, une houppe azur, et une aiguille en petite épée (l'Azure Cloud de Sensei Yun).
  - **Repaire** : un disque de bronze terni rivé de fer, au cordon de cuir, et une aiguille en hache (la grande hache du Chef).
  - **Arène** : un disque émaillé azur cerclé d'or pâle, frappé des trois pics de l'Alliance, une houppe dorée, et une aiguille en pointe de lance (le capitaine).
- **L'usage :**
  - Clic droit : il cherche, puis donne la direction et la distance. Il dit aussi quand on est arrivé, ou qu'aucune structure n'est à portée. Recharge de 5 s.
  - L'aiguille se met à jour seule, mais une recherche lointaine coûte une à deux secondes au serveur. Il ne recherche donc de lui-même qu'après un trajet de 2 000 blocs, au plus une fois par minute.
  - Il ne fonctionne qu'à la surface (Overworld).
- **Une nouvelle structure importante** (boss ou PNJ à quêtes) reçoit son pendentif dans `MurimItems.pendants()`, `PendantTextures` et `pendant-models.cjs`. Sa structure doit aussi avoir un nom (`structure.murimblock.<id>`).

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
réel de la lance avec 24 taëls, un coup ferme l'échange ; butin du marchand et taëls des bandits ; chaque pendentif vendu
128 taëls, achat réel d'un pendentif avec deux piles, et un pendentif sans structure à portée le dit sans pointer nulle
part. En vrai client, `runSiteTour` vérifie que chaque pendentif pointe au même endroit que `/locate`. Portrait en vrai client :
`gradlew -PgameTests runMugshotTour` (`alliance-merchant-front.png`, `-side.png`).
