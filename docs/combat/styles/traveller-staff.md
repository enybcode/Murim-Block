# L'Art du Bâton du Voyageur (Traveller's Staff Art)

> **État : implémenté sur `test` le 16/10/2026, en attente de validation en jeu par Enzo.** Source : la fiche de conception
> d'Enzo « Art fondamental du bâton » (16/10/2026). Les coûts, recharges et portées sont ceux qu'elle propose. Les noms en
> jeu sont en anglais (règle du projet). Les règles communes à tous les styles sont dans [../STYLES.md](../STYLES.md).

## L'idée

Un art de bâton tenu à deux mains : **pression au contact et protection frontale**. Le bâton du voyageur, en frêne avec
deux viroles de fer et une cordelette rouge, fait deux blocs de long ; ses animations le font glisser dans les mains.

## Le parchemin

- **« Traveller's Staff Scroll »** (Parchemin du Bâton du Voyageur). **Pour l'instant vendu par le marchand de l'Alliance au
  même prix que le parchemin du Poing Long** : 40 taels d'argent, un par réassort (Enzo, 16/10/2026). Aussi dans l'onglet
  Murimblock.
- La fiche ne définit ni quêtes, ni secret, ni évolutions : **le parchemin enseigne les trois techniques d'un coup**, placées
  sur les touches 1 à 3. C'est une règle générale : une technique qu'aucune quête ne débloque, et qui n'est pas un secret,
  vient avec le manuel (`MartialData.learn`). Aucun autre art n'est concerné.

## Le passif : « Third Resonance » (Troisième résonance)

- Les M1 sont ceux d'Epic Fight. Le bâton se bat comme une lance (`capabilities/weapons/traveller_staff.json`), mais
  l'art de la lance ne s'y applique pas.
- **Le troisième coup d'une chaîne fait le double de ses dégâts** (×2, avant la réduction de combo). La place du coup compte,
  même si les deux précédents ont raté. Une chaîne laissée 1,5 s repart au premier coup ; une technique la coupe.
- Le bonus ne s'applique pas aux techniques.

## Les techniques

Toutes les animations sont jouées **1,5 fois plus vite** que la fiche (demande d'Enzo, « trop lent »). Les temps ci-dessous
sont ceux du jeu.

| Technique | Qi | Recharge | Durée | Coups |
|---|---|---|---|---|
| **Vertical Strike** (Frappe verticale) | 12 | 6 s | 0,67 s | 1 coup ×1,5, contact à 0,30 s, 2,5 blocs devant |
| **Flurry of Points** (Rafale de pointes) | 18 | 9 s | 0,8 s | 4 × 0,3 puis 0,6 (léger recul), 3 blocs devant |
| **Spinning Rampart** (Rempart tournoyant) | 10 + 5/s | 12 s après la fin | 3 s au plus | aucun |

- **Frappe verticale** : levé au-dessus de la tête à deux mains, abattu droit devant, contact de l'extrémité. Pieds ancrés.
- **Rafale de pointes** : de profil, cinq estocs droits ; la main gauche guide, le bâton glisse dans les mains. Contacts à
  0,15 / 0,25 / 0,36 / 0,47 / 0,59 s. Le dernier pousse un peu. Une cible qui sort de l'axe évite les suivants.
- **Rempart tournoyant** : **touche maintenue**. Le bâton tourne devant le corps comme une hélice (un tour en 0,27 s).
  - Tout ce qui vient de **devant** (le demi-espace face au joueur) est arrêté, quel qu'en soit le type : dégâts, recul,
    étourdissement et effets. Les projectiles sont neutralisés (pas renvoyés). Le dos n'est pas couvert, ni ce qui ne vient
    d'aucun point (chute, poison déjà appliqué, noyade...).
  - Origine d'une attaque : l'attaquant en mêlée, la provenance pour un projectile, le centre pour une explosion.
  - 10 Qi au départ, puis 5 Qi par seconde ; il s'arrête au relâchement, à 3 s, à court de Qi, si le bâton quitte la main
    ou si le joueur est mis à terre. Sa recharge (12 s) part de sa fin.
  - Pendant la rotation : marche à 40 %, pas de saut ni d'autre technique. Toute autre action (frappe, esquive) arrête le
    rempart sans sa sortie.
  - Départ 0,1 s, sortie 0,17 s. La boucle est une « living motion » côté client (`StaffClient`), annoncée à tous les
    joueurs proches par `StaffSpinPayload` (protocole réseau 20).

## Ce qui est en jeu

- Code : `martial/TravellerStaff.java` (gameplay), `MartialArts` (l'art), `MartialCombat` (passif, crochets),
  `MartialService` (rampart tenu, recharge à la fin), `client/StaffClient.java`, `network/StaffSpinPayload.java`,
  `martial/TravellerStaffItems.java` (le parchemin), `MurimAnimations.buildStaffArt` (boîtes de coups).
- Animations : `scripts/animation/staff-art.cjs` (IK à deux segments, aucune traversée du corps ni des bras, vérifiée image
  par image ; projet Blockbench `art/blockbench/traveller_staff_art.bbmodel`). Bâton : `traveller-staff-model.cjs`.
- Icônes : `scripts/worldgen/TechniqueIcons.java` (style « road dusk »). Parchemin : `scripts/animation/traveller-scroll.cjs`.
- Tests : `TravellerStaffGameTests` (parchemin, frappe devant et pas derrière, rafale, troisième coup doublé, rempart
  devant/derrière, Qi, 3 s, recharge, relâchement). Vrai client : `gradlew -PgameTests runTravellerStaffTour` (écrans et
  icônes, barre de combat, les trois techniques au clavier) et `runStaffTour` (film des animations au ralenti).
- À décider : un lieu définitif pour le parchemin ; quêtes, évolutions, secret et finish éventuels.
