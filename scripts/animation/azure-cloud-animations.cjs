// Generates the Azure Cloud Sword Art's animations for the Epic Fight biped (docs/combat/styles/azure-cloud-sword.md).
//   cloud_step_slash: a 5-block dash, low and fast, the blade swept from left to right across the path.
//   cloud_step_dive / cloud_step_riposte: the dash plunging from the air, and the short double cut after a parry.
//   swallow_returns: the secret, a spinning cut from behind the attacker.
//   thousand_drizzle (+ _storm): six quick thrusts and a last pushing one; drizzle_wave: one thrust sending a wave.
//   heaven_splitting_crescent: a rising crescent that leaves the ground; cloud_splitting_descent: the slam from the air.
//   Flowing Cloud Stance's other strikes: rising_cloud, falling_rain_air (the air attack) and wind_step (the sprinting attack).
//   The four default strikes (drifting_cut, returning_cut, piercing_mist, falling_rain) are made in Blockbench:
//   art/blockbench/azure_cloud_m1.bbmodel (azure-m1.cjs), turned into game files by blockbench-to-epicfight.cjs.
//   finish_azure: Last Rain Over the Clouds, the art's finish.
// The hits come from boxes on the root joint (MurimAnimations), not from the blade: these poses only have to read well.
// Usage: node scripts/animation/azure-cloud-animations.cjs [--preview <dir>] [--blade]
// Poses: see pose.cjs (character axes: X right, Y front, Z up; the root's position is in metres, +Y forward).
const fs = require('fs');
const path = require('path');
const b = require('./biped.cjs');
const { arm, locals, write, MURIM_BIPED } = require('./pose.cjs');
const preview = require('./preview.cjs');

/** How far the dash carries the player, in blocks (the root moves this much forward). */
const DASH = 5.0;
/** Standing hip height. */
const HIP = 0.72;

// ---------------------------------------------------------------- Cloud Step Slash

/** Low guard, blade forward and down, weight on the front foot. */
const STANCE = (y = 0) => ({
  Root: { pos: [0, y, HIP] },
  Chest: [['x', -8]],
  Arm_R: [['x', 25], ['y', -10]], Hand_R: [['x', 40]],
  Arm_L: [['x', 10], ['y', 8]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 15]], Leg_R: [['x', -25]],
  Thigh_L: [['x', -5]], Leg_L: [['x', -20]],
});

/** Gathering: body coiled to the left, blade drawn back against the left hip, knees bent. */
const WIND_UP = {
  Root: { pos: [0, -0.1, 0.66], rot: [['z', 20]] },
  Chest: [['x', -15], ['z', 25]], Head: [['z', -20]],
  Arm_R: [['x', 40], ['z', 50]], Hand_R: [['x', 70]],
  Arm_L: [['x', 30], ['y', 20]], Hand_L: [['x', 40]],
  Thigh_R: [['x', 40]], Leg_R: [['x', -60]],
  Thigh_L: [['x', -20]], Leg_L: [['x', -30]],
};

/** Mid-dash: low, leaning into the run, blade straight ahead. */
const DASH_MID = (y = DASH * 0.5, z = 0.62) => ({
  Root: { pos: [0, y, z], rot: [['z', 5]] },
  Chest: [['x', -28], ['z', 5]], Head: [['x', 18]],
  Arm_R: [['x', 85]], Hand_R: [['x', 10]],
  Arm_L: [['x', -20], ['y', 30]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 50]], Leg_R: [['x', -40]],
  Thigh_L: [['x', -35]], Leg_L: [['x', -50]],
});

/** End of the dash: the blade has swept across to the right, the body turned with it, in a deep lunge. */
const SWEPT = (y, z = 0.6) => ({
  Root: { pos: [0, y, z], rot: [['z', -25]] },
  Chest: [['x', -25], ['z', -35]], Head: [['x', 15], ['z', 30]],
  Arm_R: [['x', 80], ['z', -70], ['y', -20]], Hand_R: [['x', 5]],
  Arm_L: [['x', 20], ['y', 25]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 55]], Leg_R: [['x', -50]],
  Thigh_L: [['x', -40]], Leg_L: [['x', -20]],
});

