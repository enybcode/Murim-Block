// Writes the Murim map's biome JSONs and biome tags (docs/world/map.md).
// Each biome starts from a vanilla biome (its features, carvers and mobs), with the colours, temperature and rain of
// Enzo's biome guide, plus our own features (EXTRA).
//
// Usage: node tools/worldgen/generate-biomes.cjs <vanilla biome folder>
// The vanilla folder is data/minecraft/worldgen/biome extracted from the client-extra jar in build/moddev/artifacts.
const fs = require('fs');
const path = require('path');

const vanilla = process.argv[2];
if (!vanilla || !fs.existsSync(path.join(vanilla, 'plains.json'))) {
  console.error('Give the folder holding the vanilla biome JSONs (data/minecraft/worldgen/biome).');
  process.exit(1);
}
const root = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'data');

// Zone colours from the guide: grass, foliage, water, fog, sky.
const NORTH = ['#A9A47A', '#7F8466', '#3D5A6B', '#C9CED4', '#A7B6C8'];
const QINGHAI = ['#9BB46A', '#7FA05A', '#3FB3C8', '#D8E6F0', '#7DB2EA'];
const SHAANXI = ['#B8A96A', '#9C9A5C', '#A88E55', '#E3D6B8', '#A9C2DE'];
const PLAINS = ['#94B35A', '#77A24A', '#4A8BB8', '#DDE6EC', '#8FB6E0'];
const SHANDONG = ['#86B55B', '#6E9F45', '#3F7FB5', '#E2EBF0', '#92BCE6'];
const SICHUAN = ['#5E9E4A', '#4E8F3A', '#4C8F8A', '#C9D6D0', '#A7BCC8'];
const HUBEI = ['#6FAE4F', '#5A9A3C', '#3E8C9E', '#D2DDE0', '#9DBFD6'];
const UNNAM = ['#4FAE3A', '#3E9A2E', '#2FA39A', '#CFE0D4', '#86B9E6'];
const HONAM = ['#64A84A', '#519A38', '#3A97A5', '#D9E4E2', '#95C0E0'];

// id: [vanilla template, temperature, downfall, colours, English name]
const BIOMES = {
  frozen_tundra: ['snowy_plains', -0.5, 0.3, NORTH, 'Frozen Tundra'],
  cold_desert: ['windswept_gravelly_hills', 0.05, 0.1, NORTH, 'Cold Desert'],
  frozen_steppe: ['snowy_plains', 0.25, 0.2, NORTH, 'Frozen Steppe'],
  qinghai_plateau: ['meadow', 0.3, 0.4, QINGHAI, 'Qinghai Plateau'],
  western_snow_peaks: ['jagged_peaks', -0.7, 0.9, QINGHAI, 'Western Snow Peaks'],
  loess_plateau: ['savanna_plateau', 0.9, 0.3, SHAANXI, 'Loess Plateau'],
  central_plains: ['sunflower_plains', 0.8, 0.4, PLAINS, 'Central Plains'],
  shandong_hills: ['forest', 0.7, 0.6, SHANDONG, 'Shandong Hills'],
  sacred_mountain: ['windswept_forest', 0.6, 0.7, SHANDONG, 'Sacred Mountain'],
  red_basin: ['plains', 0.85, 0.8, SICHUAN, 'Red Basin'],
  bamboo_mountains: ['bamboo_jungle', 0.9, 0.9, SICHUAN, 'Bamboo Mountains'],
  hubei_waterlands: ['swamp', 0.85, 0.9, HUBEI, 'Hubei Waterlands'],
  river_delta: ['swamp', 0.85, 0.9, ['#6FAE4F', '#5A9A3C', '#6E8F7A', '#D2DDE0', '#9DBFD6'], 'River Delta'],
  stone_forest: ['sparse_jungle', 0.9, 0.6, ['#8AA85A', '#6E9447', '#2FA39A', '#CFE0D4', '#86B9E6'], 'Stone Forest'],
  western_gorges: ['windswept_forest', 0.35, 0.8, UNNAM, 'Western Gorges'],
  unnam_jungle: ['jungle', 0.95, 0.9, UNNAM, 'Unnam Jungle'],
  stone_peaks: ['sparse_jungle', 0.85, 0.85, HONAM, 'Stone Peaks'],
  rice_terraces: ['plains', 0.85, 0.85, HONAM, 'Rice Terraces'],
  // The south grows denser: the subtropical forest is a full jungle.
  subtropical_forest: ['jungle', 0.9, 0.85, HONAM, 'Subtropical Forest'],
  southeast_coast: ['forest', 0.85, 0.8, ['#5FAA48', '#519A38', '#3AA0B8', '#DDE8EC', '#8FBCE0'], 'Southeast Coast'],
};

