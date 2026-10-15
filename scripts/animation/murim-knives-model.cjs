// Two Murim knives, daggers for Epic Fight (its own dagger moveset, data/murimblock/capabilities/weapons):
//   bishou        匕首, a straight double-edged dagger with a raised ridge, a gold guard with upturned ends, a black cord
//                 grip and a cut jade pommel;
//   flying_knife  飞刀, a thin leaf blade, a red-wrapped grip, a steel ring at the pommel and a red cloth hanging from it.
// Chosen by Enzo from the proposals of 15/10/2026 (2 and 4). For each knife, as the Azure Peak Spear:
//   models/item/<name>_3d.json   the knife in the hand;
//   models/item/<name>.json      NeoForge's separate transforms: the inventory, the ground and frames keep the flat
//                                icon textures/item/<name>.png, drawn here (the proposal sprite Enzo chose);
//   art/blockbench/<name>.bbmodel  the Blockbench source.
// Built upright (the grip along +y from the bottom of the 16 x 16 box, the blade's flat facing z), then every cube is
// turned 45 degrees about the model's centre onto the icon's diagonal. A knife is shorter than a sword: it ends about
// three pixels short of the corner a sword's point reaches.
// Usage: node scripts/animation/murim-knives-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const TEX_SIZE = 32;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const shade = (h, f) => hex(h).map(v => Math.max(0, Math.min(255, Math.round(v * f))));
const noise = (x, y) => ((x * 73 + y * 151 + x * y * 7) % 11) / 10;

/** 8 x 8 swatches, one per material: (u, v) -> [colour, light]. */
const SWATCHES = {
  steel: (u, v) => ['#b9c1c8', 0.93 + noise(u, v) * 0.1],
  steelDark: (u, v) => ['#727b84', 0.94 + noise(u, v) * 0.08],
  edge: (u, v) => ['#f2f5f7', 0.97 + (v % 4 === 0 ? 0.04 : 0)],
  gold: (u, v) => [v % 3 === 1 ? '#a8822f' : '#E2B746', 0.95 + noise(u, v) * 0.08],
  jade: (u, v) => [(u + v) % 5 === 0 ? '#9adcb8' : '#5fbf8f', 0.92 + noise(u, v) * 0.12],
  cord: (u, v) => [(u + v) % 4 < 2 ? '#2e2e38' : '#18181e', 1],
  red: (u, v) => [(u + v) % 4 < 2 ? '#c4302a' : '#8e1c18', 0.96 + noise(u, v) * 0.06],
  cloth: (u, v) => [v % 3 === 0 ? '#a82420' : '#d8392f', 0.95 + noise(u, v) * 0.08],
  ring: (u, v) => ['#8f99a3', 0.92 + noise(u, v) * 0.12],
};
const SWATCH_AT = Object.fromEntries(Object.keys(SWATCHES).map((m, i) => [m, [(i % 4) * 8, Math.floor(i / 4) * 8]]));
function texture() {
  const tex = new Uint8Array(TEX_SIZE * TEX_SIZE * 4);
  for (const [m, paintIt] of Object.entries(SWATCHES)) {
    const [sx, sy] = SWATCH_AT[m];
    for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
      const [c, light] = paintIt(u, v);
      tex.set([...shade(c, light), 255], ((sy + v) * TEX_SIZE + sx + u) * 4);
    }
  }
  return tex;
}

// ---------------------------------------------------------------- the knives (upright: the grip along +y)

function build(cubes) {
  const parts = [];
  const part = (name, from, to, mat) => parts.push({ name, from, to, mat });
  const round = (name, y0, y1, w, mat, d = w) => part(name, [8 - w / 2, y0, 8 - d / 2], [8 + w / 2, y1, 8 + d / 2], mat);
  /** A blade, row by row from y0: a flat steel body, bright edges on both sides, a raised ridge, a fine point. */
  const blade = (y0, widths, row, t) => {
    widths.forEach((w, i) => {
      const y = y0 + i * row, e = Math.min(0.22, w / 3);
      part('blade_' + i, [8 - w / 2 + e, y, 8 - t / 2], [8 + w / 2 - e, y + row, 8 + t / 2], 'steel');
      part('edge_l' + i, [8 - w / 2, y, 8 - t / 3], [8 - w / 2 + e, y + row, 8 + t / 3], 'edge');
      part('edge_r' + i, [8 + w / 2 - e, y, 8 - t / 3], [8 + w / 2, y + row, 8 + t / 3], 'edge');
    });
    const top = y0 + widths.length * row;
    return top;
  };
  cubes({ part, round, blade });
  return parts;
}

