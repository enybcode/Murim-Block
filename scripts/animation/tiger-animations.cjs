// The great tiger's animations (art/blockbench/tiger.bbmodel, its bones from tiger-model.cjs), written into the project
// and filmed in Blockbench itself to GIFs (docs/mobs/images/tiger-<name>.gif):
//   idle   (4 s, loop): it breathes, the head sweeps slowly round, the tail ripples, a snarl twitches the jaw.
//   walk   (1.4 s, loop): a heavy prowl, diagonal legs together, the elbows and knees folding as each paw swings
//          forward, the shoulders rolling, the head steady and low, the tail swaying.
//   swipe (0.8 s), bite (0.9 s), pounce (1.6 s), double_swipe (1.2 s), maul (1.8 s): its attacks (docs/mobs/tiger.md).
//   hurt (0.4 s) and death (1.4 s): it flinches; it falls on its side.
// Each animation is sampled from smooth curves every 0.05 s into linear keyframes.
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/animation/tiger-model.cjs && node scripts/animation/tiger-animations.cjs [--port=9232] [--film=false]
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { stableId } = require('./mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const FILE = path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);
const FPS = 20;
const TAU = Math.PI * 2;
const TAILS = 3;

const smooth = t => t * t * (3 - 2 * t);
/** The tail lashing with an attack: keys [[time, degrees]] for its first bones, the tip following a little behind. */
function tailLash(t, keys) {
  const a = keyed(keys.map(([k, v]) => [k, [v]]), t)[0], b = keyed(keys.map(([k, v]) => [k + 0.08, [v]]), t)[0];
  return { tail0: { rot: [a, 6 * Math.sin(t * 9), 0] }, tail1: { rot: [b * 0.8, 9 * Math.sin(t * 9 - 0.6), 0] }, tail2: { rot: [b * 0.6, 12 * Math.sin(t * 9 - 1.2), 0] } };
}
/** Between keys [[time, value]]: eased, held before the first and after the last. */
function keyed(keys, t) {
  if (t <= keys[0][0]) return keys[0][1];
  for (let i = 0; i < keys.length - 1; i++) {
    const [ta, a] = keys[i], [tb, b] = keys[i + 1];
    if (t <= tb) { const s = smooth((t - ta) / (tb - ta)); return a.map((v, k) => v + (b[k] - v) * s); }
  }
  return keys[keys.length - 1][1];
}

// Each pose function gives, for time t, {bone: {rot: [x, y, z] degrees, pos: [x, y, z] pixels}}.
// Rotation x: positive swings the lower end of a hanging leg back, lowers the front of the body and the nose, and drops
// the jaw open.
const ANIMS = {
  idle: { length: 4, loop: 'loop', pose(t) {
    const breath = Math.sin(TAU * t / 2), look = Math.sin(TAU * t / 4);
    const snarl = Math.max(0, Math.sin(Math.PI * (t - 2.3) / 0.7)) * (t > 2.3 && t < 3 ? 1 : 0);
    const out = {
      body: { pos: [0, 0.35 * breath, 0], rot: [0.6 * breath, 0, 0] },
      head: { rot: [3 * Math.sin(TAU * t / 2 + 1) - 0.6 * breath, 12 * look, 2 * look] },
      jaw: { rot: [9 * snarl, 0, 0] },
      front_leg_l: { rot: [-0.6 * breath, 0, 0] }, front_leg_r: { rot: [-0.6 * breath, 0, 0] },
      hind_leg_l: { rot: [-0.6 * breath, 0, 0] }, hind_leg_r: { rot: [-0.6 * breath, 0, 0] },
    };
    for (let i = 0; i < TAILS; i++) out['tail' + i] = { rot: [3 * Math.sin(TAU * t / 2 - i * 0.5), 9 * Math.sin(TAU * t / 4 - i * 0.55), 0] };
    return out;
  } },
  walk: { length: 1.4, loop: 'loop', pose(t) {
    const p = TAU * t / 1.4, A = 20;
    const swing = ph => A * Math.sin(p + ph);
    // The lower leg folds while its paw swings forward (the upper leg's swing turning forward).
    const fold = ph => Math.max(0, Math.cos(p + ph)) * 24;
    const out = {
      body: { pos: [0, 0.8 * Math.cos(2 * p), 0], rot: [1.2 * Math.sin(2 * p), 2 * Math.sin(p), 2.5 * Math.sin(p)] },
      head: { rot: [1.2 * Math.sin(2 * p) + 2 * Math.cos(2 * p), -3 * Math.sin(p), -2 * Math.sin(p)] },
      jaw: { rot: [0, 0, 0] },
      front_leg_l: { rot: [swing(0), 0, 0] }, front_foot_l: { rot: [fold(0), 0, 0] },
      front_leg_r: { rot: [swing(Math.PI), 0, 0] }, front_foot_r: { rot: [fold(Math.PI), 0, 0] },
      hind_leg_r: { rot: [swing(0.4), 0, 0] }, hind_foot_r: { rot: [fold(0.4) * 0.7, 0, 0] },
      hind_leg_l: { rot: [swing(Math.PI + 0.4), 0, 0] }, hind_foot_l: { rot: [fold(Math.PI + 0.4) * 0.7, 0, 0] },
    };
    for (let i = 0; i < TAILS; i++) out['tail' + i] = { rot: [4 * Math.cos(2 * p - i * 0.4), 13 * Math.sin(p - i * 0.6), 0] };
    return out;
  } },
  // ---------------------------------------------------------------- attacks
  // swipe (0.8 s): the right paw cocked up and out, then raked across and down.
  swipe: { length: 0.8, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.18, [0, 0.5, 1]], [0.32, [0, -1, -2]], [0.45, [0, -1, -2]], [0.8, [0, 0, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.18, [-3, -8, -3]], [0.32, [4, 10, 3]], [0.45, [4, 9, 3]], [0.8, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.18, [-6, -10, 0]], [0.32, [6, 10, 0]], [0.8, [0, 0, 0]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.18, [10, 0, 0]], [0.32, [22, 0, 0]], [0.55, [12, 0, 0]], [0.8, [0, 0, 0]]]) },
      front_leg_r: { rot: K([[0, [0, 0, 0]], [0.18, [-78, 22, -28]], [0.32, [38, -30, 14]], [0.45, [26, -22, 8]], [0.8, [0, 0, 0]]]) },
      front_foot_r: { rot: K([[0, [0, 0, 0]], [0.18, [50, 0, 0]], [0.32, [-10, 0, 0]], [0.8, [0, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.18, [-4, 0, 0]], [0.32, [8, 0, 0]], [0.8, [0, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.32, [-8, 0, 0]], [0.8, [0, 0, 0]]]) }, hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.32, [-8, 0, 0]], [0.8, [0, 0, 0]]]) },
      ...tailLash(t, [[0, 0], [0.18, -10], [0.32, 12], [0.8, 0]]),
    };
  } },
  // bite (0.9 s): it draws back with the jaws wide, lunges, snaps them shut, and wrenches its head side to side.
  bite: { length: 0.9, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    const shake = t > 0.45 && t < 0.8 ? Math.sin((t - 0.45) / 0.35 * Math.PI * 3) * (1 - (t - 0.45) / 0.35) : 0;
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.25, [0, -1.5, 2.5]], [0.42, [0, 0, -5]], [0.6, [0, -0.5, -4]], [0.9, [0, 0, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.25, [-4, 0, 0]], [0.42, [7, 0, 0]], [0.6, [5, 0, 0]], [0.9, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.25, [-14, 0, 0]], [0.42, [12, 0, 0]], [0.6, [8, 0, 0]], [0.9, [0, 0, 0]]]).map((v, i) => i === 1 ? v + 16 * shake : i === 2 ? v + 8 * shake : v) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.25, [32, 0, 0]], [0.38, [34, 0, 0]], [0.44, [0, 0, 0]], [0.9, [0, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.25, [6, 0, 0]], [0.42, [-26, 0, 0]], [0.6, [-18, 0, 0]], [0.9, [0, 0, 0]]]) },
      front_leg_r: { rot: K([[0, [0, 0, 0]], [0.25, [6, 0, 0]], [0.42, [-22, 0, 0]], [0.6, [-16, 0, 0]], [0.9, [0, 0, 0]]]) },
      front_foot_l: { rot: K([[0, [0, 0, 0]], [0.42, [12, 0, 0]], [0.9, [0, 0, 0]]]) }, front_foot_r: { rot: K([[0, [0, 0, 0]], [0.42, [12, 0, 0]], [0.9, [0, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.25, [-12, 0, 0]], [0.42, [22, 0, 0]], [0.6, [16, 0, 0]], [0.9, [0, 0, 0]]]) },
      hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.25, [-12, 0, 0]], [0.42, [22, 0, 0]], [0.6, [16, 0, 0]], [0.9, [0, 0, 0]]]) },
      hind_foot_l: { rot: K([[0, [0, 0, 0]], [0.25, [18, 0, 0]], [0.42, [-4, 0, 0]], [0.9, [0, 0, 0]]]) },
      hind_foot_r: { rot: K([[0, [0, 0, 0]], [0.25, [18, 0, 0]], [0.42, [-4, 0, 0]], [0.9, [0, 0, 0]]]) },
      ...tailLash(t, [[0, 0], [0.25, -12], [0.42, 10], [0.9, 0]]),
    };
  } },
  // pounce (1.6 s): it sinks low, springs forward through the air forelegs out and jaws open, lands on its forepaws,
  // and gathers itself (the game moves the tiger itself; here it slides back to its place at the end).
  pounce: { length: 1.6, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.35, [0, -4, 2]], [0.62, [0, 9, -14]], [0.86, [0, 0, -22]], [1.05, [0, -2, -22]], [1.35, [0, 0, -22]], [1.6, [0, 0, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.35, [3, 0, 0]], [0.62, [-12, 0, 0]], [0.86, [10, 0, 0]], [1.05, [5, 0, 0]], [1.35, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.35, [8, 0, 0]], [0.62, [-8, 0, 0]], [0.86, [8, 0, 0]], [1.35, [0, 0, 0]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.35, [6, 0, 0]], [0.55, [30, 0, 0]], [0.9, [26, 0, 0]], [1.1, [0, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.35, [-18, 0, 0]], [0.62, [-72, 0, -6]], [0.86, [-22, 0, 0]], [1.05, [-4, 0, 0]], [1.35, [0, 0, 0]]]) },
      front_leg_r: { rot: K([[0, [0, 0, 0]], [0.35, [-18, 0, 0]], [0.62, [-68, 0, 6]], [0.86, [-18, 0, 0]], [1.05, [-4, 0, 0]], [1.35, [0, 0, 0]]]) },
      front_foot_l: { rot: K([[0, [0, 0, 0]], [0.35, [40, 0, 0]], [0.62, [18, 0, 0]], [0.86, [0, 0, 0]], [1.05, [26, 0, 0]], [1.35, [0, 0, 0]]]) },
      front_foot_r: { rot: K([[0, [0, 0, 0]], [0.35, [40, 0, 0]], [0.62, [18, 0, 0]], [0.86, [0, 0, 0]], [1.05, [26, 0, 0]], [1.35, [0, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.35, [-30, 0, 0]], [0.62, [52, 0, 0]], [0.86, [22, 0, 0]], [1.05, [-10, 0, 0]], [1.35, [0, 0, 0]]]) },
      hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.35, [-30, 0, 0]], [0.62, [56, 0, 0]], [0.86, [24, 0, 0]], [1.05, [-10, 0, 0]], [1.35, [0, 0, 0]]]) },
      hind_foot_l: { rot: K([[0, [0, 0, 0]], [0.35, [46, 0, 0]], [0.62, [-12, 0, 0]], [0.86, [10, 0, 0]], [1.05, [24, 0, 0]], [1.35, [0, 0, 0]]]) },
      hind_foot_r: { rot: K([[0, [0, 0, 0]], [0.35, [46, 0, 0]], [0.62, [-12, 0, 0]], [0.86, [10, 0, 0]], [1.05, [24, 0, 0]], [1.35, [0, 0, 0]]]) },
      ...tailLash(t, [[0, 0], [0.35, -18], [0.62, 16], [0.86, -10], [1.35, 0]]),
    };
  } },
  // double_swipe (1.2 s): a rake with the right paw, then a heavier one back with the left.
  double_swipe: { length: 1.2, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.3, [0, -1, -2]], [0.62, [0, -1.5, -3]], [1.2, [0, 0, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.15, [-3, -8, -3]], [0.3, [4, 10, 3]], [0.45, [-4, 10, 4]], [0.62, [6, -14, -4]], [0.8, [5, -12, -3]], [1.2, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.15, [-6, -10, 0]], [0.3, [6, 10, 0]], [0.45, [-4, 10, 0]], [0.62, [8, -14, 0]], [1.2, [0, 0, 0]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.15, [12, 0, 0]], [0.62, [28, 0, 0]], [0.9, [10, 0, 0]], [1.2, [0, 0, 0]]]) },
      front_leg_r: { rot: K([[0, [0, 0, 0]], [0.15, [-78, 22, -28]], [0.3, [38, -30, 14]], [0.45, [10, -10, 4]], [0.7, [4, 0, 0]], [1.2, [0, 0, 0]]]) },
      front_foot_r: { rot: K([[0, [0, 0, 0]], [0.15, [50, 0, 0]], [0.3, [-10, 0, 0]], [0.6, [0, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.3, [6, 0, 0]], [0.45, [-80, -24, 30]], [0.62, [40, 32, -16]], [0.8, [28, 24, -10]], [1.2, [0, 0, 0]]]) },
      front_foot_l: { rot: K([[0, [0, 0, 0]], [0.45, [52, 0, 0]], [0.62, [-12, 0, 0]], [1.2, [0, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.62, [-10, 0, 0]], [1.2, [0, 0, 0]]]) }, hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.62, [-10, 0, 0]], [1.2, [0, 0, 0]]]) },
      ...tailLash(t, [[0, 0], [0.15, -10], [0.3, 12], [0.45, -10], [0.62, 14], [1.2, 0]]),
    };
  } },
  // hurt (0.4 s): a flinch, the head thrown back and aside, the body jolted.
  hurt: { length: 0.4, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.08, [0, -0.6, 1.2]], [0.4, [0, 0, 0]]]), rot: K([[0, [0, 0, 0]], [0.08, [-4, 5, 3]], [0.4, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.08, [-16, 14, 6]], [0.4, [0, 0, 0]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.08, [24, 0, 0]], [0.4, [0, 0, 0]]]) },
      ...tailLash(t, [[0, 0], [0.08, -16], [0.4, 0]]),
    };
  } },
  // death (1.4 s): it staggers, falls on its right side, the legs stiffen out, the head and tail go limp.
  death: { length: 1.4, loop: 'once', pose(t) {
    const K = keys => keyed(keys, t);
    return {
      body: { pos: K([[0, [0, 0, 0]], [0.3, [0, -1, 0]], [0.8, [0, -14.5, 0]], [1.4, [0, -14.5, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.3, [-6, 0, -8]], [0.8, [0, 0, 88]], [0.95, [0, 0, 84]], [1.4, [0, 0, 88]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.3, [-18, 0, 0]], [0.8, [10, 0, -20]], [1.4, [14, 0, -24]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.3, [26, 0, 0]], [0.8, [12, 0, 0]], [1.4, [10, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.8, [-20, 0, 0]], [1.4, [-24, 0, 0]]]) }, front_leg_r: { rot: K([[0, [0, 0, 0]], [0.8, [-10, 0, 0]], [1.4, [-12, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.8, [18, 0, 0]], [1.4, [22, 0, 0]]]) }, hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.8, [10, 0, 0]], [1.4, [12, 0, 0]]]) },
      tail0: { rot: K([[0, [0, 0, 0]], [0.8, [-20, 0, 0]], [1.4, [-24, 0, 0]]]) }, tail1: { rot: K([[0, [0, 0, 0]], [1.4, [10, 0, 0]]]) }, tail2: { rot: K([[0, [0, 0, 0]], [1.4, [20, 0, 0]]]) },
    };
  } },
  // maul (1.8 s): the heavy blow: it sinks on its haunches, rears up roaring, the right paw high, and smashes it down.
  maul: { length: 1.8, loop: 'once', pose(t) {
    const K = (keys) => keyed(keys, t);
    const out = {
      body: { pos: K([[0, [0, 0, 0]], [0.45, [0, -3.5, 2]], [0.72, [0, 3, -2]], [0.92, [0, -1.5, -4]], [1.15, [0, -1, -4]], [1.8, [0, 0, 0]]]),
        rot: K([[0, [0, 0, 0]], [0.45, [6, 0, 0]], [0.72, [-20, -6, 0]], [0.92, [6, 8, -4]], [1.15, [4, 6, -3]], [1.8, [0, 0, 0]]]) },
      head: { rot: K([[0, [0, 0, 0]], [0.45, [10, 0, 0]], [0.72, [-12, 8, 0]], [0.92, [8, -10, 0]], [1.15, [6, -8, 0]], [1.8, [0, 0, 0]]]) },
      jaw: { rot: K([[0, [0, 0, 0]], [0.45, [6, 0, 0]], [0.62, [34, 0, 0]], [0.92, [28, 0, 0]], [1.2, [12, 0, 0]], [1.8, [0, 0, 0]]]) },
      front_leg_r: { rot: K([[0, [0, 0, 0]], [0.45, [8, 0, 0]], [0.72, [-82, -10, -14]], [0.92, [26, 14, 8]], [1.15, [20, 10, 4]], [1.8, [0, 0, 0]]]) },
      front_foot_r: { rot: K([[0, [0, 0, 0]], [0.45, [0, 0, 0]], [0.72, [48, 0, 0]], [0.92, [-6, 0, 0]], [1.8, [0, 0, 0]]]) },
      front_leg_l: { rot: K([[0, [0, 0, 0]], [0.45, [8, 0, 0]], [0.72, [-30, 0, 0]], [0.92, [6, 0, 0]], [1.8, [0, 0, 0]]]) },
      front_foot_l: { rot: K([[0, [0, 0, 0]], [0.72, [26, 0, 0]], [0.92, [0, 0, 0]], [1.8, [0, 0, 0]]]) },
      hind_leg_l: { rot: K([[0, [0, 0, 0]], [0.45, [-16, 0, 0]], [0.72, [-24, 0, 0]], [0.92, [-6, 0, 0]], [1.8, [0, 0, 0]]]) },
      hind_leg_r: { rot: K([[0, [0, 0, 0]], [0.45, [-16, 0, 0]], [0.72, [-24, 0, 0]], [0.92, [-6, 0, 0]], [1.8, [0, 0, 0]]]) },
      hind_foot_l: { rot: K([[0, [0, 0, 0]], [0.45, [20, 0, 0]], [0.72, [12, 0, 0]], [1.8, [0, 0, 0]]]) },
      hind_foot_r: { rot: K([[0, [0, 0, 0]], [0.45, [20, 0, 0]], [0.72, [12, 0, 0]], [1.8, [0, 0, 0]]]) },
    };
    const lash = K([[0, [0]], [0.45, [-14]], [0.72, [10]], [0.92, [-8]], [1.8, [0]]])[0];
    for (let i = 0; i < TAILS; i++) out['tail' + i] = { rot: [lash * (i < 3 ? 1 : 0.5), 8 * Math.sin(TAU * t / 0.9 - i * 0.6), 0] };
    return out;
  } },
};

