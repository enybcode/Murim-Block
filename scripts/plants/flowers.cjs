// Thirty flowers of the Murim (docs/cultivation/plants.md, Enzo 18/10/2026: "thirty more flowers"), six a region, in
// the herbs' style (herb-models.cjs): voxels at 16 texels a block, moving by rarity. Built from shapes a flower can take
// (a cup, a star, a trumpet, bells, a spike, a globe of florets, a daisy, a spider lily, an orchid, a flowering shrub),
// each flower its shape, its size, its colours and its leaves. One stage (in bloom) for now.
// Usage: node scripts/plants/flowers.cjs [flower...]   (and their icons: textures/item/flower)
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');
const { make, shade, flat, GREEN, GREEN_D, GREEN_L } = require('./herb-models.cjs');
const { ASSETS } = require('./plant-kit.cjs');

// ---------------------------------------------------------------- the shapes

/** The bone a flower head goes on: its own for the rare (it breathes) and the very rare (it glows). */
function headBone(ctx, f, at) {
  if (f.rarity === 'rare') ctx.group('bloom', at);
  if (f.rarity === 'very_rare') ctx.group('glow', at);
}
const petal = f => (p, n, c, t) => shade((t.u + t.v) % 3 === 0 ? f.c.light : f.c.petal, n);
const tipLight = (f, y) => (p, n) => shade(p[1] > y ? f.c.light : f.c.petal, n);

/** A stem from (x, 0, z), `h` high, leaning `deg` about `axis`; two small leaves a third of the way up. Returns its top. */
function stem(ctx, x, z, h, axis = 'z', deg = 0, leaves = true) {
  const top = ctx.stick([x, 0, z], h, flat(GREEN), axis, deg);
  if (leaves) {
    const at = [x + (top[0] - x) * 0.35, h * 0.35, z + (top[2] - z) * 0.35];
    ctx.box([3, 0.5, 1], at, flat(GREEN_L));
    if (h > 6) ctx.box([1, 0.5, 3], [at[0], h * 0.6, at[2] + (top[2] - z) * 0.25], flat(GREEN));
  }
  return top;
}
/** Long strap leaves from the foot, arching out (irises, daylilies, orchids). */
function straps(ctx, x, z, list, col = GREEN) {
  for (const [axis, deg, len] of list) ctx.stick([x, 0, z], len, (p, n) => shade(p[1] > len * 0.6 ? GREEN_L : col, n), axis, deg);
}

