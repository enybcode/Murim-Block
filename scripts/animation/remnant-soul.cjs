// The remnant soul of an old master (docs/cultivation/qi-cultivation.md), a ghost in the inner world, as a Blockbench /
// GeckoLib model with relief, its texture painted face by face, and its floating animation:
//   art/blockbench/remnant_soul.geo.json             the model (open it in Blockbench: File > Open Model) - also the game's
//   assets/murimblock/geo/entity/remnant_soul.geo.json
//   assets/murimblock/animations/entity/remnant_soul.animation.json   "idle": floating, breathing, the tail swaying
//   assets/murimblock/textures/entity/remnant_soul.png (+ _glowmask.png: what glows in the dark)
// The old man: a topknot with a jade pin, long white eyebrows hanging, closed eyes, a moustache and a long beard in
// steps down to the chest; a robe with a crossed collar and a red sash, wide sleeves meeting before him (hands hidden);
// no legs: below the sash the robe thins into a curling tail of mist. Pale and see-through, fading towards the tail.
// Usage: node scripts/animation/remnant-soul.cjs
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const TEX = 128;

// ---------------------------------------------------------------- materials: colour (r, g, b), alpha, glow

const MAT = {
  skin: { c: [222, 214, 204], a: 0.72 },
  hair: { c: [236, 242, 246], a: 0.9, glow: true },
  beard: { c: [240, 245, 248], a: 0.88, glow: true },
  brow: { c: [246, 250, 252], a: 0.95, glow: true },
  robe: { c: [150, 176, 190], a: 0.6 },
  robeDark: { c: [112, 138, 156], a: 0.62 },
  collar: { c: [214, 226, 232], a: 0.8 },
  sash: { c: [178, 70, 64], a: 0.8 },
  jade: { c: [120, 214, 176], a: 0.95, glow: true },
  mist: { c: [176, 214, 228], a: 0.5, glow: true },
};

// ---------------------------------------------------------------- the model: bones and cubes (Bedrock units, y up, front -z)

const bones = [];
function bone(name, parent, pivot, rotation) {
  const b = { name, pivot, cubes: [] };
  if (parent) b.parent = parent;
  if (rotation) b.rotation = rotation;
  bones.push(b);
  return b;
}
/** A cube from `origin` of `size`, of a material; `fade` lowers its alpha (the tail); `rot` turns it about `pivot`. */
function cube(b, origin, size, mat, opts = {}) {
  b.cubes.push({ origin, size, mat, ...opts });
}

const soul = bone('soul', null, [0, 0, 0]);
const body = bone('body', 'soul', [0, 12, 0]);
const head = bone('head', 'body', [0, 24, 0]);

// Head: face, hair round the back and sides, a topknot with a jade pin.
cube(head, [-4, 24, -4], [8, 8, 8], 'skin', { part: 'face' });
cube(head, [-4.5, 26, -1], [9, 6.5, 5.5], 'hair', { part: 'hair' });
cube(head, [-4.5, 31.5, -4.5], [9, 1, 9], 'hair', { part: 'hair' });
cube(head, [-1.5, 32.5, -1], [3, 2.5, 3], 'hair', { part: 'hair' });
cube(head, [-3, 33.5, 0], [6, 0.6, 0.6], 'jade');
// Long eyebrows, hanging down past the eyes.
cube(head, [-3.6, 28.6, -4.6], [3, 0.8, 0.8], 'brow');
cube(head, [0.6, 28.6, -4.6], [3, 0.8, 0.8], 'brow');
cube(head, [-4.4, 26.2, -4.5], [0.8, 3, 0.8], 'brow');
cube(head, [3.6, 26.2, -4.5], [0.8, 3, 0.8], 'brow');
// Nose, moustache with its ends falling.
cube(head, [-0.6, 26.4, -4.9], [1.2, 1.8, 1], 'skin', { part: 'nose' });
cube(head, [-3, 25.5, -4.8], [6, 1, 1], 'beard');
cube(head, [-3.6, 23.2, -4.7], [1, 2.6, 0.9], 'beard');
cube(head, [2.6, 23.2, -4.7], [1, 2.6, 0.9], 'beard');
// The beard in steps, narrowing down over the chest.
cube(head, [-3, 21.5, -5.2], [6, 3.5, 1.6], 'beard', { part: 'beard' });
cube(head, [-2.4, 18.5, -5.4], [4.8, 3, 1.5], 'beard', { part: 'beard' });
cube(head, [-1.6, 15.8, -5.2], [3.2, 2.7, 1.3], 'beard', { part: 'beard' });
cube(head, [-0.8, 13.8, -4.9], [1.6, 2, 1], 'beard', { part: 'beard' });

