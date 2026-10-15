// The Orthodox Alliance soldiers' spear, the Azure Peak Spear (天峰枪): a willow-leaf head with a raised midrib on an
// azure enamel socket where the three peaks of the Alliance flag are cast, gold collars, a tassel of blue horsehair,
// a navy lacquered shaft with three gold bands, a gold butt spike (style chosen on scripts/concepts/alliance-spears.cjs).
//   models/item/alliance_spear_3d.json   the spear in the hand (a spear for Epic Fight);
//   models/item/alliance_spear.json      NeoForge's separate transforms: the inventory, the ground and frames keep the
//                                        flat icon textures/item/alliance_spear.png, drawn here from the model;
//   art/blockbench/alliance_spear.bbmodel  the Blockbench source.
// Built upright (the shaft along +y, the head's flat facing z), then every cube is turned 45 degrees about the model's
// centre onto the icon's diagonal, as the Bandit Dao and the great axe.
// Usage: node scripts/animation/alliance-spear-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'alliance_spear';

// ---------------------------------------------------------------- the 32 x 32 texture of the 3D model

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
/** 8 x 8 swatches, one per material, painted texel by texel: (u, v) -> [colour, light]. The Alliance's colours. */
const SWATCHES = {
  // Navy lacquer, a sheen running along the shaft.
  navy: (u, v) => [u % 4 === 1 ? '#22406e' : '#14284A', 0.96 + noise(u, v) * 0.06],
  // Gold with a line engraved round it.
  gold: (u, v) => [v % 3 === 1 ? '#b08a3a' : '#D9B25F', 0.95 + noise(u, v) * 0.08],
  // Azure enamel, a little lighter at its rim.
  azure: (u, v) => [(v === 0 || v === 7) ? '#5d8fd6' : '#2B5FA8', 0.96 + noise(u, v) * 0.06],
  snow: (u, v) => ['#EEF3F8', 1],
  ice: (u, v) => ['#9FB6CF', 0.97 + noise(u, v) * 0.05],
  steel: (u, v) => ['#9aa1a8', 0.93 + noise(u, v) * 0.1],
  steelDark: (u, v) => ['#6f767d', 0.94 + noise(u, v) * 0.08],
  edge: (u, v) => ['#f2f4f6', 0.97 + (v % 4 === 0 ? 0.04 : 0)],
  // Blue horsehair, darker toward its ends.
  tassel: (u, v) => [(u % 2) ? '#2B5FA8' : '#4a7cc4', 1 - v / 18],
  knot: (u, v) => [(u + v) % 3 === 0 ? '#14284A' : '#2B5FA8', 1],
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
const ICON = { navy: '#1c3560', gold: '#D9B25F', azure: '#2B5FA8', snow: '#EEF3F8', ice: '#9FB6CF', steel: '#a3aab1',
  steelDark: '#6f767d', edge: '#f2f4f6', tassel: '#3b6db8', knot: '#14284A' };

// ---------------------------------------------------------------- the cubes (upright: the shaft along +y)

const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
/** A square section round the shaft's axis (x 8, z 8), w wide, from y0 to y1. */
const round = (name, y0, y1, w, mat) => part(name, [8 - w / 2, y0, 8 - w / 2], [8 + w / 2, y1, 8 + w / 2], mat);

// The butt: a gold cap and its spike.
round('butt_tip', -8.0, -7.4, 0.45, 'gold');
round('butt_cap', -7.4, -6.4, 1.15, 'gold');
// The shaft, three gold bands.
round('shaft', -6.4, 14.6, 1.0, 'navy');
for (const [i, y] of [-3.4, 3.4, 10.2].entries()) round('band_' + i, y, y + 0.4, 1.3, 'gold');
// The socket: gold collars, azure enamel, the three peaks of the flag on both faces (the middle one tallest, snow-capped).
round('collar_low', 14.6, 15.1, 1.5, 'gold');
round('socket', 15.1, 16.8, 1.3, 'azure');
for (const [side, z0, z1] of [['front', 7.3, 7.35], ['back', 8.65, 8.7]]) {
  part('peak_mid_' + side, [7.8, 15.3, z0], [8.2, 16.35, z1], 'snow');
  part('peak_left_' + side, [7.45, 15.3, z0], [7.8, 15.85, z1], 'ice');
  part('peak_right_' + side, [8.2, 15.3, z0], [8.55, 15.85, z1], 'ice');
}
round('collar_high', 16.8, 17.2, 1.4, 'gold');
// The tassel: a knot under the socket, eight strands of blue horsehair round the shaft.
round('tassel_knot', 14.0, 14.6, 1.4, 'knot');
[[-0.75, 0], [0.75, 0], [0, -0.75], [0, 0.75], [-0.5, -0.5], [0.5, 0.5], [0.5, -0.5], [-0.5, 0.5]].forEach(([dx, dz], i) => {
  const len = 2.4 + (i % 3) * 0.35;
  part('tassel_' + i, [8 + dx - 0.18, 14.0 - len, 8 + dz - 0.18], [8 + dx + 0.18, 14.0, 8 + dz + 0.18], 'tassel');
});
// The head: a willow leaf, row by row, a flat steel body, bright edges, a raised midrib, a fine point.
const WIDTHS = [0.9, 1.5, 1.9, 2.0, 1.9, 1.6, 1.25, 0.9, 0.5], ROW = 0.62, HEAD_Y = 17.2, T = 0.3;
WIDTHS.forEach((w, i) => {
  const y = HEAD_Y + i * ROW, e = Math.min(0.25, w / 3);
  part('leaf_' + i, [8 - w / 2 + e, y, 8 - T / 2], [8 + w / 2 - e, y + ROW, 8 + T / 2], 'steel');
  part('leaf_edge_l' + i, [8 - w / 2, y, 8 - T / 3], [8 - w / 2 + e, y + ROW, 8 + T / 3], 'edge');
  part('leaf_edge_r' + i, [8 + w / 2 - e, y, 8 - T / 3], [8 + w / 2, y + ROW, 8 + T / 3], 'edge');
});
const LEAF_TOP = HEAD_Y + WIDTHS.length * ROW;
part('midrib', [7.85, HEAD_Y, 7.75], [8.15, LEAF_TOP - ROW, 8.25], 'steelDark');
part('point', [7.88, LEAF_TOP, 7.9], [8.12, LEAF_TOP + 0.8, 8.1], 'edge');

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
  // Drawn broader than the model, as Minecraft's sprites are, or the spear is a thread at this size.
  const broad = ([x, y]) => [8 + (x - 8) * 2.2, y];
  const quads = PARTS.map(p => ({ mat: p.mat, pts: [[p.from[0], p.from[1]], [p.to[0], p.from[1]], [p.to[0], p.to[1]], [p.from[0], p.to[1]]].map(broad).map(turn) }));
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
  const order = ['tassel', 'knot', 'navy', 'gold', 'azure', 'ice', 'snow', 'steelDark', 'steel', 'edge'];
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
    if (near) out.set([20, 24, 34, 255], (py * 16 + px) * 4);
  }
  return out;
}

// ---------------------------------------------------------------- the game models

/** In the hand: held at the middle of the shaft and tilted 35 degrees, as Epic Fight holds its own spear (its spear
 *  animations are made for that); 2.6 blocks long. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  thirdperson_lefthand: { rotation: [0, 90, -35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = `Murimblock - scripts/animation/alliance-spear-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
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
  console.log('wrote the Azure Peak Spear:', PARTS.length, 'cubes');
}
