# La Hallebarde du Croissant de Lune (月牙戟, *Crescent Moon Halberd*)

> **État : en jeu sur `test` le 19/10/2026** (choisie par Enzo parmi quatre propositions, faite en autonomie la nuit
> du 19/10 ; demandes d'Enzo : « une arme pour l'art martial comme d'habitude », « utilise Photon et GeckoLib et crée un
> art original en termes d'effets »). À valider en jeu par Enzo. Noms en jeu en anglais.

Art **orthodoxe**, élément **la lune et l'eau**. Élégant, tout en cercles, à distance moyenne. Une hallebarde chinoise
(*ji*) : une lance avec une lame en croissant sur le côté. Il s'apprend à **Qi Condensation** ; son rouleau enseigne
les quatre techniques et l'ultime (pas de quêtes, pas encore de secret ni de finish propre).

## L'arme

La **Crescent Moon Halberd** (`MurimItems.CRESCENT_MOON_HALBERD`) : hampe laquée bleu nuit cerclée d'argent, poignée de
soie blanche, houppe blanche sous un collier d'argent, longue pointe d'argent, et sur le côté un **croissant d'acier pâle
dont le tranchant brille d'une lumière de lune** (pleine lumière : il luit dans le noir), une pierre de lune dans la
douille, un petit croc de l'autre côté pour accrocher. Une lance pour Epic Fight, traînée d'arme bleu pâle. Modèle :
`scripts/animation/crescent-halberd-model.cjs` (53 cubes, source Blockbench `art/blockbench/crescent_moon_halberd.bbmodel`).
Fabrication : `" QI" / " SQ" / "S  "` (deux quartz, un lingot de fer, deux bâtons). Aussi dans les
coffres de l'arène de l'Alliance et des caches des sites, avec le rouleau (plus fréquent dans l'arène).

## Passif « Waxing Moon »

- Les frappes de lance d'Epic Fight. Chaque frappe qui touche un ennemi **remplit la lune d'une phase** (6 pour la pleine
  lune ; elle décroît après 8 s sans toucher).
- La lune est **affichée à côté du viseur** : un petit disque qui se remplit de droite à gauche, blanc et auréolé quand il
  est plein (`client/CrescentMoonClient`).
- Pleine, la **frappe suivante lance un croissant de lumière** à 15 blocs, qui traverse tout sur son passage (×1,2).

## Les techniques

| # | Nom | Qi | Recharge | Effet |
|---|---|---|---|---|
| 1 | **Moonlit Circle** | 45 | 12 s | Un tour complet bas autour de soi (touche tout autour), puis un **anneau d'eau de lune** au sol (4,5 blocs, 5 s) qui tourne lentement : tout ennemi dedans est ralenti. |
| 2 | **Tide Hook** | 35 | 9 s | Le croc va chercher l'ennemi devant (7 blocs) : il est **tiré** jusque devant la hallebarde ; s'il est trop lourd (60 PV ou plus, ou très résistant au recul), il est **fauché** et mis à terre. Un fil de lumière de lune relie le croc à sa prise. |
| 3 | **Falling Crescent** | 55 | 13 s | Une grande coupe en diagonale : **trois croissants de lumière** partent en éventail (18° entre eux), 15 blocs, traversants (×0,9 chacun). |
| 4 | **Mirror Lake Stance** | 30 + 5/s | 15 s | **Tenue** (3 s au plus) : la hallebarde debout devant soi, un **miroir d'eau** apparaît devant le corps. Coups et projectiles venus de face s'y enfoncent, et chacun est **renvoyé à son auteur en croissant** (×1,2). On avance lentement ; pas d'autre technique pendant. |
| 5 | **Night of the Full Moon** (ultime) | 180 | 4 min | La hallebarde levée au ciel : **la nuit tombe** autour (le ciel et l'écran s'assombrissent en bleu profond pour qui est à moins de 30 blocs), une **pleine lune géante se lève** au-dessus du joueur, puis ses **rayons de lune** frappent les ennemis à 12 blocs, huit salves de quatre rayons (×0,8 chacun, ralentissement). |

## Les effets (originaux, GeckoLib + Photon)

Tous les effets de lune sont des **modèles GeckoLib** générés (`scripts/animation/crescent-moon-fx.cjs`, entité
`martial/MoonQiEntity`, rendu `client/MoonQiRenderer`, palette qui brille et se voit en transparence) :

- le **croissant** : couché, tranchant blanc, chair bleu pâle, gaine de Qi translucide, gouttes de lumière derrière ;
- l'**anneau** de Moonlit Circle : bande d'eau de lune, anneau pointillé qui tourne en sens inverse, sceau de croissant
  au centre qui respire (animation `spin`) ;
- le **miroir** de Mirror Lake : disque d'eau debout cerclé de lumière d'argent, un croissant dedans (animation `ripple`) ;
- la **pleine lune** : sphère de pierre de lune aux mers grises, halo qui tourne (animation `turn`) ;
- les **rayons de lune** : colonnes de lumière de la lune au sol.

Les lumières sont celles de **Photon** (`CrescentMoonPayloads.Fx`, protocole 38) : un éclair bleu pâle à chaque impact,
la lumière froide et durable de la pleine lune, et la nuit qui tombe. Aura : gouttes de lune qui montent en cercle autour
des pieds.

## Le monde intérieur

Un **lac noir et immobile** cerclé de pierre sombre, une passerelle de planches bordée de lanternes, des pavillons sur
pilotis, des saules, un **chemin de lune** sous l'eau vers le nord, et une **lune immense** basse sur le lac
(`InnerTheme.CRESCENT_MOON`, [../../cultivation/inner-worlds.md](../../cultivation/inner-worlds.md)) ; la brume y est
repoussée pour que la lune se voie.

## Ce qui est en jeu

| Élément | Code | Vérifié par |
|---|---|---|
| L'art, ses techniques, le passif | `MartialArts` (`CRESCENT_MOON_HALBERD`), `martial/CrescentMoonHalberd` | `CrescentMoonGameTests` (7 tests) |
| Les animations (hallebarde à deux mains, sur les poses de la Lance du Pic Céleste) | `scripts/animation/crescent-halberd.cjs`, `MurimAnimations.buildCrescentMoonHalberd` | idem, `gradlew -PgameTests runCrescentMoonTour` (film en vrai client) |
| Les effets GeckoLib, les lumières Photon, la lune du viseur | `MoonQiEntity`, `MoonQiRenderer`, `CrescentMoonFx`, `CrescentMoonClient`, `CrescentMoonPayloads` | `runCrescentMoonTour` |
| L'arme, le rouleau, les icônes (famille « nuit du lac »), l'aura | `MurimItems`, `TechniqueIcons.java`, `ScrollTexture.java`, `AuraClient` | idem |
| Le monde intérieur | `InnerTheme`, `InnerWorlds.moonLake` | `runInnerWorldsTour` |

Pas encore : un secret, un finish propre, un guide du Dragon d'Or, l'équilibrage en vrai combat, le jeu à deux joueurs.
