// The Bandit Warlord's second phase (Enzo, 16/10/2026: "cut all the top off, only his legs, and in its place that",
// with a picture of a golden giant rising behind a goalkeeper): our own Murim take, the **Mountain God's Avatar**, a
// colossus of golden Qi rising from his waist, his own legs still under it. Mock-up first: a model to look at.
//   The legs: the warlord's own (bandit-warlord-3d.cjs), dark trousers and iron-plated boots.
//   The avatar: a burning waist where it springs from his belt; a huge bare torso of glowing gold, muscles traced in
//   orange (pecs, abs, the cut of the shoulders); arms twice as thick as his, fists like boulders; a head with a jutting
//   jaw, white burning eyes, a mouth wide open in a roar, fangs; and hair like a Super Saiyan's (Enzo: "better hair,
//   more Super Saiyan"): great tapering spikes of bright gold standing up and flaring out, a few swept back, two
//   locks falling over the brow.
// On the same biped skeleton (the game can swap the mesh at the second phase); drawn twice a man's size like him, it
// stands some seven blocks tall. Usage: node scripts/animation/warlord-avatar-3d.cjs [--port=9236] [--film=false]
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const kit = require('./mesh-kit.cjs');
const { isSide } = kit;
const { animation, groupsByName } = require('./blockbench-rig.cjs');
const warlord = require('./bandit-warlord-3d.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 256;
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];

/** His palette, and the avatar's: Y gold, L its light, O the orange of its lines, K the crest's red, V the eyes' white fire, Q the mouth, X the fangs, J the burning waist. */
const AVATAR = { key: 'avatar', c: { ...warlord.WARLORD.c,
  Y: '#ffd24a', L: '#fff1a6', O: '#d9781e', K: '#d8261c', V: '#fffdf0', Q: '#5a1606', X: '#fff8e0', J: '#ff9a2a',
  P: '#ffec3a', U: '#fffbd0', Z: '#b87a10' } };

// The legs: his own (thighs, shins and what is strapped on them).
// His own legs, doubled like the rest about his waist (Enzo: the legs, too small under so great a body, were what jarred):
// twice as long and as thick, his trousers and his iron-plated boots; the feet stand twelve texels below his old ones.
const twice = ([x, y, z]) => [x * 2, 12 + 2 * (y - 12), z * 2];
const PIECES = warlord.PIECES.filter(p => /^(Thigh|Leg)_/.test(p.bone)).map(p => ({ ...p, from: twice(p.from), to: twice(p.to),
  pivot: p.pivot && twice(p.pivot), hinge: p.hinge && { ...p.hinge, y: 12 + 2 * (p.hinge.y - 12) } }));
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });

/** Glowing muscle: gold, its light along the top, shaded at the edges (no speckles: a body of light). */
const glow = (f, u, v, w, h) => (f === 'up' ? 'L' : f === 'down' ? 'y' : v === 0 ? 'L' : isSide(f) && (u === 0 || u === w - 1) ? 'y' : 'Y');

// The face, 12 x 12, the viewer's view: brows knotted in fury, white burning eyes, a mouth wide open on its fangs.
const FACE = [
  'LLYYYYYYYYLL',
  'YYYYYYYYYYYY',
  'YOOOYYYYOOOY',
  'YYOOOYYOOOYY',
  'YYVVOYYOVVYY',
  'YYVVYYYYVVYY',
  'YYYYYyyYYYYY',
  'YOXXXXXXXXOY',
  'YOQQQQQQQQOY',
  'YOQQQQQQQQOY',
  'YYOXQQQQXOYY',
  'yYYOOOOOOYYy'];

// Its size (Enzo: "twice his old body, and the arms must not touch the ground"): his own upper body doubled about his
// waist (y 12), the same widths and heights twice over; only the forearms are shorter than twice, so the fists hang at
// his knees, clear of the ground.
const W = 12, up = y => W + 2 * (y - W);

// ---- the burning waist, where it springs from his belt
piece('Torso', 'jacket', 'waist_fire', [-9, 10.5, -5.5], [9, 13.5, 4.5], (f, u, v) => (v === 0 ? 'Y' : (u + v) % 3 === 0 ? 'J' : 'O'));

