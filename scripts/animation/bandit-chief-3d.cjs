// The Bandit Chief (山大王), the mountain bandits' leader and the first mini-boss, in 3D on Epic Fight's biped
// skeleton (drawn 1.3 times larger in game): a huge brute with an immense two-handed axe.
//   art/blockbench/bandit_chief.bbmodel (+ bandit_chief/chief.png)   the Blockbench project, to look at and edit;
//   assets/murimblock/animmodels/entity/bandit_chief.json            the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/bandit_chief.png               its atlas.
// The look: a shaved head with a hair knot and a long tail of hair, a braided beard with a gold bead, a scar across the
// left eye, gold rings in the ears; a leather vest open on a bare chest and a heavy belly; a wide studded belt with a
// bronze tiger-face buckle and lamellar tassets; a tiger skin over the back and the left shoulder, its head on the
// shoulder and a paw hanging on the chest; an iron pauldron with a bronze spike on the right shoulder; bare tattooed
// arms with iron bracers; baggy crimson trousers puffing over fur-cuffed boots.
// Usage: node scripts/animation/bandit-chief-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, all, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/**
 * Colour letters: S skin, A hair and beard, E eyes, W whites (eyes, tiger's muzzle, fangs), Y scar, M lips, T tattoo,
 * V vest, R trousers, D dark trim, B belt, G bronze, I iron, O tiger orange, K tiger stripes, F boots, U fur, N red cord.
 */
const CHIEF = { key: 'chief', c: {
  S: '#b07a4c', A: '#1c1714', E: '#120e0c', W: '#e8dcc6', Y: '#8a3a2e', M: '#7a4a34', T: '#2d3a4a', V: '#4a2e1e', R: '#6e1f1c',
  D: '#2a1a14', B: '#1e1612', G: '#c39a3c', I: '#8b9198', O: '#d07a28', K: '#1e1410', F: '#3a2618', U: '#8c7a62', N: '#a7302a' } };

// The face (8 x 8, the viewer's view: the chief's right on the left): heavy brows, a scar down through his left eye,
// a moustache and the beard.
const FACE = ['ssssssss', 'SSSSSSYS', 'AAASSAYA', 'SWESSEYS', 'SSSssSYS', 'SAAMMAAS', 'AAAAAAAA', 'AAAAAAAA'];

/**
 * His faces (client/MobFaces picks one each tick), as changes to rows of FACE: 2 the brows (they knot in anger), 3 the
 * eyes (the scarred left eye never moves), 5 the mouth under the moustache (it mutters, and bares the teeth in a shout).
 */
const FACES = {
  calm: {}, blink: { 3: 'SssSSsYS' }, left: { 3: 'SEWSSEYS' }, right: { 3: 'SWESSEYS' }, mutter: { 5: 'SAAeeAAS' },
  angry: { 2: 'sAAAAAYA' }, angry_blink: { 2: 'sAAAAAYA', 3: 'SssSSsYS' }, angry_left: { 2: 'sAAAAAYA', 3: 'SEWSSEYS' },
  angry_right: { 2: 'sAAAAAYA' }, shout: { 2: 'sAAAAAYA', 5: 'SAWWWWAS' },
};

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
/** Tiger fur: orange, wavy black bands across it (broken here and there), the last row white. */
const WAVE = [0, 0, 1, 1, 1, 0];
const tiger = (f, u, v, w, h) => {
  if (isSide(f) && v === h - 1) return 'W';
  const band = (v + WAVE[u % 6]) % 4;
  return band === 0 && u % 7 !== 3 ? 'K' : band === 1 && u % 3 === 0 ? 'o' : 'O';
};
/** Iron plate: rivets along the top row, a dark lower edge. */
const plate = (f, u, v, w, h) => (isSide(f) && v === 0 && u % 2 ? 'G' : isSide(f) && v === h - 1 ? 'i' : 'I');

// ---- head: shaved, the hair knot and its tail, the braided beard, gold rings
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') return ((FACES[o.face || 'calm'] || {})[v] || FACE[v])[u];
  if (f === 'up') return (u + v) % 4 === 0 ? 's' : 'S';
  if (f === 'down') return 'A';
  if (f === 'south') return v > 5 ? 's' : 'S';
  const front = f === 'east' ? w - 1 - u : u;
  if (v >= 5 && front <= 2) return 'A';
  if (front === 3 && (v === 3 || v === 4)) return 's';
  return 'S';
});
piece('Head', 'hat', 'hair_knot', [-1.4, 32, -0.2], [1.4, 34.4, 2.6], (f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'N' : (u + v) % 2 ? 'a' : 'A'));
piece('Head', 'hat', 'hair_tail', [-0.9, 24.8, 4.0], [0.9, 33.0, 5.2], (f, u, v, w, h) => (v === 1 ? 'N' : v === h - 1 ? 'a' : 'A'),
  { tilt: -4, pivot: [0, 33.0, 4.6] });
