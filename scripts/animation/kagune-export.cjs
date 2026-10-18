// Exports the kagune's two forms (art/blockbench/kagune_forms.bbmodel, scripts/concepts/kagune-forms.cjs) for the game:
// the bones, the cubes and the three animations sampled in Blockbench itself (each bone's local position, rotation and
// scale, 20 times a second), so the game plays exactly what the mock-up shows. Read by client/KaguneModel and drawn on
// the player by client/KaguneOverlay (docs/combat/styles/devouring-demon-art.md).
//
//   node scripts/animation/kagune-export.cjs [--port=9236]
// Writes assets/murimblock/kagune/kagune_forms.json and textures/entity/kagune_forms.png.
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9236);
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const proj = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'kagune_forms.bbmodel'), 'utf8'));
const FPS = 20;

async function connect() {
  const targets = await fetch(`http://127.0.0.1:${PORT}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let n = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.error || (m.result && m.result.exceptionDetails)) p.reject(new Error(JSON.stringify(m.error || m.result.exceptionDetails).slice(0, 600))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const k = ++n; pending.set(k, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: k, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}
/** The project with one animation, loaded in pieces (all three at once crash Blockbench), retried on a reload. */
async function loadWith(animation) {
  const text = JSON.stringify({ ...proj, animations: animation ? [animation] : [] });
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      const bb = await connect();
      await bb.evaluate('window.__k = ""; true');
      for (let i = 0; i < text.length; i += 400000) await bb.evaluate('window.__k += ' + JSON.stringify(text.slice(i, i + 400000)) + '; true');
      try { await bb.evaluate('(async () => { for (const p of ModelProject.all.slice()) await p.close(true); return true; })()'); } catch (e) { bb.close(); await new Promise(r => setTimeout(r, 2000)); continue; }
      await bb.evaluate("(() => { setupProject(Formats.free); Codecs.project.parse(JSON.parse(window.__k), 'p.bbmodel'); Canvas.updateAll(); return true; })()");
      await bb.evaluate('(async () => { await new Promise(r => setTimeout(r, 800)); Modes.options.animate.select(); if (Animation.all[0]) Animation.all[0].select(); return true; })()');
      return bb;
    } catch (e) { console.log('retry', String(e).slice(0, 80)); await new Promise(r => setTimeout(r, 3000)); }
  }
  throw new Error('could not load');
}
const round = (v, d = 4) => +v.toFixed(d);

(async () => {
  // The bones and cubes, at rest.
  let bb = await loadWith(null);
  const model = JSON.parse(await bb.evaluate(`(() => {
    const groups = Group.all.slice();
    const index = new Map(groups.map((g, i) => [g, i]));
    const r = v => +v.toFixed(4);
    const bones = groups.map(g => ({ name: g.name, parent: g.parent && g.parent !== 'root' ? index.get(g.parent) : -1,
      pos: g.mesh.position.toArray().map(r), rot: g.mesh.quaternion.toArray().map(r), scale: g.mesh.scale.toArray().map(r) }));
    const cubes = Outliner.elements.filter(e => e instanceof Cube).map(e => {
      const faces = {};
      for (const [f, face] of Object.entries(e.faces)) if (face.texture !== null && face.texture !== false) faces[f] = face.uv.map(r);
      return { name: e.name, bone: e.parent && e.parent !== 'root' ? index.get(e.parent) : -1,
        pos: e.mesh.position.toArray().map(r), rot: e.mesh.quaternion.toArray().map(r),
        from: e.from.map((v, k) => r(v - e.origin[k])), to: e.to.map((v, k) => r(v - e.origin[k])), faces };
    });
    return JSON.stringify({ bones, cubes });
  })()`));
  bb.close();
  console.log(model.bones.length, 'bones,', model.cubes.length, 'cubes');

  // Each animation: every bone's local transform, sampled; bones that never move from rest are left out.
  model.animations = {};
  for (const a of proj.animations) {
    bb = await loadWith(a);
    const frames = Math.round(a.length * FPS) + 1;
    const samples = [];
    for (let f = 0; f < frames; f++) {
      samples.push(JSON.parse(await bb.evaluate(`(() => { Timeline.setTime(${Math.min(f / FPS, a.length)}); Animator.preview();
        return JSON.stringify(Group.all.map(g => [...g.mesh.position.toArray(), ...g.mesh.quaternion.toArray(), ...g.mesh.scale.toArray()].map(v => +v.toFixed(4)))); })()`)));
    }
    bb.close();
    const tracks = {};
    model.bones.forEach((b, i) => {
      const rest = [...b.pos, ...b.rot, ...b.scale];
      if (samples.every(s => s[i].every((v, k) => Math.abs(v - rest[k]) < 1e-3))) return;
      tracks[i] = samples.map(s => s[i]);
    });
    model.animations[a.name] = { length: a.length, loop: a.loop === 'loop', fps: FPS, tracks };
    console.log(a.name, frames, 'frames,', Object.keys(tracks).length, 'moving bones');
  }
  fs.mkdirSync(path.join(ASSETS, 'kagune'), { recursive: true });
  fs.writeFileSync(path.join(ASSETS, 'kagune', 'kagune_forms.json'), JSON.stringify(model));
  // The atlas.
  const png = proj.textures[0].source.split(',')[1];
  fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'kagune_forms.png'), Buffer.from(png, 'base64'));
  console.log('->', path.relative(ROOT, path.join(ASSETS, 'kagune', 'kagune_forms.json')), (fs.statSync(path.join(ASSETS, 'kagune', 'kagune_forms.json')).size / 1e6).toFixed(1), 'MB');
})().catch(e => { console.error(e); process.exit(1); });
