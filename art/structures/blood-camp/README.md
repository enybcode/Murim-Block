# Camp de la secte du Sang — avant-poste du désert

Fichier : `camp_secte_sang_65.schem` — Minecraft Java 1.21.1, format WorldEdit Sponge v3.

Le camp occupe **65 × 65 blocs au sol**. Le fichier mesure **65 × 23 × 65**
(largeur × hauteur × profondeur), fondations et marge d'air comprises.
Il contient 22 210 blocs non vides, soit un volume de collage de 97 175 cellules.

## Contenu

- Une enceinte basse avec porte couverte et deux tours de guet.
- Deux dortoirs meublés, six lits chacun.
- Une armurerie, des réserves et une cuisine.
- Un sanctuaire surélevé d'un bloc, avec autel, bibliothèques et cristaux décoratifs.
- Une cour de 19 × 17 blocs, dégagée sur cinq blocs de hauteur.
- Un espace d'entraînement et trois bassins fermés de sang sombre.
- Huit coffres et neuf tonneaux vides ; aucun butin prédéfini, aucun mob, aucun boss.

Les tours sont accessibles par des **échelles intérieures**. L'accès au
sanctuaire comporte un escalier sous l'avancée de son toit.
Les marques de la cour sont des incrustations au niveau du sol.

## Dépendances

Utiliser le mod principal **Murimblock**, avec son bloc `murimblock:dark_blood`.
Tout le reste utilise les blocs de Minecraft. Aucun addon n'est fourni ou créé.
L'ancien addon Blood Sect Decor n'est pas requis.

WorldEdit sert à importer le fichier. Le camp n'est pas ajouté automatiquement
à la génération du monde et ce livrable n'installe rien dans le profil de jeu.

## Importer plus tard

1. Copier le `.schem` dans `config/worldedit/schematics` du profil Minecraft.
2. Dans un monde de test, choisir un terrain plat et libre. Se placer à l'endroit
   prévu pour l'entrée sud, les pieds au niveau de marche souhaité.
3. Sans changer de position entre les commandes :

```text
//schem load camp_secte_sang_65
//paste
```

Coller **avec l'air**, sans `-a`, pour conserver les intérieurs dégagés. Le collage
remplace les blocs dans son volume. Les fondations occupent quatre niveaux sous
les pieds, en comptant le sol visible.

Si WorldEdit annonce le collage en `(X, Y, Z)`, les limites du volume sont :

- X de `X − 32` à `X + 32` ;
- Y de `Y − 4` à `Y + 18` ;
- Z de `Z − 64` à `Z` : le camp s'étend **vers le nord**.

L'entrée est au point de collage. Le centre de la cour est `(X, Y, Z − 26)`.
`//undo` permet d'annuler si ce collage est la dernière opération WorldEdit.

La protection des apparitions dans une base WorldEdit se configure séparément
avec les commandes Murimblock ; un fichier `.schem` ne transporte pas cette
protection. Aucun réglage d'apparition ni de faction n'a été modifié ici.

## Aperçus et vérifications

- `export-stats.json` : dimensions, matériaux, coordonnées et SHA-256 du fichier.
- `verification.json` : résultat des contrôles indépendants après réouverture du NBT.

Vérifié : 97 175 cellules décodées, 58 états de blocs valides, 19 points
d'accès reliés à l'entrée, cour dégagée de 323 cases, bassins bordés avec fond,
55 entités de blocs cohérentes et 80 vérifications de supports (lits, échelles,
bannières, lanternes).

Ces premiers contrôles sont statiques. Les aperçus et GIFs sont conservés localement, hors du dépôt. Le `.schem` conserve les véritables types et propriétés des blocs.

Le 17 octobre 2026, le même fichier a ensuite été importé dans un véritable
client Minecraft 1.21.1 avec Murimblock, dans un monde désertique de test.
Après les mises à jour de voisinage et la stabilisation, les types des 22 210
blocs sont préservés et les 38 cellules de sang restent contenues. Les
55 entités de blocs (lits, coffres, bannières, etc.) sont importées.
Le rapport `verification-minecraft.json` conserve les résultats de cet essai. Les GIFs locaux sont capturés depuis le framebuffer du jeu : vue à 360° et visite de l'entrée, de la cour et du bassin.
Cette capture ne constitue pas une vérification multijoueur ni de tous les intérieurs.


## Sources et reproduction

Le générateur est [`scripts/worldgen/build_blood_camp.py`](../../../scripts/worldgen/build_blood_camp.py), avec un contrôle indépendant dans [`verify_blood_camp.py`](../../../scripts/worldgen/verify_blood_camp.py).

Prérequis des scripts : Python, `numpy`, `nbtlib` et le rapport de blocs `reports/blocks.json` généré par Minecraft **1.21.1**. Ce rapport reste un fichier local ; il n'est pas inclus dans le dépôt.

Depuis la racine du dépôt, avec le chemin du rapport local :

```text
python scripts/worldgen/verify_blood_camp.py --block-report /chemin/reports/blocks.json
python scripts/worldgen/build_blood_camp.py --block-report /chemin/reports/blocks.json --asset-dir build/blood-camp-regenerated
python scripts/worldgen/verify_blood_camp.py --block-report /chemin/reports/blocks.json --asset-dir build/blood-camp-regenerated
```

La géométrie est déterministe ; la date de génération inscrite dans le NBT fait varier le SHA-256 d'une nouvelle exportation.

Pour refaire la visite native, avec Java 21 et les dépendances du projet :

```text
gradlew.bat -PgameTests runBloodCampTour
```

Ce lancement lit le `.schem` versionné, crée uniquement un monde de développement sous `build/blood-camp-tour-run` et vérifie ses blocs après leurs mises à jour. Il ne place pas le camp automatiquement dans une sauvegarde de joueur.
