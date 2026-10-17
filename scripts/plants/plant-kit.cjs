// The plants' kit (docs/cultivation/plants.md): a herb as small boxes, painted texel by texel, in growth stages, written
// as GeckoLib models (one geometry a stage, one texture a colour, their animations) for the planted and spirit herbs,
// which move; the same boxes are what the still wild plants are made of. Every box turns about one axis only, by 0,
// 22.5 or 45 degrees either way: what Minecraft's block models allow, so a wild plant can be the same model, baked.
//
// Space: Blockbench pixels, 16 a block, y up, the plant's foot at (0, 0, 0) in the middle of its block.
// A box: its stages (the ones it shows in), its bone, its size, its centre, how it is turned, and paint(point, normal,
// colours, texel) -> '#rrggbb' or null (clear), the point where the texel lies once the box is turned.
const fs = require('fs');
const path = require('path');
const kit = require('../animation/mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const ALLOWED = [-45, -22.5, 0, 22.5, 45];

const hexRgb = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const hex = c => '#' + c.map(x => Math.max(0, Math.min(255, Math.round(x))).toString(16).padStart(2, '0')).join('');
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const mix = (a, b, t) => (typeof a === 'string' ? mix(hexRgb(a), b, t) : typeof b === 'string' ? mix(a, hexRgb(b), t) : a.map((x, i) => x + (b[i] - x) * clamp(t)));

/** A point turned `deg` about `axis` round `pivot` (Blockbench's and GeckoLib's sense). */
function turn(axis, deg, pivot, p) {
  if (!deg) return p;
  const a = deg * Math.PI / 180, c = Math.cos(a), s = Math.sin(a), [x, y, z] = p.map((v, i) => v - pivot[i]);
  const r = axis === 'x' ? [x, y * c - z * s, y * s + z * c] : axis === 'y' ? [x * c + z * s, y, -x * s + z * c] : [x * c - y * s, x * s + y * c, z];
  return r.map((v, i) => v + pivot[i]);
}
const CORNERS = {
  north: (a, b) => [[b[0], b[1], a[2]], [a[0], b[1], a[2]], [a[0], a[1], a[2]]], south: (a, b) => [[a[0], b[1], b[2]], [b[0], b[1], b[2]], [b[0], a[1], b[2]]],
  east: (a, b) => [[b[0], b[1], b[2]], [b[0], b[1], a[2]], [b[0], a[1], a[2]]], west: (a, b) => [[a[0], b[1], a[2]], [a[0], b[1], b[2]], [a[0], a[1], b[2]]],
  up: (a, b) => [[b[0], b[1], b[2]], [a[0], b[1], b[2]], [a[0], b[1], a[2]]], down: (a, b) => [[b[0], a[1], a[2]], [a[0], a[1], a[2]], [a[0], a[1], b[2]]],
};
const NORMAL = { north: [0, 0, -1], south: [0, 0, 1], east: [1, 0, 0], west: [-1, 0, 0], up: [0, 1, 0], down: [0, -1, 0] };

class Plant {
  /** name: the herb's id; stages: how many; density: texels a pixel; atlas: the texture's side in texels. */
  constructor(name, stages, density = 2, atlas = 256) {
    this.name = name;
    this.stages = stages;
    this.D = density;
    this.atlas = atlas;
    this.pieces = [];
    /** Per stage: bone name -> { parent, pivot }. */
    this.bones = Array.from({ length: stages }, () => ({ root: { pivot: [0, 0, 0] } }));
  }

  bone(stages, name, parent, pivot) {
    for (const s of stages) this.bones[s][name] = { parent, pivot };
  }

  /** A box. deg must be one of ALLOWED. */
  box(stages, bone, size, centre, paint, axis = 'y', deg = 0, glow = null) {
    if (!ALLOWED.includes(deg)) throw new Error(`${this.name}: a box turned ${deg} degrees`);
    const from = centre.map((v, i) => v - size[i] / 2), to = centre.map((v, i) => v + size[i] / 2);
    const id = this.pieces.length;
    const p = { stages, bone, from, to, density: this.D, axis, deg, pivot: centre };
    const at = (f, u, v, w, h) => {
      const [tl, tr, bl] = CORNERS[f](from, to), a = (u + 0.5) / w, b = (v + 0.5) / h;
      return turn(axis, deg, centre, [0, 1, 2].map(i => tl[i] + (tr[i] - tl[i]) * a + (bl[i] - tl[i]) * b));
    };
    p.paint = (f, u, v, w, h, outfit) => {
      const c = paint(at(f, u, v, w, h), turn(axis, deg, [0, 0, 0], NORMAL[f]), outfit.c, { f, u, v, w, h, id });
      if (!c) return null;
      const k = 0.95 + 0.08 * (((u * 73 + v * 151 + id * 37) % 11) / 10);
      return hex(hexRgb(c).map(x => x * k));
    };
    p.glow = glow ? (f, u, v, w, h, outfit) => glow(at(f, u, v, w, h), outfit.c, { f, u, v, w, h, id }) : null;
    this.pieces.push(p);
    return p;
  }

  /**
   * A flat piece lying along direction `angle` (degrees in the x-z plane, a multiple of 22.5), centred `r` out from
   * `centre`: `length` along the direction, `width` across, `thick` high. Turned about y only (a petal, a leaf).
   */
  radial(stages, bone, centre, angle, r, length, width, thick, paint, glow = null) {
    const a = ((angle % 180) + 270) % 180 - 90;  // [-90, 90)
    const rad = angle * Math.PI / 180, c = [centre[0] + Math.cos(rad) * r, centre[1], centre[2] + Math.sin(rad) * r];
    if (Math.abs(a) <= 45) return this.box(stages, bone, [length, thick, width], c, paint, 'y', a === 0 ? 0 : -a, glow);
    return this.box(stages, bone, [width, thick, length], c, paint, 'y', a > 0 ? 90 - a : -90 - a, glow);
  }

  geo(stage) {
    const used = this.pieces.filter(p => p.stages.includes(stage));
    const bones = Object.entries(this.bones[stage]).map(([name, b]) => ({ name, ...(b.parent ? { parent: b.parent } : {}), pivot: b.pivot, cubes: [] }));
    const byName = Object.fromEntries(bones.map(b => [b.name, b]));
    for (const p of used) {
      const uv = Object.fromEntries(kit.FACES.map(f => {
        const [u0, v0, u1, v1] = p.uv[f];
        return [f, { uv: [u0 / this.D, v0 / this.D], uv_size: [(u1 - u0) / this.D, (v1 - v0) / this.D] }];
      }));
      const c = { origin: p.from.map(v => +v.toFixed(4)), size: p.to.map((v, i) => +(v - p.from[i]).toFixed(4)), uv };
      // Bedrock (GeckoLib, Blockbench) turns x and y the other way round from Java and from turn() here.
      if (p.deg) { c.pivot = p.pivot.map(v => +v.toFixed(4)); c.rotation = [p.axis === 'x' ? -p.deg : 0, p.axis === 'y' ? -p.deg : 0, p.axis === 'z' ? p.deg : 0]; }
      if (!byName[p.bone]) throw new Error(`${this.name}: no bone ${p.bone} in stage ${stage}`);
      byName[p.bone].cubes.push(c);
    }
    return {
      format_version: '1.12.0',
      'minecraft:geometry': [{
        description: { identifier: `geometry.${this.name}_stage${stage}`, texture_width: this.atlas / this.D, texture_height: this.atlas / this.D,
          visible_bounds_width: 2, visible_bounds_height: 2, visible_bounds_offset: [0, 0.5, 0] },
        bones,
      }],
    };
  }

  /** Writes the geometries, the textures (one a colour, its glowmask if any part glows) and the animations. */
  write(colours, animations) {
    const faces = kit.pack(this.pieces, this.atlas);
    const rows = Math.max(...faces.map(f => f.v + f.h));
    const dirs = { geo: path.join(ASSETS, 'geo', 'block', 'plant'), tex: path.join(ASSETS, 'textures', 'block', 'plant'), anim: path.join(ASSETS, 'animations', 'block', 'plant') };
    Object.values(dirs).forEach(d => fs.mkdirSync(d, { recursive: true }));
    for (let s = 0; s < this.stages; s++) fs.writeFileSync(path.join(dirs.geo, `${this.name}_stage${s}.geo.json`), JSON.stringify(this.geo(s)) + '\n');
    const written = [];
    for (const outfit of colours) {
      const img = kit.paintAtlas(faces, outfit, this.atlas);
      const file = path.join(dirs.tex, `${this.name}_${outfit.key}.png`);
      fs.writeFileSync(file, kit.png(this.atlas, this.atlas, img));
      written.push(file);
      if (this.pieces.some(p => p.glow)) {
        const glow = new Uint8Array(this.atlas * this.atlas * 4);
        for (const face of faces) {
          if (!face.p.glow) continue;
          for (let v = 0; v < face.h; v++) for (let u = 0; u < face.w; u++) {
            const c = face.p.glow(face.f, u, v, face.w, face.h, outfit);
            if (c) glow.set([...hexRgb(c), 255], ((face.v + v) * this.atlas + face.u + u) * 4);
          }
        }
        fs.writeFileSync(path.join(dirs.tex, `${this.name}_${outfit.key}_glowmask.png`), kit.png(this.atlas, this.atlas, glow));
      }
    }
    fs.writeFileSync(path.join(dirs.anim, `${this.name}.animation.json`), JSON.stringify({ format_version: '1.8.0', animations }) + '\n');
    console.log(`${this.name}: ${this.pieces.length} boxes, ${this.stages} stages, atlas rows ${rows} of ${this.atlas}, ${colours.length} colours`);
    return { faces, written };
  }
}

/** Keys sampled every `step` seconds over `length` of f(t) -> [x, y, z] (GeckoLib's plain keys). */
function sampled(length, step, f) {
  const out = {};
  for (let t = 0; t <= length + 1e-9; t += step) out[(Math.round(t * 100) / 100).toFixed(2)] = f(t).map(v => Math.round(v * 100) / 100);
  return out;
}

module.exports = { Plant, sampled, mix, hex, hexRgb, clamp, turn, ASSETS, ROOT };
