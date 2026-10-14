// Generates every JSON file of the Murim biomes (docs/biomes): block states, block and item models, loot tables,
// recipes, tags, language entries, configured and placed features, carvers and the seven biomes.
//
//   node scripts/worldgen/generate-nature.cjs
//
// The block list must match com.murimblock.world.block.MurimBlocks; textures come from NatureTextures.java.
// Re-run after any change here: files it owns are overwritten, nothing else is touched (lang is merged).
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..', 'src', 'main', 'resources');
const NS = 'murimblock';
const A = (...p) => path.join(ROOT, 'assets', NS, ...p);
const D = (...p) => path.join(ROOT, 'data', ...p);
const written = [];
function write(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, typeof data === 'string' ? data : JSON.stringify(data, null, 2) + '\n');
  written.push(path.relative(ROOT, file));
}
const id = n => `${NS}:${n}`;
const blockTex = n => `${NS}:block/${n}`;

// ---------------------------------------------------------------- content
const WOODS = {
  osmanthus: 'Sweet Osmanthus', banyan: 'Banyan', huangshan_pine: 'Huangshan Pine', jujube: 'Jujube',
  dove_tree: 'Dove Tree', willow: 'Weeping Willow', peach: 'Peach', desert_poplar: 'Desert Poplar',
};
const STONES = {
  karst_limestone: ['Karst Limestone', 'pickaxe', 'all'], mossy_karst_limestone: ['Mossy Karst Limestone', 'pickaxe', 'all'],
  pale_granite: ['Pale Granite', 'pickaxe', 'all'], pillar_sandstone: ['Pillar Sandstone', 'pickaxe', 'column'],
  loess: ['Loess', 'shovel', 'all'], red_loess: ['Red Loess', 'shovel', 'all'], gobi_gravel: ['Gobi Gravel', 'shovel', 'all'],
};
// name: [english, kind]  kind: cross | tall | flower | lotus | strand
const PLANTS = {
  karst_fern: ['Karst Fern', 'cross'], rhododendron: ['Huangshan Rhododendron', 'tall'], sea_buckthorn: ['Sea Buckthorn', 'cross'],
  feather_grass: ['Feather Grass', 'cross'], bamboo_shoot: ['Bamboo Shoot', 'cross'], arrow_bamboo: ['Arrow Bamboo', 'cross'],
  tree_fern: ['Tree Fern', 'tall'], lotus_pad: ['Lotus Leaf', 'lotus'], lotus_blossom: ['Lotus Blossom', 'lotus'],
  wild_rice: ['Wild Rice', 'tall'], chrysanthemum: ['Chrysanthemum', 'flower'], rapeseed: ['Rapeseed Flower', 'flower'],
  tamarisk: ['Tamarisk', 'tall'], saxaul: ['Saxaul', 'cross'], cistanche: ['Cistanche', 'cross'],
  banyan_roots: ['Banyan Aerial Roots', 'strand'], willow_strands: ['Willow Strands', 'strand'],
};
const BIOME_NAMES = {
  jade_karst_forest: 'Jade Karst Forest', loess_plateau: 'Loess Plateau', hexi_frontier_desert: 'Hexi Frontier Desert',
  cloud_sea_peaks: 'Cloud Sea Peaks', shu_bamboo_sea: 'Shu Bamboo Sea', jiangnan_waterlands: 'Jiangnan Waterlands',
  peach_blossom_valley: 'Peach Blossom Valley',
};

// ---------------------------------------------------------------- assets
const lang = {};
const cutout = { render_type: 'minecraft:cutout' };
const cutoutMipped = { render_type: 'minecraft:cutout_mipped' };

function simpleBlock(name, model) {
  write(A('blockstates', `${name}.json`), { variants: { '': { model: id(`block/${name}`) } } });
  write(A('models', 'block', `${name}.json`), model);
  write(A('models', 'item', `${name}.json`), { parent: id(`block/${name}`) });
}
function pillarState(name) {
  const m = id(`block/${name}`);
  write(A('blockstates', `${name}.json`), { variants: {
    'axis=x': { model: m, x: 90, y: 90 }, 'axis=y': { model: m }, 'axis=z': { model: m, x: 90 } } });
}
function flatItem(name, texture) {
  write(A('models', 'item', `${name}.json`), { parent: 'minecraft:item/generated', textures: { layer0: texture } });
}

for (const [name, [english, , shape]] of Object.entries(STONES)) {
  lang[`block.${NS}.${name}`] = english;
  simpleBlock(name, shape === 'column'
    ? { parent: 'minecraft:block/cube_column', textures: { side: blockTex(name), end: blockTex(name + '_top') } }
    : { parent: 'minecraft:block/cube_all', textures: { all: blockTex(name) } });
}

for (const [wood, english] of Object.entries(WOODS)) {
  lang[`block.${NS}.${wood}_log`] = `${english} Log`;
  lang[`block.${NS}.${wood}_planks`] = `${english} Planks`;
  lang[`block.${NS}.${wood}_leaves`] = `${english} Leaves`;
  lang[`block.${NS}.${wood}_sapling`] = `${english} Sapling`;
  pillarState(`${wood}_log`);
  write(A('models', 'block', `${wood}_log.json`), { parent: 'minecraft:block/cube_column',
    textures: { side: blockTex(`${wood}_log`), end: blockTex(`${wood}_log_top`) } });
  write(A('models', 'item', `${wood}_log.json`), { parent: id(`block/${wood}_log`) });
  simpleBlock(`${wood}_planks`, { parent: 'minecraft:block/cube_all', textures: { all: blockTex(`${wood}_planks`) } });
  simpleBlock(`${wood}_leaves`, { parent: 'minecraft:block/cube_all', textures: { all: blockTex(`${wood}_leaves`) }, ...cutoutMipped });
  write(A('blockstates', `${wood}_sapling.json`), { variants: { '': { model: id(`block/${wood}_sapling`) } } });
  write(A('models', 'block', `${wood}_sapling.json`), { parent: 'minecraft:block/cross', textures: { cross: blockTex(`${wood}_sapling`) }, ...cutout });
  flatItem(`${wood}_sapling`, blockTex(`${wood}_sapling`));
}

