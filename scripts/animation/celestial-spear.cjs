// The Celestial Peak Spear's techniques (docs/combat/styles/celestial-peak-spear.md), the spear held in both hands:
//   celestial_piercing_dragon        a lunge of 4 blocks, the spear driven level through all in its line;
//   celestial_piercing_dragon_long   the same, 6 blocks (evolved: Soaring Dragon);
//   celestial_piercing_dive          from the air: a dive point first, pinning the target to the ground;
//   celestial_plum_blossom           five quick thrusts fanned out like a plum flower's petals, then a sixth that pushes;
//   celestial_plum_rain              eight thrusts, then the sixth (evolved: Plum Rain);
//   celestial_plum_air               from the air: three thrusts down, then the shaft brought down on the target;
//   celestial_thousand_sweep         a whole turn crouched, the spear level at arm's length at the knees, knocking down;
//   celestial_ten_thousand_sweep     two turns (evolved: Sweep of Ten Thousand);
//   celestial_sweep_landing          from the air: the spear planted as the body lands, a wave round it.
// As the Bandit Chief's axe (chief-axe.cjs): each key pose says where the right hand holds the spear and which way the
// spear points; both arms and the wrist are solved for it, the left hand on the shaft ahead of the right (the leading
// hand of a spear, the right one pushing from behind). The trunk and legs are posed by hand. Written straight into the
// game files (animmodels/animations/biped/murim/celestial_*.json), every joint turning the shortest way between keys.
// An Epic Fight spear is held tilted 20 degrees up from the rig's sword line (its hand display: 35 degrees, a sword's 55):
// the spear's line here is that tilted line (SPEAR_TILT to try another).
// Usage: node scripts/animation/celestial-spear.cjs
// Axes: the character's (x right, y front, z up), in blocks; the root's position +y forward.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { measure, model, inBody } = require('./ik.cjs');
const { writeSmooth, sample, arm, bind } = require('./pose.cjs');
const b = require('./biped.cjs');

const HIP = 0.764;
const norm = v => { const l = Math.hypot(...v); return v.map(x => x / l); };
const add = (a, c, k = 1) => a.map((x, i) => x + c[i] * k);
const dot = (a, c) => a.reduce((s, x, i) => s + x * c[i], 0);
const square = (want, d) => norm(add(want, d, -dot(want, d)));
/** Turned `deg` about the vertical (+ to the left). */
const yawed = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x * Math.cos(a) - y * Math.sin(a), x * Math.sin(a) + y * Math.cos(a), z]; };
/** Tipped `deg` up (+) about the character's x. */
const pitched = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x, y * Math.cos(a) - z * Math.sin(a), y * Math.sin(a) + z * Math.cos(a)]; };

// ---------------------------------------------------------------- the spear in the hand
const TILT = Number(process.env.SPEAR_TILT ?? 20);
const bindPose = b.modelSpace(arm, require('./pose.cjs').locals({}));
const HILT0 = b.origin(bindPose.Tool_R);
/** The spear's line in the bind pose: the rig's sword line (hilt to point), tipped up by the spear's tilt. */
const SWORD0 = norm(add([0.37, 1.005, 0.73], HILT0, -1));
const LINE0 = norm(pitched(SWORD0, TILT));
/** Its flat (the leaf blade's face), square to the line, as the sword's edge turned with it. */
const FLAT0 = norm(pitched([0, 0, -1], TILT));
const apply = (m, p) => [0, 1, 2].map(r => m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]);
const carried = (m, p) => apply(b.mul(m.Tool_R, b.invRigid(bindPose.Tool_R)), p);
/** A point of the spear `s` blocks along it from the right hand (behind it when negative). */
const along = (m, s) => carried(m, add(HILT0, LINE0, s));
/** Where the left hand holds: on the shaft ahead of the right hand. */
const FRONT = 0.48;
/** The spear in the hand: its butt 1.0 behind, its point 1.6 ahead, the shaft about 0.08 thick. */
const BUTT = -1.0, POINT = 1.6, SHAFT = 0.05;
const ALONG = []; for (let s = BUTT; s <= POINT + 1e-9; s += 0.1) ALONG.push(s);
const REACH = 0.58;
function through(m) {
  const depth = inBody(m, SHAFT);
  let worst = { depth: 0, part: null, at: 0 };
  for (const s of ALONG) { const r = depth(along(m, s)); if (r.depth > worst.depth) worst = { ...r, at: s }; }
  return worst;
}

