// Mock-up of a high-level art's flesh tentacles on a player (Enzo, 17/10/2026: "Kaneki's kagune from Tokyo Ghoul, on the
// back, with an idle where they move like tentacles"): our own design in that spirit, not the character's. Four
// tentacles out of the small of the back. Second pass (17/10, "inspire yourself from other Tokyo Ghoul mods": One Eyed,
// the NeoForge Rinkaku): thick living muscle in a bright crimson, round in section, fibres along their length, a glowing
// core, sharp at the tip, no plates (those are for a kakuja stage); an idle where they wrap round the player in an S,
// writhe, and pulse, a swelling running from the root to the tip. Third pass (17/10, after Enzo's reference picture: "it
// must move a lot more"): blood red, thick to the end with a broad blade of a tip, two arching up over the shoulders and
// two curling down to the feet, and big, lively movement: the whole tentacles swing from the back, waves run down
// them, the tips lash.
//
//   node scripts/concepts/kagune-preview.cjs [--port=9236] [--form=kakuja] [--rest] [--check]
// --form=kakuja: the form at 100 Fury (Enzo, 17/10, after his references: Kaneki's centipede kakuja), an overlay on the
// skin like the blood crystals: the kagune becomes one great centipede, purple plates with glowing red joints and hooked
// spines, coiled in a ring round the player, and a purple half-mask on the face.
// Writes art/blockbench/kagune_preview.bbmodel (model + idle, open in Animate) and the GIFs and stills in
// docs/combat/styles/images/kagune-*.
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const kit = require('../animation/mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9236);
const id = s => kit.stableId('kagune/' + s);

// ---------------------------------------------------------------- texture (64 x 64)
const A = 64, rgba = new Uint8Array(A * A * 4);
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
let seed = 7; const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
const put = (x, y, c, f = 1) => { const o = (y * A + x) * 4; rgba[o] = Math.min(255, c[0] * f); rgba[o + 1] = Math.min(255, c[1] * f); rgba[o + 2] = Math.min(255, c[2] * f); rgba[o + 3] = 255; };
const REG = {};
function region(name, x0, y0, w, h, paint) { REG[name] = [x0, y0, x0 + w, y0 + h]; for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) put(x0 + x, y0 + y, ...paint(x, y, w, h)); }
// Scales (fifth pass, after Enzo's figurine): glossy bright red, diamond scales in rows, each lit on its upper side and
// shadowed below, with dark seams between them.
region('scales', 0, 0, 16, 16, (x, y) => {
  const u = ((x + (Math.floor(y / 8) % 2) * 4) % 8) - 3.5, v = (y % 8) - 3.5;
  const d = Math.abs(u) + Math.abs(v);
  if (d > 4.6) return [hex('#6A0814'), 1];
  return [v < -1 ? hex('#FF6A70') : d < 2 ? hex('#E8202E') : hex('#B8121F'), 0.96 + rnd() * 0.06];
});
// The scales as laid on a segment's face: one row of diamonds, about a texel per pixel.
REG.scales_face = [0, 0, 8, 4];
// The root: a knot of the same red where they leave the back.
region('root', 32, 0, 16, 16, () => [rnd() < 0.3 ? hex('#FF5A62') : rnd() < 0.5 ? hex('#7A0A16') : hex('#C01424'), 0.92 + rnd() * 0.12]);
// The player: skin, white hair, a black robe with a red sash, dark trousers, boots.
region('skin', 0, 16, 16, 16, () => [hex('#D9A882'), 0.95 + rnd() * 0.08]);
region('hair', 16, 16, 16, 16, () => [hex('#ECECF0'), 0.9 + rnd() * 0.12]);
region('robe', 32, 16, 16, 16, (x, y) => [y % 7 === 6 ? hex('#2C2C34') : hex('#18181E'), 0.9 + rnd() * 0.15]);
region('sash', 48, 16, 16, 16, () => [hex('#8E1A1E'), 0.9 + rnd() * 0.15]);
region('trousers', 0, 32, 16, 16, () => [hex('#24242A'), 0.9 + rnd() * 0.12]);
region('boots', 16, 32, 16, 16, () => [hex('#121214'), 0.9 + rnd() * 0.12]);
// The face: skin with the eyes, one of them red-black (the ghoul's eye).
region('face', 32, 32, 16, 16, (x, y) => {
  if (y >= 6 && y <= 8 && ((x >= 3 && x <= 5) || (x >= 10 && x <= 12))) return x >= 10 ? (x === 11 && y === 7 ? [hex('#FF2020'), 1] : [hex('#101010'), 1]) : [x === 4 ? hex('#202020') : hex('#F2F2F2'), 1];
  if (y === 12 && x >= 6 && x <= 9) return [hex('#8A5A48'), 1];
  return [hex('#D9A882'), 0.95 + rnd() * 0.08];
});
region('hairfront', 48, 32, 16, 16, (x, y) => (y < 5 || (y < 9 && (x < 3 || x > 12 || (x > 6 && x < 9)))) ? [hex('#ECECF0'), 0.92 + rnd() * 0.1] : [hex('#D9A882'), 0.95 + rnd() * 0.08]);
// The kakuja (100 Fury): purple plates lit along the top edge, dark violet spines, glowing red joints, the half-mask.
region('kakuja_plate', 0, 48, 16, 16, (x, y) => [y < 2 ? hex('#A070D0') : y >= 6 && y <= 9 ? (y === 7 || y === 8 ? hex('#FF3A44') : hex('#C01828')) : y > 13 ? hex('#3A1C5E') : hex('#6E3CA0'), 0.92 + rnd() * 0.1]);
region('kakuja_spine', 16, 48, 16, 16, (x, y) => [x > 11 ? hex('#E0202E') : y < 5 ? hex('#7A58A8') : hex('#2A1440'), 0.9 + rnd() * 0.12]);
region('kakuja_joint', 32, 48, 16, 16, (x, y) => [Math.abs(y - 7.5) < 3 ? hex('#FF3A44') : hex('#B0101E'), 0.95 + rnd() * 0.06]);
const MASK = ['LLLLD', 'PPPPD', 'PKRKP', 'PPKPD', 'PPPPD', 'WDWDP', 'DWDWD'];
const MASK_COLOURS = { L: '#A878D8', P: '#6E3CA0', D: '#2A1440', K: '#0A0608', R: '#FF2A30', W: '#ECE4D4' };
region('kakuja_mask', 48, 48, 5, 7, (x, y) => [hex(MASK_COLOURS[MASK[y][x]]), MASK[y][x] === 'R' ? 1 : 0.95 + rnd() * 0.06]);
region('kakuja_mask_edge', 53, 48, 3, 7, () => [hex('#2A1440'), 1]);
const atlas = Buffer.from(kit.png(A, A, rgba));

