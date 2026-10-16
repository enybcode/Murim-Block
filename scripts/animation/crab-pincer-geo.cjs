// The giant pincer the Crab King calls out of the ground (docs/mobs/crab-king.md, his "Pincer from the deep"), a
// Blockbench model for GeckoLib, in the king's own look (crab-king-3d.cjs: raw red shell, brown knobbed crust, dark
// tips), cubes painted texel by texel (two texels a pixel), drawn 2.2 times its size (some seven blocks tall:
// client/CrabPincerRenderer). It stands on the ground, its jaws up:
//   rubble      a ring of broken ground and crust about its foot, still;
//   turn        turns the claw about its foot (its y rotation);
//   pincer      the claw on it, its foot joint: it rises and sinks (its position) and leans (x: toward its front; z: to
//               its side), the lean shared down its joints (Enzo, 16/10: "the pincer's joints well done"):
//     seg1, seg2  the arm's next two segments, each on its own joint (a ring of dark skin between them);
//     palm      the swollen palm, knobbed and spiked, on the last joint;
//     jaw_fixed the jaw on its left (-x), curving up and in, teeth on its inner edge;
//     jaw_moving the jaw on its right (+x), opened and shut about its root (its z rotation).
// The jaws meet some 50 pixels up, about 0.9 block apart halfway: they close on whoever stands on it.
//   geo/entity/crab_pincer.geo.json, animations/entity/crab_pincer.animation.json, textures/entity/crab_pincer.png;
//   art/blockbench/crab_pincer.bbmodel (written by Blockbench itself from the geometry: --bbmodel).
// Its one animation, "attack", is the pincer's whole life (CrabPincerEntity, Enzo 16/10: "it must sweep, then strike
// to its side, and try to pinch the players"): under the ground until EMERGE (cracks there meanwhile), bursting up wide
// open; leaning down almost flat and SWEEPING all round its foot at the ground (jump it); up again, then STRIKING down
// to its side; up, then lunging to its front, jaws wide, and PINCHING shut there; sinking back, gone at END.
// Usage: node scripts/animation/crab-pincer-geo.cjs [--bbmodel] [--port=9234]
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ATLAS = 256;
const D = 2;
/** The animation's moments (seconds): it rises; it sweeps (from, to); it strikes to its side; it pinches; it sinks; gone. */
const EMERGE = 0.6, SWEEP_FROM = 1.4, SWEEP_TO = 2.2, STRIKE = 2.85, PINCH = 3.7, SINK = 4.0, END = 4.6;

// ---------------------------------------------------------------- noise and colours (as the king's)

let seed = 4242;
const rand = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
const P = new Uint8Array(512);
{ const p = [...Array(256).keys()]; let s = 99; const r = () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 4294967296; };
  for (let i = 255; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [p[i], p[j]] = [p[j], p[i]]; } for (let i = 0; i < 512; i++) P[i] = p[i & 255]; }
