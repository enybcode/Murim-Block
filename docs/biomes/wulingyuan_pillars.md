# Forêt de pierre de Wulingyuan (`murimblock:wulingyuan_pillars`) — biome du centre

> Demande d'Enzo (17/10/2026), avec des photos du vrai Zhangjiajie et de mondes Minecraft qui l'imitent : « crée ce
> biome pour le centre, ça doit être aussi joli ».

![Par-dessus les sommets](images/wulingyuan_pillars/wulingyuan_pillars-tops-2.png)

## Inspiration

**Le paysage réel.** Wulingyuan, dans le Hunan (parc de Zhangjiajie), site UNESCO depuis 1992.
- Plus de 3 000 piliers de grès quartzeux, dont beaucoup dépassent 200 m.
- Ce sont les restes d'un ancien plateau. L'eau a suivi les fissures de la roche et l'a découpée en colonnes, si serrées
  que les gorges entre elles n'ont parfois que quelques mètres. Leurs sommets sont donc presque tous à la même hauteur.
- La roche est gris pâle et blanche, avec des bandes rouille et des coulures sombres. Elle est taillée en gradins le long
  de ses strates, et chaque gradin porte de la verdure.
- Des pins poussent sur les sommets et à même les parois, et des brumes montent des gorges.

## Dans le jeu

- **Où** : dans la région du centre, sur les terres humides qui ne sont pas des montagnes (humidité au-dessus de 0,1,
  `MurimBiomeSource.WULINGYUAN_HUMIDITY`). Les montagnes du centre restent les pics de la mer de nuages, le reste la vallée
  des fleurs de pêcher.
- **Les piliers** (`PillarForestFeature`) :
  - toute la forêt est dessinée d'un coup sur une grille de cellules de 21 blocs, avec un pilier par cellule (une
    cellule sur sept environ reste vide : une gorge plus large, une clairière) ;
  - rayon de 3,5 à 10 blocs, surtout des piliers fins et quelques géants ;
  - les sommets suivent l'ancien plateau, vers y 168 à quelques blocs près, et un pilier sur quatre est plus bas, plus
    usé. Depuis le fond, ils montent de 60 à 110 blocs ;
  - section en carré arrondi, un peu aplatie et tournée ; les parois avancent et reculent à chaque strate (de 5 à 9
    blocs), des cannelures de pluie les creusent, et certains piliers sont amincis à mi-hauteur ;
  - roche : pierre et diorite, longues coulures verticales de calcite blanche et de tuf ou d'andésite sombre, bandes
    rouille (granite, terre cuite, terre stérile) le long de certaines strates et sous la terre du sommet, un peu de
    mousse ;
  - de l'herbe sur le sommet et sur chaque gradin, et des lianes qui pendent des parois.
- **Un pilier est entier ou absent** : chaque chunk ne pose que ses propres blocs, d'après la même grille, si bien qu'un
  pilier est le même d'un chunk à l'autre. Au bord du biome, près d'un site Murim, ou là où le sol est trop haut pour
  qu'il dépasse de 30 blocs, le pilier est laissé de côté tout entier, plutôt que coupé.
- **Arbres** : peu, pour que la roche se voie. Des osmanthus et des pins du Huangshan à couronne plate sur les sommets
  et au fond des gorges, et des pins de falaise qui sortent des parois (`cliff_pine`, entre y 80 et 190).
- **Climat** : température 0,8, humidité 0,9 ; ciel #9EBCD8, brouillard #C3D0D8 ; herbe #6DB33F, feuillage #4F9A36,
  eau #4C9A8E.
- **Brume** : le brouillard est ramené à 62 % de sa distance (`MurimMistClient`). Les piliers lointains s'effacent les
  uns derrière les autres.
- **Faune** : poules, lapins, perroquets. Musique de la jungle vanilla.

![Les sommets](images/wulingyuan_pillars/wulingyuan_pillars-tops.png)
![Du fond d'une gorge](images/wulingyuan_pillars/wulingyuan_pillars-gorge-up.png)

## Vérification

- Tests unitaires : `MurimMapTest` (le centre humide donne ce biome).
- GameTests : `MurimMapGameTests` (le biome est bien dans le centre).
- En vrai client : `MURIM_BIOME_ONLY=wulingyuan_pillars gradlew -PgameTests runBiomeTour` cherche le biome dans un monde
  neuf. Il vérifie que ses blocs sont là, puis le photographie par-dessus les sommets et depuis le fond d'une gorge.
