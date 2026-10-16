// The Bandit Warlord (docs/mobs/bandit-warlord.md), the great bandit camp's boss: a mountain of muscle who fights with
// his fists, drawn twice a man's size in game. Mock-up for Enzo first (16/10/2026: "inspired by Nappa, a mountain of
// muscle who fights with his fists"; our own Murim warlord, not a copy): a shaved, gleaming skull, a thick black
// horseshoe moustache falling past the chin, heavy brows over small hard eyes, a gold ring in one ear; a neck as wide as
// the head; a black iron cuirass trimmed with gold and a bronze bear's face on the breast, over a belly of crimson
// lamellae; great flared pauldrons of crimson lacquer edged with gold; huge bare arms, iron bracers, fists wrapped in
// white cloth; a red sash knotted in front; dark baggy trousers, iron-plated boots.
// On Epic Fight's biped skeleton, like the Bandit Chief: the Blockbench project art/blockbench/bandit_warlord.bbmodel
// carries his model and his moves (pose notation of long-fist.cjs); filmed in Blockbench to docs/mobs/images/warlord-*.
//   idle, walk           at rest he breathes and rolls his shoulders; he walks heavy, swinging his arms;
//   roar                 the entrance: crouched in the hall's door, he rises, beats his fists together and roars, arms flexed;
//   mountain_crusher     a straight right: wound back, a step, the fist driven through;
//   earth_breaker        both fists raised over his head and brought down on the ground (a shockwave in game);
//   bull_charge          the shoulder lowered, a rush of four blocks, the impact;
//   sky_breaker          crouched low, an uppercut that throws the target up.
//   sky_fall             his entrance and the art's ultimate: down out of the sky, a landing fist-first, up into the roar.
//   collapse             Mountain Collapse: the right fist raised high, driven into the ground ahead from one knee (a
//                        fissure runs out from it in game, breaking the ground);
//   leap, leap_land      Thousand-Jin Drop: a crouch and a leap high toward his foe, falling fist first; the landing
//                        (a crater and a shockwave in game).
// In game (16/10/2026, Enzo: "reprends le boss"): the mesh assets/murimblock/animmodels/entity/bandit_warlord.json, its
// atlas textures/entity/bandit_warlord.png, and his moves as Epic Fight animations biped/murim/warlord_* (pose.cjs
// writeSmooth); the fall split in two for the game: warlord_dive (held while he falls, a loop) and warlord_land (from the
// impact: the landing, up, the roar).
// Usage: node scripts/animation/bandit-warlord-3d.cjs [--port=9236] [--film=false]
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const kit = require('./mesh-kit.cjs');
const { isSide, all, rim } = kit;
const { animation, groupsByName } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];

/** S skin, H skin's sheen, A hair, E eyes, W whites, M mouth, I iron, G gold, B bronze, R crimson, D dark trim, C cloth wraps, T trousers, F boots, N sash. */
const WARLORD = { key: 'warlord', c: {
  S: '#b97f50', H: '#dcac7c', A: '#151010', E: '#120c0a', W: '#ece2cf', M: '#6e2a22', I: '#2e3036', G: '#d6a83c',
  B: '#9a6a2a', R: '#9c1f1a', D: '#1a1412', C: '#e4dac6', T: '#2b2530', F: '#1d1816', N: '#b8261e' } };

// The face, 8 x 8, the viewer's view: a gleaming brow, heavy brows, small eyes, a broad nose, the horseshoe moustache
// running down both sides of the mouth to the chin.
const FACE = ['SSHHSSSS', 'SSSSSSSS', 'AAASSAAA', 'SWESSEWS', 'SSSssSSS', 'SAAAAAAS', 'SASMMSAS', 'SAssssAS'];

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
/** Muscle: the skin, a darker crease every few texels, sheen on top. */
const muscle = (f, u, v, w, h) => (f === 'up' ? 'H' : f === 'down' ? 's' : isSide(f) && (u + 2 * v) % 7 === 0 ? 's' : v === 0 ? 'H' : 'S');
/** Lacquer: crimson, gold along the edges, a rivet row. */
const lacquer = (f, u, v, w, h) => (isSide(f) && (v === h - 1 || v === 0) ? 'G' : isSide(f) && v === 1 && u % 3 === 1 ? 'g' : (u + v) % 5 === 0 ? 'r' : 'R');

