"""Fileuse de linceuls (the spider's new look; design sheet: docs/mobs/fileuse-linceuls.png).

A tomb spider that weaves the shrouds of the dead and wears them on its back, with the red paper seals torn from the
coffins. Its own skeleton: the cephalothorax (Root), the abdomen behind it, the head in front with two fangs, and
eight legs of three segments each, laid straight out to the sides at rest (as vanilla's spider); STAND arches them
for the renders. The shroud bands are in relief here; their torn ends will hang as cloth with the clips.
"""

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

# name, parent, model position (x left, y up, z forward; pixels). Parents before children.
JOINTS = [
    ("Root", None, (0, 8, 0)),
    ("Abdomen", "Root", (0, 9, -3)),
    ("Head", "Root", (0, 8, 5)),
    ("Fang_L", "Head", (1.5, 6, 8)),
    ("Fang_R", "Head", (-1.5, 6, 8)),
]
LEG_Z = [3.5, 1.5, -0.5, -2.5]
for _i, _z in enumerate(LEG_Z, 1):
    for _side, _x in (("L", 1), ("R", -1)):
        JOINTS += [
            (f"Leg{_i}{_side}_hip", "Root", (4 * _x, 8, _z)),
            (f"Leg{_i}{_side}_knee", f"Leg{_i}{_side}_hip", (10 * _x, 8, _z)),
            (f"Leg{_i}{_side}_foot", f"Leg{_i}{_side}_knee", (16 * _x, 8, _z)),
        ]
HELPERS = []

# Standing: the legs raised at the hip, bent down at the knee and the foot, fanned forward and back.
SPREAD = [38, 13, -13, -38]
STAND = {}
for _i, _spread in enumerate(SPREAD, 1):
    for _side, _s in (("L", 1), ("R", -1)):
        STAND[f"Leg{_i}{_side}_hip"] = (0, -_s * _spread, _s * 38)
        STAND[f"Leg{_i}{_side}_knee"] = (0, 0, -_s * 95)
        STAND[f"Leg{_i}{_side}_foot"] = (0, 0, -_s * 30)
STAND["Root"] = (0, 0, 0)
# Rearing to spit its web: the front legs raised, the body tilted up.
SHOW_POSE = dict(STAND)
SHOW_POSE["Root"] = (-22, 0, 0)
for _side, _s in (("L", 1), ("R", -1)):
    SHOW_POSE[f"Leg1{_side}_hip"] = (0, -_s * 50, _s * 70)
    SHOW_POSE[f"Leg1{_side}_knee"] = (0, 0, -_s * 60)
    SHOW_POSE[f"Leg2{_side}_hip"] = (0, -_s * 20, _s * 55)

VIEW = ((0, 8, -4), (0, 8, 8), 46, 18, 0.8)

CHITIN = hexes("#0f0d0c", "#1a1715", "#252019", "#352e27", "#4a4038")
SHEEN = hexes("#3d3530", "#4f4640", "#665b52", "#7d7068", "#958a80")
SHROUD = hexes("#8f8670", "#b3aa92", "#cfc8b4", "#e3ddcb", "#f1ecdc")
DIRTY = hexes("#6b6252", "#837966", "#a39a82", "#b7ad95", "#c9c0a8")
SEAL = hexes("#4a1010", "#621616", "#7a2020", "#962a26", "#b13a30")
INK = (30, 18, 14)
BONE = hexes("#8f8670", "#b3aa92", "#cfc5ae", "#e2dbc6", "#f1ecdc")
EYE, EYE_DIM = (230, 40, 30), (96, 16, 14)


def chitin(salt, offset=0):
    def paint(face, x, y, w, h):
        # A sheen along the top edges, the hard plates' seams.
        if face != "bottom" and face != "top" and y == 0:
            return SHEEN[2]
        if face == "top" and (x == 0 or x == w - 1):
            return SHEEN[1]
        return shade(CHITIN, face, x, y, h, salt, 0.15, offset)
    return paint


# Eight eyes on the 6 by 5 face: the main pair in the middle, six smaller around it.
MAIN_EYES = {(2, 2), (3, 2)}
EYES = MAIN_EYES | {(1, 1), (4, 1), (0, 2), (5, 2), (1, 3), (4, 3)}


def head(face, x, y, w, h):
    if face == "front" and (x, y) in EYES:
        return EYE if (x, y) in MAIN_EYES else EYE_DIM
    if face == "front" and y == h - 1:
        return CHITIN[0]
    return chitin(3)(face, x, y, w, h)


def head_glow(face, x, y, w, h):
    if face == "front" and (x, y) in EYES:
        return EYE + (255,) if (x, y) in MAIN_EYES else EYE_DIM + (70,)
    return None


def fang(face, x, y, w, h):
    return BONE[1] if y == h - 1 else shade(BONE, face, x, y, h, 5, offset=1 if y == 0 else 0)


