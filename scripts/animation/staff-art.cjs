// The Traveller's Staff Art's three techniques (Enzo's design sheet "Art fondamental du bâton", 16/10/2026), with the
// Traveller's Staff (traveller-staff-model.cjs: two blocks long, held at its middle):
//   staff_vertical_strike   1.00 s. Held as a two-handed sabre (the left hand at the lower end, the right a shoulder's
//                           width above it), raised in both hands before the forehead, the staff up and a little back
//                           clear over the skull (0.20 s), brought down in one vertical arc right of the body's middle,
//                           the top end striking ahead at chest height (0.45 s), the trunk following it down without
//                           planting it (0.65 s), back on guard (1.00 s). The feet do not move.
//   staff_point_flurry      1.20 s. Gripped low, side-on: five straight thrusts with the end, the right hand driving the
//                           staff from the hip through the left hand, which stays forward as a guide; each thrust drawn
//                           back so it reads apart (contacts 0.22, 0.38, 0.54, 0.70, 0.88 s); the last pushed further,
//                           the back heel turning; back on guard (1.20 s).
//   staff_rampart_start     0.15 s. The staff slides through the hands to its middle and comes up before the chest,
//                           already turning.
//   staff_rampart_loop      0.40 s, looping. The arms hold still before the chest; the staff turns in the fingers about
//                           its middle, like a propeller in the plane before the body (one turn in 0.4 s). It passes
//                           well clear of the legs below and of the face above.
//   staff_rampart_end       0.25 s. The spin braked upright, the staff slid back to the low grip, on guard.
// Each key says where the right hand holds, which way the staff points, and where along the staff the hand is (the grip):
// both arms and the wrist are solved for it, the left hand on the shaft ahead of the right, and nothing of the staff,
// nor the elbows, may pass through the body (the solve gives way, then every sampled frame is checked). The grip and the
// spin are the hand joint's (Tool_R) own slide and turn, so the arms never wind round. Written into the game files
// (animmodels/animations/biped/murim/staff_*.json) and a Blockbench project to look at (art/blockbench/
// traveller_staff_art.bbmodel: the rig's sword replaced by the staff, its slide and spin included).
// Usage: node scripts/animation/staff-art.cjs
// Axes: the character's (x right, y front, z up), in blocks; the root's position +y forward; heights from the ground.
const fs = require('fs');
const path = require('path');
const { project, toBB, toBlockbenchEuler, uuid } = require('./blockbench-rig.cjs');
const { model, measure, inBody } = require('./ik.cjs');
const { sample, writeFrames, arm, MURIM_BIPED } = require('./pose.cjs');
const pose3 = require('./pose.cjs');
const b = require('./biped.cjs');

const HIP = 0.764;
const norm = v => { const l = Math.hypot(...v); return v.map(x => x / l); };
const add = (a, c, k = 1) => a.map((x, i) => x + c[i] * k);
const d2 = (a, c) => a.reduce((s, x, i) => s + (x - c[i]) ** 2, 0);
const yawed = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x * Math.cos(a) - y * Math.sin(a), x * Math.sin(a) + y * Math.cos(a), z]; };
const pitched = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x, y * Math.cos(a) - z * Math.sin(a), y * Math.sin(a) + z * Math.cos(a)]; };
const rot3 = m => [m[0], m[1], m[2], 0, m[4], m[5], m[6], 0, m[8], m[9], m[10], 0, 0, 0, 0, 1];
const apply = (m, p) => [0, 1, 2].map(r => m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]);
const turnOnly = (m, v) => [0, 1, 2].map(r => m[r * 4] * v[0] + m[r * 4 + 1] * v[1] + m[r * 4 + 2] * v[2]);
const transpose3 = m => [m[0], m[4], m[8], 0, m[1], m[5], m[9], 0, m[2], m[6], m[10], 0, 0, 0, 0, 1];

// ---------------------------------------------------------------- the staff in the hand
const bindPose = b.modelSpace(arm, pose3.locals({}));
const HILT0 = b.origin(bindPose.Tool_R);
/** The staff's line in the bind pose: the rig's sword line tipped up 20 degrees, as the spears are held. */
const LINE0 = norm(pitched(norm(add([0.37, 1.005, 0.73], HILT0, -1)), 20));
/** The same line in the hand joint's own frame. */
const U_LOCAL = turnOnly(transpose3(rot3(bindPose.Tool_R)), LINE0);
/** The staff: one block either side of its middle; the low grip holds it near its end (its middle 0.85 ahead of the
 *  right hand, only 0.15 of it behind the fist), as a staff is held to strike or thrust. */
