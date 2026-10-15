// Pixel-art flags and logos of the five factions (docs/lore/README.md), drawn in code so they can be redrawn.
//   node scripts/art/factions.cjs [outDir] [previewScale]
// Writes <id>-logo.png (32x32), <id>-flag.png (32x48) and, at previewScale, a sheet of all of them (factions-sheet.png).
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const OUT = process.argv[2] || path.join(__dirname, "../../art/factions");
const SCALE = +(process.argv[3] || 8);

// ---------- A tiny canvas ----------
const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
function canvas(w, h) {
  const px = new Array(w * h).fill(null);
  const c = {
    w, h, px, ox: 0, oy: 0,
    set(x, y, col) { x = Math.round(x + c.ox); y = Math.round(y + c.oy); if (x >= 0 && y >= 0 && x < w && y < h && col) px[y * w + x] = typeof col === "string" ? hex(col) : col; },
    get(x, y) { return x >= 0 && y >= 0 && x < w && y < h ? px[y * w + x] : null; },
    rect(x0, y0, x1, y1, col) { for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) c.set(x, y, col); },
    // Fills every pixel whose centre passes test(x + .5, y + .5); colour may be a function of (x, y).
    fill(test, col, x0 = -2, y0 = -2, x1 = 40, y1 = 50) {
      for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) if (test(x + .5, y + .5)) c.set(x, y, typeof col === "function" ? col(x, y) : col);
    },
    disk(cx, cy, r, col) { c.fill((x, y) => (x - cx) ** 2 + (y - cy) ** 2 <= r * r, col); },
    poly(pts, col) {
      c.fill((x, y) => {
        let inside = false;
        for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
          const [xi, yi] = pts[i], [xj, yj] = pts[j];
          if ((yi > y) !== (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside;
        }
        return inside;
      }, col);
    },
    star(x, y, col, big) { c.set(x, y, col); c.set(x - 1, y, col); c.set(x + 1, y, col); c.set(x, y - 1, col); c.set(x, y + 1, col); if (big) { c.set(x - 2, y, col); c.set(x + 2, y, col); c.set(x, y - 2, col); c.set(x, y + 2, col); } },
  };
  return c;
}
// A dark outline around everything drawn on a transparent layer, the way Minecraft items read.
function outline(layer, col) {
  const add = [];
  for (let y = 0; y < layer.h; y++) for (let x = 0; x < layer.w; x++) {
    if (layer.get(x, y)) continue;
    if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => layer.get(x + dx, y + dy))) add.push([x, y]);
  }
  add.forEach(([x, y]) => { layer.px[y * layer.w + x] = hex(col); });
}
function paste(dst, src, dx = 0, dy = 0) {
  for (let y = 0; y < src.h; y++) for (let x = 0; x < src.w; x++) { const p = src.get(x, y); if (p) { const X = x + dx, Y = y + dy; if (X >= 0 && Y >= 0 && X < dst.w && Y < dst.h) dst.px[Y * dst.w + X] = p; } }
}
// Deterministic grain, so cloth and metal are not flat.
const grain = (x, y, seed = 1) => { const n = Math.sin(x * 12.9898 + y * 78.233 + seed * 37.719) * 43758.5453; return n - Math.floor(n); };
const shade = (h, k) => { const [r, g, b] = hex(h); const f = v => Math.max(0, Math.min(255, Math.round(v * k))); return [f(r), f(g), f(b), 255]; };

