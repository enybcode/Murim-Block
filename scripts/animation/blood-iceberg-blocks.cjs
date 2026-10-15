// The Blood Iceberg's two blocks (BloodIceberg, SanguineBlocks): blood crystal, and blood crystal veined with glowing blood.
// 16 x 16 pixel art in the art's palette: large cut facets, bright ridges, glints; the veined one crossed by a bright
// vein of blood (it gives light 9). Writes their textures, block models and blockstates.
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

const wrap = (d) => Math.min(Math.abs(d), 16 - Math.abs(d));
/**
 * Cut gem (Enzo's choice among four, 15/10/2026): a few large facets, each one shade lit from the top left, bright ridges
 * between them, a glint on some; the veined one crossed by a bright vein of blood.
 */
function gem(x, y, seed) {
  const pts = []; for (let k = 0; k < 6; k++) pts.push([hash(k, 1, seed) * 16, hash(k, 2, seed) * 16, k]);
  let a = null, d0 = 99, d1 = 99;
  for (const q of pts) { const d = Math.hypot(wrap(x + .5 - q[0]), wrap(y + .5 - q[1])); if (d < d0) { d1 = d0; d0 = d; a = q; } else if (d < d1) d1 = d; }
  const shades = [0x5c0a18, 0x8a1226, 0x6e0d1e, 0xa8182f, 0x4a0814, 0x7a1022];
  if (d1 - d0 < .8) return 0xe0506a;
  const lit = ((x + .5 - a[0]) * -.6 + (y + .5 - a[1]) * -.8) / 8;
  let c = mix(shades[a[2]], 0xd63a52, .25 + lit * .5);
  if (d0 < .9 && a[2] % 3 === 0) c = 0xffc2cc;
  return c;
}
const tex = path.join(root, 'textures/block'), models = path.join(root, 'models/block'), states = path.join(root, 'blockstates');
for (const d of [tex, models, states]) fs.mkdirSync(d, { recursive: true });
const blocks = {
  blood_crystal_block: (x, y) => gem(x, y, 3),
  blood_crystal_vein: (x, y) => Math.abs(y - (7 + 3 * Math.sin(x * .7))) < .8 ? mix(P.bright, P.glint, hash(x, y, 2)) : gem(x, y, 11),
};
for (const [name, pixel] of Object.entries(blocks)) {
  fs.writeFileSync(path.join(tex, name + '.png'), png(16, 16, pixel));
  fs.writeFileSync(path.join(models, name + '.json'), JSON.stringify({ parent: 'minecraft:block/cube_all', textures: { all: 'murimblock:block/' + name } }, null, 2) + '\n');
  fs.writeFileSync(path.join(states, name + '.json'), JSON.stringify({ variants: { '': { model: 'murimblock:block/' + name } } }, null, 2) + '\n');
  console.log(name);
}
