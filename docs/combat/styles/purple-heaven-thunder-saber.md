# Le Sabre du Tonnerre du Ciel Pourpre (紫霄雷刀, *Zǐxiāo Léidāo*)

> **État : en jeu sur `test` depuis le 17/10/2026** (validé sur le principe par Enzo : « c'est validé, implémente ça au jeu »,
> avec une arme « plus massive »). Le manuel est **donné par la foudre** (voir « Le manuel »). Le Zhanmadao **se craftera** :
> sa recette reste à choisir par Enzo ; d'ici là il est dans l'onglet créatif. Code : `martial/ThunderSaber`, `martial/ThunderSaberMoves`, `client/ThunderSaberFx` ; animations
> `scripts/animation/thunder-saber-art.cjs` ; tests `ThunderSaberGameTests` ; tour `gradlew -PgameTests runThunderSaberTour`.
> Demande d'Enzo :
> - « un nouvel art martial du même niveau que Shooting Star, même qualité graphique » ;
> - « une grosse arme à deux mains, la hache ou l'épée » ;
> - « je te laisse tout gérer, l'inspi libre à toi, et l'arme tu peux la générer ».
>
> C'est un art **libre** et **intermédiaire**, au même rang que l'Épée des Sept Étoiles ([seven-stars-sword.md](seven-stars-sword.md)) :
> - il s'apprend à partir de **Qi Condensation** ;
> - il compte **cinq techniques**, un passif, un secret, un ultime et un finish ;
> - ses effets sont riches : éclairs, anneaux, sceau, nuage et colonne dessinés en lumière additive, particules à nous,
>   lumières Photon (comme les Sept Étoiles).
>
> Les chiffres sont un premier jet, alignés sur [../technique-balance.md](../technique-balance.md). Les règles communes
> sont dans [../STYLES.md](../STYLES.md). Les noms en jeu sont en anglais : le nom anglais d'abord, la traduction entre
> parenthèses.

## L'idée

Dans le xianxia, la **foudre pourpre** (紫雷) est la foudre du Ciel lui-même, celle qui frappe les cultivateurs aux
grandes percées. Cet art est celui d'un sabreur qui a survécu à la sienne et en a gardé le tonnerre dans sa lame.

- **L'arme : le zhanmadao** (斩马刀, « le sabre qui tranche les chevaux »), le grand sabre chinois à deux mains. Il a une lame longue et large à un seul tranchant, et une poignée longue comme un avant-bras.
- **Le principe : le poids, puis la foudre.** Chaque coup lourd **bat un tambour de tonnerre**. Les tambours chargés **réveillent la foudre** dans la technique suivante. On frappe lourd pour charger, puis on libère le Ciel.
- **Le contraste avec les Sept Étoiles :**
  - les Étoiles sont un art de **précision**, vif, au fleuret, qui construit sur l'ennemi ;
  - le Tonnerre est un art de **masse**, lent et inarrêtable, qui construit **sur soi** et éclate en zone.
- **Sa faiblesse :** il est lent. Les M1 sont lourds, les tambours s'éteignent hors du combat, et il faut s'engager : une technique ratée laisse ouvert.
- **Un clin d'œil au monde** : sous un **vrai orage** Minecraft, le Ciel répond. Les tambours se chargent deux fois plus vite (voir le passif).

## Le manuel