const fade = t => t * t * t * (t * (t * 6 - 15) + 10), lerp = (a, b, t) => a + (b - a) * t;
const grad = (h, x, y, z) => { const u = (h & 15) < 8 ? x : y, v = (h & 15) < 4 ? y : (h & 15) === 12 || (h & 15) === 14 ? x : z; return ((h & 1) ? -u : u) + ((h & 2) ? -v : v); };
function noise(x, y, z) {
  const X = Math.floor(x) & 255, Y = Math.floor(y) & 255, Z = Math.floor(z) & 255;
  x -= Math.floor(x); y -= Math.floor(y); z -= Math.floor(z);
  const u = fade(x), v = fade(y), w = fade(z);
  const A = P[X] + Y, AA = P[A] + Z, AB = P[A + 1] + Z, B = P[X + 1] + Y, BA = P[B] + Z, BB = P[B + 1] + Z;
  return lerp(lerp(lerp(grad(P[AA], x, y, z), grad(P[BA], x - 1, y, z), u), lerp(grad(P[AB], x, y - 1, z), grad(P[BB], x - 1, y - 1, z), u), v),
    lerp(lerp(grad(P[AA + 1], x, y, z - 1), grad(P[BA + 1], x - 1, y, z - 1), u), lerp(grad(P[AB + 1], x, y - 1, z - 1), grad(P[BB + 1], x - 1, y - 1, z - 1), u), v), w);
}
const fbm = (x, y, z, o = 3) => { let s = 0, a = 0.5, f = 1; for (let i = 0; i < o; i++) { s += a * noise(x * f, y * f, z * f); a *= 0.5; f *= 2.03; } return s; };
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const smooth = (a, b, v) => { const t = clamp((v - a) / (b - a)); return t * t * (3 - 2 * t); };
const hexRgb = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const C = Object.fromEntries(Object.entries({
  flesh: '#6e1a20', fleshLight: '#97353a', fleshDeep: '#3a070c', vein: '#1e0610',
  crust: '#55361f', crustLight: '#8c6440', crustDeep: '#221409', spike: '#1e130b',
  claw: '#701820', clawLight: '#a8363c', tip: '#160809', tooth: '#a0937a',
  dirt: '#5b4430', dirtDark: '#3b2a1c', stone: '#6f6f6f', stoneDark: '#4a4a4a',
}).map(([k, v]) => [k, hexRgb(v)]));
const mix = (a, b, t) => a.map((x, i) => x + (b[i] - x) * clamp(t));
const hex = c => '#' + c.map(x => Math.max(0, Math.min(255, Math.round(x))).toString(16).padStart(2, '0')).join('');
const S = 1 / 8; // the noise's scale: a pixel here is half the king's (the pincer is drawn at its own size)
const veinAt = v => 1 - smooth(0.006, 0.028, Math.abs(noise(v[0] * 0.42 * S * 8 + 7, v[1] * 0.16 * S * 8, v[2] * 0.42 * S * 8)));
const flesh = v => {
  const h = fbm(v[0] * 0.28, v[1] * 0.28, v[2] * 0.28);
  return mix(mix(mix(C.fleshDeep, C.flesh, smooth(-0.85, -0.45, h)), C.fleshLight, smooth(0.12, 0.45, h)), C.vein, veinAt(v) * 0.35);
};
const crust = (v, n) => {
  const h = fbm(v[0] * 0.25, v[1] * 0.25, v[2] * 0.25), g = noise(v[0] * 1.1, v[1] * 1.1, v[2] * 1.1);
  return mix(mix(C.crustDeep, C.crust, smooth(-0.4, 0.0, h)), C.crustLight, smooth(0.08, 0.42, h + g * 0.25) * (0.55 + 0.45 * Math.max(0, n[1])));
};
const claw = (y0, y1) => v => {
  const t = clamp((v[1] - y0) / (y1 - y0));
  return mix(mix(C.claw, C.clawLight, smooth(-0.1, 0.4, fbm(v[0] * 0.45, v[1] * 0.45, v[2] * 0.45)) * 0.7), C.tip, smooth(0.7, 0.98, t));
};
const rubble = (v, n) => {
  const h = noise(v[0] * 0.6, v[1] * 0.6, v[2] * 0.6);
  return h > 0.15 ? mix(C.stoneDark, C.stone, h) : mix(C.dirtDark, C.dirt, h + 0.5);
};
const flat = c => () => c;

// ---------------------------------------------------------------- cubes: one turn about one axis (no ambiguity)

const BONES = { rubble: { pivot: [0, 0, 0] }, turn: { pivot: [0, 0, 0] }, pincer: { parent: 'turn', pivot: [0, 0, 0] },
  seg1: { parent: 'pincer', pivot: [0, 6, 0] }, seg2: { parent: 'seg1', pivot: [0, 12, 0] },
  palm: { parent: 'seg2', pivot: [0, 18, 0] }, jaw_fixed: { parent: 'palm', pivot: [-7, 22, 0] }, jaw_moving: { parent: 'palm', pivot: [7, 22, 0] } };
