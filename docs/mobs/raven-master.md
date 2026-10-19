# Le Maître Corbeau (鸦主, *Raven Master*)

Maître errant de l'Art du Corbeau messager (`docs/combat/styles/raven-art.md`). En jeu, validé par Enzo le 19/10/2026 (look 2, le
masque de corbeau ; les deux maîtres coexistent). Ce n'est **pas** le Maître des Corbeaux de la Tour du Corbeau (`crow-master.md`), qui
vend le rouleau : les deux existent (choix d'Enzo). Celui-ci ne vend rien ; il garde l'art et le laisse à qui le bat.

## Allure

Le look n° 2 « Raven Mask » des concepts (`images/raven-master-2-*.png`), retenu : demi-masque de laque noire à bec de
corbeau et cercles d'ambre autour des yeux (la bouche reste nue et bouge, `client/MobFaces`), longs cheveux blancs
noués sur la nuque, robe bleu-noir bordée de bleu nuit, trois rangs d'épaulières de plumes, tablier bleu nuit ourlé
d'ambre, plaque de laque à la ceinture ; son corbeau d'ombre perché sur l'épaule gauche (pièce de son modèle, la forme
et les couleurs du familier), l'éventail de fer ouvert devant la poitrine (l'objet `iron_fan`, ouvert par `FanRenderer`).

- Modèle : `node scripts/animation/raven-master-3d.cjs game` → `animmodels/entity/raven_master.json`,
  `textures/entity/raven_master.png` et ses 9 visages, `art/blockbench/raven_master.bbmodel`.
- Mouvements : `scripts/animation/raven-master-motions.cjs` → `raven_master_idle` (4 s : immobile, l'éventail ouvert
  devant la poitrine, la main gauche dans le dos, la tête qui se penche d'un coup sur le côté et y reste, comme un
  oiseau) et `raven_master_walk` (grands pas silencieux, 1,1 s).

## Ses apparitions (`RavenMasterSightings`)

- Il erre seul : **un seul** Maître Corbeau dans le monde à la fois ; il va d'un cultivateur à l'autre.
- À partir de la **Condensation du Qi**, environ **toutes les 10 minutes** par joueur (7 min 30 à 12 min 30), si le
  joueur est dehors (ciel visible), en survie ou aventure, dans l'Overworld, hors difficulté paisible, `doMobSpawning`
  activé. Une fois battu par ce joueur, il vient deux fois moins souvent (15 à 25 min). L'échéance est gardée dans les
  données du joueur (survit à un redémarrage).
- Il se montre **loin (40 à 80 blocs)**, **dans le champ de vision** (±40° autour du regard), **perché** : on tire 96
  colonnes, chacune grimpe jusqu'au sommet le plus haut à 2 blocs près (cime d'arbre, rocher, toit : les feuilles
  comptent), au moins 3 blocs au-dessus du sol alentour (sans feuilles), ses yeux à vue des yeux du joueur ; on garde la
  plus dominante. Rien trouvé : nouvel essai 30 s plus tard.
- Il reste **immobile**, tourné vers le joueur, **trois corbeaux tournoyant** au-dessus de lui (`RavenFamiliarEntity`),
  sans barre de boss. Au bout de **20 à 30 s**, ou si le joueur s'approche **à moins de 20 blocs** sans croiser son
  regard, il s'envole dans un **nuage de corbeaux** (plumes, fumée noire, encre, envoyés aux joueurs jusqu'à 160 blocs)
  et disparaît.

## Le regard

