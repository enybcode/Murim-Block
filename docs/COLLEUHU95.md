# Branche Colleuhu95 : créer sa propre technique

Branche personnelle de Colleuhu95, créée le 14 octobre 2026 depuis `test` (`cd74b16`) à la demande d'Enzo,
propriétaire du projet. Elle sert à concevoir et à essayer une technique martiale originale sans toucher au
travail des autres. Ce fichier remplace, sur cette branche seulement, le « GitHub Workflow » d'`AGENTS.md`.

## Règles de la branche

- **Travailler uniquement sur `Colleuhu95`.** Commit et push vers `origin/Colleuhu95`, jamais vers `test` ni `main`.
- **Ne jamais fusionner** dans `test` ou `main`, ne jamais forcer un push, ne jamais supprimer ou réécrire une
  branche. Quand la technique est prête, ouvrir une pull request `Colleuhu95` → `test` : Enzo décide de l'intégrer.
- **Rester à jour** : récupérer `test` de temps en temps (`git fetch origin` puis `git merge origin/test`, jamais de
  rebase d'une branche déjà poussée). Résoudre les conflits en gardant le travail des deux côtés.
- **Ne pas modifier le travail existant** (Azure Cloud Sword Art, Long Fist, mobs, biomes) sauf le strict nécessaire
  pour brancher la nouvelle technique, et le signaler dans la pull request.
- Les règles « In-Game Text », « Verification And Cloud » et « Multiple Coding Agents » d'`AGENTS.md` restent valables :
  textes du jeu en anglais dans `assets/murimblock/lang/en_us.json`, `src/gameTest` réservé au développement.
- Ne jamais mettre dans le dépôt : mondes locaux, état de l'IDE, caches, identifiants, fichiers `.local`.

## Installer

Versions à respecter (ne pas mettre à jour) : Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Gradle 9.2.1 (fourni par
`gradlew`), Epic Fight 21.17.3.1, GeckoLib 4.9.3, Photon 2.2.8, TerraBlender 4.1.0.8 (Gradle télécharge les
dépendances tout seul).

1. **Git** et **GitHub CLI** (`gh`), puis `gh auth login` avec le compte GitHub invité comme collaborateur.
2. **JDK 21** (Eclipse Temurin 21 conseillé). `java -version` doit afficher 21.
3. **Node.js 20 ou plus** : les scripts d'animation et d'icônes sont en JavaScript (`scripts/animation`).
4. **Blockbench** (gratuit) : pour voir et retoucher les animations. Les scripts le pilotent dans une instance isolée :
   `Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<dossier vide>`.
5. **IntelliJ IDEA** (Community suffit), facultatif : ouvrir le dossier, il importe le projet Gradle.
6. Cloner et se placer sur la branche :
   ```bash
   git clone https://github.com/enybcode/Murim-Block.git Murimblock
   cd Murimblock
   git switch Colleuhu95
   ```
7. Vérifier que tout compile (Windows : `gradlew.bat`) :
   ```bash
   ./gradlew build compileGameTestJava
   ./gradlew -PgameTests runGameTestServer
   ```
8. Jouer : `./gradlew runClient` lance Minecraft avec le mod. Dans un monde créatif avec les commandes activées :
   `/martial learn murimblock:<art>` apprend un art, `/martial unlock murimblock:<technique>` débloque une technique,
   `/martial bar set <case 1-10> murimblock:<technique>` la place sur la barre de combat (touche V pour le mode combat).

## Comment une technique est faite

Lire d'abord `docs/combat/STYLES.md` (règles communes) et `docs/combat/styles/long-fist.md` (l'art le plus récent,
un bon modèle). Le commit `42df647` (« Add the Long Fist ») montre tous les fichiers à toucher pour un art complet.

| Étape | Où |
|---|---|
| Fiche de conception (idée, chiffres, animation décrite) | `docs/combat/styles/<art>.md` |
| Déclaration : `Technique(id, art, coûtQi, rechargeEnTicks)` (20 ticks = 1 s), l'art, ses quêtes et évolutions | `martial/MartialArts.java` |
| Ce que fait la technique (variante sol/air, déplacement, dégâts) | une classe par art, comme `martial/LongFist.java` |
| Branchement : arme exigée et lancement | `martial/Techniques.java` (`canUse`, `perform`) |
| Animation Epic Fight (attaque, collider, phases) | `integration/epicfight/MurimAnimations.java` |
| Le mouvement lui-même, généré en JSON | `scripts/animation/<art>.cjs` (voir `long-fist.cjs`) → `assets/murimblock/animmodels/animations/biped/murim/` |
| Icône 16×16 de la technique | `scripts/worldgen/TechniqueIcons.java` → `textures/gui/technique/` |
| Nom et description en anglais | `assets/murimblock/lang/en_us.json` (`technique.murimblock.<id>` et `.desc`) |
| Tests serveur | `src/gameTest/java/com/murimblock/<Art>GameTests.java` |
| Tour en vrai client (captures, GIF) | `src/gameTest/java/com/murimblock/<Art>Tour.java`, tâche Gradle `run<Art>Tour` |

Le plus simple pour commencer : une nouvelle technique dans un nouvel art à soi (son id, son rouleau ou manuel), pour ne
pas modifier les arts existants. Si le réseau change (nouveau paquet), augmenter la version du protocole dans
`network/MurimblockNetworking.java`.

## Vérifier avant chaque push

```bash
./gradlew build compileGameTestJava
./gradlew -PgameTests runGameTestServer
```

Puis essayer la technique en jeu (`runClient`). Les tests automatiques ne remplacent pas l'essai en jeu.
Dans la pull request vers `test` : ce que fait la technique, ses chiffres, les checks lancés et leurs résultats,
et ce qui n'a pas été vérifié.
