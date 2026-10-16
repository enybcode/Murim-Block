// The Blood Cultists (Blood Sect), redone in Murimblock's own style from Colleuhu95's Blood Ninja (addons/blood-sect-decor),
// in cloth only, on Epic Fight's biped skeleton (no weapon, no armour baked in: those are items of their own). One model
// for five outfits, the sect's ranks, like the bandits' (a piece an outfit does not wear is clear in its atlas):
//   acolyte      a black hood over the head, open on the face, a blood-red cloth mask from under the eyes, tied behind;
//   disciple     bare-headed, a topknot and a red headband with two tails, the mask;
//   zealot       a blood-red hood, a long black veil to a point under the chin, a black sash;
//   bloodbinder  the head shaved and marked with the sect's drop in red, the mask, the darkest robe;
//   elder        a black hood, bone-white lapels and collar, the face bare, a long grey moustache and beard.
// All wear the long robe crossed right over left, the sash knotted in front with its tails, the sect's mark on the back (a
// drop of blood in a ring), sleeves gathered at the wrist by black wraps and a red cord, the robe's panels to the knee with
// a red hem, trousers bound at the shin with two cords, cloth shoes. The face at 16 x 16 (hd-face.cjs), red-eyed, ten
// expressions (client/MobFaces).
//   art/blockbench/blood_cultist.bbmodel (+ blood_cultist/<outfit>.png)       the Blockbench project.
// In game they are the Blood Sect Decor addon's Blood Ninja (blood-cultist-addon.cjs writes its files).
// Usage: node scripts/animation/blood-cultist-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/** The faces' common traits: red eyes, level brows, a stern mouth. */
const FACE = { iris: '#c0202c', brow: 'flat', mouth: 'stern', fringe: [3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3] };

/**
 * Colour letters: S skin, A hair, H hood or headwear, R robe, L lapels and collar, X blood red (hems, ties, cords), D
 * bright blood (the mark, the tattoo), M mask or veil, B under-robe, K wraps, P trousers, F shoes, Y sash, Z bone white.
 * Each outfit: its colours (c), its face's traits (t), what it wears.
 */
const OUTFITS = [
  { key: 'acolyte', hood: true, mask: true, t: { skin: '#d8b49a', hair: '#0f0c0d' },
    c: { H: '#141114', R: '#2a1217', L: '#a3202a', X: '#a3202a', D: '#d8323a', M: '#7c121c', B: '#0e0b0c', K: '#1a1517', P: '#1c161a', F: '#0d0a0b', Y: '#8e1822', Z: '#e8ddc8' } },
  { key: 'disciple', band: true, knot: true, mask: true, t: { skin: '#cfa588', hair: '#141012', fringe: [5, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 5] },
    c: { H: '#a3202a', R: '#3a1219', L: '#a3202a', X: '#a3202a', D: '#d8323a', M: '#7c121c', B: '#0e0b0c', K: '#1a1517', P: '#1f181b', F: '#0d0a0b', Y: '#8e1822', Z: '#e8ddc8' } },
  { key: 'zealot', hood: true, veil: true, t: { skin: '#c99c80', hair: '#0f0c0d' },
    c: { H: '#7c121c', R: '#1a1214', L: '#a3202a', X: '#a3202a', D: '#d8323a', M: '#1e1416', B: '#0e0b0c', K: '#161214', P: '#161214', F: '#0b0909', Y: '#141114', Z: '#e8ddc8' } },
  { key: 'bloodbinder', mask: true, shaved: true, t: { skin: '#c49478', hair: '#b08468', fringe: null, brow: 'thick' },
    c: { H: '#141114', R: '#1c1416', L: '#7c121c', X: '#8e1822', D: '#c42030', M: '#5a0e14', B: '#0b0909', K: '#141012', P: '#141012', F: '#0b0909', Y: '#5a0e14', Z: '#d8ccb6' } },
  { key: 'elder', hood: true, t: { skin: '#d2ad92', hair: '#8f8a86', brow: 'thick', moustache: 'long', beard: true, iris: '#d03030' },
    c: { H: '#0f0c0e', R: '#120e10', L: '#e8ddc8', X: '#a3202a', D: '#d8323a', M: '#7c121c', B: '#2a1217', K: '#141012', P: '#141012', F: '#0b0909', Y: '#a3202a', Z: '#e8ddc8' } },
];
for (const o of OUTFITS) { o.t = { ...FACE, ...o.t }; o.c.S = o.t.skin; o.c.A = o.t.hair; }
const hood = { wear: o => o.hood }, masked = { wear: o => o.mask }, tied = { wear: o => o.mask || o.veil };

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
const both = fn => { for (const [side, sx] of [['R', 1], ['L', -1]]) fn(side, sx, (a, b) => (sx > 0 ? [a, b] : [-b, -a]), side.toLowerCase()); };

