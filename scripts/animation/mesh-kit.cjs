// Shared by the 3D mob scripts (bandit-3d.cjs, bandit-chief-3d.cjs): a mob made of boxes on Epic Fight's biped
// skeleton, painted texel by texel, written as an Epic Fight skinned mesh (a HumanoidMesh), its atlases and a Blockbench
// project to look at and edit.
//
// A piece: its bone (a biped joint), its mesh part (one of PARTS), a box in Blockbench pixels (x the mob's right, y up,
// z its back; it faces north, -z) and what it shows on each face: paint(face, u, v, w, h, outfit) -> a colour letter
// (looked up in the outfit's palette; lower case a shade darker), a hex colour, or null for clear, u and v as the face is
// seen from outside, v down. Optional: wear(outfit) (clear in the outfits that do not wear it), a tilt about x (degrees,
// around `pivot`, Blockbench's convention: +y toward +z for a positive angle), a hinge (its vertices on the plane
// y = hinge.y follow `hinge.bone` half way, so elbows, knees and the waist bend smoothly), a density (texels per pixel,
// 1 by default: 2 gives a 16 x 16 face on an 8-pixel head, see hd-face.cjs).
const crypto = require('crypto');
const zlib = require('zlib');
const { at } = require('./blockbench-rig.cjs');

/** A uuid made from a name, so a project written again does not change. */
const stableId = name => {
  const h = crypto.createHash('md5').update(name).digest('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`;
};

// ---------------------------------------------------------------- painting

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
/** A little grain so flat colours read as cloth and leather (the same for a given texel). */
const grain = (u, v, s) => 0.95 + (((u * 73 + v * 151 + s * 37) % 7) / 6) * 0.08;
/** A touch of light from above, on top of the game's own shading. */
const LIGHT = { up: 1.04, down: 0.86, north: 1, south: 0.94, east: 0.97, west: 0.97 };

function colour(letter, outfit, face, u, v, seed) {
  if (letter == null || letter === '.') return null;
  if (typeof letter === 'string' && letter.startsWith('#')) return [...hex(letter), 255];
  const dark = letter !== letter.toUpperCase();
  const h = outfit.c[letter.toUpperCase()];
  if (!h) throw new Error('no colour ' + letter + ' in ' + outfit.key);
  const f = (dark ? 0.8 : 1) * LIGHT[face] * grain(u, v, seed);
  return [...hex(h).map(x => Math.max(0, Math.min(255, Math.round(x * f)))), 255];
}

const isSide = f => f === 'north' || f === 'south' || f === 'east' || f === 'west';
const all = l => () => l;
/** Cloth hanging in folds: a fold every third column, the hem darker. */
const folds = (l, hem) => (f, u, v, w, h) => (isSide(f) && v === h - 1 && hem ? hem : (isSide(f) && u % 3 === 2 ? l.toLowerCase() : l));
/** A fringe: the last row every other texel. */
const fringe = (l) => (f, u, v, w, h) => (isSide(f) && v === h - 1 ? (u % 2 ? null : l.toLowerCase()) : (u % 3 === 1 ? l.toLowerCase() : l));
/** Wrapped strips running round a limb, slanted. */
const wraps = (l, strap) => (f, u, v, w, h) => (f === 'up' || f === 'down' ? l.toLowerCase() : strap && v === Math.floor(h / 2) ? strap : (u + v) % 3 === 0 ? l.toLowerCase() : l);
/** Only the rim of a top or bottom face: what lies inside is hidden in the body. */
const rim = (paintIt) => (f, u, v, w, h, o) => ((f === 'up' || f === 'down') && u > 0 && v > 0 && u < w - 1 && v < h - 1 ? null : paintIt(f, u, v, w, h, o));

// ---------------------------------------------------------------- faces and the atlas

const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
const size = (p, f) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, r = d => Math.max(1, Math.round(d * (p.density || 1)));
  if (f === 'north' || f === 'south') return [r(x1 - x0), r(y1 - y0)];
  if (f === 'east' || f === 'west') return [r(z1 - z0), r(y1 - y0)];
  return [r(x1 - x0), r(z1 - z0)];
};

