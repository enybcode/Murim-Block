# L'Art de l'Épée du Nuage Azur (青云剑法, *Qīngyún Jiànfǎ*)

> **État : art complet implémenté le 13/10/2026 par Claude (carte blanche d'Enzo), local, à essayer et régler par Enzo.** Voir « Art complet » ci-dessous ; les chiffres sont un premier jet. Les règles communes à tous les styles sont dans [../STYLES.md](../STYLES.md).
>
> Les noms en jeu sont en anglais (règle du projet). Le nom anglais est donné entre parenthèses.

## Où on en est

**Première tranche implémentée le 12/10/2026, en local :**
- **Le manuel :** `MartialManualItem`. Le pupitre de la bibliothèque du sanctuaire en donne un exemplaire par joueur (`SectLibrary`).
- **L'apprentissage :** lire le manuel apprend le style et Cloud Step Slash, qui se place dans le premier emplacement libre de la barre.
- **Les données du joueur** (`MartialData`, sauvegardées) : styles connus, techniques débloquées, barre de 10 emplacements, maîtrise, manuels reçus.
- **L'onglet Techniques :**
  - à gauche, le style et ses techniques, celles qui sont verrouillées en gris ;
  - à droite, la fiche de la technique choisie (Qi, recharge, maîtrise) et la barre de combat. Un clic sur un emplacement y place la technique, un clic droit le vide.
- **La barre de combat :**
  - en mode combat, les touches 1 à 9 de Minecraft et une 10ᵉ touche Murim (0, soit & é " ' ( - è _ ç à en AZERTY) lancent les techniques au lieu de changer d'objet ;
  - en mode combat, elle remplace la barre d’objets en bas de l’écran (10 emplacements dans le cadre de la barre de Minecraft, élargi), avec la recharge de chaque technique et l’arme tenue dans une petite case à gauche.
- **Les vérifications du serveur avant le lancement :** mode combat, technique débloquée, joueur libre d'agir, recharge, Qi, épée en main.
- **Cloud Step Slash (au sol)** est une attaque de ruée Epic Fight avec sa propre animation (`scripts/animation/azure-cloud-animations.cjs`) :
  - le joueur avance de 5 blocs ;
  - les zones de touche d'Epic Fight infligent les dégâts : ×1,4 sur l'épée, jusqu'à 4 cibles sur tout le chemin ;
  - elle coûte 12 Qi et se recharge en 6 s.
- **Commandes de test** (opérateur ; joueur visé en option à la fin) :
  - `/martial info` : arts, techniques, maîtrise, recharges et barre ;
  - `/martial learn azure_cloud_sword` : comme lire le manuel ;
  - `/martial unlock <technique|all>` et `/martial forget <technique|art|all>` (oublier l'art permet de recevoir à nouveau le manuel) ;
  - `/martial mastery set|add <technique|all> <valeur>` : de 0 à 100 ;
  - `/martial bar set <1-10> <technique>` et `/martial bar clear <1-10>` ;
  - `/martial cooldown reset`.
- **Vérifications :**
  - `MartialGameTests` : pupitre, lecture, refus de lancement, une vraie ruée qui blesse un zombie, et toutes les commandes `/martial` ;
  - en vrai client, `gradlew -PgameTests runMartialTour` : vraie touche 1, ruée de 5,00 blocs, 3 zombies sur 3 touchés, Qi, recharge connue du client.

**Art complet, 13/10/2026 (local, protocole réseau 11) :**

| Élément | Ce qui est fait | Code |
|---|---|---|
| Style principal | Le premier art appris devient le style principal ; son passif donne les M1 et la parade. `/martial main <art>`. | `MartialData.mainArt` |
| Passif Flowing Cloud Stance : M1 | Une épée (épée, épée longue, uchigatana, tachi) frappe avec les coups du style au lieu de l'enchaînement de l'arme : 4 emplacements choisis parmi 6 coups, le 4ᵉ forcément Rising Cloud (soulève) ou Falling Rain (met à terre). Par défaut : Drifting Cut, Returning Cut, Piercing Mist, Falling Rain. Wind Step est l'attaque en courant, Falling Rain l'attaque en l'air. Réglable dans l'onglet Techniques (clic : coup suivant, clic droit : précédent) ou `/martial strike <1-4> <coup>`. | mixin `EpicFightStanceMixin`, `AzureCloudSword.autoAttackMotions` |
| Breath of the Blade | Un coup M1 qui touche un ennemi (mob hostile ou joueur) rend 1 Qi (une fois par coup). | `MartialCombat` |
| Parade du passif | Parade parfaite : +6 Qi, fenêtre de riposte de 1,5 s (barre de combat encadrée d'or, « Riposte ») : la prochaine technique prend sa variante de riposte, coûte moitié moins et peut couper le recul de la garde. Le déséquilibre de 0,6 s existait déjà. | `MartialCombat.onPerfectParry` |
| Cloud Step Slash | Au sol, touche **maintenue** : garde de dégainage inspirée de Zoro (accroupi, lame rangée le long de la hanche gauche), on ne marche plus mais on tourne avec la caméra pour viser, sans coût tant qu'on tient ; touche **relâchée** (ou après 3 s) : la ruée de 5 blocs en 0,2 s dans la direction visée, la lame dégainée d'une coupe à plat de gauche à droite, arrêt accroupi dos à la cible, bras tendu. Effets (côté client, rien sur le réseau) : images rémanentes bleu pâle le long du trajet, lignes de vitesse, poussière au départ, léger élargissement du champ de vision pour le lanceur (`CloudStepEffects`). Animations faites dans Blockbench (`art/blockbench/azure_cloud_step.bbmodel`, `scripts/animation/azure-cloud-step.cjs`). Un tapotement fait la ruée tout de suite. En l'air : plongée en diagonale (4 avant, 2,5 bas) qui met à terre. Riposte : ruée courte (2,5) qui frappe deux fois. | `AzureCloudSword.perform` |
| Swallow Returns (secret) | Cloud Step Slash dans les 5 ticks d'une parade parfaite : téléportation dans le dos de l'attaquant (s'il y a la place), tournoiement qui le frappe. Révélé la 1ʳᵉ fois (« A swallow returns to its nest… »), affiché ensuite dans l'onglet, jamais sur la barre. | `AzureCloudSword.swallowReturns` |
| Thousand Drizzle | Un ennemi à 3 blocs ou moins devant (la portée des estocs) : 6 estocs (×0,35, tiennent la cible) puis un dernier qui repousse (×0,9, impact ×3). Sinon (ennemi plus loin, ou personne) : un seul estoc qui lance une onde de Qi droit devant, sans viser personne : une grande épée fantôme bleu pâle (environ 3 blocs) qui brille dans le noir, pointe en avant, avec deux images rémanentes (`QiBladeRenderer`) (×0,8 de l'attaque, s'arrête sur le premier corps, l'interrompt). Riposte : le dernier estoc brise la garde 2 s. | `QiBladeEntity` (onde) |
| Heaven-Splitting Crescent | Au sol : croissant montant ; le joueur monte de 3 blocs et **emporte la cible avec lui**, tenue devant lui à sa hauteur jusqu'au sommet (0,5 s), puis lâchée (riposte : les deux retombent lentement 1,5 s). Le client déplace le joueur et en informe le serveur avec un temps de ping de retard : le serveur recalcule aussi la hauteur du saut avec la physique de Minecraft, pour placer la cible à la hauteur du joueur sans ce retard (`MartialCombat.leap`, `carry`). En l'air (≥ 1,2 bloc du sol) : Cloud-Splitting Descent, le joueur est jeté au sol, onde de choc rayon 3 qui met à terre. Pas de dégâts de chute pendant ces sauts. | |
| Combos bornés | Chaque coup de l'art sur une même cible dans les 1,5 s du précédent perd 8 % de dégâts (plancher 40 %) ; une mise à terre termine le combo. | `MartialCombat.comboFactor` |
| Quêtes | 1. The Patient Blade : 10 parades parfaites contre des ennemis, puis 5 ennemis touchés par Cloud Step Slash → Thousand Drizzle. 2. Above the Sea of Clouds : sonner la cloche du sanctuaire, puis 3 combos (≥ 3 coups puis une mise à terre) → Heaven-Splitting Crescent. Progression dans l'onglet et à l'écran. `/martial quest <compteur> <n>`. | `MartialProgress` |
| Maîtrise | +1 par lancer qui touche un ennemi (une fois par lancer) ; passif : +1 tous les 8 coups M1 qui touchent. Style = moyenne passif + 3 techniques. | `MartialProgress` |
| Évolutions | Style 30 : Thousand-Li Cloud Step (2ᵉ ruée gratuite dans les 1,5 s). Style 60 : Storm of Needles (rafale plus large, aiguille de Qi traversante à 8 blocs). Style 90 : Azure Heaven Crescent (lame de Qi verticale traversante, 10 blocs). Annoncées en jeu, noms changés dans l'onglet. | |
| Finish | Last Rain Over the Clouds (B sur un K.O., style principal + épée) : épée vers le ciel, nuage de Qi et pluie lumineuse au-dessus de la cible, saut et taille verticale à 1,5 s, la pluie s'arrête net. Film de 2,6 s pour les deux joueurs. Sinon finish générique. | `AzureCloudSword.finishTick`, `KnockdownService.execute` |
| Icônes | Thousand Drizzle, Heaven-Splitting Crescent, Swallow Returns, le passif. | `scripts/worldgen/TechniqueIcons.java` |

Toutes les touches viennent de boîtes posées sur la racine du squelette (`MurimAnimations`), pas de la lame : un coup touche là où il est dessiné, quelle que soit l'épée. Epic Fight tourne ces boîtes avec la rotation de la racine dans l'animation : le script d'animation garde donc la racine face à l'avant et met la rotation du corps sur le torse (sauf Swallow Returns, qui tournoie exprès). Les boîtes sont vérifiées par `AzureHitboxGameTests` (47 positions : où chaque coup doit toucher et ne pas toucher), joué près de l'origine du monde : à 10 millions de blocs, où le serveur de test place ses arènes, les calculs en `float` d'Epic Fight n'ont plus qu'un bloc de précision. Les animations sont générées par `scripts/animation/azure-cloud-animations.cjs` (Node 20+) ; elles sont un premier jet, à retoucher.

**Vérifications du 13/10 :** `MartialDataTest` (8 tests unitaires : règle des M1, secret, oubli complet, anciennes sauvegardes, quêtes, maîtrise, combos) ; `AzureCloudSwordGameTests` (13 GameTests serveur : chaîne M1, Qi, riposte à moitié prix, Swallow Returns, garde brisée, 7 touches de Thousand Drizzle, onde à 4,4 blocs, croissant qui soulève, variantes en l'air, combo dégressif et quête, quêtes par le jeu, évolution et 2ᵉ ruée, finish) ; en vrai client, `runMartialTour` étendu (vrai clic M1, touches 2 et 3, onde, montée de 4,9 blocs, captures de l'onglet).


**Parade (14/10) :** toutes les attaques de l'art se parent, joueur contre joueur, face à face (`AzureParryGameTests`, 18 coups × sans garde / parade parfaite / parade normale, tableau « Azure parry » dans le journal). Parade parfaite : 0 dégât sur chaque coup, lames de Qi comprises ; sur Thousand Drizzle et la riposte du Cloud Step, le déséquilibre de l'attaquant coupe aussi les coups suivants. Parade normale : la moitié des dégâts. Un coup pris sur la garde ne fait que ses dégâts : ni soulèvement (Rising Cloud), ni emport (croissant), ni garde brisée, ni étourdissement de l'onde ou du croissant de Qi ; il reste le petit recul de la garde. Swallow Returns frappe de dos : la garde, qui ne couvre que l'avant, ne l'arrête pas (voulu). Corrigé au passage : Rising Cloud ne soulevait pas un joueur (l'étourdissement du coup, appliqué après notre crochet, annulait l'élan) ; l'élan part maintenant au tick suivant.
**Non fait :** l'évasion (sortir d'un combo une fois, commune à tous les styles), le maître de la secte, la description des styles en datapack, les sons et effets Photon propres, la validation à deux vrais clients. Le K.O. reste réservé au PvP.

## L'idée

C'est l'art fondateur de la secte de la Falaise des Nuages, celle du sanctuaire au sommet de l'aiguille. On le reconnaît à trois choses :

- **Une épée fluide, rapide et précise**, qui gagne en se plaçant mieux que l'adversaire, pas en frappant plus fort.
- **Un style de contre** : sa parade parfaite est la plus récompensée, et ses meilleurs combos partent d'elle.
- **Le jeu aérien.** Il soulève l'adversaire, le poursuit dans les airs et le rabat au sol, comme un nuage qui monte puis retombe en pluie.

C'est le style de départ idéal : il apprend à parer, à enchaîner, et à finir proprement un combo.

## Le livre

- **« Azure Cloud Sword Manual »** (Manuel de l'Épée du Nuage Azur), un livre relié de jade.
- **Où le trouver :** sur le pupitre de la bibliothèque du Sanctuaire de la Falaise des Nuages. Le Jeton de la Secte mène donc au premier art martial, et le sanctuaire devient vraiment « le début de chaque aventure ». Plus tard, le maître de la secte pourra le remettre en personne.
- **En l'utilisant**, le style apparaît dans l'onglet Techniques, avec son passif et sa première compétence. Le livre n'est pas consommé : il peut se prêter à un autre joueur.

## Le passif : « Flowing Cloud Stance » (Posture du Nuage qui s'écoule)

Un seul passif est actif à la fois : celui du style principal.

### Les M1

L'enchaînement est rapide, en 4 coups. Le style autorise 6 coups, et le joueur en place 4 dans ses emplacements M1 :

| Coup | Effet | Remarque |
|---|---|---|
| **Drifting Cut** (Coupe flottante) | Taille horizontale rapide | Ouverture |
| **Returning Cut** (Coupe de retour) | Revers | Enchaînement |
| **Piercing Mist** (Brume perçante) | Estoc, un peu plus de portée | Utile à mi-distance |
| **Rising Cloud** (Nuage montant) | Taille montante qui soulève légèrement la cible | Lanceur vers le jeu aérien |
| **Falling Rain** (Pluie tombante) | Taille plongeante qui met à terre | Finisseur : termine le combo |
| **Wind Step** (Pas du vent) | Petit pas en avant avec une coupe | Rattrape une cible qui recule |

- **Règle des emplacements :** le 4ᵉ emplacement doit être un lanceur (Rising Cloud) ou un finisseur (Falling Rain). Un enchaînement M1 se termine donc toujours sur l'un ou l'autre.
- **En l'air**, le 4ᵉ coup devient toujours Falling Rain, qui rabat la cible au sol. Un combo aérien ne peut donc pas durer.

### La parade

| Parade | Effet avec ce passif |
|---|---|
| **Parfaite** (dans les 0,5 s avant le coup) | Le coup est annulé. L'attaquant est **déséquilibré** pendant 0,6 seconde : il ne peut ni attaquer ni parer. Pendant 1,5 seconde, la prochaine compétence lancée prend sa **variante de riposte** (voir plus bas) et coûte moitié moins de Qi. Le joueur récupère 6 Qi. |
| **Normale** (garde tenue) | Les dégâts sont réduits de 50 %, avec un léger recul. Pas de bonus. |

### Le bonus : « Breath of the Blade » (Souffle de la lame)

Chaque M1 qui **touche** un ennemi rend 1 Qi, et une parade parfaite en rend 6. Frapper dans le vide ne rend rien.

C'est ce qui fait tourner l'économie du Qi en combat. Aujourd'hui, le Qi remonte d'environ 1 % par minute, ce qui est bien trop lent pour payer des compétences en plein combat (voir les questions ouvertes).

## Les compétences actives

| Compétence | Débloquée par | Qi | Recharge |
|---|---|---|---|
| 1. **Cloud Step Slash** (Taille du Pas de Nuage) | Le livre | 12 | 6 s |
| 2. **Thousand Drizzle** (Bruine des Mille Gouttes) | Quête 1 | 18 | 9 s |
| 3. **Heaven-Splitting Crescent** (Croissant qui fend le ciel) | Quête 2 | 25 | 14 s |

Les valeurs sont un premier jet, à régler en jeu.

### 1. Cloud Step Slash

Une ruée de 5 blocs en avant qui tranche tout sur son passage. C'est l'outil pour se replacer : engager, poursuivre ou s'échapper.

- **Au sol :** ruée horizontale. Elle traverse la cible, ce qui vous replace derrière elle.
- **En l'air :** ruée **plongeante** en diagonale vers le sol. Elle met à terre la cible touchée et termine le combo.
- **Riposte** (après une parade parfaite) : ruée courte qui frappe deux fois au lieu d'une.

### 2. Thousand Drizzle

Une rafale d'estocs très rapides devant soi (6 touches), puis un dernier estoc qui repousse. Elle tient la cible en place pendant la rafale, ce qui en fait le cœur des combos.

- **Cible proche :** la rafale complète.
- **Cible à mi-distance** (4 à 7 blocs) : la rafale part en **onde d'épée**, un projectile court qui ne touche qu'une fois, sans repousser. Elle sert à interrompre un adversaire qui charge.
- **Riposte :** sur une cible déséquilibrée, le dernier estoc **brise la garde**. La cible ne peut plus parer pendant 2 secondes.

### 3. Heaven-Splitting Crescent

Une taille en croissant, de bas en haut, qui **soulève la cible et le joueur** dans les airs. C'est l'ouverture du jeu aérien.

- **Au sol :** le lanceur. La cible et le joueur montent d'environ 4 blocs.
- **En l'air :** **« Cloud-Splitting Descent »** (Descente qui fend les nuages). Le joueur s'abat au sol et crée une onde de choc en cercle (rayon 3) qui met à terre. C'est le grand finisseur.
- **Riposte :** le croissant soulève plus haut et laisse plus de temps pour enchaîner en l'air.

## Le finish : « Last Rain Over the Clouds » (Dernière pluie sur les nuages)

C'est l'exécution signature de l'art, jouée avec B sur un joueur K.O. (voir [la mise à terre](../STYLES.md#mise-à-terre-et-finish-pvp)) :

1. Le joueur pointe son épée vers le ciel.
2. Un nuage de Qi bleu pâle se forme au-dessus de la cible, et une fine pluie lumineuse commence à tomber.
3. Le joueur saute et retombe en une seule taille verticale, qui fend le nuage en deux.
4. La pluie s'arrête d'un coup.

Il dure environ 2 secondes, pendant lesquelles un tiers peut l'interrompre en frappant le joueur qui l'exécute.

## Les enchaînements

Ce sont des combos vrais mais bornés. Chacun se termine par une mise à terre, avec un relevé invulnérable. Les dégâts baissent de 8 % par coup dans un même combo, sans descendre sous 40 %. La cible peut s'échapper une fois avec l'évasion.

- **Combo de base (sol → air) :**
  1. M1 ×3 ;
  2. **Heaven-Splitting Crescent** (on monte) ;
  3. M1 ×2 en l'air ;
  4. **Cloud-Splitting Descent**, qui met à terre.
- **Combo de contre :**
  1. parade parfaite ;
  2. **Thousand Drizzle** en riposte (garde brisée) ;
  3. M1 ×3 ;
  4. **Cloud Step Slash**, qui traverse et vous replace derrière ;
  5. Falling Rain, qui met à terre.
- **Poursuite :**
  1. Thousand Drizzle à mi-distance (l'onde arrête une charge) ;
  2. **Cloud Step Slash** pour rattraper ;
  3. M1 ×4 qui finit sur Falling Rain.

## Le mouvement secret : « Swallow Returns » (Retour de l'Hirondelle)

Il n'apparaît pas dans l'onglet Techniques tant qu'on ne l'a pas découvert.

- **Comment le déclencher :** lancer Cloud Step Slash **dans les 5 ticks** (un quart de seconde) qui suivent une parade parfaite.
- **Ce qu'il fait :** au lieu de la ruée, le joueur **réapparaît instantanément dans le dos** de l'attaquant et frappe un coup qui le retourne. Il ouvre n'importe quel combo.
- **La première fois** qu'on le réussit, un message (« A swallow returns to its nest… ») révèle la technique et l'ajoute à l'onglet.

## Débloquer les compétences

| Étape | Ce qu'il faut faire | Récompense |
|---|---|---|
| Livre | Utiliser le manuel | Le style, le passif, Cloud Step Slash |
| Quête 1 : « The Patient Blade » (La lame patiente) | Réussir **10 parades parfaites** contre des ennemis, puis toucher **5 ennemis** avec Cloud Step Slash | Thousand Drizzle |
| Quête 2 : « Above the Sea of Clouds » (Au-dessus de la mer de nuages) | Monter au sommet du Sanctuaire et **sonner la cloche** de la tour, puis placer **3 combos complets** finis par une mise à terre | Heaven-Splitting Crescent |

L'onglet Techniques affiche la quête en cours et sa progression, par exemple « Perfect parries 6/10 ».

## Maîtrise et évolutions

- **La maîtrise de chaque technique** monte de 0 à 100. Elle progresse quand la technique touche un ennemi vivant : mob hostile ou joueur. Les animaux et l'air ne comptent pas.
- **La maîtrise du style** est la moyenne de ses techniques, M1 compris.

Quand le style atteint une maîtrise donnée, chaque technique gagne son évolution, qui remplace la version de base :

| Maîtrise du style | Évolution |
|---|---|
| 30 | **Cloud Step Slash → « Thousand-Li Cloud Step »** (Pas de nuage de mille lieues) : on peut relancer une seconde ruée dans la seconde qui suit. |
| 60 | **Thousand Drizzle → « Storm of Needles »** (Tempête d'aiguilles) : la rafale est plus large, et le dernier estoc lance une aiguille de Qi qui traverse jusqu'à 8 blocs. |
| 90 | **Heaven-Splitting Crescent → « Azure Heaven Crescent »** (Croissant du Ciel Azur) : le croissant projette une lame d'énergie verticale en plus du lanceur. |

## Contre-jeu face à ce style

- **Ne pas attaquer n'importe quand :** contre l'Épée du Nuage Azur, une attaque prévisible se fait parer parfaitement. Les feintes et les attaques à retardement la battent.
- **Cloud Step Slash et Thousand Drizzle ont une recharge assez longue.** Une fois qu'ils sont utilisés, on a quelques secondes pour attaquer.
- **Le jeu aérien commence par Heaven-Splitting Crescent**, qui se voit venir : une roulade ou une parade l'évite.
- **L'évasion** permet de sortir d'un combo une fois.

## Les animations

- **Au début :** des animations d'épée d'Epic Fight, pour valider le jeu.
- **Ensuite :** des animations Murim faites dans Blockbench (dans le pipeline existant, `docs/ANIMATION_PIPELINE.md`), avec des effets Photon (traînées de nuage bleu pâle, onde au sol) et des sons.

## Questions ouvertes pour Enzo

1. **L'économie du Qi.** Le Qi remonte aujourd'hui d'environ 1 % par minute, et de 5 % en méditant. Avec des compétences à 12-25 Qi, il faut une source en combat. Je propose « Breath of the Blade » (les coups qui touchent rendent du Qi), plus peut-être une régénération plus rapide hors combat. D'accord ?
2. **La fenêtre de la parade parfaite.** Elle fait 0,5 s pour tous les styles. Un passif peut-il la modifier ?
3. ~~Le livre au sanctuaire~~ : **un exemplaire par joueur** (décision d'Enzo, 12/10/2026).
4. ~~Le mouvement secret~~ : **révélé dans l'onglet une fois découvert** (lecture de la réponse d'Enzo, à confirmer).
