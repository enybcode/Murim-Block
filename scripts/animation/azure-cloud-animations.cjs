// Generates the Azure Cloud Sword Art's animations for the Epic Fight biped (docs/combat/styles/azure-cloud-sword.md).
//   cloud_step_slash: a 5-block dash, low and fast, the blade swept from left to right across the path.
// Usage: node scripts/animation/azure-cloud-animations.cjs [--preview <dir>]
// Poses: see pose.cjs (character axes: X right, Y front, Z up; the root's position is in metres, +Y forward).
const fs = require('fs');
const path = require('path');
const { arm, write, MURIM_BIPED } = require('./pose.cjs');
const preview = require('./preview.cjs');

/** How far the dash carries the player, in blocks (the root moves this much forward). */
const DASH = 5.0;

// ---------------------------------------------------------------- Cloud Step Slash

/** Low guard, blade forward and down, weight on the front foot. */
const STANCE = (y = 0) => ({
  Root: { pos: [0, y, 0.72] },
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
const DASH_MID = {
  Root: { pos: [0, DASH * 0.5, 0.62], rot: [['z', 5]] },
  Chest: [['x', -28], ['z', 5]], Head: [['x', 18]],
  Arm_R: [['x', 85]], Hand_R: [['x', 10]],
  Arm_L: [['x', -20], ['y', 30]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 50]], Leg_R: [['x', -40]],
  Thigh_L: [['x', -35]], Leg_L: [['x', -50]],
};

/** End of the dash: the blade has swept across to the right, the body turned with it, in a deep lunge. */
const SWEPT = (y) => ({
  Root: { pos: [0, y, 0.6], rot: [['z', -25]] },
  Chest: [['x', -25], ['z', -35]], Head: [['x', 15], ['z', 30]],
  Arm_R: [['x', 80], ['z', -70], ['y', -20]], Hand_R: [['x', 5]],
  Arm_L: [['x', 20], ['y', 25]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 55]], Leg_R: [['x', -50]],
  Thigh_L: [['x', -40]], Leg_L: [['x', -20]],
});

const anims = {
  cloud_step_slash: [
    { t: 0, pose: STANCE(0) },
    { t: 0.14, pose: WIND_UP },
    { t: 0.26, pose: DASH_MID },
    { t: 0.42, pose: SWEPT(DASH * 0.96) },
    { t: 0.62, pose: SWEPT(DASH) },
    { t: 1.0, pose: STANCE(DASH) },
  ],
};

const previewDir = process.argv.includes('--preview') ? process.argv[process.argv.indexOf('--preview') + 1] : null;
for (const [name, keys] of Object.entries(anims)) {
  const frames = write(MURIM_BIPED, name, keys);
  if (previewDir) {
    fs.mkdirSync(previewDir, { recursive: true });
    // The preview pins the root forward position, so the pose is readable on one image.
    keys.forEach((k, i) => {
      const pinned = { ...k.pose, Root: { ...(k.pose.Root || {}), pos: k.pose.Root && k.pose.Root.pos ? [k.pose.Root.pos[0], 0, k.pose.Root.pos[2]] : undefined } };
      preview.render(path.join(previewDir, `${name}-${i}.png`), [{ arm, locals: require('./pose.cjs').locals(pinned) }]);
    });
  }
}
console.log('wrote', Object.keys(anims).length, 'animation(s) to', MURIM_BIPED);
