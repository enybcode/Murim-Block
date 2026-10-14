# Le Poing Long (长拳, *Chángquán*)

> **État : implémenté sur `test` le 14/10/2026, en attente de validation en jeu par Enzo** (voir « Ce qui est en jeu » à la fin). Où le trouver (décision d'Enzo du 14/10, l'art le plus répandu) : le **Bandit Chief** le lâche 3 fois sur 10 (tué par un joueur, plus avec Looting), et un **tonneau déjà présent** dans chaque site en garde une réserve (le butin d'un coffre de forge de village, et le rouleau 4 fois sur 10) : la baie de la cloche du temple de la Mer de Bambous, le kang du chef dans le repaire des bandits, la baie de la cloche du poste de signaux, la salle commune du hameau des pêchers, un type de maison du village sur pilotis. Aucun bloc ajouté ni déplacé ; le Jardin de la Lune de Lotus n'a pas de tonneau, donc pas de réserve (`SiteLoot`, `loot_table/chests/murim_cache.json`). Le premier art martial du poing : un art de débutant, simple et lisible. **Décision d'Enzo : les M1 restent ceux d'Epic Fight à mains nues ; les compétences, le mouvement secret et le finish ont leurs propres animations.** Les chiffres sont un premier jet. Les règles communes à tous les styles sont dans [../STYLES.md](../STYLES.md), et le premier art, à l'épée, dans [azure-cloud-sword.md](azure-cloud-sword.md).
>
> Les noms en jeu sont en anglais (règle du projet). Le nom anglais est donné en premier, la traduction entre parenthèses.

## L'idée

Le Poing Long est la boxe qu'on apprend en premier dans les écoles chinoises : des coups de poing droits, de grands coups de pied, des postures larges et des déplacements rapides. Des bases solides, rien de mystérieux.

- **Les mains nues** (main principale vide).
- **Le même rôle que l'Épée du Nuage Azur** : un art de départ. Il apprend à frapper, à enchaîner, à parer, mais au corps à corps.
- **Plus court et plus rapide que l'épée** : il faut s'approcher, mais les coups partent vite.

## Le rouleau

- **« Long Fist Scroll »** (Rouleau du Poing Long), un rouleau de toile simple.
- **Où le trouver** (proposition) : à la bibliothèque du Sanctuaire de la Falaise des Nuages, à côté du manuel de l'épée. Ce sont les deux arts que la secte enseigne à ses nouveaux disciples. Un exemplaire par joueur.
- **En l'utilisant**, le style apparaît dans l'onglet Techniques, avec son passif et sa première compétence. Le rouleau est consommé : un rouleau, une lecture (gardé par qui connaît déjà l'art).

## Le passif : « Steady Breath » (Souffle régulier)

Un seul passif est actif à la fois : celui du style principal.

- **Les M1 ne changent pas** : ce sont ceux d'Epic Fight à mains nues (son enchaînement de coups de poing, son attaque en courant et son attaque en l'air), avec ses animations.
- **Le passif n'ajoute qu'un effet** : chaque coup de poing qui touche un ennemi (mob hostile ou joueur) **rend 1 Qi**, et une parade parfaite en rend 5. Frapper dans le vide ne rend rien.

### La parade

- **La garde à mains nues** : le style permet de garder sans arme. Epic Fight n'a pas de garde de poing : le style a la sienne (les avant-bras levés devant le visage, poings fermés). Sans le Poing Long comme style principal, la garde Murim demande toujours une arme en main.
- **Parade parfaite** (dans les 0,5 s avant le coup) : le coup est annulé et l'attaquant est déséquilibré 0,6 seconde, comme pour tous les styles. Elle ouvre aussi le mouvement secret.
- **Parade normale** (garde tenue) : les dégâts sont réduits de 50 %, avec un léger recul.

## Les compétences actives

| Compétence | Débloquée par | Qi | Recharge |
|---|---|---|---|
| 1. **Tiger Step Punch** (Coup de poing du pas du tigre) | Le rouleau | 10 | 5 s |
| 2. **Rapid Fists** (Poings rapides) | Quête 1 | 15 | 8 s |
| 3. **Whirlwind Kick** (Coup de pied tourbillon) | Quête 2 | 20 | 12 s |

Chaque compétence a seulement deux versions : au sol et en l'air. Chacune a sa propre animation, faite dans Blockbench.

### 1. Tiger Step Punch

Un bond de 3 blocs en avant, terminé par un coup de poing (×1,3 l'attaque). C'est l'outil pour **s'approcher**, ce dont un art à mains nues a le plus besoin.

- **Au sol :** le bond et le coup de poing, qui repousse d'un bloc.
- **En l'air :** le joueur plonge en diagonale vers le sol et frappe vers le bas ; la cible touchée est mise à terre.
- **Animation :** le joueur se ramasse bas comme un tigre, poing arrière armé à la hanche, main avant ouverte vers la cible ; il bondit au ras du sol et retombe en fente profonde, le poing droit lancé à fond, l'épaule en avant, l'autre poing ramené à la hanche. En l'air : les deux poings serrés vers l'avant pendant la plongée, puis un coup de poing vers le sol à l'arrivée.
- **Effet :** de la poussière au départ du bond.

### 2. Rapid Fists

Une rafale de coups de poing très rapides devant soi (5 touches à ×0,3), puis un dernier coup qui repousse de 3 blocs (×0,8). La rafale tient la cible en place : c'est le **cœur des combos**.

- **Au sol :** la rafale complète, à 2 blocs de portée.
- **En l'air :** 3 touches seulement, puis un poing marteau (les deux poings joints) qui rabat la cible au sol.
- **Animation :** planté dans la posture du cavalier, les pieds larges, le joueur enchaîne des coups droits alternés, les épaules qui tournent à chaque coup ; le dernier part avec un pas en avant et tout le corps derrière le poing.
- **Effet :** des images rémanentes des poings pendant la rafale, une petite onde au dernier coup.

### 3. Whirlwind Kick

Le coup de pied tourbillon du Poing Long (旋风脚, *xuànfēng jiǎo*) : un saut en vrille, jambe tendue, qui frappe **tout autour** du joueur (rayon 2,5 blocs, ×1,0) et repousse de 3 blocs. C'est la réponse quand on est **encerclé**.

- **Au sol :** le tourbillon autour de soi.
- **En l'air :** le joueur retombe en tournoyant et frappe tout autour de lui à l'atterrissage ; pas de dégâts de chute.
- **Animation :** un élan d'un pas, le saut, le corps qui fait un tour complet à l'horizontale de la jambe d'attaque, les bras qui accompagnent la rotation, puis la réception accroupie, une main au sol.
- **Effet :** un cercle de poussière soulevée au ras du sol, tout autour.

## Le mouvement secret : « Inch Force » (Force du pouce)

寸劲, *cùn jìn* : la force qui ne demande aucun élan, le coup de poing donné à un pouce de la cible. Il n'apparaît pas dans l'onglet tant qu'on ne l'a pas découvert.

- **Comment le déclencher :** lancer Tiger Step Punch quand la cible est **déjà au contact** (à 1 bloc ou moins devant), dans la seconde qui suit une parade parfaite.
- **Ce qu'il fait :** au lieu de bondir, le joueur frappe sur place, sans élan. Au moment de l'impact, tout s'arrête un instant, puis la cible est **projetée à 6 blocs** (×2,0 l'attaque) et reste étourdie 1 seconde.
- **Animation :** le poing posé presque contre la poitrine de la cible, une petite torsion sèche des hanches et de l'épaule, à peine visible ; le joueur reste figé, le bras tendu, pendant que la cible s'envole.
- **Effet :** un anneau de Qi blanc sur la poitrine de la cible et une onde dans l'air derrière elle.
- **La première fois** qu'on le réussit, un message (« Power needs no distance… ») révèle la technique et l'ajoute à l'onglet.

C'est le petit secret d'un art de base : il récompense le joueur qui pare, reste collé à son adversaire et comprend que le bond est inutile au contact.

## Les enchaînements

Des combos vrais mais bornés, comme pour tous les styles : ils finissent par un repoussement ou une mise à terre, les dégâts baissent de 8 % par coup dans un même combo (sans descendre sous 40 %), et la cible peut s'échapper une fois avec l'évasion.

- **Combo de base :**
  1. **Tiger Step Punch** pour s'approcher ;
  2. M1 ×3 ;
  3. **Rapid Fists**, qui repousse.
- **Contre (le mouvement secret) :**
  1. parade parfaite, au contact ;
  2. **Tiger Step Punch** tout de suite : **Inch Force**, qui projette la cible.
- **Encerclé :**
  1. **Whirlwind Kick** pour faire de la place ;
  2. **Tiger Step Punch** sur l'ennemi le plus proche.

## Le finish : « Final Palm » (Dernière paume)

L'exécution du style, jouée avec B sur un joueur K.O. (voir [la mise à terre](../STYLES.md#mise-à-terre-et-finish-pvp)), avec sa propre animation : le joueur s'installe dans une posture large, inspire en ramenant lentement les mains devant lui, puis frappe d'une seule paume à la poitrine. L'anneau d'Inch Force s'allume au point d'impact et la victime retombe. Environ 2 secondes, filmé comme les autres finishs.

## Débloquer les compétences

| Étape | Ce qu'il faut faire | Récompense |
|---|---|---|
| Rouleau | Utiliser le rouleau | Le style, le passif, Tiger Step Punch |
| Quête 1 : « First Steps » (Premiers pas) | Toucher **30 fois** des ennemis avec les coups de poing, puis **5 ennemis** avec Tiger Step Punch | Rapid Fists |
| Quête 2 : « Unbroken Flurry » (Rafale ininterrompue) | Placer **10 fois** Rapid Fists en entier sur un ennemi (toutes les touches) | Whirlwind Kick |

L'onglet Techniques affiche la quête en cours et sa progression, comme pour l'épée.

## Maîtrise et évolutions

- **La maîtrise de chaque technique** monte de 0 à 100 quand elle touche un ennemi vivant, comme pour l'épée. Le passif gagne +1 tous les 8 coups de poing qui touchent.
- **La maîtrise du style** est la moyenne de ses techniques, passif compris.

Les évolutions restent simples : la même technique, un peu plus forte.

| Maîtrise du style | Évolution |
|---|---|
| 30 | **Tiger Step Punch** : le bond passe de 3 à 5 blocs. |
| 60 | **Rapid Fists** : 7 touches au lieu de 5. |
| 90 | **Whirlwind Kick** : rayon de 3,5 blocs, et il met à terre au lieu de repousser. |

## Contre-jeu face à ce style

- **Garder ses distances** : il frappe de près. Une fois Tiger Step Punch utilisé, il lui faut 5 secondes pour revenir d'un bond.
- **Parer la rafale** : Rapid Fists part toujours droit devant ; une parade parfaite la coupe.
- **Ne pas se laisser coller après une attaque parée** : c'est là que tombe Inch Force.
- **Whirlwind Kick se recharge en 12 secondes** : après l'avoir vu, on peut l'encercler à nouveau.

## Animations et effets

- **Les M1** : les animations d'Epic Fight à mains nues, sans changement.
- **À faire dans Blockbench**, comme Cloud Step Slash (`art/blockbench/`, puis un script dans `scripts/animation/`, voir `docs/ANIMATION_PIPELINE.md`) :
  - Tiger Step Punch, au sol et en l'air ;
  - Rapid Fists, au sol et en l'air ;
  - Whirlwind Kick, au sol et en l'air ;
  - Inch Force ;
  - le finish, Final Palm ;
  - la garde à mains nues, si Epic Fight n'en a pas.
- **Les effets**, côté client, comme les entailles de Thousand Drizzle : la poussière du bond, les images rémanentes et l'onde de la rafale, le cercle de poussière du tourbillon, l'anneau et l'onde d'Inch Force.

## Notes pour l'implémentation

- **Les mains nues dans Epic Fight :** la main vide utilise la catégorie d'arme « poing » d'Epic Fight, avec ses propres M1. Le passif n'a rien à changer à l'enchaînement, seulement à compter les coups qui touchent (comme Breath of the Blade pour l'épée).
- **La garde à mains nues :** `MurimGuardSkill` demande aujourd'hui une arme en main ; il faut l'ouvrir aux mains nues quand ce style est le style principal.
- **Les touches** des compétences viennent de boîtes posées sur la racine du squelette, comme pour l'épée, et sont vérifiées par un GameTest de positions comme `AzureHitboxGameTests`.
- **Tout le reste existe déjà** avec l'épée : la ruée (Cloud Step Slash), la rafale qui tient la cible (Thousand Drizzle), le secret déclenché après une parade parfaite (Swallow Returns), la frappe tout autour (le tournoiement de Swallow Returns), la mise à terre, les quêtes, la maîtrise, les combos dégressifs et le finish filmé.

## Ce qui est en jeu (14/10/2026)

- **Le rouleau** `murimblock:long_fist_scroll` (texture `scripts/worldgen/ScrollTexture.java`), dans l'onglet créatif Murimblock. Le lire apprend l'art, Tiger Step Punch (placé dans la barre) et, si c'est le premier art, en fait le style principal. On le trouve sur le Bandit Chief et dans les réserves des sites (voir en tête).
- **L'onglet Techniques** : le rouleau sur l'étagère, le passif (sans M1 à choisir : « les coups à mains nues »), les techniques, le secret caché, la quête en cours. Sur le passif d'un art qui n'est pas le style principal, un bouton **« Make it my main style »** change de style principal (avant, seule la commande le permettait). Icônes : `scripts/worldgen/TechniqueIcons.java`, teintes ambre.
- **Code** : `martial/LongFist.java` (variantes, Inch Force, effets, finish), `MartialArts` (techniques, quêtes, évolutions), `MartialCombat` (Qi des coups, quêtes, combos), `MurimGuardSkill` (garde à mains nues seulement si le Poing Long est le style principal), `client/LongFistClient.java` (pose de garde), `KnockdownService` / `FinishCinematic` (Final Palm). Animations : `scripts/animation/long-fist.cjs` → `art/blockbench/long_fist.bbmodel` → `animmodels/animations/biped/murim/`. Protocole réseau 15.
- **Écarts avec la fiche** :
  - Tiger Step Punch : la boîte couvre tout le bond (jusqu'à 3,4 blocs, 5,4 pour Tiger Leap), comme Cloud Step Slash.
  - Inch Force peut couper le recul de la garde, comme une riposte de l'épée (sinon la seconde de fenêtre passait dans le recul). La cible est déplacée tick par tick (6 blocs en arc bas, arrêtée par les murs) : l'étourdissement d'Epic Fight annule toute vitesse donnée.
  - Rapid Fists : pas encore d'images rémanentes des poings ni d'onde au dernier coup, seulement des étincelles de coup critique sur la cible.
- **Tests** : `LongFistGameTests` (rouleau, mains nues, garde, Qi des coups, Inch Force, chaque technique et sa version en l'air, quêtes, évolutions, finish, et le relevé des boîtes de frappe), `LongFistDataTest`. En vrai client, `gradlew -PgameTests runLongFistTour` (14/10, PASSED) : vrai clic M1 (le coup de poing d'Epic Fight, +1 Qi), garde tenue au vrai bouton (pose du Poing Long), Tiger Step Punch au clavier (bond de 3,46 blocs), Rapid Fists, Whirlwind Kick (devant et derrière touchés), Inch Force (cible projetée à 7,5 blocs), plongée en l'air, Final Palm. Pas encore validé par Enzo ni à deux clients.

## Questions ouvertes pour Enzo

1. **Où trouver le rouleau ?** Je propose la bibliothèque du sanctuaire, à côté du manuel de l'épée.
2. **Les dégâts à mains nues.** Un poing nu ne fait que 1 point dans Minecraft (une épée en fer : 6). Faut-il un bonus de dégâts à mains nues (par exemple +4), ou le Qi suffit-il comme seul effet du passif ?
3. **Les techniques seulement les mains vides ?** Proposition : oui, comme les techniques d'épée demandent une épée.
