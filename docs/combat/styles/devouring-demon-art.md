# L'Art du Démon Dévoreur (噬魔功, *Shì Mó Gōng*)

> **État : proposition, rien en jeu** (17/10/2026). Demandes d'Enzo :
> - « imagine un art martial autour de [la kagune], avec une barre de rage et des effets un peu comme du haki des rois » ;
> - « ne crée rien, fais comme la technique d'éclair pour me montrer tes idées » ;
> - « prends comme inspiration Kaneki de Tokyo Ghoul, le haki des rois, et la furie de Kyubi dans Naruto contre Pain » ;
> - « avec une barre de rage et des déplacements à quatre pattes, ça doit être terrifiant à affronter » ;
> - c'est une technique **de haut niveau, donc 8 techniques** ;
> - « j'aimerais qu'il ait des attaques sans rage et avec, et quand il est à 100 de rage tout change et il se met à quatre
>   pattes » ;
> - « on doit plus jouer autour de la kagune » ;
> - **quatrième version** : « non, nouvelles idées de skills, n'oublie pas le haki des rois ».
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
chaque technique est une **forme** que prend la kagune. Elle s'élève en cage, creuse le sol, empale et manie ses ennemis
comme des pantins, les draine par des fils, s'arrache pour leurrer, et se couvre d'éclairs noirs quand il y coule son haki. Et à 100 de rage, elle **devient ses pattes**.

Les trois inspirations, et ce qu'on en garde :
- **Kaneki (Tokyo Ghoul)** : la kagune qui frappe, empale, saisit, dévore et change de forme, puis la carapace qui la
  couvre (le kakuja). C'est le cœur de l'art.
- **Le haki des rois (One Piece)** : la **volonté** qui écrase. Ici, elle **passe par la kagune** : il la libère depuis une
  couronne de tentacules (Sovereign's Will), il la coule dans la kagune pour frapper sans toucher (Sovereign's Mantle), et
  la forme du démon la porte en permanence.
- **La furie de Kyubi (Naruto contre Pain)** : la bête **à quatre pattes**, le manteau rouge qui bouillonne, la perte de
  contrôle, et la sphère de destruction dans la gueule (ici, une gueule faite de kagune).

## Le principe : deux formes

| | **La forme humaine** (*Marrow Form*) | **La forme du démon** (*Demon Form*), à 100 de rage |
|---|---|---|
| **La kagune** | **quatre tentacules** dans le dos, qui prennent la forme de chaque technique | **huit tentacules** : **quatre deviennent ses pattes**, quatre restent pour frapper |
| **Le corps** | debout | **à quatre pattes sur la kagune**, la carapace, le manteau rouge |
| **Les techniques** | cinq **sans rage**, deux de **haki** qui **dépensent** la rage, l'ultime | **toutes les attaques changent** : huit attaques de bête, sur les mêmes touches |
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
| Les Liens de sang (technique 4) | +3 par seconde et par lien |
| L'Offrande tranchée (technique 5) | +15 |
| Tuer un ennemi | +10 |
| Une parade parfaite de la kagune | +10 |

- Elle **retombe** de 4 par seconde après 5 s sans frapper ni être frappé.
- Les techniques de haki (6 et 7) en **coûtent** une part.
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

> **Quatrième version** (demande d'Enzo : « non, nouvelles idées de skills, n'oublie pas le haki des rois ») : huit
> techniques nouvelles, toutes faites par la kagune, dont **deux de haki des rois**. Le haki n'est plus un simple effet :
> c'est une volonté qu'on **libère** (technique 6) ou qu'on **coule dans la kagune** (technique 7), et la forme du démon la
> porte en permanence.

La **part** suit le Sanguine Art (mains nues durcies : environ 16 à Qi Crystallization, un peu plus haut à Foundation
Establishment). Le garde-fou commun (`BurstGuard`) reste : aucun enchaînement ne tue un joueur en un instant.

