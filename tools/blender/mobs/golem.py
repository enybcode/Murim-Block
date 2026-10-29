"""Gardien-talisman (the Iron Golem's new look; design sheet: docs/mobs/gardien-talisman.png).

The stone statue of a general that guarded the village temple's gate, woken by a Taoist ward: a yellow paper talisman
pasted on its forehead, hanging over its face. Grey-green stone carved as lamellar armour, lion-head pauldrons, a small
helmeted head sunk between huge shoulders, arms down to the knees with iron chains wound round the forearms and bronze
bells hanging from them. Runes carved in the chest and arms glow gold (the emissive layer). Moss and cracks.

Its skeleton is the biped's, joint for joint, set to the golem's build (2.7 blocks): short legs of 13 pixels, a deep
trunk of 20, arms of 27. The talisman has a joint of its own so the mod can tear it off when the golem dies.
"""

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

HIP, KNEE = 13, 7
WAIST = 19
SHOULDER, ELBOW = 31, 18
NECK = 32
ARM_X = 11.5

# name, parent, model position at rest: murim_biped's joints, set to the golem's build.
JOINTS = [
    ("Root", None, (0, HIP, 0)),
    ("Torso", "Root", (0, HIP + 1, 0)),
    ("Chest", "Torso", (0, WAIST, 0)),
    ("Head", "Chest", (0, NECK, 0)),
]
for _side, _x in (("L", 1), ("R", -1)):
    JOINTS += [
        (f"Thigh_{_side}", "Root", (3.5 * _x, HIP, 0)),
        (f"Leg_{_side}", f"Thigh_{_side}", (3.5 * _x, KNEE, 0)),
        (f"Knee_{_side}", f"Thigh_{_side}", (3.5 * _x, KNEE, 0)),
        (f"Shoulder_{_side}", "Chest", (0, NECK, 0)),
        (f"Arm_{_side}", f"Shoulder_{_side}", (ARM_X * _x, SHOULDER, 0)),
        (f"Hand_{_side}", f"Arm_{_side}", (ARM_X * _x, ELBOW, 0)),
        (f"Tool_{_side}", f"Hand_{_side}", (ARM_X * _x, 4, 0)),
        (f"Elbow_{_side}", f"Arm_{_side}", (ARM_X * _x, ELBOW, 0)),
    ]
JOINTS.append(("Talisman", "Head", (0, NECK + 8, 5)))
HELPERS = [("Elbow_L", "Hand_L"), ("Elbow_R", "Hand_R"), ("Knee_L", "Leg_L"), ("Knee_R", "Leg_R")]

# Renders: body centre, face centre, ortho scales, aspect.
VIEW = ((0, 22, 0), (0, 36, 2), 56, 18, 1.1)
STAND = {}
# The sky fist: both arms thrown up, the chest open, leaning back.
SHOW_POSE = {
    "Arm_R": (-165, 12), "Hand_R": (-10, 0), "Arm_L": (-165, -12), "Hand_L": (-10, 0),
    "Chest": (-10, 0), "Head": (-14, 0), "Thigh_L": (-14, 0), "Leg_L": (10, 0), "Thigh_R": (10, 0),
}

STONE = hexes("#4d5249", "#6b7066", "#7d8277", "#8f9488", "#a9ada2")
DARK = hexes("#33372f", "#41463d", "#4f544a", "#5f645b", "#6f7469")
MOSS = hexes("#3c4724", "#4f5c2e", "#6f7d4a", "#86955a", "#9cab6a")
PAPER = hexes("#9c7f24", "#c9a530", "#e8c547", "#f2d665", "#fae79a")
INK = (176, 42, 30)
IRON = hexes("#1e1e22", "#2c2c31", "#3e3e44", "#55555c", "#6e6e76")
BRONZE = hexes("#5a4119", "#7a5c22", "#9a7530", "#b08a3a", "#d0aa5a")
RUNE = (255, 207, 90)
RUNE_DIM = (200, 142, 40)
EYE = (255, 196, 80)
CRACK = (52, 56, 48)


def flat(ramp, salt, offset=0, rough=0.1):
    return lambda face, x, y, w, h: shade(ramp, face, x, y, h, salt, rough, offset)


