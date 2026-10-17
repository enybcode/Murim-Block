// Puts the Murim's plants in game (docs/cultivation/plants.md): from one list of the 43 (13 herbs, 30 flowers), writes
//   src/main/java/com/murimblock/world/plant/PlantCatalog.java   the species, in a FIXED order (their index is saved in
//                                                               worlds: append only, never reorder);
//   assets: the wild plants' still 3D block models (baked from their GeckoLib models, the boxes' turns allowed in Java
//           block models), blockstates (colours and turns at random), item models (pixel art icons), the crop's blockstate;
//   data: the wild plants' loot (the herb and sometimes its seed; the flower and sometimes its seed), their patches in their
//         regions' biomes (biome modifiers), rare spirit specimens of the herbs, the flowers' tags;
//   lang: every name, merged into en_us.json.
// Usage: node scripts/plants/plants-data.cjs
const fs = require('fs');
const path = require('path');
const { ASSETS, ROOT } = require('./plant-kit.cjs');
const { FLOWERS } = require('./flowers.cjs');
const { kindOf } = require('./seed-icons.cjs');

const DATA = path.join(ROOT, 'src', 'main', 'resources', 'data', 'murimblock');
const JAVA = path.join(ROOT, 'src', 'main', 'java', 'com', 'murimblock', 'world', 'plant');
const write = (file, obj) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2) + '\n'); };

const REGIONS = {
  centre: ['peach_blossom_valley', 'cloud_sea_peaks', 'wulingyuan_pillars'],
  north: ['tianshan_snowfields', 'kunlun_glaciers'],
  west: ['hexi_frontier_desert', 'loess_plateau'],
  south: ['jade_karst_forest', 'shu_bamboo_sea', 'camphor_hills'],
  east: ['jiangnan_waterlands', 'camphor_hills'],
};
const ALL = [...new Set(Object.values(REGIONS).flat())];
const SOIL_OF = { centre: 'GRASS', north: 'ALPINE', west: 'SAND', south: 'GRASS', east: 'WET' };
const CLIMATE_OF = { centre: 'MILD', north: 'COLD', west: 'HOT', south: 'MILD', east: 'MILD' };
const KIND_NAME = { seeds: 'Seeds', bulb: 'Bulb', cutting: 'Cutting', rhizome: 'Rhizome', spores: 'Spores' };

