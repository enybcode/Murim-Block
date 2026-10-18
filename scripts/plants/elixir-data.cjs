// The furnace's elixirs (docs/cultivation/alchemy.md, AlchemyElixirs): their icons (16 x 16 pixel art: pills of their
// colour, decoctions in their flask), the passives' effect icons (18 x 18), the Treatise on Elixirs and the rare recipe
// scroll, their names and what they do, and where the treatise and the scrolls are found (the sites' caches, the
// arena's chests, the bandit camps').
// Usage: node scripts/plants/elixir-data.cjs
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');
const { ASSETS, ROOT } = require('./plant-kit.cjs');

const DATA = path.join(ROOT, 'src', 'main', 'resources', 'data', 'murimblock');
const write = (file, obj) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, Buffer.isBuffer(obj) ? obj : JSON.stringify(obj, null, 2) + '\n'); };

const FLASK = ['................', '.......KK.......', '......KCCK......', '.......SS.......', '......SLLS......', '.....SLPPPS.....', '....SLPPPPPS....',
  '....SLPPPPPS....', '....SPPPPPDS....', '....SPPPPDDS....', '.....SPPDDS.....', '......SSSS......', '................', '................', '................', '................'];
const PILL = ['................', '................', '.....A....A.....', '................', '.......SS.......', '.....SSLLSS.....', '....SLLPPPPS....',
  '....SLPPPPPS....', '....SPPPPPDS....', '....SPPPPDDS....', '.....SPDDDS.....', '......SSSS......', '...A........A...', '................', '................', '................'];
// The scrolls' pills: a pill ringed with a gold band and its aura.
const GREAT = ['................', '...A.......A....', '................', '......SSSS......', '.....SLLPPS.....', '....SLPPPPPS....', '...SLPPPPPPPS...',
  '...GGGGGGGGGG...', '...SPPPPPPPDS...', '....SPPPPPDS....', '.....SPPDDS.....', '......SSSS......', '................', '.A..........A...', '................', '................'];
const FLASK_PAL = (s, l, p, d) => ({ K: '#8a5a30', C: '#c08a50', S: s, L: l, P: p, D: d });
const PILL_PAL = (s, l, p, d, a) => ({ S: s, L: l, P: p, D: d, A: a });

