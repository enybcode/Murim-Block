// The Long Fist (docs/combat/styles/long-fist.md), its techniques as a Blockbench project (art/blockbench/long_fist.bbmodel).
// Its M1 are Epic Fight's own bare-hand strikes; these are the rest, bare-handed:
//   long_fist_guard     the guard: forearms up before the face, fists closed, knees soft;
//   long_fist_guard_hit a blow taken on that guard: the forearms pushed back, the body giving a little;
//   tiger_step_punch    crouched low like a tiger, the right fist cocked at the hip, the left hand open toward the target;
//                       a bound of 3 blocks along the ground, landing in a deep lunge with the right fist thrown out
//                       (tiger_step_punch_long: the same bound over 5 blocks, Tiger Leap);
//   tiger_step_dive     in the air: both fists forward, then one punch driven down at the ground;
//   rapid_fists         rooted in the horse stance, five alternate straight punches, then a step and a whole-body punch
//                       (rapid_fists_storm: seven, Hundred Fists);
//   rapid_fists_air     three punches, then both fists joined and brought down on the target (hammer fist);
//   whirlwind_kick      the tornado kick of the Long Fist (xuanfeng jiao): a step, a leap, a whole turn in the air with the
//                       right leg straight out, landing crouched, a hand on the ground (whirlwind_kick_great: Tornado Kick);
//   whirlwind_descent   in the air: the same turn coming down, the kick sweeping round as the body lands;
//   inch_force          the secret: the fist almost on the chest, a dry twist of the hips, and the body frozen, arm out;
//   final_palm          the finish: a wide stance, a slow breath with the arms, down on one knee, one palm on the chest.
// Usage: node scripts/animation/long-fist.cjs, then node scripts/animation/blockbench-to-epicfight.cjs art/blockbench/long_fist.bbmodel
//
// Pose notation: see azure-m1.cjs (character axes: x its right, y its front, z up; Arm x swings the arm forward and up,
// Hand x bends the elbow, Thigh x swings the leg forward, Leg x bends the knee back). A fist cocked at the hip: the upper
// arm back (Arm x -28), the elbow bent (Hand x 105). The root moves the body (y forward, z up); it only turns (rot z) in
// the whirlwind, whose hit box is a ring all around anyway.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

const HIP = 0.764;
/** How far Tiger Step Punch carries the player (blocks). */
const BOUND = 3.0, BOUND_LONG = 5.0;
/** Tiger Step Punch from the air: how far forward and down the dive takes the player (blocks). */
const DIVE = 3.0, DROP = 2.0;

const COCKED_R = { Arm_R: [['x', -28], ['z', -6]], Hand_R: [['x', 105]] };
const COCKED_L = { Arm_L: [['x', -28], ['z', 6]], Hand_L: [['x', 105]] };
const PUNCH_R = { Arm_R: [['x', 90], ['z', 4]], Hand_R: [['x', 0]] };
const PUNCH_L = { Arm_L: [['x', 90], ['z', -4]], Hand_L: [['x', 0]] };

/** The guard: forearms up before the face, the left a little ahead, knees soft, the right foot back. */
const GUARD = (y = 0) => ({
  Root: { pos: [0, y, HIP - 0.06] },
  Torso: [['x', -6], ['z', -10]], Chest: [['x', -4]], Head: [['x', 8], ['z', 10]],
  Arm_R: [['x', 50], ['z', 18]], Hand_R: [['x', 112]],
  Arm_L: [['x', 62], ['z', -10]], Hand_L: [['x', 92]],
  Thigh_R: [['x', -16], ['y', -4]], Leg_R: [['x', -18]],
  Thigh_L: [['x', 22], ['y', 4]], Leg_L: [['x', -24]],
});

/** The horse stance: feet wide, knees bent and out, the trunk upright; the arms are added by the caller. */
const HORSE = (y = 0, turn = 0) => ({
  Root: { pos: [0, y, 0.58] },
  Torso: [['x', -6], ['z', turn]], Chest: [['x', -2], ['z', turn * 0.3]], Head: [['x', 6], ['z', -turn * 1.1]],
  Thigh_R: [['x', 55], ['y', -32]], Leg_R: [['x', -72]],
  Thigh_L: [['x', 55], ['y', 32]], Leg_L: [['x', -72]],
});