// ---------------------------------------------------------------- the herbs
// species (its models' name), its wild block (an existing one, or new), its herb item, names, harvest, where.
const HERBS = [
  { species: 'chrysanthemum', wild: 'chrysanthemum', existing: true, herb: 'chrysanthemum_flower', name: 'Chrysanthemum', herbName: 'Chrysanthemum Flower', rarity: 'common', region: 'centre', harvest: 'KEEP', colours: ['gold', 'white', 'purple'], stages: 4, biomes: ['peach_blossom_valley'] },
  { species: 'bamboo_shoot', wild: 'bamboo_shoot', existing: true, herb: 'fresh_bamboo_shoot', name: 'Bamboo Shoot', herbName: 'Fresh Bamboo Shoot', rarity: 'common', region: 'south', harvest: 'UPROOT', biomes: ['shu_bamboo_sea'] },
  { species: 'astragalus_root', wild: 'milk_vetch', existing: true, herb: 'astragalus_root', name: 'Astragalus', herbName: 'Astragalus Root', rarity: 'common', region: 'east', soil: 'GRASS', harvest: 'UPROOT', biomes: ['camphor_hills'] },
  { species: 'kunlun_sedge', wild: 'kunlun_sedge', existing: true, herb: 'kunlun_sedge_bundle', name: 'Kunlun Sedge', herbName: 'Kunlun Sedge Bundle', rarity: 'common', region: 'north', harvest: 'KEEP', biomes: REGIONS.north },
  { species: 'fern_frond', wild: 'karst_fern', existing: true, herb: 'fern_frond', name: 'Karst Fern', herbName: 'Fern Frond', rarity: 'common', region: 'south', harvest: 'KEEP', biomes: ['jade_karst_forest', 'wulingyuan_pillars'] },
  { species: 'sea_buckthorn_berries', wild: 'sea_buckthorn', existing: true, herb: 'sea_buckthorn_berries', name: 'Sea Buckthorn', herbName: 'Sea Buckthorn Berries', rarity: 'uncommon', region: 'west', harvest: 'KEEP', biomes: ['loess_plateau'] },
  { species: 'lotus_seed_pod', wild: 'lotus_blossom', existing: true, herb: 'lotus_seed_pod', name: 'Lotus', herbName: 'Lotus Seed Pod', rarity: 'uncommon', region: 'east', soil: 'WATER', harvest: 'KEEP', biomes: ['jiangnan_waterlands'], keepModel: true },
  { species: 'cistanche', wild: 'cistanche', existing: true, herb: 'cistanche_stem', name: 'Cistanche', herbName: 'Cistanche Stem', rarity: 'rare', region: 'west', harvest: 'UPROOT', biomes: ['hexi_frontier_desert'] },
  { species: 'rhododendron_blossom', wild: 'rhododendron', existing: true, herb: 'rhododendron_blossom', name: 'Rhododendron', herbName: 'Rhododendron Blossom', rarity: 'rare', region: 'centre', harvest: 'KEEP', biomes: ['cloud_sea_peaks'], keepModel: true },
  { species: 'snow_lotus', wild: 'snow_lotus', existing: true, herb: 'snow_lotus_flower', name: 'Snow Lotus', herbName: 'Snow Lotus Flower', rarity: 'rare', region: 'north', harvest: 'UPROOT', biomes: REGIONS.north },
  { species: 'ginseng_root', wild: 'ginseng', herb: 'ginseng_root', name: 'Ginseng', herbName: 'Ginseng Root', rarity: 'rare', region: 'centre', harvest: 'UPROOT', biomes: ['cloud_sea_peaks', 'peach_blossom_valley'] },
  { species: 'lingzhi', wild: 'lingzhi', herb: 'lingzhi_cap', name: 'Lingzhi', herbName: 'Lingzhi Cap', rarity: 'very_rare', region: 'centre', harvest: 'KEEP', biomes: ['wulingyuan_pillars', 'jade_karst_forest', 'shu_bamboo_sea'] },
  { species: 'goji_berries', wild: 'goji_bush', herb: 'goji_berries', name: 'Goji', herbName: 'Goji Berries', rarity: 'common', region: 'west', soil: 'SAND', harvest: 'KEEP', biomes: ['loess_plateau'] },
];
const ENGLISH = {
  peony: 'Peony', cymbidium_orchid: 'Cymbidium Orchid', camellia: 'Camellia', balloon_flower: 'Balloon Flower', red_spider_lily: 'Red Spider Lily',
  white_lily: 'White Lily', blue_poppy: 'Himalayan Blue Poppy', edelweiss: 'Edelweiss', gentian: 'Gentian', alpine_forget_me_not: 'Alpine Forget-me-not',
  snow_saxifrage: 'Snow Saxifrage', tianshan_tulip: 'Tianshan Tulip', desert_candle: 'Desert Candle', desert_iris: 'Desert Iris', chinese_pink: 'Chinese Pink',
  field_poppy: 'Field Poppy', caper_flower: 'Caper Flower', harmal: 'Harmal', hydrangea: 'Hydrangea', gardenia: 'Gardenia', hibiscus: 'Hibiscus',
  garden_balsam: 'Garden Balsam', ginger_lily: 'Ginger Lily', blue_flag_iris: 'Blue Flag Iris', water_lily: 'Water Lily', morning_glory: 'Morning Glory',
  daylily: 'Daylily', jasmine: 'Jasmine', ladybells: 'Ladybells', udumbara: 'Udumbara',
};
const REGION_FR = { Centre: 'centre', Nord: 'north', Ouest: 'west', Sud: 'south', Est: 'east' };

