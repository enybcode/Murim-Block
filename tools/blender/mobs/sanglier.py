"""Sanglier du culte (the zombified piglin's new look; design sheet: docs/mobs/sanglier-du-culte.png).

A warrior of the Boar clan, sworn to the demonic cult, who came back rotten from the Nether: a wide boar's head with
jowls, a snout with a gold ring, tusks, a drooping ear and a torn one, a black crest; lacquered lamellar armour with
gold trim, broken over the ribs; a red sash, pauldrons, bracers and greaves. His eyes burn like embers. The golden
sword vanilla gives him is his clan's sabre.
"""

from disciple import side_x
from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#6e3e3c", "#8e5452", "#a86c68", "#c98a86", "#e0aca6")
ROT = hexes("#3a4a2c", "#4e6338", "#6f8a5a", "#8fae7a", "#a8c696")
BONE = hexes("#8a8270", "#aaa290", "#c8c0ac", "#e8e0cc", "#f6f0e0")
LAC = hexes("#1a0a0c", "#2f1215", "#4a1f22", "#66302f", "#80403c")
GOLD = hexes("#6a4e14", "#8f6c22", "#b48a2e", "#d6a83a", "#f0cc66")
RED = hexes("#4a0c0c", "#6e1414", "#8f1c1c", "#a32020", "#c43a34")
DARK = hexes("#1e0c0c", "#2a1212", "#3a181a", "#4c2224", "#5e2e2e")
HAIR = hexes("#0e0808", "#1a1010", "#2a1a1a", "#3a2626", "#4a3232")
EYE = (255, 106, 58)
SOCKET, NOSTRIL, NAIL = (40, 14, 12), (70, 30, 28), (50, 24, 22)


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "DSSSSSSD",
    "RSLLLLSS",
    "RRKKSKKS",
    "ROESSEOS",
    "BSSSSSSS",
    "BSSSSSSD",
    "DSSSSSSD",
    "DDSSSSDD",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "D": SKIN[1], "K": SKIN[0], "R": ROT[2], "B": BONE[2], "O": SOCKET,
                "E": EYE}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        # Rot eating the right side of his head, down to the jaw bone.
        if face == "right" and 2 <= fx <= 5 and 2 <= y <= 6:
            return BONE[2] if (fx, y) in ((4, 5), (5, 5), (5, 6)) else shade(ROT, face, x, y, h, 9)
    return shade(SKIN, face, x, y, h, 11, 0.12)


def head_glow(face, x, y, w, h):
    return EYE + (255,) if face == "front" and y == 3 and x in (2, 5) else None


def jowl(face, x, y, w, h):
    return shade(SKIN, face, x, y, h, 13, 0.12, -1 if face != "top" else 0)


def snout(face, x, y, w, h):
    if face == "front":
        if y == 1 and x in (0, 3):
            return NOSTRIL
        return SKIN[4] if y == 0 else SKIN[3]
    if face == "top":
        return SKIN[4]
    return shade(SKIN, face, x, y, h, 15, offset=1)


def ring(face, x, y, w, h):
    return GOLD[4] if face in ("front", "top") else GOLD[2]


def tusk(face, x, y, w, h):
    return BONE[4] if y == 0 else BONE[3] if face in ("front", "right", "left") else BONE[1]


def ear(torn):
    def paint(face, x, y, w, h):
        if torn and face in ("front", "back") and (x, y) in ((w - 1, 0), (w - 1, 1)):
            return None
        if face == "front" and not torn and y < h - 1:
            return SKIN[4] if x == 0 else SKIN[3]
        return shade(SKIN, face, x, y, h, 17, 0.1, -1)
    return paint


def crest(face, x, y, w, h):
    if face == "top":
        return HAIR[3] if (x + y) % 2 == 0 else HAIR[2]
    return shade(HAIR, face, x, y, h, 19, 0.15)


# Body ---------------------------------------------------------------------------------------------------------

def lamellar(face, x, y, w, h, salt):
    """Lacquered plates laced in rows, a dark lace line between rows, the edges worn bright."""
    if y % 3 == 2:
        return LAC[1]
    if x % 2 == 0 and y % 3 == 0:
        return LAC[3]
    return shade(LAC, face, x, y, h, salt, 0.1)


def body(face, x, y, w, h):
    if face == "front":
        if y == 0:
            return GOLD[3]
        # The armour is broken over his right ribs: rot and bone show through.
        if x <= 3 and 2 <= y <= 7:
            if x >= 1 and y in (3, 5, 7):
                return BONE[3] if x < 3 else BONE[2]
            return ROT[1] if (x + y) % 3 == 0 else ROT[2]
    if face == "back" and y == 0:
        return GOLD[2]
    return lamellar(face, x, y, w, h, 31)


