// A Blockbench project on Epic Fight's biped skeleton: a Steve-sized swordsman whose bones (groups) sit on the biped's
// joints, so an animation made in Blockbench can be turned into an Epic Fight animation (see blockbench-to-epicfight.cjs).
//
// Axes. Epic Fight's model space (pose.cjs): X the character's right, Y its front, Z up, in blocks. Blockbench: x right,
// y up, z toward the back (the character faces north, -z), in pixels. So bb = 16 * (x, z, -y).
//
// Rotations. A pose gives each joint a rotation r in the character's own axes, applied in its parent's (posed) frame;
// a Blockbench bone's rotation is applied the same way, in its parent's frame. Blockbench shows a bone's rotation as
// Euler angles (x, y, z) in degrees and builds it as Rz(z) * Ry(-y) * Rx(-x) (Bedrock's convention: x and y negated).
const crypto = require('crypto');
const zlib = require('zlib');
const b = require('./biped.cjs');

const AXES = { x: [1, 0, 0], y: [0, 1, 0], z: [0, 0, 1] };
const PX = 16;

/** Epic Fight model space (blocks) -> Blockbench (pixels). */
const toBB = ([x, y, z]) => [x * PX, z * PX, -y * PX];

/** A pose's joint rotation [[axis, deg], ...] (character axes) as a 3x3 matrix (row-major 9). */
function rotationOf(spec) {
  let r = b.rotation([1, 0, 0], 0);
  for (const [axis, deg] of spec || []) r = b.mul(b.rotation(AXES[axis], deg), r);
  return [r[0], r[1], r[2], r[4], r[5], r[6], r[8], r[9], r[10]];
}

// P maps Epic Fight axes to Blockbench axes: bb = P * ef.
const P = [1, 0, 0, 0, 0, 1, 0, -1, 0];
const PT = [1, 0, 0, 0, 0, -1, 0, 1, 0];
const mul3 = (a, c) => {
  const r = new Array(9).fill(0);
  for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) for (let k = 0; k < 3; k++) r[i * 3 + j] += a[i * 3 + k] * c[k * 3 + j];
  return r;
};

/** A rotation (Epic Fight character axes) as Blockbench's keyframe Euler angles [x, y, z] in degrees. */
function toBlockbenchEuler(r) {
  const m = mul3(mul3(P, r), PT);
  // m = Rz(c) Ry(b) Rx(a): three.js 'ZYX'.
  const m11 = m[0], m12 = m[1], m21 = m[3], m22 = m[4], m31 = m[6], m32 = m[7], m33 = m[8];
  const bAng = Math.asin(-Math.max(-1, Math.min(1, m31)));
  let a, c;
  if (Math.abs(m31) < 0.9999999) { a = Math.atan2(m32, m33); c = Math.atan2(m21, m11); } else { a = 0; c = Math.atan2(-m12, m22); }
  const deg = v => Math.round(v * 180 / Math.PI * 100) / 100;
  // Blockbench negates x and y.
  return [deg(-a) || 0, deg(-bAng) || 0, deg(c) || 0];
}

/** Blockbench keyframe Euler angles back to a rotation in Epic Fight character axes. */
function fromBlockbenchEuler([x, y, z]) {
  const rz = b.rotation([0, 0, 1], z), ry = b.rotation([0, 1, 0], -y), rx = b.rotation([1, 0, 0], -x);
  const m4 = b.mul(rz, b.mul(ry, rx));
  const m = [m4[0], m4[1], m4[2], m4[4], m4[5], m4[6], m4[8], m4[9], m4[10]];
  return mul3(mul3(PT, m), P);
}

// ---------------------------------------------------------------- the rig

const arm = b.loadArmature();
const bind = b.modelSpace(arm, {});
const at = name => toBB(b.origin(bind[name]));

/** Flat colours, one texel each, in a 16x16 palette. */
const COLORS = {
  skin: [201, 154, 122], hair: [46, 34, 26], robe: [62, 127, 168], robeDark: [40, 88, 122], sash: [230, 224, 208],
  pants: [42, 47, 69], boots: [59, 44, 34], steel: [220, 226, 234], edge: [250, 252, 255], gold: [201, 167, 75], grip: [70, 46, 30],
};
const COLOR_INDEX = Object.fromEntries(Object.keys(COLORS).map((k, i) => [k, i]));

