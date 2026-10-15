// The Crab King (蟹王), the boss of the crab art (docs/mobs/crab-king.md): a martial artist the crab men's sickness has
// taken whole, in 3D on Epic Fight's biped skeleton (drawn 1.5 times larger in game), after the concept Enzo chose
// (scripts/concepts/crab-art-pixel.cjs, stage 3): cubes painted texel by texel (two texels a pixel), turned at angles
// so nothing reads as one big box.
//   art/blockbench/crab_king.bbmodel (+ crab_king/crab_king.png)   the Blockbench project (its turned cubes upright);
//   assets/murimblock/animmodels/entity/crab_king.json             the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/crab_king.png                its atlas.
// The look: raw red flesh with dark veins, ridged plates on the chest and the belly; a stepped shell of brown crust over
// the back and the head like a hood, knobbed and spiked, its brow ragged over the face; blue eyes on stalks under the
// brow, the face sunk and dark, mandibles at the mouth; crusted shoulders; the forearms swollen, ending in long curved
// pincers, the right one bigger: the outer finger on the forearm (Hand_*), the inner one on the hand (Tool_*), so turning
// the hand opens the pincer; a second pair of thin arms with small pincers; crusted knees and clawed feet; what is left
// of his robe: a dark sash with a jade pendant, a torn hem at the hips.
// Usage: node scripts/animation/crab-king-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { at } = require('./blockbench-rig.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 512;
const D = 2;

// ---------------------------------------------------------------- noise and colours

let seed = 7331;
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
  flesh: '#6e1a20', fleshLight: '#97353a', fleshDeep: '#3a070c', vein: '#1e0610', skinSick: '#7d5f55',
  crust: '#55361f', crustLight: '#8c6440', crustDeep: '#221409', spike: '#1e130b',
  claw: '#701820', clawLight: '#9e3036', tip: '#160809', tooth: '#a0937a', pus: '#9a8238', eye: '#2d93dc', glint: '#bfe6ff', pupil: '#06080c',
  sash: '#24242c', sashDark: '#15151a', jade: '#5fae8a', jadeDark: '#2f6b52', gold: '#b08a3a',
}).map(([k, v]) => [k, hexRgb(v)]));
const mix = (a, b, t) => a.map((x, i) => x + (b[i] - x) * clamp(t));
const hex = c => '#' + c.map(x => Math.max(0, Math.min(255, Math.round(x))).toString(16).padStart(2, '0')).join('');

const veinAt = v => 1 - smooth(0.006, 0.028, Math.abs(noise(v[0] * 0.42 + 7, v[1] * 0.16, v[2] * 0.42) + 0.25 * noise(v[0] * 1.3, v[1] * 1.3, v[2] * 1.3)));
const flesh = (v) => {
  const h = fbm(v[0] * 0.55, v[1] * 0.55, v[2] * 0.55);
  let c = mix(C.fleshDeep, C.flesh, smooth(-0.7, -0.3, h));
  c = mix(c, C.fleshLight, smooth(0.12, 0.45, h));
  return mix(c, C.vein, veinAt(v) * 0.8);
};
const crust = (v, n) => {
  const h = fbm(v[0] * 0.5, v[1] * 0.5, v[2] * 0.5), g = noise(v[0] * 2.2, v[1] * 2.2, v[2] * 2.2);
  const c = mix(C.crustDeep, C.crust, smooth(-0.4, 0.0, h));
  return mix(c, C.crustLight, smooth(0.08, 0.42, h + g * 0.25) * (0.55 + 0.45 * Math.max(0, n[1])));
};
const clawPaint = (yTop, yBot) => (v) => {
  const t = clamp((yTop - v[1]) / (yTop - yBot));
  const c = mix(C.claw, C.clawLight, smooth(-0.1, 0.4, fbm(v[0] * 0.9, v[1] * 0.9, v[2] * 0.9)) * 0.7);
  return mix(c, C.tip, smooth(0.62, 0.95, t));
};
const flat = c => () => c;

// ---------------------------------------------------------------- turned cubes

/** Quaternions as [x, y, z, w]. */
const qMul = (a, b) => [a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1], a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
  a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3], a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2]];
