// Sanguine Art: character motion only. In-game Photon effects live in SanguineEffects.java.
// Epic Fight BIPED, shared with the project's existing player animation pipeline.
// node scripts/animation/sanguine-art.cjs [output.bbmodel]
// EPICFIGHT_BIPED may point to the local pinned armature when Gradle's cache is elsewhere.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const rig = require('./blockbench-rig.cjs');
const ik = require('./ik.cjs');

const HIP = rig.ROOT_BIND[2];
const FEET = { footR: { at: [0.24, -0.16, 0.01], w: 20 }, footL: { at: [-0.24, 0.24, 0.01], w: 20 } };
const LEG_VARS = [
  ['Thigh_R', 0, -55, 100], ['Thigh_R', 1, -45, 45], ['Leg_R', 0, -130, -2],
  ['Thigh_L', 0, -55, 100], ['Thigh_L', 1, -45, 45], ['Leg_L', 0, -130, -2],
];

// Pose axes: x right, y forward, z up. The arms act from the shoulders, the elbows
// bend at Hand_R/L. Root stays facing forward: the torso provides the attack twist.
function ready({x=0, y=0, h=.65, turn=-14, lean=-5}={}) {
  return {
    Root: { pos:[x,y,h], rot:[] },
    Torso:[['x',lean],['z',turn]], Chest:[['x',-3],['z',turn*.25]],
    Head:[['x',-lean+3],['z',-turn*1.25]],
    Shoulder_R:[['z',-2]], Shoulder_L:[['z',2]],
    Arm_R:[['x',36],['y',-12],['z',6]], Hand_R:[['x',100]], Tool_R:[['z',-5]],
    Arm_L:[['x',62],['y',14],['z',-10]], Hand_L:[['x',88]], Tool_L:[['z',5]],
    Thigh_R:[['x',15],['y',-11]], Leg_R:[['x',-50]],
    Thigh_L:[['x',43],['y',10]], Leg_L:[['x',-46]],
  };
}
const G = ready();
const pose = (options, limbs={}) => ({...ready(options), ...limbs});
const K = (t,p) => ({t,pose:p});

const guard = [
  K(0,G), K(.4,pose({h:.642,turn:-15,lean:-6},{Hand_R:[['x',102]],Hand_L:[['x',90]]})),
  K(.8,pose({h:.652,turn:-13,lean:-4},{Arm_L:[['x',63],['y',14],['z',-10]]})),
  K(1.2,pose({h:.646,turn:-14,lean:-5})), K(1.6,G),
];

