// The Bandit Chief's armour, for a player to wear (the chief drops a piece now and then:
// data/murimblock/loot_table/entities/bandit_chief.json): his look in four pieces on a man's build, not his bulk.
//   chief_brow_band       an iron brow band with a bronze tiger plaque and ear guards, a bronze hair crown, red ribbons;
//   chief_lamellar_vest   a cuirass of iron lamellae laced with red cord, the studded belt and its tiger buckle, the tiger
//                         skin over the back and the left shoulder (its head on the shoulder, a paw on the chest), the
//                         iron pauldron and its bronze spike on the right, iron bracers;
//   chief_tassets         baggy crimson trousers puffing at the shin, lamellar tassets over the thighs;
//   chief_fur_boots       dark leather boots with fur cuffs.
// Written here:
//   assets/murimblock/animmodels/armor/<piece>.json       each piece's Epic Fight mesh: Epic Fight draws a mod's armour
//                                                         with the mesh it finds under animmodels/armor/<item>, in the
//                                                         texture the mesh names (render_properties);
//   assets/murimblock/textures/models/armor/chief_lamellar_3d.png      their atlas;
//   assets/murimblock/textures/models/armor/chief_lamellar_layer_1/2.png
//                                                         the flat armour Minecraft draws on its own player model (without
//                                                         Epic Fight's): the same look painted on its boxes;
//   assets/murimblock/textures/item/<piece>.png, models/item/<piece>.json   the icons;
//   art/blockbench/chief_armor.bbmodel                    the pieces on the biped's joints, to look at and edit.
// The pieces sit on a player's body (Blockbench pixels: head 8 cube, body 8 x 12 x 4, arms and legs 4 x 12 x 4), bending
// where the bandit's do (bandit-3d.cjs): the waist at 18, the elbows at 17, the knees at 6.
// Usage: node scripts/animation/chief-armor.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, folds, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ATLAS = 128;
const TEXTURE = 'murimblock:textures/models/armor/chief_lamellar_3d.png';

/**
 * Colour letters (the chief's): I iron, J light iron, G bronze, D dark trim, B belt, V leather, R crimson, N red cord,
 * O tiger orange, K tiger stripes, W white (fangs, muzzle, pin), F boots, U fur.
 */
const ARMOUR = { key: 'chief_lamellar', c: {
  I: '#8b9198', J: '#a9afb5', G: '#c39a3c', D: '#2a1a14', B: '#1e1612', V: '#4a2e1e', R: '#6e1f1c', N: '#a7302a',
  O: '#d07a28', K: '#1e1410', W: '#e8dcc6', F: '#3a2618', U: '#8c7a62' } };

// ---------------------------------------------------------------- the pieces

/** The four items, each with its mesh's parts (a chestplate's arms are their own parts, as in Epic Fight's own). */
const ITEMS = {
  chief_brow_band: ['head'],
  chief_lamellar_vest: ['torso', 'rightArm', 'leftArm'],
  chief_tassets: ['torso', 'rightLeg', 'leftLeg'],
  chief_fur_boots: ['rightLeg', 'leftLeg'],
};
const PIECES = [];
const piece = (item, bone, part, name, from, to, paint, extra = {}) => PIECES.push({ item, bone, part, name, from, to, paint, ...extra });

/** Lamellae: rows of small iron plates two texels high, staggered, each row laced with red cord along its foot. */
const lamellae = (f, u, v) => {
  if (!isSide(f)) return 'i';
  if (v % 2 === 1) return u % 2 === 0 ? 'N' : 'i';
  return (u + Math.floor(v / 2)) % 2 === 0 ? 'I' : 'J';
};
/** Iron plate: rivets along the top row, a dark lower edge. */
const plate = (f, u, v, w, h) => (isSide(f) && v === 0 && u % 2 ? 'G' : isSide(f) && v === h - 1 ? 'i' : 'I');
/** Tiger fur: orange, wavy black bands across it (broken here and there), the last row white. */
const WAVE = [0, 0, 1, 1, 1, 0];
const tiger = (f, u, v, w, h) => {
  if (isSide(f) && v === h - 1) return 'W';
  const band = (v + WAVE[u % 6]) % 4;
  return band === 0 && u % 7 !== 3 ? 'K' : band === 1 && u % 3 === 0 ? 'o' : 'O';
};
/** A tiger's face in bronze, three rows: eyes, nose, fangs. */
const tigerFace = (f, u, v, w) => {
  if (f !== 'north') return 'g';
  if (v === 0) return u === 0 || u === w - 1 ? 'K' : 'G';
  if (v === 1) return u > 0 && u < w - 1 ? 'k' : 'G';
  return u % 2 ? 'W' : 'g';
};
const mirror = ([x0, y0, z0], [x1, y1, z1]) => [[-x1, y0, z0], [-x0, y1, z1]];

