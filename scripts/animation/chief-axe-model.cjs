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

// ---------------------------------------------------------------- the 32 x 32 texture of the 3D model

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
/** A little noise that stays the same for a given texel. */
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
/**
 * Sixteen 8 x 8 swatches, one per material, each painted texel by texel: (u, v) inside the swatch -> [colour, light].
 * A face takes the corner of its swatch that matches its own size (two texels per model pixel), so its details are not
 * stretched.
 */
const SWATCHES = {
  // Hammered steel in four tones, from the dark neck to the bright band by the edge.
  steel1: (u, v) => ['#626970', 0.92 + noise(u, v) * 0.12],
  steel2: (u, v) => ['#7d848b', 0.92 + noise(u, v) * 0.12],
  steel3: (u, v) => ['#999fa6', 0.93 + noise(u, v) * 0.1],
  steel4: (u, v) => ['#b7bdc2', 0.94 + noise(u, v) * 0.08],
  edge: (u, v) => ['#eef2f5', 0.97 + (v % 4 === 0 ? 0.05 : 0)],
  // Iron with rivet heads; blackened iron for the blade's root plates.
  iron: (u, v) => [(u % 4 === 1 && v % 4 === 1) ? '#8a9097' : '#50565d', 0.95 + noise(u, v) * 0.08],
  ironDark: (u, v) => ['#33373c', 0.94 + noise(u, v) * 0.1],
  // Bronze with an engraved wave running round it.
  bronze: (u, v) => [((u + Math.floor(v / 2) * 2) % 6 === 0) ? '#8a6a28' : '#c39a3c', 0.95 + noise(u, v) * 0.08],
  bronzeDark: (u, v) => ['#8a6a28', 0.95 + noise(u, v) * 0.08],
  // The haft: dark wood, its grain running along it.
  wood: (u, v) => [(u % 3 === 0) ? '#3a2416' : '#4e3220', 0.94 + noise(u, v) * 0.1],
  // Red cord wound slantwise, a dark line between turns.
  cord: (u, v) => [((u + v) % 4 === 0) ? '#4a120e' : ((u + v) % 4 === 1 ? '#a8322a' : '#8e2a22'), 1],
  // Leather, a row of stitches.
  leather: (u, v) => [v % 4 === 2 && u % 2 === 0 ? '#b59a6a' : '#3a2618', 0.95 + noise(u, v) * 0.08],
  tassel: (u, v) => [(u % 2) ? '#a52c22' : '#cf3f30', 1 - (v / 16)],
  jade: (u, v) => ['#3f8f6e', 0.9 + ((u + v) % 3) * 0.08],
  engraving: (u, v) => ['#4c5258', 1],
  ridge: (u, v) => ['#dfe4e8', 1],
};
const SWATCH_AT = Object.fromEntries(Object.keys(SWATCHES).map((m, i) => [m, [(i % 4) * 8, Math.floor(i / 4) * 8]]));
for (const [m, paintIt] of Object.entries(SWATCHES)) {
  const [sx, sy] = SWATCH_AT[m];
  for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
    const [c, light] = paintIt(u, v);
    TEX.set([...shade(c, light), 255], ((sy + v) * TEX_SIZE + sx + u) * 4);
  }
}
/** The colour of each material on the icon. */
const ICON = { steel1: '#6a7178', steel2: '#858c93', steel3: '#a1a7ad', steel4: '#bcc2c7', edge: '#f2f4f6', iron: '#565c63', ironDark: '#33373c',
  bronze: '#c39a3c', bronzeDark: '#8a6a28', wood: '#4e3220', cord: '#8e2a22', leather: '#3a2618', tassel: '#c4382c', jade: '#3f8f6e',
  engraving: '#4c5258', ridge: '#dfe4e8' };

// ---------------------------------------------------------------- the cubes (upright: the haft along +y, the blade toward -x)

const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
/** The same piece on both faces of the blade: z0..z1 in front, mirrored about z 8 behind. */
const both = (name, [x0, y0], [x1, y1], [z0, z1], mat) => {
  part(name + '_front', [x0, y0, z0], [x1, y1, z1], mat);
  part(name + '_back', [x0, y0, 16 - z1], [x1, y1, 16 - z0], mat);
};

