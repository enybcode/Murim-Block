"""Clip sets for the biped, written as keyframes (Claude's first versions; Enzo fixes them by eye in Blender).

Each set becomes skeletal/animations/<set>.gltf (one glTF animation per clip, from a Blender action); its settings
(loop, transition, mask) are in the hand-written sidecar <set>.json next to it.

Keys are at 20 frames per second, one frame per game tick. A key maps joint names to a rotation in degrees about X,
then Y, then Z (bone axes are the mod's model axes: X left, Y up, Z forward), or to {"loc": (x, y, z)}, a move in
pixels from the rest, or to both: {"rot": (x, y, z), "loc": (x, y, z)}. Joints missing from a key keep the
previous key's value. Elbow and Knee follow by themselves (their constraint is baked into the export).
"""

import math

import bpy
from mathutils import Matrix, Quaternion, Vector

FPS = 20

# The boxing guard: hands up in front of the face, elbows in.
GUARD = {
    "Arm_L": (-70, -20, 0), "Hand_L": (-100, 0, 0),
    "Arm_R": (-50, 20, 0), "Hand_R": (-110, 0, 0),
}


def bent_legs(drop):
    """Thighs forward and shins back so the feet stay under the hips while the root sinks by `drop` pixels."""
    angle = math.degrees(math.acos((12 - drop) / 12))
    return {
        "Root": {"loc": (0, -drop, 0)},
        "Thigh_L": (-angle, 0, 0), "Leg_L": (2 * angle, 0, 0),
        "Thigh_R": (-angle, 0, 0), "Leg_R": (2 * angle, 0, 0),
    }


SETS = {
    "test": {
        # A boxer's bounce in guard: the legs carry it, twice a second.
        "bounce": [
            (0, {**GUARD, **bent_legs(0.5)}),
            (5, bent_legs(1.5)),
            (10, bent_legs(0.5)),
            (15, bent_legs(1.5)),
            (20, bent_legs(0.5)),
        ],
        # The right hand waves while the chest turns a little toward it.
        "wave": [
            (0, {"Arm_R": (-160, 0, 0), "Hand_R": (-20, 0, 0), "Chest": (0, 0, 0), "Head": (0, 0, 0)}),
            (10, {"Hand_R": (-70, 0, 0), "Chest": (0, -10, 0), "Head": (0, 10, 0)}),
            (20, {"Hand_R": (-20, 0, 0), "Chest": (0, 0, 0), "Head": (0, 0, 0)}),
            (30, {"Hand_R": (-70, 0, 0), "Chest": (0, -10, 0), "Head": (0, 10, 0)}),
            (40, {"Hand_R": (-20, 0, 0), "Chest": (0, 0, 0), "Head": (0, 0, 0)}),
        ],
    },
}


def mirror(key):
    """The same key with left and right swapped (as the mod's JointTransform.mirrored)."""
    result = {}
    for joint, value in key.items():
        name = joint[:-2] + {"_L": "_R", "_R": "_L"}[joint[-2:]] if joint[-2:] in ("_L", "_R") else joint
        if isinstance(value, dict):
            flipped = {}
            if "rot" in value:
                x, y, z = value["rot"]
                flipped["rot"] = (x, -y, -z)
            if "loc" in value:
                x, y, z = value["loc"]
                flipped["loc"] = (-x, y, z)
            result[name] = flipped
        else:
            x, y, z = value
            result[name] = (x, -y, -z)
    return result


def cycle(first, second, length):
    """A loop of two half cycles: `first`, `second`, then both mirrored, back to `first` at `length`."""
    quarter = length // 4
    return [(0, first), (quarter, second), (2 * quarter, mirror(first)), (3 * quarter, mirror(second)),
            (length, first)]


def half_cycle(keys, length):
    """A loop from the first half's keys [(frame, key)...] (frames under length / 2), then the same mirrored."""
    half = length // 2
    return keys + [(frame + half, mirror(key)) for frame, key in keys] + [(length, keys[0][1])]


# Signs, for a joint at rest (bone axes = model axes): a limb hanging down swings forward with a negative X
# (a forearm bends at the elbow with a negative X, a shin bends at the knee with a positive X), and a left limb moves
# out to the side with a positive Z (a right one with a negative Z). The torso, chest and head lean forward with a
# positive X and turn toward their left with a positive Y.

ARMS_DOWN = {"Arm_L": (0, 0, 4), "Arm_R": (0, 0, -4), "Hand_L": (-6, 0, 0), "Hand_R": (-6, 0, 0)}
STRAIGHT = {"Root": {"loc": (0, 0, 0)}, "Torso": (0, 0, 0), "Chest": (0, 0, 0), "Head": (0, 0, 0),
            "Thigh_L": (0, 0, 0), "Leg_L": (0, 0, 0), "Thigh_R": (0, 0, 0), "Leg_R": (0, 0, 0)}


def crouch(drop):
    """Sneaking: hips sunk by `drop`, knees forward, the torso leaning 24° and the head upright."""
    return {**bent_legs(drop), "Torso": (24, 0, 0), "Head": (-24, 0, 0),
            "Arm_L": (-30, 0, 6), "Arm_R": (-30, 0, -6), "Hand_L": (-15, 0, 0), "Hand_R": (-15, 0, 0)}


# Orthodox boxing stance (left foot ahead): hips turned 20° to the right, weight on the balls of the feet, the rear
# heel up; the lead hand out in front of the face, the rear hand at the chin. `low` sinks it for the bounce.
def stance(low):
    lead_thigh, lead_shin, rear_thigh, rear_shin, drop = (-28, 62, 12, 42, 1.8) if low else (-22, 50, 15, 30, 1.0)
    return {
        "Root": {"rot": (0, -20, 0), "loc": (0, -drop, 0)},
        "Thigh_L": (lead_thigh, 0, 6), "Leg_L": (lead_shin, 0, 0),
        "Thigh_R": (rear_thigh, 0, -6), "Leg_R": (rear_shin, 0, 0),
        "Torso": (6, 0, 0), "Chest": (4, -10, 0), "Head": (-10, 30, 0),
        "Arm_L": (-75, 0, 25), "Hand_L": (-105, 0, 0),
        "Arm_R": (-50, 0, -10), "Hand_R": (-130, 0, 0),
    }


