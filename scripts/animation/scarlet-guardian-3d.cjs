// The Guardian of the Scarlet Threshold (Gardien du Seuil Écarlate), the Blood Sect's boss, after Enzo's concept sheet of
// 16/10/2026: four blocks tall in game (the biped drawn 2.2 times larger), on Epic Fight's biped skeleton. Being that big,
// it is drawn finer than our other mobs: its cloth at 3 texels a pixel, its armour at 4, every pattern measured in real
// pixels (paint functions get x and y in pixels, and the size of one texel). As every model of ours, the body in cloth only
// and its armour apart (pieces of their own, worn on top, that it may drop):
//   the body     a black under-robe woven fine, crossed by crimson lapels with an embroidered edge; a thick crimson sash
//                knotted in front (its knot and loop in relief), its ends hanging; long panels of crimson and black to the
//                shins, hemmed, torn at their ends; sleeves bound with crimson strips; black trousers crossed with crimson
//                bindings; cloth boots; a hair knot that stands out of the helm;
//   the armour   a great helm of dark iron: a brow ridge, the eye slit under it with two red glints, a visor in relief with
//                its breathing holes, a ridge down its face, a crack bleeding red on its right, scratches and wear; a
//                gorget; lamellae laced with crimson cord on the breast, the flanks and the back, a square plate on the
//                breast; pauldrons in three tiers (a broad plate studded with raised rivets, two tiers of lamellae), a
//                blood gem on the left; banded vambraces and gauntlets; a fauld and flaring tassets; square knee plates,
//                greaves and sabatons; on the back an iron harness holding three great blood crystals, well apart in a
//                fan, faceted, with small shards at their feet.
//   art/blockbench/scarlet_guardian.bbmodel (+ scarlet_guardian/body.png)         the body;
//   art/blockbench/scarlet_guardian_armor.bbmodel (+ scarlet_guardian/armor.png)  the armour.
// The game files (meshes, animations, the boss) come once Enzo validates the look.
// Usage: node scripts/animation/scarlet-guardian-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide } = kit;

const ROOT = path.join(__dirname, '..', '..');
/** Texels a pixel: the cloth's, the armour's; their atlases. */
const DB = 3, DA = 4, ATLAS = 512, ARMOUR_ATLAS = 512;
/** How much larger than the biped it is drawn in game: 4 blocks tall. */
const SCALE = 2.2;

/** Body: S skin, A hair, K black cloth, R crimson cloth, W crimson strips, P trousers, B boots, N cord, O embroidery (dull gold). */
const BODY = { key: 'body', c: { S: '#a9805f', A: '#141012', K: '#1d181a', R: '#7a1c22', W: '#8e2028', P: '#191416', B: '#151112',
  N: '#5a1218', O: '#8a6a3a' } };
/**
 * Armour: I dark iron, J its light edge, G bronze rims and bands, V rivets, Q blood red (the crack, the gem's base), H the
 * gem's light, E the eyes' glow in the slit, Y blood crystal, Z its light, L black leather, W crimson lacing, X scratches.
 */
const ARMOUR = { key: 'armor', c: { I: '#3b3738', J: '#5c5656', G: '#6e5536', V: '#b09464', Q: '#9e1a28', H: '#ff6a7a', E: '#ff3a32',
  Y: '#8a1424', Z: '#d93a52', L: '#141012', W: '#6a1a20', X: '#57514f' } };

/** A small deterministic hash of a texel: 0 to 1. */
const hash = (a, b, c = 0) => { let n = (a * 374761393 + b * 668265263 + c * 2147483647) | 0; n = (n ^ (n >>> 13)) * 1274126177; return ((n ^ (n >>> 16)) >>> 0) / 4294967296; };