// ---------------------------------------------------------------- into the project
const project = JSON.parse(fs.readFileSync(FILE, 'utf8'));
const groups = {};
const walk = g => { if (g && typeof g === 'object') { groups[g.name] = g; (g.children || []).forEach(walk); } };
project.outliner.forEach(walk);
const r3 = v => Math.round(v * 1000) / 1000;
project.animations = Object.entries(ANIMS).map(([name, a]) => {
  const animators = {};
  const steps = Math.round(a.length * FPS);
  for (let s = 0; s <= steps; s++) {
    const t = s / FPS, pose = a.pose(t);
    for (const [bone, { rot, pos }] of Object.entries(pose)) {
      const g = groups[bone];
      if (!g) throw new Error('no bone ' + bone);
      const an = animators[g.uuid] ||= { name: bone, type: 'bone', keyframes: [] };
      if (rot) an.keyframes.push({ channel: 'rotation', data_points: [{ x: r3(rot[0]), y: r3(rot[1]), z: r3(rot[2]) }], uuid: stableId(`tiger/${name}/${bone}/r${s}`), time: r3(t), color: -1, interpolation: 'linear' });
      if (pos) an.keyframes.push({ channel: 'position', data_points: [{ x: r3(pos[0]), y: r3(pos[1]), z: r3(pos[2]) }], uuid: stableId(`tiger/${name}/${bone}/p${s}`), time: r3(t), color: -1, interpolation: 'linear' });
    }
  }
  return { uuid: stableId('tiger/anim/' + name), name, loop: a.loop, override: false, length: a.length, snapping: FPS, selected: false,
    anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators };
});
fs.writeFileSync(FILE, JSON.stringify(project));
console.log('tiger animations:', project.animations.map(a => `${a.name} ${a.length}s`).join(', '));

