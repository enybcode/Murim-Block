// The pill furnace (丹炉, docs/cultivation/alchemy.md), as the Murim webtoons draw it (Enzo, 18/10/2026): a bronze-gold
// furnace round as a ball on three beast legs, a domed lid crowned with a flame finial, ring handles on its sides, a
// band of formation runes round its belly that light up as it refines, the fire under it on a glowing array. Bigger
// than a block (some 1.6 blocks tall). Voxels at 16 texels a block, as the plants (plant-kit.cjs).
//   bones: array (the formation on the ground), fire, body (legs, belly, handles), runes (the glowing band), lid (lifts
//   off at the end), finial, pills (fly out at the end), light (the column of light out of it), smoke.
// Animations: "idle" (embers, the runes asleep), "refine" (the fire roaring, the runes pulsing, the lid rattling, smoke),
// "finish" (the lid flies up in a column of light, the pills rise out of it, the lid falls back).
// Usage: node scripts/plants/cauldron-model.cjs
const { Plant, sampled } = require('./plant-kit.cjs');

const P = new Plant('alchemy_cauldron', 1, 1, 128);
const S = [0];
const shade = (hexc, n) => {
  const k = n[1] > 0 ? 1.12 : n[1] < 0 ? 0.55 : n[0] > 0 || n[2] < 0 ? 0.92 : 0.76;
  return '#' + [1, 3, 5].map(i => Math.max(0, Math.min(255, Math.round(parseInt(hexc.slice(i, i + 2), 16) * k))).toString(16).padStart(2, '0')).join('');
};
const GOLD = '#a8742a', GOLD_L = '#e0b050', GOLD_D = '#5a3612', DARK = '#2a1a0c', RUNE = '#ffb040', JADE = '#3a8a6a';
/** Bronze-gold, a little worn: lighter on its edges and highs, darker in its hollows. */
const metal = (p, n, c, t) => shade((t.u + t.v * 2 + t.id) % 17 === 0 ? GOLD_L : (t.u * 3 + t.v + t.id) % 23 === 0 ? GOLD_D : GOLD, n);
const bone = (name, parent, pivot) => P.bone(S, name, parent, pivot);
const box = (b, size, c, paint, axis = 'y', deg = 0, glow) => P.box(S, b, size, c, paint, axis, deg, glow);
const own = () => true;

bone('array', 'root', [0, 0, 0]);
bone('fire', 'root', [0, 1, 0]);
bone('body', 'root', [0, 0, 0]);
bone('runes', 'body', [0, 11, 0]);
bone('lid', 'body', [0, 18, 0]);
bone('finial', 'lid', [0, 23, 0]);
bone('pills', 'body', [0, 16, 0]);
bone('light', 'body', [0, 18, 0]);
bone('smoke', 'body', [0, 22, 0]);

// The formation on the ground: a ring of runes and eight trigram marks, glowing.
for (let k = 0; k < 16; k++) {
  const a = k * Math.PI / 8, r = 10;
  box('array', [k % 2 ? 1 : 2, 0.3, k % 2 ? 2 : 1], [Math.round(Math.cos(a) * r), 0.15, Math.round(Math.sin(a) * r)], () => RUNE, 'y', 0, own);
}
for (let k = 0; k < 8; k++) {
  const a = k * Math.PI / 4 + Math.PI / 8, r = 7.5;
  box('array', [2, 0.3, 1], [Math.round(Math.cos(a) * r), 0.15, Math.round(Math.sin(a) * r)], () => '#ffd070', 'y', k % 2 ? 45 : 0, own);
}
// The fire under it: flames in tongues, glowing.
const flame = (p) => (p[1] > 5 ? '#ffe680' : p[1] > 3.2 ? '#ffa030' : '#e8481a');
box('fire', [4, 5, 4], [0, 3, 0], flame, 'y', 0, own);
box('fire', [3, 4, 3], [2.5, 2.5, 1], flame, 'y', 45, own);
box('fire', [3, 4, 3], [-2.5, 2.5, -1], flame, 'y', 22.5, own);
box('fire', [2, 3, 2], [0.5, 2, -2.5], flame, 'y', -22.5, own);

