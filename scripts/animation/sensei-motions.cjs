// Sensei Yun's own idle and walk, in place of Epic Fight's plain standing and walking:
//   sensei_idle  five seconds that loop: an old master at rest, a little stooped, the left hand behind his back, the
//                Azure Cloud held low in the right, its point toward the ground; slow breaths, a look to the side; he
//                brings his hand up to stroke his beard twice, puts it back behind him, glances the other way and nods;
//   sensei_walk  one stride in 1.2 seconds that loops (Epic Fight times it to his pace): short measured steps, the knees
//                soft, a slight rise and fall and a turn of the shoulders, the hand still behind his back, the sword low
//                swinging a little against the steps.
// The face moves by texture (client/MobFaces). Written as game files with quaternion turns between keys (pose.cjs
// writeSmooth), and on his own model with his sword in hand as a Blockbench project to watch:
// build/blockbench/azure_sensei_animations.bbmodel (Animate tab).
// Usage: node scripts/animation/sensei-motions.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Head: x
// pitch (+ down), z yaw (+ left), y tilt.
const fs = require('fs');
const path = require('path');
const { groupsByName, animation } = require('./blockbench-rig.cjs');
const { writeSmooth, MURIM_BIPED } = require('./pose.cjs');
const { keysOf } = require('./epicfight-to-blockbench.cjs');

const ROOT = path.join(__dirname, '..', '..');
/** The root's height when standing (the biped's hips), as in chief-axe.cjs. */
const HIP = 0.764;
/** Breathing: the chest rising (degrees back) at time t, `period` seconds a breath. */
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
const head = (yaw = 0, pitch = 0, tilt = 0) => ({ Head: [['x', pitch], ['y', tilt], ['z', yaw]] });

/**
 * The left hand behind his back, the forearm across the small of it: the arm first turned on itself (z) so the elbow
 * bends toward his spine, then swung back (x).
 */
const HAND_BEHIND = { Arm_L: [['z', -78], ['x', -28]], Hand_L: [['x', 100]] };
/** The left hand at his beard: `low` 0 at the chin to 1 halfway down the beard. */
const STROKE = (low) => ({ Arm_L: [['x', 34 - 8 * low], ['y', -18], ['z', 8]], Hand_L: [['x', 118 - 26 * low]] });
/**
 * The sword held low in the right hand, its point toward the ground ahead (40 degrees below level); `swing` degrees
 * forward. Epic Fight holds a sword's grip about 1.5 pixels above the hand joint, toward the elbow, where his wide
 * sleeve covers it: lowered so, the pommel would sink into his forearm and the tassel with it (it may not swing
 * through the grip, TasselSwordRenderer). So in his own motions the sword sits lower and further back in the hand
 * (HOLD): the grip in his fist, the pommel just behind the sleeve, the tassel hanging free. checkHold() says where.
 */
const SWORD = (swing = 0) => ({ Arm_R: [['x', 4 + swing], ['y', -5]], Hand_R: [['x', 6]], Tool_R: [['x', -25]] });
/** Where the sword is moved in his hand: pixels to his right, front and up, in the hand's own frame. */
const HOLD = [0, -1.2, -2.5];

// ---------------------------------------------------------------- the idle

/** Standing at rest: feet a little apart, the knees soft, a slight stoop, slow breaths. */
const STAND = (t, sway) => ({
  Root: { pos: [0.012 * sway, 0, HIP - 0.012] },
  Torso: [['x', 4], ['z', 2 * sway]], Chest: [['x', 3 + breath(t, 3.2, 1.6)]],
  Thigh_R: [['x', 2], ['y', -4]], Leg_R: [['x', -3]], Thigh_L: [['x', 2], ['y', 4]], Leg_L: [['x', -3]],
});
const idleKey = (t, { sway = 0, yaw = 0, pitch = 4, tilt = 0, hand = HAND_BEHIND } = {}) =>
  ({ t, pose: { ...STAND(t, sway), ...SWORD(), ...hand, ...head(yaw, pitch, tilt) } });

const LOOP = 5.0;
const sensei_idle = [
  idleKey(0),
  idleKey(0.9, { sway: 0.5, yaw: 12, pitch: 2 }),
  // The beard stroked twice, the head bowed a little in thought.
  idleKey(1.5, { sway: 0.8, yaw: 4, pitch: 8, hand: STROKE(0) }),
  idleKey(2.0, { sway: 0.8, yaw: 2, pitch: 10, hand: STROKE(1) }),
  idleKey(2.5, { sway: 0.6, yaw: 2, pitch: 8, hand: STROKE(0.1) }),
  idleKey(3.0, { sway: 0.4, yaw: 0, pitch: 10, tilt: 3, hand: STROKE(1) }),
  // The hand back behind him, a glance the other way, a nod.
  idleKey(3.6, { sway: 0, yaw: -14, pitch: 2 }),
  idleKey(4.3, { sway: -0.3, yaw: -4, pitch: 12 }),
  idleKey(LOOP),
];

// ---------------------------------------------------------------- the walk