// Giant bamboo culm: an 8x8 stem with its own top, and its feathery leaves.
lang[`block.${NS}.moso_bamboo`] = 'Moso Bamboo';
lang[`block.${NS}.moso_bamboo_leaves`] = 'Moso Bamboo Leaves';
simpleBlock('moso_bamboo', {
  parent: 'minecraft:block/block', textures: { side: blockTex('moso_bamboo'), top: blockTex('moso_bamboo_top'), particle: blockTex('moso_bamboo') },
  elements: [{ from: [4, 0, 4], to: [12, 16, 12], faces: {
    north: { uv: [4, 0, 12, 16], texture: '#side' }, south: { uv: [4, 0, 12, 16], texture: '#side' },
    east: { uv: [4, 0, 12, 16], texture: '#side' }, west: { uv: [4, 0, 12, 16], texture: '#side' },
    up: { uv: [4, 4, 12, 12], texture: '#top', cullface: 'up' }, down: { uv: [4, 4, 12, 12], texture: '#top', cullface: 'down' } } }],
});
simpleBlock('moso_bamboo_leaves', { parent: 'minecraft:block/cube_all', textures: { all: blockTex('moso_bamboo_leaves') }, ...cutoutMipped });

for (const [name, [english, kind]] of Object.entries(PLANTS)) {
  lang[`block.${NS}.${name}`] = english;
  if (kind === 'tall') {
    write(A('blockstates', `${name}.json`), { variants: {
      'half=lower': { model: id(`block/${name}_bottom`) }, 'half=upper': { model: id(`block/${name}_top`) } } });
    for (const half of ['bottom', 'top']) {
      write(A('models', 'block', `${name}_${half}.json`), { parent: 'minecraft:block/cross', textures: { cross: blockTex(`${name}_${half}`) }, ...cutout });
    }
    flatItem(name, blockTex(`${name}_top`));
  } else if (kind === 'lotus') {
    write(A('blockstates', `${name}.json`), { variants: {
      '': [0, 90, 180, 270].map(y => ({ model: id(`block/${name}`), y })) } });
    const pad = { from: [0, 0.25, 0], to: [16, 0.25, 16], faces: {
      up: { uv: [0, 0, 16, 16], texture: '#pad' }, down: { uv: [0, 16, 16, 0], texture: '#pad' } } };
    const elements = [pad];
    if (name === 'lotus_blossom') {
      for (const [from, to, rot] of [[[4, 0.25, 8], [12, 8.25, 8], 45], [[8, 0.25, 4], [8, 8.25, 12], 45]]) {
        elements.push({ from, to, rotation: { origin: [8, 8, 8], axis: 'y', angle: rot, rescale: true }, shade: false, faces: {
          north: { uv: [0, 0, 16, 16], texture: '#flower' }, south: { uv: [0, 0, 16, 16], texture: '#flower' },
          east: { uv: [0, 0, 16, 16], texture: '#flower' }, west: { uv: [0, 0, 16, 16], texture: '#flower' } } });
      }
    }
    write(A('models', 'block', `${name}.json`), { parent: 'minecraft:block/block', ambientocclusion: false,
      textures: { pad: blockTex('lotus_pad'), flower: blockTex('lotus_flower'), particle: blockTex('lotus_pad') }, elements, ...cutout });
    flatItem(name, blockTex(name === 'lotus_blossom' ? 'lotus_flower' : 'lotus_pad'));
  } else {
    write(A('blockstates', `${name}.json`), { variants: { '': { model: id(`block/${name}`) } } });
    write(A('models', 'block', `${name}.json`), { parent: 'minecraft:block/cross', textures: { cross: blockTex(name) }, ...cutout });
    flatItem(name, blockTex(name));
  }
}
for (const [biome, english] of Object.entries(BIOME_NAMES)) lang[`biome.${NS}.${biome}`] = english;
lang[`itemGroup.${NS}_nature`] = 'Murimblock: Nature';

const langFile = A('lang', 'en_us.json');
const current = JSON.parse(fs.readFileSync(langFile, 'utf8'));
write(langFile, { ...current, ...lang });

