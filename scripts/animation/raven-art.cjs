// The Raven Messenger Art's moves (docs/combat/styles/raven-art.md), the iron fan in the right hand, built on the fan's
// own poses (sect-weapons.cjs: the scholar's stance, the sweeps, the fan over the head, the lunge):
//   raven_gate        Raven Gate: the fan raised, then snapped down to point at the ground under the target (the door opens);
//   halls_step        Halls Step: the fan drawn, then swept across the body (one goes through as it passes);
//   raven_exchange    Exchange: a flick right to left, then the fan's edge pointed at the target (the places change);
//   swallowing_gate   Swallowing Gate: the fan opened wide before the body, the left hand under it, braced;
//   closed_room       Closed Room: the fan raised, then shut with a clap into the left palm before the chest;
//   halls_unfold      The Halls Unfold: arms spread, a whole turn with the fan high, raised to the sky, then snapped down;
//   last_message      the finish: the fan opened over the fallen, held while they sink, then shut with a snap.
// Usage: node scripts/animation/raven-art.cjs
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');
const { HIP, look, LEGS_R, LEGS_L, LEGS_SOFT, SWEEP, FAN_GUARD, FAN_LEFT, FAN_OVER, FAN_DRAWN, FAN_LUNGE } = require('./sect-weapons.cjs').poses;

/** The fan pointed down ahead at the ground `ahead` blocks off: the arm out and low, the trunk leaning over it. */
const POINT_DOWN = (y = 0, lean = 14) => ({
  Root: { pos: [0, y, HIP - 0.08] },
  Torso: [['x', -lean], ['z', 6]], Chest: [['x', -8]], ...look(6, 24),
  Arm_R: [['x', 58], ['z', -6]], Hand_R: [['x', -30]], Tool_R: [['x', -92]],
  ...FAN_LEFT, ...LEGS_R,
});
/** The fan opened wide before the body, its face out, the left hand under the right wrist, the legs braced. */
const SHIELD = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.1] },
  Torso: [['x', -4]], Chest: [['x', -4]], ...look(0, 4),
  Arm_R: [['x', 84], ['z', -8]], Hand_R: [['x', 90]], Tool_R: [['x', -10], ['y', 70]],
  Arm_L: [['x', 74], ['z', 22]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 26], ['y', -12]], Leg_R: [['x', -30]], Thigh_L: [['x', -18], ['y', 12]], Leg_L: [['x', -18]],
});
/** The fan shut into the left palm before the chest: both hands meet. */
const CLAP = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.06] },
  Torso: [['x', -6]], Chest: [['x', -6]], ...look(0, 8),
  Arm_R: [['x', 70], ['z', -26]], Hand_R: [['x', 10]], Tool_R: [['x', -92]],
  Arm_L: [['x', 66], ['z', 30]], Hand_L: [['x', 14]],
  ...LEGS_SOFT,
});
/** Both arms spread wide, the fan open at the end of the right, the trunk turned `turn` (a spin). */
const SPREAD = (turn = 0, y = 0) => ({
  Root: { pos: [0, y, HIP], rot: [['z', turn]] },
  Torso: [['x', 6]], Chest: [['x', 4]], ...look(0, -12),
  Arm_R: [['x', 30], ['z', 84]], Hand_R: [['x', 10]], Tool_R: [['y', 90], ['x', -80]],
  Arm_L: [['x', 30], ['z', -84]], Hand_L: [['x', 10]],
  ...LEGS_SOFT,
});
/** The fan raised straight to the sky, the face turned up. */
const TO_SKY = (y = 0) => ({
  Root: { pos: [0, y, HIP + 0.04] },
  Torso: [['x', 10]], Chest: [['x', 8]], ...look(0, -40),
  Arm_R: [['x', 176], ['z', 4]], Hand_R: [['x', 4]], Tool_R: [['y', 90], ['x', -80]],
  Arm_L: [['x', 10], ['z', -30]], Hand_L: [['x', 20]],
  ...LEGS_SOFT,
});

