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
        # The alchemist's seal engraved under the mouth.
        sx, sy = x - (w - 6) // 2, y - 9
        if 0 <= sx < 6 and 0 <= sy < 5:
            return BRONZE[1] if SEAL[sy][sx] == "X" else BRONZE[3]
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
    """The vessel's belly, with the alchemist's seal engraved on its front."""
    if face == "front":
        sx, sy = x - (w - 6) // 2, y
        if 0 <= sx < 6 and 0 <= sy < 5:
            return BRONZE[1] if SEAL[sy][sx] == "X" else BRONZE[3]
    return bronze(21, -1)(face, x, y, w, h)


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
        Part("handle_l", "Handle_L", (6.5, 19, -1.5), (2, 4, 3), handle),
        Part("handle_r", "Handle_R", (-8.5, 19, -1.5), (2, 4, 3), handle),
        Part("handle_l_tip", "Handle_L", (8.5, 22, -1), (1, 1, 2), knob),
        Part("handle_r_tip", "Handle_R", (-9.5, 22, -1), (1, 1, 2), knob),
        # The lid, its upper step, the knob and a smoke vent.
        Part("lid", "Lid", (-5.5, 25, -5.5), (11, 2, 11), lid),
        Part("lid_top", "Lid", (-3.5, 27, -3.5), (7, 1, 7), lid),
        Part("knob", "Lid", (-1, 28, -1), (2, 2, 2), knob),
        Part("vent", "Lid", (2, 27, 1), (2, 1, 2), smoke_vent),
    ]
    for name, (x, z) in LEGS.items():
        p += [
            Part(f"leg_{name}", f"Leg_{name}", (x - 1.5, 5, z - 1.5), (3, 5, 3), leg),
            Part(f"shin_{name}", f"Shin_{name}", (x - 1.25, 1, z - 1.25), (2.5, 4, 2.5), leg),
            Part(f"claw_{name}", f"Shin_{name}", (x - 2, 0, z - 2), (4, 1, 4), claw),
        ]
    return p
