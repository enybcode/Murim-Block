// The bandits' own idles, five seconds that loop, in place of Epic Fight's plain standing:
//   bandit_idle  weight on one leg, the left hand on the belt, the dao hanging ready; he breathes, glances left, lifts the
//                dao to look along its edge and turns it over, lowers it, glances right;
//   chief_idle   the great axe resting on his right shoulder in one hand, the left fist on his buckle, feet wide; big slow
//                breaths, a look to the left, a roll of the neck from side to side, the chin up.
// The faces move too, but by texture (MobFaces: blinks, glances, a mutter, a frown in combat): the biped has no bones in
// the face. Written as game files with quaternion turns between keys (pose.cjs writeSmooth), and as a Blockbench project
// to look at the keys (art/blockbench/mob_idles.bbmodel).
// Usage: node scripts/animation/mob-idles.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Head: x
// pitch (+ down), z yaw (+ left), y tilt.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');
const { hold, begin, HIP } = require('./chief-axe.cjs');

const LOOP = 5.0;
/** Breathing: the chest rising (degrees back) at time t, `period` seconds a breath. */
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
/** A head turned `yaw` (+ left), pitched `pitch` (+ down), tilted `tilt`. */
const head = (yaw = 0, pitch = 0, tilt = 0) => ({ Head: [['x', pitch], ['y', tilt], ['z', yaw]] });

// ---------------------------------------------------------------- the bandit

/** The stance: weight on the left leg, the right knee loose, the left hand on the belt. */
const BANDIT_BODY = (t, sway) => ({
  Root: { pos: [0.015 * sway, 0, HIP - 0.03] },
  Torso: [['z', 3 * sway], ['y', -2 * sway]], Chest: [['x', breath(t, 2.5, 2)]],
  Thigh_R: [['x', 8], ['y', -6]], Leg_R: [['x', -12]], Thigh_L: [['x', -2], ['y', 4]], Leg_L: [['x', -2]],
  Arm_L: [['x', 12], ['y', 42]], Hand_L: [['x', 82]],
});
/**
 * The dao hanging ready, or lifted out to the side to look along the edge (`look` 0 to 1) and turned over on itself
 * (`turn` degrees about the blade's own length: the roll comes first, then the tilt), clear of the face.
 */
const DAO = (look = 0, turn = 0) => ({
  Arm_R: [['x', 18 + 22 * look], ['y', -6 - 16 * look]], Hand_R: [['x', 30 + 50 * look]],
  Tool_R: [['y', turn], ['x', -20 - 40 * look]],
});
const bandit = (t, { sway = 0, yaw = 0, pitch = 0, tilt = 0, look = 0, turn = 0 } = {}) =>
  ({ t, pose: { ...BANDIT_BODY(t, sway), ...DAO(look, turn), ...head(yaw, pitch, tilt) } });

const bandit_idle = [
  bandit(0),
  bandit(0.8, { sway: 0.5, yaw: 18, pitch: -2 }),
  bandit(1.5, { sway: 1, yaw: 4 }),
  // The edge looked along, the blade turned over, put down again.
  bandit(2.1, { sway: 1, yaw: -16, pitch: 18, look: 1, turn: 20 }),
  bandit(2.7, { sway: 0.8, yaw: -18, pitch: 20, look: 1, turn: 110 }),
  bandit(3.3, { sway: 0.4, yaw: -6, pitch: 6, look: 0.2, turn: 10 }),
  bandit(4.2, { sway: -0.4, yaw: -15, pitch: -2 }),
  bandit(LOOP),
];

// ---------------------------------------------------------------- the chief

/** Feet wide, the left fist on the buckle, the trunk and the breathing. */
const CHIEF_BODY = (t, sway) => ({
  Root: { pos: [0.015 * sway, 0, HIP - 0.06] },
  Torso: [['z', 2 * sway]], Chest: [['x', breath(t, 2.5, 3)]],
  Thigh_R: [['x', 6], ['y', -12]], Leg_R: [['x', -8]], Thigh_L: [['x', -4], ['y', 12]], Leg_L: [['x', -6]],
  Arm_L: [['x', 22], ['y', 18], ['z', -30]], Hand_L: [['x', 95]],
});
// The axe on his right shoulder: the hand at the chest, the haft up and back over the shoulder, the blade behind.
begin();
const SHOULDERED = hold({ ...CHIEF_BODY(0, 0), ...head() }, [0.34, 0.2, 0.42], [0.05, -0.5, 0.86], [0, -1, 0],
  { Arm_R: [['x', 40], ['y', -20], ['z', 10]], Hand_R: [['x', 100]], Tool_R: [['x', -10], ['y', 0], ['z', 0]] }, { twoHands: false });
const ARM = { Arm_R: SHOULDERED.Arm_R, Hand_R: SHOULDERED.Hand_R, Tool_R: SHOULDERED.Tool_R };
const chief = (t, { sway = 0, yaw = 0, pitch = 0, tilt = 0 } = {}) =>
  ({ t, pose: { ...CHIEF_BODY(t, sway), ...ARM, ...head(yaw, pitch, tilt) } });

const chief_idle = [
  chief(0),
  chief(1.1, { sway: 0.6, yaw: 20, pitch: 2 }),
  chief(2.0, { sway: 1, yaw: 6 }),
  // The neck rolled from side to side.
  chief(2.7, { sway: 0.6, tilt: 16, pitch: 4 }),
  chief(3.3, { sway: 0, tilt: -16, pitch: 4 }),
  // The chin up, looking down on whoever is there.
  chief(4.1, { sway: -0.5, pitch: -9 }),
  chief(LOOP),
];

const anims = { bandit_idle, chief_idle };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'mob_idles.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('mob_idles', anims), null, 1));
  console.log('wrote', out);
}
