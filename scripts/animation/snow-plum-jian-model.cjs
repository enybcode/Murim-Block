// The Snow Plum Jian (雪梅剑), the Orthodox Alliance disciples' sword (style 6 of scripts/concepts/alliance-swords.cjs):
// a long slender jian in the Alliance's colours. Built as the Azure Cloud sword (azure-cloud-sword-model.cjs), whose
// renderer swings the tassel (client/TasselSwordRenderer):
//   models/item/snow_plum_jian_body.json    the sword without its tassel;
//   models/item/snow_plum_jian_tassel.json  the tassel alone, hanging straight down from the model's centre (8, 8, 8);
//   models/item/snow_plum_jian_held.json    the hand's transforms only (the vanilla sword's);
//   models/item/snow_plum_jian.json         NeoForge's separate transforms: the flat icon (textures/item/snow_plum_jian.png)
//                                           in the inventory, on the ground and in frames;
//   art/blockbench/snow_plum_jian.bbmodel   the Blockbench project; texture textures/item/snow_plum_jian_3d.png (32 x 32).
// Built upright (the blade up +y, flat in x, thin in z), then turned 45 degrees onto the icon's diagonal, so the vanilla
// swords' hand transforms put it in the hand with its two edges up and down, leading the cuts.
//   blade:  long and slender, double-edged, in strips across: a bright bevel, a steel flat, a pale ice-blue ridge, the
//           other flat and bevel; the three peaks of the Alliance inlaid at its root and three white plum blossoms
//           climbing above them on both flats (the plum that flowers under the snow of the peaks); a point in steps;
//   guard:  small, gold, its two wings bent up toward the blade, as a jian's are, a tongue under the blade;
//   grip:   azure cord wound over navy between two gold rings; a gold pommel cap;
//   tassel: long blue horsehair in two strands, a gold bead and a knot, on a cord from the pommel.
// Usage: node scripts/animation/snow-plum-jian-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'snow_plum_jian';

// ---------------------------------------------------------------- the 32 x 32 texture

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
/** 8 x 8 swatches, one per material, painted texel by texel: (u, v) -> [colour, light]. The Alliance's colours. */
const SWATCHES = {
  ridge: (u, v) => ['#c9d8e8', 0.97 + noise(u, v) * 0.05],
  steel: (u, v) => [(u + v) % 7 === 0 ? '#b4bcc4' : '#a3abb3', 0.94 + noise(u, v) * 0.08],
  edge: (u, v) => ['#f2f5f8', 0.98 + (v % 4 === 0 ? 0.03 : 0)],
  gold: (u, v) => [v % 3 === 1 ? '#b08a3a' : '#D9B25F', 0.95 + noise(u, v) * 0.08],
  goldDark: (u, v) => ['#9c7a32', 0.95 + noise(u, v) * 0.08],
  // Azure cord wound slantwise over navy: a dark line between the turns.
  wrap: (u, v) => [((v + (u % 2)) % 4 === 3) ? '#14284A' : ((v + (u % 2)) % 4 === 1 ? '#5d8fd6' : '#2B5FA8'), 1],
  snow: (u, v) => ['#EEF3F8', 1],
  ice: (u, v) => ['#9FB6CF', 0.98 + noise(u, v) * 0.04],
  plum: (u, v) => ['#fbfcfe', 1],
  // Blue horsehair, a little darker toward its ends.
  tassel: (u, v) => [(u % 2) ? '#2B5FA8' : '#4a7cc4', 1 - v / 24],
  knot: (u, v) => [(u + v) % 3 === 0 ? '#14284A' : '#2B5FA8', 1],
};
const SWATCH_AT = Object.fromEntries(Object.keys(SWATCHES).map((m, i) => [m, [(i % 4) * 8, Math.floor(i / 4) * 8]]));
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
for (const [m, paintIt] of Object.entries(SWATCHES)) {
  const [sx, sy] = SWATCH_AT[m];
  for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
    const [c, light] = paintIt(u, v);
    TEX.set([...shade(c, light), 255], ((sy + v) * TEX_SIZE + sx + u) * 4);
  }
}

