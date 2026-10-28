"""Disciple déchu (the zombie's new look; design sheet: docs/mobs/disciple-dechu.png).

A disciple of an orthodox sect, killed in a fight and raised by the dead Qi left in him: a sect robe crossed over
the chest with an indigo trim, a rope belt with its knot and loose ends, a jade token at the hip, the sleeve torn off
the right arm, bandaged forearm and shin, wrapped legs and cloth shoes, a fallen bun with its broken pin, a lock of
hair loose down the back, the eyes lit by dead Qi. The sect's emblem on his back is cut through by the blow that
killed him.
"""

from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#3b4743", "#55645a", "#6f8069", "#89987f", "#a3ad93")
ROBE = hexes("#6e6455", "#8f8572", "#b2a78f", "#cdc3aa", "#e3dac3")
INDIGO = hexes("#1c2236", "#2a3350", "#3a4668", "#52608a", "#6f7ca6")
HAIR = hexes("#0f0d12", "#1b181e", "#29252b", "#3b3640", "#524b59")
CORD = hexes("#2e1c12", "#432a1b", "#5b3a29", "#76523b", "#916d52")
BLOOD = hexes("#3a0e10", "#561517", "#741f1f", "#8f2c28", "#a33a31")
BANDAGE = hexes("#7f7864", "#a39a82", "#c6bda3", "#ddd5bd", "#eee8d6")
WRAP = hexes("#4b4740", "#625d54", "#7d776b", "#968f81", "#aca595")
TROUSER = hexes("#3f3a36", "#544e48", "#6b645c", "#827a70", "#988f83")
SHOE = hexes("#1a130f", "#271c16", "#36271e", "#47352a", "#5a4637")
JADE = hexes("#1f4a38", "#2f6b4f", "#3f8a63", "#5fb07f", "#8fd3a5")
EYE, EYE_DIM = (150, 255, 225), (90, 200, 175)
SOCKET, MOUTH, TOOTH, NAIL = (24, 26, 26), (30, 18, 20), (196, 188, 160), (40, 34, 30)


def flat(ramp, salt, offset=0, rough=0.1):
    return lambda face, x, y, w, h: shade(ramp, face, x, y, h, salt, rough, offset)


def side_x(face, x, w):
    """How far toward the front a texel of a side face is (0: back), so both sides paint alike."""
    return x if face == "right" else w - 1 - x


# Head ---------------------------------------------------------------------------------------------------------

FACE = [
    "LLLLLLLL",
    "SLLLLLLS",
    "SLLLLLLS",
    "SBBSSBBS",
    "DOESSEOD",
    "SDDLDDDS",
    "DSMMTMSD",
    "DDSSSSDD",
]
FACE_COLOURS = {"S": SKIN[2], "L": SKIN[3], "D": SKIN[1], "K": SKIN[0], "B": HAIR[2], "O": SOCKET, "E": EYE,
                "M": MOUTH, "T": TOOTH}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        # The ear.
        if y in (4, 5) and fx in (3, 4):
            return SKIN[1] if fx == 3 else SKIN[0]
        if y >= 6 and fx >= 5:
            return SKIN[1]
    return shade(SKIN, face, x, y, h, 11, 0.12)


def head_glow(face, x, y, w, h):
    if face == "front" and y == 4 and x in (2, 5):
        return EYE + (255,)
    return None


def hair_cap(face, x, y, w, h):
    """Covers the top of the head; a ragged fringe in front, sideburns, all of the back."""
    if face == "bottom":
        return None
    if face == "front":
        keep = y == 0 or (y == 1 and x not in (3, 4)) or (y == 2 and x in (0, 1, 2, 6, 7)) or (y == 3 and x in (0, 7))
    elif face in ("right", "left"):
        fx = side_x(face, x, w)
        keep = y < 2 or fx < 5 or (fx == 7 and y < 4)
    else:
        keep = True
    if not keep:
        return None
    colour = shade(HAIR, face, x, y, h, 21, 0.1)
    # Strands catch the light in thin lines.
    if face != "top" and (x * 7 + 3) % 4 == 0 and y in (0, 1):
        colour = HAIR[3]
    if face == "top" and x == 4:
        colour = HAIR[1]
    return colour


def hair_lock(face, x, y, w, h):
    if y == h - 1 and x != 0:
        return None
    return HAIR[3] if y == 0 else shade(HAIR, face, x, y, h, 23, 0.1)


