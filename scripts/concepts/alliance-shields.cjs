// Concept sheets for the Orthodox Alliance's shields: five Chinese shields in the Alliance's colours (azure field, gold
// and cream trim, navy ink: scripts/art/factions.cjs), each bearing its flag (a sword planted in three snowy peaks).
// Rendered from five angles and once beside a man, to docs/factions/images/alliance-shield-<style>-<view>.png. Concepts
// to choose from, not game files. Built as the spears and swords (alliance-spears.cjs): Blockbench pixels at a man's
// scale, its face toward -z (the viewer of the front view), its middle at x 0, y CY.
//
//   node scripts/concepts/alliance-shields.cjs [--only=round] [--port=9234]
const fs = require('node:fs');
const path = require('node:path');
const { connect, render } = require('./chief-axes.cjs');
const { ALLIANCE, spearProject, sizeProject } = require('./alliance-spears.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'factions', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9234);
/** The shield's middle: held at a man's forearm. */
const CY = 15, CX = -2;

// ---------------------------------------------------------------- shared parts

/** A disc of `mat` facing -z, radius r, from z0 to z1, in rows `step` tall. */
function disc(b, mat, shade, r, z0, z1, step = 0.5, cx = CX, cy = CY) {
  for (let y = -r; y < r; y += step) {
    const m = Math.min(Math.abs(y), Math.abs(y + step));
    const w = Math.sqrt(Math.max(0, r * r - m * m));
    if (w > 0.05) b.box(mat, shade, [cx - w, cy + y, z0], [cx + w, cy + y + step, z1]);
  }
}

/** A ring between r0 and r1: a disc with the middle left out, row by row. */
function ring(b, mat, shade, r0, r1, z0, z1, step = 0.5, cx = CX, cy = CY) {
  for (let y = -r1; y < r1; y += step) {
    const m = Math.min(Math.abs(y), Math.abs(y + step));
    const w1 = Math.sqrt(Math.max(0, r1 * r1 - m * m)), w0 = Math.sqrt(Math.max(0, r0 * r0 - m * m));
    if (w1 <= 0.05) continue;
    if (w0 <= 0.05 || m >= r0) b.box(mat, shade, [cx - w1, cy + y, z0], [cx + w1, cy + y + step, z1]);
    else {
      b.box(mat, shade, [cx - w1, cy + y, z0], [cx - w0, cy + y + step, z1]);
      b.box(mat, shade, [cx + w0, cy + y, z0], [cx + w1, cy + y + step, z1]);
    }
  }
}

/** The flag on the face (at z, toward -z), s its scale: three snowy peaks, a sword planted in the middle one. */
function emblem(b, z, s = 1, cx = CX, cy = CY) {
  const f = [z - 0.12, z];
  const box = (mat, shade, x0, y0, x1, y1) => b.box(mat, shade, [cx + x0 * s, cy + y0 * s, f[0]], [cx + x1 * s, cy + y1 * s, f[1]]);
  // The peaks: a wide base, the middle one tallest, snow on their tops.
  box('ice', 9, -4.0, -3.0, 4.0, -2.0);
  box('ice', 10, -3.6, -2.0, -1.4, -1.0); box('ice', 10, 1.4, -2.0, 3.6, -1.0);
  box('snow', 13, -3.1, -1.0, -1.9, -0.3); box('snow', 13, 1.9, -1.0, 3.1, -0.3);
  box('ice', 10, -1.5, -2.0, 1.5, 0.2);
  box('snow', 14, -1.0, 0.2, 1.0, 1.2);
  // The sword: a pale blade rising from the middle peak, a gold guard, a brown grip.
  box('edge', 15, -0.3, -0.6, 0.3, 3.0);
  box('gold', 12, -1.1, 3.0, 1.1, 3.5);
  box('grip', 7, -0.25, 3.5, 0.25, 4.6);
  box('gold', 11, -0.4, 4.6, 0.4, 5.0);
}

/** The grip behind (+z): a strap for the forearm and a handle. */
function strap(b, z) {
  b.box('leather', 6, [CX - 3.5, CY - 0.6, z], [CX + 3.5, CY + 0.6, z + 0.4]);
  b.box('wood', 6, [CX - 0.5, CY - 2.5, z], [CX + 0.5, CY + 2.5, z + 0.9]);
}

// ---------------------------------------------------------------- the styles

const STYLES = {
  round: {
    name: 'Azure Peak Round Shield', zh: '天峰圆盾', fr: 'Bouclier rond du Pic azur',
    colours: { ...ALLIANCE, grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // Lacquered wood in azure, a gold rim, the flag painted on it, a gold boss in the middle.
      disc(b, 'azure', 8, 7.0, -0.5, 0.5);
      ring(b, 'gold', 11, 6.4, 7.2, -0.7, 0.6);
      ring(b, 'navy', 6, 5.9, 6.4, -0.62, 0.5);
      emblem(b, -0.5, 1.05);
      // Gold studs round the rim.
      for (let k = 0; k < 12; k++) {
        const a = k * Math.PI / 6, x = CX + Math.cos(a) * 6.8, y = CY + Math.sin(a) * 6.8;
        b.box('gold', 14, [x - 0.3, y - 0.3, -0.9], [x + 0.3, y + 0.3, -0.7]);
      }
      strap(b, 0.5);
    },
    text: "Le bouclier rond du soldat : bois laqué d'azur, bordé d'or et clouté, le drapeau de l'Alliance peint au milieu (les trois pics enneigés, l'épée plantée). Le plus simple et le plus lisible : il dit tout de suite à qui l'on a affaire.",
  },
  rattan: {
    name: 'Rattan Shield', zh: '藤牌', fr: 'Bouclier de rotin',
    colours: { ...ALLIANCE, rattan: '#c9a96a', rattanDark: '#8f7240', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // Rattan coiled into a dome: rings stepping forward to the middle, light and dark turns.
      for (let i = 0; i < 7; i++) {
        const r1 = 7.0 - i, r0 = Math.max(0, r1 - 1);
        const z = -0.4 - i * 0.45;
        if (r0 > 0) ring(b, i % 2 ? 'rattanDark' : 'rattan', 9, r0, r1, z, 0.4);
        else disc(b, 'rattan', 10, r1, z, 0.4);
      }
      // An azure disc painted on the dome's top with the flag; a blue cord round the edge.
      disc(b, 'azure', 8, 2.6, -3.6, -3.1);
      emblem(b, -3.6, 0.55);
      ring(b, 'navy', 7, 6.8, 7.3, -0.6, 0.45);
      strap(b, 0.4);
    },
    text: "Le tengpai (藤牌), le bouclier de rotin tressé en dôme des fantassins des Ming, léger et souple, qui fait glisser les lames. Rotin clair et foncé en spirale, une cordelette bleue au bord, et au sommet du dôme un médaillon azur aux trois pics. Pour une troupe rapide, avec le sabre.",
  },
  tower: {
    name: 'Citadel Tower Shield', zh: '天峰长牌', fr: 'Grand pavois de la Citadelle',
    colours: { ...ALLIANCE, grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // A tall plank shield, a little narrower at the foot, azure with a gold frame and a navy band.
      const rows = [[5.0, -11, -7], [5.4, -7, 9], [5.0, 9, 11]];
      for (const [hw, y0, y1] of rows) b.box('azure', 8, [CX - hw, CY + y0, -0.5], [CX + hw, CY + y1, 0.5]);
      b.box('gold', 11, [CX - 5.6, CY + 9, -0.7], [CX + 5.6, CY + 9.8, 0.6]);
      b.box('gold', 11, [CX - 5.2, CY - 11, -0.7], [CX + 5.2, CY - 10.2, 0.6]);
      b.box('gold', 11, [CX - 5.6, CY - 7, -0.7], [CX - 4.9, CY + 9, 0.6]);
      b.box('gold', 11, [CX + 4.9, CY - 7, -0.7], [CX + 5.6, CY + 9, 0.6]);
      b.box('navy', 6, [CX - 4.9, CY - 7.6, -0.6], [CX + 4.9, CY - 6.6, 0.5]);
      // A pointed top like a gable, the flag large in the middle, three gold studs at the foot.
      b.box('gold', 12, [CX - 3.6, CY + 9.8, -0.6], [CX + 3.6, CY + 10.6, 0.5]);
      b.box('gold', 13, [CX - 1.8, CY + 10.6, -0.55], [CX + 1.8, CY + 11.4, 0.45]);
      b.box('gold', 14, [CX - 0.6, CY + 11.4, -0.5], [CX + 0.6, CY + 12.2, 0.4]);
      emblem(b, -0.5, 1.15, CX, CY + 1.5);
      for (const x of [-3, 0, 3]) b.box('gold', 14, [CX + x - 0.35, CY - 9.2, -0.85], [CX + x + 0.35, CY - 8.5, -0.6]);
      strap(b, 0.5);
    },
    text: "Le changpai (长牌), le grand pavois de bois des gardes de la Citadelle du Pic céleste, aussi haut qu'un homme jusqu'à l'épaule : azur cadré d'or, un fronton en pignon, le drapeau en grand. Lent mais imprenable : la muraille de l'Alliance, pour les gardes aux portes.",
  },
  buckler: {
    name: 'Peak Bronze Buckler', zh: '铜峰盾', fr: 'Rondache de bronze des Pics',
    colours: { ...ALLIANCE, bronze: '#c39a3c', bronzeDark: '#8a6a28', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // A small round of bronze, a raised rim, the three peaks cast in relief, a long azure tassel hanging from its foot.
      disc(b, 'bronze', 9, 4.6, -0.4, 0.4);
      ring(b, 'bronzeDark', 7, 4.1, 4.8, -0.7, 0.45);
      emblem(b, -0.4, 0.75);
      b.box('gold', 11, [CX - 0.5, CY - 5.3, -0.4], [CX + 0.5, CY - 4.6, 0.2]);
      for (const x of [-0.35, 0, 0.35]) b.box('azure', 7, [CX + x - 0.15, CY - 9.5, -0.25], [CX + x + 0.15, CY - 5.3, 0.05]);
      strap(b, 0.4);
    },
    text: "Une petite rondache de bronze pour les disciples qui se battent à l'épée et au bouclier : légère, pour parer et frapper du bord. Les trois pics coulés en relief, un rebord épais, une longue houppe azur au pied qui se balancerait comme celle des épées.",
  },
  lion: {
    name: 'Guardian Lion Shield', zh: '狮头牌', fr: 'Bouclier à tête de lion gardien',
    colours: { ...ALLIANCE, mane: '#b08a3a', face: '#E9D9A6', red: '#a8322a', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220', dark: '#14284A' },
    build(b) {
      // A round azure shield, its rim gold, and on it a guardian lion's face in gold (the lions at the Citadel's gate).
      disc(b, 'azure', 8, 7.0, -0.5, 0.5);
      ring(b, 'gold', 11, 6.4, 7.2, -0.7, 0.6);
      // The mane: rays of gold round the face.
      ring(b, 'mane', 9, 3.4, 5.6, -0.75, -0.5);
      for (let k = 0; k < 12; k++) {
        const a = k * Math.PI / 6 + Math.PI / 12, x = CX + Math.cos(a) * 5.6, y = CY + Math.sin(a) * 5.6;
        b.box('mane', 11, [x - 0.4, y - 0.4, -0.85], [x + 0.4, y + 0.4, -0.6]);
      }
      disc(b, 'face', 12, 3.4, -0.95, -0.5);
      const f = (mat, shade, x0, y0, x1, y1, d = 0.15) => b.box(mat, shade, [CX + x0, CY + y0, -1.0 - d], [CX + x1, CY + y1, -0.95]);
      // Brows, round eyes, a broad nose, an open mouth with teeth, and the three peaks on its forehead.
      f('gold', 13, -2.4, 1.0, -0.6, 1.5); f('gold', 13, 0.6, 1.0, 2.4, 1.5);
      f('dark', 5, -2.0, 0.0, -0.9, 0.9); f('dark', 5, 0.9, 0.0, 2.0, 0.9);
      f('snow', 15, -1.75, 0.45, -1.35, 0.8, 0.2); f('snow', 15, 1.15, 0.45, 1.55, 0.8, 0.2);
      f('mane', 10, -0.8, -1.2, 0.8, 0.2, 0.3);
      f('red', 8, -1.8, -2.8, 1.8, -1.6);
      for (const x of [-1.5, -0.5, 0.5, 1.5]) f('snow', 15, x - 0.3, -1.95, x + 0.3, -1.6, 0.2);
      f('ice', 10, -1.2, 1.8, 1.2, 2.2); f('snow', 14, -0.3, 2.2, 0.3, 2.9); f('snow', 13, -1.0, 2.2, -0.6, 2.5); f('snow', 13, 0.6, 2.2, 1.0, 2.5);
      strap(b, 0.5);
    },
    text: "Le shitoupai (狮头牌), le bouclier à tête de lion gardien : la face dorée d'un des lions qui gardent la porte de la Citadelle, crinière en rayons, gueule ouverte, et les trois pics sur son front. Le plus impressionnant : pour les gardes d'honneur ou un officier.",
  },
};

// ---------------------------------------------------------------- rendering

const VIEWS = {
  front: [[CX, CY + 2, -46], [CX, CY, 0]], three: [[CX - 32, CY + 10, -32], [CX, CY, 0]], side: [[CX - 46, CY + 2, 0.01], [CX, CY, 0]],
  back: [[CX + 30, CY + 8, 34], [CX, CY, 0]], detail: [[CX - 6, CY + 3, -20], [CX, CY + 0.5, 0]],
};

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, spearProject('shield_' + key, style), VIEWS, v => path.join(OUT, `alliance-shield-${key}-${v}.png`), [480, 520]);
    await render(bb, sizeProject('shield_' + key, style), { size: [[-24, 20, -76], [-8, 16, 0]] }, v => path.join(OUT, `alliance-shield-${key}-${v}.png`), [480, 520]);
    console.log('rendered', key);
  }
  bb.close();
}

module.exports = { STYLES };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
