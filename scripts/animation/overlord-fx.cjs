// The Overlord Spear's shapes of Qi (martial/OverlordQiEntity, client/OverlordQiRenderer), GeckoLib models drawn in one
// shared palette texture (textures/entity/overlord_qi.png, 32 x 32, one colour a texel; its _glowmask keeps the colours
// that shine). Each cube takes one texel: flat colours, shaded row by row, in the game's pixel style.
//   geo/overlord_beam.geo.json        Tyrant's Thrust's beam, one block long and one wide at scale 1 (the renderer
//                                     stretches it along its line): a white-hot core, an orange layer, a red sheath, a
//                                     spiral of Qi round them, a spearhead of light at its end;
//   geo/overlord_giant_spear.geo.json the ultimate's spear, point up, 4 blocks long at scale 1 (made ten times bigger):
//                                     a gold spiked pommel, an eight-sided black shaft banded in gold and cracked with
//                                     embers, a crimson mane of twelve ribbons, three gold claws, a broad leaf blade of
//                                     black steel with gold-white edges and a burning seam, wrapped in a shell of red Qi;
// Usage: node scripts/animation/overlord-fx.cjs
const fs = require('fs');
const path = require('path');
const { png } = require('./mesh-kit.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock');
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

// ---------------------------------------------------------------- the palette: a texel a colour

const S = 32, TEX = new Uint8Array(S * S * 4), GLOW = new Uint8Array(S * S * 4);
const AT = {};
let next = 0;
/** A colour of the palette: its texel; `glow` if it shines; `a` its opacity (255 solid). */
function colour(name, rgb, glow = false, a = 255) {
  const i = next++, x = i % S, y = Math.floor(i / S);
  TEX.set([...hex(rgb), a], (y * S + x) * 4);
  if (glow) GLOW.set([...hex(rgb), a], (y * S + x) * 4);
  AT[name] = [x, y];
}
// A ramp of `n` colours from one to the other, named name0..name(n-1).
function ramp(name, from, to, n, glow, a = 255) {
  const [r0, g0, b0] = hex(from), [r1, g1, b1] = hex(to);
  for (let i = 0; i < n; i++) {
    const k = n === 1 ? 0 : i / (n - 1), c = [r0 + (r1 - r0) * k, g0 + (g1 - g0) * k, b0 + (b1 - b0) * k].map(v => Math.round(v));
    colour(name + i, '#' + c.map(v => v.toString(16).padStart(2, '0')).join(''), glow, a);
  }
}
ramp('iron', '#1c1c21', '#3e3f48', 6, false);     // the shaft and the blade, dark to lit
ramp('gold', '#7a5418', '#ffe08a', 6, false);
ramp('edge', '#d8cfb8', '#fffaf0', 3, true);       // the blade's edges shine a little
ramp('ember', '#b0200c', '#ffe6a0', 6, true);      // red to white-hot
ramp('hair', '#5a0a0e', '#e0303a', 5, false);
ramp('shell', '#ff2a1a', '#ff8a3a', 3, true, 110); // the red Qi round the blade, see-through
ramp('core', '#ffd27a', '#ffffff', 3, true);
ramp('mid', '#ff5a1e', '#ffb050', 3, true, 210);
ramp('sheath', '#c81014', '#ff3a1a', 3, true, 120);
ramp('ring', '#b80c10', '#ffb040', 6, true, 230);
colour('wave0', '#ff3a14', true, 110);
colour('wave1', '#ff7a2a', true, 170);
colour('wave2', '#ffd890', true, 220);

