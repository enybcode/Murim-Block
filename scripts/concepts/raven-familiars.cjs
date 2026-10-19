// Concept sheets for the Raven Art's familiar (docs/combat/styles/raven-art.md, to come): three crows to choose from,
// built in Blockbench pixels (a parrot's size, about 12 pixels tall), rendered from the side, three quarters, the front,
// and once on a man's shoulder for its size, to docs/combat/styles/images/raven-<style>-<view>.png. Concepts, not game
// files. Same helpers as the spear and axe sheets (chief-axes.cjs).
//   node scripts/concepts/raven-familiars.cjs [--only=shadow,sun] [--port=9244]
const fs = require('node:fs');
const path = require('node:path');
const { palette, builder, textureOf, connect, render, id } = require('./chief-axes.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9244);

/**
 * A crow standing, facing -z (its beak toward -z), its feet at y 0, centred on x 0. Materials: c.body (the plumage),
 * c.wing (the folded wings, darker), 'sheen' (the blue or violet gloss on the coverts and the crown), 'beak', 'leg',
 * 'eye', 'glint'; c.tip (optional) paints the tips of the primaries and the tail.
 */
function crow(b, c) {
  // Thin legs, three-toed feet.
  for (const s of [-1, 1]) {
    b.box('leg', 6, [s * 0.9 - 0.25, 0, 0.1], [s * 0.9 + 0.25, 2.4, 0.6]);
    b.box('leg', 4, [s * 0.9 - 0.25, 0, -1.2], [s * 0.9 + 0.25, 0.3, 0.6]);
    b.box('leg', 4, [s * 0.9 - 0.7, 0, -0.6], [s * 0.9 + 0.7, 0.3, -0.2]);
  }
  // A long, low body: the breast forward, the back sloping down to the tail.
  b.box(c.body, 6, [-1.9, 2.3, -1.4], [1.9, 5.6, 3.0]);
  b.box(c.body, 8, [-1.7, 2.7, -2.3], [1.7, 5.3, -1.3]);
  b.box(c.body, 5, [-1.6, 4.9, 2.6], [1.6, 5.8, 4.4]);
  b.box(c.body, 4, [-1.5, 2.1, 0.6], [1.5, 2.6, 3.2]);
  // The wedge tail: the middle feathers longest.
  for (const [x, len, sh] of [[-1.1, 4.0, 3], [-0.45, 4.8, 4], [0.45, 4.8, 4], [1.1, 4.0, 3]]) {
    b.box(c.wing, sh, [x - 0.4, 4.7, 4.0], [x + 0.4, 5.2, 4.0 + len]);
    if (c.tip) b.box(c.tip, 12, [x - 0.4, 4.7, 4.0 + len - 0.6], [x + 0.4, 5.22, 4.0 + len]);
  }
  // The folded wings: coverts glossed on top, the secondaries, the long primaries crossing over the tail.
  for (const s of [-1, 1]) {
    const x0 = s > 0 ? 1.8 : -2.35, x1 = s > 0 ? 2.35 : -1.8;
    b.box(c.wing, 5, [x0, 2.9, -1.0], [x1, 5.5, 3.2]);
    b.box('sheen', 9, [x0 - (s > 0 ? 0 : 0.05), 4.6, -0.9], [x1 + (s > 0 ? 0.05 : 0), 5.55, 1.6]);
    b.box(c.wing, 3, [s * 1.7 - 0.35, 3.6, 3.0], [s * 1.7 + 0.35, 5.0, 5.4]);
    b.box(c.wing, 2, [s * 1.4 - 0.3, 3.9, 5.4], [s * 1.4 + 0.3, 4.9, 7.4]);
    if (c.tip) b.box(c.tip, 12, [s * 1.4 - 0.3, 3.9, 6.8], [s * 1.4 + 0.3, 4.9, 7.4]);
  }
  // The head, small and forward; a glossy crown; the shaggy throat.
  b.box(c.body, 7, [-1.35, 5.2, -4.1], [1.35, 7.8, -1.5]);
  b.box('sheen', 10, [-1.25, 7.6, -3.9], [1.25, 7.95, -1.8]);
  for (const [x, y] of [[-0.7, 4.6], [0, 4.3], [0.7, 4.6]]) b.box(c.body, 4, [x - 0.4, y, -3.6], [x + 0.4, y + 1.0, -2.6]);
  // The heavy beak: a deep base, a hooked tip, the lower mandible, the bristles over the nostrils.
  b.box('beak', 8, [-0.7, 6.0, -5.7], [0.7, 7.3, -4.0]);
  b.box('beak', 10, [-0.45, 6.15, -6.6], [0.45, 7.0, -5.7]);
  b.box('beak', 6, [-0.3, 5.9, -6.9], [0.3, 6.5, -6.4]);
  b.box('beak', 5, [-0.55, 5.75, -5.4], [0.55, 6.1, -4.0]);
  b.box(c.body, 3, [-0.75, 6.9, -4.6], [0.75, 7.4, -3.9]);
  // The eyes, a glint in each.
  for (const s of [-1, 1]) {
    const lo = s > 0 ? 1.35 : -1.41, hi = s > 0 ? 1.41 : -1.35;
    b.box('eye', 13, [lo, 6.7, -3.4], [hi, 7.3, -2.8]);
    b.box('glint', 15, [lo + s * 0.01, 7.05, -3.35], [hi + s * 0.01, 7.25, -3.15]);
  }
}

const STYLES = {
  shadow: {
    name: 'Shadow Raven', fr: 'Corbeau d\'ombre',
    colours: { feather: '#262833', wing: '#1c1e27', sheen: '#3d5274', beak: '#33343a', leg: '#3a3a42', eye: '#f2b33d', glint: '#ffffff', ring: '#d9a83f', scroll: '#e9dcb4' },
    build(b) {
      crow(b, { body: 'feather', wing: 'wing' });
      // A messenger's tube on its back, a gold ring on a leg.
      b.box('scroll', 10, [-0.6, 5.6, 0.3], [0.6, 6.5, 2.9]);
      b.box('ring', 12, [-0.7, 5.5, 0.2], [0.7, 6.6, 0.55]);
      b.box('ring', 12, [-0.7, 5.5, 2.65], [0.7, 6.6, 3.0]);
      b.box('ring', 12, [0.6, 1.2, 0.0], [1.2, 1.6, 0.7]);
    },
    text: 'Un grand corbeau noir aux yeux d\'ambre, plumage à reflets bleu nuit, gorge hérissée. Sur le dos, l\'étui à message cerclé d\'or du messager ; un anneau d\'or à la patte. Sobre et réaliste.',
  },
  sun: {
    name: 'Three-Legged Sun Crow', fr: 'Corbeau solaire à trois pattes',
    colours: { feather: '#1e1e24', wing: '#17171c', sheen: '#4a2a2e', beak: '#d9a83f', leg: '#b8862e', eye: '#ff5a1e', glint: '#fff0c0', crest: '#c81e1a', gold: '#f0c24a' },
    build(b) {
      crow(b, { body: 'feather', wing: 'wing', tip: 'gold' });
      // The third leg, in the middle, a little behind.
      b.box('leg', 6, [-0.25, 0, 0.9], [0.25, 2.3, 1.4]);
      b.box('leg', 4, [-0.6, 0, 0.5], [0.6, 0.3, 1.5]);
      // Two crimson plumes swept back from the crown, a gold collar.
      for (const x of [-0.45, 0.45]) for (let i = 0; i < 4; i++)
        b.box('crest', 9 + i, [x - 0.2, 7.9 + i * 0.35, -2.4 + i * 0.75], [x + 0.2, 8.25 + i * 0.35, -1.6 + i * 0.75]);
      b.box('gold', 11, [-1.4, 5.0, -3.2], [1.4, 5.4, -1.6]);
    },
    text: 'Le corbeau à trois pattes des légendes chinoises, messager du soleil : plumage d\'un noir profond, bec et pattes d\'or, yeux de braise, crête de trois plumes pourpres, collier d\'or, pointes des ailes et de la queue dorées. Le plus « wuxia ».',
  },
  spirit: {
    name: 'Spirit Raven', fr: 'Corbeau spectral',
    colours: { feather: '#2a2038', wing: '#1f172c', sheen: '#5a3e8a', beak: '#3a2e4a', leg: '#2e2638', eye: '#7ff0ff', glint: '#ffffff', wisp: '#6a4e9a', glow: '#9ff6ff' },
    build(b) {
      crow(b, { body: 'feather', wing: 'wing', tip: 'glow' });
      // Smoky wisps rising from its back and trailing from its tail.
      for (const [x, y, z, h] of [[-0.9, 5.6, 0.8, 1.6], [0.6, 5.7, 2.0, 2.2], [-0.1, 5.6, 3.2, 1.3]]) for (let i = 0; i < 3; i++)
        b.box('wisp', 6 + i * 2, [x - 0.25 + i * 0.15, y + i * h / 3, z - 0.25 + i * 0.3], [x + 0.25 + i * 0.15, y + (i + 1) * h / 3, z + 0.25 + i * 0.3]);
      for (const [x, z] of [[-0.6, 8.6], [0.5, 9.2]]) b.box('wisp', 7, [x - 0.25, 4.7, z], [x + 0.25, 5.1, z + 1.4]);
    },
    text: 'Un corbeau fait d\'ombre et de Qi : plumage violet-noir, des volutes de fumée qui montent de son dos et s\'étirent derrière sa queue, yeux et pointes des plumes d\'un cyan lumineux (brilleraient dans le noir). Le plus « surnaturel ».',
  },
};

// ---------------------------------------------------------------- projects

const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];

