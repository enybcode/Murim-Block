// The Seven Stars Sword's light (client/SevenStarsFx): textures drawn additively (bright on black, so black adds
// nothing), white at the heart and pale blue at the edges, as the art's silver and night blue:
//   textures/fx/seven_stars/star.png    a four-pointed star: a bright core, four thin rays, a soft halo (32 x 32);
//   textures/fx/seven_stars/dot.png     a soft round glow (16 x 16): a star not lit yet, a flash;
//   textures/fx/seven_stars/beam.png    a line of light across the texture's height, soft on its edges (16 x 16);
//   textures/fx/seven_stars/ring.png    a thin bright ring (64 x 64), for the burst's wave;
//   textures/particle/star_dust.png     a tiny sparkle (8 x 8), the star dust particle.
// Usage: node scripts/animation/seven-stars-fx-textures.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock', 'textures');

/** An image of n x n from a brightness function (0..1) at the pixel's centre (u, v in -1..1), tinted white to blue. */
function image(n, f) {
  const px = new Uint8Array(n * n * 4);
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    const u = ((x + 0.5) / n) * 2 - 1, v = ((y + 0.5) / n) * 2 - 1;
    const b = Math.max(0, Math.min(1, f(u, v)));
    // The brightest is white; the faint parts lean to pale blue.
    const r = 255 * b * (0.72 + 0.28 * b), g = 255 * b * (0.86 + 0.14 * b), bl = 255 * b;
    px.set([Math.round(r), Math.round(g), Math.round(bl), Math.round(255 * b)], (y * n + x) * 4);
  }
  return kit.png(n, n, px);
}

const star = (u, v) => {
  const r = Math.hypot(u, v);
  const core = Math.exp(-r * r * 60);
  const rays = Math.max(Math.exp(-u * u * 900) * (1 - Math.abs(v)), Math.exp(-v * v * 900) * (1 - Math.abs(u)));
  const halo = Math.exp(-r * r * 6) * 0.35;
  return core + rays * 0.9 + halo;
};
const dot = (u, v) => Math.exp(-(u * u + v * v) * 4.5);
const beam = (u, v) => Math.exp(-v * v * 14);
const ring = (u, v) => { const r = Math.hypot(u, v); return Math.exp(-((r - 0.86) ** 2) * 900) + Math.exp(-((r - 0.86) ** 2) * 60) * 0.25; };
const dust = (u, v) => Math.max(Math.exp(-(u * u + v * v) * 10), Math.exp(-u * u * 60) * (1 - Math.abs(v)) * 0.8, Math.exp(-v * v * 60) * (1 - Math.abs(u)) * 0.8);

const dir = path.join(ASSETS, 'fx', 'seven_stars');
fs.mkdirSync(dir, { recursive: true });
fs.writeFileSync(path.join(dir, 'star.png'), image(32, star));
fs.writeFileSync(path.join(dir, 'dot.png'), image(16, dot));
fs.writeFileSync(path.join(dir, 'beam.png'), image(16, beam));
fs.writeFileSync(path.join(dir, 'ring.png'), image(64, ring));
// The particle is drawn translucent, not added: its alpha is its shape, its colour white.
const solid = (n, f) => { const px = new Uint8Array(n * n * 4); for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) { const b = Math.max(0, Math.min(1, f(((x + 0.5) / n) * 2 - 1, ((y + 0.5) / n) * 2 - 1))); px.set([255, 255, 255, Math.round(255 * Math.sqrt(b))], (y * n + x) * 4); } return kit.png(n, n, px); };
fs.writeFileSync(path.join(ASSETS, 'particle', 'star_dust.png'), solid(8, dust));
console.log('wrote the Seven Stars Sword\'s light textures');

