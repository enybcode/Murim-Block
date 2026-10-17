# Proposition : plantes Murim, jardins et alchimie

Statut : **proposition de conception, non implémentée**. Idée discutée avec Colleuhu95 le 17 octobre 2026, publiée à sa demande pour la revue d'Enzo. Les durées, bonus et plafonds proposés ci-dessous restent à équilibrer ; ce document ne change ni les plantes, ni les recettes, ni la cultivation en jeu.

**Je partirais sur un système où les plantes servent à préparer sa progression : explorer, cultiver, raffiner, puis assimiler le Qi.** Les ressources rares doivent donner envie de voyager et les jardins de secte doivent avoir une vraie utilité.

Ce qui suit est une **proposition de fonctionnement**, avec des chiffres de départ à équilibrer. Je m’appuie sur la flore et la cultivation déjà présentes dans Murimblock.

**La base : distinguer trois effets sur le joueur.**

| Ce que la préparation améliore | Effet concret |
|---|---|
| **Réserve de Qi** | Récupérer l’énergie dépensée pour utiliser ses techniques. |
| **Cultivation, donc Qi maximal** | Progresser vers la prochaine étape et pouvoir stocker davantage de Qi. |
| **Puissance temporaire** | Obtenir un avantage limité avant un combat : récupération, résistance ou dégâts. |

Dans le projet actuel, les royaumes donnent déjà des bonus de santé, dégâts, armure et vitesse. Les grandes percées passent par le démon du cœur. **Les plantes doivent accompagner cette progression**, sans permettre d’obtenir indéfiniment des statistiques en mangeant des ingrédients. [Règles actuelles de cultivation](../../src/main/java/com/murimblock/cultivation/CultivationRules.java)

**Pour les plantes, je recommande un mélange de cueillette et de culture.**

Tout rendre cultivable facilement réduirait l’intérêt des expéditions. Tout réserver à la cueillette rendrait les jardins inutiles. Le compromis serait :

- les ingrédients courants se cultivent facilement ;
- les ingrédients rares nécessitent de reproduire leur milieu ;
- les spécimens exceptionnels nécessitent aussi un lieu naturellement riche en Qi.

Voici une répartition fondée sur les plantes existantes. **Les fonctions alchimiques indiquées sont proposées pour le jeu.**

| Plante ou ressource | Où elle pousse et conditions proposées | Rareté | Utilité alchimique |
|---|---|---|---|
| **Chrysanthème** | Vallée des pêchers ; terre fertile, lumière, humidité modérée | Commune | Stabiliser les mélanges et les préparations de méditation |
| **Pousses de bambou** | Shu ; sol humide, proximité de bambous | Commune | Préparations de récupération et ingrédients secondaires |
| **Astragale** | Collines aux camphriers ; terre cultivée et humide | Commune | Fertilité des jardins et base de préparations simples |
| **Argousier** | Plateau de lœss ; terrain drainé, soleil | Peu commune | Soins et récupération physique |
| **Lotus** | Jiangnan ; bassin d’eau calme avec fond vaseux | Peu commune | Rassemblement et circulation du Qi |
| **Fougère karstique** | Forêt de jade ; ombre, mousse et humidité | Commune | Préparations de purification |
| **Cistanche** | Désert du Hexi ; sable chaud, près du saxaoul | Rare | Préparations puissantes de cultivation et de percée |
| **Rhododendron du Huangshan** | Pics de la mer de nuages ; altitude, sol montagneux et humidité | Rare | Stabilisation des préparations de haut niveau |
| **Lotus des neiges** | Tianshan et Kunlun ; froid, altitude et sol rocheux | Rare ; spécimens anciens exceptionnels | Alchimie avancée et préparation aux grandes percées |
| **Laîche alpine** | Prairies froides du nord | Commune dans son milieu | Ingrédient auxiliaire des recettes utilisant les plantes du froid |

Une plante rare doit être rare **dans son habitat favorable**, pas simplement difficile à trouver parce qu’elle apparaît partout avec une chance minuscule. L’herboriste apprend donc à reconnaître les bons endroits.

**La croissance aurait quatre étapes lisibles.**

**Graine ou bouture → jeune pousse → plante mature → maturité spirituelle.**

La croissance dépend de quatre choses : **sol, humidité, lumière et température**. La maturité spirituelle demande en plus du Qi ambiant.

- Conditions adaptées : croissance normale.
- Conditions imparfaites : croissance ralentie, qualité limitée.
- Conditions incompatibles : croissance arrêtée, puis dépérissement après un délai visible.
- Bon environnement spirituel : possibilité d’atteindre une qualité supérieure.

Pour commencer, je proposerais environ **20–40 minutes de croissance pour une plante courante**, **1–2 heures pour une rare** et plusieurs heures pour une maturité spirituelle. Ce sont des durées de fonctionnement du monde dans une zone active : dormir ne fait pas mûrir instantanément le jardin.

La poudre d’os peut aider la croissance ordinaire. **Elle ne remplace ni le vieillissement spirituel ni les conditions du milieu.**

Récolter les fleurs ou les baies conserve généralement la plante et permet une repousse. Récolter une racine entière détruit le pied : il faut choisir entre une ressource immédiate et une production durable.

**La rareté, l’âge et la qualité seraient trois informations différentes.**

Un lotus des neiges mal cultivé reste une espèce rare, mais donne un mauvais ingrédient. Un chrysanthème bien entretenu peut être excellent sans devenir une plante légendaire.

Chaque récolte indique :

