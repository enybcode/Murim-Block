// The Purple Heaven Thunder Saber's light (client/ThunderSaberFx). Drawn additively (bright on black, black adds
// nothing): white at the heart, the purple lightning at the edges (docs/combat/styles/purple-heaven-thunder-saber.md:
// violet, white and gold). The storm cloud is the one texture drawn translucent, not added.
//   textures/fx/thunder/beam.png      a line of light along the texture, soft on its edges (16 x 16): bolts, arcs, waves;
//   textures/fx/thunder/glow.png      a soft round glow (32 x 32): flashes, the hearts of impacts;
//   textures/fx/thunder/ring.png      a thin bright ring (64 x 64): the shockwaves, the drum;
//   textures/fx/thunder/shock.png     a ring of cracks on the ground, broken and uneven (128 x 128): where a blow lands;
//   textures/fx/thunder/seal.png      the thunder seal (128 x 128): a double ring of square spirals (雷纹) round a bolt;
//   textures/fx/thunder/crescent.png  a crescent, bright on its outer edge (64 x 64): Severing Flash's wave;
//   textures/fx/thunder/cloud.png     a dark storm puff, soft edged (64 x 64, translucent);
//   textures/particle/thunder_spark.png  a small spark (8 x 8), the thunder spark particle.
// Usage: node scripts/animation/thunder-saber-fx-textures.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'textures');

/** n x m from a brightness function (0..1) at the pixel's centre (u, v in -1..1), tinted white to violet. */
function image(n, f, m = n) {
  const px = new Uint8Array(n * m * 4);
  for (let y = 0; y < m; y++) for (let x = 0; x < n; x++) {
    const u = ((x + 0.5) / n) * 2 - 1, v = ((y + 0.5) / m) * 2 - 1;
    const b = Math.max(0, Math.min(1, f(u, v, x, y)));
    // The brightest is white; the faint parts lean to the purple of the lightning.
    const r = 255 * b * (0.7 + 0.3 * b), g = 255 * b * (0.52 + 0.48 * b * b), bl = 255 * b;
    px.set([Math.round(r), Math.round(g), Math.round(bl), Math.round(255 * b)], (y * n + x) * 4);
  }
  return kit.png(n, m, px);
}

// A little value noise that tiles.
const hash = (x, y) => { const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453; return s - Math.floor(s); };
function noise(x, y, period) {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
  const h = (a, b) => hash(((a % period) + period) % period, ((b % period) + period) % period);
  const s = t => t * t * (3 - 2 * t);
  const a = h(xi, yi), b = h(xi + 1, yi), c = h(xi, yi + 1), d = h(xi + 1, yi + 1);
  return a + (b - a) * s(xf) + (c - a) * s(yf) + (a - b - c + d) * s(xf) * s(yf);
}

