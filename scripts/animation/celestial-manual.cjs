// The Celestial Peak Spear Manual's item sprite (CelestialPeakItems), made like the Azure Cloud Sword Manual
// (scripts/worldgen/ManualTexture.java) so the arts' manuals read as one set: a bound book in the Alliance's azure, a navy
// spine with gold bands, a gold frame, the pages showing at the edge; on the cover the three snowy peaks of the Alliance
// flag, the middle one higher, over a spear laid across with its red tassel.
//   node scripts/animation/celestial-manual.cjs
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
const SPINE = 0x10213f, SPINE_LIGHT = 0x1d3766;
const COVER = 0x2b5fa8, COVER_DARK = 0x214c8a, COVER_LIGHT = 0x4a7cc4;
const GOLD = 0xe2b746, GOLD_DARK = 0xa7792a;
const PAGES = 0xede3c6, PAGES_SHADE = 0xc9bc97;
const SNOW = 0xeef3f8, SNOW_SHADE = 0x9fb6cf, ROCK = 0x173563;
const STEEL = 0xdfe5ea, TASSEL = 0xc0302a;

const img = Array.from({ length: 16 }, () => Array(16).fill(null));
// The book: x 2..13, y 1..14; the spine on the left (x 2..3), the pages at the right edge and the bottom.
for (let y = 1; y <= 14; y++) for (let x = 2; x <= 13; x++) {
  let c;
  if (x <= 3) c = x === 3 ? SPINE_LIGHT : SPINE;
  else if (x === 13 || y === 14) c = (y === 14 && x >= 5) || (x === 13 && y >= 2) ? ((x + y) % 2 === 0 ? PAGES : PAGES_SHADE) : COVER_DARK;
  else c = (x + y * 3) % 7 === 0 ? COVER_DARK : COVER;
  img[y][x] = c;
}
// The gold frame (x 5..11, y 2..12) and the corners; the spine's bands.
for (let x = 5; x <= 11; x++) { img[2][x] = GOLD; img[12][x] = GOLD_DARK; }
for (let y = 2; y <= 12; y++) { img[y][5] = GOLD; img[y][11] = GOLD_DARK; }
img[1][4] = GOLD; img[1][12] = GOLD; img[13][4] = GOLD_DARK; img[13][12] = GOLD_DARK;
for (const y of [3, 7, 11]) { img[y][2] = GOLD_DARK; img[y][3] = GOLD; }
// The three peaks, inside the frame (x 6..10): the middle one up to y 3, the side ones to y 5, snow on their tops.
const peaks = [
  [8, 3, SNOW], [7, 4, SNOW], [8, 4, SNOW], [9, 4, SNOW_SHADE],
  [6, 5, SNOW], [7, 5, ROCK], [8, 5, SNOW], [9, 5, ROCK], [10, 5, SNOW_SHADE],
  [6, 6, ROCK], [7, 6, ROCK], [8, 6, ROCK], [9, 6, ROCK], [10, 6, ROCK],
  [6, 7, ROCK], [7, 7, COVER_DARK], [8, 7, ROCK], [9, 7, COVER_DARK], [10, 7, ROCK],
];
for (const [x, y, c] of peaks) img[y][x] = c;
// The spear laid across under them: a gold shaft, its steel point on the right, the red tassel at its socket.
for (let x = 6; x <= 9; x++) img[9][x] = GOLD;
img[9][10] = STEEL; img[8][10] = COVER_LIGHT; img[10][9] = TASSEL; img[10][8] = TASSEL;
img[4][6] = COVER_LIGHT; img[10][7] = COVER_LIGHT;

const tex = path.join(root, 'textures/item'), models = path.join(root, 'models/item');
fs.writeFileSync(path.join(tex, 'celestial_peak_spear_manual.png'), png(16, 16, (x, y) => img[y][x]));
fs.writeFileSync(path.join(models, 'celestial_peak_spear_manual.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/celestial_peak_spear_manual' } }) + '\n');
console.log('celestial_peak_spear_manual');
