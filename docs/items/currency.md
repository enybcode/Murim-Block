# La monnaie du Murim

Demande d'Enzo (16/10/2026) : une monnaie comme dans la Chine ancienne.

Quatre monnaies. Chacune vaut **9 de la précédente**. À l'établi, 9 pièces en carré 3 × 3 en font 1 de la suivante, et
1 se défait en 9.

| Objet | Nom chinois | Valeur | D'où elle vient |
|---|---|---|---|
| **Copper Cash** (pièce de cuivre) | 铜钱 *tóngqián*, le *wén* | 1 | bandits (2 à 8), hommes-crabes (1 à 6), tués par un joueur |
| **String of Cash** (ligature) | 一串钱 | 9 pièces | à l'établi ; le marchand vend 4 pains pour 1 ligature |
| **Silver Tael** (tael d'argent) | 银两 *yínliǎng* | 81 pièces | les prix du marchand, le butin des soldats et des boss |
| **Gold Yuanbao** (lingot d'or) | 金元宝 *jīn yuánbǎo* | 9 taels | les fortunes : le Jeton de la Secte coûte 64 taels et 32 yuanbao |

## Inspiration et simplification

- **La sapèque** : la pièce ronde à trou carré a servi pendant deux mille ans, des Qin aux Qing.
- **La ligature** : on enfilait les pièces sur une cordelette par le trou carré, en ligatures, en principe de mille.
- **Le tael et le yuanbao** : l'argent se comptait au poids, en taels, coulé en petits lingots en forme de bateau. Le
  *yuánbǎo* en or était le grand lingot des fortunes.
- **Ce qu'on a simplifié** : dans l'histoire, mille pièces faisaient à peu près un tael. Ici, le rapport est de 9 entre
  chaque niveau, pour que les échanges tiennent dans la grille 3 × 3 de l'établi.

## Fichiers

- Objets : `MurimItems.COPPER_CASH`, `CASH_STRING`, `SILVER_TAEL`, `GOLD_YUANBAO` (onglet Murimblock).
- Icônes :
  - `scripts/animation/currency-icons.cjs` pour la pièce, la ligature et le yuanbao ;
  - `silver-tael-icon.cjs` pour le tael.
- Recettes de change : `data/murimblock/recipe/<grand>_from_<petit>.json` et `<petit>_from_<grand>.json`.
- Tests : `CurrencyGameTests` (le change dans les deux sens, la petite monnaie des bandits).