// Torso: the robe, its crossed collar, the red sash and its knot.
cube(body, [-4.5, 12, -2.5], [9, 12, 5], 'robe', { part: 'robe' });
cube(body, [-3.6, 15, -2.9], [3.4, 8.5, 0.5], 'collar', { rotation: [0, 0, -22], pivot: [-1, 23, -2.7] });
cube(body, [0.2, 15, -2.95], [3.4, 8.5, 0.5], 'collar', { rotation: [0, 0, 22], pivot: [1, 23, -2.7] });
cube(body, [-4.8, 13.5, -2.8], [9.6, 2.2, 5.6], 'sash', { part: 'sash' });
cube(body, [1.2, 10.5, -3.05], [1.4, 3.4, 0.4], 'sash');
cube(body, [2.4, 10.8, -3.05], [1, 2.6, 0.4], 'sash');

// Sleeves: down from the shoulders, then folded forward, the wide cuffs meeting before the belly (the hands inside).
for (const side of [-1, 1]) {
  const arm = bone(side < 0 ? 'arm_right' : 'arm_left', 'body', [side * 6, 23, 0]);
  cube(arm, side < 0 ? [-7.5, 15.5, -2.5] : [4.5, 15.5, -2.5], [3, 8, 5], 'robe', { part: 'sleeve' });
  cube(arm, side < 0 ? [-7.8, 12.5, -7.5] : [4.3, 12.5, -7.5], [3.5, 5, 6], 'robe', { part: 'sleeve' });
  cube(arm, side < 0 ? [-5, 12, -9.5] : [0, 12, -9.5], [5, 5.5, 4.5], 'robeDark', { part: 'cuff' });
}
cube(body, [-1.5, 13.6, -9.9], [3, 2.2, 1], 'skin', { part: 'hands' });

// No legs: the robe flares at the hem, then thins into mist curling back and fading.
const hem = bone('hem', 'body', [0, 12, 0]);
cube(hem, [-5, 7, -3], [10, 5, 6.5], 'robe', { part: 'robe', fade: 0.9 });
const tail1 = bone('tail_1', 'hem', [0, 7, 0.5]);
cube(tail1, [-4, 3, -2.2], [8, 4, 5.5], 'mist', { part: 'mist', fade: 0.85 });
const tail2 = bone('tail_2', 'tail_1', [0, 3, 1]);
cube(tail2, [-2.8, 0, -1.2], [5.6, 3, 4.4], 'mist', { part: 'mist', fade: 0.65 });
const tail3 = bone('tail_3', 'tail_2', [0, 0, 1.6]);
cube(tail3, [-1.8, -2.6, 0], [3.6, 2.6, 3.4], 'mist', { part: 'mist', fade: 0.45 });
const tail4 = bone('tail_4', 'tail_3', [0, -2.6, 2.6]);
cube(tail4, [-0.9, -4.4, 1.6], [1.8, 1.8, 2.4], 'mist', { part: 'mist', fade: 0.3 });

// ---------------------------------------------------------------- texture: every face its own rectangle

