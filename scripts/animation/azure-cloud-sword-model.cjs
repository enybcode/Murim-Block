// The Azure Cloud (蒼雲劍), the sword of the Azure Cloud Sect's sensei, in 3D: a jian, the straight double-edged sword of
// the Chinese masters, in shades of blue. A Blockbench project in the Java item model format
// (art/blockbench/azure_cloud_sword.bbmodel) and the game files it gives, under assets/murimblock (texture
// textures/item/azure_cloud_sword_3d.png, 32 x 32):
//   models/item/azure_cloud_sword_body.json    the sword without its tassel;
//   models/item/azure_cloud_sword_tassel.json  the tassel alone, hanging straight down from the model's centre (8, 8, 8);
//   models/item/azure_cloud_sword_held.json    the hand's transforms only (a renderer draws the body, then the tassel
//                                               turned about the pommel where its pendulum has swung it, as the dao's);
//   models/item/azure_cloud_sword.json         NeoForge's separate transforms: the flat icon in the inventory, on the
//                                               ground and in frames (textures/item/azure_cloud_sword.png, drawn from
//                                               the model below).
// As the Bandit Dao: built upright, then every cube turned 45 degrees about the model's centre to lie on the icon's
// diagonal, so the vanilla swords' hand transforms put it in the hand; the tassel's cord hangs from the bottom of the
// pommel's ring, POMMEL_END.
// A master's sword, fine and kept bright, in relief:
//   blade:  straight and double-edged, tapering a little toward the point, in strips across: a bright bevel, a flat of
//           steel with an azure sheen, a raised central ridge, the other flat, the other bevel; a cloud engraved and
//           inlaid in blue on both flats at the root; the point in three steps; a silver collar at the root;
//   guard:  silver clouds: a bar with a blue enamel inlay, its ends curling up into cloud scrolls, a tongue toward the blade;
//   grip:   azure silk cord wound over dark blue between two silver rings;
//   pommel: a silver cloud (ruyi) head with a sapphire, and a ring under it;
//   tassel: a long azure tassel, as a jian's: a cord, a bead of white jade, a knot and three strands.
// Usage: node scripts/animation/azure-cloud-sword-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');

// ---------------------------------------------------------------- the 32 x 32 texture

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
/** A little noise that stays the same for a given texel. */
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
/** A cloud scroll, 8 x 8 (x the line of the inlay). */
const CLOUD = ['........', '..xxx...', '.x...x..', '.x.x.x..', '..xx.xx.', '.....x.x', '.xxxxx.x', '........'];
/**
 * Sixteen 8 x 8 swatches, one per material, each painted texel by texel: (u, v) inside the swatch -> [colour, light].
 * A face takes the corner of its swatch that matches its own size (two texels per model pixel).
 */
