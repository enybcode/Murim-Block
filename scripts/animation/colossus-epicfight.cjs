// The Son of the Demon (the Bandit Warlord's second phase, warlord-avatar-3d.cjs) for Epic Fight: his own armature and
// mesh (assets/murimblock/animmodels/entity/son_of_the_demon.json, a HumanoidMesh), his atlas, and his animations
// (animmodels/animations/colossus/*.json).
//   His armature (Enzo, 16/10/2026: "make his own skeleton"): Epic Fight's biped, every joint moved to where his body
//   bends (warlord-avatar-3d.cjs SKELETON: the hips wider, the knees halfway down his doubled legs, the neck on the
//   traps, the shoulders in the deltoids...), its rotations kept: the joints keep their names and axes, so he is driven
//   as any humanoid, and a pose of the biped reads the same on him. The whole body is raised twelve texels so his feet
//   stand at the entity's feet (in Blockbench they hang below).
//   His animations: the poses of warlord-avatar-3d.cjs (pose notation, pose.cjs) turned into this armature's local
//   matrices, every joint turning the shortest way between keys; the root's height lifted to his hips.
// Usage: node scripts/animation/colossus-epicfight.cjs
const fs = require('fs');
const path = require('path');
const b = require('./biped.cjs');
const kit = require('./mesh-kit.cjs');
const avatar = require('./warlord-avatar-3d.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ATLAS = 256;
/** His feet are twelve texels below the biped's: everything is raised by that much. */
const LIFT = 12;

// ---------------------------------------------------------------- the armature
const arm = b.loadArmature();
const bind = b.modelSpace(arm, {});
const fromBB = ([x, y, z]) => [x / 16, -z / 16, y / 16];
const rotOnly = m => [m[0], m[1], m[2], 0, m[4], m[5], m[6], 0, m[8], m[9], m[10], 0, 0, 0, 0, 1];
/** Every joint's model-space matrix: the biped's rotation, at his own place (joints not placed follow their parent). */
const global = {};
for (const name of arm.order) {
  const j = arm.joints[name];
  const place = avatar.SKELETON[name];
  if (place) {
    const [x, y, z] = fromBB([place[0], place[1] + LIFT, place[2]]);
    const m = rotOnly(bind[name]);
    m[3] = x; m[7] = y; m[11] = z;
    global[name] = m;
  } else {
    global[name] = j.parent ? b.mul(global[j.parent], j.local) : bind[name];
  }
}
const local = {};
for (const name of arm.order) {
  const j = arm.joints[name];
  local[name] = j.parent ? b.mul(b.invRigid(global[j.parent]), global[name]) : global[name];
}
const round = m => m.map(v => (Math.abs(v) < 5e-7 ? 0 : Math.round(v * 1e6) / 1e6));
function hierarchy(name) {
  const j = arm.joints[name];
  return { name, transform: round(local[name]), children: j.children.map(hierarchy) };
}
const ROOT_LIFT = b.origin(global.Root)[2] - b.origin(bind.Root)[2];

// ---------------------------------------------------------------- poses on it (pose.cjs's rule, this armature)
const AXES = { x: [1, 0, 0], y: [0, 1, 0], z: [0, 0, 1] };
function locals(pose) {
  const out = {};
  for (const name of arm.order) {
    const spec = name === 'Root' ? (pose.Root && pose.Root.rot) || [] : pose[name] || [];
    let r = b.rotation([1, 0, 0], 0);
    for (const [axis, deg] of spec) r = b.mul(b.rotation(AXES[axis], deg), r);
    const B = rotOnly(global[name]);
    if (name === 'Root') {
      const pos = (pose.Root && pose.Root.pos) || b.origin(bind.Root);
      out[name] = b.mul(b.translation(pos[0], pos[1], pos[2] + ROOT_LIFT), b.mul(r, rotOnly(local.Root)));
    } else {
      out[name] = b.mul(local[name], b.mul(b.invRigid(B), b.mul(r, B)));
    }
  }
  return out;
}
function quat(m) {
  const [a, bb, c, , d, e, f, , g, h, i] = m, tr = a + e + i;
  if (tr > 0) { const k = Math.sqrt(tr + 1) * 2; return [(h - f) / k, (c - g) / k, (d - bb) / k, k / 4]; }
  if (a > e && a > i) { const k = Math.sqrt(1 + a - e - i) * 2; return [k / 4, (bb + d) / k, (c + g) / k, (h - f) / k]; }
  if (e > i) { const k = Math.sqrt(1 + e - a - i) * 2; return [(bb + d) / k, k / 4, (f + h) / k, (c - g) / k]; }
  const k = Math.sqrt(1 + i - a - e) * 2; return [(c + g) / k, (f + h) / k, k / 4, (d - bb) / k];
}
function slerp(p, q, t) {
  let dot = p.reduce((s, v, n) => s + v * q[n], 0);
  if (dot < 0) { q = q.map(v => -v); dot = -dot; }
  if (dot > 0.9995) { const r = p.map((v, n) => v + (q[n] - v) * t), l = Math.hypot(...r); return r.map(v => v / l); }
  const th = Math.acos(dot), sa = Math.sin((1 - t) * th) / Math.sin(th), sb = Math.sin(t * th) / Math.sin(th);
  return p.map((v, n) => v * sa + q[n] * sb);
}
function matrix([x, y, z, w], [tx, ty, tz]) {
  return [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), tx, 2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), ty,
    2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), tz, 0, 0, 0, 1];
}
function write(name, keys) {
  const L = keys.map(k => locals(k.pose)), end = keys[keys.length - 1].t, times = [];
  for (let t = 0; t < end - 1e-9; t += 1 / 40) times.push(Math.round(t * 1000) / 1000);
  for (const k of keys) if (!times.includes(k.t)) times.push(k.t);
  times.sort((x, y) => x - y);
  const frames = times.map(t => {
    let i = 0;
    while (i < keys.length - 2 && t > keys[i + 1].t) i++;
    const f = Math.min(1, Math.max(0, (t - keys[i].t) / (keys[i + 1].t - keys[i].t)));
    const out = {};
    for (const j of arm.order) {
      const a = L[i][j], c = L[i + 1][j];
      out[j] = matrix(slerp(quat(a), quat(c), f), [3, 7, 11].map(n => a[n] + (c[n] - a[n]) * f));
    }
    return out;
  });
  const json = { animation: arm.order.map(joint => ({ name: joint, time: times, transform: frames.map(fr => round(fr[joint])) })) };
  const dir = path.join(ASSETS, 'animmodels', 'animations', 'colossus');
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name + '.json'), JSON.stringify(json));
  console.log('wrote colossus/' + name, times.length, 'frames,', end, 's');
}

