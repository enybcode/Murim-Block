# Hameau de la Source aux Fleurs de Pêcher

![Vue aérienne : les maisons de terre autour du pré, les champs, les pêchers](images/sites/peach_spring_hamlet-a.jpg)
![Vue de l'autre côté](images/sites/peach_spring_hamlet-b.jpg)
![En arrivant](images/sites/peach_spring_hamlet-ground.jpg)

| Fiche | |
|---|---|
| Nom en jeu (id) | Peach Spring Hamlet (`murimblock:peach_spring_hamlet`) |
| Biome | Vallée des fleurs de pêcher (`peach_blossom_valley`) |
| Inspiration | *La Source aux fleurs de pêcher* de Tao Yuanming : un village paysan resté hors du temps, sans rang ni richesse |
| Emprise | 41 × 41 blocs ; faîtages à 8 blocs au-dessus du sol, bambous jusqu'à 11 |
| Fréquence | Une tentative par carré de 22 × 22 chunks, à 7 chunks au moins d'une autre ; seulement si le terrain convient |
| Terrain accepté | Dénivelé de 6 blocs au plus sur la zone, aucune eau ; le site entier dans le biome |
| Trouver | `/locate structure murimblock:peach_spring_hamlet` |
| Code | [PeachHamlet.java](../../src/main/java/com/murimblock/world/structure/site/PeachHamlet.java) |

## Plan

- **Le pré du village** (11 × 11) : terre battue, puits à margelle moussue et treuil au centre, quatre lanternes basses.
- **Quatre maisons de terre damée** tournées vers le pré : assise de pierre, murs de terre (boue tassée), poteaux d'angle en chêne écorcé, fenêtres en treillis, **toit de chaume** (mosaïque de bambou, faîtage en bottes de paille aux bouts relevés).
  - **La salle des anciens** (nord, la plus grande, sans aucune peinture) : longue table et bancs, vieux rouleau sur son pupitre, bougies, azalée en fleur.
  - **Trois foyers** : un lit, un tonneau, un four ou un fumoir, un métier à tisser ou un établi, un pot de fleurs, un tapis.
- **Le petit temple du dieu du sol** (tudi miao, sud-est) : niche de pierre sur un socle, jarre d'offrandes, bougies, minuscule toit pyramidal de tuiles.
- **Les champs**, clos d'une haie de bambou avec un portillon :
  - deux bandes de cultures (blé, carottes, pommes de terre, betteraves) de part et d'autre d'un canal d'irrigation (la terre labourée reste humide) ;
  - une bande de rizière au fond, rangs de riz sauvage dans la boue entre des rangs d'eau.
- **Six pêchers en fleur**, des pétales roses tombés dans l'herbe autour d'eux.
- **Un écran de bambous** à l'arrière du hameau.

**Chemins** (terre battue et gravier) : chaque porte, le temple du dieu du sol et le portillon des champs mènent au pré. Aucun chemin ne finit dans le vide.

## Vérification (14/10/2026)

- **GameTest** `SiteGameTests` : construit sur un sol de pierre, 3319 blocs posés, 20 vérifications, aucun problème ; identique bloc pour bloc construit chunk par chunk ; structure et structure set enregistrées.
- **Visite en vrai client** (`gradlew -PgameTests runSiteTour`, monde neuf, graine 20261012) :
  - `/locate structure murimblock:peach_spring_hamlet` répond en 83 ms : `[1120, ~, 2240]`, à 2504 blocs du point de départ ;
  - le site généré est dans `peach_blossom_valley` (origine 1128 63 2248, tourné NONE, niveau du sol 63) ;
  - 20 vérifications sur le site réellement généré : aucun toit ouvert, rien de naturel dans les pièces, aucun sol ni fondation au-dessus du vide.

## Conseils : ce qu'on devrait y mettre

- **PNJ** : des villageois « hors du temps » qui ne connaissent ni l'empereur ni les sectes ; un ancien qui raconte l'histoire du lieu.
- **Secret** : ce sont les seuls à ne pas réagir au rang de cultivation du joueur. Une quête pourrait révéler pourquoi (une formation qui cache la vallée, un ancien maître retiré).
- **Récolte** : les pêches (été) et une rare **pêche d'immortalité** pour la cultivation ; graines et outils à échanger.
- **Animaux** : poules, cochons, un buffle, canards mandarins sur l'eau.
- **Règle** : aucun combat ni pillage ici sans conséquence (perte de réputation, les villageois partent) ; c'est un refuge.
- **Ambiance** : pétales qui tombent (modèle Blockbench, comme la boule d'eau, plutôt que des particules vanilla), fumée des foyers au crépuscule.
