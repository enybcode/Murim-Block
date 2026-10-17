// The Purple Heaven Thunder Saber's moves for the player (docs/combat/styles/purple-heaven-thunder-saber.md), the Purple
// Heaven Zhanmadao (or any greatsword) in both hands. Written as Epic Fight animations biped/murim/ts_*. Both arms and the
// wrist are solved for where the hands hold the saber, which way it points and which way its edge faces, as the Bandit
// Chief's great axe (chief-axe.cjs: hold, the left hand on the grip behind the right). The damage is the server's
// (martial/ThunderSaberMoves), on the ticks these clips reach their blow; the body is moved by the server too (the leap,
// the turns' advance), so no clip moves its root forward.
//   ts_descends       Thunder Descends: a crouch, the saber raised over the head in the leap, brought down at 0.7 s;
//   ts_rolling        Rolling Thunder: the saber level, arms out, two full turns (0.05 to 0.95 s), a follow-through;
//   ts_rolling_long   the same, three turns (Third Turn);
//   ts_drum_plant     Heaven's Drum: the saber planted point down before the feet, both hands on the grip, held 3.4 s;
//   ts_drum_strike    the drum beaten: a sharp drop onto the saber, then back on guard;
//   ts_flash_charge   Severing Flash charging: low, the saber wound far back on the right, level, held 2.4 s;
//   ts_flash_cut      the cut: swept level across the front to the left (the wave leaves at 0.15 s);
//   ts_wrath          Nine Heavens' Wrath: the saber raised straight to the sky, held, brought down at 3.0 s;
//   ts_sovereign      Thunder Sovereign's Descent: the saber planted, the head up to the bolt (0.5 s), back on guard;
//   ts_finish         The Last Thunderclap: the saber raised to the sky (the bolt at 0.7 s), brought down at 1.3 s.
// Usage: node scripts/animation/thunder-saber-art.cjs
// Axes: the character's (x right, y front, z up), in blocks; the root's position +y forward.
const { hold, begin, check, HIP } = require('./chief-axe.cjs');
const { writeSmooth } = require('./pose.cjs');

const yawed = ([x, y, z], deg) => { const a = deg * Math.PI / 180; return [x * Math.cos(a) - y * Math.sin(a), x * Math.sin(a) + y * Math.cos(a), z]; };
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });
const LEGS_WIDE = { Thigh_R: [['x', 18], ['y', -10]], Leg_R: [['x', -14]], Thigh_L: [['x', -16], ['y', 10]], Leg_L: [['x', -12]] };
const LEGS_LOW = { Thigh_R: [['x', 50], ['y', -10]], Leg_R: [['x', -60]], Thigh_L: [['x', -6], ['y', 12]], Leg_L: [['x', -40]] };
const ARMS_GUESS = { Arm_R: [['x', 60], ['y', 0], ['z', 10]], Hand_R: [['x', 40]], Tool_R: [['x', -30], ['y', 0], ['z', 0]], Arm_L: [['x', 60], ['y', 20], ['z', -20]], Hand_L: [['x', 40]] };
/** Turned `deg` round the vertical: the whole body (the spins). */
const spin = (deg, pose) => ({ ...pose, Root: { ...pose.Root, rot: [['z', deg]] } });

/** Ready: feet apart, the saber slanting up across the body, its point high on the right, the edge out front. */
const GUARD = () => hold(
  { Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['z', -10]], Chest: [['x', -6], ['z', -4]], ...look(-14), ...LEGS_WIDE },
  [0.06, 0.36, 0.3], [0.45, 0.4, 0.8], [0.2, 1, 0], { Arm_R: [['x', 40], ['y', -10], ['z', 20]], Hand_R: [['x', 50]], Tool_R: [['x', -10], ['y', 0], ['z', 0]], Arm_L: [['x', 50], ['y', 20], ['z', -10]], Hand_L: [['x', 50]] });

