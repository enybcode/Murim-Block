// Renders the voxel concept scenes of the future Murim structures to docs/biomes/images/structures-<scene>-<view>.png.
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/concepts/render-concepts.cjs [--only=karst,peaks] [--port=9232]
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'biomes', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);
const ALL = ['karst', 'peaks', 'loess', 'bamboo', 'jiangnan', 'peach', 'desert'];
const ONLY = arg('only') ? arg('only').split(',') : ALL;
const SKY = { karst: '#B9CDD2', peaks: '#C9D3DA', loess: '#CFC6AE', bamboo: '#AFC0BA', jiangnan: '#C2D2DA', peach: '#D6DCE8', desert: '#D9CFB0' };

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /app\.asar\/index\.html$/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: n, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}

(async () => {
  const c = await connect(PORT);
  try {
    await c.evaluate(`(async () => { if (Project) await Project.close(true); newProject(Formats.free); return true; })()`);
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'st-runtime.js'), 'utf8'));
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'scenes.js'), 'utf8'));
    fs.mkdirSync(OUT, { recursive: true });
    for (const scene of ONLY) {
      const views = await c.evaluate(`SCENES.${scene}()`);
      const voxels = await c.evaluate('ST.count()');
      for (const view of views) {
        const png = await c.evaluate(`ST.render(${JSON.stringify(view)}, 1280, 720, ${JSON.stringify(SKY[scene])})`);
        const file = path.join(OUT, `structures-${scene}-${view.name}.png`);
        fs.writeFileSync(file, Buffer.from(png.split(',')[1], 'base64'));
        console.log(scene, view.name, voxels, 'voxels ->', path.relative(ROOT, file));
      }
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
