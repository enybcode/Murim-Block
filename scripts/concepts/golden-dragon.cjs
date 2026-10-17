// The Golden Dragon, concept (16/10/2026): a legendary unique mob, a god roaming the Murim. Chosen by Enzo after
// several passes: a tall S, a massive head of stacked plates, big ivory antlers, a thick mane, long thin whiskers, fish
// scales painted in the texture (64 x 64 tiles), in white gold and jade ("or blanc et jade"). Its own design, built
// the way of the chunky dragons Enzo showed (not a copy of any). Bedrock geo + a 512 x 512 atlas; not in the mod yet.
//   node scripts/concepts/golden-dragon.cjs <out dir>
const fs = require('node:fs'), path = require('node:path'), zlib = require('node:zlib');
const out = process.argv[2]; fs.mkdirSync(out, { recursive: true });
function png(w, h, pixel) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y), o = y * (w * 4 + 1) + 1 + x * 4;
    raw[o] = (c >>> 16) & 255; raw[o + 1] = (c >>> 8) & 255; raw[o + 2] = c & 255; raw[o + 3] = 255; }
  const crc = (buf) => { let c = ~0; for (const b of buf) { c ^= b; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hash = (x, y, s) => { let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967295; };
const mix = (a, b, t) => { t = Math.max(0, Math.min(1, t)); const ch = (s) => Math.round(((a >>> s) & 255) * (1 - t) + ((b >>> s) & 255) * t); return (ch(16) << 16) | (ch(8) << 8) | ch(0); };

// ---------------------------------------------------------------- the painted tiles (32 x 32)
const T = 64;
// Gold: deep, warm, a highlight only on each scale's crown, dark seams; not a flat bright yellow.
const G_HI = 0xffe9a0, G = 0xe8b440, G_MID = 0xc98e22, G_DK = 0x8a5612, G_SEAM = 0x4a2a08;
const B_HI = 0xfff0cc, B = 0xead2a0, B_DK = 0xb8955a, B_SEAM = 0x6a4a20;
const V_DK = 0x0a3a2a, V = 0x1a6a4a, V_MID = 0x2ea070, V_HI = 0x8ae0b8;
const H_HI = 0xfff8e0, H = 0xe8d8a0, H_DK = 0xb09a60, H_SEAM = 0x7a6a40; // horns: ivory, as the reference's

/** Gold fish scales of width w (a tile is 64 wide): the scale under a texel, and where in it. */
function fishScale(x, y, w, dark) {
  const h = w * 0.72, R = w * 0.62;
  let best = null;
  const row0 = Math.floor(y / h);
  for (let row = row0 - 1; row <= row0 + 1 && !best; row++) {
    const off = (((row % 2) + 2) % 2) * w / 2;
    const col = Math.floor((x - off) / w);
    for (let cc = col - 1; cc <= col + 1; cc++) {
      const cx = cc * w + off + w / 2, cy = row * h;
      const dd = Math.hypot(x + 0.5 - cx, y + 0.5 - cy) / R;
      if (dd <= 1 && y + 0.5 >= cy - R * 0.2) { best = { dd, dy: (y + 0.5 - cy) / R, dx: (x + 0.5 - cx) / R, row, cc }; break; }
    }
  }
  if (!best) return 0x5a4e3a;
  const { dd, dy, dx } = best;
  const SPEC = 0xffffff, LIGHT = 0xfaf4e0, MID = 0xe2d6b4, DEEP = 0xa8987a, RIM = 0x5a4e3a;
  if (dd > 0.9) return RIM;                                                          // the fine dark outline
  let c = mix(LIGHT, MID, Math.max(0, dy) * 0.9 + Math.abs(dx) * 0.4);              // lit at the middle
  c = mix(c, DEEP, Math.max(0, dd - 0.55) * 1.8);                                   // darker to the rim
  if (Math.hypot(dx + 0.15, dy - 0.15) < 0.22) c = mix(c, SPEC, 0.7);               // the shine
  if (dark) c = mix(c, 0x5a4e3a, 0.22);
  return c;
}
const TILES = {
  scales: (x, y) => fishScale(x, y, 16, false),
  scalesDark: (x, y) => fishScale(x, y, 16, true),
  belly: (x, y) => {
    const py = y % 16, e = Math.min(x, 63 - x) / 10;
    let c = mix(0xf4fff8, 0xc8e8d4, py / 13);
    if (py >= 13) c = mix(0x7aa88c, 0x3a6a50, (py - 13) / 2);
    c = mix(c, 0x7aa88c, Math.max(0, 1 - e) * 0.6);
    return c;
  },
  plate: (x, y) => fishScale(x, y, 21.33, false),
  horn: (x, y) => { let c = mix(H_HI, H, Math.abs(x - 20) / 44 + hash(x >> 3, y >> 3, 5) * 0.05); if (y % 16 === 0) c = mix(c, H_DK, 0.5); if (x > 56) c = mix(c, H_DK, 0.5); return c; },
  mane: (x, y) => { // flowing locks: strands along the tile, each lit along a crest, darker between
    const strand = Math.floor(y / 5), sy = (y % 5) / 4, wave = Math.sin(x / 9 + strand * 1.7) * 0.6;
    const lit = 1 - Math.abs(sy - 0.45 - wave * 0.2) * 1.6;
    let c = mix(V_DK, V_MID, Math.max(0, lit));
    if (lit > 0.75 && (x + strand * 7) % 13 < 7) c = mix(c, V_HI, 0.7);
    return mix(c, V_DK, hash(strand, x >> 3, 6) * 0.2);
  },
  flame: (x, y) => mix(mix(V_HI, V_MID, y / 64), V, hash(x >> 2, y >> 2, 7) * 0.25),
  eye: (x, y) => { const d = Math.hypot(x - 15.5, y - 15.5) / 16; if (Math.abs(x - 15.5) < 2.2 && d < 0.8) return 0x1a0420; return mix(0xfff2ff, 0xb070ff, d * 1.3); },
  mouth: (x, y) => mix(0x5a0c14, 0x2a0408, y / 32 + hash(x, y, 8) * 0.2),
  tongue: (x, y) => mix(0xd23a4a, 0x8a1a24, Math.abs(x - 15.5) / 16 + hash(x, y, 9) * 0.1),
  tooth: (x, y) => mix(0xfffaf0, 0xd8ccb0, y / 32),
  claw: (x, y) => mix(0x3a2a20, 0x14100c, y / 32 + hash(x, y, 10) * 0.2),
  whisker: (x, y) => mix(0x1a1020, 0x2a1838, hash(x, y, 11)),
  pearl: (x, y) => mix(0xffffff, 0xd8b8ff, Math.hypot(x - 15.5, y - 15.5) / 20),
  nose: (x, y) => { let c = TILES.plate(x, y); if (Math.hypot(x - 9, y - 16) < 4 || Math.hypot(x - 22, y - 16) < 4) c = 0x2a0a08; return c; },
};
const NAMES = Object.keys(TILES);
const NATIVE = new Set(['scales', 'scalesDark', 'belly', 'plate', 'mane', 'flame', 'horn']);
const atlas = png(512, 512, (x, y) => { const i = Math.floor(y / T) * 8 + Math.floor(x / T); const n = NAMES[i]; if (!n) return 0;
  const lx = x % T, ly = y % T; return NATIVE.has(n) ? TILES[n](lx, ly) : TILES[n](lx >> 1, ly >> 1); });
const tileUV = (name) => { const i = NAMES.indexOf(name); return [(i % 8) * T, Math.floor(i / 8) * T]; };

// ---------------------------------------------------------------- geometry helpers
const cubes = {};
/** A box; each face a whole tile (top and sides may differ: tiles = name or {side, top, bottom}). */
function cube(bone, center, size, tiles, rotation) {
  const t = typeof tiles === 'string' ? { side: tiles, top: tiles, bottom: tiles } : tiles;
  const face = (n) => ({ uv: tileUV(n), uv_size: [T, T] });
  const [w, h, d] = size;
  const c = { origin: [center[0] - w / 2, center[1] - h / 2, center[2] - d / 2], size,
    uv: { north: face(t.front || t.side), south: face(t.side), east: face(t.side), west: face(t.side), up: face(t.top), down: face(t.bottom) } };
  if (rotation) { c.rotation = rotation; c.pivot = center; }
  (cubes[bone] = cubes[bone] || []).push(c);
}
const deg = (r) => r * 180 / Math.PI;
/** The rotation that lays a box's z along d (checked on a test model: Blockbench shows Bedrock geometry mirrored in x). */
function along(d) { const n = Math.hypot(...d); return [deg(Math.asin(d[1] / n)), deg(Math.atan2(d[0], d[2])), 0]; }
function rod(bone, a, b, w, h, tiles, extra = 2) { const d = [b[0] - a[0], b[1] - a[1], b[2] - a[2]]; cube(bone, [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2], [w, h, Math.hypot(...d) + extra], tiles, along(d)); }
const chain = (bone, pts, w, h, tiles, taper = 1) => { for (let i = 0; i + 1 < pts.length; i++) { const k = 1 - (1 - taper) * i / (pts.length - 1); rod(bone, pts[i], pts[i + 1], w * k, h * k, tiles); } };
const add = (p, q) => [p[0] + q[0], p[1] + q[1], p[2] + q[2]];

// ---------------------------------------------------------------- the dragon: rearing, the head high facing ahead (+z),
// the body coiling down and round behind it, the tail curling up at the side.
const MANE = { side: 'mane', top: 'flame', bottom: 'mane' };
// ---------------------------------------------------------------- the dragon, after Enzo's golden long: a tall S, the head
// at the top facing ahead (+z), the body winding down left and right, the tail curling up at the bottom in a tuft of
// flame; the belly's broad plates turned to the front; slender limbs, one foreleg raised; amber mane and tufts.
const BODY = { side: 'scales', top: 'scalesDark', bottom: 'belly' };
const P = [[0, 640, 40], [0, 610, 22], [-50, 560, 6], [-100, 490, -4], [-96, 420, 6], [-30, 372, 16], [56, 340, 12], [106, 286, 0],
  [96, 214, -8], [30, 172, 4], [-48, 150, 14], [-100, 108, 12], [-92, 54, 2], [-24, 30, 4], [56, 40, 16], [104, 80, 26], [116, 132, 30], [100, 160, 30]];
function spline(t) {
  const n = P.length - 1, f = t * (n - 2) + 1, i = Math.min(Math.floor(f), n - 2), u = f - i, r = [];
  for (let k = 0; k < 3; k++) { const p0 = P[i - 1][k], p1 = P[i][k], p2 = P[i + 1][k], p3 = P[i + 2][k];
    r.push(0.5 * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u * u + (-p0 + 3 * p1 - 3 * p2 + p3) * u * u * u)); }
  return r;
}
const radius = (t) => t < 0.05 ? 32 + t * 120 : t < 0.5 ? 38 : Math.max(9, 38 - (t - 0.5) * 60);
/** For a segment along d: the rotation, flipped end for end if need be so the box's underside (the belly) faces the front. */
function bellyFront(d) {
  const pick = (v) => { const r = along(v), x = r[0] * Math.PI / 180, y = r[1] * Math.PI / 180;
    return { r, belly: [Math.sin(x) * Math.sin(y), -Math.cos(x), Math.sin(x) * Math.cos(y)] }; };
  const a = pick(d), b = pick(d.map((v) => -v));
  return a.belly[2] <= b.belly[2] ? a : b; // (the formula above is in the mirrored frame: the smaller z faces the viewer)
}
const SEG = 40;
for (let i = 0; i < SEG; i++) {
  const t0 = i / SEG, t1 = (i + 1) / SEG, tm = (t0 + t1) / 2, a = spline(t0), b = spline(t1), c = spline(tm), r = radius(tm);
  const d = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], len = Math.hypot(...d) + 8, { r: rot, belly } = bellyFront(d);
  cube('body', c, [r * 2, r * 2, len], BODY, rot);
  // The broad belly plates, a band on the underside; the amber crest of hair along the back, raked back.
  // The belly: a band of plates on the front of the body (the side turned to +z), a square bar so its turn does not matter.
  const n0 = Math.hypot(...d), dz = d[2] / n0, front = [-d[0] / n0 * dz, -d[1] / n0 * dz, 1 - dz * dz], fl = Math.hypot(...front);
  const fr = front.map((v) => v / fl);
  cube('belly', add(c, fr.map((v) => v * r * 0.5)), [r * 1.05, r * 1.05, len - 2], 'belly', rot);
  if (i % 2 === 1 && tm < 0.93) {
    const back = add(fr.map((v) => -v * 0.6), [0, 0.4, 0]), base = add(c, back.map((v) => v * r * 0.9)), n = Math.hypot(...d), hgt = r * (tm < 0.2 ? 1.0 : 0.7);
    rod('crest', base, add(base, add(back.map((v) => v * hgt), d.map((v) => -v / n * hgt * 0.5))), 5, r * 0.8, MANE);
  }
}
// The tail's flame: amber locks fanning out.
{ const e = spline(1), f = spline(0.96), dir = [e[0] - f[0], e[1] - f[1], e[2] - f[2]], n = Math.hypot(...dir);
  for (let k = -3; k <= 3; k++) rod('tail', e, add(e, [dir[0] / n * 110 + k * 16, dir[1] / n * 110 + Math.abs(k) * -14 + k * 10, dir[2] / n * 50 + k * 6]), 16, 24, MANE); }

