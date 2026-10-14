// Apply the current character clips to the approved body model without changing its cubes or texture.
const fs=require('node:fs'),path=require('node:path');
const root=path.join(__dirname,'../../art/blockbench');
const body=JSON.parse(fs.readFileSync(path.join(root,'blood_crystal_body.bbmodel'),'utf8'));
const actor=JSON.parse(fs.readFileSync(path.join(root,'sanguine_art.bbmodel'),'utf8'));
function groups(nodes,map=new Map()) {
  for(const node of nodes)if(typeof node==='object'){map.set(node.name,node);groups(node.children,map);}return map;
}
const target=groups(body.outliner),source=groups(actor.outliner);
for(const [name,group] of source) {
  const dest=target.get(name);if(!dest)throw new Error('Missing joint '+name);
  if(Math.hypot(...group.origin.map((v,i)=>v-dest.origin[i]))>1e-5)throw new Error('Different joint origin '+name);
}
body.name='blood_crystal_animated';body.animations=structuredClone(actor.animations);
for(const clip of body.animations) {
  const tracks={};
  for(const track of Object.values(clip.animators))tracks[target.get(track.name).uuid]=track;
  clip.animators=tracks;
}
body.sanguine_notes={revision:3,preview:'Approved blood crystal geometry with current Sanguine character clips. Floating fan shows maximum stacks; its game count is earned by landed M1 strikes.'};
fs.writeFileSync(path.join(root,'blood_crystal_animated.bbmodel'),JSON.stringify(body,null,1)+'\n');
