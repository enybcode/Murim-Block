// The Crescent Moon Halberd's ultimate's moonlight (martial/MoonQiEntity, client/MoonQiRenderer), GeckoLib models in
// one shared palette texture (textures/entity/moon_qi.png, 32 x 32, a texel a colour; its _glowmask keeps what shines),
// voxels of flat colour in the game's pixel style, and their animations (animations/moon_qi.animation.json):
//   geo/moon_full.geo.json      the ultimate's full moon, 2 blocks across at scale 1 (made far bigger): a shell of pale
//                               moonstone with grey seas, wrapped in a halo of light (turn);
//   geo/moon_beam.geo.json      a beam of moonlight one block long and wide at scale 1 (stretched down to the ground).
// Usage: node scripts/animation/crescent-moon-fx.cjs
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
ramp('edge', '#cfeeff', '#ffffff', 3, true);            // the cutting edge, white-hot moonlight
ramp('flesh', '#5aa8ff', '#b8e4ff', 4, true, 235);      // the crescent's body, pale blue
ramp('sheath', '#3a78e0', '#7ab8ff', 3, true, 100);     // a see-through sheath of Qi
ramp('drop', '#8ccfff', '#eaf8ff', 3, true, 200);       // drops of light
ramp('water', '#1e4a8a', '#4a8ad8', 4, true, 150);      // moon-water, see-through
ramp('silver', '#9fb4d8', '#f0f6ff', 4, true);          // rims of light
ramp('stone', '#b8c0cc', '#f4f6fa', 5, true);           // the moon's pale stone
ramp('sea', '#7d8696', '#a4acb8', 3, true);             // its grey seas
ramp('halo', '#6aa8ff', '#cfe8ff', 3, true, 70);        // its halo
ramp('beam', '#7ab8ff', '#ffffff', 3, true, 170);

const uvOf = name => { const [x, y] = AT[name]; return Object.fromEntries(FACES.map(f => [f, { uv: [x, y], uv_size: [1, 1] }])); };
const cube = (name, from, to) => ({ origin: from, size: [0, 1, 2].map(i => +(to[i] - from[i]).toFixed(3)), uv: uvOf(name) });

/** A flat region of the XZ plane in voxels `step` px, at height y0..y1, merged in runs along x: `pick(x, z)` names the colour or null. */
function flat(step, half, y0, y1, pick) {
  const out = [];
  for (let z = -half; z < half; z += step) {
    let run = null;
    const flush = () => { if (run) out.push(cube(run.name, [run.x0, y0, z], [run.x1, y1, z + step])); run = null; };
    for (let x = -half; x < half; x += step) {
      const name = pick(x + step / 2, z + step / 2);
      if (name && run && run.name === name) run.x1 = x + step;
      else { flush(); if (name) run = { name, x0: x, x1: x + step }; }
    }
    flush();
  }
  return out;
}
/** The same in the XY plane (upright), merged along x. */
function upright(step, half, z0, z1, pick) {
  return flat(step, half, 0, 0, pick).map(c => {
    const [x, , z] = c.origin, [w, , d] = c.size;
    return { origin: [x, z, z0], size: [w, d, +(z1 - z0).toFixed(3)], uv: c.uv };
  });
}

function geo(id, bones, bounds) {
  return {
    format_version: '1.12.0',
    'minecraft:geometry': [{
      description: { identifier: 'geometry.' + id, texture_width: S, texture_height: S, visible_bounds_width: bounds, visible_bounds_height: bounds,
        visible_bounds_offset: [0, bounds / 4, 0] },
      bones,
    }],
  };
}
const write = (name, bones, bounds) => {
  fs.writeFileSync(path.join(ASSETS, 'geo', name + '.geo.json'), JSON.stringify(geo(name, bones, bounds), null, 1) + '\n');
  console.log(name, bones.reduce((n, b) => n + (b.cubes || []).length, 0), 'cubes');
};

// ---------------------------------------------------------------- the full moon, a sphere of 32 px (2 blocks) across

{
  const shell = [], Rm = 16, step = 2;
  for (let x = -Rm; x < Rm; x += step) for (let y = -Rm; y < Rm; y += step) for (let z = -Rm; z < Rm; z += step) {
    const cx = x + step / 2, cy = y + step / 2, cz = z + step / 2, d = Math.hypot(cx, cy, cz);
    if (d > Rm || d < Rm - step * 1.3) continue;
    // Grey seas in patches, the rest pale stone lit more at the top.
    const sea = Math.sin(cx * 0.35 + cy * 0.2) + Math.cos(cz * 0.3 - cy * 0.15) > 1.15;
    const name = sea ? 'sea' + ((x + y + z) / step & 1 ? 1 : 2) : 'stone' + Math.max(0, Math.min(4, Math.round(2 + cy / 8)));
    shell.push(cube(name, [x, y, z], [x + step, y + step, z + step]));
  }
  const halo = [];
  for (let i = 0; i < 48; i++) {
    const a = i * Math.PI * 2 / 48, r = 21, x = Math.cos(a) * r, y = Math.sin(a) * r;
    halo.push(cube('halo' + (i % 3), [x - 1.6, y - 1.6, -0.5], [x + 1.6, y + 1.6, 0.5]));
  }
  write('moon_full', [
    { name: 'moon', pivot: [0, 0, 0], cubes: shell },
    { name: 'halo', pivot: [0, 0, 0], cubes: halo },
  ], 3);
}

// ---------------------------------------------------------------- a beam of moonlight, 16 px long

{
  const c = [];
  c.push(cube('beam0', [-8, 0, -8], [8, 16, 8]));
  c.push(cube('beam1', [-5, 0, -5], [5, 16, 5]));
  c.push(cube('beam2', [-2.5, 0, -2.5], [2.5, 16, 2.5]));
  write('moon_beam', [{ name: 'root', pivot: [0, 0, 0], cubes: c }], 2);
}

// ---------------------------------------------------------------- the animations

const anims = {
  format_version: '1.8.0',
  animations: {
    // The full moon turns its face slowly, its halo the other way.
    turn: { loop: true, animation_length: 20, bones: {
      moon: { rotation: { '0.0': [0, 0, 0], '10.0': [0, 180, 0], '20.0': [0, 360, 0] } },
      halo: { rotation: { '0.0': [0, 0, 0], '10.0': [0, 0, -180], '20.0': [0, 0, -360] } },
    } },
  },
};
fs.writeFileSync(path.join(ASSETS, 'animations', 'moon_qi.animation.json'), JSON.stringify(anims, null, 1) + '\n');
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'moon_qi.png'), png(S, S, TEX));
fs.writeFileSync(path.join(ASSETS, 'textures', 'entity', 'moon_qi_glowmask.png'), png(S, S, GLOW));
console.log('moon_qi palette', next, 'colours');
