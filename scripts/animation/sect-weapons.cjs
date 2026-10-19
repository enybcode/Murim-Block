// The moves of the three sect weapons that Epic Fight has no type for (MurimWeaponCategories): the iron fan, the chain
// whip and the flying needles, as a Blockbench project (art/blockbench/sect_weapons.bbmodel) to look at before they go
// into the game. Each weapon has a stance (looped), four M1 strikes, a running attack and an air attack, as Epic Fight's
// own weapon types do (MurimWeapons builds their movesets):
//   iron fan      fan_idle: the fan open before the chest, the left hand behind the back, a scholar's stance;
//                 fan_auto1 a flick of the open fan right to left; fan_auto2 the backhand, rising left to right;
//                 fan_auto3 a full circle over the head, then down across; fan_auto4 a lunge, the fan's edge driven ahead;
//                 fan_dash a running lunge; fan_air a cut down from the air;
//   chain whip    whip_idle: the handle low at the side, the chain hanging (the renderer swings it);
//                 whip_auto1 an overhead lash; whip_auto2 a sidearm lash right to left; whip_auto3 a circle over the head
//                 then a lash ahead; whip_auto4 a low sweep at the legs, crouching; whip_dash a running lash; whip_air a
//                 lash down from the air (the chain's reach is the weapon's collider, MurimWeapons);
//   needles       needles_idle: the needles held at the chest between the fingers, the left hand open ahead;
//                 needles_auto1 a flick throw from the chest; needles_auto2 a backhand throw from the left shoulder;
//                 needles_auto3 an overhand throw from by the ear; needles_auto4 both arms swept wide, a fan of needles;
//                 needles_dash a hop back and a throw; needles_air a throw down from the air (the needles leave the hand
//                 at the times MurimWeapons gives).
// Usage: node scripts/animation/sect-weapons.cjs, then node scripts/animation/blockbench-to-epicfight.cjs art/blockbench/sect_weapons.bbmodel
//
// Pose notation: see azure-m1.cjs (x the character's right, y its front, z up; Arm x swings the arm forward and up,
// Hand x bends the elbow, Tool_R is the wrist: Tool_R x -92 lays the weapon along the forearm). The root never turns.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

const HIP = 0.764;
const look = (yaw, pitch = 0) => ({ Head: [['x', pitch], ['z', -yaw * 0.85]] });
const LEGS_R = { Thigh_R: [['x', 30], ['y', -6]], Leg_R: [['x', -30]], Thigh_L: [['x', -20], ['y', 6]], Leg_L: [['x', -12]] };
const LEGS_L = { Thigh_R: [['x', -18], ['y', -6]], Leg_R: [['x', -14]], Thigh_L: [['x', 34], ['y', 6]], Leg_L: [['x', -30]] };
const LEGS_SOFT = { Thigh_R: [['x', 12], ['y', -4]], Leg_R: [['x', -14]], Thigh_L: [['x', -8], ['y', 4]], Leg_L: [['x', -10]] };

/** A sweep of the right arm turned `yaw` (+ left) at `pitch`, the trunk turned `trunk`; the wrist sets the weapon flat. */
const SWEEP = (yaw, trunk, { y = 0, z = HIP - 0.06, pitch = 86, hand = 10, tool = [['y', 90], ['x', -92]], legs = LEGS_R, left } = {}) => ({
  Root: { pos: [0, y, z] },
  Torso: [['z', trunk * 0.6]], Chest: [['x', -8], ['z', trunk * 0.4]], ...look(trunk),
  Arm_R: [['x', pitch], ['z', yaw - trunk]], Hand_R: [['x', hand]], Tool_R: tool,
  ...(left || { Arm_L: [['x', 20 - trunk * 0.3], ['y', 30]], Hand_L: [['x', 30]] }),
  ...legs,
});

// ---------------------------------------------------------------- the iron fan