const BODY_PIECES = [], ARMOUR_PIECES = [];
/** A paint in pixels: (face, x, y, width, height, texel) from the piece's own texels at its density. */
const inPixels = (paint, d) => (f, u, v, w, h, o) => paint(f, (u + 0.5) / d, (v + 0.5) / d, w / d, h / d, 1 / d, o);
const cloth = (bone, part, name, from, to, paint, extra = {}) => {
  const d = extra.density || DB;
  BODY_PIECES.push({ bone, part, name, from, to, ...extra, density: d, paint: inPixels(paint, d) });
};
const steel = (bone, part, name, from, to, paint, extra = {}) => {
  const d = extra.density || DA;
  ARMOUR_PIECES.push({ bone, part, name, from, to, ...extra, density: d, paint: inPixels(paint, d) });
};
const both = fn => { for (const [side, sx] of [['R', 1], ['L', -1]]) fn(side, sx, (a, b) => (sx > 0 ? [a, b] : [-b, -a]), side.toLowerCase()); };
/** Only a piece's sides painted, its top and bottom left out (a band round a limb). */
const rim = paint => (f, x, y, W, H, t, o) => (f === 'up' || f === 'down' ? null : paint(f, x, y, W, H, t, o));
/** Whether (x, y) lies within `t` of a piece's edge. */
const nearEdge = (x, y, W, H, t, k = 1) => x < t * k || y < t * k || x > W - t * k || y > H - t * k;

// ================================================================ the cloth's patterns

/** Woven cloth: a fine diagonal twill, soft folds every 1.5 pixels, a little wear. */
const weave = l => (f, x, y, W, H, t) => {
  const lo = l.toLowerCase();
  if (isSide(f) && (x % 1.5) < t) return lo;                     // a fold
  if (Math.floor((x + y) / t) % 4 === 0 && hash(Math.floor(x / t), Math.floor(y / t)) < 0.5) return lo;   // the twill
  return l;
};
/** A hem: a band of `band` along the bottom, a stitched line above it. */
const hemmed = (paint, band) => (f, x, y, W, H, t, o) => {
  if (isSide(f) && y > H - 0.5) return band;
  if (isSide(f) && y > H - 0.5 - t && Math.floor(x / t) % 3 !== 0) return 'O';
  return paint(f, x, y, W, H, t, o);
};
/** A torn end: the last pixels torn to uneven lengths, strip by strip (each strip a pixel wide). */
const torn = (paint, depth) => (f, x, y, W, H, t, o) => {
  if (isSide(f)) {
    const strip = Math.floor(x), cut = H - depth * hash(strip, 7) - t * Math.floor(hash(Math.floor(x / t), 3) * 2);
    if (y > cut) return null;
  }
  return paint(f, x, y, W, H, t, o);
};

// ================================================================ the body, in cloth