// Our own features, added to the template's: id: [[decoration step, placed feature]]. Step 2 is local_modifications,
// before the vegetation, so trees grow on the rock tops.
const EXTRA = {
  sacred_mountain: [[2, 'murimblock:granite_spire']],
  stone_peaks: [[2, 'murimblock:pillar_forest']],
  qinghai_plateau: [[1, 'murimblock:high_lake']],
  stone_forest: [[2, 'murimblock:karst_blade']],
  rice_terraces: [[2, 'murimblock:rice_terrace']],
};

// Ambient particles (docs/world/map.md, step 5): id: [particle, probability per block shown]. Drifting snow in the
// north and on the peaks, dust over the Gobi and the loess, floating motes in the southern forests.
const PARTICLES = {
  frozen_tundra: ['snowflake', 0.004],
  frozen_steppe: ['snowflake', 0.002],
  western_snow_peaks: ['snowflake', 0.008],
  cold_desert: ['white_ash', 0.006],
  loess_plateau: ['white_ash', 0.003],
  bamboo_mountains: ['spore_blossom_air', 0.003],
  stone_peaks: ['spore_blossom_air', 0.002],
  unnam_jungle: ['spore_blossom_air', 0.006],
  subtropical_forest: ['spore_blossom_air', 0.004],
};

const rgb = hex => parseInt(hex.slice(1), 16);
const write = (file, data) => {
  fs.mkdirSync(path.dirname(file), {recursive: true});
  fs.writeFileSync(file, JSON.stringify(data, null, 2) + '\n');
};

for (const [id, [template, temperature, downfall, colours]] of Object.entries(BIOMES)) {
  const biome = JSON.parse(fs.readFileSync(path.join(vanilla, template + '.json'), 'utf8'));
  const [grass, foliage, water, fog, sky] = colours.map(rgb);
  biome.temperature = temperature;
  biome.downfall = downfall;
  biome.has_precipitation = true;
  delete biome.temperature_modifier;
  const effects = {...biome.effects, grass_color: grass, foliage_color: foliage, water_color: water,
    fog_color: fog, sky_color: sky};
  delete effects.grass_color_modifier;
  if (PARTICLES[id]) {
    effects.particle = {options: {type: "minecraft:" + PARTICLES[id][0]}, probability: PARTICLES[id][1]};
  }
  biome.effects = effects;
  for (const [step, feature] of EXTRA[id] || []) {
    while (biome.features.length <= step) biome.features.push([]);
    biome.features[step] = [feature, ...biome.features[step]];
  }
  write(path.join(root, 'murimblock', 'worldgen', 'biome', id + '.json'), biome);
}

const ids = Object.keys(BIOMES).map(id => 'murimblock:' + id);
write(path.join(root, 'minecraft', 'tags', 'worldgen', 'biome', 'is_overworld.json'), {replace: false, values: ids});

// Names for the F3 screen and /locate, merged into the English lang file.
const langFile = path.join(root, '..', 'assets', 'murimblock', 'lang', 'en_us.json');
const lang = JSON.parse(fs.readFileSync(langFile, 'utf8'));
for (const [id, entry] of Object.entries(BIOMES)) {
  lang['biome.murimblock.' + id] = entry[4];
}
fs.writeFileSync(langFile, JSON.stringify(lang, null, 2) + '\n');
console.log(`Wrote ${ids.length} biomes.`);