/** The bow stance at the end of a bound or a step: right leg bent ahead, left leg straight behind, right fist out. */
const LUNGE = (y, settle = 0) => ({
  Root: { pos: [0, y, 0.52 + settle * 0.03] },
  Torso: [['x', -16 + settle * 4], ['z', 24]], Chest: [['x', -4], ['z', 8]], Head: [['x', 18 - settle * 4], ['z', -28]],
  ...PUNCH_R, ...COCKED_L,
  Thigh_R: [['x', 66]], Leg_R: [['x', -68]],
  Thigh_L: [['x', -36]], Leg_L: [['x', -6]],
});

// ---------------------------------------------------------------- the guard

const long_fist_guard = [
  { t: 0, pose: GUARD() },
  { t: 0.6, pose: { ...GUARD(), Root: { pos: [0, 0, HIP - 0.075] }, Torso: [['x', -8], ['z', -10]] } },
  { t: 1.2, pose: GUARD() },
];

/** A blow taken on the guard: the forearms pushed back, the body giving a little, then the guard again. */
const long_fist_guard_hit = [
  { t: 0, pose: GUARD() },
  { t: 0.06, pose: { ...GUARD(-0.12), Torso: [['x', 6], ['z', -10]], Head: [['x', -6], ['z', 10]],
    Arm_R: [['x', 60], ['z', 22]], Hand_R: [['x', 122]], Arm_L: [['x', 70], ['z', -14]], Hand_L: [['x', 110]] } },
  { t: 0.35, pose: GUARD(-0.12) },
];

// ---------------------------------------------------------------- Tiger Step Punch

/** Crouched low like a tiger, leaning in: the right fist cocked at the hip, the left hand open toward the target. */
const COIL = {
  Root: { pos: [0, 0, 0.48] },
  Torso: [['x', -40], ['z', 18]], Chest: [['x', -6]], Head: [['x', 42], ['z', -18]],
  ...COCKED_R, Arm_L: [['x', 76], ['z', -6]], Hand_L: [['x', 14]],
  Thigh_R: [['x', -30]], Leg_R: [['x', -62]],
  Thigh_L: [['x', 82]], Leg_L: [['x', -102]],
};

/** Thrown forward low, the back leg driving, the front knee drawn up. */
const BOUNDING = (y) => ({
  Root: { pos: [0, y, 0.62] },
  Torso: [['x', -52], ['z', 14]], Chest: [['x', -6]], Head: [['x', 50], ['z', -14]],
  ...COCKED_R, Arm_L: [['x', 84], ['z', -4]], Hand_L: [['x', 6]],
  Thigh_R: [['x', -48]], Leg_R: [['x', -12]],
  Thigh_L: [['x', 72]], Leg_L: [['x', -92]],
});

/** Coming down, the legs gathering under the body, the fist starting out. */
const GATHER = (y) => ({
  Root: { pos: [0, y, 0.62] },
  Torso: [['x', -38], ['z', 16]], Chest: [['x', -6]], Head: [['x', 36], ['z', -16]],
  Arm_R: [['x', 30], ['z', 2]], Hand_R: [['x', 70]], Arm_L: [['x', 40], ['z', -4]], Hand_L: [['x', 60]],
  Thigh_R: [['x', 50]], Leg_R: [['x', -82]],
  Thigh_L: [['x', 40]], Leg_L: [['x', -70]],
});

/** The bound over `bound` blocks; the evolved one (Tiger Leap, 5 blocks) takes the same time, only longer strides. */
const bounding = (bound) => [
  { t: 0, pose: GUARD() },
  { t: 0.1, pose: COIL },
  { t: 0.16, pose: BOUNDING(bound * 0.23) },
  { t: 0.24, pose: GATHER(bound * 0.73) },
  // Landing in the lunge, the punch thrown with it (hit 0.24 to 0.36).
  { t: 0.3, pose: LUNGE(bound) },
  { t: 0.56, pose: LUNGE(bound, 1) },
  { t: 0.78, pose: GUARD(bound) },
];
const tiger_step_punch = bounding(BOUND);
const tiger_step_punch_long = bounding(BOUND_LONG);

