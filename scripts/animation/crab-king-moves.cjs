// The Crab King's moves (docs/mobs/crab-king.md), on the biped skeleton his mesh is made for (crab-king-3d.cjs: the outer
// finger of each pincer on the forearm, the inner one on the hand, so turning the hand about the front opens the pincer):
//   crab_king_idle   five seconds that loop: hunched, feet wide, the pincers held out low; slow breaths, a look about,
//                    the pincers clacking twice;
//   crab_king_sweep  the great right pincer wound back on the right, open, and swept level across the front, snapping;
//   crab_king_snap   both pincers lifted open, then a lunge, both driven forward and snapped shut;
//   crab_king_slam   both pincers raised high over the head and slammed to the ground ahead (the shockwave is the
//                    server's, CrabKingEntity, at CrabKingAnimations.SLAM_IMPACT);
//   crab_king_roar   under half his health: the pincers flung wide and open, the head thrown back.
// Written as game files with quaternion turns between keys (pose.cjs writeSmooth), and as a Blockbench project to look
// at the keys (art/blockbench/crab_king_moves.bbmodel).
// Usage: node scripts/animation/crab-king-moves.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Arm y spreads
// the arm out to its side (the right negative); Head x pitch (+ down), z yaw (+ left).
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');

const HIP = 0.764;
/** The pincers opened by `open` degrees (the hand turned about the front, away from the outer finger). */
const pincers = (openR, openL = openR) => ({ Tool_R: [['y', -openR]], Tool_L: [['y', openL]] });
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
const WIDE = { Thigh_R: [['x', 10], ['y', -8]], Leg_R: [['x', -16]], Thigh_L: [['x', -6], ['y', 8]], Leg_L: [['x', -14]] };

/** The stance: hunched, feet wide, the pincers out low and a little forward. */
const STANCE = (t = 0, { yaw = 0, open = 0 } = {}) => ({
  Root: { pos: [0, 0, HIP - 0.04] },
  Torso: [['x', -12]], Chest: [['x', -8 + breath(t, 2.5, 3)]], Head: [['x', -14], ['z', yaw]],
  Arm_R: [['x', 18], ['y', -20]], Hand_R: [['x', 26]], Arm_L: [['x', 14], ['y', 18]], Hand_L: [['x', 22]],
  ...pincers(open, open * 0.8), ...WIDE,
});

const anims = {};

// The idle: breathing, a look left then right, two clacks of the pincers.
{
  const clack = t => Math.max(0, Math.sin((t - 1.1) * Math.PI / 0.3)) * (t > 1.1 && t < 1.7 ? 24 : 0) + (t > 3.2 && t < 3.5 ? 24 * Math.sin((t - 3.2) * Math.PI / 0.3) : 0);
  const yaw = t => 18 * Math.sin((2 * Math.PI * t) / 5);
  const keys = [];
  for (let t = 0; t <= 5.0001; t += 0.1) keys.push({ t: Math.round(t * 10) / 10, pose: STANCE(t, { yaw: yaw(t), open: clack(t) }) });
  anims.crab_king_idle = keys;
}

// The sweep: the great right pincer, open, wound back on the right and swept level across the front.
anims.crab_king_sweep = [
  { t: 0, pose: STANCE() },
  { t: 0.35, pose: { ...STANCE(), Torso: [['x', -8], ['z', -38]], Chest: [['x', -4], ['z', -14]], Head: [['x', -10], ['z', 40]],
      Arm_R: [['x', 84], ['z', -74]], Hand_R: [['x', 14]], ...pincers(34, 6), Arm_L: [['x', 30], ['y', 24]] } },
  { t: 0.5, pose: { ...STANCE(), Torso: [['x', -10], ['z', 0]], Chest: [['x', -6]], Head: [['x', -10]],
      Arm_R: [['x', 88], ['z', 0]], Hand_R: [['x', 10]], ...pincers(20, 6), Arm_L: [['x', 22], ['y', 30]],
      Thigh_R: [['x', 20], ['y', -8]], Leg_R: [['x', -22]], Thigh_L: [['x', -14], ['y', 8]], Leg_L: [['x', -10]] } },
  { t: 0.64, pose: { ...STANCE(), Torso: [['x', -10], ['z', 36]], Chest: [['x', -6], ['z', 12]], Head: [['x', -10], ['z', -30]],
      Arm_R: [['x', 80], ['z', 72]], Hand_R: [['x', 22]], ...pincers(0, 6), Arm_L: [['x', 14], ['y', 34]],
      Thigh_R: [['x', 20], ['y', -8]], Leg_R: [['x', -22]], Thigh_L: [['x', -14], ['y', 8]], Leg_L: [['x', -10]] } },
  { t: 1.0, pose: STANCE() },
];