// ---------------------------------------------------------------- the blade as the arm's extension

/**
 * The right arm reaching out at `pitch` (0 hanging, 90 forward, 180 overhead) and turned `yaw` degrees toward the left
 * (negative: to the right), the wrist bent so the blade carries the arm's line (`bend` adds to that, + lifts the tip).
 */
const blade = (pitch, yaw = 0, bend = 0) => ({
  Arm_R: [['x', pitch], ['z', yaw]], Hand_R: [['x', -85 + bend]],
});

/** The Azure Cloud ready stance: light, sideways, blade forward at chest height, the left hand open by the hip. */
const GUARD = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.03], rot: [['z', -12]] },
  Chest: [['x', -6], ['z', 10]], Head: [['z', 6]],
  ...blade(55, -8, 45),
  Arm_L: [['x', -10], ['y', 22]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 18]], Leg_R: [['x', -22]],
  Thigh_L: [['x', -12]], Leg_L: [['x', -18]],
});

/** Feet apart, weight forward: the legs of a cut. */
const LUNGE_LEGS = {
  Thigh_R: [['x', 42]], Leg_R: [['x', -38]],
  Thigh_L: [['x', -28]], Leg_L: [['x', -14]],
};

/** A horizontal cut at shoulder height: blade swept `yaw` (+ left), chest turned with it. */
const CUT = (yaw, y = 0, z = HIP - 0.06, chest = yaw * 0.4) => ({
  Root: { pos: [0, y, z], rot: [['z', chest * 0.4]] },
  Chest: [['x', -12], ['z', chest]], Head: [['z', -chest * 0.6]],
  ...blade(84, yaw, 22),
  Arm_L: [['x', 15], ['y', 30 - chest * 0.3]], Hand_L: [['x', 35]],
  ...LUNGE_LEGS,
});

/** A thrust: arm and blade straight ahead (tilted by `yaw`, `pitch`), the front foot far forward. */
const THRUST = (y, yaw = 0, pitch = 86) => ({
  Root: { pos: [0, y, HIP - 0.1] },
  Chest: [['x', -18], ['z', -12]], Head: [['x', 8], ['z', 10]],
  ...blade(pitch, yaw, 24),
  Arm_L: [['x', -30], ['y', 35]], Hand_L: [['x', 10]],
  Thigh_R: [['x', 60]], Leg_R: [['x', -45]],
  Thigh_L: [['x', -35]], Leg_L: [['x', -10]],
});

/** Drawn back for a thrust: blade by the right hip, point forward. */
const DRAWN = (y, yaw = 0) => ({
  Root: { pos: [0, y, HIP - 0.06], rot: [['z', -18]] },
  Chest: [['x', -8], ['z', -22]], Head: [['z', 18]],
  Arm_R: [['x', 20], ['z', yaw - 25]], Hand_R: [['x', -12]],
  Arm_L: [['x', 40], ['y', 15]], Hand_L: [['x', 40]],
  Thigh_R: [['x', 30]], Leg_R: [['x', -30]],
  Thigh_L: [['x', -15]], Leg_L: [['x', -20]],
});

// ---------------------------------------------------------------- Flowing Cloud Stance: the strikes (M1)

/** Drifting Cut: a quick horizontal cut from right to left. */
const drifting_cut = [
  { t: 0, pose: GUARD() },
  { t: 0.1, pose: CUT(-50, 0, HIP - 0.04, -32) },
  { t: 0.19, pose: CUT(0, 0.15) },
  { t: 0.28, pose: CUT(48, 0.2, HIP - 0.08, 30) },
  { t: 0.42, pose: CUT(55, 0.2, HIP - 0.08, 32) },
  { t: 0.58, pose: GUARD(0.2) },
];

/** Returning Cut: the backhand, from left to right. */
const returning_cut = [
  { t: 0, pose: GUARD() },
  { t: 0.08, pose: CUT(50, 0, HIP - 0.06, 32) },
  { t: 0.17, pose: CUT(0, 0.12) },
  { t: 0.26, pose: CUT(-48, 0.2, HIP - 0.08, -30) },
  { t: 0.4, pose: CUT(-55, 0.2, HIP - 0.08, -32) },
  { t: 0.55, pose: GUARD(0.2) },
];

