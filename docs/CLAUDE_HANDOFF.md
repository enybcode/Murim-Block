# Reprise Murimblock Pour Claude

> Point de reprise actualisé le 13 octobre : [là où je me suis arrêter](../l%C3%A0%20o%C3%B9%20je%20me%20suis%20arr%C3%AAter/README.md).
> Ce fichier conserve l'historique du 12 octobre : les passages « Techniques vide »,
> « crabe non intégré » et les anciens SHA/protocoles ne décrivent plus l'état courant.
> La tranche garde/K.O./premier art est publiée dans `84b2cac`, protocole 10, PR #5.
> 13 octobre au soir : art Azure Cloud Sword complet (passif, variantes, secret, quêtes, maîtrise, évolutions, finish),
> protocole 11, voir `docs/combat/styles/azure-cloud-sword.md`. 14 octobre : Cloud Step Slash tenu pour viser puis relâché (garde de Zoro, images rémanentes), protocole 12 ; coupes retardées de Thousand Drizzle, protocole 13. Mountain Bandit en jeu (`MountainBanditEntity`, patch Epic Fight humanoïde, modèle 3D à 5 tenues (`scripts/animation/bandit-3d.cjs`, mesh `entity/mountain_bandit`), Dao de bandit 3D tenu tranchant en bas, houppe à pendule (`BanditDaoRenderer`), lâché 1 fois sur 100, coupes Murim tranchant en tête, toutes parables (`MountainBanditGameTests`), aussi fréquent que le zombie dans les biomes Murim). Bandit Chief, mini-boss sans apparition naturelle (œuf, /summon) : modèle 3D ×1,3, grande hache à deux mains, fendoir, fauchage et saut écrasant à onde de choc, tous parables, barre de boss (`docs/mobs/bandit-chief.md`). Idles propres au bandit et au chef (`mob-idles.cjs`) et visages vivants par texture, 10 expressions par atlas (`client/MobFaces`).; croissant à 7 blocs, suspension (case qui brille), plongée, explosion et exécution, protocole 14 ; validé à deux vrais clients (scripts/mp/run-mp-tour.sh). Checkout de ce PC : `C:\Users\enzob\Documents\Murimblock`.
> 14 octobre : Poing Long (`docs/combat/styles/long-fist.md`), premier art à mains nues : rouleau, M1 d'Epic Fight, passif Steady Breath, garde à mains nues, Tiger Step Punch / Rapid Fists / Whirlwind Kick, secret Inch Force, quêtes, évolutions, finish Final Palm, bouton de style principal dans Techniques ; protocole 15. Lieu du rouleau à décider.
> 14 octobre au soir : cultivation (`docs/cultivation/qi-cultivation.md`) : méditation refaite (C, lotus, aura, Qi Max condensé), percées animées, monde intérieur `murimblock:inner_world` (île à son image, corps laissé assis, démon du cœur à chaque grande percée, missions de l'âme du vieux maître), perception du Qi (G), élixirs, bonus de royaume ; protocole 16, ancienne charge de Qi (R) retirée. Tour client : runCultivationTour.
> 14 octobre : Sensei Yun (`AzureSenseiEntity`, `docs/mobs/azure-sensei.md`), maître du Nuage Azur dans la salle des Trois Purs du temple de la Mer de Bambous : modèle 3D (`sensei-3d.cjs`), idle et marche (`sensei-motions.cjs`), épée Azure Cloud à houppe balancée (`TasselSwordRenderer`), neutre, clic droit = défi sans manuel (14/10 : le manuel ne se gagne qu'en le battant, toujours lâché ; seule source). Boss difficile (300 PV, techniques de l'art, seconde phase sous 50 %, récupère seul), barre de boss bleue à nuages, lâche son épée à 0,5 % et toujours le manuel, +30 Qi Max à la première victoire. Manuels consommés à la lecture. `runSenseiTour`. Hameau des pêchers envahi (3 hommes-crabes et leurs crabes, maladie). Les sites des mondes anciens reçoivent leurs habitants à la première visite (`SiteResidents`).
> 14 octobre au soir : **carte Murim** (`docs/biomes/murim-map.md`, `world/map`) : bulle de 150 000 × 150 000 blocs aux régions fixes (centre, nord, ouest, sud, est), anneau d'océan de 10 000 blocs, puis vanilla ; région TerraBlender retirée (surfaces dans les règles par défaut) ; point d'apparition aléatoire dans la bulle (nouveaux mondes). Deux biomes du nord : Tianshan (épicéa de Schrenk, lotus des neiges, laîche) et Kunlun (roche du Kunlun, jade blanc de Hotan, glace). Techniques : dégâts selon l'arme et le Qi, pas de one-shot (`BurstGuard`) ; Sensei Yun seule source du manuel (à battre), Poing Long sur le chef et dans les tonneaux des sites ; dialogue du vieux maître et épreuve du démon (protocole 17).
> 15 octobre : **avant-poste de l'Alliance** (`alliance_outpost`, `AllianceOutpost`, `docs/structures/alliance-outpost.md`), septième site : fort de 53 × 47 blocs (murs, 3 tours, tour de guet à feu de signal, salle, caserne, écuries, entraînement, infirmerie, avis, puits, salon de thé), uniquement au centre ou à moins de 1 500 blocs d'une frontière de régions (`Site.allowedAt`, `MurimMap.centreOrFrontier`) ; 4 villageois nommés et 2 chevaux en attendant les soldats ; tuiles azur (`MurimBlocks.AZURE_ROOF_*`). Fleurs et herbes Murim tenues à 8 blocs des sites.
> 15 octobre au soir : **arène de l'Alliance** (`alliance_arena`, `AllianceArena`, `docs/structures/alliance-arena.md`), huitième site d'après la maquette validée : cour de 73 × 73, leitai de 27 × 27 à 4 sceaux d'invocation, tribunes, 2 salles des troupes, pavillon des juges ; peu commune (structure set 40/16), centre ou frontières ; 3 coffres `chests/alliance_arena` + cache ; le capitaine n'apparaît que là (`AllianceArena.captain`, sergent en attendant `peak_captain` de la session « design »). `SitePlan.MAX_REACH` 60.
> 15 octobre, nuit : **tigre** (`murimblock:tiger`, `docs/mobs/tiger.md`) : modèle à 25 cubes et pelage pixel art (`tiger-model.cjs`), animations Blockbench (`tiger-animations.cjs`) exportées vers Epic Fight (`tiger-epicfight.cjs`, squelette propre à 15 os), loup × 2 (16 PV, 8 dégâts), agressif, région du centre seulement, cinq attaques parables dont un bond, peau et griffe à 20 % (icônes `tiger-item-icons.cjs`). Tests `TigerGameTests`, film `runTigerTour`.
> 16 octobre : **grand camp de bandits** (`bandit_camp`, `BanditCamp`, `docs/structures/bandit-camp.md`), neuvième site : palissade ronde de 85 blocs, fossé, porte et tours, grande salle (siège vide + tambour de guerre pour le futur boss), place du feu, dortoirs, tentes, butin, cages, enclos, cercle d'entraînement ; 30 Mountain Bandits (`BanditCamp.POSTS`) ; dans toutes les régions, habillé selon la région de son centre (`BanditCamp.theme`) ; structure set 128/48 ; coffres `chests/bandit_camp`. Boss pas encore fait.
> 16 octobre : **régions tirées au hasard** (`MurimMap.Layout`, `MurimMap.configure` depuis `MurimSurface` au démarrage du serveur, `MurimMap.heart`) : chaque monde distribue les quatre régions extérieures dans des quartiers tournés au hasard ; cinq régions de même surface (`CENTER_SHARE` = racine(0,8/pi)). Sans monde (tests unitaires), disposition classique. Ne plus supposer « nord = −z » : utiliser `MurimMap.heart(région, part)`.
> 16 octobre : **Bandit Warlord** en jeu (`BanditWarlordEntity`, `docs/mobs/bandit-warlord.md`) : boss des camps, 400 PV, poings (Poing Brise-Montagne : crusher, earth breaker à onde de choc, charge, uppercut qui projette), barre pourpre. Le camp compte ses 35 (30 bandits + 5 Bandit Chiefs, barre « Bandit Camp: n / 35 slain », `BanditCampCount`) ; au dernier mort il tombe du ciel en météore (`WarlordEntrance`), cinématique pour les joueurs du camp (`client/WarlordCinematic`, protocole 21), le camp devient un cratère pour de bon (`WarlordCrater`). Visite `runWarlordTour`.
> 16 octobre : **Art du Bâton du Voyageur** (`traveller_staff`, `docs/combat/styles/traveller-staff.md`, fiche d'Enzo) : bâton du voyageur à deux mains (`MurimItems.TRAVELLER_STAFF`), passif Third Resonance (3e M1 ×2), Vertical Strike, Flurry of Points, Spinning Rampart tenu (bloque tout ce qui vient de face, 10 Qi + 5/s, 3 s, recharge à la fin) ; animations ×1,5 (`staff-art.cjs`), parchemin qui enseigne les trois (lieu à décider) ; protocole réseau 20. Tests `TravellerStaffGameTests`, tours `runTravellerStaffTour` et `runStaffTour`.
> 16 octobre : **Art du Bâton du Voyageur** (`traveller_staff`, `docs/combat/styles/traveller-staff.md`, fiche d'Enzo) : bâton du voyageur à deux mains (`MurimItems.TRAVELLER_STAFF`), passif Third Resonance (3e M1 ×2), Vertical Strike, Flurry of Points, Spinning Rampart tenu (bloque tout ce qui vient de face, 10 Qi + 5/s, 3 s, recharge à la fin) ; animations ×1,5 (`staff-art.cjs`), parchemin qui enseigne les trois, vendu par le marchand de l'Alliance au prix du Poing Long (40 taels) ; protocole réseau 20. Tests `TravellerStaffGameTests`, tours `runTravellerStaffTour` et `runStaffTour`.
> 17 octobre : **Épée des Sept Étoiles** (`seven_stars_sword`, `docs/combat/styles/seven-stars-sword.md`), art libre de milieu de partie (Qi Condensation), au fleuret seulement (Dawn Day) : passif Star Marks (une étoile par coup, 7 éclatent), Shooting Star, Dipper Steps, Orbiting Stars, Constellation Link, Falling Star Rain, secret Polaris Riposte, ultime Seven Stars Sword Array, finish Extinguished Stars ; effets lumineux Photon (`client/SevenStarsFx`, `SevenStarsTechniqueFx`) ; Fleuret des Sept Étoiles et manuel (`SevenStarsItems`, source à décider) ; protocole 23. Tests `SevenStarsGameTests`, film `runSevenStarsTour`. Filtre de GameTests : `-PgameTestFilter=mot1,mot2` (`testing/GameTestFilter`).
> 18 octobre : **Lance du Seigneur Conquérant** (`overlord_spear`, `docs/combat/styles/overlord-spear.md`, conception d'Enzo), art intermédiaire à la lance (Qi Condensation), rouleau donné à la maîtrise 50 de la Lance du Pic Céleste : passif Conqueror's Weight (5 charges, bélier ×1,8), Tyrant's Thrust tenu (rayon de Qi), Earth-Splitting Sweep, Falling Mountain, Overlord's Charge, Hundred Battles Rampage, secret Breaking the Cauldrons, ultime Conqueror Descends (lance géante GeckoLib), finish Overlord's Verdict ; arme Lance de la Ruine (`SPEAR_OF_RUIN`) ; `martial/OverlordSpear`, protocole 30. Tests `OverlordSpearGameTests`, film `runOverlordTour`.
> 17 octobre : **Sabre du Tonnerre du Ciel Pourpre** (`thunder_saber`, `docs/combat/styles/purple-heaven-thunder-saber.md`), art libre intermédiaire à la grande épée (validé par Enzo) : passif Rolling Drums (tambours de tonnerre, hyperarmure, électrisé), Thunder Descends, Rolling Thunder, Heaven's Drum (tenu), Severing Flash (chargé), Nine Heavens' Wrath, secret Heaven Answers, ultime Thunder Sovereign's Descent, finish The Last Thunderclap ; Zhanmadao du Ciel Pourpre massif (`purple-heaven-zhanmadao-model.cjs`), animations à deux mains (`thunder-saber-art.cjs`, solveur de la hache du chef), éclairs en lumière additive + Photon (`client/ThunderSaberFx`) ; protocole 24. Tests `ThunderSaberGameTests`, tours `runThunderSaberTour` (effets) et `runThunderSaberPoseTour` (poses seules).
> 17 octobre : **Cloche d'Or** (`golden_bell`, `docs/combat/styles/golden-bell.md`), art de départ défensif à mains nues, sans passif : forme gardée (peau en or, −40 % de dégâts, 15 Qi + 4/s, la cloche sonne à la sortie), Iron Rebound (peau de fer, renvoie un coup), Rooted Mountain (peau de pierre, immobile, soigne), variantes dorées dans la forme, secret Bell Toll, finish Last Bell ; skin recoloré (`client/GoldenBellSkins`, `PlayerSkinMixin`) ; protocole 24. Moine de la Cloche d'Or (`GoldenBellMonkEntity`, `docs/mobs/golden-bell-monk.md`) au temple de la Mer de Bambous : vend le manuel 40 taels, rend les coups aux poings, forme dorée sous 50 %. Tests `GoldenBellGameTests`, `GoldenBellMonkGameTests`, film `runGoldenBellTour`.

> 18 octobre : **plantes du Murim en jeu** (`docs/cultivation/plants.md`, `world/plant`) : 13 herbes et 30 fleurs en 3D voxels (blocs sauvages figés cuits des modèles GeckoLib, icônes pixel art), apparition dans les biomes de leur région (modificateurs de biomes), graines/bulbes/boutures/rhizomes/spores, plant cultivé à 4 stades animé selon la rareté (`PlantCropBlock`, catalogue `PlantCatalog` à ordre figé), pureté et maturité des herbes, maturité spirituelle sur terre Murim, récolte gardée ou arrachée ; pilules faites des herbes récoltées. Tests `PlantGrowthTest`, `PlantGameTests`, visite `runGardenTour`. Chaudron : prochaine tranche.
> 18 octobre : **four à pilules** (`docs/cultivation/alchemy.md`, `alchemy/`) façon webtoon (GeckoLib, `cauldron-model.cjs`), fabricable ; eau puis herbes dans l'ordre, feu au charbon, écran de feu (soufflet, Qi infusé, jauge et bandes) en 3 étapes ; pureté = herbes + adresse ; surchauffe = résidu + petite explosion ; 7 recettes (Manuel d'alchimie, 2 parchemins rares), 5 nouvelles pilules/décoctions ; pas de nouveau paquet réseau. Tests `AlchemyGameTests`, visite `runFurnaceTour`.
> 18 octobre, nuit : **écran du four à la Minecraft** (cases eau/herbes/charbon/pilules, livre de recettes des seules recettes connues, par pages, écran fermé à la fin) et **24 élixirs** à passifs temporaires faits d'herbes et d'objets vanilla (31 recettes ; Manuel, Traité des élixirs, parchemin `recipe_scroll` ; 7 passifs `AlchemyEffects`). Voir `docs/cultivation/alchemy.md`.
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
- Ni jauge jaune/stamina limitante, ni livres/items Epic Fight exposes (sauf leurs
  ARMES, autorisees depuis le 15/10 a la demande d'Enzo, onglet Murimblock: Weapons), ni
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
  contre 20, seul + escorte) ; structures plus tard. 10 XP, 10 Qi ; butin (14/10) : 1-2 morues, 0-1 saumon, trident
  1 % tué par un joueur (le crabe cuirassé : une morue une fois sur deux, trident 1 % aussi).
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
  les structures : world/structure/kit (BuildKit, RoofTiles). Jeton de la Secte (boussole de jade) vendu par le
  marchand de l'Alliance (64 taels + 32 yuanbao d'or, 16/10 ; monnaie a 4 niveaux : docs/items/currency.md), plus donne a la connexion. Pas dans les mondes plats/tests. docs/structures.
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
