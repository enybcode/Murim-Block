// The Silver Tael's item icon (16 x 16): a yuanbao, the boat-shaped silver ingot, its two ends raised like wings and a
// round dome in the middle, lit from the top left, outlined in dark grey as Minecraft's items are.
//   assets/murimblock/textures/item/silver_tael.png
// Usage: node scripts/animation/silver-tael-icon.cjs [out.png]
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

// o the outline, 1 to 5 the silver from shade to light, w a glint.
const ICON = [
  '................',
  '................',
  '................',
  '.ooo........ooo.',
  'o5w4o......o432o',
  'o544o.oooo.o332o',
  'o5444o5w44o3332o',
  '.o444o5544o3332o',
  '.o4444o44o33332o',
  '..o44443333332o.',
  '..o44433333222o.',
  '...o333322222o..',
  '....oo222211oo..',
  '......oooooo....',
  '................',
  '................',
];
const COLOURS = { o: '#2b2f36', 1: '#4f5660', 2: '#7d8590', 3: '#a7b0ba', 4: '#cdd4db', 5: '#eef2f6', w: '#ffffff' };

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const rgba = new Uint8Array(16 * 16 * 4);
ICON.forEach((row, y) => {
  if (row.length !== 16) throw new Error('row ' + y + ' is ' + row.length + ' wide');
  [...row].forEach((c, x) => { if (COLOURS[c]) rgba.set([...hex(COLOURS[c]), 255], (y * 16 + x) * 4); });
});
const out = process.argv[2] || path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'item', 'silver_tael.png');
fs.writeFileSync(out, kit.png(16, 16, rgba));
console.log('wrote', out);