def stone(salt, offset=0):
    """Weathered stone: grain, a few dark cracks, moss gathering on top faces and low on the sides."""
    def paint(face, x, y, w, h):
        n = noise(x, y, salt + 7)
        if n < 0.035:
            return CRACK
        if face == "top" and noise(x, y, salt + 9) < 0.3:
            return MOSS[2] if n < 0.5 else MOSS[3]
        if face not in ("top", "bottom") and y >= h - 2 and noise(x, y, salt + 11) < 0.22:
            return MOSS[1]
        return shade(STONE, face, x, y, h, salt, 0.16, offset)
    return paint


# Head: a heavy brow, two amber eye slits glowing either side of the talisman, a stern shut mouth.
FACE = [
    "SSSSSSSS",
    "SSSSSSSS",
    "BBBSSBBB",
    "KEESSEEK",
    "SSSSSSSS",
    "SSSNNSSS",
    "SSSSSSSS",
    "SKMMMMKS",
    "SSSSSSSS",
    "DSSSSSSD",
]


def head(face, x, y, w, h):
    if face == "front":
        c = FACE[y][x]
        if c == "E":
            return EYE
        if c == "B":
            return STONE[1]
        if c in "KN":
            return STONE[2]
        if c == "M":
            return DARK[1]
        if c == "D":
            return STONE[1]
    return stone(13)(face, x, y, w, h)


def head_glow(face, x, y, w, h):
    if face == "front" and FACE[y][x] == "E":
        return EYE + (255,)
    return None


def helmet(face, x, y, w, h):
    if face == "front" and y == h - 1:
        return DARK[2]
    return shade(DARK, face, x, y, h, 17, 0.12, 1)


def talisman(face, x, y, w, h):
    """Yellow paper, red ink: a seal at the top, a stroke down the middle, the ward's sign below."""
    if face in ("front", "back"):
        if face == "front":
            if y == 1 and x in (0, 2) or y in (0, 2) and x == 1:
                return INK
            if x == 1 and 3 <= y <= 5:
                return INK
            if y == 7 and x != 1 or y == 8 and x == 1:
                return INK
        if y == h - 1 and x == 1:
            return None
        return PAPER[2] if noise(x, y, 19) > 0.15 else PAPER[1]
    return PAPER[1]


def rune(dx, dy):
    """The ward sign in the chest disc: three bars and a stroke down through them."""
    return abs(dx) < 0.6 and abs(dy) <= 2 or dy in (-2, 0, 2) and abs(dx) < 2


# Trunk -------------------------------------------------------------------------------------------------------

def chest(face, x, y, w, h):
    if face == "front":
        # The rune disc high on the chest.
        dx, dy = x - (w - 1) / 2, y - 4
        r = dx * dx + dy * dy
        if r <= 22:
            return DARK[2]
        # Below the disc, a smooth breastplate: one carved line across, worn smooth by the rain.
        if y >= 8:
            return STONE[2] if y == 9 else shade(STONE, face, x, y, h, 24, 0.04, 0)
    if face == "back" and y >= 3 and (x + y) % 5 == 0:
        return STONE[2]
    return stone(23)(face, x, y, w, h)


def chest_glow(face, x, y, w, h):
    if face == "front":
        dx, dy = x - (w - 1) / 2, y - 4
        r = dx * dx + dy * dy
        pass
    return None


def belt(face, x, y, w, h):
    if face != "top" and face != "bottom" and y in (0, h - 1):
        return DARK[1]
    return shade(DARK, face, x, y, h, 29, 0.06, 1)


def waist(face, x, y, w, h):
    return shade(STONE, face, x, y, h, 31, 0.05, -1)


def tablet(face, x, y, w, h):
    """The apron hanging from the belt: a smooth plate with a carved border."""
    if face == "front" and (x in (0, w - 1) or y == h - 1):
        return STONE[1]
    return shade(STONE, face, x, y, h, 33, 0.04, 0)


# The beast face on the belt buckle, as on a general's armour: brows, two eyes, a mouth biting the belt.
BEAST = [
    "BBBBBBB",
    "BEBBBEB",
    "BBBNBBB",
    "BMMMMMB",
    "BBMBMBB",
]


def beast(face, x, y, w, h):
    if face != "front":
        return BRONZE[2]
    c = BEAST[y][x]
    return BRONZE[0] if c == "E" else BRONZE[1] if c == "M" else BRONZE[4] if c == "N" else BRONZE[3]


