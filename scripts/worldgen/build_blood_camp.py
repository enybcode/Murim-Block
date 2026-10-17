"""Author a 65-block Blood Sect outpost. Writes assets only, never a Minecraft world."""
from pathlib import Path
from collections import Counter
from functools import lru_cache
import hashlib, json, math, sys, time
import numpy as np

import argparse
import nbtlib as n
ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--block-report', type=Path, required=True, help='Minecraft 1.21.1 generated reports/blocks.json')
parser.add_argument('--asset-dir', type=Path, default=ROOT / 'art/structures/blood-camp')
args = parser.parse_args()
OUT = args.asset_dir
OUT.mkdir(parents=True, exist_ok=True)
REG = json.loads(args.block_report.read_text(encoding='utf-8'))
REG['murimblock:dark_blood'] = {'states': [{'properties': {'level': str(i)}, 'default': i == 0} for i in range(16)]}
BLOCKS, BES, TARGETS, POOLS, BUILDINGS = {}, {}, {}, [], []
MIN, MAX, ORIGIN = (-32, -3, -32), (32, 19, 32), (0, 1, 32)

@lru_cache(None)
def st(name, **kw):
    name = name if ':' in name else 'minecraft:' + name
    states = REG[name]['states']
    props = dict(next(s for s in states if s.get('default')).get('properties', {}))
    props.update({k: str(v).lower() for k, v in kw.items()})
    assert any(s.get('properties', {}) == props for s in states), (name, props)
    return name + ('[' + ','.join(f'{k}={v}' for k,v in sorted(props.items())) + ']' if props else '')

AIR = st('air')
STONE = st('deepslate_bricks')
TILE = st('deepslate_tiles')
TRIM = st('polished_blackstone')
PALE = st('smooth_sandstone')
WOOD = st('dark_oak_planks')
LOG = st('dark_oak_log', axis='y')
POST = st('stripped_mangrove_log', axis='y')
RED = st('red_nether_bricks')
BLOOD = st('murimblock:dark_blood', level=0)

def put(x, y, z, block):
    p = (int(x), int(y), int(z))
    assert all(MIN[i] <= p[i] <= MAX[i] for i in range(3)), p
    BES.pop(p, None)
    if block == AIR: BLOCKS.pop(p, None)
    else: BLOCKS[p] = block

def at(x, y, z): return BLOCKS.get((x,y,z), AIR)

def box(x0,x1,y0,y1,z0,z1, block):
    for y in range(y0,y1+1):
        for z in range(z0,z1+1):
            for x in range(x0,x1+1): put(x,y,z,block)

def floor(x0,x1,z0,z1,y,block): box(x0,x1,y,y,z0,z1,block)

def be(x,y,z,block,kind,data=None):
    put(x,y,z,block)
    BES[x,y,z] = (kind, data or {})

def chest(x,y,z,facing='south'):
    be(x,y,z,st('chest',facing=facing),'minecraft:chest',{'Items':n.List[n.Compound]([])})

def barrel(x,y,z):
    be(x,y,z,st('barrel',facing='up'),'minecraft:barrel',{'Items':n.List[n.Compound]([])})

def banner(x,y,z,facing='south'):
    be(x,y,z,st('red_wall_banner',facing=facing),'minecraft:banner',{'patterns':n.List[n.Compound]([])})

def bed(x,y,z,facing='north'):
    dx,dz={'north':(0,-1),'south':(0,1),'east':(1,0),'west':(-1,0)}[facing]
    for xx,zz,part in [(x,z,'foot'),(x+dx,z+dz,'head')]:
        be(xx,y,zz,st('red_bed',facing=facing,part=part),'minecraft:bed')

def lamp(x,z,y=0):
    put(x,y+1,z,TRIM)
    put(x,y+2,z,st('lantern'))

def roof(x0,x1,z0,z1,y,rise=4,overhang=2):
    """Continuous hollow hip roof, tiled slopes, raised corner tips and ridge caps."""
    a,b,c,d=x0-overhang,x1+overhang,z0-overhang,z1+overhang
    for z in range(c,d+1):
        for x in range(a,b+1):
            distances=[x-a,b-x,z-c,d-z]
            level=min(min(distances),rise)
            face=['east','west','south','north'][distances.index(min(distances))]
            value=st('deepslate_tile_stairs',facing=face) if level<rise else TILE
            put(x,y+level,z,value)
    # Curled up eave tips are supported by the preceding slope blocks.
    for x in (a,b):
        for z in (c,d):
            put(x,y+1,z,st('deepslate_tile_slab',type='bottom'))
    # Restrained vermilion ridge, with dark finials.
    mid=(c+d)//2
    for x in range(a+rise,b-rise+1):
        put(x,y+rise+1,mid,st('red_nether_brick_slab'))
    for x in (a+rise,b-rise):
        put(x,y+rise+1,mid,TRIM)
        put(x,y+rise+2,mid,st('polished_blackstone_slab'))