// ---- the brow band: iron, its tiger plaque in front, ear guards, a bronze crown on top, red ribbons behind
const HEAD = 'chief_brow_band';
piece(HEAD, 'Head', 'head', 'band', [-4.5, 27.6, -4.5], [4.5, 29.8, 4.5], rim(plate));
piece(HEAD, 'Head', 'head', 'plaque', [-1.6, 27.2, -4.95], [1.6, 30.2, -4.45], tigerFace);
for (const [name, x0, x1] of [['guard_r', 4.45, 4.95], ['guard_l', -4.95, -4.45]])
  piece(HEAD, 'Head', 'head', name, [x0, 24.6, -2.2], [x1, 27.8, 1.8], (f, u, v, w, h) => (v === h - 1 ? 'i' : v === 0 && u === 2 ? 'G' : 'I'));
piece(HEAD, 'Head', 'head', 'crown', [-1.3, 32, -0.4], [1.3, 33.4, 2.0], (f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'g' : 'G'));
piece(HEAD, 'Head', 'head', 'pin', [-2.4, 32.5, 0.6], [2.4, 32.9, 1.0], () => 'W');
piece(HEAD, 'Head', 'head', 'ribbon_knot', [-1.1, 27.4, 4.45], [1.1, 29.9, 5.2], (f, u) => (u % 2 ? 'n' : 'N'));
piece(HEAD, 'Head', 'head', 'ribbon_l', [-1.6, 24.0, 4.6], [-0.6, 27.6, 5.0], fringe('N'), { tilt: -18, pivot: [-1.1, 27.6, 4.8] });
piece(HEAD, 'Head', 'head', 'ribbon_r', [0.6, 24.6, 4.6], [1.6, 27.6, 5.0], fringe('N'), { tilt: -26, pivot: [1.1, 27.6, 4.8] });

// ---- the lamellar vest
const VEST = 'chief_lamellar_vest';
piece(VEST, 'Chest', 'torso', 'chest_plates', [-4.6, 18, -2.6], [4.6, 24.2, 2.6], rim(lamellae), { hinge: { y: 18, bone: 'Torso' } });
piece(VEST, 'Torso', 'torso', 'belly_plates', [-4.6, 13.4, -2.6], [4.6, 18, 2.6], rim(lamellae), { hinge: { y: 18, bone: 'Chest' } });
piece(VEST, 'Chest', 'torso', 'collar', [-3.2, 23.4, -2.9], [3.2, 24.6, 2.9], rim((f, u) => (isSide(f) && u % 3 === 1 ? 'G' : 'V')));
piece(VEST, 'Torso', 'torso', 'belt', [-4.9, 12.2, -2.95], [4.9, 14.6, 2.95], rim((f, u, v) => (isSide(f) && v === 1 && u % 3 === 1 ? 'G' : 'B')));
piece(VEST, 'Torso', 'torso', 'buckle', [-1.8, 11.8, -3.45], [1.8, 15.0, -2.9], tigerFace);
piece(VEST, 'Chest', 'torso', 'pelt_back', [-4.8, 13.0, 2.6], [4.7, 24.6, 3.3], tiger);
piece(VEST, 'Chest', 'torso', 'tiger_paw', [-4.2, 15.8, -3.25], [-2.8, 21.4, -2.6], (f, u, v, w, h) => (v === h - 1 ? (u % 2 ? 'W' : 'K') : tiger(f, u, v, w, h)));
// The right shoulder: the iron pauldron in two plates and its bronze spike.
piece(VEST, 'Arm_R', 'rightArm', 'pauldron', [3.3, 21.4, -2.85], [8.9, 24.9, 2.85], rim(plate));
piece(VEST, 'Arm_R', 'rightArm', 'pauldron_lame', [3.7, 19.9, -2.65], [8.7, 21.5, 2.65], rim((f, u, v, w, h) => (v === h - 1 ? 'i' : 'I')));
piece(VEST, 'Arm_R', 'rightArm', 'pauldron_spike', [5.4, 24.9, -0.6], [6.6, 26.5, 0.6], (f, u, v) => (v === 0 ? 'W' : 'G'));
// The left shoulder: the tiger skin and its head.
piece(VEST, 'Arm_L', 'leftArm', 'pelt_shoulder', [-8.9, 20.6, -2.95], [-3.3, 24.9, 2.95], rim(tiger));
piece(VEST, 'Arm_L', 'leftArm', 'tiger_head', [-8.5, 24.3, -4.0], [-4.5, 27.5, 0.6], (f, u, v) => {
  if (f === 'north') {
    // Seen from the front: stripes on the brow, yellow eyes, a white muzzle and a dark nose.
    if (v === 0) return u % 2 ? 'K' : 'O';
    if (v === 1) return u === 1 || u === 2 ? 'G' : 'O';
    if (v === 2) return u === 1 || u === 2 ? 'W' : 'O';
    return u === 1 || u === 2 ? 'k' : 'K';
  }
  if (f === 'down') return 'W';
  return (u + v) % 3 === 0 ? 'K' : 'O';
});
piece(VEST, 'Arm_L', 'leftArm', 'tiger_jaw', [-8.1, 23.6, -4.3], [-4.9, 24.4, -1.0], (f, u) => (f === 'north' ? (u % 2 ? 'W' : 'K') : 'O'));
piece(VEST, 'Arm_L', 'leftArm', 'tiger_ear_r', [-5.7, 27.5, -1.0], [-4.6, 28.6, 0.0], (f) => (f === 'north' ? 'K' : 'O'));
piece(VEST, 'Arm_L', 'leftArm', 'tiger_ear_l', [-8.4, 27.5, -1.0], [-7.3, 28.6, 0.0], (f) => (f === 'north' ? 'K' : 'O'));
// Iron bracers on both forearms.
const bracer = rim((f, u, v, w, h) => (v === 0 || v === h - 1 ? 'D' : isSide(f) && v === 1 && u % 2 ? 'G' : 'I'));
piece(VEST, 'Hand_R', 'rightArm', 'bracer_r', [3.6, 12.4, -2.4], [8.4, 16.2, 2.4], bracer);
piece(VEST, 'Hand_L', 'leftArm', 'bracer_l', ...mirror([3.6, 12.4, -2.4], [8.4, 16.2, 2.4]), bracer);