/** Plain cloth with soft folds: a darker thread every few texels, never noise. */
const cloth = l => (f, u, v) => (isSide(f) && (u % 4 === 3) ? l.toLowerCase() : l);

// ---- the head: the hood over it, open on the face; the face; the mask over its lower half and its ties
// The head: the hair under the hood or the topknot; shaved, the skin and, on the crown, the sect's drop in red.
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'north') return 'S';
  if (!o.shaved) return 'A';
  if (f === 'up') {
    const c = (w - 1) / 2, dy = v - 4.6, half = dy >= 0 ? Math.sqrt(Math.max(0, 1.8 * 1.8 - dy * dy)) : 1.8 * (1 + dy / 3.4);
    return dy > -3.4 && Math.abs(u - c) <= half ? 'D' : (u + v) % 4 === 0 ? 'a' : 'A';
  }
  // Two red lines down the back of the head.
  if (f === 'south' && (u === 2 || u === w - 3) && v <= 4) return 'D';
  return (u + v) % 4 === 0 ? 'a' : 'A';
});
// The face, kept clean: level brows, open red eyes (the mask covers it from just under them).
const facePaints = Object.fromEntries(OUTFITS.map(o => [o.key, hd.paint(o.t)]));
piece('Head', 'head', 'face', ...hd.PLATE, (f, u, v, w, h, o) => facePaints[o.key](f, u, v, w, h, o), { density: hd.DENSITY });
// The hood: a shell over the head, its front opening framing the face (the brow to the chin, the cheeks left bare).
piece('Head', 'hat', 'hood', [-4.5, 23.6, -4.4], [4.5, 32.6, 4.5], (f, u, v, w, h) => {
  if (f === 'down') return null;
  if (f === 'north') {
    // The opening: the face shows from the hood's brow (two texels down) to its bottom, inside a one-texel rim.
    if (u >= 1 && u <= w - 2 && v >= 2) return null;
    return v === 1 || u === 1 || u === w - 2 ? 'h' : 'H';
  }
  // Two folds down the back and the sides, a darker edge at the bottom.
  if (v === h - 1) return 'h';
  return (u === 2 || u === w - 3) && f !== 'up' ? 'h' : 'H';
}, hood);
// The hood's thick brim round the opening: over the brow and down both cheeks to the mask.
piece('Head', 'hat', 'hood_brim_top', [-3.9, 30.2, -4.75], [3.9, 31.0, -4.35], (f, u) => (f === 'north' && u % 3 === 1 ? 'h' : 'H'), hood);
for (const x of [-3.9, 3.1]) piece('Head', 'hat', 'hood_brim_' + (x < 0 ? 'l' : 'r'), [x, 26.6, -4.75], [x + 0.8, 30.2, -4.35], (f, u, v) => (v % 3 === 2 ? 'h' : 'H'), hood);
// The hood's peak, a little point at the back of the crown.
piece('Head', 'hat', 'hood_peak', [-1.5, 32.4, 1.8], [1.5, 33.6, 4.6], (f, u, v) => (v === 0 ? 'h' : 'H'), hood);
// Bare-headed: the topknot tied with a red cord, the headband over the brow, its two tails behind (cloth).
piece('Head', 'hat', 'topknot', [-1.3, 32, -0.6], [1.3, 34.6, 2.0], (f, u, v, w, h) => (v === h - 1 ? 'X' : v === 0 ? 'a' : 'A'), { wear: o => o.knot });
piece('Head', 'hat', 'headband', [-4.3, 29.0, -4.3], [4.3, 30.4, 4.3], rim((f, u, v) => (f === 'north' && v === 0 ? 'h' : 'H')), { wear: o => o.band });
for (const [i, x] of [[0, -1.0], [1, 0.2]]) piece('Head', 'hat', 'band_tail_' + i, [x, 23.6 + i, 4.35], [x + 0.8, 29.8, 4.7], fringe('H'),
  { wear: o => o.band, cloth: { outer: 'south', links: 3, plane: { normal: [0, 0, 1], at: 4.3 } } });
