# Suite prévue et prompt de reprise

## Objectif de jeu

La profondeur souhaitée s'inspire de Jujutsu Shenanigans, sans reprendre ses
attaques : l'identité vient de l'art martial, pas d'un personnage prédéfini.
Deux joueurs avec la même arme doivent pouvoir jouer des styles différents.
Le style principal donne M1, parade et passif ; un seul passif actif à la fois.
La barre accepte à terme les compétences de plusieurs arts possédés, sous
condition d'arme et d'état. Les secrets se découvrent par des conditions/timings.

La barre de **10 compétences** actuelle n'est pas les **4 M1 configurables**
souhaités : ce sont deux systèmes différents. Epic Fight reste l'autorité de
mêlée et le moteur d'animation ; pas de retour à l'ancien moteur indépendant.

## Prochaines étapes, pas une liste déjà terminée

1. **Stabiliser la tranche publiée.** Relancer les contrôles sur le nouveau PC,
   valider les biomes/mobs/boules d'eau, la garde, le K.O., le finish et Cloud Step
   Slash. Deux vrais clients sur serveur dédié : latence, arrivée d'un observateur,
   mort/reconnexion, dimension et équipement. Ne fusionner que sur accord d'Enzo.
2. **Flowing Cloud Stance (proposition de Claude).** Choix du passif principal
   sauvegardé dans Techniques, un seul actif, combo M1 d'épée propre à l'art,
   parade/riposte personnalisées et tests. Définir les timings et effets avec
   Enzo avant de modifier globalement le comportement des autres styles.
3. **Deuxième et troisième techniques.** Thousand Drizzle puis Heaven-Splitting
   Crescent, chacune en tranche jouable avec animation, phases, contacts et
   limites de cibles ; pas de dégâts ajoutés parallèlement à ceux d'Epic Fight.
4. **Variantes.** Au sol/en l'air/après parade, combos bornés et interruption ;
   tests des transitions, des recharges et du Qi validés serveur.
5. **Progression.** Maîtrise sur vraie touche autorisée, quêtes, évolutions et
   secret Swallow Returns ; prévenir le farm trivial et conserver les données.
6. **Finish de l'art.** Last Rain Over the Clouds, original, synchronisé et
   interruptible, avec caméra sûre pour participants/observateurs.
7. **Extension du contenu.** Quatre M1 configurables et styles décrits par
   données ; proposer ensuite un autre art/mob sans réécrire le moteur.
8. **Clash physique en dernier.** Préparer trajectoires/phases/contact dans
   l'architecture, mais ne pas ajouter un clash approximatif aux premières tranches.

Référence de conception : [Azure Cloud Sword](../docs/combat/styles/azure-cloud-sword.md)
et [styles](../docs/combat/STYLES.md). Elles contiennent des objectifs futurs :
le code et les tests de la tranche restent la preuve de l'état réel.

## Questions encore ouvertes

- K.O. contre les mobs ou uniquement PvP ? Aujourd'hui PvP.
- Durée 30 s et récupération à 25 % de vie : valeurs actuelles, équilibre à valider.
- Explosions parables ? Le code courant ne les pare pas ; ne pas changer sans décision.
- Secret caché puis affiché après découverte : direction proposée, confirmation à obtenir.
- Économie du Qi du passif, récompenses après parade et variantes : chiffres de
  la fiche non approuvés comme équilibrage final.

Décisions déjà notées : pas de réanimation manuelle du K.O. par un autre joueur,
manuel du sanctuaire individuel, mort/exécution avec pénalité de Qi maximum,
garde de base sans nouveau coût Qi. Préserver les autres coûts existants.

## Travail de collecte séparé du gameplay

Le formulaire pour les amis doit rester simple : une page, pseudo, nom de
l'art, arme, passif, trois compétences, autres idées facultatives. Pas de
chiffres obligatoires ; le développeur s'occupe du code et de l'équilibrage.
Le script existant est multipage : l'adapter avant d'annoncer qu'il correspond
à la dernière demande. La connexion Google et les autorisations se valident
par l'utilisateur ; aucune collecte de comptes/mots de passe dans une fiche.

La page Forge des techniques dans Claude est un artéfact externe, pas une
partie du mod. Ne pas présumer sa portabilité ni publier les réponses privées.
Le serveur Discord Murimblock a été créé séparément ; salons/règles/forums
et protections natives préparés. Bots/tickets et connexion OAuth n'ont pas
été terminés depuis ce chat. Ce travail n'est pas installé par un clone Git.

## Prompt prêt pour Claude ou Codex sur le nouveau PC

```text
Tu reprends Murimblock depuis le dépôt enybcode/Murim-Block, branche test.
Lis AGENTS.md, CLAUDE.md, le dossier « là où je me suis arrêter »,
docs/README.md et docs/CLOUD_IMPORT.md. Ne recharge pas tous les anciens audits.

Vérifie le vrai dossier, la branche, le SHA, les fichiers locaux et les versions.
Le dernier patch de gameplay documenté est 84b2cac ; cherche les commits plus
récents sans reset ni écrasement. Le projet IntelliJ est la source, pas MCreator
ni une ancienne copie Murimblock claude. Un seul agent écrit dans ce checkout.

Confirme Java 21 / Minecraft 1.21.1 / NeoForge 21.1.248 et les artefacts épinglés.
Gradle doit télécharger les dépendances, pas les copier dans notre JAR.
Sur Windows, lance gradlew.bat build compileGameTestJava, puis
gradlew.bat -PgameTests runGameTestServer. En cloud utilise exclusivement
bash scripts/cloud/verify.sh puis scripts/cloud/gradle.sh, en gardant proxy/CA.

Fais d'abord un bilan du socle présent : parade Murim, K.O./portage/finish,
pénalité de mort, manuel individuel, Cloud Step Slash, Techniques et barre de
10 compétences. Les 4 M1 configurables ne sont pas cette barre. Les deux autres
techniques sont seulement définies ; maîtrise automatique/quêtes/passif ne
sont pas implémentés. Les deux mobs et les biomes sont déjà intégrés.

Pour cette première reprise, ne modifie pas le gameplay. Rapporte les tests
réellement exécutés, prépare l'essai graphique et deux vrais clients, puis
propose la première tranche Flowing Cloud Stance, limitée et testable via
Epic Fight. Ne prétends pas qu'une capture ou un test factice vaut deux clients.
Ne lance pas automatiquement les tours qui recréent leurs mondes jetables.

Garde les textes du jeu en anglais et la DA pixel-art manuscrit épurée.
Ne réintroduis pas la stamina/GUI/items natifs Epic Fight, un moteur copié,
une deuxième boucle de dégâts ou un clash précoce. Respecte les coûts Qi
existants et ne rajoute pas de coût à la garde de base.

Pas de commit/push pendant ce premier diagnostic. Pour les futures tranches
autorisées : tests, commit ciblé, push origin/test, PR vers main ; fusion
uniquement après validation explicite d'Enzo. N'active aucun accès distant,
ne transfère aucun credential, monde ou état IDE dans le dépôt.
```
