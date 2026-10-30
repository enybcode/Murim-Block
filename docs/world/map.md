# Murim map (plan, step 0)

Enzo's brief (29 Oct 2026): the overworld is one fixed Murim continent, cold and empty in the north, lush in the
south, sea in the east. Source: Enzo's doc "Murim Block — Guide des biomes" (27 Oct 2026). Mockup:
[Carte du Murim](https://claude.ai/artifact/RmzwZsWZ5Vqwv98G3c39Fk) (private artifact; it runs a small version of the
algorithm below). Capitals come later, but their ground is reserved from step 2.

## Shape

- North is -Z. The continent is a rounded blob about 10 000 blocks wide, centred on 0,0, with bays and capes
  (no square edge). Ocean all around; the west ends in snow peaks, the east in beaches and capes.
- Same layout in every world (not random like V1). Only the small noise detail depends on the seed.
- Zone borders are wavy: a two-scale domain warp (bends of ~3 600 blocks up to ~600 blocks off, wiggles of
  ~950 blocks). Zones, rivers and reserved sites are all defined in warped space, so they always agree.
- Crossing west to east: about 30 min sprinting, 12 min on a horse.

## Zones and biomes

Coordinates are the guide's (warped space), approximate. Biome ids are `murimblock:` unless noted.
"From V1" means the V1 biome (branch `test`) is reused and adapted, with Enzo's OK of 29 Oct 2026.

| Zone | Where (X, Z) | Biomes | From V1 | Ground Y |
|---|---|---|---|---|
| Northern wastes | Z < -2100 | `frozen_steppe` (to -3000), `cold_desert` (to -4200), `frozen_tundra` | `tianshan_snowfields` (spruce, meadow), `hexi_frontier_desert` (Gobi gravel) | 78-90 |
| Qinghai | X < -2000, -2100 < Z < 0 | `qinghai_plateau`, `western_snow_peaks` (west edge) | `kunlun_glaciers` | 120-150 |
| Shaanxi | -2000 < X < 0, -2100 < Z < 0 | `loess_plateau` | `loess_plateau` | 90-110, ravines -15 to -30 |
| Central plains | 0 < X < 2300, -2100 < Z < 0 | `central_plains` | | 64-72 |
| Shandong | X > 2300, -2100 < Z < 0 | `shandong_hills`, `sacred_mountain` (3400, -1100) | `cloud_sea_peaks` (Huangshan pine) | 70-100, peak ~210 |
| Sichuan | X < -200, 0 < Z < 2300 | `red_basin` (centre), `bamboo_mountains` (rim) | `shu_bamboo_sea` | 70-90 / 100-180 |
| Hubei | -200 < X < 2100, 0 < Z < 2300 | `hubei_waterlands`, `river_delta` (X > 2100) | `jiangnan_waterlands` (canals, ponds) | 62-70 |
| Unnam | X < 0, Z > 2300 | `stone_forest` (Z < 3500), `western_gorges` (X < -3500), `unnam_jungle` | `jade_karst_forest` (stone towers) | 90-110 / 70-90 |
| Honam | 0 < X < 2500, Z > 2300 | `stone_peaks` (600, 2900), `rice_terraces`, `subtropical_forest` | `wulingyuan_pillars`, `camphor_hills` | 70-90, peaks +60 to +150 |
| East coast | X > 2500, Z > 2300, and the shore | `southeast_coast`, `minecraft:beach`, vanilla oceans | | 63, shelf 5-15 deep |

Each biome JSON sets `grass_color`, `foliage_color`, `water_color`, `fog_color` and `sky_color` from the guide.
Caves, oceans and beaches stay vanilla.

## Relief and water

- North wall: a ridge 20-40 blocks high along Z -2100, lower (10-20) above the central plains, with four passes at
  X -3400, -900, 1200 (the fortified pass) and 3800.
- Mountain chains along Z 0: wooded and high west of X -200, low hills east of it. Gorge walls 60-100 blocks high
  along X -200 between Sichuan and Hubei.
- Yellow river: north to south along X 0 (Shaanxi / central plains border), 30-60 wide, then a tributary to Wuhan.
- Great river: west to east from the Sichuan basin through the gorges to Wuhan (950, 1100), then a delta at the
  coast, 60-100 wide after Wuhan.
- Lakes: Qinghai salt lake (-3500, -1100), r ~220, white crust; the great lake (1250, 2350), r ~380.

## Reserved sites

Flattened and kept free of ravines and rivers (except the Wuhan confluence) from step 2:

- Capitals: Chang-An (-1300, -700), 300 x 300, ground Y 100; Wuhan (950, 1100), 300 x 300, ground Y 66.
- Faction sites: North Sky HQ (-2000, -3200), Silent Night (1000, -4300), Iron Castle (3000, -3200),
  Tang clan (-2800, 1300), Broken Fist (-2500, 3000), sacred mountain (3400, -1100).

Capitals will be hand-built, saved as 48 x 48 structure tiles and placed once at their fixed spot.

## How it plugs in (no TerraBlender)

- `MurimMap`: pure Java. Zone at (x, z), target height, rivers, lakes, reserved sites. Unit tested; a test writes
  the map as a PNG.
