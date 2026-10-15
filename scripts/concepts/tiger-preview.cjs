// Preview pictures of the tiger (art/blockbench/tiger.bbmodel, from scripts/animation/tiger-model.cjs), rendered in
// Blockbench itself, to docs/mobs/images/tiger-<view>.png: front three-quarter, side, front, back three-quarter, the face.
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/concepts/tiger-preview.cjs [--port=9232]
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'mobs', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let n = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const k = ++n; pending.set(k, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: k, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}

async function render(bb, project, views, file, size = [480, 560]) {
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'p.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 40 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 300)); return Outliner.elements.length; })()`);
  if (loaded !== project.elements.length) throw new Error(`${project.name}: ${loaded} of ${project.elements.length} cubes loaded`);
  for (const [view, [pos, target, s]] of Object.entries(views)) {
    const [w, h] = s || size;
    const data = await bb.evaluate(`(async () => {
      const p = window.__tigerPreview || (window.__tigerPreview = new Preview({ id: 'tiger_render', offscreen: true, antialias: true }));
      p.resize(${w}, ${h});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 38 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#EDE4CF');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(file(view), Buffer.from(data.split(',')[1], 'base64'));
  }
}

/** The tiger: about 100 pixels from nose to tail tip, 54 tall, facing north (-z). */
const VIEWS = {
  three: [[-125, 82, -150], [0, 33, -6], [1100, 720]], side: [[-200, 38, -6], [0, 33, -6], [1100, 620]],
  front: [[0, 38, -170], [0, 30, 0], [720, 720]], back: [[110, 76, 150], [0, 30, 4], [1100, 720]],
  face: [[-32, 46, -112], [0, 34, -50], [720, 620]],
};

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const bb = await connect(PORT);
  const tiger = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'tiger.bbmodel'), 'utf8'));
  await render(bb, tiger, VIEWS, v => path.join(OUT, `tiger-${v}.png`));
  bb.close();
  console.log('rendered the tiger to', path.relative(ROOT, OUT));
}

main().catch(e => { console.error(e); process.exit(1); });