// ---- head: shaved and gleaming, the moustache, the ear ring
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h) => {
  if (f === 'north') return FACE[v][u];
  if (f === 'up') return (u === 3 || u === 4) && v > 1 && v < 5 ? 'H' : (u + v) % 5 === 0 ? 's' : 'S';
  if (f === 'down') return 'S';
  if (f === 'south') return v > 5 ? 's' : 'S';
  const front = f === 'east' ? w - 1 - u : u;
  if (front === 0 && v >= 5) return 'A';                       // the moustache's ends, seen from the side
  if (front === 3 && (v === 3 || v === 4)) return 's';         // the ear
  return v < 2 ? 'H' : 'S';
});
piece('Head', 'hat', 'moustache_r', [2.2, 22.6, -4.25], [3.4, 25.2, -3.95], (f, u, v) => (v % 3 === 2 ? 'a' : 'A'));
piece('Head', 'hat', 'moustache_l', [-3.4, 22.6, -4.25], [-2.2, 25.2, -3.95], (f, u, v) => (v % 3 === 2 ? 'a' : 'A'));
piece('Head', 'hat', 'brow_ridge', [-3.6, 28.6, -4.4], [3.6, 29.4, -3.8], (f, u) => (f === 'north' && (u === 3 || u === 4) ? 's' : 'A'));
piece('Head', 'hat', 'ear_ring', [-4.6, 25.0, -0.6], [-4.0, 26.4, 0.6], (f, u, v) => ((u + v) % 2 ? 'g' : 'G'));

// ---- neck and trunk: the bull neck, the cuirass with its bear face, the crimson lamellae, the sash
piece('Chest', 'torso', 'traps', [-6.2, 22.8, -2.6], [6.2, 25.8, 3.2], rim(muscle));
piece('Chest', 'torso', 'cuirass', [-6.6, 17.4, -3.8], [6.6, 24, 3.4], (f, u, v, w, h) => {
  if (isSide(f) && (v === 0 || v === h - 1)) return 'G';
  if (f === 'north') {
    const c = (w - 1) / 2, du = Math.abs(u - c);
    // A bronze bear's face on the breast: ears, eyes, snout.
    if (v >= 1 && v <= 5 && du <= 2.5) {
      if (v === 1 && du >= 1.5) return 'B';
      if (v === 2 && du >= 1 && du <= 2) return 'D';
      if (v === 4 && du <= 0.5) return 'D';
      return v === 5 ? 'b' : 'B';
    }
    return (u % 4 === 0) ? 'i' : 'I';
  }
  return (u + v) % 6 === 0 ? 'i' : 'I';
}, { hinge: { y: 17.4, bone: 'Torso' } });
piece('Torso', 'torso', 'lamellae', [-6.0, 12.4, -3.6], [6.0, 17.6, 3.0], (f, u, v, w, h) => {
  if (f === 'down') return 'T';
  return isSide(f) && v % 2 === 1 ? 'r' : isSide(f) && u % 2 === v % 2 ? 'R' : 'r';
}, { hinge: { y: 17.4, bone: 'Chest' } });
piece('Torso', 'jacket', 'sash', [-6.3, 11.6, -3.9], [6.3, 13.8, 3.3], rim((f, u, v, w, h) => (isSide(f) && v === 0 ? 'n' : 'N')));
piece('Torso', 'jacket', 'sash_knot', [-1.4, 8.0, -4.4], [1.4, 12.6, -3.8], (f, u, v, w, h) => (v === h - 1 ? 'n' : v % 2 ? 'n' : 'N'));

