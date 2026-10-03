const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '../..');
const glyphSource = fs.readFileSync(path.join(__dirname, 'manuscript-glyphs.json'), 'utf8');
const glyphs = JSON.parse(glyphSource);
const definition = JSON.parse(fs.readFileSync(path.join(root,
  'src/main/resources/assets/murimblock/font/manuscript.json'), 'utf8'));
const provider = definition.providers.find(provider => provider.type === 'bitmap');
const image = new PNG({ width: provider.chars[0].length * 8, height: provider.chars.length * 8 });
image.data.fill(0);

provider.chars.forEach((line, row) => {
  assert.equal(line.length, 16);
  [...line].forEach((character, column) => {
    if (character === '\0') return;
    const glyph = glyphs[character];
    assert.ok(glyph, `Missing glyph ${character}`);
    assert.ok(glyph.length <= 8 && glyph.every(line => /^[01]{1,7}$/.test(line)));
    glyph.forEach((line, y) => [...line].forEach((pixel, x) => {
      if (pixel !== '1') return;
      const offset = ((row * 8 + y) * image.width + column * 8 + x) * 4;
      image.data.fill(255, offset, offset + 4);
    }));
  });
});

const destination = path.join(root, 'src/main/resources/assets/murimblock/textures/font/manuscript.png');
fs.mkdirSync(path.dirname(destination), { recursive: true });
fs.writeFileSync(destination, PNG.sync.write(image));
fs.writeFileSync(path.join(__dirname, 'manuscript-font.js'),
  '// Generated from manuscript-glyphs.json by generate-manuscript-font.cjs.\n'
  + 'globalThis.murimManuscriptGlyphs = ' + glyphSource.trimEnd() + ';\n');
console.log(`Generated ${image.width}x${image.height} manuscript font with binary alpha.`);
