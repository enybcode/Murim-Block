// Epic Fight biped animations (from the installed Epic Fight jar, or our own JSON files) as keys for the Blockbench rig:
// to look at Epic Fight's own attacks next to ours. Epic Fight's animations are its authors' assets: what this makes from
// them stays in build/ or a scratch folder, never in the repository.
//   node scripts/animation/epicfight-to-blockbench.cjs <out.bbmodel> <name=jar-path-or-file> ...
//   e.g. sword_auto1=assets/epicfight/animmodels/animations/biped/combat/sword_auto1.json
const fs = require('fs');
const b = require('./biped.cjs');
const { project, BONES, ROOT_BIND } = require('./blockbench-rig.cjs');

const arm = b.loadArmature();
const bind = b.modelSpace(arm, {});
const rot3 = m => [m[0], m[1], m[2], m[4], m[5], m[6], m[8], m[9], m[10]];
const rot4 = m => [m[0], m[1], m[2], 0, m[3], m[4], m[5], 0, m[6], m[7], m[8], 0, 0, 0, 0, 1];
const only4 = m => [m[0], m[1], m[2], 0, m[4], m[5], m[6], 0, m[8], m[9], m[10], 0, 0, 0, 0, 1];

/**
 * The rotation r (character axes, in the parent's frame) a joint's local matrix holds: the inverse of pose.cjs, where
 * local = jointLocal * B^-1 * r * B (B the joint's bind rotation in model space), and for the root local = T(pos) * r * jointLocal.
 */
function rotationFromLocal(name, local) {
  const j = arm.joints[name];
  if (name === 'Root') return rot3(b.mul(only4(local), b.invRigid(only4(j.local))));
  const B = only4(bind[name]);
  return rot3(b.mul(B, b.mul(b.invRigid(only4(j.local)), b.mul(only4(local), b.invRigid(B)))));
}

/** An Epic Fight animation JSON as keys [{t, rots, pos}] for the rig (the root's ground position relative to its start). */
function keysOf(json) {
  const tracks = Object.fromEntries(json.animation.map(a => [a.name, a]));
  const times = [...new Set(json.animation.flatMap(a => a.time))].sort((x, y) => x - y);
  const at = (track, t) => {
    // Nearest key at or before t (the files key every joint at the same times; good enough to look at).
    let i = 0;
    while (i + 1 < track.time.length && track.time[i + 1] <= t + 1e-6) i++;
    return track.transform[i];
  };
  const first = tracks.Root.transform[0];
  return times.map(t => {
    const rots = {};
    for (const bone of BONES) if (tracks[bone]) rots[bone] = rotationFromLocal(bone, at(tracks[bone], t));
    const m = at(tracks.Root, t);
    const pos = [m[3] - first[3], m[7] - first[7] + ROOT_BIND[1], m[11]];
    return { t, rots, pos };
  });
}

if (require.main === module) {
  const [out, ...specs] = process.argv.slice(2);
  const anims = {};
  for (const s of specs) {
    const [name, src] = s.split('=');
    const text = fs.existsSync(src) ? fs.readFileSync(src, 'utf8') : b.readFromJar(src);
    anims[name] = keysOf(JSON.parse(text));
  }
  fs.writeFileSync(out, JSON.stringify(project('epicfight_reference', anims)));
  console.log('wrote', out, Object.keys(anims).join(', '));
}

module.exports = { keysOf, rotationFromLocal };
void rot4;
