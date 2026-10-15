// Concept sheets for the crab art's transformation (a design to choose from, not game files), after Enzo's reference:
// not a clean crab costume but an infection. The player's flesh turns a dark raw red, a brown crust full of knobs and
// spikes grows over it, the hands stretch into long thin pincers down to the ground. Three stages:
//   human     the player as he is;
//   infected  stage 1: the forearms turned to raw red flesh in crusted patches, dark veins up the arms and the neck, the
//             hands stretched into long pincers down to the knees; crust on a knee and a shoulder;
//   shell     stage 2: the shirt gone over a red, ridged torso; a knobbed brown shell over the back and up over the head
//             like a hood, its rim over the brow; the pincers down to the shins; crusted knees and clawed feet;
//   king      stage 3: the hood become a great knobbed carapace over the shoulders, blue eyes on red stalks under its rim,
//             mandibles at the mouth, a second pair of thin arms with small pincers, the pincers down to the ground.
// Rendered on a dark ground from the front three-quarter and the back three-quarter, and the four side by side, to
// docs/combat/styles/images/crab-art-<stage>-<view>.png. Blockbench pixels: x the wearer's right, y up, facing -z.
//
//   node scripts/concepts/crab-art-stages.cjs [--port=9234]
const fs = require('node:fs');
const path = require('node:path');
const { palette, textureOf, connect, id } = require('./chief-axes.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9234);

const COLOURS = {
  skin: '#c69c7b', hair: '#3b2a1c', shirt: '#4a4f57', trousers: '#2e3247', shoe: '#3a3a3a', eye: '#3c2a8a',
  flesh: '#8a1f27', fleshDark: '#4a0d14', vein: '#260509', crust: '#6b4528', crustDark: '#33200f', knob: '#8a5e3a',
  claw: '#7a1820', tip: '#1e0a08', eyeBlue: '#3aa8f0', bone: '#d9cfb8',
};
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];

/** A seeded random (the same knobs on every render). */
function rng(seed) {
  let s = seed >>> 0;
  return () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 4294967296; };
}

/** The parts of one figure: box(mat, shade 0-15, from, to, [rotation [x, y, z] about origin]). */
function builder(seed) {
  const parts = [], r = rng(seed);
  const box = (mat, shade, from, to, extra = {}) => parts.push({ mat, shade, from, to, ...extra });
  return { parts, box, r };
}

/** A rod of square section `t` from p0 to p1 (built along +x, turned: Blockbench applies z, then y). */
function rod(b, mat, shade, p0, p1, t, extra = {}) {
  const d = p1.map((v, i) => v - p0[i]), len = Math.hypot(...d);
  const rz = Math.atan2(d[1], d[0]) * 180 / Math.PI, ry = -Math.asin(d[2] / len) * 180 / Math.PI;
  b.box(mat, shade, [p0[0], p0[1] - t / 2, p0[2] - t / 2], [p0[0] + len, p0[1] + t / 2, p0[2] + t / 2], { origin: p0, rotation: [0, ry, rz], ...extra });
}

/**
 * Knobs of crust scattered over a face of a box (axis 'x', 'y' or 'z', side +1 or -1): `n` lumps, each a small cube,
 * some with a spike on top, their shades varied, so the crust reads rough.
 */
function knobs(b, from, to, axis, side, n, size = 1.2, extra = {}) {
  const ax = { x: 0, y: 1, z: 2 }[axis], u = (ax + 1) % 3, v = (ax + 2) % 3;
  for (let i = 0; i < n; i++) {
    const s = size * (0.6 + b.r() * 0.8), pu = from[u] + b.r() * (to[u] - from[u]), pv = from[v] + b.r() * (to[v] - from[v]);
    const face = side > 0 ? to[ax] : from[ax];
    const lo = [0, 0, 0], hi = [0, 0, 0];
    lo[u] = pu - s / 2; hi[u] = pu + s / 2; lo[v] = pv - s / 2; hi[v] = pv + s / 2;
    lo[ax] = side > 0 ? face - 0.2 : face - s * 0.7; hi[ax] = side > 0 ? face + s * 0.7 : face + 0.2;
    b.box(b.r() < 0.5 ? 'knob' : 'crust', 5 + Math.floor(b.r() * 8), lo, hi, extra);
    if (b.r() < 0.35) {
      const t = s * 0.45, lo2 = [...lo], hi2 = [...hi];
      lo2[u] = pu - t / 2; hi2[u] = pu + t / 2; lo2[v] = pv - t / 2; hi2[v] = pv + t / 2;
      if (side > 0) { lo2[ax] = hi[ax] - 0.1; hi2[ax] = hi[ax] + s * 0.8; } else { hi2[ax] = lo[ax] + 0.1; lo2[ax] = lo[ax] - s * 0.8; }
      b.box('crustDark', 4 + Math.floor(b.r() * 5), lo2, hi2, extra);
    }
  }
}

