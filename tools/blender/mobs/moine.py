"""Moine du Vide (the Enderman's new look; design sheet: docs/mobs/moine-vide.png, moves: docs/mobs/boards/moine.png).

A monk who meditated on the Void until it answered: a gaunt giant with an ashen shaved head, a black blindfold
knotted behind (two tails hanging on his left), a torn black robe with wide sleeves down to his ankles, a hemp rope
at the waist, straw sandals, very long thin hands. A rosary of violet beads floats around his neck. The blindfold is
its own part so the mod can hide it when he tears it off; the eyes under it glow.

His skeleton is the biped's, joint for joint, stretched to the Enderman's height (2.9 blocks): legs of 26 pixels,
a 12 pixel trunk, arms of 29 pixels.
"""

import os

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

HIP, KNEE = 26, 13
SHOULDER, ELBOW = 37, 24
NECK = 38

# name, parent, model position at rest: murim_biped's joints, stretched.
JOINTS = [
    ("Root", None, (0, HIP, 0)),
    ("Torso", "Root", (0, HIP + 1, 0)),
    ("Chest", "Torso", (0, HIP + 6, 0)),
    ("Head", "Chest", (0, NECK, 0)),
]
for _side, _x in (("L", 1), ("R", -1)):
    JOINTS += [
        (f"Thigh_{_side}", "Root", (1.5 * _x, HIP, 0)),
        (f"Leg_{_side}", f"Thigh_{_side}", (1.5 * _x, KNEE, 0)),
        (f"Knee_{_side}", f"Thigh_{_side}", (1.5 * _x, KNEE, 0)),
        (f"Shoulder_{_side}", "Chest", (0, NECK, 0)),
        (f"Arm_{_side}", f"Shoulder_{_side}", (5 * _x, SHOULDER, 0)),
        (f"Hand_{_side}", f"Arm_{_side}", (5 * _x, ELBOW, 0)),
        (f"Tool_{_side}", f"Hand_{_side}", (5 * _x, 9, 0)),
        (f"Elbow_{_side}", f"Arm_{_side}", (5 * _x, ELBOW, 0)),
    ]
# The rosary's beads, calm: a ring floating clear of the neck and shoulders, lower in front, each bead bobbing a
# little off the ring. Angry (renders only, MURIM_EYES_OPEN): torn off, the beads orbit wide around his body, the
# ring tilted, the cord broken.
import math  # noqa: E402

ANGRY = bool(os.environ.get("MURIM_EYES_OPEN"))
SLOTS = 12


def _ring(t, angry):
    a = math.radians(90 + t * 360 / SLOTS)
    if angry:
        return 13.0 * math.cos(a), 29 + 5.0 * math.sin(a) + 1.5 * math.sin(3 * a), 11.0 * math.sin(a)
    return 8.4 * math.cos(a), NECK - 1 - 2.0 * math.sin(a) + 0.7 * math.sin(3 * a), 5.8 * math.sin(a) + 0.6


BEADS = [_ring(i, ANGRY) for i in range(SLOTS)]
SPACERS = [_ring(i + 0.5, False) for i in range(SLOTS)]
JOINTS.append(("Rosary", "Chest", (0, NECK - 2, 0)))
# The gold spacers on the cord, and the blindfold: the mod hides both once he has torn them off.
JOINTS.append(("Cord", "Rosary", (0, NECK - 2, 0)))
JOINTS.append(("Blindfold", "Head", (0, NECK + 5, 0)))
JOINTS += [(f"Bead_{i}", "Rosary", BEADS[i]) for i in range(SLOTS)]
HELPERS = [("Elbow_L", "Hand_L"), ("Elbow_R", "Hand_R"), ("Knee_L", "Leg_L"), ("Knee_R", "Leg_R")]

