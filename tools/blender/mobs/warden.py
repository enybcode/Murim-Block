"""Gardien des tombes (the mortal trial's last dead; design sheet: docs/mobs/gardien-tombes.png).

A zombie gone wrong ("a bit mutant, do not change too much"): grey-green rotting skin with sores, red eyes glowing in
sunken sockets, a torn mouth and a swollen cheek, a few strands of hair left. A torn shirt over bare ribs, a hunch
with bone nubs and three bone spikes down the spine, a bone knot and spur on the right shoulder, a swollen right
forearm, clawed hands and bare clawed feet under ragged trousers. Drawn 1.6 times as big in game (the entity's scale).
"""

from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#2f3d2e", "#455a42", "#5d7559", "#71886a", "#879c7d")
SORE = hexes("#3d1a1a", "#5a2626", "#7c3d3a", "#94524b", "#a8665c")
BONE = hexes("#7d755e", "#a99f80", "#c4bb9c", "#d6cdb0", "#e8e1c8")
SHIRT = hexes("#1b1e28", "#262a37", "#34394a", "#454b5f", "#585f75")
PANTS = hexes("#231f30", "#332e45", "#45405a", "#575170", "#6a6484")
HAIR = hexes("#121014", "#1d1a20", "#2a262d", "#39343d", "#4a444f")
EYE, EYE_CORE = (255, 59, 47), (255, 208, 160)
SOCKET, MOUTH, NAIL = (26, 20, 20), (36, 21, 21), (59, 51, 40)


def side_x(face, x, w):
    """How far toward the front a texel of a side face is (0: back), so both sides paint alike."""
    return x if face == "right" else w - 1 - x


def flesh(face, x, y, w, h, salt, offset=0):
    """Rotting skin: blotches and a few sores."""
    n = noise(x, y, salt + 7)
    if face not in ("top", "bottom") and n < 0.025:
        return SORE[2]
    if face not in ("top", "bottom") and n < 0.04:
        return SORE[1]
    return shade(SKIN, face, x, y, h, salt, 0.14, offset)


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "SSSSSSSS",
    "SLSSSSLS",
    "DKKSSKKD",
    "DOOSSOOD",
    "SOESSEOS",
    "SDSLSDDW",
    "DSTMTMSW",
    "DDMMMDDS",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "D": SKIN[1], "K": SKIN[0], "O": SOCKET, "E": EYE, "M": MOUTH,
                "T": BONE[3], "W": SORE[2]}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        # The ear, half rotted away.
        if y in (3, 4) and fx == 3:
            return SKIN[0]
        # The torn mouth runs back along the right cheek.
        if face == "right" and y == 6 and fx >= 5:
            return MOUTH
    return flesh(face, x, y, w, h, 11)


def head_glow(face, x, y, w, h):
    if face == "front" and y == 4 and x in (2, 5):
        return EYE_CORE + (255,)
    if face == "front" and y == 3 and x in (2, 5):
        return EYE + (200,)
    return None


def hair(face, x, y, w, h):
    """A few greasy strands left on the scalp."""
    if face == "bottom":
        return None
    if face == "top":
        keep = noise(x, y, 13) < 0.45
    elif face == "front":
        keep = y == 0 and x in (1, 2, 5)
    else:
        keep = y == 0 and noise(x, y, 17) < 0.35
    return shade(HAIR, face, x, y, h, 19, 0.1) if keep else None


def brow(face, x, y, w, h):
    """A heavy, bony brow ridge over the sockets."""
    if face == "bottom":
        return SKIN[0]
    if face == "front":
        return BONE[1] if x in (2, 5) else SKIN[1]
    return shade(SKIN, face, x, y, h, 21, offset=-1)


def cheek(face, x, y, w, h):
    """The swollen left cheek, with a sore."""
    if face in ("front", "left") and (x + y) % 3 == 0:
        return SORE[3]
    return shade(SORE, face, x, y, h, 23, 0.15, offset=-1) if noise(x, y, 25) < 0.4 \
        else shade(SKIN, face, x, y, h, 27, offset=1)


def jaw(face, x, y, w, h):
    """The jaw hanging a little loose, a row of teeth."""
    if face == "top":
        return MOUTH
    if face == "front":
        return BONE[3] if y == 0 and x % 2 == 0 else SKIN[1]
    return shade(SKIN, face, x, y, h, 29, offset=-1)


# Body ---------------------------------------------------------------------------------------------------------

# The shirt's tear across the chest (front face, 8 x 12): the ribs show through it.
TEAR = {(x, y) for y in range(2, 9) for x in range(1, 7) if abs(x - 3.5) + abs(y - 5) * 0.6 < 3.6}


def body(face, x, y, w, h):
    if face == "front" and (x, y) in TEAR:
        # Bare ribs between strips of dark flesh.
        if y in (3, 5, 7) and x not in (3, 4):
            return BONE[3] if y != 7 else BONE[2]
        if x in (3, 4) and y < 8:
            return BONE[1]
        return SKIN[0] if noise(x, y, 31) < 0.5 else SORE[1]
    if face == "back" and x in (3, 4):
        # The spine's knobs.
        return BONE[2] if y % 2 == 0 else SKIN[1]
    return flesh(face, x, y, w, h, 33)


def shirt(face, x, y, w, h):
    """The torn shirt: open over the ribs, ragged at the hem, a rip down the left side."""
    if face in ("top", "bottom"):
        return None
    if face == "front" and (x, y) in TEAR:
        return None
    if face == "front" and (x, y) in {(0, 5), (7, 3), (7, 4)}:
        return None
    if y == h - 1 and noise(x, ord(face[0]), 35) < 0.5:
        return None
    if face == "left" and y > 4 and side_x(face, x, w) == 2:
        return None
    if face == "back" and y in range(0, 6) and x in (3, 4):
        return None
    colour = shade(SHIRT, face, x, y, h, 37, 0.12)
    return SHIRT[1] if noise(x, y, 39) < 0.12 else colour


def rib(face, x, y, w, h):
    return BONE[4] if face == "top" else BONE[3] if face == "front" else BONE[1]


def hunch(face, x, y, w, h):
    """The hunch over the shoulder blades: stretched skin, bone nubs breaking through."""
    if face in ("back", "top") and (x, y) in {(1, 1), (4, 0), (2, 3), (5, 2)}:
        return BONE[3]
    return flesh(face, x, y, w, h, 41, offset=-1 if face == "bottom" else 0)


def bone(face, x, y, w, h):
    """A growth of bone, paler toward its tip (the top)."""
    if face == "top":
        return BONE[4]
    if face == "bottom":
        return BONE[0]
    level = 3 if y < h / 3 else 2 if y < 2 * h / 3 else 1
    return BONE[level + (1 if face == "front" and y == 0 else 0) - (1 if face == "back" else 0)]


def knot(face, x, y, w, h):
    """The bone knot on the right shoulder, grown through the skin."""
    if face == "bottom":
        return SKIN[1]
    if y == h - 1 and face != "top":
        return SORE[1] if x % 2 else SKIN[1]
    return BONE[3] if (x + y) % 3 else BONE[1]


# Arms ---------------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    if face == "bottom":
        return SKIN[1]
    # Clawed fingertips.
    if y == h - 1 and face == "front" and x % 2 == 0:
        return NAIL
    return flesh(face, x, y, w, h, 51)


def sleeve(face, x, y, w, h):
    """The left sleeve, torn off above the elbow."""
    if face == "bottom":
        return None
    if y == h - 1 and noise(x, ord(face[0]), 53) < 0.55:
        return None
    return shade(SHIRT, face, x, y, h, 55, 0.12)


def swelling(face, x, y, w, h):
    """The swollen right forearm: tight skin, veins and sores."""
    if face in ("top", "bottom"):
        return SKIN[1]
    if (x + 2 * y) % 7 == 0:
        return SORE[2]
    if x == (y + 1) % w and face != "back":
        return SKIN[0]
    return flesh(face, x, y, w, h, 57, offset=1 if y == 0 else 0)


