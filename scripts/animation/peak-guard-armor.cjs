// The Peak Guard's armour, worn by the Captain of the Peak Guard and dropped by him (docs/mobs/peak-captain.md), for a
// player to wear: four pieces in the Alliance's iron and gold.
//   peak_guard_helm       an iron dome with a gold brim and bands, cheek guards, a lamellar neck curtain, a gold crest,
//                         a tall red plume sweeping back and two pheasant feathers;
//   peak_guard_lamellar   a cuirass of iron lamellae laced with gold, a gold mirror plate with a cyan gem on the chest,
//                         pauldrons in three tiers (the top one a gold lion's face), banded gauntlets;
//   peak_guard_tassets    lamellar tassets in two tiers over each thigh, an apron of lamellae between the legs, knee
//                         guards;
//   peak_guard_greaves    iron greaves rimmed in gold, over the boots.
// Written here (as the Bandit Chief's, chief-armor.cjs):
//   assets/murimblock/animmodels/armor/<piece>.json          each piece's Epic Fight mesh;
//   assets/murimblock/textures/models/armor/peak_guard_3d.png their atlas;
//   assets/murimblock/textures/models/armor/peak_guard_layer_1/2.png   the flat armour of Minecraft's own model;
//   assets/murimblock/textures/item/<piece>.png, models/item/<piece>.json   the icons;
//   art/blockbench/peak_guard_armor.bbmodel                   the pieces on the biped's joints.
// Usage: node scripts/animation/peak-guard-armor.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, fringe, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ATLAS = 128;
const TEXTURE = 'murimblock:textures/models/armor/peak_guard_3d.png';

/** I iron, J bright iron, G gold, D dark red (eyes, tongue), E cyan gem, T red plume, K feather brown, W white, L leather. */
const ARMOUR = { key: 'peak_guard', c: {
  I: '#8b9198', J: '#d2d8de', G: '#E9C46A', D: '#7a1418', E: '#3fb8c8', T: '#c0202c', K: '#6b4a2e', W: '#efeae0', L: '#5a3a24' } };

const ITEMS = {
  peak_guard_helm: ['head'],
  peak_guard_lamellar: ['torso', 'rightArm', 'leftArm'],
  peak_guard_tassets: ['torso', 'rightLeg', 'leftLeg'],
  peak_guard_greaves: ['rightLeg', 'leftLeg'],
};
const PIECES = [];
const piece = (item, bone, part, name, from, to, paint, extra = {}) => PIECES.push({ item, bone, part, name, from, to, paint, ...extra });
const mirror = ([x0, y0, z0], [x1, y1, z1]) => [[-x1, y0, z0], [-x0, y1, z1]];

/** Lamellar: small iron plates in staggered rows, three texels high, laced with gold, a rivet in each. */
const lamellar = (f, u, v) => {
  if (!isSide(f)) return 'i';
  if (v % 3 === 2) return u % 2 === 0 ? 'G' : 'i';
  const plate = (u + Math.floor(v / 3)) % 2;
  if (v % 3 === 0 && u % 2 === plate) return 'g';
  return plate ? 'I' : 'J';
};

// ---- the helm
const HELM = 'peak_guard_helm';
piece(HELM, 'Head', 'head', 'dome', [-4.5, 28.4, -4.5], [4.5, 33, 4.5], (f, u, v, w, h) => (f === 'down' ? null : isSide(f) && (v === h - 1 || v === h - 3) ? 'G' : (u + v) % 4 === 0 ? 'J' : 'I'));
piece(HELM, 'Head', 'head', 'top', [-3, 33, -3], [3, 34.2, 3], (f, u, v) => ((u + v) % 3 === 0 ? 'J' : 'I'));
piece(HELM, 'Head', 'head', 'brim', [-4.9, 28.2, -5.1], [4.9, 28.9, -4.4], () => 'G');
piece(HELM, 'Head', 'head', 'curtain', [-4.6, 24.6, 3.6], [4.6, 28.6, 4.9], (f, u, v) => lamellar('south', u, v));
for (const [name, x0, x1] of [['cheek_r', 4.4, 5.0], ['cheek_l', -5.0, -4.4]])
  piece(HELM, 'Head', 'head', name, [x0, 24.8, -2.2], [x1, 28.6, 2.6], (f, u, v, w, h) => (v === h - 1 || u === 0 ? 'G' : 'I'));
