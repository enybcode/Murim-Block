// Concept renders of the crab art's transformation, between the two first drafts (Enzo, 15/10/2026: the cubes too
// goofy, the sculpt "too far from Minecraft: find the middle"): still cubes and pixels, as a Minecraft mob, but
// detailed as the best modded mobs are. Every cube is painted texel by texel (two texels a pixel): the infection's raw
// flesh, its veins ahead of it, the crust's hollows and knobs, a little dither and darker edges; and the shapes are many
// small cubes turned at angles, so nothing reads as one big box: the shell a stepped dome, the pincers curved chains of
// tapering cubes, the knobs and spikes small cubes standing off the surface. Flat Minecraft-like light.
//   infected  the forearms raw, veins up the arms and the neck, long pincers to the knees, crust on a shoulder and a knee;
//   shell     the torso raw and ridged, a knobbed stepped shell over the back and the head like a hood, crusted knees and
//             clawed feet, pincers to the shins;
//   king      a great spiked shell, blue eyes on stalks under its brow, mandibles, a second pair of thin arms, the
//             pincers to the ground.
// Built with three.js inside Blockbench (its offscreen preview renders it): a design to choose from, not game files.
// Writes docs/combat/styles/images/crab-pixel-<stage>-<view>.png. Units: Blockbench pixels, x the wearer's right, y up,
// the figure facing -z.
//
//   node scripts/concepts/crab-art-pixel.cjs [--port=9234] [--only=king]
const fs = require('node:fs');
const path = require('node:path');
const { connect } = require('./chief-axes.cjs');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT = path.join(ROOT, 'docs', 'combat', 'styles', 'images');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9234);
const STAGES = ['human', 'infected', 'shell', 'king'];

