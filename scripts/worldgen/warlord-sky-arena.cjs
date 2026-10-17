// The Bandit Warlord's sky arena, the hollowed meteor (docs/mobs/bandit-warlord.md), as a real structure: vanilla
// Minecraft 1.21.1 blocks in a WorldEdit Sponge v3 schematic, with its stats, its static checks and its plans.
// Same shape as the validated mock-up (scripts/concepts/warlord-sky-arena.js), solid all through.
//
//   node scripts/worldgen/warlord-sky-arena.cjs [--out=art/structures/warlord-sky-arena]
//
// Writes warlord_sky_arena.schem, export-stats.json, verification.json and plans-de-l-arene.png.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const crypto = require('node:crypto');

const ROOT = path.resolve(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const OUT = path.resolve(arg('out') || path.join(ROOT, 'art', 'structures', 'warlord-sky-arena'));
const NAME = 'warlord_sky_arena';

// ---------------------------------------------------------------- the shape (as the mock-up)
const seeded = seed => { let s = seed; return () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff); };
const rnd = seeded(1610);
const hash = (x, y, z) => { let h = (x * 374761393 + y * 668265263 + z * 1274126177) | 0; h = Math.imul(h ^ (h >>> 13), 1274126177); return ((h ^ (h >>> 16)) & 0xffff) / 0x7fff - 1; };
const smooth = t => t * t * (3 - 2 * t);
function noise(x, y, z) {
  const xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z), xf = smooth(x - xi), yf = smooth(y - yi), zf = smooth(z - zi);
  const l = (a, b, t) => a + (b - a) * t, c = (dx, dy, dz) => hash(xi + dx, yi + dy, zi + dz);
  return l(l(l(c(0, 0, 0), c(1, 0, 0), xf), l(c(0, 1, 0), c(1, 1, 0), xf), yf), l(l(c(0, 0, 1), c(1, 0, 1), xf), l(c(0, 1, 1), c(1, 1, 1), xf), yf), zf);
}
const fbm = (x, y, z) => noise(x, y, z) * 0.6 + noise(x * 2.1, y * 2.1, z * 2.1) * 0.3 + noise(x * 4.3, y * 4.3, z * 4.3) * 0.1;

/** A rock 170 blocks across; its top a bowl 40 deep round a flat fighting floor 96 across; its underside a cone. */
const R = 84, TOP = 86, DEPTH = 100, FLAT = 48, BOWL = 72, RISE = 40;
const floorAt = r => (r < FLAT ? 0 : RISE * Math.pow((r - FLAT) / (BOWL - FLAT), 1.5));
function rock(x, y, z) {
  const r = Math.hypot(x, z), n = fbm(x * 0.035, y * 0.035, z * 0.035);
  const body = y >= 0 ? (r / R) ** 2 + (y / TOP) ** 2 <= 1 + 0.3 * n
                      : (r / (R * (1 + 0.16 * noise(x * 0.015 + 3, 0, z * 0.015)))) ** 2 + Math.pow(-y / DEPTH, 0.85) <= 1 + 0.4 * n;
  if (!body) return false;
  // The fighting floor stays flat (no bump): the whole of it is ground to fight on.
  if (r < FLAT - 1) return y <= 0;
  if (r <= BOWL + 4 * n && y > floorAt(r) + 1.2 * noise(x * 0.3, 0, z * 0.3)) return false;
  return true;
}

