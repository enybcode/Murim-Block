"""Mob models on the biped's skeleton: the parts in relief, their texture painted in code, export and renders.

A mob (tools/blender/mobs/<name>.py) lists its parts: boxes in model pixels (Y up, facing +Z, left on +X), each
bound to a joint of the biped (rigid), to the trunk (twisting between Torso and Chest like the body) or built as a
limb (bending at the elbow or knee like the player's). The texture is packed and painted here, one texel per model
pixel, so every part keeps Minecraft's pixel size; a part may overlap another to stand out by less than a pixel.

Paint functions take (face, x, y, w, h) and return an (r, g, b, a) colour (0-255) or None (transparent):
face is "top", "bottom", "front", "back", "right" (the mob's right, -X) or "left"; x runs across the face as seen
from outside it, y down it (for the top, from the back to the front).
"""

import math
import os

import bpy
import numpy as np

import murim_biped

# Where the trunk's twist sits between the Torso and the Chest (the share of the Chest), as in murim_biped.
_TRUNK = [(12, 0.0), (14, 0.25), (16, 0.25), (18, 0.5), (20, 0.75), (22, 0.75), (24, 1.0)]


def trunk_weights(y):
    if y <= 12:
        return {"Torso": 1.0}
    if y >= 24:
        return {"Chest": 1.0}
    for (y0, c0), (y1, c1) in zip(_TRUNK, _TRUNK[1:]):
        if y0 <= y <= y1:
            chest = c0 + (c1 - c0) * (y - y0) / (y1 - y0)
            return {"Torso": 1 - chest, "Chest": chest} if chest < 1 else {"Chest": 1.0}
    return {"Chest": 1.0}


class Part:
    """One box. bind: a joint name (rigid), "trunk", or ("limb", upper, lower, helper, hinge y, opens)."""

    def __init__(self, name, bind, corner, size, paint, inflate=0.0, emissive=None, cloth=None):
        self.name, self.bind, self.corner, self.size = name, bind, corner, size
        self.paint, self.inflate, self.emissive = paint, inflate, emissive
        # Cloth ("front" or "back": the side of the body it hangs on) is simulated in game, not built: its corner is
        # the middle of its top edge, its size (width, length, 1); only its texture is packed here.
        self.cloth = cloth
        self.uv = None


def arm(side):
    return ("limb", f"Arm_{side}", f"Hand_{side}", f"Elbow_{side}", 18, "back")


def leg(side):
    return ("limb", f"Thigh_{side}", f"Leg_{side}", f"Knee_{side}", 6, "front")


def footprint(part):
    w, h, d = (int(math.ceil(s)) for s in part.size)
    return 2 * (d + w), d + h


def pack(parts, width):
    """Shelf packing, tallest first; returns the texture height (a power of two)."""
    order = sorted(parts, key=lambda p: (-footprint(p)[1], -footprint(p)[0], p.name))
    x = y = shelf = 0
    for part in order:
        fw, fh = footprint(part)
        if x + fw > width:
            x, y, shelf = 0, y + shelf, 0
        part.uv = (x, y)
        x += fw
        shelf = max(shelf, fh)
    height = 16
    while height < y + shelf:
        height *= 2
    return height


def regions(part):
    w, h, d = (int(math.ceil(s)) for s in part.size)
    u, v = part.uv
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def paint_texture(parts, width, height, emissive=False):
    pixels = np.zeros((height, width, 4), dtype=np.float32)
    for part in parts:
        paint = part.emissive if emissive else part.paint
        if paint is None:
            continue
        for face, (fx, fy, fw, fh) in regions(part).items():
            for y in range(fh):
                for x in range(fw):
                    colour = paint(face, x, y, fw, fh)
                    if colour is not None:
                        r, g, b, *a = colour
                        pixels[fy + y, fx + x] = (r / 255, g / 255, b / 255, (a[0] if a else 255) / 255)
    return pixels


def save_png(pixels, path, name):
    height, width = pixels.shape[:2]
    image = bpy.data.images.new(name, width, height, alpha=True)
    image.pixels.foreach_set(np.flipud(pixels).ravel())
    image.filepath_raw = os.path.abspath(path)
    image.file_format = "PNG"
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    image.save()
    return image


def build(parts, width, height, name, collection=None):
    """The biped's armature with the mob's parts skinned to it; returns the armature."""
    collection = collection or bpy.context.scene.collection
    data = bpy.data.armatures.new(name)
    armature = bpy.data.objects.new(name, data)
    collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    bpy.ops.object.mode_set(mode="EDIT")
    bones = {}
    for joint, parent, position in murim_biped.JOINTS:
        bone = data.edit_bones.new(joint)
        bone.head = murim_biped.blender(*position)
        bone.tail = murim_biped.blender(position[0], position[1] + 2, position[2])
        bone.roll = 0
        if parent:
            bone.parent = bones[parent]
            bone.use_connect = False
        bones[joint] = bone
    bpy.ops.object.mode_set(mode="OBJECT")
    for helper, follows in murim_biped.HELPERS:
        constraint = armature.pose.bones[helper].constraints.new("COPY_ROTATION")
        constraint.target = armature
        constraint.subtarget = follows
        constraint.owner_space = "LOCAL"
        constraint.target_space = "LOCAL"
        constraint.influence = 0.5
    for part in parts:
        if part.cloth:
            continue
        box = murim_biped.BoxBuilder(part.name, part.corner, part.size, part.uv, part.inflate, tex_height=height,
                                     tex_width=width)
        y0, h = part.corner[1], part.size[1]
        if isinstance(part.bind, tuple):
            _, upper, lower, helper, hinge, opens = part.bind
            box.sides(hinge, y0 + h, {upper: 1.0}, {upper: 1.0})
            box.top({upper: 1.0})
            box.sides(y0, hinge, {lower: 1.0}, {lower: 1.0})
            box.bottom({lower: 1.0})
            box.hinge(hinge, upper, lower, helper, opens)
        else:
            weights = trunk_weights if part.bind == "trunk" else (lambda y, joint=part.bind: {joint: 1.0})
            levels = sorted({y0, y0 + h, *(y for y, _ in _TRUNK if y0 < y < y0 + h)}) if part.bind == "trunk" \
                else [y0, y0 + h]
            for lo, hi in zip(levels, levels[1:]):
                box.sides(lo, hi, weights(lo), weights(hi))
            box.top(weights(y0 + h))
            box.bottom(weights(y0))
        obj = box.finish(armature)
        collection.objects.link(obj)
    return armature


# Painting helpers.

def hexes(*codes):
    return [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in codes]


def noise(x, y, salt):
    h = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFFFF) / 4294967296


def shade(ramp, face, x, y, h, salt, rough=0.1, offset=0):
    """A colour from a 5-tone ramp (dark to light), shaded by hand: light from above, the top row of a side catching
    it, the bottom row in shadow, a little grain."""
    level = 2 + offset
    if face == "top":
        level += 1
    elif face == "bottom":
        level -= 2
    else:
        if y == 0:
            level += 1
        elif y == h - 1:
            level -= 1
    n = noise(x, y, salt)
    if n < rough:
        level -= 1
    elif n > 1 - rough * 0.6:
        level += 1
    return ramp[max(0, min(len(ramp) - 1, level))]


def cloth_spec(parts, width, height):
    """The cloth pieces for the mod (skeletal/mobs/<name>.json): where each hangs and its two faces in the texture."""
    pieces = []
    for part in parts:
        if not part.cloth:
            continue
        faces = regions(part)
        pieces.append({
            "name": part.name, "joint": part.bind, "side": part.cloth, "pin": list(part.corner),
            "width": part.size[0], "length": part.size[1],
            "front": list(faces["front"]), "back": list(faces["back"]),
        })
    return {"texture": [width, height], "cloth": pieces}
