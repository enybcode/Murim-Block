// The Blood Soldiers (blood-soldier-3d.cjs) and their armour in game (Enzo, 17/10/2026: the lacquered armour, "on va la
// faire droppable"):
//   the Blood Lacquer set, four pieces a player can wear (com.murimblock.item.MurimItems):
//     blood_lacquer_mask      the iron face guard with its fangs, and its sides;
//     blood_lacquer_cuirass   the lacquered breast and back, the collar, the flaring pauldrons, the bracers;
//     blood_lacquer_tassets   the tassets over the thighs, the knee guards;
//     blood_lacquer_greaves   the greaves and the iron toe caps;
//   written as Epic Fight meshes (animmodels/armor/<piece>.json) on one atlas (textures/models/armor/blood_lacquer_3d.png),
//   Minecraft's flat armour layers (blood_lacquer_layer_1/2), the icons and item models;
//   the soldier's body as the cultists' (blood-cultist-game.cjs): quads by part of the ninja's model, an atlas per outfit
//   and its blink (textures/entity/blood_soldier/), and the armour's quads by piece, on the same armour atlas, drawn over it
//   for each piece it wears (models/entity/blood_soldier.json).
// Usage: node scripts/animation/blood-soldier-game.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const soldier = require('./blood-soldier-3d.cjs');
const { quads } = require('./blood-cultist-game.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ATLAS = soldier.ATLAS, ARMOUR_ATLAS = soldier.ARMOUR_ATLAS;
const TEXTURE = 'murimblock:textures/models/armor/blood_lacquer_3d.png';
const ARMOUR = soldier.ARMOUR;

/** Each piece of the set: its slot's key in blood_soldier.json, the Epic Fight mesh's parts, and the pieces it holds. */
const ITEMS = {
  blood_lacquer_mask: { slot: 'head', parts: ['head'], names: /^(face_guard|guard_[rl])$/ },
  blood_lacquer_cuirass: { slot: 'chest', parts: ['torso', 'rightArm', 'leftArm'], names: /^(front|back|collar|pauldron.*|bracer_.*)$/ },
  blood_lacquer_tassets: { slot: 'legs', parts: ['rightLeg', 'leftLeg'], names: /^(tasset_.*|knee_.*)$/ },
  blood_lacquer_greaves: { slot: 'feet', parts: ['rightLeg', 'leftLeg'], names: /^(greave_.*|toe_cap_.*)$/ },
};
/** A biped joint's Epic Fight mesh part. */
const PART_OF = { Head: 'head', Chest: 'torso', Torso: 'torso', Arm_R: 'rightArm', Hand_R: 'rightArm', Arm_L: 'leftArm', Hand_L: 'leftArm',
  Thigh_R: 'rightLeg', Leg_R: 'rightLeg', Thigh_L: 'leftLeg', Leg_L: 'leftLeg' };

const SET = soldier.APIECES.filter(p => p.design === 'b_lacquered').map(p => {
  const item = Object.keys(ITEMS).find(k => ITEMS[k].names.test(p.name));
  if (!item) throw new Error('no item for ' + p.name);
  return { ...p, item, part: PART_OF[p.bone] };
});

// ---------------------------------------------------------------- the flat armour (Minecraft's own player model)

function box(img, u, v, w, h, d, paint) {
  const faces = { up: [u + d, v, w, d], down: [u + d + w, v, w, d], east: [u, v + d, d, h], north: [u + d, v + d, w, h],
    west: [u + d + w, v + d, d, h], south: [u + d + w + d, v + d, w, h] };
  for (const [f, [x0, y0, fw, fh]] of Object.entries(faces))
    for (let y = 0; y < fh; y++) for (let x = 0; x < fw; x++) {
      const c = kit.colour(paint(f, x, y, fw, fh), ARMOUR, f, x, y, 3);
      if (c) img.set(c, ((y0 + y) * 64 + x0 + x) * 4);
    }
}
/** Lacquer at one texel a pixel: red, a black rim at the top, a laced row every other. */
const flat = (f, x, y) => (f === 'up' ? 'T' : f === 'down' ? null : y === 0 ? 'I' : y % 2 === 0 ? 'u' : 'Q');

function flatLayers() {
  const one = new Uint8Array(64 * 32 * 4), two = new Uint8Array(64 * 32 * 4);
  // The mask: the iron guard over the lower face, the fangs.
  box(one, 0, 0, 8, 8, 8, (f, x, y) => {
    if (f === 'up' || f === 'down' || f === 'south' || y < 4) return null;
    if (f !== 'north') return y < 7 ? 'I' : null;
    if (y === 4) return 'G';
    return y === 5 && (x === 2 || x === 5) ? 'E' : 'I';
  });
  // The cuirass, the drop on the breast; the pauldrons and bracers on the arms.
  box(one, 16, 16, 8, 12, 4, (f, x, y) => (f === 'down' ? null : f === 'north' && x >= 3 && x <= 4 && y >= 3 && y <= 5 ? 'E' : y > 9 ? null : flat(f, x, y)));
  box(one, 40, 16, 4, 12, 4, (f, x, y) => (f === 'down' ? null : y <= 3 ? flat(f, x, y) : y >= 7 && y <= 9 ? flat(f, x, y - 7) : null));
  // The greaves on the shins.
  box(one, 0, 16, 4, 12, 4, (f, x, y) => (f === 'up' || f === 'down' || y < 7 ? null : y === 11 ? 'I' : flat(f, x, y - 7)));
  // The tassets over the thighs, the knee guards.
  box(two, 0, 16, 4, 12, 4, (f, x, y) => (f === 'up' || f === 'down' ? null : y <= 4 ? flat(f, x, y) : f === 'north' && (y === 6 || y === 7) ? 'Q' : null));
  return [one, two];
}

// ---------------------------------------------------------------- the icons

const ICONS = {
  blood_lacquer_mask: [
    '................',
    '................',
    '................',
    '..GGGGGGGGGGGG..',
    '..GIIIIJJIIIIG..',
    '.IGIINIJJINIIGI.',
    '.IGIINIJJINIIGI.',
    '.IGNNNNNNNNNNGI.',
    '.IGIEIEIIEIEIGI.',
    '..GIEIIIIIIEIG..',
    '..GIIIIIIIIIIG..',
    '...GIIIIIIIIG...',
    '....GGGGGGGG....',
    '................',
    '................',
    '................',
  ],
  blood_lacquer_cuirass: [
    '................',
    '.TTTT......TTTT.',
    'TQQQQIIIIIIQQQQT',
    'IuuuuIIIIIIuuuuI',
    'QQQQQITQQTIQQQQQ',
    '.uuuIQQEEQQIuuu.',
    '.QQ.IQEuuEQI.QQ.',
    '.II.IuEuuEuI.II.',
    '.QQ.IQQEEQQI.QQ.',
    '.uu.IuuuuuuI.uu.',
    '.II.IQQQQQQI.II.',
    '....IuuuuuuI....',
    '....IQQQQQQI....',
    '....IIIIIIII....',
    '................',
    '................',
  ],
  blood_lacquer_tassets: [
    '................',
    '................',
    '..IIIII..IIIII..',
    '..TQQQI..IQQQT..',
    '..uuuuI..Iuuuu..',
    '..QQQQI..IQQQQ..',
    '..uuuuI..Iuuuu..',
    '..IIIII..IIIII..',
    '................',
    '...IQQI..IQQI...',
    '...IuuI..IuuI...',
    '...IIII..IIII...',
    '................',
    '................',
    '................',
    '................',
  ],
  blood_lacquer_greaves: [
    '................',
    '................',
    '................',
    '...IIII..IIII...',
    '...TQQI..IQQT...',
    '...uuuI..Iuuu...',
    '...QQQI..IQQQ...',
    '...uuuI..Iuuu...',
    '...QQQI..IQQQ...',
    '...IIII..IIII...',
    '..IIJII..IIJII..',
    '.IIIIII..IIIIII.',
    '.JJJJJJ..JJJJJJ.',
    '................',
    '................',
    '................',
  ],
};

function icon(rows) {
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => {
    const c = kit.colour(l === '.' ? null : l, ARMOUR, y < 6 ? 'up' : 'north', x, y, 5);
    if (c) img.set(c, (y * 16 + x) * 4);
  }));
  // A dark outline round the shape, as Minecraft's items have.
  const out = new Uint8Array(img);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (img[(y * 16 + x) * 4 + 3]) continue;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3]; }))
      out.set([20, 14, 16, 255], (y * 16 + x) * 4);
  }
  return out;
}

