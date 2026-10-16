# Vallée des fleurs de pêcher (`murimblock:peach_blossom_valley`) — biome moyen

## Inspiration

**Le récit.** *La Source aux fleurs de pêcher*, de Tao Yuanming (vers 421) :
- Un pêcheur de Wuling remonte un ruisseau bordé de pêchers. Les pétales couvrent le sol.
- Il se glisse par une grotte étroite et découvre une vallée cachée, avec de bons champs, de beaux étangs, des mûriers et des bambous.
- Les habitants y ont fui les guerres des Qin et ignorent tout des dynasties qui ont suivi.
- La tradition situe le lieu dans le comté de Taoyuan (Hunan).

**Le paysage choisi.**
- Pour donner au biome son propre relief, la vallée est ancrée dans le pays des **piliers de grès de Wulingyuan** (Zhangjiajie), tout proche.
- Plus de 3 000 piliers de grès quartzeux, dont beaucoup dépassent 200 m.
- Grès fauve à gris, en lits horizontaux, très différent du calcaire lisse du karst.
- Climat subtropical humide (environ 1 400 mm de pluie), brume fréquente, floraison en mars et avril.

**Les peintures.** Qiu Ying (XVIe siècle), Wen Zhengming, Ding Yunpeng (1582) et Gu Fuzhen (1706) ont tous peint *La Source aux fleurs de pêcher*. Le style « bleu-vert » (qinglü) est la référence de couleur.

## Dans le jeu

- **Relief.** Le biome occupe les terres tempérées à chaudes d'humidité moyenne, des vallées aux collines (érosion 1 à 6).
  - Les piliers de grès sont générés par `RockPillarFeature`, style PILLAR :
    - section carrée aux angles arrondis ;
    - rayon de 2 à 4 blocs, hauteur de 26 à 58 blocs ;
    - flancs droits à litage horizontal, sommet plat et vert.
  - Ils sont espacés pour former une forêt de colonnes, pas un mur.
- **Sol.**
  - Le grès des piliers remplace la pierre sur 22 blocs, avec une bande de grès lisse tous les 6 blocs.
  - Les pentes raides sont en grès.
- **Climat et couleurs.**
  - température 0,65, humidité 0,8 ;
  - ciel #B6CDE0, brouillard rosé #E7D9DE ;
  - eau #5AA8A0 ;
  - herbe #86C26A, feuillage #6DB458.
- **Brume.** Légère : le brouillard est ramené à 70 % de sa distance normale.
- **Faune.** Lapins, moutons, poules.
- **Musique.** Celle de la cerisaie (cherry grove).

## Flore

| Plante | Bloc du mod | Notes |
|---|---|---|
| Pêcher (*Prunus persica*) | `peach_*` | Petit arbre à rameaux sombres, d'un rose plus profond que le cerisier vanilla. Écorce rouge sombre luisante à lenticelles. Quelques premières feuilles vertes. |
| Pétales roses | vanilla `pink_petals` | Les pétales qui couvrent le sol, comme dans le récit. |
| Chrysanthème | `chrysanthemum` | La fleur emblème de Tao Yuanming (« cueillant des chrysanthèmes à la haie de l'est »). |
| Colza | `rapeseed` | Champs jaunes de printemps. |
| Herbe | vanilla | |

## Structures prévues

1. **Entrée par la grotte et embarcadère du pêcheur.**
   - Tunnel étroit de 2×3 blocs sur environ 20 blocs, de plus en plus clair vers la sortie.
   - Dehors : ruisseau bordé de pêchers, barque amarrée, stèle.
2. **Village paysan archaïque.**
   - Maisons en terre damée au toit de chaume, très légèrement courbé.
   - Haies de bambou, puits, rizières, bosquet de pêchers, petite salle des anciens sans peinture.
   - L'étrangeté du lieu, c'est qu'il est intact : aucun signe de rang.
3. **Moulin à eau et petit temple du dieu du sol** (tudi miao).
   - Noria de bambou (tongche) et sanctuaire de 3×3 blocs.
4. **Forteresse ronde en terre d'un clan caché** (en option).
   - Modèle réel : les tulou du Fujian (UNESCO).

## À ajouter plus tard

- **Fruits de pêcher** : récolte en été, et une « pêche d'immortalité » rare pour la cultivation.
- **Mobs :**
  - cerf d'eau ;
  - faisan doré ;
  - canard mandarin ;
  - macaques des falaises ;
  - villageois de la vallée (PNJ hors du temps).
- **Relief :**
  - vallée fermée par un anneau de piliers avec un seul passage par une grotte (générateur dédié) ;
  - pins de Masson au sommet des piliers.
- **Ambiance :** pétales qui tombent. On les ferait en modèle Blockbench, comme la boule d'eau, plutôt qu'avec des particules vanilla.

## Sources

- https://en.wikipedia.org/wiki/The_Peach_Blossom_Spring
- https://en.wikipedia.org/wiki/Wulingyuan
- https://silkqin.com/02qnpu/16xltq/xl169tyc.htm
- https://global.chinadaily.com.cn/a/201804/12/WS5acea09ca3105cdcf6517b8e.html
- https://en.wikipedia.org/wiki/Fujian_tulou
