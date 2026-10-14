# Sensei Yun (云师父)

Maître de la Secte du Nuage Azur, inspiré de Sensei Wu : un vieux maître d'épée en nuances de bleu. Il garde le Temple
taoïste de la Mer de Bambous brumeuse (`misty_bamboo_temple`, fiche `docs/structures`) : la structure le place dans le
Hall des Trois Purs, entre l'autel et les coussins, face aux portes. Aussi par œuf (`Sensei Yun Spawn Egg`) ou
`/summon murimblock:azure_sensei`.

![Sensei Yun en jeu](images/sensei-ingame.png)

## Allure

Modèle 3D sur le squelette biped d'Epic Fight (`scripts/animation/sensei-3d.cjs`, 63 pièces) :

- chapeau de paille en cinq gradins, bande et bouton azur ; crâne chauve sous le chapeau, couronne de cheveux blancs
  derrière, sourcils broussailleux, moustache et longue barbe blanche à anneau azur ;
- robe azur croisée, col et revers bordés de blanc et de bleu ciel sur une sous-robe blanche, nuage blanc brodé dans
  le dos ; larges manches à revers bleu ciel tombant sur les mains, le dos de chaque manche plus bas ;
- ceinture indigo à liseré azur, fermoir nuage d'argent, nœud et pans à gauche, pendentif de jade blanc et sa houppe à
  droite ; jupe de robe fendue sur chaque jambe, ourlet bleu ciel à nuages blancs ; pantalon indigo, chaussettes
  blanches, chaussons de toile ; le fourreau laqué vide de son épée en travers du dos.
- Visage vivant (`MobFaces`, 10 atlas) : clignements, regards, marmonnement ; en combat les sourcils se nouent, il crie
  quand il frappe.

Son épée, **Azure Cloud** (蒼雲劍) : un jian droit à double tranchant, nuage gravé et incrusté de bleu à la racine
des plats, garde d'argent en volutes de nuage à émail bleu, poignée de soie azur entre deux bagues d'argent, pommeau
en tête de nuage (ruyi) à saphir, longue houppe azur à perle de jade blanc. Images `images/azure-cloud-sword-*.png`.

## Comportement

- **Neutre** : il se promène dans son hall (6 blocs autour de sa place, une promenade toutes les 12 s en moyenne),
  regarde qui entre, n'attaque que qui le frappe. Ne disparaît jamais, garde sa place dans sa sauvegarde (`Home`).
- **Clic droit** : il donne à chaque joueur son exemplaire du **Azure Cloud Sword Manual**, une fois (le même que la
  bibliothèque du sanctuaire : un joueur en reçoit un, de l'un ou de l'autre). Pas à quelqu'un qu'il combat.
- **Combat** : 80 PV, armure 6, recul résisté à 50 %, armure de stun 6. Pas d'attaque propre : l'Azure Cloud donne 6.
  Il frappe avec les coups M1 de l'Art de l'Épée du Nuage Azur (`AzureSenseiPatch`) : Drifting Cut puis Returning Cut
  (≤ 2,5 blocs), Falling Rain plus rare (recharge 5 s). Tous parables (garde Murim). Les techniques de l'art (Cloud Step
  Slash, Thousand Drizzle…) ne sont pas encore branchées sur lui.
- Butin : rien pour l'instant (l'épée ne tombe pas) ; les drops seront décidés plus tard. 30 XP.

## Mouvements

`scripts/animation/sensei-motions.cjs` → `animations/biped/murim/sensei_idle.json`, `sensei_walk.json` :

- **idle** (5 s en boucle) : un peu voûté, main gauche dans le dos (avant-bras en travers des reins), l'épée tenue
  basse à droite, pointe vers le sol devant ; respirations lentes, regard de côté ; il porte la main à sa barbe et la
  caresse deux fois, la remet dans son dos, regarde de l'autre côté, hoche la tête. Chaque sensei commence son idle à
  son propre moment (`IdlePhase`).
- **marche** (une foulée en 1,2 s, calée par Epic Fight sur son allure) : petits pas mesurés, genoux souples, léger
  balancement des épaules, main toujours dans le dos, épée basse qui balance un peu.
- L'épée est tenue 40° sous l'horizontale et décalée dans le poing (`HOLD`) : Epic Fight tient la poignée d'une épée
  1,5 pixel au-dessus du joint de la main, sous sa large manche ; abaissée ainsi, le pommeau s'enfoncerait dans
  l'avant-bras et la houppe avec. Décalée, la poignée est dans le poing, le pommeau dépasse derrière la manche et la
  houppe pend librement (`checkHold()` l'affiche à chaque génération). En combat, les coups M1 reprennent la tenue
  standard : la poignée y est sous la manche.

![La garde et la houppe derrière le poing](images/sensei-ingame-hilt.png)

## Fichiers

- Modèle : `scripts/animation/sensei-3d.cjs` (avec `mesh-kit.cjs`) → `animmodels/entity/azure_sensei.json`,
  `textures/entity/azure_sensei.png` et ses 9 visages, `art/blockbench/azure_sensei.bbmodel`.
- Épée : `scripts/animation/azure-cloud-sword-model.cjs` → `models/item/azure_cloud_sword(_body|_tassel|_held).json`,
  `textures/item/azure_cloud_sword(_3d).png`, `art/blockbench/azure_cloud_sword.bbmodel` ; icône dessinée d'après le
  modèle. En main, `client/TasselSwordRenderer` dessine la lame puis la houppe balancée par `TasselPendulum` (comme le
  Dao de bandit). Images de présentation : `scripts/concepts/sensei-preview.cjs`.
- Code : `AzureSenseiEntity`, `AzureSenseiPatch`, `AzureSenseiRenderer`, `MurimAnimations.buildAzureSensei`,
  `SectLibrary.give`, `BambooTemple` (sa place dans le hall).
- Tests : `AzureSenseiGameTests` (épée et sa catégorie, idle et marche, maison sauvegardée, neutralité, manuel donné une
  fois, coups du Nuage Azur), `SiteGameTests` (le temple compte 1 sensei) ; en vrai client
  `gradlew -PgameTests runSenseiTour` (idle de face et de côté, tour de caméra autour de la garde, marche de profil,
  combat contre un zombie d'entraînement ; rapport `build/sensei-tour-run/sensei-tour-report.txt`).

## Limites connues

- Il n'utilise encore que les coups M1 de l'art, pas ses techniques (Cloud Step Slash, Thousand Drizzle, etc.).
- Les projets Blockbench d'aperçu placent l'épée approximativement dans sa main ; la vraie place est celle du jeu
  (`checkHold()`, `runSenseiTour`).
- Vérifié en solo (GameTests serveur, vrai client) ; à valider à deux clients.
