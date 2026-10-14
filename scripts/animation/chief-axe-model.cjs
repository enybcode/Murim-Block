// The Bandit Chief's great axe, the Crescent Moon (月牙斧): a crescent blade whose horns curl back, on a haft taller than a man, a back
// spike, an iron point on top, the grip wrapped in red cord, bronze rings, a red tassel under the blade.
//   models/item/chief_great_axe_3d.json   the axe in the hand (Epic Fight's greatsword transforms, a little smaller);
//   models/item/chief_great_axe.json      NeoForge's separate transforms: the inventory, the ground and frames keep
//                                         the flat icon textures/item/chief_great_axe.png, drawn here from the model;
//   art/blockbench/chief_great_axe.bbmodel  the Blockbench source.
// Built upright (the haft along +y), then every cube is turned 45 degrees about the model's centre onto the icon's
// diagonal, as the Bandit Dao: the blade on the -x side, so in the hand its edge faces down and leads the cuts made for
// the dao (bandit-dao-model.cjs). The right hand holds the haft near y 1, the left hand below it.
// Usage: node scripts/animation/chief-axe-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');

// ---------------------------------------------------------------- the 16 x 16 texture of the 3D model

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(16 * 16 * 4);
const paint = (x, y, h) => { TEX.set([...hex(h), 255], (y * 16 + x) * 4); };
// Row 0-1: the blade's flat, from the socket (left) to the edge (right); row 2: the edge; row 3: the socket's iron.
['#5a6068', '#646a72', '#6e757d', '#7a8189', '#868d95', '#9299a0', '#9ea5ab', '#aab0b6',
  '#b5bbc0', '#bfc5c9', '#c9ced2', '#d3d7da', '#dde0e3', '#e6e9eb', '#eff1f2', '#f8f9fa'].forEach((c, x) => { paint(x, 0, c); paint(x, 1, c); });
for (let x = 0; x < 16; x++) paint(x, 2, x < 8 ? '#ffffff' : '#e9edf0');
for (let x = 0; x < 16; x++) paint(x, 3, x % 4 === 0 ? '#3d4248' : '#565c63');
for (let x = 0; x < 16; x++) paint(x, 4, x % 3 === 0 ? '#3a2416' : '#4e3220');                    // the haft's wood
for (let y = 5; y < 9; y++) for (let x = 0; x < 16; x++) paint(x, y, (x + y) % 2 ? '#8e2a22' : '#5e1814');   // red cord
for (let x = 0; x < 16; x++) paint(x, 9, x % 2 ? '#b08a36' : '#c9a24a');                            // bronze
for (let y = 10; y < 13; y++) for (let x = 0; x < 16; x++) paint(x, y, y === 12 ? '#7e2018' : '#c4382c');  // tassel

const MAT = {
  blade: { north: [0, 0, 16, 1], south: [16, 1, 0, 2], east: [0, 3, 16, 4], west: [0, 2, 16, 3], up: [0, 3, 16, 4], down: [0, 3, 16, 4] },
  edge: { north: [8, 0, 16, 1], south: [16, 1, 8, 2], east: [0, 2, 8, 3], west: [0, 2, 8, 3], up: [0, 2, 8, 3], down: [0, 2, 8, 3] },
  iron: { all: [0, 3, 16, 4] },
  wood: { all: [0, 4, 16, 5] },
  cord: { all: [0, 5, 16, 9] },
  bronze: { all: [0, 9, 16, 10] },
  tassel: { all: [0, 10, 16, 13] },
};
/** The colour of each material on the icon. */
const ICON = { blade: '#a9b0b6', edge: '#f2f4f6', iron: '#565c63', wood: '#4e3220', cord: '#8e2a22', bronze: '#c39a3c', tassel: '#c4382c' };

// ---------------------------------------------------------------- the cubes (upright: the haft along +y, the blade toward -x)

const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
part('pommel', [7.1, -6.6, 7.1], [8.9, -5.6, 8.9], 'iron');
part('haft', [7.4, -5.6, 7.4], [8.6, 20.0, 8.6], 'wood');
part('grip', [7.2, -4.2, 7.2], [8.8, 3.2, 8.8], 'cord');
part('ring_low', [7.1, 11.85, 7.1], [8.9, 12.55, 8.9], 'bronze');
part('socket', [6.8, 12.55, 6.9], [9.2, 20.0, 9.1], 'iron');
part('point', [7.6, 20.0, 7.6], [8.4, 22.6, 8.4], 'iron');
// The crescent (Crescent Moon, the style chosen on the concept sheet, scripts/concepts/chief-axes.cjs): a narrow neck,
// the blade widening out to a long curved edge, its two horns curling back toward the haft.
part('blade_neck', [5.1, 14.3, 7.65], [6.8, 17.7, 8.35], 'blade');
part('blade_mid', [3.55, 13.25, 7.68], [5.1, 18.8, 8.32], 'blade');
part('blade_wide', [2.0, 12.0, 7.72], [3.55, 20.0, 8.28], 'blade');
part('blade_edge', [0.75, 10.8, 7.77], [2.0, 21.25, 8.23], 'edge');
part('horn_top', [0.75, 21.25, 7.77], [3.25, 22.2, 8.23], 'edge');
part('horn_top_tip', [2.75, 22.2, 7.8], [3.85, 22.95, 8.2], 'edge');
part('horn_low', [0.75, 9.85, 7.77], [3.25, 10.8, 8.23], 'edge');
part('horn_low_tip', [2.75, 9.1, 7.8], [3.85, 9.85, 8.2], 'edge');
// The back spike, and the tassel hanging under the blade's neck.
part('spike', [9.2, 15.25, 7.7], [11.7, 17.1, 8.3], 'iron');
part('spike_tip', [11.7, 15.7, 7.8], [12.9, 16.55, 8.2], 'edge');
part('tassel_cord', [6.0, 8.0, 7.8], [6.75, 11.85, 8.2], 'tassel');
part('tassel_tuft', [5.6, 6.2, 7.6], [7.15, 8.0, 8.4], 'tassel');

