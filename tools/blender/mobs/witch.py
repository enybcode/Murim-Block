"""Empoisonneuse des Cinq Venins (the witch's new look; design sheet: docs/mobs/empoisonneuse.png).

A disciple of the Five Venoms sect, driven out of the cities, who raises snakes, scorpions, centipedes, toads and
spiders in the marshes for their venom. Beautiful and deadly, drawn as the best player skins are (Enzo's reference,
29 Oct 2026): hair in relief over the head (bangs over the brow, locks down the cheeks), large eyes with a dark lash
line and a bright iris, a blush, a small mouth; strong contrast and rich shading. Black hair with violet sheen in a
high bun, a silver serpent hairpin with a jade tassel; a black robe with a white crossed collar edged in gold, a jade
sash knotted in front, a short skirt to the knees and long front and back panels to the ankles, the front embroidered
in gold with the five venoms; wide violet cuffs; dark stockings, black slippers; gourds of poison and glowing vials at
the hip. On the biped, slim arms.
"""

from disciple import side_x
from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

SKIN = hexes("#c99c8c", "#e0b4a2", "#f0cdbc", "#f8dfd2", "#fff0e8")
BLACK = hexes("#08060c", "#110c18", "#1b1424", "#271d34", "#372a48")
COLLAR = hexes("#a89cb8", "#c8bed6", "#e2daee", "#f2eef8", "#ffffff")
VIOLET = hexes("#3e1666", "#55208a", "#6e30ac", "#8a48c8", "#a86ae0")
GOLD = hexes("#6b5212", "#957220", "#c49a2e", "#d9b444", "#f0d683")
JADE = hexes("#0f4a2a", "#1a6a3e", "#259154", "#38b06c", "#6cdc8c")
HAIR = hexes("#050407", "#0b0910", "#131019", "#1e1a28", "#342e48")
SILVER = hexes("#6e6e7a", "#8c8c98", "#a8a8b4", "#c9c9d6", "#eeeef6")
GOURD = hexes("#3e2410", "#55331a", "#6b3f22", "#7a4a28", "#94603a")
VENOM, VENOM_LIGHT = (95, 209, 122), (210, 255, 222)
IRIS, IRIS_LIGHT, LASH, BLUSH, LIP, RED = (40, 150, 90), (120, 230, 160), (14, 8, 18), (244, 170, 172), \
    (204, 90, 110), (176, 42, 30)


def rich(ramp, face, x, y, h, salt, offset=0):
    """Shading as in a fine skin: light from above, a gentle darkening down the face, and grain in two tones."""
    level = 2 + offset + (1 if face == "top" else -2 if face == "bottom" else 0)
    if face not in ("top", "bottom"):
        level -= int(2 * y / max(h, 1) + 0.3) if h > 3 else 0
        level += 1 if y == 0 else 0
    n = noise(x, y, salt)
    level += 1 if n > 0.82 else -1 if n < 0.18 else 0
    return ramp[max(0, min(4, level))]


# Head ---------------------------------------------------------------------------------------------------------

# The face under the hair: the brow in the hair's shadow, a dark lash line winged out, the iris dark below and bright
# above, a blush, a small mouth.
FACE = [
    "DDDDDDDD",
    "DDDDDDDD",
    "SSSSSSSS",
    "SKKSSKKS",
    "SEISSIES",
    "SPSSSSPS",
    "SSSMMSSS",
    "SSSSSSSS",
]
FACE_COLOURS = {"S": SKIN[3], "D": SKIN[1], "K": LASH, "E": IRIS, "I": IRIS_LIGHT, "P": BLUSH, "M": LIP}


def head(face, x, y, w, h):
    if face == "front":
        if FACE[y][x] == "S" and (x in (0, w - 1)):
            return SKIN[2]
        return FACE_COLOURS[FACE[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if fx >= 6 and y >= 3:
            return SKIN[2]
    return rich(HAIR, face, x, y, h, 11, 1)


def head_glow(face, x, y, w, h):
    return IRIS_LIGHT + (120,) if face == "front" and FACE[y][x] == "I" else None


def hair_outer(face, x, y, w, h):
    """The hair in relief over the head: bangs over the brow parted off-centre, locks down both cheeks."""
    if face == "front":
        bangs = y == 0 or (y == 1 and x not in (4,)) or (y == 2 and x in (0, 1, 2, 6, 7))
        locks = x in (0, 7) and y <= 6 or x == 1 and y <= 4
        if not (bangs or locks):
            return None
        # A sheen along the bangs.
        if y == 1 and x in (2, 5):
            return HAIR[4]
        return rich(HAIR, face, x, y, h, 21, 1)
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        if fx >= 6 and 3 <= y <= 6:
            return None
        return HAIR[4] if fx % 3 == 1 and y in (1, 2) else rich(HAIR, face, x, y, h, 23, 1)
    if face == "bottom":
        return None
    return HAIR[4] if face == "top" and x in (2, 3) and y in (2, 3, 4) else rich(HAIR, face, x, y, h, 25, 1)


def lock(face, x, y, w, h):
    if y == h - 1 and face in ("front", "back"):
        return HAIR[1]
    return HAIR[4] if y % 4 == 1 else rich(HAIR, face, x, y, h, 27, 1)


def bun(face, x, y, w, h):
    if face == "bottom":
        return HAIR[1]
    return HAIR[4] if (x + 2 * y) % 5 == 0 else rich(HAIR, face, x, y, h, 29, 1)


def ribbon(face, x, y, w, h):
    return GOLD[4] if (x + y) % 2 == 0 else GOLD[3]


def pin(face, x, y, w, h):
    """The silver hairpin: a serpent's head at its end, a red eye."""
    if face in ("front", "top") and x == w - 1:
        return RED
    return SILVER[4] if face == "top" else SILVER[3]


def jade_drop(face, x, y, w, h):
    return GOLD[3] if y == 0 else JADE[4] if face in ("front", "top", "left", "right") else JADE[3]


def jade_glow(face, x, y, w, h):
    return JADE[4] + (110,) if y > 0 and face != "bottom" else None


def long_hair(face, x, y, w, h):
    if face == "front":
        return HAIR[1]
    return HAIR[4] if x % 3 == 1 and y % 4 < 2 else rich(HAIR, face, x, y, h, 31, 1)


# Body ---------------------------------------------------------------------------------------------------------

def bodice(face, x, y, w, h):
    """The black robe: a white crossed collar, two pixels wide and edged in gold, the left over the right."""
    if face == "front":
        edge = 1 + y // 2
        if y < 2 and x in (3, 4):
            return SKIN[2]
        if y < 8 and x == edge:
            return GOLD[3]
        if y < 8 and x in (edge + 1, edge + 2):
            return COLLAR[3] if x == edge + 1 else COLLAR[1]
    return rich(BLACK, face, x, y, h, 33, 1)


def bust(face, x, y, w, h):
    """The bust, as on the best skins: one volume out of the bodice, lit on top, its underside in shadow, the crossed
    collar running down across it (gold edge, white lapel)."""
    if face == "top":
        return BLACK[4] if x % 3 else BLACK[3]
    if face == "bottom":
        return BLACK[0]
    if face == "front":
        if x == 1 + y:
            return GOLD[3]
        if x in (2 + y, 3 + y):
            return COLLAR[3] if x == 2 + y else COLLAR[1]
        if y == 0:
            return BLACK[4]
        return BLACK[2] if y == h - 1 else BLACK[3]
    return BLACK[2] if y < h - 1 else BLACK[1]


def sash(face, x, y, w, h):
    if face in ("top", "bottom"):
        return JADE[1]
    if y in (0, h - 1):
        return GOLD[3]
    return JADE[4] if noise(x, y, 37) > 0.75 else JADE[3]


def bow(face, x, y, w, h):
    """The sash's knot in front: two loops, a gold clasp in the middle."""
    if face == "front" and x == w // 2:
        return GOLD[4]
    return rich(JADE, face, x, y, h, 39, 1)


def tassel(face, x, y, w, h):
    return GOLD[3] if y == 0 else JADE[4] if y % 2 else JADE[2]


# The five venoms in gold, one above the other down the front panel: snake, scorpion, centipede, toad, spider.
VENOMS = [
    [".X.", "X..", ".X.", "..X", ".X."],
    ["X.X", ".X.", "XXX", ".X."],
    ["XXX", "X.X", "XXX"],
    [".X.", "XXX", "X.X"],
]


def venom_at(y, x, w):
    row = 1
    c = x - (w // 2 - 1)
    for motif in VENOMS:
        if row <= y < row + len(motif):
            return 0 <= c < 3 and motif[y - row][c] == "X"
        row += len(motif) + 1
    return False


def front_panel(face, x, y, w, h):
    """The long front panel to the ankles, swinging as she walks: violet, a gold border, the five venoms."""
    if face in ("front", "back"):
        if y == h - 1 or x in (0, w - 1):
            return GOLD[3]
        if face == "front" and venom_at(y, x, w):
            return GOLD[4] if (x + y) % 2 else GOLD[3]
        return rich(VIOLET, face, x, y, h, 41, 0)
    return GOLD[2]


def back_panel(face, x, y, w, h):
    if face in ("front", "back") and (y == h - 1 or x in (0, w - 1)):
        return GOLD[3]
    return rich(BLACK, face, x, y, h, 43, 1)


def skirt(face, x, y, w, h):
    """The short skirt to the knees: black, soft pleats, a violet hem."""
    if face == "top":
        return BLACK[2]
    if face == "bottom":
        return BLACK[0]
    if y == h - 1:
        return VIOLET[3]
    return BLACK[3] if x % 3 == 0 else rich(BLACK, face, x, y, h, 45, 1)


def gourd(face, x, y, w, h):
    if face == "top" and w == 1:
        return RED
    return GOURD[4] if (x, y) == (0, 0) else rich(GOURD, face, x, y, h, 47, 0)


def vial(face, x, y, w, h):
    if y == 0:
        return SILVER[3]
    return VENOM_LIGHT if (x, y) == (0, 1) and face == "front" else VENOM


def vial_glow(face, x, y, w, h):
    return None if y == 0 else VENOM + (255,)


# Arms and legs -------------------------------------------------------------------------------------------------

def arm_paint(face, x, y, w, h):
    # Black sleeves, a white inner cuff, pale hands with a warm shade.
    if y >= h - 2:
        return SKIN[2] if face == "bottom" or y == h - 1 else SKIN[3]
    if y == h - 3:
        return COLLAR[3]
    return rich(BLACK, face, x, y, h, 51, 1)


def cuff(face, x, y, w, h):
    """The wide violet cuff over the forearm, edged in gold, open below."""
    if face == "bottom":
        return None
    if face == "top":
        return VIOLET[1]
    if y == h - 1:
        return GOLD[3]
    return rich(VIOLET, face, x, y, h, 53, 0)


def bracelet(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    return SILVER[4] if (x + y) % 3 == 0 else SILVER[3]


def leg_paint(face, x, y, w, h):
    # Dark stockings with a sheen down the shin, black slippers with a gold toe.
    if y <= 1:
        return GOLD[3] if face == "front" and y == 1 and x == 1 else BLACK[1]
    if face == "front" and x == 1 and 2 <= y <= 9:
        return BLACK[4]
    return rich(BLACK, face, x, y, h, 55, 1)


def parts():
    p = [
        # Head: the face, the hair in relief over it, the bun with its gold ribbon, the serpent pin, its jade tassel.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), head, emissive=head_glow),
        Part("hair_outer", "Head", (-4, 24, -4), (8, 8, 8), hair_outer, inflate=0.5),
        Part("bun", "Head", (-2, 32.5, -3), (4, 3, 3), bun),
        Part("bun_loop", "Head", (-1.5, 35, -2.5), (3, 2, 2), bun),
        Part("ribbon", "Head", (-2, 32.6, -3), (4, 1, 3), ribbon, inflate=0.25),
        Part("pin", "Head", (-5.5, 33.6, -2), (10, 1, 1), pin),
        Part("drop_1", "Head", (4.2, 31.2, -2), (1, 2, 1), jade_drop, emissive=jade_glow),
        Part("drop_2", "Head", (4.2, 30, -2), (1, 1, 1), jade_drop, emissive=jade_glow),
        # Long hair down the back, two locks before the shoulders.
        Part("hair_long", "trunk", (-3.5, 13, -2.8), (7, 11, 1), long_hair),
        Part("lock_r", "trunk", (-3.8, 17, 2.1), (1, 7, 1), lock),
        Part("lock_l", "trunk", (2.8, 17, 2.1), (1, 7, 1), lock),
        # The robe: bodice and its white collar, the bust, the jade sash and its bow and tassels.
        Part("body", "trunk", (-4, 12, -2), (8, 12, 4), bodice),
        Part("bust", "Chest", (-3.5, 17.5, 0), (7, 4, 2), bust, tilt=(38, 21.5, 0)),
        Part("sash", "Torso", (-4, 14, -2), (8, 2, 4), sash, inflate=0.25),
        Part("bow", "Torso", (-1.5, 13.6, 2.2), (3, 3, 1), bow),
        Part("tassel_1", "Torso", (-0.6, 14, 2.9), (1, 5, 1), tassel, cloth="front"),
        Part("tassel_2", "Torso", (0.5, 14, 2.9), (1, 4, 1), tassel, cloth="front"),
        # A short skirt to the knees; long panels front and back to the ankles, swinging.
        Part("skirt", "Torso", (-4.5, 6.5, -2.5), (9, 6, 5), skirt),
        Part("panel_front", "Torso", (0, 12.5, 2.8), (5, 12, 1), front_panel, cloth="front"),
        Part("panel_back", "Torso", (0, 12.5, -2.8), (6, 12, 1), back_panel, cloth="back"),
        # At the hip: two gourds of poison and two glowing vials.
        Part("gourd_1", "Torso", (4.4, 9.5, -1.2), (2, 3, 2), gourd),
        Part("gourd_1_neck", "Torso", (4.9, 12.5, -0.7), (1, 1, 1), gourd),
        Part("gourd_2", "Torso", (4.6, 9, 0.8), (2, 2, 2), gourd),
        Part("vial_1", "Torso", (-5.2, 12, 0.5), (1, 3, 1), vial, emissive=vial_glow),
        Part("vial_2", "Torso", (-5.2, 12.3, -0.9), (1, 3, 1), vial, emissive=vial_glow),
        # Slender arms in black sleeves, wide violet cuffs, the serpent bracelet.
        Part("arm_r", arm("R"), (-7, 12, -1.5), (3, 12, 3), arm_paint),
        Part("cuff_r", "Hand_R", (-7.5, 14, -2), (4, 4, 4), cuff, inflate=0.25),
        Part("arm_l", arm("L"), (4, 12, -1.5), (3, 12, 3), arm_paint),
        Part("cuff_l", "Hand_L", (3.5, 14, -2), (4, 4, 4), cuff, inflate=0.25),
        Part("bracelet", "Hand_L", (4, 13, -1.5), (3, 1, 3), bracelet, inflate=0.2),
        # Legs in dark stockings and slippers.
        Part("leg_r", leg("R"), (-3.5, 0, -1.5), (3, 12, 3), leg_paint),
        Part("leg_l", leg("L"), (0.5, 0, -1.5), (3, 12, 3), leg_paint),
    ]
    if __import__("os").environ.get("MURIM_PREVIEW_CLOTH"):
        # For the renders only: the cloth pieces as stiff boxes hanging straight, as they rest in game.
        for part in [q for q in p if q.cloth]:
            w, length = part.size[0], part.size[1]
            x, y, z = part.corner
            p.append(Part(part.name + "_preview", part.bind, (x - w / 2, y - length, z - 0.5), (w, length, 1),
                          part.paint))
    return p


# The throw: the right arm flung up and forward, the body turned.
SHOW_POSE = {
    "Arm_R": (-160, 20), "Hand_R": (-20, 0), "Arm_L": (30, -10), "Hand_L": (-20, 0),
    "Chest": (-8, -20), "Head": (-6, -15), "Thigh_L": (-20, 0), "Leg_L": (10, 0), "Thigh_R": (15, 0),
}


# Clips (docs/mobs/boards/witch.png), from the storyboard's poses by murim_clips.board, on the biped.
def _clips():
    from murim_clips import board

    def pose(**p):
        p.setdefault("w", p["fR"])
        return board(p)

    stand = pose(lean=2, head=4, aR=20, fR=70, aL=-4, fL=10, tF=4, sF=0, tB=-4, sB=-8)
    stand_b = pose(lean=2, head=8, aR=24, fR=84, aL=-4, fL=12, tF=4, sF=0, tB=-4, sB=-8, hy=0.5)
    walk_a = pose(lean=4, head=2, aR=-10, fR=0, aL=14, fL=18, tF=22, sF=10, tB=-16, sB=-22, hips=8)
    walk_b = pose(lean=4, head=2, aR=14, fR=18, aL=-10, fL=0, tF=-16, sF=-22, tB=22, sB=10, hips=-8)
    pass_a = pose(lean=4, head=2, hy=-1, aR=2, fR=8, aL=2, fL=8, tF=4, sF=-24, tB=-2, sB=-6)
    pass_b = pose(lean=4, head=2, hy=-1, aR=2, fR=8, aL=2, fL=8, tF=-2, sF=-6, tB=4, sB=-24)
    draw = pose(lean=-10, head=-8, aR=-60, fR=-110, aL=40, fL=70, tF=10, sF=4, tB=-24, sB=-30, twist=-20)
    throw = pose(lean=14, head=-6, hx=6, aR=150, fR=140, aL=-30, fL=-10, tF=34, sF=6, tB=-30, sB=-36, twist=24)
    after = pose(lean=10, head=-4, hx=6, aR=100, fR=90, aL=-20, fL=0, tF=30, sF=6, tB=-28, sB=-34, twist=14)
    wind = pose(lean=-4, head=-6, aR=20, fR=160, aL=10, fL=30, tF=10, sF=4, tB=-16, sB=-22, twist=-30, armY=40)
    lash = pose(lean=12, head=-10, hx=4, aR=90, fR=95, aL=-20, fL=0, tF=26, sF=6, tB=-24, sB=-30, twist=20)
    glide = pose(lean=-16, head=6, hx=-6, aR=-30, fR=-20, aL=30, fL=40, tF=20, sF=10, tB=-30, sB=-20)
    land = pose(lean=4, head=-4, aR=20, fR=70, aL=-4, fL=10, tF=14, sF=0, tB=-14, sB=-20)
    lift = pose(lean=-2, head=-4, aR=60, fR=120, aL=0, fL=20, tF=4, sF=0, tB=-4, sB=-8)
    drink = pose(lean=-8, head=-26, aR=110, fR=175, aL=0, fL=20, tF=4, sF=0, tB=-4, sB=-8)
    flinch = pose(lean=-8, head=-14, hx=-3, aR=10, fR=40, aL=-10, fL=20, tF=8, sF=2, tB=-16, sB=-10)
    kneel = pose(lean=10, head=18, hy=17, aR=10, fR=20, aL=0, fL=10, tF=90, sF=-10, tB=80, sB=-20)
    fallen = pose(lean=76, head=16, hy=30, hx=10, aR=70, fR=60, aL=60, fL=50, tF=90, sF=0, tB=86, sB=-10)
    return {
        "idle": [(0, stand), (30, stand_b), (60, stand)],
        "walk": [(0, walk_a), (6, pass_a), (12, walk_b), (18, pass_b), (24, walk_a)],
        "throw": [(0, stand), (12, draw), (14, throw), (20, after), (30, stand)],
        "needles": [(0, stand), (7, wind), (8, lash), (16, lash), (22, stand)],
        "glide": [(0, stand), (4, glide), (10, glide), (16, land)],
        "drink": [(0, stand), (8, lift), (12, drink), (28, drink), (32, stand)],
        "flinch": [(0, stand), (3, flinch), (10, stand)],
        "death": [(0, stand), (10, kneel), (20, fallen)],
    }


CLIPS = _clips() if "bpy" in __import__("sys").modules else {}
