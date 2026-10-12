// Edits the presentation video from the frames filmed by `gradlew -PgameTests runShowcase` (build/showcase-run):
// original synthesised music, title cards and captions, vertical 1080x1920 MP4 plus a light 720x1280 copy for
// Discord, and the music alone as WAV. Runs in the isolated Blockbench (Chromium) on port 9232.
//
//   node scripts/showcase/make-video.cjs [--out=build/showcase-run/video]
const fs = require('node:fs');
const path = require('node:path');

const ROOT = path.resolve(__dirname, '..', '..');
const RUN = path.join(ROOT, 'build', 'showcase-run');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const OUT = path.resolve(ROOT, arg('out') || path.join('build', 'showcase-run', 'video'));

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /app\.asar\/index\.html$/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails).slice(0, 2000))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: n, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve, reject });
    ws.send(JSON.stringify({ id: n, method, params }));
  });
  return { evaluate, send, close: () => ws.close() };
}
/** Video samples in a fragmented MP4 from MediaRecorder: sums the trun counts of the 'vide' track. */
function videoSamples(b) {
  const counts = {}, kinds = {}; let track = 0;
  (function walk(start, end) {
    for (let p = start; p + 8 <= end;) {
      let size = b.readUInt32BE(p), hdr = 8; const type = b.toString('latin1', p + 4, p + 8);
      if (size === 1) { size = Number(b.readBigUInt64BE(p + 8)); hdr = 16; }
      if (size === 0) size = end - p;
      if (['moov', 'trak', 'mdia', 'minf', 'stbl', 'moof', 'traf'].includes(type)) walk(p + hdr, p + size);
      if (type === 'tkhd') track = b.readUInt32BE(p + hdr + (b[p + hdr] === 1 ? 20 : 12));
      if (type === 'tfhd') track = b.readUInt32BE(p + hdr + 4);
      if (type === 'hdlr') kinds[track] = b.toString('latin1', p + hdr + 8, p + hdr + 12);
      if (type === 'trun') counts[track] = (counts[track] || 0) + b.readUInt32BE(p + hdr + 4);
      p += size;
    }
  })(0, b.length);
  const video = Object.keys(kinds).find(k => kinds[k] === 'vide');
  return counts[video] || 0;
}

const save = (name, dataUrl) => {
  fs.mkdirSync(OUT, { recursive: true });
  const file = path.join(OUT, name);
  fs.writeFileSync(file, Buffer.from(dataUrl.split(',')[1], 'base64'));
  console.log(path.relative(ROOT, file), (fs.statSync(file).size / 1e6).toFixed(1), 'MB');
};

(async () => {
  const shots = JSON.parse(fs.readFileSync(path.join(RUN, 'shots.json'), 'utf8'));
  const frames = fs.readdirSync(path.join(RUN, 'frames')).filter(f => f.endsWith('.png')).length;
  const seconds = frames / 20 + 0.6;
  const frameDir = 'file:///' + path.join(RUN, 'frames').split(path.sep).join('/') + '/';
  const c = await connect(9232);
  try {
    // The Blockbench window is usually in the background: make the page behave as focused and active.
    await c.send('Emulation.setFocusEmulationEnabled', { enabled: true }).catch(() => {});
    await c.send('Page.setWebLifecycleState', { state: 'active' }).catch(() => {});
    await c.evaluate(fs.readFileSync(path.join(__dirname, 'video-runtime.js'), 'utf8'));
    await c.evaluate(`(async () => { window.__music = await MV.compose(${seconds}); return true; })()`);
    const fetchStash = async (name, size) => {
      const parts = [];
      for (let i = 0; i * 2 * 1024 * 1024 < size; i++) parts.push(Buffer.from((await c.evaluate(`MV.slice(${i})`)).split(',')[1], 'base64'));
      fs.mkdirSync(OUT, { recursive: true });
      const file = path.join(OUT, name);
      fs.writeFileSync(file, Buffer.concat(parts));
      console.log(path.relative(ROOT, file), (size / 1e6).toFixed(1), 'MB');
    };
    await fetchStash('murimblock-theme.wav', await c.evaluate('MV.stash(MV.wav(window.__music))'));
    const mime = await c.evaluate(`['video/mp4;codecs=avc1.640028,mp4a.40.2', 'video/mp4', 'video/webm;codecs=vp9,opus'].find(t => MediaRecorder.isTypeSupported(t))`);
    const ext = mime.startsWith('video/mp4') ? 'mp4' : 'webm';
    console.log('recording as', mime, frames, 'frames,', seconds.toFixed(1), 's');
    for (const [name, W, H, bitrate] of [['murimblock-presentation', 1080, 1920, 9000000], ['murimblock-presentation-discord', 720, 1280, 1050000]]) {
      const size = await c.evaluate(`MV.record({ frameCount: ${frames}, frameUrl: i => ${JSON.stringify(frameDir)} + String(i).padStart(5, '0') + '.png',
        shots: ${JSON.stringify(shots)}, W: ${W}, H: ${H}, bitrate: ${bitrate}, music: window.__music, mime: ${JSON.stringify(mime)} })`);
      await fetchStash(`${name}.${ext}`, size);
      if (ext === 'mp4') {
        const n = videoSamples(fs.readFileSync(path.join(OUT, `${name}.${ext}`)));
        console.log(`  ${n} video frames for ${frames} filmed`);
        if (n < frames * 0.9) throw new Error(`${name}: only ${n} of ${frames} frames were encoded`);
      }
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
