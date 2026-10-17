// The herbs at maturity (docs/cultivation/plants.md), in the chrysanthemum's style (chrysanthemum.cjs): voxels at
// Minecraft's 16 texels a block, stems a pixel wide, painted flat with a little shade by face. One stage each for now
// (the mature plant), to be grown into four like the chrysanthemum once chosen.
// Usage: node scripts/plants/herb-models.cjs [herb...]
const { Plant, sampled } = require('./plant-kit.cjs');

/** A colour shaded by the face it is on: lit from above, darker below and on two sides. */
const shade = (hexc, n) => {
  const k = n[1] > 0 ? 1.08 : n[1] < 0 ? 0.62 : n[0] > 0 || n[2] < 0 ? 0.92 : 0.8;
  const c = [1, 3, 5].map(i => Math.max(0, Math.min(255, Math.round(parseInt(hexc.slice(i, i + 2), 16) * k))));
  return '#' + c.map(x => x.toString(16).padStart(2, '0')).join('');
};
const flat = col => (p, n) => shade(col, n);
/** Two colours in a checker by texel (a little texture). */
const speckle = (a, b) => (p, n, c, t) => shade((t.u * 3 + t.v * 5 + t.id) % 4 === 0 ? b : a, n);

/**
 * How much a herb moves, by its rarity (Enzo, 18/10/2026: "more or less, as it is rare"):
 *   common     every stem and branch sways in the wind, each at its own pace;
 *   uncommon   and its berries shiver, its pads float;
 *   rare       and its flower breathes (opens and closes a little), a part of it glows, pulsing;
 *   very rare  and its glowing parts pulse stronger, motes of Qi rise round it all the time.
 */
const TIERS = { common: { sway: 3.5 }, uncommon: { sway: 3.5 }, rare: { sway: 2.5 }, very_rare: { sway: 1.5 } };
const RARITY = { bamboo_shoot: 'common', astragalus_root: 'common', kunlun_sedge: 'common', fern_frond: 'common', goji_berries: 'common',
  sea_buckthorn_berries: 'uncommon', lotus_seed_pod: 'uncommon', cistanche: 'rare', rhododendron_blossom: 'rare', snow_lotus: 'rare',
  ginseng_root: 'rare', lingzhi: 'very_rare' };
const LOOP = 4;

function make(name, build) {
  const P = new Plant(name, 1, 1, 64);
  const S = [0], tier = RARITY[name];
  /** Every bone made, its kind (branch, berries, float, bloom, glow, motes) and its pivot. */
  const bones = [];
  let current = 'body';
  P.bone(S, 'body', 'root', [0, 0, 0]);
  bones.push({ name: 'body', kind: 'body', pivot: [0, 0, 0] });
  /** A new bone of this kind, child of the bone in use (or of `parent`); the boxes after it go on it. */
  const group = (kind, pivot, parent = current) => {
    const bn = kind + bones.length;
    P.bone(S, bn, parent, pivot);
    bones.push({ name: bn, kind, pivot });
    current = bn;
    return bn;
  };
  /** Back to the plant's body (or a given bone) for the boxes after. */
  const on = bn => { current = bn || 'body'; };
  const glowOf = kind => (kind === 'glow' ? (pt, c) => '#ffffff' : null);
  const box = (size, c, paint, axis = 'y', deg = 0, glow) => P.box(S, current, size, c, paint, axis, deg,
    glow || glowOf(bones.find(b => b.name === current).kind) || undefined);
  /** A branch from `base`, on its own bone (it sways): `len` long, tilted `deg` about `axis` (z: toward -x; x: toward +z). */
  const stick = (base, len, paint, axis = 'z', deg = 0, w = 1) => {
    on('body');
    group('branch', base);
    const a = deg * Math.PI / 180, dir = axis === 'z' ? [-Math.sin(a), Math.cos(a), 0] : [0, Math.cos(a), Math.sin(a)];
    box([w, len, w], base.map((v, i) => v + dir[i] * len / 2), paint, axis, deg);
    return base.map((v, i) => v + dir[i] * len);
  };
  build({ box, stick, group, on, here: () => current, P });
  if (tier === 'very_rare') {
    on('body');
    group('motes', [0, 0, 0]);
    for (let k = 0; k < 6; k++) {
      const a = k * Math.PI / 3, r = 5 + (k % 2);
      box([1, 1, 1], [Math.cos(a) * r, 3 + (k % 3) * 2.5, Math.sin(a) * r], () => '#fff4c8', 'y', 0, () => '#fff4c8');
    }
  }
  // The animation: one loop of LOOP seconds, every bone moving as its kind and the herb's rarity say.
  const amp = TIERS[tier].sway, anim = {};
  bones.forEach((b, i) => {
    const ph = i * 1.7, w = t => 2 * Math.PI * t / LOOP;
    if (b.kind === 'branch') anim[b.name] = { rotation: sampled(LOOP, 0.2, t => [amp * 0.6 * Math.sin(w(t) + ph * 1.3), 0, amp * Math.sin(w(t) + ph)]) };
    if (b.kind === 'berries') anim[b.name] = { rotation: sampled(LOOP, 0.1, t => [5 * Math.sin(w(t) * 4 + ph), 0, 5 * Math.sin(w(t) * 3 + ph * 2)]) };
    if (b.kind === 'float') anim[b.name] = { position: sampled(LOOP, 0.2, t => [0, 0.35 * Math.sin(w(t) + ph), 0]),
      rotation: sampled(LOOP, 0.2, t => [2 * Math.sin(w(t) + ph), 4 * Math.sin(w(t) * 0.5 + ph), 2 * Math.cos(w(t) + ph)]) };
    if (b.kind === 'bloom') anim[b.name] = { scale: sampled(LOOP, 0.2, t => { const k = 1 + 0.07 * Math.sin(w(t) + ph); return [k, 1 + 0.04 * Math.sin(w(t) + ph), k]; }) };
    if (b.kind === 'glow') { const g = tier === 'very_rare' ? 0.12 : 0.07; anim[b.name] = { scale: sampled(LOOP, 0.2, t => { const k = 1 + g * Math.sin(w(t) * 2 + ph); return [k, k, k]; }) }; }
    if (b.kind === 'motes') anim[b.name] = { rotation: sampled(LOOP, 0.2, t => [0, -90 * t, 0]), position: sampled(LOOP, 0.2, t => [0, 1.2 * Math.sin(w(t) * 2), 0]) };
  });
  P.write([{ key: 'default', c: {} }], { sway: { loop: true, animation_length: LOOP, bones: anim } });
}

