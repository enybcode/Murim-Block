// Preview sheets of Sensei Yun and his sword the Azure Cloud, rendered in Blockbench itself (what you see there is what
// is rendered), to docs/mobs/images: the sword alone from four angles and its hilt up close (azure-cloud-sword-<view>.png),
// the sensei from five angles and his face (sensei-<view>.png), and the sensei holding the sword in his right hand
// (sensei-sword-<view>.png): the sword's cubes are put on the Tool_R bone as the game holds it, the blade forward.
// Run the two model scripts first (azure-cloud-sword-model.cjs, sensei-3d.cjs).
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9242 --user-data-dir=<empty profile>)
//   node scripts/concepts/sensei-preview.cjs [--port=9242]
const fs = require('node:fs');
const path = require('node:path');
const sword = require('../animation/azure-cloud-sword-model.cjs');
const { at } = require('../animation/blockbench-rig.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'mobs', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9242);

const free = (name, elements, textures, extra = {}) => ({
  meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name, model_identifier: '', visible_box: [1, 1, 0],
  variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
  resolution: { width: sword.TEX_SIZE, height: sword.TEX_SIZE }, elements, outliner: elements.map(e => e.uuid), textures, ...extra,
});

/** The sword upright, its tassel hanging at rest. */
const swordProject = () => free('azure_cloud_sword', sword.elements(true), [sword.texture()]);

/** The sensei with the sword in his right hand: the sword's cubes on Tool_R, the blade forward (-z), the edges up and down. */
function holdingProject() {
  const sensei = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'azure_sensei.bbmodel'), 'utf8'));
  const [tx, ty, tz] = at('Tool_R');
  const tex = sensei.textures.length;
  const gripC = (sword.POMMEL_END + 1.75 + sword.GRIP1) / 2;
  const pommelZ = -(sword.POMMEL_END - gripC);
  // Upright sword (x across the blade, y along it, z its thickness) -> the hand's frame (x thickness, y across, z along).
  const body = sword.PARTS.map(p => {
    const a = [p.from[2] - 8, p.from[0] - 8, -(p.from[1] - gripC)], b = [p.to[2] - 8, p.to[0] - 8, -(p.to[1] - gripC)];
    return { ...p, from: a.map((v, i) => Math.min(v, b[i])), to: a.map((v, i) => Math.max(v, b[i])) };
  });
  // The tassel hangs straight down from the pommel's ring.
  const tassel = sword.TASSEL.map(p => ({ ...p, from: [p.from[0] - 8, p.from[1] - 8, pommelZ + p.from[2] - 8], to: [p.to[0] - 8, p.to[1] - 8, pommelZ + p.to[2] - 8] }));
  const cubes = [...tassel, ...body].map((p, i) => {
    const f = sword.faces(p);
    for (const k of Object.keys(f)) f[k] = { uv: f[k].uv, texture: tex };
    return { name: 'sword_' + p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: [tx + p.from[0], ty + p.from[1], tz + p.from[2]], to: [tx + p.to[0], ty + p.to[1], tz + p.to[2]], autouv: 0, color: 0,
      origin: [tx, ty, tz], rotation: [0, 0, 0], faces: f, type: 'cube', uuid: '0d0d0d0d-0000-4000-b000-' + String(i).padStart(12, '0') };
  });
  const groups = {};
  const walk = g => { if (g && typeof g === 'object') { groups[g.name] = g; (g.children || []).forEach(walk); } };
  sensei.outliner.forEach(walk);
  groups.Tool_R.children.push(...cubes.map(c => c.uuid));
  // Each texture keeps its own UV size: the sword's is 32 x 32, the atlas 128 x 128.
  sensei.textures.push({ ...sword.texture(), id: String(tex) });
  sensei.elements.push(...cubes);
  return { ...sensei, name: 'sensei_with_sword' };
}

// ---------------------------------------------------------------- rendering

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
      const p = window.__senseiPreview || (window.__senseiPreview = new Preview({ id: 'sensei_render', offscreen: true, antialias: true }));
      p.resize(${w}, ${h});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 38 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#EDE4CF');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(file(view), Buffer.from(data.split(',')[1], 'base64'));
  }
}

/** The sword: its middle about (8, 5.4, 8), the flat facing north. */
const SWORD_VIEWS = {
  front: [[8, 6, -44], [8, 5.4, 8]], three: [[-24, 14, -26], [8, 5.4, 8]], edge: [[-44, 6, 8.01], [8, 5.4, 8]], back: [[32, 16, 50], [8, 5.4, 8]],
  hilt: [[16, 4, -15], [8, 0.8, 8], [560, 480]],
};
/** The sensei: about 37 pixels tall with his hat, facing north. */
const SENSEI_VIEWS = {
  front: [[0, 22, -66], [0, 18, 0]], three: [[-46, 30, -50], [0, 18, 0]], side: [[-66, 20, 0.01], [0, 18, 0]], back: [[44, 30, 52], [0, 18, 0]],
  face: [[-6, 30, -24], [0, 27, 0], [560, 480]],
};
const HOLD_VIEWS = { three: [[-50, 28, -58], [0, 17, -6], [560, 560]], side: [[-72, 20, -8], [0, 16, -8], [560, 560]] };

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const bb = await connect(PORT);
  await render(bb, swordProject(), SWORD_VIEWS, v => path.join(OUT, `azure-cloud-sword-${v}.png`));
  const sensei = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'azure_sensei.bbmodel'), 'utf8'));
  await render(bb, sensei, SENSEI_VIEWS, v => path.join(OUT, `sensei-${v}.png`));
  await render(bb, holdingProject(), HOLD_VIEWS, v => path.join(OUT, `sensei-sword-${v}.png`));
  bb.close();
  console.log('rendered the sword, the sensei and the sensei with his sword to', path.relative(ROOT, OUT));
}

module.exports = { holdingProject, swordProject };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
