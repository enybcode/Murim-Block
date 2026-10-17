// The Captain of the Peak Guard (docs/mobs/peak-captain.md), in cloth only, on Epic Fight's biped skeleton: his armour
// (peak-guard-armor.cjs) and his spear (crimson-dragon-spear-model.cjs) are items of their own, worn and held, each in
// its own Blockbench project. After the concept sheet Enzo chose on 15/10/2026, the "guard" proposal.
// A long azure coat crossed right over left over a white under-robe, gold-edged lapels, wide cuffs and long panels
// hemmed with clouds, a leather belt with a gold beast-head buckle, a jade pendant, a white cape with the Alliance's
// emblem (a sword planted in three peaks, in a gold ring), indigo trousers and black boots with gold toes. A topknot;
// the face at 16 x 16 (hd-face.cjs), scarred, a long moustache and a goatee, ten expressions (client/MobFaces).
//   art/blockbench/peak_captain.bbmodel (+ peak_captain/peak_captain.png)   the Blockbench project;
//   assets/murimblock/animmodels/entity/peak_captain.json                    the Epic Fight mesh;
//   assets/murimblock/textures/entity/peak_captain/peak_captain.png (+ _<face>.png)   the atlases.
// Usage: node scripts/animation/peak-captain-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

const FACE = { skin: '#c99670', hair: '#1f1814', iris: '#3a2616', fringe: [6, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 6],
  brow: 'thick', mouth: 'stern', moustache: 'long', goatee: true, scar: [[4, 5], [4, 6], [5, 7], [5, 10], [6, 11]] };

/** S skin, A hair, R coat, Q light azure, G gold, W white, L leather, P trousers, F boots, C cape, N cape lining, T tassel, K fur, E jade. */
const OUTFIT = { key: 'peak_captain', c: { R: '#2B5FA8', Q: '#5d8fd6', G: '#E9C46A', W: '#efeae0', L: '#5a3a24', P: '#1e2c48',
  F: '#1b1715', C: '#f0ebe0', N: '#2B5FA8', T: '#c0202c', K: '#6b4a2e', E: '#3fb8c8', S: FACE.skin, A: FACE.hair } };

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
const both = fn => { for (const [side, sx] of [['R', 1], ['L', -1]]) fn(side, sx, (a, b) => (sx > 0 ? [a, b] : [-b, -a]), side.toLowerCase()); };

/** A cloud motif along a hem: little white curls on the base colour. */
const cloudHem = (base, curl) => (u, v) => {
  const k = (u + (v % 2) * 2) % 6;
  return (v === 0 && (k === 1 || k === 2)) || (v === 1 && (k === 0 || k === 3)) ? curl : base;
};

// ---- head and face, the topknot
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w) => {
  if (f === 'north' || f === 'down') return 'S';
  if (f === 'up' || f === 'south') return (u + v) % 5 === 0 ? 'a' : 'A';
  const front = f === 'east' ? w - 1 - u : u;
  if (v < 3 || front >= 4) return (u + v) % 3 === 0 ? 'a' : 'A';
  return 'S';
});
const facePaint = hd.paint(FACE);
piece('Head', 'head', 'face', ...hd.PLATE, facePaint, { density: hd.DENSITY });
piece('Head', 'hat', 'knot', [-1.3, 32, -0.4], [1.3, 34, 2.2], (f, u, v) => (v === 0 ? 'a' : 'A'));