const HALF = 1.0, LOW = 0.85, MIDDLE = 0;
/** Spinning, the staff turns in the fingers, a little ahead of the palm and the wrists. */
const FINGERS = 0.1;
const SHAFT = 0.04;
/** How far the hands reach from the shoulders: the arm's two bones (0.30 and 0.27) all but straight. */
const REACH = 0.55;
/** A point of the staff `s` blocks along it from the right hand, in a posed body. */
const along = (m, s) => apply(b.mul(m.Tool_R, b.invRigid(bindPose.Tool_R)), add(HILT0, LINE0, s));


/** The arms as capsules (shoulder to elbow, elbow to wrist), each 0.125 thick: the staff may touch them only in the fists. */
const WRIST_R = b.origin(arm.joints.Tool_R.local);
function WRIST_R_BIND() { return apply(bindPose.Hand_R, WRIST_R); }
function armSegments(m) {
  const sR = b.origin(m.Arm_R), eR = b.origin(m.Hand_R), wR = apply(m.Hand_R, WRIST_R);
  const sL = b.origin(m.Arm_L), eL = b.origin(m.Hand_L), wL = b.origin(m.Tool_L);
  return { segments: [[sR, eR], [eR, wR], [sL, eL], [eL, wL]], wrists: [wR, wL] };
}
const toSegment = (p, [a, c]) => {
  const ab = add(c, a, -1), t = Math.max(0, Math.min(1, ab.reduce((s, v, i) => s + v * (p[i] - a[i]), 0) / Math.max(1e-9, ab.reduce((s, v) => s + v * v, 0))));
  return Math.sqrt(d2(p, add(a, ab, t)));
};
/** The fist: the arm's last 4 pixels (a quarter block) up from the wrist, where the staff goes through the hand. */
const FIST = 0.25;
/** How deep a point of the staff is in an arm (> 0: inside), the fists left out: on a forearm, only what is further up
 *  it than the fist counts. */
function intoArms(m) {
  const { segments, wrists } = armSegments(m);
  return p => {
    if (wrists.some(w => Math.sqrt(d2(p, w)) < 0.16)) return 0;
    return Math.max(...segments.map(([a, c], i) => {
      if (i % 2 === 1) {
        // A forearm (elbow to wrist): cut short of the fist.
        const len = Math.sqrt(d2(a, c));
        if (len <= FIST) return -1;
        c = add(a, add(c, a, -1), (len - FIST) / len);
      }
      return 0.125 + SHAFT - toSegment(p, [a, c]);
    }));
  };
}

/** The arm's two bones, from the bind pose: shoulder to elbow, elbow to wrist. */
const UPPER = Math.sqrt(d2(b.origin(bindPose.Hand_R), b.origin(bindPose.Arm_R))), FORE = Math.sqrt(d2(WRIST_R_BIND(), b.origin(bindPose.Hand_R))) ;
/** Where the elbows bend toward: out to the side and down. */
const POLES = { R: [0.8, -0.15, -0.6], L: [-0.8, -0.15, -0.6] };
const ARMS = ['Arm_R', 'Hand_R', 'Tool_R', 'Arm_L', 'Hand_L'];
let previous = null;
const begin = () => { previous = null; };
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });
const STANCE = { Thigh_L: [['x', 24], ['y', 6]], Leg_L: [['x', -20]], Thigh_R: [['x', -18], ['y', -8]], Leg_R: [['x', -16]] };

/**
 * A pose holding the staff: `body` gives the root, trunk, head and legs; `hand` is where the right hand holds (from
 * the root), `dir` where the staff points, `grip` where the staff's middle is ahead of the right hand, `front` how far
 * along it the left hand holds. The hold gives a little (the hand up to 0.05, the staff turned up to 8 degrees) if the
 * staff would pass through the body or out of the hands' reach. Both arms and the wrist are then solved; the elbows are
 * kept out of the body and the arms near a natural bend.
 */