// The head, at the spine's start, facing +z: a long snout, a lion's brow, the jaw open.
const H0 = spline(0), hx = H0[0], hy = H0[1], hz = H0[2];
const PLATE = { side: 'plate', top: 'plate', bottom: 'belly' };
// The head: big and heavy, plates stacked like armour. hx, hy, hz: the back of the skull.
cube('head', [hx, hy + 6, hz + 24], [100, 72, 78], PLATE);                                  // the skull
cube('head', [hx, hy + 46, hz + 30], [80, 14, 58], 'plate');                                // the crown plate
cube('head', [hx, hy - 4, hz + 92], [78, 48, 70], { side: 'plate', top: 'plate', bottom: 'belly', front: 'nose' }); // the muzzle, broad
cube('head', [hx, hy + 24, hz + 88], [52, 14, 60], 'plate', [-6, 0, 0]);                    // the bridge, a ridge up the muzzle
cube('head', [hx, hy + 30, hz + 120], [40, 14, 12], 'plate');                               // the nose's bulge
cube('head', [hx, hy - 30, hz + 108], [86, 14, 40], 'belly');                               // the upper lip
cube('jaw', [hx, hy - 58, hz + 88], [74, 20, 76], { side: 'plate', top: 'mouth', bottom: 'belly' }, [-18, 0, 0]); // the jaw, open
cube('jaw', [hx, hy - 40, hz + 86], [66, 4, 66], 'mouth', [-9, 0, 0]);
cube('jaw', [hx, hy - 48, hz + 92], [26, 8, 50], 'tongue', [-14, 0, 0]);
for (const side of [-1, 1]) {
  for (let k = 0; k < 4; k++) cube('teeth', [hx + side * 30, hy - 38, hz + 82 + k * 12], [8, 12, 8], 'tooth');
  cube('teeth', [hx + side * 33, hy - 42, hz + 124], [12, 26, 12], 'tooth');                // the fangs
  cube('teeth', [hx + side * 28, hy - 68, hz + 118], [10, 22, 10], 'tooth', [-18, 0, 0]);
  cube('head', [hx + side * 48, hy + 18, hz + 54], [8, 14, 26], 'eye');                    // the eyes, deep set
  cube('head', [hx + side * 42, hy + 34, hz + 54], [30, 16, 48], 'plate', [0, 0, side * -14]); // the brow, heavy
  cube('head', [hx + side * 52, hy + 30, hz + 24], [18, 22, 44], 'plate', [0, side * 10, side * -20]); // behind the brow
  // The cheek plates: two thick blades sweeping back.
  for (let k = 0; k < 2; k++) cube('head', [hx + side * (58 + k * 8), hy - 10 - k * 22, hz + 18 - k * 8], [14, 26, 52], 'plate', [-8 - k * 10, side * 24, side * (-12 - k * 8)]);
  // The antlers: big, ivory, three tines each.
  const root = [hx + side * 32, hy + 50, hz + 18], m1 = [hx + side * 48, hy + 110, hz - 4], m2 = [hx + side * 72, hy + 160, hz - 36], tip = [hx + side * 82, hy + 208, hz - 70];
  chain('horns', [root, m1, m2, tip], 26, 26, 'horn', 0.5);
  rod('horns', m1, add(m1, [side * 56, 24, 8]), 16, 16, 'horn');
  rod('horns', add(m1, [side * 56, 24, 8]), add(m1, [side * 70, 66, 0]), 12, 12, 'horn');
  rod('horns', m2, add(m2, [side * -14, 52, 24]), 14, 14, 'horn');
  // The whiskers: long thin dark lines, out to the sides and drooping.
  const w = [[hx + side * 38, hy - 10, hz + 118]]; for (let k = 1; k <= 7; k++) w.push([hx + side * (38 + k * 34), hy - 10 + (k < 3 ? k * 12 : 24 - (k - 2) * 26), hz + 118 - k * 10]);
  chain('whiskers', w, 4, 4, 'whisker');
}
// The mane: a heavy mass of big locks round the head and down the neck, in layers.
for (const [count, r0, len, w, h, z] of [[12, 52, 70, 38, 26, 0], [14, 58, 110, 34, 22, -24]]) {
  for (let k = 0; k < count; k++) {
    const ang = (k + (z ? 0.5 : 0)) / count * Math.PI * 2, ca = Math.cos(ang), sa = Math.sin(ang);
    if (sa < -0.7) continue;
    const base = [hx + ca * r0, hy + 10 + sa * r0 * 0.8, hz + z], tip = [hx + ca * (r0 + len), hy + 14 + sa * (r0 + len) * 0.85, hz + z - 50];
    rod('mane', base, tip, w, h, MANE);
  }
}
for (let k = 0; k < 6; k++) { const c = spline(0.02 + k * 0.03), r = radius(0.02 + k * 0.03); for (const side of [-1, 1]) rod('mane', add(c, [side * r * 0.7, r * 0.5, 0]), add(c, [side * (r + 50), r + 30 - k * 4, -40]), 30, 22, MANE); }
for (let k = -2; k <= 2; k++) rod('mane', [hx + k * 16, hy - 40, hz + 50], [hx + k * 22, hy - 100, hz + 34], 22, 18, MANE); // the beard
// The limbs: slender, scaled, an amber tuft at the elbow, four claws spread; the right foreleg raised, as in the image.
const limb = (t, side, raised) => {
  const c = spline(t), r = radius(t);
  const sh = add(c, [side * r * 0.9, -r * 0.2, 12]);
  const el = raised ? add(sh, [side * 34, 56, 24]) : add(sh, [side * 30, -40, 18]);
  const wr = raised ? add(el, [side * 4, 60, 20]) : add(el, [side * 6, -44, 18]);
  rod('legs', sh, el, 20, 20, BODY); rod('legs', el, wr, 16, 16, BODY);
  rod('legs', el, add(el, [side * 22, raised ? -10 : 12, -30]), 12, 10, MANE);                 // the elbow tuft
  cube('legs', wr, [22, 10, 22], { side: 'scales', top: 'scales', bottom: 'belly' });
  for (let k = 0; k < 4; k++) { const sp = (k - 1.5) * 0.5, b = add(wr, [Math.sin(sp) * 10, raised ? 4 : -4, 8]);
    rod('claws', b, add(b, [Math.sin(sp) * 16, raised ? 14 : -10, 12 + Math.cos(sp) * 6]), 4, 4, 'claw'); }
};
limb(0.14, 1, true); limb(0.14, -1, false); limb(0.52, 1, false); limb(0.52, -1, false);
// The pearl, held before the raised claw.
{ const c = add(spline(0.14), [radius(0.14) + 80, 90, 70]); cube('pearl', c, [22, 22, 22], 'pearl', [45, 45, 0]); }


const bones = [{ name: 'dragon', pivot: [0, 0, 0], cubes: [] }];
for (const [name, list] of Object.entries(cubes)) bones.push({ name, parent: 'dragon', pivot: [0, 0, 0], cubes: list });
const geo = { format_version: '1.12.0', 'minecraft:geometry': [{ description: { identifier: 'geometry.golden_dragon', texture_width: 512, texture_height: 512,
  visible_bounds_width: 60, visible_bounds_height: 40, visible_bounds_offset: [0, 10, 0] }, bones }] };
fs.writeFileSync(path.join(out, 'golden_dragon.geo.json'), JSON.stringify(geo));
fs.writeFileSync(path.join(out, 'golden_dragon.png'), atlas);
console.log('cubes', Object.values(cubes).reduce((n, l) => n + l.length, 0));
