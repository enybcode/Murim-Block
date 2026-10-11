# Codex Et Claude Sur Murimblock

## Meme Projet, Deux Conversations

Le dossier de reference sur ce PC est `C:\Users\enzob\Documents\Murim block`.
Il s'agit du projet IntelliJ versionne dans `enybcode/Murim-Block`.
Ne pas selectionner le workspace MCreator ni le dossier du modele de crabe.

La video fournie montre l'onglet **Code** de l'application Claude, avec un
projet selectionne. Ce n'est pas une preuve d'une liaison automatique avec
Codex. Les deux agents peuvent lire le meme code, mais leurs conversations,
comptes et quotas restent distincts. Aucun pont MCP entre agents, transfert
automatique d'historique ou controle de compte n'est installe ici.

`AGENTS.md` contient les regles communes. `CLAUDE.md` importe ce fichier pour
Claude Code, dans le terminal comme dans l'application Desktop.

## Ouvrir Claude Comme Dans La Video

1. Ouvrir l'application **Claude** et se connecter soi-meme a son compte.
2. Ouvrir l'onglet **Code**, puis une nouvelle session locale (**Local**).
3. Choisir `C:\Users\enzob\Documents\Murim block` comme dossier de projet.
4. Verifier que la session travaille dans le checkout actuel sur `test`.
   Pour examiner les changements locaux, ne pas choisir un worktree isole :
   celui-ci ne reprend pas automatiquement les fichiers non committes.
5. Commencer en mode **Plan** pour la lecture, ou **Manual** pour approuver
   les actions. Ne pas activer le contournement des permissions.
6. Envoyer le prompt de verification ci-dessous et controler le dossier,
   la branche et les fichiers cites dans sa reponse.

Un compte disposant de l'acces Claude Code est necessaire. L'authentification
Desktop et celle du CLI doivent etre verifiees separement ; ne pas deduire
qu'elles fonctionnent toutes les deux d'un seul ecran connecte. Ne jamais
placer mot de passe, code OAuth ou cle API dans une conversation ou le depot.

## Verification Initiale En Lecture Seule

```text
Tu travailles sur Murimblock. Cette premiere mission est uniquement une
verification de connexion et une lecture du projet. Lis CLAUDE.md, AGENTS.md,
docs/README.md et docs/CLOUD_IMPORT.md. Confirme le dossier reel, la branche,
le dernier commit et les fichiers modifies localement. Lis ensuite
docs/ARCHITECTURE.md et les sources correspondantes pour expliquer brievement
comment le mod est organise. Signale ce que tu n'as pas pu verifier.
Ne modifie aucun fichier, ne lance pas le jeu ou de build, ne cree pas de
commit, ne pousse rien et ne fusionne rien. Ne consulte pas de credentials,
mondes, historiques de conversations ni fichiers prives hors du projet.
```

Ce controle reussi prouve la lecture du projet, pas le rendu Minecraft, les
GameTests, le multijoueur ou la connexion a IntelliJ.

## Alternative Dans Le Terminal IntelliJ

Claude Code est un executable distinct de l'application Desktop. Depuis le
terminal integre, dans la racine du projet :

```powershell
claude --version
claude auth status
claude
```

Si necessaire, `claude auth login --claudeai` ouvre la connexion d'abonnement
dans le navigateur. Faire cette connexion soi-meme, dans son terminal.
Le CLI observe sur ce PC le 11 octobre 2026 est la version `2.1.201`, installee
dans `C:\Users\enzob\.local\bin\claude.exe`. Git Bash est deja present.

Le plugin officiel **Claude Code** pour JetBrains est optionnel : il ajoute
les diffs et le contexte de l'IDE. Il n'est pas requis pour l'onglet Code
Desktop. Installer le plugin et redemarrer IntelliJ seulement lorsque c'est
souhaite ; verifier ensuite la connexion via `/ide` dans Claude. Ne pas
exposer le serveur IDE sur le reseau ou activer l'execution automatique pour
faire fonctionner deux IA. Ne pas confondre le plugin avec un serveur MCP
JetBrains generique.

## Eviter Les Ecrasements

- Dans le dossier partage : un seul agent ecrit ; l'autre relit sans modifier.
- Exemple : Codex implemente, Claude audite le diff ; ensuite Codex corrige.
- Arreter l'agent qui ecrit avant de passer la main. Reinspecter l'etat Git
  apres chaque relais, car l'autre agent a pu changer les fichiers.
- Pour coder en parallele, demander des worktrees separes et attribuer les
  responsabilites. Integrer les changements verifies dans `test` sans reset
  destructif ni force-push. `main` attend toujours la validation d'Enzo.
- Une instruction Markdown est une regle de travail, pas un verrou technique
  garantissant qu'aucun conflit de fichiers ne peut arriver.

Handoff minimal : objectif, branche et SHA, fichiers concernes, tests vraiment
executes et resultats, controles en jeu encore necessaires. Les tests et les
limites du cloud restent ceux de `AGENTS.md` et `docs/CLOUD_IMPORT.md`.

Les captures et outils temporaires restent dans `.local`. Les preferences
privees de Claude, les sessions, les worktrees et les fichiers de connexion
IDE sont ignores par Git. Ne pas ajouter de credentials aux fichiers partages.

## Sources Officielles

- [Claude Code Desktop](https://code.claude.com/docs/en/desktop) : session
  locale, choix du dossier, modes, worktrees et configuration partagee.
- [Instructions de projet](https://code.claude.com/docs/en/memory) : imports
  `@path` dans `CLAUDE.md`.
- [Integration JetBrains](https://code.claude.com/docs/en/jetbrains) : plugin
  optionnel et connexion IDE.
- [Demarrage CLI](https://code.claude.com/docs/en/quickstart) : installation
  et authentification.

Sources consultees le 11 octobre 2026. Leur consultation ne remplace pas
une verification locale de connexion au compte.
