// The Crescent Moon Halberd (月牙戟), the Crescent Moon Halberd Art's weapon (docs/combat/styles/crescent-moon-halberd.md,
// Enzo 19/10/2026): a ji, the Chinese halberd, a spear and a crescent blade in one. A shaft of night-blue lacquer bound in
// silver, a white tassel under a silver collar, a long silver point, and on one side a crescent moon of pale steel whose
// edge glows moonlight blue (full-bright: it shines in the dark), a moonstone set in its socket, a small hook on the other
// side to catch and trip.
//   models/item/crescent_moon_halberd_3d.json   the halberd in the hand (a spear for Epic Fight);
//   models/item/crescent_moon_halberd.json      NeoForge's separate transforms: inventory, ground and frames keep the icon;
//   art/blockbench/crescent_moon_halberd.bbmodel  the Blockbench source.
// Built as the Spear of Ruin (ruin-spear-model.cjs): cubes in the item's space, turned 45 degrees on the diagonal.
// Usage: node scripts/animation/crescent-halberd-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'crescent_moon_halberd';

// ---------------------------------------------------------------- the 32 x 32 texture of the 3D model

const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;
const SWATCHES = {
  // Night-blue lacquer, a faint sheen along the grain.
  shaft: (u, v) => [u % 4 === 1 ? '#2a3a66' : '#1c2850', 0.95 + noise(u, v) * 0.08],
  // Silver fittings, a bright line on each.
  silver: (u, v) => [v % 3 === 0 ? '#e4e8f0' : '#aab2c2', 0.96 + noise(u, v) * 0.06],
  // The steel of the point and the crescent, pale and cold.
  steel: (u, v) => [(u + v) % 5 === 0 ? '#dfe6f2' : '#c2cbdb', 0.97 + noise(u, v) * 0.05],
  // The moon's edge: moonlight, white at the heart, pale blue at the rims.
  moon: (u, v) => [u % 4 === 1 || u % 4 === 2 ? '#f2fbff' : '#9fd8ff', 1],
  // The white tassel, a little blue in its folds.
  tassel: (u, v) => [(u % 2) ? '#f2f6ff' : '#cfdcf2', 1 - v / 24],
  // The grip's binding: white silk over the lacquer.
  wrap: (u, v) => [(u + v) % 2 ? '#e8ecf4' : '#3a4a7a', 1],
};
const SWATCH_AT = Object.fromEntries(Object.keys(SWATCHES).map((m, i) => [m, [(i % 4) * 8, Math.floor(i / 4) * 8]]));
for (const [m, paintIt] of Object.entries(SWATCHES)) {
  const [sx, sy] = SWATCH_AT[m];
  for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
    const [c, light] = paintIt(u, v);
    TEX.set([...shade(c, light), 255], ((sy + v) * TEX_SIZE + sx + u) * 4);
  }
}
const GLOWING = new Set(['moon']);

// ---------------------------------------------------------------- the cubes, in the item's space (y along the shaft)

const PARTS = [];
const box = (mat, from, to) => PARTS.push({ name: `${mat}_${PARTS.length}`, mat, from, to });
const C = 8;
/** A square section round the shaft's axis, `w` wide, from y0 to y1. */
const section = (mat, y0, y1, w) => box(mat, [C - w / 2, y0, C - w / 2], [C + w / 2, y1, C + w / 2]);

// The butt: a silver cap and a spike.
section('silver', -8, -7.4, 0.5);
section('silver', -7.4, -6.2, 1.3);
// The shaft, two silver bands, the silk-bound grip between them.
section('shaft', -6.2, 3.4, 0.9);
section('silver', 3.4, 3.9, 1.15);
section('wrap', 3.9, 9.2, 1.0);
section('silver', 9.2, 9.7, 1.15);
section('shaft', 9.7, 20.6, 0.9);
// The collar, the tassel hanging from it on the hook's side.
section('silver', 20.6, 21.6, 1.6);
box('tassel', [C - 1.9, 16.2, C - 0.45], [C - 0.95, 20.6, C + 0.45]);
box('tassel', [C - 2.3, 14.8, C - 0.35], [C - 1.5, 17.0, C + 0.35]);
box('tassel', [C - 1.4, 15.4, C - 0.3], [C - 0.9, 16.4, C + 0.3]);
// The socket, the moonstone set in it on both faces.
section('silver', 21.6, 24.2, 1.2);
box('moon', [C - 0.35, 22.4, C - 0.75], [C + 0.35, 23.3, C + 0.75]);
// The point: a long leaf of steel, its edges and tip bright.
const point = [[24.2, 25.4, 1.7], [25.4, 27.0, 1.5], [27.0, 28.6, 1.15], [28.6, 30.0, 0.8], [30.0, 31.0, 0.45], [31.0, 31.8, 0.2]];
for (const [y0, y1, w] of point) {
  box('steel', [C - w / 2, y0, C - 0.22], [C + w / 2, y1, C + 0.22]);
  if (w > 0.5) box('moon', [C - 0.08, y0, C - 0.24], [C + 0.08, y1, C + 0.24]);
}
// The crescent moon on the right: the lands between a great circle and a smaller one set back toward the shaft, in
// columns half a pixel wide; its outer rim glows.
const OUTER = { x: C + 0.8, y: 24.8, r: 4.4 }, INNER = { x: C - 0.5, y: 24.8, r: 4.0 };
for (let x = C + 0.6; x < OUTER.x + OUTER.r; x += 0.5) {
  const mid = x + 0.25;
  const dy = Math.sqrt(Math.max(0, OUTER.r ** 2 - (mid - OUTER.x) ** 2));
  if (dy <= 0) continue;
  const top = OUTER.y + dy, bottom = OUTER.y - dy;
  const di = Math.sqrt(Math.max(0, INNER.r ** 2 - (mid - INNER.x) ** 2));
  const ranges = di > 0 && mid < INNER.x + INNER.r ? [[INNER.y + di, top], [bottom, INNER.y - di]] : [[bottom, top]];
  for (const [y0, y1] of ranges) {
    if (y1 - y0 < 0.2) continue;
    const rim = Math.min(0.5, (y1 - y0) / 2);
    const far = mid > OUTER.x + OUTER.r - 1.2;
    // The steel of the blade, the glowing rim on its outer side (the top of the upper horn, the bottom of the lower).
    if (far) { box('moon', [x, y0, C - 0.2], [x + 0.5, y1, C + 0.2]); continue; }
    if (y0 >= OUTER.y) { box('steel', [x, y0, C - 0.2], [x + 0.5, y1 - rim, C + 0.2]); box('moon', [x, y1 - rim, C - 0.22], [x + 0.5, y1, C + 0.22]); }
    else if (y1 <= OUTER.y) { box('moon', [x, y0, C - 0.22], [x + 0.5, y0 + rim, C + 0.22]); box('steel', [x, y0 + rim, C - 0.2], [x + 0.5, y1, C + 0.2]); }
    else box('steel', [x, y0, C - 0.2], [x + 0.5, y1, C + 0.2]);
  }
}
// The small hook on the other side, to catch a leg or a blade.
box('steel', [C - 1.9, 22.2, C - 0.2], [C - 0.6, 22.9, C + 0.2]);
box('steel', [C - 2.4, 22.6, C - 0.18], [C - 1.8, 24.2, C + 0.18]);
box('moon', [C - 2.55, 23.9, C - 0.2], [C - 2.1, 24.6, C + 0.2]);