/** The bone each part of the arm hangs on, by height (pixels): its foot to y 6, then each joint. */
const armBone = y => (y < 6 ? 'pincer' : y < 12 ? 'seg1' : 'seg2');
const PIECES = [];
let n = 0;
const turn = (axis, deg, pivot, p) => {
  if (!deg) return p;
  const a = deg * Math.PI / 180, c = Math.cos(a), s = Math.sin(a), [x, y, z] = p.map((v, i) => v - pivot[i]);
  const r = axis === 'x' ? [x, y * c - z * s, y * s + z * c] : axis === 'y' ? [x * c + z * s, y, -x * s + z * c] : [x * c - y * s, x * s + y * c, z];
  return r.map((v, i) => v + pivot[i]);
};
const CORNERS = {
  north: (a, b) => [[b[0], b[1], a[2]], [a[0], b[1], a[2]], [a[0], a[1], a[2]]], south: (a, b) => [[a[0], b[1], b[2]], [b[0], b[1], b[2]], [b[0], a[1], b[2]]],
  east: (a, b) => [[b[0], b[1], b[2]], [b[0], b[1], a[2]], [b[0], a[1], a[2]]], west: (a, b) => [[a[0], b[1], a[2]], [a[0], b[1], b[2]], [a[0], a[1], b[2]]],
  up: (a, b) => [[b[0], b[1], b[2]], [a[0], b[1], b[2]], [a[0], b[1], a[2]]], down: (a, b) => [[b[0], a[1], a[2]], [a[0], a[1], a[2]], [a[0], a[1], b[2]]],
};
const NORMAL = { north: [0, 0, -1], south: [0, 0, 1], east: [1, 0, 0], west: [-1, 0, 0], up: [0, 1, 0], down: [0, -1, 0] };
/** A cube of size s centred on c on `bone`, turned `deg` about `axis` round its centre, painted by paint(point, normal). */
function cube(bone, s, c, paint, axis = 'z', deg = 0, edge = 0.85) {
  const id = n++, from = c.map((v, i) => v - s[i] / 2), to = c.map((v, i) => v + s[i] / 2);
  const p = { bone, from, to, density: D, axis, deg, pivot: c };
  p.paint = (f, u, v, w, h) => {
    const [tl, tr, bl] = CORNERS[f](from, to), a = (u + 0.5) / w, b = (v + 0.5) / h;
    const local = [0, 1, 2].map(i => tl[i] + (tr[i] - tl[i]) * a + (bl[i] - tl[i]) * b);
    const point = turn(axis, deg, c, local), nn = turn(axis, deg, [0, 0, 0], NORMAL[f]);
    let k = 0.96 + 0.07 * (((u * 73 + v * 151 + id * 37) % 11) / 10);
    if ((u === 0 || v === 0 || u === w - 1 || v === h - 1) && w > 2 && h > 2) k *= edge;
    return hex(paint(point, nn).map(x => x * k));
  };
  PIECES.push(p);
  return p;
}
/** A chain of cubes along points in the x-y plane (z at z0), tapering through radii: a jaw. */
function chain(bone, pts, radii, paint, segs, depth = 1) {
  const at = t => { const x = t * (pts.length - 1), i = Math.min(pts.length - 2, Math.floor(x)), f = x - i; return [0, 1].map(k => pts[i][k] + (pts[i + 1][k] - pts[i][k]) * f); };
  for (let i = 0; i < segs; i++) {
    const a = at(i / segs), b = at((i + 1) / segs), t = (i + 0.5) / segs, r = lerp(radii[0], radii[1], t);
    const len = Math.hypot(b[0] - a[0], b[1] - a[1]) + r * 1.1, deg = Math.atan2(b[1] - a[1], b[0] - a[0]) * 180 / Math.PI - 90;
    cube(bone, [r * 2, len, r * 2 * depth], [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, 0], paint, 'z', deg, 0.88);
  }
  return at;
}
/** Knobs of crust over a box's faces (sides and top), each turned at random about y, some with a spike. */
function knobs(bone, c, r, count, size, spikeChance = 0.35) {
  for (let i = 0; i < count; i++) {
    const side = Math.floor(rand() * 5), s = size * (0.6 + rand() * 0.8);
    const u = (rand() - 0.5) * 2, v = (rand() - 0.5) * 2;
    let at;
    if (side === 0) at = [c[0] + r[0], c[1] + v * r[1], c[2] + u * r[2]];
    else if (side === 1) at = [c[0] - r[0], c[1] + v * r[1], c[2] + u * r[2]];
    else if (side === 2) at = [c[0] + u * r[0], c[1] + v * r[1], c[2] + r[2]];
    else if (side === 3) at = [c[0] + u * r[0], c[1] + v * r[1], c[2] - r[2]];
    else at = [c[0] + u * r[0], c[1] + r[1], c[2] + v * r[2]];
    cube(bone, [s, s * 0.8, s], at, crust, 'y', rand() * 80);
    if (rand() < spikeChance) {
      const out = side === 0 ? [1, 0, 0] : side === 1 ? [-1, 0, 0] : side === 2 ? [0, 0, 1] : side === 3 ? [0, 0, -1] : [0, 1, 0];
      for (let k = 0; k < 3; k++) {
        const w = s * 0.5 * (1 - k / 3) + 0.3;
        cube(bone, [w, out[1] ? s * 0.6 : w, w], at.map((x, j) => x + out[j] * (s * 0.5 + k * s * 0.55) + (out[1] ? 0 : 0)), flat(mix(C.crust, C.spike, 0.4 + k * 0.3)), 'y', 0, 0.9);
      }
    }
  }
}

