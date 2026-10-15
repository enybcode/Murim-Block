// The Celestial Peak Spear Manual's item sprite (CelestialPeakItems): a military booklet bound in azure, gold corners,
// the three peaks of the Alliance stamped in gold on its cover, a red silk marker hanging out.
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
// . empty, o outline, b azure cover, B azure dark, p pages, P page shade, g gold, G gold dark, r red marker.
const ART = [
  '................',
  '..oooooooooooo..',
  '..ogbbbbbbbbgpo.',
  '..obbbbbbbbbbpo.',
  '..obbbbbgbbbbpo.',
  '..obbbbggbbbbpo.',
  '..obbgbgggbgbpo.',
  '..obbgggggggbpo.',
  '..obgggggggggpo.',
  '..obbbbbbbbbbpo.',
  '..obBBBBBBBBBpo.',
  '..obbbbbbbbbbpo.',
  '..ogbbbbbbbbgPo.',
  '..oooooooorooo..',
  '.........rr.....',
  '.........r......',
];
const C = { o: 0x101a2e, b: 0x2b5fa8, B: 0x1f4682, p: 0xeee6d2, P: 0xc8bea6, g: 0xd9b25f, G: 0xa8842e, r: 0xc0302a };
const tex = path.join(root, 'textures/item'), models = path.join(root, 'models/item');
fs.writeFileSync(path.join(tex, 'celestial_peak_spear_manual.png'), png(16, 16, (x, y) => { const ch = ART[y][x]; return ch === '.' ? null : C[ch]; }));
fs.writeFileSync(path.join(models, 'celestial_peak_spear_manual.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/celestial_peak_spear_manual' } }) + '\n');
console.log('celestial_peak_spear_manual');
