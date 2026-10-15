// Concept renders of the crab art's transformation, sculpted rather than built of cubes (Enzo, 15/10/2026: "more
// detail, more realistic, play with colours and shapes, less square"), after his reference: an infection, not a costume.
// The player keeps his blocky body (in game it is his skin), but what grows over it is organic: raw red flesh spreading
// from the hands with dark veins ahead of it, a brown crust swollen with knobs and spikes, long curved pincers, sickly
// pustules. Three stages after the human:
//   infected  the forearms swollen into raw flesh, veins up the arms and the neck, long pincers to the knees, a crust on
//             a shoulder and a knee;
//   shell     the shirt gone over a ridged red torso, a knobbed shell over the back and the head like a hood, its brow
//             jutting, pincers to the shins, crusted knees and clawed feet;
//   king      a great spiked carapace over the shoulders, blue eyes on stalks under its brow, mandibles, a second pair
//             of thin arms, the pincers to the ground, pustules.
// Built with three.js inside Blockbench (its offscreen preview renders it): a design to choose from, not game files.
// Writes docs/combat/styles/images/crab-sculpt-<stage>-<view>.png. Units: Blockbench pixels, x the wearer's right, y up,
// the figure facing -z.
//
//   node scripts/concepts/crab-art-sculpt.cjs [--port=9234] [--only=king]
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
  let seed = 1234 + stage * 977;
  const rand = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
  const P = new Uint8Array(512);
  { const p = [...Array(256).keys()]; for (let i = 255; i > 0; i--) { const j = Math.floor(rand() * (i + 1)); [p[i], p[j]] = [p[j], p[i]]; } for (let i = 0; i < 512; i++) P[i] = p[i & 255]; }
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
  const fbm = (x, y, z, o = 4) => { let s = 0, a = 0.5, f = 1; for (let i = 0; i < o; i++) { s += a * noise(x * f, y * f, z * f); a *= 0.5; f *= 2.03; } return s; };
  const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
  const smooth = (a, b, v) => { const t = clamp((v - a) / (b - a)); return t * t * (3 - 2 * t); };
  const col = h => new THREE.Color(h);
  const mix = (a, b, t) => a.clone().lerp(b, clamp(t));

  // ------------------------------------------------ palette
  const C = {
    skin: col('#b98d6c'), skinSick: col('#8a7466'), hair: col('#2e2117'), shirt: col('#3d4249'), trousers: col('#262a3a'), shoe: col('#2c2c2c'),
    flesh: col('#7d1620'), fleshLight: col('#b83a3f'), fleshDeep: col('#2e0508'), vein: col('#1c0610'),
    crust: col('#4a2f1b'), crustLight: col('#86603a'), crustDeep: col('#1d120a'), spike: col('#24170d'),
    claw: col('#6e141c'), clawLight: col('#a8333a'), tip: col('#120607'), tooth: col('#9c8f74'),
    pus: col('#8f7a35'), eye: col('#2f9ff0'),
  };

  const root = new THREE.Group();
  const mat = (opts = {}) => new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.75, metalness: 0.0, ...opts });

  /** Colours a geometry's vertices with fn(worldish position, normal) and returns a mesh. */
  function paint(geo, fn, m) {
    geo = geo.index ? geo.toNonIndexed() : geo;
    const pos = geo.attributes.position, nor = geo.attributes.normal, c = new Float32Array(pos.count * 3), v = new THREE.Vector3(), n = new THREE.Vector3();
    for (let i = 0; i < pos.count; i++) {
      v.fromBufferAttribute(pos, i); n.fromBufferAttribute(nor, i);
      const k = fn(v, n).clone().convertSRGBToLinear(); c[i * 3] = k.r; c[i * 3 + 1] = k.g; c[i * 3 + 2] = k.b;
    }
    geo.setAttribute('color', new THREE.BufferAttribute(c, 3));
    return new THREE.Mesh(geo, m || mat());
  }

  /** Pushes a geometry's vertices along their normal by fn(position, normal) (indexed, so seams stay closed). */
  function displace(geo, fn) {
    geo = THREE.BufferGeometryUtils ? geo : geo;
    geo.computeVertexNormals();
    const pos = geo.attributes.position, nor = geo.attributes.normal, v = new THREE.Vector3(), n = new THREE.Vector3();
    // Vertices at the same place move the same: keyed by position.
    const moved = new Map();
    for (let i = 0; i < pos.count; i++) {
      v.fromBufferAttribute(pos, i); n.fromBufferAttribute(nor, i);
      const key = v.x.toFixed(3) + ',' + v.y.toFixed(3) + ',' + v.z.toFixed(3);
      let d = moved.get(key);
      if (!d) { const a = fn(v, n); d = n.clone().multiplyScalar(a); moved.set(key, d); }
      pos.setXYZ(i, v.x + d.x, v.y + d.y, v.z + d.z);
    }
    geo.computeVertexNormals();
    return geo;
  }

  /** A tube along points (THREE.Vector3) with radii, its section an ellipse (flat: the side-to-side squash). */
  function tube(points, radii, flat = 1, segs = 14, sides = 12) {
    const curve = new THREE.CatmullRomCurve3(points);
    const frames = curve.computeFrenetFrames(segs, false);
    const verts = [], idx = [];
    for (let i = 0; i <= segs; i++) {
      const t = i / segs, p = curve.getPointAt(t), N = frames.normals[i], B = frames.binormals[i];
      const r = radii.length === 2 ? lerp(radii[0], radii[1], t) : radii[Math.min(radii.length - 1, Math.floor(t * (radii.length - 1)))] * (1 - (t * (radii.length - 1) % 1)) + radii[Math.min(radii.length - 1, Math.ceil(t * (radii.length - 1)))] * (t * (radii.length - 1) % 1);
      for (let j = 0; j < sides; j++) {
        const a = j / sides * Math.PI * 2;
        verts.push(p.x + (Math.cos(a) * N.x * flat + Math.sin(a) * B.x) * r, p.y + (Math.cos(a) * N.y * flat + Math.sin(a) * B.y) * r, p.z + (Math.cos(a) * N.z * flat + Math.sin(a) * B.z) * r);
      }
    }
    for (let i = 0; i < segs; i++) for (let j = 0; j < sides; j++) {
      const a = i * sides + j, b = i * sides + (j + 1) % sides, c = (i + 1) * sides + j, d = (i + 1) * sides + (j + 1) % sides;
      idx.push(a, c, b, b, c, d);
    }
    // Caps.
    const s = curve.getPointAt(0), e = curve.getPointAt(1), si = verts.length / 3; verts.push(s.x, s.y, s.z, e.x, e.y, e.z);
    for (let j = 0; j < sides; j++) { idx.push(si, j, (j + 1) % sides); idx.push(si + 1, segs * sides + (j + 1) % sides, segs * sides + j); }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(verts, 3)); g.setIndex(idx); g.computeVertexNormals();
    return g;
  }
  const V = (x, y, z) => new THREE.Vector3(x, y, z);

  /** An ellipsoid at c with radii r, bumped by noise (amp) and knobs (count, size). */
  function lump(c, r, amp, knobs = 0, knobSize = 1, detail = 48) {
    const g = new THREE.SphereGeometry(1, detail, Math.round(detail * 0.75));
    g.scale(r[0], r[1], r[2]);
    const ks = [];
    for (let i = 0; i < knobs; i++) {
      const th = rand() * Math.PI * 2, ph = Math.acos(2 * rand() - 1);
      ks.push([V(Math.sin(ph) * Math.cos(th) * r[0], Math.cos(ph) * r[1], Math.sin(ph) * Math.sin(th) * r[2]), knobSize * (0.6 + rand() * 0.8)]);
    }
    displace(g, (v) => {
      let d = fbm(v.x * 0.45 + c.x, v.y * 0.45 + c.y, v.z * 0.45 + c.z) * amp;
      for (const [k, s] of ks) { const q = v.distanceTo(k); d += s * Math.exp(-(q * q) / (s * s * 0.9)); }
      return d;
    });
    g.translate(c.x, c.y, c.z);
    return g;
  }

  /** Crust colouring: dark in the hollows, light on the knobs' tops, a little grain. */
  const crustPaint = (v, n) => {
    const h = fbm(v.x * 0.5, v.y * 0.5, v.z * 0.5), g = fbm(v.x * 2.6, v.y * 2.6, v.z * 2.6, 2);
    let c = mix(C.crustDeep, C.crust, smooth(-0.35, 0.05, h));
    c = mix(c, C.crustLight, smooth(0.05, 0.4, h + g * 0.4) * (0.6 + 0.4 * Math.max(0, n.y)));
    return c;
  };
  /** Raw flesh: glossy red, darker in the folds, veins of near-black. */
  const fleshPaint = (v) => {
    const h = fbm(v.x * 0.55, v.y * 0.55, v.z * 0.55), vein = Math.abs(noise(v.x * 0.45 + 7, v.y * 0.18, v.z * 0.45) + 0.3 * noise(v.x * 1.4, v.y * 1.4, v.z * 1.4));
    let c = mix(C.fleshDeep, C.flesh, smooth(-0.6, -0.2, h));
    c = mix(c, C.fleshLight, smooth(0.1, 0.45, h));
    return mix(c, C.vein, (1 - smooth(0.008, 0.035, vein)) * 0.85);
  };
  const fleshMat = () => mat({ roughness: 0.38 });
  const crustMat = () => mat({ roughness: 0.92 });
  const clawMat = () => mat({ roughness: 0.3, metalness: 0.05 });

  /** Spikes: dark cones standing out of a surface along its normal, at random points of an ellipsoid shell. */
  function spikes(group, c, r, count, len, upOnly = true) {
    for (let i = 0; i < count; i++) {
      const th = rand() * Math.PI * 2, ph = Math.acos(upOnly ? rand() * 0.9 + 0.05 : 2 * rand() - 1);
      const d = V(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
      if (upOnly && d.z < -0.55) continue;
      const at = V(c.x + d.x * r[0] * 0.93, c.y + d.y * r[1] * 0.93, c.z + d.z * r[2] * 0.93);
      const n = V(d.x / r[0], d.y / r[1], d.z / r[2]).normalize();
      const l = len * (0.5 + rand() * 0.8);
      const g = new THREE.ConeGeometry(l * 0.28, l, 8, 3);
      g.translate(0, l / 2, 0);
      const m = paint(g, (v) => mix(C.crust, C.spike, v.y / l), crustMat());
      m.position.copy(at); m.quaternion.setFromUnitVectors(V(0, 1, 0), n.lerp(V(0, 1, 0), 0.25).normalize());
      group.add(m);
    }
  }

  /** Pustules: small glossy yellow domes in clusters. */
  function pustules(group, c, spread, count) {
    for (let i = 0; i < count; i++) {
      const s = 0.18 + rand() * 0.3, g = new THREE.SphereGeometry(s, 12, 9);
      const m = paint(g, (v) => mix(C.pus, C.fleshDeep, smooth(0, -s, v.y) * 0.6), mat({ roughness: 0.2 }));
      m.position.set(c.x + (rand() - 0.5) * spread[0], c.y + (rand() - 0.5) * spread[1], c.z + (rand() - 0.5) * spread[2]);
      group.add(m);
    }
  }

  // ------------------------------------------------ the player (blocky, as his skin), painted with the infection
  /** How far the infection has reached a point of the player's body: 0 clean, 1 raw. */
  function infection(part, v) {
    const n = fbm(v.x * 0.4 + 3, v.y * 0.4, v.z * 0.4) * 0.6;
    if (stage === 0) return 0;
    if (part === 'arm') return stage >= 2 ? 1 : clamp(1 - smooth(-7.5, -3.5, v.y) + n * 0.5);
    if (part === 'torso') return stage >= 2 ? 1 : clamp(n - 0.75);
    if (part === 'head') return stage >= 3 ? clamp(0.55 + n) : stage === 2 ? clamp(smooth(27, 24, v.y) * 0.8 + n - 0.2) : clamp(smooth(26, 24, v.y) * 0.5 + n * 0.3 - 0.15);
    if (part === 'leg') return stage >= 2 ? clamp(n + 0.1) : 0;
    return 0;
  }
  function body(part, base, v) {
    const inf = infection(part, v);
    const vein = Math.abs(noise(v.x * 0.8 + 11, v.y * 0.5, v.z * 0.8));
    // Ahead of the raw flesh, veins: a band where the infection is coming.
    const ahead = stage === 0 ? 0 : smooth(0.02, 0.3, inf + 0.12) * (1 - smooth(0.6, 0.9, inf));
    let c = base.clone();
    if (part === 'head' && stage >= 2) c = mix(c, C.skinSick, 0.6);
    c = mix(c, fleshPaint(v), smooth(0.45, 0.7, inf));
    return mix(c, C.vein, ahead * (1 - smooth(0.02, 0.06, vein)));
  }
  function block(part, base, from, to, parent, seg = 6) {
    const s = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
    const g = new THREE.BoxGeometry(s[0], s[1], s[2], Math.ceil(s[0] * seg / 4), Math.ceil(s[1] * seg / 4), Math.ceil(s[2] * seg / 4));
    g.translate((from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2);
    const m = paint(g, v => body(part, base, v), mat({ roughness: stage >= 2 && part === 'torso' ? 0.45 : 0.85 }));
    (parent || root).add(m);
    return m;
  }

  // Legs and feet.
  for (const x0 of [-4, 0]) {
    block('leg', C.trousers, [x0, 1, -2], [x0 + 4, 12, 2]);
    block('leg', C.shoe, [x0, 0, -2], [x0 + 4, 1, 2]);
  }
  // Torso: the shirt, or (stage 2+) the raw ridged chest.
  block('torso', C.shirt, [-4, 12, -2], [4, 24, 2]);
  if (stage >= 2) {
    const chest = new THREE.Group(); root.add(chest);
    for (const sx of [-1, 1]) chest.add(paint(lump(V(sx * 2, 21.3, -1.85), [2.1, 1.7, 0.42], 0.12), fleshPaint, fleshMat()));
    for (let i = 0; i < 5; i++)
      chest.add(paint(lump(V(0, 19 - i * 1.45, -1.9), [2.9 - i * 0.22, 0.85, 0.32], 0.08), fleshPaint, fleshMat()));
  }
  // Head and hair.
  const head = block('head', C.skin, [-4, 24, -4], [4, 32, 4]);
  block('hair', C.hair, [-4.1, 29.2, -4.1], [4.1, 32.1, 4.1]);
  block('hair', C.hair, [-4.1, 24, 1.8], [4.1, 29.2, 4.1]);
  // Eyes: dark and sunken at the last stage (the stalks see for him).
  for (const x of [-2, 2]) {
    const g = new THREE.BoxGeometry(2, 1, 0.1); g.translate(x, 27.5, -4.05);
    root.add(new THREE.Mesh(g, new THREE.MeshStandardMaterial({ color: stage >= 3 ? '#140405' : stage === 2 ? '#2f9ff0' : '#33266e', emissive: stage === 2 ? '#0b3a66' : '#000000', roughness: 0.4 })));
  }

  // Arms, each in its own group turned about the shoulder: the pincer goes with it.
  for (const side of [1, -1]) {
    const arm = new THREE.Group();
    arm.position.set(side * 6, 22, 0);
    arm.rotation.set(-0.2, 0, side * (0.1 + stage * 0.04));
    root.add(arm);
    const x0 = side > 0 ? -2 : -2;
    block('arm', stage >= 2 ? C.flesh : C.shirt, [x0, -4, -2], [x0 + 4, 2, 2], arm);
    block('arm', C.skin, [x0, -10, -2], [x0 + 4, -4, 2], arm);
    if (stage === 0) continue;
    const big = side > 0, k = (big ? 1.15 : 0.92) * (1 + (stage - 1) * 0.16), L = [0, 9.5, 13, 16.5][stage] * (big ? 1 : 0.88);
    // The forearm swollen into raw flesh, crust on its back.
    arm.add(paint(lump(V(0, -6.5, 0), [2.5 * k, 4.2, 2.5 * k], 0.35, 4, 0.5), fleshPaint, fleshMat()));
    arm.add(paint(lump(V(side * 1.2, -6, 1.1), [1.6 * k, 3.2, 1.5 * k], 0.5, 6, 0.6), crustPaint, crustMat()));
    // The palm: a long swollen club, crusted at its knuckles.
    const top = -10, palmLen = L * 0.38;
    arm.add(paint(lump(V(0, top - palmLen / 2, 0), [2.2 * k, palmLen / 2 + 0.8, 1.9 * k], 0.25, 3, 0.4), (v, n) => mix(fleshPaint(v), C.claw, 0.45), clawMat()));
    arm.add(paint(lump(V(side * 0.8 * k, top - 0.8, 0.6), [1.6 * k, 1.5, 1.4 * k], 0.5, 5, 0.55), crustPaint, crustMat()));
    if (stage >= 2) spikes(arm, V(side * 0.9 * k, top - 0.5, 0.5), [1.6 * k, 1.6, 1.4 * k], 3 + stage, 1.4);
    // The two fingers: the outer long and hooked, the inner shorter; a gloss from deep red to black at the tips.
    const y0 = top - palmLen + 0.6;
    const fingers = [
      { pts: [V(side * 1.0 * k, y0, -0.2), V(side * 1.7 * k, y0 - L * 0.3, -0.9), V(side * 1.0 * k, y0 - L * 0.52, -1.5), V(-side * 0.2, y0 - L * 0.62, -1.4)], r: [1.05 * k, 0.85 * k, 0.55 * k, 0.12] },
      { pts: [V(-side * 1.0 * k, y0, -0.2), V(-side * 1.5 * k, y0 - L * 0.26, -0.7), V(-side * 0.6 * k, y0 - L * 0.44, -1.2), V(side * 0.15, y0 - L * 0.5, -1.2)], r: [0.95 * k, 0.75 * k, 0.45 * k, 0.1] },
    ];
    for (const f of fingers) {
      const g = tube(f.pts, f.r, 0.75, 20, 12);
      const yTop = f.pts[0].y, yBot = f.pts[f.pts.length - 1].y;
      arm.add(paint(g, (v) => {
        const t = clamp((yTop - v.y) / (yTop - yBot));
        let c = mix(C.claw, C.clawLight, smooth(-0.2, 0.4, fbm(v.x * 0.8, v.y * 0.8, v.z * 0.8)) * 0.6);
        return mix(c, C.tip, smooth(0.65, 0.95, t));
      }, clawMat()));
      // Teeth along the inner edge.
      const curve = new THREE.CatmullRomCurve3(f.pts);
      for (let i = 1; i <= 3; i++) {
        const p = curve.getPointAt(0.12 + i * 0.16), toward = V(0 - p.x, 0, -0.4).normalize();
        const g2 = new THREE.ConeGeometry(0.28 * k, 0.9 * k, 6); g2.translate(0, 0.45 * k, 0);
        const m = paint(g2, () => C.tooth, mat({ roughness: 0.4 }));
        m.position.copy(p).addScaledVector(toward, 0.5 * k); m.quaternion.setFromUnitVectors(V(0, 1, 0), toward);
        arm.add(m);
      }
    }
    if (stage >= 3) pustules(arm, V(-side * 1.6, -5.5, -1.6), [1.2, 3, 0.6], 7);
  }

  // Crust patches: a shoulder and a knee first, then both knees and the feet with claws.
  if (stage >= 1) root.add(paint(lump(V(6.2, 23.8, 0), [3, 1.7, 2.7], 0.5, 8, 0.6), crustPaint, crustMat()));
  if (stage >= 1) root.add(paint(lump(V(-2, 7.2, -1.9), [2.4, 2.2, 1.1], 0.45, 6, 0.55), crustPaint, crustMat()));
  if (stage >= 2) {
    root.add(paint(lump(V(2, 7, -1.9), [2.4, 2.2, 1.1], 0.45, 6, 0.55), crustPaint, crustMat()));
    for (const x of [-2, 2]) {
      root.add(paint(lump(V(x, 1.1, -0.4), [2.6, 1.4, 2.8], 0.4, 6, 0.45), crustPaint, crustMat()));
      for (const dx of [-1.3, 0, 1.3]) {
        const g = tube([V(x + dx, 0.9, -2.4), V(x + dx * 1.1, 0.6, -3.8), V(x + dx * 1.15, 0.05, -4.6)], [0.45, 0.08], 1, 8, 8);
        root.add(paint(g, () => C.tip, clawMat()));
      }
    }
    if (stage < 3) root.add(paint(lump(V(-6.2, 23.6, 0), [2.6, 1.5, 2.4], 0.45, 6, 0.5), crustPaint, crustMat()));
  }

  // The shell: over the back, then over the head like a hood, its brow jutting over the face.
  if (stage >= 2) {
    const big = stage >= 3, s = big ? 1.25 : 1;
    const shell = new THREE.Group(); root.add(shell);
    shell.add(paint(lump(V(0, 19, 3.6), [7.4 * s, 8 * s, 3.2 * s], 0.7, big ? 26 : 16, 0.9), crustPaint, crustMat()));
    shell.add(paint(lump(V(0, 30.6 + (big ? 0.6 : 0), 3.6), [6.4 * s, 4.0 * s, 5.0 * s], 1.0, big ? 30 : 18, 1.0), crustPaint, crustMat()));
    shell.add(paint(lump(V(0, 32.4 + (big ? 0.6 : 0), -3.9 - (big ? 0.8 : 0)), [5.4 * s, 0.9, 1.4 * s], 0.5, 12, 0.6), crustPaint, crustMat()));
    shell.add(paint(lump(V(0, 25.5, 1.5), [8.2 * s, 2.6, 4.4 * s], 0.6, 12, 0.7), crustPaint, crustMat()));
    spikes(shell, V(0, 30.6 + (big ? 0.6 : 0), 3.6), [6.4 * s, 4.0 * s, 5.0 * s], big ? 24 : 12, big ? 2.4 : 1.6);
    spikes(shell, V(0, 19, 3.6), [7.4 * s, 8 * s, 3.2 * s], big ? 14 : 7, big ? 2.2 : 1.5, false);
    if (big) for (const sx of [-1, 1]) {
      root.add(paint(lump(V(sx * 8.5, 24.5, 0.5), [3.4, 2.4, 3.4], 0.6, 10, 0.7), crustPaint, crustMat()));
      spikes(root, V(sx * 8.5, 24.5, 0.5), [3.4, 2.4, 3.4], 5, 2.2);
    }
  }

  // The last stage: eyes on stalks, mandibles, a second pair of thin arms, pustules.
  if (stage >= 3) {
    for (const sx of [-1, 1]) {
      const g = tube([V(sx * 2.2, 31.2, -4.6), V(sx * 2.7, 32.4, -5.9), V(sx * 3.0, 32.9, -6.8)], [0.55, 0.38], 1, 10, 10);
      root.add(paint(g, fleshPaint, fleshMat()));
      const eye = new THREE.Mesh(new THREE.SphereGeometry(0.7, 20, 16), new THREE.MeshStandardMaterial({ color: '#2a8fd6', emissive: '#082c52', roughness: 0.12 }));
      eye.position.set(sx * 3.05, 33.2, -7.2); root.add(eye);
      const pupil = new THREE.Mesh(new THREE.SphereGeometry(0.3, 12, 10), new THREE.MeshStandardMaterial({ color: '#050505', roughness: 0.2 }));
      pupil.position.set(sx * 3.08, 33.2, -7.82); root.add(pupil);
      // Mandibles at the mouth.
      const mg = tube([V(sx * 1.4, 25.6, -4.1), V(sx * 1.9, 24.6, -5.2), V(sx * 0.6, 23.6, -5.6)], [0.55, 0.12], 1, 10, 8);
      root.add(paint(mg, (v) => mix(C.crust, C.tip, smooth(25.5, 23.6, v.y)), clawMat()));
      // A thin arm from under the shoulder, hanging in front of the big one, with a small pincer.
      const ag = tube([V(sx * 4.3, 20.5, -1.6), V(sx * 5.4, 15, -3.4), V(sx * 5.0, 9.5, -3.6), V(sx * 4.4, 5.5, -3.0)], [0.75, 0.6, 0.5, 0.4], 1, 18, 10);
      root.add(paint(ag, fleshPaint, fleshMat()));
      for (const o of [-1, 1]) {
        const cg = tube([V(sx * 4.4, 5.6, -3.0), V(sx * 4.4 + o * 0.6, 4.2, -3.3), V(sx * 4.4 + o * 0.1, 2.9, -3.5)], [0.4, 0.08], 1, 8, 8);
        root.add(paint(cg, () => C.tip, clawMat()));
      }
    }
    pustules(root, V(-2.5, 15, -2.2), [2.5, 3, 0.4], 9);
    pustules(root, V(1.5, 25.2, -4.2), [1.6, 1.2, 0.2], 5);
  }

  // Light: a warm key from the front left, a cold rim from behind, a dim fill.
  const key = new THREE.DirectionalLight('#ffe2c4', 1.25); key.position.set(-30, 50, -40); root.add(key);
  const rim = new THREE.DirectionalLight('#7fb6ff', 1.2); rim.position.set(40, 30, 50); root.add(rim);
  const fill = new THREE.HemisphereLight('#51505a', '#1a1412', 0.6); root.add(fill);
  return root;
}

async function shoot(bb, stage, views, file, size) {
  for (const [view, [pos, target]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      while (ModelProject.all.length) await ModelProject.all[0].close(true);
      setupProject(Formats.free);
      const group = (${buildScene.toString()})(THREE, ${stage});
      Canvas.scene.add(group);
      const p = window.__sculptPreview || (window.__sculptPreview = new Preview({ id: 'sculpt_render', offscreen: true, antialias: true }));
      p.resize(${size[0]}, ${size[1]});
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 34 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#1d1c1f');
      const hidden = Canvas.scene.children.filter(o => o !== group && o.isLight && o.visible); hidden.forEach(o => o.visible = false);
      const out = await new Promise(res => p.screenshot({ crop: false }, d => res(d)));
      Canvas.scene.background = bg; hidden.forEach(o => o.visible = true); Canvas.scene.remove(group);
      group.traverse(o => { if (o.geometry) o.geometry.dispose(); if (o.material) o.material.dispose(); });
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
    await shoot(bb, STAGES.indexOf(name), VIEWS, v => path.join(OUT, `crab-sculpt-${name}-${v}.png`), [520, 640]);
    console.log('rendered', name);
  }
  bb.close();
}

if (require.main === module) main().catch(e => { console.error(e); process.exit(1); });
