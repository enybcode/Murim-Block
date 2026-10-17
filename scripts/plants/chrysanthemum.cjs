// The chrysanthemum (菊花, docs/cultivation/plants.md), the first herb: a common flower of the peach valley, its heads
// the herb. Made as the good 3D flower packs make theirs (Weskerson's Nature, Actually 3D Flowers & Plants): at
// Minecraft's own 16 texels a block, thin stems a pixel wide, narrow leaves tilted out, and heads of flat planes whose
// pixel art (cut out where clear) draws the petals: a round top seen from above and two crossed sides for the dome of a
// florist's chrysanthemum, its incurved petals in rows. Three colours: gold (the classic), white, purple.
//   stage 0  the seedling: two seed leaves;
//   stage 1  the young plant: a stem, leaves, a green bud showing its colour;
//   stage 2  mature: three stems, two heads open and a bud (the harvest);
//   stage 3  spirit maturity: the heads' petal tips gone pale and glowing, motes of Qi turning round them.
// Animations: "sway" (looping, every stage) and "spirit" (looping, stage 3: the motes).
// Usage: node scripts/plants/chrysanthemum.cjs
const { Plant, sampled, mix, hex } = require('./plant-kit.cjs');

const P = new Plant('chrysanthemum', 4, 1, 64);
const COLOURS = [
  { key: 'gold', c: { tip: '#ffe066', petal: '#f5b324', deep: '#c47a12', shade: '#8f5208', spirit: '#fff6c8' } },
  { key: 'white', c: { tip: '#ffffff', petal: '#ece6d2', deep: '#c8bd9c', shade: '#9a8f70', spirit: '#ffffff' } },
  { key: 'purple', c: { tip: '#ee9ad0', petal: '#c4569c', deep: '#8e2c70', shade: '#5e1a4a', spirit: '#ffe0f4' } },
];
const STEM = '#4f7d2c', STEM_DARK = '#365c1d', LEAF = '#4a7b2b', LEAF_LIGHT = '#679d3c', LEAF_DARK = '#2f5419';

// ---------------------------------------------------------------- pixel art on planes

/** A plane: a box with no thickness along one axis ('x', 'y' or 'z'); paint(u, v, w, h, colours) on its two faces. */
function plane(stages, bone, axis, size, centre, art, turnAxis = 'y', deg = 0, glow = null) {
  const s = axis === 'x' ? [0, size[1], size[0]] : axis === 'y' ? [size[0], 0, size[1]] : [size[0], size[1], 0];
  const facing = axis === 'x' ? ['east', 'west'] : axis === 'y' ? ['up', 'down'] : ['north', 'south'];
  const paint = (p, n, c, t) => (facing.includes(t.f) ? art(t.u, t.v, t.w, t.h, c, t.f) : null);
  const g = glow ? (p, c, t) => (facing.includes(t.f) ? glow(t.u, t.v, t.w, t.h, c, t.f) : null) : null;
  return P.box(stages, bone, s, centre, paint, turnAxis, deg, g);
}

/** The head's top, seen from above: a disc of petals, its rows ringing in toward a lighter heart, dark seams between. */
const headTop = spirit => (u, v, w, h, c) => {
  const cx = (w - 1) / 2, cy = (h - 1) / 2, dx = u - cx, dy = v - cy, d = Math.hypot(dx, dy) / (w / 2);
  if (d > 1.02) return null;
  const ring = d > 0.7 ? 0 : d > 0.4 ? 1 : 2, seam = (Math.round(Math.atan2(dy, dx) / (Math.PI / 4)) + ring) % 2 === 0;
  let col = ring === 0 ? (seam ? c.deep : c.petal) : ring === 1 ? (seam ? c.petal : c.tip) : c.tip;
  if (spirit && ring === 0) col = seam ? c.petal : c.spirit;
  return col;
};
/** The head's side: a dome of incurved petals in rows (each row offset), darker below, its top rounded off. */
const headSide = spirit => (u, v, w, h, c) => {
  const cx = (w - 1) / 2, x = Math.abs(u - cx) / (w / 2), rowsFromTop = v;
  // The dome's outline: the top row narrower.
  if (rowsFromTop === 0 && x > 0.55) return null;
  if (rowsFromTop === 1 && x > 0.9) return null;
  const scale = (u + (v % 2)) % 2 === 0;
  const base = v >= h - 1 ? c.shade : v >= h - 2 ? c.deep : scale ? c.petal : c.tip;
  if (spirit && v <= 1) return scale ? c.spirit : c.tip;
  return v >= h - 1 && x > 0.8 ? null : base;
};
const glowHead = (u, v, w, h, c) => (v <= 1 ? c.spirit : null);
const glowTop = (u, v, w, h, c) => {
  const d = Math.hypot(u - (w - 1) / 2, v - (h - 1) / 2) / (w / 2);
  return d > 0.7 && d <= 1.02 ? c.spirit : null;
};