const SHAPES = {
  /** A cup of four petals round a heart (tulip, poppy, peony when big). */
  cup(ctx, f, at, big = f.big) {
    headBone(ctx, f, at);
    const s = big ? 1.5 : 1, [x, y, z] = at;
    ctx.box([3 * s, 1, 3 * s], [x, y + 0.5, z], flat(f.c.deep));
    for (const [dx, dz, axis, deg] of [[0, -1, 'x', -22.5], [0, 1, 'x', 22.5], [1, 0, 'z', -22.5], [-1, 0, 'z', 22.5]])
      ctx.box(dx ? [1, 3 * s, 3 * s] : [3 * s, 3 * s, 1], [x + dx * 1.8 * s, y + 1.8 * s, z + dz * 1.8 * s], tipLight(f, y + 2.6 * s), axis, deg);
    if (big) for (const r of [0, 45]) ctx.box([2.6, 2, 0.8], [x, y + 2.2, z], flat(f.c.light), 'y', r === 0 ? 22.5 : -22.5);
    ctx.box([1, 1, 1], [x, y + 1.5 * s, z], flat(f.c.centre));
  },
  /** A star of petals lying open, its heart raised (balloon flower, pinks, saxifrage, caper). */
  star(ctx, f, at) {
    headBone(ctx, f, at);
    const [x, y, z] = at, r = f.big ? 2.5 : 1.8;
    for (const deg of [0, 45]) for (const [sx, sz] of [[2 * r, 1], [1, 2 * r]]) ctx.box([sx, 0.5, sz], [x, y + 0.5, z], petal(f), 'y', deg);
    ctx.box([1, 1, 1], [x, y + 1, z], flat(f.c.centre));
    if (f.stamens) for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) ctx.box([0.5, 2, 0.5], [x + dx * 0.8, y + 2, z + dz * 0.8], flat(f.stamens));
  },
  /** A trumpet: a tube, then four petals flaring out and up, stamens out of it (lilies, gentian, morning glory, daylily). */
  trumpet(ctx, f, at) {
    headBone(ctx, f, at);
    const [x, y, z] = at, s = f.big ? 1.4 : 1;
    ctx.box([1, 2, 1], [x, y + 1, z], flat(f.c.deep));
    for (const [dx, dz, axis, deg] of [[0, -1, 'x', -45], [0, 1, 'x', 45], [1, 0, 'z', -45], [-1, 0, 'z', 45]])
      ctx.box(dx ? [1, 3 * s, 2 * s] : [2 * s, 3 * s, 1], [x + dx * 1.3 * s, y + 3 * s, z + dz * 1.3 * s], tipLight(f, y + 3.5 * s), axis, deg);
    ctx.box([0.5, 2, 0.5], [x, y + 3, z], flat(f.c.centre));
  },
  /** Bells hanging along an arching stem (impatiens, ladybells, lily of the valley). */
  bells(ctx, f, at, from, count = 4) {
    for (let k = 1; k <= count; k++) {
      const t = k / (count + 0.5), p = [from[0] + (at[0] - from[0]) * t, from[1] + (at[1] - from[1]) * t, from[2] + (at[2] - from[2]) * t];
      if (k === 1) headBone(ctx, f, p);
      ctx.box([1, 2, 1], [p[0], p[1] - 1.5, p[2]], flat(f.c.petal));
      ctx.box([2, 0.5, 2], [p[0], p[1] - 2.6, p[2]], flat(f.c.light));
    }
  },
  /** A spike of florets up a tall stem, wide below, narrow at the top (hollyhock, desert candle, ginger lily). */
  spike(ctx, f, x, z, h) {
    const top = stem(ctx, x, z, h, 'z', 0, true);
    headBone(ctx, f, [x, h * 0.4, z]);
    for (let y = Math.round(h * 0.4); y < h + 2; y++) {
      const w = y > h ? 1 : y > h * 0.75 ? 2 : 3;
      ctx.box([w, 1, w], [top[0], y + 0.5, top[2]], (p, n, c, t) => shade((y + t.u) % 2 ? f.c.petal : f.c.light, n));
    }
  },
  /** A round head of tiny florets (hydrangea, forget-me-not, globe flowers). */
  globe(ctx, f, at, r = f.big ? 3 : 2) {
    headBone(ctx, f, at);
    const [x, y, z] = at;
    const layers = r >= 3 ? [[0.5, 1.5], [1.5, 2.5], [2.5, 3], [3.5, 3], [4.5, 2.5], [5.5, 1.5]] : [[0.5, 1], [1.5, 2], [2.5, 2], [3.5, 1]];
    for (const [h, w] of layers) {
      const paint = (p, n, c, t) => shade((t.u * 2 + t.v * 3 + Math.round(h)) % 3 === 0 ? f.c.light : (t.u + t.v) % 4 === 0 ? f.c.deep : f.c.petal, n);
      ctx.box([w * 2, 1, w * 2 - (w >= 2.5 ? 2 : 0)], [x, y + h, z], paint);
      if (w >= 2.5) ctx.box([w * 2 - 2, 1, w * 2], [x, y + h, z], paint);
    }
  },
  /** A flat daisy: a cross of petals, its heart raised (edelweiss, asters). */
  daisy(ctx, f, at) {
    headBone(ctx, f, at);
    const [x, y, z] = at;
    ctx.box([5, 0.5, 3], [x, y + 0.5, z], petal(f));
    ctx.box([3, 0.5, 5], [x, y + 0.5, z], petal(f));
    ctx.box([2, 1, 2], [x, y + 1, z], flat(f.c.centre));
  },
  /** The red spider lily: bare stem, a crown of long curled filaments flying out and up. */
  spider(ctx, f, at) {
    headBone(ctx, f, at);
    const [x, y, z] = at;
    for (const [dx, dz, axis, deg] of [[0, -1, 'x', -45], [0, 1, 'x', 45], [1, 0, 'z', -45], [-1, 0, 'z', 45]]) {
      ctx.box(dx ? [1, 3, 0.5] : [0.5, 3, 1], [x + dx * 1.2, y + 1.6, z + dz * 1.2], flat(f.c.petal), axis, deg);
      ctx.box(dx ? [1, 3, 0.5] : [0.5, 3, 1], [x + dx * 2.6, y + 3.6, z + dz * 2.6], flat(f.c.light), axis, deg / 2);
    }
    for (const r of [22.5, -22.5]) ctx.box([5, 0.5, 0.5], [x, y + 1.2, z], flat(f.c.deep), 'y', r);
    ctx.box([1, 1, 1], [x, y + 1, z], flat(f.c.centre));
  },
  /** An orchid: long arching leaves, a spike leaning out with three flowers along it. */
  orchid(ctx, f) {
    straps(ctx, 0, 0, [['z', 45, 10], ['z', -45, 9], ['x', 45, 9], ['x', -45, 10], ['z', 22.5, 11]], GREEN_D);
    const top = ctx.stick([0, 0, 0], 11, flat(GREEN), 'x', -22.5);
    for (let k = 0; k < 3; k++) {
      const t = 0.55 + k * 0.2, p = [top[0] * t, top[1] * t, top[2] * t];
      if (k === 0) headBone(ctx, f, p);
      ctx.box([2, 2, 1], p, flat(f.c.petal));
      ctx.box([1, 1, 2], [p[0] + 1, p[1], p[2]], flat(f.c.light));
      ctx.box([1, 1, 2], [p[0] - 1, p[1], p[2]], flat(f.c.light));
      ctx.box([1, 1, 1], [p[0], p[1] - 1, p[2] - 0.6], flat(f.c.centre));
    }
  },
  /** A flowering shrub: branches with dark leaves and blooms at their tips (camellia, gardenia, jasmine). */
  shrub(ctx, f) {
    for (const [axis, deg, len] of [['z', 22.5, 9], ['z', -22.5, 8], ['x', 22.5, 9], ['x', -22.5, 8], ['z', 0, 10]]) {
      const top = ctx.stick([0, 0, 0], len, flat('#5a4026'), axis, deg);
      ctx.box([3, 0.6, 1], [top[0], top[1] - 1.5, top[2]], flat(GREEN_D));
      ctx.box([1, 0.6, 3], [top[0], top[1] - 2.5, top[2]], flat('#3f6b2a'));
      headBone(ctx, f, top);
      if (f.small) for (const [dx, dz] of [[0, 0], [1, 1], [-1, 1], [1, -1]]) ctx.box([1, 1, 1], [top[0] + dx, top[1] + (dx + dz) * 0.3, top[2] + dz], flat(f.c.petal));
      else {
        ctx.box([3, 1, 3], [top[0], top[1] + 0.5, top[2]], petal(f));
        ctx.box([2, 1, 2], [top[0], top[1] + 1.5, top[2]], flat(f.c.light));
        ctx.box([1, 1, 1], [top[0], top[1] + 2, top[2]], flat(f.c.centre));
      }
    }
  },
};

