"""Disciple des sables (the husk's new look; design sheet: docs/mobs/disciple-sables.png).

A disciple of a desert sect who died of thirst crossing the sands: the sun dried his flesh, so he no longer burns. A
split bamboo hat with a wide brim, a veil over the mouth, a faded ochre robe crossed over the chest, a rope belt with
an empty, cracked gourd at the hip, forearms wrapped like a mummy's, wrapped shins and straw sandals; amber eyes.
"""

from disciple import side_x
from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#4a3a26", "#6b5638", "#8a7048", "#a88c62", "#c2a87e")
ROBE = hexes("#6e5226", "#8f6c34", "#ad8844", "#c9a25a", "#dcbc7a")
WRAP = hexes("#7a6c4c", "#9a8a66", "#b8a57a", "#ddcc9e", "#ece0bc")
STRAW = hexes("#5e4620", "#7c5e2c", "#9a7838", "#c29a52", "#d8b46a")
TROUSER = hexes("#2e261e", "#42382c", "#5a4c3c", "#72624e", "#8a7860")
ROPE = hexes("#2e1c10", "#432a18", "#5b3a22", "#6b4a2a", "#8a6440")
GOURD = hexes("#5a3e1e", "#7c5a2e", "#9c723c", "#b88a4a", "#d0a462")
HAIR = hexes("#1a140e", "#2a2018", "#3a2e22", "#4c3e30", "#5e4e3e")
EYE = (255, 201, 74)
SOCKET, NAIL = (30, 22, 14), (40, 30, 20)


def robe_dirt(ramp, face, x, y, h, salt, top_y, bottom_y):
    """Sun-faded cloth: paler on the shoulders, sandier toward the hem (model heights top_y..bottom_y)."""
    model_y = top_y - (top_y - bottom_y) * (y + 0.5) / max(h, 1)
    offset = 1 if model_y > 21 and noise(x, y, salt + 3) < 0.5 else -1 if model_y < 4 and noise(x, y, salt) < 0.5 else 0
    return shade(ramp, face, x, y, h, salt, 0.12, offset)


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "DDDDDDDD",
    "HSLLLLSH",
    "HSLLLLSH",
    "HKKSSKKH",
    "HOESSEOH",
    "SDSLLSDS",
    "DSSSSSSD",
    "DDSSSSDD",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "D": SKIN[1], "K": SKIN[0], "H": HAIR[2], "O": SOCKET, "E": EYE}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if fx < 4 or y == 0:
            return shade(HAIR, face, x, y, h, 11)
        if y in (3, 4) and fx in (4, 5):
            return SKIN[1] if fx == 4 else SKIN[0]
    if face in ("back", "top"):
        return shade(HAIR, face, x, y, h, 13)
    return shade(SKIN, face, x, y, h, 15, 0.12)


def head_glow(face, x, y, w, h):
    return EYE + (255,) if face == "front" and y == 4 and x in (2, 5) else None


def veil(face, x, y, w, h):
    if face == "top":
        return WRAP[2]
    return WRAP[1] if y == 1 and face != "bottom" else shade(WRAP, face, x, y, h, 17, 0.1)


def veil_tail(face, x, y, w, h):
    return WRAP[3] if y == 0 else WRAP[1] if y == h - 1 else WRAP[2]


def brim(face, x, y, w, h):
    """The hat's wide brim, woven in rings, with a piece split out of its front corner."""
    if face == "top" and x >= w - 4 and y >= h - 3 and x - y >= w - h - 1:
        return None
    if face in ("front", "right") and x >= w - 3 and face == "front":
        return None
    if face == "bottom":
        return STRAW[0] if (x + y) % 2 else STRAW[1]
    if face == "top":
        ring = max(abs(x - w / 2 + 0.5), abs(y - h / 2 + 0.5))
        return STRAW[4] if int(ring) % 2 == 0 and noise(x, y, 19) < 0.7 else STRAW[3]
    return STRAW[2]


def crown(level):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return STRAW[0]
        if level == 1 and face != "top":
            return ROPE[2]
        return shade(STRAW, face, x, y, h, 21 + level, 0.12, 1 if face == "top" else 0)
    return paint


# Body ---------------------------------------------------------------------------------------------------------

