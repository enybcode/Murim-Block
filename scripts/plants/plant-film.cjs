// Films a herb's animations in Blockbench (plant-kit.cjs): the model loaded once, its looping animations played to each
// moment in turn, a picture a frame, to <out>/<herb>-<colour>-stage<k>/frame-NNN.png (then FramesToGif).
//   node scripts/plants/plant-film.cjs <herb> <out dir> [--colour=gold] [--stage=2] [--frames=20] [--length=4]
//        [--view=three|close] [--port=9235]
const fs = require('node:fs');
const path = require('node:path');
const { connect } = require('../concepts/chief-axes.cjs');
const { ASSETS } = require('./plant-kit.cjs');

const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const [herb, out] = process.argv.slice(2).filter(a => !a.startsWith('--'));
const PORT = Number(arg('port') || 9235), STAGE = Number(arg('stage') || 0), FRAMES = Number(arg('frames') || 20), LENGTH = Number(arg('length') || 4);
const COLOUR = arg('colour') || 'default';
const VIEWS = { three: [[-17, 15, -22], [0, 6.5, 0]], close: [[-11, 13, -15], [0, 7.5, 0]] };

async function main() {
  const tex = 'data:image/png;base64,' + fs.readFileSync(path.join(ASSETS, 'textures', 'block', 'plant', `${herb}_${COLOUR}.png`)).toString('base64');
  const geo = fs.readFileSync(path.join(ASSETS, 'geo', 'block', 'plant', `${herb}_stage${STAGE}.geo.json`), 'utf8');
  const anim = fs.readFileSync(path.join(ASSETS, 'animations', 'block', 'plant', herb + '.animation.json'), 'utf8');
  const dir = path.join(out, `${herb}-${COLOUR}-stage${STAGE}`);
  fs.mkdirSync(dir, { recursive: true });
  const bb = await connect(PORT);
  await bb.evaluate(`(async () => {
    while (ModelProject.all.length) await ModelProject.all[0].close(true);
    newProject(Formats.bedrock);
    Codecs.bedrock.parse(JSON.parse(${JSON.stringify(geo)}), 'plant.geo.json');
    new Cube({ name: 'ground', from: [-8, -1, -8], to: [8, 0, 8] }).init();
    const tex = new Texture({ name: 'plant.png' }).fromDataURL(${JSON.stringify(tex)}).add(false);
    await new Promise(r => setTimeout(r, 300));
    Cube.all.forEach(c => { if (c.name !== 'ground') for (const f in c.faces) c.faces[f].texture = tex.uuid; });
    Animator.loadFile({ path: 'plant.animation.json', content: ${JSON.stringify(anim)} });
    Modes.options.animate.select();
    await new Promise(r => setTimeout(r, 200));
    // Every animation of the herb plays at once (the sway, and the spirit's motes on stage 3).
    Animation.all.forEach(a => { a.playing = true; });
    Canvas.updateAll();
  })()`);
  const [pos, target] = VIEWS[arg('view') || 'three'];
  for (let f = 0; f < FRAMES; f++) {
    const t = LENGTH * f / FRAMES;
    const data = await bb.evaluate(`(async () => {
      Timeline.setTime(${t});
      Animator.preview();
      const p = window.__plantFilm || (window.__plantFilm = new Preview({ id: 'plant_film', offscreen: true, antialias: true }));
      p.resize(420, 420);
      p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 40 });
      p.camera.lookAt(...${JSON.stringify(target)});
      const ground = Cube.all.find(c => c.name === 'ground');
      if (ground && ground.mesh) ground.mesh.material = new THREE.MeshLambertMaterial({ color: 0x5b8a3a });
      const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#9fc3e0');
      return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
    })()`);
    fs.writeFileSync(path.join(dir, `frame-${String(f).padStart(3, '0')}.png`), Buffer.from(data.split(',')[1], 'base64'));
  }
  bb.close();
  console.log('filmed', herb, COLOUR, 'stage', STAGE, FRAMES, 'frames to', dir);
}

main().catch(e => { console.error(e); process.exit(1); });
