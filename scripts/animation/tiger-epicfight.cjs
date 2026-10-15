// The tiger for Epic Fight: its mesh and armature (assets/murimblock/animmodels/entity/tiger.json) and its animations
// (animmodels/animations/tiger/<name>.json), from the Blockbench project (art/blockbench/tiger.bbmodel, written by
// tiger-model.cjs and tiger-animations.cjs), and its texture (textures/entity/tiger.png). What Blockbench shows is what
// the game plays.
//
// Axes. A joint's own frame is Blockbench's (x, y up, z toward the back; the tiger faces -z), in blocks; the Root joint
// turns it into Epic Fight's model space (z up, the front toward +y), as the crab's armature does. Every joint sits at
// its group's pivot with no rotation of its own, so a joint's local matrix is its offset from its parent's pivot (plus
// any animated position) and the bone's rotation, built as Blockbench does: Rz(z) * Ry(-y) * Rx(-x). A group's rest
// rotation (the tail's droop) is added to the animated angles, as Blockbench adds them.
// The pounce's leap is left to the game (the tiger is thrown forward), so its forward and upward travel is dropped here.
//
//   node scripts/animation/tiger-epicfight.cjs
const fs = require('node:fs');
const path = require('node:path');
const { sample } = require('./blockbench-to-epicfight.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const model = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel'), 'utf8'));
const STEP = 1 / 40;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the skeleton
const ROOT_M = [1, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0, 0, 0, 0, 0, 1];   // Blockbench axes -> Epic Fight's (x, -z, y)
const joints = ['Root'];
const info = { Root: { pivot: [0, 0, 0], rest: [0, 0, 0], parent: null, children: [] } };
const boneOf = {};
(function walk(nodes, parent) {
  for (const n of nodes) {
    if (typeof n !== 'object') { boneOf[n] = parent; continue; }
    joints.push(n.name);
    info[n.name] = { pivot: n.origin, rest: n.rotation || [0, 0, 0], parent, children: [] };
    info[parent].children.push(n.name);
    walk(n.children || [], n.name);
  }
})(model.outliner, 'Root');

const r6 = v => Math.round(v * 1e6) / 1e6;
const offset = name => { const j = info[name], p = info[j.parent]; return j.pivot.map((v, k) => (v - p.pivot[k]) / 16); };
const rad = d => d * Math.PI / 180;
function rotation([x, y, z]) {
  const cx = Math.cos(rad(-x)), sx = Math.sin(rad(-x)), cy = Math.cos(rad(-y)), sy = Math.sin(rad(-y)), cz = Math.cos(rad(z)), sz = Math.sin(rad(z));
  const Rx = [1, 0, 0, 0, cx, -sx, 0, sx, cx], Ry = [cy, 0, sy, 0, 1, 0, -sy, 0, cy], Rz = [cz, -sz, 0, sz, cz, 0, 0, 0, 1];
  const mul = (a, b) => { const r = []; for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) r.push(a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j]); return r; };
  return mul(Rz, mul(Ry, Rx));
}
/** A joint's local matrix (row-major 4 x 4, translation in the last column). */
function local(name, euler = [0, 0, 0], pos = [0, 0, 0]) {
  if (name === 'Root') return ROOT_M;
  const r = rotation(euler), t = offset(name).map((v, k) => v + pos[k] / 16);
  return [r[0], r[1], r[2], t[0], r[3], r[4], r[5], t[1], r[6], r[7], r[8], t[2], 0, 0, 0, 1].map(r6);
}
const hierarchy = name => ({ name, transform: local(name), children: info[name].children.map(hierarchy) });