/** Shelf packing, tallest faces first: each face gets its rectangle of texels in the atlas (piece.uv[face]). */
function pack(pieces, atlas) {
  const faces = [];
  for (const p of pieces) for (const f of FACES) { const [w, h] = size(p, f); faces.push({ p, f, w, h }); }
  faces.sort((a, b) => b.h - a.h || b.w - a.w);
  let x = 0, y = 0, shelf = 0;
  for (const face of faces) {
    if (x + face.w > atlas) { x = 0; y += shelf; shelf = 0; }
    face.u = x; face.v = y;
    x += face.w; shelf = Math.max(shelf, face.h);
  }
  if (y + shelf > atlas) throw new Error('the atlas is full: ' + (y + shelf) + ' rows');
  for (const face of faces) (face.p.uv ||= {})[face.f] = [face.u, face.v, face.u + face.w, face.v + face.h];
  return faces;
}

function paintAtlas(faces, outfit, atlas) {
  const img = new Uint8Array(atlas * atlas * 4);
  faces.forEach((face, seed) => {
    if (face.p.wear && !face.p.wear(outfit)) return;
    for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
      const c = colour(face.p.paint(face.f, u, v, face.w, face.h, outfit), outfit, face.f, u, v, seed);
      if (c) img.set(c, ((face.v + v) * atlas + face.u + u) * 4);
    }
  });
  return img;
}

function png(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) Buffer.from(rgba.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------------------------------------------------------------- the Epic Fight mesh

/** The biped's joints, in the order of Epic Fight's armature (entity/biped): a vertex names its joint by this index. */
const JOINTS = ['Root', 'Thigh_R', 'Leg_R', 'Knee_R', 'Thigh_L', 'Leg_L', 'Knee_L', 'Torso', 'Chest', 'Head', 'Shoulder_R', 'Arm_R', 'Hand_R',
  'Tool_R', 'Elbow_R', 'Shoulder_L', 'Arm_L', 'Hand_L', 'Tool_L', 'Elbow_L'];
/** A HumanoidMesh's parts: every one must hold something. */
const PARTS = ['head', 'hat', 'torso', 'jacket', 'rightArm', 'leftArm', 'rightSleeve', 'leftSleeve', 'rightLeg', 'leftLeg', 'rightPants', 'leftPants'];

/** The corners of a face as seen from outside: top left, top right, bottom right, bottom left. */
function corners(p, f) {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  switch (f) {
    case 'north': return [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]];
    case 'south': return [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]];
    case 'east': return [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]];
    case 'west': return [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]];
    case 'up': return [[x1, y1, z1], [x0, y1, z1], [x0, y1, z0], [x1, y1, z0]];
    default: return [[x1, y0, z0], [x0, y0, z0], [x0, y0, z1], [x1, y0, z1]];
  }
}

/** A corner of a piece as it stands: turned by its own turnCorner (any axes) if it has one, else tilted about x. */
function tilt(p, [x, y, z]) {
  if (p.turnCorner) return p.turnCorner([x, y, z]);
  if (!p.tilt) return [x, y, z];
  const a = p.tilt * Math.PI / 180, [, py, pz] = p.pivot, dy = y - py, dz = z - pz;
  return [x, py + dy * Math.cos(a) - dz * Math.sin(a), pz + dy * Math.sin(a) + dz * Math.cos(a)];
}

/** Blockbench pixels to Epic Fight's mesh space: blocks, z up, the front toward +y. */
const toMesh = ([x, y, z]) => [x / 16, -z / 16, y / 16];

