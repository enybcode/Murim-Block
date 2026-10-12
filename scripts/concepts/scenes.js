// The seven concept scenes (one per Murim biome), built with ST (st-runtime.js). Each returns its camera views.
// Sources and dimensions: docs/biomes (architecture research), 1 voxel = 1 block = 1 m.
window.SCENES = (() => {
  const S = ST;

  /** 1. Jade karst forest: Zhuang/Yao stilt houses by the Li River, a Yulong-style hump bridge, karst towers. */
  function karst() {
    S.clear();
    const r = S.seeded(11);
    S.ground(-70, -60, 70, 60, (x, z) => (Math.abs(z - 6) < 7 ? 0 : 2 + Math.sin(x * 0.07) * 0.6), 'grass_lush', 'dirt');
    S.water(-70, 0, 70, 12, 1, 'water_jade');
    for (const [x, z, rr, h] of [[-48, -40, 9, 46], [-18, -50, 8, 58], [14, -44, 10, 40], [44, -48, 9, 54], [62, -22, 7, 38], [-62, -16, 7, 34], [74, 34, 8, 44], [-70, 38, 9, 50]]) {
      S.peak(x, z, 2, rr, h, r() < 0.5 ? 'limestone' : 'limestone', 'leaves_osm', r);
    }
    // Hump-backed single-arch stone bridge over the river (Yulong Bridge, scaled).
    for (let z = -2; z <= 14; z++) {
      const t = (z - 6) / 8;
      const top = Math.round(2 + 7 * (1 - t * t));
      for (let x = -2; x <= 2; x++) {
        for (let y = 2; y <= top; y++) {
          const inArch = Math.hypot(z - 6, (y - 1) * 1.1) < 5.2;
          if (!inArch) S.set(x, y, z, y === top ? 'stone_brick' : 'stone_brick');
        }
      }
      S.set(-2, top + 1, z, 'stone_brick'); S.set(2, top + 1, z, 'stone_brick');
    }
    // Stilt houses on the far bank.
    for (const [hx, hz] of [[-30, -16], [-14, -18], [12, -17]]) {
      const y0 = 3;
      for (const [dx, dz] of [[0, 0], [10, 0], [0, 8], [10, 8], [5, 0], [5, 8]]) S.fill(hx + dx, y0, hz + dz, hx + dx, y0 + 2, hz + dz, 'wood_dark');
      S.fill(hx - 1, y0 + 3, hz - 1, hx + 11, y0 + 3, hz + 9, 'wood');           // floor
      S.walls(hx, y0 + 4, hz, hx + 10, y0 + 7, hz + 8, 'wood_light');
      S.fill(hx + 1, y0 + 4, hz + 9, hx + 9, y0 + 4, hz + 10, 'wood');           // front veranda
      for (let x = hx + 1; x <= hx + 9; x += 2) S.set(x, y0 + 5, hz + 10, 'wood_dark');
      S.set(hx + 3, y0 + 5, hz + 8, 'wood_dark'); S.set(hx + 7, y0 + 5, hz + 8, 'wood_dark');
      S.roof(hx, hz, hx + 10, hz + 8, y0 + 8, { type: 'gable', mat: 'tile' });
      S.set(hx + 5, y0 + 6, hz + 10, 'lantern');
    }
    // Banyan at the bridge foot, osmanthus and bamboo clumps along the bank.
    S.tree(-8, 2, 16, { trunk: 'bark', leaves: 'leaves', h: 6, r: 6, flat: true, rnd: r });
    for (let k = 0; k < 6; k++) S.fill(-12 + k * 2, 3, 16 + (k % 2), -12 + k * 2, 7, 16 + (k % 2), 'bark');
    for (const x of [-40, -24, 22, 34]) S.tree(x, 2, 18 + Math.round(r() * 6), { leaves: 'leaves_osm', h: 4, r: 3, rnd: r });
    for (const x of [-52, -46, 26, 30, 52]) for (let k = 0; k < 6; k++) S.bamboo(x + Math.round(r() * 4), 2, -4 + Math.round(r() * 3), 8 + Math.round(r() * 5), r);
    // Bamboo raft on the river.
    S.fill(18, 1, 6, 26, 1, 8, 'bamboo');
    return [{ name: 'a', position: [40, 34, 70], target: [-6, 8, -8], fov: 55 }, { name: 'b', position: [-30, 14, 40], target: [-12, 8, -12], fov: 58 }];
  }

  /** 2. Cloud sea peaks: cliff plank road to a Taoist shrine on a granite spire, a lookout pavilion, cliff pines. */
  function peaks() {
    S.clear();
    const r = S.seeded(22);
    // Sea of clouds: a soft white floor far below.
    S.fill(-80, 0, -80, 80, 0, 80, 'snow');
    for (const [x, z, rr, h] of [[0, 0, 9, 70], [-30, -20, 6, 58], [28, -26, 7, 64], [-44, 18, 5, 46], [40, 16, 6, 52], [12, -50, 6, 60], [-18, -52, 5, 50]]) {
      for (let y = 1; y <= h; y++) {
        const shrink = Math.floor((y / h) * 5) * 0.12;
        const rr2 = rr * (1 - shrink) + (y % 7 === 0 ? 0.6 : 0);
        for (let dx = -rr - 1; dx <= rr + 1; dx++) for (let dz = -rr - 1; dz <= rr + 1; dz++)
          if (Math.hypot(dx, dz) <= rr2 + (r() - 0.5)) S.set(x + dx, y, z + dz, 'granite');
      }
    }
    // Shrine terrace on the main spire top (y 70).
    const top = 70;
    S.fill(-6, top + 1, -5, 6, top + 1, 5, 'stone_brick');
    S.hall(-4, -3, 3, 6, top + 2, { bay: 3, wall: 'red_wall', column: 'red', roofMat: 'tile_green', platform: 1, storey: 4 });
    S.fill(0, top + 2, 6, 0, top + 3, 6, 'cobble'); S.set(0, top + 4, 6, 'fire');   // incense burner
    // Plank road clinging to the spire face, chain rail (dark), climbing in a spiral.
    for (let k = 0; k < 120; k++) {
      const a = k * 0.09, y = 18 + Math.round(k * 0.43);
      if (y > top) break;
      const rr = 9 * (1 - Math.floor((y / 70) * 5) * 0.12) + 1.6;
      const x = Math.round(Math.cos(a) * rr), z = Math.round(Math.sin(a) * rr);
      S.set(x, y, z, 'wood'); S.set(Math.round(Math.cos(a) * (rr + 1)), y + 1, Math.round(Math.sin(a) * (rr + 1)), 'ridge');
    }
    // Lookout pavilion on the second spire, cliff pines growing out sideways.
    S.pavilion(-30, -20, 59, 2, { roofMat: 'tile' });
    for (const [x, y, z, dx] of [[9, 56, 3, 1], [-8, 46, -4, -1], [27, 50, -20, 1], [-36, 40, 18, -1], [6, 64, -8, 1]]) {
      for (let k = 0; k < 3; k++) S.set(x + dx * k, y + (k > 1 ? 1 : 0), z, 'bark_red');
      S.blob(x + dx * 3, y + 2, z, 3, 0, 2, 'leaves_dark', 0.15, r);
      S.blob(x + dx * 2, y + 3, z, 2, 0, 2, 'leaves_dark', 0.2, r);
    }
    return [{ name: 'a', position: [52, 84, 62], target: [0, 62, 0], fov: 50 }, { name: 'b', position: [-20, 74, 34], target: [0, 72, -2], fov: 52 }];
  }

  /** 3. Loess plateau: cliffside yaodong cave terraces, and a sunken courtyard (dikengyuan) on the tableland. */
  function loess() {
    S.clear();
    const r = S.seeded(33);
    // Tableland at y 30 with a gully front stepping down in terraces to the valley at y 6.
    S.ground(-60, -50, 60, 50, (x, z) => (z < -12 ? 30 : z < -4 ? 22 : z < 4 ? 14 : 6), (x, z, h) => (h === 6 ? 'grass_dry' : 'grass_dry'), 'loess', 26);
    for (let x = -60; x <= 60; x++) for (let y = 7; y <= 30; y++) for (let z = -12; z <= 4; z++) {
      const tier = z < -4 ? 22 : z < 4 ? 14 : 6;
      if (y <= tier && S.get(x, y, z) && (y % 9 === 0)) S.set(x, y, z, 'red_loess');
    }
    // Two tiers of cave dwellings cut into the walls, arched doors with lattice upper halves.
    for (const [tierY, wallZ] of [[14, 3], [22, -5]]) {
      for (let cx = -40; cx <= 40; cx += 8) {
        if (r() < 0.2) continue;
        for (let y = tierY + 1; y <= tierY + 3; y++) for (let x = cx - 1; x <= cx + 1; x++) S.set(x, y, wallZ, null);
        S.set(cx, tierY + 4, wallZ, null);
        for (let x = cx - 2; x <= cx + 2; x++) for (let y = tierY + 1; y <= tierY + 5; y++) if (!(Math.abs(x - cx) <= 1 && y <= tierY + 3) && !(x === cx && y === tierY + 4)) S.set(x, y, wallZ, 'mud_brick');
        S.fill(cx - 1, tierY + 1, wallZ - 1, cx + 1, tierY + 2, wallZ - 1, 'wood_dark');
        S.fill(cx - 1, tierY + 3, wallZ - 1, cx + 1, tierY + 3, wallZ - 1, 'wood');
        S.set(cx, tierY + 4, wallZ - 1, 'wood');
        S.set(cx + 2, tierY + 2, wallZ + 1, 'banner');
      }
      for (let x = -44; x <= 44; x++) S.set(x, tierY + 1, wallZ + 2, x % 6 === 0 ? 'mud_brick' : null);
    }
    // Sunken courtyard on the tableland: a 12x12 pit 6 deep with caves in its walls and a jujube in the middle.
    const px = 18, pz = -32;
    S.fill(px - 6, 25, pz - 6, px + 6, 30, pz + 6, null);
    S.fill(px - 6, 24, pz - 6, px + 6, 24, pz + 6, 'dirt');
    for (const [dx, dz] of [[-6, -2], [-6, 2], [6, -2], [6, 2], [-2, -6], [2, -6], [-2, 6], [2, 6]]) {
      const ox = Math.sign(dx), oz = Math.sign(dz);
      for (let k = 0; k <= 3; k++) S.fill(px + dx + ox * k, 25, pz + dz + oz * k, px + dx + ox * k, 27, pz + dz + oz * k, null);
      S.set(px + dx, 28, pz + dz, 'mud_brick');
    }
    S.tree(px, 24, pz, { trunk: 'bark', leaves: 'leaves', h: 4, r: 2, rnd: r });
    for (let x = px - 7; x <= px + 7; x++) for (const z of [pz - 7, pz + 7]) S.set(x, 31, z, 'mud_brick');
    for (let z = pz - 7; z <= pz + 7; z++) for (const x of [px - 7, px + 7]) S.set(x, 31, z, 'mud_brick');
    // Millet fields and wild apricot trees on the plateau.
    S.fill(-40, 30, -45, -10, 30, -20, 'field');
    for (const [x, z] of [[-6, -40], [34, -14], [-30, -16]]) S.tree(x, 30, z, { leaves: 'leaves_pink', h: 3, r: 2, rnd: r });
    return [{ name: 'a', position: [10, 42, 64], target: [0, 18, -8], fov: 55 }, { name: 'b', position: [44, 56, -6], target: [18, 26, -32], fov: 50 }];
  }

  /** 4. Shu bamboo sea: a Qingcheng-style Taoist temple stepping up a slope, covered stair, bamboo cable bridge. */
  function bamboo() {
    S.clear();
    const r = S.seeded(44);
    S.ground(-60, -60, 60, 60, (x, z) => 4 + Math.max(0, -z) * 0.55 + Math.sin(x * 0.1) * 1.5, 'grass_lush', 'dirt', 40);
    // Gorge with a stream, crossed by a sagging bamboo bridge.
    for (let x = -60; x <= 60; x++) for (let z = 20; z <= 30; z++) for (let y = -2; y <= 8; y++) S.set(x, y, z, null);
    S.water(-60, 20, 60, 30, -3);
    for (let z = 18; z <= 32; z++) {
      const sag = Math.round(3 * Math.sin(((z - 18) / 14) * Math.PI));
      for (let x = -32; x <= -30; x++) S.set(x, 8 - sag, z, 'bamboo');
      S.set(-33, 9 - sag, z, 'ridge'); S.set(-29, 9 - sag, z, 'ridge');
    }
    // Three halls stepping up the slope along the axis, linked by a covered stair.
    const halls = [[0, 6, 3, 8], [-1, -6, 3, 8], [0, -20, 5, 10]];
    let prevTop = null;
    for (const [x0, z0, bays, depth] of halls) {
      const gy = Math.round(4 + Math.max(0, -(z0 + depth / 2)) * 0.55) + 1;
      S.hall(x0 - bays * 2, z0, bays, depth, gy, { bay: 4, wall: 'white', column: 'wood_dark', roofMat: 'tile', platform: 2, storey: 5 });
      if (prevTop) for (let z = z0 + depth + 3; z <= prevTop.z; z++) {
        const y = Math.round(gy + (z - (z0 + depth + 3)) * -(gy - prevTop.y) / Math.max(1, prevTop.z - (z0 + depth + 3)));
        S.fill(-1, y, z, 1, y, z, 'stone_brick'); S.set(-2, y + 4, z, 'tile'); S.set(2, y + 4, z, 'tile'); S.fill(-1, y + 5, z, 1, y + 5, z, 'tile');
      }
      prevTop = { y: gy, z: z0 - 1 };
    }
    // Giant bamboo everywhere else.
    for (let k = 0; k < 220; k++) {
      const x = Math.round((r() - 0.5) * 116), z = Math.round((r() - 0.5) * 116);
      if (Math.abs(x) < 16 && z > -34 && z < 20) continue;
      if (z >= 18 && z <= 32) continue;
      const gy = Math.round(4 + Math.max(0, -z) * 0.55 + Math.sin(x * 0.1) * 1.5);
      S.bamboo(x, gy, z, 12 + Math.round(r() * 9), r);
    }
    return [{ name: 'a', position: [40, 40, 56], target: [0, 14, -6], fov: 55 }, { name: 'b', position: [-58, 22, 40], target: [-20, 10, 10], fov: 55 }];
  }

  /** 5. Jiangnan water country: canal street of whitewashed houses with horse-head gables, arched bridge, lotus pond, willows. */
  function jiangnan() {
    S.clear();
    const r = S.seeded(55);
    S.ground(-60, -50, 60, 50, () => 2, 'grass', 'dirt');
    S.fill(-60, -1, -3, 60, 2, 3, null);
    S.water(-60, -3, 60, 3, 0);
    S.fill(-60, 0, -4, 60, 2, -4, 'stone_brick'); S.fill(-60, 0, 4, 60, 2, 4, 'stone_brick');
    // Houses on both banks: white walls, dark timber, overhanging upper floor, stepped horse-head firewalls.
    for (const side of [-1, 1]) {
      for (let x = -52; x <= 44; x += 9) {
        const z0 = side < 0 ? -13 : 6, z1 = side < 0 ? -6 : 13;
        S.walls(x, 3, z0, x + 7, 8, z1, 'white');
        S.fill(x, 9, z0, x + 7, 9, z1, 'wood_dark');
        for (const gx of [x, x + 7]) { // horse-head gables stepping up to the ridge
          for (let s = 0; s < 3; s++) S.fill(gx, 10 + s, z0 + s * 1, gx, 10 + s, z1 - s * 1, 'white');
          for (let s = 0; s < 3; s++) { S.set(gx, 11 + s, z0 + s, 'tile'); S.set(gx, 11 + s, z1 - s, 'tile'); }
          S.fill(gx, 13, z0 + 3, gx, 13, z1 - 3, 'tile');
        }
        S.roof(x + 1, z0, x + 6, z1, 10, { type: 'gable', mat: 'tile', overhang: 0, lift: false });
        const front = side < 0 ? z1 : z0;
        S.fill(x + 2, 3, front, x + 5, 5, front, 'wood_dark');                     // shopfront boards
        for (let wx = x + 1; wx <= x + 6; wx += 2) S.set(wx, 7, front, 'wood');     // lattice windows
        S.set(x + 4, 6, front + side * -1, 'lantern');
      }
    }
    // Hump-backed bridge over the canal, a flat slab bridge further along.
    for (let z = -6; z <= 6; z++) { const top = Math.round(3 + 4 * (1 - (z / 6) ** 2)); S.fill(-2, 3, z, 2, top, z, 'stone_brick'); if (Math.abs(z) < 3) S.fill(-1, 1, z, 1, top - 2, z, null); }
    S.fill(28, 3, -4, 31, 3, 4, 'stone_brick');
    // Lotus pond with a pavilion behind the houses, willows on the banks.
    S.fill(-30, 1, 22, -6, 2, 40, null); S.water(-30, 22, -6, 40, 1);
    for (let k = 0; k < 60; k++) { const x = -29 + Math.round(r() * 22), z = 23 + Math.round(r() * 16); S.set(x, 2, z, r() < 0.25 ? 'lotus_flower' : 'lotus'); }
    S.pavilion(-18, 31, 2, 2, { roofMat: 'tile' });
    for (const [x, z] of [[-36, 20], [0, 22], [-40, 34], [36, -20], [-40, -22], [18, 24]]) S.willow(x, 2, z, r);
    S.fill(10, 0, -1, 14, 0, 1, 'wood'); // a moored boat
    return [{ name: 'a', position: [52, 16, 2], target: [-8, 5, 0], fov: 55 }, { name: 'b', position: [-34, 26, 52], target: [-14, 4, 24], fov: 55 }];
  }

  /** 6. Peach blossom valley: hidden archaic farm village among peach groves, ringed by sandstone pillars, a cave entrance. */
  function peach() {
    S.clear();
    const r = S.seeded(66);
    S.ground(-64, -64, 64, 64, (x, z) => 3 + Math.sin(x * 0.05) + Math.cos(z * 0.06), 'grass_lush', 'dirt');
    // Ring of quartz-sandstone pillars and a cliff with the cave entrance of the story.
    for (let k = 0; k < 20; k++) {
      const a = (k / 20) * Math.PI * 2 + r() * 0.2, d = 50 + r() * 10;
      const x = Math.round(Math.cos(a) * d), z = Math.round(Math.sin(a) * d);
      const rr = 3 + Math.round(r() * 3), h = 34 + Math.round(r() * 30);
      for (let y = 3; y <= h; y++) for (let dx = -rr; dx <= rr; dx++) for (let dz = -rr; dz <= rr; dz++)
        if (Math.max(Math.abs(dx), Math.abs(dz)) * 0.8 + Math.hypot(dx, dz) * 0.2 <= rr) S.set(x + dx, y, z + dz, y % 5 === 0 ? 'sandstone_layer' : 'sandstone');
      S.blob(x, h + 1, z, rr - 1, 0, rr - 1, 'leaves_dark', 0.3, r);
    }
    S.fill(-24, 3, -52, 24, 30, -40, 'sandstone');
    S.fill(-1, 4, -52, 1, 6, -40, null); // the narrow cave passage
    // Stream from the cave through the village.
    for (let z = -40; z <= 64; z++) { const x = Math.round(Math.sin(z * 0.08) * 6); S.fill(x - 1, 3, z, x + 1, 4, z, null); S.water(x - 1, z, x + 1, z, 2); }
    // Thatched rammed-earth houses, wattle fences, a well, a water wheel.
    for (const [x, z] of [[-26, -14], [-16, 8], [14, -18], [22, 6], [-28, 22], [10, 26]]) {
      S.walls(x, 4, z, x + 8, 7, z + 5, 'rammed');
      S.fill(x + 3, 4, z + 5, x + 4, 6, z + 5, 'wood_dark');
      S.roof(x, z, x + 8, z + 5, 8, { type: 'gable', mat: 'thatch', lift: false });
      for (let fx = x - 2; fx <= x + 10; fx++) S.set(fx, 4, z + 8, fx % 2 === 0 ? 'bamboo' : null);
    }
    S.walls(-2, 4, -2, 2, 5, 2, 'cobble'); S.water(-1, -1, 1, 1, 4);
    for (let a = 0; a < 16; a++) S.set(Math.round(Math.sin(30 * 0.08) * 6) + 3, 6 + Math.round(Math.sin(a / 16 * Math.PI * 2) * 3), 30 + Math.round(Math.cos(a / 16 * Math.PI * 2) * 3), 'wood');
    // Peach trees everywhere, petals on the ground, rapeseed fields.
    for (let k = 0; k < 46; k++) {
      const x = Math.round((r() - 0.5) * 90), z = Math.round((r() - 0.5) * 90);
      if (S.get(x, 5, z)) continue;
      S.tree(x, 4, z, { trunk: 'bark_red', leaves: 'leaves_pink', h: 3, r: 2 + Math.round(r()), rnd: r });
    }
    S.fill(30, 4, -36, 44, 4, -24, 'gold');
    return [{ name: 'a', position: [40, 30, 52], target: [0, 6, -8], fov: 58 }, { name: 'b', position: [0, 14, -36], target: [0, 6, 10], fov: 60 }];
  }

  /** 7. Hexi frontier desert: Jiayuguan-style pass fortress with gate tower, rammed-earth Great Wall, beacon towers, poplar oasis. */
  function desert() {
    S.clear();
    const r = S.seeded(77);
    S.ground(-80, -70, 80, 70, (x, z) => 2 + Math.sin(x * 0.04) * 0.8, (x, z) => (Math.sin(x * 0.11) + Math.cos(z * 0.09 + x * 0.03) > 1.1 ? 'sand' : 'gravel'), 'sand');
    // Fortress: square rammed-earth enclosure with layered walls and crenels.
    const R = 22;
    for (let y = 3; y <= 12; y++) for (let x = -R; x <= R; x++) for (let z = -R; z <= R; z++) {
      const onWall = (Math.abs(x) >= R - 2 || Math.abs(z) >= R - 2);
      if (!onWall) continue;
      if (Math.abs(x) <= 2 && z >= R - 2 && y <= 7) continue; // south gate passage
      S.set(x, y, z, y % 3 === 0 ? 'sandstone_layer' : 'rammed');
    }
    for (let x = -R; x <= R; x += 2) { S.set(x, 13, -R, 'mud_brick'); S.set(x, 13, R, 'mud_brick'); }
    for (let z = -R; z <= R; z += 2) { S.set(-R, 13, z, 'mud_brick'); S.set(R, 13, z, 'mud_brick'); }
    // Three-storey gate tower above the south gate (red walls, grey double roof).
    S.fill(-6, 13, R - 4, 6, 13, R, 'stone_brick');
    S.hall(-5, R - 3, 3, 3, 14, { bay: 3, wall: 'red_wall', column: 'red', roofMat: 'tile', platform: 1, storey: 4, base: 'stone_brick' });
    S.hall(-4, R - 3, 2, 3, 22, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile', platform: 0, storey: 3, base: 'stone_brick', doors: false });
    // Corner turrets and inner yard buildings with flat roofs.
    for (const [x, z] of [[-R, -R], [R, -R], [-R, R], [R, R]]) { S.fill(x - 2, 13, z - 2, x + 2, 16, z + 2, 'rammed'); S.roof(x - 2, z - 2, x + 2, z + 2, 17, { type: 'pyramid', mat: 'tile', overhang: 0 }); }
    S.fill(-14, 3, -14, -4, 6, -6, 'mud_brick'); S.fill(4, 3, -14, 14, 6, -6, 'mud_brick');
    // The Great Wall running off to the west and east, and a chain of beacon towers.
    for (let x = -80; x <= 80; x++) { if (Math.abs(x) < R) continue; const z = Math.round(Math.sin(x * 0.03) * 3); S.fill(x, 3, z - 1, x, 9, z + 1, x % 4 === 0 ? 'sandstone_layer' : 'rammed'); }
    for (const [x, z] of [[-60, -40], [-30, -60], [40, -56], [70, -30]]) {
      for (let y = 3; y <= 12; y++) { const w = Math.round(4 - (y - 3) / 4); S.fill(x - w, y, z - w, x + w, y, z + w, y % 3 === 0 ? 'hay' : 'rammed'); }
      S.set(x, 13, z, 'fire');
    }
    // Poplar oasis with a pool, tamarisk, yardangs on the horizon.
    S.fill(-50, 2, 30, -36, 2, 42, null); S.water(-50, 30, -36, 42, 1);
    for (const [x, z] of [[-54, 28], [-46, 46], [-32, 34], [-56, 40]]) S.tree(x, 2, z, { trunk: 'bark', leaves: 'leaves_gold', h: 5, r: 3, rnd: r });
    for (const [x, z, len] of [[50, 40, 26], [64, 52, 18]]) for (let i = 0; i < len; i++) { const h = Math.round(8 * (1 - i / len)); S.fill(x - i, 3, z - 2, x - i, 3 + h, z + 2, i % 3 === 0 ? 'sandstone_layer' : 'red_loess'); }
    return [{ name: 'a', position: [46, 30, 70], target: [0, 10, 4], fov: 55 }, { name: 'b', position: [-70, 40, -20], target: [0, 10, 0], fov: 55 }];
  }

  return { karst, peaks, loess, bamboo, jiangnan, peach, desert };
})();
true;
