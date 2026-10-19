// The Crow Master (鸦师), the hermit of the Raven Tower who keeps the Raven Messenger Art (docs/mobs/crow-master.md),
// in 3D on Epic Fight's biped skeleton, after Sensei Yun's build (sensei-3d.cjs): an old man with long white hair in a
// topknot held by a gold pin, bushy white brows, a drooping moustache and a white beard, amber eyes like his raven's;
// a black robe crossed right over left, edged in gold over a grey under-robe, a raven embroidered in gold on the back;
// a short mantle of black feathers over the shoulders; wide black sleeves with gold cuffs; a violet sash with an amber
// stripe and a gold clasp, an amber stone on its cord; a messenger's tube ringed in gold across his back; the skirt split
// over each leg, hemmed in gold; dark trousers, grey socks, black cloth shoes.
//   art/blockbench/crow_master.bbmodel (+ crow_master/master.png)   the Blockbench project;
//   assets/murimblock/animmodels/entity/crow_master.json            the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/crow_master.png (+ its faces) its atlas.
// Usage: node scripts/animation/crow-master-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/**
 * Colour letters: S skin, A white hair, brows and beard, E amber eyes, M mouth, H black feathers (the mantle), C amber
 * (beard ring, sash stripe, cords), R black robe, Q gold trim, I grey under-robe and socks, W white (eyes), D shadows,
 * B violet sash, G gold, J amber stone, P trousers, F shoes, K the tube's lacquer, X the white hair's shadowed strands.
 */
const SENSEI = { key: 'master', c: {
  S: '#d4ab88', A: '#eeeae2', E: '#e0a030', M: '#9a6e5c', H: '#26283a', C: '#d9922e', R: '#1d1f2a', Q: '#d9a83f', I: '#8e8e96',
  W: '#f7f4ec', D: '#0e0f16', B: '#3a2850', G: '#e8c25a', J: '#f2b33d', P: '#1a1b24', F: '#0f0f14', K: '#232634', X: '#d4cec2' } };

// The face (8 x 8, the viewer's view: his right on the left). The hat's brim hides the top row and shades the next; then
// the bushy brows, the eyes (W their whites), the nose, the moustache, the beard.
const FACE = ['AAAAAAAA', 'SSSSSSSS', 'AAAsSAAA', 'SWESSEWS', 'SSSssSSS', 'SAAAAAAS', 'AAAMMAAA', 'AAAAAAAA'];

/** His faces (client/MobFaces), as changes to rows of FACE: 2 the brows, 3 the eyes, 6 the mouth in the beard. */
const FACES = {
  calm: {}, blink: { 3: 'SssSSssS' }, left: { 3: 'SEWSSEWS' }, right: { 3: 'SWESSWES' }, mutter: { 6: 'AAAmmAAA' },
  angry: { 2: 'sAAAAAAs' }, angry_blink: { 2: 'sAAAAAAs', 3: 'SssSSssS' }, angry_left: { 2: 'sAAAAAAs', 3: 'SEWSSEWS' },
  angry_right: { 2: 'sAAAAAAs', 3: 'SWESSWES' }, shout: { 2: 'sAAAAAAs', 6: 'AAMEEMAA' },
};
const faceRow = (o, v) => ((FACES[o.face || 'calm'] || {})[v] || FACE[v]);

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
/** The same piece on his right (x > 0, as given) and mirrored on his left. */
const pair = (bone, part, name, from, to, paint, extra = {}) => {
  piece(bone, part, name + '_r', from, to, paint, extra);
  piece(bone, part, name + '_l', [-to[0], from[1], from[2]], [-from[0], to[1], to[2]], paint, extra.pivot ? { ...extra, pivot: [-extra.pivot[0], extra.pivot[1], extra.pivot[2]] } : extra);
};
/** Straw: woven rings (a darker line every other row) and spokes, the rim darker. */
const straw = (f, u, v, w, h) => {
  if (f === 'up' || f === 'down') return u === 0 || v === 0 || u === w - 1 || v === h - 1 ? 'h' : (u + v) % 3 === 0 || (u - v + 99) % 5 === 0 ? 'h' : 'H';
  return v === h - 1 ? 'h' : u % 2 === 0 ? 'H' : 'h';
};
/** A raven embroidered in gold between his shoulders, its wings spread. */
const CLOUD_BACK = ['RRRQQRRR', 'QRQQQQRQ', 'QQQQQQQQ', 'RQQQQQQR', 'RRRQQRRR', 'RRQRRQRR'];

