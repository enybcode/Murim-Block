# Audit Du Code Et Preparation Cloud

Date : 8 octobre 2026. Travail dans le depot IntelliJ, branche `test`.
Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Gradle 9.2.1 et Epic Fight
21.17.3.1 / artefact Modrinth `8HHhJt6i` : versions conservees.
`main` reste au commit valide `5e7bf31`, sans fusion de ces corrections.

## Inspection Effectuee

Lecture des 66 fichiers Java de production, des ressources, du build, des
consignes Git et des tests pertinents. Les paquets existants restent en place :

| Zone | Points verifies |
| --- | --- |
| `Murimblock`, `network` | Enregistrement, cotes logiques, deux paquets d'intention, protocole 7 |
| `qi`, `qi.charge` | Invariants numeriques, codecs, sauvegarde, regeneration, recompenses, verrouillage et cycle de vie |
| `cultivation`, `combat` | Progression, permissions, attachments, mode reel Epic Fight et miroir temporaire |
| `api` | Contrats Qi/cultivation/combat et evenement de changement de mode |
| `integration.epicfight`, mixins | Garde/roulade, couts, restriction des contenus, recettes, loot et synchronisation |
| `client`, GUI/HUD | Touches, police locale, bornes, pages, effets Qi, reglages et renderer |
| `mob` | Enregistrement, attributs moteur, IA animee, equipement et absence de recompenses |
| `src/test`, `src/gameTest`, `.github` | Couverture, fixtures reseau, isolation du JAR et controles Linux |

Des declarations du moteur epingle ont ete relues quand necessaire, notamment
`PlayerSkills.listSkillContainers`. Les evenements de tick et de connexion ont
ete controles dans les sources NeoForge 21.1.248. Aucun code Epic Fight n'a ete
copie dans le mod et aucune nouvelle dependance de gameplay n'a ete ajoutee.

## Corrections Et Optimisations

1. **Permissions des commandes Qi.** `MurimblockCommands` exposait `set`, `add`,
   `remove`, `refill` et `reset` aux non-operateurs. Les mutations de Qi/Qi Max
   exigent maintenant le niveau 2, y compris pour soi-meme ; les consultations
   et `/combat` restent accessibles. Tests du graphe Brigadier et execution reelle
   serveur avec permissions 0 et 2. Le nom des commandes n'a pas change.
2. **Historique anti-farm.** `QiKillTracker.recordKill` parcourait toutes les
   histoires du serveur a chaque kill. Il ne purge maintenant que l'histoire
   concernee ; `QiEvents.onServerTick` fait la maintenance globale toutes les
   200 ticks, y compris quand aucun joueur ne tue de mob. Les seuils et recompenses
   restent identiques. Pas de remise a zero sur simple deconnexion.
3. **Cycle de vie des recompenses.** Les maps statiques pouvaient survivre au
   passage entre deux mondes solo. Elles sont videes au demarrage et a l'arret
   du serveur ; les premieres victoires persistantes ne sont pas effacees.
   Les appels de recompense et les cooldowns utilisent le meme temps overworld,
   independamment de la dimension du joueur. Un timestamp boss futur n'est pas
   considere comme une victoire recente.
4. **Regeneration extreme.** Une multiplication valide mais trop grande pouvait
   produire Infinity puis une exception dans `QiData.addQi`. La regeneration
   sature maintenant au maximum. Les valeurs invalides et negatives restent
   rejetees, les taux habituels et les fractions sont conserves.
5. **Charge cote client.** `QiChargeClientHandler` verifie aussi l'etat occupe
   Epic Fight. L'intention et les visuels sont remis a zero a la deconnexion et
   lors du remplacement de l'objet joueur, afin de renvoyer une nouvelle intention.
   Le FOV est remis a zero a la deconnexion. Le serveur garde son autorite ; aucun
   nouveau paquet ni cout Qi n'est ajoute. Une prediction cliente n'est pas un
   acquittement reseau exhaustif de toutes les decisions d'autres addons.
6. **Attributs de charge.** `QiChargeService` ne reapplique plus un modificateur
   identique a chaque tick. Si un autre systeme le supprime ou le modifie, il est
   reinstalle. Les autres modificateurs ne sont pas retires.
7. **Actions moteur.** `EpicFightCombatDefaults.enforce` ne materialise plus une
   liste a chaque tick et par joueur. Les controles et paquets sur changement
   restent identiques ; aucune nouvelle boucle de gameplay n'est ajoutee.
