// Every animation the Mountain Bandit and the Bandit Chief play in game, on their own 3D models with their weapon in the
// hand, as Blockbench projects to watch: build/blockbench/mountain_bandit_animations.bbmodel and
// bandit_chief_animations.bbmodel. Open one in Blockbench, Animate tab, pick an animation and play.
//   bandit: bandit_idle (its own), walk, run (its chase), its three cuts (drifting_cut, returning_cut, falling_rain), hit_short,
//           hit_long, death;
//   chief:  chief_idle (his own: the axe on the shoulder), walk_twohand (his walk and chase), chief_cleave, chief_sweep,
//           chief_slam, hit_short, death.
// The faces' atlases come too (one texture per outfit and face, named <outfit>_<face>): put one on the head's front to
// see a face; in game client/MobFaces changes them by itself.
// The moves and reactions are Epic Fight's own, read from the installed jar: their authors' assets, so these projects
// stay in build/, never in the repository. The cuts and the chief's attacks are ours (animmodels/animations/biped/murim).
// The weapons are drawn as cubes on the Tool_R bone along the rig's sword line (blockbench-rig.cjs), the edge down as in
// game; the game draws the real item models there.
// Usage: node scripts/animation/mob-animations-preview.cjs
const fs = require('fs');
const path = require('path');
const b = require('./biped.cjs');
const { at, groupsByName, animation } = require('./blockbench-rig.cjs');
const { keysOf } = require('./epicfight-to-blockbench.cjs');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const TEX = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'entity');
const { OUTFITS, FACES: BANDIT_FACES } = require('./bandit-3d.cjs');
const { FACES: CHIEF_FACES } = require('./bandit-chief-3d.cjs');
const OURS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'animmodels', 'animations', 'biped', 'murim');
const EF = name => 'assets/epicfight/animmodels/animations/biped/' + name + '.json';

/** An animation file: ours by name, or Epic Fight's by its path in the jar. */
const load = src => JSON.parse(fs.existsSync(src) ? fs.readFileSync(src, 'utf8') : b.readFromJar(src));

// ---------------------------------------------------------------- the weapons' colours (a 16 x 16 palette texture)

const COLOURS = ['#aab0b6', '#f2f4f6', '#5d646b', '#962e24', '#b08a36', '#4e3220', '#565c63', '#c4382c'];
const [STEEL, EDGE, SPINE, CORD, BRASS, WOOD, IRON, TASSEL] = COLOURS.map((c, i) => i);
const palette = () => {
  const img = new Uint8Array(16 * 16 * 4);
  COLOURS.forEach((c, i) => {
    const rgb = [1, 3, 5].map(k => parseInt(c.slice(k, k + 2), 16));
    for (let y = 0; y < 2; y++) for (let x = 0; x < 2; x++) img.set([...rgb, 255], ((y + 2 * Math.floor(i / 8)) * 16 + (i % 8) * 2 + x) * 4);
  });
  return png(16, 16, img);
};
const uvOf = i => [(i % 8) * 2 + 0.5, Math.floor(i / 8) * 2 + 0.5, (i % 8) * 2 + 1.5, Math.floor(i / 8) * 2 + 1.5];

/** Cubes in the Tool_R bone's frame: the hand at the origin, forward -z, up +y (the edge faces -y). */
function weapon(kind) {
  const [tx, ty, tz] = at('Tool_R');
  const cubes = [];
  const c = (name, [x0, y0, z0], [x1, y1, z1], colour) => cubes.push({ name, from: [tx + x0, ty + y0, tz + z0], to: [tx + x1, ty + y1, tz + z1], colour });
  if (kind === 'dao') {
    c('grip', [-0.6, -0.6, -1], [0.6, 0.6, 5.5], CORD);
    c('pommel', [-0.8, -0.8, 5.5], [0.8, 0.8, 6.5], BRASS);
    c('tassel', [-0.3, -3.6, 5.7], [0.3, -0.8, 6.3], TASSEL);
    c('guard', [-1.9, -1.9, -1.8], [1.9, 1.9, -1], BRASS);
    // The blade: the spine straight on top, the edge swelling below toward a clipped point.
    [0.85, 1.05, 1.3, 1.55, 1.85, 2.1].forEach((drop, i) => {
      c('blade_' + i, [-0.3, -drop, -1.8 - (i + 1) * 2.3], [0.3, 0.75, -1.8 - i * 2.3], STEEL);
      c('edge_' + i, [-0.15, -drop - 0.2, -1.8 - (i + 1) * 2.3], [0.15, -drop, -1.8 - i * 2.3], EDGE);
    });
    c('tip', [-0.3, -0.6, -17.4], [0.3, 0.75, -15.6], STEEL);
  } else {
    // The great axe: a long haft through both hands, the crescent below its far end, a spike on top, a point ahead.
    c('haft', [-0.6, -0.6, -26], [0.6, 0.6, 9], WOOD);
    c('grip', [-0.75, -0.75, -2], [0.75, 0.75, 7], CORD);
    c('pommel', [-0.9, -0.9, 9], [0.9, 0.9, 10.2], IRON);
    c('socket', [-0.9, -1.4, -28], [0.9, 1.4, -19], IRON);
    c('point', [-0.4, -0.4, -31], [0.4, 0.4, -28], IRON);
    [[-1.4, -3.2, -29, -18], [-3.2, -5.2, -30, -17], [-5.2, -7.0, -31, -16]].forEach(([y1, y0, z0, z1], i) =>
      c('blade_' + i, [-0.35, y0, z0], [0.35, y1, z1], STEEL));
    c('edge', [-0.2, -7.8, -31.5], [0.2, -7.0, -15.5], EDGE);
    c('spike', [-0.3, 1.4, -24.5], [0.3, 4.6, -22.5], IRON);
    c('tassel', [-0.3, -5.0, -18.6], [0.3, -1.4, -18.0], TASSEL);
  }
  return cubes;
}