SETS["biped"] = {
    # Standing: arms hang a little out, a slow sway of the arms and head.
    "idle": [
        (0, {**STRAIGHT, **ARMS_DOWN}),
        (30, {"Arm_L": (-3, 0, 6), "Arm_R": (3, 0, -6), "Head": (2, 3, 0)}),
        (60, {**ARMS_DOWN, "Head": (0, 0, 0)}),
    ],
    # Right foot lands ahead (hips at their lowest), passes under the body (highest), then the other foot reaches
    # forward; the second half is the first mirrored. Arms swing against the legs and the chest turns with them.
    # Minecraft walks at 4.3 blocks a second, a human jog: a step is 0.9 block (feet 7.7 px ahead and 7.5 behind with
    # the hips 9.2 px up), as far as a 12 px leg on the ground reaches, so it takes about 4.6 steps a second
    # (move_speed in biped.json); FootPlanter keeps each foot where it lands.
    "walk": half_cycle([
        # Landing: both feet down (front heel, rear toes), so the rear foot carries the body until the front is down.
        (0, {"Root": {"loc": (0, -2.8, 0)}, "Torso": (2, 0, 0), "Chest": (0, -5, 0), "Head": (-2, 5, 0),
             "Thigh_R": (-45, 0, 0), "Leg_R": (9, 0, 0), "Thigh_L": (36, 0, 0), "Leg_L": (6, 0, 0),
             "Arm_L": (-24, 0, 4), "Arm_R": (22, 0, -4), "Hand_L": (-20, 0, 0), "Hand_R": (-8, 0, 0)}),
        # Passing: the standing knee stays a little bent (hips 11.4 px up), the other foot swings through high.
        (4, {"Root": {"loc": (0, -0.6, 0)}, "Torso": (2, 0, 0), "Chest": (0, 0, 0), "Head": (-2, 0, 0),
             "Thigh_R": (-18, 0, 0), "Leg_R": (36, 0, 0), "Thigh_L": (-24, 0, 0), "Leg_L": (85, 0, 0),
             "Arm_L": (0, 0, 4), "Arm_R": (0, 0, -4), "Hand_L": (-12, 0, 0), "Hand_R": (-12, 0, 0)}),
        # Reaching: the swinging foot comes forward still 1.2 px up, so it does not skim the ground before it lands.
        (6, {"Root": {"loc": (0, -1.4, 0)}, "Torso": (2, 0, 0), "Chest": (0, 2.5, 0), "Head": (-2, -2.5, 0),
             "Thigh_R": (14, 0, 0), "Leg_R": (24, 0, 0), "Thigh_L": (-55, 0, 0), "Leg_L": (65, 0, 0),
             "Arm_L": (11, 0, 4), "Arm_R": (-12, 0, -4), "Hand_L": (-10, 0, 0), "Hand_R": (-16, 0, 0)}),
    ], 16),
    # Leaning in, a long stride with the rear heel kicked up, forearms bent and pumping.
    "run": cycle(
        {"Root": {"loc": (0, -1.5, 0)}, "Torso": (12, 0, 0), "Chest": (0, -8, 0), "Head": (-12, 8, 0),
         "Thigh_R": (-50, 0, 0), "Leg_R": (25, 0, 0), "Thigh_L": (35, 0, 0), "Leg_L": (60, 0, 0),
         "Arm_L": (-50, 0, 6), "Arm_R": (35, 0, -6), "Hand_L": (-85, 0, 0), "Hand_R": (-70, 0, 0)},
        {"Root": {"loc": (0, 0.6, 0)}, "Torso": (12, 0, 0), "Chest": (0, 0, 0), "Head": (-12, 0, 0),
         "Thigh_R": (-15, 0, 0), "Leg_R": (30, 0, 0), "Thigh_L": (-35, 0, 0), "Leg_L": (100, 0, 0),
         "Arm_L": (-10, 0, 6), "Arm_R": (5, 0, -6), "Hand_L": (-80, 0, 0), "Hand_R": (-80, 0, 0)},
        12),
    "sneak": [
        (0, crouch(2.5)),
        (20, crouch(2.7)),
        (40, crouch(2.5)),
    ],
    "sneak_walk": cycle(
        {**crouch(2.8), "Thigh_R": (-55, 0, 0), "Leg_R": (60, 0, 0), "Thigh_L": (-15, 0, 0), "Leg_L": (70, 0, 0),
         "Arm_L": (-40, 0, 6), "Arm_R": (-20, 0, -6)},
        {**crouch(2.6), "Thigh_R": (-38, 0, 0), "Leg_R": (75, 0, 0), "Thigh_L": (-50, 0, 0), "Leg_L": (100, 0, 0),
         "Arm_L": (-30, 0, 6), "Arm_R": (-30, 0, -6)},
        24),
    # Off the ground: one knee drives up, arms rise; holds the last frame until landing.
    "jump": [
        (0, {**STRAIGHT, "Torso": (5, 0, 0), "Thigh_R": (-45, 0, 0), "Leg_R": (70, 0, 0), "Thigh_L": (5, 0, 0),
             "Leg_L": (25, 0, 0), "Arm_L": (-35, 0, 10), "Arm_R": (-20, 0, -15), "Hand_L": (-30, 0, 0),
             "Hand_R": (-30, 0, 0)}),
        (6, {"Thigh_R": (-55, 0, 0), "Leg_R": (85, 0, 0), "Thigh_L": (10, 0, 0), "Leg_L": (35, 0, 0),
             "Arm_L": (-45, 0, 15), "Arm_R": (-30, 0, -20)}),
    ],
    # Arms out for balance, legs paddling slowly.
    "fall": [
        (0, {**STRAIGHT, "Torso": (-5, 0, 0), "Arm_L": (-20, 0, 45), "Arm_R": (-20, 0, -35), "Hand_L": (-20, 0, 0),
             "Hand_R": (-20, 0, 0), "Thigh_L": (-25, 0, 0), "Leg_L": (35, 0, 0), "Thigh_R": (5, 0, 0),
             "Leg_R": (20, 0, 0)}),
        (10, {"Arm_L": (-20, 0, 35), "Arm_R": (-20, 0, -45), "Thigh_L": (5, 0, 0), "Leg_L": (20, 0, 0),
              "Thigh_R": (-25, 0, 0), "Leg_R": (35, 0, 0)}),
        (20, {"Arm_L": (-20, 0, 45), "Arm_R": (-20, 0, -35), "Thigh_L": (-25, 0, 0), "Leg_L": (35, 0, 0),
              "Thigh_R": (5, 0, 0), "Leg_R": (20, 0, 0)}),
    ],
    # Hand over hand up a ladder, the opposite knee high.
    "climb": cycle(
        {**STRAIGHT, "Arm_R": (-165, 0, -5), "Hand_R": (-25, 0, 0), "Arm_L": (-125, 0, 5), "Hand_L": (-60, 0, 0),
         "Thigh_R": (-15, 0, 0), "Leg_R": (25, 0, 0), "Thigh_L": (-55, 0, 0), "Leg_L": (75, 0, 0)},
        {**STRAIGHT, "Arm_R": (-145, 0, -5), "Hand_R": (-45, 0, 0), "Arm_L": (-145, 0, 5), "Hand_L": (-45, 0, 0),
         "Thigh_R": (-35, 0, 0), "Leg_R": (50, 0, 0), "Thigh_L": (-35, 0, 0), "Leg_L": (50, 0, 0)},
        16),
    # Seated: thighs forward and apart, hands forward on the reins.
    "ride": [
        (0, {**STRAIGHT, "Thigh_L": (-80, 0, 15), "Thigh_R": (-80, 0, -15), "Leg_L": (10, 0, 0), "Leg_R": (10, 0, 0),
             "Arm_L": (-35, 0, 4), "Arm_R": (-35, 0, -4), "Hand_L": (-20, 0, 0), "Hand_R": (-20, 0, 0)}),
        (20, {"Torso": (3, 0, 0)}),
        (40, {"Torso": (0, 0, 0)}),
    ],
    # Drawn standing (the renderer lays the body down like vanilla): arms reach ahead, sweep out and back to the
    # sides, legs flutter.
    "swim": [
        (0, {**STRAIGHT, "Arm_L": (-170, 0, 10), "Arm_R": (-170, 0, -10), "Hand_L": (-5, 0, 0), "Hand_R": (-5, 0, 0),
             "Thigh_L": (-15, 0, 0), "Leg_L": (10, 0, 0), "Thigh_R": (15, 0, 0), "Leg_R": (5, 0, 0)}),
        (8, {"Arm_L": (-120, 0, 70), "Arm_R": (-120, 0, -70), "Hand_L": (-30, 0, 0), "Hand_R": (-30, 0, 0),
             "Thigh_L": (15, 0, 0), "Leg_L": (5, 0, 0), "Thigh_R": (-15, 0, 0), "Leg_R": (10, 0, 0)}),
        (14, {"Arm_L": (-10, 0, 15), "Arm_R": (-10, 0, -15), "Hand_L": (-60, 0, 0), "Hand_R": (-60, 0, 0),
              "Thigh_L": (-15, 0, 0), "Leg_L": (10, 0, 0), "Thigh_R": (15, 0, 0), "Leg_R": (5, 0, 0)}),
        (20, {"Arm_L": (-170, 0, 10), "Arm_R": (-170, 0, -10), "Hand_L": (-5, 0, 0), "Hand_R": (-5, 0, 0),
              "Thigh_L": (-15, 0, 0), "Leg_L": (10, 0, 0), "Thigh_R": (15, 0, 0), "Leg_R": (5, 0, 0)}),
    ],
    # Drawn standing (the renderer lays the body in the bed like vanilla): arms along the body, still.
    "sleep": [
        (0, {**STRAIGHT, "Arm_L": (0, 0, 3), "Arm_R": (0, 0, -3), "Hand_L": (-4, 0, 0), "Hand_R": (-4, 0, 0)}),
        (40, {"Head": (0, 0, 0)}),
    ],
    # Elytra (drawn standing, the renderer lays it along the flight): arms swept back and out like wings, legs
    # together, a slow sway.
    "glide": [
        (0, {**STRAIGHT, "Arm_L": (20, 0, 25), "Arm_R": (20, 0, -25), "Hand_L": (-5, 0, 0), "Hand_R": (-5, 0, 0),
             "Thigh_L": (4, 0, 2), "Thigh_R": (4, 0, -2), "Leg_L": (6, 0, 0), "Leg_R": (6, 0, 0)}),
        (20, {"Arm_L": (20, 0, 30), "Arm_R": (20, 0, -20), "Chest": (0, 4, 0)}),
        (40, {"Arm_L": (20, 0, 25), "Arm_R": (20, 0, -25), "Chest": (0, 0, 0)}),
    ],
    # Riptide (the renderer spins the body like vanilla): arms straight ahead together, like a dive.
    "spin": [
        (0, {**STRAIGHT, "Arm_L": (-175, 0, -4), "Arm_R": (-175, 0, 4), "Hand_L": (0, 0, 0), "Hand_R": (0, 0, 0),
             "Thigh_L": (0, 0, -1), "Thigh_R": (0, 0, 1)}),
        (20, {"Head": (0, 0, 0)}),
    ],
    # Additive: the chest rises a little and the shoulders lift on each breath.
    "breathe": [
        (0, {"Chest": (0, 0, 0), "Shoulder_L": {"loc": (0, 0, 0)}, "Shoulder_R": {"loc": (0, 0, 0)}}),
        (40, {"Chest": (-1.5, 0, 0), "Shoulder_L": {"loc": (0, 0.25, 0)}, "Shoulder_R": {"loc": (0, 0.25, 0)}}),
        (80, {"Chest": (0, 0, 0), "Shoulder_L": {"loc": (0, 0, 0)}, "Shoulder_R": {"loc": (0, 0, 0)}}),
    ],
}

