// The Murim swing trail (Enzo, 16/10/2026: "inspire-toi de la Poleblade", the Dawn Day blade's full, pixelated ribbon):
// our own 64 x 64 pixel-art trail texture in greys, each weapon's item skin tinting it, and the weapons' skins
// themselves (colour, the blade's span, a smooth ribbon of 20 interpolations).
// The texture across (v): 0 is the blade's tip, the swing's outer edge, 1 the hand (checked in game). A bright rim at the tip, a dark
// band under it, then the body fading towards the hand. Along it (u): 0 is the newest, 1 the oldest; the ribbon breaks
// into pixel streaks as it fades, the rim lasting longest.
//   node scripts/animation/swing-trail-texture.cjs
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const assets = path.join(__dirname, '../../src/main/resources/assets/murimblock');
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const [r, g, b, a] = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = r; raw[o + 1] = g; raw[o + 2] = b; raw[o + 3] = a; }
  const crc = (buf) => { let c = ~0; for (const v of buf) { c ^= v; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]), c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hash = (x, y) => { let h = (x * 374761393 + y * 668265263) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967295; };
const N = 64;
const texture = png(N, N, (x, y) => {
  const u = (x + 0.5) / N, v = (y + 0.5) / N;
  // Each row (a line across the blade) ends at its own length: the rim longest, the body shorter, ragged by pixels.
  const reach = (v < 0.07 ? 0.97 : v < 0.22 ? 0.9 : 0.84 - (v - 0.22) * 0.45) - hash(7, y) * 0.08;
  if (u > reach) return [0, 0, 0, 0];
  // Streaks: every few rows a lighter line runs along the trail, as the Poleblade's ribbon shows its sweep.
  const streak = y % 5 === 0 ? 1.12 : 1;
  let grey, alpha;
  // Epic Fight draws a trail half see-through whatever its alpha: the ribbon reads by contrast, a bright rim over a
  // band near black (the Poleblade's look), then the weapon's colour, full, down the blade.
  if (v < 0.08) { grey = 255; alpha = 255; }                                   // the outer rim, white-hot
  else if (v < 0.24) { grey = 45 + 40 * (v - 0.08) / 0.16; alpha = 255; }       // the dark band under the rim
  else if (v < 0.86) { grey = 175 - 55 * (v - 0.24) / 0.62; alpha = 255; }      // the body, the weapon's colour
  else { grey = 120; alpha = 255 * (1 - (v - 0.86) / 0.14); }                   // fading into the hand
  // Older, fainter; a few pixels dropped near the end, so it breaks up rather than fades smooth.
  const age = u / reach;
  alpha *= age < 0.7 ? 1 : 1 - (age - 0.7) / 0.3 * 0.55;
  if (age > 0.78 && hash(x, y) < (age - 0.78) * 2.2) alpha = 0;
  grey = Math.min(255, Math.round(grey * streak * (0.92 + hash(x * 3, y) * 0.08)));
  return [grey, grey, grey, Math.max(0, Math.round(alpha))];
});
fs.mkdirSync(path.join(assets, 'textures/particle'), { recursive: true });
fs.writeFileSync(path.join(assets, 'textures/particle/murim_trail.png'), texture);

// The weapons: colour (deep, as the Poleblade's), the span along the Tool joint (from near the hand to past the point).
const skin = (color, begin, end, lifetime = 5) => ({ trail: { color, begin_pos: begin, end_pos: end, lifetime, interpolations: 20,
  texture_path: 'murimblock:textures/particle/murim_trail.png', particle_type: 'epicfight:swing_trail' } });
const SKINS = {
  bandit_dao: skin([205, 190, 165], [0, 0, -0.1], [0, 0.2, -1.35]),
  snow_plum_jian: skin([255, 140, 185], [0, 0, -0.1], [0, 0.15, -1.45]),
  azure_cloud_sword: skin([70, 145, 255], [0, 0, -0.1], [0, 0.15, -1.45]),
  chief_great_axe: skin([225, 95, 35], [0, 0, -0.5], [0, -0.24, -2.1], 6),
  alliance_spear: skin([105, 160, 255], [0, 0.15, -0.5], [0, 0.25, -2.05], 6),
  crimson_dragon_spear: skin([215, 25, 35], [0, 0.15, -0.5], [0, 0.25, -2.05], 6),
  // The Spear of Ruin: an ember trail, orange as its burning cracks.
  spear_of_ruin: skin([255, 96, 30], [0, 0.15, -0.5], [0, 0.25, -2.1], 7),
  // The Crescent Moon Halberd: a trail of moonlight, pale blue.
  crescent_moon_halberd: skin([160, 215, 255], [0, 0.15, -0.5], [0, 0.25, -2.1], 7),
  bishou: skin([55, 205, 140], [0, 0, -0.1], [0, 0.1, -0.95]),
  flying_knife: skin([230, 45, 40], [0, 0, -0.1], [0, 0.1, -0.95]),
  iron_fan: skin([240, 195, 100], [0, 0, -0.05], [0, 0.1, -0.7]),
  chain_whip: skin([165, 175, 195], [0, 0, -0.1], [0, 0, -0.85]),
};
for (const [item, s] of Object.entries(SKINS))
  fs.writeFileSync(path.join(assets, 'item_skins', item + '.json'), JSON.stringify(s, null, 2) + '\n');
console.log('murim_trail.png and', Object.keys(SKINS).length, 'item skins');