const ELIXIRS = {
  swift_wind_pill: ['Swift Wind Pill', 'Light feet: you run faster', PILL, PILL_PAL('#2a5a3a', '#e8fff0', '#9ae0b0', '#4aa070', '#d8fff0')],
  night_eye_pill: ['Night-Eye Pill', 'Your eyes pierce the dark', PILL, PILL_PAL('#2a2a5a', '#e0e8ff', '#7a8ae8', '#3a4aa0', '#ffe080')],
  fish_breath_pill: ['Fish-Breath Pill', 'You breathe under water', PILL, PILL_PAL('#1a4a5a', '#e0faff', '#6ad0e8', '#2a8aa0', '#a0f0ff')],
  fire_ward_pill: ['Fire-Ward Pill', 'Fire and lava do not burn you', PILL, PILL_PAL('#5a1a0a', '#ffe0b0', '#f08a30', '#b04a10', '#ffd060')],
  feather_fall_pill: ['Feather-Fall Pill', 'You fall like a feather', PILL, PILL_PAL('#4a4a5a', '#ffffff', '#e0e0f0', '#a0a0b8', '#ffffff')],
  iron_skin_pill: ['Iron Skin Pill', 'Your skin hardens like iron', PILL, PILL_PAL('#2a2a2a', '#f0f0f0', '#a8a8a8', '#606060', '#d0d0d0')],
  qi_flow_pill: ['Qi Flow Pill', 'Your Qi flows back faster', PILL, PILL_PAL('#1a3a6a', '#e0f0ff', '#5aa8e8', '#2a6ab0', '#a0d8ff')],
  clear_mind_decoction: ['Clear Mind Decoction', 'Toxicity leaves your meridians', FLASK, FLASK_PAL('#3a6a6a', '#f4fffe', '#c8f0ec', '#6ad0c0')],
  leaping_toad_pill: ['Leaping Toad Pill', 'You leap like a toad', PILL, PILL_PAL('#3a5a1a', '#f0ffd0', '#a8d060', '#6a9a2a', '#e0ff90')],
  stoneheart_pill: ['Stoneheart Pill', 'Blows bite less', PILL, PILL_PAL('#3a3028', '#e8dccc', '#9a8a78', '#5a4a3a', '#c8b8a0')],
  tiger_might_pill: ['Tiger Might Pill', 'Your own blows strike harder', PILL, PILL_PAL('#5a2a0a', '#ffe8c0', '#e8902a', '#9a5010', '#2a1a0a')],
  thorn_body_pill: ['Thorn Body Pill', 'Who strikes you is struck back', PILL, PILL_PAL('#1a3a1a', '#d8f0c8', '#5a9a3a', '#2a5a1a', '#c0e070')],
  blood_vigor_pill: ['Blood Vigor Pill', 'Your blows heal you', PILL, PILL_PAL('#4a0a0a', '#ffb0b0', '#c02a2a', '#7a1010', '#ff6060')],
  keen_senses_pill: ['Keen Senses Pill', 'You see the enemies round you through walls', PILL, PILL_PAL('#5a4a0a', '#fff8d0', '#e8d060', '#a08a20', '#fff0a0')],
  golden_body_pill: ['Golden Body Pill', 'A golden layer of life wraps you', PILL, PILL_PAL('#6a4a0a', '#fff4c0', '#f0c040', '#b08010', '#ffe880')],
  deep_sea_elixir: ['Deep Sea Elixir', 'The sea carries you and lets you see', FLASK, FLASK_PAL('#1a4a6a', '#e8faff', '#9ad8f0', '#2a90c0')],
  miners_vigor_pill: ['Miner\'s Vigor Pill', 'You dig and strike faster', PILL, PILL_PAL('#1a2a5a', '#e0e8ff', '#4a6ad8', '#c03a3a', '#ff7a7a')],
  great_qi_flow_pill: ['Great Qi Flow Pill', 'Your Qi flows back much faster', PILL, PILL_PAL('#1a2a5a', '#ffffff', '#7ac0ff', '#3a70d0', '#ffe080')],
  fortune_pill: ['Fortune Pill', 'Luck smiles on you; villagers honour you', PILL, PILL_PAL('#1a4a2a', '#e8ffe8', '#3ac060', '#f0c040', '#ffe880')],
  shadow_step_pill: ['Shadow Step Pill', 'You walk unseen and swift', PILL, PILL_PAL('#1a1028', '#c8b8e0', '#4a3a6a', '#20182e', '#8a6ab0')],
  diamond_body_pill: ['Diamond Body Pill', 'A body like diamond: little hurts you', GREAT, { S: '#1a4a5a', L: '#ffffff', P: '#8af0f0', D: '#3ab0c0', A: '#c0ffff', G: '#f0c040' }],
  phoenix_rebirth_pill: ['Phoenix Rebirth Pill', 'Flames mend you and do not burn you', GREAT, { S: '#5a1a0a', L: '#fff0a0', P: '#ff7a2a', D: '#c03a10', A: '#ffd060', G: '#f0c040' }],
  heavenly_tiger_pill: ['Heavenly Tiger Pill', 'The might of a heavenly tiger', GREAT, { S: '#4a1a0a', L: '#fff0d0', P: '#f0a030', D: '#2a1a0a', A: '#ffe080', G: '#f0c040' }],
  dragon_breath_elixir: ['Dragon Breath Elixir', 'Your Qi pours back, your mind clears', FLASK, FLASK_PAL('#4a1a5a', '#fff0ff', '#e0a8f0', '#a050c0')],
};

const SCROLL = ['................', '................', '..WW.........WW.', '..WRWWWWWWWWWRW.', '..WRPPPPPPPPPRW.', '...WPDPPDPDPPW..', '...WPPPPPPPPPW..',
  '...WPDPDPPDPPW..', '...WPPPPPPPPPW..', '...WPPPDPPDPPW..', '...WPPPPPPPGGW..', '..WRPPPPPPPGGRW.', '..WRWWWWWWWWWRW.', '..WW.........WW.', '................', '................'];
