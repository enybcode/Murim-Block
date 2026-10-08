# Animations Des Mobs : Epic Fight Et GeckoLib

Etude et socle controles le 2026-10-08. Lire aussi le
[guide de creation des mobs](MOB_CREATION_GUIDE.md) et la
[fiche de remise](examples/mobs/MOB_REQUEST_TEMPLATE.md).

## Decision Pour Murimblock

Epic Fight reste le moteur des humanoides combattants : poses, phases, collisions,
IA de melee animee et synchronisation. Murimblock definit ses entites, leurs regles,
leurs ressources originales et les raccordements. Ne pas faire jouer les attaques
du meme corps par deux moteurs independants. GeckoLib n'est pas ajoute a ce stade.

Un mob d'ambiance non patche par Epic Fight pourra utiliser GeckoLib si son design
le justifie. Ce choix doit se faire par type d'entite, pas a chaque frame.
L'existence d'une integration upstream ne garantit pas tous les modeles/addons.

## Versions Et Preuves

| Element | Version / etat examine |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Java | 21 |
| Epic Fight | 21.17.3.1, artifact Modrinth `8HHhJt6i`, client et serveur |
| Binaire Epic Fight SHA-256 | `8B882554CF10086398340FBDC741819EE72A801A3ADCE516C7F4768326A39526` |
| GeckoLib | Non declare / non installe dans ce projet. Documentation GeckoLib 4 et pont fourni par Epic Fight examines ; aucun couple de binaires GeckoLib valide en jeu ici. |
| Blender / exporteur | Documentation officielle consultee, mais pas de nouveau mesh/clip Murim exporte ou teste dans cette livraison. |

Les constats precis ci-dessous viennent du sources jar correspondant a **21.17.3.1**,
pas uniquement du wiki ou de la branche GitHub actuelle. Les noms de cette branche
peuvent evoluer : reverifier avant toute mise a jour. Les sources locales extraites
et les captures restent dans les caches / `build`, hors du mod publie.

## Prototype Effectivement Installe

`murimblock:training_opponent` est un **Zombie derive**, a apparence vanilla
provisoire, equipe d'une epee en fer. Ce n'est pas encore notre bandit humain.

| Fichier Murimblock | Role actuel |
| --- | --- |
| `mob/MurimEntities.java` | EntityType, dimensions, suivi reseau et attributs a la creation. |
| `mob/TrainingOpponentEntity.java` | Ciblage/poursuite, equipement et restrictions du prototype. Aucun goal melee vanilla appliquant des degats. |
| `integration/epicfight/EpicFightMobSupport.java` | Ajoute explicitement les attributs humanoides Epic Fight aux types custom. |
| `client/TrainingOpponentRenderer.java` | Renderer Zombie client de secours ; le preset Epic Fight fournit le rendu anime. |
| `data/murimblock/epicfight_mobpatch/training_opponent.json` | `preset: minecraft:zombie`, delegue au patch compatible existant. |
| `data/murimblock/loot_table/entities/training_opponent.json` | Table de loot vide. |
| `qi/QiRewardManager.java` | Recompense Qi explicite de zero pour ce seul ID. Autres recompenses inchangees. |
| `src/gameTest/.../FoundationGameTests.java` | Patch/attributs/goals, NBT, mort/recompenses, un contact anime et IA autonome. |
| `src/gameTest/.../ClientVisualSmoke.java` | Suivi client, renderer Epic Fight et captures idle/attaque reelle. |

Invocation, avec commandes autorisees, dans un monde de test en difficulte Normal :

```mcfunction
/summon murimblock:training_opponent ~ ~ ~3
/kill @e[type=murimblock:training_opponent]
```

Il est hostile aux joueurs en survie, peut les tuer et n'est pas equilibre.
Pas de spawn naturel, d'oeuf creatif, de loot, d'XP ou de gain de Qi. Pas de
brulure solaire, de conversion naturelle en drowned, de bebe ou d'equipement
aleatoire au spawn. Une commande/NBT admin peut changer son equipement : ce
prototype n'interdit pas les commandes et ne pretend pas couvrir tous leurs usages.
Il garde des caracteristiques Zombie (sons, undead/faction, navigation et despawn).

