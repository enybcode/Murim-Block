# Combat Rework Plan: Audit, Architecture And Delivery Gates

Audit du 2026-10-04. Document de preparation, pas une implementation.
Les noms de modules proposes ci-dessous ne designent pas du code deja livre.
Cette mission ne modifie ni le gameplay, ni les licences, ni les dependances,
ni les sauvegardes. Aucun commit, push ou merge n'est effectue.

## 1. Perimetre Et Versions Verifiees

Projet IntelliJ : `C:\Users\enzob\Documents\Murim block`, pas le workspace MCreator.
Instructions lues : [AGENTS.md](../AGENTS.md). Les prochaines implementations
restent sur `test`; seul l'accord explicite du joueur autorise un merge dans `main`.
Pour cette mission, la consigne explicite de ne pas commit/push prime.

| Element | Observation dans le depot ou l'outil |
| --- | --- |
| Branche | `test`, suivi de `origin/test`, sans divergence locale observee |
| HEAD et origin/test | `8111362f8a54baaf898bd9cbeb6d12afff04f35f` |
| main et origin/main locaux | `23323b5fae120bd420cf37f75a79a42a0198418a` ; references locales, pas un nouveau fetch |
| Remote | `https://github.com/enybcode/Murim-Block.git` |
| Minecraft | `1.21.1`, plage declaree `[1.21.1]` |
| NeoForge | `21.1.248` ; metadata generee : minimum cette version |
| Java | toolchain et release 21 ; Gradle utilise Temurin `21.0.11+10` |
| Gradle / ModDev | wrapper `9.2.1` / plugin `2.0.144` |
| Parchment | `1.21.1`, mappings `2024.11.17` |
| Tests | JUnit Jupiter via BOM `5.11.4`; source set GameTest dev-only |
| Dependances gameplay declarees | aucune bibliotheque tierce ajoutee; Minecraft/NeoForge et leurs bibliotheques, dont JOML |
| Licence Murimblock declaree | `All Rights Reserved` dans `gradle.properties` |

Sources : [gradle.properties](../gradle.properties), [build.gradle](../build.gradle),
[wrapper](../gradle/wrapper/gradle-wrapper.properties),
[metadata](../src/main/templates/META-INF/neoforge.mods.toml).
Epic Fight, PAL, playerAnimator et GeckoLib ne sont pas installes par ce build.

Modifications deja presentes, a conserver sans les inclure automatiquement
dans une future livraison :

- `src/main/java/com/murimblock/client/QiChargeClientEffects.java` : diff d'espacement dans la declaration de classe.
- `combat/MeleeRules.java`, `combat/SwordExchange.java` et leurs deux tests : fichiers locaux non suivis.
- Dossiers non suivis `GUI_References/`, `Murimblock_Profile_GUI/`, `Murimblock_Profile_GUI_V3_Final/`, `Murimblock_Profile_GUI_V4_AssetKit/`.

L'audit examine le working tree, pas seulement HEAD. Ses resultats de tests
incluent donc les prototypes locaux. Ces fichiers ne doivent pas etre supprimes
ou publies comme consequence implicite de la refonte.

## 2. Etat Reel De Murimblock

### 2.1 Criteres De Preuve

**Branche au jeu** signifie qu'un enregistrement/event/input appelle le code.
**Teste serveur** signifie qu'un scenario headless exerce des entites Minecraft.
**Logique isolee** signifie qu'il manque les adaptateurs de jeu, meme si JUnit passe.
**Non observe visuellement** signifie qu'aucun client graphique ou essai a deux
clients n'a ete execute pendant cet audit. Une capture precedente ou une case
cochee dans un ancien document ne remplace pas cette verification.

### 2.2 Combat, Garde Et Equipement

Racine des fichiers de cette section : `src/main/java/com/murimblock/`.

| Fichier et symboles inspectes | Ce qui existe vraiment | Limite importante |
| --- | --- | --- |
| [Murimblock](../src/main/java/com/murimblock/Murimblock.java), constructeur | Enregistre attachments, reseau, tick serveur, lifecycle, `onAttack`, `onIncomingDamage` | Aucun runtime du package `combat.attack` n'est enregistre |
| [CombatService](../src/main/java/com/murimblock/combat/CombatService.java), `setCombatMode`, `resetCombatMode` | Mode temporaire valide au serveur, feedback, evenement addon | Le mode n'est ni un moteur d'attaque ni un style equipe |
| [MeleeCombatService](../src/main/java/com/murimblock/combat/MeleeCombatService.java), `canUseSwordCombat`, `onAttack` | Epee principale, mode actif, main secondaire vide, vivant/non spectateur, pas d'usage d'item; verrouillage pendant certaines recuperations | Un premier coup autorise n'est pas annule : Minecraft applique encore son attaque, son cooldown et ses degats |
| Meme service, `requestGuard`, `onIncomingDamage`, `tick` | Garde frontale server-side, son/particules, impact/break, ralentissement, arret sprint | Detection via source de degats + cone, pas via contact physique des lames |
| [GuardRequest](../src/main/java/com/murimblock/combat/GuardRequest.java), `matches`, `isLive` | Lease lie au slot et a une copie de l'ItemStack, rafraichi toutes les 10 ticks client, expiration 30 ticks serveur | Pas de sequence de requete d'attaque ni de protocole de combo |
| [MeleeData](../src/main/java/com/murimblock/combat/MeleeData.java), `Action`, `locksAttack` | Etat temporaire IDLE/GUARD/SWING/GUARD_IMPACT/GUARD_BREAK, timestamps synchronises | `CLASH` est reserve, pas une fonctionnalite active; SWING n'a pas de clip/trajectoire associe |
| [WeaponCategory](../src/main/java/com/murimblock/combat/WeaponCategory.java), `of` | Categories par tags/classes; differencie epee, hache, trident, armes a distance, mains nues | Classifier une arme n'installe pas son comportement |
| [CombatProfiles](../src/main/java/com/murimblock/combat/CombatProfiles.java), `BASIC_SWORD`, `basicFor`, `find` | Un profil `murimblock:basic_sword`, lie a la categorie SWORD | Pas de technique apprise/equipee; `basicFor` choisit toujours le profil fixe |
| [CombatProfile](../src/main/java/com/murimblock/combat/CombatProfile.java), `Guard` | Cone, budget Qi, mouvement et delais parametres par profil | Pas de repertoire de coups, animation, degats par coup ou quatre M1 |

La garde actuelle couvre un cone horizontal de 120 degres, exige une ligne
de vue et une source melee directe supportee. Elle ne bloque pas fleches,
chute, magie/explosion ou sources contournant le bouclier. Un block coute
`max(4, damage * 4)` Qi; insuffisance : Qi a zero, 14 ticks de break, le coup
passe. Impact : 4 ticks; SWING : 10 ticks; vitesse de garde : 35 %.

Ces couts existent deja : la garde fait partie du perimetre a remplacer.
Au jalon 6, sa nouvelle politique sera gratuite, meme a zero Qi. Il ne suffit
pas de mettre ses nombres a zero : le constructeur `CombatProfile.Guard`
et `canPay` imposent actuellement des valeurs strictement positives.
Ne pas transformer globalement `QiService` pour obtenir une garde gratuite.
Pendant les premieres tranches, la garde historique conserve ses regles,
tant qu'elle n'est pas explicitement remplacee; aucun nouveau cout n'est ajoute.

### 2.3 Hitboxes Et Noyau Isole

Package [combat.attack](../src/main/java/com/murimblock/combat/attack) :

| Symboles | Contrat observe | Ce qui manque pour le jeu |
| --- | --- | --- |
| `AttackTimeline.phaseAt`, `contactWindow` | Preparation/contact/recuperation, bornes semi-ouvertes, clipping d'un intervalle traverse | Runtime qui avance le temps et detecte effectivement les contacts |
| `AttackDefinition` | IDs du coup/animation, timeline, trajectoire couvrant sa duree | Chargement de fichiers, vitesse, degats, conditions, style et lecture du clip |
| `BladePose.toWorld`, `BladeTrajectory.sample` | Poignee/pointe en blocs, rotation yaw, interpolation lineaire de points | Squelette, socket main/arme, transformation pitch/miroir et bake depuis la pose reelle |
| `BladeGeometry.intersect` | Intersection instantanee de capsules via JOML; protections numeriques/recentrage | Collision balayee entre ticks et collisions lame/corps/blocs |
| `BladeCollider.fromAttack` | Role ATTACKING uniquement dans CONTACT, horodatage commun | Production des poses depuis les animations/equipements de vraies entites |
| `BladeContactDetector.detect` | Contacts instantanes attaque/attaque ou attaque/garde et point d'effet | Pas appele au runtime. Garder les deux branches dormantes avant le jalon 8 |
| `AttackContactResolver.resolve` | Tri chronologique, obstruction avant clash/garde/corps en cas d'egalite; premier impact consomme le coup | Generation des candidats, autorisation serveur, historique inter-ticks et application des degats |