const BOOK = ['................', '..KKKKKKKKKKKK..', '..KRRRRRRRRRRPK.', '..KRRRRRRRRRRPK.', '..KRRRRYYRRRRPK.', '..KRRRYGGYRRRPK.', '..KRRYGGGGYRRPK.',
  '..KRRYGGGGYRRPK.', '..KRRRYGGYRRRPK.', '..KRRRRDDRRRRPK.', '..KRRRYRRYRRRPK.', '..KRRRRRRRRRRPK.', '..KDDDDDDDDDDPK.', '..KKKKKKKKKKKKD.', '................', '................'];
const OTHER = {
  elixir_treatise: { rows: BOOK, pal: { K: '#0a1a2a', R: '#1a4a7a', D: '#0a2a4a', Y: '#e8c060', G: '#5ab0e0', P: '#f0e4c8' } },
  recipe_scroll: { rows: SCROLL, pal: { W: '#6a4a2a', R: '#d0a030', P: '#efe2c0', D: '#3a2a20', G: '#e8b020' } },
};

// The passives' icons, 18 x 18 (a 16 x 16 drawing with a pixel of margin).
const EFFECTS = {
  qi_flow: ['Qi Flow', ['................', '.....BBBBB......', '...BBLLLLLBB....', '..BLL.....LLB...', '.BL...BBB...LB..', '.BL..BLLLB..LB..', 'BL..BL...LB..LB.',
    'BL..BL.B..LB.LB.', 'BL..BL.LB.LB.LB.', 'BL...BL...LB..B.', '.BL...BBBBL..LB.', '.BL.........LB..', '..BLL.....LLB...', '...BBLLLLLBB....', '.....BBBBB......', '................'], { B: '#2a6ab0', L: '#9ad8ff' }],
  iron_skin: ['Iron Skin', ['................', '..GGGGGGGGGGGG..', '..GLLLLLLLLLLG..', '..GLWWLLLLLLDG..', '..GLWLLLLLLLDG..', '..GLLLLLLLLLDG..', '..GLLLLLLLLLDG..',
    '..GLLLLLLLLLDG..', '...GLLLLLLLDG...', '...GLLLLLLLDG...', '....GLLLLLDG....', '.....GLLLDG.....', '......GLDG......', '.......GG.......', '................', '................'], { G: '#3a3a3a', L: '#a8a8a8', W: '#ffffff', D: '#6a6a6a' }],
  tiger_might: ['Tiger Might', ['................', '..O....O....O...', '..OO...OO...OO..', '...OO...OO...OO.', '...OY...OY...OY.', '....OY...OY...OY', '....OY...OY...OY',
    '....OY...OY...OY', '...OY...OY...OY.', '...OY...OY...OY.', '..OY...OY...OY..', '..OY...OY...OY..', '.OY...OY...OY...', '.O....O....O....', '................', '................'], { O: '#c05a10', Y: '#ffc060' }],
  thorn_body: ['Thorn Body', ['................', '.......G........', '...G...G...G....', '....G.GGG.G.....', '.....GGLGG......', '.G..GGLLLGG..G..', '..GGGLLLLLGGG...',
    'GGGLLLLLLLLLGGG.', '..GGGLLLLLGGG...', '.G..GGLLLGG..G..', '.....GGLGG......', '....G.GGG.G.....', '...G...G...G....', '.......G........', '................', '................'], { G: '#2a6a1a', L: '#8ad060' }],
  blood_vigor: ['Blood Vigor', ['................', '.......R........', '.......R........', '......RRR.......', '......RRR.......', '.....RRLRR......', '.....RLLRR......',
    '....RRLRRRR.....', '....RLRRRRR.....', '...RRRRRRRDR....', '...RRRRRRRDR....', '...RRRRRRDDR....', '....RRRRDDR.....', '.....RRRRR......', '................', '................'], { R: '#b01a1a', L: '#ff8a8a', D: '#6a0a0a' }],
  keen_senses: ['Keen Senses', ['................', '................', '................', '.....YYYYYY.....', '...YY......YY...', '..Y...DDDD...Y..', '.Y...DDKKDD...Y.',
    'Y...DDKWKKDD...Y', '.Y...DDKKDD...Y.', '..Y...DDDD...Y..', '...YY......YY...', '.....YYYYYY.....', '................', '................', '................', '................'], { Y: '#e8c040', D: '#a07a10', K: '#2a1a0a', W: '#ffffff' }],
  clear_mind: ['Clear Mind', ['................', '.......C........', '......CLC.......', '......CLC.......', '..C...CLC...C...', '..CC..CLC..CC...', '..CLC.CLC.CLC...',
    '...CLCCLCCLC....', '...CLLCLCLLC....', '....CLLLLLC.....', '.CCCCLLLLLCCCC..', '..CLLLLLLLLLC...', '...CCCCCCCCC....', '................', '................', '................'], { C: '#3a9a8a', L: '#c8fff0' }],
};

