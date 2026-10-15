// Faces at 16 x 16, twice Minecraft's head resolution, drawn from a few traits: hair and its fringe, brows, eyes, nose,
// mouth, moustache and beard, a scar. A mob script puts one on a thin plate over the head's front (a mesh-kit piece of
// density 2), and draws it once for each of client/MobFaces' faces (calm, blink, left, right, mutter, the angry ones,
// shout), each a copy of the atlas, like the 8 x 8 faces.
//   const hd = require('./hd-face.cjs');
//   piece('Head', 'head', 'face', ...hd.PLATE, hd.paint(traits));   // its paint reads outfit.face
// Traits: skin, hair, iris (hex colours); fringe (16 lengths, column by column, from the viewer's left; none: shaved);
// band (a row of the headband), brow ('flat', 'soft', 'angry', 'thick'), eyes ('open', 'narrow', 'closed'),
// mouth ('calm', 'smile', 'stern', 'shout', 'mutter'), moustache (true or 'long'), goatee, beard, blush, marks (shaved
// crown), scar ([x, y] texels), locks (rows of hair falling at the sides).
const N = 16;

const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const mix = (a, b, t) => '#' + hex(a).map((x, i) => Math.round(x + (hex(b)[i] - x) * t).toString(16).padStart(2, '0')).join('');

/** The face's colours, from its skin, hair and eyes. */
function palette(t) {
  const { skin, hair, iris } = t;
  return {
    S: skin, s: mix(skin, '#5a3020', 0.22), d: mix(skin, '#4a2418', 0.38), t: mix(skin, '#fff4e0', 0.2), b: mix(skin, '#d0604c', 0.3),
    A: hair, a: mix(hair, '#000000', 0.35), h: mix(hair, '#ffffff', 0.18), B: mix(hair, '#000000', 0.2),
    Z: '#efeae0', E: iris, e: '#120c0a', k: mix(hair, '#000000', 0.5),
    M: mix(skin, '#8a2e24', 0.45), m: mix(skin, '#3a1410', 0.6), N: mix(skin, '#5a3020', 0.3),
    R: hair, r: mix(hair, '#000000', 0.3), X: mix(skin, '#9a3a30', 0.4), Q: '#2B5FA8', q: '#1e4580', W: '#ebe6d8',
  };
}

/** How each of MobFaces' faces changes the traits. */
const EXPRESSIONS = {
  calm: {}, blink: { eyes: 'closed' }, left: { look: 'left' }, right: { look: 'right' }, mutter: { mouth: 'mutter' },
  angry: { brow: 'angry', mouth: 'stern' }, angry_blink: { brow: 'angry', mouth: 'stern', eyes: 'closed' },
  angry_left: { brow: 'angry', mouth: 'stern', look: 'left' }, angry_right: { brow: 'angry', mouth: 'stern', look: 'right' },
  shout: { brow: 'angry', mouth: 'shout' },
};

