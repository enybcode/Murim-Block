# L'Épée des Sept Étoiles (七星剑法, *Qīxīng Jiànfǎ*)

> **État : implémenté le 17/10/2026 sur `test`** (voir « Ce qui est en jeu » à la fin), à valider par Enzo. Demandes d'Enzo :
> - « un bon art martial tourné autour de l'épée, un art intermédiaire, pas démoniaque, juste normal » ;
> - « beaucoup d'effets de particules ou même de modélisation à partir d'intermédiaire, on peut mettre du budget » ;
> - « les effets plus clairs, c'est goofy », « change le premier skill, c'est nul », « intermédiaire, c'est 5 attaques » ;
> - « tout doit se compléter » ;
> - « ça ne se joue qu'au fleuret, et il a un fleuret à son effigie » ;
> - « les skills un par un pour vraiment tout contrôler », puis « fais tout l'art martial en gardant le niveau graphique du passif » ;
> - « utilise Photon pour les effets lumineux », « tu te sers toujours de Photon ».
>
> Un art **libre** (ni orthodoxe ni démoniaque), au-dessus de l'Épée du Nuage Azur et en dessous du Sanguine Art. Les
> chiffres sont un premier jet, alignés sur [../technique-balance.md](../technique-balance.md) et sur le Poing
> Brise-Montagne ; les règles communes sont dans [../STYLES.md](../STYLES.md). Les noms en jeu sont en anglais ; le nom
> anglais d'abord, la traduction entre parenthèses.

## L'idée

L'art des épéistes errants qui suivent la Grande Ourse. Chaque coup d'épée **pose une étoile** sur l'adversaire ; on les
accumule, puis on les **fait éclater**. Là où le Nuage Azur est un art de **ruées et de coupes aériennes**, les Sept
Étoiles sont un art de **précision** : on construit, puis on encaisse.

- **Uniquement au fleuret** : les fleurets de Dawn Day (fer, or, diamant, netherite) et le **fleuret de l'art**, le
  Fleuret des Sept Étoiles (plus bas). Pas d'épée, pas de sabre : un art d'**estocs**, vif et précis.
- **Un art de milieu de partie** : appris à partir de **Qi Condensation**, comme le Poing Brise-Montagne.
- **Cinq techniques**, comme tous les arts intermédiaires (les arts de départ en ont trois).
- **Sa faiblesse** : il faut **construire** les étoiles avant de faire très mal, et un coup reçu en fait perdre.

## Le manuel

