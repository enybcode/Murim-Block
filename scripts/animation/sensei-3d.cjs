// Sensei Yun (雲, "cloud"), the old master of the Azure Cloud Sect who keeps the Misty Bamboo Temple, in 3D on Epic
// Fight's biped skeleton. After Sensei Wu: a small wise old man under a wide conical straw hat, with bushy white brows,
// a long drooping moustache and a white beard down to his sash; here in the sect's shades of blue.
//   art/blockbench/azure_sensei.bbmodel (+ azure_sensei/sensei.png)   the Blockbench project, to look at and edit;
//   assets/murimblock/animmodels/entity/azure_sensei.json             the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/azure_sensei.png (+ its faces)  its atlas.
// The look: the straw hat in five steps with an azure band and knob; an azure outer robe crossed right over left, its
// collar and lapels edged in white and sky blue over a white under-robe, a white cloud embroidered on the back; wide
// sleeves with sky-blue cuffs falling over the hands, the back of each sleeve hanging lower; an indigo sash with an azure
// stripe, a silver cloud clasp, its knot and tails on the left, a white jade pendant and its tassel on the right; the
// robe's skirt split over each leg down to the shins, hemmed in sky blue with white clouds; indigo trousers, white socks,
// dark blue cloth shoes; the empty lacquered scabbard of his sword across his back.
// Usage: node scripts/animation/sensei-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/**
 * Colour letters: S skin, A white hair, brows and beard, E eyes, M mouth, H straw, C azure (hat band, beard ring, cords),
 * R robe, Q sky blue (cuffs, hems), I white under-robe and socks, W cloud white (trim, embroidery), D navy (shadows),
 * B sash, G silver, J white jade, P trousers, F shoes, K scabbard lacquer, X the white hair's shadowed strands; W also
 * gives the whites of the eyes.
 */
const SENSEI = { key: 'sensei', c: {
  S: '#e0b994', A: '#efebe4', E: '#20232b', M: '#b0806a', H: '#d9b66c', C: '#3d8ee0', R: '#2f6fc0', Q: '#9ccaf0', I: '#e9f3fb',
  W: '#f7fbff', D: '#1e3768', B: '#22346a', G: '#cdd6de', J: '#d3ece3', P: '#25407a', F: '#17233f', K: '#1d3c7c', X: '#dcd6cc' } };

// The face (8 x 8, the viewer's view: his right on the left). The hat's brim hides the top row and shades the next; then
// the bushy brows, the eyes (W their whites), the nose, the moustache, the beard.
const FACE = ['SSSSSSSS', 'ssssssss', 'AAAsSAAA', 'SWESSEWS', 'SSSssSSS', 'SAAAAAAS', 'AAAMMAAA', 'AAAAAAAA'];

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
/** The sect's emblem embroidered between his shoulders: a white ring round a sky-blue cloud swirl. */
const CLOUD_BACK = ['RRWWWWRR', 'RWQQWQWR', 'RWQWWQWR', 'RWQQQQWR', 'RRWWWWRR', 'RRRRRRRR'];

// ---- head: the face, white hair at the sides and back
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') return faceRow(o, v)[u];
  if (f === 'up') return 'A';
  if (f === 'down') return 'A';
  // Bald under the hat; a crown of white hair round the back of the head.
  if (f === 'south') return v < 2 ? 's' : v < 3 ? 'S' : (u + v) % 3 === 0 ? 'X' : 'A';
  // A side, `front` counted from the face: the beard low at the front, the cheek, the ear, the crown of hair behind.
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 2) return 's';
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
// The beard, in steps down to the sash, tied with an azure ring.
piece('Head', 'head', 'beard_top', [-3.0, 23.0, -4.6], [3.0, 26.1, -3.4], (f, u, v) => ((u + v) % 3 === 0 ? 'X' : 'A'));
piece('Head', 'head', 'beard_mid', [-2.3, 19.2, -4.5], [2.3, 23.0, -3.5], (f, u, v) => (u % 3 === 1 ? 'X' : 'A'));
piece('Head', 'head', 'beard_ring', [-1.7, 18.6, -4.65], [1.7, 19.5, -3.35], (f, u) => (u % 2 ? 'c' : 'C'));
piece('Head', 'head', 'beard_low', [-1.5, 15.8, -4.35], [1.5, 18.6, -3.6], (f, u, v) => (u % 3 === 1 ? 'X' : 'A'));
piece('Head', 'head', 'beard_tip', [-0.7, 14.5, -4.15], [0.7, 15.8, -3.75], all('A'));

