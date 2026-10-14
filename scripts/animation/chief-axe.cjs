// The Bandit Chief's attacks with his great axe, two-handed and heavy, slow to come so they can be read and parried:
//   chief_cleave  the axe swung up behind the head, then brought down to the ground in front;
//   chief_sweep   wound far back on the right, then swept level across the front to the left;
//   chief_slam    a crouch, a leap forward, and the axe driven into the ground where he lands (the shockwave is the
//                 server's, BanditChiefEntity).
// The game files (animmodels/animations/biped/murim/chief_*.json) are written here, every joint turning the shortest way
// from one key to the next (quaternions), so a wrist solved two equivalent ways never spins between keys. The key poses
// also go into a Blockbench project to look at (art/blockbench/bandit_chief_axe.bbmodel); Blockbench turns its angles one
// by one between keys, so its in-betweens may differ from the game's.
// Each key pose says where the right hand holds the axe, which way the axe points and which way its edge faces (the
// edge leads every cut, as the dao's: azure-m1.cjs); both arms and the wrist are solved for it, the left hand a fist
// behind the right on the haft (ik.cjs, point `haft`). The trunk and legs are posed by hand.
// Usage: node scripts/animation/chief-axe.cjs
// Axes: the character's (x right, y front, z up), in blocks; the root's position +y forward.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { measure } = require('./ik.cjs');
const { arm, locals, writeFrames, MURIM_BIPED } = require('./pose.cjs');

const HIP = 0.764;
/** The sword line the rig measures, hilt to point (ik.cjs): the axe points along it. */
const LINE = 1.04;

const norm = v => { const l = Math.hypot(...v); return v.map(x => x / l); };
const add = (a, b, k = 1) => a.map((x, i) => x + b[i] * k);
const dot = (a, b) => a.reduce((s, x, i) => s + x * b[i], 0);
/** `want` with its part along `d` taken away: a direction square to the axe. */
const square = (want, d) => norm(add(want, d, -dot(want, d)));
/** Turned `deg` about the vertical (+ to the left). */
const yawed = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x * Math.cos(a) - y * Math.sin(a), x * Math.sin(a) + y * Math.cos(a), z]; };

/** Head kept looking ahead while the trunk turns by `yaw`. */
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });

/**
 * A pose holding the axe: `body` gives the root, trunk, head and legs; `hand` is where the right hand holds (relative to
 * the root), `dir` where the axe points, `edge` where its edge faces. Returns the body with both arms and the wrist solved.
 */
/** The arms and wrist of the key solved last: the next key turns the same way round, so the motion between stays short. */
let previous = null;
/** Starts a new attack: its first key is solved from scratch. */
const begin = () => { previous = null; };
const ARMS = ['Arm_R', 'Hand_R', 'Tool_R', 'Arm_L', 'Hand_L'];

