// Generates the Murim knock-down animations for the Epic Fight biped:
//   downed_idle, downed_crawl, carry_idle, carry_walk, carried, finish_generic.
// Usage: node scripts/animation/knockdown-animations.cjs [--preview <dir>]
//
// Poses are written as joint rotations relative to the parent, in the character's own axes as if it stood in the bind
// pose: X = its right, Y = its front, Z = up along the spine. For a limb hanging down, +X swings it forward, -Y raises a
// right limb sideways (+Y a left one). The root takes a position (model space, metres) and rotations about its origin.
const fs = require('fs');
const path = require('path');
const preview = require('./preview.cjs');

const { arm, locals, write: writeTo, MURIM_BIPED: OUT } = require('./pose.cjs');
const write = (name, keys) => writeTo(OUT, name, keys);

// ---------------------------------------------------------------- poses

const STAND = {};

/** Lying on the belly, the hips a little behind the entity's origin so the body is centred on its small hitbox. */
const proneRoot = (extra = []) => ({ pos: [0, -0.3, 0.16], rot: [['x', -90], ...extra] });

const DOWNED_IDLE = {
  Root: proneRoot(),
  Chest: [['x', 10]],
  Head: [['x', 14]],
  // Propped on the forearms: upper arms under the shoulders, forearms flat forward.
  Arm_R: [['x', 85], ['y', 8]], Hand_R: [['x', 85]],
  Arm_L: [['x', 85], ['y', -8]], Hand_L: [['x', 85]],
  Thigh_R: [['y', -7]], Thigh_L: [['y', 7]],
  Leg_R: [['x', -10]], Leg_L: [['x', -10]],
};

/** One half of the crawl: right arm reaching far ahead, left arm pulling under the chest, right knee drawn up. */
function crawl(side) {
  const s = side === 'R' ? 1 : -1, o = side === 'R' ? 'L' : 'R';
  return {
    Root: proneRoot([['z', 5 * s]]),
    Chest: [['x', 10], ['z', -4 * s]],
    Head: [['x', 14], ['z', -3 * s]],
    ['Arm_' + side]: [['x', 165], ['y', -10 * s]], ['Hand_' + side]: [['x', 12]],
    ['Arm_' + o]: [['x', 50], ['y', 12 * s]], ['Hand_' + o]: [['x', 70]],
    ['Thigh_' + side]: [['x', 12], ['y', -42 * s]], ['Leg_' + side]: [['x', -55]],
    ['Thigh_' + o]: [['y', 8 * s]], ['Leg_' + o]: [['x', -8]],
  };
}

/** Carrying someone over the right shoulder: the right arm wraps their legs in front, the body leans a little. */
const carryArms = (swing = 0) => ({
  Chest: [['x', -6]],
  Arm_R: [['x', 30], ['y', 4]], Hand_R: [['x', 105]],
  Arm_L: [['x', swing], ['y', 6]], Hand_L: [['x', 10]],
});
const CARRY_IDLE = { ...carryArms(4) };

function step(phase) {
  // phase 0: right foot ahead; 0.5: left foot ahead; 0.25 / 0.75: passing, swing knee bent.
  const legs = {
    0: { Thigh_R: [['x', 24]], Leg_R: [['x', -4]], Thigh_L: [['x', -20]], Leg_L: [['x', -18]], z: 0.745 },
    0.25: { Thigh_R: [['x', 0]], Leg_R: [['x', -6]], Thigh_L: [['x', 18]], Leg_L: [['x', -42]], z: 0.77 },
    0.5: { Thigh_L: [['x', 24]], Leg_L: [['x', -4]], Thigh_R: [['x', -20]], Leg_R: [['x', -18]], z: 0.745 },
    0.75: { Thigh_L: [['x', 0]], Leg_L: [['x', -6]], Thigh_R: [['x', 18]], Leg_R: [['x', -42]], z: 0.77 },
  }[phase];
  const swing = phase === 0 ? 14 : phase === 0.5 ? -10 : 2;
  const { z, ...joints } = legs;
  return { Root: { pos: [0, 0, z] }, ...carryArms(swing), ...joints };
}