// ---- the tassets: crimson trousers, lamellar plates over the front and the outer side of each thigh
const LEGS = 'chief_tassets';
piece(LEGS, 'Torso', 'torso', 'waist', [-4.4, 11.6, -2.35], [4.4, 13.6, 2.35], rim(folds('R')));
for (const [side, part] of [['R', 'rightLeg'], ['L', 'leftLeg']]) {
  const at = (from, to) => (side === 'R' ? [from, to] : mirror(from, to));
  const s = side.toLowerCase();
  piece(LEGS, 'Thigh_' + side, part, 'thigh_' + s, ...at([-0.3, 6, -2.35], [4.3, 12.4, 2.35]), folds('R'), { hinge: { y: 6, bone: 'Leg_' + side } });
  piece(LEGS, 'Leg_' + side, part, 'shin_' + s, ...at([-0.3, 2.2, -2.35], [4.3, 6, 2.35]), folds('R'), { hinge: { y: 6, bone: 'Thigh_' + side } });
  piece(LEGS, 'Leg_' + side, part, 'puff_' + s, ...at([-0.65, 3.4, -2.7], [4.65, 5.8, 2.7]), rim((f, u, v, w, h) => (v === h - 1 || u % 3 === 2 ? 'r' : 'R')));
  piece(LEGS, 'Thigh_' + side, part, 'tasset_front_' + s, ...at([0.0, 7.4, -3.0], [4.4, 12.6, -2.4]), lamellae);
  piece(LEGS, 'Thigh_' + side, part, 'tasset_side_' + s, ...at([4.35, 7.4, -2.3], [4.95, 12.6, 2.3]), lamellae);
}

// ---- the fur boots
const FEET = 'chief_fur_boots';
for (const [side, part] of [['R', 'rightLeg'], ['L', 'leftLeg']]) {
  const at = (from, to) => (side === 'R' ? [from, to] : mirror(from, to));
  const s = side.toLowerCase();
  piece(FEET, 'Leg_' + side, part, 'boot_' + s, ...at([-0.55, 0, -2.6], [4.55, 3.0, 2.6]), (f, u, v, w, h) => (f === 'down' || (isSide(f) && v === h - 1) ? 'f' : 'F'));
  piece(FEET, 'Leg_' + side, part, 'toe_' + s, ...at([-0.55, 0, -3.3], [4.55, 1.3, -2.6]), (f) => (f === 'down' ? 'f' : 'F'));
  piece(FEET, 'Leg_' + side, part, 'strap_' + s, ...at([-0.6, 1.4, -2.65], [4.6, 1.9, 2.65]), rim((f, u) => (isSide(f) && u === 2 ? 'G' : 'D')));
  piece(FEET, 'Leg_' + side, part, 'cuff_' + s, ...at([-0.75, 2.7, -2.8], [4.75, 4.0, 2.8]), rim((f, u) => (u % 2 ? 'u' : 'U')));
}

