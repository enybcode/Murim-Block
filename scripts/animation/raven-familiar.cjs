// The Raven Messenger Art's familiar (martial/RavenFamiliarEntity, client/RavenFamiliarRenderer), the Shadow Raven Enzo
// chose (scripts/concepts/raven-familiars.cjs, "shadow"): a big black raven, a night-blue sheen, amber eyes, a messenger's
// tube ringed in gold on its back and a gold ring on a leg. In GeckoLib, about a parrot's size, with bones to move:
//   body, head (the beak, the eyes), tail, legs, wing_l / wing_r (folded on the back), spread_l / spread_r (the open
//   wings, hidden when it perches);
// one palette texture (textures/entity/raven_familiar.png, 16 shades a material; its _glowmask keeps the eyes and the
// gold), and its animations: perch (looking about, the tail flicking, now and then a shake of the wings) and fly (the
// open wings beating, the legs tucked).
// Usage: node scripts/animation/raven-familiar.cjs
const fs = require('fs');
const path = require('path');
const { png } = require('./mesh-kit.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock');
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the palette: a row per material, 16 shades

const COLOURS = { feather: '#262833', wing: '#1c1e27', sheen: '#3d5274', beak: '#33343a', leg: '#3a3a42', eye: '#f2b33d', glint: '#ffffff',
  ring: '#d9a83f', scroll: '#e9dcb4' };
const GLOWS = new Set(['eye', 'glint', 'ring']);
const NAMES = Object.keys(COLOURS), TEX = new Uint8Array(16 * 16 * 4), GLOW = new Uint8Array(16 * 16 * 4);
NAMES.forEach((n, row) => {
  const base = hex(COLOURS[n]);
  for (let s = 0; s < 16; s++) {
    const f = 0.55 + s * 0.05, c = [...base.map(v => Math.max(0, Math.min(255, Math.round(v * f)))), 255];
    TEX.set(c, (row * 16 + s) * 4);
    if (GLOWS.has(n)) GLOW.set(c, (row * 16 + s) * 4);
  }
});

// ---------------------------------------------------------------- bones and cubes

const BONES = {};
const order = [];
function bone(name, parent, pivot) {
  BONES[name] = { name, parent, pivot, cubes: [] };
  order.push(name);
}
/** A cube on a bone, of a material at a shade, from/to in model pixels (the crow faces -z, its feet at y 0). */
function box(b, mat, shade, from, to, rot, pivot) {
  const row = NAMES.indexOf(mat);
  if (row < 0) throw new Error('no material ' + mat);
  const c = { origin: from, size: [0, 1, 2].map(i => +(to[i] - from[i]).toFixed(3)),
    uv: Object.fromEntries(FACES.map(f => [f, { uv: [shade, row], uv_size: [1, 1] }])) };
  if (rot) { c.rotation = rot; c.pivot = pivot; }
  BONES[b].cubes.push(c);
}

bone('root', null, [0, 0, 0]);
bone('legs', 'root', [0, 2.4, 0.3]);
bone('body', 'root', [0, 4, 0.5]);
bone('head', 'body', [0, 5.6, -1.8]);
bone('tail', 'body', [0, 5, 4]);
bone('wing_l', 'body', [-1.9, 5.3, -0.8]);
bone('wing_r', 'body', [1.9, 5.3, -0.8]);
bone('spread_l', 'body', [-1.9, 5.3, 0.4]);
bone('spread_r', 'body', [1.9, 5.3, 0.4]);

// The legs and their three-toed feet, the gold ring on the right.
for (const s of [-1, 1]) {
  box('legs', 'leg', 6, [s * 0.9 - 0.25, 0, 0.1], [s * 0.9 + 0.25, 2.4, 0.6]);
  box('legs', 'leg', 4, [s * 0.9 - 0.25, 0, -1.2], [s * 0.9 + 0.25, 0.3, 0.6]);
  box('legs', 'leg', 4, [s * 0.9 - 0.7, 0, -0.6], [s * 0.9 + 0.7, 0.3, -0.2]);
}
box('legs', 'ring', 12, [0.6, 1.2, 0.0], [1.2, 1.6, 0.7]);
// The body: the breast forward, the back sloping to the tail; the messenger's tube on the back.
box('body', 'feather', 6, [-1.9, 2.3, -1.4], [1.9, 5.6, 3.0]);
box('body', 'feather', 8, [-1.7, 2.7, -2.3], [1.7, 5.3, -1.3]);
box('body', 'feather', 5, [-1.6, 4.9, 2.6], [1.6, 5.8, 4.4]);
box('body', 'feather', 4, [-1.5, 2.1, 0.6], [1.5, 2.6, 3.2]);
box('body', 'scroll', 10, [-0.6, 5.6, 0.3], [0.6, 6.5, 2.9]);
box('body', 'ring', 12, [-0.7, 5.5, 0.2], [0.7, 6.6, 0.55]);
box('body', 'ring', 12, [-0.7, 5.5, 2.65], [0.7, 6.6, 3.0]);
// The wedge tail.
for (const [x, len, sh] of [[-1.1, 4.0, 3], [-0.45, 4.8, 4], [0.45, 4.8, 4], [1.1, 4.0, 3]])
  box('tail', 'wing', sh, [x - 0.4, 4.7, 4.0], [x + 0.4, 5.2, 4.0 + len]);
