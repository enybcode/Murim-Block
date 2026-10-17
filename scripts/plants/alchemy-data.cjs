// The alchemy's data (docs/cultivation/alchemy.md): its icons (16 x 16 pixel art: the furnace, the manual, the recipe
// scrolls, the decoctions in their porcelain flasks, the pills, the residue), its item and block models, its names, the
// furnace's and the manual's crafting recipes, the furnace's loot, its tool tag, and the rare scrolls in the sites' caches.
// Usage: node scripts/plants/alchemy-data.cjs
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');
const { ASSETS, ROOT } = require('./plant-kit.cjs');

const DATA = path.join(ROOT, 'src', 'main', 'resources', 'data', 'murimblock');
const write = (file, obj) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, Buffer.isBuffer(obj) ? obj : typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2) + '\n'); };

const FLASK = ['................', '.......KK.......', '......KCCK......', '.......SS.......', '......SLLS......', '.....SLPPPS.....', '....SLPPPPPS....',
  '....SLPPPPPS....', '....SPPPPPDS....', '....SPPPPDDS....', '.....SPPDDS.....', '......SSSS......', '................', '................', '................', '................'];
const PILL = ['................', '................', '.....A....A.....', '................', '.......SS.......', '.....SSLLSS.....', '....SLLPPPPS....',
  '....SLPPPPPS....', '....SPPPPPDS....', '....SPPPPDDS....', '.....SPDDDS.....', '......SSSS......', '...A........A...', '................', '................', '................'];
const SCROLL = ['................', '................', '..WW.........WW.', '..WRWWWWWWWWWRW.', '..WRPPPPPPPPPRW.', '...WPDPPDPDPPW..', '...WPPPPPPPPPW..',
  '...WPDPDPPDPPW..', '...WPPPPPPPPPW..', '...WPPPDPPDPPW..', '...WPPPPPPPGGW..', '..WRPPPPPPPGGRW.', '..WRWWWWWWWWWRW.', '..WW.........WW.', '................', '................'];
const ICONS = {
  alchemy_furnace: { rows: ['.......Y........', '......YYY.......', '.....GGGGG......', '....GYGYGYG.....', '...GGGGGGGGG....', '..GGRGGRGGRGG...', '..GGGGGGGGGGG...',
    '..GLGGGGGGGLG...', '...GGGGGGGGG....', '....GGGGGGG.....', '....D..F..D.....', '...DD.FFF.DD....', '..DD..FOF..DD...', '.......F........', '................', '................'],
    pal: { G: '#a8742a', Y: '#e8c060', R: '#ffb040', L: '#e0b050', D: '#5a3612', F: '#ff9a2a', O: '#ffe680' } },
  alchemy_manual: { rows: ['................', '..KKKKKKKKKKKK..', '..KRRRRRRRRRRPK.', '..KRRRRRRRRRRPK.', '..KRRRRYYRRRRPK.', '..KRRRYGGYRRRPK.', '..KRRYGGGGYRRPK.',
    '..KRRYGGGGYRRPK.', '..KRRRYGGYRRRPK.', '..KRRRRDDRRRRPK.', '..KRRRYRRYRRRPK.', '..KRRRRRRRRRRPK.', '..KDDDDDDDDDDPK.', '..KKKKKKKKKKKKD.', '................', '................'],
    pal: { K: '#2a1208', R: '#8a2018', D: '#5a1008', Y: '#e8c060', G: '#c08a30', P: '#f0e4c8' } },
  martial_elixir_recipe: { rows: SCROLL, pal: { W: '#6a4a2a', R: '#c03a2a', P: '#efe2c0', D: '#3a2a20', G: '#c82a2a' } },
  soul_soothing_pill_recipe: { rows: SCROLL, pal: { W: '#6a4a2a', R: '#8a3ac0', P: '#efe2c0', D: '#3a2a20', G: '#a050e0' } },
  healing_decoction: { rows: FLASK, pal: { K: '#8a5a30', C: '#c08a50', S: '#3a5a3a', L: '#f0f4ec', P: '#d8e8d8', D: '#7ab88a' } },
  qi_decoction: { rows: FLASK, pal: { K: '#8a5a30', C: '#c08a50', S: '#2a4a6a', L: '#f0f4fc', P: '#d8e4f4', D: '#5a9ad8' } },
  purifying_brew: { rows: FLASK, pal: { K: '#8a5a30', C: '#c08a50', S: '#3a6a5a', L: '#f4fffa', P: '#c8ecdc', D: '#4ab890' } },
  martial_elixir: { rows: PILL, pal: { S: '#5a0a0a', L: '#ff9a8a', P: '#d8302a', D: '#901a14', A: '#ffd0a0' } },
  soul_soothing_pill: { rows: PILL, pal: { S: '#3a1a5a', L: '#fff0b8', P: '#e8c060', D: '#a0782a', A: '#d8b0ff' } },
  pill_residue: { rows: ['................', '................', '................', '................', '................', '.......SS.......', '.....SSDDS......',
    '....SDDKDDS.S...', '...SDKDDDKDS....', '...SDDDKDDDS....', '....SSDDDSS.....', '..S...SSS...S...', '................', '................', '................', '................'],
    pal: { S: '#2a2622', D: '#4a4440', K: '#6a3a2a' } },
};