// ---------------------------------------------------------------- model (1 unit = 1 pixel, player 32 high, facing -z)
const FACE_NAMES = ['north', 'east', 'south', 'west', 'up', 'down'];
const elements = [];
function cube(name, from, to, origin, faces, rotation = [0, 0, 0]) {
  const e = { name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true, from, to, autouv: 0, color: 0,
    origin, rotation, faces: Object.fromEntries(FACE_NAMES.map(f => [f, { uv: REG[typeof faces === 'string' ? faces : (faces[f] || faces.all)], texture: 0 }])),
    type: 'cube', uuid: id('e/' + name) };
  elements.push(e);
  return e.uuid;
}
const group = (name, origin, children, rotation = [0, 0, 0]) => ({ name, origin, rotation, color: 0, uuid: id('g/' + name), export: true,
  mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0, children });

// The player (rebuilt by each build of the project).
function buildPlayer() {
const head = group('head', [0, 24, 0], [cube('head', [-4, 24, -4], [4, 32, 4], [0, 24, 0], { north: 'face', up: 'hair', south: 'hair', east: 'hairfront', west: 'hairfront', down: 'skin' }),
  cube('hair_top', [-4.5, 29, -4.5], [4.5, 32.5, 4.5], [0, 24, 0], 'hair'), cube('fringe', [-4.5, 28, -4.6], [4.5, 30, -4.2], [0, 24, 0], 'hair')]);
const body = group('body', [0, 24, 0], [cube('body', [-4, 12, -2], [4, 24, 2], [0, 24, 0], 'robe'), cube('sash', [-4.2, 12, -2.2], [4.2, 14, 2.2], [0, 24, 0], 'sash')]);
const armR = group('arm_right', [-5, 22, 0], [cube('arm_right', [-8, 12, -2], [-4, 24, 2], [-5, 22, 0], { all: 'robe', down: 'skin' }),
  cube('hand_right', [-8, 12, -2], [-4, 13.5, 2], [-5, 22, 0], 'skin')]);
const armL = group('arm_left', [5, 22, 0], [cube('arm_left', [4, 12, -2], [8, 24, 2], [5, 22, 0], { all: 'robe', down: 'skin' }),
  cube('hand_left', [4, 12, -2], [8, 13.5, 2], [5, 22, 0], 'skin')]);
const legR = group('leg_right', [-2, 12, 0], [cube('leg_right', [-4, 0, -2], [0, 12, 2], [-2, 12, 0], 'trousers'), cube('boot_right', [-4.1, 0, -2.1], [0.1, 3, 2.1], [-2, 12, 0], 'boots')]);
const legL = group('leg_left', [2, 12, 0], [cube('leg_left', [0, 0, -2], [4, 12, 2], [2, 12, 0], 'trousers'), cube('boot_left', [-0.1, 0, -2.1], [4.1, 3, 2.1], [2, 12, 0], 'boots')]);
return { list: [head, body, armR, armL, legR, legL], head, body, armR, armL };
}
// The tentacles (fifth pass, 17/10, after Enzo's figurine and anime references): glossy bright red tubes covered in
// diamond scales, fat in the middle and pointed at the end. The two upper ones climb out and loop over, the two lower
// ones sweep out wide and curl. The idle is mirrored left and right and lively, and it never touches the ground or the
// body: every frame is checked in Blockbench itself, and where a tentacle would touch, that frame's movement is turned
// down (SOLVE) until nothing does.
const SEGMENTS = 18, SEG = 3.4;
const TENTACLES = [
  // name, root on the back, the root's turn (pitch: up < 0; yaw: out), side, its curve in three stretches [x, y] per joint
  ['upper_right', [-1.4, 16.5, 4.6], [-46, -64, 0], 1, [[-3, -2], [4, -7], [10, -5]]],
  ['upper_left', [1.4, 16.5, 4.6], [-46, 64, 0], -1, [[-3, -2], [4, -7], [10, -5]]],
  ['lower_right', [-1.6, 13.4, 2.8], [8, -66, 0], 1, [[1, 1], [-2, 5], [-7, 6]]],
  ['lower_left', [1.6, 13.4, 2.8], [8, 66, 0], -1, [[1, 1], [-2, 5], [-7, 6]]],
];
const FORM = arg('form') || 'kagune';
/** The centipede of the kakuja: one great chain. */
/** The kakuja's two centipedes (Enzo: "there are two of them"): name, root, the way they leave (yaw), their coil, length. */
const CENTIPEDES = [
  // name, root, how it leaves (yaw out, pitch up), length; its loose curve along the body: [up/down, out/in] per joint,
  // as waves along it (amplitude in degrees, waves along its length, phase).
  { name: 'centipede_right', root: [-1.6, 11, 9], yaw: -70, pitch: -30, segments: 30, seg: 3.6, side: 1,
    curveX: [7, 1.3, 0.4], curveY: [9, 0.9, 1.2] },
  { name: 'centipede_left', root: [1.6, 11, 9], yaw: 70, pitch: -14, segments: 28, seg: 3.6, side: -1,
    curveX: [8, 1.1, 2.2], curveY: [9, 1.0, 0.3] },
]
const CHAINS = FORM === 'kakuja' ? CENTIPEDES.map(c => [c.name]) : TENTACLES;
const LENGTH = 2.4, STEP = 0.08, FPS = 20, FRAMES = Math.round(LENGTH * FPS);
const SAMPLES = Math.round(LENGTH / STEP);

