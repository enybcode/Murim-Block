// Preview sheets of the three concept looks of the Raven Master (scripts/animation/raven-master-3d.cjs), rendered in
// Blockbench itself, to docs/mobs/images/raven-master-<n>-<view>.png, and a contact sheet for each look
// (raven-master-<n>-sheet.png) and one of the three side by side (raven-master-sheet.png).
// Each scene: the body (cloth only), his iron fan open in his raised right hand (a separate item: dark iron guards, night-
// blue silk, a gold rim), and for the mask look the familiar on his left shoulder. The far view puts him standing on a
// rock with two ravens, seen from below against a dusk sky at about 30 pixels tall, the size he has at 40-60 blocks on a
// 1080p screen (shown enlarged x2 in the sheets, pixel for pixel). The composed scenes are also written to
// art/blockbench/raven_master_<n>_scene.bbmodel, to open in Blockbench.
//   node scripts/animation/raven-master-3d.cjs
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9252 --user-data-dir=<empty profile>)
//   node scripts/concepts/raven-master-preview.cjs [--only=1,3] [--port=9252]
const fs = require('node:fs');
const path = require('node:path');
const { palette, builder, textureOf, connect, id } = require('./chief-axes.cjs');
const { crow, STYLES } = require('./raven-familiars.cjs');
const { at } = require('../animation/blockbench-rig.cjs');
const { VARIANTS } = require('../animation/raven-master-3d.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'mobs', 'images');
const ART = path.join(ROOT, 'art', 'blockbench');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9252);

const PROPS = palette({ ...STYLES.shadow.colours, iron: '#3c3f47', ironEdge: '#7a808a', silk: '#24304a', silkLight: '#3d5274',
  stone: '#7b7d84', stoneDark: '#5d5f66', moss: '#4f5a3c' });
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];

/** Boxes of PROPS' materials as Blockbench cubes on texture `tex`; each box may carry rotation and origin. */
function cubes(parts, tex, tag) {
  return parts.map((p, n) => {
    const row = PROPS.names.indexOf(p.mat);
    if (row < 0) throw new Error('no material ' + p.mat);
    const uv = [p.shade, row, p.shade + 1, row + 1];
    return { name: `${tag}_${p.mat}_${n}`, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: p.from, to: p.to, autouv: 0, color: 0, origin: p.origin || [0, 0, 0], rotation: p.rotation || [0, 0, 0],
      faces: Object.fromEntries(FACES.map(f => [f, { uv, texture: tex }])), type: 'cube', uuid: id('ravenmaster/' + tag + '/' + n) };
  });
}

/**
 * The iron fan open, in the rest frame of the right hand (the hand hanging, the item pointing forward, -z): its pivot at
 * Tool_R, 13 ribs fanned over 150 degrees in the x-z plane round -z, silk between them, a gold rim, iron guards.
 */
function fan() {
  const b = builder(), [tx, ty, tz] = at('Tool_R'), o = [tx, ty, tz], RIB = 8.4, N = 13, OPEN = 150;
  for (let i = 0; i < N; i++) {
    const a = -OPEN / 2 + (OPEN * i) / (N - 1), guard = i === 0 || i === N - 1;
    b.box(guard ? 'ironEdge' : 'iron', guard ? 9 : 7, [tx - (guard ? 0.3 : 0.18), ty - 0.2, tz - RIB - (guard ? 0.3 : 0)], [tx + (guard ? 0.3 : 0.18), ty + 0.2, tz - 0.3],
      { rotation: [0, a, 0], origin: o });
    if (i < N - 1) {
      const m = a + OPEN / (N - 1) / 2;
      b.box(i % 2 ? 'silk' : 'silkLight', i % 2 ? 9 : 8, [tx - 0.95, ty - 0.06, tz - RIB + 0.5], [tx + 0.95, ty + 0.06, tz - 3.2], { rotation: [0, m, 0], origin: o });
      b.box('ring', 11, [tx - 0.98, ty - 0.08, tz - RIB + 0.1], [tx + 0.98, ty + 0.08, tz - RIB + 0.6], { rotation: [0, m, 0], origin: o });
      if (i === 5 || i === 6) b.box('eye', 13, [tx - 0.5, ty - 0.1, tz - 6.2], [tx + 0.5, ty + 0.1, tz - 5.2], { rotation: [0, m, 0], origin: o });
    }
  }
  b.box('ring', 12, [tx - 0.6, ty - 0.35, tz - 0.9], [tx + 0.6, ty + 0.35, tz + 0.3]);
  b.box('eye', 14, [tx - 0.25, ty - 1.4, tz - 0.55], [tx + 0.25, ty - 0.35, tz - 0.05]);
  return b.parts;
}

