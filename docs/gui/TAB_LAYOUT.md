# Onglets Integres

Les quatre dispositions sont integrees dans `MurimProfileScreen`, en anglais
avec la nouvelle police pixel. Ouvrir `tab-layout-demo.html` depuis ce depot
pour consulter leur apercu avec des valeurs illustratives. Les interactions
de gameplay sont inchangees.

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

Les valeurs et la silhouette de la demo sont illustratives. La police utilise
les memes glyphes et avances que `murimblock:manuscript` dans le mod. Elle
conserve de vraies minuscules, des pixels nets et un fallback Minecraft pour
les noms de joueurs non ASCII. Le GUI Minecraft utilise les vraies donnees
synchronisees du joueur et les touches configurees, pas les exemples de la demo.

## Corrections Integrees

1. Le rendu du joueur est conditionne a `page.showsPlayer()`, vrai uniquement
   pour `Page.PROFILE`.
2. Chaque page a sa propre disposition, au lieu d'afficher les memes
   blocs de profil sur toutes les pages.
3. Les boutons sont recomposes avec les sprites existants : icone de 11 pixels
   de haut a y=175, texte a y=190, avec au moins 4 pixels entre les deux.
4. `MurimProfileLayout` definit les rectangles de texte. Les tests excluent
   explicitement les icones et illustrations de ces rectangles.
5. `font.split` gere les lignes longues ; une ellipse et une infobulle montrent
   ce qui ne tient toujours pas. Chaque champ est limite par un scissor.
6. Les valeurs viennent de `MurimblockApi` et les touches des `KeyMapping`.
   Aucun bouton de technique ou de percee sans action disponible n'est ajoute.

## Verification Et Validation

- Build local reussi et 107 tests sans echec : ressources, police, geometrie,
  espaces reserves et remplissage des deux barres de Qi notamment.
- Client lance depuis IntelliJ ; ouverture avec K et navigation souris dans
  les quatre pages observes en jeu. Le personnage reste sur Profile uniquement.
- Info affiche bien la touche de recharge configuree C, au lieu du R de la demo.
- Le controle du PC a ete interrompu par l'utilisateur avant les essais manuels
  de fermeture, clavier, redimensionnement et changements de valeurs via commandes.
  Ces essais ne sont pas declares verifies.
- Enzo a explicitement valide la version integree le 2026-10-03 et demande
  sa publication GitHub. La promotion de `test` vers `main` suit les controles CI.

## Verification De La Demo

Avec Node.js, Playwright et Chrome disponibles, executer
`node docs/gui/verify-tab-demo.cjs` depuis la racine du depot. Le script verifie
les quatre pages, les limites des textes, la navigation souris et clavier,
l'absence d'erreurs navigateur et le viewport mobile. Il produit les captures
dans `build/gui-demo` puis ferme le navigateur et son serveur temporaire.

Pour regenerer la police, installer `pngjs` dans l'environnement Node puis
executer `node docs/gui/generate-manuscript-font.cjs`. La source editable est
`manuscript-glyphs.json` ; le PNG Minecraft et `manuscript-font.js` sont generes.

## Langue Du Mod

`en_us.json` est la source de tous les libelles traduisibles. Le mod utilise
le fallback anglais de Minecraft meme si une autre langue est selectionnee.
Les messages de commande et statuts de cultivation sont egalement anglais.
Le client de developpement utilise deja `lang:en_us` dans `run/options.txt`.
Les champs historiques `frenchName` de l'API sont conserves pour compatibilite
mais ne sont pas utilises pour afficher les interfaces du jeu.