// ---------------------------------------------------------------- the cubes (upright: the blade up +y, flat in x, thin in z)

const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
const strip = (name, x0, x1, y0, y1, half, mat) => part(name, [x0, y0, 8 - half], [x1, y1, 8 + half], mat);
const pair = (name, x0, x1, y0, y1, z0, z1, mat) => {
  part(name + '_r', [x0, y0, z0], [x1, y1, z1], mat);
  part(name + '_l', [16 - x1, y0, z0], [16 - x0, y1, z1], mat);
};
/** A flat piece on both faces of the blade, `at` from the middle (z 8), `t` thick. */
const faces2 = (name, x0, x1, y0, y1, at, mat, t = 0.05) => {
  part(name + '_front', [x0, y0, 8 - at - t], [x1, y1, 8 - at], mat);
  part(name + '_back', [x0, y0, 8 + at], [x1, y1, 8 + at + t], mat);
};

// The tassel, hanging from the model's centre (8, 8, 8): its own model, swung by the renderer. Long, in two strands.
const TASSEL = [
  { name: 'tassel_cord', from: [7.85, 6.1, 7.85], to: [8.15, 8.0, 8.15], mat: 'tassel' },
  { name: 'tassel_bead', from: [7.55, 5.35, 7.55], to: [8.45, 6.1, 8.45], mat: 'gold' },
  { name: 'tassel_knot', from: [7.6, 4.7, 7.6], to: [8.4, 5.35, 8.4], mat: 'knot' },
  { name: 'tassel_strand_a', from: [7.4, 0.9, 7.65], to: [7.95, 4.7, 8.2], mat: 'tassel' },
  { name: 'tassel_strand_b', from: [8.05, 0.5, 7.8], to: [8.6, 4.7, 8.35], mat: 'tassel' },
];
/** The bottom of the pommel in the upright sword: where the cord hangs. */
const POMMEL_END = -3.8;

// The pommel: a gold cap, a small foot for the cord.
part('pommel_foot', [7.65, POMMEL_END, 7.65], [8.35, POMMEL_END + 0.3, 8.35], 'goldDark');
part('pommel_cap', [7.3, POMMEL_END + 0.3, 7.35], [8.7, POMMEL_END + 1.25, 8.65], 'gold');
// The grip between two gold rings, in three lengths so the cord's turns keep their size.
const GRIP0 = POMMEL_END + 1.25, GRIP1 = 2.6;
[[GRIP0, -0.7], [-0.7, 0.95], [0.95, GRIP1]].forEach(([y0, y1], i) => part('grip_' + i, [7.5, y0, 7.5], [8.5, y1, 8.5], 'wrap'));
part('grip_ring_low', [7.38, GRIP0, 7.38], [8.62, GRIP0 + 0.3, 8.62], 'gold');
part('grip_ring_high', [7.38, GRIP1 - 0.3, 7.38], [8.62, GRIP1, 8.62], 'gold');
// The guard: small, its wings bent up toward the blade; a tongue under the blade.
part('guard_core', [7.0, GRIP1, 7.4], [9.0, GRIP1 + 0.7, 8.6], 'gold');
pair('guard_wing', 9.0, 9.7, GRIP1 + 0.2, GRIP1 + 0.9, 7.5, 8.5, 'gold');
pair('guard_wing_tip', 9.3, 9.8, GRIP1 + 0.9, GRIP1 + 1.4, 7.6, 8.4, 'gold');
part('guard_tongue', [7.45, GRIP1 + 0.7, 7.55], [8.55, GRIP1 + 1.2, 8.45], 'goldDark');