piece('Head', 'head', 'beard', [-3.2, 21.8, -4.6], [3.2, 25.0, -2.4], (f, u, v, w, h) => ((u + v) % 3 === 0 ? 'a' : 'A'));
piece('Head', 'head', 'beard_braid', [-1.0, 19.6, -4.4], [1.0, 21.8, -3.0], (f, u, v) => (v % 2 ? 'a' : 'A'));
piece('Head', 'head', 'beard_bead', [-1.2, 20.9, -4.6], [1.2, 21.5, -2.8], all('G'));
piece('Head', 'head', 'ring_r', [4.0, 25.2, -0.7], [4.5, 26.6, 0.7], (f, u, v) => (u + v) % 2 ? 'g' : 'G');
piece('Head', 'head', 'ring_l', [-4.5, 25.2, -0.7], [-4.0, 26.6, 0.7], (f, u, v) => (u + v) % 2 ? 'g' : 'G');

// ---- body: the bare chest in the open vest, the belly, the belt and its tiger buckle
piece('Chest', 'torso', 'traps', [-5.6, 22.6, -2.0], [5.6, 24.6, 2.8], rim((f, u) => (f === 'north' && u > 2 && u < 8 ? 'S' : 'V')));
piece('Chest', 'torso', 'chest', [-5.2, 18, -2.8], [5.2, 24, 2.8], (f, u, v, w, h) => {
  if (f === 'north') {
    if (u <= 1 || u >= w - 2) return 'V';
    // The pecs, a tattooed stripe on the right one.
    if (v === 2 && u > 1 && u < w - 2) return 's';
    if (u === 3 && v >= 3 && v <= 4) return 'T';
    return 'S';
  }
  if (f === 'up') return 'V';
  return v === h - 1 ? 'v' : 'V';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-5.2, 12.6, -3.6], [5.2, 18, 2.8], (f, u, v, w, h) => {
  if (f === 'north') return u <= 1 || u >= w - 2 ? 'V' : u === 5 && v === 2 ? 's' : v === 0 ? 's' : 'S';
  if (f === 'down') return 'R';
  return 'V';
}, { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'vest_r', [2.2, 17.6, -3.2], [5.4, 24.2, -2.6], (f, u, v) => (f === 'north' && u === 0 ? 'D' : f === 'north' && v % 2 === 1 && u === 2 ? 'G' : 'V'));
piece('Chest', 'jacket', 'vest_l', [-5.4, 17.6, -3.2], [-2.2, 24.2, -2.6], (f, u, v, w) => (f === 'north' && u === w - 1 ? 'D' : f === 'north' && v % 2 === 1 && u === 0 ? 'G' : 'V'));
piece('Torso', 'jacket', 'belt', [-5.6, 12.2, -4.0], [5.6, 14.8, 3.1], rim((f, u, v, w, h) => (isSide(f) && v === 1 && u % 3 === 1 ? 'G' : 'B')));
piece('Torso', 'jacket', 'buckle', [-2.0, 11.6, -4.6], [2.0, 15.4, -3.9], (f, u, v) => {
  if (f !== 'north') return 'g';
  // A tiger's face in bronze: eyes, nose, a snarl.
  if (v === 1 && (u === 0 || u === 3)) return 'K';
  if (v === 2 && (u === 1 || u === 2)) return 'k';
  if (v === 3) return u % 2 ? 'W' : 'g';
  return 'G';
});
piece('Thigh_R', 'rightPants', 'tasset_r', [0.3, 7.4, -3.2], [4.4, 12.4, -2.6], (f, u, v) => (v % 2 ? 'i' : f === 'north' && u % 2 ? 'D' : 'I'));
piece('Thigh_L', 'leftPants', 'tasset_l', [-4.4, 7.4, -3.2], [-0.3, 12.4, -2.6], (f, u, v) => (v % 2 ? 'i' : f === 'north' && u % 2 ? 'D' : 'I'));

// ---- the tiger skin: over the back, over the left shoulder with its head, a paw hanging on the chest
piece('Chest', 'jacket', 'pelt_back', [-5.8, 12.8, 2.8], [5.6, 24.8, 3.6], tiger);
piece('Arm_L', 'leftSleeve', 'pelt_shoulder', [-9.6, 21.2, -3.4], [-3.2, 25.4, 3.4], rim(tiger));
piece('Arm_L', 'leftSleeve', 'tiger_head', [-9.4, 24.6, -4.6], [-4.4, 28.2, 0.8], (f, u, v, w, h) => {
  if (f === 'north') {
    // Seen from the front: stripes on the brow, yellow eyes, a white muzzle and a dark nose.
    if (v === 0) return u % 2 ? 'K' : 'O';
    if (v === 1) return u === 1 || u === 3 ? 'G' : 'O';
    if (v === 2) return u === 2 ? 'k' : u >= 1 && u <= 3 ? 'W' : 'O';
    return u >= 1 && u <= 3 ? 'W' : 'K';
  }
  if (f === 'down') return 'W';
  return (u + v) % 3 === 0 ? 'K' : 'O';
});
piece('Arm_L', 'leftSleeve', 'tiger_jaw', [-8.9, 23.8, -4.9], [-4.9, 24.6, -1.2], (f, u) => (f === 'north' ? (u % 2 ? 'W' : 'K') : 'O'));
piece('Arm_L', 'leftSleeve', 'tiger_ear_r', [-5.6, 28.2, -1.0], [-4.4, 29.4, 0.2], (f) => (f === 'north' ? 'K' : 'O'));
piece('Arm_L', 'leftSleeve', 'tiger_ear_l', [-9.4, 28.2, -1.0], [-8.2, 29.4, 0.2], (f) => (f === 'north' ? 'K' : 'O'));
piece('Chest', 'jacket', 'tiger_paw', [-5.0, 15.4, -3.6], [-3.4, 21.6, -2.8], (f, u, v, w, h) => (v === h - 1 ? (u % 2 ? 'W' : 'K') : tiger(f, u, v, w, h)));

// ---- the right shoulder: an iron pauldron in two plates and a bronze spike
piece('Arm_R', 'rightSleeve', 'pauldron', [3.2, 22.2, -3.2], [9.6, 25.6, 3.2], rim(plate));
piece('Arm_R', 'rightSleeve', 'pauldron_lame', [3.6, 20.6, -3.0], [9.4, 22.4, 3.0], rim((f, u, v, w, h) => (v === h - 1 ? 'i' : 'I')));
piece('Arm_R', 'rightSleeve', 'pauldron_spike', [5.8, 25.6, -0.6], [7.0, 27.2, 0.6], (f, u, v) => (v === 0 ? 'W' : 'G'));

// ---- arms: bare, tattooed, iron bracers
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [a0, a1] = xs(3.4, 8.6), [b0, b1] = xs(3.1, 8.9);
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + side.toLowerCase(), [a0, 16.6, -2.6], [a1, 24, 2.6],
    (f, u, v, w, h) => (f === 'up' || f === 'down' ? 'S' : v === 3 || (v === 4 && u % 2) ? 'T' : 'S'), { hinge: { y: 16.6, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'forearm_' + side.toLowerCase(), [a0, 10.4, -2.6], [a1, 16.6, 2.6],
    (f, u, v, w, h) => (f === 'down' ? 's' : v === h - 1 && isSide(f) ? (u % 2 ? 's' : 'S') : 'S'), { hinge: { y: 16.6, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'bracer_' + side.toLowerCase(), [b0, 11.6, -2.9], [b1, 15.8, 2.9],
    rim((f, u, v, w, h) => (v === 1 || v === h - 2 ? 'D' : isSide(f) && v === 2 && u % 2 ? 'G' : 'I')));
}

// ---- legs: baggy crimson trousers puffing over fur-cuffed boots
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [t0, t1] = xs(-0.4, 4.4), [p0, p1] = xs(-0.7, 4.7), [c0, c1] = xs(-0.6, 4.6);
  const s = side.toLowerCase();
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2.6], [t1, 12.6, 2.6], (f, u) => (isSide(f) && u % 3 === 2 ? 'r' : 'R'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2.6], [t1, 6, 2.6], (f, u, v) => (f === 'up' ? 'R' : v >= 2 ? 'F' : 'R'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Leg_' + side, part + 'Pants', 'puff_' + s, [p0, 3.8, -2.9], [p1, 6.2, 2.9], rim((f, u, v, w, h) => (v === h - 1 ? 'r' : u % 3 === 2 ? 'r' : 'R')));
  piece('Leg_' + side, part + 'Pants', 'cuff_' + s, [c0, 3.0, -2.8], [c1, 4.0, 2.8], rim((f, u) => (u % 2 ? 'u' : 'U')));
  piece('Leg_' + side, part + 'Pants', 'toe_' + s, [t0, 0, -3.6], [t1, 1.4, 2.6], (f, u, v, w, h) => (f === 'down' || v === h - 1 ? 'f' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, CHIEF, ATLAS));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'bandit_chief'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'bandit_chief.png'), atlas);
  // One atlas per face; the calm face keeps the plain name.
  for (const face of Object.keys(FACES)) if (face !== 'calm')
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'bandit_chief_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...CHIEF, face }, ATLAS)));
  fs.writeFileSync(path.join(art, 'bandit_chief', 'chief.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'bandit_chief.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'bandit_chief.bbmodel'), JSON.stringify(kit.bbmodel('bandit_chief', PIECES, [CHIEF], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Bandit Chief:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, CHIEF, FACES };
