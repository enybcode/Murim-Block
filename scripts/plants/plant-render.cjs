// A herb's GeckoLib models seen in Blockbench (plant-kit.cjs): every stage in every colour, from a player's height at
// three-quarters, on a patch of grass, posed at a moment of its sway; to PNGs in <out> (<herb>-<colour>-stage<k>.png).
//   node scripts/plants/plant-render.cjs <herb> <out dir> [--colours=gold,white] [--port=9235] [--t=1.0] [--view=three]
const fs = require('node:fs');
const path = require('node:path');
const { connect } = require('../concepts/chief-axes.cjs');
const { ASSETS } = require('./plant-kit.cjs');

const arg = n => (process.argv.find(a => a.startsWith(`--${n}=`)) || '').split('=')[1];
const [herb, out] = process.argv.slice(2).filter(a => !a.startsWith('--'));
const PORT = Number(arg('port') || 9235), T = Number(arg('t') || 1.0);
const VIEWS = { three: [[-17, 15, -22], [0, 6.5, 0]], side: [[-27, 8, 0.01], [0, 6.5, 0]], top: [[-4, 30, -12], [0, 5, 0]], close: [[-9, 14, -12], [0, 10, 0]] };

async function main() {
  const dir = path.join(ASSETS, 'textures', 'block', 'plant');
  const colours = arg('colours') ? arg('colours').split(',') : fs.readdirSync(dir).filter(f => f.startsWith(herb + '_') && !f.includes('glowmask')).map(f => f.slice(herb.length + 1, -4));
  const anim = fs.readFileSync(path.join(ASSETS, 'animations', 'block', 'plant', herb + '.animation.json'), 'utf8');
  const stages = fs.readdirSync(path.join(ASSETS, 'geo', 'block', 'plant')).filter(f => f.startsWith(herb + '_stage')).length;
  fs.mkdirSync(out, { recursive: true });
  const bb = await connect(PORT);
  for (const colour of colours) {
    const png = 'data:image/png;base64,' + fs.readFileSync(path.join(dir, `${herb}_${colour}.png`)).toString('base64');
    for (let s = 0; s < stages; s++) {
      if (arg('stage') && Number(arg('stage')) !== s) continue;
      const geo = fs.readFileSync(path.join(ASSETS, 'geo', 'block', 'plant', `${herb}_stage${s}.geo.json`), 'utf8');
      await bb.evaluate(`(async () => {
        while (ModelProject.all.length) await ModelProject.all[0].close(true);
        newProject(Formats.bedrock);
        Codecs.bedrock.parse(JSON.parse(${JSON.stringify(geo)}), 'plant.geo.json');
        // A patch of grass under it: a block's top.
        const ground = new Cube({ name: 'ground', from: [-8, -1, -8], to: [8, 0, 8] }).init();
        const tex = new Texture({ name: 'plant.png' }).fromDataURL(${JSON.stringify(png)}).add(false);
        await new Promise(r => setTimeout(r, 250));
        Cube.all.forEach(c => { if (c.name !== 'ground') for (const f in c.faces) c.faces[f].texture = tex.uuid; });
        Animator.loadFile({ path: 'plant.animation.json', content: ${JSON.stringify(anim)} });
        Modes.options.animate.select(); await new Promise(r => setTimeout(r, 150));
        for (const name of ['sway', 'spirit']) { const a = Animation.all.find(x => x.name === name); if (a) { a.select(); Timeline.setTime(${T}); } }
        Animator.preview();
        Canvas.updateAll();
        await new Promise(r => setTimeout(r, 150));
      })()`);
      for (const view of (arg('view') || 'three').split(',')) {
        const [pos, target] = VIEWS[view];
        const data = await bb.evaluate(`(async () => {
          const p = window.__plantPreview || (window.__plantPreview = new Preview({ id: 'plant_render', offscreen: true, antialias: true }));
          p.resize(420, 420);
          p.loadAnglePreset({ position: ${JSON.stringify(pos)}, target: ${JSON.stringify(target)}, projection: 'perspective', fov: 40 });
          p.camera.lookAt(...${JSON.stringify(target)});
          const ground = Cube.all.find(c => c.name === 'ground');
          if (ground && ground.mesh) ground.mesh.material = new THREE.MeshLambertMaterial({ color: 0x5b8a3a });
          const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#9fc3e0');
          return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
        })()`);
        fs.writeFileSync(path.join(out, `${herb}-${colour}-stage${s}${view === 'three' ? '' : '-' + view}${arg('frame') ? '-' + arg('frame') : ''}.png`), Buffer.from(data.split(',')[1], 'base64'));
      }
    }
  }
  bb.close();
  console.log('rendered', herb, colours.join(','), stages, 'stages to', out);
}