const ARMS = ['Arm_R', 'Hand_R', 'Tool_R', 'Arm_L', 'Hand_L'];
let previous = null;
const begin = () => { previous = null; };
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });

/**
 * A pose holding the spear: `body` gives the root, trunk, head and legs; `hand` is where the right hand holds (from the
 * root), `dir` where the spear points, `flat` roughly where its blade's face looks. Both arms and the wrist are solved.
 * The hold gives a little (the hand up to 0.25, the spear turned up to 25 degrees) if the spear would pass through the
 * body or out of the left hand's reach.
 */
function hold(body, hand, dir, flat = [1, 0, 0], guess = {}, front = FRONT) {
  const pose = JSON.parse(JSON.stringify({
    Arm_R: [['x', 40], ['y', 0], ['z', 20]], Hand_R: [['x', 60]], Tool_R: [['x', -10], ['y', 0], ['z', 0]],
    Arm_L: [['x', 70], ['y', 10], ['z', -20]], Hand_L: [['x', 30]], ...guess, ...body,
  }));
  const from = previous, root = pose.Root.pos;
  const d2 = (a, c) => a.reduce((s, x, i) => s + (x - c[i]) ** 2, 0);
  const turned = (v, [ax, ay, az]) => {
    const r = (a, i, j) => { const c = Math.cos(a * Math.PI / 180), n = Math.sin(a * Math.PI / 180), w = [...v]; w[i] = v[i] * c - v[j] * n; w[j] = v[i] * n + v[j] * c; v = w; };
    r(ax, 1, 2); r(ay, 2, 0); r(az, 0, 1); return v;
  };
  const give = [0, 0, 0, 0, 0, 0];
  const line = () => {
    const d = norm(turned(norm(dir), give.slice(3))), f = square(flat, d);
    const H = add(add(root, hand), give.slice(0, 3).map(v => v / 100));
    return { d, f, H };
  };
  const posed0 = model(pose), depth0 = inBody(posed0, SHAFT);
  const shoulder = (j) => posed0[j].filter((_, i) => i % 4 === 3).slice(0, 3);
  const sR = shoulder('Arm_R'), sL = shoulder('Arm_L');
  const beyond = (p, s) => Math.max(0, Math.sqrt(d2(p, s)) - REACH) ** 2;
  const holdCost = () => {
    const { d, H } = line();
    let into = 0;
    for (const s of ALONG) into += Math.max(0, depth0(add(H, d, s)).depth) ** 2;
    const moved = 3 * give.slice(0, 3).reduce((c, v) => c + (v / 100) ** 2, 0) + 1.5e-4 * give.slice(3).reduce((c, v) => c + v * v, 0);
    return 60 * into + 400 * (beyond(H, sR) + beyond(add(H, d, front), sL)) + moved;
  };
  let low = holdCost(), step = 8;
  while (step > 0.1) {
    let improved = false;
    for (let i = 0; i < 6; i++) for (const s of [step, -step]) {
      const old = give[i], limit = i < 3 ? 25 : 25;
      give[i] = Math.max(-limit, Math.min(limit, old + s));
      const c = holdCost();
      if (c < low - 1e-9) { low = c; improved = true; } else give[i] = old;
    }
    if (!improved) step *= 0.6;
  }
  const { d, f, H } = line();
  const T = add(H, d, 1), F = add(add(H, d, 0.5), f, 0.1), L = add(H, d, front);
  const vars = [['Arm_R', 0, -60, 200], ['Arm_R', 1, -70, 70], ['Arm_R', 2, -150, 150], ['Hand_R', 0, 0, 150],
    ['Tool_R', 0, -180, 180], ['Tool_R', 1, -180, 180], ['Tool_R', 2, -180, 180],
    ['Arm_L', 0, -60, 200], ['Arm_L', 1, -70, 70], ['Arm_L', 2, -150, 150], ['Hand_L', 0, 0, 150]];
  const get = ([j, i]) => pose[j][i][1], set = ([j, i], v) => { pose[j][i][1] = v; };
  const natural = vars.map(get);
  const pull = () => vars.reduce((c, v, n) => c + 3e-6 * (get(v) - natural[n]) ** 2, 0);
  const cost = () => {
    const m = model(pose), at = measure(pose, m);
    const hilt = at.hilt, tip = along(m, 1), face = carried(m, add(add(HILT0, LINE0, 0.5), FLAT0, 0.1));
    return 4 * d2(hilt, H) + d2(tip, T) + 0.5 * d2(face, F) + 3 * d2(at.handL, L) + pull();
  };
  let best = cost(), st = 24;
  for (let it = 0; it < 3000 && st > 0.1; it++) {
    let improved = false;
    for (const v of vars) for (const s of [st, -st]) {
      const old = get(v), nv = Math.max(v[2], Math.min(v[3], old + s));
      set(v, nv); const c = cost();
      if (c < best - 1e-9) { best = c; improved = true; } else set(v, old);
    }
    if (!improved) st *= 0.6;
  }
  const near = (a, c) => { while (a - c > 180) a -= 360; while (c - a > 180) a += 360; return a; };
  for (const j of ARMS) {
    const xyz = pose[j];
    if (from && xyz.length === 3) {
      const was = from[j].map(r => r[1]), [a, bb, c] = xyz.map(r => r[1]);
      const options = [[a, bb, c], [a + 180, 180 - bb, c + 180]].map(o => o.map((v, i) => near(v, was[i])));
      const dist = o => o.reduce((s, v, i) => s + (v - was[i]) ** 2, 0);
      const pick = dist(options[0]) <= dist(options[1]) ? options[0] : options[1];
      xyz.forEach((r, i) => { r[1] = pick[i]; });
    } else if (from) xyz.forEach((r, i) => { r[1] = near(r[1], from[j][i][1]); });
    xyz.forEach(r => { r[1] = Math.round(r[1]); });
  }
  previous = Object.fromEntries(ARMS.map(j => [j, JSON.parse(JSON.stringify(pose[j]))]));
  const m = model(pose), at = measure(pose, m), into = through(m);
  const missL = Math.sqrt(d2(at.handL, along(m, front))), off = Math.sqrt(d2(at.hilt, H));
  if (missL > 0.08 || off > 0.08) console.warn(`  pose off (hand ${hand.map(x => x.toFixed(2))}): right hand ${off.toFixed(2)}, left hand ${missL.toFixed(2)} from the shaft`);
  if (into.depth > 2 * SHAFT) console.warn(`  spear through the body (hand ${hand.map(x => x.toFixed(2))}): ${into.depth.toFixed(2)} into ${into.part}, ${into.at.toFixed(1)} along`);
  return pose;
}

