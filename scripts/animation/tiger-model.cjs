// A tiger of the Murim (Blockbench free model, art/blockbench/tiger.bbmodel), in the clean blocky style Enzo chose (a
// community "tiger variations" picture as a style reference; the model and its texture are drawn here): a few big
// boxes at one texel per pixel like Minecraft's own mobs, a flat pixel-art coat. A long body, a broad head with a
// white muzzle, a pink nose and a jaw that can open on two fangs, white cheek tufts, small ears with the white spot on
// their backs, two-part legs (so they bend) with white paws and dark claw lines, a three-part tail with a dark tip.
// Bigger than a player: about 4.4 blocks from nose to tail tip, the back two blocks high.
// The coat is painted from the 3D position of every texel: stair-stepped dark stripes down the flanks and across the
// back, rings on the legs and the tail, marks on the forehead; white belly, chest, muzzle, cheeks and paws.
//
//   node scripts/animation/tiger-model.cjs        (writes art/blockbench/tiger.bbmodel and its texture)
const fs = require('node:fs');
const path = require('node:path');
const { png, stableId } = require('./mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const ATLAS = 256;
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the boxes
// x across (+x the tiger's left), y up from the ground, z along (it faces north, -z). Units: pixels.
const P = [];
const add = (name, bone, part, from, to) => P.push({ name, bone, part, from, to });
const pair = (name, bone, part, from, to) => {
  add(name + '_l', bone.replace('#', 'l'), part, from, to);
  add(name + '_r', bone.replace('#', 'r'), part, [-to[0], from[1], from[2]], [-from[0], to[1], to[2]]);
};
add('body', 'body', 'body', [-8, 16, -16], [8, 31, 16]);
add('head', 'head', 'head', [-7, 21, -27], [7, 34, -15]);
add('muzzle', 'head', 'muzzle', [-4, 22, -32], [4, 27, -27]);
pair('cheek', 'head', 'cheek', [6, 21, -26], [9, 27, -19]);
pair('ear', 'head', 'ear', [3, 34, -22], [6, 37, -20]);
pair('fang', 'head', 'fang', [2, 20, -32], [3, 22, -31]);
add('jaw', 'jaw', 'jaw', [-3, 19, -31], [3, 22, -26]);
for (const [leg, z0, z1] of [['front', -15, -8], ['hind', 8, 15]]) {
  pair(leg + '_upper', leg + '_leg_#', 'leg', [2, 8, z0], [8, leg === 'front' ? 18 : 19, z1]);
  pair(leg + '_lower', leg + '_foot_#', 'shin', [2, 0, z0], [8, 8, z1]);
  pair(leg + '_toes', leg + '_foot_#', 'toes', [2, 0, z0 - 2], [8, 3, z0]);
}
add('tail0', 'tail0', 'tail', [-2, 26, 16], [2, 30, 25]);
add('tail1', 'tail1', 'tail', [-1.5, 26.5, 25], [1.5, 29.5, 33]);
add('tail2', 'tail2', 'tail', [-1.5, 26.5, 33], [1.5, 29.5, 40]);

// ---------------------------------------------------------------- the coat
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const C = {
  orange: hex('#E39A35'), dark: hex('#C97F27'), light: hex('#F0B455'), white: hex('#EEE7DC'), shade: hex('#D9CFC1'),
  stripe: hex('#2A2F3E'), nose: hex('#D88A8A'), noseDark: hex('#9E5555'), mouth: hex('#3B1F22'), tongue: hex('#C9646C'),
  eye: hex('#E8B23A'), pupil: hex('#1E2230'), fang: hex('#F7F3EA'), claw: hex('#3A3F4E'),
};
const hash = (a, b) => { const s = Math.sin(a * 127.1 + b * 311.7) * 43758.5453; return s - Math.floor(s); };
/** A Minecraft-like speckle: a few texels a shade lighter or darker. */
const tone = (c, x, y, z) => { const h = hash(Math.floor(x * 7 + z * 3), Math.floor(y * 5 - z)); return h < 0.12 ? c.map(v => v * 0.93) : h > 0.9 ? c.map(v => Math.min(255, v * 1.05)) : c; };

/**
 * The flank stripes: one every 4 pixels along z, stepping one pixel along every 3 rows (the stair-stepped look),
 * two pixels wide near the spine and one lower down, each ending at its own height above the belly.
 */
const STRIPES = [];
for (let z = -14.5, k = 0; z < 15; k++) { STRIPES.push({ z, k, dir: hash(k, 5) < 0.5 ? -1 : 1, end: 19 + Math.floor(hash(k, 3) * 6), cross: 1 + Math.floor(hash(k, 9) * 4) }); z += 3 + Math.floor(hash(k, 7) * 3); }
/** On the flanks: does a stripe pass this texel? Each steps one pixel aside every two rows, two wide near the spine. */
function flankStripe(y, z, top) {
  const zi = Math.floor(z), d = top - y;
  return STRIPES.some(st => {
    if (y < st.end) return false;
    const c = Math.floor(st.z + st.dir * (Math.floor(d / 2) % 3));
    return zi === c || (d < 4 && zi === c + st.dir);
  });
}
/** Across the back: the same stripes, reaching in from the flanks, meeting over the spine or not. */
function backStripe(ax, z) {
  const zi = Math.floor(z);
  return STRIPES.some(st => (zi === Math.floor(st.z) || zi === Math.floor(st.z) + st.dir) && ax > st.cross - 1);
}

function paint(part, f, x, y, z, p) {
  const ax = Math.abs(x);
  const side = f === 'east' || f === 'west';
  switch (part) {
    case 'body': {
      if (f === 'down') return C.white;
      if (f === 'up') {
        if (backStripe(ax, z)) return C.stripe;
        return ax < 1 ? C.dark : C.orange;
      }
      if (f === 'north') {
        if (y < 25 && ax < 6) return y < 22 ? C.white : C.shade;
        return y > 28 && ax % 4 === 1 ? C.stripe : C.orange;
      }
      if (f === 'south') return y < 20 ? C.white : (y % 4 === 1 && ax > 2 ? C.stripe : C.orange);
      if (y < 19) return C.white;
      if (y < 20) return hash(z, 1) < 0.5 ? C.white : C.light;
      if (flankStripe(y, z, 31)) return C.stripe;
      return y > 28 ? C.dark : y < 22 ? C.light : C.orange;
    }
    case 'head': {
      if (f === 'north') {
        // Eyes: a dark brow line slanting down to the nose, an amber eye with a dark pupil, white above the brows.
        if (y >= 29 && y < 30 && ax >= 2 && ax < 5) return C.stripe;                        // the brow line
        if (y >= 27 && y < 29 && ax >= 2 && ax < 5) {                                        // the eye: white, amber, pupil
          if (ax < 3) return y >= 28 ? C.stripe : C.white;
          return ax < 4 ? (y >= 28 ? C.pupil : C.eye) : C.white;
        }
        if (y >= 30 && y < 31 && ax >= 2 && ax < 6) return C.white;                         // white over the eye
        if (y >= 31) {                                                                       // forehead marks
          if (ax < 1 && y >= 32) return C.stripe;
          if ((ax >= 3 && ax < 4 && y >= 32) || (ax >= 6 && y >= 31 && y < 33)) return C.stripe;
          return C.orange;
        }
        if (ax >= 5 && y < 30) return C.white;                                              // the white cheeks
        return ax < 2 ? C.light : C.orange;
      }
      if (f === 'up') return ((Math.floor(z) + 30) % 4 === 0 && ax > 1.5 && ax < 5.5) ? C.stripe : C.orange;
      if (f === 'down') return C.white;
      if (f === 'south') return C.orange;
      if (y < 26) return C.white;
      return (Math.floor(y) + Math.floor(z / 2)) % 5 === 0 && z < -18 && y > 27 ? C.stripe : C.orange;
    }
    case 'muzzle': {
      if (f === 'up') return z < -30 && ax < 2 ? C.nose : ax < 1 ? C.light : C.orange;
      if (f === 'north') {
        if (y >= 25 && ax < 2) return y >= 26 || ax < 1 ? C.nose : C.noseDark;               // the nose
        if (ax < 1 && y >= 23) return C.mouth;                                               // under the nose
        if (y < 23 && ax >= 1 && ax < 3) return C.mouth;                                     // the lips, each side
        if (y < 25 && y >= 23 && ax >= 2 && (Math.floor(ax) + Math.floor(y)) % 2 === 0) return C.shade; // whisker dots
        return C.white;
      }
      if (side) return y >= 26 ? C.orange : y < 23 ? C.mouth : C.white;
      return C.white;
    }
    case 'cheek': return y >= 26 ? C.orange : (f !== 'down' && (y + z) % 4 === 0 && y > 22) ? C.stripe : C.white;
    case 'ear': {
      const cx = (p.from[0] + p.to[0]) / 2;
      if (f === 'north') return y >= 36 || Math.abs(x - cx) > 1 ? C.stripe : C.white;
      if (f === 'south') return Math.abs(x - cx) < 1 && y >= 35 && y < 36 ? C.white : C.stripe;
      return y >= 36 ? C.stripe : C.orange;
    }
    case 'fang': return C.fang;
    case 'jaw': {
      if (f === 'up') return z < -29 ? C.mouth : C.tongue;
      if (f === 'north') return C.white;
      return C.white;
    }
    case 'leg': {
      const inner = (f === 'east' && x < 0) || (f === 'west' && x > 0) || f === 'south';
      if (inner || f === 'down') return C.white;
      if (f === 'up') return C.orange;
      return y % 4 === 1 && y > 9 ? C.stripe : y < 10 ? C.light : C.orange;
    }
    case 'shin': {
      if (f === 'up') return C.orange;
      if (f === 'down') return C.shade;
      const outer = (f === 'east' && x > 0) || (f === 'west' && x < 0);
      return outer && y >= 6 && y < 7 ? C.stripe : C.white;
    }
    case 'toes': {
      // Claw lines between the toes, and dark claw tips along the front edge.
      if (f === 'down') return C.shade;
      const rel = x - p.from[0];
      if ((f === 'north' || f === 'up') && Math.floor(rel) % 2 === 1 && rel > 0.5 && rel < 5.5) return C.claw;
      if (f === 'north' && y < 1) return C.shade;
      return C.white;
    }
    case 'tail': {
      if (p.name === 'tail2' && z >= 37) return C.stripe;                                      // the dark tip
      if (f === 'down') return C.white;
      return Math.floor(z) % 3 === 0 ? C.stripe : C.orange;
    }
  }
  return [255, 0, 255];
}

// ---------------------------------------------------------------- packing and painting
const dims = (p, f) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to, r = d => Math.max(1, Math.round(d));
  if (f === 'north' || f === 'south') return [r(x1 - x0), r(y1 - y0)];
  if (f === 'east' || f === 'west') return [r(z1 - z0), r(y1 - y0)];
  return [r(x1 - x0), r(z1 - z0)];
};
function point(p, f, u, v, w, h) {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  const a = (u + 0.5) / w, b = (v + 0.5) / h, Y = y1 - b * (y1 - y0);
  switch (f) {
    case 'north': return [x1 - a * (x1 - x0), Y, z0];
    case 'south': return [x0 + a * (x1 - x0), Y, z1];
    case 'east': return [x1, Y, z1 - a * (z1 - z0)];
    case 'west': return [x0, Y, z0 + a * (z1 - z0)];
    case 'up': return [x0 + a * (x1 - x0), y1, z0 + b * (z1 - z0)];
    default: return [x0 + a * (x1 - x0), y0, z1 - b * (z1 - z0)];
  }
}
const faces = [];
for (const p of P) for (const f of FACES) { const [w, h] = dims(p, f); faces.push({ p, f, w, h }); }
faces.sort((a, b) => b.h - a.h || b.w - a.w);
let cx = 0, cy = 0, shelf = 0;
for (const face of faces) {
  if (cx + face.w > ATLAS) { cx = 0; cy += shelf; shelf = 0; }
  face.u = cx; face.v = cy; cx += face.w; shelf = Math.max(shelf, face.h);
}
if (cy + shelf > ATLAS) throw new Error('the atlas is full: ' + (cy + shelf));
const img = new Uint8Array(ATLAS * ATLAS * 4);
for (const face of faces) {
  (face.p.uv ||= {})[face.f] = [face.u, face.v, face.u + face.w, face.v + face.h];
  for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
    const [x, y, z] = point(face.p, face.f, u, v, face.w, face.h);
    const c = tone(paint(face.p.part, face.f, x, y, z, face.p), x, y, z).map(n => Math.max(0, Math.min(255, Math.round(n))));
    img.set([...c, 255], ((face.v + v) * ATLAS + face.u + u) * 4);
  }
}