for (const p of PARTS) for (const v of [...p.from, ...p.to]) if (v < -16 || v > 32) throw new Error(`${p.name} out of the model's bounds: ${v}`);

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
const gameFaces = p => Object.fromEntries(Object.entries(faces(p)).map(([face, x]) => [face, {
  ...x, uv: x.uv.map(v => v * 16 / TEX_SIZE),
  ...(GLOWING.has(p.mat) ? { neoforge_data: { block_light: 15, sky_light: 15, ambient_occlusion: false } } : {}),
}]));

// ---------------------------------------------------------------- the icon, drawn in pixel art on the diagonal

function iconDrawn() {
  const img = new Uint8Array(16 * 16 * 4);
  const P = { K: '#0c0e18', S: '#1c2850', s: '#2f4278', V: '#c8cfdd', v: '#8e97ab', W: '#f2fbff', M: '#9fd8ff', T: '#eef2fb', t: '#c4d0e8', G: '#e8ecf4' };
  const at = (x, y) => {
    const u = x + 0.5, v = 15.5 - y, along = (u + v) / 2, perp = (u - v) / 2;
    // The crescent, on the right of the shaft near the head.
    const dOut = Math.hypot(along - 11.2, perp - 0.9), dIn = Math.hypot(along - 11.2, perp + 0.6);
    if (perp > 0.5 && dOut <= 3.6 && dIn > 3.1) return dOut > 2.9 ? 'W' : 'V';
    // The point.
    if (along >= 11.6 && along <= 15.7) {
      const w = 0.95 * (1 - (along - 11.6) / 4.3);
      if (Math.abs(perp) <= w + 0.1) return Math.abs(perp) < 0.25 ? 'M' : perp < 0 ? 'v' : 'V';
    }
    if (along >= 10.6 && along < 11.6 && Math.abs(perp) <= 0.7) return 'V';
    if (along >= 10.0 && along < 10.6 && Math.abs(perp) <= 0.95) return 'V';
    // The tassel on the left under the collar.
    if (along >= 7.6 && along < 10.0 && perp < -0.4 && perp >= -1.9) return (x + y) % 2 ? 'T' : 't';
    // The shaft, its grip and bands, the butt.
    if (along >= 1.2 && along < 10.0 && Math.abs(perp) <= 0.55) {
      if (along > 3.4 && along < 5.6) return (x + y) % 2 ? 'G' : 's';
      if (Math.abs(along - 3.2) < 0.3 || Math.abs(along - 5.8) < 0.3) return 'V';
      return perp < 0 ? 's' : 'S';
    }
    if (along >= 0.3 && along < 1.2 && Math.abs(perp) <= 0.7) return 'V';
    return null;
  };
  const grid = [];
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) grid[y * 16 + x] = at(x, y);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let c = grid[y * 16 + x];
    if (!c && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && grid[ny * 16 + nx]; })) c = 'K';
    if (c) img.set([...hex(P[c]), 255], (y * 16 + x) * 4);
  }
  return img;
}

// ---------------------------------------------------------------- the game models

/** In the hand: as the Spear of Ruin, held at the middle of the shaft and tilted for Epic Fight's spear. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  thirdperson_lefthand: { rotation: [0, 90, -35], translation: [0, 2, 0.5], scale: [1.3, 1.3, 1.3] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.1, 1.1, 1.1] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const CREDIT = `Murimblock - scripts/animation/crescent-halberd-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
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
  // A spear for Epic Fight; its swing leaves a trail of moonlight.
  const data = path.join(ROOT, 'src', 'main', 'resources', 'data', 'murimblock', 'capabilities', 'weapons');
  fs.writeFileSync(path.join(data, `${NAME}.json`), JSON.stringify({ type: 'epicfight:spear' }, null, 2) + '\n');
  console.log('wrote the Crescent Moon Halberd:', PARTS.length, 'cubes');
}
module.exports = { PARTS, SWATCH_AT, TEX, TEX_SIZE, faces };