/** In the air: both fists forward, the body pitching over, then one punch driven down; landing crouched. */
const tiger_step_dive = [
  { t: 0, pose: { Root: { pos: [0, 0, HIP] }, Torso: [['x', -40]], Chest: [['x', -4]], Head: [['x', 36]],
    Arm_R: [['x', 100]], Hand_R: [['x', 22]], Arm_L: [['x', 100]], Hand_L: [['x', 22]],
    Thigh_R: [['x', 62]], Leg_R: [['x', -92]], Thigh_L: [['x', 62]], Leg_L: [['x', -92]] } },
  { t: 0.12, pose: { Root: { pos: [0, DIVE * 0.35, HIP - DROP * 0.3] }, Torso: [['x', -68]], Chest: [['x', -6]], Head: [['x', 58]],
    ...PUNCH_R, ...COCKED_L,
    Thigh_R: [['x', 30]], Leg_R: [['x', -42]], Thigh_L: [['x', 30]], Leg_L: [['x', -42]] } },
  { t: 0.26, pose: { Root: { pos: [0, DIVE, 0.5 - DROP] }, Torso: [['x', -50], ['z', 12]], Chest: [['x', -6]], Head: [['x', 44], ['z', -12]],
    Arm_R: [['x', 64]], Hand_R: [['x', 0]], ...COCKED_L,
    Thigh_R: [['x', 72]], Leg_R: [['x', -112]], Thigh_L: [['x', -12]], Leg_L: [['x', -62]] } },
  { t: 0.6, pose: { Root: { pos: [0, DIVE, 0.52 - DROP] }, Torso: [['x', -44], ['z', 10]], Chest: [['x', -6]], Head: [['x', 40], ['z', -10]],
    Arm_R: [['x', 60]], Hand_R: [['x', 10]], ...COCKED_L,
    Thigh_R: [['x', 70]], Leg_R: [['x', -110]], Thigh_L: [['x', -10]], Leg_L: [['x', -60]] } },
];

// ---------------------------------------------------------------- Rapid Fists

/** One punch of the flurry from the horse stance: the right (side 1) or the left (side -1) fist out, the other cocked. */
const FLURRY = (side) => ({
  ...HORSE(0, 18 * side),
  ...(side > 0 ? { ...PUNCH_R, ...COCKED_L } : { ...PUNCH_L, ...COCKED_R }),
});

/** Both fists back at the hips, the trunk wound to the left before the last punch. */
const WIND = { ...HORSE(0, -26), ...COCKED_R, ...COCKED_L };

/**
 * The flurry of `n` punches: they land every 0.1 s from 0.1 s (their hits in MurimAnimations: RAPID_FIST_GAP), then the
 * trunk winds and the last punch lands 0.24 s after the last of them, with a step and the whole body behind it.
 */
const flurry = (n) => {
  const keys = [{ t: 0, pose: GUARD() }, { t: 0.06, pose: { ...HORSE(0, 0), ...COCKED_R, ...COCKED_L } }];
  for (let i = 0; i < n; i++) keys.push({ t: 0.1 * (i + 1), pose: FLURRY(i % 2 === 0 ? 1 : -1) });
  const last = 0.1 * n;
  keys.push({ t: last + 0.12, pose: WIND }, { t: last + 0.24, pose: LUNGE(0.6) },
      { t: last + 0.5, pose: LUNGE(0.6, 1) }, { t: last + 0.62, pose: GUARD(0.6) });
  return keys.map(k => ({ ...k, t: Math.round(k.t * 100) / 100 }));
};
const rapid_fists = flurry(5);
/** Evolved (Hundred Fists): seven punches. */
const rapid_fists_storm = flurry(7);