const qRot = (q, v) => { const p = qMul(qMul(q, [...v, 0]), [-q[0], -q[1], -q[2], q[3]]); return [p[0], p[1], p[2]]; };
const qAxis = (axis, a) => { const s = Math.sin(a / 2), l = Math.hypot(...axis); return [axis[0] / l * s, axis[1] / l * s, axis[2] / l * s, Math.cos(a / 2)]; };
const qEuler = (x, y, z) => qMul(qMul(qAxis([0, 0, 1], z), qAxis([0, 1, 0], y)), qAxis([1, 0, 0], x));
const qFrom = (a, b) => {
  const la = Math.hypot(...a), lb = Math.hypot(...b), u = a.map(x => x / la), w = b.map(x => x / lb), d = u[0] * w[0] + u[1] * w[1] + u[2] * w[2];
  if (d < -0.9999) return qAxis(Math.abs(u[0]) < 0.9 ? [1, 0, 0] : [0, 0, 1], Math.PI);
  const c = [u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]], s = Math.sqrt((1 + d) * 2);
  return [c[0] / s, c[1] / s, c[2] / s, s / 2];
};
const Q0 = [0, 0, 0, 1];
const add = (a, b) => a.map((x, i) => x + b[i]), sub = (a, b) => a.map((x, i) => x - b[i]), scale = (a, k) => a.map(x => x * k);
const norm = a => { const l = Math.hypot(...a); return a.map(x => x / l); };

/** The corners of a face as seen from outside (as mesh-kit's): top left, top right, bottom right, bottom left. */
function corners(p, f) {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  switch (f) {
    case 'north': return [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]];
    case 'south': return [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]];
    case 'east': return [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]];
    case 'west': return [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]];
    case 'up': return [[x1, y1, z1], [x0, y1, z1], [x0, y1, z0], [x1, y1, z0]];
    default: return [[x1, y0, z0], [x0, y0, z0], [x0, y0, z1], [x1, y0, z1]];
  }
}
const NORMALS = { north: [0, 0, -1], south: [0, 0, 1], east: [1, 0, 0], west: [-1, 0, 0], up: [0, 1, 0], down: [0, -1, 0] };

const PIECES = [];
let pieceN = 0;
/**
 * A cube of size s centred on c, turned by q (about its centre), on `bone`, in mesh part `part`, its texels painted by
 * paint(point, normal) -> [r, g, b] with the point where it stands; a dither and darker edge texels.
 */
function cube(bone, part, s, c, q, paint, edge = 0.84) {
  const id = pieceN++;
  const p = {
    bone, part, name: part + '_' + id, from: [c[0] - s[0] / 2, c[1] - s[1] / 2, c[2] - s[2] / 2], to: [c[0] + s[0] / 2, c[1] + s[1] / 2, c[2] + s[2] / 2],
    density: D, turnCorner: q === Q0 ? null : (k) => add(qRot(q, sub(k, c)), c),
  };
  if (!p.turnCorner) delete p.turnCorner;
  p.paint = (f, u, v, w, h) => {
    const [tl, tr, , bl] = corners(p, f), a = (u + 0.5) / w, b = (v + 0.5) / h;
    const local = add(add(tl, scale(sub(tr, tl), a)), scale(sub(bl, tl), b));
    const point = add(qRot(q, sub(local, c)), c), n = qRot(q, NORMALS[f]);
    let col = paint(point, n);
    if (!col) return null;
    let k = 0.96 + 0.07 * (((u * 73 + v * 151 + id * 37) % 11) / 10);
    if ((u === 0 || v === 0 || u === w - 1 || v === h - 1) && w > 2 && h > 2) k *= edge;
    return hex(col.map(x => x * k));
  };
  PIECES.push(p);
  return p;
}
const block = (bone, part, from, to, paint, edge) => cube(bone, part, sub(to, from), scale(add(from, to), 0.5), Q0, paint, edge);

