// Concept sheets for the Overlord Spear Art's own spear (霸王枪, docs/combat/styles/overlord-spear.md): three styles in
// the art's colours (black iron, blood red, gold), each after its brute, destructive power. Rendered from five angles
// and once beside a man for its size, to docs/combat/styles/images/overlord-spear-<style>-<view>.png.
// Concepts to choose from, not game files. Built as alliance-spears.cjs (same helpers, same scale).
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9244 --user-data-dir=<empty profile>)
//   node scripts/concepts/overlord-spears.cjs [--only=tyrant,cauldron] [--port=9244]
const fs = require('node:fs');
const path = require('node:path');
const { connect, render } = require('./chief-axes.cjs');
const { spearProject, sizeProject } = require('./alliance-spears.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9244);

/** The art's colours. */
const OVERLORD = {
  iron: '#2a2a2e', blood: '#9e1a1a', gold: '#D9A83F', steel: '#a4abb2', edge: '#eef1f4', bronze: '#7a5a2e',
  ember: '#ff5a1e', hair: '#b81c1c',
};

const shaft = (b, mat, y0, y1, w = 1.6, shade = 6) => b.box(mat, shade, [-w / 2, y0, -w / 2], [w / 2, y1, w / 2]);
const band = (b, mat, y, h = 0.8, w = 2.1, shade = 11) => b.box(mat, shade, [-w / 2, y, -w / 2], [w / 2, y + h, w / 2]);

/** A thick mane of horsehair hanging from y. */
function mane(b, y, len = 6.5) {
  b.box('blood', 6, [-1.3, y - 1.2, -1.3], [1.3, y, 1.3]);
  const at = [[-1.4, 0], [1.4, 0], [0, -1.4], [0, 1.4], [-1, -1], [1, 1], [1, -1], [-1, 1], [-0.5, 1.3], [0.5, -1.3], [1.3, 0.5], [-1.3, -0.5]];
  at.forEach(([x, z], i) => b.box('hair', 6 + (i % 5), [x - 0.35, y - 1.2 - len * (0.7 + 0.08 * (i % 4)), z - 0.35], [x + 0.35, y - 1.2, z + 0.35]));
}

/** A taotie, the beast mask of the old bronzes, on both faces of a socket at y: brows, eyes, fangs. */
function taotie(b, y, face = 1.15) {
  for (const s of [-1, 1]) {
    const [za, zb] = s < 0 ? [-face - 0.15, -face] : [face, face + 0.15];
    b.box('gold', 12, [-1.2, y + 2.2, za], [1.2, y + 2.7, zb]);              // the brow
    b.box('blood', 13, [-0.9, y + 1.4, za], [-0.3, y + 1.9, zb]);             // the eyes
    b.box('blood', 13, [0.3, y + 1.4, za], [0.9, y + 1.9, zb]);
    b.box('gold', 10, [-0.2, y + 0.6, za], [0.2, y + 2.2, zb]);               // the nose ridge
    b.box('gold', 13, [-0.9, y, za], [-0.6, y + 0.8, zb]);                    // the fangs
    b.box('gold', 13, [0.6, y, za], [0.9, y + 0.8, zb]);
  }
}

const STYLES = {
  tyrant: {
    name: "Tyrant's Spear", zh: '霸王枪', fr: 'Lance du Tyran',
    build(b) {
      // A spiked butt, a black iron shaft banded in gold, the grip wrapped in red.
      b.box('gold', 12, [-0.4, -3.2, -0.4], [0.4, -2, 0.4]);
      b.box('iron', 8, [-1.1, -2, -1.1], [1.1, 0, 1.1]);
      shaft(b, 'iron', 0, 40);
      b.box('blood', 7, [-0.95, 14, -0.95], [0.95, 22, 0.95]);
      for (const y of [6, 13.4, 22, 32]) band(b, 'gold', y, 0.6);
      mane(b, 40.5);
      // A socket cast as a taotie mask, gold collars.
      band(b, 'gold', 39.6, 0.9, 2.5);
      b.box('iron', 9, [-1.15, 40.5, -1.15], [1.15, 43.5, 1.15]);
      taotie(b, 40.6);
      band(b, 'gold', 43.5, 0.7, 2.4);
      // A broad, thick head with a blood groove down its middle.
      const widths = [2.0, 3.2, 3.8, 4.0, 3.9, 3.5, 3.0, 2.4, 1.7, 1.0], row = 1.15, t = 0.7;
      widths.forEach((w, i) => {
        const y = 44.2 + i * row, e = Math.min(0.5, w / 3);
        b.box('steel', 5 + Math.round(4 * i / widths.length), [-w / 2 + e, y, -t / 2], [w / 2 - e, y + row, t / 2]);
        b.box('edge', 14, [-w / 2, y, -t / 3], [-w / 2 + e, y + row, t / 3]);
        b.box('edge', 14, [w / 2 - e, y, -t / 3], [w / 2, y + row, t / 3]);
      });
      b.box('blood', 9, [-0.25, 44.6, -0.4], [0.25, 53.5, 0.4]);
      b.box('edge', 15, [-0.3, 55.7, -0.2], [0.3, 56.8, 0.2]);
    },
    text: "La lance du conquérant à l'état pur : hampe de fer noir cerclée d'or, poignée gainée de rouge, douille coulée en masque de taotie (la bête des bronzes antiques, yeux de sang), crinière rouge épaisse, fer large et épais creusé d'une gouttière de sang, talon à pointe. Lourde, massive, faite pour enfoncer.",
  },
  cauldron: {
    name: 'Cauldron-Breaker Halberd', zh: '破釜戟', fr: 'Hallebarde Brise-Chaudron',
    build(b) {
      b.box('bronze', 10, [-1.1, -2.4, -1.1], [1.1, 0, 1.1]);
      shaft(b, 'iron', 0, 41, 1.6, 5);
      for (const y of [5, 12, 26, 34]) band(b, 'bronze', y, 0.9, 2.2, 9);
      b.box('blood', 7, [-0.95, 13, -0.95], [0.95, 25.5, 0.95]);
      mane(b, 41.5, 5.5);
      // A bronze socket like the leg of a ding cauldron, its rim cracked open.
      band(b, 'gold', 40.6, 0.9, 2.6);
      b.box('bronze', 11, [-1.25, 41.5, -1.25], [1.25, 44.5, 1.25]);
      taotie(b, 41.7, 1.25);
      // The ji: a straight spear head and, beside it, a crescent axe blade with a hook behind.
      b.box('steel', 8, [-0.8, 44.5, -0.3], [0.8, 54, 0.3]);
      b.box('edge', 14, [-0.8, 44.5, -0.2], [-0.5, 54, 0.2]);
      b.box('edge', 14, [0.5, 44.5, -0.2], [0.8, 54, 0.2]);
      b.box('edge', 15, [-0.5, 54, -0.2], [0.5, 55.4, 0.2]);
      b.box('edge', 15, [-0.2, 55.4, -0.15], [0.2, 56.2, 0.15]);
      // The crescent, rows stepping outward then back (the moon's curve), its edge bright, a blood ember at its heart.
      [[1.0, 2.0], [1.0, 3.2], [1.0, 4.0], [1.0, 4.5], [1.0, 4.6], [1.0, 4.4], [1.0, 3.8], [1.0, 2.8]].forEach(([x0, x1], i) => {
        const y = 45.0 + i * 0.9;
        b.box('steel', 7, [x0, y, -0.28], [x1 - 0.4, y + 0.9, 0.28]);
        b.box('edge', 14, [x1 - 0.4, y, -0.2], [x1, y + 0.9, 0.2]);
      });
      b.box('blood', 12, [2.0, 47.4, -0.32], [2.8, 49.2, 0.32]);
      // The hook behind, curving down.
      b.box('steel', 8, [-2.6, 47, -0.25], [-0.8, 47.8, 0.25]);
      b.box('edge', 14, [-3.1, 45.6, -0.2], [-2.4, 47.8, 0.2]);
    },
    text: "La hallebarde ji des généraux de l'Antiquité, en l'honneur du « briser les chaudrons » de Xiang Yu : une pointe droite, une lame en croissant pour fendre et un crochet pour arracher, une douille de bronze en pied de chaudron ding au masque de taotie. Plus de coupe et de balayage, idéale pour Earth-Splitting Sweep.",
  },
  ruin: {
    name: 'Spear of Ruin', zh: '灭世枪', fr: 'Lance de la Ruine',
    build(b) {
      b.box('gold', 12, [-0.5, -3, -0.5], [0.5, -1.6, 0.5]);
      b.box('iron', 7, [-1.0, -1.6, -1.0], [1.0, 0, 1.0]);
      shaft(b, 'iron', 0, 40, 1.6, 4);
      // Cracks of ember running up the black shaft, as if the Qi inside burst it.
      for (const [y, x, z] of [[4, -0.81, 0], [9, 0, 0.81], [17, 0.81, 0], [25, 0, -0.81], [31, -0.81, 0], [36, 0, 0.81]]) {
        b.box('ember', 15, [x - (z ? 0.3 : 0.06), y, z - (x ? 0.3 : 0.06)], [x + (z ? 0.3 : 0.06), y + 2.2, z + (x ? 0.3 : 0.06)]);
      }
      for (const y of [12, 28]) band(b, 'gold', y, 0.6);
      mane(b, 40.5, 6);
      band(b, 'gold', 39.6, 0.9, 2.6);
      // Three gold claws grip the head.
      b.box('iron', 8, [-1.1, 40.5, -1.1], [1.1, 43, 1.1]);
      for (const s of [-1, 1]) b.box('gold', 12, [s * 1.1 - 0.3, 41.5, -0.35], [s * 1.1 + 0.3 + s * 0.4, 45.2, 0.35]);
      // A long, wide head of black steel split by a glowing seam, edges of gold-white.
      const widths = [2.4, 3.4, 3.8, 3.8, 3.6, 3.3, 2.9, 2.4, 1.9, 1.3, 0.7], row = 1.2, t = 0.65;
      widths.forEach((w, i) => {
        const y = 43 + i * row, e = Math.min(0.45, w / 3);
        b.box('iron', 9 + Math.round(3 * i / widths.length), [-w / 2 + e, y, -t / 2], [w / 2 - e, y + row, t / 2]);
        b.box('edge', 15, [-w / 2, y, -t / 3], [-w / 2 + e, y + row, t / 3]);
        b.box('edge', 15, [w / 2 - e, y, -t / 3], [w / 2, y + row, t / 3]);
      });
      b.box('ember', 15, [-0.2, 43.4, -0.36], [0.2, 54.5, 0.36]);
      b.box('ember', 14, [-0.9, 46, -0.35], [-0.2, 46.4, 0.35]);
      b.box('ember', 14, [0.2, 49, -0.35], [1.0, 49.4, 0.35]);
      b.box('edge', 15, [-0.25, 56.2, -0.18], [0.25, 57.2, 0.18]);
    },
    text: "La puissance destructrice rendue visible : fer et hampe d'acier noir fendus de veines de Qi incandescentes, comme si la force à l'intérieur allait les faire éclater ; trois griffes d'or tiennent le fer, crinière rouge. La plus « spectaculaire » : ses fissures peuvent briller dans le noir et s'embraser pendant les techniques.",
  },
};
for (const s of Object.values(STYLES)) s.colours = { ...OVERLORD };

const VIEWS = { front: [[0, 28, -118], [0, 26, 0]], three: [[-80, 40, -80], [0, 26, 0]],
  head: [[0, 49, -34], [0, 48, 0]], head_side: [[-34, 49, 0.01], [0, 48, 0]], head_three: [[26, 53, -26], [0, 48, 0]] };
const SIZE_VIEW = [[-28, 30, -96], [-6, 24, 0]];

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, spearProject(key, style), VIEWS, v => path.join(OUT, `overlord-spear-${key}-${v}.png`), [420, 600]);
    await render(bb, sizeProject(key, style), { size: SIZE_VIEW }, v => path.join(OUT, `overlord-spear-${key}-${v}.png`), [480, 600]);
    console.log('rendered', key);
  }
  bb.close();
}

if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
module.exports = { STYLES, OVERLORD };