// ---- The techniques' (seven-stars-sword.md): the seals on the ground and the swords of light.
/** An image of w x h from a brightness function of the pixel (x, y in pixels), tinted as the others. */
function imageWH(w, h, f) {
  const px = new Uint8Array(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const b = Math.max(0, Math.min(1, f(x + 0.5, y + 0.5)));
    px.set([Math.round(255 * b * (0.72 + 0.28 * b)), Math.round(255 * b * (0.86 + 0.14 * b)), Math.round(255 * b), Math.round(255 * b)], (y * w + x) * 4);
  }
  return kit.png(w, h, px);
}
const DIPPER = [[-2.2, 0.1], [-1.2, 0.35], [-0.4, 0.4], [0.4, 0.4], [1.9, 1.0], [2.0, -0.1], [0.8, -0.6]];
const LINES = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 3]];
const segDist = (px, py, ax, ay, bx, by) => { const dx = bx - ax, dy = by - ay, l = dx * dx + dy * dy, t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / l)); return Math.hypot(px - ax - t * dx, py - ay - t * dy); };
/** The Big Dipper's seal (128): an outer ring, its ticks, an inner ring, the Dipper drawn inside with its stars. */
const SEAL = 128;
const sealDipper = (x, y) => {
  const c = SEAL / 2, dx = x - c, dy = y - c, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
  let b = 0;
  b = Math.max(b, Math.exp(-((r - 60) ** 2) / 1.2));
  b = Math.max(b, 0.55 * Math.exp(-((r - 53) ** 2) / 0.8));
  if (r > 53 && r < 60) { const tick = Math.abs(((a / (Math.PI * 2) * 49) % 1 + 1) % 1 - 0.5); b = Math.max(b, tick < 0.08 ? 0.5 : 0); }
  b = Math.max(b, 0.25 * Math.exp(-((r - 46) ** 2) / 0.6));
  const s = 17, sx = (u) => c + u * s, sy = (v) => c - v * s;
  for (const [i, j] of LINES) b = Math.max(b, 0.55 * Math.exp(-(segDist(x, y, sx(DIPPER[i][0]), sy(DIPPER[i][1]), sx(DIPPER[j][0]), sy(DIPPER[j][1])) ** 2) / 0.5));
  for (const [u, v] of DIPPER) {
    const ex = x - sx(u), ey = y - sy(v), d = Math.hypot(ex, ey);
    b = Math.max(b, Math.exp(-d * d / 3), Math.exp(-ex * ex / 0.4) * Math.max(0, 1 - Math.abs(ey) / 6), Math.exp(-ey * ey / 0.4) * Math.max(0, 1 - Math.abs(ex) / 6));
  }
  return b;
};
/** A star's sigil (64): a ring and a four-pointed star in it. */
const sealStar = (x, y) => {
  const c = 32, dx = x - c, dy = y - c, r = Math.hypot(dx, dy);
  const ring = Math.exp(-((r - 28) ** 2) / 1.0) + 0.4 * Math.exp(-((r - 23) ** 2) / 0.6);
  const star = Math.max(Math.exp(-dx * dx / 1.2) * Math.max(0, 1 - Math.abs(dy) / 20), Math.exp(-dy * dy / 1.2) * Math.max(0, 1 - Math.abs(dx) / 20)) + Math.exp(-r * r / 12);
  return Math.max(ring, star * 0.9);
};
/** A sword of light (32 x 128), point up: a bright core down the blade, a soft glow, a guard, a short grip. */
const sword = (x, y) => {
  const c = 16, dx = Math.abs(x - c);
  if (y < 8) { const w = (y / 8) * 3; return Math.exp(-(dx * dx) / (0.5 + w)) * (dx < w + 1 ? 1 : 0.6); }
  if (y < 100) return Math.exp(-dx * dx / 2.2) + 0.35 * Math.exp(-dx * dx / 18);
  if (y < 104) return dx < 11 ? 0.9 - dx * 0.03 : 0;
  return dx < 2 ? 0.7 : 0.15 * Math.exp(-dx * dx / 8);
};
fs.writeFileSync(path.join(dir, 'seal_dipper.png'), imageWH(SEAL, SEAL, sealDipper));
fs.writeFileSync(path.join(dir, 'seal_star.png'), imageWH(64, 64, sealStar));
fs.writeFileSync(path.join(dir, 'light_sword.png'), imageWH(32, 128, sword));
console.log('and the seals and the sword of light');
