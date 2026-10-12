// The four structure proposals (docs/structures), built with ST (st-runtime.js) after their plans.
window.PROPOSALS = (() => {
  const S = ST;

  /** 1. Cloud Cliff Sanctuary: Taoist sect seat on a granite spire, spiral plank road, sea of clouds. */
  function sanctuary() {
    S.clear();
    const r = S.seeded(101);
    const TOP = 80;
    // Sea of clouds: layered white puffs far below.
    for (let k = 0; k < 900; k++) {
      const x = Math.round((r() - 0.5) * 220), z = Math.round((r() - 0.5) * 220), y = 8 + Math.round(r() * 5);
      S.blob(x, y, z, 3 + Math.round(r() * 4), 1, 3 + Math.round(r() * 4), 'cloud', 0.3, r);
    }
    // Main spire: wide flat summit, jointed flanks narrowing downward a little.
    for (let y = 0; y <= TOP; y++) {
      const rr = 13 + Math.floor((y / TOP) * 4) + (y % 7 === 0 ? 1 : 0);
      for (let dx = -rr - 1; dx <= rr + 1; dx++) for (let dz = -rr - 1; dz <= rr + 1; dz++)
        if (Math.hypot(dx, dz) <= rr + (r() - 0.5) * 1.2) S.set(dx, y, dz, 'granite');
    }
    // Neighbour spires with cliff pines.
    for (const [x, z, rr, h] of [[-46, -30, 7, 66], [44, -36, 8, 72], [-40, 34, 6, 54], [52, 30, 6, 60], [6, -64, 9, 76], [-70, -2, 5, 50]]) {
      for (let y = 0; y <= h; y++) for (let dx = -rr; dx <= rr; dx++) for (let dz = -rr; dz <= rr; dz++)
        if (Math.hypot(dx, dz) <= rr * (1 - 0.3 * y / h) + (r() - 0.5)) S.set(x + dx, y, z + dz, 'granite');
      S.pine(x + rr, h - 6, z, 4, r);
    }
    // Summit: paved court, paifang gate on the south, main hall north (green tiles), towers, side halls.
    S.fill(-15, TOP, -15, 15, TOP, 15, 'stone_brick');
    S.hall(-10, -13, 5, 9, TOP + 1, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile_green', platform: 2, storey: 6 });
    S.hall(-15, -2, 2, 7, TOP + 1, { bay: 3, wall: 'white', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 4 }); // dormitory
    S.hall(9, -2, 2, 7, TOP + 1, { bay: 3, wall: 'white', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 4 });  // library
    for (const x of [-13, 13]) { // bell and drum towers
      S.fill(x - 2, TOP + 1, 9, x + 2, TOP + 5, 13, 'red_wall');
      S.roof(x - 2, 9, x + 2, 13, TOP + 6, { type: 'pyramid', mat: 'tile_green' });
    }
    // Paifang: three openings, roofed tops.
    for (const x of [-6, -2, 2, 6]) S.fill(x, TOP + 1, 15, x, TOP + 7, 15, 'red');
    S.fill(-7, TOP + 8, 15, 7, TOP + 8, 15, 'wood_dark');
    S.roof(-7, 15, 7, 15, TOP + 9, { type: 'gable', mat: 'tile_green', overhang: 1 });
    S.fill(0, TOP + 1, 3, 0, TOP + 2, 3, 'cobble'); S.set(0, TOP + 3, 3, 'fire'); S.fill(0, TOP + 4, 3, 0, TOP + 9, 3, 'smoke');
    S.pine(-12, TOP, -16, 5, r); S.pine(12, TOP, 18, 4, r);
    // Disciples in the court, the master on the hall steps.
    for (const [x, z] of [[-4, 6], [-1, 8], [3, 7], [5, 5], [-6, 9]]) S.person(x, TOP, z, 'robe_white', 'hair');
    S.person(0, TOP + 2, -2, 'robe_blue', 'gold');
    // Spiral plank road with chain rail and a rest pavilion halfway.
    for (let k = 0; k < 260; k++) {
      const a = k * 0.07, y = 6 + Math.round(k * 0.29);
      if (y > TOP) break;
      const rr = 13 + Math.floor((y / TOP) * 4) + 1.6;
      const x = Math.round(Math.cos(a) * rr), z = Math.round(Math.sin(a) * rr);
      S.set(x, y, z, 'wood'); S.set(Math.round(Math.cos(a) * (rr + 1)), y + 1, Math.round(Math.sin(a) * (rr + 1)), 'ridge');
      if (k === 130) { S.fill(x - 2, y, z - 2, x + 2, y, z + 2, 'wood'); S.pavilion(x, z, y, 1, { roofMat: 'tile' }); }
    }
    S.person(Math.round(Math.cos(4.2) * 16), 6 + Math.round(60 * 0.29), Math.round(Math.sin(4.2) * 16), 'robe_red');
    return { position: [58, 104, 74], target: [0, 76, 0], fov: 50 };
  }

  /** 2. Jade River stilt village: houses on stilts, quays, hump bridge, banyan, terraces, the crab man's cave. */
  function village() {
    S.clear();
    const r = S.seeded(202);
    S.ground(-80, -60, 80, 60, (x, z) => (Math.abs(z) < 8 ? -2 : z > 0 ? 2 + Math.min(12, Math.max(0, (z - 22) * 0.5)) : 2), 'grass_lush', 'dirt', 10);
    S.water(-80, -7, 80, 7, 0, 'water_jade');
    // Rice terraces stepping up on the south bank.
    for (let step = 0; step < 6; step++) S.fill(-70, 3 + step * 2, 24 + step * 4, -20, 3 + step * 2, 27 + step * 4, 'rice');
    // Karst towers behind.
    for (const [x, z, rr, h] of [[-60, -44, 9, 52], [-24, -54, 8, 62], [18, -48, 10, 46], [56, -42, 9, 58], [70, 30, 8, 44]]) S.peak(x, z, 2, rr, h, 'limestone', 'leaves_osm', r);
    // Stone quays on both banks.
    S.fill(-80, 1, -9, 80, 2, -8, 'stone_brick'); S.fill(-80, 1, 8, 80, 2, 9, 'stone_brick');
    // Stilt houses on the north bank, balconies over the water; the herbalist's is wider with drying racks.
    const houses = [[-50, -22, 'Maison'], [-34, -22, 'Herbo'], [-8, -22, 'Maison'], [10, -22, 'Maison'], [30, -22, 'Maison']];
    for (const [hx, hz, kind] of houses) {
      const w = kind === 'Herbo' ? 13 : 10;
      for (const [dx, dz] of [[0, 0], [w, 0], [0, 8], [w, 8], [w >> 1, 0], [w >> 1, 8], [0, 12], [w, 12]]) S.fill(hx + dx, -1, hz + dz, hx + dx, 4, hz + dz, 'wood_dark');
      S.fill(hx - 1, 5, hz - 1, hx + w + 1, 5, hz + 13, 'wood');
      S.walls(hx, 6, hz, hx + w, 9, hz + 8, 'wood_light');
      for (let x = hx; x <= hx + w; x += 2) S.set(x, 6, hz + 13, 'wood_dark');
      S.roof(hx, hz, hx + w, hz + 8, 10, { type: 'gable', mat: 'tile' });
      S.set(hx + (w >> 1), 8, hz + 9, 'lantern');
      if (kind === 'Herbo') for (let x = hx + 1; x <= hx + w - 1; x += 2) { S.set(x, 6, hz + 11, 'bamboo'); S.set(x, 7, hz + 11, 'leaves_light'); }
      S.person(hx + 3, 5, hz + 11, kind === 'Herbo' ? 'robe_white' : 'robe_blue');
    }
    // Hump bridge.
    for (let z = -10; z <= 10; z++) {
      const top = Math.round(2 + 7 * (1 - (z / 10) ** 2));
      for (let x = -2; x <= 2; x++) for (let y = 0; y <= top; y++) if (Math.hypot(z, (y + 1) * 1.15) > 6.5) S.set(x, y, z, 'stone_brick');
      S.set(-2, top + 1, z, 'stone_brick'); S.set(2, top + 1, z, 'stone_brick');
    }
    S.person(0, 9, 0, 'robe_red');
    // Giant banyan on the south quay with aerial roots, bamboo rafts with a cormorant fisherman.
    S.tree(-12, 2, 16, { trunk: 'bark', leaves: 'leaves', h: 7, r: 7, flat: true, rnd: r });
    for (let k = 0; k < 10; k++) { const x = -18 + Math.round(r() * 12), z = 12 + Math.round(r() * 8); S.fill(x, 3, z, x, 8, z, 'bark'); }
    for (const x of [20, 44]) { S.fill(x, 0, -2, x + 7, 0, 1, 'bamboo'); S.person(x + 3, 0, 0, 'robe_black'); S.set(x + 5, 1, -1, 'robe_black'); }
    // The crab man's cave in a karst tower at the river's east end.
    S.peak(66, -16, 0, 9, 40, 'limestone', 'leaves_osm', r);
    S.fill(58, 1, -18, 70, 7, -14, null);
    S.fill(57, 0, -18, 70, 0, -12, 'water_jade');
    S.fill(61, 1, -16, 61, 3, -16, 'teal'); S.set(61, 4, -16, 'teal'); S.fill(62, 3, -16, 63, 3, -16, 'teal');
    S.fill(58, 1, -13, 59, 1, -13, 'teal'); S.fill(64, 1, -13, 65, 1, -13, 'teal');
    S.set(60, 5, -14, 'lantern');
    // Osmanthus and willows along the banks.
    for (const x of [-70, -42, -22, 24, 50]) S.tree(x, 2, -30 - Math.round(r() * 4), { leaves: 'leaves_osm', h: 4, r: 3, rnd: r });
    for (const x of [-60, 36, 58]) S.willow(x, 2, 12, r);
    return { position: [52, 36, 44], target: [8, 4, -12], fov: 60 };
  }

  /** 3. Yellow River bandit lair: yaodong caves in three cliff tiers, sunken courtyard of the chief on the plateau. */
  function lair() {
    S.clear();
    const r = S.seeded(303);
    S.ground(-70, -70, 70, 50, (x, z) => (z < -14 ? 32 : z < -6 ? 24 : z < 2 ? 16 : z < 10 ? 8 : 6), 'grass_dry', 'loess', 30);
    for (let x = -70; x <= 70; x++) for (let z = -14; z <= 10; z++) for (let y = 7; y <= 32; y++) if (S.get(x, y, z) === 'loess' && y % 9 === 0) S.set(x, y, z, 'red_loess');
    // Three tiers of arched cave dwellings.
    for (const [tierY, wallZ, robe] of [[8, 9, 'robe_red'], [16, 1, 'robe_black'], [24, -7, 'robe_black']]) {
      for (let cx = -42; cx <= 42; cx += 7) {
        if (r() < 0.12) continue;
        for (let y = tierY + 1; y <= tierY + 3; y++) for (let x = cx - 1; x <= cx + 1; x++) S.set(x, y, wallZ, null);
        S.set(cx, tierY + 4, wallZ, null);
        for (let x = cx - 2; x <= cx + 2; x++) for (let y = tierY + 1; y <= tierY + 5; y++)
          if (!(Math.abs(x - cx) <= 1 && y <= tierY + 3) && !(x === cx && y === tierY + 4)) S.set(x, y, wallZ, 'mud_brick');
        S.fill(cx - 1, tierY + 1, wallZ - 1, cx + 1, tierY + 2, wallZ - 1, 'wood_dark');
        S.fill(cx - 1, tierY + 3, wallZ - 1, cx + 1, tierY + 4, wallZ - 1, 'wood');
      }
      // Bandit flags on poles along each terrace, a guard here and there.
      // Small pennants between the doors (not in front of them).
      for (let x = -38; x <= 38; x += 21) { const fx = x + 3; S.fill(fx, tierY + 1, wallZ + 3, fx, tierY + 5, wallZ + 3, 'wood_dark'); S.fill(fx + 1, tierY + 4, wallZ + 3, fx + 2, tierY + 5, wallZ + 3, 'banner'); }
      for (let x = -36; x <= 36; x += 19) S.person(x, tierY, wallZ + 2, robe, 'robe_black');
    }
    // Entrance terrace: millstone, horse pen, campfire.
    S.fill(-12, 7, 14, -8, 7, 18, 'stone'); S.set(-10, 8, 16, 'stone_brick');
    for (let x = 10; x <= 22; x++) { S.set(x, 7, 13, 'wood'); S.set(x, 7, 22, 'wood'); }
    for (let z = 13; z <= 22; z++) { S.set(10, 7, z, 'wood'); S.set(22, 7, z, 'wood'); }
    S.fill(15, 7, 16, 17, 8, 17, 'wood_dark'); S.fill(0, 7, 18, 0, 7, 18, 'fire'); S.fill(0, 8, 18, 0, 12, 18, 'smoke');
    // Plateau: sunken courtyard (12x12, 6 deep) with the chief's caves, a watchtower, millet.
    const px = 22, pz = -40;
    S.fill(px - 6, 27, pz - 6, px + 6, 32, pz + 6, null);
    S.fill(px - 6, 26, pz - 6, px + 6, 26, pz + 6, 'dirt');
    for (const [dx, dz] of [[-6, -2], [-6, 2], [6, -2], [6, 2], [-2, -6], [2, -6]]) {
      const ox = Math.sign(dx), oz = Math.sign(dz);
      for (let k = 0; k <= 3; k++) S.fill(px + dx + ox * k, 27, pz + dz + oz * k, px + dx + ox * k, 29, pz + dz + oz * k, null);
      S.set(px + dx, 30, pz + dz, 'mud_brick');
    }
    S.tree(px, 26, pz, { leaves: 'leaves', h: 4, r: 2, rnd: r });
    S.person(px + 2, 26, pz + 2, 'robe_red', 'gold');
    for (let x = px - 7; x <= px + 7; x++) for (const z of [pz - 7, pz + 7]) S.set(x, 33, z, 'mud_brick');
    for (let z = pz - 7; z <= pz + 7; z++) for (const x of [px - 7, px + 7]) S.set(x, 33, z, 'mud_brick');
    S.fill(-20, 33, -24, -16, 44, -20, 'wood_dark'); S.fill(-21, 44, -25, -15, 44, -19, 'wood');
    S.roof(-21, -25, -15, -19, 45, { type: 'pyramid', mat: 'tile' }); S.person(-18, 44, -22, 'robe_black');
    S.fill(-60, 32, -60, -30, 32, -30, 'field');
    return { position: [6, 46, 70], target: [4, 18, -14], fov: 56 };
  }

  /** 4. Jade Pass fortress: rammed-earth enclosure, barbican and three-storey gate, yamen, Great Wall, beacon chain, caravan. */
  function fortress() {
    S.clear();
    const r = S.seeded(404);
    S.ground(-110, -100, 110, 100, (x, z) => 2 + Math.sin(x * 0.04) * 0.6, (x, z) => (Math.sin(x * 0.11) + Math.cos(z * 0.09 + x * 0.03) > 1.1 ? 'sand' : 'gravel'), 'sand');
    const R = 30;
    for (let y = 3; y <= 14; y++) for (let x = -R; x <= R; x++) for (let z = -R; z <= R; z++) {
      if (!(Math.abs(x) >= R - 2 || Math.abs(z) >= R - 2)) continue;
      if (Math.abs(x) <= 2 && z >= R - 2 && y <= 8) continue;
      S.set(x, y, z, y % 3 === 0 ? 'sandstone_layer' : 'rammed');
    }
    for (let x = -R; x <= R; x += 2) { S.set(x, 15, -R, 'mud_brick'); S.set(x, 15, R, 'mud_brick'); }
    for (let z = -R; z <= R; z += 2) { S.set(-R, 15, z, 'mud_brick'); S.set(R, 15, z, 'mud_brick'); }
    // Barbican courtyard in front of the south gate.
    for (let y = 3; y <= 11; y++) for (let x = -10; x <= 10; x++) for (let z = R; z <= R + 14; z++) {
      if (!(Math.abs(x) >= 9 || z >= R + 13)) continue;
      if (x >= 9 && z > R + 4 && z < R + 9 && y <= 7) continue; // side gate of the barbican
      S.set(x, y, z, y % 3 === 0 ? 'sandstone_layer' : 'rammed');
    }
    // Three-storey gate tower.
    S.fill(-8, 15, R - 4, 8, 15, R, 'stone_brick');
    S.hall(-7, R - 3, 3, 3, 16, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile', platform: 1, storey: 4 });
    S.hall(-5, R - 3, 2, 3, 24, { bay: 5, wall: 'red_wall', column: 'red', roofMat: 'tile', platform: 0, storey: 3, doors: false });
    for (const [x, z] of [[-R, -R], [R, -R], [-R, R], [R, R]]) { S.fill(x - 3, 15, z - 3, x + 3, 18, z + 3, 'rammed'); S.roof(x - 3, z - 3, x + 3, z + 3, 19, { type: 'pyramid', mat: 'tile', overhang: 0 }); }
    // Inside: the general's yamen, barracks, stables, well, drilling soldiers.
    S.hall(-9, -18, 4, 9, 3, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile', platform: 2, storey: 5 });
    S.fill(-26, 3, -6, -14, 6, 10, 'mud_brick'); S.fill(-26, 7, -6, -14, 7, 10, 'mud_brick');
    S.fill(14, 3, -6, 26, 4, 10, 'wood'); S.roof(14, -6, 26, 10, 5, { type: 'gable', mat: 'tile', lift: false });
    S.walls(-2, 3, 8, 2, 4, 12, 'cobble'); S.water(-1, 9, 1, 11, 4);
    for (let i = 0; i < 12; i++) S.person(-8 + (i % 6) * 3, 2, 14 + Math.floor(i / 6) * 3, 'armor', 'armor');
    S.person(1, 5, -6, 'robe_red', 'gold');
    for (let x = -R; x <= R; x += 8) S.person(x, 15, -R + 1, 'armor', 'armor');
    // The Great Wall west and east, beacon towers in a chain with smoke.
    for (let x = -110; x <= 110; x++) {
      if (Math.abs(x) < R) continue;
      const z = Math.round(Math.sin(x * 0.03) * 3);
      S.fill(x, 3, z - 1, x, 10, z + 1, x % 4 === 0 ? 'sandstone_layer' : 'rammed');
      if (x % 3 === 0) S.set(x, 11, z - 1, 'mud_brick');
    }
    for (const [x, z] of [[-70, -54], [-36, -84], [44, -78], [84, -46]]) {
      for (let y = 3; y <= 13; y++) { const w = Math.round(4 - (y - 3) / 4); S.fill(x - w, y, z - w, x + w, y, z + w, y % 3 === 0 ? 'hay' : 'rammed'); }
      S.set(x, 14, z, 'fire'); S.fill(x, 15, z, x, 30, z, 'smoke');
    }
    // A Silk Road caravan camped by the barbican.
    for (let i = 0; i < 5; i++) { S.camel(16 + i * 6, 2, R + 22 + (i % 2) * 3); S.person(15 + i * 6, 2, R + 26, 'robe_white', 'robe_white'); }
    for (const [x, z] of [[-60, 50], [-52, 62], [-44, 48]]) S.tree(x, 2, z, { trunk: 'bark', leaves: 'leaves_gold', h: 5, r: 3, rnd: r });
    return { position: [70, 52, 100], target: [0, 8, 6], fov: 55 };
  }

  return { sanctuary, village, lair, fortress };
})();
true;
