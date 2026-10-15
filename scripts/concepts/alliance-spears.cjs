// Concept sheets for the Orthodox Alliance soldiers' spear: five styles in the Alliance's colours (azure field, gold and
// cream trim, navy ink: scripts/art/factions.cjs), each with a nod to its flag (a sword planted in three snowy peaks).
// Rendered from five angles and once beside a man for its size, to docs/factions/images/alliance-spear-<style>-<view>.png.
// Concepts to choose from, not game files.
// Built in Blockbench pixels at a man's scale (the Mountain Bandit's model, 32 pixels tall): the shaft upright from y 0,
// its centre at x 0, z 0, the blade's flat facing z.
//
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9234 --user-data-dir=<empty profile>)
//   node scripts/concepts/alliance-spears.cjs [--only=azure_peak,banner] [--port=9234]
const fs = require('node:fs');
const path = require('node:path');
const { palette, builder, textureOf, connect, render, id } = require('./chief-axes.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'factions', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9234);

/** The Alliance's colours, shared by every style (each adds its own). */
const ALLIANCE = {
  azure: '#2B5FA8', navy: '#14284A', gold: '#D9B25F', cream: '#E9D9A6', snow: '#EEF3F8', ice: '#9FB6CF',
  steel: '#a9b0b6', edge: '#f2f4f6',
};

// ---------------------------------------------------------------- shared parts

/** A shaft of `mat` from y0 to y1, its section w. */
const shaft = (b, mat, y0, y1, w = 1.4, shade = 7) => b.box(mat, shade, [-w / 2, y0, -w / 2], [w / 2, y1, w / 2]);
/** A band round the shaft. */
const band = (b, mat, y, h = 0.7, w = 1.9, shade = 11) => b.box(mat, shade, [-w / 2, y, -w / 2], [w / 2, y + h, w / 2]);

/**
 * A leaf-shaped head from y0, rows `widths` tall `row` each: a flat steel body, bright edges, a raised midrib.
 * Returns the tip's height.
 */
function leaf(b, y0, widths, row = 1.1, t = 0.45) {
  widths.forEach((w, i) => {
    const y = y0 + i * row, e = Math.min(0.4, w / 3);
    b.box('steel', 6 + Math.round(4 * i / widths.length), [-w / 2 + e, y, -t / 2], [w / 2 - e, y + row, t / 2]);
    b.box('edge', 14, [-w / 2, y, -t / 3], [-w / 2 + e, y + row, t / 3]);
    b.box('edge', 14, [w / 2 - e, y, -t / 3], [w / 2, y + row, t / 3]);
  });
  const top = y0 + widths.length * row;
  b.box('steel', 4, [-0.25, y0, -0.4], [0.25, top - row, 0.4]);
  b.box('edge', 15, [-0.2, top, -0.15], [0.2, top + 0.7, 0.15]);
  return top + 0.7;
}

/** Horsehair tassel hanging from y round the shaft, `len` long. */
function tassel(b, mat, y, len = 5, spread = 1.2) {
  b.box(mat, 8, [-1.1, y - 1, -1.1], [1.1, y, 1.1]);
  for (const [x, z] of [[-spread, 0], [spread, 0], [0, -spread], [0, spread], [-0.8, -0.8], [0.8, 0.8], [0.8, -0.8], [-0.8, 0.8]]) {
    b.box(mat, 6 + Math.round(Math.abs(x + z) * 2), [x - 0.3, y - 1 - len * (0.75 + 0.25 * Math.abs(x)), z - 0.3], [x + 0.3, y - 1, z + 0.3]);
  }
}

/** The flag's three peaks in small, on a socket face (z = ±face), the middle one tallest; snow tops. */
function peaks(b, y, face = 1.05, s = 1) {
  for (const sign of [-1, 1]) {
    const z0 = sign * face, z1 = sign * (face + 0.12);
    const [za, zb] = z0 < z1 ? [z0, z1] : [z1, z0];
    b.box('snow', 13, [-0.4 * s, y, za], [0.4 * s, y + 1.5 * s, zb]);
    b.box('ice', 9, [-1.0 * s, y, za], [-0.4 * s, y + 0.9 * s, zb]);
    b.box('ice', 9, [0.4 * s, y, za], [1.0 * s, y + 0.9 * s, zb]);
  }
}

// ---------------------------------------------------------------- the styles

const STYLES = {
  azure_peak: {
    name: 'Azure Peak Spear', zh: '天峰枪', fr: 'Lance du Pic azur',
    colours: { ...ALLIANCE },
    build(b) {
      shaft(b, 'navy', 0, 40);
      for (const y of [6, 18, 30]) band(b, 'gold', y);
      b.box('gold', 9, [-0.9, -2, -0.9], [0.9, 0, 0.9]);
      b.box('gold', 12, [-0.4, -3, -0.4], [0.4, -2, 0.4]);
      // The socket in azure enamel, the three peaks cast on it, gold collars.
      band(b, 'gold', 39.4, 0.8, 2.2);
      b.box('azure', 8, [-1.0, 40.2, -1.0], [1.0, 43.2, 1.0]);
      peaks(b, 40.6, 1.0, 0.9);
      band(b, 'gold', 43.2, 0.6, 2.1);
      tassel(b, 'azure', 40, 5);
      leaf(b, 43.8, [1.4, 2.4, 3.0, 3.2, 3.0, 2.6, 2.0, 1.4, 0.8]);
    },
    text: "La lance de base du soldat : hampe laquée marine à trois bagues d'or, douille émaillée d'azur où sont coulés les trois pics du drapeau, houppe de crin bleu (au lieu du rouge des bandits), fer en feuille de saule à arête.",
  },
  three_peaks: {
    name: 'Three Peaks Trident', zh: '三峰叉', fr: 'Trident des Trois Pics',
    colours: { ...ALLIANCE, wrap: '#E9D9A6' },
    build(b) {
      shaft(b, 'azure', 0, 39, 1.4, 6);
      b.box('wrap', 9, [-0.85, 2, -0.85], [0.85, 9, 0.85]);
      band(b, 'gold', 15); band(b, 'gold', 27);
      b.box('gold', 9, [-0.9, -2, -0.9], [0.9, 0, 0.9]);
      // The fork: a gold crossbar, two side prongs rising like the lower peaks, the middle one the sword.
      band(b, 'gold', 38.6, 1, 2.3);
      b.box('gold', 11, [-4.6, 39.6, -0.5], [4.6, 40.8, 0.5]);
      for (const s of [-1, 1]) {
        b.box('steel', 8, [s * 3.4 - 0.55, 40.8, -0.35], [s * 3.4 + 0.55, 44.5, 0.35]);
        b.box('steel', 9, [s * 3.1 - 0.45, 44.5, -0.3], [s * 3.1 + 0.45, 46.3, 0.3]);
        b.box('edge', 15, [s * 2.9 - 0.25, 46.3, -0.2], [s * 2.9 + 0.25, 47.4, 0.2]);
        b.box('snow', 13, [s * 3.4 - 0.6, 40.8, -0.4], [s * 3.4 + 0.6, 41.6, 0.4]);
      }
      // The middle prong: a straight sword blade with its gold guard, as on the flag.
      b.box('gold', 12, [-1.6, 40.8, -0.6], [1.6, 41.6, 0.6]);
      b.box('steel', 9, [-0.75, 41.6, -0.3], [0.75, 51, 0.3]);
      b.box('edge', 15, [-0.75, 41.6, -0.2], [-0.45, 51, 0.2]);
      b.box('edge', 15, [0.45, 41.6, -0.2], [0.75, 51, 0.2]);
      b.box('ice', 8, [-0.15, 41.6, -0.35], [0.15, 50, 0.35]);
      b.box('edge', 15, [-0.45, 51, -0.2], [0.45, 52.2, 0.2]);
      b.box('edge', 15, [-0.15, 52.2, -0.15], [0.15, 52.8, 0.15]);
    },
    text: "Le drapeau en arme : trois fourchons sur une traverse d'or, les deux latéraux montent comme les pics bas, celui du milieu est une lame d'épée droite à garde d'or, l'épée plantée dans les pics. Hampe azur, poignée crème. Pour un officier ou une garde d'honneur.",
  },
  banner: {
    name: 'Banner Spear', zh: '旗枪', fr: 'Lance à fanion',
    colours: { ...ALLIANCE, wood: '#4e3220' },
    build(b) {
      shaft(b, 'wood', 0, 41, 1.4, 6);
      for (const y of [8, 22]) band(b, 'gold', y);
      b.box('gold', 9, [-0.9, -2, -0.9], [0.9, 0, 0.9]);
      band(b, 'gold', 40.4, 0.8, 2.1);
      b.box('gold', 8, [-0.8, 41.2, -0.8], [0.8, 43.6, 0.8]);
      tassel(b, 'cream', 41.2, 3, 1.0);
      leaf(b, 43.6, [1.2, 2.2, 2.8, 2.8, 2.4, 1.8, 1.2, 0.6], 1.15);
      // The pennant: the Alliance flag in small, tied below the head, a swallow tail flying behind.
      const x0 = 0.7, x1 = 8.6, y0 = 30.5, y1 = 39.5, t = 0.12;
      b.box('cream', 12, [x0, y0, -t], [x1, y1, t]);
      b.box('azure', 9, [x0 + 0.5, y0 + 0.5, -t - 0.02], [x1, y1 - 0.5, t + 0.02]);
      // The swallow tail: a notch cut at the flying end.
      b.box('cream', 12, [x1, y0, -t], [x1 + 1.2, y0 + 3.4, t]);
      b.box('cream', 12, [x1, y1 - 3.4, -t], [x1 + 1.2, y1, t]);
      b.box('azure', 9, [x1, y0 + 0.5, -t - 0.02], [x1 + 0.9, y0 + 3.0, t + 0.02]);
      b.box('azure', 9, [x1, y1 - 3.0, -t - 0.02], [x1 + 0.9, y1 - 0.5, t + 0.02]);
      // On it, the three snowy peaks and the planted sword.
      const zf = t + 0.05, zb = -t - 0.05;
      for (const [a, c] of [[zf, zf + 0.05], [zb - 0.05, zb]]) {
        b.box('ice', 9, [2.0, 32.4, a], [7.6, 33.6, c]);
        b.box('snow', 13, [2.6, 33.6, a], [4.0, 34.6, c]);
        b.box('snow', 13, [5.6, 33.6, a], [7.0, 34.6, c]);
        b.box('snow', 13, [3.8, 33.6, a], [5.8, 35.8, c]);
        b.box('edge', 15, [4.55, 34.6, a], [5.05, 37.6, c]);
        b.box('gold', 12, [4.0, 37.2, a], [5.6, 37.7, c]);
        b.box('navy', 6, [4.55, 37.7, a], [5.05, 38.6, c]);
      }
    },
    text: "La lance du porte-drapeau : sous le fer, un fanion à queue d'aronde aux couleurs de l'Alliance, bord crème, champ azur, les trois pics enneigés et l'épée plantée. Hampe de bois à bagues d'or, houppe crème. Un soldat sur cinq la porterait, pour qu'on voie l'Alliance arriver de loin.",
  },
  guard_ji: {
    name: 'Citadel Guard Halberd', zh: '天峰戟', fr: 'Hallebarde de la Citadelle',
    colours: { ...ALLIANCE },
    build(b) {
      shaft(b, 'navy', 0, 40, 1.5);
      for (const y of [5, 16, 28]) band(b, 'gold', y, 0.6);
      b.box('gold', 9, [-1.0, -2.2, -1.0], [1.0, 0, 1.0]);
      b.box('gold', 12, [-0.4, -3.4, -0.4], [0.4, -2.2, 0.4]);
      band(b, 'gold', 39.4, 0.8, 2.2);
      b.box('azure', 8, [-1.0, 40.2, -1.0], [1.0, 44.2, 1.0]);
      peaks(b, 40.8, 1.0, 0.9);
      band(b, 'gold', 44.2, 0.6, 2.1);
      tassel(b, 'azure', 40, 4.5);
      leaf(b, 44.8, [1.2, 2.0, 2.6, 2.6, 2.2, 1.6, 1.0]);
      // The crescent side blade (the moon over the peaks), its edge facing out, and a short spike on the other side.
      const arc = [[1.0, 41.0, 43.6], [1.0, 40.2, 44.6], [0.9, 39.2, 45.6], [0.8, 38.2, 46.8], [0.7, 37.4, 47.8]];
      let x = -1.0;
      arc.forEach(([w, y0, y1], i) => {
        b.box('steel', 6 + i * 2, [x - w, y0, -0.3], [x, y1, 0.3]);
        x -= w;
      });
      // The curved edge, its two horns curling back toward the shaft.
      b.box('edge', 15, [x - 0.5, 37.6, -0.25], [x, 47.6, 0.25]);
      b.box('edge', 15, [x - 0.1, 47.6, -0.22], [x + 1.0, 48.6, 0.22]);
      b.box('edge', 15, [x + 0.8, 48.4, -0.2], [x + 1.6, 49.2, 0.2]);
      b.box('edge', 15, [x - 0.1, 36.6, -0.22], [x + 1.0, 37.6, 0.22]);
      b.box('edge', 15, [x + 0.8, 36.0, -0.2], [x + 1.6, 36.8, 0.2]);
      b.box('gold', 11, [-1.6, 41.2, -0.5], [-1.0, 43.0, 0.5]);
      b.box('steel', 7, [1.0, 41.6, -0.3], [3.4, 42.6, 0.3]);
      b.box('edge', 15, [3.4, 41.8, -0.2], [4.4, 42.4, 0.2]);
    },
    text: "Le ji (戟) des gardes de la Citadelle du Pic céleste : un fer de lance, une lame en croissant d'un côté (la lune sur les pics) et un ergot de l'autre, sur une douille azur aux trois pics. Hampe marine à bagues d'or, talon pointu. Plus lourde et plus menaçante, pour les postes de garde.",
  },
  snow_lance: {
    name: 'Snow Peak Lance', zh: '雪峰枪', fr: 'Lance du Pic enneigé',
    colours: { ...ALLIANCE, wrap: '#E9D9A6' },
    build(b) {
      shaft(b, 'azure', 0, 44, 1.2, 7);
      b.box('wrap', 9, [-0.75, 10, -0.75], [0.75, 17, 0.75]);
      band(b, 'gold', 9.4, 0.6, 1.7); band(b, 'gold', 17, 0.6, 1.7); band(b, 'gold', 30, 0.6, 1.7);
      b.box('gold', 9, [-0.8, -1.6, -0.8], [0.8, 0, 0.8]);
      // The ferrule shaped as the peaks: stepped snow over ice, widest at the bottom.
      b.box('ice', 9, [-1.5, 43.4, -1.5], [1.5, 44.4, 1.5]);
      b.box('snow', 12, [-1.2, 44.4, -1.2], [1.2, 45.4, 1.2]);
      b.box('snow', 14, [-0.8, 45.4, -0.8], [0.8, 46.2, 0.8]);
      // The head: the flag's sword, a long straight blade on a gold guard.
      b.box('gold', 12, [-2.0, 46.2, -0.6], [2.0, 47.0, 0.6]);
      b.box('gold', 10, [-2.4, 46.4, -0.4], [-2.0, 47.4, 0.4]);
      b.box('gold', 10, [2.0, 46.4, -0.4], [2.4, 47.4, 0.4]);
      b.box('steel', 9, [-0.8, 47.0, -0.3], [0.8, 56, 0.3]);
      b.box('edge', 15, [-0.8, 47.0, -0.2], [-0.5, 56, 0.2]);
      b.box('edge', 15, [0.5, 47.0, -0.2], [0.8, 56, 0.2]);
      b.box('ice', 8, [-0.15, 47.0, -0.35], [0.15, 55, 0.35]);
      b.box('edge', 15, [-0.5, 56, -0.2], [0.5, 57.2, 0.2]);
      b.box('edge', 15, [-0.2, 57.2, -0.15], [0.2, 58, 0.15]);
    },
    text: "Une longue lance de cavalier, fine et élancée, dont le fer est l'épée du drapeau : lame droite sur une garde d'or, plantée dans une virole en pics enneigés. Hampe azur, poignée crème. La plus noble : pour les cavaliers ou les disciples d'élite de l'art Celestial Peak Spear.",
  },
};

// ---------------------------------------------------------------- projects

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
        faces: Object.fromEntries(FACES.map(f => [f, { uv, texture }])), type: 'cube', uuid: id('spear/' + key + n + '/' + dx),
      };
    }),
  };
}

