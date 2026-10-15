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

/** A disc of `mat` facing -z, radius r, from z0 to z1, in rows `step` tall (shade a number or a function of the row). */
function disc(b, mat, shade, r, z0, z1, step = 0.5, cx = CX, cy = CY) {
  for (let y = -r; y < r; y += step) {
    const m = Math.min(Math.abs(y), Math.abs(y + step));
    const w = Math.sqrt(Math.max(0, r * r - m * m));
    const s = typeof shade === 'function' ? shade(y) : shade;
    if (w > 0.05) b.box(mat, s, [cx - w, cy + y, z0], [cx + w, cy + y + step, z1]);
  }
}

/** A ring between r0 and r1: a disc with the middle left out, row by row. */
function ring(b, mat, shade, r0, r1, z0, z1, step = 0.5, cx = CX, cy = CY) {
  for (let y = -r1; y < r1; y += step) {
    const m = Math.min(Math.abs(y), Math.abs(y + step));
    const w1 = Math.sqrt(Math.max(0, r1 * r1 - m * m)), w0 = Math.sqrt(Math.max(0, r0 * r0 - m * m));
    const s = typeof shade === 'function' ? shade(y) : shade;
    if (w1 <= 0.05) continue;
    if (w0 <= 0.05 || m >= r0) b.box(mat, s, [cx - w1, cy + y, z0], [cx + w1, cy + y + step, z1]);
    else {
      b.box(mat, s, [cx - w1, cy + y, z0], [cx - w0, cy + y + step, z1]);
      b.box(mat, s, [cx + w0, cy + y, z0], [cx + w1, cy + y + step, z1]);
    }
  }
}

/** Light from the top left: brighter toward the top of a round face. */
const lit = (base, span = 4) => y => Math.max(0, Math.min(15, Math.round(base + (y / 7) * span)));

/**
 * A domed face: `rings` steps from radius r to the middle, each one `rise` nearer the viewer (-z) than the last, a little
 * lighter, so the face swells toward its middle. The back stays flat at z back.
 */
function dome(b, mat, base, r, rings, rise, back, cx = CX, cy = CY) {
  for (let i = 0; i < rings; i++) {
    const r1 = r * (1 - i / rings), r0 = r * (1 - (i + 1) / rings), z = -i * rise;
    if (i === rings - 1) disc(b, mat, lit(base + i), r1, z - rise, back, 0.5, cx, cy);
    else ring(b, mat, lit(base + i), r0, r1, z - rise, back, 0.5, cx, cy);
  }
  return -rings * rise;
}

/** A domed stud of `mat` at (x, y) on a face at z: two stacked squares and a bright point. */
function stud(b, mat, x, y, z, s = 1) {
  b.box(mat, 9, [x - 0.45 * s, y - 0.45 * s, z - 0.25 * s], [x + 0.45 * s, y + 0.45 * s, z]);
  b.box(mat, 12, [x - 0.3 * s, y - 0.3 * s, z - 0.45 * s], [x + 0.3 * s, y + 0.3 * s, z - 0.25 * s]);
  b.box(mat, 15, [x - 0.12 * s, y - 0.05 * s, z - 0.5 * s], [x + 0.05 * s, y + 0.18 * s, z - 0.45 * s]);
}

/**
 * The flag in relief on a face at z (facing -z), s its scale: three peaks in three layers (pale rock, its shade, snow on
 * the tops, each nearer), the sword standing proud of them: blade with its ridge, gold guard, grip, pommel.
 */
