// Original Sanguine Art textures. Dependency-free PNG writer; run with Node 20+.
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock/textures');
function crc(b) { let c = -1; for (const n of b) { c ^= n; for(let i=0;i<8;i++) c = (c>>>1) ^ ((c&1)?0xedb88320:0); } return (c^-1)>>>0; }
function chunk(type,b) { const t=Buffer.from(type), head=Buffer.alloc(4), tail=Buffer.alloc(4); head.writeUInt32BE(b.length);tail.writeUInt32BE(crc(Buffer.concat([t,b])));return Buffer.concat([head,t,b,tail]); }
function png(file,size,pixel) {
  const data=Buffer.alloc(size*(size*4+1));
  for(let y=0;y<size;y++)for(let x=0;x<size;x++){const p=pixel(x,y);for(let i=0;i<4;i++)data[y*(size*4+1)+1+x*4+i]=p[i];}
  const h=Buffer.alloc(13);h.writeUInt32BE(size,0);h.writeUInt32BE(size,4);h[8]=8;h[9]=6;
  const out=path.join(root,file);fs.mkdirSync(path.dirname(out),{recursive:true});
  fs.writeFileSync(out,Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',h),chunk('IDAT',zlib.deflateSync(data)),chunk('IEND',Buffer.alloc(0))]));
}
png('particle/sanguine_spark.png',32,(x,y)=>{const d=Math.hypot(x-15.5,y-15.5)/15.5;return [255,255,255,Math.round(255*Math.pow(Math.max(0,1-d),1.6))];});
for(const [id,kind] of [['bloodbane_drain',1],['scarlet_tear',2],['devourer_of_worlds',3],['crimson_stance',4],['crystal_barrage',5]])
  png(`gui/technique/${id}.png`,16,(x,y)=>{
    const dx=x-7.5,dy=y-7.5,r=Math.hypot(dx,dy),a=Math.atan2(dy,dx);
    const crystal=(x,y)=>Math.abs(x)/2.4+Math.abs(y)/6<1;
    const mark=kind===1?crystal(dx,dy)
      :kind===2?Math.abs(Math.abs(dx)-Math.abs(dy))<1.2&&r<7
      :kind===3?(Math.abs(dx)<4&&Math.abs(dy)<4&&Math.abs(dx)>2)||crystal(dx,dy)
      :kind===5?[-4,0,4].some(k=>Math.abs(dx-k)/1.6+Math.abs(dy)/5<1)
      :r>3&&r<5||Math.abs(dx)<2&&Math.abs(dy)<3;
    return mark?[132+Math.round(r*3),14+Math.round(r*2),22+Math.round(r*2),255]:[29,12,23,r<7.6?255:0];
  });
png('item/sanguine_art_scroll.png',16,(x,y)=>{
  if(x<3||x>12||y<1||y>14)return[0,0,0,0];
  if(y===1||y===14||x===3||x===12)return[103,31,42,255];
  if((y===5||y===8||y===11)&&x>5&&x<10)return[151,28,50,255];
  return[237,208,158,255];
});