/** In the air: legs drawn up, three punches, then both fists joined over the head and brought down. */
const AIR_TUCK = { Thigh_R: [['x', 60]], Leg_R: [['x', -90]], Thigh_L: [['x', 50]], Leg_L: [['x', -80]] };
const AIR_PUNCH = (side) => ({
  Root: { pos: [0, 0, HIP] },
  Torso: [['x', -14], ['z', 16 * side]], Chest: [['x', -4]], Head: [['x', 12], ['z', -18 * side]],
  ...(side > 0 ? { ...PUNCH_R, ...COCKED_L } : { ...PUNCH_L, ...COCKED_R }), ...AIR_TUCK,
});
const HAMMER = (arms, lean) => ({
  Root: { pos: [0, 0, HIP] },
  Torso: [['x', lean]], Chest: [['x', -4]], Head: [['x', -lean * 0.8]],
  Arm_R: [['x', arms], ['z', 12]], Hand_R: [['x', 6]], Arm_L: [['x', arms], ['z', -12]], Hand_L: [['x', 6]], ...AIR_TUCK,
});
const rapid_fists_air = [
  { t: 0, pose: { ...AIR_PUNCH(-1), ...COCKED_R, ...COCKED_L } },
  { t: 0.08, pose: AIR_PUNCH(1) },
  { t: 0.18, pose: AIR_PUNCH(-1) },
  { t: 0.28, pose: AIR_PUNCH(1) },
  // Joined over the head (0.4), then down (hit 0.44 to 0.56).
  { t: 0.4, pose: HAMMER(172, 14) },
  { t: 0.5, pose: HAMMER(70, -40) },
  { t: 0.8, pose: HAMMER(60, -36) },
];

// ---------------------------------------------------------------- Whirlwind Kick

/** A moment of the turn in the air: `turn` degrees round (from 0 to 360), the right leg straight out to the side. */
const TURN = (turn, z, out = 1) => ({
  Root: { pos: [0, 0.3, z], rot: [['z', turn]] },
  Torso: [['x', 8], ['z', 12]], Chest: [['x', 2]], Head: [['x', -4], ['z', -10]],
  Arm_R: [['x', 90], ['z', 70]], Hand_R: [['x', 30]], Arm_L: [['x', 96], ['z', 54]], Hand_L: [['x', 40]],
  Thigh_R: [['x', 6], ['y', -84 * out]], Leg_R: [['x', -4 - (1 - out) * 50]],
  Thigh_L: [['x', 70]], Leg_L: [['x', -96]],
});

/** Landed: crouched low, the left hand down on the ground, the turn complete. */
const KNEEL_DOWN = (turn) => ({
  Root: { pos: [0, 0.3, 0.42], rot: [['z', turn]] },
  Torso: [['x', -34], ['z', -6]], Chest: [['x', -4]], Head: [['x', 30], ['z', 6]],
  Arm_R: [['x', 24], ['z', -72]], Hand_R: [['x', 24]], Arm_L: [['x', 40], ['z', -6]], Hand_L: [['x', 0]],
  Thigh_R: [['x', 86]], Leg_R: [['x', -122]],
  Thigh_L: [['x', -26]], Leg_L: [['x', -70]],
});

// The kick sweeps round from 0.26 to 0.52 (its ring hit box in MurimAnimations); Blockbench turns each key less than 180.
const whirlwind_kick = [
  { t: 0, pose: GUARD() },
  { t: 0.1, pose: { Root: { pos: [0, 0.3, 0.62], rot: [['z', -60]] }, Torso: [['x', -10], ['z', -20]], Chest: [['x', -4]], Head: [['x', 8], ['z', 40]],
    Arm_R: [['x', 40], ['z', -60]], Hand_R: [['x', 40]], Arm_L: [['x', 60], ['z', -40]], Hand_L: [['x', 40]],
    Thigh_R: [['x', -20]], Leg_R: [['x', -20]], Thigh_L: [['x', 40]], Leg_L: [['x', -52]] } },
  { t: 0.2, pose: { ...TURN(0, 1.18, 0.4), Arm_R: [['x', 124], ['z', 30]], Arm_L: [['x', 124], ['z', 50]] } },
  { t: 0.28, pose: TURN(90, 1.26) },
  { t: 0.36, pose: TURN(180, 1.26) },
  { t: 0.44, pose: TURN(270, 1.12) },
  { t: 0.52, pose: TURN(360, 0.92, 0.5) },
  { t: 0.62, pose: KNEEL_DOWN(360) },
  { t: 0.86, pose: KNEEL_DOWN(360) },
];