// Three distinct body actions: right straight, left cross, rising right strike.
const combo = [
  K(0,G),
  K(.10,pose({x:-.025,y:-.025,h:.625,turn:24,lean:-7},{
    Arm_R:[['x',-18],['y',-16],['z',8]],Hand_R:[['x',122]],
    Arm_L:[['x',66],['y',12],['z',-6]],Hand_L:[['x',84]],Shoulder_R:[['z',-5]]})),
  K(.18,pose({x:.035,y:.09,h:.62,turn:-18,lean:-12},{
    Arm_R:[['x',91],['y',-5],['z',20]],Hand_R:[['x',8]],Tool_R:[['z',-12]],
    Arm_L:[['x',40],['y',18],['z',-8]],Hand_L:[['x',108]]})),
  K(.23,pose({x:.04,y:.11,h:.618,turn:-23,lean:-14},{
    Arm_R:[['x',95],['y',-4],['z',27]],Hand_R:[['x',6]],
    Arm_L:[['x',36],['y',18]],Hand_L:[['x',110]]})),
  K(.32,pose({h:.638,turn:-9},{Arm_R:[['x',34],['y',-10]],Hand_R:[['x',103]]})),
  K(.39,pose({x:.025,y:-.025,h:.625,turn:-29,lean:-7},{
    Arm_L:[['x',-12],['y',17],['z',-10]],Hand_L:[['x',122]],
    Arm_R:[['x',58],['y',-12]],Hand_R:[['x',90]],Shoulder_L:[['z',5]]})),
  K(.48,pose({x:-.04,y:.075,h:.616,turn:20,lean:-12},{
    Arm_L:[['x',93],['y',4],['z',-24]],Hand_L:[['x',8]],Tool_L:[['z',12]],
    Arm_R:[['x',38],['y',-16]],Hand_R:[['x',111]]})),
  K(.54,pose({x:-.045,y:.1,h:.618,turn:26,lean:-14},{
    Arm_L:[['x',97],['y',5],['z',-31]],Hand_L:[['x',6]],
    Arm_R:[['x',34],['y',-16]],Hand_R:[['x',113]]})),
  K(.66,pose({h:.62,turn:6},{Arm_R:[['x',32],['y',-10]],Hand_R:[['x',106]]})),
  K(.78,pose({x:-.03,y:-.045,h:.555,turn:23,lean:-17},{
    Arm_R:[['x',-30],['y',-15],['z',6]],Hand_R:[['x',112]],
    Arm_L:[['x',68],['y',14]],Hand_L:[['x',92]]})),
  K(.92,pose({x:.035,y:.1,h:.67,turn:-17,lean:6},{
    Arm_R:[['x',99],['y',-8],['z',24]],Hand_R:[['x',51]],Tool_R:[['z',-18]],
    Arm_L:[['x',43],['y',20]],Hand_L:[['x',105]],Chest:[['x',6],['z',-5]]})),
  K(1.02,pose({x:.025,y:.085,h:.677,turn:-24,lean:8},{
    Arm_R:[['x',122],['y',-10],['z',29]],Hand_R:[['x',30]],
    Arm_L:[['x',42],['y',18]],Hand_L:[['x',110]],Chest:[['x',7],['z',-6]]})),
  K(1.13,pose({h:.65,turn:-18,lean:0},{Arm_R:[['x',74],['y',-12]],Hand_R:[['x',70]]})),
  K(1.32,pose({h:.642,turn:-15})), K(1.52,G),
];

// The right hand rises above the head and the crystal grows there, held like a spear (15/10/2026, Enzo's request);
// the left hand stays up in front. The body leans back a little under its weight.
const C = pose({h:.64,turn:10,lean:3},{Arm_R:[['x',168],['y',-8],['z',10]],Hand_R:[['x',14]],Tool_R:[['x',-10]],
 Shoulder_R:[['z',-6]],Arm_L:[['x',52],['y',16]],Hand_L:[['x',96]],Chest:[['x',4]]});
const charge=[K(0,C),K(.75,{...C,Arm_R:[['x',172],['y',-8],['z',10]],Chest:[['x',5],['z',2]]}),K(1.5,C)];
// The throw: the arm draws back behind the head, then whips forward over it and follows through low; the crystal
// leaves at 0.30 s (SanguineArt launches it six ticks after the throw starts).
const throwing=[K(0,C),
 K(.12,pose({y:-.04,h:.63,turn:24,lean:7},{Arm_R:[['x',200],['y',-12],['z',14]],Hand_R:[['x',48]],Tool_R:[['x',-25]],
  Shoulder_R:[['z',-8]],Arm_L:[['x',60],['y',18]],Hand_L:[['x',90]],Chest:[['x',6]]})),
 K(.30,pose({y:.10,h:.615,turn:-22,lean:-15},{Arm_R:[['x',112],['y',-6],['z',18]],Hand_R:[['x',6]],Tool_R:[['x',-12]],
  Arm_L:[['x',30],['y',20]],Hand_L:[['x',104]]})),
 K(.42,pose({y:.11,h:.61,turn:-28,lean:-16},{Arm_R:[['x',72],['y',-4],['z',24]],Hand_R:[['x',8]]})),
 K(.72,pose({h:.635,turn:-18})),K(1.05,G)];

