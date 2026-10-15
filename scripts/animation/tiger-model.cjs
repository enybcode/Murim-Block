// A great tiger of the Murim (Blockbench free model, art/blockbench/tiger.bbmodel), after Enzo's reference: much bigger
// than a player, coming on with its head low. Still cubes, but sculpted round: every volume is a bevelled box (three
// crossed cubes, so no hard edge), and the body, the neck, the skull and the muzzle are rounded hulls built from slices
// that swell and narrow along the animal; the limbs are angled at their joints, the tail curls up in seven segments.
// A hump of shoulder muscle over a deep white chest, huge forelegs on wide white paws with round toes and long white
// claws, heavy haunches; a low broad head under a heavy scowling brow, pale cruel eyes, the white mask of the face,
// white cheek ruffs, a wrinkled snarling muzzle, the mouth shut on long fangs, laid-back ears with the white spot on
// their backs, whiskers. Old claw scars across the left eye and the left flank. The coat is painted from the 3D
// position of every texel (two per model pixel), the stripes measured round the body from the spine, so they wrap the
// hull, run on from one cube to the next, fork, thin towards the belly and ring the legs and the tail.
//
//   node scripts/animation/tiger-model.cjs        (writes art/blockbench/tiger.bbmodel and its texture)
const fs = require('node:fs');
const path = require('node:path');
const { png, stableId } = require('./mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const ATLAS = 1024, DENSITY = 2;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- building
// x across (+x the tiger's left), y up from the ground, z along (it faces north, -z). Units: model pixels.
const P = [];
let serial = 0;
const cube = (name, bone, part, from, to, extra = {}) => P.push({ name: `${name}#${serial++}`, bone, part, from, to, ...extra });
/** A bevelled box: three crossed cubes, each edge cut back by r (0: a plain cube). */
function box(name, bone, part, from, to, r = 'auto', extra = {}) {
  const [x0, y0, z0] = from, [x1, y1, z1] = to;
  const small = Math.min(x1 - x0, y1 - y0, z1 - z0);
  const k = r === 'auto' ? Math.min(2.5, Math.round(small * 0.3 * 2) / 2) : r;
  if (k <= 0 || small <= 2 * k) { cube(name, bone, part, from, to, extra); return; }
  cube(name, bone, part, [x0, y0 + k, z0 + k], [x1, y1 - k, z1 - k], extra);
  cube(name, bone, part, [x0 + k, y0, z0 + k], [x1 - k, y1, z1 - k], extra);
  cube(name, bone, part, [x0 + k, y0 + k, z0], [x1 - k, y1 - k, z1], extra);
}
/** Left and right: the box as given (+x, the left) and mirrored. */
function pair(name, bone, part, from, to, r = 'auto', extra = {}) {
  box(name + '_l', bone.replace('#', 'l'), part, from, to, r, extra);
  const flip = { ...extra };
  if (extra.pivot) flip.pivot = [-extra.pivot[0], extra.pivot[1], extra.pivot[2]];
  if (extra.rot && (extra.axis === 'y' || extra.axis === 'z')) flip.rot = -extra.rot;
  box(name + '_r', bone.replace('#', 'r'), part, [-to[0], from[1], from[2]], [-from[0], to[1], to[2]], r, flip);
}
/**
 * A rounded hull along z: sections [z, cy, rx, ry] (centre height, half-width, half-height), cut into slices about
 * `step` long, each a bevelled box of the size interpolated at its middle (bevel ~40 % of its smaller half-size).
 */
function hull(name, bone, part, sections, step = 3) {
  const at = z => {
    for (let i = 0; i < sections.length - 1; i++) {
      const [za, ...a] = sections[i], [zb, ...b] = sections[i + 1];
      if (z >= za && z <= zb) { const t = (z - za) / (zb - za), s = t * t * (3 - 2 * t) * 0.5 + t * 0.5; return a.map((v, k) => v + (b[k] - v) * s); }
    }
    return sections[z < sections[0][0] ? 0 : sections.length - 1].slice(1);
  };
  const z0 = sections[0][0], z1 = sections[sections.length - 1][0], n = Math.max(1, Math.round((z1 - z0) / step));
  for (let i = 0; i < n; i++) {
    const a = z0 + (z1 - z0) * i / n, b = z0 + (z1 - z0) * (i + 1) / n, [cy, rx, ry] = at((a + b) / 2);
    box(name, bone, part, [-rx, cy - ry, a], [rx, cy + ry, b], Math.round(Math.min(rx, ry) * 0.42 * 2) / 2);
  }
  return at;
}

// ---------------------------------------------------------------- the tiger
// The body: chest swelling into the shoulders, a waist, the hips; the hump of the shoulder blades on top.
const BODY = [[-31, 31, 8, 9], [-25, 31.5, 11.5, 12.5], [-16, 31.5, 11.5, 12.5], [-7, 31, 10, 10.5], [2, 31, 9.5, 9.8],
  [11, 31.5, 10.5, 10.5], [19, 32, 11, 10.5], [26, 32, 8.5, 8.5], [30, 32, 5, 5]];
const bodyAt = hull('body', 'body', 'coat', BODY);
pair('blade', 'body', 'coat', [2, 39, -26], [9.5, 45, -12]);
pair('deltoid', 'body', 'coat', [8.5, 24, -27], [13, 37, -12]);
// The neck and its ruffs, the head: skull, muzzle, cheeks, brows, eyes, nose, flews, teeth, ears, whiskers.
const NECK = [[-41, 29.5, 8, 8.5], [-37, 30, 8.5, 9], [-31, 31, 8.5, 9.5]];
const neckAt = hull('neck', 'head', 'coat', NECK);
box('dewlap', 'head', 'belly', [-6.5, 19, -40], [6.5, 25, -29]);
const SKULL = [[-51, 29.8, 8.4, 7.2], [-48, 30.4, 10.8, 8.6], [-44, 30.8, 11.8, 9], [-40, 30.4, 10.6, 8.6]];
const skullAt = hull('skull', 'head', 'skull', SKULL, 2.5);
const MUZZLE = [[-59, 25.4, 5.2, 3.6], [-56, 25.7, 6.4, 4.3], [-52, 26, 7.2, 4.9], [-49.5, 26.4, 7.6, 5.1]];
hull('muzzle', 'head', 'muzzle', MUZZLE, 2.5);
pair('cheek', 'head', 'cheek', [9.5, 21, -49], [13.4, 28.6, -41.5]);
pair('brow', 'head', 'brow', [0.5, 32.4, -52.6], [9.2, 34.8, -49.4], 0.5, { rot: 12, axis: 'z', pivot: [0.5, 32.4, -50.9] });
pair('eye', 'head', 'eye', [2.4, 30, -51.9], [7.6, 32.4, -50.4], 0);
box('snarl', 'head', 'snarl', [-4, 28.6, -57], [4, 30, -50.5], 0.5);
box('nose', 'head', 'nose', [-2.8, 26.8, -60], [2.8, 29.6, -57], 0.6);
pair('flew', 'head', 'flew', [1.4, 20.6, -59], [6.4, 23.6, -50], 0.8);
box('incisors', 'head', 'fang', [-1.6, 21.2, -59.4], [1.6, 22.6, -58.4], 0);
pair('fang', 'head', 'fang', [2.3, 17.6, -58.8], [3.7, 22.4, -57.4], 0.4);
pair('ear', 'head', 'ear', [4, 37, -45], [9.6, 42.6, -42], 0.8, { rot: 16, pivot: [6.8, 37, -43.5] });
const JAW = { rot: -3, pivot: [0, 21, -46] };
box('jaw', 'jaw', 'jaw', [-5.4, 17, -57.5], [5.4, 21, -45], 'auto', JAW);
for (const [k, y, a] of [[1, 26.6, 14], [2, 25.6, 4], [3, 24.6, -7], [4, 23.8, -16]])
  pair('whisker' + k, 'head', 'whisker', [6.2, y, -55.3], [19, y + 0.3, -55], 0, { rot: a, axis: 'y', pivot: [6.2, y, -55.2] });
// Front legs: the upper leg slanting back to the elbow, the forearm forward to the wrist, a wide paw, round toes, claws.
pair('front_upper', 'front_leg_#', 'leg', [4.2, 16, -27], [12.8, 41, -13], 'auto', { rot: 6, pivot: [8.5, 34, -20] });
pair('elbow', 'front_leg_#', 'leg', [5.2, 15, -16], [11.8, 22, -10.5]);
pair('forearm', 'front_foot_#', 'leg_low', [4.6, 4, -26], [12.4, 19, -15], 'auto', { rot: -5, pivot: [8.5, 16, -20] });
pair('front_paw', 'front_foot_#', 'paw', [3.8, 0, -29], [13.2, 5, -14]);
// Hind legs: the haunch, the shank slanting back to the hock, the hock down to the paw.
pair('thigh', 'hind_leg_#', 'leg', [4.6, 19, 7], [12.6, 43, 25]);
pair('shank', 'hind_foot_#', 'leg', [5.4, 8, 15], [11.4, 24, 23], 'auto', { rot: 20, pivot: [8.4, 20, 19] });
pair('hock', 'hind_foot_#', 'leg_low', [5.6, 3, 18], [10.8, 12, 24]);
pair('hind_paw', 'hind_foot_#', 'paw', [4.8, 0, 11.5], [11.6, 4, 24.5]);
for (const [leg, z, x0, step, w, h, len] of [['front', -29, 3.9, 2.4, 2.2, 4.2, 4], ['hind', 11.5, 4.9, 1.7, 1.6, 3.4, 3.4]])
  for (let i = 0; i < 4; i++) {
    const x = x0 + i * step;
    pair(`${leg}_toe${i}`, leg + '_foot_#', 'toe', [x, 0, z - len], [x + w, h, z + 1], 0.6);
    pair(`${leg}_claw${i}`, leg + '_foot_#', 'claw', [x + w * 0.28, 0, z - len - 1.8], [x + w * 0.72, 1.6, z - len + 0.2], 0);
  }
// Tail: seven segments curling up and back from the hips, thinning, the last black.
const TAIL = [[0, 33, 29], [0, 33.6, 34], [0, 35.4, 38.6], [0, 38.6, 42.2], [0, 42.8, 44.4], [0, 47.4, 45], [0, 51.6, 44], [0, 54.8, 41.8]];
for (let i = 0; i < TAIL.length - 1; i++) {
  const [, ya, za] = TAIL[i], [, yb, zb] = TAIL[i + 1], th = 2.4 - i * 0.18;
  const len = Math.hypot(yb - ya, zb - za), ang = Math.atan2(yb - ya, zb - za) * 180 / Math.PI;
  box('tail' + i, 'tail' + i, i === TAIL.length - 2 ? 'tail_tip' : 'tail', [-th, ya - th, za], [th, ya + th, za + len + 0.8], th > 1.6 ? 0.8 : 0.5,
    { rot: -ang, pivot: [0, ya, za], ord: i });
}

const LIFT = 5;
for (const p of P) {
  p.orig = { from: p.from, to: p.to };
  if (p.bone.includes('_leg') || p.bone.includes('_foot')) continue;
  p.from = [p.from[0], p.from[1] + LIFT, p.from[2]]; p.to = [p.to[0], p.to[1] + LIFT, p.to[2]];
  if (p.pivot) p.pivot = [p.pivot[0], p.pivot[1] + LIFT, p.pivot[2]];
  p.lift = LIFT;
}

// ---------------------------------------------------------------- the coat
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const C = {
  deep: hex('#8A3E12'), orange: hex('#D2701F'), pale: hex('#E8A256'), cream: hex('#EDDCC0'), white: hex('#F4EFE6'),
  shade: hex('#D9D2C6'), black: hex('#120D0A'), brown: hex('#33201A'), nose: hex('#A85E52'), noseDark: hex('#4A2420'),
  eye: hex('#F3DCC6'), iris: hex('#D46B4E'), pupil: hex('#0A0605'), rim: hex('#1C0C08'), gum: hex('#7E2C34'),
  claw: hex('#F6F2E8'), clawBase: hex('#BDB6A8'), fang: hex('#F6F1E2'), fangRoot: hex('#D9C9A2'),
  scar: hex('#9A5A52'), scarLight: hex('#C99088'), pad: hex('#2A1C18'),
};
const mix = (a, b, t) => a.map((v, i) => v + (b[i] - v) * Math.max(0, Math.min(1, t)));
const frac = x => x - Math.floor(x);
const hash = (x, y) => { const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453; return s - Math.floor(s); };
function vnoise(x, y) {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi, s = t => t * t * (3 - 2 * t);
  const a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
  return a + (b - a) * s(xf) + (c - a) * s(yf) + (a - b - c + d) * s(xf) * s(yf);
}
const grain = (c, x, y, z) => c.map(v => v * (0.9 + 0.16 * hash(Math.round(x * 2) + z * 0.7, Math.round(y * 2) + z * 1.3)));
function bodyStripe(z, d) {
  const t = z * 0.17 + 0.85 * Math.sin(d * 0.26 + z * 0.12) + 0.8 * vnoise(d * 0.2 + 3, z * 0.17);
  const width = 0.42 - d * 0.019;
  if (width <= 0.05) return false;
  const f = frac(t);
  if (f < width) return !(vnoise(d * 0.5, z * 0.4 + 9) > 0.76 && f > width * 0.4 && f < width * 0.62);
  return frac(t + 0.5) < width * 0.3 && vnoise(z * 0.35, d * 0.35) > 0.6;
}
const scarred = (u, v, offsets, slope) => offsets.some(o => Math.abs(v - (o - u * slope)) < 0.34);
/** Round a hull: the angle from the spine (0 on top, pi underneath) and the distance along the coat from the spine. */
function round(at, x, y, z) {
  const [cy, rx, ry] = at(z), th = Math.atan2(Math.abs(x) / rx, (y - cy) / ry);
  return { th, d: th * (rx + ry) / 2 };
}

function paint(part, f, x, y, z, piece) {
  const ax = Math.abs(x);
  switch (part) {
    case 'coat': {
      const head = piece.bone === 'head';
      const { th, d } = round(head ? neckAt : bodyAt, x, y, z);
      if (th > (head ? 1.85 : 2.25) || (z < -27 && ax < 7 && y < 33) || f === 'down') return grain(C.white, x, y, z);
      if (!head && x > 0 && z > -6 && z < 9 && scarred(z, y, [35, 37.4, 39.8], 0.55) && y > 27 && y < 37) return grain(z % 2 < 1 ? C.scar : C.scarLight, x, y, z);
      if (bodyStripe(z, d)) return grain(C.black, x, y, z);
      return grain(d < 4 ? mix(C.deep, C.orange, d / 4) : d < 12 ? mix(C.orange, C.pale, (d - 4) / 8) : mix(C.pale, C.cream, (d - 12) / 4), x, y, z);
    }
    case 'belly': return grain(y < 20 ? C.shade : C.white, x, y, z);
    case 'leg': case 'leg_low': {
      const inner = (f === 'east' && x < 0) || (f === 'west' && x > 0) || (f === 'south' && part === 'leg_low');
      if (inner || f === 'down') return grain(C.white, x, y, z);
      if (part === 'leg_low' && f === 'north' && y < 12) return grain(C.white, x, y, z);
      const t = y * 0.21 + 0.55 * Math.sin(z * 0.4 + ax * 0.4) + 0.5 * vnoise(z * 0.3, y * 0.22);
      if (y > 6 && frac(t) < (part === 'leg' ? 0.3 : 0.2)) return grain(C.black, x, y, z);
      return grain(part === 'leg' ? mix(C.orange, C.pale, (43 - y) / 25) : mix(C.pale, C.white, (19 - y) / 10), x, y, z);
    }
    case 'paw': {
      if (f !== 'down') return grain(C.white, x, y, z);
      // Under the paw: white fur round one broad dark pad.
      const mx = (piece.from[0] + piece.to[0]) / 2, mz = (piece.from[2] + piece.to[2]) / 2 + 1;
      return Math.abs(x - mx) < (piece.to[0] - piece.from[0]) * 0.28 && Math.abs(z - mz) < (piece.to[2] - piece.from[2]) * 0.2 ? grain(C.pad, x, y, z) : grain(C.shade, x, y, z);
    }
    case 'toe': {
      if (f !== 'down') return grain(y < 1.6 && f !== 'up' ? C.shade : C.white, x, y, z);
      const mx = (piece.from[0] + piece.to[0]) / 2, mz = (piece.from[2] + piece.to[2]) / 2;
      return Math.hypot(x - mx, (z - mz) * 0.7) < 0.75 ? grain(C.pad, x, y, z) : grain(C.shade, x, y, z);     // a toe bean
    }
    case 'claw': return y < 0.5 || z > piece.to[2] - 0.5 ? C.clawBase : C.claw;
    case 'skull': {
      const { th } = round(skullAt, x, y, z);
      if (z < -49.5) {
        if (x > 0 && y > 26 && scarred(x, y, [38.4, 40, 41.6], 1.15)) return grain(C.scar, x, y, z);
        if (y > 33.6) {
          if (ax < 0.55) return grain(C.black, x, y, z);
          if (Math.abs(ax - (2 + (y - 33.6) * 0.6)) < 0.42) return grain(C.black, x, y, z);
          if (Math.abs(ax - (4.6 + (y - 33.6) * 0.9)) < 0.46) return grain(C.black, x, y, z);
        }
        if (y < 29.6) return grain(C.white, x, y, z);
        return grain(mix(C.orange, C.deep, (y - 30) / 6), x, y, z);
      }
      if (y < 27.5 && th > 1.2) return grain(C.white, x, y, z);
      const t = (th < 0.6 ? z * 0.32 : (z * 0.4 + y * 0.6) * 0.36) + 0.6 * Math.sin(x * 0.4);
      if (frac(t) < 0.28 && ax > 1) return grain(C.black, x, y, z);
      return grain(th < 0.7 ? C.deep : C.orange, x, y, z);
    }
    case 'cheek': case 'ruff': {
      if (frac(z * 0.32 + y * 0.27) < 0.16 && y > 23) return grain(C.black, x, y, z);
      return grain(C.white, x, y, z);
    }
    case 'brow': {
      if (f === 'down' || (f === 'north' && y < piece.from[1] + 0.9)) return grain(C.white, x, y, z);
      if (f === 'north') return grain(ax < 2.2 ? C.black : C.deep, x, y, z);
      return grain(frac(x * 0.45) < 0.4 ? C.black : C.deep, x, y, z);
    }
    case 'eye': {
      if (f !== 'north') return C.rim;
      const ex = ax - 5, ey = y - (31.2 + ex * 0.42);
      if (Math.abs(ey) > 0.95 || Math.abs(ex) > 2.5) return ey > 0 ? C.black : grain(C.white, x, y, z);
      if (x > 0 && scarred(x, y, [38.4, 40], 1.15)) return C.scar;
      if (Math.abs(ey) > 0.68 || Math.abs(ex) > 2.2) return C.rim;
      if (Math.abs(ex + 0.4) < 0.36 && Math.abs(ey) < 0.52) return C.pupil;
      if (Math.abs(ex + 0.4) < 1) return C.iris;
      return C.eye;
    }
    case 'muzzle': {
      if (y > 29.4 && !(f === 'north' && z < -58.5)) return grain(ax < 1 ? C.pale : C.orange, x, y, z);
      if (f === 'north' || z < -58) {
        if (ax < 0.4 && y < 27) return C.black;
        if (y < 22.4) return C.black;
        if (y < 26.4 && y > 23.6 && ax > 1.4 && ax < 5 && (Math.round(x * 2) + Math.round(y * 2)) % 3 === 0) return C.brown;
        if (x > 0 && scarred(x, y, [30.4, 32], 1.15)) return C.scar;
        return grain(C.white, x, y, z);
      }
      if (y < 22.4) return C.black;
      if (frac(z * 0.5 + y * 0.4) < 0.2 && y > 26.8) return grain(C.black, x, y, z);
      return grain(y > 27.8 ? C.orange : C.white, x, y, z);
    }
    case 'snarl': return grain(frac(z * 0.75) < 0.42 ? C.black : C.deep, x, y, z);
    case 'nose': return (f === 'north' || f === 'up') && ax < 2 && y > 27.6 ? grain(C.nose, x, y, z) : C.noseDark;
    case 'flew': return y < 21.2 || f === 'down' ? C.black : grain(C.white, x, y, z);
    case 'fang': return y < piece.to[1] - 1.4 || piece.name.startsWith('incisors') ? grain(C.fang, x, y, z) : C.fangRoot;
    case 'jaw': return f === 'up' ? grain(C.gum, x, y, z) : f === 'north' && y > 20.4 ? C.black : grain(y < 18.4 ? C.shade : C.white, x, y, z);
    case 'ear': {
      const cx = (piece.from[0] + piece.to[0]) / 2;
      if (f === 'north') {
        if (y > piece.to[1] - 0.8 || Math.abs(x - cx) > (piece.to[0] - piece.from[0]) / 2 - 0.6) return C.black;
        return y < piece.from[1] + 1.6 ? grain(C.cream, x, y, z) : grain(C.orange, x, y, z);
      }
      if (f === 'south') return Math.hypot(x - cx, y - 38.6) < 1.3 ? C.white : C.black;
      return y > piece.to[1] - 1 ? C.black : grain(C.orange, x, y, z);
    }
    case 'whisker': return C.white;
    case 'tail': case 'tail_tip': {
      const along = piece.ord + (z - piece.from[2]) / Math.max(1, piece.to[2] - piece.from[2]);
      if (part === 'tail_tip') return grain(C.black, x, y, z);
      if (frac(along * 1.1 + 0.2) < 0.4) return grain(C.black, x, y, z);
      return grain(f === 'down' ? C.white : C.orange, x, y, z);
    }
  }
  return [255, 0, 255];
}

// ---------------------------------------------------------------- packing and painting
const dims = (p, f) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, r = d => Math.max(1, Math.round(d * DENSITY));
  if (f === 'north' || f === 'south') return [r(x1 - x0), r(y1 - y0)];
  if (f === 'east' || f === 'west') return [r(z1 - z0), r(y1 - y0)];
  return [r(x1 - x0), r(z1 - z0)];
};
function point(p, f, u, v, w, h) {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  const a = (u + 0.5) / w, b = (v + 0.5) / h, Y = y1 - b * (y1 - y0);
  switch (f) {
    case 'north': return [x1 - a * (x1 - x0), Y, z0];
    case 'south': return [x0 + a * (x1 - x0), Y, z1];
    case 'east': return [x1, Y, z1 - a * (z1 - z0)];
    case 'west': return [x0, Y, z0 + a * (z1 - z0)];
    case 'up': return [x0 + a * (x1 - x0), y1, z0 + b * (z1 - z0)];
    default: return [x0 + a * (x1 - x0), y0, z1 - b * (z1 - z0)];
  }
}
/** A face wholly inside another unrotated cube of the same bone (the hidden faces of a bevelled box) gets no texels. */
function hidden(p, f) {
  if (p.rot) return false;
  const e = 0.01, plane = { north: [2, p.from[2]], south: [2, p.to[2]], east: [0, p.to[0]], west: [0, p.from[0]], up: [1, p.to[1]], down: [1, p.from[1]] }[f];
  return P.some(q => q !== p && q.bone === p.bone && !q.rot && [0, 1, 2].every(k => k === plane[0]
    ? q.from[k] < plane[1] - e && q.to[k] > plane[1] + e
    : q.from[k] <= p.from[k] + e && q.to[k] >= p.to[k] - e));
}
const faces = [];
for (const p of P) for (const f of FACES) {
  if (hidden(p, f)) { (p.uv ||= {})[f] = null; continue; }
  const [w, h] = dims(p, f); faces.push({ p, f, w, h });
}
faces.sort((a, b) => b.h - a.h || b.w - a.w);
let cx = 0, cy = 0, shelf = 0;
for (const face of faces) {
  if (cx + face.w > ATLAS) { cx = 0; cy += shelf; shelf = 0; }
  face.u = cx; face.v = cy; cx += face.w; shelf = Math.max(shelf, face.h);
}
if (cy + shelf > ATLAS) throw new Error('the atlas is full: ' + (cy + shelf));
const img = new Uint8Array(ATLAS * ATLAS * 4);
for (const face of faces) {
  (face.p.uv ||= {})[face.f] = [face.u, face.v, face.u + face.w, face.v + face.h];
  for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
    const [x, y, z] = point(face.p, face.f, u, v, face.w, face.h);
    const c = paint(face.p.part, face.f, x, y - (face.p.lift || 0), z, { ...face.p, from: face.p.orig.from, to: face.p.orig.to }).map(n => Math.max(0, Math.min(255, Math.round(n))));
    img.set([...c, 255], ((face.v + v) * ATLAS + face.u + u) * 4);
  }
}