// ---------------------------------------------------------------- the player and the arms' pose

/** The arms hang a little forward and out, heavy with what they carry: about the shoulder. */
const armPose = (side, out = 8) => ({ origin: [side * 6, 22, 0], rotation: [12, 0, side * out] });

/** The player, his shirt and arms as `stage` leaves them (0 human, 1 infected forearms, 2+ the shirt gone). */
function player(b, stage) {
  for (const x0 of [-4, 0]) {
    b.box('trousers', 6, [x0, 1, -2], [x0 + 4, 12, 2]);
    if (stage < 2) b.box('shoe', 6, [x0, 0, -2], [x0 + 4, 1, 2]);
  }
  if (stage < 2) b.box('shirt', 7, [-4, 12, -2], [4, 24, 2]);
  else torso(b);
  for (const side of [1, -1]) {
    const x0 = side > 0 ? 4 : -8, pose = armPose(side, stage ? 8 : 6);
    b.box(stage < 2 ? 'shirt' : 'flesh', stage < 2 ? 8 : 7, [x0, 18, -2], [x0 + 4, 24, 2], pose);
    b.box(stage ? 'flesh' : 'skin', 7, [x0, 12, -2], [x0 + 4, 18, 2], pose);
  }
  b.box(stage >= 3 ? 'fleshDark' : 'skin', 8, [-4, 24, -4], [4, 32, 4]);
  b.box('hair', 6, [-4.1, 29, -4.1], [4.1, 32.1, 4.1]);
  b.box('hair', 6, [-4.1, 24, 2], [4.1, 29, 4.1]);
  for (const x0 of [-3, 1]) b.box(stage >= 3 ? 'vein' : stage >= 2 ? 'eyeBlue' : 'eye', stage === 2 ? 12 : 6, [x0, 27, -4.15], [x0 + 2, 28, -4]);
}

/** A raw red torso, ridged like a crab's underside and a flayed chest: darker grooves between swollen plates. */
function torso(b) {
  b.box('flesh', 6, [-4, 12, -2], [4, 24, 2]);
  b.box('flesh', 9, [-3.6, 19.5, -2.4], [-0.3, 23, -2]);
  b.box('flesh', 9, [0.3, 19.5, -2.4], [3.6, 23, -2]);
  for (let i = 0; i < 3; i++) {
    const y = 13 + i * 2.1;
    b.box('flesh', 8 - i, [-2.6, y, -2.35], [-0.25, y + 1.7, -2]);
    b.box('flesh', 8 - i, [0.25, y, -2.35], [2.6, y + 1.7, -2]);
  }
  b.box('fleshDark', 4, [-0.25, 12.5, -2.3], [0.25, 23, -2.05]);
}

/** Dark veins running up from the forearm (on the arm's front and outer side). */
function veins(b, side, pose) {
  const x = side * 6;
  b.box('vein', 6, [x - 0.3, 14, -2.12], [x + 0.2, 21, -2.02], pose);
  b.box('vein', 6, [x + 0.2, 19, -2.12], [x + 1.4, 19.5, -2.02], pose);
  b.box('vein', 6, [x + side * 2.02 - (side > 0 ? 0 : 0.1), 15, -0.3], [x + side * 2.02 + (side > 0 ? 0.1 : 0), 22.5, 0.2], pose);
}

/**
 * A long thin pincer from the wrist down (`len` its length below the wrist, `k` its thickness): the hand swollen into a
 * narrow red palm with crusted knuckles, then two long fingers, the outer one longer and hooked, dark tips.
 */
