// Renders an Epic Fight mesh as written (animmodels/entity/<name>.json and its atlas), in its rest pose, through
// Blockbench's offscreen preview: to look at a generated mob before the game. Views from the front three-quarter, the
// back three-quarter, the side and the face, on a dark ground, to docs/mobs/images/<name>-<view>.png.
//   node scripts/animation/mesh-preview.cjs <name> [--port=9234] [--scale=1.5] [--out=docs/mobs/images]
const fs = require('node:fs');
const path = require('node:path');
const { connect } = require('../concepts/chief-axes.cjs');

const ROOT = path.join(__dirname, '..', '..');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const name = process.argv[2];
if (!name || name.startsWith('--')) { console.error('usage: node scripts/animation/mesh-preview.cjs <name>'); process.exit(1); }
const PORT = Number(arg('port') || 9234), SCALE = Number(arg('scale') || 1), OUT = path.join(ROOT, arg('out') || 'docs/mobs/images');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');

/** Runs in Blockbench's page: the mesh as a THREE.Group, Blockbench pixels (y up, facing -z). */
function build(THREE, mesh, png, scale) {
  const v = mesh.vertices, pos = v.positions.array, uv = v.uvs.array, nor = v.normals.array;
  const P = [], U = [], N = [];
  for (const part of Object.values(v.parts)) {
    const a = part.array;
    for (let i = 0; i < a.length; i += 3) {
      const p = a[i] * 3, t = a[i + 1] * 2, n = a[i + 2] * 3;
      // Epic Fight's mesh space (blocks, z up, the front toward +y) back to Blockbench pixels.
      P.push(pos[p] * 16, pos[p + 2] * 16, -pos[p + 1] * 16);
      U.push(uv[t], uv[t + 1]);
      N.push(nor[n], nor[n + 2], -nor[n + 1]);
    }
  }
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.Float32BufferAttribute(P, 3));
  g.setAttribute('uv', new THREE.Float32BufferAttribute(U, 2));
  g.setAttribute('normal', new THREE.Float32BufferAttribute(N, 3));
  const img = new Image(); img.src = png;
  const tex = new THREE.Texture(img);
  tex.magFilter = THREE.NearestFilter; tex.minFilter = THREE.NearestFilter; tex.flipY = false;
  if ('colorSpace' in tex) tex.colorSpace = THREE.SRGBColorSpace; else tex.encoding = THREE.sRGBEncoding;
  const root = new THREE.Group();
  const m = new THREE.Mesh(g, new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.1, side: THREE.DoubleSide }));
  m.scale.setScalar(scale);
  root.add(m);
  const key = new THREE.DirectionalLight('#fff1e0', 1.15); key.position.set(-20, 60, -35); root.add(key);
  const rim = new THREE.DirectionalLight('#8fb8ff', 0.55); rim.position.set(30, 20, 50); root.add(rim);
  root.add(new THREE.AmbientLight('#8a8790', 0.85));
  return { root, img, tex };
}

async function main() {
  const mesh = JSON.parse(fs.readFileSync(path.join(ASSETS, 'animmodels', 'entity', name + '.json'), 'utf8'));
  const png = 'data:image/png;base64,' + fs.readFileSync(path.join(ASSETS, 'textures', 'entity', name + '.png')).toString('base64');
  const s = SCALE, h = 34 * s;
  const views = {
    front: [[-38 * s, 30 * s, -64 * s], [0, h * 0.55, 0]], back: [[44 * s, 34 * s, 58 * s], [0, h * 0.58, 0]],
    side: [[-74 * s, 26 * s, -8 * s], [0, h * 0.55, 0]], face: [[-16 * s, 30 * s, -36 * s], [0, 27 * s, 0]],
  };
  fs.mkdirSync(OUT, { recursive: true });
  const bb = await connect(PORT);
  for (const [view, [p, t]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      while (ModelProject.all.length) await ModelProject.all[0].close(true);
      setupProject(Formats.free);
      const { root, img, tex } = (${build.toString()})(THREE, ${JSON.stringify(mesh)}, ${JSON.stringify(png)}, ${s});
      await new Promise(r => img.complete ? r() : img.onload = r); tex.needsUpdate = true;
      Canvas.scene.add(root);
      const pv = window.__meshPreview || (window.__meshPreview = new Preview({ id: 'mesh_render', offscreen: true, antialias: true }));
      pv.resize(560, 680);
      pv.loadAnglePreset({ position: ${JSON.stringify(p)}, target: ${JSON.stringify(t)}, projection: 'perspective', fov: 34 });
      pv.camera.lookAt(...${JSON.stringify(t)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#1f1e22');
      const hidden = Canvas.scene.children.filter(o => o !== root && o.isLight && o.visible); hidden.forEach(o => o.visible = false);
      const out = await new Promise(res => pv.screenshot({ crop: false }, d => res(d)));
      Canvas.scene.background = bg; hidden.forEach(o => o.visible = true); Canvas.scene.remove(root);
      root.traverse(o => { if (o.geometry) o.geometry.dispose(); if (o.material) { if (o.material.map) o.material.map.dispose(); o.material.dispose(); } });
      return out;
    })()`);
    fs.writeFileSync(path.join(OUT, `${name}-${view}.png`), Buffer.from(data.split(',')[1], 'base64'));
  }
  bb.close();
  console.log('rendered', name, Object.keys(views).join(', '));
}

main().catch(e => { console.error(e); process.exit(1); });
