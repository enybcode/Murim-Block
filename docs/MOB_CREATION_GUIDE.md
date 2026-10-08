# Guide De Creation Des Mobs Murimblock

## Objectif Et Etat Actuel

Tu definis le mob et son apparence. Je prends en charge Java, NeoForge, l'IA,
Epic Fight, les collisions, le reseau, les ressources techniques et les tests.
Tu peux aussi creer les modeles/animations toi-meme ou les commander a un artiste.
Ce guide concerne Minecraft **1.21.1**, NeoForge **21.1.248**, Java **21** et
Epic Fight **21.17.3.1**. Une autre version demande une nouvelle verification.

La preparation actuelle fournit `com.murimblock.mob.MurimEntities`, deja raccorde
au bus du mod, sans aucune entite enregistree. Les exemples ci-dessous restent
dans `docs`, hors du jar. Aucun bandit, nouveau spawn ou pack d'animations Murim
n'est encore implemente. Les patches vanilla deja fournis par Epic Fight restent
actifs. On ne modifie pas les boss ou tous les zombies pour preparer un seul mob.

Le contenu Epic Fight est retire, mais son moteur/API restent disponibles. La
garde et la roulade de base fonctionnent sans livre ; Techniques est vide. Le Qi,
la cultivation, les recompenses et l'API publique Murim restent separes du moteur.

## Choisir Le Type De Travail

| Besoin | Solution | Attention |
| --- | --- | --- |
| Changer uniquement la texture d'un mob vanilla | Resource pack ou renderer cible. | Un resource pack global change tous les exemplaires concernes. |
| Creer une variante precise d'un zombie | Type d'entite Murim derive de Zombie, patch/preset compatible. | Les traits du zombie, dont soleil et IA, doivent etre conserves ou explicitement adaptes. |
| Creer un bandit humain | Nouvelle entite et IA, renderer/mesh humanoide, patch Epic Fight adapte. | Ne pas attribuer automatiquement le preset zombie a un simple Monster. |
| Creer une creature a quatre pattes | Nouvelle entite, rig/mesh, patch et animations propres. | Les clips biped ne sont pas compatibles par simple changement d'ID. |
| Transformer un mob vanilla existant | Modification ciblee de son patch, avec essais des autres usages. | Cela affecte les mondes existants ; ce n'est pas le choix par defaut. |

Pour commencer, je recommande **un humanoide melee invoque a la commande**, avec
une arme vanilla et un seul coup. Ni generation naturelle ni comportement de
boss avant validation du rendu et des degats. Ce n'est pas un nouveau mob deja livre.

## Ce Que Tu Dois Me Fournir

Utilise [MOB_REQUEST_TEMPLATE.md](examples/mobs/MOB_REQUEST_TEMPLATE.md).
Ne bloque pas sur un nombre inconnu : ecris `A proposer` et je propose un reglage.

### 1. Identite Et Silhouette

- Nom affiche en anglais, nom de travail, role dans le Murim, niveau de danger.
- Mob vanilla a adapter ou nouvelle entite ; normal, elite ou boss.
- References face/profil/dos, taille en blocs, proportions, tenue, couleurs.
- Main portant l'arme, categorie d'arme, objets utilises et emplacement de fixation.
- Skin PNG ou texture avec dimensions et UV ; fichier source `.bbmodel`/`.blend`
  si disponible. Preciser si c'est une simple skin humanoide 64x64 ou un corps distinct.
- Elements mobiles : manteau, cheveux, queue, fourreau, armes separees.

Une image est une reference, pas un modele directement utilisable. Une skin sur
un humanoide compatible peut servir au prototype ; une silhouette distincte
demande un mesh et des poids de skinning. Je verifierai la compatibilite avant
de faire produire toute la serie d'animations.

### 2. IA Et Monde

- Hostile/neutre/allie, faction, entites ciblees ou ignorees, reaction aux allies.
- Distance d'aggro, poursuite, retour au point d'origine, fuite, patrouille.
- Marche/course, portes, eau, escaliers, saut, soleil ; comportements a conserver.
- Biomes/structures, rarete, nombre par groupe, conditions jour/nuit ou lumiere.
- Loot exact avec chances, XP et Qi souhaite ; penalite de farm ou premiere victoire.
- Persistance, nommage, despawn, interactions joueur, sons souhaites.

Un mob inconnu tombe actuellement sur le calcul de recompense Qi existant, dont
le fallback de base est 1. Cela ne constitue pas un equilibrage volontaire du mob.
Je raccorderai chaque mob a `QiRewardManager`, sans contourner l'anti-farm ni
attribuer automatiquement un bonus de boss.

### 3. Fiche De Combat

Pour chaque coup, fournir un nom de travail et une reference video/croquis :