# Renders: body centre, face centre, ortho scales, aspect.
VIEW = ((0, 24, 0), (0, 42, 1), 60, 16, 1.25)
STAND = {}
# The Void palm: the right arm thrust out flat, a long step forward.
SHOW_POSE = {
    "Arm_R": (-88, 6), "Hand_R": (-4, 0), "Arm_L": (20, -8), "Hand_L": (-30, 0),
    "Chest": (10, 18), "Head": (-8, -12), "Thigh_L": (-28, 0), "Leg_L": (26, 0), "Thigh_R": (18, 0),
}

SKIN = hexes("#5d566a", "#7b748a", "#9a93a8", "#b9b2c4", "#d2ccdb")
ROBE = hexes("#0e0b12", "#16121b", "#1d1824", "#2a2333", "#3a3145")
BAND = hexes("#08060a", "#0f0c13", "#18131d", "#231c2a", "#30273a")
ROPE = hexes("#2a1d13", "#3a2a1e", "#4f3a28", "#675038", "#80684a")
STRAW = hexes("#5e4c2c", "#7c6a40", "#a08a5a", "#bba670", "#d2c08a")
BEAD = hexes("#4a1d78", "#6a2aa8", "#8a3fd0", "#a050f0", "#c58cff")
# The Enderman's eyes: wide magenta bars with a pink core.
EYE = (222, 100, 250)
EYE_RIM = (160, 0, 215)
GLINT = (238, 214, 255)
GOLD = hexes("#4a3512", "#7a5a1e", "#a8832e", "#d0aa4a", "#ecd27e")
SOCKET, MOUTH, RIB = (40, 34, 50), (88, 78, 98), (128, 120, 144)


def flat(ramp, salt, offset=0, rough=0.1):
    return lambda face, x, y, w, h: shade(ramp, face, x, y, h, salt, rough, offset)


def side_x(face, x, w):
    return x if face == "right" else w - 1 - x


# Head: shaved, hollow cheeks, the brow heavy over sunken eyes (under the blindfold), a thin shut mouth.
FACE = [
    "LLLLLLLL",
    "SLLLLLLS",
    "OOODDOOO",
    "EPEOOEPE",
    "KOOKKOOK",
    "DSSLSSSD",
    "DDSMMSDD",
    "KDSSSSDK",
]
FACE_COLOURS = {"S": SKIN[3], "L": SKIN[4], "D": SKIN[2], "K": SKIN[1], "O": SOCKET, "E": EYE_RIM, "P": EYE,
                "M": MOUTH}


def head(face, x, y, w, h):
    if face == "front":
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if y in (3, 4) and fx in (2, 3):
            return SKIN[2] if fx == 2 else SKIN[1]
        if y >= 5 and fx >= 5:
            return SKIN[2]
    if face == "top":
        # A few faded ordination scars on the shaved crown.
        if (x, y) in ((2, 3), (4, 3), (3, 5), (5, 5), (2, 6)):
            return SKIN[2]
    return shade(SKIN, face, x, y, h, 11, 0.14, 1)


def head_glow(face, x, y, w, h):
    if face == "front" and y == 3 and FACE[3][x] in "EP":
        return (120, 0, 190, 255) if FACE[3][x] == "E" else (200, 40, 240, 255)
    return None


