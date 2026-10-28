"""Paysan possédé (the zombie villager's new look; design sheet: docs/mobs/paysan-possede.png).

A villager with no technique, killed by the dead that walk at night. A passing monk stuck a yellow talisman on his
forehead to seal the body, but the seal is too weak. The villager's big nose, a knotted headband, a patched hemp
tunic crossed over the chest, a farmer's apron, rolled sleeves and trousers, straw sandals; dull eyes, little Qi.
"""

from disciple import side_x
from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#3e4c36", "#5a6a4c", "#728560", "#8fa27c", "#a8b894")
TUNIC = hexes("#4e4434", "#665a46", "#7f6e52", "#9c8a6a", "#b8a888")
APRON = hexes("#2e4020", "#405a2c", "#55703a", "#6a8a4a", "#86a660")
TROUSER = hexes("#3a3228", "#4e4438", "#6e6252", "#8a7c68", "#a29480")
BAND = hexes("#2a2018", "#3e3026", "#5a4a3a", "#74624e", "#8e7c66")
HAIR = hexes("#121010", "#1e1a18", "#2b2624", "#3a3430", "#4a433e")
PAPER = hexes("#9a8226", "#c2a636", "#d8bc46", "#e8cf55", "#f6e27a")
STRAW = hexes("#5e4620", "#7c5e2c", "#9a7838", "#c29a52", "#d8b46a")
INK, INK_DARK = (179, 32, 32), (120, 20, 20)
EYE = (150, 190, 172)
SOCKET, MOUTH, NAIL = (30, 38, 26), (38, 30, 26), (44, 40, 32)


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "HHHHHHHH",
    "HSLLLLSH",
    "SSLLLLSS",
    "SKKSSKKS",
    "SEOSSOES",
    "SDSSSSDS",
    "DSMMMMSD",
    "DDSSSSDD",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "D": SKIN[1], "K": SKIN[0], "H": HAIR[2], "O": SOCKET, "E": EYE,
                "M": MOUTH}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if y < 1 or (fx < 2 and y < 4):
            return shade(HAIR, face, x, y, h, 11)
        if y in (3, 4) and fx in (3, 4):
            return SKIN[1] if fx == 3 else SKIN[0]
    if face in ("back", "top"):
        return shade(HAIR, face, x, y, h, 13)
    return shade(SKIN, face, x, y, h, 15, 0.12)


def head_glow(face, x, y, w, h):
    """The dull eyes: a faint glow, little Qi left in him."""
    return EYE + (150,) if face == "front" and y == 4 and x in (1, 6) else None


def nose(face, x, y, w, h):
    if face == "bottom":
        return SKIN[0]
    if face == "top":
        return SKIN[3]
    if face == "front":
        return SKIN[3] if y == 0 else SKIN[1] if y == h - 1 else SKIN[2]
    return shade(SKIN, face, x, y, h, 17, offset=-1)


def headband(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    return BAND[3] if (x + y) % 4 == 0 else shade(BAND, face, x, y, h, 19, 0.1)


def band_end(face, x, y, w, h):
    return BAND[3] if y == 0 else BAND[1] if y == h - 1 else BAND[2]


def talisman(face, x, y, w, h):
    """A yellow paper seal: a red stroke down the middle, two bars across, a smudged seal at the foot."""
    if face != "front":
        return PAPER[1] if face != "back" else PAPER[2]
    if x == 1 and 0 < y < h - 1:
        return INK
    if y in (1, 3) and x in (0, 2) and noise(x, y, 21) < 0.8:
        return INK_DARK
    if y == h - 1:
        return INK_DARK if x == 1 else PAPER[2]
    return PAPER[4] if y == 0 else PAPER[3]


# Body ---------------------------------------------------------------------------------------------------------

def body(face, x, y, w, h):
    if face == "front":
        if y == 0 and x in (3, 4):
            return SKIN[1]
        # The tunic wraps from his right shoulder down to the left: a darker fold along the edge.
        if x == 7 - y // 2 and y < 8:
            return TUNIC[1]
        # A patch sewn over a tear.
        if x in (1, 2) and y in (3, 4):
            return TUNIC[4] if (x + y) % 2 else TUNIC[3]
    return shade(TUNIC, face, x, y, h, 31, 0.1)


def collar(face, x, y, w, h):
    if face == "front" and x in (3, 4):
        return None
    return TUNIC[1] if face in ("top", "bottom") else shade(TUNIC, face, x, y, h, 33, offset=-1)


def string(face, x, y, w, h):
    return None if face in ("top", "bottom") else BAND[2]


def apron(face, x, y, w, h):
    """The farmer's apron: a pocket stitched on, earth rubbed into the lower edge."""
    if face == "front" and 1 <= x <= 4 and y in (2, 3):
        return APRON[1] if y == 2 else APRON[2]
    if y >= h - 2:
        return shade(APRON, face, x, y, h, 35, 0.15, -1)
    return shade(APRON, face, x, y, h, 37, 0.1)


def apron_hang(face, x, y, w, h):
    if y == h - 1 and noise(x, 2, 39) < 0.3:
        return None
    return shade(APRON, face, x, y, h, 41, 0.12, -1 if y >= h - 2 else 0)


def back_hang(face, x, y, w, h):
    if y == h - 1 and noise(x, 3, 43) < 0.4:
        return None
    return shade(TUNIC, face, x, y, h, 45, 0.1)


def pouch(face, x, y, w, h):
    if y == 0 and face != "bottom":
        return BAND[2]
    return shade(hexes("#8a7c58", "#a8986e", "#c2b288", "#d8cba8", "#ece2c4"), face, x, y, h, 47)


# Arms ---------------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    if y < 4:
        return shade(TUNIC, face, x, y, h, 51)
    if face == "bottom":
        return SKIN[1]
    return NAIL if y == h - 1 and face != "top" and x % 2 == 0 else shade(SKIN, face, x, y, h, 53, 0.12)


def sleeve(face, x, y, w, h):
    if face == "bottom":
        return None
    return shade(TUNIC, face, x, y, h, 55, 0.1, 1 if face == "top" else 0)


def roll(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    return TUNIC[4] if (x + y) % 2 == 0 else TUNIC[3]


def trouser_roll(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    return TROUSER[4] if (x + y) % 2 == 0 else TROUSER[3]


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(face, x, y, w, h):
    if face == "bottom":
        return STRAW[1]
    if face == "top":
        return TROUSER[2]
    if y >= 7:
        if y == h - 1:
            return STRAW[2]
        return NAIL if face == "front" and y == h - 2 and x % 2 == 0 else shade(SKIN, face, x, y, h, 61, 0.12)
    return shade(TROUSER, face, x, y, h, 63, 0.1)


def strap(face, x, y, w, h):
    return None if face in ("top", "bottom") else STRAW[3] if x % 2 == 0 else STRAW[2]


def sole(face, x, y, w, h):
    return STRAW[3] if face == "top" else STRAW[1]


def parts():
    return [
        # Head: the face, the villager's big nose, short hair, the knotted headband and its ends, the talisman.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("nose", "Head", (-1, 24, 4), (2, 4, 2), nose),
        Part("hair", "Head", (-4, 31, -4), (8, 1, 8), lambda f, x, y, w, h: shade(HAIR, f, x, y, h, 23), inflate=0.3),
        Part("headband", "Head", (-4, 29.5, -4), (8, 1, 8), headband, inflate=0.4),
        Part("band_knot", "Head", (2.5, 29.3, -4.9), (2, 1, 1), band_end),
        Part("band_end_1", "Head", (3, 26.5, -5.2), (1, 3, 1), band_end),
        Part("band_end_2", "Head", (2, 27.2, -5.0), (1, 2, 1), band_end),
        Part("talisman", "Head", (-1.5, 27, 4.1), (3, 5, 1), talisman),
        # Trunk: the tunic crossed over the chest with its patch, the apron and its string, a rice pouch at the hip.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("collar", "trunk", (-4, 22, -2), (8, 2, 4), collar, inflate=0.4),
        Part("apron_top", "trunk", (-3, 14, 2.05), (6, 6, 1), apron),
        Part("apron_string", "trunk", (-4, 19.5, -2), (8, 1, 4), string, inflate=0.35),
        Part("apron_front", "Torso", (0, 14, 2.65), (6, 7, 1), apron_hang, cloth="front"),
        Part("tunic_back", "Torso", (0, 13.8, -2.6), (7, 6, 1), back_hang, cloth="back"),
        Part("pouch", "Torso", (-5.4, 10, -1), (2, 3, 2), pouch),
        # Arms: sleeves rolled to the elbow, bare forearms.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_r", "Arm_R", (-8, 18, -2), (4, 6, 4), sleeve, inflate=0.4),
        Part("roll_r", "Arm_R", (-8, 17, -2), (4, 1, 4), roll, inflate=0.6),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_l", "Arm_L", (4, 18, -2), (4, 6, 4), sleeve, inflate=0.4),
        Part("roll_l", "Arm_L", (4, 17, -2), (4, 1, 4), roll, inflate=0.6),
        # Legs: trousers rolled to the knee, bare shins, straw sandals.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint),
        Part("troll_r", "Thigh_R", (-3.9, 5, -2), (4, 1, 4), trouser_roll, inflate=0.45),
        Part("troll_l", "Thigh_L", (-0.1, 5, -2), (4, 1, 4), trouser_roll, inflate=0.45),
        Part("strap_r", "Leg_R", (-3.9, 1, -2), (4, 1, 4), strap, inflate=0.3),
        Part("strap_l", "Leg_L", (-0.1, 1, -2), (4, 1, 4), strap, inflate=0.3),
        Part("sole_r", "Leg_R", (-3.9, 0, -2), (4, 1, 5), sole, inflate=0.3),
        Part("sole_l", "Leg_L", (-0.1, 0, -2), (4, 1, 5), sole, inflate=0.3),
    ]