// ---------------------------------------------------------------- his moves in game
const { REST, ROAR, HIP } = avatar;
/** A stand-in blow until the Majin Fist's (Enzo gives them one by one): a heavy straight right, a step in. 1.2 s. */
const punch = [
  { t: 0, pose: REST },
  { t: 0.35, pose: { ...REST, Torso: [['x', -4], ['z', -26]], Chest: [['z', -8]], Head: [['z', 26]],
    Arm_R: [['x', -30], ['y', -20]], Hand_R: [['x', 100]], Arm_L: [['x', 40], ['y', 14]], Hand_L: [['x', 40]] } },
  { t: 0.5, pose: { ...REST, Root: { pos: [0, 0.5, HIP] }, Torso: [['x', -14], ['z', 24]], Chest: [['z', 8]], Head: [['x', 10], ['z', -24]],
    Arm_R: [['x', 88], ['y', -6]], Hand_R: [['x', 4]], Arm_L: [['x', -20], ['y', 18]], Hand_L: [['x', 80]],
    Thigh_R: [['x', -20], ['y', -8]], Thigh_L: [['x', 30], ['y', 8]], Leg_L: [['x', -30]] } },
  { t: 0.75, pose: { ...REST, Root: { pos: [0, 0.5, HIP] }, Torso: [['x', -12], ['z', 20]], Head: [['x', 8], ['z', -20]],
    Arm_R: [['x', 84], ['y', -6]], Hand_R: [['x', 8]], Arm_L: [['x', -18], ['y', 18]], Hand_L: [['x', 80]],
    Thigh_R: [['x', -20], ['y', -8]], Thigh_L: [['x', 30], ['y', 8]], Leg_L: [['x', -30]] } },
  { t: 1.2, pose: { ...REST, Root: { pos: [0, 0.5, HIP] } } },
];

