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
from mathutils import Quaternion, Vector

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


def rotation(degrees):
    x, y, z = (math.radians(d) for d in degrees)
    return Quaternion((1, 0, 0), x) @ Quaternion((0, 1, 0), y) @ Quaternion((0, 0, 1), z)


def make_action(armature, name, keys):
    """Keys the clip into a new action named `name` (assigned to the armature) and returns it."""
    bpy.context.scene.render.fps = FPS
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
    return action


def build_set(armature, set_name):
    """Every clip of the set as an action; returns {clip name: last frame}."""
    lengths = {}
    for name, keys in SETS[set_name].items():
        make_action(armature, name, keys)
        lengths[name] = keys[-1][0]
    return lengths