// The foot: a bronze cap and a short iron spike.
part('pommel_tip', [7.8, -8.7, 7.8], [8.2, -8.1, 8.2], 'edge');
part('pommel_spike', [7.6, -8.1, 7.6], [8.4, -6.9, 8.4], 'iron');
part('pommel_cap', [7.05, -6.9, 7.05], [8.95, -5.7, 8.95], 'bronze');
// The haft, its grip wound in red cord between two bronze rings, a ring higher up, a leather collar under the head.
part('haft', [7.4, -5.7, 7.4], [8.6, 20.0, 8.6], 'wood');
part('grip', [7.2, -4.4, 7.2], [8.8, 3.4, 8.8], 'cord');
part('grip_ring_low', [7.1, -4.8, 7.1], [8.9, -4.4, 8.9], 'bronze');
part('grip_ring_high', [7.1, 3.4, 7.1], [8.9, 3.8, 8.9], 'bronze');
part('haft_ring', [7.15, 7.2, 7.15], [8.85, 7.6, 8.85], 'bronze');
part('collar', [7.25, 10.2, 7.25], [8.75, 11.85, 8.75], 'leather');
part('ring_low', [7.05, 11.85, 7.05], [8.95, 12.55, 8.95], 'bronze');
// Iron langets riveted down the haft from the socket, front and back.
both('langet', [7.75, 9.0], [8.25, 12.55], [7.25, 7.4], 'iron');
both('langet_rivet', [7.85, 9.6], [8.15, 9.9], [7.15, 7.25], 'bronze');
// The socket, banded in bronze top and bottom, a rivet through its middle.
part('socket', [6.8, 12.55, 6.9], [9.2, 20.0, 9.1], 'iron');
part('socket_band_low', [6.65, 12.55, 6.75], [9.35, 13.2, 9.25], 'bronze');
part('socket_band_high', [6.65, 19.35, 6.75], [9.35, 20.0, 9.25], 'bronze');
both('socket_rivet', [7.6, 16.1], [8.4, 16.9], [6.75, 6.9], 'bronze');
// On top, a leaf-shaped point with a ridge down its middle.
part('point_base', [7.55, 20.0, 7.55], [8.45, 20.8, 8.45], 'iron');
part('point_leaf', [7.3, 20.8, 7.75], [8.7, 22.4, 8.25], 'steel4');
part('point_upper', [7.55, 22.4, 7.8], [8.45, 23.4, 8.2], 'steel4');
part('point_tip', [7.8, 23.4, 7.85], [8.2, 24.2, 8.15], 'edge');
part('point_ridge', [7.9, 20.8, 7.7], [8.1, 23.4, 8.3], 'ridge');

// The crescent (Crescent Moon, the style chosen on the concept sheet, scripts/concepts/chief-axes.cjs): a narrow neck
// held by riveted plates, the blade widening out to a long curved edge, thinner and brighter along its bevel, a round
// moon hole ringed in bronze, a cloud engraved on its flat, its two horns curling back toward the haft.
part('blade_neck', [5.1, 14.3, 7.65], [5.4, 17.7, 8.35], 'steel1');
both('root_plate', [5.4, 14.0], [6.8, 18.0], [7.55, 7.65], 'ironDark');
part('root_plate_core', [5.4, 14.0, 7.65], [6.8, 18.0, 8.35], 'ironDark');
both('root_rivet_low', [5.85, 14.6], [6.25, 15.0], [7.48, 7.55], 'bronze');
both('root_rivet_high', [5.85, 17.0], [6.25, 17.4], [7.48, 7.55], 'bronze');
part('blade_mid', [3.55, 13.25, 7.68], [5.1, 18.8, 8.32], 'steel2');
both('cloud_a', [3.9, 15.6], [4.7, 15.9], [7.62, 7.68], 'engraving');
both('cloud_b', [4.35, 15.9], [4.65, 16.6], [7.62, 7.68], 'engraving');
both('cloud_c', [3.9, 16.6], [4.6, 16.9], [7.62, 7.68], 'engraving');
part('blade_wide_low', [2.0, 12.0, 7.72], [3.55, 15.1, 8.28], 'steel3');
part('blade_wide_high', [2.0, 16.9, 7.72], [3.55, 20.0, 8.28], 'steel3');
part('blade_wide_left', [2.0, 15.1, 7.72], [2.4, 16.9, 8.28], 'steel3');
part('blade_wide_right', [3.2, 15.1, 7.72], [3.55, 16.9, 8.28], 'steel3');
part('moon_ring_top', [2.3, 16.85, 7.66], [3.3, 17.15, 8.34], 'bronze');
part('moon_ring_low', [2.3, 14.85, 7.66], [3.3, 15.15, 8.34], 'bronze');
part('moon_ring_left', [2.3, 15.15, 7.66], [2.55, 16.85, 8.34], 'bronze');
part('moon_ring_right', [3.05, 15.15, 7.66], [3.3, 16.85, 8.34], 'bronze');
// The edge, an arc: furthest out in the middle, curving back at both ends into the horns (mirrored about y 16, the
// blade's middle). A thin bright bevel on the outside, a band of bright steel behind it.
const MID = 16;
const arc = (name, [x0, y0], [x1, y1], [z0, z1], mat) => {
  part(name + '_up', [x0, y0, z0], [x1, y1, z1], mat);
  if (y0 > MID) part(name + '_low', [x0, 2 * MID - y1, z0], [x1, 2 * MID - y0, z1], mat);
};
part('bevel_mid', [0.6, 13.6, 7.84], [1.2, 18.4, 8.16], 'edge');
arc('bevel_1', [0.8, 18.4], [1.4, 20.0], [7.84, 8.16], 'edge');
arc('bevel_2', [1.1, 20.0], [1.7, 21.0], [7.84, 8.16], 'edge');
arc('bevel_3', [1.5, 21.0], [2.2, 21.8], [7.84, 8.16], 'edge');
part('band_mid', [1.2, 13.0, 7.77], [2.0, 19.0, 8.23], 'steel4');
arc('band_1', [1.4, 19.0], [2.0, 20.6], [7.77, 8.23], 'steel4');
arc('band_2', [1.7, 20.6], [2.1, 21.2], [7.77, 8.23], 'steel4');
// Webs of steel joining the horns to the blade, so the crescent is one piece, thinner toward its points.
arc('horn_web', [2.0, 20.0], [3.3, 21.6], [7.74, 8.26], 'steel3');
arc('horn_web_inner', [3.3, 20.0], [3.9, 21.0], [7.76, 8.24], 'steel3');
arc('horn_web_tip', [3.3, 21.0], [3.6, 21.6], [7.78, 8.22], 'steel4');
arc('horn', [2.0, 21.6], [3.4, 22.4], [7.8, 8.2], 'edge');
arc('horn_curl', [3.2, 22.2], [4.0, 22.9], [7.83, 8.17], 'edge');
arc('horn_point', [3.8, 22.6], [4.5, 23.2], [7.86, 8.14], 'edge');

