// A great tiger, in cubes (Blockbench free model, art/blockbench/tiger.bbmodel), after Enzo's reference: a beast of the
// Murim, far bigger than any tiger, coming on with its head low. A hump of shoulder muscle over a deep white chest,
// huge forelegs on wide white paws with long white claws, heavy haunches, a spine ridge; a low broad head under a heavy
// scowling brow, pale cruel eyes, the white mask of the face, a wrinkled snarling muzzle, the mouth shut on long fangs
// that show past the lips, flews, a ruff of white fur, laid-back ears with the white spot on their backs, whiskers; a
// tail lashing up. Old claw scars across the left eye and the left flank. Two texels per model pixel; the coat is
// painted from the 3D position of every texel, so the broad black stripes run on from one cube to the next, fork, thin
// towards the belly, cross the spine and ring the legs and the tail.
//
//   node scripts/animation/tiger-model.cjs        (writes art/blockbench/tiger.bbmodel and its texture)
const fs = require('node:fs');
const path = require('node:path');
const { png, stableId } = require('./mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const ATLAS = 512, DENSITY = 2;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the pieces
// x across (+x the tiger's left), y up from the ground, z along (the tiger faces north, -z). Units: model pixels.
// part: how it is painted; bone: its group ('#' becomes l or r); rot/axis/pivot: a tilt (about x unless axis is y or z).
const P = [];
const add = (name, bone, part, from, to, extra = {}) => P.push({ name, bone, part, from, to, ...extra });
const mirror = (name, bone, part, from, to, extra = {}) => {
  add(name + '_l', bone.replace('#', 'l'), part, from, to, extra);
  const flip = { ...extra };
  if (extra.pivot) flip.pivot = [-extra.pivot[0], extra.pivot[1], extra.pivot[2]];
  if (extra.rot && (extra.axis === 'y' || extra.axis === 'z')) flip.rot = -extra.rot;
  add(name + '_r', bone.replace('#', 'r'), part, [-to[0], from[1], from[2]], [-from[0], to[1], to[2]], flip);
};

// Body: chest, the hump of the shoulders, barrel, belly, hips, haunches, the spine ridge, the neck and its white front.
add('chest', 'body', 'coat', [-10.5, 18, -28], [10.5, 37, -8]);
add('breast', 'body', 'belly', [-7, 15.5, -30], [7, 30, -27]);
mirror('shoulder', 'body', 'coat', [6.5, 30, -27], [12.5, 42, -11]);
mirror('deltoid', 'body', 'coat', [10, 20, -27.5], [13.5, 33, -13]);
add('withers', 'body', 'coat', [-6.5, 37, -26], [6.5, 42, -11]);
add('barrel', 'body', 'coat', [-9, 20, -9], [9, 35.5, 12]);
add('belly', 'body', 'belly', [-7, 17.6, -8], [7, 20, 10]);
add('hips', 'body', 'coat', [-9.5, 21, 11], [9.5, 37, 24]);
mirror('haunch', 'body', 'coat', [8.5, 16, 10], [12.5, 34, 25]);
add('ridge', 'body', 'ridge', [-1.6, 35.5, -9], [1.6, 37, 21]);
add('ridge_front', 'body', 'ridge', [-2, 42, -24], [2, 43.2, -13]);
add('neck', 'head', 'coat', [-8.5, 19, -37], [8.5, 34, -26]);
add('dewlap', 'head', 'belly', [-6.5, 15.5, -37], [6.5, 19, -27]);
mirror('ruff', 'head', 'ruff', [8, 17.5, -41], [11.6, 30, -28.5]);
// Head, low and forward: skull, cheeks, the heavy brows knotted in anger, the muzzle with its snarl, nose, flews,
// the teeth, a dark gap behind them, ears laid back.
add('skull', 'head', 'skull', [-10, 19, -50], [10, 35, -37]);
mirror('eye', 'head', 'eye', [2.4, 28, -51.6], [7.8, 30.6, -50]);
mirror('cheek', 'head', 'cheek', [9.4, 18.5, -48], [12.6, 28, -39]);
mirror('brow', 'head', 'brow', [0.6, 30.4, -51.4], [9.6, 33, -49.4], { rot: 12, axis: 'z', pivot: [0.6, 30.4, -50.4] });
add('muzzle', 'head', 'muzzle', [-6.5, 21, -58], [6.5, 27.8, -49.5]);
add('snarl', 'head', 'snarl', [-4.6, 27.8, -56], [4.6, 28.8, -50]);
add('nose', 'head', 'nose', [-2.8, 25.8, -59], [2.8, 28.4, -56.2]);
mirror('flew', 'head', 'flew', [1.6, 18.6, -58], [6.7, 21.2, -49.5]);
add('incisors', 'head', 'fang', [-1.6, 19.6, -58.2], [1.6, 21, -57.4]);
mirror('fang', 'head', 'fang', [2.2, 14.6, -57.8], [3.8, 20.6, -56.2]);
mirror('ear', 'head', 'ear', [4, 34.6, -43], [9, 39, -40.8], { rot: 16, pivot: [6.5, 34.6, -41.9] });
// The jaw, shut but for a snarl's gap: chin and the lower fangs, on their own bone.
const JAW = { rot: -3, pivot: [0, 18.6, -46] };
add('jaw', 'jaw', 'jaw', [-5.4, 14.6, -56.6], [5.4, 18.6, -44], JAW);
add('chin', 'jaw', 'jaw', [-4.2, 13.6, -55.4], [4.2, 14.6, -46], JAW);
// Whiskers: long thin fans off each side of the muzzle.
for (const [k, y, a] of [[1, 25.6, 14], [2, 24.6, 4], [3, 23.6, -7], [4, 22.8, -16]])
  mirror('whisker' + k, 'head', 'whisker', [6.4, y, -54.3], [18, y + 0.3, -54], { rot: a, axis: 'y', pivot: [6.4, y, -54.2] });
// Front legs, huge: upper (with the elbow), forearm, a wide white paw, four toes, four long white claws.
mirror('front_upper', 'front_leg_#', 'leg', [4.2, 15, -27], [12.5, 36, -14]);
mirror('elbow', 'front_leg_#', 'leg', [5.6, 14, -14.5], [11.5, 21, -11.5]);
mirror('forearm', 'front_leg_#', 'leg_low', [4.6, 4, -26], [12.5, 16, -15]);
mirror('front_paw', 'front_leg_#', 'paw', [3.6, 0, -30], [13.4, 4.5, -14]);
// Hind legs: thigh, hock, paw, toes, claws.
mirror('thigh', 'hind_leg_#', 'leg', [5, 14, 12], [11, 31, 24]);
mirror('hock', 'hind_leg_#', 'leg_low', [5.4, 3, 18.5], [10.4, 15.5, 24]);
mirror('hind_paw', 'hind_leg_#', 'paw', [4.6, 0, 12.5], [11.4, 3.6, 24.5]);
for (const [leg, z, x0, step, w] of [['front', -30, 3.9, 2.4, 2.1], ['hind', 12.5, 4.8, 1.65, 1.5]]) for (let i = 0; i < 4; i++) {
  const x = x0 + i * step;
  mirror(`${leg}_toe${i}`, leg + '_leg_#', 'toe', [x, 0, z - 3], [x + w, leg === 'front' ? 3.6 : 3, z]);
  mirror(`${leg}_claw${i}`, leg + '_leg_#', 'claw', [x + w * 0.25, 0, z - (leg === 'front' ? 5.4 : 4.4)], [x + w * 0.75, leg === 'front' ? 1.8 : 1.4, z - 3]);
}
// Tail: five segments lashing up and back, the last black.
add('tail1', 'tail', 'tail', [-2.6, 31.5, 23], [2.6, 36.5, 32], { rot: -18, pivot: [0, 34, 23] });
add('tail2', 'tail', 'tail', [-2.3, 34.6, 30.5], [2.3, 39, 38], { rot: -38, pivot: [0, 36.8, 30.5] });
add('tail3', 'tail', 'tail', [-2.1, 40, 34.5], [2.1, 44, 41.5], { rot: -62, pivot: [0, 42, 34.5] });
add('tail4', 'tail', 'tail', [-1.9, 46.5, 36], [1.9, 50.2, 42], { rot: -30, pivot: [0, 48.3, 36] });
add('tail5', 'tail', 'tail_tip', [-1.7, 50.6, 39.6], [1.7, 54, 45], { rot: 10, pivot: [0, 52.3, 39.6] });

// Everything but the legs stands LIFT higher: painted as if it did not (the colours keep their heights).
const LIFT = 7;
for (const p of P) {
  p.orig = { from: p.from, to: p.to };
  if (p.bone.includes('_leg')) continue;
  p.from = [p.from[0], p.from[1] + LIFT, p.from[2]]; p.to = [p.to[0], p.to[1] + LIFT, p.to[2]];
  if (p.pivot) p.pivot = [p.pivot[0], p.pivot[1] + LIFT, p.pivot[2]];
  p.lift = LIFT;
}

// ---------------------------------------------------------------- the coat
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const C = {
  deep: hex('#8A3E12'), orange: hex('#D2701F'), pale: hex('#E8A256'), cream: hex('#EDDCC0'), white: hex('#F4EFE6'),
  shade: hex('#D9D2C6'), black: hex('#120D0A'), brown: hex('#33201A'), nose: hex('#A85E52'), noseDark: hex('#4A2420'),
  eye: hex('#F3DCC6'), iris: hex('#D46B4E'), pupil: hex('#0A0605'), rim: hex('#1C0C08'), throat: hex('#2E0C0D'),
  gum: hex('#7E2C34'), claw: hex('#F6F2E8'), clawBase: hex('#BDB6A8'), fang: hex('#F6F1E2'), fangRoot: hex('#D9C9A2'),
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
/** Fur grain: strands of light and dark, texel by texel. */
const grain = (c, x, y, z) => c.map(v => v * (0.9 + 0.16 * hash(Math.round(x * 2) + z * 0.7, Math.round(y * 2) + z * 1.3)));

/** Body stripes: broad wavy bands across the body (along z), widest at the spine, forking, gone by the belly. */
function bodyStripe(z, d) {
  const t = z * 0.18 + 0.85 * Math.sin(d * 0.28 + z * 0.12) + 0.8 * vnoise(d * 0.22 + 3, z * 0.18);
  const width = 0.4 - d * 0.02;
  if (width <= 0.05) return false;
  const f = frac(t);
  if (f < width) return !(vnoise(d * 0.5, z * 0.4 + 9) > 0.76 && f > width * 0.4 && f < width * 0.62);
  return frac(t + 0.5) < width * 0.3 && vnoise(z * 0.35, d * 0.35) > 0.6;
}
/** Claw scars: three diagonal lines. */
const scarred = (u, v, offsets, slope) => offsets.some(o => Math.abs(v - (o - u * slope)) < 0.32);

function paint(part, f, x, y, z, piece) {
  const ax = Math.abs(x);
  switch (part) {
    case 'coat': {
      const top = piece.to[1];
      const d = f === 'up' ? ax * 0.8 : f === 'down' ? 20 : (top - y) + Math.max(0, 42 - top) * (piece.name.startsWith('haunch') ? 0.5 : 1)
        + (f === 'north' || f === 'south' ? ax * 0.3 : 0);
      const bellyLine = piece.name === 'neck' ? 25 : 23.5;
      if (f === 'down' || y < bellyLine || (f === 'north' && (piece.name === 'chest' || piece.name === 'neck') && ax < 6.5 && y < 32)) return grain(C.white, x, y, z);
      if (f === 'east' && piece.name === 'barrel' && scarred(z, y, [33, 35.4, 37.8], 0.55) && y > 24 && z > -6 && z < 8) return grain(z % 2 ? C.scar : C.scarLight, x, y, z);
      if (bodyStripe(z, d)) return grain(C.black, x, y, z);
      return grain(d < 4 ? mix(C.deep, C.orange, d / 4) : d < 12 ? mix(C.orange, C.pale, (d - 4) / 8) : mix(C.pale, C.cream, (d - 12) / 3), x, y, z);
    }
    case 'ridge': return grain(frac(z * 0.2) < 0.45 ? C.black : C.deep, x, y, z);
    case 'belly': return grain(f === 'up' ? C.cream : y < 18 ? C.shade : C.white, x, y, z);
    case 'leg': case 'leg_low': {
      const inner = (f === 'east' && x < 0) || (f === 'west' && x > 0) || (f === 'south' && part === 'leg_low');
      if (inner || f === 'down') return grain(C.white, x, y, z);
      if (part === 'leg_low' && f === 'north' && y < 11) return grain(C.white, x, y, z);   // white fronts of the forearms
      const t = y * 0.22 + 0.55 * Math.sin(z * 0.45 + ax * 0.4) + 0.5 * vnoise(z * 0.3, y * 0.22);
      const width = part === 'leg' ? 0.3 : 0.2;
      if (y > 5 && frac(t) < width) return grain(C.black, x, y, z);
      return grain(part === 'leg' ? mix(C.orange, C.pale, (36 - y) / 22) : mix(C.pale, C.white, (16 - y) / 10), x, y, z);
    }
    case 'paw': return grain(f === 'down' ? C.pad : C.white, x, y, z);
    case 'toe': return grain(f === 'down' ? C.pad : (f === 'east' || f === 'west') && y < 2.6 ? C.shade : C.white, x, y, z);
    case 'claw': return y < 0.5 || z > piece.to[2] - 0.4 ? C.clawBase : C.claw;
    case 'skull': {
      if (f === 'north') {
        // Eyes slanted in anger: pale whites, a reddish iris, a small pupil, black rims; the left crossed by the scars.
        const ex = ax - 5, ey = y - (29.6 + (ax - 5) * 0.3);
        if (Math.abs(ex) < 2.2 && Math.abs(ey) < 0.95) {
          if (x > 0 && scarred(x, y, [37, 38.6, 40.2], 1.15)) return C.scar;
          if (Math.abs(ex) > 1.9 || Math.abs(ey) > 0.7) return C.rim;
          if (Math.abs(ex + 0.3) < 0.32 && Math.abs(ey) < 0.45) return C.pupil;
          if (Math.abs(ex + 0.3) < 0.85) return C.iris;
          return C.eye;
        }
        if (Math.abs(ex) < 2.6 && ey > -2 && ey <= -0.95) return grain(C.white, x, y, z);               // white under the eye
        if (x > 0 && y > 25 && scarred(x, y, [37, 38.6, 40.2], 1.15)) return grain(C.scar, x, y, z);
        if (y > 32) {                                                                                     // the forehead's marks
          if (ax < 0.55 && y > 32.4) return grain(C.black, x, y, z);
          if (Math.abs(ax - (1.8 + (y - 32) * 0.6)) < 0.4) return grain(C.black, x, y, z);
          if (Math.abs(ax - (4.4 + (y - 32) * 0.9)) < 0.45) return grain(C.black, x, y, z);
          if (Math.abs(ax - (7.2 + (y - 32) * 0.6)) < 0.45) return grain(C.black, x, y, z);
        }
        if (ax > 6.6 && y < 28.5) return grain(C.white, x, y, z);
        return grain(mix(C.orange, C.deep, (y - 27) / 7), x, y, z);
      }
      if (f === 'up' || f === 'south') {
        const t = (f === 'up' ? z : y) * 0.32 + 0.7 * Math.sin(x * 0.4);
        if (frac(t) < 0.3 && ax > 1) return grain(C.black, x, y, z);
        return grain(f === 'up' ? C.deep : C.orange, x, y, z);
      }
      if (f === 'down') return grain(C.white, x, y, z);
      if (y < 26.5) return grain(C.white, x, y, z);
      if (frac((z * 0.45 + y * 0.6) * 0.34) < 0.26) return grain(C.black, x, y, z);
      return grain(mix(C.orange, C.deep, (y - 26) / 8), x, y, z);
    }
    case 'eye': {
      if (f !== 'north') return C.rim;
      const ex = ax - 5.1, ey = y - (29.3 + ex * 0.42);
      if (Math.abs(ey) > 0.85 || Math.abs(ex) > 2.45) return ey > 0 ? C.black : grain(C.white, x, y, z);
      if (x > 0 && scarred(x, y, [36.6, 38.2], 1.15)) return C.scar;
      if (Math.abs(ey) > 0.62 || Math.abs(ex) > 2.15) return C.rim;
      if (Math.abs(ex + 0.4) < 0.34 && Math.abs(ey) < 0.5) return C.pupil;
      if (Math.abs(ex + 0.4) < 0.95) return C.iris;
      return C.eye;
    }
    case 'cheek': {
      if (frac(z * 0.36 + y * 0.28) < 0.22 && y > 21.5) return grain(C.black, x, y, z);
      return grain(C.white, x, y, z);
    }
    case 'ruff': {
      if (y > 26.5) return grain(mix(C.cream, C.orange, (y - 26.5) / 3), x, y, z);
      if (frac(z * 0.3 + y * 0.24) < 0.14 && y > 22) return grain(C.black, x, y, z);
      return grain(C.white, x, y, z);
    }
    case 'brow': {
      // The heavy brow: white along its underside (the white over the eyes), dark orange and black stripes above.
      if (f === 'down') return grain(C.white, x, y, z);
      if (f === 'north') return y < piece.from[1] + 0.9 ? grain(C.white, x, y, z) : grain(ax < 2.2 ? C.black : C.deep, x, y, z);
      return grain(frac(x * 0.45) < 0.4 ? C.black : C.deep, x, y, z);
    }
    case 'muzzle': {
      if (f === 'up') return grain(ax < 1 ? C.pale : frac(z * 0.7) < 0.3 && z > -55 ? C.deep : C.orange, x, y, z);
      if (f === 'north') {
        if (ax < 0.4 && y < 25.8) return C.black;
        if (y < 21.6) return C.black;
        if (y < 25.4 && y > 23 && ax > 1.4 && ax < 5 && (Math.round(x * 2) + Math.round(y * 2)) % 3 === 0) return C.brown;
        if (x > 0 && scarred(x, y, [29.4, 31], 1.15)) return C.scar;
        return grain(C.white, x, y, z);
      }
      if (f === 'east' || f === 'west') {
        if (y < 21.6) return C.black;
        if (frac(z * 0.5 + y * 0.4) < 0.2 && y > 25) return grain(C.black, x, y, z);
        return grain(y > 26 ? C.orange : C.white, x, y, z);
      }
      return grain(C.white, x, y, z);
    }
    case 'snarl': return grain(frac(z * 0.75) < 0.42 ? C.black : C.deep, x, y, z);
    case 'nose': return (f === 'north' || f === 'up') && ax < 2 && y > 27.4 ? grain(C.nose, x, y, z) : C.noseDark;
    case 'flew': return y < 19.1 || f === 'down' ? C.black : grain(C.white, x, y, z);
    case 'fang': return y < piece.to[1] - 1.4 || piece.name === 'incisors' ? grain(C.fang, x, y, z) : C.fangRoot;
    case 'throat': return f === 'north' ? C.gum : C.throat;
    case 'jaw': {
      if (f === 'up') return grain(C.gum, x, y, z);
      if (f === 'north' && y > 18.1) return C.black;
      return grain(y < 14 ? C.shade : C.white, x, y, z);
    }
    case 'ear': {
      if (f === 'north') {
        const lo = piece.from[0], hi = piece.to[0];
        if (y > piece.to[1] - 0.7 || x < lo + 0.5 || x > hi - 0.5) return C.black;                      // the black rim
        return y < piece.from[1] + 1.4 ? grain(C.cream, x, y, z) : grain(C.orange, x, y, z);             // a pale tuft low
      }
      if (f === 'south') {
        const cx = (piece.from[0] + piece.to[0]) / 2;
        return Math.hypot(x - cx, y - 36.6) < 1.2 ? C.white : C.black;
      }
      return y > 37.8 ? C.black : grain(C.orange, x, y, z);
    }
    case 'whisker': return C.white;
    case 'tail': case 'tail_tip': {
      const along = piece.ord + (z - piece.from[2]) / (piece.to[2] - piece.from[2]);
      if (part === 'tail_tip' && along > 4.35) return grain(C.black, x, y, z);
      if (frac(along * 1.8 + 0.1) < 0.38) return grain(C.black, x, y, z);
      return grain(f === 'down' ? C.white : C.orange, x, y, z);
    }
  }
  return [255, 0, 255];
}
P.filter(p => p.part.startsWith('tail')).forEach((p, i) => { p.ord = i; });

// ---------------------------------------------------------------- packing and painting
const dims = (p, f) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, r = d => Math.max(1, Math.round(d * DENSITY));
  if (f === 'north' || f === 'south') return [r(x1 - x0), r(y1 - y0)];
  if (f === 'east' || f === 'west') return [r(z1 - z0), r(y1 - y0)];
  return [r(x1 - x0), r(z1 - z0)];
};
/** The model point under texel (u, v) of face f (Blockbench's face orientations: north seen from the north, etc.). */
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
const faces = [];
for (const p of P) for (const f of FACES) { const [w, h] = dims(p, f); faces.push({ p, f, w, h }); }
faces.sort((a, b) => b.h - a.h || b.w - a.w);
let cx = 0, cy = 0, shelf = 0;
for (const face of faces) {
  if (cx + face.w > ATLAS) { cx = 0; cy += shelf; shelf = 0; }
  face.u = cx; face.v = cy; cx += face.w; shelf = Math.max(shelf, face.h);
}
if (cy + shelf > ATLAS) throw new Error('the atlas is full');
const img = new Uint8Array(ATLAS * ATLAS * 4);
for (const face of faces) {
  (face.p.uv ||= {})[face.f] = [face.u, face.v, face.u + face.w, face.v + face.h];
  for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
    const [x, y, z] = point(face.p, face.f, u, v, face.w, face.h);
    const lift = face.p.lift || 0;
    const c = paint(face.p.part, face.f, x, y - lift, z, { ...face.p, from: face.p.orig.from, to: face.p.orig.to }).map(n => Math.max(0, Math.min(255, Math.round(n))));
    img.set([...c, 255], ((face.v + v) * ATLAS + face.u + u) * 4);
  }
}

