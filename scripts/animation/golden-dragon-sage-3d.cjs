// The Golden Dragon in human form (docs/mobs/golden-dragon.md), "the one above all", on Epic Fight's biped skeleton,
// as Sensei Yun (sensei-3d.cjs). After Enzo's references: an ageless master, long white hair falling to his waist under
// a gold crown, a white beard as long, eyes that glow; here in the dragon's white gold and jade.
//   art/blockbench/golden_dragon_sage.bbmodel (+ golden_dragon_sage/sage.png)   the Blockbench project;
//   assets/murimblock/animmodels/entity/golden_dragon_sage.json                  the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/golden_dragon_sage.png                     its atlas.
// The look: a gold crown (guan) on a topknot pinned with jade; the white hair long behind, two locks before the
// shoulders; glowing jade eyes under white brows, a long moustache, the beard to his sash; an outer robe of deep jade
// crossed right over left, edged in white gold, a gold dragon coiling on its back and a small one on the breast; a white fur
// collar about the shoulders; wide sleeves edged in white gold and gold; a white-gold sash with a gold clasp and a white jade
// pendant; the robe long to his ankles, split in front; dark jade trousers, white socks, black shoes. (16/10: deep jade
// outside, white gold within, so he stands out; the plain white-gold robe looked dull.)
// Usage: node scripts/animation/golden-dragon-sage-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;
const NAME = 'golden_dragon_sage';

/**
 * Colour letters: S skin, A white hair and beard, X its shadowed strands, E the eyes' glow, W white (the eyes' rims, fur,
 * socks), M mouth, R the deep jade robe, Q white gold (edges, under-robe), D the robe's shadows, G gold, B the white-gold sash, J white jade,
 * P trousers, F shoes, U fur shadow.
 */
const SAGE = { key: 'sage', c: {
  S: '#ecd2b4', A: '#f6f4ef', X: '#d6d1c6', E: '#8affdc', W: '#ffffff', M: '#b88a74', R: '#1f7a58', Q: '#f1e3b6', D: '#145440',
  G: '#f0c24a', B: '#e8d9a8', J: '#dff4ea', P: '#1d4f3e', F: '#1c1c1a', U: '#e2ddd2' } };

// The face (8 x 8, the viewer's view): the hair's parting above, white brows, glowing eyes, the moustache, the beard.
const FACE = ['AAAssAAA', 'AsSSSSsA', 'AAASSAAA', 'SWEssEWS', 'SSSssSSS', 'SAAAAAAS', 'AAAMMAAA', 'AAAAAAAA'];
const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
const pair = (bone, part, name, from, to, paint, extra = {}) => {
  piece(bone, part, name + '_r', from, to, paint, extra);
  piece(bone, part, name + '_l', [-to[0], from[1], from[2]], [-from[0], to[1], to[2]], paint, extra.pivot ? { ...extra, pivot: [-extra.pivot[0], extra.pivot[1], extra.pivot[2]] } : extra);
};
const hair = (f, u, v) => ((u * 2 + v) % 5 === 0 ? 'X' : 'A');
/** The gold dragon coiling up his back (8 wide), on the white-gold robe. */
const DRAGON_BACK = ['RRGGRRRR', 'RGRRGRRR', 'RRRRGRRR', 'RRRGRRRR', 'RRGRRGGR', 'RGRRGRRG', 'RRGGRRRG', 'RRRRRGGR', 'RRRRGRRR', 'RRRGRRRR', 'RRGRRRRR'];

// ---- head: the face, the hair over the head and down the back
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h) => {
  if (f === 'north') return FACE[v][u];
  if (f === 'up' || f === 'down') return 'A';
  if (f === 'south') return hair(f, u, v);
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 3 || front >= 3) return hair(f, u, v);
  if (v >= 5 && front <= 1) return 'A';
  return front === 2 && v === 4 ? 's' : 'S';
});
pair('Head', 'head', 'brow', [0.6, 28.9, -4.55], [3.8, 29.8, -3.9], hair);
pair('Head', 'head', 'moustache', [0.3, 26.1, -4.6], [2.7, 26.9, -3.95], hair);
pair('Head', 'head', 'moustache_strand', [2.0, 19.6, -4.75], [2.8, 26.1, -4.15], (f, u, v, w, h) => (v === h - 1 ? 'X' : 'A'));
// The beard, long and full, to the sash.
piece('Head', 'head', 'beard_top', [-3.3, 22.4, -4.65], [3.3, 26.1, -3.4], hair);
piece('Head', 'head', 'beard_mid', [-2.8, 18.4, -4.6], [2.8, 22.4, -3.5], hair);
piece('Head', 'head', 'beard_low', [-2.0, 14.8, -4.45], [2.0, 18.4, -3.6], hair);
piece('Head', 'head', 'beard_tip', [-1.0, 13.2, -4.25], [1.0, 14.8, -3.75], all('A'));
// The hair falling behind to the waist, and a lock before each shoulder.
piece('Head', 'hat', 'hair_back', [-4.3, 19.0, 3.6], [4.3, 31.0, 4.6], (f, u, v, w, h) => (v === h - 1 ? 'X' : hair(f, u, v)));
pair('Head', 'hat', 'hair_lock', [3.6, 17.5, -3.2], [4.6, 27.0, -1.6], (f, u, v, w, h) => (v === h - 1 ? 'X' : hair(f, u, v)));
// The topknot and the gold crown pinned with jade.
piece('Head', 'hat', 'topknot', [-1.4, 32.0, -0.8], [1.4, 33.6, 2.0], hair);
piece('Head', 'hat', 'crown', [-1.9, 32.6, -1.2], [1.9, 35.0, 2.4], (f, u, v, w, h) => (v === 0 || v === h - 1 ? 'g' : (u + v) % 4 === 0 ? 'g' : 'G'));
piece('Head', 'hat', 'crown_pin', [-3.4, 33.6, 0.3], [3.4, 34.1, 0.8], (f, u) => (u === 0 || u >= 12 ? 'J' : 'B'));