// The snap: both pincers lifted wide open, then a lunge, both driven forward and snapped shut.
anims.crab_king_snap = [
  { t: 0, pose: STANCE() },
  { t: 0.4, pose: { ...STANCE(), Torso: [['x', -2]], Chest: [['x', 8]], Head: [['x', -6]],
      Arm_R: [['x', 72], ['y', -28]], Hand_R: [['x', 34]], Arm_L: [['x', 72], ['y', 28]], Hand_L: [['x', 34]], ...pincers(38) } },
  { t: 0.56, pose: { ...STANCE(), Root: { pos: [0, 0.35, HIP - 0.1] }, Torso: [['x', -20]], Chest: [['x', -8]], Head: [['x', -12]],
      Arm_R: [['x', 92], ['y', -6]], Hand_R: [['x', 6]], Arm_L: [['x', 92], ['y', 6]], Hand_L: [['x', 6]], ...pincers(26),
      Thigh_R: [['x', 38], ['y', -6]], Leg_R: [['x', -40]], Thigh_L: [['x', -22], ['y', 8]], Leg_L: [['x', -12]] } },
  { t: 0.64, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.1] }, Torso: [['x', -22]], Chest: [['x', -8]], Head: [['x', -12]],
      Arm_R: [['x', 92], ['y', -4]], Hand_R: [['x', 4]], Arm_L: [['x', 92], ['y', 4]], Hand_L: [['x', 4]], ...pincers(0),
      Thigh_R: [['x', 40], ['y', -6]], Leg_R: [['x', -42]], Thigh_L: [['x', -24], ['y', 8]], Leg_L: [['x', -12]] } },
  { t: 0.9, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.08] }, Torso: [['x', -18]],
      Arm_R: [['x', 70], ['y', -12]], Hand_R: [['x', 20]], Arm_L: [['x', 70], ['y', 12]], Hand_L: [['x', 20]], ...pincers(0) } },
  { t: 1.25, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.04] } } },
];

// The slam: both pincers raised high over the head, the back arched, then slammed to the ground ahead, crouching.
const SLAM_IMPACT = 0.74;
anims.crab_king_slam = [
  { t: 0, pose: STANCE() },
  { t: 0.5, pose: { ...STANCE(), Root: { pos: [0, -0.05, HIP + 0.02] }, Torso: [['x', 6]], Chest: [['x', 12]], Head: [['x', -18]],
      Arm_R: [['x', 168], ['y', -16]], Hand_R: [['x', 30]], Arm_L: [['x', 168], ['y', 16]], Hand_L: [['x', 30]], ...pincers(30),
      Thigh_R: [['x', 4], ['y', -8]], Leg_R: [['x', -8]], Thigh_L: [['x', -4], ['y', 8]], Leg_L: [['x', -6]] } },
  { t: SLAM_IMPACT, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.24] }, Torso: [['x', -36]], Chest: [['x', -18]], Head: [['x', 10]],
      Arm_R: [['x', 62], ['y', -12]], Hand_R: [['x', 4]], Arm_L: [['x', 62], ['y', 12]], Hand_L: [['x', 4]], ...pincers(4),
      Thigh_R: [['x', 56], ['y', -8]], Leg_R: [['x', -76]], Thigh_L: [['x', 40], ['y', 8]], Leg_L: [['x', -62]] } },
  { t: 1.05, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.2] }, Torso: [['x', -32]], Chest: [['x', -16]], Head: [['x', 4]],
      Arm_R: [['x', 60], ['y', -14]], Hand_R: [['x', 8]], Arm_L: [['x', 60], ['y', 14]], Hand_L: [['x', 8]], ...pincers(10),
      Thigh_R: [['x', 52], ['y', -8]], Leg_R: [['x', -70]], Thigh_L: [['x', 36], ['y', 8]], Leg_L: [['x', -58]] } },
  { t: 1.45, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.04] } } },
];

// The roar: the pincers flung wide and open, the chest thrown out, the head back.
anims.crab_king_roar = [
  { t: 0, pose: STANCE() },
  { t: 0.45, pose: { ...STANCE(), Torso: [['x', 4]], Chest: [['x', 16]], Head: [['x', -30]],
      Arm_R: [['x', 46], ['y', -62]], Hand_R: [['x', 24]], Arm_L: [['x', 46], ['y', 62]], Hand_L: [['x', 24]], ...pincers(42) } },
  { t: 1.5, pose: { ...STANCE(), Torso: [['x', 6]], Chest: [['x', 18]], Head: [['x', -34], ['z', 6]],
      Arm_R: [['x', 50], ['y', -66]], Hand_R: [['x', 20]], Arm_L: [['x', 50], ['y', 66]], Hand_L: [['x', 20]], ...pincers(40) } },
  { t: 2.0, pose: STANCE() },
];

module.exports = { anims, SLAM_IMPACT };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'crab_king_moves.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('crab_king_moves', anims), null, 1));
  console.log('wrote', out);
}
