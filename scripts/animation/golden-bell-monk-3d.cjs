// The Golden Bell monk (金钟罩僧), a Shaolin-style warrior monk who keeps the Golden Bell's sutra in the Misty Bamboo
// Temple (docs/mobs/golden-bell-monk.md), in 3D on Epic Fight's biped skeleton, all in cloth: a shaved head with the
// nine ordination dots, a 16 x 16 face (hd-face.cjs), a saffron robe crossed right over left on a pale under-robe, a
// brown sash knotted in front with its two tails, a long string of dark wooden prayer beads with a red tassel, short
// sleeves to the elbow, bare forearms in off-white cloth wraps, loose saffron trousers bound at the shins with the same
// wraps, black cloth shoes.
// Two outfits on one mesh: `monk`, his own skin, and `gold`, every skin texel gold for the Golden Bell form (the face
// too), each with client/MobFaces' ten expressions.
//   art/blockbench/golden_bell_monk.bbmodel (+ golden_bell_monk/<outfit>.png)        the Blockbench project;
//   assets/murimblock/animmodels/entity/golden_bell_monk.json                         the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/golden_bell_monk.png (+ _<face>.png)            his atlases;
//   assets/murimblock/textures/entity/golden_bell_monk_gold.png (+ _gold_<face>.png)  the golden form's.
// Usage: node scripts/animation/golden-bell-monk-3d.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, fringe, wraps, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/** His face: shaved, the ordination dots on the crown, level brows, a calm mouth (a guardian, not a brawler). */
const FACE = { skin: '#c99068', hair: '#2a211b', iris: '#3a2616', marks: [4, 6, 9, 11], brow: 'flat', mouth: 'calm' };
/** The same face in the golden form: gold skin, amber eyes. */
const GOLD_FACE = { ...FACE, skin: '#e3b23c', iris: '#8a4f0e' };

/**
 * Colour letters: S skin, R saffron robe, Y pale under-robe, B brown sash, W cloth wraps, T brown strap, D dark wood
 * (beads), C red tassel, P trousers, F shoes.
 */
const BASE = { R: '#e2861f', Y: '#f3d9a0', B: '#6b4226', W: '#e6dcc4', T: '#7a4e2c', D: '#3b2416', C: '#b0302a', P: '#c4701c', F: '#1b1715' };
const OUTFITS = [
  { key: 'monk', c: { ...BASE, S: FACE.skin } },
  { key: 'gold', c: { ...BASE, S: GOLD_FACE.skin } },
];

const PIECES = [];
const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });

/** Skin, with a polished glint now and then in the golden form. */
const skin = (f, u, v, w, h, o) => (o && o.key === 'gold' && (u * 3 + v * 5) % 11 === 0 ? '#fbe9a0' : 'S');

// ---- head: shaved all round, a shadow at the nape; the face on its plate
piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h, o) => {
  if (f === 'south' && v === h - 1) return 's';
  return skin(f, u, v, w, h, o);
});
// The ordination dots burnt on the crown (the head's hat layer): two rows of three, a shade darker than the skin.
[-2, 0, 2].forEach((x, i) => [-1.4, 1.0].forEach((z, j) =>
  piece('Head', 'hat', 'dot_' + i + j, [x - 0.5, 32, z - 0.5], [x + 0.5, 32.1, z + 0.5], (f) => (f === 'up' ? 's' : null))));
const facePaints = { monk: hd.paint(FACE), gold: hd.paint(GOLD_FACE) };
piece('Head', 'head', 'face', ...hd.PLATE, (f, u, v, w, h, o) => facePaints[o.key](f, u, v, w, h, o), { density: hd.DENSITY });

// ---- body: the saffron robe crossed right over left, the pale under-robe at the neck
piece('Chest', 'torso', 'chest', [-4, 18, -2], [4, 24, 2], (f, u, v, w) => {
  if (f === 'north' && v <= 1 && u >= 3 && u <= 4) return 'Y';
  return isSide(f) && (u + v * 2) % 7 === 0 ? 'r' : 'R';
}, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'torso', 'belly', [-4, 13, -2], [4, 18, 2], (f, u) => (f === 'down' ? 'P' : isSide(f) && u % 3 === 2 ? 'r' : 'R'), { hinge: { y: 18, bone: 'Chest' } });
piece('Chest', 'jacket', 'collar', [-2.6, 23.3, -2.4], [2.6, 24.4, 2.4], rim(all('Y')));
[[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [3.0, 18.7]].forEach(([x, y], i) =>
  piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.45], [x + 1.3, y + 1.0, -2.0], (f, u) => (u === 0 ? 'y' : 'Y')));
