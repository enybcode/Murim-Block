// Paints the Grave Warden (original pixel art, 64x64, the zombie model's layout) and its glowing eyes into
// textures/entity. A zombie gone wrong: grey-green rotting skin with sores, a torn shirt over bare ribs, ragged
// trousers; below row 32, the bone and flesh of its growths (see GraveWardenMutationLayer).
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const W = 64;
const hex = (h, a = 255) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), a];
const C = {
    skin: hex('#5d7559'), skinLight: hex('#71886a'), skinDark: hex('#455a42'), sore: hex('#7c3d3a'),
    soreDark: hex('#5a2626'), bone: hex('#d6cdb0'), boneDark: hex('#a99f80'), shirt: hex('#34394a'),
    shirtDark: hex('#262a37'), pants: hex('#45405a'), pantsDark: hex('#332e45'), socket: hex('#1a1414'),
    mouth: hex('#241515'), nail: hex('#3b3328'), eye: hex('#ff3b2f'), eyeCore: hex('#ffd0a0'),
};
const hash = (x, y, s) => { let h = x * 374761393 + y * 668265263 + s * 2147483647; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967296; };

function canvas() { return Buffer.alloc(W * W * 4); }
const put = (img, x, y, c) => img.set(c, (y * W + x) * 4);

// Box UV like vanilla: top and bottom on the first row, then east, north (front), west, south (back).
function box(img, u, v, w, h, d, paint) {
    const face = (f, fu, fv, fw, fh) => {
        for (let y = 0; y < fh; y++) for (let x = 0; x < fw; x++) {
            const c = paint(f, x, y, fw, fh);
            if (c) put(img, fu + x, fv + y, c);
        }
    };
    face('u', u + d, v, w, d);
    face('d', u + d + w, v, w, d);
    face('e', u, v + d, d, h);
    face('n', u + d, v + d, w, h);
    face('w', u + d + w, v + d, d, h);
    face('s', u + 2 * d + w, v + d, w, h);
}

// Rotting skin: blotches, a few sores.
const flesh = (salt) => (f, x, y) => {
    const n = hash(x, y, salt);
    return n < 0.06 ? C.sore : n < 0.1 ? C.soreDark : n < 0.3 ? C.skinDark : n > 0.9 ? C.skinLight : C.skin;
};

const base = canvas();
const eyes = canvas();

// Head: sunken eyes, a torn mouth with teeth, a swollen left cheek.
box(base, 0, 0, 8, 8, 8, (f, x, y) => {
    if (f === 'n') {
        if (y === 3 && (x === 1 || x === 2 || x === 5 || x === 6)) return C.socket;
        if (y === 4 && (x === 2 || x === 5)) return C.socket;
        if (y === 6 && x >= 2 && x <= 5) return x % 2 === 0 ? C.bone : C.mouth;
        if (y === 7 && x >= 3 && x <= 5) return C.mouth;
        if (x === 7 && y >= 4 && y <= 6) return C.sore;
    }
    if (f === 'u' && hash(x, y, 3) < 0.25) return C.skinDark;
    return flesh(1)(f, x, y);
});
// Eyes glow: one pixel each, a dim ring around.
put(eyes, 8 + 2, 8 + 3, C.eye); put(eyes, 8 + 5, 8 + 3, C.eye);
put(eyes, 8 + 2, 8 + 4, hex('#ff3b2f', 150)); put(eyes, 8 + 5, 8 + 4, hex('#ff3b2f', 150));
put(eyes, 8 + 1, 8 + 3, hex('#ff3b2f', 110)); put(eyes, 8 + 6, 8 + 3, hex('#ff3b2f', 110));

// Body: a torn shirt; the ribs show through on the right side of the chest.
box(base, 16, 16, 8, 12, 4, (f, x, y, w, h) => {
    const torn = hash(x, y, 7) < 0.18 || y > 9 && hash(x, y, 8) < 0.5;
    if (f === 'n' && x <= 3 && y >= 2 && y <= 7) return y % 2 === 0 ? C.bone : C.skinDark;
    if (torn || f === 'u' || f === 'd') return flesh(9)(f, x, y);
    return (x + y) % 5 === 0 ? C.shirtDark : C.shirt;
});
// Arms: torn sleeves at the shoulder, bare rotting arms, dark nails.
box(base, 40, 16, 4, 12, 4, (f, x, y, w, h) => {
    if (y < 3 && f !== 'd' && hash(x, y, 11) > 0.3) return C.shirt;
    // The hand: rotting skin, dark claws at the fingertips.
    if (f === 'd') return y === 0 || x === 0 && y === 3 ? C.nail : flesh(13)(f, x, y);
    if (y === h - 1 && f !== 'u' && x % 2 === 0) return C.nail;
    return flesh(12)(f, x, y);
});
// Legs: ragged trousers to the shin, then bare skin and bare feet.
box(base, 0, 16, 4, 12, 4, (f, x, y, w, h) => {
    const hem = 7 + Math.floor(hash(x, 0, 13) * 3);
    if (f === 'd') return C.skinDark;
    if (f !== 'u' && y >= hem) return flesh(14)(f, x, y);
    return (x * 3 + y) % 6 === 0 ? C.pantsDark : C.pants;
});

// Growths: bone spikes, a shoulder knot, a swollen forearm, a hunch.
box(base, 0, 32, 1, 4, 1, (f, x, y) => y === 0 ? C.boneDark : C.bone);
box(base, 8, 32, 2, 2, 2, (f, x, y) => (x + y) % 2 === 0 ? C.bone : C.boneDark);
box(base, 16, 32, 5, 5, 5, (f, x, y) => {
    const n = hash(x, y, 21);
    return n < 0.12 ? C.sore : n < 0.2 ? C.soreDark : n < 0.45 ? C.skinDark : C.skin;
});
box(base, 40, 32, 6, 3, 2, (f, x, y) => (f === 's' && y === 1 && x % 2 === 1) ? C.bone : flesh(31)(f, x, y));

function png(img) {
    const raw = Buffer.alloc((W * 4 + 1) * W);
    for (let y = 0; y < W; y++) img.copy(raw, y * (W * 4 + 1) + 1, y * W * 4, (y + 1) * W * 4);
    const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
    const crc = b => { let c = 0xffffffff; for (const x of b) c = crcTable[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
    const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
    const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(W, 4); ihdr[8] = 8; ihdr[9] = 6;
    return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

const out = path.join(__dirname, '../../src/main/resources/assets/murimblock/textures/entity');
fs.mkdirSync(out, { recursive: true });
fs.writeFileSync(path.join(out, 'grave_warden.png'), png(base));
fs.writeFileSync(path.join(out, 'grave_warden_eyes.png'), png(eyes));
console.log('wrote grave_warden.png and grave_warden_eyes.png');