/** Piercing Mist: drawn back, then a long thrust with a step. */
const piercing_mist = [
  { t: 0, pose: GUARD() },
  { t: 0.12, pose: DRAWN(-0.05) },
  { t: 0.22, pose: THRUST(0.55) },
  { t: 0.38, pose: THRUST(0.6) },
  { t: 0.6, pose: GUARD(0.6) },
];

/** Low, blade down by the right foot, ready to rise. */
const LOW = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.14], rot: [['z', -15]] },
  Chest: [['x', -25], ['z', -15]], Head: [['x', 15], ['z', 10]],
  Arm_R: [['x', 5], ['z', -25]], Hand_R: [['x', -20]],
  Arm_L: [['x', 25], ['y', 20]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 60]], Leg_R: [['x', -70]],
  Thigh_L: [['x', -10]], Leg_L: [['x', -40]],
});

/** High: blade risen over the head, body stretched upward on the toes. */
const HIGH = (y = 0, z = HIP + 0.04) => ({
  Root: { pos: [0, y, z] },
  Chest: [['x', 12]], Head: [['x', -15]],
  ...blade(168, 5, 20),
  Arm_L: [['x', 40], ['y', 30]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 10]], Leg_R: [['x', -5]],
  Thigh_L: [['x', -20]], Leg_L: [['x', -25]],
});

/** Rising Cloud: from low by the foot to high overhead, a cut that lifts. */
const rising_cloud = [
  { t: 0, pose: GUARD() },
  { t: 0.12, pose: LOW(0) },
  { t: 0.2, pose: { ...CUT(0, 0.15, HIP - 0.06, 0), ...blade(80, 0, 0) } },
  { t: 0.28, pose: HIGH(0.2) },
  { t: 0.42, pose: HIGH(0.2) },
  { t: 0.6, pose: GUARD(0.2) },
];

/** Overhead, both hands on the hilt, blade back. */
const OVERHEAD = (y = 0, z = HIP) => ({
  Root: { pos: [0, y, z] },
  Chest: [['x', 10]], Head: [['x', -8]],
  ...blade(172, 0, 70),
  Arm_L: [['x', 165], ['y', -12]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 20]], Leg_R: [['x', -15]],
  Thigh_L: [['x', -15]], Leg_L: [['x', -15]],
});

/** The cut has fallen: blade low in front, body folded over it, knees bent. */
const FALLEN = (y = 0, z = HIP - 0.14) => ({
  Root: { pos: [0, y, z] },
  Chest: [['x', -32]], Head: [['x', 20]],
  ...blade(66, 0, 12),
  Arm_L: [['x', 48], ['y', -8]], Hand_L: [['x', 10]],
  Thigh_R: [['x', 55]], Leg_R: [['x', -60]],
  Thigh_L: [['x', -25]], Leg_L: [['x', -40]],
});

/** Falling Rain: overhead, then straight down. Ends the M1 chain (knocks down). */
const falling_rain = [
  { t: 0, pose: GUARD() },
  { t: 0.14, pose: OVERHEAD(0) },
  { t: 0.25, pose: FALLEN(0.3) },
  { t: 0.45, pose: FALLEN(0.3) },
  { t: 0.68, pose: GUARD(0.3) },
];

/** Same cut from the air: legs drawn up, no step. */
const AIR_LEGS = { Thigh_R: [['x', 70]], Leg_R: [['x', -90]], Thigh_L: [['x', 40]], Leg_L: [['x', -100]] };
const falling_rain_air = [
  { t: 0, pose: { ...OVERHEAD(0), ...AIR_LEGS } },
  { t: 0.12, pose: { ...OVERHEAD(0), ...AIR_LEGS } },
  { t: 0.24, pose: { ...FALLEN(0, HIP - 0.05), ...AIR_LEGS } },
  { t: 0.5, pose: FALLEN(0) },
  { t: 0.72, pose: GUARD(0) },
];

