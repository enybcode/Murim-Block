# Là où je me suis arrêté : Murimblock

Point de reprise préparé le **13 octobre 2026**. Ce dossier rassemble le code
publié, les décisions du projet et les échanges récents consultés dans Claude.
Ce n'est ni une copie intégrale des conversations, ni une validation de l'alpha.

## Lire dans cet ordre

1. [État du mod et derniers patchs](ETAT_DU_MOD.md).
2. [Installation sur un autre PC](INSTALLATION_AUTRE_PC.md).
3. [Suite prévue et prompt de reprise](SUITE_ET_PROMPT.md).
4. Les règles [AGENTS.md](../AGENTS.md), puis [l'index technique](../docs/README.md).

## Point de sauvegarde

| Élément | État vérifié à la rédaction |
|---|---|
| Dépôt | [enybcode/Murim-Block](https://github.com/enybcode/Murim-Block) |
| Branche de travail | `test` |
| Dernier patch de gameplay | `84b2cac0b08997e9f03da7df4dae31cc9e7e3d43` |
| Base précédente | `041f763` : corrections du sanctuaire |
| `origin/main` | `6291b860095740d57b50e0aaa0bfe52372ad3a45` |
| Proposition vers `main` | [PR #5](https://github.com/enybcode/Murim-Block/pull/5), brouillon ouvert |
| Version du mod / réseau | `0.1.0` / protocole `10` |

Le commit de publication de **ce dossier** vient après `84b2cac` et ne modifie
pas le gameplay. Ces SHA sont des repères historiques, pas une consigne de
revenir en arrière. Toujours vérifier le dernier état distant avant de reprendre.

**Ne pas repartir de `main` pour continuer ce travail** : la PR #5 n'est pas
fusionnée à la rédaction. Les tests réussis ne remplacent pas la validation
en jeu d'Enzo. Ne jamais forcer un push ni écraser des modifications locales.

## Où nous nous sommes arrêtés

La première tranche de l'art **Azure Cloud Sword** fonctionne dans le code :
manuel, apprentissage, onglet Techniques, barre de dix compétences et attaque
**Cloud Step Slash**. Le K.O. PvP, le portage, le finish générique et la garde
Murim sont aussi publiés sur `test`.

Claude proposait ensuite le passif **Flowing Cloud Stance**, avec M1 propres au
style, parade personnalisée et un seul passif actif. **Ce passif n'est pas
implémenté** et la proposition n'est pas une autorisation de commencer sans
revoir les validations restantes. Les commandes `/martial` ont été ajoutées
pour essayer le socle avant cette suite.

Les derniers échanges portaient sur un formulaire simple pour que les amis
proposent des arts : une arme, un passif et au moins trois compétences.
Le PDF et le script Google Forms sont dans Git ; le formulaire Google lui-même
n'a pas été créé depuis la session consultée. Voir les détails dans les autres fichiers.

## Preuves et limites

- Le 13 octobre : build et compilation GameTests réussis, **131 tests unitaires**
  sans échec et **81 GameTests requis** réussis, exécutés localement.
- GitHub Actions a également réussi pour `84b2cac`, y compris compilation et
  tests serveur : [run Build #76](https://github.com/enybcode/Murim-Block/actions/runs/38391796324).
- Rapports graphiques du 12 octobre lus avec `PASSED` : parade, K.O./finish et
  art martial. Ils n'ont pas été relancés durant la publication du 13 octobre.
- **Deux vrais clients, latence et validation humaine restent à faire.**
- La réussite de GitHub Actions n'est pas une preuve que l'environnement cloud
  personnel de Claude/Codex est configuré ou que le rendu graphique y fonctionne.

Pour publier ce dossier : `build compileGameTestJava` a été relancé avec succès
(tâches à jour, tests unitaires non réexécutés), puis un nouveau serveur a passé
les 81 GameTests requis. Les 38 liens locaux des documents concernés ont été
contrôlés sans lien cassé, ainsi que `git diff --check`. Aucun gameplay modifié.

## Attention aux anciens textes

`docs/CLAUDE_HANDOFF.md` contient un historique utile mais certains passages
étaient devenus contradictoires : Techniques vide, crabe non intégré, anciens
SHA, protocole 7/8/9. Le protocole courant est **10**, les deux mobs sont intégrés
et Techniques affiche les arts appris. `docs/ANIMATION_PIPELINE.md` décrit aussi
une ancienne étape avant l'utilisation de GeckoLib/Photon pour les boules d'eau.
Privilégier le code, ce dossier et [la livraison du 13 octobre](../docs/DELIVERY_2026_10_13.md).

Les captures privées de Claude, données de compte, jetons, mondes et caches ne
sont pas copiés ici. Les observations des échanges sont résumées, sans publier
les conversations ou les propositions privées des autres personnes.