const ROTATION = { angle: -45, axis: 'z', origin: [8, 8, 8] };

function faces(mat) {
  const m = MAT[mat], out = {};
  for (const f of ['north', 'east', 'south', 'west', 'up', 'down']) out[f] = { uv: m[f] || m.all, texture: '#0' };
  return out;
}

// ---------------------------------------------------------------- the icon: the model seen from the front, fitted to 16 x 16

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
  const quads = PARTS.map(p => ({ mat: p.mat, pts: [[p.from[0], p.from[1]], [p.to[0], p.from[1]], [p.to[0], p.to[1]], [p.from[0], p.to[1]]].map(turn) }));
  const xs = quads.flatMap(q => q.pts.map(p => p[0])), ys = quads.flatMap(q => q.pts.map(p => p[1]));
  const x0 = Math.min(...xs), y0 = Math.min(...ys), span = Math.max(Math.max(...xs) - x0, Math.max(...ys) - y0);
  const inside = (pts, x, y) => {
    let c = false;
    for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
      const [xi, yi] = pts[i], [xj, yj] = pts[j];
      if ((yi > y) !== (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c;
    }
    return c;
  };
  // Later parts drawn over earlier ones; the haft under the blade.
  const order = ['tassel', 'wood', 'cord', 'iron', 'bronze', 'blade', 'edge'];
  for (let py = 0; py < 16; py++) for (let px = 0; px < 16; px++) {
    // Pixel centre in model units (the icon's y goes down).
    const x = x0 + (px + 0.5) / 16 * span, y = y0 + (15.5 - py) / 16 * span;
    let hit = null;
    for (const mat of order) for (const q of quads) if (q.mat === mat && inside(q.pts, x, y)) hit = mat;
    if (hit) img.set([...hex(ICON[hit]), 255], (py * 16 + px) * 4);
  }
  // A dark outline round the shape, as Minecraft's items have.
  const out = new Uint8Array(img);
  for (let py = 0; py < 16; py++) for (let px = 0; px < 16; px++) {
    if (img[(py * 16 + px) * 4 + 3]) continue;
    const near = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => {
      const nx = px + dx, ny = py + dy;
      return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3];
    });
    if (near) out.set([28, 22, 20, 255], (py * 16 + px) * 4);
  }
  return out;
}

// ---------------------------------------------------------------- the game models

/** Epic Fight's greatsword transforms, smaller (1.3 instead of 2: the chief himself is drawn 1.3 times larger): the haft in both hands. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 45], translation: [0, 8.5, 0], scale: [1.3, 1.3, 1.3] },
  thirdperson_lefthand: { rotation: [0, 90, -45], translation: [0, 8.5, 0], scale: [1.3, 1.3, 1.3] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = 'Murimblock - scripts/animation/chief-axe-model.cjs (Blockbench source: art/blockbench/chief_great_axe.bbmodel)';
const model3d = {
  credit: CREDIT, texture_size: [16, 16],
  textures: { 0: 'murimblock:item/chief_great_axe_3d', particle: 'murimblock:item/chief_great_axe' },
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: faces(p.mat) })),
  display: DISPLAY,
};
const flat = { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/chief_great_axe' } };
const itemModel = { loader: 'neoforge:separate_transforms', base: { parent: 'murimblock:item/chief_great_axe_3d' }, perspectives: { gui: flat, ground: flat, fixed: flat } };

function bbmodel() {
  const id = name => { const h = crypto.createHash('md5').update('chief_great_axe/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
  const elements = PARTS.map(p => {
    const f = faces(p.mat);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin, faces: f, type: 'cube', uuid: id(p.name) };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: 'chief_great_axe', parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: 'chief_great_axe_3d.png', folder: 'item', namespace: 'murimblock', id: '0', width: 16, height: 16,
      uv_width: 16, uv_height: 16, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(16, 16, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'chief_great_axe_3d.png'), png(16, 16, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'chief_great_axe.png'), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'chief_great_axe_3d.json'), JSON.stringify(model3d, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'chief_great_axe.json'), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'chief_great_axe.bbmodel'), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the great axe:', PARTS.length, 'cubes');
}