// Scarlet Tear (15/10/2026, Enzo's request): the blood sword forms piece by piece in the open right hand, held low
// before the body; the sword rises to the right shoulder, the torso coiled; a great level arc from right to left
// with a step in; then the arm draws back behind the head and throws the sword (it leaves at 1.24 s).
const tear = [
  K(0,G),
  // Forming: the right forearm rises upright beside the body, the fist up, and the sword grows from it toward the
  // sky, piece by piece; the eyes on it, the left hand in guard.
  K(.10,pose({h:.63,turn:12,lean:-4},{Arm_R:[['x',62],['y',-46],['z',6]],Hand_R:[['x',96]],Tool_R:[['x',-6]],
    Arm_L:[['x',60],['y',16],['z',-10]],Hand_L:[['x',90]],Head:[['x',2],['z',-14]]})),
  K(.50,pose({h:.628,turn:16,lean:-3},{Arm_R:[['x',70],['y',-50],['z',6]],Hand_R:[['x',100]],Tool_R:[['x',-4]],
    Arm_L:[['x',60],['y',16],['z',-10]],Hand_L:[['x',90]],Chest:[['x',-2]],Head:[['x',-4],['z',-16]]})),
  // Coiled: the sword up by the right shoulder, the torso turned away, weight back.
  K(.68,pose({x:-.02,y:-.035,h:.612,turn:40,lean:-5},{Arm_R:[['x',104],['y',-80],['z',22]],Hand_R:[['x',34]],Tool_R:[['x',-8]],
    Shoulder_R:[['z',-7]],Arm_L:[['x',72],['y',32],['z',-8]],Hand_L:[['x',78]],Chest:[['x',2],['z',8]]})),
  // The arc: level, right to left, the arm straight, a step in and down.
  K(.77,pose({y:.04,h:.6,turn:6,lean:-12},{Arm_R:[['x',96],['y',-22],['z',14]],Hand_R:[['x',12]],Tool_R:[['x',-4]],
    Arm_L:[['x',42],['y',22]],Hand_L:[['x',100]],Chest:[['x',3],['z',-2]]})),
  K(.86,pose({y:.095,h:.59,turn:-36,lean:-16},{Arm_R:[['x',92],['y',36],['z',10]],Hand_R:[['x',10]],Tool_R:[['x',-4]],
    Arm_L:[['x',30],['y',26]],Hand_L:[['x',106]],Chest:[['x',4],['z',-8]]})),
  K(.97,pose({y:.095,h:.598,turn:-42,lean:-13},{Arm_R:[['x',78],['y',54],['z',4]],Hand_R:[['x',30]],
    Arm_L:[['x',34],['y',26]],Hand_L:[['x',104]],Chest:[['x',3],['z',-8]]})),
  // The javelin throw. Withdrawal: the body turns side-on, the right arm reaches straight back at shoulder height
  // carrying the sword over the shoulder point forward, the left arm points at the target, the weight goes back.
  K(1.06,pose({y:-.05,h:.615,turn:48,lean:10},{Arm_R:[['x',-72],['y',-14],['z',8]],Hand_R:[['x',6]],Tool_R:[['x',-10]],
    Shoulder_R:[['z',-4]],Arm_L:[['x',92],['y',8],['z',-4]],Hand_L:[['x',12]],Chest:[['x',-4],['z',10]],Head:[['x',-6],['z',-30]]})),
  // The block: still further back, the front foot plants.
  K(1.18,pose({y:-.02,h:.6,turn:52,lean:15},{Arm_R:[['x',-80],['y',-12],['z',6]],Hand_R:[['x',4]],Tool_R:[['x',-10]],
    Shoulder_R:[['z',-6]],Arm_L:[['x',96],['y',6],['z',-4]],Hand_L:[['x',8]],Chest:[['x',-6],['z',12]],Head:[['x',-8],['z',-34]]})),
  // The whip: hips and torso turn first, the elbow comes up high over the shoulder, the hand still behind.
  K(1.28,pose({y:.05,h:.62,turn:8,lean:2},{Arm_R:[['x',152],['y',-10],['z',20]],Hand_R:[['x',72]],Tool_R:[['x',-14]],
    Shoulder_R:[['z',-8]],Arm_L:[['x',58],['y',22],['z',-6]],Hand_L:[['x',70]],Chest:[['x',4],['z',-4]],Head:[['x',-2],['z',-6]]})),
  // The release: the arm unfolds high and forward, the left arm pulls down to the hip; the sword leaves point first.
  K(1.35,pose({y:.12,h:.612,turn:-30,lean:-14},{Arm_R:[['x',124],['y',-4],['z',16]],Hand_R:[['x',4]],Tool_R:[['x',-8]],
    Arm_L:[['x',18],['y',30]],Hand_L:[['x',96]],Chest:[['x',6],['z',-10]]})),
  // The follow-through: the body falls forward over the front leg, the arm sweeps down across it.
  K(1.50,pose({y:.16,h:.575,turn:-42,lean:-24},{Arm_R:[['x',62],['y',34],['z',8]],Hand_R:[['x',18]],
    Arm_L:[['x',22],['y',32]],Hand_L:[['x',100]],Chest:[['x',8],['z',-12]]})),
  K(1.72,pose({y:.08,h:.615,turn:-24,lean:-10},{Arm_R:[['x',44],['y',-4]],Hand_R:[['x',70]]})), K(1.95,G),
];

