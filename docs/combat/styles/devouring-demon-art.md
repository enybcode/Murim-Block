# L'Art du Démon Dévoreur (噬魔功, *Shì Mó Gōng*)

> **État : proposition, rien en jeu** (17/10/2026). Demandes d'Enzo :
> - « imagine un art martial autour de [la kagune], avec une barre de rage et des effets un peu comme du haki des rois » ;
> - « ne crée rien, fais comme la technique d'éclair pour me montrer tes idées » ;
> - « prends comme inspiration Kaneki de Tokyo Ghoul, le haki des rois, et la furie de Kyubi dans Naruto contre Pain » ;
> - « avec une barre de rage et des déplacements à quatre pattes, ça doit être terrifiant à affronter » ;
> - c'est une technique **de haut niveau, donc 8 techniques** ;
> - **deuxième version** : « j'aimerais qu'il ait des attaques sans rage et avec, et quand il est à 100 de rage tout change
>   et il se met à quatre pattes ».
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
kagune qu'on a modélisée). Plus il se bat, plus la **faim** monte. Quand elle est pleine, **le démon prend sa place**.

Les trois inspirations, et ce qu'on en garde :
- **Kaneki (Tokyo Ghoul)** : les tentacules qui frappent, empalent, saisissent et dévorent. Le combattant guérit en se
  nourrissant, et la carapace du démon finit par le recouvrir (le kakuja).
- **Le haki des rois (One Piece)** : une **volonté** qui écrase. Quand il la libère, l'air devient lourd, des éclairs
  noirs et rouges crépitent, et les faibles **tombent à genoux** ou s'évanouissent, sans même avoir été touchés.
- **La furie de Kyubi (Naruto contre Pain)** : la bête qui prend le dessus. Le manteau de chakra rouge qui bouillonne
  sur le corps, la course **à quatre pattes**, les rugissements qui soufflent tout, la perte de contrôle, et la sphère de
  destruction concentrée dans la gueule.

## Le principe : deux formes

L'art a **deux visages**, et la barre de rage fait passer de l'un à l'autre.

| | **La forme humaine** (*Marrow Form*) | **La forme du démon** (*Demon Form*), à 100 de rage |
|---|---|---|
| **Le corps** | debout, les quatre tentacules dans le dos | **à quatre pattes**, six tentacules, la carapace, le manteau rouge |
| **Les techniques** | huit techniques ; certaines **sans rage**, d'autres **dépensent** la rage | **les huit changent** : chacune devient sa version bête |
| **La rage** | elle **monte** (avec les M1 et les techniques sans rage) | elle **descend**, et la forme dure tant qu'il en reste |
| **Ce qu'on fait** | on construit, on contrôle, on se nourrit | on chasse, on écrase, on détruit |

- **Les techniques sans rage** (1 à 4) : on peut toujours les lancer. Elles **remplissent** la barre.
- **Les techniques à rage** (5 à 7) : elles coûtent **un morceau de la barre** en plus du Qi. Plus fortes, mais elles
  retardent la transformation.
- **L'ultime** (8) : à 100 de rage, au lieu d'attendre, on peut aussi **dépenser toute la barre** d'un coup.
- **Le choix du joueur** : dépenser la rage pour les techniques 5 à 7, ou la garder pour atteindre 100 et se transformer.

**Pourquoi c'est terrifiant à affronter :** l'adversaire **voit la barre monter** et sait ce qui arrive.
- Quand la rage dépasse 70, l'adversaire entend un **battement de cœur** qui s'accélère, et les tentacules s'agitent plus
  fort.
- À 100, le démon **rugit**, le monde s'arrête une demi-seconde, et la bête se jette à quatre pattes.
- La bête court plus vite qu'un cheval, grimpe aux murs et bondit de l'un à l'autre.

**Sa faiblesse :**
- Hors combat, la rage retombe vite.
- En forme du démon, il ne peut ni parer ni fuir (le démon ne recule pas).
- À la fin de la forme, il s'effondre (voir « Le contrecoup »).

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

Une barre rouge à côté de celle du Qi, sur **100 points**. Elle bat comme un cœur, de plus en plus vite.

**En forme humaine, elle monte :**

| Action | Fureur |
|---|---|
| Un M1 qui touche | +3 |
| Une technique **sans rage** qui touche | +8 |
| Recevoir un coup | +1 par point de vie perdu (le démon aime la douleur) |
| Dévorer (technique 4) | +25 |
| Tuer un ennemi | +10 |
| Une parade parfaite | +10 |