// ---------------------------------------------------------------- the pincer

// The rubble about its foot: broken ground and crust, low, in a ring.
for (let i = 0; i < 14; i++) {
  const a = i / 14 * Math.PI * 2 + rand() * 0.3, d = 9 + rand() * 4, s = 3 + rand() * 3;
  cube('rubble', [s, 1.5 + rand() * 2.5, s * (0.7 + rand() * 0.5)], [Math.cos(a) * d, 0.8, Math.sin(a) * d], rubble, 'y', rand() * 90);
}
// The arm out of the ground: segments narrowing, the shell in plates, crust on it.
// (From just below the ground: leant flat, nothing of it may stick up behind its foot.)
for (let i = 0; i < 4; i++) {
  const y = -6 + i * 6, w = 12 - i * 0.6, bone = armBone(y + 1);
  // Each segment's shell, a little narrower at its ends; the joint above it a ring of dark skin, set in.
  cube(bone, [w, 5.2, w * 0.85], [0, y + 3, 0], flesh, 'y', i * 7);
  cube(bone, [w + 0.7, 1.4, w * 0.85 + 0.7], [0, y + 4.6, 0], v => mix(flesh(v), C.claw, 0.3), 'y', i * 7);
  cube(bone, [w - 2.4, 1.2, w * 0.85 - 2.4], [0, y + 5.9, 0], v => mix(C.fleshDeep, C.vein, 0.3), 'y', 0, 0.95);
  knobs(bone, [0, y + 3, 0], [w / 2, 2.4, w * 0.42], 4, 2.4, 0.3);
}
// The palm: swollen, knobbed and spiked.
cube('palm', [18, 10, 13], [0, 18, 0], flesh);
cube('palm', [16, 4, 11], [0, 24, 0], v => mix(flesh(v), C.claw, 0.4));
// The joint under the palm, dark skin set in.
cube('palm', [9, 1.4, 7], [0, 18.6, 0], v => mix(C.fleshDeep, C.vein, 0.3), 'y', 0, 0.95);
knobs('palm', [0, 18, 0], [9, 5, 6.5], 22, 3, 0.45);
// The jaws: from the palm's top, curving up and in, meeting some 50 pixels up; teeth on the inner edges.
const JAWS = [
  // The fixed jaw broad, the moving one slimmer, both tapering to a hooked point.
  ['jaw_fixed', [[-7, 22], [-12.5, 33], [-10, 44], [-2, 52]], -1, [4.4, 0.6], 1.15],
  ['jaw_moving', [[7, 22], [11.5, 33], [9, 44], [1.5, 51]], 1, [3.4, 0.5], 0.95],
];
for (const [bone, pts, side, radii, depth] of JAWS) {
  const at = chain(bone, pts, radii, claw(22, 52), 16, depth);
  // Serrated teeth along the inner edge, bigger near the root.
  for (let i = 0; i < 7; i++) {
    const t = 0.1 + i * 0.11, p = at(t), w = radii[0] * (1 - t) + radii[1] * t, sz = 2.4 - i * 0.22;
    cube(bone, [sz, sz * 0.7, 1.4], [p[0] - side * (w + sz * 0.3), p[1], 0], flat(C.tooth), 'z', side * (35 + i * 4), 0.9);
  }
  // A crust ridge along the outer edge of the root.
  for (let i = 0; i < 4; i++) { const p = at(0.05 + i * 0.1); cube(bone, [2.4, 2.6, 2.6], [p[0] + side * radii[0] * 0.9, p[1], 0], crust, 'z', side * 15); }
  knobs(bone, [pts[1][0] + side * 1.5, pts[1][1], 0], [2, 4, 2], 4, 1.6, 0.5);
}