// The mask: cloth from the bridge of the nose down over the chin, standing a little off the face, a seam down its middle,
// a folded edge at the top, two pleats; it wraps round the cheeks to the hood.
piece('Head', 'hat', 'mask', [-4.2, 23.8, -4.35], [4.2, 27.0, -3.6], (f, u, v, w, h) => {
  if (f === 'south' || f === 'up') return null;
  if (f !== 'north') return v === 0 ? 'X' : 'M';
  // At twice the texels: the folded top edge and its shadow, the cloth lifted over the nose, soft pleats falling
  // from it, a darker hem under the chin.
  const c = (w - 1) / 2;
  if (v === 0) return 'X';
  if (v === 1) return Math.abs(u - c) < 1.6 ? 'X' : 'x';
  if (v === h - 1) return 'm';
  if (v <= 3 && Math.abs(u - c) < 1.1) return 'X';
  if ((v >= 3 && Math.abs(Math.abs(u - c) - (2.5 + (v - 3) * 0.5)) < 0.5) || (v >= 4 && Math.abs(Math.abs(u - c) - 6) < 0.5)) return 'm';
  return 'M';
}, { density: 2, ...masked });
// The veil: longer, a point under the chin, a red hem, standing a little off the face.
piece('Head', 'hat', 'veil', [-4.3, 21.4, -4.5], [4.3, 27.0, -3.6], (f, u, v, w, h) => {
  if (f === 'south' || f === 'up') return null;
  const c = (w - 1) / 2, depth = h - 1 - v, reach = (c + 0.6) * Math.min(1, (v + 1) / (h * 0.42));
  if (f === 'north') {
    if (v >= h * 0.42 && Math.abs(u - c) > (c + 0.6) * (1 - (v - h * 0.42) / (h * 0.62))) return null;
    if (v === 0) return 'X';
    const edge = Math.abs(u - c) > (c + 0.6) * (1 - (v - h * 0.42) / (h * 0.62)) - 1.2 && v >= h * 0.42;
    return edge ? 'X' : u % 4 === 1 ? 'm' : 'M';
  }
  return v < h * 0.42 ? (v === 0 ? 'X' : 'M') : null;
}, { density: 2, wear: o => o.veil });
piece('Head', 'hat', 'mask_sides', [-4.65, 24.6, -3.6], [4.65, 27.0, 3.4], (f, u, v, w, h) => (isSide(f) && f !== 'north' && f !== 'south' ? (v === 0 ? 'X' : 'M') : null), tied);
// The knot at the back of the head, and its two tails (cloth).
piece('Head', 'hat', 'mask_knot', [-0.7, 25.4, 4.5], [0.7, 26.9, 5.3], (f, u, v) => (v === 0 ? 'X' : 'M'), tied);
for (const [n, x0, x1] of [['l', -2.4, -0.7], ['r', 0.7, 2.4]]) piece('Head', 'hat', 'mask_loop_' + n, [x0, 25.6, 4.6], [x1, 26.8, 5.1], (f, u, v) => (v === 0 ? 'X' : u % 2 ? 'm' : 'M'), tied);
for (const [i, x] of [[0, -1.1], [1, 0.3]]) piece('Head', 'hat', 'mask_tail_' + i, [x, 22.8, 4.9], [x + 0.8, 26.4, 5.3], fringe('M'),
  { ...tied, cloth: { outer: 'south', links: 3, plane: { normal: [0, 0, 1], at: 4.6 } } });