const SPECIES = [
  ...HERBS.map(h => ({ ...h, kind: 'HERB', soil: h.soil || SOIL_OF[h.region], climate: CLIMATE_OF[h.region], colours: h.colours || ['default'], stages: h.stages || 1 })),
  ...FLOWERS.map(f => {
    const region = REGION_FR[f.region] || 'any';
    return { species: f.name, wild: f.name, name: ENGLISH[f.name], kind: 'FLOWER', rarity: f.rarity, region, harvest: 'KEEP', colours: ['default'], stages: 1,
      soil: f.name === 'water_lily' ? 'WATER' : region === 'any' ? 'GRASS' : SOIL_OF[region], climate: region === 'any' ? 'ANY' : CLIMATE_OF[region],
      biomes: region === 'any' ? ALL : REGIONS[region], waterBlock: f.name === 'water_lily' };
  }),
];
for (const s of SPECIES) {
  s.seedKind = kindOf(s.species);
  s.seed = (s.kind === 'HERB' ? s.wild : s.species) + '_' + s.seedKind;
  s.seedIcon = `seed/${s.species}_${s.seedKind}`;
}

// ---------------------------------------------------------------- the Java catalog

function catalog() {
  const lines = SPECIES.map((s, i) => `        new Species(${i}, "${s.species}", "${s.wild}", ${s.existing ? 'true' : 'false'}, ${s.herb ? `"${s.herb}"` : 'null'}, "${s.seed}", Kind.${s.kind}, `
    + `Rarity.${s.rarity.toUpperCase()}, Soil.${s.soil}, Climate.${s.climate}, Harvest.${s.harvest}, ${s.stages}, List.of(${s.colours.map(c => `"${c}"`).join(", ")}))`);
  return `package com.murimblock.world.plant;

import java.util.List;

/**
 * The Murim's plants (docs/cultivation/plants.md). Written by scripts/plants/plants-data.cjs: do not edit by hand.
 * The order is saved in worlds (a crop's species is its index): new species go at the end, none is ever moved.
 */
public final class PlantCatalog {
    private PlantCatalog() {
    }

    public enum Kind { HERB, FLOWER }
    public enum Rarity { COMMON, UNCOMMON, RARE, VERY_RARE }
    /** What it roots in: as the wild plants (MurimPlantBlock.Soil), and WATER: floating on still water. */
    public enum Soil { GRASS, SAND, WET, ALPINE, WATER }
    /** The warmth it wants: COLD (the snows), HOT (the dry lands), MILD (the rest), ANY. */
    public enum Climate { COLD, MILD, HOT, ANY }
    /** Harvested, it stays (flowers, berries, leaves: it grows them back) or it is pulled up whole (roots, shoots). */
    public enum Harvest { KEEP, UPROOT }

    /**
     * A species: its index (saved), its models' name, its wild block (an existing block or one MurimFlora makes), its herb
     * item (herbs only) and what it grows from, its kind, rarity, soil, climate and harvest, its growth stages modelled (4,
     * or 1 grown by scale), and its colours.
     */
    public record Species(int index, String id, String wild, boolean existingWild, String herb, String seed, Kind kind, Rarity rarity,
                          Soil soil, Climate climate, Harvest harvest, int stagesModelled, List<String> colours) {
        public boolean isHerb() { return kind == Kind.HERB; }
    }

    public static final List<Species> ALL = List.of(
${lines.join(',\n')}
    );

    public static Species byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : ALL.get(0);
    }

    public static Species byId(String id) {
        for (Species s : ALL) if (s.id().equals(id)) return s;
        return null;
    }
}
`;
}

// ---------------------------------------------------------------- still 3D block models, from the GeckoLib models