/** Crouched to leap: low, the saber held back and down on the right, its edge up. */
const CROUCH = () => hold(
  { Root: { pos: [0, 0, HIP - 0.26] }, Torso: [['x', -12]], Chest: [['x', 4]], ...look(0, -6),
    Thigh_R: [['x', 50]], Leg_R: [['x', -90]], Thigh_L: [['x', 40]], Leg_L: [['x', -80]] },
  [0.22, 0.15, 0.3], [0.3, -0.75, -0.6], [0, 0, 1], { Arm_R: [['x', 30], ['y', -10], ['z', 30]], Hand_R: [['x', 30]], Tool_R: [['x', -50], ['y', 0], ['z', 0]], Arm_L: [['x', 60], ['y', 10], ['z', -30]], Hand_L: [['x', 40]] });

/** Raised: both hands up at the brow, the saber upright over the head and tipped back, its edge out front. */
const RAISED = (z = 0.02, back = -0.25, legs = { Thigh_R: [['x', -14]], Leg_R: [['x', -10]], Thigh_L: [['x', 16]], Leg_L: [['x', -8]] }) => hold(
  { Root: { pos: [0, 0, HIP + z] }, Torso: [['x', 4]], Chest: [['x', 4]], ...look(0, -12), ...legs },
  [0.05, 0.38, 0.9], [0.1, back, 0.96], [0, 1, 0], { Arm_R: [['x', 150], ['y', 0], ['z', 10]], Hand_R: [['x', 25]], Tool_R: [['x', -45], ['y', 0], ['z', 0]], Arm_L: [['x', 145], ['y', 10], ['z', -10]], Hand_L: [['x', 30]] });

/** The cut has landed: both hands low in front, the saber's point at the ground ahead, the edge down. */
const CHOP = (z = -0.24, legs = { Thigh_R: [['x', 64]], Leg_R: [['x', -78]], Thigh_L: [['x', -24]], Leg_L: [['x', -46]] }) => hold(
  { Root: { pos: [0, 0, HIP + z] }, Torso: [['x', -26]], Chest: [['x', -16]], ...look(0, 22), ...legs },
  [0.02, 0.5, 0.18], [0, 0.72, -0.7], [0, 0, -1], { Arm_R: [['x', 70], ['y', 0], ['z', 6]], Hand_R: [['x', 6]], Tool_R: [['x', -70], ['y', 0], ['z', 0]], Arm_L: [['x', 70], ['y', 10], ['z', -10]], Hand_L: [['x', 20]] });

/** Drawn back out of the ground, the saber swung out to the right and up, on the way back to the guard. */
const RECOVER = (z = -0.12) => hold(
  { Root: { pos: [0, 0, HIP + z] }, Torso: [['x', -10], ['z', -8]], Chest: [['x', -6]], ...look(-8, 6), ...LEGS_WIDE },
  [0.12, 0.38, 0.3], [0.9, 0.3, 0.3], [0, 1, 0.2], { Arm_R: [['x', 50], ['y', -10], ['z', 30]], Hand_R: [['x', 40]], Tool_R: [['x', -20], ['y', 0], ['z', 0]], Arm_L: [['x', 55], ['y', 20], ['z', -20]], Hand_L: [['x', 40]] });

/**
 * A level sweep, the hands turned `yaw` (+ left), the saber pointing out level `ahead` degrees further round, the edge
 * toward the sweep (to its left); `low` lowers the stance (the charge).
 */
