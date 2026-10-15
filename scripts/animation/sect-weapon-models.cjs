// The models of the three sect weapons (sect-weapons.cjs gives their moves), one shared palette texture
// (textures/item/sect_weapons.png: a row of 16 shades per material):
//   iron fan (铁扇)    drawn by client/FanRenderer from its pieces, opened round its pivot: iron_fan_rib (a lacquered rib),
//                      iron_fan_guard (an outer steel guard stick), iron_fan_panel (a wedge of azure silk with a gold rim and
//                      a white lotus petal), iron_fan_rivet (the pivot);
//   chain whip (九节鞭) its handle (chain_whip_handle) and one link (chain_whip_link) and the dart at its end
//                      (chain_whip_dart): client/ChainWhipRenderer hangs the links one after another on a simulated chain;
//   flying needles      three needles held between the fingers (flying_needles_3d), and one needle in flight
//                      (flying_needle, drawn along +x by client/FlyingNeedleRenderer).
// Every piece is built upright about the model's centre; the renderers turn them onto the swords' 45-degree diagonal, so
// the vanilla sword's hand transforms (the _held models) put them in the hand. The flat icons (inventory, ground, frames)
// are drawn here too.
// Usage: node scripts/animation/sect-weapon-models.cjs
const fs = require('fs');
const path = require('path');
const { palette } = require('../concepts/chief-axes.cjs');
const { png } = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const COLOURS = {
  steel: '#a9b0b6', edge: '#eef1f3', lacquer: '#25222e', gold: '#D9B25F', silk: '#2B5FA8', silkLight: '#5d8fd6',
  lotus: '#f4f7fb', grip: '#14284A', cord: '#2B5FA8', red: '#c4382c', iron: '#565c63',
};
const PAL = palette(COLOURS);
const TEXTURE = 'murimblock:item/sect_weapons';

/** A model of boxes: [material, shade 0-15, from, to]. Its faces read one texel of the palette. */
function model(boxes, credit) {
  const face = ([mat, shade]) => {
    const row = PAL.names.indexOf(mat);
    if (row < 0) throw new Error('no material ' + mat);
    return { uv: [shade, row, shade + 1, row + 1], texture: '#0' };
  };
  return {
    credit, textures: { 0: TEXTURE, particle: TEXTURE },
    elements: boxes.map(b => ({ from: b[2], to: b[3], faces: Object.fromEntries(['north', 'east', 'south', 'west', 'up', 'down'].map(f => [f, face(b)])) })),
  };
}
const CREDIT = 'Murimblock - scripts/animation/sect-weapon-models.cjs';
const write = (name, json) => fs.writeFileSync(path.join(ASSETS, 'models', 'item', name + '.json'), JSON.stringify(json, null, 1) + '\n');

// ---------------------------------------------------------------- the iron fan: its pivot at (8, PIVOT, 8)

const PIVOT = 1.0, RIB = 10.0;
/** The fan's geometry, shared with FanRenderer: the pivot's height, the ribs' length, how far it opens (degrees). */
const FAN = { pivot: PIVOT, rib: RIB, ribs: 13, open: 150 };
write('iron_fan_rib', model([
  ['lacquer', 6, [7.85, PIVOT, 7.88], [8.15, PIVOT + RIB, 8.12]],
  ['gold', 12, [7.88, PIVOT + RIB - 0.5, 7.86], [8.12, PIVOT + RIB, 8.14]],
], CREDIT));
write('iron_fan_guard', model([
  ['steel', 9, [7.75, PIVOT, 7.78], [8.25, PIVOT + RIB + 0.4, 8.22]],
  ['edge', 15, [7.75, PIVOT + 2, 7.8], [7.85, PIVOT + RIB + 0.4, 8.2]],
  ['gold', 12, [7.7, PIVOT + 0.6, 7.72], [8.3, PIVOT + 1.4, 8.28]],
], CREDIT));
// A wedge of silk between two ribs: narrow by the pivot, as wide as the gap at the rim, a gold rim, a white lotus petal.
const GAP = 2 * Math.PI * RIB * (FAN.open / 360) / (FAN.ribs - 1);
const panel = [];
for (let i = 0; i < 8; i++) {
  const r0 = PIVOT + 2 + i * (RIB - 2.6) / 8, r1 = r0 + (RIB - 2.6) / 8, w = GAP * ((r0 + r1) / 2 - PIVOT) / RIB + 0.15;
  panel.push([i % 2 ? 'silk' : 'silkLight', 7 + (i % 3), [8 - w / 2, r0, 7.96], [8 + w / 2, r1, 8.04]]);
}
panel.push(['gold', 12, [8 - GAP / 2 - 0.1, PIVOT + RIB - 0.6, 7.94], [8 + GAP / 2 + 0.1, PIVOT + RIB - 0.2, 8.06]]);
panel.push(['lotus', 14, [7.7, PIVOT + RIB - 3.6, 7.92], [8.3, PIVOT + RIB - 1.6, 8.08]]);
write('iron_fan_panel', model(panel, CREDIT));
write('iron_fan_rivet', model([
  ['gold', 12, [7.4, PIVOT - 0.6, 7.6], [8.6, PIVOT + 0.6, 8.4]],
  ['red', 9, [7.9, PIVOT - 3.2, 7.9], [8.1, PIVOT - 0.6, 8.1]],
  ['red', 10, [7.7, PIVOT - 4.4, 7.7], [8.3, PIVOT - 3.2, 8.3]],
], CREDIT));