// ---------------------------------------------------------------- the project
const elements = P.map(p => ({
  name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
  from: p.from, to: p.to, autouv: 0, color: 0, origin: p.pivot || p.from.map((v, k) => (v + p.to[k]) / 2),
  rotation: p.rot ? (p.axis === 'y' ? [0, p.rot, 0] : p.axis === 'z' ? [0, 0, p.rot] : [p.rot, 0, 0]) : [0, 0, 0],
  faces: Object.fromEntries(FACES.map(f => [f, { uv: p.uv[f], texture: 0 }])), type: 'cube', uuid: stableId('tiger/' + p.name),
}));
const PIVOTS = { body: [0, 35, 0], head: [0, 35, -28], jaw: [0, 25.6, -46], front_leg_l: [8, 28, -20], front_leg_r: [-8, 28, -20],
  hind_leg_l: [8, 23, 18], hind_leg_r: [-8, 23, 18], tail: [0, 41, 23] };
const group = (name, children) => ({ name, origin: PIVOTS[name] || [0, 0, 0], color: 0, uuid: stableId('tiger/bone/' + name), export: true,
  mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0,
  children: [...elements.filter((e, i) => P[i].bone === name).map(e => e.uuid), ...children] });
const outliner = [group('body', [group('head', [group('jaw', [])]), group('front_leg_l', []), group('front_leg_r', []),
  group('hind_leg_l', []), group('hind_leg_r', []), group('tail', [])])];
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
fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel'), JSON.stringify(project));
fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.png'), texture);
console.log('tiger:', P.length, 'cubes, atlas', ATLAS, 'used to row', cy + shelf);