const KNIVES = {
  bishou: build(({ part, round, blade }) => {
    // The pommel: a gold cap, a jade block, a gold collar.
    round('pommel_cap', 0.4, 0.7, 0.8, 'gold');
    round('pommel_jade', 0.7, 2.0, 1.5, 'jade');
    round('pommel_collar', 2.0, 2.3, 1.2, 'gold');
    // The grip, black cord with three raised turns.
    round('grip', 2.3, 5.6, 0.95, 'cord');
    for (const [i, y] of [2.9, 3.9, 4.9].entries()) round('cord_' + i, y, y + 0.3, 1.1, 'cord');
    // The guard: a gold bar, its ends turned up toward the blade, a jade bead at its heart.
    part('guard', [6.1, 5.6, 7.45], [9.9, 6.2, 8.55], 'gold');
    part('guard_end_l', [6.1, 6.2, 7.55], [6.6, 6.9, 8.45], 'gold');
    part('guard_end_r', [9.4, 6.2, 7.55], [9.9, 6.9, 8.45], 'gold');
    part('guard_jade', [7.6, 5.7, 7.3], [8.4, 6.4, 8.7], 'jade');
    // The blade: double-edged, straight, narrowing to its point, a raised ridge down its middle.
    const top = blade(6.2, [1.6, 1.55, 1.5, 1.42, 1.32, 1.18, 1.0, 0.78, 0.52], 0.74, 0.32);
    part('ridge', [7.85, 6.4, 7.79], [8.15, top - 0.6, 8.21], 'steelDark');
    part('point', [7.88, top, 7.9], [8.12, top + 0.6, 8.1], 'edge');
  }),
  flying_knife: build(({ part, round, blade }) => {
    // The ring at the pommel, a square loop of steel, and the red cloth tied through it, hanging in folds.
    part('ring_bottom', [7.0, 0.2, 7.75], [9.0, 0.6, 8.25], 'ring');
    part('ring_left', [7.0, 0.6, 7.75], [7.4, 2.0, 8.25], 'ring');
    part('ring_right', [8.6, 0.6, 7.75], [9.0, 2.0, 8.25], 'ring');
    part('ring_top', [7.0, 2.0, 7.75], [9.0, 2.4, 8.25], 'ring');
    [[0.0, 0.2], [0.35, -0.9], [0.8, -2.0], [0.55, -3.1], [0.1, -4.2]].forEach(([dx, y], i) =>
      part('cloth_' + i, [7.65 + dx, y - 1.15, 7.94], [8.35 + dx - i * 0.04, y + 0.05, 8.06], 'cloth'));
    // The grip, wrapped in red; a narrow steel collar for a guard.
    round('grip', 2.4, 5.4, 0.85, 'red');
    round('collar', 5.4, 5.8, 1.15, 'ring', 0.8);
    // The blade: a thin leaf, widest a third of the way up.
    const top = blade(5.8, [0.7, 1.1, 1.3, 1.35, 1.25, 1.05, 0.8, 0.5], 0.75, 0.26);
    part('ridge', [7.88, 6.0, 7.82], [8.12, top - 0.6, 8.18], 'steelDark');
    part('point', [7.9, top, 7.92], [8.1, top + 0.55, 8.08], 'edge');
  }),
};

const ROTATION = { angle: -45, axis: 'z', origin: [8, 8, 8] };
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