- **« Purple Heaven Thunder Saber Manual »** (Manuel du Sabre du Tonnerre du Ciel Pourpre) : un livret de bronze sombre, frappé du motif du tonnerre (雷纹, les spirales carrées des bronzes antiques), avec un éclair d'or au dos.
- Lu, il est consommé. Le style apparaît dans l'onglet Techniques avec son passif et sa première technique.
- **Où le trouver** (décidé par Enzo le 17/10/2026 : « frappé par la foudre, au Qi Crystallization, avec un pôle de cuivre
  et une grosse arme en main ») : pendant un **vrai orage**, à ciel ouvert, un joueur au stade **Qi Crystallization** ou
  au-delà se tient à **2 blocs au plus d'un paratonnerre** (le bloc de cuivre de Minecraft, lui aussi à ciel ouvert), **une
  grande épée en main** (n'importe laquelle pour Epic Fight). L'orage se tourne vers lui ; après **6 secondes**, un éclair
  frappe le paratonnerre et le traverse **sans le blesser**, et le ciel lui donne le manuel, **une seule fois** par joueur
  (`martial/ThunderCalling`). En dessous du royaume, un message dit que l'orage passe sans répondre.
- Le manuel se lit à partir de Qi Condensation (règle commune des arts intermédiaires), mais on ne l'obtient qu'à Qi
  Crystallization.

## L'arme : le Zhanmadao du Ciel Pourpre (*Purple Heaven Zhanmadao*)

> **Déjà généré** : `node scripts/animation/purple-heaven-zhanmadao-model.cjs` écrit le modèle tenu
> (`models/item/purple_heaven_zhanmadao_3d.json`), l'icône d'inventaire, la texture 32 × 32 et la source Blockbench
> (`art/blockbench/purple_heaven_zhanmadao.bbmodel`), sur le modèle de la grande hache du Chef Bandit. L'objet lui-même
> n'est pas encore enregistré dans le jeu.

- **La lame :** longue et large, à un seul tranchant.
  - Elle s'élargit vers la pointe, coupée en biais (la pointe est dans l'axe du tranchant).
  - L'acier est un **gris d'orage**, presque bleu, avec un tranchant clair et un dos plus sombre.
  - Un **éclair d'or incrusté** court le long de la gouttière, sur les deux faces, de la garde aux deux tiers de la lame.
- **La garde :** un disque de bronze ciselé du **motif du tonnerre**, une pierre violette sur chaque face.
- **La poignée :** longue, pour les deux mains, liée de cordelette violette, avec trois bagues de bronze.
- **Le pommeau :** un petit **tambour de bronze** à la peau d'or. D'en dessous pend une houppe violette au nœud d'or.
- **En main :** c'est une grande épée pour Epic Fight (type `epicfight:greatsword`, comme la grande hache du Chef Bandit). Elle prend donc la posture, la course et l'enchaînement de la grande épée.
- **Ses stats :** un peu au-dessus de la grande épée de diamant, rare.
- **Avec l'art,** les **trois tambours** du joueur s'allument sur l'éclair incrusté : un tiers de l'éclair brille par tambour chargé, avec une petite lumière Photon violette qui pulse à la garde.

## Le passif : « Rolling Drums » (Tambours roulants)

- **Les tambours :** chaque **M1 qui touche** donne **1 coup de tambour**. Trois coups chargent **un tambour**. On en porte **3 au plus**.
  - Une parade parfaite donne 2 coups. Un M1 qui touche **trois ennemis** à la fois en donne 2.
  - On les voit à l'écran : trois petits tambours de bronze à côté de la barre de Qi, discrets, qui s'allument en violet.