function png(rows, pal, size) {
  rows.forEach((r, i) => { if (r.length !== 16) throw new Error(`row ${i} is ${r.length} wide: ${r}`); });
  const img = new Uint8Array(size * size * 4), off = (size - 16) / 2;
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    if (l === '.') return;
    const c = pal[l];
    if (!c) throw new Error('no colour ' + l);
    img.set([parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16), 255], ((y + off) * size + x + off) * 4);
  }));
  return kit.png(size, size, img);
}

const itemIcon = (name, rows, pal) => {
  write(path.join(ASSETS, 'textures', 'item', 'alchemy', name + '.png'), png(rows, pal, 16));
  write(path.join(ASSETS, 'models', 'item', name + '.json'), { parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/alchemy/${name}` } });
};
for (const [id, [, , rows, pal]] of Object.entries(ELIXIRS)) itemIcon(id, rows, pal);
for (const [id, { rows, pal }] of Object.entries(OTHER)) itemIcon(id, rows, pal);
for (const [id, [, rows, pal]] of Object.entries(EFFECTS)) write(path.join(ASSETS, 'textures', 'mob_effect', id + '.png'), png(rows, pal, 18));

// The treatise and the rare scrolls in the chests: each scroll with its recipe on it.
const SCROLL_RECIPES = ['diamond_body_pill', 'phoenix_rebirth_pill', 'heavenly_tiger_pill', 'dragon_breath_elixir'];
for (const [chest, weight] of [['murim_cache', 2], ['alliance_arena', 1], ['bandit_camp', 1]]) {
  const file = path.join(DATA, 'loot_table', 'chests', chest + '.json');
  if (!fs.existsSync(file)) continue;
  const table = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (JSON.stringify(table).includes('elixir_treatise')) continue;
  table.pools.push({ rolls: 1, entries: [{ type: 'minecraft:empty', weight: 30 - weight * 5 },
    { type: 'minecraft:item', name: 'murimblock:elixir_treatise', weight: weight * 3 },
    ...SCROLL_RECIPES.map(r => ({ type: 'minecraft:item', name: 'murimblock:recipe_scroll', weight,
      functions: [{ function: 'minecraft:set_custom_data', tag: `{recipe:"${r}"}` }] }))] });
  write(file, table);
}

const lang = {
  'item.murimblock.elixir_treatise': 'Treatise on Elixirs', 'item.murimblock.recipe_scroll': 'Recipe Scroll',
  'item.murimblock.recipe_scroll.of': 'Recipe Scroll: %s',
  'alchemy.murimblock.elixir_taken': '%s taken. Toxicity %s', 'alchemy.murimblock.toxicity': 'Toxicity +%s',
  'alchemy.murimblock.blank': 'This scroll is blank', 'alchemy.murimblock.scroll_hint': 'Read it to learn its recipe (the scroll is used up)',
};
for (const [id, [name, passive]] of Object.entries(ELIXIRS)) {
  lang[`item.murimblock.${id}`] = name;
  lang[`alchemy.murimblock.recipe.${id}`] = name;
  lang[`alchemy.murimblock.passive.${id}`] = passive;
}
for (const [id, [name]] of Object.entries(EFFECTS)) lang[`effect.murimblock.${id}`] = name;

const lp = path.join(ASSETS, 'lang', 'en_us.json');
let text = fs.readFileSync(lp, 'utf8');
const have = JSON.parse(text);
const add = Object.entries(lang).filter(([k]) => !(k in have));
if (add.length) text = text.replace(/\n}\s*$/, ',\n' + add.map(([k, v]) => `  ${JSON.stringify(k)}: ${JSON.stringify(v)}`).join(',\n') + '\n}\n');
JSON.parse(text);
fs.writeFileSync(lp, text);
console.log(`elixirs: ${Object.keys(ELIXIRS).length} elixirs, ${Object.keys(EFFECTS).length} effects, ${add.length} names`);
