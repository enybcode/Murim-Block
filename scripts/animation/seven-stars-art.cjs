// The Seven Stars Sword's moves for the player (docs/combat/styles/seven-stars-sword.md), a florett in the right hand.
// Written as Epic Fight animations biped/murim/sss_* (pose.cjs writeSmooth). The damage is the server's
// (martial/SevenStarsSword), on the ticks these clips reach their blow; the body is moved by the server too (the comet's
// streak, the steps round the target), so no clip moves its root forward.
//   sss_shooting_star   Shooting Star: crouched, the blade low behind; the body gone in a streak at 0.15 s, the lunge
//                       held where it comes out, then back on guard;
//   sss_dipper_thrust   one bound of Dipper Steps: a quick draw and a thrust (played again at each bound);
//   sss_orbit           Orbiting Stars: the blade raised upright before the face, the left hand along it;
//   sss_link            Constellation Link: the blade pointed at the sky, held, then brought down sharply;
//   sss_rain            Falling Star Rain: both hands on the hilt above the head, the point to the sky, then down
//                       toward the circle;
//   sss_polaris         Polaris Riposte (the secret): a short straight thrust to the heart;
//   sss_array           Seven Stars Sword Array (the ultimate): down on one knee, the florett planted point first;
//   sss_finish          Extinguished Stars (the finish): seven quick thrusts down at the body, then the blade drawn
//                       back to the hip.
// Usage: node scripts/animation/seven-stars-art.cjs
// Pose notation: see azure-m1.cjs (Arm x forward and up, Hand x bends the elbow, Tool_R x -92 lays the blade along the
// forearm).
const { writeSmooth } = require('./pose.cjs');

const HIP = 0.764;
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });

/** On guard: right foot ahead, the florett level and forward at chest height, the left hand up behind, as a fencer. */
const GUARD = {
  Root: { pos: [0, 0, HIP - 0.04] },
  Torso: [['z', -20]], Chest: [['x', -4], ['z', -8]], ...look(-24),
  Arm_R: [['x', 40], ['y', -6]], Hand_R: [['x', 38]], Tool_R: [['x', -80]],
  Arm_L: [['x', 20], ['y', 70]], Hand_L: [['x', 90]],
  Thigh_R: [['x', 24], ['y', -4]], Leg_R: [['x', -18]], Thigh_L: [['x', -16], ['y', 6]], Leg_L: [['x', -14]],
};
/** Drawn back: the elbow behind, the blade level along the forearm, ready to go. */
const DRAWN = (low = 0) => ({
  Root: { pos: [0, 0, HIP - 0.1 - low] },
  Torso: [['x', -6 - low * 60], ['z', -36]], Chest: [['x', -6], ['z', -12]], ...look(-46, low * 40),
  Arm_R: [['x', -30], ['y', -12]], Hand_R: [['x', 118]], Tool_R: [['x', -92]],
  Arm_L: [['x', 70], ['z', -16]], Hand_L: [['x', 8]],
  Thigh_R: [['x', 26 + low * 80], ['y', -4]], Leg_R: [['x', -36 - low * 80]], Thigh_L: [['x', -16], ['y', 4]], Leg_L: [['x', -40 - low * 40]],
});
/** The thrust: arm and blade one straight line ahead, the front knee deep, the left arm thrown back for balance. */
const LUNGE = (depth = 1) => ({
  Root: { pos: [0, 0, HIP - 0.08 - 0.1 * depth] },
  Torso: [['x', -6], ['z', 8]], Chest: [['x', -10], ['z', 6]], ...look(14, 6),
  Arm_R: [['x', 88], ['z', -4]], Hand_R: [['x', 0]], Tool_R: [['x', -92]],
  Arm_L: [['x', -40], ['y', 50]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 30 + 40 * depth]], Leg_R: [['x', -30 - 44 * depth]], Thigh_L: [['x', -20 - 20 * depth]], Leg_L: [['x', -6]],
});
/** The blade upright before the face, the left hand's fingers along it (Orbiting Stars). */
const UPRIGHT = {
  Root: { pos: [0, 0, HIP - 0.03] },
  Torso: [['z', -6]], Chest: [['x', -2]], ...look(-6, -4),
  Arm_R: [['x', 46], ['y', -4]], Hand_R: [['x', 104]], Tool_R: [['x', -92]],
  Arm_L: [['x', 50], ['y', 20]], Hand_L: [['x', 100]],
  Thigh_R: [['x', 6], ['y', -6]], Leg_R: [['x', -8]], Thigh_L: [['x', -4], ['y', 6]], Leg_L: [['x', -8]],
};
/** The point to the sky, the arm straight up, the body stretched (Constellation Link). */
const SKY = {
  Root: { pos: [0, 0, HIP + 0.02] },
  Torso: [['x', 6]], Chest: [['x', 6]], ...look(0, -24),
  Arm_R: [['x', 172], ['z', 6]], Hand_R: [['x', 4]], Tool_R: [['x', -92]],
  Arm_L: [['x', 10], ['y', 60]], Hand_L: [['x', 70]],
  Thigh_R: [['x', 6], ['y', -6]], Leg_R: [['x', -4]], Thigh_L: [['x', -6], ['y', 6]], Leg_L: [['x', -4]],
};
/** Brought down sharply: the arm forward and down, the blade pointing at the ground ahead. */
const DOWN = {
  Root: { pos: [0, 0, HIP - 0.1] },
  Torso: [['x', -16]], Chest: [['x', -8]], ...look(0, 18),
  Arm_R: [['x', 56], ['z', -4]], Hand_R: [['x', 4]], Tool_R: [['x', -70]],
  Arm_L: [['x', -30], ['y', 50]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 40]], Leg_R: [['x', -46]], Thigh_L: [['x', -20]], Leg_L: [['x', -30]],
};
/** Both hands on the hilt above the head, the point to the sky (Falling Star Rain). */
const RAISED = {
  Root: { pos: [0, 0, HIP + 0.02] },
  Torso: [['x', 6]], Chest: [['x', 8]], ...look(0, -20),
  Arm_R: [['x', 166], ['z', 10]], Hand_R: [['x', 14]], Tool_R: [['x', -96]],
  Arm_L: [['x', 160], ['z', -16]], Hand_L: [['x', 28]],
  Thigh_R: [['x', 14]], Leg_R: [['x', -10]], Thigh_L: [['x', -16]], Leg_L: [['x', -8]],
};
/** Down on the right knee, both hands on the pommel, the florett planted point first before the knee. */
const KNEEL = {
  Root: { pos: [0, 0, HIP - 0.34] },
  Torso: [['x', -10]], Chest: [['x', -6]], ...look(0, 10),
  Arm_R: [['x', 40]], Hand_R: [['x', 20]], Tool_R: [['x', -170]],
  Arm_L: [['x', 44], ['z', -20]], Hand_L: [['x', 40]],
  Thigh_R: [['x', -10]], Leg_R: [['x', -100]], Thigh_L: [['x', 80]], Leg_L: [['x', -84]],
};
/** Over a body on the ground: bent, the point thrust down (`side` -1 to 1 moves it along the body). */
const STAB = (down, side = 0) => ({
  Root: { pos: [0, 0, HIP - 0.12] },
  Torso: [['x', -30], ['z', side * 6]], Chest: [['x', -14]], ...look(side * 8, 30),
  Arm_R: [['x', down ? 40 : 90], ['z', -side * 10]], Hand_R: [['x', down ? 0 : 60]], Tool_R: [['x', down ? -40 : -92]],
  Arm_L: [['x', -20], ['y', 50]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 40]], Leg_R: [['x', -50]], Thigh_L: [['x', -14]], Leg_L: [['x', -30]],
});
/** The blade drawn back to the left hip, as if sheathed, the head raised. */
const SHEATHED = {
  Root: { pos: [0, 0, HIP - 0.03] },
  Torso: [['z', 10]], Chest: [['x', -2]], ...look(0, -6),
  Arm_R: [['x', 30], ['z', -30]], Hand_R: [['x', 90]], Tool_R: [['x', -150]],
  Arm_L: [['x', 10], ['z', -10]], Hand_L: [['x', 70]],
  Thigh_R: [['x', 4], ['y', -4]], Leg_R: [['x', -4]], Thigh_L: [['x', -4], ['y', 4]], Leg_L: [['x', -4]],
};