const beam = (u, v) => Math.exp(-v * v * 16) + Math.exp(-v * v * 3) * 0.25;
const glow = (u, v) => Math.exp(-(u * u + v * v) * 4.2);
const ring = (u, v) => { const r = Math.hypot(u, v); return Math.exp(-((r - 0.86) ** 2) * 800) + Math.exp(-((r - 0.86) ** 2) * 50) * 0.3; };
/** Cracks: a ring broken by noise, and lines running out from the middle. */
const shock = (u, v) => {
  const r = Math.hypot(u, v), a = Math.atan2(v, u);
  const band = Math.exp(-((r - 0.8) ** 2) * 160) * (0.4 + 0.6 * noise(a * 8 + 20, r * 3, 1000));
  let rays = 0;
  for (let k = 0; k < 11; k++) {
    const ak = k / 11 * Math.PI * 2 + hash(k, 3) * 0.4;
    const wob = Math.sin(r * 14 + k) * 0.05;
    const d = Math.abs(Math.sin(a - ak - wob)) * r;
    rays = Math.max(rays, Math.exp(-d * d * 1400) * (r < 0.85 ? 1 - r * 0.6 : 0) * (r > 0.12 ? 1 : 0));
  }
  return band + rays * 0.9;
};
/** The thunder seal: rings, square spirals between them, a bolt in the middle. */
function seal(u, v) {
  const r = Math.hypot(u, v), a = Math.atan2(v, u);
  const line = (d, w) => Math.exp(-d * d / (w * w));
  let b = line(r - 0.95, 0.018) + line(r - 0.88, 0.01) * 0.8 + line(r - 0.58, 0.012) + line(r - 0.52, 0.008) * 0.7;
  // Sixteen square spirals in the band between 0.6 and 0.86.
  if (r > 0.6 && r < 0.86) {
    const N = 16, k = Math.floor((a + Math.PI) / (Math.PI * 2) * N), mid = (k + 0.5) / N * Math.PI * 2 - Math.PI;
    // The cell's own frame: across (s) and out (t), both -1..1.
    const s = Math.sin(a - mid) * r / (Math.PI * 2 * 0.73 / N / 2) * 0.9, t = (r - 0.73) / 0.12;
    const x = s, y = t, w = 0.16;
    const seg = (x0, y0, x1, y1) => {
      const dx = x1 - x0, dy = y1 - y0, l2 = dx * dx + dy * dy;
      const k2 = Math.max(0, Math.min(1, ((x - x0) * dx + (y - y0) * dy) / l2));
      const px = x0 + dx * k2 - x, py = y0 + dy * k2 - y;
      return Math.exp(-(px * px + py * py) / (w * w));
    };
    b += Math.max(seg(-0.75, 0.75, -0.75, -0.75), seg(-0.75, -0.75, 0.75, -0.75), seg(0.75, -0.75, 0.75, 0.6),
        seg(0.75, 0.6, -0.4, 0.6), seg(-0.4, 0.6, -0.4, -0.35), seg(-0.4, -0.35, 0.35, -0.35), seg(0.35, -0.35, 0.35, 0.2));
  }
  // The bolt in the middle.
  const P = [[0.14, -0.42], [-0.12, 0.02], [0.13, 0.02], [-0.1, 0.44]];
  for (let i = 0; i + 1 < P.length; i++) {
    const [x0, y0] = P[i], [x1, y1] = P[i + 1], dx = x1 - x0, dy = y1 - y0, l2 = dx * dx + dy * dy;
    const k = Math.max(0, Math.min(1, ((u - x0) * dx + (v - y0) * dy) / l2));
    const px = x0 + dx * k - u, py = y0 + dy * k - v;
    b += Math.exp(-(px * px + py * py) / 0.0016);
  }
  return b;
}
/** A crescent: the outer arc bright, the inner one soft, its horns thinning. */
const crescent = (u, v) => {
  const d1 = Math.hypot(u, v - 0.45), d2 = Math.hypot(u, v - 0.78);
  const edge = Math.exp(-((d1 - 0.97) ** 2) * 500);
  const body = d1 < 0.98 && d2 > 0.98 ? 0.55 : 0;
  return (edge + body) * Math.max(0, 1 - Math.abs(u) * 1.02);
};
const spark = (u, v) => Math.max(Math.exp(-(u * u + v * v) * 9), Math.exp(-u * u * 50) * (1 - Math.abs(v)) * 0.8, Math.exp(-v * v * 50) * (1 - Math.abs(u)) * 0.8);

/** The storm puff: dark violet-grey, its alpha a soft round blot broken by noise. */
function cloud(n) {
  const px = new Uint8Array(n * n * 4);
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    const u = ((x + 0.5) / n) * 2 - 1, v = ((y + 0.5) / n) * 2 - 1, r = Math.hypot(u, v);
    const nz = 0.55 * noise(x / 8, y / 8, n / 8) + 0.3 * noise(x / 4, y / 4, n / 4) + 0.15 * noise(x / 2, y / 2, n / 2);
    const a = Math.max(0, Math.min(1, (1 - r * 1.05) * 1.8 * (0.55 + 0.7 * nz)));
    const shade = 0.75 + 0.35 * nz;
    px.set([Math.round(46 * shade), Math.round(38 * shade), Math.round(66 * shade), Math.round(255 * a)], (y * n + x) * 4);
  }
  return kit.png(n, n, px);
}

const out = (rel, data) => {
  const f = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, data);
  console.log('wrote', path.relative(ROOT, f));
};
out('fx/thunder/beam.png', image(16, beam));
out('fx/thunder/glow.png', image(32, glow));
out('fx/thunder/ring.png', image(64, ring));
out('fx/thunder/shock.png', image(128, shock));
out('fx/thunder/seal.png', image(128, seal));
out('fx/thunder/crescent.png', image(64, crescent));
out('fx/thunder/cloud.png', cloud(64));
out('particle/thunder_spark.png', image(8, spark));
