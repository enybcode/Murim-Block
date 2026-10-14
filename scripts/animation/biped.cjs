// Epic Fight biped skeleton maths, shared by the Murim player animation scripts.
// Matrices are row-major 4x4 arrays of 16 numbers, translation in the last column, exactly as in Epic Fight's JSON
// (armature "transform" and animation "transform"). A joint's matrix is relative to its parent ("local").
// Model space is Epic Fight's own: Z up; see FORWARD below.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

/** Reads the biped armature: from the Epic Fight jar in the Gradle cache, or a file given in EPICFIGHT_BIPED. */
function loadArmature() {
  let text;
  if (process.env.EPICFIGHT_BIPED) text = fs.readFileSync(process.env.EPICFIGHT_BIPED, 'utf8');
  else text = readFromJar('assets/epicfight/animmodels/entity/biped.json');
  const j = JSON.parse(text).armature;
  const joints = {};
  (function walk(node, parent) {
    joints[node.name] = { name: node.name, parent, local: node.transform.slice(), children: (node.children || []).map(c => c.name) };
    (node.children || []).forEach(c => walk(c, node.name));
  })(j.hierarchy[0], null);
  return { order: j.joints, joints };
}

/** Finds the pinned Epic Fight jar (Modrinth 8HHhJt6i) in the Gradle cache and reads one entry (stored or deflated). */
function readFromJar(entry) {
  const home = process.env.USERPROFILE || process.env.HOME;
  const base = path.join(home, '.gradle', 'caches', 'modules-2', 'files-2.1', 'maven.modrinth', 'epic-fight', '8HHhJt6i');
  const jar = findFile(base, f => f.endsWith('.jar'));
  if (!jar) throw new Error('Epic Fight jar not found under ' + base + ' (run a Gradle build first, or set EPICFIGHT_BIPED)');
  const buf = fs.readFileSync(jar);
  // Central directory: scan for the entry name.
  let eocd = buf.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  const cdOffset = buf.readUInt32LE(eocd + 16), cdCount = buf.readUInt16LE(eocd + 10);
  let p = cdOffset;
  for (let i = 0; i < cdCount; i++) {
    const method = buf.readUInt16LE(p + 10), csize = buf.readUInt32LE(p + 20);
    const nlen = buf.readUInt16LE(p + 28), xlen = buf.readUInt16LE(p + 30), clen = buf.readUInt16LE(p + 32);
    const local = buf.readUInt32LE(p + 42);
    const name = buf.toString('utf8', p + 46, p + 46 + nlen);
    if (name === entry) {
      const lnlen = buf.readUInt16LE(local + 26), lxlen = buf.readUInt16LE(local + 28);
      const data = buf.subarray(local + 30 + lnlen + lxlen, local + 30 + lnlen + lxlen + csize);
      return (method === 0 ? data : zlib.inflateRawSync(data)).toString('utf8');
    }
    p += 46 + nlen + xlen + clen;
  }
  throw new Error(entry + ' not in ' + jar);
}

function findFile(dir, pred) {
  if (!fs.existsSync(dir)) return null;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const f = path.join(dir, e.name);
    if (e.isDirectory()) { const r = findFile(f, pred); if (r) return r; } else if (pred(f)) return f;
  }
  return null;
}

// ---------------------------------------------------------------- matrices

function mul(a, b) {
  const r = new Array(16).fill(0);
  for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) {
    let s = 0;
    for (let k = 0; k < 4; k++) s += a[i * 4 + k] * b[k * 4 + j];
    r[i * 4 + j] = s;
  }
  return r;
}

/** Inverse of a rigid transform (rotation + translation). */
function invRigid(m) {
  const r = [m[0], m[4], m[8], 0, m[1], m[5], m[9], 0, m[2], m[6], m[10], 0, 0, 0, 0, 1];
  const t = [m[3], m[7], m[11]];
  r[3] = -(r[0] * t[0] + r[1] * t[1] + r[2] * t[2]);
  r[7] = -(r[4] * t[0] + r[5] * t[1] + r[6] * t[2]);
  r[11] = -(r[8] * t[0] + r[9] * t[1] + r[10] * t[2]);
  return r;
}

function translation(x, y, z) { return [1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1]; }

/** Rotation by an angle in degrees about a unit axis (model-space axis when applied on the left). */
function rotation(axis, deg) {
  const [x, y, z] = norm(axis), a = deg * Math.PI / 180, c = Math.cos(a), s = Math.sin(a), t = 1 - c;
  return [t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0,
          t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0,
          t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0, 0, 0, 0, 1];
}

function norm(v) { const l = Math.hypot(...v); return v.map(x => x / l); }
function origin(m) { return [m[3], m[7], m[11]]; }

/** Model-space matrices of every joint for a set of local matrices. */
function modelSpace(arm, locals) {
  const out = {};
  for (const name of arm.order) {
    const j = arm.joints[name];
    const l = locals[name] || j.local;
    out[name] = j.parent ? mul(out[j.parent], l) : l;
  }
  return out;
}

module.exports = { loadArmature, readFromJar, mul, invRigid, translation, rotation, origin, modelSpace, norm };