// ---------------------------------------------------------------- the blocks
const B = {
  basalt: 'minecraft:basalt[axis=y]', blackstone: 'minecraft:blackstone', smooth_basalt: 'minecraft:smooth_basalt',
  scorched: 'minecraft:tuff', obsidian: 'minecraft:obsidian', crying: 'minecraft:crying_obsidian', magma: 'minecraft:magma_block', lava: 'minecraft:lava[level=0]',
  log: 'minecraft:dark_oak_log[axis=y]', gold: 'minecraft:gold_block', purple: 'minecraft:purple_wool',
  pillar: 'minecraft:polished_blackstone', bowl: 'minecraft:waxed_cut_copper', netherrack: 'minecraft:netherrack', fire: 'minecraft:fire[age=0,east=false,north=false,south=false,up=false,west=false]',
  air: 'minecraft:air',
};
const X0 = -R - 22, Y0 = -DEPTH - 16, NX = 2 * (R + 22) + 1, NY = DEPTH + 16 + TOP + 30, NZ = NX;
const grid = new Uint16Array(NX * NY * NZ); // palette index + 1; 0 = air
const palette = [], paletteIndex = new Map();
const pal = name => { if (!paletteIndex.has(name)) { paletteIndex.set(name, palette.length); palette.push(name); } return paletteIndex.get(name); };
pal(B.air);
const idx = (x, y, z) => ((y - Y0) * NZ + (z - X0)) * NX + (x - X0);
const inside = (x, y, z) => x >= X0 && x < X0 + NX && y >= Y0 && y < Y0 + NY && z >= X0 && z < X0 + NZ;
const set = (x, y, z, b) => { if (inside(x, y, z)) grid[idx(x, y, z)] = b ? pal(b) : 0; };
const get = (x, y, z) => (inside(x, y, z) ? grid[idx(x, y, z)] : 0);