// ---------------------------------------------------------------- GeckoLib geometry, animation, texture

function geo(faces) {
  const bones = Object.entries(BONES).map(([name, b]) => ({ name, ...(b.parent ? { parent: b.parent } : {}), pivot: b.pivot, cubes: [] }));
  const byName = Object.fromEntries(bones.map(b => [b.name, b]));
  for (const p of PIECES) {
    const uv = Object.fromEntries(kit.FACES.map(f => {
      const [u0, v0, u1, v1] = p.uv[f];
      return [f, { uv: [u0 / D, v0 / D], uv_size: [(u1 - u0) / D, (v1 - v0) / D] }];
    }));
    const size = p.to.map((v, i) => +(v - p.from[i]).toFixed(4));
    const c = { origin: p.from.map(v => +v.toFixed(4)), size, uv };
    if (p.deg) { c.pivot = p.pivot.map(v => +v.toFixed(4)); c.rotation = [p.axis === 'x' ? p.deg : 0, p.axis === 'y' ? p.deg : 0, p.axis === 'z' ? p.deg : 0].map(v => +v.toFixed(3)); }
    byName[p.bone].cubes.push(c);
  }
  return {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: 'geometry.crab_pincer', texture_width: ATLAS / D, texture_height: ATLAS / D,
        visible_bounds_width: 3, visible_bounds_height: 4.5, visible_bounds_offset: [0, 1.5, 0] },
      bones,
    }],
  };
}

/** Keys at times (seconds) for a bone channel: [[t, [x, y, z]], ...] in GeckoLib's shape. */
// Plain Bedrock keys (arrays): GeckoLib and Blockbench both read them.
const keys = list => Object.fromEntries(list.map(([t, v]) => [t.toFixed(2), v]));
const OPEN_F = 30, OPEN_M = -30, SHUT_F = -4, SHUT_M = 4;
const jaws = (fixed, moving) => [fixed, moving];
/** The jaws through its life: wide while it rises, clacking as it sweeps, shut on the strike, wide, snapped at PINCH. */
const JAW_KEYS = [[0, jaws(26, -26)], [EMERGE + 0.3, jaws(32, -32)], [SWEEP_FROM, jaws(10, -10)], [SWEEP_FROM + 0.2, jaws(24, -24)],
  [SWEEP_FROM + 0.4, jaws(6, -6)], [SWEEP_FROM + 0.6, jaws(24, -24)], [SWEEP_TO, jaws(8, -8)], [STRIKE - 0.3, jaws(SHUT_F, SHUT_M)],
  [STRIKE + 0.2, jaws(SHUT_F, SHUT_M)], [3.2, jaws(36, -36)], [PINCH - 0.06, jaws(40, -40)], [PINCH, jaws(SHUT_F, SHUT_M)],
  [SINK, jaws(-2, 2)], [END, jaws(14, -14)]];
/** The whole claw's lean through its life (degrees: x toward its front, z to its side): shared down its joints. */
const LEAN = [[0, [0, 0, 0]], [EMERGE + 0.4, [0, 0, 0]], [SWEEP_FROM, [76, 0, 0]], [SWEEP_TO, [76, 0, 0]], [2.45, [-12, 0, 0]],
            [2.62, [0, 0, -16]], [STRIKE, [0, 0, 84]], [STRIKE + 0.15, [0, 0, 80]], [3.2, [-10, 0, 0]], [3.45, [-20, 0, 0]],
            [PINCH, [78, 0, 0]], [PINCH + 0.15, [74, 0, 0]], [SINK, [60, 0, 0]], [END, [0, 0, 0]]];
