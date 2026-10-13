// Stick-figure PNG previews of biped poses (front view X-Z and side view Y-Z), to check a pose before trying it in game.
const fs = require('fs');
const zlib = require('zlib');
const b = require('./biped.cjs');

const BONES = [
  ['Root', 'Torso'], ['Torso', 'Chest'], ['Chest', 'Head'], ['Head', 'HEAD_TOP'],
  ['Chest', 'Arm_R'], ['Arm_R', 'Hand_R'], ['Hand_R', 'Tool_R'],
  ['Chest', 'Arm_L'], ['Arm_L', 'Hand_L'], ['Hand_L', 'Tool_L'],
  ['Root', 'Thigh_R'], ['Thigh_R', 'Leg_R'], ['Leg_R', 'FOOT_R'],
  ['Root', 'Thigh_L'], ['Thigh_L', 'Leg_L'], ['Leg_L', 'FOOT_L'],
];
const COLORS = { R: [220, 60, 60], L: [60, 110, 230], C: [40, 40, 40] };

/** Model-space points of a pose, with the bone ends (head top, feet) added. */
function points(arm, locals, offset = [0, 0, 0]) {
  const m = b.modelSpace(arm, locals);
  const p = {};
  for (const n of arm.order) p[n] = b.origin(m[n]);
  const at = (j, y) => { const r = m[j]; return [r[1] * y + r[3], r[5] * y + r[7], r[9] * y + r[11]]; };
  p.HEAD_TOP = at('Head', 0.45); p.FOOT_R = at('Leg_R', 0.375); p.FOOT_L = at('Leg_L', 0.375);
  for (const k in p) p[k] = p[k].map((v, i) => v + offset[i]);
  return p;
}

/** Renders figures ([{arm, locals, offset}]) into a PNG: front view on the left, side view on the right. */
function render(file, figures, title) {
  const W = 640, H = 360, img = Buffer.alloc(W * H * 3, 255);
  const scale = 110, ground = H - 30;
  const views = [{ cx: W / 4, axis: 0 }, { cx: 3 * W / 4, axis: 1 }];
  const plot = (x, y, c) => { if (x >= 0 && y >= 0 && x < W && y < H) { const i = (y * W + x) * 3; img[i] = c[0]; img[i + 1] = c[1]; img[i + 2] = c[2]; } };
  const line = (x0, y0, x1, y1, c, w = 2) => {
    const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0), 1);
    for (let s = 0; s <= n; s++) { const x = Math.round(x0 + (x1 - x0) * s / n), y = Math.round(y0 + (y1 - y0) * s / n); for (let a = -w; a <= w; a++) for (let d = -w; d <= w; d++) plot(x + a, y + d, c); }
  };
  for (const v of views) line(v.cx - 150, ground, v.cx + 150, ground, [150, 150, 150], 0);
  line(W / 2, 0, W / 2, H, [200, 200, 200], 0);
  for (const f of figures) {
    const p = points(f.arm, f.locals, f.offset);
    for (const v of views) for (const [a, c] of BONES) {
      const col = f.color || (a.endsWith('_R') || c.endsWith('_R') ? COLORS.R : a.endsWith('_L') || c.endsWith('_L') ? COLORS.L : COLORS.C);
      // Front view looks at the character's front: its right (+X) on the viewer's left. Side view: forward (+Y) to the right.
      const sx = q => v.axis === 0 ? v.cx - q[0] * scale : v.cx + q[1] * scale;
      line(Math.round(sx(p[a])), Math.round(ground - p[a][2] * scale), Math.round(sx(p[c])), Math.round(ground - p[c][2] * scale), col);
    }
  }
  // PNG encoding (RGB, no filter).
  const raw = Buffer.alloc((W * 3 + 1) * H);
  for (let y = 0; y < H; y++) img.copy(raw, y * (W * 3 + 1) + 1, y * W * 3, (y + 1) * W * 3);
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td) >>> 0);
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 2;
  fs.writeFileSync(file, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
}

let table;
function crc32(buf) {
  if (!table) { table = new Int32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; table[n] = c; } }
  let c = -1;
  for (const x of buf) c = table[(c ^ x) & 0xff] ^ (c >>> 8);
  return c ^ -1;
}

module.exports = { render, points };
