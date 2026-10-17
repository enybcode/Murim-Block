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

## Les coups

| Coup | Animation | Distance | Recharge | Effets | État |
|---|---|---|---|---|---|
| Direct du droit (`straight_right`) | `colossus/punch` | 0 à 6 | 1,5 s | son propre collider, ×1,2 | provisoire, en attendant les coups d'Enzo |

## Tests

- `SonOfTheDemonGameTests` :
  - le combat construit à partir de la liste ;
  - l'explosion qui blesse et projette ;
  - le retour dans l'arène après une chute ;
  - sa mort qui pose les joueurs et le butin en bas.