const uvOf = name => { const [x, y] = AT[name]; return Object.fromEntries(FACES.map(f => [f, { uv: [x, y], uv_size: [1, 1] }])); };
/** A cube from `from` to `to` (model pixels) in one palette colour, turned `rot` degrees about `pivot`. */
const cube = (name, from, to, rot, pivot) => {
  const c = { origin: from, size: [0, 1, 2].map(i => +(to[i] - from[i]).toFixed(3)), uv: uvOf(name) };
  if (rot) { c.rotation = rot; c.pivot = pivot || [0, 0, 0]; }
  return c;
};
/** An eight-sided prism round the y axis from y0 to y1, `w` across. */
const prism = (name, y0, y1, w, out = []) => {
  out.push(cube(name, [-w / 2, y0, -w * 0.207], [w / 2, y1, w * 0.207]));
  out.push(cube(name, [-w * 0.207, y0, -w / 2], [w * 0.207, y1, w / 2]));
  out.push(cube(name, [-w / 2, y0, -w * 0.207], [w / 2, y1, w * 0.207], [0, 45, 0], [0, y0, 0]));
  out.push(cube(name, [-w * 0.207, y0, -w / 2], [w * 0.207, y1, w / 2], [0, 45, 0], [0, y0, 0]));
  return out;
};

function geo(id, cubes, bounds) {
  return {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: 'geometry.' + id, texture_width: S, texture_height: S, visible_bounds_width: bounds, visible_bounds_height: bounds,
        visible_bounds_offset: [0, bounds / 2, 0] },
      bones: [{ name: 'root', pivot: [0, 0, 0], cubes }],
    }],
  };
}
const write = (name, cubes, bounds) => {
  fs.writeFileSync(path.join(ASSETS, 'geo', name + '.geo.json'), JSON.stringify(geo(name, cubes, bounds), null, 1) + '\n');
  console.log(name, cubes.length, 'cubes');
};

// ---------------------------------------------------------------- the beam: 16 long, 16 wide at scale 1

{
  const c = [];
  prism('sheath0', 0, 12.5, 9, c);
  prism('mid1', 0, 13.5, 5.5, c);
  prism('core2', 0, 15, 2.6, c);
  // A spiral of Qi wound round it: short bars turning a sixth at each step up.
  for (let i = 0; i < 12; i++) {
    const y = 0.4 + i * 1.0, a = i * 60;
    c.push(cube(i % 2 ? 'ember5' : 'ember4', [4.0, y, -0.4], [5.0, y + 0.7, 0.4], [0, a, 0], [0, y, 0]));
  }
  // The spearhead of light: rows narrowing to a point.
  [[7, 'ember3'], [5.6, 'ember4'], [4.2, 'ember5'], [3, 'core1'], [1.8, 'core2'], [0.9, 'core2']].forEach(([w, m], i) => {
    const y = 12.5 + i * 0.6;
    c.push(cube(m, [-w / 2, y, -w * 0.18], [w / 2, y + 0.6, w * 0.18]));
  });
  write('overlord_beam', c, 2);
}

// ---------------------------------------------------------------- the giant spear: 64 long at scale 1, point up

