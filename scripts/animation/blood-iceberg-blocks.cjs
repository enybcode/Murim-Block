// The Blood Iceberg's blood temple (BloodIceberg, SanguineBlocks), in the "blood temple" style Enzo chose among four sets
// (15/10/2026): red lacquered stone and gold. Six blocks, 16 x 16 vanilla-like pixel art: rough stone, bricks, polished
// stone, the gold seal, gold crystal, a lantern; and blood, a liquid, its still and flowing textures animated. Writes the
// textures, block models and blockstates.
//   node scripts/animation/blood-iceberg-blocks.cjs
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = c > 0xffffff ? (c >>> 24) & 255 : 255; }
  const crc = (buf) => { let c = ~0; for (const v of buf) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]), c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hash = (x, y, s) => { let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967295; };
const sm = (t) => t * t * (3 - 2 * t);
/** Tileable value noise over a `size` square, `cells` cells across. */
function noise(x, y, cells, seed, size = 16) {
  const f = size / cells, gx = x / f, gy = y / f, x0 = Math.floor(gx), y0 = Math.floor(gy), tx = sm(gx - x0), ty = sm(gy - y0);
  const g = (i, j) => hash(((i % cells) + cells) % cells, ((j % cells) + cells) % cells, seed);
  const a = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * tx, b = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * tx;
  return a + (b - a) * ty;
}
const fbm = (x, y, seed) => noise(x, y, 4, seed) * .55 + noise(x, y, 8, seed + 1) * .3 + hash(x, y, seed + 2) * .15;
const ramp = (r, t) => r[Math.max(0, Math.min(r.length - 1, Math.floor(t * r.length)))];

// Red lacquered stone, gold, the lanterns' light.
const STONE = [0x2a0808, 0x420e0c, 0x5c1612, 0x761e18, 0x8e2a20, 0xa8402c];
const GOLD = [0x6a4a12, 0x8a6a20, 0xb08a2a, 0xd0aa3a, 0xf0d070, 0xfff0b0];
const GLOW = [0xd06020, 0xf0902c, 0xffb848, 0xffd890, 0xfff2d0];
const BLOCKS = {
  // Rough: the base, the slope, under everything.
  blood_temple_rock: (x, y) => ramp(STONE, fbm(x, y, 1) * .95),
  // Bricks, two courses a block, joints dark, a lit top edge: the tiers, the wall, the channel.
  blood_temple_bricks: (x, y) => {
    const course = y >> 3, by = y & 7, bx = (x + (course ? 8 : 0)) & 15;
    if (by === 7 || bx === 15) return STONE[0];
    return ramp(STONE, .35 + fbm(x, y, 7) * .45 + (by === 0 ? .25 : 0) - (by === 6 ? .15 : 0));
  },
  // Polished, framed by a bevel: the floor, the wall's top.
  blood_temple_polished: (x, y) => {
    if (x === 0 || y === 0) return STONE[4];
    if (x === 15 || y === 15) return STONE[0];
    if (x === 1 || y === 1) return STONE[3];
    return ramp(STONE, .4 + noise(x, y, 2, 11) * .25);
  },
  // The seal: a ring and a heart of gold inlaid in the polished stone, spokes to the edges.
  blood_temple_seal: (x, y) => {
    const d = Math.hypot(x - 7.5, y - 7.5);
    if (Math.abs(d - 5.6) < .75) return ramp(GOLD, .55 + (x + y < 15 ? .3 : 0));
    if (d < 2.2) return ramp(GOLD, d < 1.2 ? .95 : .6);
    if ((x === 7 || x === 8) && (y < 3 || y > 12) || (y === 7 || y === 8) && (x < 3 || x > 12)) return ramp(GOLD, .5);
    if (x === 0 || y === 0) return STONE[4];
    if (x === 15 || y === 15) return STONE[0];
    return ramp(STONE, .3 + noise(x, y, 2, 13) * .2);
  },
  // Gold crystal in clustered facets: the spires.
  blood_temple_gold: (x, y) => {
    const a = noise(x, y, 4, 21), b = noise(x + 3, y - 2, 4, 23);
    let t = .25 + a * .55 + (b > .62 ? .2 : 0);
    if (Math.abs(((x + y * .6) % 5.3) - 2.6) < .35 && a > .45) t = .95;
    return ramp(GOLD, t);
  },
  // A lantern: light behind a lattice in a red frame (light 15): the spires' tips, set in the wall.
  blood_temple_lantern: (x, y) => {
    if (x === 0 || y === 0 || x === 15 || y === 15) return STONE[1];
    if (x === 1 || y === 1 || x === 14 || y === 14) return STONE[2];
    if (x === 5 || x === 10 || y === 5 || y === 10) return STONE[1];
    return ramp(GLOW, .55 + (1 - Math.hypot(x - 7.5, y - 7.5) / 8) * .45 + hash(x, y, 31) * .08);
  },
};

// Blood: deep red, darker clots drifting, a few bright glints; a little see-through.
const BLOOD = [0x3a0208, 0x56040e, 0x720814, 0x8e0e1c, 0xae1a28, 0xd23a46];
const blood = (x, y, frame, frames, size) => {
  const t = frame / frames, shift = t * size;
  const n = noise(x, y - shift, 4, 41, size) * .6 + noise(x + shift * .5, y, 8, 43, size) * .4;
  let c = ramp(BLOOD, .15 + n * .8);
  if (hash(x, Math.floor(y - shift + size * 4) % size, 47) > .985) c = 0xf07080;
  return (0xe6000000 | c) >>> 0;
};
function animated(name, size, frames, frametime) {
  const tex = path.join(root, 'textures/block');
  fs.writeFileSync(path.join(tex, name + '.png'), png(size, size * frames, (x, y) => blood(x, y % size, Math.floor(y / size), frames, size)));
  fs.writeFileSync(path.join(tex, name + '.png.mcmeta'), JSON.stringify({ animation: { frametime, interpolate: true } }, null, 2) + '\n');
}

const tex = path.join(root, 'textures/block'), models = path.join(root, 'models/block'), states = path.join(root, 'blockstates');
for (const d of [tex, models, states]) fs.mkdirSync(d, { recursive: true });
for (const [name, pixel] of Object.entries(BLOCKS)) {
  fs.writeFileSync(path.join(tex, name + '.png'), png(16, 16, pixel));
  fs.writeFileSync(path.join(models, name + '.json'), JSON.stringify({ parent: 'minecraft:block/cube_all', textures: { all: 'murimblock:block/' + name } }, null, 2) + '\n');
  fs.writeFileSync(path.join(states, name + '.json'), JSON.stringify({ variants: { '': { model: 'murimblock:block/' + name } } }, null, 2) + '\n');
  console.log(name);
}
animated('blood_still', 16, 20, 3);
animated('blood_flow', 32, 20, 2);
fs.writeFileSync(path.join(models, 'blood.json'), JSON.stringify({ textures: { particle: 'murimblock:block/blood_still' } }, null, 2) + '\n');
fs.writeFileSync(path.join(states, 'blood.json'), JSON.stringify({ variants: { '': { model: 'murimblock:block/blood' } } }, null, 2) + '\n');
console.log('blood (still, flowing)');
