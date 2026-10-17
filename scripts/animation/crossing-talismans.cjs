// The crossing talismans' item sprites (CrossingTalismanItem): a strip of paper talisman (fu) hanging upright, a seal
// at its head, characters brushed down it, a tassel below. The Nether's on red paper with gold ink, the End's on black
// paper with violet ink like stars, the Murim's on pale paper with jade ink.
//   node scripts/animation/crossing-talismans.cjs
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = c === null ? 0 : 255; }
  const crc = (buf) => { let c = ~0; for (const v of buf) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]), c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
// The strip: x 5..10, y 1..12; a seal (a square) near the top; three characters of ink below; a tassel at y 13..15.
const INK = [[7, 6], [8, 6], [6, 7], [9, 7], [7, 8], [8, 8], [7, 9], [8, 10], [6, 10], [9, 11], [7, 11]];
const TALISMANS = {
  nether_crossing_talisman: { paper: 0xc8302a, shade: 0x8a1a16, edge: 0x5a0e0a, ink: 0xffd860, seal: 0xffd860, tassel: 0xffb020 },
  end_crossing_talisman: { paper: 0x1e1828, shade: 0x120e18, edge: 0x07060a, ink: 0xc890ff, seal: 0xe8d0ff, tassel: 0x9a60e0 },
  murim_crossing_talisman: { paper: 0xf2e6c4, shade: 0xd8c898, edge: 0x8a7a50, ink: 0x1f8a62, seal: 0xd23a2a, tassel: 0x2fa878 },
};
for (const [name, c] of Object.entries(TALISMANS)) {
  const img = (x, y) => {
    if (x >= 5 && x <= 10 && y >= 1 && y <= 12) {
      if (x === 5 || x === 10 || y === 1 || y === 12) return c.edge;
      if (x >= 6 && x <= 9 && y >= 2 && y <= 4) return (x === 6 || x === 9 || y === 2 || y === 4) ? c.seal : c.paper; // the seal
      if (INK.some(([ix, iy]) => ix === x && iy === y)) return c.ink;
      return x === 9 ? c.shade : c.paper;
    }
    if ((x === 7 || x === 8) && y === 13) return c.edge;                      // the knot
    if (y >= 14 && y <= 15 && (x === 6 || x === 7 || x === 8 || x === 9)) return (x + y) % 2 ? c.tassel : c.edge; // the tassel
    return null;
  };
  fs.writeFileSync(path.join(root, 'textures/item', name + '.png'), png(16, 16, img));
  fs.writeFileSync(path.join(root, 'models/item', name + '.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/' + name } }) + '\n');
  console.log(name);
}