// ---- the pauldrons, the arms: bare and huge, iron bracers, the fists wrapped in cloth
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [p0, p1] = xs(4.0, 11.8), [q0, q1] = xs(4.4, 11.4), [a0, a1] = xs(4.4, 10.6), [b0, b1] = xs(4.1, 10.9), [f0, f1] = xs(4.6, 10.4);
  piece('Arm_' + side, part + 'Sleeve', 'pauldron_' + s, [p0, 21.4, -4.2], [p1, 25.8, 4.2], lacquer);
  piece('Arm_' + side, part + 'Sleeve', 'pauldron_lame_' + s, [q0, 19.6, -3.8], [q1, 21.6, 3.8], rim((f, u, v, w, h) => (v === h - 1 ? 'G' : 'r')));
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, 15.4, -3.1], [a1, 22.4, 3.1], muscle, { hinge: { y: 15.4, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'forearm_' + s, [a0, 9.6, -3.1], [a1, 15.4, 3.1], muscle, { hinge: { y: 15.4, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'bracer_' + s, [b0, 10.6, -3.4], [b1, 14.6, 3.4],
    rim((f, u, v, w, h) => (v === 0 || v === h - 1 ? 'G' : isSide(f) && v === 2 && u % 2 ? 'g' : 'I')));
  piece('Hand_' + side, part + 'Arm', 'fist_' + s, [f0, 6.6, -3.3], [f1, 9.8, 3.3],
    (f, u, v, w, h) => (f === 'north' && v === 0 && u % 2 === 1 ? 's' : (u + v) % 3 === 0 ? 'c' : 'C'));
}

// ---- legs: dark baggy trousers over iron-plated boots, a plate on each thigh
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [t0, t1] = xs(0.2, 5.2), [p0, p1] = xs(0.0, 5.5), [g0, g1] = xs(0.1, 5.4);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 5.6, -3.0], [t1, 12.4, 3.0], (f, u) => (isSide(f) && u % 3 === 2 ? 't' : 'T'), { hinge: { y: 5.6, bone: 'Leg_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'tasset_' + s, [p0, 7.6, -3.5], [p1, 12.2, -2.9], (f, u, v) => (v === 0 ? 'G' : v % 2 ? 'i' : 'I'));
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -3.0], [t1, 5.6, 3.0], (f, u, v, w, h) => (v < 2 ? 'T' : 'F'), { hinge: { y: 5.6, bone: 'Thigh_' + side } });
  piece('Leg_' + side, part + 'Pants', 'greave_' + s, [g0, 1.2, -3.4], [g1, 4.6, -2.8], (f, u, v) => (v === 0 ? 'G' : 'I'));
  piece('Leg_' + side, part + 'Pants', 'toe_' + s, [t0, 0, -4.0], [t1, 1.4, 3.0], (f, u, v, w, h) => (f === 'down' ? 'd' : 'F'));
}

// ---------------------------------------------------------------- the moves
// Pose notation (long-fist.cjs): character axes x its right, y its front, z up. Arm x swings the arm forward and up,
// Arm y < 0 takes the right arm out to the side (> 0 the left), Arm x 90 then z -90 holds the right arm out level to the side, Hand x bends the elbow, Thigh x swings the leg forward,
// Leg x < 0 bends the knee, Torso x < 0 leans forward, Torso z turns the trunk (+ to the left).
const HIP = 0.764;

const REST = (y = 0, breath = 0) => ({
  Root: { pos: [0, y, HIP] },
  Torso: [['x', -3 - breath]], Chest: [['x', breath]], Head: [['x', 4]],
  Arm_R: [['x', 6], ['y', -16]], Hand_R: [['x', 22]], Arm_L: [['x', 6], ['y', 16]], Hand_L: [['x', 22]],
  Thigh_R: [['y', -6]], Leg_R: [], Thigh_L: [['y', 6]], Leg_L: [],
});

const idle = [
  { t: 0, pose: REST() },
  { t: 1.0, pose: { ...REST(0, 3), Arm_R: [['x', 8], ['y', -20]], Arm_L: [['x', 8], ['y', 20]], Head: [['x', 2], ['z', 6]] } },
  { t: 2.0, pose: { ...REST(), Head: [['x', 4], ['z', -6]] } },
  { t: 3.0, pose: REST() },
];

/** A heavy walk: long strides, the trunk rolling, the arms swinging against the legs. */
const STEP = (y, k) => ({
  Root: { pos: [0, y, HIP - 0.02 + 0.02 * Math.abs(k)] },
  Torso: [['x', -6], ['z', -10 * k]], Chest: [['z', -4 * k]], Head: [['x', 6], ['z', 12 * k]],
  Arm_R: [['x', -26 * k], ['y', -16]], Hand_R: [['x', 30]], Arm_L: [['x', 26 * k], ['y', 16]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 30 * k], ['y', -6]], Leg_R: [['x', k < 0 ? -30 : -6]], Thigh_L: [['x', -30 * k], ['y', 6]], Leg_L: [['x', k > 0 ? -30 : -6]],
});
const walk = [
  { t: 0, pose: STEP(0, 1) }, { t: 0.3, pose: STEP(0, 0) }, { t: 0.6, pose: STEP(0, -1) }, { t: 0.9, pose: STEP(0, 0) }, { t: 1.2, pose: STEP(0, 1) },
];

