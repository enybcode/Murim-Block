// Builds the Old Master (original model): GeckoLib geometry, idle animation and 128x64 texture, painted per face.
// A white-haired master seated in the lotus on a crimson cushion, hands joined on the lap, eyes closed.
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const ASSETS = path.join(__dirname, '../src/main/resources/assets/murimblock');
const TW = 128, TH = 64;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const C = {
    robe: hex('#3d4b6c'), robeLight: hex('#536590'), robeDark: hex('#2a3450'), inner: hex('#e9e4d6'),
    sash: hex('#b38b3f'), sashDark: hex('#8a6a2c'), cushion: hex('#8c2b2b'), cushionDark: hex('#6a1e22'),
    trim: hex('#d6a843'), skin: hex('#d8ae88'), skinShade: hex('#b88e6a'), lid: hex('#6e4f3c'),
    hair: hex('#efefea'), hairShade: hex('#c8c8cc'), jade: hex('#3fa47d'), jadeDark: hex('#2b7a5b'),
};

// Faces: n (front, toward -z), s (back), e, w (sides), u (top), d (bottom). fx, fy: pixel in the face, fy 0 at the top.
const robeCloth = (f, fx, fy, w, h) => f === 'u' ? C.robeLight : f === 'd' ? C.robeDark
    : (fx + fy * 3) % 7 === 0 ? C.robeLight : fy === h - 1 ? C.robeDark : C.robe;
const paint = {
    cushion: (f, fx, fy, w, h) => f === 'u' ? ((fx === 0 || fy === 0 || fx === w - 1 || fy === h - 1) ? C.trim : C.cushion)
        : f === 'd' ? C.cushionDark : fy === 1 ? C.trim : fy === 2 ? C.cushionDark : C.cushion,
    lap: (f, fx, fy, w, h) => f === 'n' && (fx === 3 || fx === 10) && fy > 0 ? C.robeDark : robeCloth(f, fx, fy, w, h),
    torso: (f, fx, fy, w, h) => {
        if (fy >= 6 && fy <= 7) return fy === 7 ? C.sashDark : C.sash;
        if (f === 'n') {
            // Crossed collar: a white V from the shoulders to the sash, the left side over the right.
            const d = Math.abs(fx - 3.5);
            if (fy < 6 && d >= 3.5 - fy * 0.6 - 0.5 && d <= 3.5 - fy * 0.6 + 1.1) return C.inner;
        }
        return robeCloth(f, fx, fy, w, h);
    },
    sleeve: (f, fx, fy, w, h) => fy === h - 1 && f !== 'u' ? C.inner : robeCloth(f, fx, fy, w, h),
    forearm: (f, fx, fy, w, h) => f === 'n' ? (fx === 0 || fx === w - 1 ? C.robeDark : C.inner) : robeCloth(f, fx, fy, w, h),
    hands: (f, fx, fy) => f === 'u' && fx % 3 === 2 ? C.skinShade : fy === 0 && f !== 'u' ? C.skinShade : C.skin,
    head: (f, fx, fy) => {
        if (f === 'u') return (fx + fy) % 5 === 0 ? C.hairShade : C.hair;
        if (f === 'd') return C.skinShade;
        if (f === 's') return fy === 7 ? C.hairShade : C.hair;
        if (f === 'e' || f === 'w') {
            if (fy < 3 || fx >= 6) return fx % 3 === 0 ? C.hairShade : C.hair;
            return fx === 4 && fy >= 3 && fy <= 5 ? C.skinShade : C.skin;
        }
        // Front: hairline, long white brows, closed eyes, nose, a mustache falling into the beard.
        if (fy === 0) return C.hair;
        if (fy === 1) return fx === 0 || fx === 7 ? C.hair : fx === 3 || fx === 4 ? C.skinShade : C.skin;
        if (fy === 2) return fx <= 2 || fx >= 5 ? C.hair : C.skin;
        if (fy === 3) return fx === 0 || fx === 7 ? C.hair : fx === 1 || fx === 2 || fx === 5 || fx === 6 ? C.lid : C.skin;
        if (fy === 4) return fx === 3 || fx === 4 ? C.skinShade : fx === 0 || fx === 7 ? C.skinShade : C.skin;
        if (fy === 5) return fx >= 1 && fx <= 6 ? (fx === 3 || fx === 4 ? C.hairShade : C.hair) : C.skin;
        return fx >= 1 && fx <= 6 ? C.hair : C.skin;
    },
    bun: (f, fx, fy) => (fx + fy) % 3 === 0 ? C.hairShade : C.hair,
    pin: (f, fx) => fx === 0 || fx === 7 ? C.jadeDark : C.jade,
    beard: (f, fx, fy, w, h) => f === 'n' && (fx + fy) % 4 === 1 ? C.hairShade : fy === h - 1 ? C.hairShade : C.hair,
};