| # | Forme humaine | Rage | Qi | Recharge | Forme du démon (la même touche, une autre attaque) |
|---|---|---|---|---|---|
| 1 | **Crimson Cage** (La Cage cramoisie) | donne +8 | 200 | 12 s | **Rending Pounce** (Le Bond qui déchire) |
| 2 | **Burrowing Centipede** (Le Mille-Pattes fouisseur) | donne +8 | 180 | 9 s | **Stampede** (La Charge de la bête) |
| 3 | **Marionette** (Le Pantin) | donne +10 | 250 | 16 s | **Piercing Legs** (Les Pattes qui transpercent) |
| 4 | **Blood Tethers** (Les Liens de sang) | donne +3 par seconde liée | 220 | 18 s | **Feast** (Le Festin) |
| 5 | **Severed Offering** (L'Offrande tranchée) | donne +15 | 150 + 10 % de vie | 20 s | **Wall Ambush** (L'Embuscade du mur) |
| 6 | **Sovereign's Will** (La Volonté du Souverain), **haki** | coûte 40 | 400 | 30 s | **Demon King's Roar** (Le Rugissement du Roi Démon) |
| 7 | **Sovereign's Mantle** (Le Manteau du Souverain), **haki dans la kagune** | coûte 30 | 350 | 25 s | **Black Lightning Storm** (L'Orage d'éclairs noirs) |
| 8 | **Devourer's Crown** (La Couronne du Dévoreur) | toute la barre | 900 | 10 min | **Demon's Maw** (La Bouche du Démon) |

En forme du démon, **chaque touche lance une autre attaque** : voir « Les huit attaques de la forme du démon ».

### 1. Crimson Cage (La Cage cramoisie) — sans rage