/** Builds the project; `scale[i][s]` turns tentacle i's movement down at sample s (1 = the full idle). */
function build(scale) {
  return FORM === 'kakuja' ? buildKakuja(scale) : buildKagune(scale);
}

/** The project around the player and the chains, with its idle. */
function project(rootGroup, animators) {
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: FORM === 'kakuja' ? 'kakuja_preview' : 'kagune_preview', model_identifier: '',
    visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: A, height: A }, elements: elements.slice(), outliner: [rootGroup],
    textures: [{ path: '', name: 'kagune.png', folder: '', namespace: 'murimblock', id: '0', width: A, height: A, uv_width: A, uv_height: A, particle: false,
      use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop',
      frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false, uuid: id('texture'), source: 'data:image/png;base64,' + atlas.toString('base64') }],
    animations: [{ uuid: id('anim/idle'), name: 'idle', loop: 'loop', override: false, length: LENGTH, snapping: 20, selected: false, anim_time_update: '',
      blend_weight: '', start_delay: '', loop_delay: '', animators }],
  };
}
const kf = (channel, t, [x, y, z]) => ({ channel, data_points: [{ x: +x.toFixed(2), y: +y.toFixed(2), z: +z.toFixed(2) }], uuid: id(`kf/${channel}/${t}/${x}/${y}/${z}`), time: +t.toFixed(2), color: -1, interpolation: 'catmullrom' });
function breathe(add, playerBones) {
  const breath = [0, 0.6, 1.2, 1.8, 2.4];
  add(playerBones.body, breath.map(t => kf('rotation', t, [Math.sin((2 * Math.PI * t) / 2.4) * 1.5, 0, 0])));
  add(playerBones.head, breath.map(t => kf('rotation', t, [Math.sin((2 * Math.PI * t) / 2.4 + 0.5) * -2.5, 0, 0])));
  add(playerBones.armR, breath.map(t => kf('rotation', t, [0, 0, 5 + Math.sin((2 * Math.PI * t) / 2.4) * 2])));
  add(playerBones.armL, breath.map(t => kf('rotation', t, [0, 0, -5 - Math.sin((2 * Math.PI * t) / 2.4) * 2])));
}

