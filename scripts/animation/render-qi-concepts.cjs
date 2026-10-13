// Concept images of the four proposed looks for Thousand Drizzle's wave of Qi (instead of Minecraft's dust specks),
// rendered in an isolated Blockbench: our swordsman at the end of the thrust, the effect flying ahead, a target dummy.
//   (isolated Blockbench: Blockbench.exe --remote-debugging-port=9232 --user-data-dir=<empty profile>)
//   node scripts/animation/render-qi-concepts.cjs <out dir> [--port=9232]
// Blockbench units are pixels (16 to a block); the swordsman faces north (-z).
const fs = require('node:fs');
const path = require('node:path');
const { project } = require('./blockbench-rig.cjs');
const { anims } = require('./azure-m1.cjs');

const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const PORT = Number(arg('port') || 9232);
const OUT = process.argv.slice(2).find(a => !a.startsWith('--')) || 'build/qi-concepts';

async function connect(port) {
  const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then(r => r.json());
  const target = targets.find(t => t.type === 'page' && /index\.html/.test(t.url));
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise(r => ws.addEventListener('open', r, { once: true }));
  let id = 0; const pending = new Map();
  ws.addEventListener('message', e => { const m = JSON.parse(e.data); const p = pending.get(m.id); if (!p) return; pending.delete(m.id);
    if (m.result && m.result.exceptionDetails) p.reject(new Error(JSON.stringify(m.result.exceptionDetails))); else p.resolve(m.result); });
  const evaluate = expression => new Promise((resolve, reject) => {
    const n = ++id; pending.set(n, { resolve: r => resolve(r.result.value), reject });
    ws.send(JSON.stringify({ id: n, method: 'Runtime.evaluate', params: { expression, awaitPromise: true, returnByValue: true } }));
  });
  return { evaluate, close: () => ws.close() };
}