**Forme humaine :** il plante ses quatre tentacules dans le sol. Ils filent sous terre et **jaillissent en cercle** autour de
la cible (jusqu'à 14 blocs), en huit barreaux courbes qui se referment au-dessus d'elle comme une cage.
- La cible est **enfermée** 4 s dans 5 blocs de large : elle ne peut pas en sortir, et toucher un barreau fait mal (×0,3,
  repoussé vers le centre).
- Pendant la cage, le joueur peut frapper **à travers** les barreaux avec les M1 : ses coups traversent sa propre chair.
- Un coup puissant de la cible casse un barreau : elle peut sortir par le trou.
- **L'avertissement :** le sol se fend en anneau une demi-seconde avant, ce qui laisse une chance de sauter dehors.


### 2. Burrowing Centipede (Le Mille-Pattes fouisseur) — sans rage

**Forme humaine :** les tentacules se tressent en vrille sous lui et le **tirent dans le sol**. Il **disparaît sous terre**
1,5 s, file jusqu'à 12 blocs (on voit une bosse de terre qui court vers la cible), puis **jaillit** sous elle.
- À la sortie : ×1,2, la cible est **projetée en l'air**, prête pour un coup en l'air.
- Sous terre, il ne prend aucun dégât, mais il ne voit que la bosse qui avance.
- Ne passe pas sous la pierre dure (obsidienne, bedrock) : il ressort avant.


### 3. Marionette (Le Pantin) — sans rage

**Forme humaine :** deux tentacules **transpercent** un ennemi proche (à 5 blocs) et **le soulèvent**. Le joueur s'en sert
comme d'une arme : il **le balance** sur les autres pendant 3 s, puis le **jette** (20 blocs).
- La cible prend ×0,8, puis ×0,6 à l'atterrissage. Chaque ennemi qu'elle percute prend ×0,6 et tombe.
- Ne prend qu'un ennemi **plus faible d'un royaume** ou immobilisé. Un égal debout se dégage d'un coup de garde.
- Contre un boss, les tentacules s'y accrochent au lieu de le soulever, et **tirent le joueur** jusqu'à lui.


### 4. Blood Tethers (Les Liens de sang) — sans rage

**Forme humaine :** les quatre tentacules se divisent en **quatre fils rouges** qui filent chacun vers un ennemi différent
(10 blocs) et s'y plantent. Tenu jusqu'à 6 s.
- Chaque lien **draine** sa cible : ×0,15 par seconde, et le joueur en regagne la moitié en vie.
- **+3 de fureur par seconde** et par lien.
- Une cible peut **casser son lien** en s'éloignant à plus de 14 blocs, ou en le tranchant (un coup puissant sur le fil).
- Pendant la tenue, le joueur peut marcher lentement mais pas frapper.


### 5. Severed Offering (L'Offrande tranchée) — sans rage, coûte de la vie

**Forme humaine :** il **arrache lui-même** un de ses tentacules (10 % de sa vie) et le jette au sol devant lui. Le morceau
**vit tout seul** 8 s.
- Le tentacule tranché **se tord** et fouette ce qui l'approche (×0,4 par coup, à 3 blocs).
- Les mobs le prennent pour cible : il **attire** leur attention.
- Le joueur peut le **faire éclater** en relançant la technique : une explosion de chair (×1,2 à 4 blocs) qui ralentit.
- Le tentacule du joueur repousse en 4 s. Pendant ce temps, il n'en a que trois.
- **+15 de fureur** : le démon aime sa propre douleur.


### 6. Sovereign's Will (La Volonté du Souverain) — coûte 40 de rage, le haki des rois

**Forme humaine :** les quatre tentacules **se déploient en couronne** au-dessus de sa tête, grands ouverts, immobiles. Il
fixe ses ennemis et **libère sa volonté**. L'onde part de la couronne de kagune et se répand à 20 blocs.

Ce que fait la volonté, selon la différence de royaume (lue comme la perception du Qi) :
- **Deux royaumes ou plus en dessous** : la cible **tombe à genoux**, sans pouvoir rien faire, 4 s. Les mobs ordinaires
  **s'évanouissent** (étourdis 6 s), les animaux s'enfuient.
- **Un royaume en dessous** : la cible est **écrasée** : Lenteur III, Faiblesse II, plus de sprint, 5 s.
- **Même royaume** : les deux volontés **se heurtent**. La cible est **oppressée** (Lenteur I 3 s, techniques à 20 % de Qi
  en plus). Si elle a elle-même un art de haki, c'est un **choc des souverains** (voir plus bas).
- **Plus fort que lui** : rien. Mais lui le sent, et sa vue tremble.

**Les effets, les plus importants de l'art :**
- au déclenchement, **le temps s'arrête une demi-seconde** autour de lui : les particules se figent, le son se coupe ;
- puis l'onde part en **anneau d'air déformé** depuis la couronne, et des **éclairs noirs et rouges** crépitent le long des
  tentacules et jusqu'au sol ;
- les feuilles tombent des arbres, l'herbe se couche, le verre se fissure (sans casser), et les nuages juste au-dessus
  s'écartent ;
- **pour ceux qui la subissent** : l'écran se **désature**, des veines rouges poussent depuis les bords, un battement de cœur
  lourd se fait entendre, la vue est tirée vers le sol, et un message en lettres manuscrites qui tremblent :
  « *You cannot breathe.* »

**Le choc des souverains :** deux joueurs de même royaume qui lancent un haki dans la même seconde. Leurs ondes se rencontrent
à mi-chemin : un **mur d'éclairs noirs** entre eux, le ciel se fend, et pendant 2 s, personne autour ne peut bouger. Le plus
haut en Qi l'emporte et l'autre est repoussé.


### 7. Sovereign's Mantle (Le Manteau du Souverain) — coûte 30 de rage, le haki dans la kagune

**Forme humaine :** il **coule sa volonté dans la kagune**. Pendant 12 s, les tentacules se couvrent d'un **manteau d'éclairs
noirs** qui crépitent le long de leurs écailles.
- Les coups de kagune **frappent sans toucher** : chaque M1 et chaque technique libère un **arc noir** qui porte 3 blocs plus
  loin que le tentacule et frappe aussi celui qui est juste derrière la cible.
- Les coups **traversent la garde à moitié** : une garde levée n'arrête que la moitié des dégâts.
- Un ennemi **plus faible d'un royaume** touché par un coup chargé de haki est **étourdi** 1 s.
- Les tentacules **ne peuvent plus être tranchés** pendant le manteau.
- **Les effets :** la kagune vire au rouge très sombre, presque noir, avec des éclairs noirs qui sautent d'un tentacule à
  l'autre ; à chaque coup, un éclair noir zèbre l'air jusqu'à la cible, avec une lumière Photon rouge sombre.
- **L'inspiration :** le haki des rois « enrobé » des plus grands, qui frappe sans contact.


### 8. Devourer's Crown (La Couronne du Dévoreur) — l'ultime

**Forme humaine :** à 100 de rage, au lieu de laisser la transformation venir, il **la force**. Toute la barre est dépensée.
- La kagune **éclate** : une explosion de chair à 5 blocs (×1,5).
- La **Volonté du Souverain** (technique 6) part d'elle-même, sans rage ni recharge, à 30 blocs.
- La forme du démon qui suit dure **30 s au lieu de 20**, et commence avec le **Manteau du Souverain** (technique 7) déjà en
  place.


### Les idées en réserve (pour remplacer une technique si tu préfères)

- **Kagune Wings** (Les Ailes de kagune) : les tentacules s'aplatissent en **ailes** de chair pour planer et frapper en
  piqué.
- **Fang Drill** (La Vrille-croc) : les quatre tentacules se tressent en vrille devant lui et il charge, perçant tout.
- **Twin Scythes** (Les Faux jumelles) : deux tentacules se soudent en faux le long des bras pendant 8 s.
- **Thousand Thorns** (Les Mille Épines) : la kagune se hérisse et tire une salve d'épines qui restent plantées.
- **Devour** (Dévorer) : la kagune se ferme en mâchoire sur un ennemi immobilisé et le serre pour soigner.
- **King's Gaze** (Le Regard du Roi), haki : il fixe **une seule** cible ; si elle est plus faible, elle reste figée tant
  qu'il la regarde (3 s au plus).

## Les huit attaques de la forme du démon

> Demande d'Enzo : « et quand la rage est au max, ça change toutes les attaques ». À 100 de rage, **les huit touches
> changent d'attaque** : ce ne sont plus les techniques de l'homme en plus grand, mais celles d'une **bête** qui chasse à
> quatre pattes sur sa kagune. Elles ne coûtent pas de rage (seulement du Qi, deux fois moins) et rechargent vite : la
> bête frappe sans compter. Les M1 changent aussi.

**Les M1 de la bête :** un enchaînement de cinq coups. Deux coups de griffes en avançant, une morsure, puis les quatre
tentacules libres s'abattent en croix devant elle, et un dernier coup de queue de kagune à 360°. Plus rapides et plus
larges (6 blocs) que ceux de l'homme.

| Touche | Forme humaine | Forme du démon | Qi | Recharge | Dégâts (× la part) | Rôle |
|---|---|---|---|---|---|---|
| 1 | Crimson Cage | **Rending Pounce** (Le Bond qui déchire) | 100 | 4 s | 1,0 + 3 × 0,4 | Fondre sur une proie |
| 2 | Burrowing Centipede | **Stampede** (La Charge de la bête) | 120 | 7 s | 1,2, renversé | Traverser, écraser |
| 3 | Marionette | **Piercing Legs** (Les Pattes qui transpercent) | 120 | 6 s | 6 × 0,35 | Piétiner, clouer |
| 4 | Blood Tethers | **Feast** (Le Festin) | 150 | 12 s | 2,0 + soin, +20 de rage | Se nourrir, durer |
| 5 | Severed Offering | **Wall Ambush** (L'Embuscade du mur) | 100 | 6 s | 1,4, plaqué | Tomber d'en haut |
| 6 | Sovereign's Will | **Demon King's Roar** (Le Rugissement du Roi Démon), **haki** | 250 | 20 s | 0,8 + à genoux | Faire plier tout le monde |
| 7 | Sovereign's Mantle | **Black Lightning Storm** (L'Orage d'éclairs noirs), **haki** | 220 | 16 s | 8 × 0,3 | Zone autour de soi |
| 8 | Devourer's Crown | **Demon's Maw** (La Bouche du Démon) | 450 | une fois par forme | 4,0 au centre | Tout finir |

### 1. Rending Pounce (Le Bond qui déchire)

La bête se tasse sur ses pattes de kagune, puis **bondit** sur la proie (jusqu'à 14 blocs, 20 depuis un mur). Elle
l'écrase sous elle (×1,0, plaquée 1 s), puis la **lacère** trois fois des griffes et des tentacules (3 × 0,4) avant de
repartir d'un bond en arrière.
- **Les effets :** une traînée rouge pendant le bond, et des griffures qui brûlent au sol 1 s.

### 2. Stampede (La Charge de la bête)

Elle baisse la tête et **charge** sur 16 blocs, ses quatre pattes de kagune martelant le sol. Tout ce qui est sur son chemin
est **renversé** et piétiné (×1,2). Elle peut tourner pendant la charge, en suivant la visée.
- Les murs de terre, de bois et de verre se brisent devant elle, si les règles du monde le permettent.
- **Les effets :** le sol tremble pour tous ceux qui sont à 12 blocs, et chaque pas laisse un trou.

### 3. Piercing Legs (Les Pattes qui transpercent)

La bête se dresse sur ses deux pattes arrière de kagune et **abat les deux pattes avant** comme des pieux, encore et encore,
en avançant : six coups en 1,5 s sur 4 blocs devant elle (6 × 0,35).
- Chaque coup qui touche **cloue** la cible 0,3 s : à trois coups, elle ne peut plus bouger.
- **L'inspiration :** une araignée géante qui frappe de ses pattes.

### 4. Feast (Le Festin)

Les quatre tentacules libres **se referment en gueule** sur n'importe quel ennemi à 4 blocs, même debout (sauf un plus fort
que lui), et le **plaquent sous la bête**, qui le dévore (×2,0 en trois morsures).
- Elle regagne **30 % des dégâts** en vie, et **+20 de rage** : la forme dure plus longtemps.
- **Sur un joueur à terre (K.O. PvP)**, c'est le finish de l'art, en cinématique.
- **Les effets :** en vue de la victime, la gueule de kagune remplit l'écran, et les bords deviennent rouges à chaque
  morsure.

### 5. Wall Ambush (L'Embuscade du mur)

Accrochée à un mur ou à un plafond (ou depuis n'importe quelle hauteur), la bête **se laisse tomber** sur la cible visée en
dessous, toutes pattes en avant (×1,4, plaquée 1,5 s). Au sol, la touche la fait bondir sur le mur le plus proche (jusqu'à
10 blocs) pour s'y accrocher.
- Une ombre grandit sous la cible une demi-seconde avant : c'est la seule chance d'esquiver.
- **Pour la cible :** un grincement au-dessus d'elle, puis le battement de cœur s'arrête net juste avant l'impact.

### 6. Demon King's Roar (Le Rugissement du Roi Démon) — le haki des rois

La bête lève la tête, les huit tentacules dressés en couronne, et **rugit**. Le rugissement porte sa volonté sur **30 blocs**
tout autour, et il ne connaît plus de pitié :
- **Un royaume ou plus en dessous** : **à genoux** 4 s, sans pouvoir rien faire. Les mobs ordinaires s'évanouissent.
- **Même royaume** : la cible est **écrasée** (Lenteur III, Faiblesse II, plus de sprint, 4 s), sa garde est brisée, et ses
  techniques tenues s'arrêtent.
- **Plus fort que lui** : il est seulement **repoussé** de 6 blocs.
- Tous ceux qui sont à 8 blocs prennent ×0,8 et sont soufflés.
- **Les effets :** le temps se fige une demi-seconde ; une onde de son visible, en anneaux déformés ; des éclairs noirs et
  rouges qui fendent le ciel au-dessus de la bête ; les arbres proches perdent leurs feuilles d'un coup. Pour ceux qui la
  subissent : écran désaturé, veines rouges, et « *You cannot breathe.* »

### 7. Black Lightning Storm (L'Orage d'éclairs noirs) — le haki dans la kagune

La bête **plante ses huit tentacules** dans le sol autour d'elle et y coule tout son haki. Pendant 2 s, des **éclairs noirs**
sortent du sol autour d'elle, d'un tentacule à l'autre et vers tout ennemi à 10 blocs (8 × 0,3).
- Chaque éclair qui touche **étourdit** 0,5 s un ennemi plus faible d'un royaume.
- Les éclairs **sautent** d'un ennemi à l'autre (2 sauts) quand ils sont proches.
- Pendant l'orage, la bête ne bouge pas, mais elle ne peut pas être poussée ni étourdie.
- **Les effets :** des éclairs noirs à cœur rouge, des modèles en zigzag comme ceux du Sabre du Tonnerre, avec une lumière
  Photon rouge sombre qui clignote ; le sol se fend en étoile autour d'elle.

### 8. Demon's Maw (La Bouche du Démon)

Une fois par forme.
- Les quatre tentacules libres se referment devant sa tête en **une gueule** de chair.
- Dans la gueule, une **sphère** de Qi noir et rouge grossit en 1,5 s, parcourue d'éclairs noirs, et attire la lumière et
  les particules autour d'elle.
- La gueule la crache : elle file 30 blocs et **explose** sur 8 blocs (×4,0 au centre, ×2,0 au bord). Le cratère reste, si
  les règles du monde le permettent.
- Elle met **fin à la forme** juste après : le démon a tout donné, et le contrecoup commence.
- Inspiration : la bombe de Kyubi.

## La forme du démon : *Demon Form*

**La transformation (à 100 de rage, ou par Devourer's Crown) :**
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
- **Toutes les attaques changent**, M1 compris (voir « Les huit attaques de la forme du démon »).
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
  - les formes qu'elle prend : la cage, la vrille sous terre, les fils de sang, le tentacule tranché, la couronne, le
    manteau d'éclairs noirs ;
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
