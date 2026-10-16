# Murimblock

**Un mod Minecraft inspiré du Murim et du Wuxia, autour des arts martiaux,
du Qi et de l'exploration de paysages chinois.**

Murimblock construit progressivement un monde où la progression du personnage,
les combats animés et l'exploration se complètent. Son identité visuelle conserve
les formes et le pixel art de Minecraft, avec des interfaces de manuscrit épurées.

Le projet est **en développement** : le contenu ci-dessous est présent dans le
code, mais l'équilibrage et la validation multijoueur restent en cours.

## Explorer Le Monde

Sept biomes apportent leurs paysages, leur flore et leurs matériaux :

- **Jade Karst Forest** : tours calcaires, végétation et brume.
- **Cloud Sea Peaks** : aiguilles de granite et pins de falaise.
- **Shu Bamboo Sea** : forêts de bambous géants.
- **Jiangnan Waterlands** : étangs, lotus et saules.
- **Peach Blossom Valley** : pêchers et piliers de grès.
- **Loess Plateau** : terres de lœss et reliefs érodés.
- **Hexi Frontier Desert** : paysages arides, gravier et formations désertiques.

Ils s'accompagnent de nouveaux arbres, plantes, roches et sols. Un premier lieu,
le **Cloud Cliff Sanctuary**, complète ce travail sur le monde.

## Affronter Les Créatures

- **Armored Crab** : un crabe cuirassé aux deux pinces animées, présent près de l'eau.
- **Crab Man** : un humain infecté par le crabe, accompagné de deux crabes cuirassés.
  Il attaque avec sa grosse pince et projette une volée de boules d'eau.

Ces créatures disposent de modèles et d'animations propres. Epic Fight assure
leur combat animé ; GeckoLib anime les boules d'eau et Photon leurs effets lumineux.

## Cultiver Son Qi

Le joueur possède une réserve de **Qi**, une capacité maximale et des moyens
de régénération passive ou de charge active. Les combats permettent de gagner
du Qi selon les règles de récompense du mod.

La cultivation comprend **neuf royaumes et quatre étapes**, avec des conditions
de percée pour progresser. Une interface dédiée rassemble le profil, la
cultivation, les informations et les réglages, avec un HUD Qi discret.

Le combat repose actuellement sur **Epic Fight**, avec garde et roulade.
Ses interfaces et sa progression natives sont adaptées à Murimblock.
Les textes affichés en jeu sont en anglais.

## La Suite

Sur `test`, une première tranche d'art martial est présente : **Azure Cloud Sword**,
son manuel, **Cloud Step Slash**, l'onglet **Techniques** et une barre de compétences.
La garde Murim, le K.O. PvP, le portage et un finish générique sont aussi implémentés.
Cette tranche attend encore sa validation en jeu avant promotion dans `main`.

Les passifs/styles avancés, quatre coups M1 configurables, autres compétences,
quêtes et mouvements secrets restent à développer. Les affrontements physiques
entre les lames restent une direction future, pas du contenu livré.

La priorité est de stabiliser l'existant avant d'élargir cette progression.

## Jouer Et Suivre Le Projet

Le projet cible **Minecraft 1.21.1**, **NeoForge 21.1.248** et **Java 21**.
Il utilise Epic Fight 21.17.3.1, GeckoLib 4.9.3, Photon 2.2.8 et TerraBlender
4.1.0.8, ainsi que les dépendances de Photon, LDLib2 et KilaGraph.
Clients et serveur doivent utiliser les mêmes versions du mod et de ses dépendances.

**Sauvegardez votre monde avant un essai.** L'intégration retire certains objets
natifs d'Epic Fight ; consultez les [détails de compatibilité](docs/EPIC_FIGHT.md).

- **`test`** : changements à essayer et à valider.
- **`main`** : version stable après validation, pas nécessairement tout le contenu de `test`.
- [Documentation technique et guides de création](docs/README.md).
- [Là où je me suis arrêté : état et installation sur un autre PC](l%C3%A0%20o%C3%B9%20je%20me%20suis%20arr%C3%AAter/README.md).
- [Présentation détaillée des biomes](docs/biomes/README.md).

Le mod n'est pas encore présenté comme une alpha finalisée.

