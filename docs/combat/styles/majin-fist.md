# Poing du Majin (Majin Fist)

L'art du **Fils du Démon** (`SonOfTheDemonEntity`), seconde phase du Seigneur des bandits. Il s'utilise dans l'arène du
ciel, après la transformation (voir [la fiche du boss](../../mobs/bandit-warlord.md)). Enzo donne les coups **un par
un**.

## Le système (17/10/2026, en place)

Chaque coup est une entrée de `MajinFist.MOVES` (`src/main/java/com/murimblock/mob/majin/MajinFist.java`) :

| Champ | Sens |
|---|---|
| `id` | nom du coup |
| `animation` | son animation (`ColossusAnimations`, écrite dans `scripts/animation/colossus-epicfight.cjs`) |
| `minRange` / `maxRange` | à quelle distance de sa cible il l'utilise (blocs) |
| `cooldown` | ticks avant de pouvoir le refaire |
| `weight` | combien il le préfère parmi les coups possibles |
| `belowHealth` | seulement quand sa vie est au plus cette part (1 : toujours ; 0,5 : seconde moitié) |
| `beats` | ses effets, chacun à un instant de l'animation (secondes) |

- `SonOfTheDemonPatch` construit son combat à partir de cette liste.
- `MajinFist.tick` déclenche les effets du coup en cours, une fois chacun, à leur instant.
- Outils pour les effets :
  - `blast` : explosion autour d'un point, dégâts et projection ;
  - `strikeAhead` : frappe en ligne devant lui ;
  - `crater` : sol cassé, qui revient au bout de 15 s comme tout sol brisé ;
  - `launch` : projeter en l'air.
- Tous les coups se parent avec la garde Murim. Une cible n'est frappée qu'une fois par coup.

Ajouter un coup se fait en quatre étapes :

1. ses poses dans `colossus-epicfight.cjs`, puis lancer `node scripts/animation/colossus-epicfight.cjs` ;
2. son accesseur dans `ColossusAnimations` ;
3. une ligne `Move` dans `MajinFist.MOVES` ;
4. un GIF pour Enzo.

## L'arène

- Qui tombe de l'arène, à plus de 24 blocs sous le sol près d'elle, est ramené sur le sol, sur du rocher ferme près du
  bord, et perd 6 points de vie. Ça vaut aussi pour le boss.
- Règle de chute confirmée par Enzo le 17/10.
- **L'arène disparaît après le combat** (Enzo, 17/10, `ArenaCollapse`) :
  - son butin tombe sur le sol ;
  - au bout de 5 s, le météore gronde et s'effrite ;
  - à 8 s, les joueurs et le butin sont posés en douceur au fond du cratère du camp, avec Chute lente ;
  - le météore est ensuite retiré bloc par bloc (`SkyArena.remove`), en environ 9 s ;
  - les sols cassés pendant le combat sont oubliés, pour qu'ils ne reviennent pas flotter dans le vide.

## Les coups : l'Orgueil du Soleil (17/10/2026, inspiré d'Escanor)

Enzo : « un art martial haut niveau, 6 skills inspirés d'Escanor, sers-toi de Photon, un truc qui a de la gueule ».
Chaque impact a sa lumière Photon, et les soleils ont la leur, qui les suit. Les GIF sont dans `images/majin/`, en
local seulement. Film : `gradlew -PgameTests runMajinTour`.

| # | Technique | Distance | Recharge | Vie | Effet |
|---|---|---|---|---|---|
| 1 | **Poing Solaire** (`sunfire_fist`) | 0 à 6 | 1,2 s | toujours | le direct du droit ; à l'impact, explosion de flammes devant lui (0,35 × attaque, brûle 3 s) |
| 2 | **Flamme de l'Orgueil** (`pride_flare`) | 5 à 18 | 9 s | toujours | paume armée qui s'illumine, puis poussée : une vague de feu solaire file au sol sur 21 blocs, 4,5 de large, avec des boules de lumière qui éclatent dessus (0,5 × attaque, brûle 4 s) |
| 3 | **Soleil Cruel** (`cruel_sun`) | 9 à 34 | 14 s | toujours | un petit soleil grossit au-dessus de sa paume levée, puis il le lance en arc sur sa cible : grosse explosion de 5,5 blocs, cratère, feu (1,3 × attaque) |
| 4 | **Jugement de Midi** (`noon_judgment`) | 0 à 40 | 20 s | sous 75 % | il pointe le ciel ; des cercles d'or apparaissent sous chaque joueur et autour de lui ; une seconde plus tard, des piliers de lumière s'abattent (0,8 × attaque) : il faut sortir du cercle |
| 5 | **Météore de Midi** (`midday_meteor`) | 8 à 24 | 16 s | sous 60 % | il bondit très haut, un cercle de feu se resserre là où il va tomber ; impact, cratère, puis un anneau de feu court au sol (0,6 × attaque) : il faut le sauter |
| 6 | **The One** | — | une fois | à 35 % | il s'élève, intouchable 3 s ; un grand soleil naît au-dessus de lui ; le ciel devient doré et le titre « THE ONE » s'affiche. Pendant 28 s, il est 30 % plus rapide, 25 % plus fort, et sa chaleur double. Puis il abat le grand soleil au centre de l'arène : 18 blocs de rayon, 2,2 × attaque. Un cercle d'or prévient 4 s avant : il faut fuir au bord ou parer |

**Passifs** :
- **Ardeur du Soleil** : sa chaleur brûle qui reste à moins de 5 blocs, 1 dégât par seconde ; 8 blocs et 2 dégâts pendant The One.
- **Vers Midi** : sa force monte de 5 % toutes les 30 s de combat, jusqu'à 30 %, comme le soleil vers midi.

Tous les coups se parent avec la garde Murim.

## Tests

- `SonOfTheDemonGameTests` :
  - le combat construit à partir de la liste ;
  - l'explosion qui blesse et projette ;
  - le retour dans l'arène après une chute ;
  - sa mort qui pose les joueurs et le butin en bas ;
  - The One qui vient à 35 % de sa vie, une seule fois.
