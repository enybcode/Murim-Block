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
// Colour ramps, darkest first: each texel takes a level, nudged by a fur grain (single texels a shade up or down,
// strands along the hair, a soft mottling), so no area is one flat colour.
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const RAMP = {
  O: ['#8F4E16', '#AE611C', '#C77424', '#DC8D2F', '#E9A340', '#F2B858', '#F8CC78'].map(hex),
  W: ['#ABA192', '#C2B8A9', '#D6CDBF', '#E5DED3', '#F0EBE3', '#F8F5EF'].map(hex),
  S: ['#11141C', '#1C2130', '#282E40', '#363E54'].map(hex),
  N: ['#8E4B4B', '#B86A6A', '#D68888', '#E6A3A0'].map(hex),
  M: ['#2A1416', '#3B1F22', '#5A2D33'].map(hex),
};
const C = { eye: hex('#E8B23A'), eyeLight: hex('#F5D26A'), pupil: hex('#141822'), fang: hex('#F7F3EA'), tongue: hex('#C9646C') };
const hash = (a, b) => { const s = Math.sin(a * 127.1 + b * 311.7) * 43758.5453; return s - Math.floor(s); };
function vnoise(x, y) {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi, s = t => t * t * (3 - 2 * t);
  const a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
  return a + (b - a) * s(xf) + (c - a) * s(yf) + (a - b - c + d) * s(xf) * s(yf);
}
/** A colour from a ramp: [ramp, level]; the fur grain moves the level. Plain arrays are exact colours. */
const sh = (r, l) => ({ r, l });
function tone(c, x, y, z, f) {
  if (Array.isArray(c)) return c;
  const ramp = RAMP[c.r];
  const fx = Math.floor(x + 100), fy = Math.floor(y + 100), fz = Math.floor(z + 100);
  const [u, v] = f === 'up' || f === 'down' ? [fx, fz] : f === 'east' || f === 'west' ? [fz, fy] : [fx, fy];
  const strand = (f === 'up' || f === 'down') ? hash(u * 3, 7) : hash(u, 13);            // hair running down the face
  const g = hash(u * 1.7 + (f === 'north' ? 5 : f === 'south' ? 9 : f === 'up' ? 3 : 1), v * 2.3);
  let l = c.l + (vnoise(u / 3.5, v / 3.5) - 0.5) * 1.1 + (strand < 0.18 ? -0.6 : strand > 0.86 ? 0.4 : 0);
  if (g < 0.12) l -= 1; else if (g > 0.9) l += 1;
  return ramp[Math.max(0, Math.min(ramp.length - 1, Math.round(l)))];
}

// The flank stripes: close together (gaps of 2 or 3), each stepping its own way down the flank, wide near the spine and
// thinning, some forking low down into two prongs, each ending at its own height; short flecks between some of them.
const STRIPES = [];
for (let z = -15.5, k = 0; z < 15.5; k++) {
  STRIPES.push({ z, k, dir: hash(k, 5) < 0.5 ? -1 : 1, end: 17 + Math.floor(hash(k, 3) * 7), cross: Math.floor(hash(k, 9) * 4),
    fork: hash(k, 21) < 0.4 ? 21 + Math.floor(hash(k, 23) * 4) : 0, fleck: hash(k, 31) < 0.5 ? 22 + Math.floor(hash(k, 33) * 6) : 0 });
  z += 2.5 + Math.floor(hash(k, 7) * 2);
}
function flankStripe(y, z, top) {
  const zi = Math.floor(z), d = top - y;
  return STRIPES.some(st => {
    if (y < st.end) return false;
    const c = Math.floor(st.z + st.dir * (Math.floor(d / 2) % 3));
    if (st.fork && y < st.fork) return zi === c - 1 || zi === c + 1;                       // the two prongs
    if (zi === c) return true;
    if (d < 4 && zi === c + st.dir) return true;                                            // wide by the spine
    return st.fleck && zi === c + 2 * st.dir && y >= st.fleck && y < st.fleck + 2;          // a fleck beside it
  });
}
function backStripe(ax, z) {
  const zi = Math.floor(z);
  return STRIPES.some(st => (zi === Math.floor(st.z) || (ax > 3 && zi === Math.floor(st.z) + st.dir)) && ax >= st.cross);
}
/** The ragged edge of the white belly fur along a flank. */
const bellyLine = z => 19 + Math.floor(hash(Math.floor(z + 50), 11) * 2.5);

