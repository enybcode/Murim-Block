// Mock-ups of the Seven Stars Sword's effects (docs/combat/styles/seven-stars-sword.md), for Enzo to judge before they are
// made: a Blood Soldier's body stands for the target (scripts/animation/blood-soldier-3d.cjs), and the effects are boxes on
// a texture of their own (silver, white, night blue), turned any way. Three scenes, each a Blockbench project:
//   constellation  the seven star marks over the target in the Big Dipper, joined by lines of light;
//   rain           Falling Star Rain: the zone's ring of stars on the ground, four stars falling with their trails, three
//                  craters of light where others fell;
//   array          the Seven Stars Sword Array: the Big Dipper's seal on the ground, seven swords of light around the
//                  target in the Dipper, their points on it.
// Written to art/concepts/seven_stars/<scene>.bbmodel; render with blockbench-render.cjs --views=scene,scene_top.
// No glow in Blockbench: in game the light parts shine (glowmask) and Photon lights them.
// Usage: node scripts/concepts/seven-stars-mock.cjs
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');
const soldier = require('../animation/blood-soldier-3d.cjs');

const ROOT = path.join(__dirname, '..', '..');
const OUT = path.join(ROOT, 'art', 'concepts', 'seven_stars');

// ---------------------------------------------------------------- the effects' texture: swatches, and the seals

const FX = 128;
/** Swatches of 4 x 4 along the top row: their names and colours. */
const SWATCH = { white: '#ffffff', light: '#eaf2ff', silver: '#c4d2ec', pale: '#9fd8ff', blue: '#5a86e0', deep: '#2f4cb0', night: '#1b2556' };
const swatchUv = name => { const i = Object.keys(SWATCH).indexOf(name); return [i * 4, 0, i * 4 + 4, 4]; };
/** The Big Dipper, its seven stars in order down the handle to the bowl (units), and its lines. */
const DIPPER = [[-2.2, 0.1], [-1.2, 0.35], [-0.4, 0.4], [0.4, 0.4], [1.9, 1.0], [2.0, -0.1], [0.8, -0.6]];
const DIPPER_LINES = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 3]];
/** The seals at (x0, y0) of size n: the Dipper's seal (rings, ticks, its stars joined), and a star's sigil. */
const SEALS = { dipper: [0, 16, 96], star: [96, 16, 32] };

function fxTexture() {
  const px = new Uint8Array(FX * FX * 4);
  const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
  const set = (x, y, h, a = 255) => { if (x < 0 || y < 0 || x >= FX || y >= FX) return; px.set([...hex(h), a], (y * FX + x) * 4); };
  Object.values(SWATCH).forEach((h, i) => { for (let y = 0; y < 4; y++) for (let x = 0; x < 4; x++) set(i * 4 + x, y, h); });
  // The Dipper's seal: two rings, ticks between them, the seven stars and their lines inside.
  {
    const [x0, y0, n] = SEALS.dipper, c = n / 2;
    for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
      const dx = x + 0.5 - c, dy = y + 0.5 - c, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
      if (Math.abs(r - (c - 2)) < 1) set(x0 + x, y0 + y, SWATCH.light);
      else if (Math.abs(r - (c - 9)) < 0.8) set(x0 + x, y0 + y, SWATCH.blue);
      else if (r > c - 9 && r < c - 2 && Math.abs(((a / (2 * Math.PI) * 28) % 1 + 1) % 1 - 0.5) < 0.12) set(x0 + x, y0 + y, SWATCH.pale);
    }
    const at = ([u, v]) => [c + u * 14, c - v * 14];
    for (const [i, j] of DIPPER_LINES) {
      const [ax, ay] = at(DIPPER[i]), [bx, by] = at(DIPPER[j]), steps = Math.ceil(Math.hypot(bx - ax, by - ay) * 2);
      for (let s = 0; s <= steps; s++) set(x0 + Math.round(ax + (bx - ax) * s / steps), y0 + Math.round(ay + (by - ay) * s / steps), SWATCH.silver);
    }
    for (const p of DIPPER) {
      const [sx, sy] = at(p);
      for (let k = -3; k <= 3; k++) { set(x0 + Math.round(sx) + k, y0 + Math.round(sy), Math.abs(k) < 2 ? SWATCH.white : SWATCH.light); set(x0 + Math.round(sx), y0 + Math.round(sy) + k, Math.abs(k) < 2 ? SWATCH.white : SWATCH.light); }
    }
  }
  // A star's sigil: a ring and a four-pointed star.
  {
    const [x0, y0, n] = SEALS.star, c = n / 2;
    for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
      const dx = x + 0.5 - c, dy = y + 0.5 - c, r = Math.hypot(dx, dy);
      if (Math.abs(r - (c - 1.5)) < 0.9) set(x0 + x, y0 + y, SWATCH.blue);
      else if (Math.abs(dx) * Math.abs(dy) < 3 && r < c - 3) set(x0 + x, y0 + y, r < 3 ? SWATCH.white : SWATCH.light);
    }
  }
  return kit.png(FX, FX, px);
}

