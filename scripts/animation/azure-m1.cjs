// The four default strikes (M1) of Flowing Cloud Stance, as a Blockbench project to look at and edit before they go
// into the game: art/blockbench/azure_cloud_m1.bbmodel.
//   drifting_cut   a wide horizontal cut, right to left;
//   returning_cut  the backhand that comes back, left to right;
//   piercing_mist  drawn back, then a long lunging thrust;
//   falling_rain   raised in both hands, then a cut straight down (ends the chain).
// Usage: node scripts/animation/azure-m1.cjs
//
// Pose notation (pose.cjs): each joint turns in the character's axes (x its right, y its front, z up) within its
// parent: Arm x swings the arm forward and up (0 hanging, 90 straight ahead, 180 overhead), Arm z turns it toward the
// left (negative: to the right), Hand (the forearm, at the elbow) x bends the elbow. Tool_R is the wrist: the blade
// points forward from a hanging forearm; Tool_R x -90 lays it along the forearm, as an extension of the arm.
// The root never turns (its hit box would turn with it, see azure-cloud-animations.cjs): the body turns at the torso.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

const HIP = 0.764;

/** Head kept looking ahead while the trunk turns by `yaw`. */
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });

/** The ready stance: right foot ahead, knees soft, sword up and forward at chest height, left hand open in front. */
const GUARD = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.05] },
  Torso: [['z', -12]], Chest: [['x', -4], ['z', -6]], ...look(-18),
  Arm_R: [['x', 32], ['y', -8]], Hand_R: [['x', 42]], Tool_R: [['x', -32]],
  Arm_L: [['x', 28], ['y', 14]], Hand_L: [['x', 34]],
  Thigh_R: [['x', 22], ['y', -4]], Leg_R: [['x', -18]],
  Thigh_L: [['x', -14], ['y', 4]], Leg_L: [['x', -16]],
});

/** A horizontal sweep: the straight arm and its blade turned `yaw` (+ left), the trunk turned `trunk` with it. */
const SWEEP = (yaw, trunk, { y = 0, z = HIP - 0.08, pitch = 86, lead = 'R' } = {}) => ({
  Root: { pos: [0, y, z] },
  Torso: [['z', trunk * 0.6]], Chest: [['x', -10], ['z', trunk * 0.4]], ...look(trunk),
  Arm_R: [['x', pitch], ['z', yaw - trunk]], Hand_R: [['x', 10]], Tool_R: [['x', -92]],
  Arm_L: [['x', 20 - trunk * 0.3], ['y', 34]], Hand_L: [['x', 30]],
  ...(lead === 'R'
    ? { Thigh_R: [['x', 46], ['y', -6]], Leg_R: [['x', -42]], Thigh_L: [['x', -26], ['y', 6]], Leg_L: [['x', -10]] }
    : { Thigh_R: [['x', -18], ['y', -6]], Leg_R: [['x', -14]], Thigh_L: [['x', 40], ['y', 6]], Leg_L: [['x', -36]] }),
});

const drifting_cut = [
  { t: 0, pose: GUARD() },
  // Wound up to the right: the blade far back on the right, the trunk turned away, weight on the back foot.
  { t: 0.1, pose: { ...SWEEP(-115, -45, { z: HIP - 0.06, pitch: 84 }), Arm_L: [['x', 55], ['y', 10]], Hand_L: [['x', 20]],
    Thigh_R: [['x', 12]], Leg_R: [['x', -20]], Thigh_L: [['x', -6]], Leg_L: [['x', -26]] } },
  // Through the front with a step.
  { t: 0.19, pose: SWEEP(-5, 0, { y: 0.2 }) },
  // Carried through to the left.
  { t: 0.27, pose: SWEEP(95, 42, { y: 0.28, z: HIP - 0.1 }) },
  { t: 0.4, pose: SWEEP(105, 46, { y: 0.28, z: HIP - 0.1, pitch: 82 }) },
  { t: 0.56, pose: GUARD(0.28) },
];

