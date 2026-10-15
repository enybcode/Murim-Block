// Scarlet Tear's blood sword (GeckoLib), the "cracked crystal" style Enzo chose: a dark wine crystal blade split by
// glowing cracks, a shard floating beside each segment, a crown of crystal spikes for a guard, gems at the pommel and
// the point. Its animations: it forms
// piece by piece from the grip to the point, glows while held, flies point first, and explodes.
//
//   node scripts/animation/blood-sword.cjs
//
// Units are pixels (16 to a block). The grip's middle is the origin; the blade runs up +Y to the point at 29 px,
// so the sword is about 2 blocks long. Writes its geo, animations, texture and glowmask.
const fs = require('node:fs'), path = require('node:path');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

// Its texture, blood_sword.png (64 x 64, eight 16 x 16 tiles), and blood_sword_glowmask.png: only the cracks, the
// floating shards and the gems shine in the dark. Tiles: 0 crystal body, 1 lit facet, 2 crack glow, 3 crown, 4 grip
// wrap, 5 gem, 6 shard, 7 dark.
const zlib = require('node:zlib');
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = (c >>> 24) & 255; }
  const crc = (buf) => { let c = ~0; for (const v of buf) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]), c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hash = (x, y, t) => { let h = (x * 374761393 + y * 668265263 + t * 2147483647) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967295; };
const mix = (a, b, t) => { const ch = (k) => Math.round(((a >>> k) & 255) * (1 - t) + ((b >>> k) & 255) * t); return 0xff000000 | (ch(16) << 16) | (ch(8) << 8) | ch(0); };
const P = { dark: 0xff0c0208, body: 0xff2a0618, edge: 0xff5a1034, shine: 0xffff8aa8, glow: 0xffff2d55, crown: 0xff52102e, crownDark: 0xff1a0510, wrap: 0xff2a0618 };
const GLOWS = new Set([2, 5]);
function tile(t, x, y) {
  const n = hash(x, y, 91 + t * 31);
  switch (t) {
    case 0: { let c = mix(P.dark, P.body, 0.35 + 0.4 * n); if (x === 0 || x === 15) c = mix(c, P.edge, 0.5);
      if ((x + Math.floor(y / 3)) % 7 === 0) c = mix(c, P.dark, 0.5); return c; }
    case 1: { const d = Math.min(x, 15 - x); return d < 2 ? mix(P.edge, P.shine, n * 0.5) : mix(P.body, P.edge, 0.3 + n * 0.3); }
    case 2: { const d = Math.min(1, Math.hypot(x - 7.5, y - 7.5) / 9); return mix(P.shine, P.glow, d * 0.8 + n * 0.2); }
    case 3: { let c = mix(P.crownDark, P.crown, 0.4 + 0.4 * n); if (x < 1 || y < 1) c = mix(c, P.shine, 0.35); if (x > 14 || y > 14) c = mix(c, P.dark, 0.5); return c; }
    case 4: return mix(((x + y) % 6) < 3 ? P.wrap : P.dark, P.body, n * 0.2);
    case 5: { const d = Math.hypot(x - 7.5, y - 7.5) / 8; return mix(P.shine, P.glow, Math.min(1, d * 1.2)); }
    case 6: return mix(P.edge, P.dark, y / 15 * 0.7 + n * 0.15);
    default: return mix(P.dark, 0xff000000, 0.4 + n * 0.3);
  }
}
const at = (x, y) => Math.floor(y / 16) * 4 + Math.floor(x / 16);
const tex = path.join(root, 'textures/entity');
fs.writeFileSync(path.join(tex, 'blood_sword.png'), png(64, 64, (x, y) => at(x, y) < 8 ? tile(at(x, y), x % 16, y % 16) : 0));
fs.writeFileSync(path.join(tex, 'blood_sword_glowmask.png'), png(64, 64, (x, y) => GLOWS.has(at(x, y)) ? tile(at(x, y), x % 16, y % 16) : 0));

