# La Hallebarde du Croissant de Lune (月牙戟, *Crescent Moon Halberd*)

> **État : en jeu sur `test` le 19/10/2026, validé par Enzo le 19/10 (« oui valider », série D)** (choisie par Enzo parmi quatre propositions, faite en autonomie la nuit
> du 19/10, techniques refaites le 19/10 au matin (série D) ; demandes d'Enzo : « une arme pour l'art martial comme d'habitude », « utilise Photon et GeckoLib et crée un
> art original en termes d'effets »). Validé par Enzo sur les captures ; reste à l'essayer en vraie partie. Noms en jeu en anglais.

Art **orthodoxe**, élément **la lune et l'eau**. Élégant, tout en cercles, à distance moyenne. Une hallebarde chinoise
(*ji*) : une lance avec une lame en croissant sur le côté. Il s'apprend à **Qi Condensation** ; son rouleau enseigne
les quatre techniques et l'ultime (pas de quêtes, pas encore de secret ni de finish propre). Un art de ruse : ses reflets d'eau trompent l'ennemi.

## L'arme

La **Crescent Moon Halberd** (`MurimItems.CRESCENT_MOON_HALBERD`) : hampe laquée bleu nuit cerclée d'argent, poignée de
soie blanche, houppe blanche sous un collier d'argent, longue pointe d'argent, et sur le côté un **croissant d'acier pâle
dont le tranchant brille d'une lumière de lune** (pleine lumière : il luit dans le noir), une pierre de lune dans la
douille, un petit croc de l'autre côté pour accrocher. Une lance pour Epic Fight, traînée d'arme bleu pâle. Modèle :
`scripts/animation/crescent-halberd-model.cjs` (53 cubes, source Blockbench `art/blockbench/crescent_moon_halberd.bbmodel`).
Fabrication : `" QI" / " SQ" / "S  "` (deux quartz, un lingot de fer, deux bâtons). Aussi dans les
coffres de l'arène de l'Alliance et des caches des sites, avec le rouleau (plus fréquent dans l'arène).

## Refonte du 19/10 : le reflet trompeur

Enzo a gardé l'arme, les animations et la **pleine lune au-dessus de la tête** de l'ultime, mais pas le reste
(« j'aime bien l'idée de la lune au-dessus de la tête mais sinon non ») ; il a choisi la **série D** : un art de ruse,
où des **reflets d'eau** de soi trompent l'ennemi. Les premières techniques (croissants, anneau, croc, miroir tenu) et la
lune du viseur sont retirées.

Un **reflet** (`ReflectionEntity`) est le corps même du joueur, **dans la pose qu'il tient à cet instant**, dessiné en lumière
d'eau bleue translucide qui ondule (`client/ReflectionRenderer`, comme les images rémanentes du Nuage Azur) : il frappe
quand on frappe. Les mobs alentour se retournent contre lui ; le premier coup porté par quelqu'un (pas son maître, pas un
mur ni une chute) le **brise en éclats de verre d'eau** (éclaboussures, lumière Photon). Six au plus à la fois.

## Passif « Mirror Image »

- Les frappes de lance d'Epic Fight.
- **Chaque esquive** (roulade ou pas de côté d'Epic Fight) laisse un reflet là où elle a commencé, pour 4 s : les ennemis
  qui étaient sur toi se jettent sur lui. Une fois toutes les 5 s au plus.

## Les techniques

| # | Nom | Qi | Recharge | Effet |
|---|---|---|---|---|
| 1 | **Twin Reflection** | 40 | 10 s | Une grande coupe : un reflet sort **de l'autre côté de l'ennemi** devant (9 blocs) et frappe avec toi, **dans son dos** (garde brisée, ×1,2). Le reflet reste 5 s. |
| 2 | **Shattered Mirror** | 50 | 12 s | Un tour complet avec la hallebarde : **tous tes reflets éclatent** en éclats de verre d'eau qui coupent autour d'eux (3 blocs, ×1,4, ralentissement). Sans reflet, les éclats partent de toi, plus faibles (×0,8). |
| 3 | **Swap** | 25 | 6 s | Tu **échanges ta place** avec ton reflet le plus proche (24 blocs) ; ce qui était sur toi est maintenant sur lui. Refusée sans reflet. |
| 4 | **Hall of Mirrors** | 90 | 30 s | **Quatre reflets** t'entourent pendant 6 s. Chaque seconde, chacun traverse l'eau jusqu'à un ennemi à lui (14 blocs) et le frappe (×0,45) ; les ennemis se tournent vers eux. |
| 5 | **Night of the Full Moon** (ultime) | 180 | 4 min | Inchangée : la nuit tombe, une **pleine lune géante se lève au-dessus de la tête** et ses rayons frappent les ennemis à 12 blocs, huit salves. |

## Les effets

- Les reflets : le corps du joueur en lumière d'eau translucide, qui ondule et s'éclaire en apparaissant.
- Leur apparition et leur bris : éclaboussures, éclats de verre bleu et blanc, étincelles de lune, lumière **Photon**.
- La pleine lune et ses rayons : modèles **GeckoLib** (`MoonQiEntity`, `scripts/animation/crescent-moon-fx.cjs`), lumière
  froide Photon, la nuit qui tombe sur l'écran (`CrescentMoonPayloads.Fx`).

## Le monde intérieur

Un **lac noir et immobile** cerclé de pierre sombre, une passerelle de planches bordée de lanternes, des pavillons sur
pilotis, des saules, un **chemin de lune** sous l'eau vers le nord, et une **lune immense** basse sur le lac
(`InnerTheme.CRESCENT_MOON`, [../../cultivation/inner-worlds.md](../../cultivation/inner-worlds.md)) ; la brume y est
repoussée pour que la lune se voie.

## Ce qui est en jeu

| Élément | Code | Vérifié par |
|---|---|---|
| L'art, ses techniques, le passif, les reflets | `MartialArts` (`CRESCENT_MOON_HALBERD`), `martial/CrescentMoonHalberd`, `martial/ReflectionEntity` | `CrescentMoonGameTests` (7 tests) |
| Les animations (hallebarde à deux mains, sur les poses de la Lance du Pic Céleste) | `scripts/animation/crescent-halberd.cjs`, `MurimAnimations.buildCrescentMoonHalberd` | idem, `gradlew -PgameTests runCrescentMoonTour` (film en vrai client) |
| Les reflets dessinés, la lune GeckoLib, les lumières Photon | `client/ReflectionRenderer`, `MoonQiEntity`, `MoonQiRenderer`, `CrescentMoonFx`, `CrescentMoonClient`, `CrescentMoonPayloads` | `runCrescentMoonTour` |
| L'arme, le rouleau, les icônes (famille « nuit du lac »), l'aura | `MurimItems`, `TechniqueIcons.java`, `ScrollTexture.java`, `AuraClient` | idem |
| Le monde intérieur | `InnerTheme`, `InnerWorlds.moonLake` | `runInnerWorldsTour` |

Pas encore : un secret, un finish propre, un guide du Dragon d'Or, l'équilibrage en vrai combat, le jeu à deux joueurs.
