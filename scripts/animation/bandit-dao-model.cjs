// The Bandit Dao in 3D: a Blockbench project in the Java item model format (art/blockbench/bandit_dao.bbmodel) and the
// game files it gives, under assets/murimblock (texture textures/item/bandit_dao_3d.png, 32 x 32):
//   models/item/bandit_dao_body.json    the sword without its tassel;
//   models/item/bandit_dao_tassel.json  the tassel alone, hanging straight down from the model's centre (8, 8, 8);
//   models/item/bandit_dao_held.json    the hand's transforms only: client/BanditDaoRenderer draws the body, then the
//                                       tassel turned about the pommel where its pendulum has swung it;
//   models/item/bandit_dao.json         NeoForge's separate transforms: in the inventory, on the ground and in frames
//                                       the game keeps the flat icon, textures/item/bandit_dao.png, drawn here from the
//                                       model seen from the front (the tassel hanging along the grip).
// The sword is built upright, then every cube is turned 45 degrees about the model's centre so it lies on the same
// diagonal as the 16x16 icon (pommel at the bottom left, point at the top right): with the vanilla swords' hand
// transforms, it sits in the hand exactly as the flat dao did. The cord hangs from the bottom of the pommel's ring,
// (8, -3.5, 8) upright, (8 - 11.5/sqrt 2, 8 - 11.5/sqrt 2, 8) once turned: BanditDaoRenderer's PIVOT. The grip is long
// (7.2) so the pommel and its tassel come out of the fist.
// A bandit's sabre, worn rather than fine, in relief:
//   blade: long and broad (1.6 blocks in the hand, 3.4 pixels across at its widest), curving a little more toward a
//          clipped point, in strips from the back to the edge: a thick dark spine, a
//          fuller sunk along it (thinner than the steel on either side), the hammered flat, a thin bright bevel nicked
//          twice by use; a false edge sharpened along the back of the point; a brass collar at its root;
//   guard: a round brass dish with a darker rim and a raised boss, an engraved ferrule;
//   grip:  red cord wound slantwise over black lacquer between two brass rings; pommel: a brass cap and a ring;
//   tassel: a cord, a wooden bead, a knot and three red strands.
// Usage: node scripts/animation/bandit-dao-model.cjs
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
/**
 * Sixteen 8 x 8 swatches, one per material, each painted texel by texel: (u, v) inside the swatch -> [colour, light].
 * A face takes the corner of its swatch that matches its own size (two texels per model pixel), so its details are not
 * stretched.
 */