function pincer(b, side, len, k, pose = armPose(side, 8)) {
  const cx = side * 6, add = (mat, shade, from, to) => b.box(mat, shade, from, to, pose);
  const w = 1.7 * k, d = 2.1 * k, top = 12.5, palm = top - len * 0.35;
  add('flesh', 7, [cx - w, palm, -d], [cx + w, top, d]);
  add('fleshDark', 5, [cx - w - 0.05, palm + (top - palm) * 0.45, -d - 0.05], [cx + w + 0.05, palm + (top - palm) * 0.55, d + 0.05]);
  knobs(b, [cx - w, palm + 1, -d], [cx + w, top, d], 'x', side, 3, 1.1 * k, pose);
  knobs(b, [cx - w, palm + 1, -d], [cx + w, top, d], 'z', 1, 2, 1.0 * k, pose);
  // The fingers: the outer one long and hooked inward at its end, the inner one shorter.
  const f = 1.1 * k, gap = 0.5 * k;
  const fingers = [[cx + side * (gap / 2 + f / 2), len * 0.65, true], [cx - side * (gap / 2 + f / 2), len * 0.5, false]];
  for (const [x, l, outer] of fingers) {
    const bot = palm - l;
    add('claw', outer ? 8 : 7, [x - f / 2, palm - l * 0.55, -f], [x + f / 2, palm, f]);
    add('claw', outer ? 9 : 8, [x - f * 0.38, bot + l * 0.1, -f * 0.75], [x + f * 0.38, palm - l * 0.55, f * 0.75]);
    add('tip', 6, [x - f * 0.3, bot - 0.6, -f * 0.5], [x + f * 0.3, bot + l * 0.1, f * 0.5]);
    if (outer) add('tip', 6, [Math.min(x, x - side * f), bot - 0.6, -f * 0.45], [Math.max(x, x - side * f), bot + 0.3, f * 0.45]);
    // Notches on the inner edge.
    const toIn = outer ? -side : side;
    for (let i = 0; i < 3; i++) {
      const y = palm - l * (0.2 + i * 0.22), xi = x + toIn * f / 2;
      add('bone', 10, [Math.min(xi, xi + toIn * 0.4), y - 0.5, -0.35], [Math.max(xi, xi + toIn * 0.4), y, 0.35]);
    }
  }
}

// ---------------------------------------------------------------- the crust

/** Crust patches: on a shoulder, on a knee (stage 1), on both knees and over the feet with claws (stage 2+). */
function patches(b, stage) {
  const sh = armPose(1, 8);
  b.box('crust', 7, [3.8, 21, -2.3], [8.3, 24.4, 2.3], sh);
  knobs(b, [3.8, 21, -2.3], [8.3, 24.4, 2.3], 'y', 1, 4, 1.3, sh);
  knobs(b, [3.8, 21, -2.3], [8.3, 24.4, 2.3], 'x', 1, 3, 1.2, sh);
  const knees = stage >= 2 ? [-4, 0] : [-4];
  for (const x0 of knees) {
    b.box('crust', 7, [x0 - 0.3, 4, -2.4], [x0 + 4.3, 8.5, 1.5]);
    knobs(b, [x0 - 0.3, 4, -2.4], [x0 + 4.3, 8.5, 1.5], 'z', -1, 4, 1.3);
  }
  if (stage < 2) return;
  for (const x0 of [-4, 0]) {
    b.box('crust', 6, [x0 - 0.4, 0, -2.6], [x0 + 4.4, 2.6, 2.4]);
    knobs(b, [x0 - 0.4, 0, -2.6], [x0 + 4.4, 2.6, 2.4], 'y', 1, 3, 1.0);
    for (const t of [0.8, 2, 3.2]) b.box('tip', 5, [x0 + t - 0.4, 0, -4], [x0 + t + 0.4, 0.9, -2.6]);
  }
}

