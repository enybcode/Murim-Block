// Renders the real Bandit Warlord sky arena from its blocks (art/structures/warlord-sky-arena/warlord_sky_arena.schem),
// not from the mock-up: reads the schematic, keeps the blocks open to the air and draws them with st-runtime.js over a
// sea of clouds (isolated Blockbench).
//
//   node scripts/concepts/render-sky-arena-schem.cjs [--out=art/structures/warlord-sky-arena] [--port=9232]
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');

const ROOT = path.resolve(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const DIR = path.join(ROOT, 'art', 'structures', 'warlord-sky-arena');
const OUT = path.resolve(arg('out') || DIR);
const PORT = Number(arg('port') || 9232);

// ---- the schematic (Sponge v3)
const buf = zlib.gunzipSync(fs.readFileSync(path.join(DIR, 'warlord_sky_arena.schem')));
let p = 0;
const u8 = () => buf[p++], i16 = () => { const v = buf.readInt16BE(p); p += 2; return v; }, i32 = () => { const v = buf.readInt32BE(p); p += 4; return v; };
const str = () => { const n = buf.readUInt16BE(p); p += 2; const s = buf.toString('utf8', p, p + n); p += n; return s; };
function payload(t) {
  switch (t) {
    case 1: return buf.readInt8(p++); case 2: return i16(); case 3: return i32(); case 4: { p += 8; return 0; } case 5: { p += 4; return 0; } case 6: { p += 8; return 0; }
    case 7: { const n = i32(); const v = buf.subarray(p, p + n); p += n; return v; } case 8: return str();
    case 9: { const et = u8(), n = i32(); const a = []; for (let i = 0; i < n; i++) a.push(payload(et)); return a; }
    case 10: { const o = {}; for (;;) { const tt = u8(); if (tt === 0) return o; const name = str(); o[name] = payload(tt); } }
    case 11: { const n = i32(); const a = []; for (let i = 0; i < n; i++) a.push(i32()); return a; }
    default: throw new Error('tag ' + t);
  }
}
const rt = u8(); str();
const S = payload(rt).Schematic;
const W = S.Width, H = S.Height, L = S.Length, [ox, oy, oz] = S.Offset;
const names = []; for (const [name, i] of Object.entries(S.Blocks.Palette)) names[i] = name;
const cells = new Uint16Array(W * H * L);
{ const d = S.Blocks.Data; let i = 0, k = 0; while (i < d.length) { let v = 0, sh = 0, b; do { b = d[i++]; v |= (b & 0x7f) << sh; sh += 7; } while (b & 0x80); cells[k++] = v; } }
const at = (x, y, z) => (x < 0 || y < 0 || z < 0 || x >= W || y >= H || z >= L ? 0 : cells[x + z * W + y * W * L]);

// ---- its blocks as st-runtime materials
const MAT = {
  'minecraft:basalt[axis=y]': 'basalt', 'minecraft:blackstone': 'basalt_dark', 'minecraft:smooth_basalt': 'basalt', 'minecraft:tuff': 'scorched',
  'minecraft:obsidian': 'obsidian', 'minecraft:crying_obsidian': 'obsidian', 'minecraft:magma_block': 'magma', 'minecraft:dark_oak_log[axis=y]': 'wood_dark',
  'minecraft:gold_block': 'gold', 'minecraft:purple_wool': 'banner_purple', 'minecraft:polished_blackstone': 'iron', 'minecraft:waxed_cut_copper': 'bronze',
  'minecraft:netherrack': 'fire', 'minecraft:fire': 'fire',
};
const mat = name => MAT[name] || MAT[name.replace(/\[.*$/, '')] || 'stone';
const air = names.indexOf('minecraft:air');
const byMat = {};
for (let y = 0; y < H; y++) for (let z = 0; z < L; z++) for (let x = 0; x < W; x++) {
  const v = cells[x + z * W + y * W * L];
  if (v === air) continue;
  const open = at(x + 1, y, z) === air || at(x - 1, y, z) === air || at(x, y + 1, z) === air || at(x, y - 1, z) === air || at(x, y, z + 1) === air || at(x, y, z - 1) === air
    || x === 0 || y === 0 || z === 0 || x === W - 1 || y === H - 1 || z === L - 1;
  if (!open) continue;
  // World position: the paste point (the fighting floor's middle, standing on it) at (0, 1, 0).
  (byMat[mat(names[v])] = byMat[mat(names[v])] || []).push(x + ox, y + oy + 1, z + oz);
}

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html$/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const q = pending.get(m.id); if (!q) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) q.reject(new Error(JSON.stringify(m.result.exceptionDetails).slice(0, 1500))); else q.resolve(m.result); });
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
    await c.evaluate('ST.clear(); true');
    for (const [m, xyz] of Object.entries(byMat)) {
      for (let i = 0; i < xyz.length; i += 90000) {
        const part = xyz.slice(i, i + 90000);
        await c.evaluate(`(() => { const a = ${JSON.stringify(part)}; for (let i = 0; i < a.length; i += 3) ST.set(a[i], a[i + 1], a[i + 2], ${JSON.stringify(m)}); return true; })()`);
      }
    }
    // The sea of clouds far below.
    await c.evaluate(`(() => { const rnd = ST.seeded(77);
      for (let i = 0; i < 170; i++) { const x = Math.round((rnd() - 0.5) * 900), z = Math.round((rnd() - 0.5) * 900);
        ST.blob(x, -170 + Math.round(rnd() * 8), z, 14 + Math.floor(rnd() * 16), 3, 10 + Math.floor(rnd() * 14), 'cloud', 0.2, rnd); }
      return true; })()`);
    const views = {
      'render-overview': { position: [215, 200, 235], target: [0, -24, 0], fov: 44 },
      'render-bowl': { position: [52, 44, 58], target: [-8, -2, -8], fov: 62 },
      'render-floor': { position: [-22, 3.6, 26], target: [6, 3, -8], fov: 70 },
    };
    for (const [name, v] of Object.entries(views)) {
      const png = await c.evaluate(`ST.render(${JSON.stringify({ ...v, unclamped: true })}, 1920, 1080, '#9FB8DC', 520, 1300)`);
      const file = path.join(OUT, `${name}.png`);
      fs.writeFileSync(file, Buffer.from(png.split(',')[1], 'base64'));
      console.log(name, await c.evaluate('ST.count()'), 'voxels ->', file);
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exit(1); });