/**
 * The film's end (Enzo, 17/10/2026: "he grabs us and leaps into the air with us before throwing us onto the island in
 * the sky"), seen from the player's own eyes (client/TransformationCinematic):
 * grab, 1.2 s: lunging, his right arm thrown forward at the viewer (closing on him at 0.6 s), then the fist brought back
 * against his chest, holding him;
 * leap, 1.0 s: a deep crouch (0.5 s), then the spring (0.7 s), arms flung up, the fist still at his chest;
 * fly, 1.2 s, looped: tucked in the air, the free arm up, the head thrown back roaring;
 * throw, 0.8 s: the fist drawn back over his head and hurled forward and down (released at 0.35 s);
 * land, 1.6 s: down from the sky on bent legs, one fist on the ground, then up into a roar.
 */
const HOLD = { ...REST, Torso: [['x', 8]], Head: [['x', 14]], Arm_R: [['x', 70], ['y', 34]], Hand_R: [['x', 104]] };
const CROUCH = (d) => ({ ...HOLD, Root: { pos: [0, 0, HIP - 0.3 * d] }, Torso: [['x', 8 + 22 * d]], Head: [['x', 14 - 30 * d]],
  Arm_L: [['x', -20 * d], ['y', 20]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 64 * d], ['y', -10]], Leg_R: [['x', -110 * d]], Thigh_L: [['x', 64 * d], ['y', 10]], Leg_L: [['x', -110 * d]] });
const TUCK = (k) => ({ ...HOLD, Torso: [['x', -8 - 4 * k]], Head: [['x', -34 - 6 * k]], Arm_L: [['x', 150 + 10 * k], ['y', 24]], Hand_L: [['x', 20]],
  Thigh_R: [['x', 46 + 6 * k], ['y', -10]], Leg_R: [['x', -70 - 8 * k]], Thigh_L: [['x', 30 - 6 * k], ['y', 10]], Leg_L: [['x', -50 + 8 * k]] });
const LUNGE = { ...REST, Root: { pos: [0, 0.7, HIP - 0.1] }, Torso: [['x', 28]], Chest: [['x', 8]], Head: [['x', -16]],
  Arm_R: [['x', 98], ['y', -4]], Hand_R: [['x', 6]], Arm_L: [['x', 10], ['y', 24]], Hand_L: [['x', 30]],
  Thigh_R: [['x', -26], ['y', -8]], Leg_R: [['x', -10]], Thigh_L: [['x', 36], ['y', 8]], Leg_L: [['x', -40]] };
const grab = [
  { t: 0, pose: REST },
  { t: 0.3, pose: { ...REST, Root: { pos: [0, 0, HIP - 0.06] }, Torso: [['x', 12]], Arm_R: [['x', 30], ['y', -40]], Hand_R: [['x', 70]] } },
  { t: 0.6, pose: LUNGE },
  { t: 0.72, pose: { ...LUNGE, Hand_R: [['x', 20]] } },
  { t: 1.05, pose: { ...HOLD, Root: { pos: [0, 0.3, HIP] } } },
  { t: 1.2, pose: HOLD },
];
/**
 * In the air (Enzo: "he is upright at the top when he throws us: not possible, physically"): his whole body pitched into
 * his flight, the legs trailing bent behind him, the free arm out for balance, the head up to see ahead; the viewer's
 * fist held at his chest.
 */
