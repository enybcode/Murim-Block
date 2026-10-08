# Publication sur GitHub

Le depot principal est https://github.com/enybcode/Murim-Block.

## Branches

- `test` : modifications en cours de verification, disponibles pour les essais en jeu.
- `main` : version stable, contenant uniquement les modifications validees par Enzo.
- `master` : ancienne branche principale, conservee pour son historique.

## Pour chaque modification

1. Developper sur `test` et verifier les changements.
2. Creer un commit cible puis le publier sur GitHub, sur `test`.
3. Ouvrir ou mettre a jour la pull request `test` vers `main`.
4. Attendre la validation explicite d'Enzo, notamment pour le rendu et les interactions en jeu.
5. Fusionner vers `main` lorsque les controles GitHub et la validation utilisateur sont satisfaits.

Un build reussi ne remplace pas la validation en jeu. La branche `test` reste disponible apres la fusion.

Le fichier `AGENTS.md` impose ce fonctionnement aux agents qui travaillent dans ce depot.

## Dossiers Et Cloud

Ne pas creer deux copies du code nommees `test` et `main` dans le projet IntelliJ.
Les branches Git representent ces deux etats. Les sources livrees sont dans
`src/main`, les tests unitaires dans `src/test` et le harnais de jeu dans
`src/gameTest`. Les anciennes images sont des references sous `docs/gui/legacy-v4`.

Le cloud importe le meme depot GitHub et doit selectionner `test` pour les
changements non valides. Voir `CLOUD_IMPORT.md` pour les scripts, le cache et les
acces reseau. Ne pas reinitialiser un checkout sale ni ecraser un travail cloud
pour le synchroniser. Faire un commit cible et mettre a jour la PR existante.
