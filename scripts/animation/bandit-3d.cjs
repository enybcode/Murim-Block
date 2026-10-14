// The Mountain Bandit in 3D: one detailed model on Epic Fight's biped skeleton, five outfits painted on it.
//   art/blockbench/mountain_bandit.bbmodel          the Blockbench project (bones: the biped's joints, as blockbench-rig.cjs
//                                                   places them), to look at and edit;
//   art/blockbench/mountain_bandit/<outfit>.png     its five atlases;
//   assets/murimblock/animmodels/entity/mountain_bandit.json   the same model as an Epic Fight skinned mesh (its parts
//                                                   named as the biped's, so it loads as a HumanoidMesh);
//   assets/murimblock/textures/entity/mountain_bandit/3d/<outfit>.png   the atlases, for the game.
// One model for every outfit, like the axolotl's colours: a piece an outfit does not wear is clear in its atlas.
// Real volume where the flat skin only painted: a crossed collar, a knotted sash whose tails hang, a robe skirt that
// flares over each thigh and follows it, rolled sleeves, wrapped bracers and gaiters, sandals, a pouch; and, by outfit,
// a headband with its knot and tails, a hair knot, a leather or lacquered shoulder guard, a wine gourd, a scarf, the wolf
// hood with its ears and snout, the wolf pelt and tail, the turban and its tail, the dune veil, the gilded mask.
// Each texel is one model pixel (Minecraft's scale), painted face by face below.
// Usage: node scripts/animation/bandit-3d.cjs
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { at, uuid } = require('./blockbench-rig.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

// ---------------------------------------------------------------- the outfits

/**
 * Colour letters: H headwear, A hair, S skin, E eyes, M mouth or mask, R robe, D robe trim, U sleeves, B sash, W wraps,
 * P trousers, F sandals, L leather, G metal, K gourd, N cord, O fur, V veil. A lower-case letter is the same a shade darker.
 */
const OUTFITS = [
  { key: 'ash', band: true, knot: true, guard: true, gourd: true,
    c: { H: '#3b3a3f', A: '#2a2220', S: '#c69c6d', E: '#1b1b22', M: '#9c7a55', R: '#77716a', D: '#4a4640', U: '#6c665f', B: '#a08455', W: '#d3c7ac', P: '#4a4640', F: '#5b4430', L: '#6b4a2e', G: '#b8a27a', K: '#b5793a', N: '#3a2a1a', O: '#77716a', V: '#77716a' } },
  { key: 'crimson', band: true, knot: true, scarf: true,
    c: { H: '#a7302a', A: '#2a2220', S: '#c99e70', E: '#1b1b22', M: '#8f6c4c', R: '#5e4c42', D: '#3a2e29', U: '#54443b', B: '#a7302a', W: '#cbbf9f', P: '#3b3632', F: '#4a3626', L: '#5a3a24', G: '#c9a74b', K: '#b5793a', N: '#3a2a1a', O: '#5e4c42', V: '#5e4c42' } },
  { key: 'wolf', hood: true,
    c: { H: '#8a7a66', A: '#4a3524', S: '#bf9265', E: '#1b1b22', M: '#5a4030', R: '#6b4f36', D: '#3f2e20', U: '#5f4630', B: '#2f2620', W: '#8a7a66', P: '#4d4136', F: '#3a2a1c', L: '#4a3524', G: '#9a8a74', K: '#b5793a', N: '#2a1e14', O: '#8f8170', V: '#6b4f36' } },
  { key: 'dune', turban: true, veil: true, guard: true, gourd: true,
    c: { H: '#d9c08f', A: '#3a2a1c', S: '#b0804f', E: '#1b1b22', M: '#c9ad78', R: '#b88a4e', D: '#7e5a2c', U: '#a77d47', B: '#6b3d2a', W: '#e3d3ae', P: '#9c7a50', F: '#5e4129', L: '#7a5232', G: '#d4b06a', K: '#9b6630', N: '#4a3020', O: '#b88a4e', V: '#cdb486' } },
  { key: 'gilded', band: true, knot: true, mask: true, guard: true,
    c: { H: '#1e1b22', A: '#1e1b22', S: '#c69c6d', E: '#120f14', M: '#d4a637', R: '#2b2a33', D: '#17161c', U: '#25242c', B: '#d4a637', W: '#4a4652', P: '#23222a', F: '#18161a', L: '#1c1a20', G: '#d4a637', K: '#b5793a', N: '#d4a637', O: '#2b2a33', V: '#2b2a33' } },
];

// The face, as on the sheet (8 x 8, the viewer's view: the bandit's right on the left).
const FACE = ['AAAAAAAA', 'AAAAAAAA', 'ASSSSSSA', 'SEESSEES', 'SSSSSSSS', 'SSMMMMSS', 'SMMMMMMS', 'SSSSSSSS'];

// ---------------------------------------------------------------- colours

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
/** A little grain so flat colours read as cloth and leather (the same for a given texel). */
const grain = (u, v, s) => 0.95 + (((u * 73 + v * 151 + s * 37) % 7) / 6) * 0.08;
/** A touch of light from above, on top of the game's own shading. */
const LIGHT = { up: 1.04, down: 0.86, north: 1, south: 0.94, east: 0.97, west: 0.97 };

function colour(letter, outfit, face, u, v, seed) {
  if (letter == null || letter === '.') return null;
  if (typeof letter === 'string' && letter.startsWith('#')) return [...hex(letter), 255];
  const dark = letter !== letter.toUpperCase();
  const h = outfit.c[letter.toUpperCase()];
  if (!h) throw new Error('no colour ' + letter + ' in ' + outfit.key);
  const f = (dark ? 0.8 : 1) * LIGHT[face] * grain(u, v, seed);
  return [...hex(h).map(x => Math.max(0, Math.min(255, Math.round(x * f)))), 255];
}

// ---------------------------------------------------------------- the pieces

const [, HEAD] = at('Head');
const [, ELBOW] = at('Hand_R');
const [, KNEE] = at('Leg_R');

/**
 * A piece: its bone, its box (Blockbench pixels: x the bandit's right, y up, z its back; it faces north, -z), what it
 * shows on each face (paint(face, u, v, w, h, outfit) -> a colour letter, a hex colour, or null for clear, u and v as the
 * face is seen from outside, v down), which outfits wear it, which mesh part it belongs to, and optionally a tilt about
 * x (degrees, around `pivot`) or a hinge (its vertices on the plane y = hinge.y follow `hinge.bone` half way, so elbows,
 * knees and the waist bend smoothly).
 */
const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, wear: () => true, ...extra });
const all = l => () => l;
const isSide = f => f === 'north' || f === 'south' || f === 'east' || f === 'west';
/** Cloth hanging in folds: a fold every third column, the hem darker. */
const folds = (l, hem) => (f, u, v, w, h) => (isSide(f) && v === h - 1 && hem ? hem : (isSide(f) && u % 3 === 2 ? l.toLowerCase() : l));
/** A fringe: the last row every other texel. */
const fringe = (l) => (f, u, v, w, h) => (isSide(f) && v === h - 1 ? (u % 2 ? null : l.toLowerCase()) : (u % 3 === 1 ? l.toLowerCase() : l));
/** Wrapped strips running round a limb, slanted. */
const wraps = (l, strap) => (f, u, v, w, h) => (f === 'up' || f === 'down' ? l.toLowerCase() : strap && v === Math.floor(h / 2) ? strap : (u + v) % 3 === 0 ? l.toLowerCase() : l);
/** Only the rim of a top or bottom face: what lies inside is hidden in the body. */
const rim = (paintIt) => (f, u, v, w, h, o) => ((f === 'up' || f === 'down') && u > 0 && v > 0 && u < w - 1 && v < h - 1 ? null : paintIt(f, u, v, w, h, o));

