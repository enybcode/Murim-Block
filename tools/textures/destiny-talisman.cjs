// Paints the Destiny Talisman (original pixel art, 16x16, board docs/items/boards/destiny-talisman.png, B chosen by
// Enzo) into textures/item: a white "mutton-fat" jade bi disc lit from the top left, a carved groove ring, a gold ring
// around the open hole, a red cord knot on top.
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const W = 16;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const JADE = ['#fffdf4', '#f1ead6', '#d9cfb2', '#b3a585', '#7f7258'].map(hex), LINE = hex('#3a3022');
const GOLD = ['#fff0a8', '#f3b83e', '#b0742a'].map(hex);
const CORD = ['#e0483a', '#a3263a', '#6a1730'].map(hex);
const g = Array.from({ length: W }, () => Array(W).fill(null));

const cx = 8.0, cy = 9.5, R = 5.9;
for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    const dx = x + 0.5 - cx, dy = y + 0.5 - cy, r = Math.hypot(dx, dy);
    if (r > R || r < 1.45) continue;
    const light = -(dx + dy) / Math.max(r, 0.01) * Math.min(1, r / 3);
    if (r < 2.5) {
        g[y][x] = light > 0.3 ? GOLD[0] : light > -0.3 ? GOLD[1] : GOLD[2];
        continue;
    }
    let i = light > 0.55 ? 1 : light > -0.25 ? 2 : light > -0.75 ? 3 : 4;
    if (r > 3.7 && r < 4.4) i = Math.min(4, i + 1);
    g[y][x] = JADE[i];
}
for (const [x, y] of [[5, 6], [6, 5], [4, 8]]) g[y][x] = JADE[0];
// A dark outline around every painted pixel, like vanilla items (the hole gets its dark inside too).
const line = [];
for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    if (!g[y][x] && [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => g[y + dy]?.[x + dx])) line.push([x, y]);
}
for (const [x, y] of line) g[y][x] = LINE;
for (const [x, y, c] of [[8, 3, CORD[1]], [8, 2, CORD[0]], [7, 1, CORD[0]], [9, 1, CORD[1]], [8, 1, CORD[2]], [7, 0, CORD[1]],
    [9, 0, CORD[2]]]) g[y][x] = c;

const px = Buffer.alloc(W * W * 4);
g.forEach((row, y) => row.forEach((c, x) => { if (c) px.set(c, (y * W + x) * 4); }));
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