/** Runs in Blockbench: builds each effect as a THREE group (glowing, additive), films it, removes it. */
const RUNTIME = String.raw`
window.QI = (() => {
  const glow = (color, opacity) => new THREE.MeshBasicMaterial({ color: new THREE.Color(color), transparent: true, opacity,
    blending: THREE.AdditiveBlending, depthWrite: false });
  const box = (g, w, h, d, x, y, z, mat, ry = 0, rx = 0) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat);
    m.position.set(x, y, z); m.rotation.set(rx, ry, 0, 'YXZ'); g.add(m); return m;
  };
  /** A blocky crescent lying level, bowed forward: cubes along an arc of radius r, thick in the middle, pointed at the ends. */
  const crescent = (g, cz, cy, r, opacity, color, cell = 1.6) => {
    const steps = 28;
    for (let i = 0; i <= steps; i++) {
      const a = -1.15 + 2.3 * i / steps, k = 1 - Math.abs(a) / 1.15;
      const thick = 1 + 4 * Math.sqrt(k);
      for (let t = 0; t < thick; t += cell * 0.9) {
        const rr = r - t;
        box(g, cell, cell * 0.8, cell, Math.sin(a) * rr, cy, cz - Math.cos(a) * rr + r, glow(color, opacity * (0.55 + 0.45 * (1 - t / thick))));
      }
    }
  };
  /**
   * A ghost of the sword, point first along -z ('yaw' turns it about the vertical), at (x, y, z) its blade's middle,
   * 'scale' times the real sword, 'opacity' its glow. 'flat' lays it level (for a spinning blade).
   */
  const ghost = (g, x, y, z, scale, opacity, yaw = 0, roll = 0, flat = false) => {
    const s = new THREE.Group();
    const add = (w, h, d, ox, oy, oz, color, o) => {
      const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), glow(color, o));
      m.position.set(ox, oy, oz); s.add(m);
    };
    add(0.9, 2.4, 22, 0, 0, 0, 0xbfe8ff, opacity);
    add(0.5, 0.9, 22.4, 0, -1.1, 0, 0xffffff, opacity);
    add(0.5, 0.9, 22.4, 0, 1.1, 0, 0xffffff, opacity * 0.7);
    add(4, 1.4, 1.4, 0, 0, 11.8, 0x9fd0ff, opacity);
    add(1.2, 1.2, 5, 0, 0, 15, 0x6fb0e8, opacity);
    s.scale.setScalar(scale);
    s.position.set(x, y, z);
    s.rotation.set(flat ? Math.PI / 2 : 0, yaw, roll, 'YXZ');
    if (flat) { s.rotation.set(0, yaw, Math.PI / 2, 'YXZ'); }
    g.add(s);
    return s;
  };
  /** A target dummy for scale: a zombie-sized figure in plain blocks. */
  const dummy = (g, z) => {
    const skin = new THREE.MeshLambertMaterial({ color: 0x5f8a4a }), shirt = new THREE.MeshLambertMaterial({ color: 0x2f7f86 }),
      pants = new THREE.MeshLambertMaterial({ color: 0x3a3f8a });
    box(g, 8, 8, 8, 0, 28, z, skin); box(g, 8, 12, 4, 0, 18, z, shirt);
    box(g, 4, 12, 4, -6, 18, z, shirt); box(g, 4, 12, 4, 6, 18, z, shirt);
    box(g, 4, 12, 4, -2, 6, z, pants); box(g, 4, 12, 4, 2, 6, z, pants);
  };
  const EFFECTS = {
    crescent(g) {
      // The blade of Qi and two fading afterimages behind it.
      crescent(g, -78, 19, 26, 0.95, 0xbfe8ff);
      crescent(g, -64, 19, 25, 0.35, 0x7fc6ff);
      crescent(g, -52, 19, 24, 0.15, 0x5aa8f0);
      box(g, 2, 2, 2, 0, 19, -52, glow(0xffffff, 0.9));
    },
    needles(g) {
      // Six fine needles of Qi fanned a little, each a bright head on a fading streak.
      const fan = [[-11, 22], [-6, 15], [-2, 25], [3, 18], [7, 23], [12, 16]];
      fan.forEach(([deg, y], i) => {
        const a = deg * Math.PI / 180, d = 70 + (i % 3) * 7;
        const hx = Math.sin(a) * d, hz = -Math.cos(a) * d - 8;
        for (let s = 0; s < 8; s++) {
          const back = s * 4.5, x = hx - Math.sin(a) * back, z = hz + Math.cos(a) * back;
          box(g, s === 0 ? 1.4 : 0.9, s === 0 ? 1.4 : 0.9, 4.5, x, y, z, glow(s === 0 ? 0xffffff : 0x9fd8ff, 0.9 * (1 - s / 8)), -a);
        }
      });
    },
    phantom(g) {
      // A ghost of the sword flying point first, two afterimages behind.
      [[-80, 0.8], [-66, 0.32], [-53, 0.14]].forEach(([z, o]) => {
        box(g, 0.9, 2.4, 22, 0, 20, z, glow(0xbfe8ff, o));
        box(g, 0.5, 0.9, 22, 0, 18.9, z, glow(0xffffff, o));
        box(g, 4, 1.4, 1.4, 0, 20, z + 11.8, glow(0x9fd0ff, o));
        box(g, 1.2, 1.2, 5, 0, 20, z + 15, glow(0x6fb0e8, o));
      });
      box(g, 3, 3, 3, 0, 20, -92, glow(0xffffff, 0.7));
    },
    // ---------------------------------------------------------------- variants of the phantom blade
    phantom_trail(g) {
      // One ghost sword, and behind it a continuous ribbon of light that thins and fades (no separate afterimages).
      ghost(g, 0, 20, -82, 1, 0.85);
      for (let s = 0; s < 18; s++) {
        const z = -70 + s * 2.2, k = 1 - s / 18;
        box(g, 0.5, 2.6 * k + 0.4, 2.3, 0, 20, z, glow(s < 4 ? 0xffffff : 0x8fd0ff, 0.75 * k));
      }
    },
    phantom_volley(g) {
      // Three ghost swords side by side in a tight fan, one a little ahead: "a thousand drops".
      [[-7, 15, -74, 0.65], [0, 23, -86, 0.9], [7, 15, -74, 0.65]].forEach(([x, y, z, o]) => {
        ghost(g, x, y, z, 0.85, o);
        for (let s = 1; s < 8; s++) box(g, 0.5, 1.2, 2.5, x, y, z + 10 + s * 2.5, glow(0x8fd0ff, o * (1 - s / 8) * 0.7));
      });
    },
    phantom_spin(g) {
      // The ghost sword flies level and spins like a blade wheel; its turn leaves a ring of light.
      [0, 45, 90, 135].forEach((deg, i) => ghost(g, 0, 20, -80, 0.9, i === 0 ? 0.9 : 0.22, deg * Math.PI / 180, 0, true));
      for (let i = 0; i < 40; i++) {
        const a = Math.PI * 2 * i / 40;
        box(g, 1.4, 0.6, 1.4, Math.cos(a) * 12, 20, -80 + Math.sin(a) * 12, glow(i % 2 ? 0xbfe8ff : 0x7fc0ff, 0.35));
      }
      for (let s = 0; s < 8; s++) box(g, 10 - s, 0.4, 2, 0, 20, -64 + s * 2.5, glow(0x7fc0ff, 0.25 * (1 - s / 8)));
    },
    phantom_giant(g) {
      // One great ghost sword, twice the size, its edges bright, slower and heavier.
      ghost(g, 0, 22, -86, 2.1, 0.75);
      for (let s = 0; s < 5; s++) ghost(g, 0, 22, -72 + s * 7, 2.1, 0.12 * (1 - s / 5));
    },
    cloud(g) {
      // Rings of pale mist opening forward like a shock wave.
      [[-46, 7, 0.55], [-62, 11, 0.4], [-80, 15, 0.25]].forEach(([z, r, o]) => {
        for (let i = 0; i < 26; i++) {
          const a = Math.PI * 2 * i / 26, s = 3.2 + (i % 3) * 0.8;
          box(g, s, s, s * 0.8, Math.cos(a) * r, 19 + Math.sin(a) * r, z, glow(i % 2 ? 0xd8eeff : 0x9fcaf0, o));
        }
      });
    },
  };
  async function render(name, view, w, h) {
    const g = new THREE.Group();
    EFFECTS[name](g);
    dummy(g, -112);
    const light = new THREE.AmbientLight(0xffffff, 0.15);
    g.add(light);
    Canvas.scene.add(g);
    const oldBg = Canvas.scene.background;
    Canvas.scene.background = new THREE.Color('#2c3b4a');
    const p = window.__qiPreview || (window.__qiPreview = new Preview({ id: 'qi_concepts', offscreen: true, antialias: true }));
    p.resize(w, h);
    p.loadAnglePreset({ position: view.position, target: view.target, projection: 'perspective', fov: 38 });
    p.camera.far = 100000; p.camera.updateProjectionMatrix(); p.camera.lookAt(...view.target);
    const data = await new Promise(res => p.screenshot({ crop: false }, res));
    Canvas.scene.remove(g); Canvas.scene.background = oldBg;
    g.traverse(o => { if (o.geometry) o.geometry.dispose(); if (o.material) o.material.dispose(); });
    return data;
  }
  return { render, names: Object.keys(EFFECTS) };
})();
true;
`;