const SWATCHES = {
  // The blade: the raised ridge catching the light, the flats of steel with an azure sheen, the bright bevels.
  ridge: (u, v) => ['#dde8f1', 0.96 + noise(u, v) * 0.06],
  steel: (u, v) => [(u + v) % 7 === 0 ? '#a3bfdc' : '#8fafd3', 0.94 + noise(u, v) * 0.09],
  edge: (u, v) => ['#f4f8fb', 0.98 + (v % 4 === 0 ? 0.03 : 0)],
  // The flat at the root: a cloud scroll inlaid in blue.
  engrave: (u, v) => (CLOUD[v][u] === 'x' ? ['#2a63d8', 1] : ['#8fafd3', 0.94 + noise(u, v) * 0.09]),
  // Silver with an engraved wave; darker silver for rims and rings.
  silver: (u, v) => [((u + Math.floor(v / 2) * 2) % 6 === 0) ? '#8e9aa6' : '#cfd8e0', 0.96 + noise(u, v) * 0.06],
  silverDark: (u, v) => [(u + v) % 4 === 0 ? '#5f6b78' : '#7f8b98', 0.96 + noise(u, v) * 0.06],
  // Deep blue enamel with a lighter swirl.
  enamel: (u, v) => [CLOUD[v][u] === 'x' ? '#79b2ec' : '#2559ad', 1],
  // Azure silk cord wound slantwise over dark blue: a dark line between the turns.
  wrap: (u, v) => [((v + (u % 2)) % 4 === 3) ? '#1d3a72' : ((v + (u % 2)) % 4 === 1 ? '#4c92e0' : '#3a7cd0'), 1],
  // The sapphire, a highlight in its corner.
  gem: (u, v) => [u === 1 && v === 0 ? '#8fc0ff' : (u + v) % 3 === 0 ? '#163a8c' : '#1f4cae', 1],
  // The tassel, lighter toward its ends; the bead of white jade.
  tassel: (u, v) => [(u % 2) ? '#2f72c8' : '#4a90e2', 0.9 + (v / 40)],
  jade: (u, v) => [(u + v) % 3 === 0 ? '#b2d9cc' : '#d6efe6', 0.97 + noise(u, v) * 0.05],
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
/** A strip of the blade from x0 to x1 over y0..y1, `half` thick on either side of the middle (z 8). */
const strip = (name, x0, x1, y0, y1, half, mat) => part(name, [x0, y0, 8 - half], [x1, y1, 8 + half], mat);
/** Both sides of a symmetric part: from x0 to x1 on the right of the axis (x > 8) and its mirror on the left. */
const pair = (name, x0, x1, y0, y1, z0, z1, mat) => {
  part(name + '_r', [x0, y0, z0], [x1, y1, z1], mat);
  part(name + '_l', [16 - x1, y0, z0], [16 - x0, y1, z1], mat);
};

// The tassel, hanging from the model's centre (8, 8, 8): its own model, swung by the renderer. Long, as a jian's.
const TASSEL = [
  { name: 'tassel_cord', from: [7.8, 6.3, 7.8], to: [8.2, 8.0, 8.2], mat: 'tassel' },
  { name: 'tassel_bead', from: [7.4, 5.4, 7.4], to: [8.6, 6.3, 8.6], mat: 'jade' },
  { name: 'tassel_knot', from: [7.45, 4.7, 7.45], to: [8.55, 5.4, 8.55], mat: 'tassel' },
  { name: 'tassel_strand_a', from: [7.1, 1.6, 7.5], to: [7.75, 4.7, 8.15], mat: 'tassel' },
  { name: 'tassel_strand_b', from: [7.7, 1.1, 7.7], to: [8.3, 4.7, 8.3], mat: 'tassel' },
  { name: 'tassel_strand_c', from: [8.25, 1.6, 7.85], to: [8.9, 4.7, 8.5], mat: 'tassel' },
];
/** The bottom of the pommel's ring in the upright sword: where the cord hangs. */
const POMMEL_END = -3.6;

// The pommel: a silver cloud head, its two lobes, a sapphire through it; a ring under it for the tassel's cord.
part('pommel_core', [6.9, POMMEL_END + 0.35, 7.3], [9.1, POMMEL_END + 1.75, 8.7], 'silver');
pair('pommel_lobe', 9.1, 9.8, POMMEL_END + 0.6, POMMEL_END + 1.5, 7.45, 8.55, 'silver');
pair('pommel_curl', 9.3, 9.9, POMMEL_END + 1.5, POMMEL_END + 1.95, 7.5, 8.5, 'silverDark');
part('pommel_gem', [7.5, POMMEL_END + 0.65, 7.2], [8.5, POMMEL_END + 1.45, 8.8], 'gem');
part('ring_left', [7.45, POMMEL_END, 7.85], [7.7, POMMEL_END + 0.35, 8.15], 'silverDark');
part('ring_right', [8.3, POMMEL_END, 7.85], [8.55, POMMEL_END + 0.35, 8.15], 'silverDark');
part('ring_bottom', [7.45, POMMEL_END, 7.85], [8.55, POMMEL_END + 0.15, 8.15], 'silverDark');
// The grip between two silver rings: in three lengths, so the cord's turns keep their size.
const GRIP0 = POMMEL_END + 1.75, GRIP1 = 3.4;
[[GRIP0, 0.0], [0.0, 1.7], [1.7, GRIP1]].forEach(([y0, y1], i) => part('grip_' + i, [7.42, y0, 7.42], [8.58, y1, 8.58], 'wrap'));
part('grip_ring_low', [7.27, GRIP0, 7.27], [8.73, GRIP0 + 0.45, 8.73], 'silver');
part('grip_ring_high', [7.27, GRIP1 - 0.45, 7.27], [8.73, GRIP1, 8.73], 'silver');
// The guard: a silver bar inlaid with blue enamel, its ends curling up into cloud scrolls, a tongue toward the blade.
part('guard_bar', [6.0, GRIP1, 7.2], [10.0, GRIP1 + 1.0, 8.8], 'silver');
part('guard_enamel', [6.6, GRIP1 + 0.2, 7.1], [9.4, GRIP1 + 0.8, 8.9], 'enamel');
pair('guard_wing', 10.0, 10.9, GRIP1 + 0.2, GRIP1 + 0.85, 7.35, 8.65, 'silver');
pair('guard_scroll', 10.7, 11.5, GRIP1 - 0.5, GRIP1 + 1.9, 7.4, 8.6, 'silver');
pair('guard_scroll_top', 10.0, 11.3, GRIP1 + 1.75, GRIP1 + 2.35, 7.45, 8.55, 'silver');
pair('guard_scroll_curl', 10.0, 10.5, GRIP1 + 1.35, GRIP1 + 1.75, 7.5, 8.5, 'silverDark');
pair('guard_scroll_foot', 10.2, 11.3, GRIP1 - 0.9, GRIP1 - 0.4, 7.45, 8.55, 'silverDark');
part('guard_tongue', [7.4, GRIP1 + 1.0, 7.35], [8.6, GRIP1 + 1.6, 8.65], 'silver');
part('blade_collar', [6.95, GRIP1 + 1.6, 7.55], [9.05, GRIP1 + 2.1, 8.45], 'silverDark');

// The blade, segment by segment toward the point: straight, double-edged, tapering a little.
const BLADE0 = GRIP1 + 2.1, SEG = 2.6;
const HALF_WIDTHS = [1.3, 1.27, 1.22, 1.16, 1.08];
HALF_WIDTHS.forEach((hw, i) => {
  const y0 = BLADE0 + i * SEG, y1 = y0 + SEG, flat = 'steel';
  strip('bevel_l_' + i, 8 - hw, 8 - hw + 0.35, y0, y1, 0.14, 'edge');
  strip('flat_l_' + i, 8 - hw + 0.35, 7.7, y0, y1, 0.27, flat);
  strip('ridge_' + i, 7.7, 8.3, y0, y1, 0.38, 'ridge');
  strip('flat_r_' + i, 8.3, 8 + hw - 0.35, y0, y1, 0.27, flat);
  strip('bevel_r_' + i, 8 + hw - 0.35, 8 + hw, y0, y1, 0.14, 'edge');
});
part('inlay', [8 - HALF_WIDTHS[0] + 0.4, BLADE0 + 0.4, 7.69], [8 + HALF_WIDTHS[0] - 0.4, BLADE0 + 3.6, 8.31], 'engrave');
// The point, in three steps: the flats narrowing between the bevels, then the bright tip.
const P0 = BLADE0 + HALF_WIDTHS.length * SEG;
[[0.88, 0.9], [0.56, 0.7]].forEach(([hw, len], i) => {
  const y0 = P0 + (i === 0 ? 0 : 0.9), y1 = y0 + len;
  strip('point_bevel_l_' + i, 8 - hw, 8 - hw + 0.3, y0, y1, 0.13, 'edge');
  strip('point_' + i, 8 - hw + 0.3, 8 + hw - 0.3, y0, y1, 0.3 - i * 0.06, i === 0 ? 'ridge' : 'steel');
  strip('point_bevel_r_' + i, 8 + hw - 0.3, 8 + hw, y0, y1, 0.13, 'edge');
});
strip('point_tip', 7.75, 8.25, P0 + 1.6, P0 + 2.1, 0.12, 'edge');

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
/** The same faces in the game model's 0-16 UV space. */
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, { ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE) }]));