/** Crouched low, the fists on the ground. */
const CROUCH = {
  Root: { pos: [0, 0, 0.5] },
  Torso: [['x', -52]], Chest: [['x', -6]], Head: [['x', 48]],
  Arm_R: [['x', 34], ['y', -10]], Hand_R: [['x', 10]], Arm_L: [['x', 34], ['y', 10]], Hand_L: [['x', 10]],
  Thigh_R: [['x', 74], ['y', -14]], Leg_R: [['x', -110]], Thigh_L: [['x', 74], ['y', 14]], Leg_L: [['x', -110]],
};
/** Standing tall, the arms raised and flexed, the head thrown back in a roar. */
const FLEX = {
  Root: { pos: [0, 0, HIP + 0.02] },
  Torso: [['x', 6]], Chest: [['x', 6]], Head: [['x', -24]],
  Arm_R: [['x', 92], ['z', -84]], Hand_R: [['x', 112]], Arm_L: [['x', 92], ['z', 84]], Hand_L: [['x', 112]],
  Thigh_R: [['y', -14]], Leg_R: [], Thigh_L: [['y', 14]], Leg_L: [],
};
const roar = [
  { t: 0, pose: CROUCH },
  { t: 0.5, pose: { ...REST(), Torso: [['x', -10]] } },
  // The fists beaten together before the chest, twice.
  { t: 0.7, pose: { ...REST(), Arm_R: [['x', 70], ['z', 30]], Hand_R: [['x', 60]], Arm_L: [['x', 70], ['z', -30]], Hand_L: [['x', 60]] } },
  { t: 0.85, pose: { ...REST(), Arm_R: [['x', 60], ['z', 6]], Hand_R: [['x', 80]], Arm_L: [['x', 60], ['z', -6]], Hand_L: [['x', 80]] } },
  { t: 1.0, pose: { ...REST(), Arm_R: [['x', 70], ['z', 30]], Hand_R: [['x', 60]], Arm_L: [['x', 70], ['z', -30]], Hand_L: [['x', 60]] } },
  { t: 1.15, pose: { ...REST(), Arm_R: [['x', 60], ['z', 6]], Hand_R: [['x', 80]], Arm_L: [['x', 60], ['z', -6]], Hand_L: [['x', 80]] } },
  { t: 1.45, pose: FLEX },
  { t: 2.2, pose: { ...FLEX, Head: [['x', -30]] } },
  { t: 2.6, pose: REST() },
];

const mountain_crusher = [
  { t: 0, pose: REST() },
  // Wound back: the trunk turned right, the right fist drawn back by the ear.
  { t: 0.3, pose: { ...REST(), Torso: [['x', -6], ['z', -34]], Chest: [['z', -10]], Head: [['z', 40]],
    Arm_R: [['x', -36], ['y', -30]], Hand_R: [['x', 118]], Arm_L: [['x', 60], ['y', 10]], Hand_L: [['x', 40]],
    Thigh_R: [['x', -16], ['y', -8]], Leg_R: [['x', -10]], Thigh_L: [['x', 20], ['y', 8]], Leg_L: [['x', -16]] } },
  // A step and the fist driven through.
  { t: 0.45, pose: { Root: { pos: [0, 0.6, HIP - 0.08] }, Torso: [['x', -22], ['z', 30]], Chest: [['z', 10]], Head: [['x', 14], ['z', -36]],
    Arm_R: [['x', 92], ['y', -4]], Hand_R: [['x', 0]], Arm_L: [['x', -30], ['y', 20]], Hand_L: [['x', 100]],
    Thigh_R: [['x', -30], ['y', -6]], Leg_R: [['x', -10]], Thigh_L: [['x', 54], ['y', 8]], Leg_L: [['x', -50]] } },
  { t: 0.7, pose: { Root: { pos: [0, 0.6, HIP - 0.08] }, Torso: [['x', -18], ['z', 26]], Chest: [['z', 8]], Head: [['x', 12], ['z', -32]],
    Arm_R: [['x', 88], ['y', -6]], Hand_R: [['x', 6]], Arm_L: [['x', -26], ['y', 20]], Hand_L: [['x', 100]],
    Thigh_R: [['x', -30], ['y', -6]], Leg_R: [['x', -10]], Thigh_L: [['x', 54], ['y', 8]], Leg_L: [['x', -50]] } },
  { t: 1.1, pose: REST(0.6) },
];