/**
 * The kakuja: one great centipede out of the small of the back, purple plates and red joints, a pair of hooked spines on
 * each segment and one along its back every other, coiled in a ring round the player, its tail ending in a blade; the
 * purple half-mask over the left of the face, its eye slit glowing. The idle: the coil breathes and slides, a slow wave
 * along it, the joints pulse.
 */
function buildKakuja(scale) {
  elements.length = 0;
  const bones = {}, meat = {}, legs = {};
  function centipede({ name, root, yaw, pitch, segments: N, seg: L, side, curveX, curveY }) {
    const width = k => (k < 2 ? 4.6 + k : k > N - 7 ? Math.max(1.6, 6.4 * (N - k) / 7) : 6.4);
    let child = null;
    for (let k = N - 1; k >= 0; k--) {
      const z0 = root[2] + k * L, w = width(k), h = w * 0.82, o = [root[0], root[1], z0], mid = [root[0], root[1], z0 + L / 2];
      const parts = [];
      if (k === N - 1) {
        // The tail: a long curved blade.
        parts.push(cube(`${name}_tail`, [root[0] - w / 2, root[1] - h / 2, z0], [root[0] + w / 2, root[1] + h / 2, z0 + L * 2.4], mid, 'kakuja_plate'));
        parts.push(cube(`${name}_tail_tip`, [root[0] - 0.6, root[1] - 0.6, z0 + L * 2.2], [root[0] + 0.6, root[1] + 0.6, z0 + L * 3.4], mid, 'kakuja_spine', [-12, 0, 0]));
      } else {
        // A plate (a red band across it), then the glowing joint before the next one.
        parts.push(cube(`${name}_${k}`, [root[0] - w / 2, root[1] - h / 2, z0 + 0.4], [root[0] + w / 2, root[1] + h / 2, z0 + L - 0.7], mid, 'kakuja_plate'));
        parts.push(cube(`${name}_${k}_joint`, [root[0] - w * 0.47, root[1] - h * 0.47, z0 + L - 1.2], [root[0] + w * 0.47, root[1] + h * 0.47, z0 + L + 0.6], mid, 'kakuja_joint'));
        // A pair of hooked spines, out to the sides and swept back, like a centipede's legs; red at the tip.
        const sx = w / 2, sl = 3.4 + w * 0.55;
        const legL = group(`${name}_leg_l_${k}`, [root[0] + sx, root[1], z0 + 1.4], [cube(`${name}_${k}_spine_l`, [root[0] + sx - 0.3, root[1] - 0.6, z0 + 0.8], [root[0] + sx + sl, root[1] + 0.6, z0 + 2.0], [root[0] + sx, root[1], z0 + 1.4], 'kakuja_spine', [0, -32, 18])]);
        const legR = group(`${name}_leg_r_${k}`, [root[0] - sx, root[1], z0 + 1.4], [cube(`${name}_${k}_spine_r`, [root[0] - sx - sl, root[1] - 0.6, z0 + 0.8], [root[0] - sx + 0.3, root[1] + 0.6, z0 + 2.0], [root[0] - sx, root[1], z0 + 1.4], 'kakuja_spine', [0, 32, -18])]);
        legs[`${name}_${k}`] = [legL, legR];
        if (k % 2 === 0 && k < N - 4) parts.push(cube(`${name}_${k}_spine_back`, [root[0] - 0.4, root[1] + h / 2 - 0.2, z0 + 0.8], [root[0] + 0.4, root[1] + h / 2 + sl * 0.8, z0 + 1.6], [root[0], root[1] + h / 2, z0 + 1.2], 'kakuja_spine', [-40, 0, 0]));
      }
      const m = group(`${name}_flesh_${k}`, mid, parts);
      meat[`${name}_${k}`] = m;
      const kids = [m, ...(legs[`${name}_${k}`] || [])];
      if (child) kids.push(child);
      // The coil: out to its side and up, then bending all the way round.
      // Its loose curve: gentle waves of bending along it, out and up, then wandering.
      const u = k / N;
      const rest = k === 0 ? [pitch, yaw, 0]
        : [curveX[0] * Math.sin(2 * Math.PI * curveX[1] * u + curveX[2]), side * curveY[0] * Math.sin(2 * Math.PI * curveY[1] * u + curveY[2]), 0];
      child = group(`${name}_${k}`, o, kids, rest);
      bones[`${name}_${k}`] = child;
    }
    return child;
  }
  const playerBones = buildPlayer();
  // The half-mask, on the head: a thin plate over the left half of the face (5 x 7 texels, a texel a pixel), and a horn
  // from the temple.
  playerBones.head.children.push(
    cube('mask', [-0.5, 24.6, -4.45], [4.5, 31.6, -4.05], [0, 24, 0], { all: 'kakuja_mask_edge', north: 'kakuja_mask' }),
    cube('mask_horn', [3.6, 29.6, -3.6], [4.8, 33.6, -2.4], [4.2, 30, -3], 'kakuja_spine', [0, 0, -24]));
  // The stalk out of the back that carries both.
  const knot = cube('kakuja_knot', [-3.4, 9.4, 1.8], [3.4, 15.8, 10.4], [0, 12, 2], 'kakuja_plate');
  const knotJoint = cube('kakuja_knot_joint', [-2.8, 10, 5.6], [2.8, 15.2, 7.0], [0, 12, 2], 'kakuja_joint');
  const knotJoint2 = cube('kakuja_knot_joint_2', [-2.8, 10, 8.6], [2.8, 15.2, 9.6], [0, 12, 2], 'kakuja_joint');
  const rootGroup = group('player', [0, 0, 0], [...playerBones.list, group('kakuja', [0, 12, 2], [knot, knotJoint, knotJoint2, ...CENTIPEDES.map(centipede)])]);
  const animators = {};
  const add = (g, keys) => { animators[g.uuid] = { name: g.name, type: 'bone', keyframes: keys }; };
  CENTIPEDES.forEach((c, i) => {
    for (let k = 0; k < c.segments; k++) {
      const keys = [], pulse = [], legL = [], legR = [];
      for (let sI = 0; sI <= SAMPLES; sI++) {
        const t = sI * STEP, a = scale[i][sI % SAMPLES], w = (2 * Math.PI * t) / LENGTH + i * 2.1;
        // A slither: a wave that runs from the root to the tail, side to side and up and down, every joint its share;
        // the root sways the whole body.
        const x = Math.sin(w - k * 0.42) * 2.4 * a + (k === 0 ? Math.sin(w * 0.5 + i) * 6 * a : 0);
        const y = c.side * (Math.cos(w - k * 0.38) * 3.2 * a + (k === 0 ? Math.cos(w * 0.5) * 8 * a : 0));
        const z = c.side * Math.sin(w - k * 0.5) * 1.6 * a;
        keys.push(kf('rotation', t, [x, y, z]));
        const p = Math.max(0, Math.sin(2 * w - k * 0.35));
        const sc = 1 + 0.07 * p * p;
        pulse.push(kf('scale', t, [sc, sc, 1]));
        // The legs: a centipede's walking wave, each pair a little behind the one before, left and right in turn.
        const step = 2 * w * 2 - k * 0.9;
        legL.push(kf('rotation', t, [Math.cos(step) * 10, Math.sin(step) * 28, 0]));
        legR.push(kf('rotation', t, [Math.cos(step + Math.PI) * 10, Math.sin(step + Math.PI) * 28, 0]));
      }
      add(bones[`${c.name}_${k}`], keys);
      add(meat[`${c.name}_${k}`], pulse);
      const lg = legs[`${c.name}_${k}`];
      if (lg) { add(lg[0], legL); add(lg[1], legR); }
    }
  });
  breathe(add, playerBones);
  return project(rootGroup, animators);
}