const AIR = (k) => ({ ...HOLD, Root: { pos: [0, 0, HIP], rot: [['x', 28 + 4 * k]] }, Torso: [['x', 6]], Chest: [['x', 4]], Head: [['x', -34]],
  Arm_L: [['x', 50 + 10 * k], ['y', 50], ['z', -10]], Hand_L: [['x', 20]],
  Thigh_R: [['x', -26 - 6 * k], ['y', -10]], Leg_R: [['x', -60 + 8 * k]], Thigh_L: [['x', -8 + 6 * k], ['y', 10]], Leg_L: [['x', -80 - 8 * k]] });
const leap = [
  { t: 0, pose: HOLD }, { t: 0.5, pose: CROUCH(1) }, { t: 0.62, pose: CROUCH(1.05) },
  { t: 0.75, pose: { ...HOLD, Torso: [['x', -12]], Head: [['x', -36]], Arm_L: [['x', 170], ['y', 20]], Thigh_R: [['x', -12]], Thigh_L: [['x', -12]] } },
  { t: 1.0, pose: AIR(0) },
];
const fly = [{ t: 0, pose: AIR(0) }, { t: 0.6, pose: AIR(1) }, { t: 1.2, pose: AIR(0) }];
/**
 * The throw in the air: drawn back, twisting, the fist over his head and the knees drawn up under him; then the whole
 * body whipping forward and down, the arm hurled through, the legs kicking back (released at 0.35 s); carried on into a
 * dive, arms spread, head first towards the bowl.
 */
const THROW_BACK = { ...HOLD, Root: { pos: [0, 0, HIP], rot: [['x', 2]] }, Torso: [['x', -30], ['y', 24]], Chest: [['x', -10]], Head: [['x', -16]],
  Arm_R: [['x', 176], ['y', -24]], Hand_R: [['x', 46]], Arm_L: [['x', 70], ['y', 40]], Hand_L: [['x', 30]],
  Thigh_R: [['x', 44], ['y', -10]], Leg_R: [['x', -90]], Thigh_L: [['x', 30], ['y', 10]], Leg_L: [['x', -100]] };
const THROWN = { ...HOLD, Root: { pos: [0, 0, HIP], rot: [['x', 50]] }, Torso: [['x', 40], ['y', -22]], Chest: [['x', 8]], Head: [['x', -40]],
  Arm_R: [['x', 40], ['y', -12]], Hand_R: [['x', 0]], Arm_L: [['x', -40], ['y', 36]], Hand_L: [['x', 20]],
  Thigh_R: [['x', -40], ['y', -10]], Leg_R: [['x', -50]], Thigh_L: [['x', -20], ['y', 10]], Leg_L: [['x', -70]] };
const DIVE = { ...THROWN, Root: { pos: [0, 0, HIP], rot: [['x', 62]] }, Torso: [['x', 10]], Head: [['x', -50]],
  Arm_R: [['x', 60], ['y', -60]], Hand_R: [['x', 10]], Arm_L: [['x', 60], ['y', 60]], Hand_L: [['x', 10]] };
const throwIt = [
  { t: 0, pose: AIR(0) },
  { t: 0.25, pose: THROW_BACK },
  { t: 0.35, pose: THROWN },
  { t: 0.8, pose: DIVE },
];
const dive = [{ t: 0, pose: DIVE }, { t: 0.5, pose: { ...DIVE, Head: [['x', -54]], Arm_R: [['x', 64], ['y', -64]], Arm_L: [['x', 64], ['y', 64]] } }, { t: 1.0, pose: DIVE }];
const SLAMMED = { ...CROUCH(1.15), Torso: [['x', 40]], Head: [['x', -20]], Arm_R: [['x', 20], ['y', -30]], Hand_R: [['x', 10]], Arm_L: [['x', 30], ['y', 30]], Hand_L: [['x', 30]] };
const land = [
  { t: 0, pose: SLAMMED }, { t: 0.6, pose: SLAMMED },
  { t: 1.2, pose: ROAR }, { t: 1.6, pose: { ...ROAR, Head: [['x', -34]] } },
];

