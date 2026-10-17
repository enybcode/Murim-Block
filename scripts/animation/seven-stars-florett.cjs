// The Seven Stars Florett (Fleuret des Sept Étoiles), the Seven Stars Sword's own weapon (Enzo, 17/10/2026: "ça ne se joue
// qu'au fleuret, et il a un fleuret à son effigie"): a slender thrusting blade on Dawn Day's florett moveset. Upright,
// its point up, in Blockbench pixels (x across the blade, y along it, z its thickness), the grip's top at the origin:
//   the blade     long and narrow, polished steel, a bright ridge down its middle; above the guard the Big Dipper
//                 inlaid in silver, its seven stars joined by a fine line; the point bright;
//   the guard     a four-pointed star in the blade's plane, silver, its long arms the quillons ending in small stars,
//                 its heart a diamond enamelled night blue with a white star; a small round cup under it; a knuckle
//                 bow sweeping from the guard to the pommel;
//   the grip      wrapped in night-blue cord, a silver wire wound round it;
//   the pommel    a silver star in the blade's plane, a bead of white light at its heart; a short night-blue tassel.
// Written as a Blockbench project (art/blockbench/seven_stars_florett.bbmodel + its texture) and as the game's files:
//   assets/murimblock/models/item/seven_stars_florett_held.json   the 3D model held (a vanilla item model: its pieces as
//                                                                 elements, the diagonals turned 45 degrees about z),
//                                                                 set as the Azure Cloud's (azure_cloud_sword_held.json);
//   assets/murimblock/textures/item/seven_stars_florett_3d.png    its atlas;
//   assets/murimblock/textures/item/seven_stars_florett.png       the icon (16 x 16, drawn here), and the item's model;
//   assets/murimblock/textures/item/seven_stars_manual.png        the art's manual's icon;
//   data/murimblock/capabilities/weapons/seven_stars_florett.json Epic Fight: a florett of Dawn Day (its moveset);
//   assets/murimblock/item_skins/seven_stars_florett.json         its swing trail, pale blue.
// Usage: node scripts/animation/seven-stars-florett.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 256, D = 4;

/** S steel, T its bright edge, K its shadow, A silver, W white, N night blue, B blue enamel, C cord, P the point's light. */
const LOOK = { key: 'seven_stars_florett', c: { S: '#a9b4c4', T: '#e6edf6', K: '#6b7688', A: '#d7dde8', W: '#ffffff', N: '#1b2556',
  B: '#2f4cb0', C: '#22305f', P: '#f4f8ff' } };

const PIECES = [];
/** A piece painted in pixels: (face, x, y, width, height, texel), as the Guardian's (scarlet-guardian-3d.cjs). */
const piece = (name, from, to, paint, extra = {}) => {
  const d = extra.density || D;
  PIECES.push({ bone: 'Root', part: 'body', name, from, to, ...extra, density: d,
    paint: (f, u, v, w, h, o) => paint(f, (u + 0.5) / d, (v + 0.5) / d, w / d, h / d, 1 / d, o) });
};
/** A piece turned about the z axis by `deg` about the point (cx, cy): a star's diagonal arms, in the blade's plane. */
const turnedZ = (deg, cx, cy) => {
  const a = deg * Math.PI / 180;
  return { turnCorner: ([x, y, z]) => { const dx = x - cx, dy = y - cy; return [cx + dx * Math.cos(a) - dy * Math.sin(a), cy + dx * Math.sin(a) + dy * Math.cos(a), z]; },
    rot: [0, 0, deg], pivot: [cx, cy, 0] };
};