function emblem(b, z, s = 1, cx = CX, cy = CY) {
  const box = (mat, shade, x0, y0, x1, y1, d0, d1) => b.box(mat, shade, [cx + x0 * s, cy + y0 * s, z - d1 * s], [cx + x1 * s, cy + y1 * s, z - d0 * s]);
  // The rock: the range's foot, the two side peaks, the tall middle one; their right faces in shade.
  box('ice', 7, -4.2, -3.2, 4.2, -2.4, 0, 0.25);
  box('ice', 10, -3.8, -2.4, -1.2, -1.2, 0, 0.35); box('ice', 7, -2.4, -2.4, -1.2, -1.2, 0.05, 0.4);
  box('ice', 10, 1.2, -2.4, 3.8, -1.2, 0, 0.35); box('ice', 7, 2.6, -2.4, 3.8, -1.2, 0.05, 0.4);
  box('ice', 10, -3.3, -1.2, -1.7, -0.6, 0, 0.35); box('ice', 10, 1.7, -1.2, 3.3, -0.6, 0, 0.35);
  box('ice', 11, -1.8, -2.4, 1.8, 0.0, 0, 0.45); box('ice', 8, 0.4, -2.4, 1.8, 0.0, 0.05, 0.5);
  box('ice', 11, -1.3, 0.0, 1.3, 0.8, 0, 0.45);
  // Snow on the tops, standing a little proud.
  box('snow', 14, -3.1, -0.9, -1.9, -0.4, 0.3, 0.5); box('snow', 14, 1.9, -0.9, 3.1, -0.4, 0.3, 0.5);
  box('snow', 15, -1.0, 0.5, 1.0, 1.4, 0.4, 0.65); box('snow', 13, -0.5, 1.4, 0.5, 1.8, 0.4, 0.65);
  // The sword, planted in the middle peak and standing proud of everything.
  box('edge', 15, -0.35, -0.8, 0.35, 3.1, 0.6, 0.85); box('ice', 9, -0.08, -0.8, 0.08, 3.0, 0.85, 0.9);
  box('edge', 15, -0.2, -1.2, 0.2, -0.8, 0.6, 0.8);
  box('gold', 12, -1.3, 3.1, 1.3, 3.6, 0.55, 0.95); box('gold', 9, -1.3, 3.1, -1.0, 3.8, 0.55, 0.9); box('gold', 9, 1.0, 3.1, 1.3, 3.8, 0.55, 0.9);
  box('grip', 7, -0.28, 3.6, 0.28, 4.8, 0.6, 0.9); box('grip', 4, -0.3, 4.0, 0.3, 4.15, 0.55, 0.92); box('grip', 4, -0.3, 4.4, 0.3, 4.55, 0.55, 0.92);
  box('gold', 13, -0.45, 4.8, 0.45, 5.3, 0.55, 0.95);
}

/** The back (+z, from `back`): two crossed battens, a forearm strap with its buckle, a handle on blocks. */
function backing(b, back, w = 5.5) {
  b.box('wood', 5, [CX - w, CY + 2.2, back], [CX + w, CY + 3.2, back + 0.5]);
  b.box('wood', 5, [CX - w, CY - 3.2, back], [CX + w, CY - 2.2, back + 0.5]);
  b.box('leather', 6, [CX - 3.6, CY - 0.6, back], [CX + 3.6, CY + 0.6, back + 0.35]);
  b.box('leather', 4, [CX - 3.6, CY - 0.6, back + 0.35], [CX - 3.0, CY + 0.6, back + 0.6]);
  b.box('gold', 12, [CX + 1.6, CY - 0.7, back + 0.35], [CX + 2.4, CY + 0.7, back + 0.5]);
  b.box('wood', 6, [CX - 0.8, CY - 2.6, back], [CX + 0.8, CY - 1.8, back + 0.9]);
  b.box('wood', 6, [CX - 0.8, CY + 1.8, back], [CX + 0.8, CY + 2.6, back + 0.9]);
  b.box('leather', 7, [CX - 0.45, CY - 2.0, back + 0.9], [CX + 0.45, CY + 2.0, back + 1.4]);
  for (const y of [-1.2, -0.2, 0.8]) b.box('leather', 4, [CX - 0.5, CY + y, back + 0.9], [CX + 0.5, CY + y + 0.25, back + 1.45]);
}