- Hostile, mais il n'attaque **que si on le regarde dans les yeux** : le regard du joueur qu'il guette (celui à qui il
  se montre ; posé par un œuf, le plus proche) sur son visage (le test de l'Enderman : cosinus > 1 − 0,025/distance), à
  moins de 64 blocs, rien entre eux. Le frapper (une flèche de loin, n'importe quel joueur) revient au même.
- Alors il éclate en corbeaux sur son perchoir, ses corbeaux s'égaillent, et il **ressort d'une porte des Salles du
  Corbeau à 2,5–5 blocs devant le joueur** (« You met the Raven Master's eyes. »), et se bat.

## Le combat

Boss difficile, dans l'esprit de Sensei Yun : **300 PV**, armure 10 (robustesse 4), recul résisté à 90 %, armure de
stun 16, 9 par coup (6 à lui, 3 de l'éventail). Barre de boss **violette** « Raven Master » (10 crans), visible seulement
pendant le combat. Tout est parable (garde Murim).

- **L'éventail de fer** (`RavenMasterPatch`) : les quatre M1 de l'éventail enchaînés (le revers, le contre-revers, le
  cercle au-dessus de la tête, la fente), le revers puis la fente, et la fente en courant sur qui reste à 3–6 blocs.
- **La porte d'ombre** : l'éventail balayé (`halls_step`), il passe par une porte et ressort **2 blocs derrière** son
  adversaire, face à lui (portes `RavenFx` aux deux bouts). Toutes les 6 à 10 s (3 à 5,5 s en seconde phase), surtout si
  l'adversaire est à plus de 6 blocs.
- **La nuée de corbeaux** : l'éventail pointé (`raven_exchange`), 5 corbeaux (8 en seconde phase) partent de lui, volent
  sur l'adversaire et le picorent **4 fois** (une toutes les 0,3 s, 35 % de son attaque chacune), en le suivant
  lentement. Chaque coup de bec est un coup de lui : la garde le prend, **une parade parfaite disperse la nuée**. Toutes
  les 15 à 20 s (9 à 12 s en seconde phase), à moins de 16 blocs et à vue.
- **Seconde phase**, sous la moitié de ses PV : nuage de corbeaux, « You have looked long enough. Now the ravens will
  have your eyes. », 40 % plus rapide, portes et nuées plus fréquentes, nuée plus grande. Elle dure jusqu'à ce qu'il soit
  entier.
- **Fin de la rencontre** : s'il perd son adversaire de vue **10 s**, si celui-ci fuit **au-delà de 48 blocs**, meurt,
  passe en créatif ou spectateur, il se disperse en corbeaux (« The Raven Master scatters into ravens. He is gone. ») et
  retourne à son errance : la prochaine apparition suit le rythme habituel.

## Battu

- Tué par un joueur : **toujours le rouleau de l'Art du Corbeau** (`raven_art_scroll`) et 15 à 20 taels d'argent
  (+0 à 3 par niveau de Pillage) (`loot_table/entities/raven_master.json`). Rien s'il meurt autrement. 100 XP.
- **Qi** : première victoire **+40 de Qi Max** (`CultivationRules.firstVictoryQiMax`), récompense de Qi de base 35,
  bonus de première victoire 120 (`QiRewardManager`). Lu par le Sens du Qi comme Condensation du Qi, pic.
- Il peut revenir ensuite, deux fois moins souvent. Il ne peut pas être avalé par la Porte qui avale (tag
  `raven_unswallowable`, comme les autres boss).

## Œuf et commande

Œuf `Raven Master Spawn Egg` (onglet Murimblock), `/summon murimblock:raven_master` : posé ainsi, il n'est pas
« fugitif » : il reste là (sauvegardé), regarde le joueur le plus proche (corbeaux qui tournent, récupère 2 PV/s), se bat
avec qui croise son regard et, la rencontre finie, se remet à guetter au lieu de disparaître. Celui des apparitions
n'est jamais sauvegardé (rechargé, il ne guetterait personne).

## Fichiers

- Code : `mob/RavenMasterEntity`, `mob/RavenMasterSightings`, `integration/epicfight/RavenMasterPatch`,
  `client/RavenMasterRenderer`, `MurimAnimations.RAVEN_MASTER_IDLE/WALK`, `MurimEntities.RAVEN_MASTER`,
  `MurimItems.RAVEN_MASTER_SPAWN_EGG`. Pas de nouveau paquet réseau (protocole inchangé).
- Tests : `RavenMasterGameTests` (l'éventail et ses mouvements, ignore qui ne croise pas son regard et sans barre de
  boss, croiser son regard le fait combattre à côté du joueur avec sa barre, le fugitif s'en va quand on approche, la nuée
  picore, la porte d'ombre derrière l'adversaire, le rouleau toujours, apparitions dès la Condensation et deux fois moins
  souvent une fois battu, unique) : `gradlew -PgameTests -PgameTestFilter=raven runGameTestServer`.
- Visite en vrai client : `gradlew -PgameTests runRavenMasterTour` (monde plat, un grand épicéa et un rocher : de loin
  par-dessus l'épaule, au zoom, de côté sur son perchoir ; le regard croisé, le nuage sur l'arbre, son arrivée ; le
  combat, la nuée, une porte, la barre de boss ; portraits de face, de trois quarts, de côté, de dos, l'éventail) ;
  rapport `build/raven-master-tour-run/raven-master-tour-report.txt`, images `build/raven-master-tour-run/screenshots`.

## Limites connues

- « Son coin du monde » est pris comme : un seul Maître Corbeau à la fois, qui erre d'un joueur à l'autre. Pas de
  région ni de repaire attitré (à décider avec Enzo).
- Ses techniques sont les siennes, pas celles du joueur (pas de chambre close, pas d'échange de places).
- L'éventail est tenu dans la pose de l'art (devant l'épaule droite) ; à revoir si Enzo le veut plus nettement devant le
  visage.
- Difficulté réglée à l'œil contre un joueur immobile : à ajuster après un vrai combat. Vérifié en solo (GameTests,
  vrai client) ; à valider à deux clients.
