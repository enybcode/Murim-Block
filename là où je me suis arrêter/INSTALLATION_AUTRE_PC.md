# Reprendre sur un autre PC

## Logiciels nécessaires pour développer

| Logiciel | Version/rôle |
|---|---|
| Git | Cloner et travailler sur `test` ; aucun token partagé dans Git |
| JDK | **Java 21**, JDK complet, pas seulement un JRE ; Temurin convient |
| IntelliJ IDEA | Support Java/Gradle, ouvrir le dépôt ; autre IDE possible mais IntelliJ est notre référence |
| Gradle | **9.2.1 via le wrapper fourni** ; ne pas installer un Gradle global différent |
| Node.js | 20+ pour le wrapper cloud ; **22+ pour les outils concepts/vidéo** qui utilisent WebSocket global ; non requis par le gameplay |
| Bash | Git Bash sous Windows pour `scripts/cloud` ; Bash sous Linux/cloud |
| Session graphique | Nécessaire pour `runClient` et les visites visuelles, GPU/pilote OpenGL adapté à Minecraft |

Aucun workspace MCreator à installer/importer. Blockbench est optionnel pour
éditer les modèles, PNG et animations ; Blender/exporteur Epic Fight seulement
si un nouveau mesh skinné doit être exporté. La version exacte de ces outils
artistiques n'est pas épinglée ici : vérifier le format avec le pipeline et
faire un export pilote avant de remplacer les assets du jeu.

## Dépendances du jeu, épinglées dans Gradle

Gradle les télécharge automatiquement lors du build et de l'import IDE.
Ne pas copier leurs JAR dans `src/main/resources` ni les embarquer dans notre JAR.
Pour **jouer hors de l'IDE**, créer un profil NeoForge 1.21.1 et installer ces
mods compatibles avec leurs versions exactes, côté client et serveur :

| Composant | Version utilisée | Artefact Modrinth dans `gradle.properties` |
|---|---|---|
| Minecraft | 1.21.1 | fourni par NeoForge/Gradle |
| NeoForge | 21.1.248 | `neo_version` |
| Epic Fight | 21.17.3.1 | `8HHhJt6i` |
| GeckoLib | 4.9.3 NeoForge | `Grwn5rUB` |
| Photon Editor | 2.2.8 | `T9SRwGMM` |
| LDLib2 | 2.2.42 | `aEbn4hye` |
| KilaGraph | 21.1.0.16 | `KwiKbOzM` |
| TerraBlender | 4.1.0.8 NeoForge | `6e8GCrLb` |
| Murimblock | 0.1.0, protocole 10 | build de `test` |

Les métadonnées autorisent certains intervalles plus larges ; cette table est
la combinaison réellement utilisée, pas une garantie pour toutes les versions
ultérieures. Client/serveur doivent avoir la même build. Ne pas choisir les
variantes Forge/Fabric d'un mod à la place de NeoForge.

Autres composants téléchargés par Gradle : ModDevGradle **2.0.144**,
Foojay resolver convention **1.0.0**, mappings Parchment Minecraft **1.21.1 /
2024.11.17**, JUnit BOM/Jupiter **5.11.4** et launcher des tests.
Ces éléments ne se déposent pas dans le dossier `mods` d'un joueur.

## Installation Windows / IntelliJ

Sur un autre PC, choisir son propre dossier ; le chemin de l'ancien PC
`C:\Users\enzob\Documents\Murim block` n'est pas une dépendance.

```powershell
git clone --branch test https://github.com/enybcode/Murim-Block.git "Murim block"
Set-Location -LiteralPath "Murim block"
git status --short --branch
git log -1 --oneline
java -version
.\gradlew.bat --version
.\gradlew.bat build compileGameTestJava
.\gradlew.bat -PgameTests runGameTestServer
```

Si le dépôt est privé, se connecter avec son compte autorisé via Git Credential
Manager/GitHub ; ne pas demander ni recopier le token de l'ancien PC.
Configurer `JAVA_HOME` et le JDK Gradle d'IntelliJ sur **21**. Ouvrir la racine
contenant `build.gradle`, attendre la synchronisation Gradle, puis lancer :

```powershell
.\gradlew.bat runClient
```

Pour le client normal : **pas de `-PgameTests`**. Ne pas activer le harnais de
développement dans le profil destiné aux joueurs. Le JAR de distribution
`build/libs/murimblock-0.1.0.jar` est distinct du `-sources.jar`.