// ---- the trunk (Enzo's reference, 16/10: big simple volumes, the muscles drawn in the texture, not stacked cubes): a
// waist, a middle and a huge chest widening in a V; the pecs, the abs, the ribs and the back painted in shades of gold.
/** Shades of gold, light to deep: U, L, Y, y (Y darker), Z, and O for the deepest lines. */
const shade = k => (k > 0.8 ? 'U' : k > 0.6 ? 'L' : k > 0.38 ? 'Y' : k > 0.18 ? 'y' : 'Z');
/** A plain side: lit at the top, shaded down and at the edges. */
const plain = (f, u, v, w, h) => f === 'up' ? 'L' : f === 'down' ? 'Z'
  : shade(0.62 - 0.35 * (v / Math.max(1, h - 1)) - (isSide(f) && (u === 0 || u === w - 1) ? 0.22 : 0));
/** A muscle's roundness on a face: light at its top-left, dark at its rim (cx, cy, rx, ry in texels). */
const bulge = (u, v, cx, cy, rx, ry) => {
  const dx = (u - cx) / rx, dy = (v - cy) / ry, d = dx * dx + dy * dy;
  if (d > 1) return null;
  return 0.6 - 0.42 * d - 0.14 * dx - 0.18 * dy;
};

// The waist, the abs painted on its front: two columns of three blocks, deep lines between.
piece('Torso', 'torso', 'waist', [-9.5, 12.8, -6], [9.5, 20.5, 5.2], (f, u, v, w, h) => {
  if (f !== 'north') return plain(f, u, v, w, h);
  const c = (w - 1) / 2, col = u < c ? -1 : 1, du = Math.abs(u - c);
  if (du < 0.6 || du > 7.5) return du > 7.5 ? shade(0.25) : 'Z';
  const row = Math.floor(v / (h / 3)), vv = v - row * (h / 3);
  if (vv < 0.6) return 'Z';
  return shade(bulge(du, vv, 4, 1.2, 4.2, 2.2) ?? 0.3);
}, { hinge: { y: 20.5, bone: 'Chest' } });
// The middle: the ribs and the serratus at the sides, the lats beginning.
piece('Chest', 'torso', 'middle', [-12, 20.5, -6.4], [12, 26, 5.8], (f, u, v, w, h) => {
  if (f !== 'north') return plain(f, u, v, w, h);
  const c = (w - 1) / 2, du = Math.abs(u - c);
  if (du < 0.6) return 'Z';
  if (du > 7 && (v + Math.floor(du)) % 3 === 0) return 'Z';
  return shade(0.5 - 0.25 * (du / c) + 0.1 * (1 - v / h));
}, { hinge: { y: 20.5, bone: 'Torso' } });
// The chest: huge, the two pecs painted round and lit, the collarbones above, the deltoids' start at the corners.
piece('Chest', 'torso', 'chest', [-15, 26, -7], [15, 38, 6.4], (f, u, v, w, h) => {
  if (f === 'up') return 'L';
  if (f === 'south') {
    // The back: the spine, the shoulder blades.
    const c = (w - 1) / 2, du = Math.abs(u - c);
    if (du < 0.6) return 'Z';
    return shade(bulge(du, v, 7, 5, 6, 5) ?? 0.32);
  }
  if (f !== 'north') return plain(f, u, v, w, h);
  const c = (w - 1) / 2, du = Math.abs(u - c);
  if (du < 0.6 && v > 2) return 'Z';
  if (v === 1 && du > 2 && du < 11) return 'U';                        // the collarbones
  const pec = bulge(du, v, 7.5, 7.2, 7.2, 4.8);
  if (pec !== null) return shade(pec);
  if (v >= 11) return 'Z';                                              // the shadow under the pecs
  return shade(0.45);
}, { hinge: { y: 26, bone: 'Torso' } });
// The traps: a mound from the shoulders up the neck, the small head sunk into them.
piece('Chest', 'torso', 'traps', [-11, 38, -4.5], [11, 41, 5], (f, u, v, w, h) => (f === 'up' ? 'L' : shade(0.6 - 0.25 * v / h)));