/** The scholar's stance: the open fan before the chest, its face out, the left hand behind the back. */
const FAN_GUARD = (y = 0, sway = 0) => ({
  Root: { pos: [0, y, HIP - 0.02] },
  Torso: [['z', -10 + sway]], Chest: [['x', -2]], ...look(-12 + sway),
  Arm_R: [['x', 40], ['z', 18]], Hand_R: [['x', 92]], Tool_R: [['x', -10], ['y', 70]],
  Arm_L: [['x', -26], ['z', 30]], Hand_L: [['x', 96]],
  ...LEGS_SOFT,
});
const fan_idle = [
  { t: 0, pose: FAN_GUARD(0, 0) },
  { t: 1.0, pose: FAN_GUARD(0, 6) },
  { t: 2.0, pose: FAN_GUARD(0, 0) },
];
const FAN_LEFT = { Arm_L: [['x', -26], ['z', 30]], Hand_L: [['x', 96]] };
const fan_auto1 = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.08, pose: SWEEP(-80, -30, { pitch: 80, hand: 30, left: FAN_LEFT }) },
  { t: 0.16, pose: SWEEP(0, 0, { y: 0.12, left: FAN_LEFT }) },
  { t: 0.24, pose: SWEEP(85, 34, { y: 0.18, z: HIP - 0.09, left: FAN_LEFT }) },
  { t: 0.34, pose: SWEEP(92, 36, { y: 0.18, z: HIP - 0.09, pitch: 82, left: FAN_LEFT }) },
  { t: 0.5, pose: FAN_GUARD(0.18) },
];
const fan_auto2 = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.08, pose: SWEEP(90, 34, { pitch: 60, hand: 40, tool: [['y', -90], ['x', -92]], left: FAN_LEFT, legs: LEGS_L }) },
  { t: 0.17, pose: SWEEP(0, 0, { y: 0.12, pitch: 96, tool: [['y', -90], ['x', -92]], left: FAN_LEFT, legs: LEGS_L }) },
  { t: 0.25, pose: SWEEP(-90, -36, { y: 0.16, pitch: 128, tool: [['y', -90], ['x', -92]], left: FAN_LEFT, legs: LEGS_L }) },
  { t: 0.36, pose: SWEEP(-96, -38, { y: 0.16, pitch: 132, tool: [['y', -90], ['x', -92]], left: FAN_LEFT, legs: LEGS_L }) },
  { t: 0.52, pose: FAN_GUARD(0.16) },
];
/** The fan raised over the head, turned `yaw` round it. */
const FAN_OVER = (yaw, y = 0) => ({
  Root: { pos: [0, y, HIP + 0.02] },
  Torso: [['x', 6], ['z', yaw * 0.3]], Chest: [['x', 6]], ...look(yaw * 0.3, -10),
  Arm_R: [['x', 160], ['z', yaw]], Hand_R: [['x', 20]], Tool_R: [['y', 90], ['x', -80]],
  Arm_L: [['x', 40], ['y', 30]], Hand_L: [['x', 40]],
  ...LEGS_SOFT,
});
const fan_auto3 = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.1, pose: FAN_OVER(-60) },
  { t: 0.2, pose: FAN_OVER(40, 0.06) },
  { t: 0.3, pose: FAN_OVER(140, 0.1) },
  { t: 0.4, pose: { ...SWEEP(40, 20, { y: 0.24, z: HIP - 0.14, pitch: 60, legs: LEGS_R }), Torso: [['x', -16], ['z', 12]] } },
  { t: 0.5, pose: { ...SWEEP(70, 30, { y: 0.26, z: HIP - 0.16, pitch: 40, legs: LEGS_R }), Torso: [['x', -20], ['z', 18]] } },
  { t: 0.68, pose: FAN_GUARD(0.26) },
];
/** The fan drawn back, then its edge driven straight ahead in a lunge. */
const FAN_DRAWN = {
  Root: { pos: [0, -0.04, HIP - 0.08] },
  Torso: [['z', -30]], Chest: [['x', -6], ['z', -10]], ...look(-40),
  Arm_R: [['x', -24], ['y', -10]], Hand_R: [['x', 118]], Tool_R: [['x', -92]],
  Arm_L: [['x', 80], ['z', -16]], Hand_L: [['x', 6]],
  Thigh_R: [['x', 18], ['y', -4]], Leg_R: [['x', -30]], Thigh_L: [['x', -14], ['y', 4]], Leg_L: [['x', -34]],
};
const FAN_LUNGE = (y) => ({
  Root: { pos: [0, y, HIP - 0.17] },
  Torso: [['x', -8], ['z', 8]], Chest: [['x', -10], ['z', 6]], ...look(14, 6),
  Arm_R: [['x', 86], ['z', -4]], Hand_R: [['x', 2]], Tool_R: [['x', -92]],
  Arm_L: [['x', -36], ['y', 40]], Hand_L: [['x', 16]],
  Thigh_R: [['x', 72]], Leg_R: [['x', -74]], Thigh_L: [['x', -40]], Leg_L: [['x', -4]],
});
const fan_auto4 = [
  { t: 0, pose: FAN_GUARD() },
  { t: 0.12, pose: FAN_DRAWN },
  { t: 0.22, pose: FAN_LUNGE(0.6) },
  { t: 0.38, pose: FAN_LUNGE(0.64) },
  { t: 0.62, pose: FAN_GUARD(0.64) },
];
const fan_dash = [
  { t: 0, pose: FAN_DRAWN },
  { t: 0.14, pose: FAN_LUNGE(1.0) },
  { t: 0.3, pose: FAN_LUNGE(1.6) },
  { t: 0.5, pose: FAN_GUARD(1.6) },
];
const AIR_LEGS = { Thigh_R: [['x', 50]], Leg_R: [['x', -70]], Thigh_L: [['x', 30]], Leg_L: [['x', -50]] };
const fan_air = [
  { t: 0, pose: { ...FAN_OVER(0), ...AIR_LEGS } },
  { t: 0.12, pose: { ...SWEEP(20, 0, { pitch: 150, z: HIP + 0.05, legs: AIR_LEGS }) } },
  { t: 0.24, pose: { ...SWEEP(30, 10, { pitch: 30, z: HIP - 0.1, legs: AIR_LEGS }), Torso: [['x', -24]] } },
  { t: 0.42, pose: FAN_GUARD() },
];