def claw(face, x, y, w, h):
    return BONE[2] if face in ("front", "top") else NAIL if face == "bottom" else BONE[1]


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(face, x, y, w, h):
    if face == "bottom":
        return SKIN[0]
    if face == "top":
        return PANTS[2]
    # Ragged trousers down to mid-shin, bare skin below.
    edge = 5 + (1 if noise(x, ord(face[0]), 61) < 0.5 else 0)
    if y < edge:
        return shade(PANTS, face, x, y, h, 63, 0.12)
    if y == h - 1 and face == "front" and x % 2 == 0:
        return NAIL
    return flesh(face, x, y, w, h, 65)


def rag(face, x, y, w, h):
    """A strip of trouser hanging from the knee."""
    if face in ("top", "bottom"):
        return None
    if y == h - 1 and x % 2:
        return None
    return shade(PANTS, face, x, y, h, 67, 0.15, offset=-1)


def parts():
    spine = [
        # A bone spike: its bottom on the back, leaning back as it rises (less so the lower it grows).
        Part(f"spike_{i}", joint, (-0.5, y, -4.4 + i * 0.6), (1, 4 - i * 0.5, 1), bone, tilt=(lean, y, -4.4 + i * 0.6))
        for i, (joint, y, lean) in enumerate((("Chest", 21, 35), ("Chest", 18, 28), ("Torso", 15, 20)))
    ]
    return [
        # Head: sunken red eyes under a bony brow, a swollen left cheek, the jaw hanging loose, a few strands.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("brow", "Head", (-4, 28.6, 3.5), (8, 1, 1), brow),
        Part("cheek", "Head", (3.5, 25, 0.5), (1, 2, 3), cheek),
        Part("jaw", "Head", (-2.5, 23.4, 1.8), (5, 1, 2.6), jaw),
        Part("hair", "Head", (-4, 30, -4), (8, 2, 8), hair, inflate=0.25),
        # Trunk: skin with the ribs bare under a torn shirt; the hunch and three spikes down the spine.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("shirt", "trunk", (-4, 12, -2), (8, 12, 4), shirt, inflate=0.35),
        *[Part(f"rib_{i}", "Chest", (side, 17.5 + 2 * i, 1.8), (2, 0.8, 0.8), rib)
          for i in range(3) for side in (-3.2, 1.2)],
        Part("hunch", "Chest", (-3, 19, -3.8), (6, 4, 2.2), hunch),
        *spine,
        # The right shoulder's bone knot and its spur, short and out on the shoulder's edge to keep clear of the head.
        Part("knot", "Arm_R", (-7.5, 22.6, -1.5), (3, 3, 3), knot),
        Part("spur", "Arm_R", (-8.1, 25.4, -0.7), (1.2, 2.2, 1.2), bone, tilt=(20, 25.4, 0)),
        # Arms: the right forearm swollen, the left sleeve torn, claws on both hands.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_paint),
        Part("swell_r", "Hand_R", (-8.6, 13.4, -2.6), (5, 4, 5), swelling),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_l", "Arm_L", (4, 18, -2), (4, 6, 4), sleeve, inflate=0.35),
        *[Part(f"claw_{side}{i}", f"Hand_{side}", (x0 + i * 1.5, 10.6, 1.0), (1, 1.6, 1), claw)
          for side, x0 in (("R", -7.6), ("L", 4.6)) for i in range(3)],
        # Legs: ragged trousers to mid-shin, a rag hanging from the right knee, bare clawed feet.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint),
        Part("rag_r", "Leg_R", (-3.4, 3, 2.05), (2, 3, 0.6), rag),
        *[Part(f"toe_{side}{i}", f"Leg_{side}", (x0 + i * 1.4, 0, 2.0), (1, 1, 1.2), claw)
          for side, x0 in (("R", -3.6), ("L", 0.2)) for i in range(3)],
    ]