const rgba = new Float32Array(TEX * TEX * 4);
const glow = new Float32Array(TEX * TEX * 4);
let shelfX = 0, shelfY = 0, shelfH = 0;
function alloc(w, h) {
  if (shelfX + w > TEX) { shelfX = 0; shelfY += shelfH; shelfH = 0; }
  const at = [shelfX, shelfY];
  shelfX += w;
  shelfH = Math.max(shelfH, h);
  if (shelfY + h > TEX) throw new Error('texture full');
  return at;
}
function hash(x, y, s) {
  let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0;
  h = (h ^ (h >>> 13)) * 1274126177;
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}
function put(x, y, c, a, glowIt) {
  const i = (y * TEX + x) * 4;
  rgba[i] = c[0]; rgba[i + 1] = c[1]; rgba[i + 2] = c[2]; rgba[i + 3] = a;
  if (glowIt) { glow[i] = c[0]; glow[i + 1] = c[1]; glow[i + 2] = c[2]; glow[i + 3] = Math.min(1, a + 0.2); }
}
const shade = (c, k) => c.map(v => Math.max(0, Math.min(255, Math.round(v * k))));

/** Paints one face (w x h texels) of a cube: its material, a little noise, and what its part draws on it. */
function paint(x0, y0, w, h, face, cubeDef, salt) {
  const m = MAT[cubeDef.mat];
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const n = hash(x0 + x, y0 + y, salt);
    let c = shade(m.c, 0.92 + n * 0.12);
    let a = m.a * (cubeDef.fade ?? 1);
    const part = cubeDef.part;
    const t = h > 1 ? y / (h - 1) : 0;
    if (part === 'mist' || (part === 'robe' && cubeDef.fade)) {
      // Fading downwards, a few holes of nothing: mist.
      a *= 1 - t * 0.45;
      if (n < 0.12) a *= 0.35;
    }
    if (part === 'robe' || part === 'sleeve' || part === 'cuff') {
      // Folds: darker vertical streaks; the hem edge pale.
      if ((x + Math.floor(n * 2)) % 4 === 0 && face !== 'up' && face !== 'down') c = shade(c, 0.82);
      if (part === 'cuff' && y === h - 1) c = MAT.collar.c;
    }
    if (part === 'beard' || part === 'hair') {
      // Strands: lines along the length.
      if ((x + (part === 'beard' ? 0 : y)) % 2 === 0) c = shade(c, 0.9);
      if (part === 'beard' && n < 0.08) a *= 0.6;
    }
    if (part === 'sash' && y === 0) c = shade(c, 1.2);
    put(x0 + x, y0 + y, c, a, m.glow);
  }
  // The face: closed eyes, wrinkles, cheeks, the mouth hidden under the moustache.
  if (cubeDef.part === 'face' && face === 'north') {
    const ink = [92, 104, 116];
    for (const ex of [1, 2, 5, 6]) put(x0 + ex, y0 + 3, ink, 0.9, false);
    put(x0 + 1, y0 + 4, shade(MAT.skin.c, 0.86), 0.85, false);
    put(x0 + 6, y0 + 4, shade(MAT.skin.c, 0.86), 0.85, false);
    for (const wx of [2, 3, 4, 5]) put(x0 + wx, y0 + 1, shade(MAT.skin.c, 0.9), 0.85, false);
    put(x0 + 1, y0 + 5, [214, 190, 186], 0.85, false);
    put(x0 + 6, y0 + 5, [214, 190, 186], 0.85, false);
  }
}

const geoBones = bones.map(b => {
  const out = { name: b.name, pivot: b.pivot };
  if (b.parent) out.parent = b.parent;
  if (b.rotation) out.rotation = b.rotation;
  if (b.cubes.length) out.cubes = b.cubes.map((c, ci) => {
    const [w, h, d] = c.size.map(v => Math.max(1, Math.ceil(v)));
    const faces = { north: [w, h], south: [w, h], east: [d, h], west: [d, h], up: [w, d], down: [w, d] };
    const uv = {};
    for (const [face, [fw, fh]] of Object.entries(faces)) {
      const [ux, uy] = alloc(fw, fh);
      paint(ux, uy, fw, fh, face, c, bones.indexOf(b) * 31 + ci * 7 + face.length);
      uv[face] = face === 'up' || face === 'down' ? { uv: [ux + fw, uy + fh], uv_size: [-fw, -fh] } : { uv: [ux, uy], uv_size: [fw, fh] };
    }
    const cubeOut = { origin: c.origin, size: c.size, uv };
    if (c.rotation) { cubeOut.rotation = c.rotation; cubeOut.pivot = c.pivot; }
    return cubeOut;
  });
  return out;
});