// ---- head
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') {
    if (o.mask && (v === 5 || v === 6)) return 'H';
    return FACE[v][u];
  }
  if (f === 'up') return 'A';
  if (f === 'down') return 'S';
  if (f === 'south') return v > 5 ? 'a' : 'A';
  // A side, `front` counted from the face: the cheek, an ear, hair behind.
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 2) return 'A';
  if (front <= 1) return v >= 5 && v <= 6 && front === 0 ? 'M' : 'S';
  if (front === 3 && (v === 3 || v === 4)) return 's';
  return v === 7 && front < 4 ? 'S' : 'A';
});
piece('Head', 'head', 'hair_knot', [-1.5, 32, 0], [1.5, 34, 3], (f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'B' : (u + v) % 2 ? 'a' : 'A'),
  { wear: o => o.knot });
piece('Head', 'head', 'band', [-4.35, 28.4, -4.35], [4.35, 29.6, 4.35], rim((f, u, v) => (v === 0 ? 'H' : 'h')), { wear: o => o.band });
piece('Head', 'head', 'band_knot', [-1.1, 28.1, 4.3], [1.1, 30.0, 5.2], (f, u) => (u % 2 ? 'h' : 'H'), { wear: o => o.band });
piece('Head', 'head', 'band_tail_l', [-1.6, 24.4, 4.6], [-0.6, 28.4, 5.0], fringe('H'), { wear: o => o.band, tilt: -18, pivot: [-1.1, 28.4, 4.8] });
piece('Head', 'head', 'band_tail_r', [0.6, 25.0, 4.6], [1.6, 28.4, 5.0], fringe('H'), { wear: o => o.band, tilt: -26, pivot: [1.1, 28.4, 4.8] });
piece('Head', 'head', 'mask', [-4.2, 26.4, -4.45], [4.2, 29.2, -3.95], (f, u, v, w, h) => {
  if (f !== 'north') return 'm';
  if (v === 1 && (u === 1 || u === 2 || u === 5 || u === 6)) return 'E';
  return v === 0 || v === h - 1 ? 'm' : 'M';
}, { wear: o => o.mask });
piece('Head', 'head', 'veil', [-4.4, 24.2, -4.6], [4.4, 27.0, -4.1], folds('V', 'v'), { wear: o => o.veil });
piece('Head', 'hat', 'hood', [-4.6, 23.6, -4.6], [4.6, 32.8, 4.6], (f, u, v, w, h) => {
  // Open on the face and underneath, fur all round.
  if (f === 'north' && v > 1 && u > 0 && u < w - 1) return null;
  if (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1) return null;
  return (u * 7 + v * 13) % 9 === 0 ? 'o' : 'O';
}, { wear: o => o.hood });
piece('Head', 'head', 'ear_l', [-3.8, 32.6, -1.2], [-1.6, 34.8, 0.2], (f, u, v) => (f === 'north' && v > 0 ? 'm' : 'O'), { wear: o => o.hood });
piece('Head', 'head', 'ear_r', [1.6, 32.6, -1.2], [3.8, 34.8, 0.2], (f, u, v) => (f === 'north' && v > 0 ? 'm' : 'O'), { wear: o => o.hood });
piece('Head', 'head', 'snout', [-1.8, 31.0, -5.9], [1.8, 32.6, -4.6], (f, u, v, w) => (f === 'north' && v === 0 && (u === 1 || u === 2) ? 'E' : f === 'down' ? 'o' : 'O'),
  { wear: o => o.hood });