/** A lobed leaf on a plane, its tip at the far end (u high): a midrib, two notches each side. */
const leafArt = (u, v, w, h) => {
  const t = u / (w - 1), mid = (h - 1) / 2, half = (h / 2) * (t < 0.15 ? 0.5 : t > 0.85 ? 0.45 : 1);
  if (Math.abs(v - mid) > half) return null;
  if ((u === Math.round(w * 0.45) || u === Math.round(w * 0.75)) && Math.abs(v - mid) >= half - 0.6) return null;   // the notches
  if (Math.abs(v - mid) < 0.6) return LEAF_LIGHT;
  return (u + v) % 3 === 0 ? LEAF_DARK : LEAF;
};

// ---------------------------------------------------------------- the plant

/** A stem a pixel wide from (x, z) up to `top`. */
function stem(stages, bone, x, z, top) {
  P.box(stages, bone, [1, top, 1], [x, top / 2, z], (p, n) => (n[0] > 0 || n[2] < 0 ? STEM : STEM_DARK));
}
/** A leaf from (x, y, z) out along `dir` (+x, -x, +z or -z), `len` long, `wide` wide, tilted up 22.5 degrees. */
function leaf(stages, bone, x, y, z, dir, len = 4, wide = 2) {
  const along = dir === '+x' || dir === '-x', sign = dir[0] === '+' ? 1 : -1;
  const c = along ? [x + sign * len / 2, y, z] : [x, y, z + sign * len / 2];
  // The plane lies flat, then tips up: about z for a leaf along x, about x for one along z.
  const art = (u, v, w, h) => leafArt(along ? (sign > 0 ? u : w - 1 - u) : (sign > 0 ? v : h - 1 - v), along ? v : u, along ? w : h, along ? h : w);
  const size = along ? [len, wide] : [wide, len];
  plane(stages, bone, 'y', size, c, art, along ? 'z' : 'x', along ? sign * 22.5 : -sign * 22.5);
}
/**
 * A head: a ball of petals in voxels, `r` its radius (2 or 3), its top at `top`: layers narrowing up and down (corners
 * cut), each face painted as rows of incurved petals (a scale pattern, light tips), and a ring of single petal tips
 * sticking out at its waist, drooping a little, for a flower's ragged outline.
 */
function head(stages, bone, x, top, z, r, spirit) {
  const H = r * 2, y0 = top - H;
  const paint = (p, n, c, t) => {
    const up = (p[1] - y0) / H;
    if (n[1] > 0) return spirit && up > 0.8 ? c.spirit : (t.u + t.v) % 2 ? c.tip : c.petal;
    if (n[1] < 0) return c.shade;
    const scale = (Math.floor(p[0] + p[2]) + Math.floor(p[1])) % 2 === 0;
    if (up < 0.3) return scale ? c.deep : c.shade;
    if (spirit && up > 0.75) return scale ? c.spirit : c.tip;
    return scale ? c.petal : up > 0.6 ? c.tip : c.deep;
  };
  const glow = spirit ? (p) => (p[1] - y0 > H * 0.7 ? '#ffffff' : null) : null;
  // Layers: [height from the bottom, half width]; each a cross of two boxes (the corners cut).
  const LAYERS = r >= 3 ? [[0.5, 1.5], [1.5, 2.5], [2.5, 3], [3.5, 3], [4.5, 2.5], [5.5, 1.5]] : [[0.5, 1], [1.5, 2], [2.5, 2], [3.5, 1]];
  for (const [h, w] of LAYERS) {
    const cut = w >= 2.5 ? 1 : 0;
    P.box(stages, bone, [w * 2, 1, w * 2 - cut * 2], [x, y0 + h, z], paint, 'y', 0, glow);
    if (cut) P.box(stages, bone, [w * 2 - cut * 2, 1, w * 2], [x, y0 + h, z], paint, 'y', 0, glow);
  }
  // Petal tips out of its waist, all round, one pixel each, the diagonal ones a little lower.
  const waist = y0 + H * 0.45, out = (r >= 3 ? 3 : 2) + 0.5;
  for (let k = 0; k < 8; k++) {
    const a = k * Math.PI / 4, d = k % 2 ? out - 0.5 : out;
    P.box(stages, bone, [1, 1, 1], [Math.round(x + Math.cos(a) * d * 1.0) + (x % 1), waist - (k % 2 ? 0.5 : 0), Math.round(z + Math.sin(a) * d) + (z % 1)],
      (pp, n, c) => (spirit ? c.spirit : n[1] < 0 ? c.deep : c.tip), 'y', 0, glow);
  }
  P.box(stages, bone, [2, 1, 2], [x, y0 - 0.5, z], () => STEM_DARK);   // the calyx
}