function paint(part, f, x, y, z, p) {
  const ax = Math.abs(x);
  const side = f === 'east' || f === 'west';
  switch (part) {
    case 'body': {
      if (f === 'down') return sh('W', 2.4);
      if (f === 'up') return backStripe(ax, z) ? sh('S', 1) : sh('O', ax < 1.5 ? 1.4 : 2.1);
      if (f === 'north') {
        if (y < 25 && ax < 5.5) {                                                           // the white chest
          if (ax > 3.5 && (Math.floor(y) + Math.floor(ax)) % 3 === 0 && y > 20) return sh('S', 2);
          return sh('W', y < 21 ? 3 : 3.8);
        }
        if (y > 26 && Math.floor(ax) % 3 === 1 && y < 31 - (ax % 2)) return sh('S', 1);
        return sh('O', 2.6);
      }
      if (f === 'south') {
        if (y < bellyLine(ax * 3)) return sh('W', 3);
        if (ax > 1.5 && (Math.floor(y) + Math.floor(ax / 2)) % 3 === 0) return sh('S', 1);
        return sh('O', 2.4);
      }
      const b = bellyLine(z);
      if (y < b) return sh('W', 3.2 + (y - 16) * 0.2);
      if (y < b + 1) return sh('O', 5.2);
      if (flankStripe(y, z, 31)) return sh('S', 1);
      return sh('O', 1.6 + (31 - y) / 12 * 3);
    }
    case 'head': {
      if (f === 'north') {
        if (y >= 29 && y < 30 && ax >= 1.5 && ax < 5) return sh('S', 0.5);                // the brow line
        if (y >= 27 && y < 29 && ax >= 2 && ax < 5) {                                        // the eye
          if (ax < 3) return y >= 28 ? sh('S', 1) : sh('W', 4);
          if (ax < 4) return y >= 28 ? C.pupil : C.eye;
          return y >= 28 ? C.eyeLight : sh('W', 4);
        }
        if (y >= 30 && y < 31 && ax >= 1.5 && ax < 6) return sh('W', 4.5);                 // white over the eye
        if (y >= 31) {                                                                       // forehead marks
          if (ax < 1 && y >= 31.5) return sh('S', 1);
          if (ax >= 2 && ax < 3 && y >= 32) return sh('S', 1);
          if (ax >= 3.5 && ax < 4.5 && y >= 31 && y < 33) return sh('S', 1);
          return sh('O', 2.4);
        }
        if (ax >= 5) return (ax >= 5.5 && y > 23 && Math.floor(y) - Math.floor(ax / 2) === 21 + Math.floor((ax - 5.5) / 2) % 1 * 0 || (ax >= 5.5 && y > 24 && y < 28 && Math.floor(y) === Math.floor(ax / 2) + 22)) ? sh('S', 1.5) : sh('W', 3.6);  // cheeks: two slanting streaks
        return sh('O', ax < 1.5 ? 4.4 : 3.4);
      }
      if (f === 'up') return ((Math.floor(z) + 31) % 3 === 0 && ax > 1 && ax < 6) || (ax < 1 && Math.floor(z) % 3 === 0) ? sh('S', 1) : sh('O', 2);
      if (f === 'down') return sh('W', 2.5);
      if (f === 'south') return (Math.floor(y) % 3 === 0 && ax > 2) ? sh('S', 1) : sh('O', 2.2);
      if (y < 25 + (Math.floor(z) % 2)) return (Math.floor(y + z * 0.5) % 3 === 0 && y > 22) ? sh('S', 2) : sh('W', 3.4);
      return (Math.floor(y + z * 0.6) % 3 === 0) ? sh('S', 1) : sh('O', 1.8 + (34 - y) / 9 * 2);
    }
    case 'muzzle': {
      if (f === 'up') return z < -30 && ax < 2 ? sh('N', 2) : ax < 1 ? sh('O', 4.6) : (Math.floor(z) % 2 === 0 && ax > 2.5 ? sh('S', 2) : sh('O', 3.4));
      if (f === 'north') {
        if (y >= 25 && ax < 2) return y >= 26 ? sh('N', ax < 1 ? 3 : 2) : ax < 1 ? sh('N', 1) : sh('N', 0);   // the nose
        if (ax < 1 && y >= 23) return sh('M', 1);
        if (y < 23 && ax >= 1 && ax < 3) return sh('M', 1);
        if (y < 25 && y >= 23 && ax >= 2 && (Math.floor(ax) + Math.floor(y)) % 2 === 0) return sh('W', 0.6);  // whisker dots
        return sh('W', 4);
      }
      if (side) return y >= 26 ? sh('O', 3) : y < 23 ? sh('M', 1) : (Math.floor(z) % 2 === 0 && y < 25 ? sh('W', 1) : sh('W', 3.6));
      return sh('W', 3);
    }
    case 'cheek': {
      if (y >= 26) return sh('O', 2.6);
      if (f !== 'down' && (Math.floor(y) + Math.floor(z)) % 3 === 0 && y > 22) return sh('S', 2);
      return sh('W', f === 'down' ? 2 : 3.8);
    }
    case 'ear': {
      const cx = (p.from[0] + p.to[0]) / 2;
      if (f === 'north') return y >= 36 || Math.abs(x - cx) > 1 ? sh('S', 1) : sh('W', 4.4);
      if (f === 'south') return Math.abs(x - cx) < 1 && y >= 35 && y < 36 ? sh('W', 5) : sh('S', 1);
      return y >= 36 ? sh('S', 1) : sh('O', 2.4);
    }
    case 'fang': return C.fang;
    case 'jaw': {
      if (f === 'up') return z < -29 ? sh('M', 0) : C.tongue;
      return sh('W', f === 'down' ? 2 : 3.6);
    }
    case 'leg': {
      const inner = (f === 'east' && x < 0) || (f === 'west' && x > 0) || f === 'south';
      if (inner || f === 'down') return sh('W', 3.2);
      if (f === 'up') return sh('O', 2);
      const u = f === 'north' ? ax : z;
      // Bands round the leg, stepping a row every three pixels across, one in five broken.
      const band = Math.floor(y) + Math.floor((u + 20) / 3) % 2;
      if (y > 9 && band % 3 === 0 && hash(band, Math.floor((u + 20) / 2)) > 0.2) return sh('S', 1);
      return y < 10 ? sh('O', 5) : sh('O', 2.2 + (19 - y) / 9 * 2);
    }
    case 'shin': {
      if (f === 'up') return sh('O', 3);
      if (f === 'down') return sh('W', 1.6);
      const outer = (f === 'east' && x > 0) || (f === 'west' && x < 0);
      if (outer && y >= 6) return Math.floor(y) === 6 ? sh('S', 1) : sh('O', 4.6);
      if (outer && y >= 3 && y < 4 && Math.floor(z) % 2 === 0) return sh('S', 2);
      return sh('W', 3.6 - (y < 1 ? 1 : 0));
    }
    case 'toes': {
      if (f === 'down') return sh('W', 1.4);
      const rel = x - p.from[0];
      if ((f === 'north' || f === 'up') && Math.floor(rel) % 2 === 1 && rel > 0.5 && rel < 5.5) return sh('S', 2);   // claw lines
      if (f === 'north' && y < 1) return sh('W', 1.8);
      return sh('W', 4);
    }
    case 'tail': {
      if (p.name === 'tail2' && z >= 37) return sh('S', 1);                                     // the dark tip
      if (f === 'down') return sh('W', 3);
      const zi = Math.floor(z);
      if ((zi + (f === 'up' ? 1 : 0)) % 3 === 0 || (p.name === 'tail2' && zi % 3 === 1)) return sh('S', 1);
      return sh('O', f === 'up' ? 2 : 3);
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
    const c = tone(paint(face.p.part, face.f, x, y, z, face.p), x, y, z, face.f).map(n => Math.max(0, Math.min(255, Math.round(n))));
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