GUARD_LEGS = {**GUARD, **bent_legs(1.0), "Torso": (4, 0, 0), "Chest": (5, 0, 0), "Head": (-9, 0, 0)}

SETS["fists"] = {
    # The combat idle: the stance, bouncing twice a second on the legs.
    "stance": [
        (0, stance(False)),
        (5, stance(True)),
        (10, stance(False)),
        (15, stance(True)),
        (20, stance(False)),
    ],
    # The combat walk: a shuffle, the lead foot steps, then the rear foot follows; the hands stay up.
    "stance_walk": [
        (0, stance(False)),
        (4, {**stance(False), "Thigh_L": (-38, 0, 6), "Leg_L": (55, 0, 0)}),
        (8, stance(True)),
        (12, {**stance(False), "Thigh_R": (2, 0, -6), "Leg_R": (60, 0, 0)}),
        (16, stance(False)),
    ],
    # The guard as a layer: the whole body while standing, the arms only while moving (see fists.json).
    "guard": [
        (0, GUARD_LEGS),
        (10, {"Hand_L": (-95, 0, 0), "Hand_R": (-105, 0, 0)}),
        (20, {"Hand_L": (-100, 0, 0), "Hand_R": (-110, 0, 0)}),
    ],
    # A lead jab from the guard, legs first: the rear foot pushes and the weight goes forward, the hips and chest
    # turn the lead shoulder in, and only then the arm shoots out; it comes back the same way.
    "jab": [
        (0, {**GUARD_LEGS, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, 0)}}),
        (2, {"Root": {"rot": (0, -8, 0), "loc": (0, -1.2, 0.8)}, "Thigh_R": (25, 0, 0), "Leg_R": (35, 0, 0),
             "Thigh_L": (-25, 0, 0), "Leg_L": (30, 0, 0), "Chest": (6, -15, 0), "Head": (-10, 15, 0),
             "Arm_L": (-80, -10, 0), "Hand_L": (-70, 0, 0)}),
        (3, {"Root": {"rot": (0, -12, 0), "loc": (0, -1.4, 1.5)}, "Thigh_R": (30, 0, 0), "Leg_R": (30, 0, 0),
             "Thigh_L": (-32, 0, 0), "Leg_L": (38, 0, 0), "Chest": (8, -22, 0), "Head": (-12, 25, 0),
             "Arm_L": (-90, 0, 0), "Hand_L": (-5, 0, 0)}),
        (5, {"Root": {"rot": (0, -6, 0), "loc": (0, -1.1, 0.6)}, "Thigh_R": (15, 0, 0), "Leg_R": (25, 0, 0),
             "Thigh_L": (-22, 0, 0), "Leg_L": (30, 0, 0), "Chest": (6, -8, 0), "Head": (-10, 8, 0),
             "Arm_L": (-75, -15, 0), "Hand_L": (-80, 0, 0)}),
        (8, {**GUARD_LEGS, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, 0)}}),
    ],
    # A jab stepping in (root motion, see fists.json): the rear foot pushes the body forward, the lead foot reaches
    # ahead and lands as the fist does, then the rear foot drags back under; the root ends 12 px (0.7 block) ahead,
    # which moves the body itself.
    "step_in": [
        (0, {**GUARD_LEGS, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, 0)}}),
        (2, {"Root": {"rot": (0, -4, 0), "loc": (0, -1.2, 4)}, "Thigh_R": (22, 0, 0), "Leg_R": (32, 0, 0),
             "Thigh_L": (-45, 0, 0), "Leg_L": (50, 0, 0), "Chest": (4, -8, 0), "Head": (-10, 8, 0),
             "Arm_L": (-70, -10, 0), "Hand_L": (-80, 0, 0)}),
        (4, {"Root": {"rot": (0, -10, 0), "loc": (0, -1.5, 9)}, "Thigh_R": (28, 0, 0), "Leg_R": (30, 0, 0),
             "Thigh_L": (-30, 0, 0), "Leg_L": (25, 0, 0), "Chest": (8, -20, 0), "Head": (-12, 20, 0),
             "Arm_L": (-90, 0, 0), "Hand_L": (-5, 0, 0)}),
        (6, {"Root": {"rot": (0, -5, 0), "loc": (0, -1.2, 12)}, "Thigh_R": (10, 0, 0), "Leg_R": (45, 0, 0),
             "Thigh_L": (-20, 0, 0), "Leg_L": (30, 0, 0), "Chest": (6, -8, 0), "Head": (-10, 8, 0),
             "Arm_L": (-75, -15, 0), "Hand_L": (-80, 0, 0)}),
        (10, {**GUARD_LEGS, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, 12)}}),
    ],
}



# Every fighting clip starts from the square guard with every joint it moves keyed (a joint first keyed later would
# hold that later value from the start).
READY = {**GUARD_LEGS, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, 0)}}


def legs(lead_thigh, lead_shin, rear_thigh, rear_shin):
    """The lead (left) and rear (right) legs: thigh forward with a negative X, shin bent with a positive X."""
    return {"Thigh_L": (lead_thigh, 0, 0), "Leg_L": (lead_shin, 0, 0), "Thigh_R": (rear_thigh, 0, 0),
            "Leg_R": (rear_shin, 0, 0)}


def hips(turn, drop, ahead=0.0, lean=0.0):
    """The pelvis turned `turn` degrees toward its left (+) or right (-), sunk `drop` px, moved `ahead` px."""
    return {"Root": {"rot": (lean, turn, 0), "loc": (0, -drop, ahead)}}