def plate(face, x, y, w, h):
    """The chest plate left on his left breast, trimmed in gold."""
    if face == "front" and (x == 0 or y == 0):
        return GOLD[3]
    return lamellar(face, x, y, w, h, 33)


def sash(face, x, y, w, h):
    if face in ("top", "bottom"):
        return RED[1]
    return RED[3] if (x + y) % 4 == 0 else shade(RED, face, x, y, h, 35, 0.1)


def sash_end(face, x, y, w, h):
    return RED[4] if y == 0 else RED[1] if y == h - 1 else RED[3] if y % 2 else RED[2]


def tasset(face, x, y, w, h):
    """An armour skirt plate hanging from the sash: lacquer, a gold hem."""
    if y == h - 1:
        return GOLD[2] if face in ("front", "back") else GOLD[1]
    return lamellar(face, x, y, w, h, 37)


def pauldron(face, x, y, w, h):
    if face == "bottom":
        return LAC[0]
    if y == h - 1 and face != "top":
        return GOLD[3]
    return lamellar(face, x, y, w, h, 39) if face != "top" else LAC[3]


# Arms ---------------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    if face == "bottom":
        return SKIN[1]
    if y >= 6 and y < 9:
        return shade(DARK, face, x, y, h, 41)
    if (x * 3 + y * 5) % 11 == 0 and y < 6:
        return ROT[2]
    return NAIL if y == h - 1 and face != "top" and x % 2 == 0 else shade(SKIN, face, x, y, h, 43, 0.12)


def bracer(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    return GOLD[3] if y == 0 else shade(DARK, face, x, y, h, 45, 0.1)


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(face, x, y, w, h):
    if face == "bottom":
        return DARK[0]
    if face == "top":
        return DARK[2]
    if y >= 10:
        return shade(DARK, face, x, y, h, 47, offset=-1)
    if face == "front" and y in (3, 4) and x in (1, 2):
        return ROT[2]
    return shade(DARK, face, x, y, h, 49, 0.1)


def greave(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    if y == 0:
        return GOLD[3]
    return lamellar(face, x, y, w, h, 51)


def parts():
    return [
        # Head: the boar's head widened by its jowls, snout and ring, tusks, ears, crest.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("jowl_r", "Head", (-5, 24, -2), (1, 4, 5), jowl),
        Part("jowl_l", "Head", (4, 24, -2), (1, 4, 5), jowl),
        Part("snout", "Head", (-2, 24.5, 4), (4, 3, 2), snout),
        Part("ring", "Head", (-0.5, 23.6, 5.5), (1, 1, 1), ring),
        Part("tusk_r", "Head", (-2.8, 24, 4.2), (1, 2, 1), tusk),
        Part("tusk_l", "Head", (1.8, 24, 4.2), (1, 2, 1), tusk),
        Part("ear_r", "Head", (-6, 27.5, -1), (2, 3, 1), ear(False)),
        Part("ear_l", "Head", (4, 29, -1), (2, 3, 1), ear(True)),
        Part("crest", "Head", (-1, 32, -4), (2, 2, 7), crest),
        # Trunk: the lamellar armour broken over the ribs, the plate left on his left breast, the red sash and its
        # end, armour skirt plates front and back.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("plate", "trunk", (0.5, 15, 2.05), (3, 7, 1), plate),
        Part("sash", "trunk", (-4, 14, -2), (8, 2, 4), sash, inflate=0.5),
        Part("sash_end", "Torso", (2.5, 13.6, 2.95), (1, 5, 1), sash_end, cloth="front"),
        Part("tasset_front", "Torso", (-1, 13.8, 2.6), (6, 5, 1), tasset, cloth="front"),
        Part("tasset_back", "Torso", (0, 13.8, -2.6), (7, 6, 1), tasset, cloth="back"),
        # Arms: pauldrons, rotting skin, gold-trimmed bracers.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_paint),
        Part("pauldron_r", "Arm_R", (-8.5, 21, -2.5), (5, 3, 5), pauldron),
        Part("bracer_r", "Hand_R", (-8, 13, -2), (4, 3, 4), bracer, inflate=0.35),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_paint),
        Part("pauldron_l", "Arm_L", (3.5, 21, -2.5), (5, 3, 5), pauldron),
        Part("bracer_l", "Hand_L", (4, 13, -2), (4, 3, 4), bracer, inflate=0.35),
        # Legs: dark trousers, lacquered greaves.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint),
        Part("greave_r", "Leg_R", (-3.9, 2, -2), (4, 4, 4), greave, inflate=0.35),
        Part("greave_l", "Leg_L", (-0.1, 2, -2), (4, 4, 4), greave, inflate=0.35),
    ]
