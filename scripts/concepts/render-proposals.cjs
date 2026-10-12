// Renders the four structure proposals to docs/structures/images/<name>.png (isolated Blockbench, port 9232).
//
//   node scripts/concepts/render-proposals.cjs [--only=sanctuary,village]
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'structures', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const ALL = ['sanctuary', 'village', 'lair', 'fortress'];
const ONLY = arg('only') ? arg('only').split(',') : ALL;
const SKY = { sanctuary: '#C9D3DA', village: '#B9CDD2', lair: '#CFC6AE', fortress: '#D9CFB0' };

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /app\.asar\/index\.html$/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails).slice(0, 1500))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: n, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}

(async () => {
  const c = await connect(9232);
  try {
    await c.evaluate(`(async () => { if (Project) await Project.close(true); newProject(Formats.free); return true; })()`);
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'st-runtime.js'), 'utf8'));
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'proposals.js'), 'utf8'));
    fs.mkdirSync(OUT, { recursive: true });
    for (const name of ONLY) {
      const view = await c.evaluate(`PROPOSALS.${name}()`);
      const png = await c.evaluate(`ST.render(${JSON.stringify(view)}, 1600, 900, ${JSON.stringify(SKY[name])})`);
      const file = path.join(OUT, `${name}.png`);
      fs.writeFileSync(file, Buffer.from(png.split(',')[1], 'base64'));
      console.log(name, await c.evaluate('ST.count()'), 'voxels ->', path.relative(ROOT, file));
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