function spearProject(key, style) {
  const { pal, elements } = elementsOf(key, style, 0);
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: 'alliance_spear_' + key, model_identifier: '',
    visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements, outliner: elements.map(e => e.uuid), textures: [textureOf(key + '.png', pal.png, 0)],
  };
}

/** The spear standing beside a man (the Mountain Bandit's model), at his right. */
function sizeProject(key, style) {
  const man = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'mountain_bandit.bbmodel'), 'utf8'));
  const n = man.textures.length;
  const { pal, elements } = elementsOf(key, style, n, -12);
  return { ...man, name: 'man_with_' + key, elements: [...man.elements, ...elements], outliner: [...man.outliner, ...elements.map(e => e.uuid)],
    textures: [...man.textures, textureOf(key + '.png', pal.png, n)] };
}

/** The whole spear from the front and three quarters, its head up close from the front and the side, and from behind. */
/** Its head framed from the top of its parts: the styles' heads stand at different heights. */
function viewsOf(style) {
  const b = builder(); style.build(b);
  const top = Math.max(...b.parts.map(p => p.to[1])), h = top - 6;
  return {
    front: [[0, 28, -118], [0, 26, 0]], three: [[-80, 40, -80], [0, 26, 0]],
    head: [[0, h + 1, -32], [0, h, 0]], head_side: [[-32, h + 1, 0.01], [0, h, 0]], head_three: [[24, h + 5, -24], [0, h, 0]],
  };
}
const SIZE_VIEW = [[-28, 30, -96], [-6, 24, 0]];

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, spearProject(key, style), viewsOf(style), v => path.join(OUT, `alliance-spear-${key}-${v}.png`), [420, 600]);
    await render(bb, sizeProject(key, style), { size: SIZE_VIEW }, v => path.join(OUT, `alliance-spear-${key}-${v}.png`), [480, 600]);
    console.log('rendered', key);
  }
  bb.close();
}

module.exports = { STYLES };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
