// Voxel mock-up of the Orthodox Alliance's arena (docs/structures/alliance-arena.md), built with ST (st-runtime.js).
// 1 voxel = 1 block. x east, z south, y up. A great walled court round a leitai, the raised fighting platform of the
// Murim where challenges are settled: whoever falls off loses. The captain waits on it and calls up his units: they
// rise on the four summoning seals of the leitai and pour out of the two troop halls. Stands east and west, the
// judges' pavilion north, the gate and a paifang south.
window.ARENA = (() => {
  const S = ST;
  const P = 6;                 // the court
  const W = 36;                // the walls (outer faces): a court of 73 x 73
  const L = 13, TOP = 3;       // half the leitai's side (27 x 27), its height above the court
  const SEAL = 8;              // the summoning seals on the leitai, at (+-SEAL, +-SEAL)
  const SF = L + 8, SB = W - 3, SZ = 18;   // the stands: front and back x, half their length

  function height(x, z) {
    const inside = Math.abs(x) <= W + 3 && z >= -W - 3 && z <= W + 3;
    if (inside) return P;
    const dx = Math.max(Math.abs(x) - W - 3, 0), dz = Math.max(-W - 3 - z, 0, z - W - 3);
    const d = Math.hypot(dx, dz);
    return Math.max(2, Math.round(P - d * 0.12 + Math.sin(x * 0.11) * 0.8 + Math.cos(z * 0.09) * 0.8));
  }

  function banner(x, y, z, h = 8) {
    S.fill(x, y + 1, z, x, y + h, z, 'wood_dark');
    S.fill(x + 1, y + h - 3, z, x + 1, y + h, z, 'banner_azure'); S.set(x + 1, y + h - 4, z, 'gold');
  }

  /** A covered stand (s: -1 west, 1 east): four tiers rising away from the leitai, a gallery, columns, a roof. */
  function stand(s) {
    const front = SF * s, back = SB * s, gal = (SF + 4) * s;
    for (let z = -SZ; z <= SZ; z++) {
      for (let i = 0; i < 4; i++) S.fill(front + i * s, P + 1, z, front + i * s, P + 1 + i, z, 'stone_brick');
      S.fill(Math.min(gal, back), P + 1, z, Math.max(gal, back), P + 4, z, 'stone_brick');
      S.fill(Math.min(gal, back), P + 5, z, Math.max(gal, back), P + 5, z, 'wood_dark');
    }
    for (let z = -SZ; z <= SZ; z += 6) { S.fill(front, P + 1, z, front, P + 9, z, 'red'); S.fill(back, P + 6, z, back, P + 9, z, 'red'); }
    for (let z = -SZ; z <= SZ; z++) if (z % 6 !== 0) { S.fill(back, P + 6, z, back, P + 8, z, 'white'); S.set(back, P + 9, z, 'wood_dark'); }
    S.roof(Math.min(front, back), -SZ, Math.max(front, back), SZ, P + 10, { type: 'hip', mat: 'tile_azure', overhang: 1 });
  }

  /** A troop hall in a south corner of the court (s: -1 west, 1 east), its wide doors open towards the leitai. */
  function troopHall(s) {
    const x0 = Math.min(22 * s, 32 * s), x1 = Math.max(22 * s, 32 * s), z0 = 23, z1 = 32;
    S.fill(x0 - 1, P + 1, z0 - 1, x1 + 1, P + 1, z1 + 1, 'stone_brick');
    for (let y = P + 2; y <= P + 6; y++) for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) {
      if (x !== x0 && x !== x1 && z !== z0 && z !== z1) continue;
      const column = (x - x0) % 5 === 0 && (z === z0 || z === z1) || (z - z0) % 3 === 0 && (x === x0 || x === x1);
      S.set(x, y, z, column ? 'red' : 'white');
    }
    for (let y = P + 2; y <= P + 5; y++) for (let i = 0; i < 5; i++) S.set((25 + i) * s, y, z0, 'wood_dark');   // the doors
    for (let x = x0; x <= x1; x++) S.set(x, P + 6, z0, 'wood_dark');
    S.roof(x0, z0, x1, z1, P + 7, { type: 'hip', mat: 'tile_azure', overhang: 2 });
    banner(24 * s, P + 1, z0 - 2, 8);
  }

  function build() {
    S.clear();
    const r = S.seeded(777);
    S.ground(-110, -100, 110, 120, height, () => 'grass', 'dirt', 3);

    // The road from the south, through a paifang, to the gate.
    for (let z = W + 1; z <= 120; z++) for (let x = -3; x <= 3; x++) S.set(x, height(x, z), z, Math.abs(x) === 3 ? 'cobble' : 'stone_brick');

    // The court: flagstones, a pale way from the gate to the leitai.
    S.fill(-W, P, -W, W, P, W, 'stone_brick');
    for (let x = -W + 2; x <= W - 2; x++) for (let z = -W + 2; z <= W - 2; z++) if (r() < 0.25) S.set(x, P, z, 'stone');
    S.fill(-3, P, L + 1, 3, P, W, 'marble');

    // The walls: stone, white, azure coping; the gate passage open.
    for (let x = -W; x <= W; x++) for (let z = -W; z <= W; z++) {
      const ring = Math.min(x + W, W - x, z + W, W - z);
      if (ring > 1 || (Math.abs(x) <= 4 && z >= W - 1)) continue;
      S.fill(x, P + 1, z, x, P + 4, z, 'stone_brick');
      S.fill(x, P + 5, z, x, P + 8, z, 'white');
      S.set(x, P + 9, z, 'tile_azure');
    }
    for (const x of [-26, -16, 16, 26]) banner(x, P + 9, -W + 1, 5);

    // The gate: red posts, a dark lintel faced with gold, an azure roof, lanterns.
    for (const x of [-5, 5]) S.fill(x, P + 1, W - 1, x, P + 11, W, 'red');
    S.fill(-7, P + 11, W - 1, 7, P + 11, W, 'wood_dark');
    S.fill(-4, P + 10, W, 4, P + 10, W, 'gold');
    S.roof(-7, W - 1, 7, W, P + 12, { type: 'gable', mat: 'tile_azure', overhang: 1 });
    for (const x of [-2, 2]) S.set(x, P + 8, W - 1, 'lantern');
    // The paifang where the road arrives.
    const pz = W + 14, py = height(0, pz);
    for (const x of [-7, -3, 3, 7]) S.fill(x, py + 1, pz, x, py + 9, pz, 'red');
    S.fill(-8, py + 10, pz, 8, py + 10, pz, 'wood_dark'); S.fill(-2, py + 9, pz, 2, py + 9, pz, 'gold');
    S.roof(-8, pz, 8, pz, py + 11, { type: 'gable', mat: 'tile_azure', overhang: 1 });

    // ------------------------------------------------ the leitai
    const t = P + TOP;
    for (let x = -L; x <= L; x++) for (let z = -L; z <= L; z++) {
      const edge = Math.abs(x) === L || Math.abs(z) === L, corner = Math.abs(x) === L && Math.abs(z) === L;
      S.fill(x, P + 1, z, x, t - 1, z, corner ? 'red' : edge ? 'red_wall' : 'stone_brick');
      S.set(x, t, z, corner ? 'red' : 'marble');
      if (Math.max(Math.abs(x), Math.abs(z)) === L - 2) S.set(x, t, z, 'banner');   // the ring, in red
    }
    // The four summoning seals: gold rings on the leitai where the captain's units rise.
    for (const sx of [-SEAL, SEAL]) for (const sz of [-SEAL, SEAL])
      for (let dx = -1; dx <= 1; dx++) for (let dz = -1; dz <= 1; dz++) S.set(sx + dx, t, sz + dz, dx || dz ? 'gold' : 'tile_azure');
    for (const x of [-L, L]) for (const z of [-L, L]) { S.fill(x, t + 1, z, x, t + 5, z, 'red'); S.set(x, t + 6, z, 'lantern'); }
    for (let i = 1; i <= TOP; i++) S.fill(-3, P + 1, L + i, 3, t - i, L + i, 'stone_brick');   // steps up from the gate side
    for (const x of [-L - 2, L + 2]) for (const z of [-L - 2, L + 2]) banner(x, P, z, 11);

    // ------------------------------------------------ stands, troop halls, the judges' pavilion
    stand(-1); stand(1);
    troopHall(-1); troopHall(1);
    const pz0 = -W + 3, pz1 = -W + 11, px = 13;
    S.fill(-px, P + 1, pz0, px, P + 2, pz1, 'stone_brick');
    for (let i = 1; i <= 2; i++) S.fill(-4, P + 1, pz1 + i, 4, P + 3 - i, pz1 + i, 'stone_brick');
    for (let y = P + 3; y <= P + 9; y++) for (let x = -px; x <= px; x++) for (let z = pz0; z <= pz1; z++) {
      const back = z === pz0, side = Math.abs(x) === px, front = z === pz1;
      const column = (front || back) && Math.abs(x) % 4 === 1 || side && (back || front);
      if (column) S.set(x, y, z, 'red');
      else if (y === P + 9 && (front || back || side)) S.set(x, y, z, 'wood_dark');
      else if (back || side) S.set(x, y, z, 'white');
    }
    S.roof(-px, pz0, px, pz1, P + 10, { type: 'hip', mat: 'tile_azure', overhang: 2 });
    // The captain's seat between gold, the Alliance's banners behind it, two great bells.
    S.set(0, P + 3, pz0 + 1, 'wood_dark'); S.set(0, P + 4, pz0 + 1, 'wood_dark'); S.set(-1, P + 3, pz0 + 1, 'gold'); S.set(1, P + 3, pz0 + 1, 'gold');
    for (const x of [-6, -3, 3, 6]) S.fill(x, P + 4, pz0 + 1, x, P + 7, pz0 + 1, 'banner_azure');
    for (const x of [-9, 9]) S.fill(x, P + 3, pz0 + 2, x, P + 5, pz0 + 2, 'bronze');

    // ------------------------------------------------ round the court
    for (const x of [-12, 12]) for (const z of [20, 28]) { S.set(x, P + 1, z, 'stone_brick'); S.set(x, P + 2, z, 'fire'); }   // braziers
    for (const x of [-8, 8]) { S.set(x, P + 1, 31, 'wood_dark'); S.fill(x, P + 2, 31, x, P + 3, 31, 'red_wall'); }            // war drums
    for (const x0 of [-20, 17]) for (let i = 0; i < 4; i++) { S.set(x0 + i, P + 1, -W + 2, 'wood_dark'); S.set(x0 + i, P + 2, -W + 2, i % 2 ? 'armor' : 'wood_light'); }
    for (const z of [18, 24, 30]) for (const x of [-5, 5]) { S.fill(x, P + 1, z, x, P + 2, z, 'stone_brick'); S.set(x, P + 3, z, 'lantern'); }

    // ------------------------------------------------ people
    S.person(0, t, -2, 'robe_blue', 'gold');                               // the captain, on the leitai
    S.person(0, t, 6, 'robe_white');                                       // a challenger
    // His units, called up: one on each seal, in a pillar of Qi; more pouring out of the troop halls.
    for (const sx of [-SEAL, SEAL]) for (const sz of [-SEAL, SEAL]) {
      S.person(sx, t, sz, 'robe_blue', 'hair');
      for (let y = t + 5; y <= t + 9; y++) if (r() < 0.7) S.set(sx, y, sz, 'qi');
    }
    for (const s of [-1, 1]) for (const [x, z] of [[24, 20], [27, 19], [30, 21], [26, 16]]) S.person(x * s, P, z, 'robe_blue', 'hair');
    for (const x of [-3, 3]) S.person(x, P, W - 3, 'robe_blue', 'hair');      // guards at the gate
    for (let k = 0; k < 20; k++) {                                           // spectators on the stands
      const s = k % 2 ? 1 : -1, tier = Math.floor(r() * 5), z = Math.round((r() - 0.5) * SZ * 2);
      const x = tier < 4 ? (SF + tier) * s : (SF + 6 + Math.floor(r() * 4)) * s, y = tier < 4 ? P + 1 + tier : P + 5;
      S.person(x, y, z, r() < 0.5 ? 'robe_white' : 'robe_blue');
    }

    // Pines round about.
    for (let k = 0; k < 50; k++) {
      const x = Math.round((r() - 0.5) * 210), z = Math.round(-95 + r() * 210);
      if (Math.abs(x) < 46 && z > -46 && z < 60) continue;
      if (Math.abs(x) < 6) continue;
      S.pine(x, height(x, z), z, 5 + Math.floor(r() * 4), r);
    }
  }

  // ---------------------------------------------------------------- views
  const SKY = '#C3D2DE';
  function overview() { build(); return { view: { unclamped: true, position: [100, 72, 118], target: [0, 6, 0], fov: 50 }, sky: SKY, fog: [320, 800] }; }
  function gate() { build(); return { view: { unclamped: true, position: [10, 11, 84], target: [0, 13, 20], fov: 55 }, sky: SKY, fog: [240, 700] }; }
  function leitai() { build(); return { view: { unclamped: true, position: [4, 26, 30], target: [0, 9, -10], fov: 64 }, sky: SKY, fog: [240, 700] }; }
  function plan() {
    build();
    const marks = [
      ['1', 0, P + 15, W], ['2', 0, P + 16, W + 14], ['3', 0, P + 9, 2], ['4', -27, P + 17, 0], ['5', 27, P + 17, 0],
      ['6', 0, P + 19, -W + 7], ['7', SEAL, P + 9, SEAL], ['8', -27, P + 14, 27], ['9', 27, P + 14, 27],
      ['10', L + 2, P + 15, -L - 2], ['11', -12, P + 6, 24], ['12', 8, P + 7, 31]];
    return { view: { unclamped: true, position: [0, 150, 90], target: [0, 6, 4], fov: 56 }, sky: SKY, fog: [500, 1100], marks };
  }

  return { overview, gate, leitai, plan };
})();
true;