/** A geo model's cubes as a Java block model: the plant's foot in the middle of the block. */
function blockModel(species, stage, colour) {
  const geo = JSON.parse(fs.readFileSync(path.join(ASSETS, 'geo', 'block', 'plant', `${species}_stage${stage}.geo.json`), 'utf8'))['minecraft:geometry'][0];
  const TW = geo.description.texture_width;
  const elements = [];
  for (const bone of geo.bones) {
    // The motes of spirit maturity float: not on a still plant.
    if (bone.name.startsWith('motes')) continue;
    for (const c of bone.cubes || []) {
      const from = [c.origin[0] + 8, c.origin[1], c.origin[2] + 8], to = from.map((v, i) => v + c.size[i]);
      const e = { from: from.map(v => +v.toFixed(3)), to: to.map(v => +v.toFixed(3)), faces: {} };
      if (c.rotation) {
        const axis = c.rotation[0] ? 'x' : c.rotation[1] ? 'y' : 'z';
        const angle = axis === 'z' ? c.rotation[2] : -c.rotation[axis === 'x' ? 0 : 1];
        e.rotation = { origin: [c.pivot[0] + 8, c.pivot[1], c.pivot[2] + 8].map(v => +v.toFixed(3)), axis, angle };
      }
      for (const [face, u] of Object.entries(c.uv)) {
        const thin = (face === 'north' || face === 'south') ? c.size[0] === 0 || c.size[1] === 0 : (face === 'east' || face === 'west') ? c.size[2] === 0 || c.size[1] === 0 : c.size[0] === 0 || c.size[2] === 0;
        if (thin) continue;
        const k = 16 / TW, uv = [u.uv[0] * k, u.uv[1] * k, (u.uv[0] + u.uv_size[0]) * k, (u.uv[1] + u.uv_size[1]) * k].map(v => +v.toFixed(4));
        e.faces[face] = { uv, texture: '#plant' };
      }
      if (Object.keys(e.faces).length) elements.push(e);
    }
  }
  return { ambientocclusion: false, render_type: 'minecraft:cutout',
    textures: { plant: `murimblock:block/plant/${species}_${colour}`, particle: `murimblock:block/plant/${species}_${colour}` }, elements };
}

// ---------------------------------------------------------------- loot, features, lang

const custom = (maturity, purity) => ({ function: 'minecraft:set_custom_data', tag: `{maturity:"${maturity}",purity:${purity}}` });
const SEED_CHANCE = { common: 0.3, uncommon: 0.25, rare: 0.2, very_rare: 0.5 };
function wildLoot(s) {
  const ownBlock = { type: 'minecraft:item', name: `murimblock:${s.wild}`, conditions: [{ condition: 'minecraft:match_tool', predicate: { items: 'minecraft:shears' } }] };
  const produce = s.herb
    ? { type: 'minecraft:item', name: `murimblock:${s.herb}`, functions: [custom('mature', 70)] }
    : { type: 'minecraft:item', name: `murimblock:${s.wild}` };
  const pools = [{ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:alternatives', children: s.herb ? [ownBlock, produce] : [produce] }],
    conditions: [{ condition: 'minecraft:survives_explosion' }] }];
  pools.push({ rolls: 1, bonus_rolls: 0, entries: [{ type: 'minecraft:item', name: `murimblock:${s.seed}` }],
    conditions: [{ condition: 'minecraft:random_chance', chance: SEED_CHANCE[s.rarity] }, { condition: 'minecraft:inverted', term: { condition: 'minecraft:match_tool', predicate: { items: 'minecraft:shears' } } }] });
  return { type: 'minecraft:block', pools };
}
const PATCH = { common: [3, 16], uncommon: [6, 10], rare: [16, 6], very_rare: [160, 2] };
function patch(stateName, tries, spread = 6, extra = {}) {
  return { type: 'minecraft:random_patch', config: { tries, xz_spread: spread, y_spread: 3, feature: { feature: { type: 'minecraft:simple_block',
    config: { to_place: { type: 'minecraft:simple_state_provider', state: { Name: stateName, ...extra } } } },
    placement: [{ type: 'minecraft:block_predicate_filter', predicate: { type: 'minecraft:all_of', predicates: [
      { type: 'minecraft:matching_blocks', blocks: 'minecraft:air' }, { type: 'minecraft:would_survive', state: { Name: stateName, ...extra } }] } }] } } };
}
const placed = (feature, chance, onWater = false) => ({ feature, placement: [{ type: 'minecraft:rarity_filter', chance }, { type: 'minecraft:in_square' },
  { type: 'minecraft:heightmap', heightmap: onWater ? 'WORLD_SURFACE_WG' : 'WORLD_SURFACE_WG' }, { type: 'minecraft:biome' }, { type: 'murimblock:away_from_sites', margin: 8 }] });

