# Les factions du Murim — proposition (à valider par Enzo)

> **Statut : proposition de conception, rien n'est construit.** Rédigée le 15/10/2026 après les décisions d'Enzo :
> - deux factions pour la bêta : le camp **orthodoxe** et le camp **démoniaque**, celui des arts du sang ;
> - l'**Alliance démoniaque dans le désert** et l'**Alliance orthodoxe au milieu, dans les montagnes sacrées** ;
> - « on n'est pas pressés ».
>
> Les noms anglais sont ceux qui apparaîtraient en jeu. Tout le reste est à discuter.

Objectif bêta :
- 3 boss jouables ;
- 2 factions avec leur capitale ;
- 12 arts avec leur progression ;
- au moins 20 mobs.

Les factions sont la colonne vertébrale : les arts, les mobs et les boss s'y rattachent.

## Les deux camps

### L'Alliance orthodoxe — *Orthodox Alliance* (正派武林盟)

- **Qui.** Les sectes « droites » du Murim, unies sous un Chef de l'Alliance (盟主) : sectes d'épée, monastère bouddhiste, clans de lance et de bâton. Elles gardent les routes, chassent les bandits et traquent le Culte.
- **Valeurs.** La droiture, la discipline, la maîtrise du Qi par la méditation. On progresse en servant la secte.
- **Capitale : la Citadelle du Pic céleste — *Heavenly Peak Citadel* (天峰城).**
  - Où : au centre de la carte, dans les Pics de la mer de nuages (`cloud_sea_peaks`), les montagnes sacrées, inspirées du Huangshan et du mont Hua.
  - Construite sur plusieurs aiguilles de granite reliées par des passerelles et des escaliers taillés dans la falaise, au-dessus de la mer de nuages.
  - Lieux :
    - la Grande Salle de l'Alliance ;
    - la bibliothèque des sectes (où l'on apprend les arts) ;
    - les cours d'entraînement ;
    - un monastère ;
    - une porte fortifiée au pied, avec un marché.
  - Le sanctuaire de la Falaise des Nuages, déjà dans ce biome, deviendrait un avant-poste de l'Alliance.

### Le Culte démoniaque — *Heavenly Demon Cult* (天魔神教)

- **Qui.** Les écoles « hétérodoxes » réunies sous le Démon céleste (天魔) : arts du sang, poisons, chaînes, ombres. Chassés des terres centrales, ils se sont retirés dans le désert de l'ouest.
- **Valeurs.** La force avant tout ; on monte en rang en prouvant sa puissance, parfois aux dépens de ses frères.
- **Capitale : la Forteresse des Sables pourpres — *Crimson Sand Fortress* (赤沙堡).**
  - Où : à l'ouest, dans le désert de la frontière du Hexi (`hexi_frontier_desert`).
  - Inspiration :
    - les grottes de Mogao à Dunhuang, des centaines de grottes creusées dans une falaise du désert ;
    - les forts de terre crue de la route de la soie, comme la passe de Yumen.
  - Une falaise de grès creusée de grottes sur plusieurs étages, devant elle une enceinte de terre crue rouge, au milieu des yardangs.
  - Lieux :
    - la Salle du Démon céleste au fond de la falaise ;
    - le Bassin de sang (lieu de l'Art Sanguin) ;
    - des grottes-bibliothèques ;
    - une arène ;
    - un marché noir.

### Rejoindre une faction (proposition)

- **Neutre au départ.** On rejoint un camp en se présentant à sa capitale et en réussissant une épreuve d'entrée. Pour l'Alliance : vaincre un groupe de bandits. Pour le Culte : survivre à l'arène.
- **Rangs** : Disciple externe → Disciple interne → Disciple de cœur → Ancien.
  - On monte avec la réputation (quêtes de faction, ennemis du camp adverse vaincus) et le royaume de cultivation.
  - Chaque rang ouvre de nouveaux arts de la faction.
- **Hostilité.** Les gardes de la capitale adverse attaquent les membres de l'autre camp. Avec des amis, les deux camps peuvent s'affronter en PvP, avec la mise à terre et le finish existants.
- **Changer de camp** : possible une fois, au prix de la trahison : perte du rang et capitale d'origine hostile. À trancher.

## Les 12 arts

Répartition proposée : 5 orthodoxes, 5 démoniaques, 2 libres (trouvables partout, comme le Poing Long). Des armes variées, pour que chaque art se joue différemment.

| # | Art (nom en jeu) | Camp | Arme | Style de jeu | État |
|---|---|---|---|---|---|
| 1 | Azure Cloud Sword | Orthodoxe | Épée | Pas et coupes aériennes, précision | ✅ fait |
| 2 | Golden Bell Palm | Orthodoxe | Mains nues | Moine : paumes lourdes, corps d'airain, encaisser | à faire |
| 3 | Pine Wind Staff | Orthodoxe | Bâton | Allonge, balayages, contrôle de foule | à faire |
| 4 | Celestial Peak Spear | Orthodoxe | Lance | Charges, percées à longue portée | à faire |
| 5 | Moon Lotus Fan | Orthodoxe | Éventail de fer | Rafales de vent, soutien, soin léger | à faire |
| 6 | Sanguine Art (Blood Crystal) | Démoniaque | Mains nues | Cristaux de sang, forme cristalline | ✅ fait (Colleuhu95) |
| 7 | Heavenly Demon Saber | Démoniaque | Sabre (dao) | Puissance brute, art du chef du Culte | à faire |
| 8 | Withering Chain | Démoniaque | Fouet-chaîne | Attraper, tirer, étrangler à distance | à faire |
| 9 | Hundred Venoms Hand | Démoniaque | Aiguilles | Poison, attaques à distance, usure | à faire |
| 10 | Shadow Step Daggers | Démoniaque | Dagues | Assassin : disparitions, coups dans le dos | à faire |
| 11 | Long Fist | Libre | Mains nues | L'art le plus répandu, polyvalent | ✅ fait |
| 12 | Wind-Chasing Arrows | Libre | Arc | Archer nomade des steppes | à faire |

Chaque art suit le même gabarit que les deux premiers :
- un rouleau et un passif ;
- 3 techniques et un secret ;
- des quêtes de déblocage, de la maîtrise et des évolutions ;
- un finish.

Ce gabarit commun permettra d'enchaîner plus vite.

## Les boss (3 au moins)

| Boss | Où | Rôle |
|---|---|---|
| Sensei Yun | Temple de la mer de bambous | ✅ fait : maître de l'Azure Cloud Sword |
| Chef de l'Alliance — *Alliance Leader* | Grande Salle, Citadelle du Pic céleste | Boss orthodoxe : épreuve ultime pour les orthodoxes (devenir Ancien), cible pour le Culte |
| Démon céleste — *Heavenly Demon* | Salle du Démon céleste, Forteresse des Sables pourpres | Boss démoniaque : Heavenly Demon Saber et sang, deux phases ; même logique inversée |
| (Bonus) Le mystère des glaces | Montagnes du Kunlun | Boss caché pour plus tard |

Le Chef Bandit reste un mini-boss.

## Les mobs (20 au moins)

**Existants (6) :**
- crabe cuirassé ;
- homme-crabe ;
- bandit des montagnes ;
- Chef Bandit ;
- Sensei Yun ;
- démon du cœur.

**De faction (6) :**
- Alliance :
  - disciple orthodoxe (épée) ;
  - moine guerrier (paumes) ;
  - garde de la Citadelle (lance).
- Culte :
  - fidèle du Culte (sabre) ;
  - guerrier de sang (Art Sanguin) ;
  - empoisonneur des ombres (aiguilles, dagues).

**De biome (9), un pour chaque biome qui n'en a pas :**

| Biome | Mob | Idée |
|---|---|---|
| Champs de neige du Tianshan | Loup des neiges | En meute, dans la neige profonde |
| Glaciers du Kunlun | Gardien de jade | Statue de jade blanc qui s'éveille |
| Désert du Hexi | Pillard des sables | Cavalier nomade, arc |
| Plateau de lœss | Gardien de tombe | Soldat de terre cuite des tombes impériales |
| Forêt des pics de jade | Singe des falaises | Vole des objets, saute de tour en tour |
| Mer de bambous de Shu | Panda géant | Neutre, terrible s'il est provoqué |
| Pays des eaux du Jiangnan | Fantôme des eaux (水鬼) | Tire sous l'eau ceux qui longent les canaux |
| Vallée des pêchers | Esprit renard (狐狸精) | Illusions et charmes |
| Collines aux camphriers | Tigre de Chine du Sud | Chasseur solitaire des lisières |

Total : 6 + 6 + 9 = **21 mobs**.

## Ordre de travail proposé

1. Valider ce document : les noms, les capitales, les arts, les règles de faction.
2. **Les deux capitales** : maquettes en images d'abord, puis construction (même kit que le sanctuaire et les sites).
3. **Le système de faction** : rejoindre, rangs, réputation, hostilité des gardes, données sauvegardées et protocole réseau.
4. **Les 6 mobs de faction**, qui peuplent les capitales.
5. **Les arts**, un par un, à partir du gabarit commun.
6. **Les 2 boss chefs.**
7. **Les mobs de biome.**
8. Préparation de la bêta : serveur dédié, pack de mods, test à deux vrais clients du parcours complet.

## Questions ouvertes

- Peut-on rester neutre toute la partie ? Et changer de camp ?
- Les membres d'un même camp peuvent-ils s'affronter en PvP ?
- Les arts libres : l'arc convient-il, ou préfères-tu une autre arme (sabre de vagabond, gourde d'ivrogne…) ?
- Les noms chinois et anglais des capitales et des boss.