- **Ce qu'ils font :** chaque technique **bat automatiquement un tambour** si on en a un. Elle passe alors en **version foudre**, qui est plus forte, plus large et frappée de la foudre (voir chaque technique). Sans tambour, la technique part en version simple.
- **Inarrêtable :** chaque coup de l'enchaînement M1, et toutes les techniques, ont l'**hyperarmure** (pas de recul, pas d'étourdissement). On encaisse les coups sans être interrompu, mais on prend les dégâts.
- **Sous l'orage :** pendant un vrai orage, et à ciel ouvert, les tambours se chargent **deux fois plus vite**.
- **Les tambours s'éteignent** un par un après **8 s** sans toucher personne.
- **Les M1 :** l'enchaînement de grande épée d'Epic Fight (deux coups lourds, la ruée, le coup en l'air), dont le dernier de la chaîne libère une **petite décharge** (×0,3 à 2 blocs) si un tambour est plein.
- **La garde :** la garde Murim habituelle. Une parade parfaite donne 2 coups de tambour et fait vaciller l'attaquant.

## Comment tout se complète

Les tambours sont la ressource. **Toutes les techniques en consomment ou en rendent**, et leurs versions foudre se répondent :

1. **Ouvrir** : Thunder Descends tombe sur le groupe ; sa version foudre laisse les ennemis **électrisés** (ralentis, 3 s).
2. **Charger** : les M1 lourds, l'hyperarmure pour encaisser pendant qu'on charge.
3. **Tenir** : Heaven's Drum arrête les coups et **change chaque coup reçu en coup de tambour** : on recharge en se défendant.
4. **Contrôler** : Rolling Thunder ramène les ennemis à soi ; la foudre **saute d'un électrisé à l'autre**.
5. **Libérer** : Nine Heavens' Wrath **dépense tous les tambours** pour appeler la foudre sur la zone, un éclair de plus par tambour.

Severing Flash sert à **frapper de loin**, ou à **finir** un électrisé qui fuit (sa version foudre le traverse).

| Technique | Tambours | Version foudre | Plus fort sur un électrisé |
|---|---|---|---|
| Thunder Descends | bat 1 | un éclair frappe le point d'impact, onde ×1,5 plus large | — (c'est elle qui électrise) |
| Rolling Thunder | bat 1 | des arcs de foudre partent vers les ennemis proches à chaque tour | l'arc saute d'électrisé en électrisé (4 sauts) |
| Heaven's Drum | **rend** 1 coup par coup arrêté | la décharge finale électrise tout le monde autour | — |
| Severing Flash | bat 1 | la vague devient un croissant de foudre qui traverse tout | +30 % de dégâts |
| Nine Heavens' Wrath | **les bat tous** | +1 éclair par tambour dépensé | les éclairs visent d'abord les électrisés |

## Les cinq techniques

| # | Technique | Débloquée par | Qi | Recharge | Dégâts (× l'attaque) | Rôle |
|---|---|---|---|---|---|---|
| 1 | **Thunder Descends** (Le Tonnerre descend) | Le manuel | 30 | 8 s | ×1,4, onde ×0,6 | Ouvrir, plonger |
| 2 | **Rolling Thunder** (Tonnerre roulant) | Quête 1 | 40 | 12 s | 2 × 0,7 | Zone, ramener |
| 3 | **Heaven's Drum** (Le Tambour du Ciel) | Quête 2 | 45 | 16 s | 0,4 par coup arrêté | Défense, recharge |
| 4 | **Severing Flash** (L'Éclair qui tranche) | Quête 3 | 55 | 14 s | ×1,0 à ×1,8 selon la charge | À distance, finir |
| 5 | **Nine Heavens' Wrath** (Le Courroux des Neuf Cieux) | Quête 4 | 70 | 22 s | 6 à 9 × 0,4, puis ×1,5 | Zone, encaisser |

À Qi Condensation, le Zhanmadao fait environ **10** d'attaque :
- Thunder Descends ≈ 14 sur la cible, 6 autour ;
- Rolling Thunder ≈ 14 si les deux tours touchent ;
- Severing Flash chargé ≈ 18 ;
- Nine Heavens' Wrath ≈ 30 sur une cible seule si tout touche.

Le garde-fou commun (`BurstGuard`) reste valable : aucun enchaînement ne tue un joueur d'un coup.
Chaque technique a sa version au sol et en l'air, avec ses animations Epic Fight.

### 1. Thunder Descends (Le Tonnerre descend)

**Ce que fait le joueur :** il fait un **grand bond** vers la cible visée (jusqu'à 7 blocs). Au sommet, il lève le sabre à deux mains au-dessus de la tête. Il retombe en **fendant droit vers le sol**.

**Ce que ça fait :**
- La cible prend ×1,4.
- Une **onde de choc** de 3 blocs projette les autres en arrière (×0,6).
- **Version foudre :** un **éclair pourpre** frappe le point d'impact juste avant le sabre. L'onde fait 4,5 blocs, et tous ceux qu'elle touche sont **électrisés** 3 s (Lenteur I, de petits arcs sur le corps).
- **En l'air :** le bond devient une plongée en diagonale.

**Les effets :**
- **Au départ :** une poussière de pierre au sol.
- **Au sommet :** le sabre **s'allume le long de son éclair incrusté**.
- **À l'impact :**
  - un **anneau de choc** au sol : un modèle plat, fissuré, qui s'élargit et s'efface ;
  - des **éclats de roche** (particules du bloc touché) ;
  - une **fissure en étoile** qui reste 1 s.
- **Version foudre :** l'éclair est un **modèle d'éclair en zigzag** (trois variantes tirées au hasard), avec un cœur blanc et des bords violets semi-transparents. Il descend en 2 ticks, et un **flash Photon violet** puissant s'efface en 0,4 s.

### 2. Rolling Thunder (Tonnerre roulant)

**Ce que fait le joueur :** il tient le sabre à l'horizontale, bras tendus, et fait **deux tours complets** en avançant de 3 blocs, comme un tourbillon d'acier.

**Ce que ça fait :**
- Deux coups (2 × 0,7) sur tout ce qui est à 3,5 blocs.
- Chaque tour **attire** un peu les ennemis vers lui, au lieu de les repousser : on les rassemble.
- **Version foudre :** à chaque tour, des **arcs de foudre** partent vers les ennemis à 6 blocs (×0,3) et **sautent d'un électrisé à l'autre**, jusqu'à 4 sauts.
- **En l'air :** une roue verticale qui rabat au sol.

**Les effets :**
- une **traînée circulaire** large et fine qui suit la lame, comme un disque d'acier ;
- de la poussière soulevée en spirale ;
- **version foudre :** les arcs sont des **modèles de rubans brisés**, re-tirés à chaque tick pour crépiter, chacun avec une lumière Photon qui court le long de l'arc.

### 3. Heaven's Drum (Le Tambour du Ciel)

**Ce que fait le joueur :**
- Il **plante le sabre** devant lui, pointe en bas, les deux mains sur le pommeau en tambour.
- **Tant qu'il tient la touche** (3 s au plus), un **tambour de foudre** se dessine autour de lui : un cylindre d'arcs violets.
- Quand il relâche, il **frappe le pommeau** du plat de la main, comme on bat un tambour.

**Ce que ça fait :**
- Pendant la tenue, **tout coup reçu de face** (coup ou projectile) est **arrêté**. Chaque coup arrêté donne **1 coup de tambour** : on recharge en se défendant.
- **Au relâché :**
  - une **décharge** frappe tout autour (0,4 par coup arrêté, jusqu'à ×2,0) ;
  - en version foudre, elle **électrise** tout le monde.
- **Le coût :** 10 Qi, puis 5 par seconde tenue (comme le Spinning Rampart du bâton), et la recharge commence au relâché.
- **En l'air :** il tombe au sol et commence planté.

**Les effets :**
- le **cylindre d'arcs** : un modèle de rubans de foudre en anneau, qui tourne lentement ;
- **à chaque coup arrêté :** un **éclat** violet à l'endroit du coup et un battement sourd ;
- la **lumière Photon** du tambour pulse à chaque coup arrêté, un peu plus fort à chaque fois ;
- **au relâché :** une **onde de son** visible, un anneau d'air déformé qui s'élargit, et un grand flash.

### 4. Severing Flash (L'Éclair qui tranche)

**Ce que fait le joueur :** il recule le pied, le sabre bas en arrière, tranchant devant, et **charge** tant qu'il tient la touche (1,5 s au plus). Il **relâche** : une coupe horizontale d'un seul geste.

**Ce que ça fait :**
- La coupe lance une **vague tranchante** qui file **12 blocs** droit devant (×1,0, jusqu'à ×1,8 chargée à fond).
- **Version foudre :** la vague devient un **croissant de foudre** qui **traverse tout**, ennemis et feuillage, et fait **+30 %** aux électrisés.
- **En l'air :** la vague part en diagonale vers le sol.

**Les effets :**
- **Pendant la charge :** de petits arcs grimpent le long de la lame, plus vite à mesure que la charge monte, et une lumière Photon grandit sur le sabre.
- **La vague :** un **croissant modélisé**, très fin au centre et effilé aux bouts, en acier clair (ou violet en foudre). Elle laisse un sillon d'air.
- **Sur chaque ennemi touché :** une **ligne de coupe** qui brille un instant.

### 5. Nine Heavens' Wrath (Le Courroux des Neuf Cieux)

**Ce que fait le joueur :** il lève le sabre **vers le ciel**, pointe en haut, à deux mains. Un **cercle de foudre** apparaît au sol à l'endroit visé (6 blocs de large, jusqu'à 14 blocs devant). Un **nuage d'orage** se forme au-dessus.

**Ce que ça fait :**
- **Six éclairs** frappent le cercle l'un après l'autre en 1,5 s (×0,4 chacun), **plus un par tambour dépensé** : neuf avec les trois tambours, d'où le nom.
- Les éclairs **visent d'abord les électrisés**.
- **Le dernier éclair frappe le sabre lui-même.** Le joueur l'abat alors vers le sol : une **colonne de foudre** dans le cercle (×1,5).
- C'est l'encaissement de l'art. Elle **dépense tous les tambours**.
- **En l'air :** le joueur reste suspendu le temps de l'orage.

**Les effets :**
- **Le cercle au sol** est le **motif du tonnerre** : des spirales carrées en anneau, qui s'allument une à une.
- **Le nuage d'orage** est un **disque de nuage modélisé**, sombre et violacé, qui tourne et s'éclaire de l'intérieur à chaque éclair.
- **Chaque éclair :** le modèle en zigzag, un flash Photon, une marque brûlée au sol qui s'efface.
- **Le dernier éclair** court du nuage au sabre, et le sabre **brille en entier**.
- **La colonne finale :** un **cylindre de foudre** épais, avec un **grand flash Photon** qui éclaire tout le paysage une demi-seconde, et un vrai coup de tonnerre.

## Le secret : « Heaven Answers » (Le Ciel répond)

- **Comment on l'utilise :** une **parade parfaite**, puis un **M1** dans les 10 ticks. C'est une riposte immédiate : un coup de sabre montant (×1,3) suivi d'un **éclair qui tombe sur l'attaquant** (×0,5). Elle **charge un tambour entier** d'un coup.
- **Comment on la découvre :** après **30 parades parfaites** avec l'art. L'indice : « Thunder does not ask. It answers. »
- **L'effet :** une **fissure de lumière** s'ouvre dans le ciel au-dessus de l'attaquant juste avant l'éclair.

## L'ultime : « Thunder Sovereign's Descent » (La Descente du Souverain du Tonnerre)

Elle se débloque par la quête « The Sovereign's Path » (100 ennemis frappés par les éclairs). Qi 150, recharge 3 minutes.

- **L'ouverture :** le joueur plante le sabre et lève une main. Une **colonne de foudre pourpre** le frappe.
- **Pendant 8 s :**
  - les **tambours sont pleins** et ne se vident pas ;
  - le sabre est **enveloppé de foudre** ;
  - **chaque M1 appelle un éclair** sur sa cible (×0,4) ;
  - chaque pas laisse des étincelles.
- **La fin :** un **dernier éclair colossal** tombe sur l'ennemi le plus proche (×2,5 sur la cible, ×1,0 à 5 blocs).

**Les effets :** un **halo de foudre** modélisé autour du corps, les yeux qui brillent en violet, et une lumière Photon qui suit le joueur. Le ciel s'assombrit autour de lui : un voile de nuage bas, côté client.

## Le finish : « The Last Thunderclap » (Le Dernier Coup de tonnerre)

Il se joue sur un joueur à terre (B).

- Le joueur lève le sabre vers le ciel, et un éclair **frappe la lame**.
- Il l'abat d'un seul coup : un grand flash, puis le silence.
- Un coup de tonnerre roule au loin quelques secondes plus tard.

## Les effets : lourds dans la forme, violents dans la lumière

> Enzo veut « beaucoup d'effets… à partir d'intermédiaire, on peut mettre du budget », « utilise Photon pour les effets
> lumineux », et « plus clairs, c'est goofy ». Donc les formes sont **nettes**, et le spectacle vient de la **lumière** et
> du **contraste**.

- **Modèles 3D GeckoLib,** comme les cristaux du Sanguine Art :
  - l'éclair en zigzag (trois variantes) ;
  - l'arc de foudre en ruban brisé ;
  - l'anneau de choc fissuré ;
  - le croissant tranchant ;
  - le cylindre du tambour ;
  - le nuage d'orage ;
  - le cercle au motif du tonnerre ;
  - la colonne de foudre ;
  - le halo de l'ultime.
- **Les parties lumineuses** brillent dans le noir (glowmask) et sont **semi-transparentes** sur les bords. Les éclairs n'ont pas de texture floue : un cœur blanc net et un bord violet.
- **Des particules à nous :** étincelles violettes et dorées, petits arcs, poussière de pierre, cendres d'impact.
- **Des lumières dynamiques Photon,** toujours, par un `LightProvider` (`PhotonLights.addProvider`, soumis à chaque image) plutôt que des flashs isolés :
  - un flash fort et bref pour chaque éclair ;
  - une lumière qui court le long de chaque arc ;
  - une lueur qui pulse au tambour de la garde selon les tambours chargés ;
  - le grand flash de la colonne finale.
- **Les sons :** les coups lourds et les impacts sont sourds. Le tonnerre vient de Minecraft pour les éclairs, avec un roulement lointain après Nine Heavens' Wrath.
- **Les couleurs :** **violet**, **blanc** et **or**. Le bronze et le gris d'orage pour le sabre. Rien de rouge, pour ne pas se confondre avec le sang ni avec les Sept Étoiles (blanc et argent).

## Les quêtes et la maîtrise

- **Quête 1 « The First Drum » :** charger les 3 tambours **20 fois**. Débloque Rolling Thunder.
- **Quête 2 « Unmoved » :** encaisser **50 coups** pendant l'hyperarmure sans être interrompu. Débloque Heaven's Drum.
- **Quête 3 « The Drum Answers » :** arrêter **30 coups** avec Heaven's Drum. Débloque Severing Flash.
- **Quête 4 « Call Down the Sky » :** toucher **3 électrisés** avec un seul Severing Flash, 10 fois. Débloque Nine Heavens' Wrath.
- **Évolutions** à la maîtrise :
  - à 30, Thunder Descends peut être relancée une fois dans les 2 s si elle a touché ;
  - à 60, Rolling Thunder fait 3 tours au lieu de 2 ;
  - à 90, Nine Heavens' Wrath laisse un **orage** au-dessus du cercle pendant 4 s, qui frappe encore un électrisé chaque seconde.

## Ce qu'il faut pour le faire

- **L'objet :** le Zhanmadao (enregistrement, capacité Epic Fight de grande épée, recette ou butin) et le manuel (icône au motif du tonnerre).
- **Les animations :** environ 12 clips Epic Fight (chaque technique au sol et en l'air, la charge, la tenue, le secret, l'ultime et le finish), générés comme ceux du bâton (`staff-art.cjs`) et des Sept Étoiles.
- **Les effets :** une dizaine de modèles GeckoLib, leurs textures et glowmasks, 4 particules, les lumières Photon.
- **Le code :** l'art dans `MartialArts`, les tambours (sauvegardés, synchronisés), l'électrisé (un effet), l'hyperarmure, les quêtes.
- **Les vérifications :** des tests serveur (dégâts, tambours, parades : tout doit être parable sauf les éclairs de l'ultime), et un tour client filmé, comme `runTravellerStaffTour`.

## Décisions et ce qui reste à décider

1. ~~Le nom~~ : gardé.
2. ~~Où trouver le manuel~~ : donné par la foudre (voir « Le manuel »), 17/10/2026.
3. **Le Zhanmadao** : il se craftera ; **sa recette reste à choisir par Enzo**.
4. ~~La foudre pourpre~~ : gardée.
