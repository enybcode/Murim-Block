# Proposition Des Onglets

Cette demo est une proposition visuelle. Elle ne modifie ni le rendu Minecraft
ni les interactions du mod. Ouvrir `tab-layout-demo.html` depuis ce depot.

## Ce Qui Est Conserve

- Les textures V4 existantes, le parchemin, les cadres et les accents dores.
- Le panneau logique de 320 x 214 pixels, compatible avec le viewport minimum.
- Les quatre onglets et le bouton de fermeture aux memes emplacements.
- Le modele Minecraft du joueur dans Profil uniquement.

## Repartition

| Onglet | Contenu | Personnage |
| --- | --- | --- |
| Profil | Identite, royaume, stade, Qi et etat de combat | Oui |
| Techniques | Repertoire a gauche, details a droite ; etat vide tant que le systeme n'existe pas | Non |
| Cultivation | Etat actuel, prochain stade, Qi Max requis et conditions de percee | Non |
| Infos | Touches configurees et etat utile du joueur | Non |

Les valeurs et la silhouette de la demo sont illustratives. La police bitmap
de la demo est indicative : les largeurs devront etre reverifiees avec la
vraie police Minecraft et les traductions au moment de l'integration.

## Corrections A Integrer Apres Validation

1. Conditionner le rendu du joueur a `page == Page.PROFILE`.
2. Donner a chaque page sa propre disposition, au lieu d'afficher les memes
   blocs de profil sur toutes les pages.
3. Recomposer les boutons avec les sprites existants : icone de 11 pixels de
   haut a y=175, texte a y=190. Garder au moins 4 pixels entre les deux.
4. Definir des rectangles de texte avec des marges de 6 a 8 pixels. Exclure
   explicitement les icones, bordures et illustrations de ces rectangles.
5. Utiliser `font.split` pour les valeurs longues et une infobulle pour ce qui
   ne tient toujours pas. Ne pas reduire globalement la taille de la police.
6. Lire les valeurs synchronisees depuis `MurimblockApi` et les touches depuis
   les `KeyMapping`. Ne pas ajouter de boutons de technique ou de percee qui
   n'ont pas d'action disponible dans le projet.
7. Tester les quatre onglets, noms longs, royaumes longs, francais et anglais,
   changements de taille GUI, fermeture et commandes existantes en jeu.

La validation de cette demo n'est pas une validation du mod en jeu. La branche
`main` reste inchangee jusqu'a une validation explicite de la version integree.

## Verification De La Demo

Avec Node.js, Playwright et Chrome disponibles, executer
`node docs/gui/verify-tab-demo.cjs` depuis la racine du depot. Le script verifie
les quatre pages, les limites des textes, la navigation souris et clavier,
l'absence d'erreurs navigateur et le viewport mobile. Il produit les captures
dans `build/gui-demo` puis ferme le navigateur et son serveur temporaire.