function icon({ rows, pal }) {
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    if (l === '.') return;
    const c = pal[l];
    if (!c) throw new Error('no colour ' + l);
    img.set([parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16), 255], (y * 16 + x) * 4);
  }));
  return kit.png(16, 16, img);
}

for (const [name, def] of Object.entries(ICONS)) {
  write(path.join(ASSETS, 'textures', 'item', 'alchemy', name + '.png'), icon(def));
  write(path.join(ASSETS, 'models', 'item', name + '.json'), { parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/alchemy/${name}` } });
}
// The furnace's block: drawn by its block entity; its model only gives the particles.
write(path.join(ASSETS, 'blockstates', 'alchemy_furnace.json'), { variants: { 'lit=false': { model: 'murimblock:block/alchemy_furnace' }, 'lit=true': { model: 'murimblock:block/alchemy_furnace' } } });
write(path.join(ASSETS, 'models', 'block', 'alchemy_furnace.json'), { textures: { particle: 'minecraft:block/gold_block' } });
write(path.join(DATA, 'loot_table', 'blocks', 'alchemy_furnace.json'), { type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'murimblock:alchemy_furnace' }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
const tagFile = path.join(ROOT, 'src', 'main', 'resources', 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json');
const tag = fs.existsSync(tagFile) ? JSON.parse(fs.readFileSync(tagFile, 'utf8')) : { replace: false, values: [] };
if (!tag.values.includes('murimblock:alchemy_furnace')) tag.values.push('murimblock:alchemy_furnace');
write(tagFile, tag);
// Crafting: the furnace of copper and gold over coal; the manual from a book and two herbs.
write(path.join(DATA, 'recipe', 'alchemy_furnace.json'), { type: 'minecraft:crafting_shaped', category: 'misc', pattern: [' G ', 'CBC', 'C C'],
  key: { G: { item: 'minecraft:gold_ingot' }, C: { item: 'minecraft:copper_block' }, B: { item: 'minecraft:coal_block' } }, result: { id: 'murimblock:alchemy_furnace', count: 1 } });
write(path.join(DATA, 'recipe', 'alchemy_manual.json'), { type: 'minecraft:crafting_shapeless', category: 'misc',
  ingredients: [{ item: 'minecraft:book' }, { item: 'murimblock:chrysanthemum_flower' }, { item: 'murimblock:lotus_seed_pod' }], result: { id: 'murimblock:alchemy_manual', count: 1 } });
// The rare scrolls in the sites' caches and the arena's chests.
for (const [chest, weight] of [['murim_cache', 2], ['alliance_arena', 1], ['bandit_camp', 1]]) {
  const file = path.join(DATA, 'loot_table', 'chests', chest + '.json');
  if (!fs.existsSync(file)) continue;
  const table = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (JSON.stringify(table).includes('martial_elixir_recipe')) continue;
  table.pools.push({ rolls: 1, entries: [{ type: 'minecraft:empty', weight: 24 - weight * 4 }, { type: 'minecraft:item', name: 'murimblock:martial_elixir_recipe', weight: weight * 3 },
    { type: 'minecraft:item', name: 'murimblock:soul_soothing_pill_recipe', weight }, { type: 'minecraft:item', name: 'murimblock:alchemy_manual', weight: weight * 2 }] });
  write(file, table);
}

const lang = {
  'block.murimblock.alchemy_furnace': 'Pill Furnace',
  'item.murimblock.alchemy_manual': 'Alchemy Manual', 'item.murimblock.martial_elixir_recipe': 'Recipe Scroll: Martial Elixir',
  'item.murimblock.soul_soothing_pill_recipe': 'Recipe Scroll: Soul-Soothing Pill',
  'item.murimblock.healing_decoction': 'Healing Decoction', 'item.murimblock.qi_decoction': 'Qi Decoction', 'item.murimblock.purifying_brew': 'Purifying Brew',
  'item.murimblock.martial_elixir': 'Martial Elixir', 'item.murimblock.soul_soothing_pill': 'Soul-Soothing Pill', 'item.murimblock.pill_residue': 'Pill Residue',
  'alchemy.murimblock.recipe.qi_gathering_pill': 'Qi Gathering Pill', 'alchemy.murimblock.recipe.realm_breaking_pill': 'Realm-Breaking Pill',
  'alchemy.murimblock.recipe.healing_decoction': 'Healing Decoction', 'alchemy.murimblock.recipe.qi_decoction': 'Qi Decoction',
  'alchemy.murimblock.recipe.purifying_brew': 'Purifying Brew', 'alchemy.murimblock.recipe.martial_elixir': 'Martial Elixir',
  'alchemy.murimblock.recipe.soul_soothing_pill': 'Soul-Soothing Pill',
  'alchemy.murimblock.effect.healing_decoction': 'Heals over time', 'alchemy.murimblock.effect.qi_decoction': 'Restores a third of your Qi',
  'alchemy.murimblock.effect.purifying_brew': 'Draws off toxicity (once a minute)', 'alchemy.murimblock.effect.martial_elixir': 'Strength for a minute',
  'alchemy.murimblock.effect.soul_soothing_pill': 'Weakens the Heart Demon for long, draws off a little toxicity',
  'alchemy.murimblock.taken.healing_decoction': 'Warmth spreads through your wounds. Toxicity %s',
  'alchemy.murimblock.taken.qi_decoction': 'Your Qi flows back. Toxicity %s', 'alchemy.murimblock.taken.purifying_brew': 'Impurities leave your meridians. Toxicity %s',
  'alchemy.murimblock.taken.martial_elixir': 'Strength surges in your limbs. Toxicity %s', 'alchemy.murimblock.taken.soul_soothing_pill': 'Your heart grows still. Toxicity %s',
  'alchemy.murimblock.learned': 'You learned the recipe: %s', 'alchemy.murimblock.known': 'You already know everything written here',
  'alchemy.murimblock.busy': 'The furnace is at work', 'alchemy.murimblock.water_first': 'Pour spring water in first',
  'alchemy.murimblock.not_now': 'This herb does not go in now', 'alchemy.murimblock.unknown': 'You do not know how to refine this mixture',
  'alchemy.murimblock.ready': 'Ready to refine: %s. Feed the fire.', 'alchemy.murimblock.done': '%s × %s, purity %s%%',
  'alchemy.murimblock.blast': 'The furnace blazes up: everything in it burns to residue!',
  'gui.murimblock.furnace.title': 'PILL FURNACE', 'gui.murimblock.furnace.recipe': 'RECIPE', 'gui.murimblock.furnace.heat': 'HEAT',
  'gui.murimblock.furnace.steps': 'STEPS', 'gui.murimblock.furnace.fire': 'FIRE', 'gui.murimblock.furnace.hot': 'HOT', 'gui.murimblock.furnace.cold': 'COLD',
  'gui.murimblock.furnace.empty': 'Empty', 'gui.murimblock.furnace.mixture': 'A mixture...', 'gui.murimblock.furnace.unknown': 'Unknown mixture',
  'gui.murimblock.furnace.water': 'Spring water', 'gui.murimblock.furnace.role.liquid': 'Liquid', 'gui.murimblock.furnace.role.principal': 'Principal',
  'gui.murimblock.furnace.role.assistant': 'Assistant', 'gui.murimblock.furnace.role.harmonizer': 'Harmonizer',
  'gui.murimblock.furnace.step.prepare': 'Prepare', 'gui.murimblock.furnace.step.extract': 'Extract', 'gui.murimblock.furnace.step.stabilize': 'Stabilize',
  'gui.murimblock.furnace.step.refine': 'Refine', 'gui.murimblock.furnace.held': 'Held %s / %s s',
  'gui.murimblock.furnace.bellows': 'Bellows (hold)', 'gui.murimblock.furnace.charcoal': 'Add charcoal', 'gui.murimblock.furnace.infuse': 'Infuse Qi (hold)',
  'gui.murimblock.furnace.fuel': 'Fuel', 'gui.murimblock.furnace.forecast': 'Purity %s%%  ·  Toxicity %s',
  'gui.murimblock.furnace.toxicity.low': 'low', 'gui.murimblock.furnace.toxicity.medium': 'medium', 'gui.murimblock.furnace.toxicity.high': 'high',
  'gui.murimblock.furnace.add_water': 'Pour spring water in (a water bucket or bottle on the furnace)',
  'gui.murimblock.furnace.add_herbs': 'Add the herbs in the recipe\'s order (use them on the furnace)',
};
const lp = path.join(ASSETS, 'lang', 'en_us.json');
let text = fs.readFileSync(lp, 'utf8');
const have = JSON.parse(text);
const add = Object.entries(lang).filter(([k]) => !(k in have));
if (add.length) text = text.replace(/\n}\s*$/, ',\n' + add.map(([k, v]) => `  ${JSON.stringify(k)}: ${JSON.stringify(v)}`).join(',\n') + '\n}\n');
JSON.parse(text);
fs.writeFileSync(lp, text);
console.log(`alchemy: ${Object.keys(ICONS).length} icons, ${add.length} names`);
