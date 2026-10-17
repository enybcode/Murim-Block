# L'Art du Démon Dévoreur (噬魔功, *Shì Mó Gōng*)

> **État : proposition, rien en jeu** (17/10/2026). Demande d'Enzo :
> - « imagine un art martial autour de [la kagune], avec une barre de rage et des effets un peu comme du haki des rois » ;
> - « ne crée rien, fais comme la technique d'éclair pour me montrer tes idées » ;
> - « prends comme inspiration Kaneki de Tokyo Ghoul, le haki des rois, et la furie de Kyubi dans Naruto contre Pain » ;
> - « avec une barre de rage et des déplacements à quatre pattes, ça doit être terrifiant à affronter » ;
> - c'est une technique **de haut niveau, donc 8 techniques**.
>
> Les inspirations sont des **idées**, pas des copies : ni le nom, ni l'apparence de ces personnages. Les noms en jeu sont en
> anglais : le nom anglais d'abord, la traduction entre parenthèses. Les chiffres sont un premier jet, alignés sur
> [../technique-balance.md](../technique-balance.md) et le Sanguine Art, l'autre art de haut niveau.

La maquette des tentacules au repos : `node scripts/concepts/kagune-preview.cjs --port=<Blockbench isolé>` écrit le projet
`art/blockbench/kagune_preview.bbmodel` (onglet Animer) et ses GIF dans `images/kagune-idle-*.gif`. Les images restent
locales, comme toutes celles des docs.

## L'idée

