// What each plant is grown from (docs/cultivation/plants.md, Enzo 18/10/2026: "and their seeds with them"): a pouch of
// seeds dyed the flower's colour for most; a bulb (lilies, tulips, the spider lily, the orchid); a cutting (the shrubs);
// a rhizome (bamboo, irises, daylily, ginger lily, water lily, desert candle); a vial of spores (the fern, the lingzhi).
// 16 x 16 icons, textures/item/seed/<plant>_<kind>.png.
// Usage: node scripts/plants/seed-icons.cjs [out dir]
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');
const { ASSETS } = require('./plant-kit.cjs');
const { FLOWERS } = require('./flowers.cjs');

const T = {
  seeds: ['................', '......K..K......', '.......KK.......', '......SSSS......', '.....SPPPPS.....', '....SPPLLPPS....',
    '...SPPLPPPPPS...', '...SPPPPPPPPS...', '...SPPPPPPPDS...', '...SDPPPPPDDS...', '....SDDDDDDS....', '.....SSSSSS.....',
    '..c..........c..', '.c.c.....c..cc..', '..........cc....', '................'],
  bulb: ['................', '.......G........', '.......GP.......', '......BPP.......', '.....BBLPB......', '....BBLBBBB.....',
    '...BBLBBBBBB....', '...BLBBBBBDB....', '...BBBBBBDDB....', '....BBBBDDB.....', '.....BDDDB......', '......r.r.......',
    '.....r.r.r......', '....r..r..r.....', '................', '................'],
  cutting: ['................', '............LL..', '...........LGL..', '..........PGL...', '.........PW.....', '....LL..W.......',
    '...LGGLW........', '....LLW.........', '.....W..........', '....W...........', '...W............', '..W.............',
    '.WC.............', '................', '................', '................'],
  rhizome: ['................', '................', '.......P........', '......GP....P...', '.....RRR...GP...', '..RRRRRRRRRRR...',
    '.RRrRRRrRRRRRR..', '.RRRRRRRRrRRRR..', '..RRrRRRRRRRr...', '...r.r..r..r....', '..r..r.r..r.....', '................',
    '................', '................', '................', '................'],
  spores: ['................', '......KK........', '.....KKKK.......', '......GG........', '.....G..G.......', '....G....G......',
    '....GLLLLG......', '....GSSSSG......', '....GSsSsG......', '....GSSsSG......', '....GsSSSG......', '.....GGGG.......',
    '................', '................', '................', '................'],
};

const shadeHex = (h, k) => '#' + [1, 3, 5].map(i => Math.max(0, Math.min(255, Math.round(parseInt(h.slice(i, i + 2), 16) * k))).toString(16).padStart(2, '0')).join('');

/** Every plant: what it grows from, its colour (the pouch's dye, a bud's tint) and its seeds' colour. */
const KIND = {
  bulb: ['white_lily', 'red_spider_lily', 'tianshan_tulip', 'cymbidium_orchid'],
  cutting: ['camellia', 'gardenia', 'jasmine', 'hibiscus', 'hydrangea', 'sea_buckthorn_berries', 'rhododendron_blossom'],
  rhizome: ['bamboo_shoot', 'desert_iris', 'blue_flag_iris', 'daylily', 'ginger_lily', 'water_lily', 'desert_candle'],
  spores: ['fern_frond', 'lingzhi'],
};
const kindOf = name => Object.keys(KIND).find(k => KIND[k].includes(name)) || 'seeds';
const HERBS = {
  chrysanthemum: ['#f5b324', '#5a4020'], bamboo_shoot: ['#b8c060', '#000'], astragalus_root: ['#e6c84a', '#6a4a20'],
  kunlun_sedge: ['#c4cf8a', '#5a5030'], fern_frond: ['#4e8a32', '#7a5a30'], sea_buckthorn_berries: ['#f08a14', '#000'],
  lotus_seed_pod: ['#e070a0', '#e8dcb0'], cistanche: ['#b06a7a', '#3a2010'], rhododendron_blossom: ['#d8406a', '#000'],
  snow_lotus: ['#e4ead8', '#4a4030'], ginseng_root: ['#d02020', '#c09060'], lingzhi: ['#9a2a12', '#7a3a1a'],
  goji_berries: ['#e0301c', '#e8d090'],
};
const PLANTS = [
  ...Object.entries(HERBS).map(([name, [tint, seed]]) => ({ name, tint, seed })),
  ...FLOWERS.map(f => ({ name: f.name, tint: f.c.petal, seed: f.name === 'udumbara' ? '#fff4c8' : f.name === 'morning_glory' || f.name === 'peony' ? '#1a1410' : '#4a3420' })),
];

function icon(p) {
  const kind = kindOf(p.name), rows = T[kind];
  const pal = {
    // A pouch dyed the flower's colour; white pouches get a darker outline so they read.
    P: p.tint, L: shadeHex(p.tint, 1.18), D: shadeHex(p.tint, 0.72), S: shadeHex(p.tint, 0.38), K: '#c8a060', c: p.seed,
    G: kind === 'spores' ? '#b8c8d0' : '#5a9a3a', B: '#d8b888', r: '#c8b090', W: '#6a4a2a', C: '#e8d8b0', R: '#c09060', s: shadeHex(p.seed, 0.7),
  };
  if (kind === 'bulb') { pal.L = '#f0dcb0'; pal.D = '#a88858'; }
  if (kind === 'cutting') { pal.L = '#7ab84a'; pal.G = '#4e8a32'; }
  if (kind === 'rhizome') { pal.r = '#8a6a40'; }
  if (kind === 'spores') { pal.S = p.seed; pal.L = '#e8f0f4'; pal.K = '#8a6a40'; }
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    if (l === '.') return;
    const c = pal[l];
    img.set([parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16), 255], (y * 16 + x) * 4);
  }));
  return { file: `${p.name}_${kind}`, png: kit.png(16, 16, img), kind };
}

if (require.main === module) {
  const out = process.argv[2] || path.join(ASSETS, 'textures', 'item', 'seed');
  fs.mkdirSync(out, { recursive: true });
  const kinds = {};
  for (const p of PLANTS) { const i = icon(p); fs.writeFileSync(path.join(out, i.file + '.png'), i.png); kinds[i.kind] = (kinds[i.kind] || 0) + 1; }
  console.log('wrote', PLANTS.length, 'icons:', JSON.stringify(kinds));
}
module.exports = { PLANTS, kindOf };
