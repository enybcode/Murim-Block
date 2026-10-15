// Meditation (docs/cultivation/qi-cultivation.md) as a Blockbench project (art/blockbench/meditation.bbmodel):
//   meditate_sit        from standing: a step in, the legs crossing as the body goes down, seated in the lotus;
//   meditate            the lotus, held: legs crossed, back straight, hands joined before the belly, a slow breath (loop);
//   meditate_rise       from the lotus back up on the feet;
//   meditate_pulse      a small breakthrough: the breath drawn in at once, the chest lifting, the head up, then settled;
//   meditate_ascend     a great breakthrough: still in the lotus, the body rises a block and a half, hangs, comes down;
//   meditate_deviation  the Qi going wrong: doubled over, a hand to the chest, coughing, then falling to one side.
// Usage: node scripts/animation/meditation.cjs, then node scripts/animation/blockbench-to-epicfight.cjs art/blockbench/meditation.bbmodel
//
// Pose notation: see azure-m1.cjs (character axes: x its right, y its front, z up). Rotations are about the parent's
// axes, in order: a leg turned first about z (its own length, the knee facing out), then raised (x), then swung out (z)
// bends its knee (Leg x) sideways, across the other leg: the crossed legs of the lotus.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

const HIP = 0.764;
/** Hip height seated on the ground. */
const SEAT = 0.17;

const STAND = {
  Root: { pos: [0, 0, HIP] },
};

/** The lotus: `breath` 0..1 lifts the chest a little; `lift` raises the whole seated body (blocks). */
const LOTUS = (breath = 0, lift = 0) => ({
  Root: { pos: [0, 0, SEAT + lift + breath * 0.012] },
  Torso: [['x', 2 - breath * 2]], Chest: [['x', -1 - breath * 3]], Head: [['x', 4 + breath * 2]],
  // The hands joined before the belly, palms up, the elbows out.
  Arm_R: [['x', 22], ['z', 26]], Hand_R: [['x', 74]],
  Arm_L: [['x', 22], ['z', -26]], Hand_L: [['x', 74]],
  // Knees out, shins crossed: the right shin in front.
  Thigh_R: [['z', -90], ['x', 86], ['z', -50]], Leg_R: [['x', -116]],
  Thigh_L: [['z', 90], ['x', 92], ['z', 50]], Leg_L: [['x', -120]],
});

/** Halfway down: the knees bending and opening, the hands coming forward for balance. */
const KNEEL = {
  Root: { pos: [0, 0.02, 0.44] },
  Torso: [['x', -14]], Chest: [['x', -4]], Head: [['x', 14]],
  Arm_R: [['x', 34], ['z', 14]], Hand_R: [['x', 40]],
  Arm_L: [['x', 34], ['z', -14]], Hand_L: [['x', 40]],
  Thigh_R: [['z', -50], ['x', 70], ['z', -24]], Leg_R: [['x', -96]],
  Thigh_L: [['z', 50], ['x', 72], ['z', 24]], Leg_L: [['x', -100]],
};

const meditate_sit = [
  { t: 0, pose: STAND },
  { t: 0.35, pose: KNEEL },
  { t: 0.7, pose: LOTUS(0) },
  { t: 0.9, pose: LOTUS(0.2) },
];

const meditate = [
  { t: 0, pose: LOTUS(0.2) },
  { t: 1.6, pose: LOTUS(1) },
  { t: 2.4, pose: LOTUS(1) },
  { t: 4.0, pose: LOTUS(0.2) },
];

const meditate_rise = [
  { t: 0, pose: LOTUS(0.2) },
  { t: 0.3, pose: KNEEL },
  { t: 0.65, pose: STAND },
];

/** The breath drawn in at once: chest up, head back a little, the hands opening. */
const PULSE = {
  ...LOTUS(1),
  Torso: [['x', -2]], Chest: [['x', -10]], Head: [['x', -14]],
  Arm_R: [['x', 18], ['z', 36]], Hand_R: [['x', 60]], Arm_L: [['x', 18], ['z', -36]], Hand_L: [['x', 60]],
};
const meditate_pulse = [
  { t: 0, pose: LOTUS(0.2) },
  { t: 0.25, pose: PULSE },
  { t: 0.9, pose: PULSE },
  { t: 1.4, pose: LOTUS(0.2) },
];

/** Raised in the lotus, the arms opening out, palms up, the head up to the sky. */
const RISEN = (lift) => ({
  ...LOTUS(1, lift),
  Chest: [['x', -8]], Head: [['x', -18]],
  Arm_R: [['x', 30], ['z', -52]], Hand_R: [['x', 30]], Arm_L: [['x', 30], ['z', 52]], Hand_L: [['x', 30]],
});
const meditate_ascend = [
  { t: 0, pose: LOTUS(0.2) },
  { t: 0.6, pose: LOTUS(1, 0.3) },
  { t: 1.6, pose: RISEN(1.5) },
  { t: 2.8, pose: RISEN(1.55) },
  { t: 3.6, pose: LOTUS(0.6, 0) },
  { t: 4.0, pose: LOTUS(0.2) },
];

/** Doubled over, the right hand pressed to the chest, the left on the ground. */
const DOUBLED = (cough) => ({
  ...LOTUS(0),
  Torso: [['x', -34 - cough * 10]], Chest: [['x', -12 - cough * 6]], Head: [['x', 30 + cough * 8]],
  Arm_R: [['x', 52], ['z', 40]], Hand_R: [['x', 118]],
  Arm_L: [['x', 40], ['z', -18]], Hand_L: [['x', 10]],
});
/** Fallen onto the left side, propped on the left forearm. */
const FALLEN = {
  Root: { pos: [0, 0, 0.3], rot: [['y', -38]] },
  Torso: [['x', -10]], Chest: [['x', -6]], Head: [['x', 20], ['y', 20]],
  Arm_R: [['x', 46], ['z', 36]], Hand_R: [['x', 112]],
  Arm_L: [['x', 70], ['z', -40]], Hand_L: [['x', 70]],
  Thigh_R: [['z', -60], ['x', 60], ['z', -20]], Leg_R: [['x', -90]],
  Thigh_L: [['z', 60], ['x', 70], ['z', 20]], Leg_L: [['x', -100]],
};
const meditate_deviation = [
  { t: 0, pose: LOTUS(0.2) },
  { t: 0.2, pose: DOUBLED(0) },
  { t: 0.35, pose: DOUBLED(1) },
  { t: 0.5, pose: DOUBLED(0) },
  { t: 0.65, pose: DOUBLED(1) },
  { t: 0.8, pose: DOUBLED(0) },
  { t: 1.3, pose: FALLEN },
  { t: 2.2, pose: FALLEN },
];

const anims = { meditate_sit, meditate, meditate_rise, meditate_pulse, meditate_ascend, meditate_deviation };
module.exports = { anims, LOTUS, SEAT };

/** Bare hands: the rig's sword (the cubes of Tool_R) taken out of the project; the bone stays. */
function bareHands(proj) {
  const blade = new Set();
  (function walk(nodes, inTool) {
    for (const n of nodes) {
      if (typeof n === 'string') { if (inTool) blade.add(n); continue; }
      walk(n.children, inTool || n.name === 'Tool_R');
      if (n.name === 'Tool_R') n.children = n.children.filter(c => typeof c !== 'string');
    }
  })(proj.outliner, false);
  proj.elements = proj.elements.filter(e => !blade.has(e.uuid));
  return proj;
}

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'meditation.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(bareHands(project('meditation', anims)), null, 1));
  console.log('wrote', out);
}