- Elle **retombe** de 4 par seconde après 5 s sans frapper ni être frappé.
- Les techniques à rage (5 à 7) en **coûtent** une part.
- **À 100, la transformation se déclenche toute seule**, sauf si on garde la touche de l'art enfoncée pour la retenir (au
  plus 5 s : le démon finit toujours par sortir).

**En forme du démon, elle descend :**
- −5 par seconde (20 s de forme si personne ne la nourrit) ;
- chaque coup qui touche rend +2 : en se battant sans arrêt, la forme dure plus longtemps ;
- à 0, la forme prend fin.

## Les tentacules au fil de la rage

En forme humaine, la barre se voit aussi **sur le corps**, sans changer les règles :
- **0 à 49** : les quatre tentacules ondulent calmement, comme dans la maquette.
- **50 à 79** : ils s'agitent plus vite, et l'œil gauche devient rouge et noir.
- **80 à 99** : un **manteau rouge** commence à bouillonner sur la peau, en bulles, comme du sang qui bout. Les adversaires
  proches entendent le battement de cœur.

## Le passif : *Demon Marrow* (La Moelle du Démon)

- **Les M1 sont faits par les tentacules**, à mains nues. Un enchaînement à nous de quatre coups : deux fouets en croix,
  un coup d'estoc à deux tentacules, puis un balayage circulaire à 360°. Les tentacules frappent à **4 blocs**.
- **Régénération en se nourrissant** : chaque coup qui touche rend 2 % des dégâts infligés en vie.
- **Hors combat**, les tentacules se rétractent en 2 s, sauf si on tient la touche de l'art.
- **La garde** : les tentacules se croisent devant le corps. Une parade parfaite donne **+10 de fureur** et repousse
  l'attaquant d'un coup de tentacule.

## La forme du démon : *Demon Form*

**La transformation (à 100 de rage) :**
- Le joueur se tient la tête à deux mains et **hurle**. Le monde s'arrête une demi-seconde autour de lui : les
  particules se figent et le son se coupe.
- La **carapace** noire et rouge recouvre son corps en plaques et en épines.
- Les quatre tentacules deviennent **six**, plus longs et cuirassés.
- Le **manteau** devient noir et rouge et bouillonne comme de la lave. Là où il passe, l'herbe noircit.
- Il **tombe à quatre pattes**, et il y reste jusqu'à la fin de la forme.
- Une courte cinématique de 2 s pour tous ceux qui sont à 32 blocs.

**Ce que donne la forme :**
- +40 % de dégâts, 30 % de dégâts reçus en moins.
- Il guérit de 8 % de chaque coup.
- **La pression du souverain**, faible mais **permanente**, à 12 blocs autour de lui (voir la technique 5, sur ceux d'en
  dessous seulement).
- Les **M1 deviennent des coups de bête** : deux coups de griffes, une morsure, puis un coup des six tentacules à la ronde.
- **Les huit techniques changent** (voir chacune plus bas).
- **Il ne peut plus parer, ni ranger l'art, ni fuir.**

**La démarche de la bête :**
- Il court à quatre pattes : **+50 % de vitesse de sprint**, sans consommer la faim de Minecraft.
- **Saut : un bond** de 6 blocs devant, ou de 4 en hauteur.
- **Les murs** : contre un mur, il **s'y accroche** et grimpe, les tentacules agrippés. Il bondit d'un mur à l'autre.
- **Ce qu'on voit et entend :**
  - des **griffures** dans le sol à chaque appui, qui restent 2 s ;
  - de la poussière, et la traînée rouge de l'œil la nuit ;
  - pour l'adversaire, des **pas lourds et rapides** qui s'entendent derrière les murs.
- **À faire en jeu :** une posture et une course à nous dans Epic Fight (comme le K.O., qui rampe déjà), la caméra plus
  basse, et la hitbox du joueur abaissée à 1,2 bloc.

**Pour l'adversaire, tant que la forme dure :** une vignette rouge sur les bords de l'écran et un battement de cœur qui ne
s'arrête pas, dès qu'il est à 24 blocs du démon.

