// The weapon of the Purple Heaven Thunder Saber art (docs/combat/styles/purple-heaven-thunder-saber.md): the Purple
// Heaven Zhanmadao (紫霄斩马刀), a horse-cutting saber held in both hands. A long single-edged blade that widens toward
// a clipped point, dark storm steel with a bright edge and a gold lightning inlaid along its fuller; a round bronze guard
// cast with the square spirals of the thunder pattern (雷纹); a long grip bound in violet cord; a drum-shaped bronze
// pommel and a violet tassel with a gold knot.
//   models/item/purple_heaven_zhanmadao_3d.json  the saber in the hand (Epic Fight's greatsword transforms);
//   models/item/purple_heaven_zhanmadao.json     NeoForge's separate transforms: the inventory, the ground and frames
//                                                keep the flat icon textures/item/purple_heaven_zhanmadao.png;
//   art/blockbench/purple_heaven_zhanmadao.bbmodel  the Blockbench source.
// Built upright (the grip along +y, the edge toward -x), then every cube is turned 45 degrees onto the icon's diagonal,
// as the Bandit Chief's axe: in the hand its edge faces down and leads the greatsword's cuts.
// Usage: node scripts/animation/purple-heaven-zhanmadao-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'purple_heaven_zhanmadao';

// ---------------------------------------------------------------- the 32 x 32 texture of the 3D model

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
/** The thunder pattern: square spirals, one per 4 x 4 cell. */
const leiwen = (u, v) => {
  const x = u % 4, y = v % 4;
  return (y === 0 && x < 3) || (x === 2 && y < 3) || (y === 2 && x > 0 && x < 3) || (x === 0 && y > 0);
};
const SWATCHES = {
  // Storm steel: dark blue-grey, hammered.
  steel: (u, v) => ['#4a5260', 0.92 + noise(u, v) * 0.12],
  steelLight: (u, v) => ['#6c7584', 0.93 + noise(u, v) * 0.1],
  spine: (u, v) => ['#2c313a', 0.94 + noise(u, v) * 0.08],
  edge: (u, v) => ['#e8eef5', 0.96 + (v % 4 === 0 ? 0.05 : 0)],
  // The inlaid lightning: gold, a violet glint.
  inlay: (u, v) => [(u + v) % 5 === 0 ? '#d9b8ff' : '#f2c94c', 1],
  // Bronze cast with the thunder pattern.
  bronze: (u, v) => [leiwen(u, v) ? '#7a5a22' : '#c49a3e', 0.95 + noise(u, v) * 0.06],
  bronzeDark: (u, v) => ['#7a5a22', 0.95 + noise(u, v) * 0.08],
  // Violet cord wound slantwise over black ray skin.
  cord: (u, v) => [((u + v) % 4 === 0) ? '#1c1226' : ((u - v + 8) % 4 === 0 ? '#9a6ad6' : '#5e3a92'), 1],
  tassel: (u, v) => [(u % 2) ? '#6a3fa6' : '#8b5cd0', 1 - v / 18],
  knot: (u, v) => [(u + v) % 3 === 0 ? '#a07a2a' : '#e0b64e', 1],
  // The violet stone set in the guard.
  gem: (u, v) => [(u + v) % 3 === 0 ? '#e6d2ff' : '#9a5cf0', 1],
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
const ICON = { steel: '#525a68', steelLight: '#77808f', spine: '#2c313a', edge: '#f2f6fa', inlay: '#f2c94c', bronze: '#c49a3e',
  bronzeDark: '#7a5a22', cord: '#6a43a2', tassel: '#8b5cd0', knot: '#e0b64e', gem: '#b58cff' };

// ---------------------------------------------------------------- the cubes (upright: the grip along +y, the edge toward -x)

const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
const round = (name, y0, y1, w, mat, d = w) => part(name, [8 - w / 2, y0, 8 - d / 2], [8 + w / 2, y1, 8 + d / 2], mat);

// The pommel: a little bronze drum, its skin a disc of gold.
round('pommel_skin', -7.6, -7.3, 1.1, 'knot');
round('pommel_drum', -7.3, -6.0, 1.6, 'bronze');
round('pommel_rim', -6.4, -6.1, 1.8, 'bronzeDark');
// The tassel: a gold knot under the pommel, eight violet strands.
round('tassel_knot', -8.2, -7.6, 0.7, 'knot');
[[-0.4, 0], [0.4, 0], [0, -0.4], [0, 0.4], [-0.3, -0.3], [0.3, 0.3], [0.3, -0.3], [-0.3, 0.3]].forEach(([dx, dz], i) => {
  const len = 2.2 + (i % 3) * 0.5;
  part('tassel_' + i, [8 + dx - 0.18, -8.2 - len, 8 + dz - 0.18], [8 + dx + 0.18, -8.2, 8 + dz + 0.18], 'tassel');
});
// The grip: long, for two hands, bound in violet cord, three bronze ferrules.
round('grip', -6.0, 4.0, 1.05, 'cord');
for (const [i, y] of [-6.0, -1.3, 3.5].entries()) round('ferrule_' + i, y, y + 0.5, 1.25, 'bronzeDark');
// The guard: a round bronze disc, the thunder pattern on its faces, a violet stone on each face.
round('guard_wide', 4.0, 4.6, 3.8, 'bronze', 2.0);
round('guard_deep', 4.0, 4.6, 2.6, 'bronze', 3.0);
round('guard_core', 4.0, 4.6, 3.3, 'bronze', 2.6);
part('gem_front', [7.6, 4.05, 6.42], [8.4, 4.55, 6.52], 'gem');
part('gem_back', [7.6, 4.05, 9.48], [8.4, 4.55, 9.58], 'gem');
// The collar at the root of the blade.
part('collar', [6.95, 4.6, 7.55], [9.55, 5.4, 8.45], 'bronzeDark');

// The blade: single-edged, its spine straight along +x, the edge on -x widening toward the tip, then clipped.
const SPINE_X = 9.4, BASE_Y = 5.4, ROW = 0.75, T = 0.38;
const WIDTHS = [2.3, 2.35, 2.4, 2.45, 2.5, 2.55, 2.6, 2.65, 2.7, 2.75, 2.8, 2.85, 2.9, 2.95, 3.0, 3.05, 3.1, 3.15, 3.2, 3.2, 3.2, 3.2];
// The clip: over the last rows the spine falls away toward the edge, so the point sits on the edge's line.
const CLIP = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.35, 0.8, 1.3, 1.85, 2.4, 2.9];
WIDTHS.forEach((w, i) => {
  const y = BASE_Y + i * ROW, xs = SPINE_X - CLIP[i], xe = SPINE_X - w;
  const e = Math.min(0.42, w * 0.25), s = i < 16 ? 0.22 : 0;
  part('blade_' + i, [xe + e, y, 8 - T / 2], [xs - s, y + ROW, 8 + T / 2], i % 2 ? 'steel' : 'steelLight');
  part('edge_' + i, [xe, y, 8 - T / 4], [xe + e, y + ROW, 8 + T / 4], 'edge');
  if (s) part('spine_' + i, [xs - s, y, 8 - T / 2 - 0.05], [xs, y + ROW, 8 + T / 2 + 0.05], 'spine');
});
// The lightning inlaid along the fuller, on both faces: a zigzag of gold from the collar to two thirds of the blade.
const ZIG = [0, 0.35, 0.6, 0.25, -0.1, 0.25, 0.6, 0.3, 0, 0.35, 0.65, 0.3];
ZIG.forEach((dx, i) => {
  const y = BASE_Y + 0.3 + i * ROW * 1.15, x = SPINE_X - 0.95 - dx;
  for (const [side, z0, z1] of [['front', 8 - T / 2 - 0.04, 8 - T / 2], ['back', 8 + T / 2, 8 + T / 2 + 0.04]]) {
    part(`bolt_${side}_${i}`, [x - 0.28, y, z0], [x, y + ROW * 1.2, z1], 'inlay');
  }
});

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
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, { ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE) }]));