const earth_breaker = [
  { t: 0, pose: REST() },
  // Both fists raised high over his head, joined, leaning back on his toes.
  { t: 0.5, pose: { Root: { pos: [0, 0, HIP + 0.04] }, Torso: [['x', 12]], Chest: [['x', 6]], Head: [['x', -18]],
    Arm_R: [['x', 168], ['z', 18]], Hand_R: [['x', 36]], Arm_L: [['x', 168], ['z', -18]], Hand_L: [['x', 36]],
    Thigh_R: [['y', -10]], Leg_R: [], Thigh_L: [['y', 10]], Leg_L: [] } },
  { t: 0.6, pose: { Root: { pos: [0, 0, HIP + 0.04] }, Torso: [['x', 14]], Chest: [['x', 8]], Head: [['x', -20]],
    Arm_R: [['x', 175], ['z', 18]], Hand_R: [['x', 40]], Arm_L: [['x', 175], ['z', -18]], Hand_L: [['x', 40]],
    Thigh_R: [['y', -10]], Leg_R: [], Thigh_L: [['y', 10]], Leg_L: [] } },
  // Brought down on the ground before him, the knees giving.
  { t: 0.78, pose: { Root: { pos: [0, 0.3, 0.5] }, Torso: [['x', -58]], Chest: [['x', -8]], Head: [['x', 46]],
    Arm_R: [['x', 50], ['z', 14]], Hand_R: [['x', 4]], Arm_L: [['x', 50], ['z', -14]], Hand_L: [['x', 4]],
    Thigh_R: [['x', 80], ['y', -16]], Leg_R: [['x', -104]], Thigh_L: [['x', 20], ['y', 16]], Leg_L: [['x', -90]] } },
  { t: 1.2, pose: { Root: { pos: [0, 0.3, 0.5] }, Torso: [['x', -56]], Chest: [['x', -8]], Head: [['x', 44]],
    Arm_R: [['x', 48], ['z', 14]], Hand_R: [['x', 4]], Arm_L: [['x', 48], ['z', -14]], Hand_L: [['x', 4]],
    Thigh_R: [['x', 80], ['y', -16]], Leg_R: [['x', -104]], Thigh_L: [['x', 20], ['y', 16]], Leg_L: [['x', -90]] } },
  { t: 1.7, pose: REST(0.3) },
];

/** The rush: the right shoulder lowered, the arms tucked, running. */
const RUSH = (y, k) => ({
  Root: { pos: [0, y, HIP - 0.06] },
  Torso: [['x', -34], ['z', 26]], Chest: [['x', -4]], Head: [['x', 30], ['z', -26]],
  Arm_R: [['x', 26], ['y', -20]], Hand_R: [['x', 118]], Arm_L: [['x', -24 + 20 * k], ['y', 16]], Hand_L: [['x', 70]],
  Thigh_R: [['x', 46 * k]], Leg_R: [['x', k < 0 ? -70 : -20]], Thigh_L: [['x', -46 * k]], Leg_L: [['x', k > 0 ? -70 : -20]],
});
const bull_charge = [
  { t: 0, pose: REST() },
  { t: 0.35, pose: { ...RUSH(0, 0), Root: { pos: [0, -0.2, 0.6] }, Thigh_R: [['x', 40]], Leg_R: [['x', -90]], Thigh_L: [['x', -20]], Leg_L: [['x', -40]] } },
  { t: 0.5, pose: RUSH(0.8, 1) }, { t: 0.65, pose: RUSH(1.8, -1) }, { t: 0.8, pose: RUSH(2.8, 1) }, { t: 0.95, pose: RUSH(3.8, -1) },
  // The impact: the shoulder driven in, the whole body stopped.
  { t: 1.05, pose: { ...RUSH(4.2, 0), Torso: [['x', -20], ['z', 40]], Head: [['x', 16], ['z', -40]] } },
  { t: 1.6, pose: REST(4.2) },
];

const sky_breaker = [
  { t: 0, pose: REST() },
  // Crouched low, the right fist down by the knee.
  { t: 0.35, pose: { Root: { pos: [0, 0.2, 0.56] }, Torso: [['x', -30], ['z', -22]], Chest: [['x', -6]], Head: [['x', 30], ['z', 20]],
    Arm_R: [['x', -18], ['y', -12]], Hand_R: [['x', 70]], Arm_L: [['x', 50], ['y', 12]], Hand_L: [['x', 70]],
    Thigh_R: [['x', 30], ['y', -10]], Leg_R: [['x', -90]], Thigh_L: [['x', 70], ['y', 10]], Leg_L: [['x', -100]] } },
  // Up through the target, rising on his toes.
  { t: 0.5, pose: { Root: { pos: [0, 0.5, HIP + 0.08] }, Torso: [['x', 10], ['z', 20]], Chest: [['x', 4]], Head: [['x', -20], ['z', -16]],
    Arm_R: [['x', 172], ['y', -6]], Hand_R: [['x', 24]], Arm_L: [['x', -10], ['y', 20]], Hand_L: [['x', 80]],
    Thigh_R: [['x', -10], ['y', -6]], Leg_R: [], Thigh_L: [['x', 20], ['y', 6]], Leg_L: [['x', -10]] } },
  { t: 0.75, pose: { Root: { pos: [0, 0.5, HIP + 0.06] }, Torso: [['x', 8], ['z', 18]], Chest: [['x', 4]], Head: [['x', -22], ['z', -16]],
    Arm_R: [['x', 168], ['y', -6]], Hand_R: [['x', 28]], Arm_L: [['x', -10], ['y', 20]], Hand_L: [['x', 80]],
    Thigh_R: [['x', -10], ['y', -6]], Leg_R: [], Thigh_L: [['x', 20], ['y', 6]], Leg_L: [['x', -10]] } },
  { t: 1.15, pose: REST(0.5) },
];