/** Wind Step: a long sliding step forward with a diagonal cut (the sprinting attack). */
const wind_step = [
  { t: 0, pose: GUARD() },
  { t: 0.1, pose: { ...DASH_MID(0.3, 0.64), ...blade(150, -35, 20) } },
  { t: 0.22, pose: { ...CUT(10, 1.3, 0.62, 10), ...blade(110, -10, 0) } },
  { t: 0.32, pose: { ...CUT(45, 1.9, 0.6, 30), ...blade(45, 40, -10) } },
  { t: 0.46, pose: { ...CUT(50, 2.0, 0.62, 32), ...blade(45, 45, -10) } },
  { t: 0.65, pose: GUARD(2.0) },
];

// ---------------------------------------------------------------- Cloud Step Slash variants

/** In the air: the dash plunges forward and down (4 blocks ahead, 2.5 down), the blade leading. */
const DIVE = 4.0, DROP = 2.5;
const cloud_step_dive = [
  { t: 0, pose: { ...STANCE(0), ...AIR_LEGS } },
  { t: 0.12, pose: { ...WIND_UP, ...AIR_LEGS } },
  { t: 0.24, pose: { ...DASH_MID(DIVE * 0.5, HIP - DROP * 0.45), Chest: [['x', -45]], ...blade(60, 0, -20) } },
  { t: 0.4, pose: { ...SWEPT(DIVE * 0.96, HIP - DROP * 0.97) } },
  { t: 0.6, pose: SWEPT(DIVE, HIP - DROP) },
  { t: 0.95, pose: STANCE(DIVE) },
].map(k => ({ ...k, pose: { ...k.pose, Root: { ...k.pose.Root, pos: [0, k.pose.Root.pos[1], k.pose.Root.pos[2]] } } }));

/** After a perfect parry: a short dash (2.5 blocks) that cuts twice, left then right. */
const RIPOSTE_DASH = 2.5;
const cloud_step_riposte = [
  { t: 0, pose: GUARD() },
  { t: 0.08, pose: WIND_UP },
  { t: 0.17, pose: { ...CUT(80, RIPOSTE_DASH * 0.45, 0.62, 30) } },
  { t: 0.26, pose: { ...CUT(-70, RIPOSTE_DASH * 0.8, 0.62, -30) } },
  { t: 0.35, pose: { ...CUT(70, RIPOSTE_DASH, 0.62, 30) } },
  { t: 0.44, pose: SWEPT(RIPOSTE_DASH) },
  { t: 0.7, pose: GUARD(RIPOSTE_DASH) },
];

/** Swallow Returns: arriving behind the attacker still turned away, the body spins into a wide cut. */
const spin = (deg, pose) => ({ ...pose, Root: { ...pose.Root, rot: [['z', deg]] } });
const swallow_returns = [
  { t: 0, pose: spin(150, { ...LOW(0) }) },
  { t: 0.1, pose: spin(70, CUT(-60, 0, HIP - 0.08, -20)) },
  { t: 0.2, pose: spin(0, CUT(30, 0.1, HIP - 0.08, 20)) },
  { t: 0.3, pose: spin(-25, CUT(85, 0.15, HIP - 0.1, 38)) },
  { t: 0.42, pose: spin(-25, CUT(88, 0.15, HIP - 0.1, 40)) },
  { t: 0.62, pose: GUARD(0.15) },
];

// ---------------------------------------------------------------- Thousand Drizzle

/** Six quick thrusts (each a little to the side of the last), then a deep one that pushes. */
const DRIZZLE_YAWS = [0, 12, -10, 8, -14, 4];
const thousand_drizzle = (() => {
  const keys = [{ t: 0, pose: GUARD() }, { t: 0.12, pose: DRAWN(0) }];
  DRIZZLE_YAWS.forEach((yaw, i) => {
    const t = 0.2 + i * 0.12;
    keys.push({ t, pose: THRUST(0.25, yaw, 84 + (i % 2) * 6) });
    keys.push({ t: t + 0.06, pose: DRAWN(0.2, yaw) });
  });
  keys.push({ t: 0.95, pose: { ...DRAWN(0.15), Root: { pos: [0, 0.1, HIP - 0.12], rot: [['z', -30]] }, Chest: [['x', -10], ['z', -35]] } });
  keys.push({ t: 1.05, pose: THRUST(0.9) });
  keys.push({ t: 1.25, pose: THRUST(0.95) });
  keys.push({ t: 1.5, pose: GUARD(0.95) });
  return keys;
})();

