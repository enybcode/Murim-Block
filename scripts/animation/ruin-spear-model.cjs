// The Spear of Ruin (灭世枪), the Overlord Spear Art's weapon (docs/combat/styles/overlord-spear.md), after the concept
// Enzo chose on 18/10/2026 ("ruin" in scripts/concepts/overlord-spears.cjs): a long, wide head of black steel split by
// a glowing seam, gold-white edges, three gold claws gripping it, a red mane, a black shaft cracked with veins of
// burning Qi. The embers are full-bright (NeoForge's per-face light data): they glow in the dark.
//   models/item/spear_of_ruin_3d.json   the spear in the hand (a spear for Epic Fight);
//   models/item/spear_of_ruin.json      NeoForge's separate transforms: inventory, ground and frames keep the flat icon;
//   art/blockbench/spear_of_ruin.bbmodel  the Blockbench source.
// The concept's parts, scaled into the item's space, then the crimson spear's pipeline (crimson-dragon-spear-model.cjs).
// Usage: node scripts/animation/ruin-spear-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');
const { builder } = require('../concepts/chief-axes.cjs');
const { STYLES } = require('../concepts/overlord-spears.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'spear_of_ruin';

// ---------------------------------------------------------------- the 32 x 32 texture of the 3D model

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
const SWATCHES = {
  // Black iron of the shaft, dull, a faint grain.
  shaft: (u, v) => [u % 4 === 1 ? '#34343a' : '#26262b', 0.95 + noise(u, v) * 0.08],
  // Black steel of the head, a cold sheen.
  head: (u, v) => [(u + v) % 5 === 0 ? '#4a4c55' : '#33343b', 0.97 + noise(u, v) * 0.05],
  gold: (u, v) => [v % 3 === 1 ? '#a8822e' : '#D9A83F', 0.95 + noise(u, v) * 0.08],
  edge: (u, v) => ['#f4efe2', 0.97 + (v % 4 === 0 ? 0.04 : 0)],
  // Burning Qi: white-hot at the heart, orange, red at the rims.
  ember: (u, v) => [u % 4 === 1 || u % 4 === 2 ? '#ffd27a' : '#ff5a1e', 1],
  blood: (u, v) => [(u + v) % 3 === 0 ? '#6a0e0e' : '#9e1a1a', 1],
  hair: (u, v) => [(u % 2) ? '#a8161a' : '#c82428', 1 - v / 18],
};
const SWATCH_AT = Object.fromEntries(Object.keys(SWATCHES).map((m, i) => [m, [(i % 4) * 8, Math.floor(i / 4) * 8]]));
for (const [m, paintIt] of Object.entries(SWATCHES)) {
  const [sx, sy] = SWATCH_AT[m];
  for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
    const [c, light] = paintIt(u, v);
    TEX.set([...shade(c, light), 255], ((sy + v) * TEX_SIZE + sx + u) * 4);
  }
}
const ICON = { shaft: '#2e2e34', head: '#3c3e46', gold: '#D9A83F', edge: '#f8f4ea', ember: '#ff6a24', blood: '#9e1a1a', hair: '#c82428' };

// ---------------------------------------------------------------- the cubes: the concept, scaled into the item

const S = 0.55, DY = -6.24;
const b = builder();
STYLES.ruin.build(b);
/** The concept's materials: the iron of the head (bright shades) apart from the shaft's. */
const matOf = p => p.mat === 'iron' ? (p.from[1] >= 40.5 ? 'head' : 'shaft') : p.mat;
const PARTS = b.parts.map((p, i) => ({
  name: `${matOf(p)}_${i}`, mat: matOf(p),
  from: [8 + p.from[0] * S, p.from[1] * S + DY, 8 + p.from[2] * S],
  to: [8 + p.to[0] * S, p.to[1] * S + DY, 8 + p.to[2] * S],
}));

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
/** The game's faces: the embers full-bright, so they glow in the dark. */
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, {
  ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE),
  ...(p.mat === 'ember' ? { neoforge_data: { block_light: 15, sky_light: 15, ambient_occlusion: false } } : {}),
}]));

// ---------------------------------------------------------------- the icon: the model seen from the front, fitted to 16 x 16

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
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
  const order = ['hair', 'blood', 'shaft', 'gold', 'head', 'edge', 'ember'];
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
    if (near) out.set([14, 12, 16, 255], (py * 16 + px) * 4);
  }
  return out;
}


/** The icon, drawn in pixel art on the diagonal (the model seen at this size is a black thread): a gold butt, a black
 *  shaft with two embers, a red mane, a gold collar, a broad black head with bright edges and a burning seam. */
function iconDrawn() {
  const img = new Uint8Array(16 * 16 * 4);
  const C = { K: '#0e0c10', S: '#3a3a42', s: '#55565f', E: '#ff6a24', Y: '#ffd27a', G: '#D9A83F', g: '#a8822e', R: '#c82428', r: '#7a1418', H: '#2c2d33', h: '#4a4c55', W: '#f4efe2' };
  const at = (x, y) => {
    const u = x + 0.5, v = 15.5 - y, along = (u + v) / 2, perp = (u - v) / 2;
    if (along >= 8.9 && along <= 15.6) {
      const w = 2.3 * Math.sqrt(Math.max(0, 1 - Math.pow((along - 10.6) / (along < 10.6 ? 1.8 : 5.0), 2)));
      if (Math.abs(perp) <= w + 0.05) {
        if (Math.abs(perp) < 0.36) return along > 12.5 ? 'Y' : 'E';
        if (Math.abs(perp) > w - 0.55) return 'W';
        return perp < 0 ? 'h' : 'H';
      }
    }
    if (along >= 8.1 && along < 8.9 && Math.abs(perp) <= 1.3) return along < 8.5 ? 'g' : 'G';
    if (along >= 6.3 && along < 8.3 && perp > 0.3 && perp <= 2.3) return (x + y) % 2 ? 'R' : 'r';
    if (along >= 1.2 && along < 8.1 && Math.abs(perp) <= 0.55) return [3.0, 5.4].some(a => Math.abs(along - a) < 0.45) ? 'E' : perp < 0 ? 's' : 'S';
    if (along >= 0.2 && along < 1.2 && Math.abs(perp) <= 0.75) return 'G';
    return null;
  };
  const grid = [];
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) grid[y * 16 + x] = at(x, y);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let c = grid[y * 16 + x];
    if (!c && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && grid[ny * 16 + nx]; })) c = 'K';
    if (c) img.set([...hex(C[c]), 255], (y * 16 + x) * 4);
  }
  return img;
}

// ---------------------------------------------------------------- the game models

/** In the hand: as the Crimson Dragon Spear, held at the middle of the shaft and tilted 35 degrees for Epic Fight's spear. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  thirdperson_lefthand: { rotation: [0, 90, -35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = `Murimblock - scripts/animation/ruin-spear-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
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
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}.png`), png(16, 16, iconDrawn()));
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_3d.json`), JSON.stringify(model3d, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}.json`), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${NAME}.bbmodel`), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the Spear of Ruin:', PARTS.length, 'cubes');
}
module.exports = { PARTS };
