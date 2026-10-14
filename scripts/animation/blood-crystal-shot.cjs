// The Sanguine Art's thrown blood crystal (GeckoLib): a faceted crystal with four spikes and two tips, six small
// fragments circling it, and its animations, like the crab man's water ball: it forms in the hand, spins in flight,
// then shatters on the ground, on a wall or on a body; the great crystal (a full gauge) explodes instead.
// Also writes the crystal's texture and the shard sprite of the explosion's Photon particles.
//
//   node scripts/animation/blood-crystal-shot.cjs
//
// Model units are pixels (16 to a block). At scale 1 the crystal stands 12 pixels (0.75 block); the entity scales it.
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

// ---------- texture (32 x 32): crystal faces in four shades, a bright facet line, and the shard sprite ----------
const SHADES = [0xff3d0610, 0xff7a0f1e, 0xffb31d31, 0xffeb4a60, 0xffffc2ca];
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    for (let x = 0; x < w; x++) {
      const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
      raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = (c >>> 24) & 255;
    }
  }
  const crc = (buf) => { let c = ~0; for (const b of buf) { c ^= b; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
// Tiles of 8 x 8: tile t at (t % 4 * 8, floor(t / 4) * 8). Tile 0: deep face, 1: mid face, 2: lit face, 3: tip,
// 4: fragment. Each face has a darker rim and one bright diagonal facet line.
function tile(t, x, y) {
  const base = [1, 2, 3, 3, 2][t] ?? 1;
  if (x === 0 || y === 0 || x === 7 || y === 7) return SHADES[Math.max(0, base - 1)];
  if ((x + y) % 7 === 3 && t !== 4) return SHADES[Math.min(4, base + 2)];
  if (x === 1 && t !== 4) return SHADES[Math.min(4, base + 1)];
  if (t === 3 && y < 3) return SHADES[4];
  return SHADES[base];
}
const texel = (x, y) => { const t = Math.floor(y / 8) * 4 + Math.floor(x / 8); return t <= 4 ? tile(t, x % 8, y % 8) : 0; };
fs.writeFileSync(path.join(root, 'textures/entity/blood_crystal_shot.png'), png(32, 32, texel));
// The glow mask (GeckoLib AutoGlowingGeoLayer): only the palest facet lines, drawn at full brightness.
fs.writeFileSync(path.join(root, 'textures/entity/blood_crystal_shot_glowmask.png'), png(32, 32, (x, y) => {
  const c = texel(x, y); return c === SHADES[4] || c === SHADES[3] ? c : 0;
}));
// The explosion's shard sprite (8 x 8): a pale diamond, tinted crimson by the particle colour.
fs.mkdirSync(path.join(root, 'textures/particle'), { recursive: true });
fs.writeFileSync(path.join(root, 'textures/particle/blood_shard.png'), png(8, 8, (x, y) => {
  const d = Math.abs(x - 3.5) + Math.abs(y - 3.5);
  return d <= 1.5 ? 0xffffffff : d <= 3 ? 0xffd8d8d8 : 0;
}));

// ---------- geometry ----------
const T = (t) => [(t % 4) * 8, Math.floor(t / 4) * 8];
function cube(origin, size, t, rotation, pivot) {
  const [u, v] = T(t), uv = (w, h) => ({ uv: [u, v], uv_size: [Math.max(1, Math.min(8, w)), Math.max(1, Math.min(8, h))] });
  const [w, h, d] = size;
  const c = { origin, size, uv: { north: uv(w, h), south: uv(w, h), east: uv(d, h), west: uv(d, h), up: uv(w, d), down: uv(w, d) } };
  if (rotation) { c.rotation = rotation; c.pivot = pivot || [0, 0, 0]; }
  return c;
}
const bones = [];
const bone = (name, parent, pivot, cubes) => { const b = { name, pivot, cubes }; if (parent) b.parent = parent; bones.push(b); return b; };
bone('crystal', null, [0, 0, 0], []);
// The core: a gem, wide in the middle and narrowing in steps to a point at each end, each step turned 45 degrees.
bone('core', 'crystal', [0, 0, 0], [cube([-2.25, -2.5, -2.25], [4.5, 5, 4.5], 1, [0, 45, 0]), cube([-1.6, -3.5, -1.6], [3.2, 7, 3.2], 2)]);
bone('tip_top', 'crystal', [0, 3.5, 0], [cube([-1.5, 2.5, -1.5], [3, 2.2, 3], 2, [0, 45, 0], [0, 3.5, 0]),
  cube([-0.9, 4.7, -0.9], [1.8, 1.8, 1.8], 3, [0, 0, 0], [0, 3.5, 0]), cube([-0.4, 6.5, -0.4], [0.8, 1.2, 0.8], 3, [0, 45, 0], [0, 3.5, 0])]);
bone('tip_bottom', 'crystal', [0, -3.5, 0], [cube([-1.5, -4.7, -1.5], [3, 2.2, 3], 0, [0, 45, 0], [0, -3.5, 0]),
  cube([-0.9, -6.5, -0.9], [1.8, 1.8, 1.8], 0, [0, 0, 0], [0, -3.5, 0]), cube([-0.4, -7.7, -0.4], [0.8, 1.2, 0.8], 0, [0, 45, 0], [0, -3.5, 0])]);
// Six spikes leaning hard out of the gem at different heights and lengths, so its outline bristles.
const SPIKES = [[0, 1, 48, 6], [60, -1.2, 52, 5], [120, 0.4, 44, 6.5], [180, -0.6, 50, 5.5], [240, 1.4, 46, 4.5], [300, -1.6, 55, 5]];
SPIKES.forEach(([yaw, h, tilt, length], i) => {
  bone('spike_' + i, 'crystal', [0, h, 0], [cube([-0.85, h, -0.85], [1.7, length, 1.7], i % 2 ? 2 : 3, [tilt, yaw + 45, 0], [0, h, 0])]);
});
// Six fragments circling the crystal, each a small shard on its own bone (they orbit with `fragments`).
bone('fragments', null, [0, 0, 0], []);
const FRAGMENTS = [];
for (let i = 0; i < 6; i++) {
  const a = (i / 6) * Math.PI * 2, r = 7.5, y = [2, -1.5, 0.5, -2.5, 3, -0.5][i];
  const at = [Math.cos(a) * r, y, Math.sin(a) * r];
  FRAGMENTS.push({ name: 'frag_' + i, at, a });
  bone('frag_' + i, 'fragments', at, [cube([at[0] - 0.6, at[1] - 1.1, at[2] - 0.6], [1.2, 2.2, 1.2], 4, [20 * (i % 3), 45, 15 * ((i + 1) % 3)], at)]);
}
const geo = { format_version: '1.12.0', 'minecraft:geometry': [{
  description: { identifier: 'geometry.blood_crystal_shot', texture_width: 32, texture_height: 32,
    visible_bounds_width: 3, visible_bounds_height: 3, visible_bounds_offset: [0, 0, 0] }, bones }] };
fs.writeFileSync(path.join(root, 'geo/blood_crystal_shot.geo.json'), JSON.stringify(geo, null, 2) + '\n');

// ---------- animations ----------
const round = (v) => Math.round(v * 1000) / 1000;
const vec = (v) => v.map(round);
const kf = (pairs) => Object.fromEntries(pairs.map(([t, v]) => [String(round(t)), vec(v)]));
const PIECES = ['core', 'tip_top', 'tip_bottom', 'spike_0', 'spike_1', 'spike_2', 'spike_3', 'spike_4', 'spike_5'];
// Direction each piece leaves in when the crystal breaks (model space), from where it sits.
const PIECE_DIR = { core: [0, 0.2, 0], tip_top: [0, 1, 0], tip_bottom: [0, -1, 0],
  ...Object.fromEntries([0, 60, 120, 180, 240, 300].map((yaw, i) => { const a = yaw * Math.PI / 180; return ['spike_' + i, [Math.sin(a), 0.3 - 0.15 * i % 0.5, Math.cos(a)]]; })) };
const norm = (v) => { const l = Math.hypot(...v) || 1; return v.map((x) => x / l); };
// Pseudo-random but fixed spins, so the export is reproducible.
const spin = (i, k) => [((i * 97 + k * 31) % 7 - 3) * 120, ((i * 53 + k * 17) % 5 - 2) * 140, ((i * 71 + k * 13) % 7 - 3) * 110];

const animations = {};
// Forming in the hand: the crystal grows from nothing with a small overshoot; the fragments fly in from afar.
animations['animation.blood_crystal_shot.form'] = { animation_length: 0.6, bones: {
  crystal: { scale: kf([[0, [0, 0, 0]], [0.35, [1.15, 1.15, 1.15]], [0.6, [1, 1, 1]]]) },
  ...Object.fromEntries(FRAGMENTS.map((f, i) => [f.name, {
    position: kf([[0, [f.at[0] * 0.8, f.at[1] + 2, f.at[2] * 0.8]], [0.3 + i * 0.04, [0, 0, 0]]]),
    scale: kf([[0, [0, 0, 0]], [0.3 + i * 0.04, [1, 1, 1]]]) }])),
} };
// Held: the fragments circle the crystal (a turn a second) and the crystal pulses.
animations['animation.blood_crystal_shot.charge'] = { loop: true, animation_length: 1, bones: {
  fragments: { rotation: kf([[0, [0, 0, 0]], [0.5, [0, 180, 0]], [1, [0, 360, 0]]]) },
  crystal: { scale: kf([[0, [1, 1, 1]], [0.5, [1.06, 1.06, 1.06]], [1, [1, 1, 1]]]) },
  ...Object.fromEntries(FRAGMENTS.map((f, i) => [f.name, {
    position: kf([[0, [0, 0, 0]], [0.5, [0, i % 2 ? 1 : -1, 0]], [1, [0, 0, 0]]]) }])),
} };
// In flight the crystal drills round its long axis (pointed along the flight) and the fragments trail round it.
animations['animation.blood_crystal_shot.fly'] = { loop: true, animation_length: 0.5, bones: {
  crystal: { rotation: kf([[0, [0, 0, 0]], [0.25, [0, 180, 0]], [0.5, [0, 360, 0]]]) },
  fragments: { rotation: kf([[0, [0, 0, 0]], [0.25, [0, -180, 0]], [0.5, [0, -360, 0]]]) },
} };
// A break: each piece leaves along `dir` (model space) by `reach` pixels, `lift` up first, then falls by `fall`,
// spinning; all of it shrinks away at the end. Fragments go further.
function shatter(name, { length, reach, lift, fall, bias = [0, 0, 0], fragReach }) {
  const out = {}, end = length, gone = length * 0.82;
  PIECES.forEach((p, i) => {
    const d = norm(PIECE_DIR[p].map((v, k) => v + bias[k]));
    const far = d.map((v) => v * reach);
    out[p] = {
      position: kf([[0, [0, 0, 0]], [end * 0.18, [far[0] * 0.55, far[1] * 0.55 + lift, far[2] * 0.55]],
        [end * 0.6, [far[0], far[1] + lift * 0.4 - fall * 0.6, far[2]]], [end, [far[0] * 1.05, far[1] - fall, far[2] * 1.05]]]),
      rotation: kf([[0, [0, 0, 0]], [end, spin(i, 1)]]),
      scale: kf([[0, [1, 1, 1]], [end * 0.12, [1.12, 1.12, 1.12]], [gone, [0.8, 0.8, 0.8]], [end, [0, 0, 0]]]),
    };
  });
  FRAGMENTS.forEach((f, i) => {
    const d = norm([Math.cos(f.a) + bias[0], 0.3 + bias[1], Math.sin(f.a) + bias[2]]);
    const far = d.map((v) => v * fragReach);
    out[f.name] = {
      position: kf([[0, [0, 0, 0]], [end * 0.3, [far[0], far[1] + lift, far[2]]], [end, [far[0] * 1.2, far[1] - fall, far[2] * 1.2]]]),
      rotation: kf([[0, [0, 0, 0]], [end, spin(i + 7, 2)]]),
      scale: kf([[0, [1, 1, 1]], [gone, [0.7, 0.7, 0.7]], [end, [0, 0, 0]]]),
    };
  });
  out.crystal = { rotation: kf([[0, [0, 0, 0]], [end, [0, 0, 0]]]) };
  out.fragments = { rotation: kf([[0, [0, 0, 0]], [end, [0, 0, 0]]]) };
  animations['animation.blood_crystal_shot.' + name] = { animation_length: length, bones: out };
}
// On the ground: the pieces skid outward over the floor and settle. Model -Z faces the flight.
shatter('shatter_ground', { length: 0.75, reach: 7, lift: 3, fall: 4, bias: [0, -0.4, 0], fragReach: 11 });
// On a wall (model -Z points into it): the pieces burst back off it, then slide down it.
shatter('shatter_wall', { length: 0.75, reach: 7, lift: 1, fall: 7, bias: [0, 0, 1.2], fragReach: 10 });
// On a body: the crystal breaks against it, the pieces spray round it and drop.
shatter('shatter_body', { length: 0.75, reach: 8, lift: 2, fall: 9, bias: [0, 0, 0.6], fragReach: 12 });
// The great crystal: a violent burst in every direction, far, spinning hard.
shatter('explode', { length: 0.9, reach: 22, lift: 6, fall: 10, fragReach: 30 });
fs.writeFileSync(path.join(root, 'animations/blood_crystal_shot.animation.json'),
  JSON.stringify({ format_version: '1.8.0', animations }, null, 2) + '\n');
console.log('blood crystal shot: ' + bones.length + ' bones, ' + Object.keys(animations).length + ' animations');
