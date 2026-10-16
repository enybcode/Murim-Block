// Mock-up of the Bandit Warlord's second-phase arena (Enzo, 16/10/2026: "an arena in the sky, a kind of hollowed
// meteor"; then "bigger, his blows strike wide areas, it needs a really big area", "and deeper too"), drawn with
// st-runtime.js by render-warlord-sky-arena.cjs. 1 voxel = 1 block; the arena floor's middle is (0, 0, 0). Proposal
// only, nothing in game (docs/mobs/bandit-warlord.md).
window.SKY = (() => {
  const S = ST;
  const rnd = S.seeded(1610);

  // ---- value noise, smooth, -1..1
  const hash = (x, y, z) => { let h = (x * 374761393 + y * 668265263 + z * 1274126177) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xffff) / 0x7fff - 1; };
  const smooth = t => t * t * (3 - 2 * t);
  function noise(x, y, z) {
    const xi = Math.floor(x), yi = Math.floor(y), zi = Math.floor(z), xf = smooth(x - xi), yf = smooth(y - yi), zf = smooth(z - zi);
    const l = (a, b, t) => a + (b - a) * t;
    const c = (dx, dy, dz) => hash(xi + dx, yi + dy, zi + dz);
    return l(l(l(c(0, 0, 0), c(1, 0, 0), xf), l(c(0, 1, 0), c(1, 1, 0), xf), yf), l(l(c(0, 0, 1), c(1, 0, 1), xf), l(c(0, 1, 1), c(1, 1, 1), xf), yf), zf);
  }
  const fbm = (x, y, z) => noise(x, y, z) * 0.6 + noise(x * 2.1, y * 2.1, z * 2.1) * 0.3 + noise(x * 4.3, y * 4.3, z * 4.3) * 0.1;

  // ---- the meteor: a rock some 170 blocks across, its top hollowed into a bowl 40 deep round a fighting floor near
  // 100 across, its underside a rough cone
  const R = 84, TOP = 86, DEPTH = 100;
  /** The bowl's floor height at a distance from the middle: flat over the fighting floor, rising steeply to the rim. */
  const FLAT = 48, BOWL = 72, RISE = 40;
  const floorAt = r => (r < FLAT ? 0 : RISE * Math.pow((r - FLAT) / (BOWL - FLAT), 1.5));
  function rock(x, y, z) {
    const r = Math.hypot(x, z), n = fbm(x * 0.035, y * 0.035, z * 0.035);
    const body = y >= 0 ? (r / R) ** 2 + (y / TOP) ** 2 <= 1 + 0.3 * n
                        : (r / (R * (1 + 0.16 * noise(x * 0.015 + 3, 0, z * 0.015)))) ** 2 + Math.pow(-y / DEPTH, 0.85) <= 1 + 0.4 * n;
    if (!body) return false;
    // Hollowed: the bowl, its rim a little ragged.
    if (r <= BOWL + 4 * n && y > floorAt(r) + (r < FLAT ? 0.45 : 1.2) * noise(x * 0.3, 0, z * 0.3)) return false;
    return true;
  }

  function build() {
    S.clear();
    const X0 = -R - 14, Y0 = -DEPTH - 10, NX = 2 * R + 29, NY = DEPTH + TOP + 22;
    const grid = new Uint8Array(NX * NY * NX);
    const idx = (x, y, z) => ((x - X0) * NY + (y - Y0)) * NX + (z - X0);
    for (let x = X0; x < X0 + NX; x++) for (let z = X0; z < X0 + NX; z++) {
      if (Math.hypot(x, z) > R * 1.5) continue;
      for (let y = Y0; y < Y0 + NY; y++) if (rock(x, y, z)) grid[idx(x, y, z)] = 1;
    }
    const at = (x, y, z) => x >= X0 && x < X0 + NX && z >= X0 && z < X0 + NX && y >= Y0 && y < Y0 + NY && grid[idx(x, y, z)] === 1;
    for (let x = X0; x < X0 + NX; x++) for (let z = X0; z < X0 + NX; z++) for (let y = Y0; y < Y0 + NY; y++) {
      if (!at(x, y, z)) continue;
      const open = !at(x, y + 1, z) || !at(x, y - 1, z) || !at(x + 1, y, z) || !at(x - 1, y, z) || !at(x, y, z + 1) || !at(x, y, z - 1);
      if (!open) continue;
      const r = Math.hypot(x, z), up = !at(x, y + 1, z);
      let m;
      if (up && r < FLAT + 2 && y <= 1) {
        // The fighting floor: scorched basalt, the scar of his landing in the middle, cracks glowing out from it.
        const a = Math.atan2(z, x), crack = Math.abs(((a / (Math.PI * 2)) * 14 + 0.25 * fbm(x * 0.1, 0, z * 0.1) + 10) % 1 - 0.5);
        if (r < 6) m = rnd() < 0.35 ? 'magma' : 'obsidian';
        else if (r < 11) m = rnd() < 0.5 ? 'obsidian' : 'scorched';
        else if (crack > 0.475 && r < FLAT - 4) m = 'magma';
        else m = rnd() < 0.18 ? 'basalt_dark' : rnd() < 0.5 ? 'scorched' : 'basalt';
      } else {
        // Its walls and underside: dark basalt, veined with molten rock.
        const v = Math.abs(noise(x * 0.025 + 7, y * 0.035, z * 0.025));
        m = v < 0.025 && y < TOP * 0.6 ? 'magma' : up && y > 4 ? (rnd() < 0.4 ? 'scorched' : 'basalt') : rnd() < 0.5 ? 'basalt_dark' : 'basalt';
      }
      S.set(x, y, z, m);
    }
    const surface = (x, z) => { let y = Y0 + NY - 1; while (y > Y0 && !at(x, y, z)) y--; return y + 1; };
    const bottom = (x, z) => { let y = Y0; while (y < Y0 + NY && !at(x, y, z)) y++; return y; };
    // Molten drips hanging from its underside.
    for (let i = 0; i < 70; i++) {
      const a = rnd() * Math.PI * 2, r = rnd() * R * 0.7, x = Math.round(Math.cos(a) * r), z = Math.round(Math.sin(a) * r);
      const y = bottom(x, z), len = 3 + Math.floor(rnd() * 10);
      if (y >= 0) continue;
      for (let k = 1; k <= len; k++) S.set(x, y - k, z, k >= len - 1 ? 'magma' : 'basalt_dark');
    }
    // Spikes round the rim, a few leaning out.
    for (let i = 0; i < 34; i++) {
      const a = (i / 34) * Math.PI * 2 + rnd() * 0.15, r0 = BOWL + 4 + rnd() * 4, h = 5 + Math.floor(rnd() * 12), lean = 0.25 + rnd() * 0.3;
      const base = surface(Math.round(Math.cos(a) * r0), Math.round(Math.sin(a) * r0)) - 1;
      for (let k = 0; k < h; k++) {
        const rr = r0 + k * lean, w = k < h - 3 ? 1 : 0;
        const cx = Math.round(Math.cos(a) * rr), cz = Math.round(Math.sin(a) * rr);
        for (let dx = -w; dx <= w; dx++) for (let dz = -w; dz <= w; dz++) S.set(cx + dx, base + k, cz + dz, k === h - 1 ? 'scorched' : 'basalt_dark');
      }
    }
    // A few broken boulders on the fighting floor, low (no cover from his blows), thrown off by the fall.
    for (let i = 0; i < 9; i++) {
      const a = rnd() * Math.PI * 2, r = 16 + rnd() * (FLAT - 22), x = Math.round(Math.cos(a) * r), z = Math.round(Math.sin(a) * r);
      S.blob(x, 1, z, 2 + Math.floor(rnd() * 2), 1, 2, rnd() < 0.5 ? 'basalt_dark' : 'scorched', 0.25, rnd);
    }
    // Twelve braziers round the fighting floor and the warlord's banners on the rim (purple and gold).
    for (let i = 0; i < 12; i++) {
      const a = (i / 12) * Math.PI * 2 + Math.PI / 12, x = Math.round(Math.cos(a) * (FLAT - 2)), z = Math.round(Math.sin(a) * (FLAT - 2));
      const y = surface(x, z);
      S.set(x, y, z, 'iron'); S.set(x, y + 1, z, 'iron'); S.set(x, y + 2, z, 'bronze');
      S.fill(x - 1, y + 3, z - 1, x + 1, y + 3, z + 1, 'bronze'); S.fill(x, y + 4, z, x, y + 5, z, 'fire');
    }
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * Math.PI * 2, x = Math.round(Math.cos(a) * (BOWL - 2)), z = Math.round(Math.sin(a) * (BOWL - 2));
      const y = surface(x, z);
      S.fill(x, y, z, x, y + 15, z, 'wood_dark');
      const tx = -Math.sin(a), tz = Math.cos(a);
      for (let k = -3; k <= 3; k++) S.set(x + Math.round(tx * k), y + 15, z + Math.round(tz * k), 'gold');
      for (let k = -2; k <= 2; k++) for (let d = 1; d <= 10; d++) S.set(x + Math.round(tx * k), y + 15 - d, z + Math.round(tz * k), d === 10 && k === 0 ? 'gold' : d === 10 ? null : 'banner_purple');
    }
    // Rocks drifting round it, torn off by the fall.
    for (let i = 0; i < 26; i++) {
      const a = rnd() * Math.PI * 2, r = 105 + rnd() * 55, y = -60 + rnd() * 100, s = 2 + Math.floor(rnd() * 4);
      for (let k = 0; k < 3; k++) S.blob(Math.round(rnd() * 4 - 2) + Math.round(Math.cos(a) * r), Math.round(y + rnd() * 3 - 1.5), Math.round(rnd() * 4 - 2) + Math.round(Math.sin(a) * r), s, Math.max(1, s - 1), s, rnd() < 0.5 ? 'basalt' : 'scorched', 0.3, rnd);
    }
    // (Embers rising from the cracks are particles in game, not drawn here.)
    for (let i = 0; i < 0; i++) {
      const a = rnd() * Math.PI * 2, r = rnd() * FLAT * 0.8;
      S.set(Math.round(Math.cos(a) * r), 2 + Math.floor(rnd() * 20), Math.round(Math.sin(a) * r), 'ember');
    }
    // The sea of clouds far below.
    for (let i = 0; i < 170; i++) {
      const x = Math.round((rnd() - 0.5) * 900), z = Math.round((rnd() - 0.5) * 900);
      S.blob(x, -170 + Math.round(rnd() * 8), z, 14 + Math.floor(rnd() * 16), 3, 10 + Math.floor(rnd() * 14), 'cloud', 0.2, rnd);
    }
    // For scale: the warlord (3.6 blocks: 4 here, purple and gold) near the scar, two players facing him.
    const warlord = (x, z) => {
      S.fill(x, 1, z, x + 1, 2, z, 'robe_black'); S.fill(x, 3, z, x + 1, 3, z, 'banner_purple'); S.set(x - 1, 3, z, 'skin'); S.set(x + 2, 3, z, 'skin');
      S.set(x - 1, 4, z, 'banner_purple'); S.set(x + 2, 4, z, 'banner_purple'); S.fill(x, 4, z, x + 1, 4, z, 'iron'); S.set(x, 5, z, 'skin'); S.set(x + 1, 5, z, 'skin');
    };
    warlord(3, -4);
    S.person(-12, 0, 14, 'robe_blue'); S.person(-17, 0, 8, 'robe_red');
  }
  build();

  const SKY_COLOUR = '#9FB8DC', FOG = [520, 1300];
  const view = (position, target, fov = 45) => ({ view: { position, target, fov, unclamped: true }, sky: SKY_COLOUR, fog: FOG });
  return {
    overview: () => view([215, 200, 235], [0, -24, 0], 44),
    rim: () => view([52, 44, 58], [-8, -2, -8], 62),
    floor: () => view([-22, 3.6, 26], [6, 3, -8], 70),
    below: () => view([165, -180, 140], [0, -30, 0], 46),
    plan: () => ({ ...view([0.5, 370, 0.6], [0, 0, 0], 38),
      marks: [['1', 0, 0, 0], ['2', 30, 0, -24], ['3', Math.round(Math.cos(Math.PI / 12) * (FLAT - 2)), 4, Math.round(Math.sin(Math.PI / 12) * (FLAT - 2))],
        ['4', BOWL - 2, RISE, 0], ['5', 0, TOP, BOWL + 8]] }),
  };
})();
true;