/** The face as 16 rows of colour letters (palette), the viewer's view. */
function grid(o) {
  const g = Array.from({ length: N }, () => Array(N).fill('S'));
  const set = (x, y, c) => { if (x >= 0 && x < N && y >= 0 && y < N && c) g[y][x] = c; };
  const mirror = (x, y, c) => { set(x, y, c); set(N - 1 - x, y, c); };
  // Skin modelling: the sides and the jaw in shadow, the cheekbones lit.
  for (let y = 0; y < N; y++) { set(0, y, 's'); set(N - 1, y, 's'); }
  for (let x = 1; x < N - 1; x++) set(x, 15, 's');
  mirror(1, 15, 'd'); mirror(2, 14, 's'); mirror(1, 14, 's');
  mirror(3, 11, 't');
  if (o.blush) { mirror(3, 12, 'b'); mirror(4, 12, 'b'); }
  // Hair: the fringe, its length column by column, with lit strands and its shadow on the forehead.
  if (o.fringe) {
    for (let x = 0; x < N; x++) {
      const len = o.fringe[x];
      for (let y = 0; y < len; y++) set(x, y, 'A');
      if (len > 0) set(x, len - 1, (x + len) % 3 === 0 ? 'a' : 'A');
    }
    for (const [x, y] of o.shine || [[3, 1], [4, 1], [5, 2], [11, 1], [12, 2]]) set(x, y, 'h');
    for (let x = 1; x < N - 1; x++) { const y = o.fringe[x]; if (y > 0 && y < 7 && g[y][x] === 'S') set(x, y, 's'); }
  } else {
    for (let x = 1; x < N - 1; x++) set(x, 0, 't');
    for (const x of o.marks || []) set(x, 2, 'd');
  }
  if (o.band != null) for (let x = 0; x < N; x++) set(x, o.band, x % 4 === 1 ? 'q' : 'Q');
  // Brows (row 6), the eye sockets (row 7), the eyes (rows 8 and 9): each eye 4 x 2, its outer corner lined, a 2 x 2
  // iris, the pupil on the nose side; the right eye mirrors the left.
  const L = 3, R = 9;
  const brow = o.brow || 'flat';
  for (let i = 0; i < 4; i++) {
    const y = brow === 'angry' ? (i >= 2 ? 7 : 6) : brow === 'soft' && i === 0 ? 7 : 6;
    if (brow === 'thick') { set(L + i, 5, 'B'); set(R + 3 - i, 5, 'B'); }
    set(L + i, y, 'B'); set(R + 3 - i, y, 'B');
  }
  const eye = (x0, outerLeft) => {
    const at = (i, y, c) => set(outerLeft ? x0 + i : x0 + 3 - i, y, c);
    if (o.eyes === 'closed') {
      for (let i = 0; i < 4; i++) { at(i, 8, 's'); at(i, 9, i === 0 || i === 3 ? 's' : 'k'); }
      return;
    }
    const narrow = o.eyes === 'narrow';
    for (let i = 0; i < 4; i++) if (g[7][outerLeft ? x0 + i : x0 + 3 - i] === 'S') at(i, 7, 's');
    at(0, 8, 'k'); at(1, 8, narrow ? 'k' : 'E'); at(2, 8, narrow ? 'k' : 'E'); at(3, 8, narrow ? 'k' : 'Z');
    at(0, 9, 'Z'); at(1, 9, 'E'); at(2, 9, 'e'); at(3, 9, 'Z');
  };
  eye(L, true); eye(R, false);
  // A glance: both irises a texel toward the side looked at (the mob's left is the viewer's right).
  if (o.look && o.eyes !== 'closed') {
    const dx = o.look === 'left' ? 1 : -1;
    for (const x0 of [L, R]) {
      const row8 = [0, 1, 2, 3].map(i => g[8][x0 + i]), row9 = [0, 1, 2, 3].map(i => g[9][x0 + i]);
      for (let i = 0; i < 4; i++) {
        const from = i - dx;
        if (from >= 0 && from < 4) { if (row8[from] === 'E') set(x0 + i, 8, 'E'); set(x0 + i, 9, row9[from] === 'k' ? 'Z' : row9[from]); }
        else set(x0 + i, 9, 'Z');
      }
    }
  }
  for (const x of [L - 1, R + 4]) set(x, 9, 's');
  // The nose: a lit bridge and a shadow under it.
  set(8, 9, 's'); set(8, 10, 'N'); set(7, 11, 'N'); set(8, 11, 'd'); set(7, 10, 't');
  // The mouth.
  const mouth = o.mouth || 'calm';
  if (mouth === 'calm') { for (let x = 6; x <= 9; x++) set(x, 13, x === 6 || x === 9 ? 'N' : 'M'); set(7, 14, 's'); set(8, 14, 's'); }
  if (mouth === 'smile') { set(5, 12, 'N'); set(10, 12, 'N'); for (let x = 6; x <= 9; x++) set(x, 13, 'M'); set(7, 14, 's'); set(8, 14, 's'); }
  if (mouth === 'stern') for (let x = 5; x <= 10; x++) set(x, 13, 'm');
  if (mouth === 'mutter') { set(6, 13, 'N'); set(7, 13, 'm'); set(8, 13, 'm'); set(9, 13, 'N'); set(7, 14, 'M'); set(8, 14, 'M'); }
  if (mouth === 'shout') {
    for (let x = 6; x <= 9; x++) { set(x, 12, 'm'); set(x, 13, 'm'); set(x, 14, x === 6 || x === 9 ? 'm' : 'M'); }
    set(7, 12, 'W'); set(8, 12, 'W');
  }
  // Facial hair.
  if (o.moustache) {
    for (let x = 5; x <= 10; x++) if (!(mouth === 'shout' && x >= 6 && x <= 9)) set(x, 12, 'R');
    set(4, 13, 'R'); set(11, 13, 'R');
    if (o.moustache === 'long') { set(4, 14, 'R'); set(11, 14, 'R'); set(4, 15, 'r'); set(11, 15, 'r'); }
  }
  if (o.goatee) for (let y = 14; y < N; y++) for (let x = 6; x <= 9; x++) if (!(mouth === 'shout' && y === 14)) set(x, y, y === 15 && (x === 6 || x === 9) ? 'r' : 'R');
  if (o.beard) {
    for (let y = 11; y < N; y++) for (let x = 0; x < N; x++) {
      const edge = Math.min(x, N - 1 - x);
      if ((y >= 13 || edge <= 2) && !(y >= 12 && y <= 14 && x >= 6 && x <= 9 && mouth !== 'calm')) if (!(y === 13 && x >= 6 && x <= 9)) set(x, y, (x + y) % 4 === 0 ? 'r' : 'R');
    }
    for (let x = 0; x < N; x++) for (let y = 9; y < 11; y++) if (Math.min(x, N - 1 - x) <= 1) set(x, y, 'R');
  }
  if (o.scar) for (const [x, y] of o.scar) set(x, y, 'X');
  if (o.locks) for (let y = 0; y < o.locks; y++) { set(0, y, 'A'); set(N - 1, y, 'A'); set(1, y, y % 3 === 0 ? 'a' : 'A'); set(N - 2, y, y % 3 === 1 ? 'a' : 'A'); }
  return g;
}

/** The face drawn for one of MobFaces' faces (its name, or 'calm'), as hex colours. */
function draw(traits, face = 'calm') {
  const o = { ...traits, ...(EXPRESSIONS[face] || {}) };
  const pal = palette(o);
  return grid(o).map(row => row.map(c => pal[c] || c));
}

/** The plate over the head's front (an 8-pixel head from y 24 to 32, its face at z -4), 16 x 16 texels. */
const PLATE = [[-4, 24, -4.03], [4, 32, -4.0]];

/** A mesh-kit paint for the plate: the face of outfit.face on its front, nothing elsewhere. */
function paint(traits) {
  const cache = {};
  return (f, u, v, w, h, outfit) => {
    if (f !== 'north') return null;
    const face = (outfit && outfit.face) || 'calm';
    const g = cache[face] || (cache[face] = draw(traits, face));
    // The north face is seen from outside: u runs from the viewer's left, as the grid does.
    return g[v][u];
  };
}

module.exports = { N, EXPRESSIONS, palette, grid, draw, paint, PLATE, DENSITY: 2 };
