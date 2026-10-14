// Heaven-Splitting Crescent, remade in Blockbench (art/blockbench/azure_crescent.bbmodel):
//   heaven_splitting_crescent  crouched deep, the blade low behind on the right; one rising cut, the straight arm
//                              sweeping the blade from behind the heel, through the front, up over the head, the edge
//                              leading; the player leaves the ground with it (0.2 s, AzureCloudSword) and rises 3 blocks
//                              holding the blade straight up at the sky, the body stretched, the left knee drawn up;
//   crescent_slam              at the top, with the body it carried: one cut straight down that throws it to the ground.
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

/** The blade chopped down from overhead (`pitch` as for SWING), along the arm, its edge leading down. */
const CHOP = (pitch) => ({ Arm_R: [['x', pitch], ['z', 4]], Hand_R: [['x', 5]], Tool_R: [['y', 0], ['x', -92]] });
const TUCK = { Thigh_R: [['x', 70]], Leg_R: [['x', -100]], Thigh_L: [['x', 80]], Leg_L: [['x', -105]] };

/**
 * At the top of the rise, with the body it carried: the blade swung back over the head, the body arched, then one cut
 * straight down that throws the body to the ground (AzureCloudSword: it falls hard and the landing shakes the ground);
 * the player comes down slowly, folded over the cut.
 */
const crescent_slam = [
  { t: 0, pose: SKY(1) },
  { t: 0.08, pose: { Root: { pos: [0, 0, 0.8] }, Torso: [['x', 22]], Chest: [['x', 8]], Head: [['x', -14]], ...CHOP(176),
    Arm_L: [['x', 60], ['z', 20]], Hand_L: [['x', 20]], ...TUCK } },
  { t: 0.14, pose: { Root: { pos: [0, 0.1, 0.8] }, Torso: [['x', -15]], Chest: [['x', -4]], Head: [['x', 10]], ...CHOP(140),
    Arm_L: [['x', 10], ['z', 30]], Hand_L: [['x', 20]], ...TUCK } },
  { t: 0.2, pose: { Root: { pos: [0, 0.2, 0.76] }, Torso: [['x', -45]], Chest: [['x', -8]], Head: [['x', 34]], ...CHOP(88),
    Arm_L: [['x', -40], ['z', 30]], Hand_L: [['x', 20]],
    Thigh_R: [['x', -20]], Leg_R: [['x', -40]], Thigh_L: [['x', 85]], Leg_L: [['x', -100]] } },
  { t: 0.45, pose: { Root: { pos: [0, 0.2, 0.76] }, Torso: [['x', -40]], Chest: [['x', -8]], Head: [['x', 30]], ...CHOP(84),
    Arm_L: [['x', -40], ['z', 30]], Hand_L: [['x', 20]],
    Thigh_R: [['x', -10]], Leg_R: [['x', -50]], Thigh_L: [['x', 80]], Leg_L: [['x', -95]] } },
  { t: 0.7, pose: { Root: { pos: [0, 0.2, 0.76] }, Torso: [['x', -20]], Chest: [['x', -4]], Head: [['x', 14]], ...CHOP(70),
    Arm_L: [['x', -10], ['z', 25]], Hand_L: [['x', 20]], ...TUCK } },
];

const anims = { heaven_splitting_crescent, crescent_slam };
module.exports = { anims };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_crescent.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_crescent', anims), null, 1));
  console.log('wrote', out);
}