// ---------------------------------------------------------------- the effects' boxes

const ELEMENTS = [];
let count = 0;
/**
 * A box of size (w, l, t) centred on x and z, along +y from its foot, then turned (Blockbench's order: x, then y, then z)
 * about its foot and set at `at`; every face one swatch, or the top face a seal.
 */
function box(name, at, [w, l, t], swatch, rot = [0, 0, 0], seal = null) {
  const from = [at[0] - w / 2, at[1], at[2] - t / 2], to = [at[0] + w / 2, at[1] + l, at[2] + t / 2];
  const uv = swatchUv(swatch), faces = {};
  // A seal's sides are clear (an empty corner of the texture): only its faces show.
  for (const f of kit.FACES) faces[f] = { uv: seal ? (f === 'up' ? [seal[0], seal[1], seal[0] + seal[2], seal[1] + seal[2]] : [124, 0, 128, 4]) : uv, texture: 1 };
  if (seal) faces.down = { uv: [seal[0], seal[1], seal[0] + seal[2], seal[1] + seal[2]], texture: 1 };
  ELEMENTS.push({ name: name + '_' + (count++), box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from, to, autouv: 0, color: 0, origin: at, rotation: rot, faces, type: 'cube', uuid: kit.stableId('seven_stars/' + name + count) });
}
/** A box pointing from a to b (its length along the line). */
function segment(name, a, b, w, swatch) {
  const d = b.map((v, i) => v - a[i]), L = Math.hypot(...d);
  const pitch = Math.acos(d[1] / L) * 180 / Math.PI, yaw = Math.atan2(d[0], d[2]) * 180 / Math.PI;
  box(name, a, [w, L, w], swatch, [pitch, yaw, 0]);
}
/** A star: three crossed bars and a turned core, `s` pixels across. */
function star(name, at, s, bright = 'white') {
  const t = s * 0.18;
  box(name, [at[0], at[1] - s / 2, at[2]], [t, s, t], bright);
  box(name, [at[0] - s / 2, at[1], at[2]], [t, s, t], bright, [0, 0, -90]);
  box(name, [at[0], at[1], at[2] - s / 2], [t, s, t], bright, [90, 0, 0]);
  box(name, [at[0], at[1] - s * 0.22, at[2]], [s * 0.44, s * 0.44, s * 0.44], 'light', [45, 45, 0]);
}
/** A sword of light, its point at the end of `dir` from its pommel. */
function lightSword(name, pommel, dir) {
  const n = Math.hypot(...dir), u = dir.map(v => v / n), at = k => pommel.map((v, i) => v + u[i] * k);
  segment(name + '_grip', at(0), at(4), 0.8, 'silver');
  segment(name + '_guard', at(4), at(5), 5, 'light');
  segment(name + '_blade', at(5), at(24), 2.4, 'pale');
  segment(name + '_core', at(5), at(24.4), 1.0, 'white');
  segment(name + '_point', at(24), at(27), 1.2, 'white');
}

