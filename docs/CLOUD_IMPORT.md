# Importer Murimblock Dans Le Cloud

## Source Et Branches

Depot : [enybcode/Murim-Block](https://github.com/enybcode/Murim-Block).
Le projet IntelliJ est la source de verite, pas l'ancien dossier MCreator.

`test` et `main` sont deux branches du meme depot, pas deux dossiers a fusionner.
`test` contient le GUI et les corrections a essayer. `main` reste la version
validee par Enzo. Le cloud doit selectionner **test** pour importer ce travail.
Un build vert ne donne pas l'autorisation de fusionner sur `main`.

La conversation cloud **Configurer Murim-Block**, inspectee le 8 octobre 2026,
avait prepare `/workspace/Murim-Block` depuis `main` au commit `5e7bf31`.
Java 21 et Gradle 9.2.1 avaient fonctionne. Le dernier build documente avait
echoue avec HTTP 403 sur `maven.neoforged.net`. La liste reseau etait modifiee
dans un brouillon : son application effective n'est pas verifiee depuis ce chat.

Le transfert normal passe par GitHub, deja relie a cet environnement : aucun
upload de `run`, de caches ou de sauvegardes n'est necessaire. Une archive source
locale du commit publie sur `test` peut aussi servir de copie de secours ; une
archive n'a pas l'historique Git et ne remplace pas le checkout du depot.

## Organisation A Conserver

| Chemin | Role | Importer dans le cloud |
| --- | --- | --- |
| `src/main/java`, `src/main/resources`, `src/main/templates` | Mod livre | Oui, via Git |
| `src/test` | Tests unitaires et controle du JAR | Oui |
| `src/gameTest` | Tests serveur, fixtures et client visuel de developpement | Oui, jamais dans le JAR |
| `gradle`, `gradlew`, fichiers Gradle | Construction reproductible | Oui |
| `scripts/cloud` | Configuration et verifications headless | Oui |
| `docs`, `AGENTS.md`, `.github` | Consignes et documentation | Oui |
| `docs/gui/legacy-v4` | Anciennes references, inactives | Oui, hors ressources du mod |
| `.local`, `.gradle`, `build`, `.idea`, `run`, `saves`, `logs` | Local, caches, resultats, mondes | Non |

Ne pas envoyer un JAR comme remplacement du code source. Ne pas embarquer le
JAR Epic Fight : Gradle telecharge l'artefact epingle. Ne pas copier les identifiants
Windows/Git Credential Manager ou des tokens dans un fichier du depot.

## Configuration De L'Environnement Existant

1. Choisir le depot `enybcode/Murim-Block` et la branche `test`.
2. Conserver un JDK **21**, un Node.js **20 ou plus**, Bash et Git.
3. Ajouter comme variable persistante `GRADLE_USER_HOME=/workspace/.gradle-home`
   si ce chemin reste accessible en ecriture. Sinon, les scripts utilisent
   `.local/cloud-gradle` dans le checkout. Ne pas utiliser `/home/agent/.gradle`
   si l'agent n'a pas les droits sur ce dossier.
4. Preserver `HTTP_PROXY`, `HTTPS_PROXY`, leur certificat et les reglages Java
   fournis par la plateforme. Ne pas les remplacer par un proxy personnel.
5. Dans le script d'installation, depuis le checkout, executer :

```bash
bash scripts/cloud/setup.sh
```

6. Le meme script peut servir de maintenance apres un changement de dependances.
   Sauvegarder les reglages et regenerer le cache d'environnement si necessaire.
7. Relancer les verifications ci-dessous avant de declarer l'environnement pret.

Les scripts ne changent pas de branche, ne creent pas de commit, ne fusionnent
pas et n'effacent pas de monde. Un `export` fait uniquement dans le script
d'installation ne persiste pas forcement pendant le travail de l'agent : utiliser
la variable d'environnement persistante ou toujours notre wrapper cloud.

## Reseau Et Proxy Java

Preserver la liste de domaines existante et ajouter les domaines de construction
necessaires. Cette liste est une base issue des dependances et du dernier
onboarding, pas une preuve de leur autorisation effective :

```text
services.gradle.org
downloads.gradle.org
plugins.gradle.org
plugins-artifacts.gradle.org
repo.maven.apache.org
repo1.maven.org
maven.neoforged.net
maven.parchmentmc.org
maven.fabricmc.net
api.modrinth.com
cdn.modrinth.com
libraries.minecraft.net
piston-meta.mojang.com
piston-data.mojang.com
resources.download.minecraft.net
github.com
release-assets.githubusercontent.com
objects.githubusercontent.com
```

Les redirections peuvent demander un domaine supplementaire. Le JDK est fourni
par l'environnement : ne pas compter sur Foojay pour l'installer sans verifier
separement ses acces reseau. Pour les telechargements de construction, GET/HEAD
suffisent habituellement ; la publication Git utilise des permissions distinctes.

Java ne reprend pas automatiquement les variables proxy comme certains outils
shell. `scripts/cloud/gradle.sh` les traduit en proprietes JVM pour le wrapper
et Gradle, a chaque invocation. La configuration est locale au cache et conserve
les proprietes existantes. Le script accepte le proxy HTTP gere, sans identifiants
dans l'URL ; HTTPS reste utilise pour les destinations TLS. Il ne copie aucun
secret dans les sources. Les exceptions `NO_PROXY` sont traduites pour les noms
d'hotes ; les CIDR, inexpressibles dans ce format Java, restent sur le proxy.

- HTTP 403 : verifier les regles effectives et le domaine refuse dans le cloud.
- `UnknownHostException` : verifier le proxy JVM et les variables fournies.
- Cache non accessible : corriger `GRADLE_USER_HOME`, sans changer les droits systeme.
- `PKIX path building failed` : faire utiliser a Java le magasin de confiance gere
  contenant le certificat de la plateforme. Conserver la verification TLS ; ne
  pas utiliser `curl -k`, un trust-all ou supprimer les variables proxy.

## Verification Reelle

Depuis le checkout choisi par l'environnement :

```bash
git status --short --branch
git rev-parse HEAD
java -version
node --version
bash scripts/cloud/verify.sh
```

`verify.sh` controle les scripts, execute les tests unitaires avec inspection du
JAR, compile le harnais puis lance un vrai serveur GameTest. Il echoue si aucun
test requis ne passe, meme si le serveur s'arrete sans erreur. Les rapports sont
dans `build/reports/tests/test`, les captures/logs serveur dans `build/game-test-run`,
et le resume serveur dans `build/cloud-verification/gametest-output.log`.

Pour une commande Gradle supplementaire, utiliser :

```bash
bash scripts/cloud/gradle.sh --no-daemon --max-workers=2 build
bash scripts/cloud/gradle.sh prepareClientRun
```

La premiere resolution a froid peut etre longue. Un wrapper disponible ne
prouve pas que NeoForge, Parchment, Minecraft et Epic Fight sont telecharges.
Un cache ne remplace pas un test reussi contre les regles reseau effectives.

Le client visuel exige une session graphique, pas seulement un serveur Linux
headless. Il reste testable localement dans IntelliJ sur un monde jetable, via
`-PgameTests runVisualSmoke`. Ne pas activer `-PgameTests` dans le client habituel.
Le JAR normal ne contient pas ce harnais. Aucun script n'accepte automatiquement
l'EULA Minecraft ni n'ouvre un serveur public.

## Prompt De Reprise Cloud

```text
Travaille dans le checkout GitHub de enybcode/Murim-Block selectionne sur test.
Lis AGENTS.md, docs/README.md, docs/CLOUD_IMPORT.md et le dernier rapport d'audit.
Verifie la branche, le SHA et les modifications existantes sans rien ecraser.
Utilise Java 21 et bash scripts/cloud/gradle.sh pour chaque commande Gradle.
Inspecte l'etat et la politique reseau effectifs de ton environnement ; le dernier
onboarding etait bloque en HTTP 403 sur maven.neoforged.net. Ne contourne ni le
proxy ni TLS. Execute bash scripts/cloud/verify.sh et rapporte les vrais resultats,
les nombres de tests et les limitations. Ne pretends pas avoir verifie le rendu
ou deux clients si aucun client graphique n'a tourne. Ne migre pas les sauvegardes.
Pour des modifications terminees et verifiees, suis le workflow test vers main
de AGENTS.md, en preservant les changements des autres. Ne fusionne jamais main
sans validation explicite d'Enzo. N'ouvre aucun serveur public.
```

## Sources Officielles

- [Environnements Codex Cloud](https://learn.chatgpt.com/docs/environments/cloud-environment) : checkout, installation, cache et variables.
- [Acces Internet Codex Cloud](https://learn.chatgpt.com/docs/cloud/internet-access) : allowlist et methodes HTTP.

Ces pages expliquent les principes de configuration. Les observations propres
a Murimblock viennent de la conversation d'onboarding et des scripts du depot,
pas d'une nouvelle execution dans l'environnement distant.