/** Coming down: the knees drawn up, the right fist cocked under him, the left arm thrown up. */
const DIVE = z => ({
  Root: { pos: [0, 0, z] },
  Torso: [['x', -20]], Chest: [['x', -6]], Head: [['x', 30]],
  Arm_R: [['x', -30], ['y', -14]], Hand_R: [['x', 110]], Arm_L: [['x', 150], ['z', -30]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 70], ['y', -14]], Leg_R: [['x', -100]], Thigh_L: [['x', 40], ['y', 14]], Leg_L: [['x', -110]],
});
/** The landing: the left knee down, the right foot planted, the right fist driven into the ground, the left arm out. */
const LANDED = (lean = 0) => ({
  Root: { pos: [0, 0, 0.42] },
  Torso: [['x', -44 + lean]], Chest: [['x', -8]], Head: [['x', 40 - lean]],
  Arm_R: [['x', 50], ['y', -6]], Hand_R: [['x', 0]], Arm_L: [['x', 20], ['y', 60]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 92], ['y', -12]], Leg_R: [['x', -96]], Thigh_L: [['x', -6], ['y', 12]], Leg_L: [['x', -104]],
});
const sky_fall = [
  { t: 0, pose: DIVE(4.2) },
  { t: 0.3, pose: DIVE(2.2) },
  { t: 0.45, pose: DIVE(0.9) },
  // The impact (the crater opens here in game).
  { t: 0.52, pose: LANDED(-6) },
  { t: 0.6, pose: LANDED(2) },
  { t: 1.3, pose: LANDED(0) },
  // Up out of the dust, slowly, then the roar.
  { t: 1.8, pose: { ...REST(), Torso: [['x', -14]], Head: [['x', 10]] } },
  { t: 2.2, pose: FLEX },
  { t: 2.9, pose: { ...FLEX, Head: [['x', -30]] } },
  { t: 3.3, pose: REST() },
];

/** Mountain Collapse: up on his toes, the right fist raised high behind his head; then down on one knee, the fist driven into the ground ahead. */
const collapse = [
  { t: 0, pose: REST() },
  { t: 0.45, pose: { Root: { pos: [0, 0, HIP + 0.04] }, Torso: [['x', 14], ['z', -16]], Chest: [['x', 6]], Head: [['x', -16], ['z', 14]],
    Arm_R: [['x', 176], ['y', -10]], Hand_R: [['x', 50]], Arm_L: [['x', 40], ['y', 30]], Hand_L: [['x', 60]],
    Thigh_R: [['y', -10]], Leg_R: [], Thigh_L: [['x', 20], ['y', 10]], Leg_L: [['x', -10]] } },
  { t: 0.55, pose: { Root: { pos: [0, 0, HIP + 0.05] }, Torso: [['x', 16], ['z', -18]], Chest: [['x', 8]], Head: [['x', -18], ['z', 14]],
    Arm_R: [['x', 182], ['y', -10]], Hand_R: [['x', 56]], Arm_L: [['x', 40], ['y', 30]], Hand_L: [['x', 60]],
    Thigh_R: [['y', -10]], Leg_R: [], Thigh_L: [['x', 20], ['y', 10]], Leg_L: [['x', -10]] } },
  { t: 0.7, pose: LANDED(-8) },
  { t: 0.8, pose: LANDED(2) },
  { t: 1.3, pose: LANDED(0) },
  { t: 1.8, pose: REST() },
];
/** Thousand-Jin Drop: a deep crouch, then up and over in the dive (the entity itself leaps: the root stays at the hips). */
const leap = [
  { t: 0, pose: REST() },
  { t: 0.3, pose: { ...CROUCH, Arm_R: [['x', -40], ['y', -16]], Hand_R: [['x', 40]], Arm_L: [['x', -40], ['y', 16]], Hand_L: [['x', 40]] } },
  { t: 0.45, pose: { ...DIVE(HIP), Arm_R: [['x', 150], ['z', 30]], Hand_R: [['x', 30]] } },
  { t: 0.9, pose: DIVE(HIP) },
  { t: 2.4, pose: DIVE(HIP) },
];
/** The landing of the drop: down fist first, the knee giving, then up. */
const leap_land = [
  { t: 0, pose: LANDED(-8) },
  { t: 0.12, pose: LANDED(2) },
  { t: 0.6, pose: LANDED(0) },
  { t: 1.1, pose: REST() },
];