// ---------------------------------------------------------------- stances
const STANCE = { Thigh_L: [['x', 24], ['y', 6]], Leg_L: [['x', -20]], Thigh_R: [['x', -18], ['y', -8]], Leg_R: [['x', -16]] };
const LUNGE = { Thigh_L: [['x', 62], ['y', 6]], Leg_L: [['x', -58]], Thigh_R: [['x', -34], ['y', -6]], Leg_R: [['x', -6]] };
const CROUCH = { Thigh_L: [['x', 64], ['y', 14]], Leg_L: [['x', -96]], Thigh_R: [['x', 30], ['y', -20]], Leg_R: [['x', -84]] };
const TUCK = { Thigh_L: [['x', 70]], Leg_L: [['x', -90]], Thigh_R: [['x', 50]], Leg_R: [['x', -80]] };

/** On guard: left foot forward, the spear low and level from the right hip, the point at the enemy's chest. */
const GUARD = (y = 0, turn = 22) => hold({ Root: { pos: [0, y, HIP - 0.05] }, Torso: [['x', -4], ['z', turn]], Chest: [['z', turn * 0.3]], ...look(turn), ...STANCE },
  [0.24, 0.02, 0.2], yawed([0, 1, 0.12], 8));
/** Drawn back to strike: the spear pulled in along its line, the weight on the back foot. */
const WIND = (y = 0, turn = 34) => hold({ Root: { pos: [0, y, HIP - 0.1] }, Torso: [['x', 2], ['z', turn]], Chest: [['z', turn * 0.3]], ...look(turn), ...STANCE },
  [0.3, -0.24, 0.2], yawed([0, 1, 0.08], 10));