// ---------- The five factions ----------
const FACTIONS = [
  {
    id: "orthodox_alliance", name: "Orthodox Alliance", han: "正派武林盟",
    field: "#2B5FA8", trim: "#E9D9A6", ink: "#14284A",
    meaning: "Azure and gold. A sword planted in the three sacred peaks: the Alliance guarding the centre.",
    emblem(c) {
      const snow = (x, y) => (x > 12 ? shade("#D6E2EE", 1) : hex("#F4F7FA"));
      c.poly([[0, 22], [6, 12], [12, 22]], (x, y) => (x > 6 ? hex("#9FB6CF") : hex("#C9D8E8")));
      c.poly([[12, 22], [18, 12], [24, 22]], (x, y) => (x > 18 ? hex("#9FB6CF") : hex("#C9D8E8")));
      c.poly([[6.5, 22], [12, 7], [17.5, 22]], snow);
      // The sword, point down into the peak.
      c.rect(11, 5, 12, 19, "#DCE4EC"); c.rect(12, 5, 12, 19, "#A9B6C4"); c.set(11, 20, "#C9D2DC"); c.set(12, 20, "#A9B6C4");
      c.rect(8, 4, 15, 4, "#D9B25F"); c.set(8, 4, "#B08A3A"); c.set(15, 4, "#B08A3A");
      c.rect(11, 1, 12, 3, "#6B3E26"); c.rect(11, 0, 12, 0, "#D9B25F");
    },
  },
  {
    id: "heavenly_demon_cult", name: "Heavenly Demon Cult", han: "天魔神教",
    field: "#7E141C", trim: "#1A1012", ink: "#0E0708",
    meaning: "Crimson and black. The red moon of Hong Yue with the Demon's eye open in it, and the blood it drinks.",
    emblem(c) {
      c.disk(12, 11.5, 9, (x, y) => ((x - 9) ** 2 + (y - 8) ** 2 < 26 ? hex("#F06A4F") : (x + y > 28 ? hex("#B8232C") : hex("#DE3F31"))));
      // The eye: an almond, a gold iris inside it, a black slit pupil.
      const eye = (x, y) => ((x - 12) / 7) ** 2 + ((y - 11.5) / 3.3) ** 2 <= 1;
      c.fill(eye, "#140A0B");
      c.fill((x, y) => eye(x, y) && (x - 12) ** 2 + (y - 11.5) ** 2 <= 9.5 && ((x - 12) / 6) ** 2 + ((y - 11.5) / 2.6) ** 2 <= 1, (x, y) => (y < 11 ? hex("#FFE08A") : hex("#F2B63C")));
      c.rect(11.5, 9, 11.5, 13, "#140A0B"); c.rect(12, 10, 12, 12, "#140A0B");
    },
  },
  {
    id: "northern_abyss_palace", name: "Northern Dark Palace", han: "北冥宫",
    field: "#1B2230", trim: "#C9D6E3", ink: "#0B0F16",
    meaning: "Night and frost. The seven stars of the Northern Dipper over a snow peak: the north that burned and still watches.",
    emblem(c) {
      c.poly([[2, 23], [12, 13], [22, 23]], (x, y) => (x > 12 ? hex("#8FA9C4") : hex("#DCE7F2")));
      c.poly([[9, 16], [12, 13], [15, 16], [13, 15], [12, 16], [11, 15]], "#F7FBFF");
      // The Big Dipper: bowl at left, handle curving up to the right.
      [[3, 9], [5, 12], [9, 12], [9, 8], [13, 6], [17, 4], [21, 2]].forEach(([x, y], i) => c.star(x, y, i === 6 ? "#FFFFFF" : "#BFE3FF", i === 6));
    },
  },
  {
    id: "four_great_families", name: "Four Great Families", han: "四大世家",
    field: "#2E6B45", trim: "#C9A45C", ink: "#12301F",
    meaning: "Jade and gold. Four camphor leaves around one seal: four bloodlines bound to one land.",
    emblem(c) {
      const leaf = (cx, cy, ax, ay) => c.fill((x, y) => {
        const dx = x - cx, dy = y - cy; const u = dx * ax + dy * ay, v = -dx * ay + dy * ax;
        return (u / 6.2) ** 2 + (v / 2.8) ** 2 <= 1;
      }, (x, y) => (grain(x, y, 4) > .5 ? hex("#7FC28A") : hex("#5AA66A")));
      const s = Math.SQRT1_2;
      leaf(6.5, 6.5, s, s); leaf(17.5, 6.5, -s, s); leaf(6.5, 17.5, s, -s); leaf(17.5, 17.5, -s, -s);
      // Veins
      for (let k = 2; k <= 6; k++) { c.set(12 - k, 12 - k, "#3E8550"); c.set(11 + k, 12 - k, "#3E8550"); c.set(12 - k, 11 + k, "#3E8550"); c.set(11 + k, 11 + k, "#3E8550"); }
      // The seal
      c.rect(8, 8, 15, 15, "#D9B25F"); c.rect(8, 15, 15, 15, "#B08A3A"); c.rect(15, 8, 15, 15, "#B08A3A");
      c.rect(10, 10, 13, 13, "#8C6420"); c.rect(11, 11, 12, 12, "#D9B25F");
    },
  },
  {
    id: "thousand_canals_pavilion", name: "Thousand Canals Pavilion", han: "千渠阁",
    field: "#4B2F7A", trim: "#C9A45C", ink: "#22143A",
    meaning: "Violet and gold. A square-holed coin over the canal waters: everything has a price on the eastern waters.",
    emblem(c) {
      c.disk(12, 9.5, 8.5, (x, y) => (x + y < 17 ? hex("#F2CF6B") : (x + y > 25 ? hex("#A87D2A") : hex("#E0B64E"))));
      c.fill((x, y) => { const d = Math.hypot(x - 12, y - 9.5); return d > 6.3 && d < 7.2; }, "#B8892F");
      c.rect(10, 8, 13, 11, null); for (let y = 8; y <= 11; y++) for (let x = 10; x <= 13; x++) c.px[(y + c.oy) * c.w + x + c.ox] = null;
      c.rect(9, 7, 14, 7, "#A87D2A"); c.rect(9, 12, 14, 12, "#F2CF6B"); c.rect(9, 8, 9, 11, "#A87D2A"); c.rect(14, 8, 14, 11, "#F2CF6B");
      // Waves
      for (let x = 1; x <= 22; x++) {
        const w = Math.round(Math.sin(x * .9) * 1.2);
        c.set(x, 19 + w, "#E8F4FF"); c.set(x, 20 + w, "#5FC4C0");
        c.set(x, 22 + Math.round(Math.sin(x * .9 + 2) * 1.2), "#3E9C99");
      }
    },
  },
];

