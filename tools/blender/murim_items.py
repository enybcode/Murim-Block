"""Held items in Blender as the mod draws them, to see weapons in the renders and to put a second hand on a haft.

The mod draws an item held in the right hand at the Tool_R joint, through SkeletalPlayerRenderer.RIGHT_GRIP (our
joint to vanilla's hand) and vanilla's "handheld" third person transform; a flat item lies in its square [0, 1] x
[0, 1] at z 0.5, and a sword, axe or tool runs along its diagonal, from the handle at (0, 0) to the head at (1, 1).
Matrices here map that square to the joint's own space in model pixels (X left, Y up, Z forward), which is also the
space of an object parented to the joint's bone (from its head).
"""

import math

import bpy
from mathutils import Euler, Matrix, Vector

PIXEL = 1 / 16


def _rot(axis, degrees):
    return Matrix.Rotation(math.radians(degrees), 4, axis)


def right_grip():
    """SkeletalPlayerRenderer.RIGHT_GRIP (blocks)."""
    return (Matrix.Translation((1 * PIXEL, 9 * PIXEL, 0)) @ _rot("X", 90) @ _rot("Y", 180)
            @ Matrix.Translation((1 * PIXEL, 2 * PIXEL, -10 * PIXEL)))


def handheld():
    """item/handheld's thirdperson_righthand, as ItemTransform.apply then ItemRenderer's -0.5 (blocks)."""
    rotation = _rot("X", 0) @ _rot("Y", -90) @ _rot("Z", 55)  # JOML rotationXYZ: X, then Y, then Z
    return (Matrix.Translation((0, 4 * PIXEL, 0.5 * PIXEL)) @ rotation @ Matrix.Scale(0.85, 4)
            @ Matrix.Translation((-0.5, -0.5, -0.5)))


def item_to_joint():
    """The item's square to the Tool_R joint's space, in model pixels."""
    return Matrix.Scale(16, 4) @ right_grip() @ handheld()


def along(t, across=0.0):
    """A point of the item's diagonal (t from the handle, 0, to the head, 1), `across` it toward its top left."""
    return Vector((t - across, t + across, 0.5))


def point(t, across=0.0):
    """That point in the joint's space (pixels)."""
    return item_to_joint() @ along(t, across)


# Where a hand holds the haft (t along the diagonal): the right hand's own grip is where the mod puts it; the left
# hand of a two-handed hold goes further up the haft.
SECOND_HAND = {"axe": 0.33, "tool": 0.38}

# The proxies' shapes: (from t, to t, half width across the diagonal, colour).
SHAPES = {
    "sword": [(0.04, 0.24, 0.035, (0.35, 0.22, 0.12)), (0.24, 0.27, 0.16, (0.55, 0.58, 0.62)),
              (0.27, 0.95, 0.06, (0.78, 0.82, 0.86))],
    "axe": [(0.04, 0.86, 0.035, (0.42, 0.29, 0.18)), (0.66, 0.92, 0.2, (0.6, 0.63, 0.67))],
    "tool": [(0.04, 0.62, 0.035, (0.42, 0.29, 0.18)), (0.62, 0.95, 0.13, (0.6, 0.63, 0.67))],
}


def proxy(armature, kind, bone="Tool_R"):
    """A flat stand-in of the item, held by `bone` (renders only; not exported)."""
    to_joint = item_to_joint()
    mesh = bpy.data.meshes.new(f"held_{kind}")
    verts, faces, colours = [], [], []
    for start, end, half, colour in SHAPES[kind]:
        base = len(verts)
        for t, a in ((start, -half), (end, -half), (end, half), (start, half)):
            for z in (0.47, 0.53):
                p = Vector((t - a, t + a, z))
                verts.append(to_joint @ p)
        # A thin box: front, back and the four sides.
        quads = [(0, 2, 4, 6), (1, 7, 5, 3), (0, 1, 3, 2), (2, 3, 5, 4), (4, 5, 7, 6), (6, 7, 1, 0)]
        faces += [tuple(base + i for i in quad) for quad in quads]
        colours += [colour] * len(quads)
    mesh.from_pydata([tuple(v) for v in verts], [], faces)
    mesh.update()
    obj = bpy.data.objects.new(f"held_{kind}", mesh)
    for colour in dict.fromkeys(colours):
        material = bpy.data.materials.new(f"held_{kind}_{len(obj.data.materials)}")
        material.diffuse_color = (*colour, 1)
        material.use_nodes = True
        material.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (*colour, 1)
        obj.data.materials.append(material)
    index = {c: i for i, c in enumerate(dict.fromkeys(colours))}
    for polygon, colour in zip(mesh.polygons, colours):
        polygon.material_index = index[colour]
    bpy.context.scene.collection.objects.link(obj)
    parent_to_joint(obj, armature, bone)
    return obj


def parent_to_joint(obj, armature, bone):
    """Parents `obj` to a joint so that its own coordinates are the joint's space (pixels, from the joint)."""
    obj.parent = armature
    obj.parent_type = "BONE"
    obj.parent_bone = bone
    # A bone child hangs from the bone's tail (2 px up its Y); bone axes are the mod's (X, Y up, Z forward) once
    # Blender's own bone frame (Y along the bone) is taken into account.
    obj.matrix_parent_inverse = Matrix.Identity(4)
    obj.matrix_basis = bone_frame() @ Matrix.Translation((0, -2, 0))


def bone_frame():
    """The mod's joint axes in Blender's bone axes (a bone pointing up with no roll)."""
    # Blender: the bone's Y is world Z (up), its X world X, its Z world -Y... and the mod's model point (x, y, z) is
    # Blender's (x, -z, y). In bone space a mod vector (x, y, z) is therefore (x, y, z): the frames agree.
    return Matrix.Identity(4)


# The body's boxes a held item must stay out of: joint, model min and max corners (rest, pixels).
BODY = [
    ("head", "Head", (-4, 24, -4), (4, 32, 4)),
    ("chest", "Chest", (-4, 18, -2), (4, 24, 2)),
    ("belly", "Torso", (-4, 12, -2), (4, 18, 2)),
    ("thigh R", "Thigh_R", (-3.9, 6, -2), (0.1, 12, 2)),
    ("thigh L", "Thigh_L", (-0.1, 6, -2), (3.9, 12, 2)),
    ("shin R", "Leg_R", (-3.9, 0, -2), (0.1, 6, 2)),
    ("shin L", "Leg_L", (-0.1, 0, -2), (3.9, 6, 2)),
    ("arm L", "Arm_L", (4, 18, -2), (8, 24, 2)),
    ("forearm L", "Hand_L", (4, 12, -2), (8, 18, 2)),
]
# Along the item, from just past the hand to the tip.
SAMPLES = [0.3 + 0.05 * i for i in range(14)]


def body_hits(armature, two_hands, margin=0.5):
    """The body parts the item in the right hand passes through now (a two-handed hold may touch the left arm)."""
    pose = armature.pose.bones
    tool = pose["Tool_R"].matrix
    found = set()
    for t in SAMPLES:
        for across in (-0.04, 0.04):
            p = tool @ point(t, across)
            for name, joint, low, high in BODY:
                if two_hands and joint in ("Arm_L", "Hand_L"):
                    continue
                bone = pose[joint]
                rest = bone.bone.matrix_local @ bone.matrix.inverted() @ p
                x, y, z = rest.x, rest.z, -rest.y  # Blender (x, y, z) is the model's (x, -z, y)
                if all(lo + margin < v < hi - margin for v, lo, hi in zip((x, y, z), low, high)):
                    found.add(name)
    return found
