// The Bandit Chief's attacks with his great axe, two-handed and heavy, slow to come so they can be read and parried:
//   chief_cleave  the axe lifted upright over the head, brought down to the ground in front, drawn out to the right;
//   chief_sweep   wound far back on the right, then swept level across the front to the left;
//   chief_slam    a crouch, the axe low behind, a leap forward with the axe swung up on the right, and the axe driven
//                 into the ground where he lands (the shockwave is the server's, BanditChiefEntity).
// The game files (animmodels/animations/biped/murim/chief_*.json) are written here, every joint turning the shortest way
// from one key to the next (quaternions), so a wrist solved two equivalent ways never spins between keys. The key poses
// also go into a Blockbench project to look at (art/blockbench/bandit_chief_axe.bbmodel); Blockbench turns its angles one
// by one between keys, so its in-betweens may differ from the game's.
// Each key pose says where the right hand holds the axe, which way the axe points and which way its edge faces (the
// edge leads every cut, as the dao's: azure-m1.cjs); both arms and the wrist are solved for it, the left hand a fist
// behind the right on the haft (ik.cjs, point `haft`). The trunk and legs are posed by hand.
// The axe is long (its butt 0.70 behind the right hand) and two hands together reach only the middle of the body: the
// hold gives a little wherever a key would put the axe or the hands through the trunk, head or legs, keys are added
// wherever the joints' own path between two keys would, and what remains is reported (check).
// Usage: node scripts/animation/chief-axe.cjs
// Axes: the character's (x right, y front, z up), in blocks; the root's position +y forward.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { measure, model, inBody, onLine } = require('./ik.cjs');
const { writeSmooth, sample, arm, anglesOf } = require('./pose.cjs');
const { modelSpace, origin } = require('./biped.cjs');

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
/** Turned by [x, y, z] degrees about the x, then y, then z axis. */
const turned = (v, [ax, ay, az]) => {
  const r = (a, i, j) => { const c = Math.cos(a * Math.PI / 180), n = Math.sin(a * Math.PI / 180), w = [...v]; w[i] = v[i] * c - v[j] * n; w[j] = v[i] * n + v[j] * c; v = w; };
  r(ax, 1, 2); r(ay, 2, 0); r(az, 0, 1);
  return v;
};

/**
 * The great axe along the rig's sword line, in blocks from the right hand (chief-axe-model.cjs: the model's y, scaled
 * 1.3, the hand 0.13 + 8.5/16 short of its middle, as Epic Fight holds an item): the butt's spike 0.70 behind the hand,
 * the grip from 0.35 behind to 0.29 ahead, the point 1.98 ahead. None of it may pass through the body, nor the hands
 * holding it. The haft is about 0.1 thick.
 */
const BUTT = -0.70, POINT = 1.98, HAFT = 0.06;
/**
 * How far in the axe may go (its middle line, the haft's thickness counted): resting against the body, a little into the
 * tunic, is all a two-handed grip allows in a chop (two hands together reach only the middle of the body, so the long
 * butt comes back against the belly); passing through, or out of the back, is not.
 */
const TOUCH = 2 * HAFT;
/** How far a hand reaches from its shoulder (the rig's 0.57, a little short of the arm locked straight). */
const REACH = 0.55;
const ALONG = [];
for (let s = BUTT; s <= POINT + 1e-9; s += 0.1) ALONG.push(s);
/** How deep the axe goes into the body in a posed model (0: clear), where, and its squares summed along it. */
function through(m) {
  const depth = inBody(m, HAFT);
  let worst = { depth: 0, part: null, at: 0, sum: 0 };
  for (const s of ALONG) {
    const r = depth(onLine(m, s));
    if (r.depth > 0) worst.sum += r.depth ** 2;
    if (r.depth > worst.depth) worst = { ...r, at: s, sum: worst.sum };
  }
  return worst;
}

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

