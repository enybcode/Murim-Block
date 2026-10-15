// Voxel mock-up of the Orthodox Alliance's capital (docs/structures/alliance-capital.md), built with ST (st-runtime.js).
// 1 voxel = 1 block. x east, z south, y up. The lower town lies in the valley (y 2) south of a 30-block cliff; the inner
// city stands on the plateau above (y 32); five granite needles rise from the plateau, the highest bearing the Great Hall.
window.CAPITAL = (() => {
  const S = ST;
  const PLATEAU = 32, TOWN = 2;
  const MAIN = { x: 0, z: -72, r: 24, h: 132 };           // the Great Hall's needle, summit y 164
  const NEEDLES = [
    { name: 'library', x: -78, z: -58, r: 15, h: 104 },   // the sects' library tower
    { name: 'arena', x: 78, z: -62, r: 17, h: 96 },       // the arena of the Alliance
    { name: 'elders', x: -46, z: -134, r: 12, h: 124 },   // the elders' residences
    { name: 'bell', x: 50, z: -128, r: 12, h: 118 },      // bell and beacon tower
  ];
  const top = n => PLATEAU + n.h;

  // A sheer, slightly ragged cliff between the town (south) and the plateau (north).
  const cliffZ = x => 40 + Math.round(Math.sin(x * 0.07) * 2 + Math.sin(x * 0.023) * 3);
  function height(x, z) { return z >= cliffZ(x) ? TOWN : PLATEAU; }

  /** A granite needle, hollow (only its skin is drawn), jointed every 9 blocks, a flat summit. */
  function needle(n, r, solidHalf = false) {
    const y0 = PLATEAU, y1 = PLATEAU + n.h;
    for (let y = y0 - 2; y <= y1; y++) {
      const t = (y - y0) / n.h;
      const base = n.r * (1 - 0.3 * t) * (t > 0.92 ? Math.sqrt(1 - (t - 0.92) / 0.2) : 1) + (y % 9 === 0 ? 0.8 : 0);
      const lx = Math.round(Math.sin(y * 0.031 + n.x) * 2.5), lz = Math.round(Math.cos(y * 0.027 + n.z) * 2);
      const R = Math.ceil(base * 1.25) + 1;
      for (let dx = -R; dx <= R; dx++) for (let dz = -R; dz <= R; dz++) {
        const a = Math.atan2(dz, dx);
        const rr = base * (1 + 0.13 * Math.sin(a * 3 + n.x * 0.1 + Math.floor(y / 13)) + 0.08 * Math.sin(a * 7 + y * 0.05));
        const d = Math.hypot(dx - lx, dz - lz) + (r() - 0.5) * 0.9;
        if (d > rr) continue;
        const skin = d > rr - 2.2 || y === y1;
        if (skin || (solidHalf && dx <= 0)) S.set(n.x + dx, y, n.z + dz, !skin ? 'stone' : y > y1 - 3 && d > rr - 1.5 && r() < 0.3 ? 'limestone_moss' : 'granite');
      }
    }
    // Cliff pines on the flanks.
    for (let k = 0; k < 4; k++) {
      const a = r() * Math.PI * 2, y = y0 + 20 + Math.floor(r() * (n.h - 30));
      const rr = n.r * (1 - 0.3 * (y - y0) / n.h) * 1.1;
      S.pine(n.x + Math.round(Math.cos(a) * (rr + 1)), y, n.z + Math.round(Math.sin(a) * (rr + 1)), 3, r);
    }
    return y1;
  }

  /** A sagging plank bridge with chain rails between two points. */
  function bridge(x0, y0, z0, x1, y1, z1) {
    const len = Math.hypot(x1 - x0, z1 - z0), steps = Math.ceil(len * 1.5);
    const nx = -(z1 - z0) / len, nz = (x1 - x0) / len;
    for (let i = 0; i <= steps; i++) {
      const t = i / steps, sag = Math.sin(Math.PI * t) * len * 0.06;
      const x = x0 + (x1 - x0) * t, z = z0 + (z1 - z0) * t, y = Math.round(y0 + (y1 - y0) * t - sag);
      for (const s of [-1, 0, 1]) S.set(Math.round(x + nx * s), y, Math.round(z + nz * s), 'wood');
      for (const s of [-2, 2]) S.set(Math.round(x + nx * s), y + 1, Math.round(z + nz * s), 'chain');
    }
  }

  /** Azure banner on a pole. */
  function banner(x, y, z, h = 8) {
    S.fill(x, y + 1, z, x, y + h, z, 'wood_dark');
    S.fill(x + 1, y + h - 4, z, x + 1, y + h, z, 'banner_azure'); S.set(x + 1, y + h - 5, z, 'gold');
  }

  /** Paifang arch with azure roofs across an avenue of width w at z. */
  function paifang(cx, y, z, w = 9) {
    const hw = Math.floor(w / 2);
    for (const x of [cx - hw - 2, cx - hw + 2, cx + hw - 2, cx + hw + 2]) S.fill(x, y + 1, z, x, y + 9, z, 'red');
    S.fill(cx - hw - 3, y + 10, z, cx + hw + 3, y + 10, z, 'wood_dark');
    S.fill(cx - hw, y + 9, z, cx + hw, y + 9, z, 'gold');
    S.roof(cx - hw - 3, z, cx + hw + 3, z, y + 11, { type: 'gable', mat: 'tile_azure', overhang: 1 });
  }

  function stall(x, y, z, awning) {
    for (const [dx, dz] of [[0, 0], [3, 0], [0, 2], [3, 2]]) S.fill(x + dx, y + 1, z + dz, x + dx, y + 3, z + dz, 'wood_dark');
    S.fill(x, y + 4, z, x + 3, y + 4, z + 2, awning);
    S.fill(x + 1, y + 1, z + 1, x + 2, y + 1, z + 1, 'wood');
    S.set(x + 1, y + 2, z + 1, ['rice', 'lotus_flower', 'gold', 'hay'][(x * 7 + z) & 3]);
  }

  /** A courtyard house: a main hall north, two side rooms, a wall with a gate to the south. */
  function courtyard(x, z, w, d, r) {
    const y = TOWN;
    S.walls(x, y + 1, z, x + w, y + 3, z + d, 'white');
    S.fill(x + Math.floor(w / 2), y + 1, z + d, x + Math.floor(w / 2) + 1, y + 2, z + d, null);
    for (let xx = x; xx <= x + w; xx++) { S.set(xx, y + 4, z, 'tile'); S.set(xx, y + 4, z + d, 'tile'); }
    for (let zz = z; zz <= z + d; zz++) { S.set(x, y + 4, zz, 'tile'); S.set(x + w, y + 4, zz, 'tile'); }
    S.hall(x + 2, z + 2, Math.max(1, Math.floor((w - 4) / 4)), 5, y, { bay: 4, wall: 'white', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 4 });
    if (r() < 0.6) S.tree(x + w - 3, y, z + d - 3, { leaves: r() < 0.3 ? 'leaves_pink' : 'leaves', h: 4, r: 2, rnd: r });
  }

  function villagers(x0, z0, x1, z1, n, r, y = TOWN) {
    const robes = ['robe_blue', 'robe_white', 'robe_red', 'robe_black', 'wood_light', 'teal'];
    for (let i = 0; i < n; i++) {
      const x = Math.round(x0 + r() * (x1 - x0)), z = Math.round(z0 + r() * (z1 - z0));
      if (S.get(x, y + 1, z) || S.get(x, y + 2, z)) continue;
      S.person(x, y, z, robes[Math.floor(r() * robes.length)], r() < 0.3 ? 'thatch' : 'hair');
    }
  }

  // ---------------------------------------------------------------- the whole capital
  function build({ cutaway = false } = {}) {
    const needles = cutaway ? NEEDLES.filter(n => n.name !== 'arena') : NEEDLES;
    S.clear();
    const r = S.seeded(7001);
    // Land: the valley of the lower town, the cliff, the plateau of the inner city.
    S.ground(-170, -175, 170, 190, height, (x, z, h) => h === TOWN ? 'grass' : 'grass_lush', 'dirt', 2);
    for (let x = -170; x <= 170; x++) { // the cliff's rock face
      const z = cliffZ(x) - 1;
      for (let y = TOWN; y < PLATEAU; y++) { S.set(x, y, z, y % 7 === 0 ? 'stone' : 'granite'); if ((x * 13 + y * 7) % 11 === 0) S.set(x, y, z + 1, 'granite'); }
    }

    // ------------------------------------------------ the lower town (villagers, market, inns, crafts)
    const WALL = { x0: -120, x1: 120, z0: 44, z1: 170 };
    for (let y = TOWN + 1; y <= TOWN + 10; y++) for (let x = WALL.x0; x <= WALL.x1; x++) for (let z = WALL.z0; z <= WALL.z1; z++) {
      const edge = x <= WALL.x0 + 1 || x >= WALL.x1 - 1 || z >= WALL.z1 - 1;
      if (!edge) continue;
      if (Math.abs(x) <= 3 && z >= WALL.z1 - 1 && y <= TOWN + 7) continue; // the south gate
      S.set(x, y, z, y === TOWN + 10 && (x + z) % 2 === 0 ? null : 'stone_brick');
    }
    for (const [x, z] of [[WALL.x0, WALL.z1], [WALL.x1, WALL.z1], [WALL.x0, WALL.z0 + 4], [WALL.x1, WALL.z0 + 4]]) {
      S.fill(x - 4, TOWN + 1, z - 4, x + 4, TOWN + 13, z + 4, 'stone_brick');
      S.roof(x - 4, z - 4, x + 4, z + 4, TOWN + 14, { type: 'pyramid', mat: 'tile_azure' });
    }
    // The great south gate: a three-storey tower over the gate, guards, banners.
    S.fill(-12, TOWN + 1, WALL.z1 - 3, 12, TOWN + 12, WALL.z1 + 1, 'stone_brick');
    S.fill(-3, TOWN + 1, WALL.z1 - 3, 3, TOWN + 7, WALL.z1 + 1, null);
    S.hall(-10, WALL.z1 - 3, 5, 4, TOWN + 12, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile_azure', platform: 1, storey: 5 });
    S.hall(-6, WALL.z1 - 2, 3, 3, TOWN + 24, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile_azure', platform: 0, storey: 4, doors: false });
    for (const x of [-6, 6]) S.person(x, TOWN, WALL.z1 + 3, 'armor', 'armor');
    for (const x of [-14, 14]) banner(x, TOWN + 12, WALL.z1 + 1, 9);
    // Avenue, paifang arches, the market square.
    S.fill(-3, TOWN, 74, 3, TOWN, WALL.z1, 'cobble');
    S.fill(-120, TOWN, 104, 120, TOWN, 106, 'cobble');
    paifang(0, TOWN, 150);
    S.fill(-24, TOWN, 92, 24, TOWN, 118, 'cobble');
    for (const [x, z] of [[-22, 94], [-16, 94], [-10, 94], [8, 94], [14, 94], [20, 94], [-22, 112], [-16, 112], [8, 112], [14, 112], [20, 112]])
      stall(x, TOWN, z, ['awning_red', 'awning_blue', 'awning_gold'][(x + z) % 3 < 0 ? 0 : (x + z) % 3]);
    villagers(-24, 90, 24, 120, 70, r);
    villagers(-3, 76, 3, 168, 30, r);
    villagers(-118, 104, 118, 106, 30, r);
    // Courtyard houses in blocks on both sides; special buildings near the avenue.
    for (const side of [-1, 1]) for (let bx = 0; bx < 4; bx++) for (let bz = 0; bz < 4; bz++) {
      const x = side < 0 ? -114 + bx * 22 : 32 + bx * 22, z = 70 + bz * 22 + (bz >= 2 ? 6 : 0);
      if (z >= 98 && z <= 112) continue; // the cross street
      if (side < 0 && bx === 0 && bz === 0) continue; // the pool and the falls
      courtyard(x, z, 18, 16, r);
    }
    // The inn (two storeys, lanterns), the teahouse by the stream, the forge, the herbalist, the recruiting hall.
    S.hall(30, 146, 4, 10, TOWN, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile', platform: 1, storey: 9 });
    for (let x = 31; x <= 46; x += 3) S.set(x, TOWN + 7, 157, 'lantern');
    S.hall(-34, 146, 3, 9, TOWN, { bay: 4, wall: 'wood_light', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 5 });
    S.fill(60, TOWN + 1, 104, 64, TOWN + 3, 108, 'cobble'); S.set(62, TOWN + 4, 106, 'fire'); S.fill(62, TOWN + 5, 106, 62, TOWN + 16, 106, 'smoke'); // forge
    S.hall(-30, 74, 3, 8, TOWN, { bay: 4, wall: 'white', column: 'wood_dark', roofMat: 'tile_green', platform: 1, storey: 4 }); // herbalist
    S.hall(-14, 122, 7, 12, TOWN, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile_azure', platform: 2, storey: 6 }); // recruiting hall
    banner(-17, TOWN + 2, 138); banner(17, TOWN + 2, 138);
    // The stream from the falls, its pool, stone bridges, willows.
    S.water(-56, 42, -34, 58, TOWN, 'water');
    for (let z = 58; z <= WALL.z1 + 6; z++) { const x = -45 + Math.round(Math.sin(z * 0.08) * 6); S.fill(x - 2, TOWN, z, x + 2, TOWN, z, 'water'); }
    for (const z of [105, 140]) { const x = -45 + Math.round(Math.sin(z * 0.08) * 6); S.fill(x - 3, TOWN + 1, z - 1, x + 3, TOWN + 1, z + 1, 'stone_brick'); }
    for (const z of [70, 90, 122, 150]) S.willow(-45 + Math.round(Math.sin(z * 0.08) * 6) + 6, TOWN, z, r);
    // Rice fields outside the walls.
    for (let i = 0; i < 6; i++) S.fill(-160 + i * 12, TOWN, 166, -150 + i * 12, TOWN, 186, i % 2 ? 'paddy' : 'rice');
    for (let i = 0; i < 6; i++) S.fill(90 + i * 12, TOWN, 166, 100 + i * 12, TOWN, 186, i % 2 ? 'rice' : 'paddy');

    // ------------------------------------------------ the great stair and the falls
    // The great stair: a hundred steps climbing the cliff from the town's head to the inner city's gate.
    const foot = cliffZ(0) + (PLATEAU - TOWN);
    for (let z = cliffZ(0) - 1; z <= foot; z++) {
      const h = Math.min(PLATEAU, TOWN + (foot - z));
      S.fill(-6, TOWN, z, 6, h, z, 'stone_brick');
      S.fill(-6, h + 1, z, -6, h + 1, z, 'white'); S.fill(6, h + 1, z, 6, h + 1, z, 'white');
      if ((foot - z) % 10 === 0 && z > cliffZ(0)) { S.set(-6, h + 2, z, 'lantern'); S.set(6, h + 2, z, 'lantern'); }
    }
    paifang(0, TOWN, foot + 2); paifang(0, PLATEAU, cliffZ(0) - 4);
    const fz = cliffZ(-44) - 1;
    for (let y = TOWN + 1; y <= PLATEAU; y++) S.fill(-46, y, fz + 1, -42, y, fz + 1, 'water'); // the falls
    S.fill(-46, PLATEAU, -20, -42, PLATEAU, fz, 'water');
    for (let k = 0; k < 18; k++) S.blob(-44 + Math.round((r() - 0.5) * 8), TOWN + 1, fz + 3 + Math.round(r() * 4), 2, 1, 2, 'cloud', 0.4, r);

    // ------------------------------------------------ the inner city on the plateau
    for (let x = -130; x <= 130; x++) { const z = cliffZ(x) - 4; S.fill(x, PLATEAU + 1, z, x, PLATEAU + 6, z, (x & 7) === 0 ? 'red' : 'red_wall'); if (x % 2 === 0) S.set(x, PLATEAU + 7, z, 'tile_azure'); }
    S.fill(-6, PLATEAU + 1, cliffZ(0) - 4, 6, PLATEAU + 5, cliffZ(0) - 4, null);
    // Pines and old trees over the plateau.
    for (let k = 0; k < 70; k++) { const x = Math.round((r() - 0.5) * 300), z = Math.round(-160 + r() * 190); if (Math.abs(x) < 70 && z > -40) continue; S.pine(x, PLATEAU, z, 4 + Math.floor(r() * 4), r); }
    S.fill(-3, PLATEAU, -40, 3, PLATEAU, 20, 'stone_brick');
    // Two training grounds with disciples in rows, weapon racks, a master before each.
    for (const sx of [-1, 1]) {
      const x0 = sx < 0 ? -62 : 16, x1 = sx < 0 ? -16 : 62;
      S.fill(x0, PLATEAU, -6, x1, PLATEAU, 14, 'stone_brick');
      for (let i = 0; i < 24; i++) S.person(x0 + 4 + (i % 8) * 5, PLATEAU, -2 + Math.floor(i / 8) * 5, 'robe_white', 'hair');
      S.person((x0 + x1) >> 1, PLATEAU, -8, 'robe_blue', 'gold');
      for (let x = x0 + 2; x <= x1 - 2; x += 3) S.fill(x, PLATEAU + 1, 13, x, PLATEAU + 3, 13, 'wood_dark');
    }
    // Disciples' halls, the infirmary, the armoury, the kitchens, the Hall of Merit (faction quests).
    for (const x of [-120, -96, 74, 98]) S.hall(x, -6, 4, 9, PLATEAU, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile', platform: 1, storey: 5 });
    S.hall(-120, -32, 3, 10, PLATEAU, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile_green', platform: 1, storey: 5 });
    S.hall(104, -32, 3, 10, PLATEAU, { bay: 4, wall: 'stone_brick', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 5 });
    S.hall(-110, -110, 3, 8, PLATEAU, { bay: 4, wall: 'wood_light', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 4 });
    S.fill(-104, PLATEAU + 6, -106, -104, PLATEAU + 16, -106, 'smoke');
    S.hall(-18, -36, 9, 10, PLATEAU, { bay: 4, wall: 'red_wall', column: 'red', roofMat: 'tile_azure', platform: 2, storey: 6 });
    villagers(-120, -40, 120, 18, 40, r, PLATEAU);
    // The lotus pond and its zigzag bridge.
    S.water(26, -40, 56, -22, PLATEAU, 'water_jade');
    for (let k = 0; k < 30; k++) S.set(27 + Math.floor(r() * 28), PLATEAU + 1, -39 + Math.floor(r() * 16), r() < 0.3 ? 'lotus_flower' : 'lotus');
    for (let i = 0; i <= 30; i++) S.set(26 + i, PLATEAU + 1, -31 + ((i >> 2) % 2 ? 2 : -2), 'stone_brick');

    // ------------------------------------------------ the needles
    const mainTop = needle(MAIN, r, cutaway);
    for (const n of needles) needle(n, r);
    for (const [x, z, rr, h] of [[-140, -160, 14, 140], [130, -165, 15, 150], [0, -175, 18, 160], [-150, -60, 10, 90], [150, -50, 11, 95], [-110, -175, 11, 120], [100, -180, 12, 130]])
      needle({ x, z, r: rr, h }, r);
    // The zigzag path up the main needle's south face, with rest pavilions.
    const face = MAIN.z + MAIN.r + 1;
    let y = PLATEAU, x = -14, dir = 1;
    while (y < mainTop - 2) {
      for (let k = 0; k < 28 && y < mainTop - 2; k++) {
        S.fill(x, y, face - 1, x, y, face + 1, 'stone_brick'); S.set(x, y + 1, face + 2, 'white');
        x += dir; if (k % 2 === 1) y++;
      }
      S.fill(x - 2, y, face - 1, x + 2, y, face + 3, 'stone_brick');
      if (r() < 0.5) S.pavilion(x, face + 1, y, 1, { roofMat: 'tile_azure', h: 3 });
      dir = -dir;
    }

    // ------------------------------------------------ the summit: the Great Hall of the Alliance
    S.fill(MAIN.x - 19, mainTop, MAIN.z - 19, MAIN.x + 19, mainTop, MAIN.z + 19, 'marble');
    S.hall(MAIN.x - 15, MAIN.z - 16, 6, 16, mainTop + 1, { bay: 5, wall: 'red_wall', column: 'red', roofMat: 'tile_azure', platform: 3, storey: 9 });
    S.roof(MAIN.x - 11, MAIN.z - 13, MAIN.x + 11, MAIN.z - 3, mainTop + 26, { type: 'hip', mat: 'tile_azure' }); // a second roof tier
    S.fill(MAIN.x - 11, mainTop + 14, MAIN.z - 13, MAIN.x + 11, mainTop + 25, MAIN.z - 3, 'red_wall');
    S.fill(MAIN.x - 1, mainTop + 1, MAIN.z + 8, MAIN.x + 1, mainTop + 2, MAIN.z + 10, 'bronze'); // the great cauldron
    S.fill(MAIN.x, mainTop + 3, MAIN.z + 9, MAIN.x, mainTop + 9, MAIN.z + 9, 'smoke');
    for (const bx of [-17, -9, 9, 17]) banner(MAIN.x + bx, mainTop, MAIN.z + 17, 10);
    for (const [dx, dz] of [[-6, 12], [6, 12], [-3, 14], [3, 14]]) S.person(MAIN.x + dx, mainTop, MAIN.z + dz, 'armor', 'armor');
    S.person(MAIN.x, mainTop + 4, MAIN.z - 2, 'robe_white', 'gold');

    // The satellite needles' buildings.
    const lib = NEEDLES[0], arena = NEEDLES[1], eld = NEEDLES[2], bell = NEEDLES[3];
    if (!cutaway) {
    S.fill(lib.x - 11, top(lib), lib.z - 11, lib.x + 11, top(lib), lib.z + 11, 'stone_brick');
    for (let t = 0; t < 4; t++) { // the library: a four-tier tower
      const w = 8 - t * 2, yy = top(lib) + 1 + t * 7;
      S.walls(lib.x - w, yy, lib.z - w, lib.x + w, yy + 4, lib.z + w, t % 2 ? 'white' : 'red_wall');
      S.roof(lib.x - w, lib.z - w, lib.x + w, lib.z + w, yy + 5, { type: 'pyramid', mat: 'tile_azure', overhang: 2 });
    }
    S.fill(arena.x - 14, top(arena), arena.z - 14, arena.x + 14, top(arena), arena.z + 14, 'stone_brick');
    for (let a = 0; a < 72; a++) { // the arena's ring of stands
      const ang = a / 72 * Math.PI * 2;
      for (let s = 0; s < 3; s++) S.set(arena.x + Math.round(Math.cos(ang) * (12 - s)), top(arena) + 3 - s, arena.z + Math.round(Math.sin(ang) * (12 - s)), 'stone_brick');
    }
    S.fill(arena.x - 5, top(arena) + 1, arena.z - 5, arena.x + 5, top(arena) + 1, arena.z + 5, 'marble');
    S.person(arena.x - 2, top(arena) + 1, arena.z, 'robe_blue'); S.person(arena.x + 2, top(arena) + 1, arena.z, 'robe_red');
    villagers(arena.x - 11, arena.z - 11, arena.x + 11, arena.z + 11, 30, r, top(arena) + 3);
    }
    S.fill(eld.x - 9, top(eld), eld.z - 9, eld.x + 9, top(eld), eld.z + 9, 'stone_brick');
    S.hall(eld.x - 7, eld.z - 6, 3, 8, top(eld) + 1, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile_azure', platform: 1, storey: 5 });
    S.pine(eld.x + 7, top(eld), eld.z + 7, 4, r);
    S.fill(bell.x - 9, top(bell), bell.z - 9, bell.x + 9, top(bell), bell.z + 9, 'stone_brick');
    S.fill(bell.x - 4, top(bell) + 1, bell.z - 4, bell.x + 4, top(bell) + 10, bell.z + 4, 'red_wall');
    S.roof(bell.x - 4, bell.z - 4, bell.x + 4, bell.z + 4, top(bell) + 11, { type: 'pyramid', mat: 'tile_azure', overhang: 2 });
    S.set(bell.x, top(bell) + 7, bell.z + 5, 'bronze');
    // Chain bridges from the summit court to every needle.
    for (const n of needles) {
      const dx = n.x - MAIN.x, dz = n.z - MAIN.z, d = Math.hypot(dx, dz);
      bridge(MAIN.x + dx / d * 19, mainTop, MAIN.z + dz / d * 19, n.x - dx / d * (n.r - 2), top(n), n.z - dz / d * (n.r - 2));
    }
    // The sea of clouds between the needles.
    if (!cutaway) for (let k = 0; k < 260; k++) {
      const cx = Math.round((r() - 0.5) * 320), cz = Math.round(-30 - r() * 150), cy = 64 + Math.round(r() * 26);
      S.blob(cx, cy, cz, 4 + Math.round(r() * 5), 1, 3 + Math.round(r() * 4), 'cloud', 0.35, r);
    }
    return mainTop;
  }

  // ---------------------------------------------------------------- views
  const SKY = '#C3D2DE';
  function overview() { build(); return { view: { unclamped: true, position: [120, 150, 330], target: [0, 66, -40], fov: 52 }, sky: SKY, fog: [420, 1300] }; }
  function town() { build(); return { view: { unclamped: true, position: [70, 46, 240], target: [-6, 4, 108], fov: 55 }, sky: SKY, fog: [260, 900] }; }
  function inner() { build(); return { view: { unclamped: true, position: [-110, 70, 70], target: [-10, PLATEAU, -10], fov: 55 }, sky: SKY, fog: [260, 900] }; }
  function summit() { const t = build(); return { view: { unclamped: true, position: [70, t + 40, 40], target: [0, t + 6, -70], fov: 55 }, sky: SKY, fog: [300, 1000] }; }
  function plan() {
    const t = build();
    const cz = cliffZ(0);
    const marks = [
      ['1', 0, 14, 172], ['2', 0, 8, 105], ['3', 38, 14, 151], ['4', -28, 12, 151], ['5', 62, 12, 106], ['6', -24, 10, 78], ['7', 0, 14, 128],
      ['8', -45, 12, 50], ['9', 0, PLATEAU, cz + 12], ['10', 0, PLATEAU + 6, cz - 4], ['11', -40, PLATEAU + 4, 4], ['12', 0, PLATEAU + 10, -30],
      ['13', -86, PLATEAU + 8, 0], ['14', -118, PLATEAU + 8, -30], ['15', 110, PLATEAU + 8, -26], ['16', -104, PLATEAU + 8, -106], ['17', 41, PLATEAU + 2, -31],
      ['18', 0, PLATEAU + 60, MAIN.z + MAIN.r], ['19', 0, t + 12, MAIN.z - 8], ['20', NEEDLES[0].x, top(NEEDLES[0]) + 14, NEEDLES[0].z], ['21', NEEDLES[1].x, top(NEEDLES[1]) + 6, NEEDLES[1].z],
      ['22', NEEDLES[2].x, top(NEEDLES[2]) + 8, NEEDLES[2].z], ['23', NEEDLES[3].x, top(NEEDLES[3]) + 14, NEEDLES[3].z], ['24', -40, t - 8, -66]];
    return { view: { unclamped: true, position: [0, 300, 250], target: [0, 30, -6], fov: 56 }, sky: SKY, fog: [900, 2000], marks };
  }

  /** The secrets: the main needle cut in half along x = 0 (its east half gone), the hidden ways inside shown. */
  function secrets() {
    const t = build({ cutaway: true });
    const cz = cliffZ(0);
    // Solid rock and earth under the plateau west of the cut, where the passage runs.
    S.fill(-60, TOWN, MAIN.z + 14, 0, PLATEAU - 1, cz - 2, 'dirt');
    for (let y = TOWN; y < PLATEAU; y += 5) S.fill(-60, y, MAIN.z + 14, 0, y, cz - 2, 'stone');
    // Cut: nothing east of x = 0 above the plateau, nor under it along the passage.
    for (let y = PLATEAU + 1; y <= t + 60; y++) for (let x = 1; x <= 175; x++) for (let z = -200; z <= cz; z++) S.set(x, y, z, null);
    for (let y = TOWN; y <= PLATEAU; y++) for (let x = 1; x <= 175; x++) for (let z = MAIN.z + 14; z <= cz + 2; z++) S.set(x, y, z, null);
    const room = (x0, y0, z0, x1, y1, z1, floor, wall) => { S.fill(x0, y0, z0, x1, y1, z1, null); S.fill(x0, y0 - 1, z0, x1, y0 - 1, z1, floor); if (wall) S.fill(x0 - 1, y0, z0, x0 - 1, y1, z1, wall); };
    // A. Behind the Great Hall's throne, a stair down into the sealed vault: chests, racks of forbidden manuals.
    for (let k = 0; k < 16; k++) S.fill(-6, t - k, MAIN.z - 14 + k, 0, t - k, MAIN.z - 14 + k, null), S.fill(-6, t - k - 1, MAIN.z - 14 + k, 0, t - k - 1, MAIN.z - 14 + k, 'stone_brick');
    room(-16, t - 28, MAIN.z - 12, 0, t - 17, MAIN.z + 10, 'marble', 'red_wall');
    for (let z = MAIN.z - 10; z <= MAIN.z + 8; z += 4) { S.fill(-14, t - 28, z, -12, t - 27, z + 1, 'gold'); S.fill(-8, t - 28, z, -6, t - 24, z, 'wood_dark'); S.fill(-8, t - 26, z, -6, t - 26, z, 'paper'); }
    for (const z of [MAIN.z - 11, MAIN.z + 9]) S.set(-2, t - 20, z, 'lantern');
    S.fill(-1, t - 28, MAIN.z - 2, 0, t - 24, MAIN.z + 2, 'bronze'); // the sealed door
    // B. A shaft with a ladder down the heart of the needle.
    S.fill(-4, PLATEAU + 16, MAIN.z - 2, 0, t - 29, MAIN.z + 2, null);
    for (let y = PLATEAU + 16; y <= t - 29; y += 3) S.fill(-4, y, MAIN.z - 2, -4, y, MAIN.z + 2, 'wood');
    // C. The dragon-vein chamber at the needle's foot: Qi wells up from the rock.
    room(-20, PLATEAU + 2, MAIN.z - 14, 0, PLATEAU + 15, MAIN.z + 14, 'granite', null);
    for (let k = 0; k < 220; k++) S.set(-19 + Math.floor(r2() * 19), PLATEAU + 2 + Math.floor(r2() * 13), MAIN.z - 13 + Math.floor(r2() * 27), 'qi');
    S.fill(-12, PLATEAU + 2, MAIN.z - 3, -8, PLATEAU + 2, MAIN.z + 3, 'qi');
    S.person(-10, PLATEAU + 2, MAIN.z, 'robe_white', 'hair');
    // D. The passage south under the plateau, down to the cave behind the falls.
    for (let z = MAIN.z + 14; z <= cz - 2; z++) {
      const y = z < MAIN.z + 40 ? PLATEAU + 2 - Math.round((z - MAIN.z - 14) * (PLATEAU - TOWN - 6) / 26) : TOWN + 2;
      const yy = Math.max(TOWN + 2, y);
      S.fill(-3, yy, z, 0, yy + 3, z, null); S.fill(-3, yy - 1, z, 0, yy - 1, z, 'stone_brick');
      if (z % 6 === 0) S.set(-3, yy + 3, z, 'lantern');
    }
    room(-14, TOWN + 2, cz - 12, 0, TOWN + 9, cz - 2, 'stone', null);
    S.fill(-14, TOWN + 2, cz - 2, -10, TOWN + 8, cz - 2, 'water');
    // E. Cells cut into the rock, barred with chains.
    for (let k = 0; k < 3; k++) { const z = MAIN.z - 8 + k * 7; room(-10, PLATEAU + 24, z, 0, PLATEAU + 28, z + 4, 'stone', null); for (let y = PLATEAU + 24; y <= PLATEAU + 28; y++) S.set(0, y, z + 2, 'chain'); }
    const marks = [['A', -8, t - 22, MAIN.z - 1], ['B', -2, (PLATEAU + t) / 2, MAIN.z], ['C', -6, PLATEAU + 9, MAIN.z + 8], ['D', -1, TOWN + 12, MAIN.z + 60], ['E', -4, PLATEAU + 26, MAIN.z + 7]];
    return { view: { unclamped: true, position: [190, (PLATEAU + t) / 2 + 20, MAIN.z + 40], target: [0, (PLATEAU + t) / 2 - 10, MAIN.z + 22], fov: 62 }, sky: SKY, fog: [500, 1400], marks };
  }
  const r2 = S.seeded(99);

  return { overview, town, inner, summit, plan, secrets };
})();
true;