// ---------------------------------------------------------------- the chain whip

/** The handle low at the right side, the chain hanging from it; the left hand open ahead. */
const WHIP_GUARD = (y = 0, sway = 0) => ({
  Root: { pos: [0, y, HIP - 0.04] },
  Torso: [['z', -14 + sway]], Chest: [['x', -4]], ...look(-16 + sway),
  Arm_R: [['x', 16], ['z', -14]], Hand_R: [['x', 36]], Tool_R: [['x', -60]],
  Arm_L: [['x', 46], ['y', 20]], Hand_L: [['x', 40]],
  ...LEGS_R,
});
const whip_idle = [
  { t: 0, pose: WHIP_GUARD(0, 0) },
  { t: 1.2, pose: WHIP_GUARD(0, 5) },
  { t: 2.4, pose: WHIP_GUARD(0, 0) },
];
/** The arm thrown back high over the shoulder, the handle pointing behind. */
const WHIP_BACK = (yaw = 0) => ({
  Root: { pos: [0, -0.04, HIP] },
  Torso: [['x', 6], ['z', -20 + yaw * 0.3]], Chest: [['x', 8]], ...look(-10, -6),
  Arm_R: [['x', 170], ['z', -10 + yaw]], Hand_R: [['x', 60]], Tool_R: [['x', -92]],
  Arm_L: [['x', 70], ['y', 10]], Hand_L: [['x', 10]],
  ...LEGS_SOFT,
});
/** The lash: the arm snapped down ahead, the handle pointing at the target, the body leaning in. */
const WHIP_SNAP = (y, yaw = 0, pitch = 70) => ({
  Root: { pos: [0, y, HIP - 0.1] },
  Torso: [['x', -14], ['z', 10 + yaw * 0.3]], Chest: [['x', -8]], ...look(10, 8),
  Arm_R: [['x', pitch], ['z', yaw]], Hand_R: [['x', 4]], Tool_R: [['x', -92]],
  Arm_L: [['x', -20], ['y', 30]], Hand_L: [['x', 20]],
  ...LEGS_R,
});
const whip_auto1 = [
  { t: 0, pose: WHIP_GUARD() },
  { t: 0.14, pose: WHIP_BACK() },
  { t: 0.24, pose: WHIP_SNAP(0.14, 0, 100) },
  { t: 0.3, pose: WHIP_SNAP(0.18, 0, 64) },
  { t: 0.46, pose: WHIP_SNAP(0.18, 0, 56) },
  { t: 0.66, pose: WHIP_GUARD(0.18) },
];
const whip_auto2 = [
  { t: 0, pose: WHIP_GUARD() },
  { t: 0.12, pose: SWEEP(-120, -46, { pitch: 80, hand: 20, tool: [['x', -92]] }) },
  { t: 0.22, pose: SWEEP(-10, 0, { y: 0.12, pitch: 84, tool: [['x', -92]] }) },
  { t: 0.3, pose: SWEEP(90, 40, { y: 0.16, pitch: 80, tool: [['x', -92]] }) },
  { t: 0.46, pose: SWEEP(100, 44, { y: 0.16, pitch: 76, tool: [['x', -92]] }) },
  { t: 0.64, pose: WHIP_GUARD(0.16) },
];
/** The arm up, turning the chain round over the head, `yaw` round. */
const WHIP_CIRCLE = (yaw) => ({
  Root: { pos: [0, 0, HIP] },
  Torso: [['z', yaw * 0.2]], Chest: [['x', 4]], ...look(0, -8),
  Arm_R: [['x', 150], ['z', yaw * 0.25]], Hand_R: [['x', 30]], Tool_R: [['y', yaw], ['x', -60]],
  Arm_L: [['x', 40], ['y', 20]], Hand_L: [['x', 30]],
  ...LEGS_SOFT,
});
const whip_auto3 = [
  { t: 0, pose: WHIP_GUARD() },
  { t: 0.1, pose: WHIP_CIRCLE(-90) },
  { t: 0.2, pose: WHIP_CIRCLE(0) },
  { t: 0.3, pose: WHIP_CIRCLE(90) },
  { t: 0.4, pose: WHIP_CIRCLE(180) },
  { t: 0.5, pose: WHIP_SNAP(0.2, 0, 92) },
  { t: 0.56, pose: WHIP_SNAP(0.24, 0, 66) },
  { t: 0.74, pose: WHIP_GUARD(0.24) },
];
/** Crouched low, the arm swept along the ground at the legs. */
const WHIP_LOW = (yaw, y = 0) => ({
  Root: { pos: [0, y, HIP - 0.3] },
  Torso: [['x', -20], ['z', yaw * 0.4]], Chest: [['x', -10]], ...look(yaw * 0.3, 14),
  Arm_R: [['x', 50], ['z', yaw]], Hand_R: [['x', 6]], Tool_R: [['x', -92]],
  Arm_L: [['x', -30], ['y', 40]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 70], ['y', -10]], Leg_R: [['x', -100]], Thigh_L: [['x', -10], ['y', 20]], Leg_L: [['x', -60]],
});
const whip_auto4 = [
  { t: 0, pose: WHIP_GUARD() },
  { t: 0.14, pose: WHIP_LOW(-110) },
  { t: 0.26, pose: WHIP_LOW(-10, 0.1) },
  { t: 0.36, pose: WHIP_LOW(100, 0.14) },
  { t: 0.52, pose: WHIP_LOW(110, 0.14) },
  { t: 0.78, pose: WHIP_GUARD(0.14) },
];
const whip_dash = [
  { t: 0, pose: WHIP_BACK() },
  { t: 0.16, pose: WHIP_SNAP(0.9, 0, 96) },
  { t: 0.24, pose: WHIP_SNAP(1.3, 0, 64) },
  { t: 0.44, pose: WHIP_GUARD(1.4) },
];
const whip_air = [
  { t: 0, pose: { ...WHIP_BACK(), ...AIR_LEGS } },
  { t: 0.14, pose: { ...WHIP_SNAP(0.1, 0, 90), ...AIR_LEGS } },
  { t: 0.22, pose: { ...WHIP_SNAP(0.14, 0, 30), ...AIR_LEGS } },
  { t: 0.44, pose: WHIP_GUARD() },
];

