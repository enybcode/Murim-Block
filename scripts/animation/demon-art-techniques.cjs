// The Devouring Demon Art's technique bodies (docs/combat/styles/devouring-demon-art.md). The kagune does the striking
// (client/KaguneStrikes points it at the foe); the body only drives it:
//   demon_lash  0 Fury, standing (0.75 s): the shoulders hunch a little as the tentacles coil, the chest pushes forward a
//               touch as they shoot out (the player does almost nothing: the kagune strikes);
//   demon_bite  100 Fury, on all fours (0.85 s): the head draws back as a centipede rears, then thrusts as it bites.
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
// Enzo, 18/10/2026: the player must do almost nothing, the kagune strikes. So the body barely stirs: the shoulders
// hunch a little as the tentacles coil, the chest pushes forward a touch as they shoot out.
const LASH = [
  { t: 0, pose: lash({}) },
  { t: 0.15, pose: lash({ twist: 6, lean: 4, crouch: 3, openR: 4, openL: 4 }) },
  { t: 0.32, pose: lash({ twist: -5, lean: 7, head: -2, crouch: 4, openR: 8, openL: 8 }) },
  { t: 0.75, pose: lash({}) },
];

// The bite, on all fours: the head draws back a little as the centipede rears, then thrusts as it strikes.
const BITE = [
  { t: 0, pose: beast({}) },
  { t: 0.25, pose: beast({ head: -6, lean: 88 }) },
  { t: 0.42, pose: beast({ head: 8, lean: 93 }) },
  { t: 0.85, pose: beast({}) },
];
/** On all fours: set down so the lowest hand or foot is on the ground, and the lunge carried forward. */
const grounded = (keys, forward) => keys.map((k, i) => {
  const c = contacts(k.pose), low = Math.min(c.handR, c.handL, c.footR, c.footL);
  const p = k.pose.Root.pos;
  return { t: k.t, pose: { ...k.pose, Root: { ...k.pose.Root, pos: [p[0], p[1] + forward[i], p[2] - low] } } };
});
const anims = { demon_lash: LASH, demon_bite: grounded(BITE, [0, -0.02, 0.05, 0]) };
if (process.argv.includes('--check')) {
  for (const k of anims.demon_bite) console.log(k.t.toFixed(2), Object.entries(contacts(k.pose)).map(([j, z]) => `${j} ${z.toFixed(2)}`).join('  '));
} else {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
}
