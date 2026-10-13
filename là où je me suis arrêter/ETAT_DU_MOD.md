# État du mod et patchs

## Direction conservée

Murim/Wuxia dans la DA Minecraft : pixel art, formes lisibles, manuscrit clair
et épuré. Textes **en anglais en jeu**, dans `assets/murimblock/lang/en_us.json`.
La police manuscrit reste propre aux écrans Murim, sans remplacer celle de Minecraft.
Profil 3D uniquement dans Profile ; quatre pages Profile, Techniques, Cultivation,
Info. Stage reste provisoire (`???`), pas de voyant Combat Mode On/Off inutile.

**Epic Fight est une dépendance**, pas du code copié dans Murim. Son moteur gère
les animations, poses, contacts, dégâts de mêlée et synchronisation. Murim définit
ses actions, progression, ressources originales et restrictions. Les menus,
livres/items et progression natifs Epic Fight ne constituent pas nos techniques.
Pas de stamina jaune limitante ni de nouveau coût Qi pour garde, roulade ou M1.
Les compétences Murim peuvent coûter du Qi, comme Cloud Step Slash.

## Historique récent publié

| Commit | Travail |
|---|---|
| `6fd9e83` | Crab Man, volée de boules d'eau, GeckoLib et Photon |
| `0065328` | Glow des boules d'eau et déclenchement à 3-6 blocs |
| `1215e27` | Sept biomes chinois et Cloud Cliff Sanctuary |
| `d8a2c57` | Visite des biomes, présentation vidéo et outils de concepts |
| `a0fb89c` | README présentant le mod |
| `041f763` | Toits, entrée, surplombs et conservation du spawn |
| `84b2cac` | Parade, K.O./portage/finish, premier art, barre, commandes et tests |

Les biomes et les deux mobs précèdent le dernier patch : ne pas les recréer.
Le patch `84b2cac` publie 109 fichiers modifiés/ajoutés, sans mondes ni caches.

## Fonctionnalités présentes

| Domaine | État réel et points d'entrée |
|---|---|
| GUI | Clear Manuscript adaptatif ; `client/gui/MurimProfileScreen`, `MurimProfileLayout` |
| Qi/cultivation | Sauvegarde, synchronisation, charge serveur, récompenses/anti-farm, neuf royaumes/quatre étapes ; `qi`, `cultivation`, `api` |
| Monde | Jade Karst Forest, Cloud Sea Peaks, Shu Bamboo Sea, Jiangnan Waterlands, Peach Blossom Valley, Loess Plateau, Hexi Frontier Desert ; `world`, données `worldgen`, `docs/biomes` |
| Sanctuaire | Construction unique enregistrée, recherche et construction progressives, spawn conservé, Jeton de la Secte ; `SanctuaryPlacement`, `SanctuaryBuilder`, `world/structure/kit` |
| Armored Crab | Modèle/rig/animations propres, pinces avec contacts Epic Fight, spawn côtier et récompenses ; `ArmoredCrabEntity`, `ArmoredCrabEpicFight`, `docs/ARMORED_CRAB_DELIVERY.md` |
| Crab Man | Humain infecté, grosse pince, escorte de deux crabes, attaque à distance ; `CrabManEntity`, `CrabManEpicFight` |
| Boules d'eau | Une grosse et quatre petites, trajectoire serveur et éclatements ; GeckoLib pour leur rendu/animation, Photon pour la lumière ; `WaterBallEntity`, `WaterBallRenderer`, `WaterBallLights` |
| Garde | `MurimGuardSkill` dans l'emplacement de garde Epic Fight ; parfaite 10 ticks (0,5 s), normale 50 % de dégâts ; attaques de face, flèches/boules d'eau ; exclusions et réactions selon le code |
| K.O. | Un coup mortel PvP met à terre ; ramper, porter/poser, exécuter ; récupération après 30 s, compte suspendu pendant portage/finish ; `combat/knockdown`, `KnockdownClient` |
| Finish | Générique et interruptible ; caméra pour les deux participants, retour à la vue normale ; `FinishCinematic`, `FinishCinematicPayload` |
| Mort | Vraie mort : inventaire selon les règles normales du monde, réduction définitive de 10 % du Qi maximum, plancher 100 ; K.O. seul sans pénalité ; `QiData.afterDeath`, `QiEvents` |
| Art martial | Azure Cloud Sword : manuel reçu une fois par joueur à la bibliothèque, apprentissage sauvegardé ; `martial/MartialData`, `MartialService`, `MartialManualItem`, `SectLibrary` |
| Cloud Step Slash | Ruée ~5 blocs, 12 Qi, 120 ticks/6 s de recharge ; dégâts via l'animation/collider Epic Fight, pas une deuxième boucle ; `Techniques.perform`, `MurimAnimations` |
| Techniques/barre | Sélection et affectation par clic, dix emplacements, clic droit pour vider ; touches hotbar en combat, hotbar normale hors combat ; `CombatBarClient`, `TechniquePayloads` |
| Outils admin | `/martial`, opérateur niveau 2 ; apprendre, débloquer, oublier, régler la maîtrise, barre et recharges ; `MartialCommands` |

