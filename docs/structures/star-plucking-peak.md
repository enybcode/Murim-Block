# Le pic de l'Étoile Cueillie (摘星峰, *Zhāixīng Fēng*)

> Demande d'Enzo du 17/10/2026 : « une immense montagne dans le nord, une structure qui touche la limite de construction
> tout en haut ». C'est **le plus haut point du monde**, et là qu'on trouve l'Épée des Sept Étoiles
> ([../combat/styles/seven-stars-sword.md](../combat/styles/seven-stars-sword.md)).
>
> Nom en jeu : **Star-Plucking Peak**. Code : `world/structure/StarPeakBuilder`, `StarPeakPlacement`.

## Ce que c'est

- **Un seul par monde Murim**, construit au premier démarrage (après le sanctuaire et l'antre du Roi Crabe), **au cœur
  du nord** : le point à 55 % du rayon de la bulle dans le quartier du nord (`MurimMap.heart(NORTH, 0.55)`), ou le point le
  plus proche autour (anneaux de 256 blocs) où le nord tient tout autour de la montagne, loin de la côte, et sans site à
  moins de 100 blocs (`SiteSpacing`). Sa position est gardée dans les données du monde.
- **Un massif de 224 blocs de rayon**, monté du sol jusqu'au **dernier bloc du monde (y = 319)** : un sommet arrondi, des
  épaules, puis de longues pentes ; **cinq éperons** qui partent du sommet ; la surface rugueuse (bruit).
- **Les matériaux** : roche du Kunlun et pierre à l'intérieur, des **veines de jade blanc brut** ; gravier et roche en
  surface ; **neige et glace tassée** au-dessus de y ≈ 205.
- **Le sommet** : une plateforme de 5 × 5 en **jade blanc** bordée de diorite polie, sur le bloc y = 319. On y tient debout
  (y = 320), plus haut que tout ce qu'on peut construire.
- Construit colonne par colonne, quelques millisecondes par tick, les chunks tenus chargés par un ticket le temps du
  chantier : le serveur ne gèle pas.
- Pas encore de chemin taillé pour monter : on grimpe (blocs, escalade). À décider.

## L'Épée des Sept Étoiles

- Un joueur **au stade Qi Liquidation ou au-delà** qui **médite trois minutes sans s'arrêter sur la plateforme du
  sommet** reçoit le **Manuel de l'Épée des Sept Étoiles** et le **Fleuret des Sept Étoiles** (dans son inventaire, ou à
  ses pieds s'il est plein), **une seule fois** par joueur.
- En s'asseyant : « On the roof of the world, the seven stars turn above you. Stay seated ». En dessous du royaume :
  « The stars stay silent: they answer no one below Qi Liquidation ». Déjà reçu : « The seven stars have already
  answered you ».
- Un coup reçu interrompt la méditation, comme partout : les trois minutes recommencent.

## Vérifié

- `StarPeakGameTests` (4 tests) : le sommet sur le bloc le plus haut et la plateforme, les pentes qui descendent, le
  rayon ; le don après trois minutes, une seule fois ; rien sous Qi Liquidation ni hors du sommet ; la plateforme de jade.
- `gradlew -PgameTests runStarPeakTour` : un monde neuf, la montagne construite, vue de loin (sud, est, d'en bas), du
  sommet, et un joueur qui y médite et reçoit l'épée (les trois minutes raccourcies). Photos dans
  `build/star-peak-tour-run/screenshots`.