/** The shell over the back and up over the head like a hood, its rim jutting over the brow; `big` for stage 3. */
function hood(b, big) {
  const W = big ? 8.5 : 6.8, top = big ? 37 : 34.5, front = big ? -6.5 : -5;
  // The back: a dome.
  b.box('crust', 7, [-W, 13, 2], [W, 26, 4]);
  b.box('crust', 8, [-W + 1.2, 14, 4], [W - 1.2, 25, 5.6]);
  b.box('crust', 8, [-W + 2.5, 15.5, 5.6], [W - 2.5, 23.5, 6.6]);
  knobs(b, [-W, 13, 2], [W, 26, 6.6], 'z', 1, big ? 16 : 10, 1.6);
  // Over the shoulders (behind the neck) and the head: walls at the sides and the back, a cap over the top jutting
  // forward; the face stays open under it.
  b.box('crust', 7, [-W, 24, -0.5], [W, 27.5, 4.5]);
  b.box('crust', 7, [-W, 24, -3], [-4.3, 27, -0.5]);
  b.box('crust', 7, [4.3, 24, -3], [W, 27, -0.5]);
  b.box('crust', 8, [-W + 0.6, 27.5, 0], [-4.2, 32, 5]);
  b.box('crust', 8, [4.2, 27.5, 0], [W - 0.6, 32, 5]);
  b.box('crust', 8, [-4.2, 27.5, 4.2], [4.2, 32, 5]);
  b.box('crust', 8, [-W + 0.6, 32, front + 1], [W - 0.6, top - 1.5, 5]);
  b.box('crust', 9, [-W + 1.6, top - 1.5, front + 1.5], [W - 1.6, top, 4.5]);
  knobs(b, [-W + 0.6, top - 1.5, front + 1], [W - 0.6, top, 5], 'y', 1, big ? 18 : 12, 1.8);
  knobs(b, [-W, 24, -1], [W, top - 1.5, 5], 'x', 1, big ? 7 : 4, 1.5);
  knobs(b, [-W, 24, -1], [W, top - 1.5, 5], 'x', -1, big ? 7 : 4, 1.5);
  // The rim over the brow, ragged.
  b.box('crustDark', 6, [-W + 0.6, 32, front], [W - 0.6, 33.5, front + 1.2]);
  for (let x = -W + 1; x < W - 1; x += 1.6) b.box('crust', 5 + Math.floor(b.r() * 5), [x, 31.2 - b.r() * 0.7, front + 0.1], [x + 1.2, 32.1, front + 1]);
  knobs(b, [-W + 0.6, 32, front], [W - 0.6, 33.5, front + 1.2], 'z', -1, big ? 6 : 4, 1.4);
}

/** Stage 3: eyes on stalks under the rim, mandibles at the mouth, a second pair of thin arms with small pincers. */
function king(b) {
  for (const x of [-2.6, 2.6]) {
    rod(b, 'flesh', 8, [x, 31, -5.2], [x * 1.25, 33.4, -7.6], 0.9);
    b.box('flesh', 9, [x * 1.25 - 0.9, 32.7, -8.5], [x * 1.25 + 0.9, 34.5, -6.9]);
    b.box('eyeBlue', 12, [x * 1.25 - 0.7, 32.9, -8.65], [x * 1.25 + 0.7, 34.3, -8.4]);
    b.box('vein', 4, [x * 1.25 - 0.2, 33.4, -8.75], [x * 1.25 + 0.2, 33.8, -8.6]);
  }
  for (const side of [1, -1]) {
    b.box('flesh', 8, [side * 0.6 - 0.8, 24.3, -4.9], [side * 0.6 + 0.8, 26.2, -4]);
    rod(b, 'crust', 7, [side * 1.8, 25.6, -4.4], [side * 1.2, 23.6, -5.6], 0.7);
    // A thin arm from under the shoulder, hanging along the big one, a small pincer.
    const pose = { origin: [side * 4.5, 20, -1], rotation: [6, 0, side * 4] };
    b.box('flesh', 8, [side * 4.5 - 0.6, 6, -1.6], [side * 4.5 + 0.6, 20, -0.4], pose);
    b.box('fleshDark', 5, [side * 4.5 - 0.65, 13, -1.65], [side * 4.5 + 0.65, 13.6, -0.35], pose);
    b.box('claw', 8, [side * 4.5 - 0.5, 2.5, -1.5], [side * 4.5 + 0.1, 6, -0.6], pose);
    b.box('claw', 8, [side * 4.5 + 0.2, 3.2, -1.4], [side * 4.5 + 0.6, 6, -0.7], pose);
    b.box('tip', 6, [side * 4.5 - 0.4, 1.8, -1.4], [side * 4.5, 2.5, -0.7], pose);
  }
}

