// The Golden Bell's moves for the player (docs/combat/styles/golden-bell.md), bare hands. Written as Epic Fight
// animations biped/murim/gb_* (pose.cjs writeSmooth); what they do is the server's (martial/GoldenBell).
//   gb_form      Golden Bell: feet together, the palms brought together before the chest, the head bowed, a breath that
//                straightens the body (the bell comes down on it);
//   gb_rebound   Iron Rebound: a half step back, the right palm thrust out flat, the left hand on the hip, held a second;
//   gb_rooted    Rooted Mountain: a deep horse stance, the fists at the hips, held four seconds;
//   gb_finish    Last Bell (the finish): over the body, the palm laid on its chest, three pushes as the bell rings, up.
// Usage: node scripts/animation/golden-bell-art.cjs
// Pose notation: see azure-m1.cjs (Arm x forward and up, Hand x bends the elbow, Arm y out to the side, - for the right).
const { writeSmooth } = require('./pose.cjs');

const HIP = 0.764;
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });

const STAND = {
  Root: { pos: [0, 0, HIP - 0.02] }, Torso: [], Chest: [], ...look(0),
  Arm_R: [['x', 10], ['y', -8]], Hand_R: [['x', 20]], Arm_L: [['x', 10], ['y', 8]], Hand_L: [['x', 20]],
  Thigh_R: [['y', -4]], Leg_R: [], Thigh_L: [['y', 4]], Leg_L: [],
};
/** The palms together before the chest, the elbows out, the head bowed. */
const PRAY = (bow = 1) => ({
  Root: { pos: [0, 0, HIP - 0.03 * bow] }, Torso: [['x', 4 * bow]], Chest: [['x', 4 * bow]], ...look(0, 18 * bow),
  Arm_R: [['x', 46], ['y', -34]], Hand_R: [['x', 112], ['z', 30]], Arm_L: [['x', 46], ['y', 34]], Hand_L: [['x', 112], ['z', -30]],
  Thigh_R: [['y', -2]], Leg_R: [['x', -4 * bow]], Thigh_L: [['y', 2]], Leg_L: [['x', -4 * bow]],
});
/** Straightened, chest out, the palms still joined: the bell has come down. */
const CROWNED = {
  Root: { pos: [0, 0, HIP] }, Torso: [['x', -4]], Chest: [['x', -6]], ...look(0, -6),
  Arm_R: [['x', 50], ['y', -32]], Hand_R: [['x', 108], ['z', 30]], Arm_L: [['x', 50], ['y', 32]], Hand_L: [['x', 108], ['z', -30]],
  Thigh_R: [['y', -4]], Leg_R: [], Thigh_L: [['y', 4]], Leg_L: [],
};
/** The right palm thrust out flat, the body behind it, the left hand on the hip. */
const PALM_OUT = (reach = 1) => ({
  Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['z', -10]], Chest: [['x', -4], ['z', -8]], ...look(-14),
  Arm_R: [['x', 70 + 18 * reach], ['y', -6]], Hand_R: [['x', 30 - 30 * reach]], Arm_L: [['x', 14], ['y', 40]], Hand_L: [['x', 80]],
  Thigh_R: [['x', 22], ['y', -4]], Leg_R: [['x', -20]], Thigh_L: [['x', -18], ['y', 4]], Leg_L: [['x', -12]],
});
/** The horse stance: feet wide, knees deep, back straight, the fists at the hips. */
const HORSE = (sink = 1) => ({
  Root: { pos: [0, 0, HIP - 0.2 * sink] }, Torso: [], Chest: [['x', -4]], ...look(0, -2),
  Arm_R: [['x', -24], ['y', -18]], Hand_R: [['x', 96]], Arm_L: [['x', -24], ['y', 18]], Hand_L: [['x', 96]],
  Thigh_R: [['x', 50 * sink], ['y', -34]], Leg_R: [['x', -72 * sink]], Thigh_L: [['x', 50 * sink], ['y', 34]], Leg_L: [['x', -72 * sink]],
});
/** Bent over a body on the ground, the right palm laid on it (`press` deeper at each ring). */
const LAY = (press = 0) => ({
  Root: { pos: [0, 0, HIP - 0.28 - 0.03 * press] }, Torso: [['x', -40 - 4 * press]], Chest: [['x', -16]], ...look(0, 30),
  Arm_R: [['x', 70 + 8 * press], ['y', -6]], Hand_R: [['x', 10]], Arm_L: [['x', 46], ['y', 30]], Hand_L: [['x', 110], ['z', -30]],
  Thigh_R: [['x', 70]], Leg_R: [['x', -90]], Thigh_L: [['x', -10]], Leg_L: [['x', -60]],
});

const GAME = {
  gb_form: [
    { t: 0, pose: STAND },
    { t: 0.25, pose: PRAY(1) },
    { t: 0.6, pose: PRAY(1) },
    { t: 0.85, pose: CROWNED },
    { t: 1.1, pose: STAND },
  ],
  gb_rebound: [
    { t: 0, pose: STAND },
    { t: 0.1, pose: PALM_OUT(1) },
    { t: 1.0, pose: PALM_OUT(0.95) },
    { t: 1.25, pose: STAND },
  ],
  gb_rooted: [
    { t: 0, pose: STAND },
    { t: 0.2, pose: HORSE(1) },
    { t: 3.8, pose: HORSE(1) },
    { t: 4.05, pose: STAND },
  ],
  gb_finish: [
    { t: 0, pose: STAND },
    { t: 0.4, pose: LAY(0) },
    { t: 0.6, pose: LAY(1) },
    { t: 0.9, pose: LAY(0) },
    { t: 1.1, pose: LAY(1) },
    { t: 1.4, pose: LAY(0) },
    { t: 1.6, pose: LAY(2) },
    { t: 2.0, pose: LAY(0) },
    { t: 2.4, pose: PRAY(1) },
  ],
};

if (require.main === module) for (const [name, keys] of Object.entries(GAME)) writeSmooth(name, keys);

module.exports = { GAME };
