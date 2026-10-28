"""Archer impérial (the skeleton's new look; design sheet: docs/mobs/archer-imperial.png).

An archer of the old imperial guard, fallen holding a mountain pass: bare bones (thin limbs, as vanilla's skeleton)
inside the rusted lamellar armour he died in. A spiked helmet with a wide brim, a neck guard and a broken plume;
pauldrons standing off the shoulders; a breastplate of lacquered lamellae in brass bands, green with verdigris; a
leather belt with a brass buckle and a skirt of three panels; leather bracers and greaves, worn boots; a quiver on
his back with red fletchings showing. The bow is the item he holds.
"""

from murim_mob import Part, arm, hexes, leg, noise, shade

TEXTURE_WIDTH = 128

BONE = hexes("#8f8670", "#b3aa92", "#cdc5ae", "#e2dbc6", "#f1ecdc")
LACQUER = hexes("#3a1310", "#541c16", "#6b2b22", "#843a2d", "#9c4c3a")
BRASS = hexes("#5e4316", "#7f5d1f", "#a07626", "#b8892d", "#d2a849")
RUST = hexes("#3e1d14", "#5a2a1c", "#7d3a2c", "#97503a", "#b06a4c")
VERDIGRIS = hexes("#33402a", "#465538", "#5e6a3e", "#768350", "#90a066")
LEATHER = hexes("#21160f", "#2f2016", "#3b2a20", "#4c372a", "#5e4636")
WOOD = hexes("#2b180e", "#3c2214", "#4a2a18", "#5d3721", "#73472c")
FLETCH = hexes("#5a1414", "#741c1c", "#9a2b2b", "#b33a35", "#c95247")
FEATHER = hexes("#4a4740", "#625e55", "#7d786c", "#979182", "#b0aa99")
VOID, DARK = (18, 16, 14), (40, 34, 30)


def side_x(face, x, w):
    return x if face == "right" else w - 1 - x


# Head -----------------------------------------------------------------------------------------------------------

SKULL = [
    "LLLLLLLL",
    "SLLLLLLS",
    "SSSSSSSS",
    "SOOSSOOS",
    "SOOSSOOS",
    "DSSNNSSD",
    "DTVTVTVD",
    "DDSSSSDD",
]
SKULL_COLOURS = {"S": BONE[2], "L": BONE[3], "D": BONE[1], "O": VOID, "N": DARK, "T": BONE[3], "V": VOID}


def skull(face, x, y, w, h):
    if face == "front":
        return SKULL_COLOURS[SKULL[y][x]]
    if face in ("right", "left"):
        fx = side_x(face, x, w)
        # The cheekbone's hollow and the jaw's hinge.
        if y in (4, 5) and fx in (4, 5):
            return BONE[1]
        if y == 6 and fx == 5:
            return VOID
    if face == "bottom":
        return BONE[0]
    # Cracks on the crown.
    if face in ("top", "back") and noise(x, y, 5) < 0.08:
        return BONE[0]
    return shade(BONE, face, x, y, h, 3, 0.12)


def brim(face, x, y, w, h):
    if face == "bottom":
        return RUST[0]
    if face == "top":
        return RUST[2] if (x + y) % 5 == 0 else shade(RUST, face, x, y, h, 7, 0.2, offset=-1)
    return RUST[2] if x % 3 == 0 else RUST[1]


def dome(face, x, y, w, h):
    if face == "bottom":
        return None
    if face != "top" and y == h - 1:
        return BRASS[2] if x % 2 == 0 else BRASS[1]
    if noise(x, y, 11) < 0.15:
        return VERDIGRIS[2]
    return shade(LACQUER, face, x, y, h, 9, 0.15, offset=-1)


def cone(face, x, y, w, h):
    if face == "bottom":
        return None
    return shade(RUST, face, x, y, h, 13, 0.2)


def spike(face, x, y, w, h):
    return BRASS[4] if y == 0 else shade(BRASS, face, x, y, h, 15)