piece(HELM, 'Head', 'head', 'crest', [-0.5, 34.2, -3.8], [0.5, 36.2, 3.0], () => 'G');
piece(HELM, 'Head', 'head', 'plume_base', [-0.8, 34.6, 2.2], [0.8, 36.4, 3.8], () => 'G');
// The plume rises from its gold base and sweeps back; its tail hangs from its tip behind the neck curtain.
piece(HELM, 'Head', 'head', 'plume', [-1.1, 35.8, 2.6], [1.1, 42, 4.6], fringe('T'), { tilt: 30, pivot: [0, 35.8, 3.6] });
piece(HELM, 'Head', 'head', 'plume_tail', [-0.9, 34.4, 5.8], [0.9, 41.4, 7.6], fringe('T'), { tilt: 10, pivot: [0, 41.4, 6.7] });
for (const [name, sx] of [['feather_r', 1], ['feather_l', -1]]) {
  // Rooted in the crown beside the crest, swept back over the plume.
  const [x0, x1] = sx > 0 ? [1.3, 1.9] : [-1.9, -1.3];
  piece(HELM, 'Head', 'head', name, [x0, 33.6, 0], [x1, 45, 0.6], (f, u, v) => (v % 3 === 0 ? 'k' : v < 3 ? 'W' : 'K'), { tilt: 22 + sx * 4, pivot: [sx * 1.6, 33.6, 0.3] });
}

// ---- the lamellar: cuirass, mirror plate, pauldrons, gauntlets
const VEST = 'peak_guard_lamellar';
piece(VEST, 'Chest', 'torso', 'cuirass', [-4.7, 17.8, -2.8], [4.7, 23.6, 2.8], rim(lamellar), { hinge: { y: 18, bone: 'Torso' } });
piece(VEST, 'Chest', 'torso', 'cuirass_rim', [-4.75, 23.2, -2.85], [4.75, 23.9, 2.85], rim(() => 'G'));
piece(VEST, 'Torso', 'torso', 'cuirass_low', [-4.7, 14.6, -2.8], [4.7, 18, 2.8], rim(lamellar), { hinge: { y: 18, bone: 'Chest' } });
piece(VEST, 'Chest', 'torso', 'mirror', [-1.8, 18.8, -3.3], [1.8, 22.6, -2.8], (f, u, v, w, h) => {
  if (f !== 'north') return 'g';
  const cx = (w - 1) / 2, cy = (h - 1) / 2, d = Math.hypot(u - cx, v - cy);
  return d > 1.7 ? 'G' : d > 0.9 ? 'J' : 'E';
});
for (const [side, sx, part] of [['R', 1, 'rightArm'], ['L', -1, 'leftArm']]) {
  const at = (from, to) => (sx > 0 ? [from, to] : mirror(from, to));
  const outer = sx > 0 ? 'east' : 'west';
  const s = side.toLowerCase();
  [[3.0, 9.6, 21.6, 25.2], [3.4, 9.9, 19.8, 23.0], [3.8, 10.2, 18.0, 21.0]].forEach(([i0, i1, y0, y1], tier) => {
    piece(VEST, 'Arm_' + side, part, 'pauldron_' + tier + '_' + s, ...at([i0, y0, -3.1 - tier * 0.2], [i1, y1, 3.1 + tier * 0.2]), rim((f, u, v, w, h) => {
      if (tier === 0 && f === outer) {
        // The lion: gold face, dark eyes, a red tongue.
        if (v === 1 && (u === 1 || u === w - 2)) return 'D';
        if (v === h - 1 && u === Math.floor(w / 2)) return 'D';
        return (u + v) % 4 === 0 ? 'g' : 'G';
      }
      if (v === h - 1) return 'G';
      return (u + v) % 2 ? 'I' : 'J';
    }));
  });
  piece(VEST, 'Hand_' + side, part, 'bracer_' + s, ...at([3.6, 11.6, -2.45], [8.4, 16.6, 2.45]), rim((f, u, v, w, h) => (v === 0 || v === h - 1 || v === 2 ? 'G' : (u + v) % 3 ? 'J' : 'I')));
  piece(VEST, 'Hand_' + side, part, 'hand_plate_' + s, ...at([3.9, 10.2, -2.3], [8.1, 11.6, 2.3]), rim((f, u, v) => (v === 0 ? 'G' : 'I')));
}