// Three beast legs: a thigh out of the belly, a curved shin, a clawed paw.
for (const a of [90, 210, 330]) {
  const r = a * Math.PI / 180, cx = Math.cos(r), cz = Math.sin(r);
  box('body', [3, 3, 3], [Math.round(cx * 6), 7.5, Math.round(cz * 6)], metal);
  box('body', [2, 4, 2], [Math.round(cx * 7), 4.5, Math.round(cz * 7)], metal);
  box('body', [4, 1.5, 4], [Math.round(cx * 7.5), 0.75, Math.round(cz * 7.5)], (p, n) => shade(GOLD_D, n));
  box('body', [1, 1, 1], [Math.round(cx * 9), 0.5, Math.round(cz * 9)], (p, n) => shade(GOLD_L, n));
}
// The belly, round as a ball: layers, the corners cut, widest at its middle.
const BELLY = [[7.5, 4.5], [8.5, 6], [9.5, 7], [10.5, 7.5], [11.5, 8], [12.5, 8], [13.5, 7.5], [14.5, 7], [15.5, 6], [16.5, 5]];
for (const [y, w] of BELLY) {
  const cut = w >= 6 ? 2 : 1;
  box('body', [w * 2, 1, w * 2 - cut * 2], [0, y, 0], metal);
  box('body', [w * 2 - cut * 2, 1, w * 2], [0, y, 0], metal);
  if (w >= 7) box('body', [w * 2 - 2, 1, w * 2 - 2], [0, y, 0], metal, 'y', 45);
}
// The band of formation runes round its widest part (its own bone: they pulse as it refines).
for (let k = 0; k < 16; k++) {
  const a = k * Math.PI / 8, r = 8.1;
  box('runes', [k % 2 ? 1 : 2, 2, k % 2 ? 2 : 1], [Math.cos(a) * r, 12, Math.sin(a) * r], () => (k % 4 === 0 ? '#ffe080' : RUNE), 'y', 0, own);
}
// The ring handles on its sides (east and west), and two jade studs front and back.
for (const s of [-1, 1]) {
  box('body', [1, 1, 5], [s * 8.5, 16, 0], (p, n) => shade(GOLD_L, n));
  box('body', [1, 4, 1], [s * 9.5, 14.5, -2], (p, n) => shade(GOLD, n));
  box('body', [1, 4, 1], [s * 9.5, 14.5, 2], (p, n) => shade(GOLD, n));
  box('body', [1, 1, 5], [s * 9.5, 12.5, 0], (p, n) => shade(GOLD_L, n));
  box('body', [2, 2, 1], [0, 12, s * 8.4], (p, n) => shade(JADE, n));
}
// The neck, dark inside.
box('body', [10, 1, 10], [0, 17.5, 0], (p, n) => (n[1] > 0 ? DARK : shade(GOLD_D, n)));
// The domed lid: a broad rim, then narrowing up, vents round it, and its flame finial.
const LID = [[18.5, 6], [19.5, 5.5], [20.5, 4.5], [21.5, 3]];
for (const [y, w] of LID) {
  box('lid', [w * 2, 1, w * 2 - 2], [0, y, 0], metal);
  box('lid', [w * 2 - 2, 1, w * 2], [0, y, 0], metal);
}
for (const [x, z] of [[4, 0], [-4, 0], [0, 4], [0, -4]]) box('lid', [1, 0.5, 1], [x, 20.05, z], () => DARK);
box('finial', [2, 1, 2], [0, 22.5, 0], (p, n) => shade(GOLD_L, n));
box('finial', [1, 2, 1], [0, 24, 0], (p, n) => shade('#e8c060', n));
box('finial', [3, 1, 1], [0, 23.5, 0], (p, n) => shade(GOLD_L, n));
box('finial', [1, 1, 3], [0, 23.5, 0], (p, n) => shade(GOLD_L, n));
// The pills inside (out at the finish), a column of light, smoke.
for (const [x, z] of [[-1.5, 0], [1.5, 0.5], [0, -1.5]]) box('pills', [1.5, 1.5, 1.5], [x, 17, z], () => '#ffe9a0', 'y', 0, own);
box('light', [6, 16, 6], [0, 26, 0], () => '#fff4c0', 'y', 0, own);
for (const [x, z, y] of [[3, 3, 22], [-3, -2, 23.5], [2, -3, 25]]) box('smoke', [2, 2, 2], [x, y, z], () => '#d8d0c8');