## Ce qui n'est pas terminé

Thousand Drizzle et Heaven-Splitting Crescent sont définies mais
`Techniques.perform` ne les exécute pas encore. Les débloquer par commande ne
leur donne pas un comportement. La maîtrise est enregistrée/affichée et réglable
par commande, **pas augmentée automatiquement par les touches réussies**.

Le passif Flowing Cloud Stance, les M1 du style, quatre M1 configurables, quêtes,
évolutions, variantes, combos bornés, évasion, secrets, finish propre à l'art et
styles par datapack restent à implémenter. Le clash physique des lames est en dernier.
Les chiffres et enchaînements des fiches de conception ne sont pas tous du gameplay livré.

## Touches et essai rapide

Valeurs par défaut, à vérifier dans les options du joueur : K profil, V combat
(ou porter/poser près d'un joueur K.O.), B exécution, R charge Qi, clic gauche
attaque, clic droit garde, Alt gauche roulade, G verrouillage. Les touches 1-9
et le dixième emplacement (0 par défaut) lancent la barre en combat.

Dans un **monde jetable avec commandes**, une épée en main :

```mcfunction
/martial learn murimblock:azure_cloud_sword
/martial bar set 1 murimblock:cloud_step_slash
/martial info
/martial cooldown reset
```

Ouvrir K, vérifier Techniques, entrer en combat puis lancer l'emplacement 1.
Le serveur vérifie notamment l'arme, le Qi, la recharge et l'état d'action.
Éviter `/martial unlock all` comme preuve que toutes les techniques sont jouables.

## Échanges récents et outils de proposition

La conversation Claude **Murim Block projet vérification** a été relue à l'écran
sur les derniers échanges de la tranche, des commandes et des formulaires.
Les anciens « rien n'est commité » ne sont plus vrais : Codex a publié le tout
dans `84b2cac`, après les contrôles du 13 octobre.

Enzo souhaitait une collecte **simple**, sans dossier administratif : un art,
une arme, un passif et au moins trois skills. Claude a préparé une page/artéfact
« Forge des techniques », puis un PDF de deux pages et un script Forms.
La page externe et ses données ne sont pas prouvées exportées dans Git.

- [PDF](../docs/combat/forge-des-techniques.pdf) : présent ; Claude déclarait
  avoir vérifié le rendu, pas la saisie dans tous les lecteurs PDF.
- [Script Forms](../docs/combat/formulaire-art-martial.gs) : présent ; crée
  plusieurs pages, pas encore aligné sur la dernière proposition simplifiée.
- Dernière proposition : **une page, huit questions**, pseudo, art, arme,
  passif, trois techniques et autres idées facultatives, sans chiffres obligatoires.
- Le Google Form n'a pas été créé dans la session consultée : accès Chrome/Google
  indisponible. Aucun lien public réel ni collecte de réponses ne doit être inventé.

## Limites connues

Tests locaux du 13 octobre : 131 JUnit et 81 GameTests réussis. Voir
[le rapport](../docs/DELIVERY_2026_10_13.md). Rapports graphiques du 12 octobre
présents avec PASSED ; pas de nouvel essai graphique dans cette documentation.
Deux vrais clients et latence restent à valider.

Diagnostics connus : heightmaps non initialisées dans les fixtures de génération,
placement de spawn `minecraft:camel`, `epicfight:air_slash` et avertissements de
développement des dépendances. Ne pas déclarer « aucun bug » parce que la CI passe.