def hall(name,x0,x1,z0,z1,face='south',base=0,height=4):
    cx,cz=(x0+x1)//2,(z0+z1)//2
    floor(x0-1,x1+1,z0-1,z1+1,base,STONE)
    floor(x0+1,x1-1,z0+1,z1-1,base,WOOD)
    for y in range(base+1,base+height+1):
        for x in range(x0,x1+1):
            for z in (z0,z1): put(x,y,z,st('gray_terracotta'))
        for z in range(z0,z1+1):
            for x in (x0,x1): put(x,y,z,st('gray_terracotta'))
    for x in range(x0,x1+1):
        for z in (z0,z1):
            put(x,base+1,z,RED)
            put(x,base+height,z,st('dark_oak_log',axis='x'))
    for z in range(z0,z1+1):
        for x in (x0,x1):
            put(x,base+1,z,RED)
            put(x,base+height,z,st('dark_oak_log',axis='z'))
    xs=sorted(set([x0,x1]+list(range(x0+4,x1,4))))
    zs=sorted(set([z0,z1]+list(range(z0+4,z1,4))))
    for x in xs:
        for z in (z0,z1): box(x,x,base+1,base+height,z,z,POST)
    for z in zs:
        for x in (x0,x1): box(x,x,base+1,base+height,z,z,POST)
    for x in range(x0+2,x1-1,4):
        for z in (z0,z1): box(x,x+1,base+2,base+3,z,z,st('gray_stained_glass'))
    for z in range(z0+2,z1-1,4):
        for x in (x0,x1): box(x,x,base+2,base+3,z,z,st('gray_stained_glass'))
    if face in ('north','south'):
        zz=z0 if face=='north' else z1
        box(cx-1,cx+1,base+1,base+3,zz,zz,AIR)
        entry=(cx,base+1,zz)
    else:
        xx=x0 if face=='west' else x1
        box(xx,xx,base+1,base+3,cz-1,cz+1,AIR)
        entry=(xx,base+1,cz)
    # Interior ceiling lights attached to roof beams.
    floor(x0,x1,cz,cz,base+height,st('dark_oak_log',axis='x'))
    for x in (x0+3,x1-3):
        put(x,base+height-1,cz,st('lantern',hanging=True))
    roof(x0,x1,z0,z1,base+height+1,4 if height<6 else 5)
    TARGETS[name]=(cx,base+1,cz)
    TARGETS[name+' entrance']=entry
    BUILDINGS.append({'name':name,'bounds':[x0,x1,z0,z1],'floor':base,'door':face})

def tower(cx,cz,name):
    a,b,c,d=cx-4,cx+4,cz-4,cz+4
    floor(a,b,c,d,0,STONE)
    for x in (a,b): box(x,x,1,7,c,d,STONE)
    for z in (c,d): box(a,b,1,7,z,z,STONE)
    box(a+1,b-1,1,7,c+1,d-1,AIR)
    floor(a,b,c,d,7,WOOD)
    # Entrance faces inward; ladder is entirely inside, backed by the north wall.
    box(cx-1,cx+1,1,3,c,c,AIR)
    lx,lz=cx-2,c+1
    put(lx,8,c,LOG)
    for y in range(1,9): put(lx,y,lz,st('ladder',facing='south'))
    for x in (a,b):
        for z in (c,d): box(x,x,1,11,z,z,LOG)
    for x in range(a,b+1):
        for z in (c,d): put(x,8,z,st('dark_oak_fence'))
    for z in range(c,d+1):
        for x in (a,b): put(x,8,z,st('dark_oak_fence'))
    put(lx,8,c,LOG)
    roof(a,b,c,d,12,3,1)
    floor(a,b,cz,cz,11,st('dark_oak_log',axis='x'))
    put(cx,10,cz,st('lantern',hanging=True))
    banner(cx,5,d+1)
    TARGETS[name+' ground']=(cx,1,cz)
    TARGETS[name+' lookout']=(cx,8,cz)
    BUILDINGS.append({'name':name,'bounds':[a,b,c,d],'floor':0,'lookout':7})

# Stable three-block sandstone foundation, with the local ground at Y=0.
box(-32,32,-3,-1,-32,32,st('sandstone'))
floor(-32,32,-32,32,0,st('sand'))
floor(-28,28,-28,28,0,st('smooth_sandstone'))
for z in range(-28,29):
    for x in range(-28,29):
        r=(x*73856093 ^ z*19349663)&255
        if r<35: put(x,0,z,st('cut_sandstone'))
        elif r<43: put(x,0,z,st('sandstone'))