// ---- body: the coat over the white under-robe, gold lapels
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w) => {
  if (f === 'north' && v <= 1 && u >= 3 && u <= 4) return 'W';
  return f === 'north' && (u === 0 || u === w - 1) ? 'r' : (u + v * 3) % 9 === 0 ? 'r' : 'R';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v) => (f === 'down' ? 'P' : (u + v * 3) % 9 === 0 ? 'r' : 'R'), { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar_in', [-2.2, 23.6, -2.3], [2.2, 24.6, 2.3], rim(all('W')));
piece('Chest', 'jacket', 'collar', [-2.8, 23.2, -2.5], [2.8, 24.3, 2.5], rim((f, u) => (u % 2 ? 'G' : 'g')));
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.6], [3.3, 13.0]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.2, y + (i === 5 ? 5.6 : 1.0), -2.0], (f, u) => (u === 0 ? 'g' : 'G')));
// The belt with its beast-head buckle (jade eyes), the jade pendant and its tassel, the sash tail. The pendant, the
// tassel and the sash hang a little forward, over the tassets when he wears them.
piece('Torso', 'jacket', 'belt', [-4.6, 13.2, -2.6], [4.6, 15.4, 2.6], rim((f, u, v) => (isSide(f) && v === 1 && u % 3 === 1 ? 'G' : 'L')));
piece('Torso', 'jacket', 'buckle', [-1.6, 12.6, -3.2], [1.6, 16, -2.6], (f, u, v, w, h) => {
  if (f !== 'north') return 'g';
  if (v === 1 && (u === 0 || u === w - 1)) return 'E';
  if (v === h - 1 && u >= 1 && u <= w - 2) return 'g';
  return (u + v) % 3 === 0 ? 'g' : 'G';
});
piece('Torso', 'jacket', 'pendant', [2.2, 10.2, -3.5], [3.4, 11.8, -3.1], (f, u, v) => (u === 0 && v === 0 ? 'W' : 'E'));
piece('Torso', 'jacket', 'pendant_tassel', [2.45, 7.6, -3.45], [3.15, 10.2, -3.15], fringe('T'),
  { cloth: { outer: 'north', links: 3, plane: { normal: [0, 0, -1], at: 3.2 } } });
piece('Torso', 'jacket', 'sash_tail', [-3.8, 8.4, -3.5], [-2.6, 13.2, -3.1], fringe('Q'),
  { cloth: { outer: 'north', links: 4, plane: { normal: [0, 0, -1], at: 3.2 } } });
// The cape, its azure lining at the edges, the emblem on the back. Behind the cuirass, which is at most 2.85 deep.
// Cloth: the cape, the sash and the tassel are not in the rigid mesh; client/ClothLayer hangs them from their joint and
// lets them swing (cloth/peak_captain.json, below).
piece('Chest', 'jacket', 'cape', [-4.8, 3, 3.0], [4.8, 24.4, 3.6], (f, u, v, w, h) => {
  if (!isSide(f) || f === 'north') return 'N';
  if (u === 0 || u === w - 1 || v === h - 1) return 'G';
  if (f === 'south') {
    const cx = (w - 1) / 2, cy = 6, d = Math.hypot(u - cx, (v - cy) * 1.1);
    if (Math.abs(d - 4.2) < 0.6) return 'G';
    if (d < 3.7) {
      if (Math.abs(u - cx) < 0.6 && v >= cy - 3 && v <= cy + 1) return 'W';
      if (v === cy - 2 && Math.abs(u - cx) < 1.6) return 'G';
      if (v >= cy + 1 && v <= cy + 3 && Math.abs(u - cx) <= 2 * (v - cy)) return 'Q';
    }
  }
  return (u + v) % 7 === 0 ? 'c' : 'C';
}, { tilt: -8, pivot: [0, 24.4, 3.3], cloth: { outer: 'south', links: 7, plane: { normal: [0, 0, 1], at: 2.95 } } });

// ---- arms: the sleeves, wide cuffs hemmed with clouds, the white under-sleeve, the hands
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'right' : 'left';
  const [a0, a1] = xs(4, 8), [c0, c1] = xs(3.4, 8.6), [u0, u1] = xs(3.8, 8.2);
  piece('Arm_' + side, part + 'Arm', 'upper_' + s, [a0, 17, -2], [a1, 24, 2], (f, u, v) => ((u + v * 3) % 9 === 0 ? 'r' : 'R'), { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'fore_' + s, [a0, 11, -2], [a1, 17, 2], (f, u, v, w, h) => (f === 'down' || v >= h - 2 ? 'S' : 'R'), { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'under_' + s, [u0, 12.2, -2.2], [u1, 13.4, 2.2], rim(all('W')));
  piece('Hand_' + side, part + 'Sleeve', 'cuff_' + s, [c0, 13.2, -2.7], [c1, 17.2, 2.7],
    rim((f, u, v, w, h) => (!isSide(f) ? 'R' : v >= h - 2 ? cloudHem('Q', 'W')(u, h - 1 - v) : v === h - 3 ? 'G' : 'R')), { hinge: { y: 17, bone: 'Arm_' + side } });
});

