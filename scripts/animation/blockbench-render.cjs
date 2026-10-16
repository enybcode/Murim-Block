// Films the animations of a Blockbench project, in Blockbench itself (what you see there is what is filmed): every
// animation, frame by frame, from the side and from the front three-quarter, to PNG frames (then GIFs with FramesToGif).
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/animation/blockbench-render.cjs <project.bbmodel> <out dir> [--fps=20] [--only=a,b] [--port=9232]
const fs = require('node:fs');
const path = require('node:path');

const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);
const FPS = Number(arg('fps') || 20);
const [file, out] = process.argv.slice(2).filter(a => !a.startsWith('--'));

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
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

/** The camera views, in pixels around the character (it faces north, -z): side (its right) and front three-quarter. */
const VIEWS = {
  side: { position: [70, 22, -6], target: [0, 16, -6] },
  front: { position: [-42, 30, -58], target: [0, 15, -4] },
  // From above (the blade's flat shows wide when it lies level).
  top: { position: [0.5, 95, -12], target: [0, 15, -10] },
  // Three-quarter from behind (the back is south, +z).
  back: { position: [42, 30, 58], target: [0, 15, -4] },
  // Straight from behind (what a hand behind the back looks like).
  rear: { position: [0, 24, 72], target: [0, 15, 0] },
  // Wide, for a dash (the character runs 5 blocks, 80 pixels, toward -z): from its right, and three-quarter ahead.
  wide: { position: [140, 34, -40], target: [0, 12, -40] },
  wide_front: { position: [115, 52, -150], target: [0, 12, -46] },
  // Farther back and higher, for a tall model (a boss with something over its head): its three-quarter front, its side,
  // its three-quarter back.
  tall_front: { position: [-38, 34, -54], target: [0, 19, -2] },
  tall_side: { position: [68, 24, -2], target: [0, 19, -2] },
  tall_back: { position: [38, 34, 54], target: [0, 19, 2] },
  // Close on the chest and the head, for checking textures.
  close_front: { position: [-14, 26, -30], target: [0, 22, -2] },
  close_back: { position: [14, 26, 30], target: [0, 22, 2] },
};

(async () => {
  const c = await connect(PORT);
  try {
    const model = fs.readFileSync(file, 'utf8');
    const names = await c.evaluate(`(async () => {
      if (typeof Project !== 'undefined' && Project) await Project.close(true);
      setupProject(Formats.free);
      Codecs.project.parse(JSON.parse(${JSON.stringify(model)}), 'project.bbmodel');
      Canvas.updateAll();
      return Animation.all.map(a => a.name);
    })()`);
    const only = arg('only') ? arg('only').split(',') : names;
    for (const name of names.filter(n => only.includes(n))) {
      const length = await c.evaluate(`(() => { const a = Animation.all.find(a => a.name === ${JSON.stringify(name)}); a.select(); return a.length; })()`);
      const frames = Math.round(length * FPS) + 1;
      for (const [view, spec] of Object.entries(VIEWS).filter(([v]) => (arg('views') || 'side,front').split(',').includes(v))) {
        const dir = path.join(out, name, view);
        fs.mkdirSync(dir, { recursive: true });
        for (let f = 0; f < frames; f++) {
          const png = await c.evaluate(`(async () => {
            Timeline.setTime(${f / FPS});
            Animator.preview();
            const p = window.__azurePreview || (window.__azurePreview = new Preview({ id: 'azure_render', offscreen: true, antialias: true }));
            p.resize(480, 480);
            p.loadAnglePreset({ position: ${JSON.stringify(spec.position)}, target: ${JSON.stringify(spec.target)}, projection: 'perspective', fov: 40 });
            p.camera.lookAt(...${JSON.stringify(spec.target)});
            const bg = Canvas.scene.background;
            Canvas.scene.background = new THREE.Color('#C9D6DE');
            const grid = Canvas.scene.children.filter(o => o.type === 'Group' && o.visible && /grid/i.test(o.name || ''));
            return await new Promise(res => p.screenshot({ crop: false }, data => { Canvas.scene.background = bg; res(data); }));
          })()`);
          fs.writeFileSync(path.join(dir, `frame-${String(f).padStart(3, '0')}.png`), Buffer.from(png.split(',')[1], 'base64'));
        }
        console.log(name, view, frames, 'frames ->', dir);
      }
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