// ---------------------------------------------------------------- the techniques

const raven_gate = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.14, pose: FAN_OVER(-20) },
  { t: 0.26, pose: POINT_DOWN(0.05) },
  { t: 0.5, pose: POINT_DOWN(0.05, 16) },
  { t: 0.75, pose: FAN_GUARD(0.05) },
];
const halls_step = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.1, pose: FAN_DRAWN },
  { t: 0.2, pose: SWEEP(85, 34, { y: 0.1, z: HIP - 0.09, left: FAN_LEFT }) },
  { t: 0.3, pose: SWEEP(92, 36, { y: 0.1, z: HIP - 0.09, pitch: 82, left: FAN_LEFT }) },
  { t: 0.45, pose: FAN_GUARD(0.1) },
];
const raven_exchange = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.1, pose: SWEEP(-80, -30, { pitch: 80, hand: 30, left: FAN_LEFT }) },
  { t: 0.2, pose: SWEEP(60, 24, { y: 0.06, left: FAN_LEFT }) },
  { t: 0.3, pose: FAN_LUNGE(0.2) },
  { t: 0.42, pose: FAN_LUNGE(0.22) },
  { t: 0.6, pose: FAN_GUARD(0.22) },
];
const swallowing_gate = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.12, pose: SWEEP(-60, -20, { pitch: 90, hand: 40, left: FAN_LEFT, legs: LEGS_L }) },
  { t: 0.22, pose: SHIELD(-0.02) },
  { t: 0.42, pose: SHIELD(-0.04) },
  { t: 0.6, pose: FAN_GUARD(-0.02) },
];
const closed_room = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.2, pose: FAN_OVER(-30) },
  { t: 0.3, pose: FAN_OVER(-10, 0.02) },
  { t: 0.42, pose: CLAP(0.04) },
  { t: 0.6, pose: CLAP(0.04) },
  { t: 0.8, pose: FAN_GUARD(0.04) },
];

// ---------------------------------------------------------------- the ultimate

const halls_unfold = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.35, pose: SPREAD(0) },
  { t: 0.6, pose: SPREAD(90) },
  { t: 0.85, pose: SPREAD(180) },
  { t: 1.1, pose: SPREAD(270) },
  { t: 1.35, pose: SPREAD(360) },
  { t: 1.6, pose: { ...TO_SKY(), Root: { pos: [0, 0, HIP + 0.04], rot: [['z', 360]] } } },
  { t: 1.75, pose: { ...TO_SKY(), Root: { pos: [0, 0, HIP + 0.06], rot: [['z', 360]] } } },
  { t: 1.9, pose: { ...POINT_DOWN(0, 22), Root: { pos: [0, 0, HIP - 0.12], rot: [['z', 360]] } } },
  { t: 2.2, pose: { ...POINT_DOWN(0, 22), Root: { pos: [0, 0, HIP - 0.12], rot: [['z', 360]] } } },
  { t: 2.6, pose: { ...FAN_GUARD(), Root: { pos: [0, 0, HIP - 0.02], rot: [['z', 360]] } } },
];

// ---------------------------------------------------------------- the finish

const last_message = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.3, pose: FAN_OVER(0) },
  { t: 0.45, pose: POINT_DOWN(0, 18) },
  { t: 1.5, pose: POINT_DOWN(0, 20) },
  { t: 1.62, pose: FAN_OVER(-20, 0.02) },
  { t: 1.72, pose: CLAP(0.02) },
  { t: 2.0, pose: CLAP(0.02) },
  { t: 2.4, pose: FAN_GUARD(0.02) },
];

const anims = { raven_gate, halls_step, raven_exchange, swallowing_gate, closed_room, halls_unfold, last_message };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'raven_art.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('raven_art', anims), null, 1));
  console.log('wrote', out);
}