// ---------- Logo and flag ----------
function emblemLayer(f, w, h, ox, oy) {
  const e = canvas(w, h); e.ox = ox; e.oy = oy; f.emblem(e); e.ox = 0; e.oy = 0;
  outline(e, f.ink);
  return e;
}
function logo(f) {
  const c = canvas(32, 32);
  // A round seal in the faction's colour, a trim ring, the emblem on top.
  c.disk(16, 16, 15.5, f.ink);
  c.disk(16, 16, 14.5, f.trim);
  c.disk(16, 16, 13.2, (x, y) => (grain(x, y, 2) > .82 ? shade(f.field, .9) : (x + y > 36 ? shade(f.field, .85) : hex(f.field))));
  paste(c, emblemLayer(f, 32, 32, 4, 4.5));
  return c;
}
function flag(f) {
  const c = canvas(32, 48);
  // Pole bar and finials
  c.rect(0, 0, 31, 2, "#3A2618"); c.rect(1, 1, 30, 1, "#6B4A2E"); c.rect(0, 0, 1, 2, "#D9B25F"); c.rect(30, 0, 31, 2, "#D9B25F");
  // Cloth with a swallowtail bottom
  const cloth = (x, y) => x >= 3 && x < 29 && y >= 3 && (y < 41 || (y < 47 && Math.abs(x + .5 - 16) > (y - 41) * 2.2));
  c.fill((x, y) => cloth(x, y), (x, y) => {
    const fold = Math.sin(x * .55) * .06;
    return shade(f.field, 1 + fold - (grain(x, y, 5) > .86 ? .07 : 0) - (x > 24 ? .06 : 0));
  });
  // Trim along the sides and the hem
  c.fill((x, y) => cloth(x, y) && (x < 5 || x > 26 || !cloth(x, y + 2) || !cloth(x - 2, y) || !cloth(x + 2, y)), f.trim);
  c.fill((x, y) => cloth(x, y) && (x === 5 || x === 26) && y > 4 && y < 41, shade(f.trim, .75));
  // Hanging loops
  [6, 12, 19, 25].forEach(x => { c.set(x, 3, f.ink); c.set(x + 1, 3, f.ink); });
  paste(c, emblemLayer(f, 32, 48, 4, 10));
  return c;
}

// ---------- PNG ----------
function png(c, scale = 1) {
  const W = c.w * scale, H = c.h * scale, raw = Buffer.alloc((W * 4 + 1) * H);
  for (let y = 0; y < H; y++) {
    raw[y * (W * 4 + 1)] = 0;
    for (let x = 0; x < W; x++) {
      const p = c.px[Math.floor(y / scale) * c.w + Math.floor(x / scale)] || [0, 0, 0, 0];
      raw.set(p, y * (W * 4 + 1) + 1 + x * 4);
    }
  }
  const crcTable = Array.from({ length: 256 }, (_, n) => { let k = n; for (let i = 0; i < 8; i++) k = k & 1 ? 0xedb88320 ^ (k >>> 1) : k >>> 1; return k >>> 0; });
  const crc = b => { let r = 0xffffffff; for (const v of b) r = crcTable[(r ^ v) & 255] ^ (r >>> 8); return (r ^ 0xffffffff) >>> 0; };
  const chunk = (t, d) => { const l = Buffer.alloc(4); l.writeUInt32BE(d.length); const td = Buffer.concat([Buffer.from(t), d]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(td)); return Buffer.concat([l, td, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk("IHDR", ihdr), chunk("IDAT", zlib.deflateSync(raw)), chunk("IEND", Buffer.alloc(0))]);
}

fs.mkdirSync(OUT, { recursive: true });
const sheet = canvas(FACTIONS.length * 40 + 4, 92);
sheet.fill(() => true, "#15120E", 0, 0, sheet.w, sheet.h);
FACTIONS.forEach((f, i) => {
  const l = logo(f), fl = flag(f);
  fs.writeFileSync(path.join(OUT, f.id + "-logo.png"), png(l));
  fs.writeFileSync(path.join(OUT, f.id + "-flag.png"), png(fl));
  fs.mkdirSync(path.join(OUT, "large"), { recursive: true });
  fs.writeFileSync(path.join(OUT, "large", f.id + "-logo.png"), png(l, SCALE));
  fs.writeFileSync(path.join(OUT, "large", f.id + "-flag.png"), png(fl, SCALE));
  paste(sheet, l, 4 + i * 40, 2); paste(sheet, fl, 4 + i * 40, 40);
});
fs.writeFileSync(path.join(OUT, "factions-sheet.png"), png(sheet, SCALE));
fs.writeFileSync(path.join(OUT, "factions.json"), JSON.stringify(FACTIONS.map(({ emblem, ...f }) => f), null, 2));
console.log("Wrote", FACTIONS.length, "logos and flags to", OUT);
