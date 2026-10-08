# Compte Rendu : Manuscrit Clair Et Socle Des Mobs

Livraison du 2026-10-08 dans le projet IntelliJ, branche `test`.
Les controles techniques ci-dessous ne remplacent pas la validation du joueur.
Pas de promotion automatique sur `main`.

## GUI Livre

Les quatre pages et les reglages utilisent maintenant le Manuscrit Clair :
parchemin uni, contours simples, quelques accents dores, onglets sans icone.
Les anciennes textures detaillees ne servent plus de fonds au menu. Aucun
decoupage d'image IA, bruit de texture ou illustration sous le texte.

Le cadre s'adapte au viewport jusqu'a 480 x 300 pixels logiques, avec des zones
de texte reservees et des pixels entiers. Le joueur 3D reste uniquement dans
Profile. Profile/Info affichent le placeholder de stade ??? au lieu de l'ancien
statut Awakened Human. Le voyant Combat Mode On/Off est retire, mais la commande
et son raccourci restent fonctionnels. Cultivation garde sa vraie progression.

Navigation, fermeture, camera, cinq reglages booleens et ouverture des controles
sont raccordes aux interactions existantes. La police reste locale aux ecrans
Murimblock. Le petit HUD Qi en jeu n'est pas agrandi. Techniques reste vide :
aucun faux style ou skill Epic Fight n'est ajoute pour remplir l'espace.

Implementation : `MurimProfileScreen`, `MurimProfileLayout`, `en_us.json`.
Contrat detaille : [TAB_LAYOUT.md](gui/TAB_LAYOUT.md).

## Debut Concret Pour Les Mobs

Un adversaire technique invocable est ajoute : `murimblock:training_opponent`.
Il garde l'apparence/sons vanilla Zombie, une epee en fer, le preset anime Epic
Fight et ses comportements de combat. Il est hostile en survie et non equilibre.
Ce n'est pas un personnage Murim final ou un simple exemple JSON inactif.

Attributs moteur explicites, renderer client, IA sans second chemin melee vanilla,
table de loot vide et Qi/XP a zero. Aucun spawn naturel, oeuf, cout de Qi nouveau,
garde de mob ou animation originale ajoute. Les autres mobs et boss restent
sur leur integration existante ; aucune migration globale du monde.

```mcfunction
/summon murimblock:training_opponent ~ ~ ~3
/kill @e[type=murimblock:training_opponent]
```

Utiliser un monde jetable, commandes autorisees et difficulte Normal.

## Animations Et Recherche

[ANIMATION_PIPELINE.md](ANIMATION_PIPELINE.md) decrit le moteur effectivement
installe **21.17.3.1** : types/patches, rig/mesh/clip, declarations, horloge,
phases de contact, deduplication des hits, autorite serveur, IA et synchronisation.
Il precise les fichiers a remettre pour une animation originale et les etapes
de son integration. README, architecture et guide des mobs sont actualises.

Le pont GeckoLib examine peut annuler le rendu Geo au profit d'Epic Fight pour
une meme entite patchee. Il ne convertit pas automatiquement les animations.
GeckoLib n'est pas installe ici : la compatibilite d'un couple de versions ou
d'une armure concrete n'est donc pas declaree validee en execution.

## Boucle De Verification Executee

| Controle local final | Resultat |
| --- | --- |
| `gradlew.bat build compileGameTestJava prepareClientRun` | Reussi ; 117 tests unitaires, zero echec/erreur. |
| `gradlew.bat -PgameTests runGameTestServer` | Reussi ; 24 GameTests serveur obligatoires. |
| `gradlew.bat -PgameTests runVisualSmoke` | Reussi ; client reel, marqueur final, 15 captures et controles de pixels. |
| Inspection visuelle des captures | Quatre pages, reglages, taille compacte/large, modele et poses du mob inspectes. |
| Inspection du jar de release | Pas de FoundationGameTests, ClientVisualSmoke/ClientCaptureChecks ou override de la police Minecraft globale. |
| `git diff --check` | Pas d'erreur d'espacement. |

Les tests GUI couvrent rectangles non chevauchants, compteur Qi borne, labels,
police bitmap, navigation souris, fermeture X/K/E/Escape, les six reglages avec
restauration et l'ouverture des keybinds. Captures reelles en 1280x720 et 960x720,
GUI scale 2 et 3. Les controles de pixels verifient les aplats exacts du menu et
un modele non vide dont la pose change lors de l'attaque synchronisee.

Les cinq nouveaux tests serveur verifient patch/armature/attributs et goals,
equipment/adulte apres NBT, mort sans loot/XP/Qi, absence de dommage pendant la
preparation puis un contact unique, selection autonome d'une attaque et absence
de hit retarde apres suppression de l'attaquant pendant la preparation.
Les 19 regressions precedentes gardent Qi/cultivation, sauvegarde, recharge,
garde frontale, roulade et filtrage du contenu Epic Fight controles.

Corrections pendant la boucle : centrage de X, suppression d'une infobulle de
reglage superflue, tests du nouveau compteur adaptatif au lieu des anciennes
textures. Le controle de goals a ete corrige pour reconnaitre la poursuite Epic
Fight (sous-classe melee sans callback de degat). Une assertion de ressources
incorrecte a ete retiree : le classpath contient naturellement la police vanilla.
Les derniers runs ci-dessus sont passes apres ces corrections.

Fichiers locaux de verification : `build/reports/tests/test`,
`build/game-test-run/logs/latest.log`,
`build/client-smoke-run/visual-smoke-passed.txt` et
`build/client-smoke-run/screenshots/01-profile.png` a `15-mob-contact.png`.
Le harness et ses worlds ne sont pas la configuration normale IntelliJ ;
`prepareClientRun` sans `-PgameTests` restaure la preparation normale.

## Limites Et Validation A Faire

1. Ton approbation visuelle dans IntelliJ : resolution/GUI scale habituels, longs
   noms, navigation clavier et reglages. Redemarrer le jeu pour charger les classes.
2. Combat contre le prototype en troisieme personne : garde/esquive, plusieurs
   distances, obstacles, escaliers et eau. Les tests ne prouvent pas le ressenti.
3. Deux clients sur serveur dedie : pose des observateurs, retard reseau, tracking
   tardif, deconnexion, dimensions, arme changee, mort/stun pendant une attaque.
   Le client solo et les connexions embarquees ne valent pas un test a deux joueurs.
4. Choisir le vrai design du premier bandit, puis verifier un rig et un export
   original. Aucun convertisseur automatique GeckoLib vers Epic Fight livre.
5. GeckoLib seulement si necessaire, avec version NeoForge 1.21.1 epinglee et
   tests d'un modele concret. Pas de garantie universelle sur les autres addons.

Les diagnostics upstream deja connus restent : sous-titres Epic Fight manquants,
`air_slash`, couche optionnelle WaveyCapes et connexion au service web Epic Fight.
Ils ne font pas echouer ces runs, mais ne sont pas annonces comme corriges.
La compatibilite JEI/EMI et les usages admin inhabituels ne sont pas exhaustifs.

Les modifications utilisateur hors perimetre et les references `.local` sont
preservees et exclues de la publication. Les donnees sauvegardees, IDs existants
et API publique ne sont pas migres. Le prototype ajoute un ID neuf : ne pas le
renommer arbitrairement dans un monde qui l'a sauvegarde.

Suite raisonnable : validation de ce GUI/prototype, choix du bandit et une seule
attaque originale aller-retour. Styles joueur, quatre M1 et clash physique ne
sont pas implementes dans cette livraison.
