// A short clip from frames filmed at 20 fps (00000.png, 00001.png...): the presentation video's runtime
// (video-runtime.js: original music, a caption per shot, fades) on a 16:9 MP4. Runs in the isolated Blockbench
// (Chromium) whose remote debugging port is given.
//
//   node scripts/showcase/make-clip.cjs <frames dir> <out.mp4> [--port=9232] [--shots=<json>]
//   --shots: [{"name":"duel","title":"...","subtitle":"...","start":0,"end":99}, ...] (frames, inclusive); one shot
//   over all the frames without it.
const fs = require('node:fs');
const path = require('node:path');

const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').slice(n.length + 3);
const [dir, out] = process.argv.slice(2).filter(a => !a.startsWith('--'));
const PORT = Number(arg('port') || 9232);
const W = 1280, H = 720, BITRATE = 8_000_000;

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails).slice(0, 2000))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: n, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}

(async () => {
  const frames = fs.readdirSync(dir).filter(f => /^\d{5}\.png$/.test(f)).length;
  if (!frames) throw new Error('no frames in ' + dir);
  const shots = arg('shots') ? JSON.parse(arg('shots')) : [{ name: 'clip', title: '', subtitle: '', start: 0, end: frames - 1 }];
  const seconds = frames / 20 + 0.6;
  const frameDir = 'file:///' + path.resolve(dir).split(path.sep).join('/') + '/';
  const c = await connect(PORT);
  try {
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'video-runtime.js'), 'utf8'));
    await c.evaluate(`(async () => { window.__music = await MV.compose(${seconds}); return true; })()`);
    const mime = await c.evaluate(`['video/mp4;codecs=avc1.640028,mp4a.40.2', 'video/mp4'].find(t => MediaRecorder.isTypeSupported(t)) || null`);
    if (!mime) throw new Error('this Chromium cannot record MP4');
    console.log('recording', frames, 'frames,', seconds.toFixed(1), 's, as', mime);
    const size = await c.evaluate(`MV.record({ frameCount: ${frames}, frameUrl: i => ${JSON.stringify(frameDir)} + String(i).padStart(5, '0') + '.png',
      shots: ${JSON.stringify(shots)}, W: ${W}, H: ${H}, bitrate: ${BITRATE}, music: window.__music, mime: ${JSON.stringify(mime)} })`);
    const parts = [];
    for (let i = 0; i * 2 * 1024 * 1024 < size; i++) parts.push(Buffer.from((await c.evaluate(`MV.slice(${i})`)).split(',')[1], 'base64'));
    fs.mkdirSync(path.dirname(path.resolve(out)), { recursive: true });
    fs.writeFileSync(out, Buffer.concat(parts));
    console.log('wrote', out, (fs.statSync(out).size / 1e6).toFixed(1), 'MB');
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