const STAGES = {
  human: { title: 'Human', build: b => player(b, 0) },
  infected: {
    title: 'Stage 1 · Infected', build: b => {
      player(b, 1);
      for (const side of [1, -1]) { veins(b, side, armPose(side, 8)); pincer(b, side, side > 0 ? 11 : 9, side > 0 ? 1 : 0.85); }
      b.box('vein', 6, [1.5, 24, -4.12], [2, 26, -4.02]);
      b.box('vein', 6, [2, 24.8, -4.12], [3.2, 25.2, -4.02]);
      patches(b, 1);
    },
  },
  shell: {
    title: 'Stage 2 · Shell', build: b => {
      player(b, 2);
      for (const side of [1, -1]) pincer(b, side, side > 0 ? 13.5 : 11.5, side > 0 ? 1.15 : 1);
      patches(b, 2); hood(b, false);
    },
  },
  king: {
    title: 'Stage 3 · Crab King', build: b => {
      player(b, 3);
      for (const side of [1, -1]) pincer(b, side, side > 0 ? 15.5 : 14, side > 0 ? 1.35 : 1.2, armPose(side, 12));
      patches(b, 3); hood(b, true); king(b);
    },
  },
};

// ---------------------------------------------------------------- Blockbench projects and rendering

function elements(key, dx = 0) {
  const b = builder(Object.keys(STAGES).indexOf(key) * 7919 + 17);
  STAGES[key].build(b);
  const pal = palette(COLOURS);
  return {
    pal,
    elements: b.parts.map((p, n) => {
      const row = pal.names.indexOf(p.mat);
      if (row < 0) throw new Error(key + ': no material ' + p.mat);
      const uv = [p.shade, row, p.shade + 1, row + 1];
      const origin = p.origin ? [p.origin[0] + dx, p.origin[1], p.origin[2]] : [dx, 0, 0];
      return {
        name: `${p.mat}_${n}`, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
        from: [p.from[0] + dx, p.from[1], p.from[2]], to: [p.to[0] + dx, p.to[1], p.to[2]], autouv: 0, color: 0,
        origin, rotation: p.rotation || [0, 0, 0],
        faces: Object.fromEntries(FACES.map(f => [f, { uv, texture: 0 }])), type: 'cube', uuid: id('crab/' + key + n + '/' + dx),
      };
    }),
  };
}

function project(name, keys, spacing = 0) {
  const all = [];
  let pal;
  keys.forEach((k, i) => { const e = elements(k, ((keys.length - 1) / 2 - i) * spacing); pal = e.pal; all.push(...e.elements); });
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name, model_identifier: '',
    visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements: all, outliner: all.map(e => e.uuid), textures: [textureOf('crab_art.png', pal.png, 0)],
  };
}

/** As chief-axes.cjs's render, on a dark ground. */
async function render(bb, project, views, file, size) {
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'p.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 60 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 300)); return Outliner.elements.length; })()`);
  if (loaded !== project.elements.length) throw new Error(`${project.name}: ${loaded} of ${project.elements.length} cubes loaded`);
  for (const [view, [pos, target]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      const p = window.__crabPreview || (window.__crabPreview = new Preview({ id: 'crab_render', offscreen: true, antialias: true }));
      p.resize(${size[0]}, ${size[1]});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 38 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#2a2a2c');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(file(view), Buffer.from(data.split(',')[1], 'base64'));
  }
}

const VIEWS = { front: [[-40, 32, -66], [0, 19, 0]], back: [[46, 36, 60], [0, 19, 0]] };
const LINEUP = { lineup: [[-36, 38, -160], [0, 19, 0]] };

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  // The first draft's stage names.
  for (const f of fs.readdirSync(OUT)) if (/^crab-art-pincers-/.test(f)) fs.unlinkSync(path.join(OUT, f));
  const bb = await connect(PORT);
  for (const key of Object.keys(STAGES)) {
    await render(bb, project('crab_art_' + key, [key]), VIEWS, v => path.join(OUT, `crab-art-${key}-${v}.png`), [480, 560]);
    console.log('rendered', key);
  }
  await render(bb, project('crab_art_lineup', Object.keys(STAGES), 38), LINEUP, v => path.join(OUT, `crab-art-${v}.png`), [1200, 560]);
  bb.close();
}

module.exports = { STAGES, COLOURS };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
