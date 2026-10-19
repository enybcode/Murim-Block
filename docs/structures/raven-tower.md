# La Tour du Corbeau (`raven_tower`)

Dixième site : l'ermitage du Maître des Corbeaux (`docs/mobs/crow-master.md`), source du rouleau de l'Art du Corbeau
messager. Le guide du Dragon d'Or (« Guide: Raven Messenger Art ») y mène.

- **Où** : pics de la mer de nuages, forêt karstique de jade, piliers de Wulingyuan, collines de camphriers
  (`tags/worldgen/biome/has_structure/raven_tower`). Rare : structure set 40/14 (sel 71010). Terrain plat exigé
  (écart de 6 blocs au plus).
- **Plan** (`world/structure/site/RavenTower`) : socle de pierre sombre de 15 × 15 en deux marches, escalier au sud,
  deux lanternes ; tour carrée de 9 × 9 sur 4 étages de 5 blocs : poteaux rouges aux angles et au milieu des murs, murs
  de papier dans des cadres de bois sombre, une avancée de tuiles noires autour de chaque étage, toit en pyramide de
  tuiles grises à pointe dorée ; échelles d'étage en étage dans l'angle nord-ouest.
  - rez-de-chaussée : la pièce du Maître (bibliothèque, lutrin, table à thé, tonneau, un perchoir) ;
  - 1er : la réserve (tonneaux, coffre ; un tonneau tient la cache des sites, `SiteLoot.CACHE`) ;
  - 2e : la chambre (lit noir, bougie) ;
  - 3e : la galerie ouverte des perchoirs, plumes noires et nid.
- **Habitant** : le Maître des Corbeaux (1), posé à la génération (`SiteResidents`).

Tests : `SiteGameTests.ravenTowerIsWholeAndCarried`. Visite : `gradlew -PgameTests runRavenFamiliarTour`.