// Crystallization: brace across the chest, open the shoulders, then settle in guard.
const form=[K(0,G),K(.25,pose({h:.58,turn:0,lean:-12},{Arm_R:[['x',78],['z',-35]],Hand_R:[['x',83]],
 Arm_L:[['x',78],['z',35]],Hand_L:[['x',83]]})),
 K(.55,pose({h:.64,turn:0,lean:5},{Arm_R:[['x',38],['y',-55]],Hand_R:[['x',24]],Arm_L:[['x',38],['y',55]],Hand_L:[['x',24]],Chest:[['x',8]]})),
 K(.9,pose({h:.65,turn:-5,lean:0},{Arm_R:[['x',42],['y',-35]],Hand_R:[['x',56]],Arm_L:[['x',46],['y',35]],Hand_L:[['x',55]]})),K(1.4,G)];
// Command the dorsal fan with both palms. The final shot leaves at 1.2 seconds.
const barrage=[K(0,G),K(.16,pose({y:-.03,h:.59,turn:0,lean:-10},{Arm_R:[['x',12],['y',-26]],Hand_R:[['x',112]],
 Arm_L:[['x',12],['y',26]],Hand_L:[['x',112]]})),
 K(.30,pose({y:.07,h:.63,turn:0,lean:-12},{Arm_R:[['x',88],['y',-12]],Hand_R:[['x',12]],Tool_R:[['x',-20]],
 Arm_L:[['x',88],['y',12]],Hand_L:[['x',12]],Tool_L:[['x',-20]]})),
 K(.8,pose({y:.05,h:.625,turn:0,lean:-8},{Arm_R:[['x',85],['y',-12]],Hand_R:[['x',18]],Arm_L:[['x',85],['y',12]],Hand_L:[['x',18]]})),
 K(1.22,pose({y:.055,h:.63,turn:0,lean:-10},{Arm_R:[['x',87],['y',-12]],Hand_R:[['x',14]],Arm_L:[['x',87],['y',12]],Hand_L:[['x',14]]})),K(1.65,G)];
const CLIPS={sanguine_guard:guard,sanguine_combo:combo,sanguine_crystal_charge:charge,
 sanguine_crystal_throw:throwing,sanguine_scarlet_tear:tear,sanguine_crystal_form:form,sanguine_crystal_barrage:barrage};

