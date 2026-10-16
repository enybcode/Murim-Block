# Pics de la mer de nuages (`murimblock:cloud_sea_peaks`) — biome moyen

## Inspiration

**Le paysage réel.** Ce biome reprend le Huangshan, les « monts Jaunes » de l'Anhui, site UNESCO mixte depuis 1990.
- 77 sommets dépassent 1 000 m. Le plus haut, le pic du Lotus, fait 1 864 m.
- Le granite est grossier et clair, gris pâle à chamois. Ses diaclases verticales le débitent en parois, piliers et aiguilles.
- Climat :
  - environ 2 450 mm de pluie sur 180 jours ;
  - de la neige environ 31 jours par an ;
  - des nuages ou du brouillard au sommet de 200 à 256 jours par an.
- La **mer de nuages** (yunhai) apparaît quand les pics dépassent une couche nuageuse.
- Le pin du Huangshan domine entre 800 et 1 800 m.

**À l'époque féodale.**
- Le paysage était presque le même qu'aujourd'hui, sans téléphériques. Les escaliers de pierre et les temples existaient dès les Tang.
- Xu Xiake l'a gravi en 1616 et 1618.
- Au XVIIe siècle, l'école de peinture du Huangshan (Hongren, Mei Qing, Shitao) en a fait sa référence visuelle.

## Dans le jeu

- **Relief.** Le biome occupe les montagnes fraîches de l'intérieur des terres (érosion 0 à 2), plus les créneaux tempérés humides les plus montagneux (érosion 0). Les côtes fraîches restent vanilla. Les aiguilles de granite sont générées par `RockPillarFeature`, style SPIRE :
  - rayon de 3 à 6 blocs, hauteur de 28 à 64 blocs ;
  - blocs décalés tous les 6 blocs pour imiter les diaclases ;
  - rétrécissement par paliers et sommet tranché.
- **Pins de falaise.** `CliffPineFeature` cherche des parois verticales et y fait pousser un pin de côté : son tronc sort d'une fissure, se redresse, et porte deux nuages plats d'aiguilles, comme le célèbre « pin qui accueille les hôtes ».
- **Sol.**
  - Le granite pâle remplace la pierre sur 48 blocs, et les pentes raides sont en granite nu.
  - Mousse par plaques sur les replats, neige au-dessus de l'altitude 170.
- **Climat et couleurs.**
  - température 0,3 (neige en altitude), humidité 0,9 ;
  - ciel #A9B8C2, brouillard #DCE3E6 ;
  - herbe #6B9A5A, feuillage #3D6B44.
- **Brume.** La plus dense des sept : le brouillard tombe à 38 % de sa distance normale. Les aiguilles s'effacent les unes derrière les autres, comme dans la peinture « monochrome à l'encre ».
- **Faune.** Chèvres, lapins.
- **Musique.** Celle des pics déchiquetés (jagged peaks).

## Flore

| Plante | Bloc du mod | Notes |
|---|---|---|
| Pin du Huangshan (*Pinus hwangshanensis*) | `huangshan_pine_*` | Tronc courbé, couronne en nuages plats étagés, aiguilles vert-bleu sombre, écorce en plaques brun-rouge. Il pousse aussi sur les parois. |
| Rhododendron du Huangshan (*R. maculiferum anwheiense*) | `rhododendron` (2 blocs) | Boutons roses qui s'ouvrent en blanc, en avril et mai. |
| Herbe, fougère, mousse | vanilla | |

## Structures prévues

1. **Chemin de planches à flanc de falaise** (zhandao), menant à un sanctuaire taoïste au sommet.
   - Modèles réels : le chemin Changkong du Huashan, le palais Nanyan du Wudang (UNESCO).
   - Planches sur des poutres scellées dans la roche, rampe en chaîne.
   - Terrasse et salle à 3 travées : colonnes rouges, toit vert vernissé aux angles relevés, brûle-parfum.
   - Factions : secte d'épée style Huashan, secte d'arts internes style Wudang. Le chemin de planches peut servir d'épreuve.
2. **Temple suspendu.**
   - Modèle réel : le Xuankong Si, environ 40 salles sur des porte-à-faux à environ 50 m du sol.
3. **Siège de la secte au sommet**, avec un « palais d'or ».
   - Modèles réels : le palais d'or du Wudang (1416) et les temples de l'Emei.
   - Cours en enfilade le long de la crête, tours de la cloche et du tambour, salle principale à double toit.
4. **Ermitage au toit de chaume et pavillon d'observation.**
   - Un pavillon est déjà sur la maquette.

## À ajouter plus tard

- **Vraie mer de nuages.** Une couche de brouillard dense à une altitude fixe, sous les sommets, via un shader Photon ou une bande de brouillard selon l'altitude du joueur.
- **Sources chaudes et cascades**, comme la cascade des Neuf Dragons.
- **Mobs :**
  - macaque du Tibet ;
  - faisan argenté ;
  - léopard nébuleux.
- **Arbres et plantes :**
  - mélèze doré (*Pseudolarix*, doré en automne) ;
  - magnolia du Huangshan ;
  - théier (thé Maofeng) en terrasses.
- **Objets wuxia :** thé Maofeng, gel de pin, champignons de falaise.

## Sources

- https://en.wikipedia.org/wiki/Huangshan
- https://en.wikivoyage.org/wiki/Huangshan
- https://whc.unesco.org/en/list/547/ (fiche UNESCO, consultée via des pages secondaires)
- https://whc.unesco.org/en/list/705 (Wudang)
- https://en.shaanxi.gov.cn/tourism/te/202110/t20211021_2195226.html (chemin de planches du Huashan)
- https://projects.mcah.columbia.edu/jaanus/record/sanen (les « trois distances » de Guo Xi)
