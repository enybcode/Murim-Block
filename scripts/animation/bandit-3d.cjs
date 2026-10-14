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
const kit = require('./mesh-kit.cjs');
const { isSide, all, folds, fringe, wraps, rim } = kit;

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
const FACE = ['AAAAAAAA', 'AAAAAAAA', 'AaaSSaaA', 'SZESSEZS', 'SSSssSSS', 'SSMMMMSS', 'SMMMMMMS', 'SSSSSSSS'];

/**
 * The faces it can make (client/MobFaces picks one each tick: blinks, glances, a mutter; a frown, a shout in combat), as
 * changes to rows of FACE: 2 the brows, 3 the eyes (Z their whites), 5 and 6 the mouth. Each face is a copy of the atlas.
 */
const FACES = {
  calm: {}, blink: { 3: 'SssSSssS' }, left: { 3: 'SEZSSEZS' }, right: { 3: 'SZESSZES' }, mutter: { 6: 'SMMeeMMS' },
  angry: { 2: 'ASaaaaSA' }, angry_blink: { 2: 'ASaaaaSA', 3: 'SssSSssS' }, angry_left: { 2: 'ASaaaaSA', 3: 'SEZSSEZS' },
  angry_right: { 2: 'ASaaaaSA', 3: 'SZESSZES' }, shout: { 2: 'ASaaaaSA', 5: 'SSMZZMSS', 6: 'SMeeeeMS' },
};
const faceRow = (o, v) => ((FACES[o.face || 'calm'] || {})[v] || FACE[v]);

// ---------------------------------------------------------------- the pieces


/** The pieces (see mesh-kit.cjs). */
const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });

// ---- head
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') {
    if (o.mask && (v === 5 || v === 6)) return 'H';
    const l = faceRow(o, v)[u];
    // The whites of the eyes.
    return l === 'Z' ? '#e9e4d8' : l;
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

// ---------------------------------------------------------------- write

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlases = OUTFITS.map(o => kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
  const faceDir = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'entity', 'mountain_bandit', '3d');
  fs.mkdirSync(faceDir, { recursive: true });
  for (const o of OUTFITS) for (const face of Object.keys(FACES)) if (face !== 'calm')
    fs.writeFileSync(path.join(faceDir, o.key + '_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...o, face }, ATLAS)));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(assets, 'textures', 'entity', 'mountain_bandit', '3d'), { recursive: true });
  fs.mkdirSync(path.join(art, 'mountain_bandit'), { recursive: true });
  OUTFITS.forEach((o, i) => {
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'mountain_bandit', '3d', o.key + '.png'), atlases[i]);
    fs.writeFileSync(path.join(art, 'mountain_bandit', o.key + '.png'), atlases[i]);
  });
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'mountain_bandit.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'mountain_bandit.bbmodel'), JSON.stringify(kit.bbmodel('mountain_bandit', PIECES, OUTFITS, atlases, ATLAS, 'entity/mountain_bandit/3d'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the 3D bandit:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFITS, FACES };