const T = (t) => [(t % 4) * 16, Math.floor(t / 4) * 16];
function cube(origin, size, t, rotation, pivot) {
  const [u, v] = T(t), uv = (w, h) => ({ uv: [u, v], uv_size: [Math.max(1, Math.min(16, w * 2)), Math.max(1, Math.min(16, h * 2))] });
  const [w, h, d] = size;
  const c = { origin, size, uv: { north: uv(w, h), south: uv(w, h), east: uv(d, h), west: uv(d, h), up: uv(w, d), down: uv(w, d) } };
  if (rotation) { c.rotation = rotation; c.pivot = pivot || origin.map((o, i) => o + size[i] / 2); }
  return c;
}
const bones = [];
const bone = (name, parent, pivot, cubes) => { const b = { name, pivot, cubes }; if (parent) b.parent = parent; bones.push(b); return b; };
bone('sword', null, [0, 0, 0], []);
// The hilt: a crystal pommel with a gem, a dark wrapped grip, and a crown of six crystal spikes round a faceted collar.
bone('pommel', 'sword', [0, -4.4, 0], [cube([-1, -5.4, -1], [2, 2, 2], 0, [45, 45, 0]), cube([-0.5, -6.4, -0.5], [1, 1, 1], 5)]);
bone('grip', 'sword', [0, 0, 0], [cube([-0.7, -3.4, -0.7], [1.4, 6.8, 1.4], 4)]);
const crown = [cube([-1.4, 3, -1.4], [2.8, 1.6, 2.8], 0, [0, 45, 0])];
for (let k = 0; k < 6; k++) { const r = k * Math.PI / 3;
  crown.push(cube([Math.cos(r) * 1.8 - 0.4, 3.8, Math.sin(r) * 1.8 - 0.4], [0.8, 2.6 - (k % 2) * 0.8, 0.8], 6,
    [Math.sin(r) * 30, 0, -Math.cos(r) * 30], [Math.cos(r) * 1.8, 4, Math.sin(r) * 1.8])); }
bone('guard', 'sword', [0, 3.8, 0], crown);
// The blade: six dark crystal segments, each a little askew, split by glowing cracks (one across, one along), and a
// shard floating free beside each one (its own bone, so it can drift while the sword is held).
const SEGMENTS = [], SHARDS = [];
for (let i = 0; i < 6; i++) {
  const y = 5 + i * 3.7, w = 2.8 - i * 0.24, off = i % 2 ? 0.25 : -0.25;
  SEGMENTS.push('blade_' + i);
  bone('blade_' + i, 'sword', [0, y + 1.7, 0], [cube([-w / 2 + off * 0.3, y, -0.5], [w, 3.4, 1], i % 2 ? 0 : 1, [0, 0, i % 2 ? 2 : -2], [0, y + 1.7, 0]),
    cube([-w / 2 + 0.3, y + 3.3, -0.55], [w - 0.6, 0.35, 1.1], 2, [0, 0, i % 2 ? 12 : -12], [0, y + 3.4, 0]),
    cube([-0.15 + off, y + 0.5, -0.55], [0.3, 2.4, 1.1], 2, [0, 0, off * 40], [0, y + 1.7, 0])]);
  const sx = (i % 2 ? 1 : -1) * (w / 2 + 1.4);
  SHARDS.push('shard_' + i);
  bone('shard_' + i, 'blade_' + i, [sx, y + 1.8, 0], [cube([sx - 0.4, y + 1, -0.4], [0.8, 1.6, 0.8], i % 2 ? 2 : 6, [20, 30 * i, 25], [sx, y + 1.8, 0])]);
}
bone('tip', 'sword', [0, 28.1, 0], [cube([-0.9, 27.2, -0.45], [1.8, 1.8, 0.9], 1, [0, 0, 45], [0, 28.1, 0]),
  cube([-0.35, 28.6, -0.3], [0.7, 1.6, 0.6], 5)]);
