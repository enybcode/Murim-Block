// The Golden Bell's bell of light (client/GoldenBellSkins), drawn additively (bright on black): its texture runs round the
// bell (u) and from its top down to its lip (v): a soft gold body, brighter bands at its shoulder and waist, a bright lip,
// faint inscriptions in rows. textures/fx/golden_bell/bell.png (64 x 64).
// And the art's manual's icon, textures/item/golden_bell_manual.png: a saffron cover, a gold bell on it.
// Usage: node scripts/animation/golden-bell-fx-textures.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const N = 64, px = new Uint8Array(N * N * 4);
for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
  const v = (y + 0.5) / N, u = (x + 0.5) / N;
  let b = 0.16 + 0.1 * Math.sin(u * Math.PI * 16) * 0.3;
  for (const [at, w, k] of [[0.3, 0.02, 0.6], [0.58, 0.015, 0.45], [0.9, 0.035, 0.95]]) b = Math.max(b, k * Math.exp(-((v - at) ** 2) / (w * w)));
  // Rows of small marks between the bands, like an inscription.
  if (v > 0.38 && v < 0.52 && (x % 4 === 1) && (y % 3 !== 0)) b = Math.max(b, 0.32);
  b = Math.min(1, b);
  px.set([Math.round(255 * b), Math.round(196 * b), Math.round(84 * b), Math.round(255 * b)], (y * N + x) * 4);
}
const dir = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'fx', 'golden_bell');
fs.mkdirSync(dir, { recursive: true });
fs.writeFileSync(path.join(dir, 'bell.png'), kit.png(N, N, px));

const MANUAL_LOOK = { key: 'manual', c: { K: '#3a1e0c', S: '#c4671c', D: '#8f4512', G: '#ffd36e', Y: '#e0a83a', P: '#f2e6c8' } };
const MANUAL_ICON = [
  '................',
  '..KKKKKKKKKKKK..',
  '..KSSSSSSSSSSPK.',
  '..KSSSSGGSSSSPK.',
  '..KSSSSYYSSSSPK.',
  '..KSSSGYYGSSSPK.',
  '..KSSSGYYGSSSPK.',
  '..KSSGYYYYGSSPK.',
  '..KSGGGGGGGGSPK.',
  '..KSSSSGGSSSSPK.',
  '..KSSSSSSSSSSPK.',
  '..KDDDDDDDDDDPK.',
  '..KSSSSSSSSSSPK.',
  '..KSSSSSSSSSSPK.',
  '..KKKKKKKKKKKKD.',
  '................',
];
const icon = new Uint8Array(16 * 16 * 4);
MANUAL_ICON.forEach((row, y) => [...row].forEach((l, x) => { if (l === '.') return; const c = kit.colour(l, MANUAL_LOOK, 'north', x, y, 5); if (c) icon.set(c, (y * 16 + x) * 4); }));
const assets = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock');
fs.writeFileSync(path.join(assets, 'textures', 'item', 'golden_bell_manual.png'), kit.png(16, 16, icon));
fs.writeFileSync(path.join(assets, 'models', 'item', 'golden_bell_manual.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/golden_bell_manual' } }) + '\n');
console.log('wrote the Golden Bell\'s bell texture and its manual\'s icon');
