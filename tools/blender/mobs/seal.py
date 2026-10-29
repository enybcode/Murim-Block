"""Sceau de jade vivant (the slime's new look; design sheet: docs/mobs/sceau-jade.png).

The jade seal of a fallen emperor: the Mandate of Heaven stayed caught in the stone when the dynasty fell, and the seal
hops in search of a new master. Built at the big slime's size (2 blocks); the game scales it down for the smaller ones.
Its own skeleton: the block (Root), the crouching beast on top (Beast, its Head) and the silk tassel (Tassel). A gold
band round the block, a corner mended in gold, clouds carved in relief on the sides, white veins in the jade, and the
underside carved with 受命於天 in cinnabar. Only the beast's eyes glow.
"""

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

JOINTS = [
    ("Root", None, (0, 0, 0)),
    ("Beast", "Root", (0, 21, 0)),
    ("Head", "Beast", (-10, 24, 0)),
    ("Tassel", "Root", (14.6, 20.5, 6)),
]
HELPERS = []

STAND = {}
# Caught mid-hop, tipped back so its carved underside shows, the beast clinging on and looking down.
SHOW_POSE = {"Root": (-28, 25, 0), "Head": (14, -20, 0), "Tassel": (30, 0, 15)}
VIEW = ((0, 16, 0), (-10, 26, 2), 46, 18, 1.0)

JADE = hexes("#2f5e4c", "#4f8a6f", "#6fa58a", "#9fd0b4", "#c9e6d3")
VEIN = (232, 239, 224)
GOLD = hexes("#6b4c18", "#8a6a22", "#b0862e", "#d2a849", "#f0d48a")
CINNABAR = hexes("#5a0e10", "#8a1c1c", "#b3262a", "#d4433a", "#e8705a")
EYE, EYE_CORE = (200, 40, 36), (255, 120, 90)

# The four characters of the underside, read top to bottom, right column first: 受命 | 於天.
GLYPHS = {
    "shou": [
        ".XXXXXXXXX.",
        "...X..X..X.",
        ".X..X..X...",
        "XXXXXXXXXXX",
        "X.........X",
        ".XXXXXXX...",
        "...X...X...",
        "....X.X....",
        ".....X.....",
        "...XX.XX...",
        ".XX.....XX.",
    ],
    "ming": [
        ".....X.....",
        "....X.X....",
        "...X...X...",
        ".XXXXXXXXX.",
        "...........",
        ".XXX.XXXX..",
        ".X.X.X..X..",
        ".XXX.X..X..",
        ".....X.XX..",
        ".....X.....",
        ".....X.....",
    ],
    "yu": [
        ".X....XXX..",
        "XXXXX..X...",
        ".X....X.X..",
        ".XXXX.....X",
        ".X..X...X..",
        ".X..X..X...",
        ".X..X.....X",
        ".X..X....X.",
        "X...X...X..",
        "...XX..X...",
        "...........",
    ],
    "tian": [
        "...........",
        ".XXXXXXXXX.",
        ".....X.....",
        ".....X.....",
        "XXXXXXXXXXX",
        ".....X.....",
        "....X.X....",
        "...X...X...",
        "..X.....X..",
        ".X.......X.",
        "...........",
    ],
}
# Where each glyph starts on the 28 x 28 underside (x, y).
GLYPH_AT = {"shou": (15, 2), "ming": (15, 15), "yu": (2, 2), "tian": (2, 15)}