/** In the air: the same turn coming down (the body falls, AzureCloudSword-style), the kick sweeping round as it lands. */
const whirlwind_descent = [
  { t: 0, pose: TURN(0, HIP, 0.5) },
  { t: 0.08, pose: TURN(120, HIP) },
  { t: 0.16, pose: TURN(240, HIP) },
  { t: 0.26, pose: TURN(360, HIP - 0.1) },
  { t: 0.34, pose: KNEEL_DOWN(360) },
  { t: 0.7, pose: KNEEL_DOWN(360) },
];

// ---------------------------------------------------------------- Inch Force (the secret)

/** The fist almost on the chest, the elbow a little bent, the stance short; then the dry twist (hit 0.08 to 0.14). */
const CLOSE = {
  Root: { pos: [0, 0, 0.66] },
  Torso: [['x', -8], ['z', -10]], Chest: [['x', -2], ['z', -4]], Head: [['x', 8], ['z', 12]],
  Arm_R: [['x', 82], ['z', 6]], Hand_R: [['x', 36]], Arm_L: [['x', 52], ['z', -8]], Hand_L: [['x', 92]],
  Thigh_R: [['x', 34]], Leg_R: [['x', -36]], Thigh_L: [['x', -20]], Leg_L: [['x', -10]],
};
const TWIST = {
  ...CLOSE,
  Root: { pos: [0, 0.08, 0.64] },
  Torso: [['x', -10], ['z', 16]], Chest: [['x', -2], ['z', 6]], Head: [['x', 10], ['z', -18]],
  ...PUNCH_R, Arm_L: [['x', 30], ['z', -6]], Hand_L: [['x', 100]],
};
const inch_force = [
  { t: 0, pose: CLOSE },
  { t: 0.07, pose: CLOSE },
  { t: 0.1, pose: TWIST },
  // Frozen, the arm out, while the body flies.
  { t: 0.5, pose: TWIST },
  { t: 0.72, pose: { ...TWIST, Torso: [['x', -8], ['z', 10]], Arm_R: [['x', 70], ['z', 4]], Hand_R: [['x', 30]] } },
];

// ---------------------------------------------------------------- Final Palm (the finish)

const WIDE = (arms) => ({ ...HORSE(0, 0), Head: [['x', 4]], ...arms });
const final_palm = [
  { t: 0, pose: GUARD() },
  // The stance, the arms low and open.
  { t: 0.3, pose: WIDE({ Arm_R: [['x', 18], ['z', -30]], Hand_R: [['x', 20]], Arm_L: [['x', 18], ['z', 30]], Hand_L: [['x', 20]] }) },
  // The breath: the arms rise round to overhead...
  { t: 0.7, pose: WIDE({ Arm_R: [['x', 150], ['z', -36]], Hand_R: [['x', 10]], Arm_L: [['x', 150], ['z', 36]], Hand_L: [['x', 10]], Head: [['x', -12]] }) },
  // ...and come down before the chest.
  { t: 1.0, pose: WIDE({ Arm_R: [['x', 62], ['z', 10]], Hand_R: [['x', 100]], Arm_L: [['x', 62], ['z', -10]], Hand_L: [['x', 100]] }) },
  // Down on the right knee over the downed body, the right palm drawn up by the head.
  { t: 1.22, pose: { Root: { pos: [0, 0.2, 0.34] }, Torso: [['x', -26]], Chest: [['x', -4]], Head: [['x', 30]],
    Arm_R: [['x', 104], ['z', 8]], Hand_R: [['x', 112]], Arm_L: [['x', 40], ['z', -8]], Hand_L: [['x', 90]],
    Thigh_R: [['x', -8]], Leg_R: [['x', -102]], Thigh_L: [['x', 82]], Leg_L: [['x', -96]] } },
  // The palm on the chest (the blow lands at 1.4 s).
  { t: 1.4, pose: { Root: { pos: [0, 0.2, 0.32] }, Torso: [['x', -50]], Chest: [['x', -6]], Head: [['x', 46]],
    Arm_R: [['x', 84], ['z', 4]], Hand_R: [['x', 0]], ...COCKED_L,
    Thigh_R: [['x', -8]], Leg_R: [['x', -104]], Thigh_L: [['x', 84]], Leg_L: [['x', -98]] } },
  { t: 2.0, pose: { Root: { pos: [0, 0.2, 0.34] }, Torso: [['x', -44]], Chest: [['x', -6]], Head: [['x', 40]],
    Arm_R: [['x', 82], ['z', 4]], Hand_R: [['x', 4]], ...COCKED_L,
    Thigh_R: [['x', -8]], Leg_R: [['x', -102]], Thigh_L: [['x', 84]], Leg_L: [['x', -96]] } },
];

