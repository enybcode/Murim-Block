// The Blood Iceberg's two blocks (BloodIceberg, SanguineBlocks): blood crystal, and blood crystal veined with glowing blood.
// 16 x 16 pixel art in the art's palette: dark crimson facets cut on the diagonal, a lit edge on each, black flaws; the
// veined one crossed by bright red veins (it gives light 9). Writes their textures, block models and blockstates.
//   node scripts/animation/blood-iceberg-blocks.cjs
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = 255; }
  const crc = (buf) => { let c = ~0; for (const v of buf) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]), c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hash = (x, y, s) => { let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967295; };
const mix = (a, b, t) => { const ch = (k) => Math.round(((a >>> k) & 255) * (1 - t) + ((b >>> k) & 255) * t); return (ch(16) << 16) | (ch(8) << 8) | ch(0); };
const P = { flaw: 0x140105, dark: 0x2e040b, deep: 0x5c0a18, red: 0x9b1429, bright: 0xd63a52, glint: 0xff9aa8 };

/**
 * Crystal facets: a few large cells (a Voronoi of seed points, wrapping so the block tiles), each cut flat in one shade
 * and lighter toward its heart like a gem, dark seams between them, a glint on the brightest.
 */
function crystal(x, y, seed) {
  const pts = [];
  for (let k = 0; k < 7; k++) pts.push([hash(k, 1, seed) * 16, hash(k, 2, seed) * 16, [P.deep, P.red, P.deep, P.dark, P.red, P.deep, P.bright][k]]);
  const dist = (px, py, q) => { const dx = Math.min(Math.abs(px - q[0]), 16 - Math.abs(px - q[0])), dy = Math.min(Math.abs(py - q[1]), 16 - Math.abs(py - q[1])); return Math.hypot(dx, dy); };
  const cell = (px, py) => { let best = 0, d0 = 1e9, d1 = 1e9; pts.forEach((q, i) => { const d = dist(px + .5, py + .5, q); if (d < d0) { d1 = d0; d0 = d; best = i; } else if (d < d1) d1 = d; }); return { best, d0, d1 }; };
  const c = cell(x, y);
  // A seam where two cells meet.
  if (c.d1 - c.d0 < .9) return mix(P.flaw, P.dark, .5);
  const base = pts[c.best][2];
  let col = mix(base, P.glint, Math.max(0, .35 - c.d0 * .07));
  if (c.d0 < .8 && (base === P.bright || base === P.red)) col = P.glint;
  return mix(col, P.dark, hash(x, y, seed) * .12);
}
const vein = (x, y) => Math.abs(y - (7 + 3 * Math.sin(x * .7))) < .9 || Math.abs(x - (5 + 2 * Math.sin(y * .9))) < .7 && y > 6;

const tex = path.join(root, 'textures/block'), models = path.join(root, 'models/block'), states = path.join(root, 'blockstates');
for (const d of [tex, models, states]) fs.mkdirSync(d, { recursive: true });
const blocks = {
  blood_crystal_block: (x, y) => crystal(x, y, 3),
  blood_crystal_vein: (x, y) => vein(x, y) ? mix(P.bright, P.glint, hash(x, y, 9) * .5) : crystal(x, y, 11),
};
for (const [name, pixel] of Object.entries(blocks)) {
  fs.writeFileSync(path.join(tex, name + '.png'), png(16, 16, pixel));
  fs.writeFileSync(path.join(models, name + '.json'), JSON.stringify({ parent: 'minecraft:block/cube_all', textures: { all: 'murimblock:block/' + name } }, null, 2) + '\n');
  fs.writeFileSync(path.join(states, name + '.json'), JSON.stringify({ variants: { '': { model: 'murimblock:block/' + name } } }, null, 2) + '\n');
  console.log(name);
}