// ---- the straw hat (douli): wide, in five steps, an azure band and knob
const HAT = [[8.0, 30.8, 31.6], [6.4, 31.6, 32.6], [4.8, 32.6, 33.6], [3.2, 33.6, 34.6], [1.8, 34.6, 35.6]];
HAT.forEach(([r, y0, y1], i) => piece('Head', 'hat', 'hat_' + i, [-r, y0, -r], [r, y1, r],
  i === 2 ? (f, u, v, w, h) => (isSide(f) ? (u % 4 === 1 ? 'c' : 'C') : straw(f, u, v, w, h)) : straw));
piece('Head', 'hat', 'hat_knob', [-0.7, 35.6, -0.7], [0.7, 36.6, 0.7], (f, u, v) => (v === 0 ? 'C' : 'c'));

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
// The sash: indigo, an azure stripe, a silver cloud clasp; its knot and tails on his left; the jade pendant on his right.
piece('Torso', 'jacket', 'sash', [-4.5, 15.4, -2.5], [4.5, 17.6, 2.5], rim((f, u, v) => (v === 1 && isSide(f) ? 'C' : (u + v) % 5 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_clasp', [-1.3, 15.6, -3.0], [1.3, 17.4, -2.45], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 'C' : 'G'));
piece('Torso', 'jacket', 'sash_knot', [-3.9, 15.0, -3.3], [-2.0, 17.9, -2.5], (f, u, v) => (v === 1 ? 'b' : 'B'));
piece('Torso', 'jacket', 'sash_tail_a', [-3.7, 9.6, -3.5], [-2.9, 15.0, -3.1], fringe('B'));
piece('Torso', 'jacket', 'sash_tail_b', [-2.7, 10.4, -3.5], [-1.9, 15.0, -3.1], fringe('B'));
piece('Torso', 'jacket', 'pendant_cord', [2.35, 12.4, -3.05], [2.65, 15.4, -2.75], all('C'));
piece('Torso', 'jacket', 'pendant_jade', [1.7, 11.0, -3.25], [3.3, 12.4, -2.55], (f, u, v) => (f === 'north' && u === 1 && v === 0 ? 'W' : (u + v) % 3 === 0 ? 'j' : 'J'));
piece('Torso', 'jacket', 'pendant_tassel', [2.15, 8.6, -3.1], [2.85, 11.0, -2.7], fringe('C'));
// The empty scabbard of his sword across his back, a little off to his right: lacquer, silver mouth, band and chape.
piece('Chest', 'jacket', 'scabbard', [0.6, 9.0, 2.05], [2.0, 23.4, 3.1], (f, u, v, w, h) => (v <= 1 || v === Math.floor(h / 2) || v >= h - 2 ? 'G' : (u + v) % 6 === 0 ? 'k' : 'K'),
  { tilt: 6, pivot: [1.3, 23.4, 2.6] });
piece('Chest', 'jacket', 'scabbard_strap', [-4.3, 21.2, 2.0], [4.3, 22.0, 2.3], all('B'));

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

// ---- legs: the robe's skirt split over each leg, hemmed in sky blue with white clouds; trousers, socks, cloth shoes
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
  fs.mkdirSync(path.join(art, 'azure_sensei'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'azure_sensei.png'), atlas);
  // One atlas per face; the calm face keeps the plain name.
  for (const face of Object.keys(FACES)) if (face !== 'calm')
    fs.writeFileSync(path.join(assets, 'textures', 'entity', 'azure_sensei_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...SENSEI, face }, ATLAS)));
  fs.writeFileSync(path.join(art, 'azure_sensei', 'sensei.png'), atlas);
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'azure_sensei.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'azure_sensei.bbmodel'), JSON.stringify(kit.bbmodel('azure_sensei', PIECES, [SENSEI], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote Sensei Yun:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, SENSEI, FACES };