- **son espèce**, qui détermine ses usages ;
- **sa maturité**, qui détermine sa puissance disponible ;
- **sa pureté**, qui détermine les impuretés transmises à la préparation.

Une bonne qualité doit surtout améliorer le rendement et réduire la toxicité. Elle ne doit pas multiplier toutes les statistiques.

Pour les spécimens « centenaires », l’âge peut être un **rang spirituel de fiction**, clairement distingué du temps réellement attendu en jeu.

**Les jardins de secte permettraient de maîtriser ces conditions.**

Un jardin comporterait plusieurs espaces : bassin à lotus, parcelles humides, jardin sec, serre froide ou terrasse de montagne.

On peut transporter une plante et aménager son milieu. En revanche, un lieu de pouvoir conserve un avantage pour les meilleures récoltes. Son apport de Qi est **partagé entre les plantes** : entasser cent plantes au même endroit ralentit leur maturation spirituelle.

Cela donne une utilité aux territoires et aux échanges entre sectes. Une secte du désert peut produire de bonnes ressources du Hexi et acheter les ingrédients du nord.

**Le chaudron serait une vraie étape de préparation.**

Une recette repose sur des rôles précis :

| Élément | Rôle |
|---|---|
| **Ingrédient principal** | Détermine l’effet recherché |
| **Assistant** | Renforce ou complète cet effet |
| **Harmonisant** | Stabilise le mélange et limite les impuretés |
| **Liquide et éventuel liant** | Permettent l’extraction, puis la formation d’un élixir ou d’une pilule |

Le joueur suit quatre opérations : **préparer les ingrédients, extraire à la bonne température, stabiliser, puis raffiner**.

Une recette découverte indique les ingrédients, l’ordre et les plages de température. Le résultat dépend de ce que fait le joueur : une préparation correctement exécutée ne devrait pas échouer à cause d’un pourcentage caché.

Je proposerais trois niveaux d’équipement :

- **Chaudron simple** : décoctions, soins et récupération.
- **Chaudron alchimique gravé** : pilules de cultivation, contrôle plus précis de la chaleur et infusion de Qi.
- **Chaudron supérieur** : ingrédients très concentrés et préparations avancées.

Un meilleur chaudron facilite la maîtrise. Il ne transforme pas une mauvaise récolte en ingrédient exceptionnel.

Une surchauffe légère dégrade la pureté ; une erreur importante produit des résidus inutilisables. L’infusion consomme la **réserve de Qi** de l’alchimiste et ne lui rapporte pas automatiquement du Qi maximal.

**Les préparations auraient des fonctions séparées.**

| Préparation | Effet proposé | Limite |
|---|---|---|
| **Décoction de récupération** | Restaure progressivement une partie de la réserve de Qi | Même effet non cumulable |
| **Préparation médicinale** | Rend de la vie progressivement | Le soin s’interrompt ou diminue pendant les dégâts |
| **Pilule de rassemblement du Qi** | Augmente le Qi maximal ; base actuelle de **+10** avant toxicité | Apport permanent plafonné |
| **Encens de méditation** | Exemple : **+15 % de condensation pendant 10 minutes** | Une seule source active ; respecte la fatigue de méditation |
| **Pilule de rupture** | Aide à supporter les coups du démon du cœur | Ne gagne pas l’épreuve à la place du joueur |
| **Élixir martial** | Exemple : **+10 % de dégâts pendant 60 secondes** | Un seul élixir martial actif, avec toxicité |
| **Préparation purifiante** | Accélère temporairement l’élimination des impuretés | Ne remet pas instantanément la toxicité à zéro |

Pour éviter de devenir puissant uniquement en consommant des stocks, je proposerais que **les gains directs de Qi maximal provenant des pilules couvrent au maximum 20 % de la progression d’un royaume**. Toutes les recettes de progression partagent cette limite.

Exemple : entre **100 et 250 de Qi maximal**, cela représente **30 points au maximum** venant directement des pilules. Le reste vient de la méditation, du combat, des missions et des épreuves. Ce plafond serait une nouvelle règle à valider.

**La toxicité donnerait un coût aux préparations puissantes.**

Murimblock possède déjà une base utile : une pilule ajoute actuellement **30 de toxicité**, celle-ci diminue de **1 par minute**, et elle réduit l’efficacité des pilules suivantes.

Je conserverais ce principe en faisant varier les impuretés avec la pureté du produit :

- préparation ordinaire : bénéfice correct, davantage d’impuretés ;
- préparation raffinée : bénéfice mieux conservé, moins d’impuretés ;
- consommation excessive : efficacité réduite, puis effets nocifs.

Les remèdes de purification doivent fonctionner lentement et ne pas se cumuler. Sinon, le joueur pourrait alterner pilule et antidote sans aucune limite.

**Le culte du Sang aurait sa propre méthode alchimique dans ce système commun.**

Il pourrait employer des recettes plus agressives : récupération après combat, poussée de puissance temporaire ou préparation aux techniques sanguines. En échange, elles produiraient davantage d’impuretés ou une fatigue après leur effet.

Cela donnerait une identité au culte, tout en gardant une progression comparable à celle des autres factions.

**Concrètement, une session pourrait se dérouler ainsi :** le joueur récolte du lotus dans le Jiangnan, entretient son chrysanthème au jardin et récupère de l’argousier. Il raffine une pilule au chaudron, vérifie sa pureté et sa toxicité, puis la consomme pour passer de **220 à 230 de Qi maximal**. Il continue sa progression jusqu’au seuil de **250**, prépare sa percée et affronte le démon du cœur. **La puissance permanente vient ensuite du royaume atteint.**