// ---------------------------------------------------------------- the chain whip

/** The handle: a pommel, a grip wound in azure cord, a gold collar, the ring the chain hangs from at its top (y TOP). */
const WHIP = { top: 4.6, link: 2.6, links: 10 };
write('chain_whip_handle', model([
  ['gold', 12, [7.3, -3.6, 7.3], [8.7, -2.6, 8.7]],
  ['grip', 7, [7.45, -2.6, 7.45], [8.55, 2.6, 8.55]],
  ...[-1.8, -0.6, 0.6, 1.8].map(y => ['cord', 10, [7.4, y, 7.4], [8.6, y + 0.4, 8.6]]),
  ['gold', 11, [7.25, 2.6, 7.25], [8.75, 3.4, 8.75]],
  ['steel', 9, [7.6, 3.4, 7.6], [8.4, 4.0, 8.4]],
  ['iron', 6, [7.75, 4.0, 7.9], [8.25, WHIP.top, 8.1]],
], CREDIT));
// One link from (8, 8, 8) along +y, WHIP.link long: a steel bar between two rings.
write('chain_whip_link', model([
  ['iron', 6, [7.82, 8, 7.82], [8.18, 8 + 0.4, 8.18]],
  ['steel', 9, [7.7, 8 + 0.4, 7.7], [8.3, 8 + WHIP.link - 0.4, 8.3]],
  ['edge', 14, [7.7, 8 + 0.6, 7.7], [7.8, 8 + WHIP.link - 0.6, 8.3]],
  ['iron', 6, [7.82, 8 + WHIP.link - 0.4, 7.82], [8.18, 8 + WHIP.link, 8.18]],
], CREDIT));
// The dart at the end, from (8, 8, 8) along +y, and a red cloth behind it.
write('chain_whip_dart', model([
  ['red', 9, [7.5, 8, 7.9], [8.5, 9.2, 8.1]],
  ['steel', 10, [7.6, 9.2, 7.6], [8.4, 10.4, 8.4]],
  ['edge', 15, [7.75, 10.4, 7.75], [8.25, 11.6, 8.25]],
  ['edge', 15, [7.9, 11.6, 7.9], [8.1, 12.4, 8.1]],
], CREDIT));

// ---------------------------------------------------------------- the flying needles

/** Three needles held between the fingers, upright, a little spread; turned onto the diagonal like a sword. */
const needle = (x, y0, y1, z) => [
  ['steel', 10, [x - 0.12, y0, z - 0.12], [x + 0.12, y1 - 0.8, z + 0.12]],
  ['edge', 15, [x - 0.07, y1 - 0.8, z - 0.07], [x + 0.07, y1, z + 0.07]],
  ['gold', 12, [x - 0.18, y0, z - 0.18], [x + 0.18, y0 + 0.4, z + 0.18]],
];
const held = model([...needle(7.2, 2.0, 9.6, 8), ...needle(8, 2.4, 10.4, 8), ...needle(8.8, 2.0, 9.6, 8)], CREDIT);
held.elements.forEach(e => { e.rotation = { angle: -45, axis: 'z', origin: [8, 8, 8] }; });
write('flying_needles_3d', { ...held, display: {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
} });
// One needle in flight, along +x through the centre, 8 pixels long.
write('flying_needle', model([
  ['gold', 12, [4, 7.82, 7.82], [4.6, 8.18, 8.18]],
  ['steel', 10, [4.6, 7.88, 7.88], [11, 8.12, 8.12]],
  ['edge', 15, [11, 7.92, 7.92], [12.4, 8.08, 8.08]],
], CREDIT));