const geo = {
  format_version: '1.12.0',
  'minecraft:geometry': [{
    description: { identifier: 'geometry.remnant_soul', texture_width: TEX, texture_height: TEX,
      visible_bounds_width: 2, visible_bounds_height: 3, visible_bounds_offset: [0, 1, 0] },
    bones: geoBones,
  }],
};

// ---------------------------------------------------------------- the idle: floating, breathing, the tail swaying

const L = 4;
const keys = (fn, n = 8) => Object.fromEntries(Array.from({ length: n + 1 }, (_, i) => [((L * i) / n).toFixed(2), fn((i / n) * Math.PI * 2)]));
const idle = {
  loop: true, animation_length: L,
  bones: {
    soul: { position: keys(p => [0, +(Math.sin(p) * 1.2).toFixed(3), 0]) },
    body: { rotation: keys(p => [+(Math.sin(p) * 1.5).toFixed(3), 0, 0]) },
    head: { rotation: keys(p => [+(Math.sin(p + 1) * 2).toFixed(3), +(Math.sin(p * 0.5) * 6).toFixed(3), 0]) },
    hem: { rotation: keys(p => [+(4 + Math.sin(p + 0.5) * 3).toFixed(3), 0, +(Math.sin(p) * 2).toFixed(3)]) },
    tail_1: { rotation: keys(p => [+(10 + Math.sin(p + 1) * 6).toFixed(3), 0, +(Math.sin(p + 0.5) * 5).toFixed(3)]) },
    tail_2: { rotation: keys(p => [+(16 + Math.sin(p + 1.6) * 8).toFixed(3), 0, +(Math.sin(p + 1) * 8).toFixed(3)]) },
    tail_3: { rotation: keys(p => [+(22 + Math.sin(p + 2.2) * 10).toFixed(3), 0, +(Math.sin(p + 1.6) * 10).toFixed(3)]) },
    tail_4: { rotation: keys(p => [+(28 + Math.sin(p + 2.8) * 12).toFixed(3), 0, +(Math.sin(p + 2.2) * 12).toFixed(3)]) },
  },
};
const animation = { format_version: '1.8.0', animations: { idle } };

// ---------------------------------------------------------------- write

function png(data) {
  const raw = Buffer.alloc((TEX * 4 + 1) * TEX);
  for (let y = 0; y < TEX; y++) for (let x = 0; x < TEX; x++) {
    const i = (y * TEX + x) * 4, o = y * (TEX * 4 + 1) + 1 + x * 4;
    raw[o] = data[i]; raw[o + 1] = data[i + 1]; raw[o + 2] = data[i + 2]; raw[o + 3] = Math.round(data[i + 3] * 255);
  }
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const v of buf) c = table[(c ^ v) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, d) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(d.length);
    const td = Buffer.concat([Buffer.from(type), d]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(TEX, 0); ihdr.writeUInt32BE(TEX, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

const geoText = JSON.stringify(geo, null, '\t');
for (const out of [path.join(ROOT, 'art', 'blockbench', 'remnant_soul.geo.json'), path.join(ASSETS, 'geo', 'entity', 'remnant_soul.geo.json')]) {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, geoText);
}
fs.mkdirSync(path.join(ASSETS, 'animations', 'entity'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'animations', 'entity', 'remnant_soul.animation.json'), JSON.stringify(animation, null, 2));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'remnant_soul.png'), png(rgba));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'remnant_soul_glowmask.png'), png(glow));
fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', 'remnant_soul.png'), png(rgba));
console.log('wrote the remnant soul:', bones.length, 'bones,', bones.reduce((s, b) => s + b.cubes.length, 0), 'cubes; texture', `${shelfY + shelfH}/${TEX} rows used`);
