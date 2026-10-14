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

/**
 * Local matrices of every joint from ready rotations {joint: 3x3 row-major, character axes} and the root's position:
 * the same rule as locals(), for rotations that do not come from a pose spec (a Blockbench animation).
 */
function localsFromRotations(rots, rootPos) {
  const out = {};
  const m4 = r => [r[0], r[1], r[2], 0, r[3], r[4], r[5], 0, r[6], r[7], r[8], 0, 0, 0, 0, 1];
  for (const name of arm.order) {
    const j = arm.joints[name];
    const r = rots[name] ? m4(rots[name]) : b.rotation([1, 0, 0], 0);
    const B = rotOnly(bind[name]);
    if (name === 'Root') out[name] = b.mul(b.translation(...(rootPos || b.origin(bind.Root))), b.mul(r, rotOnly(j.local)));
    else out[name] = b.mul(j.local, b.mul(b.invRigid(B), b.mul(r, B)));
  }
  return out;
}

/**
 * The angles, about x, then y, then z as locals() reads them, that give joint `name` the local matrix `L`: a pose spec
 * back from a frame (`axes` 1 for a joint turned about x only, such as an elbow).
 */
function anglesOf(name, L, axes = 3) {
  const B = rotOnly(bind[name]), J = rotOnly(arm.joints[name].local);
  const r = b.mul(B, b.mul(b.invRigid(J), b.mul(rotOnly(L), b.invRigid(B))));
  const deg = v => v * 180 / Math.PI;
  const x = ['x', deg(Math.atan2(r[9], r[10]))];
  if (axes === 1) return [x];
  return [x, ['y', deg(Math.asin(Math.max(-1, Math.min(1, -r[8]))))], ['z', deg(Math.atan2(r[4], r[0]))]];
}

const round = m => m.map(v => Math.abs(v) < 5e-7 ? 0 : Math.round(v * 1e6) / 1e6);

/** Writes an Epic Fight animation from frames already computed: times (seconds) and local matrices per frame. */
function writeFrames(dir, name, times, frames) {
  const json = {
    animation: arm.order.map(joint => ({ name: joint, time: times, transform: frames.map(f => round(f[joint])) })),
  };
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name + '.json'), JSON.stringify(json));
}

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


/** A rotation (the 3 x 3 part of a row-major 4 x 4) as a quaternion [x, y, z, w]. */
function quat(m) {
  const [a, b, c, , d, e, f, , g, h, i] = m, tr = a + e + i;
  if (tr > 0) { const k = Math.sqrt(tr + 1) * 2; return [(h - f) / k, (c - g) / k, (d - b) / k, k / 4]; }
  if (a > e && a > i) { const k = Math.sqrt(1 + a - e - i) * 2; return [k / 4, (b + d) / k, (c + g) / k, (h - f) / k]; }
  if (e > i) { const k = Math.sqrt(1 + e - a - i) * 2; return [(b + d) / k, k / 4, (f + h) / k, (c - g) / k]; }
  const k = Math.sqrt(1 + i - a - e) * 2; return [(c + g) / k, (f + h) / k, k / 4, (d - b) / k];
}
function slerp(p, q, t) {
  let d = p.reduce((s, v, n) => s + v * q[n], 0);
  if (d < 0) { q = q.map(v => -v); d = -d; }
  if (d > 0.9995) { const r = p.map((v, n) => v + (q[n] - v) * t), l = Math.hypot(...r); return r.map(v => v / l); }
  const th = Math.acos(d), sa = Math.sin((1 - t) * th) / Math.sin(th), sb = Math.sin(t * th) / Math.sin(th);
  return p.map((v, n) => v * sa + q[n] * sb);
}
function matrix([x, y, z, w], [tx, ty, tz]) {
  return [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), tx,
    2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), ty,
    2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), tz, 0, 0, 0, 1];
}
const STEP = 1 / 40;
/**
 * Writes an Epic Fight animation from keyframes [{t, pose}] into MURIM_BIPED, every joint turning the shortest way from one
 * key to the next (quaternions) and every key landing exactly on its time: two keys whose angles say the same turn two
 * different ways never spin between them. Returns nothing; logs what it wrote.
 */
function writeSmooth(name, keys, dir = MURIM_BIPED) {
  const { times, frames } = sample(keys);
  writeFrames(dir, name, times, frames);
  console.log('wrote', name, times.length, 'frames,', keys[keys.length - 1].t, 's');
}

/** The frames writeSmooth writes for keyframes [{t, pose}]: their times and every joint's local matrix at each. */
function sample(keys) {
  const L = keys.map(k => locals(k.pose)), end = keys[keys.length - 1].t, times = [];
  for (let t = 0; t < end - 1e-9; t += STEP) times.push(Math.round(t * 1000) / 1000);
  // Every key on time, exactly.
  for (const k of keys) if (!times.includes(k.t)) times.push(k.t);
  times.sort((x, y) => x - y);
  const frames = times.map(t => {
    let i = 0;
    while (i < keys.length - 2 && t > keys[i + 1].t) i++;
    const f = Math.min(1, Math.max(0, (t - keys[i].t) / (keys[i + 1].t - keys[i].t)));
    const out = {};
    for (const j of arm.order) {
      const a = L[i][j], b = L[i + 1][j];
      out[j] = matrix(slerp(quat(a), quat(b), f), [3, 7, 11].map(n => a[n] + (b[n] - a[n]) * f));
    }
    return out;
  });
  return { times, frames };
}

module.exports = { arm, bind, locals, localsFromRotations, write, writeFrames, MURIM_BIPED, writeSmooth, sample, anglesOf };