const SWEEP = (yaw, trunk, { y = 0, lead = 'R', ahead = 0, low = 0 } = {}) => hold(
  { Root: { pos: [0, y, HIP - 0.1 - low] }, Torso: [['z', trunk * 0.6]], Chest: [['x', -12], ['z', trunk * 0.4]], ...look(trunk),
    ...(lead === 'R'
      ? { Thigh_R: [['x', 46 + low * 120], ['y', -8]], Leg_R: [['x', -44 - low * 140]], Thigh_L: [['x', -26], ['y', 8]], Leg_L: [['x', -12 - low * 100]] }
      : { Thigh_R: [['x', -18], ['y', -8]], Leg_R: [['x', -14]], Thigh_L: [['x', 42], ['y', 8]], Leg_L: [['x', -38]] }) },
  yawed([0.02, 0.4, 0.34], yaw), yawed([0, 1, -0.05], yaw + ahead), yawed([-1, 0, 0], yaw + ahead),
  { Arm_R: [['x', 80], ['y', 0], ['z', yaw - trunk]], Hand_R: [['x', 14]], Tool_R: [['x', -88], ['y', 90], ['z', 0]],
    Arm_L: [['x', 70], ['y', 10], ['z', (yaw - trunk) * 0.8]], Hand_L: [['x', 30]] });

/** The saber held level straight out in front, arms out, the edge to the left: the turns of Rolling Thunder. */
const LEVEL = () => hold(
  { Root: { pos: [0, 0, HIP - 0.12] }, Torso: [['x', -4]], Chest: [['x', -6]], ...look(0, 4), ...LEGS_WIDE },
  [0.02, 0.5, 0.36], [0.35, 1, -0.02], [-1, 0.3, 0], { Arm_R: [['x', 85], ['y', 0], ['z', 10]], Hand_R: [['x', 10]], Tool_R: [['x', -88], ['y', 90], ['z', 0]], Arm_L: [['x', 80], ['y', 10], ['z', -10]], Hand_L: [['x', 20]] });

/** Planted: the saber point down before the feet, both hands high on its grip, the body a little low. */
const PLANT = (z = -0.08, pitch = 6) => hold(
  { Root: { pos: [0, 0, HIP + z] }, Torso: [['x', -6]], Chest: [['x', -4]], ...look(0, pitch), ...LEGS_WIDE },
  [0.02, 0.42, 0.5], [0, 0.18, -1], [0, 1, 0], { Arm_R: [['x', 60], ['y', 0], ['z', 10]], Hand_R: [['x', 50]], Tool_R: [['x', -120], ['y', 0], ['z', 0]], Arm_L: [['x', 55], ['y', 10], ['z', -10]], Hand_L: [['x', 50]] });

/** The saber raised straight to the sky, both hands above the head, the body stretched. */
const SKY = (z = 0.03) => hold(
  { Root: { pos: [0, 0, HIP + z] }, Torso: [['x', 6]], Chest: [['x', 6]], ...look(0, -26), ...LEGS_WIDE },
  [0.04, 0.22, 1.02], [0, 0.04, 1], [0, 1, 0], { Arm_R: [['x', 170], ['y', 0], ['z', 6]], Hand_R: [['x', 10]], Tool_R: [['x', -30], ['y', 0], ['z', 0]], Arm_L: [['x', 165], ['y', 10], ['z', -8]], Hand_L: [['x', 16]] });

const TUCKED = { Thigh_R: [['x', 40]], Leg_R: [['x', -60]], Thigh_L: [['x', 30]], Leg_L: [['x', -50]] };
const LANDED = { Thigh_R: [['x', 70]], Leg_R: [['x', -95]], Thigh_L: [['x', -10]], Leg_L: [['x', -60]] };

/** The turns: a key every quarter turn (the joints turn the shortest way between keys), from `t0`, `turns` of them. */
function turning(t0, turns, quarter) {
  const keys = [];
  const level = LEVEL();
  for (let q = 0; q <= turns * 4; q++) keys.push({ t: t0 + q * quarter, pose: spin(q * 90, level) });
  return keys;
}

