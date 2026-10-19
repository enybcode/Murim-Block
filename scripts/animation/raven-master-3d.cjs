// The Raven Master (鸦主), the wandering master of the Raven Messenger Art (docs/combat/styles/raven-art.md), in three
// CONCEPT looks for Enzo to choose from, on Epic Fight's biped skeleton (mesh-kit.cjs), each its own Blockbench project.
// The concepts write no game assets, only art/ (the scene with his iron fan and his ravens is put
// together by scripts/concepts/raven-master-preview.cjs). Cloth only, as all our models: the iron fan is its own item.
// Palette of the familiar already chosen, the Shadow Messenger Raven (raven-familiars.cjs): black with a night-blue sheen,
// amber eyes, gold rings, parchment.
//   1 hermit  the Shadow Hermit: a thin man under a wide black conical hat whose black gauze veil falls round his head
//             (parted in front: the amber eyes show through), a mantle of black feathers over the shoulders and a long
//             feather cloak to the calves, its hem ragged in feather tips; a charcoal robe, a night-blue sash, an amber cord;
//   2 mask    the Raven Mask: a black lacquered half mask with a crow's beak and amber eye rings, long white hair falling
//             down the back; a blue-black robe trimmed in night blue; three tiers of feather pauldrons on each shoulder;
//             a raven on his left shoulder (in the preview);
//   3 lord    the Messenger Lord: a noble scholar in deep indigo over black, hems and lapels trimmed in gold-amber, a
//             standing collar of black feathers; the familiar's message case, black lacquer ringed in gold, slung across
//             his back; his hair in a topknot under a small gold crown, a black feather pin through it; a fine moustache.
//   art/blockbench/raven_master_<n>.bbmodel (+ raven_master/<n>.png)
// The Raven Mask (2) was chosen and is in the game (the Raven Master, docs/mobs/raven-master.md): `game` writes it,
// his shadow raven perched on his left shoulder as part of his mesh (the familiar's shape, raven-familiars.cjs):
//   assets/murimblock/animmodels/entity/raven_master.json             the Epic Fight mesh (a HumanoidMesh);
//   assets/murimblock/textures/entity/raven_master.png (+ its faces)  one atlas per face (client/MobFaces: the mask
//                                                                      hides his eyes, the mouth below it moves);
//   art/blockbench/raven_master.bbmodel (+ raven_master/game.png)     the game model, to look at and edit.
// Usage: node scripts/animation/raven-master-3d.cjs [1|2|3]   (all three concepts by default)
//        node scripts/animation/raven-master-3d.cjs game      (the game's model)
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const hd = require('./hd-face.cjs');
const { isSide, all, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/** The familiar's colours (raven-familiars.cjs, shadow). */
const RAVEN = { Y: '#30374b', K: '#262833', N: '#3d5274', O: '#f2b33d', G: '#d9a83f', W: '#e9dcb4', L: '#141318' };

/**
 * Colour letters: S skin, A hair, K feather black, N night-blue sheen, O amber, G gold, W parchment, L black lacquer,
 * R robe, Q its trim, U under-robe, B sash, P trousers, F shoes, H hat, V veil. Lower case: a shade darker.
 */
const VARIANTS = {
  1: { key: 'hermit', name: 'Shadow Hermit',
    c: { ...RAVEN, S: '#d8c3ae', A: '#16161c', R: '#1f2029', Q: '#343a4e', U: '#2b2f3d', B: '#26314c', P: '#15161c', F: '#0f1014', H: '#17171d', V: '#1b1c24' },
    t: { skin: '#d8c3ae', hair: '#16161c', iris: '#f2b33d', fringe: [6, 6, 5, 4, 3, 2, 2, 1, 1, 2, 2, 3, 4, 5, 6, 6], brow: 'flat', eyes: 'open', mouth: 'stern', locks: 8 } },
  2: { key: 'mask', name: 'Raven Mask',
    c: { ...RAVEN, S: '#e2cdb8', A: '#e9e6e0', R: '#1a2030', Q: '#3d5274', U: '#11141c', B: '#141318', P: '#14161e', F: '#0e0f13', H: '#141318', V: '#1b1c24' },
    t: { skin: '#e2cdb8', hair: '#e9e6e0', iris: '#f2b33d', fringe: [7, 6, 5, 4, 3, 3, 2, 2, 2, 2, 3, 3, 4, 5, 6, 7], brow: 'flat', eyes: 'open', mouth: 'stern', locks: 16 } },
  3: { key: 'lord', name: 'Messenger Lord',
    c: { ...RAVEN, S: '#e3c6a8', A: '#141218', R: '#272650', Q: '#d9a83f', U: '#141318', B: '#141318', P: '#16151f', F: '#0f0e14', H: '#141218', V: '#1b1c24' },
    t: { skin: '#e3c6a8', hair: '#141218', iris: '#f2b33d', fringe: [3, 3, 2, 2, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3], brow: 'flat', eyes: 'narrow', mouth: 'calm', moustache: 'long' } },
};

// ---------------------------------------------------------------- painting helpers

/** Overlapping rows of feathers, two texels high, each row offset; a dark edge under each feather, a sheen now and then. */
const feathers = (L = 'K', S = 'N') => (f, u, v) => {
  if (!isSide(f)) return f === 'up' ? L : L.toLowerCase();
  // Scales two texels wide and high, each row offset by one: a lit top left (Y, the soft sheen), a shadow bottom right
  // under the next row; now and then a feather catches the night-blue sheen (S).
  const row = Math.floor(v / 2), x = (u + row) % 2, top = v % 2 === 0;
  if (!top && x === 1) return L.toLowerCase();
  if (top && x === 0) return (Math.floor((u + row) / 2) * 5 + row * 3) % 13 === 0 ? S : 'Y';
  return L;
};
/** Ragged feather tips on a side's last two rows: points every third texel. */
const ragged = (paint, depth = 2) => (f, u, v, w, h, o) => {
  if (isSide(f)) {
    if (v === h - 1) return u % 3 === 1 ? paint(f, u, v, w, h, o).toLowerCase() : null;
    if (depth > 1 && v === h - 2) return u % 3 === 2 ? null : paint(f, u, v, w, h, o);
  }
  return paint(f, u, v, w, h, o);
};
/** Cloth: a fold every third column, a hem colour on the last rows. */
const cloth = (L, hem, hemRows = 1, hem2) => (f, u, v, w, h) => {
  if (isSide(f) && hem && v >= h - hemRows) return hem2 && v === h - hemRows ? hem2 : hem;
  return isSide(f) && u % 3 === 2 ? L.toLowerCase() : L;
};

// ---------------------------------------------------------------- the model

function build(n, { game = false } = {}) {
  const V = VARIANTS[n];
  const PIECES = [];
  const piece = (bone, part, name, from, to, paint, extra = {}) => PIECES.push({ bone, part, name, from, to, paint, ...extra });
  const pair = (bone, part, name, from, to, paint, extra = {}) => {
    const l = b => b.replace(/_R$/, '_L');
    piece(bone, part, name + '_r', from, to, paint, extra);
    piece(l(bone), part.replace('right', 'left'), name + '_l', [-to[0], from[1], from[2]], [-from[0], to[1], to[2]], paint,
      extra.pivot ? { ...extra, pivot: [-extra.pivot[0], extra.pivot[1], extra.pivot[2]] } : extra);
  };
  const thin = n === 1;

  // ---- the head: hair round the sides and back, the 16 x 16 face on its plate
  piece('Head', 'head', 'head', [-4, 24, -4], [4, 32, 4], (f, u, v, w, h) => {
    if (f === 'north') return 'S';
    if (f === 'up') return 'A';
    if (f === 'down') return 's';
    if (f === 'south') return (u + v) % 3 === 0 ? 'a' : 'A';
    const front = f === 'east' ? w - 1 - u : u;
    if (n === 3 && front <= 2 && v >= 3) return front === 2 && v === 4 ? 's' : 'S';
    if (front <= 1 && v >= 2 && n !== 2) return 'S';
    return (u + v) % 3 === 0 ? 'a' : 'A';
  });
  let facePaint = hd.paint(V.t);
  if (n === 2) {
    // The half mask over the 16 x 16 face: black lacquer from the brow to the cheekbones, amber rings round the eyes, a
    // night-blue gloss on its upper edge; the mouth and chin left bare.
    const base = hd.paint(V.t);
    facePaint = (f, u, v, w, h, o) => {
      if (f !== 'north') return null;
      const eyeL = u >= 2 && u <= 7, eyeR = u >= 8 && u <= 13;
      if (v >= 4 && v <= 11 && !(v === 11 && (u <= 2 || u >= 13)) && !(v === 10 && (u === 0 || u === 15))) {
        if (v >= 7 && v <= 10 && (eyeL || eyeR)) {
          const x = eyeL ? u - 2 : u - 8;
          // The ring (amber), the hole (dark), the eye glowing amber in it.
          if (v === 7 || v === 10 || x === 0 || x === 5) return (x === 0 || x === 5) && (v === 7 || v === 10) ? 'L' : 'O';
          return (eyeL ? x === 3 || x === 2 : x === 2 || x === 3) && v === 9 ? '#ffd27a' : v === 8 && (x === 2 || x === 3) ? 'O' : '#0b0a0e';
        }
        if (v === 4) return u % 5 === 2 ? 'N' : 'l';
        return (u + v) % 7 === 0 ? 'N' : 'L';
      }
      return base(f, u, v, w, h, o);
    };
  }
  piece('Head', 'head', 'face', ...hd.PLATE, facePaint, { density: hd.DENSITY });

  // ---- the body: the robe crossed right over left, the trim on the lapels, the under-robe at the neck
  const bw = thin ? 3.6 : 4;
  piece('Chest', 'torso', 'chest', [-bw, 18, -2], [bw, 24, 2], (f, u, v, w) => {
    if (f === 'north') return v <= 1 && u >= 3 && u <= w - 4 ? 'U' : 'R';
    return isSide(f) && u % 3 === 2 ? 'r' : 'R';
  }, { hinge: { y: 18, bone: 'Torso' } });
  piece('Torso', 'torso', 'belly', [-bw, 13, -2], [bw, 18, 2], (f, u) => (f === 'down' ? 'P' : isSide(f) && u % 3 === 2 ? 'r' : 'R'),
    { hinge: { y: 18, bone: 'Chest' } });
  // The lapels in steps, from his left shoulder down to his right hip.
  const lapel = n === 3 ? 'G' : 'Q';
  [[-1.8, 22.6], [-0.6, 21.6], [0.6, 20.6], [1.8, 19.6], [2.8, 18.6]].forEach(([x, y], i) =>
    piece('Chest', 'jacket', 'lapel_' + i, [x, y, -2.4], [x + 1.2, y + 1.0, -2.0], all(lapel)));
  piece('Torso', 'jacket', 'front_edge', [2.8, 13.0, -2.35], [3.6, 18.7, -2.0], all(lapel));
  // The sash.
  const sashBand = n === 1 ? 'O' : n === 2 ? 'N' : 'G';
  piece('Torso', 'jacket', 'sash', [-bw - 0.4, 15.4, -2.45], [bw + 0.4, 17.4, 2.45], rim((f, u, v) => (v === 1 && isSide(f) ? sashBand : (u + v) % 5 === 0 ? 'b' : 'B')));
  // The amber cord and its tassel (1), a lacquer plaque (2), a gold clasp and a jade-amber pendant (3).
  if (n === 1) {
    piece('Torso', 'jacket', 'cord', [-2.6, 11.0, -2.85], [-2.0, 15.4, -2.5], (f, u, v, w, h) => (v >= h - 2 ? (u % 2 ? null : 'o') : 'O'));
    piece('Torso', 'jacket', 'cord_knot', [-3.0, 15.0, -3.0], [-1.6, 16.4, -2.45], all('O'));
  } else if (n === 2) {
    piece('Torso', 'jacket', 'plaque', [-1.2, 15.5, -2.9], [1.2, 17.3, -2.45], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 'O' : 'L'));
  } else {
    piece('Torso', 'jacket', 'clasp', [-1.3, 15.5, -2.95], [1.3, 17.3, -2.45], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? 'L' : 'G'));
    piece('Torso', 'jacket', 'pendant_cord', [2.35, 12.0, -3.0], [2.65, 15.4, -2.7], all('G'));
    piece('Torso', 'jacket', 'pendant', [1.8, 10.6, -3.2], [3.2, 12.0, -2.6], (f, u, v) => (u === 1 && v === 0 ? 'W' : 'O'));
    piece('Torso', 'jacket', 'pendant_tassel', [2.15, 8.4, -3.05], [2.85, 10.6, -2.75], (f, u, v, w, h) => (v === h - 1 ? (u % 2 ? null : 'k') : 'K'));
  }

  // ---- the arms: wide sleeves, the cuff trimmed, the hands
  for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
    const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
    const [a0, a1] = thin ? xs(4.5, 7.5) : xs(4, 8), [h0, h1] = thin ? xs(4.7, 7.3) : xs(4.4, 7.6), [s0, s1] = thin ? xs(3.8, 8.2) : xs(3.4, 8.6);
    const s = side.toLowerCase(), cuff = n === 3 ? 'G' : n === 2 ? 'Q' : 'q';
    piece('Arm_' + side, part + 'Arm', 'upper_arm_' + s, [a0, 17, -2], [a1, 24, 2], cloth('R'), { hinge: { y: 17, bone: 'Hand_' + side } });
    piece('Hand_' + side, part + 'Arm', 'hand_' + s, [h0, 10.6, -1.7], [h1, 13.2, 1.7], (f, u, v) => (f === 'down' ? 's' : v === 0 ? 'U' : 'S'));
    piece('Hand_' + side, part + 'Sleeve', 'sleeve_' + s, [s0, 12.4, -2.7], [s1, 17.0, 2.7],
      rim(cloth('R', cuff, n === 3 ? 2 : 1, n === 3 ? 'K' : undefined)), { hinge: { y: 17, bone: 'Arm_' + side } });
    piece('Hand_' + side, part + 'Sleeve', 'sleeve_hang_' + s, [s0, n === 3 ? 9.4 : 10.6, 0.7], [s1, 12.4, 2.7],
      (f, u, v, w, h) => (f === 'up' ? null : isSide(f) && v === h - 1 ? cuff : f === 'down' ? 'r' : 'R'));
  }

  // ---- the legs: the robe's skirt split over each leg, trousers, wrapped shins, cloth shoes
  const hemY = n === 3 ? 0.8 : n === 1 ? 3.2 : 2.4;
  for (const [side, sx, part] of [['R', 1, 'right'], ['L', -1, 'left']]) {
    const xs = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
    const [t0, t1] = thin ? xs(0.4, 3.6) : xs(0, 4), [u0, u1] = xs(-0.3, 4.3), [l0, l1] = xs(-0.55, 4.55), [d0, d1] = thin ? xs(0.2, 3.8) : xs(-0.2, 4.2);
    const s = side.toLowerCase(), inner = sx > 0 ? 'west' : 'east';
    const open = (f, u, v, w, h) => f === inner || f === 'up' || (f === 'down' && u > 0 && v > 0 && u < w - 1 && v < h - 1);
    piece('Thigh_' + side, part + 'Leg', 'thigh_' + s, [t0, 6, -2], [t1, 13, 2], all('P'), { hinge: { y: 6, bone: 'Leg_' + side } });
    piece('Leg_' + side, part + 'Leg', 'shin_' + s, [t0, 0, -2], [t1, 6, 2], (f, u, v) => (isSide(f) && (u + v) % 3 === 0 ? 'p' : 'P'), { hinge: { y: 6, bone: 'Thigh_' + side } });
    piece('Thigh_' + side, part + 'Pants', 'skirt_' + s, [u0, 6.6, -2.4], [u1, 13.2, 2.4], (f, u, v, w, h) => (open(f, u, v, w, h) ? null : u % 3 === 2 ? 'r' : 'R'));
    const hem = n === 3 ? 'G' : n === 2 ? 'Q' : 'q';
    piece('Leg_' + side, part + 'Pants', 'skirt_hem_' + s, [l0, hemY, -2.65], [l1, 6.6, 2.65], (f, u, v, w, h) => {
      if (open(f, u, v, w, h)) return null;
      if (v === h - 1) return hem;
      if (n === 3 && v === h - 2) return 'K';
      return u % 3 === 2 ? 'r' : 'R';
    });
    piece('Leg_' + side, part + 'Pants', 'shoe_' + s, [d0, 0, -2.8], [d1, 1.1, 2.2], (f, u, v, w, h) => (f === 'down' ? 'f' : isSide(f) && v === 0 ? 'f' : 'F'));
  }

  // ---------------------------------------------------------------- what makes each one
  if (n === 1) {
    // The hat (a wide black cone in five steps), a night-blue band, an amber bead on top.
    const HAT = [[9.0, 30.6, 31.4], [7.2, 31.4, 32.4], [5.4, 32.4, 33.6], [3.6, 33.6, 34.8], [1.9, 34.8, 36.2]];
    HAT.forEach(([r, y0, y1], i) => piece('Head', 'hat', 'hat_' + i, [-r, y0, -r], [r, y1, r],
      (f, u, v, w, h) => (i === 1 && isSide(f) ? (u % 5 === 2 ? 'n' : 'N') : f === 'down' ? 'h' : isSide(f) && v === h - 1 ? 'h' : (u + v) % 4 === 0 ? 'h' : 'H')));
    piece('Head', 'hat', 'hat_bead', [-0.7, 36.2, -0.7], [0.7, 37.4, 0.7], (f, u, v) => (v === 0 && isSide(f) ? '#ffd27a' : 'O'));
    // The veil of black gauze from the brim: parted in front (a few strands over the face), full at the sides and back,
    // its hem ragged.
    const R = 7.6, y0 = 23.2, y1 = 30.6;
    const gauze = (f, u, v, w, h) => (isSide(f) ? (v >= h - 2 ? ((u + v) % 2 ? null : 'v') : v > 1 && (u * 2 + v) % 5 === 0 ? null : (u + v * 2) % 5 === 0 ? 'v' : 'V') : null);
    piece('Head', 'hat', 'veil_back', [-R, y0, R - 0.3], [R, y1, R], gauze);
    piece('Head', 'hat', 'veil_side_r', [R - 0.3, y0, -R], [R, y1, R], gauze);
    piece('Head', 'hat', 'veil_side_l', [-R, y0, -R], [-R + 0.3, y1, R], gauze);
    piece('Head', 'hat', 'veil_front', [-R, y0 + 1.2, -R], [R, y1, -R + 0.3], (f, u, v, w, h) => {
      if (f !== 'north' && f !== 'south') return null;
      const c = f === 'north' ? u : w - 1 - u, mid = c >= 4 && c <= w - 5;
      if (mid) return v === 0 ? 'V' : null;
      return v === h - 1 ? (c % 2 ? null : 'v') : (c + v) % 5 === 0 ? 'v' : 'V';
    });
    // The feather mantle over the shoulders and chest, ragged at its hem, a night-blue collar.
    piece('Chest', 'jacket', 'mantle', [-5.4, 18.8, -2.9], [5.4, 24.4, 2.9], rim(ragged(feathers())));
    piece('Chest', 'jacket', 'mantle_collar', [-3.0, 23.6, -3.1], [3.0, 24.8, 3.1], rim((f, u) => (u % 3 === 1 ? 'n' : 'N')));
    pair('Arm_R', 'rightSleeve', 'mantle_arm', [3.4, 18.6, -2.6], [8.8, 24.6, 2.6], rim(ragged(feathers())));
    // The long cloak down the back to the calves, flaring a little, its feather tips ragged.
    piece('Chest', 'jacket', 'cloak', [-6.2, 3.0, 2.6], [6.2, 24.2, 4.1], ragged(feathers(), 2), { tilt: -3, pivot: [0, 24.2, 2.9] });
    piece('Chest', 'jacket', 'cloak_tips', [-6.6, 2.0, 3.4], [6.6, 6.0, 4.6], ragged(feathers()), { tilt: -3, pivot: [0, 24.2, 2.9] });
  }

  if (n === 2) {
    // Long white hair down his back from the crown, its ends ragged, a black cord tying it at the nape.
    piece('Head', 'hat', 'hair_crown', [-4.3, 28.0, -4.3], [4.3, 32.4, 4.3], (f, u, v, w, h) => {
      if (f === 'north' || f === 'down') return null;
      if (isSide(f) && f !== 'south') { const front = f === 'east' ? w - 1 - u : u; if (front === 0) return v < 2 ? 'A' : null; }
      return (u + v) % 4 === 0 ? 'a' : 'A';
    });
    piece('Head', 'hat', 'hair_sides', [-4.4, 20.5, -2.2], [4.4, 28.0, 4.4], (f, u, v, w, h) => {
      if (f === 'north' || f === 'up') return null;
      if (f === 'down') return u === 0 || u === w - 1 || v === h - 1 ? 'a' : null;
      if (isSide(f) && v === h - 1) return u % 2 ? null : 'a';
      return u % 3 === 1 ? 'a' : 'A';
    });
    piece('Chest', 'jacket', 'hair_tail', [-3.0, 9.0, 2.2], [3.0, 24.6, 3.4], (f, u, v, w, h) =>
      (isSide(f) && v >= h - 2 ? (v === h - 1 ? (u % 3 === 1 ? 'a' : null) : u % 3 === 2 ? null : 'A') : u % 3 === 1 ? 'a' : 'A'));
    piece('Chest', 'jacket', 'hair_cord', [-1.8, 21.6, 3.3], [1.8, 22.6, 3.7], all('L'));
    // The mask's strap and the crow's beak: a deep base over the nose, narrowing, its tip hooked down; a gloss on top.
    piece('Head', 'head', 'mask_strap', [-4.25, 27.6, -4.25], [4.25, 28.4, 4.25], rim((f, u) => (f === 'north' ? null : 'L')));
    const beak = (f, u, v, w, h) => (f === 'up' ? (u === Math.floor(w / 2) ? 'N' : 'L') : f === 'down' ? 'l' : isSide(f) && f !== 'north' && v === 0 ? 'Y' : 'L');
    piece('Head', 'head', 'beak_base', [-1.3, 26.0, -5.4], [1.3, 28.6, -4.0], beak);
    piece('Head', 'head', 'beak_mid', [-0.95, 25.4, -7.4], [0.95, 27.9, -5.4], beak);
    piece('Head', 'head', 'beak_tip', [-0.6, 24.2, -9.0], [0.6, 27.0, -7.4], beak);
    // Three tiers of feather pauldrons on each shoulder, the lowest widest; a feather stole across the back.
    pair('Arm_R', 'rightSleeve', 'pauldron_a', [3.2, 22.6, -3.0], [8.9, 25.0, 3.0], rim(ragged(feathers(), 1)));
    pair('Arm_R', 'rightSleeve', 'pauldron_b', [3.6, 20.6, -3.3], [9.4, 23.2, 3.3], rim(ragged(feathers(), 1)));
    pair('Arm_R', 'rightSleeve', 'pauldron_c', [4.0, 18.4, -3.6], [9.8, 21.2, 3.6], rim(ragged(feathers('K', 'N'), 2)));
    piece('Chest', 'jacket', 'stole', [-4.4, 19.6, -2.5], [4.4, 24.3, 2.5], rim(ragged(feathers())));
    // A night-blue tabard falling from the sash between the legs, hemmed in amber.
    piece('Torso', 'jacket', 'tabard', [-1.8, 5.6, -2.9], [1.8, 15.4, -2.6], (f, u, v, w, h) => (f === 'south' ? null : v >= h - 1 ? 'O' : u === 0 || u === w - 1 ? 'L' : 'Q'));
    // In the game, his shadow raven on his left shoulder (the concept scenes set it there themselves, the preview script).
    if (game) shoulderRaven(piece);
  }

  if (n === 3) {
    // The topknot: a bun on the crown, a small gold crown round it, a gold pin through it and a black feather from it.
    piece('Head', 'hat', 'bun', [-1.4, 32.0, -0.6], [1.4, 34.6, 2.2], (f, u, v) => ((u + v) % 3 === 0 ? 'a' : 'A'));
    piece('Head', 'hat', 'crown', [-1.8, 32.0, -1.0], [1.8, 33.6, 2.6], rim((f, u, v, w) => (f === 'north' && u === Math.floor(w / 2) ? 'O' : v === 0 ? 'G' : 'g')));
    piece('Head', 'hat', 'pin', [-3.2, 33.8, 0.5], [3.2, 34.3, 1.0], (f, u, v, w) => (u === 0 || u === w - 1 ? 'O' : 'G'));
    piece('Head', 'hat', 'feather', [-3.0, 34.0, 0.9], [-2.4, 34.6, 8.4], (f, u, v, w, h) => {
      const along = f === 'east' || f === 'west' ? (f === 'west' ? u : w - 1 - u) : f === 'up' || f === 'down' ? v : 0;
      return along >= 6 ? 'N' : along % 2 ? 'k' : 'K';
    }, { tilt: -28, pivot: [-2.7, 34.0, 0.9] });
    piece('Head', 'hat', 'feather_vane', [-3.1, 33.4, 2.4], [-2.3, 34.0, 7.6], (f, u) => (u % 2 ? 'k' : 'K'), { tilt: -28, pivot: [-2.7, 34.0, 0.9] });
    // The standing collar of black feathers, rising behind the head, the night-blue sheen on its tips; gold at its foot.
    piece('Chest', 'jacket', 'collar_foot', [-4.4, 23.4, -2.6], [4.4, 24.4, 2.6], rim(() => 'G'));
    piece('Head', 'hat', 'collar_back', [-5.0, 23.6, 4.0], [5.0, 30.4, 5.0], (f, u, v, w, h) => {
      if (isSide(f) && v === 0) return u % 3 === 1 ? 'N' : null;
      if (isSide(f) && v === 1) return u % 3 === 2 ? null : 'N';
      return feathers()(f, u, v, w, h);
    });
    pair('Head', 'hat', 'collar_side', [4.1, 23.6, -1.8], [5.0, 28.8, 5.0], (f, u, v, w, h) => {
      if (isSide(f) && v === 0) return u % 3 === 1 ? 'N' : null;
      if (isSide(f) && v === 1) return u % 3 === 2 ? null : 'N';
      return feathers()(f, u, v, w, h);
    });
    // The message case across his back, from his right shoulder to his left hip: black lacquer, gold rings, a parchment
    // cap; and its strap over the chest.
    piece('Chest', 'jacket', 'case', [-1.3, 10.5, 2.1], [1.3, 25.0, 4.7], (f, u, v, w, h) => {
      if (f === 'up') return 'W';
      if (v <= 1 || v === Math.floor(h / 2) || v >= h - 2) return v === 0 ? 'O' : 'G';
      return (u + v) % 6 === 0 ? 'N' : 'L';
    }, { turnCorner: ([x, y, z]) => { const a = -28 * Math.PI / 180, px = 0, py = 17.5; const dx = x - px, dy = y - py; return [px + dx * Math.cos(a) - dy * Math.sin(a), py + dx * Math.sin(a) + dy * Math.cos(a), z]; },
      rot: [0, 0, -28], pivot: [0, 17.5, 3.2] });
    piece('Chest', 'jacket', 'case_strap', [-3.0, 18.4, -2.25], [-1.9, 23.8, -2.0], all('L'));
    // Gold-trimmed overlapping front panels of the outer robe falling from the sash.
    piece('Torso', 'jacket', 'apron', [-2.4, 3.6, -2.95], [2.4, 15.4, -2.6], (f, u, v, w, h) => (f === 'south' ? null : v >= h - 1 || u === 0 || u === w - 1 ? 'G' : u % 3 === 1 ? 'u' : 'U'));
  }
  return PIECES;
}