{
  const c = [];
  // The pommel: a gold spike and a ring.
  c.push(cube('gold4', [-0.6, 0, -0.6], [0.6, 2.5, 0.6]));
  prism('gold2', 2.5, 4.2, 3.6, c);
  // The shaft: eight sides of black iron, gold bands, cracks of embers on four faces.
  prism('iron1', 4.2, 38, 2.6, c);
  for (const y of [9, 17, 25, 33]) prism('gold3', y, y + 0.9, 3.2, c);
  for (const [y, f] of [[6, 0], [12, 1], [20, 2], [28, 3], [35, 0], [14, 2], [22, 0], [30, 1]]) {
    const a = f * 90, len = 2.4 + (y % 3);
    c.push(cube('ember3', [-0.18, y, 1.2], [0.18, y + len, 1.42], [0, a, 0], [0, y, 0]));
    c.push(cube('ember5', [-0.07, y + 0.4, 1.38], [0.07, y + len - 0.4, 1.46], [0, a, 0], [0, y, 0]));
  }
  // The mane: a knot and twelve crimson ribbons flaring out and falling.
  prism('hair1', 37, 39, 4.2, c);
  for (let i = 0; i < 12; i++) {
    const a = i * 30, len = 7 + (i % 3) * 1.5;
    c.push(cube(i % 2 ? 'hair3' : 'hair4', [-0.45, 37.5 - len, 2.0], [0.45, 37.5, 2.7], [-14, a, 0], [0, 37.5, 0]));
    c.push(cube('hair2', [-0.3, 37.5 - len - 1.2, 2.45], [0.3, 37.5 - len, 3.0], [-14, a, 0], [0, 37.5, 0]));
  }
  // The socket and its collar, three gold claws gripping the blade.
  prism('gold4', 39, 40.2, 3.8, c);
  prism('iron3', 40.2, 43.5, 3.0, c);
  for (let k = 0; k < 3; k++) {
    const a = k * 120;
    c.push(cube('gold3', [-0.5, 41, 1.4], [0.5, 44.5, 2.2], [0, a, 0], [0, 41, 0]));
    c.push(cube('gold5', [-0.4, 44.5, 1.1], [0.4, 46.8, 1.8], [-18, a, 0], [0, 44.5, 0]));
    c.push(cube('gold4', [-0.3, 46.5, 0.6], [0.3, 48, 1.2], [-40, a, 0], [0, 46.5, 0]));
  }
  // The blade: a broad leaf in rows, the steel lit toward the point, gold-white edges, a ridge, a burning seam.
  // Twenty thin rows: a smooth leaf, widest a third of the way up.
  const W = [], N = 20;
  for (let i = 0; i < N; i++) { const t = (i + 0.5) / N; W.push(+(Math.max(0.8, 8.2 * Math.sin(Math.PI * Math.pow(t, 0.62)) * (1 - 0.15 * t))).toFixed(2)); }
  const row = 0.8, y0 = 43.5, T = 1.3;
  W.forEach((w, i) => {
    const y = y0 + i * row, e = Math.min(0.9, w / 4), lit = 'iron' + Math.min(5, 2 + Math.floor(i / 6));
    c.push(cube(lit, [-w / 2 + e, y, -T / 2], [w / 2 - e, y + row, T / 2]));
    c.push(cube(i < 3 ? 'gold5' : 'edge' + Math.min(2, Math.floor(i / 7)), [-w / 2, y, -T / 3], [-w / 2 + e, y + row, T / 3]));
    c.push(cube(i < 3 ? 'gold5' : 'edge' + Math.min(2, Math.floor(i / 7)), [w / 2 - e, y, -T / 3], [w / 2, y + row, T / 3]));
  });
  const top = y0 + W.length * row;
  c.push(cube('iron0', [-0.35, y0, -T / 2 - 0.12], [0.35, top - row, T / 2 + 0.12]));        // the ridge
  c.push(cube('ember4', [-0.2, y0 + 0.4, -T / 2 - 0.2], [0.2, top - row * 1.5, T / 2 + 0.2]));  // the seam
  c.push(cube('core2', [-0.08, y0 + 1.5, -T / 2 - 0.26], [0.08, top - row * 3, T / 2 + 0.26]));
  for (const [y, s] of [[y0 + 3.0, -1], [y0 + 6.2, 1], [y0 + 9.4, -1]]) c.push(cube('ember3', [s < 0 ? -2.4 : 0.2, y, -T / 2 - 0.15], [s < 0 ? -0.2 : 2.4, y + 0.3, T / 2 + 0.15]));
  c.push(cube('edge2', [-0.4, top, -0.3], [0.4, top + 1.6, 0.3]));                          // the point
  write('overlord_giant_spear', c, 5);
}

fs.mkdirSync(path.join(ASSETS, 'textures', 'entity'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_qi.png'), png(S, S, TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_qi_glowmask.png'), png(S, S, GLOW));
// GeckoLib wants an animation file: an empty one.
fs.writeFileSync(path.join(ASSETS, 'animations', 'overlord_qi.animation.json'), JSON.stringify({ format_version: '1.8.0', animations: {} }, null, 2) + '\n');
// The first textures (one per shape) are replaced by the palette; the ring and the cracks gave way to Epic Fight's fracture.
for (const old of ['overlord_ring', 'overlord_cracks']) { const g = path.join(ASSETS, 'geo', old + '.geo.json'); if (fs.existsSync(g)) fs.unlinkSync(g); }
for (const old of ['overlord_beam', 'overlord_beam_glowmask', 'overlord_giant_spear', 'overlord_giant_spear_glowmask']) {
  const f = path.join(ASSETS, 'textures', 'entity', old + '.png');
  if (fs.existsSync(f)) fs.unlinkSync(f);
}
console.log('palette', next, 'colours');