def plume(face, x, y, w, h):
    # Broken: dark and frayed at the top.
    if y == 0:
        return FLETCH[0]
    return shade(FLETCH, face, x, y, h, 17, 0.25)


def neck_guard(face, x, y, w, h):
    if face == "top":
        return None
    if y == h - 1 and face in ("back", "right", "left") and x % 2 == 0:
        return BRASS[2]
    return shade(LACQUER, face, x, y, h, 19, 0.15)


# Body -----------------------------------------------------------------------------------------------------------

def spine(face, x, y, w, h):
    """The bare spine and ribs under the armour (seen at the neck and the waist)."""
    if face == "front" and y % 2 == 1:
        return VOID
    return shade(BONE, face, x, y, h, 21, 0.1, offset=-1)


def lamellae(salt):
    def paint(face, x, y, w, h):
        if face in ("top", "bottom"):
            return LACQUER[1]
        # Rows of lacquered plates laced with brass bands every third row; verdigris where the lacing rotted.
        if y % 4 == 0:
            return BRASS[2] if x % 2 == 0 else BRASS[1]
        if noise(x, y, salt) < 0.1:
            return VERDIGRIS[2] if noise(x, y, salt + 1) < 0.5 else VERDIGRIS[1]
        if (x + (y // 4)) % 2 == 0 and y % 4 == 3:
            return LACQUER[0]
        return shade(LACQUER, face, x, y, h, salt, 0.12, offset=-1)
    return paint


def pauldron(face, x, y, w, h):
    if face == "bottom":
        return LACQUER[0]
    if face != "top" and y == h - 1:
        return BRASS[2]
    if noise(x, y, 25) < 0.18:
        return RUST[3]
    return shade(RUST, face, x, y, h, 23, 0.2)


def belt(face, x, y, w, h):
    if face in ("top", "bottom"):
        return LEATHER[1]
    return LEATHER[3] if y == 0 else shade(LEATHER, face, x, y, h, 27)


def buckle(face, x, y, w, h):
    if face == "front" and 0 < x < w - 1 and 0 < y < h - 1:
        return LEATHER[1]
    return BRASS[4] if face == "front" and y == 0 else BRASS[3]


def skirt(face, x, y, w, h):
    """A skirt panel of lamellae over a thigh, its hem cut in a point."""
    if face == "top":
        return None
    if face == "bottom":
        return LACQUER[0]
    if y == h - 1 and face in ("front", "back") and (x == 0 or x == w - 1):
        return None
    if y == 0:
        return BRASS[1]
    return shade(LACQUER, face, x, y, h, 29, 0.15, offset=-1)


def front_flap(face, x, y, w, h):
    if y == h - 1 and x in (0, w - 1):
        return None
    if y == 0:
        return BRASS[2]
    return shade(LACQUER, face, x, y, h, 31, 0.15, offset=-1)


def quiver(face, x, y, w, h):
    if face == "top":
        return LEATHER[0]
    if y in (1, h - 2):
        return BRASS[2]
    return shade(LEATHER, face, x, y, h, 33, 0.18, offset=1)


def fletching(face, x, y, w, h):
    if y == h - 1:
        return WOOD[3]
    return FLETCH[3] if y == 0 else shade(FLETCH, face, x, y, h, 35)


# Limbs ----------------------------------------------------------------------------------------------------------

def bone_limb(face, x, y, w, h):
    # A knob at the elbow or knee (the box's middle), darker joints.
    if y in (h // 2 - 1, h // 2):
        return BONE[3] if face != "bottom" else BONE[1]
    if face == "bottom":
        return BONE[1]
    return shade(BONE, face, x, y, h, 37, 0.1)


def bracer(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    if y in (0, h - 1):
        return BRASS[2]
    return LEATHER[3] if (x + y) % 3 == 0 else shade(LEATHER, face, x, y, h, 39, 0.15, offset=1)


def greave(face, x, y, w, h):
    if face in ("top", "bottom"):
        return None
    if face == "front" and x == w // 2:
        return BRASS[3]
    return shade(LEATHER, face, x, y, h, 41, 0.15, offset=1)


def boot(face, x, y, w, h):
    if face == "bottom":
        return LEATHER[0]
    if face == "top":
        return None
    return LEATHER[3] if y == 0 else shade(LEATHER, face, x, y, h, 43, 0.12)


def parts():
    return [
        # Head: the skull, then the helmet: brim, lacquered dome with its brass hem, the cone, the spike and the broken
        # plume, and the neck guard behind.
        Part("head", "Head", (-4, 24, -4), (8, 8, 8), skull),
        Part("brim", "Head", (-5, 30, -5), (10, 1, 10), brim),
        Part("dome", "Head", (-4, 31, -4), (8, 2, 8), dome, inflate=0.4),
        Part("cone", "Head", (-2, 33.4, -2), (4, 2, 4), cone),
        Part("spike", "Head", (-0.5, 35.4, -0.5), (1, 3, 1), spike),
        Part("plume", "Head", (-0.5, 35.4, -2.5), (1, 3, 1), plume),
        Part("neck_guard", "Head", (-4.5, 26, -5), (9, 4, 1), neck_guard),
        # Trunk: the bare spine under a lamellar breastplate, pauldrons on the shoulders, the belt and buckle, the
        # skirt's front flap.
        Part("spine", "trunk", (-1, 12, -1), (2, 12, 2), spine),
        Part("plastron", "trunk", (-4, 15, -2), (8, 9, 4), lamellae(45), inflate=0.3),
        Part("pauldron_r", "Arm_R", (-7.5, 21, -2.5), (4, 3, 5), pauldron),
        Part("pauldron_l", "Arm_L", (3.5, 21, -2.5), (4, 3, 5), pauldron),
        Part("belt", "trunk", (-4, 13, -2), (8, 2, 4), belt, inflate=0.45),
        Part("buckle", "Torso", (-1, 12.6, 2.2), (2, 2, 1), buckle),
        Part("front_flap", "Torso", (-1.5, 8.5, 2.3), (3, 4, 1), front_flap),
        Part("quiver", "Chest", (0.5, 13, -4.8), (3, 9, 2), quiver),
        *[Part(f"fletch_{i}", "Chest", (0.5 + i, 22, -4.3 + 0.2 * i), (1, 2, 1), fletching) for i in range(3)],
        # Arms: thin bones with leather bracers.
        Part("arm_r", arm("R"), (-6, 12, -1), (2, 12, 2), bone_limb),
        Part("arm_l", arm("L"), (4, 12, -1), (2, 12, 2), bone_limb),
        Part("bracer_r", "Hand_R", (-6, 13, -1), (2, 4, 2), bracer, inflate=0.45),
        Part("bracer_l", "Hand_L", (4, 13, -1), (2, 4, 2), bracer, inflate=0.45),
        # Legs: thin bones, the skirt's side panels over the thighs, greaves and boots.
        Part("leg_r", leg("R"), (-3, 0, -1), (2, 12, 2), bone_limb),
        Part("leg_l", leg("L"), (1, 0, -1), (2, 12, 2), bone_limb),
        Part("skirt_r", "Thigh_R", (-4.2, 8, -2), (3, 4, 4), skirt, inflate=0.2),
        Part("skirt_l", "Thigh_L", (1.2, 8, -2), (3, 4, 4), skirt, inflate=0.2),
        Part("greave_r", "Leg_R", (-3, 1.5, -1), (2, 4, 2), greave, inflate=0.45),
        Part("greave_l", "Leg_L", (1, 1.5, -1), (2, 4, 2), greave, inflate=0.45),
        Part("boot_r", "Leg_R", (-3, 0, -1.5), (2, 2, 3), boot, inflate=0.4),
        Part("boot_l", "Leg_L", (1, 0, -1.5), (2, 2, 3), boot, inflate=0.4),
    ]