// The rock, solid; then its faces dressed.
const solid = new Uint8Array(NX * NY * NZ);
for (let x = X0; x < X0 + NX; x++) for (let z = X0; z < X0 + NZ; z++) {
  if (Math.hypot(x, z) > R * 1.5) continue;
  for (let y = Y0; y < Y0 + NY; y++) if (rock(x, y, z)) solid[idx(x, y, z)] = 1;
}
const isRock = (x, y, z) => inside(x, y, z) && solid[idx(x, y, z)] === 1;
for (let x = X0; x < X0 + NX; x++) for (let z = X0; z < X0 + NZ; z++) for (let y = Y0; y < Y0 + NY; y++) {
  if (!isRock(x, y, z)) continue;
  const open = !isRock(x, y + 1, z) || !isRock(x, y - 1, z) || !isRock(x + 1, y, z) || !isRock(x - 1, y, z) || !isRock(x, y, z + 1) || !isRock(x, y, z - 1);
  const r = Math.hypot(x, z), up = !isRock(x, y + 1, z);
  let b;
  if (!open) b = B.basalt;
  else if (up && r < FLAT + 2 && y <= 1) {
    // The fighting floor: scorched basalt; the scar of his landing in the middle, cracks glowing out from it.
    const a = Math.atan2(z, x), crack = Math.abs(((a / (Math.PI * 2)) * 14 + 0.25 * fbm(x * 0.1, 0, z * 0.1) + 10) % 1 - 0.5);
    if (r < 4) b = B.magma;
    else if (r < 6) b = rnd() < 0.35 ? B.magma : rnd() < 0.2 ? B.crying : B.obsidian;
    else if (r < 11) b = rnd() < 0.5 ? B.obsidian : B.scorched;
    else if (crack > 0.462 && r < FLAT - 4) b = B.magma;
    // A ring of lava round the fighting floor, at the foot of its walls.
    else if (r >= FLAT - 1 && r < FLAT + 1) b = B.magma;
    else b = rnd() < 0.18 ? B.blackstone : rnd() < 0.5 ? B.scorched : B.smooth_basalt;
  } else {
    // Its walls and underside: basalt and blackstone, veined with molten rock.
    const v = Math.abs(noise(x * 0.025 + 7, y * 0.035, z * 0.025));
    b = v < 0.025 && y < TOP * 0.6 ? B.magma : up && y > 4 ? (rnd() < 0.4 ? B.scorched : B.smooth_basalt) : rnd() < 0.5 ? B.blackstone : B.basalt;
  }
  set(x, y, z, b);
}
const surface = (x, z) => { let y = Y0 + NY - 1; while (y > Y0 && !isRock(x, y, z)) y--; return y + 1; };
const bottom = (x, z) => { let y = Y0; while (y < Y0 + NY && !isRock(x, y, z)) y++; return y; };
// Molten drips under it.
for (let i = 0; i < 70; i++) {
  const a = rnd() * Math.PI * 2, r = rnd() * R * 0.7, x = Math.round(Math.cos(a) * r), z = Math.round(Math.sin(a) * r);
  const y = bottom(x, z), len = 3 + Math.floor(rnd() * 10);
  if (y >= 0) continue;
  for (let k = 1; k <= len; k++) set(x, y - k, z, k >= len - 1 ? B.magma : B.blackstone);
}
// Spikes round the rim, a few leaning out.
for (let i = 0; i < 34; i++) {
  const a = (i / 34) * Math.PI * 2 + rnd() * 0.15, r0 = BOWL + 4 + rnd() * 4, h = 5 + Math.floor(rnd() * 12), lean = 0.25 + rnd() * 0.3;
  const base = surface(Math.round(Math.cos(a) * r0), Math.round(Math.sin(a) * r0)) - 1;
  for (let k = 0; k < h; k++) {
    const rr = r0 + k * lean, w = k < h - 3 ? 1 : 0, cx = Math.round(Math.cos(a) * rr), cz = Math.round(Math.sin(a) * rr);
    for (let dx = -w; dx <= w; dx++) for (let dz = -w; dz <= w; dz++) set(cx + dx, base + k, cz + dz, k === h - 1 ? B.scorched : B.blackstone);
  }
}
// A few low broken boulders on the fighting floor (no cover from his blows).
const blob = (cx, cy, cz, rx, ry, rz, b, holes) => {
  for (let x = -rx; x <= rx; x++) for (let y = -ry; y <= ry; y++) for (let z = -rz; z <= rz; z++)
    if (x * x / (rx * rx + 0.5) + y * y / (ry * ry + 0.5) + z * z / (rz * rz + 0.5) <= 1 && rnd() > holes) set(cx + x, cy + y, cz + z, b);
};
const boulders = [];
for (let i = 0; i < 9; i++) {
  const a = rnd() * Math.PI * 2, r = 16 + rnd() * (FLAT - 22), x = Math.round(Math.cos(a) * r), z = Math.round(Math.sin(a) * r);
  blob(x, 1, z, 2 + Math.floor(rnd() * 2), 1, 2, rnd() < 0.5 ? B.blackstone : B.scorched, 0.25);
  boulders.push([x, 1, z]);
}
// Twelve braziers round the fighting floor (eternal fire on netherrack) and six banners on the rim.
const braziers = [], banners = [];
for (let i = 0; i < 12; i++) {
  const a = (i / 12) * Math.PI * 2 + Math.PI / 12, x = Math.round(Math.cos(a) * (FLAT - 2)), z = Math.round(Math.sin(a) * (FLAT - 2));
  const y = surface(x, z);
  set(x, y, z, B.pillar); set(x, y + 1, z, B.pillar); set(x, y + 2, z, B.pillar);
  for (let dx = -1; dx <= 1; dx++) for (let dz = -1; dz <= 1; dz++) set(x + dx, y + 3, z + dz, B.bowl);
  set(x, y + 3, z, B.netherrack); set(x, y + 4, z, B.fire);
  braziers.push([x, y, z]);
}
for (let i = 0; i < 6; i++) {
  const a = (i / 6) * Math.PI * 2, x = Math.round(Math.cos(a) * (BOWL - 2)), z = Math.round(Math.sin(a) * (BOWL - 2));
  const y = surface(x, z), tx = -Math.sin(a), tz = Math.cos(a);
  for (let k = 0; k <= 15; k++) set(x, y + k, z, B.log);
  for (let k = -3; k <= 3; k++) set(x + Math.round(tx * k), y + 15, z + Math.round(tz * k), B.gold);
  for (let k = -2; k <= 2; k++) for (let d = 1; d <= 10; d++) {
    const bx = x + Math.round(tx * k), bz = z + Math.round(tz * k);
    if (d < 10) set(bx, y + 15 - d, bz, B.purple); else if (k === 0) set(bx, y + 15 - d, bz, B.gold);
  }
  banners.push([x, y, z]);
}
// Rocks drifting round it, torn off by the fall (kept near, within the schematic).
for (let i = 0; i < 20; i++) {
  const a = rnd() * Math.PI * 2, r = R + 6 + rnd() * 12, y = -40 + rnd() * 70, s = 1 + Math.floor(rnd() * 3);
  for (let k = 0; k < 3; k++) blob(Math.round(rnd() * 4 - 2 + Math.cos(a) * r), Math.round(y + rnd() * 3 - 1.5), Math.round(rnd() * 4 - 2 + Math.sin(a) * r), s, Math.max(1, s - 1), s, rnd() < 0.5 ? B.basalt : B.scorched, 0.3);
}

