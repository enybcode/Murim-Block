# Le Poing Brise-Montagne (碎山拳, *Suì Shān Quán*)

> **État : proposition du 16/10/2026, pas encore implémentée.** Demande d'Enzo : l'art martial que lâche le Seigneur des
> bandits, boss des grands camps ([../../mobs/bandit-warlord.md](../../mobs/bandit-warlord.md)) : « un art avec beaucoup
> de dégâts de zone et de destruction d'éléments ». Ses techniques sont les mouvements du boss : le joueur apprend ce
> qu'il vient d'affronter, comme avec Sensei Yun et le capitaine. Les chiffres sont un premier jet, alignés sur
> [../technique-balance.md](../technique-balance.md) ; les règles communes sont dans [../STYLES.md](../STYLES.md).
> Les noms en jeu sont en anglais ; le nom anglais d'abord, la traduction entre parenthèses.

## L'idée

L'art des seigneurs bandits : pas de grâce, de la **force brute**. Chaque coup fait trembler le sol, fend la roche et
renverse tout ce qui est autour. C'est l'opposé du Poing Long (précis, rapide, un adversaire à la fois) : le Poing
Brise-Montagne est **lent, lourd, et frappe en zone**, contre les groupes, et il **casse le décor** : murs, sol, arbres.

- **À mains nues**, comme le Poing Long et le Sanguine Art.
- **Un art de milieu de partie** : appris à partir de **Qi Condensation** (le rouleau refuse un lecteur plus faible),
  plus fort que les arts de départ, en dessous du Sanguine Art.
- **Son identité** : **zone + destruction**. Peu de coups, chacun touche tout ce qui l'entoure et laisse un cratère.
- **Sa faiblesse** : lent à préparer (chaque technique s'annonce et se pare), coûteux en Qi, peu efficace contre un
  adversaire seul et rapide.

## Le rouleau

- **« Mountain-Shattering Fist Scroll »**, un rouleau de cuir épais taché de poussière, un poing dans une montagne
  fendue au dos.
- **Toujours lâché par le Seigneur des bandits** à sa défaite (sa seule source, comme les autres arts de boss). Un camp
  dans chaque région : chaque joueur peut aller le chercher.
- Lu, il est consommé ; le style apparaît dans l'onglet Techniques avec son passif et sa première technique.

## Les M1 et le passif : « Earthshaker » (Ébranle-terre)

- **Les M1** : les coups à mains nues d'Epic Fight, comme le Poing Long.
- **Le passif** : chaque coup de poing qui touche **fait trembler le sol** autour de la cible : **30 % de ses dégâts
  à tous les ennemis à 1,5 bloc** d'elle (sans toucher la cible une seconde fois).
- **Mains de pierre** : à mains nues, le joueur casse la roche et le bois **trois fois plus vite**.
- **La garde** : la garde Murim habituelle, les avant-bras en croix. Parade parfaite : l'attaquant est repoussé.

## Les compétences actives

Les dégâts sont des multiples de l'attaque à mains nues, avec **+4 de main endurcie** pour cet art (comme le +4 du
Sanguine Art, `TechniquePower`). Exemple : à Qi Condensation, environ **8** d'attaque à mains nues.

| Compétence | Débloquée par | Qi | Recharge | Dégâts | Destruction |
|---|---|---|---|---|---|
| 1. **Mountain-Crushing Punch** (Poing écrase-montagne) | Le rouleau | 40 | 8 s | ×1,6 la cible, ×0,8 dans le cône | Un tunnel de 2 × 2 sur 4 blocs devant |
| 2. **Earth Breaker** (Brise-terre) | Quête 1 | 60 | 14 s | ×1,2 à tous dans 5 blocs, projette en l'air | Un cratère de 3 blocs de rayon |
| 3. **Bull Charge** (Charge du taureau) | Quête 2 | 50 | 12 s | ×1,0 à chaque ennemi traversé, renverse | Traverse les murs : 2 de large, 3 de haut, sur 6 blocs |

À 8 d'attaque : le Poing ≈ 13 sur la cible et 6 autour, le Brise-terre ≈ 10 à chacun, la Charge ≈ 8 à chacun. Contre
un groupe de cinq, le Brise-terre retire environ 50 de vie en un seul coup.

### 1. Mountain-Crushing Punch

Un direct du droit, armé de loin : le joueur fait un pas et enfonce le poing. **L'onde part en cône** : la cible prend
×1,6 et recule de 4 blocs, tout ce qui est dans un **cône de 5 blocs** devant prend ×0,8. **Les blocs devant éclatent** :
un tunnel de 2 × 2 sur 4 blocs (de quoi percer un mur ou une colline).

- **En l'air** : le poing abattu vers le bas, une onde qui rabat les ennemis au sol.
- **Animation** : celle du boss (`mountain_crusher`), à la taille du joueur.
- **Effets** : un anneau de poussière qui part du poing, des éclats des blocs cassés.

### 2. Earth Breaker

Les deux poings levés au-dessus de la tête et abattus sur le sol : **une onde de choc circulaire de 5 blocs**. Tous
ceux qu'elle touche prennent ×1,2, **sont projetés en l'air** et ralentis 3 s. **Le sol se creuse** : un cratère de
3 blocs de rayon, les blocs arrachés sautent en l'air en débris et retombent autour.

- **En l'air** : le joueur plonge poings en avant ; le cratère et l'onde sont plus grands (4 et 6 blocs), ×1,5.
- **Animation** : `earth_breaker`.

### 3. Bull Charge