# Low fortified perimeter: masonry footings, timber panels, vermilion bands, tiled caps.
for x in range(-29,30):
    for z in (-29,29):
        box(x,x,1,4,z,z,WOOD)
        put(x,1,z,STONE);put(x,4,z,RED);put(x,5,z,st('deepslate_tile_slab'))
for z in range(-29,30):
    for x in (-29,29):
        box(x,x,1,4,z,z,WOOD)
        put(x,1,z,STONE);put(x,4,z,RED);put(x,5,z,st('deepslate_tile_slab'))
for t in range(-29,30,6):
    for x,z in [(t,-29),(t,29),(-29,t),(29,t)]:
        box(x,x,1,5,z,z,STONE);put(x,6,z,st('polished_blackstone_slab'))

# Entrance and its seven-block-wide unobstructed approach.
box(-3,3,1,5,26,29,AIR)
for x in (-5,-4,4,5): box(x,x,1,6,26,28,POST)
floor(-5,5,26,28,6,st('dark_oak_log',axis='x'))
roof(-5,5,26,28,7,2,2)
for x in (-5,5): banner(x,5,29)
floor(-3,3,15,32,0,st('polished_deepslate'))
for z in range(16,33):
    for x in (-4,4): put(x,0,z,PALE)
tower(-24,24,'West watchtower')
tower(24,24,'East watchtower')

# Habitable camp wings with side doors opening onto the courtyard.
hall('West barracks',-25,-13,-7,3,'east')
hall('East barracks',13,25,-7,3,'west')
hall('Armory',-25,-13,9,16,'east')
hall('Supplies and kitchen',13,25,9,16,'west')
for side in (-1,1):
    center=19*side
    for x in (center-3,center,center+3):
        bed(x,1,-4,'north');bed(x,1,0,'south')
    for x in (center-4,center+4):chest(x,1,-2,'south')
# Workshops: display rails, anvils and work surfaces; no item entities or loot.
for z in (10,12,14):
    put(-24,1,z,st('smithing_table') if z==10 else st('anvil',facing='south'))
    barrel(-22,1,z)
for x in (-20,-18,-16):
    put(x,1,10,st('dark_oak_fence'))
    put(x,2,10,st('dark_oak_fence'))
for x in (-20,-17): chest(x,1,15,'north')
for x in (15,17,19):
    barrel(x,1,10);barrel(x,1,15)
for z in (11,14):
    be(24,1,z,st('smoker',facing='west'),'minecraft:smoker',{'Items':n.List[n.Compound]([])})
put(22,1,15,st('crafting_table'))
put(22,1,10,st('water_cauldron',level=3))
for z in (11,12,13): put(20,1,z,st('dark_oak_slab',type='top'))

# Main hall, raised by one block. The threshold staircase is beneath the front eave.
hall('Blood sanctuary',-10,10,-25,-13,'south',1,6)
floor(-11,11,-26,-12,0,STONE)
for x in range(-3,4): put(x,1,-11,st('polished_deepslate_stairs',facing='north'))
floor(-3,3,-10,-4,0,st('polished_deepslate'))
for x in (-8,8): banner(x,5,-12)
# Low altar and built-in scripture shelves along the back wall.
floor(-3,3,-23,-21,2,TRIM)
for x in (-7,-6,6,7):box(x,x,2,3,-24,-24,st('bookshelf'))
for x in (-5,5):chest(x,2,-23)
for x in (-4,4):lamp(x,-20,1)
TARGETS['Blood sanctuary']=(0,2,-17)

# Open practice ground (northwest), small fenced ritual garden (northeast).
floor(-25,-16,-24,-14,0,st('coarse_dirt'))
for x in (-23,-19):
    put(x,1,-22,st('dark_oak_fence'));put(x,2,-22,st('target'))
    put(x,3,-22,st('hay_block'))
floor(16,25,-25,-14,0,STONE)
floor(17,24,-24,-15,0,TRIM)
# Basin is recessed below walking height and cannot spill over a full-block rim.
floor(18,23,-22,-17,-1,TRIM)
floor(18,23,-22,-17,0,BLOOD)
POOLS.append([18,23,0,-22,-17])
# A small blood-crystal cluster: faceted stepped glass, dark root, no emissive neon.
def crystal(cx,cz,base,h,r=1):
    for yy in range(h):
        radius=r if yy<h-2 else 0
        for dx in range(-radius,radius+1):
            for dz in range(-radius,radius+1):
                if abs(dx)+abs(dz)<=radius:
                    put(cx+dx,base+yy,cz+dz,st('red_stained_glass') if yy>0 else RED)
crystal(20,-24,1,5,1)
crystal(24,-23,1,3,0)
crystal(17,-24,1,3,0)
# Small ceremonial vessels inside the sanctuary: contained actual fluid.
for x in (-7,7):
    floor(x-1,x+1,-19,-17,1,TRIM)
    put(x,1,-18,BLOOD)
    POOLS.append([x,x,1,-18,-18])
