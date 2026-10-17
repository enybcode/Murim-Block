# Styles de combat (conception)

> **État : conception et premier art complet en local.** La garde Murim, le K.O./portage/finish générique et l'Art de l'Épée du Nuage Azur entier (passif et M1 configurables, riposte, variantes, secret, quêtes, maîtrise, évolutions, finish propre, combos dégressifs) sont implémentés ([la fiche de l'art](styles/azure-cloud-sword.md)). Restent l'évasion, les styles en datapack et le mélange de plusieurs styles. Voir [la publication du 13 octobre](../DELIVERY_2026_10_13.md) pour les contrôles réellement exécutés. Le clash physique entre armes est pour plus tard.

## L'idée

Retrouver la profondeur de Jujutsu Shenanigans sans copier ses attaques. L'identité ne vient pas d'un personnage, mais des **arts martiaux** :

- chaque style a ses coups ;
- il a ses variantes et ses enchaînements ;
- on les découvre en le maîtrisant.

La même arme se joue différemment selon le style.

**Epic Fight reste le moteur** : animations, touches, dégâts, parade, roulade, mise à terre. Murimblock décide quelle animation jouer, quand, et à quelles conditions. Il n'y a pas de seconde boucle de dégâts, et l'ancienne refonte du combat (supprimée au commit `cefb618`) ne revient pas.

## Ce qu'apporte un style

| Élément | Contenu |
|---|---|
| **Passif** | Change la façon de se battre : l'enchaînement M1 (animations, rythme, portée) **et la parade** (voir plus bas). Il peut ajouter un second bonus, par exemple « frappe plus fort », ou plus de dégâts après une parade. **Un seul passif est actif à la fois.** |
| **M1** | Un enchaînement de coups de base. Les 4 emplacements M1 configurables se choisissent parmi les coups autorisés par le style. |
| **Compétences actives** | Au moins 3. Chacune a un coût en Qi, une recharge, ses variantes contextuelles (au sol, en l'air, après une parade, selon la distance ou l'état de la cible) et sa maîtrise. |
| **Enchaînements** | Des routes M1 + compétences qu'un bon joueur peut placer. Certaines compétences ont une variante quand elles suivent un coup précis. |
| **Mouvements secrets** | Déclenchés par une condition ou un timing (« A puis B dans les 10 ticks »), pas par une touche de plus. Après le prototype. |
| **Finish** | **Chaque art martial a son finish** : l'exécution signature du style, jouée sur un joueur à terre (voir « Mise à terre et finish »). C'est le finish du style principal qui est joué. |

## Obtenir et progresser

1. **Le livre.** On trouve le livre du style. En l'utilisant une fois, le style apparaît dans l'onglet Techniques, avec sa première compétence débloquée.
2. **Les quêtes de déblocage.** Chaque compétence suivante se débloque par une quête : une suite d'actions, par exemple « réussir 10 parades », puis « placer la compétence 1 en l'air sur un ennemi ». Une fois une quête finie, la suivante s'ouvre. On ne débloque jamais tout d'un coup.
3. **La maîtrise par technique.** Utiliser une technique pour de vrai (toucher un ennemi, pas frapper l'air) fait monter sa maîtrise. La maîtrise du style dépend de celle de ses techniques.
4. **Les évolutions.** Une technique évolue en une version améliorée (nouvelle variante, effet en plus) quand le style atteint un certain niveau de maîtrise.

Les styles, quêtes, paliers de maîtrise et évolutions sont décrits en **données** (datapack). Ajouter un style ne demande donc pas de nouveau code.

## La barre de combat (comme Blox Fruits)

- **Hors combat**, les touches 1 à 0 (`& é " ' ( - è _ ç à`) changent d'objet, comme d'habitude.
- **En mode combat** (touche V), ces mêmes touches lancent les compétences placées dans la barre de combat. On change d'arme à la molette, ou en quittant le mode combat.
- La barre de combat s'organise dans l'onglet Techniques, en glissant les compétences débloquées dans les 10 emplacements.

## Mélanger les styles

- **Le style principal** donne les M1, la parade et le passif. **Un seul passif est actif à la fois** : changer de style principal change de passif.
- **La barre de combat** accepte les compétences actives de **tous les styles possédés**, tant que l'arme tenue le permet (une technique d'épée demande une épée).

On construit ainsi sa propre façon de se battre.

## Combos et contre-jeu

Les combos sont **vrais mais bornés** : un bon joueur enchaîne sans que l'autre puisse agir, mais rien n'est infini.

- **Chaque combo se termine.** Une mise à terre (ou un nombre maximal de coups) libère la cible, qui se relève brièvement invulnérable (relevé d'Epic Fight).
- **Les dégâts diminuent** au fil d'un même combo.
- **Une évasion**, payée en Qi et à longue recharge, permet de sortir d'un combo une fois.
- **La parade et l'esquive** restent les défenses de base, hors combo.

### Deux sortes de parade

> **Implémenté le 12/10/2026, en local** : la garde Murim (`MurimGuardSkill`) remplace la garde de base d'Epic Fight dans l'emplacement de garde de tous les joueurs. Elle se tient sur la touche de garde (clic droit), une arme en main, en mode combat. Elle pare les coups **et les flèches** venant de face, mais ni les explosions, ni le feu, ni la magie. Epic Fight seul ne suffisait pas : sa garde de base bloque tout, sans parade parfaite et sans les flèches, et sa compétence « Parrying » n'arrête les flèches qu'en parade réussie.

| Parade | Quand | Effet |
|---|---|---|
| **Parade parfaite** | La parade est lancée dans les **0,5 seconde (10 ticks) avant** que le coup touche. | Le coup est entièrement annulé (une flèche rebondit). Un attaquant au corps à corps est **déséquilibré** (animation de déséquilibre d'Epic Fight). Plus tard, l'effet de réussite du passif s'y ajoutera. |
| **Parade normale** | La garde est levée plus tôt et tenue jusqu'au coup. | **La moitié des dégâts** passe, sans faire tressaillir le joueur. La parade ne déclenche rien. |

**Toute attaque a sa fenêtre de parade parfaite** (demande d'Enzo) : coups, flèches et autres projectiles, la boule d'eau du Crab Man, et les compétences, même celles faites pour percer une garde. La parade normale reste plus étroite : une attaque qui perce la garde, la magie ou le feu la traversent entièrement. Rien ne pare ce qui n'est pas une attaque (chute, noyade, brûlure), ce que rien n'arrête (le vide, `/kill`), une explosion, ni le finish. Une boule d'eau parée parfaitement éclate sur la lame sans rien faire ; en parade normale, elle fait la moitié de ses dégâts, sans lenteur, avec la moitié de sa poussée.

Une seule parade parfaite par levée de garde : relâcher et relever la garde très vite ne la redonne pas (garde-fou repris d'Epic Fight). Vérifié par `ParryGameTests` (5 tests) et en vrai client par `gradlew -PgameTests runParryTour` (zombie et vraies flèches).

Le serveur compare le moment où la parade a été lancée au moment où le coup arrive. Le client ne décide pas si sa parade est parfaite.

### La parade dépend du passif

On peut toujours parer, mais la parade change selon le passif actif :
- ce que la parade normale encaisse (part des dégâts réduite, recul) ;
- ce que la parade parfaite déclenche : une riposte, un déséquilibre de l'attaquant, ou un bonus pour la compétence suivante ;
- son animation.

La fenêtre de la parade parfaite est de 0,5 seconde pour tous les styles. Reste à décider avec Enzo si un passif peut l'allonger ou la raccourcir.

Par exemple, un style d'épée défensif aurait une parade parfaite très récompensée. Un style lourd aurait une parade normale qui encaisse plus, mais une parade parfaite sans riposte.

Côté Epic Fight, la parade est sa compétence de garde. Le mod impose aujourd'hui la garde de base (`EpicFightCombatDefaults`). Le passif choisirait la compétence de garde à placer : une garde Murim propre au style, ou une variante d'Epic Fight. Les réglages sont **à vérifier** dans l'API d'Epic Fight.

Côté coûts : les compétences coûtent du Qi et ont une recharge. Les M1, la parade et la roulade restent gratuits.

## Mise à terre et finish (PvP)

> **Implémenté le 12/10/2026, en local** (inspiré de Deepwoken). Le finish joué est pour l'instant un finish générique ; chaque art martial aura le sien.

En PvP, **on ne tue pas directement un joueur** :

1. **La mise à terre.** Quand ses points de vie tombent à zéro sous les coups d'un joueur (ou de ses projectiles), il ne meurt pas : il tombe **K.O.** avec 1 point de vie.
   - Il est allongé (posture « ramper » de Minecraft, donc une petite hitbox) et ne fait que **ramper très lentement**, sans pouvoir sauter.
   - Il ne peut ni attaquer, ni parer, ni esquiver, ni utiliser d'objet ou de compétence, ni charger son Qi.
   - Il ne prend plus de dégâts, sauf le finish et ce que rien n'arrête (le vide, `/kill`).
2. **Le choix.** Un joueur à moins de 3 blocs qui regarde le K.O. voit « [V] Carry   [B] Execute » :
   - **V : le porter.** Le K.O. est mis sur l'épaule du porteur. Ses hanches reposent sur l'épaule droite, le buste pend dans le dos et les jambes devant, tenues par le bras droit. Le porteur avance 25 % moins vite et ne peut pas se battre. **V à nouveau : le poser** devant soi. Le porté ne peut pas descendre tout seul.
   - **B : l'exécuter.** Le finish se joue : l'arme est levée à deux mains, puis enfoncée vers le sol. Le coup tombe à 0,9 seconde et le joueur meurt vraiment, sur place, allongé (« X was executed by Y »). **C'est une mort normale** : l'inventaire tombe au sol, la mort coûte du Qi (voir ci-dessous) et le tueur reçoit sa récompense de Qi habituelle.
3. **Sinon, il se relève** au bout de **30 secondes**, avec 25 % de sa vie. Le compte s'arrête pendant qu'on le porte ou qu'on l'exécute. **Personne ne peut le relever plus tôt** (décision d'Enzo).

**Le prix d'une mort.** Toute vraie mort fait perdre **pour de bon 10 % du Qi maximum**, sans jamais descendre sous la réserve de départ (100). Une exécution, une mort face à un mob ou une chute comptent toutes. Un K.O. n'est pas une mort et ne coûte rien. Le joueur voit « Death costs you … maximum Qi for good ».

**Contre-jeu.** Un coup sur celui qui exécute, avant que la lame tombe, interrompt le finish. Si le porteur tombe K.O. à son tour, meurt ou se déconnecte, il lâche celui qu'il porte.

**Le serveur décide de tout :**
- qui est K.O. ;
- qui peut agir sur lui : la distance, la ligne de vue, un seul joueur à la fois ;
- le début et la fin du portage et du finish.

Le client ne fait qu'envoyer « je veux porter / poser / exécuter », puis il joue les animations. Tous les joueurs voient la même chose, car l'état K.O. est synchronisé avec chacun. Un joueur qui se déconnecte K.O. le reste à son retour.

**Les touches.** V et B ne prennent ce sens qu'en regardant un joueur K.O. de près, ou en portant quelqu'un. Le reste du temps, V garde son rôle (mode combat). B est réglable dans les contrôles (« Execute »).

**Les animations**, toutes originales, sont générées par `scripts/animation/knockdown-animations.cjs` pour le squelette d'Epic Fight :

| Animation | Joueur | Quand |
|---|---|---|
| `downed_idle` | K.O. | Allongé, appuyé sur les avant-bras, tête relevée |
| `downed_crawl` | K.O. | Rampe en tirant avec un bras puis l'autre, un genou ramené à chaque traction |
| `carry_idle` / `carry_walk` | Porteur | Debout, ou en marchant, le bras droit autour des jambes du porté |
| `carried` | Porté | Plié sur l'épaule du porteur, bras ballants |
| `finish_generic` | Exécuteur | Le finish générique |

Un joueur K.O. qui meurt reste allongé : ni la réaction au coup, ni l'animation de mort d'Epic Fight ne le relèvent.

**Le finish en cinématique.** Les deux joueurs du finish, celui qui exécute et celui qui est exécuté, le voient comme un court film (inspiré d'une vidéo d'exemple d'Enzo). Les autres joueurs le voient normalement. La caméra quitte la première personne pour 4 plans coupés, filmés d'un côté pour l'exécuteur et de l'autre pour la victime :
1. un plan large de côté pendant que l'arme se lève ;
2. une contre-plongée depuis la tête de la victime, vers la lame levée ;
3. une plongée sur l'impact, avec une secousse ;
4. un lent mouvement tournant autour de l'exécuteur.

Pendant les 2 secondes du film, l'interface et les noms sont masqués et des bandes noires de cinéma s'affichent. La victime ne peut plus bouger. Son écran de mort attend la fin du film. Si le finish est interrompu, le film s'arrête net. Côté code : `client/FinishCinematic` (une caméra posée sur une entité invisible) et le message `FinishCinematicPayload`, envoyé par le serveur aux deux joueurs seulement (protocole réseau 9).

**Vérification :**
- **Tests de jeu** (`KnockdownGameTests`, 8 tests) :
  - un coup mortel d'un joueur met K.O., alors qu'un mob tue normalement ;
  - un K.O. est épargné, ne peut pas se battre ni charger son Qi, mais le vide le tue ;
  - il se relève à la fin du temps ;
  - le portage, le blocage de la descente, la pause du compte et la pose ;
  - la portée des actions ;
  - le finish tue, crédite l'exécuteur, fait tomber l'inventaire et coûte 10 % du Qi maximum ;
  - une mort face à un mob coûte aussi 10 %, mais un K.O. ne coûte rien ;
  - un coup sur l'exécuteur interrompt le finish.
- **En vrai client** (`gradlew -PgameTests runKnockdownTour`) : un monde plat avec deux joueurs factices, vus par le client comme de vrais joueurs. La visite vérifie 16 points et prend des photos et des séquences d'images (`build/knockdown-tour-run/screenshots`) :
  - le K.O. par un vrai coup et sa synchronisation ;
  - pour chaque état, l'animation Murim effectivement jouée par Epic Fight, côté porteur local comme côté autre joueur ;
  - l'indication à l'écran ;
  - le portage, la marche, la pose ;
  - le finish, et le film vu par l'exécuteur et par la victime (caméra hors du joueur, écran de mort après le film).
- **Pour faire des GIF :** `java scripts/animation/FramesToGif.java <dossier d'images> <sortie.gif>`.
- **Reste à faire à la main :** deux vrais clients sur un serveur dédié, avec de la latence réelle.

**Décisions d'Enzo (12/10/2026)** :
- on ne peut pas relever un joueur K.O. ;
- l'exécution compte comme une mort normale : l'inventaire tombe et le Qi maximum baisse de 10 % pour de bon.

**Questions encore ouvertes pour Enzo :**
- La mise à terre vaut-elle aussi contre les mobs ? (Pour l'instant : seulement entre joueurs.)
- La durée du K.O. (30 s) et la vie au relevé (25 %) conviennent-elles ?

## Serveur et réseau

- Le client envoie seulement « je lance l'emplacement N ».
- Le serveur vérifie :
  - le mode combat ;
  - la compétence débloquée et l'arme compatible ;
  - la recharge et le Qi ;
  - l'état du joueur (pas déjà occupé, pas en train de charger le Qi) ;
  - le contexte de la variante.
- Puis il lance l'animation Epic Fight, que tous les joueurs voient.
- Les données du joueur (styles possédés, quêtes, maîtrises, barre) sont sauvegardées et synchronisées avec lui.

## Prototype proposé (première tranche)

Le premier style est détaillé dans [styles/azure-cloud-sword.md](styles/azure-cloud-sword.md). Le premier art du poing, à mains nues : [styles/long-fist.md](styles/long-fist.md). Le premier art du bâton, à deux mains : [styles/traveller-staff.md](styles/traveller-staff.md). Un art intermédiaire à la grande arme à deux mains, proposé le 17/10/2026 : [styles/purple-heaven-thunder-saber.md](styles/purple-heaven-thunder-saber.md).

**Un seul style, à l'épée**, par exemple « L'Art de l'Épée, volume I » :

- un livre qui l'ajoute à l'onglet Techniques ;
- un passif qui change l'enchaînement M1 de l'épée et sa parade ;
- 3 compétences actives, la 1ʳᵉ débloquée par le livre, les 2ᵉ et 3ᵉ par deux petites quêtes ;
- 2 variantes contextuelles, par exemple une riposte après une parade réussie et une version plongeante en l'air (exemples à valider) ;
- la barre de combat sur 1 à 0 en mode combat, avec coût en Qi et recharge ;
- la maîtrise par technique, affichée dans l'onglet Techniques ;
- les combos bornés : fin par mise à terre, dégâts dégressifs, évasion en Qi.

**Après le prototype** :
- les évolutions ;
- les mouvements secrets ;
- un deuxième style, pour tester le mélange.

### Ordre de travail

1. **Fondations** : définitions de style en données, données du joueur sauvegardées et synchronisées, livre, onglet Techniques.
2. **Barre et lancement** : touches en mode combat, message au serveur, vérifications, lecture de l'animation Epic Fight, recharge et Qi, affichage.
3. **Passif et M1** : faire changer l'enchaînement M1 de l'arme par le style. Epic Fight permet, d'après son API, de choisir le style d'une arme selon le joueur ; c'est **à vérifier** avant de s'engager.
4. **Quêtes et maîtrise.**
5. **Combos bornés et évasion.**
6. **Tests** : tests de jeu côté serveur, puis validation à deux clients.

## À décider plus tard

- Le nom, le thème et les compétences exactes du premier style, et ses animations (Epic Fight de base au début, puis animations Murim faites dans Blockbench).
- Où trouver les livres : coffres, mobs, marchands ou sectes.
- Le contenu exact des quêtes et les paliers de maîtrise.