// ---------------------------------------------------------------- the flying needles

/** The needles held at the chest between the fingers, the left hand open ahead to aim. */
const NEEDLE_GUARD = (y = 0, sway = 0) => ({
  Root: { pos: [0, y, HIP - 0.04] },
  Torso: [['z', -20 + sway]], Chest: [['x', -4]], ...look(-22 + sway),
  Arm_R: [['x', 20], ['z', 40]], Hand_R: [['x', 120]], Tool_R: [['x', -40]],
  Arm_L: [['x', 70], ['z', -10]], Hand_L: [['x', 20]],
  ...LEGS_R,
});
const needles_idle = [
  { t: 0, pose: NEEDLE_GUARD(0, 0) },
  { t: 1.0, pose: NEEDLE_GUARD(0, 4) },
  { t: 2.0, pose: NEEDLE_GUARD(0, 0) },
];
/** The throwing arm straight out toward `yaw`, the needles just gone. */
const THROWN = (yaw, pitch = 86, y = 0.08, trunk = 10) => ({
  Root: { pos: [0, y, HIP - 0.06] },
  Torso: [['z', trunk]], Chest: [['x', -6], ['z', trunk * 0.5]], ...look(trunk),
  Arm_R: [['x', pitch], ['z', yaw - trunk]], Hand_R: [['x', 4]], Tool_R: [['x', -92]],
  Arm_L: [['x', -20], ['y', 30]], Hand_L: [['x', 20]],
  ...LEGS_R,
});
const needles_auto1 = [
  { t: 0, pose: NEEDLE_GUARD() },
  { t: 0.08, pose: { ...NEEDLE_GUARD(), Arm_R: [['x', 30], ['z', 70]], Hand_R: [['x', 130]] } },
  { t: 0.14, pose: THROWN(-10) },
  { t: 0.3, pose: THROWN(-14, 84) },
  { t: 0.44, pose: NEEDLE_GUARD(0.08) },
];
const needles_auto2 = [
  { t: 0, pose: NEEDLE_GUARD() },
  { t: 0.09, pose: { ...NEEDLE_GUARD(), Torso: [['z', 30]], Arm_R: [['x', 70], ['z', 110]], Hand_R: [['x', 120]], Tool_R: [['x', -92]] } },
  { t: 0.16, pose: THROWN(-30, 88, 0.08, -10) },
  { t: 0.32, pose: THROWN(-40, 86, 0.08, -14) },
  { t: 0.46, pose: NEEDLE_GUARD(0.08) },
];
const needles_auto3 = [
  { t: 0, pose: NEEDLE_GUARD() },
  { t: 0.1, pose: { ...NEEDLE_GUARD(), Torso: [['x', 8], ['z', -30]], Arm_R: [['x', 160], ['z', -10]], Hand_R: [['x', 110]], Tool_R: [['x', -92]] } },
  { t: 0.18, pose: { ...THROWN(0, 70, 0.12, 6), Torso: [['x', -14], ['z', 6]] } },
  { t: 0.34, pose: { ...THROWN(0, 60, 0.12, 6), Torso: [['x', -18], ['z', 6]] } },
  { t: 0.5, pose: NEEDLE_GUARD(0.12) },
];
/** Both arms crossed at the chest, then swept wide apart: a fan of needles. */
const needles_auto4 = [
  { t: 0, pose: NEEDLE_GUARD() },
  { t: 0.12, pose: { ...NEEDLE_GUARD(), Torso: [['x', -10]], Arm_R: [['x', 60], ['z', 60]], Hand_R: [['x', 110]],
    Arm_L: [['x', 60], ['z', -60]], Hand_L: [['x', 110]], Thigh_R: [['x', 40]], Leg_R: [['x', -60]], Thigh_L: [['x', 40]], Leg_L: [['x', -60]] } },
  { t: 0.2, pose: { ...THROWN(-50, 84, 0.1, 0), Arm_L: [['x', 84], ['z', 50]], Hand_L: [['x', 4]], Torso: [['x', 4]] } },
  { t: 0.36, pose: { ...THROWN(-56, 82, 0.1, 0), Arm_L: [['x', 82], ['z', 56]], Hand_L: [['x', 4]], Torso: [['x', 4]] } },
  { t: 0.58, pose: NEEDLE_GUARD(0.1) },
];
/** A hop back, then a throw while landing. */
const needles_dash = [
  { t: 0, pose: NEEDLE_GUARD() },
  { t: 0.12, pose: { ...NEEDLE_GUARD(-0.6), Root: { pos: [0, -0.6, HIP + 0.2] }, ...AIR_LEGS } },
  { t: 0.22, pose: { ...THROWN(-10, 86, -1.2, 6), ...AIR_LEGS } },
  { t: 0.36, pose: THROWN(-12, 84, -1.4, 6) },
  { t: 0.5, pose: NEEDLE_GUARD(-1.4) },
];
const needles_air = [
  { t: 0, pose: { ...NEEDLE_GUARD(), ...AIR_LEGS } },
  { t: 0.1, pose: { ...NEEDLE_GUARD(), Arm_R: [['x', 150], ['z', 20]], Hand_R: [['x', 100]], Tool_R: [['x', -92]], ...AIR_LEGS } },
  { t: 0.18, pose: { ...THROWN(0, 40, 0, 6), Torso: [['x', -24]], ...AIR_LEGS } },
  { t: 0.4, pose: NEEDLE_GUARD() },
];

const anims = {
  fan_idle, fan_auto1, fan_auto2, fan_auto3, fan_auto4, fan_dash, fan_air,
  whip_idle, whip_auto1, whip_auto2, whip_auto3, whip_auto4, whip_dash, whip_air,
  needles_idle, needles_auto1, needles_auto2, needles_auto3, needles_auto4, needles_dash, needles_air,
};
module.exports = { anims, poses: { HIP, look, LEGS_R, LEGS_L, LEGS_SOFT, SWEEP, FAN_GUARD, FAN_LEFT, FAN_OVER, FAN_DRAWN, FAN_LUNGE, AIR_LEGS } };

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'sect_weapons.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project('sect_weapons', anims), null, 1));
  console.log('wrote', out);
}