Le preset configure deja des comportements/enchainements upstream. Nous n'avons
pas invente une garde NPC, un style Murim ou des nouveaux combos pour ce mob.
Le JSON d'exemple dans `docs/examples/mobs` reste inactif, distinct du preset live.

## Comment Le Moteur Fonctionne

### 1. Entite, Patch Et Attributs

`MobPatchReloadListener` charge `data/<namespace>/epicfight_mobpatch/<id>.json`
et resout l'EntityType deja enregistre. Un `preset` delegue a son provider de patch.
Ce fichier ne cree ni l'entite ni ses attributs. `ZombiePatch` / `HumanoidMobPatch`
associent le rig, les animations de vie, les comportements et le renderer.

`EpicFightAttributes` ajoute automatiquement des attributs a une liste de types
vanilla ; nos types custom n'y sont pas inclus. Le helper Murim ajoute poids,
impact, max strikes, stun armor, armor negation et les attributs de main secondaire.
L'oublier peut provoquer une erreur lorsque le moteur accede a ces attributs.

`MobPatch.onJoinWorld` initialise l'IA cote serveur si l'entite n'est pas `NoAI`.
Un mob invoque directement avec `{NoAI:1b}` n'est donc pas un test de son IA.
Le harness rejoint le monde normalement avant de geler le prototype pour le rendu.

### 2. Mesh, Armature Et Clip Ne Sont Pas La Meme Chose

Le mesh contient geometrie, UV et poids ; l'armature contient la hierarchie des
joints et leur pose de reference ; le clip contient leurs transformations animees.
`AnimationManager` et `JsonAssetLoader` chargent les donnees. Les animateurs
client/serveur calculent les poses ; le renderer skinned affiche le mesh.

Un humanoide compatible peut commencer avec `Armatures.BIPED` et les animations
installees. Une creature aux proportions/joints differents demande un retargeting,
voire une armature et des animations originales. Renommer des bones ou changer
un ID de preset ne realise pas cette adaptation.

### 3. Declaration D'Attaque Et Horloge

Un export de mouvement ne declare pas a lui seul un coup jouable. Pour une attaque
originale, il faut l'enregistrer avec le type `AttackAnimation`, une armature,
un ou plusieurs joints/colliders, des phases et les proprietes de combat.
La version examinee expose `AnimationManager.AnimationRegistryEvent.newBuilder`
et `AnimationBuilder.nextAccessor` pour les declarations dans notre namespace.
Ce raccordement sera ajoute lorsque le premier export original sera disponible.

`AttackAnimation.Phase` contient notamment `start`, `antic`, `preDelay`, `contact`,
`recovery`, `end`, la main et les colliders. La fenetre de contact est comprise
entre `preDelay` et `contact`, et non une simple keyframe graphique.

Exemple reel examine : `Animations.BIPED_MOB_ONEHAND1`, ID
`epicfight:biped/combat/mob_onehand1`, declare transition 0.08 s, antic 0.45 s,
preDelay 0.55 s, contact 0.66 s, recovery 0.95 s, joint `toolR` et armature BIPED.
Ces nombres sont ceux du clip reference, pas une specification Murim a copier.
Le collider null dans cette declaration est resolu par le moteur via l'equipement.

Le moteur utilise le temps ecoule et precedent de son `AnimationPlayer`, plus la
vitesse de lecture. Pose, trajectoire et collision doivent suivre cette meme
horloge. Ne pas utiliser un compteur de frames de rendu ou un second timer pour
les degats. Une vitesse x2 ne doit pas laisser le contact au temps reel initial.

### 4. Collisions Et Degats Serveur

`AttackAnimation.attackTick` / `hurtCollidingEntities` echantillonnent le mouvement
des colliders entre les poses precedentes/actuelles pendant la phase autorisee.
Le moteur filtre les cibles, la visibilite et le nombre de frappes.
Il garde les listes de cibles deja essayees / effectivement touchees pour ne pas
reappliquer le meme contact chaque tick ; elles sont reinitialisees a une nouvelle
phase. Un coup volontairement multiphase peut donc toucher plusieurs fois.

