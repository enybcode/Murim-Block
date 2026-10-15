// Scarlet Bloom, the Sanguine Art's finish (docs/combat/styles/sanguine-art.md): the executor's own motion, 7.8 s.
//   0.00  ready, bare hands;
//   0.35  the right arm drawn high behind, knees bent;
//   0.62  down on the right knee, the right hand driven into the ground (the crystals erupt at 0.70 s, the body flies);
//   1.40  rising, the head lifted to follow the body into the sky;
//   2.10  the right hand raised open to the sky, the left low: the dorsal crystals weave their cage (to 3.40 s);
//   3.40  the cage closes, slowly (black and white): the hand closes with it, to 4.85;
//   5.00  the fist pulled down hard: the bloom bursts the cage, held in the slow motion; the body falls at 5.60 and
//         lands, executed, at 6.10;
//   6.40  the fist lowered, the head bowed to the crater, held to the end.
// scarlet_bloom_flung: the victim's own motion while thrown into the sky (see below).
// Usage: node scripts/animation/scarlet-bloom.cjs  (writes animmodels/animations/biped/murim/scarlet_bloom*.json and
// art/blockbench/scarlet_bloom.bbmodel). Pose notation: see azure-m1.cjs (x its right, y its front, z up; Arm x swings
// the arm forward and up, Hand x bends the elbow, Thigh x swings the leg forward, Leg x bends the knee back).
const fs = require('fs');
const path = require('path');
const { project, ROOT_BIND } = require('./blockbench-rig.cjs');
const { convert } = require('./blockbench-to-epicfight.cjs');
const { writeFrames, MURIM_BIPED } = require('./pose.cjs');

const HIP = ROOT_BIND[2];

const READY = {
  Root: { pos: [0, 0, HIP - 0.04] }, Torso: [['x', -4]], Chest: [['x', -2]], Head: [['x', 4]],
  Arm_R: [['x', 30], ['z', -10]], Hand_R: [['x', 60]], Arm_L: [['x', 30], ['z', 10]], Hand_L: [['x', 60]],
  Thigh_R: [['x', 12], ['y', -6]], Leg_R: [['x', -20]], Thigh_L: [['x', -6], ['y', 6]], Leg_L: [['x', -10]],
};
const WIND_UP = {
  Root: { pos: [0, -0.05, HIP - 0.14] }, Torso: [['x', 10], ['z', -12]], Chest: [['x', 4]], Head: [['x', -4], ['z', 10]],
  Arm_R: [['x', 165], ['z', -22]], Hand_R: [['x', 30]], Arm_L: [['x', 50], ['z', 18]], Hand_L: [['x', 70]],
  Thigh_R: [['x', 42], ['y', -10]], Leg_R: [['x', -62]], Thigh_L: [['x', 30], ['y', 10]], Leg_L: [['x', -58]],
};
// Down on the right knee, bent over, the right hand in the ground ahead, the left arm thrown back.
const PLUNGE = {
  Root: { pos: [0, 0.18, 0.36] }, Torso: [['x', -56]], Chest: [['x', -8]], Head: [['x', 40]],
  Arm_R: [['x', 72], ['z', 4]], Hand_R: [['x', 4]], Arm_L: [['x', -34], ['z', 24]], Hand_L: [['x', 20]],
  Thigh_R: [['x', -8]], Leg_R: [['x', -104]], Thigh_L: [['x', 84]], Leg_L: [['x', -98]],
};
const RISING = {
  Root: { pos: [0, 0.05, HIP - 0.08] }, Torso: [['x', 2]], Chest: [['x', 2]], Head: [['x', -34]],
  Arm_R: [['x', 120], ['z', -10]], Hand_R: [['x', 20]], Arm_L: [['x', 20], ['z', 22]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 20], ['y', -14]], Leg_R: [['x', -30]], Thigh_L: [['x', 4], ['y', 14]], Leg_L: [['x', -14]],
};
// The hand open to the sky, leaning back a little, the stance wide.
const RAISED = (tremble = 0) => ({
  Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['x', 10 + tremble]], Chest: [['x', 4]], Head: [['x', -42 - tremble]],
  Arm_R: [['x', 172 - tremble], ['z', -14]], Hand_R: [['x', 2]], Arm_L: [['x', 16], ['z', 28]], Hand_L: [['x', 24]],
  Thigh_R: [['x', 18], ['y', -18]], Leg_R: [['x', -24]], Thigh_L: [['x', -4], ['y', 18]], Leg_L: [['x', -12]],
});
const CLOSE = {
  ...RAISED(0), Torso: [['x', -2]], Head: [['x', -28]], Arm_R: [['x', 150], ['z', -12]], Hand_R: [['x', 70]],
};
const PULL = {
  Root: { pos: [0, 0.04, HIP - 0.12] }, Torso: [['x', -16], ['z', 8]], Chest: [['x', -4]], Head: [['x', -18]],
  Arm_R: [['x', 104], ['z', -4]], Hand_R: [['x', 112]], Arm_L: [['x', 10], ['z', 26]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 30], ['y', -16]], Leg_R: [['x', -46]], Thigh_L: [['x', 8], ['y', 16]], Leg_L: [['x', -30]],
};
const AFTER = {
  Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['x', -6]], Chest: [['x', -2]], Head: [['x', 18]],
  Arm_R: [['x', 58], ['z', -6]], Hand_R: [['x', 92]], Arm_L: [['x', 8], ['z', 14]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 12], ['y', -10]], Leg_R: [['x', -14]], Thigh_L: [['x', -2], ['y', 10]], Leg_L: [['x', -8]],
};

