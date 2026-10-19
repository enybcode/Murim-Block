// The Raven Master's own idle and walk (docs/mobs/raven-master.md), in place of Epic Fight's plain standing and walking:
//   raven_master_idle  four seconds that loop: the watcher on his perch, quite still, the iron fan open before his chest
//                      (the scholar's stance of the fan, sect-weapons.cjs FAN_GUARD, raised a little toward his chin),
//                      the left hand behind his back; slow breaths, and now and then his head cocks sharply to one side
//                      and holds there, as a bird's does, then to the other;
//   raven_master_walk  one stride in 1.1 seconds that loops (Epic Fight times it to his pace): long quiet steps, the fan
//                      still before his chest, the left hand behind his back, the shoulders hardly turning.
// The mouth moves by texture under his mask (client/MobFaces). Written as game files with quaternion turns between keys
// (pose.cjs writeSmooth).
// Usage: node scripts/animation/raven-master-motions.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Head: x
// pitch (+ down), z yaw (+ left), y tilt.
const { writeSmooth } = require('./pose.cjs');
const { poses } = require('./sect-weapons.cjs');

/** The root's height when standing (the biped's hips), as in chief-axe.cjs. */
const HIP = poses.HIP;
/** Breathing: the chest rising (degrees back) at time t, `period` seconds a breath. */
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
const head = (yaw = 0, pitch = 0, tilt = 0) => ({ Head: [['x', pitch], ['y', tilt], ['z', yaw]] });

/** The open fan before his chest, a little higher than the fan's own stance: its rim under his chin. */
const FAN = { Arm_R: [['x', 48], ['z', 16]], Hand_R: [['x', 98]], Tool_R: [['x', -10], ['y', 70]] };
/**
 * The left hand behind his back, the forearm across the small of it (as Sensei Yun's, sensei-motions.cjs): the arm
 * first turned on itself so the elbow bends toward his spine, then swung back.
 */
const HAND_BEHIND = { Arm_L: [['z', -78], ['x', -28]], Hand_L: [['x', 100]] };

// ---------------------------------------------------------------- the idle

/** Standing watch: upright, the feet together, the knees soft, slow breaths. */
const STAND = (t) => ({
  Root: { pos: [0, 0, HIP - 0.01] },
  Torso: [['x', -1]], Chest: [['x', -1 + breath(t, 3.0, 1.4)]],
  Thigh_R: [['x', 2], ['y', -3]], Leg_R: [['x', -3]], Thigh_L: [['x', 2], ['y', 3]], Leg_L: [['x', -3]],
});
const idleKey = (t, yaw = 0, pitch = 3, tilt = 0) => ({ t, pose: { ...STAND(t), ...FAN, ...HAND_BEHIND, ...head(yaw, pitch, tilt) } });

const LOOP = 4.0;
const raven_master_idle = [
  idleKey(0),
  idleKey(0.7),
  // A sharp cock of the head to his right, held.
  idleKey(0.82, 4, 2, 9),
  idleKey(1.6, 5, 2, 9),
  idleKey(1.75, 0, 4, 0),
  idleKey(2.5, 0, 4, 0),
  // To his left, a little lower, held.
  idleKey(2.62, -6, 6, -8),
  idleKey(3.5, -6, 6, -8),
  idleKey(3.65, 0, 3, 0),
  idleKey(LOOP),
];

// ---------------------------------------------------------------- the walk

/** One leg's swing: `a` the thigh forward (degrees), `knee` its bend. */
const leg = (side, a, knee) => ({ ['Thigh_' + side]: [['x', a]], ['Leg_' + side]: [['x', -knee]] });
/** A moment of the stride: the right leg's swing and knee, the left's, the rise, the shoulders' turn. */
const walkKey = (t, r, rk, l, lk, rise, turn) => ({ t, pose: {
  Root: { pos: [0, 0, HIP - 0.015 + rise] },
  Torso: [['x', 1], ['z', turn]], Chest: [['x', 0], ['z', -turn * 0.5]],
  ...leg('R', r, rk), ...leg('L', l, lk), ...FAN, ...HAND_BEHIND, ...head(-turn * 0.3, 3),
} });
const STRIDE = 1.1;
const raven_master_walk = [
  walkKey(0, 30, 4, -24, 12, 0, -2),       // the right heel down, the left leg behind
  walkKey(0.275, 4, 6, 8, 42, 0.018, 0),   // passing: the left leg comes through, bent
  walkKey(0.55, -24, 12, 30, 4, 0, 2),     // the left heel down
  walkKey(0.825, 8, 42, 4, 6, 0.018, 0),   // passing: the right leg comes through
  walkKey(STRIDE, 30, 4, -24, 12, 0, -2),
];

const anims = { raven_master_idle, raven_master_walk };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
}