// ---------------------------------------------------------------- the thirty

const F = (name, fr, region, rarity, shape, c, opts = {}) => ({ name, fr, region, rarity, shape, c, ...opts });
const C = (petal, light, deep, centre) => ({ petal, light, deep, centre });
const FLOWERS = [
  // The centre: sacred mountains and valleys.
  F('peony', 'Pivoine (mudan)', 'Centre', 'uncommon', 'cup', C('#e0508a', '#ff9ac4', '#9a2058', '#f2c84a'), { big: true, stems: 2 }),
  F('cymbidium_orchid', 'Orchidée cymbidium', 'Centre', 'rare', 'orchid', C('#d8e6a0', '#f4f8d8', '#9aa860', '#b02a3a')),
  F('camellia', 'Camélia', 'Centre', 'uncommon', 'shrub', C('#c8202e', '#f05060', '#801018', '#f2d040')),
  F('balloon_flower', 'Platycodon (jiegeng)', 'Centre', 'common', 'star', C('#6a5ad8', '#9a8af0', '#3a2a90', '#e8e0ff'), { stems: 3 }),
  F('red_spider_lily', 'Lys araignée rouge', 'Centre', 'uncommon', 'spider', C('#d8202a', '#ff5a50', '#901018', '#f8c040'), { stems: 3, bare: true }),
  F('white_lily', 'Lys blanc (baihe)', 'Centre', 'uncommon', 'trumpet', C('#f4f2e8', '#ffffff', '#c8d0a0', '#d88a20'), { big: true, stems: 2 }),
  // The north: the snows.
  F('blue_poppy', 'Pavot bleu de l\'Himalaya', 'Nord', 'rare', 'cup', C('#5aa8e8', '#a8d8ff', '#2a68b0', '#f2d040'), { stems: 2 }),
  F('edelweiss', 'Edelweiss', 'Nord', 'uncommon', 'daisy', C('#eef0e4', '#ffffff', '#b8c0a8', '#d8c050'), { stems: 3 }),
  F('gentian', 'Gentiane', 'Nord', 'common', 'trumpet', C('#2a48c0', '#5a80f0', '#18287a', '#e8e8ff'), { stems: 3, short: true }),
  F('alpine_forget_me_not', 'Myosotis des cimes', 'Nord', 'common', 'globe', C('#68a8f0', '#a8d0ff', '#3a6ac0', '#f2e050'), { stems: 3, short: true }),
  F('snow_saxifrage', 'Saxifrage des neiges', 'Nord', 'common', 'star', C('#f4dce8', '#ffffff', '#c890b0', '#f2c840'), { stems: 4, short: true }),
  F('tianshan_tulip', 'Tulipe du Tianshan', 'Nord', 'uncommon', 'cup', C('#e83a2a', '#ffd040', '#a01a10', '#3a2010'), { stems: 3 }),
  // The west: the dry lands.
  F('desert_candle', 'Chandelle du désert', 'Ouest', 'rare', 'spike', C('#f0905a', '#ffc890', '#c05a30', '#f2d040'), { tall: true }),
  F('desert_iris', 'Iris des sables', 'Ouest', 'uncommon', 'trumpet', C('#8a5ac8', '#c8a8f0', '#4a2a88', '#f2d040'), { straps: true, stems: 2 }),
  F('chinese_pink', 'Œillet de Chine (shizhu)', 'Ouest', 'common', 'star', C('#e8508a', '#ffa8c8', '#a02858', '#ffffff'), { stems: 4 }),
  F('field_poppy', 'Coquelicot', 'Ouest', 'common', 'cup', C('#e02a1a', '#ff6a50', '#900a08', '#201010'), { stems: 4 }),
  F('caper_flower', 'Fleur de câprier', 'Ouest', 'uncommon', 'star', C('#f8f4f0', '#ffffff', '#d8c8c0', '#a0d040'), { stems: 2, big: true, stamens: '#e050a0' }),
  F('harmal', 'Harmale', 'Ouest', 'common', 'star', C('#f4f0d8', '#ffffff', '#c8c0a0', '#e8c040'), { stems: 4, short: true }),
  // The south: the humid lands and plains.
  F('hydrangea', 'Hortensia (xiuqiu)', 'Sud', 'uncommon', 'globe', C('#7a90e0', '#c0c8ff', '#4a5aa8', '#e8e0ff'), { big: true, stems: 2 }),
  F('gardenia', 'Gardénia (zhizi)', 'Sud', 'uncommon', 'shrub', C('#f8f6ec', '#ffffff', '#d8d0b0', '#f2d870')),
  F('hibiscus', 'Hibiscus (mujin)', 'Sud', 'common', 'trumpet', C('#e868a8', '#ffb0d8', '#a02a68', '#f8f0a0'), { big: true, stems: 2 }),
  F('garden_balsam', 'Balsamine (fengxian)', 'Sud', 'common', 'bells', C('#e8406a', '#ff90b0', '#a01a40', '#f8e060'), { stems: 2 }),
  F('ginger_lily', 'Hédychie (lys gingembre)', 'Sud', 'uncommon', 'spike', C('#f8f4e4', '#ffffff', '#d8d0b0', '#f2c840')),
  F('blue_flag_iris', 'Iris des marais (yuanwei)', 'Sud', 'common', 'trumpet', C('#5a4ac8', '#9a90f0', '#2a2088', '#f2d040'), { straps: true, stems: 2 }),
  // The east: the water country.
  F('water_lily', 'Nénuphar (shuilian)', 'Est', 'uncommon', 'waterlily', C('#f0a0c0', '#ffe0ec', '#c0507a', '#f2d040')),
  F('morning_glory', 'Volubilis (qianniu)', 'Est', 'common', 'vine', C('#4a60e0', '#a8b8ff', '#2a2a90', '#ffffff')),
  F('daylily', 'Hémérocalle (xuancao)', 'Est', 'common', 'trumpet', C('#f08a1a', '#ffc050', '#b04a08', '#f8e080'), { straps: true, stems: 2, big: true }),
  F('jasmine', 'Jasmin (moli)', 'Est', 'common', 'shrub', C('#fafaf2', '#ffffff', '#d8d8c8', '#f2e070'), { small: true }),
  F('ladybells', 'Adénophore (shashen)', 'Est', 'uncommon', 'bells', C('#8a90e8', '#c0c8ff', '#4a50a8', '#f8f0ff'), { stems: 2 }),
  // The legend: the udumbara blooms once in three thousand years.
  F('udumbara', 'Udumbara (youtan)', 'Partout, très rarement', 'very_rare', 'udumbara', C('#ffffff', '#fffbe8', '#d8e8f0', '#fff4c8')),
];