// ---------------------------------------------------------------- the mesh
const W = model.resolution.width, H = model.resolution.height;
const toMesh = ([x, y, z]) => [x / 16, -z / 16, y / 16];
const positions = [], uvs = [], normals = [], vcounts = [], vindices = [], tris = [];
const NORMAL = { north: [0, 1, 0], south: [0, -1, 0], east: [1, 0, 0], west: [-1, 0, 0], up: [0, 0, 1], down: [0, 0, -1] };
function corners(e, f) {
  const [x0, y0, z0] = e.from, [x1, y1, z1] = e.to;
  switch (f) {
    case 'north': return [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]];
    case 'south': return [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]];
    case 'east': return [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]];
    case 'west': return [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]];
    case 'up': return [[x1, y1, z1], [x0, y1, z1], [x0, y1, z0], [x1, y1, z0]];
    default: return [[x1, y0, z0], [x0, y0, z0], [x0, y0, z1], [x1, y0, z1]];
  }
}
for (const e of model.elements) {
  if (e.rotation.some(v => v)) throw new Error(e.name + ' is turned: bake turns into bones');
  const joint = joints.indexOf(boneOf[e.uuid]);
  if (joint < 0) throw new Error(e.name + ' has no bone');
  const ids = new Map();
  const vertex = c => {
    const k = c.join(',');
    if (!ids.has(k)) { positions.push(...toMesh(c).map(r6)); vcounts.push(1); vindices.push(joint, 0); ids.set(k, positions.length / 3 - 1); }
    return ids.get(k);
  };
  for (const f of FACES) {
    const face = e.faces[f];
    if (!face || face.texture === null) continue;
    const vs = corners(e, f).map(vertex), [u0, v0, u1, v1] = face.uv, base = uvs.length / 2;
    for (const [u, v] of [[u0, v0], [u1, v0], [u1, v1], [u0, v1]]) uvs.push(r6(u / W), r6(v / H));
    const n = NORMAL[f];
    let ni = -1;
    for (let i = 0; i < normals.length; i += 3) if (normals[i] === n[0] && normals[i + 1] === n[1] && normals[i + 2] === n[2]) ni = i / 3;
    if (ni < 0) { normals.push(...n); ni = normals.length / 3 - 1; }
    for (const k of [0, 3, 2, 0, 2, 1]) tris.push(vs[k], base + k, ni);
  }
}
const block = (array, stride) => ({ stride, count: array.length / stride, array });
const entity = {
  vertices: { positions: block(positions, 3), uvs: block(uvs, 2), normals: block(normals, 3), vcounts: block(vcounts, 1),
    weights: block([1], 1), vindices: block(vindices, 1), parts: { noGroups: block(tris, 3) } },
  armature: { joints, hierarchy: [hierarchy('Root')] },
  fps: 24,
};
fs.mkdirSync(path.join(ASSETS, 'animmodels', 'entity'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'animmodels', 'entity', 'tiger.json'), JSON.stringify(entity));
fs.mkdirSync(path.join(ASSETS, 'textures', 'entity'), { recursive: true });
fs.copyFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.png'), path.join(ASSETS, 'textures', 'entity', 'tiger.png'));

// ---------------------------------------------------------------- the animations
const dir = path.join(ASSETS, 'animmodels', 'animations', 'tiger');
fs.mkdirSync(dir, { recursive: true });
for (const a of model.animations) {
  const tracks = {};
  for (const [id, an] of Object.entries(a.animators)) {
    const name = joints.find(j => model.outliner && j === an.name) || an.name;
    tracks[name] = { rotation: an.keyframes.filter(k => k.channel === 'rotation'), position: an.keyframes.filter(k => k.channel === 'position') };
  }
  const times = [];
  for (let t = 0; t < a.length - 1e-9; t += STEP) times.push(Math.round(t * 1000) / 1000);
  times.push(Math.round(a.length * 1000) / 1000);
  const animation = joints.map(name => ({
    name, time: times,
    transform: times.map(t => {
      const tr = tracks[name];
      const e = (tr && sample(tr.rotation, t)) || [0, 0, 0];
      let p = (tr && sample(tr.position, t)) || [0, 0, 0];
      p = [-p[0], p[1], p[2]];                                      // Blockbench shows positions with x negated
      if (a.name === 'pounce' && name === 'body') p = [p[0], Math.min(0, p[1]), 0];
      return local(name, e.map((v, k) => v + info[name].rest[k]), p);
    }),
  }));
  fs.writeFileSync(path.join(dir, a.name + '.json'), JSON.stringify({ animation }));
  console.log('tiger/' + a.name, times.length, 'frames,', a.length, 's');
}
console.log('tiger:', joints.length, 'joints,', positions.length / 3, 'vertices,', tris.length / 9, 'triangles');