SETS["fists"].update({
    # The rear straight, legs first: the rear heel turns out and the rear hip drives the right shoulder through, the
    # chest follows, the right fist shoots out last; the left hand stays home at the chin.
    "cross": [
        (0, READY),
        (2, {**hips(6, 1.2, 0.5), **legs(-24, 34, 22, 36), "Chest": (5, 6, 0), "Head": (-9, -6, 0)}),
        (3, {**hips(14, 1.4, 1.2), **legs(-28, 36, 30, 30), "Chest": (7, 20, 0), "Head": (-11, -18, 0),
             "Arm_R": (-75, 12, 0), "Hand_R": (-60, 0, 0)}),
        (5, {**hips(16, 1.4, 1.6), "Chest": (8, 26, 0), "Head": (-12, -24, 0),
             "Arm_R": (-92, 5, 0), "Hand_R": (-4, 0, 0)}),
        (7, {**hips(8, 1.2, 0.6), **legs(-24, 34, 22, 36), "Chest": (6, 10, 0), "Head": (-10, -8, 0),
             "Arm_R": (-70, 18, 0), "Hand_R": (-80, 0, 0)}),
        (9, READY),
    ],
    # The lead hook: the weight sits on the lead leg, the lead heel pivots and the hips snap right; the left arm,
    # bent square at the elbow and level with the shoulder, comes round with the body.
    "hook": [
        (0, READY),
        (2, {**hips(8, 1.4, 0.2), **legs(-26, 40, 18, 30), "Chest": (5, 12, 0), "Head": (-9, -10, 0),
             "Arm_L": (-40, -10, 60), "Hand_L": (-100, 0, 0)}),
        (4, {**hips(-18, 1.6, 0.6), **legs(-30, 44, 20, 32), "Chest": (6, -26, 0), "Head": (-10, 22, 0),
             "Arm_L": (0, -20, 88), "Hand_L": (-92, 0, 0)}),
        (6, {**hips(-26, 1.6, 0.6), "Chest": (6, -34, 0), "Head": (-10, 28, 0),
             "Arm_L": (0, -45, 86), "Hand_L": (-88, 0, 0)}),
        (8, {**hips(-10, 1.2, 0.2), **legs(-24, 36, 18, 30), "Chest": (5, -10, 0), "Head": (-9, 8, 0),
             "Arm_L": (-60, -15, 25), "Hand_L": (-100, 0, 0)}),
        (10, READY),
    ],
    # The rear uppercut: the knees dip, the right hand drops a little, then the legs drive the body up and the hips
    # turn left as the fist rises in front of the chin.
    "uppercut": [
        (0, READY),
        (3, {**hips(-6, 3.2, 0), **legs(-38, 70, 30, 64), "Chest": (10, -10, 0), "Head": (-12, 8, 0),
             "Arm_R": (-30, 10, 0), "Hand_R": (-110, 0, 0)}),
        (5, {**hips(14, 1.0, 1.0, -4), **legs(-18, 22, 14, 18), "Chest": (-2, 20, 0), "Head": (-6, -16, 0),
             "Arm_R": (-50, 15, -5), "Hand_R": (-115, 0, 0)}),
        (6, {**hips(18, 0.6, 1.2, -6), "Chest": (-6, 24, 0), "Head": (-2, -20, 0),
             "Arm_R": (-70, 12, -5), "Hand_R": (-120, 0, 0)}),
        (9, {**hips(8, 1.2, 0.4), **legs(-24, 36, 18, 32), "Chest": (4, 8, 0), "Head": (-9, -6, 0),
             "Arm_R": (-55, 18, 0), "Hand_R": (-105, 0, 0)}),
        (12, READY),
    ],
    # In the air: both hands clasped high, the knees tucked, then the fists hammer down as the legs kick out.
    "hammer": [
        (0, READY),
        (3, {**hips(0, 1.0, 0, -10), **legs(-70, 100, -50, 100), "Chest": (-10, 0, 0), "Head": (-4, 0, 0),
             "Arm_L": (-170, -10, -8), "Hand_L": (-30, 0, 0), "Arm_R": (-170, 10, 8), "Hand_R": (-30, 0, 0)}),
        (5, {**hips(0, 1.0, 0, 12), **legs(-45, 50, -30, 60), "Chest": (18, 0, 0), "Head": (-14, 0, 0),
             "Arm_L": (-80, -12, -10), "Hand_L": (-10, 0, 0), "Arm_R": (-80, 12, 10), "Hand_R": (-10, 0, 0)}),
        (7, {**hips(0, 1.0, 0, 16), "Chest": (22, 0, 0), "Head": (-16, 0, 0),
             "Arm_L": (-35, -10, -6), "Hand_L": (-15, 0, 0), "Arm_R": (-35, 10, 6), "Hand_R": (-15, 0, 0)}),
        (10, READY),
    ],
})

SETS["combat"] = {
    # Dodges: a quick hop out of the way (root motion), low, the guard kept; a roll forward.
    "dodge_back": [
        (0, READY),
        (2, {**hips(0, 3.0, -6, -6), **legs(-40, 70, -10, 60), "Chest": (-4, 0, 0)}),
        (5, {**hips(0, 2.0, -15, -4), **legs(-30, 50, 0, 40), "Chest": (0, 0, 0)}),
        (8, {**READY, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, -16)}}),
    ],
    "dodge_left": [
        (0, READY),
        (2, {"Root": {"rot": (0, 0, 8), "loc": (5, -3.0, 0)}, "Thigh_L": (-20, 0, 25), "Leg_L": (40, 0, 0),
             "Thigh_R": (-30, 0, 5), "Leg_R": (60, 0, 0), "Chest": (4, 0, -6)}),
        (5, {"Root": {"rot": (0, 0, 4), "loc": (14, -2.0, 0)}, "Thigh_L": (-25, 0, 5), "Leg_L": (45, 0, 0),
             "Thigh_R": (-25, 0, 10), "Leg_R": (45, 0, 0), "Chest": (5, 0, -2)}),
        (8, {**READY, "Root": {"rot": (0, 0, 0), "loc": (16, -1.0, 0)}}),
    ],
    "dodge_right": [
        (0, READY),
        (2, {"Root": {"rot": (0, 0, -8), "loc": (-5, -3.0, 0)}, "Thigh_R": (-20, 0, -25), "Leg_R": (40, 0, 0),
             "Thigh_L": (-30, 0, -5), "Leg_L": (60, 0, 0), "Chest": (4, 0, 6)}),
        (5, {"Root": {"rot": (0, 0, -4), "loc": (-14, -2.0, 0)}, "Thigh_R": (-25, 0, -5), "Leg_R": (45, 0, 0),
             "Thigh_L": (-25, 0, -10), "Leg_L": (45, 0, 0), "Chest": (5, 0, 2)}),
        (8, {**READY, "Root": {"rot": (0, 0, 0), "loc": (-16, -1.0, 0)}}),
    ],
    # A forward roll: tucked, a whole turn about the hips' X, back on the feet a block and a half on.
    "dodge_forward": [
        (0, READY),
        (2, {"Root": {"rot": (60, 0, 0), "loc": (0, -5.0, 4)}, **legs(-80, 120, -80, 120), "Chest": (30, 0, 0),
             "Head": (30, 0, 0), "Arm_L": (-120, 0, 10), "Arm_R": (-120, 0, -10)}),
        (4, {"Root": {"rot": (150, 0, 0), "loc": (0, -6.0, 11)}}),
        (6, {"Root": {"rot": (240, 0, 0), "loc": (0, -6.0, 17)}}),
        (8, {"Root": {"rot": (330, 0, 0), "loc": (0, -4.0, 22)}, **legs(-60, 100, -60, 100)}),
        (12, {**READY, "Root": {"rot": (360, 0, 0), "loc": (0, -1.0, 24)}}),
    ],
    # Reactions: a short flinch (head and chest snap back), a long stagger back a step, a knockdown on the back and
    # up again, a guard shaken by a blow, a guard broken open, an attack turned aside by a parry.
    "hit_short": [
        (0, READY),
        (2, {**hips(0, 1.4, -1, -4), "Chest": (-10, 6, 0), "Head": (-22, -6, 0),
             "Arm_L": (-50, -25, 10), "Arm_R": (-35, 25, -10)}),
        (6, READY),
    ],
    "hit_long": [
        (0, READY),
        (2, {**hips(6, 1.8, -2, -8), "Torso": (-6, 0, 0), "Chest": (-14, 10, 0), "Head": (-18, -8, 0),
             "Arm_L": (-30, -20, 30), "Hand_L": (-50, 0, 0), "Arm_R": (-20, 20, -35), "Hand_R": (-50, 0, 0)}),
        (6, {**hips(4, 2.4, -5, -4), **legs(-10, 30, 25, 50)}),
        (10, {**hips(0, 1.6, -5, 0), **legs(-26, 40, -20, 40), "Torso": (4, 0, 0), "Chest": (2, 4, 0)}),
        (14, {**READY, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, -5)}}),
    ],
    "knockdown": [
        (0, READY),
        (3, {"Root": {"rot": (-40, 0, 0), "loc": (0, -4, -4)}, **legs(-50, 30, -10, 20), "Torso": (-6, 0, 0),
             "Chest": (-10, 0, 0), "Head": (10, 0, 0), "Arm_L": (-60, 0, 50), "Arm_R": (-60, 0, -50),
             "Hand_L": (-30, 0, 0), "Hand_R": (-30, 0, 0)}),
        (7, {"Root": {"rot": (-88, 0, 0), "loc": (0, -9.5, -10)}, **legs(-10, 10, -6, 6), "Head": (20, 0, 0),
             "Arm_L": (-10, 0, 40), "Arm_R": (-10, 0, -40), "Hand_L": (-10, 0, 0), "Hand_R": (-10, 0, 0)}),
        (22, {"Root": {"rot": (-88, 0, 0), "loc": (0, -9.5, -10)}}),
        (28, {"Root": {"rot": (-30, 0, 0), "loc": (0, -7, -10)}, **legs(-80, 130, -70, 120), "Chest": (30, 0, 0),
              "Head": (10, 0, 0), "Arm_L": (20, 0, 20), "Arm_R": (20, 0, -20)}),
        (33, {"Root": {"rot": (0, 0, 0), "loc": (0, -4, -10)}, **legs(-40, 80, -30, 70), "Chest": (15, 0, 0)}),
        (36, {**READY, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, -10)}}),
    ],
    "guard_hit": [
        (0, READY),
        (2, {**hips(0, 1.6, -1.5, -3), "Chest": (-4, 0, 0), "Head": (-12, 0, 0),
             "Arm_L": (-82, -25, 0), "Hand_L": (-115, 0, 0), "Arm_R": (-62, 25, 0), "Hand_R": (-120, 0, 0)}),
        (5, READY),
    ],
    "guard_break": [
        (0, READY),
        (2, {**hips(0, 1.0, -2, -10), "Chest": (-16, 0, 0), "Head": (-20, 0, 0),
             "Arm_L": (-120, 0, 70), "Hand_L": (-20, 0, 0), "Arm_R": (-120, 0, -70), "Hand_R": (-20, 0, 0)}),
        (8, {**hips(0, 2.2, -4, -6), **legs(-10, 40, 20, 50), "Chest": (-10, 0, 0),
             "Arm_L": (-60, 0, 60), "Arm_R": (-60, 0, -60)}),
        (14, {**hips(0, 1.6, -4, 0), **legs(-26, 40, -20, 40), "Chest": (6, 0, 0), "Head": (-8, 0, 0),
              "Arm_L": (-20, 0, 20), "Hand_L": (-40, 0, 0), "Arm_R": (-20, 0, -20), "Hand_R": (-40, 0, 0)}),
        (18, {**READY, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, -4)}}),
    ],
    "parried": [
        (0, READY),
        (2, {**hips(-10, 1.0, -1, -8), "Chest": (-12, -14, 0), "Head": (-14, 10, 0),
             "Arm_L": (-150, -20, 30), "Hand_L": (-20, 0, 0), "Arm_R": (-140, 20, -30), "Hand_R": (-20, 0, 0)}),
        (7, {**hips(-4, 1.8, -3, -4), **legs(-14, 40, 22, 46), "Chest": (-6, -6, 0),
             "Arm_L": (-80, -20, 30), "Hand_L": (-60, 0, 0), "Arm_R": (-70, 20, -30), "Hand_R": (-60, 0, 0)}),
        (12, {**READY, "Root": {"rot": (0, 0, 0), "loc": (0, -1.0, -3)}}),
    ],
}


