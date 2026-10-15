// Voxel mock-up of an outpost of the Orthodox Alliance (docs/structures/alliance-outpost.md), built with ST
// (st-runtime.js). 1 voxel = 1 block. x east, z south, y up. A walled garrison on a levelled knoll (y 8) above a road
// (y 2): a gate to the south, the command hall to the north, barracks west, stables east, the training yard between,
// and in the north-east corner the watchtower whose beacon fire relays word to the capital.
window.OUTPOST = (() => {
  const S = ST;
  const LOW = 2, P = 8;                                   // the land around, the outpost's yard
  const X0 = -26, X1 = 26, Z0 = -22, Z1 = 24;             // the walls
  const TOWER = { x: 21, z: -17 };                         // the watchtower (north-east)

  // The knoll: a levelled top for the walls, slopes down to the land, the road cut down its south side.
  function height(x, z) {
    const inside = x >= X0 - 3 && x <= X1 + 3 && z >= Z0 - 3 && z <= Z1 + 3;
    if (inside) return P;
    const dx = Math.max(X0 - 3 - x, 0, x - X1 - 3), dz = Math.max(Z0 - 3 - z, 0, z - Z1 - 3);
    const d = Math.hypot(dx, dz);
    return Math.max(LOW, Math.round(P - d * 0.45 + Math.sin(x * 0.2) * 0.6 + Math.cos(z * 0.17) * 0.6));
  }

  /** An azure banner on a pole. */
  function banner(x, y, z, h = 7) {
    S.fill(x, y + 1, z, x, y + h, z, 'wood_dark');
    S.fill(x + 1, y + h - 3, z, x + 1, y + h, z, 'banner_azure'); S.set(x + 1, y + h - 4, z, 'gold');
  }

  /** A square tower: stone base, white storeys, an azure pyramid roof; `beacon`: a brazier of fire and its smoke. */
  function tower(cx, cz, h, w, { beacon = false } = {}) {
    S.fill(cx - w, P + 1, cz - w, cx + w, P + 3, cz + w, 'stone_brick');
    S.walls(cx - w, P + 4, cz - w, cx + w, P + h, cz + w, 'white');
    for (const [dx, dz] of [[-w, -w], [w, -w], [-w, w], [w, w]]) S.fill(cx + dx, P + 4, cz + dz, cx + dx, P + h, cz + dz, 'red');
    for (let y = P + 6; y < P + h; y += 5) for (const s of [-1, 1]) { S.set(cx + s * w, y, cz, null); S.set(cx, y, cz + s * w, null); } // arrow slits
    S.fill(cx - w - 1, P + h + 1, cz - w - 1, cx + w + 1, P + h + 1, cz + w + 1, 'wood_dark');      // the lookout floor
    for (const [dx, dz] of [[-w - 1, -w - 1], [w + 1, -w - 1], [-w - 1, w + 1], [w + 1, w + 1]]) S.fill(cx + dx, P + h + 2, cz + dz, cx + dx, P + h + 4, cz + dz, 'red');
    S.roof(cx - w - 1, cz - w - 1, cx + w + 1, cz + w + 1, P + h + 5, { type: 'pyramid', mat: 'tile_azure', overhang: 2 });
    if (beacon) {
      const by = P + h + 2;
      S.fill(cx - 1, by, cz - 1, cx + 1, by, cz + 1, 'bronze'); S.fill(cx - 1, by + 1, cz - 1, cx + 1, by + 1, cz + 1, 'fire');
      S.set(cx, by + 2, cz, 'fire');
    }
    return P + h;
  }

  function dummy(x, z) { S.fill(x, P + 1, z, x, P + 2, z, 'wood'); S.set(x, P + 3, z, 'hay'); S.set(x - 1, P + 2, z, 'wood'); S.set(x + 1, P + 2, z, 'wood'); }
  function rack(x, z) { S.fill(x, P + 1, z, x + 3, P + 1, z, 'wood_dark'); S.fill(x, P + 2, z, x + 3, P + 2, z, 'wood'); for (let i = 0; i < 4; i++) S.set(x + i, P + 3, z, i % 2 ? 'armor' : 'wood_light'); }

  // ---------------------------------------------------------------- the whole outpost
  function build() {
    S.clear();
    const r = S.seeded(4242);
    S.ground(-90, -80, 90, 110, height, (x, z, h) => h >= P ? 'grass' : h > LOW + 2 ? 'grass_lush' : 'grass', 'dirt', 3);

    // The road: up from the south to the gate, cut into the slope; on along the land east and west.
    for (let z = Z1 + 1; z <= 110; z++) {
      const y = Math.max(LOW, Math.min(P, P - Math.floor((z - Z1 - 1) / 3)));
      for (let x = -2; x <= 2; x++) { S.fill(x, LOW - 1, z, x, y, z, null); S.fill(x, LOW - 2, z, x, y - 1, z, 'dirt'); S.set(x, y, z, Math.abs(x) === 2 ? 'cobble' : 'stone_brick'); }
    }
    for (let x = -90; x <= 90; x++) for (let z = 60; z <= 63; z++) if (Math.abs(x) > 2) S.set(x, LOW, z, 'gravel');

    // The yard: trodden earth, a flagged way from the gate to the hall.
    S.fill(X0, P, Z0, X1, P, Z1, 'dirt');
    S.fill(-2, P, -12, 2, P, Z1, 'stone_brick');

    // ------------------------------------------------ walls, corner towers, gate
    for (let x = X0; x <= X1; x++) for (let z = Z0; z <= Z1; z++) {
      const edge = x <= X0 + 1 || x >= X1 - 1 || z <= Z0 + 1 || z >= Z1 - 1;
      if (!edge) continue;
      if (Math.abs(x) <= 2 && z >= Z1 - 1) continue; // the gate passage
      S.fill(x, P + 1, z, x, P + 3, z, 'stone_brick');
      S.fill(x, P + 4, z, x, P + 6, z, 'white');
      const outer = x === X0 || x === X1 || z === Z0 || z === Z1;
      if (outer && (x + z) % 2 === 0) S.set(x, P + 7, z, 'tile');            // crenels with dark caps
      else if (!outer) S.set(x, P + 6, z, 'wood_dark');                       // the walkway
    }
    for (const [x, z] of [[X0 + 2, Z0 + 2], [X0 + 2, Z1 - 2], [X1 - 2, Z1 - 2]]) tower(x, z, 9, 2);
    const watchTop = tower(TOWER.x, TOWER.z, 18, 3, { beacon: true });
    for (let k = 0; k < 40; k++) S.blob(TOWER.x + Math.round(k * 0.25 + Math.sin(k * 0.7)), watchTop + 6 + k, TOWER.z - Math.round(k * 0.12), 1 + Math.floor(k / 14), 1, 1 + Math.floor(k / 14), 'smoke', 0.3, r);

    // The gatehouse over the passage: red posts, a lintel, an azure gable roof, banners each side.
    for (const x of [-3, 3]) S.fill(x, P + 1, Z1 - 1, x, P + 9, Z1, 'red');
    S.fill(-4, P + 8, Z1 - 2, 4, P + 9, Z1 + 1, 'wood_dark');
    S.fill(-2, P + 7, Z1, 2, P + 7, Z1, 'gold');
    S.roof(-5, Z1 - 3, 5, Z1 + 2, P + 10, { type: 'gable', mat: 'tile_azure', overhang: 1 });
    S.fill(-2, P + 1, Z1 - 1, 2, P + 6, Z1, null);
    for (const x of [-5, 5]) banner(x, P + 6, Z1 + 1, 6);
    // The paifang where the road starts up the knoll.
    const py = LOW + 5;
    for (const x of [-5, -2, 2, 5]) S.fill(x, py - 4, 44, x, py + 6, 44, 'red');
    S.fill(-6, py + 7, 44, 6, py + 7, 44, 'wood_dark'); S.fill(-3, py + 6, 44, 3, py + 6, 44, 'gold');
    S.roof(-7, 43, 7, 45, py + 8, { type: 'gable', mat: 'tile_azure', overhang: 1 });

    // ------------------------------------------------ inside
    // The command hall (north): red columns, white walls, an azure roof.
    S.hall(-10, -20, 5, 7, P, { bay: 4, wall: 'white', column: 'red', roofMat: 'tile_azure', platform: 1, storey: 5 });
    for (const x of [-6, 6]) banner(x, P, -11, 8);
    // The barracks (west): a long white hall, bunks inside.
    S.hall(-24, -12, 2, 20, P, { bay: 4, wall: 'white', column: 'wood_dark', roofMat: 'tile', platform: 1, storey: 4 });
    // The stables (east): posts, hay, a shed roof; two horses at the rail (camels stand in for them here).
    for (let z = 2; z <= 14; z += 4) for (const x of [14, 23]) S.fill(x, P + 1, z, x, P + 4, z, 'wood_dark');
    S.roof(13, 1, 24, 15, P + 5, { type: 'gable', mat: 'tile', overhang: 1 });
    for (let z = 3; z <= 13; z += 2) S.fill(21, P + 1, z, 23, P + 1, z, 'hay');
    S.camel(16, P, 5); S.camel(16, P, 10);
    // The training yard: dummies in rows, racks, two disciples sparring, an instructor.
    for (let x = -6; x <= 8; x += 4) for (const z of [-4, 0]) dummy(x, z);
    rack(-6, 6); rack(4, 6);
    S.person(-1, P, 10, 'robe_white'); S.person(1, P, 10, 'robe_blue');
    S.person(0, P, 3, 'robe_blue', 'hair');
    for (const [x, z] of [[-6, -2], [-2, -2], [2, -2], [6, -2]]) S.person(x, P, z, 'robe_white');
    // The infirmary: a white awning on posts, mats under it.
    for (const [x, z] of [[-20, 14], [-12, 14], [-20, 20], [-12, 20]]) S.fill(x, P + 1, z, x, P + 4, z, 'wood_dark');
    S.fill(-20, P + 5, 14, -12, P + 5, 20, 'awning_blue');
    for (let x = -19; x <= -14; x += 2) S.fill(x, P + 1, 16, x, P + 1, 18, 'robe_white');
    S.person(-15, P, 19, 'robe_white', 'hair');
    // The notice board: missions of the Alliance, by the gate.
    S.fill(5, P + 1, 19, 5, P + 4, 19, 'wood_dark'); S.fill(9, P + 1, 19, 9, P + 4, 19, 'wood_dark');
    S.fill(5, P + 5, 19, 9, P + 5, 19, 'tile_azure'); S.fill(6, P + 2, 19, 8, P + 4, 19, 'paper');
    S.person(7, P, 21, 'robe_red');
    // The well, supplies, a cart.
    S.walls(-9, P + 1, 13, -7, P + 1, 15, 'cobble'); S.set(-8, P + 1, 14, 'water');
    S.fill(-9, P + 2, 13, -9, P + 3, 13, 'wood'); S.fill(-7, P + 2, 15, -7, P + 3, 15, 'wood'); S.fill(-9, P + 4, 13, -7, P + 4, 15, 'wood');
    for (const [x, z, m] of [[12, 18, 'wood_light'], [13, 18, 'hay'], [12, 19, 'wood_light'], [14, 19, 'wood_light'], [13, 19, 'hay']]) S.set(x, P + 1, z, m);
    // Guards: on the walls, at the gate, atop the watchtower.
    for (const [x, z] of [[-14, Z0 + 1], [10, Z0 + 1], [X0 + 1, 2], [X1 - 1, -4], [-12, Z1 - 1], [12, Z1 - 1]]) S.person(x, P + 6, z, 'robe_blue', 'hair');
    for (const x of [-4, 4]) S.person(x, P, Z1 + 2, 'robe_blue', 'hair');
    S.person(TOWER.x, watchTop + 1, TOWER.z + 2, 'robe_blue', 'hair');
    S.person(0, P, -10, 'robe_blue', 'hair'); // the commander, at the hall's door
    // Banners along the walls.
    for (const x of [-18, -8, 8, 18]) banner(x, P + 6, Z0, 5);

    // ------------------------------------------------ outside: the travellers' tea stall, pines, fields
    for (const [x, z] of [[8, 34], [14, 34], [8, 40], [14, 40]]) S.fill(x, height(x, z) + 1, z, x, height(x, z) + 4, z, 'wood_dark');
    S.fill(8, height(11, 37) + 5, 34, 14, height(11, 37) + 5, 40, 'awning_red');
    S.fill(10, height(11, 37) + 1, 36, 12, height(11, 37) + 1, 38, 'wood');
    S.person(9, height(9, 38), 38, 'robe_red'); S.person(13, height(13, 36), 36, 'teal');
    for (let k = 0; k < 46; k++) {
      const x = Math.round((r() - 0.5) * 170), z = Math.round(-75 + r() * 180);
      if (Math.abs(x) < 34 && z > -30 && z < 50) continue;
      if (Math.abs(x) < 5) continue;
      S.pine(x, height(x, z), z, 5 + Math.floor(r() * 4), r);
    }
    for (let x = -80; x <= -40; x++) for (let z = 70; z <= 100; z++) S.set(x, LOW, z, (x + z) % 4 === 0 ? 'water' : 'rice');
    return watchTop;
  }

  // ---------------------------------------------------------------- views
  const SKY = '#C3D2DE';
  function overview() { build(); return { view: { unclamped: true, position: [78, 58, 96], target: [0, 10, 2], fov: 50 }, sky: SKY, fog: [260, 700] }; }
  function road() { build(); return { view: { unclamped: true, position: [9, 7, 70], target: [0, 12, 20], fov: 55 }, sky: SKY, fog: [200, 600] }; }
  function yard() { build(); return { view: { unclamped: true, position: [4, 36, 18], target: [0, 8, -8], fov: 64 }, sky: SKY, fog: [200, 600] }; }
  function plan() {
    const t = build();
    const marks = [
      ['1', 0, P + 11, Z1 + 1], ['2', 0, LOW + 13, 44], ['3', TOWER.x, t + 9, TOWER.z], ['4', 0, P + 10, -16], ['5', 1, P + 4, 2],
      ['6', -20, P + 8, -2], ['7', 19, P + 7, 8], ['8', -16, P + 7, 17], ['9', 7, P + 7, 19], ['10', -8, P + 6, 14],
      ['11', X0 + 2, P + 13, Z0 + 2], ['12', 11, 14, 37]];
    return { view: { unclamped: true, position: [0, 115, 78], target: [0, 8, 4], fov: 56 }, sky: SKY, fog: [400, 900], marks };
  }

  return { overview, road, yard, plan };
})();
true;