/** A bed of nine plants of one stage, three blocks square, colours mixed and turned each its way: as a player meets them. */
async function field(stage) {
  const dir = path.join(ASSETS, 'textures', 'block', 'plant');
  const colours = fs.readdirSync(dir).filter(f => f.startsWith(herb + '_') && !f.includes('glowmask')).map(f => f.slice(herb.length + 1, -4));
  const pngs = colours.map(c => 'data:image/png;base64,' + fs.readFileSync(path.join(dir, `${herb}_${c}.png`)).toString('base64'));
  const geo = fs.readFileSync(path.join(ASSETS, 'geo', 'block', 'plant', `${herb}_stage${stage}.geo.json`), 'utf8');
  const bb = await connect(PORT);
  const data = await bb.evaluate(`(async () => {
    while (ModelProject.all.length) await ModelProject.all[0].close(true);
    newProject(Formats.free);
    const texs = [];
    for (const src of ${JSON.stringify(pngs)}) texs.push(new Texture({ name: 't' + texs.length + '.png' }).fromDataURL(src).add(false));
    await new Promise(r => setTimeout(r, 300));
    const model = JSON.parse(${JSON.stringify(geo)})['minecraft:geometry'][0];
    const tw = model.description.texture_width, th = model.description.texture_height;
    let k = 0;
    for (let gx = -1; gx <= 1; gx++) for (let gz = -1; gz <= 1; gz++, k++) {
      const turn = [0, 90, 180, 270][(k * 7) % 4], tex = texs[(k * 5 + 1) % texs.length];
      const ox = gx * 16 + ((k * 13) % 5) - 2, oz = gz * 16 + ((k * 11) % 5) - 2;
      const group = new Group({ name: 'plant' + k, origin: [ox, 0, oz], rotation: [0, turn, 0] }).init();
      for (const b of model.bones) for (const c of (b.cubes || [])) {
        const cube = new Cube({ name: b.name, from: [c.origin[0] + ox, c.origin[1], c.origin[2] + oz],
          to: [c.origin[0] + c.size[0] + ox, c.origin[1] + c.size[1], c.origin[2] + c.size[2] + oz],
          origin: c.pivot ? [c.pivot[0] + ox, c.pivot[1], c.pivot[2] + oz] : [ox, 0, oz], rotation: c.rotation || [0, 0, 0] }).addTo(group).init();
        for (const f in cube.faces) {
          const u = c.uv[f]; if (!u) continue;
          cube.faces[f].texture = tex.uuid;
          cube.faces[f].uv = [u.uv[0] * 16 / tw * (Project.texture_width / 16), u.uv[1] * 16 / th * (Project.texture_height / 16), (u.uv[0] + u.uv_size[0]) * Project.texture_width / tw, (u.uv[1] + u.uv_size[1]) * Project.texture_height / th];
        }
      }
    }
    new Cube({ name: 'ground', from: [-24, -1, -24], to: [24, 0, 24] }).init();
    Canvas.updateAll();
    await new Promise(r => setTimeout(r, 300));
    const p = window.__plantPreview || (window.__plantPreview = new Preview({ id: 'plant_render', offscreen: true, antialias: true }));
    p.resize(900, 560);
    p.loadAnglePreset({ position: [-30, 34, -52], target: [0, 4, 0], projection: 'perspective', fov: 45 });
    p.camera.lookAt(0, 4, 0);
    const ground = Cube.all.find(c => c.name === 'ground');
    if (ground && ground.mesh) ground.mesh.material = new THREE.MeshLambertMaterial({ color: 0x5b8a3a });
    const bg = Canvas.scene.background; Canvas.scene.background = new THREE.Color('#9fc3e0');
    return await new Promise(res => p.screenshot({ crop: false }, d => { Canvas.scene.background = bg; res(d); }));
  })()`);
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, `${herb}-field-stage${stage}.png`), Buffer.from(data.split(',')[1], 'base64'));
  bb.close();
  console.log('rendered a bed of', herb, 'stage', stage);
}

(arg('field') ? field(Number(arg('field'))) : main()).catch(e => { console.error(e); process.exit(1); });
