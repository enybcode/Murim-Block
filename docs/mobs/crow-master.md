# Le Maître des Corbeaux (*Crow Master*)

Ermite de la Tour du Corbeau (`docs/structures/raven-tower.md`), gardien de l'Art du Corbeau messager
(`docs/combat/styles/raven-art.md`). Validé comme source de l'art par Enzo le 19/10.

- **Apparence** : vieil homme aux longs cheveux blancs noués en chignon (épingle d'or), sourcils broussailleux,
  moustache tombante et barbe blanche, yeux d'ambre comme son corbeau ; robe noire croisée bordée d'or sur une sous-robe
  grise, corbeau brodé d'or dans le dos, pèlerine de plumes noires sur les épaules, ceinture violette à bande ambre,
  pierre d'ambre, étui à message cerclé d'or en travers du dos. Modèle 3D Epic Fight `scripts/animation/crow-master-3d.cjs`
  (sur la base de Sensei Yun), 10 visages (`client/MobFaces`).
- **Son corbeau** : un Corbeau d'ombre (`RavenFamiliarEntity`) le suit toujours.
- **Clic droit** : une réplique (la première fois sa présentation, ensuite une au hasard), puis l'écran d'échange :
  rouleau de l'art 64 taels d'argent, éventail de fer 16, perchoir à corbeau 6, pilule de rassemblement du Qi 6.
- **Intouchable** : un coup ne rencontre que des plumes ; il passe par une porte et ressort derrière l'agresseur, puis
  refuse tout échange pendant 30 s. Ne disparaît jamais, reste dans sa tour (4 blocs).
- Œuf d'apparition dans l'onglet créatif ; `/summon murimblock:crow_master`.

Code : `mob/CrowMasterEntity`, `integration/epicfight/CrowMasterPatch`, `client/CrowMasterRenderer`.
Tests : `RavenArtGameTests.theCrowMasterSellsTheScrollAndMeetsBlowsWithFeathers`, `SiteGameTests.ravenTowerIsWholeAndCarried`.
Visite : `gradlew -PgameTests runRavenFamiliarTour`.