// ---- legs: trousers, the coat's long panels hemmed with clouds over the longer white under-robe, the boots
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'right' : 'left';
  const [t0, t1] = xs(0, 4), [k0, k1] = xs(-0.4, 4.4), [w0, w1] = xs(-0.2, 4.2), [d0, d1] = xs(-0.3, 4.3);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], all('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'under_robe_' + s, [w0, 4.2, -2.25], [w1, 13, 2.25], f => (f === 'up' || f === 'down' ? null : 'W'));
  piece('Thigh_' + side, part + 'Pants', 'coat_' + s, [k0, 5.2, -2.45], [k1, 13.1, 2.45], (f, u, v, w, h) => {
    if (f === 'up' || f === 'down') return null;
    if (v >= h - 2) return cloudHem('Q', 'W')(u, h - 1 - v);
    if (v === h - 3) return 'G';
    return u % 3 === 2 ? 'r' : 'R';
  });
  piece('Leg_' + side, part + 'Pants', 'boot_' + s, [d0, 0, -2.6], [d1, 5.2, 2.3], (f, u, v, w, h) => {
    if (v === h - 1) return 'f';
    if (v <= 1 && isSide(f)) return 'K';
    if (f === 'north' && v >= h - 2) return 'G';
    return 'F';
  });
});

/**
 * The cloth pieces for client/ClothLayer, in Blockbench pixels: each hangs from its top edge (two corners, left x first)
 * on its joint, `length` down along `drop` (the piece's rest slant), in `links` links, `thickness` thick; `outer` and
 * `inner` are its two broad faces in the atlas (u0 at the left corner, v0 at the top); it stays on the outer side of
 * `plane` (normal and distance from the joint's origin plane, in the model's rest space) so it never sinks into him.
 */
function cloth(pieces, atlas) {
  const strips = pieces.filter(p => p.cloth).map(p => {
    const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, a = (p.tilt || 0) * Math.PI / 180;
    const outer = p.cloth.outer, inner = outer === 'south' ? 'north' : 'south';
    const z = outer === 'south' ? z1 - (z1 - z0) / 2 : z0 + (z1 - z0) / 2;
    // A face's uv with u0 at the left corner (x0): the south face starts at x0, the north at x1.
    const face = f => { const [u0, v0, u1, v1] = p.uv[f]; return f === 'south' ? [u0, v0, u1, v1] : [u1, v0, u0, v1]; };
    return { name: p.name, joint: p.bone, top: [[x0, y1, z], [x1, y1, z]], length: y1 - y0, thickness: z1 - z0,
      drop: [0, -Math.cos(a), -Math.sin(a)].map(v => Math.round(v * 1e4) / 1e4), links: p.cloth.links,
      outer: face(outer), inner: face(inner), plane: p.cloth.plane };
  });
  return { texture_size: atlas, strips };
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const dir = path.join(assets, 'textures', 'entity', 'peak_captain');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(dir, { recursive: true });
  fs.mkdirSync(path.join(art, 'peak_captain'), { recursive: true });
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, OUTFIT, ATLAS));
  fs.writeFileSync(path.join(dir, 'peak_captain.png'), atlas);
  fs.writeFileSync(path.join(art, 'peak_captain', 'peak_captain.png'), atlas);
  for (const face of Object.keys(hd.EXPRESSIONS)) if (face !== 'calm')
    fs.writeFileSync(path.join(dir, 'peak_captain_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...OUTFIT, face }, ATLAS)));
  const m = kit.mesh(PIECES.filter(p => !p.cloth), ATLAS);
  fs.mkdirSync(path.join(assets, 'cloth'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'cloth', 'peak_captain.json'), JSON.stringify(cloth(PIECES, ATLAS), null, 1) + '\n');
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'peak_captain.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'peak_captain.bbmodel'), JSON.stringify(kit.bbmodel('peak_captain', PIECES, [OUTFIT], [atlas], ATLAS, 'entity/peak_captain'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Peak Captain:', PIECES.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFIT, FACE };