def tuft(face, x, y, w, h):
    if face == "bottom":
        return None
    return HAIR[3] if face == "top" and (x + y) % 2 == 0 else shade(HAIR, face, x, y, h, 24, 0.1)


def bun(face, x, y, w, h):
    if face != "top" and face != "bottom" and y == h - 1:
        return CORD[2]
    return HAIR[4] if face == "top" and x == 1 else shade(HAIR, face, x, y, h, 25)


def pin(face, x, y, w, h):
    return CORD[4] if x == w - 1 and face == "front" else shade(CORD, face, x, y, h, 27, offset=1)


def brow(face, x, y, w, h):
    """The brow ridge: heavy brows over sunken eyes, skin between them."""
    if face == "front":
        return HAIR[2] if x in (0, 1, 4, 5) else SKIN[2]
    if face == "bottom":
        return SKIN[0]
    return shade(SKIN, face, x, y, h, 29, offset=-1)


def nose(face, x, y, w, h):
    if face == "bottom":
        return SKIN[0]
    if face == "front":
        return SKIN[3] if y == 0 else SKIN[2]
    return SKIN[1]


# Body ---------------------------------------------------------------------------------------------------------

def robe_dirt(ramp, face, x, y, h, salt, top_y, bottom_y):
    """Robe cloth: dirtier and darker toward the hem (model heights top_y..bottom_y)."""
    model_y = top_y - (top_y - bottom_y) * (y + 0.5) / max(h, 1)
    offset = -1 if model_y < 3 and noise(x, y, salt + 5) < 0.6 else 0
    return shade(ramp, face, x, y, h, salt, 0.1, offset)


def body(face, x, y, w, h):
    if face == "front":
        # The neck and the inner robe in the V of the collar.
        if y == 0 and x in (3, 4):
            return SKIN[1]
        if y in (1, 2) and x in (3, 4):
            return ROBE[1]
        # Dried blood over the fatal wound, on his left breast, with a run below it.
        stain = {(5, 3), (6, 3), (4, 4), (5, 4), (6, 4), (7, 4), (5, 5), (6, 5), (6, 6), (6, 7)}
        if (x, y) in stain:
            if (x, y) in ((5, 4), (6, 4)):
                return SKIN[0]
            return BLOOD[1] if (x + y) % 3 else BLOOD[2]
        if (x, y) in ((4, 3), (7, 5), (5, 6)):
            return BLOOD[3]
        if x in (2, 5) and y > 8:
            return ROBE[1]
    if face == "back":
        # The sect's emblem, a ring, cut through by the killing blow.
        ring = {(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5)}
        cut = {(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6)}
        if (x, y) in cut:
            return BLOOD[0] if (x + y) % 4 else SKIN[0]
        if (x, y) in ring:
            return INDIGO[2]
    if face in ("right", "left") and side_x(face, x, w) == 1 and y > 2:
        return ROBE[1]
    return robe_dirt(ROBE, face, x, y, h, 31, 24, 12)


def collar(face, x, y, w, h):
    if face == "front" and x in (3, 4):
        return None
    if face in ("top", "bottom"):
        return INDIGO[1]
    return INDIGO[3] if y == 0 and x % 2 == 0 else shade(INDIGO, face, x, y, h, 33, 0.15)


def lapel(face, x, y, w, h):
    if face in ("front", "top"):
        return INDIGO[3] if x == w - 1 else INDIGO[2]
    return shade(INDIGO, face, x, y, h, 35, offset=-1)


def belt(face, x, y, w, h):
    if face in ("top", "bottom"):
        return CORD[1]
    # Twisted rope.
    return CORD[3] if (x + y) % 3 == 0 else CORD[2] if (x + y) % 3 == 1 else CORD[1]


def knot(face, x, y, w, h):
    return CORD[3] if (x, y) in ((0, 0), (1, 1)) else shade(CORD, face, x, y, h, 37)


def cord_end(face, x, y, w, h):
    if y == h - 1:
        return CORD[4] if face != "bottom" else CORD[3]
    return CORD[3] if y % 2 == 0 else CORD[2]


def skirt(face, x, y, w, h):
    """The robe's skirt over a thigh: ragged hem, darker with dirt below."""
    if face == "top":
        return None
    if face == "bottom":
        return None
    if y == h - 1 and noise(x, 1, 41 + ord(face[0])) < 0.5:
        return None
    return robe_dirt(ROBE, face, x, y, h, 45, 12, 5)