def pauldron(face, x, y, w, h):
    if face in ("right", "left", "front") and y == 2 and x in (1, w - 2):
        return DARK[1]  # the lion's eyes
    if face in ("right", "left", "front") and y == 4 and 1 <= x <= w - 2:
        return DARK[2]  # its mouth
    return shade(STONE, face, x, y, h, 37, 0.14, 0)


# Limbs -------------------------------------------------------------------------------------------------------

def arm(face, x, y, w, h):
    # A rune carved down the outside of the upper arm.
    if face in ("right", "left") and x == w // 2 and 3 <= y <= 9:
        return RUNE_DIM
    if face in ("right", "left") and y in (4, 6, 8) and 1 <= x <= w - 2:
        return RUNE_DIM
    return stone(41)(face, x, y, w, h)


def arm_glow(face, x, y, w, h):
    if face in ("right", "left") and (x == w // 2 and 3 <= y <= 9 or y in (4, 6, 8) and 1 <= x <= w - 2):
        return RUNE + (255,)
    return None


def fist(face, x, y, w, h):
    if face == "front" and y in (1, 3) and 0 < x < w - 1:
        return STONE[1]  # knuckles
    return stone(43, 1)(face, x, y, w, h)


def chain(face, x, y, w, h):
    return IRON[3] if (x + y) % 2 else IRON[2]


def bell(face, x, y, w, h):
    if face == "bottom":
        return BRONZE[0]
    if face == "top":
        return BRONZE[2]
    return BRONZE[4] if (x, y) == (0, 0) else BRONZE[3] if y < h - 1 else BRONZE[1]


def leg(face, x, y, w, h):
    return stone(47)(face, x, y, w, h)


def foot(face, x, y, w, h):
    return shade(DARK, face, x, y, h, 49, 0.14, 1)


def rune_disc(face, x, y, w, h):
    """The raised disc on the chest: a gold ring at its rim, the ward sign in its middle."""
    if face != "front":
        return shade(DARK, face, x, y, h, 51, 0.1, 1)
    c = (w - 1) / 2
    if (x - c) ** 2 + (y - c) ** 2 >= 7:
        return DARK[0] if (x + y) % 2 else DARK[1]
    return RUNE_DIM if disc_lit(x, y, w) else STONE[1]


def rune_disc_glow(face, x, y, w, h):
    return RUNE + (255,) if face == "front" and disc_lit(x, y, w) else None


def disc_lit(x, y, w):
    c = (w - 1) / 2
    dx, dy = x - c, y - c
    r = dx * dx + dy * dy
    return r < 7 and (abs(dx) < 0.5 and abs(dy) <= 2 or dy in (-2, 0, 2) and abs(dx) <= 1)



def moss(face, x, y, w, h):
    return MOSS[3] if noise(x, y, 57) < 0.4 else MOSS[2] if face == "top" else MOSS[1]


def limb(side, upper, lower, helper, hinge, opens):
    return ("limb", f"{upper}_{side}", f"{lower}_{side}", f"{helper}_{side}", hinge, opens)


def parts():
    p = [
        # Head, sunk forward between the shoulders, under a flat stone helmet; the talisman over its face.
        Part("head", "Head", (-4, NECK - 1, -3), (8, 10, 8), head, emissive=head_glow),
        # A general's helmet: a domed cap, cheek guards down the sides of the face, a crest from front to back.
        Part("helmet", "Head", (-4.5, NECK + 7, -3.5), (9, 2, 9), helmet, inflate=0.2),
        Part("helmet_dome", "Head", (-3.5, NECK + 9, -2.5), (7, 1, 7), helmet),
        Part("helmet_crest", "Head", (-0.5, NECK + 9.5, -3.5), (1, 2, 9), helmet),
        Part("cheek_r", "Head", (-5, NECK + 1, -1), (1, 6, 6), helmet),
        Part("cheek_l", "Head", (4, NECK + 1, -1), (1, 6, 6), helmet),
        Part("brow", "Head", (-4, NECK + 5.5, 4.6), (8, 1, 1), stone(15, -1)),
        Part("jaw", "Head", (-3, NECK - 1, 4.4), (6, 2, 1), stone(16)),
        Part("talisman", "Talisman", (-1.5, NECK + 0.5, 5.65), (3, 9, 1), talisman, inflate=-0.35),
        # Trunk: the huge chest carved as armour, a belt, the waist, a stone apron at the front.
        Part("chest", "Chest", (-9, WAIST, -5.5), (18, 13, 11), chest, emissive=chest_glow),
        # In relief on the chest: the rune disc, three rows of lamellae under it, a ridge down the back.
        Part("rune_disc", "Chest", (-3.5, WAIST + 6.5, 5.3), (7, 7, 1), rune_disc, emissive=rune_disc_glow),
        Part("spine", "Chest", (-1.5, WAIST + 1, -6.3), (3, 11, 1), stone(25, -1)),
        Part("moss_back", "Chest", (2, WAIST + 9, -6.4), (4, 3, 1), moss),
        Part("moss_shoulder", "Chest", (-8, WAIST + 13, -3), (4, 1, 5), moss),
        Part("belt", "Torso", (-7.5, WAIST - 3, -4.5), (15, 3, 9), belt, inflate=0.3),
        Part("buckle", "Torso", (-3.5, WAIST - 4, 4.6), (7, 5, 1), beast),
        Part("waist", "Torso", (-7, HIP, -4), (14, 5, 8), waist),
        Part("tablet", "Torso", (-3.5, HIP - 5, 4.1), (7, 7, 1), tablet),
        Part("tasset_r", "Torso", (-7.5, HIP - 2, -3), (4, 4, 7), stone(35), inflate=0.2),
        Part("tasset_l", "Torso", (3.5, HIP - 2, -3), (4, 4, 7), stone(36), inflate=0.2),
    ]
    for side, x in (("R", -1), ("L", 1)):
        s = side.lower()
        ax = ARM_X * x - 2.5
        lx = 3.5 * x - 3
        p += [
            # Pauldron: a lion's head over the shoulder.
            Part(f"pauldron_{s}", "Chest", (ARM_X * x - 3, SHOULDER - 2, -4), (6, 5, 8), pauldron, inflate=0.3),
            Part(f"muzzle_{s}", "Chest", (ARM_X * x + (3.2 if x > 0 else -4.2), SHOULDER - 1, -1.5), (1, 3, 3),
                 pauldron),
            Part(f"mane_{s}", "Chest", (ARM_X * x - 3, SHOULDER + 3.2, -3), (6, 1, 6), stone(38, -1)),
            # The elbow guard and the knuckles stand out of the arm.
            Part(f"elbow_{s}", f"Arm_{side}", (ax - 0.5, ELBOW + 1, -3), (6, 3, 6), stone(39, -1)),
            Part(f"knuckles_{s}", f"Hand_{side}", (ax, 3.5, 3.1), (5, 2, 1), fist),
            # Arm to the knees, a big square fist; chains round the forearm and a bell hanging from them.
            Part(f"arm_{s}", limb(side, "Arm", "Hand", "Elbow", ELBOW, "back"), (ax, 7, -2.5), (5, 24, 5), arm,
                 emissive=arm_glow),
            Part(f"fist_{s}", f"Hand_{side}", (ax - 0.5, 1, -3), (6, 6, 6), fist),
            *[Part(f"chain_{s}_{i}", f"Hand_{side}", (ax - 0.5, 9 + 2.5 * i, -3), (6, 1, 6), chain, inflate=0.1)
              for i in range(3)],
            Part(f"bell_{s}", f"Hand_{side}", (ARM_X * x + (2.6 if x > 0 else -5.6), 8, -1.5), (3, 3, 3), bell),
            Part(f"bell_cord_{s}", f"Hand_{side}", (ARM_X * x + (3.1 if x > 0 else -4.1), 11, -0.5), (1, 1, 1),
                 chain),
            # Leg: short and thick, a knee plate, a heavy dark foot.
            Part(f"leg_{s}", limb(side, "Thigh", "Leg", "Knee", KNEE, "front"), (lx, 1, -3), (6, HIP - 1, 6), leg),
            Part(f"knee_{s}", f"Leg_{side}", (lx + 0.5, KNEE - 2, 3.1), (5, 3, 1), stone(48, -1)),
            Part(f"foot_{s}", f"Leg_{side}", (lx - 0.5, 0, -3.5), (7, 2, 8), foot),
            Part(f"moss_foot_{s}", f"Leg_{side}", (lx + (3 if x > 0 else 0), 2, -2), (3, 1, 3), moss),
        ]
    return p
