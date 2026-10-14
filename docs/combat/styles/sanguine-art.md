# Sanguine Art — Blood Crystal

Branche personnelle **Colleuhu95**. Révision du 14 octobre 2026 demandée par Colleuhu95 : remplacer Bloodbane Drain par un cristal chargé puis lancé, utiliser Scarlet Tear et les M1 pour préparer la forme finale fournie dans `cristal-de-sang-kit.zip`, et consommer les cristaux dorsaux sans quitter la forme.

## Fonctionnement validé

Sanguine Art doit être le style principal. Les deux mains sont vides. Le joueur conserve son skin et son équipement : la forme est une surcouche transparente animée par le squelette Epic Fight, pas un objet d'armure.

La jauge est plafonnée à 10. Un M1 qui touche un ennemi donne 1 point ; Scarlet Tear donne 3 points par utilisation réussie, même si plusieurs cibles sont touchées. Air, parade, garde, allié et animal ne donnent aucun point. La jauge pleine attend une activation manuelle, sans transformation automatique.

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