def pendant(face, x, y, w, h):
    if y < 3:
        return CORD[2] if face not in ("top", "bottom") else CORD[1]
    if y < 5:
        return JADE[4] if y == 3 and face == "front" else shade(JADE, face, x, y, h, 47, offset=1)
    return BLOOD[3] if face != "bottom" else BLOOD[1]


# Arms ---------------------------------------------------------------------------------------------------------

def arm_bare(face, x, y, w, h):
    """The right arm: the sleeve torn off at the elbow, a bandaged forearm, clawed hand."""
    if y < 6:
        return robe_dirt(ROBE, face, x, y, h, 51, 24, 12)
    if face == "bottom":
        return SKIN[0] if y == 0 else SKIN[1]
    if y == h - 1 and face != "top" and x % 2 == 0:
        return NAIL
    if y in (7, 9):
        return shade(BANDAGE, face, x, y, h, 53, offset=-1)
    return shade(SKIN, face, x, y, h, 55, 0.1)


def arm_sleeved(face, x, y, w, h):
    if y >= 10 or face == "bottom":
        if face == "bottom":
            return SKIN[0] if y == 0 else SKIN[1]
        return NAIL if y == h - 1 and x % 2 == 0 else shade(SKIN, face, x, y, h, 57, 0.1)
    return robe_dirt(ROBE, face, x, y, h, 59, 24, 12)


def sleeve(torn):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return None if torn else ROBE[0]
        if face == "top":
            return shade(ROBE, face, x, y, h, 61)
        if torn and y == h - 1 and noise(x, ord(face[0]), 63) < 0.55:
            return None
        return shade(ROBE, face, x, y, h, 67 if torn else 69, 0.1)
    return paint


def cuff(face, x, y, w, h):
    """The wide sleeve's opening: robe cloth with the indigo hem of the collar."""
    if face == "bottom":
        return ROBE[0] if 0 < x < w - 1 and 0 < y < h - 1 else INDIGO[1]
    if face == "top":
        return None
    return INDIGO[2] if y == h - 1 else shade(ROBE, face, x, y, h, 71, 0.1)


def band(ramp, salt):
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return None
        return ramp[3] if (x + salt) % 3 == 0 else shade(ramp, face, x, y, h, salt, 0.1)
    return paint


def band_tail(ramp):
    return lambda face, x, y, w, h: ramp[3] if y == 0 else ramp[2] if y < h - 1 else ramp[1]


# Legs ---------------------------------------------------------------------------------------------------------

def leg_paint(wraps):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return SHOE[0]
        if face == "top":
            return TROUSER[2]
        if y >= 10:
            return shade(SHOE, face, x, y, h, 73)
        if y >= 6:
            ramp = WRAP if wraps else BANDAGE
            return ramp[1] if (y + x) % 4 == 0 else shade(ramp, face, x, y, h, 75 if wraps else 77, 0.1)
        return shade(TROUSER, face, x, y, h, 79)
    return paint


def shoe(face, x, y, w, h):
    if face == "bottom":
        return SHOE[0]
    if face == "top":
        return None
    return SHOE[3] if y == 0 else shade(SHOE, face, x, y, h, 81, 0.15)


def toe(face, x, y, w, h):
    return SHOE[3] if face == "top" else SHOE[2] if face == "front" else SHOE[1]


def parts():
    p = [
        # Head: the face, its brow and nose, the hair over it, a lock down the back, the fallen bun and its pin.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("brow", "Head", (-3, 28, 3.4), (6, 1, 1), brow),
        Part("nose", "Head", (-0.5, 25.5, 3.5), (1, 2, 1), nose),
        Part("hair", "Head", (-4, 28, -4), (8, 4, 8), hair_cap, inflate=0.3),
        Part("tuft_1", "Head", (-3, 32, -1), (3, 1, 3), tuft),
        Part("tuft_3", "Head", (-1.5, 31.8, 2.4), (2, 1, 2), tuft),
        Part("side_lock_r", "Head", (-4.6, 24, 1.8), (1, 4, 1), hair_lock),
        Part("side_lock_l", "Head", (3.6, 23.5, 2.2), (1, 5, 1), hair_lock),
        Part("hair_lock", "Head", (0.5, 22, -4.9), (2, 7, 1), hair_lock),
        Part("bun", "Head", (-1.5, 31.5, -4), (3, 3, 3), bun),
        Part("pin", "Head", (-3.4, 33, -3), (4, 1, 1), pin),
        # Trunk: the robe, its collar and the crossed lapel stepping down to the belt, the rope belt, knot and ends,
        # the jade token.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), body),
        Part("collar", "trunk", (-4, 22, -2), (8, 2, 4), collar, inflate=0.45),
        # The outer panel crosses from his left shoulder down to the right of the belt, a band two pixels wide.
        *[Part(f"lapel_{i}", "trunk", (2.5 - i, 22 - i, 1.6), (2, 1, 1), lapel) for i in range(7)],
        Part("belt", "trunk", (-4, 14, -2), (8, 2, 4), belt, inflate=0.5),
        Part("knot", "Torso", (-3, 13.5, 2.1), (2, 2, 1), knot),
        Part("cord_end_1", "Torso", (-2.8, 10.5, 2.35), (1, 3, 1), cord_end),
        Part("cord_end_2", "Torso", (-1.7, 11, 2.35), (1, 3, 1), cord_end),
        Part("pendant", "Torso", (4.15, 8, -0.5), (1, 6, 1), pendant),
        # Arms: the right sleeve torn off at the elbow over a bandaged forearm, the left sleeve whole down to a wide
        # trimmed cuff.
        Part("arm_r", arm("R"), (-8, 12, -2), (4, 12, 4), arm_bare),
        Part("sleeve_r", "Arm_R", (-8, 17, -2), (4, 7, 4), sleeve(True), inflate=0.45),
        Part("band_r1", "Hand_R", (-8, 15, -2), (4, 1, 4), band(BANDAGE, 83), inflate=0.3),
        Part("band_r2", "Hand_R", (-8, 13, -2), (4, 1, 4), band(BANDAGE, 85), inflate=0.3),
        Part("band_r_tail", "Hand_R", (-8.75, 11.5, -0.5), (1, 2, 1), band_tail(BANDAGE)),
        Part("arm_l", arm("L"), (4, 12, -2), (4, 12, 4), arm_sleeved),
        Part("sleeve_l", "Arm_L", (4, 17, -2), (4, 7, 4), sleeve(False), inflate=0.45),
        Part("sleeve_l_low", "Hand_L", (4, 15, -2), (4, 3, 4), sleeve(False), inflate=0.5),
        Part("cuff_l", "Hand_L", (4, 14, -2), (4, 2, 4), cuff, inflate=0.7),
        # Legs: the robe's skirt over each thigh, wrapped (right) or bandaged (left) shins, cloth shoes.
        Part("leg_r", leg("R"), (-3.9, 0, -2), (4, 12, 4), leg_paint(True)),
        Part("leg_l", leg("L"), (-0.1, 0, -2), (4, 12, 4), leg_paint(False)),
        Part("skirt_r", "Thigh_R", (-3.9, 5, -2), (4, 7, 4), skirt, inflate=0.5),
        Part("skirt_l", "Thigh_L", (-0.1, 5, -2), (4, 7, 4), skirt, inflate=0.5),
        Part("wrap_r1", "Leg_R", (-3.9, 4, -2), (4, 1, 4), band(WRAP, 87), inflate=0.28),
        Part("wrap_r2", "Leg_R", (-3.9, 2.5, -2), (4, 1, 4), band(WRAP, 89), inflate=0.28),
        Part("band_l1", "Leg_L", (-0.1, 4, -2), (4, 1, 4), band(BANDAGE, 91), inflate=0.3),
        Part("band_l2", "Leg_L", (-0.1, 2.5, -2), (4, 1, 4), band(BANDAGE, 93), inflate=0.3),
        Part("band_l_tail", "Leg_L", (4.15, 1.5, -0.5), (1, 2, 1), band_tail(BANDAGE)),
        Part("shoe_r", "Leg_R", (-3.9, 0, -2), (4, 2, 4), shoe, inflate=0.35),
        Part("shoe_l", "Leg_L", (-0.1, 0, -2), (4, 2, 4), shoe, inflate=0.35),
        Part("toe_r", "Leg_R", (-3.9, 0, 2.0), (4, 1, 1), toe),
        Part("toe_l", "Leg_L", (-0.1, 0, 2.0), (4, 1, 1), toe),
    ]
    return p