function hold(body, hand, dir, { grip = LOW, front = 0.42, guess = {}, poles = POLES } = {}) {
  const pose = JSON.parse(JSON.stringify({
    Arm_R: [['x', 40], ['y', 0], ['z', 20]], Hand_R: [['x', 60]], Tool_R: [['x', -10], ['y', 0], ['z', 0]],
    Arm_L: [['x', 70], ['y', 10], ['z', -20]], Hand_L: [['x', 30]], ...guess, ...body,
  }));
  const lo = grip - HALF, hi = grip + HALF, ALONG = [];
  for (let s = lo; s <= hi + 1e-9; s += 0.08) ALONG.push(s);
  const root = pose.Root.pos, from = previous;
  // Each frame starts from the last one's arms and stays near them (the solution cannot flip round between frames).
  if (from) for (const j of ARMS) pose[j] = JSON.parse(JSON.stringify(from[j]));
  const still = () => from ? 2e-8 * ARMS.reduce((c, j) => c + pose[j].reduce((cc, r, i) => cc + (r[1] - from[j][i][1]) ** 2, 0), 0) : 0;
  const turned = (v, [ax, ay, az]) => {
    const r = (a, i, j) => { const c = Math.cos(a * Math.PI / 180), n = Math.sin(a * Math.PI / 180), w = [...v]; w[i] = v[i] * c - v[j] * n; w[j] = v[i] * n + v[j] * c; v = w; };
    r(ax, 1, 2); r(ay, 2, 0); r(az, 0, 1); return v;
  };
  const give = [0, 0, 0, 0, 0, 0];
  const line = () => ({ d: norm(turned(norm(dir), give.slice(3))), H: add(add(root, hand), give.slice(0, 3).map(v => v / 100)) });
  const posed0 = model(pose), depth0 = inBody(posed0, SHAFT);
  const shoulder = j => b.origin(posed0[j]);
  const sR = shoulder('Arm_R'), sL = shoulder('Arm_L');
  const beyond = (p, s) => Math.max(0, Math.sqrt(d2(p, s)) - REACH) ** 2;
  const holdCost = () => {
    const { d, H } = line();
    let into = 0;
    for (const s of ALONG) into += Math.max(0, depth0(add(H, d, s)).depth + 0.02) ** 2;
    const moved = 3 * give.slice(0, 3).reduce((c, v) => c + (v / 100) ** 2, 0) + 1.5e-4 * give.slice(3).reduce((c, v) => c + v * v, 0);
    return 80 * into + 400 * (beyond(H, sR) + beyond(add(H, d, front), sL)) + moved;
  };
  let low = holdCost(), step = 8;
  while (step > 0.1) {
    let improved = false;
    for (let i = 0; i < 6; i++) for (const s of [step, -step]) {
      const old = give[i];
      give[i] = Math.max(i < 3 ? -5 : -8, Math.min(i < 3 ? 5 : 8, old + s));
      const c = holdCost();
      if (c < low - 1e-9) { low = c; improved = true; } else give[i] = old;
    }
    if (!improved) step *= 0.6;
  }
  const { d, H } = line();
  const T = add(H, d, 1), L = add(H, d, front);
  // Each arm by two-bone IK: the elbow placed by the law of cosines from the arm's true lengths, bent out and down
  // (the pole), then the joint angles fitted to that elbow and wrist; the right wrist alone turns the staff.
  const fit = (vars, cost) => {
    const get = ([j, i]) => pose[j][i][1], set = ([j, i], v) => { pose[j][i][1] = v; };
    let best = cost(), st = 24;
    for (let it = 0; it < 3000 && st > 0.05; it++) {
      let improved = false;
      for (const v of vars) for (const s of [st, -st]) {
        const old = get(v), nv = Math.max(v[2], Math.min(v[3], old + s));
        set(v, nv); const c = cost();
        if (c < best - 1e-12) { best = c; improved = true; } else set(v, old);
      }
      if (!improved) st *= 0.6;
    }
  };
  const m0 = model(pose);
  for (const [side, W, pole] of [['R', H, poles.R], ['L', L, poles.L]]) {
    const S = b.origin(m0['Arm_' + side]);
    // Never quite straight: a straight arm leaves the elbow's turn free (and the solve flipping round).
    const D = Math.min(0.97 * (UPPER + FORE), Math.max(1e-3, Math.sqrt(d2(W, S))));
    const u = norm(add(W, S, -1));
    const pv = add(pole, u, -pole.reduce((s, v, i) => s + v * u[i], 0)), v = norm(pv);
    const x = (UPPER * UPPER - FORE * FORE + D * D) / (2 * D), h = Math.sqrt(Math.max(0, UPPER * UPPER - x * x));
    const E = add(add(S, u, x), v, h);
    const wristOf = m => side === 'R' ? apply(m.Hand_R, WRIST_R) : b.origin(m.Tool_L);
    fit([['Arm_' + side, 0, -90, 220], ['Arm_' + side, 1, -110, 110], ['Arm_' + side, 2, -180, 180], ['Hand_' + side, 0, 0, 160]],
      () => { const m = model(pose); return d2(b.origin(m['Hand_' + side]), E) + d2(wristOf(m), W) + still(); });
  }
  fit([['Tool_R', 0, -180, 180], ['Tool_R', 1, -180, 180], ['Tool_R', 2, -180, 180]], () => {
    const m = model(pose); return d2(along(m, 1), T) + d2(along(m, 0.5), add(H, d, 0.5)) + still();
  });
  const near = (a, c) => { while (a - c > 180) a -= 360; while (c - a > 180) a += 360; return a; };
  for (const j of ARMS) {
    const xyz = pose[j];
    if (from) xyz.forEach((r, i) => { r[1] = near(r[1], from[j][i][1]); });
    xyz.forEach(r => { r[1] = Math.round(r[1]); });
  }
  previous = Object.fromEntries(ARMS.map(j => [j, JSON.parse(JSON.stringify(pose[j]))]));
  const m = model(pose), at = measure(pose, m);
  const missL = Math.sqrt(d2(at.handL, along(m, front))), off = Math.sqrt(d2(at.hilt, H));
  const aim = norm(add(along(m, 1), along(m, 0), -1)), wrong = Math.acos(Math.min(1, aim.reduce((c, v, i) => c + v * d[i], 0))) * 180 / Math.PI;
  if (wrong > 4) console.warn(`  staff off its line at ${solvingAt}s by ${wrong.toFixed(0)} degrees`);
  if (missL > 0.08 || off > 0.08) console.warn(`  pose off at ${solvingAt}s (hand ${hand.map(x => x.toFixed(2))}): right hand ${off.toFixed(2)}, left hand ${missL.toFixed(2)} from the shaft`);
  return pose;
}