// ---- the head: small on so great a body, sunk between the traps; the roar, the brow
// Resting on the traps (sunk one texel into them, never into the chest): the jaw stays clear of the pecs.
const HY = 40, HT = HY + 12;
piece('Head', 'head', 'head', [-6, HY, -6.6], [6, HT, 5.4], (f, u, v, w, h) => {
  if (f === 'north') return FACE[Math.floor(v * 12 / h)][Math.floor(u * 12 / w)];
  if (f === 'up') return 'P';
  return v < 3 ? 'P' : plain(f, u, v, w, h);
});
piece('Head', 'hat', 'brow', [-5.6, HY + 7.6, -7.4], [5.6, HY + 8.8, -6.4], (f, u, v) => (f === 'north' ? 'O' : 'Z'));

// The hair, a Super Saiyan's: spikes of bright gold (P, its light U at the tips, Z in the shade), each three blocks
// narrowing to its point, standing on the scalp and turned out about its root: up and flaring, a few swept back, two
// locks framing the face. A spike: its root's place [x, y, z] on the head, its length, its width at the root, turned
// [about x: back, about z: out to the side].
/** A spike: three blocks narrowing, all turned about its root by rot (degrees: x then z). */
function spike(name, root, length, width, rot) {
  const [rx, ry, rz] = root;
  const parts = [[0, 0.45, 1], [0.4, 0.75, 0.7], [0.7, 1, 0.4]];
  parts.forEach(([a, b, k], i) => {
    const w = width * k / 2;
    piece('Head', 'hat', name + '_' + i, [rx - w, ry + length * a, rz - w], [rx + w, ry + length * b, rz + w],
      (f, u, v, wd, h) => (i === 2 && v < 2 ? 'U' : isSide(f) && (u === 0 || u === wd - 1) ? 'Z' : f === 'down' ? 'Z' : 'P'),
      { pivot: root, rot: [rot[0], 0, rot[1]], turnCorner: c => turn(c, root, rot) });
  });
}
/** A corner turned about the root: first about x (a positive angle sweeps the point back), then about z (out to the side). */
function turn([x, y, z], [px, py, pz], [ax, az]) {
  let dx = x - px, dy = y - py, dz = z - pz;
  const a = ax * Math.PI / 180, b = az * Math.PI / 180;
  [dy, dz] = [dy * Math.cos(a) - dz * Math.sin(a), dy * Math.sin(a) + dz * Math.cos(a)];
  [dx, dy] = [dx * Math.cos(b) - dy * Math.sin(b), dx * Math.sin(b) + dy * Math.cos(b)];
  return [px + dx, py + dy, pz + dz];
}
const TOP = HT - 2;
// Standing up and flaring out, the tallest in the middle: thick wedges.
spike('spike_top', [0, TOP, 0], 20, 9, [12, 0]);
spike('spike_r1', [3.3, TOP, 0], 16, 8, [10, -28]);
spike('spike_l1', [-3.3, TOP, 0], 16, 8, [10, 28]);
spike('spike_r2', [5, TOP - 2, 1], 13, 7, [6, -58]);
spike('spike_l2', [-5, TOP - 2, 1], 13, 7, [6, 58]);
// Swept back.
spike('spike_back', [0, TOP - 1, 3], 16, 9, [55, 0]);
spike('spike_back_r', [3.8, TOP - 2, 3], 13, 7, [50, -30]);
spike('spike_back_l', [-3.8, TOP - 2, 3], 13, 7, [50, 30]);
// The front (Enzo: "do the hair in front a little better"): a Super Saiyan's fringe, short spikes jutting forward and
// up over the brow, the middle one longest; two locks framing the face at the temples.
spike('bang_mid', [0, TOP + 1, -5.5], 9, 4.2, [-58, 0]);
spike('bang_r', [3, TOP + 0.5, -5.2], 7.5, 3.8, [-50, -24]);
spike('bang_l', [-3, TOP + 0.5, -5.2], 7.5, 3.8, [-50, 24]);
spike('bang_rr', [5.2, TOP, -4.6], 6, 3.2, [-40, -50]);
spike('bang_ll', [-5.2, TOP, -4.6], 6, 3.2, [-40, 50]);
spike('lock_r', [6.2, TOP, -5], 7, 3, [-172, -6]);
spike('lock_l', [-6.2, TOP, -5], 7, 3, [-172, 6]);
// The scalp under them, gold.
piece('Head', 'hat', 'hairline', [-6.4, TOP - 1, -6.8], [6.4, TOP + 2.4, 6], (f, u, v, w, h) => (f === 'up' ? 'P' : v === h - 1 && u % 2 ? 'Z' : 'P'));