/** At mid-distance: one thrust that sends the wave of Qi down the line of the blade. */
const drizzle_wave = [
  { t: 0, pose: GUARD() },
  { t: 0.12, pose: DRAWN(-0.05) },
  { t: 0.24, pose: THRUST(0.35) },
  { t: 0.42, pose: THRUST(0.4) },
  { t: 0.62, pose: GUARD(0.4) },
];

// ---------------------------------------------------------------- Heaven-Splitting Crescent

/** Crouched deep, blade low behind the right leg: the crescent starts from the ground. */
const COIL = {
  Root: { pos: [0, 0, HIP - 0.22], rot: [['z', -20]] },
  Chest: [['x', -30], ['z', -20]], Head: [['x', 20], ['z', 15]],
  Arm_R: [['x', -25], ['z', -30]], Hand_R: [['x', -15]],
  Arm_L: [['x', 35], ['y', 25]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 75]], Leg_R: [['x', -95]],
  Thigh_L: [['x', 10]], Leg_L: [['x', -60]],
};
/** Airborne after the crescent: blade high, knees drawn up. */
const RISEN = { ...HIGH(0.3, HIP + 0.08), ...AIR_LEGS };
const heaven_splitting_crescent = [
  { t: 0, pose: GUARD() },
  { t: 0.14, pose: COIL },
  { t: 0.24, pose: { ...CUT(0, 0.2, HIP - 0.02, 0), ...blade(85, 0, 0), Chest: [['x', 5]] } },
  { t: 0.34, pose: RISEN },
  { t: 0.6, pose: RISEN },
  { t: 0.9, pose: { ...HIGH(0.3), ...AIR_LEGS, ...blade(120, 0, 20) } },
];

/** Cloud-Splitting Descent: in the air, blade raised in both hands, then driven down as the body slams the ground. */
const PLANT = {
  Root: { pos: [0, 0.2, HIP - 0.3] },
  Chest: [['x', -40]], Head: [['x', 25]],
  Arm_R: [['x', 40]], Hand_R: [['x', -115]],
  Arm_L: [['x', 42], ['y', -10]], Hand_L: [['x', -40]],
  Thigh_R: [['x', 85]], Leg_R: [['x', -110]],
  Thigh_L: [['x', -5]], Leg_L: [['x', -80]],
};
const cloud_splitting_descent = [
  { t: 0, pose: { ...OVERHEAD(0, HIP + 0.05), ...AIR_LEGS } },
  { t: 0.14, pose: { ...OVERHEAD(0, HIP + 0.05), ...AIR_LEGS } },
  { t: 0.22, pose: { ...FALLEN(0.1, HIP - 0.1), ...AIR_LEGS } },
  { t: 0.28, pose: PLANT },
  { t: 0.55, pose: PLANT },
  { t: 0.85, pose: GUARD(0.2) },
];

// ---------------------------------------------------------------- the finish: Last Rain Over the Clouds

/** The blade pointed straight at the sky, the left hand on the hip. */
const TO_THE_SKY = {
  Root: { pos: [0, 0, HIP] },
  Chest: [['x', 6]], Head: [['x', -25]],
  ...blade(176, 0, 0),
  Arm_L: [['x', -5], ['y', 35]], Hand_L: [['x', 70]],
  Thigh_R: [['y', -6]], Thigh_L: [['y', 6]],
};
const GATHER = {
  Root: { pos: [0, 0, HIP - 0.3] },
  Chest: [['x', -20]], Head: [['x', -10]],
  ...blade(165, 0, 60),
  Arm_L: [['x', 160], ['y', -15]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 80]], Leg_R: [['x', -105]],
  Thigh_L: [['x', 75]], Leg_L: [['x', -100]],
};
const LEAP = { ...OVERHEAD(0, HIP + 0.9), ...AIR_LEGS, ...blade(178, 0, 85) };
const CLEAVE = { ...FALLEN(0.15, HIP + 0.25), ...AIR_LEGS, ...blade(70, 0, -20) };
const KNEEL = {
  Root: { pos: [0, 0.15, HIP - 0.34] },
  Chest: [['x', -30]], Head: [['x', 15]],
  Arm_R: [['x', 45]], Hand_R: [['x', -120]],
  Arm_L: [['x', 45], ['y', -10]], Hand_L: [['x', -35]],
  Thigh_R: [['x', 90]], Leg_R: [['x', -90]],
  Thigh_L: [['x', 0]], Leg_L: [['x', -95]],
};
const finish_azure = [
  // The blow lands at 1.5 s on the server (AzureCloudSword.FINISH_HIT_TICK); the cut is drawn a little before.
  { t: 0, pose: {} },
  { t: 0.4, pose: TO_THE_SKY },
  { t: 1.0, pose: { ...TO_THE_SKY, Head: [['x', -30]] } },
  { t: 1.12, pose: GATHER },
  { t: 1.26, pose: LEAP },
  { t: 1.4, pose: CLEAVE },
  { t: 1.46, pose: KNEEL },
  { t: 1.85, pose: KNEEL },
  { t: 2.2, pose: {} },
];

