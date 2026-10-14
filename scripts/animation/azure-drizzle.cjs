// Thousand Drizzle's flurry (an enemy within 3 blocks), remade in Blockbench (art/blockbench/azure_drizzle.bbmodel):
//   thousand_drizzle  six quick cuts, each along its own line (diagonals, a horizontal, a vertical), the straight arm
//                     sweeping the blade edge first across the front, each through the middle at its hit (0.2 to 0.8 s,
//                     MurimAnimations.DRIZZLE_THRUSTS); then a wide last cut with a step in (through the middle at
//                     1.05 s, DRIZZLE_FINAL + 0.07), held, and back to guard. The storm (evolved) plays the same.
// The lines of the cuts are SLASHES below; the game draws a sharp cut in the air along each (DrizzleSlashEffects keeps
// the same table). Usage: node scripts/animation/azure-drizzle.cjs
// Pose notation: see azure-m1.cjs. Each arm pose is solved (ik.cjs) for the blade to lie along its cut, edge leading.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { GUARD } = require('./azure-m1.cjs');
const { solve } = require('./ik.cjs');

/**
 * The cuts: when the blade passes through the middle (s), the line's angle across the front (degrees from level,
 * positive rising to the right) and which way it goes along it (+1 from left to right).
 */
const SLASHES = [
  { t: 0.2, roll: -35, way: 1 },   // from high on the left down to the right
  { t: 0.32, roll: 0, way: -1 },   // level, right to left
  { t: 0.44, roll: 40, way: 1 },   // from low on the left up to the right
  { t: 0.56, roll: -80, way: 1 },  // from above straight down
  { t: 0.68, roll: -50, way: -1 }, // from low on the right up to the left
  { t: 0.8, roll: -10, way: 1 },   // level, left to right
];
/** The last cut: wide and level, right to left, with a step in. */
const FINAL = { t: 1.05, roll: 5, way: -1, sweep: 80 };
/** How far each side of the middle a cut sweeps (degrees). */
const SWEEP = 62;

const rad = d => d * Math.PI / 180;
const norm = v => { const l = Math.hypot(...v); return v.map(x => x / l); };
/** Where the blade points `a` degrees along a cut's line from straight ahead (character axes). */
function direction(roll, a) {
  const u = [Math.cos(rad(roll)), 0, Math.sin(rad(roll))];
  return norm([u[0] * Math.sin(rad(a)), Math.cos(rad(a)), u[2] * Math.sin(rad(a)) - 0.08]);
}

/** The body for a cut: in a short stance, the trunk turning with the blade. */
function body(step, turn) {
  return {
    Root: { pos: [0, step, 0.7] },
    Torso: [['x', -10], ['z', turn]], Chest: [['x', -4], ['z', turn * 0.4]], Head: [['x', 8], ['z', -turn * 0.8]],
    Arm_L: [['x', 20 - turn * 0.3], ['z', 30]], Hand_L: [['x', 30]],
    Thigh_R: [['x', 34]], Leg_R: [['x', -34]], Thigh_L: [['x', -22]], Leg_L: [['x', -18]],
  };
}

let guess = { Arm_R: [['x', 80], ['y', 0], ['z', 0]], Hand_R: [['x', 10]], Tool_R: [['y', 0], ['x', -92]] };
/** The pose `a` degrees along a cut (negative: the side it comes from), solved from the last one (smooth angles). */
function cutPose(cut, a, step) {
  const d = direction(cut.roll, a * cut.way);
  // Sideways, the trunk turns toward the blade.
  const turn = -d[0] * 28;
  const pose = { ...body(step, turn), ...JSON.parse(JSON.stringify(guess)) };
  // The cuts turn about a point before the chest (the elbow bends to bring the blade across to the left).
  const pivot = [0.12, step + 0.2, 1.22];
  const hilt = pivot.map((v, i) => v + d[i] * 0.42);
  const tangent = norm(direction(cut.roll, (a + 1) * cut.way).map((v, i) => v - d[i]));
  const r = solve(pose, [['Arm_R', 0, -90, 200], ['Arm_R', 1, -120, 120], ['Arm_R', 2, -150, 150], ['Hand_R', 0, 0, 130], ['Tool_R', 0, -200, 200]], {
    hilt: { at: hilt, w: 3 },
    tip: { at: hilt.map((v, i) => v + d[i] * 1.04), w: 3 },
    // The edge leads the sweep.
    edge: { at: hilt.map((v, i) => v + d[i] * 0.5 + tangent[i] * 0.1), w: 1 },
  }, 3000, 2e-6);
  guess = { Arm_R: r.pose.Arm_R, Hand_R: r.pose.Hand_R, Tool_R: r.pose.Tool_R };
  return r.pose;
}

const keys = [{ t: 0, pose: GUARD() }];
keys.push({ t: 0.12, pose: cutPose(SLASHES[0], -SWEEP - 10, 0.15) });
for (const c of SLASHES) {
  keys.push({ t: c.t - 0.05, pose: cutPose(c, -SWEEP, 0.2) });
  keys.push({ t: c.t, pose: cutPose(c, 0, 0.25) });
  keys.push({ t: c.t + 0.04, pose: cutPose(c, SWEEP, 0.25) });
}
// The last cut: gathered far to the right, then all the way across with a step in, held.
keys.push({ t: 0.95, pose: cutPose(FINAL, -FINAL.sweep - 10, 0.3) });
keys.push({ t: FINAL.t, pose: cutPose(FINAL, 0, 0.75) });
keys.push({ t: FINAL.t + 0.07, pose: cutPose(FINAL, FINAL.sweep, 0.9) });
keys.push({ t: 1.3, pose: cutPose(FINAL, FINAL.sweep + 4, 0.9) });
keys.push({ t: 1.5, pose: GUARD(0.9) });

const anims = { thousand_drizzle: keys, thousand_drizzle_storm: keys };
module.exports = { anims, SLASHES, FINAL };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_drizzle.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_drizzle', anims), null, 1));
  console.log('wrote', out);
}
