// The Traveller's Staff Scroll's item sprite (TravellerStaffItems): a road traveller's scroll, unrolled between two dark
// wooden rods with bronze knobs, its paper aged, a staff drawn across it in ink with its red cord, the art's mark (a road
// sign) in the corner; made at 16 px in the arts' manuals' pixel style (celestial-manual.cjs).
//   node scripts/animation/traveller-scroll.cjs
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
const PAPER = 0xe9d9b0, PAPER_SHADE = 0xcdb886, PAPER_EDGE = 0xa98e5c;
const ROD = 0x4a2e1a, ROD_LIGHT = 0x6e4628, KNOB = 0xc08a3e, KNOB_DARK = 0x8a5f28;
const INK = 0x2b2420, INK_SOFT = 0x6b5a48, CORD = 0xc0302a, MARK = 0x9a3a22;

const img = Array.from({ length: 16 }, () => Array(16).fill(null));
// The paper: x 3..12, y 3..12, shaded along its edges, a little wavy at the sides.
for (let y = 3; y <= 12; y++) for (let x = 3; x <= 12; x++) {
  const edge = x === 3 || x === 12;
  img[y][x] = edge ? PAPER_EDGE : (y === 3 || y === 12 || (x + y) % 9 === 0) ? PAPER_SHADE : PAPER;
}
// The rods across the top and bottom, past the paper, bronze knobs at their ends.
for (const y of [2, 13]) {
  for (let x = 2; x <= 13; x++) img[y][x] = x % 3 === 0 ? ROD_LIGHT : ROD;
  img[y][1] = KNOB; img[y][14] = KNOB_DARK;
}
// The staff in ink, from low left to high right, its two shoes darker, the red cord a third along.
const line = [[4, 11], [5, 10], [6, 10], [7, 9], [8, 8], [9, 7], [10, 6], [11, 5]];
for (const [x, y] of line) img[y][x] = INK_SOFT;
img[11][4] = INK; img[5][11] = INK; img[9][7] = CORD; img[10][6] = CORD;
// The art's mark, a little red seal in the top left corner.
img[4][4] = MARK; img[4][5] = MARK; img[5][4] = MARK;
// A line of writing down the right side.
for (const y of [8, 10]) img[y][11] = INK_SOFT;

const tex = path.join(root, 'textures/item'), models = path.join(root, 'models/item');
fs.writeFileSync(path.join(tex, 'traveller_staff_scroll.png'), png(16, 16, (x, y) => img[y][x]));
fs.writeFileSync(path.join(models, 'traveller_staff_scroll.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/traveller_staff_scroll' } }) + '\n');
console.log('traveller_staff_scroll');