const scarlet_bloom = [
  { t: 0, pose: READY },
  { t: 0.35, pose: WIND_UP },
  { t: 0.62, pose: PLUNGE },
  { t: 1.0, pose: PLUNGE },
  { t: 1.4, pose: RISING },
  { t: 2.1, pose: RAISED(0) },
  { t: 2.8, pose: RAISED(3) },
  { t: 3.4, pose: RAISED(-2) },
  // The cage closes (in black and white): the hand closes with it, slowly.
  { t: 4.2, pose: { ...RAISED(1), Arm_R: [['x', 164], ['z', -13]], Hand_R: [['x', 34]] } },
  { t: 4.85, pose: CLOSE },
  { t: 5.0, pose: PULL },
  { t: 5.6, pose: PULL },
  { t: 6.4, pose: AFTER },
  { t: 7.8, pose: AFTER },
];

// ---------------------------------------------------------------- the victim, swept into the sky (4.3 s)
// Played on the body from the eruption (0.7 s into the finish) to the burst (5.0 s). Swept off the ground by the great
// crystal: the whole body thrown over backwards, the legs flung ahead, the arms torn back past the head, the head
// whipping, the limbs flailing as it goes up; then, hanging in the cage, it rights itself a little and hangs spread,
// swaying; as the cage closes (from 2.8 s) it curls up on itself until it bursts.
const SWEPT = (back, flail) => ({
  Root: { pos: [0, 0, HIP], rot: [['x', back]] }, Torso: [['x', 18 + flail * 6]], Chest: [['x', 12]],
  Head: [['x', -46 + flail * 28], ['z', flail * 14]],
  Arm_R: [['x', 205 - flail * 35], ['z', -34 - flail * 30]], Hand_R: [['x', 10 + Math.abs(flail) * 40]],
  Arm_L: [['x', 205 + flail * 35], ['z', 34 - flail * 30]], Hand_L: [['x', 10 + Math.abs(flail) * 30]],
  Thigh_R: [['x', 62 + flail * 26], ['y', -18]], Leg_R: [['x', -26 - Math.max(0, flail) * 50]],
  Thigh_L: [['x', 62 - flail * 26], ['y', 18]], Leg_L: [['x', -26 - Math.max(0, -flail) * 50]],
});
const SPREAD = (sway = 0) => ({
  Root: { pos: [0, 0, HIP], rot: [['x', 14 + sway]] }, Torso: [['x', 12 + sway]], Chest: [['x', 4]], Head: [['x', -30 - sway * 2], ['z', sway * 4]],
  Arm_R: [['x', 24], ['z', -86 - sway]], Hand_R: [['x', 14]], Arm_L: [['x', 24], ['z', 86 + sway]], Hand_L: [['x', 14]],
  Thigh_R: [['x', -4], ['y', -22]], Leg_R: [['x', -20]], Thigh_L: [['x', -4], ['y', 22]], Leg_L: [['x', -20]],
});
const CURLED = {
  Root: { pos: [0, 0, HIP], rot: [['x', -10]] }, Torso: [['x', -34]], Chest: [['x', -14]], Head: [['x', 30]],
  Arm_R: [['x', 70], ['z', 20]], Hand_R: [['x', 120]], Arm_L: [['x', 70], ['z', -20]], Hand_L: [['x', 120]],
  Thigh_R: [['x', 96], ['y', -8]], Leg_R: [['x', -120]], Thigh_L: [['x', 96], ['y', 8]], Leg_L: [['x', -120]],
};
const scarlet_bloom_flung = [
  { t: 0, pose: SWEPT(20, 0) },
  { t: 0.15, pose: SWEPT(55, 1) },
  { t: 0.35, pose: SWEPT(70, -1) },
  { t: 0.55, pose: SWEPT(62, 0.9) },
  { t: 0.75, pose: SWEPT(74, -0.8) },
  { t: 0.95, pose: SWEPT(58, 0.7) },
  { t: 1.15, pose: SWEPT(48, -0.5) },
  { t: 1.5, pose: SPREAD(0) },
  { t: 2.1, pose: SPREAD(5) },
  { t: 2.8, pose: SPREAD(-3) },
  { t: 3.9, pose: CURLED },
  { t: 4.3, pose: CURLED },
];

module.exports = { scarlet_bloom, scarlet_bloom_flung };

if (require.main === module) {
  const proj = project('scarlet_bloom', { scarlet_bloom, scarlet_bloom_flung });
  // Bare hands: the rig's sword taken out (the Tool_R bone stays).
  const blade = new Set();
  (function walk(nodes, inTool) {
    for (const n of nodes) {
      if (typeof n === 'string') { if (inTool) blade.add(n); continue; }
      walk(n.children, inTool || n.name === 'Tool_R');
      if (n.name === 'Tool_R') n.children = n.children.filter(c => typeof c !== 'string');
    }
  })(proj.outliner, false);
  proj.elements = proj.elements.filter(e => !blade.has(e.uuid));
  const art = path.join(__dirname, '..', '..', 'art', 'blockbench', 'scarlet_bloom.bbmodel');
  fs.writeFileSync(art, JSON.stringify(proj, null, 1));
  for (const clip of proj.animations) {
    const { times, frames } = convert(proj, clip);
    writeFrames(MURIM_BIPED, clip.name, times, frames);
    console.log('wrote', clip.name + ':', times.length, 'frames,', clip.length, 's');
  }
}
