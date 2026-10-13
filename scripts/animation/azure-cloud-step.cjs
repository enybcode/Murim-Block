// Cloud Step Slash, remade in Blockbench (art/blockbench/azure_cloud_step.bbmodel), after Zoro's one-sword draw-cut dash:
//   cloud_step_aim    held while the technique's key is down: a crouched draw stance, the blade tucked back along the
//                     left hip like a sword in its sheath, the right hand on the hilt, the left hand holding the "sheath";
//                     the player aims with the camera;
//   cloud_step_slash  the key let go: the body shoots 5 blocks forward, low, the blade drawn in one level cut from the left
//                     hip out to the right (edge leading), and stops past the target, low, turned away, the arm held out;
//                     then the blade is flicked clean (Epic Fight then blends back to the stance).
// Usage: node scripts/animation/azure-cloud-step.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; Tool_R is the wrist, Tool_R x -92 lays the
// blade along the forearm; the cut's arm is the M1 sweep's). The root never turns (its hit box would turn with it): the
// body turns at the torso. The poses were solved for where the hilt, the point and the feet must be.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

/** How far the dash carries the player, in blocks. */
const DASH = 5.0;

/** The draw stance: crouched deep, leaning in, the blade sheathed back along the left hip, eyes on the target. */
const AIM = (breath = 0) => ({
  Root: { pos: [0, 0, 0.47 - breath * 0.02] },
  Torso: [['x', -45 - breath * 3], ['z', 28]], Chest: [['x', -6], ['z', -1]], Head: [['x', 40 + breath * 3], ['z', -24]],
  Arm_R: [['x', 33], ['y', 18], ['z', 47]], Hand_R: [['x', 52]], Tool_R: [['z', 27], ['y', 45], ['x', -172]],
  Arm_L: [['x', -4], ['y', -20], ['z', -6]], Hand_L: [['x', 55]],
  Thigh_R: [['x', -47], ['y', -15]], Leg_R: [['x', -15]],
  Thigh_L: [['x', 68], ['y', 9]], Leg_L: [['x', -36]],
});

/** Launched: thrown forward almost level, the back leg driving, the blade still sheathed. */
const LAUNCH = (y) => ({
  Root: { pos: [0, y, 0.5] },
  Torso: [['x', -60], ['z', 15]], Chest: [['x', -6], ['z', -20]], Head: [['x', 54], ['z', -12]],
  Arm_R: [['x', 79], ['y', 20], ['z', 56]], Hand_R: [['x', 0]], Tool_R: [['z', 24], ['y', -54], ['x', -142]],
  Arm_L: [['x', -8], ['y', -17], ['z', -20]], Hand_L: [['x', 101]],
  Thigh_R: [['x', -55]], Leg_R: [['x', -8]],
  Thigh_L: [['x', 72]], Leg_L: [['x', -95]],
});

/** The draw: the straight arm sweeping the level blade (`armX` pitch, `armZ` yaw), the trunk turning through. */
const DRAW = (y, trunk, lean, armX, armZ) => ({
  Root: { pos: [0, y, 0.52] },
  Torso: [['x', lean], ['z', trunk]], Chest: [['x', -6], ['z', trunk * 0.3]], Head: [['x', -lean * 0.9], ['z', -trunk * 0.9]],
  Arm_R: [['x', armX], ['z', armZ]], Hand_R: [['x', 10]], Tool_R: [['y', -90], ['x', -92]],
  Arm_L: [['x', -40], ['z', 30]], Hand_L: [['x', 20]],
  Thigh_R: [['x', -40]], Leg_R: [['x', -60]],
  Thigh_L: [['x', 50]], Leg_L: [['x', -80]],
});

/** Past the target: stopped low on the right knee, turned away, the arm stretched out to the right, the blade level behind. */
const AFTER = (y, settle = 0) => ({
  Root: { pos: [0, y, 0.4 - settle * 0.02] },
  Torso: [['x', -30 + settle * 4], ['z', -45]], Chest: [['x', -6], ['z', -15]], Head: [['x', 30 + settle * 8], ['z', 40]],
  Arm_R: [['x', 128], ['y', -72], ['z', -52]], Hand_R: [['x', 0]], Tool_R: [['z', 52], ['y', -83], ['x', -114]],
  Arm_L: [['x', -40], ['y', -12], ['z', -48]], Hand_L: [['x', 95]],
  Thigh_R: [['x', -28], ['y', -10]], Leg_R: [['x', -48]],
  Thigh_L: [['x', 85], ['y', 8]], Leg_L: [['x', -73]],
});

/** The blade flicked clean: the point snapped down and out at the right side (clear of the legs), before guard. */
const FLICK = (y) => ({
  ...AFTER(y, 1),
  Arm_R: [['x', 80], ['y', -103], ['z', -25]], Hand_R: [['x', 64]], Tool_R: [['z', 21], ['y', -5], ['x', -102]],
});

const cloud_step_aim = [
  { t: 0, pose: AIM(0) },
  { t: 0.6, pose: AIM(1) },
  { t: 1.2, pose: AIM(0) },
];

const cloud_step_slash = [
  { t: 0, pose: AIM(0) },
  { t: 0.04, pose: LAUNCH(0.4) },
  // The draw as the body flies: out of the "sheath" on the left, through the front, out to the right.
  { t: 0.08, pose: DRAW(1.6, 30, -50, 109, 40) },
  { t: 0.12, pose: DRAW(3.0, 0, -48, 130, -11) },
  { t: 0.16, pose: DRAW(4.3, -35, -45, 112, -70) },
  // Stopped past the target, held still a moment.
  { t: 0.21, pose: AFTER(DASH) },
  { t: 0.5, pose: AFTER(DASH, 1) },
  { t: 0.62, pose: FLICK(DASH) },
  // Held, then Epic Fight blends back to the stance (an Euler path to the guard would pass the blade through the legs).
  { t: 0.75, pose: FLICK(DASH) },
];

const anims = { cloud_step_aim, cloud_step_slash };
module.exports = { anims, DASH, POSES: { AIM, LAUNCH, DRAW, AFTER, FLICK } };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_cloud_step.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_cloud_step', anims), null, 1));
  console.log('wrote', out);
}