def vein(x, y, salt):
    """White veins running across the jade in long diagonal threads."""
    return (x * 2 + y * 3 + salt) % 29 == 0 and noise(x // 4, y // 4, salt) < 0.55


def jade(salt, offset=0, rough=0.035):
    def paint(face, x, y, w, h):
        if face != "bottom" and vein(x, y, salt):
            return VEIN
        return shade(JADE, face, x, y, h, salt, rough, offset)
    return paint


def underside(x, y):
    """The seal's face: cinnabar ground, a carved frame, the four characters standing in jade."""
    if x in (0, 27) or y in (0, 27):
        return JADE[1]
    if x == 1 or x == 26 or y == 1 or y == 26:
        return CINNABAR[2]
    for name, (gx, gy) in GLYPH_AT.items():
        if gx <= x < gx + 11 and gy <= y < gy + 11:
            return JADE[3] if GLYPHS[name][y - gy][x - gx] == "X" else CINNABAR[2 if (x + y) % 5 else 1]
    if x == 13 or x == 14 or y == 13 or y == 14:
        return JADE[3]
    return CINNABAR[2]


def block(face, x, y, w, h):
    if face == "bottom":
        return underside(x, y)
    # The ink that has soaked into the lowest row of the sides.
    if face not in ("top",) and y == h - 1:
        return CINNABAR[1]
    return jade(5)(face, x, y, w, h)


def step(face, x, y, w, h):
    return JADE[4] if face == "top" and (x + y) % 7 else jade(9, 1)(face, x, y, w, h)


def band(face, x, y, w, h):
    if face in ("top", "bottom"):
        return GOLD[2]
    # A bright upper edge, a dark lower one, a hammered grain between.
    if y == 0:
        return GOLD[4]
    if y == h - 1:
        return GOLD[1]
    return GOLD[3] if noise(x, y, 31) > 0.15 else GOLD[2]


def mend(face, x, y, w, h):
    """The corner broken off and mended in gold, a dark seam where it meets the jade."""
    if face in ("left", "back", "right", "front") and x == 0:
        return GOLD[0]
    return shade(GOLD, face, x, y, h, 33, 0.1, offset=1)


def cloud(face, x, y, w, h):
    return JADE[3] if face == "top" else shade(JADE, face, x, y, h, 13, 0.08, offset=-1)


def beast(salt, offset=0):
    def paint(face, x, y, w, h):
        return shade(JADE, face, x, y, h, salt, 0.05, offset)
    return paint


def spine(face, x, y, w, h):
    return JADE[0] if face != "top" else (JADE[1] if x % 2 else JADE[2])


def head(face, x, y, w, h):
    if face == "front":
        if y == 1 and x in (1, w - 2):
            return EYE
        if y == 0:
            return JADE[3]
    return shade(JADE, face, x, y, h, 17, 0.1, offset=1)


def head_glow(face, x, y, w, h):
    if face == "front" and y == 1 and x in (1, w - 2):
        return EYE_CORE + (230,)
    return None


def snout(face, x, y, w, h):
    if face == "front" and y == h - 1 and 0 < x < w - 1:
        return JADE[0]
    if face == "front" and y == 0 and x in (0, w - 1):
        return JADE[0]
    return shade(JADE, face, x, y, h, 19, 0.1)


def cord(face, x, y, w, h):
    return CINNABAR[1] if (y + x) % 3 == 0 else CINNABAR[2]


def threads(face, x, y, w, h):
    if face == "bottom":
        return CINNABAR[1]
    return CINNABAR[3] if (x + face.__len__()) % 2 and y < h - 1 else shade(CINNABAR, face, x, y, h, 23, 0.15)


def cap(face, x, y, w, h):
    return GOLD[4] if face == "top" or y == 0 else GOLD[2]


def on_side(side, u, v, w, h, depth=0.8):
    """A raised piece on one side of the 28-pixel block: u across the side, v up it."""
    if side == "front":
        return (u, v, 14), (w, h, depth)
    if side == "back":
        return (u, v, -14 - depth), (w, h, depth)
    if side == "left":
        return (14, v, u), (depth, h, w)
    return (-14 - depth, v, u), (depth, h, w)


def clouds(side):
    """Two cloud scrolls facing each other and a wave between them, below the gold band."""
    bars = [
        (-11, 9, 8, 1.5), (-11, 4, 1.5, 5), (-9.5, 4, 4.5, 1.5), (-6.5, 5.5, 1.5, 2),
        (3, 10, 8, 1.5), (9.5, 5, 1.5, 5), (5.5, 5, 4, 1.5), (5.5, 6.5, 1.5, 2),
        (-5, 2, 10, 1.5), (-5, 3.5, 1.5, 1.5), (3.5, 0.5, 1.5, 1.5),
    ]
    return [Part(f"cloud_{side}_{i}", "Root", *on_side(side, u, v, w, h), cloud) for i, (u, v, w, h) in enumerate(bars)]


def parts():
    p = [
        # The block, a step on top where the beast crouches, the gold band, the mended corner.
        Part("block", "Root", (-14, 0, -14), (28, 20, 28), block),
        Part("step", "Root", (-13, 20, -13), (26, 1, 26), step),
        Part("band", "Root", (-14, 13, -14), (28, 2, 28), band, inflate=0.35),
        Part("mend", "Root", (8.6, 15, 8.6), (5.6, 5.2, 5.6), mend, inflate=0.15),
        # The beast (a hornless dragon) crouched along the top: its body, a heavy haunch, a ridge down its back,
        # four paws gripping the step, its tail curling up behind.
        Part("body", "Beast", (-8, 21, -4), (12, 4.5, 8), beast(3)),
        Part("haunch", "Beast", (2, 21, -4.5), (5, 6, 9), beast(7, 1)),
        Part("ridge", "Beast", (-7, 25.5, -1), (12, 1, 2), spine),
        *[Part(f"paw_{i}", "Beast", (x, 21, z), (3, 2, 3), beast(11, 1))
          for i, (x, z) in enumerate(((-9.5, 2.5), (-9.5, -5.5), (5.5, 3.5), (5.5, -6.5)))],
        Part("tail_root", "Beast", (7, 22, -1.5), (3, 3, 3), beast(13)),
        Part("tail_up", "Beast", (8.5, 25, -1), (2, 4, 2), beast(13, 1)),
        Part("tail_curl", "Beast", (6, 29, -1), (4.5, 1.5, 2), beast(13, 1)),
        # The head, turned to face the front: a mane behind it, a square snout, swept-back horns; its eyes glow.
        Part("head", "Head", (-13.5, 23, -3), (7, 6, 7), head, emissive=head_glow),
        Part("mane", "Head", (-14, 23.5, -4.2), (8, 5, 1.5), beast(15, -1)),
        Part("snout", "Head", (-12, 23.4, 4), (4, 3, 2), snout),
        Part("horn_l", "Head", (-13, 29, -2.5), (1.5, 2, 3), beast(21, 1)),
        Part("horn_r", "Head", (-8.5, 29, -2.5), (1.5, 2, 3), beast(21, 1)),
        # The silk cord from the beast's neck to the edge, hanging down the side, a gold cap and a tassel.
        Part("cord_top", "Root", (-6, 21, 5.6), (20.6, 0.8, 0.8), cord),
        Part("cord_side", "Tassel", (14.2, 11, 5.6), (0.8, 10, 0.8), cord),
        Part("cap", "Tassel", (13.8, 9.5, 5.2), (1.6, 1.5, 1.6), cap),
        Part("tassel", "Tassel", (13.7, 3.5, 5.1), (1.8, 6, 1.8), threads),
    ]
    for side in ("front", "back", "left", "right"):
        p += clouds(side)
    return p