8. **HUD Qi.** Le remplissage maximum est borne aux 182 pixels de son sprite ;
   il pouvait depasser de 1 pixel. Taille, position et discret caractere du HUD
   sont conserves.

Ces optimisations retirent du travail et des allocations identifiables. Aucun
gain FPS/TPS chiffre n'est promis sans profilage d'une vraie charge multijoueur.

## Rangement Et Construction

- Cinq textures V4 inutilisees, totalisant 466 053 octets, archivees dans
  `docs/gui/legacy-v4/assets`, hors JAR. La demonstration HTML garde son chemin
  et pointe sur ces references ; le GUI courant n'a pas ete redessine.
- Connexion de joueur GameTest NeoForge extraite vers
  `src/gameTest/java/com/murimblock/testing/GameTestPlayers.java`. Les tests de
  permissions et d'historique sont classes dans leurs paquets respectifs.
- Nouveau `DistributionBoundaryTest` inspectant le JAR construit, son JSON,
  l'absence du harnais et des anciennes images, et la police Minecraft globale.
  Le JAR est un input des tests, afin que son changement invalide leur cache.
- CI avec permissions de lecture seules, limite de duree, annulation des runs
  obsoletes, controle des scripts cloud et compilation explicite du harnais.
- Index `docs/README.md`, guide `docs/CLOUD_IMPORT.md`, scripts headless et
  consignes cloud dans `AGENTS.md`. Pas de duplication des sources par branche.

## Verifications Executees

- Build et compilation du harnais sur Windows/Java 21 : reussis.
- **126 tests unitaires** : reussis, aucun echec ; JAR et ressources verifies.
- **27 GameTests serveur requis** : reussis, dont permissions, cycle de vie
  transient et calcul suivant le joueur dans les dimensions vanilla.
- Client Minecraft reel : smoke reexecute, **15 captures**, controles pixel,
  quatre pages, six reglages, navigation, touches, HUD compact, mob suivi et
  changements de pose Epic Fight ; reussi. Profile et HUD inspectes visuellement.
- **8 tests Node** de proxy : reussis ; syntaxe Bash des trois scripts validee.
- Wrapper cloud execute sous Git Bash avec le cache existant : Gradle 9.2.1 et
  Java 21 confirmes. Cela ne remplace pas un build a froid dans le cloud distant.
- Modification locale preexistante de `QiChargeClientEffects.java` preservee,
  hors commit ; aucune sauvegarde utilisateur supprimee ou migree.

Les premiers essais ont detecte une mauvaise assertion de chemin de ressource
et une fixture de joueur vanilla incompatible avec les paquets Epic Fight.
Ces erreurs de test ont ete corrigees, puis les suites completes relancees.

## Limites Et Prochaine Validation

- Le cloud existant a ete retrouve, mais son dernier onboarding etait bloque
  en HTTP 403 sur NeoForge. Les scripts et les sources sont prepares sur GitHub ;
  les reglages effectifs et un nouveau build distant restent a verifier.
- Le smoke solo et les connexions simulees ne constituent pas deux vrais clients.
  Restent a essayer : latence, connexion tardive, changement de dimension en
  charge, mort/reconnexion, garde et combos avec deux clients sur serveur dedie.
- Le test interdimension verifie les dimensions vanilla ; aucun mod de dimension
  au temps independant n'a ete installe pour cette passe.
- Des diagnostics amont Epic Fight persistent : sous-titres manquants, reference
  `air_slash`, couche optionnelle WaveyCapes et service web indisponible. Ils ne
  sont pas presentes comme corriges ni masques pour faire croire a un log vide.
- Pas de validation de JEI/EMI, de rendu GeckoLib combine, de styles Murim ni
  de quatre M1 : ces fonctionnalites ne sont pas terminees par cet audit.
- Le retrait des objets Epic Fight dans les inventaires charges reste intentionnel
  et potentiellement destructif pour d'anciens objets : conserver la recommandation
  de sauvegarder le monde avant essai. Aucun scan global de NBT imbrique ajoute.
- La validation en jeu par Enzo reste necessaire avant toute fusion dans `main`.

La reprise cloud et son prompt sont dans `CLOUD_IMPORT.md`. La prochaine tranche
doit commencer par verifier ce checkout et ses acces effectifs, pas par une
nouvelle refonte de l'architecture ou une mise a niveau arbitraire du moteur.
