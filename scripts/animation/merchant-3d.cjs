// The Alliance Merchant (盟商), who trades in silver taels for the Orthodox Alliance, in 3D on Epic Fight's biped
// skeleton. A well-fed, shrewd and smiling trader in the Alliance's azure and pale gold, all in cloth (his wares and any
// armour are items of their own): a black gauze cap with a gold bead, a 16 x 16 face (hd-face.cjs) with narrow eyes and
// a long thin moustache, an azure silk robe brocaded with gold coins and edged in pale gold, crossed right over left over
// a white under-robe, wide sleeves, a pale gold sash over his paunch with a red cloth purse at his left hip, the robe's
// skirt split over each leg, indigo trousers and black cloth shoes.
//   art/blockbench/alliance_merchant.bbmodel (+ alliance_merchant/merchant.png)   the Blockbench project;
//   assets/murimblock/animmodels/entity/alliance_merchant.json                     the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/alliance_merchant.png (+ one per face)       its atlases (client/MobFaces).
// Usage: node scripts/animation/merchant-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/**
 * Colour letters: S skin, A hair, K black gauze (cap), G pale gold, R azure robe, Q light azure, I white under-robe,
 * B sash, P trousers, F shoes, C red purse, D dark brown cord.
 */
const MERCHANT = { key: 'merchant', c: {
  S: '#e2b48a', A: '#1a1512', K: '#1c1b20', G: '#E9D9A6', R: '#2B5FA8', Q: '#5d8fd6', I: '#efeae0', B: '#d8c27e',
  P: '#22324e', F: '#1b1715', C: '#a8322c', D: '#4a2e1c' } };

/** His face: shrewd narrow eyes, a merchant's smile, a long thin moustache; the cap hides most of his hair. */
const FACE = { skin: '#e2b48a', hair: '#1a1512', iris: '#3a2616',
  fringe: [9, 6, 3, 3, 3, 2, 2, 2, 2, 2, 2, 3, 3, 3, 6, 9], shine: [[4, 1], [11, 1]],
  brow: 'soft', eyes: 'narrow', mouth: 'smile', moustache: 'long', blush: true };

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });

/** Azure silk brocaded with small gold coins (a ring with a square hole), every few texels. */
const brocade = (l = 'R') => (f, u, v) => {
  if (!isSide(f)) return l;
  const cu = (u + (Math.floor(v / 4) % 2) * 2) % 4, cv = v % 4;
  return cu === 1 && cv === 1 ? 'G' : (u + v) % 7 === 0 ? l.toLowerCase() : l;
};

// ---- head: hair at the sides and back under the cap; the face on its plate
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h) => {
  if (f === 'north') return 'S';
  if (f === 'up') return 'A';
  if (f === 'down') return 'S';
  if (f === 'south') return v < 6 ? 'A' : 'a';
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 3) return 'A';
  if (front >= 4) return (u + v) % 3 === 0 ? 'a' : 'A';
  if (front === 3 && (v === 3 || v === 4)) return v === 3 ? 'S' : 's';
  return 'S';
});
piece('Head', 'head', 'face', ...hd.PLATE, hd.paint(FACE), { density: hd.DENSITY });
// The cap: black gauze, a raised crown, a gold bead at the front and two short flaps behind.
piece('Head', 'hat', 'cap', [-4.4, 30.2, -4.4], [4.4, 32.6, 4.4], (f, u, v, w, h) => (f === 'down' ? null : isSide(f) && v === h - 1 ? 'k' : 'K'));
piece('Head', 'hat', 'cap_crown', [-3.0, 32.6, -2.2], [3.0, 34.4, 3.4], (f, u, v) => ((u + v) % 4 === 0 ? 'k' : 'K'));
piece('Head', 'hat', 'cap_bead', [-0.7, 30.6, -4.9], [0.7, 32.0, -4.4], (f, u, v) => (v === 0 ? 'G' : 'g'));
for (const x of [-2.4, 1.2]) piece('Head', 'hat', 'cap_flap_' + (x < 0 ? 'l' : 'r'), [x, 27.4, 4.4], [x + 1.2, 31.0, 4.9], (f, u, v, w, h) => (v === h - 1 ? 'k' : 'K'));

