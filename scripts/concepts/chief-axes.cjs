// Concept sheets for the Bandit Chief's great axe: six styles, each rendered from four angles and once standing beside
// the chief for its size, to docs/mobs/images/chief-axe-<style>-<view>.png. Concepts to choose from, not game files:
// the chosen style then goes into scripts/animation/chief-axe-model.cjs.
// Built at the chief's own scale (Blockbench pixels, before the game draws both 1.3 times larger): the haft upright from
// y 0, the blade toward -x as in the game model (so its edge faces down in his hands), the haft's centre at x 0, z 0.
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/concepts/chief-axes.cjs [--only=crescent,tiger] [--port=9232]
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { png } = require('../animation/mesh-kit.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'mobs', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);

// ---------------------------------------------------------------- materials: one row of 16 shades each

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
/** A style's palette: material -> base colour. The texture holds 16 shades of each, dark (0) to light (15). */
function palette(colours) {
  const names = Object.keys(colours), img = new Uint8Array(16 * 16 * 4);
  names.forEach((n, row) => {
    const base = hex(colours[n]);
    for (let s = 0; s < 16; s++) {
      const f = 0.55 + s * 0.05;
      img.set([...base.map(v => Math.max(0, Math.min(255, Math.round(v * f)))), 255], (row * 16 + s) * 4);
    }
  });
  return { names, png: png(16, 16, img) };
}

// ---------------------------------------------------------------- building

/** A style's parts: box(material, shade 0-15, from, to, [tilt about z, around `pivot`]). */
function builder() {
  const parts = [];
  const box = (mat, shade, from, to, extra = {}) => parts.push({ mat, shade, from, to, ...extra });
  /** A haft from y0 to y1 (its section `w`), with bands of `band` material at the given heights. */
  const haft = (mat, y0, y1, w = 1.6) => box(mat, 7, [-w / 2, y0, -w / 2], [w / 2, y1, w / 2]);
  const ring = (mat, y, h = 0.9, w = 2.2) => box(mat, 10, [-w / 2, y, -w / 2], [w / 2, y + h, w / 2]);
  return { parts, box, haft, ring };
}

/**
 * A blade in steps from the haft outward (toward -x when side is -1): each step's [width, bottom, top]; the last steps
 * are the edge, brighter. thickness t, thinner toward the edge.
 */
function blade(b, mat, edgeMat, x0, steps, side = -1, t = 0.9) {
  let x = x0;
  steps.forEach(([w, y0, y1], i) => {
    const last = i >= steps.length - 1, k = i / Math.max(1, steps.length - 1);
    const tt = t * (1 - 0.35 * k);
    const [a, c] = side < 0 ? [x - w, x] : [x, x + w];
    b.box(last ? edgeMat : mat, last ? 14 : 5 + Math.round(k * 7), [a, y0, -tt / 2], [c, y1, tt / 2]);
    x += side * w;
  });
}

const STYLES = {
  crescent: {
    name: 'Crescent Moon', zh: '月牙斧', fr: 'Croissant de lune',
    colours: { steel: '#a9b0b6', edge: '#eef1f3', iron: '#565c63', wood: '#4e3220', cord: '#8e2a22', bronze: '#c39a3c', tassel: '#c4382c' },
    build(b) {
      b.haft('wood', 0, 34);
      b.box('cord', 6, [-1.05, 3, -1.05], [1.05, 12.6, 1.05]);
      b.box('iron', 6, [-1.2, -1.2, -1.2], [1.2, 0.2, 1.2]);
      b.ring('bronze', 24);
      b.box('iron', 5, [-1.6, 24.9, -1.4], [1.6, 34.5, 1.4]);
      b.box('iron', 8, [-0.5, 34.5, -0.5], [0.5, 38, 0.5]);
      // The crescent: a narrow neck, a wide curved edge, its two horns curling back toward the haft.
      blade(b, 'steel', 'edge', -1.6, [[2.2, 27.2, 31.6], [2.0, 25.8, 33.0], [2.0, 24.2, 34.6], [1.6, 22.6, 36.2]]);
      b.box('edge', 14, [-9.4, 36.2, -0.3], [-6.2, 37.4, 0.3]);
      b.box('edge', 14, [-6.8, 37.4, -0.25], [-5.4, 38.4, 0.25]);
      b.box('edge', 14, [-9.4, 21.4, -0.3], [-6.2, 22.6, 0.3]);
      b.box('edge', 14, [-6.8, 20.4, -0.25], [-5.4, 21.4, 0.25]);
      // The back spike and the tassel.
      b.box('iron', 6, [1.6, 28.4, -0.4], [4.8, 30.8, 0.4]);
      b.box('edge', 13, [4.8, 29.0, -0.25], [6.4, 30.1, 0.25]);
      b.box('tassel', 9, [-2.6, 19, -0.3], [-1.6, 24, 0.3]);
      b.box('tassel', 7, [-3.1, 16.6, -0.6], [-1.1, 19, 0.6]);
    },
    text: 'Le modèle actuel : un croissant d\'acier aux deux cornes recourbées, une pointe arrière, une houppe rouge. Lisible et élégant, une hache de chef de bande classique.',
  },
  tiger: {
    name: 'Tiger Maw', zh: '虎口斧', fr: 'Gueule de tigre',
    colours: { steel: '#9aa3ab', edge: '#f2f4f6', bronze: '#c39a3c', gold: '#e2b84a', eye: '#f5d24a', fang: '#efe7d2', lacquer: '#3a1612', tassel: '#d0561e', dark: '#2a1a12' },
    build(b) {
      b.haft('lacquer', 0, 33);
      for (const y of [4, 9, 14]) b.ring('gold', y, 0.7);
      b.box('bronze', 6, [-1.3, -1.6, -1.3], [1.3, 0.3, 1.3]);
      // The socket is a bronze tiger's head, jaws open toward the blade.
      b.box('bronze', 8, [-2.6, 24.4, -2.2], [2.4, 31.6, 2.2]);
      b.box('bronze', 10, [-1.8, 31.6, -1.6], [-0.4, 33, -0.2]);
      b.box('bronze', 10, [0.4, 31.6, -1.6], [1.8, 33, -0.2]);
      // Its face toward the blade: a heavy brow, wide eyes above the blade, fangs closing on it from above and below.
      b.box('bronze', 11, [-3.4, 31.0, -2.0], [-2.4, 31.9, 2.0]);
      b.box('eye', 15, [-3.0, 30.4, -2.1], [-2.4, 31.0, -1.1]);
      b.box('eye', 15, [-3.0, 30.4, 1.1], [-2.4, 31.0, 2.1]);
      b.box('dark', 3, [-3.05, 30.5, -1.8], [-2.95, 30.9, -1.4]);
      b.box('dark', 3, [-3.05, 30.5, 1.4], [-2.95, 30.9, 1.8]);
      for (const z of [-1.6, -0.4, 0.8]) {
        b.box('fang', 13, [-3.6, 29.6, z], [-2.6, 30.4, z + 0.8]);
        b.box('fang', 13, [-3.6, 25.4, z], [-2.6, 26.2, z + 0.8]);
      }
      b.box('bronze', 9, [-3.4, 24.6, -2.0], [-2.4, 25.4, 2.0]);
      // A broad blade flaring out of the jaws.
      blade(b, 'steel', 'edge', -2.6, [[2.2, 26, 30.4], [2.4, 24.6, 31.8], [2.6, 23, 33.4], [2.4, 21.6, 34.8], [1.4, 20.8, 35.6]], -1, 1.0);
      // A tail curling back from the head, and the tassel.
      b.box('bronze', 7, [2.4, 27, -0.5], [4.4, 28.4, 0.5]);
      b.box('bronze', 7, [4.0, 28.4, -0.45], [5.4, 30.6, 0.45]);
      b.box('gold', 12, [4.4, 30.6, -0.35], [5.8, 31.6, 0.35]);
      b.box('tassel', 9, [-0.4, 19.8, 2.2], [0.4, 24.4, 2.8]);
      b.box('tassel', 7, [-0.8, 17.4, 2.0], [0.8, 19.8, 3.0]);
    },
    text: 'La douille est une tête de tigre en bronze, gueule ouverte, d\'où jaillit une large lame évasée ; la queue du tigre se recourbe à l\'arrière. Elle répond à sa peau de tigre et à la boucle de sa ceinture.',
  },
  splitter: {
    name: 'Mountain Splitter', zh: '开山斧', fr: 'Fend-la-montagne',
    colours: { steel: '#7b8189', edge: '#e9edf0', iron: '#4a4e55', bronze: '#b58b3a', wood: '#5a3a22', leather: '#22180f' },
    build(b) {
      b.haft('wood', 0, 32, 1.9);
      b.box('leather', 6, [-1.15, 2, -1.15], [1.15, 13, 1.15]);
      b.box('iron', 6, [-1.4, -1.4, -1.4], [1.4, 0.4, 1.4]);
      // A massive cleaver head, a hammer face behind it.
      b.box('iron', 5, [-2.4, 21.6, -1.6], [1.6, 33.6, 1.6]);
      b.box('iron', 7, [1.6, 25, -1.8], [5.2, 30, 1.8]);
      b.box('iron', 9, [5.2, 25.4, -2.0], [5.8, 29.6, 2.0]);
      blade(b, 'steel', 'edge', -2.4, [[3.2, 21.4, 33.8], [3.4, 20.4, 33.8], [3.0, 19.2, 33.8], [1.2, 18.6, 33.8]], -1, 1.4);
      // Three rings through the top of the blade: they ring when it swings.
      for (const x of [-4.4, -7.4, -10.2]) {
        b.box('bronze', 10, [x - 0.9, 33.8, -0.25], [x + 0.9, 34.3, 0.25]);
        b.box('bronze', 8, [x - 0.9, 34.3, -0.25], [x - 0.5, 35.9, 0.25]);
        b.box('bronze', 8, [x + 0.5, 34.3, -0.25], [x + 0.9, 35.9, 0.25]);
        b.box('bronze', 10, [x - 0.9, 35.9, -0.25], [x + 0.9, 36.4, 0.25]);
      }
      b.box('iron', 8, [-0.6, 33.6, -0.6], [0.6, 35.4, 0.6]);
    },
    text: 'Un tranchoir massif de bûcheron de légende, presque carré, une face de marteau au dos, trois anneaux de bronze passés dans le haut de la lame qui sonnent à chaque coup. La plus brutale.',
  },
  twin: {
    name: 'Twin Moons', zh: '双月斧', fr: 'Lunes jumelles',
    colours: { steel: '#9fb4c7', edge: '#f4f8fb', silver: '#c9cfd6', cord: '#1f3a5c', wood: '#2c2420', tassel: '#e0e6ee' },
    build(b) {
      b.haft('wood', 0, 32);
      b.box('cord', 6, [-1.05, 4, -1.05], [1.05, 13, 1.05]);
      b.box('silver', 9, [-0.6, -3.4, -0.6], [0.6, 0, 0.6]);
      b.box('silver', 12, [-0.3, -4.6, -0.3], [0.3, -3.4, 0.3]);
      b.ring('silver', 23.4);
      b.box('silver', 8, [-1.5, 24.3, -1.3], [1.5, 32.4, 1.3]);
      // Two crescents back to back, and a spear point on top.
      for (const side of [-1, 1]) {
        blade(b, 'steel', 'edge', side * 1.5, [[2.0, 25.2, 31.4], [2.2, 24, 32.6], [2.0, 23, 33.6], [1.4, 22.4, 34.2]], side, 0.8);
        const xo = side < 0 ? [-9.4, -7.8] : [7.8, 9.4];
        b.box('edge', 14, [xo[0], 34.2, -0.25], [xo[1], 35.4, 0.25]);
        b.box('edge', 14, [xo[0], 21.2, -0.25], [xo[1], 22.4, 0.25]);
      }
      b.box('steel', 9, [-0.9, 32.4, -0.45], [0.9, 37, 0.45]);
      b.box('edge', 13, [-0.6, 37, -0.3], [0.6, 40, 0.3]);
      b.box('edge', 15, [-0.25, 40, -0.15], [0.25, 41.2, 0.15]);
      b.box('tassel', 10, [-0.35, 18.4, 1.3], [0.35, 23.4, 1.9]);
      b.box('tassel', 8, [-0.7, 16.2, 1.1], [0.7, 18.4, 2.1]);
    },
    text: 'Deux croissants dos à dos et une pointe de lance au sommet : elle frappe des deux côtés et peut piquer. Acier bleuté, argent et corde indigo, une arme de duelliste plus fine que brutale.',
  },
  yue: {
    name: "Warlord's Yue", zh: '大钺', fr: 'Yue du seigneur de guerre',
    colours: { steel: '#b0b6bc', gold: '#e0b347', lacquer: '#8e1c1c', bronze: '#a8843e', tassel: '#c62828', dark: '#2a1a14' },
    build(b) {
      b.haft('lacquer', 0, 32);
      for (const y of [3, 7, 11, 15]) b.ring('gold', y, 0.6);
      // A gold dragon-head pommel.
      b.box('gold', 9, [-1.4, -2.2, -1.4], [1.4, 0.2, 1.4]);
      b.box('gold', 11, [-2.2, -1.6, -0.6], [-1.4, -0.4, 0.6]);
      b.ring('gold', 21.6, 1.2, 2.6);
      b.box('bronze', 8, [-1.8, 22.8, -1.4], [1.8, 30.6, 1.4]);
      // The wide fan of the yue: a half moon from the socket, a round hole cut in it, a gilt edge.
      const fan = [[2.0, 23.6, 29.8], [2.0, 22.2, 31.2], [2.0, 21, 32.4], [2.0, 20, 33.4], [1.8, 19.2, 34.2], [1.4, 18.6, 34.8]];
      let x = -1.8;
      fan.forEach(([w, y0, y1], i) => {
        const last = i === fan.length - 1;
        // The hole: steps 1 and 2 split around it.
        if (i === 1 || i === 2) {
          b.box('steel', 7 + i, [x - w, y0, -0.45], [x, 25.2, 0.45]);
          b.box('steel', 7 + i, [x - w, 28.2, -0.45], [x, y1, 0.45]);
        } else b.box(last ? 'gold' : 'steel', last ? 13 : 7 + i, [x - w, y0, -0.45], [x, y1, 0.45]);
        x -= w;
      });
      // Long double tassels from the socket.
      for (const z of [-1.0, 1.0]) {
        b.box('tassel', 9, [0.6, 15, z - 0.3], [1.2, 22.8, z + 0.3]);
        b.box('tassel', 7, [0.3, 11.6, z - 0.5], [1.5, 15, z + 0.5]);
      }
      b.box('gold', 12, [-0.5, 30.6, -0.5], [0.5, 33.6, 0.5]);
    },
    text: 'Le yue, hache cérémonielle des généraux : un large éventail d\'acier en demi-lune, percé d\'un cercle, bordé d\'or, deux longues houppes rouges et un pommeau en tête de dragon. Celle d\'un bandit qui se prend pour un roi.',
  },
  brute: {
    name: "Brute's Iron", zh: '粗铁斧', fr: 'Fer du brutal',
    colours: { rust: '#7a4a2a', iron: '#4f4a45', edge: '#cfd3d6', chain: '#3d3d40', bone: '#d9cdb0', leather: '#3a2618', wood: '#4a3020' },
    build(b) {
      b.haft('wood', 0, 31, 2.0);
      b.box('leather', 5, [-1.2, 3, -1.2], [1.2, 6.5, 1.2]);
      b.box('leather', 5, [-1.2, 9, -1.2], [1.2, 12, 1.2]);
      // A chain wound round the haft.
      for (let i = 0; i < 7; i++) b.box('chain', 9 - (i % 2) * 3, [-1.35, 14 + i * 1.1, -1.35], [1.35, 14.6 + i * 1.1, 1.35]);
      // A bone knob for a pommel.
      b.box('bone', 11, [-1.6, -2.4, -1.6], [1.6, 0.2, 1.6]);
      b.box('bone', 13, [-0.8, -3.2, -0.8], [0.8, -2.4, 0.8]);
      // A crude chipped blade, rusted, its edge only bright where it was ground.
      b.box('iron', 5, [-2.2, 22, -1.5], [1.4, 31.4, 1.5]);
      const steps = [[2.6, 21.2, 32.2], [1.8, 20.6, 31.4], [2.2, 19.4, 32.8], [1.6, 20.2, 33.4], [1.2, 18.8, 32]];
      let x = -2.2;
      steps.forEach(([w, y0, y1], i) => {
        const last = i === steps.length - 1;
        b.box(last ? 'edge' : 'rust', last ? 9 : 4 + (i % 3) * 3, [x - w, y0, -0.6], [x, y1, 0.6]);
        x -= w;
      });
      // Chips out of the edge, studs on the cheek.
      b.box('rust', 3, [x - 0.4, 24, -0.4], [x + 0.6, 25.4, 0.4]);
      for (const [sx, sy] of [[-4, 23], [-6.4, 29], [-5, 26]]) b.box('iron', 10, [sx, sy, -0.75], [sx + 0.8, sy + 0.8, 0.75]);
      b.box('iron', 7, [1.4, 26, -0.6], [3.4, 28.6, 0.6]);
      b.box('bone', 12, [3.4, 26.4, -0.4], [5.0, 28.2, 0.4]);
    },
    text: 'Forgée à la hâte et jamais entretenue : une lame ébréchée et rouillée dont seul le fil meulé brille, une chaîne enroulée sur le manche, un pommeau d\'os. La hache d\'un brigand, pas d\'un général.',
  },
};

// ---------------------------------------------------------------- Blockbench projects

const id = s => { const h = crypto.createHash('md5').update(s).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];

function elementsOf(key, style, texture, dx = 0) {
  const b = builder();
  style.build(b);
  const pal = palette(style.colours);
  return {
    pal,
    elements: b.parts.map((p, n) => {
      const row = pal.names.indexOf(p.mat);
      if (row < 0) throw new Error(key + ': no material ' + p.mat);
      const uv = [p.shade, row, p.shade + 1, row + 1];
      return {
        name: `${p.mat}_${n}`, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
        from: [p.from[0] + dx, p.from[1], p.from[2]], to: [p.to[0] + dx, p.to[1], p.to[2]], autouv: 0, color: 0,
        origin: [dx, 0, 0], rotation: [0, 0, 0],
        faces: Object.fromEntries(FACES.map(f => [f, { uv, texture }])), type: 'cube', uuid: id(key + n + '/' + dx),
      };
    }),
  };
}

const textureOf = (name, data, i) => ({
  path: '', name, folder: '', namespace: '', id: String(i), width: 16, height: 16, uv_width: 16, uv_height: 16, particle: false,
  use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', frame_time: 1,
  frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false,
  uuid: id('tex/' + name), source: 'data:image/png;base64,' + data.toString('base64'),
});

/** The axe alone. */
function axeProject(key, style) {
  const { pal, elements } = elementsOf(key, style, 0);
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: 'chief_axe_' + key, model_identifier: '',
    visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements, outliner: elements.map(e => e.uuid), textures: [textureOf(key + '.png', pal.png, 0)],
  };
}