function buildKagune(scale) {
  elements.length = 0;
  const bones = {}, flesh = {};
  const tube = (name, k, root, w, z0, len) => {
    const mid = [root[0], root[1], z0 + len / 2];
    // Round: a square and the same square turned 45 degrees round the length (an octagon), all scales.
    return [cube(`${name}_${k}`, [root[0] - w / 2, root[1] - w / 2, z0 - 0.5], [root[0] + w / 2, root[1] + w / 2, z0 + len + 0.5], mid, 'scales_face'),
      cube(`${name}_${k}_round`, [root[0] - w * 0.43, root[1] - w * 0.43, z0 - 0.3], [root[0] + w * 0.43, root[1] + w * 0.43, z0 + len + 0.3], mid, 'scales_face', [0, 0, 45])];
  };
  function tentacle(name, root, turn, side, curve) {
    // Fat in the middle, narrower at the root, pointed at the end.
    const width = k => { const u = k / (SEGMENTS - 1); const body = 3.8 + 5 * Math.sin(Math.PI * Math.pow(u, 0.7)); return u > 0.72 ? Math.max(0.9, body * (1 - u) / 0.28) : body; };
    let child = null;
    for (let k = SEGMENTS - 1; k >= 0; k--) {
      const z0 = root[2] + k * SEG, w = width(k), o = [root[0], root[1], z0];
      const meatBone = group(`${name}_flesh_${k}`, [root[0], root[1], z0 + SEG / 2], tube(name, k, root, Math.max(0.9, w), z0, SEG));
      flesh[`${name}_${k}`] = meatBone;
      const kids = [meatBone];
      if (child) kids.push(child);
      const stretch = curve[k < 6 ? 0 : k < 12 ? 1 : 2];
      const rest = k === 0 ? turn : [stretch[0], side * stretch[1], 0];
      child = group(`${name}_${k}`, o, kids, rest);
      bones[`${name}_${k}`] = child;
    }
    return child;
  }
  const rootKnot = cube('knot', [-3.4, 12.4, 1.8], [3.4, 17.8, 5.4], [0, 14, 2], 'root');
  const kagune = group('kagune', [0, 14, 2], [rootKnot, ...TENTACLES.map(([n, r, t, side, c]) => tentacle(n, r, t, side, c))]);
  // The player (its bones rebuilt so their cubes join this build's elements).
  const playerBones = buildPlayer();
  const rootGroup = group('player', [0, 0, 0], [...playerBones.list, kagune]);

  const animators = {};
  const add = (g, keys) => { animators[g.uuid] = { name: g.name, type: 'bone', keyframes: keys }; };
  TENTACLES.forEach(([name, , , side], i) => {
    // Mirrored: the left ones move as the right ones seen in a mirror (yaw and roll turned round), half a beat later.
    const pair = Math.floor(i / 2), beat = side < 0 ? Math.PI : 0;
    for (let k = 0; k < SEGMENTS; k++) {
      const g = bones[`${name}_${k}`], m = flesh[`${name}_${k}`], keys = [], pulse = [];
      for (let s = 0; s <= SAMPLES; s++) {
        const t = s * STEP, a = scale[i][s % SAMPLES];
        const w = (2 * Math.PI * t) / LENGTH + beat, phase = pair * 1.7 + k * 0.33;
        const amp = (2.4 + k * 0.62) * a;
        const swingX = k === 0 ? Math.sin(w + pair) * 12 * a : 0, swingY = k === 0 ? Math.cos(w + pair * 2) * 14 * a : 0;
        const x = Math.sin(w - phase) * amp + swingX;
        const y = side * (Math.cos(w - phase * 0.8 + 0.6) * amp * 0.9 + swingY);
        const z = side * Math.sin(w * 2 - phase) * amp * 0.5;
        keys.push(kf('rotation', t, [x, y, z]));
        const p = Math.max(0, Math.sin(2 * w - k * 0.45));
        const sc = 1 + 0.16 * p * p;
        pulse.push(kf('scale', t, [sc, sc, 1]));
      }
      add(g, keys);
      add(m, pulse);
    }
  });
  breathe(add, playerBones);
  return project(rootGroup, animators);
}

