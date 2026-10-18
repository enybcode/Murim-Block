// The Overlord Spear's shapes of Qi (martial/OverlordQiEntity, client/OverlordQiRenderer), GeckoLib models:
//   geo/overlord_beam.geo.json            Tyrant's Thrust's beam: a lance of light one block long and one wide (the renderer
//                                         stretches it along its line): a white-gold core, a red sheath, a gold point;
//   textures/entity/overlord_beam.png     its texture, the sheath half see-through; _glowmask: all of it shines;
//   geo/overlord_giant_spear.geo.json     the ultimate's spear: the Spear of Ruin's own cubes (ruin-spear-model.cjs), its
//                                         point up, two blocks long (the renderer makes it twenty times bigger);
//   textures/entity/overlord_giant_spear.png  the spear's texture; _glowmask: its burning cracks and its edges shine.
// Usage: node scripts/animation/overlord-fx.cjs
const fs = require('fs');
const path = require('path');
const { png } = require('./mesh-kit.cjs');
const ruin = require('./ruin-spear-model.cjs');

const ASSETS = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'murimblock');
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];

function geo(id, w, h, cubes, bounds) {
  return {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: 'geometry.' + id, texture_width: w, texture_height: h, visible_bounds_width: bounds, visible_bounds_height: bounds,
        visible_bounds_offset: [0, bounds / 2, 0] },
      bones: [{ name: 'root', pivot: [0, 0, 0], cubes }],
    }],
  };
}

// ---------------------------------------------------------------- the beam

// A 16 x 16 texture of three swatches: the core (white-gold), the sheath (red, half see-through), the point (gold).
const BEAM_TEX = new Uint8Array(16 * 16 * 4), BEAM_GLOW = new Uint8Array(16 * 16 * 4);
const swatch = (sx, sy, paint) => { for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) { const [rgb, a] = paint(u, v); BEAM_TEX.set([...hex(rgb), a], ((sy + v) * 16 + sx + u) * 4); BEAM_GLOW.set([...hex(rgb), a], ((sy + v) * 16 + sx + u) * 4); } };
swatch(0, 0, (u, v) => [(u + v) % 3 === 0 ? '#ffffff' : '#fff1c2', 255]);
swatch(8, 0, (u, v) => [u % 3 === 0 ? '#ff7a2e' : '#ff2a1a', 190]);
swatch(0, 8, (u, v) => [v % 2 ? '#ffd27a' : '#f0b030', 255]);
const uvOf = ([sx, sy], w, h) => Object.fromEntries(FACES.map(f => [f, { uv: [sx, sy], uv_size: [Math.max(1, Math.min(8, w)), Math.max(1, Math.min(8, h))] }]));
const box = (origin, size, at) => ({ origin, size, uv: uvOf(at, Math.round(size[0] / 2), Math.round(size[1] / 2)) });
const beamCubes = [
  box([-6, 0, -6], [12, 12, 12], [8, 0]),          // the red sheath
  box([-4.5, 12, -4.5], [9, 2, 9], [8, 0]),
  box([-2.5, 0, -2.5], [5, 15, 5], [0, 0]),         // the white-gold core, out past the sheath
  box([-3, 13, -3], [6, 1.5, 6], [0, 8]),           // the point, gold, narrowing
  box([-1.5, 14.5, -1.5], [3, 1, 3], [0, 8]),
  box([-0.5, 15.5, -0.5], [1, 0.5, 1], [0, 0]),
];
fs.writeFileSync(path.join(ASSETS, 'geo', 'overlord_beam.geo.json'), JSON.stringify(geo('overlord_beam', 16, 16, beamCubes, 4), null, 2) + '\n');
fs.mkdirSync(path.join(ASSETS, 'textures', 'entity'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_beam.png'), png(16, 16, BEAM_TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_beam_glowmask.png'), png(16, 16, BEAM_GLOW));

// ---------------------------------------------------------------- the giant spear

// The Spear of Ruin's cubes, upright (they are built upright before the item's 45 degree tilt), centred on x and z, the
// butt at y 0. Bedrock's x runs the other way: mirrored, which the spear does not mind.
const y0 = Math.min(...ruin.PARTS.map(p => p.from[1]));
const spearCubes = ruin.PARTS.map(p => {
  const f = ruin.faces(p);
  const size = [0, 1, 2].map(i => +(p.to[i] - p.from[i]).toFixed(4));
  return {
    origin: [+(-(p.to[0] - 8)).toFixed(4), +(p.from[1] - y0).toFixed(4), +(p.from[2] - 8).toFixed(4)], size,
    uv: Object.fromEntries(FACES.map(face => [face, { uv: [f[face].uv[0], f[face].uv[1]], uv_size: [f[face].uv[2] - f[face].uv[0], f[face].uv[3] - f[face].uv[1]] }])),
  };
});
const top = Math.max(...ruin.PARTS.map(p => p.to[1])) - y0;
fs.writeFileSync(path.join(ASSETS, 'geo', 'overlord_giant_spear.geo.json'), JSON.stringify(geo('overlord_giant_spear', ruin.TEX_SIZE, ruin.TEX_SIZE, spearCubes, Math.ceil(top / 16) + 1), null, 2) + '\n');
// Its texture, and a glowmask keeping only the embers (and a little of the gold-white edges).
const S = ruin.TEX_SIZE, glow = new Uint8Array(S * S * 4);
const [ex, ey] = ruin.SWATCH_AT.ember, [gx, gy] = ruin.SWATCH_AT.edge;
for (let v = 0; v < 8; v++) for (let u = 0; u < 8; u++) {
  for (const [sx, sy] of [[ex, ey], [gx, gy]]) { const o = ((sy + v) * S + sx + u) * 4; glow.set(ruin.TEX.subarray(o, o + 4), o); }
}
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_giant_spear.png'), png(S, S, ruin.TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'overlord_giant_spear_glowmask.png'), png(S, S, glow));
// GeckoLib wants an animation file: an empty one.
fs.writeFileSync(path.join(ASSETS, 'animations', 'overlord_qi.animation.json'), JSON.stringify({ format_version: '1.8.0', animations: {} }, null, 2) + '\n');
console.log('wrote the beam (', beamCubes.length, 'cubes) and the giant spear (', spearCubes.length, 'cubes,', (top / 16).toFixed(2), 'blocks long)');