const AXES = ['x','y','z'];
const NAMES = [...rig.BONES,'Tool_L'];
function normalized(p) {
  const r={Root:{pos:[...(p.Root?.pos||[0,0,HIP])],rot:[]}};
  for(const name of NAMES.filter(n=>n!=='Root')) {
    const angles=Object.fromEntries(p[name]||[]);
    r[name]=AXES.map(a=>[a,angles[a]||0]);
  }
  return r;
}
function interpolate(a,b,f) {
  const p={Root:{pos:a.Root.pos.map((x,n)=>x+(b.Root.pos[n]-x)*f),rot:[]}};
  for(const n of NAMES.filter(n=>n!=='Root'))p[n]=a[n].map(([axis,v],i)=>[axis,v+(b[n][i][1]-v)*f]);
  return p;
}

// Bake gentle easing to ordinary linear keys: the existing Epic Fight converter
// samples linear Blockbench keys, so previews and exports follow the same motion.
function bake(keys) {
  const normalizedKeys=keys.map(k=>({...k,pose:normalized(k.pose)}));
  const end=keys.at(-1).t;
  const times=new Set(keys.map(k=>k.t));
  for(let t=0;t<end;t+=1/40)times.add(Math.round(t*10000)/10000);
  times.add(end);
  return [...times].sort((a,b)=>a-b).map(t=>{
    let i=0;while(i<keys.length-2 && t>keys[i+1].t)i++;
    let f=Math.max(0,Math.min(1,(t-keys[i].t)/(keys[i+1].t-keys[i].t)));
    f=f*f*(3-2*f);
    const p=interpolate(normalizedKeys[i].pose,normalizedKeys[i+1].pose,f);
    const legs=ik.solve(p,LEG_VARS,FEET,180,0).pose;
    return K(t,legs);
  });
}

function palettePng() {
  const colors=[[200,156,140],[22,19,28],[45,25,36],[25,19,30],[116,12,23],
    [23,23,35],[36,24,34],[147,16,23],[188,74,56],[152,18,24],[72,9,15]];
  const W=16,H=16,raw=Buffer.alloc((W*4+1)*H);
  colors.forEach((c,i)=>c.concat(255).forEach((v,k)=>raw[1+i*4+k]=v));
  const table=Array.from({length:256},(_,n)=>{let c=n;for(let k=0;k<8;k++)c=c&1?0xedb88320^(c>>>1):c>>>1;return c>>>0;});
  const chunk=(type,data)=>{const td=Buffer.concat([Buffer.from(type),data]);let c=0xffffffff;
    for(const x of td)c=table[(c^x)&255]^(c>>>8);
    const len=Buffer.alloc(4),crc=Buffer.alloc(4);len.writeUInt32BE(data.length);crc.writeUInt32BE((c^0xffffffff)>>>0);
    return Buffer.concat([len,td,crc]);};
  const ih=Buffer.alloc(13);ih.writeUInt32BE(W,0);ih.writeUInt32BE(H,4);ih[8]=8;ih[9]=6;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ih),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
}
function addCube(p,g,name,from,to,color) {
  const uv=[color,0,color+1,1],id=rig.uuid();
  p.elements.push({name,type:'cube',uuid:id,from,to,origin:from.map((v,i)=>(v+to[i])/2),box_uv:false,
    autouv:0,faces:Object.fromEntries(['north','east','south','west','up','down'].map(f=>[f,{uv:[...uv],texture:0}]))});
  g.children.push(id);
}
function bodyOnly(p) {
  const g=rig.groupsByName(p.outliner),swordIds=new Set(g.Tool_R.children.filter(c=>typeof c==='string'));
  g.Tool_R.children=g.Tool_R.children.filter(c=>typeof c!=='string');
  p.elements=p.elements.filter(e=>!swordIds.has(e.uuid));
  const left={...g.Tool_R,name:'Tool_L',origin:rig.at('Tool_L'),uuid:rig.uuid(),children:[],isOpen:false};
  g.Hand_L.children.push(left);g.Tool_L=left;
  for(const side of ['R','L']) {
    const hand=p.elements.find(e=>e.name==='hand_'+side.toLowerCase());
    g['Hand_'+side].children=g['Hand_'+side].children.filter(c=>c!==hand.uuid);
    g['Tool_'+side].children.push(hand.uuid);
    const [x,y]=g['Hand_'+side].origin;
    addCube(p,g['Hand_'+side],'crimson_wrap_'+side,[x-2.06,y-4.2,-2.06],[x+2.06,y-3.2,2.06],4);
  }
  const [x,y,z]=g.Head.origin;
  addCube(p,g.Head,'face_mask',[x-3.75,y+.5,z-4.09],[x+3.75,y+2.5,z-3.99],1);
  for(const dx of [-1.8,1.8])addCube(p,g.Head,'crimson_eye',[x+dx-.55,y+3.75,z-4.09],[x+dx+.55,y+4.25,z-3.99],7);
  p.textures[0].name='sanguine_palette.png';p.textures[0].source='data:image/png;base64,'+palettePng().toString('base64');
  return {p,g};
}