const GAME = {
  sss_shooting_star: [
    { t: 0, pose: GUARD },
    { t: 0.12, pose: DRAWN(0.4) },
    { t: 0.16, pose: LUNGE(1) },
    { t: 0.38, pose: LUNGE(1) },
    { t: 0.6, pose: GUARD },
  ],
  sss_dipper_thrust: [
    { t: 0, pose: DRAWN(0.1) },
    { t: 0.08, pose: LUNGE(0.7) },
    { t: 0.18, pose: LUNGE(0.7) },
    { t: 0.3, pose: GUARD },
  ],
  sss_orbit: [
    { t: 0, pose: GUARD },
    { t: 0.2, pose: UPRIGHT },
    { t: 0.45, pose: UPRIGHT },
    { t: 0.65, pose: GUARD },
  ],
  sss_link: [
    { t: 0, pose: GUARD },
    { t: 0.25, pose: SKY },
    { t: 0.55, pose: SKY },
    { t: 0.62, pose: DOWN },
    { t: 0.85, pose: DOWN },
    { t: 1.05, pose: GUARD },
  ],
  sss_rain: [
    { t: 0, pose: GUARD },
    { t: 0.3, pose: RAISED },
    { t: 0.6, pose: RAISED },
    { t: 0.7, pose: DOWN },
    { t: 1.6, pose: DOWN },
    { t: 1.85, pose: GUARD },
  ],
  sss_polaris: [
    { t: 0, pose: GUARD },
    { t: 0.06, pose: LUNGE(0.9) },
    { t: 0.22, pose: LUNGE(0.9) },
    { t: 0.4, pose: GUARD },
  ],
  sss_array: [
    { t: 0, pose: GUARD },
    { t: 0.25, pose: RAISED },
    { t: 0.45, pose: KNEEL },
    { t: 2.4, pose: KNEEL },
    { t: 2.8, pose: GUARD },
  ],
  sss_finish: [
    { t: 0, pose: GUARD },
    { t: 0.25, pose: STAB(false, 0) },
    ...[0, 1, 2, 3, 4, 5, 6].flatMap(i => {
      const t = 0.3 + i * 0.14, side = [-0.8, 0.4, -0.2, 0.8, -0.5, 0.2, 0][i];
      return [{ t: t + 0.05, pose: STAB(true, side) }, { t: t + 0.12, pose: STAB(false, side) }];
    }),
    { t: 1.5, pose: STAB(false, 0) },
    { t: 1.9, pose: SHEATHED },
    { t: 2.5, pose: SHEATHED },
  ],
};

if (require.main === module) for (const [name, keys] of Object.entries(GAME)) writeSmooth(name, keys);

module.exports = { GAME, GUARD, LUNGE };