(async () => {
  const c = await connect(PORT);
  try {
    // The swordsman held at the end of the thrust (Piercing Mist's lunge), the effect leaving the blade.
    const lunge = anims.piercing_mist[2].pose;
    const still = { thrust: [{ t: 0, pose: { ...lunge, Root: { pos: [0, 0, lunge.Root.pos[2]] } } }, { t: 0.05, pose: { ...lunge, Root: { pos: [0, 0, lunge.Root.pos[2]] } } }] };
    const model = JSON.stringify(project('qi_concepts', still));
    await c.evaluate(`(async () => {
      if (typeof Project !== 'undefined' && Project) await Project.close(true);
      setupProject(Formats.free);
      Codecs.project.parse(JSON.parse(${JSON.stringify(model)}), 'qi.bbmodel');
      Animation.all[0].select(); Timeline.setTime(0.02); Animator.preview();
      return true;
    })()`);
    await c.evaluate(RUNTIME);
    fs.mkdirSync(OUT, { recursive: true });
    const view = { position: [118, 48, 12], target: [0, 16, -54] };
    const labels = { crescent: '1-croissant-de-qi', needles: '2-bruine-d-aiguilles', phantom: '3-lame-fantome', cloud: '4-vague-de-nuage',
      phantom_trail: '3a-lame-fantome-trainee', phantom_volley: '3b-volee-de-lames', phantom_spin: '3c-lame-tournoyante', phantom_giant: '3d-lame-geante' };
    const only = arg('only') ? arg('only').split(',') : null;
    for (const name of (await c.evaluate('QI.names')).filter(n => !only || only.includes(n))) {
      const png = await c.evaluate(`QI.render(${JSON.stringify(name)}, ${JSON.stringify(view)}, 1280, 720)`);
      const file = path.join(OUT, labels[name] + '.png');
      fs.writeFileSync(file, Buffer.from(png.split(',')[1], 'base64'));
      console.log('wrote', file);
    }
  } finally { c.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