function build() {
  const baked=Object.fromEntries(Object.entries(CLIPS).map(([n,k])=>[n,bake(k)]));
  const showcase=[];
  for(const [name,offset] of [['sanguine_combo',0],['sanguine_crystal_charge',1.8],['sanguine_crystal_throw',3.4],['sanguine_scarlet_tear',4.7],['sanguine_crystal_form',6.3],['sanguine_crystal_barrage',7.9]])
    showcase.push(...baked[name].map(k=>K(Math.round((k.t+offset)*10000)/10000,k.pose)));
  showcase.push(K(10.0,baked.sanguine_guard[0].pose));baked.sanguine_showcase=showcase;
  const {p,g}=bodyOnly(rig.project('sanguine_art',baked));
  for(const a of p.animations) {
    const keys=baked[a.name];
    a.snapping=40;if(['sanguine_guard','sanguine_showcase','sanguine_crystal_charge'].includes(a.name))a.loop='loop';
    a.animators[g.Tool_L.uuid]={name:'Tool_L',type:'bone',keyframes:keys.map(k=>{
      const v=rig.toBlockbenchEuler(rig.rotationOf(k.pose.Tool_L));
      return {uuid:rig.uuid(),channel:'rotation',time:k.t,interpolation:'linear',color:-1,
        data_points:[{x:v[0],y:v[1],z:v[2]}]};})};
    a.markers=({sanguine_combo:[.18,.48,.92],sanguine_crystal_throw:[.30],
      sanguine_scarlet_tear:[.77,.86,1.35],sanguine_crystal_form:[.55],sanguine_crystal_barrage:[.30,1.22]}[a.name]||[])
      .map(time=>({time,color:5}));
  }
  p.animations[0].selected=true;
  p.editor_state={mode:'animate',tool:'move_tool',selected_elements:[],selected_groups:[],
    selected_texture:p.textures[0].uuid,previews:{}};
  p.sanguine_notes={revision:3,target:'Murimblock / Epic Fight 21.17.3.1 / Minecraft 1.21.1',
    particles:'Photon 2.2.8, in game. No particle geometry in this model.',
    scope:'Character-only authoring project. Playable clips exported with export-sanguine.cjs.',
    foot_targets:FEET,body_rig:'Shared original Murimblock BIPED preview rig, articulated wrists.'};
  return {model:p,baked};
}

if(require.main===module) {
  const out=process.argv[2]||path.join(__dirname,'../../art/blockbench/sanguine_art.bbmodel');
  const {model,baked}=build();
  fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify(model,null,1));
  console.log('Wrote',out,model.elements.length,'cubes;',model.animations.length,'body-only animations.');
  for(const [name,keys] of Object.entries(baked)) {
    let maxFootError=0;
    for(const k of keys) {
      const m=ik.measure(k.pose);
      for(const foot of ['footR','footL'])maxFootError=Math.max(maxFootError,Math.hypot(...m[foot].map((v,i)=>v-FEET[foot].at[i])));
    }
    console.log(name,keys.length,'poses,',keys.at(-1).t,'s, max planted-foot error',maxFootError.toFixed(4),'blocks');
  }
}
module.exports={build,CLIPS,FEET,palettePng};
