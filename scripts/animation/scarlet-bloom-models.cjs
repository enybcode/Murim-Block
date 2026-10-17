// Scarlet Bloom's crystals (martial/ScarletBloom, client/ScarletPieceRenderer), GeckoLib models in the style of the
// Sanguine Art's thrown crystal: the same faceted look and the same texture (textures/entity/blood_crystal_shot.png and
// its glowmask), so they read as one family.
//   geo/scarlet_spike.geo.json  a crystal spike rising from the ground: a faceted pillar, a two-step point, three small
//                               crystals leaning out at its foot (1.1 block tall at scale 1, its foot at the origin);
//   geo/scarlet_rod.geo.json    a beam of the cage: a long thin crystal, pointed at both ends (1 block at scale 1, along
//                               +Y from the origin);
//   geo/scarlet_shard.geo.json  a shard: a small crystal pointed both ways (0.75 block, centred);
//   geo/scarlet_bloom.geo.json  the bloom that bursts the cage: fourteen crystal petals thrown out all round from a core
//                               (about 1.2 blocks across at scale 1, centred).
// And the blood of the body it bursts, on its own texture (textures/entity/scarlet_blood.png, dark to bright red, glossy):
//   geo/scarlet_blood.geo.json  a gout of blood: a lumpy blob trailing two drops (about 0.4 block at scale 1, centred);
//   geo/scarlet_puddle.geo.json a pool of blood on the ground: a flat irregular round of slabs (1 block across at scale
//                               1, its underside at the origin).
// Model units are pixels (16 to a block). Open them in Blockbench (File > Open, Bedrock geometry) to look or edit.
//   node scripts/animation/scarlet-bloom-models.cjs
const fs = require('node:fs'), path = require('node:path');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

/** A cube with every face on one patch of the crystal texture (its shades run across the patch). */
function cube(origin, size, uv, rotation = [0, 45, 0], pivot = null) {
  const face = (w, h) => ({ uv, uv_size: [w, h] });
  const [w, h, d] = size;
  return {
    origin, size, rotation, pivot: pivot || [origin[0] + w / 2, origin[1], origin[2] + d / 2],
    uv: { north: face(w, h), south: face(w, h), east: face(d, h), west: face(d, h), up: face(w, d), down: face(w, d) },
  };
}

function geo(identifier, bones, bounds) {
  return { format_version: '1.12.0', 'minecraft:geometry': [{
    description: { identifier, texture_width: 32, texture_height: 32, visible_bounds_width: bounds, visible_bounds_height: bounds,
      visible_bounds_offset: [0, bounds / 4, 0] },
    bones,
  }] };
}

// The thrown crystal's texture patches: [8,0] the body, [16,0] and [24,0] the bright and dark tips, [0,8] the fragments.
const BODY = [8, 0], BRIGHT = [16, 0], DARK = [24, 0], FRAG = [0, 8];

// ---- the spike
const spike = geo('geometry.scarlet_spike', [{ name: 'spike', pivot: [0, 0, 0], cubes: [
  cube([-2, 0, -2], [4, 12, 4], BODY),
  cube([-1.4, 11, -1.4], [2.8, 4.5, 2.8], BRIGHT),
  cube([-0.7, 15, -0.7], [1.4, 2.5, 1.4], BRIGHT),
  // Three small crystals at its foot, leaning out.
  ...[0, 120, 240].map(a => cube([-0.8, 0, -0.8], [1.6, 6, 1.6], DARK, [28, a + 20, 0], [0, 0, 0])),
] }], 3);

// ---- the beam
const rod = geo('geometry.scarlet_rod', [{ name: 'rod', pivot: [0, 0, 0], cubes: [
  cube([-0.6, 1, -0.6], [1.2, 14, 1.2], BODY),
  cube([-0.35, 0, -0.35], [0.7, 1, 0.7], BRIGHT),
  cube([-0.35, 15, -0.35], [0.7, 1, 0.7], BRIGHT),
] }], 3);

// ---- a shard: a small crystal, pointed both ways, centred (0.75 block long at scale 1)
const shard = geo('geometry.scarlet_shard', [{ name: 'shard', pivot: [0, 0, 0], cubes: [
  cube([-1.5, -3.5, -1.5], [3, 7, 3], BODY),
  cube([-0.9, 3.5, -0.9], [1.8, 2.5, 1.8], BRIGHT),
  cube([-0.9, -6, -0.9], [1.8, 2.5, 1.8], DARK),
] }], 2);