/** Builds a flower: its stems (2 to 4, spread over the block, heights varied) each with its shape. */
function build(f) {
  return ctx => {
    if (f.shape === 'orchid' || f.shape === 'shrub') return SHAPES[f.shape](ctx, f);
    if (f.shape === 'waterlily') {
      ctx.group('float', [0, 0, 0]);
      ctx.box([10, 0.5, 9], [0, 0.25, 0], (p, n, c, t) => shade((t.u * 3 + t.v * 5) % 4 === 0 ? '#3a7028' : '#4a8a32', n));
      ctx.box([6, 0.5, 5], [-4, 0.35, 4], flat('#5a9a3a'));
      return SHAPES.cup(ctx, f, [1, 0.5, 0], false);
    }
    if (f.shape === 'vine') {
      // A bamboo stake, the vine twining up it, trumpets out of it.
      ctx.stick([0, 0, 0], 13, flat('#c8b070'), 'z', 0);
      for (let y = 1; y < 13; y++) ctx.box([1, 1, 1], [[1, 0, -1, 0][y % 4], y + 0.5, [0, 1, 0, -1][y % 4]], flat(y % 3 ? GREEN : GREEN_L));
      for (const [y, dx, dz] of [[4, 1, 0], [8, -1, 0], [11, 0, 1]]) {
        ctx.box([3, 0.5, 1], [dx * 2, y - 1, dz * 2], flat(GREEN_L));
        SHAPES.trumpet(ctx, f, [dx * 2, y, dz * 2]);
        ctx.on('body');
      }
      return;
    }
    if (f.shape === 'udumbara') {
      // A mossy stone, threads of the finest stalks out of it, a tiny white flower at each tip.
      ctx.box([7, 3, 6], [0, 1.5, 0], (p, n, c, t) => (n[1] > 0 && (t.u + t.v) % 3 ? '#5a8a3a' : shade('#7a7a72', n)));
      ctx.group('glow', [0, 3, 0]);
      for (const [x, z, h] of [[-2, -1, 4], [-1, 1, 5], [0, -2, 3], [1, 0, 6], [2, 2, 4], [2, -1, 5], [-2, 2, 3], [0, 1, 4]]) {
        ctx.box([0.5, h, 0.5], [x, 3 + h / 2, z], flat('#e8f0e0'));
        ctx.box([1, 1, 1], [x, 3 + h + 0.5, z], flat('#ffffff'));
      }
      return;
    }
    const n = f.stems || 1, spots = [[0, 0], [3, 2], [-3, 2.5], [2, -3], [-2.5, -2.5]];
    for (let i = 0; i < n; i++) {
      const [x, z] = spots[i], h = (f.short ? 5 : f.tall ? 12 : 8) - i * (f.short ? 1 : 1.5);
      if (f.straps && i === 0) straps(ctx, x, z, [['z', 22.5, 9], ['z', -22.5, 8], ['x', 22.5, 9], ['x', -45, 7]]);
      if (f.shape === 'spike') { SHAPES.spike(ctx, f, x, z, h - (i ? 2 : 0)); ctx.on('body'); continue; }
      const [axis, deg] = [['z', 0], ['z', 22.5], ['x', -22.5], ['x', 22.5], ['z', -22.5]][i];
      const top = stem(ctx, x, z, h, axis, deg, !f.bare);
      if (f.shape === 'bells') {
        const arch = ctx.stick(top, 5, flat(GREEN), i % 2 ? 'x' : 'z', 45 * (i % 2 ? 1 : -1));
        SHAPES.bells(ctx, f, arch, top);
      } else SHAPES[f.shape](ctx, f, top);
      ctx.on('body');
    }
  };
}

