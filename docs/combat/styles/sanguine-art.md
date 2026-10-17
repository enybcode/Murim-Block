# Sanguine Art — Blood Crystal

Branche personnelle **Colleuhu95**. Révision du 14 octobre 2026 demandée par Colleuhu95 : remplacer Bloodbane Drain par un cristal chargé puis lancé, utiliser Scarlet Tear et les M1 pour préparer la forme finale fournie dans `cristal-de-sang-kit.zip`, et consommer les cristaux dorsaux sans quitter la forme.

## Fonctionnement validé

Sanguine Art doit être le style principal. Les deux mains sont vides. Le joueur conserve son skin et son équipement : la forme est une surcouche transparente animée par le squelette Epic Fight, pas un objet d'armure.

La jauge est plafonnée à 20 (15/10/2026, demande d'Enzo ; 10 avant). Un M1 qui touche un ennemi donne 1 point ; Scarlet Tear donne 3 points par utilisation réussie, même si plusieurs cibles sont touchées. Air, parade, garde, allié et animal ne donnent aucun point. La jauge pleine attend une activation manuelle, sans transformation automatique.

| Case initiale | Technique | Qi | Recharge | Action |
|---|---|---:|---:|---|
| 1 | Blood Crystal | 150 | 160 ticks | Maintenir pour charger un cristal à la main droite, relâcher pour lancer. Charge minimum 12 ticks, maximum 30 ticks. Lancement 6 ticks après le geste ; portée 24 blocs, vitesse 0,9 bloc/tick, dégâts ×1,2 de l'attaque. Aucun soin. |
| 2 | Scarlet Tear | 225 | 200 ticks | Arc frontal Epic Fight, dégâts ×1,5, stun long. Ajoute 3 points à la jauge sur un contact valide. |
| 3 | Blood Crystal Form | 300 | 80 ticks | Jauge pleine requise. Consomme la jauge et active la surcouche finale avec **30 % de réduction des dégâts reçus** (15 % avant le 15/10/2026, demande d'Enzo), fixe et globale. |
| 4 | Crystal Barrage | 180 | 120 ticks | Lance tous les cristaux gagnés aux M1, un tous les 2 ticks après un geste de 6 ticks. Chaque projectile inflige ×0,45 de l'attaque : davantage de cristaux donne davantage de dégâts cumulés. |

Dans la forme, les M1 réussis ajoutent un cristal dorsal, jusqu'à 10. Scarlet Tear ne génère pas de cristaux dorsaux. La salve consomme immédiatement les cristaux gagnés ; **la forme et la protection restent actives**. Les M1 suivants peuvent regagner des cristaux. Les pointes fixes de la surcouche restent visibles à zéro cristal gagné.

Les dégâts des techniques utilisent également le facteur Qi commun de `TechniquePower`, intégré depuis `origin/test`. Le garde-fou PvP commun `BurstGuard` reste applicable : une salve plus fournie augmente les dégâts jusqu'à sa limite commune, sans permettre de tuer instantanément un joueur. Les M1 conservent les dégâts normaux des poings.

La fin du combat est la sortie du mode combat Epic Fight, touche V. Aucune expiration à 30 secondes. Mort, K.O., changement de dimension, déconnexion, changement de style ou main occupée retirent également cet état. Une interruption du geste annule les projectiles encore attachés ; les cristaux déjà lancés finissent leur trajet normal. Les dépenses ne sont pas remboursées.

Les projectiles suivent une collision serveur et s'arrêtent sur les murs, sont parables et ne traversent pas une cible. Leur type de dégâts ignore uniquement le délai d'immunité entre coups pour permettre une vraie salve ; il conserve armure, absorption, invulnérabilité et garde. La protection ne s'applique pas aux sources qui contournent l'invulnérabilité.

## Progression

Le rouleau apprend Blood Crystal et le passif Crimson Stance. 20 cristaux simples qui touchent un ennemi débloquent Scarlet Tear ; 15 Scarlet Tear réussis débloquent la forme ; 10 M1 réussis dans la forme débloquent Crystal Barrage. Les identifiants sauvegardés `bloodbane_drain` et `devourer_of_worlds` restent stables malgré les nouveaux noms.

## Assets et rendu

La géométrie et la texture approuvées du kit sont conservées dans `art/blockbench/blood_crystal_body.bbmodel`, `animmodels/overlay/blood_crystal_body.json` et `textures/entity/blood_crystal.png`. Le skin reste visible à travers la transparence. Steve et Alex utilisent le même asset avec ajustement des bras dans les matrices de la surcouche. La vue Epic Fight à la première personne suit la visibilité de ses bras ; le fallback Minecraft dessine seulement le bras visible.

Les 10 cristaux flottants et leur hover viennent du kit. Le projectile est dérivé d'un seul de ces cristaux, recentré à l'origine. Les mouvements du joueur sont reproductibles via `sanguine-art.cjs` et `export-sanguine.cjs`. Le projet du personnage contient la charge, le lancer, Scarlet Tear, la transformation, la salve, la garde et le combo ; aucun ancien drain ou vortex.

La charge suit l'articulation `Tool_R` du personnage. En première personne Epic Fight, le cristal est dessiné dans cette même articulation du modèle des bras, afin de suivre le poignet dans la vue rapprochée.

**Pas de particules** (décision d'Enzo, 14/10/2026 : elles enlaidissaient l'art). Seules les animations, les cristaux et l'armure se voient ; Photon ne donne qu'un bref éclair de lumière rouge au sommet du lancer, de Scarlet Tear, de la cristallisation et de la salve. Vos propres cristaux du dos et l'armure de cristal ne sont pas dessinés à la première personne, où leurs éclats couvraient l'écran ; les autres joueurs et la troisième personne les voient. Les coups de l'art n'ont pas non plus la bouffée blanche d'Epic Fight à l'impact (type de particule vide `murimblock:no_hit`). Versions Minecraft 1.21.1, NeoForge 21.1.248 et Epic Fight 21.17.3.1 inchangées.

## Vérification

`gradlew.bat build compileGameTestJava`, `gradlew.bat -PgameTests runGameTestServer`, puis `gradlew.bat -PgameTests runSanguineTour`. Les tests couvrent charge et collision, interruption, progression par contacts réels, activation manuelle, persistance, consommation, dégâts de salve, protection fixe et parade. La tour native (`runSanguineTour`, dans `build.gradle`) produit les captures et vérifie les touches, et que vos cristaux du dos restent hors de la vue à la première personne. Aucun monde de test ne fait partie du dépôt.

Avant la fusion d'`origin/test`, le test `CultivationGameTests.theRemnantSoulGivesTheDaysMissions` échouait dans un monde de test réutilisé et passait dans un monde neuf. Les changements de cultivation reçus ensuite proviennent de cette fusion. Les fixtures de cet art réutilisent les chunks simulés de l'origine et nettoient joueurs et cibles, même lors d'un échec, pour éviter les interférences entre tests.

## Blood Crystal, refait le 15/10/2026 (demande d'Enzo)

Sur le modèle de la boule d'eau du Crab Man :

- **Dans la main** : le cristal se forme (animation GeckoLib `form`), six petits fragments tournent autour de lui (`charge`). Il est penché vers l'avant.
- **La charge consomme la jauge** : après 0,6 s, tant qu'on reste appuyé, il tire un point de la jauge de sang toutes les 0,1 s (`BloodCrystalCharge` : la jauge entière en 2 s) et grossit (échelle 0,6 + 0,17 par point). Dégâts : ×1,2 + 0,09 par point. À jauge pleine, le grand cristal est donc le même qu'avec l'ancienne jauge de 10.
- **À 20 points** (la jauge pleine), c'est le **grand cristal** : 3 blocs de large, plus lent et plus lourd. À l'impact, il **explose** (rayon 3,5 blocs, ×2 au centre à ×1 au bord, recul), sans casser de bloc. Le corps touché directement prend les dégâts du cristal, pas l'explosion.
- **Physique** : lancé comme une pierre (traînée 0,99, gravité 0,025, 0,035 pour le grand), visé pour arriver où regarde le lanceur ; il tourne sur lui-même en vol. Les cristaux de la salve restent droits.
- **Impacts** : il éclate selon ce qu'il touche (`shatter_ground`, `shatter_wall`, `shatter_body`), comme la boule d'eau ; le grand cristal joue `explode`. Un bref éclair rouge Photon à chaque impact ; **seule l'explosion lance des particules** Photon (160 éclats aux rouges du cristal, qui retombent et rebondissent).
- Il part seul après 3 s. Interrompue, la charge perd les points tirés.
- Modèle, animations, texture et éclat : `node scripts/animation/blood-crystal-shot.cjs`. Tests : `holdingDrawsTheGaugeIntoTheCrystal`, `aFullGaugeMakesAGreatCrystalThatExplodesWithoutBreakingBlocks`, `BloodCrystalChargeTest`, et la séquence « crystal-great » de `runSanguineTour`.

## Scarlet Tear, refait le 15/10/2026 (demande d'Enzo)

L'épée de sang, dans le style du cristal (même texture) : pommeau, poignée, garde en éclats, lame de six segments, pointe ; environ 2 blocs.

- **Formation (0 à 0,65 s)** : l'avant-bras droit se lève à côté du corps et l'épée se forme du manche vers la pointe, morceau par morceau (animation GeckoLib `form`), vers le ciel.
- **L'arc (0,7 à 0,9 s)** : l'épée remonte à l'épaule, le torse vrillé, puis un grand arc horizontal de droite à gauche avec un pas en avant (×1,5, plusieurs cibles, +3 à la jauge comme avant).
- **Le lancer de javelot (1,05 à 1,5 s)** : le corps se met de profil, le bras droit tendu loin en arrière, le bras gauche pointé vers la cible, le poids en arrière ; blocage, puis fouetté (hanches, torse, coude haut) et lâcher **pointe la première** à 1,35 s, et le corps bascule vers l'avant. Après l'arc, l'épée est tenue comme un javelot, pointe vers l'avant du corps, quoi que fasse le bras, visée où regarde le lanceur. Elle **explose** à l'impact : coup direct ×1,2, explosion de 2,5 blocs (×1,2 au centre, moitié au bord), 80 éclats Photon, aucun bloc cassé. Elle explose dans l'axe de son vol.
- Dans la main, l'épée prolonge l'avant-bras (direction coude → main calculée à chaque image) ; plus petite en première personne.
- Une autre animation qui commence fait disparaître l'épée encore en main.
- Fichiers : `node scripts/animation/blood-sword.cjs` (modèle et animations), `sanguine-art.cjs` (le corps). Test : `scarletTearSwingsItsBloodSwordThenThrowsItToExplode` ; séquence « scarlet-sword » de `runSanguineTour`.

## Scarlet Tear en double activation (15/10/2026, demande d'Enzo)

- **1re activation** (24 Qi, recharge 10 s) : l'épée de sang se forme dans la main (animation `sanguine_sword_summon`, 1 s : l'avant-bras levé, l'épée pousse vers le ciel, puis un moulinet et la garde). Elle reste en main : **on se bat avec comme avec une épée**. Le clic gauche enchaîne les quatre coupes d'épée (copies des frappes Azure, `sanguine_blade_*`, sans bouffée d'Epic Fight), +5 d'attaque tant qu'elle est sortie ; chaque coupe qui touche donne +1 à la jauge, comme un coup de poing.
- **Tant qu'elle est sortie, aucune autre technique** (refus « throw your blood sword first »).
- **2e activation** (gratuite, malgré la recharge, comme le plongeon du croissant) : le **lancer de javelot** (`sanguine_sword_throw`, 0,95 s, l'épée part pointe la première à 0,85 s, après une course d'élan, voir plus bas) ; elle explose à l'impact. Un lancer qui touche donne +3 à la jauge (hors forme) et compte pour la quête Scarlet Release.
- Elle disparaît si le joueur quitte le mode combat, prend un objet en main, est mis K.O. ou meurt.
- L'ancien arc de cercle est retiré : les coupes à l'épée le remplacent.

## L'épée « Cristal fendu » (15/10/2026, choix d'Enzo)

- Choisie parmi quatre styles plus sombres et plus détaillés (sang coagulé, lame-veine, croc du démon, cristal fendu).
- Lame de cristal pourpre très sombre en six segments légèrement de travers, fendus de fissures lumineuses (une en travers, une le long) ; un éclat flotte à côté de chaque segment et dérive autour de la lame tant qu'elle est en main ; garde en couronne de six pics de cristal ; gemmes au pommeau et à la pointe.
- Texture propre `textures/entity/blood_sword.png` (64 × 64) et `blood_sword_glowmask.png` : seules les fissures, les éclats lumineux et les gemmes brillent dans le noir. Le cristal garde sa texture.
- Mêmes noms d'os que l'ancienne épée : formation pièce par pièce, vol et explosion inchangés. Tout est généré par `node scripts/animation/blood-sword.cjs`.

## L'invocation en dégainé (15/10/2026, demande d'Enzo)

- Vu de l'extérieur, le joueur **dégaine** son épée au lieu de se mettre en garde (`sanguine_sword_summon`, 1,15 s) : la main droite va chercher la poignée à la hanche gauche, la main gauche tient le fourreau (invisible), le corps se ramasse et se vrille ; puis il dégaine d'un coup en grand arc horizontal vers la droite, la main gauche ramenant le fourreau en arrière ; un coup sec pour chasser le sang (chiburi : la lame se lève puis claque le long de la jambe), et la garde.
- La lame se forme le long du fourreau, derrière la hanche, pointe en arrière et un peu vers le bas, comme tirée d'un fourreau invisible (jusqu'à 0,38 s) ; elle prolonge ensuite le bras pendant l'arc et le chiburi, puis passe dans la prise normale à partir de 0,84 s (`BloodCrystalOverlay.DRAW`, `GRIP_FROM`, `bodyDirection`).

## M1 de l'épée : le combo d'épée de base d'Epic Fight (15/10/2026, demande d'Enzo)

- Les coupes Azure étaient propres à l'Épée du Nuage Azur : tant que l'épée de sang est sortie, le clic gauche joue désormais le combo d'épée de base d'Epic Fight (`SWORD_AUTO1` à `AUTO3`, l'attaque en course `SWORD_DASH` et l'attaque aérienne `SWORD_AIR_SLASH`), comme n'importe quelle épée.
- Leurs étincelles d'impact sont coupées pour l'épée de sang seulement (`SanguineHitSparkMixin`, `SanguineArt.silencesHit`) : l'art reste sans particules ; les autres épées gardent les leurs.
- L'épée étant tenue main vide, Epic Fight lui donnerait la portée d'un poing : elle coupe avec la portée d'une épée longue (`SanguineSwordReachMixin`, `ColliderPreset.LONGSWORD`).
- Le bonus (+5 d'attaque) et la jauge (+1 par coupe qui touche) ne changent pas. Les copies `sanguine_blade_*` sont retirées.
- Tranchant (15/10/2026, demande d'Enzo) : en main, l'épée n'était orientée que par l'axe de sa lame, sa rotation autour de la lame laissée au hasard, si bien qu'elle frappait du plat. Ses tranchants (±X du modèle) sont maintenant dans le plan de l'avant-bras et de la lame, comme une vraie prise, et ses plats face aux côtés (`BloodCrystalRenderer.inHandOrientation`) : les coupes d'Epic Fight portent sur le fil.
- Première personne (15/10/2026) : en garde, Epic Fight tient la main vide basse, hors de l'écran ; l'épée est levée devant les yeux (vers le haut et un peu vers l'intérieur) pour qu'on la voie, et revient en douceur dans la prise pour les coupes, le dégainé et le lancer (`BloodCrystalOverlay.firstPersonRaise`).

## Le lancer avec élan (15/10/2026, demande d'Enzo)

- `sanguine_sword_throw` refait en vrai lancer de javelot, 1,3 s : l'épée portée au-dessus de l'épaule droite, pointe devant ; **course d'élan** de trois foulées (le bras gauche balance) ; **pas croisé** qui met le corps de profil, bras droit tendu loin en arrière, poids en arrière ; **blocage** de la jambe gauche plantée loin devant ; **fouetté** des hanches au torse puis au coude haut au-dessus de l'épaule ; lâcher pointe la première à **0,85 s** (`SanguineArt.SWORD_RELEASE_TICKS` = 17) ; le corps bascule par-dessus la jambe avant, la jambe droite repasse, un dernier pas ramène en garde.
- Les pieds marchent vraiment : chaque clé peut porter ses propres cibles de pieds (`F(R, L)` dans `sanguine-art.cjs`, interpolées dans `bake`), un pied posé reste fixe pendant qu'Epic Fight avance le joueur avec la racine. Le joueur avance d'environ 1,65 bloc.

## La transformation, ses 30 secondes et sa fin (15/10/2026, demande d'Enzo)

- **Une vraie transformation, douloureuse** (`sanguine_crystal_form`, 2,1 s) : le joueur se plie, les deux mains crispées sur la poitrine, les genoux qui lâchent, et tremble ; puis il se ramasse et, à 1,2 s, les cristaux éclatent hors du corps : dos cambré, bras grands ouverts, tête renversée dans un cri ; il se redresse en garde. Sons serveur : cri de douleur (3 et 13 ticks), craquements d'améthyste de plus en plus forts, éclatement à 24 ticks (`SanguineArt.tickForm`).
- **Les cristaux poussent, sans étapes** : pendant l'animation, la croûte et les éclats sortent du corps joint par joint (la poitrine d'abord, puis le ventre, les épaules, les bras, les avant-bras, les cuisses, les jambes), en rampant avec la douleur jusqu'à 42 % en tremblant, puis jaillissent à 1,05 s (108 % au pic) et se posent à 1,45 s (`BloodCrystalOverlay.growth`, `delayOf` : chaque joint du mesh est mis à l'échelle autour de son pivot).
- **30 secondes, combat ou non** (`SanguineArt.FORM_TICKS` = 600) : quitter le mode combat n'enlève plus la forme ; il arrête seulement la charge, l'épée et la salve. La défense de 30 % et les cristaux du dos restent jusqu'à la fin.
- **La fin** : les cristaux se détachent. Le corps : 13 cristaux partent de la poitrine, des épaules, des avant-bras, du dos, des cuisses et des genoux (`BloodCrystalEntity.shed`), ceux du dos se décrochent aussi (`drop`) ; ils tombent pointe en avant (état `FALLING`, sans dégâts ni lumière) et se brisent au sol (animation `shatter_ground`). Sur le corps, la croûte rétrécit en 5 ticks pendant qu'ils tombent.
- Tests : `fullGaugeNeedsTheKeyAndFormLastsThirtySecondsThenSheds`, `exitingCombatKeepsTheFormAndItsCrystals`, la défense hors combat ; tour : séquences `form-again` et `form-shed`.

## Blood Iceberg, la dernière technique (15/10/2026, demande d'Enzo)

- **5e technique** (`blood_iceberg`, 750 Qi, **recharge 9 min** = 10 800 ticks) ; quête « Crimson Peak » : tenir trois Blood Crystal Forms jusqu'à leur fin (`crimson_peak.forms`). Icône dans `TechniqueIcons.java` (une montagne de cristal, une silhouette au sommet).
- **Animation** (`sanguine_iceberg`, 1,7 s) : un genou presque à terre, les deux paumes frappent le sol (0,3 s), puis le joueur se redresse avec la montagne, bras levés, et se tient au sommet bras ouverts.
- **La montagne** (`BloodIceberg`) : 30 × 30 blocs (un carré arrondi), sommet à 9 blocs au-dessus des pieds, flancs irréguliers et 12 flèches de cristal (+3 à +7,5) autour du sommet, plate au milieu pour s'y tenir ; elle comble la pente jusqu'à 12 blocs sous les pieds. Elle monte du sol en 1 s (6 ticks après le cast), couche par couche, et **porte en haut** le joueur et tout ce qui se trouve dessus (`lift`). Elle ne remplit que l'air et ce qu'un bloc peut remplacer (herbe, fleurs, neige) : elle ne casse jamais un bloc ; l'herbe et les fleurs remplacées ne reviennent pas.
- **Les blocs** (`SanguineBlocks`) : `blood_crystal_block` et `blood_crystal_vein` (veines lumineuses, lumière 9, 12 % des blocs et le haut des flèches) ; textures en facettes de cristal générées par `scripts/animation/blood-iceberg-blocks.cjs`. Aucun objet, aucun butin. Chaque bloc programme sa disparition (tick planifié, sauvegardé avec le chunk) : **au bout de 60 s, la montagne s'effrite du haut vers le bas** en quelques secondes, et rien ne reste même si le serveur s'arrête avant.
- **Sur la montagne** (au-dessus, sur les flancs ou à son pied, tant qu'elle tient) : **Force I et Régénération I**, sans particules, rafraîchies chaque seconde ; **un cristal de sang toutes les 3 s** (un cristal dans le dos en forme, sinon un point de jauge) (`SanguineArt.icebergCrystal`).
- Tests : `BloodIcebergTest` (la forme), `bloodIcebergRaisesAMountainGivesItsGiftsThenCrumbles` (montée, sommet, effets, cristal, recharge, disparition complète après 60 s) ; tour : séquence `iceberg`.

### L'arène (15/10/2026, choix d'Enzo)

- La montagne devient une **arène de 45 blocs** de diamètre (`BloodIceberg.shape`) : une piste ronde et plate 3 blocs au-dessus des pieds (rayon 16), avec un **sceau de sang lumineux** (deux cercles, huit rayons tournés vers les flèches, le centre) en cristal veiné ; deux gradins (+2, +4) ; une enceinte crénelée de 6 blocs au-dessus de la piste avec **huit grandes flèches** (+6) ; un talus extérieur jusqu'au sol. Montée en 1,5 s (30 ticks) ; tout ce qui s'y trouve est porté sur la piste ou l'enceinte. Environ 8 000 blocs, posés sans mise à jour des voisins.
- Blocs au style **« gemme taillée »** (choisi parmi quatre propositions) : grandes facettes rouge vif éclairées d'en haut à gauche, arêtes claires, éclats ; le veiné traversé d'une veine de sang (`blood-iceberg-blocks.cjs`).
- Test : gabarit vide `murimblock:iceberg_arena` de 48 × 48 (`FoundationGameTests`), le joueur au milieu, pour que l'arène n'empiète pas sur les tests voisins.

### Le temple de sang et son sang qui coule (15/10/2026, choix d'Enzo)

- **Style « Temple de sang »** (choisi parmi quatre ensembles : obsidienne, os et sang, temple, glace) : pierre laquée rouge et or. Six blocs (`SanguineBlocks`) : `blood_temple_rock` (base, talus, dessous), `blood_temple_bricks` (gradins, mur, rigole), `blood_temple_polished` (piste, faîte du mur), `blood_temple_seal` (le sceau d'or incrusté dans la piste, lumière 4), `blood_temple_gold` (cristal d'or des flèches, lumière 6), `blood_temple_lantern` (pointe des flèches et lanternes serties dans le mur, lumière 15). Les anciens `blood_crystal_block` / `_vein` sont retirés.
- **Le sang, un liquide** (`SanguineFluids`, `BloodLiquidBlock`, rendu `client/BloodFluidClient`) : plus épais que l'eau (tick 8, s'étend sur 4 blocs), ne crée jamais de source, on y nage, brouillard rouge sombre dedans ; textures fixe et coulante animées (`blood_still`, `blood_flow`). Pas de seau.
- **Dans l'arène** : une **rigole de sang** fait le tour de la piste (un bloc plus bas, au pied des gradins) ; **huit cascades** tombent d'une encoche du faîte du mur, à mi-chemin entre deux flèches, dévalent les gradins et se jettent dans la rigole. Le sang est versé une fois l'arène dressée ; ses sources se tarissent 10 s avant l'effondrement (tick planifié, `DRY_BEFORE`), le sang qui coule s'écoule de lui-même, et il n'en reste rien.
- Formes : `BloodIceberg.shape` (parties FLOOR, CHANNEL, TIER, WALL, SPIRE, FALL, SLOPE) et `kind` (quel bloc à quelle hauteur). Textures : `node scripts/animation/blood-iceberg-blocks.cjs`. Le test d'arène vérifie aussi le sang (rigole et cascades, plus rien à la fin).

## Les fioles de sang (15/10/2026, demande d'Enzo)

- **Blood Vial** (`SanguineItems`, `BloodVialItem`, pile de 16, onglet Murimblock) : on remplit une **fiole vide dans le sang** (rigole ou cascades du temple de Blood Iceberg) comme dans l'eau (`SanguineItems.fill`, sur `RightClickItem`).
- **La boire** (1,6 s, animation de boisson) donne **10 cristaux de sang** : hors forme, 10 points de jauge ; en Blood Crystal Form, 10 cristaux dans le dos (le maximum) (`SanguineArt.bloodCrystals`). Seulement en posture Crimson Stance et en combat (sinon : « The blood answers only the Crimson Stance, in combat »). La fiole vide va dans le sac, pas dans la main (l'art se bat à mains nues) ; la jauge ne se vide plus tant qu'une fiole est en main (`SanguineArt.tick`, `holdsVial`).
- Apparence : la fiole d'eau de Minecraft, son liquide teinté rouge sang (`models/item/blood_vial.json`, couleur dans `BloodFluidClient.vialColour`), à la demande d'Enzo. Tests : `aBloodVialGivesTenCrystalsAndTheGaugeHoldsWhileOneIsInHand`, `aGlassBottleFillsWithBlood`.

## Art de haut niveau (15/10/2026)

Le parchemin n'enseigne qu'à partir de **Qi Crystallization** ; coûts en Qi (150, 225, 300, 180, 750 : une fois et demie ceux du premier ajustement, 15/10/2026), main durcie par le sang (+4) et parts retouchées : voir `docs/combat/technique-balance.md`. Icône de Blood Iceberg redessinée en temple (mur de briques, flèches d'or et lanternes, sang qui coule).

### Les règles du temple (15/10/2026, demande d'Enzo)

- **Incassable** : la pierre du temple ne se casse ni à la main, ni aux explosions (résistance 3 600 000), ni aux pistons ; elle ne part qu'à la fin de ses 60 s (ou en créatif). Durée confirmée par Enzo le 15/10/2026 : le temple tient 60 s, la technique se recharge en 9 min.
- **Rien ne se construit dedans** : tant qu'il tient, aucun bloc ne se pose et aucun seau ne se vide dans son emprise (45 × 45, du dessous jusqu'au-dessus des flèches) (`BloodIceberg.inArena`, `BlockEvent.EntityPlaceEvent`, `RightClickBlock` d'un seau).
- **Si son lanceur meurt, il s'effondre aussitôt** : le sang d'abord, puis la pierre du haut vers le bas en une demi-seconde (`BloodIceberg.collapse`, `collapseTick` sur `ServerTickEvent`).
- Test : `theTempleIsUnbreakableClosedToBuildingAndFallsWithItsCaster`.

## Le finish : Floraison écarlate — *Scarlet Bloom* (15/10/2026, demande d'Enzo)

D'après le film de référence d'Enzo. B sur un joueur au sol (K.O.), pour qui a l'art sanguin en art principal **et
toutes ses techniques débloquées** (`martial/ScarletBloom`, `ScarletBloom.finishes`). 7,8 s, vu par les deux joueurs
comme un film à caméra mobile (`client/FinishCinematic.placeScarlet`), et par tous ceux qui sont autour :

1. 0 s : l'exécuteur plonge la main dans le sol (`scripts/animation/scarlet-bloom.cjs`, clip `scarlet_bloom`).
2. 0,7 s : un anneau de grands pics de cristal jaillit, un pic géant sous le corps, des blocs du sol volent ; la victime
   est **emportée** à 9 blocs de haut, renversée en arrière et se débattant (clip `scarlet_bloom_flung` ; plus la pose de
   K.O. pendant ce temps, la tête ne suit plus le regard).
3. 2 s : dix cristaux quittent le dos de l'exécuteur, prennent place autour du corps et tissent une **cage** de rayons
   de cristal ; le corps y pend, écartelé.
4. 3,5 s : la cage se referme lentement en tournant, **en noir et blanc** pour les deux joueurs (post-effet
   `shaders/post/scarlet_monochrome.json`) ; le corps se recroqueville.
5. 5 s : l'exécuteur serre le poing : **exécution**. Une fleur de cristal s'ouvre, la cage éclate et **le corps éclate
   avec elle**, au ralenti : tête, torse, bras et jambes (dans la peau du joueur) s'écartent parmi des giclées de sang
   et de gros éclats ; 5,6 s : le temps repart, tout est projeté.
6. 5 à 9 s : **pluie de sang** autour du cratère ; les morceaux du corps retombent et restent dans des flaques de sang ;
   6,1 s : un cratère de grands cristaux se dresse.

Tout est fait de modèles (Enzo ne voulait pas de particules) : `ScarletPieceEntity`, rendue par
`client/ScarletPieceRenderer` (cristaux `geo/scarlet_spike|rod|shard|bloom` dans le style des cristaux de sang, sang
`geo/scarlet_blood|puddle` et sa texture, blocs du sol, membres du modèle de joueur avec sa peau ; script
`scripts/animation/scarlet-bloom-models.cjs`). Rien n'est construit ni sauvegardé ; tout disparaît 11,5 s après le début,
ou tout de suite si le finish est interrompu (le corps est alors reposé au sol). Protocole réseau 19.

Vérifications : `ScarletBloomGameTests` (réservé aux maîtres de l'art ; le corps monte, éclate en six morceaux, est
exécuté, crédit à l'exécuteur ; tout est retiré à la fin ; interrompu, le corps est reposé vivant) ; film en vrai client :
`gradlew -PgameTests runScarletBloomTour` (vue de l'exécuteur, vue d'un témoin).