/** The thrust driven home: the right hand pushed out ahead, the body behind the shaft, the front knee bent. */
const THRUST = (y = 0, yaw = 0, pitch = 0, z = -0.14, legs = LUNGE) => hold({ Root: { pos: [0, y, HIP + z] }, Torso: [['x', -10], ['z', -8 + yaw * 0.4]], Chest: [['x', -4], ['z', yaw * 0.3]], ...look(-8 + yaw * 0.6, -pitch * 0.4), ...legs },
  yawed([0.12, 0.58, 0.3 + pitch * 0.006], yaw), yawed(pitched([0, 1, 0], pitch), yaw));
/** Pulled back between quick thrusts: the hand at the hip again, the spear aimed where the next goes. */
const DRAW = (y = 0, yaw = 0, pitch = 0) => hold({ Root: { pos: [0, y, HIP - 0.1] }, Torso: [['x', -6], ['z', 14 + yaw * 0.3]], Chest: [['z', yaw * 0.2]], ...look(14 + yaw * 0.5), ...STANCE },
  yawed([0.24, 0.06, 0.22], yaw * 0.5), yawed(pitched([0, 1, 0], pitch), yaw));

// ---------------------------------------------------------------- 1. Piercing Dragon
function piercing(distance) {
  begin();
  return [
    { t: 0, pose: GUARD() },
    { t: 0.12, pose: WIND(-0.1) },
    // The lunge: the body flies along the spear, the point first.
    { t: 0.2, pose: THRUST(distance * 0.4, 0, 2, -0.1) },
    { t: 0.32, pose: THRUST(distance * 0.88, 0, 0, -0.16) },
    { t: 0.42, pose: THRUST(distance, 0, 0, -0.18) },
    // The spear drawn back, the body rising into guard.
    { t: 0.58, pose: DRAW(distance) },
    { t: 0.78, pose: GUARD(distance) },
  ];
}
const celestial_piercing_dragon = piercing(4);
const celestial_piercing_dragon_long = piercing(6);

/** From the air: the spear lifted over the head pointing down ahead, then the dive, the point driven into the ground. */
begin();
const DIVE_DROP = 1.6;
const AIR_RAISE = (y, z) => hold({ Root: { pos: [0, y, HIP + z] }, Torso: [['x', 6]], Chest: [['x', 4]], ...look(0, 20), ...TUCK },
  [0.12, 0.18, 0.62], pitched([0, 1, 0], -55));
const celestial_piercing_dive = [
  { t: 0, pose: AIR_RAISE(0, 0.05) },
  { t: 0.14, pose: AIR_RAISE(0.2, 0.02) },
  { t: 0.3, pose: hold({ Root: { pos: [0, 1.2, HIP - DIVE_DROP] }, Torso: [['x', -22]], Chest: [['x', -10]], ...look(0, 26), ...CROUCH }, [0.08, 0.42, 0.26], pitched([0, 1, 0], -58)) },
  { t: 0.5, pose: hold({ Root: { pos: [0, 1.3, HIP - DIVE_DROP - 0.06] }, Torso: [['x', -24]], Chest: [['x', -10]], ...look(0, 26), ...CROUCH }, [0.08, 0.46, 0.22], pitched([0, 1, 0], -60)) },
  { t: 0.85, pose: GUARD(1.3) },
].map(k => k.t === 0.85 ? { ...k, pose: { ...k.pose, Root: { pos: [0, 1.3, HIP - 0.05 - DIVE_DROP] } } } : k);

