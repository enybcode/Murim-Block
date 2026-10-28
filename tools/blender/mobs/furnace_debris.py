"""The Fourneau d'alchimie burst into pieces (storyboard: docs/mobs/boards/furnace_burst.png).

Not a mob: the pieces the furnace breaks into when it explodes, each built where it sat on the vessel so they fly out
from there (FurnaceBurst in game). A part named "<piece>__<part>" belongs to that piece. Each wall breaks into three
thick shards of bronze with stepped broken edges: the outer face keeps its relief (belly, belt, rim, ribs, studs, the
furnace mouth on the front), the inside is blackened by the fire, the break shows fresh, bright metal. The lid, the
knob, the handles, the seal and the legs fly whole, as built for the furnace.
"""

from murim_mob import Part
import furnace as F

TEXTURE_WIDTH = 256

JOINTS = [("Root", None, (0, 10, 0))]
HELPERS = []
STAND = {}
SHOW_POSE = {}
VIEW = F.VIEW

# The wall's thickness, inward from its outer face (6 px from the middle).
SURFACE, THICK = 6.0, 1.4
OPPOSITE = {"front": "back", "back": "front", "left": "right", "right": "left"}
# Each wall's three shards, as rectangles (along the wall, from bottom; to along, to top) in the wall's own frame.
SHARDS = {
    "a": [(-6, 16, 1, 24), (-6, 13, -2, 16)],
    "b": [(1, 15, 6, 24)],
    "c": [(-6, 10, -2, 13), (-2, 10, 1, 16), (1, 10, 6, 15)],
}


def plate(wall, salt):
    def paint(face, x, y, w, h):
        if face == wall:
            return F.bronze(salt)(face, x, y, w, h)
        if face == OPPOSITE[wall]:
            # Blackened by the fire inside.
            return F.SOOT if F.noise(x, y, salt) < 0.7 else F.DARK[0]
        # The break: fresh metal, brighter than the tarnished outside.
        return F.BRONZE[4] if F.noise(x, y, salt + 3) < 0.6 else F.BRONZE[3]
    return paint


def mouth(face, x, y, w, h):
    if face == "front" and 0 < x < w - 1 and 0 < y < h - 1:
        return F.EMBER[2] if (x + y) % 2 else F.EMBER[1]
    return F.SOOT


# Relief on the outer face: (first along, last along, bottom, top, how far it stands out, painter, front only).
RELIEF = [
    (-6, 6, 10, 15, 0.5, F.belly, False),
    (-6, 6, 15, 16, 0.35, F.band(7), False),
    (-6.5, 6.5, 23, 25, 0.5, F.band(5), False),
    (-6.6, -5.4, 10, 24, 0.6, F.rib, False),
    (5.4, 6.6, 10, 24, 0.6, F.rib, False),
    (-3, 3, 18, 21, 0.05, mouth, True),
    (-4.5, 4.5, 21, 22.5, 1.6, F.frame, True),
    (-4, 4, 16.5, 17.3, 1.3, F.frame, True),
    (3, 4, 17, 21, 1.0, F.frame, True),
    (-4, -3, 17, 21, 1.0, F.frame, True),
    *[(-4.5 + 3 * i, -3.5 + 3 * i, 23.3, 24.3, 1.0, F.stud, False) for i in range(4)],
]


def box(wall, a0, a1, y0, y1, n0, n1):
    """A box in a wall's frame (along a, up y, out n from the middle) as a corner and a size on the vessel."""
    if wall == "front":
        x0, x1, z0, z1 = a0, a1, n0, n1
    elif wall == "back":
        x0, x1, z0, z1 = -a1, -a0, -n1, -n0
    elif wall == "left":
        x0, x1, z0, z1 = n0, n1, -a1, -a0
    else:
        x0, x1, z0, z1 = -n1, -n0, a0, a1
    return (x0, y0, z0), (x1 - x0, y1 - y0, z1 - z0)


def wall_parts(wall, salt):
    parts = []
    for shard, rects in SHARDS.items():
        piece = f"{wall}_{shard}"
        for i, (a0, y0, a1, y1) in enumerate(rects):
            corner, size = box(wall, a0, a1, y0, y1, SURFACE - THICK, SURFACE)
            parts.append(Part(f"{piece}__plate{i}", "Root", corner, size, plate(wall, salt + i)))
            for j, (r0, r1, b0, b1, out, paint, front) in enumerate(RELIEF):
                if front and wall != "front":
                    continue
                # The relief this rectangle carries (a rim or a rib reaching past the edge stays with it).
                c0, c1 = max(r0, a0 - (0.6 if a0 <= -6 else 0)), min(r1, a1 + (0.6 if a1 >= 6 else 0))
                d0, d1 = max(b0, y0), min(b1, y1 + (1 if y1 >= 24 else 0))
                if c1 - c0 < 0.3 or d1 - d0 < 0.3:
                    continue
                corner, size = box(wall, c0, c1, d0, d1, SURFACE - 0.01, SURFACE + out)
                parts.append(Part(f"{piece}__relief{i}_{j}", "Root", corner, size, paint))
    return parts


WHOLE = {
    "lid": ("lid_skirt", "lid", "lid_top", "knob_base", "vent"),
    "knob": ("knob",),
    "handle_l": ("handle_l_top", "handle_l_out", "handle_l_low"),
    "handle_r": ("handle_r_top", "handle_r_out", "handle_r_low"),
    "seal": ("plaque",),
}


def parts():
    p = []
    for salt, wall in enumerate(("front", "back", "left", "right")):
        p += wall_parts(wall, 31 + 5 * salt)
    for part in F.parts():
        piece = next((key for key, names in WHOLE.items() if part.name in names), None)
        if piece is None:
            for leg in F.LEGS:
                if part.name.endswith("_" + leg) or part.name.startswith(f"toe_{leg}"):
                    piece = f"leg_{leg}"
        if piece is not None:
            part.name, part.bind = f"{piece}__{part.name}", "Root"
            p.append(part)
    return p