cloth('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, x, y) => (f === 'north' && y >= 3 ? 'S' : 'A'), { density: 1 });
cloth('Head', 'hat', 'hair_knot', [-1.2, 33.4, -0.2], [1.2, 36.0, 2.2], (f, x, y, W, H, t) => (y > H - 0.5 ? 'N' : Math.floor(y / t) % 3 === 0 ? 'a' : 'A'));
cloth('Head', 'hat', 'hair_knot_top', [-0.7, 36.0, 0.3], [0.7, 37.0, 1.7], (f, x, y, W, H, t) => ((x + y) % 1 < t ? 'a' : 'A'));
// The robe: black, the crimson lapels crossed right over left down to the sash, their edge embroidered in dull gold.
cloth('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, x, y, W, H, t) => {
  if (f === 'north') {
    const c = Math.abs(x - W / 2), outer = 2.6 - y * 0.42, inner = outer - 1.1;
    if (c <= outer && c >= inner) return Math.abs(c - outer) < t || Math.abs(c - inner) < t ? 'O' : weave('R')(f, x, y, W, H, t);
  }
  return weave('K')(f, x, y, W, H, t);
}, { hinge: { y: 18, bone: 'Torso' } });
cloth('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, x, y, W, H, t) => (f === 'down' ? 'P' : weave('K')(f, x, y, W, H, t)), { hinge: { y: 18, bone: 'Chest' } });
// The sash: thick, its edges embroidered; knotted in front a little to the left (the knot and a loop in relief), its two
// ends hanging, torn, to the knees.
const sash = (f, x, y, W, H, t) => (isSide(f) && (y < t * 2 || y > H - t * 2) ? (y < t || y > H - t ? 'r' : 'O') : weave('R')(f, x, y, W, H, t));
cloth('Torso', 'jacket', 'sash', [-4.7, 13.0, -2.75], [4.7, 15.8, 2.75], rim(sash));
cloth('Torso', 'jacket', 'sash_knot', [-2.9, 12.7, -3.7], [-0.2, 16.1, -2.7], (f, x, y, W, H, t) => (nearEdge(x, y, W, H, t) ? 'r' : weave('R')(f, x, y, W, H, t)));
cloth('Torso', 'jacket', 'sash_loop', [-4.4, 13.4, -3.5], [-2.9, 15.6, -2.8], (f, x, y, W, H, t) => (x < t * 2 ? 'r' : weave('R')(f, x, y, W, H, t)));
for (const [i, x, len] of [[0, -2.8, 6.2], [1, -1.4, 7.6]])
  cloth('Torso', 'jacket', 'sash_end_' + i, [x, 13.0 - len, -3.7], [x + 1.3, 13.0, -3.25], torn(weave('R'), 1.4));
// The robe's long panels: front and back of each thigh, crimson with black strips, hemmed, torn at their ends; black ones
// at the sides.
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightPants' : 'leftPants';
  const [p0, p1] = xs(-0.8, 4.8);
  const panel = (f, x, y, W, H, t) => (Math.floor(x) % 3 === 1 ? weave('K')(f, x, y, W, H, t) : weave('R')(f, x, y, W, H, t));
  cloth('Thigh_' + side, part, 'panel_front_' + s, [p0, 2.4, -3.0], [p1, 13.2, -2.6], torn(panel, 2.6));
  cloth('Thigh_' + side, part, 'panel_back_' + s, [p0, 1.8, 2.6], [p1, 13.2, 3.0], torn(panel, 2.6));
  cloth('Thigh_' + side, part, 'panel_side_' + s, sx > 0 ? [4.4, 3.4, -2.6] : [-4.8, 3.4, -2.6], sx > 0 ? [4.8, 13.0, 2.6] : [-4.4, 13.0, 2.6], torn(weave('K'), 2.0));
});
// Arms: the sleeves black, bound by crimson strips crossing at the forearm, the hands.
const strips = base => (f, x, y, W, H, t) => {
  const a = (x + y) % 2, b = (x - y + 40) % 2;
  if (a < 0.45 || b < 0.45) return (a < t || b < t) ? 'n' : 'W';
  return weave(base)(f, x, y, W, H, t);
};
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightArm' : 'leftArm', sleeve = sx > 0 ? 'rightSleeve' : 'leftSleeve';
  const [a0, a1] = xs(4, 8), [w0, w1] = xs(3.8, 8.2);
  cloth('Arm_' + side, part, 'upper_' + s, [a0, 17, -2], [a1, 24, 2], weave('K'), { hinge: { y: 17, bone: 'Hand_' + side } });
  cloth('Hand_' + side, part, 'fore_' + s, [a0, 11, -2], [a1, 17, 2], (f, x, y, W, H, t) => (y > H - 2 || f === 'down' ? 'S' : weave('K')(f, x, y, W, H, t)), { hinge: { y: 17, bone: 'Arm_' + side } });
  cloth('Hand_' + side, sleeve, 'strips_' + s, [w0, 13.0, -2.2], [w1, 17.0, 2.2], rim(strips('K')));
});
// Legs: black trousers, crimson bindings crossed on the shins, cloth boots with a seam.
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightLeg' : 'leftLeg', pants = sx > 0 ? 'rightPants' : 'leftPants';
  const [t0, t1] = xs(0, 4), [b0, b1] = xs(-0.2, 4.2), [o0, o1] = xs(-0.3, 4.3);
  cloth('Thigh_' + side, part, 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], weave('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  cloth('Leg_' + side, part, 'shin_' + s, [t0, 0, -2], [t1, 6, 2], weave('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  cloth('Leg_' + side, pants, 'bindings_' + s, [b0, 1.6, -2.2], [b1, 6.0, 2.2], rim(strips('P')));
  cloth('Leg_' + side, pants, 'boot_' + s, [o0, 0, -2.4], [o1, 1.6, 2.4], (f, x, y, W, H, t) => (y < t ? 'b' : isSide(f) && Math.abs(y - H / 2) < t / 2 ? 'b' : 'B'));
});

// ================================================================ the armour's patterns