/** A target: at time `t`, the body, where the right hand holds, where the staff points, its grip, the left hand's place
 *  along it, its spin in the fingers (degrees). The animation is solved from targets every 0.05 s (densify). */
const target = (t, bodySpec, hand, dir, { grip = LOW, front = 0.42, spin = 0, poles = POLES } = {}) => ({ t, body: bodySpec, hand, dir: norm(dir), grip, front, spin, poles });
const body = (y = 0, z = 0, torsoX = 0, turn = 0, headPitch = 0, legs = STANCE, chestX = null) => ({
  Root: { pos: [0, y, HIP + z] }, Torso: [['x', torsoX], ['z', turn]], Chest: [['x', chestX ?? torsoX * 0.4], ['z', turn * 0.3]], ...look(turn, headPitch), ...legs });
const lerp = (a, c, k) => a + (c - a) * k;
/** Two bodies blended: every angle and the root's position (both written by body(), so with the same joints). */
function blendBody(a, c, k) {
  const out = JSON.parse(JSON.stringify(a));
  for (const j of Object.keys(a)) {
    if (j === 'Root') out.Root.pos = a.Root.pos.map((v, i) => lerp(v, c.Root.pos[i], k));
    else out[j] = a[j].map(([axis, v], i) => [axis, lerp(v, (c[j] && c[j][i]) ? c[j][i][1] : v, k)]);
  }
  return out;
}
/** The targets between keys, smoothed (ease in and out), solved every `step` seconds: every sample's hold avoids the
 *  body and the arms, so nothing passes through them between the keys either. */
let solvingAt = 0;
/**
 * The right hand's place brought within both arms' reach, the staff's slant kept: an arm cannot stretch, so the hands
 * come closer to the shoulders instead (shown when it moves them more than 3 centimetres).
 */
function inReach(bodySpec, hand, dir, front, t) {
  const m = model(bodySpec), root = bodySpec.Root.pos, sR = b.origin(m.Arm_R), sL = b.origin(m.Arm_L);
  let H = add(root, hand);
  const pull = (p, sh) => { const dd = Math.sqrt(d2(p, sh)); return dd <= REACH ? p : add(sh, add(p, sh, -1), REACH / dd); };
  for (let n = 0; n < 12; n++) {
    H = pull(H, sR);
    const L = add(H, dir, front), L2 = pull(L, sL);
    H = add(H, add(L2, L, -1));
  }
  const moved = add(H, root, -1);
  if (Math.sqrt(d2(moved, hand)) > 0.03 && t !== undefined) console.warn(`  brought within reach at ${t}s: hand ${hand.map(x => x.toFixed(2))} -> ${moved.map(x => x.toFixed(2))}`);
  return moved;
}