# The Disciple déchu (the zombie's look; docs/mobs/disciple-dechu.png): a dead body that still half remembers its
# training. Stooped and slack, it drags its left foot, lurches forward reaching when it chases, and keeps one fist up
# out of habit when it faces a fight. Its blows are wide and slow to read: a haymaker, a jab then a haymaker, a lunge.
SLUMP = {**bent_legs(0.8), "Torso": (14, 0, 0), "Chest": (8, 0, 0), "Head": (-6, 0, 10),
         "Arm_L": (-12, 0, 6), "Arm_R": (-8, 0, -4), "Hand_L": (-14, 0, 0), "Hand_R": (-20, 0, 0)}
# The guard it half remembers: left foot ahead, the left fist up, the right arm hanging.
HALF_GUARD = {"Root": {"rot": (0, -14, 0), "loc": (0, -1.4, 0)}, **legs(-20, 36, 10, 28),
              "Torso": (10, 0, 0), "Chest": (6, -6, 0), "Head": (-12, 14, 6),
              "Arm_L": (-70, -10, 20), "Hand_L": (-95, 0, 0), "Arm_R": (-14, 0, -6), "Hand_R": (-24, 0, 0)}
SHAMBLE_LAND = {"Root": {"loc": (0, -2.2, 0)}, "Torso": (16, 0, -3), "Chest": (6, -6, 0), "Head": (-8, 6, 8),
                "Thigh_R": (-32, 0, 0), "Leg_R": (8, 0, 0), "Thigh_L": (26, 0, 0), "Leg_L": (10, 0, 0),
                "Arm_L": (-18, 0, 6), "Arm_R": (8, 0, -4), "Hand_L": (-18, 0, 0), "Hand_R": (-10, 0, 0)}
FLINCH_REST = {"Torso": (14, 0, 0), "Chest": (8, 0, 0), "Head": (-6, 0, 10), "Arm_L": (-12, 0, 6),
               "Arm_R": (-8, 0, -4)}

