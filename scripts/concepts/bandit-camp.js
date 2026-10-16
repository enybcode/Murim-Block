// Voxel mock-up of a great bandit camp (docs/structures/bandit-camp.md), built with ST (st-runtime.js). 1 voxel =
// 1 block. x east, z south, y up. A hollow among wooded hills, ringed by a palisade of sharpened logs and a ditch, the
// gate to the south behind spiked barricades, four watchtowers; inside, the chief's longhall to the north, a bonfire
// plaza in the middle, two bunkhouses and a ring of hide tents, the loot store, prisoners' cages, a horse pen and a
// training ring. It holds thirty hostile men, each shown where he stands; when the thirty are dead, the war drum before
// the longhall sounds and the boss comes out of it (his seat stands empty until then). One camp in each region of the
// Murim map, dressed for its land: the centre's wooded hollow, the north's snows, the west's dry lands, the south's
// bamboo and karst, the east's waterlands.
window.CAMP = (() => {
  const S = ST;
  const P = 6;            // the camp's floor
  const R = 42;           // the palisade's radius
  const GATE = 3;         // half the gate's width
  /** Each region's camp: its ground, hills, palisade, roofs, tents, trees, and whether its ditch holds water. */
  const THEMES = {
    centre: { ground: 'grass_dry', hill: 'grass', wall: 'log', tip: 'log_end', roof: 'thatch', bunk: 'hide_dark', tents: ['hide', 'hide_dark'], trees: 'pine', rock: 'stone' },
    north: { ground: 'snow', hill: 'snow', wall: 'wood_dark', tip: 'snow', roof: 'snow', bunk: 'snow', tents: ['robe_white', 'hide'], trees: 'pine', rock: 'granite' },
    west: { ground: 'sand', hill: 'loess', wall: 'rammed', tip: 'mud_brick', roof: 'thatch', bunk: 'sandstone', tents: ['hide', 'sandstone'], trees: 'none', rock: 'sandstone_layer' },
    south: { ground: 'grass_lush', hill: 'grass_lush', wall: 'bamboo', tip: 'bamboo_dark', roof: 'thatch', bunk: 'bamboo_dark', tents: ['hide', 'hide_dark'], trees: 'bamboo', rock: 'limestone' },
    east: { ground: 'grass', hill: 'grass_lush', wall: 'log', tip: 'log_end', roof: 'tile', bunk: 'tile', tents: ['hide', 'hide_dark'], trees: 'round', rock: 'stone', moat: true },
  };
  let T = THEMES.centre;

  // The hollow: flat inside the ditch, wooded hills rising all round, highest to the north.
  function height(x, z) {
    const d = Math.hypot(x, z);
    if (d <= R + 2) return P;
    if (d <= R + 4) return P - 2;                                       // the ditch
    const rise = (d - R - 4) * (0.28 + Math.max(0, -z) / 400);
    const road = Math.abs(x) < 4 && z > 0 ? 0.15 : 1;                  // the road keeps low to the south
    return Math.round(P + rise * road + Math.sin(x * 0.13) * 1.2 + Math.cos(z * 0.11) * 1.2);
  }

  /** A stilted watchtower: four log legs, a railed platform, a thatch pyramid roof, a ladder. */
  function tower(cx, cz, h = 11) {
    for (const [dx, dz] of [[-2, -2], [2, -2], [-2, 2], [2, 2]]) S.fill(cx + dx, P + 1, cz + dz, cx + dx, P + h + 3, cz + dz, 'log');
    S.fill(cx - 2, P + h, cz - 2, cx + 2, P + h, cz + 2, 'wood');
    for (let i = -2; i <= 2; i++) for (const [x, z] of [[cx + i, cz - 2], [cx + i, cz + 2], [cx - 2, cz + i], [cx + 2, cz + i]]) S.set(x, P + h + 1, z, 'wood_dark');
    S.roof(cx - 2, cz - 2, cx + 2, cz + 2, P + h + 4, { type: 'pyramid', mat: T.roof, ridge: 'wood_dark', overhang: 1 });
    S.fill(cx, P + 1, cz + 3, cx, P + h, cz + 3, 'wood_light');           // the ladder
    // A crossed brace between the legs.
    for (let y = 0; y < h - 2; y++) { S.set(cx - 2 + Math.round(y * 4 / (h - 2)), P + 2 + y, cz - 2, 'wood'); }
    return P + h;
  }

  /** A hide tent: an A-frame of hides on poles, its door flap to `face` (z sign). */
  function tent(cx, cz, w = 2, d = 3, mat = 'hide') {
    for (let k = 0; k <= w; k++) {
      const y = P + 1 + (w - k);
      for (let z = cz - d; z <= cz + d; z++) { S.set(cx - k, y, z, mat); S.set(cx + k, y, z, mat); }
    }
    S.fill(cx, P + 1, cz - d - 1, cx, P + w + 2, cz - d - 1, 'log');
    S.fill(cx, P + 1, cz + d + 1, cx, P + w + 2, cz + d + 1, 'log');
    S.set(cx, P + w + 2, cz - d - 1, 'rag');
    S.fill(cx - 1, P + 1, cz + d, cx + 1, P + 1, cz + d, null);           // the open flap
    S.set(cx, P + 1, cz + d, 'hide_dark');
  }

  /** A long log house: log walls, a door in the middle of the long side facing `side`, a thatch roof. */
  function longhouse(x0, z0, x1, z1, h, roofMat = 'thatch', wall = 'log') {
    S.walls(x0, P + 1, z0, x1, P + h, z1, wall);
    for (const [x, z] of [[x0, z0], [x1, z0], [x0, z1], [x1, z1]]) S.fill(x, P + 1, z, x, P + h + 1, z, 'log_end');
    S.roof(x0, z0, x1, z1, P + h + 1, { type: 'gable', mat: roofMat, ridge: 'wood_dark', overhang: 1 });
  }

  function crate(x, z, n = 1) { for (let i = 0; i < n; i++) S.set(x, P + 1 + i, z, i % 2 ? 'wood' : 'wood_light'); }

  // The thirty, by where they stand (mock-up figures; the real camp would hold Mountain Bandits, their chief, archers).
  const MEN = [];
  const man = (x, y, z, robe = 'robe_black', hat = 'hair', role = 'bandit') => { MEN.push(role); S.person(x, y, z, robe, hat); };

  function build(region = 'centre') {
    T = THEMES[region];
    S.clear(); MEN.length = 0;
    const r = S.seeded(1313);
    S.ground(-110, -110, 110, 120, height, (x, z, h) => (h >= P && Math.hypot(x, z) < R + 2 ? T.ground : T.hill), 'dirt', 3);
    if (T.moat) for (let x = -R - 5; x <= R + 5; x++) for (let z = -R - 5; z <= R + 5; z++) {
      const d = Math.hypot(x, z);
      if (d > R + 2 && d <= R + 4 && !(Math.abs(x) <= 2 && z > 0)) S.set(x, P - 1, z, 'water');
    }
    // Trodden ground inside, paths of mud.
    for (let x = -R; x <= R; x++) for (let z = -R; z <= R; z++) {
      const d = Math.hypot(x, z);
      if (d > R) continue;
      if (r() < 0.18 || d < 9 || Math.abs(x) < 2 || (Math.abs(z) < 2 && d < 34)) S.set(x, P, z, r() < 0.5 ? 'mud' : 'dirt');
    }
    // The road from the south, over a log bridge across the ditch.
    for (let z = R; z <= 120; z++) for (let x = -2; x <= 2; x++) S.set(x, height(x, z), z, z > R + 4 ? (r() < 0.6 ? 'mud' : 'gravel') : 'wood');

    // ------------------------------------------------ the palisade
    for (let a = 0; a < 360; a += 0.6) {
      const t = a * Math.PI / 180, x = Math.round(Math.sin(t) * R), z = Math.round(Math.cos(t) * R);
      if (z > 0 && Math.abs(x) <= GATE) continue;
      const tall = (x + z) % 2 === 0 ? 7 : 6;
      S.fill(x, P + 1, z, x, P + tall, z, T.wall);
      S.set(x, P + tall + 1, z, T.tip);                                  // the sharpened tips
    }
    // Inside it, a walkway of planks on posts, a man's height below the tips.
    for (let a = 0; a < 360; a += 0.8) {
      const t = a * Math.PI / 180, x = Math.round(Math.sin(t) * (R - 1)), z = Math.round(Math.cos(t) * (R - 1));
      if (z > 0 && Math.abs(x) <= GATE + 4) continue;
      S.set(x, P + 4, z, 'wood');
    }
    // The gate: two log towers, an open gate of logs, a skull-less trophy of red rags, barricades of spikes outside.
    for (const s of [-1, 1]) {
      const gx = s * (GATE + 3);
      S.fill(gx - 2, P + 1, R - 2, gx + 2, P + 9, R + 2, 'log');
      S.fill(gx - 1, P + 1, R - 1, gx + 1, P + 9, R + 1, null);
      S.fill(gx - 2, P + 9, R - 2, gx + 2, P + 9, R + 2, 'wood');
      S.roof(gx - 2, R - 2, gx + 2, R + 2, P + 10, { type: 'pyramid', mat: T.roof, ridge: 'wood_dark', overhang: 1 });
      S.fill(gx - 1, P + 6, R + 2, gx + 1, P + 8, R + 2, 'rag');
      // The gate leaves, swung open inward.
      S.fill(s * GATE, P + 1, R - 1, s * GATE, P + 6, R - 5, 'log');
    }
    S.fill(-GATE - 5, P + 10, R, GATE + 5, P + 10, R, 'log');               // the lintel
    for (const x of [-9, -7, 7, 9]) for (let k = 0; k < 3; k++) { S.set(x + k - 1, P + 1 + k, R + 7 + k, 'log'); S.set(x - k + 1, P + 1 + k, R + 7 + k, 'log'); }

    // ------------------------------------------------ watchtowers
    const towers = [[-30, -30], [30, -30], [-32, 26], [32, 26]];
    const tops = towers.map(([x, z]) => tower(x, z));

    // ------------------------------------------------ the chief's longhall (north)
    S.fill(-15, P + 1, -37, 15, P + 1, -23, 'stone');
    longhouse(-14, -36, 14, -24, 6, T.roof, 'log');
    S.fill(-2, P + 2, -24, 2, P + 5, -24, null);                           // the doorway
    S.fill(-3, P + 6, -23, 3, P + 7, -23, 'rag');                           // the red banner over it
    for (const x of [-5, 5]) S.fill(x, P + 2, -22, x, P + 9, -22, 'log');   // banner poles
    for (const x of [-5, 5]) S.fill(x + 1, P + 6, -22, x + 1, P + 9, -22, 'rag');
    S.fill(-1, P + 2, -35, 1, P + 3, -35, 'wood_dark');                     // the chief's seat
    S.set(-2, P + 2, -35, 'hide'); S.set(2, P + 2, -35, 'hide');
    // The war drum: a great red drum on a dark stand before the hall; it sounds when the thirty are dead.
    S.fill(6, P + 1, -20, 10, P + 1, -20, 'wood_dark'); S.fill(6, P + 1, -18, 10, P + 1, -18, 'wood_dark');
    S.fill(7, P + 2, -21, 9, P + 5, -17, 'rag'); S.fill(7, P + 6, -21, 9, P + 6, -17, 'hide');
    S.fill(6, P + 3, -20, 6, P + 4, -18, 'gold'); S.fill(10, P + 3, -20, 10, P + 4, -18, 'gold');

    // ------------------------------------------------ the bonfire plaza (centre)
    S.walls(-2, P + 1, -2, 2, P + 1, 2, 'stone');
    S.fill(-1, P + 1, -1, 1, P + 3, 1, 'fire');
    S.set(0, P + 4, 0, 'fire');
    for (let k = 0; k < 18; k++) S.blob(Math.round(Math.sin(k * 0.7) * 1.5), P + 6 + k, -Math.round(k * 0.3), 1 + Math.floor(k / 8), 1, 1 + Math.floor(k / 8), 'smoke', 0.35, r);
    for (const [x0, z0, x1, z1] of [[-6, -7, 6, -7], [-6, 7, 6, 7], [-7, -5, -7, 5], [7, -5, 7, 5]]) S.fill(x0, P + 1, z0, x1, P + 1, z1, 'log');  // benches
    S.fill(9, P + 1, -2, 9, P + 3, -2, 'wood_dark'); S.fill(11, P + 1, -2, 11, P + 3, -2, 'wood_dark'); S.fill(9, P + 4, -2, 11, P + 4, -2, 'wood_dark');
    S.set(10, P + 2, -2, 'iron'); S.set(10, P + 1, -2, 'fire');              // the cooking pot

    // ------------------------------------------------ bunkhouses and tents
    longhouse(-38, -12, -28, 10, 5, T.bunk, 'log');                        // west bunkhouse
    longhouse(28, -12, 38, 10, 5, T.bunk, 'log');                          // east bunkhouse
    S.fill(-28, P + 2, -2, -28, P + 4, 1, null); S.fill(28, P + 2, -2, 28, P + 4, 1, null);
    for (const [x, z, m] of [[-20, -16, 'hide'], [-14, -14, 'hide_dark'], [14, -14, 'hide'], [20, -16, 'hide_dark'],
      [-22, 14, 'hide_dark'], [-16, 18, 'hide'], [16, 18, 'hide_dark'], [22, 14, 'hide'], [-10, 26, 'hide'], [10, 26, 'hide_dark']]) tent(x, z, 2, 3, T.tents[m === 'hide' ? 0 : 1]);

    // ------------------------------------------------ the loot store (north-east), cages (south-west), horse pen, training ring
    for (const [x, z, n] of [[18, -28, 3], [19, -28, 2], [20, -28, 3], [18, -27, 1], [21, -27, 2], [22, -29, 1]]) crate(x, z, n);
    S.fill(24, P + 1, -30, 26, P + 1, -26, 'gold');                         // a heap of stolen silver and gold
    S.fill(25, P + 2, -29, 25, P + 2, -27, 'gold');
    S.fill(15, P + 1, -22, 20, P + 2, -20, 'wood');                         // a stolen cart
    for (const [x, z] of [[15, -23], [20, -23], [15, -19], [20, -19]]) S.set(x, P + 1, z, 'wood_dark');
    S.fill(16, P + 3, -22, 19, P + 3, -20, 'rag');                          // silk bales on it
    for (const cx of [-30, -24, -18]) {                                     // three cages of bamboo bars
      for (let x = cx - 2; x <= cx + 2; x++) for (let z = 30; z <= 34; z++)
        if ((x === cx - 2 || x === cx + 2 || z === 30 || z === 34) && (x + z) % 2 === 0) S.fill(x, P + 1, z, x, P + 4, z, 'bamboo_dark');
      S.fill(cx - 2, P + 5, 30, cx + 2, P + 5, 34, 'wood_dark');
    }
    S.person(-24, P, 32, 'robe_white', 'hair');                               // a prisoner
    for (let x = -22; x <= -8; x++) for (const z of [-28, -18]) if (x % 2 === 0) S.fill(x, P + 1, z, x, P + 2, z, 'wood');   // horse pen rails
    for (let z = -28; z <= -18; z++) if (z % 2 === 0) { S.fill(-22, P + 1, z, -22, P + 2, z, 'wood'); }
    for (const [x, z] of [[-18, -22], [-12, -24]]) { S.camel(x, P, z); }
    S.fill(-20, P + 1, -26, -19, P + 2, -25, 'hay');
    for (let a = 0; a < 360; a += 8) { const t = a * Math.PI / 180; S.set(Math.round(22 + Math.sin(t) * 6), P + 1, Math.round(26 + Math.cos(t) * 6), 'log'); }
    for (const [x, z] of [[19, 24], [25, 28]]) { S.fill(x, P + 1, z, x, P + 2, z, 'wood'); S.set(x, P + 3, z, 'hay'); }

    // ------------------------------------------------ the thirty
    man(-9, P + 1, 3, 'robe_red', 'hair', 'lieutenant');                    // (the seat stays empty: the boss's)
    man(-4, P + 1, -21, 'robe_red', 'hair', 'lieutenant'); man(4, P + 1, -21, 'robe_red', 'hair', 'lieutenant');   // 2 at the hall's door
    man(-5, P + 1, 4, 'robe_red', 'hair', 'lieutenant');                     // 1 at the fire
    towers.forEach(([x, z], i) => man(x, tops[i], z, 'robe_black', 'hair', 'archer'));   // 4 archers on the towers
    for (const s of [-1, 1]) { man(s * 2, P, R + 3, 'robe_black', 'hair', 'gate'); man(s * (GATE + 3), P + 9, R, 'robe_black', 'hair', 'gate'); }   // 4 at the gate
    for (const [x, z] of [[-5, -7], [3, -7], [-7, 2], [7, -3], [2, 7], [-3, 7]]) man(x, P + 1, z);                // 6 round the fire
    for (const [x, z] of [[20, 24], [24, 24], [22, 29], [23, 26]]) man(x, P, z);                                  // 4 sparring
    for (const [x, z] of [[17, -26], [23, -24]]) man(x, P, z);                                                    // 2 at the loot
    for (const [x, z] of [[-21, 28], [-27, 28]]) man(x, P, z);                                                    // 2 guarding the cages
    for (const [x, z] of [[-35, -20], [36, -16], [-30, 0], [30, 2]]) man(x, P + 4, z);                             // 4 on the walkway
    // Pines on the hills round about.
    for (let k = 0; k < 90; k++) {
      const x = Math.round((r() - 0.5) * 210), z = Math.round(-105 + r() * 220);
      if (Math.hypot(x, z) < R + 9 || (Math.abs(x) < 6 && z > 0)) continue;
      if (T.trees === 'pine') S.pine(x, height(x, z), z, 5 + Math.floor(r() * 4), r);
      else if (T.trees === 'bamboo') { for (let b = 0; b < 4; b++) S.bamboo(x + (b % 2) * 2, height(x, z), z + (b >> 1) * 2, 9 + Math.floor(r() * 6), r); }
      else if (T.trees === 'round') S.tree(x, height(x, z), z, { trunk: 'bark', leaves: 'leaves', h: 5, r: 3, rnd: r });
    }
    for (let k = 0; k < (T.trees === 'none' ? 60 : 30); k++) {
      const x = Math.round((r() - 0.5) * 200), z = Math.round(-100 + r() * 210);
      if (Math.hypot(x, z) < R + 8) continue;
      S.blob(x, height(x, z), z, 2, 1, 2, T.rock, 0.2, r);
    }
    return MEN.length;
  }

  /** The boss: twice a man's height, crimson armour over black, a gold crown, a great dao in his right hand. */
  function boss(x, y, z) {
    S.fill(x - 1, y + 1, z - 1, x, y + 3, z, 'robe_black');                // legs
    S.fill(x - 1, y + 4, z - 1, x, y + 6, z, 'rag');                       // the body in crimson armour
    S.fill(x - 2, y + 5, z - 1, x - 2, y + 6, z, 'gold'); S.fill(x + 1, y + 5, z - 1, x + 1, y + 6, z, 'gold');   // shoulder plates
    S.fill(x - 2, y + 2, z - 1, x - 2, y + 4, z - 1, 'skin'); S.fill(x + 1, y + 2, z - 1, x + 1, y + 4, z - 1, 'skin');   // arms
    S.fill(x - 1, y + 7, z - 1, x, y + 8, z, 'skin');                       // head
    S.fill(x - 1, y + 9, z - 1, x, y + 9, z, 'gold');                       // crown
    S.fill(x - 1, y + 8, z - 1, x, y + 8, z - 1, 'hair');                   // brows
    S.fill(x - 2, y + 1, z - 2, x - 2, y + 1, z - 6, 'iron');               // the dao, its point on the ground
    S.fill(x - 2, y + 2, z - 2, x - 2, y + 2, z - 4, 'iron');
  }

  // ---------------------------------------------------------------- the crater
  /**
   * The camp after the Warlord's fall (the Mountain-Shattering Fist's ultimate): where he lands, in the plaza, a crater
   * RC blocks across its rim and DEPTH deep, its floor of broken rock; everything that stood inside it gone; a raised rim
   * of thrown earth and rubble; round it the camp in ruins, the nearer the more broken (roofs blown off, walls down,
   * tents flattened), the palisade burst outward on the side facing the crater; embers and dust. The arena of the fight
   * (and of PvP after).
   */
  const RC = 30, DEPTH = 15;
  function crater(r) {
    const floor = d => Math.round(P - DEPTH * (1 - (d / RC) ** 2));
    for (let x = -R - 6; x <= R + 6; x++) for (let z = -R - 6; z <= R + 6; z++) {
      const d = Math.hypot(x, z);
      if (d < RC) {
        // Inside: everything above the new floor gone, the floor of broken rock and scorched earth.
        const h = floor(d);
        for (let y = h + 1; y <= P + 30; y++) S.set(x, y, z, null);
        // The deeper, the darker and more broken: scorched rock at the heart, earth up the slopes.
        const deep = 1 - d / RC;
        for (let y = h - 3; y <= h; y++) S.set(x, y, z, y < h ? 'stone' : deep > 0.75 ? (r() < 0.6 ? 'robe_black' : 'stone') : deep > 0.4 ? (r() < 0.5 ? 'stone' : 'gravel') : (r() < 0.6 ? 'dirt' : 'mud'));
        if (d > 3 && d < 9 && r() < 0.06) S.set(x, h + 1, z, 'fire');
      } else if (d < RC + 4) {
        // The rim: thrown earth and rubble, raised two or three blocks.
        const up = Math.round(3 * (1 - (d - RC) / 4) + r() * 1.2);
        for (let y = P + 1; y <= P + up; y++) S.set(x, y, z, r() < 0.5 ? 'dirt' : r() < 0.6 ? 'stone' : 'gravel');
        for (let y = P + up + 1; y <= P + 30; y++) if (S.get(x, y, z) && r() < 0.85) S.set(x, y, z, null);
      } else if (d <= R + 2) {
        // The ruins round it: the nearer, the more blown away, the high parts first.
        const blast = Math.max(0.35, 1 - (d - RC - 4) / (R + 2 - RC - 4));
        for (let y = P + 1; y <= P + 30; y++) {
          if (!S.get(x, y, z)) continue;
          const high = (y - P) / 12;
          if (r() < blast * (0.55 + high)) S.set(x, y, z, null);
        }
        if (r() < 0.04 * (1 + blast)) S.blob(x, P + 1, z, 1, 1, 1, r() < 0.5 ? 'stone' : 'dirt', 0.3, r);
        if (r() < 0.006 * (1 + 2 * blast)) S.set(x, P + 1, z, 'fire');
      }
    }
    // The palisade burst outward on the plaza's side: logs thrown flat beyond it.
    for (let k = 0; k < 40; k++) {
      const a = r() * Math.PI * 2, d = R + 3 + r() * 9, x = Math.round(Math.sin(a) * d), z = Math.round(Math.cos(a) * d);
      const len = 3 + Math.floor(r() * 4), along = r() < 0.5;
      for (let i = 0; i < len; i++) S.set(along ? x + i : x, P + 1, along ? z : z + i, 'log');
    }
    // Rubble thrown out across the rim, dust still hanging over the hollow.
    for (let k = 0; k < 70; k++) {
      const a = r() * Math.PI * 2, d = RC + r() * 20, x = Math.round(Math.sin(a) * d), z = Math.round(Math.cos(a) * d);
      S.blob(x, P + 1, z, 1, 1, 1, r() < 0.6 ? 'stone' : 'dirt', 0.4, r);
    }
    for (let k = 0; k < 90; k++) {
      const a = r() * Math.PI * 2, d = r() * RC * 0.9;
      S.blob(Math.round(Math.sin(a) * d), floor(d) + 3 + Math.floor(r() * 10), Math.round(Math.cos(a) * d), 1, 1, 1, 'smoke', 0.5, r);
    }
    return floor(0);
  }

  /**
   * The Warlord falling like a meteor: wrapped in a shell of fire from the feet up, a thick tail of flame turning to
   * smoke behind him, sparks thrown off.
   */
  function falling(x, y, z) {
    const r = S.seeded(7);
    // The fire shell under and round him (he comes down feet first), leaving his head and shoulders seen.
    for (let dx = -5; dx <= 5; dx++) for (let dz = -5; dz <= 5; dz++) for (let dy = -4; dy <= 9; dy++) {
      const d = Math.hypot(dx, dz * 1.0, (dy - 3) * 0.8);
      if (d > 4.2 && d < 5.6 && dy < 1 && dz < 1 && r() < 0.75) S.set(x + dx, y + dy, z + dz, 'fire');
    }
    for (let dx = -3; dx <= 3; dx++) for (let dz = -3; dz <= 3; dz++) for (let dy = -6; dy <= -3; dy++)
      if (Math.hypot(dx, dz, dy + 3) < 3.6) S.set(x + dx, y + dy, z + dz, 'fire');
    boss(x, y, z);
    // The tail: wide and burning near him, thinning to smoke high up.
    for (let k = 1; k < 44; k++) {
      const w = Math.max(1, Math.round(4.5 - k / 12));
      S.blob(x + Math.round(Math.sin(k * 0.7) * 0.8), y + 4 + k, z - 5 - Math.round(k * 0.6), w, 1, w, k < 14 ? 'fire' : 'smoke', 0.4, r);
    }
    // Sparks.
    for (let k = 0; k < 40; k++) S.set(x + Math.round((r() - 0.5) * 18), y + Math.round(r() * 24), z + Math.round((r() - 0.5) * 18), 'fire');
  }

  // ---------------------------------------------------------------- views
  const SKY = '#C3D2DE';
  function overview() { build(); return { view: { unclamped: true, position: [96, 74, 118], target: [0, 4, 0], fov: 52 }, sky: SKY, fog: [300, 800] }; }
  function gate() { build(); return { view: { unclamped: true, position: [6, 27, 80], target: [0, 9, 34], fov: 56 }, sky: SKY, fog: [220, 650] }; }
  function bossView() { build(); boss(0, P, -16); for (let k = 0; k < 8; k++) S.set(Math.round(Math.sin(k) * 3), P + 2 + k, -16 + Math.round(Math.cos(k) * 2), 'smoke');
    return { view: { unclamped: true, position: [-9, 15, 6], target: [0, 10, -18], fov: 58 }, sky: SKY, fog: [220, 650] }; }
  const regionView = region => () => { build(region); return { view: { unclamped: true, position: [96, 74, 118], target: [0, 4, 0], fov: 52 }, sky: region === 'north' ? '#D8E2EA' : SKY, fog: [300, 800] }; };
  function plaza() { build(); return { view: { unclamped: true, position: [-24, 22, 34], target: [2, 6, -12], fov: 64 }, sky: SKY, fog: [220, 650] }; }
  function plan() {
    const n = build();
    const marks = [
      ['1', 0, P + 16, R], ['2', -30, P + 20, -30], ['3', 0, P + 18, -30], ['4', 0, P + 10, 0], ['5', -33, P + 13, -1],
      ['6', 0, P + 8, 18], ['7', 22, P + 8, -26], ['8', -24, P + 10, 32], ['9', -15, P + 8, -23], ['10', 22, P + 8, 26],
      ['11', 0, P + 4, R + 8], ['12', 38, P + 12, 18]];
    return { view: { unclamped: true, position: [0, 150, 70], target: [0, 4, 2], fov: 58 }, sky: SKY, fog: [500, 1100], marks, men: n };
  }

  /** The fall: the camp emptied, the Warlord coming down out of the sky onto the plaza. */
  function fall() { build(); for (let x = -R; x <= R; x++) for (let z = -R; z <= R; z++) for (let y = P + 1; y <= P + 16; y++) {
      const v = S.get(x, y, z); if (v === 'robe_black' || v === 'robe_blue' || v === 'robe_white' || v === 'skin' || v === 'hair' || v === 'robe_red') S.set(x, y, z, null); }
    falling(0, P + 40, 4);
    // Where he will land: the plaza's ground already cracking, glowing.
    for (let a = 0; a < 6.28; a += 0.4) for (let k = 2; k < 9; k++) S.set(Math.round(Math.sin(a + k * 0.2) * k), P, Math.round(Math.cos(a + k * 0.2) * k), 'fire');
    return { view: { unclamped: true, position: [52, 30, 70], target: [0, 26, 0], fov: 62 }, sky: SKY, fog: [300, 800] }; }
  /** After the fall: the crater, the Warlord standing at its heart. */
  function craterView() { build(); const r = S.seeded(99); const h = crater(r); boss(0, h, 0);
    return { view: { unclamped: true, position: [96, 74, 118], target: [0, 0, 0], fov: 52 }, sky: SKY, fog: [300, 800] }; }
  /** From the rim, looking down at him. */
  function craterRim() { build(); const r = S.seeded(99); const h = crater(r); boss(0, h, 0);
    return { view: { unclamped: true, position: [-6, 22, 40], target: [0, h + 4, 0], fov: 60 }, sky: SKY, fog: [220, 650] }; }
  return { overview, gate, plaza, plan, boss: bossView, fall, crater: craterView, crater_rim: craterRim, north: regionView('north'), west: regionView('west'), south: regionView('south'), east: regionView('east'), men: () => build() };
})();
true;
