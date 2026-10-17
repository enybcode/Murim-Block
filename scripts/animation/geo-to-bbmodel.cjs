// A GeckoLib (Bedrock) model opened in Blockbench itself, with its texture: renders it from a few sides to
// docs/mobs/images/<name>-<view>.png and saves the Blockbench project Blockbench makes of it to art/blockbench/<name>.bbmodel,
// so the model can be opened and edited there. With --pose=<animation>@<seconds>, the bones are posed by the model's own
// animation at that time first (GeckoLib's keys, Blockbench's playback).
//   node scripts/animation/geo-to-bbmodel.cjs <name> [--port=9234] [--pose=attack@1.0]
const fs = require('node:fs');
const path = require('node:path');
const { connect } = require('../concepts/chief-axes.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const name = process.argv[2];
const PORT = Number(arg('port') || 9234), POSE = arg('pose');

async function main() {
  const geo = fs.readFileSync(path.join(ASSETS, 'geo', 'entity', name + '.geo.json'), 'utf8');
  const anim = fs.existsSync(path.join(ASSETS, 'animations', 'entity', name + '.animation.json'))
    ? fs.readFileSync(path.join(ASSETS, 'animations', 'entity', name + '.animation.json'), 'utf8') : null;
  const png = 'data:image/png;base64,' + fs.readFileSync(path.join(ASSETS, 'textures', 'entity', name + '.png')).toString('base64');
  const bb = await connect(PORT);
  const loaded = await bb.evaluate(`(async () => {
    while (ModelProject.all.length) await ModelProject.all[0].close(true);
    newProject(Formats.bedrock);
    Codecs.bedrock.parse(JSON.parse(${JSON.stringify(geo)}), '${name}.geo.json');
    const tex = new Texture({ name: '${name}.png' }).fromDataURL(${JSON.stringify(png)}).add(false);
    await new Promise(r => setTimeout(r, 300));
    Cube.all.forEach(c => { for (const f in c.faces) c.faces[f].texture = tex.uuid; });
    ${anim ? `Animator.loadFile({ path: '${name}.animation.json', content: ${JSON.stringify(anim)} });` : ''}
    Canvas.updateAll();
    return Cube.all.length + ' cubes, ' + Group.all.length + ' bones, ' + (typeof Animation !== 'undefined' ? Animation.all.length : 0) + ' animations';
  })()`);
  console.log('Blockbench loaded', loaded);
  if (POSE) {
    const [animName, t] = POSE.split('@');
    console.log(await bb.evaluate(`(async () => {
      const a = Animation.all.find(x => x.name === '${animName}');
      if (!a) return 'no animation ${animName}';
      // Animations only show in the Animate mode.
      Modes.options.animate.select(); await new Promise(r => setTimeout(r, 200));
      a.select(); Timeline.setTime(${Number(t)}); Animator.preview(); Canvas.updateAll();
      await new Promise(r => setTimeout(r, 200));
      return 'posed at ${t} s';
    })()`));
  }
  const views = { front: [[0, 32, -105], [0, 26, 0]], three: [[-72, 46, -72], [0, 24, 0]], side: [[-105, 32, 0.01], [0, 26, 0]], top: [[0, 110, -30], [0, 20, 0]] };
  const out = path.join(ROOT, 'docs', 'mobs', 'images');
  fs.mkdirSync(out, { recursive: true });
  const suffix = POSE ? '-' + POSE.replace('@', '-').replace('.', '_') : '';
  for (const [view, [pos, target]] of Object.entries(views)) {
    const data = await bb.evaluate(`(async () => {
      ${POSE ? `{ const a = Animation.all.find(x => x.name === '${POSE.split('@')[0]}'); if (a) { Modes.options.animate.select(); a.select(); Timeline.setTime(${Number(POSE.split('@')[1])}); Animator.preview(); } }` : ''}
      const p = window.__geoPreview || (window.__geoPreview = new Preview({ id: 'geo_render', offscreen: true, antialias: true }));
      p.resize(520, 600);
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 38 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#2a2a2c');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(path.join(out, `${name}${suffix}-${view}.png`), Buffer.from(data.split(',')[1], 'base64'));
  }
  if (!POSE) {
    const project = await bb.evaluate(`Codecs.project.compile({ raw: false })`);
    fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', name + '.bbmodel'), project);
    console.log('saved art/blockbench/' + name + '.bbmodel');
  }
  bb.close();
}

main().catch(e => { console.error(e); process.exit(1); });
