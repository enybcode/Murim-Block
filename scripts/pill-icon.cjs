// Draws the Meridian-Opening Pill icon (16x16, original pixel art) into textures/item.
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const W = 16, px = Buffer.alloc(W * W * 4);
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const put = (x, y, c) => { const i = (y * W + x) * 4; px.set(c, i); };
const OUTLINE = hex('#163f4a'), DARK = hex('#2a7f86'), BASE = hex('#46b9b4'), LIGHT = hex('#8ee8dc'),
    SHINE = hex('#f2fff9'), GOLD = hex('#e8c25a'), GOLD_DARK = hex('#b07f2a');
const cx = 7.5, cy = 8, r = 5.6;
for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    const d = Math.hypot(x - cx, y - cy);
    if (d > r + 0.9) continue;
    if (d > r) { put(x, y, OUTLINE); continue; }
    // Light from the top left: shade by the offset along that diagonal.
    const t = ((x - cx) + (y - cy)) / (2 * r);
    put(x, y, t < -0.35 ? LIGHT : t > 0.3 ? DARK : BASE);
}
// A gold band of Qi around the pill, and a shine.
for (const [x, y] of [[3, 9], [4, 10], [5, 10], [6, 11], [7, 11], [8, 11], [9, 10], [10, 10], [11, 9], [12, 8]]) put(x, y, GOLD);
for (const [x, y] of [[4, 11], [8, 12], [12, 9]]) put(x, y, GOLD_DARK);
for (const [x, y] of [[5, 5], [6, 5], [5, 6]]) put(x, y, SHINE);
const raw = Buffer.alloc((W * 4 + 1) * W);
for (let y = 0; y < W; y++) px.copy(raw, y * (W * 4 + 1) + 1, y * W * 4, (y + 1) * W * 4);
const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = b => { let c = 0xffffffff; for (const x of b) c = crcTable[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(W, 4); ihdr[8] = 8; ihdr[9] = 6;
const png = Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
const out = path.join(__dirname, '../src/main/resources/assets/murimblock/textures/item/meridian_opening_pill.png');
fs.writeFileSync(out, png);
console.log('wrote', out);