/** Iron with wear: a scratch now and then (a short light line), a darker pit. */
const iron = (x, y, t) => {
  const hx = Math.floor(x / t), hy = Math.floor(y / t);
  if (hash(Math.floor(x * 0.7), Math.floor(y * 1.3), 5) < 0.05 && Math.abs(((x - y * 0.6) % 1.7)) < t) return 'X';
  return hash(hx, hy, 9) < 0.06 ? 'i' : 'I';
};
/** Riveted plate: a thin bronze rim, a rivet (two texels) inset in each corner, a lighter top edge, wear. */
const plate = (f, x, y, W, H, t) => {
  if (f === 'up') return y < t || x < t || x > W - t ? 'G' : iron(x, y, t) === 'I' ? 'J' : 'X';
  if (f === 'down') return 'i';
  if (nearEdge(x, y, W, H, t)) return y < t ? 'J' : 'G';
  const rx = Math.min(x, W - x), ry = Math.min(y, H - y);
  if (rx > t * 1.5 && rx < t * 3.5 && ry > t * 1.5 && ry < t * 3.5) return rx > t * 2 && rx < t * 3 && ry > t * 2 && ry < t * 3 ? 'V' : 'g';
  if (y < t * 2) return 'J';
  return iron(x, y, t);
};
/** Lamellae: rows of small iron plates (1.5 by 2 pixels) laced with crimson cord, a rivet on each, a bronze hem. */
const lamellae = (f, x, y, W, H, t) => {
  if (!isSide(f)) return f === 'up' ? 'J' : 'i';
  if (y > H - t * 2) return y > H - t ? 'g' : 'G';
  const row = Math.floor(y / 2), py = y - row * 2, px = (x + row * 0.75) % 1.5;
  if (py > 2 - t * 1.5) return (Math.floor(x / t) % 3 === 0) ? 'L' : 'W';    // the lacing between rows
  if (px > 1.5 - t) return 'i';                                              // the gap between plates
  if (py < t) return 'J';                                                    // each plate's lit top edge
  if (Math.abs(px - 0.75) < t * 0.6 && Math.abs(py - 0.5) < t * 0.6 && (Math.floor((x + row * 0.75) / 1.5) + row) % 2 === 0) return 'G';   // a small rivet, every other plate
  return iron(x, y, t);
};
/** Banded: iron bands a pixel and a half high, a bronze edge at each end, a line of rivets on each band. */
const banded = (f, x, y, W, H, t) => {
  if (!isSide(f)) return f === 'up' ? 'J' : 'i';
  if (y < t * 2 || y > H - t * 2) return 'G';
  const b = y % 1.5;
  if (b < t) return 'i';
  if (b < t * 2) return 'J';
  if (Math.abs(b - 0.75) < t * 0.6 && Math.abs((x % 2) - 1) < t * 0.6) return 'G';
  return iron(x, y, t);
};

// ================================================================ the armour