def abdomen(face, x, y, w, h):
    # A dark hourglass of plates under the shrouds, glossy ridges along its length.
    if face == "top" and x in (w // 2 - 1, w // 2):
        return SHEEN[1]
    if face in ("right", "left") and y % 3 == 0:
        return CHITIN[3]
    return shade(CHITIN, face, x, y, h, 7, 0.15)


def shroud(salt):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return None
        # Dirtier toward the ground, torn at the lower edge, stitched along the middle.
        if face in ("right", "left") and y == h - 1 and noise(x, salt, 9) < 0.5:
            return None
        if face in ("right", "left") and y > h * 0.6:
            return shade(DIRTY, face, x, y, h, salt, 0.2)
        if face == "top" and y == h // 2 and x % 2 == 0:
            return DIRTY[1]
        return shade(SHROUD, face, x, y, h, salt, 0.15)
    return paint


def torn_end(face, x, y, w, h):
    if y == h - 1 and x % 2 == 1:
        return None
    return shade(DIRTY, face, x, y, h, 11, 0.25)


def seal(face, x, y, w, h):
    # Red paper with a black brushed stroke down the middle.
    if face in ("top", "front") and x == w // 2 and 0 < y < h - 1:
        return INK
    return shade(SEAL, face, x, y, h, 13, 0.15, offset=1)


def spinneret(face, x, y, w, h):
    return shade(SHEEN, face, x, y, h, 15)


def thread(face, x, y, w, h):
    return SHROUD[4] if (x + y) % 2 == 0 else SHROUD[3]


def segment(salt, tip=False):
    def paint(face, x, y, w, h):
        # Leg segments are boxes along X: their "sides" run the leg's length. A pale band at each joint; the tip claw.
        if tip and face == "left" or tip and face == "right":
            return BONE[2]
        if face in ("front", "back", "top", "bottom") and (x == 0 or x == w - 1):
            return SHEEN[2]
        return shade(CHITIN, face, x, y, h, salt, 0.2)
    return paint


def parts():
    p = [
        Part("thorax", "Root", (-4, 5, -3), (8, 6, 8), chitin(17)),
        Part("thorax_ridge", "Root", (-3, 11, -2), (6, 1, 6), chitin(19, 1)),
        Part("head", "Head", (-3, 5, 5), (6, 5, 4), head, emissive=head_glow),
        Part("brow", "Head", (-2.5, 10, 6), (5, 1, 2.5), chitin(21, 1)),
        Part("fang_l", "Fang_L", (0.6, 3, 8), (1.5, 3, 1.2), fang),
        Part("fang_r", "Fang_R", (-2.1, 3, 8), (1.5, 3, 1.2), fang),
        Part("abdomen", "Abdomen", (-5, 4, -16), (10, 9, 13), abdomen),
        Part("spinneret", "Abdomen", (-1.5, 6, -17), (3, 2, 1), spinneret),
        Part("thread", "Abdomen", (-0.5, 6.5, -22), (1, 1, 5), thread),
        # Shroud bands wrapped over the abdomen (open underneath), their torn ends hanging, red seals on top.
        Part("shroud_1", "Abdomen", (-5, 8, -7), (10, 5, 2), shroud(23), inflate=0.45),
        Part("shroud_2", "Abdomen", (-5, 7, -11), (10, 6, 2), shroud(25), inflate=0.45),
        Part("shroud_3", "Abdomen", (-5, 9, -14.5), (10, 4, 1.5), shroud(27), inflate=0.45),
        Part("shroud_end_l", "Abdomen", (5.1, 3, -10.5), (1, 4, 1), torn_end),
        Part("shroud_end_r", "Abdomen", (-6.1, 4.5, -6.5), (1, 4, 1), torn_end),
        Part("shroud_tail", "Abdomen", (-1.5, 10, -18), (3, 1, 3), torn_end),
        Part("seal_1", "Abdomen", (-2.5, 13.3, -11.5), (2, 0.6, 3), seal),
        Part("seal_2", "Abdomen", (1, 13.3, -7.5), (2, 0.6, 3), seal),
    ]
    for i, z in enumerate(LEG_Z, 1):
        for side, s in (("L", 1), ("R", -1)):
            x0 = lambda a, b: (a, b - a) if s > 0 else (-b, b - a)
            fx, fw = x0(3.5, 10.5)
            tx, tw = x0(10, 16.5)
            px, pw = x0(16, 21)
            p += [
                Part(f"leg{i}{side}_femur", f"Leg{i}{side}_hip", (fx, 7, z - 1), (fw, 2, 2), segment(31 + i)),
                Part(f"leg{i}{side}_tibia", f"Leg{i}{side}_knee", (tx, 7.25, z - 0.75), (tw, 1.5, 1.5),
                     segment(41 + i)),
                Part(f"leg{i}{side}_tarsus", f"Leg{i}{side}_foot", (px, 7.5, z - 0.5), (pw, 1, 1),
                     segment(51 + i, tip=True)),
            ]
    return p