// ---------------------------------------------------------------- the icons: the bloom as the shapes draw it, 16 x 16

const ICONS = {
  cup: ['................', '................', '.....P.PP.P.....', '....PLPLLPLP....', '....PPLCCLPP....', '....DPPCCPPD....', '.....DPPPPD.....', '......DDDD......', '.......G........', '.......G..LL....', '....LL.G.LL.....', '.....LLG........', '.......G........', '.......G........', '.......G........', '................'],
  star: ['................', '.......L........', '......LPL.......', '..L...PPP...L...', '..PLP.PPP.PLP...', '...PPPPCPPPP....', '....PPCCCPP.....', '..LPPPPCPPPPL...', '...PP.PPP.PP....', '.....LPPPL......', '.......PG.......', '.......G..LL....', '....LL.G.LL.....', '.....LLG........', '.......G........', '.......G........'],
  trumpet: ['................', '..L.........L...', '..PL.......LP...', '...PL..C..LP....', '...PPL.C.LPP....', '....PPLCLPP.....', '.....PPCPP......', '......PDP.......', '......DDD.......', '.......G........', '.......G..LL....', '....LL.G.LL.....', '.....LLG........', '.......G........', '.......G........', '................'],
  bells: ['................', '......GGGG......', '.....G....G.....', '....G......G....', '...G...P....G...', '...G..PPP...P...', '..PP..LLL..PPP..', '.PPP........LLL.', '.LLL............', '.......G........', '.......G........', '....LL.G........', '.....LLG.LL.....', '.......GLL......', '.......G........', '................'],
  spike: ['.......L........', '......LPL.......', '......PLP.......', '.....LPPPL......', '.....PPLPP......', '....LPPPPPL.....', '....PPPLPPP.....', '....LPPPPPL.....', '....PPPPLPP.....', '.....DPPPD......', '.......G........', '....LL.G.LL.....', '.....LLGLL......', '.......G........', '.......G........', '................'],
  globe: ['................', '.....PLPLP......', '....LPPDPPL.....', '...PPLPPLPPP....', '...LPPDPLPDL....', '...PDPPLPPPP....', '...PPLPPDPLP....', '....LPPPLPP.....', '.....PDPPD......', '.......G........', '.......G..LL....', '....LL.G.LL.....', '.....LLG........', '.......G........', '.......G........', '................'],
  daisy: ['................', '................', '.......P........', '......PLP.......', '..PP..PLP..PP...', '..PLPPCCCPPLP...', '...PPCCCCCPP....', '..PLPPCCCPPLP...', '..PP..PLP..PP...', '......PLP.......', '.......P........', '.......G..LL....', '....LL.G.LL.....', '.....LLG........', '.......G........', '................'],
  spider: ['..L.........L...', '...P.......P....', '..L.P..L..P.L...', '....PP.P.PP.....', '..L..PPPPP..L...', '...PPPPCPPPP....', '.......C........', '.....P.D.P......', '....P..G..P.....', '.......G........', '.......G........', '.......G........', '.......G........', '.......G........', '.......G........', '................'],
  orchid: ['................', '.........PLP....', '........LPCPL...', '.........PDP....', '......PLP.G.....', '.....LPCPLG.....', '......PDPG......', '.LL......G...LL.', '..LL....G...LL..', '...LL...G..LL...', '....LL..G.LL....', '.....LL.GLL.....', '......LLGL......', '.......LG.......', '.......GG.......', '................'],
  shrub: ['................', '...PLP....PLP...', '..PLCLP..PLCLP..', '...PLP.LL.PLP...', '....LLPLPLL.....', '...LLPLCLPLL....', '..LL.LPLPL.LL...', '...LLLLLLLLL....', '....LL.W.LL.....', '.......W........', '......W.W.......', '.....W...W......', '.......W........', '................', '................', '................'],
  waterlily: ['................', '................', '................', '.....P..P.......', '....PLPPLP......', '...PLPCCPLP.....', '....PPCCPP......', '..GGGPPPPGGGG...', '.GLLGGGGGGLLGG..', '.GGLLGGGGLLGGG..', '..GGGLLLLGGGG...', '....GGGGGG......', '................', '................', '................', '................'],
  vine: ['.......W........', '......LWG...L...', '..P...GWL..LP...', '..PL...WG.LPP...', '..PCL..WL..C....', '...P..GWG.......', '......LW.L......', '...L...WG.P.....', '..LPL..WLPC.....', '...C..GW..P.....', '......LWG.......', '.......WL.......', '......GW........', '.......W........', '.......W........', '................'],
  udumbara: ['................', '................', '...P...P..P.....', '...|.P.|..|.P...', '...|.|.|..|.|...', '.P.|.|.|.P|.|...', '.|.|.|.|.|..|...', '.|..|..|.|.|....', '..SSSSSSSSSSS...', '.SSMSSSMSSSMSS..', '.SSSSSMSSSSSSS..', '..SSSSSSSSSSS...', '................', '................', '................', '................'],
};