begin();
const ts_descends = [
  { t: 0, pose: GUARD() },
  { t: 0.18, pose: CROUCH() },
  // In the leap: the saber over the head, knees drawn up.
  { t: 0.34, pose: RAISED(0.06, -0.2, TUCKED) },
  { t: 0.6, pose: RAISED(0.06, -0.45, TUCKED) },
  // Down where it lands, at 0.7 s.
  { t: 0.7, pose: CHOP(-0.3, LANDED) },
  { t: 0.95, pose: CHOP(-0.32, LANDED) },
  { t: 1.2, pose: RECOVER() },
  { t: 1.45, pose: GUARD() },
];

const QUARTER = 0.1125;
begin();
const rollingKeys = turns => [
  { t: 0, pose: GUARD() },
  { t: 0.05, pose: LEVEL() },
  ...turning(0.05, turns, QUARTER).slice(1),
  { t: 0.05 + turns * 4 * QUARTER + 0.12, pose: SWEEP(60, 40, { lead: 'L', ahead: 30 }) },
  { t: 0.05 + turns * 4 * QUARTER + 0.4, pose: GUARD() },
];
const ts_rolling = rollingKeys(2);
begin();
const ts_rolling_long = rollingKeys(3);

begin();
const ts_drum_plant = [
  { t: 0, pose: GUARD() },
  { t: 0.3, pose: PLANT() },
  { t: 3.4, pose: PLANT(-0.1) },
];

begin();
const ts_drum_strike = [
  { t: 0, pose: PLANT() },
  { t: 0.07, pose: PLANT(-0.2, 14) },
  { t: 0.3, pose: PLANT(-0.12, 8) },
  { t: 0.65, pose: GUARD() },
];

begin();
const CHARGE = () => SWEEP(-100, -55, { ahead: -40, low: 0.12 });
const ts_flash_charge = [
  { t: 0, pose: GUARD() },
  { t: 0.25, pose: CHARGE() },
  { t: 2.4, pose: CHARGE() },
];

begin();
const ts_flash_cut = [
  { t: 0, pose: CHARGE() },
  { t: 0.08, pose: SWEEP(12, 10, { y: 0.2, ahead: 28 }) },
  { t: 0.16, pose: SWEEP(100, 66, { y: 0.25, lead: 'L', ahead: 35 }) },
  { t: 0.42, pose: SWEEP(106, 70, { y: 0.25, lead: 'L', ahead: 35 }) },
  { t: 0.75, pose: GUARD() },
];

begin();
const ts_wrath = [
  { t: 0, pose: GUARD() },
  { t: 0.4, pose: SKY() },
  { t: 2.85, pose: SKY(0.05) },
  // The last bolt has struck the saber: brought down onto the circle at 3.0 s.
  { t: 3.0, pose: CHOP(-0.28) },
  { t: 3.3, pose: CHOP(-0.3) },
  { t: 3.6, pose: RECOVER() },
  { t: 3.9, pose: GUARD() },
];

begin();
const ts_sovereign = [
  { t: 0, pose: GUARD() },
  { t: 0.3, pose: PLANT(-0.06, -30) },
  // The bolt strikes at 0.5 s: the body thrown back a little, the head up.
  { t: 0.55, pose: PLANT(0.02, -40) },
  { t: 0.9, pose: PLANT(-0.04, -10) },
  { t: 1.3, pose: GUARD() },
];

begin();
const ts_finish = [
  { t: 0, pose: GUARD() },
  { t: 0.5, pose: SKY() },
  { t: 1.15, pose: SKY(0.05) },
  // Down on the body at 1.3 s.
  { t: 1.3, pose: CHOP(-0.34) },
  { t: 1.8, pose: CHOP(-0.34) },
  { t: 2.1, pose: RECOVER() },
  { t: 2.4, pose: GUARD() },
];

const GAME = { ts_descends, ts_rolling, ts_rolling_long, ts_drum_plant, ts_drum_strike, ts_flash_charge, ts_flash_cut, ts_wrath, ts_sovereign, ts_finish };

if (require.main === module) for (const [name, keys] of Object.entries(GAME)) { check(name, keys); writeSmooth(name, keys); }

module.exports = { GAME };