Le premier build télécharge beaucoup de données et peut être long. Si la
résolution échoue : lire la première erreur, contrôler réseau/proxy/JDK,
ne pas désactiver TLS, ne pas supprimer les mondes ou forcer un reset Git.

## Linux ou environnement cloud

Java 21, Git, Node 20+ et Bash, cache accessible en écriture. Depuis le dépôt :

```bash
bash scripts/cloud/setup.sh
bash scripts/cloud/verify.sh
bash scripts/cloud/gradle.sh build compileGameTestJava
```

Utiliser **le wrapper `scripts/cloud/gradle.sh`** pour les commandes suivantes :
il conserve proxy et confiance CA et configure un cache modifiable.
Pas de désactivation des certificats ni de contournement d'une allowlist.
Le dernier onboarding cloud personnel connu rencontrait un HTTP 403 NeoForge ;
la CI GitHub réussie n'atteste pas la réparation de cet environnement distinct.
Un serveur headless ne valide pas les GUI, animations rendues ou deux clients.
Voir [CLOUD_IMPORT.md](../docs/CLOUD_IMPORT.md).

## Claude et Codex sur le nouveau PC

Facultatifs pour compiler le mod. Installer les applications/CLI depuis leurs
sources officielles et se connecter **soi-même** ; aucun compte n'est transféré
par `git clone`. Un accès Claude Code actif est nécessaire pour ses fonctions.

- Ouvrir le **même checkout `test`**, mode lecture/Plan au premier passage.
- Lire `CLAUDE.md` (importe `AGENTS.md`) et ce dossier ; utiliser le prompt de reprise.
- Un seul agent écrit dans un checkout à la fois. Worktrees séparés seulement
  si demandés ; ils ne récupèrent pas automatiquement les fichiers non committés.
- Le plugin Claude Code JetBrains et `/ide` sont optionnels ; pas de serveur
  IDE public ni de contournement des permissions.
- Le 13 octobre sur l'ancien PC : Desktop Claude connecté, mais `claude auth status`
  indiquait CLI déconnecté et `gh auth status` GitHub CLI non connecté.
  **Le push Git fonctionnait** : ces trois connexions sont indépendantes.
- GitHub CLI, si souhaité : `gh auth login`, connexion à effectuer par l'utilisateur.
- Remote Control d'une session locale et Claude cloud sont deux modes distincts.
  Aucun accès distant nouveau n'a été activé/confirmé dans cette reprise.
  Choisir le mode, autoriser uniquement le dépôt nécessaire et vérifier ses accès.

Sources : [Claude Desktop](https://code.claude.com/docs/en/desktop),
[Remote Control](https://code.claude.com/docs/en/remote-control),
[Claude cloud](https://code.claude.com/docs/en/claude-code-on-the-web).
Ne pas mettre identifiants, liens de connexion sensibles ou clés dans GitHub.

## Assets et outils à transférer séparément

Les ressources **nécessaires au jeu** sont dans `src/main/resources` : un clone
permet de les compiler. Les projets éditables originaux Blockbench/Blender,
le dossier local `crabe`, références et scènes non versionnées ne sont pas
garantis présents dans Git. Pour reprendre la création artistique, transférer
seulement les sources utiles par un moyen privé, avec leurs textures/rigs.
Ne pas publier tout l'ancien profil utilisateur pour les récupérer.

Pas de `npm install` global du projet : aucun `package.json` n'est présent.
Les générateurs d'animations utilisent les modules Node standard et le squelette
du JAR Epic Fight déjà téléchargé. `biped.cjs` cherche actuellement le cache
`~/.gradle/caches/.../epic-fight/8HHhJt6i` : avec un autre `GRADLE_USER_HOME`,
adapter ce chemin ou fournir `EPICFIGHT_BIPED` vers un fichier de référence local.
Ne pas redistribuer ce squelette upstream comme un nouvel asset original.

Les outils concepts/vidéo ont besoin d'un Blockbench **isolé**, port local 9232,
profil vide ; ne jamais exposer son port de débogage sur Internet. Ils sont
optionnels : voir [scripts/README.md](../scripts/README.md). Les exports de jeu
sont déjà versionnés ; ne pas relancer tous les générateurs pour un premier build.

**Ne pas transférer comme source** : `.idea`, `.gradle`, `build`, `run`, mondes,
logs, `.local`, sessions des agents ou credentials. Sauvegarder les mondes
séparément avant tout essai ; l'ancien filtrage des items Epic Fight peut en
retirer de sauvegardes existantes.
