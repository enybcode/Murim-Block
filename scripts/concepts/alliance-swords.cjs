// Concept sheets for the Orthodox Alliance's sword: five styles in the Alliance's colours (azure field, gold and cream
// trim, navy ink: scripts/art/factions.cjs), each with a nod to its flag (a sword planted in three snowy peaks). Rendered
// from five angles and once beside a man, to docs/factions/images/alliance-sword-<style>-<view>.png. Concepts to choose
// from, not game files. Built as the spears (scripts/concepts/alliance-spears.cjs): Blockbench pixels at a man's scale,
// the pommel at y 0, the blade up, its flat facing z.
//
//   node scripts/concepts/alliance-swords.cjs [--only=peak_jian] [--port=9234]
const fs = require('node:fs');
const path = require('node:path');
const { connect, render } = require('./chief-axes.cjs');
const { ALLIANCE, tassel, spearProject, sizeProject } = require('./alliance-spears.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'factions', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9234);

// ---------------------------------------------------------------- shared parts

/**
 * A straight double-edged blade from y0 to y1, w wide, its last rows narrowing to a point: a flat steel body, bright
 * edges, a ridge (or a fuller) of `ridge` down the middle.
 */
function straight(b, y0, y1, w, ridge = 'ice', t = 0.4) {
  const e = 0.3, tip = Math.min(3, w * 1.4), body = y1 - tip;
  b.box('steel', 8, [-w / 2 + e, y0, -t / 2], [w / 2 - e, body, t / 2]);
  b.box('edge', 14, [-w / 2, y0, -t / 3], [-w / 2 + e, body, t / 3]);
  b.box('edge', 14, [w / 2 - e, y0, -t / 3], [w / 2, body, t / 3]);
  b.box(ridge, 9, [-0.15, y0, -t / 2 - 0.05], [0.15, body, t / 2 + 0.05]);
  // The point, in narrowing steps.
  const steps = 4;
  for (let i = 0; i < steps; i++) {
    const ww = w * (1 - (i + 1) / (steps + 1)), y = body + i * tip / steps;
    b.box(i === steps - 1 ? 'edge' : 'steel', i === steps - 1 ? 15 : 10, [-ww / 2, y, -t / 3], [ww / 2, y + tip / steps, t / 3]);
    b.box('edge', 14, [-ww / 2, y, -t / 4], [-ww / 2 + 0.25, y + tip / steps, t / 4]);
    b.box('edge', 14, [ww / 2 - 0.25, y, -t / 4], [ww / 2, y + tip / steps, t / 4]);
  }
}

/** A grip from y0 to y1 wrapped in `mat`, a dark turn every `step`. */
function grip(b, mat, turn, y0, y1, w = 1.1, step = 0.9) {
  b.box(mat, 8, [-w / 2, y0, -w / 2], [w / 2, y1, w / 2]);
  for (let y = y0 + step / 2; y < y1 - 0.2; y += step) b.box(turn, 5, [-w / 2 - 0.05, y, -w / 2 - 0.05], [w / 2 + 0.05, y + 0.25, w / 2 + 0.05]);
}

/** The flag's three peaks as small inlays on both faces of a blade or a disc at (x, y), z ± face. */
function peakInlay(b, x, y, face, s = 1) {
  for (const sign of [-1, 1]) {
    const [za, zb] = sign < 0 ? [-face - 0.06, -face] : [face, face + 0.06];
    b.box('ice', 9, [x - 1.0 * s, y, za], [x + 1.0 * s, y + 0.5 * s, zb]);
    b.box('snow', 13, [x - 0.9 * s, y + 0.5 * s, za], [x - 0.4 * s, y + 0.9 * s, zb]);
    b.box('snow', 13, [x + 0.4 * s, y + 0.5 * s, za], [x + 0.9 * s, y + 0.9 * s, zb]);
    b.box('snow', 14, [x - 0.3 * s, y + 0.5 * s, za], [x + 0.3 * s, y + 1.4 * s, zb]);
  }
}

// ---------------------------------------------------------------- the styles

const STYLES = {
  peak_jian: {
    name: 'Peak Guard Jian', zh: '天峰剑', fr: 'Jian à garde des Pics',
    colours: { ...ALLIANCE },
    build(b) {
      // A gold pommel, an azure grip wound in navy, a blue tassel from the pommel.
      b.box('gold', 10, [-0.9, 0, -0.75], [0.9, 1.2, 0.75]);
      b.box('gold', 13, [-0.45, -0.4, -0.4], [0.45, 0, 0.4]);
      grip(b, 'azure', 'navy', 1.2, 5.6);
      tassel(b, 'azure', 0.0, 3.2, 0.7);
      // The guard: the three peaks in gold, snow on their tops, the blade rising out of the middle one: the flag itself.
      b.box('gold', 9, [-2.8, 5.6, -0.7], [2.8, 6.3, 0.7]);
      for (const s of [-1, 1]) {
        b.box('gold', 11, [s * 2.0 - 0.75, 6.3, -0.6], [s * 2.0 + 0.75, 7.1, 0.6]);
        b.box('snow', 14, [s * 2.0 - 0.35, 7.1, -0.5], [s * 2.0 + 0.35, 7.5, 0.5]);
      }
      b.box('gold', 12, [-1.0, 6.3, -0.65], [1.0, 7.8, 0.65]);
      b.box('snow', 14, [-0.6, 7.8, -0.55], [0.6, 8.2, 0.55]);
      straight(b, 8.2, 24.5, 1.7);
    },
    text: "Le jian (épée droite à deux tranchants) du disciple de l'Alliance. Sa garde est le drapeau : trois pics d'or coiffés de neige, la lame sort du pic du milieu, l'épée plantée dans les montagnes. Poignée azur filée de marine, pommeau d'or, houppe bleue.",
  },
  banner_sword: {
    name: 'Banner Sword', zh: '盟旗剑', fr: 'Épée du drapeau',
    colours: { ...ALLIANCE, grip: '#6B3E26', pale: '#DCE4EC' },
    build(b) {
      // Exactly the flag's sword: a broad pale blade, a straight gold bar, a brown grip, a gold pommel.
      b.box('gold', 10, [-1.1, 0, -0.8], [1.1, 1.3, 0.8]);
      b.box('gold', 13, [-0.5, -0.5, -0.45], [0.5, 0, 0.45]);
      grip(b, 'grip', 'navy', 1.3, 5.8, 1.15, 1.1);
      b.box('gold', 11, [-3.4, 5.8, -0.8], [3.4, 6.8, 0.8]);
      b.box('gold', 9, [-3.9, 5.6, -0.6], [-3.4, 7.0, 0.6]);
      b.box('gold', 9, [3.4, 5.6, -0.6], [3.9, 7.0, 0.6]);
      straight(b, 6.8, 25, 2.4, 'ice', 0.45);
      // A blued fuller near the guard, the three peaks inlaid in it.
      b.box('azure', 8, [-0.6, 7.2, -0.27], [0.6, 12.4, 0.27]);
      peakInlay(b, 0, 7.8, 0.27, 0.55);
    },
    text: "L'épée du drapeau, telle qu'elle y est dessinée : large lame claire, barre de garde droite en or, poignée brune, pommeau d'or. Une gouttière bleuie près de la garde porte les trois pics incrustés. La plus reconnaissable : chacun saura d'où vient son porteur.",
  },
  soldier_dao: {
    name: 'Alliance Dao', zh: '盟刀', fr: "Dao de l'Alliance",
    colours: { ...ALLIANCE, cream: '#E9D9A6' },
    build(b) {
      // A gold ring pommel, a navy grip, a cream tassel.
      b.box('gold', 11, [-0.9, -0.6, -0.3], [0.9, -0.2, 0.3]);
      b.box('gold', 11, [-0.9, -0.6, -0.3], [-0.5, 1.0, 0.3]);
      b.box('gold', 11, [0.5, -0.6, -0.3], [0.9, 1.0, 0.3]);
      b.box('gold', 10, [-0.8, 1.0, -0.7], [0.8, 1.6, 0.7]);
      grip(b, 'navy', 'gold', 1.6, 6.0, 1.15, 1.1);
      tassel(b, 'cream', -0.6, 3.0, 0.6);
      // A round guard of azure enamel, rimmed in gold, the three peaks on it.
      b.box('gold', 10, [-2.3, 6.0, -2.3], [2.3, 6.4, 2.3]);
      b.box('azure', 8, [-2.0, 6.4, -2.0], [2.0, 6.9, 2.0]);
      b.box('gold', 12, [-0.8, 6.9, -0.6], [0.8, 7.5, 0.6]);
      // The blade: single-edged, its back straight (+x), widening a little toward a clipped point, the edge on -x.
      const rows = [1.7, 1.8, 1.9, 2.0, 2.15, 2.3, 2.4];
      rows.forEach((w, i) => {
        const y = 7.5 + i * 2.2, back = 0.7;
        b.box('steel', 7 + i % 2, [back - w + 0.35, y, -0.22], [back, y + 2.2, 0.22]);
        b.box('edge', 14, [back - w, y, -0.14], [back - w + 0.35, y + 2.2, 0.14]);
        b.box('steel', 5, [back - 0.3, y, -0.28], [back, y + 2.2, 0.28]);
      });
      const top = 7.5 + rows.length * 2.2;
      b.box('steel', 9, [-1.3, top, -0.2], [0.7, top + 1.0, 0.2]);
      b.box('edge', 15, [-0.6, top + 1.0, -0.16], [0.7, top + 1.8, 0.16]);
      b.box('edge', 15, [0.1, top + 1.8, -0.12], [0.7, top + 2.4, 0.12]);
      peakInlay(b, -0.4, 8.4, 0.22, 0.5);
    },
    text: "Le sabre (dao) des soldats de l'Alliance, le pendant propre du dao des bandits : lame à un tranchant au dos droit, pointe coupée, les trois pics gravés près de la garde. Garde ronde émaillée d'azur cerclée d'or, poignée marine, pommeau en anneau d'or, houppe crème. L'arme la plus courante de la troupe.",
  },
  cloud_longsword: {
    name: 'Cloud Sea Longsword', zh: '云海长剑', fr: 'Longue épée de la Mer de nuages',
    colours: { ...ALLIANCE, cream: '#E9D9A6', blued: '#3c5f8f' },
    build(b) {
      // A long two-handed grip wrapped in cream, a gold pommel.
      b.box('gold', 10, [-1.0, 0, -0.8], [1.0, 1.2, 0.8]);
      grip(b, 'cream', 'navy', 1.2, 8.4, 1.15, 1.0);
      // A gold guard like a cloud: two wings curling up at their ends.
      b.box('gold', 11, [-3.0, 8.4, -0.7], [3.0, 9.2, 0.7]);
      for (const s of [-1, 1]) {
        b.box('gold', 10, [s * 3.0 - (s > 0 ? 0 : 0.8), 8.4, -0.6], [s * 3.0 + (s > 0 ? 0.8 : 0), 9.8, 0.6]);
        b.box('gold', 12, [s * 2.6 - 0.4, 9.8, -0.5], [s * 2.6 + 0.4, 10.4, 0.5]);
      }
      straight(b, 9.2, 34, 1.9, 'blued', 0.42);
      peakInlay(b, 0, 10.0, 0.25, 0.5);
    },
    text: "Une longue épée à deux mains pour les disciples d'élite et les gardes de la Grande Salle : poignée crème longue de deux mains, garde d'or en nuage aux ailes recourbées, lame droite d'1,5 bloc à la gouttière bleuie et aux trois pics incrustés. La mer de nuages des Pics, en épée.",
  },
  oath_jian: {
    name: 'Oath Jian', zh: '盟誓剑', fr: 'Jian du Serment',
    colours: { ...ALLIANCE, ivory: '#F2EBDA', jade: '#3f8f6e' },
    build(b) {
      // An ivory lacquered grip banded in gold, a gold pommel with a jade bead and a cream tassel.
      b.box('gold', 11, [-0.8, 0, -0.8], [0.8, 1.0, 0.8]);
      b.box('jade', 10, [-0.45, -0.7, -0.45], [0.45, 0, 0.45]);
      grip(b, 'ivory', 'gold', 1.0, 5.6, 1.1, 1.5);
      tassel(b, 'cream', -0.7, 3.4, 0.6);
      // The guard: the Alliance's seal, an azure plaque rimmed in gold, the peaks and the sword on it, upright.
      b.box('gold', 10, [-2.2, 5.6, -0.55], [2.2, 9.4, 0.55]);
      b.box('azure', 8, [-1.8, 5.95, -0.6], [1.8, 9.05, 0.6]);
      for (const sign of [-1, 1]) {
        const [za, zb] = sign < 0 ? [-0.68, -0.6] : [0.6, 0.68];
        b.box('ice', 9, [-1.4, 6.3, za], [1.4, 6.9, zb]);
        b.box('snow', 13, [-0.5, 6.9, za], [0.5, 7.9, zb]);
        b.box('snow', 13, [-1.3, 6.9, za], [-0.7, 7.4, zb]);
        b.box('snow', 13, [0.7, 6.9, za], [1.3, 7.4, zb]);
        b.box('edge', 15, [-0.15, 7.4, za], [0.15, 8.7, zb]);
        b.box('gold', 12, [-0.5, 8.3, za], [0.5, 8.5, zb]);
      }
      straight(b, 9.4, 26, 1.6, 'gold', 0.38);
    },
    text: "Le jian d'apparat des officiers, sur lequel on prête serment en entrant dans l'Alliance : la garde est le sceau de la faction, une plaque azur cerclée d'or où l'on voit les pics et l'épée. Poignée laquée ivoire baguée d'or, perle de jade et houppe crème au pommeau, filet d'or le long de la lame.",
  },
};

// ---------------------------------------------------------------- rendering

function viewsOf() {
  return {
    front: [[0, 15, -64], [0, 14, 0]], three: [[-44, 22, -44], [0, 14, 0]], edge: [[-64, 15, 0.01], [0, 14, 0]],
    guard: [[0, 9, -22], [0, 8, 0]], guard_three: [[16, 13, -16], [0, 8, 0]],
  };
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, spearProject('sword_' + key, style), viewsOf(style), v => path.join(OUT, `alliance-sword-${key}-${v}.png`), [420, 560]);
    await render(bb, sizeProject('sword_' + key, style), { size: [[-26, 22, -80], [-6, 17, 0]] }, v => path.join(OUT, `alliance-sword-${key}-${v}.png`), [480, 520]);
    console.log('rendered', key);
  }
  bb.close();
}

module.exports = { STYLES };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
