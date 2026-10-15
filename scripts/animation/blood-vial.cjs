// The blood vial's item sprite (BloodVialItem): a vanilla-shaped bottle, a cork, dark blood with a lit side and a glint.
//   node scripts/animation/blood-vial.cjs
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
// Rows top to bottom: . empty, o outline glass, k cork, K cork dark, g glass highlight, b blood, d blood dark, l blood lit, w glint.
const ART = [
  '................',
  '......oooo......',
  '......kKKk......',
  '......oKKo......',
  '......o..o......',
  '.....oogwoo.....',
  '....ogbllbbo....',
  '...ogbllbbbdo...',
  '...oglbbbbbdo...',
  '...oglbbbbbdo...',
  '...oglbbbbddo...',
  '...ogbbbbbddo...',
  '....obbbbddo....',
  '.....oddddo.....',
  '......oooo......',
  '................',
];
const C = { o: 0x2a1418, k: 0x9a6a3a, K: 0x6e4422, g: 0xe8d8dc, b: 0x9a0e1e, d: 0x5c0610, l: 0xd23a46, w: 0xffffff };
const tex = path.join(root, 'textures/item'), models = path.join(root, 'models/item');
fs.mkdirSync(tex, { recursive: true }); fs.mkdirSync(models, { recursive: true });
fs.writeFileSync(path.join(tex, 'blood_vial.png'), png(16, 16, (x, y) => { const ch = ART[y][x]; return ch === '.' ? null : C[ch]; }));
fs.writeFileSync(path.join(models, 'blood_vial.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/blood_vial' } }) + '\n');
console.log('blood_vial');
