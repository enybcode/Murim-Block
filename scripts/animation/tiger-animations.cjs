// The great tiger's animations (art/blockbench/tiger.bbmodel, its bones from tiger-model.cjs), written into the project
// and filmed in Blockbench itself to GIFs (docs/mobs/images/tiger-<name>.gif):
//   idle   (4 s, loop): it breathes, the head sweeps slowly round, the tail ripples, a snarl twitches the jaw.
//   walk   (1.4 s, loop): a heavy prowl, diagonal legs together, the elbows and knees folding as each paw swings
//          forward, the shoulders rolling, the head steady and low, the tail swaying.
//   attack (1.8 s): it sinks on its haunches, rears up roaring, the right paw raised high, and smashes it down and
//          across; then settles back.
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
  attack: { length: 1.8, loop: 'once', pose(t) {
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
const FILMS = { idle: ['three'], walk: ['three', 'side'], attack: ['three', 'side'] };

async function film() {
  const bb = await connect(PORT);
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'tiger.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 60 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 400)); return Outliner.elements.length + ' cubes, ' + Animation.all.length + ' animations'; })()`);
  console.log('loaded', loaded);
  const out = path.join(ROOT, 'build', 'tiger-animations');
  for (const [name, views] of Object.entries(FILMS)) for (const view of views) {
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