// Real lava (Enzo: "put lava for the cracks and all"): every molten block that cannot flow becomes a lava source, that is
// with no air beside it or under it: the floor's cracks and the scar, the veins on upward faces. Those open to the side
// or below (walls, underside, drips) stay magma blocks, or the lava would run down the meteor into the sky.
const lavaIndex = pal(B.lava), magmaIndex = pal(B.magma);
const toLava = [];
for (let y = Y0 + 1; y < Y0 + NY - 1; y++) for (let z = X0 + 1; z < X0 + NZ - 1; z++) for (let x = X0 + 1; x < X0 + NX - 1; x++) {
  if (grid[idx(x, y, z)] !== magmaIndex) continue;
  if (get(x, y - 1, z) && get(x + 1, y, z) && get(x - 1, y, z) && get(x, y, z + 1) && get(x, y, z - 1)) toLava.push(idx(x, y, z));
}
for (const i of toLava) grid[i] = lavaIndex;

// ---------------------------------------------------------------- trimmed to what is built
let mn = [Infinity, Infinity, Infinity], mx = [-Infinity, -Infinity, -Infinity];
for (let y = Y0; y < Y0 + NY; y++) for (let z = X0; z < X0 + NZ; z++) for (let x = X0; x < X0 + NX; x++)
  if (grid[idx(x, y, z)]) { mn = [Math.min(mn[0], x), Math.min(mn[1], y), Math.min(mn[2], z)]; mx = [Math.max(mx[0], x), Math.max(mx[1], y), Math.max(mx[2], z)]; }
const W = mx[0] - mn[0] + 1, H = mx[1] - mn[1] + 1, L = mx[2] - mn[2] + 1;

