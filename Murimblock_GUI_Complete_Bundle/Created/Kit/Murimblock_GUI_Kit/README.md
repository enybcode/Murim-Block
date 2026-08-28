# Murimblock GUI Kit

## Édition pixel art / Aseprite

Le kit est désormais construit nativement sur une grille **270 × 179 px**, avec une palette limitée, des traits de 1 à 2 pixels et aucun anticrénelage. Les versions du dossier `Mockups/` et des dossiers d’assets sont des agrandissements ×4 en **nearest-neighbor** ; les textures sources prêtes pour Minecraft sont dans `Minecraft_1x/`.

Les boutons, onglets, slots, tomes, panneaux, icônes Qi et décorations principales ont été redessinés en pixels entiers. Ne pas appliquer de filtrage bilinéaire lors de l’intégration.

Kit graphique autonome pour les futures interfaces de Murimblock. Aucun fichier du mod, code Java, comportement de gameplay ou système de Qi n’est inclus.

## Direction visuelle

Interface sobre inspirée du Murim / Wuxia : encre noire, blanc cassé, gris, accents Qi bleus et sélection dorée. Les éléments sont reconstruits proprement à partir des références artistiques, sans découpe directe.

## Échelle et dimensions

- Base Minecraft visée : **270 × 179 px**.
- Mockups fournis : **1080 × 716 px** (×4).
- Assets individuels : dessinés en SVG avec dimensions PNG ×4, pour permettre un downscale contrôlé.
- Pour une version 1×, réduire exactement à 25 % avec un filtre nearest-neighbor pour un rendu plus pixelisé, ou Lanczos pour un rendu lissé.

## Dossiers

- Mockups/ : vues complètes Profil et Techniques avec texte de démonstration.
- Panels/ : fonds et panneaux réutilisables, sans texte.
- Buttons/ : boutons génériques, fermeture et retour.
- Tabs/ : fonds d’onglets sans icône ni texte.
- Qi/ : icône, fond de barre et remplissages séparés.
- Techniques/ : slots de techniques.
- Tomes/ : lignes / cartes de tomes.
- Icons/ : pictogrammes autonomes.
- Decorations/ : séparateurs, bambou, montagnes, cercle de pinceau et ornements.
- SVG/ : sources vectorielles de tous les PNG.

## États

Boutons : normal, hover, pressed, disabled, selected. Onglets : normal, hover, active, disabled. Techniques : normal, hover, selected, locked, unknown. Tomes : normal, selected, locked, unknown.

## Superposition conseillée

- Barre de Qi : qi_bar_background.png → remplissage recadré horizontalement → qi_icon.png → texte dynamique du jeu.
- Slot technique : état du slot → icône de technique → cadenas ou point d’interrogation si nécessaire.
- Tome : état du tome → icône de livre → libellé dynamique.
- Onglet : fond d’état → icône → libellé dynamique.

## Règles d’intégration future

Les PNG individuels ne contiennent pas de texte. Conserver le ratio des assets, éviter les redimensionnements fractionnaires et ne pas fusionner le remplissage de Qi avec son fond. Les mockups servent de référence de composition uniquement.
