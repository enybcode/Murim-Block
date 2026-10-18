// The Overlord Spear's moves (docs/combat/styles/overlord-spear.md), the spear held in both hands, built on the Celestial
// Peak Spear's poses (celestial-spear.cjs: the same hold, both arms solved for where the spear points):
//   overlord_tyrant_charge   Tyrant's Thrust held: drawn far back, low, the weight on the back foot (held to its end);
//   overlord_tyrant_release  let go: a driving thrust, the body lunging half a block behind the point;
//   overlord_earth_sweep     Earth-Splitting Sweep: the spear raised, brought down on the ground, then a whole turn at the knees;
//   overlord_falling_leap    Falling Mountain: a crouch, then up with the spear over the head, its point down ahead;
//   overlord_falling_plant   its landing: the spear planted to the socket, the body crouched over it;
//   overlord_charge          Overlord's Charge: wound up, then a charge of ten blocks, the point ahead, the body behind it;
//   overlord_rampage         Hundred Battles Rampage: twelve thrusts in two seconds, a step every three, then one that throws;
//   overlord_ascend          Conqueror Descends: the spear raised to the sky, held, then brought down toward the target;
//   overlord_verdict         the finish: the point under the fallen, the body lifted on it, then driven into the ground.
// Usage: node scripts/animation/overlord-spear.cjs
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth, sample, arm } = require('./pose.cjs');
const b = require('./biped.cjs');
const c = require('./celestial-spear.cjs');
const { hold, begin, GUARD, WIND, THRUST, DRAW, SWEEP, STANCE, LUNGE, CROUCH, TUCK, HIP, yawed, pitched, look, through, SHAFT, AIR_RAISE } = c;

const r3 = t => Math.round(t * 1000) / 1000;
/** The pose moved to stand `y` blocks forward (root), keeping its own height. */
const at = (pose, y) => ({ ...pose, Root: { ...pose.Root, pos: [pose.Root.pos[0], y, pose.Root.pos[2]] } });

// ---------------------------------------------------------------- 1. Tyrant's Thrust
begin();
/** Drawn far back: the hand behind the hip, the trunk turned away, the point still on the enemy. */
const DEEP_WIND = (y = 0) => hold({ Root: { pos: [0, y, HIP - 0.16] }, Torso: [['x', 4], ['z', 46]], Chest: [['z', 16]], ...look(40), ...STANCE },
  [0.34, -0.42, 0.18], yawed([0, 1, 0.06], 12));
const overlord_tyrant_charge = [
  { t: 0, pose: GUARD() },
  { t: 0.2, pose: DEEP_WIND(-0.05) },
  { t: 2.2, pose: DEEP_WIND(-0.08) },
];
begin();
const overlord_tyrant_release = [
  { t: 0, pose: DEEP_WIND(-0.08) },
  { t: 0.08, pose: THRUST(0.3, 0, 0, -0.14) },
  { t: 0.16, pose: THRUST(0.55, 0, 0, -0.2) },
  { t: 0.42, pose: THRUST(0.6, 0, 0, -0.2) },
  { t: 0.7, pose: GUARD(0.6) },
];

// ---------------------------------------------------------------- 2. Earth-Splitting Sweep
begin();
/** The spear high over the head, both arms up, the point to the sky behind. */
const RAISED = hold({ Root: { pos: [0, 0, HIP - 0.04] }, Torso: [['x', 10]], Chest: [['x', 6]], ...look(0, -14), ...STANCE },
  [0.14, 0.04, 0.82], pitched([0, 1, 0], 62));
/** Brought down on the ground ahead: the point in the earth, the body bent over it. */
const SLAMMED = hold({ Root: { pos: [0, 0.1, HIP - 0.3] }, Torso: [['x', -30]], Chest: [['x', -12]], ...look(0, 24), ...CROUCH },
  [0.12, 0.42, 0.26], pitched([0, 1, 0], -52));
const overlord_earth_sweep = (() => {
  const keys = [{ t: 0, pose: GUARD() }, { t: 0.22, pose: RAISED }, { t: 0.34, pose: SLAMMED }, { t: 0.44, pose: SLAMMED }];
  const per = 0.32, start = 0.5;
  for (let q = 0; q <= 4; q++) keys.push({ t: r3(start + per * q / 4), pose: SWEEP(q * 90, 0.1) });
  const back = GUARD(0.1); back.Root.rot = [['z', 360]];
  keys.push({ t: r3(start + per + 0.36), pose: back });
  return keys;
})();

// ---------------------------------------------------------------- 3. Falling Mountain
begin();
const CROUCHED = hold({ Root: { pos: [0, 0, HIP - 0.34] }, Torso: [['x', -18]], Chest: [['x', -8]], ...look(0, 10), ...CROUCH },
  [0.22, 0.12, 0.3], pitched([0, 1, 0], 30));
const overlord_falling_leap = [
  { t: 0, pose: GUARD() },
  { t: 0.15, pose: CROUCHED },
  { t: 0.3, pose: AIR_RAISE(0, 0.05) },
  { t: 1.6, pose: AIR_RAISE(0, 0.05) },
];
begin();
const PLANTED = (z) => hold({ Root: { pos: [0, 0.15, HIP + z] }, Torso: [['x', -26]], Chest: [['x', -12]], ...look(0, 24), ...CROUCH },
  [0.14, 0.36, 0.22], pitched([0, 1, 0], -84));