// ---------------------------------------------------------------- Sponge v3 schematic (NBT, gzip)
function nbt() {
  const parts = [];
  const u8 = v => parts.push(Buffer.from([v & 255]));
  const i16 = v => { const b = Buffer.alloc(2); b.writeInt16BE(v); parts.push(b); };
  const i32 = v => { const b = Buffer.alloc(4); b.writeInt32BE(v); parts.push(b); };
  const str = s => { const b = Buffer.from(s, 'utf8'); i16(b.length); parts.push(b); };
  const tag = (type, name) => { u8(type); str(name); };
  return { parts, u8, i16, i32, str, tag, end: () => u8(0), bytes: () => Buffer.concat(parts) };
}
const varints = [];
for (let y = mn[1]; y <= mx[1]; y++) for (let z = mn[2]; z <= mx[2]; z++) for (let x = mn[0]; x <= mx[0]; x++) {
  let v = grid[idx(x, y, z)];
  while (v >= 0x80) { varints.push((v & 0x7f) | 0x80); v >>>= 7; }
  varints.push(v);
}
const o = nbt();
o.tag(10, '');
o.tag(10, 'Schematic');
o.tag(3, 'Version'); o.i32(3);
o.tag(3, 'DataVersion'); o.i32(3955);
o.tag(2, 'Width'); o.i16(W); o.tag(2, 'Height'); o.i16(H); o.tag(2, 'Length'); o.i16(L);
// The paste point is the middle of the fighting floor, on the floor.
o.tag(11, 'Offset'); o.i32(3); o.i32(mn[0]); o.i32(mn[1] - 1); o.i32(mn[2]);
o.tag(10, 'Metadata'); o.tag(8, 'Name'); o.str('Bandit Warlord sky arena'); o.tag(8, 'Author'); o.str('Murimblock'); o.end();
o.tag(10, 'Blocks');
o.tag(10, 'Palette'); palette.forEach((name, i) => { o.tag(3, name); o.i32(i); }); o.end();
o.tag(7, 'Data'); o.i32(varints.length); o.parts.push(Buffer.from(varints));
o.tag(9, 'BlockEntities'); o.u8(10); o.i32(0);
o.end();
o.end();
o.end();
const schem = zlib.gzipSync(o.bytes());
fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, `${NAME}.schem`), schem);

// ---------------------------------------------------------------- stats and static checks
const counts = {};
let nonAir = 0;
for (let y = mn[1]; y <= mx[1]; y++) for (let z = mn[2]; z <= mx[2]; z++) for (let x = mn[0]; x <= mx[0]; x++) {
  const v = grid[idx(x, y, z)]; if (!v) continue; nonAir++; counts[palette[v]] = (counts[palette[v]] || 0) + 1;
}
// Lava that could flow: open to the air beside or below (must be none).
let lavaCount = 0, lavaLeaks = 0;
for (let y = mn[1]; y <= mx[1]; y++) for (let z = mn[2]; z <= mx[2]; z++) for (let x = mn[0]; x <= mx[0]; x++) {
  if (grid[idx(x, y, z)] !== lavaIndex) continue;
  lavaCount++;
  if (!get(x, y - 1, z) || !get(x + 1, y, z) || !get(x - 1, y, z) || !get(x, y, z + 1) || !get(x, y, z - 1)) lavaLeaks++;
}
// The fighting floor: columns within FLAT - 1 whose top is the floor (y 0), with 30 blocks of air above (bar braziers and boulders).
let floorColumns = 0, clearColumns = 0, highestFloor = -Infinity, lowestFloor = Infinity;
for (let x = -FLAT; x <= FLAT; x++) for (let z = -FLAT; z <= FLAT; z++) {
  if (Math.hypot(x, z) >= FLAT - 1) continue;
  floorColumns++;
  let top = 30; while (top > -10 && !get(x, top, z)) top--;
  highestFloor = Math.max(highestFloor, top); lowestFloor = Math.min(lowestFloor, top);
  let clear = true; for (let y = 1; y <= 30; y++) if (get(x, y, z)) { clear = false; break; }
  if (get(x, 0, z) && clear) clearColumns++;
}
const rimHeights = [];
for (let i = 0; i < 72; i++) { const a = (i / 72) * Math.PI * 2; rimHeights.push(surface(Math.round(Math.cos(a) * BOWL), Math.round(Math.sin(a) * BOWL))); }
const sha256 = crypto.createHash('sha256').update(schem).digest('hex');
const stats = {
  file: `${NAME}.schem`, format: 'Sponge v3', minecraft: '1.21.1', data_version: 3955,
  size: [W, H, L], minimum: mn, maximum: mx, paste_point: 'middle of the fighting floor, standing on it (0, 1, 0)',
  non_air: nonAir, air: W * H * L - nonAir, palette_states: palette.length, blocks: counts, bytes: schem.length, sha256,
  targets: { 'Fighting floor middle (the scar)': [0, 1, 0], 'Braziers': braziers.map(([x, y, z]) => [x, y, z]), 'Banners': banners.map(([x, y, z]) => [x, y, z]) },
};
const verification = {
  format: 'Sponge v3', data_version: 3955, size: [W, H, L], decoded_blocks: W * H * L, palette_states: palette.length, block_entities: 0, entities: 0,
  fighting_floor: { radius: FLAT - 1, diameter: 2 * (FLAT - 1), columns: floorColumns, clear_columns_30_high: clearColumns, floor_y_range: [lowestFloor, highestFloor] },
  bowl: { rim_radius: BOWL, rim_height_above_floor: [Math.min(...rimHeights), Math.max(...rimHeights)] },
  vanilla_only: palette.every(p => p.startsWith('minecraft:')),
  lava: { sources: lavaCount, contained: lavaLeaks === 0, open_beside_or_below: lavaLeaks },
  braziers: braziers.length, banners: banners.length, boulders_on_floor: boulders.length,
};
fs.writeFileSync(path.join(OUT, 'export-stats.json'), JSON.stringify(stats, null, 2) + '\n');
fs.writeFileSync(path.join(OUT, 'verification.json'), JSON.stringify(verification, null, 2) + '\n');