| Champ | Exemple de proposition, pas une valeur deja en jeu |
| --- | --- |
| Mouvement | Taille horizontale a une main. |
| Preparation | 0.00 a 0.20 seconde. |
| Contact autorise | 0.20 a 0.32 seconde. |
| Recuperation | 0.32 a 0.65 seconde. |
| Duree/vitesse | Clip 0.65 seconde, vitesse initiale 1.0. |
| Degats | 4 points de vie, soit 2 coeurs, a equilibrer. |
| Portee | Selon la lame et son collider, pas une distance arbitraire illimitee. |
| Deplacement | Fixe ou avance pendant le coup, distance a preciser. |
| Combo | Coup suivant autorise ou fin d'enchainement. |
| Interruption | Stun permis pendant preparation, comportement pendant contact. |
| Defense | Bloquable par devant, esquivable, conditions d'attaque speciale. |
| Cibles | Une seule cible ou balayage avec nombre de cibles limite. |

Preciser egalement garde/esquive, frequence, direction, contre-attaque et reaction
a un coup bloque. **Pas de nouveau cout de Qi pour ces actions actuellement.**
On conserve les regles d'action du moteur ; une garde de mob personnalisee n'est
pas automatiquement creee en equipant une epee ou en animant ses bras.

Les secondes sont le temps du clip de reference. Si la vitesse change, les phases
et la pose doivent utiliser la meme horloge du moteur. Une video a 30 FPS et le
serveur a 20 ticks/seconde n'autorisent pas deux calculs independants de contact.

## Produire Les Animations

### Option A : Prototype Avec Les Clips De La Dependance

Pour un rig compatible, je peux referencer des animations de l'Epic Fight installe.
Par exemple, le fragment inactif `humanoid_behavior.fragment.json` reference
`epicfight:biped/combat/mob_onehand1`. C'est un identifiant dans le moteur, pas un
fichier d'animation copie dans Murimblock. Les modeles et proportions doivent
tout de meme convenir a ce rig ; ce n'est pas une conversion universelle.

Cette option permet de tester vite l'IA et les degats avant de financer ou produire
des mouvements originaux. Les assets Epic Fight ont des conditions distinctes du
code GPL : nous ne redistribuons pas leurs fichiers extraits. Voir les
[conditions officielles des assets](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS).

### Option B : Tes Propres Animations

1. Definir avec moi le rig cible et un seul mouvement pilote.
2. Modeliser/texturer dans Blockbench ou Blender en gardant la silhouette Minecraft.
3. Dans Blender, preparer l'armature, les poids, les UV, l'origine et l'echelle.
4. Exporter un idle court puis une attaque simple avec l'exporteur Epic Fight.
5. Me remettre projet source, exports et video. Je teste chargement, deformation,
   pivot de l'arme, mouvement racine et rendu dans Minecraft.
6. Corriger cet aller-retour avant de produire marche, course, attaques et defenses.

Un `.bbmodel`, un `animation.json` GeckoLib ou une animation vanilla n'est **pas**
directement le format de mesh/armature/animation d'Epic Fight. Le modele pixel-art
peut venir de Blockbench ; le retargeting, le skinning et l'export compatible restent
une etape distincte. N'ajoutons pas GeckoLib ou un deuxieme moteur juste pour
faire jouer les memes attaques : cela demanderait une autre architecture.