/** One leg's swing: `a` the thigh forward (degrees), `knee` its bend. */
const leg = (side, a, knee) => ({ ['Thigh_' + side]: [['x', a]], ['Leg_' + side]: [['x', -knee]] });
/** A moment of the stride: the right leg's swing and knee, the left's, the rise, the shoulders' turn. */
const walkKey = (t, r, rk, l, lk, rise, turn) => ({ t, pose: {
  Root: { pos: [0, 0, HIP - 0.02 + rise] },
  Torso: [['x', 2], ['z', turn]], Chest: [['x', 1], ['z', -turn * 0.6]],
  ...leg('R', r, rk), ...leg('L', l, lk), ...HAND_BEHIND, ...SWORD(-turn * 1.6), ...head(-turn * 0.4, 5),
} });
const STRIDE = 1.2;
const sensei_walk = [
  walkKey(0, 26, 4, -20, 14, 0, -3),      // the right heel down, the left leg behind
  walkKey(0.3, 2, 6, 8, 44, 0.02, 0),     // passing: the left leg comes through, bent
  walkKey(0.6, -20, 14, 26, 4, 0, 3),     // the left heel down
  walkKey(0.9, 8, 44, 2, 6, 0.02, 0),     // passing: the right leg comes through
  walkKey(STRIDE, 26, 4, -20, 14, 0, -3),
];

const anims = { sensei_idle, sensei_walk };

// ---------------------------------------------------------------- the sword in his fist

const b = require('./biped.cjs');
const { arm, bind, locals } = require('./pose.cjs');
const rotOnly = m => [m[0], m[1], m[2], 0, m[4], m[5], m[6], 0, m[8], m[9], m[10], 0, 0, 0, 0, 1];
const apply = (m, [x, y, z]) => [0, 1, 2].map(r => m[r * 4] * x + m[r * 4 + 1] * y + m[r * 4 + 2] * z + m[r * 4 + 3]);
/** HOLD in Hand_R's own frame (blocks; x right, y front, z up as the hand stands at rest), Tool_R's parent. */
const HOLD_LOCAL = apply(b.invRigid(rotOnly(bind.Hand_R)), HOLD.map(v => v / 16));

/** Moves the sword in his hand in a written animation: Tool_R's place, every frame. */
function holdInFist(name) {
  const file = path.join(MURIM_BIPED, name + '.json');
  const json = JSON.parse(fs.readFileSync(file, 'utf8'));
  const tool = json.animation.find(j => j.name === 'Tool_R');
  for (const m of tool.transform) [3, 7, 11].forEach((n, i) => { m[n] = Math.round((m[n] + HOLD_LOCAL[i]) * 1e6) / 1e6; });
  fs.writeFileSync(file, JSON.stringify(json));
}

/**
 * Where the Azure Cloud's pommel, grip and guard fall in his hand for a pose, through Epic Fight's own chain (the
 * held-item correction on Tool_R, then the vanilla sword's hand transforms of azure_cloud_sword_held.json), in the hand's
 * rest frame in Blockbench pixels (x right, y up, z back), against his fist (y 10.6 to 13.2, z within 1.7) and his
 * sleeve (from y 12.2, z within 2.7; sensei-3d.cjs).
 */
function checkHold(label, pose) {
  const T = (x, y, z) => b.translation(x, y, z), S = s => [s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1];
  const R = (axis, deg) => b.rotation({ x: [1, 0, 0], y: [0, 1, 0], z: [0, 0, 1] }[axis], deg);
  const item = [T(0, 0, -0.13), R('x', -90), T(0, 4 / 16, 0.5 / 16), R('y', -90), R('z', 55), S(0.85), T(-0.5, -0.5, -0.5)].reduce(b.mul);
  const L = locals(pose);
  L.Tool_R = b.mul(T(...HOLD_LOCAL), L.Tool_R);
  const M = b.modelSpace(arm, L);
  const toHand = b.mul(bind.Hand_R, b.invRigid(M.Hand_R));
  // A point on the sword's axis at height y of the upright model, turned onto the diagonal as the item model is.
  const at = y => { const d = (8 + (y - 8) * Math.SQRT1_2) / 16, p = apply(toHand, apply(b.mul(M.Tool_R, item), [d, d, 0.5])); return [p[0] * 16, p[2] * 16, -p[1] * 16]; };
  const f = p => p.slice(1).map(v => v.toFixed(1)).join(', ');
  const pommel = at(-3.6);
  console.log(`  ${label}: pommel (y, z) ${f(pommel)}, ${(pommel[2] - 2.7).toFixed(1)} px behind the sleeve; grip ${f(at(0.8))}; guard ${f(at(3.4))}`);
}

module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) {
    writeSmooth(name, keys);
    holdInFist(name);
  }
  // The same in every frame of both (the hand and the sword keep their angle; the arm swings).
  checkHold('the sword in his fist', sensei_idle[0].pose);
  // On his own model, his sword in his hand: the game files as written, read back.
  const { holdingProject } = require('../concepts/sensei-preview.cjs');
  const project = holdingProject();
  const groups = groupsByName(project.outliner);
  project.animations = Object.keys(anims).map(name =>
    animation(name, keysOf(JSON.parse(fs.readFileSync(path.join(MURIM_BIPED, name + '.json'), 'utf8'))), groups, 'loop'));
  const out = path.join(ROOT, 'build', 'blockbench', 'azure_sensei_animations.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project));
  console.log('wrote', path.relative(ROOT, out));
}
