# L'Art du Démon Dévoreur (噬魔功, *Shì Mó Gōng*)

> **État : proposition, rien en jeu** (17/10/2026). Demandes d'Enzo :
> - « imagine un art martial autour de [la kagune], avec une barre de rage et des effets un peu comme du haki des rois » ;
> - « ne crée rien, fais comme la technique d'éclair pour me montrer tes idées » ;
> - « prends comme inspiration Kaneki de Tokyo Ghoul, le haki des rois, et la furie de Kyubi dans Naruto contre Pain » ;
> - « avec une barre de rage et des déplacements à quatre pattes, ça doit être terrifiant à affronter » ;
> - c'est une technique **de haut niveau, donc 8 techniques** ;
> - « j'aimerais qu'il ait des attaques sans rage et avec, et quand il est à 100 de rage tout change et il se met à quatre
>   pattes » ;
> - **troisième version** : « on doit plus jouer autour de la kagune ».
>
> Les inspirations sont des **idées**, pas des copies : ni le nom, ni l'apparence de ces personnages. Les noms en jeu sont en
> anglais : le nom anglais d'abord, la traduction entre parenthèses. Les chiffres sont un premier jet, alignés sur
> [../technique-balance.md](../technique-balance.md) et le Sanguine Art, l'autre art de haut niveau.

La maquette des tentacules au repos : `node scripts/concepts/kagune-preview.cjs --port=<Blockbench isolé>` écrit le projet
`art/blockbench/kagune_preview.bbmodel` (onglet Animer) et ses GIF dans `images/kagune-idle-*.gif`. Les images restent
locales, comme toutes celles des docs.

## L'idée : tout passe par la kagune

Dans les romans murim, les **arts démoniaques** (魔功) sont les plus redoutés : puissants, et qui dévorent leur maître.
Le cultivateur laisse un démon affamé pousser dans sa moelle. Il en tire **quatre tentacules de chair rouge** qui sortent
de ses reins : la **Moelle du Démon** (魔髓, *Demon Marrow*, la kagune qu'on a modélisée).

**La kagune est l'arme, le bouclier, les jambes et la gueule.** Le joueur ne frappe jamais de ses mains : chaque coup et
chaque technique est une **forme** que prend la kagune. Elle fouette, plonge dans le sol, s'agrippe, s'enroule, se ferme en
mâchoire, se soude en faux, se hérisse d'épines. Et à 100 de rage, elle **devient ses pattes**.

Les trois inspirations, et ce qu'on en garde :
- **Kaneki (Tokyo Ghoul)** : la kagune qui frappe, empale, saisit, dévore et change de forme, puis la carapace qui la
  couvre (le kakuja). C'est le cœur de l'art.
- **Le haki des rois (One Piece)** : la **pression** qui écrase. Ici, elle **sort de la kagune** quand elle éclate : à la
  transformation, et autour de la forme du démon.
- **La furie de Kyubi (Naruto contre Pain)** : la bête **à quatre pattes**, le manteau rouge qui bouillonne, la perte de
  contrôle, et la sphère de destruction dans la gueule (ici, une gueule faite de kagune).

## Le principe : deux formes

| | **La forme humaine** (*Marrow Form*) | **La forme du démon** (*Demon Form*), à 100 de rage |
|---|---|---|
| **La kagune** | **quatre tentacules** dans le dos, qui prennent la forme de chaque technique | **huit tentacules** : **quatre deviennent ses pattes**, quatre restent pour frapper |
| **Le corps** | debout | **à quatre pattes sur la kagune**, la carapace, le manteau rouge |
| **Les techniques** | cinq **sans rage**, deux qui **dépensent** la rage, l'ultime | **les huit changent** : la kagune prend des formes plus grandes |
| **La rage** | elle **monte** | elle **descend** ; la forme dure tant qu'il en reste |

**Pourquoi c'est terrifiant à affronter :** on **voit la rage dans la kagune**.
- Plus la barre monte, plus les tentacules s'agitent. Au-dessus de 80, ils claquent dans le vide comme s'ils avaient faim,
  et l'adversaire entend un **battement de cœur**.
- À 100, la kagune **éclate** : quatre nouveaux tentacules sortent, s'enfoncent dans le sol, et le soulèvent. La bête
  marche alors sur des pattes de chair.
- Sur ses pattes, il court plus vite qu'un cheval, grimpe aux murs et passe de l'un à l'autre.

**Sa faiblesse :** la kagune est **son seul membre**.
- **Tranchée, elle repousse** : un coup puissant qui touche un tentacule pendant une technique le coupe (une seule fois
  par technique), et la technique s'arrête. Il repousse en 3 s.
- Hors combat, la rage retombe vite.
- En forme du démon, il ne peut ni parer ni fuir, et à la fin il s'effondre (voir « Le contrecoup »).

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
| Un M1 de kagune qui touche | +3 |
| Une technique **sans rage** qui touche | +8 |
| Recevoir un coup | +1 par point de vie perdu |
| Dévorer (technique 5) | +25 |
| Tuer un ennemi | +10 |
| Une parade parfaite de la kagune | +10 |

- Elle **retombe** de 4 par seconde après 5 s sans frapper ni être frappé.
- Les techniques à rage (6 et 7) en **coûtent** une part.
- **À 100, la kagune éclate toute seule**, sauf si on garde la touche de l'art enfoncée pour la retenir (au plus 5 s : le
  démon finit toujours par sortir).