// ---------------------------------------------------------------- Blockbench: checking, solving, filming
async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let n = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.error || (m.result && m.result.exceptionDetails)) p.reject(new Error(JSON.stringify(m.error || m.result.exceptionDetails).slice(0, 800))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const k = ++n; pending.set(k, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: k, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}
const load = (bb, proj) => bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
  setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(proj))}), 'p.bbmodel'); Canvas.updateAll();
  for (let i = 0; i < 60 && Outliner.elements.length < ${proj.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
  await new Promise(r => setTimeout(r, 300));
  Modes.options.animate.select(); Animation.all[0].select(); return Outliner.elements.length; })()`);
/**
 * CHECK at time t, for each tentacle: how many corners of its cubes, as Blockbench places them, are under the ground
 * (y below 0.5) or inside the player (body, head, arms, legs, a little grown). Its first three segments come out of
 * the back and may touch it.
 */
const check = (bb, t) => bb.evaluate(`(() => {
  Timeline.setTime(${t}); Animator.preview();
  const boxes = [[-4.6, 11.5, -2.6, 4.6, 24.5, 2.0], [-4.8, 24, -4.8, 4.8, 33, 4.7], [-8.8, 11.5, -2.6, -3.4, 24.5, 2.6], [3.4, 11.5, -2.6, 8.8, 24.5, 2.6], [-4.4, 0, -2.6, 4.4, 12.5, 2.6]];
  const names = ${JSON.stringify(CHAINS.map(t => t[0]))};
  const out = names.map(() => 0), low = names.map(() => 99), why = [];
  const v = new THREE.Vector3();
  for (const el of Outliner.elements) {
    const i = names.findIndex(n => el.name.startsWith(n + '_'));
    if (i < 0 || !el.mesh) continue;
    const seg = parseInt(el.name.slice(names[i].length + 1)) || 0;
    el.mesh.updateMatrixWorld(true);
    const pos = el.mesh.geometry.attributes.position;
    for (let p = 0; p < pos.count; p++) {
      v.fromBufferAttribute(pos, p).applyMatrix4(el.mesh.matrixWorld);
      low[i] = Math.min(low[i], v.y);
      if (v.y < 0.5) out[i]++;
      else if (seg >= 3) { const bi = boxes.findIndex(b => v.x > b[0] && v.x < b[3] && v.y > b[1] && v.y < b[4] && v.z > b[2] && v.z < b[5]); if (bi >= 0) { out[i]++; if (why.length < 6) why.push(el.name + ' box' + bi + ' ' + v.x.toFixed(1) + ',' + v.y.toFixed(1) + ',' + v.z.toFixed(1)); } }
    }
  }
  return { out, low, why };
})()`);
const shot = (bb, pos, target, w, h, t) => bb.evaluate(`(async () => {
  Timeline.setTime(${t}); Animator.preview();
  const p = window.__kaguneFilm || (window.__kaguneFilm = new Preview({ id: 'kagune_film', offscreen: true, antialias: true }));
  p.resize(${w}, ${h});
  p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 36 });
  p.camera.lookAt(...${JSON.stringify(target)});
  const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#E9E2D2');
  return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
})()`);
const save = (file, data) => fs.writeFileSync(file, Buffer.from(data.split(',')[1], 'base64'));

(async () => {
  const bb = await connect(PORT);
  // SOLVE: start from the full idle; check every sample time; turn down the samples (and their neighbours) of each
  // tentacle that touches; again, until no frame touches anything.
  const scale = CHAINS.map(() => new Array(SAMPLES).fill(process.argv.includes('--rest') ? 0 : 1));
  let proj, faults;
  for (let round = 0; round < 10; round++) {
    proj = build(scale);
    await load(bb, proj);
    faults = CHAINS.map(() => 0);
    const worst = CHAINS.map(() => 99);
    const hits = CHAINS.map(() => new Set());
    // Each sample time, and halfway between samples (the curve between keys).
    for (let s = 0; s < SAMPLES * 2; s++) {
      const r = await check(bb, (s * STEP) / 2);
      if (round === 0 && s === 0 && r.why.length) console.log('why', JSON.stringify(r.why));
      r.out.forEach((n, i) => { if (n > 0) { faults[i] += n; hits[i].add(Math.floor(s / 2)); } worst[i] = Math.min(worst[i], r.low[i]); });
    }
    console.log(`round ${round}: faults ${JSON.stringify(faults)}, lowest y ${JSON.stringify(worst.map(v => +v.toFixed(1)))}`);
    if (faults.every(n => n === 0)) break;
    hits.forEach((set, i) => set.forEach(s => { for (const d of [-1, 0, 1]) { const j = (s + d + SAMPLES) % SAMPLES; scale[i][j] *= d === 0 ? 0.7 : 0.85; } }));
  }
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${FORM}_preview.bbmodel`), JSON.stringify(proj, null, 1));
  console.log(proj.elements.length, 'cubes; movement kept per tentacle (mean):', JSON.stringify(scale.map(a => +(a.reduce((x, y) => x + y) / a.length).toFixed(2))));
  if (process.argv.includes('--check')) { bb.close(); return; }
  if (process.argv.includes('--rest')) { const out = path.join(ROOT, 'build', `${FORM}-preview`); fs.mkdirSync(out, { recursive: true });
    for (const [v, p, t] of [['front', [-30, 42, -140], [0, 26, 6]], ['side', [-150, 40, 10], [0, 26, 10]]]) save(path.join(out, 'rest-' + v + '.png'), await shot(bb, p, t, 640, 640, 0));
    bb.close(); return; }
  const out = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
  fs.mkdirSync(out, { recursive: true });
  const VIEWS = FORM === 'kakuja'
    ? { back: [[150, 100, 230], [0, 30, 40]], front: [[-70, 70, -230], [0, 30, 36]], side: [[-260, 70, 60], [0, 30, 50]] }
    : { back: [[70, 56, 118], [0, 26, 10]], front: [[-30, 42, -140], [0, 26, 6]], side: [[-150, 40, 10], [0, 26, 10]] };
  for (const [view, [pos, target]] of Object.entries(VIEWS)) {
    const dir = path.join(ROOT, 'build', `${FORM}-preview`, view);
    fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
    for (let f = 0; f < FRAMES; f++) save(path.join(dir, `frame-${String(f).padStart(3, '0')}.png`), await shot(bb, pos, target, 640, 640, f / FPS));
    fs.copyFileSync(path.join(dir, 'frame-000.png'), path.join(out, `${FORM}-${view}.png`));
    const gif = path.join(out, `${FORM}-idle-${view}.gif`);
    execFileSync('java', [path.join(ROOT, 'scripts', 'animation', 'FramesToGif.java'), dir, gif, String(1000 / FPS), '640'], { stdio: 'ignore' });
    console.log(view, FRAMES, 'frames ->', path.relative(ROOT, gif));
  }
  bb.close();
})().catch(e => { console.error(e); process.exit(1); });