// ---------------------------------------------------------------- the icon: the model seen from the front, fitted to 16 x 16

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
  // Drawn a little broader than the model, as Minecraft's sprites are.
  const broad = ([x, y]) => [8 + (x - 8) * 1.5, y];
  const quads = PARTS.filter(p => !p.name.startsWith('bolt_back')).map(p => ({ mat: p.mat, pts: [[p.from[0], p.from[1]], [p.to[0], p.from[1]], [p.to[0], p.to[1]], [p.from[0], p.to[1]]].map(broad).map(turn) }));
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
  const order = ['tassel', 'knot', 'cord', 'bronzeDark', 'bronze', 'gem', 'spine', 'steel', 'steelLight', 'edge', 'inlay'];
  for (let py = 1; py < 15; py++) for (let px = 1; px < 15; px++) {
    const x = x0 + (px - 1 + 0.5) / 14 * span, y = y0 + (14 - py + 0.5) / 14 * span;
    let hit = null;
    for (const mat of order) for (const q of quads) if (q.mat === mat && inside(q.pts, x, y)) hit = mat;
    if (hit) img.set([...hex(ICON[hit]), 255], (py * 16 + px) * 4);
  }
  const out = new Uint8Array(img);
  for (let py = 0; py < 16; py++) for (let px = 0; px < 16; px++) {
    if (img[(py * 16 + px) * 4 + 3]) continue;
    const near = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => {
      const nx = px + dx, ny = py + dy;
      return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3];
    });
    if (near) out.set([20, 16, 34, 255], (py * 16 + px) * 4);
  }
  return out;
}

// ---------------------------------------------------------------- the game models

/** In the hand: Epic Fight's greatsword transforms, as the Bandit Chief's axe; about 2.1 blocks long. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 45], translation: [0, 7.5, 0], scale: [1.25, 1.25, 1.25] },
  thirdperson_lefthand: { rotation: [0, 90, -45], translation: [0, 7.5, 0], scale: [1.25, 1.25, 1.25] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.05, 1.05, 1.05] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.05, 1.05, 1.05] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = `Murimblock - scripts/animation/purple-heaven-zhanmadao-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
const model3d = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE],
  textures: { 0: `murimblock:item/${NAME}_3d`, particle: `murimblock:item/${NAME}` },
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
  display: DISPLAY,
};
const flat = { parent: 'minecraft:item/handheld', textures: { layer0: `murimblock:item/${NAME}` } };
const itemModel = { loader: 'neoforge:separate_transforms', base: { parent: `murimblock:item/${NAME}_3d` }, perspectives: { gui: flat, ground: flat, fixed: flat } };

function bbmodel() {
  const id = name => { const h = crypto.createHash('md5').update(NAME + '/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
  const elements = PARTS.map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin, faces: f, type: 'cube', uuid: id(p.name) };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: NAME, parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: `${NAME}_3d.png`, folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
      uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}_3d.png`), png(TEX_SIZE, TEX_SIZE, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}.png`), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_3d.json`), JSON.stringify(model3d, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}.json`), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${NAME}.bbmodel`), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the Purple Heaven Zhanmadao:', PARTS.length, 'cubes');
}
module.exports = { PARTS, TEX, TEX_SIZE, SWATCH_AT, ROTATION, faces };
