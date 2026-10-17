// Renders the mock-ups of the Bandit Warlord's sky arena (warlord-sky-arena.js) to <out>/<view>.png (isolated Blockbench).
//
//   node scripts/concepts/render-warlord-sky-arena.cjs [--out=docs/mobs/images/warlord-sky-arena] [--only=overview,plan] [--port=9232]
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const OUT = path.resolve(arg('out') || path.join(ROOT, 'docs', 'mobs', 'images', 'warlord-sky-arena'));
const PORT = Number(arg('port') || 9232);
const ALL = ['overview', 'rim', 'floor', 'below', 'plan', 'rising'];
const ONLY = arg('only') ? arg('only').split(',') : ALL;

/** Runs in the page: draws a numbered badge at each mark ([label, fx, fy], fractions of the picture, from ST.render). */
const annotate = async (png, marks) => {
  const img = new Image();
  await new Promise(res => { img.onload = res; img.src = png; });
  const c = document.createElement('canvas'); c.width = img.width; c.height = img.height;
  const g = c.getContext('2d'); g.drawImage(img, 0, 0);
  const sx = img.width / 1920;
  for (const [label, fx, fy] of marks) {
    const px = fx * img.width, py = fy * img.height, rad = 22 * sx;
    g.beginPath(); g.arc(px, py, rad, 0, Math.PI * 2);
    g.fillStyle = '#A8352A'; g.fill(); g.lineWidth = 4 * sx; g.strokeStyle = '#F2D9A0'; g.stroke();
    g.fillStyle = '#FFF6E0'; g.font = `bold ${Math.round((label.length > 1 ? 22 : 26) * sx)}px sans-serif`;
    g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(label, px, py + 1);
  }
  return c.toDataURL('image/png');
};

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html$/.test(t.url));
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
  const c = await connect(PORT);
  try {
    await c.evaluate(`(async () => { if (Project) await Project.close(true); newProject(Formats.free); return true; })()`);
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'st-runtime.js'), 'utf8'));
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'warlord-sky-arena.js'), 'utf8'));
    fs.mkdirSync(OUT, { recursive: true });
    for (const name of ONLY) {
      const s = await c.evaluate(`SKY.${name}()`);
      let png = await c.evaluate(`ST.render(${JSON.stringify(s.view)}, 1920, 1080, ${JSON.stringify(s.sky)}, ${s.fog[0]}, ${s.fog[1]}, ${JSON.stringify(s.marks || null)})`);
      if (s.marks) png = await c.evaluate(`(${annotate})(${JSON.stringify(png.data)}, ${JSON.stringify(png.marks)})`);
      const file = path.join(OUT, `${name}.png`);
      fs.writeFileSync(file, Buffer.from(png.split(',')[1], 'base64'));
      console.log(name, await c.evaluate('ST.count()'), 'voxels ->', file);
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