L'[exporteur officiel Blender JSON](https://github.com/Antikythera-Studios/blender-json-addon)
et le [guide d'animation officiel](https://epicfight-docs.readthedocs.io/Guides/page2/)
servent de point de depart. Le guide historique utilise Blender 3.6 ; la liste
de versions annoncee par l'exporteur ne prouve pas que chaque couple est valide
dans notre projet. Fournir la version Blender et le commit/version de l'exporteur.
Je verifierai un petit export avec la version du moteur effectivement installee.

### Controle Avant Remise Des Fichiers

- Noms et hierarchie des joints stables, rig compatible avec les clips choisis.
- Transformations et echelle coherentes, pieds au sol, pas de glissement involontaire.
- Arme attachee au bon joint, main droite/gauche clairement indiquee.
- Pose neutre, aucune deformation etrange des coudes, epaules ou texture.
- Premiere/derniere pose raccordees pour les boucles ; attaques sans boucle accidentelle.
- Mouvement racine intentionnel, deplacement precise et non double par l'IA.
- Export JSON chargeable, toutes les textures presentes, aucun chemin absolu externe.
- Video face et profil, plus une vue de dessus si l'arc de la lame est important.
- FPS de la video et du projet, duree du clip, reperes preparation/contact/recuperation.
- Licence et provenance pour chaque ressource tierce, y compris sons et textures.

Paquet de remise conseille :

```text
mob-name/
  design.md
  references/
  source/       # .blend, .bbmodel, rig et textures de travail
  textures/     # PNG + description UV
  exports/      # JSON exportes, noms de clips stables
  previews/     # videos idle/marche/attaque, FPS indique
  rights.md     # auteurs, liens et licences
```

Ne pas preparer tous les fichiers techniques Minecraft toi-meme : je choisirai
leurs noms/chemins et j'effectuerai l'integration. Un achat d'animation ne garantit
pas son rig ni le droit de redistribuer la ressource dans un mod.

### Premiere Serie De Clips

Commencer avec idle, walk/run et **une** attaque. Ajouter ensuite hit/stun, death,
guard enter/hold/exit, reaction au blocage et dodge si le comportement les utilise.
Les clips de vie peuvent utiliser temporairement ceux de la dependance compatible.
Les variantes d'armes et combos viennent apres. Chaque clip doit etre teste sur
le vrai mesh avec l'arme, pas seulement sur un rig sans texture dans Blender.

## Ce Que Je Code Dans Le Depot

Les fichiers futurs ci-dessous ne sont pas tous deja presents. Ils indiquent les
responsabilites et les conventions **1.21.1**, pas un pack pret a copier.

| Emplacement | Responsabilite |
| --- | --- |
| `mob/MurimEntities.java` | Enregistrer les EntityType sous des IDs Murim stables. |
| `mob/<Name>Entity.java` et sous-packages | Etat sauvegarde, attributs, IA, navigation, cibles, equipement. |
| `EntityAttributeCreationEvent` | Declarer les attributs de chaque nouvelle entite. |
| `client/...` + `EntityRenderersEvent.RegisterRenderers` | Renderer vanilla/client et compatibilite du mesh anime. |
| `integration/epicfight/...` | Patches/animations propres et raccordement a l'API du moteur. |
| `data/murimblock/epicfight_mobpatch/<id>.json` | Patch data-driven si adapte au type ; son ID doit correspondre a l'EntityType. |
| `assets/murimblock/textures/entity/...png` | Textures originales redistribuables. |
| `assets/murimblock/animmodels/...json` | Mesh/armature exportes, selon les IDs declares. |
| `assets/murimblock/animmodels/animations/...json` | Clips originaux exportes, declares dans le moteur. |
| `data/murimblock/loot_table/entities/<id>.json` | Loot serveur ; `loot_table` est singulier en 1.21.1. |
| `data/murimblock/neoforge/biome_modifier/...json` | Apparition naturelle seulement quand approuvee ; verifier aussi placement/conditions. |
| `lang/en_us.json` | Nom et tout texte visible en anglais. |
| `qi/QiRewardManager.java` | Recompense explicite, anti-farm et eventuels bonus de premiere victoire. |
| `src/gameTest`, `src/test` | Tests gameplay serveur, ressources et regressions. |

Le nom JSON d'un patch ne cree pas une entite. Une entite doit etre enregistree
avant le chargement des patches. Un renderer vanilla ne suffit pas a lui donner
un mesh skinned ; un patch Epic Fight ne remplace pas automatiquement les attributs,
le loot ou les regles de spawn. La sauvegarde garde le meme ID apres publication.

### Points Techniques Verifies Dans La Version Epinglee

`MobPatchReloadListener` lit `epicfight_mobpatch`. Un `preset` delegue a un patch
existant ; une mauvaise classe de base peut provoquer une incompatibilite de cast.
Le preset d'exemple `minecraft:zombie` est donc limite a Zombie et ses sous-classes
compatibles. Ce n'est pas une recette universelle pour un PNJ humain.

Pour un patch personnalise, le moteur lit notamment `isHumanoid` (orthographe
exacte), `model`, `armature`, `renderer`, `faction`, `attributes`,
`default_livingmotions` et, cote serveur, `stun_animations`, `combat_behavior`
et `humanoid_weapon_motions`. La table de comportement humanoide utilise
`weapon_categories`, `style` et `behavior_series`. Je verifierai ces champs
avec le deserialiseur de la version epinglee, pas seulement un tutoriel ancien.

Le fragment fourni utilise un cooldown de 20 ticks, une distance strictement
comprise entre 0.0 et 2.5, un style `one_hand` et une arme `sword`. Ce n'est pas
le schema complet d'un mob, une definition de degats ou une garde. Son chargement
est verifie dans un GameTest avec le deserialiseur reel ; aucun spawn ne l'utilise.
Les bornes sont des valeurs de prototype, pas une recommandation d'equilibrage final.

Les valeurs numeriques du patch doivent respecter les types attendus : certaines
valeurs d'attribut sont lues comme DOUBLE, d'autres comme INT. Les IDs d'animation
doivent deja etre declares. Un JSON valide peut encore etre semantiquement invalide.

### Relier Animation Et Degats

Je declare une animation d'attaque avec ses phases, le joint de la main/arme et
son collider. Le serveur demarre l'action via le moteur ; le rendu affiche la pose
correspondante chez le joueur et les observateurs. Le contact autorise applique
les degats par le mecanisme Epic Fight, sans ajouter un `hurt` dans un second tick
d'IA. Les sons/particules ne declenchent jamais les degats.

Verifier une application par cible et par contact autorise. Une attaque a deux
phases distinctes peut volontairement toucher deux fois ; ce choix doit etre
declare, pas etre un doublon de l'attaque vanilla. Ne pas garder un goal melee
vanilla appliquant aussi des degats pendant le coup anime.

Changer d'arme, stun, mort, deconnexion ou changement de dimension doit arreter
ou reinitialiser l'action selon une regle explicite. Pas de degat retarde apres
la disparition de l'attaquant. Un futur clash de lames utilisera les informations
de trajectoire/phase du moteur ; il n'est pas implemente dans cette preparation.

## Boucle De Livraison Et Validation

1. Fiche et droits : valider silhouette, rig, arme et choix de classe de base.
2. Prototype invoque : entite/attributs/texture visibles, sans spawn naturel.
3. Mouvement : idle/marche/course, pieds/arme bien alignes, renderer stable.
4. Premiere attaque : preparation lisible, contact reel, recuperation, un degat.
5. Defense : direction, coups non bloquables, esquive/interruptions seulement si prevus.
6. Plusieurs clients : meme animation, cible et degats chez attaquant/observateur.
7. Monde : sauvegarde/rechargement, despawn, mort, loot, Qi, dimension et obstacles.
8. Publication : checks, commit/push sur `test`, PR vers `main`. Merge seulement
   apres ta validation en jeu. Une video de Blender ne remplace pas ce test.

Pour chaque nouveau mob, je fournis la liste des fichiers, ce qui fonctionne,
ce qui reste provisoire et les commandes d'essai. Premiere commande proposee une
fois l'entite effectivement creee : `/summon murimblock:<id>` en monde de test.

### Tests A Automatiser

- Chargement sans ressource manquante ni erreur de patch ; lancement serveur dedie.
- Entite enregistrable/invoquable, attributs disponibles, donnees sauvegardables.
- Un contact cause une seule baisse de vie, hors phase aucun degat de ce coup.
- Arme incompatible/changement d'equipement : pas d'ancien collider actif.
- Garde frontale/arriere et exceptions ; pas de cout de Qi introduit.
- Mort/stun/action interrompue : pas de hit tardif ni IA bloquee.
- Loot sans objet Epic Fight, Qi conforme et anti-farm maintenu.
- Modele/rig/texture/clips resolus ; harness dev non inclus dans le jar.

### Tests Manuels Indispensables

- Vue de face/profil/dos et troisieme personne, deux tailles GUI et plusieurs FPS.
- Lame alignement main, pieds/glissement, transitions a vitesse lente/normale/rapide.
- Combat jouable contre obstacles, escaliers, eau et plusieurs cibles, sans double hit.
- Deux clients sur serveur dedie : retard reseau, arrivee tardive d'un observateur,
  relog, equipement, dimension et mort pendant une attaque.
- Compatibilite hors combat : outils, bouclier, arc, placement, mobs et boss non cibles.

Les tests automatises reduisent les regressions ; ils ne garantissent ni le ressenti,
ni toutes les compatibilites, ni l'absence totale de bugs. Duree dependante du rig,
du nombre de clips, de l'IA et des corrections visuelles, sans date ferme.

## Sources Et Limites

Les comportements de chargement/IA ci-dessus ont ete controles dans le sources
jar de **21.17.3.1**, notamment `MobPatchReloadListener`, `CustomHumanoidMobPatch`,
`CombatBehaviors`, `Animations` et `TargetInDistance`. Les documents en ligne
peuvent concerner d'autres versions ; le binaire epingle est l'autorite technique.

- [Release effectivement utilisee](https://modrinth.com/mod/epic-fight/version/21.17.3.1-mc1.21.1-neoforge).
- [Sources officielles branche 1.21.1](https://github.com/Antikythera-Studios/epicfight/tree/1.21.1).
- [Guide officiel des entites Epic Fight](https://epicfight-docs.readthedocs.io/Guides/Entities/page1/).
- [Integration API](https://epicfight-docs.readthedocs.io/API/Starting/).
- [Loot NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/).

En attente pour le premier mob : design choisi, ressources/rig, droits, fiche de
combat et validation artistique. Aucun convertisseur automatique Blockbench vers
Epic Fight n'est livre. L'API publique Murim actuelle expose Qi/cultivation/mode,
pas encore un contrat public de creation des mobs ou de styles configurables.
