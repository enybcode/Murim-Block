# Temple taoïste de la Mer de Bambous brumeuse

| Fiche | |
|---|---|
| Nom en jeu (id) | Misty Bamboo Temple (`murimblock:misty_bamboo_temple`) |
| Biome | Mer de bambous de Shu (`shu_bamboo_sea`) |
| Inspiration | Le mont Qingcheng (UNESCO, berceau du taoïsme) : Tianshi Dong et Shangqing Gong, charpente chuandou du Sichuan |
| Emprise | 25 × 56 blocs (avec le chemin devant la porte), sur trois niveaux ; le temple monte la pente |
| Fréquence | Une tentative par carré de 22 × 22 chunks, à 7 chunks au moins d'une autre ; seulement si le terrain convient |
| Terrain accepté | Dénivelé de 20 blocs au plus, aucune eau ; chaque terrasse prend la hauteur du terrain à son endroit (2 à 7 blocs de plus que la précédente) |
| Trouver | `/locate structure murimblock:misty_bamboo_temple` |
| Code | [BambooTemple.java](../../src/main/java/com/murimblock/world/structure/site/BambooTemple.java) |

## Plan

Le temple tourne le dos au haut de la pente : on entre en bas, on monte vers la grande salle. Chaque terrasse est portée par un mur de soutènement en pierre (taillée, moussue, fissurée) jusqu'au sol ; le terrain est modelé autour de chaque niveau.

1. **La porte de la montagne** (terrasse basse) : bâtiment à trois travées, passage central ouvert, cadre de bois sombre rempli de blanc, frise de bois, toit gris aux angles relevés. Une cloche suspendue dans la travée ouest, un tambour dans la travée est, bougies, lanternes ; deux bornes à lanterne de part et d'autre de l'entrée.
2. **Un escalier de 3 de large** taillé dans le mur de la cour du milieu.
3. **La cour du milieu** : dallage d'andésite polie, balustrade de pierre sur les bords.
   - Au centre, le **taiji** (yin et yang) en pierre noire et blanche, cerclé d'andésite.
   - Le **brûle-parfum** dans l'axe : feu de camp sur un socle de pierre noire, fumée qui monte.
   - **Deux salles latérales** sur soubassement, toit à deux pentes : à l'ouest les **écritures** (bibliothèques sculptées, pupitre, bambou en pot, bougies), à l'est les **élixirs** (alambics, chaudron, fourneau, jarre).
   - Bambous dans les quatre coins, lanternes.
4. **L'escalier couvert** jusqu'à la terrasse haute : marches sur un massif de pierre, poteaux de bois sombre et garde-corps, toit de tuiles à faîtage qui monte avec les marches, lanternes.
5. **La salle des Trois Purs** (terrasse haute) : 15 × 7 sur soubassement, triple porte, treillis en façade et sur les côtés, toit gris à quatre pentes et angles relevés. Dedans : autel de granite poli, trois tentures (bleue, jaune, rouge) pour les Trois Purs, jarres d'offrandes, bougies, coussins de méditation (tapis jaunes), lanternes.
   - **Son occupant** (14/10, demande d'Enzo) : **Sensei Yun** (`murimblock:azure_sensei`), maître de la Secte du Nuage Azur, entre l'autel et les coussins, face aux portes. Neutre, il reste dans sa salle (6 blocs autour de sa place) et ne disparaît pas ; clic droit, il donne le manuel de l'Art de l'Épée du Nuage Azur une fois par joueur. Fiche : [docs/mobs/azure-sensei.md](../mobs/azure-sensei.md).

**Chemins** : tout est sur un seul axe : porte → escalier → cour (autour du brûle-parfum) → escalier couvert → salle. Les salles latérales donnent sur la cour.

## Vérification (14/10/2026)

- **GameTest** `SiteGameTests` : construit sur un sol de pierre, 9217 blocs posés, 1159 vérifications, aucun problème ; identique bloc pour bloc construit chunk par chunk ; structure et structure set enregistrées.
- **Visite en vrai client** (`gradlew -PgameTests runSiteTour`, monde neuf, graine 20261012) :
  - `/locate structure murimblock:misty_bamboo_temple` répond en 241 ms : `[448, ~, -192]`, à 487 blocs du point de départ ;
  - le site généré est dans `shu_bamboo_sea` (origine 456 63 -184, tourné COUNTERCLOCKWISE_90, niveaux 63, 69, 74) ;
  - 1159 vérifications sur le site réellement généré : aucun toit ouvert, rien de naturel dans les pièces, aucun sol ni fondation au-dessus du vide ;
  - Sensei Yun est dans sa salle (434 76 -184), seul.
- Les GameTests vérifient aussi qu'il y a exactement 1 sensei, construit d'un coup comme chunk par chunk.

## Conseils : ce qu'on devrait y mettre

- **PNJ** : le maître est là (Sensei Yun) ; encore à venir : un disciple gardien à la porte, un alchimiste dans la salle des élixirs, un archiviste aux écritures.
- **Faction** : une école d'épée façon Qingcheng ou un ordre façon Emei ; c'est une très bonne seconde secte à rejoindre, ou une rivale de la secte du sanctuaire.
- **Techniques** : un manuel d'art martial taoïste (épée ou souffle) à mériter : épreuves sur le taiji, méditation sur les coussins pour regagner du Qi plus vite.
- **Alchimie** : la salle des élixirs comme atelier de pilules de Qi (recette, ingrédients du biome : pousses de bambou, fougère arborescente, herbes de montagne).
- **Butin** : coffres d'offrandes, rouleaux (livres) dans les écritures.
- **Ambiance** : la cloche sonne à l'aube, la fumée de l'encens, des pandas roux ou des singes dorés dans les bambous autour.
