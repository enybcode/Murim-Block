# Forêt des pics de jade (`murimblock:jade_karst_forest`) — grand biome

## Inspiration

**Le paysage réel.** Ce biome reprend le karst à tours de Guilin et Yangshuo, le long de la rivière Li (Guangxi). C'est le site UNESCO « Karst de Chine du Sud ». La rivière Li fait 164 km et descend doucement de 141 m à 103 m : elle est large et lente, avec des bancs de galets. Le socle est un calcaire dévonien et carbonifère d'environ 2 600 m d'épaisseur. On y trouve deux formes :
- les **fenglin** (« forêt de pics ») : des tours isolées de 30 à 80 m qui jaillissent d'une plaine alluviale ;
- les **fengcong** (« amas de pics ») : des cônes soudés à la base, avec des dolines entre eux, pouvant atteindre environ 300 m.

**La roche et le climat.** La roche est gris pâle, striée de noir et d'ocre là où ruisselle l'eau. Les parois sont presque verticales et les tours creusées de grottes. Le climat est subtropical humide, avec des brumes matinales et des bruines au printemps, sans neige.

**À l'époque féodale.**
- Les pentes, trop raides pour être cultivées, restaient boisées.
- Les plaines étaient déjà des rizières et des villages sous les Tang et les Song.
- Han Yu (Tang) décrit la rivière comme un ruban de soie verte et les collines comme des épingles de jade. Le nom du biome vient de là.
- Xu Xiake (Ming) décrit le karst dans ses carnets de voyage.
- « Guilin » signifie « forêt d'osmanthus ».

## Dans le jeu

- **Relief.** Le biome occupe les terres chaudes et humides, tous reliefs confondus. Les tours karstiques sont générées par `RockPillarFeature`, style TOWER :
  - rayon de 4 à 8 blocs, hauteur de 24 à 58 blocs ;
  - forme cylindrique légèrement bombée et sommet en dôme boisé ;
  - stries verticales de calcaire moussu et de tuf, et lianes sur les faces.
  - Les tours sont espacées pour rester isolées, comme dans le fenglin.
- **Sol.** Le calcaire karstique remplace la pierre sur 26 blocs sous la surface, si bien que les berges et les falaises sont en calcaire. Les pentes raides sont en calcaire nu ou moussu. Le fond des eaux est en gravier et en argile.
- **Climat et couleurs.**
  - température 0,95, humidité 0,9 ;
  - ciel #8FB8C8, brouillard #C9D6D3 ;
  - eau vert jade #3E8F7E ;
  - herbe #5FA046, feuillage #3F8A3A.
- **Brume.** Moyenne : le brouillard est ramené à 55 % de sa distance normale.
- **Faune.** Cochons, poules, vaches (pour le buffle d'eau), perroquets, saumons. Les crabes cuirassés y apparaissent aussi.
- **Musique.** Celle de la jungle vanilla.

## Flore

| Plante | Bloc du mod | Notes |
|---|---|---|
| Osmanthus odorant (*Osmanthus fragrans*) | `osmanthus_log`, `_leaves`, `_planks`, `_sapling` | Couronne ronde et dense, petites fleurs dorées. C'est l'arbre le plus courant. |
| Banian (*Ficus microcarpa*) | `banyan_*`, `banyan_roots` | Large couronne avec des racines aériennes qui pendent. Il existe une forme géante 2×2 (4 pousses), rare à l'état sauvage. |
| Bambou en touffes des berges | bambou vanilla (`bamboo_no_podzol`) | Le long des rivières. |
| Fougère karstique | `karst_fern` | Sous-bois des tours. |
| Herbe haute et fougère | vanilla | |

## Structures prévues

1. **Village zhuang ou yao sur pilotis** (ganlan, diaojiaolou).
   - Modèles réels : Ping'an et Dazhai sur les rizières de Longji.
   - Maisons de 11×9 blocs sur 3 blocs de pilotis.
   - Murs en sapin, toit à deux pentes en tuiles grises, balcon avec garde-corps, grenier.
   - Les rizières descendent en terrasses.
   - Faction : clans de la rivière, herboristes et empoisonneurs du Sud.
2. **Pont de pierre à une arche et embarcadère.**
   - Modèle réel : le pont Yulong, construit en 1412, d'environ 59 m.
   - Arche semi-circulaire de 9 blocs, tablier en dos d'âne, banian au pied.
   - Lieu neutre pour des duels et des échanges.
3. **Sanctuaire troglodyte avec inscriptions.**
   - Modèles réels : la colline Fubo et la grotte des Sept Étoiles, avec des niches bouddhiques et de la calligraphie Tang–Song.
   - Petite salle à 3 travées adossée à l'entrée de la grotte, toit vert.
   - Un manuel martial scellé au fond.
4. **Pont couvert et tour du tambour dong** (en option).
   - Modèle réel : le pont « du vent et de la pluie » de Chengyang, de 64 à 78 m, avec 5 pavillons.

## À ajouter plus tard

- **Mobs :**
  - pêcheur au cormoran sur un radeau de bambou (PNJ) ;
  - buffle d'eau ;
  - langur de François, le singe des falaises karstiques ;
  - poissons des grottes.
- **Objets wuxia :**
  - thé et vin d'osmanthus ;
  - pierre de jade de rivière ;
  - herbes des grottes.
- **Relief :**
  - variante fengcong avec des amas de cônes et des dolines ;
  - arches naturelles comme la colline de la Trompe d'éléphant ;
  - grottes à concrétions sous les tours.
- **Sons :** brume matinale et clapotis.

## Sources

- https://en.wikipedia.org/wiki/South_China_Karst (résumé de la fiche UNESCO)
- https://en.wikipedia.org/wiki/Li_River
- https://geoexpro.com/crowns-of-nature-the-majestic-landscape-of-guilin/
- https://national-parks.org/china/guilin-lijiang
- https://www.worldheritagesite.org/list/South+China+Karst
- https://structurae.net/structures/20011805 (pont de Chengyang)
- Han Yu, poème sur Guilin (Tang). Xu Xiake, carnets de voyage (Ming).