/** A Catmull-Rom curve through points: its point at t. */
function curveAt(pts, t) {
  const n = pts.length - 1, x = Math.min(n - 1e-6, t * n), i = Math.floor(x), f = x - i;
  const p0 = pts[Math.max(0, i - 1)], p1 = pts[i], p2 = pts[i + 1], p3 = pts[Math.min(n, i + 2)];
  return [0, 1, 2].map(k => 0.5 * ((2 * p1[k]) + (-p0[k] + p2[k]) * f + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * f * f + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * f * f * f));
}
/** A chain of cubes along points, tapering through radii (w the side-to-side squash): a claw, a stalk, a thin arm. */
function chain(bone, part, pts, radii, paint, segs = 10, w = 0.8) {
  for (let i = 0; i < segs; i++) {
    const a = curveAt(pts, i / segs), b = curveAt(pts, (i + 1) / segs), t = (i + 0.5) / segs;
    const r = lerp(radii[0], radii[1], t), len = Math.hypot(...sub(b, a)) + r * 0.6;
    cube(bone, part, [r * 2 * w, len, r * 2], scale(add(a, b), 0.5), qFrom([0, 1, 0], sub(b, a)), paint, 0.88);
  }
}
const onEllipsoid = (c, r, th, ph) => {
  const d = [Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th)];
  return [[c[0] + d[0] * r[0], c[1] + d[1] * r[1], c[2] + d[2] * r[2]], norm([d[0] / r[0], d[1] / r[1], d[2] / r[2]])];
};
/** Knobs: small cubes half sunk into an ellipsoid's surface, turned at random; some with a spike of stacked cubes. */
function knobs(bone, part, c, r, count, size, spikeChance = 0.3, spikeLen = 1.6, filter = () => true) {
  for (let i = 0; i < count; i++) {
    const th = rand() * Math.PI * 2, ph = Math.acos(2 * rand() - 1), [p, n] = onEllipsoid(c, r, th, ph);
    if (!filter(p, n)) continue;
    const s = size * (0.55 + rand() * 0.9), q = qMul(qFrom([0, 1, 0], n), qEuler(0, rand() * 1.5, (rand() - 0.5) * 0.5));
    cube(bone, part, [s, s * 0.8, s], add(p, scale(n, s * 0.15)), q, crust);
    if (rand() < spikeChance) {
      const l = spikeLen * (0.6 + rand() * 0.8), steps = 3;
      for (let k = 0; k < steps; k++) {
        const w = s * 0.55 * (1 - k / steps) + 0.15;
        cube(bone, part, [w, l / steps + 0.1, w], add(p, scale(n, s * 0.45 + (k + 0.5) * l / steps)), q, flat(mix(C.crust, C.spike, 0.4 + k * 0.3)), 0.9);
      }
    }
  }
}
/** A lump of crust: three crossed cubes, knobs over it. */
function clump(bone, part, c, r, knobCount, knobSize, spikeChance = 0.25) {
  block(bone, part, [c[0] - r[0], c[1] - r[1] * 0.6, c[2] - r[2] * 0.8], [c[0] + r[0], c[1] + r[1] * 0.6, c[2] + r[2] * 0.8], crust);
  block(bone, part, [c[0] - r[0] * 0.75, c[1] - r[1], c[2] - r[2] * 0.6], [c[0] + r[0] * 0.75, c[1] + r[1], c[2] + r[2] * 0.6], crust);
  block(bone, part, [c[0] - r[0] * 0.6, c[1] - r[1] * 0.75, c[2] - r[2]], [c[0] + r[0] * 0.6, c[1] + r[1] * 0.75, c[2] + r[2]], crust);
  knobs(bone, part, c, r, knobCount, knobSize, spikeChance, 1.3);
}

// ---------------------------------------------------------------- the body

const [, HEAD_Y] = at('Head'), [, CHEST_Y] = at('Chest'), [, TORSO_Y] = at('Torso');
const [, ARM_Y] = at('Arm_R'), [, ELBOW_Y] = at('Hand_R'), [, WRIST_Y] = at('Tool_R');
const [, KNEE_Y] = at('Leg_R');