function densify(targets, step = 0.05) {
  for (const tg of targets) tg.hand = inReach(tg.body, tg.hand, tg.dir, tg.front, tg.t);
  begin();
  const end = targets[targets.length - 1].t, times = [];
  for (let t = 0; t < end - 1e-9; t += step) times.push(+t.toFixed(4));
  for (const tg of targets) if (!times.some(x => Math.abs(x - tg.t) < 1e-6)) times.push(tg.t);
  times.sort((x, y) => x - y);
  return times.map(t => {
    let i = 0;
    while (i < targets.length - 2 && t > targets[i + 1].t) i++;
    const a = targets[i], c = targets[i + 1] || a;
    const raw = c.t > a.t ? Math.min(1, Math.max(0, (t - a.t) / (c.t - a.t))) : 0, k = raw * raw * (3 - 2 * raw);
    const g = { grip: lerp(a.grip, c.grip, k), front: lerp(a.front, c.front, k), spin: lerp(a.spin, c.spin, raw) };
    solvingAt = t;
    const bodyNow = blendBody(a.body, c.body, k), dirNow = norm(a.dir.map((v, n) => lerp(v, c.dir[n], k)));
    const pose = hold(bodyNow, inReach(bodyNow, a.hand.map((v, n) => lerp(v, c.hand[n], k)), dirNow, g.front), dirNow, { grip: g.grip, front: g.front,
      poles: { R: a.poles.R.map((v, n) => lerp(v, c.poles.R[n], k)), L: a.poles.L.map((v, n) => lerp(v, c.poles.L[n], k)) } });
    return { t, pose, grip: g.grip, spin: g.spin };
  });
}

/** On guard, gripped low: the right hand before the right hip, the staff rising ahead at the enemy's chest. */
// The left shoulder turned toward the enemy (the left hand forward on the shaft), the right hand at the right hip.
const GUARD_BODY = body(0, 0, -2, -16), GUARD_HAND = [0.16, 0.26, 0.3], GUARD_DIR = yawed(norm([0, 1, 0.42]), 10);
const guard = t => target(t, GUARD_BODY, GUARD_HAND, GUARD_DIR, { front: 0.38 });

// ---------------------------------------------------------------- 1. Vertical Strike (1.00 s)
// Held as a two-handed sabre: the left hand at the lower end (0.15 of the staff below it), the right hand two fists
// above it, guiding; so, raised upright before the forehead, the left hand stays at the chin, in reach. The
// strike stays in one vertical plane a little right of the middle (x 0.06): raised, the staff stands up before the face
// and passes clear over the skull; coming down it stays ahead of the face and the chest.
const VS_X = 0.06, SABRE = { grip: HALF - 0.41, front: -0.26 };
const sabre = (t, bodySpec, hand, dir) => target(t, bodySpec, hand, dir, SABRE);
const staff_vertical_strike = densify([
  // On guard: the staff rising ahead at the enemy's chest, the right hand before the waist, the left at the belly.
  sabre(0, body(0, 0, -2, 8), [0.12, 0.44, 0.4], yawed([0, 1, 0.55], 2)),
  // Lifting: the hands come up before the face, the staff turning upright.
  sabre(0.1, body(0, 0.01, 2, 6, -4), [VS_X, 0.36, 0.96], [0, 0.25, 1]),
  // Raised, 0.20 s: the right hand before the forehead, the left at the chin, the staff up and a little back, clear
  // over the skull; the head level.
  sabre(0.2, body(0, 0.02, 4, 4, -4), [VS_X, 0.3, 1.16], [0, -0.3, 0.95]),
  // Coming down: forward, the hands falling before the chin...
  sabre(0.33, body(0, 0, -4, 3, 0), [VS_X, 0.4, 1.02], [0, 0.55, 0.84]),
  sabre(0.39, body(0, -0.02, -9, 2, 2), [VS_X, 0.48, 0.8], [0, 0.85, 0.5]),
  // The contact, 0.45 s: the top end strikes ahead at chest height, the arms out.
  sabre(0.45, body(0, -0.05, -14, 2, 4), [VS_X, 0.56, 0.6], [0, 1, 0.04]),
  // The follow-through: the trunk goes on down with it, the end stopping well short of the ground.
  sabre(0.65, body(0, -0.07, -16, 2, 8), [VS_X + 0.02, 0.52, 0.42], pitched([0, 1, 0], -12)),
  sabre(0.82, body(0, -0.03, -10, 6, 4), [0.1, 0.46, 0.42], pitched([0, 1, 0], 12)),
  sabre(1.0, body(0, 0, -2, 8), [0.12, 0.44, 0.4], yawed([0, 1, 0.55], 2)),
]);