crystal(0,-22,3,4,1)

# A truly clear 19 x 17 court. Markings are flush with the floor, never raised obstacles.
for z in range(-2,15):
    for x in range(-9,10):
        mat=st('polished_deepslate') if (abs(x)==9 or z in (-2,14)) else PALE
        if 3.5<math.hypot(x,z-6)<4.5:mat=RED
        put(x,0,z,mat)
        box(x,x,1,5,z,z,AIR)
# A circular blood-drop emblem inset in the court.
for dx,dz in [(0,4),(-1,5),(0,5),(1,5),(-1,6),(0,6),(1,6),(0,7)]:put(dx,0,dz,RED)
for x in (-11,11):
    for z in (-9,6,18):lamp(x,z)
for x in (-27,27):
    for z in (-26,-12,7):lamp(x,z)
for x in (-8,8):lamp(x,24)
# Rest benches beside the approach and hangings on internal walls.
for x in (-9,9):
    for z in (19,20,21):put(x,1,z,st('dark_oak_stairs',facing='west' if x<0 else 'east'))
for x in (-17,17):banner(x,4,30)
for x in (-20,-8,8,20):banner(x,4,-28)

# Recompute fence joins so the saved preview matches the placed blocks.
for (x,y,z),block in list(BLOCKS.items()):
    if block.startswith('minecraft:dark_oak_fence['):
        props={}
        for face,(dx,dz) in {'east':(1,0),'west':(-1,0),'north':(0,-1),'south':(0,1)}.items():
            nb=at(x+dx,y,z+dz)
            props[face]=nb!=AIR and not any(t in nb for t in ('lantern','ladder','glass','banner'))
        put(x,y,z,st('dark_oak_fence',**props))

TARGETS.update({'Entry':ORIGIN,'Courtyard':(0,1,6),'Training ground':(-21,1,-17),
                'Ritual garden':(25,1,-19),'Sanctuary porch':(0,2,-12)})
W,H,D=[MAX[i]-MIN[i]+1 for i in range(3)]
palette=[AIR]+sorted(set(BLOCKS.values())-{AIR});ids={v:i for i,v in enumerate(palette)}
grid=np.zeros((H,D,W),dtype=np.uint16)
for (x,y,z),value in BLOCKS.items():grid[y-MIN[1],z-MIN[2],x-MIN[0]]=ids[value]

def varints(values):
    data=bytearray()
    for value in values:
        v=int(value)
        while v>=128:data.append((v&127)|128);v>>=7
        data.append(v)
    return data

entries=[n.Compound({'Id':n.String(kind),'Pos':n.IntArray([p[i]-MIN[i] for i in range(3)]),
                     'Data':n.Compound(data)}) for p,(kind,data) in sorted(BES.items())]
offset=[MIN[i]-ORIGIN[i] for i in range(3)]
schem=n.Compound({'Version':n.Int(3),'DataVersion':n.Int(3955),
    'Width':n.Short(W),'Height':n.Short(H),'Length':n.Short(D),'Offset':n.IntArray(offset),
    'Metadata':n.Compound({'Name':n.String('Blood Sect - Desert Outpost 65'),
        'Author':n.String('Colleuhu95 / Codex'),'Date':n.Long(int(time.time()*1000)),
        'RequiredMods':n.List[n.String]([n.String('murimblock')]),
        'Description':n.String('65 x 65 Murim outpost, two barracks, workshops, sanctuary, open court. No entities or loot. Origin: feet outside south gate.')}),
    'Blocks':n.Compound({'Palette':n.Compound({b:n.Int(i) for i,b in enumerate(palette)}),
        'Data':n.ByteArray(np.frombuffer(varints(grid.ravel()),dtype=np.int8)),
        'BlockEntities':n.List[n.Compound](entries)}),'Entities':n.List[n.Compound]([])})
file=OUT/'camp_secte_sang_65.schem'
n.File({'Schematic':schem},gzipped=True).save(file)
stats={'file':file.name,'size':[W,H,D],'minimum':MIN,'maximum':MAX,'origin':ORIGIN,'offset':offset,
       'minecraft':'1.21.1','data_version':3955,'required_mod':'murimblock',
       'non_air':len(BLOCKS),'volume':W*H*D,'palette_states':len(palette),
       'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'bytes':file.stat().st_size,
       'targets':TARGETS,'pools':POOLS,'buildings':BUILDINGS,'block_entities':len(BES),
       'court':{'x':[-9,9],'z':[-2,14],'floor':0,'clear_height':5},
       'materials':dict(Counter(BLOCKS.values()))}
(OUT/'export-stats.json').write_text(json.dumps(stats,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({k:stats[k] for k in ('file','size','non_air','volume','palette_states','block_entities','bytes')},indent=2))