function icon(f) {
  const rows = ICONS[f.shape], pal = { P: f.c.petal, L: f.shape === 'shrub' || f.shape === 'waterlily' || f.shape === 'orchid' ? (f.shape === 'orchid' ? f.c.light : GREEN_D) : f.c.light,
    D: f.c.deep, C: f.c.centre, G: GREEN, W: '#5a4026', S: '#7a7a72', M: '#5a8a3a', '|': '#e8f0e0' };
  // In the shapes' leafy rows, L is a leaf below the bloom: green there.
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    if (l === '.') return;
    let col = pal[l];
    if (l === 'L' && y >= (f.shape === 'trumpet' || f.shape === 'cup' || f.shape === 'globe' || f.shape === 'daisy' || f.shape === 'spike' || f.shape === 'bells' ? 9 : f.shape === 'star' ? 11 : 99)) col = GREEN_L;
    if (f.shape === 'orchid' && l === 'L' && y >= 7) col = GREEN_D;
    if (f.shape === 'vine' && l === 'L') col = GREEN_L;
    img.set([parseInt(col.slice(1, 3), 16), parseInt(col.slice(3, 5), 16), parseInt(col.slice(5, 7), 16), 255], (y * 16 + x) * 4);
  }));
  return kit.png(16, 16, img);
}

if (require.main === module) {
  const only = process.argv.slice(2);
  const iconDir = path.join(ASSETS, 'textures', 'item', 'flower');
  fs.mkdirSync(iconDir, { recursive: true });
  for (const f of FLOWERS) {
    if (only.length && !only.includes(f.name)) continue;
    make(f.name, build(f), f.rarity);
    fs.writeFileSync(path.join(iconDir, f.name + '.png'), icon(f));
  }
}
module.exports = { FLOWERS };