const overlord_falling_plant = [
  { t: 0, pose: AIR_RAISE(0, 0.05) },
  { t: 0.08, pose: PLANTED(-0.46) },
  { t: 0.36, pose: PLANTED(-0.5) },
  { t: 0.7, pose: at(GUARD(), 0.15) },
];

// ---------------------------------------------------------------- 4. Overlord's Charge
begin();
const CHARGE_LENGTH = 10;
const overlord_charge = [
  { t: 0, pose: GUARD() },
  { t: 0.16, pose: WIND(-0.1) },
  { t: 0.26, pose: THRUST(1.2, 0, 0, -0.16) },
  { t: 0.5, pose: THRUST(CHARGE_LENGTH * 0.45, 0, 0, -0.18) },
  { t: 0.78, pose: THRUST(CHARGE_LENGTH * 0.88, 0, 0, -0.2) },
  { t: 0.9, pose: THRUST(CHARGE_LENGTH, 0, 0, -0.2) },
  { t: 1.1, pose: DRAW(CHARGE_LENGTH) },
  { t: 1.3, pose: GUARD(CHARGE_LENGTH) },
];

// ---------------------------------------------------------------- 5. Hundred Battles Rampage
const PETALS = [[-20, 8], [14, -6], [-6, 16], [22, 2], [-12, -10], [6, 10], [-24, -2], [18, 12], [-8, 4], [10, -8], [-16, 12], [4, 0]];
const RAMPAGE_GAP = 0.15, RAMPAGE_LAST = 0.22;
const rampage = (thrusts) => {
  begin();
  const keys = [{ t: 0, pose: GUARD() }];
  for (let i = 0; i < thrusts; i++) {
    const [yaw, pitch] = PETALS[i % PETALS.length], t = RAMPAGE_GAP * (i + 1), y = 0.25 * Math.floor(i / 3);
    keys.push({ t: r3(t - RAMPAGE_GAP * 0.45), pose: DRAW(y, yaw, pitch) });
    keys.push({ t: r3(t), pose: THRUST(y + 0.08, yaw, pitch, -0.12, STANCE) });
  }
  const y = 0.25 * Math.floor(thrusts / 3), last = r3(RAMPAGE_GAP * thrusts + RAMPAGE_LAST);
  keys.push({ t: r3(last - 0.12), pose: DEEP_WIND(y) });
  keys.push({ t: last, pose: THRUST(y + 0.5, 0, 0, -0.18) });
  keys.push({ t: r3(last + 0.16), pose: THRUST(y + 0.55, 0, 0, -0.2) });
  keys.push({ t: r3(last + 0.42), pose: GUARD(y + 0.55) });
  return keys;
};
const overlord_rampage = rampage(12);

// ---------------------------------------------------------------- the ultimate: Conqueror Descends
begin();
const TO_SKY = (z = 0) => hold({ Root: { pos: [0, 0, HIP + z] }, Torso: [['x', 12]], Chest: [['x', 8]], ...look(0, -34), ...STANCE },
  [0.1, 0.06, 0.9], pitched([0, 1, 0], 84));
const overlord_ascend = [
  { t: 0, pose: GUARD() },
  { t: 0.3, pose: TO_SKY(0.02) },
  { t: 2.2, pose: TO_SKY(0.04) },
  { t: 2.4, pose: THRUST(0.3, 0, -20, -0.16) },
  { t: 2.9, pose: GUARD(0.3) },
];

// ---------------------------------------------------------------- the finish: Overlord's Verdict
begin();
const LIFTED = hold({ Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['x', 14]], Chest: [['x', 8]], ...look(0, -24), ...STANCE },
  [0.12, 0.3, 0.7], pitched([0, 1, 0], 48));
const overlord_verdict = [
  { t: 0, pose: GUARD() },
  { t: 0.3, pose: THRUST(0.2, 0, -30, -0.2) },
  { t: 0.9, pose: LIFTED },
  { t: 1.4, pose: LIFTED },
  { t: 1.6, pose: at(PLANTED(-0.5), 0.2) },
  { t: 2.2, pose: at(PLANTED(-0.52), 0.2) },
];

const anims = { overlord_tyrant_charge, overlord_tyrant_release, overlord_earth_sweep, overlord_falling_leap, overlord_falling_plant,
  overlord_charge, overlord_rampage, overlord_ascend, overlord_verdict };
/** When each of the rampage's thrusts lands, and the last (seconds): the Java phases follow them (MurimAnimations). */
const timings = {
  rampage: [...Array(12)].map((_, i) => r3(RAMPAGE_GAP * (i + 1))), rampageLast: r3(RAMPAGE_GAP * 12 + RAMPAGE_LAST)
};
module.exports = { anims, timings };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) {
    const { times, frames } = sample(keys);
    let worst = { depth: 0 };
    times.forEach((t, n) => { const r = through(b.modelSpace(arm, frames[n])); if (r.depth > worst.depth) worst = { ...r, t }; });
    if (worst.depth > 2 * SHAFT) console.warn(`  ${name}: spear through the ${worst.part} at ${worst.t}s (${worst.depth.toFixed(2)})`);
    writeSmooth(name, keys);
  }
  console.log('timings', JSON.stringify(timings));
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'overlord_spear.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('overlord_spear', anims), null, 1));
  console.log('wrote', out);
}