L'ID de consommation est `(UUID attaquant, sequence)`. Le resolver n'est pas
une fonction de degats et ne connait pas les entites du monde. Sa politique
actuelle est **un impact par attaque**, pas une coupe multi-cibles.
Le clipping temporel n'est pas une collision continue. L'interpolation de deux
extremites n'est pas une rotation rigide d'epee; un bake suffisamment dense
ou une evaluation de pose commune sera necessaire.

`MeleeRules` et `SwordExchange` locaux representent une autre approximation :
cibles mutuelles et ecart de 2 ticks, puis impact differe. La recherche des
references ne trouve aucun appel depuis les handlers enregistres. Ne pas les
reactiver : attaquer simultanement ne prouve pas que deux lames se touchent.

### 2.4 Animations, Rendu, Camera Et Deplacements

- [SwordCombatClientHandler](../src/main/java/com/murimblock/client/SwordCombatClientHandler.java) : `onPlayerRender` applique la pose vanilla BLOCK au bras principal; `onHandRender` dessine une main vanilla inclinee, avec recul. Ce n'est pas un clip martial full-body.
- Ce handler utilise l'action synchronisee pour afficher les autres joueurs : le Combat Mode reste prive au proprietaire. Ne pas conditionner le rendu d'un observateur a ce flag prive.
- `onInteraction` reserve Use Item a la garde lorsqu'eligible, y compris devant un coffre; `onMovement` coupe le sprint. Le clic dans le vide ne passe pas par `AttackEntityEvent` serveur : il n'existe pas de vraie attaque autonome air/miss.
- [accesstransformer.cfg](../src/main/resources/META-INF/accesstransformer.cfg) expose uniquement le renderer de main utilise. Pas de systeme de mixins d'animation Murimblock dans la metadata inspectee.
- [QiChargeService](../src/main/java/com/murimblock/qi/charge/QiChargeService.java) : locks movement/jump transitoires, validation vivante, exclusion avec `MeleeCombatService.isBusy`.
- [QiChargeFovHandler](../src/main/java/com/murimblock/client/QiChargeFovHandler.java) anime le FOV pendant la charge; pas de camera de combat, lock-on, root motion ou renderer skeletal.
- Pas de clips/squelettes/moteur d'animation chargeables trouves dans les ressources Murimblock. Le dossier GUI et ses references ne constituent pas des animations de combat.
- Aucun patch d'AI ou d'animation des mobs n'est branche. Une source melee vanilla peut etre bloquee, mais cela ne prouve pas que son arme animee touche la garde.

### 2.5 Interface, Techniques, Qi, Sauvegarde Et Addons

- [MurimblockKeyMappings](../src/main/java/com/murimblock/client/MurimblockKeyMappings.java) : K profil, V mode, R charge par defaut; respecter les touches reconfigurees, pas des codes souris fixes.
- [MurimProfileScreen](../src/main/java/com/murimblock/client/gui/MurimProfileScreen.java), `renderTechniques` affiche une bibliotheque vide et aucun detail selectionne. Pas de selection equipee, slots M1 ou catalogue de techniques. Le joueur 3D est limite a PROFILE. Garder le layout/les ressources existants et la police `murimblock:manuscript` locale a cet ecran.
- [CombatQiHud](../src/main/java/com/murimblock/client/hud/CombatQiHud.java) remplace visuellement XP par Qi en mode combat, 182 x 5 pixels, sans texte numerique. XP non modifie. Ne pas ajouter une grosse barre de combos/guard/stamina par defaut.
- [QiService](../src/main/java/com/murimblock/qi/QiService.java), `QiData`, `QiEvents` : mutations serveur, regeneration passive/charge, recompenses sur mort attribuee au joueur. La future damage source doit conserver le killer/kill credit.
- [QiAttachments](../src/main/java/com/murimblock/qi/QiAttachments.java) et [CultivationAttachments](../src/main/java/com/murimblock/cultivation/CultivationAttachments.java) : codecs persistants, `copyOnDeath`, sync proprietaire. Conserver `player_qi`, `qi`, `qi_max`, `player_cultivation`, `realm`, `stage` et les IDs existants.
- [CombatAttachments](../src/main/java/com/murimblock/combat/CombatAttachments.java) : combat mode sync proprietaire; melee sync general. Pas de serialisation disque des actions. Ne jamais sauvegarder une attaque en cours.
- [CombatEvents](../src/main/java/com/murimblock/combat/CombatEvents.java) : nettoyage login/logout/clone/dimension/server stop et invalidation au tick quand mort/ineligible. Pas de test actuel prouvant chaque interruption dans un vrai reseau; prevoir une annulation immediate a la mort avant le traitement des contacts.
- [MurimblockNetworking](../src/main/java/com/murimblock/network/MurimblockNetworking.java) : protocole `2`, trois payloads C2S (mode, charge, garde); garde = bool uniquement. Pas de requete d'attaque, nonce, ACK ou sync d'un repertoire de styles/clips.
- [MurimblockApi](../src/main/java/com/murimblock/api/MurimblockApi.java) et [CombatApi](../src/main/java/com/murimblock/api/combat/CombatApi.java) exposent Qi/cultivation et lecture/mutation du mode. `CombatModeChangedEvent` existe; aucune API publique de move/style/contact/animation.
- `CombatCommands`, `CultivationCommands`, `MurimblockCommands` fournissent des commandes existantes; leur feedback est en anglais. Quelques messages anglais sont des literals : ne pas en faire une migration de texte hors perimetre pendant cette refonte.

### 2.6 Verification Effectuee Pendant Cet Audit

| Commande | Resultat | Ce que cela ne prouve pas |
| --- | --- | --- |
| `gradlew.bat --version` | Gradle 9.2.1, JVM 21.0.11 | Compatibilite d'une future bibliotheque |
| `gradlew.bat cleanTest test build` | Build reussi; test restaure depuis cache | Une nouvelle execution de chaque test |
| `gradlew.bat test --rerun-tasks --no-build-cache` | Execution effective : 30 suites, 209 tests, 0 echec/erreur/ignore | Rendu client, gameplay complet ou latence |
| `gradlew.bat -PcombatGameTests runGameTestServer` | 14 GameTests requis passes, serveur arrete normalement | Deux vrais clients et leurs packets/input/render |
| `git diff --check` | Aucun probleme signale avant le document | Absence generale de bugs |

Les 209 tests incluent 15 tests des deux prototypes non suivis. Les rapports
de ce working tree ne sont donc pas une preuve qu'un checkout HEAD seul a
exactement 209 tests. Aucune extraction propre supplementaire n'a ete testee
pendant cet audit. Le build signale deux avertissements de deprecation dans
`MurimblockKeyMappings` concernant l'ancien choix de bus d'EventBusSubscriber.
L'archive locale inspectee exclut `SwordGuardGameTests`, mais inclut les classes
non suivies `MeleeRules` et `SwordExchange`, compilees depuis ce working tree.
Elle ne doit pas etre presentee comme le binaire propre de HEAD ni publiee
implicitement. Aucun artefact de ce build n'a ete envoye sur GitHub.

[SwordGuardGameTests](../src/gameTest/java/com/murimblock/combat/SwordGuardGameTests.java)
exerce health/Qi reels : avant/arriere, break, release, changement d'arme/slot,
offhand/mode, chute/projectile, charge, exclusivite attaque/garde, lease,
recul et impacts repetes. Les mock players negocient les canaux NeoForge et
attendent la fin de l'invulnerabilite de login. Ce harness n'ouvre aucun client.
Les tests du noyau geometrie/temps/resolver restent des tests de logique.
La [CI](../.github/workflows/build.yml) prevoit build et GameTest, mais aucun
run GitHub n'a ete relance ou certifie pendant cette mission.