L'épaule baissée, le joueur **fonce sur 6 blocs**. Tout ennemi sur son chemin prend ×1,0 et **est renversé** ; tout
**mur sur son chemin est enfoncé** (2 de large, 3 de haut). Il s'arrête sur l'impact contre le premier ennemi lourd
(un boss) ou un bloc incassable.

- **En l'air** : la charge part vers le bas, en diagonale, jusqu'au sol, avec un petit cratère à l'arrivée.
- **Animation** : `bull_charge`.

## Le secret : « Sky Breaker » (Brise-ciel)

Accroupi, un uppercut qui monte jusqu'au ciel : la cible est **projetée à 8 blocs de haut** (×2,0), et **une colonne de
terre** jaillit sous elle et retombe en débris. Il se découvre après avoir **mis à terre 10 ennemis avec le
Brise-terre** (indice : « Strike the earth hard enough and the sky will follow »). Qi 90, recharge 30 s.

## L'ultime : « Heaven-Falling Mountain » (Montagne tombée du ciel)

> Demandes d'Enzo du 16/10 : la chute du ciel du Seigneur des bandits, son entrée, est l'ultime de cet art ; un
> **changement du terrain** ; il tombe comme une météorite, avec du feu.

Le joueur **bondit droit dans le ciel** (une trentaine de blocs), reste un instant suspendu, puis **retombe comme un
météore** sur un point visé (jusqu'à 12 blocs de côté), enveloppé de feu, une traînée de flammes derrière lui, et
atterrit poing au sol. Au sol, un cercle de fissures rougeoyantes annonce l'impact pendant la montée.

- **Dégâts** : ×3,0 au centre (3 blocs), ×1,5 jusqu'à 8 blocs ; tous projetés en arrière et renversés ; brûlure 3 s.
- **Le terrain change** : un **cratère de 8 blocs de rayon et 4 de profondeur** (le boss, à sa taille, en creuse un de
  30 et 15 : tout le camp). Mêmes règles de destruction que le reste de l'art.
- **Débloqué** à 100 de maîtrise, après le secret. Qi 200, **recharge 5 minutes**. Pendant la montée, le joueur ne peut
  pas être touché en mêlée, mais peut l'être par les flèches.
- **Animation** : `sky_fall` (sa chute et son atterrissage), une montée à créer. Pas de cinématique pour le joueur
  (seulement pour l'entrée du boss).

## Le finish : « Mountain Burial » (Ensevelissement)

Sur un joueur à terre (B) : le vainqueur l'enfonce dans le sol de trois coups de poing, un cratère s'ouvre, des
rochers retombent sur lui. Débloqué quand les trois techniques sont apprises.

## La destruction des blocs

C'est le cœur de l'art, et ce qui demande le plus de règles :

- **Ce qui casse** : la terre, le sable, le gravier, la roche, le bois, les feuilles, le verre, les minerais (dureté
  jusqu'à celle de la roche profonde). **Jamais** : la bedrock, l'obsidienne, les blocs incassables, les coffres,
  tonneaux et autres contenants, les lits, les portails.
- **Ce qui tombe** : rien ou presque (un bloc sur dix), pour que l'art ne serve pas à creuser une mine.
- **Dans les sites** (temple, avant-postes, arènes, camps, sanctuaire) : rien ne casse.
- **En multijoueur** : une règle de jeu `murimDestruction` (activée par défaut), pour qu'un serveur puisse protéger
  les bases des joueurs.

## Les quêtes et la maîtrise

- **Quête 1 « Shatter the Stone »** (Fendre la pierre) : briser 200 blocs avec Mountain-Crushing Punch. Débloque
  Earth Breaker.
- **Quête 2 « Shake the Earth »** (Faire trembler la terre) : toucher au moins 3 ennemis d'un même coup, 10 fois.
  Débloque Bull Charge.
- **Évolutions** à la maîtrise :
  - à 30, **Mountain-Splitting Punch** (Poing fend-montagne) : le cône fait 7 blocs et le tunnel 6 ;
  - à 60, **Earthquake** (Tremblement de terre) : deux ondes successives, cratère de 4 blocs ;
  - à 90, **Stampede** (Ruée) : la charge fait 10 blocs et enfonce 3 de large.

## Ce que le Seigneur des bandits en fait

Ses attaques sont celles de l'art, à sa taille (deux fois celle d'un homme) : le Poing écrase-montagne de près, le
Brise-terre quand il est entouré, la Charge du taureau pour rattraper un joueur qui recule, le Brise-ciel en seconde
phase. **Il ouvre le combat par l'ultime**, en cinématique : il tombe du ciel en météore sur le camp, qui devient un
cratère pour de bon ([../../structures/bandit-camp.md](../../structures/bandit-camp.md)). Tout s'annonce et se pare. **Ses coups cassent aussi le décor** de la cour du camp, mais pas les bâtiments
(la règle des sites), sauf sa chute d'entrée, qui creuse le camp une fois pour toutes.

## À décider avec Enzo

1. **La destruction** : de vrais blocs cassés (comme un creeper), ou des blocs qui reviennent après une minute (rien
   d'abîmé pour de bon) ?
2. **Ce que les blocs cassés donnent** : rien, un sur dix, ou tout ?
3. **Le royaume** : à partir de Qi Condensation, ou plus tard ?
4. Cet art s'ajoute-t-il aux 12 de la bêta (un 13e, « libre »), ou en remplace-t-il un ?
5. L'ultime chez le joueur : un cratère de 8 blocs, recharge 5 minutes, débloqué à 100 de maîtrise ; trop fort, ou pas assez ?