/** The axe standing beside the chief (art/blockbench/bandit_chief.bbmodel), the blade turned away from him. */
function sizeProject(key, style) {
  const chief = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'bandit_chief.bbmodel'), 'utf8'));
  const n = chief.textures.length;
  const { pal, elements } = elementsOf(key, style, n, -16);
  // The axe's faces keep its own 16 px palette (each texture has its own UV size).
  const tex = textureOf(key + '.png', pal.png, n);
  return { ...chief, name: 'chief_with_' + key, elements: [...chief.elements, ...elements], outliner: [...chief.outliner, ...elements.map(e => e.uuid)],
    textures: [...chief.textures, tex] };
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

/** Views of the axe alone (its middle about y 22) and of the pair (the chief's middle about y 18, the axe to his right). */
const AXE_VIEWS = {
  front: [[0, 24, -64], [-2, 20, 0]], three: [[-46, 34, -46], [-2, 20, 0]], edge: [[-64, 22, 0.01], [0, 20, 0]], back: [[42, 30, 46], [-2, 20, 0]],
};
const SIZE_VIEW = [[-30, 28, -78], [-8, 19, 0]];

async function render(bb, project, views, file, size = [480, 560]) {
  // Every project closed first: a scene left from another project must not show in these views.
  const loaded = await bb.evaluate(`(async () => { while (ModelProject.all.length) await ModelProject.all[0].close(true);
    setupProject(Formats.free); Codecs.project.parse(JSON.parse(${JSON.stringify(JSON.stringify(project))}), 'p.bbmodel'); Canvas.updateAll();
    for (let i = 0; i < 40 && Outliner.elements.length < ${project.elements.length}; i++) await new Promise(r => setTimeout(r, 100));
    await new Promise(r => setTimeout(r, 200)); return Outliner.elements.length; })()`);
  if (loaded !== project.elements.length) throw new Error(`${project.name}: ${loaded} of ${project.elements.length} cubes loaded`);
  for (const [view, [pos, target]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      const p = window.__axePreview || (window.__axePreview = new Preview({ id: 'axe_render', offscreen: true, antialias: true }));
      p.resize(${size[0]}, ${size[1]});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 38 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#EDE4CF');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(file(view), Buffer.from(data.split(',')[1], 'base64'));
  }
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, axeProject(key, style), AXE_VIEWS, v => path.join(OUT, `chief-axe-${key}-${v}.png`));
    await render(bb, sizeProject(key, style), { size: SIZE_VIEW }, v => path.join(OUT, `chief-axe-${key}-${v}.png`), [560, 560]);
    console.log('rendered', key);
  }
  bb.close();
}

module.exports = { STYLES, palette, builder, textureOf, connect, render, id };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