**En forme du démon, elle descend :** −5 par seconde (20 s de forme si personne ne la nourrit) ; chaque coup qui touche rend
+2 ; à 0, la forme prend fin.

## Le passif : *Demon Marrow* (La Moelle du Démon)

- **Les M1 sont faits par la kagune**, les mains vides. Un enchaînement à nous de quatre coups : deux fouets en croix, un
  coup d'estoc à deux tentacules, puis un balayage circulaire à 360°. La kagune frappe à **4 blocs**.
- **La garde, c'est la kagune** : les quatre tentacules se croisent devant le corps en bouclier. Une parade parfaite donne
  **+10 de fureur** et un tentacule fouette l'attaquant en retour.
- **Elle se nourrit** : chaque coup de kagune qui touche rend 2 % des dégâts infligés en vie.
- **Elle grimpe** : en forme humaine aussi, la kagune peut s'agripper à un mur pour **s'y tenir** (tenir la touche de saut
  contre un mur), mais pas courir dessus.
- **Hors combat**, les tentacules se rétractent dans le dos en 2 s, sauf si on tient la touche de l'art.

## Les huit techniques

La **part** suit le Sanguine Art (mains nues durcies : environ 16 à Qi Crystallization, un peu plus haut à Foundation
Establishment). Le garde-fou commun (`BurstGuard`) reste : aucun enchaînement ne tue un joueur en un instant.