// The blade, segment by segment toward the point: long, slender, double-edged, tapering a little.
const BLADE0 = GRIP1 + 1.2, SEG = 2.6;
const HALF_WIDTHS = [0.85, 0.84, 0.82, 0.8, 0.77, 0.74, 0.7];
HALF_WIDTHS.forEach((hw, i) => {
  const y0 = BLADE0 + i * SEG, y1 = y0 + SEG;
  strip('bevel_l_' + i, 8 - hw, 8 - hw + 0.25, y0, y1, 0.1, 'edge');
  strip('flat_l_' + i, 8 - hw + 0.25, 7.85, y0, y1, 0.2, 'steel');
  strip('ridge_' + i, 7.85, 8.15, y0, y1, 0.28, 'ridge');
  strip('flat_r_' + i, 8.15, 8 + hw - 0.25, y0, y1, 0.2, 'steel');
  strip('bevel_r_' + i, 8 + hw - 0.25, 8 + hw, y0, y1, 0.1, 'edge');
});
// The point, in steps.
const P0 = BLADE0 + HALF_WIDTHS.length * SEG;
[[0.55, 0.75], [0.33, 0.6]].forEach(([hw, len], i) => {
  const y0 = P0 + (i === 0 ? 0 : 0.75), y1 = y0 + len;
  strip('point_bevel_l_' + i, 8 - hw, 8 - hw + 0.2, y0, y1, 0.09, 'edge');
  strip('point_' + i, 8 - hw + 0.2, 8 + hw - 0.2, y0, y1, 0.2 - i * 0.05, i === 0 ? 'ridge' : 'steel');
  strip('point_bevel_r_' + i, 8 + hw - 0.2, 8 + hw, y0, y1, 0.09, 'edge');
});
strip('point_tip', 7.85, 8.15, P0 + 1.35, P0 + 1.8, 0.08, 'edge');
// The three peaks of the Alliance inlaid at the root, on both faces: a pale ridge, three snowy tops, the middle tallest.
faces2('peaks_base', 7.4, 8.6, BLADE0 + 0.4, BLADE0 + 0.65, 0.28, 'ice');
faces2('peak_mid', 7.88, 8.12, BLADE0 + 0.65, BLADE0 + 1.3, 0.28, 'snow');
faces2('peak_left', 7.45, 7.7, BLADE0 + 0.65, BLADE0 + 0.95, 0.28, 'snow');
faces2('peak_right', 8.3, 8.55, BLADE0 + 0.65, BLADE0 + 0.95, 0.28, 'snow');
// Three plum blossoms climbing the flats above them: five white petals round a gold heart.
[[7.55, BLADE0 + 2.6], [8.45, BLADE0 + 4.3], [7.55, BLADE0 + 6.1]].forEach(([x, y], i) => {
  faces2('plum_h_' + i, x - 0.27, x + 0.27, y - 0.09, y + 0.09, 0.2, 'plum');
  faces2('plum_v_' + i, x - 0.09, x + 0.09, y - 0.27, y + 0.27, 0.2, 'plum');
  faces2('plum_heart_' + i, x - 0.06, x + 0.06, y - 0.06, y + 0.06, 0.25, 'gold', 0.02);
});

const ROTATION = { angle: -45, axis: 'z', origin: [8, 8, 8] };

/** Each face's texels (two per model pixel, at most a swatch), in texture pixels. */
function faces(p) {
  const [sxw, syw] = SWATCH_AT[p.mat];
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  const fit = d => Math.max(1, Math.min(8, Math.round(d * 2)));
  const size = { north: [x1 - x0, y1 - y0], south: [x1 - x0, y1 - y0], east: [z1 - z0, y1 - y0], west: [z1 - z0, y1 - y0], up: [x1 - x0, z1 - z0], down: [x1 - x0, z1 - z0] };
  const out = {};
  for (const [face, [w, h]] of Object.entries(size)) out[face] = { uv: [sxw, syw, sxw + fit(w), syw + fit(h)], texture: '#0' };
  return out;
}
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, { ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE) }]));