// ---- body: the white-gold robe crossed right over left, edged in jade, the gold dragon on the back
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w, h) => {
  if (f === 'north') return u === 0 || u === w - 1 ? 'D' : 'R';
  if (f === 'south') return DRAGON_BACK[Math.min(v, 10)][u];
  return 'R';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v, w) => (f === 'down' ? 'P' : f === 'south' ? DRAGON_BACK[Math.min(v + 6, 10)][u] : 'R'),
  { hinge: { y: 18, bone: 'Chest' } });
// The white fur collar about the shoulders.
piece('Chest', 'jacket', 'fur', [-4.6, 22.6, -2.7], [4.6, 24.6, 2.7], (f, u, v) => ((u + v) % 3 === 0 ? 'U' : 'W'));
// The lapels: a jade band stepping down to his right; a small gold dragon on his breast.
[[-1.8, 21.6], [-0.6, 20.6], [0.6, 19.6], [1.8, 18.6], [3.0, 17.6]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.4], [x + 1.2, y + 1.0, -2.0], all(i % 2 ? 'Q' : 'q')));
piece('Chest', 'jacket', 'breast_dragon', [-3.4, 19.4, -2.35], [-1.6, 21.6, -2.0], (f, u, v) => ((u + v) % 2 ? 'G' : 'g'));
piece('Torso', 'jacket', 'front_band', [2.9, 13.0, -2.3], [3.9, 17.7, -2.0], all('Q'));
// The sash: jade, a gold clasp, a white jade pendant on his right.
piece('Torso', 'jacket', 'sash', [-4.5, 15.4, -2.5], [4.5, 17.6, 2.5], rim((f, u, v) => (v === 1 && isSide(f) ? 'G' : (u + v) % 5 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_clasp', [-1.4, 15.6, -3.0], [1.4, 17.4, -2.45], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 'J' : 'G'));
piece('Torso', 'jacket', 'sash_tail', [-3.6, 9.4, -3.4], [-2.6, 15.4, -3.0], fringe('B'));
piece('Torso', 'jacket', 'pendant_cord', [2.35, 12.6, -3.05], [2.65, 15.4, -2.75], all('G'));
piece('Torso', 'jacket', 'pendant_jade', [1.6, 11.0, -3.25], [3.4, 12.6, -2.55], (f, u, v) => (f === 'north' && u === 1 && v === 0 ? 'W' : 'J'));
piece('Torso', 'jacket', 'pendant_tassel', [2.15, 8.6, -3.1], [2.85, 11.0, -2.7], fringe('G'));

// ---- arms: wide sleeves, white gold, edged in jade then gold
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [a0, a1] = xs(4, 8), [h0, h1] = xs(4.4, 7.6), [s0, s1] = xs(3.3, 8.7);
  const s = side.toLowerCase();
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, 17, -2], [a1, 24, 2], (f, u, v, w) => (f === 'south' && u === Math.floor(w / 2) ? 'D' : 'R'),
    { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'hand_' + s, [h0, 10.6, -1.7], [h1, 13.2, 1.7], (f, u, v) => (f === 'down' ? 's' : v === 0 ? 'Q' : 'S'));
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_' + s, [s0, 12.0, -2.8], [s1, 17.0, 2.8],
    rim((f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'G' : isSide(f) && v === h - 2 ? 'Q' : isSide(f) && u % 3 === 2 ? 'D' : 'R')),
    { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_hang_' + s, [s0, 9.8, 0.6], [s1, 12.0, 2.8],
    (f, u, v, w, h) => (f === 'up' ? null : isSide(f) && v === h - 1 ? 'G' : f === 'down' ? 'D' : 'R'));
}

// ---- legs: the robe long to the ankles, split in front, edged in jade and gold; trousers, socks, black shoes
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [t0, t1] = xs(0, 4), [u0, u1] = xs(-0.3, 4.3), [l0, l1] = xs(-0.55, 4.55), [d0, d1] = xs(-0.2, 4.2);
  const s = side.toLowerCase();
  const inner = sx > 0 ? 'west' : 'east';
  const open = (f, u, v, w, h) => f === inner || f === 'up' || (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], (f, u, v, w, h) => (v >= h - 2 ? 'W' : 'P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [u0, 6.6, -2.4], [u1, 13.2, 2.4], (f, u, v, w, h) => (open(f, u, v, w, h) ? null : u % 3 === 2 ? 'D' : 'R'));
  piece('Leg_' + side, part + 'Pants', 'skirt_hem_' + s, [l0, 1.4, -2.65], [l1, 6.6, 2.65], (f, u, v, w, h) => {
    if (open(f, u, v, w, h)) return null;
    if (v === h - 1) return 'G';
    if (v === h - 2) return 'Q';
    return u % 3 === 2 ? 'D' : 'R';
  });
  piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.8], [d1, 1.1, 2.2], (f, u, v, w, h) => (f === 'down' ? 'f' : isSide(f) && v === h - 1 ? 'W' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, SAGE, ATLAS));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, NAME), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', NAME + '.png'), atlas);
  fs.writeFileSync(path.join(art, NAME, 'sage.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', NAME + '.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, NAME + '.bbmodel'), JSON.stringify(kit.bbmodel(NAME, PIECES, [SAGE], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Golden Dragon Sage:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, SAGE };
