// The Orthodox Alliance's Rattan Shield (藤牌), style 2 of scripts/concepts/alliance-shields.cjs, as a game item: the
// concept's own cubes, moved into the vanilla shield's frame so the vanilla shield's hand transforms (and Epic Fight's
// shield skin, item_skins/rattan_shield.json) hold it on the forearm as they hold a shield:
//   the vanilla shield is drawn about the item's corner (0, 0, 0), its plate 1 to 2 pixels out on +z, facing +z, its
//   handle behind; so is this one: its middle at x 0, y 0, its back at z 1, its dome swelling toward +z.
//   models/item/rattan_shield.json           the shield (the vanilla shield's transforms; blocking -> the next one);
//   models/item/rattan_shield_blocking.json  the same, raised (the vanilla shield's blocking transforms);
//   textures/item/rattan_shield.png          the concept's palette: one row of 16 shades per material;
//   art/blockbench/rattan_shield.bbmodel     the Blockbench project.
// Usage: node scripts/animation/rattan-shield-model.cjs
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { palette, builder } = require('../concepts/chief-axes.cjs');
const { STYLES, CENTRE } = require('../concepts/alliance-shields.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NAME = 'rattan_shield';
const style = STYLES.rattan;

// The concept's cubes: its face toward -z about (CX, CY). Turned to face +z (z flipped), its middle at the origin, its
// back (concept z 0.5) at z 1 like the vanilla plate's.
const b = builder();
style.build(b);
const pal = palette(style.colours);
const BACK = 0.5, SHIFT = 1 + BACK;
const PARTS = b.parts.map((p, i) => {
  const [x0, y0, z0] = p.from, [x1, y1, z1] = p.to;
  return {
    name: `${p.mat}_${i}`, mat: p.mat, shade: p.shade,
    from: [x0 - CENTRE[0], y0 - CENTRE[1], SHIFT - z1], to: [x1 - CENTRE[0], y1 - CENTRE[1], SHIFT - z0],
  };
});

const r4 = v => Math.round(v * 1e4) / 1e4;
const uvOf = p => { const row = pal.names.indexOf(p.mat); return [p.shade, row, p.shade + 1, row + 1]; };
const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
const elements = PARTS.map(p => ({ name: p.name, from: p.from.map(r4), to: p.to.map(r4),
  faces: Object.fromEntries(FACES.map(f => [f, { uv: uvOf(p), texture: '#0' }])) }));

/** The vanilla shield's transforms (minecraft:item/shield and shield_blocking). */
const DISPLAY = {
  thirdperson_righthand: { rotation: [0, 90, 0], translation: [10, 6, -4], scale: [1, 1, 1] },
  thirdperson_lefthand: { rotation: [0, 90, 0], translation: [10, 6, 12], scale: [1, 1, 1] },
  firstperson_righthand: { rotation: [0, 180, 5], translation: [-10, 2, -10], scale: [1.25, 1.25, 1.25] },
  firstperson_lefthand: { rotation: [0, 180, 5], translation: [10, 0, -10], scale: [1.25, 1.25, 1.25] },
  gui: { rotation: [15, -25, -5], translation: [2, 3, 0], scale: [0.65, 0.65, 0.65] },
  fixed: { rotation: [0, 180, 0], translation: [-4.5, 4.5, -5], scale: [0.55, 0.55, 0.55] },
  ground: { rotation: [0, 0, 0], translation: [2, 4, 2], scale: [0.25, 0.25, 0.25] },
};
const BLOCKING = {
  thirdperson_righthand: { rotation: [45, 155, 0], translation: [-3.49, 11, -2], scale: [1, 1, 1] },
  thirdperson_lefthand: { rotation: [45, 155, 0], translation: [11.51, 7, 2.5], scale: [1, 1, 1] },
  firstperson_righthand: { rotation: [0, 180, -5], translation: [-15, 5, -11], scale: [1.25, 1.25, 1.25] },
  firstperson_lefthand: { rotation: [0, 180, -5], translation: [5, 5, -11], scale: [1.25, 1.25, 1.25] },
  gui: DISPLAY.gui,
};
const CREDIT = `Murimblock - scripts/animation/rattan-shield-model.cjs (Blockbench source: art/blockbench/${NAME}.bbmodel)`;
const model = {
  credit: CREDIT, gui_light: 'front', textures: { 0: `murimblock:item/${NAME}`, particle: `murimblock:item/${NAME}` },
  elements, display: DISPLAY,
  overrides: [{ predicate: { blocking: 1 }, model: `murimblock:item/${NAME}_blocking` }],
};
const blocking = { credit: CREDIT, parent: `murimblock:item/${NAME}`, display: BLOCKING };

function bbmodel() {
  const id = name => { const h = crypto.createHash('md5').update(NAME + '/' + name).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`; };
  const els = PARTS.map(p => ({ name: p.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
    from: p.from.map(r4), to: p.to.map(r4), autouv: 0, color: 0, origin: [0, 0, 0], rotation: [0, 0, 0],
    faces: Object.fromEntries(FACES.map(f => [f, { uv: uvOf(p), texture: 0 }])), type: 'cube', uuid: id(p.name) }));
  return {
    meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
    name: NAME, parent: '', ambientocclusion: true, front_gui_light: true, visible_box: [1, 1, 0],
    variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: 16, height: 16 }, elements: els, outliner: els.map(e => e.uuid),
    textures: [{ path: '', name: `${NAME}.png`, folder: 'item', namespace: 'murimblock', id: '0', width: 16, height: 16, uv_width: 16, uv_height: 16,
      particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', frame_time: 1,
      frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false, uuid: id('texture'),
      source: 'data:image/png;base64,' + pal.png.toString('base64') }],
    display: DISPLAY,
  };
}

if (require.main === module) {
  const assets = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'murimblock');
  fs.writeFileSync(path.join(assets, 'textures', 'item', `${NAME}.png`), pal.png);
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}.json`), JSON.stringify(model, null, 1) + '\n');
  fs.writeFileSync(path.join(assets, 'models', 'item', `${NAME}_blocking.json`), JSON.stringify(blocking, null, 2) + '\n');
  fs.writeFileSync(path.join(ROOT, 'art', 'blockbench', `${NAME}.bbmodel`), JSON.stringify(bbmodel(), null, 1));
  const span = k => [Math.min(...PARTS.map(p => p.from[k])), Math.max(...PARTS.map(p => p.to[k]))].map(r4);
  console.log('wrote the Rattan Shield:', PARTS.length, 'cubes; x', span(0), 'y', span(1), 'z', span(2));
}
