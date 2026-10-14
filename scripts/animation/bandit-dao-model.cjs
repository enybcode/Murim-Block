// The Bandit Dao in 3D: a Blockbench project in the Java item model format (art/blockbench/bandit_dao.bbmodel) and the
// game files it gives (assets/murimblock/models/item/bandit_dao_3d.json, its texture textures/item/bandit_dao_3d.png).
// The sword is built upright, then every cube is turned 45 degrees about the model's centre so it lies on the same
// diagonal as the 16x16 icon (pommel at the bottom left, point at the top right): with the vanilla swords' hand
// transforms, it sits in the hand exactly as the flat dao did. In the inventory, on the ground and in frames the game
// keeps the flat icon (models/item/bandit_dao.json, NeoForge's separate transforms).
//   blade: thick 0.6, widening from 1.6 to 2.9 toward a clipped point, dark spine on one side, bright edge on the other;
//   guard: a round brass disc; grip: wrapped in red cloth; pommel: brass, with a short red tassel.
// Usage: node scripts/animation/bandit-dao-model.cjs
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const crypto = require('crypto');

const ROOT = path.join(__dirname, '..', '..');

// ---------------------------------------------------------------- the texture (16 x 16)

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const TEX = new Uint8Array(16 * 16 * 4);
const paint = (x, y, h) => { const [r, g, b] = hex(h); const i = (y * 16 + x) * 4; TEX[i] = r; TEX[i + 1] = g; TEX[i + 2] = b; TEX[i + 3] = 255; };
// Row 0: the blade's flat, across from the spine (left) to the edge (right).
['#6f777f', '#7d858c', '#8f979e', '#a3abb2', '#b3bac0', '#bec5ca', '#c6ccd1', '#ccd2d6',
  '#d3d8dc', '#d9dee1', '#dfe3e6', '#e6e9ec', '#ecf0f2', '#f2f4f6', '#f8f9fa', '#ffffff'].forEach((c, x) => paint(x, 0, c));
// Row 1: the same a shade darker (the flat seen from the other side).
['#666e76', '#737b82', '#848c93', '#979fa6', '#a6adb3', '#b0b7bc', '#b8bec3', '#bec4c8',
  '#c4c9cd', '#cacfd2', '#d0d4d7', '#d6dadd', '#dce0e2', '#e2e5e7', '#e8ebec', '#f0f2f3'].forEach((c, x) => paint(x, 1, c));
for (let x = 0; x < 4; x++) paint(x, 2, '#5d646b');          // the spine
for (let x = 4; x < 8; x++) paint(x, 2, '#ffffff');          // the edge
for (let x = 0; x < 4; x++) paint(x, 4, x % 2 ? '#a8843e' : '#ba9240');   // brass
for (let x = 4; x < 8; x++) paint(x, 4, '#7c5c22');          // dark brass
for (let y = 5; y < 9; y++) for (let x = 0; x < 4; x++) paint(x, y, (y + x) % 2 ? '#962e24' : '#621c16');   // red wrap
for (let y = 5; y < 8; y++) for (let x = 4; x < 6; x++) paint(x, y, y === 7 ? '#7e2018' : '#c4382c');      // tassel

