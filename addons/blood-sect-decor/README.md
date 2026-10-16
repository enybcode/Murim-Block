# Blood Sect Decor 1.10.1

Version autonome du culte du Sang essayée par Colleuhu95 dans Minecraft. Les
sources, ressources et modèles d'édition sont conservés ici pour qu'Enzo puisse
la reconstruire et l'évaluer. Ce projet produit **un JAR séparé** ; il n'est pas
inclus dans le build du mod principal Murimblock.

## Contenu

- **Blood Ninja** : 24 PV, patrouille autour de son poste, poursuit les joueurs
  en survie puis revient ; ignore les joueurs créatifs et spectateurs.
- **Blood Dao** réservé au ninja, avec M1 diagonal animé, traînée et gouttes de sang.
  M1 : 4 dégâts, impact à 0,40 s, intervalle minimum de 1,15 s.
- **Blood Cloud Step Slash** : préparation de 0,50 s, ruée de 5 blocs,
  6 dégâts par cible, 4 cibles au maximum, recharge de 5,50 s.
  La première cible réellement blessée rend **15 % des PV maximum**, une seule
  fois par ruée et sans dépasser le maximum (3,6 PV pour un ninja de 24 PV).
- Liquide de sang sombre, seau et particules. Aucun drop d'objet ajouté ;
  le dao ne tombe pas à la mort. Le ninja donne 6 points d'expérience.

Le [détail du comportement](docs/blood-dao.md) contient les chiffres et limites.
Le [rapport de livraison](../../docs/DELIVERY_BLOOD_CULT_2026_10_16.md) donne les
résultats des contrôles, dont l'échec optionnel Dawn Day du projet principal.
Les modèles de référence et les poses sont dans `art/`, notamment le ninja à bras
raccourcis et `art/blockbench/blood_dao.bbmodel`. Certains anciens assets restent
dans le projet pour conserver les références de la version testée.

## Construction et contrôles

Java **21**, Minecraft **1.21.1**, NeoForge **21.1.248**, Gradle **9.2.1**.
Utiliser le wrapper du dépôt, depuis sa racine :

```powershell
.\gradlew.bat -p addons/blood-sect-decor build compileGameTestJava
.\gradlew.bat -p addons/blood-sect-decor -PgameTests runGameTestServer
```

Sur Linux/macOS, remplacer `.\gradlew.bat` par `./gradlew`.
Le JAR est créé dans `addons/blood-sect-decor/build/libs/blood-sect-decor-1.10.1.jar`.
Les classes et mondes du harnais restent dans les dossiers de développement ;
ne pas installer un JAR de test ni un JAR contenant `src/gameTest`.

Les tours graphiques facultatifs se lancent avec le même `-p` et
`-PgameTests runCloudStepTour` ou `-PgameTests runDaoM1Tour`.
Le client normal utilise `-p addons/blood-sect-decor runClient`, sans `-PgameTests`.

## Installation de test

Installer le JAR dans un profil NeoForge 1.21.1. Remplacer l'ancienne version
de Blood Sect Decor ; ne pas charger deux JAR déclarant le mod `bloodsect`.
Installer WorldEdit 7.3.8 pour importer la [base souterraine](../../art/structures/blood-cult/README.md).
WorldEdit n'est pas une dépendance du mob ni du liquide.

Dans un monde de test avec commandes autorisées :

```text
/summon bloodsect:blood_ninja ~ ~ ~
/give @s bloodsect:blood_ninja_spawn_egg
/give @s bloodsect:blood_bucket
```

Le point d'apparition devient le poste de garde. Passer en survie pour combattre,
en créatif pour interrompre. Les ninjas sont placés manuellement : la structure
ne contient aucune entité et aucun spawn automatique n'est installé.

## Limites de cette publication

Cette version conserve son espace de noms **`bloodsect`**, ses textes anglais
dans `assets/bloodsect/lang/en_us.json`, son rendu et son combat autonomes.
Elle ne remplace aucun mob existant de Murimblock. Les textes et techniques du
mod principal restent dans `assets/murimblock/lang/en_us.json`.

L'intégration du ninja aux règles Epic Fight/Murimblock (parade, progression,
factions, génération, drops) et le boss du culte restent à concevoir et valider.
Ne pas présenter cet addon comme un mob déjà intégré à ces systèmes.
La compatibilité en jeu avec le pack Murimblock complet et le multijoueur
reste à vérifier ; les essais utilisateur précédents portaient sur le profil
NeoForge avec Blood Sect Decor et WorldEdit.