| # | Forme humaine : la kagune devient… | Rage | Qi | Recharge | Forme du démon (la même touche) |
|---|---|---|---|---|---|
| 1 | **Crimson Lash** (Le Fouet cramoisi) : **des fouets** | donne +8 | 150 | 6 s | **Lash Storm** (La Tempête de fouets) |
| 2 | **Impaling Thorns** (Les Épines qui empalent) : **des pieux sous terre** | donne +8 | 200 | 10 s | **Thorn Forest** (La Forêt d'épines) |
| 3 | **Marrow Grapple** (Le Grappin de moelle) : **des grappins** | donne +5 | 120 | 5 s | **Web Swing** (La Toile) |
| 4 | **Centipede Coil** (Le Cocon du Mille-Pattes) : **un cocon** | donne +5 par coup arrêté | 300 | 18 s | **Drill Charge** (La Charge en vrille) |
| 5 | **Devour** (Dévorer) : **une mâchoire** | donne +25 | 250 | 20 s | **Feast** (Le Festin) |
| 6 | **Twin Scythes** (Les Faux jumelles) : **deux faux** | coûte 30 | 350 | 14 s | **Reaper's Wheel** (La Roue de la Faucheuse) |
| 7 | **Thousand Thorns** (Les Mille Épines) : **des épines tirées** | coûte 35 | 400 | 16 s | **Thorn Barrage** (Le Déluge d'épines) |
| 8 | **Kagune Burst** (L'Éclatement de la kagune) : **elle éclate** | toute la barre | 900 | 10 min | **Demon's Maw** (La Bouche du Démon) |

En forme du démon, les techniques **ne coûtent plus de rage** (seulement du Qi, deux fois moins) et rechargent deux fois
plus vite.

### 1. Crimson Lash (Le Fouet cramoisi) — la kagune en fouets, sans rage

**Forme humaine :** les quatre tentacules se dressent derrière lui, puis **fouettent l'un après l'autre** devant lui en
éventail, de gauche à droite, chacun un peu plus loin (de 3 à 6 blocs).
- 4 × 0,5 dans un éventail de 90° sur 6 blocs, et **+8 de fureur** s'il touche.
- **Les effets :** chaque fouet laisse une **traînée rouge** courbe, en ruban pixelisé rouge sang ; un claquement, et
  l'écran tremble pour ceux qui sont touchés.

**Forme du démon, Lash Storm :** les quatre tentacules libres fouettent **tout autour**, sans s'arrêter, pendant 2 s, en
tournoyant au-dessus de la bête (8 × 0,4 à 7 blocs).

### 2. Impaling Thorns (Les Épines qui empalent) — la kagune en pieux, sans rage

**Forme humaine :** deux tentacules **plongent dans le sol** devant lui et **jaillissent sous la cible** en pieux (jusqu'à
10 blocs).
- La cible est **empalée** : ×1,2 et clouée 1,5 s. Puis les tentacules la **tirent jusqu'à ses pieds**, prête pour Devour.
- **L'avertissement :** la terre se fend en cercle sous la cible une demi-seconde avant.

**Forme du démon, Thorn Forest :** les quatre pattes s'enfoncent dans le sol. **Une forêt de pieux** jaillit autour de lui
sur 8 blocs, en trois vagues qui s'éloignent (3 × 0,7). La dernière vague cloue 1 s.

### 3. Marrow Grapple (Le Grappin de moelle) — la kagune en grappins, sans rage

**Forme humaine :** un tentacule **file droit devant** (jusqu'à 16 blocs) et s'accroche à ce qu'il touche.
- **Sur un bloc** : la kagune le **tire jusque-là**, très vite. C'est le déplacement de l'art (passer un fossé, monter sur
  un toit, fuir).
- **Sur un ennemi** : ×0,6, et elle le **tire à lui** (ou se tire jusqu'à lui s'il est plus lourd, comme un boss).
- Deux charges : on peut s'accrocher deux fois à la suite.

**Forme du démon, Web Swing :** il lance ses **quatre tentacules libres** à la fois, sur les murs, les arbres et le sol, et
**se balance** d'une prise à l'autre au-dessus du combat, comme une araignée. Retomber sur un ennemi l'écrase (×1,0).

### 4. Centipede Coil (Le Cocon du Mille-Pattes) — la kagune en cocon, sans rage

**Forme humaine :** les quatre tentacules **s'enroulent autour de lui** en spirale serrée, comme un cocon, et tournent. Tenu
jusqu'à 3 s.
- **Arrête tout ce qui vient de toutes les directions** (coups et projectiles) tant qu'il tient.
- **Chaque coup arrêté est renvoyé** : un tentacule sort du cocon et frappe celui qui a frappé (0,4). +5 de fureur par
  coup arrêté.
- Au relâché, la kagune se déroule d'un coup : un balayage à 360° (×0,8) qui repousse tout.
- Le coût : du Qi au départ, puis 30 par seconde tenue (comme le Spinning Rampart du bâton).

**Forme du démon, Drill Charge :** les quatre tentacules libres **se tressent en vrille** devant lui. Il charge sur ses
pattes (12 blocs), la vrille en avant, et **perce** tout sur son passage (×1,4, projeté). Les murs de terre et de bois se
brisent, si les règles du monde le permettent.

### 5. Devour (Dévorer) — la kagune en mâchoire, sans rage

**Forme humaine :** les quatre tentacules **s'ouvrent comme une mâchoire** autour d'un ennemi proche (à 4 blocs), se
referment sur lui et le soulèvent. Quatre secondes, et la cible ne peut rien faire, comme dans la pince du Roi Crabe.
- La mâchoire serre trois fois (×1,5 en tout) ; il regagne **30 % des dégâts** en vie et **+25 de fureur**.
- Ne marche que sur un ennemi **immobilisé** (empalé, plaqué, étourdi, K.O.) ou plus faible d'un royaume. Sur un égal
  debout, la mâchoire ne fait que le repousser.
- **Sur un joueur à terre (K.O. PvP)**, c'est le **finish de l'art** : la kagune l'avale, en cinématique.
- **Les effets :** en vue de la victime, les tentacules se referment sur l'écran ; les bords deviennent rouges et la vue
  tremble à chaque serrage.

**Forme du démon, Feast :** la mâchoire attrape **n'importe qui à sa portée**, même debout (sauf un plus fort que lui), et
le serre sous la bête (×2,0). La forme regagne **+20 de rage**, donc dure plus longtemps.

### 6. Twin Scythes (Les Faux jumelles) — la kagune en faux, coûte 30 de rage

**Forme humaine :** deux tentacules descendent le long de ses bras et **se soudent en deux grandes faux** de chair durcie,
lames courbes à la place des mains, pendant 8 s.
- Les M1 deviennent des **coups de faux** : plus lents, ×1,6 chacun, et ils traversent la garde à moitié.
- Relancer la technique pendant ces 8 s : un **double coup en croix** qui envoie deux lames de rage (×1,2, 10 blocs).
- **Les effets :** les faux luisent sur le fil ; chaque coup laisse une longue traînée rouge sombre.

**Forme du démon, Reaper's Wheel :** les quatre tentacules libres deviennent **quatre faux** et la bête **tournoie** sur ses
pattes, une roue de lames à 5 blocs (6 × 0,6 en 1,5 s).

### 7. Thousand Thorns (Les Mille Épines) — la kagune en épines tirées, coûte 35 de rage

**Forme humaine :** les quatre tentacules se dressent, **se hérissent d'épines** comme un porc-épic, puis les **tirent** vers
la cible, en salve (24 épines en 1,5 s, ×0,15 chacune, 20 blocs).
- Chaque épine qui touche **reste plantée** et ralentit ; à 6 épines, la cible est **clouée** 1 s.
- Elles se parent une à une, et s'arrêtent sur les murs.
- **Les effets :** les épines sont de petits modèles rouges et pointus ; elles restent plantées dans le sol 3 s.

**Forme du démon, Thorn Barrage :** les tentacules tirent leurs épines **tout autour** et **vers le ciel** : elles
retombent en pluie sur un cercle de 10 blocs (60 épines en 3 s).

### 8. Kagune Burst (L'Éclatement de la kagune) — l'ultime

**Forme humaine :** à 100 de rage, au lieu de laisser la transformation venir, il **la force**. Toute la barre est dépensée.
- La kagune **éclate** : une explosion de chair à 5 blocs (×1,5), et la **pression du souverain** se répand à 20 blocs
  (voir plus bas).
- La forme du démon qui suit dure **30 s au lieu de 20**.

**Forme du démon, Demon's Maw (La Bouche du Démon) :** une fois par forme.
- Les quatre tentacules libres se referment devant sa tête en **une gueule** de chair.
- Dans la gueule, une **sphère** de Qi noir et rouge grossit en 1,5 s.
- La gueule la crache : elle file 30 blocs et **explose** sur 8 blocs (×4,0 au centre, ×2,0 au bord). Le cratère reste, si
  les règles du monde le permettent.
- Elle met **fin à la forme** juste après : le démon a tout donné, et le contrecoup commence.
- Inspiration : la bombe de Kyubi.

## La forme du démon : *Demon Form*

**La transformation (à 100 de rage, ou par Kagune Burst) :**
- Le joueur se tient la tête à deux mains et **hurle**. Le monde s'arrête une demi-seconde : les particules se figent et
  le son se coupe.
- **Quatre nouveaux tentacules** jaillissent de ses reins, s'enfoncent dans le sol devant et derrière lui, et le
  **soulèvent** : ce sont ses pattes. Les quatre premiers restent dressés au-dessus de lui.
- La **carapace** noire et rouge recouvre la kagune et la moitié de son corps, en plaques et en épines.
- Le **manteau** rouge et noir bouillonne sur sa peau comme de la lave. Là où il passe, l'herbe noircit.
- Une courte cinématique de 2 s pour tous ceux qui sont à 32 blocs.

**La pression du souverain** (le haki) sort de la kagune au moment où elle éclate, à 20 blocs, puis reste autour de la
bête, plus faible, à 12 blocs. Selon la différence de royaume (lue comme la perception du Qi) :
- **Deux royaumes ou plus en dessous** : la cible **tombe à genoux** 4 s (ensuite, tant qu'elle reste à 12 blocs, elle est
  ralentie). Les mobs ordinaires **s'évanouissent** ou s'enfuient.
- **Un royaume en dessous** : la cible est **écrasée** : Lenteur III, Faiblesse II, plus de sprint, 5 s.
- **Même royaume** : la cible est **oppressée** : Lenteur I 3 s, et ses techniques coûtent 20 % de Qi en plus.
- **Plus fort que lui** : rien.
- **Les effets :** l'onde part en **anneau d'air déformé**, des **éclairs noirs et rouges** crépitent au sol, les feuilles
  tombent et le verre se fissure. Pour ceux qui la subissent, l'écran se **désature**, des veines rouges poussent depuis
  les bords, et un message en lettres manuscrites qui tremblent : « *You cannot breathe.* »

**Ce que donne la forme :**
- +40 % de dégâts, 30 % de dégâts reçus en moins ; il guérit de 8 % de chaque coup.
- Les **M1** deviennent des coups des quatre tentacules libres, plus larges et plus longs (6 blocs).
- **Les huit techniques changent** (voir chacune plus haut).
- **Il ne peut plus parer, ni ranger l'art, ni fuir.**

**La démarche sur les pattes de kagune :**
- Il avance **porté par les quatre tentacules-pattes**, le corps penché en avant, les mains au sol entre elles : à quatre
  pattes comme une bête, mais haut sur la chair.
- **+50 % de vitesse de sprint**, sans consommer la faim de Minecraft. **Saut : un bond** de 6 blocs devant, ou de 4 en
  hauteur.
- **Les murs** : les pattes s'y plantent et il **grimpe** dessus, puis bondit d'un mur à l'autre.
- **Ce qu'on voit et entend :** des **trous** dans le sol à chaque pas des pattes, qui restent 2 s ; de la poussière ; la
  traînée rouge de l'œil la nuit ; des **pas lourds et rapides** qui s'entendent derrière les murs.
- **À faire en jeu :** une posture et une course à nous dans Epic Fight (comme le K.O., qui rampe déjà), les pattes de
  kagune animées en marche (comme les pattes du Roi Crabe), la caméra plus haute et en arrière.

**Pour l'adversaire, tant que la forme dure :** une vignette rouge sur les bords de l'écran et un battement de cœur qui ne
s'arrête pas, dès qu'il est à 24 blocs du démon.

**Le contrecoup :** à la fin de la forme, la kagune se **rétracte d'un coup** et le démon rend le corps.
- Le joueur tombe **à genoux 3 s**, sans défense.
- Puis il reste **épuisé** 45 s : la fureur ne monte plus, Lenteur I, pas de régénération de Qi, et la kagune n'a plus que
  **deux** tentacules.
- C'est le moment où l'adversaire peut le tuer : il **faut** avoir gagné avant.

## Ce qu'il faudrait créer (quand ce sera validé)

- **Les modèles :**
  - la **kagune** (déjà en maquette : `scripts/concepts/kagune-preview.cjs`, `art/blockbench/kagune_preview.bbmodel`) à
    quatre tentacules, et à huit pour la forme du démon, avec les **pattes** ;
  - les formes qu'elle prend : la mâchoire, les faux, la vrille, le cocon, les épines (petits modèles tirés) ;
  - la **carapace** (sur la kagune et le corps) ;
  - le **manteau** (une couche translucide sur le joueur, qui bouillonne) ;
  - la **sphère** de la Bouche du Démon ;
  - le **livre**.
- **Les animations** (Epic Fight pour le joueur, os de la kagune pour les tentacules, comme la maquette) :
  - les M1 de la kagune (forme humaine) et ceux de la forme du démon ;
  - les **seize** versions des techniques (huit par forme), au sol et en l'air ;
  - la **marche sur les pattes de kagune**, l'accroche aux murs et le bond ;
  - la transformation et le contrecoup.
- **La barre de fureur** dans le HUD, à côté de celle du Qi, discrète comme elle.
- **Les effets d'écran** pour ceux qui subissent la pression et la forme du démon : désaturation, veines rouges, battement
  de cœur, vignette rouge.
- **Les sons** (à créer ou à prendre dans Minecraft) : battement de cœur, fouet, chair qui se déchire, mâchoire,
  rugissement.

## Questions pour Enzo

- **Le nom** : *Devouring Demon Art* (L'Art du Démon Dévoreur) te va ? Et *Fury* pour la barre de rage ?
- **Le royaume** : à partir de Foundation Establishment, ou plus tôt, comme le Sanguine Art (Qi Crystallization) ?
- **Où le trouver** : prison scellée, boss, ou le monde intérieur après avoir cédé au démon ?
- **La transformation à 100** : automatique (on peut la retenir 5 s), ou seulement quand le joueur la déclenche ?
- **La kagune tranchée** : on garde cette faiblesse (un coup puissant coupe un tentacule et arrête la technique) ?
- **Les blocs** : la vrille et la Bouche du Démon doivent-elles casser des blocs ?