// ---------------------------------------------------------------- 2. Flurry of Points (1.20 s)
// Side-on (the trunk turned 32 degrees, the left shoulder toward the enemy), gripped low: the right hand drives the staff from behind the hip up to the
// left hand, which stays forward as a guide (it holds further back along the staff as the right hand comes up to it).
// The staff runs on one straight line at the enemy (FLURRY_DIR, through FLURRY_AT beside the right hip): the right hand
// slides along it from the hip (BACK) to just behind the left hand (OUT), the left hand stays put on it as a guide. The
// arms alone are too short for a long stroke, so the trunk drives it: side-on when drawn back (the right shoulder
// back), squared up and leaning into the thrust (the right shoulder carried forward). The root stays put: Epic Fight
// turns an action animation's root travel into the player's own movement.
const SIDE = -48, FLURRY_DIR = norm([-0.04, 1, 0.1]), FLURRY_AT = [0.17, 0, 0.3], LEFT_AT = 0.56;
/** Drawn back, the right hand holds further up the staff (its end 0.4 behind the fist); on the thrust the staff shoots
 *  forward through both hands to the low grip, so it travels 0.25 further than the arms can carry it. */
const SHOT = LOW - 0.25;
/** The right elbow out to the side and back: the forearm comes in across the staff, not along it (so the staff behind
 *  the fist passes under the wrist, clear of the forearm). */
const THRUST_POLES = { R: [1, -0.5, 0.1], L: POLES.L };
const onLine = (s, yaw) => add(FLURRY_AT, yawed(FLURRY_DIR, yaw), s);
const BACK = (t, yaw = 0) => target(t, body(0, -0.07, -4, SIDE, 0), onLine(-0.06, yaw), yawed(FLURRY_DIR, yaw), { front: LEFT_AT + 0.06, grip: SHOT, poles: THRUST_POLES });
const OUT = (t, yaw = 0, far = 0) => target(t, body(0, -0.1 - far * 0.04, -12 - far * 6, SIDE + 30 + far * 10, 0,
  far ? { Thigh_L: [['x', 34], ['y', 6]], Leg_L: [['x', -32]], Thigh_R: [['x', -26], ['y', -8]], Leg_R: [['x', -6]] } : STANCE),
  onLine(0.38 + far * 0.04, yaw), yawed(FLURRY_DIR, yaw), { front: LEFT_AT + 0.04 - 0.38 - far * 0.04, poles: THRUST_POLES });
const CONTACTS = [0.22, 0.38, 0.54, 0.7, 0.88];
const flurryTargets = [guard(0), BACK(0.15)];
CONTACTS.forEach((t, i) => {
  const last = i === CONTACTS.length - 1, yaw = [0, 3, -3, 2, 0][i];
  flurryTargets.push(OUT(t, yaw, last ? 1 : 0));
  if (!last) flurryTargets.push(BACK(+(t + 0.08).toFixed(3), yaw * 0.5));
});
flurryTargets.push(BACK(1.03), guard(1.2));
const staff_point_flurry = densify(flurryTargets);

// ---------------------------------------------------------------- 3. Spinning Rampart (start, loop, end)
// The arms hold still: both hands before the chest, side by side at the staff's middle; the staff, upright at first,
// turns in the fingers about its middle in the plane before the body.
const SPIN_BODY = body(0, -0.06, -4, 2, 0, { Thigh_L: [['x', 22], ['y', 8]], Leg_L: [['x', -26]], Thigh_R: [['x', -12], ['y', -8]], Leg_R: [['x', -20]] });
const spinAt = (t, spin) => target(t, SPIN_BODY, [0.02, 0.4, 0.52], [0, 0, 1], { grip: MIDDLE, front: 0.12, spin });
// Brought upright before the chest still gripped low, then let slide down through the hands to its middle.
const uprightLow = (t, spin = 0) => target(t, SPIN_BODY, [0.02, 0.4, 0.52], [0, 0, 1], { grip: LOW, front: 0.3, spin });
const staff_rampart_start = densify([guard(0), uprightLow(0.1), spinAt(0.15, 40)], 0.025);
const STEPS = 16, LOOP = 0.4;
begin();
const SPIN_POSE = hold(SPIN_BODY, [0.02, 0.4, 0.52], [0, 0, 1], { grip: MIDDLE, front: 0.12 });
const staff_rampart_loop = [...Array(STEPS + 1)].map((_, i) => ({ t: +(LOOP * i / STEPS).toFixed(4), pose: SPIN_POSE, grip: MIDDLE, spin: 360 * i / STEPS }));
// Braked upright (a half turn, from the loop's last half: upright, the staff looks the same either way up), then slid back
// to the low grip and brought down on guard (0.25 s in all: in 0.2 the staff would whip down faster than any blow).
const staff_rampart_end = densify([spinAt(0, -180), spinAt(0.05, -95), spinAt(0.1, -25), spinAt(0.12, 0), guard(0.25)], 0.025);

const anims = { staff_vertical_strike, staff_point_flurry, staff_rampart_start, staff_rampart_loop, staff_rampart_end };
/** Everything played 1.5 times faster than keyed (Enzo, 16/10/2026: "too slow"): the keys above keep the design sheet's
 *  times, the files are written sped up, and the timings the gameplay reads are the played ones. */