// ---- the body: the robe crossed right over left, the black under-robe at the collar, the red lapels
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w) => {
  if (f === 'north' && v <= 1 && u >= 3 && u <= 4) return 'B';
  return cloth('R')(f, u, v);
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u, v) => (f === 'down' ? 'P' : cloth('R')(f, u, v)), { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar_in', [-2.2, 23.6, -2.3], [2.2, 24.6, 2.3], rim(all('B')));
piece('Chest', 'jacket', 'collar', [-2.8, 23.2, -2.5], [2.8, 24.3, 2.5], rim((f, u) => (u % 3 === 0 ? 'l' : 'L')));
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.6], [3.3, 15.4]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.2, y + (i === 5 ? 3.2 : 1.0), -2.0], (f, u) => (u === 0 ? 'l' : 'L')));
// The sect's mark on the back: a drop of blood in a ring of bone white.
piece('Chest', 'jacket', 'mark', [-2.6, 16.6, 2.0], [2.6, 21.8, 2.3], (f, u, v, w, h) => {
  if (f !== 'south') return null;
  // At three texels a pixel (16 x 16): a ring, inside it a drop, round below and drawn to a point at the top, lit on its
  // left.
  const cx = (w - 1) / 2, cy = (h - 1) / 2, R = w / 2 - 0.4, d = Math.hypot(u - cx, v - cy);
  if (d > R) return null;
  if (d > R - 1.3) return 'Z';
  const r = R * 0.42, by = cy + R * 0.22, dy = v - by, tip = R * 0.78;
  const half = dy >= 0 ? Math.sqrt(Math.max(0, r * r - dy * dy)) : r * (1 + dy / tip);
  if (dy > -tip && Math.abs(u - cx) <= half) return u < cx - r * 0.3 && dy > -r * 0.6 && dy < r * 0.4 ? 'D' : 'X';
  return 'R';
}, { density: 3 });
// The wide sash, its knot in front, its two tails (cloth).
piece('Torso', 'jacket', 'sash', [-4.6, 13.0, -2.6], [4.6, 16.0, 2.6], rim((f, u, v, w, h) => (isSide(f) && (v === 0 || v === h - 1) ? 'y' : 'Y')));
piece('Torso', 'jacket', 'sash_knot', [-2.6, 13.4, -3.3], [-0.4, 15.8, -2.6], (f, u, v) => (v === 0 || u === 0 ? 'y' : 'Y'));
piece('Torso', 'jacket', 'sash_loop', [-4.1, 14.0, -3.2], [-2.6, 15.4, -2.7], (f, u, v) => (u === 0 ? 'y' : 'Y'));
for (const [i, x] of [[0, -2.5], [1, -1.4]]) piece('Torso', 'jacket', 'sash_tail_' + i, [x, 6.6 + i, -3.5], [x + 1.1, 13.6, -3.1], fringe('Y'),
  { cloth: { outer: 'north', links: 4, plane: { normal: [0, 0, -1], at: 3.2 } } });

// The hood's cowl falling on the chest and the back, a red hem (behind the collar, above the mark).
piece('Chest', 'jacket', 'cowl', [-4.3, 22.4, -2.7], [4.3, 24.4, 2.7], rim((f, u, v, w, h) => {
  if (f === 'north' && Math.abs(u - (w - 1) / 2) < 2.6) return null;    // open in front on the collar
  return v === h - 1 ? 'X' : u % 4 === 3 ? 'h' : 'H';
}), hood);

// ---- the arms: the sleeves, gathered at the wrist by black wraps, the hands
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'right' : 'left';
  const [a0, a1] = xs(4, 8), [c0, c1] = xs(3.3, 8.7), [w0, w1] = xs(3.8, 8.2), [p0, p1] = xs(3.8, 8.3), [r0, r1] = xs(3.7, 8.3);
  // A fold of the cowl over the shoulder.
  piece('Arm_' + side, part + 'Sleeve', 'shoulder_' + s, [p0, 21.6, -2.5], [p1, 24.3, 2.5], rim((f, u, v, w, h) => (v === h - 1 && isSide(f) ? 'X' : u % 4 === 3 ? 'h' : 'H')), hood);
  // A thin red cord round the wrist over the wraps.
  piece('Hand_' + side, part + 'Sleeve', 'wrist_cord_' + s, [r0, 12.6, -2.3], [r1, 13.1, 2.3], rim(all('X')));
  piece('Arm_' + side, part + 'Arm', 'upper_' + s, [a0, 17, -2], [a1, 24, 2], cloth('R'), { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'fore_' + s, [a0, 11, -2], [a1, 17, 2], (f, u, v, w, h) => (f === 'down' || v >= h - 2 ? 'S' : 'R'), { hinge: { y: 17, bone: 'Arm_' + side } });
  // The sleeve's wide end, a red hem.
  piece('Hand_' + side, part + 'Sleeve', 'cuff_' + s, [c0, 14.4, -2.9], [c1, 17.2, 2.9],
    rim((f, u, v, w, h) => (!isSide(f) ? 'R' : v === h - 1 ? 'X' : cloth('R')(f, u, v))), { hinge: { y: 17, bone: 'Arm_' + side } });
  // The wraps from the sleeve to the wrist, crossing.
  piece('Hand_' + side, part + 'Sleeve', 'wrap_' + s, [w0, 12.0, -2.2], [w1, 14.6, 2.2], rim((f, u, v) => ((u + v) % 3 === 0 ? 'k' : 'K')));
});

