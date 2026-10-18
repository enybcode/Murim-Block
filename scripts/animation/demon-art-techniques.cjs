// The Devouring Demon Art's technique bodies (docs/combat/styles/devouring-demon-art.md). The kagune does the striking
// (client/KaguneStrikes points it at the foe); the body only drives it:
//   demon_lash  0 Fury, standing (0.7 s): the shoulders wind to the right, then the whole body whips to the left, the
//               arms flung open, as the four tentacles sweep the arc in front of him;
//   demon_bite  100 Fury, on all fours (0.8 s): the beast draws back on its haunches, then lunges, head first, as a
//               centipede shoots out and bites.
//
//   node scripts/animation/demon-art-techniques.cjs [--check]
// Writes animmodels/animations/biped/murim/demon_lash.json and demon_bite.json. --check prints the bite's contacts
// (on all fours, no hand nor foot may go under the ground).
const { writeSmooth } = require('./pose.cjs');
const { beast, contacts } = require('./demon-beast-gait.cjs');

// The lash: X is his right, Z up the spine; -Y raises the right arm sideways, +Y the left.
const lash = ({ twist = 0, lean = 0, armR = 0, armL = 0, openR = 0, openL = 0, head = 0, crouch = 0 }) => ({
  Root: { rot: [['z', twist * 0.35]] },
  Torso: [['x', lean], ['z', twist * 0.4]],
  Chest: [['x', lean * 0.5], ['z', twist * 0.25]],
  Head: [['x', -lean * 0.6 + head], ['z', -twist * 0.5]],
  Arm_R: [['x', armR], ['y', -openR]], Hand_R: [['x', 20]],
  Arm_L: [['x', armL], ['y', openL]], Hand_L: [['x', 20]],
  Thigh_R: [['x', crouch]], Leg_R: [['x', -crouch * 1.6]],
  Thigh_L: [['x', crouch]], Leg_L: [['x', -crouch * 1.6]],
});
const LASH = [
  { t: 0, pose: lash({}) },
  // Wound to the right, crouching, the arms gathered: the tentacles drawn back.
  { t: 0.18, pose: lash({ twist: 40, lean: 10, armR: -20, armL: 40, openR: 20, openL: 10, crouch: 14 }) },
  // The whip, to the left: everything flung open.
  { t: 0.3, pose: lash({ twist: -35, lean: 16, armR: 30, armL: -10, openR: 70, openL: 60, head: -6, crouch: 18 }) },
  { t: 0.42, pose: lash({ twist: -42, lean: 14, armR: 20, armL: -20, openR: 60, openL: 70, head: -4, crouch: 16 }) },
  { t: 0.7, pose: lash({}) },
];

// The bite, on all fours: back on the haunches (hips low, chest up), then the lunge (the body thrown forward, the
// head thrust out), then back to the crouch.
const BITE = [
  { t: 0, pose: beast({}) },
  { t: 0.22, pose: beast({ lean: 72, hip: 0.4, head: -10, armR: -14, armL: -14, thighR: 12, thighL: 12, kneeR: -14, kneeL: -14 }) },
  { t: 0.36, pose: beast({ lean: 96, hip: 0.5, head: 12, armR: 34, armL: 30, liftHandR: 18, liftHandL: 14, thighR: -24, thighL: -22 }) },
  { t: 0.5, pose: beast({ lean: 94, hip: 0.48, head: 8, armR: 26, armL: 22, thighR: -18, thighL: -16 }) },
  { t: 0.8, pose: beast({}) },
];
/** On all fours: set down so the lowest hand or foot is on the ground, and the lunge carried forward. */
const grounded = (keys, forward) => keys.map((k, i) => {
  const c = contacts(k.pose), low = Math.min(c.handR, c.handL, c.footR, c.footL);
  const p = k.pose.Root.pos;
  return { t: k.t, pose: { ...k.pose, Root: { ...k.pose.Root, pos: [p[0], p[1] + forward[i], p[2] - low] } } };
});
const anims = { demon_lash: LASH, demon_bite: grounded(BITE, [0, -0.15, 0.35, 0.3, 0]) };
if (process.argv.includes('--check')) {
  for (const k of anims.demon_bite) console.log(k.t.toFixed(2), Object.entries(contacts(k.pose)).map(([j, z]) => `${j} ${z.toFixed(2)}`).join('  '));
} else {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
}
