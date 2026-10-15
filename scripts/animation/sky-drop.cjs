// The Alliance's reinforcements dropping from the sky (the Captain of the Peak Guard's summons, docs/mobs/peak-captain.md):
//   alliance_sky_fall      the fall, a loop: the body upright, the knees drawn up, the free arm out for balance, the
//                          spear held high, the coat's weight rocking a little;
//   alliance_sky_landing   the landing, once: the impact in a deep crouch (the left knee down, the right foot planted,
//                          the left fist on the ground, the spear swept out to the side), a beat held, then up into guard.
// Written as game files with quaternion turns between keys (pose.cjs writeSmooth), and as a Blockbench project to look
// at the keys (art/blockbench/sky_drop.bbmodel).
// Usage: node scripts/animation/sky-drop.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). A hanging
// limb: +x swings it forward, -y raises a right limb sideways (+y a left one).
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');

const HIP = 0.764;

// ---------------------------------------------------------------- the fall

/** Falling: `rock` from -1 to 1 rocks the body and the arms a little. */
const fall = (t, rock) => ({ t, pose: {
  Root: { pos: [0, 0, HIP] },
  Torso: [['x', -6 + 3 * rock]], Chest: [['x', -4]],
  Head: [['x', 14 - 3 * rock]],
  // The knees drawn up, the right a little higher.
  Thigh_R: [['x', 48 + 6 * rock], ['y', -6]], Leg_R: [['x', -78]],
  Thigh_L: [['x', 34 - 6 * rock], ['y', 6]], Leg_L: [['x', -66]],
  // The left arm out for balance, the spear arm high.
  Arm_L: [['x', 10], ['y', 72 + 8 * rock]], Hand_L: [['x', 20]],
  Arm_R: [['x', 150 - 6 * rock], ['y', -14]], Hand_R: [['x', 18]],
} });
const alliance_sky_fall = [fall(0, 0), fall(0.25, 1), fall(0.5, 0), fall(0.75, -1), fall(1.0, 0)];

// ---------------------------------------------------------------- the landing

/** The crouch at impact (`depth` 1) or rising out of it (to 0, standing in guard). */
const crouch = (t, depth, { head = 0, spear = 1 } = {}) => ({ t, pose: {
  Root: { pos: [0, 0.04 * depth, HIP - 0.42 * depth] },
  Torso: [['x', -30 * depth]], Chest: [['x', -10 * depth]],
  Head: [['x', -52 * depth + head]],
  // The right foot planted ahead, the left knee down behind.
  Thigh_R: [['x', 92 * depth], ['y', -10 * depth]], Leg_R: [['x', -96 * depth]],
  Thigh_L: [['x', 18 * depth], ['y', 8 * depth]], Leg_L: [['x', -112 * depth]],
  // The left fist on the ground, the spear swept out to the right.
  Arm_L: [['x', 22 * depth], ['y', 14 * depth]], Hand_L: [['x', 6 * depth]],
  Arm_R: [['x', 30 + 10 * (1 - spear)], ['y', -70 * spear]], Hand_R: [['x', 30]],
} });
const alliance_sky_landing = [
  // The impact: from the fall straight into the crouch, a little bounce deeper.
  crouch(0, 0.7, { head: 10 }),
  crouch(0.08, 1.06),
  crouch(0.18, 1.0),
  // The beat held, the head coming up to look at the foe.
  crouch(0.5, 0.98, { head: -6 }),
  // Up into guard.
  crouch(0.85, 0.35, { spear: 0.4 }),
  crouch(1.2, 0, { spear: 0 }),
];

const anims = { alliance_sky_fall, alliance_sky_landing };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'sky_drop.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('sky_drop', anims), null, 1));
  console.log('wrote', out);
}