// ---------------------------------------------------------------- the icon: the model seen from the front, fitted to 16 x 16

const ICON = { ridge: '#c9d8e8', steel: '#a3abb3', edge: '#f2f5f8', gold: '#D9B25F', goldDark: '#9c7a32', wrap: '#2B5FA8',
  snow: '#EEF3F8', ice: '#9FB6CF', plum: '#fbfcfe', tassel: '#3b6db8', knot: '#14284A' };

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
  // Broader than the model, as Minecraft's sprites are, or the slender blade is a thread at this size.
  const WIDEN = { grip: 2.4, pommel: 1.8, tassel: 1.8, guard: 1.5, peak: 1, plum: 1 };
  const factor = p => { for (const [k, w] of Object.entries(WIDEN)) if (p.name.startsWith(k)) return w; return 1.9; };
  const broad = p => ([x, y]) => [8 + (x - 8) * factor(p), y];
  const hang = y => POMMEL_END - (8 - y) * 0.3;
  const all = [...TASSEL.map(p => ({ ...p, from: [p.from[0], hang(p.from[1]), p.from[2]], to: [p.to[0], hang(p.to[1]), p.to[2]] })),
    ...PARTS.filter(p => !p.name.startsWith('peak') && !p.name.startsWith('plum'))];
  const quads = all.map(p => ({ mat: p.mat, pts: [[p.from[0], p.from[1]], [p.to[0], p.from[1]], [p.to[0], p.to[1]], [p.from[0], p.to[1]]].map(broad(p)).map(turn) }));
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
  const order = ['tassel', 'knot', 'wrap', 'goldDark', 'gold', 'steel', 'ridge', 'edge'];
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
    if (near) out.set([20, 24, 40, 255], (py * 16 + px) * 4);
  }
  return out;
}

// ---------------------------------------------------------------- the game models

/** The vanilla handheld sword's transforms, so the sword sits in the hand as any sword does. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = `Murimblock - scripts/animation/snow-plum-jian-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
const TEXTURES = { 0: `murimblock:item/${NAME}_3d`, particle: `murimblock:item/${NAME}` };
const bodyModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
};
const tasselModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: TASSEL.map(p => ({ name: p.name, from: p.from, to: p.to, faces: gameFaces(p) })),
};
const heldModel = { credit: CREDIT, parent: 'builtin/entity', textures: { particle: `murimblock:item/${NAME}` }, display: DISPLAY };
const flat = { parent: 'minecraft:item/handheld', textures: { layer0: `murimblock:item/${NAME}` } };
const itemModel = { loader: 'neoforge:separate_transforms', base: { parent: `murimblock:item/${NAME}_held` }, perspectives: { gui: flat, ground: flat, fixed: flat } };

// ---------------------------------------------------------------- the Blockbench project (Java item model format)

const id = name => { const h = crypto.createHash('md5').update(NAME + '/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
function bbmodel() {
  const drop = 8 - POMMEL_END;
  const hanging = TASSEL.map(p => ({ ...p, from: [p.from[0], p.from[1] - drop, p.from[2]], to: [p.to[0], p.to[1] - drop, p.to[2]] }));
  const els = [...hanging, ...PARTS].map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin, faces: f, type: 'cube', uuid: id(p.name) };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: NAME, parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements: els, outliner: els.map(e => e.uuid),
    textures: [{ path: '', name: `${NAME}_3d.png`, folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
      uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

module.exports = { PARTS, TASSEL, POMMEL_END };

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}_3d.png`), png(TEX_SIZE, TEX_SIZE, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}.png`), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_body.json`), JSON.stringify(bodyModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_tassel.json`), JSON.stringify(tasselModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_held.json`), JSON.stringify(heldModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}.json`), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${NAME}.bbmodel`), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the Snow Plum Jian:', PARTS.length, 'cubes and a tassel of', TASSEL.length);
}