def body(face, x, y, w, h):
    if face == "front":
        if y == 0 and x in (3, 4):
            return SKIN[1]
        if y in (1, 2) and x in (3, 4):
            return ROBE[1]
        # Sand caked in the folds.
        if (x, y) in ((1, 9), (6, 10), (2, 11)):
            return STRAW[3]
    if face == "back":
        # A sun, his desert sect's mark, bleached almost white.
        sun = {(3, 3), (4, 3), (3, 4), (4, 4), (2, 2), (5, 2), (2, 5), (5, 5)}
        if (x, y) in sun:
            return ROBE[4]
    return robe_dirt(ROBE, face, x, y, h, 31, 24, 12)


def collar(face, x, y, w, h):
    if face == "front" and x in (3, 4):
        return None
    return ROBE[1] if face in ("top", "bottom") else shade(ROBE, face, x, y, h, 33, 0.12, -1)


def lapel(face, x, y, w, h):
    if face in ("front", "top"):
        return ROBE[2] if x == w - 1 else ROBE[1]
    return shade(ROBE, face, x, y, h, 35, offset=-2)


def belt(face, x, y, w, h):
    if face in ("top", "bottom"):
        return ROPE[1]
    return ROPE[3] if (x + y) % 3 == 0 else ROPE[2] if (x + y) % 3 == 1 else ROPE[1]


def knot(face, x, y, w, h):
    return ROPE[3] if (x, y) in ((0, 0), (1, 1)) else shade(ROPE, face, x, y, h, 37)


def cord_end(face, x, y, w, h):
    return ROPE[4] if y == h - 1 else ROPE[3] if y % 2 == 0 else ROPE[2]


def panel(face, x, y, w, h):
    """A robe panel below the belt, its hem frayed and sandy."""
    if y == h - 1 and noise(x, 3, 49 + ord(face[0])) < 0.5:
        return None
    if y >= h - 2:
        return STRAW[2]
    return ROBE[1] if y == 0 else robe_dirt(ROBE, face, x, y, h, 51, 12, 5)


def skirt(face, x, y, w, h):
    if face in ("top", "bottom") or (y == h - 1 and noise(x, 1, 41 + ord(face[0])) < 0.5):
        return None
    return robe_dirt(ROBE, face, x, y, h, 45, 12, 5)


def gourd(face, x, y, w, h):
    if face == "front" and x == 1 and 0 < y < h - 1:
        return GOURD[0]  # The crack.
    if y == 1 and face not in ("top", "bottom"):
        return ROPE[2]  # Its waist, tied.
    return shade(GOURD, face, x, y, h, 39, 0.1, 1 if y == 0 else 0)


def gourd_neck(face, x, y, w, h):
    return GOURD[1] if face != "top" else GOURD[0]


# Arms ---------------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    """Robe at the shoulder, then the forearm wrapped in strips of linen down to the clawed hand."""
    if y < 5:
        return robe_dirt(ROBE, face, x, y, h, 53, 24, 12)
    if face == "bottom":
        return SKIN[1]
    if y >= 10:
        return NAIL if y == h - 1 and face != "top" and x % 2 == 0 else shade(SKIN, face, x, y, h, 55, 0.1)
    # Strips wound on a slant.
    return WRAP[1] if (x + y) % 3 == 0 else shade(WRAP, face, x, y, h, 57, 0.1)


def sleeve(face, x, y, w, h):
    if face == "bottom":
        return None
    if face == "top":
        return shade(ROBE, face, x, y, h, 61, offset=1)
    if y == h - 1 and noise(x, ord(face[0]), 63) < 0.5:
        return None
    return robe_dirt(ROBE, face, x, y, h, 65, 24, 17)


def band(ramp, salt):
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return None
        return ramp[3] if (x + salt) % 3 == 0 else shade(ramp, face, x, y, h, salt, 0.1)
    return paint


def band_tail(face, x, y, w, h):
    return WRAP[3] if y == 0 else WRAP[2] if y < h - 1 else WRAP[1]


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(face, x, y, w, h):
    if face == "bottom":
        return STRAW[1]
    if face == "top":
        return TROUSER[2]
    if y >= 10:
        # Bare toes in the sandal, the straw sole under them.
        if y == h - 1:
            return STRAW[2]
        return SKIN[2] if face == "front" else shade(SKIN, face, x, y, h, 71)
    if y >= 6:
        return WRAP[1] if (x + y) % 4 == 0 else shade(WRAP, face, x, y, h, 73, 0.1)
    return shade(TROUSER, face, x, y, h, 75)


def sole(face, x, y, w, h):
    return STRAW[3] if face == "top" else STRAW[1]


def parts():
    return [
        # Head: the face under the hat's shadow, the veil over the mouth and its loose end, the split bamboo hat.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("veil", "Head", (-4, 23.6, 3.6), (8, 3, 1), veil, inflate=0.3),
        Part("veil_tail", "Head", (3.7, 21, 2.2), (1, 5, 1), veil_tail),
        Part("hat_brim", "Head", (-7, 31.6, -7), (14, 1, 14), brim),
        Part("hat_1", "Head", (-5, 32.6, -5), (10, 1, 10), crown(1)),
        Part("hat_2", "Head", (-3.5, 33.6, -3.5), (7, 1, 7), crown(2)),
        Part("hat_3", "Head", (-2, 34.6, -2), (4, 1, 4), crown(3)),
        Part("hat_tip", "Head", (-0.5, 35.6, -0.5), (1, 1, 1), crown(4)),
        # Trunk: the faded robe, its collar and crossed lapel, the rope belt, knot and end, the cracked gourd.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("collar", "trunk", (-4, 22, -2), (8, 2, 4), collar, inflate=0.45),
        *[Part(f"lapel_{i}", "trunk", (2.5 - i, 22 - i, 1.6), (2, 1, 1), lapel) for i in range(7)],
        Part("belt", "trunk", (-4, 14, -2), (8, 2, 4), belt, inflate=0.5),
        Part("knot", "Torso", (-3, 13.5, 2.1), (2, 2, 1), knot),
        Part("rope_1", "Torso", (-2.3, 13.6, 2.95), (1, 4, 1), cord_end, cloth="front"),
        Part("panel_front", "Torso", (0, 13.8, 2.6), (6, 7, 1), panel, cloth="front"),
        Part("panel_back", "Torso", (0, 13.8, -2.6), (7, 8, 1), panel, cloth="back"),
        Part("gourd", "Torso", (4.3, 9, 0), (2, 4, 2), gourd),
        Part("gourd_neck", "Torso", (4.8, 13, 0.5), (1, 1, 1), gourd_neck),
        # Arms: short sleeves torn at the hem over forearms wound in linen, a loose strip on the right.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_r", "Arm_R", (-8, 18, -2), (4, 6, 4), sleeve, inflate=0.45),
        Part("wrap_r1", "Hand_R", (-8, 15, -2), (4, 1, 4), band(WRAP, 83), inflate=0.3),
        Part("wrap_r2", "Hand_R", (-8, 13, -2), (4, 1, 4), band(WRAP, 85), inflate=0.3),
        Part("wrap_r_tail", "Hand_R", (-8.75, 11, -0.5), (1, 3, 1), band_tail),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_l", "Arm_L", (4, 18, -2), (4, 6, 4), sleeve, inflate=0.45),
        Part("wrap_l1", "Hand_L", (4, 16, -2), (4, 1, 4), band(WRAP, 87), inflate=0.3),
        Part("wrap_l2", "Hand_L", (4, 14, -2), (4, 1, 4), band(WRAP, 89), inflate=0.3),
        # Legs: the robe's skirt, wrapped shins, straw sandals.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint),
        Part("skirt_r", "Thigh_R", (-3.9, 5, -2), (4, 7, 4), skirt, inflate=0.5),
        Part("skirt_l", "Thigh_L", (-0.1, 5, -2), (4, 7, 4), skirt, inflate=0.5),
        Part("shin_r1", "Leg_R", (-3.9, 4, -2), (4, 1, 4), band(WRAP, 91), inflate=0.28),
        Part("shin_l1", "Leg_L", (-0.1, 3, -2), (4, 1, 4), band(WRAP, 93), inflate=0.28),
        Part("strap_r", "Leg_R", (-3.9, 1, -2), (4, 1, 4), band(STRAW, 95), inflate=0.3),
        Part("strap_l", "Leg_L", (-0.1, 1, -2), (4, 1, 4), band(STRAW, 97), inflate=0.3),
        Part("sole_r", "Leg_R", (-3.9, 0, -2), (4, 1, 5), sole, inflate=0.3),
        Part("sole_l", "Leg_L", (-0.1, 0, -2), (4, 1, 5), sole, inflate=0.3),
    ]