function main() {
  write(path.join(JAVA, 'PlantCatalog.java'), catalog());
  const lang = {};
  const flowerTag = [];
  const byBiomeSet = {};
  SPECIES.forEach((s, index) => {
    // Still 3D models of the wild plants (the existing blocks too, but for the tall rhododendron and the floating lotus).
    if (!s.keepModel) {
      const stage = s.stages === 4 ? 2 : 0;
      const models = s.colours.map(c => {
        const name = s.colours.length > 1 ? `plant/${s.wild}_${c}` : `plant/${s.wild}`;
        write(path.join(ASSETS, 'models', 'block', name + '.json'), blockModel(s.species, stage, c));
        return `murimblock:block/${name}`;
      });
      const variants = [];
      for (const m of models) for (const y of [0, 90, 180, 270]) variants.push({ model: m, y });
      write(path.join(ASSETS, 'blockstates', s.wild + '.json'), { variants: { '': variants } });
    }
    // Items: the wild block's own item shows the pixel art icon; the herb; what it grows from.
    const icon = s.kind === 'HERB' ? `herb/${s.species}` : `flower/${s.species}`;
    write(path.join(ASSETS, 'models', 'item', s.wild + '.json'), { parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/${icon}` } });
    if (s.herb) write(path.join(ASSETS, 'models', 'item', s.herb + '.json'), { parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/herb/${s.species}` } });
    write(path.join(ASSETS, 'models', 'item', s.seed + '.json'), { parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/${s.seedIcon}` } });
    // Loot of the wild plant.
    write(path.join(DATA, 'loot_table', 'blocks', s.wild + '.json'), wildLoot(s));
    // Where the new wild plants grow (the existing ones already have their patches).
    if (!s.existing) {
      const [chance, tries] = PATCH[s.rarity];
      write(path.join(DATA, 'worldgen', 'configured_feature', `plant_${s.wild}.json`), patch(`murimblock:${s.wild}`, tries, s.rarity === 'very_rare' ? 3 : 6));
      write(path.join(DATA, 'worldgen', 'placed_feature', `plant_${s.wild}.json`), placed(`murimblock:plant_${s.wild}`, chance, s.waterBlock));
      (byBiomeSet[s.biomes.join(',')] ||= []).push(`murimblock:plant_${s.wild}`);
    }
    // Rare spirit specimens of the herbs, glowing in the wild: a crop at spirit maturity.
    if (s.kind === 'HERB' && s.soil !== 'WATER') {
      write(path.join(DATA, 'worldgen', 'configured_feature', `spirit_${s.wild}.json`), patch('murimblock:murim_crop', 2, 4, { Properties: { species: String(index), age: '3' } }));
      write(path.join(DATA, 'worldgen', 'placed_feature', `spirit_${s.wild}.json`), placed(`murimblock:spirit_${s.wild}`, s.rarity === 'very_rare' ? 900 : 500));
      (byBiomeSet[s.biomes.join(',')] ||= []).push(`murimblock:spirit_${s.wild}`);
    }
    if (s.kind === 'FLOWER') flowerTag.push(`murimblock:${s.wild}`);
    // Names.
    if (!s.existing) lang[`block.murimblock.${s.wild}`] = s.name;
    if (s.herb) lang[`item.murimblock.${s.herb}`] = s.herbName;
    lang[`item.murimblock.${s.seed}`] = `${s.name} ${KIND_NAME[s.seedKind]}`;
  });
  // One biome modifier per set of biomes.
  Object.entries(byBiomeSet).forEach(([biomes, features], i) => {
    write(path.join(DATA, 'neoforge', 'biome_modifier', `plants_${i}.json`), { type: 'neoforge:add_features',
      biomes: biomes.split(',').map(b => `murimblock:${b}`), features, step: 'vegetal_decoration' });
  });
  // The crop: drawn by its block entity; its block model only gives the particles.
  write(path.join(ASSETS, 'blockstates', 'murim_crop.json'), { variants: { '': { model: 'murimblock:block/murim_crop' } } });
  write(path.join(ASSETS, 'models', 'block', 'murim_crop.json'), { textures: { particle: 'murimblock:block/plant/chrysanthemum_gold' } });
  write(path.join(DATA, 'tags', 'block', 'plant_flowers.json'), { values: flowerTag });
  // Names and texts the plants show.
  Object.assign(lang, {
    'block.murimblock.murim_crop': 'Planted Herb',
    'tooltip.murimblock.herb.maturity.young': 'Young', 'tooltip.murimblock.herb.maturity.mature': 'Mature',
    'tooltip.murimblock.herb.maturity.spirit': 'Spirit Maturity', 'tooltip.murimblock.herb.purity': 'Purity %s%%',
    'tooltip.murimblock.herb.rarity.common': 'Common', 'tooltip.murimblock.herb.rarity.uncommon': 'Uncommon',
    'tooltip.murimblock.herb.rarity.rare': 'Rare', 'tooltip.murimblock.herb.rarity.very_rare': 'Very Rare',
    'tooltip.murimblock.seed.plant': 'Plant it: %s', 'tooltip.murimblock.seed.soil.grass': 'in good earth', 'tooltip.murimblock.seed.soil.sand': 'in sand or dry earth',
    'tooltip.murimblock.seed.soil.wet': 'in wet earth or mud, near water', 'tooltip.murimblock.seed.soil.alpine': 'in rock, gravel or thin mountain soil',
    'tooltip.murimblock.seed.soil.water': 'on still water',
    'message.murimblock.crop.status': '%s: %s · light %s · warmth %s · water %s · purity %s%%',
    'message.murimblock.crop.stage.0': 'Seedling', 'message.murimblock.crop.stage.1': 'Young', 'message.murimblock.crop.stage.2': 'Mature', 'message.murimblock.crop.stage.3': 'Spirit maturity',
    'message.murimblock.crop.good': 'good', 'message.murimblock.crop.poor': 'poor', 'message.murimblock.crop.none': 'none',
    'message.murimblock.crop.qi': 'Spirit maturity needs Murim land rich in Qi, and few plants crowding it',
  });
  const lp = path.join(ASSETS, 'lang', 'en_us.json');
  let text = fs.readFileSync(lp, 'utf8');
  const have = JSON.parse(text);
  const add = Object.entries(lang).filter(([k]) => !(k in have));
  if (add.length) text = text.replace(/\n}\s*$/, ',\n' + add.map(([k, v]) => `  ${JSON.stringify(k)}: ${JSON.stringify(v)}`).join(',\n') + '\n}\n');
  JSON.parse(text);
  fs.writeFileSync(lp, text);
  console.log(`${SPECIES.length} species; ${add.length} names added; ${Object.keys(byBiomeSet).length} biome modifiers`);
}

if (require.main === module) main();
module.exports = { SPECIES };