// The back spike: a hooked beak on a bronze ring.
part('spike_ring', [9.2, 15.0, 7.6], [9.6, 17.35, 8.4], 'bronze');
part('spike_base', [9.6, 15.2, 7.7], [10.8, 17.15, 8.3], 'iron');
part('spike_mid', [10.8, 15.45, 7.75], [11.8, 16.9, 8.25], 'iron');
part('spike_hook', [11.8, 15.3, 7.8], [12.7, 16.4, 8.2], 'steel4');
part('spike_hook_tip', [12.3, 14.6, 7.84], [12.95, 15.3, 8.16], 'edge');

// The tassel under the blade's neck: a jade bead, a knot, a bronze cap and three red strands.
part('tassel_bead', [5.95, 11.2, 7.6], [6.85, 12.1, 8.4], 'jade');
part('tassel_knot', [6.1, 10.4, 7.75], [6.7, 11.2, 8.25], 'tassel');
part('tassel_cord', [6.2, 8.2, 7.85], [6.6, 10.4, 8.15], 'tassel');
part('tassel_cap', [5.85, 7.7, 7.55], [6.95, 8.2, 8.45], 'bronze');
part('tassel_strand_a', [5.7, 5.6, 7.6], [6.2, 7.7, 8.1], 'tassel');
part('tassel_strand_b', [6.15, 5.2, 7.7], [6.65, 7.7, 8.3], 'tassel');
part('tassel_strand_c', [6.6, 5.6, 7.9], [7.1, 7.7, 8.4], 'tassel');

const ROTATION = { angle: -45, axis: 'z', origin: [8, 8, 8] };

/** Each face's texels (two per model pixel, at most a swatch), in texture pixels. */
function faces(p) {
  const [sx, sy] = SWATCH_AT[p.mat];
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  const fit = d => Math.max(1, Math.min(8, Math.round(d * 2)));
  const size = { north: [x1 - x0, y1 - y0], south: [x1 - x0, y1 - y0], east: [z1 - z0, y1 - y0], west: [z1 - z0, y1 - y0], up: [x1 - x0, z1 - z0], down: [x1 - x0, z1 - z0] };
  const out = {};
  for (const [face, [w, h]] of Object.entries(size)) out[face] = { uv: [sx, sy, sx + fit(w), sy + fit(h)], texture: '#0' };
  return out;
}
/** The same faces in the game model's 0-16 UV space. */
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, { ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE) }]));

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
  const order = ['tassel', 'jade', 'wood', 'leather', 'cord', 'iron', 'ironDark', 'bronzeDark', 'bronze', 'steel1', 'steel2', 'steel3', 'steel4', 'engraving', 'ridge', 'edge'];
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
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE],
  textures: { 0: 'murimblock:item/chief_great_axe_3d', particle: 'murimblock:item/chief_great_axe' },
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
  display: DISPLAY,
};
const flat = { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/chief_great_axe' } };
const itemModel = { loader: 'neoforge:separate_transforms', base: { parent: 'murimblock:item/chief_great_axe_3d' }, perspectives: { gui: flat, ground: flat, fixed: flat } };

function bbmodel() {
  const id = name => { const h = crypto.createHash('md5').update('chief_great_axe/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
  const elements = PARTS.map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin, faces: f, type: 'cube', uuid: id(p.name) };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: 'chief_great_axe', parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: 'chief_great_axe_3d.png', folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
      uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'chief_great_axe_3d.png'), png(TEX_SIZE, TEX_SIZE, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'chief_great_axe.png'), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'chief_great_axe_3d.json'), JSON.stringify(model3d, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'chief_great_axe.json'), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'chief_great_axe.bbmodel'), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the great axe:', PARTS.length, 'cubes');
}