// ---- head: the face, white hair at the sides and back
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') return faceRow(o, v)[u];
  if (f === 'up') return (u + v) % 3 === 0 ? 'X' : 'A';
  if (f === 'down') return 'A';
  // White hair all over the back of the head, falling to the neck.
  if (f === 'south') return (u + v) % 3 === 0 ? 'X' : 'A';
  // A side, `front` counted from the face: the beard low at the front, the cheek, the ear, the crown of hair behind.
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 1) return 'A';
  if (v >= 5 && front <= 2) return 'A';
  if (front >= 5 && v >= 3) return (u + v) % 3 === 0 ? 'X' : 'A';
  if (front === 3 && (v === 3 || v === 4)) return v === 3 ? 'S' : 's';
  return 'S';
});
// The bushy brows, their outer ends drooping past the temples.
pair('Head', 'head', 'brow', [0.7, 28.9, -4.55], [3.9, 29.9, -3.9], (f, u, v) => ((u + v) % 3 === 0 ? 'X' : 'A'));
pair('Head', 'head', 'brow_end', [3.6, 27.6, -4.4], [4.7, 29.5, -3.5], (f, u, v, w, h) => (v === h - 1 ? 'X' : 'A'));
// The moustache: under the nose, then two long strands drooping past the chin.
pair('Head', 'head', 'moustache', [0.3, 26.1, -4.6], [2.6, 26.9, -3.95], (f, u) => (u % 3 === 0 ? 'X' : 'A'));
pair('Head', 'head', 'moustache_strand', [1.8, 20.6, -4.75], [2.6, 26.1, -4.15], (f, u, v, w, h) => (v === h - 1 ? 'X' : 'A'));
// The beard, in steps down to the sash, tied with an amber ring.
piece('Head', 'head', 'beard_top', [-3.0, 23.0, -4.6], [3.0, 26.1, -3.4], (f, u, v) => ((u + v) % 3 === 0 ? 'X' : 'A'));
piece('Head', 'head', 'beard_mid', [-2.3, 19.2, -4.5], [2.3, 23.0, -3.5], (f, u, v) => (u % 3 === 1 ? 'X' : 'A'));
piece('Head', 'head', 'beard_ring', [-1.7, 18.6, -4.65], [1.7, 19.5, -3.35], (f, u) => (u % 2 ? 'c' : 'C'));
piece('Head', 'head', 'beard_low', [-1.5, 15.8, -4.35], [1.5, 18.6, -3.6], (f, u, v) => (u % 3 === 1 ? 'X' : 'A'));
piece('Head', 'head', 'beard_tip', [-0.7, 14.5, -4.15], [0.7, 15.8, -3.75], all('A'));

// ---- the topknot on the crown, a gold pin through it, and long white hair falling behind to the shoulders
piece('Head', 'hat', 'topknot', [-1.3, 32, -0.6], [1.3, 34.2, 1.8], (f, u, v) => ((u + v) % 3 === 0 ? 'X' : 'A'));
piece('Head', 'hat', 'topknot_tie', [-1.45, 32.2, -0.75], [1.45, 32.8, 1.95], all('C'));
piece('Head', 'hat', 'hairpin', [-2.6, 33.2, 0.4], [2.6, 33.6, 0.8], (f, u) => (u === 0 ? 'g' : 'G'));
piece('Head', 'hat', 'hair_back', [-4.2, 22.4, 3.9], [4.2, 31.6, 4.6], (f, u, v, w, h) => (f === 'up' ? 'A' : v === h - 1 && u % 2 ? null : (u + v) % 3 === 0 ? 'X' : 'A'));