function png(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) Buffer.from(rgba.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------------------------------------------------------------- the cubes (upright: the blade up +y, flat in x, thin in z)

/** Face UVs (in texture pixels) by material. */
const MAT = {
  flat: { north: [0, 0, 16, 1], south: [16, 1, 0, 2], east: [4, 2, 8, 3], west: [0, 2, 4, 3], up: [0, 2, 4, 3], down: [0, 2, 4, 3] },
  tip: { north: [0, 0, 16, 1], south: [16, 1, 0, 2], east: [4, 2, 8, 3], west: [0, 2, 4, 3], up: [4, 2, 8, 3], down: [0, 2, 4, 3] },
  brass: { all: [0, 4, 4, 5] },
  brassDark: { all: [4, 4, 8, 5] },
  wrap: { all: [0, 5, 4, 9] },
  tassel: { all: [4, 5, 6, 8] },
};
const PARTS = [];
const part = (name, from, to, mat) => PARTS.push({ name, from, to, mat });
const Z0 = 7.7, Z1 = 8.3;
part('tassel_tuft', [7.3, -3.3, 7.3], [8.7, -2.4, 8.7], 'tassel');
part('tassel_cord', [7.7, -2.4, 7.7], [8.3, -0.5, 8.3], 'tassel');
part('pommel', [7.2, -0.5, 7.2], [8.8, 0.7, 8.8], 'brass');
part('grip', [7.4, 0.7, 7.4], [8.6, 4.9, 8.6], 'wrap');
part('guard', [6.1, 4.9, 6.9], [9.9, 5.7, 9.1], 'brass');
part('guard_cross', [6.9, 4.9, 6.1], [9.1, 5.7, 9.9], 'brass');
part('ferrule', [7.2, 5.7, 7.5], [8.8, 6.3, 8.5], 'brassDark');
// The blade: the spine nearly straight on the -x side, the edge swelling out on the +x side toward the point.
const widths = [1.6, 1.8, 2.05, 2.3, 2.6, 2.85];
widths.forEach((w, i) => {
  const y0 = 6.3 + i * 2, x0 = 7.25 + i * 0.05;
  part('blade_' + i, [x0, y0, Z0], [x0 + w, y0 + 2, Z1], 'flat');
});
// The clipped point: the edge cut back toward the spine.
const sx = 7.25 + 6 * 0.05;
part('tip_0', [sx, 18.3, Z0], [sx + 2.4, 19.0, Z1], 'tip');
part('tip_1', [sx, 19.0, Z0], [sx + 1.5, 19.6, Z1], 'tip');
part('tip_2', [sx, 19.6, Z0], [sx + 0.7, 20.1, Z1], 'tip');

const ROTATION = { angle: -45, axis: 'z', origin: [8, 8, 8] };

function faces(mat) {
  const m = MAT[mat], out = {};
  for (const f of ['north', 'east', 'south', 'west', 'up', 'down']) out[f] = { uv: m[f] || m.all, texture: '#0' };
  return out;
}

// ---------------------------------------------------------------- the game model

/** The vanilla handheld sword's transforms, so the dao sits in the hand as any sword does. */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};

const model3d = {
  credit: 'Murimblock - scripts/animation/bandit-dao-model.cjs (Blockbench source: art/blockbench/bandit_dao.bbmodel)',
  texture_size: [16, 16],
  textures: { 0: 'murimblock:item/bandit_dao_3d', particle: 'murimblock:item/bandit_dao' },
  elements: PARTS.map(p => ({ name: p.name, from: p.from, to: p.to, rotation: ROTATION, faces: faces(p.mat) })),
  display: DISPLAY,
};

const itemModel = {
  loader: 'neoforge:separate_transforms',
  base: { parent: 'murimblock:item/bandit_dao_3d' },
  perspectives: {
    gui: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
    ground: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
    fixed: { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/bandit_dao' } },
  },
};

// ---------------------------------------------------------------- the Blockbench project (Java item model format)

const uuid = () => crypto.randomUUID();
function bbmodel() {
  const elements = PARTS.map(p => {
    const f = faces(p.mat);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: 0 };
    return { name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, rotation: [0, 0, ROTATION.angle], origin: ROTATION.origin,
      faces: f, type: 'cube', uuid: uuid() };
  });
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: 'bandit_dao', parent: '', ambientocclusion: true, front_gui_light: false, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements, outliner: elements.map(e => e.uuid),
    textures: [{ path: '', name: 'bandit_dao_3d.png', folder: 'item', namespace: 'murimblock', id: '0', width: 16, height: 16,
      uv_width: 16, uv_height: 16, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
      visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + png(16, 16, TEX).toString('base64') }],
    display: DISPLAY,
  };
}

module.exports = { bbmodel };

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'bandit_dao_3d.png'), png(16, 16, TEX));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao_3d.json'), JSON.stringify(model3d, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'bandit_dao.json'), JSON.stringify(itemModel, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'bandit_dao.bbmodel'), JSON.stringify(bbmodel(), null, 1));
  console.log('wrote the 3D dao:', PARTS.length, 'cubes');
}
