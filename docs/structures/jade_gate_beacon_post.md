# Relais de la Tour à Signaux de la Porte de Jade

| Fiche | |
|---|---|
| Nom en jeu (id) | Jade Gate Beacon Post (`murimblock:jade_gate_beacon_post`) |
| Biome | Désert de la frontière du Hexi (`hexi_frontier_desert`) |
| Inspiration | Le relais de poste de Xuanquanzhi, les tours à signaux (fengsui) de Dunhuang et de Yumenguan (la Porte de Jade), la route de la soie |
| Emprise | 35 × 39 blocs (avec la route devant la porte) ; la fumée du feu de signal monte depuis 15 blocs au-dessus du sol |
| Fréquence | Une tentative par carré de 26 × 26 chunks, à 9 chunks au moins d'une autre ; seulement si le terrain convient |
| Terrain accepté | Dénivelé de 6 blocs au plus sur la zone, aucune eau ; le site entier dans le biome |
| Trouver | `/locate structure murimblock:jade_gate_beacon_post` |
| Code | [BeaconPost.java](../../src/main/java/com/murimblock/world/structure/site/BeaconPost.java) |

## Plan

- **L'enceinte** (29 × 29) : mur de terre damée (grès strié), 2 d'épaisseur et 7 de haut, assise de grès lisse en bas, bandeau de grès taillé en haut, chemin de ronde et créneaux côté extérieur.
- **Quatre tourelles d'angle** (5 × 5), qui débordent du mur et portent des créneaux.
- **La tour à signaux** sur la tourelle nord-est : fût 3 × 3 de 7 blocs, bandes de bottes de roseaux (foin) tous les 3 blocs comme sur les tours Han, échelle sur sa face ouest, **feu de signal** (feu de camp sur une botte) au sommet, entouré d'un muret. Sa fumée se voit de très loin.
- **La porterie** (sud) : massif de mur épaissi autour d'un passage de 3 × 4, linteau de grès taillé ; au-dessus, **le pavillon de la porte** : colonnes vermillon, soubassement de terre cuite rouge, treillis, toit gris à quatre pentes et angles relevés ; une cloche, un tambour (bloc musical), un tonneau, une lanterne ; une porte à chaque bout donne sur le chemin de ronde. Bannières rouges sur la façade.
- **L'escalier du chemin de ronde** le long du mur sud, à l'est de la porte, sur une rampe pleine.
- **La salle de l'officier** (nord) : sur un soubassement, murs enduits, colonnes vermillon, treillis en façade, toit gris à deux pentes ; pupitre (registre de la route), tables de cartographie, étagères, bannières jaunes impériales, tonneaux.
- **Le dortoir des voyageurs** (ouest) et **les écuries** (est) : bâtiments bas sous des toits à une pente adossés au mur ; lits et tonneaux d'un côté, stalles en barrières, foin et abreuvoirs de l'autre, ouvertes sur la cour.
- **La cour** : sable, allée dallée de la porte à la salle, puits à margelle et treuil, cargaisons des caravanes (tonneaux, foin), barre d'attache devant les écuries, lanternes sur bornes.

- **Ses occupants** (14/10, demande d'Enzo) : le relais a été pris par des pillards, **4 Mountain Bandits en tenue Dune Raider** (`murimblock:mountain_bandit`) : leur meneur dans la salle de l'officier, au-dessus du registre, un dans la cour, un près des marchandises, un dans le dortoir. Ils ne disparaissent pas et ne reviennent pas une fois tués ; en Paisible, Minecraft les retire.

**Chemins** : porte → allée → salle ; escalier → chemin de ronde → pavillon de la porte ; tourelle → échelle → feu de signal. Aucun chemin ne finit dans le vide.

## Vérification (14/10/2026)

- **GameTest** `SiteGameTests` : construit sur un sol de pierre, 4933 blocs posés, 327 vérifications, aucun problème ; identique bloc pour bloc construit chunk par chunk ; structure et structure set enregistrées.
- **Visite en vrai client** (`gradlew -PgameTests runSiteTour`, monde neuf, graine 20261012) :
  - `/locate structure murimblock:jade_gate_beacon_post` répond en 425 ms : `[-3744, ~, 5984]`, à 7058 blocs du point de départ ;
  - le site généré est dans `hexi_frontier_desert` (origine -3736 120 5992, tourné NONE, niveau du sol 120) ;
  - 327 vérifications sur le site réellement généré : aucun toit ouvert, rien de naturel dans les pièces, aucun sol ni fondation au-dessus du vide.

## Conseils : ce qu'on devrait y mettre

- **PNJ** : l'officier du relais (garnison impériale), des soldats sur le chemin de ronde, un palefrenier, des marchands de caravane qui font étape.
- **Animaux** : chameaux et chevaux dans les écuries.
- **Gameplay du feu de signal** : allumer le feu (ou l'éteindre) déclenche un événement : renforts de la garnison, ou au contraire attaque de pillards du désert. Plusieurs relais en chaîne, à vue les uns des autres, rendraient l'idée encore plus forte.
- **Commerce** : premier vrai comptoir de la route de la soie ; vendre la cistanche, le jade de Hotan, les marchandises de l'Ouest.
- **Butin** : coffres de cargaison (soie, épices, jade), coffre de l'officier (laissez-passer, carte du Hexi).
- **Faction** : garnison impériale, contrebandiers ; dernière halte avant le jianghu sans loi de l'Ouest.