SETS["disciple"] = {
    # Slumped, the head sinking to one side until it jerks up again.
    "idle": [
        (0, SLUMP),
        (24, {"Torso": (16, 0, 2), "Chest": (10, 4, 0), "Head": (-2, 6, 24), "Arm_L": (-14, 0, 8),
              "Arm_R": (-6, 0, -6)}),
        (30, {"Head": (0, 4, 30)}),
        (33, {"Head": (-14, -6, -6), "Chest": (6, -2, 0)}),
        (40, {"Head": (-8, -2, 4)}),
        (60, SLUMP),
    ],
    # A shamble: a full step with the right foot, the left one dragged after it, the body rolling over them.
    "walk": [
        (0, SHAMBLE_LAND),
        (7, {"Root": {"loc": (0, -1.0, 0)}, "Torso": (15, 0, 2), "Chest": (2, 0, 0), "Head": (-6, 0, 14),
             "Thigh_R": (-6, 0, 0), "Leg_R": (6, 0, 0), "Thigh_L": (-4, 0, 0), "Leg_L": (18, 0, 0),
             "Arm_L": (-6, 0, 6), "Arm_R": (-4, 0, -4), "Hand_L": (-14, 0, 0), "Hand_R": (-16, 0, 0)}),
        (14, {"Root": {"loc": (0, -1.8, 0)}, "Torso": (14, 0, 4), "Chest": (4, 6, 0), "Head": (-8, -4, 6),
              "Thigh_L": (-22, 0, 0), "Leg_L": (6, 0, 0), "Thigh_R": (24, 0, 0), "Leg_R": (8, 0, 0),
              "Arm_L": (6, 0, 6), "Arm_R": (-16, 0, -4), "Hand_L": (-10, 0, 0), "Hand_R": (-22, 0, 0)}),
        (21, {"Root": {"loc": (0, -0.6, 0)}, "Torso": (16, 0, 0), "Chest": (2, 0, 0), "Head": (-6, 0, 10),
              "Thigh_R": (-30, 0, 0), "Leg_R": (60, 0, 0), "Thigh_L": (4, 0, 0), "Leg_L": (8, 0, 0),
              "Arm_L": (-8, 0, 6), "Arm_R": (0, 0, -4), "Hand_L": (-14, 0, 0), "Hand_R": (-14, 0, 0)}),
        (28, SHAMBLE_LAND),
    ],
    # The chase: thrown forward, long ragged strides, both arms reaching for the prey.
    "run": cycle(
        {"Root": {"loc": (0, -1.8, 0)}, "Torso": (26, 0, 0), "Chest": (4, -8, 0), "Head": (-24, 8, 0),
         "Thigh_R": (-48, 0, 0), "Leg_R": (22, 0, 0), "Thigh_L": (34, 0, 0), "Leg_L": (55, 0, 0),
         "Arm_L": (-75, 0, 10), "Arm_R": (-55, 0, -10), "Hand_L": (-20, 0, 0), "Hand_R": (-30, 0, 0)},
        {"Root": {"loc": (0, 0.4, 0)}, "Torso": (26, 0, 0), "Chest": (0, 0, 0), "Head": (-24, 0, 0),
         "Thigh_R": (-12, 0, 0), "Leg_R": (28, 0, 0), "Thigh_L": (-30, 0, 0), "Leg_L": (95, 0, 0),
         "Arm_L": (-60, 0, 14), "Arm_R": (-70, 0, -14), "Hand_L": (-30, 0, 0), "Hand_R": (-20, 0, 0)},
        16),
    # Facing a fight: the half-remembered guard, swaying.
    "stance": [
        (0, HALF_GUARD),
        (20, {"Root": {"rot": (0, -10, 0), "loc": (0, -1.8, 0)}, "Head": (-8, 10, 12),
              "Arm_L": (-62, -10, 22), "Hand_L": (-88, 0, 0), "Arm_R": (-8, 0, -8)}),
        (40, HALF_GUARD),
    ],
    # The clumsy blow: the right arm flung back, then a wide haymaker that carries the body past its feet.
    "strike": [
        (0, HALF_GUARD),
        (3, {"Root": {"rot": (0, -30, 0), "loc": (0, -1.6, -0.5)}, "Chest": (4, -25, 0), "Head": (-10, 30, 0),
             "Arm_R": (20, 0, -70), "Hand_R": (-60, 0, 0), "Arm_L": (-40, 0, 20), "Hand_L": (-60, 0, 0)}),
        (5, {"Root": {"rot": (0, 15, 0), "loc": (0, -2.2, 2)}, **legs(-36, 40, 10, 25), "Chest": (14, 30, 0),
             "Head": (-16, -20, 0), "Arm_R": (-95, -45, 0), "Hand_R": (-15, 0, 0), "Arm_L": (-20, 0, 25),
             "Hand_L": (-40, 0, 0)}),
        (8, {"Root": {"rot": (0, 24, 0), "loc": (0, -2.6, 2.6)}, "Torso": (24, 0, 0), "Chest": (18, 36, 0),
             "Head": (-20, -24, 10), "Arm_R": (-60, -60, 10), "Hand_R": (-30, 0, 0)}),
        (18, HALF_GUARD),
    ],
    # The memory of a technique: a quick left jab, then the right haymaker.
    "double": [
        (0, HALF_GUARD),
        (3, {"Root": {"rot": (0, -22, 0), "loc": (0, -1.6, 1)}, "Chest": (8, -18, 0), "Head": (-12, 22, 0),
             "Arm_L": (-95, -10, 0), "Hand_L": (-10, 0, 0)}),
        (6, {"Root": {"rot": (0, -8, 0), "loc": (0, -1.4, 0.5)}, "Chest": (6, 0, 0), "Head": (-10, 8, 0),
             "Arm_L": (-60, -10, 15), "Hand_L": (-80, 0, 0)}),
        (8, {"Root": {"rot": (0, -20, 0), "loc": (0, -1.6, 0)}, "Chest": (4, -20, 0),
             "Arm_R": (-30, 20, -40), "Hand_R": (-80, 0, 0)}),
        (10, {"Root": {"rot": (0, 16, 0), "loc": (0, -2.0, 2)}, **legs(-34, 40, 10, 25), "Chest": (12, 28, 0),
              "Head": (-14, -18, 0), "Arm_R": (-95, -40, 0), "Hand_R": (-10, 0, 0)}),
        (14, {"Root": {"rot": (0, 10, 0), "loc": (0, -1.8, 1.5)}, "Arm_R": (-70, -30, 0), "Hand_R": (-40, 0, 0)}),
        (22, HALF_GUARD),
    ],
    # The lunge: it sinks, throws itself forward with both arms out, lands short and stumbles (in place: the mob's
    # own motion carries it).
    "lunge": [
        (0, HALF_GUARD),
        (5, {"Root": {"rot": (0, 0, 0), "loc": (0, -3.5, -1)}, **legs(-40, 80, -30, 70), "Torso": (30, 0, 0),
             "Chest": (10, 0, 0), "Head": (-30, 0, 0), "Arm_L": (40, 0, 20), "Arm_R": (40, 0, -20),
             "Hand_L": (-30, 0, 0), "Hand_R": (-30, 0, 0)}),
        (9, {"Root": {"rot": (0, 0, 0), "loc": (0, 1.5, 3)}, **legs(-40, 20, 30, 40), "Torso": (34, 0, 0),
             "Chest": (8, 0, 0), "Head": (-34, 0, 0), "Arm_L": (-110, 0, 10), "Arm_R": (-110, 0, -10),
             "Hand_L": (-10, 0, 0), "Hand_R": (-10, 0, 0)}),
        (13, {"Root": {"loc": (0, -3, 3)}, **legs(-50, 70, 10, 50), "Torso": (30, 0, 0),
              "Arm_L": (-80, 0, 20), "Arm_R": (-90, 0, -20), "Hand_L": (-30, 0, 0), "Hand_R": (-30, 0, 0)}),
        (19, {"Root": {"rot": (0, 8, 0), "loc": (0, -2, 1.5)}, **legs(-24, 36, 12, 26), "Torso": (20, 0, 0),
              "Head": (-16, -8, 10), "Arm_L": (-40, 0, 20), "Arm_R": (-30, 0, -14)}),
        (26, HALF_GUARD),
    ],
    # Struck: the head snaps back and the arms fly out (upper body only, see disciple.json).
    "flinch": [
        (0, FLINCH_REST),
        (2, {"Torso": (-4, 0, 0), "Chest": (-12, 8, 0), "Head": (-30, -10, -8), "Arm_L": (-30, 0, 30),
             "Arm_R": (-24, 0, -30)}),
        (8, FLINCH_REST),
    ],
    # Death, within vanilla's 20 ticks: the knees give, it kneels, slumps and falls on its face.
    "death": [
        (0, SLUMP),
        (4, {"Root": {"rot": (0, 0, 0), "loc": (0, -4, 0)}, **legs(-50, 95, -45, 90), "Torso": (20, 0, 0),
             "Head": (-20, 0, 0), "Arm_L": (-10, 0, 20), "Arm_R": (-10, 0, -20)}),
        (8, {"Root": {"rot": (0, 0, 0), "loc": (0, -6, 0)}, **legs(0, 90, 0, 90), "Torso": (10, 0, 0),
             "Chest": (10, 0, 0), "Head": (20, 0, 15), "Arm_L": (0, 0, 6), "Arm_R": (0, 0, -6),
             "Hand_L": (-6, 0, 0), "Hand_R": (-6, 0, 0)}),
        (14, {"Root": {"rot": (88, 0, 0), "loc": (0, -10.5, 4)}, **legs(0, 0, 0, 0), "Torso": (0, 0, 0),
              "Chest": (0, 0, 0), "Head": (-10, 20, 0), "Arm_L": (-20, 0, 30), "Arm_R": (-10, 0, -25)}),
        (16, {"Root": {"rot": (84, 0, 0), "loc": (0, -10, 4)}}),
        (20, {"Root": {"rot": (88, 0, 0), "loc": (0, -10.5, 4)}}),
    ],
}

# The Disciple déchu armed, from the validated storyboards (docs/mobs/boards/): sword, axe and tool. Poses are written
# as on the boards, in profile: angles in degrees on screen, limbs from straight down (0) toward the front (+), the
# torso from upright leaning forward (+), the weapon from straight down, forward at 90, up at 180; hx/hy move the hips
# (board pixels, 34 to the torso's 12 model pixels). board() turns them into joint turns; what a profile cannot show
# (the hips and chest turning, an arm swinging across) is added as twists. The item is in the right hand; the axe's
# and the tool's left hand is put on the haft afterwards (bake_second_hand).
BOARD_PIXEL = 12 / 34
# The weapon's own turn from the forearm when the wrist is straight (murim_items: a held item points 80° forward).
WRIST_REST = 80


