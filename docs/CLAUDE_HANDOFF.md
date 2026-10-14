# Reprise Murimblock Pour Claude

> Point de reprise actualisé le 13 octobre : [là où je me suis arrêter](../l%C3%A0%20o%C3%B9%20je%20me%20suis%20arr%C3%AAter/README.md).
> Ce fichier conserve l'historique du 12 octobre : les passages « Techniques vide »,
> « crabe non intégré » et les anciens SHA/protocoles ne décrivent plus l'état courant.
> La tranche garde/K.O./premier art est publiée dans `84b2cac`, protocole 10, PR #5.
> 13 octobre au soir : art Azure Cloud Sword complet (passif, variantes, secret, quêtes, maîtrise, évolutions, finish),
> protocole 11, voir `docs/combat/styles/azure-cloud-sword.md`. 14 octobre : Cloud Step Slash tenu pour viser puis relâché (garde de Zoro, images rémanentes), protocole 12 ; coupes retardées de Thousand Drizzle, protocole 13. Checkout de ce PC : `C:\Users\enzob\Documents\Murimblock`.

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
- Crab Man (`murimblock:crab_man`, 12/10/2026, local sur `test`) : humain infecte
  du modele `crabe/human-crab-animated-v2` (V6, 87,5 %), armature Epic Fight
  propre (21 joints), 20 PV, armure 8 / tenacite 2, recul (jab `claw_snap` a
  fort impact), Vitesse I dans l'eau, ne brule pas, arrive toujours avec 2 crabes
  cuirasses. Spawn naturel rare pres de l'eau (memes biomes que le crabe, poids 3
  contre 20, seul + escorte) ; structures plus tard. 10 XP, 10 Qi, sans butin.
  Attaques jouees a la vitesse animee (`ATTACK_SPEED_FACTOR` 0, crabe inclus).
  Boule d'eau (12/10/2026, demande d'Enzo, sans particules Minecraft) : clip
  `crab_man/water_ball` (ActionAnimation, vers 4 blocs : 3 a 6, ligne de vue, recharge
  140 ticks). Une grosse boule + 4 petites (`WaterBallEntity`, rendu GeckoLib
  du modele Blockbench `crabe/human-crab-animated-v2/water-ball`) se forment
  dans la pince a 0,3 s et partent a 0,82 s, trajectoire de fleche lente
  (0,8 bloc/tick, trainee 0,99, gravite 0,05, visee calculee). Eclatement selon
  la cible : sol, mur (ou plafond) ou corps (suit la cible). Grosse : 4 degats,
  Lenteur I 3 s, petit recul direct (Epic Fight annule le recul vanilla d'un
  projectile) ; petites : 1 degat, Lenteur I 2 s. Ignore le crab man et ses crabes.
- Biomes Murim (12/10/2026, local, demande d'Enzo) : 7 biomes de la Chine
  feodale (karst de Guilin, plateau de loess, desert du Hexi : grands ; Huangshan,
  mer de bambous de Shu, Jiangnan, vallee des pechers : moyens), sans structures.
  TerraBlender 4.1.0.8 requis (region murimblock:overworld, poids 7). 57 blocs de
  flore et de roche (onglet Murimblock: Nature), reliefs en Java (tours karstiques,
  aiguilles, piliers, yardangs, bambous, pins de falaise, etangs, sous-sol par biome),
  brume shanshui cote client. Fiches, sources et maquettes des futures structures :
  docs/biomes. Donnees generees par scripts/worldgen (generate-nature.cjs,
  NatureTextures.java) ; verification en vrai client : gradlew -PgameTests runBiomeTour.
- Sanctuaire de la Falaise des Nuages (12/10/2026, local) : structure unique, construite
  au premier demarrage (SanctuaryPlacement, sauvegarde murimblock_sanctuary) au coeur des
  Pics de la mer de nuages les plus proches du spawn (4000 blocs), a 64 blocs au moins ;
  le point d'apparition n'est PAS deplace (demande d'Enzo). Le chemin finit au sud par
  une passerelle dans l'axe du portique ; couche sous les toits fermee. Construction repartie sur
  les ticks (SanctuaryPlacement.Job, ticket de chargement, ~50 ms max par tick). Kit commun pour
  les structures : world/structure/kit (BuildKit, RoofTiles). Jeton de la Secte (boussole de jade) donne une
  fois a la premiere connexion. Pas dans les mondes plats/tests. docs/structures.
- Mise a terre PvP (12/10/2026, local) : un coup mortel d'un joueur met K.O. au lieu de tuer
  (combat/knockdown : KnockdownService, KnockdownEvents ; client/KnockdownClient). Ramper,
  V porter/poser, B executer (finish generique, l'art martial aura le sien), releve apres 30 s
  (personne ne peut relever). Execution = mort normale (inventaire au sol). Toute vraie mort
  coute 10 % du Qi maximum pour de bon, plancher 100 (QiData.afterDeath, QiEvents.onPlayerDeath).
  Animations originales Epic Fight generees par scripts/animation (MurimAnimations, mouvements
  Motions ajoutes via INIT_ANIMATOR et MODIFY_PLAYER_LIVING_MOTION). Protocole reseau 8.
  Finish vu en cinematique par ses deux joueurs (client/FinishCinematic, FinishCinematicPayload,
  protocole 9). Tests : KnockdownGameTests, gradlew -PgameTests runKnockdownTour. docs/combat/STYLES.md.
- Garde Murim (12/10/2026, local) : MurimGuardSkill (herite de ParryingSkill d'Epic Fight) dans l'emplacement de
  garde ; parade parfaite (10 ticks) sur toute attaque (fleches, boules d'eau, competences), parade normale 50 %.
- Art de l'Epee du Nuage Azur, 1re tranche (12/10/2026, local) : package martial (MartialArts, MartialData,
  MartialService, Techniques, SectLibrary, MartialManualItem), barre de combat (CombatBarClient, touches 1-9 + 10e
  touche Murim), onglet Techniques, Cloud Step Slash (DashAttackAnimation, MurimAnimations). Protocole reseau 10.
  Tests : MartialGameTests, ParryGameTests, gradlew -PgameTests runMartialTour / runParryTour.
- GeckoLib 4.9.3 et Photon 2.2.8 (Low-Drag, effets/lumieres dynamiques) sont
  des dependances requises depuis le 12/10/2026, a la demande d'Enzo, avec
  LDLib2 2.2.42 et KilaGraph 21.1.0.16. GeckoLib rend la boule d'eau du
  Crab Man (reflets en glowmask, brillent dans le noir) ; Photon fait le flash de
  lumiere dynamique a l'eclatement (`WaterBallLights`, PhotonLights.flash, rien
  si ses lumieres dynamiques sont coupees dans sa config). Deux moteurs
  de rendu/animation sur une meme entite demandent toujours une integration
  concrete ; aucun convertisseur universel.
- Modele de crabe travaille separement dans le dossier local `crabe`, variante
  `armored-crab-v2` : Blockbench modifiable, huit pattes, deux pinces, carapace
  bleu-vert et pince droite plus grosse. Pas importe dans le mod ; inspecter
  ses fichiers et son rig avant integration, sans publier le dossier local.

## Verifications Et Limites

- 12 octobre (local, test) : 130 tests unitaires, 60 GameTests serveur, smoke
  client reel (captures GUI, mobs, boule d'eau, nuit Photon) et visite des biomes
  et du sanctuaire en monde neuf (runBiomeTour), tous passants. Outils : scripts/README.md.
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