if (require.main === module) {
  // ---- the set: one atlas, its meshes, the flat layers, the icons
  const faces = kit.pack(SET, ARMOUR_ATLAS), rows = Math.max(...faces.map(f => f.v + f.h));
  if (rows > ARMOUR_ATLAS) throw new Error('the armour atlas overflows: ' + rows);
  const atlas = kit.png(ARMOUR_ATLAS, ARMOUR_ATLAS, kit.paintAtlas(faces, { ...ARMOUR, key: 'blood_lacquer' }, ARMOUR_ATLAS));
  const armour = path.join(ASSETS, 'textures', 'models', 'armor');
  fs.writeFileSync(path.join(armour, 'blood_lacquer_3d.png'), atlas);
  const [one, two] = flatLayers();
  fs.writeFileSync(path.join(armour, 'blood_lacquer_layer_1.png'), kit.png(64, 32, one));
  fs.writeFileSync(path.join(armour, 'blood_lacquer_layer_2.png'), kit.png(64, 32, two));
  const model = { texture_size: ATLAS, armour_texture_size: ARMOUR_ATLAS, outfits: soldier.OUTFITS.map(o => o.key), parts: null, armour: {} };
  for (const [item, { slot, parts }] of Object.entries(ITEMS)) {
    const pieces = SET.filter(p => p.item === item);
    const m = kit.mesh(pieces, ARMOUR_ATLAS, parts);
    fs.writeFileSync(path.join(ASSETS, 'animmodels', 'armor', item + '.json'), JSON.stringify({ ...m, render_properties: { texture_path: TEXTURE } }));
    fs.writeFileSync(path.join(ASSETS, 'textures', 'item', item + '.png'), kit.png(16, 16, icon(ICONS[item])));
    fs.writeFileSync(path.join(ASSETS, 'models', 'item', item + '.json'),
      JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/' + item } }, null, 2) + '\n');
    model.armour[slot] = quads(pieces, ARMOUR_ATLAS);
    console.log('wrote', item + ':', pieces.length, 'pieces,', m.vertices.positions.count, 'vertices');
  }
  // ---- the soldier: its body's quads, its outfits' atlases (and their blink)
  const body = soldier.PIECES.map(p => ({ ...p }));
  const bodyFaces = kit.pack(body, ATLAS);
  const texDir = path.join(ASSETS, 'textures', 'entity', 'blood_soldier');
  fs.mkdirSync(texDir, { recursive: true });
  for (const o of soldier.OUTFITS) {
    fs.writeFileSync(path.join(texDir, o.key + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(bodyFaces, o, ATLAS)));
    fs.writeFileSync(path.join(texDir, o.key + '_blink.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(bodyFaces, { ...o, face: 'blink' }, ATLAS)));
  }
  model.parts = quads(body, ATLAS);
  fs.writeFileSync(path.join(ASSETS, 'models', 'entity', 'blood_soldier.json'), JSON.stringify(model));
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.writeFileSync(path.join(art, 'blood_lacquer_armor.bbmodel'), JSON.stringify(kit.bbmodel('blood_lacquer_armor', SET, [{ ...ARMOUR, key: 'blood_lacquer' }], [atlas], ARMOUR_ATLAS, 'models/armor'), null, 1));
  console.log('wrote the Blood Soldier:', soldier.OUTFITS.length, 'outfits; the Blood Lacquer set, atlas rows', rows, 'of', ARMOUR_ATLAS);
}

module.exports = { SET, ITEMS };