const SPEED = 1.5;
const played = t => +(t / SPEED).toFixed(4);
const timings = { vertical: played(0.45), flurry: CONTACTS.map(played) };

// ---------------------------------------------------------------- the grip and the spin, on the hand joint
const lerpAt = (keys, t, f) => {
  let i = 0;
  while (i < keys.length - 2 && t > keys[i + 1].t) i++;
  const a = keys[i], c = keys[i + 1] || a, k = c.t > a.t ? Math.min(1, Math.max(0, (t - a.t) / (c.t - a.t))) : 0;
  return f(a) + (f(c) - f(a)) * k;
};

/** Every sampled frame of an animation, the staff slid to its grip and turned by its spin in the hand. */
function frames(keys) {
  const { times, frames: fr } = sample(keys);
  const out = fr.map((f, n) => {
    const t = times[n], grip = lerpAt(keys, t, k => k.grip), spin = lerpAt(keys, t, k => k.spin);
    const m = b.modelSpace(arm, f);
    // The spin: about the model's forward axis through the hand, in the hand joint's own frame.
    const axis = norm(turnOnly(transpose3(rot3(m.Tool_R)), [0, 1, 0]));
    const turn = b.rotation(axis, spin);
    const slide = b.translation(...U_LOCAL.map(v => v * grip));
    // In the fingers, ahead of the palm, as much as the grip has come to the middle.
    const ahead = FINGERS * Math.max(0, 1 - grip / LOW), fwd = norm(turnOnly(transpose3(rot3(m.Tool_R)), [0, 1, 0]));
    const fingers = b.translation(...fwd.map(v => v * ahead));
    return { ...f, Tool_R: b.mul(f.Tool_R, b.mul(fingers, b.mul(turn, slide))) };
  });
  return { times, frames: out, grips: times.map(t => lerpAt(keys, t, k => k.grip)) };
}

/** How deep the staff or the elbows go into the body in a frame (and where). */
function check(f, grip) {
  const m = b.modelSpace(arm, f), inside = inBody(m, 0);
  let worst = { depth: 0, part: null, what: '' };
  for (let s = grip - HALF; s <= grip + HALF + 1e-9; s += 0.05) {
    // The staff's points, in the slid and turned hand: its middle is at the hand's own origin now.
    const p = apply(b.mul(m.Tool_R, b.invRigid(bindPose.Tool_R)), add(HILT0, LINE0, s - grip));
    const r = inside(p);
    if (r.depth - SHAFT > worst.depth) worst = { depth: r.depth - SHAFT, part: r.part, what: 'staff ' + s.toFixed(2) };
  }
  const arms = intoArms(m);
  for (let s = grip - HALF; s <= grip + HALF + 1e-9; s += 0.05) {
    const p = apply(b.mul(m.Tool_R, b.invRigid(bindPose.Tool_R)), add(HILT0, LINE0, s - grip)), r = arms(p);
    if (r > worst.depth) worst = { depth: r, part: 'an arm', what: 'staff ' + s.toFixed(2) };
  }
  for (const j of ['Hand_R', 'Hand_L']) {
    const r = inBody(m, -0.03)(b.origin(m[j]));
    if (r.depth > worst.depth) worst = { ...r, what: 'elbow ' + j };
  }
  return worst;
}

// ---------------------------------------------------------------- the Blockbench project to look at
/** The rotation of each joint in the rig's terms (character axes), and the hand joint's slide, from final frames. */
function rigKeys(times, fr) {
  const binds = b.modelSpace(arm, pose3.locals({}));
  return times.map((t, n) => {
    const f = fr[n], rots = {};
    for (const j of arm.order) {
      const J = arm.joints[j].local, B = rot3(binds[j]), L = f[j];
      const r = j === 'Root' ? b.mul(rot3(L), b.invRigid(rot3(J))) : b.mul(B, b.mul(b.invRigid(J), b.mul(L, b.invRigid(B))));
      rots[j] = [r[0], r[1], r[2], r[4], r[5], r[6], r[8], r[9], r[10]];
    }
    // The slide: the hand joint's own offset, seen in its parent's rest frame (the rig's axes).
    const J = arm.joints.Tool_R.local, x = b.mul(b.invRigid(J), f.Tool_R);
    const offset = turnOnly(rot3(binds.Hand_R), turnOnly(rot3(J), b.origin(x)));
    return { t, rots, pos: b.origin(f.Root), slide: offset };
  });
}