// ---- the arms: long and thick, four volumes each (the shoulder, the upper arm, the forearm, the fist), the muscles
// painted on them: the deltoid's round, the biceps and the triceps, the forearm's swell; the fists at his knees, never
// on the ground
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const elbow = up(15.4);
  /** A limb's side with a muscle painted round in its upper part (where it swells), lit on the outer edge. */
  const limb = (cy, ry) => (f, u, v, w, h) => {
    if (f === 'up') return 'L';
    if (f === 'down') return 'Z';
    const m = bulge(u, v, (w - 1) / 2, cy * h, w * 0.55, ry * h);
    return shade((m ?? 0.3) - (v === h - 1 ? 0.15 : 0));
  };
  const [d0, d1] = xs(14.6, 25.4), [a0, a1] = xs(15.4, 24.4), [b0, b1] = xs(15, 24.8), [f0, f1] = xs(14.6, 25.2);
  piece('Arm_' + side, part + 'Sleeve', 'deltoid_' + s, [d0, 29, -6.4], [d1, 39, 6.4], limb(0.35, 0.65));
  piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, elbow, -5.4], [a1, 29.2, 5.4], limb(0.4, 0.5), { hinge: { y: elbow, bone: 'Hand_' + side } });
  piece('Hand_' + side, part + 'Arm', 'forearm_' + s, [b0, 11, -5.2], [b1, elbow, 5.2], limb(0.3, 0.55), { hinge: { y: elbow, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'fist_' + s, [f0, 4, -5.6], [f1, 11, 5.6], (f, u, v, w, h) =>
    f === 'north' ? (v < 3 ? (u % 3 === 2 ? 'Z' : v === 0 ? 'L' : 'Y') : v === 4 ? 'y' : 'Y') : plain(f, u, v, w, h));
}

// ---------------------------------------------------------------- its moves (the mock-up's)
const HIP = 0.764;
const REST = { Root: { pos: [0, 0, HIP] }, Torso: [], Chest: [], Head: [['x', 6]],
  Arm_R: [['x', 4], ['y', -10]], Hand_R: [['x', 18]], Arm_L: [['x', 4], ['y', 10]], Hand_L: [['x', 18]],
  Thigh_R: [['y', -8]], Leg_R: [], Thigh_L: [['y', 8]], Leg_L: [] };
/** The roar of the picture: the arms flung wide and down, fists clenched, the head thrown back. */
const ROAR = { ...REST, Torso: [['x', 6]], Chest: [['x', 8]], Head: [['x', -26]],
  Arm_R: [['y', -58], ['x', 10]], Hand_R: [['x', 30]], Arm_L: [['y', 58], ['x', 10]], Hand_L: [['x', 30]],
  Thigh_R: [['y', -14]], Thigh_L: [['y', 14]] };
/**
 * At rest (Enzo: "do the walk and the idle so I see what it gives"): a heavy breath, the chest swelling and the shoulders
 * rolling up with it, the arms swinging a little away from the body, the head turning one way then the other, a snarl.
 * 3.2 s, looped.
 */
const BREATH = (k, turn) => ({ ...REST, Root: { pos: [0, 0, HIP + 0.01 * k] }, Torso: [['x', -2 * k]], Chest: [['x', -4 * k]],
  Head: [['x', 4 + 4 * k], ['z', turn]],
  Arm_R: [['x', 4 + 3 * k], ['y', -10 - 6 * k]], Hand_R: [['x', 18 + 6 * k]], Arm_L: [['x', 4 + 3 * k], ['y', 10 + 6 * k]], Hand_L: [['x', 18 + 6 * k]] });
const idle = [
  { t: 0, pose: BREATH(0, 0) },
  { t: 0.8, pose: BREATH(1, 10) },
  { t: 1.6, pose: BREATH(0, 14) },
  { t: 2.4, pose: BREATH(1, -10) },
  { t: 3.2, pose: BREATH(0, 0) },
];
/**
 * The walk: heavy strides, the body dropping on each step and the trunk rolling, the arms swinging against the legs, wide
 * of the body, the knees bending on his own skeleton.
 * 1.4 s, looped.
 */
const STEP = (k, drop) => ({ ...REST, Root: { pos: [0, 0, HIP - drop] },
  Torso: [['x', -8], ['z', -3 * k]], Chest: [['x', -2], ['z', -2 * k]], Head: [['x', 10], ['z', 4 * k]],
  Arm_R: [['x', -24 * k], ['y', -14]], Hand_R: [['x', 26]], Arm_L: [['x', 24 * k], ['y', 14]], Hand_L: [['x', 26]],
  Thigh_R: [['x', 26 * k + (k === 0 ? 10 : 0)], ['y', -8]], Leg_R: [['x', k < 0 ? -34 : k === 0 ? -40 : -6]], Thigh_L: [['x', -26 * k], ['y', 8]], Leg_L: [['x', k > 0 ? -34 : -6]] });
const walk = [
  { t: 0, pose: STEP(1, 0.04) }, { t: 0.35, pose: STEP(0, 0) }, { t: 0.7, pose: STEP(-1, 0.04) }, { t: 1.05, pose: STEP(0, 0) },
  { t: 1.4, pose: STEP(1, 0.04) },
];

const ANIMS = {
  awaken: [
    { t: 0, pose: { ...REST, Torso: [['x', -30]], Chest: [['x', -20]], Head: [['x', 40]], Arm_R: [['x', 20], ['y', -6]], Arm_L: [['x', 20], ['y', 6]] } },
    { t: 0.8, pose: { ...REST, Torso: [['x', -10]] } },
    { t: 1.3, pose: ROAR },
    { t: 2.4, pose: { ...ROAR, Head: [['x', -32]] } },
    { t: 3.0, pose: REST },
  ],
  idle,
  walk,
};

/**
 * His own skeleton (Enzo: "and make his own skeleton"): the biped's joints, moved to where his body bends: the hips
 * wider, the knees halfway down his doubled legs, the chest, the neck on the traps, the shoulders in the deltoids, the
 * elbows and the fists where his arms are (Blockbench texels). The joints keep their names and their axes, so every pose
 * of the biped still reads; only where each turns has changed. The game will carry it as his own armature.
 */
const SKELETON = {
  Root: [0, 12, 0], Torso: [0, 13, 0], Chest: [0, 26, 0], Head: [0, 40, 0],
  Shoulder_R: [0, 38, 0], Arm_R: [19.8, 36, 0], Hand_R: [19.8, 18.8, 0], Tool_R: [19.8, 7, 0.5],
  Shoulder_L: [0, 38, 0], Arm_L: [-19.8, 36, 0], Hand_L: [-19.8, 18.8, 0], Tool_L: [-19.8, 7, 0.5],
  Thigh_R: [4, 12, 0], Leg_R: [4, 0.4, 0], Thigh_L: [-4, 12, 0], Leg_L: [-4, 0.4, 0],
};

function buildProject() {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, AVATAR, ATLAS));
  const proj = kit.bbmodel('warlord_avatar', PIECES, [AVATAR], [atlas], ATLAS, 'entity');
  (function own(nodes) { for (const g of nodes) if (typeof g === 'object') { if (SKELETON[g.name]) g.origin = SKELETON[g.name]; own(g.children); } })(proj.outliner);
  const groups = groupsByName(proj.outliner);
  proj.animations = Object.entries(ANIMS).map(([n, keys]) => animation(n, keys, groups, n === 'awaken' ? 'once' : 'loop'));
  return { proj, atlas };
}