function elementsOf(key, style, texture, dx = 0, dy = 0) {
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
        from: [p.from[0] + dx, p.from[1] + dy, p.from[2]], to: [p.to[0] + dx, p.to[1] + dy, p.to[2]], autouv: 0, color: 0,
        origin: [dx, dy, 0], rotation: [0, 0, 0],
        faces: Object.fromEntries(FACES.map(f => [f, { uv, texture }])), type: 'cube', uuid: id('raven/' + key + n + '/' + dx + dy),
      };
    }),
  };
}

function crowProject(key, style) {
  const { pal, elements } = elementsOf(key, style, 0);
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false }, name: 'raven_' + key, model_identifier: '',
    visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements, outliner: elements.map(e => e.uuid), textures: [textureOf(key + '.png', pal.png, 0)],
  };
}

/** The crow perched on a man's shoulder (the Mountain Bandit's model): his right shoulder is at about x -6, y 24. */
function shoulderProject(key, style) {
  const man = JSON.parse(fs.readFileSync(path.join(ROOT, 'art', 'blockbench', 'mountain_bandit.bbmodel'), 'utf8'));
  const n = man.textures.length;
  const { pal, elements } = elementsOf(key, style, n, -5.5, 24.5);
  return { ...man, name: 'man_with_' + key, elements: [...man.elements, ...elements], outliner: [...man.outliner, ...elements.map(e => e.uuid)],
    textures: [...man.textures, textureOf(key + '.png', pal.png, n)] };
}

const VIEWS = { side: [[-30, 7, 0.01], [0, 4.6, 1.2]], three: [[-19, 12, -19], [0, 4.6, 1]], front: [[0, 8, -26], [0, 5, 0]] };
const SHOULDER_VIEW = [[-30, 30, -44], [-4, 24, 0]];

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : Object.keys(STYLES);
  const bb = await connect(PORT);
  for (const key of only) {
    const style = STYLES[key];
    await render(bb, crowProject(key, style), VIEWS, v => path.join(OUT, `raven-${key}-${v}.png`), [420, 420]);
    await render(bb, shoulderProject(key, style), { shoulder: SHOULDER_VIEW }, v => path.join(OUT, `raven-${key}-${v}.png`), [420, 520]);
    console.log('rendered', key);
  }
  bb.close();
}

module.exports = { STYLES, crow };
if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
