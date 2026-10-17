// Writes the locator pendants' item models from the sect token's compass overrides: for each pendant, 32 frame models
// (<name>_pendant_NN, its own drawing: scripts/worldgen/PendantTextures.java) and its item model, whose "angle"
// overrides pick the frames.
//   node scripts/worldgen/pendant-models.cjs
const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, '../../src/main/resources/assets/murimblock/models/item');
// Only the structures that matter: those with a boss or with quest givers (Enzo, 15/10).
const PENDANTS = ['misty_bamboo_temple', 'yellow_river_bandit_den', 'alliance_arena'];

const token = JSON.parse(fs.readFileSync(path.join(dir, 'sect_token.json'), 'utf8'));
for (const p of PENDANTS) {
  const name = `${p}_pendant`;
  for (let n = 0; n < 32; n++) {
    if (n === 16) continue; // frame 16 is the item model itself, as for the sect token
    const frame = String(n).padStart(2, '0');
    fs.writeFileSync(path.join(dir, `${name}_${frame}.json`), JSON.stringify({
      parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/${name}_${frame}` },
    }, null, 2) + '\n');
  }
  const overrides = token.overrides.map(o => ({
    predicate: o.predicate,
    model: o.model === 'murimblock:item/sect_token' ? `murimblock:item/${name}`
      : o.model.replace('murimblock:item/sect_token_', `murimblock:item/${name}_`),
  }));
  fs.writeFileSync(path.join(dir, `${name}.json`), JSON.stringify({
    parent: 'minecraft:item/generated', textures: { layer0: `murimblock:item/${name}_16` }, overrides,
  }, null, 2) + '\n');
}
console.log(`${PENDANTS.length} pendants x 32 frames -> ${dir}`);