def board(p):
    lean = p.get("lean", 0)
    # A knee only bends forward: the shin never points further forward than its thigh (4° short of straight).
    sF, sB = min(p["sF"], p["tF"] - 4), min(p["sB"], p["tB"] - 4)
    # A two-handed hold (an aL/fL pose without its own armZ) brings the right hand in front of the body.
    key = {
        "Root": {"rot": (0, p.get("hips", 0), 0),
                 "loc": (0, -p.get("hy", 0) * BOARD_PIXEL, p.get("hx", 0) * BOARD_PIXEL)},
        "Torso": (lean * 0.6, p.get("twist", 0) * 0.4, 0),
        "Chest": (lean * 0.4, p.get("twist", 0) * 0.6, 0),
        "Head": (p.get("head", 0), -p.get("twist", 0) * 0.5 - p.get("hips", 0) * 0.5, 0),
        # Leaning forward turns the chest, so a hanging arm points back by as much: the arm turns that much more.
        "Arm_R": (-(p["aR"] + lean), p.get("armY", 0), p.get("armZ", 0)),
        "Hand_R": (-(p["fR"] - p["aR"]), 0, 0),
        "Tool_R": (WRIST_REST - (p["w"] - p["fR"]), p.get("wristY", 0), 0),
        "Thigh_R": (-p["tF"], 0, p.get("legZ", 0)), "Leg_R": (p["tF"] - sF, 0, 0),
        "Thigh_L": (-p["tB"], 0, -p.get("legZ", 0)), "Leg_L": (p["tB"] - sB, 0, 0),
    }
    if "aL" in p:
        key["Arm_L"] = (-(p["aL"] + lean), 0, p.get("armLZ", 8))
        key["Hand_L"] = (-(p["fL"] - p["aL"]), 0, 0)
    return key


def poses(**named):
    return {name: board(p) for name, p in named.items()}


# Sword: the arm alone; the left hand makes the sword fingers behind.
SWORD_GUARD = dict(lean=8, head=-4, aR=38, fR=80, w=100, aL=-100, fL=175, tF=22, sF=4, tB=-24, sB=14, hy=2,
                   armZ=-6, hips=-14, twist=-6)
S = poses(
    guard=SWORD_GUARD,
    sway=dict(SWORD_GUARD, w=106, fR=84, head=-8, hy=3),
    armed=dict(lean=-8, head=-12, hx=-4, aR=205, fR=235, w=250, aL=75, fL=92, tF=18, sF=10, tB=-36, sB=22, hy=2,
               hips=-24, twist=-28, armZ=-16),
    cut=dict(lean=26, head=6, hx=10, hy=4, aR=100, fR=104, w=118, aL=-40, fL=-20, tF=46, sF=2, tB=-42, sB=8,
             hips=14, twist=26, armY=-34, armZ=-10),
    cut_end=dict(lean=36, head=14, hx=15, hy=6, aR=35, fR=20, w=12, aL=-60, fL=-40, tF=50, sF=4, tB=-44, sB=6,
                 hips=20, twist=36, armY=-60, armZ=10),
    turn=dict(lean=20, head=4, hx=12, hy=4, aR=60, fR=30, w=265, aL=-50, fL=-30, tF=30, sF=6, tB=-40, sB=10,
              hips=10, twist=-8, armY=36, armZ=-20),
    backhand=dict(lean=16, head=-2, hx=20, hy=4, aR=92, fR=92, w=96, aL=-60, fL=-50, tF=-20, sF=10, tB=40, sB=4,
                  hips=-14, twist=-30, armY=46, armZ=-26, wristY=-30),
    stagger=dict(lean=36, head=14, hx=15, hy=6, aR=35, fR=60, w=70, aL=-60, fL=-40, tF=50, sF=4, tB=-44, sB=6,
                 hips=-18, twist=-30, armY=40),
    coil=dict(lean=14, head=-6, hx=-6, hy=7, aR=15, fR=80, w=92, aL=60, fL=92, tF=42, sF=-14, tB=-30, sB=40,
              hips=-16, twist=-10, armZ=-8),
    lunge=dict(lean=22, head=-10, hx=30, hy=7, aR=92, fR=92, w=92, aL=-80, fL=-70, tF=72, sF=8, tB=-58, sB=-52,
               hips=-10, twist=-4),
)

# Axe: two hands, the right at the end of the haft.
AXE_GUARD = dict(lean=14, head=-6, hy=4, aR=12, fR=62, w=206, tF=24, sF=0, tB=-28, sB=16, hips=-10, armZ=22,
                 aL=30, fL=80)
AXE_RAISE = dict(lean=-10, head=-16, hy=0, aR=172, fR=198, w=232, tF=12, sF=0, tB=-20, sB=8, armZ=18, aL=160,
                 fL=200)
AXE_CHOP = dict(lean=28, head=4, hx=8, hy=9, aR=100, fR=110, w=100, tF=50, sF=-10, tB=-32, sB=42, armZ=22,
                aL=80, fL=100)
AXE_STUCK = dict(lean=40, head=16, hx=10, hy=12, aR=55, fR=40, w=20, tF=54, sF=-14, tB=-30, sB=48, armZ=22,
                 aL=40, fL=40)
A = poses(
    guard=AXE_GUARD,
    breath=dict(AXE_GUARD, lean=17, hy=6, w=202, head=-2),
    raise_=AXE_RAISE,
    hold=dict(AXE_RAISE, lean=-15, aR=180, fR=206, w=244, head=-20),
    chop=AXE_CHOP,
    stuck=AXE_STUCK,
    heave=dict(AXE_STUCK, lean=30, hy=9, aR=40, fR=60, w=120),
    sweep_back=dict(lean=4, head=-8, hx=-4, hy=4, aR=-24, fR=30, w=268, tF=18, sF=4, tB=-34, sB=20, hips=-24,
                    twist=-40, aL=10, fL=40),
    sweep=dict(lean=20, head=0, hx=8, hy=6, aR=80, fR=86, w=92, tF=40, sF=0, tB=-36, sB=18, hips=20, twist=36,
               armY=-30, aL=80, fL=90),
    carry=dict(lean=2, head=-10, hx=10, hy=3, aR=150, fR=188, w=212, tF=30, sF=4, tB=-30, sB=14, hips=24,
               twist=30, armY=-20, aL=150, fL=190),
    run=dict(lean=26, head=-14, aR=160, fR=192, w=218, tF=46, sF=30, tB=-42, sB=52, aL=150, fL=190),
    leap=dict(lean=12, head=-16, hx=10, hy=-10, aR=182, fR=212, w=242, tF=62, sF=70, tB=-24, sB=60, aL=170,
              fL=210),
)

# Tool: held like a staff, the head toward the prey.
TOOL_GUARD = dict(lean=12, head=-4, hy=3, aR=18, fR=74, w=82, tF=22, sF=2, tB=-26, sB=16, hips=-12, armZ=22,
                  aL=40, fL=80)
TOOL_POKE = dict(lean=22, head=-2, hx=10, hy=5, aR=74, fR=88, w=88, tF=40, sF=2, tB=-36, sB=12, hips=-12,
                 aL=70, fL=88)
TOOL_STUMBLE = dict(lean=34, head=14, hx=14, hy=9, aR=40, fR=50, w=40, tF=50, sF=2, tB=-30, sB=26, aL=40, fL=50)
T = poses(
    guard=TOOL_GUARD,
    feint=dict(TOOL_GUARD, aR=40, fR=84, w=86, hx=4, lean=16),
    sweep_back=dict(lean=2, head=-8, hx=-4, hy=4, aR=-14, fR=24, w=250, tF=18, sF=4, tB=-34, sB=20, hips=-24,
                    twist=-40, aL=10, fL=40),
    sweep=dict(lean=26, head=4, hx=8, hy=8, aR=72, fR=80, w=62, tF=44, sF=-4, tB=-36, sB=22, hips=20, twist=36,
               armY=-30, aL=60, fL=80),
    sweep_end=dict(lean=30, head=12, hx=12, hy=9, aR=30, fR=30, w=12, tF=40, sF=2, tB=-30, sB=26, hips=26,
                   twist=44, armY=-40, aL=30, fL=30),
    pull=dict(lean=8, head=-6, hx=-4, hy=4, aR=-14, fR=64, w=86, tF=18, sF=4, tB=-30, sB=18, hips=-12, aL=10,
              fL=70),
    poke=TOOL_POKE,
    poke2=dict(TOOL_POKE, hx=14, lean=26),
    run=dict(lean=26, head=-16, aR=30, fR=82, w=86, tF=46, sF=30, tB=-42, sB=52, aL=40, fL=84),
    ram=dict(TOOL_POKE, hx=18, lean=30, tF=56, sF=6, tB=-48, sB=10),
    stumble=TOOL_STUMBLE,
)