// Legs: raw flesh, crust on the knees, clawed crusted feet; a torn hem of the robe over the thighs.
for (const [side, sx, leg, pants] of [['R', 1, 'rightLeg', 'rightPants'], ['L', -1, 'leftLeg', 'leftPants']]) {
  const x = sx * 2;
  const thigh = block('Thigh_' + side, leg, [x - 2.2, KNEE_Y, -2.2], [x + 2.2, 12.4, 2.2], flesh);
  thigh.hinge = { y: KNEE_Y, bone: 'Leg_' + side };
  const shin = block('Leg_' + side, leg, [x - 2.1, 0.8, -2.1], [x + 2.1, KNEE_Y, 2.1], flesh);
  shin.hinge = { y: KNEE_Y, bone: 'Thigh_' + side };
  clump('Leg_' + side, pants, [x, KNEE_Y - 0.6, -2.2], [2.3, 2.1, 0.9], 8, 0.9, 0.2);
  clump('Leg_' + side, pants, [x, 1.2, -0.3], [2.5, 1.2, 2.7], 7, 0.8, 0.1);
  for (const dx of [-1.3, 0, 1.3]) chain('Leg_' + side, pants, [[x + dx, 0.9, -2.5], [x + dx * 1.1, 0.6, -3.9], [x + dx * 1.15, 0.1, -4.8]], [0.48, 0.15], flat(C.tip), 4, 1);
  // The torn hem: strips of the old robe, uneven.
  for (let i = 0; i < 4; i++) {
    const hx = x - 1.8 + i * 1.2, len = 2.2 + rand() * 2.4;
    cube('Thigh_' + side, pants, [1.1, len, 0.4], [hx, 12.2 - len / 2, -2.45], qEuler((rand() - 0.5) * 0.2, 0, (rand() - 0.5) * 0.25),
      (v) => mix(C.sash, C.sashDark, smooth(12, 12 - len, v[1])), 0.9);
  }
}

// Belly and chest: raw, in ridged plates; the sash and its jade pendant.
const belly = block('Torso', 'torso', [-4.2, TORSO_Y - 0.6, -2.2], [4.2, CHEST_Y, 2.2], flesh);
belly.hinge = { y: CHEST_Y, bone: 'Chest' };
const chestBox = block('Chest', 'torso', [-4.4, CHEST_Y, -2.3], [4.4, HEAD_Y, 2.3], flesh);
chestBox.hinge = { y: CHEST_Y, bone: 'Torso' };
for (const sx of [-1, 1]) cube('Chest', 'jacket', [3.8, 3.2, 0.7], [sx * 2.05, 21.4, -2.5], qEuler(0.1, sx * -0.15, sx * 0.08), flesh);
for (let i = 0; i < 3; i++) cube('Chest', 'jacket', [5.6 - i * 0.5, 1.3, 0.5], [0, 19.2 - i * 1.4, -2.45], qEuler(0.12, 0, 0), flesh);
for (let i = 0; i < 2; i++) cube('Torso', 'jacket', [5 - i * 0.6, 1.3, 0.5], [0, 16.4 - i * 1.4, -2.4], qEuler(0.12, 0, 0), flesh);
block('Torso', 'jacket', [-4.5, 12.6, -2.6], [4.5, 14.4, 2.6], (v) => mix(C.sash, C.sashDark, noise(v[0] * 0.8, v[1], v[2] * 0.8) + 0.3));
block('Torso', 'jacket', [1.2, 11.2, -2.95], [2.8, 13.6, -2.6], (v, n) => mix(C.jadeDark, C.jade, 0.5 + 0.5 * noise(v[0] * 2, v[1] * 2, 0)));
block('Torso', 'jacket', [1.6, 13.6, -2.9], [2.4, 14.6, -2.65], flat(C.gold));
for (let i = 0; i < 7; i++) cube('Torso', 'jacket', [0.5, 0.5, 0.3], [-1.5 - rand() * 2, 14.6 + rand() * 2.5, -2.55], Q0, flat(C.pus), 0.8);

// The shell over the back: a dome lying on its side, in layers from the back, knobbed and spiked.
for (let z = 0; z < 4.2; z += 0.8) {
  const t = z / 4.2, k = Math.sqrt(Math.max(0, 1 - t * t));
  block('Chest', 'jacket', [-8.8 * k, 19.5 - 9 * k, 2.3 + z], [8.8 * k, 19.5 + 7.8 * k, 3.1 + z], crust, 0.9);
}
knobs('Chest', 'jacket', [0, 19.5, 2.9], [8.8, 8.6, 4.2], 44, 1.3, 0.35, 2.2, (p, n) => n[2] > 0.2);
// The nape and a collar of crust over the shoulders.
block('Chest', 'jacket', [-5.8, 22.5, 2.3], [5.8, 28.5, 6.4], crust, 0.9);
for (let x = -9.8; x < 9.8; x += 1.4) cube('Chest', 'jacket', [1.6, 2 + rand(), 5.4], [x + 0.7, 24.6 + rand() * 0.6, 1.5], qEuler(0, 0, (rand() - 0.5) * 0.5), crust);