**Le contrecoup :** à la fin de la forme, le démon rend le corps.
- Le joueur tombe **à genoux 3 s**, sans défense.
- Puis il reste **épuisé** 45 s : la fureur ne monte plus, Lenteur I, pas de régénération de Qi.
- C'est le moment où l'adversaire peut le tuer : il **faut** avoir gagné avant.

## Les huit techniques

La **part** suit le Sanguine Art (mains nues durcies : environ 16 à Qi Crystallization, un peu plus haut à Foundation
Establishment). Le garde-fou commun (`BurstGuard`) reste : aucun enchaînement ne tue un joueur en un instant.

| # | Forme humaine | Rage | Qi | Recharge | Forme du démon (la même touche) |
|---|---|---|---|---|---|
| 1 | **Crimson Lash** (Le Fouet cramoisi) | **donne** +8 | 150 | 6 s | **Rending Claws** (Les Griffes qui déchirent) |
| 2 | **Impaling Thorns** (Les Épines qui empalent) | **donne** +8 | 200 | 10 s | **Thorn Field** (Le Champ d'épines) |
| 3 | **Centipede Coil** (Le Repli du Mille-Pattes) | **donne** +5 par coup arrêté | 300 | 18 s | **Carapace Charge** (La Charge de la Carapace) |
| 4 | **Devour** (Dévorer) | **donne** +25 | 250 | 20 s | **Feast** (Le Festin) |
| 5 | **Sovereign's Pressure** (La Pression du Souverain) | **coûte** 40 | 400 | 30 s | **Sovereign's Dread** (L'Effroi du Souverain) |
| 6 | **Predator's Pounce** (Le Bond du Prédateur) | **coûte** 25 | 180 | 9 s | **Hunting Leap** (Le Bond du Chasseur) |
| 7 | **Beast's Roar** (Le Rugissement de la Bête) | **coûte** 35 | 350 | 16 s | **Cataclysm Roar** (Le Rugissement du Cataclysme) |
| 8 | **Demon Release** (Le Démon libéré) | **toute** la barre | 900 | 10 min | **Demon's Maw** (La Bouche du Démon) |

En forme du démon, les techniques **ne coûtent plus de rage** (seulement du Qi, deux fois moins) et rechargent deux fois
plus vite : la bête frappe sans compter.

### 1. Crimson Lash (Le Fouet cramoisi) — sans rage

**Forme humaine :** les quatre tentacules se dressent derrière lui, puis **fouettent l'un après l'autre** devant lui en
éventail, de gauche à droite, chacun un peu plus loin (de 3 à 6 blocs).
- 4 × 0,5 dans un éventail de 90° sur 6 blocs, et **+8 de fureur** s'il touche.
- **Les effets :** chaque fouet laisse une **traînée rouge** courbe, en ruban pixelisé rouge sang. Un claquement de fouet,
  et l'écran tremble pour ceux qui sont touchés.

**Forme du démon, Rending Claws :** il **bondit** de 4 blocs sur la cible et la lacère : trois coups de griffes et un coup
des six tentacules (3 × 0,5 puis ×1,0). Les griffures restent au sol et brûlent 1 s.

### 2. Impaling Thorns (Les Épines qui empalent) — sans rage

**Forme humaine :** il tend une main ouverte vers la cible. Deux tentacules **plongent dans le sol** et **jaillissent sous
elle** en pieux (jusqu'à 10 blocs).
- La cible est **empalée** : ×1,2, et elle est clouée sur place 1,5 s. Puis les tentacules la **tirent jusqu'à ses
  pieds**, prête pour Devour.
- **L'avertissement :** la terre se fend en cercle sous la cible une demi-seconde avant, ce qui laisse le temps d'esquiver.

**Forme du démon, Thorn Field :** il plante ses six tentacules dans le sol. **Un champ de pieux** jaillit autour de lui sur
8 blocs, en trois vagues qui s'éloignent (3 × 0,7). Ceux qui sont touchés par la dernière vague sont cloués 1 s.

### 3. Centipede Coil (Le Repli du Mille-Pattes) — sans rage

**Forme humaine :** les quatre tentacules **s'enroulent autour de lui** en spirale serrée, comme un cocon, et tournent. Tenu
jusqu'à 3 s.
- **Arrête tout ce qui vient de toutes les directions** (coups et projectiles) tant qu'il tient.
- **Chaque coup arrêté est renvoyé** : un tentacule sort du cocon et frappe celui qui a frappé (0,4). **+5 de fureur par
  coup arrêté.**
- Au relâché, les tentacules se déroulent d'un coup : un balayage à 360° (×0,8) qui repousse tout.
- Le coût : du Qi au départ, puis 30 par seconde tenue (comme le Spinning Rampart du bâton).

**Forme du démon, Carapace Charge :** le démon ne se défend plus, il **fonce**. Il se roule dans sa carapace et charge à
quatre pattes sur 12 blocs, en écrasant tout sur son passage (×1,2, renversé). Les murs de terre et de bois se brisent
devant lui, si les règles du monde le permettent.

### 4. Devour (Dévorer) — sans rage

**Forme humaine :** les tentacules **saisissent** un ennemi proche (à 3 blocs), le soulèvent, et il **le mord** à la gorge.
Quatre secondes, pendant lesquelles la cible ne peut rien faire, comme dans la pince du Roi Crabe.
- ×1,5 en trois morsures ; il regagne **30 % des dégâts** en vie, et **+25 de fureur**.
- Ne marche que sur un ennemi **immobilisé** (empalé, plaqué, étourdi, K.O.) ou plus faible d'un royaume. Sur un égal
  debout, les tentacules ne font que le repousser.
- **Sur un joueur à terre (K.O. PvP)**, c'est le **finish de l'art** : il est dévoré, en cinématique.
- **Les effets :** en vue de la victime, la gueule remplit l'écran. Les bords deviennent rouges et la vue tremble à chaque
  morsure.

**Forme du démon, Feast :** il dévore **n'importe qui à sa portée**, même debout (sauf un plus fort que lui). La victime est
plaquée sous ses pattes et mordue (×2,0). La forme regagne **+20 de rage**, donc dure plus longtemps.

### 5. Sovereign's Pressure (La Pression du Souverain) — coûte 40 de rage, le haki

**Forme humaine :** il se redresse, immobile, et **relâche sa volonté**. Une onde invisible se répand à 20 blocs. Selon
la différence de royaume (lue comme la perception du Qi) :
- **Deux royaumes ou plus en dessous** : la cible **tombe à genoux**, sans pouvoir rien faire, 4 s. Les mobs ordinaires
  **s'évanouissent** (étourdis 6 s) ou s'enfuient s'ils sont animaux.
- **Un royaume en dessous** : la cible est **écrasée** : Lenteur III, Faiblesse II, plus de sprint, 5 s.
- **Même royaume** : la cible est seulement **oppressée** : Lenteur I 3 s, et ses techniques coûtent 20 % de Qi en plus.
- **Plus fort que lui** : rien. Mais lui le sent, et sa vue tremble.

**Les effets, les plus importants de l'art :**
- au déclenchement, **le temps s'arrête une demi-seconde** autour de lui, puis l'onde part en **anneau d'air déformé** ;
- des **éclairs noirs et rouges** crépitent au sol et sur les blocs proches ;
- les feuilles tombent des arbres, l'herbe se couche, et le verre se fissure (sans casser) ;
- **pour ceux qui la subissent** : l'écran se **désature**, des veines rouges poussent depuis les bords, un battement de cœur
  lourd se fait entendre, la vue est tirée vers le sol, et un message en lettres manuscrites qui tremblent :
  « *You cannot breathe.* »

**Forme du démon, Sovereign's Dread :** la pression devient **l'effroi**. À 30 blocs, tous ceux d'en dessous **tombent à
genoux** 3 s, même un seul royaume plus bas. À royaume égal, la cible est **écrasée**. Et tant que la forme dure, les
ennemis qui le regardent dans les yeux sont ralentis.

### 6. Predator's Pounce (Le Bond du Prédateur) — coûte 25 de rage

**Forme humaine :** il se ramasse, les tentacules plantés derrière lui, puis **bondit** sur la cible (jusqu'à 12 blocs) les
tentacules en avant, et l'écrase au sol.
- ×1,0 et la cible est **plaquée au sol** 1 s (elle ne peut pas parer). Depuis un mur, le bond porte à 16 blocs.
- **Les effets :** une silhouette floue de rouge pendant l'élan ; à l'atterrissage, un cratère de fissures, et la caméra
  de la cible tremble fort.

**Forme du démon, Hunting Leap :** **trois bonds** à la suite, d'un ennemi à l'autre ou de mur en mur, sans toucher terre
entre deux (×0,9 chacun, plaqué 0,5 s). C'est la chasse : le démon traverse un groupe en une seconde.

### 7. Beast's Roar (Le Rugissement de la Bête) — coûte 35 de rage

**Forme humaine :** il **rugit** vers l'avant, la gueule grande ouverte, les tentacules écartés.
- Un **cône** de 10 blocs : ×0,8, et tout est **soufflé** à 8 blocs en arrière.
- **Brise la garde** : une garde levée est cassée et la cible est étourdie 1 s.
- Les blocs fragiles (verre, feuilles, herbe, fleurs) **volent en éclats** dans le cône, si les règles du monde le permettent.
- **Les effets :** une **onde de son visible**, en anneaux déformés qui partent de la gueule ; la poussière se soulève en
  cône ; un rugissement grave mêlé à des voix.

**Forme du démon, Cataclysm Roar :** le rugissement part **tout autour** de lui (12 blocs), souffle tout, casse toutes les
gardes et **éteint** les techniques en cours des ennemis proches (leur tenue s'arrête, comme Heaven's Drum ou Spinning
Rampart).

### 8. Demon Release (Le Démon libéré) — l'ultime

**Forme humaine :** à 100 de rage, au lieu de laisser la transformation venir, il **la force**. Toute la barre est dépensée,
et la forme du démon commence **avec un souffle** : une onde de pression à 20 blocs (comme Sovereign's Pressure) et une
explosion de chair autour de lui (×1,5 à 5 blocs). La forme qui suit dure **30 s au lieu de 20**.

**Forme du démon, Demon's Maw (La Bouche du Démon) :** une fois par forme.
- Il concentre dans sa gueule une **sphère** de chair et de Qi noir et rouge, qui grossit en 1,5 s.
- Il la crache : elle file 30 blocs et **explose** sur 8 blocs (×4,0 au centre, ×2,0 au bord).
- Le cratère reste, si les règles du monde le permettent.
- Elle met **fin à la forme** juste après : le démon a tout donné, et le contrecoup commence.
- Inspiration : la bombe de Kyubi.

**Les effets :**
- Au déclenchement de la forme, le **ciel s'assombrit** autour de lui (32 blocs) et un **pilier de lumière rouge** monte.
- La sphère attire les particules et la lumière autour d'elle avant de partir, avec une **lumière Photon** rouge qui grandit.

## Ce qu'il faudrait créer (quand ce sera validé)

- **Les modèles :**
  - la **kagune** (déjà en maquette : `scripts/concepts/kagune-preview.cjs`, `art/blockbench/kagune_preview.bbmodel`) à
    quatre tentacules, et à six pour la forme du démon ;
  - la **carapace** (sur le corps et les tentacules) ;
  - le **manteau** (une couche translucide sur le joueur, qui bouillonne) ;
  - la **sphère** de la Bouche du Démon ;
  - le **livre**.
- **Les animations Epic Fight :**
  - les M1 des tentacules (forme humaine) et les M1 de la bête (forme du démon) ;
  - les **seize** versions des techniques (huit par forme), au sol et en l'air ;
  - la **posture et la course à quatre pattes**, l'accroche aux murs et le bond ;
  - la transformation et le contrecoup.
- **La barre de fureur** dans le HUD, à côté de celle du Qi, discrète comme elle.
- **Les effets d'écran** pour ceux qui subissent la pression, le rugissement et la forme du démon : désaturation, veines
  rouges, battement de cœur, vignette rouge.
- **Les sons** (à créer ou à prendre dans Minecraft) : battement de cœur, fouet, rugissement, mâchoire.

## Questions pour Enzo

- **Le nom** : *Devouring Demon Art* (L'Art du Démon Dévoreur) te va ? Et *Fury* pour la barre de rage ?
- **Le royaume** : à partir de Foundation Establishment, ou plus tôt, comme le Sanguine Art (Qi Crystallization) ?
- **Où le trouver** : prison scellée, boss, ou le monde intérieur après avoir cédé au démon ?
- **La transformation à 100** : automatique (on peut la retenir 5 s), ou seulement quand le joueur la déclenche ?
- **Le contrecoup** : 3 s à genoux puis 45 s d'épuisement, c'est assez dur, ou trop ?
- **Les blocs** : le rugissement, la charge et la Bouche du Démon doivent-ils casser des blocs ?