// ---- the blade: from the guard to the point; polished steel, the Big Dipper inlaid
const DIPPER = [[0.0, 0.0], [0.3, 1.1], [0.15, 2.2], [-0.1, 3.3], [0.4, 4.7], [-0.35, 5.2], [-0.4, 3.9]];
const DIPPER_LINES = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 3]];
const DIPPER_FROM = 1.4;
piece('blade', [-0.8, 3.6, -0.3], [0.8, 27.0, 0.3], (f, x, y, W, H, t) => {
  if (f === 'up' || f === 'down') return 'S';
  if (f === 'east' || f === 'west') return 'T';
  // y runs down from the blade's top: from the guard is H - y.
  const c = W / 2, fromGuard = H - y;
  for (const [sx, sy] of DIPPER) {
    const d = Math.abs(x - (c + sx)) + Math.abs(fromGuard - (DIPPER_FROM + sy));
    if (d <= t * 1.6) return d <= t * 0.6 ? 'W' : 'A';
  }
  for (const [i, j] of DIPPER_LINES) {
    const p0 = [c + DIPPER[i][0], DIPPER_FROM + DIPPER[i][1]], p1 = [c + DIPPER[j][0], DIPPER_FROM + DIPPER[j][1]];
    const L = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]), k = ((x - p0[0]) * (p1[0] - p0[0]) + (fromGuard - p0[1]) * (p1[1] - p0[1])) / (L * L);
    if (k >= 0 && k <= 1 && Math.hypot(x - (p0[0] + k * (p1[0] - p0[0])), fromGuard - (p0[1] + k * (p1[1] - p0[1]))) < t * 0.55) return 'A';
  }
  if (x < t || x > W - t) return 'T';
  return x < c ? 'S' : 'K';
});
// The point narrowing in two steps.
piece('point', [-0.5, 27.0, -0.22], [0.5, 29.0, 0.22], (f, x, y, W, H, t) => (y < t * 2 ? 'P' : x < t || x > W - t ? 'T' : 'S'));
piece('tip', [-0.22, 29.0, -0.12], [0.22, 30.4, 0.12], () => 'P');

// ---- the guard: a four-pointed star in the blade's plane, long arms out to the sides, short ones up and down, its
// enamelled heart; a small round cup under it, round the grip
const arm = (f, x, y, W, H, t) => (f === 'north' || f === 'south' ? (Math.abs(y - H / 2) < t ? 'T' : y < H / 2 ? 'A' : 'K') : 'A');
const armV = (f, x, y, W, H, t) => (f === 'north' || f === 'south' ? (Math.abs(x - W / 2) < t ? 'T' : x < W / 2 ? 'A' : 'K') : 'A');
const GY = 2.4;
function star(name, cy, across, up, down, thick, z) {
  piece(name + '_across', [-across, cy - thick / 2, -z], [across, cy + thick / 2, z], arm);
  piece(name + '_up', [-thick / 2, cy, -z], [thick / 2, cy + up, z], armV);
  piece(name + '_down', [-thick / 2, cy - down, -z], [thick / 2, cy, z], armV);
  // The diagonals, short, behind the heart.
  for (const d of [45, -45]) piece(name + '_diag' + (d > 0 ? 'a' : 'b'), [-thick * 1.3, cy - thick / 2, -z * 0.8], [thick * 1.3, cy + thick / 2, z * 0.8], arm, turnedZ(d, 0, cy));
}
star('guard', GY, 4.6, 1.4, 1.2, 0.9, 0.5);
// The quillons' tips: a small star at each end.
for (const s of [1, -1]) {
  const cx = s * 4.9, n = s > 0 ? 'r' : 'l';
  piece('tip_star_h' + n, [cx - 0.7, GY - 0.22, -0.4], [cx + 0.7, GY + 0.22, 0.4], () => 'T');
  piece('tip_star_v' + n, [cx - 0.22, GY - 0.7, -0.4], [cx + 0.22, GY + 0.7, 0.4], () => 'T');
}
piece('guard_heart', [-1.2, GY - 1.2, -0.7], [1.2, GY + 1.2, 0.7], (f, x, y, W, H, t) => {
  if (f !== 'north' && f !== 'south') return 'A';
  const dx = Math.abs(x - W / 2), dy = Math.abs(y - H / 2);
  if (dx + dy > W / 2) return null;
  if (dx + dy > W / 2 - t * 1.5) return 'A';
  // A white four-pointed star on the blue.
  return Math.min(dx + dy * 3, dy + dx * 3) < W * 0.3 ? 'W' : 'B';
});
piece('cup', [-1.6, 0.9, -1.6], [1.6, 1.5, 1.6], (f, x, y, W, H, t) => (f === 'up' ? 'A' : f === 'down' ? 'K' : y < t ? 'T' : 'A'));
// The knuckle bow: from the guard's front, down along the grip, to the pommel.
piece('bow_top', [-0.22, 0.4, -3.3], [0.22, 2.0, -2.9], () => 'A');
piece('bow_link1', [-0.22, 1.6, -2.9], [0.22, 2.0, -1.2], () => 'A');
piece('bow_mid', [-0.22, -4.6, -3.1], [0.22, 0.4, -2.7], () => 'A');
piece('bow_low', [-0.22, -6.0, -2.4], [0.22, -4.6, -2.0], () => 'A');
piece('bow_foot', [-0.22, -6.4, -2.0], [0.22, -6.0, -0.6], () => 'A');

// ---- the grip: night-blue cord, a silver wire wound round it at even turns
piece('grip', [-0.65, -5.6, -0.65], [0.65, 0.9, 0.65], (f, x, y, W, H, t) => {
  if (!isSide(f)) return 'C';
  const off = { north: 0, east: W, south: 2 * W, west: 3 * W }[f];
  return ((y + (x + off) * 0.25) % 0.9) < t ? 'A' : 'N';
});
// ---- the pommel: a silver star in the blade's plane, a bead of light at its heart; a short tassel
star('pommel', -6.6, 1.7, 0.9, 1.1, 0.7, 0.45);
piece('pommel_bead', [-0.55, -7.15, -0.55], [0.55, -6.05, 0.55], (f, x, y, W, H, t) => (Math.hypot(x - W / 2, y - H / 2) < W * 0.32 ? 'W' : 'B'));
piece('tassel_cord', [-0.15, -8.6, -0.15], [0.15, -7.7, 0.15], () => 'N');
piece('tassel', [-0.5, -11.6, -0.5], [0.5, -8.6, 0.5], (f, x, y, W, H, t) => (y < t * 2 ? 'A' : isSide(f) && y > H - t && Math.floor(x / t) % 2 ? null : Math.floor(x / t) % 2 ? 'N' : 'B'));

/** The held model's place: the Azure Cloud's (x and z round 8, its grip's middle at y 0.75), three quarters of the size. */
const HELD_SCALE = 0.75, HELD_Y = 2.5;
const held = ([x, y, z]) => [8 + x * HELD_SCALE, HELD_Y + y * HELD_SCALE, 8 + z * HELD_SCALE].map(v => Math.round(v * 1e4) / 1e4);

/** The pieces (packed) as a vanilla item model's elements. */
function heldModel() {
  const elements = PIECES.map(pc => {
    const e = { name: pc.name, from: held(pc.from), to: held(pc.to), faces: {} };
    if (pc.rot && pc.rot[2]) e.rotation = { angle: pc.rot[2], axis: 'z', origin: held(pc.pivot) };
    for (const f of kit.FACES) {
      const [u0, v0, u1, v1] = pc.uv[f];
      e.faces[f] = { uv: [u0, v0, u1, v1].map(v => Math.round(v * 16 / ATLAS * 1e4) / 1e4), texture: '#0' };
    }
    return e;
  });
  const azure = JSON.parse(fs.readFileSync(path.join(ROOT, 'src/main/resources/assets/murimblock/models/item/azure_cloud_sword_held.json'), 'utf8'));
  return { credit: 'Murimblock - scripts/animation/seven-stars-florett.cjs (Blockbench source: art/blockbench/seven_stars_florett.bbmodel)',
    textures: { 0: 'murimblock:item/seven_stars_florett_3d', particle: 'murimblock:item/seven_stars_florett' }, elements, display: azure.display };
}

/** A 16 x 16 icon from rows of letters (LOOK's colours), a dark outline round it. */
function icon(rows, look) {
  const img = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => [...row].forEach((l, x) => { const c = l === '.' ? null : kit.colour(l, look, 'north', x, y, 5); if (c) img.set(c, (y * 16 + x) * 4); }));
  const out = new Uint8Array(img);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (img[(y * 16 + x) * 4 + 3]) continue;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const nx = x + dx, ny = y + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && img[(ny * 16 + nx) * 4 + 3]; }))
      out.set([14, 18, 40, 255], (y * 16 + x) * 4);
  }
  return kit.png(16, 16, out);
}
const FLORETT_ICON = [
  '..............TP',
  '.............TS.',
  '............TS..',
  '...........TS...',
  '..........TS....',
  '.........TS.....',
  '........TS......',
  '.......WS.......',
  '..A...WS........',
  '...AABW.........',
  '....ABA.........',
  '...ANBA.........',
  '..ANNAA.........',
  '..NN..A.........',
  '.AWA............',
  '..A.............',
];
const MANUAL_LOOK = { key: 'manual', c: { N: '#1b2556', B: '#2f4cb0', A: '#d7dde8', W: '#ffffff', K: '#10173a', G: '#9aa6bf' } };
const MANUAL_ICON = [
  '................',
  '..KKKKKKKKKKKK..',
  '..KNNNNNNNNNNAK.',
  '..KNNNNNNNNNNAK.',
  '..KNWNNNNNNNNAK.',
  '..KNNANWNNNNNAK.',
  '..KNNNNNAWNNNAK.',
  '..KNNNNNNNAWNAK.',
  '..KNNNNNNNWNAAK.',
  '..KNNNNNNWANNAK.',
  '..KNNNNNNNNNNAK.',
  '..KBBBBBBBBBBAK.',
  '..KNNNNNNNNNNAK.',
  '..KNNNNNNNNNNAK.',
  '..KKKKKKKKKKKKG.',
  '................',
];

module.exports = { PIECES, LOOK, ATLAS };

if (require.main === module) {
  const faces = kit.pack(PIECES, ATLAS), rows = Math.max(...faces.map(f => f.v + f.h));
  if (rows > ATLAS) throw new Error('the atlas overflows: ' + rows);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, LOOK, ATLAS));
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'seven_stars_florett'), { recursive: true });
  fs.writeFileSync(path.join(art, 'seven_stars_florett', 'seven_stars_florett.png'), atlas);
  const project = kit.bbmodel('seven_stars_florett', PIECES, [LOOK], [atlas], ATLAS, 'item');
  project.animations = [{ uuid: kit.stableId('seven_stars_florett/pose'), name: 'pose', loop: 'once', override: false, length: 0, snapping: 20,
    selected: false, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators: {} }];
  fs.writeFileSync(path.join(art, 'seven_stars_florett.bbmodel'), JSON.stringify(project, null, 1));
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'seven_stars_florett_3d.png'), atlas);
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'seven_stars_florett.png'), icon(FLORETT_ICON, LOOK));
  fs.writeFileSync(path.join(assets, 'textures', 'item', 'seven_stars_manual.png'), icon(MANUAL_ICON, MANUAL_LOOK));
  fs.writeFileSync(path.join(assets, 'models', 'item', 'seven_stars_florett_held.json'), JSON.stringify(heldModel(), null, 1) + '\n');
  const flat = { parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/seven_stars_florett' } };
  fs.writeFileSync(path.join(assets, 'models', 'item', 'seven_stars_florett.json'), JSON.stringify({ loader: 'neoforge:separate_transforms',
    base: { parent: 'murimblock:item/seven_stars_florett_held' }, perspectives: { gui: flat, ground: flat, fixed: flat } }, null, 2) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', 'seven_stars_manual.json'), JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: 'murimblock:item/seven_stars_manual' } }) + '\n');
  fs.mkdirSync(path.join(assets, 'item_skins'), { recursive: true });
  fs.writeFileSync(path.join(assets, 'item_skins', 'seven_stars_florett.json'), JSON.stringify({ trail: { color: [170, 205, 255], begin_pos: [0.0, 0.0, -0.8],
    end_pos: [0.0, 0.0, -2.5], lifetime: 4, interpolations: 4, texture_path: 'epicfight_dd:textures/particle/pixelated_katana_trail.png', particle_type: 'epicfight:swing_trail' } }, null, 2) + '\n');
  const caps = path.join(ROOT, 'src', 'main', 'resources', 'data', 'murimblock', 'capabilities', 'weapons');
  fs.mkdirSync(caps, { recursive: true });
  fs.writeFileSync(path.join(caps, 'seven_stars_florett.json'), JSON.stringify({ attributes: { common: { impact: 1.0, armor_negation: 8.0, max_strikes: 3 } },
    type: 'epicfight_dd:florett' }, null, 2) + '\n');
  console.log('wrote the Seven Stars Florett:', PIECES.length, 'pieces, atlas rows', rows, 'of', ATLAS, '; its game files and the manual\'s icon');
}