// ---- body: the robe crossed right over left, edged in white and sky blue, the white under-robe at the neck
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w, h) => {
  if (f === 'north') return (v <= 1 && u >= 3 && u <= 4) ? 'I' : u === 0 || u === w - 1 ? 'r' : 'R';
  if (f === 'south') return CLOUD_BACK[Math.min(v, 5)][u];
  if (f === 'up') return 'R';
  return 'R';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v, w) => (f === 'down' ? 'P' : isSide(f) && (u === 0 || u === w - 1) && f !== 'north' ? 'r' : 'R'),
  { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar', [-2.6, 23.3, -2.4], [2.6, 24.4, 2.4], rim((f, u) => (u % 2 ? 'I' : 'W')));
// The right panel over the left: the white edge, then a sky blue band beside it, stepping down to the right side.
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.6]].forEach(([x, y], i) => {
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.3, y + 1.0, -2.0], all('W'));
  piece('Chest', 'jacket', 'lapel_band_' + i, [x - 0.9, y - 0.2, -2.3], [x, y + 1.0, -2.0], all('Q'));
});
piece('Torso', 'jacket', 'front_band', [2.9, 13.0, -2.3], [3.9, 18.7, -2.0], all('Q'));
piece('Torso', 'jacket', 'front_edge', [3.9, 13.0, -2.45], [4.2, 18.7, -2.0], all('W'));
// The sash: violet, an amber stripe, a gold clasp; its knot and tails on his left; the amber stone on his right.
piece('Torso', 'jacket', 'sash', [-4.5, 15.4, -2.5], [4.5, 17.6, 2.5], rim((f, u, v) => (v === 1 && isSide(f) ? 'C' : (u + v) % 5 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_clasp', [-1.3, 15.6, -3.0], [1.3, 17.4, -2.45], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 'C' : 'G'));
piece('Torso', 'jacket', 'sash_knot', [-3.9, 15.0, -3.3], [-2.0, 17.9, -2.5], (f, u, v) => (v === 1 ? 'b' : 'B'));
piece('Torso', 'jacket', 'sash_tail_a', [-3.7, 9.6, -3.5], [-2.9, 15.0, -3.1], fringe('B'));
piece('Torso', 'jacket', 'sash_tail_b', [-2.7, 10.4, -3.5], [-1.9, 15.0, -3.1], fringe('B'));
piece('Torso', 'jacket', 'pendant_cord', [2.35, 12.4, -3.05], [2.65, 15.4, -2.75], all('C'));
piece('Torso', 'jacket', 'pendant_jade', [1.7, 11.0, -3.25], [3.3, 12.4, -2.55], (f, u, v) => (f === 'north' && u === 1 && v === 0 ? 'W' : (u + v) % 3 === 0 ? 'j' : 'J'));
piece('Torso', 'jacket', 'pendant_tassel', [2.15, 8.6, -3.1], [2.85, 11.0, -2.7], fringe('C'));
// The messenger's tube across his back, ringed in gold, on its strap.
piece('Chest', 'jacket', 'scabbard', [-3.6, 19.6, 2.4], [3.6, 21.0, 3.8], (f, u, v, w, h) => (u <= 0 || u === Math.floor(w / 2) || u >= w - 1 ? 'G' : (u + v) % 6 === 0 ? 'k' : 'K'),
  { tilt: -14, pivot: [0, 20.3, 3.1] });
piece('Chest', 'jacket', 'scabbard_strap', [-4.3, 21.2, 2.0], [4.3, 22.0, 2.3], all('B'));

// ---- the mantle of black feathers: two layers over the chest and the back, their lower edge feathered
const FEATHERS = (f, u, v, w, h) => (f === 'up' ? 'H' : f === 'down' ? null : isSide(f) && v === h - 1 && u % 2 ? null : (u + v) % 4 === 0 ? 'h' : 'H');
piece('Chest', 'jacket', 'mantle', [-4.9, 20.6, -2.9], [4.9, 24.6, 2.9], FEATHERS);
piece('Chest', 'jacket', 'mantle_low', [-4.6, 19.4, -2.6], [4.6, 20.6, 2.6], (f, u, v, w, h) => (f === 'up' ? null : f === 'down' ? null : u % 2 ? 'h' : null));
pair('Arm_R', 'rightSleeve', 'mantle_shoulder', [3.7, 20.4, -2.8], [8.8, 24.7, 2.8], FEATHERS);

// ---- arms: wide sleeves with sky-blue cuffs falling over the hands, their backs hanging lower
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [a0, a1] = xs(4, 8), [h0, h1] = xs(4.4, 7.6), [s0, s1] = xs(3.4, 8.6);
  const s = side.toLowerCase();
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, 17, -2], [a1, 24, 2], (f, u, v, w, h) => (f === 'south' && u === Math.floor(w / 2) ? 'r' : 'R'),
    { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'hand_' + s, [h0, 10.6, -1.7], [h1, 13.2, 1.7], (f, u, v) => (f === 'down' ? 's' : v === 0 ? 'Q' : 'S'));
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_' + s, [s0, 12.2, -2.7], [s1, 17.0, 2.7],
    rim((f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'W' : isSide(f) && v === h - 2 ? 'Q' : isSide(f) && u % 3 === 2 ? 'r' : 'R')),
    { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_hang_' + s, [s0, 10.4, 0.7], [s1, 12.2, 2.7],
    (f, u, v, w, h) => (f === 'up' ? null : isSide(f) && v === h - 1 ? 'W' : f === 'down' ? 'r' : 'R'));
}

// ---- legs: the robe's skirt split over each leg, hemmed in gold; trousers, socks, cloth shoes
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [t0, t1] = xs(0, 4), [u0, u1] = xs(-0.3, 4.3), [l0, l1] = xs(-0.55, 4.55), [d0, d1] = xs(-0.2, 4.2);
  const s = side.toLowerCase();
  // The inner side (toward the other leg) is left open.
  const inner = sx > 0 ? 'west' : 'east';
  const open = (f, u, v, w, h) => f === inner || f === 'up' || (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], (f, u, v, w, h) => (v >= h - 3 ? 'I' : 'P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [u0, 6.6, -2.4], [u1, 13.2, 2.4],
    (f, u, v, w, h) => (open(f, u, v, w, h) ? null : u % 3 === 2 ? 'r' : 'R'));
  piece('Leg_' + side, part + 'Pants', 'skirt_hem_' + s, [l0, 2.6, -2.65], [l1, 6.6, 2.65], (f, u, v, w, h) => {
    if (open(f, u, v, w, h)) return null;
    if (v === h - 1) return 'Q';
    if (v === h - 2) return u % 4 === 1 ? 'W' : 'Q';
    return u % 3 === 2 ? 'r' : 'R';
  });
  piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.8], [d1, 1.1, 2.2], (f, u, v, w, h) => (f === 'down' ? 'f' : isSide(f) && v === h - 1 ? 'I' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, SENSEI, ATLAS));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'crow_master'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'crow_master.png'), atlas);
  // One atlas per face; the calm face keeps the plain name.
  for (const face of Object.keys(FACES)) if (face !== 'calm')
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'crow_master_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...SENSEI, face }, ATLAS)));
  fs.writeFileSync(path.join(art, 'crow_master', 'master.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'crow_master.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'crow_master.bbmodel'), JSON.stringify(kit.bbmodel('crow_master', PIECES, [SENSEI], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Crow Master:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, SENSEI, FACES };
