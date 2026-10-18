# L'alchimie : le four à pilules

> **État : en jeu le 18/10/2026** (tranche 2 de [plants.md](plants.md)). Décisions d'Enzo : un four **comme dans les
> webtoons de Murim**, dans le monde avec un **écran de feu** ; les recettes apprises dans un **Manuel d'alchimie** (les
> rares sur des parchemins) ; le four **fabricable** ; une grosse surchauffe donne un **résidu et une petite explosion** ;
> **pas de plafond** aux pilules, seule la toxicité freine.

## Le four (丹炉)

Un four de bronze doré rond comme une boule sur trois pattes de bête, un couvercle en dôme et son fleuron en flamme, des
anses en anneau, une ceinture de **runes de formation** qui s'allument quand il chauffe, posé sur un cercle de formation
avec son propre feu (environ 1,6 bloc de haut). Au repos : des braises. En raffinage : le feu rugit, les runes pulsent, le
couvercle tressaute et fume. À la fin : le couvercle s'envole dans une colonne de lumière, les pilules jaillissent, le
couvercle retombe. Modèle et animations : `scripts/plants/cauldron-model.cjs` (GeckoLib), rendu
`client/PillFurnaceRenderer`.

Fabrication : un lingot d'or, un bloc de charbon et cinq blocs de cuivre (` G ` / `CBC` / `C C`).

## Comment on raffine

1. **L'eau** : un seau ou une fiole d'eau dans sa case de l'écran (versée aussitôt, le seau vide reste) ou sur le four.
2. **Les herbes, dans l'ordre de la recette**, dans leurs trois cases (ou utilisées sur le four) : la **principale**,
   l'**assistante**, l'**harmonisante**. Une herbe qui ne va pas là est refusée ; on ne reprend que la dernière entrée, avant que
   le travail commence (accroupi, main vide : toutes).
3. **Le feu** : du charbon (de bois ou minéral) dans sa case, que le four prend quand son feu en a besoin, ou sur le four.
   Le feu tient les braises (320) ; le **soufflet** (maintenu) fait monter la chaleur, laissée seule elle redescend.
4. **L'écran** (main vide sur le four), **à la Minecraft**, choisi par Enzo le 18/10 : à gauche le livre de recettes de
   Minecraft (les recettes connues, un clic y met l'eau et les herbes depuis l'inventaire, les plus pures d'abord ; les autres en
   rouge avec où les apprendre ; les bandes de la recette montrée ; une recherche) ; à droite le four : l'eau, les trois herbes,
   la flèche vers les pilules, la flamme sur le charbon, la chaleur en **thermomètre** avec la **bande dorée** de l'étape en
   cours, **Fan** et **Qi** maintenus, le Qi, les quatre étapes et la pureté visée ; l'inventaire dessous. Ce qui manque
   apparaît en transparence dans sa case.
5. **Extraire** : tenir l'aiguille dans la bande le temps voulu. **Stabiliser** : la laisser redescendre dans une bande plus
   basse. **Raffiner** : tenir une bande étroite **en infusant son Qi** (il se vide pendant ce temps).
6. **Les pilules sortent** dans leur case (ce qui n'y tient pas jaillit du four), leur pureté faite de celle des herbes (60 %) et de l'adresse (40 % : le temps idéal sur le temps
   mis), moins ce qu'a coûté la chaleur de trop (plus de 120 au-dessus d'une bande). Une pilule de plus si la pureté atteint
   80, une de plus si une herbe était à maturité spirituelle.
7. **Au-delà de 950** : tout brûle en **résidu**, une gerbe de flammes et une petite explosion qui blesse sans rien casser.

On ne raffine que ce qu'on sait : une recette inconnue ne démarre pas. Réussite ou explosion, **l'écran se ferme** chez tous ceux
qui l'ont ouvert, pour qu'on voie l'animation (demande d'Enzo).

## Les recettes

| Recette | Herbes (dans l'ordre) | Extraire / Stabiliser / Raffiner | Qi par seconde | Où l'apprendre |
|---|---|---|---|---|
| Pilule de Rassemblement du Qi | cosse de lotus, baies d'argousier, chrysanthème | 600-700 / 400-480 / 520-590 | 6 | Manuel |
| Pilule de Percée | tige de cistanche, rhododendron, cosse de lotus | 680-760 / 440-500 / 600-650 | 12 | Manuel |
| Décoction de soin | baies d'argousier, racine d'astragale | 450-560 / 300-400 / 380-460 | 3 | Manuel |
| Décoction de Qi | baies de goji, pousse de bambou | 480-580 / 320-420 / 420-500 | 4 | Manuel |
| Breuvage purifiant | fronde de fougère, chrysanthème | 420-520 / 300-380 / 360-430 | 5 | Manuel |
| Élixir martial | racine de ginseng, botte de laîche, baies de goji | 700-790 / 460-520 / 620-670 | 16 | Parchemin |
| Pilule d'Apaisement de l'âme | chapeau de lingzhi, lotus des neiges, chrysanthème | 720-800 / 480-540 / 640-690 | 20 | Parchemin |

Le Manuel d'alchimie se fabrique (un livre, un chrysanthème, une cosse de lotus) et se trouve aussi dans les caches des
sites ; les parchemins rares sont dans les caches, l'arène et les camps de bandits. Les recettes apprises sont gardées à la
mort.

## Ce que font les pilules

Toutes ajoutent leur toxicité (`CultivationRules`, qui baisse d'un point par minute) : **moins elles sont pures, plus**
(×1 à 50 % de pureté, ×0,7 à 100 %, ×1,18 à 20 %), et donnent ce que la toxicité et la pureté laissent de leur effet.

| Pilule | Effet | Toxicité |
|---|---|---|
| Rassemblement du Qi, Percée | comme avant (`PillItem`) | 30, selon la pureté |
| Décoction de soin | régénération II (10 s au mieux) | 8 |
| Décoction de Qi | un tiers de la réserve de Qi | 12 |
| Breuvage purifiant | retire 25 de toxicité (une fois par minute) | aucune |
| Élixir martial | force I pendant une minute | 20 |
| Apaisement de l'âme | démon du cœur affaibli une fois et demie plus longtemps qu'avec la Percée, retire 10 de toxicité | — |

## Code et vérifications

`alchemy/` : `AlchemyRecipes`, `AlchemyKnowledge`, `PillFurnaceBlock`, `PillFurnaceBlockEntity` (chaleur, étapes, pureté,
explosion), `PillFurnaceMenu` (six cases, données synchronisées, recettes connues et boutons par les clics de menu de Minecraft :
aucun paquet réseau propre), `AlchemyPillItem`, `AlchemyBookItem`, `AlchemyEvents` (pureté des deux premières pilules, recettes gardées à la
mort). Écran : `client/PillFurnaceScreen`. Données, icônes et noms : `scripts/plants/alchemy-data.cjs`.

Tests : `AlchemyGameTests` (l'eau d'abord et l'ordre des herbes, une recette inconnue ne démarre pas, les trois étapes
tenues donnent des pilules pures et le raffinage boit le Qi, l'explosion donne un résidu sans rien casser, le manuel et un
parchemin, le breuvage purifiant, les cases de l'écran et leur ordre, le livre qui remplit une recette, l'écran fermé à la fin). En vrai client : `gradlew -PgameTests runFurnaceTour`.

## Plus tard

Un four supérieur (bandes plus larges, plus facile), des fours dans les sites, la couleur du breuvage qui change avec ses
herbes, d'autres recettes (culte du Sang), une lumière Photon sur le four.