/**
 * The Shadow Messenger Raven (raven-familiars.cjs: its shape, its colours, its message case) as pieces of his chest,
 * standing on top of his left pauldrons, its beak ahead (-z) as he looks. On the chest rather than the arm: the bird
 * does not tilt when he swings his arm.
 */
function shoulderRaven(piece) {
  const { crow, STYLES } = require('../concepts/raven-familiars.cjs');
  const colours = STYLES.shadow.colours;
  const [ox, oy, oz] = [-6.4, 25.0, 0.4];
  let n = 0;
  const b = { box: (mat, shade, from, to) => {
    if (!colours[mat]) throw new Error('no raven colour ' + mat);
    piece('Chest', 'jacket', 'raven_' + n++, [from[0] + ox, from[1] + oy, from[2] + oz], [to[0] + ox, to[1] + oy, to[2] + oz], all(colours[mat]));
  } };
  crow(b, { body: 'feather', wing: 'wing' });
  b.box('scroll', 10, [-0.6, 5.6, 0.3], [0.6, 6.5, 2.9]);
  b.box('ring', 12, [-0.7, 5.5, 0.2], [0.7, 6.6, 0.55]);
  b.box('ring', 12, [-0.7, 5.5, 2.65], [0.7, 6.6, 3.0]);
}

