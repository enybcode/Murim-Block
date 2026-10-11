# Reprise Murimblock Pour Claude

Etat prepare le 12 octobre 2026. Synthese des decisions humaines et du depot,
pas une preuve de nouveaux tests ni une copie exhaustive des conversations.
Lire les details seulement lorsque la tache les concerne. Le code prime sur
une documentation contradictoire ; signaler les ecarts au lieu de les masquer.

## Source Et Versions

- Source : projet IntelliJ Murim block, depot `enybcode/Murim-Block`.
- Sur ce PC : `C:\Users\enzob\Documents\Murim block`, pas le dossier distinct
  `Murimblock claude`, pas MCreator. Verifier le vrai cwd avant toute action.
- Au debut de ce handoff, `test` et `origin/test` etaient au commit `9aaa67c` ;
  `origin/main` au commit `5e7bf31`. Ces SHA sont des reperes, pas des pins.
- PR ouverte : https://github.com/enybcode/Murim-Block/pull/3. Ne pas fusionner
  sans validation explicite d'Enzo, meme si les tests ou CI passent.
- Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Gradle 9.2.1, Epic Fight
  21.17.3.1 / Modrinth `8HHhJt6i`. Protocole Murim 7. Conserver ces versions.
- Modification locale preexistante observee : `QiChargeClientEffects.java`.
  Preserver son contenu ; ne pas l'inclure aveuglement dans un commit.

## Decisions A Ne Pas Refaire

- Theme Murim/Wuxia, style Minecraft pixel-art, manuscrit clair et epure.
  Pas de parchemin realiste, micro-details, icones surchargees ou texte illisible.
- Interface en anglais, font manuscrit uniquement dans nos ecrans. Quatre pages :
  Profile, Techniques, Cultivation, Info. Joueur 3D uniquement dans Profile.
  Profile/Info : Stage ??? ; pas de Awakened Human ni voyant Combat Mode On/Off.
- HUD Qi discret : petite barre bleue, pas de gros panneau ni informations
  permanentes superflues. Les anciennes references V4 sont historiques.
- Direction retenue : Epic Fight comme dependance, pas un moteur de combat
  copie ou une reprise du vieux chantier de refonte autonome. Garder son moteur
  et ses API, restreindre sa presentation et sa progression natives.
- Ni jauge jaune/stamina limitante, ni livres/items Epic Fight exposes, ni
  techniques Epic Fight dans Techniques. Pas de nouveau cout Qi pour garde,
  roulade ou attaques de base ; conserver les comportements Qi hors perimetre.
- Ne pas extraire les animations/assets Epic Fight dans notre JAR : leurs
  droits sont distincts de ceux du code. Referencer la dependance installee.

## Existant Et Points D'Entree

- GUI adaptatif Clear Manuscript : `MurimProfileScreen`, `MurimProfileLayout`,
  `assets/murimblock/lang/en_us.json`. Navigation et reglages raccordes.
- `CombatQiHud` : barre bleue de 182 pixels en combat ; XP sauvegardee intacte.
- Epic Fight gere animation, poses, colliders, horloge, degats, combos de base,
  garde/roll et sync. `EpicFightBridge`, `EpicFightCombatDefaults`, les mixins,
  `EpicFightContentPolicy`, `EpicFightControls` et `EpicFightGuiAdapter` cadrent
  son integration. Le miroir `CombatData` n'est pas une seconde autorite.
- Qi/cultivation : attachments sauvegardes, regeneration, charge validee serveur,
  verrouillage de mouvement, recompenses/anti-farm et premieres victoires boss.
  Mutations des commandes Qi reservees aux operateurs. API : `com.murimblock.api`.
- `MurimEntities.TRAINING_OPPONENT` : adversaire invocable Zombie/epee en fer,
  patch, IA et rendu Epic Fight. Pas de spawn naturel, loot, XP ou Qi. Prototype
  technique non equilibre, pas un mob Murim final. `/summon murimblock:training_opponent`.
- Techniques est vide. Styles par technique, quatre M1 configurables, combos
  Murim originaux, clash physique des lames et remplacement total du combat
  vanilla ne sont PAS implementes. Ne pas les presenter comme livres.
- GeckoLib n'est pas ajoute. Deux moteurs de rendu/animation sur une meme
  entite demandent une integration concrete ; aucun convertisseur universel.
- Modele de crabe travaille separement dans le dossier local `crabe`, variante
  `armored-crab-v2` : Blockbench modifiable, huit pattes, deux pinces, carapace
  bleu-vert et pince droite plus grosse. Pas importe dans le mod ; inspecter
  ses fichiers et son rig avant integration, sans publier le dossier local.

## Verifications Et Limites

- Audit du 8 octobre : 126 tests unitaires, 27 GameTests serveur requis,
  smoke graphique reel avec 15 captures, 8 tests Node proxy, selon le rapport
  `CODE_AUDIT_2026_10_08.md`. Ce sont des resultats historiques, pas un rerun
  garanti sur le checkout ou la copie actuellement ouverte dans Claude.
- Deux vrais clients, latence, tracking tardif, equipement, interruptions,
  mort/reconnexion et dimensions restent a valider. Le solo ne les remplace pas.
- Cloud existant : dernier onboarding connu bloque HTTP 403 sur NeoForge.
  Scripts prepares ; acces et build de cet environnement distant a recontroler.
- Diagnostics amont Epic Fight encore connus : sous-titres, air_slash,
  WaveyCapes optionnel et service web. Pas de compatibilite JEI/EMI garantie.
- Filtrage des anciens items Epic Fight potentiellement destructif : tests sur
  copie jetable et sauvegarde prealable, jamais migration globale d'un monde.
- Les skills Minecraft sont des aides, pas une preuve d'execution ou une
  permission de mettre a niveau Minecraft. Leur validateur generique ne couvre
  pas notre source set `src/gameTest` ; utiliser les checks de `AGENTS.md`.

## Suite Proposee, Pas Encore Une Alpha Validee

1. Confirmer cwd, branche, diff et lecture de ce handoff, sans ecrire de gameplay.
2. Definir avec Enzo le contenu minimum de l'alpha ; aucune date ferme promise.
3. Valider le socle GUI/combat et les transitions a deux clients avant extension.
4. Choisir un premier vrai mob et une seule attaque originale, avec modele,
   texture, rig, pivots, clip, comportement et contact/deduplication serveur.
5. Styles/techniques et quatre M1 : conception puis tranche testable via Epic
   Fight, pas une deuxieme boucle de degats. Clash des armes en dernier.

## Lire A La Demande

`docs/README.md` est l'index. Details : `EPIC_FIGHT.md`, `ARCHITECTURE.md`,
`gui/TAB_LAYOUT.md`, `ANIMATION_PIPELINE.md`, `MOB_CREATION_GUIDE.md`,
`ADDON_API.md`, `QI_REWARDS.md`, `CLOUD_IMPORT.md`. Les exemples de mobs et
`DATA_DRIVEN.md` decrivent aussi du futur, pas uniquement des fonctions actives.
Un seul agent ecrit a la fois dans le checkout partage. Faire un relais court,
preserver les edits de l'autre, demander les autorisations de compte a Enzo.