// ---------------------------------------------------------------- the project
const elements = P.map(p => ({
  name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
  from: p.from, to: p.to, autouv: 0, color: 0, origin: p.pivot || p.from.map((v, k) => (v + p.to[k]) / 2),
  rotation: p.rot ? (p.axis === 'y' ? [0, p.rot, 0] : p.axis === 'z' ? [0, 0, p.rot] : [p.rot, 0, 0]) : [0, 0, 0],
  faces: Object.fromEntries(FACES.map(f => [f, p.uv[f] ? { uv: p.uv[f], texture: 0 } : { uv: [0, 0, 0, 0], texture: null }])),
  type: 'cube', uuid: stableId('tiger/' + p.name),
}));
// The joints: shoulders and hips where the legs meet the (lifted) body, elbows, knees, the neck, the jaw, the tail's.
const PIVOTS = { body: [0, 37, 0], head: [0, 37, -33], jaw: [0, 26, -46],
  front_leg_l: [8.5, 39, -20], front_leg_r: [-8.5, 39, -20], front_foot_l: [8.5, 18, -19], front_foot_r: [-8.5, 18, -19],
  hind_leg_l: [8.6, 39, 16], hind_leg_r: [-8.6, 39, 16], hind_foot_l: [8.4, 22, 19], hind_foot_r: [-8.4, 22, 19] };