// ---------------------------------------------------------------- the project
const elements = P.map(p => ({
  name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
  from: p.from, to: p.to, autouv: 0, color: 0, origin: p.from.map((v, k) => (v + p.to[k]) / 2), rotation: [0, 0, 0],
  faces: Object.fromEntries(FACES.map(f => [f, { uv: p.uv[f], texture: 0 }])), type: 'cube', uuid: stableId('tiger/' + p.name),
}));
// The joints, and the tail's rest pose: drooping from the hips, then curling up.
const BONES = {
  body: { origin: [0, 24, 0] }, head: { origin: [0, 27, -15] }, jaw: { origin: [0, 21, -26] },
  front_leg_l: { origin: [5, 17, -11.5] }, front_leg_r: { origin: [-5, 17, -11.5] },
  front_foot_l: { origin: [5, 8, -11.5] }, front_foot_r: { origin: [-5, 8, -11.5] },
  hind_leg_l: { origin: [5, 18, 11.5] }, hind_leg_r: { origin: [-5, 18, 11.5] },
  hind_foot_l: { origin: [5, 8, 11.5] }, hind_foot_r: { origin: [-5, 8, 11.5] },
  tail0: { origin: [0, 28, 16], rotation: [32, 0, 0] }, tail1: { origin: [0, 28, 25], rotation: [-14, 0, 0] },
  tail2: { origin: [0, 28, 33], rotation: [-26, 0, 0] },
};
const group = (name, children = []) => ({ name, origin: BONES[name].origin, rotation: BONES[name].rotation || [0, 0, 0], color: 0,
  uuid: stableId('tiger/bone/' + name), export: true, mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0,
  children: [...elements.filter((e, i) => P[i].bone === name).map(e => e.uuid), ...children] });
const leg = (name, foot) => group(name, [group(foot)]);
const outliner = [group('body', [group('head', [group('jaw')]), leg('front_leg_l', 'front_foot_l'), leg('front_leg_r', 'front_foot_r'),
  leg('hind_leg_l', 'hind_foot_l'), leg('hind_leg_r', 'hind_foot_r'), group('tail0', [group('tail1', [group('tail2')])])])];
const texture = png(ATLAS, ATLAS, img);
const project = {
  meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: 'tiger', model_identifier: '', visible_box: [1, 1, 0],
  variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
  resolution: { width: ATLAS, height: ATLAS }, elements, outliner,
  textures: [{ path: '', name: 'tiger.png', folder: 'entity', namespace: 'murimblock', id: '0', width: ATLAS, height: ATLAS, uv_width: ATLAS,
    uv_height: ATLAS, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default',
    render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
    saved: false, uuid: stableId('tiger/texture'), source: 'data:image/png;base64,' + texture.toString('base64') }],
};
const file = path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel');
fs.mkdirSync(path.dirname(file), { recursive: true });
fs.writeFileSync(file, JSON.stringify(project));
fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.png'), texture);
console.log('tiger:', P.length, 'cubes, atlas', ATLAS, 'used to row', cy + shelf);