Documentation confrontee au code : [COMBAT_REWORK](COMBAT_REWORK.md),
[SWORD_COMBAT](SWORD_COMBAT.md), [ARCHITECTURE](ARCHITECTURE.md),
[ADDON_API](ADDON_API.md), [DATA_DRIVEN](DATA_DRIVEN.md).
Les guides de direction ne sont pas des implementations de styles/combos.

## 3. Epic Fight : Etude Des Sources

### 3.1 Snapshot Exact Et Compatibilite Limitee

Sources inspectees dans un clone de recherche hors du projet, branche `1.21.1`,
commit **`a78aa24b72e90a9d09f5fd61925e4369d117abaf`** (2026-05-26).
Son [catalogue de versions](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/gradle/libs.versions.toml)
declare Epic Fight **21.17.3**, Minecraft **1.21.1**, Java **21**, NeoForge
**21.1.219**, ModDev **2.0.134**, Parchment **2024.11.17**.

La version publiee examinee par metadata Modrinth est
[21.17.3.1-mc1.21.1-neoforge](https://modrinth.com/mod/epic-fight/version/8HHhJt6i),
publiee le 2026-05-31. Ce n'est **pas** la preuve que le commit inspecte est
exactement la source de ce binaire hotfix. Ne pas melanger leurs API sans
verification. L'API de listing est
[filtrable par MC/loader](https://api.modrinth.com/v2/project/epic-fight/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D).

Les versions MC/Java concordent et le minimum NeoForge du snapshot est inferieur
a celui de Murimblock. C'est une **compatibilite declaree plausible**, pas une
integration testee : aucun lancement combine ou build de l'addon n'a ete fait.
Les tutoriels Forge 1.20.1/EF 20.x ne definissent pas le contrat de ce snapshot.
La [documentation API officielle](https://epicfight-docs.readthedocs.io/API/Starting/)
et son [guide de migration](https://epicfight-docs.readthedocs.io/API/Migration/)
completent la lecture, mais le code epingle prime sur des exemples plus anciens.

### 3.2 Chaine Animation -> Pose -> Contact -> Degats

Les chemins suivants sont relatifs a `src/main/java/yesman/epicfight/` dans
le snapshot ci-dessus. Les liens pointent tous vers cette revision fixe.

| Source inspectee | Mecanisme observe et consequence pour Murimblock |
| --- | --- |
| [AnimationManager](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/animation/AnimationManager.java), `apply`, `readResourcepackAnimation`, validation registre | Registre d'accessors par nom/ID, reload armatures/clips, dependances de skills et validation client. Un ID seul ne rend pas un clip disponible. Ne pas reprendre son invocation reflexive comme format de datapack Murimblock. |
| [JsonAssetLoader](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/asset/JsonAssetLoader.java), `loadArmature`, `loadClipForAnimation`, `getTransformSheet` | Hierarchie et transforms de repos, keyframes par joint, format matrices/attributs, correction des axes Blender. Le serveur charge les chemins de joints utiles aux attaques et le mouvement, pas seulement un timer. |
| [Armature](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/model/Armature.java), `getBoundTransformFor`; `Joint`, `JointTransform`, `Pose`, `TransformSheet` | Compose les transforms dans la hierarchie, interpolation rotations/translations et matrices de pose. Les pivots, bind transforms et sockets font partie du contrat. |
| [AnimationPlayer](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/animation/AnimationPlayer.java), `tick`, `getCurrentPose` | Temps en secondes avance par A_TICK x vitesse, temps precedent/courant et interpolation visuelle. Ce format n'est pas directement le temps en ticks du noyau Murimblock. |
| [ServerAnimator](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/animation/ServerAnimator.java), `playAnimation`, `tick`; `ClientAnimator`, `LinkAnimation` | Serveur evalue la logique/pose; client gere aussi layers, priorites, transitions et motions. Debut/fin/interruption appellent des hooks, pas juste le renderer. |
| [AttackAnimation](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/animation/types/AttackAnimation.java), `bindPhaseState`, `attackTick`, `hurtCollidingEntities`, `getPlaySpeed` | Phases start/antic/preDelay/contact/recovery/end, etats mouvement/rotation/attack. `tick` et `linkTick` n'infligent les coups que cote logique serveur. Clipping sur l'intervalle precedent/courant evite de perdre une phase traversee. |
| [MultiCollider](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/collider/MultiCollider.java), `updateAndSelectCollideEntity`; `Collider`, `OBBCollider`, `MultiOBBCollider` | Echantillonne plusieurs poses entre deux temps; transform du joint + modele + mouvement monde, AABB englobante puis test des volumes. C'est du balayage echantillonne, pas la preuve d'un clash lame/lame continu. |
| [PlayerPatch](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch.java), `attack`; `LivingEntityPatch.attack` | EF reutilise `Player.attack` sous controle de son contexte, neutralise certaines regles vanilla et injecte sa source via mixins. Copier `AttackAnimation` sans ces patches ne reproduit pas les degats d'EF. |
| [MixinDamageSources](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/mixin/common/MixinDamageSources.java); `VanillaEntityEventHooks`, `NeoForgeEntityEvent` | Source enrichie, events incoming/pre/post, reactions/armor/stun. Ne pas copier un reset global d'invulnerabilite : EF le fait dans un contexte avec listes de cibles et traitement specifique. |

Dans `AttackAnimation`, deux collections distinctes suivent les cibles essayees
et effectivement touchees, remises a zero a l'entree de phase. Un hit bloque
ou refuse peut etre marque comme essaye. Cette distinction est utile pour ne
pas reessayer un meme corps a chaque tick. Murimblock doit definir sa propre
cle de contact/phase et ne pas simplement recopier le plafond multi-strikes EF.

### 3.3 Styles, Combos, Interruptions Et Reseau

- [ComboAttacks](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/skill/common/ComboAttacks.java) : `executeOnServer` selectionne le move depuis capability + compteur, distingue dash/air/monture, avance le combo, joue serveur puis diffuse aux trackers **et au joueur**. `updateContainer` remet le compteur a zero apres inactivite; des events modifient le compteur.
- [WeaponCapability](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/world/capabilities/item/WeaponCapability.java), `getCurrentSet`, `getAutoAttackMotion` et [Moveset](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/ex_cap/data/Moveset.java) : capabilities/styles lies a l'arme, predicats recevant le patch du joueur, registres et reload. Le style peut dependre du joueur, mais le systeme de quatre slots M1 personnalises n'est pas fourni cle en main.
- `ComboAttackAnimation`, `StateSpectrum`, `EntityState` : permission de poursuivre le combo, mouvement/rotation, skills et inaction evoluent avec les phases. Une interruption appelle `end` et peut produire `AttackPhaseEndEvent` avec son motif d'interruption.
- [GuardSkill](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/skill/guard/GuardSkill.java) : hook de degats, mouvements par categorie, hold et consommation de stamina. Ne pas importer ces couts/skills/stats dans le Qi de Murimblock.
- [EpicFightServerBoundPayloadHandler](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/network/EpicFightServerBoundPayloadHandler.java), `handleExecuteSkill` -> `SkillContainer.requestCasting/canUse` : intention client, selection/execution et checks serveur. Autre voie `handleAnimatorControl` : commande generique d'animation venant du client. Ne pas exposer cette voie comme autorisation des coups Murimblock.
- `SPAnimatorControl` transmet action, animation, entity ID, transition et layer/priority; `AnimationManager` verifie les registres. `VanillaPlayerEventHooks.onStartTracking` synchronise aussi les nouveaux observateurs. La presence de packets ne garantit ni securite de tous les chemins ni alignement temporel sous latence.
- [ActionAnimation](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/animation/types/ActionAnimation.java) utilise la courbe de coordonnees et des sync de position; mouvement local/joueur implique predictions/validation. Murimblock commence sans deplacement root-motion.
- [EpicFightCameraAPI](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/client/camera/EpicFightCameraAPI.java) regroupe TPS decouple, lock-on, zoom, raycasts et rotation du modele. Ce n'est pas indispensable pour un premier skin anime en F5; conserver camera Minecraft/FOV Qi au debut.

### 3.4 Donnees Et Couplage A Ne Pas Sous-Estimer

`gameasset/Animations.java` enregistre `SWORD_AUTO1/2/3`, phases et vitesse de
reference; les mouvements resident dans `assets/epicfight/animmodels/animations/`.
`sword_auto1.json` a ete lu pour identifier les tableaux temps/matrices et joints
Root/Thigh/Leg/Knee; `animmodels/entity/biped.json` fournit le modele/armature
necessaire. Ces ressources n'ont pas ete copiees dans Murimblock.

Le rig EF possede notamment des articulations que le modele cubique vanilla
n'a pas. Un renommage JSON ne retargete pas ce rig : transformations locales,
pivots, bras slim, main dominante, socket outil et root doivent etre adaptes.
Garder la meme sequence sous forme simplifiee peut perdre les flexions de
coudes/genoux. Mesurer le resultat plutot que promettre une fidelite identique.

[SkinnedMesh](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/src/main/java/yesman/epicfight/api/client/model/SkinnedMesh.java),
`PPlayerRenderer` et `RenderEngine` utilisent meshes ponderes, renderer/layers
patches, shaders/config et compatibilites. La branche possede un chemin compute
avec detection de support; ne pas inventer une obligation OpenGL 4.6 universelle.
Shaders, meshes et rig sont aussi des ressources soumises a leur propre licence.

Graphe minimal pour une extraction fidele :

```text
AttackAnimation -> Action/MainFrame/Static/DynamicAnimation + StateSpectrum
                -> AnimationPlayer/Animator + variables/events/properties
                -> LivingEntityPatch/PlayerPatch + capability item/skills/stats
                -> Armature/Joint/Pose/TransformSheet + matrices + clips
                -> Collider + monde + damage sources + hooks/mixins
                -> registres/reload + network + animation accessors
Rendu           -> meshes/skin/armor/item layers + resources + shader/renderer
```

Les [scripts de build](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/build.gradle.kts)
declarent NeoForge/Minecraft, Mixin et de nombreux mods de compatibilite;
plusieurs sont `compileOnly`, d'autres `implementation` (Architectury, bundle
Simply, CreativeCore/PlayerRevive). La metadata de mod exige seulement MC et
NeoForge, et le listing publie n'annonce pas de mods obligatoires additionnels.
Il faut distinguer dependances de compilation, inclusion effective et plugins
optionnels : copier `RenderEngine` tire aussi des imports de compatibilite.
Le JAR API produit par le build ne constitue pas un runtime autonome.

Extraction estimee : maths/pose seulement = travail moyen avec decouplage;
loader/animator/collider = travail eleve; renderer + moteur de degats/skills
complet = tres eleve, proche d'un fork maintenu. Aucun sous-ensemble autonome
n'a ete compile pendant cet audit. La geometrie Murimblock deja testee evite
de remplacer son noyau juste pour imiter la forme des classes EF.

## 4. Licences : Deux Decisions Separees

### 4.1 Code

Le [LICENSE du snapshot](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/LICENSE)
est GPLv3; sa notice mentionne v3 ou ulterieure. Une reprise distribuee
implique notices, modifications identifiables, licence applicable et source
correspondante. Une combinaison derivee peut imposer la GPL a l'ensemble
concerne; mettre les copies dans un sous-package ne resout pas cela.
La declaration Murimblock All Rights Reserved ne permet pas de livrer
silencieusement une telle reprise comme du code proprietaire.

Le [README licence](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/README.md#license)
annonce aussi des conditions de redistribution (ressources originales,
renommage, attribution et licence de retour sur les modifications), ainsi
qu'une formulation restrictive pour les forks publics. Ne pas trancher ici
les possibles tensions avec GPLv3 : obtenir clarification des titulaires et
validation de la strategie de licence avant une reprise publique. Ce document
est une analyse technique des textes, pas un avis juridique.

### 4.2 Animations Et Autres Ressources

[LICENSE-ASSETS](https://github.com/Antikythera-Studios/epicfight/blob/a78aa24b72e90a9d09f5fd61925e4369d117abaf/LICENSE-ASSETS)
reserve les ressources dans `src/main/resources/assets`, dont animations,
modeles, meshes, sons, textures, shaders et bibliotheques natives. La licence
demande une permission ecrite avant copie, modification, distribution ou usage
separe. **Aucune permission de ce type n'a ete fournie pour Murimblock.**
La GPL du Java n'autorise donc pas a embarquer les clips/rig EF dans notre mod.
Convertir un clip en PAL, rebaker ses trajectoires ou le renommer ne supprime
pas ses droits. Ne pas utiliser le chargement depuis un JAR present chez le
joueur comme contournement pour un moteur autonome.

Demande de permission a preparer : clips precis et revisions, rig/bind-pose
necessaires, droit de retargeter/baker, distribution du mod/source/public GitHub,
attribution et utilisation gratuite/commerciale. Attendre un texte qui couvre
ces usages; un lien de telechargement gratuit n'est pas une autorisation.

Le README autorise l'usage du mod dans un modpack et la creation d'addons utilisant
sa dependance. Un prototype addon jouant les ressources via **l'installation
officielle EF**, sans extraction/republication, est une autre voie; confirmer
les termes du projet/addon, notamment commercial, avant diffusion. Ce n'est
pas l'autonomie demandee et ce n'est pas retenu automatiquement.

### 4.3 Voie Legale De Repli

Produire un clip original sur un rig cubique propre (Blender/Blockbench), avec
provenance et autorisation de distribution explicites. S'inspirer de principes
de phases/poses ne veut pas dire recopier code ou keyframes. Des animations
CC0 ou sous licence commerciale appropriee peuvent aussi servir, apres audit
asset par asset et retargeting; aucun pack de ce type n'a ete valide ici.

Pour l'adaptateur de rendu, [Player Animation Library (PAL)](https://github.com/PlayerAnimationLibrary/PlayerAnimationLibrary)
a ete examine au commit **`10e019f89fa25d0cd6f50fb8768586a969106911`**, branche
`1.21.1`, version source **1.1.6**, Java 21, NeoForge de build 21.1.230.
Sa [licence MIT](https://github.com/PlayerAnimationLibrary/PlayerAnimationLibrary/blob/10e019f89fa25d0cd6f50fb8768586a969106911/LICENSE)
permet la reutilisation sous conservation des notices. La
[publication 1.1.6 NeoForge/MC 1.21.1](https://modrinth.com/mod/player-animation-library/version/yjxtkvnD)
existe. Sa declaration NeoForge `[21.1,)` inclut notre version; aucun essai
avec Murimblock n'a encore confirme les mixins, le build ou le rendu.

Fichiers PAL lus : `PlayerAnimationAccess` (layers par joueur),
`PlayerAnimationController` (trigger avec age en ticks, bones/pivots),
`AnimationController` (temps, transitions/speed modifiers), builds core/NeoForge,
metadata et liste de mixins humanoid/player/armor/held item/first-person.
PAL est un lecteur, pas une autorite de degats ni une API de combat/mobs.
Il embarque/utilise notamment MochaFloats 4.1.0 et Javassist 3.30.2-GA; verifier
leurs notices et leur packaging avant integration. Pas besoin d'imposer GeckoLib
pour animer le joueur. Le reader PAL n'accepte pas directement le JSON EF.

L'ancien playerAnimator 2.0.4+1.21.1 existe sous MIT, mais son
[README officiel](https://github.com/KosmX/minecraftPlayerAnimator)
indique l'arret des evolutions et recommande PAL. Ne pas le choisir par simple
habitude. PAL reste un candidat a qualifier au jalon 2, pas une garantie.

## 5. Choix D'Architecture

### 5.1 Comparaison

| Approche | Benefice | Cout reel / limites | Conclusion |
| --- | --- | --- | --- |
| API d'Epic Fight, installation officielle | Skin/rig/combats/animations deja relies; prototype visuel le plus court | Depend de tout EF et de son pipeline, capabilities/skills/updates; remplacer basic attack et proprietes par joueur; eviter double autorite; ressource utilisable dans le cadre addon, pas extractible librement | Option de prototype ou integration volontaire, pas un choix impose |
| Reprise ciblee de composants EF | Autonomie possible sur un sous-ensemble; algorithmes matures | GPL et strategie Murimblock a approuver; assets toujours distincts; graphe de dependances large; maintenance des patches/mixins/chargement a notre charge | Possible apres decision de licence, pas copie de classes opportuniste |
| Moteur Murimblock original, inspire des principes EF | Styles/techniques/4 M1 et sauvegarde a nous; damage authority unique; pas de dependance EF | Runtime, contacts, replication et contenu a developper; qualite des animations a produire/adapter; budget QA important | **Recommandation pour l'autonomie souhaitee** |

Socle recommande : conserver le noyau `combat.attack`, ajouter le runtime serveur
et les donnees Murimblock, utiliser **PAL 1.1.6 comme adaptateur client candidat**
pour les skins cubiques. JOML reste la base maths/geometrie. Valider PAL dans
un spike visible/dedicated-server avant de l'adopter definitivement. Si les
tests de clock/socket ne passent pas, adapter son interface MIT ou choisir
un backend natif cubique limite; ne pas importer tout EF pour masquer le probleme.

La voie initiale souhaitee avec clips EF est conditionnelle a la permission
du chapitre 4 et a un retargeting satisfaisant. Sans cette permission, le meme
moteur commence avec un clip original, pas une copie non autorisee. Remplacer
ensuite un clip ne doit pas remplacer les classes de degats ou les saves.

### 5.2 Modules Proposes Dans L'Organisation Existante

| Package / fichiers proposes | Responsabilite |
| --- | --- |
| `combat/definition/MoveDefinition`, `StyleDefinition`, `CombatDefinitionLoader` | Definitions immuables et validation Codecs; IDs namespaces, pas de code arbitraire provenant d'un datapack |
| `combat/style/StyleResolver`, `CombatLoadoutData`, `CombatLoadoutService` | Technique equipee -> style compatible; droits serveur et exactement quatre slots de moves |
| `combat/runtime/CombatRuntime`, `ActionInstance`, `CombatClock`, `ActionTransitions` | Un proprietaire de l'action, phases, sequence, snapshots, buffering et interruptions |
| `combat/animation/PoseSampler`, `RigDefinition`, `WeaponSocket`, `ClipManifest` | Evaluation pure/deterministe partagee, rig propre, conversion/bake, provenance et hash |
| `combat/contact/WorldContactSampler`, `ContactBatch` | Broadphase AABB, trajectoire balayee, corps/blocs; tous les candidats d'un intervalle commun |
| `combat/damage/CombatDamageAdapter` | Unique point d'application, source avec attaquant, regles vanilla pertinentes et events NeoForge |
| `combat/action/GuardAction`, `ActionCostPolicy` | Garde/interceptions de melee, transitions et extension de cout gratuite par defaut |
| `client/combat/MurimAnimationBackend`, `PalAnimationBackend`, `CombatAnimationClient` | Rendu uniquement, animation du skin/armor/arme et interpolation des snapshots |
| `network/AttackIntentPayload`, `ActionSnapshotPayload`, `CombatLoadoutPayload` | Intention C2S limitee, ACK/snapshot S2C, validation des versions/revisions |
| `api/combat` | Extension additive : snapshots, enregistrement defs/evenements autorises; garder les methodes actuelles |
| `data/murimblock/combat/{moves,styles}/`, `assets/murimblock/animations/` | Balance autoritaire serveur vs clips visuels; manifeste/bake commun distribue des deux cotes |

Pas de second mod de gameplay necessaire. Une lib de rendu peut rester une
dependance ou etre packagee conformement a sa licence : cela ne lui confie
ni techniques, ni Qi, ni damage authority. Les addons n'utilisent pas nos
packets/attachments internes, ni les types PAL comme contrat public.

### 5.3 Contrat Des Styles Et Quatre M1

`WeaponCategory` decide la famille et la compatibilite; le material influence
les attributs, pas l'art martial. `StyleResolver` utilise la technique equipee
**cote serveur**; la relation technique -> style est une nouvelle fonctionnalite,
pas un champ existant a deviner dans `CombatProfile`.

Une definition de move possede : ID, clip et revision, rig/socket, vitesse
positive bornee, degats de base/modifier, windup, fenetre(s) de contact,
recovery, regles de rotation/mouvement/cancel, hit policy et limite de portee.
Commencer par une seule fenetre et un impact. Les quatre slots ne signifient
pas que toutes les armes ont quatre animations hardcodees.

Chaque joueur a un loadout de **quatre IDs ordonnes** parmi les moves autorises
par son style. Duplications autorisees pour permettre un petit style initial;
pas de slot null. Valeurs initiales = quatre moves/defaults valides. Un seul
buffer M1 suivant, seulement dans la fenetre de chainage du move actuel;
sinon rejet. Index cyclique 0..3, reset apres timeout de combo, interruption,
changement de style/equipement. Le serveur choisit le slot courant apres une
intention M1 : le client ne soumet ni degats, ni animation arbitraire.

Deux joueurs avec la meme epee peuvent donc avoir style/loadout differents.
Ne pas stocker cette selection dans une capability globale partagee d'item.
Ajouter un nouveau style avec des moves existants doit demander des donnees,
pas un switch Java dans le moteur. Un nouveau type de rig/action exotique
peut demander un adaptateur : l'extensibilite n'est pas une promesse universelle.

Nouvelle attachment persistante versionnee pour technique/style/loadouts,
owner-sync, `copyOnDeath` et defaults pour anciens saves, sans modifier Qi/cultivation.
Schema propose : technique active, style actif, revision, dictionnaire
`styleId -> quatre moveIds`; conserver les configurations des autres styles
quand le joueur change sa technique. Les droits proviennent d'une politique
serveur de techniques apprises/accordees, pas d'un bool envoye par le GUI.
Conserver les IDs inconnus dans les donnees si leur addon est absent, mais les
rendre inactifs; default valide explicite pour jouer. Sur update/load/reload,
valider droits, categorie, quatre slots, IDs et revision. Modifications atomiques
au repos; rejeter un packet stale plutot que perdre la configuration d'un joueur.

### 5.4 Horloge, Pose Et Collision

Une horloge monotone serveur commune par session, plus dimension et generation
de vie du joueur. Sequence attribuee par serveur; nonce client deduplique seulement
l'intention. Ne pas ordonner des contacts avec l'age local de chaque attaque.
L'action conserve une definition/snapshot immuable jusqu'a sa fin.
Le snapshot transporte aussi `serverNowTick` et un ID de session. Le client
maintient une correspondance avec ses ticks locaux par handshake/echanges de
temps et corrections periodiques bornees; ne pas assimiler `getGameTime()`
de sa dimension au compteur global serveur. Mesurer l'offset/RTT et l'erreur
de phase, remettre la correspondance a zero en reconnexion/changement de monde.
Pas de rewind/lag compensation des collisions dans la premiere implementation :
l'attaque commence au tick d'acceptation serveur, avec retour explicite au client.

Pour une vitesse figee `s` : `clipAgeTicks = (serverTick - startTick) * s`.
Pose, root visuel, trajectoire, contact et recovery utilisent cet age unique;
convertir une seule fois les secondes d'un import EF en ticks. Les transitions
de blending ont des durees explicites et ne redemarrent pas un timer de degats.
Une modification d'attack speed en cours n'affecte pas le coup deja accepte;
elle s'applique au suivant. Le ralentissement serveur ralentit animation et
contacts ensemble, pas selon une horloge murale independante.

PAL a son propre compteur et ses speed modifiers : un `trigger` a age zero
a la reception n'est pas suffisant. Adapter seek/age d'entree, corrections et
interpolation depuis le temps accepte, en verifiant la convention de vitesse.
Pas de MoLang dependante de donnees client, d'IK ou de root-motion gameplay
dans les premiers clips de contact. La geometrie serveur ne lit jamais le
renderer client ni une position de pointe envoyee par le joueur.

Source de verite animation : clip/rig/socket versionnes, originaux ou autorises.
Exporter depuis le meme contenu le clip visuel et la trajectoire de poignee/pointe
en blocs pour le serveur. Tester les matrices/socket a plusieurs ages, yaw,
main gauche et slim; appliquer tout retargeting **avant** le bake. Le blend
affectant la lame doit etre reproduit dans la pose de collision ou termine
avant CONTACT. Camera/look/layers cosmetiques ne doivent pas deplacer le socket
de collision pendant une phase active. Resource packs cosmetiques ne changent
pas les hitboxes et doivent etre controles pour eviter une representation trompeuse.

Au serveur : clipping de [age precedent, age courant), broadphase englobant
l'intervalle, echantillonnage adaptatif borne puis narrowphase avec JOML,
corps et blocs. Tester les mouvements rapides/tunnels entre samples et affiner
le temps de premier contact. Ce n'est pas un claim de CCD exact. Ne pas valider
uniquement une intersection a la fin du tick ni la cible du crosshair.

### 5.5 Degats, Transitions Et Autorite

- Le serveur valide vivant, dimension, categorie/style/loadout, cooldown/phase, equipement, spam et nonce. Une prediction client est cosmetique et retractable; initialement, preferer ACK serveur.
- Pour une arme geree par le nouveau runtime, annuler le chemin d'attaque vanilla depuis client **et serveur**, y compris packets directs. Au serveur, distinguer suppression d'un ancien packet vanilla et intention custom : ne pas creer deux attaques pour un clic.
- M1 doit pouvoir produire un swing manque sans cible. Mining/interaction de bloc ne deviennent pas automatiquement une attaque. Regle recommandee : en posture combat, M1 = strike; en exploration devant un bloc, M1 = mining; melee d'entite geree = custom dans les deux postures. V n'est plus un fallback vers degats vanilla. A confirmer au test du lot, pas un blocage de cet audit.
- Axes/tridents/ranged/unarmed restent historiques pendant le lot epee. La suppression du combat vanilla global est progressive par categorie, pas une suppression aveugle de `Player.attack`, du mining ou des projectiles.
- `CombatDamageAdapter` applique une seule fois un contact accepte; ledger d'ActionId/contact ou phase+cible selon la hit policy. Un block/rejet consomme aussi le contact prevu, sans nouvelle tentative chaque tick. Le premier lot consomme toute l'attaque au premier impact; etendre multi-hit seulement avec politique et tests dedies.
- Ne pas appeler `Player.attack` depuis un event puis reinfliger manuellement des degats. Ne pas restaurer la vie apres un hit pour simuler un block/clash. Ne pas mettre globalement `invulnerableTime` a zero. Specifier et tester les i-frames contre attaques distinctes au jalon 4.
- Degats passent par la pipeline serveur Minecraft/NeoForge, avec attribution du joueur, armure/resistance, absorption, PvP et teams/friendly fire; definir explicitement enchantements, durabilite, knockback, crit et sweeping. Le cooldown vanilla ne doit pas s'ajouter une seconde fois au move. Tester kill credit/recompenses Qi et equipement break.
- Le snapshot de depart comprend slot, arme/categorie, components pertinents, main, style/loadout revision, vitesse/degats. Un changement de durabilite provoque par notre propre impact ne doit pas etre interprete comme un swap hostile. Un slot/item/components modifie par l'exterieur annule avant le prochain contact et ne supprime pas la recovery deja due.
- Une requete style/technique/loadout pendant WINDUP/CONTACT/RECOVERY est rejetee avec motif anglais. Un equipement force par inventaire/addon/mort annule le contact restant et le buffer; pas de nouveau coup gratuit. Garder les tests de slot identique et swap avant tick.
- Garde ne peut pas etre offensive en meme temps : attaque acceptee baisse la garde; impact/stun/recovery bornent sa relevee. Release retire la protection sans effacer une recuperation. Pour la garde nouvelle, cout par defaut zero; extension `ActionCostPolicy` non mutante par defaut, sans activer Qi/stamina.
- Interruption par mort/logout/dimension/respawn invalide generation et actions, supprime buffers/leases/modifiers, envoie STOP si des observateurs existent. Paquets tardifs d'une ancienne generation ignores. Quitter la posture ne raccourcit pas un coup/recovery : differer le changement jusqu'au repos ou conserver l'etat, avec regle unique testee.
- ActionSnapshot S2C : identite serveur/generation, dimension, action/style/move, startTick, vitesse, revision et motif de stop. Owner + trackers, snapshot en StartTracking, stop/reset en changement de monde. Handshake de manifestes et protocole explicite; refuser les builds/clips incompatibles plutot que jouer un coup invisible.

## 6. Plan Par Jalons

Chaque jalon se livre separement sur `test`, avec checks et validation humaine
en jeu avant `main`. Les tests a deux clients commencent au socle : ils ne
sont pas reportes uniquement au jalon 7. Ne pas activer `BladeClash` ou la
garde geometrique des lames avant le dernier jalon.

### Jalon 1 : Isoler Les Responsabilites Existantes

**Modules** : `Murimblock`, `MeleeCombatService`, `CombatService`,
`SwordCombatClientHandler`, `CombatEvents`, tests et docs.
Identifier les points proprietaires input/etat/garde/degats/rendu; introduire
une frontiere runtime et une matrice de routage par categorie, sans suppression
globale. Conserver categories, Qi/cultivation/API/HUD/saves et logique isolee.
Ne pas toucher aux prototypes non suivis; les documenter comme dormants.

**Dependance** : baseline reexecutable, aucune lib imposee.
**Risques** : doubles subscriptions, desactivation mining/right-click, inclusion
des fichiers locaux, inversion de la distinction mode/eligibilite.
**Gate jeu** : epee/garde historique, K/V/R, coffre/offhand, charge, hache,
bow, mains nues restent identiques. Checks unit/build/GameTest avant et apres.

### Jalon 2 : Socle Animation Et Combat

**Modules** : nouveaux `combat/runtime`, `combat/animation`, `client/combat`,
snapshot S2C/StartTracking, `CombatEvents`; `build.gradle`/metadata uniquement
si le spike PAL est retenu. Pose sampler/shared clock, backend client et
generation d'actions; premier preview sans degats, autorise par serveur.
Conserver renderer vanilla/skin/armor/camera; remplacer uniquement la couche
de pose du preview, pas les coups ou la garde existante.

**Dependance** : PAL 1.1.6 candidat + notices/packaging transitive verifies,
clip original de test, aucun asset EF sans permission. Pas de root motion.
**Risques** : layers et bras slim, client classes sur dedicated server, compteur
PAL decale, double main first-person, blend/socket incoherent.
**Gate jeu** : skin/torse/bras/jambes reels animes F5, bras gauche/droit,
armure alignee, retour propre a idle; autre joueur voit la meme phase,
join tracking au milieu et reset dimension/logout. Dedicated server boot.

### Jalon 3 : Premier Style Jouable

**Modules** : `StyleDefinition`, `MoveDefinition`, loader restreint, runtime,
`AttackIntentPayload`, routage M1 et backend; donnees `basic_sword`.
Installer un style d'epee avec un move au debut, puis un petit enchainement;
buffer/etats bornes. Supprimer attaques/cooldown/swing vanilla **pour cette
route geree seulement**, meme si V est off; pas de seconde pipeline de degats.
Les autres familles restent temporaires vanilla. Pour etre jouable, connecter
la version minimale des contacts/degats du jalon 4, pas un hit au crosshair.

**Dependance** : socle J2 + permission ecrite EF pour la voie desiree, puis
retargeting/bake; a defaut clips originaux. Si les droits manquent, le sous-lot
"style avec animations EF" est bloque, pas pretendument realise avec autre chose.
**Risques** : preview presente a tort comme gameplay, input doublonne, droits,
rig/sockets, technique absente et choix de profil arbitraire.
**Gate jeu** : swing a vide, preparation/recovery lisibles, hit/miss distincts,
plusieurs attaques sans degats doubles, pas de voie vanilla pour l'epee geree;
deux clients observent le coup. Aucun cout Qi nouveau, aucun clash.

### Jalon 4 : Contact, Temps Et Degats Fiables

**Modules** : `WorldContactSampler`, `ContactBatch`, `CombatDamageAdapter`,
`AttackTimeline/Definition`, `BladePose/Trajectory`, resolver, GameTests.
Finaliser le bake de pose et clipping speed-scaled, balayage corps/blocs,
ledger multi-ticks, policy d'un impact et effets serveur. Retenir les corrections
numeriques du noyau. Ne pas brancher son detector lame/lame a ce stade.

**Dependance** : premiere route animee J3; maths JOML et pipeline NeoForge.
**Risques** : tunneling, float/world-border, contacts derriere un mur, i-frames,
double cooldown/enchantements, swing animation plus long que la logique.
**Gate jeu** : health change seulement en CONTACT, un impact pour une attaque,
miss/passage rapide/obstacle valides, vitesse lente/rapide synchronisee,
armure/PvP/team/durabilite et kill reward Qi correctement traites. Debug lame
se superpose au rendu en F5; essais a deux clients avec latence controlee.

### Jalon 5 : Styles Et Quatre M1 Par Joueur

**Modules** : `combat/style`, attachment loadout persistante, loader styles/moves,
`CombatLoadoutPayload`, API additive, onglet Techniques et `en_us.json`.
Introduire le petit catalogue de techniques equipees qui manque aujourd'hui,
sans inventer d'un coup tout un systeme de progression. Deux styles d'epee,
quatre slots ordonnes, selection serveur autorisee, buffer/fenetres/combo reset.
Remplacer `basicFor` fixe sur cette route par `StyleResolver`; conserver les IDs
existants et defauts des saves. Pas de redesign general du GUI/HUD.

**Dependance** : J4 et animations/moves catalogues; choix explicite d'une seule
technique active determinant le style pour ce premier lot.
**Risques** : moves non appris, persistance/rights, stale revisions, couplage
item global, GUI qui depasse, changement de technique en plein coup.
**Gate jeu** : deux joueurs meme epee, styles/loadouts et animations distincts;
4 slots modifiables parmi autorises seulement, rejects serveur, relog/death
restore loadout, addon absent/reload sans crash ni coup invisible. Cinquieme
move/style ajoute par donnees sans modifier le runtime. Tous labels anglais.

### Jalon 6 : Actions Utiles, Dont Garde Sans Cout Qi

**Modules** : `GuardAction`, `ActionCostPolicy`, remplacement du volet garde de
`CombatProfile`/`MeleeCombatService`, input, poses, tests de garde et exclusion Qi.
Garde levee/maintien/release/impact/recovery explicites; interruption frontale
des melee supportees, meme a Qi zero. Remplacer la consommation et le break
par manque de Qi de la route migree. Impact/reaction reste un etat, pas une
immunite universelle. Ne pas creer un nouveau budget de stamina pour contourner
la consigne. Esquive/parry time si retenus : sous-lots apres garde, pas obligatoires
dans le premier moteur jouable. Pas encore de clash ou garde physique de lames.

**Dependance** : J4/5, gestion input lease et source/etat serveur; animations
originales/autorisees garde/reaction. Extension cout par defaut gratuite.
**Risques** : anciens checks `cost > 0`, modifier stale, guard+attack, release
effacant recoil, blocages de projectiles/magie par erreur, Qi regen confondue
avec une depense. Preserver couts/recompenses/charge hors refonte.
**Gate jeu** : Qi identique avant/apres block hors regeneration connue, garde
a zero Qi, avant/arriere, release/focus/GUI/timeout, reprise de charge,
pas d'attaque pendant le lock. Remplacer uniquement les expectations historiques
de Qi volontairement rendues obsoletes dans les GameTests.

### Jalon 7 : Stabilisation Multijoueur Et Transitions

**Modules** : runtime/clock, reseau, StartTracking, lifecycle, equips/loadouts,
API et suites de regression. Completer dedup/rejets/ACK, revisions/handshake,
stop/generations, coherence sous lag, logging limite. Conserver synchro privee
Qi/loadout et publique action; ne pas diffuser tout l'inventaire ou les techniques.
Profilage broadphase/sampling et bornes sur nombre d'actions/samples par tick.

**Dependance** : J2..6, deux clients et serveur dedie avec builds identiques,
outil de latence/simulation dev documente sans nouveau service impose.
**Risques** : late packet apres respawn/dimension, replay, sequence reutilisee,
client prediction persistante, reset style/slot esquivant recovery, ecart PAL.
**Gate jeu** : matrice 0/100/200 ms RTT cible et jitter controle quand disponible,
FPS 30/60/144 et ticks serveur ralentis; attaque simultanee sans clash encore,
equipement change avant hit, mort pendant CONTACT, logout/rejoin, dimension,
spectateur/menus/focus, tracking tardif. Logs serveur et captures des deux vues
concordent avec health et ActionId. Ne pas qualifier "stabilise" sur un mock seul.

### Jalon 8 : Clash Physique Des Armes, En Dernier

**Modules** : `BladeContactDetector`, `BladeGeometry`, balayage paire de lames,
`ContactBatch`/resolver, interruptions/reactions et feedback.
Activer seulement maintenant contacts attaque/attaque et attaque/garde physiques;
remplacer la garde conique uniquement pour les attaques avec trajectoires
supportees, garder un adaptateur explicite pour autres sources melee/mobs.
Poses au meme temps monde, roles valides, premier contact obstruction/clash/
garde/corps ordonne dans un batch global; un clash consomme les deux attaques
avant leurs impacts ulterieurs, pas les degats precedemment appliques.

**Dependance** : trajectoires precises + clock + ledger + multijoueur J4/7,
animation de recoil originale/autorisee, mesure de performances.
**Risques** : tunneling lame mince, contacts a trois, ordre des acteurs/packets,
fausse collision a distance, blade inactive protectrice, O(n^2) non borne.
**Gate jeu** : lames touchantes annulent les impacts futurs, lames separees
meme au clic simultane ne bloquent pas; windup/recovery inactifs, reprise a
l'issue du recoil, troisieme attaquant valide, mur prioritaire, main gauche,
sword lengths differents et lag. Ni health restore ni comparaison de cibles.

### Extension Mobs Et Autres Armes

Ne pas annoncer une refonte complete Minecraft apres deux styles d'epee.
Le runtime accepte un actor abstraction, pas uniquement `ServerPlayer`.
Apres la premiere epee stable, ajouter les mobs **par famille** : humanoides
armees en premier, puis non-humanoides (griffes/morsures), chacun avec AI
preparation/contact/recovery propre et adaptateur de rig original. Les boss
restent exclus, ainsi que projectiles/explosions tant qu'un lot ne les cible pas.
Une garde de mob exige une decision d'AI, pas une pose permanente.
PAL joueur ne suffit pas a animer creeper/spider/quadrupede. Les armes hache/
trident/mains nues ont chacune leurs definitions, pas un rebadge de l'epee.

## 7. Estimation Prudente

Fourchettes en **jours-personne de travail concentre**, pas un calendrier ni
une promesse d'execution automatique. Hypotheses : equipe connaissant NeoForge,
versions fixes, deux styles d'epee, petit catalogue, rig cubique, ressources
originales ou autorisees, infrastructure de test disponible. QA manuelle incluse.
Pas de temps garanti pour obtenir une permission; elle peut ne jamais arriver.

| Jalon | Developpement | Creation/adaptation animations | Tests/integration | Incertitude dominante |
| --- | --- | --- | --- | --- |
| 1 Isolation | 2-4 | 0 | 1-2 | Routage et preservation legacy |
| 2 Socle + spike | 8-15 | 2-4 | 3-6 | Clock PAL, layers et dedicated server |
| 3 Premier style | 3-6 | 3-7 si EF autorise; 5-10 si original | 2-4 | Retargeting/droits; reutilise le minimum de J4 |
| 4 Contacts/degats | 7-12 | 2-4 | 4-7 | Sampling, i-frames et enchantements |
| 5 Styles/4 M1 | 8-14 | 5-10 | 4-7 | Deux styles suffisamment distincts et sauvegarde |
| 6 Garde/actions | 4-7 | 2-4 | 3-5 | Transitions et regression legacy Qi |
| 7 Multiplayer | 7-12 | 1-3 | 6-10 | Cas limites reseau et profiling |
| 8 Clash | 10-18 | 3-6 | 7-12 | Sweeps precis, melee collective et cout CPU |

Les budgets dev/tests du minimum J4 employe en J3 se repartissent entre les
deux lots, pas deux fois. Le travail artistique est incremental : pas recompte
integralement a chaque jalon. Un fork GPL complet de renderer/moteur EF,
toutes les armes ou tous les mobs ajoutent des semaines/mois non inclus.
Compter plutot plusieurs mois de travail et d'iterations pour tout ce perimetre
qu'une mise a jour instantanee. Reestimer apres le spike et le premier hit reel.

## 8. Verification Et Definition De Livraison

### Checks Automatisables

- Baseline unit/build/GameTest avec resultat execute distingue du cache; aucune classe `src/gameTest` dans le JAR de production.
- Codecs : unknown IDs, valeurs NaN/inf/vitesse negative, quatre slots, droits/categorie/technique, migrations additives et saves historiques intacts.
- Horloge : bornes exactes, s=0.5/1/2, tick traversant CONTACT complet, aucun double advance, seek tardif, annulation, nouvelle generation et revisions.
- Pose/export : golden transforms de notre rig aux ages connus, socket main/arme, conversion axes/unites, version/hash commun; pas d'assets externes non autorises dans l'archive.
- Geometry : broadphase conservative, corps/blocs/sweeps rapides, segment degenere, presque parallele, world border, permuter ordre des acteurs; lame/lame isolee avant J8.
- Degats : exactement un appel autorise par hit policy, spoof/replay/packets vanilla refuses sur route custom, misses, walls, teams/PvP, immune/rejected hit, equip switch, death/dimension avant contact, killer/Qi reward et durabilite.
- Garde : lifecycle lease et ralentissement propres; gratuite sur la route migree; aucun changement des autres regles Qi/cultivation.
- Reseau : roundtrip/limites payload, serveur n'accepte pas cible/degats/temps/poses arbitraires, cadence bornee, snapshots track/self, builds incompatibles refuses; harness ne simule pas a lui seul le reseau reel.
- Dedicated server : aucun import client initialise dans gameplay, modpack minimal et packaging transitive/notices verifies; unit tests sans serveur client GL.

### Essais Manuels Obligatoires

Arena de test isolee, sauvegarde des vrais mondes avant lot avec persistance.
Deux comptes/clients, serveur dedie, epees controlees, cibles et health logs.
Pour chaque move : video F5 de face/dos/profil, deux bras, classic/slim et armor;
premiere personne jouable sans double main ni mouvement nauseant. Si une capture
reelle n'est pas disponible, laisser le gate visuel non valide.

Verifier hit/miss, preparation, hit au contact, retour idle, combo/buffer,
walk/sprint/jump, mort, menu/focus, food/shield/offhand, slot rapide, changement
de style bloque, relog/dimension, nouveaux trackers, FPS/TPS/latence. Comparer
les deux vues au log serveur, pas seulement la video de l'attaquant.
Comparer Qi/cultivation/XP/UI et recompenses avant/apres. Ne pas ajouter de
nouvelle information HUD juste pour masquer un manque de feedback d'animation.

Une tranche est finie quand son scope, ses limites, ses checks et les preuves
humaines sont explicites. Tests verts = candidat `test`, pas validation `main`.

## 9. Incertitudes Et Blocages Restants

1. **Bloquant pour embarquer clips EF** : aucune permission ressources/rig ni provenance d'autorisation. Repli original disponible sans bloquer le noyau.
2. **Bloquant pour copier du code GPL dans la distribution actuelle** : choix de licence Murimblock et clarification des conditions README. Aucune relicence automatique.
3. **Technique a qualifier** : PAL declare les bonnes versions, mais clock/socket/blend/mixins/transitives pas encore testes avec Murimblock. Le rendu peut differer d'EF apres retargeting.
4. **Version EF** : source 21.17.3 inspectee, binaire 21.17.3.1 reference; aucune equivalence exacte certifiee. Epingle source et artefact correspondants avant un addon reel.
5. **Contenu absent** : techniques equipees, apprentissage, repertoire de moves, loadout persistant, clips et patchs mobs restent a construire. Ne pas vendre ces structures comme disponibles.
6. **Decisions de design a valider en jeu, non bloquantes pour l'audit** : une technique active pour un style; duplicate slots permis; combo cyclique buffer 1; V posture sans fallback vanilla; stance mining/interaction; armor/crit/i-frame policies. Les valeurs de balance restent provisoires.
7. **Validation humaine absente de cette mission** : F5/first-person, animation finale, deux clients et ressenti du lag. Les tests executes ne prouvent pas ces points.
8. **Perimetre final et effort** : tous les mobs non-boss/autres armes demandent des lots de contenu/AI/QA en plus. Pas de migration globale ou clash anticipe pour gagner artificiellement une etape.

## 10. Deuxieme Prompt : Premiere Tranche D'Implementation

Ce prompt cible J1 + le **preview visible de J2**, pas deja tout le moteur.
Un preview synchronise est testable en jeu, mais n'est pas presente comme un
nouveau combat jouable. J3/4 installeront ensuite la premiere vraie attaque.

```text
Travaille dans le projet IntelliJ Murimblock sur test et lis AGENTS.md ainsi que
docs/COMBAT_REWORK_PLAN.md. Recontrole branche/dirty tree; preserve tous les
fichiers locaux et references hors perimetre. N'effectue pas de merge dans main.

Implemente uniquement la premiere tranche : une animation d'epee originale
de preparation/coupe/recuperation visible sur le skin en troisieme personne,
declenchee et horodatee par le serveur via une commande dev /combat preview.
Ce n'est pas encore une attaque de gameplay : aucun degat, clash, nouvelle garde,
combo, style equipe, slot M1, root motion, cout Qi ou modification de sauvegarde.
Ne remplace pas le combat vanilla dans cette tranche; les commandes/touches et
la garde historiques doivent conserver leur comportement et leurs couts.

Utilise un backend isole; qualifie PAL 1.1.6 NeoForge pour Minecraft 1.21.1 et
Java 21 sur notre NeoForge 21.1.248, verifie notices/transitives et dedicated
server. Ne rajoute pas Epic Fight comme dependance, ne copie pas son code GPL,
ses clips, rig, meshes ou shaders. Sans permission ressources ecrite couvrant
retargeting et distribution, cree le clip de test original sur notre rig cubique.
Documente sa provenance; ne pretends pas qu'il s'agit d'une animation EF.

Ajoute un etat temporaire de preview avec ID/generation, startTick et duree,
un snapshot serveur vers owner + trackers et pour StartTracking, et le nettoyage
fin/mort/logout/respawn/dimension. Age accepte par serveur, pas redemarrage a
zero sur chaque reception. Une commande dev limitee et les messages nouveaux
sont en anglais et traduisibles dans en_us.json. Ne modifie pas l'API addon existante.

Refuse le preview si garde/charge/usage d'item/action incompatibles. Annule le
preview si attaque normale, usage d'item, changement d'equipement ou etat
incompatible survient; ne supprime pas cette action historique pour proteger
le preview. Ne touche pas au HUD/font/camera ni aux autres armes ou mobs.
Le rendu anime corps/bras/jambes et arme, conserve skin/armor, supporte classic,
slim et main gauche, puis revient a idle. Prevois un socket et une timeline
partageables pour les prochains lots, sans contact ou hit autoritaire maintenant.

Teste codecs/temps/cleanups et absence de degats/couts, execute unit/build et
les 14 GameTests historiques. Lance une verification dediee sans classes client.
Fais les essais visuels F5/first-person et deux clients quand disponibles; sinon
signale precisement ces gates non verifies, sans les marquer passes.
Documente les fichiers ajoutes, les limites et la procedure de test en jeu.
Respecte ensuite le workflow test de AGENTS.md pour une tranche terminee,
en excluant les fichiers locaux sans rapport; jamais de merge sans mon accord.
```

Le lot suivant, apres validation du preview/horloge/socket, sera une seule vraie
attaque d'epee server-authoritative avec miss/contact/recovery et un seul impact,
en retirant le chemin vanilla seulement sur cette route. Pas de quatre slots,
de refonte des mobs ou de clash dans ce premier hit jouable.

## 11. Suivi Apres Autorisation D'Implementation

L'audit ci-dessus decrit l'etat observe avant implementation. Apres autorisation
de l'utilisateur, la premiere tranche du chapitre 10 est preparee sur `test` :
commande serveur `/combat preview`, clip original PAL et sync temporaire sans
degats ni nouveaux couts. Voir `COMBAT_PREVIEW.md` pour les fichiers, dependances,
resultats et procedure en jeu. Les hypotheses et recommandations de l'audit
ne sont pas retroactivement presentees comme deja disponibles.

PAL est qualifie en build/unit et au demarrage du serveur dedie; son moteur
Molang exact embarque est utilise dans les tests. Les gates visuels, deux vrais
clients et socket server-world-space restent ouverts. Aucune permission Epic
Fight n'a ete obtenue, aucun asset/code EF n'est importe. Le workflow GitHub de
ce nouveau lot suit AGENTS.md; la mission d'audit precedente n'avait fait ni
commit ni push. Aucun merge `main` sans validation explicite en jeu.