// The brown sash, its knot in front and two tails.
piece('Torso', 'jacket', 'sash', [-4.5, 14.2, -2.5], [4.5, 16.4, 2.5], rim((f, u, v) => (isSide(f) && (u + v) % 4 === 0 ? 'b' : 'B')));
piece('Torso', 'jacket', 'sash_knot', [-1.1, 13.9, -3.0], [1.1, 16.7, -2.5], (f, u, v) => (v === 1 ? 'b' : 'B'));
piece('Torso', 'jacket', 'sash_tail_a', [-1.0, 9.6, -3.0], [-0.1, 13.9, -2.65], fringe('B'));
piece('Torso', 'jacket', 'sash_tail_b', [0.2, 10.4, -3.0], [1.1, 13.9, -2.65], fringe('B'));
// The prayer beads: a long loop from the neck down over the chest, a red tassel at its foot.
for (let i = 0; i < 11; i++) {
  const a = Math.PI * (0.08 + 0.84 * i / 10), x = -Math.cos(a) * 3.0, y = 23.6 - Math.sin(a) * 4.6;
  piece('Chest', 'jacket', 'bead_' + i, [x - 0.55, y - 0.55, -2.95], [x + 0.55, y + 0.55, -2.15], (f, u, v) => (u === 0 && v === 0 ? 'd' : 'D'));
}
piece('Chest', 'jacket', 'bead_big', [-0.8, 17.9, -3.15], [0.8, 19.3, -2.2], (f, u, v) => (v === 0 ? 'D' : 'd'));
piece('Torso', 'jacket', 'tassel', [-0.45, 15.6, -3.35], [0.45, 17.9, -3.0], fringe('C'));
// The robe's back hangs a little lower than its front.
piece('Torso', 'jacket', 'robe_back', [-4.3, 9.4, 2.0], [4.3, 13.2, 2.45], (f, u, v, w, h) => (f === 'up' ? null : isSide(f) && v === h - 1 ? 'r' : u % 3 === 1 ? 'r' : 'R'));

// ---- arms: short saffron sleeves to the elbow, bare forearms in cloth wraps with a brown strap, the hands bare
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [a0, a1] = xs(4, 8), [b0, b1] = xs(3.75, 8.25), [c0, c1] = xs(3.6, 8.4);
  piece('Arm_' + side, part + 'Arm', 'upper_' + s, [a0, 17, -2], [a1, 24, 2], skin, { hinge: { y: 17, bone: 'Hand_' + side } });
  piece('Arm_' + side, part + 'Sleeve', 'sleeve_' + s, [c0, 18.2, -2.4], [c1, 24.3, 2.4],
    rim((f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'r' : isSide(f) && u % 3 === 2 ? 'r' : 'R')));
  piece('Hand_' + side, part + 'Arm', 'fore_' + s, [a0, 11, -2], [a1, 17, 2], (f, u, v, w, h, o) => (f === 'down' ? 's' : skin(f, u, v, w, h, o)),
    { hinge: { y: 17, bone: 'Arm_' + side } });
  piece('Hand_' + side, part + 'Sleeve', 'wrap_' + s, [b0, 12.6, -2.25], [b1, 16.6, 2.25], rim(wraps('W', 'T')));
}

// ---- legs: loose saffron trousers, the robe's skirt over the thighs, shin wraps and black cloth shoes
for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
  const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const s = side.toLowerCase();
  const [t0, t1] = xs(0, 4), [k0, k1] = xs(-0.4, 4.4), [w0, w1] = xs(-0.25, 4.25), [d0, d1] = xs(-0.2, 4.2);
  piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], (f, u) => (isSide(f) && u % 3 === 1 ? 'p' : 'P'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], all('P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [k0, 7.6, -2.4], [k1, 13, 2.4],
    (f, u, v, w, h) => (f === 'up' || f === 'down' ? null : v === h - 1 ? 'b' : u % 3 === 2 ? 'r' : 'R'));
  piece('Leg_' + side, part + 'Pants', 'shin_wrap_' + s, [w0, 1.2, -2.25], [w1, 5.4, 2.25], rim(wraps('W', null)));
  piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.7], [d1, 1.3, 2.2], (f, u, v, w, h) => (f === 'down' ? 'f' : 'F'));
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const dir = path.join(assets, 'textures', 'entity');
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'golden_bell_monk'), { recursive: true });
  const atlases = OUTFITS.map(o => kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
  OUTFITS.forEach((o, i) => {
    const base = o.key === 'monk' ? 'golden_bell_monk' : 'golden_bell_monk_gold';
    fs.writeFileSync(path.join(dir, base + '.png'), atlases[i]);
    fs.writeFileSync(path.join(art, 'golden_bell_monk', o.key + '.png'), atlases[i]);
    // One atlas per face; the calm face keeps the plain name.
    for (const face of Object.keys(hd.EXPRESSIONS)) if (face !== 'calm')
      fs.writeFileSync(path.join(dir, base + '_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...o, face }, ATLAS)));
  });
  const m = kit.mesh(PIECES, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'golden_bell_monk.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'golden_bell_monk.bbmodel'), JSON.stringify(kit.bbmodel('golden_bell_monk', PIECES, OUTFITS, atlases, ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Golden Bell monk:', PIECES.length, 'pieces,', OUTFITS.length, 'outfits,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

module.exports = { PIECES, OUTFITS, FACE, GOLD_FACE };