/** A shadow raven (raven-familiars.cjs) standing at (x, y, z), turned by quarter turns `q` about y (1: its beak to +x). */
function raven(x, y, z, q = 0) {
  const b = builder();
  crow(b, { body: 'feather', wing: 'wing' });
  b.box('scroll', 10, [-0.6, 5.6, 0.3], [0.6, 6.5, 2.9]);
  b.box('ring', 12, [-0.7, 5.5, 0.2], [0.7, 6.6, 0.55]);
  b.box('ring', 12, [-0.7, 5.5, 2.65], [0.7, 6.6, 3.0]);
  const turn = ([px, py, pz]) => { let a = [px, pz]; for (let k = 0; k < ((q % 4) + 4) % 4; k++) a = [-a[1], a[0]]; return [a[0] + x, py + y, a[1] + z]; };
  return b.parts.map(p => {
    const a = turn(p.from), c = turn(p.to);
    return { ...p, from: a.map((v, i) => Math.min(v, c[i])), to: a.map((v, i) => Math.max(v, c[i])) };
  });
}

/** The rock he stands on in the far view, its top at y 0, a dead branch on it; two ravens perched. */
function rock() {
  const b = builder();
  b.box('stoneDark', 7, [-16, -30, -13], [17, -12, 14]);
  b.box('stone', 8, [-13, -12, -11], [14, -4, 12]);
  b.box('stone', 9, [-9, -4, -8], [10, 0, 9]);
  b.box('moss', 8, [-9.2, -0.6, 2], [3, 0.2, 9.2]);
  b.box('stoneDark', 6, [8, -4, -11], [14, -1, -4]);
  return [...b.parts, ...raven(11, -4, -6, 1), ...raven(-12, -4, 4, -1)];
}

function scene(n, { far = false } = {}) {
  const body = JSON.parse(fs.readFileSync(path.join(ART, `raven_master_${n}.bbmodel`), 'utf8'));
  const tex = body.textures.length;
  const groups = {};
  const walk = g => { if (g && typeof g === 'object') { groups[g.name] = g; (g.children || []).forEach(walk); } };
  body.outliner.forEach(walk);
  // The right arm raised, the forearm up in front of him, the fan open beside his chest.
  groups.Arm_R.rotation = [12, 0, 24];
  groups.Hand_R.rotation = [84, 0, 0];
  const fanCubes = cubes(fan(), tex, 'fan');
  groups.Tool_R.children.push(...fanCubes.map(c => c.uuid));
  const loose = [];
  if (n === 2) loose.push(...cubes(raven(-7.0, 25.0, 1.0, 3), tex, 'shoulder_raven'));
  if (far) loose.push(...cubes(rock(), tex, 'rock'));
  body.textures.push(textureOf('raven_master_props.png', PROPS.png, tex));
  body.elements.push(...fanCubes, ...loose);
  body.outliner.push(...loose.map(c => c.uuid));
  return { ...body, name: `raven_master_${n}_${far ? 'far' : 'scene'}` };
}

// ---------------------------------------------------------------- rendering