piece('Head', 'hat', 'turban', [-4.6, 28.4, -4.6], [4.6, 32.9, 4.6], (f, u, v, w, h) => {
  if (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1) return null;
  if (f === 'north' && u >= 4 && u <= 5 && v <= 3) return 'B';
  return (v + Math.floor(u / 3)) % 3 === 2 ? 'h' : 'H';
}, { wear: o => o.turban });
piece('Head', 'hat', 'turban_tail', [2.0, 23.6, 4.4], [3.6, 28.6, 5.0], fringe('H'), { wear: o => o.turban, tilt: -14, pivot: [2.8, 28.6, 4.7] });

// ---- body: the robe, the crossed collar, the sash
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w, h) => {
  if (f === 'north') return (v === 0 && u >= 3 && u <= 5) || (v === 1 && u === 4) ? 'W' : u === 0 || u === w - 1 ? 'r' : 'R';
  if (f === 'south') return u === 0 || u === w - 1 ? 'r' : v === 0 ? 'D' : 'R';
  if (f === 'up') return 'D';
  return 'R';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v, w) => (f === 'down' ? 'P' : isSide(f) && (u === 0 || u === w - 1) && f !== 'north' ? 'r' : 'R'),
  { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar', [-2.6, 23.3, -2.4], [2.6, 24.4, 2.4], rim(all('D')));
// The left side over the right: the collar's dark edge steps down from the left of the neck to the right side.
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.7]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.3, y + 1.0, -2.0], all('D')));
piece('Torso', 'jacket', 'sash', [-4.5, 16.6, -2.5], [4.5, 18.8, 2.5], rim((f, u, v) => ((u + v) % 4 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_knot', [-3.8, 16.2, -3.3], [-1.6, 19.2, -2.5], (f, u, v) => (v === 1 ? 'b' : 'B'));
piece('Torso', 'jacket', 'sash_tail_a', [-3.6, 11.2, -3.5], [-2.8, 16.2, -3.1], fringe('B'));
piece('Torso', 'jacket', 'sash_tail_b', [-2.5, 11.8, -3.5], [-1.7, 16.2, -3.1], fringe('B'));
piece('Torso', 'jacket', 'pouch', [1.2, 13.4, -2.9], [3.4, 15.8, -2.0], (f, u, v, w) => (v === 0 ? 'l' : f === 'north' && v === 1 && u === Math.floor(w / 2) ? 'G' : 'L'));
piece('Torso', 'jacket', 'gourd', [-3.8, 12.0, 2.1], [-2.0, 14.0, 3.9], (f, u, v) => (u === 0 && v === 0 ? '#e0a860' : (u + v) % 4 === 3 ? 'k' : 'K'), { wear: o => o.gourd });
piece('Torso', 'jacket', 'gourd_neck', [-3.4, 14.0, 2.5], [-2.4, 15.2, 3.5], (f, u, v) => (v === 0 ? 'N' : 'K'), { wear: o => o.gourd });
piece('Chest', 'jacket', 'scarf', [-3.0, 22.8, -2.7], [3.0, 24.6, 2.7], rim((f, u, v) => ((u + v) % 3 === 0 ? 'h' : 'H')), { wear: o => o.scarf });
piece('Chest', 'jacket', 'scarf_end', [1.0, 18.6, -3.1], [2.6, 23.0, -2.7], fringe('H'), { wear: o => o.scarf });
piece('Chest', 'jacket', 'pelt', [-4.4, 12.0, 2.05], [4.4, 24.4, 2.9], (f, u, v, w, h) => (v === h - 1 ? 'o' : (u * 7 + v * 13) % 9 === 0 ? 'o' : 'O'), { wear: o => o.hood });
piece('Torso', 'jacket', 'wolf_tail', [-0.9, 8.4, 2.4], [0.9, 12.4, 3.6], (f, u, v, w, h) => (v === h - 1 ? 'W' : (u + v) % 3 === 0 ? 'o' : 'O'),
  { wear: o => o.hood, tilt: -12, pivot: [0, 12.4, 3.0] });

// ---- arms: sleeves rolled at the elbow, wrapped forearms, a shoulder guard or the wolf's fur
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [a0, a1] = xs(4, 8), [c0, c1] = xs(3.7, 8.3), [f0, f1] = xs(3.6, 8.4);
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + side.toLowerCase(), [a0, 17, -2], [a1, 24, 2], (f, u, v, w, h) => (f === 'south' && u === Math.floor(w / 2) ? 'u' : 'U'),
    { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'forearm_' + side.toLowerCase(), [a0, 11, -2], [a1, 17, 2], (f, u, v, w, h) => {
    if (f === 'down') return 'S';
    if (f === 'up') return 'U';
    return v >= h - 2 ? 'S' : v === 0 ? 'u' : 'W';
  }, { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Arm_' + side, part + 'Sleeve', 'cuff_' + side.toLowerCase(), [c0, 16.9, -2.3], [c1, 18.1, 2.3], rim((f, u, v) => (v === 0 ? 'U' : 'D')));
  piece('Hand_' + side, part + 'Sleeve', 'bracer_' + side.toLowerCase(), [c0, 12.4, -2.3], [c1, 15.6, 2.3], rim(wraps('W', 'L')));
  piece('Arm_' + side, part + 'Sleeve', 'shoulder_fur_' + side.toLowerCase(), [f0, 22.2, -2.5], [f1, 24.5, 2.5], rim((f, u, v) => ((u * 7 + v * 13) % 9 === 0 ? 'o' : 'O')),
    { wear: o => o.hood });
}
piece('Arm_L', 'leftSleeve', 'shoulder_guard', [-8.7, 21.4, -2.7], [-3.5, 24.7, 2.7], (f, u, v, w, h) => {
  if (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1) return null;
  if (isSide(f) && v === 1 && u % 2 === 1) return 'G';
  return isSide(f) && v === h - 1 ? 'l' : 'L';
}, { wear: o => o.guard });

// ---- legs: the robe's skirt flaring over each thigh (it follows the leg), trousers, gaiters, sandals
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [t0, t1] = xs(0, 4), [s0, s1] = xs(-0.3, 4.3), [h0, h1] = xs(-0.55, 4.55), [g0, g1] = xs(-0.3, 4.3), [d0, d1] = xs(-0.2, 4.2);
  // The inner side (toward the other leg) is left open.
  const inner = sx > 0 ? 'west' : 'east';
  const skirt = (hem) => (f, u, v, w, h) => (f === inner || f === 'up' || (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1) ? null
    : hem && v === h - 1 ? 'D' : u % 3 === 2 ? 'r' : 'R');
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + side.toLowerCase(), [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + side.toLowerCase(), [t0, 0, -2], [t1, 6, 2], (f, u, v, w, h) => (v >= h - 1 ? 'F' : 'P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + side.toLowerCase(), [s0, 9.6, -2.35], [s1, 13.2, 2.35], skirt(false));
  piece('Thigh_' + side, part + 'Pants', 'skirt_hem_' + side.toLowerCase(), [h0, 7.6, -2.6], [h1, 9.6, 2.6], skirt(true));
  piece('Leg_' + side, part + 'Pants', 'gaiter_' + side.toLowerCase(), [g0, 1.2, -2.3], [g1, 5.2, 2.3],
    rim((f, u, v, w, h) => (v === 0 || v === h - 1 ? 'L' : v % 2 ? 'w' : 'W')));
  piece('Leg_' + side, part + 'Pants', 'sandal_' + side.toLowerCase(), [d0, 0, -2.7], [d1, 0.8, 2.2], (f, u, v) => (f === 'down' ? 'f' : v === 0 ? 'F' : 'f'));
}

// ---------------------------------------------------------------- faces and the atlas

const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
const size = (p, f) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, r = d => Math.max(1, Math.round(d));
  if (f === 'north' || f === 'south') return [r(x1 - x0), r(y1 - y0)];
  if (f === 'east' || f === 'west') return [r(z1 - z0), r(y1 - y0)];
  return [r(x1 - x0), r(z1 - z0)];
};

/** Shelf packing, tallest faces first: each face gets its rectangle of texels in the atlas. */
function pack() {
  const faces = [];
  for (const p of PIECES) for (const f of FACES) { const [w, h] = size(p, f); faces.push({ p, f, w, h }); }
  faces.sort((a, b) => b.h - a.h || b.w - a.w);
  let x = 0, y = 0, shelf = 0;
  for (const face of faces) {
    if (x + face.w > ATLAS) { x = 0; y += shelf; shelf = 0; }
    face.u = x; face.v = y;
    x += face.w; shelf = Math.max(shelf, face.h);
  }
  if (y + shelf > ATLAS) throw new Error('the atlas is full: ' + (y + shelf) + ' rows');
  for (const face of faces) (face.p.uv ||= {})[face.f] = [face.u, face.v, face.u + face.w, face.v + face.h];
  return faces;
}

function paintAtlas(faces, outfit) {
  const img = new Uint8Array(ATLAS * ATLAS * 4);
  faces.forEach((face, seed) => {
    if (!face.p.wear(outfit)) return;
    for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
      const c = colour(face.p.paint(face.f, u, v, face.w, face.h, outfit), outfit, face.f, u, v, seed);
      if (!c) continue;
      const i = ((face.v + v) * ATLAS + face.u + u) * 4;
      img.set(c, i);
    }
  });
  return img;
}

function png(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) Buffer.from(rgba.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------------------------------------------------------------- the Epic Fight mesh

/** The biped's joints, in the order of Epic Fight's armature (entity/biped): a vertex names its joint by this index. */
const JOINTS = ['Root', 'Thigh_R', 'Leg_R', 'Knee_R', 'Thigh_L', 'Leg_L', 'Knee_L', 'Torso', 'Chest', 'Head', 'Shoulder_R', 'Arm_R', 'Hand_R',
  'Tool_R', 'Elbow_R', 'Shoulder_L', 'Arm_L', 'Hand_L', 'Tool_L', 'Elbow_L'];
const PARTS = ['head', 'hat', 'torso', 'jacket', 'rightArm', 'leftArm', 'rightSleeve', 'leftSleeve', 'rightLeg', 'leftLeg', 'rightPants', 'leftPants'];

/** The corners of a face as seen from outside: top left, top right, bottom right, bottom left. */
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

/** A tilted piece turns about x around its pivot (Blockbench's convention: +y toward +z for a positive angle). */
function tilt(p, [x, y, z]) {
  if (!p.tilt) return [x, y, z];
  const a = p.tilt * Math.PI / 180, [, py, pz] = p.pivot, dy = y - py, dz = z - pz;
  return [x, py + dy * Math.cos(a) - dz * Math.sin(a), pz + dy * Math.sin(a) + dz * Math.cos(a)];
}

/** Blockbench pixels to Epic Fight's mesh space: blocks, z up, the front toward +y. */
const toMesh = ([x, y, z]) => [x / 16, -z / 16, y / 16];

function mesh() {
  const positions = [], uvs = [], normals = [], vcounts = [], vindices = [], weights = [1, 0.5];
  const parts = Object.fromEntries(PARTS.map(n => [n, []]));
  const normalIndex = n => {
    for (let i = 0; i < normals.length; i += 3) if (Math.abs(normals[i] - n[0]) + Math.abs(normals[i + 1] - n[1]) + Math.abs(normals[i + 2] - n[2]) < 1e-4) return i / 3;
    normals.push(...n); return normals.length / 3 - 1;
  };
  const round = v => Math.round(v * 1e6) / 1e6;
  for (const p of PIECES) {
    const own = JOINTS.indexOf(p.bone);
    if (own < 0) throw new Error('no joint ' + p.bone);
    const cache = new Map();
    const vertex = c => {
      const key = c.join(',');
      if (cache.has(key)) return cache.get(key);
      const m = toMesh(tilt(p, c));
      positions.push(...m.map(round));
      if (p.hinge && Math.abs(c[1] - p.hinge.y) < 0.01) {
        vcounts.push(2); vindices.push(own, 1, JOINTS.indexOf(p.hinge.bone), 1);
      } else {
        vcounts.push(1); vindices.push(own, 0);
      }
      cache.set(key, positions.length / 3 - 1);
      return positions.length / 3 - 1;
    };
    for (const f of FACES) {
      const cs = corners(p, f), ids = cs.map(vertex);
      const [u0, v0, u1, v1] = p.uv[f];
      const uvBase = uvs.length / 2;
      for (const [u, v] of [[u0, v0], [u1, v0], [u1, v1], [u0, v1]]) uvs.push(u / ATLAS, v / ATLAS);
      // The outward normal, from the (tilted) corners in mesh space.
      const [a, b, c] = [0, 3, 2].map(i => toMesh(tilt(p, cs[i])));
      const e1 = b.map((x, i) => x - a[i]), e2 = c.map((x, i) => x - a[i]);
      const n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]];
      const len = Math.hypot(...n), ni = normalIndex(n.map(x => round(x / len)));
      // Two triangles, counter-clockwise from outside: top left, bottom left, bottom right; top left, bottom right, top right.
      for (const k of [0, 3, 2, 0, 2, 1]) parts[p.part].push(ids[k], uvBase + k, ni);
    }
  }
  const block = (array, stride) => ({ stride, count: array.length / stride, array });
  return {
    vertices: {
      positions: block(positions, 3), uvs: block(uvs, 2), normals: block(normals, 3),
      vcounts: block(vcounts, 1), weights: block(weights, 1), vindices: block(vindices, 1),
      parts: Object.fromEntries(Object.entries(parts).map(([n, a]) => [n, block(a, 3)])),
    },
  };
}

// ---------------------------------------------------------------- the Blockbench project

const BONES = {
  Root: ['Torso', 'Thigh_R', 'Thigh_L'], Torso: ['Chest'], Chest: ['Head', 'Shoulder_R', 'Shoulder_L'], Head: [],
  Shoulder_R: ['Arm_R'], Arm_R: ['Hand_R'], Hand_R: ['Tool_R'], Tool_R: [],
  Shoulder_L: ['Arm_L'], Arm_L: ['Hand_L'], Hand_L: ['Tool_L'], Tool_L: [],
  Thigh_R: ['Leg_R'], Leg_R: [], Thigh_L: ['Leg_L'], Leg_L: [],
};

function bbmodel(atlases) {
  const elements = PIECES.map(p => ({
    name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from: p.from, to: p.to, autouv: 0, color: 0, origin: p.pivot || p.from.map((v, k) => (v + p.to[k]) / 2),
    rotation: [p.tilt || 0, 0, 0], faces: Object.fromEntries(FACES.map(f => [f, { uv: p.uv[f], texture: 0 }])),
    type: 'cube', uuid: uuid(),
  }));
  const group = name => ({
    name, origin: at(name), color: 0, uuid: uuid(), export: true, mirror_uv: false, isOpen: true, locked: false, visibility: true,
    autouv: 0, children: [...elements.filter((e, i) => PIECES[i].bone === name).map(e => e.uuid), ...BONES[name].map(group)],
  });
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name: 'mountain_bandit', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
    timeline_setups: [], unhandled_root_fields: {}, resolution: { width: ATLAS, height: ATLAS }, elements, outliner: [group('Root')],
    textures: OUTFITS.map((o, i) => ({
      path: '', name: o.key + '.png', folder: 'entity/mountain_bandit/3d', namespace: 'murimblock', id: String(i), width: ATLAS, height: ATLAS,
      uv_width: ATLAS, uv_height: ATLAS, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default',
      render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
      saved: false, uuid: uuid(), source: 'data:image/png;base64,' + atlases[i].toString('base64'),
    })),
  };
}

// ---------------------------------------------------------------- write

if (require.main === module) {
  const faces = pack();
  const atlases = OUTFITS.map(o => png(ATLAS, ATLAS, paintAtlas(faces, o)));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(assets, 'textures', 'entity', 'mountain_bandit', '3d'), { recursive: true });
  fs.mkdirSync(path.join(art, 'mountain_bandit'), { recursive: true });
  OUTFITS.forEach((o, i) => {
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'mountain_bandit', '3d', o.key + '.png'), atlases[i]);
    fs.writeFileSync(path.join(art, 'mountain_bandit', o.key + '.png'), atlases[i]);
  });
  const m = mesh();
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'mountain_bandit.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'mountain_bandit.bbmodel'), JSON.stringify(bbmodel(atlases), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the 3D bandit:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFITS };
