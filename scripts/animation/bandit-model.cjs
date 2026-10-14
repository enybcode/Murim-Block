// The Mountain Bandit (山賊) as a Blockbench project on Epic Fight's biped skeleton (art/blockbench/mountain_bandit.bbmodel),
// and its five outfits as textures (art/blockbench/mountain_bandit/<outfit>.png). One model for every outfit, like the
// axolotl's colours: the pieces only some outfits wear (the wolf hood and pelt, the dune turban, the headband's tails)
// are in the model, and an outfit that does not wear one leaves its texels clear.
//   bones: the biped's joints (Root, Torso, Chest, Head, Shoulder, Arm, Hand (the forearm), Tool (the hand), Thigh, Leg),
//          as blockbench-rig.cjs places them, so the biped's animations play on it;
//   texture: one 128x128 atlas per outfit, one texel per pixel of the model, painted face by face below;
//   dao: a broad curved sabre in the right hand, for the preview (in game it is the item it holds).
// Usage: node scripts/animation/bandit-model.cjs
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { animation, groupsByName, at, uuid } = require('./blockbench-rig.cjs');

const ATLAS = 128;

// ---------------------------------------------------------------- the outfits (as on the sheet)

const OUTFITS = [
  { key: 'ash', name: 'Ash Wanderer', band: true,
    c: { H: '#3b3a3f', A: '#2a2220', S: '#c69c6d', E: '#1b1b22', M: '#9c7a55', R: '#77716a', D: '#55504a', U: '#77716a', B: '#a08455', W: '#d3c7ac', P: '#4a4640', F: '#5b4430' } },
  { key: 'crimson', name: 'Crimson Scarf', band: true,
    c: { H: '#a7302a', A: '#2a2220', S: '#c99e70', E: '#1b1b22', M: '#8f6c4c', R: '#5e4c42', D: '#3e332e', U: '#5e4c42', B: '#a7302a', W: '#cbbf9f', P: '#3b3632', F: '#4a3626' } },
  { key: 'wolf', name: 'Wolf Pelt', hood: true,
    c: { H: '#8a7a66', A: '#4a3524', S: '#bf9265', E: '#1b1b22', M: '#5a4030', R: '#6b4f36', D: '#4a3524', U: '#9a8a74', B: '#2f2620', W: '#8a7a66', P: '#4d4136', F: '#3a2a1c' } },
  { key: 'dune', name: 'Dune Raider', turban: true,
    c: { H: '#d9c08f', A: '#d9c08f', S: '#b0804f', E: '#1b1b22', M: '#c9ad78', R: '#b88a4e', D: '#8e6633', U: '#b88a4e', B: '#6b3d2a', W: '#e3d3ae', P: '#9c7a50', F: '#5e4129' } },
  { key: 'gilded', name: 'Gilded Mask', band: true, mask: true,
    c: { H: '#1e1b22', A: '#1e1b22', S: '#c69c6d', E: '#120f14', M: '#d4a637', R: '#2b2a33', D: '#17161c', U: '#2b2a33', B: '#d4a637', W: '#4a4652', P: '#23222a', F: '#18161a' } },
];
const STEEL = { spine: '#8d949c', flat: '#c9d0d6', edge: '#f2f5f8', grip: '#3a2a1a', wrap: '#7a2a22', guard: '#a8843e' };

// The front of the body, as on the sheet (16 x 32, the viewer's view: the bandit's right on the left).
const GRID = [
  '....HHHHHHHH....', '....HHHHHHHH....', '....ASSSSSSA....', '....SEESSEES....',
  '....SSSSSSSS....', '....SSMMMMSS....', '....SMMMMMMS....', '.....SSSSSS.....',
  'UUUUDRRRRRRDUUUU', 'UUUUDRRDDRRDUUUU', 'UUUURRDRRDRRUUUU', 'RRRRRRRDDRRRRRRR',
  'RRRRRRRRRRRRRRRR', 'RRRRBBBBBBBBRRRR', 'WWWWRRRBBRRRWWWW', 'WWWWRRRRRRRRWWWW',
  'WWWWDRRRRRRDWWWW', 'WWWWDRRRRRRDWWWW', 'SSSSDRRRRRRDSSSS', 'SSSSDRRRRRRDSSSS',
  '....PPPPPPPP....', '....PPPPPPPP....', '....PPPPPPPP....', '....PPPPPPPP....',
  '....PPPPPPPP....', '....PPPPPPPP....', '....WWWWWWWW....', '....WWWWWWWW....',
  '....WWWWWWWW....', '....WWWWWWWW....', '....FFFFFFFF....', '....FFFFFFFF....',
];

// ---------------------------------------------------------------- colours

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const scale = ([r, g, b], f) => [r, g, b].map(v => Math.max(0, Math.min(255, Math.round(v * f))));
/** A little grain so flat colours read as cloth and leather (the same for a given texel). */
const grain = (u, v, s) => 0.94 + (((u * 73 + v * 151 + s * 37) % 7) / 6) * 0.1;
/** Light from above: tops a touch brighter, undersides darker. */
const LIGHT = { up: 1.06, down: 0.78, north: 1, south: 0.9, east: 0.95, west: 0.95 };

// ---------------------------------------------------------------- the pieces

const [, HY] = at('Head'), [, CY] = at('Chest'), [, TY] = at('Torso');
const [AX] = at('Arm_R'), [, EY] = at('Hand_R'), [TX, TYY, TZ] = at('Tool_R');
const [LX] = at('Thigh_R'), [, KY] = at('Leg_R');
const head0 = Math.round(HY), chest0 = Math.round(CY), belly0 = Math.round(TY), elbow = Math.round(EY), knee = Math.round(KY);

/**
 * Each piece: its bone, its box (Blockbench pixels: x the bandit's right, y up, z its back; it faces north, -z) and what
 * it shows on each face: a function (face, u, v, w, h, outfit) -> a colour letter, a hex colour, or null for clear.
 */
const PIECES = [];
const piece = (bone, name, from, to, paint, light = true) => PIECES.push({ bone, name, from, to, paint, light });
const grid = (row, col) => GRID[row][col];

// Head: the face from the sheet; hair at the sides and back under the headwear rows; an ear on each side.
piece('Head', 'head', [-4, head0, -4], [4, head0 + 8, 4], (f, u, v, w, h, o) => {
  if (f === 'north') {
    const l = grid(v, 4 + u);
    if (l === '.') return 'S';
    // The gilded mask: gold over the eyes (they show through), black cloth over the mouth.
    if (o.mask && v >= 2 && v <= 4) return l === 'E' ? 'E' : 'M';
    if (o.mask && (v === 5 || v === 6) && l !== '.') return 'H';
    return l;
  }
  if (f === 'up') return o.band ? (u === 0 || v === 0 || u === w - 1 || v === h - 1 ? 'H' : 'A') : 'H';
  if (f === 'down') return 'S';
  if (v < 2) return 'H';
  if (f === 'east' || f === 'west') {
    const front = f === 'east' ? w - 1 - u : u;
    if (front === 0 && v >= 2 && v <= 6) return v >= 5 ? 'M' : 'S';
    if (front === 2 && (v === 3 || v === 4)) return 'S';
    return v === 7 ? 'S' : 'A';
  }
  // The back: hair, the headband's knot.
  return o.band && (u === 3 || u === 4) && v < 4 ? 'H' : 'A';
});
// The headband's tails behind the head (headband outfits only).
piece('Head', 'band_tail_l', [-1.7, head0 + 3.2, 4], [-0.5, head0 + 6.4, 4.6], (f, u, v, w, h, o) => (o.band ? 'H' : null));
piece('Head', 'band_tail_r', [0.5, head0 + 2.6, 4], [1.7, head0 + 6.4, 4.6], (f, u, v, w, h, o) => (o.band ? 'H' : null));
// The wolf hood: fur all round, open on the face.
piece('Head', 'hood', [-4.6, head0 - 0.4, -4.6], [4.6, head0 + 8.8, 4.6], (f, u, v, w, h, o) => {
  if (!o.hood || f === 'down') return null;
  if (f === 'north' && v >= 2 && u >= 1 && u <= w - 2) return null;
  // Fur: a few darker hairs; the rim round the face darker.
  if (f === 'north') return 'h';
  return (u * 7 + v * 13) % 9 === 0 ? 'h' : 'H';
});
// The dune turban: wraps around the top of the head, a knot at the front.
piece('Head', 'turban', [-4.6, head0 + 4.6, -4.6], [4.6, head0 + 8.9, 4.6], (f, u, v, w, h, o) => {
  if (!o.turban || f === 'down') return null;
  if (f === 'north' && u >= 3 && u <= 5 && v >= 1) return 'D';
  // Cloth wound round in bands, each a little darker at its lower edge.
  return (v + Math.floor((u + (f === 'up' ? v : 0)) / 3)) % 3 === 2 ? 'h' : 'H';
});

// Chest and belly: the robe from the sheet, the belt across them.
piece('Chest', 'chest', [-4, chest0, -2], [4, head0, 2], (f, u, v, w, h) => {
  if (f === 'north') return grid(8 + v, 4 + u);
  if (f === 'up') return 'U';
  if (f === 'down') return 'R';
  if (v === h - 1) return 'B';
  if (f === 'south') return u === 0 || u === w - 1 ? 'D' : 'R';
  return 'R';
});
piece('Torso', 'belly', [-4, belly0, -2], [4, chest0, 2], (f, u, v, w, h) => {
  if (f === 'north') return grid(14 + v, 4 + u);
  if (f === 'down') return 'P';
  if (f === 'up') return 'R';
  if (f === 'south') return u === 0 || u === w - 1 ? 'D' : 'R';
  return v >= h - 2 ? 'D' : 'R';
});
// The sash, a little proud of the robe, knotted in front.
piece('Torso', 'sash', [-4.4, chest0 - 0.6, -2.4], [4.4, chest0 + 1.2, 2.4], (f, u, v, w, h) => {
  if (f === 'up' || f === 'down') return null;
  if (f === 'north' && (u === 3 || u === 4)) return 'D';
  return 'B';
});
// The wolf pelt hanging down the back.
piece('Chest', 'pelt', [-4.4, belly0 + 0.5, 2.05], [4.4, head0 + 0.4, 2.9], (f, u, v, w, h, o) => {
  if (!o.hood) return null;
  if (v === h - 1) return 'h';
  return (u * 7 + v * 13) % 9 === 0 ? 'h' : 'H';
});

// Arms: the sleeve, then the wrapped forearm and the hand.
for (const [side, x] of [['R', AX], ['L', -AX]]) {
  piece('Arm_' + side, 'upper_arm_' + side.toLowerCase(), [x - 2, elbow, -2], [x + 2, head0, 2], (f, u, v) => {
    if (f === 'up') return 'U';
    if (f === 'down') return 'R';
    return v < 3 ? 'U' : 'R';
  });
  piece('Hand_' + side, 'forearm_' + side.toLowerCase(), [x - 2, elbow - 6, -2], [x + 2, elbow, 2], (f, u, v, w, h) => {
    if (f === 'up') return 'W';
    if (f === 'down') return 'S';
    if (v >= h - 2) return 'S';
    // Wrapped in strips, a darker line between them.
    return v % 2 === 0 ? 'W' : 'w';
  });
  // The wolf's fur shoulders.
  piece('Arm_' + side, 'shoulder_fur_' + side.toLowerCase(), [x - 2.4, head0 - 1.6, -2.4], [x + 2.4, head0 + 0.4, 2.4], (f, u, v, w, h, o) => {
    if (!o.hood || f === 'down') return null;
    return (u * 7 + v * 13) % 9 === 0 ? 'h' : 'H';
  });
}

// Legs: trousers, the robe's skirt over the thigh, wrapped shins, sandals.
for (const [side, x] of [['R', LX], ['L', -LX]]) {
  piece('Thigh_' + side, 'thigh_' + side.toLowerCase(), [x - 2, knee, -2], [x + 2, belly0, 2], (f) => (f === 'down' ? 'P' : 'P'));
  piece('Thigh_' + side, 'skirt_' + side.toLowerCase(), [x - 2.25, knee + 1.4, -2.25], [x + 2.25, belly0 + 0.4, 2.25], (f, u, v, w, h) => {
    if (f === 'up' || f === 'down') return null;
    if (v === h - 1) return 'D';
    // The robe opens at the side: a darker fold where its two panels meet.
    const inner = side === 'R' ? (f === 'west') : (f === 'east');
    if (inner) return null;
    return u === 0 || u === w - 1 ? 'D' : 'R';
  });
  piece('Leg_' + side, 'shin_' + side.toLowerCase(), [x - 2, 0, -2], [x + 2, knee, 2], (f, u, v, w, h) => {
    if (f === 'up') return 'W';
    if (f === 'down') return 'F';
    if (v >= h - 2) return 'F';
    // Wrapped in strips, a darker line between them.
    return v % 2 === 1 ? 'W' : 'w';
  });
}

// The dao in the right hand: grip behind the hand, a round guard, a broad blade widening toward a clipped point.
// Its line is the sword's line in blockbench-rig.cjs (forward, -z), its edge down (-y), so the art's poses fit.
const dao = [];
const daoPiece = (name, from, to, color) => dao.push({ name, from, to, color });
daoPiece('dao_grip', [TX - 0.5, TYY - 0.5, TZ - 1], [TX + 0.5, TYY + 0.5, TZ + 3.2], 'grip');
daoPiece('dao_pommel', [TX - 0.7, TYY - 0.7, TZ + 3.2], [TX + 0.7, TYY + 0.7, TZ + 3.8], 'guard');
daoPiece('dao_guard', [TX - 1.3, TYY - 1.5, TZ - 1.7], [TX + 1.3, TYY + 1.1, TZ - 1], 'guard');
const segments = [[1.0, 0.6], [1.15, 0.6], [1.35, 0.55], [1.6, 0.5], [1.85, 0.4], [2.0, 0.15]];
segments.forEach(([below, above], i) => {
  const z0 = TZ - 1.7 - i * 2.1, z1 = z0 - 2.1;
  daoPiece('dao_blade_' + i, [TX - 0.25, TYY - below, z1], [TX + 0.25, TYY + above, z0], 'blade');
});
daoPiece('dao_tip', [TX - 0.2, TYY - 1.7, TZ - 1.7 - 6 * 2.1 - 1.0], [TX + 0.2, TYY - 0.2, TZ - 1.7 - 6 * 2.1], 'blade');

// ---------------------------------------------------------------- the atlas: one rectangle per face, packed in rows

const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
function faceSize(from, to, f) {
  const s = from.map((v, i) => Math.max(1, Math.round(Math.abs(to[i] - v))));
  if (f === 'north' || f === 'south') return [s[0], s[1]];
  if (f === 'east' || f === 'west') return [s[2], s[1]];
  return [s[0], s[2]];
}
function pack(items) {
  let x = 0, y = 0, rowH = 0;
  for (const it of items) {
    if (x + it.w > ATLAS) { x = 0; y += rowH; rowH = 0; }
    it.x = x; it.y = y;
    x += it.w; rowH = Math.max(rowH, it.h);
  }
  if (y + rowH > ATLAS) throw new Error('The atlas is too small: ' + (y + rowH));
}
const rects = [];
for (const p of PIECES) for (const f of FACES) { const [w, h] = faceSize(p.from, p.to, f); rects.push({ piece: p, face: f, w, h }); }
for (const d of dao) for (const f of FACES) { const [w, h] = faceSize(d.from, d.to, f); rects.push({ piece: d, face: f, w, h, dao: true }); }
rects.sort((a, b) => b.h - a.h);
pack(rects);

/** The atlas of one outfit, RGBA. */
function paintAtlas(o) {
  const px = new Uint8Array(ATLAS * ATLAS * 4);
  const set = (x, y, rgb, a = 255) => { const i = (y * ATLAS + x) * 4; px[i] = rgb[0]; px[i + 1] = rgb[1]; px[i + 2] = rgb[2]; px[i + 3] = a; };
  rects.forEach((r, n) => {
    for (let v = 0; v < r.h; v++) for (let u = 0; u < r.w; u++) {
      let rgb;
      if (r.dao) {
        const c = r.piece.color;
        if (c === 'blade') {
          // The flats: dark spine at the top, bright edge at the bottom; the top face is the spine, the bottom the edge.
          if (r.face === 'up') rgb = hex(STEEL.spine);
          else if (r.face === 'down') rgb = hex(STEEL.edge);
          else if (r.face === 'east' || r.face === 'west') rgb = hex(v === 0 ? STEEL.spine : v === r.h - 1 ? STEEL.edge : STEEL.flat);
          else rgb = hex(STEEL.flat);
        } else if (c === 'grip') rgb = hex((u + v) % 2 === 0 ? STEEL.grip : STEEL.wrap);
        else rgb = hex(STEEL.guard);
        rgb = scale(rgb, LIGHT[r.face]);
      } else {
        const l = r.piece.paint(r.face, u, v, r.w, r.h, o);
        if (!l) continue;
        // A lower-case letter is its colour a shade darker (the lines between wrapping strips).
        rgb = l.startsWith('#') ? hex(l) : l === l.toLowerCase() ? scale(hex(o.c[l.toUpperCase()]), 0.82) : hex(o.c[l]);
        rgb = scale(rgb, LIGHT[r.face] * grain(u, v, n));
      }
      set(r.x + u, r.y + v, rgb);
    }
  });
  return png(ATLAS, ATLAS, px);
}

function png(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    Buffer.from(rgba.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  }
  const table = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
  const crc = buf => { let c = -1; for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; };
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
    return Buffer.concat([len, td, c]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------------------------------------------------------------- the Blockbench project

function element(p, faces) {
  return {
    name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from: p.from, to: p.to, autouv: 0, color: 0, origin: p.from.map((v, k) => (v + p.to[k]) / 2),
    faces, type: 'cube', uuid: uuid(),
  };
}

function build(outfitIndex, anims) {
  const elements = [];
  const byBone = {};
  const faceUvs = new Map();
  for (const r of rects) {
    if (!faceUvs.has(r.piece)) faceUvs.set(r.piece, {});
    faceUvs.get(r.piece)[r.face] = { uv: [r.x, r.y, r.x + r.w, r.y + r.h], texture: 0 };
  }
  for (const p of PIECES) {
    const e = element(p, faceUvs.get(p));
    elements.push(e);
    (byBone[p.bone] = byBone[p.bone] || []).push(e.uuid);
  }
  for (const d of dao) {
    const e = element(d, faceUvs.get(d));
    elements.push(e);
    (byBone.Tool_R = byBone.Tool_R || []).push(e.uuid);
  }
  const group = (name, kids = []) => ({ name, origin: at(name), color: 0, uuid: uuid(), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: true, autouv: 0, children: [...(byBone[name] || []), ...kids] });
  const head = group('Head');
  const armR = group('Arm_R', [group('Hand_R', [group('Tool_R')])]);
  const armL = group('Arm_L', [group('Hand_L', [group('Tool_L')])]);
  const chest = group('Chest', [head, group('Shoulder_R', [armR]), group('Shoulder_L', [armL])]);
  const torso = group('Torso', [chest]);
  const leg = side => group('Thigh_' + side, [group('Leg_' + side)]);
  const root = group('Root', [torso, leg('R'), leg('L')]);
  const outliner = [root];
  const groups = groupsByName(outliner);
  // Every outfit's atlas is in the project; the faces use the first, the others are a click away in the texture panel.
  const order = [outfitIndex, ...OUTFITS.map((_, i) => i).filter(i => i !== outfitIndex)];
  const textures = order.map(i => ({
    path: '', name: OUTFITS[i].key + '.png', folder: '', namespace: '', id: String(i), width: ATLAS, height: ATLAS,
    uv_width: ATLAS, uv_height: ATLAS, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '',
    render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false,
    visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + paintAtlas(OUTFITS[i]).toString('base64'),
  }));
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name: 'mountain_bandit', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
    timeline_setups: [], unhandled_root_fields: {}, resolution: { width: ATLAS, height: ATLAS },
    elements, outliner, textures,
    animations: Object.entries(anims).map(([n, keys]) => animation(n, keys, groups, 'loop')),
  };
}

// ---------------------------------------------------------------- a pose to look at it (pose.cjs notation)

/** At rest, the dao held low and forward, the weight on the back foot. */
const REST = (breath = 0) => ({
  Root: { pos: [0, 0, 0.764 - breath * 0.01] },
  Chest: [['x', -2 - breath * 2]], Head: [['x', 4 + breath]],
  Arm_R: [['x', 14 + breath * 3], ['y', -6]], Hand_R: [['x', 30]], Tool_R: [['x', -72]],
  Arm_L: [['x', 4 - breath * 2], ['y', 8]], Hand_L: [['x', 14]],
  Thigh_R: [['x', 8], ['y', -4]], Leg_R: [['x', -6]], Thigh_L: [['x', -6], ['y', 4]], Leg_L: [['x', -4]],
});
const ANIMS = { rest: [{ t: 0, pose: REST(0) }, { t: 1.2, pose: REST(1) }, { t: 2.4, pose: REST(0) }] };

module.exports = { OUTFITS, build, ANIMS };

if (require.main === module) {
  const dir = path.join(__dirname, '..', '..', 'art', 'blockbench');
  fs.mkdirSync(path.join(dir, 'mountain_bandit'), { recursive: true });
  fs.writeFileSync(path.join(dir, 'mountain_bandit.bbmodel'), JSON.stringify(build(0, ANIMS), null, 1));
  OUTFITS.forEach(o => fs.writeFileSync(path.join(dir, 'mountain_bandit', o.key + '.png'), paintAtlas(o)));
  console.log('wrote mountain_bandit.bbmodel and', OUTFITS.length, 'outfits;', PIECES.length + dao.length, 'cubes');
  // The previews (one project per outfit, its atlas first) for the renders.
  if (process.argv[2]) {
    fs.mkdirSync(process.argv[2], { recursive: true });
    OUTFITS.forEach((o, i) => fs.writeFileSync(path.join(process.argv[2], o.key + '.bbmodel'), JSON.stringify(build(i, ANIMS))));
  }
}