// Bones and their cubes (model pixels, y up, front toward -z). The head bone turns toward the player.
const bones = [
    { name: 'root', pivot: [0, 0, 0] },
    { name: 'float', parent: 'root', pivot: [0, 0, 0] },
    { name: 'cushion', parent: 'float', pivot: [0, 0, 0], cubes: [['cushion', [-7, 0, -7], [14, 3, 14]]] },
    { name: 'lap', parent: 'float', pivot: [0, 3, 0], cubes: [['lap', [-7, 3, -6], [14, 4, 9]]] },
    {
        name: 'body', parent: 'float', pivot: [0, 7, 0.5], cubes: [
            ['torso', [-4, 7, -2], [8, 10, 5]],
            ['sleeve', [4, 9, -2], [3, 8, 5]], ['sleeve', [-7, 9, -2], [3, 8, 5]],
            ['forearm', [2, 7, -6], [4, 3, 5]], ['forearm', [-6, 7, -6], [4, 3, 5]],
            ['hands', [-2, 7, -6], [4, 2, 3]],
        ]
    },
    {
        name: 'head', parent: 'body', pivot: [0, 17, 0.5], cubes: [
            ['head', [-4, 17, -3.5], [8, 8, 8]], ['bun', [-2, 25, -1.5], [4, 3, 4]], ['pin', [-4, 26, 0], [8, 1, 1]],
        ]
    },
    { name: 'beard', parent: 'head', pivot: [0, 17, -3.5], cubes: [['beard', [-2, 11, -3.5], [4, 6, 1]]] },
];

// Box UVs packed on shelves; the same layout as vanilla boxes (top row: up, down; then e, n, w, s).
const px = Buffer.alloc(TW * TH * 4);
let shelfX = 0, shelfY = 0, shelfH = 0;
const put = (x, y, c) => px.set(c, (y * TW + x) * 4);
for (const bone of bones) {
    if (!bone.cubes) continue;
    bone.cubes = bone.cubes.map(([kind, origin, size]) => {
        const [w, h, d] = size, uw = 2 * (w + d), uh = d + h;
        if (shelfX + uw > TW) { shelfX = 0; shelfY += shelfH; shelfH = 0; }
        if (shelfY + uh > TH) throw new Error('texture full at ' + kind);
        const u = shelfX, v = shelfY;
        shelfX += uw; shelfH = Math.max(shelfH, uh);
        const face = (f, fu, fv, fw, fh) => {
            for (let y = 0; y < fh; y++) for (let x = 0; x < fw; x++) put(fu + x, fv + y, paint[kind](f, x, y, fw, fh));
        };
        face('u', u + d, v, w, d);
        face('d', u + d + w, v, w, d);
        face('e', u, v + d, d, h);
        face('n', u + d, v + d, w, h);
        face('w', u + d + w, v + d, d, h);
        face('s', u + 2 * d + w, v + d, w, h);
        return { origin, size, uv: [u, v] };
    });
}

const geo = {
    format_version: '1.12.0',
    'minecraft:geometry': [{
        description: {
            identifier: 'geometry.old_master', texture_width: TW, texture_height: TH,
            visible_bounds_width: 2, visible_bounds_height: 3, visible_bounds_offset: [0, 1.5, 0],
        },
        bones,
    }],
};
const wave = (amp, speed, base = 0) => `${base} + math.sin(query.anim_time * ${speed}) * ${amp}`;
const animation = {
    format_version: '1.8.0',
    animations: {
        'animation.old_master.idle': {
            loop: true, animation_length: 8,
            bones: {
                // Floats a little above the ground, breathes slowly, the beard stirs.
                float: { position: [0, wave(1.2, 45, 3), 0] },
                body: { scale: [wave(0.015, 90, 1), wave(0.02, 90, 1), wave(0.015, 90, 1)] },
                beard: { rotation: [wave(4, 60), 0, wave(2, 40)] },
            },
        },
    },
};

const raw = Buffer.alloc((TW * 4 + 1) * TH);
for (let y = 0; y < TH; y++) px.copy(raw, y * (TW * 4 + 1) + 1, y * TW * 4, (y + 1) * TW * 4);
const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = b => { let c = 0xffffffff; for (const x of b) c = crcTable[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(TW, 0); ihdr.writeUInt32BE(TH, 4); ihdr[8] = 8; ihdr[9] = 6;
const png = Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);

const write = (rel, data) => {
    const out = path.join(ASSETS, rel);
    fs.mkdirSync(path.dirname(out), { recursive: true });
    fs.writeFileSync(out, data);
    console.log('wrote', out);
};
write('geo/entity/old_master.geo.json', JSON.stringify(geo, null, 2) + '\n');
write('animations/entity/old_master.animation.json', JSON.stringify(animation, null, 2) + '\n');
write('textures/entity/old_master.png', png);
