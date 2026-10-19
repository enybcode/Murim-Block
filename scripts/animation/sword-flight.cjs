// Riding a flying sword (docs/cultivation/refonte-cultivation.md, SwordFlight), as a Blockbench project
// (art/blockbench/sword_flight.bbmodel). Enzo, 19/10/2026: "comme s'il faisait du skate"; of four poses tried (skate,
// surf, grab, classic) he chose the skate, a loop with a slight sway:
//   sword_ride   side-on, the feet apart along the blade, knees soft, arms loose and a little out, looking ahead.
// Usage: node scripts/animation/sword-flight.cjs, then node scripts/animation/blockbench-to-epicfight.cjs art/blockbench/sword_flight.bbmodel
//
// Pose notation: see azure-m1.cjs (character axes: x its right, y its front, z up). Side-on, the root is turned 80
// degrees to the left (its right side leads, along the way); the head turns back to look ahead.
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');

/** Turned side-on: the right side leading. */
const SIDE = 80;

/** Skating: `s` 0..1 the sway (knees a touch deeper, arms drifting). */
const SKATE = (s) => ({
  Root: { pos: [0, 0, 0.7 - s * 0.03], rot: [['z', SIDE]] },
  Torso: [['x', -6 - s * 2], ['z', -8]], Chest: [['x', -2]], Head: [['x', 6], ['z', -SIDE + 12]],
  Arm_R: [['x', 12 + s * 4], ['y', -24 - s * 6]], Hand_R: [['x', 22]],
  Arm_L: [['x', 8 - s * 4], ['y', 26 + s * 6]], Hand_L: [['x', 18]],
  Thigh_R: [['x', 24 + s * 4], ['y', -22]], Leg_R: [['x', -38 - s * 8]],
  Thigh_L: [['x', 24 + s * 4], ['y', 22]], Leg_L: [['x', -38 - s * 8]],
});

/** A loop swaying 0 → 1 → 0 over `period` seconds. */
const loop = (pose, period) => [
  { t: 0, pose: pose(0) },
  { t: period / 2, pose: pose(1) },
  { t: period, pose: pose(0) },
];

const anims = {
  sword_ride: loop(SKATE, 2.4),
};
module.exports = { anims };

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

if (require.main === module) {
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'sword_flight.bbmodel');
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(bareHands(project('sword_flight', anims)), null, 1));
  console.log('wrote', out);
}
