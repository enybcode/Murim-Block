# Arène céleste du Seigneur des bandits : le météore creusé

![Dans un vrai Minecraft, depuis le bord](en-jeu-02-from-the-rim.jpg)
![De nuit, éclairée par sa lave](en-jeu-06-at-night.jpg)

`warlord_sky_arena.schem` est le modèle réel de l'arène de la seconde phase du
[Seigneur des bandits](../../../docs/mobs/bandit-warlord.md#seconde-phase--larène-dans-le-ciel-maquette-du-1610-à-valider).
C'est la structure à construire. Les images de la fiche du boss (`docs/mobs/images/warlord-sky-arena/`) ne sont que la
maquette qui a servi à la choisir.

Format : WorldEdit Sponge v3, Minecraft Java 1.21.1 (DataVersion 3955), **blocs vanilla seulement**.

- Volume : **195 × 173 × 204** blocs (largeur × hauteur × profondeur).
- **Sol de combat plat : Ø 94** (6 917 cases), dont 6 687 sans rien au-dessus sur 30 blocs. Le reste est occupé par
  12 braseros et 9 rochers bas.
- **Cuvette** : le bord est à 72 blocs du centre, et les parois montent de **26 à 47 blocs** au-dessus du sol selon
  l'endroit (environ 40).
- **De la vraie lave** (1 026 sources) : un **anneau de lave** autour du sol de combat, au pied des parois ; une **mare**
  sur la cicatrice de l'atterrissage (obsidienne et obsidienne pleureuse autour) ; **14 fissures** qui en rayonnent ; et
  le dessus des veines des parois. Aucune ne peut couler : le magma reste en blocs de magma partout où il est ouvert sur
  le côté ou dessous (parois, dessous, coulées), sinon la lave dégoulinerait du météore jusqu'au sol.
- **Dessous**, un cône de basalte de 116 blocs, veiné de magma, avec des coulées qui pendent. Une vingtaine de rochers
  flottent autour.
- **Décor** :
  - 12 braseros en pierre noire polie et cuivre ciselé, avec un feu éternel sur netherrack ;
  - 6 bannières de laine pourpre et d'or sur des mâts de chêne noir ;
  - 34 pics de pierre noire sur le bord.
- Construction seulement : aucun mob, aucun coffre, aucune entité de bloc.

La lave du sol **brûle comme en vanilla** : on passe l'anneau et les fissures en sautant. C'est voulu ; on pourra la
remplacer par un bloc Murim si c'est gênant en combat.

## Importation (pour l'essayer)

1. Installer WorldEdit 7.3.8 dans un profil NeoForge 1.21.1.
2. Copier le `.schem` dans `config/worldedit/schematics` du profil.
3. Ouvrir un monde de test avec les commandes autorisées. En mode spectateur, on ne tombe pas avant le collage.

```text
/gamemode spectator
/tp @s 0 250 0
//schem load warlord_sky_arena
//paste -a
```

- **Le point de collage est le milieu du sol de combat**, debout dessus : le sol est à Y − 1 sous le joueur.
- Collé depuis Y = 250, le volume occupe **Y = 134 à 306**. La limite du monde est 320, donc ne pas coller plus haut
  que Y = 263.
- `-a` ne colle pas l'air : rien à creuser dans le ciel, et le collage est beaucoup plus rapide (1 475 785 blocs au
  lieu de 6 881 940 cellules).
- `//undo` annule le collage s'il est la dernière opération WorldEdit.

## Sources et vérification

- **Généré par** `node scripts/worldgen/warlord-sky-arena.cjs`. Même forme que la maquette
  (`scripts/concepts/warlord-sky-arena.js`), mais plein, en vrais blocs, avec un sol de combat parfaitement plat.
- **`en-jeu-*.jpg`** : photos dans un vrai client Minecraft. Le `.schem` y est collé bloc par bloc dans un monde plat,
  le sol à Y 250 (`MURIM_SKY_ARENA=1 gradlew -PgameTests runWeaponTour`, `SkyArenaTour`) : de loin, depuis le bord, sur
  le sol, l'anneau de lave, par-dessous et de nuit. Les nuages sont ceux de Minecraft.
- **`plans-de-l-arene.png`** est calculé depuis ses blocs : en haut, vu de dessus ; en bas, une coupe par le milieu.
  La ligne rouge marque la hauteur du sol de combat.
- **`export-stats.json`** : dimensions, palette et nombre de blocs, positions des braseros et des bannières,
  empreinte SHA-256.
- **`verification.json`** : les contrôles statiques (format, palette vanilla, sol dégagé, hauteur des parois). Le fichier
  a été relu par un décodeur NBT séparé : 6 881 940 cellules décodées, autant qu'attendu.
- **Collé dans un vrai monde** par `SkyArenaTour` (1 475 785 blocs placés). Pas encore essayé avec WorldEdit lui-même.