// ---------------------------------------------------------------- the icon: the model seen from the front, fitted to 16 x 16

/** The colour of each material on the icon. */
const ICON = { ridge: '#dde8f1', steel: '#8fafd3', edge: '#f4f8fb', engrave: '#5f93d6', silver: '#cfd8e0', silverDark: '#7f8b98',
  enamel: '#2559ad', wrap: '#3474c4', gem: '#2353b8', tassel: '#3f86da', jade: '#d6efe6' };

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
  // Drawn broader than the model, as Minecraft's sprites are, or the blade is a thread at this size; each part by its
  // own amount, so the wide guard does not swallow the icon: the blade twice, the grip two and a half times, the
  // pommel and the tassel a little, the guard hardly.
  const WIDEN = { grip: 2.2, pommel: 1.6, ring: 1.6, tassel: 1.6, guard: 1.45, blade: 1.3 };
  const factor = p => { for (const [k, w] of Object.entries(WIDEN)) if (p.name.startsWith(k)) return w; return 1.55; };
  const broad = p => ([x, y]) => [8 + (x - 8) * factor(p), y];
  // The tassel under the pommel, shortened so the blade keeps the room.
  const hang = y => POMMEL_END - (8 - y) * 0.3;
  const all = [...TASSEL.map(p => ({ ...p, from: [p.from[0], hang(p.from[1]), p.from[2]], to: [p.to[0], hang(p.to[1]), p.to[2]] })), ...PARTS];
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
  // Later materials are drawn over earlier ones.
  const order = ['tassel', 'jade', 'wrap', 'silverDark', 'silver', 'gem', 'enamel', 'steel', 'engrave', 'ridge', 'edge'];
  for (let py = 1; py < 15; py++) for (let px = 1; px < 15; px++) {
    const x = x0 + (px - 1 + 0.5) / 14 * span, y = y0 + (14 - py + 0.5) / 14 * span;
    let hit = null;
    for (const mat of order) for (const q of quads) if (q.mat === mat && inside(q.pts, x, y)) hit = mat;
    if (hit) img.set([...hex(ICON[hit]), 255], (py * 16 + px) * 4);
  }
  // A dark blue outline round the shape, as Minecraft's items have.
  const out = new Uint8Array(img);
  for (let py = 0; py < 16; py++) for (let px = 0; px < 16; px++) {
    if (img[(py * 16 + px) * 4 + 3]) continue;
    const near = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => {
      const nx = px + dx, ny = py + dy;
      return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3];
    });
    if (near) out.set([16, 26, 52, 255], (py * 16 + px) * 4);
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