/** A tassel hanging from (x, y) in front of the face at z: a gold cap, a knot, strands of `mat`. */
function hangTassel(b, mat, x, y, z, len = 4) {
  b.box('gold', 12, [x - 0.4, y - 0.6, z - 0.4], [x + 0.4, y, z + 0.4]);
  b.box(mat, 9, [x - 0.45, y - 1.2, z - 0.45], [x + 0.45, y - 0.6, z + 0.45]);
  [[-0.3, 0], [0, -0.25], [0.3, 0.05], [0.05, 0.3]].forEach(([dx, dz], i) =>
    b.box(mat, 6 + i, [x + dx - 0.15, y - 1.2 - len * (0.85 + 0.05 * i), z + dz - 0.15], [x + dx + 0.15, y - 1.2, z + dz + 0.15]));
}

// ---------------------------------------------------------------- the styles

const STYLES = {
  round: {
    name: 'Azure Peak Round Shield', zh: '天峰圆盾', fr: 'Bouclier rond du Pic azur',
    colours: { ...ALLIANCE, grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // Lacquered planks under the azure: a slight dome, its rows lighter toward the top.
      const top = dome(b, 'azure', 6, 6.6, 5, 0.18, 0.6);
      // A thick gold rim, bevelled: a broad band and a raised bead on it; a navy line inside.
      ring(b, 'gold', lit(9), 6.4, 7.3, -0.5, 0.7);
      ring(b, 'gold', lit(12), 6.7, 7.05, -0.8, -0.5);
      ring(b, 'navy', 5, 5.9, 6.4, -0.55, 0.6);
      // Twelve domed studs on the rim, the flag in relief in the middle.
      for (let k = 0; k < 12; k++) {
        const a = k * Math.PI / 6, x = CX + Math.cos(a) * 6.85, y = CY + Math.sin(a) * 6.85;
        stud(b, 'gold', x, y, -0.8, 0.9);
      }
      emblem(b, top, 1.05);
      // Two azure tassels hanging from the rim's foot.
      hangTassel(b, 'azure', CX - 2.2, CY - 7.0, 0.2, 3.5);
      hangTassel(b, 'azure', CX + 2.2, CY - 7.0, 0.2, 3.5);
      backing(b, 0.6);
    },
    text: "Le bouclier rond du soldat, en bois laqué d'azur légèrement bombé : un épais rebord d'or biseauté à douze clous en dôme, et au centre le drapeau de l'Alliance en relief, les trois pics en couches de roche et de neige, l'épée plantée qui ressort au premier plan. Deux houppes azur au pied ; au dos, renforts croisés, sangle à boucle d'or et poignée gainée.",
  },
  rattan: {
    name: 'Rattan Shield', zh: '藤牌', fr: 'Bouclier de rotin',
    colours: { ...ALLIANCE, rattan: '#c9a96a', rattanDark: '#8f7240', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // Rattan coiled into a deep dome: each coil a ring nearer the viewer, light and dark turns, small bindings across.
      const coils = 9, r = 7.0, rise = 0.42;
      for (let i = 0; i < coils - 2; i++) {
        const r1 = r * (1 - i / coils), r0 = r * (1 - (i + 1) / coils);
        ring(b, i % 2 ? 'rattanDark' : 'rattan', lit(i % 2 ? 6 : 9), r0, r1, -i * rise - rise, 0.5);
        for (let k = 0; k < 8; k++) {
          const a = k * Math.PI / 4 + i * 0.4, rr = (r0 + r1) / 2, x = CX + Math.cos(a) * rr, y = CY + Math.sin(a) * rr;
          b.box('rattanDark', 4, [x - 0.15, y - 0.15, -i * rise - rise - 0.1], [x + 0.15, y + 0.15, -i * rise - rise]);
        }
      }
      // On the dome's top, an azure medallion rimmed in gold, the flag on it.
      const z = -(coils - 2) * rise;
      disc(b, 'azure', lit(8), r * 2 / coils + 0.4, z - 0.4, z + 0.3);
      ring(b, 'gold', lit(11), r * 2 / coils + 0.2, r * 2 / coils + 0.7, z - 0.55, z + 0.3);
      emblem(b, z - 0.4, 0.42);
      // A blue cord bound round the edge.
      ring(b, 'navy', 6, 6.7, 7.3, -0.55, 0.55);
      for (let k = 0; k < 16; k++) {
        const a = k * Math.PI / 8, x = CX + Math.cos(a) * 7.0, y = CY + Math.sin(a) * 7.0;
        b.box('azure', 10, [x - 0.25, y - 0.25, -0.7], [x + 0.25, y + 0.25, 0.7]);
      }
      backing(b, 0.5, 5.2);
    },
    text: "Le tengpai (藤牌), le bouclier de rotin des fantassins des Ming, tressé en dôme profond qui fait glisser les lames : spirale de rotin clair et foncé, petites ligatures, une cordelette bleue liée tout autour du bord. Au sommet, un médaillon azur cerclé d'or porte le drapeau en relief. Léger et rapide, pour une troupe au sabre.",
  },
  tower: {
    name: 'Citadel Tower Shield', zh: '天峰长牌', fr: 'Grand pavois de la Citadelle',
    colours: { ...ALLIANCE, grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220', iron: '#5a6068' },
    build(b) {
      // Vertical planks, alternate shades, a little curved: the middle ones nearer the viewer.
      const planks = 6, hw = 5.4;
      for (let i = 0; i < planks; i++) {
        const x0 = CX - hw + i * (2 * hw / planks), x1 = x0 + 2 * hw / planks, bow = 0.35 * Math.sin(Math.PI * (i + 0.5) / planks);
        b.box('azure', 7 + (i % 2) * 2, [x0, CY - 11, -0.4 - bow], [x1, CY + 9, 0.5]);
      }
      // A gold frame standing proud, iron corner brackets with rivets, a raised panel round the flag.
      b.box('gold', 11, [CX - 5.8, CY + 9, -1.0], [CX + 5.8, CY + 9.9, 0.6]);
      b.box('gold', 10, [CX - 5.8, CY - 11.9, -1.0], [CX + 5.8, CY - 11, 0.6]);
      b.box('gold', 12, [CX - 5.8, CY - 11, -1.0], [CX - 5.0, CY + 9, 0.6]);
      b.box('gold', 9, [CX + 5.0, CY - 11, -1.0], [CX + 5.8, CY + 9, 0.6]);
      for (const [x, y] of [[-5.4, 9.4], [5.4, 9.4], [-5.4, -11.4], [5.4, -11.4]]) {
        b.box('iron', 7, [CX + x - 1.0, CY + y - 1.0, -1.25], [CX + x + 1.0, CY + y + 1.0, -1.0]);
        stud(b, 'gold', CX + x, CY + y, -1.25, 0.7);
      }
      b.box('navy', 5, [CX - 3.9, CY - 3.8, -1.0], [CX + 3.9, CY + 6.6, -0.6]);
      b.box('azure', 9, [CX - 3.5, CY - 3.4, -1.1], [CX + 3.5, CY + 6.2, -0.9]);
      emblem(b, -1.1, 0.75, CX, CY + 1.0);
      // A navy band below with three gold studs, and the gable top in three steps.
      b.box('navy', 6, [CX - 5.0, CY - 8.0, -0.9], [CX + 5.0, CY - 6.6, -0.5]);
      for (const x of [-3, 0, 3]) stud(b, 'gold', CX + x, CY - 9.4, -0.6, 0.9);
      b.box('gold', 12, [CX - 3.8, CY + 9.9, -0.8], [CX + 3.8, CY + 10.7, 0.5]);
      b.box('gold', 13, [CX - 2.0, CY + 10.7, -0.7], [CX + 2.0, CY + 11.5, 0.45]);
      b.box('gold', 14, [CX - 0.7, CY + 11.5, -0.6], [CX + 0.7, CY + 12.4, 0.4]);
      backing(b, 0.5, 5.0);
    },
    text: "Le changpai (长牌), le grand pavois des gardes de la Citadelle du Pic céleste : planches verticales laquées d'azur, légèrement cintrées, un cadre d'or en saillie, des équerres de fer rivetées aux coins, un panneau surélevé où le drapeau est sculpté, un fronton en pignon à trois gradins. Lent mais imprenable : la muraille de l'Alliance, pour les gardes des portes.",
  },
  buckler: {
    name: 'Peak Bronze Buckler', zh: '铜峰盾', fr: 'Rondache de bronze des Pics',
    colours: { ...ALLIANCE, bronze: '#c39a3c', bronzeDark: '#8a6a28', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220' },
    build(b) {
      // Hammered bronze, domed, with engraved rings; a thick rolled rim.
      const top = dome(b, 'bronze', 5, 4.4, 4, 0.22, 0.4);
      for (const rr of [3.3, 2.2]) ring(b, 'bronzeDark', 5, rr - 0.12, rr + 0.12, top + 0.05 + (4.4 - rr) * -0.05, top + 0.6);
      ring(b, 'bronzeDark', lit(6), 4.2, 4.9, -0.7, 0.5);
      ring(b, 'bronze', lit(12), 4.45, 4.7, -0.95, -0.7);
      emblem(b, top, 0.68);
      // A gold ring at its foot, the long azure tassel from it.
      b.box('gold', 11, [CX - 0.5, CY - 5.4, -0.3], [CX + 0.5, CY - 4.7, 0.2]);
      hangTassel(b, 'azure', CX, CY - 5.4, -0.05, 4.2);
      backing(b, 0.4, 3.6);
    },
    text: "Une petite rondache de bronze martelé pour les disciples qui se battent à l'épée et au bouclier : bombée, gravée d'anneaux, un rebord roulé épais, les trois pics et l'épée coulés en relief au centre. Une longue houppe azur au pied, qui se balancerait comme celle des épées.",
  },
  lion: {
    name: 'Guardian Lion Shield', zh: '狮头牌', fr: 'Bouclier à tête de lion gardien',
    colours: { ...ALLIANCE, mane: '#b08a3a', face: '#E9D9A6', red: '#a8322a', grip: '#6B3E26', leather: '#3a2618', wood: '#4e3220', dark: '#14284A' },
    build(b) {
      // A domed azure shield, a gold rim with studs.
      dome(b, 'azure', 6, 6.6, 3, 0.2, 0.6);
      ring(b, 'gold', lit(9), 6.4, 7.3, -0.6, 0.7);
      ring(b, 'gold', lit(12), 6.7, 7.05, -0.9, -0.6);
      for (let k = 0; k < 8; k++) {
        const a = k * Math.PI / 4 + Math.PI / 8;
        stud(b, 'gold', CX + Math.cos(a) * 6.85, CY + Math.sin(a) * 6.85, -0.9, 0.8);
      }
      // The guardian lion's head, sculpted in gold, its face standing out: the mane in two rows of curls.
      ring(b, 'mane', lit(8), 3.6, 5.4, -1.0, -0.5);
      for (let k = 0; k < 14; k++) {
        const a = k * 2 * Math.PI / 14, x = CX + Math.cos(a) * 5.3, y = CY + Math.sin(a) * 5.3;
        b.box('mane', 11, [x - 0.55, y - 0.55, -1.4], [x + 0.55, y + 0.55, -1.0]);
        b.box('gold', 13, [x - 0.25, y - 0.25, -1.55], [x + 0.25, y + 0.25, -1.4]);
      }
      for (let k = 0; k < 12; k++) {
        const a = k * 2 * Math.PI / 12 + 0.26, x = CX + Math.cos(a) * 4.2, y = CY + Math.sin(a) * 4.2;
        b.box('mane', 9, [x - 0.45, y - 0.45, -1.3], [x + 0.45, y + 0.45, -1.0]);
      }
      disc(b, 'face', lit(11, 3), 3.5, -1.5, -0.5);
      disc(b, 'face', lit(13, 3), 2.6, -1.8, -1.5);
      const f = (mat, shade, x0, y0, x1, y1, d0, d1) => b.box(mat, shade, [CX + x0, CY + y0, -d1], [CX + x1, CY + y1, -d0]);
      // Heavy brows, deep-set round eyes with a glint, a broad flat nose, puffed cheeks, an open mouth with fangs.
      f('gold', 13, -2.6, 0.9, -0.5, 1.6, 1.8, 2.3); f('gold', 13, 0.5, 0.9, 2.6, 1.6, 1.8, 2.3);
      f('dark', 4, -2.1, -0.1, -0.8, 0.9, 1.8, 1.9); f('dark', 4, 0.8, -0.1, 2.1, 0.9, 1.8, 1.9);
      f('gold', 12, -1.75, 0.15, -1.15, 0.65, 1.9, 2.05); f('gold', 12, 1.15, 0.15, 1.75, 0.65, 1.9, 2.05);
      f('snow', 15, -1.6, 0.45, -1.4, 0.6, 2.05, 2.1); f('snow', 15, 1.3, 0.45, 1.5, 0.6, 2.05, 2.1);
      f('mane', 10, -0.9, -1.3, 0.9, 0.3, 1.8, 2.5); f('mane', 13, -0.5, -0.2, 0.5, 0.3, 2.5, 2.6);
      f('dark', 3, -0.7, -1.3, -0.3, -1.0, 2.4, 2.55); f('dark', 3, 0.3, -1.3, 0.7, -1.0, 2.4, 2.55);
      f('face', 12, -2.6, -1.6, -1.0, -0.4, 1.8, 2.15); f('face', 12, 1.0, -1.6, 2.6, -0.4, 1.8, 2.15);
      f('red', 7, -1.9, -2.9, 1.9, -1.6, 1.6, 1.85);
      f('dark', 2, -1.5, -2.7, 1.5, -1.9, 1.55, 1.7);
      for (const x of [-1.5, 1.5]) f('snow', 15, x - 0.25, -2.6, x + 0.25, -1.7, 1.85, 2.05);
      for (const x of [-0.75, -0.25, 0.25, 0.75]) f('snow', 14, x - 0.18, -1.95, x + 0.18, -1.6, 1.85, 2.0);
      // The three peaks on its forehead, small, in relief.
      f('ice', 10, -1.1, 1.8, 1.1, 2.2, 1.8, 2.0); f('snow', 15, -0.3, 2.2, 0.3, 2.9, 1.8, 2.1);
      f('snow', 13, -1.0, 2.2, -0.6, 2.5, 1.8, 2.05); f('snow', 13, 0.6, 2.2, 1.0, 2.5, 1.8, 2.05);
      // An iron ring in its jaws.
      ring(b, 'gold', 10, 0.9, 1.25, -2.0, -1.8, 0.25, CX, CY - 3.6);
      backing(b, 0.6);
    },
    text: "Le shitoupai (狮头牌), le bouclier à tête de lion gardien : sur un fond azur bombé à rebord d'or clouté, la tête sculptée d'un des lions qui gardent la porte de la Citadelle : crinière en deux rangs de boucles, sourcils lourds, yeux enfoncés, nez épaté, gueule ouverte à crocs tenant un anneau d'or, et les trois pics sur son front. Pour les gardes d'honneur ou un officier.",
  },
};

// ---------------------------------------------------------------- rendering

const VIEWS = {
  front: [[CX, CY + 2, -46], [CX, CY, 0]], three: [[CX - 30, CY + 12, -30], [CX, CY, -1]], side: [[CX - 40, CY + 6, -8], [CX, CY, 0]],
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