SETS["disciple"].update({
    "sword_stance": [(0, S["guard"]), (20, S["sway"]), (40, S["guard"])],
    "sword_strike": [(0, S["guard"]), (4, S["armed"]), (6, S["armed"]), (8, S["cut"]), (14, S["cut_end"]),
                     (22, S["guard"])],
    "sword_double": [(0, S["guard"]), (4, S["armed"]), (6, S["armed"]), (8, S["cut"]), (12, S["turn"]),
                     (15, S["backhand"]), (20, S["stagger"]), (28, S["guard"])],
    "sword_thrust": [(0, S["guard"]), (6, S["coil"]), (9, S["lunge"]), (11, S["lunge"]), (18, S["coil"]),
                     (26, S["guard"])],
    "axe_stance": [(0, A["guard"]), (24, A["breath"]), (48, A["guard"])],
    "axe_strike": [(0, A["guard"]), (8, A["raise_"]), (11, A["hold"]), (12, A["chop"]), (14, A["stuck"]),
                   (21, A["stuck"]), (26, A["heave"]), (30, A["guard"])],
    "axe_double": [(0, A["guard"]), (5, A["sweep_back"]), (8, A["sweep"]), (15, A["carry"]), (20, A["chop"]),
                   (22, A["stuck"]), (29, A["stuck"]), (34, A["guard"])],
    "axe_lunge": [(0, A["guard"]), (6, A["run"]), (10, A["leap"]), (12, A["chop"]), (14, A["stuck"]),
                  (22, A["stuck"]), (26, A["heave"]), (30, A["guard"])],
    "tool_stance": [(0, T["guard"]), (12, T["feint"]), (16, T["guard"]), (34, T["feint"]), (38, T["guard"]),
                    (48, T["guard"])],
    "tool_strike": [(0, T["guard"]), (6, T["sweep_back"]), (8, T["sweep"]), (14, T["sweep_end"]),
                    (22, T["guard"])],
    "tool_double": [(0, T["guard"]), (3, T["pull"]), (5, T["poke"]), (9, T["pull"]), (12, T["poke2"]),
                    (18, T["stumble"]), (24, T["guard"])],
    "tool_lunge": [(0, T["guard"]), (6, T["run"]), (10, T["ram"]), (12, T["ram"]), (19, T["stumble"]),
                   (26, T["guard"])],
})
# The clips whose left hand holds the haft (bake_second_hand), by item.
TWO_HANDED = {name: name.split("_")[0] for name in SETS["disciple"] if name.split("_")[0] in ("axe", "tool")}



def rotation(degrees):
    x, y, z = (math.radians(d) for d in degrees)
    return Quaternion((1, 0, 0), x) @ Quaternion((0, 1, 0), y) @ Quaternion((0, 0, 1), z)


# Step cycles keyed straight from key to key: Blender's default ease stops the planted foot at every key and then
# hurries it, so it slides against the ground; at an even speed it keeps up with the body.
LINEAR = {"walk", "run", "sneak_walk", "stance_walk", "climb"}


def make_action(armature, name, keys):
    """Keys the clip into a new action named `name` (assigned to the armature) and returns it."""
    bpy.context.scene.render.fps = FPS
    bpy.context.preferences.edit.keyframe_new_interpolation_type = "LINEAR" if name in LINEAR else "BEZIER"
    if armature.animation_data is None:
        armature.animation_data_create()
    action = bpy.data.actions.new(name)
    action.use_fake_user = True
    armature.animation_data.action = action
    pose = armature.pose.bones
    for bone in pose:
        bone.rotation_mode = "QUATERNION"
        bone.rotation_quaternion = Quaternion()
        bone.location = Vector()
    held = {}
    for frame, values in keys:
        held.update(values)
        for joint, value in held.items():
            bone = pose[joint]
            if isinstance(value, dict):
                rot, loc = value.get("rot"), value.get("loc")
            else:
                rot, loc = value, None
            if rot is not None:
                bone.rotation_quaternion = rotation(rot)
                bone.keyframe_insert("rotation_quaternion", frame=frame)
            if loc is not None:
                bone.location = Vector(loc)
                bone.keyframe_insert("location", frame=frame)
    if name in TWO_HANDED:
        bake_second_hand(armature, keys[-1][0], TWO_HANDED[name])
    return action


def bake_second_hand(armature, last, item):
    """
    Puts the left hand on the haft of the item held in the right, on every frame: the right arm first swings the haft
    in toward the left shoulder if it is out of reach, then a two-bone reach (the arm, then the forearm) takes the left
    hand to the haft point murim_items gives, the elbow bending the way it already bent; then keys both arms.
    """
    import murim_items
    on_haft = Vector(murim_items.point(murim_items.SECOND_HAND[item]))
    pose = armature.pose.bones
    scene = bpy.context.scene
    for frame in range(0, last + 1):
        scene.frame_set(frame)
        # Out of the left arm's reach, the right arm swings the haft in toward the left shoulder until it is in reach.
        left = pose["Arm_L"].head.copy()
        reach = (pose["Hand_L"].head - left).length + (pose["Tool_L"].head - pose["Hand_L"].head).length
        for _ in range(12):
            target = pose["Tool_R"].matrix @ on_haft
            if (target - left).length <= reach * 0.96:
                break
            right = pose["Arm_R"].head.copy()
            axis = (target - right).cross(left - right)
            if axis.length < 1e-6:
                break
            turn(pose["Arm_R"], right, Quaternion(axis.normalized(), math.radians(6)))
            bpy.context.view_layer.update()
        target = pose["Tool_R"].matrix @ on_haft
        shoulder, elbow, wrist = pose["Arm_L"].head.copy(), pose["Hand_L"].head.copy(), pose["Tool_L"].head.copy()
        upper, lower = (elbow - shoulder).length, (wrist - elbow).length
        toward = target - shoulder
        distance = min(max(toward.length, 1e-3), upper + lower - 1e-3)
        toward.normalize()
        bend = (elbow - shoulder) - toward * (elbow - shoulder).dot(toward)
        bend = bend.normalized() if bend.length > 1e-4 else Vector((0, 0, -1))
        cos = max(-1.0, min(1.0, (upper * upper + distance * distance - lower * lower) / (2 * upper * distance)))
        new_elbow = shoulder + toward * upper * cos + bend * upper * math.sqrt(1 - cos * cos)
        turn(pose["Arm_L"], shoulder, (elbow - shoulder).rotation_difference(new_elbow - shoulder))
        bpy.context.view_layer.update()
        elbow, wrist = pose["Hand_L"].head.copy(), pose["Tool_L"].head.copy()
        turn(pose["Hand_L"], elbow, (wrist - elbow).rotation_difference(target - elbow))
        bpy.context.view_layer.update()
        for name in ("Arm_R", "Arm_L", "Hand_L"):
            pose[name].keyframe_insert("rotation_quaternion", frame=frame)


def turn(bone, about, rotation):
    """Turns a pose bone by `rotation` (armature space) about the point `about`."""
    bone.matrix = (Matrix.Translation(about) @ rotation.to_matrix().to_4x4() @ Matrix.Translation(-about)
                   @ bone.matrix)


def build_set(armature, set_name):
    """Every clip of the set as an action; returns {clip name: last frame}."""
    lengths = {}
    for name, keys in SETS[set_name].items():
        make_action(armature, name, keys)
        lengths[name] = keys[-1][0]
    return lengths