const SWATCHES = {
  // The blade: the dark spine, the fuller darker still, the hammered flat, the bright bevel.
  spine: (u, v) => ['#5d646b', 0.94 + noise(u, v) * 0.08],
  fuller: (u, v) => ['#4c5258', 0.96 + noise(u, v) * 0.06],
  steel: (u, v) => ['#a3abb2', 0.92 + noise(u, v) * 0.12],
  edge: (u, v) => ['#eef2f5', 0.97 + (v % 4 === 0 ? 0.05 : 0)],
  // Brass with an engraved wave; darker brass for the rim and the ferrule.
  brass: (u, v) => [((u + Math.floor(v / 2) * 2) % 6 === 0) ? '#7c5c22' : '#ba9240', 0.95 + noise(u, v) * 0.08],
  brassDark: (u, v) => [(u + v) % 4 === 0 ? '#5e4518' : '#7c5c22', 0.95 + noise(u, v) * 0.08],
  // Red cord wound slantwise over black lacquer: a dark line between the turns.
  wrap: (u, v) => [((v + (u % 2)) % 3 === 2) ? '#1e1512' : ((v + (u % 2)) % 3 === 1 ? '#b5392d' : '#962e24'), 1],
  lacquer: (u, v) => ['#1e1512', 0.95 + noise(u, v) * 0.1],
  tassel: (u, v) => [(u % 2) ? '#a52c22' : '#cf3f30', 1 - (v / 16)],
  bead: (u, v) => [(v % 3 === 0) ? '#4a2c18' : '#6b4026', 0.95 + noise(u, v) * 0.08],
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

// The edge is on the -x side of the upright blade, the spine on the +x side: once turned onto the diagonal and put in a
// hand with the vanilla sword transforms, the edge faces down and forward, the spine up.
const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
/** A strip of the blade from x0 to x1 over y0..y1, `half` thick on either side of the middle (z 8). */
const strip = (name, x0, x1, y0, y1, half, mat) => part(name, [x0, y0, 8 - half], [x1, y1, 8 + half], mat);

// The tassel, hanging from the model's centre (8, 8, 8): its own model, swung by the renderer.
const TASSEL = [
  { name: 'tassel_cord', from: [7.8, 6.6, 7.8], to: [8.2, 8.0, 8.2], mat: 'tassel' },
  { name: 'tassel_bead', from: [7.5, 5.9, 7.5], to: [8.5, 6.6, 8.5], mat: 'bead' },
  { name: 'tassel_knot', from: [7.65, 5.5, 7.65], to: [8.35, 5.9, 8.35], mat: 'tassel' },
  { name: 'tassel_strand_a', from: [7.3, 3.7, 7.55], to: [7.8, 5.5, 8.05], mat: 'tassel' },
  { name: 'tassel_strand_b', from: [7.75, 3.4, 7.75], to: [8.25, 5.5, 8.25], mat: 'tassel' },
  { name: 'tassel_strand_c', from: [8.2, 3.7, 7.95], to: [8.7, 5.5, 8.45], mat: 'tassel' },
];
/** The bottom of the pommel's ring in the upright sword: where the cord hangs. */
const POMMEL_END = -3.5;

// The pommel: a brass cap, and a ring under it for the tassel's cord.
part('pommel_cap', [7.15, POMMEL_END + 0.5, 7.15], [8.85, POMMEL_END + 1.2, 8.85], 'brass');
part('ring_left', [7.45, POMMEL_END, 7.85], [7.7, POMMEL_END + 0.5, 8.15], 'brassDark');
part('ring_right', [8.3, POMMEL_END, 7.85], [8.55, POMMEL_END + 0.5, 8.15], 'brassDark');
part('ring_bottom', [7.45, POMMEL_END, 7.85], [8.55, POMMEL_END + 0.2, 8.15], 'brassDark');
// The grip between two brass rings.
// In three lengths, so the cord's turns keep their size (a face takes at most one swatch).
[[POMMEL_END + 1.2, 0.2], [0.2, 2.55], [2.55, 4.9]].forEach(([y0, y1], i) => part('grip_' + i, [7.4, y0, 7.4], [8.6, y1, 8.6], 'wrap'));
part('grip_ring_low', [7.25, POMMEL_END + 1.2, 7.25], [8.75, POMMEL_END + 1.7, 8.75], 'brass');
part('grip_ring_high', [7.25, 4.4, 7.25], [8.75, 4.9, 8.75], 'brass');
// The guard: a round dish, its darker rim underneath, a raised boss, then the ferrule.
part('guard_rim_a', [5.9, 4.75, 6.45], [10.1, 4.9, 9.55], 'brassDark');
part('guard_rim_b', [6.45, 4.75, 5.9], [9.55, 4.9, 10.1], 'brassDark');
part('guard_a', [5.7, 4.9, 6.7], [10.3, 5.65, 9.3], 'brass');
part('guard_b', [6.7, 4.9, 5.7], [9.3, 5.65, 10.3], 'brass');
part('guard_c', [6.2, 4.9, 6.2], [9.8, 5.65, 9.8], 'brass');
part('guard_boss', [7.05, 5.65, 7.05], [8.95, 6.0, 8.95], 'brass');
part('ferrule', [6.5, 6.0, 7.4], [9.1, 6.55, 8.6], 'brassDark');

// The blade, segment by segment toward the point, long and broad: its spine curving a little more near the end, the
// blade widening toward the point as a brigand's sabre does.
const WIDTHS = [2.2, 2.4, 2.65, 2.9, 3.2, 3.5, 3.8, 4.0];
const SEG = 2.0, ROOT_Y = 6.55;
const spineAt = i => 8.75 - 0.04 * i - 0.014 * i * i;
// Two nicks in the bevel (segment, from y, to y): a bandit's blade is not kept up.
const NICKS = { 2: [11.3, 11.7], 5: [17.4, 17.8] };
WIDTHS.forEach((w, i) => {
  const y0 = ROOT_Y + i * SEG - (i === 0 ? 0 : 0.2), y1 = ROOT_Y + (i + 1) * SEG, xs = spineAt(i), xe = xs - w;
  const last = i === WIDTHS.length - 1;
  // The spine, the fuller sunk beside it (it stops before the point), the hammered flat, the bevel.
  strip('spine_' + i, xs - 0.4, xs, y0, y1, 0.36, last ? 'edge' : 'spine');
  if (!last) {
    strip('fuller_' + i, xs - 0.85, xs - 0.4, y0, y1, 0.26, 'fuller');
    strip('flat_' + i, xe + 0.55, xs - 0.85, y0, y1, 0.32, 'steel');
  } else strip('flat_' + i, xe + 0.55, xs - 0.4, y0, y1, 0.32, 'steel');
  const nick = NICKS[i];
  if (nick) {
    strip('bevel_' + i + 'a', xe, xe + 0.55, y0, nick[0], 0.17, 'edge');
    strip('bevel_' + i + 'n', xe + 0.25, xe + 0.55, nick[0], nick[1], 0.17, 'edge');
    strip('bevel_' + i + 'b', xe, xe + 0.55, nick[1], y1, 0.17, 'edge');
  } else strip('bevel_' + i, xe, xe + 0.55, y0, y1, 0.17, 'edge');
});
// A brass collar holding the blade's root.
part('blade_collar', [spineAt(0) - WIDTHS[0] - 0.08, ROOT_Y, 7.56], [spineAt(0) + 0.08, ROOT_Y + 0.65, 8.44], 'brassDark');
// The clipped point: the edge rising to the spine in steps, a false edge along the back.
const sx = spineAt(WIDTHS.length), TIP_Y = ROOT_Y + WIDTHS.length * SEG;
[[3.4, 0.8], [2.4, 0.7], [1.4, 0.6]].reduce((y, [w, h], i) => {
  strip('tip_' + i, sx - w + 0.45, sx - 0.35, y, y + h, 0.28 - i * 0.04, 'steel');
  strip('tip_' + i + '_bevel', sx - w, sx - w + 0.45, y, y + h, 0.16 - i * 0.01, 'edge');
  strip('tip_' + i + '_back', sx - 0.35, sx, y, y + h, 0.2 - i * 0.02, 'edge');
  return y + h;
}, TIP_Y);
strip('tip_point', sx - 0.6, sx, TIP_Y + 2.1, TIP_Y + 2.6, 0.12, 'edge');

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
const ICON = { spine: '#5d646b', fuller: '#4c5258', steel: '#a3abb2', edge: '#eef2f5', brass: '#ba9240', brassDark: '#7c5c22',
  wrap: '#962e24', lacquer: '#1e1512', tassel: '#c4382c', bead: '#6b4026' };

function icon() {
  const img = new Uint8Array(16 * 16 * 4);
  const a = ROTATION.angle * Math.PI / 180;
  const turn = ([x, y]) => [8 + (x - 8) * Math.cos(a) - (y - 8) * Math.sin(a), 8 + (x - 8) * Math.sin(a) + (y - 8) * Math.cos(a)];
  // Drawn broader than the model, as Minecraft's sprites are, or the blade is a thread at this size: twice across the
  // blade and the guard, three times across the grip, the pommel and the tassel.
  const broad = p => ([x, y]) => [8 + (x - 8) * (p.from[1] < 4.75 ? 3.0 : 2.0), y];
  // The tassel under the pommel, shortened so the blade keeps the room.
  const hang = y => POMMEL_END - (8 - y) * 0.55;
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
  const order = ['tassel', 'bead', 'lacquer', 'wrap', 'brassDark', 'brass', 'fuller', 'spine', 'steel', 'edge'];
  // One pixel of margin all round for the outline.
  for (let py = 1; py < 15; py++) for (let px = 1; px < 15; px++) {
    const x = x0 + (px - 1 + 0.5) / 14 * span, y = y0 + (14 - py + 0.5) / 14 * span;
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

/** The vanilla handheld sword's transforms, so the dao sits in the hand as any sword does. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};

const CREDIT = 'Murimblock - scripts/animation/bandit-dao-model.cjs (Blockbench source: art/blockbench/bandit_dao.bbmodel)';
const TEXTURES = { 0: 'murimblock:item/bandit_dao_3d', particle: 'murimblock:item/bandit_dao' };
const bodyModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: gameFaces(p) })),
};
const tasselModel = {
  credit: CREDIT, texture_size: [TEX_SIZE, TEX_SIZE], textures: TEXTURES,
  elements: TASSEL.map(p => ({ name: p.name, from: p.from, to: p.to, faces: gameFaces(p) })),
};
const heldModel = { credit: CREDIT, parent: 'builtin/entity', textures: { particle: 'murimblock:item/bandit_dao' }, display: DISPLAY };

const itemModel = {
  loader: 'neoforge:separate_transforms',
  base: { parent: 'murimblock:item/bandit_dao_held' },
  perspectives: {
    gui: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
    ground: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
    fixed: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
  },
};

// ---------------------------------------------------------------- the Blockbench project (Java item model format)

const id = name => { const h = crypto.createHash('md5').update('bandit_dao/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
function bbmodel() {
  // The whole sword, its tassel at rest along the grip's line.
  const drop = 8 - POMMEL_END;
  const hanging = TASSEL.map(p => ({ ...p, from: [p.from[0], p.from[1] - drop, p.from[2]], to: [p.to[0], p.to[1] - drop, p.to[2]] }));
  const elements = [...hanging, ...PARTS].map(p => {
    const f = faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin,
      faces: f, type: 'cube', uuid: id(p.name) };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: 'bandit_dao', parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX_SIZE, height: TEX_SIZE }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: 'bandit_dao_3d.png', folder: 'item', namespace: 'murimblock', id: '0', width: TEX_SIZE, height: TEX_SIZE,
      uv_width: TEX_SIZE, uv_height: TEX_SIZE, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + png(TEX_SIZE, TEX_SIZE, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

module.exports = { bbmodel };

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'bandit_dao_3d.png'), png(TEX_SIZE, TEX_SIZE, TEX));
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'bandit_dao.png'), png(16, 16, icon()));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao_body.json'), JSON.stringify(bodyModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao_tassel.json'), JSON.stringify(tasselModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao_held.json'), JSON.stringify(heldModel, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao.json'), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'bandit_dao.bbmodel'), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the 3D dao:', PARTS.length, 'cubes and a tassel of', TASSEL.length);
}
