// Paints the Destiny Talisman (original pixel art, 16x16) into textures/item: an aged yellow paper talisman, the
// character 命 ("fate") in red ink between two red rules, a row of the eight ores it is made of under it, quartz at
// the top, and a red tassel hanging from the bottom.
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const W = 16;
const hex = (h, a = 255) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), a];
const C = {
    outline: hex('#4a3317'), paper: hex('#e8c66a'), light: hex('#f6dd8c'), dark: hex('#c9a24b'), aged: hex('#d8b25a'),
    ink: hex('#b3201c'), inkDark: hex('#7c1312'), tassel: hex('#c8302a'), tasselDark: hex('#86201c'),
    quartz: hex('#efe9e2'),
};
const ORES = ['#2b2b2b', '#c8774a', '#d8d2cc', '#f4c63d', '#e0281e', '#2f56c4', '#5ce8e0', '#2fcf62'].map(h => hex(h));
const hash = (x, y) => { let h = x * 374761393 + y * 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967296; };
const px = Buffer.alloc(W * W * 4);
const put = (x, y, c) => px.set(c, (y * W + x) * 4);

// The paper: light left edge, shaded right edge, a few aged specks; a dark outline around it.
for (let y = 0; y <= 14; y++) for (let x = 3; x <= 12; x++) {
    const edge = x === 3 || x === 12 || y === 0 || y === 14;
    put(x, y, edge ? C.outline : x === 4 ? C.light : x === 11 ? C.dark : hash(x, y) < 0.18 ? C.aged : C.paper);
}
// Two red rules framing the character.
for (let x = 5; x <= 10; x++) {
    put(x, 2, C.ink);
    put(x, 11, C.inkDark);
}
put(7, 1, C.quartz);
put(8, 1, C.quartz);
// 命: a roof, its eaves, a bar, then the mouth on the left and the seal stroke on the right.
const glyph = [
    [7, 4], [8, 4], [6, 5], [9, 5], [5, 6], [10, 6], [6, 7], [7, 7], [8, 7], [9, 7],
    [6, 8], [8, 8], [9, 8], [6, 9], [7, 9], [9, 9], [9, 10],
];
for (const [x, y] of glyph) put(x, y, C.ink);
put(10, 7, C.inkDark);
put(8, 9, C.inkDark);
// The ores it was made of, in a row under the lower rule.
ORES.forEach((c, i) => put(4 + i, 12, c));
// The tassel.
put(7, 14, C.tassel);
put(8, 14, C.tasselDark);
put(7, 15, C.tasselDark);
put(8, 15, C.tassel);

const raw = Buffer.alloc((W * 4 + 1) * W);
for (let y = 0; y < W; y++) px.copy(raw, y * (W * 4 + 1) + 1, y * W * 4, (y + 1) * W * 4);
const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = b => { let c = 0xffffffff; for (const x of b) c = crcTable[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(W, 4); ihdr[8] = 8; ihdr[9] = 6;
const png = Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
const out = path.join(__dirname, '../../src/main/resources/assets/murimblock/textures/item/destiny_talisman.png');
fs.writeFileSync(out, png);
console.log('wrote', out);
