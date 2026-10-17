// Voxel mock-ups of the Crab King's lair (docs/structures/crab-king-lair.md): a secret boss arena, one per world, in a
// huge cave under a swamp, a war junk half sunk in the mud at its bottom. Built with ST (st-runtime.js). 1 voxel = 1
// block. x east, z south, y up; the cave floor at y 0. Three proposals:
//   A  the Sunken Pit      a round cavern under a dome, the junk stranded at the back, a tunnel down from a sinkhole;
//   B  the Junk's Nave     a long gallery, the junk heeled over in its middle, mud flats either side, a channel;
//   C  the Root Well       a round pit open to the swamp through a shaft hung with mangrove roots, a beam of light,
//                          the junk broken in two, its bow standing out of the mud.
// The views cut the near half of the cave away (a diorama) or look from the entrance; the sinkhole is seen from the
// swamp above.
window.CAVE = (() => {
  const S = ST;
  let r = S.seeded(1);

  /** A smooth noise in [-1, 1], for rough rock. */
  const n3 = (x, y, z) => Math.sin(x * 0.31 + z * 0.17) * 0.5 + Math.sin(y * 0.43 - x * 0.12) * 0.3 + Math.cos(z * 0.27 + y * 0.21) * 0.4;
  const rock = (x, y, z) => {
    const k = n3(x * 1.7, y * 1.3, z * 1.9);
    return k > 0.75 ? 'moss' : k > -0.25 ? 'deepslate' : k > -0.65 ? 'stone' : 'cobble';
  };

  /**
   * The cave: an ellipsoid (radii rx, ry, rz about (cx, cy, cz)) roughened by noise, its shell two blocks thick, its floor
   * flat mud at y 0. `keep(x, y, z)` says which shell voxels to draw (the views cut the cave open). Returns `inside`.
   */
  function cave(cx, cy, cz, rx, ry, rz, keep, { floor = 0, roughness = 0.1 } = {}) {
    const inside = (x, y, z) => {
      if (y < floor) return false;
      const d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2;
      return d < 1 + roughness * n3(x, y, z);
    };
    for (let x = Math.floor(cx - rx - 4); x <= cx + rx + 4; x++) for (let z = Math.floor(cz - rz - 4); z <= cz + rz + 4; z++)
      for (let y = floor - 1; y <= cy + ry + 4; y++) {
        if (inside(x, y, z)) continue;
        let near = false;
        for (const [dx, dy, dz] of [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1], [2, 0, 0], [-2, 0, 0], [0, 2, 0], [0, -2, 0], [0, 0, 2], [0, 0, -2]])
          if (inside(x + dx, y + dy, z + dz)) { near = true; break; }
        if (!near || !keep(x, y, z)) continue;
        S.set(x, y, z, y <= floor ? 'mud_dark' : rock(x, y, z));
      }
    // The mud floor, darker in hollows, puddles of swamp water.
    for (let x = Math.floor(cx - rx); x <= cx + rx; x++) for (let z = Math.floor(cz - rz); z <= cz + rz; z++) {
      if (!inside(x, floor, z)) continue;
      const k = n3(x * 0.6, 3, z * 0.6);
      S.set(x, floor, z, k > 0.6 ? 'swamp_water' : k < -0.4 ? 'mud_dark' : 'swamp_mud');
    }
    return inside;
  }

  /** Stalactites from the ceiling (and a few stalagmites), glowing lichen on the walls. */
  function dress(inside, cx, cz, rx, rz, top, keep, count = 70) {
    for (let k = 0; k < count; k++) {
      const x = Math.round(cx + (r() - 0.5) * rx * 1.8), z = Math.round(cz + (r() - 0.5) * rz * 1.8);
      let y = top;
      while (y > 1 && !inside(x, y, z)) y--;
      if (y <= 3 || !keep(x, y + 1, z)) continue;
      const len = 2 + Math.floor(r() * 7);
      for (let i = 0; i < len; i++) if (inside(x, y - i, z) && y - i > 6) S.set(x, y - i, z, i > len - 3 ? 'stone' : 'deepslate');
    }
    for (let k = 0; k < count * 3; k++) {
      const a = r() * Math.PI * 2, h = 2 + r() * (top * 0.7);
      const x = Math.round(cx + Math.cos(a) * rx * 1.02), z = Math.round(cz + Math.sin(a) * rz * 1.02), y = Math.round(h);
      for (let d = 0; d < 6; d++) {
        const px = Math.round(cx + Math.cos(a) * (rx - d)), pz = Math.round(cz + Math.sin(a) * (rz - d));
        if (S.get(px, y, pz) && S.get(px, y, pz) !== 'glow_lichen' && keep(px, y, pz)) { S.set(px, y, pz, 'glow_lichen'); break; }
      }
    }
  }

  /**
   * A war junk (戰船): a rounded hull, a high stern castle, three masts with battened sails (torn), lanterns. Built in
   * its own axes (u along, from stern to bow; v across; h up), then pitched (bow down: + degrees), rolled (heeled to +v),
   * turned (yaw degrees about up) and placed with its keel's middle at (x, y, z). `broken` cuts it at u = broken and
   * tips the bow half up out of the mud. Nothing below `mudY` is drawn (it is in the mud), and mud banks up against it.
   */
  function junk(x0, y0, z0, { len = 44, width = 7, pitch = 0, roll = 0, yaw = 0, mudY = 0, broken = null, bowRise = 70 } = {}) {
    const rad = d => d * Math.PI / 180;
    const parts = [];
    const put = (u, v, h, m) => parts.push([u, v, h, m]);
    const half = len / 2;
    for (let u = -half; u <= half; u++) {
      const t = u / half;                                         // -1 stern, +1 bow
      const w = Math.max(1, Math.round(width * (1 - Math.max(0, t) ** 2 * 0.75 - Math.max(0, -t) ** 4 * 0.25)));
      const deck = Math.round(8 + Math.max(0, -t - 0.55) * 14 + Math.max(0, t - 0.7) * 8);
      for (let v = -w; v <= w; v++) {
        const bottom = Math.round((Math.abs(v) / w) ** 2 * 5);
        put(u, v, bottom, 'wood_dark');                           // the hull's bottom
        if (Math.abs(v) === w) for (let h = bottom; h <= deck + 1; h++) put(u, v, h, h === deck - 1 ? 'red' : 'wood_dark');   // the sides, a red strake
        put(u, v, deck, (u + v) % 7 === 0 ? 'wood_dark' : 'wood');   // the deck (holed below)
      }
      if (Math.abs(u) === half) for (let v = -w; v <= w; v++) for (let h = 0; h <= deck; h++) put(u, v, h, 'wood_dark');
    }
    // The stern castle: two decks, white walls, red posts, a tiled roof (drawn as a ridge).
    for (let u = -half; u <= -half + 9; u++) for (let v = -6; v <= 6; v++) for (let h = 1; h <= 5; h++) {
      const deck = Math.round(8 + Math.max(0, -u / half - 0.55) * 14);
      const wall = u === -half + 9 || Math.abs(v) === 6;
      if (wall) put(u, v, deck + h, Math.abs(v) === 6 && u % 3 === 0 ? 'red' : 'white');
      if (h === 5) put(u, v, deck + h + 1, Math.abs(v) >= 5 ? 'tile' : 'ridge');
    }
    for (const v of [-3, 3]) put(-half + 9, v, 18, 'lantern');
    // Masts and their battened sails, torn.
    for (const [u, h, w] of [[-half * 0.25, 24, 8], [half * 0.25, 19, 6], [half * 0.7, 12, 4]]) {
      const mu = Math.round(u);
      for (let k = 6; k <= h; k++) put(mu, 0, k, 'wood_dark');
      for (let k = 10; k <= h - 1; k++) for (let s = -w; s <= w; s++) {
        if (r() < 0.18 + (k < 13 ? 0.25 : 0)) continue;              // holes, the lower edge in rags
        put(mu + 1, s, k, k % 3 === 0 ? 'wood_dark' : 'sail');
      }
    }
    // Lanterns along the rail.
    for (let u = -half + 12; u <= half - 6; u += 8) for (const v of [-1, 1]) put(u, v * (width - 1), 9, 'lantern');
    // Place each piece: bow-down pitch, heel, yaw; the bow half of a broken junk tipped up about the break.
    for (let [u, v, h, m] of parts) {
      if (broken !== null && u > broken) {
        const du = u - broken, a = rad(bowRise);
        [u, h] = [broken + du * Math.cos(a) - h * Math.sin(a) + 2, du * Math.sin(a) + h * Math.cos(a) - 6];
      } else if (broken !== null && u > broken - 2 && r() < 0.5) continue;   // the splintered break
      // roll about u, then pitch about v, then yaw about h.
      let vv = v * Math.cos(rad(roll)) - h * Math.sin(rad(roll)), hh = v * Math.sin(rad(roll)) + h * Math.cos(rad(roll));
      let uu = u * Math.cos(rad(pitch)) + hh * Math.sin(rad(pitch)); hh = -u * Math.sin(rad(pitch)) + hh * Math.cos(rad(pitch));
      const X = uu * Math.cos(rad(yaw)) - vv * Math.sin(rad(yaw)), Z = uu * Math.sin(rad(yaw)) + vv * Math.cos(rad(yaw));
      const wx = Math.round(x0 + X), wy = Math.round(y0 + hh), wz = Math.round(z0 + Z);
      if (wy <= mudY) continue;
      if (!S.get(wx, wy, wz) || m !== 'wood') S.set(wx, wy, wz, m);
      // Mud banked up against the hull where it meets the floor.
      if (wy <= mudY + 2 && m === 'wood_dark') for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]])
        if (!S.get(wx + dx, mudY + 1, wz + dz) && r() < 0.6) S.set(wx + dx, mudY + 1, wz + dz, 'mud_dark');
    }
  }

  /** The Crab King, life-size enough to read (3 blocks tall): crusted shell, red flesh, great claws. */
  function king(x, y, z) {
    S.blob(x, y + 2, z, 1.6, 1.4, 1.2, 'shell', 0, r);
    S.blob(x, y + 3, z + 0.5, 1.4, 1.0, 1.1, 'shell_dark', 0, r);
    S.fill(x - 1, y + 1, z, x - 1, y + 1, z, 'shell_dark'); S.fill(x + 1, y + 1, z, x + 1, y + 1, z, 'shell_dark');
    S.set(x - 1, y, z, 'shell_dark'); S.set(x + 1, y, z, 'shell_dark');
    for (const s of [-1, 1]) {
      S.fill(x + s * 2, y + 1, z - 1, x + s * 3, y + 2, z - 2, 'shell');
      S.set(x + s * 3, y + 1, z - 3, 'shell_dark'); S.set(x + s * 2, y + 1, z - 3, 'shell_dark');
    }
    S.set(x, y + 4, z - 1, 'qi');
  }

  /** A crab, and a crab's nest (a heap of shells and bones). */
  const crab = (x, y, z) => { S.set(x, y + 1, z, 'shell'); S.set(x + 1, y + 1, z, 'shell_dark'); };
  function nest(x, y, z) {
    for (let k = 0; k < 14; k++) S.set(x + Math.round((r() - 0.5) * 4), y + 1 + (k > 10 ? 1 : 0), z + Math.round((r() - 0.5) * 4), r() < 0.35 ? 'bone' : r() < 0.5 ? 'shell_dark' : 'shell');
  }

  // ---------------------------------------------------------------- A: the Sunken Pit
  const A = { cx: 0, cy: 6, cz: 0, rx: 46, ry: 30, rz: 46 };
  function buildA(open) {
    S.clear(); r = S.seeded(11);
    const keep = (x, y, z) => !open || z < 6 && y < A.cy + A.ry - 4 + (z < -30 ? 10 : 0);
    const inside = cave(A.cx, A.cy, A.cz, A.rx, A.ry, A.rz, keep);
    dress(inside, A.cx, A.cz, A.rx, A.rz, A.cy + A.ry, keep);
    // The tunnel down from the sinkhole, through the south wall, a ramp of roots and mud.
    for (let i = 0; i < 40; i++) {
      const z = A.rz - 4 + i, y = Math.round(i * 0.9);
      for (let x = -4; x <= 4; x++) {
        if (keep(x, y, z) || !open) S.set(x, y, z, i % 6 === 0 && Math.abs(x) === 4 ? 'mangrove' : 'swamp_mud');
        if (Math.abs(x) === 4) for (let h = 1; h <= 5; h++) if (!open || keep(x, y + h, z)) S.set(x, y + h, z, rock(x, y + h, z));
      }
    }
    // The junk, stranded at the back, bow down into the mud, heeled.
    junk(0, -4, -24, { len: 62, width: 9, pitch: -7, roll: 9, yaw: 0 });
    // The arena: the open mud in front of it (some 70 blocks across), nests round its edge, crabs, the king.
    for (let k = 0; k < 8; k++) { const a = k / 8 * Math.PI * 2; nest(Math.round(Math.cos(a) * 36), 0, Math.round(Math.sin(a) * 36) + 4); }
    for (let k = 0; k < 14; k++) crab(Math.round((r() - 0.5) * 60), 0, Math.round((r() - 0.3) * 50));
    king(0, 1, -4);
    S.person(0, 1, 30, 'robe_white');
  }
  function buildSurface() {
    S.clear(); r = S.seeded(5);
    // The Jiangnan Waterlands (murimblock:jiangnan_waterlands): flat polders, shallow lotus ponds on mud, willows along
    // the banks, a stone arch bridge. One old pond has fallen in: its middle is a black sinkhole ringed with mud and
    // crab shells, the bridge that crossed it broken off over the drop; the way down to the Crab King.
    const pond = (x, z) => n3(x * 0.16, 2, z * 0.16) > 0.25 || Math.hypot(x, z) < 16;
    S.ground(-80, -80, 80, 80, () => 1, () => 'grass_lush', 'swamp_mud', 2);
    for (let x = -80; x <= 80; x++) for (let z = -80; z <= 80; z++) if (pond(x, z)) {
      S.set(x, 1, z, null); S.set(x, 0, z, 'water_jade');
      if (Math.hypot(x, z) > 18 && r() < 0.07) S.set(x, 1, z, r() < 0.25 ? 'lotus_flower' : 'lotus');
    }
    // The sinkhole: a funnel of mud into the dark.
    for (let x = -15; x <= 15; x++) for (let z = -15; z <= 15; z++) {
      const d = Math.hypot(x, z);
      if (d < 8) { for (let y = -16; y <= 1; y++) S.set(x, y, z, null); S.set(x, -17, z, 'mud_dark'); }
      else if (d < 15) { const y = Math.round(-((15 - d) / 7) * 6); for (let k = y; k <= 1; k++) S.set(x, k, z, null); S.set(x, y, z, d < 11 ? 'mud_dark' : 'swamp_mud'); }
    }
    for (let k = 0; k < 18; k++) { const a = r() * Math.PI * 2, d = 9 + r() * 6; S.set(Math.round(Math.cos(a) * d), Math.round(-((15 - d) / 7) * 6) + 1, Math.round(Math.sin(a) * d), r() < 0.4 ? 'bone' : 'shell'); }
    // The stone arch bridge from the south bank, broken off over the drop.
    for (let z = 30; z >= 4; z--) {
      const arch = Math.round(4 - ((z - 17) / 13) ** 2 * 3);
      if (z < 9 && r() < 0.4) continue;
      for (let x = -2; x <= 2; x++) S.set(x, arch, z, 'stone_brick');
      for (const x of [-2, 2]) if (z % 2 === 0) S.set(x, arch + 1, z, 'stone');
    }
    // A fallen shrine gate on the bank, a crab-shell totem.
    for (const x of [-26, -20]) S.fill(x, 1, 18, x, 8, 18, 'red');
    S.fill(-27, 8, 18, -19, 8, 18, 'wood_dark'); S.fill(-27, 9, 18, -19, 9, 18, 'tile');
    S.blob(18, 4, 14, 1.4, 2.4, 1.4, 'shell', 0, r); S.set(18, 7, 14, 'bone');
    // Willows on the banks, a white-walled cottage far off.
    for (let k = 0; k < 60; k++) {
      const x = Math.round((r() - 0.5) * 150), z = Math.round((r() - 0.5) * 150);
      if (pond(x, z) || Math.hypot(x, z) < 20) continue;
      S.willow(x, 2, z, r);
    }
    S.hall(-55, -50, 3, 6, 1, { wall: 'white', column: 'wood_dark', roofMat: 'tile', roofType: 'gable' });
    S.person(1, 5, 22, 'robe_white');
  }

  // ---------------------------------------------------------------- B: the Junk's Nave
  const B = { cx: 0, cy: 4, cz: 0, rx: 72, ry: 30, rz: 38 };
  function buildB(open) {
    S.clear(); r = S.seeded(22);
    const keep = (x, y, z) => !open || z < 4 && y < B.cy + B.ry - 3;
    const inside = cave(B.cx, B.cy, B.cz, B.rx, B.ry, B.rz, keep, { roughness: 0.08 });
    dress(inside, B.cx, B.cz, B.rx, B.rz, B.cy + B.ry, keep, 110);
    // A channel of swamp water along the north side.
    for (let x = -60; x <= 60; x++) for (let z = -30; z <= -22; z++) if (inside(x, 0, z)) S.set(x, 0, z, 'swamp_water');
    // The junk in the middle, heeled hard to the north, sunk to its waterline: its deck the boss's stage.
    junk(0, -3, -6, { len: 54, width: 8, pitch: 3, roll: -22, yaw: 0 });
    // Mud flats east and west: the arena (some 40 blocks either side), nests, crabs.
    for (const s of [-1, 1]) {
      for (let k = 0; k < 4; k++) nest(s * (38 + k * 6), 0, -10 + k * 8);
      for (let k = 0; k < 8; k++) crab(s * Math.round(32 + r() * 30), 0, Math.round((r() - 0.5) * 50));
    }
    king(4, 4, -4);
    S.person(-38, 1, 12, 'robe_white');
  }

  // ---------------------------------------------------------------- C: the Root Well
  const C = { cx: 0, cy: 8, cz: 0, rx: 44, ry: 34, rz: 44 };
  function buildC(open) {
    S.clear(); r = S.seeded(33);
    const top = C.cy + C.ry;
    const keep = (x, y, z) => (!open || z < 6 && y < top - 6) && Math.hypot(x, z - 4) > 9;   // the shaft in the roof
    const inside = cave(C.cx, C.cy, C.cz, C.rx, C.ry, C.rz, keep);
    dress(inside, C.cx, C.cz, C.rx, C.rz, top, keep);
    // The shaft: mangrove roots hanging down through it, a beam of daylight to the floor, a pool where it lands.
    for (let k = 0; k < 22; k++) {
      const a = r() * Math.PI * 2, d = 4 + r() * 5, x = Math.round(Math.cos(a) * d), z = Math.round(4 + Math.sin(a) * d);
      const len = 8 + Math.floor(r() * 18);
      for (let i = 0; i < len; i++) if (!open || keep(x, top - i, z) || z < 6) S.set(x + (i % 7 === 6 ? 1 : 0), top - i, z, 'mangrove');
    }
    for (let y = 1; y < top; y++) for (let x = -5; x <= 5; x++) for (let z = -1; z <= 9; z++)
      if (Math.hypot(x, z - 4) < 4.5 - y * 0.03 && (x + y + z) % 2 === 0 && !S.get(x, y, z)) S.set(x, y, z, 'beam');
    for (let x = -7; x <= 7; x++) for (let z = -3; z <= 11; z++) if (Math.hypot(x, z - 4) < 6.5) S.set(x, 0, z, 'swamp_water');
    // The junk broken in two: its stern lying on the west, its bow standing up out of the mud to the north-east.
    junk(-24, -2, -10, { len: 50, width: 7, pitch: -4, roll: 16, yaw: 30, broken: 6, bowRise: 64 });
    // A ring of nests, the king beside the pool, a challenger coming in.
    for (let k = 0; k < 10; k++) { const a = k / 10 * Math.PI * 2; nest(Math.round(Math.cos(a) * 34), 0, Math.round(Math.sin(a) * 34)); }
    for (let k = 0; k < 12; k++) crab(Math.round((r() - 0.5) * 60), 0, Math.round((r() - 0.5) * 60));
    king(6, 1, 8);
    S.person(0, 1, 34, 'robe_white');
  }

  // ---------------------------------------------------------------- views
  const DARK = '#1C221E', SKY = '#B9C7B4';
  const v = (position, target, fov = 55) => ({ unclamped: true, position, target, fov });
  return {
    a_open() { buildA(true); return { view: v([10, 48, 92], [0, 4, -12], 55), sky: DARK, fog: [220, 520] }; },
    a_inside() { buildA(false); return { view: v([3, 14, 40], [0, 6, -24], 70), sky: DARK, fog: [90, 160] }; },
    a_surface() { buildSurface(); return { view: v([34, 30, 62], [0, -2, 4], 52), sky: '#AEC3CF', fog: [140, 320] }; },
    b_open() { buildB(true); return { view: v([12, 52, 92], [0, 2, -8], 60), sky: DARK, fog: [240, 560] }; },
    b_inside() { buildB(false); return { view: v([-56, 12, 20], [10, 8, -8], 70), sky: DARK, fog: [100, 180] }; },
    c_open() { buildC(true); return { view: v([0, 76, 124], [0, 8, -4], 52), sky: DARK, fog: [220, 520] }; },
    c_inside() { buildC(false); return { view: v([4, 10, 38], [-6, 18, -12], 74), sky: DARK, fog: [90, 170] }; },
  };
})();
true;