const PIECES = ['pommel', 'grip', 'guard', ...SEGMENTS, 'tip'];
fs.writeFileSync(path.join(root, 'geo/blood_sword.geo.json'), JSON.stringify({ format_version: '1.12.0', 'minecraft:geometry': [{
  description: { identifier: 'geometry.blood_sword', texture_width: 64, texture_height: 64,
    visible_bounds_width: 3, visible_bounds_height: 4, visible_bounds_offset: [0, 1, 0] }, bones }] }, null, 2) + '\n');

const round = (v) => Math.round(v * 1000) / 1000;
const kf = (pairs) => Object.fromEntries(pairs.map(([t, v]) => [String(round(t)), v.map(round)]));
const animations = {};
// Forming, 0.55 s: from the grip outward, each piece flies in from beside the blade, grows past its size and
// settles, the next one 0.045 s later; the point last.
const form = {};
PIECES.forEach((p, i) => {
  const start = i * 0.045, side = i % 2 ? 1 : -1;
  form[p] = {
    position: kf([[0, [side * 3, -1, 1.5]], [start, [side * 3, -1, 1.5]], [start + 0.12, [0, 0, 0]]]),
    scale: kf([[0, [0, 0, 0]], [start, [0, 0, 0]], [start + 0.08, [1.25, 1.25, 1.25]], [start + 0.16, [1, 1, 1]]]),
  };
});
animations['animation.blood_sword.form'] = { animation_length: 0.6, bones: form };
// Held: a slow pulse runs up the blade.
animations['animation.blood_sword.held'] = { loop: true, animation_length: 1.2, bones: Object.fromEntries(SEGMENTS.map((s, i) => [s, {
  scale: kf([[0, [1, 1, 1]], [0.1 + i * 0.12, [1.08, 1, 1.08]], [0.3 + i * 0.12, [1, 1, 1]], [1.2, [1, 1, 1]]]) }]).concat(
  // The shards drift round the blade.
  SHARDS.map((s, i) => [s, { position: kf([[0, [0, 0, 0]], [0.3, [0, i % 2 ? 0.4 : -0.4, 0.2]], [0.6, [0, 0, 0]], [0.9, [0, i % 2 ? -0.4 : 0.4, -0.2]], [1.2, [0, 0, 0]]]),
    rotation: kf([[0, [0, 0, 0]], [1.2, [0, 360, 0]]]) }]))) };
// In flight: point first, the sword turns slowly round its blade.
animations['animation.blood_sword.fly'] = { loop: true, animation_length: 0.6, bones: {
  sword: { rotation: kf([[0, [0, 0, 0]], [0.3, [0, 180, 0]], [0.6, [0, 360, 0]]]) } } };
// The explosion: every piece bursts away from where it sat, spinning, and shrinks away.
const spin = (i) => [((i * 97) % 7 - 3) * 130, ((i * 53) % 5 - 2) * 150, ((i * 71) % 7 - 3) * 120];
const explode = {};
PIECES.forEach((p, i) => {
  const bone_ = bones.find((b) => b.name === p), y = bone_.pivot[1] - 12;
  const a = i * 2.4, out = [Math.cos(a) * 14, y * 0.8 + 6, Math.sin(a) * 14];
  explode[p] = {
    position: kf([[0, [0, 0, 0]], [0.18, [out[0] * 0.6, out[1] * 0.6, out[2] * 0.6]], [0.7, [out[0], out[1] - 10, out[2]]]]),
    rotation: kf([[0, [0, 0, 0]], [0.7, spin(i)]]),
    scale: kf([[0, [1, 1, 1]], [0.1, [1.3, 1.3, 1.3]], [0.55, [0.8, 0.8, 0.8]], [0.7, [0, 0, 0]]]),
  };
});
animations['animation.blood_sword.explode'] = { animation_length: 0.7, bones: explode };
fs.writeFileSync(path.join(root, 'animations/blood_sword.animation.json'), JSON.stringify({ format_version: '1.8.0', animations }, null, 2) + '\n');
console.log('blood sword: ' + bones.length + ' bones, ' + Object.keys(animations).length + ' animations');