// ---------------------------------------------------------------- animations

const w = (t, len) => 2 * Math.PI * t / len;
const zero = len => sampled(len, len, () => [0, 0, 0]);
const animations = {
  idle: { loop: true, animation_length: 2, bones: {
    fire: { scale: sampled(2, 0.1, t => { const k = 0.4 + 0.06 * Math.sin(w(t, 2) * 3); return [k, k * (1 + 0.2 * Math.sin(w(t, 2) * 5)), k]; }) },
    runes: { scale: sampled(2, 2, () => [0.98, 0.98, 0.98]) },
    array: { scale: sampled(2, 2, () => [0.9, 1, 0.9]) },
    pills: { scale: zero(2) }, light: { scale: zero(2) }, smoke: { scale: zero(2) },
  } },
  refine: { loop: true, animation_length: 2, bones: {
    fire: { scale: sampled(2, 0.1, t => { const k = 1 + 0.15 * Math.sin(w(t, 2) * 4); return [k, k * (1 + 0.3 * Math.sin(w(t, 2) * 6)), k]; }) },
    runes: { scale: sampled(2, 0.1, t => { const k = 1 + 0.04 * Math.sin(w(t, 2) * 2); return [k, 1 + 0.25 * Math.sin(w(t, 2) * 2), k]; }) },
    array: { rotation: sampled(2, 0.1, t => [0, -45 * t, 0]) },
    lid: { position: sampled(2, 0.05, t => [0, Math.max(0, Math.sin(w(t, 2) * 6)) * 0.4, 0]), rotation: sampled(2, 0.05, t => [Math.sin(w(t, 2) * 6) * 1.5, 0, Math.cos(w(t, 2) * 5) * 1.5]) },
    smoke: { position: sampled(2, 0.1, t => [0, t * 2, 0]), scale: sampled(2, 0.1, t => { const k = 1 - 0.4 * t / 2; return [k, k, k]; }) },
    pills: { scale: zero(2) }, light: { scale: zero(2) },
  } },
  finish: { loop: false, animation_length: 3, bones: {
    fire: { scale: sampled(3, 0.1, t => { const k = t < 0.6 ? 1.4 : Math.max(0.4, 1.4 - (t - 0.6)); return [k, k, k]; }) },
    runes: { scale: sampled(3, 0.1, t => { const k = t < 0.8 ? 1.06 : 1; return [k, t < 0.8 ? 1.5 : 1, k]; }) },
    lid: { position: sampled(3, 0.05, t => [0, t < 0.4 ? t * 30 : t < 2.2 ? 12 - (t - 0.4) * 0.5 : Math.max(0, 11 - (t - 2.2) * 14), 0]),
      rotation: sampled(3, 0.05, t => [0, t < 2.3 ? t * 360 : 828, t < 2.3 ? 8 * Math.sin(t * 6) : 0]) },
    light: { scale: sampled(3, 0.05, t => { const k = t < 0.3 ? t / 0.3 : t < 1.6 ? 1 : Math.max(0, 1 - (t - 1.6)); return [k, k * 1.2, k]; }) },
    pills: { position: sampled(3, 0.05, t => [0, t < 0.4 ? 0 : t < 1.4 ? (t - 0.4) * 9 : 9 + Math.sin((t - 1.4) * 4) * 0.5, 0]),
      rotation: sampled(3, 0.05, t => [0, t * 240, 0]), scale: sampled(3, 0.05, t => { const k = t < 0.4 ? 0 : t < 2.6 ? 1 : Math.max(0, 1 - (t - 2.6) * 2.5); return [k, k, k]; }) },
    smoke: { scale: zero(3) },
  } },
};
P.write([{ key: 'bronze', c: {} }], animations);
