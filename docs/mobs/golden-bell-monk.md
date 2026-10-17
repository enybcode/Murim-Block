# Moine de la Cloche d'Or (金钟罩僧) — *Golden Bell Monk*

Moine guerrier de style Shaolin, demandé par Enzo le 17/10/2026 avec l'art de la Cloche d'Or
([../combat/styles/golden-bell.md](../combat/styles/golden-bell.md)). Il garde le sutra de l'art et le vend. Il vit dans
la **cour du milieu du temple taoïste de la Mer de Bambous** (`BambooTemple`), près du taiji, côté salle des écritures,
tourné vers l'escalier couvert qui monte chez Sensei Yun. Sinon : œuf (`Golden Bell Monk Spawn Egg`) ou
`/summon murimblock:golden_bell_monk`.

## Allure

Modèle 3D sur le squelette biped d'Epic Fight (`scripts/animation/golden-bell-monk-3d.cjs`), **tout en tissu** :

- crâne rasé, les points d'ordination sur le dessus ; visage en **16 x 16** (`hd-face.cjs`), sourcils droits, bouche
  calme ;
- robe safran croisée sur une sous-robe pâle, manches courtes jusqu'au coude ; ceinture brune nouée devant, deux pans ;
- long chapelet de perles de bois sombre, une grosse perle et un gland rouge ;
- avant-bras nus pris dans des bandes de tissu blanc cassé à lanière brune ; pantalon safran, bandes aux mollets,
  chaussons noirs ;
- visage vivant (`MobFaces`, 10 atlas) ;
- **forme d'or** : un second jeu de 10 atlas où toute la peau est or poli, le visage aussi (yeux ambre).

Fichiers : `animmodels/entity/golden_bell_monk.json`, `textures/entity/golden_bell_monk*.png` et
`golden_bell_monk_gold*.png`, projet `art/blockbench/golden_bell_monk.bbmodel`.

## Comportement

- **Neutre** : il se promène autour de son poste (6 blocs), ne disparaît jamais et **ne frappe jamais le premier**.
- **Clic droit** : l'écran d'échange de Minecraft, comme le marchand de l'Alliance.
- **Frappé** : il ferme l'échange et **se bat à mains nues** avec les coups d'Epic Fight (`FIST_AUTO1/2/3` enchaînés,
  `FIST_DASH` sur un adversaire à 2,5-5 blocs), tous parables. **En colère, il refuse d'échanger.**
- **Il se calme** après **30 s** sans être frappé (ou si son adversaire meurt, une fois les 30 s passées).
- **40 PV**, armure 2. Tuable ; il ne lâche rien de spécial.

### Forme de la Cloche d'Or

Sous **la moitié de ses PV**, **une fois par combat**, il prend la forme d'or pendant **10 s** :

- sa peau devient or (drapeau synchronisé, `GoldenBellMonkEntity.golden()`, le rendu change d'atlas) ;
- **−40 %** de dégâts reçus, aucun recul ;
- une cloche sonne à l'entrée, résonne à la sortie.

La forme redevient disponible quand il s'est calmé.

## Échanges

| Objet | Prix | Stock |
|---|---|---|
| **Golden Bell Manual** | 40 taëls (le prix du Poing Long chez le marchand) | 1 |
| Qi Gathering Pill | 6 taëls | 4 |
| 2 bougies | 1 ligature de sapèques | 8 |
| 4 pains | 1 ligature de sapèques | 16 |

Réapprovisionnement toutes les demi-journées (12 000 ticks).

## Code et tests

- `mob/GoldenBellMonkEntity`, `integration/epicfight/GoldenBellMonkPatch`, `client/GoldenBellMonkRenderer`.
- `GoldenBellMonkGameTests` (`-PgameTestFilter=goldenbellmonk`) : manuel à 40 taëls acheté depuis l'écran d'échange,
  refus d'échanger une fois frappé ; neutre envers un joueur voisin, puis cible son agresseur ; forme d'or sous la
  moitié des PV, −40 % de dégâts, fin après 10 s.
- `SiteGameTests` : le temple de la Mer de Bambous compte Sensei Yun et le moine.
- Un monde où le temple a déjà ses habitants (Sensei Yun présent) ne reçoit pas le moine après coup
  (`SiteResidents` ne complète pas un site déjà peuplé) : œuf ou `/summon`.