function hold(body, hand, dir, edge, guess = {}) {
  const pose = JSON.parse(JSON.stringify({
    Arm_R: [['x', 60], ['y', 0], ['z', 10]], Hand_R: [['x', 40]], Tool_R: [['x', -30], ['y', 0], ['z', 0]],
    Arm_L: [['x', 60], ['y', 20], ['z', -20]], Hand_L: [['x', 40]], ...guess, ...body,
  }));
  const from = previous;
  const root = pose.Root.pos;
  const d = norm(dir), e = square(edge, d);
  const H = add(root, hand), T = add(H, d, LINE), M = add(H, d, LINE / 2), E = add(M, e, 0.1);
  const vars = [['Arm_R', 0, -60, 200], ['Arm_R', 1, -70, 70], ['Arm_R', 2, -150, 150], ['Hand_R', 0, 0, 150],
    ['Tool_R', 0, -180, 180], ['Tool_R', 1, -180, 180], ['Tool_R', 2, -180, 180],
    ['Arm_L', 0, -60, 200], ['Arm_L', 1, -70, 70], ['Arm_L', 2, -150, 150], ['Hand_L', 0, 0, 150]];
  const get = ([j, i]) => pose[j][i][1], set = ([j, i], v) => { pose[j][i][1] = v; };
  const d2 = (a, b) => a.reduce((s, x, i) => s + (x - b[i]) ** 2, 0);
  // A light pull toward the pose's natural guess (per degree squared): a raised arm stays a raised arm, not some
  // twisted equivalent, so the motion between keys reads right.
  const natural = vars.map(get);
  const pull = () => vars.reduce((c, v, n) => c + 3e-6 * (get(v) - natural[n]) ** 2, 0);
  const cost = () => {
    const m = measure(pose);
    return 4 * d2(m.hilt, H) + d2(m.tip, T) + 2 * d2(m.edge, E) + 3 * d2(m.handL, m.haft) + pull();
  };
  let best = cost(), step = 24;
  for (let it = 0; it < 3000 && step > 0.1; it++) {
    let improved = false;
    for (const v of vars) for (const s of [step, -step]) {
      const old = get(v), nv = Math.max(v[2], Math.min(v[3], old + s));
      set(v, nv); const c = cost();
      if (c < best - 1e-9) { best = c; improved = true; } else set(v, old);
    }
    if (!improved) step *= 0.6;
  }
  // The same turn written the way nearest the key before: an x, y, z turn is also (x + 180, 180 - y, z + 180), and any
  // angle is also itself 360 degrees on. Between keys the angles go straight from one to the other: no spin.
  const near = (a, b) => { while (a - b > 180) a -= 360; while (b - a > 180) a += 360; return a; };
  for (const j of ARMS) {
    const xyz = pose[j];
    if (from && xyz.length === 3) {
      const was = from[j].map(r => r[1]), [a, b, c] = xyz.map(r => r[1]);
      const options = [[a, b, c], [a + 180, 180 - b, c + 180]].map(o => o.map((v, i) => near(v, was[i])));
      const dist = o => o.reduce((d, v, i) => d + (v - was[i]) ** 2, 0);
      const best = dist(options[0]) <= dist(options[1]) ? options[0] : options[1];
      xyz.forEach((r, i) => { r[1] = best[i]; });
    } else if (from) xyz.forEach((r, i) => { r[1] = near(r[1], from[j][i][1]); });
    xyz.forEach(r => { r[1] = Math.round(r[1]); });
  }
  previous = Object.fromEntries(ARMS.map(j => [j, JSON.parse(JSON.stringify(pose[j]))]));
  const m = measure(pose);
  const miss = Math.sqrt(d2(m.handL, m.haft)), off = Math.sqrt(d2(m.hilt, H));
  if (miss > 0.05 || off > 0.08) console.warn(`  pose off (hand ${hand}, axe ${dir.map(x => x.toFixed(2))}): right hand ${off.toFixed(2)}, left hand ${miss.toFixed(2)} from the haft`);
  return pose;
}

const LEGS_WIDE = { Thigh_R: [['x', 18], ['y', -10]], Leg_R: [['x', -14]], Thigh_L: [['x', -16], ['y', 10]], Leg_L: [['x', -12]] };

/** Ready: feet apart, the axe slanting up across the body, the head high on the right, the edge out front. */
const GUARD = (y = 0) => hold(
  { Root: { pos: [0, y, HIP - 0.06] }, Torso: [['z', -10]], Chest: [['x', -6], ['z', -4]], ...look(-14), ...LEGS_WIDE },
  [0.06, 0.36, 0.3], [0.45, 0.4, 0.8], [0.2, 1, 0], { Arm_R: [['x', 40], ['y', -10], ['z', 20]], Hand_R: [['x', 50]], Tool_R: [['x', -10], ['y', 0], ['z', 0]], Arm_L: [['x', 50], ['y', 20], ['z', -10]], Hand_L: [['x', 50]] });

/** Raised: both hands above the head, the axe lying back behind it (a long haft: its head stays high), its edge to the sky. */
const RAISED = (y = 0, z = 0.02, legs = { Thigh_R: [['x', -14]], Leg_R: [['x', -10]], Thigh_L: [['x', 16]], Leg_L: [['x', -8]] }) => hold(
  { Root: { pos: [0, y, HIP + z] }, Torso: [['x', 10]], Chest: [['x', 8]], ...look(0, -12), ...legs },
  [0.04, 0.02, 0.8], [0.05, -0.88, -0.42], [0, 0.4, 1], { Arm_R: [['x', 165], ['y', 0], ['z', 10]], Hand_R: [['x', 25]], Tool_R: [['x', -45], ['y', 0], ['z', 0]], Arm_L: [['x', 160], ['y', 10], ['z', -10]], Hand_L: [['x', 30]] });