def blindfold(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    # The knot at the back.
    if face == "back" and x in (5, 6):
        return BAND[3]
    return shade(BAND, face, x, y, h, 21, 0.15, 1)


# Robe ---------------------------------------------------------------------------------------------------------

def robe_upper(face, x, y, w, h):
    if face == "front":
        # The V of the crossed collar: bare chest, ribs showing.
        half = abs(2 * x + 1 - w) // 2
        if half < 3 - y // 2 and y < 6:
            return RIB if y in (2, 4) else SKIN[2]
        if half == 3 - y // 2 and y < 6:
            return ROBE[4]
    if face == "back" and y in (3, 4) and 2 <= x <= 5:
        # A tear across the back.
        return SKIN[1] if (x + y) % 2 else ROBE[0]
    return shade(ROBE, face, x, y, h, 31, 0.12, 1)


def robe_lower(face, x, y, w, h):
    if face == "front" and x == w // 2:
        return ROBE[0]
    return shade(ROBE, face, x, y, h, 33, 0.12, 1)


def rope(face, x, y, w, h):
    return ROPE[3] if (x + y) % 2 else shade(ROPE, face, x, y, h, 41, 0.1)


def rope_end(face, x, y, w, h):
    if y == h - 1:
        return ROPE[1]
    return shade(ROPE, face, x, y, h, 43, 0.1)


def robe_panel(face, x, y, w, h):
    """The robe below the belt, to the ankles; its hem torn into strips."""
    if face in ("front", "back"):
        torn = int(noise(x, 0, 51) * 4)
        if y >= h - torn:
            return None
        if y == h - torn - 1:
            return ROBE[0]
        if noise(x, y, 53) < 0.04:
            return None
    return shade(ROBE, face, x, y, h, 55, 0.14, 1)


def skirt(face, x, y, w, h):
    if face in ("front", "back", "right", "left") and y == h - 1 and noise(x, 0, 57) < 0.5:
        return None
    return shade(ROBE, face, x, y, h, 59, 0.12, 1)


def sleeve(salt):
    def paint(face, x, y, w, h):
        if face in ("front", "back", "right", "left") and y == h - 1 and noise(x, salt, 61) < 0.45:
            return None
        if face == "bottom":
            return ROBE[0]
        return shade(ROBE, face, x, y, h, salt, 0.12, 1)
    return paint


def bone_limb(face, x, y, w, h):
    """Long thin ashen forearms and shins; a knuckle or knee darker every few pixels."""
    if face not in ("top", "bottom") and y % 6 == 5:
        return SKIN[2]
    return shade(SKIN, face, x, y, h, 63, 0.12, 0)


def finger(face, x, y, w, h):
    if y == h - 1:
        return SKIN[1]
    return shade(SKIN, face, x, y, h, 65, 0.1, 1)


def sandal(face, x, y, w, h):
    if face == "top":
        return STRAW[3] if (x + y) % 2 else STRAW[2]
    return STRAW[2] if (x + y) % 2 else STRAW[1]


def strap(face, x, y, w, h):
    return STRAW[1]


def bead(face, x, y, w, h):
    """A glass bead lit from inside: a bright glint on the upper corner, the lower edge in shadow."""
    if face == "top":
        return BEAD[4] if (x, y) == (0, 0) else BEAD[3]
    if face == "bottom":
        return BEAD[1]
    if (x, y) == (0, 0):
        return GLINT
    return BEAD[1] if y == h - 1 and x == w - 1 else BEAD[2] if y == h - 1 else BEAD[3]


def bead_glow(face, x, y, w, h):
    if face == "bottom":
        return None
    return (GLINT if (x, y) == (0, 0) else BEAD[1]) + (255,)


def guru(face, x, y, w, h):
    """The guru bead at the front, bigger, carved: a dark rim, the Void's eye glowing at its centre."""
    if face in ("front", "back", "right", "left") and (x, y) == (1, 1):
        return EYE
    if face == "top":
        return BEAD[3] if (x + y) % 2 else BEAD[4]
    if face == "bottom":
        return BEAD[0]
    if x in (0, w - 1) and y in (0, h - 1):
        return BEAD[1]
    return BEAD[2] if (x + y) % 2 else BEAD[3]


def guru_glow(face, x, y, w, h):
    if face in ("front", "back", "right", "left") and (x, y) == (1, 1):
        return (240, 210, 255, 255)
    if face in ("front", "right", "left") and (x == 1 or y == 1):
        return BEAD[2] + (255,)
    return None


def spacer(face, x, y, w, h):
    return GOLD[3] if face == "top" else GOLD[2]


def tassel(face, x, y, w, h):
    if y == 0:
        return GOLD[3]
    if y == h - 1 and x % 2:
        return None
    return BEAD[1] if (x + y) % 2 else BEAD[2]


def band_tail(face, x, y, w, h):
    if y == h - 1 and x == 0:
        return None
    return shade(BAND, face, x, y, h, 71, 0.12, 1)


def limb(side, upper, lower, helper, hinge, opens):
    return ("limb", f"{upper}_{side}", f"{lower}_{side}", f"{helper}_{side}", hinge, opens)


def parts():
    p = [
        # Head and blindfold (hidden in game once torn off), its two tails hanging behind his left ear.
        Part("head", "Head", (-4, NECK, -4), (8, 8, 8), head, emissive=head_glow),
        Part("blindfold", "Blindfold", (-4, NECK + 4, -4), (8, 2, 8), blindfold, inflate=0.35),
        Part("blindfold_tail_1", "Blindfold", (2.6, NECK + 0.5, -4.9), (1, 5, 1), band_tail),
        Part("blindfold_tail_2", "Blindfold", (3.7, NECK + 1.5, -4.6), (1, 4, 1), band_tail),
        # Trunk: the gaunt chest in the crossed robe, the rope belt and its ends.
        Part("robe_upper", "Chest", (-4, HIP + 6, -2), (8, 6, 4), robe_upper),
        Part("collar", "Chest", (-4, NECK - 1, -2), (8, 1, 4), flat(ROBE, 35, 2), inflate=0.4),
        Part("robe_lower", "Torso", (-4, HIP, -2), (8, 6, 4), robe_lower),
        Part("rope", "Torso", (-4, HIP + 0.5, -2), (8, 1, 4), rope, inflate=0.45),
        Part("rope_knot", "Torso", (1, HIP, 2.2), (2, 2, 1), rope),
        # Cloth, simulated in game: the robe's long front and back to the ankles, the rope's ends.
        Part("robe_front", "Torso", (0, HIP + 0.5, 2.5), (8, 23, 1), robe_panel, cloth="front"),
        Part("robe_back", "Torso", (0, HIP + 0.5, -2.5), (8, 24, 1), robe_panel, cloth="back"),
        Part("rope_end_1", "Torso", (1.6, HIP, 3.0), (1, 6, 1), rope_end, cloth="front"),
        Part("rope_end_2", "Torso", (2.6, HIP, 3.0), (1, 5, 1), rope_end, cloth="front"),
    ]
    for side, x in (("R", -1), ("L", 1)):
        ax = -6 if side == "R" else 4
        lx = -3 if side == "R" else 0
        p += [
            # Arm: a thin ashen limb under a wide sleeve that flares past the elbow; long fingers.
            Part(f"arm_{side.lower()}", limb(side, "Arm", "Hand", "Elbow", ELBOW, "back"), (ax, 9, -1), (2, 29, 2),
                 bone_limb),
            Part(f"sleeve_{side.lower()}", f"Arm_{side}", (ax - 0.5, ELBOW + 1, -1.5), (3, 13, 3), sleeve(81),
                 inflate=0.3),
            Part(f"sleeve_low_{side.lower()}", f"Hand_{side}", (ax - 1, ELBOW - 6, -2), (4, 7, 4), sleeve(83),
                 inflate=0.2),
            *[Part(f"finger_{side.lower()}_{i}", f"Hand_{side}", (ax + 0.5, 5 + (i % 2), -1.2 + 0.8 * i),
                   (1, 4 - (i % 2), 1), finger) for i in range(3)],
            # Leg: the robe over the thigh, a long bare shin, a straw sandal.
            Part(f"leg_{side.lower()}", limb(side, "Thigh", "Leg", "Knee", KNEE, "front"), (lx + 0.5, 0, -1),
                 (2, HIP, 2), bone_limb),
            Part(f"skirt_{side.lower()}", f"Thigh_{side}", (lx, KNEE + 2, -2), (3, HIP - KNEE - 2, 4), skirt,
                 inflate=0.4),
            Part(f"sandal_{side.lower()}", f"Leg_{side}", (lx, 0, -2), (3, 1, 5), sandal, inflate=0.1),
            Part(f"strap_{side.lower()}", f"Leg_{side}", (lx + 0.5, 1, 0.5), (2, 1, 1), strap, inflate=0.15),
        ]
    # The rosary: its beads float, each on a joint of its own (Bead_i under Rosary), so the mod can spin the ring
    # and, once he tears it off, fling the beads into a wide orbit around him.
    for i, (x, y, z) in enumerate(BEADS):
        if i == 0:
            p.append(Part("bead_guru", "Bead_0", (x - 1.5, y - 1.5, z - 1.5), (3, 3, 3), guru, emissive=guru_glow))
            p.append(Part("bead_tassel", "Bead_0", (x - 1, y - 5.5, z - 0.5), (2, 4, 1), tassel))
        else:
            p.append(Part(f"bead_{i}", f"Bead_{i}", (x - 1, y - 1, z - 1), (2, 2, 2), bead, emissive=bead_glow))
        if not ANGRY:
            # The gold spacers ride with the bead before them; the cord is broken once he is angry.
            sx, sy, sz = SPACERS[i]
            p.append(Part(f"spacer_{i}", "Cord", (sx - 0.5, sy - 0.5, sz - 0.5), (1, 1, 1), spacer))
    if os.environ.get("MURIM_EYES_OPEN"):
        # For the renders only: the blindfold torn off, as in game once angered.
        p = [q for q in p if not q.name.startswith("blindfold")]
    if os.environ.get("MURIM_PREVIEW_CLOTH"):
        # For the renders only: the cloth pieces as stiff boxes hanging straight, as they rest in game.
        for part in [q for q in p if q.cloth]:
            w, length = part.size[0], part.size[1]
            x, y, z = part.corner
            p.append(Part(part.name + "_preview", part.bind, (x - w / 2, y - length, z - 0.5), (w, length, 1), part.paint))
    return p


# Clips (docs/mobs/boards/moine.png, v3), from the storyboard's poses by murim_clips.board. The beads are not keyed:
# the mod moves them (SkeletalMobs: around the neck, in orbit, gathered, flung, burst, fallen).
def _clips():
    from murim_clips import board

    def pose(**p):
        p.setdefault("w", p["fR"])
        return board(p)

    pray = pose(lean=16, head=10, aR=4, fR=112, aL=10, fL=116, tF=6, sF=2, tB=-6, sB=-2)
    pray_b = pose(lean=18, head=14, hy=1, aR=4, fR=114, aL=10, fL=118, tF=6, sF=2, tB=-6, sB=-2)
    walk_a = pose(lean=16, head=10, hy=1, aR=4, fR=112, aL=10, fL=116, tF=22, sF=10, tB=-18, sB=-12)
    walk_b = pose(lean=16, head=10, hy=1, aR=4, fR=112, aL=10, fL=116, tF=-18, sF=-22, tB=22, sB=10)
    pass_a = pose(lean=16, head=10, hy=-1, aR=4, fR=112, aL=10, fL=116, tF=4, sF=-30, tB=-2, sB=-6)
    pass_b = pose(lean=16, head=10, hy=-1, aR=4, fR=112, aL=10, fL=116, tF=-2, sF=-6, tB=4, sB=-30)
    hand_up = pose(lean=8, head=-6, aR=150, fR=210, aL=20, fL=150, tF=6, sF=2, tB=-6, sB=-2)
    tear = pose(lean=-6, head=-14, aR=120, fR=150, aL=-10, fL=10, tF=8, sF=2, tB=-8, sB=-4)
    tall = pose(lean=-4, head=-4, hy=-2, aR=40, fR=30, aL=-40, fL=-30, tF=10, sF=4, tB=-10, sB=-6)
    guard = pose(lean=8, head=-4, aR=30, fR=90, aL=10, fL=60, tF=12, sF=4, tB=-14, sB=-8)
    guard_b = pose(lean=10, head=-2, hy=1, aR=32, fR=94, aL=12, fL=64, tF=12, sF=4, tB=-14, sB=-8)
    gwalk_a = pose(lean=10, head=-4, aR=30, fR=90, aL=10, fL=60, tF=24, sF=10, tB=-20, sB=-14)
    gwalk_b = pose(lean=10, head=-4, aR=30, fR=90, aL=10, fL=60, tF=-20, sF=-24, tB=24, sB=10)
    wind = pose(lean=-2, head=-2, hx=-6, aR=-50, fR=40, aL=50, fL=90, tF=6, sF=0, tB=-24, sB=-16)
    palm = pose(lean=18, head=-10, hx=12, hy=4, aR=92, fR=90, aL=-40, fL=-20, tF=40, sF=4, tB=-36, sB=-24)
    palm_hold = pose(lean=16, head=-8, hx=12, hy=5, aR=88, fR=88, aL=-36, fL=-20, tF=40, sF=4, tB=-36, sB=-24)
    stagger = pose(lean=-18, head=-24, hx=-8, aR=-30, fR=-10, aL=40, fL=20, tF=20, sF=4, tB=-30, sB=-24)
    dazed = pose(lean=6, head=16, hy=4, aR=10, fR=0, aL=6, fL=-6, tF=10, sF=2, tB=-12, sB=-8)
    call = pose(lean=4, head=-6, aR=80, fR=70, aL=-20, fL=0, tF=12, sF=4, tB=-14, sB=-8)
    draw = pose(lean=-8, head=-6, hx=-4, aR=20, fR=60, aL=30, fL=50, tF=6, sF=0, tB=-22, sB=-14)
    fling = pose(lean=18, head=-6, hx=8, aR=92, fR=88, aL=-30, fL=-10, tF=34, sF=4, tB=-30, sB=-20)
    spent = pose(lean=6, head=-4, aR=40, fR=70, aL=0, fL=20, tF=12, sF=4, tB=-14, sB=-8)
    tight = pose(lean=12, head=0, hy=2, aR=20, fR=100, aL=20, fL=100, tF=10, sF=2, tB=-10, sB=-6)
    crouch = pose(lean=24, head=-6, hy=10, aR=70, fR=100, aL=30, fL=60, tF=70, sF=0, tB=-40, sB=-60)
    cross = pose(lean=10, head=4, aR=40, fR=140, aL=46, fL=150, tF=10, sF=2, tB=-12, sB=-6)
    burst = pose(lean=-6, head=-8, aR=100, fR=110, aL=-80, fL=-100, tF=14, sF=4, tB=-16, sB=-8)
    flinch = pose(lean=-8, head=-14, hx=-3, aR=20, fR=70, aL=-6, fL=40, tF=8, sF=2, tB=-16, sB=-10)
    knees = pose(lean=10, head=20, hy=34, aR=10, fR=0, aL=0, fL=-10, tF=90, sF=-10, tB=80, sB=-20)
    fallen = pose(lean=80, head=20, hy=50, hx=10, aR=70, fR=60, aL=60, fL=50, tF=90, sF=0, tB=90, sB=10)
    return {
        "idle": [(0, pray), (30, pray_b), (60, pray)],
        "walk": [(0, walk_a), (8, pass_a), (15, walk_b), (23, pass_b), (30, walk_a)],
        "stance": [(0, guard), (20, guard_b), (40, guard)],
        "stance_walk": [(0, gwalk_a), (15, gwalk_b), (30, gwalk_a)],
        "anger": [(0, pray), (8, hand_up), (12, tear), (20, tall), (28, guard)],
        "palm": [(0, guard), (10, wind), (14, palm), (30, palm_hold), (40, guard)],
        "stagger": [(0, palm), (6, stagger), (12, dazed), (20, dazed)],
        "volley": [(0, guard), (6, call), (13, draw), (15, fling), (20, fling), (36, spent), (46, guard)],
        "step": [(0, guard), (8, tight), (9, crouch), (20, wind), (24, palm), (36, palm_hold), (40, guard)],
        "burst": [(0, guard), (8, cross), (10, burst), (14, burst), (30, guard)],
        "flinch": [(0, guard), (3, flinch), (10, guard)],
        "death": [(0, tall), (12, knees), (24, fallen), (30, fallen)],
    }


CLIPS = _clips() if "bpy" in __import__("sys").modules else {}