// ---------------------------------------------------------------- the held models (hand transforms only) and the items

const SWORD_DISPLAY = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 4, 0.5], scale: [0.85, 0.85, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
  head: { rotation: [0, 180, 0], translation: [0, 13, 7], scale: [1, 1, 1] },
};
const flat = name => ({ parent: 'minecraft:item/handheld', textures: { layer0: 'murimblock:item/' + name } });
for (const name of ['iron_fan', 'chain_whip']) {
  write(name + '_held', { credit: CREDIT, parent: 'builtin/entity', textures: { particle: 'murimblock:item/' + name }, display: SWORD_DISPLAY });
  write(name, { loader: 'neoforge:separate_transforms', base: { parent: 'murimblock:item/' + name + '_held' },
    perspectives: { gui: flat(name), ground: flat(name), fixed: flat(name) } });
}
write('flying_needles', { loader: 'neoforge:separate_transforms', base: { parent: 'murimblock:item/flying_needles_3d' },
  perspectives: { gui: flat('flying_needles'), ground: flat('flying_needles'), fixed: flat('flying_needles') } });

// ---------------------------------------------------------------- the flat icons, 16 x 16

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
function icon(draw) {
  const img = new Uint8Array(16 * 16 * 4);
  const set = (x, y, c) => { if (x >= 0 && y >= 0 && x < 16 && y < 16) img.set(hex(c), (y * 16 + x) * 4); };
  draw(set);
  // A dark outline round the shape.
  const out = new Uint8Array(img);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (img[(y * 16 + x) * 4 + 3]) continue;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => { const a = x + dx, b = y + dy; return a >= 0 && b >= 0 && a < 16 && b < 16 && img[(b * 16 + a) * 4 + 3]; }))
      out.set([20, 22, 34, 255], (y * 16 + x) * 4);
  }
  return png(16, 16, out);
}
const ICONS = {
  // The open fan: an arc of silk round the pivot at the bottom left, ribs radiating, a gold rim.
  iron_fan: set => {
    const px = 3, py = 13;
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const dx = x - px, dy = py - y, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx) * 180 / Math.PI;
      if (a < -8 || a > 98 || r > 11.5) continue;
      if (r > 10.5) set(x, y, COLOURS.gold);
      else if (r > 3) set(x, y, Math.round(a / 12) % 2 ? COLOURS.silk : COLOURS.silkLight);
      else set(x, y, COLOURS.lacquer);
      if (r > 3 && r <= 10.5 && Math.abs(((a + 6) % 12) - 6) < 1.2) set(x, y, COLOURS.lacquer);
    }
    set(px, py, COLOURS.gold); set(px + 1, py, COLOURS.gold); set(px, py - 1, COLOURS.gold);
    set(px - 1, py + 1, COLOURS.red); set(px - 2, py + 2, COLOURS.red);
  },
  // The whip: the handle at the bottom left, the chain curling up to the dart at the top right.
  chain_whip: set => {
    [[2, 14], [3, 13], [4, 12]].forEach(([x, y]) => set(x, y, COLOURS.grip));
    set(1, 15, COLOURS.gold); set(5, 11, COLOURS.gold);
    const chain = [[6, 10], [7, 9], [8, 9], [9, 10], [10, 10], [11, 9], [12, 8], [12, 7], [11, 6], [10, 5], [10, 4], [11, 3], [12, 2]];
    chain.forEach(([x, y], i) => set(x, y, i % 2 ? COLOURS.steel : COLOURS.iron));
    set(13, 1, COLOURS.edge); set(14, 0, COLOURS.edge); set(11, 2, COLOURS.red);
  },
  // Three needles fanned between the fingers.
  flying_needles: set => {
    for (let i = 0; i < 10; i++) { set(3 + i, 12 - i, COLOURS.steel); set(2 + i, 10 - i, COLOURS.steel); set(5 + i, 13 - i, COLOURS.steel); }
    set(13, 2, COLOURS.edge); set(12, 0, COLOURS.edge); set(15, 3, COLOURS.edge);
    set(3, 12, COLOURS.gold); set(2, 10, COLOURS.gold); set(5, 13, COLOURS.gold);
  },
};
fs.writeFileSync(path.join(ASSETS, 'textures', 'item', 'sect_weapons.png'), PAL.png);
for (const [name, draw] of Object.entries(ICONS)) fs.writeFileSync(path.join(ASSETS, 'textures', 'item', name + '.png'), icon(draw));

module.exports = { FAN, WHIP };
console.log('wrote the sect weapons: fan', FAN, 'whip', WHIP);