// ---------------------------------------------------------------- loot, recipes, tags
const selfDrop = name => ({ type: 'minecraft:block', pools: [{ rolls: 1, bonus_rolls: 0,
  entries: [{ type: 'minecraft:item', name: id(name) }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
const shearsOrSilk = { condition: 'minecraft:any_of', terms: [
  { condition: 'minecraft:match_tool', predicate: { items: 'minecraft:shears' } },
  { condition: 'minecraft:match_tool', predicate: { predicates: { 'minecraft:enchantments': [{ enchantments: 'minecraft:silk_touch', levels: { min: 1 } }] } } }] };
function leavesLoot(leaves, sapling) {
  const pools = [{ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:alternatives', children: [
    { type: 'minecraft:item', name: id(leaves), conditions: [shearsOrSilk] },
    ...(sapling ? [{ type: 'minecraft:item', name: id(sapling), conditions: [{ condition: 'minecraft:survives_explosion' },
      { condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: [0.05, 0.0625, 0.083333336, 0.1] }] }] : []),
  ] }] }, { rolls: 1, bonus_rolls: 0, conditions: [{ condition: 'minecraft:inverted', term: shearsOrSilk }],
    entries: [{ type: 'minecraft:item', name: 'minecraft:stick', functions: [
      { function: 'minecraft:set_count', count: { type: 'minecraft:uniform', min: 1, max: 2 } }, { function: 'minecraft:explosion_decay' }],
      conditions: [{ condition: 'minecraft:table_bonus', enchantment: 'minecraft:fortune', chances: [0.02, 0.022222223, 0.025, 0.033333335, 0.1] }] }] }];
  return { type: 'minecraft:block', pools };
}
const loot = (name, table) => write(D(NS, 'loot_table', 'blocks', `${name}.json`), table);
for (const name of Object.keys(STONES)) loot(name, selfDrop(name));
for (const wood of Object.keys(WOODS)) {
  loot(`${wood}_log`, selfDrop(`${wood}_log`));
  loot(`${wood}_planks`, selfDrop(`${wood}_planks`));
  loot(`${wood}_sapling`, selfDrop(`${wood}_sapling`));
  loot(`${wood}_leaves`, leavesLoot(`${wood}_leaves`, `${wood}_sapling`));
  write(D(NS, 'recipe', `${wood}_planks.json`), { type: 'minecraft:crafting_shapeless', category: 'building', group: 'planks',
    ingredients: [{ item: id(`${wood}_log`) }], result: { id: id(`${wood}_planks`), count: 4 } });
}
loot('moso_bamboo', selfDrop('moso_bamboo'));
loot('moso_bamboo_leaves', leavesLoot('moso_bamboo_leaves', null));
write(D(NS, 'recipe', 'bamboo_planks_from_moso_bamboo.json'), { type: 'minecraft:crafting_shapeless', category: 'building', group: 'planks',
  ingredients: [{ item: id('moso_bamboo') }], result: { id: 'minecraft:bamboo_planks', count: 2 } });
for (const [name, [, kind]] of Object.entries(PLANTS)) {
  if (kind === 'tall') {
    loot(name, { type: 'minecraft:block', pools: [{ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:item', name: id(name),
      conditions: [{ condition: 'minecraft:block_state_property', block: id(name), properties: { half: 'lower' } }] }],
      conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
  } else if (kind === 'strand') {
    loot(name, { type: 'minecraft:block', pools: [{ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:item', name: id(name),
      conditions: [{ condition: 'minecraft:match_tool', predicate: { items: 'minecraft:shears' } }] }] }] });
  } else loot(name, selfDrop(name));
}

const tag = (registry, name, values, ns = 'minecraft') => write(D(ns, 'tags', registry, `${name}.json`), { replace: false, values });
const logs = Object.keys(WOODS).map(w => id(`${w}_log`));
const planks = Object.keys(WOODS).map(w => id(`${w}_planks`));
const leaves = [...Object.keys(WOODS).map(w => id(`${w}_leaves`)), id('moso_bamboo_leaves')];
const saplings = Object.keys(WOODS).map(w => id(`${w}_sapling`));
const plants = Object.keys(PLANTS).map(id);
for (const registry of ['block', 'item']) {
  tag(registry, 'logs_that_burn', logs);
  tag(registry, 'planks', planks);
  tag(registry, 'leaves', leaves);
  tag(registry, 'saplings', saplings);
  tag(registry, 'small_flowers', [id('chrysanthemum'), id('rapeseed')]);
  tag(registry, 'tall_flowers', [id('rhododendron')]);
}
tag('block', 'mineable/pickaxe', Object.entries(STONES).filter(([, v]) => v[1] === 'pickaxe').map(([k]) => id(k)));
tag('block', 'mineable/shovel', Object.entries(STONES).filter(([, v]) => v[1] === 'shovel').map(([k]) => id(k)));
tag('block', 'mineable/axe', [...logs, ...planks, id('moso_bamboo')]);
tag('block', 'mineable/hoe', leaves);
tag('block', 'replaceable_by_trees', [...leaves, ...plants.filter(p => !p.includes('lotus'))]);
tag('block', 'sword_efficient', [...leaves, ...plants]);

// ---------------------------------------------------------------- worldgen: configured features
const state = (name, props) => props ? { Name: name, Properties: props } : { Name: name };
const provider = (name, props) => ({ type: 'minecraft:simple_state_provider', state: state(name, props) });
const weighted = list => ({ type: 'minecraft:weighted_state_provider', entries: list.map(([data, weight]) => ({ data, weight })) });
const uniform = (min, max) => ({ type: 'minecraft:uniform', min_inclusive: min, max_inclusive: max });
const leafState = w => provider(id(`${w}_leaves`), { distance: '7', persistent: 'false', waterlogged: 'false' });
const logState = w => provider(id(`${w}_log`), { axis: 'y' });
const CF = (name, type, config) => write(D(NS, 'worldgen', 'configured_feature', `${name}.json`), { type, config });
const PF = (name, feature, placement) => write(D(NS, 'worldgen', 'placed_feature', `${name}.json`), { feature, placement });
const twoLayers = (limit, lower, upper) => ({ type: 'minecraft:two_layers_feature_size', limit, lower_size: lower, upper_size: upper });
function tree(name, wood, trunk, foliage, size, decorators = [], dirt = 'minecraft:dirt') {
  CF(name, 'minecraft:tree', { decorators, dirt_provider: provider(dirt), force_dirt: false, ignore_vines: true,
    foliage_placer: foliage, foliage_provider: leafState(wood), minimum_size: size, trunk_placer: trunk, trunk_provider: logState(wood) });
}
const strandDecorator = (block, probability, min, max) => ({ type: id('hanging_strands'), block: provider(id(block)), probability, length: uniform(min, max) });

// Sweet osmanthus: short trunk, dense evergreen crown dotted with golden flowers, irregular (leaves spread at random
// inside the crown, like the azalea, not a ball).
tree('osmanthus', 'osmanthus', { type: 'minecraft:straight_trunk_placer', base_height: 3, height_rand_a: 2, height_rand_b: 0 },
  { type: 'minecraft:random_spread_foliage_placer', foliage_height: 3, leaf_placement_attempts: 70, offset: 0, radius: 3 }, twoLayers(1, 0, 1));
// Banyan: broad spreading crown with aerial roots; the giant one from 2x2 saplings or rarely in the wild.
tree('banyan', 'banyan', { type: 'minecraft:fancy_trunk_placer', base_height: 6, height_rand_a: 6, height_rand_b: 0 },
  { type: 'minecraft:fancy_foliage_placer', height: 4, offset: 4, radius: 3 },
  { type: 'minecraft:two_layers_feature_size', limit: 0, lower_size: 0, min_clipped_height: 4, upper_size: 0 },
  [strandDecorator('banyan_roots', 0.12, 2, 7)]);
tree('banyan_giant', 'banyan', { type: 'minecraft:dark_oak_trunk_placer', base_height: 8, height_rand_a: 3, height_rand_b: 2 },
  { type: 'minecraft:dark_oak_foliage_placer', offset: 0, radius: 1 },
  { type: 'minecraft:three_layers_feature_size', limit: 1, lower_size: 0, middle_size: 1, upper_limit: 1, upper_size: 2 },
  [strandDecorator('banyan_roots', 0.15, 3, 10)]);
// Huangshan pine: a leaning trunk, branches held level, flat pads of needles, a flat parasol (TieredPineFeature).
CF('huangshan_pine', id('tiered_pine'), {});
// Jujube: small thorny tree, forked, a light ragged crown with red dates.
tree('jujube', 'jujube', { type: 'minecraft:forking_trunk_placer', base_height: 3, height_rand_a: 2, height_rand_b: 1 },
  { type: 'minecraft:random_spread_foliage_placer', foliage_height: 2, leaf_placement_attempts: 40, offset: 0, radius: 2 }, twoLayers(1, 0, 2));
// Dove tree: tall straight trunk, a high open crown hung with white bracts.
tree('dove_tree', 'dove_tree', { type: 'minecraft:straight_trunk_placer', base_height: 7, height_rand_a: 3, height_rand_b: 1 },
  { type: 'minecraft:random_spread_foliage_placer', foliage_height: 4, leaf_placement_attempts: 90, offset: 0, radius: 3 }, twoLayers(1, 0, 1));
// Weeping willow: short trunk, a loose crown, long curtains of fronds sweeping the water.
tree('willow', 'willow', { type: 'minecraft:straight_trunk_placer', base_height: 4, height_rand_a: 2, height_rand_b: 0 },
  { type: 'minecraft:random_spread_foliage_placer', foliage_height: 3, leaf_placement_attempts: 80, offset: 0, radius: 3 }, twoLayers(1, 0, 1),
  [strandDecorator('willow_strands', 0.8, 3, 8)]);
// Peach: small twiggy tree with deep pink blossom (cherry trunk and foliage shapes, peach blocks).
tree('peach', 'peach', { type: 'minecraft:cherry_trunk_placer', base_height: 4,
  branch_count: { type: 'minecraft:weighted_list', distribution: [{ data: 1, weight: 1 }, { data: 2, weight: 2 }, { data: 3, weight: 1 }] },
  branch_end_offset_from_top: uniform(-1, 0), branch_horizontal_length: uniform(2, 3),
  branch_start_offset_from_top: { min_inclusive: -3, max_inclusive: -2 }, height_rand_a: 1, height_rand_b: 0 },
  { type: 'minecraft:cherry_foliage_placer', corner_hole_chance: 0.25, hanging_leaves_chance: 0.12, hanging_leaves_extension_chance: 0.2,
    height: 4, offset: 0, radius: 3, wide_bottom_layer_hole_chance: 0.25 }, twoLayers(1, 0, 2));
// Desert poplar: bent, forked, rough-barked trunk with a golden ragged crown.
tree('desert_poplar', 'desert_poplar', { type: 'minecraft:forking_trunk_placer', base_height: 4, height_rand_a: 2, height_rand_b: 2 },
  { type: 'minecraft:random_spread_foliage_placer', foliage_height: 2, leaf_placement_attempts: 35, offset: 0, radius: 2 }, twoLayers(1, 0, 2), [], 'minecraft:coarse_dirt');
// "Dead for a thousand years, standing": bare poplar snags in the gravel.
CF('desert_poplar_snag', 'minecraft:block_column', { allowed_placement: { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' },
  direction: 'up', prioritize_tip: false, layers: [{ height: uniform(2, 5), provider: logState('desert_poplar') }] });

// Patches of plants.
function patch(name, block, tries, spread, survive = true, props) {
  const to = block.startsWith('{') ? JSON.parse(block) : provider(block, props);
  const filter = survive
    ? { type: 'minecraft:all_of', predicates: [{ type: 'minecraft:matching_blocks', blocks: 'minecraft:air' },
        { type: 'minecraft:would_survive', state: to.state || to.entries[0].data }] }
    : { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' };
  CF(name, 'minecraft:random_patch', { tries, xz_spread: spread, y_spread: 3, feature: {
    feature: { type: 'minecraft:simple_block', config: { to_place: to } },
    placement: [{ type: 'minecraft:block_predicate_filter', predicate: filter }] } });
}
const half = { half: 'lower' };
patch('patch_karst_fern', id('karst_fern'), 48, 7);
patch('patch_feather_grass', id('feather_grass'), 64, 7);
patch('patch_sea_buckthorn', id('sea_buckthorn'), 16, 5);
patch('patch_saxaul', id('saxaul'), 10, 6);
patch('patch_cistanche', id('cistanche'), 4, 4);
patch('patch_arrow_bamboo', id('arrow_bamboo'), 64, 7);
patch('patch_bamboo_shoot', id('bamboo_shoot'), 12, 5);
patch('flower_chrysanthemum', id('chrysanthemum'), 32, 6);
patch('flower_rapeseed', id('rapeseed'), 64, 7);
patch('patch_dead_bush', 'minecraft:dead_bush', 6, 6);
for (const tall of ['rhododendron', 'tree_fern', 'wild_rice', 'tamarisk']) {
  CF(`patch_${tall}`, 'minecraft:random_patch', { tries: tall === 'wild_rice' ? 48 : 24, xz_spread: 6, y_spread: 3, feature: {
    feature: { type: 'minecraft:simple_block', config: { to_place: provider(id(tall), half) } },
    placement: [{ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:all_of', predicates: [
      { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' }, { type: 'minecraft:matching_blocks', offset: [0, 1, 0], blocks: 'minecraft:air' },
      { type: 'minecraft:would_survive', state: state(id(tall), half) }] } }] } });
}
CF('patch_lotus', 'minecraft:random_patch', { tries: 16, xz_spread: 7, y_spread: 3, feature: {
  feature: { type: 'minecraft:simple_block', config: { to_place: weighted([[state(id('lotus_pad')), 3], [state(id('lotus_blossom')), 1]]) } },
  placement: [{ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:all_of', predicates: [
    { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' }, { type: 'minecraft:would_survive', state: state(id('lotus_pad')) }] } }] } });
CF('patch_pink_petals', 'minecraft:random_patch', { tries: 64, xz_spread: 6, y_spread: 2, feature: {
  feature: { type: 'minecraft:simple_block', config: { to_place: weighted([1, 2, 3, 4].flatMap(n => ['north', 'east', 'south', 'west']
    .map(f => [state('minecraft:pink_petals', { facing: f, flower_amount: String(n) }), 1]))) } },
  placement: [{ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:all_of', predicates: [
    { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' },
    { type: 'minecraft:would_survive', state: state('minecraft:pink_petals', { facing: 'north', flower_amount: '1' }) }] } }] } });
CF('patch_grass_lush', 'minecraft:random_patch', { tries: 48, xz_spread: 7, y_spread: 3, feature: {
  feature: { type: 'minecraft:simple_block', config: { to_place: weighted([[state('minecraft:short_grass'), 4], [state('minecraft:fern'), 1]]) } },
  placement: [{ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:all_of', predicates: [
    { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' }, { type: 'minecraft:would_survive', state: state('minecraft:short_grass') }] } }] } });

// Rock peaks, yardangs, ground bodies, groves, cliff pines, ponds.
const P = b => provider(id(b));
// The peaks: the rock in long vertical streaks of its accent (wet dark runs on limestone, damp joints on granite), soil
// on every ledge with its low plants, and trees grown on the summit.
const grassTop = provider('minecraft:grass_block', { snowy: 'false' });
CF('karst_tower', id('rock_pillar'), { style: 'tower', rock: P('karst_limestone'),
  accent: weighted([[state('minecraft:tuff'), 3], [state(id('mossy_karst_limestone')), 2]]), top: grassTop,
  plant: weighted([[state(id('karst_fern')), 3], [state('minecraft:short_grass'), 2]]),
  radius: uniform(4, 8), height: uniform(24, 58), summit: id('trees_karst') });
CF('granite_spire', id('rock_pillar'), { style: 'spire', rock: P('pale_granite'),
  accent: weighted([[state('minecraft:stone'), 2], [state('minecraft:andesite'), 1]]),
  top: weighted([[state('minecraft:moss_block'), 2], [state('minecraft:grass_block', { snowy: 'false' }), 1]]),
  plant: weighted([[state('minecraft:short_grass'), 2], [state('minecraft:moss_carpet'), 1]]),
  radius: uniform(3, 6), height: uniform(28, 64), summit: id('huangshan_pine') });
CF('sandstone_pillar', id('rock_pillar'), { style: 'pillar', rock: P('pillar_sandstone'), accent: P('pillar_sandstone'),
  top: grassTop, plant: weighted([[state('minecraft:short_grass'), 3], [state('minecraft:fern'), 1]]),
  radius: uniform(2, 4), height: uniform(26, 58), summit: id('huangshan_pine') });
CF('yardang', id('yardang'), { body: provider('minecraft:smooth_sandstone'), band: provider('minecraft:sandstone'),
  length: uniform(14, 38), height: uniform(5, 13), width: uniform(3, 6) });
const ground = (name, biome, body, band, depth, spacing) => CF(name, id('ground_body'),
  { biome: id(biome), body: state(body), band: state(band), depth, band_spacing: spacing });
ground('ground_body_karst', 'jade_karst_forest', id('karst_limestone'), id('karst_limestone'), 26, 0);
ground('ground_body_loess', 'loess_plateau', id('loess'), id('red_loess'), 64, 11);
ground('ground_body_gobi', 'hexi_frontier_desert', 'minecraft:sandstone', 'minecraft:smooth_sandstone', 10, 9);
ground('ground_body_peaks', 'cloud_sea_peaks', id('pale_granite'), id('pale_granite'), 48, 0);
ground('ground_body_peach', 'peach_blossom_valley', id('pillar_sandstone'), id('pillar_sandstone'), 22, 0);
// Jiangnan: fields, strips and canals out of the shallow lake.
CF('jiangnan_waterlands', id('waterlands'), { biome: id('jiangnan_waterlands'), land: 0.3 });
CF('moso_bamboo_grove', id('moso_bamboo_grove'), {});
CF('cliff_pine', id('cliff_pine'), {});
CF('jiangnan_pond', id('shallow_pond'), {});
// Loess gullies: frequent, shallow, wide canyons that open at the plateau surface.
write(D(NS, 'worldgen', 'configured_carver', 'loess_gully.json'), { type: 'minecraft:canyon', config: {
  probability: 0.08, y: { type: 'minecraft:uniform', min_inclusive: { absolute: 70 }, max_inclusive: { absolute: 110 } },
  yScale: 2.6, lava_level: { above_bottom: 8 }, replaceable: '#minecraft:overworld_carver_replaceables',
  vertical_rotation: { type: 'minecraft:uniform', min_inclusive: -0.125, max_exclusive: 0.125 },
  shape: { distance_factor: { type: 'minecraft:uniform', min_inclusive: 0.6, max_exclusive: 1.0 }, thickness: {
    type: 'minecraft:trapezoid', min: 0.0, max: 5.0, plateau: 2.0 }, width_smoothness: 3,
    horizontal_radius_factor: { type: 'minecraft:uniform', min_inclusive: 0.9, max_exclusive: 1.3 },
    vertical_radius_default_factor: 1.0, vertical_radius_center_factor: 0.0 } } });
tag('block', 'overworld_carver_replaceables', [id('loess'), id('red_loess'), id('karst_limestone'), id('pale_granite'), id('pillar_sandstone'), id('gobi_gravel')]);

// ---------------------------------------------------------------- placed features
const inSquare = { type: 'minecraft:in_square' };
const biomeFilter = { type: 'minecraft:biome' };
const count = n => ({ type: 'minecraft:count', count: n });
const chance = n => ({ type: 'minecraft:rarity_filter', chance: n });
const heightmap = h => ({ type: 'minecraft:heightmap', heightmap: h });
const dry = { type: 'minecraft:surface_water_depth_filter', max_water_depth: 0 };
const survives = s => ({ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:would_survive', state: s } });
// Patchy like Minecraft's own vegetation: many where the 2D biome noise is high, few where it is low (groves and
// clearings, fields of flowers) instead of an even sprinkle.
const patchy = (level, below, above) => ({ type: 'minecraft:noise_threshold_count', noise_level: level, below_noise: below, above_noise: above });
const weightedCount = pairs => ({ type: 'minecraft:count', count: { type: 'minecraft:weighted_list',
  distribution: pairs.map(([data, weight]) => ({ data, weight })) } });
const treePlacement = (n, sapling) => [n, inSquare, dry, heightmap('OCEAN_FLOOR'), biomeFilter, survives(state(id(sapling), { stage: '0' }))];
const surfacePatch = n => [...(typeof n === 'number' ? [count(n)] : [n]), inSquare, heightmap('WORLD_SURFACE_WG'), biomeFilter];
// A tree list feature: pick one tree per attempt.
function treeMix(name, entries, fallback) {
  // Entries are placed features: inline ones with no extra placement.
  const inline = feature => ({ feature, placement: [] });
  CF(name, 'minecraft:random_selector', { default: inline(fallback), features: entries.map(([feature, c]) => ({ chance: c, feature: inline(feature) })) });
}
treeMix('trees_karst', [[id('banyan'), 0.25]], id('osmanthus'));
PF('trees_karst', id('trees_karst'), treePlacement(patchy(0, 1, 6), 'osmanthus_sapling'));
PF('trees_karst_giant', id('banyan_giant'), [chance(18), inSquare, dry, heightmap('OCEAN_FLOOR'), biomeFilter, survives(state(id('banyan_sapling'), { stage: '0' }))]);
PF('bamboo_clumps_karst', 'minecraft:bamboo_no_podzol', [chance(3), inSquare, heightmap('WORLD_SURFACE_WG'), biomeFilter]);
PF('trees_loess', id('jujube'), treePlacement(patchy(0.35, 0, 2), 'jujube_sapling'));
PF('trees_desert_poplar', id('desert_poplar'), [chance(5), weightedCount([[2, 1], [4, 1]]), inSquare, dry, heightmap('OCEAN_FLOOR'), biomeFilter]);
PF('desert_poplar_snag', id('desert_poplar_snag'), [chance(10), inSquare, heightmap('WORLD_SURFACE_WG'), biomeFilter]);
PF('trees_huangshan_pine', id('huangshan_pine'), treePlacement(patchy(0, 1, 4), 'huangshan_pine_sapling'));
PF('cliff_pine', id('cliff_pine'), [count(14), inSquare, { type: 'minecraft:height_range', height: { type: 'minecraft:uniform',
  min_inclusive: { absolute: 72 }, max_inclusive: { absolute: 240 } } }, biomeFilter]);
PF('moso_bamboo_grove', id('moso_bamboo_grove'), [count(3), inSquare, heightmap('WORLD_SURFACE_WG'), biomeFilter]);
PF('trees_dove_tree', id('dove_tree'), treePlacement(weightedCount([[0, 3], [1, 2]]), 'dove_tree_sapling'));
PF('trees_willow', id('willow'), treePlacement(patchy(-0.2, 1, 4), 'willow_sapling'));
PF('trees_peach', id('peach'), treePlacement(patchy(0, 1, 4), 'peach_sapling'));
PF('patch_karst_fern', id('patch_karst_fern'), surfacePatch(patchy(0, 1, 5)));
PF('patch_feather_grass', id('patch_feather_grass'), surfacePatch(patchy(0, 1, 6)));
PF('patch_sea_buckthorn', id('patch_sea_buckthorn'), surfacePatch(chance(2)));
PF('patch_tamarisk', id('patch_tamarisk'), surfacePatch(chance(2)));
PF('patch_saxaul', id('patch_saxaul'), surfacePatch(1));
PF('patch_cistanche', id('patch_cistanche'), surfacePatch(chance(4)));
PF('patch_dead_bush', id('patch_dead_bush'), surfacePatch(1));
PF('patch_rhododendron', id('patch_rhododendron'), surfacePatch(chance(2)));
PF('patch_arrow_bamboo', id('patch_arrow_bamboo'), surfacePatch(patchy(0, 1, 5)));
PF('patch_tree_fern', id('patch_tree_fern'), surfacePatch(1));
PF('patch_bamboo_shoot', id('patch_bamboo_shoot'), surfacePatch(1));
PF('patch_wild_rice', id('patch_wild_rice'), surfacePatch(2));
PF('patch_lotus', id('patch_lotus'), surfacePatch(patchy(0, 1, 8)));
PF('flower_chrysanthemum', id('flower_chrysanthemum'), surfacePatch(chance(2)));
PF('flower_rapeseed', id('flower_rapeseed'), surfacePatch(patchy(0.25, 0, 8)));
PF('patch_pink_petals', id('patch_pink_petals'), surfacePatch(patchy(0, 0, 4)));
PF('patch_grass_lush', id('patch_grass_lush'), surfacePatch(patchy(-0.2, 2, 6)));
PF('karst_tower', id('karst_tower'), [weightedCount([[0, 4], [1, 4], [2, 1]]), inSquare, biomeFilter]);
PF('granite_spire', id('granite_spire'), [weightedCount([[0, 2], [1, 3], [2, 1]]), inSquare, biomeFilter]);
PF('sandstone_pillar', id('sandstone_pillar'), [weightedCount([[0, 5], [1, 3], [2, 1]]), inSquare, biomeFilter]);
PF('yardang', id('yardang'), [chance(3), inSquare, biomeFilter]);
PF('jiangnan_pond', id('jiangnan_pond'), [count(2), inSquare, heightmap('OCEAN_FLOOR_WG'), biomeFilter]);
for (const g of ['karst', 'loess', 'gobi', 'peaks', 'peach']) PF(`ground_body_${g}`, id(`ground_body_${g}`), []);
PF('jiangnan_waterlands', id('jiangnan_waterlands'), []);

// ---------------------------------------------------------------- biomes
const plains = JSON.parse(fs.readFileSync(path.join(__dirname, 'vanilla', 'plains_features.json'), 'utf8'));
// One global order for every Murim feature in each step, so no two biomes ever disagree on order.
const ORDER = {
  0: ['ground_body_karst', 'ground_body_loess', 'ground_body_gobi', 'ground_body_peaks', 'ground_body_peach'],
  1: ['jiangnan_waterlands', 'jiangnan_pond'],
  2: ['karst_tower', 'granite_spire', 'sandstone_pillar', 'yardang'],
  9: ['trees_karst', 'trees_karst_giant', 'bamboo_clumps_karst', 'trees_loess', 'trees_desert_poplar', 'desert_poplar_snag',
    'trees_huangshan_pine', 'cliff_pine', 'moso_bamboo_grove', 'trees_dove_tree', 'trees_willow', 'trees_peach',
    'patch_karst_fern', 'patch_feather_grass', 'patch_sea_buckthorn', 'patch_tamarisk', 'patch_saxaul', 'patch_cistanche',
    'patch_dead_bush', 'patch_rhododendron', 'patch_arrow_bamboo', 'patch_tree_fern', 'patch_bamboo_shoot', 'patch_wild_rice',
    'patch_lotus', 'flower_chrysanthemum', 'flower_rapeseed', 'patch_pink_petals', 'patch_grass_lush'],
};
const VANILLA_TAIL = ['minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin'];
function features(mine, extra = {}) {
  const steps = plains.map(list => list.slice());
  steps[9] = ['minecraft:glow_lichen'];
  for (const [step, names] of Object.entries(ORDER)) {
    for (const n of names) if (mine.includes(n)) steps[step].push(id(n));
  }
  steps[9].push(...VANILLA_TAIL.filter(f => (extra.vanilla || []).includes(f)));
  return steps;
}
const spawn = (type, weight, min, max) => ({ type: `minecraft:${type}`, weight, minCount: min, maxCount: max });
const MONSTERS = [spawn('spider', 100, 4, 4), spawn('zombie', 95, 4, 4), spawn('zombie_villager', 5, 1, 1), spawn('skeleton', 100, 4, 4),
  spawn('creeper', 100, 4, 4), spawn('slime', 100, 4, 4), spawn('enderman', 10, 1, 4), spawn('witch', 5, 1, 1)];
const DESERT_MONSTERS = [spawn('spider', 100, 4, 4), spawn('zombie', 19, 4, 4), spawn('zombie_villager', 1, 1, 1), spawn('husk', 80, 4, 4),
  spawn('skeleton', 100, 4, 4), spawn('creeper', 100, 4, 4), spawn('slime', 100, 4, 4), spawn('enderman', 10, 1, 4), spawn('witch', 5, 1, 1)];
const hex = s => parseInt(s.replace('#', ''), 16);
function biome(name, { temperature, downfall, precipitation = true, sky, fog, water, waterFog, grass, foliage, music, creatures,
                         monsters = MONSTERS, waterCreatures = [], waterAmbient = [], mine, vanilla = [], carvers = [] }) {
  write(D(NS, 'worldgen', 'biome', `${name}.json`), {
    has_precipitation: precipitation, temperature, downfall,
    effects: {
      sky_color: hex(sky), fog_color: hex(fog), water_color: hex(water), water_fog_color: hex(waterFog), grass_color: hex(grass),
      foliage_color: hex(foliage),
      mood_sound: { block_search_extent: 8, offset: 2.0, sound: 'minecraft:ambient.cave', tick_delay: 6000 },
      music: { sound: music, min_delay: 12000, max_delay: 24000, replace_current_music: false },
    },
    carvers: { air: ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon', ...carvers] },
    features: features(mine, { vanilla }),
    spawn_costs: {},
    spawners: { ambient: [spawn('bat', 10, 8, 8)], axolotls: [], creature: creatures, misc: [], monster: monsters,
      underground_water_creature: [spawn('glow_squid', 10, 4, 6)], water_ambient: waterAmbient, water_creature: waterCreatures },
  });
}
biome('jade_karst_forest', { temperature: 0.95, downfall: 0.9, sky: '#8FB8C8', fog: '#C9D6D3', water: '#3E8F7E', waterFog: '#2A5E55',
  grass: '#5FA046', foliage: '#3F8A3A', music: 'minecraft:music.overworld.jungle',
  creatures: [spawn('pig', 10, 4, 4), spawn('chicken', 10, 4, 4), spawn('cow', 6, 2, 4), spawn('parrot', 20, 1, 2)],
  waterAmbient: [spawn('salmon', 5, 1, 5)], waterCreatures: [spawn('squid', 2, 1, 4)],
  mine: ['ground_body_karst', 'karst_tower', 'trees_karst', 'trees_karst_giant', 'bamboo_clumps_karst', 'patch_karst_fern', 'patch_grass_lush'],
  vanilla: ['minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane'] });
biome('loess_plateau', { temperature: 0.7, downfall: 0.3, sky: '#B9C4CC', fog: '#D8C9A3', water: '#A88A4E', waterFog: '#7A6236',
  grass: '#A3A55C', foliage: '#7D8E4A', music: 'minecraft:music.overworld.meadow',
  creatures: [spawn('sheep', 12, 2, 4), spawn('rabbit', 8, 2, 3), spawn('horse', 4, 2, 4), spawn('chicken', 6, 2, 4)],
  mine: ['ground_body_loess', 'trees_loess', 'patch_feather_grass', 'patch_sea_buckthorn', 'patch_dead_bush'],
  vanilla: ['minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_pumpkin'], carvers: [id('loess_gully')] });
biome('hexi_frontier_desert', { temperature: 2.0, downfall: 0.0, precipitation: false, sky: '#9FC4E8', fog: '#E3CFA3', water: '#4C9B9A',
  waterFog: '#2E5F5E', grass: '#BFB06A', foliage: '#A8A060', music: 'minecraft:music.overworld.desert',
  creatures: [spawn('rabbit', 4, 2, 3), spawn('camel', 1, 1, 1)], monsters: DESERT_MONSTERS,
  mine: ['ground_body_gobi', 'yardang', 'trees_desert_poplar', 'desert_poplar_snag', 'patch_tamarisk', 'patch_saxaul', 'patch_cistanche', 'patch_dead_bush'] });
biome('cloud_sea_peaks', { temperature: 0.3, downfall: 0.9, sky: '#A9B8C2', fog: '#DCE3E6', water: '#4A8A9A', waterFog: '#2F5A66',
  grass: '#6B9A5A', foliage: '#3D6B44', music: 'minecraft:music.overworld.jagged_peaks',
  creatures: [spawn('goat', 5, 1, 3), spawn('rabbit', 4, 2, 3)],
  mine: ['ground_body_peaks', 'granite_spire', 'trees_huangshan_pine', 'cliff_pine', 'patch_rhododendron', 'patch_grass_lush'] });
biome('shu_bamboo_sea', { temperature: 0.75, downfall: 0.9, sky: '#A4B2B5', fog: '#B7C7B8', water: '#3D7A6A', waterFog: '#28524A',
  grass: '#6FAF4A', foliage: '#5E9E3C', music: 'minecraft:music.overworld.bamboo_jungle',
  creatures: [spawn('panda', 80, 1, 2), spawn('chicken', 10, 4, 4), spawn('pig', 8, 4, 4), spawn('parrot', 10, 1, 2)],
  mine: ['moso_bamboo_grove', 'trees_dove_tree', 'patch_arrow_bamboo', 'patch_tree_fern', 'patch_bamboo_shoot', 'patch_grass_lush'],
  vanilla: ['minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'] });
biome('jiangnan_waterlands', { temperature: 0.8, downfall: 0.95, sky: '#AEC3CF', fog: '#D3DDDC', water: '#5E9E92', waterFog: '#3F6E66',
  grass: '#7AB55A', foliage: '#5CA046', music: 'minecraft:music.overworld.swamp',
  creatures: [spawn('cow', 8, 2, 4), spawn('chicken', 10, 4, 4), spawn('pig', 6, 2, 4), spawn('frog', 10, 2, 5)],
  waterAmbient: [spawn('salmon', 8, 1, 5)], waterCreatures: [spawn('squid', 2, 1, 4)],
  mine: ['jiangnan_waterlands', 'jiangnan_pond', 'trees_willow', 'patch_wild_rice', 'patch_lotus', 'patch_grass_lush'],
  vanilla: ['minecraft:patch_sugar_cane'] });
biome('peach_blossom_valley', { temperature: 0.65, downfall: 0.8, sky: '#B6CDE0', fog: '#E7D9DE', water: '#5AA8A0', waterFog: '#3B7A73',
  grass: '#86C26A', foliage: '#6DB458', music: 'minecraft:music.overworld.cherry_grove',
  creatures: [spawn('rabbit', 6, 2, 3), spawn('sheep', 8, 2, 4), spawn('chicken', 8, 2, 4)],
  mine: ['ground_body_peach', 'sandstone_pillar', 'trees_peach', 'flower_chrysanthemum', 'flower_rapeseed', 'patch_pink_petals', 'patch_grass_lush'] });

// Biome tags.
const all = Object.keys(BIOME_NAMES).map(id);
tag('worldgen/biome', 'murim_biomes', all, NS);
tag('worldgen/biome', 'misty', [id('jade_karst_forest'), id('cloud_sea_peaks'), id('shu_bamboo_sea'), id('peach_blossom_valley'), id('jiangnan_waterlands')], NS);
// Armored crabs live along the karst rivers and the Jiangnan ponds too (merged into the existing tag).
{
  const crabTag = D(NS, 'tags', 'worldgen', 'biome', 'spawns_armored_crab.json');
  const values = JSON.parse(fs.readFileSync(crabTag, 'utf8')).values;
  for (const b of [id('jade_karst_forest'), id('jiangnan_waterlands')]) if (!values.includes(b)) values.push(b);
  write(crabTag, { replace: false, values });
}
tag('worldgen/biome', 'is_desert', [id('hexi_frontier_desert')], 'c');
tag('worldgen/biome', 'is_mountain', [id('cloud_sea_peaks')], 'c');
tag('worldgen/biome', 'is_plateau', [id('loess_plateau')], 'c');
tag('worldgen/biome', 'is_jungle', [id('jade_karst_forest')], 'c');
tag('worldgen/biome', 'is_forest', [id('shu_bamboo_sea'), id('peach_blossom_valley')], 'c');
tag('worldgen/biome', 'is_wet', [id('jiangnan_waterlands'), id('jade_karst_forest'), id('shu_bamboo_sea')], 'c');
tag('worldgen/biome', 'is_dry', [id('hexi_frontier_desert'), id('loess_plateau')], 'c');
tag('worldgen/biome', 'is_overworld', all, 'c');

console.log(`wrote ${written.length} files`);
