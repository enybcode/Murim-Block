"""Bête des Cinq Venins (the cave spider's new look; design sheet: docs/mobs/bete-cinq-venins.png).

A gu of the Five Venoms sect, the last beast alive in a bronze jar of five: the Fileuse's skeleton and clips (drawn at
vanilla's cave spider size, 0.7), dressed in dark jade chitin. The five venoms (scorpion, snake, centipede, toad,
spider) are inlaid in gold on its abdomen between two gold bands; under it a glowing venom sac in a bronze cage. Its
legs are banded black and gold with bronze rings at the knees and green claws; its old master's bronze horn with a red
seal stands on its back; eight golden eyes, pale fangs dripping venom.
"""

import spider as S
from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

JOINTS, HELPERS, STAND, SHOW_POSE, VIEW = S.JOINTS, S.HELPERS, S.STAND, S.SHOW_POSE, S.VIEW

JADE = hexes("#0c1a13", "#13271c", "#1f3a2c", "#2f5a43", "#4a7d61")
DARK = hexes("#080d0a", "#0f1813", "#16231c", "#1f3127", "#2b4134")
GOLD = hexes("#6e5520", "#9a7a30", "#c8a446", "#e0c25a", "#f2dc86")
BRONZE = hexes("#3e2c10", "#5e4316", "#8a6a28", "#b8892d", "#d2a849")
VENOM = hexes("#1f6a2a", "#2f9a3e", "#4fc75e", "#6fe07a", "#c6ffcb")
SEAL = hexes("#4a1010", "#621616", "#a32020", "#b8302a", "#cc4a3a")
BONE = S.BONE
EYE, EYE_DIM = (255, 210, 58), (150, 110, 20)


def jade(salt, offset=0):
    def paint(face, x, y, w, h):
        # Pale jade glints along the plates' upper edges.
        if face not in ("top", "bottom") and y == 0:
            return JADE[4]
        if face == "top" and (x == 0 or x == w - 1):
            return JADE[3]
        return shade(JADE, face, x, y, h, salt, 0.15, offset)
    return paint


def head(face, x, y, w, h):
    if face == "front" and (x, y) in S.EYES:
        return EYE if (x, y) in S.MAIN_EYES else EYE_DIM
    if face == "front" and y == h - 1:
        return DARK[0]
    return shade(DARK, face, x, y, h, 3, 0.15, 1)


def head_glow(face, x, y, w, h):
    if face == "front" and (x, y) in S.EYES:
        return EYE + (255,) if (x, y) in S.MAIN_EYES else EYE_DIM + (90,)
    return None


def fang(face, x, y, w, h):
    # Pale bone stained green at the point.
    if y == h - 1:
        return VENOM[2]
    return shade(BONE, face, x, y, h, 5, offset=1 if y == 0 else 0)


def drip(face, x, y, w, h):
    return VENOM[4] if y == 0 else VENOM[3]


def abdomen(face, x, y, w, h):
    # Plates in rings along its length, darker underneath.
    if face == "bottom":
        return DARK[1]
    if face in ("right", "left") and x % 3 == 0:
        return JADE[1]
    if face == "top" and y % 3 == 0:
        return JADE[1]
    return shade(JADE, face, x, y, h, 7, 0.15)


def band(face, x, y, w, h):
    if face == "bottom":
        return None
    return GOLD[3] if y == 0 or face == "top" and x % 3 == 0 else shade(GOLD, face, x, y, h, 9, 0.15)


# The five venoms inlaid on the back, each a little gold glyph (3 by 3) on a dark plate: scorpion (its tail curled),
# snake (an S), centipede (a spine with legs), toad (a squat round), spider (a body with legs).
GLYPHS = [
    ["#.#", ".##", "##."],
    ["##.", ".#.", ".##"],
    ["###", ".#.", "###"],
    [".#.", "###", "#.#"],
    ["#.#", ".#.", "#.#"],
]


def glyph(n):
    def paint(face, x, y, w, h):
        if face == "top" and y < 3 and x < 3:
            return GOLD[4] if GLYPHS[n][y][x] == "#" else DARK[1]
        return GOLD[1] if face != "bottom" else DARK[0]
    return paint


def sac(face, x, y, w, h):
    # Glowing venom, see-through, brighter in the middle.
    r, g, b = VENOM[3] if noise(x, y, 11) < 0.6 else VENOM[2]
    return r, g, b, 215


def sac_glow(face, x, y, w, h):
    return VENOM[3] + (150,)


def bronze(salt):
    def paint(face, x, y, w, h):
        if noise(x, y, salt) < 0.12:
            return (60, 110, 80)  # verdigris
        return shade(BRONZE, face, x, y, h, salt, 0.15, offset=1 if face == "top" else 0)
    return paint