// ---- the great helm: dark iron, a bronze band at its brow and its foot, rivets along them.
steel('Head', 'head', 'helm', [-4.8, 23.2, -4.8], [4.8, 33.4, 4.8], (f, x, y, W, H, t) => {
  if (f === 'down') return null;
  if (f === 'up') return nearEdge(x, y, W, H, t, 2) ? 'G' : iron(x, y, t);
  if (y < t * 2 || y > H - t * 2) return 'G';
  if ((Math.abs(y - 0.9) < t * 0.6 || Math.abs(y - (H - 0.9)) < t * 0.6) && Math.abs((x % 2) - 1) < t * 0.6) return 'V';
  if (f === 'north') {
    const c = W / 2;
    // The slit (under the brow ridge, 3.2 to 4.0 pixels down) and the eyes' glints in it.
    if (y > 3.15 && y < 4.0 && Math.abs(x - c) > 0.45 && x > 0.9 && x < W - 0.9) return Math.abs(Math.abs(x - c) - 2.4) < 0.45 && y < 3.6 ? 'E' : 'L';
    // The ridge down its face.
    if (Math.abs(x - c) < 0.45 && y > 2.0) return Math.abs(x - c) < t ? 'J' : 'I';
    // The crack: down from the brow on the helm's right (the viewer's left), past the slit, to the jaw.
    const crack = [[1.9, 1.6], [2.1, 2.2], [1.8, 2.8], [2.0, 4.4], [2.4, 5.0], [2.2, 5.8], [2.6, 6.6], [2.9, 7.2]];
    for (let i = 1; i < crack.length; i++) {
      const [x0, y0] = crack[i - 1], [x1, y1] = crack[i];
      if (y >= y0 && y <= y1 && Math.abs(x - (x0 + (x1 - x0) * (y - y0) / (y1 - y0))) < t * 1.2) return y > 4.4 && Math.abs(x - (x0 + (x1 - x0) * (y - y0) / (y1 - y0))) < t * 0.5 ? 'H' : 'Q';
    }
  }
  return iron(x, y, t);
});
steel('Head', 'hat', 'helm_top', [-3.2, 33.4, -3.2], [3.2, 33.9, 3.2], (f, x, y, W, H, t) => (nearEdge(x, y, W, H, t) ? 'G' : iron(x, y, t)));
// The brow ridge over the slit, and the visor below it, standing out of the helm's face.
steel('Head', 'hat', 'brow', [-4.9, 30.2, -5.15], [4.9, 31.0, -4.8], (f, x, y, W, H, t) => (y < t ? 'J' : y > H - t ? 'i' : f === 'north' && Math.abs((x % 2) - 1) < t * 0.6 && Math.abs(y - H / 2) < t * 0.6 ? 'G' : 'I'));
steel('Head', 'hat', 'visor', [-4.5, 24.0, -5.05], [4.5, 29.4, -4.8], (f, x, y, W, H, t) => {
  if (f !== 'north') return y < t ? 'J' : 'I';
  if (nearEdge(x, y, W, H, t)) return y < t ? 'J' : 'G';
  // Breathing holes in two diamonds of five on the visor's left (the viewer's right).
  for (const [cx, cy] of [[W - 2.2, 1.8], [W - 2.2, 3.4]])
    for (const [dx, dy] of [[0, 0], [-0.6, -0.6], [0.6, -0.6], [-0.6, 0.6], [0.6, 0.6]])
      if (Math.abs(x - (cx + dx)) < t * 1.1 && Math.abs(y - (cy + dy)) < t * 1.1) return 'L';
  // The crack runs on down the visor.
  if (Math.abs(x - (2.9 + y * 0.12)) < t * 1.2 && y < 2.6) return 'Q';
  if (Math.abs(x - W / 2) < 0.45) return Math.abs(x - W / 2) < t ? 'J' : 'I';
  return iron(x, y, t);
});
// ---- the gorget and the cuirass.
steel('Chest', 'torso', 'gorget', [-3.8, 23.0, -3.2], [3.8, 24.9, 3.2], rim(banded));
for (const [n, x0, x1] of [['breast_r', 2.2, 4.6], ['breast_l', -4.6, -2.8]])
  steel('Chest', 'torso', n, [x0, 15.8, -3.0], [x1, 22.8, -2.3], lamellae, { hinge: { y: 18, bone: 'Torso' } });
steel('Chest', 'torso', 'breast_plate', [-1.8, 16.8, -3.25], [1.8, 19.8, -2.6], plate);
steel('Chest', 'torso', 'breast_strap', [-2.8, 17.9, -2.75], [2.2, 18.5, -2.45], (f, x, y, W, H, t) => (Math.abs(y - H / 2) < t ? 'i' : 'L'));
steel('Chest', 'torso', 'back_plates', [-4.6, 13.4, 2.3], [4.6, 22.8, 3.0], lamellae, { hinge: { y: 18, bone: 'Torso' } });
for (const [n, x0, x1] of [['flank_r', 4.0, 4.6], ['flank_l', -4.6, -4.0]])
  steel('Chest', 'torso', n, [x0, 15.6, -2.3], [x1, 22.8, 2.3], lamellae);
