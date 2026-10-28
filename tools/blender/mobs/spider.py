"""Fileuse de linceuls (the spider's new look; design sheet: docs/mobs/fileuse-linceuls.png).

A tomb spider that weaves the shrouds of the dead and wears them on its back, with the red paper seals torn from the
coffins. Its own skeleton: the cephalothorax (Root), the abdomen behind it, the head in front with two fangs, and
eight legs of three segments each, laid straight out to the sides at rest (as vanilla's spider); STAND arches them
for the renders. The shroud bands are in relief here; their torn ends will hang as cloth with the clips.
"""

from murim_mob import Part, hexes, noise, shade

TEXTURE_WIDTH = 128

# name, parent, model position (x left, y up, z forward; pixels). Parents before children.
JOINTS = [
    ("Root", None, (0, 8, 0)),
    ("Abdomen", "Root", (0, 9, -3)),
    ("Head", "Root", (0, 8, 5)),
    ("Fang_L", "Head", (1.5, 6, 8)),
    ("Fang_R", "Head", (-1.5, 6, 8)),
]
LEG_Z = [3.5, 1.5, -0.5, -2.5]
for _i, _z in enumerate(LEG_Z, 1):
    for _side, _x in (("L", 1), ("R", -1)):
        JOINTS += [
            (f"Leg{_i}{_side}_hip", "Root", (4 * _x, 8, _z)),
            (f"Leg{_i}{_side}_knee", f"Leg{_i}{_side}_hip", (10 * _x, 8, _z)),
            (f"Leg{_i}{_side}_foot", f"Leg{_i}{_side}_knee", (16 * _x, 8, _z)),
        ]
HELPERS = []

# Standing: the legs raised at the hip, bent down at the knee and the foot, fanned forward and back.
SPREAD = [38, 13, -13, -38]
STAND = {}
for _i, _spread in enumerate(SPREAD, 1):
    for _side, _s in (("L", 1), ("R", -1)):
        STAND[f"Leg{_i}{_side}_hip"] = (0, -_s * _spread, _s * 38)
        STAND[f"Leg{_i}{_side}_knee"] = (0, 0, -_s * 95)
        STAND[f"Leg{_i}{_side}_foot"] = (0, 0, -_s * 30)
STAND["Root"] = (0, 0, 0)
# Rearing to spit its web: the front legs raised, the body tilted up.
SHOW_POSE = dict(STAND)
SHOW_POSE["Root"] = (-22, 0, 0)
for _side, _s in (("L", 1), ("R", -1)):
    SHOW_POSE[f"Leg1{_side}_hip"] = (0, -_s * 50, _s * 70)
    SHOW_POSE[f"Leg1{_side}_knee"] = (0, 0, -_s * 60)
    SHOW_POSE[f"Leg2{_side}_hip"] = (0, -_s * 20, _s * 55)

VIEW = ((0, 8, -4), (0, 8, 8), 46, 18, 0.8)

CHITIN = hexes("#0f0d0c", "#1a1715", "#252019", "#352e27", "#4a4038")
SHEEN = hexes("#3d3530", "#4f4640", "#665b52", "#7d7068", "#958a80")
SHROUD = hexes("#8f8670", "#b3aa92", "#cfc8b4", "#e3ddcb", "#f1ecdc")
DIRTY = hexes("#6b6252", "#837966", "#a39a82", "#b7ad95", "#c9c0a8")
SEAL = hexes("#4a1010", "#621616", "#7a2020", "#962a26", "#b13a30")
INK = (30, 18, 14)
BONE = hexes("#8f8670", "#b3aa92", "#cfc5ae", "#e2dbc6", "#f1ecdc")
EYE, EYE_DIM = (230, 40, 30), (96, 16, 14)


def chitin(salt, offset=0):
    def paint(face, x, y, w, h):
        # A sheen along the top edges, the hard plates' seams.
        if face != "bottom" and face != "top" and y == 0:
            return SHEEN[2]
        if face == "top" and (x == 0 or x == w - 1):
            return SHEEN[1]
        return shade(CHITIN, face, x, y, h, salt, 0.15, offset)
    return paint