const returning_cut = [
  { t: 0, pose: GUARD() },
  // The blade gathered high on the left, across the chest.
  { t: 0.09, pose: { ...SWEEP(100, 40, { pitch: 104 }), Hand_R: [['x', 34]], Arm_L: [['x', 40], ['y', 24]], Hand_L: [['x', 50]] } },
  // Back across the front, a little downward.
  { t: 0.18, pose: SWEEP(0, 0, { y: 0.18, pitch: 90, lead: 'L' }) },
  // Out to the right.
  { t: 0.26, pose: SWEEP(-100, -44, { y: 0.24, z: HIP - 0.1, pitch: 76, lead: 'L' }) },
  { t: 0.38, pose: SWEEP(-108, -48, { y: 0.24, z: HIP - 0.1, pitch: 72, lead: 'L' }) },
  { t: 0.52, pose: GUARD(0.24) },
];

/** Drawn back for the thrust: elbow behind, forearm and blade level and pointing ahead, the left hand aiming. */
const DRAWN = {
  Root: { pos: [0, -0.06, HIP - 0.1] },
  Torso: [['z', -34]], Chest: [['x', -6], ['z', -12]], ...look(-46),
  Arm_R: [['x', -30], ['y', -12]], Hand_R: [['x', 122]], Tool_R: [['x', -92]],
  Arm_L: [['x', 82], ['z', -18]], Hand_L: [['x', 4]],
  Thigh_R: [['x', 18], ['y', -4]], Leg_R: [['x', -30]], Thigh_L: [['x', -14], ['y', 4]], Leg_L: [['x', -34]],
};

/** The lunge: the whole arm and blade straight ahead, front knee deep, back leg long, left arm thrown back. */
const LUNGE = (y) => ({
  Root: { pos: [0, y, HIP - 0.17] },
  Torso: [['x', -8], ['z', 8]], Chest: [['x', -12], ['z', 6]], ...look(14, 6),
  Arm_R: [['x', 86], ['z', -4]], Hand_R: [['x', 2]], Tool_R: [['x', -92]],
  Arm_L: [['x', -38], ['y', 40]], Hand_L: [['x', 16]],
  Thigh_R: [['x', 72]], Leg_R: [['x', -74]],
  Thigh_L: [['x', -40]], Leg_L: [['x', -4]],
});

const piercing_mist = [
  { t: 0, pose: GUARD() },
  { t: 0.12, pose: DRAWN },
  { t: 0.22, pose: LUNGE(0.58) },
  { t: 0.36, pose: LUNGE(0.62) },
  { t: 0.6, pose: GUARD(0.62) },
];

/** Both hands on the hilt above the head, the blade hanging back behind the shoulders, the body stretched up. */
const RAISED = {
  Root: { pos: [0, -0.02, HIP + 0.02] },
  Torso: [['x', 8]], Chest: [['x', 8]], ...look(0, -14),
  Arm_R: [['x', 168], ['z', 8]], Hand_R: [['x', 18]], Tool_R: [['x', -30]],
  Arm_L: [['x', 164], ['z', -14]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 14]], Leg_R: [['x', -10]], Thigh_L: [['x', -16]], Leg_L: [['x', -8]],
};

/** The cut has fallen: both arms down in front, the blade pointing down at the ground ahead, body folded, knees bent. */
const FALLEN = (y) => ({
  Root: { pos: [0, y, HIP - 0.2] },
  Torso: [['x', -22]], Chest: [['x', -14]], ...look(0, 20),
  Arm_R: [['x', 68], ['z', 6]], Hand_R: [['x', 8]], Tool_R: [['x', -72]],
  Arm_L: [['x', 64], ['z', -16]], Hand_L: [['x', 14]],
  Thigh_R: [['x', 62]], Leg_R: [['x', -74]],
  Thigh_L: [['x', -22]], Leg_L: [['x', -44]],
});

const falling_rain = [
  { t: 0, pose: GUARD() },
  { t: 0.15, pose: RAISED },
  { t: 0.25, pose: FALLEN(0.3) },
  { t: 0.46, pose: FALLEN(0.32) },
  { t: 0.7, pose: GUARD(0.32) },
];

const anims = { drifting_cut, returning_cut, piercing_mist, falling_rain };
module.exports = { anims, GUARD };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'azure_cloud_m1.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('azure_cloud_m1', anims), null, 1));
  console.log('wrote', out);
}
