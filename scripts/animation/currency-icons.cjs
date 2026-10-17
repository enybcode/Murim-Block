// The Murim's money, as in ancient China (docs/items/currency.md), the item icons (16 x 16) beside the Silver Tael's
// (silver-tael-icon.cjs), in the same style: lit from the top left, outlined in dark grey as Minecraft's items are.
//   copper_cash   a round bronze cash coin (铜钱) with its square hole, a raised rim round both, four characters
//                 hinted on its face;
//   cash_string   a string of cash (一串钱): a row of coins packed edge to edge on a red cord that comes out of both
//                 ends, a knot and a short tassel hanging from the right;
//   gold_yuanbao  a gold ingot (金元宝): the tael's boat shape, larger and fuller, in gold, a bright glint.
// Usage: node scripts/animation/currency-icons.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const ICONS = {
  // o outline, 1 to 5 bronze from shade to light, r the rim, h the hole's shadow, c the characters.
  copper_cash: {
    rows: [
      '................',
      '.....oooooo.....',
      '...oo555544oo...',
      '..o5r555444r3o..',
      '..o5c544443c3o..',
      '.o554oooooo433o.',
      '.o54ohhhhhho32o.',
      '.o5c4h....h4c2o.',
      '.o44ch....hc32o.',
      '.o443h....h322o.',
      '.o432oooooo222o.',
      '..o3c33322c22o..',
      '..o3r332222r1o..',
      '...oo222211oo...',
      '.....oooooo.....',
      '................',
    ],
    colours: { o: '#2a1d14', 1: '#5c3a1e', 2: '#80552c', 3: '#a87238', 4: '#c98f48', 5: '#e3b46a', r: '#f0c97e', h: '#3a2616', c: '#6b4524' },
  },
  // o outline, R the red cord, k the knot, 1 to 5 the bronze coins, h their holes.
  cash_string: {
    rows: [
      '................',
      '................',
      '................',
      '................',
      '..ooooooooooo...',
      '.o5o4o5o4o5o4o..',
      '.o5o4o5o4o5o4o..',
      'Ro5o4o5o4o5o3oRo',
      'oRhRRhRRhRRhRRRk',
      '.o4o3o4o3o4o2oRk',
      '.o3o2o3o2o3o2o.k',
      '.o3o2o3o2o3o1o.R',
      '..ooooooooooo..R',
      '...............R',
      '................',
      '................',
    ],
    colours: { o: '#2a1d14', R: '#c0202c', k: '#7a1418', 1: '#5c3a1e', 2: '#80552c', 3: '#a87238', 4: '#c98f48', 5: '#e3b46a', h: '#3a2616' },
  },
  // As the Silver Tael (silver-tael-icon.cjs), fuller, in gold.
  gold_yuanbao: {
    rows: [
      '................',
      '................',
      '.ooo........ooo.',
      'o5w4o......o432o',
      'o544oooooooo332o',
      'o5444o5ww44o332o',
      'o5444o55w44o332o',
      '.o4444o5544o332o',
      '.o4444o4444o332o',
      '..o44443333322o.',
      '..o44443333322o.',
      '...o4333332222o.',
      '...o3332222211o.',
      '....oo22221111o.',
      '......oooooooo..',
      '................',
    ],
    colours: { o: '#3a2410', 1: '#7a4e12', 2: '#a8701a', 3: '#d49a2a', 4: '#f0c24a', 5: '#ffe08a', w: '#fffbe6' },
  },
};

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const dir = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'item');
for (const [name, { rows, colours }] of Object.entries(ICONS)) {
  const rgba = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => {
    if (row.length !== 16) throw new Error(name + ' row ' + y + ' is ' + row.length + ' wide');
    [...row].forEach((c, x) => { if (colours[c]) rgba.set([...hex(colours[c]), 255], (y * 16 + x) * 4); });
  });
  fs.writeFileSync(path.join(dir, name + '.png'), kit.png(16, 16, rgba));
  console.log('wrote', name);
}