// ---- the legs: trousers, the robe's long panels with their red hem, the shins bound in red, the shoes
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'right' : 'left';
  const [t0, t1] = xs(0, 4), [k0, k1] = xs(-0.4, 4.4), [b0, b1] = xs(-0.2, 4.2), [d0, d1] = xs(-0.3, 4.3);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], all('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'robe_' + s, [k0, 5.6, -2.45], [k1, 13.1, 2.45], (f, u, v, w, h) => {
    if (f === 'up' || f === 'down') return null;
    if (v === h - 1) return 'X';
    if (v === h - 2) return 'x';
    return cloth('R')(f, u, v);
  });
  // Black bindings on the shins, tied above and below with a thin red cord.
  piece('Leg_' + side, part + 'Pants', 'binding_' + s, [b0, 1.6, -2.25], [b1, 5.4, 2.25], rim((f, u, v, w, h) => (isSide(f) && (v === 0 || v === h - 1) ? 'X' : (u + v) % 3 === 0 ? 'k' : 'K')));
  piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.6], [d1, 1.8, 2.3], (f, u, v, w, h) => (v === h - 1 ? 'f' : 'F'));
  // The shoe's rounded toe, and the cords tied round the bindings, standing off them.
  const [o0, o1] = xs(0.2, 3.8), [q0, q1] = xs(-0.35, 4.35);
  piece('Leg_' + side, part + 'Pants', 'toe_' + s, [o0, 0, -3.1], [o1, 1.2, -2.6], (f, u, v) => (v === 0 ? 'f' : 'F'));
  for (const [i, y] of [[0, 2.2], [1, 4.4]]) piece('Leg_' + side, part + 'Pants', 'shin_cord_' + i + '_' + s, [q0, y, -2.4], [q1, y + 0.45, 2.4], rim(all('X')));
});

/** The cloth pieces for client/ClothLayer (as the Peak Captain's: peak-captain-3d.cjs). */
function clothStrips(pieces, atlas) {
  const strips = pieces.filter(p => p.cloth).map(p => {
    const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, a = (p.tilt || 0) * Math.PI / 180;
    const outer = p.cloth.outer, inner = outer === 'south' ? 'north' : 'south';
    const z = outer === 'south' ? z1 - (z1 - z0) / 2 : z0 + (z1 - z0) / 2;
    const face = f => { const [u0, v0, u1, v1] = p.uv[f]; return f === 'south' ? [u0, v0, u1, v1] : [u1, v0, u0, v1]; };
    return { name: p.name, joint: p.bone, top: [[x0, y1, z], [x1, y1, z]], length: y1 - y0, thickness: z1 - z0,
      drop: [0, -Math.cos(a), -Math.sin(a)].map(v => Math.round(v * 1e4) / 1e4), links: p.cloth.links,
      outer: face(outer), inner: face(inner), plane: p.cloth.plane };
  });
  return { texture_size: atlas, strips };
}

if (require.main === module) {
  // The game files are the Blood Sect Decor addon's (blood-cultist-addon.cjs); here, the Blockbench project.
  const faces = kit.pack(PIECES, ATLAS);
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'blood_cultist'), { recursive: true });
  const atlases = OUTFITS.map(o => kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
  OUTFITS.forEach((o, i) => fs.writeFileSync(path.join(art, 'blood_cultist', o.key + '.png'), atlases[i]));
  fs.writeFileSync(path.join(art, 'blood_cultist.bbmodel'), JSON.stringify(kit.bbmodel('blood_cultist', PIECES, OUTFITS, atlases, ATLAS, 'entity/blood_cultist'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Blood Cultists:', OUTFITS.length, 'outfits,', PIECES.length, 'pieces, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFITS };
