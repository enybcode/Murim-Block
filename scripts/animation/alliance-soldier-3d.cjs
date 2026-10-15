// The Orthodox Alliance's soldiers, five kinds in one model on Epic Fight's biped skeleton, all in cloth: their armour
// and weapons are items of their own (the spear, the jian, the rattan shield in hand). After the concept sheet Enzo
// chose on 15/10/2026. Each kind is an outfit: the pieces it does not wear are clear in its atlas.
//   spear     Spear Guard: quilted azure coat under a white sash, the veteran's scarred face;
//   sword     Sword Disciple: a topknot in a gold crown, an azure headband with tails, a white sash;
//   archer    Archer: a quilted coat under a gold sash, a side-swept fringe;
//   monk      Warrior Monk: shaved head, ochre robe, an azure kasaya over the left shoulder, prayer beads;
//   sergeant  Sergeant: quilted coat under a gold sash, a full beard.
// All wear the azure robe crossed right over left (white collar edged with gold), cloth wraps on the forearms, a
// gold-studded leather belt, the robe's skirt over each thigh, indigo trousers and black boots. Faces at 16 x 16
// (hd-face.cjs), each kind its own, ten expressions each (client/MobFaces).
//   art/blockbench/alliance_soldier.bbmodel (+ alliance_soldier/<kind>.png)          the Blockbench project;
//   assets/murimblock/animmodels/entity/alliance_soldier.json                         the Epic Fight mesh;
//   assets/murimblock/textures/entity/alliance_soldier/<kind>.png (+ _<face>.png)      the atlases.
// Usage: node scripts/animation/alliance-soldier-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, fringe, wraps, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;
const KINDS = ['spear', 'sword', 'archer', 'monk', 'sergeant'];

/** Each kind's face (hd-face.cjs traits). */
const FACES = {
  spear: { skin: '#c99670', hair: '#262019', iris: '#3a2616', fringe: [6, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 6],
    brow: 'flat', mouth: 'stern', moustache: true, goatee: true, scar: [[11, 5], [11, 6], [12, 7], [12, 10], [13, 11]] },
  sword: { skin: '#d9ab84', hair: '#1c1714', iris: '#4a2e1a', fringe: [9, 7, 5, 5, 4, 4, 3, 2, 2, 3, 4, 4, 5, 5, 7, 9], band: 3 },
  archer: { skin: '#e0b48e', hair: '#2a1f18', iris: '#5a3a1c', fringe: [8, 7, 6, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 2, 4, 7],
    eyes: 'narrow', shine: [[2, 1], [3, 2], [5, 3], [6, 3], [9, 1]] },
  monk: { skin: '#cf9d72', hair: '#3a2e26', iris: '#3a2616', marks: [5, 7, 8, 10], brow: 'soft', eyes: 'closed', mouth: 'smile' },
  sergeant: { skin: '#bf8a62', hair: '#1a1512', iris: '#2e1e12', fringe: [5, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 5],
    brow: 'thick', mouth: 'stern', beard: true },
};

/**
 * Colour letters: S skin, A hair, R robe, Q accent (light azure; the monk's kasaya), G pale gold, W white, L leather,
 * P trousers, F boots, D dark wood (beads).
 */
const BASE = { R: '#2B5FA8', Q: '#5d8fd6', G: '#E9D9A6', W: '#ebe6d8', L: '#5a3a24', P: '#22324e', F: '#1b1715', D: '#4a2e1c' };
const OUTFITS = KINDS.map(key => ({ key, c: { ...BASE, S: FACES[key].skin, A: FACES[key].hair,
  ...(key === 'monk' ? { R: '#b8862e', Q: '#2B5FA8' } : {}) } }));

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
/** Worn by these kinds only. */
const only = (...kinds) => ({ wear: o => kinds.includes(o.key) });

// ---- head: hair at the sides and back (the monk shaved), the face on its plate
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  const bald = o.key === 'monk';
  if (f === 'north') return 'S';
  if (f === 'up') return bald ? 'S' : 'A';
  if (f === 'down') return 'S';
  if (bald) return f === 'south' && v === h - 1 ? 's' : 'S';
  if (f === 'south') return v < 6 ? 'A' : (o.key === 'sergeant' ? 'A' : 'a');
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 3) return 'A';
  if (o.key === 'sergeant' && v >= 5 && front <= 4) return (u + v) % 4 === 0 ? 'a' : 'A'; // the beard round the jaw
  if (front >= 4) return (u + v) % 3 === 0 ? 'a' : 'A';
  if (front === 3 && (v === 3 || v === 4)) return v === 3 ? 'S' : 's';
  return 'S';
});
const facePaints = Object.fromEntries(KINDS.map(k => [k, hd.paint(FACES[k])]));
piece('Head', 'head', 'face', ...hd.PLATE, (f, u, v, w, h, o) => facePaints[o.key](f, u, v, w, h, o), { density: hd.DENSITY });
// The sword disciple's topknot in its gold crown, and the azure headband with its tails behind.
piece('Head', 'hat', 'knot', [-1.4, 32, -0.2], [1.4, 34.4, 2.6], (f, u, v) => (v === 0 ? 'a' : 'A'), only('sword'));
piece('Head', 'hat', 'crown', [-1.2, 33.4, -0.6], [1.2, 34.8, 1.4], all('G'), only('sword'));
piece('Head', 'hat', 'band', [-4.35, 29.95, -4.35], [4.35, 30.55, 4.35], rim(all('Q')), only('sword'));
piece('Head', 'hat', 'band_tail', [-1.4, 23.6, 4.4], [1.4, 28.6, 4.8], fringe('Q'), { tilt: -16, pivot: [0, 28.6, 4.6], ...only('sword') });

// ---- body: the robe crossed right over left, a white collar edged with gold
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w) => (f === 'north' && (u === 0 || u === w - 1) ? 'r' : 'R'), { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f) => (f === 'down' ? 'P' : 'R'), { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar', [-2.6, 23.3, -2.4], [2.6, 24.4, 2.4], rim(all('W')));
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.7]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.3, y + 1.0, -2.0], (f, u) => (u === 0 ? 'G' : 'W')));
// The quilted coat (guard, archer, sergeant) and the sashes.
piece('Chest', 'jacket', 'quilting', [-4.3, 18, -2.3], [4.3, 23.4, 2.3], rim((f, u, v) => (v % 2 === 0 ? 'r' : 'R')),
  { hinge: { y: 18, bone: 'Torso' }, ...only('spear', 'archer', 'sergeant') });
piece('Torso', 'jacket', 'sash', [-4.5, 16.6, -2.5], [4.5, 18.4, 2.5],
  rim((f, u, v, w, h, o) => (o.key === 'archer' || o.key === 'sergeant' ? 'G' : o.key === 'monk' ? 'Q' : 'W')));
piece('Torso', 'jacket', 'sash_tail', [-3.6, 11.4, -3.3], [-2.4, 16.6, -2.9], fringe('W'), only('sword'));
// The monk's kasaya across from the left shoulder, and his prayer beads.
piece('Chest', 'jacket', 'kasaya', [-4.3, 17.4, -2.3], [4.3, 24.3, 2.3], (f, u, v, w, h) => {
  if (f === 'up' || f === 'down') return null;
  const diag = f === 'north' ? u - (w - 1 - v) : f === 'south' ? (w - 1 - u) - (w - 1 - v) : 99;
  return Math.abs(diag) <= 1 ? (v % 3 === 0 ? 'G' : 'Q') : null;
}, { hinge: { y: 18, bone: 'Torso' }, ...only('monk') });
for (let i = 0; i < 9; i++) {
  const a = Math.PI * (0.1 + 0.8 * i / 8), x = -Math.cos(a) * 3.2, y = 23.4 - Math.sin(a) * 3.4;
  piece('Chest', 'jacket', 'bead_' + i, [x - 0.5, y - 0.5, -2.8], [x + 0.5, y + 0.5, -2.2], all(i === 4 ? 'G' : 'D'), only('monk'));
}
// The belt, gold-studded leather, a gold buckle.
piece('Torso', 'jacket', 'belt', [-4.5, 12.6, -2.5], [4.5, 14.4, 2.5], rim((f, u, v) => (isSide(f) && v === 1 && u % 3 === 1 ? 'G' : 'L')));
piece('Torso', 'jacket', 'buckle', [-1.2, 12.3, -2.9], [1.2, 14.7, -2.4], all('G'));

// ---- arms: sleeves, the hands, cloth wraps on the forearms
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [a0, a1] = xs(4, 8), [b0, b1] = xs(3.7, 8.3);
  piece('Arm_' + side, part + 'Arm', 'upper_' + s, [a0, 17, -2], [a1, 24, 2], all('R'), { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'fore_' + s, [a0, 11, -2], [a1, 17, 2], (f, u, v, w, h) => (f === 'down' || v >= h - 2 ? 'S' : 'R'),
    { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'wrap_' + s, [b0, 12.4, -2.3], [b1, 16, 2.3],
    rim((f, u, v, w, h, o) => wraps('W', o.key === 'monk' ? 'D' : 'R')(f, u, v, w, h)));
}

// ---- legs: trousers, the robe's skirt over each thigh, black boots
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [t0, t1] = xs(0, 4), [k0, k1] = xs(-0.4, 4.4), [d0, d1] = xs(-0.3, 4.3);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], all('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [k0, 7.2, -2.4], [k1, 13, 2.4],
    (f, u, v, w, h) => (f === 'up' || f === 'down' ? null : v === h - 1 ? 'G' : u % 3 === 2 ? 'r' : 'R'));
  piece('Leg_' + side, part + 'Pants', 'boot_' + s, [d0, 0, -2.6], [d1, 4.5, 2.3], (f, u, v, w, h) => (v === h - 1 ? 'f' : v === 0 && isSide(f) ? 'G' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const dir = path.join(assets, 'textures', 'entity', 'alliance_soldier');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(dir, { recursive: true });
  fs.mkdirSync(path.join(art, 'alliance_soldier'), { recursive: true });
  const atlases = OUTFITS.map(o => kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
  OUTFITS.forEach((o, i) => {
    fs.writeFileSync(path.join(dir, o.key + '.png'), atlases[i]);
    fs.writeFileSync(path.join(art, 'alliance_soldier', o.key + '.png'), atlases[i]);
    // One atlas per face; the calm face keeps the plain name.
    for (const face of Object.keys(hd.EXPRESSIONS)) if (face !== 'calm')
      fs.writeFileSync(path.join(dir, o.key + '_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...o, face }, ATLAS)));
  });
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'alliance_soldier.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'alliance_soldier.bbmodel'), JSON.stringify(kit.bbmodel('alliance_soldier', PIECES, OUTFITS, atlases, ATLAS, 'entity/alliance_soldier'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Alliance soldiers:', PIECES.length, 'pieces,', OUTFITS.length, 'kinds,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFITS, FACES, KINDS };
