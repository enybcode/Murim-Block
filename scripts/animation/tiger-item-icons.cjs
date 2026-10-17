// The tiger's two drops, as 16 x 16 item icons outlined in dark brown like Minecraft's items:
//   tiger_pelt: the skin laid out flat like a rug, seen from above: the head at the top (white muzzle, pink nose, dark
//               eyes), the four legs splayed at the corners, the striped back down the middle, the tail below;
//   tiger_claw: one great curved claw, ivory shading to a dark root, hung on a loop of red cord.
// Also a picture of both, twelve times bigger, for the docs (docs/mobs/images/tiger-items.png).
//   node scripts/animation/tiger-item-icons.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ICONS = {
  tiger_pelt: {
    rows: [
      '................',
      '.....oooooo.....',
      '....o3s54s3o....',
      '....o4ewwe4o....',
      '.oo.o4wppw4o.oo.',
      'o3so.owwwwo.os3o',
      'o45oooo33oooo54o',
      '.o354s5444s453o.',
      '..o4s45ss54s4o..',
      '..o35s4444s53o..',
      '..o4s44ss44s4o..',
      '.o344s4444s443o.',
      'o43ooo4ss4ooo34o',
      'o3so..o44o..os3o',
      '.oo...o4so...oo.',
      '.......oo.......',
    ],
    colours: { o: '#3A2416', 3: '#C97F27', 4: '#E39A35', 5: '#F2B95A', s: '#1C2130', w: '#EEE7DC', e: '#1C2130', p: '#D88A8A' },
  },
  tiger_claw: {
    rows: [
      '................',
      '................',
      '......ooooo.....',
      '....oo44443oo...',
      '...o4443332211o.',
      '..o4433oooo21o..',
      '..o433o....o1o..',
      '.o4332o.....oo..',
      '.o4321o.........',
      '.o4321o.........',
      '.o3211o.........',
      '.orrrro.........',
      '..occo..........',
      '.oc..co.........',
      '.oc..co.........',
      '..occo..........',
    ],
    colours: { o: '#2B2620', 1: '#8E8574', 2: '#BDB4A2', 3: '#DCD3C0', 4: '#F5F0E4', r: '#5A3A2A', c: '#A8352A' },
  },
};

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const pixels = {};
for (const [id, { rows, colours }] of Object.entries(ICONS)) {
  if (rows.length !== 16) throw new Error(id + ' has ' + rows.length + ' rows');
  const rgba = new Uint8Array(16 * 16 * 4);
  rows.forEach((row, y) => {
    if (row.length !== 16) throw new Error(id + ' row ' + y + ' is ' + row.length + ' wide');
    [...row].forEach((c, x) => {
      if (c === '.') return;
      if (!colours[c]) throw new Error(id + ': no colour for ' + c);
      rgba.set([...hex(colours[c]), 255], (y * 16 + x) * 4);
    });
  });
  pixels[id] = rgba;
  const out = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'textures', 'item', id + '.png');
  fs.writeFileSync(out, kit.png(16, 16, rgba));
  console.log('wrote', path.relative(ROOT, out));
}

// Both, twelve times bigger, side by side on a parchment ground.
const S = 12, PAD = 24, W = PAD * 3 + 32 * S / 2 * 2, H = PAD * 2 + 16 * S;
const sheet = new Uint8Array(W * H * 4);
for (let i = 0; i < W * H; i++) sheet.set([237, 228, 207, 255], i * 4);
Object.values(pixels).forEach((rgba, k) => {
  const ox = PAD + k * (16 * S + PAD), oy = PAD;
  for (let y = 0; y < 16 * S; y++) for (let x = 0; x < 16 * S; x++) {
    const i = (Math.floor(y / S) * 16 + Math.floor(x / S)) * 4;
    if (rgba[i + 3]) sheet.set(rgba.slice(i, i + 4), ((oy + y) * W + ox + x) * 4);
  }
});
const doc = path.join(ROOT, 'docs', 'mobs', 'images', 'tiger-items.png');
fs.writeFileSync(doc, kit.png(W, H, sheet));
console.log('wrote', path.relative(ROOT, doc));
