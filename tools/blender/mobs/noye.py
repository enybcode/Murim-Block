"""Disciple noyé (the drowned's new look; design sheet: docs/mobs/disciple-noye.png).

A disciple who drowned in a frozen lake during a trial, kept by the cold water's Qi: bloated blue-green skin, long
hair loose down the back and a wet lock over one eye, a sect robe soaked dark below the chest, kelp draped on the
shoulder and knotted for a belt, shells grown into the cloth, one shoe lost; one cyan eye glows through the hair.
"""

from disciple import side_x
from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#33504e", "#4a6e6b", "#5f8885", "#6f9a96", "#8cb6b1")
ROBE = hexes("#1c2c3e", "#26394d", "#2f4a63", "#3f5f7e", "#567898")
HAIR = hexes("#0c1413", "#152120", "#1f2e2b", "#2c403c", "#3a524d")
KELP = hexes("#1e3415", "#2c4a1f", "#3a5e2a", "#4f7a3a", "#6a9850")
SHELL = hexes("#8a8272", "#a89f8c", "#c0b8a8", "#d8d0c0", "#ece6d8")
SHOE = hexes("#141418", "#1e1e24", "#2a2a30", "#3a3a42", "#4a4a54")
EYE = (127, 232, 255)
SOCKET, MOUTH, NAIL = (14, 24, 24), (22, 34, 34), (30, 40, 40)


def soaked(face, x, y, h, salt, top_y, bottom_y):
    """Robe cloth soaked darker below the chest (model height 19), with drips running down."""
    model_y = top_y - (top_y - bottom_y) * (y + 0.5) / max(h, 1)
    offset = -1 if model_y < 19 else 0
    if model_y < 19 and noise(x, 0, salt) < 0.25:
        offset -= 1
    return shade(ROBE, face, x, y, h, salt, 0.1, offset)


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "HHHHHHHH",
    "HHSLLLSH",
    "HHSLLLSH",
    "HHKSSKKH",
    "HHSSSEOH",
    "SHSBLSSS",
    "DHSMMMSD",
    "DDSSSSDD",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "B": SKIN[4], "D": SKIN[1], "K": SKIN[0], "H": HAIR[2], "O": SOCKET,
                "E": EYE, "M": MOUTH}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if fx < 5 or y < 2:
            return shade(HAIR, face, x, y, h, 11)
    if face in ("back", "top"):
        return shade(HAIR, face, x, y, h, 13)
    return shade(SKIN, face, x, y, h, 15, 0.12)


def head_glow(face, x, y, w, h):
    return EYE + (255,) if face == "front" and y == 4 and x == 5 else None


def hair_hang(face, x, y, w, h):
    """Wet hair hanging in strands; the ends split into points."""
    if y == h - 1 and x % 2 == 1:
        return None
    if face == "bottom":
        return HAIR[0]
    return HAIR[3] if (x * 5 + y) % 6 == 0 else shade(HAIR, face, x, y, h, 17, 0.1)


def kelp_strand(face, x, y, w, h):
    if y == h - 1 and face != "top":
        return KELP[4]
    return KELP[3] if (y + x) % 2 == 0 else KELP[2]


# Body ---------------------------------------------------------------------------------------------------------

def body(face, x, y, w, h):
    if face == "front":
        if y == 0 and x in (3, 4):
            return SKIN[1]
        if y in (1, 2) and x in (3, 4):
            return ROBE[0]
    if face == "back":
        # The sect's ring, half washed out.
        if (x, y) in {(3, 2), (4, 2), (2, 3), (2, 4), (3, 5)}:
            return ROBE[4]
    return soaked(face, x, y, h, 31, 24, 12)


def collar(face, x, y, w, h):
    if face == "front" and x in (3, 4):
        return None
    return ROBE[0] if face in ("top", "bottom") else shade(ROBE, face, x, y, h, 33, 0.12, -1)


def lapel(face, x, y, w, h):
    if face in ("front", "top"):
        return ROBE[2] if x == w - 1 else ROBE[1]
    return shade(ROBE, face, x, y, h, 35, offset=-2)


def kelp_belt(face, x, y, w, h):
    if face in ("top", "bottom"):
        return KELP[1]
    return KELP[3] if (x + 2 * y) % 4 == 0 else KELP[2] if (x + y) % 2 else KELP[1]


def knot(face, x, y, w, h):
    return KELP[3] if (x + y) % 2 == 0 else KELP[1]


def panel(face, x, y, w, h):
    """A soaked robe panel, its hem torn in long points."""
    if y >= h - 2 and noise(x, y, 49 + ord(face[0])) < 0.45:
        return None
    return ROBE[0] if y == 0 else soaked(face, x, y, h, 51, 12, 5)


def skirt(face, x, y, w, h):
    if face in ("top", "bottom") or (y == h - 1 and noise(x, 1, 41 + ord(face[0])) < 0.5):
        return None
    return soaked(face, x, y, h, 45, 12, 5)


def shell(face, x, y, w, h):
    if face == "front" or face == "right" or face == "left":
        return SHELL[3] if (x + y) % 2 == 0 else SHELL[2]
    return SHELL[1]


def drape(face, x, y, w, h):
    """Kelp lying over the shoulder."""
    if face == "bottom":
        return None
    return KELP[3] if (x + y) % 3 == 0 else KELP[2]


# Arms ---------------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    if y >= 9 or face == "bottom":
        if face == "bottom":
            return SKIN[1]
        return NAIL if y == h - 1 and x % 2 == 0 else shade(SKIN, face, x, y, h, 55, 0.12)
    return soaked(face, x, y, h, 53, 24, 12)


def sleeve(face, x, y, w, h):
    if face == "bottom":
        return None
    if face == "top":
        return shade(ROBE, face, x, y, h, 61)
    if y >= h - 2 and noise(x, y, 63 + ord(face[0])) < 0.45:
        return None
    return soaked(face, x, y, h, 65, 24, 15)


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(shod):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return SHOE[0] if shod else SKIN[1]
        if face == "top":
            return ROBE[1]
        if y >= 10:
            if shod:
                return shade(SHOE, face, x, y, h, 71)
            return NAIL if face == "front" and y == h - 1 and x % 2 == 0 else shade(SKIN, face, x, y, h, 73, 0.12)
        if not shod and y >= 7:
            # The trouser leg torn off above the bare foot.
            if y == 7 and noise(x, 7, 75) < 0.5:
                return ROBE[0]
            return shade(SKIN, face, x, y, h, 77, 0.12)
        return shade(ROBE, face, x, y, h, 79, 0.1, -1)
    return paint


def parts():
    return [
        # Head: the face behind the wet lock, hair hanging down the back and over the temples, kelp caught in it.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("hair_cap", "Head", (-4, 30, -4), (8, 2, 8), hair_hang, inflate=0.3),
        Part("hair_back", "Head", (-4, 19, -4.6), (8, 10, 1), hair_hang),
        Part("lock_eye", "Head", (-2.6, 24, 4.05), (2, 7, 1), hair_hang),
        Part("lock_r", "Head", (-4.7, 20, 0.5), (1, 9, 1), hair_hang),
        Part("lock_l", "Head", (3.7, 21, 1.5), (1, 8, 1), hair_hang),
        Part("kelp_hair", "Head", (3.75, 18, -1.5), (1, 9, 1), kelp_strand),
        # Trunk: the soaked robe, its collar and lapel, the kelp belt with its knot and trailing ends, a shell.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("collar", "trunk", (-4, 22, -2), (8, 2, 4), collar, inflate=0.45),
        *[Part(f"lapel_{i}", "trunk", (2.5 - i, 22 - i, 1.6), (2, 1, 1), lapel) for i in range(7)],
        Part("belt", "trunk", (-4, 14, -2), (8, 2, 4), kelp_belt, inflate=0.5),
        Part("knot", "Torso", (1, 13.5, 2.1), (2, 2, 1), knot),
        Part("kelp_1", "Torso", (1.4, 13.6, 2.95), (1, 6, 1), kelp_strand, cloth="front"),
        Part("kelp_2", "Torso", (2.4, 13.8, 2.95), (1, 4, 1), kelp_strand, cloth="front"),
        Part("panel_front", "Torso", (-1, 13.8, 2.6), (6, 8, 1), panel, cloth="front"),
        Part("panel_back", "Torso", (0, 13.8, -2.6), (7, 9, 1), panel, cloth="back"),
        Part("shell_chest", "trunk", (1, 18, 2.05), (1, 1, 1), shell),
        # Arms: whole sleeves torn at the forearm, kelp over the left shoulder, shells on the right arm.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_r", "Arm_R", (-8, 16, -2), (4, 8, 4), sleeve, inflate=0.45),
        Part("shell_1", "Arm_R", (-8.7, 19, 0), (1, 2, 2), shell),
        Part("shell_2", "Hand_R", (-8.6, 14, -1), (1, 1, 1), shell),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_paint),
        Part("sleeve_l", "Arm_L", (4, 15, -2), (4, 9, 4), sleeve, inflate=0.45),
        Part("kelp_shoulder", "Arm_L", (4, 23.4, -2), (4, 1, 4), drape, inflate=0.35),
        Part("kelp_arm", "Arm_L", (7.7, 17, 0.5), (1, 6, 1), kelp_strand),
        # Legs: the soaked skirt, one shoe, one bare foot under a torn trouser leg.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint(True)),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint(False)),
        Part("skirt_r", "Thigh_R", (-3.9, 5, -2), (4, 7, 4), skirt, inflate=0.5),
        Part("skirt_l", "Thigh_L", (-0.1, 5, -2), (4, 7, 4), skirt, inflate=0.5),
    ]