// ---- the tassets
const LEGS = 'peak_guard_tassets';
piece(LEGS, 'Torso', 'torso', 'apron', [-1.8, 7.4, -3.1], [1.8, 13.4, -2.8], (f, u, v, w, h) => (v === h - 1 || u === 0 || u === w - 1 ? 'G' : lamellar(f, u, v)));
piece(LEGS, 'Torso', 'torso', 'waist', [-4.65, 12.6, -2.75], [4.65, 14.2, 2.75], rim((f, u, v) => (v === 0 ? 'G' : lamellar(f, u, v))));
for (const [side, sx, part] of [['R', 1, 'rightLeg'], ['L', -1, 'leftLeg']]) {
  const at = (from, to) => (sx > 0 ? [from, to] : mirror(from, to));
  const s = side.toLowerCase();
  const tasset = (f, u, v, w, h) => (f === 'up' || f === 'down' ? null : v === h - 1 ? 'G' : lamellar(f, u, v));
  piece(LEGS, 'Thigh_' + side, part, 'tasset_' + s, ...at([-0.6, 8.6, -2.9], [4.6, 13.3, 2.9]), tasset);
  piece(LEGS, 'Thigh_' + side, part, 'tasset_low_' + s, ...at([-0.6, 5.6, -3.05], [4.6, 9.0, 1.0]), tasset);
  piece(LEGS, 'Leg_' + side, part, 'knee_' + s, ...at([-0.4, 5.2, -3.1], [4.4, 7.4, -1.8]), (f, u, v, w, h) => (v === 0 || v === h - 1 ? 'G' : u === Math.floor(w / 2) ? 'J' : 'I'));
}

// ---- the greaves
const FEET = 'peak_guard_greaves';
for (const [side, sx, part] of [['R', 1, 'rightLeg'], ['L', -1, 'leftLeg']]) {
  const at = (from, to) => (sx > 0 ? [from, to] : mirror(from, to));
  const s = side.toLowerCase();
  piece(FEET, 'Leg_' + side, part, 'greave_' + s, ...at([-0.55, 1.2, -2.95], [4.55, 5.4, 1.4]), rim((f, u, v, w, h) => (v === 0 || v === h - 1 ? 'G' : u % 2 ? 'J' : 'I')));
  piece(FEET, 'Leg_' + side, part, 'sabaton_' + s, ...at([-0.5, 0, -3.3], [4.5, 1.2, 2.5]), (f, u, v) => (f === 'down' ? 'i' : v === 0 ? 'G' : 'I'));
}

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

function flatLayers() {
  const one = new Uint8Array(64 * 32 * 4), two = new Uint8Array(64 * 32 * 4);
  // Helm: the dome with its gold bands, the brim, the crest on top.
  box(one, 0, 0, 8, 8, 8, (f, x, y) => {
    if (f === 'up') return x === 3 || x === 4 ? 'G' : (x + y) % 4 === 0 ? 'J' : 'I';
    if (f === 'down') return null;
    if (y === 3 || y === 1) return 'G';
    if (y < 4) return (x + y) % 4 === 0 ? 'J' : 'I';
    if ((f === 'east' || f === 'west') && y < 7) return 'I';
    if (f === 'south') return lamellar('south', x, y);
    return null;
  });
  // Body: lamellar, the mirror plate in front.
  box(one, 16, 16, 8, 12, 4, (f, x, y) => {
    if (f === 'up') return 'G';
    if (f === 'down') return null;
    if (f === 'north' && x >= 2 && x <= 5 && y >= 2 && y <= 5) return x >= 3 && x <= 4 && y >= 3 && y <= 4 ? 'E' : 'G';
    if (y === 0) return 'G';
    return lamellar(f, x, y);
  });
  // Arms: the pauldron on top (the lion's eyes), the gauntlet on the forearm.
  box(one, 40, 16, 4, 12, 4, (f, x, y) => {
    if (f === 'up') return 'G';
    if (f === 'down') return null;
    if (y <= 4) return y === 4 ? 'G' : f === 'east' && y === 1 && (x === 1 || x === 2) ? 'D' : (x + y) % 2 ? 'I' : 'J';
    if (y >= 7 && y <= 11) return y === 7 || y === 9 ? 'G' : 'J';
    return null;
  });
  // Feet (on the legs' box): greaves over the shins.
  box(one, 0, 16, 4, 12, 4, (f, x, y) => {
    if (f === 'up') return null;
    if (f === 'down') return 'i';
    if (y === 7 || y === 11) return 'G';
    return y > 7 ? (x % 2 ? 'J' : 'I') : null;
  });
  // Leggings: the waist band, the tassets over the thighs, the knee guards.
  box(two, 16, 16, 8, 12, 4, (f, x, y) => (f !== 'up' && f !== 'down' && y >= 9 ? (y === 9 ? 'G' : lamellar(f, x, y)) : null));
  box(two, 0, 16, 4, 12, 4, (f, x, y) => {
    if (f === 'up' || f === 'down') return null;
    if (y <= 5) return y === 5 ? 'G' : lamellar(f, x, y);
    if (f === 'north' && (y === 6 || y === 7)) return y === 6 ? 'G' : 'I';
    return null;
  });
  return [one, two];
}

