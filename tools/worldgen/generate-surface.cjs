// Writes the overworld's noise settings with the Murim map's soils (docs/world/map.md, step 4): vanilla's own
// overworld.json, with our surface rules put first inside its "above the preliminary surface" branch. Anything our
// rules leave alone falls through to vanilla's (grass, dirt, beaches, snow...). The palettes come from V1's
// MurimSurfaceRules (Enzo's OK, 29 Oct 2026), in vanilla blocks.
//
// Usage: node tools/worldgen/generate-surface.cjs <vanilla overworld noise settings JSON>
// The vanilla file is data/minecraft/worldgen/noise_settings/overworld.json from the client-extra jar.
const fs = require('fs');
const path = require('path');

const source = process.argv[2];
if (!source || !fs.existsSync(source)) {
  console.error('Give the vanilla overworld noise settings JSON (data/minecraft/worldgen/noise_settings/overworld.json).');
  process.exit(1);
}
const settings = JSON.parse(fs.readFileSync(source, 'utf8'));

const block = name => ({type: 'minecraft:block', result_state: {Name: 'minecraft:' + name}});
const seq = (...rules) => ({type: 'minecraft:sequence', sequence: rules});
const when = (condition, rule) => ({type: 'minecraft:condition', if_true: condition, then_run: rule});
const not = condition => ({type: 'minecraft:not', invert: condition});
const biome = (...ids) => ({type: 'minecraft:biome', biome_is: ids.map(id => 'murimblock:' + id)});
const steep = {type: 'minecraft:steep'};
const floor = {type: 'minecraft:stone_depth', offset: 0, add_surface_depth: false, secondary_depth_range: 0, surface_type: 'floor'};
const underFloor = {type: 'minecraft:stone_depth', offset: 0, add_surface_depth: true, secondary_depth_range: 0, surface_type: 'floor'};
const deep = {type: 'minecraft:stone_depth', offset: 0, add_surface_depth: true, secondary_depth_range: 4, surface_type: 'floor'};
const dry = {type: 'minecraft:water', offset: -1, surface_depth_multiplier: 0, add_stone_depth: false};
const above = y => ({type: 'minecraft:y_above', anchor: {absolute: y}, surface_depth_multiplier: 0, add_stone_depth: false});
const noise = min => ({type: 'minecraft:noise_threshold', noise: 'minecraft:surface', min_threshold: min, max_threshold: 1e9});
const patches = noise(0.12);
const fewPatches = noise(0.35);

const MURIM = seq(
  // Loess: bare yellow earth on the gully walls, a red fossil-soil band at Y 96-99; grass in broad patches on the
  // tablelands.
  when(biome('loess_plateau'), seq(
    when(steep, seq(when(above(96), when(not(above(99)), block('red_terracotta'))), block('terracotta'))),
    when(floor, when(dry, seq(when(noise(-0.3), block('terracotta')), block('grass_block')))),
    when(underFloor, block('terracotta')))),
  // Gobi: sand drifts over dark gravel pavements, sandstone below.
  when(biome('cold_desert'), seq(
    when(floor, seq(when(noise(-0.2), block('sand')), block('gravel'))),
    when(deep, block('sandstone')),
    when(underFloor, block('sand')))),
  // Snow peaks: bare rock and blue ice on the cliffs, snow and glacier tongues on the ledges.
  when(biome('western_snow_peaks'), seq(
    when(steep, seq(when(fewPatches, block('packed_ice')), block('stone'))),
    when(floor, when(dry, seq(when(fewPatches, block('packed_ice')), block('snow_block')))),
    when(underFloor, block('stone')))),
  // The cold north: stony slopes, packed snow drifts over the frozen meadows.
  when(biome('frozen_steppe', 'frozen_tundra'), seq(
    when(steep, block('stone')),
    when(floor, when(dry, when(patches, block('snow_block')))))),
  // The sacred mountain: sheer rock, snow on the ledges above Y 170, moss below.
  when(biome('sacred_mountain'), seq(
    when(steep, seq(when(fewPatches, block('andesite')), block('stone'))),
    when(floor, when(above(170), block('snow_block'))),
    when(floor, when(fewPatches, block('moss_block'))))),
  // Bamboo mountains: rooty coarse banks, mossy in the damp, leaf litter under the bamboo.
  when(biome('bamboo_mountains'), seq(
    when(steep, seq(when(fewPatches, block('moss_block')), when(patches, block('rooted_dirt')), block('coarse_dirt'))),
    when(floor, when(dry, when(patches, block('podzol')))))),
  // The Sichuan's red soil on its slopes.
  when(biome('red_basin'), seq(
    when(steep, seq(when(patches, block('red_terracotta')), block('terracotta'))),
    when(floor, when(dry, when(fewPatches, block('coarse_dirt')))))),
  // Waterlands and delta: mud and clay under the water and along the low banks.
  when(biome('hubei_waterlands', 'river_delta'), seq(
    when(not(dry), when(floor, seq(when(patches, block('clay')), block('mud')))),
    when(floor, when(not(above(64)), block('mud'))))),
  // Karst and gorges: bare grey rock on the slopes, mossy in the damp; gravel and clay bars in the rivers.
  when(biome('stone_forest', 'western_gorges', 'stone_peaks'), seq(
    when(steep, seq(when(fewPatches, block('mossy_cobblestone')), block('stone'))),
    when(not(dry), when(floor, seq(when(patches, block('clay')), block('gravel')))))));

const surface = settings.surface_rule.sequence[1];
if (surface.if_true.type !== 'minecraft:above_preliminary_surface' || surface.then_run.type !== 'minecraft:sequence') {
  console.error('Unexpected vanilla surface rule layout.');
  process.exit(1);
}
surface.then_run.sequence.unshift(MURIM);

const out = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'data', 'minecraft', 'worldgen', 'noise_settings',
  'overworld.json');
fs.mkdirSync(path.dirname(out), {recursive: true});
fs.writeFileSync(out, JSON.stringify(settings, null, 2) + '\n');
console.log('Wrote ' + out);