/** A bud: a small green box, its colour showing at its top. */
function bud(stages, bone, x, top, z) {
  P.box(stages, bone, [2, 2, 2], [x, top - 1, z], (p, n, c) => (n[1] > 0 ? c.petal : p[1] > top - 0.7 ? c.deep : LEAF_LIGHT));
}

// Stage 0: two seed leaves on a pixel of stem.
P.bone([0], 'sprout', 'root', [0, 0, 0]);
stem([0], 'sprout', 0, 0, 2);
leaf([0], 'sprout', 0.5, 1.6, 0, '+x', 3, 2);
leaf([0], 'sprout', -0.5, 1.6, 0, '-x', 3, 2);

// Stage 1: a stem, leaves and a bud.
P.bone([1], 'stem_a', 'root', [0, 0, 0]);
stem([1], 'stem_a', 0, 0, 7);
leaf([1], 'stem_a', 0.5, 2, 0, '+x', 4, 2);
leaf([1], 'stem_a', 0, 3.5, -0.5, '-z', 4, 2);
leaf([1], 'stem_a', -0.5, 5, 0, '-x', 3, 2);
bud([1], 'stem_a', 0, 9, 0);

// Stages 2 and 3: three stems, two heads and a bud.
const MATURE = [2, 3];
const STEMS = [
  // bone, x, z, stem height, head across (0: a bud)
  ['stem_a', 0.5, -0.5, 8, 3],
  ['stem_b', -3.5, 2.5, 6, 2],
  ['stem_c', 3.5, 3, 5, 0],
];
for (const [bone, x, z, top, r] of STEMS) {
  const hb = 'head_' + bone.slice(-1);
  P.bone(MATURE, bone, 'root', [x, 0, z]);
  P.bone(MATURE, hb, bone, [x, top, z]);
  stem(MATURE, bone, x, z, top);
  if (!r) bud(MATURE, hb, x, top + 2, z);
  else {
    const crown = top + r * 2 + 1;
    head([2], hb, x, crown, z, r, false);
    head([3], hb, x, crown, z, r, true);
  }
}
leaf(MATURE, 'stem_a', 1, 2, -0.5, '+x', 4, 2);
leaf(MATURE, 'stem_a', 0.5, 4, -1, '-z', 4, 2);
leaf(MATURE, 'stem_a', 0, 6, -0.5, '-x', 3, 2);
leaf(MATURE, 'stem_b', -3.5, 2.5, 2.5, '-x', 4, 2);
leaf(MATURE, 'stem_b', -3, 4.5, 3, '+z', 3, 2);
leaf(MATURE, 'stem_c', 4, 2, 3, '+x', 3, 2);
// Spirit maturity: motes of Qi round the heads.
P.bone([3], 'motes', 'root', [0, 10, 0]);
for (let k = 0; k < 5; k++) {
  const a = k * 72 * Math.PI / 180, r = 6 + (k % 2);
  P.box([3], 'motes', [1, 1, 1], [Math.cos(a) * r, 9 + (k % 3) * 2, Math.sin(a) * r], (p, n, c) => c.spirit, 'y', 0, (p, c) => c.spirit);
}

// ---------------------------------------------------------------- animations

const SWAY = 4;
const wave = (t, amp, phase) => amp * Math.sin(2 * Math.PI * t / SWAY + phase);
const animations = {
  sway: { loop: true, animation_length: SWAY, bones: {
    sprout: { rotation: sampled(SWAY, 0.2, t => [wave(t, 3, 0.3), 0, wave(t, 4, 0)]) },
    stem_a: { rotation: sampled(SWAY, 0.2, t => [wave(t, 2.5, 1.2), 0, wave(t, 4, 0)]) },
    stem_b: { rotation: sampled(SWAY, 0.2, t => [wave(t, 3, 2.1), 0, wave(t, 4.5, 0.9)]) },
    stem_c: { rotation: sampled(SWAY, 0.2, t => [wave(t, 3, 0.4), 0, wave(t, 4, 2.0)]) },
    head_a: { rotation: sampled(SWAY, 0.2, t => [wave(t, 4, 0.5), 0, wave(t, 5, -0.7)]) },
    head_b: { rotation: sampled(SWAY, 0.2, t => [wave(t, 4, 1.4), 0, wave(t, 5, 0.2)]) },
    head_c: { rotation: sampled(SWAY, 0.2, t => [wave(t, 4, -0.3), 0, wave(t, 4, 1.3)]) },
  } },
  spirit: { loop: true, animation_length: 6, bones: {
    motes: { rotation: sampled(6, 0.25, t => [0, -60 * t, 0]), position: sampled(6, 0.25, t => [0, Math.sin(2 * Math.PI * t / 3) * 0.8, 0]) },
  } },
};

P.write(COLOURS, animations);
module.exports = { P, COLOURS };