/** The chop has landed: both hands low in front, the axe's head at the ground ahead, the edge down. */
const CHOP = (y = 0, z = -0.24, legs = { Thigh_R: [['x', 64]], Leg_R: [['x', -78]], Thigh_L: [['x', -24]], Leg_L: [['x', -46]] }) => hold(
  { Root: { pos: [0, y, HIP + z] }, Torso: [['x', -26]], Chest: [['x', -16]], ...look(0, 22), ...legs },
  [0.02, 0.5, 0.18], [0, 0.72, -0.7], [0, 0, -1], { Arm_R: [['x', 70], ['y', 0], ['z', 6]], Hand_R: [['x', 6]], Tool_R: [['x', -70], ['y', 0], ['z', 0]], Arm_L: [['x', 70], ['y', 10], ['z', -10]], Hand_L: [['x', 20]] });

/** A level sweep turned `yaw` (+ left): both hands out in front, the axe pointing out level, the edge toward the sweep (to the left). */
const SWEEP = (yaw, trunk, { y = 0, lead = 'R' } = {}) => hold(
  { Root: { pos: [0, y, HIP - 0.1] }, Torso: [['z', trunk * 0.6]], Chest: [['x', -12], ['z', trunk * 0.4]], ...look(trunk),
    ...(lead === 'R'
      ? { Thigh_R: [['x', 46], ['y', -8]], Leg_R: [['x', -44]], Thigh_L: [['x', -26], ['y', 8]], Leg_L: [['x', -12]] }
      : { Thigh_R: [['x', -18], ['y', -8]], Leg_R: [['x', -14]], Thigh_L: [['x', 42], ['y', 8]], Leg_L: [['x', -38]] }) },
  yawed([0.02, 0.4, 0.34], yaw), yawed([0, 1, -0.05], yaw), yawed([-1, 0, 0], yaw),
  { Arm_R: [['x', 80], ['y', 0], ['z', yaw - trunk]], Hand_R: [['x', 14]], Tool_R: [['x', -88], ['y', 90], ['z', 0]],
    Arm_L: [['x', 70], ['y', 10], ['z', (yaw - trunk) * 0.8]], Hand_L: [['x', 30]] });

begin();
const chief_cleave = [
  { t: 0, pose: GUARD() },
  // Slow to come: the axe climbs behind the head.
  { t: 0.38, pose: RAISED() },
  { t: 0.46, pose: RAISED(0.05, 0.04) },
  // Down to the ground ahead with a step.
  { t: 0.56, pose: CHOP(0.3) },
  { t: 0.7, pose: CHOP(0.3, -0.26) },
  { t: 1.15, pose: GUARD(0.3) },
];

begin();
const chief_sweep = [
  { t: 0, pose: GUARD() },
  // Wound up far back on the right, the trunk turned away.
  { t: 0.34, pose: SWEEP(-115, -50) },
  { t: 0.42, pose: SWEEP(-120, -52) },
  // Through the front with a step, carried round to the left.
  { t: 0.5, pose: SWEEP(-5, 0, { y: 0.25 }) },
  { t: 0.6, pose: SWEEP(100, 66, { y: 0.3, lead: 'L' }) },
  { t: 0.74, pose: SWEEP(108, 70, { y: 0.3, lead: 'L' }) },
  { t: 1.05, pose: GUARD(0.3) },
];

begin();
const SLAM_GUARD = GUARD();
/** Crouched to leap, the axe loaded back over the right shoulder, its edge up. */
const CROUCH = hold(
  { Root: { pos: [0, 0, HIP - 0.28] }, Torso: [['x', -10]], Chest: [['x', 4]], ...look(0, -6),
    Thigh_R: [['x', 50]], Leg_R: [['x', -90]], Thigh_L: [['x', 40]], Leg_L: [['x', -80]] },
  [0.18, 0.05, 0.62], [0.25, -0.6, -0.75], [0, 0, 1], { Arm_R: [['x', 150], ['y', 0], ['z', 20]], Hand_R: [['x', 30]], Tool_R: [['x', -50], ['y', 0], ['z', 0]], Arm_L: [['x', 150], ['y', 10], ['z', -10]], Hand_L: [['x', 40]] });

