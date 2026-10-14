// Scarlet Tear's blood sword (GeckoLib), in the blood crystal's style and texture (blood_crystal_shot.png): a pommel
// shard, a grip, a guard of crystal spikes, a blade of six faceted segments and a point. Its animations: it forms
// piece by piece from the grip to the point, glows while held, flies point first, and explodes.
//
//   node scripts/animation/blood-sword.cjs
//
// Units are pixels (16 to a block). The grip's middle is the origin; the blade runs up +Y to the point at 29 px,
// so the sword is about 2 blocks long.
const fs = require('node:fs'), path = require('node:path');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock');

// Texture tiles of blood_crystal_shot.png (8 x 8 each): 0 deep, 1 mid, 2 lit, 3 tip, 4 fragment.
const T = (t) => [(t % 4) * 8, Math.floor(t / 4) * 8];
function cube(origin, size, t, rotation, pivot) {
  const [u, v] = T(t), uv = (w, h) => ({ uv: [u, v], uv_size: [Math.max(1, Math.min(8, w)), Math.max(1, Math.min(8, h))] });
  const [w, h, d] = size;
  const c = { origin, size, uv: { north: uv(w, h), south: uv(w, h), east: uv(d, h), west: uv(d, h), up: uv(w, d), down: uv(w, d) } };
  if (rotation) { c.rotation = rotation; c.pivot = pivot || origin.map((o, i) => o + size[i] / 2); }
  return c;
}
const bones = [];
const bone = (name, parent, pivot, cubes) => { const b = { name, pivot, cubes }; if (parent) b.parent = parent; bones.push(b); return b; };
bone('sword', null, [0, 0, 0], []);
// The hilt: a pommel shard, a dark grip, and the guard: a crystal bar with a spike leaning out at each end.
bone('pommel', 'sword', [0, -3.6, 0], [cube([-0.9, -4.6, -0.9], [1.8, 1.8, 1.8], 2, [0, 45, 0], [0, -3.7, 0]),
  cube([-0.4, -5.6, -0.4], [0.8, 1.2, 0.8], 3, [0, 45, 0], [0, -5, 0])]);
bone('grip', 'sword', [0, 0, 0], [cube([-0.7, -3, -0.7], [1.4, 6, 1.4], 0)]);
bone('guard', 'sword', [0, 3.6, 0], [cube([-3.2, 3, -0.9], [6.4, 1.4, 1.8], 1),
  cube([2.6, 3.4, -0.6], [1.2, 3.2, 1.2], 2, [0, 0, -35], [3.2, 3.6, 0]), cube([-3.8, 3.4, -0.6], [1.2, 3.2, 1.2], 2, [0, 0, 35], [-3.2, 3.6, 0]),
  cube([-0.8, 2.6, -1.1], [1.6, 2.2, 2.2], 3, [45, 0, 0], [0, 3.7, 0])]);
// The blade: six segments, narrowing toward the point, each a flat crystal slab with an edge shard.
const SEGMENTS = [];
for (let i = 0; i < 6; i++) {
  const y = 4.6 + i * 3.6, w = 2.6 - i * 0.22, d = 0.9 - i * 0.05;
  SEGMENTS.push('blade_' + i);
  bone('blade_' + i, 'sword', [0, y + 1.8, 0], [cube([-w / 2, y, -d / 2], [w, 3.6, d], i % 2 ? 2 : 1),
    cube([w / 2 - 0.2, y + 0.6, -0.25], [0.6, 2.2, 0.5], 3, [0, 0, -18], [w / 2, y + 1.7, 0])]);
}
bone('tip', 'sword', [0, 26.2, 0], [cube([-0.9, 26.2, -0.35], [1.8, 1.8, 0.7], 2, [0, 0, 45], [0, 27.1, 0]),
  cube([-0.4, 27.6, -0.25], [0.8, 1.6, 0.5], 3)]);
const PIECES = ['pommel', 'grip', 'guard', ...SEGMENTS, 'tip'];
fs.writeFileSync(path.join(root, 'geo/blood_sword.geo.json'), JSON.stringify({ format_version: '1.12.0', 'minecraft:geometry': [{
  description: { identifier: 'geometry.blood_sword', texture_width: 32, texture_height: 32,
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
  scale: kf([[0, [1, 1, 1]], [0.1 + i * 0.12, [1.08, 1, 1.08]], [0.3 + i * 0.12, [1, 1, 1]], [1.2, [1, 1, 1]]]) }])) };
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
