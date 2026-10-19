// The Raven Messenger Art's doors (martial/RavenGateEntity, client/RavenGateRenderer) and its feathers, in the game's
// pixel style: one palette texture (textures/entity/raven_gate.png, 16 x 16, one colour a texel; its _glowmask keeps the
// colours that shine).
//   geo/raven_gate.geo.json  a door between the worlds, lying flat (its face up), one block across at scale 1 (the
//                            renderer sizes it, stands it up or lays it down, and turns it): a black heart going to deep
//                            blue, a ring of violet light, an amber rim flecked with gold (the raven's eye), a fringe of
//                            sixteen black feathers round it, and three arms of a dark swirl turning in it;
//   textures/particle/raven_feather_0.png, _1.png  a black feather with a blue sheen, two ways (8 x 8).
// Usage: node scripts/animation/raven-fx.cjs
const fs = require('fs');
const path = require('path');
const { png } = require('./mesh-kit.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock');
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the palette

const S = 16, TEX = new Uint8Array(S * S * 4), GLOW = new Uint8Array(S * S * 4);
const AT = {};
let next = 0;
function colour(name, rgb, glow = false, a = 255) {
  const i = next++, x = i % S, y = Math.floor(i / S);
  TEX.set([...hex(rgb), a], (y * S + x) * 4);
  if (glow) GLOW.set([...hex(rgb), a], (y * S + x) * 4);
  AT[name] = [x, y];
}
function ramp(name, from, to, n, glow, a = 255) {
  const [r0, g0, b0] = hex(from), [r1, g1, b1] = hex(to);
  for (let i = 0; i < n; i++) {
    const k = n === 1 ? 0 : i / (n - 1), c = [r0 + (r1 - r0) * k, g0 + (g1 - g0) * k, b0 + (b1 - b0) * k].map(v => Math.round(v));
    colour(name + i, '#' + c.map(v => v.toString(16).padStart(2, '0')).join(''), glow, a);
  }
}
ramp('void', '#020205', '#141a36', 4, false);        // the heart, black to deep blue
ramp('veil', '#3a1c6a', '#8a5ad8', 3, true, 200);    // the ring of violet light
ramp('rim', '#b0560e', '#ffd27a', 4, true);          // the amber rim, the raven's eye
colour('gold', '#fff0a8', true);
ramp('feather', '#07080e', '#26304e', 3, false);     // black feathers, a blue sheen on their edge
ramp('swirl', '#1a1030', '#4a2a7a', 2, true, 170);

const uvOf = name => { const [x, y] = AT[name]; return Object.fromEntries(FACES.map(f => [f, { uv: [x, y], uv_size: [1, 1] }])); };
const cube = (name, from, to, rot, pivot) => {
  const c = { origin: from, size: [0, 1, 2].map(i => +(to[i] - from[i]).toFixed(3)), uv: uvOf(name) };
  if (rot) { c.rotation = rot; c.pivot = pivot || [0, 0, 0]; }
  return c;
};
function write(name, cubes, bounds) {
  const geo = {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: 'geometry.' + name, texture_width: S, texture_height: S, visible_bounds_width: bounds, visible_bounds_height: bounds,
        visible_bounds_offset: [0, 0, 0] },
      bones: [{ name: 'root', pivot: [0, 0, 0], cubes }],
    }],
  };
  fs.writeFileSync(path.join(ASSETS, 'geo', name + '.geo.json'), JSON.stringify(geo, null, 1) + '\n');
  console.log('wrote', name, cubes.length, 'cubes');
}

// ---------------------------------------------------------------- the door: a disc of rows, 8 pixels across (radius), flat at y 0