/** How far behind the joint below it each joint follows (seconds): the arm curves into a move and whips out of it. */
const LAG = 0.05;
/** The palm curling in further as it pinches (x, degrees), and as it lies for the sweep. */
const CURL = t => (t > 3.45 && t < 4.1 ? 22 * Math.sin(Math.PI * Math.min(1, (t - 3.45) / 0.65)) : 0) + (t > 1.2 && t < 2.4 ? 10 * Math.sin(Math.PI * (t - 1.2) / 1.2) : 0);
/** The lean at t, between its keys (linear, as GeckoLib plays plain keys). */
function leanAt(t) {
  t = Math.max(0, Math.min(END, t));
  for (let k = 0; k < LEAN.length - 1; k++) {
    const [t0, a] = LEAN[k], [t1, b] = LEAN[k + 1];
    if (t <= t1) { const f = t1 > t0 ? (t - t0) / (t1 - t0) : 1; return a.map((v, n) => v + (b[n] - v) * f); }
  }
  return LEAN[LEAN.length - 1][1];
}
/** Joint number `level` (0 its foot): its share of the lean, `level` lags behind, plus its own extra x. */
function joint(level, share, extra = () => 0) {
  const list = [];
  for (let t = 0; t <= END + 1e-9; t += 0.05) {
    const l = leanAt(t - level * LAG);
    list.push([Math.round(t * 100) / 100, [l[0] * share + extra(t), l[1] * share, l[2] * share].map(v => Math.round(v * 100) / 100)]);
  }
  return keys(list);
}

const ANIMATION = {
  format_version: '1.8.0',
  animations: {
    attack: {
      animation_length: END,
      bones: {
        // Turned about its foot: through the sweep, round to its side for the strike, back to its front to pinch.
        // (At 180 its roll falls square to its pinch: to its side, seen in game.)
        turn: { rotation: keys([[0, [0, -100, 0]], [SWEEP_FROM, [0, -100, 0]], [SWEEP_TO, [0, 200, 0]], [2.45, [0, 180, 0]],
          [STRIKE + 0.15, [0, 180, 0]], [3.2, [0, 360, 0]], [END, [0, 360, 0]]]) },
        // Under the ground, bursting up; leant almost flat for the sweep; up; down on its side; up; leant to its front.
        pincer: {
          position: keys([[0, [0, -60, 0]], [EMERGE, [0, -60, 0]], [EMERGE + 0.3, [0, 4, 0]], [EMERGE + 0.4, [0, 0, 0]],
            [SINK, [0, 0, 0]], [END - 0.05, [0, -62, 0]], [END, [0, -62, 0]]]),
          rotation: joint(0, 0.34),
        },
        seg1: { rotation: joint(1, 0.26) },
        seg2: { rotation: joint(2, 0.24) },
        palm: { rotation: joint(3, 0.16, CURL) },
        jaw_fixed: { rotation: keys(JAW_KEYS.map(([t, j]) => [t, [0, 0, j[0]]])) },
        jaw_moving: { rotation: keys(JAW_KEYS.map(([t, j]) => [t, [0, 0, j[1]]])) },
      },
    },
  },
};

const faces = kit.pack(PIECES, ATLAS);
const png = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { key: 'crab_pincer', c: {} }, ATLAS));
const model = geo(faces);
fs.writeFileSync(path.join(ASSETS, 'geo', 'entity', 'crab_pincer.geo.json'), JSON.stringify(model, null, 1) + '\n');
fs.writeFileSync(path.join(ASSETS, 'animations', 'entity', 'crab_pincer.animation.json'), JSON.stringify(ANIMATION, null, 1) + '\n');
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'crab_pincer.png'), png);
const used = Math.max(...faces.map(f => f.v + f.h));
console.log('wrote the giant pincer:', PIECES.length, 'cubes, atlas rows used', used, 'of', ATLAS);

module.exports = { EMERGE, SWEEP_FROM, SWEEP_TO, STRIKE, PINCH, SINK, END };