# Eight eyes on the 6 by 5 face: the main pair in the middle, six smaller around it.
MAIN_EYES = {(2, 2), (3, 2)}
EYES = MAIN_EYES | {(1, 1), (4, 1), (0, 2), (5, 2), (1, 3), (4, 3)}


def head(face, x, y, w, h):
    if face == "front" and (x, y) in EYES:
        return EYE if (x, y) in MAIN_EYES else EYE_DIM
    if face == "front" and y == h - 1:
        return CHITIN[0]
    return chitin(3)(face, x, y, w, h)


def head_glow(face, x, y, w, h):
    if face == "front" and (x, y) in EYES:
        return EYE + (255,) if (x, y) in MAIN_EYES else EYE_DIM + (70,)
    return None


def fang(face, x, y, w, h):
    return BONE[1] if y == h - 1 else shade(BONE, face, x, y, h, 5, offset=1 if y == 0 else 0)


def abdomen(face, x, y, w, h):
    # A dark hourglass of plates under the shrouds, glossy ridges along its length.
    if face == "top" and x in (w // 2 - 1, w // 2):
        return SHEEN[1]
    if face in ("right", "left") and y % 3 == 0:
        return CHITIN[3]
    return shade(CHITIN, face, x, y, h, 7, 0.15)


def shroud(salt):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return None
        # Dirtier toward the ground, torn at the lower edge, stitched along the middle.
        if face in ("right", "left") and y == h - 1 and noise(x, salt, 9) < 0.5:
            return None
        if face in ("right", "left") and y > h * 0.6:
            return shade(DIRTY, face, x, y, h, salt, 0.2)
        if face == "top" and y == h // 2 and x % 2 == 0:
            return DIRTY[1]
        return shade(SHROUD, face, x, y, h, salt, 0.15)
    return paint


def torn_end(face, x, y, w, h):
    if y == h - 1 and x % 2 == 1:
        return None
    return shade(DIRTY, face, x, y, h, 11, 0.25)


def seal(face, x, y, w, h):
    # Red paper with a black brushed stroke down the middle.
    if face in ("top", "front") and x == w // 2 and 0 < y < h - 1:
        return INK
    return shade(SEAL, face, x, y, h, 13, 0.15, offset=1)


def spinneret(face, x, y, w, h):
    return shade(SHEEN, face, x, y, h, 15)


def thread(face, x, y, w, h):
    return SHROUD[4] if (x + y) % 2 == 0 else SHROUD[3]


def segment(salt, tip=False):
    def paint(face, x, y, w, h):
        # Leg segments are boxes along X: their "sides" run the leg's length. A pale band at each joint; the tip claw.
        if tip and face == "left" or tip and face == "right":
            return BONE[2]
        if face in ("front", "back", "top", "bottom") and (x == 0 or x == w - 1):
            return SHEEN[2]
        return shade(CHITIN, face, x, y, h, salt, 0.2)
    return paint


def parts():
    p = [
        Part("thorax", "Root", (-4, 5, -3), (8, 6, 8), chitin(17)),
        Part("thorax_ridge", "Root", (-3, 11, -2), (6, 1, 6), chitin(19, 1)),
        Part("head", "Head", (-3, 5, 5), (6, 5, 4), head, emissive=head_glow),
        Part("brow", "Head", (-2.5, 10, 6), (5, 1, 2.5), chitin(21, 1)),
        Part("fang_l", "Fang_L", (0.6, 3, 8), (1.5, 3, 1.2), fang),
        Part("fang_r", "Fang_R", (-2.1, 3, 8), (1.5, 3, 1.2), fang),
        Part("abdomen", "Abdomen", (-5, 4, -16), (10, 9, 13), abdomen),
        Part("spinneret", "Abdomen", (-1.5, 6, -17), (3, 2, 1), spinneret),
        Part("thread", "Abdomen", (-0.5, 6.5, -22), (1, 1, 5), thread),
        # Shroud bands wrapped over the abdomen (open underneath), their torn ends hanging, red seals on top.
        Part("shroud_1", "Abdomen", (-5, 8, -7), (10, 5, 2), shroud(23), inflate=0.45),
        Part("shroud_2", "Abdomen", (-5, 7, -11), (10, 6, 2), shroud(25), inflate=0.45),
        Part("shroud_3", "Abdomen", (-5, 9, -14.5), (10, 4, 1.5), shroud(27), inflate=0.45),
        Part("shroud_end_l", "Abdomen", (5.1, 3, -10.5), (1, 4, 1), torn_end),
        Part("shroud_end_r", "Abdomen", (-6.1, 4.5, -6.5), (1, 4, 1), torn_end),
        Part("shroud_tail", "Abdomen", (-1.5, 10, -18), (3, 1, 3), torn_end),
        Part("seal_1", "Abdomen", (-2.5, 13.3, -11.5), (2, 0.6, 3), seal),
        Part("seal_2", "Abdomen", (1, 13.3, -7.5), (2, 0.6, 3), seal),
    ]
    for i, z in enumerate(LEG_Z, 1):
        for side, s in (("L", 1), ("R", -1)):
            x0 = lambda a, b: (a, b - a) if s > 0 else (-b, b - a)
            fx, fw = x0(3.5, 10.5)
            tx, tw = x0(10, 16.5)
            px, pw = x0(16, 21)
            p += [
                Part(f"leg{i}{side}_femur", f"Leg{i}{side}_hip", (fx, 7, z - 1), (fw, 2, 2), segment(31 + i)),
                Part(f"leg{i}{side}_tibia", f"Leg{i}{side}_knee", (tx, 7.25, z - 0.75), (tw, 1.5, 1.5),
                     segment(41 + i)),
                Part(f"leg{i}{side}_tarsus", f"Leg{i}{side}_foot", (px, 7.5, z - 0.5), (pw, 1, 1),
                     segment(51 + i, tip=True)),
            ]
    return p


# Clips, from the validated storyboard (docs/mobs/boards/spider.png). Rotations are degrees about the model's axes
# (X left, Y up, Z forward): a hip's Z lifts its leg, its Y swings it forward (negative on the left), a knee's Z bends
# it down; the body's X pitches it (negative: nose up). Keys hold their joints until the next key that names them.
GROUP_A = {("1", "L"), ("2", "R"), ("3", "L"), ("4", "R")}


def legs(lift=None, swing=None, bend=None, foot=None, only=None):
    """The legs from the standing pose: lift (hip), swing (forward, degrees) and bend (knee), per leg number."""
    key = {}
    for i, spread in enumerate(SPREAD, 1):
        for side, s in (("L", 1), ("R", -1)):
            if only and (str(i), side) not in only and str(i) not in only:
                continue
            up = (lift or {}).get(i, 0)
            forward = (swing or {}).get(i, 0) if not callable(swing) else swing(i, side)
            key[f"Leg{i}{side}_hip"] = (0, -s * (spread + forward), s * (38 + up))
            key[f"Leg{i}{side}_knee"] = (0, 0, -s * (95 + (bend or {}).get(i, 0)))
            key[f"Leg{i}{side}_foot"] = (0, 0, -s * (30 + (foot or {}).get(i, 0)))
    return key


def body(pitch=0, roll=0, up=0, ahead=0, abdomen=0, fangs=0, head=0):
    return {"Root": {"rot": (pitch, 0, roll), "loc": (0, up, ahead)}, "Abdomen": (abdomen, 0, 0),
            "Head": (head, 0, 0), "Fang_L": (0, 0, fangs), "Fang_R": (0, 0, -fangs)}


def step(phase):
    """The walk: group A swings forward lifted while group B pushes back on the ground; phase 1 swaps them."""
    def swing(i, side):
        a = (str(i), side) in GROUP_A
        return 12 if a != phase else -12
    lifted = {i: 0 for i in range(1, 5)}
    key = legs(swing=swing)
    for i in range(1, 5):
        for side, s in (("L", 1), ("R", -1)):
            if ((str(i), side) in GROUP_A) != phase:
                key[f"Leg{i}{side}_hip"] = (0, key[f"Leg{i}{side}_hip"][1], s * 52)
    return key


def mid(phase):
    """Halfway through a step: every foot down, the legs under the body."""
    return legs()


REARED = {**body(pitch=-24, up=2, abdomen=-22, fangs=35, head=-8),
          **legs(lift={1: 60, 2: 30, 3: -14, 4: -26}, swing={1: 10, 2: 6}, bend={1: -40, 2: -20, 3: 10, 4: 18})}
SPAT = {**body(pitch=-12, up=1, ahead=3, abdomen=-12, fangs=50, head=10),
        **legs(lift={1: 30, 2: 12, 3: -4, 4: -10}, swing={1: 16}, bend={1: -25})}
CROUCHED = {**body(pitch=4, up=-2.5, abdomen=-14),
            **legs(lift={1: -12, 2: -12, 3: -12, 4: -12}, bend={1: 18, 2: 18, 3: 18, 4: 18})}
LUNGED = {**body(pitch=-6, up=0.5, ahead=6, fangs=55, head=-6),
          **legs(swing={1: 22, 2: 14, 3: 6}, lift={1: 10})}
BIT = {**body(pitch=4, up=-0.5, ahead=5, fangs=-5, head=8), **legs(swing={1: 18, 2: 12, 3: 4})}
DEEP = {**body(pitch=8, up=-3.5, abdomen=-18),
        **legs(lift={1: -16, 2: -16, 3: -20, 4: -24}, bend={1: 22, 2: 22, 3: 26, 4: 30})}
FLYING = {**body(pitch=-14, up=4, ahead=4, abdomen=10, fangs=45),
          **legs(lift={1: 70, 2: 50, 3: 30, 4: 10}, swing={1: 25, 2: 18, 3: 8}, bend={1: -50, 2: -40, 3: -20})}
LANDED = {**body(pitch=6, up=-2, ahead=4, fangs=0, head=10),
          **legs(swing={1: 20, 2: 12, 3: 4}, lift={1: -6, 2: -6}, bend={1: 10, 2: 10})}
ROLLING = {**body(roll=-80, up=2), **legs(lift={1: 20, 3: 20}, bend={1: 20, 2: 30, 3: 20, 4: 30})}
DEAD = {**body(roll=-180, up=5), **legs(lift={1: -70, 2: -60, 3: -60, 4: -70}, bend={1: 20, 2: 30, 3: 30, 4: 20},
                                        foot={1: 30, 2: 30, 3: 30, 4: 30})}
TWITCH = {**DEAD, **legs(lift={1: -60, 2: -66, 3: -52, 4: -70}, bend={1: 10, 2: 36, 3: 20, 4: 20},
                         foot={1: 30, 2: 30, 3: 30, 4: 30})}
CLIMB = {"Root": {"rot": (-80, 0, 0), "loc": (0, 6, -2)}}

CLIPS = {
    "stand": [(0, STAND), (20, STAND)],
    "idle": [(0, {**body(), **legs()}), (30, {**body(up=0.4, abdomen=-4)}), (40, legs(lift={1: 12}, only={"1L"})),
             (44, legs(only={"1L"})), (60, {**body(), **legs()})],
    "walk": [(0, {**body(), **step(False)}), (3, mid(False)), (6, {**body(), **step(True)}), (9, mid(True)),
             (12, {**body(), **step(False)})],
    "climb": [(0, {**CLIMB, **step(False)}), (4, mid(False)), (8, {**CLIMB, **step(True)}), (12, mid(True)),
              (16, {**CLIMB, **step(False)})],
    "spit": [(0, {**body(), **legs()}), (8, REARED), (17, REARED), (18, SPAT), (22, SPAT), (30, {**body(), **legs()})],
    "bite": [(0, {**body(), **legs()}), (4, CROUCHED), (6, LUNGED), (7, BIT), (10, BIT), (16, {**body(), **legs()})],
    "leap": [(0, {**body(), **legs()}), (6, DEEP), (9, FLYING), (12, FLYING), (14, LANDED), (24, {**body(), **legs()})],
    "death": [(0, {**body(), **legs()}), (6, ROLLING), (10, DEAD), (14, TWITCH), (17, DEAD), (20, DEAD)],
}