/** Beside him as he is in the first phase, both at the same size. */
function besideProject(proj, first) {
  const shift = (e, dx) => ({ ...e, from: e.from.map((v, k) => v + (k === 0 ? dx : 0)), to: e.to.map((v, k) => v + (k === 0 ? dx : 0)),
    origin: e.origin.map((v, k) => v + (k === 0 ? dx : 0)) });
  const lower = (e, dy) => ({ ...e, from: e.from.map((v, k) => v + (k === 1 ? dy : 0)), to: e.to.map((v, k) => v + (k === 1 ? dy : 0)),
    origin: e.origin.map((v, k) => v + (k === 1 ? dy : 0)) });
  const a = proj.elements.map(e => shift(e, 22)), b = first.elements.map(e => ({ ...lower(shift(e, -26), -12),
    faces: Object.fromEntries(Object.entries(e.faces).map(([f, d]) => [f, { ...d, texture: 1 }])) }));
  return { ...proj, name: 'avatar_beside', elements: [...a, ...b], outliner: [...a, ...b].map(e => e.uuid), animations: [],
    textures: [proj.textures[0], { ...first.textures[0], id: '1' }] };
}

async function film(proj) {
  const { connect, load, shot, save } = warlord;
  const bb = await connect(Number(arg('port') || 9236));
  const out = path.join(ROOT, 'docs', 'mobs', 'images');
  await load(bb, { ...proj, animations: [] });
  const stills = { three: [[-112, 50, -120], [0, 26, 0], 640, 760], front: [[0, 32, -152], [0, 26, 0], 640, 760],
    back: [[106, 50, 114], [0, 26, 0], 640, 760], face: [[-12, 49, -50], [0, 47, 0], 640, 560] };
  for (const [view, [pos, target, w, h]] of Object.entries(stills)) save(path.join(out, `warlord-avatar-${view}.png`), await shot(bb, pos, target, w, h));
  // Beside the first phase.
  const first = require('./mesh-kit.cjs');
  const wl = warlord.PIECES.map(p => ({ ...p }));
  const wFaces = first.pack(wl, 128), wAtlas = first.png(128, 128, first.paintAtlas(wFaces, warlord.WARLORD, 128));
  const wProj = first.bbmodel('bandit_warlord', wl, [warlord.WARLORD], [wAtlas], 128, 'entity');
  await load(bb, besideProject(proj, { ...wProj, textures: [{ ...wProj.textures[0], uv_width: 128, uv_height: 128 }] }));
  save(path.join(out, 'warlord-avatar-beside.png'), await shot(bb, [-30, 34, -210], [0, 24, 0], 960, 760));
  // The awakening, the idle and the walk, filmed (the walk from the front three-quarter and from the side too).
  await load(bb, proj);
  const films = [['awaken', 'three', 3.0, false], ['idle', 'three', 3.2, true], ['walk', 'three', 1.4, true], ['walk', 'side', 1.4, true]];
  const cams = { three: [[-112, 50, -120], [0, 26, 0]], side: [[-160, 34, 0], [0, 26, 0]] };
  for (const [name, view, length, loop] of films) {
    const dir = path.join(ROOT, 'build', 'warlord-avatar', name + '-' + view);
    fs.rmSync(dir, { recursive: true, force: true });
    fs.mkdirSync(dir, { recursive: true });
    await bb.evaluate(`(() => { Animation.all.find(a => a.name === ${JSON.stringify(name)}).select(); return true; })()`);
    const frames = Math.round(length * 20) + (loop ? 0 : 6);
    for (let k = 0; k < frames; k++) save(path.join(dir, `frame-${String(k).padStart(3, '0')}.png`), await shot(bb, cams[view][0], cams[view][1], 640, 760, Math.min(k / 20, length)));
    execFileSync('java', [path.join(__dirname, 'FramesToGif.java'), dir, path.join(out, `warlord-avatar-${name}${view === 'three' ? '' : '-' + view}.gif`), '50', '640'], { stdio: 'ignore' });
  }
  bb.close();
  console.log('filmed the avatar');
}

if (require.main === module) {
  const { proj, atlas } = buildProject();
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'warlord_avatar'), { recursive: true });
  fs.writeFileSync(path.join(art, 'warlord_avatar', 'avatar.png'), atlas);
  fs.writeFileSync(path.join(art, 'warlord_avatar.bbmodel'), JSON.stringify(proj, null, 1));
  console.log('wrote the avatar:', PIECES.length, 'pieces');
  if (arg('film') !== 'false') film(proj).catch(e => { console.error(e); process.exit(1); });
}

module.exports = { PIECES, AVATAR, ANIMS };