/** The game's Raven Master: the Raven Mask and his raven, his mesh and his atlases (one per face). */
function writeGame() {
  const V = VARIANTS[2];
  const pieces = build(2, { game: true });
  const faces = kit.pack(pieces, ATLAS);
  const outfit = { key: 'raven_master', c: V.c };
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  const dir = path.join(assets, 'textures', 'entity');
  const art = path.join(ROOT, 'art', 'blockbench');
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, outfit, ATLAS));
  fs.writeFileSync(path.join(dir, 'raven_master.png'), atlas);
  // One atlas per face; the calm face keeps the plain name.
  for (const face of Object.keys(hd.EXPRESSIONS)) if (face !== 'calm')
    fs.writeFileSync(path.join(dir, 'raven_master_' + face + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...outfit, face }, ATLAS)));
  fs.mkdirSync(path.join(art, 'raven_master'), { recursive: true });
  fs.writeFileSync(path.join(art, 'raven_master', 'game.png'), atlas);
  const m = kit.mesh(pieces, ATLAS);
  fs.writeFileSync(path.join(assets, 'animmodels', 'entity', 'raven_master.json'), JSON.stringify(m));
  fs.writeFileSync(path.join(art, 'raven_master.bbmodel'), JSON.stringify(kit.bbmodel('raven_master', pieces, [outfit], [atlas], ATLAS, 'entity'), null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log('wrote the Raven Master (game):', pieces.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

function write(n) {
  const V = VARIANTS[n];
  const pieces = build(n);
  const faces = kit.pack(pieces, ATLAS);
  const outfit = { key: V.key, c: V.c };
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, outfit, ATLAS));
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'raven_master'), { recursive: true });
  fs.writeFileSync(path.join(art, 'raven_master', n + '.png'), atlas);
  // The mesh is only built to check every part holds something; it is not written (not in the game yet).
  const m = kit.mesh(pieces, ATLAS);
  const project = kit.bbmodel('raven_master_' + n, pieces, [{ key: 'raven_master_' + n }], [atlas], ATLAS, 'entity');
  // A piece turned about z (the message case) shows its own rotation in Blockbench.
  pieces.forEach((p, i) => { if (p.rot) project.elements[i].rotation = p.rot; });
  fs.writeFileSync(path.join(art, 'raven_master_' + n + '.bbmodel'), JSON.stringify(project, null, 1));
  const used = Math.max(...faces.map(f => f.v + f.h));
  console.log(`wrote the Raven Master ${n} (${V.name}):`, pieces.length, 'pieces,', m.vertices.positions.count, 'vertices, atlas rows used', used, 'of', ATLAS);
}

if (require.main === module && process.argv.includes('game')) writeGame();
else if (require.main === module) {
  const only = process.argv.slice(2).filter(a => VARIANTS[a]);
  for (const n of only.length ? only : Object.keys(VARIANTS)) write(Number(n));
}

module.exports = { VARIANTS, RAVEN, build, writeGame };