// ---------------------------------------------------------------- filming
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
/** Views: three-quarter from the front left, and the left side (walk). */
const VIEWS = { three: { position: [-92, 58, -108], target: [0, 18, -4] }, side: { position: [-150, 24, 0], target: [0, 18, 0] } };
const FILMS = { idle: ['three'], walk: ['three', 'side'], swipe: ['three'], bite: ['three', 'side'], pounce: ['side', 'three'], double_swipe: ['three'], maul: ['three', 'side'], hurt: ['three'], death: ['three'] };

async function film() {
  const bb = await connect(PORT);
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'tiger.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 60 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 400)); return Outliner.elements.length + ' cubes, ' + Animation.all.length + ' animations'; })()`);
  console.log('loaded', loaded);
  const out = path.join(ROOT, 'build', 'tiger-animations');
  for (const [name, views] of Object.entries(FILMS).filter(([n]) => !arg('only') || arg('only').split(',').includes(n))) for (const view of views) {
    const dir = path.join(out, name, view);
    fs.rmSync(dir, { recursive: true, force: true });
    fs.mkdirSync(dir, { recursive: true });
    const a = ANIMS[name], frames = Math.round(a.length * FPS) + (a.loop === 'loop' ? 0 : 6);
    await bb.evaluate(`(() => { Animation.all.find(a => a.name === ${JSON.stringify(name)}).select(); return true; })()`);
    for (let f = 0; f < frames; f++) {
      const t = Math.min(f / FPS, a.length);
      const png = await bb.evaluate(`(async () => {
        Timeline.setTime(${t}); Animator.preview();
        const p = window.__tigerFilm || (window.__tigerFilm = new Preview({ id: 'tiger_film', offscreen: true, antialias: true }));
        p.resize(720, 480);
        p.loadAnglePreset({ position: ${JSON.stringify(VIEWS[view].position)}, target: ${JSON.stringify(VIEWS[view].target)}, projection: 'perspective', fov: 36 });
        p.camera.lookAt(...${JSON.stringify(VIEWS[view].target)});
        const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#EDE4CF');
        return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
      })()`);
      fs.writeFileSync(path.join(dir, `frame-${String(f).padStart(3, '0')}.png`), Buffer.from(png.split(',')[1], 'base64'));
    }
    const gif = path.join(ROOT, 'docs', 'mobs', 'images', `tiger-${name}${view === 'three' ? '' : '-' + view}.gif`);
    execFileSync('java', [path.join(__dirname, 'FramesToGif.java'), dir, gif, String(1000 / FPS), '720'], { stdio: 'inherit' });
    console.log(name, view, frames, 'frames ->', path.relative(ROOT, gif));
  }
  bb.close();
}
if (arg('film') !== 'false') film().catch(e => { console.error(e); process.exit(1); });