/**
 * The hammer blow at the viewer (Enzo: "he comes at us walking slowly, then he hits us"): the fist raised high behind
 * him, then brought down and forward at a man's head, his whole weight behind it (striking at 0.45 s), held, back to
 * holding. 1.0 s.
 */
const smash = [
  { t: 0, pose: { ...REST, Torso: [['x', 4]] } },
  { t: 0.3, pose: { ...REST, Torso: [['x', -12]], Head: [['x', 8]], Arm_R: [['x', 168], ['y', -20]], Hand_R: [['x', 50]], Arm_L: [['x', 20], ['y', 20]] } },
  { t: 0.45, pose: { ...REST, Root: { pos: [0, 0.7, HIP - 0.12] }, Torso: [['x', 40]], Chest: [['x', 10]], Head: [['x', -30]],
    Arm_R: [['x', 25], ['y', -4]], Hand_R: [['x', 0]], Arm_L: [['x', -10], ['y', 22]], Hand_L: [['x', 30]],
    Thigh_R: [['x', -10], ['y', -8]], Thigh_L: [['x', 34], ['y', 8]], Leg_L: [['x', -40]] } },
  { t: 0.6, pose: { ...REST, Root: { pos: [0, 0.7, HIP - 0.12] }, Torso: [['x', 38]], Head: [['x', -28]], Arm_R: [['x', 28], ['y', -4]], Hand_R: [['x', 4]],
    Thigh_L: [['x', 34], ['y', 8]], Leg_L: [['x', -40]] } },
  { t: 1.0, pose: HOLD },
];

const GAME = {
  idle: avatar.ANIMS.idle, walk: avatar.ANIMS.walk, awaken: avatar.ANIMS.awaken, chest_beat: avatar.ANIMS.chest_beat,
  roar: [{ t: 0, pose: REST }, { t: 0.4, pose: ROAR }, { t: 1.6, pose: { ...ROAR, Head: [['x', -34]] } }, { t: 2.0, pose: REST }],
  punch, grab, leap, fly, throw: throwIt, land, smash, dive,
};

if (require.main === module) {
  // The mesh, raised to stand on the entity's feet, and the armature in one file.
  const pieces = avatar.PIECES.map(p => ({ ...p, from: [p.from[0], p.from[1] + LIFT, p.from[2]], to: [p.to[0], p.to[1] + LIFT, p.to[2]],
    pivot: p.pivot && [p.pivot[0], p.pivot[1] + LIFT, p.pivot[2]], hinge: p.hinge && { ...p.hinge, y: p.hinge.y + LIFT },
    turnCorner: p.turnCorner && (c => { const r = p.turnCorner([c[0], c[1] - LIFT, c[2]]); return [r[0], r[1] + LIFT, r[2]]; }) }));
  const faces = kit.pack(pieces, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, avatar.AVATAR, ATLAS));
  const m = kit.mesh(pieces, ATLAS);
  m.armature = { joints: arm.order, hierarchy: [hierarchy('Root')] };
  fs.writeFileSync(path.join(ASSETS, 'animmodels', 'entity', 'son_of_the_demon.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'son_of_the_demon.png'), atlas);
  console.log('wrote the Son of the Demon:', pieces.length, 'pieces,', m.vertices.positions.count, 'vertices; his root', (b.origin(global.Root)[2]).toFixed(3), 'up');
  for (const [name, keys] of Object.entries(GAME)) write(name, keys);
}

module.exports = { GAME, global, local };