// ---------------------------------------------------------------- plans from its blocks: from above, and a cut through the middle
const COLOUR = {
  [B.basalt]: [74, 70, 72], [B.blackstone]: [46, 42, 46], [B.smooth_basalt]: [72, 70, 74], [B.scorched]: [92, 90, 84], [B.obsidian]: [30, 24, 44],
  [B.crying]: [70, 30, 120], [B.magma]: [240, 106, 40], [B.lava]: [255, 130, 20], [B.log]: [64, 46, 30], [B.gold]: [226, 185, 74], [B.purple]: [94, 42, 110],
  [B.pillar]: [56, 52, 60], [B.bowl]: [190, 110, 80], [B.netherrack]: [120, 40, 40], [B.fire]: [255, 200, 60],
};
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 3 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 3 + 1)] = 0; for (let x = 0; x < w; x++) { const [r, g, b] = pixel(x, y); const o = y * (w * 3 + 1) + 1 + x * 3; raw[o] = r; raw[o + 1] = g; raw[o + 2] = b; } }
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td)); return Buffer.concat([len, td, crc]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 2;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const crcTable = Array.from({ length: 256 }, (_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
function crc32(buf) { let c = 0xffffffff; for (const b of buf) c = crcTable[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }
const S = 4, GAP = 24, SKY = [159, 184, 220];
const topW = W, topH = L, cutH = H, PW = topW * S, PH = (topH + cutH) * S + GAP;
const plan = png(PW, PH, (px, py) => {
  if (py < topH * S) {
    // From above: the highest block of each column, shaded by its height.
    const x = mn[0] + Math.floor(px / S), z = mn[2] + Math.floor(py / S);
    for (let y = mx[1]; y >= mn[1]; y--) { const v = grid[idx(x, y, z)]; if (v) { const c = COLOUR[palette[v]] || [128, 128, 128]; const f = 0.7 + 0.3 * (y - mn[1]) / H; return c.map(k => Math.min(255, Math.round(k * f))); } }
    return SKY;
  }
  if (py < topH * S + GAP) return [235, 230, 220];
  // The cut along x through the middle (z = 0), the floor's middle at its centre.
  const x = mn[0] + Math.floor(px / S), y = mx[1] - Math.floor((py - topH * S - GAP) / S);
  const v = grid[idx(x, y, 0)];
  return v ? (COLOUR[palette[v]] || [128, 128, 128]) : y === 0 ? [200, 60, 50] : SKY;
});
fs.writeFileSync(path.join(OUT, 'plans-de-l-arene.png'), plan);
console.log(`${NAME}.schem ${W}x${H}x${L}, ${nonAir} blocks, ${palette.length} states, ${schem.length} bytes`);
console.log(JSON.stringify(verification.fighting_floor), JSON.stringify(verification.bowl));