const CREDIT = 'Murimblock - scripts/animation/azure-cloud-sword-model.cjs (Blockbench source: art/blockbench/azure_cloud_sword.bbmodel)';
const TEXTURES = { 0: 'murimblock:item/azure_cloud_sword_3d', particle: 'murimblock:item/azure_cloud_sword' };
const bodyModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
};
const tasselModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: TASSEL.map(p => ({ name: p.name, from: p.from, to: p.to, faces: gameFaces(p) })),
};
const heldModel = { credit: CREDIT, parent: 'builtin/entity', textures: { particle: 'murimblock:item/azure_cloud_sword' }, display: DISPLAY };
const itemModel = {
  loader: 'neoforge:separate_transforms',
  base: { parent: 'murimblock:item/azure_cloud_sword_held' },
  perspectives: {
    gui: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/azure_cloud_sword' } },
    ground: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/azure_cloud_sword' } },
    fixed: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/azure_cloud_sword' } },
  },
};

// ---------------------------------------------------------------- the Blockbench project (Java item model format)

const id = name => { const h = crypto.createHash('md5').update('azure_cloud_sword/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
/** The whole sword with its tassel at rest along the grip's line; turned onto the diagonal unless `upright`. */
function elements(upright = false) {
  const drop = 8 - POMMEL_END;
  const hanging = TASSEL.map(p => ({ ...p, from: [p.from[0], p.from[1] - drop, p.from[2]], to: [p.to[0], p.to[1] - drop, p.to[2]] }));
  return [...hanging, ...PARTS].map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, upright ? 0 : ROTATION.angle], origin: ROTATION.origin,
      faces: f, type: 'cube', uuid: id(p.name) };
  });
}
const texture = () => ({ path: '', name: 'azure_cloud_sword_3d.png', folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
  uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
  render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
  visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, TEX).toString('base64') });
function bbmodel() {
  const els = elements();
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: 'azure_cloud_sword', parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements: els, outliner: els.map(e => e.uuid), textures: [texture()], display: DISPLAY,
  };
}

module.exports = { PARTS, TASSEL, POMMEL_END, GRIP1, TEX_SIZE, SWATCH_AT, faces, elements, texture, icon, bbmodel };

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'azure_cloud_sword_3d.png'), png(TEX_SIZE, TEX_SIZE, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'azure_cloud_sword.png'), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'azure_cloud_sword_body.json'), JSON.stringify(bodyModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'azure_cloud_sword_tassel.json'), JSON.stringify(tasselModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'azure_cloud_sword_held.json'), JSON.stringify(heldModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'azure_cloud_sword.json'), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'azure_cloud_sword.bbmodel'), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the Azure Cloud sword:', PARTS.length, 'cubes and a tassel of', TASSEL.length);
}