`MobPatch.attack` utilise `original.doHurtTarget` avec son contexte de degats.
Ne pas supprimer cette methode sur notre mob : elle reste le chemin de degats
appele par Epic Fight. En revanche, ne pas appeler aussi `hurt` ou `doHurtTarget`
dans un goal/timer Murim, un keyframe event GeckoLib ou un event client.

`HumanoidMobPatch` installe `AnimatedAttackGoal` et `TargetChasingGoal`, a partir
du comportement et de l'arme. `TargetChasingGoal` herite de `MeleeAttackGoal` mais
son `checkAndPerformAttack` est vide : sa presence n'est pas un doublon de degats.
Le test exclut ce goal de poursuite du detecteur de melee vanilla restante.

### 5. Synchronisation Et Transitions

L'IA decide sur le serveur. `playAnimationSynchronized` demarre via le moteur et
diffuse aux clients qui suivent l'entite ; les clients rendent, pas de hit local
autoritaire. Les paquets/registrations de patch Epic Fight doivent rester actifs
meme si ses GUI et ses objets sont masques ou filtres par Murimblock.

Pour un futur style custom : verrouiller l'arme/le style pour l'action ou annuler
explicitement avant une transition. Ne jamais remplacer le collider au milieu
d'une phase tout en conservant une ancienne trajectoire. Mort/stun/perte de cible,
despawn, dimension, reload et arrivee tardive d'un observateur demandent des tests
dedies. Le prototype s'appuie sur les transitions upstream ; ces cas ne sont pas
tous validates en reseau dans cette livraison. Aucun nouveau cout de Qi ajoute.

## GeckoLib : Conflits Et Limites Reelles

