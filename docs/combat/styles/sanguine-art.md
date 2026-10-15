# Sanguine Art — Blood Crystal

Branche personnelle **Colleuhu95**. Révision du 14 octobre 2026 demandée par Colleuhu95 : remplacer Bloodbane Drain par un cristal chargé puis lancé, utiliser Scarlet Tear et les M1 pour préparer la forme finale fournie dans `cristal-de-sang-kit.zip`, et consommer les cristaux dorsaux sans quitter la forme.

## Fonctionnement validé

Sanguine Art doit être le style principal. Les deux mains sont vides. Le joueur conserve son skin et son équipement : la forme est une surcouche transparente animée par le squelette Epic Fight, pas un objet d'armure.

La jauge est plafonnée à 20 (15/10/2026, demande d'Enzo ; 10 avant). Un M1 qui touche un ennemi donne 1 point ; Scarlet Tear donne 3 points par utilisation réussie, même si plusieurs cibles sont touchées. Air, parade, garde, allié et animal ne donnent aucun point. La jauge pleine attend une activation manuelle, sans transformation automatique.

| Case initiale | Technique | Qi | Recharge | Action |
|---|---|---:|---:|---|
| 1 | Blood Crystal | 18 | 160 ticks | Maintenir pour charger un cristal à la main droite, relâcher pour lancer. Charge minimum 12 ticks, maximum 30 ticks. Lancement 6 ticks après le geste ; portée 24 blocs, vitesse 0,9 bloc/tick, dégâts ×1,2 de l'attaque. Aucun soin. |
| 2 | Scarlet Tear | 24 | 200 ticks | Arc frontal Epic Fight, dégâts ×1,5, stun long. Ajoute 3 points à la jauge sur un contact valide. |
| 3 | Blood Crystal Form | 20 | 80 ticks | Jauge pleine requise. Consomme la jauge et active la surcouche finale avec **15 % de réduction des dégâts reçus**, fixe et globale. |
| 4 | Crystal Barrage | 18 | 120 ticks | Lance tous les cristaux gagnés aux M1, un tous les 2 ticks après un geste de 6 ticks. Chaque projectile inflige ×0,45 de l'attaque : davantage de cristaux donne davantage de dégâts cumulés. |

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
- **2e activation** (gratuite, malgré la recharge, comme le plongeon du croissant) : le **lancer de javelot** (`sanguine_sword_throw`, 0,95 s, l'épée part pointe la première à 0,35 s) ; elle explose à l'impact. Un lancer qui touche donne +3 à la jauge (hors forme) et compte pour la quête Scarlet Release.
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