// ---- body: the brocaded robe crossed right over left, the white under-robe at the neck, a paunch under the sash
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w, h) => {
  if (f === 'north' && v <= 1 && u >= 3 && u <= 4) return 'I';
  return brocade()(f, u, v);
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v) => (f === 'down' ? 'P' : brocade()(f, u, v)), { hinge: { y: 18, bone: 'Chest' } });
piece('Torso', 'jacket', 'paunch', [-3.4, 13.4, -2.9], [3.4, 18.4, -2.0], (f, u, v) => (f === 'up' || f === 'down' ? 'R' : brocade()(f, u, v)));
piece('Chest', 'jacket', 'collar', [-2.6, 23.3, -2.4], [2.6, 24.4, 2.4], rim((f, u) => (u % 2 ? 'G' : 'g')));
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.6]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.3, y + 1.0, -2.0], (f, u) => (u === 0 ? 'g' : 'G')));
piece('Torso', 'jacket', 'front_edge', [3.0, 13.0, -3.05], [3.6, 18.5, -2.85], all('G'));
// The sash: pale gold silk round the paunch, its knot in front; the red purse on its cord at his left hip.
piece('Torso', 'jacket', 'sash', [-4.5, 15.6, -3.2], [4.5, 17.4, 2.5], rim((f, u, v) => (isSide(f) && (u + v) % 5 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_knot', [-1.0, 15.3, -3.6], [1.0, 17.7, -3.1], (f, u, v) => (v === 1 ? 'b' : 'B'));
piece('Torso', 'jacket', 'sash_tail_a', [-0.9, 11.6, -3.6], [-0.1, 15.3, -3.25], fringe('B'));
piece('Torso', 'jacket', 'sash_tail_b', [0.2, 12.2, -3.6], [1.0, 15.3, -3.25], fringe('B'));
piece('Torso', 'jacket', 'purse_cord', [-4.0, 13.8, -2.6], [-3.7, 15.6, -2.3], all('D'));
piece('Torso', 'jacket', 'purse', [-4.9, 11.4, -3.0], [-3.1, 14.0, -1.6], (f, u, v, w, h) => (v === 0 ? 'D' : f === 'north' && u === 1 && v === 2 ? 'G' : (u + v) % 3 === 0 ? 'c' : 'C'));

// ---- arms: wide brocaded sleeves edged in pale gold, the backs hanging lower
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [a0, a1] = xs(4, 8), [h0, h1] = xs(4.4, 7.6), [s0, s1] = xs(3.4, 8.6);
  const s = side.toLowerCase();
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, 17, -2], [a1, 24, 2], brocade(), { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'hand_' + s, [h0, 10.6, -1.7], [h1, 13.2, 1.7], (f, u, v) => (f === 'down' ? 's' : v === 0 ? 'I' : 'S'));
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_' + s, [s0, 12.2, -2.7], [s1, 17.0, 2.7],
    rim((f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'G' : isSide(f) && v === h - 2 ? 'Q' : brocade()(f, u, v))), { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'sleeve_hang_' + s, [s0, 10.4, 0.7], [s1, 12.2, 2.7],
    (f, u, v, w, h) => (f === 'up' ? null : isSide(f) && v === h - 1 ? 'G' : f === 'down' ? 'r' : 'R'));
}

// ---- legs: the robe's skirt split over each leg, hemmed in pale gold; indigo trousers, black cloth shoes
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [t0, t1] = xs(0, 4), [u0, u1] = xs(-0.3, 4.3), [l0, l1] = xs(-0.55, 4.55), [d0, d1] = xs(-0.2, 4.2);
  const s = side.toLowerCase();
  const inner = sx > 0 ? 'west' : 'east';
  const open = (f, u, v, w, h) => f === inner || f === 'up' || (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], all('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [u0, 6.6, -2.4], [u1, 13.2, 2.4], (f, u, v, w, h) => (open(f, u, v, w, h) ? null : brocade()(f, u, v)));
  piece('Leg_' + side, part + 'Pants', 'skirt_hem_' + s, [l0, 3.2, -2.65], [l1, 6.6, 2.65], (f, u, v, w, h) => {
    if (open(f, u, v, w, h)) return null;
    if (v === h - 1) return 'G';
    return u % 3 === 2 ? 'r' : 'R';
  });
  piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.8], [d1, 1.1, 2.2], (f, u, v, w, h) => (f === 'down' ? 'f' : isSide(f) && v === h - 1 ? 'G' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, MERCHANT, ATLAS));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'alliance_merchant'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'alliance_merchant.png'), atlas);
  // One atlas per face; the calm face keeps the plain name.
  for (const face of Object.keys(hd.EXPRESSIONS)) if (face !== 'calm')
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'alliance_merchant_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...MERCHANT, face }, ATLAS)));
  fs.writeFileSync(path.join(art, 'alliance_merchant', 'merchant.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'alliance_merchant.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'alliance_merchant.bbmodel'), JSON.stringify(kit.bbmodel('alliance_merchant', PIECES, [MERCHANT], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Alliance Merchant:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, MERCHANT, FACE };