// ---------------------------------------------------------------- the flat armour (Minecraft's own player model)

/**
 * Paints a box laid out the vanilla way (u, v its corner; w, h, d its width, height and depth) in a 64 x 32 armour
 * texture: paint(face, x, y, fw, fh) -> a colour letter or null, x and y as the face is seen from outside.
 */
function box(img, u, v, w, h, d, paint) {
  const faces = { up: [u + d, v, w, d], down: [u + d + w, v, w, d], east: [u, v + d, d, h], north: [u + d, v + d, w, h],
    west: [u + d + w, v + d, d, h], south: [u + d + w + d, v + d, w, h] };
  for (const [f, [x0, y0, fw, fh]] of Object.entries(faces))
    for (let y = 0; y < fh; y++) for (let x = 0; x < fw; x++) {
      const c = kit.colour(paint(f, x, y, fw, fh), ARMOUR, f, x, y, 3);
      if (c) img.set(c, ((y0 + y) * 64 + x0 + x) * 4);
    }
}

function flatLayers() {
  const one = new Uint8Array(64 * 32 * 4), two = new Uint8Array(64 * 32 * 4);
  // Head: the band two rows high, the plaque in front, the ear guards, the crown on top.
  box(one, 0, 0, 8, 8, 8, (f, x, y, w, h) => {
    if (f === 'up') return x >= 3 && x <= 4 && y >= 3 && y <= 5 ? 'G' : null;
    if (f === 'down') return null;
    if (f === 'north' && x >= 3 && x <= 4 && y >= 1 && y <= 4) return y === 1 ? 'K' : y === 4 ? 'W' : 'G';
    if (y === 2) return x % 2 ? 'G' : 'I';
    if (y === 3) return 'i';
    if ((f === 'east' || f === 'west') && x >= 3 && x <= 5 && y >= 4 && y <= 6) return y === 6 ? 'i' : 'I';
    return null;
  });
  // Body: lamellae, the belt and its buckle in front, the tiger skin behind.
  box(one, 16, 16, 8, 12, 4, (f, x, y, w, h) => {
    if (f === 'up') return 'V';
    if (f === 'down') return null;
    if (f === 'south') return tiger(f, x, y, w, h);
    if (y >= 9 && y <= 10) return f === 'north' && x >= 3 && x <= 4 ? 'G' : y === 10 && x % 3 === 1 ? 'G' : 'B';
    if (y === 11) return f === 'north' && x >= 3 && x <= 4 ? 'g' : null;
    return lamellae(f, x, y);
  });
  // Arms (the right one, mirrored for the left): the pauldron on top, the bracer on the forearm.
  box(one, 40, 16, 4, 12, 4, (f, x, y, w, h) => {
    if (f === 'up') return 'I';
    if (f === 'down') return null;
    if (y <= 3) return plate(f, x, y, w, 4);
    if (y >= 7 && y <= 10) return y === 7 || y === 10 ? 'D' : y === 8 && x % 2 ? 'G' : 'I';
    return null;
  });
  // Feet (the boots, on the legs' box): fur cuffs over dark leather.
  box(one, 0, 16, 4, 12, 4, (f, x, y) => {
    if (f === 'up') return null;
    if (f === 'down') return 'f';
    if (y === 8) return x % 2 ? 'u' : 'U';
    if (y === 10) return f === 'north' && x === 2 ? 'G' : 'D';
    return y > 8 ? 'F' : null;
  });
  // Leggings: the crimson waist below the belt, the trousers, the tassets over the thighs.
  box(two, 16, 16, 8, 12, 4, (f, x, y) => (f !== 'up' && f !== 'down' && y >= 10 ? (x % 3 === 2 ? 'r' : 'R') : null));
  box(two, 0, 16, 4, 12, 4, (f, x, y) => {
    if (f === 'up' || f === 'down') return null;
    if ((f === 'north' || f === 'east') && y >= 1 && y <= 5) return lamellae(f, x, y - 1);
    if (y === 7) return 'r';
    return x % 3 === 2 ? 'r' : 'R';
  });
  return [one, two];
}

// ---------------------------------------------------------------- the icons