function staffProject(all) {
  const rig = {};
  for (const [name, { times, frames: fr }] of Object.entries(all)) rig[name] = rigKeys(times, fr);
  const p = project('traveller_staff_art', rig);
  const sword = new Set(['grip', 'guard', 'blade', 'edge']);
  const blade = p.elements.find(e => e.name === 'blade'), grip = p.elements.find(e => e.name === 'grip');
  const [tx, ty, tz] = [(grip.from[0] + grip.to[0]) / 2, (grip.from[1] + grip.to[1]) / 2, grip.from[2] + 1];
  const swordIds = new Set(p.elements.filter(e => sword.has(e.name)).map(e => e.uuid));
  p.elements = p.elements.filter(e => !sword.has(e.name));
  // The staff along the sword's line (-z from the hand), tipped up 20 degrees as in the game, centred on the hand.
  const ids = [], ash = grip.faces.north.uv, iron = blade.faces.north.uv;
  for (const [name, s0, s1, w, uv] of [['staff', -HALF, HALF, 0.5, ash], ['ferrule_low', -HALF, -HALF + 0.07, 0.6, iron], ['ferrule_high', HALF - 0.07, HALF, 0.6, iron]]) {
    const id = uuid();
    ids.push(id);
    p.elements.push({ ...blade, name, uuid: id, from: [tx - w, ty - w, tz - s1 * 16], to: [tx + w, ty + w, tz - s0 * 16], origin: [tx, ty, tz],
      rotation: [20, 0, 0], faces: Object.fromEntries(Object.entries(blade.faces).map(([f, v]) => [f, { ...v, uv }])) });
  }
  const walk = node => {
    if (!node || typeof node !== 'object' || !Array.isArray(node.children)) return;
    if (node.name === 'Tool_R') node.children = [...node.children.filter(c => !swordIds.has(c)), ...ids];
    node.children.forEach(walk);
  };
  p.outliner.forEach(walk);
  // The slide as the hand joint's position keys.
  const toolGroup = (function find(nodes) { for (const n of nodes) if (typeof n === 'object') { if (n.name === 'Tool_R') return n; const r = find(n.children); if (r) return r; } })(p.outliner);
  for (const a of p.animations) {
    const keys = rig[a.name], anim = a.animators[toolGroup.uuid];
    if (!anim) continue;
    for (const k of keys) {
      const d = toBB(k.slide);
      anim.keyframes.push({ channel: 'position', data_points: [{ x: -d[0], y: d[1], z: d[2] }], uuid: uuid(), time: k.t, color: -1, interpolation: 'linear' });
    }
  }
  return p;
}

module.exports = { anims, timings };

if (require.main === module) {
  const all = {};
  for (const [name, keys] of Object.entries(anims)) {
    const done = frames(keys);
    done.times = done.times.map(played);
    let worst = { depth: 0 };
    done.frames.forEach((f, n) => { const r = check(f, done.grips[n]); if (process.env.ENDS === name && r.depth > 0.02) console.log("  into", done.times[n], r.what, r.part, r.depth.toFixed(3)); if (r.depth > worst.depth) worst = { ...r, t: done.times[n] }; });
    // The staff's two ends from frame to frame: a jump (a wrist flipping round) shows as a sudden leap of an end.
    const ends = done.frames.map((f, n) => { const m = b.modelSpace(arm, f), w = b.mul(m.Tool_R, b.invRigid(bindPose.Tool_R));
      return [-HALF, HALF].map(s => apply(w, add(HILT0, LINE0, s))); });
    let leap = { v: 0 };
    for (let n = 1; n < ends.length; n++) {
      const dt = done.times[n] - done.times[n - 1], v = Math.max(...[0, 1].map(e => Math.sqrt(d2(ends[n][e], ends[n - 1][e])))) / dt;
      if (v > leap.v) leap = { v, t: done.times[n] };
    }
    console.log(`  fastest end: ${leap.v.toFixed(1)} blocks/s at ${leap.t}s`);
    if (process.env.ENDS === name) ends.forEach((e, n) => console.log(done.times[n], e[1].map(v => v.toFixed(2)).join(" "), e[0].map(v => v.toFixed(2)).join(" ")));
    console.log(`${name}: ${done.times.length} frames, ${done.times[done.times.length - 1]} s; ` + (worst.depth > 0.005 ? `deepest: ${worst.what} ${worst.depth.toFixed(3)} into ${worst.part} at ${worst.t}s` : 'nothing through the body'));
    writeFrames(MURIM_BIPED, name, done.times, done.frames);
    all[name] = done;
  }
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'traveller_staff_art.bbmodel');
  fs.writeFileSync(out, JSON.stringify(staffProject(all), null, 1));
  console.log('timings', JSON.stringify(timings), '\nwrote', out);
}
