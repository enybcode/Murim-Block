// Voxel mock-up of the Orthodox Alliance's arena (docs/structures/alliance-arena.md), built with ST (st-runtime.js).
// 1 voxel = 1 block. x east, z south, y up. A walled court round a leitai, the raised fighting platform of the Murim
// where challenges are settled: whoever falls off loses. The captain waits on it. Stands east and west, the judges'
// pavilion north, the gate and a paifang south.
window.ARENA = (() => {
  const S = ST;
  const P = 6;                 // the court
  const W = 24;                // the walls (outer faces)
  const L = 8, TOP = 3;        // half the leitai's side, its height above the court

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

  /** A covered stand ({@code s}: -1 west, 1 east): three tiers rising away from the leitai, a gallery, columns, a roof. */
  function stand(s) {
    const front = 14 * s, back = 22 * s;
    for (let z = -14; z <= 14; z++) {
      for (let i = 0; i < 3; i++) S.fill(front + i * s, P + 1, z, front + i * s, P + 1 + i, z, 'stone_brick');
      S.fill(Math.min(17 * s, back), P + 1, z, Math.max(17 * s, back), P + 3, z, 'stone_brick');
      S.fill(Math.min(17 * s, back), P + 4, z, Math.max(17 * s, back), P + 4, z, 'wood_dark');
    }
    for (let z = -14; z <= 14; z += 7) { S.fill(front, P + 1, z, front, P + 7, z, 'red'); S.fill(back, P + 5, z, back, P + 7, z, 'red'); }
    for (let z = -14; z <= 14; z++) if (z % 7 !== 0) { S.fill(back, P + 5, z, back, P + 6, z, 'white'); S.set(back, P + 7, z, 'wood_dark'); }
    S.roof(Math.min(front, back), -14, Math.max(front, back), 14, P + 8, { type: 'hip', mat: 'tile_azure', overhang: 2 });
  }

  function build() {
    S.clear();
    const r = S.seeded(777);
    S.ground(-90, -80, 90, 100, height, () => 'grass', 'dirt', 3);

    // The road from the south, through a paifang, to the gate.
    for (let z = W + 1; z <= 100; z++) for (let x = -2; x <= 2; x++) S.set(x, height(x, z), z, Math.abs(x) === 2 ? 'cobble' : 'stone_brick');

    // The court: flagstones.
    S.fill(-W, P, -W, W, P, W, 'stone_brick');
    for (let x = -W + 2; x <= W - 2; x++) for (let z = -W + 2; z <= W - 2; z++) if (r() < 0.25) S.set(x, P, z, 'stone');

    // The walls: stone, white, azure coping; the gate passage open.
    for (let x = -W; x <= W; x++) for (let z = -W; z <= W; z++) {
      const ring = Math.min(x + W, W - x, z + W, W - z);
      if (ring > 1 || (Math.abs(x) <= 3 && z >= W - 1)) continue;
      S.fill(x, P + 1, z, x, P + 3, z, 'stone_brick');
      S.fill(x, P + 4, z, x, P + 6, z, 'white');
      S.set(x, P + 7, z, 'tile_azure');
    }
    for (const x of [-16, -8, 8, 16]) banner(x, P + 7, -W + 1, 5);

    // The gate: red posts, a dark lintel faced with gold, an azure roof.
    for (const x of [-4, 4]) S.fill(x, P + 1, W - 1, x, P + 8, W, 'red');
    S.fill(-5, P + 8, W - 1, 5, P + 8, W, 'wood_dark');
    S.fill(-3, P + 7, W, 3, P + 7, W, 'gold');
    S.roof(-5, W - 1, 5, W, P + 9, { type: 'gable', mat: 'tile_azure', overhang: 1 });
    S.set(0, P + 6, W - 1, 'lantern');
    // The paifang where the road arrives.
    const pz = W + 10, py = height(0, pz);
    for (const x of [-5, -2, 2, 5]) S.fill(x, py + 1, pz, x, py + 7, pz, 'red');
    S.fill(-6, py + 8, pz, 6, py + 8, pz, 'wood_dark'); S.fill(-1, py + 7, pz, 1, py + 7, pz, 'gold');
    S.roof(-6, pz, 6, pz, py + 9, { type: 'gable', mat: 'tile_azure', overhang: 1 });

    // ------------------------------------------------ the leitai
    const t = P + TOP;
    for (let x = -L; x <= L; x++) for (let z = -L; z <= L; z++) {
      const edge = Math.abs(x) === L || Math.abs(z) === L, corner = Math.abs(x) === L && Math.abs(z) === L;
      S.fill(x, P + 1, z, x, t - 1, z, corner ? 'red' : edge ? 'red_wall' : 'stone_brick');
      S.set(x, t, z, corner ? 'red' : 'marble');
      if (Math.max(Math.abs(x), Math.abs(z)) === L - 2) S.set(x, t, z, 'banner');   // the ring, in red
    }
    for (const x of [-L, L]) for (const z of [-L, L]) { S.fill(x, t + 1, z, x, t + 4, z, 'red'); S.set(x, t + 5, z, 'lantern'); }
    for (let i = 1; i <= TOP; i++) S.fill(-2, P + 1, L + i, 2, t - i, L + i, 'stone_brick');   // steps up from the gate side
    // Banner poles at its corners.
    for (const x of [-10, 10]) for (const z of [-10, 10]) banner(x, P, z, 9);

    // ------------------------------------------------ stands and the judges' pavilion
    stand(-1); stand(1);
    // The pavilion: a raised floor, red columns open to the leitai, white walls, an azure hip roof.
    S.fill(-9, P + 1, -22, 9, P + 2, -15, 'stone_brick');
    S.fill(-3, P + 1, -14, 3, P + 1, -13, 'stone_brick');
    S.fill(-3, P + 2, -14, 3, P + 2, -14, 'stone_brick');
    for (let y = P + 3; y <= P + 8; y++) for (let x = -9; x <= 9; x++) for (let z = -22; z <= -15; z++) {
      const back = z === -22, side = Math.abs(x) === 9, front = z === -15;
      const column = (front || back) && Math.abs(x) % 3 === 0 || side && (back || front);
      if (column) S.set(x, y, z, 'red');
      else if (y === P + 8 && (front || back || side)) S.set(x, y, z, 'wood_dark');
      else if (back || side) S.set(x, y, z, 'white');
    }
    S.roof(-9, -22, 9, -15, P + 9, { type: 'hip', mat: 'tile_azure', overhang: 2 });
    // The captain's seat between gold, the Alliance's banners behind it, two bells.
    S.set(0, P + 3, -21, 'wood_dark'); S.set(0, P + 4, -21, 'wood_dark'); S.set(-1, P + 3, -21, 'gold'); S.set(1, P + 3, -21, 'gold');
    for (const x of [-4, -2, 2, 4]) S.fill(x, P + 4, -21, x, P + 6, -21, 'banner_azure');
    for (const x of [-6, 6]) S.fill(x, P + 3, -20, x, P + 4, -20, 'bronze');

    // ------------------------------------------------ round the court
    for (const x of [-11, 11]) { S.set(x, P + 1, 17, 'stone_brick'); S.set(x, P + 2, 17, 'fire'); }          // braziers
    for (const x of [-7, 7]) { S.set(x, P + 1, 20, 'wood_dark'); S.set(x, P + 2, 20, 'red_wall'); }          // war drums
    for (const x0 of [-12, 9]) for (let i = 0; i < 4; i++) { S.set(x0 + i, P + 1, 21, 'wood_dark'); S.set(x0 + i, P + 2, 21, i % 2 ? 'armor' : 'wood_light'); }
    for (const z of [14, 18]) for (const x of [-4, 4]) { S.fill(x, P + 1, z, x, P + 2, z, 'stone_brick'); S.set(x, P + 3, z, 'lantern'); }

    // ------------------------------------------------ people
    S.person(0, t, 0, 'robe_blue', 'gold');                                // the captain, on the leitai
    S.person(0, t, 4, 'robe_white');                                       // a challenger
    for (const x of [-2, 2]) S.person(x, P, 21, 'robe_blue', 'hair');      // guards at the gate
    for (const [x, z] of [[-19, -6], [-19, 2], [-15, 6], [-16, -10], [19, 0], [15, -4], [16, 8], [20, -10]])
      S.person(x, x < 0 ? (x <= -17 ? P + 4 : P + 1 + (-14 - x)) : (x >= 17 ? P + 4 : P + 1 + (x - 14)), z, r() < 0.5 ? 'robe_white' : 'robe_blue');

    // Pines round about.
    for (let k = 0; k < 40; k++) {
      const x = Math.round((r() - 0.5) * 170), z = Math.round(-75 + r() * 170);
      if (Math.abs(x) < 32 && z > -32 && z < 44) continue;
      if (Math.abs(x) < 5) continue;
      S.pine(x, height(x, z), z, 5 + Math.floor(r() * 4), r);
    }
  }

  // ---------------------------------------------------------------- views
  const SKY = '#C3D2DE';
  function overview() { build(); return { view: { unclamped: true, position: [70, 52, 82], target: [0, 6, 0], fov: 50 }, sky: SKY, fog: [260, 700] }; }
  function gate() { build(); return { view: { unclamped: true, position: [8, 9, 58], target: [0, 10, 10], fov: 55 }, sky: SKY, fog: [200, 600] }; }
  function leitai() { build(); return { view: { unclamped: true, position: [3, 21, 21], target: [0, 9, -8], fov: 62 }, sky: SKY, fog: [200, 600] }; }
  function plan() {
    build();
    const marks = [
      ['1', 0, P + 12, W], ['2', 0, P + 12, W + 10], ['3', 0, P + 8, 0], ['4', -18, P + 14, 0], ['5', 18, P + 14, 0],
      ['6', 0, P + 16, -18], ['7', 10, P + 12, -10], ['8', -11, P + 5, 17], ['9', 7, P + 5, 20], ['10', 10, P + 5, 21]];
    return { view: { unclamped: true, position: [0, 105, 62], target: [0, 6, 2], fov: 56 }, sky: SKY, fog: [400, 900], marks };
  }

  return { overview, gate, leitai, plan };
})();
true;