/** 16 x 16 icons, a letter a pixel (. clear), lit like the rest (a darker lower case). */
const ICONS = {
  chief_brow_band: [
    '................',
    '.......GG.......',
    '.....WWGGWW.....',
    '......gGGg......',
    '................',
    '...iiiiiiiiii...',
    '..iI........Ii..',
    '.iIGIIKGGKIIGIi.',
    '.IIiIIGkkGIIiII.',
    '.iiiiiWgWgiiiii.',
    '.Ii..........iI.',
    '.II...NnnN...II.',
    '.Ii....NN....iI.',
    '..i....nN....i..',
    '.......N.n......',
    '................',
  ],
  chief_lamellar_vest: [
    '................',
    '..IGI.....OKOO..',
    '.IIIIIVVVVOOKOO.',
    '.IiIIJIVVIJOKOO.',
    '.iiIJINJNJIOKO..',
    '..IIJIJIJIOKOO..',
    '...NiNiNiNOKO...',
    '...IJIJIJIOWK...',
    '...NiNiNiNiNi...',
    '...IJIJIJIJIJ...',
    '...BBBKGGKBBB...',
    '...BGBGkkGBGB...',
    '...NiNiWgWgNi...',
    '...IJIJIJIJIJ...',
    '...iiiiiiiiii...',
    '................',
  ],
  chief_tassets: [
    '................',
    '...RRRRRRRRRR...',
    '...rRRRRRRRRr...',
    '..IJIJIRRIJIJI..',
    '..NiNiNRRNiNiN..',
    '..JIJIJrrJIJIJ..',
    '..NiNiNrrNiNiN..',
    '..IJIJI..IJIJI..',
    '..RRRr....rRRR..',
    '..RRRr....rRRR..',
    '..RrRR....RRrR..',
    '.rRRRRr..rRRRRr.',
    '.rrRRrr..rrRRrr.',
    '..RRRr....rRRR..',
    '..rrrr....rrrr..',
    '................',
  ],
  chief_fur_boots: [
    '................',
    '................',
    '................',
    '..UuUuU..uUuUu..',
    '..uUuUu..UuUuU..',
    '...FFF....FFF...',
    '...FFF....FFF...',
    '...FFF....FFF...',
    '...DGD....DGD...',
    '...FFF....FFF...',
    '..FFFF....FFFF..',
    '.FFFFF....FFFFF.',
    '.fffff....fffff.',
    '................',
    '................',
    '................',
  ],
};

function icon(rows) {
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    const c = kit.colour(l, ARMOUR, y < 6 ? 'up' : 'north', x, y, 5);
    if (c) img.set(c, (y * 16 + x) * 4);
  }));
  return img;
}

// ---------------------------------------------------------------- writing it all

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, ARMOUR, ATLAS));
  const armour = path.join(ASSETS, 'textures', 'models', 'armor');
  fs.mkdirSync(armour, { recursive: true });
  fs.writeFileSync(path.join(armour, 'chief_lamellar_3d.png'), atlas);
  const [one, two] = flatLayers();
  fs.writeFileSync(path.join(armour, 'chief_lamellar_layer_1.png'), kit.png(64, 32, one));
  fs.writeFileSync(path.join(armour, 'chief_lamellar_layer_2.png'), kit.png(64, 32, two));
  const meshes = path.join(ASSETS, 'animmodels', 'armor');
  fs.mkdirSync(meshes, { recursive: true });
  for (const [item, parts] of Object.entries(ITEMS)) {
    const m = kit.mesh(PIECES.filter(p => p.item === item), ATLAS, parts);
    fs.writeFileSync(path.join(meshes, item + '.json'), JSON.stringify({ ...m, render_properties: { texture_path: TEXTURE } }));
    fs.writeFileSync(path.join(ASSETS, 'textures', 'item', item + '.png'), kit.png(16, 16, icon(ICONS[item])));
    fs.writeFileSync(path.join(ASSETS, 'models', 'item', item + '.json'),
      JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/' + item } }, null, 2) + '\n');
    console.log('wrote', item + ':', m.vertices.positions.count, 'vertices');
  }
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'chief_armor'), { recursive: true });
  fs.writeFileSync(path.join(art, 'chief_armor', 'chief_lamellar_3d.png'), atlas);
  fs.writeFileSync(path.join(art, 'chief_armor.bbmodel'), JSON.stringify(kit.bbmodel('chief_armor', PIECES, [ARMOUR], [atlas], ATLAS, 'models/armor'), null, 1));
  console.log('wrote the Bandit Chief\'s armour:', PIECES.length, 'pieces, atlas rows used', Math.max(...faces.map(f => f.v + f.h)), 'of', ATLAS);
}

module.exports = { PIECES, ITEMS, ARMOUR };