// The folded wings: coverts glossed on top, the secondaries, the long primaries crossing over the tail.
for (const s of [-1, 1]) {
  const b = s < 0 ? 'wing_l' : 'wing_r', x0 = s > 0 ? 1.8 : -2.35, x1 = s > 0 ? 2.35 : -1.8;
  box(b, 'wing', 5, [x0, 2.9, -1.0], [x1, 5.5, 3.2]);
  box(b, 'sheen', 9, [x0 - (s > 0 ? 0 : 0.05), 4.6, -0.9], [x1 + (s > 0 ? 0.05 : 0), 5.55, 1.6]);
  box(b, 'wing', 3, [s * 1.7 - 0.35, 3.6, 3.0], [s * 1.7 + 0.35, 5.0, 5.4]);
  box(b, 'wing', 2, [s * 1.4 - 0.3, 3.9, 5.4], [s * 1.4 + 0.3, 4.9, 7.4]);
}
// The open wings: a broad arm of coverts glossed blue, then the long fingered primaries at the tip.
for (const s of [-1, 1]) {
  const b = s < 0 ? 'spread_l' : 'spread_r', X = x => s * x, lo = (a, c) => Math.min(X(a), X(c)), hi = (a, c) => Math.max(X(a), X(c));
  box(b, 'wing', 5, [lo(1.8, 6.5), 5.0, -1.0], [hi(1.8, 6.5), 5.5, 3.0]);
  box(b, 'sheen', 9, [lo(1.8, 5.5), 5.5, -0.9], [hi(1.8, 5.5), 5.65, 1.4]);
  box(b, 'wing', 4, [lo(6.5, 9.0), 5.05, -0.6], [hi(6.5, 9.0), 5.45, 2.6]);
  // Five primaries spread like fingers, longer toward the front.
  [[-0.6, 4.2], [0.2, 4.0], [1.0, 3.6], [1.8, 3.1], [2.5, 2.5]].forEach(([z, len], i) =>
    box(b, 'wing', 2 + (i % 2), [lo(9.0, 9.0 + len), 5.1, z - 0.3], [hi(9.0, 9.0 + len), 5.4, z + 0.3]));
  // The secondaries, a trailing edge behind the arm.
  box(b, 'wing', 3, [lo(1.8, 8.0), 5.05, 3.0], [hi(1.8, 8.0), 5.4, 4.4]);
}
// The head, the glossy crown, the shaggy throat, the heavy beak, the amber eyes with their glints.
box('head', 'feather', 7, [-1.35, 5.2, -4.1], [1.35, 7.8, -1.5]);
box('head', 'sheen', 10, [-1.25, 7.6, -3.9], [1.25, 7.95, -1.8]);
for (const [x, y] of [[-0.7, 4.6], [0, 4.3], [0.7, 4.6]]) box('head', 'feather', 4, [x - 0.4, y, -3.6], [x + 0.4, y + 1.0, -2.6]);
box('head', 'beak', 8, [-0.7, 6.0, -5.7], [0.7, 7.3, -4.0]);
box('head', 'beak', 10, [-0.45, 6.15, -6.6], [0.45, 7.0, -5.7]);
box('head', 'beak', 6, [-0.3, 5.9, -6.9], [0.3, 6.5, -6.4]);
box('head', 'beak', 5, [-0.55, 5.75, -5.4], [0.55, 6.1, -4.0]);
box('head', 'feather', 3, [-0.75, 6.9, -4.6], [0.75, 7.4, -3.9]);
for (const s of [-1, 1]) {
  const lo = s > 0 ? 1.35 : -1.41, hi = s > 0 ? 1.41 : -1.35;
  box('head', 'eye', 13, [lo, 6.7, -3.4], [hi, 7.3, -2.8]);
  box('head', 'glint', 15, [lo + s * 0.01, 7.05, -3.35], [hi + s * 0.01, 7.25, -3.15]);
}