// The head: sunk dark flesh, the eyes gone dark; mandibles; the hood over it, its brow ragged; eyes on stalks.
const FACE_EYES = [[-2, 27.5], [2, 27.5]];
block('Head', 'head', [-4, HEAD_Y, -4], [4, HEAD_Y + 8, 4], (v, n) => {
  if (n[2] < -0.5) for (const [x, y] of FACE_EYES) if (Math.abs(v[0] - x) < 1 && Math.abs(v[1] - y) < 0.55) return C.fleshDeep;
  if (n[2] < -0.5 && Math.abs(v[0]) < 1.5 && Math.abs(v[1] - 25.6) < 0.5) return C.vein;
  return mix(mix(C.skinSick, flesh(v), 0.55 + 0.4 * fbm(v[0] * 0.4 + 3, v[1] * 0.4, v[2] * 0.4)), C.vein, veinAt(v) * 0.6);
});
for (const sx of [-1, 1]) {
  chain('Head', 'head', [[sx * 1.4, 25.8, -4.1], [sx * 1.9, 24.8, -5.2], [sx * 0.6, 23.9, -5.6]], [0.55, 0.15], clawPaint(25.8, 23.9), 5, 1);
  chain('Head', 'hat', [[sx * 2.2, 31.2, -4.4], [sx * 2.7, 32.3, -5.6], [sx * 3.0, 32.7, -6.4]], [0.55, 0.42], flesh, 4, 1);
  block('Head', 'hat', [sx * 3.05 - 0.8, 32.3, -7.8], [sx * 3.05 + 0.8, 33.9, -6.2], (v, n) => {
    if (n[2] > -0.6) return C.fleshDeep;
    if (Math.abs(v[1] - 33.1) < 0.4 && Math.abs(v[0] - sx * 3.05) < 0.4) return C.pupil;
    if (Math.abs(v[1] - 33.6) < 0.25 && Math.abs(v[0] - sx * 3.05 + 0.4) < 0.25) return C.glint;
    return C.eye;
  }, 0.95);
}
// The hood: a stepped dome over the head, cut back so the face shows.
{
  const c = [0, HEAD_Y + 4, 2.4], r = [7.8, 8.8, 7.6];
  for (let y = 0; y < r[1]; y += 0.8) {
    const t = (y + 0.4) / r[1], k = Math.sqrt(Math.max(0, 1 - t * t)), w = r[0] * k, d = r[2] * k;
    if (w < 0.4) continue;
    const z0 = Math.max(c[2] - d, -3.6);
    if (c[2] + d <= z0) continue;
    block('Head', 'hat', [-w, c[1] + y, z0], [w, c[1] + y + 0.8, c[2] + d], crust, 0.9);
  }
  knobs('Head', 'hat', c, r, 56, 1.4, 0.4, 2.6, (p, n) => n[1] > -0.1 && p[2] > -3.8);
  for (let x = -6.6; x < 6.6; x += 1.1) cube('Head', 'hat', [1.3, 1 + rand() * 0.8, 2.4], [x + 0.55, 32.6 - Math.abs(x) * 0.12, -4.2], qEuler(0.3 + rand() * 0.3, 0, (rand() - 0.5) * 0.4), crust);
}