// ---------------------------------------------------------------- writing

const anims = {
  cloud_step_slash: [
    { t: 0, pose: STANCE(0) },
    { t: 0.14, pose: WIND_UP },
    { t: 0.26, pose: DASH_MID() },
    { t: 0.42, pose: SWEPT(DASH * 0.96) },
    { t: 0.62, pose: SWEPT(DASH) },
    { t: 1.0, pose: STANCE(DASH) },
  ],
  cloud_step_dive, cloud_step_riposte, swallow_returns,
  thousand_drizzle, thousand_drizzle_storm: thousand_drizzle, drizzle_wave,
  heaven_splitting_crescent, cloud_splitting_descent,
  // drifting_cut, returning_cut, piercing_mist and falling_rain come from Blockbench (azure-m1.cjs, blockbench-to-epicfight.cjs).
  rising_cloud, falling_rain_air, wind_step,
  finish_azure,
};

/** Where the blade's tip is at each key (--blade): to check a cut passes in front of the body. */
function bladeReport(name, keys) {
  for (const k of keys) {
    const m = b.modelSpace(arm, locals(k.pose));
    const t = m.Tool_R, o = b.origin(t), dir = [-t[2], -t[6], -t[10]];
    const tip = o.map((v, i) => v + dir[i] * 1.05);
    console.log(name.padEnd(26), String(k.t).padEnd(5), 'tip', tip.map(v => v.toFixed(2)).join(', '));
  }
}

/**
 * Epic Fight turns a hit box on the root joint with the root's rotation in the animation: a body turned by its root
 * swings its box sideways (a 5-block dash box turned 25 degrees reaches 2 blocks to the side). So the root stays facing
 * forward and its turn goes to the torso (hips forward, the upper body turns), except where the turn is the point:
 * Swallow Returns spins, and its box spins with it, all around.
 */
const SPINS = new Set(['swallow_returns']);
function flattenRoot(pose) {
  const root = pose.Root || {};
  if (!root.rot || root.rot.length === 0) return pose;
  return { ...pose, Root: { ...root, rot: [] }, Torso: [...root.rot, ...(pose.Torso || [])] };
}

const previewDir = process.argv.includes('--preview') ? process.argv[process.argv.indexOf('--preview') + 1] : null;
for (const [name, raw] of Object.entries(anims)) {
  const keys = SPINS.has(name) ? raw : raw.map(k => ({ ...k, pose: flattenRoot(k.pose) }));
  write(MURIM_BIPED, name, keys);
  if (process.argv.includes('--blade')) bladeReport(name, keys);
  if (previewDir) {
    fs.mkdirSync(previewDir, { recursive: true });
    // The preview pins the root forward position, so the pose is readable on one image.
    keys.forEach((k, i) => {
      const root = k.pose.Root || {};
      const pinned = { ...k.pose, Root: { ...root, pos: root.pos ? [root.pos[0], 0, root.pos[2]] : undefined } };
      preview.render(path.join(previewDir, `${name}-${i}.png`), [{ arm, locals: locals(pinned) }]);
    });
  }
}
console.log('wrote', Object.keys(anims).length, 'animation(s) to', MURIM_BIPED);
