// Derive a straight, centered projectile from the approved fan; keep its facets and UVs.
const fs=require('node:fs'),path=require('node:path');
const root=path.join(__dirname,'../../src/main/resources/assets/murimblock');
const model=JSON.parse(fs.readFileSync(path.join(root,'geo/blood_crystal_floating.geo.json'),'utf8'));
const geometry=model['minecraft:geometry'][0];
const bone=structuredClone(geometry.bones.find(b=>b.name==='float_crystal_1'));
const pivot=[...bone.pivot],tilt=bone.cubes[0].rotation[2];
bone.name='projectile';delete bone.parent;delete bone.rotation;bone.pivot=[0,0,0];
for(const cube of bone.cubes) {
  cube.origin=cube.origin.map((v,i)=>v-pivot[i]);
  if(cube.pivot)cube.pivot=cube.pivot.map((v,i)=>v-pivot[i]);
  cube.rotation[2]-=tilt;
}
geometry.bones=[bone];Object.assign(geometry.description,{identifier:'geometry.blood_crystal_projectile',visible_bounds_width:2,visible_bounds_height:2,visible_bounds_offset:[0,0,0]});
fs.writeFileSync(path.join(root,'geo/blood_crystal_projectile.geo.json'),JSON.stringify(model,null,2)+'\n');