// The arms: crusted shoulders, swollen forearms, long curved pincers (the outer finger on the forearm, the inner one on
// the hand, which opens them); the right one bigger.
for (const [side, sx, arm, sleeve] of [['R', 1, 'rightArm', 'rightSleeve'], ['L', -1, 'leftArm', 'leftSleeve']]) {
  const big = sx > 0, k = big ? 1.5 : 1.25, L = big ? 12.6 : 11.2, x = sx * 6;
  const upper = block('Arm_' + side, arm, [x - 2.1, ELBOW_Y, -2.1], [x + 2.1, ARM_Y + 2, 2.1], flesh);
  upper.hinge = { y: ELBOW_Y, bone: 'Hand_' + side };
  clump('Arm_' + side, sleeve, [x + sx * 1.2, ARM_Y + 1.6, 0.2], [3.4, 2, 3.2], 16, 1.3, 0.5);
  const fore = block('Hand_' + side, arm, [x - 2.1, WRIST_Y, -2.1], [x + 2.1, ELBOW_Y, 2.1], flesh);
  fore.hinge = { y: ELBOW_Y, bone: 'Arm_' + side };
  // The forearm swollen: a sleeve of raw flesh in turned layers, crust on its back.
  for (let i = 0; i < 4; i++) {
    const y = WRIST_Y + 0.4 + i * 1.3, w = 2.3 * k * 0.85 + (i === 1 || i === 2 ? 0.35 : 0);
    cube('Hand_' + side, sleeve, [w * 2, 1.4, w * 2], [x, y, 0], qEuler(0, i * 0.18, 0), flesh, 0.9);
  }
  clump('Hand_' + side, sleeve, [x + sx * 1.4, WRIST_Y + 2.6, 1.4], [1.5 * k, 2.2, 1.3 * k], 7, 0.9, 0.3);
  // The palm: a long club, crusted at the knuckles.
  const top = WRIST_Y, palmLen = L * 0.36;
  for (let i = 0; i < 3; i++) {
    const t = i / 3;
    cube('Hand_' + side, sleeve, [(2.2 - t * 0.3) * 2 * k * 0.8, palmLen / 3 + 0.2, (1.9 - t * 0.3) * 2 * k * 0.8], [x, top - (i + 0.5) * palmLen / 3, -t * 0.4],
      qEuler(-t * 0.15, 0, 0), (v) => mix(flesh(v), C.claw, 0.5), 0.86);
  }
  clump('Hand_' + side, sleeve, [x + sx * 0.9 * k, top - 0.9, 0.6], [1.5 * k, 1.1, 1.3 * k], 8, 0.8, 0.45);
  const y0 = top - palmLen + 0.6;
  const fingers = [
    ['Hand_' + side, [[x + sx * 1.0 * k, y0, -0.2], [x + sx * 1.7 * k, y0 - L * 0.3, -0.9], [x + sx * 1.0 * k, y0 - L * 0.52, -1.5], [x - sx * 0.2, y0 - L * 0.62, -1.4]], [0.85 * k, 0.2]],
    ['Tool_' + side, [[x - sx * 1.0 * k, y0, -0.2], [x - sx * 1.5 * k, y0 - L * 0.26, -0.7], [x - sx * 0.6 * k, y0 - L * 0.44, -1.2], [x + sx * 0.15, y0 - L * 0.5, -1.2]], [0.75 * k, 0.18]],
  ];
  for (const [bone, pts, radii] of fingers) {
    chain(bone, sleeve, pts, radii, clawPaint(pts[0][1], pts[pts.length - 1][1]), 11, 0.8);
    for (let i = 1; i <= 3; i++) {
      const p = curveAt(pts, 0.12 + i * 0.16), toward = norm([x - p[0], 0, -0.3]);
      cube(bone, sleeve, [0.45 * k, 0.8 * k, 0.45 * k], add(p, scale(toward, 0.55 * k)), qFrom([0, 1, 0], toward), flat(C.tooth), 0.9);
    }
  }
  for (let i = 0; i < 6; i++) cube('Hand_' + side, sleeve, [0.5, 0.5, 0.35], [x - sx * (1.2 + rand() * 0.8), WRIST_Y + 1 + rand() * 3, -2.4], Q0, flat(C.pus), 0.8);
}

// The second pair of thin arms, from under the shoulders, small pincers.
for (const sx of [-1, 1]) {
  chain('Chest', 'jacket', [[sx * 4.3, 20.5, -1.8], [sx * 5.4, 15, -3.4], [sx * 5.0, 9.5, -3.6], [sx * 4.4, 5.6, -3.0]], [0.75, 0.42], flesh, 12, 0.9);
  for (const o of [-1, 1]) chain('Chest', 'jacket', [[sx * 4.4, 5.7, -3.0], [sx * 4.4 + o * 0.6, 4.3, -3.3], [sx * 4.4 + o * 0.1, 3, -3.5]], [0.38, 0.12], flat(C.tip), 4, 1);
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { key: 'crab_king', c: {} }, ATLAS));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'crab_king'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'crab_king.png'), atlas);
  fs.writeFileSync(path.join(art, 'crab_king', 'crab_king.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'crab_king.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'crab_king.bbmodel'), JSON.stringify(kit.bbmodel('crab_king', PIECES, [{ key: 'crab_king' }], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Crab King:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES };
