"""Fourneau d'alchimie vivant (the creeper's new look; design sheet: docs/mobs/fourneau-alchimie.png).

The bronze pill furnace of an alchemist killed by a failed refining, risen on three feet after drinking the Qi of
the lost pills. Its own skeleton: the vessel (Root), its lid (which shakes, then jumps before it bursts), two handles
and three legs of two segments (two in front, one behind). A rim, a belt and an alchemist's seal stand out on the
vessel; the furnace mouth and the embers inside glow.
"""

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

JOINTS = [
    ("Root", None, (0, 10, 0)),
    ("Lid", "Root", (0, 25, 0)),
    ("Handle_L", "Root", (7, 21, 0)),
    ("Handle_R", "Root", (-7, 21, 0)),
]
# Two legs in front (left and right) and one behind, each hip and knee.
LEGS = {"FL": (3.5, 3), "FR": (-3.5, 3), "B": (0, -4)}
for _name, (_x, _z) in LEGS.items():
    JOINTS += [(f"Leg_{_name}", "Root", (_x, 10, _z)), (f"Shin_{_name}", f"Leg_{_name}", (_x, 5, _z))]
HELPERS = []

STAND = {}
# Showing off its fuse: the lid lifted and tilted, the vessel leaning back.
SHOW_POSE = {"Root": (-8, 0, 0), "Lid": (-16, 0, 12)}
VIEW = ((0, 14, 0), (0, 18, 6), 40, 18, 1.1)

BRONZE = hexes("#4a3410", "#6b4c18", "#8f6a22", "#b0862e", "#d2a849")
DARK = hexes("#2e2008", "#3e2c10", "#5e4316", "#7a5a1e", "#94702a")
VERDIGRIS = hexes("#2f4a3a", "#3f5f48", "#4f7a5a", "#6a9474", "#8db096")
EMBER = hexes("#5a1a0c", "#8a2e10", "#c4521a", "#e8742a", "#ffb24a")
SOOT = (26, 18, 12)
FIRE, FIRE_CORE = (190, 70, 20), (240, 130, 40)


def bronze(salt, offset=0, rough=0.12):
    def paint(face, x, y, w, h):
        if face != "bottom" and noise(x, y, salt + 7) < 0.035:
            return VERDIGRIS[2] if noise(x, y, salt + 8) < 0.5 else VERDIGRIS[1]
        return shade(BRONZE, face, x, y, h, salt, rough, offset)
    return paint


SEAL = [
    "XXXXXX",
    "X.XX.X",
    "XX..XX",
    "X.XX.X",
    "XXXXXX",
]


def vessel(face, x, y, w, h):
    # The furnace mouth on the front: a dark frame, embers deep inside.
    if face == "front":
        if 3 <= x <= w - 4 and 3 <= y <= 6:
            if 4 <= x <= w - 5 and 4 <= y <= 5:
                return EMBER[2] if (x + y) % 2 else EMBER[1]
            return SOOT
    if face == "top":
        return DARK[1]
    if face == "bottom":
        return DARK[0]
    return bronze(3)(face, x, y, w, h)


def vessel_glow(face, x, y, w, h):
    if face == "front" and 4 <= x <= w - 5 and 4 <= y <= 5:
        return FIRE + (140,) if (x + y) % 2 else FIRE_CORE + (200,)
    return None


def belly(face, x, y, w, h):
    return bronze(21, -1)(face, x, y, w, h)


def plaque(face, x, y, w, h):
    """The alchemist's seal on a raised plaque."""
    if face == "front" and 0 < x < w - 1 and 0 < y < h - 1:
        return BRONZE[1] if SEAL[y - 1][x - 1] == "X" else BRONZE[4]
    return BRONZE[2] if face != "top" else BRONZE[4]


def stud(face, x, y, w, h):
    return BRONZE[4] if face in ("front", "top") else BRONZE[2]


def rib(face, x, y, w, h):
    if face in ("top", "bottom"):
        return BRONZE[4]
    return BRONZE[3] if y % 4 else BRONZE[4]


def frame(face, x, y, w, h):
    return BRONZE[4] if face == "top" else shade(DARK, face, x, y, h, 23, 0.1, offset=2)


def mask(face, x, y, w, h):
    """A beast's mask where a leg meets the vessel: two eyes and a snout."""
    if face == "front":
        if y == 1 and x in (0, w - 1):
            return SOOT
        if y == h - 1 and 0 < x < w - 1:
            return BRONZE[4]
    return shade(BRONZE, face, x, y, h, 25, 0.1, offset=1)


def band(salt):
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return BRONZE[2]
        return BRONZE[4] if y == 0 else shade(BRONZE, face, x, y, h, salt, 0.1, offset=1)
    return paint


def lid(face, x, y, w, h):
    if face == "bottom":
        return SOOT
    if face == "top" and (x + y) % 4 == 0:
        return BRONZE[3]
    return shade(DARK, face, x, y, h, 11, 0.12, offset=1)


def knob(face, x, y, w, h):
    return BRONZE[4] if face == "top" else BRONZE[3]


def handle(face, x, y, w, h):
    return shade(DARK, face, x, y, h, 13, 0.1, offset=1 if y == 0 else 0)


def leg(face, x, y, w, h):
    if face == "bottom":
        return DARK[0]
    return shade(DARK, face, x, y, h, 17, 0.12)


def claw(face, x, y, w, h):
    return BRONZE[3] if face == "top" else shade(BRONZE, face, x, y, h, 19, 0.1)


def smoke_vent(face, x, y, w, h):
    return SOOT if face == "top" else DARK[1]


def parts():
    p = [
        # The vessel, a band near its rim and one round its middle, the rim's lip, two handles.
        Part("vessel", "Root", (-6, 10, -6), (12, 14, 12), vessel, emissive=vessel_glow),
        Part("rim", "Root", (-6.5, 23, -6.5), (13, 2, 13), band(5)),
        # The belly, a little wider than the neck above it.
        Part("belly", "Root", (-6, 10, -6), (12, 5, 12), belly, inflate=0.5),
        Part("belt", "Root", (-6, 15, -6), (12, 1, 12), band(7), inflate=0.35),
        Part("foot_ring", "Root", (-5.5, 9, -5.5), (11, 1, 11), band(9)),
        # Handles: rings standing out of each side (a bar out, an upright, a bar back).
        Part("handle_l_top", "Handle_L", (6, 22, -1), (3, 1, 2), handle),
        Part("handle_l_out", "Handle_L", (8, 18, -1), (1, 4, 2), handle),
        Part("handle_l_low", "Handle_L", (6, 18, -1), (2, 1, 2), handle),
        Part("handle_r_top", "Handle_R", (-9, 22, -1), (3, 1, 2), handle),
        Part("handle_r_out", "Handle_R", (-9, 18, -1), (1, 4, 2), handle),
        Part("handle_r_low", "Handle_R", (-8, 18, -1), (2, 1, 2), handle),
        # The furnace mouth framed: a hood over it, a sill under it, posts at its sides.
        Part("hood", "Root", (-4.5, 21, 5.6), (9, 1.5, 2), frame),
        Part("sill", "Root", (-4, 16.5, 5.8), (8, 0.8, 1.5), frame),
        Part("post_l", "Root", (3, 17, 5.8), (1, 4, 1), frame),
        Part("post_r", "Root", (-4, 17, 5.8), (1, 4, 1), frame),
        # The seal on its plaque on the belly; ribs down the four corners; studs along the rim.
        Part("plaque", "Root", (-4, 9.5, 6.2), (8, 7, 0.8), plaque),
        *[Part(f"rib_{i}", "Root", (sx * 6.2 - 0.6, 10, sz * 6.2 - 0.6), (1.2, 14, 1.2), rib)
          for i, (sx, sz) in enumerate(((1, 1), (-1, 1), (1, -1), (-1, -1)))],
        *[Part(f"stud_f{i}", "Root", (-4.5 + 3 * i, 23.3, 6.4), (1, 1, 0.6), stud) for i in range(4)],
        *[Part(f"stud_b{i}", "Root", (-4.5 + 3 * i, 23.3, -7), (1, 1, 0.6), stud) for i in range(4)],
        # The lid: a flared skirt, two steps, a pearl knob on a base, a chimney.
        Part("lid_skirt", "Lid", (-6.5, 24.6, -6.5), (13, 1, 13), lid),
        Part("lid", "Lid", (-5.5, 25, -5.5), (11, 2, 11), lid),
        Part("lid_top", "Lid", (-3.5, 27, -3.5), (7, 1, 7), lid),
        Part("knob_base", "Lid", (-1.5, 28, -1.5), (3, 1, 3), knob),
        Part("knob", "Lid", (-1, 29, -1), (2, 2, 2), knob),
        Part("vent", "Lid", (2, 27, 1), (2, 2, 2), smoke_vent),
    ]
    for name, (x, z) in LEGS.items():
        out = 1 if z > 0 else -1
        p += [
            Part(f"leg_{name}", f"Leg_{name}", (x - 1.5, 5, z - 1.5), (3, 5, 3), leg),
            # A beast's mask where the leg meets the vessel, facing out.
            Part(f"mask_{name}", f"Leg_{name}", (x - 2, 7, z + out * 1.6 - 0.5), (4, 3, 1), mask),
            Part(f"knee_{name}", f"Shin_{name}", (x - 1.75, 4.2, z - 1.75), (3.5, 1, 3.5), claw),
            Part(f"shin_{name}", f"Shin_{name}", (x - 1.25, 1, z - 1.25), (2.5, 4, 2.5), leg),
            Part(f"claw_{name}", f"Shin_{name}", (x - 2, 0, z - 2), (4, 1, 4), claw),
            # Three toes reaching out.
            *[Part(f"toe_{name}{t}", f"Shin_{name}", (x - 1.75 + 1.25 * t, 0, z + out * 2 - (0 if out > 0 else 1)),
                   (1, 1, 1), claw) for t in range(3)],
        ]
    return p