GeckoLib 4 possede ses `AnimationController` et ses regles de transition ; plusieurs
controllers doivent se partager les bones sans conflits. Ce n'est pas le meme
systeme de pose qu'Epic Fight. Voir la
[documentation des controllers](https://github.com/bernie-g/geckolib/wiki/The-Animation-Controller-%28Geckolib4%29).

Dans le sources jar Epic Fight examine, `compat/geckolib/GeckolibCompat` ecoute
`GeoRenderEvent.Entity.Pre`. Si un renderer Epic Fight existe et que le patch
demande `overrideRender`, il **annule le rendu Geo** et appelle le rendu de
l'armature Epic Fight. Cela evite deux corps rendus, mais le modele/clip GeckoLib
n'est pas automatiquement repris. `GeoModelTransformer` traite notamment les
armures Geo ; ce n'est pas un convertisseur general de clips ou de creatures.

| Situation | Decision / risque |
| --- | --- |
| Mob Epic Fight sans renderer/controller Geo | Chemin recommande et teste ici pour le prototype. |
| Mob GeckoLib sans patch/rendu Epic Fight | Coexistence possible au niveau architecture, a tester avec un binaire GeckoLib 4 compatible 1.21.1/NeoForge. Ses attaques custom restent serveur. |
| Meme mob avec deux moteurs sur son corps | A eviter : le rendu Geo peut etre annule, silhouette remplacee ou clocks/transitions divergentes si un addon contourne ce chemin. |
| Armure Geo sur humanoide Epic Fight | Pont upstream present ; UV, bones, texture, couches et versions restent a valider avec l'armure concrete. |
| Accessoire anime (cape, cheveux, queue) | Couche specialisee possible ; doit recevoir la transformation du joint parent Epic Fight. Aucun tel pont Murim livre ici. |
| `.geo.json` + `.animation.json` fournis seuls | Pas directement un mesh/armature/clip Epic Fight. Retargeting, skinning et export distincts requis. |

Les animations GeckoLib declenchees depuis le serveur sont documentees
[ici](https://github.com/bernie-g/geckolib/wiki/Triggerable-Animations-%28Geckolib4%29).
Cela synchronise leur lecture, pas les colliders Epic Fight ni les degats a notre
place. Un event visuel ne constitue jamais une autorisation de dommage.

Sans GeckoLib installe, nous ne pouvons pas annoncer une compatibilite runtime
validee. Avant de l'ajouter : choisir et epingler sa release NeoForge 1.21.1,
tester client/serveur, un mob Geo sans patch, puis l'armure/accessoire precis vise.
Garder Epic Fight seul si aucun besoin concret ne justifie une seconde dependance.

## Creer Notre Premiere Animation

1. Fournir design, texture originale, dimensions, main/armee et droit de redistribution.
2. Verifier avec le developpeur le rig avant d'animer : humanoide BIPED ou rig custom.
3. Produire le modele Minecraft dans Blockbench/Blender, preparer armature et poids.
4. Creer seulement idle + une attaque pilote, avec une pose de debut/fin raccordee.
5. Exporter mesh/armature/clip avec l'[exporteur officiel Blender](https://github.com/Antikythera-Studios/blender-json-addon).
6. Remettre `.blend`, textures/UV, exports, versions exactes Blender/exporteur,
   video face/profil avec FPS, mouvement racine et reperes des phases.
7. Le developpeur cree les IDs `murimblock:...`, declarations API, collider, phases,
   IA, rendu et tests serveur. Aucun besoin que tu ecrives ces fichiers techniques.
8. Tester l'aller-retour dans Minecraft avant marche/course, combos ou defenses.

Pour le prototype, nous referencons les IDs de la dependance installee : aucun
fichier d'animation/mesh/texture Epic Fight n'est extrait puis redistribue ici.
Les [conditions des assets](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS)
sont distinctes de celles du code. Pour copier/modifier/repackager ces ressources,
obtenir un accord ecrit des titulaires, pas seulement le droit d'utiliser Java.

## Verification Et Suite

Automatise : chargement patch/attributs, un seul goal d'attaque anime, sauvegarde
de l'equipement, pas de recompense, un contact de lame, IA autonome et rendu reel.
Voir [le compte rendu de livraison](DELIVERY_CLEAR_MANUSCRIPT_MOBS.md) pour les runs.

Manuel avant promotion sur main : navigation GUI a tes resolutions, garde/esquive
contre le prototype, obstacles/escaliers/eau, mort en pleine attaque, changement
d'arme, relog et dimensions. Deux vrais clients sur serveur dedie doivent comparer
cible, debut/fin, degats uniques et arrivee tardive. Un serveur de test avec
connexions embarquees et un client solo ne remplacent pas cette verification.

Prochaine tranche conseillee : choisir un premier bandit humanoide et sa texture,
adapter un seul rig/renderer, puis un seul coup original. Pas de spawn naturel
ni de conversion globale des mobs avant validation. Garde NPC, quatre M1/styles
joueur et clash physique restent des travaux distincts, non livres ici.

## Sources Consultees

- [Release Epic Fight utilisee](https://modrinth.com/mod/epic-fight/version/21.17.3.1-mc1.21.1-neoforge).
- [Sources officielles 1.21.1](https://github.com/Antikythera-Studios/epicfight/tree/1.21.1) : `AnimationManager`, `AttackAnimation`, `MobPatchReloadListener`, `MobPatch`, `HumanoidMobPatch`, `ZombiePatch`, `TargetChasingGoal`, `EpicFightAttributes`, `RenderEngine`, `GeckolibCompat`, `GeoModelTransformer`. Compares aux sources du jar epingle ; branche mouvante, pas version runtime garantie.
- [Entites et presets](https://epicfight-docs.readthedocs.io/Guides/Entities/page1/) : orientation generale ; schema verifie dans le moteur epingle.
- [Exporteur Blender](https://github.com/Antikythera-Studios/blender-json-addon) : procedure generale, exporter a epingler lors du premier export.
- [Controllers GeckoLib 4](https://github.com/bernie-g/geckolib/wiki/The-Animation-Controller-%28Geckolib4%29) et [animations declenchables](https://github.com/bernie-g/geckolib/wiki/Triggerable-Animations-%28Geckolib4%29) : documentation, pas un test binaire de notre integration.
- [Conditions des ressources Epic Fight](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS).
