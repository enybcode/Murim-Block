// Builds the Old Master (original model): GeckoLib geometry, idle animation and 128x64 texture, painted per face.
// The ghost of a white-haired master: no legs, his robe thins into a tail of mist; hands joined in his sleeves, eyes
// closed. Drawn translucent and self-lit by OldMasterRenderer.
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const ASSETS = path.join(__dirname, '../src/main/resources/assets/murimblock');
const TW = 128, TH = 64;
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const C = {
    robe: hex('#3d4b6c'), robeLight: hex('#536590'), robeDark: hex('#2a3450'), inner: hex('#e9e4d6'),
    sash: hex('#b38b3f'), sashDark: hex('#8a6a2c'),
    mist: hex('#a9dcec'), skin: hex('#d8ae88'), skinShade: hex('#b88e6a'), lid: hex('#6e4f3c'),
    hair: hex('#efefea'), hairShade: hex('#c8c8cc'), jade: hex('#3fa47d'), jadeDark: hex('#2b7a5b'),
};

// Faces: n (front, toward -z), s (back), e, w (sides), u (top), d (bottom). fx, fy: pixel in the face, fy 0 at the top.
const robeCloth = (f, fx, fy, w, h) => f === 'u' ? C.robeLight : f === 'd' ? C.robeDark
    : (fx + fy * 3) % 7 === 0 ? C.robeLight : fy === h - 1 ? C.robeDark : C.robe;
// The tail: robe at the top, paler and more transparent toward the tip (t 0 top of the tail, 1 tip).
const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));
const tail = (start, end) => (f, fx, fy, w, h) => {
    const t = start + (end - start) * (f === 'u' ? 0 : f === 'd' ? 1 : fy / Math.max(1, h - 1));
    const base = (fx + fy) % 5 === 0 ? C.robeLight : C.robe;
    const c = mix(base, C.mist, Math.min(1, t * 1.2));
    c[3] = Math.round(255 * (1 - t * 0.75));
    return c;
};
const paint = {
    tail1: tail(0, 0.3), tail2: tail(0.3, 0.55), tail3: tail(0.55, 0.8), tail4: tail(0.8, 1),
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
    { name: 'tail1', parent: 'float', pivot: [0, 10, 0.5], cubes: [['tail1', [-3.5, 6, -1.5], [7, 4, 4]]] },
    { name: 'tail2', parent: 'tail1', pivot: [0, 6, 0.5], cubes: [['tail2', [-2.5, 3, -1], [5, 3, 3]]] },
    { name: 'tail3', parent: 'tail2', pivot: [0, 3, 0.5], cubes: [['tail3', [-1.5, 1, -0.5], [3, 2, 2]]] },
    { name: 'tail4', parent: 'tail3', pivot: [0, 1, 0.5], cubes: [['tail4', [-0.5, -1, 0], [1, 2, 1]]] },
    {
        name: 'body', parent: 'float', pivot: [0, 10, 0.5], cubes: [
            ['torso', [-4, 10, -2], [8, 10, 5]],
            ['sleeve', [4, 12, -2], [3, 8, 5]], ['sleeve', [-7, 12, -2], [3, 8, 5]],
            ['forearm', [2, 12, -6], [4, 3, 5]], ['forearm', [-6, 12, -6], [4, 3, 5]],
            ['hands', [-2, 12.5, -6.5], [4, 2, 2]],
        ]
    },
    {
        name: 'head', parent: 'body', pivot: [0, 20, 0.5], cubes: [
            ['head', [-4, 20, -3.5], [8, 8, 8]], ['bun', [-2, 28, -1.5], [4, 3, 4]], ['pin', [-4, 29, 0], [8, 1, 1]],
        ]
    },
    { name: 'beard', parent: 'head', pivot: [0, 20, -3.5], cubes: [['beard', [-2, 14, -3.5], [4, 6, 1]]] },
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
                // Hovers half a block up and drifts, breathes slowly, the beard stirs, the tail of mist ripples.
                float: { position: [0, wave(1.5, 45, 8), 0] },
                body: { scale: [wave(0.015, 90, 1), wave(0.02, 90, 1), wave(0.015, 90, 1)] },
                beard: { rotation: [wave(4, 60), 0, wave(2, 40)] },
                tail1: { rotation: [wave(5, 70), 0, wave(4, 50)] },
                tail2: { rotation: [`math.sin(query.anim_time * 70 - 50) * 9`, 0, `math.sin(query.anim_time * 50 - 50) * 7`] },
                tail3: { rotation: [`math.sin(query.anim_time * 70 - 100) * 13`, 0, `math.sin(query.anim_time * 50 - 100) * 10`] },
                tail4: { rotation: [`math.sin(query.anim_time * 70 - 150) * 17`, 0, `math.sin(query.anim_time * 50 - 150) * 14`] },
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
