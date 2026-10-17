// Export Enzo's unchanged guardian pieces to the game's Epic Fight mesh and one combined atlas.
const fs = require('fs'), path = require('path');
const kit = require('./mesh-kit.cjs');
const source = require('./scarlet-guardian-3d.cjs');
const pieces = [];
for (const [list, look] of [[source.BODY_PIECES, source.BODY], [source.ARMOUR_PIECES, source.ARMOUR]]) {
  for (const original of list) {
    const index = pieces.length;
    pieces.push({...original, uv: undefined, paint: (f,u,v,w,h) => {
      const c = kit.colour(original.paint(f,u,v,w,h,look),look,f,u,v,index);
      return c ? '#' + c.slice(0,3).map(n => n.toString(16).padStart(2,'0')).join('') : null;
    }});
  }
}
const size = 1024, faces = kit.pack(pieces,size);
const root = path.join(__dirname,'../../src/main/resources/assets/murimblock');
fs.writeFileSync(path.join(root,'textures/entity/scarlet_guardian.png'),kit.png(size,size,kit.paintAtlas(faces,{c:{}},size)));
fs.writeFileSync(path.join(root,'animmodels/entity/scarlet_guardian.json'),JSON.stringify(kit.mesh(pieces,size)));
// Same authored movements, separate accessors/colliders for the guardian's size; player timings are untouched.
for (const [name, src] of [['guardian_straight','sanguine_straight'],['guardian_cross','sanguine_cross']])
  fs.copyFileSync(path.join(root,`animmodels/animations/biped/murim/${src}.json`),path.join(root,`animmodels/animations/biped/murim/${name}.json`));
console.log(`Guardian: ${pieces.length} original pieces; 1024px atlas.`);