/** Runs in Blockbench's page: builds stage `stage` (0 to 3) as a THREE.Group. */
function buildScene(THREE, stage) {
  // ------------------------------------------------ noise
  let seed = 4321 + stage * 977;
  const rand = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
  const P = new Uint8Array(512);
  { const p = [...Array(256).keys()]; let s = 99; const r = () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 4294967296; };
    for (let i = 255; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [p[i], p[j]] = [p[j], p[i]]; } for (let i = 0; i < 512; i++) P[i] = p[i & 255]; }
  const fade = t => t * t * t * (t * (t * 6 - 15) + 10), lerp = (a, b, t) => a + (b - a) * t;
  const grad = (h, x, y, z) => { const u = (h & 15) < 8 ? x : y, v = (h & 15) < 4 ? y : (h & 15) === 12 || (h & 15) === 14 ? x : z; return ((h & 1) ? -u : u) + ((h & 2) ? -v : v); };
  function noise(x, y, z) {
    const X = Math.floor(x) & 255, Y = Math.floor(y) & 255, Z = Math.floor(z) & 255;
    x -= Math.floor(x); y -= Math.floor(y); z -= Math.floor(z);
    const u = fade(x), v = fade(y), w = fade(z);
    const A = P[X] + Y, AA = P[A] + Z, AB = P[A + 1] + Z, B = P[X + 1] + Y, BA = P[B] + Z, BB = P[B + 1] + Z;
    return lerp(lerp(lerp(grad(P[AA], x, y, z), grad(P[BA], x - 1, y, z), u), lerp(grad(P[AB], x, y - 1, z), grad(P[BB], x - 1, y - 1, z), u), v),
      lerp(lerp(grad(P[AA + 1], x, y, z - 1), grad(P[BA + 1], x - 1, y, z - 1), u), lerp(grad(P[AB + 1], x, y - 1, z - 1), grad(P[BB + 1], x - 1, y - 1, z - 1), u), v), w);
  }
  const fbm = (x, y, z, o = 3) => { let s = 0, a = 0.5, f = 1; for (let i = 0; i < o; i++) { s += a * noise(x * f, y * f, z * f); a *= 0.5; f *= 2.03; } return s; };
  const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
  const smooth = (a, b, v) => { const t = clamp((v - a) / (b - a)); return t * t * (3 - 2 * t); };
  const hash = (a, b, c) => { let h = (a * 374761393 + b * 668265263 + c * 2147483647) >>> 0; h = (h ^ (h >>> 13)) * 1274126177 >>> 0; return (h & 0xffff) / 0xffff; };
  const col = h => new THREE.Color(h);
  const mix = (a, b, t) => a.clone().lerp(b, clamp(t));
  const V = (x, y, z) => new THREE.Vector3(x, y, z);

  // ------------------------------------------------ palette
  const C = {
    skin: col('#b88a68'), skinSick: col('#8d7564'), hair: col('#2e2117'), shirt: col('#3e434b'), trousers: col('#272b3b'), shoe: col('#2b2b2b'),
    eyeHuman: col('#3b2c7a'), flesh: col('#6e1a20'), fleshLight: col('#97353a'), fleshDeep: col('#3a070c'), vein: col('#1e0610'),
    crust: col('#55361f'), crustLight: col('#8c6440'), crustDeep: col('#221409'), spike: col('#1e130b'),
    claw: col('#701820'), clawLight: col('#9e3036'), tip: col('#160809'), tooth: col('#a0937a'),
    pus: col('#9a8238'), eye: col('#2d93dc'), pupil: col('#06080c'),
  };

  // ------------------------------------------------ painted cubes
  const D = 2;  // texels a pixel
  const root = new THREE.Group();
  let boxId = 0;
  /** The point of face f at (u, v) of a box of size s, in the box's own frame (as THREE.BoxGeometry lays its UVs). */
  const facePoint = (f, s, u, v) => [
    [s[0] / 2, -s[1] / 2 + v * s[1], s[2] / 2 - u * s[2]], [-s[0] / 2, -s[1] / 2 + v * s[1], -s[2] / 2 + u * s[2]],
    [-s[0] / 2 + u * s[0], s[1] / 2, s[2] / 2 - v * s[2]], [-s[0] / 2 + u * s[0], -s[1] / 2, -s[2] / 2 + v * s[2]],
    [-s[0] / 2 + u * s[0], -s[1] / 2 + v * s[1], s[2] / 2], [s[0] / 2 - u * s[0], -s[1] / 2 + v * s[1], -s[2] / 2],
  ][f];
  const faceSize = (f, s) => [[s[2], s[1]], [s[2], s[1]], [s[0], s[2]], [s[0], s[2]], [s[0], s[1]], [s[0], s[1]]][f];
  const NORMALS = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]];

  /**
   * A cube of size s at pos (its middle), turned by quat, in parent, its texels painted by paint(point, normal) with
   * the point in the parent's frame; a dither and darker edge texels on every face.
   */
  function cube(parent, s, pos, quat, paint, edge = 0.82) {
    const id = boxId++, mats = [];
    for (let f = 0; f < 6; f++) {
      const [fw, fh] = faceSize(f, s), W = Math.max(1, Math.round(fw * D)), H = Math.max(1, Math.round(fh * D));
      const data = new Uint8Array(W * H * 4), n = V(...NORMALS[f]).applyQuaternion(quat);
      for (let j = 0; j < H; j++) for (let i = 0; i < W; i++) {
        const p = V(...facePoint(f, s, (i + 0.5) / W, (j + 0.5) / H)).applyQuaternion(quat).add(pos);
        let c = paint(p, n);
        let k = 0.96 + 0.07 * hash(i, j, id * 6 + f);
        if ((i === 0 || j === 0 || i === W - 1 || j === H - 1) && W > 2 && H > 2) k *= edge;
        data.set([clamp(c.r * k) * 255, clamp(c.g * k) * 255, clamp(c.b * k) * 255, 255], (j * W + i) * 4);
      }
      const tex = new THREE.DataTexture(data, W, H, THREE.RGBAFormat);
      tex.magFilter = THREE.NearestFilter; tex.minFilter = THREE.NearestFilter;
      if ('colorSpace' in tex) tex.colorSpace = THREE.SRGBColorSpace; else tex.encoding = THREE.sRGBEncoding;
      tex.needsUpdate = true;
      mats.push(new THREE.MeshLambertMaterial({ map: tex }));
    }
    const m = new THREE.Mesh(new THREE.BoxGeometry(s[0], s[1], s[2]), mats);
    m.position.copy(pos); m.quaternion.copy(quat);
    parent.add(m);
    return m;
  }
  const Q0 = new THREE.Quaternion();
  const qFrom = (a, b) => new THREE.Quaternion().setFromUnitVectors(a.clone().normalize(), b.clone().normalize());
  const qEuler = (x, y, z) => new THREE.Quaternion().setFromEuler(new THREE.Euler(x, y, z));
  /** An axis-aligned cube from `from` to `to`. */
  const block = (parent, from, to, paint, edge) => cube(parent, [to[0] - from[0], to[1] - from[1], to[2] - from[2]],
    V((from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2), Q0, paint, edge);

  // ------------------------------------------------ paints
  const flat = c => () => c;
  const crustPaint = (v, n) => {
    const h = fbm(v.x * 0.5, v.y * 0.5, v.z * 0.5), g = noise(v.x * 2.2, v.y * 2.2, v.z * 2.2);
    let c = mix(C.crustDeep, C.crust, smooth(-0.4, 0.0, h));
    return mix(c, C.crustLight, smooth(0.08, 0.42, h + g * 0.25) * (0.55 + 0.45 * Math.max(0, n.y)));
  };
  const veinAt = v => 1 - smooth(0.006, 0.028, Math.abs(noise(v.x * 0.42 + 7, v.y * 0.16, v.z * 0.42) + 0.25 * noise(v.x * 1.3, v.y * 1.3, v.z * 1.3)));
  const fleshPaint = (v) => {
    const h = fbm(v.x * 0.55, v.y * 0.55, v.z * 0.55);
    let c = mix(C.fleshDeep, C.flesh, smooth(-0.7, -0.3, h));
    c = mix(c, C.fleshLight, smooth(0.12, 0.45, h));
    return mix(c, C.vein, veinAt(v) * 0.8);
  };
  const clawPaint = (yTop, yBot) => (v) => {
    const t = clamp((yTop - v.y) / (yTop - yBot));
    const c = mix(C.claw, C.clawLight, smooth(-0.1, 0.4, fbm(v.x * 0.9, v.y * 0.9, v.z * 0.9)) * 0.7);
    return mix(c, C.tip, smooth(0.62, 0.95, t));
  };

  // ------------------------------------------------ shapes made of cubes
  /** A chain of cubes along points, tapering through radii (w the side-to-side squash): a claw, a stalk, a thin arm. */
  function chain(parent, pts, radii, paint, segs = 10, w = 0.8) {
    const curve = new THREE.CatmullRomCurve3(pts);
    for (let i = 0; i < segs; i++) {
      const a = curve.getPointAt(i / segs), b = curve.getPointAt((i + 1) / segs), t = (i + 0.5) / segs;
      const r = lerp(radii[0], radii[1], t), len = a.distanceTo(b) + r * 0.6;
      cube(parent, [r * 2 * w, len, r * 2], a.clone().add(b).multiplyScalar(0.5), qFrom(V(0, 1, 0), b.clone().sub(a)), paint, 0.88);
    }
    return curve;
  }
  /** A point on an ellipsoid and its outward normal, by angles. */
  const onEllipsoid = (c, r, th, ph) => {
    const d = V(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
    return [V(c.x + d.x * r[0], c.y + d.y * r[1], c.z + d.z * r[2]), V(d.x / r[0], d.y / r[1], d.z / r[2]).normalize()];
  };
  /** Knobs: small cubes half sunk into an ellipsoid's surface, turned at random; some with a spike of stacked cubes. */
  function knobs(parent, c, r, count, size, spikeChance = 0.3, spikeLen = 1.6, filter = () => true) {
    for (let i = 0; i < count; i++) {
      const th = rand() * Math.PI * 2, ph = Math.acos(2 * rand() - 1), [p, n] = onEllipsoid(c, r, th, ph);
      if (!filter(p, n)) continue;
      const s = size * (0.55 + rand() * 0.9), q = qFrom(V(0, 1, 0), n).multiply(qEuler(0, rand() * 1.5, (rand() - 0.5) * 0.5));
      cube(parent, [s, s * 0.8, s], p.clone().addScaledVector(n, s * 0.15), q, crustPaint);
      if (rand() < spikeChance) {
        const l = spikeLen * (0.6 + rand() * 0.8), steps = 3;
        for (let k = 0; k < steps; k++) {
          const w = s * 0.55 * (1 - k / steps) + 0.15;
          cube(parent, [w, l / steps + 0.1, w], p.clone().addScaledVector(n, s * 0.45 + (k + 0.5) * l / steps), q,
            (v) => mix(C.crust, C.spike, 0.4 + k * 0.3), 0.9);
        }
      }
    }
  }
  /** A stepped dome: an ellipsoid's upper part (from y fraction y0) in layers one pixel thick, as a Minecraft mob's shell. */
  function dome(parent, c, r, y0 = -0.2, step = 1, paint = crustPaint, zCut = -Infinity) {
    for (let y = r[1] * y0; y < r[1]; y += step) {
      const t = (y + step / 2) / r[1], k = Math.sqrt(Math.max(0, 1 - t * t));
      const w = r[0] * k, d = r[2] * k;
      if (w < 0.4) continue;
      const z0 = Math.max(c.z - d, zCut);
      if (c.z + d <= z0) continue;
      block(parent, [c.x - w, c.y + y, z0], [c.x + w, c.y + y + step, c.z + d], paint, 0.9);
    }
  }
  /** A lump: a dome-like clump of three cubes, its knobs over it. */
  function clump(parent, c, r, knobCount, knobSize, spikeChance = 0.25) {
    block(parent, [c.x - r[0], c.y - r[1] * 0.6, c.z - r[2] * 0.8], [c.x + r[0], c.y + r[1] * 0.6, c.z + r[2] * 0.8], crustPaint);
    block(parent, [c.x - r[0] * 0.75, c.y - r[1], c.z - r[2] * 0.6], [c.x + r[0] * 0.75, c.y + r[1], c.z + r[2] * 0.6], crustPaint);
    block(parent, [c.x - r[0] * 0.6, c.y - r[1] * 0.75, c.z - r[2]], [c.x + r[0] * 0.6, c.y + r[1] * 0.75, c.z + r[2]], crustPaint);
    knobs(parent, c, r, knobCount, knobSize, spikeChance, 1.3);
  }

  // ------------------------------------------------ the player, painted with the infection
  function infection(part, v) {
    const n = fbm(v.x * 0.4 + 3, v.y * 0.4, v.z * 0.4) * 0.6;
    if (stage === 0) return 0;
    if (part === 'arm') return stage >= 2 ? 1 : clamp(1 - smooth(-7.5, -3.5, v.y) + n * 0.5);
    if (part === 'torso') return stage >= 2 ? 1 : clamp(n - 0.75);
    if (part === 'head') return stage >= 3 ? clamp(0.55 + n) : stage === 2 ? clamp(smooth(27, 24, v.y) * 0.8 + n - 0.2) : clamp(smooth(26, 24, v.y) * 0.5 + n * 0.3 - 0.15);
    if (part === 'leg') return stage >= 2 ? clamp(n + 0.1) : 0;
    return 0;
  }
  const body = (part, base) => (v) => {
    const inf = infection(part, v);
    const ahead = stage === 0 ? 0 : smooth(0.02, 0.3, inf + 0.12) * (1 - smooth(0.6, 0.9, inf));
    let c = base.clone();
    if (part === 'head' && stage >= 2) c = mix(c, C.skinSick, 0.6);
    c = mix(c, fleshPaint(v), smooth(0.45, 0.7, inf));
    return mix(c, C.vein, ahead * veinAt(v));
  };

  for (const x0 of [-4, 0]) {
    block(root, [x0, 1, -2], [x0 + 4, 12, 2], body('leg', C.trousers));
    block(root, [x0, 0, -2], [x0 + 4, 1, 2], body('leg', C.shoe));
  }
  block(root, [-4, 12, -2], [4, 24, 2], body('torso', C.shirt));
  if (stage >= 2) {
    // A ridged chest: two plates, then the belly's segments narrowing down.
    for (const sx of [-1, 1]) cube(root, [3.6, 3, 0.6], V(sx * 1.95, 21.3, -2.2), qEuler(0.1, sx * -0.15, sx * 0.08), fleshPaint);
    for (let i = 0; i < 5; i++) cube(root, [5.6 - i * 0.5, 1.3, 0.5], V(0, 18.7 - i * 1.45, -2.15 - (i % 2) * 0.05), qEuler(0.12, 0, 0), fleshPaint);
  }
  block(root, [-4, 24, -4], [4, 32, 4], body('head', C.skin));
  block(root, [-4.1, 29.2, -4.1], [4.1, 32.1, 4.1], flat(C.hair));
  block(root, [-4.1, 24, 1.8], [4.1, 29.2, 4.1], flat(C.hair));
  for (const x of [-2, 2]) block(root, [x - 1, 27, -4.15], [x + 1, 28, -4], flat(stage >= 3 ? C.fleshDeep : stage === 2 ? C.eye : C.eyeHuman), 1);

  // Arms, each in its group turned about the shoulder, the pincer with it.
  for (const side of [1, -1]) {
    const arm = new THREE.Group();
    arm.position.set(side * 6, 22, 0);
    arm.rotation.set(-0.2, 0, side * (0.1 + stage * 0.04));
    root.add(arm);
    block(arm, [-2, -4, -2], [2, 2, 2], body('arm', stage >= 2 ? C.flesh : C.shirt));
    block(arm, [-2, -10, -2], [2, -4, 2], body('arm', C.skin));
    if (stage === 0) continue;
    const big = side > 0, k = (big ? 1.15 : 0.92) * (1 + (stage - 1) * 0.16), L = [0, 9.5, 13, 16.5][stage] * (big ? 1 : 0.88);
    // The forearm swollen: a sleeve of raw flesh in stepped cubes, crust on its back.
    for (let i = 0; i < 4; i++) {
      const y = -9.6 + i * 1.5, w = 2.3 * k + (i === 1 || i === 2 ? 0.35 : 0);
      cube(arm, [w * 2, 1.6, w * 2], V(0, y, 0), qEuler(0, i * 0.18, 0), fleshPaint, 0.9);
    }
    clump(arm, V(side * 1.3, -6.5, 1.3), [1.4 * k, 2.2, 1.2 * k], 5, 0.9);
    // The palm: a long club, tapered, crusted at the knuckles.
    const top = -10, palmLen = L * 0.38;
    for (let i = 0; i < 3; i++) {
      const t = i / 3, h = palmLen / 3 + 0.2;
      cube(arm, [(2.2 - t * 0.3) * 2 * k, h, (1.9 - t * 0.3) * 2 * k], V(0, top - (i + 0.5) * palmLen / 3, -t * 0.4), qEuler(-t * 0.15, 0, 0),
        (v) => mix(fleshPaint(v), C.claw, 0.5), 0.86);
    }
    clump(arm, V(side * 0.9 * k, top - 0.9, 0.6), [1.5 * k, 1.1, 1.3 * k], 6, 0.8, stage >= 2 ? 0.45 : 0.15);
    // The fingers: chains of cubes, the outer long and hooked, the inner shorter; teeth on their inner edges.
    const y0 = top - palmLen + 0.6;
    const fingers = [
      [[V(side * 1.0 * k, y0, -0.2), V(side * 1.7 * k, y0 - L * 0.3, -0.9), V(side * 1.0 * k, y0 - L * 0.52, -1.5), V(-side * 0.2, y0 - L * 0.62, -1.4)], [1.0 * k, 0.2]],
      [[V(-side * 1.0 * k, y0, -0.2), V(-side * 1.5 * k, y0 - L * 0.26, -0.7), V(-side * 0.6 * k, y0 - L * 0.44, -1.2), V(side * 0.15, y0 - L * 0.5, -1.2)], [0.9 * k, 0.18]],
    ];
    for (const [pts, radii] of fingers) {
      const curve = chain(arm, pts, radii, clawPaint(pts[0].y, pts[pts.length - 1].y), 11, 0.8);
      for (let i = 1; i <= 3; i++) {
        const p = curve.getPointAt(0.12 + i * 0.16), toward = V(-p.x, 0, -0.3).normalize();
        cube(arm, [0.45 * k, 0.8 * k, 0.45 * k], p.clone().addScaledVector(toward, 0.55 * k), qFrom(V(0, 1, 0), toward), flat(C.tooth), 0.9);
      }
    }
    if (stage >= 3) for (let i = 0; i < 6; i++) cube(arm, [0.5, 0.5, 0.35], V(-side * (1.2 + rand() * 0.8), -4.5 - rand() * 3, -2.4), Q0, flat(C.pus), 0.8);
  }

  // Crust patches: a shoulder and a knee first, then both knees and the feet with claws.
  if (stage >= 1) {
    clump(root, V(6.2, 23.9, 0), [2.6, 1.3, 2.4], 9, 1.0, 0.35);
    clump(root, V(-2, 7.2, -2.2), [2.2, 2, 0.8], 7, 0.9);
  }
  if (stage >= 2) {
    clump(root, V(2, 7, -2.2), [2.2, 2, 0.8], 7, 0.9);
    for (const x of [-2, 2]) {
      clump(root, V(x, 1.2, -0.3), [2.4, 1.1, 2.6], 6, 0.8, 0.1);
      for (const dx of [-1.3, 0, 1.3]) chain(root, [V(x + dx, 0.9, -2.4), V(x + dx * 1.1, 0.6, -3.8), V(x + dx * 1.15, 0.1, -4.6)], [0.45, 0.15], flat(C.tip), 4, 1);
    }
    if (stage < 3) clump(root, V(-6.2, 23.7, 0), [2.3, 1.2, 2.2], 7, 0.9, 0.3);
  }

  // The shell: a stepped dome over the back, another over the head (open over the face), a knobbed brow.
  if (stage >= 2) {
    const big = stage >= 3, s = big ? 1.22 : 1;
    const shell = new THREE.Group(); root.add(shell);
    // Over the back: a dome lying on its side (built as layers along z from the back).
    for (let z = 0; z < 3.4 * s; z += 0.8) {
      const t = z / (3.4 * s), k = Math.sqrt(Math.max(0, 1 - t * t));
      block(shell, [-7.2 * s * k, 19 - 7.6 * s * k, 2 + z], [7.2 * s * k, 19 + 6.5 * s * k, 2.8 + z], crustPaint, 0.9);
    }
    knobs(shell, V(0, 19, 2.6), [7.2 * s, 7.2 * s, 3.4 * s], big ? 40 : 26, 1.2, big ? 0.35 : 0.2, big ? 2.2 : 1.6, (p, n) => n.z > 0.2);
    // Over the head: a hood, its front cut back so the face shows.
    dome(shell, V(0, 28, 2.2), [6.4 * s, 7.2 * s, 6.2 * s], 0.0, 0.8, crustPaint, -3.4);
    knobs(shell, V(0, 28, 2.2), [6.4 * s, 7.2 * s, 6.2 * s], big ? 46 : 30, 1.3, big ? 0.4 : 0.22, big ? 2.6 : 1.8, (p, n) => n.y > -0.1 && p.z > -3.6);
    // The brow: a ragged ridge jutting over the face.
    for (let x = -5.4 * s; x < 5.4 * s; x += 1.1) cube(shell, [1.3, 1 + rand() * 0.8, 2.2], V(x + 0.55, 32.6 - Math.abs(x) * 0.12, -3.6 - (big ? 0.6 : 0)), qEuler(0.3 + rand() * 0.3, 0, (rand() - 0.5) * 0.4), crustPaint);
    // The nape: the crust closing the gap between the hood and the back.
    block(shell, [-4.8 * s, 24.5, 2.2], [4.8 * s, 30, 5.4 * s], crustPaint, 0.9);
    knobs(shell, V(0, 27.2, 4.4 * s), [4.6 * s, 2.6, 1.6], 10, 1.0, 0.2, 1.4, (p, n) => n.z > 0.3);
    // A collar over the shoulders.
    for (let x = -8 * s; x < 8 * s; x += 1.4) cube(shell, [1.6, 2 + rand(), 5], V(x + 0.7, 25 + rand() * 0.6, 1.4), qEuler(0, 0, (rand() - 0.5) * 0.5), crustPaint);
    if (big) for (const sx of [-1, 1]) clump(root, V(sx * 8.3, 24.8, 0.4), [3, 1.8, 3], 14, 1.2, 0.5);
  }

  // The last stage: eyes on stalks under the brow, mandibles, a second pair of thin arms.
  if (stage >= 3) {
    for (const sx of [-1, 1]) {
      chain(root, [V(sx * 2.2, 31.2, -4.4), V(sx * 2.7, 32.3, -5.6), V(sx * 3.0, 32.7, -6.4)], [0.55, 0.4], fleshPaint, 4, 1);
      // The eye: a cube of blue, a pupil and a glint, pixel-sized.
      cube(root, [1.5, 1.5, 1.5], V(sx * 3.05, 33.1, -7), Q0, (v, n) => n.z < -0.6 ? (Math.abs(v.y - 33.1) < 0.4 && Math.abs(v.x - sx * 3.05) < 0.4 ? C.pupil : Math.abs(v.y - 33.6) < 0.2 && Math.abs(v.x - sx * 3.05 + 0.4) < 0.2 ? col('#bfe6ff') : C.eye) : C.fleshDeep, 0.95);
      chain(root, [V(sx * 1.4, 25.6, -4.1), V(sx * 1.9, 24.6, -5.1), V(sx * 0.6, 23.7, -5.5)], [0.5, 0.15], clawPaint(25.6, 23.7), 5, 1);
      chain(root, [V(sx * 4.3, 20.5, -1.8), V(sx * 5.4, 15, -3.4), V(sx * 5.0, 9.5, -3.6), V(sx * 4.4, 5.6, -3.0)], [0.75, 0.42], fleshPaint, 12, 0.9);
      for (const o of [-1, 1]) chain(root, [V(sx * 4.4, 5.7, -3.0), V(sx * 4.4 + o * 0.6, 4.3, -3.3), V(sx * 4.4 + o * 0.1, 3, -3.5)], [0.38, 0.12], flat(C.tip), 4, 1);
    }
    for (let i = 0; i < 8; i++) cube(root, [0.5, 0.5, 0.3], V(-1.5 - rand() * 2, 13.5 + rand() * 3, -2.35), Q0, flat(C.pus), 0.8);
  }

  // Minecraft-like light: from above and a little from the front, a soft fill; a faint cold rim from behind.
  const key = new THREE.DirectionalLight('#fff1e0', 1.15); key.position.set(-20, 60, -35); root.add(key);
  const rim = new THREE.DirectionalLight('#8fb8ff', 0.55); rim.position.set(30, 20, 50); root.add(rim);
  root.add(new THREE.AmbientLight('#8a8790', 0.85));
  return root;
}

async function shoot(bb, stage, views, file, size) {
  for (const [view, [pos, target]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      while (ModelProject.all.length) await ModelProject.all[0].close(true);
      setupProject(Formats.free);
      const group = (${buildScene.toString()})(THREE, ${stage});
      Canvas.scene.add(group);
      const p = window.__pixelPreview || (window.__pixelPreview = new Preview({ id: 'pixel_render', offscreen: true, antialias: true }));
      p.resize(${size[0]}, ${size[1]});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 34 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#1f1e22');
      const hidden = Canvas.scene.children.filter(o => o !== group && o.isLight && o.visible); hidden.forEach(o => o.visible = false);
      const out = await new Promise(res => p.screenshot({ crop: false }, d => res(d)));
      Canvas.scene.background = bg; hidden.forEach(o => o.visible = true); Canvas.scene.remove(group);
      group.traverse(o => { if (o.geometry) o.geometry.dispose(); if (o.material) [].concat(o.material).forEach(m => { if (m.map) m.map.dispose(); m.dispose(); }); });
      return out;
    })()`);
    fs.writeFileSync(file(view), Buffer.from(data.split(',')[1], 'base64'));
  }
}

const VIEWS = { front: [[-38, 30, -64], [0, 19, 0]], back: [[44, 34, 58], [0, 20, 0]], face: [[-14, 30, -34], [0, 27, 0]] };

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const only = arg('only') ? arg('only').split(',') : STAGES;
  const bb = await connect(PORT);
  for (const name of only) {
    await shoot(bb, STAGES.indexOf(name), VIEWS, v => path.join(OUT, `crab-pixel-${name}-${v}.png`), [520, 640]);
    console.log('rendered', name);
  }
  bb.close();
}

if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
