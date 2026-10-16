// The Blood Sect's soldiers, its middle rank (Enzo, 17/10/2026: "rang intermédiaire, le modèle d'en dessous en plus gradé :
// meilleure armure, meilleur tout"). The Blood Cultists' body (blood-cultist-3d.cjs) in richer cloth, with a uniform of
// their own, and real armour where the cultists had a pad or two. As every model of ours, the body in cloth only and the
// armour apart (pieces of their own, to be dropped and worn).
//   The cloth (three outfits):
//     blade    bare-headed, the hair short, the blood-red mask; a crimson robe;
//     reaver   a blood-red hood and the mask; a black robe;
//     warden   a black hood and the long veil; a crimson robe;
//   all three over the cultists' robe (its sash's tails gone): a rank's tabard hanging in front from the sash to the knees, bordered in bone white,
//   the sect's drop on it; a second sash of black over the red; the lapels in bone white; the cuffs hemmed in bone white.
//   The armour (three proposals, each its own Blockbench project):
//     a_lamellar   a rounded iron helm with a lamellar neck guard and a red tassel on its crown; a cuirass of black iron
//                  lames laced in red, the sect's drop painted on its back; pauldrons in two tiers; iron bracers;
//                  tassets over the thighs; banded greaves;
//     b_lacquered  an iron face guard over the mask, fangs painted on it; a cuirass of blood-red lacquered plates rimmed
//                  in black iron, the drop embossed on the breast; broad flaring pauldrons; lacquered bracers; knee guards;
//                  lacquered greaves and iron toe caps. Chosen by Enzo on 17/10/2026: the soldiers' armour, in game as the
//                  Blood Lacquer set (blood-soldier-game.cjs);
//     c_scale      a coat of black iron scales to the thighs, rimmed in red; a high iron gorget; scaled pauldrons; iron
//                  bracers; a crimson half-cape over the left shoulder, held by a bronze clasp.
//   art/blockbench/blood_soldier.bbmodel (+ blood_soldier/<outfit>.png)            the body;
//   art/blockbench/blood_soldier_armor_<design>.bbmodel (+ blood_soldier_armor/<design>.png)   the armours.
// The game files come once Enzo picks.
// Usage: node scripts/animation/blood-soldier-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const cult = require('./blood-cultist-3d.cjs');
const { isSide, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128, ARMOUR_ATLAS = 256;

/** The faces' common traits, as the cultists': red eyes, level brows, a stern mouth; harder. */
const FACE = { iris: '#c0202c', brow: 'angry', mouth: 'stern', fringe: [3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3] };
/** The cultists' letters (blood-cultist-3d.cjs), plus O the tabard and its drop's border (bone white is Z). */
const OUTFITS = [
  { key: 'blade', mask: true, t: { skin: '#cfa588', hair: '#141012', scar: [[11, 3], [11, 4], [12, 5]], fringe: [4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4] },
    c: { H: '#141114', R: '#5a1018', L: '#e2d6c0', X: '#a3202a', D: '#d8323a', M: '#8e1822', B: '#0e0b0c', K: '#151113', P: '#171215', F: '#0b0909', Y: '#141114', Z: '#e2d6c0', O: '#7c121c' } },
  { key: 'reaver', hood: true, mask: true, t: { skin: '#d8b49a', hair: '#0f0c0d' },
    c: { H: '#8e1822', R: '#161113', L: '#e2d6c0', X: '#a3202a', D: '#d8323a', M: '#5a0e14', B: '#0e0b0c', K: '#151113', P: '#151113', F: '#0b0909', Y: '#a3202a', Z: '#e2d6c0', O: '#5a1018' } },
  { key: 'warden', hood: true, veil: true, t: { skin: '#c99c80', hair: '#0f0c0d' },
    c: { H: '#100d0f', R: '#4a0f16', L: '#e2d6c0', X: '#a3202a', D: '#d8323a', M: '#141114', B: '#0b0909', K: '#141012', P: '#141012', F: '#0b0909', Y: '#100d0f', Z: '#e2d6c0', O: '#100d0f' } },
];
for (const o of OUTFITS) { o.t = { ...FACE, ...o.t }; o.c.S = o.t.skin; o.c.A = o.t.hair; }

// ---------------------------------------------------------------- the body: the cultists', and the uniform

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
// The cultists' pieces; their face redrawn from the soldiers' traits.
const facePaints = Object.fromEntries(OUTFITS.map(o => [o.key, hd.paint(o.t)]));
// The sash's two tails give way to the tabard.
for (const p of cult.PIECES.filter(p => !p.name.startsWith('sash_tail')))
  PIECES.push(p.name === 'face' ? { ...p, paint: (f, u, v, w, h, o) => facePaints[o.key](f, u, v, w, h, o) } : { ...p });
// The tabard: from the sash to the knees in front, its border bone white, the sect's drop on it (two texels a pixel).
piece('Torso', 'jacket', 'tabard', [-2.0, 6.2, -3.05], [2.0, 13.2, -2.75], (f, u, v, w, h) => {
  if (f !== 'north') return f === 'south' ? null : 'o';
  if (u <= 0 || u >= w - 1) return 'Z';
  if (v >= h - 2) return v === h - 1 ? (u % 2 ? null : 'Z') : 'Z';
  // The drop, 2 to 5.5 pixels down: round below, drawn to a point above.
  const cx = (w - 1) / 2, by = 8.5, r = 3.0, dy = v - by, half = dy >= 0 ? Math.sqrt(Math.max(0, r * r - dy * dy)) : r * (1 + dy / 5.4);
  if (dy > -5.4 && dy < r + 0.6 && Math.abs(u - cx) <= half + 1) return Math.abs(u - cx) > half - 0.2 || dy > r - 1 ? 'Z' : u < cx - 0.8 && dy > -2 ? 'D' : 'X';
  return u % 4 === 1 ? 'o' : 'O';
}, { density: 3 });
// A second sash of black over the red, narrower, knotted on the right.
piece('Torso', 'jacket', 'sash_black', [-4.7, 15.6, -2.7], [4.7, 16.5, 2.7], rim((f, u) => (u % 4 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_black_knot', [2.0, 15.3, -3.25], [3.4, 16.8, -2.7], (f, u, v) => (v === 0 ? 'b' : 'B'));
// The cuffs hemmed in bone white.
for (const [side, x0, x1] of [['r', 3.25, 8.75], ['l', -8.75, -3.25]])
  piece(side === 'r' ? 'Hand_R' : 'Hand_L', side === 'r' ? 'rightSleeve' : 'leftSleeve', 'cuff_hem_' + side, [x0, 14.3, -2.95], [x1, 14.75, 2.95], rim(() => 'Z'));

// ---------------------------------------------------------------- the armours

/**
 * I dark iron, J its light edge, G bronze, V bright bronze (rivets), W red lacing, Q blood-red lacquer, U its dark shade
 * (and the lacquer's light edge is T), N black leather, E the fangs' and the drop's bone white.
 */
const ARMOUR = { key: 'blood_soldier_armor', c: { I: '#3a3b40', J: '#6c6f78', G: '#6e5536', V: '#a88a5a', W: '#8e1822', Q: '#8a1620', U: '#5a0e14', T: '#b8303a', N: '#141012', E: '#e2d6c0' } };
const DESIGNS = ['a_lamellar', 'b_lacquered', 'c_scale'];
/** Each design's head: a helm (only over a bare head) or a face guard (over the mask). */
const HEAD = { a_lamellar: 'helm', b_lacquered: 'guard', c_scale: null };

const APIECES = [];
/** A paint in pixels, as the Guardian's (scarlet-guardian-3d.cjs): (face, x, y, width, height, texel). */
const inPixels = (paint, d) => (f, u, v, w, h, o) => paint(f, (u + 0.5) / d, (v + 0.5) / d, w / d, h / d, 1 / d, o);
const DA = 2;
const steel = (design, bone, part, name, from, to, paint, extra = {}) => {
  const d = extra.density || DA;
  APIECES.push({ design, bone, part, name, from, to, ...extra, density: d, paint: inPixels(paint, d) });
};
const both = fn => { for (const [side, sx] of [['R', 1], ['L', -1]]) fn(side, sx, (a, b) => (sx > 0 ? [a, b] : [-b, -a]), side.toLowerCase()); };
const sides = paint => (f, x, y, W, H, t, o) => (f === 'up' || f === 'down' ? null : paint(f, x, y, W, H, t, o));
const edge = (x, y, W, H, t) => x < t || y < t || x > W - t || y > H - t;

/** Iron, its lit top edge, a darker pit now and then. */
const iron = (x, y, t) => ((Math.floor(x / t) * 7 + Math.floor(y / t) * 13) % 29 === 0 ? 'i' : 'I');
/** Lamellae: rows of iron lames (1.5 by 2 pixels) laced in red, a bronze hem. */
const lamellae = (f, x, y, W, H, t) => {
  if (!isSide(f)) return f === 'up' ? 'J' : 'i';
  if (y > H - t) return 'G';
  const row = Math.floor(y / 2), py = y - row * 2, px = (x + row * 0.75) % 1.5;
  if (py > 2 - t) return Math.floor(x / t) % 2 ? 'W' : 'w';
  if (px > 1.5 - t) return 'i';
  if (py < t) return 'J';
  return iron(x, y, t);
};
/** Banded iron: bands a pixel high, a bronze edge at each end. */
const banded = (f, x, y, W, H, t) => {
  if (!isSide(f)) return f === 'up' ? 'J' : 'i';
  if (y < t || y > H - t) return 'G';
  const b = y % 1;
  return b < t ? 'i' : b < t * 2 ? 'J' : iron(x, y, t);
};
/** Lacquered plate: blood red, a black iron rim, a lit top edge, rows laced every 2 pixels. */
const lacquer = (f, x, y, W, H, t) => {
  if (f === 'up') return 'T';
  if (f === 'down') return 'u';
  if (edge(x, y, W, H, t)) return 'I';
  if (y < t * 2) return 'T';
  if ((y % 2) > 2 - t) return Math.floor(x / t) % 3 === 0 ? 'N' : 'u';
  return (y % 2) < t * 2 ? 'Q' : ((Math.floor(x / t) + Math.floor(y / t)) % 11 === 0 ? 'u' : 'Q');
};
/** Scales: overlapping rows of round-bottomed iron scales, each lit at its top. */
const scales = (f, x, y, W, H, t) => {
  if (!isSide(f)) return f === 'up' ? 'J' : 'i';
  if (y > H - t * 2) return 'W';
  const row = Math.floor(y / 1.25), py = y - row * 1.25, px = (x + (row % 2) * 0.5) % 1;
  if (py + Math.abs(px - 0.5) * 0.9 > 1.25 - t) return 'i';
  if (py < t) return 'J';
  return 'I';
};
/** The sect's drop: is (x, y) in a drop of radius r whose round bottom is centred at (cx, cy)? */
const inDrop = (x, y, cx, cy, r) => { const dy = y - cy, half = dy >= 0 ? Math.sqrt(Math.max(0, r * r - dy * dy)) : r * (1 + dy / (r * 1.9)); return dy > -r * 1.9 && Math.abs(x - cx) <= half; };

// ---- A: lamellar
{
  const D = 'a_lamellar';
  // The helm: a rounded bowl over the brow, a bronze band at its foot, a ridge front to back; the neck guard of lames
  // round the back and sides; a finial and its red tassel on the crown.
  steel(D, 'Head', 'hat', 'helm', [-4.55, 29.4, -4.55], [4.55, 32.9, 4.55], (f, x, y, W, H, t) => {
    if (f === 'down') return null;
    if (f === 'up') return Math.abs(x - W / 2) < 0.4 ? 'J' : edge(x, y, W, H, t) ? 'G' : iron(x, y, t);
    if (y > H - t * 3) return y > H - t ? 'g' : 'G';
    if (y > H - t * 4 && Math.abs((x % 1.5) - 0.75) < t * 0.6) return 'V';
    if (y < t) return 'J';
    return (f === 'north' || f === 'south') && Math.abs(x - W / 2) < 0.4 ? 'J' : iron(x, y, t);
  });
  steel(D, 'Head', 'hat', 'helm_top', [-3.4, 32.9, -3.4], [3.4, 33.5, 3.4], (f, x, y, W, H, t) => (edge(x, y, W, H, t) ? 'G' : 'I'));
  steel(D, 'Head', 'hat', 'finial', [-0.5, 33.5, -0.5], [0.5, 34.6, 0.5], (f, x, y, W, H, t) => (y < t ? 'V' : 'G'));
  steel(D, 'Head', 'hat', 'tassel', [-0.9, 32.4, -0.9], [0.9, 34.0, 0.9], (f, x, y, W, H, t) => (f === 'up' ? 'W' : f === 'down' ? null : (Math.floor(x / t) % 2 ? 'W' : 'w')),
    { pivot: [0, 34.0, 0] });
  steel(D, 'Head', 'hat', 'neck_back', [-4.7, 25.2, 4.45], [4.7, 29.5, 4.8], lamellae);
  for (const [n, x0] of [['neck_r', 4.45], ['neck_l', -4.8]]) steel(D, 'Head', 'hat', n, [x0, 25.6, -1.6], [x0 + 0.35, 29.5, 4.45], lamellae);
  // The cuirass: front and back of lames, the back with the drop painted on it; a gorget of bands.
  steel(D, 'Chest', 'torso', 'gorget', [-3.0, 22.9, -2.9], [3.0, 24.4, 2.9], sides(banded));
  steel(D, 'Chest', 'torso', 'front', [-4.3, 15.6, -2.95], [4.3, 22.9, -2.55], lamellae, { hinge: { y: 18, bone: 'Torso' } });
  steel(D, 'Chest', 'torso', 'back', [-4.3, 15.6, 2.55], [4.3, 22.9, 2.95], (f, x, y, W, H, t) => {
    if (f === 'south' && inDrop(x, y, W / 2, 4.0, 1.6)) return inDrop(x, y, W / 2, 4.0, 1.1) ? 'Q' : 'E';
    return lamellae(f, x, y, W, H, t);
  }, { hinge: { y: 18, bone: 'Torso' } });
  both((side, sx, xs, s) => {
    const arm = sx > 0 ? 'rightArm' : 'leftArm', leg = sx > 0 ? 'rightLeg' : 'leftLeg';
    const [p0, p1] = xs(3.5, 9.0), [q0, q1] = xs(3.7, 9.3), [b0, b1] = xs(3.7, 8.3), [t0, t1] = xs(0.5, 4.6), [g0, g1] = xs(-0.35, 4.35);
    steel(D, 'Arm_' + side, arm, 'pauldron_' + s, [p0, 22.3, -2.9], [p1, 25.0, 2.9], sides(lamellae));
    steel(D, 'Arm_' + side, arm, 'pauldron_lo_' + s, [q0, 20.3, -3.05], [q1, 22.6, 3.05], sides(lamellae));
    steel(D, 'Hand_' + side, arm, 'bracer_' + s, [b0, 12.3, -2.45], [b1, 14.3, 2.45], sides(banded));
    steel(D, 'Thigh_' + side, leg, 'tasset_' + s, [t0, 8.6, -2.95], [t1, 13.0, -2.6], lamellae);
    steel(D, 'Thigh_' + side, leg, 'tasset_side_' + s, sx > 0 ? [4.45, 8.6, -2.6] : [-4.8, 8.6, -2.6], sx > 0 ? [4.8, 13.0, 2.4] : [-4.45, 13.0, 2.4], lamellae);
    steel(D, 'Leg_' + side, leg, 'greave_' + s, [g0, 1.8, -2.45], [g1, 5.4, 2.45], sides(banded));
  });
}

// ---- B: lacquered
{
  const D = 'b_lacquered';
  // The face guard over the mask: dark iron, a bronze rim, two breathing slits each side, fangs painted in bone white.
  steel(D, 'Head', 'hat', 'face_guard', [-4.4, 23.5, -4.7], [4.4, 27.2, -4.4], (f, x, y, W, H, t) => {
    if (f !== 'north') return f === 'south' ? null : 'I';
    if (edge(x, y, W, H, t)) return 'G';
    const c = W / 2;
    // The fangs: two long ones from the top lip down each side of the middle, small ones between.
    if (y > 1.4 && y < 2.0) return 'N';
    for (const [fx, len] of [[c - 1.3, 1.1], [c + 1.3, 1.1], [c - 0.45, 0.5], [c + 0.45, 0.5], [c - 2.2, 0.5], [c + 2.2, 0.5]])
      if (y >= 2.0 && y < 2.0 + len && Math.abs(x - fx) < 0.3 * (1 - (y - 2.0) / len) + t / 2) return 'E';
    if (Math.abs(x - c) > 2.8 && Math.abs(x - c) < 3.6 && (y % 0.9) < t) return 'N';
    if (y < 1.4 && Math.abs(x - c) < 0.5) return 'J';
    return iron(x, y, t);
  });
  for (const [n, x0] of [['guard_r', 4.4], ['guard_l', -4.75]]) steel(D, 'Head', 'hat', n, [x0, 23.8, -4.7], [x0 + 0.35, 27.0, -2.8], (f, x, y, W, H, t) => (edge(x, y, W, H, t) ? 'G' : iron(x, y, t)));
  // The cuirass: lacquered plates front and back, the drop embossed on the breast (bone rim, dark lacquer).
  steel(D, 'Chest', 'torso', 'front', [-4.3, 15.6, -2.95], [4.3, 22.6, -2.55], (f, x, y, W, H, t) => {
    if (f === 'north' && inDrop(x, y, W / 2, 3.9, 1.6)) return inDrop(x, y, W / 2, 3.9, 1.1) ? (x < W / 2 ? 'T' : 'u') : 'E';
    return lacquer(f, x, y, W, H, t);
  }, { hinge: { y: 18, bone: 'Torso' } });
  steel(D, 'Chest', 'torso', 'back', [-4.3, 15.6, 2.55], [4.3, 22.6, 2.95], lacquer, { hinge: { y: 18, bone: 'Torso' } });
  steel(D, 'Chest', 'torso', 'collar', [-3.0, 22.6, -2.9], [3.0, 23.6, 2.9], sides((f, x, y, W, H, t) => (y < t ? 'J' : 'I')));
  both((side, sx, xs, s) => {
    const arm = sx > 0 ? 'rightArm' : 'leftArm', leg = sx > 0 ? 'rightLeg' : 'leftLeg';
    const [p0, p1] = xs(3.4, 9.6), [q0, q1] = xs(3.8, 9.4), [b0, b1] = xs(3.7, 8.3), [k0, k1] = xs(0.0, 4.0);
    // The pauldron flares: its plate tilted out over the arm, a lame under it.
    steel(D, 'Arm_' + side, arm, 'pauldron_' + s, [p0, 22.0, -3.1], [p1, 25.0, 3.1], sides(lacquer));
    steel(D, 'Arm_' + side, arm, 'pauldron_top_' + s, sx > 0 ? [3.6, 25.0, -2.8] : [-8.4, 25.0, -2.8], sx > 0 ? [8.4, 25.4, 2.8] : [-3.6, 25.4, 2.8], lacquer);
    steel(D, 'Arm_' + side, arm, 'pauldron_lo_' + s, [q0, 20.4, -3.0], [q1, 22.2, 3.0], sides(lacquer));
    steel(D, 'Hand_' + side, arm, 'bracer_' + s, [b0, 12.2, -2.5], [b1, 14.3, 2.5], sides(lacquer));
    steel(D, 'Leg_' + side, leg, 'knee_' + s, [k0, 4.6, -2.9], [k1, 7.4, -2.45], lacquer);
    steel(D, 'Thigh_' + side, leg, 'tasset_' + s, [xs(0.5, 4.5)[0], 9.6, -2.95], [xs(0.5, 4.5)[1], 13.0, -2.6], lacquer);
    // The greaves over the shin bindings, an iron cap over the shoe's toe.
    const [g0, g1] = xs(-0.4, 4.4), [o0, o1] = xs(0.1, 3.9);
    steel(D, 'Leg_' + side, leg, 'greave_' + s, [g0, 1.4, -2.55], [g1, 4.8, 2.55], sides(lacquer));
    steel(D, 'Leg_' + side, leg, 'toe_cap_' + s, [o0, 0, -3.25], [o1, 1.3, -2.55], (f, x, y, W, H, t) => (y < t ? 'J' : 'I'));
  });
}

// ---- C: scale
{
  const D = 'c_scale';
  steel(D, 'Chest', 'torso', 'gorget', [-3.2, 22.6, -3.0], [3.2, 24.9, 3.0], sides((f, x, y, W, H, t) => (y < t ? 'J' : y > H - t ? 'G' : banded(f, x, y, W, H, t))));
  steel(D, 'Chest', 'torso', 'coat_front', [-4.3, 15.6, -2.95], [4.3, 22.6, -2.55], scales, { hinge: { y: 18, bone: 'Torso' } });
  steel(D, 'Chest', 'torso', 'coat_back', [-4.3, 15.6, 2.55], [4.3, 22.6, 2.95], scales, { hinge: { y: 18, bone: 'Torso' } });
  both((side, sx, xs, s) => {
    const arm = sx > 0 ? 'rightArm' : 'leftArm', leg = sx > 0 ? 'rightLeg' : 'leftLeg';
    const [p0, p1] = xs(3.5, 9.0), [b0, b1] = xs(3.7, 8.3), [t0, t1] = xs(-0.5, 4.6);
    steel(D, 'Arm_' + side, arm, 'pauldron_' + s, [p0, 20.6, -2.95], [p1, 24.9, 2.95], sides(scales));
    steel(D, 'Hand_' + side, arm, 'bracer_' + s, [b0, 12.3, -2.45], [b1, 14.3, 2.45], sides(banded));
    // The coat's skirt over the thighs, front and back and the outer side.
    steel(D, 'Thigh_' + side, leg, 'skirt_' + s, [t0, 8.0, -2.95], [t1, 13.0, -2.6], scales);
    steel(D, 'Thigh_' + side, leg, 'skirt_back_' + s, [t0, 8.0, 2.6], [t1, 13.0, 2.95], scales);
    steel(D, 'Thigh_' + side, leg, 'skirt_side_' + s, sx > 0 ? [4.45, 8.0, -2.6] : [-4.8, 8.0, -2.6], sx > 0 ? [4.8, 13.0, 2.6] : [-4.45, 13.0, 2.6], scales);
  });
  // The half-cape over the left shoulder, to the back of the knee, its clasp on the breast.
  steel(D, 'Arm_L', 'leftArm', 'cape_shoulder', [-9.3, 23.0, -3.1], [-3.4, 25.3, 3.3], (f, x, y, W, H, t) => (f === 'down' ? null : y > H - t && isSide(f) ? 'J' : (Math.floor(x / t) % 4 === 0 ? 'u' : 'Q')));
  steel(D, 'Chest', 'torso', 'cape', [-4.4, 7.0, 3.0], [0.6, 23.4, 3.4], (f, x, y, W, H, t) => {
    if (!isSide(f)) return 'Q';
    if (y > H - t * 2) return y > H - t ? (Math.floor(x / t) % 2 ? null : 'E') : 'E';
    if (x < t && f === 'south') return 'E';
    return Math.floor(x / t) % 5 === 2 ? 'u' : 'Q';
  }, { tilt: -6, pivot: [0, 23.4, 3.0] });
  steel(D, 'Chest', 'torso', 'clasp', [-3.4, 21.6, -3.2], [-1.8, 23.2, -2.9], (f, x, y, W, H, t) => (f === 'north' && Math.hypot(x - W / 2, y - H / 2) < 0.4 ? 'Q' : 'V'));
}

module.exports = { PIECES, OUTFITS, APIECES, ARMOUR, DESIGNS, HEAD, ATLAS, ARMOUR_ATLAS };

if (require.main === module) {
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'blood_soldier'), { recursive: true });
  fs.mkdirSync(path.join(art, 'blood_soldier_armor'), { recursive: true });
  const faces = kit.pack(PIECES, ATLAS), used = Math.max(...faces.map(f => f.v + f.h));
  if (used > ATLAS) throw new Error('the body atlas overflows: ' + used);
  const atlases = OUTFITS.map(o => kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
  OUTFITS.forEach((o, i) => fs.writeFileSync(path.join(art, 'blood_soldier', o.key + '.png'), atlases[i]));
  fs.writeFileSync(path.join(art, 'blood_soldier.bbmodel'), JSON.stringify(kit.bbmodel('blood_soldier', PIECES, OUTFITS, atlases, ATLAS, 'entity/blood_soldier'), null, 1));
  console.log('wrote the Blood Soldiers:', OUTFITS.length, 'outfits,', PIECES.length, 'pieces, atlas rows', used, 'of', ATLAS);
  for (const design of DESIGNS) {
    const pieces = APIECES.filter(p => p.design === design);
    const af = kit.pack(pieces, ARMOUR_ATLAS), rows = Math.max(...af.map(f => f.v + f.h));
    if (rows > ARMOUR_ATLAS) throw new Error(design + ' overflows: ' + rows);
    const atlas = kit.png(ARMOUR_ATLAS, ARMOUR_ATLAS, kit.paintAtlas(af, { ...ARMOUR, key: design }, ARMOUR_ATLAS));
    fs.writeFileSync(path.join(art, 'blood_soldier_armor', design + '.png'), atlas);
    fs.writeFileSync(path.join(art, 'blood_soldier_armor_' + design + '.bbmodel'),
      JSON.stringify(kit.bbmodel('blood_soldier_armor_' + design, pieces, [{ ...ARMOUR, key: design }], [atlas], ARMOUR_ATLAS, 'models/armor'), null, 1));
    console.log('  armour', design + ':', pieces.length, 'pieces, atlas rows', rows);
  }
}