- `MurimBiomeSource` (`murimblock:murim_map`): biome of the zone; vanilla for caves and ocean.
- Custom density functions read `MurimMap` for plateaus, the wall, chains and river beds; vanilla noise adds detail.
- `noise_settings/murim`: a copy of the overworld settings using those functions, with each biome's surface rules.
- `world_preset` override of `minecraft:normal`: new worlds only. Older worlds must be recreated when the relief
  changes.

## Steps

1. Biomes in place: `MurimMap`, the biome source, the 20 biome JSONs with the guide's colours and vanilla flora,
   ocean around. Relief still vanilla. Checks: map PNG, GameTest that the preset loads, real-client tour.
2. Big relief: plateaus, plains, north wall and passes, chains, west peaks, Sichuan gorges, reserved ground.
3. Water: the two rivers, Wuhan confluence, delta, the two lakes.
4. Landforms and soils: loess ravines, stone forest, Honam peaks, rice terraces, V1 generators adapted, tree
   density rising to the south.
5. Ambience: fog, particles and sounds per zone.

## Step 1: biomes in place (29 Oct 2026)

- `world/MurimMap`: the zones in Java (the mockup's math). `SPOTS` lists one sure point per biome, in guide
  coordinates. `MurimMapTest` checks them, the sea around, and that every biome is there, and draws
  `build/murim-map/murim-map.png` (1 px = 25 blocks).
- `world/MurimBiomeSource` (`murimblock:murim_map`) and `world/MurimContinents` (`murimblock:murim_continents`,
  wrapped around vanilla's `overworld/continents`), registered in `registry/MurimWorldgen`.
- Data: `data/minecraft/worldgen/world_preset/normal.json`, `.../density_function/overworld/continents.json`,
  20 biomes in `data/murimblock/worldgen/biome/` (added to `#minecraft:is_overworld`), names in `en_us.json`.
- The biomes come from `node tools/worldgen/generate-biomes.cjs <vanilla biome folder>`: each copies a vanilla
  biome's flora and mobs (snowy plains, meadow, savanna plateau, swamp, bamboo jungle...) with the guide's colours.
  Vanilla villages and most vanilla structures do not spawn in them yet (their biome tags do not list them).
- The continentalness override applies to every world using vanilla's overworld noise (default and amplified);
  worlds made before keep their old chunks and get Murim chunks beyond them.
- In game: `gradlew runMapTour` makes a new world (seed 29102026) in `run/map-tour`, checks the spawn and each spot's
  biome (PASS/FAIL in the log), and shoots each zone to `run/map-tour/screenshots` (as a spectator, so the
  first-person body does not hide the view).
- Verified 29 Oct 2026: `gradlew build` (all tests), `runMapTour` 21/21 PASS; spawn at 0, 88, 0 in the loess plateau.

## Step 2: big relief (29 Oct 2026)

- `MurimMap.relief(x, z)` gives each column a wanted ground height, a weight (how much it replaces vanilla) and a
  jaggedness. Each biome has its own (`RELIEF` table; blended over 170 blocks), then the features are added: the
  north wall and its four passes, the chain along Z 0 (peaks west, hills east), the gorge walls between the Sichuan
  and the Hubei, the sacred mountain, and the capitals' flat ground (Chang-An Y 100, Wuhan Y 66, weight 1).
- `murimblock:murim_relief` (`MurimRelief`) mixes it into vanilla's `overworld/depth` (part `offset`) and
  `overworld/sloped_cheese` (part `jaggedness`), both overridden in `data/minecraft`. The sea keeps vanilla (weight 0).
- Vanilla's noise squashes high ground, so heights above Y 90 get a 20 % boost; the tour reads the middle of five points around each stop (measured in game).
- `runMapTour` now also checks each stop's ground height (within 20 blocks where the weight is 0.7 or more) and
  shoots four relief views: the north wall, a pass, the Qinghai escarpment and the Sichuan gorges.
- Verified 30 Oct 2026: `gradlew build` (all tests), `runMapTour` 0 failures: every biome and height checked, the west
  peaks at Y 207, the sacred mountain at 217, the north wall at 141 with its pass at 103, the gorges at 188.

## V1 pillars and spires (30 Oct 2026)

- Enzo asked for V1's peaks before step 3. `PillarForestFeature` (the Wulingyuan stone forest) now fills the Honam's
  `stone_peaks`, and `RockPillarFeature` (style `spire`, placed feature `granite_spire`) stands on the
  `sacred_mountain`. The code comes from V1, with `MurimMap` in place of V1's biome and site lookups
  (`MurimMap.nearSite`).
- Vanilla blocks only for now: V1's `pale_granite`, `pillar_sandstone` and Huangshan pine are not ported, so the
  spires are stone with diorite and andesite streaks, topped with spruces. V1's `CliffPineFeature` waits for the pine.
- The features are added in `tools/worldgen/generate-biomes.cjs` (`EXTRA`), in step 2 (local_modifications), so the
  trees grow on the rock tops.
- The tour's camera now sits 45 blocks above the bare terrain (`getBaseHeight`), not above a pillar or a tree.
- Verified 30 Oct 2026: `gradlew build`, `runMapTour` 0 failures; the shots show the pillar forest and the spires.

## Open points

- Exact landmass size and how far the ocean ring goes before vanilla (or nothing) starts.
- Whether amplified / large-biome presets get the map too.