// ---- the bloom: petals on a Fibonacci sphere, each thrown out from the core
const petals = [];
const N = 14, golden = Math.PI * (3 - Math.sqrt(5));
for (let i = 0; i < N; i++) {
  const y = 1 - (i + 0.5) * 2 / N, tilt = Math.acos(y) * 180 / Math.PI, yaw = (i * golden * 180 / Math.PI) % 360;
  const long = 8 + (i % 3) * 1.5;
  petals.push(cube([-1, 0, -1], [2, long, 2], i % 2 ? BODY : DARK, [tilt, yaw, 0], [0, 0, 0]));
  petals.push(cube([-0.5, long, -0.5], [1, 2, 1], BRIGHT, [tilt, yaw, 0], [0, 0, 0]));
}
const bloom = geo('geometry.scarlet_bloom', [{ name: 'bloom', pivot: [0, 0, 0], cubes: [
  cube([-2.5, -2.5, -2.5], [5, 5, 5], BODY, [45, 45, 0], [0, 0, 0]),
  ...petals,
] }], 4);

// ---- blood: its own texture, 32 x 32: [0,0] dark, [8,0] deep, [16,0] bright, [24,0] the glossy highlight
const RED_DARK = [0, 0], RED = [8, 0], RED_BRIGHT = [16, 0], GLOSS = [24, 0];
const blood = geo('geometry.scarlet_blood', [{ name: 'blood', pivot: [0, 0, 0], cubes: [
  cube([-2.5, -2, -2], [5, 4, 4], RED, [10, 20, 0], [0, 0, 0]),
  cube([-1.5, -1, -3], [3, 3, 2], RED_BRIGHT, [0, -15, 10], [0, 0, 0]),
  cube([-1, 1.5, -1], [2, 1.5, 2], GLOSS, [0, 30, 0], [0, 0, 0]),
  cube([1.8, -3, -0.8], [1.6, 1.6, 1.6], RED_DARK, [0, 45, 0], [0, 0, 0]),
  cube([-0.5, -5, -0.5], [1, 1, 1], RED, [0, 45, 0], [0, 0, 0]),
] }], 1);
const puddle = geo('geometry.scarlet_puddle', [{ name: 'puddle', pivot: [0, 0, 0], cubes: [
  cube([-6, 0, -4], [12, 0.3, 8], RED_DARK, [0, 0, 0], [0, 0, 0]),
  cube([-4, 0.05, -6], [8, 0.3, 12], RED, [0, 15, 0], [0, 0, 0]),
  cube([-5, 0.1, -5], [9, 0.3, 9], RED, [0, 40, 0], [0, 0, 0]),
  cube([-2, 0.15, -2.5], [4, 0.3, 3], GLOSS, [0, 20, 0], [0, 0, 0]),
  cube([5, 0, 2], [2.5, 0.3, 2.5], RED_DARK, [0, 0, 0], [0, 0, 0]),
  cube([-7.5, 0, -1], [2, 0.3, 2], RED_DARK, [0, 30, 0], [0, 0, 0]),
] }], 2);

/** The blood's texture: four patches of 8 x 8, dark to bright, a little grain, the gloss pale. */
function bloodTexture() {
  const zlib = require('node:zlib');
  const W = 32, H = 32, shades = [[70, 4, 10], [128, 10, 22], [176, 24, 36], [232, 120, 128]];
  const raw = Buffer.alloc((W * 4 + 1) * H);
  for (let y = 0; y < H; y++) {
    raw[y * (W * 4 + 1)] = 0;
    for (let x = 0; x < W; x++) {
      const o = y * (W * 4 + 1) + 1 + x * 4, patch = Math.min(3, x >> 3), base = shades[patch];
      const grain = 0.9 + ((x * 7 + y * 13) % 5) * 0.05;
      raw[o] = Math.min(255, base[0] * grain); raw[o + 1] = Math.min(255, base[1] * grain); raw[o + 2] = Math.min(255, base[2] * grain);
      raw[o + 3] = y < 16 ? 255 : 0;
    }
  }
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

for (const [name, model] of [['scarlet_spike', spike], ['scarlet_rod', rod], ['scarlet_shard', shard], ['scarlet_bloom', bloom],
  ['scarlet_blood', blood], ['scarlet_puddle', puddle]])
  fs.writeFileSync(path.join(root, 'geo', name + '.geo.json'), JSON.stringify(model, null, 2) + '\n');
fs.writeFileSync(path.join(root, 'textures', 'entity', 'scarlet_blood.png'), bloodTexture());
console.log('wrote scarlet_spike, scarlet_rod, scarlet_shard, scarlet_bloom, scarlet_blood, scarlet_puddle and the blood texture');