const anims = {
  long_fist_guard, long_fist_guard_hit, tiger_step_punch, tiger_step_punch_long, tiger_step_dive, rapid_fists, rapid_fists_storm, rapid_fists_air,
  whirlwind_kick, whirlwind_kick_great: whirlwind_kick, whirlwind_descent, inch_force, final_palm,
};
module.exports = { anims, BOUND, BOUND_LONG, DIVE, DROP };

/**
 * The root only turns about the vertical (the whirlwind). Through a rotation matrix its angle would come back within
 * -180..180, sometimes as another set of Euler angles, and a whole turn would play wrong: its keys are written straight
 * from the pose instead, as Blockbench's own y angle (negated, Bedrock's convention), one turn after another.
 */
function rootTurns(proj) {
  for (const a of proj.animations) {
    const keys = anims[a.name];
    const root = Object.values(a.animators).find(an => an.name === 'Root');
    const rotations = root.keyframes.filter(k => k.channel === 'rotation');
    keys.forEach((k, i) => {
      const turn = ((k.pose.Root && k.pose.Root.rot) || []).reduce((sum, [axis, deg]) => sum + (axis === 'z' ? deg : 0), 0);
      rotations[i].data_points[0] = { x: 0, y: -turn, z: 0 };
    });
  }
  return proj;
}

/** Bare hands: the rig's sword (the cubes of Tool_R) taken out of the project; the bone stays. */
function bareHands(proj) {
  const blade = new Set();
  (function walk(nodes, inTool) {
    for (const n of nodes) {
      if (typeof n === 'string') { if (inTool) blade.add(n); continue; }
      walk(n.children, inTool || n.name === 'Tool_R');
      if (n.name === 'Tool_R') n.children = n.children.filter(c => typeof c !== 'string');
    }
  })(proj.outliner, false);
  proj.elements = proj.elements.filter(e => !blade.has(e.uuid));
  return proj;
}

// LONG_FIST_PREVIEW=<file>: also writes a copy played on the spot (the bound's way forward taken out), to film it.
if (require.main === module && process.env.LONG_FIST_PREVIEW) {
  const still = Object.fromEntries(Object.entries(anims).map(([n, keys]) => [n, keys.map(k => ({ ...k,
    pose: { ...k.pose, Root: { ...(k.pose.Root || {}), pos: k.pose.Root && k.pose.Root.pos ? [0, 0, k.pose.Root.pos[2]] : undefined } } }))]));
  const saved = { ...anims };
  Object.assign(anims, still);
  fs.writeFileSync(process.env.LONG_FIST_PREVIEW, JSON.stringify(bareHands(rootTurns(project('long_fist_preview', still))), null, 1));
  Object.assign(anims, saved);
  console.log('wrote', process.env.LONG_FIST_PREVIEW);
}

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'long_fist.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(bareHands(rootTurns(project("long_fist", anims))), null, 1));
  console.log('wrote', out);
}