function palettePng() {
  const W = 16, H = 16, raw = Buffer.alloc((W * 4 + 1) * H);
  Object.values(COLORS).forEach(([r, g, bl], i) => {
    const o = (i % W) * 4 + 1 + Math.floor(i / W) * (W * 4 + 1);
    raw[o] = r; raw[o + 1] = g; raw[o + 2] = bl; raw[o + 3] = 255;
  });
  const crcTable = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = crcTable[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

const uuid = () => crypto.randomUUID();

function cube(name, from, to, color, origin) {
  const i = COLOR_INDEX[color], uv = [i % 16, Math.floor(i / 16), i % 16 + 1, Math.floor(i / 16) + 1];
  const face = { uv, texture: 0 };
  return {
    name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from, to, autouv: 0, color: 0, origin: origin || from.map((v, k) => (v + to[k]) / 2),
    faces: { north: face, east: face, south: face, west: face, up: face, down: face }, type: 'cube', uuid: uuid(),
  };
}

/**
 * The swordsman: groups on the biped's joints (Root, Torso, Chest, Head, Arm_R, Hand_R (the forearm), Tool_R (the hand
 * holding the sword), the same on the left, Thigh and Leg), Steve-sized cubes, a straight sword along the blade's line.
 */
function buildRig() {
  const elements = [];
  const add = (...c) => { for (const x of c) elements.push(x); return c.map(x => x.uuid); };
  const group = (name, children) => ({ name, origin: at(name), color: 0, uuid: uuid(), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: true, autouv: 0, children });
  const [hx, hy, hz] = at('Head'), [, cy] = at('Chest'), [, ty] = at('Torso');
  const [ax, ay] = at('Arm_R'), [, ey] = at('Hand_R'), [tx, tyy, tz] = at('Tool_R');
  const [lx, ly] = at('Thigh_R'), [, ky] = at('Leg_R');
  const head = group('Head', add(cube('head', [hx - 4, hy, hz - 4], [hx + 4, hy + 8, hz + 4], 'skin'),
      cube('hair', [hx - 4.2, hy + 5, hz - 4.2], [hx + 4.2, hy + 8.4, hz + 4.2], 'hair'),
      cube('topknot', [hx - 1, hy + 8.4, hz + 0.5], [hx + 1, hy + 10, hz + 2.5], 'hair')));
  const sword = group('Tool_R', add(
      cube('grip', [tx - 0.5, tyy - 0.5, tz - 1], [tx + 0.5, tyy + 0.5, tz + 3], 'grip'),
      cube('guard', [tx - 1.5, tyy - 0.6, tz - 1.6], [tx + 1.5, tyy + 0.6, tz - 1], 'gold'),
      cube('blade', [tx - 0.4, tyy - 0.7, tz - 17], [tx + 0.4, tyy + 0.7, tz - 1.6], 'steel'),
      cube('edge', [tx - 0.15, tyy - 0.85, tz - 17], [tx + 0.15, tyy - 0.7, tz - 1.6], 'edge')));
  const armR = group('Arm_R', add(cube('upper_arm_r', [ax - 2, ey, -2], [ax + 2, ay + 2, 2], 'robe')));
  const handR = group('Hand_R', add(cube('forearm_r', [ax - 2, ey - 6, -2], [ax + 2, ey, 2], 'robe'),
      cube('hand_r', [ax - 2, ey - 6.5, -2], [ax + 2, ey - 5, 2], 'skin')));
  handR.children.push(sword);
  armR.children.push(handR);
  const armL = group('Arm_L', add(cube('upper_arm_l', [-ax - 2, ey, -2], [-ax + 2, ay + 2, 2], 'robe')));
  const handL = group('Hand_L', add(cube('forearm_l', [-ax - 2, ey - 6, -2], [-ax + 2, ey, 2], 'robe'),
      cube('hand_l', [-ax - 2, ey - 6.5, -2], [-ax + 2, ey - 5, 2], 'skin')));
  armL.children.push(handL);
  const chest = group('Chest', add(cube('chest', [-4, cy, -2], [4, hy, 2], 'robe'), cube('collar', [-1.5, hy - 3, -2.2], [1.5, hy, -2], 'sash')));
  // The shoulders: Epic Fight's own attacks turn them (ours do not).
  const shoulderR = group('Shoulder_R', [armR]), shoulderL = group('Shoulder_L', [armL]);
  chest.children.push(head, shoulderR, shoulderL);
  const torso = group('Torso', add(cube('belly', [-4, ty, -2], [4, cy, 2], 'robeDark'), cube('sash', [-4.2, ty, -2.2], [4.2, ty + 1.5, 2.2], 'sash')));
  torso.children.push(chest);
  const leg = (side, x) => {
    const thigh = group('Thigh_' + side, add(cube('thigh_' + side.toLowerCase(), [x - 2, ky, -2], [x + 2, ly, 2], 'pants')));
    const shin = group('Leg_' + side, add(cube('shin_' + side.toLowerCase(), [x - 2, 1.5, -2], [x + 2, ky, 2], 'pants'),
        cube('boot_' + side.toLowerCase(), [x - 2.1, 0, -2.6], [x + 2.1, 2.5, 2.1], 'boots')));
    thigh.children.push(shin);
    return thigh;
  };
  const root = group('Root', []);
  root.children.push(torso, leg('R', lx), leg('L', -lx));
  return { elements, outliner: [root] };
}

/** Every group of the outliner by name. */
function groupsByName(outliner) {
  const out = {};
  (function walk(nodes) { for (const n of nodes) if (typeof n === 'object') { out[n.name] = n; walk(n.children); } })(outliner);
  return out;
}

const BONES = ['Root', 'Torso', 'Chest', 'Head', 'Shoulder_R', 'Shoulder_L', 'Arm_R', 'Hand_R', 'Tool_R', 'Arm_L', 'Hand_L', 'Thigh_R', 'Leg_R', 'Thigh_L', 'Leg_L'];
const ROOT_BIND = b.origin(bind.Root);

/**
 * A Blockbench animation from pose keyframes [{t, pose}] (pose.cjs format): every bone's rotation at every key, and the
 * root's position (relative to its bind position) where the pose gives one.
 */
function animation(name, keys, groups, loop = 'once') {
  const animators = {};
  for (const bone of BONES) {
    const g = groups[bone];
    if (!g) continue;
    const keyframes = [];
    for (const k of keys) {
      // A key gives either a pose (pose.cjs notation) or ready rotations {rots: {bone: 3x3}, pos} (a converted animation).
      let r;
      if (k.rots) r = k.rots[bone] || rotationOf([]);
      else r = rotationOf(bone === 'Root' ? (k.pose.Root && k.pose.Root.rot) || [] : k.pose[bone] || []);
      const [x, y, z] = toBlockbenchEuler(r);
      keyframes.push({ channel: 'rotation', data_points: [{ x, y, z }], uuid: uuid(), time: k.t, color: -1, interpolation: 'linear' });
      if (bone === 'Root') {
        const pos = k.rots ? k.pos || ROOT_BIND : (k.pose.Root && k.pose.Root.pos) || ROOT_BIND;
        const d = toBB([pos[0] - ROOT_BIND[0], pos[1] - ROOT_BIND[1], pos[2] - ROOT_BIND[2]]);
        // Blockbench negates x for positions too.
        keyframes.push({ channel: 'position', data_points: [{ x: -d[0], y: d[1], z: d[2] }], uuid: uuid(), time: k.t, color: -1, interpolation: 'linear' });
      }
    }
    animators[g.uuid] = { name: bone, type: 'bone', keyframes };
  }
  const length = keys[keys.length - 1].t;
  return { uuid: uuid(), name, loop, override: false, length, snapping: 20, selected: false, anim_time_update: '',
    blend_weight: '', start_delay: '', loop_delay: '', animators };
}

/** A whole Blockbench project: the rig, its palette and the animations {name: keys}. */
function project(name, anims) {
  const { elements, outliner } = buildRig();
  const groups = groupsByName(outliner);
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name, model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
    timeline_setups: [], unhandled_root_fields: {}, resolution: { width: 16, height: 16 },
    elements, outliner,
    textures: [{ path: '', name: 'palette.png', folder: '', namespace: '', id: '0', width: 16, height: 16, uv_width: 16, uv_height: 16,
      particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto',
      frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false,
      uuid: uuid(), source: 'data:image/png;base64,' + palettePng().toString('base64') }],
    animations: Object.entries(anims).map(([n, keys]) => animation(n, keys, groups)),
  };
}

module.exports = { project, toBlockbenchEuler, fromBlockbenchEuler, rotationOf, toBB, BONES, ROOT_BIND, P, PT, mul3 };