function hold(body, hand, dir, edge, guess = {}, { twoHands = true, clear = true } = {}) {
  const pose = JSON.parse(JSON.stringify({
    Arm_R: [['x', 60], ['y', 0], ['z', 10]], Hand_R: [['x', 40]], Tool_R: [['x', -30], ['y', 0], ['z', 0]],
    Arm_L: [['x', 60], ['y', 20], ['z', -20]], Hand_L: [['x', 40]], ...guess, ...body,
  }));
  const from = previous;
  const root = pose.Root.pos;
  const d2 = (a, b) => a.reduce((s, x, i) => s + (x - b[i]) ** 2, 0);
  // Where the axe is held gives a little, if it must, to keep the axe out of the body and within both hands' reach: the
  // hand moved up to 0.3 block (in hundredths), the axe turned up to 35 degrees. The arms play no part in that (the
  // body's blocks are the trunk, head and legs), so the hold is settled first, then the arms solved to it.
  const give = [0, 0, 0, 0, 0, 0];
  const line = () => {
    const d = norm(turned(norm(dir), give.slice(3))), e = square(edge, d);
    const H = add(add(root, hand), give.slice(0, 3).map(v => v / 100)), M = add(H, d, LINE / 2);
    return { d, H, T: add(H, d, LINE), E: add(M, e, 0.1) };
  };
  if (clear) {
    const posed = model(pose), depth = inBody(posed, HAFT);
    const shoulderR = posed.Arm_R.filter((_, i) => i % 4 === 3).slice(0, 3), shoulderL = posed.Arm_L.filter((_, i) => i % 4 === 3).slice(0, 3);
    const beyond = (p, s) => Math.max(0, Math.sqrt(d2(p, s)) - REACH) ** 2;
    const holdCost = () => {
      const { d, H } = line();
      let into = 0;
      for (const s of ALONG) into += Math.max(0, depth(add(H, d, s)).depth) ** 2;
      const moved = 3 * give.slice(0, 3).reduce((c, v) => c + (v / 100) ** 2, 0) + 1.5e-4 * give.slice(3).reduce((c, v) => c + v * v, 0);
      return 60 * into + 400 * (beyond(H, shoulderR) + (twoHands ? beyond(add(H, d, -0.25), shoulderL) : 0)) + moved;
    };
    let low = holdCost(), step = 8;
    while (step > 0.1) {
      let improved = false;
      for (let i = 0; i < 6; i++) for (const s of [step, -step]) {
        const old = give[i], limit = i < 3 ? 30 : 35;
        give[i] = Math.max(-limit, Math.min(limit, old + s));
        const c = holdCost();
        if (c < low - 1e-9) { low = c; improved = true; } else give[i] = old;
      }
      if (!improved) step *= 0.6;
    }
  }
  const { H, T, E } = line();
  const vars = [['Arm_R', 0, -60, 200], ['Arm_R', 1, -70, 70], ['Arm_R', 2, -150, 150], ['Hand_R', 0, 0, 150],
    ['Tool_R', 0, -180, 180], ['Tool_R', 1, -180, 180], ['Tool_R', 2, -180, 180],
    ...(twoHands ? [['Arm_L', 0, -60, 200], ['Arm_L', 1, -70, 70], ['Arm_L', 2, -150, 150], ['Hand_L', 0, 0, 150]] : [])];
  const get = ([j, i]) => pose[j][i][1], set = ([j, i], v) => { pose[j][i][1] = v; };
  // A light pull toward the pose's natural guess (per degree squared): a raised arm stays a raised arm, not some
  // twisted equivalent, so the motion between keys reads right.
  const natural = vars.map(get);
  const pull = () => vars.reduce((c, v, n) => c + 3e-6 * (get(v) - natural[n]) ** 2, 0);
  const cost = () => {
    const m = measure(pose);
    return 4 * d2(m.hilt, H) + d2(m.tip, T) + 2 * d2(m.edge, E) + (twoHands ? 3 * d2(m.handL, m.haft) : 0) + pull();
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
  const posed = model(pose), m = measure(pose, posed), into = through(posed);
  const miss = twoHands ? Math.sqrt(d2(m.handL, m.haft)) : 0, off = Math.sqrt(d2(m.hilt, H));
  if (miss > 0.05 || off > 0.08) console.warn(`  pose off (hand ${hand}, axe ${dir.map(x => x.toFixed(2))}): right hand ${off.toFixed(2)}, left hand ${miss.toFixed(2)} from the haft`);
  if (into.depth > TOUCH) console.warn(`  axe through the body (hand ${hand}, axe ${dir.map(x => x.toFixed(2))}): ${into.depth.toFixed(2)} into ${into.part}, ${into.at.toFixed(1)} from the hand`);
  if (process.env.AXE_GIVE && give.some(v => Math.abs(v) > 1)) console.log(`  gave (hand ${hand}): moved ${give.slice(0, 3).map(v => Math.round(v))} /100, turned ${give.slice(3).map(v => Math.round(v))} deg`);
  return pose;
}

const LEGS_WIDE = { Thigh_R: [['x', 18], ['y', -10]], Leg_R: [['x', -14]], Thigh_L: [['x', -16], ['y', 10]], Leg_L: [['x', -12]] };

/** Ready: feet apart, the axe slanting up across the body, the head high on the right, the edge out front. */
const GUARD = (y = 0) => hold(
  { Root: { pos: [0, y, HIP - 0.06] }, Torso: [['z', -10]], Chest: [['x', -6], ['z', -4]], ...look(-14), ...LEGS_WIDE },
  [0.06, 0.36, 0.3], [0.45, 0.4, 0.8], [0.2, 1, 0], { Arm_R: [['x', 40], ['y', -10], ['z', 20]], Hand_R: [['x', 50]], Tool_R: [['x', -10], ['y', 0], ['z', 0]], Arm_L: [['x', 50], ['y', 20], ['z', -10]], Hand_L: [['x', 50]] });

/**
 * Lifted: the hands come up before the face, the axe upright, its edge out front. Two hands together reach no higher
 * than the brow (a short-armed body): the axe stays upright until they are there, so it never sweeps through the head.
 */
const LIFT = (y = 0) => hold(
  { Root: { pos: [0, y, HIP - 0.02] }, Torso: [['z', -4]], Chest: [['x', -2]], ...look(-6, -6), ...LEGS_WIDE },
  [0.05, 0.42, 0.7], [0.15, 0.1, 0.98], [0, 1, 0], { Arm_R: [['x', 120], ['y', 0], ['z', 10]], Hand_R: [['x', 40]], Tool_R: [['x', -30], ['y', 0], ['z', 0]], Arm_L: [['x', 110], ['y', 10], ['z', -10]], Hand_L: [['x', 40]] });

/** Raised: both hands up at the brow, the axe upright above the head and tipped back, its edge out front for the chop. */
const RAISED = (y = 0, z = 0.02, legs = { Thigh_R: [['x', -14]], Leg_R: [['x', -10]], Thigh_L: [['x', 16]], Leg_L: [['x', -8]] }) => hold(
  { Root: { pos: [0, y, HIP + z] }, Torso: [['x', 4]], Chest: [['x', 4]], ...look(0, -12), ...legs },
  [0.05, 0.38, 0.9], [0.1, -0.25, 0.96], [0, 1, 0], { Arm_R: [['x', 150], ['y', 0], ['z', 10]], Hand_R: [['x', 25]], Tool_R: [['x', -45], ['y', 0], ['z', 0]], Arm_L: [['x', 145], ['y', 10], ['z', -10]], Hand_L: [['x', 30]] });

/** The chop has landed: both hands low in front, the axe's head at the ground ahead, the edge down. */
const CHOP = (y = 0, z = -0.24, legs = { Thigh_R: [['x', 64]], Leg_R: [['x', -78]], Thigh_L: [['x', -24]], Leg_L: [['x', -46]] }) => hold(
  { Root: { pos: [0, y, HIP + z] }, Torso: [['x', -26]], Chest: [['x', -16]], ...look(0, 22), ...legs },
  [0.02, 0.5, 0.18], [0, 0.72, -0.7], [0, 0, -1], { Arm_R: [['x', 70], ['y', 0], ['z', 6]], Hand_R: [['x', 6]], Tool_R: [['x', -70], ['y', 0], ['z', 0]], Arm_L: [['x', 70], ['y', 10], ['z', -10]], Hand_L: [['x', 20]] });

/**
 * Drawn back out of the ground: the axe's head swung out to the right and up, the butt across in front of the left hip,
 * on the way back to the guard (the head rising straight up through the front would drive the butt through the belly).
 */
const RECOVER = (y = 0, z = -0.12) => hold(
  { Root: { pos: [0, y, HIP + z] }, Torso: [['x', -10], ['z', -8]], Chest: [['x', -6]], ...look(-8, 6), ...LEGS_WIDE },
  [0.12, 0.38, 0.3], [0.9, 0.3, 0.3], [0, 1, 0.2], { Arm_R: [['x', 50], ['y', -10], ['z', 30]], Hand_R: [['x', 40]], Tool_R: [['x', -20], ['y', 0], ['z', 0]], Arm_L: [['x', 55], ['y', 20], ['z', -20]], Hand_L: [['x', 40]] });

/**
 * A level sweep, the hands turned `yaw` (+ left), the axe pointing out level `ahead` degrees further round, the edge
 * toward the sweep (to its left). Pointing straight out from the hands, the long butt would run back into the trunk: so
 * wound up the axe trails behind the hands, and through the front and after it runs ahead of them, the butt past the side.
 */
const SWEEP = (yaw, trunk, { y = 0, lead = 'R', ahead = 0 } = {}) => hold(
  { Root: { pos: [0, y, HIP - 0.1] }, Torso: [['z', trunk * 0.6]], Chest: [['x', -12], ['z', trunk * 0.4]], ...look(trunk),
    ...(lead === 'R'
      ? { Thigh_R: [['x', 46], ['y', -8]], Leg_R: [['x', -44]], Thigh_L: [['x', -26], ['y', 8]], Leg_L: [['x', -12]] }
      : { Thigh_R: [['x', -18], ['y', -8]], Leg_R: [['x', -14]], Thigh_L: [['x', 42], ['y', 8]], Leg_L: [['x', -38]] }) },
  yawed([0.02, 0.4, 0.34], yaw), yawed([0, 1, -0.05], yaw + ahead), yawed([-1, 0, 0], yaw + ahead),
  { Arm_R: [['x', 80], ['y', 0], ['z', yaw - trunk]], Hand_R: [['x', 14]], Tool_R: [['x', -88], ['y', 90], ['z', 0]],
    Arm_L: [['x', 70], ['y', 10], ['z', (yaw - trunk) * 0.8]], Hand_L: [['x', 30]] });

begin();
const chief_cleave = [
  { t: 0, pose: GUARD() },
  // Slow to come: the axe lifted upright, then up over the head.
  { t: 0.2, pose: LIFT() },
  { t: 0.38, pose: RAISED() },
  { t: 0.46, pose: RAISED(0.05, 0.04) },
  // Down to the ground ahead with a step.
  { t: 0.56, pose: CHOP(0.3) },
  { t: 0.7, pose: CHOP(0.3, -0.26) },
  // Drawn out to the right, back to the guard.
  { t: 0.92, pose: RECOVER(0.3) },
  { t: 1.15, pose: GUARD(0.3) },
];

begin();
const chief_sweep = [
  { t: 0, pose: GUARD() },
  // Wound up far back on the right, the trunk turned away.
  { t: 0.34, pose: SWEEP(-95, -50, { ahead: -45 }) },
  { t: 0.42, pose: SWEEP(-98, -54, { ahead: -45 }) },
  // Through the front with a step, carried round to the left.
  { t: 0.5, pose: SWEEP(12, 10, { y: 0.25, ahead: 28 }) },
  { t: 0.6, pose: SWEEP(100, 66, { y: 0.3, lead: 'L', ahead: 35 }) },
  { t: 0.74, pose: SWEEP(108, 70, { y: 0.3, lead: 'L', ahead: 35 }) },
  { t: 1.05, pose: GUARD(0.3) },
];

begin();
const SLAM_GUARD = GUARD();
/**
 * Crouched to leap, the axe held low behind on the right, pointing back and down, its edge up: it swings up past the
 * right shoulder into the leap, the butt out in front.
 */
const CROUCH = hold(
  { Root: { pos: [0, 0, HIP - 0.28] }, Torso: [['x', -10]], Chest: [['x', 4]], ...look(0, -6),
    Thigh_R: [['x', 50]], Leg_R: [['x', -90]], Thigh_L: [['x', 40]], Leg_L: [['x', -80]] },
  [0.22, 0.15, 0.3], [0.3, -0.75, -0.6], [0, 0, 1], { Arm_R: [['x', 30], ['y', -10], ['z', 30]], Hand_R: [['x', 30]], Tool_R: [['x', -50], ['y', 0], ['z', 0]], Arm_L: [['x', 60], ['y', 10], ['z', -30]], Hand_L: [['x', 40]] });

const LANDED = { Thigh_R: [['x', 70]], Leg_R: [['x', -95]], Thigh_L: [['x', -10]], Leg_L: [['x', -60]] };
const chief_slam = [
  { t: 0, pose: SLAM_GUARD },
  { t: 0.3, pose: CROUCH },
  // In the air, the axe overhead, knees drawn up.
  { t: 0.55, pose: RAISED(0.9, 0.85, { Thigh_R: [['x', 40]], Leg_R: [['x', -60]], Thigh_L: [['x', 30]], Leg_L: [['x', -50]] }) },
  // Landed: the axe driven into the ground ahead.
  { t: 0.75, pose: CHOP(1.8, -0.3, LANDED) },
  { t: 0.95, pose: CHOP(1.8, -0.32, LANDED) },
  // Drawn out to the right, back to the guard.
  { t: 1.22, pose: RECOVER(1.8) },
  { t: 1.5, pose: GUARD(1.8) },
];

/** The arms' joints of a pose. */
const armsOf = pose => Object.fromEntries(ARMS.filter(j => pose[j]).map(j => [j, JSON.parse(JSON.stringify(pose[j]))]));
/** A frame (local matrices) back into a pose: the root's position, and every posed joint's angles. */
const ONE_AXIS = ['Hand_R', 'Hand_L', 'Leg_R', 'Leg_L'];
const poseOf = L => Object.fromEntries([['Root', { pos: origin(L.Root) }],
  ...['Torso', 'Chest', 'Head', 'Thigh_R', 'Leg_R', 'Thigh_L', 'Leg_L', ...ARMS].map(j => [j, anglesOf(j, L[j], ONE_AXIS.includes(j) ? 1 : 3)])]);
/**
 * Keys added where the joints, each turning the shortest way, carry the axe through the body between two keys: at the
 * worst frame, the pose as it was, the axe held out of the body (hold gives). A few rounds, until every stretch is clear.
 */
function refine(keys, limit = TOUCH) {
  for (let round = 0; round < 6; round++) {
    const { times, frames } = sample(keys), out = [keys[0]];
    let added = false;
    for (let i = 0; i + 1 < keys.length; i++) {
      const a = keys[i], c = keys[i + 1];
      let worst = limit, at = -1;
      times.forEach((t, n) => {
        if (t <= a.t + 1e-6 || t >= c.t - 1e-6) return;
        const r = through(modelSpace(arm, frames[n]));
        if (r.depth > worst) { worst = r.depth; at = n; }
      });
      if (at >= 0) {
        const t = times[at], m = modelSpace(arm, frames[at]), p = measure(null, m), frame = poseOf(frames[at]);
        const body = Object.fromEntries(Object.entries(frame).filter(([j]) => !ARMS.includes(j)));
        previous = armsOf(a.pose);
        const pose = hold(body, add(origin(m.Tool_R), body.Root.pos, -1), add(p.tip, p.hilt, -1), add(p.edge, p.mid, -1), armsOf(frame));
        out.push({ t, pose });
        added = true;
      }
      out.push(c);
    }
    keys = out;
    if (!added) break;
  }
  return keys;
}

const anims = { chief_cleave: refine(chief_cleave), chief_sweep: refine(chief_sweep), chief_slam: refine(chief_slam) };

/** Warns of every stretch, between the keys too, where the axe goes into the body further than it may (the game's frames). */
function check(name, keys) {
  const { times, frames } = sample(keys);
  let from = null, worst = { depth: 0 };
  const flush = t => { if (from !== null) console.warn(`  ${name}: axe through the ${worst.part} from ${from}s to ${t}s (${worst.depth.toFixed(2)} deep, ${worst.at.toFixed(1)} from the hand)`); from = null; worst = { depth: 0 }; };
  times.forEach((t, n) => {
    const r = through(modelSpace(arm, frames[n]));
    if (r.depth > TOUCH) { if (from === null) from = t; if (r.depth > worst.depth) worst = r; } else flush(t);
  });
  flush(times[times.length - 1]);
}
module.exports = { anims, hold, begin, check, HIP };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) { check(name, keys); writeSmooth(name, keys); }
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'bandit_chief_axe.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('bandit_chief_axe', anims), null, 1));
  console.log('wrote', out);
}
