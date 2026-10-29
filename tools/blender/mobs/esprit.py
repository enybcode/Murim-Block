"""Esprit de l'Archer (the stray's new look; design sheet: docs/mobs/esprit-archer.png).

The soul of an imperial archer who froze to death at the pass he guarded: the Archer impérial's skeleton and clips,
but no body left. An empty hood with two eyes of ice in its dark; a torso and arms of pale, see-through mist with a
glowing heart; no legs: under the belt the mist hangs in a tail that thins out a quarter block above the ground, with
wisps that sway (cloth). Pieces of his frosted armour float on the mist: one pauldron, a few lamellae, the belt, the
bow-arm's bracer, the quiver on his back with frozen fletchings. Icicles hang under the hood's brow and the pauldron.
The mist is translucent in game (alpha in the texture); the eyes and the heart glow.
"""

import os

from archer import BRASS, LACQUER, LEATHER, RUST
from murim_mob import Part, arm, hexes, noise, shade

TEXTURE_WIDTH = 128

MIST = hexes("#4f86a8", "#6fa7c8", "#8fc3dc", "#b4dcee", "#d6eef8")
HOOD = hexes("#3a5568", "#4f6f84", "#62859b", "#7a9fb4", "#97b9cb")
FROST = hexes("#9cc4d6", "#b8d8e6", "#d2e8f1", "#e9f6fb", "#ffffff")
HEART = (232, 251, 255)
EYE, EYE_RIM = (127, 240, 255), (60, 170, 205)
VOID = (16, 24, 34)


def frosted(ramp, salt):
    """A ramp's colour under frost: rimed on top, a crust of ice on the upper row, frost flecks."""
    def paint(face, x, y, w, h):
        if face == "top":
            return FROST[3] if noise(x, y, salt) < 0.7 else FROST[2]
        if face == "bottom":
            return ramp[0]
        if y == 0 or noise(x, y, salt + 1) < 0.12:
            return FROST[2] if noise(x, y, salt + 2) < 0.5 else FROST[3]
        return shade(ramp, face, x, y, h, salt, 0.15, offset=-1)
    return paint


def mist(salt, alpha_top=200, alpha_bottom=200):
    """See-through mist: pale streaks drifting down, thinning from alpha_top to alpha_bottom."""
    def paint(face, x, y, w, h):
        t = (y + 0.5) / max(h, 1) if face not in ("top", "bottom") else (0 if face == "top" else 1)
        alpha = alpha_top + (alpha_bottom - alpha_top) * t
        n = noise(x, y, salt)
        level = 2 + (1 if face == "top" or y == 0 else 0) + (1 if n > 0.82 else -1 if n < 0.18 else 0)
        # Brighter streaks running down the mist.
        if face not in ("top", "bottom") and noise(x, 0, salt + 7) < 0.25:
            level += 1
        r, g, b = MIST[max(0, min(4, level))]
        return r, g, b, int(max(0, min(255, alpha - 30 * (n < 0.1))))
    return paint


# Head: a hood over nothing ----------------------------------------------------------------------------------------

def dark(face, x, y, w, h):
    """The dark inside the hood, with the eyes of ice."""
    if face == "front" and y == 3 and x in (1, 2, 4, 5):
        return EYE if x in (2, 4) else EYE_RIM
    return VOID


def eyes_glow(face, x, y, w, h):
    if face == "front" and y == 3 and x in (1, 2, 4, 5):
        return EYE if x in (2, 4) else EYE_RIM
    return None


def hood(salt, hem=True):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return HOOD[0]
        # Frost gathered on the crown and shoulders of the cloth.
        if face == "top" and noise(x, y, salt) < 0.55:
            return FROST[2]
        # The hem, lined with the archer's old red lacquer.
        if hem and face not in ("top", "bottom") and y == h - 1:
            return LACQUER[2]
        return shade(HOOD, face, x, y, h, salt, 0.15)
    return paint


def icicle(face, x, y, w, h):
    if face == "top":
        return None
    return FROST[4] if y == 0 else FROST[3] if y < h - 1 else FROST[1]