// The fauld under the sash, the tassets flaring over each thigh, front, side and back.
steel('Torso', 'torso', 'fauld', [-4.7, 10.8, -3.1], [4.7, 13.0, -2.6], plate);
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightLeg' : 'leftLeg';
  const [t0, t1] = xs(-0.9, 4.9);
  steel('Thigh_' + side, part, 'tasset_front_' + s, [t0, 7.8, -3.4], [t1, 12.2, -3.0], lamellae);
  steel('Thigh_' + side, part, 'tasset_back_' + s, [t0, 8.4, 3.0], [t1, 12.2, 3.4], lamellae);
  const [o0, o1] = (sx > 0 ? [4.6, 5.0] : [-5.0, -4.6]);
  steel('Thigh_' + side, part, 'tasset_side_' + s, [o0, 8.0, -3.0], [o1, 12.2, 3.0], lamellae);
});
// ---- the back's harness: two iron bands across and three uprights, holding three great blood crystals.
steel('Chest', 'torso', 'harness_hi', [-4.8, 21.0, 3.0], [4.8, 22.4, 3.8], plate);
steel('Torso', 'torso', 'harness_lo', [-4.8, 14.0, 3.0], [4.8, 15.4, 3.8], plate);
for (const x of [-3.4, 0, 3.4]) steel('Chest', 'torso', 'harness_bar_' + (x + 4), [x - 0.4, 15.4, 3.2], [x + 0.4, 21.0, 3.6], banded);
/** A crystal's face: a lit edge and a shadowed one, a glint running up it, darker veins inside, a bright point or two. */
const crystalFace = (f, x, y, W, H, t) => {
  if (f === 'up' || f === 'down') return 'Z';
  if (x < W * 0.18) return x < t ? 'H' : 'Z';
  if (x > W * 0.82) return 'y';
  if (Math.abs(x - W * 0.42 - y * 0.05) < t) return 'Z';
  if (Math.abs((x * 0.8 + y * 0.35) % 2.2 - 1.1) < t * 0.8 && hash(Math.floor(y), 11) < 0.5) return 'y';
  return hash(Math.floor(x / t), Math.floor(y / t), 13) < 0.015 ? 'H' : 'Y';
};
/** A crystal: two crossed columns, its point narrowing in three steps, a socket of iron at its foot and small shards
 *  beside it; leaning back (lean, about x) and out to its side (roll, about z), about its foot. */