const BG = '#EDE4CF', SKY = '#8d9cb4';
async function load(bb, project) {
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'p.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 40 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 400)); return Outliner.elements.length; })()`);
  if (loaded !== project.elements.length) throw new Error(`${project.name}: ${loaded} of ${project.elements.length} cubes loaded`);
}
async function shot(bb, [pos, target, [w, h] = [480, 560], bg = BG, fov = 38]) {
  const data = await bb.evaluate(`(async () => {
    const p = window.__rmPreview || (window.__rmPreview = new Preview({ id: 'raven_master_render', offscreen: true, antialias: true }));
    p.resize(${w}, ${h});
    p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: ${fov} });
    p.camera.lookAt(...${JSON.stringify(target)});
    const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('${bg}');
    return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
  })()`);
  return Buffer.from(data.split(',')[1], 'base64');
}

/** About 38 pixels tall with the hat, facing north. */
const VIEWS = {
  front: [[0, 22, -72], [0, 18, 0]],
  three: [[-48, 30, -54], [0, 18, 0]],
  side: [[-72, 20, 0.01], [0, 18, 0]],
  back: [[46, 30, 54], [0, 18, 0]],
  face: [[-7, 29, -26], [0, 27.5, 0], [560, 480]],
};
/** From below and in front, far off: about 30 pixels tall in a 240 x 180 view. */
const FAR = [[-62, -38, -262], [0, 12, 0], [240, 180], SKY];

const TEXT = {
  1: ['Shadow Hermit', 'A thin man under a wide black conical hat; a veil of black gauze falls all round his head, parted in front so the amber eyes show through. A mantle of black feathers on the shoulders, a long feather cloak to the calves with a ragged hem of feather tips. Charcoal robe, night-blue sash, amber cord.'],
  2: ['Raven Mask', 'A black lacquered half mask with a crow\'s beak and amber eye rings; long white hair down his back. A blue-black robe trimmed in night blue, three tiers of feather pauldrons on each shoulder, a night-blue tabard hemmed in amber. His shadow raven on his left shoulder.'],
  3: ['Messenger Lord', 'A noble scholar in deep indigo over black, gold-amber trims; a standing collar of black feathers behind the head. The familiar\'s message case, black lacquer ringed in gold, slung across his back. Topknot under a small gold crown with a black feather pin; a fine moustache.'],
};