// ---------------------------------------------------------------- 2. Plum Blossom Thrusts
/** Where each quick thrust goes: turned and tipped a little differently, the petals of a plum flower. */
const PETALS = [[-20, 8], [14, -6], [-6, 16], [22, 2], [-12, -10], [6, 10], [-24, -2], [18, 12]];
const PLUM_GAP = 0.1, RAIN_GAP = 0.085, PLUM_LAST = 0.2;
function plum(thrusts, gap) {
  begin();
  const keys = [{ t: 0, pose: GUARD() }];
  for (let i = 0; i < thrusts; i++) {
    const [yaw, pitch] = PETALS[i % PETALS.length], t = gap * (i + 1);
    keys.push({ t: Math.round((t - gap * 0.45) * 1000) / 1000, pose: DRAW(0, yaw, pitch) });
    keys.push({ t: Math.round(t * 1000) / 1000, pose: THRUST(0.05, yaw, pitch, -0.12, STANCE) });
  }
  const last = Math.round((gap * thrusts + PLUM_LAST) * 1000) / 1000;
  keys.push({ t: Math.round((last - 0.1) * 1000) / 1000, pose: WIND(0) });
  keys.push({ t: last, pose: THRUST(0.45, 0, 0, -0.16) });
  keys.push({ t: Math.round((last + 0.12) * 1000) / 1000, pose: THRUST(0.5, 0, 0, -0.18) });
  keys.push({ t: Math.round((last + 0.36) * 1000) / 1000, pose: GUARD(0.5) });
  return keys;
}
const celestial_plum_blossom = plum(5, PLUM_GAP);
const celestial_plum_rain = plum(8, RAIN_GAP);

/** From the air: three thrusts down, then the shaft brought down on the target. */
begin();
const DOWN = (z, pitch, yaw = 0) => hold({ Root: { pos: [0, 0.1, HIP + z] }, Torso: [['x', -12]], Chest: [['x', -6]], ...look(0, 24), ...TUCK },
  yawed([0.1, 0.42, 0.42], yaw), yawed(pitched([0, 1, 0], pitch), yaw));
const celestial_plum_air = [
  { t: 0, pose: AIR_RAISE(0, 0.05) },
  { t: 0.06, pose: DOWN(0, -35, -12) }, { t: 0.1, pose: DOWN(-0.05, -50, -12) },
  { t: 0.16, pose: DOWN(-0.08, -35, 12) }, { t: 0.2, pose: DOWN(-0.12, -50, 12) },
  { t: 0.26, pose: DOWN(-0.15, -35, 0) }, { t: 0.3, pose: DOWN(-0.2, -55, 0) },
  // The shaft raised high, then brought down level on the target.
  { t: 0.4, pose: hold({ Root: { pos: [0, 0.2, HIP - 0.2] }, Torso: [['x', 10]], Chest: [['x', 6]], ...look(0, -10), ...TUCK }, [0.12, 0.2, 0.78], pitched([0, 1, 0], 30)) },
  { t: 0.5, pose: hold({ Root: { pos: [0, 0.4, HIP - 0.5] }, Torso: [['x', -26]], Chest: [['x', -12]], ...look(0, 26), ...CROUCH }, [0.1, 0.5, 0.2], pitched([0, 1, 0], -12)) },
  { t: 0.62, pose: hold({ Root: { pos: [0, 0.45, HIP - 0.55] }, Torso: [['x', -26]], Chest: [['x', -12]], ...look(0, 26), ...CROUCH }, [0.1, 0.5, 0.16], pitched([0, 1, 0], -14)) },
  { t: 0.85, pose: GUARD(0.45) },
].map(k => k.t === 0.85 ? { ...k, pose: { ...k.pose, Root: { pos: [0, 0.45, HIP - 0.55] } } } : k);

// ---------------------------------------------------------------- 3. Sweep of a Thousand Soldiers
/**
 * The sweep: crouched, the body turned `turn` degrees (the root itself, + to the left), the spear held out level at the
 * knees, at arm's length to the right of the body (the hands at its very end for the greatest reach).
 */