/** The mesh of `pieces` (a HumanoidMesh's parts by default; an armour's mesh names its own, e.g. torso, leftArm, rightArm). */
function mesh(pieces, atlas, partNames = PARTS) {
  const positions = [], uvs = [], normals = [], vcounts = [], vindices = [], weights = [1, 0.5];
  const parts = Object.fromEntries(partNames.map(n => [n, []]));
  const normalIndex = n => {
    for (let i = 0; i < normals.length; i += 3) if (Math.abs(normals[i] - n[0]) + Math.abs(normals[i + 1] - n[1]) + Math.abs(normals[i + 2] - n[2]) < 1e-4) return i / 3;
    normals.push(...n); return normals.length / 3 - 1;
  };
  const round = v => Math.round(v * 1e6) / 1e6;
  for (const p of pieces) {
    const own = JOINTS.indexOf(p.bone);
    if (own < 0) throw new Error('no joint ' + p.bone);
    if (!parts[p.part]) throw new Error('no part ' + p.part);
    const cache = new Map();
    const vertex = c => {
      const key = c.join(',');
      if (cache.has(key)) return cache.get(key);
      positions.push(...toMesh(tilt(p, c)).map(round));
      if (p.hinge && Math.abs(c[1] - p.hinge.y) < 0.01) {
        vcounts.push(2); vindices.push(own, 1, JOINTS.indexOf(p.hinge.bone), 1);
      } else {
        vcounts.push(1); vindices.push(own, 0);
      }
      cache.set(key, positions.length / 3 - 1);
      return positions.length / 3 - 1;
    };
    for (const f of FACES) {
      const cs = corners(p, f), ids = cs.map(vertex);
      const [u0, v0, u1, v1] = p.uv[f];
      const uvBase = uvs.length / 2;
      for (const [u, v] of [[u0, v0], [u1, v0], [u1, v1], [u0, v1]]) uvs.push(u / atlas, v / atlas);
      // The outward normal, from the (tilted) corners in mesh space.
      const [a, b, c] = [0, 3, 2].map(i => toMesh(tilt(p, cs[i])));
      const e1 = b.map((x, i) => x - a[i]), e2 = c.map((x, i) => x - a[i]);
      const n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]];
      const len = Math.hypot(...n), ni = normalIndex(n.map(x => round(x / len)));
      // Two triangles, counter-clockwise from outside: top left, bottom left, bottom right; top left, bottom right, top right.
      for (const k of [0, 3, 2, 0, 2, 1]) parts[p.part].push(ids[k], uvBase + k, ni);
    }
  }
  for (const [n, a] of Object.entries(parts)) if (!a.length) throw new Error('the mesh part ' + n + ' is empty');
  const block = (array, stride) => ({ stride, count: array.length / stride, array });
  return {
    vertices: {
      positions: block(positions, 3), uvs: block(uvs, 2), normals: block(normals, 3),
      vcounts: block(vcounts, 1), weights: block(weights, 1), vindices: block(vindices, 1),
      parts: Object.fromEntries(Object.entries(parts).map(([n, a]) => [n, block(a, 3)])),
    },
  };
}

// ---------------------------------------------------------------- the Blockbench project

const BONES = {
  Root: ['Torso', 'Thigh_R', 'Thigh_L'], Torso: ['Chest'], Chest: ['Head', 'Shoulder_R', 'Shoulder_L'], Head: [],
  Shoulder_R: ['Arm_R'], Arm_R: ['Hand_R'], Hand_R: ['Tool_R'], Tool_R: [],
  Shoulder_L: ['Arm_L'], Arm_L: ['Hand_L'], Hand_L: ['Tool_L'], Tool_L: [],
  Thigh_R: ['Leg_R'], Leg_R: [], Thigh_L: ['Leg_L'], Leg_L: [],
};

/** The project: the pieces in groups on the biped's joints, one texture per outfit (`folder` under textures/). */
function bbmodel(name, pieces, outfits, atlases, atlas, folder) {
  const elements = pieces.map(p => ({
    name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from: p.from, to: p.to, autouv: 0, color: 0, origin: p.pivot || p.from.map((v, k) => (v + p.to[k]) / 2),
    rotation: p.rot || [p.tilt || 0, 0, 0], faces: Object.fromEntries(FACES.map(f => [f, { uv: p.uv[f], texture: 0 }])),
    type: 'cube', uuid: stableId(name + '/' + p.name),
  }));
  const group = bone => ({
    name: bone, origin: at(bone), color: 0, uuid: stableId(name + '/bone/' + bone), export: true, mirror_uv: false, isOpen: true, locked: false, visibility: true,
    autouv: 0, children: [...elements.filter((e, i) => pieces[i].bone === bone).map(e => e.uuid), ...BONES[bone].map(group)],
  });
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name, model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
    timeline_setups: [], unhandled_root_fields: {}, resolution: { width: atlas, height: atlas }, elements, outliner: [group('Root')],
    textures: outfits.map((o, i) => ({
      path: '', name: o.key + '.png', folder, namespace: 'murimblock', id: String(i), width: atlas, height: atlas,
      uv_width: atlas, uv_height: atlas, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default',
      render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
      saved: false, uuid: stableId(name + '/texture/' + o.key), source: 'data:image/png;base64,' + atlases[i].toString('base64'),
    })),
  };
}

module.exports = { colour, isSide, all, folds, fringe, wraps, rim, FACES, pack, paintAtlas, png, JOINTS, PARTS, mesh, bbmodel, stableId, corners, tilt };
