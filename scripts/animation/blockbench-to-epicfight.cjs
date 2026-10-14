// Turns animations of a Blockbench project made on the Epic Fight rig (blockbench-rig.cjs) into Epic Fight animations,
// as Blockbench plays them: every bone's Euler angles (and the root's position) interpolated the way Blockbench does
// (linearly, angle by angle), sampled every 1/40 s, written to assets/murimblock/animmodels/animations/biped/murim.
//   node scripts/animation/blockbench-to-epicfight.cjs <project.bbmodel> [animation ...]
// Without names, every animation of the project. What Blockbench shows is what the game plays.
const fs = require('fs');
const { fromBlockbenchEuler, ROOT_BIND } = require('./blockbench-rig.cjs');
const { localsFromRotations, writeFrames, MURIM_BIPED } = require('./pose.cjs');

const STEP = 1 / 40;

/** A channel's value at time t: linear between keyframes, held before the first and after the last. */
function sample(keyframes, t) {
  const ks = keyframes.slice().sort((a, b) => a.time - b.time);
  if (ks.length === 0) return null;
  const v = k => { const d = k.data_points[0]; return [Number(d.x) || 0, Number(d.y) || 0, Number(d.z) || 0]; };
  if (t <= ks[0].time) return v(ks[0]);
  for (let i = 0; i + 1 < ks.length; i++) {
    const a = ks[i], b = ks[i + 1];
    if (t <= b.time) {
      const f = (t - a.time) / Math.max(1e-9, b.time - a.time), va = v(a), vb = v(b);
      return va.map((x, n) => x + (vb[n] - x) * f);
    }
  }
  return v(ks[ks.length - 1]);
}

function convert(model, animation) {
  const groups = {};
  (function walk(nodes) { for (const n of nodes) if (typeof n === 'object') { groups[n.uuid] = n.name; walk(n.children); } })(model.outliner);
  const tracks = {};
  for (const [id, a] of Object.entries(animation.animators || {})) {
    const name = groups[id] || a.name;
    tracks[name] = {
      rotation: (a.keyframes || []).filter(k => k.channel === 'rotation'),
      position: (a.keyframes || []).filter(k => k.channel === 'position'),
    };
  }
  const times = [];
  for (let t = 0; t < animation.length - 1e-9; t += STEP) times.push(Math.round(t * 1000) / 1000);
  times.push(Math.round(animation.length * 1000) / 1000);
  const frames = times.map(t => {
    const rots = {};
    for (const [bone, tr] of Object.entries(tracks)) {
      const e = sample(tr.rotation, t);
      if (e) rots[bone] = fromBlockbenchEuler(e);
    }
    let rootPos = ROOT_BIND;
    const p = tracks.Root && sample(tracks.Root.position, t);
    if (p) {
      // Blockbench negates x for positions; back from pixels and Blockbench axes (x, y up, z back) to Epic Fight's.
      const [bx, by, bz] = [-p[0], p[1], p[2]];
      rootPos = [ROOT_BIND[0] + bx / 16, ROOT_BIND[1] - bz / 16, ROOT_BIND[2] + by / 16];
    }
    return localsFromRotations(rots, rootPos);
  });
  return { times, frames };
}

if (require.main === module) {
  const [file, ...names] = process.argv.slice(2);
  const model = JSON.parse(fs.readFileSync(file, 'utf8'));
  for (const a of model.animations || []) {
    if (names.length && !names.includes(a.name)) continue;
    const { times, frames } = convert(model, a);
    writeFrames(MURIM_BIPED, a.name, times, frames);
    console.log('wrote', a.name, times.length, 'frames,', a.length, 's');
  }
}

module.exports = { convert, sample };