Dans les romans murim, les **arts démoniaques** (魔功) sont les plus redoutés : puissants, rapides à apprendre, et qui
dévorent leur maître. Celui-ci est le pire de tous. Le cultivateur laisse un démon affamé pousser dans sa moelle. Il en
tire **quatre tentacules de chair rouge** qui sortent de ses reins : la **Moelle du Démon** (魔髓, *Demon Marrow*, la
kagune qu'on a modélisée). Plus il se bat, plus la **faim** le gagne, et plus le démon prend sa place.

Les trois inspirations, et ce qu'on en garde :
- **Kaneki (Tokyo Ghoul)** : les tentacules qui frappent, empalent, saisissent et dévorent. Le combattant guérit en se
  nourrissant, et la carapace du démon finit par le recouvrir (le kakuja).
- **Le haki des rois (One Piece)** : une **volonté** qui écrase. Quand il la libère, l'air devient lourd, des éclairs
  noirs et rouges crépitent, et les faibles **tombent à genoux** ou s'évanouissent, sans même avoir été touchés.
- **La furie de Kyubi (Naruto contre Pain)** : la bête qui prend le dessus. Le manteau de chakra rouge qui bouillonne
  sur le corps, la course **à quatre pattes**, les rugissements qui soufflent tout, la peau qui brûle, la perte de
  contrôle, et la sphère de destruction concentrée dans la gueule.

**Le principe : la faim monte, la bête sort.**
- **La barre de rage** (*Fury*) se remplit en frappant, en étant frappé et en dévorant.
- Chaque palier change le joueur : plus de tentacules, le manteau rouge, la course à quatre pattes, puis la carapace.
- **Au sommet, le démon prend la main** : une puissance énorme, mais on ne peut plus parer ni fuir.

**Pourquoi c'est terrifiant à affronter :** l'adversaire le **sent venir**.
- Un battement de cœur qui s'accélère avec la rage du démon.
- Des bords d'écran qui rougissent, une pression qui fait trembler la vue.
- Une bête qui court à quatre pattes plus vite qu'un cheval et bondit des murs.
- Et un moment où le démon rugit et où le monde entier s'arrête.

**Sa faiblesse :** la faim.
- Hors combat, la rage retombe vite, et à zéro l'art ne fait presque rien.
- La forme finale épuise (voir « Le contrecoup »).
- Le démon brûle sa propre vie pour frapper : certaines techniques coûtent des **points de vie** en plus du Qi.

## Le manuel et le royaume

- **« Devouring Demon Scripture »** (L'Écriture du Démon Dévoreur) : un livret relié de cuir sombre et cousu de tendons,
  avec un œil rouge et noir sur la couverture, qui suit celui qui le tient.
- **Appris à partir de Foundation Establishment** (6ᵉ royaume), un cran au-dessus du Sanguine Art (Qi Crystallization).
  En dessous, le livre refuse son lecteur : « The demon finds no marrow strong enough to feed on. »
- **Où le trouver : à décider par Enzo.** Quelques idées :
  - au fond d'une **prison scellée** sous une secte démoniaque, gardé par le dernier qui l'a lu, devenu un monstre ;
  - **lâché par un boss** qui l'utilise lui-même ;
  - **au monde intérieur**, après avoir **cédé** au démon du cœur au lieu de le vaincre.

## La barre de rage : *Fury* (Furie)

Une barre à côté de celle du Qi, découpée en **trois paliers** d'un rouge de plus en plus sombre. Elle bat comme un cœur.

**Ce qui la remplit (100 points) :**

| Action | Fureur |
|---|---|
| Un M1 qui touche | +3 |
| Une technique qui touche | +6 à +10 |
| Recevoir un coup | +1 par point de vie perdu (le démon aime la douleur) |
| Dévorer (technique 4) | +25 |
| Tuer un ennemi | +10 |

**Ce qui la vide :** −4 par seconde après 5 s sans frapper ni être frappé. Elle tombe à zéro à la mort, au K.O. et quand
on range la Moelle.

**Les paliers changent le corps :**

| Palier | Fureur | Le corps | Ce que ça donne |
|---|---|---|---|
| **I. Hunger** (La Faim) | 0 à 32 | **deux tentacules** sortent, l'œil gauche devient rouge et noir | les techniques de base |
| **II. Frenzy** (La Frénésie) | 33 à 65 | **les quatre tentacules** ; un **manteau rouge** bouillonne sur la peau, en bulles, comme du sang qui bout | course **à quatre pattes**, +15 % de dégâts, les techniques 5 à 7 |
| **III. Demon Shell** (La Carapace du Démon) | 66 à 99 | une **carapace** noire et rouge pousse sur un bras, la moitié du visage et les tentacules, avec des épines et des plaques. Le manteau devient noir aux bords | +30 % de dégâts, 20 % de dégâts reçus en moins, la pression (technique 5) est permanente autour de lui |
| **Release** (Le Démon libéré) | 100 | voir la technique 8 | l'ultime |

## Le passif : *Demon Marrow* (La Moelle du Démon)

- **Les M1 sont faits par les tentacules**, à mains nues. Un enchaînement à nous de quatre coups : deux fouets en croix,
  un coup d'estoc à deux tentacules, puis un balayage circulaire à 360°.
  - Les tentacules frappent à **4 blocs** : la portée d'une lance, mais à mains nues.
  - Au palier II, chaque M1 frappe **deux fois** (deux tentacules, un temps l'un derrière l'autre).
- **Régénération en se nourrissant** : chaque coup qui touche rend 2 % des dégâts infligés en vie. Au palier III, c'est
  5 %.
- **Hors combat**, les tentacules se rétractent en 2 s, sauf si on tient la touche de l'art.
- **La garde** : les tentacules se croisent devant le corps. C'est la garde Murim habituelle, mais une parade parfaite
  fait **+10 de fureur** et repousse l'attaquant d'un coup de tentacule.

## Les déplacements à quatre pattes : *Beast Gait* (La Démarche de la Bête)

Au palier II et au-delà, **le sprint devient une course à quatre pattes**.
- Le joueur tombe sur les mains. Les deux tentacules du bas se plantent dans le sol comme des pattes, ceux du haut se
  dressent au-dessus du dos.
- **+50 % de vitesse de sprint**, sans faim (Minecraft) consommée.
- **Saut : un bond** de 6 blocs devant, ou de 4 en hauteur.
- **Les murs** : contre un mur, il **s'y accroche** et grimpe à quatre pattes, avec les tentacules qui s'agrippent. Il peut
  bondir d'un mur à l'autre.
- **Ce qu'on voit :**
  - des **griffures** dans le sol à chaque appui, qui restent 2 s ;
  - de la poussière, et la traînée rouge de l'œil la nuit ;
  - pour l'adversaire, des **pas lourds et rapides** qui s'entendent derrière les murs.
- **Inspiration :** Naruto à quatre queues qui charge Pain, et le kakuja de Kaneki.
- **À faire en jeu :** une posture et une course propres dans Epic Fight (mouvements Murim ajoutés comme pour le K.O., qui
  rampe déjà), la caméra un peu plus basse, et la hitbox du joueur abaissée à 1,2 bloc.

## Comment tout se complète

1. **Monter la faim** : les M1 et Crimson Lash, la garde qui contre.
2. **Atteindre et immobiliser** : Impaling Thorns (empale et tire) ou Predator's Pounce (bondit de loin).
3. **Se nourrir** : Devour sur un ennemi immobilisé (+25 de fureur, de la vie).
4. **Écraser** : au palier II, la pression du roi fait tomber les faibles, le rugissement souffle tout, et la carapace
   protège.
5. **Libérer** : à 100, l'ultime.

## Les huit techniques

| # | Technique | Débloquée par | Qi | Recharge | Dégâts (× la part) | Rôle |
|---|---|---|---|---|---|---|
| 1 | **Crimson Lash** (Le Fouet cramoisi) | Le manuel | 150 | 6 s | 4 × 0,5 | Zone devant, fureur |
| 2 | **Impaling Thorns** (Les Épines qui empalent) | Le manuel | 200 | 10 s | 1,2 + tirer à soi | Atteindre, immobiliser |
| 3 | **Predator's Pounce** (Le Bond du Prédateur) | Quête 1 | 180 | 9 s | 1,0, plaqué au sol | Plonger, engager |
| 4 | **Devour** (Dévorer) | Quête 2 | 250 | 20 s | 1,5 + soin | Se nourrir |
| 5 | **Sovereign's Pressure** (La Pression du Souverain) | Quête 3, palier II | 400 | 30 s | — (contrôle) | Écraser les faibles |
| 6 | **Centipede Coil** (Le Repli du Mille-Pattes) | Quête 4 | 300 | 18 s | 0,4 par coup renvoyé | Défense, contre |
| 7 | **Beast's Roar** (Le Rugissement de la Bête) | Quête 5, palier II | 350 | 16 s | 0,8, brise la garde | Souffler, briser |
| 8 | **Demon Release** (Le Démon libéré) | Quête 6, fureur à 100 | 900 | 10 min | voir plus bas | L'ultime |

La **part** suit le Sanguine Art (mains nues durcies : environ 16 à Qi Crystallization, un peu plus haut à Foundation
Establishment). Le garde-fou commun (`BurstGuard`) reste : aucun enchaînement ne tue un joueur en un instant.

### 1. Crimson Lash (Le Fouet cramoisi)

**Ce que fait le joueur :** les quatre tentacules se dressent derrière lui, puis **fouettent l'un après l'autre** devant
lui en éventail, de gauche à droite, chacun un peu plus loin que le précédent (de 3 à 6 blocs).

**Ce que ça fait :**
- 4 × 0,5 sur tout ce qui est dans l'éventail (90°, 6 blocs).
- **+3 de fureur par coup qui touche.**
- Au palier III, chaque fouet laisse une **coupure** au sol qui brûle 1 s.

**Les effets :** chaque fouet laisse une **traînée rouge** courbe, comme les coups d'épée du mod, en ruban pixelisé et
rouge sang. Un claquement de fouet, et l'écran tremble pour ceux qui sont touchés.

### 2. Impaling Thorns (Les Épines qui empalent)

**Ce que fait le joueur :** il tend une main ouverte vers la cible. Deux tentacules **plongent dans le sol** et **jaillissent
sous elle** en pieux (jusqu'à 10 blocs).

**Ce que ça fait :**
- La cible est **empalée** : ×1,2, et elle est clouée sur place 1,5 s (on ne peut plus bouger).
- Puis les tentacules se rétractent et **la tirent jusqu'aux pieds** du joueur, prête pour Devour.
- Une cible en l'air est transpercée en vol et ramenée au sol.

**Les effets :**
- La terre se fend et se soulève en cercle sous la cible une demi-seconde avant : c'est l'**avertissement**, qui laisse
  le temps d'esquiver.
- Les pieux sortent dans un jet de terre.

### 3. Predator's Pounce (Le Bond du Prédateur)

**Ce que fait le joueur :** il se ramasse à quatre pattes, les tentacules plantés derrière lui, puis **bondit** sur la
cible (jusqu'à 12 blocs) les tentacules en avant, et l'écrase au sol.

**Ce que ça fait :**
- ×1,0 et la cible est **plaquée au sol** 1 s (elle ne peut pas parer).
- Depuis un mur, le bond porte à 16 blocs.
- Si la cible est déjà empalée, le bond la plaque et la **cloue** 1 s de plus.

**Les effets :**
- Pendant l'élan, une silhouette floue de rouge (la vitesse).
- À l'atterrissage, un cratère de fissures, et la caméra de la cible tremble fort.
- Pour la cible, un **coup de cœur dans les oreilles** juste avant l'impact.

### 4. Devour (Dévorer)

**Ce que fait le joueur :** les tentacules **saisissent** un ennemi proche (à 3 blocs), le soulèvent, et le joueur **le mord**
à la gorge. Quatre secondes, et la cible ne peut rien faire, comme dans la pince du Roi Crabe.

**Ce que ça fait :**
- ×1,5 en trois morsures.
- Le joueur **regagne 30 % des dégâts** en vie, plus **+25 de fureur**.
- Ne marche que sur un ennemi **immobilisé** (empalé, plaqué, étourdi, K.O.) ou plus faible d'un royaume. Sur un égal
  debout, les tentacules ne font que le repousser.
- **Sur un joueur à terre (K.O. PvP)**, c'est le **finish de l'art** : il est dévoré, en cinématique, comme les autres
  finishs.

**Les effets :** en vue de la victime, la gueule du démon remplit l'écran. Les bords deviennent rouges et la vue tremble à
chaque morsure, comme la prise du Roi Crabe.

### 5. Sovereign's Pressure (La Pression du Souverain) — le haki

**Ce que fait le joueur :** il se redresse, immobile, et **relâche sa volonté**. Une onde invisible se répand à 20 blocs.

**Ce que ça fait, selon la différence de royaume (lue comme la perception du Qi) :**
- **Deux royaumes ou plus en dessous** : la cible **tombe à genoux**, sans pouvoir rien faire, 4 s. Les mobs ordinaires
  **s'évanouissent** (étourdis 6 s) ou s'enfuient s'ils sont animaux.
- **Un royaume en dessous** : la cible est **écrasée** : Lenteur III, Faiblesse II, plus de sprint, 5 s.
- **Même royaume** : la cible est seulement **oppressée** : Lenteur I 3 s, et ses techniques coûtent 20 % de Qi en plus.
- **Plus fort que lui** : rien. Mais lui le sent, et sa vue tremble.
- Au palier III, une **pression faible et permanente** (sur ceux d'en dessous seulement) l'entoure à 8 blocs.

**Les effets, les plus importants de l'art :**
- Au déclenchement, **le temps s'arrête une demi-seconde** autour de lui : les particules se figent, le son se coupe.
- Puis l'onde part en **anneau d'air déformé**.
- Des **éclairs noirs et rouges** crépitent au sol et sur les blocs proches.
- Les feuilles tombent des arbres, l'herbe se couche, et le verre se fissure (sans casser).
- **Pour ceux qui la subissent :**
  - l'écran se **désature**, des veines rouges poussent depuis les bords ;
  - un battement de cœur lourd se fait entendre ;
  - la vue est tirée vers le sol (genou à terre) ;
  - un message, en lettres manuscrites qui tremblent : « *You cannot breathe.* »

### 6. Centipede Coil (Le Repli du Mille-Pattes)

**Ce que fait le joueur :** les quatre tentacules **s'enroulent autour de lui** en spirale serrée, comme un cocon, et
tournent. Tenu jusqu'à 3 s.

**Ce que ça fait :**
- **Arrête tout ce qui vient de toutes les directions** (coups et projectiles) tant qu'il tient.
- **Chaque coup arrêté est renvoyé** : un tentacule sort du cocon et frappe celui qui a frappé (0,4).
- **+5 de fureur par coup arrêté.**
- Au relâché, les tentacules se déroulent d'un coup : un balayage à 360° (×0,8) qui repousse tout.
- **Le coût :** Qi au départ, puis 30 par seconde tenue (comme le Spinning Rampart du bâton).

**Les effets :** le cocon est un modèle de tentacules enroulés qui tourne ; à chaque coup arrêté, une gerbe de sang
(rouge, pixelisée) et un claquement mat.

### 7. Beast's Roar (Le Rugissement de la Bête)

**Ce que fait le joueur :** à quatre pattes, il **rugit** vers l'avant, la gueule grande ouverte, les tentacules écartés.

**Ce que ça fait :**
- Un **cône** de 10 blocs : ×0,8, et tout est **soufflé** à 8 blocs en arrière.
- **Brise la garde** : une garde levée est cassée et la cible est étourdie 1 s.
- Les blocs fragiles (verre, feuilles, herbe, fleurs) **volent en éclats** dans le cône, si les règles du monde le permettent.
- Au palier III, le rugissement fait aussi un **petit effet de pression** (technique 5, sur ceux d'en dessous).

**Les effets :**
- Une **onde de son visible**, en anneaux déformés qui partent de la gueule.
- La poussière se soulève en cône, et l'écran de ceux qui sont touchés tremble fort.
- Le son est un rugissement grave, mêlé à des voix.

### 8. Demon Release (Le Démon libéré) — l'ultime

**Il faut la fureur à 100.** Le joueur se tient la tête à deux mains et hurle. **Le démon prend le dessus pour 20 s**,
dans une cinématique courte de 2 s pour tous ceux qui sont à 32 blocs.

**La transformation :**
- La carapace recouvre **tout le corps**.
- Les quatre tentacules deviennent **six**, plus longs, plus épais, cuirassés.
- Le manteau rouge devient un **manteau noir et rouge** qui bouillonne comme de la lave. Là où il passe, l'herbe noircit.
- Il ne se tient plus qu'**à quatre pattes**.

**Pendant 20 s :**
- +60 % de dégâts, 40 % de dégâts reçus en moins, et la pression du souverain permanente à 20 blocs.
- Les M1 deviennent des **coups de six tentacules**.
- Il guérit de 10 % de chaque coup.
- **Il ne peut plus parer, ni ranger l'art, ni fuir** : le démon ne recule pas.
- **Une fois pendant la forme**, la touche de l'ultime lance la **Bouche du Démon** (*Demon's Maw*) :
  - il concentre dans sa gueule une **sphère** de chair et de Qi noir et rouge, qui grossit en 1,5 s ;
  - il la crache : elle file 30 blocs et **explose** sur 8 blocs (×4,0 au centre, ×2,0 au bord) ;
  - le cratère reste, si les règles du monde le permettent ;
  - inspiration : la bombe de Kyubi.

**Le contrecoup :** à la fin des 20 s, le démon rend le corps.
- Le joueur tombe **à genoux 3 s**, sans défense.
- Puis il reste **épuisé** 60 s : la fureur ne monte plus, Lenteur I, et pas de régénération de Qi.
- C'est le moment où l'adversaire peut le tuer : il **faut** avoir gagné avant.

**Les effets :** les plus forts du mod.
- Au déclenchement, le **ciel s'assombrit** autour de lui (32 blocs) et un **pilier de lumière rouge** monte.
- La carapace pousse en plaques et en épines, sur un modèle à nous, superposé au joueur comme la kagune.
- Pour les adversaires proches, l'écran garde une **vignette rouge** tant que la forme dure, et le battement de cœur ne
  s'arrête pas.

## Ce qu'il faudrait créer (quand ce sera validé)

- **Les modèles :**
  - la **kagune** (déjà en maquette : `scripts/concepts/kagune-preview.cjs`, `art/blockbench/kagune_preview.bbmodel`) en
    deux, quatre et six tentacules ;
  - la **carapace** en trois morceaux (bras, visage, tentacules) ;
  - le **manteau** (une couche translucide sur le joueur, qui bouillonne) ;
  - la **sphère** de la Bouche du Démon ;
  - le **livre**.
- **Les animations Epic Fight :**
  - les M1 des tentacules ;
  - les huit techniques, au sol et en l'air ;
  - la **posture et la course à quatre pattes**, l'accroche aux murs et le bond.
- **La barre de fureur** dans le HUD, à côté de celle du Qi, discrète comme elle.
- **Les effets d'écran** :
  - pour ceux qui subissent la pression et le rugissement : désaturation, veines rouges, battement de cœur ;
  - pour l'adversaire de l'ultime : la vignette rouge.
- **Les sons** (à créer ou à prendre dans Minecraft) : battement de cœur, fouet, rugissement, mâchoire.

## Questions pour Enzo

- **Le nom** : *Devouring Demon Art* (L'Art du Démon Dévoreur) te va ? Et *Fury* pour la barre de rage ?
- **Le royaume** : à partir de Foundation Establishment, ou plus tôt, comme le Sanguine Art (Qi Crystallization) ?
- **Où le trouver** : prison scellée, boss, ou le monde intérieur après avoir cédé au démon ?
- **Le prix** : est-ce que certaines techniques doivent coûter de la vie en plus du Qi ? Et le contrecoup de l'ultime
  (60 s d'épuisement) est-il assez dur, ou trop ?
- **Les blocs** : le rugissement et la Bouche du Démon doivent-ils casser des blocs, ou seulement en jeu PvE ?
- **La forme kakuja** : c'est bien le palier III et l'ultime, ou tu la veux dès le palier II ?