const ANIMS = { idle, walk, roar, mountain_crusher, earth_breaker, bull_charge, sky_breaker, sky_fall, collapse, leap, leap_land };

/** In game: held while he falls (the root at the hips: the entity itself falls), and from the impact on. */
const dive = [{ t: 0, pose: { ...DIVE(HIP), Arm_L: [['x', 160], ['z', -30]] } }, { t: 0.4, pose: { ...DIVE(HIP), Arm_L: [['x', 140], ['z', -40]] } },
  { t: 0.8, pose: { ...DIVE(HIP), Arm_L: [['x', 160], ['z', -30]] } }];
const land = sky_fall.filter(k => k.t >= 0.52).map(k => ({ t: Math.round((k.t - 0.52) * 1000) / 1000, pose: k.pose }));
/** The game's animations: name in game, keys. */
const GAME = { warlord_idle: idle, warlord_walk: walk, warlord_roar: roar, warlord_crusher: mountain_crusher,
  warlord_earth_breaker: earth_breaker, warlord_bull_charge: bull_charge, warlord_sky_breaker: sky_breaker,
  warlord_dive: dive, warlord_land: land, warlord_collapse: collapse, warlord_leap: leap, warlord_leap_land: leap_land };
const LOOPS = { idle: 'loop', walk: 'loop' };

// ---------------------------------------------------------------- the project
function buildProject() {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, WARLORD, ATLAS));
  const proj = kit.bbmodel('bandit_warlord', PIECES, [WARLORD], [atlas], ATLAS, 'entity');
  const groups = groupsByName(proj.outliner);
  proj.animations = Object.entries(ANIMS).map(([n, keys]) => animation(n, keys, groups, LOOPS[n] || 'once'));
  return { proj, atlas };
}