const SWEEP = (turn, y = 0) => {
  const pose = hold({ Root: { pos: [0, y, HIP - 0.26] }, Torso: [['x', -14], ['z', -10]], Chest: [['x', -6]], ...look(-10, 10), ...CROUCH },
    [0.42, 0.22, 0.12], yawed(pitched([0, 1, 0], -4), -70), [0, 0, 1], {}, -0.34);
  pose.Root.rot = [['z', turn]];
  return pose;
};
function sweep(turns) {
  begin();
  const keys = [{ t: 0, pose: GUARD() }];
  // Wound up: the spear back over the right shoulder, the trunk turned away.
  const wind = hold({ Root: { pos: [0, 0, HIP - 0.12] }, Torso: [['z', -40]], Chest: [['z', -14]], ...look(-30), ...STANCE },
    [0.32, -0.16, 0.42], yawed(pitched([0, 1, 0], 10), -150));
  keys.push({ t: 0.18, pose: wind });
  const per = 0.32, start = 0.26;
  for (let q = 0; q <= turns * 4; q++) keys.push({ t: Math.round((start + per * q / 4) * 1000) / 1000, pose: SWEEP(q * 90) });
  const end = start + per * turns;
  // The spear planted; the body rises behind it into guard.
  const planted = hold({ Root: { pos: [0, 0, HIP - 0.2] }, Torso: [['x', -10]], Chest: [['x', -4]], ...look(0, 10), ...STANCE },
    [0.2, 0.36, 0.3], pitched([0, 1, 0], -70));
  planted.Root.rot = [['z', turns * 360]];
  keys.push({ t: Math.round((end + 0.14) * 1000) / 1000, pose: planted });
  const back = GUARD(0); back.Root.rot = [['z', turns * 360]];
  keys.push({ t: Math.round((end + 0.42) * 1000) / 1000, pose: back });
  return keys;
}
const celestial_thousand_sweep = sweep(1);
const celestial_ten_thousand_sweep = sweep(2);

/** From the air: the spear raised, then planted as the body lands crouched (the wave goes out from it). */
begin();
const celestial_sweep_landing = [
  { t: 0, pose: AIR_RAISE(0, 0.05) },
  { t: 0.18, pose: hold({ Root: { pos: [0, 0.1, HIP - 0.4] }, Torso: [['x', -20]], Chest: [['x', -10]], ...look(0, 20), ...CROUCH }, [0.16, 0.34, 0.3], pitched([0, 1, 0], -78)) },
  { t: 0.3, pose: hold({ Root: { pos: [0, 0.1, HIP - 0.44] }, Torso: [['x', -22]], Chest: [['x', -10]], ...look(0, 20), ...CROUCH }, [0.16, 0.34, 0.26], pitched([0, 1, 0], -80)) },
  { t: 0.62, pose: GUARD(0.1) },
].map(k => k.t === 0.62 ? { ...k, pose: { ...k.pose, Root: { pos: [0, 0.1, HIP - 0.44] } } } : k);

const anims = { celestial_piercing_dragon, celestial_piercing_dragon_long, celestial_piercing_dive, celestial_plum_blossom, celestial_plum_rain,
  celestial_plum_air, celestial_thousand_sweep, celestial_ten_thousand_sweep, celestial_sweep_landing };
/** When each quick thrust of the flurries lands, and the last (seconds): the Java phases follow them (MurimAnimations). */
const timings = {
  plum: [...Array(5)].map((_, i) => +(PLUM_GAP * (i + 1)).toFixed(3)), plumLast: +(PLUM_GAP * 5 + PLUM_LAST).toFixed(3),
  rain: [...Array(8)].map((_, i) => +(RAIN_GAP * (i + 1)).toFixed(3)), rainLast: +(RAIN_GAP * 8 + PLUM_LAST).toFixed(3),
};
module.exports = { anims, timings };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) {
    const { times, frames } = sample(keys);
    let worst = { depth: 0 };
    times.forEach((t, n) => { const r = through(b.modelSpace(arm, frames[n])); if (r.depth > worst.depth) worst = { ...r, t }; });
    if (worst.depth > 2 * SHAFT) console.warn(`  ${name}: spear through the ${worst.part} at ${worst.t}s (${worst.depth.toFixed(2)})`);
    writeSmooth(name, keys);
  }
  console.log('timings', JSON.stringify(timings));
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'celestial_peak_spear.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('celestial_peak_spear', anims), null, 1));
  console.log('wrote', out);
}