def pose(pitch=0, roll=0, up=0, lid=0, tilt=0, roll_lid=0, legs=None):
    """A key: the vessel's lean (pitch forward +, roll), its lift; the lid lifted (px) and tilted; the legs as
    {leg: (hip swing, knee bend)} (swing back +, as X turns the foot backward)."""
    key = {"Root": {"rot": (pitch, 0, roll), "loc": (0, up, 0)},
           "Lid": {"rot": (tilt, 0, roll_lid), "loc": (0, lid, 0)}}
    for name in LEGS:
        swing, bend = (legs or {}).get(name, (0, 0))
        key[f"Leg_{name}"] = (swing, 0, 0)
        key[f"Shin_{name}"] = (bend, 0, 0)
    return key


REST = pose()
# Walking: a heavy waddle from one front foot to the other, the lid clacking at each step.
STEP_L = pose(pitch=4, roll=-6, lid=0.3, tilt=-3, legs={"FL": (-22, 18), "FR": (16, 0), "B": (-8, 6)})
PASS_L = pose(pitch=2, up=0.6, lid=1.2, tilt=2, legs={"FL": (-6, 4), "FR": (4, 10), "B": (6, 0)})
STEP_R = pose(pitch=4, roll=6, lid=0.3, roll_lid=3, legs={"FL": (16, 0), "FR": (-22, 18), "B": (-8, 6)})
PASS_R = pose(pitch=2, up=0.6, lid=1.2, tilt=-2, legs={"FL": (4, 10), "FR": (-6, 4), "B": (6, 0)})
# The fuse: the heat lifts the lid higher at every phase, the vessel trembles, then stands on its toes.
TIPTOE = {"FL": (8, -10), "FR": (8, -10), "B": (-8, -10)}
CALMED = pose(lid=0.4)
TIPPED = pose(roll=62, up=-1, lid=1.5, roll_lid=25, legs={"FL": (-30, 40), "FR": (10, 10), "B": (20, 30)})
DOWN = pose(roll=90, up=-4, lid=4, roll_lid=60, legs={"FL": (-40, 50), "FR": (-10, 30), "B": (30, 40)})


def fuse():
    keys = [(0, REST)]
    # Ticks 1 to 10: the lid rattles faster and faster, a hair higher each time.
    for i, t in enumerate((2, 4, 5, 6, 7, 8, 9, 10)):
        up = 0.6 + 0.15 * i if i % 2 == 0 else 0.15 * i
        keys.append((t, pose(lid=up, tilt=4 if i % 2 == 0 else -3)))
    # 10 to 18: the whole vessel shakes on its feet, the lid riding up.
    for t in range(11, 19):
        s = 1 if t % 2 else -1
        keys.append((t, pose(roll=2.5 * s, pitch=-1, lid=1.6 + 0.12 * (t - 10), tilt=-4 * s)))
    # 18 to 24: steam: the lid lifts high and tilts; the vessel leans back.
    for t in range(19, 25):
        s = 1 if t % 2 else -1
        keys.append((t, pose(roll=1.5 * s, pitch=-3, lid=2.8 + 0.4 * (t - 18), tilt=10 + 3 * s)))
    # 24 to 29: it screams, up on its toes, the lid lifted wide and swaying.
    for t in range(25, 30):
        s = 1 if t % 2 else -1
        keys.append((t, pose(roll=2 * s, up=1.2, lid=5.5 + 0.5 * (t - 24), tilt=-14 + 5 * s, legs=TIPTOE)))
    # 30: the explosion; the lid leaves like a cork.
    keys.append((30, pose(up=1.4, lid=12, tilt=35, legs=TIPTOE)))
    return keys


CLIPS = {
    "stand": [(0, REST), (20, REST)],
    # A simmering pot: still, then the lid lifts a pixel and drops back with a tink.
    "idle": [(0, REST), (16, pose(up=-0.1)), (19, pose(lid=1, tilt=3)), (21, pose(lid=0.1)), (22, pose(lid=0.4, tilt=-1)),
             (24, REST), (40, REST)],
    "walk": [(0, STEP_L), (4, PASS_L), (8, STEP_R), (12, PASS_R), (16, STEP_L)],
    "fuse": fuse(),
    # Cooling down: a long pschhh, the vessel settles, the lid drops back.
    "calm": [(0, pose(pitch=-3, lid=3, tilt=8)), (6, pose(pitch=-1, lid=1.5, tilt=-3)), (12, CALMED), (14, pose(lid=0.8)),
             (16, REST), (20, REST)],
    # Hit: the vessel jolts, the lid hops.
    "flinch": [(0, REST), (2, pose(pitch=-8, lid=2, tilt=-10)), (5, pose(pitch=3, lid=0.3, tilt=4)), (9, REST)],
    # Killed in time: it tips over on its side, the lid rolls off, the fire dies.
    "death": [(0, REST), (5, pose(roll=25, lid=1, roll_lid=8, legs={"FL": (-10, 10)})), (8, TIPPED), (12, DOWN),
              (14, pose(roll=86, up=-3.6, lid=4.5, roll_lid=70, legs={"FL": (-40, 50), "FR": (-10, 30), "B": (30, 40)})),
              (16, DOWN), (20, DOWN)],
}
