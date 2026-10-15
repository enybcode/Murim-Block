// Files of the Murim building blocks (MurimBlocks, "building"): the Orthodox Alliance's azure glazed roof tiles, as a
// block, stairs and a slab. Writes their texture (16 x 16, drawn here), block states, block and item models, loot
// tables, the stairs/slabs tags and their English names. Their mineable tag is written by generate-nature.cjs.
// Usage: node scripts/worldgen/generate-building.cjs
const fs = require('fs');
const path = require('path');
const { png } = require('../animation/mesh-kit.cjs');
const { AZURE_TILES } = require('./building-blocks.cjs');

const ROOT = path.join(__dirname, '..', '..');
const NS = 'murimblock';
const A = (...p) => path.join(ROOT, 'src', 'main', 'resources', 'assets', NS, ...p);
const D = (ns, ...p) => path.join(ROOT, 'src', 'main', 'resources', 'data', ns, ...p);
const write = (file, json) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, JSON.stringify(json, null, 2) + '\n'); };
const id = n => `${NS}:${n}`;
const [BLOCK, STAIRS, SLAB] = AZURE_TILES;
const TEX = `${NS}:block/${BLOCK}`;

// ---------------------------------------------------------------- the texture
// Glazed tiles in courses of four rows, each course offset by half a tile: the tile's lit upper lip, its body, a shade
// toward its foot, and a dark joint under it; a glint of glaze on every other tile.
function texture() {
  const hex = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
  const LIP = hex('#6E9BDA'), BODY = hex('#3F6FB4'), SHADE = hex('#335E9C'), JOINT = hex('#1F3D6B'), GLINT = hex('#A9C8F0');
  const rgba = new Uint8Array(16 * 16 * 4);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const course = Math.floor(y / 4), row = y % 4, shift = course % 2 ? 4 : 0;
    const u = (x + shift) % 8; // across one tile (8 wide)
    let c = row === 0 ? LIP : row === 3 ? JOINT : row === 2 ? SHADE : BODY;
    if (u === 7 && row < 3) c = JOINT;                           // the gap between two tiles
    if (row === 1 && u === 1 && ((Math.floor((x + shift) / 8) + course) % 2 === 0)) c = GLINT;
    rgba.set([...c, 255], (y * 16 + x) * 4);
  }
  return png(16, 16, rgba);
}

// ---------------------------------------------------------------- block states and models
/** Vanilla's stairs block state (minecraft:blockstates/stone_stairs.json, copied as vanilla-stairs.json), on our models. */
const stairsState = () => {
  const vanilla = JSON.parse(fs.readFileSync(path.join(__dirname, 'vanilla-stairs.json'), 'utf8'));
  const variants = {};
  for (const [key, v] of Object.entries(vanilla.variants)) variants[key] = { ...v, model: v.model.replace('minecraft:block/stone_stairs', id('block/' + STAIRS)) };
  return { variants };
};

function main() {
  write(A('blockstates', `${BLOCK}.json`), { variants: { '': { model: id(`block/${BLOCK}`) } } });
  write(A('blockstates', `${STAIRS}.json`), stairsState());
  write(A('blockstates', `${SLAB}.json`), { variants: {
    'type=bottom': { model: id(`block/${SLAB}`) }, 'type=top': { model: id(`block/${SLAB}_top`) }, 'type=double': { model: id(`block/${BLOCK}`) } } });
  const tex3 = { bottom: TEX, top: TEX, side: TEX };
  write(A('models', 'block', `${BLOCK}.json`), { parent: 'minecraft:block/cube_all', textures: { all: TEX } });
  write(A('models', 'block', `${STAIRS}.json`), { parent: 'minecraft:block/stairs', textures: tex3 });
  write(A('models', 'block', `${STAIRS}_inner.json`), { parent: 'minecraft:block/inner_stairs', textures: tex3 });
  write(A('models', 'block', `${STAIRS}_outer.json`), { parent: 'minecraft:block/outer_stairs', textures: tex3 });
  write(A('models', 'block', `${SLAB}.json`), { parent: 'minecraft:block/slab', textures: tex3 });
  write(A('models', 'block', `${SLAB}_top.json`), { parent: 'minecraft:block/slab_top', textures: tex3 });
  for (const b of AZURE_TILES) write(A('models', 'item', `${b}.json`), { parent: id(`block/${b}`) });
  fs.mkdirSync(A('textures', 'block'), { recursive: true });
  fs.writeFileSync(A('textures', 'block', `${BLOCK}.png`), texture());

  // Loot: each drops itself; a double slab two.
  const self = (name, functions) => ({ type: 'minecraft:block', pools: [{ rolls: 1, bonus_rolls: 0,
    entries: [{ type: 'minecraft:item', name: id(name), ...(functions ? { functions } : {}) }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
  write(D(NS, 'loot_table', 'blocks', `${BLOCK}.json`), self(BLOCK));
  write(D(NS, 'loot_table', 'blocks', `${STAIRS}.json`), self(STAIRS));
  write(D(NS, 'loot_table', 'blocks', `${SLAB}.json`), self(SLAB, [{ function: 'minecraft:set_count', count: 2, add: false,
    conditions: [{ condition: 'minecraft:block_state_property', block: id(SLAB), properties: { type: 'double' } }] }, { function: 'minecraft:explosion_decay' }]));

  // Tags: stairs and slabs (blocks and items).
  for (const registry of ['block', 'item']) {
    write(D('minecraft', 'tags', registry, 'stairs.json'), { replace: false, values: [id(STAIRS)] });
    write(D('minecraft', 'tags', registry, 'slabs.json'), { replace: false, values: [id(SLAB)] });
  }

  // English names.
  const langFile = A('lang', 'en_us.json');
  const lang = JSON.parse(fs.readFileSync(langFile, 'utf8'));
  Object.assign(lang, { [`block.${NS}.${BLOCK}`]: 'Azure Roof Tiles', [`block.${NS}.${STAIRS}`]: 'Azure Roof Tile Stairs',
    [`block.${NS}.${SLAB}`]: 'Azure Roof Tile Slab' });
  write(langFile, lang);
  console.log('wrote the azure roof tiles:', AZURE_TILES.join(', '));
}

main();