// ---------------------------------------------------------------- the scenes

const TARGET = soldier.OUTFITS[0];
function scene(name, build) {
  ELEMENTS.length = 0;
  build();
  const body = soldier.PIECES.map(p => ({ ...p }));
  const faces = kit.pack(body, 128);
  const project = kit.bbmodel('seven_stars_' + name, body, [TARGET], [kit.png(128, 128, kit.paintAtlas(faces, TARGET, 128))], 128, 'concept');
  const tex = fxTexture();
  project.textures.push({ ...project.textures[0], name: 'seven_stars_fx.png', id: '1', width: FX, height: FX, uv_width: FX, uv_height: FX,
    uuid: kit.stableId('seven_stars/fx'), source: 'data:image/png;base64,' + tex.toString('base64') });
  project.elements.push(...ELEMENTS);
  project.outliner.push(...ELEMENTS.map(e => e.uuid));
  project.animations = [{ uuid: kit.stableId('seven_stars/pose/' + name), name: 'pose', loop: 'once', override: false, length: 0, snapping: 20,
    selected: false, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators: {} }];
  fs.writeFileSync(path.join(OUT, name + '.bbmodel'), JSON.stringify(project));
  console.log('wrote', name + ':', ELEMENTS.length, 'effect boxes');
}

fs.mkdirSync(OUT, { recursive: true });

// Seven stars over the target, in the Dipper, tilted toward the viewer, joined by lines.
scene('constellation', () => {
  const at = ([u, v]) => [u * 7, 41 + v * 5, -v * 5];
  DIPPER.forEach((p, i) => star('star' + i, at(p), 5.5));
  for (const [i, j] of DIPPER_LINES) segment('line', at(DIPPER[i]), at(DIPPER[j]), 0.6, 'pale');
  // A faint glow on the target: a ring at its feet.
  box('ring', [0, 0.05, 0], [22, 0.1, 22], 'night', [0, 0, 0], SEALS.star);
});

// The rain: the zone round the target, four stars falling from up and behind, three craters.
scene('rain', () => {
  box('zone', [0, 0.05, 0], [80, 0.1, 80], 'night', [0, 0, 0], SEALS.dipper);
  const falls = [[[-14, 18, 10], 1.0], [[14, 34, -8], 0.9], [[2, 52, 16], 0.8], [[-24, 70, -14], 0.7]];
  for (const [[x, y, z], k] of falls) {
    star('falling', [x, y, z], 5 * k);
    // The trail, up and back the way it came (the sky to the viewer's right), thinning and darkening.
    const along = f => [x - 23 * f, y + 30 * f, z + 19 * f];
    segment('trail', [x, y, z], along(0.5), 2.4 * k, 'light');
    segment('trail', along(0.5), along(1.1), 1.6 * k, 'pale');
    segment('trail', along(1.1), along(1.8), 0.9 * k, 'blue');
  }
  for (const [x, z] of [[-22, -18], [20, 14], [8, -28]]) {
    box('crater', [x, 0.12, z], [14, 0.1, 14], 'night', [0, 0, 0], SEALS.star);
    star('burst', [x, 2.5, z], 4);
  }
});

// The array: the Dipper's seal round the target, seven swords of light on the Dipper's stars, their points on it.
scene('array', () => {
  box('seal', [0, 0.05, 0], [112, 0.1, 112], 'night', [0, 0, 0], SEALS.dipper);
  DIPPER.forEach(([u, v], i) => {
    const pos = [u * 12, 20 + (i % 2) * 6, -v * 12 - 4];
    // Drawn back from the target's chest, a little above it.
    const aim = [0, 22, 0].map((c, k) => c - pos[k]);
    // Drawn up a block from it, the points not yet on it (the moment before they strike).
    const n = Math.hypot(...aim), pommel = [0, 22, 0].map((c, k) => c - aim[k] / n * 43);
    lightSword('sword' + i, pommel, aim);
    star('glint', pos.map((p, k) => p - aim[k] / n * 2), 2.4);
  });
});