def horn(face, x, y, w, h):
    # The master's seal: a red plaque with a black stroke on the horn's front and back.
    if face in ("front", "back"):
        return (20, 12, 10) if x == w // 2 and 0 < y < h - 1 else SEAL[2]
    return SEAL[1]


def segment(salt, tip=False):
    def paint(face, x, y, w, h):
        # Boxes along X: banded black and gold down their length, a green claw at the tip.
        if tip and face in ("left", "right"):
            return VENOM[2]
        if face in ("front", "back", "top", "bottom") and x % 4 == 2:
            return GOLD[2] if face != "bottom" else GOLD[0]
        return shade(DARK, face, x, y, h, salt, 0.2, 1)
    return paint


def parts():
    p = [
        Part("thorax", "Root", (-4, 5, -3), (8, 6, 8), jade(17)),
        Part("thorax_ridge", "Root", (-3, 11, -2), (6, 1, 6), jade(19, 1)),
        # The master's horn: a bronze base, the horn, its red seal.
        Part("horn_base", "Root", (-1.5, 12, 0), (3, 1, 3), bronze(21)),
        Part("horn", "Root", (-1, 13, 0.5), (2, 2.5, 2), bronze(23)),
        Part("horn_tip", "Root", (-0.5, 15.5, 1), (1, 1.2, 1), bronze(25)),
        Part("horn_seal", "Root", (-0.75, 13.3, 2.5), (1.5, 2, 0.4), horn),
        Part("head", "Head", (-3, 5, 5), (6, 5, 4), head, emissive=head_glow),
        Part("brow", "Head", (-2.5, 10, 6), (5, 1, 2.5), jade(27, 1)),
        Part("fang_l", "Fang_L", (0.6, 3, 8), (1.5, 3, 1.2), fang),
        Part("fang_r", "Fang_R", (-2.1, 3, 8), (1.5, 3, 1.2), fang),
        Part("drip_l", "Fang_L", (1.1, 2, 8.4), (0.5, 1, 0.5), drip, emissive=drip),
        Part("drip_r", "Fang_R", (-1.6, 1.6, 8.3), (0.5, 1.2, 0.5), drip, emissive=drip),
        Part("abdomen", "Abdomen", (-5, 4, -16), (10, 9, 13), abdomen),
        Part("spinneret", "Abdomen", (-1.5, 6, -17), (3, 2, 1), bronze(29)),
        # Two gold bands around the abdomen, the five venoms inlaid between them.
        Part("band_front", "Abdomen", (-5, 4.5, -4.6), (10, 8.5, 1), band, inflate=0.35),
        Part("band_back", "Abdomen", (-5, 4.5, -14.5), (10, 8.5, 1), band, inflate=0.35),
        *[Part(f"venom_{n}", "Abdomen", (x, 13.1, z), (3, 0.5, 3), glyph(n))
          for n, (x, z) in enumerate([(-4, -13.3), (1, -13.3), (-1.5, -10.6), (-4, -7.9), (1, -7.9)])],
        # The venom sac under the abdomen, in a bronze cage.
        Part("sac", "Abdomen", (-3, 2.4, -13), (6, 2, 7), sac, emissive=sac_glow),
        *[Part(f"cage_{i}", "Abdomen", (-3.4, 2, z), (6.8, 2.6, 0.6), bronze(31 + i)) for i, z in
          enumerate((-13.3, -10.3, -7.3))],
        Part("cage_rail_l", "Abdomen", (2.8, 2, -13.3), (0.6, 0.6, 6.6), bronze(35)),
        Part("cage_rail_r", "Abdomen", (-3.4, 2, -13.3), (0.6, 0.6, 6.6), bronze(37)),
    ]
    for i, z in enumerate(S.LEG_Z, 1):
        for side, s in (("L", 1), ("R", -1)):
            x0 = lambda a, b: (a, b - a) if s > 0 else (-b, b - a)
            fx, fw = x0(3.5, 10.5)
            tx, tw = x0(10, 16.5)
            px, pw = x0(16, 21)
            rx, rw = x0(9.8, 10.9)
            p += [
                Part(f"leg{i}{side}_femur", f"Leg{i}{side}_hip", (fx, 7, z - 1), (fw, 2, 2), segment(41 + i)),
                Part(f"leg{i}{side}_tibia", f"Leg{i}{side}_knee", (tx, 7.25, z - 0.75), (tw, 1.5, 1.5),
                     segment(51 + i)),
                Part(f"leg{i}{side}_tarsus", f"Leg{i}{side}_foot", (px, 7.5, z - 0.5), (pw, 1, 1),
                     segment(61 + i, tip=True)),
                # A bronze ring at the knee.
                Part(f"leg{i}{side}_ring", f"Leg{i}{side}_knee", (rx, 6.95, z - 1.05), (rw, 2.1, 2.1), bronze(71 + i)),
            ]
    return p