TAIL.forEach(([, y, z], i) => { PIVOTS['tail' + i] = [0, y + LIFT, z]; });
const group = (name, children) => ({ name, origin: PIVOTS[name] || [0, 0, 0], color: 0, uuid: stableId('tiger/bone/' + name), export: true,
  mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0,
  children: [...elements.filter((e, i) => P[i].bone === name).map(e => e.uuid), ...children] });
const tail = i => group('tail' + i, i < TAIL.length - 2 ? [tail(i + 1)] : []);
const leg = (name, foot) => group(name, [group(foot, [])]);
const outliner = [group('body', [group('head', [group('jaw', [])]), leg('front_leg_l', 'front_foot_l'), leg('front_leg_r', 'front_foot_r'),
  leg('hind_leg_l', 'hind_foot_l'), leg('hind_leg_r', 'hind_foot_r'), tail(0)])];
const texture = png(ATLAS, ATLAS, img);
const project = {
  meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: 'tiger', model_identifier: '', visible_box: [1, 1, 0],
  variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
  resolution: { width: ATLAS, height: ATLAS }, elements, outliner,
  textures: [{ path: '', name: 'tiger.png', folder: 'entity', namespace: 'murimblock', id: '0', width: ATLAS, height: ATLAS, uv_width: ATLAS,
    uv_height: ATLAS, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default',
    render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
    saved: false, uuid: stableId('tiger/texture'), source: 'data:image/png;base64,' + texture.toString('base64') }],
};
fs.mkdirSync(path.join(ROOT, 'art', 'blockbench'), { recursive: true });
const file = path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel');
if (fs.existsSync(file)) { const old = JSON.parse(fs.readFileSync(file, 'utf8')); if (old.animations) project.animations = old.animations; }
fs.writeFileSync(file, JSON.stringify(project));
fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.png'), texture);
console.log('tiger:', P.length, 'cubes,', faces.length, 'textured faces, atlas', ATLAS, 'used to row', cy + shelf);
