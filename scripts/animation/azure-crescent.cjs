// Heaven-Splitting Crescent, remade in Blockbench (art/blockbench/azure_crescent.bbmodel):
//   heaven_splitting_crescent  crouched deep, the blade low behind on the right; one rising cut, the straight arm
//                              sweeping the blade from behind the heel, through the front, up over the head, the edge
//                              leading; the player leaves the ground with it (0.2 s, AzureCloudSword) and rises 3 blocks
//                              holding the blade straight up at the sky, the body stretched, the left knee drawn up.
// Usage: node scripts/animation/azure-crescent.cjs
// Pose notation: see azure-m1.cjs. The blade stays along the arm (Tool_R x -92); turned over (y 180), its edge leads
// a forward-and-up sweep of the arm.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { GUARD } = require('./azure-m1.cjs');

/** The arm swung to `pitch` (−35 behind the hip, 90 straight ahead, 180 straight up), the blade along it, edge leading up. */
const SWING = (pitch, yaw = 0) => ({ Arm_R: [['x', pitch], ['z', yaw]], Hand_R: [['x', 5]], Tool_R: [['y', 180], ['x', -92]] });

/** Crouched deep and turned away, the blade low behind on the right, the left hand reaching ahead. */
const COIL = {
  Root: { pos: [0, -0.05, 0.44] },
  Torso: [['x', -34], ['z', -30]], Chest: [['x', -6], ['z', -9]], Head: [['x', 34], ['z', 26]],
  ...SWING(-40, -8),
  Arm_L: [['x', 60], ['z', 20]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 30]], Leg_R: [['x', -70]], Thigh_L: [['x', 80]], Leg_L: [['x', -110]],
};

/** Coming through: the body uncoiling and pushing off, the blade passing low in front. */
const LOW = {
  Root: { pos: [0, 0.05, 0.52] },
  Torso: [['x', -24], ['z', -8]], Chest: [['x', -6], ['z', -2]], Head: [['x', 20], ['z', 6]],
  ...SWING(25),
  Arm_L: [['x', 30], ['z', 30]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 10]], Leg_R: [['x', -20]], Thigh_L: [['x', 60]], Leg_L: [['x', -70]],
};

/** Rising: the cut up through the front, the body straightening, off the ground. */
const MID = {
  Root: { pos: [0, 0.1, 0.66] },
  Torso: [['x', -6]], Chest: [['x', -2]], Head: [['x', 0]],
  ...SWING(110),
  Arm_L: [['x', -20], ['z', 30]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 0]], Leg_R: [['x', -10]], Thigh_L: [['x', 70]], Leg_L: [['x', -85]],
};

/** Going up: the blade straight at the sky, the body stretched behind it, the left knee drawn up, the left arm down. */
const SKY = (tuck = 0) => ({
  Root: { pos: [0, 0.1, 0.8 - tuck * 0.06] },
  Torso: [['x', 6 - tuck * 6]], Chest: [['x', 4]], Head: [['x', -18 + tuck * 10]],
  ...SWING(178, 4),
  Arm_L: [['x', -24], ['z', 34]], Hand_L: [['x', 16]],
  Thigh_R: [['x', -6 + tuck * 30]], Leg_R: [['x', -10 - tuck * 50]], Thigh_L: [['x', 80 + tuck * 10]], Leg_L: [['x', -95 - tuck * 10]],
});

const heaven_splitting_crescent = [
  { t: 0, pose: GUARD() },
  { t: 0.13, pose: COIL },
  // The cut (hit 0.2 to 0.34): from behind the heel, through the front, up over the head.
  { t: 0.19, pose: LOW },
  { t: 0.24, pose: MID },
  { t: 0.3, pose: SKY(0) },
  // Held up through the rise (the top about 0.7 s), the legs drawing up at the top.
  { t: 0.7, pose: SKY(0.4) },
  { t: 0.9, pose: SKY(1) },
];

const anims = { heaven_splitting_crescent };
module.exports = { anims };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_crescent.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_crescent', anims), null, 1));
  console.log('wrote', out);
}