const geo = {
  format_version: '1.12.0',
  'minecraft:geometry': [{
    description: { identifier: 'geometry.raven_familiar', texture_width: 16, texture_height: 16, visible_bounds_width: 2, visible_bounds_height: 2,
      visible_bounds_offset: [0, 0.5, 0] },
    bones: order.map(n => {
      const b = BONES[n], out = { name: n, pivot: b.pivot };
      if (b.parent) out.parent = b.parent;
      if (b.cubes.length) out.cubes = b.cubes;
      return out;
    }),
  }],
};

// ---------------------------------------------------------------- the animations

const r = (t, v) => [String(t), v];
const keys = list => Object.fromEntries(list.map(([t, v]) => [String(t), v]));
const anims = {
  'animation.raven_familiar.perch': {
    loop: true, animation_length: 6,
    bones: {
      spread_l: { scale: 0 }, spread_r: { scale: 0 },
      head: { rotation: keys([[0, [0, 0, 0]], [0.8, [0, 30, 0]], [1.6, [8, 30, 4]], [2.2, [0, 0, 0]], [3.0, [-6, -35, 0]], [3.8, [0, -35, -4]],
        [4.6, [12, 0, 0]], [4.8, [0, 0, 0]], [5.0, [12, 0, 0]], [5.2, [0, 0, 0]], [6, [0, 0, 0]]]) },
      tail: { rotation: keys([[0, [0, 0, 0]], [1.0, [8, 0, 0]], [1.2, [0, 0, 0]], [3.4, [0, 0, 0]], [3.55, [10, 6, 0]], [3.7, [0, 0, 0]], [6, [0, 0, 0]]]) },
      // A shake of the folded wings now and then.
      wing_l: { rotation: keys([[0, [0, 0, 0]], [2.4, [0, 0, 0]], [2.5, [0, 0, -18]], [2.6, [0, 0, 0]], [2.7, [0, 0, -14]], [2.8, [0, 0, 0]], [6, [0, 0, 0]]]) },
      wing_r: { rotation: keys([[0, [0, 0, 0]], [2.4, [0, 0, 0]], [2.5, [0, 0, 18]], [2.6, [0, 0, 0]], [2.7, [0, 0, 14]], [2.8, [0, 0, 0]], [6, [0, 0, 0]]]) },
      body: { position: keys([[0, [0, 0, 0]], [1.5, [0, 0.15, 0]], [3, [0, 0, 0]], [4.5, [0, 0.15, 0]], [6, [0, 0, 0]]]) },
    },
  },
  'animation.raven_familiar.fly': {
    loop: true, animation_length: 0.5,
    bones: {
      wing_l: { scale: 0 }, wing_r: { scale: 0 },
      spread_l: { rotation: keys([[0, [0, 0, -40]], [0.25, [0, 0, 38]], [0.5, [0, 0, -40]]]) },
      spread_r: { rotation: keys([[0, [0, 0, 40]], [0.25, [0, 0, -38]], [0.5, [0, 0, 40]]]) },
      legs: { rotation: [-70, 0, 0], position: [0, 1.2, 1.2] },
      body: { rotation: [-8, 0, 0], position: keys([[0, [0, 0.3, 0]], [0.25, [0, -0.3, 0]], [0.5, [0, 0.3, 0]]]) },
      tail: { rotation: keys([[0, [-6, 0, 0]], [0.25, [4, 0, 0]], [0.5, [-6, 0, 0]]]) },
      head: { rotation: [8, 0, 0] },
    },
  },
};

fs.writeFileSync(path.join(ASSETS, 'geo', 'raven_familiar.geo.json'), JSON.stringify(geo, null, 1) + '\n');
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'raven_familiar.png'), png(16, 16, TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'raven_familiar_glowmask.png'), png(16, 16, GLOW));
fs.writeFileSync(path.join(ASSETS, 'animations', 'raven_familiar.animation.json'), JSON.stringify({ format_version: '1.8.0', animations: anims }, null, 2) + '\n');
console.log('wrote the raven familiar:', order.map(n => n + ' ' + BONES[n].cubes.length).join(', '));
