// Writing Epic Fight biped animations from poses (shared by the Murim animation scripts).
//
// A pose gives joint rotations relative to the parent, in the character's own axes as if it stood in the bind pose:
// X = its right, Y = its front, Z = up along the spine. For a limb hanging down, +X swings it forward, -Y raises a
// right limb sideways (+Y a left one). The root takes a position (model space, metres: +Y is forward) and rotations.
const fs = require('fs');
const path = require('path');
const b = require('./biped.cjs');

const arm = b.loadArmature();
const bind = b.modelSpace(arm, {});
const rotOnly = m => [m[0], m[1], m[2], 0, m[4], m[5], m[6], 0, m[8], m[9], m[10], 0, 0, 0, 0, 1];
const AXES = { x: [1, 0, 0], y: [0, 1, 0], z: [0, 0, 1] };

/** Local matrices of every joint for a pose {Root: {pos, rot}, Joint: [[axis, deg], ...]}. */
function locals(pose) {
  const out = {};
  for (const name of arm.order) {
    const j = arm.joints[name];
    const spec = name === 'Root' ? (pose.Root && pose.Root.rot) || [] : pose[name] || [];
    let r = b.rotation([1, 0, 0], 0);
    for (const [axis, deg] of spec) r = b.mul(b.rotation(AXES[axis], deg), r);
    const B = rotOnly(bind[name]);
    if (name === 'Root') {
      const pos = (pose.Root && pose.Root.pos) || b.origin(bind.Root);
      out[name] = b.mul(b.translation(...pos), b.mul(r, rotOnly(j.local)));
    } else {
      out[name] = b.mul(j.local, b.mul(b.invRigid(B), b.mul(r, B)));
    }
  }
  return out;
}

const round = m => m.map(v => Math.abs(v) < 5e-7 ? 0 : Math.round(v * 1e6) / 1e6);

/** Writes an Epic Fight animation from keyframes [{t, pose}] (seconds) into a directory; returns the frames. */
function write(dir, name, keys) {
  const frames = keys.map(k => locals(k.pose));
  const json = {
    animation: arm.order.map(joint => ({
      name: joint,
      time: keys.map(k => k.t),
      transform: frames.map(f => round(f[joint])),
    })),
  };
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name + '.json'), JSON.stringify(json));
  return frames;
}

/** Where Murim's player animations live. */
const MURIM_BIPED = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock', 'animmodels', 'animations', 'biped', 'murim');

module.exports = { arm, bind, locals, write, MURIM_BIPED };