# Body -------------------------------------------------------------------------------------------------------------

def heart(face, x, y, w, h):
    return HEART


def heart_glow(face, x, y, w, h):
    return HEART


def lamella(salt):
    def paint(face, x, y, w, h):
        if face == "top":
            return FROST[3]
        if face == "bottom":
            return LACQUER[0]
        if y == h - 1 and x % 2 == 0:
            return BRASS[1]
        if noise(x, y, salt) < 0.2:
            return FROST[2]
        return shade(LACQUER, face, x, y, h, salt, 0.15, offset=-1)
    return paint


def belt(face, x, y, w, h):
    if face == "top":
        return FROST[2]
    if face == "bottom":
        return LEATHER[0]
    return LEATHER[3] if y == 0 else shade(LEATHER, face, x, y, h, 61)


def buckle(face, x, y, w, h):
    if face == "front" and 0 < x < w - 1 and 0 < y < h - 1:
        return LEATHER[1]
    return FROST[3] if y == 0 else BRASS[2]


def bracer(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    if y in (0, h - 1):
        return BRASS[1]
    return FROST[2] if noise(x, y, 63) < 0.15 else shade(LEATHER, face, x, y, h, 65, 0.15, offset=1)


def quiver(face, x, y, w, h):
    if face == "top":
        return LEATHER[0]
    if y in (1, h - 2):
        return BRASS[1]
    return FROST[2] if noise(x, y, 67) < 0.1 else shade(LEATHER, face, x, y, h, 69, 0.18, offset=1)


def frozen_fletch(face, x, y, w, h):
    if y == h - 1:
        return LEATHER[3]
    return FROST[4] if y == 0 else shade(FROST, face, x, y, h, 71, 0.2, offset=1)


def wisp(salt):
    """A wisp of the tail (cloth): fading to nothing at its end."""
    def paint(face, x, y, w, h):
        alpha = 170 * (1 - (y + 0.5) / h) ** 0.8
        if alpha < 20:
            return None
        r, g, b = MIST[3] if noise(x, y, salt) < 0.4 else MIST[2]
        return r, g, b, int(alpha)
    return paint


# The tail where the legs were: rings of mist narrowing and trailing back, thinner and fainter toward the ground.
TAIL = [
    # (bottom, height, half width, half depth, z shift, alpha at top, alpha at bottom)
    (9.0, 3.0, 3.6, 1.9, -0.2, 150, 130),
    (6.5, 2.5, 2.9, 1.6, -0.6, 130, 105),
    (4.6, 1.9, 2.1, 1.25, -1.0, 105, 75),
    (3.4, 1.2, 1.3, 0.9, -1.4, 75, 35),
]


def parts():
    p = [
        # Head: the dark inside, the hood around it (crown, sides, back, a deep brow), its point falling behind and
        # its mantle on the shoulders; icicles under the brow.
        Part("dark", "Head", (-3.5, 24.5, -3), (7, 7, 6.5), dark, emissive=eyes_glow),
        Part("hood_crown", "Head", (-4.5, 31.5, -4.5), (9, 1.5, 9), hood(41, False)),
        Part("hood_r", "Head", (-4.5, 23.5, -4.5), (1, 8, 9), hood(43)),
        Part("hood_l", "Head", (3.5, 23.5, -4.5), (1, 8, 9), hood(45)),
        Part("hood_back", "Head", (-3.5, 23.5, -4.5), (7, 8, 1), hood(47)),
        Part("hood_peak", "Head", (-2.5, 32.8, -3.5), (5, 0.8, 6), hood(59, False)),
        Part("hood_brow", "Head", (-3.5, 30.3, 3.3), (7, 1.4, 1.4), hood(49, False)),
        Part("hood_point", "Head", (-1.5, 30, -6.3), (3, 3, 2), hood(51, False)),
        Part("hood_tip", "Head", (-0.75, 28.2, -7.3), (1.5, 2.5, 1.4), hood(53, False)),
        Part("mantle", "Chest", (-5, 21.5, -3), (10, 2.5, 6), hood(55), inflate=0.2),
        *[Part(f"icicle_brow_{i}", "Head", (x, y, 4), (0.6, h, 0.6), icicle)
          for i, (x, y, h) in enumerate([(-3.2, 29.0, 1.3), (-1.4, 29.5, 0.8), (0.6, 28.8, 1.5), (2.5, 29.4, 0.9)])],
        # Trunk of mist and its heart, with the armour that floats on it: lamellae, the belt and buckle, the quiver.
        Part("mist_trunk", "trunk", (-4, 12, -2), (8, 9.5, 4), mist(57, 175, 150)),
        Part("heart", "Chest", (-1.5, 16.5, -1), (3, 3, 2), heart, emissive=heart_glow),
        Part("lamella_1", "Chest", (-4.2, 19.8, 2.1), (4.6, 1.3, 0.7), lamella(73)),
        Part("lamella_2", "Chest", (1.0, 19.4, 2.1), (3.2, 1.3, 0.7), lamella(75)),
        Part("lamella_3", "Chest", (-3.6, 17.6, 2.1), (2.6, 1.1, 0.7), lamella(77)),
        Part("lamella_4", "Torso", (1.2, 15.6, 2.1), (2.8, 1.1, 0.7), lamella(79)),
        Part("lamella_back", "Chest", (-3.0, 18.5, -2.8), (6.0, 1.3, 0.7), lamella(81)),
        Part("belt", "trunk", (-4, 12, -2), (8, 1.6, 4), belt, inflate=0.45),
        Part("buckle", "Torso", (-1, 12.2, 2.2), (2, 2, 1), buckle),
        Part("quiver", "Chest", (0.5, 13, -4.8), (3, 9, 2), quiver),
        *[Part(f"fletch_{i}", "Chest", (0.5 + i, 22, -4.3 + 0.2 * i), (1, 2, 1), frozen_fletch) for i in range(3)],
        # The right pauldron alone, frosted, with icicles hanging from its rim; the bow arm's bracer.
        Part("pauldron_r", "Arm_R", (-7.5, 21, -2.5), (4, 3, 5), frosted(RUST, 83)),
        *[Part(f"icicle_pauldron_{i}", "Arm_R", (x, 20.0 - 0.2 * i, z), (0.6, h, 0.6), icicle)
          for i, (x, z, h) in enumerate([(-7.2, 1.4, 1.2), (-7.3, -0.4, 0.8), (-7.1, -1.8, 1.4)])],
        Part("bracer_r", "Hand_R", (-6.25, 13, -1.25), (2.5, 4, 2.5), bracer, inflate=0.4),
        # Arms of mist.
        Part("mist_arm_r", arm("R"), (-6.25, 12, -1.25), (2.5, 12, 2.5), mist(85, 165, 120)),
        Part("mist_arm_l", arm("L"), (3.75, 12, -1.25), (2.5, 12, 2.5), mist(87, 165, 120)),
    ]
    # No legs: the tail hangs from the trunk.
    for i, (bottom, height, hw, hd, dz, a0, a1) in enumerate(TAIL):
        p.append(Part(f"tail_{i}", "Torso", (-hw, bottom, -hd + dz), (2 * hw, height, 2 * hd), mist(89 + 2 * i, a0, a1)))
    # Wisps that sway behind and in front, longer at the back, as the tail streams after him.
    for i, (x, length, side) in enumerate([(-2.2, 9, "back"), (0.2, 11, "back"), (2.3, 8, "back"),
                                           (-1.4, 6, "front"), (1.6, 5, "front")]):
        z = -2.1 if side == "back" else 2.1
        p.append(Part(f"wisp_{i}", "Torso", (x, 11.5, z), (2, length, 1), wisp(95 + i), cloth=side))
    if os.environ.get("MURIM_PREVIEW_CLOTH"):
        # For the renders only: the wisps hanging straight, as they rest in game.
        for part in [q for q in p if q.cloth]:
            w, length = part.size[0], part.size[1]
            x, y, z = part.corner
            p.append(Part(part.name + "_preview", part.bind, (x - w / 2, y - length, z - 0.5), (w, length, 1), part.paint))
    return p
