// Thousand Drizzle at range (no enemy within 3 blocks), remade in Blockbench (art/blockbench/azure_drizzle_wave.bbmodel):
//   drizzle_wave  one long thrust that sends the great ghost sword (QiBladeEntity) down the line of the blade: drawn far
//                 back, the body turned away, then a deep lunge, the whole right arm and the blade in one straight line
//                 at shoulder height, held while the ghost sword leaves (0.25 s), then back to guard.
// Usage: node scripts/animation/azure-drizzle-wave.cjs
// Pose notation: see azure-m1.cjs. The poses were solved for where the hilt and the point must be.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { GUARD } = require('./azure-m1.cjs');

/** Drawn back: the elbow far behind, the blade level by the right ribs, point forward; the left hand aiming ahead. */
const DRAWN = {
  Root: { pos: [0, -0.1, 0.62] },
  Torso: [['x', -6], ['z', -40]], Chest: [['x', -4], ['z', -14]], Head: [['x', 6], ['z', 48]],
  Arm_R: [['x', -78], ['y', -22], ['z', 43]], Hand_R: [['x', 139]], Tool_R: [['z', 61], ['y', 87], ['x', -107]],
  Arm_L: [['x', 103], ['z', 20]], Hand_L: [['x', 4]],
  Thigh_R: [['x', 16]], Leg_R: [['x', -34]], Thigh_L: [['x', -14]], Leg_L: [['x', -36]],
};

/** The lunge: front knee deep, back leg long, the right arm and the blade one straight line, the left arm thrown back. */
const LUNGE = (y, reach = 0) => ({
  Root: { pos: [0, y, 0.58 - reach * 0.02] },
  Torso: [['x', -14 - reach * 3], ['z', 37]], Chest: [['x', -6], ['z', 10]], Head: [['x', 14], ['z', -28]],
  Arm_R: [['x', 114], ['y', 14], ['z', -34]], Hand_R: [['x', 0]], Tool_R: [['z', -29], ['y', 56], ['x', -68]],
  Arm_L: [['x', -40], ['y', 40]], Hand_L: [['x', 10]],
  Thigh_R: [['x', 72]], Leg_R: [['x', -74]], Thigh_L: [['x', -42]], Leg_L: [['x', -4]],
});

const drizzle_wave = [
  { t: 0, pose: GUARD() },
  { t: 0.13, pose: DRAWN },
  // The thrust: fast, all the way out as the ghost sword leaves (0.25 s).
  { t: 0.21, pose: LUNGE(0.5) },
  { t: 0.27, pose: LUNGE(0.56, 1) },
  { t: 0.48, pose: LUNGE(0.56, 1) },
  { t: 0.68, pose: GUARD(0.56) },
];

const anims = { drizzle_wave };
module.exports = { anims };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_drizzle_wave.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_drizzle_wave', anims), null, 1));
  console.log('wrote', out);
}