- **« Seven Stars Sword Manual »** (Manuel de l'Épée des Sept Étoiles), un livret bleu nuit, la Grande Ourse en argent au
  dos.
- **Décidé par Enzo le 17/10/2026** : on le trouve **en méditant trois minutes au plus haut point du monde**, le sommet du
  **pic de l'Étoile Cueillie**, une fois au stade **Qi Liquidation** ou au-delà. Les étoiles donnent le manuel **et** le
  fleuret, une seule fois par joueur ([../../structures/star-plucking-peak.md](../../structures/star-plucking-peak.md)).
- Lu, il est consommé ; le style apparaît dans l'onglet Techniques avec son passif et sa première technique.

## Le passif : « Star Marks » (Marques d'étoiles)

- **Chaque coup de fleuret qui touche** pose **une étoile** sur la cible, jusqu'à **7**. Une parade parfaite en pose **2**.
- **À 7**, la constellation est complète : le **coup suivant** la fait **éclater** : **+60 %** de dégâts, la cible est
  **déséquilibrée**, les étoiles retombent à 0.
- Les étoiles **s'éteignent** une à une après 4 s sans frapper la cible, et le joueur en **perd 2** quand il est touché.
- **Les M1** : l'enchaînement de fleuret de Dawn Day (quatre estocs, `florett_auto1` à `4`), 10 % plus rapide.
- **La garde** : la garde Murim habituelle. Parade parfaite : 2 étoiles et l'attaquant déséquilibré.

## Comment tout se complète

Les étoiles sont la ressource commune : **toutes les techniques en posent, et chacune devient plus forte sur une cible
déjà marquée**. Le cycle d'un combat :

1. **Ouvrir** : Shooting Star traverse le groupe et marque tout le monde.
2. **Construire** : les M1 et Dipper Steps montent les étoiles sur la cible principale.
3. **Tenir** : Orbiting Stars arrête les coups pendant qu'on construit, et chaque coup arrêté marque l'attaquant.
4. **Encaisser** : Constellation Link fait éclater toutes les étoiles de tout le groupe en une fois.
5. **Recommencer** : chaque éclatement réduit la recharge de Shooting Star, qui rouvre le cycle.

Falling Star Rain sert à **marquer un groupe de loin** avant d'y entrer, ou à **finir** ceux qui ont déjà des étoiles.
Le secret (Polaris Riposte) **complète une constellation** d'un coup, et l'ultime frappe d'autant plus fort que la cible
porte d'étoiles.

| Technique | Pose des étoiles | Plus forte sur une cible marquée |
|---|---|---|
| Shooting Star | 1 à chaque ennemi traversé | Traverser une cible à 7 étoiles la fait **éclater** au passage |
| Dipper Steps | 1 par coupe | À 4 étoiles ou plus, **un 4ᵉ bond** et une 4ᵉ coupe |
| Orbiting Stars | 1 sur chaque attaquant arrêté | Les étoiles en orbite qui restent à la fin **partent seules** sur l'ennemi marqué le plus proche, une étoile chacune |
| Constellation Link | — (elle les dépense) | 0,25 par étoile ; **chaque éclatement retire 1 s** à la recharge de Shooting Star |
| Falling Star Rain | 1 par étoile filante qui touche | Les étoiles filantes **visent d'abord les ennemis marqués** du cercle ; à 7 étoiles, ils éclatent tout seuls |

## Les cinq techniques

| # | Technique | Débloquée par | Qi | Recharge | Dégâts (× l'attaque) | Rôle |
|---|---|---|---|---|---|---|
| 1 | **Shooting Star** (Étoile filante) | Le manuel | 30 | 7 s | ×1,3 à chacun sur la ligne | Ouvrir, traverser |
| 2 | **Dipper Steps** (Pas du Boisseau) | Quête 1 | 40 | 10 s | 3 × 0,6 | Combo, esquive |
| 3 | **Orbiting Stars** (Étoiles en orbite) | Quête 2 | 45 | 18 s | 0,4 par riposte | Défense, contre |
| 4 | **Constellation Link** (Liaison des constellations) | Quête 3 | 55 | 14 s | 0,25 par étoile | Encaisser sur plusieurs |
| 5 | **Falling Star Rain** (Pluie d'étoiles filantes) | Quête 4 | 70 | 20 s | 7 × 0,35 dans la zone | Zone, à distance |

À Qi Condensation, une épée de diamant fait environ **8** d'attaque :
- Shooting Star ≈ 10 à chacun ;
- Dipper Steps ≈ 14 si tout touche ;
- Constellation Link ≈ 14 sur une cible à 7 étoiles ;
- Falling Star Rain ≈ 20 sur une cible seule si les 7 touchent.

Chaque technique a sa version au sol et en l'air, avec ses animations Epic Fight.

### 1. Shooting Star (Étoile filante)

**Ce que fait le joueur :** il se penche, l'épée tenue basse en arrière. D'un coup, il **devient un trait de lumière** et
file **8 blocs** droit devant en 0,3 s. Il réapparaît au bout, l'épée tendue, puis se redresse.

**Ce que ça fait :** chaque ennemi traversé est coupé (×1,3) et reçoit **une étoile** ; un ennemi qui en portait déjà 7
**éclate** au passage. C'est l'ouverture : on traverse un groupe et tout le monde est marqué. En l'air, le trait plonge en diagonale vers le sol.

**Les effets :** au départ, une **image du joueur reste figée** un instant puis se dissout en poussière d'étoile. Le
trait est une **traînée fine et nette** qui reste une demi-seconde puis s'efface du début vers la fin. Sur chaque ennemi
traversé, une **petite coupe en croix** brille.

### 2. Dipper Steps (Pas du Boisseau)

**Ce que fait le joueur :** près d'une cible (4 blocs au plus), il fait **trois bonds autour d'elle** : devant, sur le
côté, derrière. À chaque bond, une coupe : montante, transversale, descendante. Il finit **dans son dos**.

**Ce que ça fait :** 3 coups (3 × 0,6), une étoile par coup. Pendant les bonds, il **ne peut pas être touché** au corps à
corps : c'est aussi une esquive. Sur une cible à **4 étoiles ou plus**, il fait **un 4ᵉ bond** et une 4ᵉ coupe. En l'air, les coupes tournent en spirale autour de la cible et la dernière la rabat au
sol.

**Les effets :** à chaque bond, **une étoile s'allume au sol** sous son pied, **reliée à la précédente** par un trait
fin : à la fin, trois étoiles reliées au sol, qui s'éteignent après 1 s.

### 3. Orbiting Stars (Étoiles en orbite)

**Ce que fait le joueur :** il lève l'épée devant son visage, la lame droite. **Sept petites étoiles** sortent de la lame
et se mettent à **tourner autour de lui**, à hauteur de poitrine, pendant 6 s.

**Ce que ça fait :** chaque fois qu'il **va être touché** (coup ou projectile), une étoile s'interpose : le coup est
**annulé**, et l'étoile file sur l'attaquant (×0,4) et lui pose **une étoile**. Sept étoiles, sept coups arrêtés. C'est
la technique défensive de l'art. À la fin des 6 s, les étoiles **qui restent** partent seules sur l'ennemi marqué le
plus proche et lui posent une étoile chacune : rien n'est perdu.

**Les effets :** sept étoiles nettes en cercle, qui tournent ; quand l'une pare, un **éclat** à l'endroit du coup, puis
elle part en ligne droite vers l'attaquant.

### 4. Constellation Link (Liaison des constellations)

**Ce que fait le joueur :** il pointe l'épée vers le ciel. **Tous les ennemis marqués** à 12 blocs autour sont **reliés
par des traits de lumière**, d'étoile en étoile. Il abaisse l'épée d'un coup sec.

**Ce que ça fait :** toutes les étoiles de tous les ennemis **éclatent ensemble** : chacun prend 0,25 par étoile qu'il
portait (jusqu'à ×1,75 pour 7 étoiles), et les étoiles retombent à 0. C'est la façon d'**encaisser** sur un groupe après
l'avoir marqué avec Shooting Star et Orbiting Stars. **Chaque éclatement retire 1 s** à la recharge de Shooting Star : on
rouvre aussitôt le cycle.

**Les effets :** les traits partent de l'épée vers chaque ennemi marqué, puis d'un ennemi à l'autre. Quand l'épée
s'abaisse, chaque étoile **éclate** en un petit anneau de lumière.

### 5. Falling Star Rain (Pluie d'étoiles filantes)

**Ce que fait le joueur :** il lève l'épée à deux mains, la pointe vers le ciel. Un **cercle** apparaît au sol à l'endroit
visé (5 blocs de large, jusqu'à 12 blocs devant). Il abaisse l'épée vers le cercle.

**Ce que ça fait :** **sept étoiles filantes** tombent du ciel dans le cercle, l'une après l'autre en 1,5 s (×0,35
chacune, une étoile par touche). Sa seule attaque de zone, lente à poser, mais elle marque tout un groupe à distance. Les étoiles filantes **visent d'abord les ennemis déjà marqués** du cercle, et un
ennemi qui atteint 7 étoiles sous la pluie **éclate tout seul**. En
l'air, le joueur reste suspendu le temps de la pluie.

**Les effets :** le cercle au sol est un **anneau fin** avec la Grande Ourse dessinée dedans. Chaque étoile filante a une
**tête brillante et une longue traînée** qui s'amincit ; là où elle tombe, un **petit éclat** et une marque au sol qui
s'efface.

## Le secret : « Polaris Riposte » (Riposte de l'étoile Polaire)

**Une parade parfaite, puis un M1 dans les 10 ticks** : une riposte immédiate, un estoc au cœur (×1,2) qui **complète la
constellation** d'un coup (7 étoiles). Elle se découvre après **30 parades parfaites** avec l'art (indice : « The pole
star does not move; everything turns around it »). Effet : l'étoile Polaire apparaît une seconde au-dessus du joueur.

## L'ultime : « Seven Stars Sword Array » (Formation des Sept Épées)

Débloquée à 100 de maîtrise. Le joueur plante son épée devant lui : **sept épées de lumière** sortent du sol autour de la
cible (10 blocs au plus), placées comme la Grande Ourse, se tournent vers elle, frappent **l'une après l'autre**, puis
**toutes ensemble** (7 × 0,4, puis ×2,0). La cible est **figée** pendant les 2 s. Qi 150, recharge 3 minutes.

## Le finish : « Extinguished Stars » (Étoiles éteintes)

Sur un joueur à terre (B) : sept estocs rapides, une étoile s'allume au-dessus de lui à chacun ; l'épée est rengainée
d'un geste et les sept étoiles s'éteignent ensemble.

## Les effets : sobres dans la forme, riches dans la lumière

> « Beaucoup d'effets… à partir d'intermédiaire » et « plus clairs, c'est goofy ». Donc : des formes **simples, fines et
> nettes**, pas de gros blocs ; ce qui fait le spectacle, c'est la **lumière**.

- **Modèles 3D GeckoLib** (comme les cristaux du Sanguine Art) : l'étoile (une étoile à quatre branches fines, un cœur
  brillant), les traits de lumière (fins, qui s'amincissent), l'épée de lumière (une lame fine sans grosse garde), le
  cercle au sol (un anneau fin, la Grande Ourse dedans).
- **Les parties lumineuses brillent dans le noir** (glowmask) et sont **semi-transparentes** sur les bords.
- **Des particules à nous** : poussière d'étoile, petits éclats.
- **Des lumières dynamiques Photon** : un éclair à chaque éclatement, une lueur qui suit les étoiles filantes.
- **Les couleurs** : blanc, argent, bleu pâle. Rien de rouge.

## Les quêtes et la maîtrise

- **Quête 1 « Seven Points »** : faire éclater **10 constellations**. Débloque Dipper Steps.
- **Quête 2 « Walk the Dipper »** : finir 15 fois Dipper Steps dans le dos d'une cible et la toucher dans les 2 s.
  Débloque Orbiting Stars.
- **Quête 3 « Unbroken Orbit »** : arrêter 30 coups avec Orbiting Stars. Débloque Constellation Link.
- **Quête 4 « Bind the Sky »** : avec Constellation Link, faire éclater les étoiles de 3 ennemis à la fois, 10 fois.
  Débloque Falling Star Rain.
- **Évolutions** à la maîtrise :
  - à 30, Shooting Star fait 12 blocs et peut être relancée une fois dans les 2 s ;
  - à 60, Dipper Steps fait 5 bonds au lieu de 3 ;
  - à 90, Falling Star Rain appelle 9 étoiles, et une constellation complétée par elle éclate toute seule.

## Le Fleuret des Sept Étoiles

Le fleuret « à son effigie » (`scripts/animation/seven-stars-florett.cjs`, `art/blockbench/seven_stars_florett.bbmodel`) :
- une lame longue et fine d'acier poli, la **Grande Ourse incrustée d'argent** au-dessus de la garde ;
- une **garde en étoile à quatre branches**, ses quillons finis par de petites étoiles, son cœur émaillé bleu nuit à
  l'étoile blanche, une petite coquille dessous, une **branche de garde** qui rejoint le pommeau ;
- une fusée de cordelette bleu nuit filée d'argent ;
- un **pommeau en étoile** au cœur de lumière, une courte houppe bleu nuit.

Un peu au-dessus du fleuret de diamant, rare, l'enchaînement de fleuret de Dawn Day. Avec l'art, le premier coup d'un
combat pose une étoile de plus, et ses incrustations s'allument une à une avec les étoiles de la cible.

## À décider avec Enzo

1. Le nom : « Seven Stars Sword » ?
2. ~~Où trouver le manuel et le fleuret~~ : décidé le 17/10 : en méditant trois minutes au sommet du pic de l'Étoile
   Cueillie, au stade Qi Liquidation (voir « Le manuel »).

## Ce qui est en jeu (17/10/2026)

| Élément | Code | Vérifié par |
|---|---|---|
| L'art, ses 5 techniques, le secret, l'ultime, 5 quêtes, 3 évolutions | `MartialArts` (`SEVEN_STARS_SWORD`) | `SevenStarsGameTests` (16 tests) |
| Le passif Star Marks, Shooting Star, Twin Comets | `martial/SevenStarsSword` | idem |
| Dipper Steps, Orbiting Stars, Constellation Link, Falling Star Rain, Polaris Riposte, Seven Stars Sword Array, le finish Extinguished Stars | `martial/SevenStarsMoves` | idem |
| Les effets : étoiles, Grande Ourse, éclatement, trait, sceaux, épées de lumière, étoiles filantes ; lumières Photon (`LightProvider`) | `client/SevenStarsFx`, `client/SevenStarsTechniqueFx` | `gradlew -PgameTests runSevenStarsTour` (photos en vrai client, de nuit) |
| Le Fleuret des Sept Étoiles (fleuret de Dawn Day, 7 d'attaque) et le manuel | `martial/SevenStarsItems`, `scripts/animation/seven-stars-florett.cjs` | idem |
| Les animations du joueur | `scripts/animation/seven-stars-art.cjs` (`biped/murim/sss_*`) | la visite |
| Les icônes | `scripts/worldgen/TechniqueIcons.java` (« star night ») | — |
| Textures de lumière | `scripts/animation/seven-stars-fx-textures.cjs` | — |

Chiffres retenus : Shooting Star 30 Qi / 7 s ; Dipper Steps 40 / 10 s ; Orbiting Stars 45 / 18 s ; Constellation Link 55 / 14 s ;
Falling Star Rain 70 / 20 s ; Seven Stars Sword Array 150 / 3 min. Protocole réseau 23.

Le manuel et le fleuret : au sommet du pic de l'Étoile Cueillie (`StarPeakPlacement`, `StarPeakGameTests`, `runStarPeakTour`) ;
aussi dans l'onglet créatif et par `/martial learn seven_stars_sword`.

### En images (vrai client, de nuit)

![Le passif : 2, 5 et 7 étoiles, l'éclatement](images/seven-stars-passive.png)
![Shooting Star](images/seven-stars-shooting-star.png)
![Dipper Steps](images/seven-stars-dipper.png)
![Orbiting Stars, Falling Star Rain, Seven Stars Sword Array](images/seven-stars-orbit-rain.png)
![Constellation Link](images/seven-stars-link.png)
![Seven Stars Sword Array](images/seven-stars-array.png)
![Polaris Riposte](images/seven-stars-polaris.png)
![Les icônes](images/seven-stars-icons.png)
