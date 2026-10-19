// The two forms of the kagune in one model (Enzo, 18/10/2026: "merge the two kagune", one model with both forms): the
// four red tentacles at 0 Fury and the two centipedes with the half-mask at 100, from the two mock-ups already checked
// against the ground and the body (kagune-preview.cjs, then --form=kakuja). Three animations:
//   idle_hunger  0 Fury: the tentacles' idle, the centipedes and the mask hidden;
//   transform    0 to 100: he hunches, the tentacles thrash and draw back into his back, the centipedes burst out of it
//                segment by segment, he throws his head back and the mask grows over his face;
//   idle_demon   100 Fury: the centipedes' idle, the tentacles hidden.
//
//   node scripts/concepts/kagune-forms.cjs [--port=9236] [--no-film]
// Writes art/blockbench/kagune_forms.bbmodel (open it in Animate) and docs/combat/styles/images/kagune-forms-*.gif.
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const kit = require('../animation/mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const ART = path.join(ROOT, 'art', 'blockbench');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9236);
const id = s => kit.stableId('kagune-forms/' + s);

const kagune = JSON.parse(fs.readFileSync(path.join(ART, 'kagune_preview.bbmodel'), 'utf8'));
const kakuja = JSON.parse(fs.readFileSync(path.join(ART, 'kakuja_preview.bbmodel'), 'utf8'));

// ---------------------------------------------------------------- one outliner
// Both have the same player (same cubes, same ids) and the same texture: keep the kagune's player, add the kakuja's
// centipedes beside its tentacles, and its mask on the head in a group of its own (so it can be shown and hidden).
const find = (node, name) => (typeof node === 'object' && node.name === name ? node : typeof node === 'object' ? (node.children || []).map(c => find(c, name)).find(Boolean) : null);
const player = JSON.parse(JSON.stringify(kagune.outliner[0]));
const tentacles = find(player, 'kagune');
const centipedes = JSON.parse(JSON.stringify(find(kakuja.outliner[0], 'kakuja')));
player.children.push(centipedes);
const kakujaHead = find(kakuja.outliner[0], 'head'), head = find(player, 'head');
const byName = Object.fromEntries(kakuja.elements.map(e => [e.uuid, e.name]));
const maskCubes = kakujaHead.children.filter(c => typeof c === 'string' && /^mask/.test(byName[c]));
const mask = { name: 'mask_group', origin: [0, 24, 0], rotation: [0, 0, 0], color: 0, uuid: id('g/mask'), export: true, mirror_uv: false,
  isOpen: true, locked: false, visibility: true, autouv: 0, children: maskCubes };
head.children.push(mask);
const seen = new Set();
const elements = [...kagune.elements, ...kakuja.elements].filter(e => !seen.has(e.uuid) && seen.add(e.uuid));

// ---------------------------------------------------------------- animations
const kf = (channel, t, [x, y, z], interpolation = 'catmullrom') => ({ channel, data_points: [{ x, y, z }],
  uuid: id(`kf/${channel}/${t}/${x}/${y}/${z}/${Math.random()}`), time: +t.toFixed(3), color: -1, interpolation });
const HIDDEN = 0.01;
const hide = (length, g) => ({ name: g.name, type: 'bone', keyframes: [kf('scale', 0, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', length, [HIDDEN, HIDDEN, HIDDEN], 'linear')] });
const anim = (name, length, loop, animators) => ({ uuid: id('anim/' + name), name, loop, override: false, length, snapping: 20, selected: false,
  anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });

const hungerIdle = kagune.animations[0], demonIdle = kakuja.animations[0];
const idleHunger = anim('idle_hunger', hungerIdle.length, 'loop', { ...hungerIdle.animators,
  [centipedes.uuid]: hide(hungerIdle.length, centipedes), [mask.uuid]: hide(hungerIdle.length, mask) });
const idleDemon = anim('idle_demon', demonIdle.length, 'loop', { ...demonIdle.animators,
  [tentacles.uuid]: hide(demonIdle.length, tentacles) });

// The transformation, 2.6 s.
const T = 2.6, transform = {};
const add = (g, keys) => { transform[g.uuid] = { name: g.name, type: 'bone', keyframes: keys }; };
const bodyG = find(player, 'body'), armR = find(player, 'arm_right'), armL = find(player, 'arm_left');
// He hunches and clutches his head, then throws it back as the centipedes burst out, then stands.
add(bodyG, [kf('rotation', 0, [0, 0, 0]), kf('rotation', 0.5, [18, 0, 0]), kf('rotation', 1.0, [22, 0, 0]), kf('rotation', 1.5, [-14, 0, 0]), kf('rotation', 2.1, [-6, 0, 0]), kf('rotation', T, [0, 0, 0])]);
add(head, [kf('rotation', 0, [0, 0, 0]), kf('rotation', 0.5, [24, 0, 0]), kf('rotation', 1.0, [28, 0, 0]), kf('rotation', 1.5, [-30, 0, 0]), kf('rotation', 2.1, [-10, 0, 0]), kf('rotation', T, [0, 0, 0])]);
add(armR, [kf('rotation', 0, [0, 0, 5]), kf('rotation', 0.5, [-150, 0, 30]), kf('rotation', 1.0, [-155, 0, 32]), kf('rotation', 1.5, [-20, 0, 40]), kf('rotation', 2.1, [-10, 0, 20]), kf('rotation', T, [0, 0, 5])]);
add(armL, [kf('rotation', 0, [0, 0, -5]), kf('rotation', 0.5, [-150, 0, -30]), kf('rotation', 1.0, [-155, 0, -32]), kf('rotation', 1.5, [-20, 0, -40]), kf('rotation', 2.1, [-10, 0, -20]), kf('rotation', T, [0, 0, -5])]);
// The tentacles thrash, then draw back into his back.
add(tentacles, [kf('scale', 0, [1, 1, 1]), kf('scale', 0.45, [1.15, 1.15, 1.15]), kf('scale', 0.9, [HIDDEN, HIDDEN, HIDDEN]), kf('scale', T, [HIDDEN, HIDDEN, HIDDEN], 'linear')]);
for (const g of tentacles.children.filter(c => typeof c === 'object')) {
  const shake = [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8].map((t, i) => kf('rotation', t, [g.rotation[0] + (i % 2 ? 14 : -14), g.rotation[1] + (i % 2 ? -10 : 10), g.rotation[2]]));
  add(g, shake);
}
// The centipedes burst out of his back segment by segment, from the root out, the second a beat after the first.
add(centipedes, [kf('scale', 0, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', 0.85, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', 0.9, [1, 1, 1], 'linear'), kf('scale', T, [1, 1, 1], 'linear')]);
centipedes.children.filter(c => typeof c === 'object' && /^centipede_/.test(c.name)).forEach((root, i) => {
  const name = root.name.replace(/_0$/, '');
  for (let k = 0, g = root; g; k++) {
    const at = 0.9 + i * 0.12 + k * 0.032;
    add(g, [kf('scale', 0, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', at, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', at + 0.06, [1.25, 1.25, 1.25], 'linear'),
      kf('scale', at + 0.14, [1, 1, 1], 'linear'), kf('scale', T, [1, 1, 1], 'linear')]);
    g = (g.children || []).find(c => typeof c === 'object' && c.name === `${name}_${k + 1}`);
  }
});
// The mask grows over his face as he throws his head back.
add(mask, [kf('scale', 0, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', 1.35, [HIDDEN, HIDDEN, HIDDEN], 'linear'), kf('scale', 1.6, [1.1, 1.1, 1.1], 'linear'), kf('scale', 1.75, [1, 1, 1], 'linear'), kf('scale', T, [1, 1, 1], 'linear')]);

// The kakuja's texture: the same atlas, drawn after the kakuja's purple, red and mask were added to it.
const proj = { ...kagune, name: 'kagune_forms', elements, outliner: [player], textures: kakuja.textures,
  animations: [idleHunger, anim('transform', T, 'once', transform), idleDemon] };
fs.writeFileSync(path.join(ART, 'kagune_forms.bbmodel'), JSON.stringify(proj, null, 1));
console.log(elements.length, 'cubes,', proj.animations.map(a => a.name).join(', '), '->', path.relative(ROOT, path.join(ART, 'kagune_forms.bbmodel')));
if (process.argv.includes('--no-film')) process.exit(0);

// ---------------------------------------------------------------- filming (isolated Blockbench)
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
const shot = (bb, pos, target, t) => bb.evaluate(`(async () => {
  Timeline.setTime(${t}); Animator.preview();
  const p = window.__formsFilm || (window.__formsFilm = new Preview({ id: 'forms_film', offscreen: true, antialias: true }));
  p.resize(640, 640);
  p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 36 });
  p.camera.lookAt(...${JSON.stringify(target)});
  const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#E9E2D2');
  return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
})()`);

/** Loads the project with only one animation in Blockbench (the three at once crash it), retrying on a reload. */
async function loadWith(animation) {
  const text = JSON.stringify({ ...proj, animations: [animation] });
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      for (let i = 0; i < 40; i++) { try { await fetch('http://127.0.0.1:' + PORT + '/json/list'); break; } catch (e) { await new Promise(r => setTimeout(r, 1000)); } }
      const bb = await connect(PORT);
      await bb.evaluate('window.__formsText = ""; true');
      for (let i = 0; i < text.length; i += 400000) await bb.evaluate('window.__formsText += ' + JSON.stringify(text.slice(i, i + 400000)) + '; true');
      try { await bb.evaluate('(async () => { for (const p of ModelProject.all.slice()) await p.close(true); return true; })()'); } catch (e) { bb.close(); await new Promise(r => setTimeout(r, 2000)); continue; }
      await bb.evaluate("(() => { setupProject(Formats.free); Codecs.project.parse(JSON.parse(window.__formsText), 'p.bbmodel'); Canvas.updateAll(); return true; })()");
      await bb.evaluate('(async () => { await new Promise(r => setTimeout(r, 800)); Modes.options.animate.select(); Animation.all[0].select(); return Outliner.elements.length; })()');
      return bb;
    } catch (e) { console.log('retry', animation.name, String(e).slice(0, 80)); await new Promise(r => setTimeout(r, 3000)); }
  }
  throw new Error('could not load ' + animation.name);
}

(async () => {
  const out = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
  const FPS = 20, view = [[-70, 70, -230], [0, 30, 36]];
  // One GIF that runs the whole story: the idle at 0 Fury, the transformation, the idle at 100 Fury.
  const dir = path.join(ROOT, 'build', 'kagune-forms');
  fs.rmSync(dir, { recursive: true, force: true }); fs.mkdirSync(dir, { recursive: true });
  let frame = 0;
  for (const a of proj.animations) {
    const bb = await loadWith(a);
    const frames = Math.round(a.length * FPS) + (a.name === 'transform' ? 1 : 0);
    for (let f = 0; f < frames; f++) {
      const data = await shot(bb, view[0], view[1], Math.min(f / FPS, a.length));
      fs.writeFileSync(path.join(dir, `frame-${String(frame++).padStart(3, '0')}.png`), Buffer.from(data.split(',')[1], 'base64'));
    }
    bb.close();
    console.log(a.name, frames, 'frames');
  }
  execFileSync('java', [path.join(ROOT, 'scripts', 'animation', 'FramesToGif.java'), dir, path.join(out, 'kagune-forms.gif'), String(1000 / FPS), '640'], { stdio: 'ignore' });
  console.log(frame, 'frames ->', path.relative(ROOT, path.join(out, 'kagune-forms.gif')));
})().catch(e => { console.error(e); process.exit(1); });
