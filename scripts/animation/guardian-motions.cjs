// The Guardian of the Scarlet Threshold's own idle and walk, in place of Epic Fight's plain standing and walking (Enzo,
// 16/10/2026). A giant in heavy armour, four blocks tall: everything slow and weighted.
//   guardian_idle  six seconds that loop: feet wide, the knees bent under the weight, the arms held out from the body by
//                  the pauldrons, the fists low; deep slow breaths that lift the shoulders; it turns its head slowly to its
//                  left, rolls its right shoulder, opens its right hand and looks down into its palm (the blood it works),
//                  closes it again, and looks to its right;
//   guardian_walk  one stride in 1.6 seconds that loops (Epic Fight times it to its pace): heavy stomping steps, the
//                  whole body dropping on each heel and swaying over the planted foot, the arms swinging short against the
//                  steps, the head steady.
// Written as game files with quaternion turns between keys (pose.cjs writeSmooth), and on its own model (body and armour)
// as a Blockbench project to watch: build/blockbench/scarlet_guardian_animations.bbmodel (Animate tab).
// Usage: node scripts/animation/guardian-motions.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Head: x
// pitch (+ down), z yaw (+ left), y tilt. Arm_R y negative (Arm_L positive) holds the arm out from the body; Hand x bends
// the elbow forward; Thigh_R y negative (Thigh_L positive) spreads the legs.
const fs = require('fs');
const path = require('path');
const { groupsByName, animation } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');

const ROOT = path.join(__dirname, '..', '..');
/** The root's height when standing (the biped's hips), as in chief-axe.cjs. */
const HIP = 0.764;
/** Breathing: the chest rising (degrees back) at time t, `period` seconds a breath. */
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
const head = (yaw = 0, pitch = 0, tilt = 0) => ({ Head: [['x', pitch], ['y', tilt], ['z', yaw]] });

// ---------------------------------------------------------------- the idle

const LOOP = 6.0;
/** The stance: feet wide, knees bent, a forward hunch, the breath lifting the chest and the shoulders. */
const STANCE = (t, sway) => {
  const b = breath(t, 3.0, 2.5);
  return {
    Root: { pos: [0.01 * sway, 0, HIP - 0.035 - 0.004 * b] },
    Torso: [['x', 6], ['z', 2 * sway]], Chest: [['x', 3 + b]],
    Thigh_R: [['x', 10], ['y', -9]], Leg_R: [['x', -14]], Thigh_L: [['x', 10], ['y', 9]], Leg_L: [['x', -14]],
  };
};
/** The arms out from the pauldrons, the fists low; `shrug` lifts both with the breath. */
const ARMS = (t, { roll = 0, palm = 0 } = {}) => {
  const lift = -breath(t, 3.0, 1.5);
  return {
    Arm_R: [['x', 6 + 30 * palm + roll], ['y', -16 - lift - 6 * palm]], Hand_R: [['x', 22 + 62 * palm]],
    Arm_L: [['x', 6], ['y', 16 + lift]], Hand_L: [['x', 22]],
  };
};
const idleKey = (t, { sway = 0, yaw = 0, pitch = 6, tilt = 0, roll = 0, palm = 0 } = {}) =>
  ({ t, pose: { ...STANCE(t, sway), ...ARMS(t, { roll, palm }), ...head(yaw, pitch, tilt) } });

const guardian_idle = [
  idleKey(0),
  idleKey(1.2, { sway: 0.6, yaw: 22, pitch: 4 }),            // a slow look to its left
  idleKey(1.9, { sway: 0.4, yaw: 8, roll: -14, tilt: -4 }),  // the right shoulder rolled back...
  idleKey(2.4, { sway: 0.2, yaw: 0, roll: 10, tilt: 3 }),    // ...and forward
  idleKey(3.1, { sway: 0, yaw: -6, pitch: 24, palm: 1 }),    // the right hand opened, looked into
  idleKey(3.8, { sway: 0, yaw: -8, pitch: 26, palm: 1, tilt: 4 }),
  idleKey(4.4, { sway: -0.4, yaw: -4, pitch: 10, palm: 0.1 }),
  idleKey(5.2, { sway: -0.6, yaw: -20, pitch: 4 }),         // a look to its right
  idleKey(LOOP),
];

// ---------------------------------------------------------------- the walk

/** One leg's swing: `a` the thigh forward (degrees), `knee` its bend. */
const leg = (side, a, knee, out) => ({ ['Thigh_' + side]: [['x', a], ['y', out]], ['Leg_' + side]: [['x', -knee]] });
/**
 * A moment of the stride: the right leg's swing and knee, the left's, the body's drop (blocks), its sway over the planted
 * foot (+ right), the shoulders' turn.
 */
const walkKey = (t, r, rk, l, lk, drop, sway, turn) => ({ t, pose: {
  Root: { pos: [0.03 * sway, 0, HIP - 0.04 - drop] },
  Torso: [['x', 7], ['z', turn], ['y', 4 * sway]], Chest: [['x', 2], ['z', -turn * 0.5]],
  ...leg('R', r, rk, -6), ...leg('L', l, lk, 6),
  // The arms swing short against the legs, held out by the pauldrons, the elbows bent.
  Arm_R: [['x', -l * 0.45], ['y', -15]], Hand_R: [['x', 24 + Math.max(0, -l) * 0.3]],
  Arm_L: [['x', -r * 0.45], ['y', 15]], Hand_L: [['x', 24 + Math.max(0, -r) * 0.3]],
  ...head(-turn * 0.5, 6, -sway * 3),
} });
const STRIDE = 1.6;
const guardian_walk = [
  walkKey(0, 24, 6, -18, 12, 0.05, 0.6, -4),      // the right heel slams down, the body dropping onto it
  walkKey(0.2, 14, 14, -14, 16, 0.07, 1, -3),     // the weight settles over the right foot
  walkKey(0.45, 0, 10, 10, 50, 0.01, 0.8, 0),     // passing: the left leg comes through, lifted high
  walkKey(0.8, -18, 12, 24, 6, 0.05, -0.6, 4),    // the left heel slams down
  walkKey(1.0, -14, 16, 14, 14, 0.07, -1, 3),
  walkKey(1.25, 10, 50, 0, 10, 0.01, -0.8, 0),    // passing: the right leg comes through
  walkKey(STRIDE, 24, 6, -18, 12, 0.05, 0.6, -4),
];

const anims = { guardian_idle, guardian_walk };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  // On its own model, body and armour together, from the keys.
  const kit = require('./mesh-kit.cjs'), g = require('./scarlet-guardian-3d.cjs');
  const pieces = [...g.BODY_PIECES.map(p => ({ ...p })), ...g.ARMOUR_PIECES.map(p => ({ ...p, name: 'a_' + p.name }))];
  const size = 512, faces = kit.pack(pieces, size);
  const look = { key: 'guardian', c: { ...g.ARMOUR.c, ...g.BODY.c } };
  const project = kit.bbmodel('scarlet_guardian_animations', pieces, [look], [kit.png(size, size, kit.paintAtlas(faces, look, size))], size, 'entity/scarlet_guardian');
  const groups = groupsByName(project.outliner);
  project.animations = Object.entries(anims).map(([name, keys]) => animation(name, keys, groups, 'loop'));
  const out = path.join(ROOT, 'build', 'blockbench', 'scarlet_guardian_animations.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project));
  console.log('wrote', path.relative(ROOT, out));
}
