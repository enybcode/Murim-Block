# Manuscrit Clair : Disposition Integree

Version implementee le 2026-10-08 sur `test`, en attente de validation en jeu.
Les references artistiques locales `Manuscrit_Clair_Pages_V2` restent hors Git.
Le nouveau rendu remplace l'utilisation des fonds textures V4 ; il ne reutilise
pas leurs decoupes, petits motifs ou icones. Les anciennes ressources et la demo
`tab-layout-demo.html` restent des references historiques, pas le GUI actuel.

## Contrat Visuel

Refonte compacte du 12/10/2026 (Enzo trouvait l'ancien ecran terne et trop grand).
Memes informations et memes controles ; seul l'habillage et la taille changent.

- Papier `#F3EBD4`, cartes `#EAE0C4` bordees `#D2C29C`, encre `#1F2420`,
  encre douce `#343B35`, or `#C9A74B` / `#8E7230`, sceau `#A8322D`, Qi `#238DD4`
  avec reflets `#73C2F2` / `#15598A`, texte secondaire `#7A6A4C`.
- Geometrie native `GuiGraphics.fill`, coordonnees entieres, bordures de 1 pixel.
- Bandeau titre encre : titre or 1x entre deux filets, petit sceau rouge (王 stylise),
  fermeture `x` carree. Quatre angles dores.
- Onglets en marque-pages sous le bandeau ; l'onglet actif se fond dans la page.
- Informations groupees en cartes ; titres de section en petites capitales avec
  un court trait or. Royaume en 2x, pastilles d'etape (Early/Middle/Late/Peak),
  barre de Qi a quarts, valeurs de Qi en points entiers (`87 / 120`), percee en
  pastille (or = prete, contour = pas prete, encre = terminee), touches en capuchons.
- Police `murimblock:manuscript` reservee a ces ecrans ; pas de police globale changee.
- Texte 1x, titre/nom 2x seulement si toute la chaine tient, sans scale fractionnaire.
- Retours a la ligne, scissor, puis ellipse/infobulle pour une valeur trop longue.

## Taille Et Zones

`MurimProfileLayout.forViewport` utilise les dimensions GUI logiques de Minecraft,
pas les pixels physiques de la fenetre. Cadre maximum **320 x 200** (contre
480 x 300 auparavant), reduit a la taille disponible avec 8 pixels de marge. Au
viewport minimum 320 x 240, le cadre fait 304 x 200. Le contenu est redispose au
lieu de reduire une grande texture.

Le titre occupe la bande haute ; les quatre onglets sont juste en dessous et
touchent la page (`PAGE_TOP`).
Le joueur, les textes, barres et boutons ont des rectangles distincts. Les
controles de reglages ne recouvrent pas le panneau de statut. Les tests couvrent
320x240, 427x240, 640x360, 854x480 et 1920x1080 en dimensions logiques.

## Chaque Page

| Page | Contenu actuel | Interaction |
| --- | --- | --- |
| Profile | Vrai joueur 3D a gauche ; nom, royaume synchronise, Stage ???, Qi courant/max a droite. | Navigation/fermeture ; aucun slot d'armure. |
| Techniques | Library et Details, etats vides lisibles. Aucun personnage. | Navigation ; pas de fausse technique ou de livre Epic Fight. |
| Cultivation | Royaume/stade reels, prochain stade, Qi Max requis, reserve et condition de percee. | Consultation ; pas de bouton de percee sans action existante. |
| Info | Raccourcis reellement configures, Stage: ??? et Qi Max. | Combat Settings. |
| Combat Settings | Six reglages camera/affichage, Stage: ???, bouton Keybinds. | Camera cyclique, cinq booleens, sauvegarde via ClientConfig, controles Minecraft. |

Le placeholder ne modifie pas les donnees de cultivation. Cultivation continue
a afficher les etapes Early/Middle/etc : ce sont des donnees utiles de progression,
distinctes de l'ancien statut Awakened Human retire de Profile/Info.
L'etat Combat Mode On/Off n'est plus affiche. Le raccourci Combat Mode reste dans
Info, car il sert a configurer une commande, pas a dupliquer un voyant de statut.
Le petit HUD Qi existant pendant le combat n'est ni agrandi ni remplace ici.

## Integration Et Verification

`MurimProfileScreen` lit `MurimblockApi` et les `KeyMapping` existants. Les IDs
serialises et l'API sont conserves. `K` (touche Profile actuelle), la touche
d'inventaire et Escape ferment le menu, tout comme X. Chaque changement d'onglet
reconstruit ses widgets ; quitter Info remet son sous-menu a l'etat normal.

La boucle de verification comprend tests unitaires des rectangles/compteurs,
client Minecraft reel, clics sur quatre onglets et six reglages avec restauration,
ouverture des controles, captures compactes/larges, controle des couleurs nettes.
Voir le [compte rendu](../DELIVERY_CLEAR_MANUSCRIPT_MOBS.md) pour les resultats et
les essais manuels encore requis. Une capture correcte ne vaut pas ton approbation.

```powershell
.\gradlew.bat test
.\gradlew.bat -PgameTests runVisualSmoke
```

Le smoke utilise uniquement le monde jetable deja present dans
`build/client-smoke-run/saves/murim-smoke`. Les captures sont ecrites dans
`build/client-smoke-run/screenshots`. Ne pas copier un monde personnel non sauvegarde
dans ce harness : il modifie deliberement les donnees de test et les inventaires.
Les classes du harness ne sont pas dans le jar publie.