{
  const R = 8, c = [];
  // Bands from the middle out: [outer radius, colour, height (layers above one another, no flicker)].
  const BANDS = [[1.6, 'void0', 0.1], [3.2, 'void1', 0.1], [4.6, 'void2', 0.1], [5.6, 'void3', 0.1], [6.3, 'veil1', 0.14], [6.8, 'veil2', 0.16],
    [7.3, 'rim1', 0.2], [7.7, 'rim2', 0.22], [8.0, 'rim3', 0.24]];
  const ROW = 0.5;
  for (let z = -R; z < R; z += ROW) {
    const zc = z + ROW / 2;
    let inner = 0;
    for (const [outer, col, h] of BANDS) {
      if (Math.abs(zc) >= outer) { inner = outer; continue; }
      const xo = Math.sqrt(outer * outer - zc * zc), xi = Math.abs(zc) < inner ? Math.sqrt(inner * inner - zc * zc) : 0;
      if (xi === 0) c.push(cube(col, [-xo, 0, z], [xo, h, z + ROW]));
      else {
        c.push(cube(col, [-xo, 0, z], [-xi, h, z + ROW]));
        c.push(cube(col, [xi, 0, z], [xo, h, z + ROW]));
      }
      inner = outer;
    }
  }
  // Gold flecks on the rim.
  for (let k = 0; k < 8; k++) {
    const a = k * 45 + 22.5;
    c.push(cube('gold', [-0.3, 0.2, 7.3], [0.3, 0.3, 7.9], [0, a, 0], [0, 0, 0]));
  }
  // The feathers: sixteen round the rim, long and tapering, every other one a little shorter and lighter.
  for (let k = 0; k < 16; k++) {
    const a = k * 22.5, long = k % 2 === 0, len = long ? 4.2 : 3.0, tone = long ? 'feather0' : 'feather1';
    c.push(cube(tone, [-0.7, 0.05, 7.4], [0.7, 0.18, 7.4 + len * 0.6], [0, a, 0], [0, 0, 0]));
    c.push(cube(tone, [-0.45, 0.05, 7.4 + len * 0.6], [0.45, 0.18, 7.4 + len * 0.85], [0, a, 0], [0, 0, 0]));
    c.push(cube('feather2', [-0.2, 0.06, 7.4 + len * 0.85], [0.2, 0.17, 7.4 + len], [0, a, 0], [0, 0, 0]));
    c.push(cube('feather2', [-0.08, 0.19, 7.6], [0.08, 0.22, 7.4 + len * 0.8], [0, a, 0], [0, 0, 0]));   // the quill
  }
  // The swirl: three arms of small squares winding in from the veil to the heart.
  for (let arm = 0; arm < 3; arm++) {
    for (let r = 1.2; r < 5.6; r += 0.55) {
      const th = arm * 2 * Math.PI / 3 + r * 0.55, x = Math.cos(th) * r, z = Math.sin(th) * r, s = 0.25 + r * 0.06;
      c.push(cube(r < 3 ? 'swirl0' : 'swirl1', [x - s, 0.12, z - s], [x + s, 0.16, z + s]));
    }
  }
  write('raven_gate', c, 2);
}

fs.mkdirSync(path.join(ASSETS, 'textures', 'entity'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'raven_gate.png'), png(S, S, TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'raven_gate_glowmask.png'), png(S, S, GLOW));
fs.writeFileSync(path.join(ASSETS, 'animations', 'raven_gate.animation.json'), JSON.stringify({ format_version: '1.8.0', animations: {} }, null, 2) + '\n');

// ---------------------------------------------------------------- the feathers (8 x 8 particles)

function feather(rows) {
  const P = { '.': null, k: '#07080e', b: '#1a2240', s: '#3a4a78', q: '#8a96b8' };
  const px = new Uint8Array(8 * 8 * 4);
  rows.forEach((row, y) => [...row].forEach((ch, x) => { const c = P[ch]; if (c) px.set([...hex(c), 255], (y * 8 + x) * 4); }));
  return png(8, 8, px);
}
fs.mkdirSync(path.join(ASSETS, 'textures', 'particle'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures', 'particle', 'raven_feather_0.png'), feather([
  '......kb',
  '.....kbs',
  '....kbs.',
  '...kbk..',
  '..kbk...',
  '.kbk....',
  '.qk.....',
  'q.......',
]));
fs.writeFileSync(path.join(ASSETS, 'textures', 'particle', 'raven_feather_1.png'), feather([
  '........',
  '.....kk.',
  '....kbsk',
  '...kbsk.',
  '..kbbk..',
  '.kbk....',
  'qkk.....',
  'q.......',
]));
fs.mkdirSync(path.join(ASSETS, 'particles'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'particles', 'raven_feather.json'), JSON.stringify({ textures: ['murimblock:raven_feather_0', 'murimblock:raven_feather_1'] }) + '\n');
console.log('palette', next, 'colours');