/** A mob's model project (mesh-kit.cjs) with the weapon on Tool_R, the palette texture and the animations. */
function build(modelFile, kind, anims, out, faces = []) {
  const project = JSON.parse(fs.readFileSync(modelFile, 'utf8'));
  // The faces' atlases: listed after the model's own textures, used by no face until one is put on the head.
  faces.forEach(([name, file], i) => project.textures.push({ ...project.textures[0], name: name + '.png', id: String(project.textures.length),
    uuid: '0f0f0f0f-0000-4000-a000-' + String(i).padStart(12, '0'), source: 'data:image/png;base64,' + fs.readFileSync(file).toString('base64') }));
  const tex = project.textures.length;
  project.textures.push({ ...project.textures[0], name: 'weapon.png', id: String(tex), width: 16, height: 16, uv_width: 16, uv_height: 16,
    uuid: '0e0e0e0e-0000-4000-a000-' + String(tex).padStart(12, '0'), source: 'data:image/png;base64,' + palette().toString('base64') });
  const groups = groupsByName(project.outliner);
  const tool = groups.Tool_R;
  weapon(kind).forEach((cube, i) => {
    const uuid = '0e0e0e0e-0000-4000-b000-' + String(i).padStart(12, '0');
    const uv = uvOf(cube.colour);
    project.elements.push({ name: kind + '_' + cube.name, box_uv: false, rescale: false, locked: false, render_order: 'default',
      allow_mirror_modeling: true, from: cube.from, to: cube.to, autouv: 0, color: 0, origin: at('Tool_R'), rotation: [0, 0, 0],
      faces: Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map(f => [f, { uv, texture: tex }])), type: 'cube', uuid });
    tool.children.push(uuid);
  });
  project.animations = Object.entries(anims).map(([name, { src, loop }]) => animation(name, keysOf(load(src)), groups, loop ? 'loop' : 'once'));
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, JSON.stringify(project));
  console.log('wrote', path.relative(ROOT, out), '-', Object.keys(anims).join(', '));
}

if (require.main === module) {
  const art = path.join(ROOT, 'art', 'blockbench'), out = path.join(ROOT, 'build', 'blockbench');
  const ours = name => ({ src: path.join(OURS, name + '.json') });
  build(path.join(art, 'mountain_bandit.bbmodel'), 'dao', {
    bandit_idle: { ...ours('bandit_idle'), loop: true }, walk: { src: EF('living/walk'), loop: true }, run: { src: EF('living/run'), loop: true },
    drifting_cut: ours('drifting_cut'), returning_cut: ours('returning_cut'), falling_rain: ours('falling_rain'),
    hit_short: { src: EF('combat/hit_short') }, hit_long: { src: EF('combat/hit_long') }, death: { src: EF('living/death') },
  }, path.join(out, 'mountain_bandit_animations.bbmodel'),
  OUTFITS.flatMap(o => Object.keys(BANDIT_FACES).filter(f => f !== 'calm').map(f => [o.key + '_' + f, path.join(TEX, 'mountain_bandit', '3d', o.key + '_' + f + '.png')])));
  build(path.join(art, 'bandit_chief.bbmodel'), 'axe', {
    chief_idle: { ...ours('chief_idle'), loop: true }, walk_twohand: { src: EF('living/walk_twohand'), loop: true },
    chief_cleave: ours('chief_cleave'), chief_sweep: ours('chief_sweep'), chief_slam: ours('chief_slam'),
    hit_short: { src: EF('combat/hit_short') }, death: { src: EF('living/death') },
  }, path.join(out, 'bandit_chief_animations.bbmodel'),
  Object.keys(CHIEF_FACES).filter(f => f !== 'calm').map(f => ['chief_' + f, path.join(TEX, 'bandit_chief_' + f + '.png')]));
}
