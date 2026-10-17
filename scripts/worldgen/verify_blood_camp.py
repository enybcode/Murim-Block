"""Independent read-back of the delivered asset, including access and block attachments."""
from pathlib import Path
from collections import deque, Counter
import hashlib, json, re, sys
import numpy as np
import argparse
import nbtlib
ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--block-report', type=Path, required=True, help='Minecraft 1.21.1 generated reports/blocks.json')
parser.add_argument('--asset-dir', type=Path, default=ROOT / 'art/structures/blood-camp')
args=parser.parse_args()
OUT=args.asset_dir
meta=json.loads((OUT/'export-stats.json').read_text(encoding='utf-8'))
path=OUT/meta['file'];s=nbtlib.load(path)['Schematic']
assert int(s['Version'])==3 and int(s['DataVersion'])==3955
W,H,D=map(int,(s['Width'],s['Height'],s['Length']))
assert [W,H,D]==meta['size'] and list(map(int,s['Offset']))==meta['offset']
assert hashlib.sha256(path.read_bytes()).hexdigest()==meta['sha256']
assert not s['Entities']
palette={int(v):str(k) for k,v in s['Blocks']['Palette'].items()}
assert list(sorted(palette))==list(range(len(palette))) and palette[0]=='minecraft:air'
values=[];v=shift=0
for b in bytes(s['Blocks']['Data']):
    v|=(b&127)<<shift
    if b&128:shift+=7;assert shift<35
    else:assert v in palette;values.append(v);v=shift=0
assert shift==0 and len(values)==W*H*D
grid=np.array(values).reshape(H,D,W)
assert np.count_nonzero(grid)==meta['non_air']
reg=json.loads(args.block_report.read_text(encoding='utf-8'))
parsed={}
for value,block in palette.items():
    m=re.fullmatch(r'([a-z_0-9]+:[a-z_0-9]+)(?:\[(.*)\])?',block);assert m,block
    name,props=m.groups();p=dict(t.split('=') for t in props.split(',')) if props else {}
    if name=='murimblock:dark_blood':assert p=={'level':'0'}
    else:assert any(state.get('properties',{})==p for state in reg[name]['states']),block
    parsed[value]=(name,p)
mn,mx=meta['minimum'],meta['maximum']
def inside(p):return all(mn[i]<=p[i]<=mx[i] for i in range(3))
def at(p):
    if not inside(p):return ('outside',{})
    x,y,z=p;return parsed[int(grid[y-mn[1],z-mn[2],x-mn[0]])]
def clear(p):
    name,_=at(p)
    return name in ('minecraft:air','minecraft:ladder','minecraft:red_wall_banner')
def support(p):
    name,props=at(p)
    return name not in ('outside','minecraft:air','minecraft:ladder','minecraft:red_wall_banner',
                        'murimblock:dark_blood','minecraft:lantern','minecraft:dark_oak_fence','minecraft:red_bed')
def stand(p):
    x,y,z=p
    return clear(p) and clear((x,y+1,z)) and (support((x,y-1,z)) or at(p)[0]=='minecraft:ladder')

start=tuple(meta['origin']);assert stand(start)
todo=deque([start]);reached={start}
while todo:
    x,y,z=todo.popleft()
    for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
        for dy in (0,1,-1):
            p=x+dx,y+dy,z+dz
            if p in reached or not stand(p):continue
            if dy==1 and not clear((x,y+2,z)):continue
            if dy==-1 and not clear((p[0],y+1,p[2])):continue
            reached.add(p);todo.append(p)
    if at((x,y,z))[0]=='minecraft:ladder':
        for dy in (-1,1):
            p=x,y+dy,z
            if p not in reached and stand(p):reached.add(p);todo.append(p)
access={name:tuple(p) in reached for name,p in meta['targets'].items()}
for name,ok in access.items():print(('PASS' if ok else 'FAIL')+' access '+name)
assert all(access.values()),{name:meta['targets'][name] for name,ok in access.items() if not ok}
court=meta['court'];cells=0
for x in range(court['x'][0],court['x'][1]+1):
    for z in range(court['z'][0],court['z'][1]+1):
        assert (x,1,z) in reached
        for y in range(1,court['clear_height']+1):assert at((x,y,z))[0]=='minecraft:air'
        cells+=1
for x0,x1,y,z0,z1 in meta['pools']:
    for x in range(x0,x1+1):
        for z in range(z0,z1+1):
            assert at((x,y,z))[0]=='murimblock:dark_blood'
            assert support((x,y-1,z))
    for x,z in [(x,z) for x in range(x0,x1+1) for z in (z0-1,z1+1)]+[(x,z) for x in (x0-1,x1+1) for z in range(z0,z1+1)]:
        assert support((x,y,z)),('open blood basin',x,y,z)

be_positions={};counts=Counter()
for e in s['Blocks']['BlockEntities']:
    p=tuple(int(e['Pos'][i])+mn[i] for i in range(3))
    kind=str(e['Id']);name,props=at(p)
    assert p not in be_positions
    be_positions[p]=kind;counts[kind]+=1
    assert kind==('minecraft:banner' if name=='minecraft:red_wall_banner' else 'minecraft:bed' if name=='minecraft:red_bed' else name)
    assert not e['Data'].get('Items',[])
    if kind=='minecraft:chest':assert clear((p[0],p[1]+1,p[2])),('chest lid blocked',p)
directions={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}
attachments=0
for yy,zz,xx in np.argwhere(grid!=0):
    p=(int(xx)+mn[0],int(yy)+mn[1],int(zz)+mn[2]);x,y,z=p;name,props=at(p)
    if name in ('minecraft:chest','minecraft:barrel','minecraft:smoker','minecraft:red_bed','minecraft:red_wall_banner'):
        assert p in be_positions,('missing block entity',p,name)
    if name=='minecraft:red_bed':
        dx,dz=directions[props['facing']];k=1 if props['part']=='foot' else -1
        other,op=at((x+dx*k,y,z+dz*k))
        assert other==name and op['part']!=props['part'] and op['facing']==props['facing']
        assert support((x,y-1,z));attachments+=1
    if name in ('minecraft:ladder','minecraft:red_wall_banner'):
        dx,dz=directions[props['facing']]
        assert support((x-dx,y,z-dz)),('unsupported attachment',p,name)
        attachments+=1
    if name=='minecraft:lantern':
        assert support((x,y+(1 if props['hanging']=='true' else -1),z)),('unsupported lantern',p)
        attachments+=1
report={'status':'passed','format':'Sponge v3','minecraft':'1.21.1','size':[W,H,D],
        'decoded_cells':len(values),'non_air':meta['non_air'],'valid_states':len(palette),
        'accessible_targets':access,'court_clear_cells':cells,'court_clear_height':5,
        'contained_blood_pools':len(meta['pools']),'checked_attachments':attachments,
        'block_entity_counts':dict(counts),'entities':0,'loot_items':0,
        'method':'Independent NBT readback, registry validation, two-block headroom and at most one-block steps, with ladders. Static checks; not Minecraft gameplay or fluid simulation.'}
(OUT/'verification.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(report,indent=2))