const GREEN = '#4f7d2c', GREEN_D = '#2f5419', GREEN_L = '#7aa845';

const HERBS = {
  // Bamboo shoots: three cones of sheaths out of the ground, brown-striped, green at the tips; each sways a little.
  bamboo_shoot({ box, group, on }) {
    for (const [x, z, h] of [[0, 0, 7], [3.5, 2.5, 5], [-3, 3, 3]]) {
      on('body'); group('branch', [x, 0, z]);
      for (let i = 0; i < h; i++) {
        const w = Math.max(1, Math.round((1 - i / h) * 4));
        box([w, 1, w], [x, i + 0.5, z], (p, n, c, t) => shade(i >= h - 2 ? GREEN_L : (i + t.u) % 3 === 0 ? '#5e461c' : '#8a6a2e', n));
      }
    }
  },
  // Astragalus (huang qi): arching stems of paired leaflets, pale yellow pea flowers at their tops.
  astragalus_root({ box, stick }) {
    for (const [axis, deg, len] of [['z', 22.5, 9], ['z', -22.5, 8], ['x', 22.5, 8], ['x', -22.5, 7], ['z', 0, 10]]) {
      const top = stick([0, 0, 0], len, flat(GREEN), axis, deg);
      for (let k = 2; k < len; k += 2) box([3, 0.6, 1], top.map(v => v * k / len), flat(k % 4 ? GREEN_L : GREEN), 'y', axis === 'z' ? 0 : 45);
      for (let k = 0; k < 3; k++) box([1, 1, 1], [top[0], top[1] + k * 0.8, top[2]], flat(k % 2 ? '#f2e07a' : '#e6c84a'));
    }
  },
  // Kunlun sedge: a tuft of thin blades leaning out every way, green going straw at the tips; each blade its own way.
  kunlun_sedge({ stick }) {
    const blades = [['z', 0, 9], ['z', 22.5, 8], ['z', -22.5, 8], ['x', 22.5, 7], ['x', -22.5, 9], ['z', 45, 6], ['z', -45, 6], ['x', 45, 5], ['x', -45, 6]];
    blades.forEach(([axis, deg, len], i) => stick([((i * 7) % 3) - 1, 0, ((i * 5) % 3) - 1], len,
      (p, n) => shade(p[1] > len * 0.7 ? '#c4cf8a' : i % 2 ? '#8fa860' : '#5f7a3a', n), axis, deg));
  },
  // A karst fern: fronds arching out of a crown, leaflets along them, a fiddlehead uncurling in the middle.
  fern_frond({ box, stick, group, on }) {
    for (const [axis, deg] of [['z', 45], ['z', -45], ['x', 45], ['x', -45], ['z', 22.5]]) {
      const top = stick([0, 0.5, 0], 9, flat(GREEN_D), axis, deg);
      for (let k = 2; k < 9; k++) {
        const t = k / 9, at = top.map((v, i) => v * t + (i === 1 ? 0.5 * (1 - t) : 0)), w = Math.max(1, Math.round((1 - t) * 4) + 1);
        box(axis === 'z' ? [1, 0.6, w] : [w, 0.6, 1], at, flat(k % 2 ? '#4e8a32' : '#7ab84a'));
      }
    }
    on('body'); group('branch', [0, 0, 0]);
    box([1, 4, 1], [0, 2, 0], flat('#4e8a32'));
    box([1, 1, 3], [0, 4.5, 1], flat('#7ab84a'));
    box([1, 2, 1], [0, 5.5, 2], flat('#7ab84a'));
    box([1, 1, 2], [0, 6.5, 1.5], flat('#4e8a32'));
  },
  // Sea buckthorn (uncommon): brown branches, narrow silver leaves, dense clusters of orange berries that shiver.
  sea_buckthorn_berries({ box, stick, group, on, here }) {
    for (const [axis, deg, len] of [['z', 22.5, 11], ['z', -22.5, 10], ['x', 22.5, 10], ['x', -22.5, 9], ['z', 0, 12]]) {
      const top = stick([0, 0, 0], len, flat('#5a4026'), axis, deg);
      const branch = here();
      for (let k = 3; k <= len; k++) {
        const at = top.map(v => v * k / len);
        if (k % 2) {
          group('berries', at);
          for (const d of [-1, 1]) box([1, 1, 1], [at[0] + (axis === 'x' ? d : 0), at[1], at[2] + (axis === 'z' ? d : 0)], flat(k % 4 === 1 ? '#f08a14' : '#c25e08'));
          on(branch);
        }
      }
      // The leaves, on the branch.
      for (let k = 4; k <= len; k += 2) box(axis === 'z' ? [3, 0.5, 1] : [1, 0.5, 3], top.map(v => v * k / len), flat('#9fb39a'));
    }
  },
  // The lotus (uncommon): two pads floating, bobbing on the water; the seed pod and a pink bud sway on their stalks.
  lotus_seed_pod({ box, stick, group, on }) {
    group('float', [-2, 0, -1]);
    box([9, 0.5, 9], [-2, 0.25, -1], speckle('#4a8a32', '#3a7028'));
    on('body'); group('float', [3.5, 0, 3]);
    box([7, 0.5, 7], [3.5, 0.75, 3], speckle('#5a9a3a', '#3a7028'));
    const top = stick([0, 0, 0], 10, flat('#5a8a34'), 'z', 0);
    box([4, 2, 4], [top[0], top[1] + 1, top[2]], (p, n, c, t) => (n[1] > 0 ? ((t.u + t.v) % 2 ? '#2a3a18' : '#c9d27a') : shade('#6fa046', n)));
    const bud = stick([2, 0, -2], 8, flat('#5a8a34'), 'x', -22.5);
    box([2, 3, 2], [bud[0], bud[1] + 1.5, bud[2]], (p, n) => shade(p[1] > bud[1] + 2 ? '#f8b8d0' : '#e070a0', n));
  },
  // Cistanche (rare): fleshy spikes out of the sand, scaled yellow-brown; their purplish florets breathe.
  cistanche({ box, group, on }) {
    for (const [x, z, h] of [[0, 0, 9], [3, 2, 6]]) {
      on('body'); group('branch', [x, 0, z]);
      for (let i = 0; i < h - 2; i++) box([3, 1, 3], [x, i + 0.5, z], (p, n, c, t) => shade((t.u + i) % 2 ? '#d8a854' : '#a87430', n));
      group('bloom', [x, h - 2, z]);
      for (let i = h - 2; i < h; i++) box([2, 1, 2], [x, i + 0.5, z], (p, n, c, t) => shade((t.u + i) % 2 ? '#8a4a5a' : '#b06a7a', n));
    }
  },
  // Rhododendron (rare): a small shrub of dark leathery leaves in rosettes, clusters of pink bells that breathe.
  rhododendron_blossom({ box, stick, group }) {
    for (const [axis, deg, len] of [['z', 22.5, 7], ['z', -22.5, 7], ['x', 22.5, 6], ['x', -22.5, 7]]) {
      const top = stick([0, 0, 0], len, flat('#5a4026'), axis, deg);
      for (const d of [0, 90]) box(d ? [1, 0.6, 5] : [5, 0.6, 1], [top[0], top[1] - 1, top[2]], flat('#3f6b2a'));
      group('bloom', top);
      for (const [dx, dy, dz] of [[0, 0, 0], [1, 0, 0], [-1, 0, 0], [0, 0, 1], [0, 0, -1], [0, 1, 0]])
        box([1, 1, 1], [top[0] + dx, top[1] + dy, top[2] + dz], flat((dx + dz + dy) % 2 ? '#d8406a' : '#ff9ab8'));
    }
  },
  // The snow lotus (rare): a rosette of woolly grey-green leaves, a globe of papery bracts that breathes, a purple heart glowing.
  snow_lotus({ box, group }) {
    for (const d of [0, 45]) for (const s of [[9, 1], [1, 9]]) box([s[0], 0.6, s[1]], [0, 0.4, 0], flat('#8fa88a'), 'y', d);
    group('bloom', [0, 1, 0]);
    for (const [h, w] of [[1, 1.5], [2, 2.5], [3, 2.5], [4, 2.5], [5, 2], [6, 1.5]])
      box([w * 2, 1, w * 2], [0, h + 0.5, 0], (p, n, c, t) => shade(h >= 5 ? '#f4f6ee' : (t.u + h) % 2 ? '#e4ead8' : '#a8c49a', n));
    group('glow', [0, 7.5, 0]);
    box([1, 1, 1], [0, 7.5, 0], flat('#9a6acc'));
  },
  // Ginseng (rare): a stalk with a whorl of three leaves of five leaflets; its red berries glow, pulsing.
  ginseng_root({ box, stick, group }) {
    stick([0, 0, 0], 8, flat(GREEN), 'z', 0);
    for (const a of [0, 120, 240]) {
      const r = a * Math.PI / 180, dx = Math.cos(r), dz = Math.sin(r);
      for (let k = 1; k <= 5; k++) box([1, 0.5, 1], [Math.round(dx * k), 8, Math.round(dz * k)], flat(GREEN));
      for (const [s, l] of [[-1, 3], [0, 4], [1, 3]]) box([2, 0.5, 2], [Math.round(dx * 5 - dz * s * 1.5), 8.2, Math.round(dz * 5 + dx * s * 1.5)], flat(l === 4 ? GREEN_L : '#5e9a36'));
    }
    group('glow', [0, 9, 0]);
    for (const [dx, dy, dz] of [[0, 0, 0], [1, 0, 0], [0, 0, 1], [-1, 0, 0], [0, 0, -1], [0, 1, 0]]) box([1, 1, 1], [dx, 9 + dy, dz], flat('#e02a2a'));
  },
  // Lingzhi (very rare): two glossy red-brown fan caps with pale rims on an old stump; the caps glow and pulse, motes rise.
  lingzhi({ box, group, on }) {
    box([6, 5, 6], [0, 2.5, 0], (p, n) => (n[1] > 0 ? shade('#a07a4a', n) : shade('#5a4026', n)));
    const cap = (w, d, c) => (p, n, cc, t) => (n[1] > 0 ? ((t.u === 0 || t.u === w - 1 || t.v === 0 || t.v === d - 1) ? '#e8b060' : (t.u + t.v) % 3 ? '#9a2a12' : '#d0602a') : shade('#6a1a0a', n));
    const rim = (w, d) => (pt, cc, t) => (t.f === 'up' && (t.u === 0 || t.u === w - 1 || t.v === 0 || t.v === d - 1) ? '#ffd890' : null);
    group('glow', [3, 4, 0]);
    box([7, 1, 5], [4, 4, 0], cap(7, 5), 'y', 0, rim(7, 5));
    on('body'); group('glow', [-3, 6, 1]);
    box([5, 1, 4], [-3, 6, 1], cap(5, 4), 'y', 0, rim(5, 4));
    box([1, 2, 1], [-3, 5, 1], flat('#6a1a0a'));
  },
  // Goji: arching thin branches with narrow leaves, small red berries hanging, a few purple flowers.
  goji_berries({ box, stick }) {
    for (const [axis, deg, len] of [['z', 45, 11], ['z', -45, 10], ['x', 45, 10], ['x', -45, 9], ['z', 22.5, 11]]) {
      const top = stick([0, 0, 0], len, flat('#8a7a5a'), axis, deg);
      for (let k = 3; k <= len; k += 2) {
        const at = top.map(v => v * k / len);
        box([1, 2, 1], [at[0], at[1] - 1.5, at[2]], (p, n) => shade(k % 4 === 1 ? '#e0301c' : '#a81a10', n));
        box(axis === 'z' ? [1, 0.5, 3] : [3, 0.5, 1], [at[0], at[1] + 0.3, at[2]], flat('#6a8a3a'));
      }
      box([1, 1, 1], [top[0], top[1] + 0.5, top[2]], flat('#9a6ac0'));
    }
  },
};

const only = process.argv.slice(2);
for (const [name, build] of Object.entries(HERBS)) if (!only.length || only.includes(name)) make(name, build);
module.exports = { HERBS };