// ---------------------------------------------------------------- the icons: drawn on the item's diagonal
// The model seen flat reads as a smudge at 16 x 16, so each icon is drawn: the knife along the diagonal (pommel bottom
// left), u along it and v across it (positive toward the edge side), coloured by part, then outlined.
const SPRITE = {
  O: '#141014', S: '#eef2f5', s: '#aeb8c2', d: '#6d7780', G: '#E2B746', g: '#a7792a', R: '#c8322b', r: '#7d1c18',
  J: '#6cc79a', j: '#2f7d5a', B: '#2a2a33', b: '#4a4a5e',
};
const taper = (u, end, len, w) => (u > end - len ? w * Math.max(0, (end - u) / len) : w);
const SHAPES = {
  bishou: (u, v) => {
    if (u >= -0.4 && u < 1.1 && Math.abs(v) < 1.0) return v < 0 ? 'J' : 'j';
    if (u >= 1.1 && u < 5 && Math.abs(v) < 0.65) return Math.floor(u * 1.6) % 2 ? 'B' : 'b';
    if (u >= 5 && u < 6 && Math.abs(v) < 1.9) return Math.abs(v) > 1.2 ? 'g' : 'G';
    if (u >= 6 && u < 17.4) { const w = taper(u, 17.4, 4.5, 1.15);
      if (Math.abs(v) < w) return Math.abs(v) < 0.35 ? 'S' : v < 0 ? 's' : 'd'; }
    return null;
  },
  flying_knife: (u, v) => {
    const rr = Math.hypot(u - 0.2, v);
    if (rr > 0.6 && rr < 1.35) return 'd';
    if (u < -0.3 && u > -4 && Math.abs(v - Math.sin(u * 1.3) * 0.8 - 0.4) < 0.6) return u > -2 ? 'R' : 'r';
    if (u >= 1.3 && u < 5.4 && Math.abs(v) < 0.6) return Math.floor(u * 1.5) % 2 ? 'R' : 'r';
    if (u >= 5.4 && u < 6.2 && Math.abs(v) < 0.9) return 's';
    if (u >= 6.2 && u < 17.6) { const t = (u - 6.2) / 11.4, w = 0.45 + Math.sin(Math.PI * Math.min(1, t * 1.15)) * 0.75;
      if (Math.abs(v) < w) return v > 0.15 ? 'S' : v > -0.3 ? 's' : 'd'; }
    return null;
  },
};
function icon(name) {
  const shape = SHAPES[name], cell = Array.from({ length: 16 }, () => Array(16).fill(null));
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const px = x + 0.5 - 3.5, py = y + 0.5 - 12.5;
    cell[y][x] = shape((px - py) * Math.SQRT1_2 / 0.74, (px + py) * Math.SQRT1_2 / 0.95);
  }
  const img = new Uint8Array(16 * 16 * 4);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    let c = cell[y][x];
    // The outline, but not round the cloth and tassel, which stay loose.
    if (!c && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const n = cell[y + dy]?.[x + dx]; return n && n !== 'R' && n !== 'r'; })) c = 'O';
    if (c) img.set([...hex(SPRITE[c]), 255], (y * 16 + x) * 4);
  }
  return img;
}

// ---------------------------------------------------------------- the game models

/** In the hand as a vanilla sword is held (minecraft:item/handheld), a little smaller. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 3, 0.5], scale: [0.8, 0.8, 0.8] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 3, 0.5], scale: [0.8, 0.8, 0.8] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
};
function files(name, parts, tex) {
  const credit = `Murimblock - scripts/animation/murim-knives-model.cjs (Blockbench source: art/blockbench/${name}.bbmodel)`;
  const model3d = {
    credit, texture_size: [TEX_SIZE, TEX_SIZE],
    textures: { 0: `murimblock:item/${name}_3d`, particle: `murimblock:item/${name}` },
    elements: parts.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
    display: DISPLAY,
  };
  const flat = { parent: 'minecraft:item/handheld', textures: { layer0: `murimblock:item/${name}` } };
  const itemModel = { loader: 'neoforge:separate_transforms', base: { parent: `murimblock:item/${name}_3d` }, perspectives: { gui: flat, ground: flat, fixed: flat } };
  const id = n => { const h = crypto.createHash('md5').update(name + '/' + n).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
  const elements = parts.map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin, faces: f, type: 'cube', uuid: id(p.name) };
  });
  const bbmodel = {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name, parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: `${name}_3d.png`, folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
      uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, tex).toString('base64') }],
    display: DISPLAY,
  };
  return { model3d, itemModel, bbmodel };
}

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const tex = texture();
  for (const [name, parts] of Object.entries(KNIVES)) {
    const { model3d, itemModel, bbmodel } = files(name, parts, tex);
    fs.writeFileSync(path.join(assets, 'textures', 'item', `${name}_3d.png`), png(TEX_SIZE, TEX_SIZE, tex));
    fs.writeFileSync(path.join(assets, 'textures', 'item', `${name}.png`), png(16, 16, icon(name)));
    fs.writeFileSync(path.join(assets, 'models', 'item', `${name}_3d.json`), JSON.stringify(model3d, null, 2) + '\n');
    fs.writeFileSync(path.join(assets, 'models', 'item', `${name}.json`), JSON.stringify(itemModel, null, 2) + '\n');
    fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${name}.bbmodel`), JSON.stringify(bbmodel, null, 1));
    console.log('wrote', name + ':', parts.length, 'cubes');
  }
}