const LANDED = { Thigh_R: [['x', 70]], Leg_R: [['x', -95]], Thigh_L: [['x', -10]], Leg_L: [['x', -60]] };
const chief_slam = [
  { t: 0, pose: SLAM_GUARD },
  { t: 0.3, pose: CROUCH },
  // In the air, the axe overhead, knees drawn up.
  { t: 0.55, pose: RAISED(0.9, 0.85, { Thigh_R: [['x', 40]], Leg_R: [['x', -60]], Thigh_L: [['x', 30]], Leg_L: [['x', -50]] }) },
  // Landed: the axe driven into the ground ahead.
  { t: 0.75, pose: CHOP(1.8, -0.3, LANDED) },
  { t: 0.95, pose: CHOP(1.8, -0.32, LANDED) },
  { t: 1.5, pose: GUARD(1.8) },
];

const anims = { chief_cleave, chief_sweep, chief_slam };
module.exports = { anims };

// ---------------------------------------------------------------- the game files: shortest turns between keys

/** A rotation (the 3 x 3 part of a row-major 4 x 4) as a quaternion [x, y, z, w]. */
function quat(m) {
  const [a, b, c, , d, e, f, , g, h, i] = m, tr = a + e + i;
  if (tr > 0) { const k = Math.sqrt(tr + 1) * 2; return [(h - f) / k, (c - g) / k, (d - b) / k, k / 4]; }
  if (a > e && a > i) { const k = Math.sqrt(1 + a - e - i) * 2; return [k / 4, (b + d) / k, (c + g) / k, (h - f) / k]; }
  if (e > i) { const k = Math.sqrt(1 + e - a - i) * 2; return [(b + d) / k, k / 4, (f + h) / k, (c - g) / k]; }
  const k = Math.sqrt(1 + i - a - e) * 2; return [(c + g) / k, (f + h) / k, k / 4, (d - b) / k];
}
function slerp(p, q, t) {
  let d = p.reduce((s, v, n) => s + v * q[n], 0);
  if (d < 0) { q = q.map(v => -v); d = -d; }
  if (d > 0.9995) { const r = p.map((v, n) => v + (q[n] - v) * t), l = Math.hypot(...r); return r.map(v => v / l); }
  const th = Math.acos(d), sa = Math.sin((1 - t) * th) / Math.sin(th), sb = Math.sin(t * th) / Math.sin(th);
  return p.map((v, n) => v * sa + q[n] * sb);
}
function matrix([x, y, z, w], [tx, ty, tz]) {
  return [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), tx,
    2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), ty,
    2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), tz, 0, 0, 0, 1];
}
const STEP = 1 / 40;
function writeSmooth(name, keys) {
  const L = keys.map(k => locals(k.pose)), end = keys[keys.length - 1].t, times = [];
  for (let t = 0; t < end - 1e-9; t += STEP) times.push(Math.round(t * 1000) / 1000);
  // Every key on time, exactly.
  for (const k of keys) if (!times.includes(k.t)) times.push(k.t);
  times.sort((x, y) => x - y);
  const frames = times.map(t => {
    let i = 0;
    while (i < keys.length - 2 && t > keys[i + 1].t) i++;
    const f = Math.min(1, Math.max(0, (t - keys[i].t) / (keys[i + 1].t - keys[i].t)));
    const out = {};
    for (const j of arm.order) {
      const a = L[i][j], b = L[i + 1][j];
      out[j] = matrix(slerp(quat(a), quat(b), f), [3, 7, 11].map(n => a[n] + (b[n] - a[n]) * f));
    }
    return out;
  });
  writeFrames(MURIM_BIPED, name, times, frames);
  console.log('wrote', name, times.length, 'frames,', end, 's');
}

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'bandit_chief_axe.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('bandit_chief_axe', anims), null, 1));
  console.log('wrote', out);
}