function crystal(name, cx, y0, height, r, cz, lean, roll = 0) {
  const pivot = [cx, y0, cz];
  const turn = ([x, y, z]) => {
    const rz = roll * Math.PI / 180, rx = lean * Math.PI / 180;
    let dx = x - cx, dy = y - y0, dz = z - cz;
    [dx, dy] = [dx * Math.cos(rz) - dy * Math.sin(rz), dx * Math.sin(rz) + dy * Math.cos(rz)];
    [dy, dz] = [dy * Math.cos(rx) - dz * Math.sin(rx), dy * Math.sin(rx) + dz * Math.cos(rx)];
    return [cx + dx, y0 + dy, cz + dz];
  };
  const at = { turnCorner: turn, pivot };
  steel('Chest', 'torso', name + '_socket', [cx - r - 0.45, y0 - 0.4, cz - r - 0.45], [cx + r + 0.45, y0 + 1.3, cz + r + 0.45], rim(banded), at);
  steel('Chest', 'torso', name + '_body', [cx - r, y0 + 1.3, cz - r * 0.72], [cx + r, y0 + height, cz + r * 0.72], crystalFace, at);
  steel('Chest', 'torso', name + '_facet', [cx - r * 0.72, y0 + 1.3, cz - r], [cx + r * 0.72, y0 + height, cz + r], crystalFace, at);
  [[0.76, 2.4], [0.5, 2.2], [0.26, 2.0]].forEach(([k, len], i) => {
    const b = y0 + height + [0, 2.4, 4.6][i];
    steel('Chest', 'torso', name + '_tip_' + i, [cx - r * k, b, cz - r * k], [cx + r * k, b + len, cz + r * k], crystalFace, at);
  });
  // Two small shards at its foot, out of the socket.
  for (const [i, dx, dz, hh, rr] of [[0, -r * 0.9, -r * 0.5, 3.0, 0.4], [1, r * 0.95, r * 0.3, 2.2, 0.32]])
    steel('Chest', 'torso', name + '_shard_' + i, [cx + dx - rr, y0 + 0.8, cz + dz - rr], [cx + dx + rr, y0 + 0.8 + hh, cz + dz + rr], crystalFace, at);
}
crystal('crystal_l', -4.6, 20.4, 12.0, 1.5, 6.4, -8, -16);
crystal('crystal_m', 0.0, 21.0, 15.0, 1.8, 7.0, -11);
crystal('crystal_r', 4.6, 20.4, 12.0, 1.5, 6.4, -8, 16);
// ---- the pauldrons: a broad plate studded with raised rivets and two tiers of lamellae below it, a blood gem on the
// left; the vambraces, gauntlets, knee plates, greaves and sabatons.
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightArm' : 'leftArm';
  [[22.0, 25.4, 3.0, 9.9, 3.7, plate], [19.8, 22.4, 3.2, 10.3, 3.55, lamellae], [17.6, 20.2, 3.4, 10.6, 3.4, lamellae]].forEach(([y0, y1, a, b, d, paint], i) => {
    const [x0, x1] = xs(a, b);
    steel('Arm_' + side, part, 'pauldron_' + i + '_' + s, [x0, y0, -d], [x1, y1, d], rim(paint));
  });
  // Raised rivets along the broad plate's front and back edges.
  for (const z of [-3.85, 3.55]) for (const k of [0, 1, 2]) {
    const [r0, r1] = xs(4.2 + k * 2.2, 4.7 + k * 2.2);
    steel('Arm_' + side, part, 'stud_' + (z < 0 ? 'f' : 'b') + k + '_' + s, [r0, 24.3, z], [r1, 24.8, z + 0.3], (f, x, y, W, H, t) => (y < t ? 'V' : 'g'));
  }
  if (sx < 0) {
    // The gem: a diamond of blood crystal in a bronze setting on the left pauldron's front, seen only from the front.
    steel('Arm_L', part, 'gem', [-8.2, 22.6, -3.98], [-5.4, 25.0, -3.72], (f, x, y, W, H, t) => {
      if (f !== 'north') return null;
      const d = Math.abs(x - W / 2) / (W / 2) + Math.abs(y - H / 2) / (H / 2);
      if (d > 1.0) return null;
      if (d > 0.82) return 'G';
      if (d < 0.22) return 'H';
      return x < W / 2 ? (y < H / 2 ? 'Z' : 'Y') : (y < H / 2 ? 'Y' : 'Q');
    }, { density: 6 });
  }
  const [v0, v1] = xs(3.2, 8.8), [g0, g1] = xs(3.4, 8.6);
  steel('Hand_' + side, part, 'vambrace_' + s, [v0, 12.4, -2.9], [v1, 17.0, 2.9], rim(banded));
  steel('Hand_' + side, part, 'gauntlet_' + s, [g0, 10.4, -2.7], [g1, 12.4, 2.7], (f, x, y, W, H, t) => {
    if (f === 'down') return 'i';
    if (y < t * 2) return 'G';
    // The fingers' plates: a seam every pixel across, a knuckle row.
    if (isSide(f) && (x % 1) < t) return 'i';
    if (isSide(f) && Math.abs(y - 0.7) < t) return 'J';
    return iron(x, y, t);
  });
});
both((side, sx, xs, s) => {
  const part = sx > 0 ? 'rightLeg' : 'leftLeg';
  const [k0, k1] = xs(-0.1, 4.1), [r0, r1] = xs(-0.4, 4.4), [b0, b1] = xs(-0.6, 4.6);
  steel('Leg_' + side, part, 'knee_' + s, [k0, 4.6, -3.25], [k1, 8.2, -2.4], plate);
  steel('Leg_' + side, part, 'greave_' + s, [r0, 1.8, -2.75], [r1, 4.8, 2.75], rim(banded));
  steel('Leg_' + side, part, 'sabaton_' + s, [b0, 0, -3.6], [b1, 1.9, 2.8], (f, x, y, W, H, t) => {
    if (y < t) return 'J';
    if (f === 'north' && y < t * 3) return 'G';
    if (isSide(f) && (x % 1.2) < t && f === 'north') return 'i';
    return iron(x, y, t);
  });
});

module.exports = { BODY_PIECES, ARMOUR_PIECES, BODY, ARMOUR, SCALE, ATLAS, ARMOUR_ATLAS };

if (require.main === module) {
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'scarlet_guardian'), { recursive: true });
  for (const [name, pieces, look, size] of [['scarlet_guardian', BODY_PIECES, BODY, ATLAS], ['scarlet_guardian_armor', ARMOUR_PIECES, ARMOUR, ARMOUR_ATLAS]]) {
    const faces = kit.pack(pieces, size);
    const used = Math.max(...faces.map(f => f.v + f.h));
    if (used > size) throw new Error(name + ': the atlas overflows (' + used + ' rows of ' + size + ')');
    const atlas = kit.png(size, size, kit.paintAtlas(faces, look, size));
    fs.writeFileSync(path.join(art, 'scarlet_guardian', look.key + '.png'), atlas);
    fs.writeFileSync(path.join(art, name + '.bbmodel'), JSON.stringify(kit.bbmodel(name, pieces, [look], [atlas], size, 'entity/scarlet_guardian'), null, 1));
    console.log('wrote', name + ':', pieces.length, 'pieces, atlas rows used', used, 'of', size);
  }
}