// ---------------------------------------------------------------- the icons

const ICONS = {
  peak_guard_helm: [
    '.......T........',
    '......TTT.......',
    '.....TTGTT......',
    '......GGG.......',
    '.....IIGJI......',
    '....IJIGIJI.....',
    '...IIIIGIIII....',
    '...GGGGGGGGG....',
    '...JI.....IJ....',
    '..GGGGGGGGGGG...',
    '...I.......I....',
    '...IG.....GI....',
    '...IJ.....JI....',
    '...GI.....IG....',
    '................',
    '................',
  ],
  peak_guard_lamellar: [
    '................',
    '.GGG........GGG.',
    'GDGDG.GGGG.GDGDG',
    'GGDGGIGWWGIGGDGG',
    'IJIJJIJGGJIJJIJI',
    '.IJIJGJIIJGJIJI.',
    '.JI.IJGGGGJI.IJ.',
    '.IJ.JIGEEGIJ.JI.',
    '.GG.IJGEEGJI.GG.',
    '.JI.JIGGGGIJ.IJ.',
    '.IJ.GiGiGiGi.JI.',
    '.GG.IJIJIJIJ.GG.',
    '....JIJIJIJI....',
    '....GGGGGGGG....',
    '................',
    '................',
  ],
  peak_guard_tassets: [
    '................',
    '..GGGGGGGGGGGG..',
    '..IJIJIJIJIJIJ..',
    '..GiGiGIIGiGiG..',
    '..JIJIGJJGJIJI..',
    '..IJIJGIIGIJIJ..',
    '..GGGGGIIGGGGG..',
    '..IJIJ.GG.JIJI..',
    '..JIJI....IJIJ..',
    '..GGGG....GGGG..',
    '..GJIG....GIJG..',
    '..GGGG....GGGG..',
    '................',
    '................',
    '................',
    '................',
  ],
  peak_guard_greaves: [
    '................',
    '................',
    '................',
    '...GGGG..GGGG...',
    '...IJIJ..JIJI...',
    '...JIJI..IJIJ...',
    '...IJIJ..JIJI...',
    '...JIJI..IJIJ...',
    '...GGGG..GGGG...',
    '...IJIJ..JIJI...',
    '..IIJII..IIJII..',
    '.IIIIII..IIIIII.',
    '.GGGGGG..GGGGGG.',
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
  // A dark outline round the shape, as Minecraft's items have.
  const out = new Uint8Array(img);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (img[(y * 16 + x) * 4 + 3]) continue;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3]; }))
      out.set([24, 22, 28, 255], (y * 16 + x) * 4);
  }
  return out;
}

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, ARMOUR, ATLAS));
  const armour = path.join(ASSETS, 'textures', 'models', 'armor');
  fs.mkdirSync(armour, { recursive: true });
  fs.writeFileSync(path.join(armour, 'peak_guard_3d.png'), atlas);
  const [one, two] = flatLayers();
  fs.writeFileSync(path.join(armour, 'peak_guard_layer_1.png'), kit.png(64, 32, one));
  fs.writeFileSync(path.join(armour, 'peak_guard_layer_2.png'), kit.png(64, 32, two));
  const meshes = path.join(ASSETS, 'animmodels', 'armor');
  for (const [item, parts] of Object.entries(ITEMS)) {
    const m = kit.mesh(PIECES.filter(p => p.item === item), ATLAS, parts);
    fs.writeFileSync(path.join(meshes, item + '.json'), JSON.stringify({ ...m, render_properties: { texture_path: TEXTURE } }));
    fs.writeFileSync(path.join(ASSETS, 'textures', 'item', item + '.png'), kit.png(16, 16, icon(ICONS[item])));
    fs.writeFileSync(path.join(ASSETS, 'models', 'item', item + '.json'),
      JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/' + item } }, null, 2) + '\n');
    console.log('wrote', item + ':', m.vertices.positions.count, 'vertices');
  }
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'peak_guard_armor'), { recursive: true });
  fs.writeFileSync(path.join(art, 'peak_guard_armor', 'peak_guard_3d.png'), atlas);
  fs.writeFileSync(path.join(art, 'peak_guard_armor.bbmodel'), JSON.stringify(kit.bbmodel('peak_guard_armor', PIECES, [ARMOUR], [atlas], ATLAS, 'models/armor'), null, 1));
  console.log('wrote the Peak Guard\'s armour:', PIECES.length, 'pieces, atlas rows used', Math.max(...faces.map(f => f.v + f.h)), 'of', ATLAS);
}

module.exports = { PIECES, ITEMS, ARMOUR };