/**
 * Carried over a shoulder, relative to the carried player's own origin (where riding puts it): hips on the carrier's
 * right shoulder, the body folded at the hips, head and arms hanging down the carrier's back, legs down their front.
 * Riding puts that origin 1.8 above the carrier's feet; CARRIED_OFFSET moves the hips onto the top of the shoulder
 * (about 1.6 high), and the carrier's right forearm holds the thighs.
 */
const CARRIED_OFFSET = [0.24, 0.05, -0.2];
const carried = (sway = 0) => ({
  Root: { pos: CARRIED_OFFSET, rot: [['y', 180], ['x', -40 + sway]] },
  Chest: [['x', -6]],
  Head: [['x', -20 + sway]],
  Arm_R: [['x', 160 + sway * 2]], Hand_R: [['x', 10]],
  Arm_L: [['x', 168 - sway * 2]], Hand_L: [['x', 6]],
  Thigh_R: [['x', 112]], Leg_R: [['x', 30]],
  Thigh_L: [['x', 118]], Leg_L: [['x', 22]],
});

/** Generic finish: the weapon raised high in both hands, then driven down into the downed player in front. */
const FINISH_RAISE = {
  Root: { pos: [0, 0, 0.76] }, Chest: [['x', 10]], Head: [['x', 8]],
  Arm_R: [['x', 168], ['y', 10]], Hand_R: [['x', 25]],
  Arm_L: [['x', 168], ['y', -10]], Hand_L: [['x', 25]],
};
const FINISH_STRIKE = {
  Root: { pos: [0, 0.1, 0.58] }, Chest: [['x', -30]], Head: [['x', -15]],
  Arm_R: [['x', 55], ['y', 8]], Hand_R: [['x', 15]],
  Arm_L: [['x', 55], ['y', -8]], Hand_L: [['x', 15]],
  Thigh_R: [['x', 45]], Leg_R: [['x', -70]], Thigh_L: [['x', 10]], Leg_L: [['x', -60]],
};

// ---------------------------------------------------------------- animations

const anims = {
  downed_idle: [{ t: 0, pose: DOWNED_IDLE }, { t: 1.5, pose: { ...DOWNED_IDLE, Chest: [['x', 12]] } }, { t: 3, pose: DOWNED_IDLE }],
  downed_crawl: [{ t: 0, pose: crawl('R') }, { t: 0.7, pose: crawl('L') }, { t: 1.4, pose: crawl('R') }],
  carry_idle: [{ t: 0, pose: CARRY_IDLE }, { t: 1.2, pose: { ...CARRY_IDLE, Chest: [['x', -4]] } }, { t: 2.4, pose: CARRY_IDLE }],
  carry_walk: [0, 0.25, 0.5, 0.75, 1].map(p => ({ t: p * 0.9, pose: step(p === 1 ? 0 : p) })),
  carried: [{ t: 0, pose: carried(0) }, { t: 1, pose: carried(4) }, { t: 2, pose: carried(0) }],
  finish_generic: [
    // The blow lands at 0.9 s on the server (KnockdownService.FINISH_HIT_TICK); the strike is drawn a little before,
    // since the animation reaches other clients a tick or two after it starts.
    { t: 0, pose: STAND }, { t: 0.4, pose: FINISH_RAISE }, { t: 0.62, pose: FINISH_RAISE },
    { t: 0.78, pose: FINISH_STRIKE }, { t: 1.25, pose: FINISH_STRIKE }, { t: 1.7, pose: STAND },
  ],
};

const previewDir = process.argv.includes('--preview') ? process.argv[process.argv.indexOf('--preview') + 1] : null;
for (const [name, keys] of Object.entries(anims)) {
  const frames = write(name, keys);
  if (previewDir) {
    fs.mkdirSync(previewDir, { recursive: true });
    keys.forEach((k, i) => preview.render(path.join(previewDir, `${name}-${i}.png`), [{ arm, locals: frames[i] }], name));
  }
}
if (previewDir) {
  // The carry pair together: carrier at the origin, the carried player where riding puts its origin (top of the head).
  preview.render(path.join(previewDir, 'carry-pair.png'), [
    { arm, locals: locals(CARRY_IDLE) },
    { arm, locals: locals(carried(0)), offset: [0, 0, 1.8], color: [40, 150, 60] },
  ]);
}
console.log('wrote', Object.keys(anims).length, 'animations to', OUT);
