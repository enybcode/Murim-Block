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
  // ---- A second series, closer to the Murim: slender long blades, small guards bent toward the blade, long tassels.
  snow_plum_jian: {
    name: 'Snow Plum Jian', zh: '雪梅剑', fr: 'Jian du Prunier sous la neige',
    colours: { ...ALLIANCE, plum: '#F4F7FB' },
    build(b) {
      // A pommel cap, a grip of azure cord, a long two-strand tassel.
      b.box('gold', 10, [-0.7, 0, -0.6], [0.7, 0.9, 0.6]);
      b.box('gold', 12, [-0.35, -0.3, -0.3], [0.35, 0, 0.3]);
      grip(b, 'azure', 'navy', 0.9, 5.6, 0.95, 0.7);
      for (const x of [-0.25, 0.25]) b.box('azure', 7, [x - 0.18, -6.0, -0.18], [x + 0.18, -0.3, 0.18]);
      b.box('gold', 11, [-0.4, -1.4, -0.4], [0.4, -0.9, 0.4]);
      // The guard: small, its two wings bent up toward the blade, as a jian's are.
      b.box('gold', 10, [-1.0, 5.6, -0.6], [1.0, 6.3, 0.6]);
      for (const s of [-1, 1]) {
        b.box('gold', 11, [s * 1.0 - (s > 0 ? 0 : 0.7), 5.9, -0.5], [s * 1.0 + (s > 0 ? 0.7 : 0), 6.8, 0.5]);
        b.box('gold', 12, [s * 1.45 - 0.25, 6.8, -0.4], [s * 1.45 + 0.25, 7.3, 0.4]);
      }
      straight(b, 6.3, 26.5, 1.3, 'ice', 0.32);
      // Plum blossoms climbing the blade's flat, white on the steel, the three peaks under them.
      peakInlay(b, 0, 7.0, 0.17, 0.4);
      for (const [x, y] of [[0.2, 9.2], [-0.25, 10.6], [0.15, 12.3]]) {
        for (const sign of [-1, 1]) {
          const [za, zb] = sign < 0 ? [-0.22, -0.16] : [0.16, 0.22];
          b.box('plum', 15, [x - 0.3, y - 0.1, za], [x + 0.3, y + 0.1, zb]);
          b.box('plum', 15, [x - 0.1, y - 0.3, za], [x + 0.1, y + 0.3, zb]);
          b.box('gold', 12, [x - 0.06, y - 0.06, sign < 0 ? za - 0.02 : zb], [x + 0.06, y + 0.06, sign < 0 ? za : zb + 0.02]);
        }
      }
    },
    text: "Un jian long et fin comme ceux des grandes sectes : petite garde aux ailes relevées vers la lame, poignée de cordelette azur, longue houppe à deux brins. Sur le plat, trois fleurs de prunier blanches montent au-dessus des trois pics : le prunier qui fleurit sous la neige des sommets.",
  },
  ruyi_jian: {
    name: 'Ruyi Cloud Jian', zh: '如意云剑', fr: 'Jian aux nuages ruyi',
    colours: { ...ALLIANCE, jade: '#3f8f6e', ray: '#c9d3df' },
    build(b) {
      // A ruyi-head pommel, a grip of pale ray skin bound in navy, a cream tassel with a jade bead.
      b.box('gold', 10, [-0.9, 0.2, -0.6], [0.9, 1.0, 0.6]);
      b.box('gold', 12, [-0.6, -0.3, -0.5], [0.6, 0.2, 0.5]);
      b.box('gold', 11, [-1.1, 0.6, -0.5], [-0.8, 1.2, 0.5]);
      b.box('gold', 11, [0.8, 0.6, -0.5], [1.1, 1.2, 0.5]);
      grip(b, 'ray', 'navy', 1.0, 5.4, 1.0, 1.4);
      b.box('jade', 11, [-0.35, -1.2, -0.35], [0.35, -0.5, 0.35]);
      tassel(b, 'cream', -1.2, 3.6, 0.5);
      // The guard: two ruyi clouds back to back, curling toward the blade.
      b.box('gold', 10, [-0.9, 5.4, -0.65], [0.9, 6.2, 0.65]);
      for (const s of [-1, 1]) {
        b.box('gold', 11, [s * 0.9 - (s > 0 ? 0 : 1.0), 5.6, -0.55], [s * 0.9 + (s > 0 ? 1.0 : 0), 6.2, 0.55]);
        b.box('gold', 12, [s * 1.6 - 0.4, 6.2, -0.5], [s * 1.6 + 0.4, 6.9, 0.5]);
        b.box('gold', 13, [s * 1.2 - 0.25, 6.6, -0.45], [s * 1.2 + 0.25, 7.0, 0.45]);
      }
      straight(b, 6.2, 25, 1.4, 'azure', 0.33);
      peakInlay(b, 0, 6.8, 0.18, 0.42);
    },
    text: "Le jian élégant d'un maître : pommeau et garde en nuages ruyi (如意, « selon le souhait »), poignée de peau de raie claire liée de marine, houppe crème et perle de jade. Une gouttière d'azur court le long de la lame, les trois pics à sa base. Pour les anciens et les maîtres d'armes de l'Alliance.",
  },
  willow_dao: {
    name: 'Willow Leaf Dao', zh: '柳叶刀', fr: 'Dao feuille de saule',
    colours: { ...ALLIANCE },
    build(b) {
      // A cap, a navy grip, a long azure tassel.
      b.box('gold', 10, [-0.75, 0, -0.6], [0.75, 0.9, 0.6]);
      grip(b, 'navy', 'gold', 0.9, 5.5, 1.0, 1.2);
      tassel(b, 'azure', 0.0, 4.2, 0.55);
      // A small oval guard, gold over azure.
      b.box('azure', 8, [-1.5, 5.5, -1.1], [1.5, 6.0, 1.1]);
      b.box('gold', 11, [-1.25, 6.0, -0.9], [1.25, 6.4, 0.9]);
      // The blade: slender and gently curved, single-edged (the edge on -x), the back bending toward +x as it rises.
      const ROW = 1.6, rows = 12;
      for (let i = 0; i < rows; i++) {
        const k = i / rows, bend = 1.6 * k * k, w = 1.25 - 0.35 * Math.max(0, k - 0.7) / 0.3, y = 6.4 + i * ROW;
        const back = 0.55 + bend;
        b.box('steel', 7 + (i % 2), [back - w + 0.3, y, -0.2], [back, y + ROW, 0.2]);
        b.box('edge', 14, [back - w, y, -0.13], [back - w + 0.3, y + ROW, 0.13]);
        b.box('steel', 5, [back - 0.25, y, -0.26], [back, y + ROW, 0.26]);
      }
      const top = 6.4 + rows * ROW, tip = 0.55 + 1.6;
      b.box('edge', 15, [tip - 0.8, top, -0.12], [tip, top + 0.8, 0.12]);
      b.box('edge', 15, [tip - 0.35, top + 0.8, -0.1], [tip, top + 1.4, 0.1]);
      peakInlay(b, 0.0, 7.0, 0.2, 0.4);
    },
    text: "Le liuyedao (柳叶刀), le sabre « feuille de saule » des héros errants : lame fine à un tranchant, doucement courbée jusqu'à la pointe, petite garde ovale, longue houppe azur, les trois pics gravés au talon. Léger et rapide, le sabre le plus répandu dans les romans de wuxia : pour les soldats d'élite et les éclaireurs.",
  },
  ring_dao: {
    name: 'Ring Pommel Saber', zh: '环首刀', fr: 'Sabre à pommeau en anneau',
    colours: { ...ALLIANCE },
    build(b) {
      // A large gold ring for a pommel, the three peaks inside it, an azure tassel tied through it.
      b.box('gold', 11, [-1.4, -2.2, -0.3], [1.4, -1.7, 0.3]);
      b.box('gold', 11, [-1.4, 0.1, -0.3], [1.4, 0.6, 0.3]);
      b.box('gold', 10, [-1.4, -2.2, -0.3], [-0.9, 0.6, 0.3]);
      b.box('gold', 10, [0.9, -2.2, -0.3], [1.4, 0.6, 0.3]);
      peakInlay(b, 0, -1.6, 0.3, 0.35);
      b.box('azure', 8, [-0.3, -3.0, -0.3], [0.3, -2.2, 0.3]);
      for (const x of [-0.3, 0, 0.3]) b.box('azure', 7, [x - 0.15, -6.5, -0.15], [x + 0.15, -3.0, 0.15]);
      // A long grip of navy cord, a plain small guard.
      grip(b, 'navy', 'azure', 0.6, 6.2, 1.0, 0.8);
      b.box('gold', 10, [-1.2, 6.2, -0.8], [1.2, 6.7, 0.8]);
      // The blade: long and straight, single-edged (the edge on -x), a slanted point.
      b.box('steel', 8, [-0.6, 6.7, -0.22], [0.75, 25, 0.22]);
      b.box('edge', 14, [-0.95, 6.7, -0.14], [-0.6, 25, 0.14]);
      b.box('steel', 5, [0.5, 6.7, -0.28], [0.75, 25, 0.28]);
      b.box('steel', 9, [-0.6, 25, -0.2], [0.75, 26.0, 0.2]);
      b.box('edge', 15, [-0.95, 25, -0.13], [0.2, 26.8, 0.13]);
      b.box('edge', 15, [-0.95, 26.8, -0.1], [-0.4, 27.4, 0.1]);
    },
    text: "Le huanshou dao (环首刀), le sabre droit à pommeau en anneau des armées anciennes, l'ancêtre du hwando des récits de Murim. Lame droite à un tranchant, longue poignée de cordelette marine, et dans le grand anneau d'or les trois pics, une houppe azur nouée au travers. Austère et militaire : l'arme de la troupe.",
  },
  seven_star_jian: {
    name: 'Seven Star Jian', zh: '七星剑', fr: 'Jian aux Sept Étoiles',
    colours: { ...ALLIANCE, star: '#FFE9A8' },
    build(b) {
      b.box('gold', 10, [-0.8, 0, -0.65], [0.8, 1.0, 0.65]);
      b.box('star', 15, [-0.25, 0.3, -0.7], [0.25, 0.7, 0.7]);
      grip(b, 'navy', 'gold', 1.0, 5.8, 1.0, 1.6);
      tassel(b, 'azure', 0.0, 4.0, 0.55);
      // A small guard, the three peaks standing on it, the middle one holding the blade.
      b.box('gold', 10, [-1.6, 5.8, -0.65], [1.6, 6.4, 0.65]);
      b.box('gold', 11, [-1.5, 6.4, -0.5], [-0.9, 6.9, 0.5]);
      b.box('gold', 11, [0.9, 6.4, -0.5], [1.5, 6.9, 0.5]);
      b.box('gold', 12, [-0.75, 6.4, -0.55], [0.75, 7.3, 0.55]);
      straight(b, 7.3, 26, 1.35, 'navy', 0.34);
      // The Big Dipper inlaid in gold along the blade's flat, on both faces.
      const dipper = [[0.15, 9.0], [-0.2, 10.4], [0.25, 11.6], [-0.1, 13.0], [0.2, 14.8], [-0.15, 16.6], [0.1, 18.6]];
      for (const [x, y] of dipper) {
        for (const [za, zb] of [[-0.24, -0.17], [0.17, 0.24]]) b.box('star', 15, [x - 0.15, y - 0.15, za], [x + 0.15, y + 0.15, zb]);
      }
    },
    text: "Le qixingjian (七星剑), l'épée aux sept étoiles des légendes taoïstes : la Grande Ourse incrustée d'or sur une gouttière marine, comme un ciel de nuit au-dessus des pics. Petite garde où se dressent les trois pics, poignée marine baguée d'or, une étoile au pommeau, houppe azur. L'épée d'un chef ou d'un héros de l'Alliance.",
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