// ---------------------------------------------------------------- filming
async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let n = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.error || (m.result && m.result.exceptionDetails)) p.reject(new Error(JSON.stringify(m.error || m.result.exceptionDetails).slice(0, 800))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const k = ++n; pending.set(k, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: k, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}
const load = (bb, proj) => bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
  setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(proj))}), 'p.bbmodel'); Canvas.updateAll();
  for (let i = 0; i < 60 && Outliner.elements.length < ${proj.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
  await new Promise(r => setTimeout(r, 400)); return Outliner.elements.length; })()`);
const shot = (bb, pos, target, w, h, t = null) => bb.evaluate(`(async () => {
  ${t === null ? '' : `Timeline.setTime(${t}); Animator.preview();`}
  const p = window.__warlordFilm || (window.__warlordFilm = new Preview({ id: 'warlord_film', offscreen: true, antialias: true }));
  p.resize(${w}, ${h});
  p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 36 });
  p.camera.lookAt(...${JSON.stringify(target)});
  const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#EDE4CF');
  return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
})()`);
const save = (file, data) => fs.writeFileSync(file, Buffer.from(data.split(',')[1], 'base64'));

/** Stills: front three-quarter, front, back three-quarter, the face; and his size beside a Mountain Bandit. */
const STILLS = {
  three: [[-46, 30, -50], [0, 17, 0], 640, 720], front: [[0, 20, -66], [0, 17, 0], 640, 720],
  back: [[44, 30, 52], [0, 17, 0], 640, 720], face: [[-8, 30, -22], [0, 27, 0], 640, 560],
};
const VIEWS = { three: [[-60, 34, -66], [0, 15, -14]], side: [[-92, 22, -14], [0, 15, -14]] };
const VIEW_OF = { bull_charge: { side: [[-150, 28, -32], [0, 15, -32]] }, sky_fall: { three: [[-96, 36, -108], [0, 34, 0]] } };
const FILMS = { idle: ['three'], walk: ['three'], roar: ['three'], mountain_crusher: ['three', 'side'], earth_breaker: ['three', 'side'],
  bull_charge: ['side'], sky_breaker: ['three', 'side'], sky_fall: ['three'], collapse: ['three', 'side'], leap: ['three'], leap_land: ['three'] };
const FPS = 20;

/** His size: the warlord twice as big, a Mountain Bandit (the crimson outfit) at a man's size beside him. */
function sizeProject(proj, atlas) {
  const bandit = require('./bandit-3d.cjs');
  const outfit = bandit.OUTFITS.find(o => o.key === 'crimson') || bandit.OUTFITS[0];
  const bPieces = bandit.PIECES.filter(p => !p.wear || p.wear(outfit)).map(p => ({ ...p }));
  const faces = kit.pack(bPieces, 128);
  const bAtlas = kit.png(128, 128, kit.paintAtlas(faces, outfit, 128));
  const bproj = kit.bbmodel("bandit", bPieces, [outfit], [bAtlas], 128, "entity");
  const big = proj.elements.map(e => ({ ...e, from: e.from.map((v, k) => v * 2 + (k === 0 ? 12 : 0)), to: e.to.map((v, k) => v * 2 + (k === 0 ? 12 : 0)),
    origin: e.origin.map((v, k) => v * 2 + (k === 0 ? 12 : 0)) }));
  const small = bproj.elements.map(e => ({ ...e, from: e.from.map((v, k) => v + (k === 0 ? -14 : 0)), to: e.to.map((v, k) => v + (k === 0 ? -14 : 0)),
    origin: e.origin.map((v, k) => v + (k === 0 ? -14 : 0)),
    faces: Object.fromEntries(Object.entries(e.faces).map(([f, d]) => [f, { ...d, texture: 1, uv: d.uv }])) }));
  const tex2 = { ...bproj.textures[0], id: '1', uv_width: 128, uv_height: 128 };
  return { ...proj, name: 'warlord_size', elements: [...big, ...small], outliner: [...big, ...small].map(e => e.uuid), animations: [], textures: [proj.textures[0], tex2] };
}

async function film(proj, atlas) {
  const bb = await connect(Number(arg('port') || 9236));
  const out = path.join(ROOT, 'docs', 'mobs', 'images');
  fs.mkdirSync(out, { recursive: true });
  await load(bb, { ...proj, animations: [] });
  for (const [view, [pos, target, w, h]] of Object.entries(STILLS)) save(path.join(out, `warlord-${view}.png`), await shot(bb, pos, target, w, h));
  const size = sizeProject(proj, atlas);
  await load(bb, size);
  save(path.join(out, 'warlord-size.png'), await shot(bb, [-30, 46, -150], [0, 32, 0], 960, 760));
  await load(bb, proj);
  for (const [name, views] of Object.entries(FILMS)) for (const view of views) {
    const dir = path.join(ROOT, 'build', 'warlord-animations', name, view);
    fs.rmSync(dir, { recursive: true, force: true });
    fs.mkdirSync(dir, { recursive: true });
    const length = ANIMS[name][ANIMS[name].length - 1].t, frames = Math.round(length * FPS) + (LOOPS[name] ? 0 : 6);
    await bb.evaluate(`(() => { Animation.all.find(a => a.name === ${JSON.stringify(name)}).select(); return true; })()`);
    for (let f = 0; f < frames; f++) save(path.join(dir, `frame-${String(f).padStart(3, '0')}.png`), await shot(bb, ((VIEW_OF[name] || {})[view] || VIEWS[view])[0], ((VIEW_OF[name] || {})[view] || VIEWS[view])[1], 640, 560, Math.min(f / FPS, length)));
    const gif = path.join(out, `warlord-${name}${view === 'three' ? '' : '-' + view}.gif`);
    execFileSync('java', [path.join(__dirname, 'FramesToGif.java'), dir, gif, String(1000 / FPS), '640'], { stdio: 'ignore' });
    console.log(name, view, frames, 'frames ->', path.relative(ROOT, gif));
  }
  bb.close();
}

if (require.main === module) {
  const { proj, atlas } = buildProject();
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'bandit_warlord'), { recursive: true });
  fs.writeFileSync(path.join(art, 'bandit_warlord', 'warlord.png'), atlas);
  fs.writeFileSync(path.join(art, 'bandit_warlord.bbmodel'), JSON.stringify(proj, null, 1));
  console.log('wrote the Bandit Warlord:', PIECES.length, 'pieces,', proj.animations.length, 'animations');
  // The game's files.
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'entity', 'bandit_warlord.png'), atlas);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'bandit_warlord.json'), JSON.stringify(kit.mesh(PIECES, ATLAS)));
  for (const [name, keys] of Object.entries(GAME)) writeSmooth(name, keys);
  if (arg('only')) { const only = arg('only').split(','); for (const k of Object.keys(FILMS)) if (!only.includes(k)) delete FILMS[k]; }
  if (arg('film') !== 'false') film(proj, atlas).catch(e => { console.error(e); process.exit(1); });
}

module.exports = { PIECES, WARLORD, ANIMS, REST, LANDED, DIVE, CROUCH, FLEX, HIP };