/** Contact sheets, drawn on a canvas in Blockbench's page. */
async function sheet(bb, layout, file) {
  const data = await bb.evaluate(`(async () => {
    const L = ${JSON.stringify(layout)};
    const c = document.createElement('canvas'); c.width = L.w; c.height = L.h; const g = c.getContext('2d');
    g.fillStyle = '#f4ecda'; g.fillRect(0, 0, L.w, L.h);
    const img = src => new Promise(r => { const i = new Image(); i.onload = () => r(i); i.src = 'data:image/png;base64,' + src; });
    for (const it of L.items) {
      if (it.img) { const i = await img(it.img); g.imageSmoothingEnabled = !it.pixel; g.drawImage(i, it.x, it.y, it.w, it.h);
        g.strokeStyle = '#b9a98a'; g.lineWidth = 1; g.strokeRect(it.x + 0.5, it.y + 0.5, it.w - 1, it.h - 1); }
      if (it.text) { g.fillStyle = it.color || '#2a2620'; g.font = it.font || '16px Georgia';
        const words = it.text.split(' '); let line = '', y = it.y;
        for (const wd of words) { const t = line ? line + ' ' + wd : wd; if (it.wrap && g.measureText(t).width > it.wrap) { g.fillText(line, it.x, y); line = wd; y += it.lh || 22; } else line = t; }
        g.fillText(line, it.x, y); }
    }
    return c.toDataURL('image/png');
  })()`);
  fs.writeFileSync(file, Buffer.from(data.split(',')[1], 'base64'));
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',').map(Number) : [1, 2, 3];
  const bb = await connect(PORT);
  const shots = {};
  for (const n of only) {
    const s = scene(n), f = scene(n, { far: true });
    fs.writeFileSync(path.join(ART, `raven_master_${n}_scene.bbmodel`), JSON.stringify(s, null, 1));
    await load(bb, s);
    shots[n] = {};
    for (const [view, v] of Object.entries(VIEWS)) {
      shots[n][view] = await shot(bb, v);
      fs.writeFileSync(path.join(OUT, `raven-master-${n}-${view}.png`), shots[n][view]);
    }
    await load(bb, f);
    shots[n].far = await shot(bb, FAR);
    fs.writeFileSync(path.join(OUT, `raven-master-${n}-far.png`), shots[n].far);
    // The sheet of one look: four views on top, the face, the far view (x2, pixel for pixel) and the text below.
    const b64 = k => shots[n][k].toString('base64'), items = [];
    items.push({ text: `Raven Master, look ${n}: ${TEXT[n][0]}`, x: 24, y: 40, font: 'bold 28px Georgia' });
    ['front', 'three', 'side', 'back'].forEach((k, i) => items.push({ img: b64(k), x: 24 + i * 372, y: 64, w: 360, h: 420 },
      { text: { front: 'front', three: 'three-quarter', side: 'side', back: 'back three-quarter' }[k], x: 30 + i * 372, y: 506, font: '15px Georgia', color: '#6a5f4c' }));
    items.push({ img: b64('face'), x: 24, y: 524, w: 420, h: 360 }, { text: 'face', x: 30, y: 906, font: '15px Georgia', color: '#6a5f4c' });
    items.push({ img: b64('far'), x: 464, y: 524, w: 480, h: 360, pixel: true },
      { text: 'seen from far (~30 px tall: about 50 blocks on a 1080p screen), enlarged x2', x: 470, y: 906, font: '15px Georgia', color: '#6a5f4c' });
    items.push({ img: b64('far'), x: 964, y: 524, w: 240, h: 180, pixel: true }, { text: 'actual size', x: 970, y: 726, font: '15px Georgia', color: '#6a5f4c' });
    items.push({ text: TEXT[n][1], x: 964, y: 770, wrap: 530, lh: 22, font: '16px Georgia' });
    items.push({ text: 'Cloth only: the iron fan (shown open in his right hand) is a separate item.', x: 964, y: 770 + 22 * 6, wrap: 530, font: 'italic 14px Georgia', color: '#6a5f4c' });
    await sheet(bb, { w: 1512, h: 940, items }, path.join(OUT, `raven-master-${n}-sheet.png`));
    console.log('rendered look', n);
  }
  if (only.length === 3) {
    const items = [{ text: 'Raven Master (鸦主): three looks to choose from', x: 24, y: 42, font: 'bold 30px Georgia' }];
    [1, 2, 3].forEach((n, i) => {
      const x = 24 + i * 500;
      items.push({ text: `${n}. ${TEXT[n][0]}`, x, y: 92, font: 'bold 22px Georgia' },
        { img: shots[n].three.toString('base64'), x, y: 108, w: 240, h: 280 }, { img: shots[n].back.toString('base64'), x: x + 248, y: 108, w: 240, h: 280 },
        { img: shots[n].face.toString('base64'), x, y: 396, w: 240, h: 206 }, { img: shots[n].far.toString('base64'), x: x + 248, y: 396, w: 240, h: 180, pixel: true },
        { text: 'far, actual size', x: x + 252, y: 596, font: '13px Georgia', color: '#6a5f4c' },
        { text: TEXT[n][1], x, y: 634, wrap: 480, lh: 20, font: '14px Georgia' });
    });
    await sheet(bb, { w: 1524, h: 820, items }, path.join(OUT, 'raven-master-sheet.png'));
  }
  bb.close();
  console.log('wrote the Raven Master previews to', path.relative(ROOT, OUT));
}

if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
