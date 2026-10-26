"""Films the biped from the front, the side and three quarters, the three views side by side in each frame.

blender -b --factory-startup --python tools/blender/render.py -- --out <frames dir> [--slim] [--skin skin.png]
    [--clip bend] [--size 320]

Without --skin it uses a test skin that shows the UV layout: one colour per face (front red, back blue, right
side green, left side yellow, top white, bottom magenta), a checker per pixel to show stretching, and an empty
outer layer. Make a GIF of the frames with scripts/frames-to-gif.java.
"""

import argparse
import math
import os
import sys

import bpy
import numpy as np
from mathutils import Quaternion, Vector

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import murim_biped  # noqa: E402

FACE_COLORS = {
    "front": (0.85, 0.2, 0.2), "back": (0.2, 0.35, 0.9), "right": (0.2, 0.75, 0.25),
    "left": (0.95, 0.85, 0.2), "top": (0.95, 0.95, 0.95), "bottom": (0.8, 0.2, 0.8),
}


def test_skin(slim):
    """A 64x64 UV check skin as a Blender image."""
    pixels = np.zeros((murim_biped.SKIN_SIZE, murim_biped.SKIN_SIZE, 4), dtype=np.float32)
    for _name, outer, _corner, size, (u, v), *_ in murim_biped.parts(slim):
        if outer:
            continue
        w, h, d = (int(math.ceil(s)) for s in size)
        regions = {
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }
        for face, (x, y, rw, rh) in regions.items():
            color = np.array(FACE_COLORS[face])
            for py in range(y, y + rh):
                for px in range(x, x + rw):
                    shade = 1.0 if (px + py) % 2 == 0 else 0.7
                    # The first row of each side face is darker, to show which way is up.
                    if face not in ("top", "bottom") and py == y:
                        shade *= 0.5
                    pixels[py, px] = (*(color * shade), 1.0)
    image = bpy.data.images.new("test_skin", murim_biped.SKIN_SIZE, murim_biped.SKIN_SIZE, alpha=True)
    # Blender images start at the bottom row.
    image.pixels.foreach_set(np.flipud(pixels).ravel())
    return image


def skin_material(image):
    material = bpy.data.materials.new("skin")
    material.use_nodes = True
    nodes = material.node_tree.nodes
    bsdf = nodes.get("Principled BSDF")
    texture = nodes.new("ShaderNodeTexImage")
    texture.image = image
    texture.interpolation = "Closest"
    links = material.node_tree.links
    links.new(texture.outputs["Color"], bsdf.inputs["Base Color"])
    links.new(texture.outputs["Alpha"], bsdf.inputs["Alpha"])
    bsdf.inputs["Roughness"].default_value = 1.0
    if hasattr(material, "surface_render_method"):
        material.surface_render_method = "DITHERED"
    return material


# Test clips: joint rotations (degrees about X then Y) at the middle frame, and how the cameras frame them
# (model point looked at, ortho scale in pixels).
CLIPS = {
    "rest": ({}, (0, 16, 0), 44),
    "bend": ({
        "Arm_L": (-60, 0), "Arm_R": (-30, 0),
        "Hand_L": (-150, 0), "Hand_R": (-110, 0),
        "Thigh_L": (-70, 0), "Leg_L": (120, 0),
        "Thigh_R": (20, 0), "Leg_R": (60, 0),
        "Chest": (0, 25), "Head": (0, -25),
    }, (0, 16, 0), 44),
    # Close-up on the left elbow and knee folding to 135°, to judge the joints.
    "joints": ({"Hand_L": (-135, 0), "Leg_L": (135, 0)}, (5, 12, 0), 28),
    "elbow": ({"Hand_L": (-135, 0)}, (6, 18, 0), 16),
    "knee": ({"Leg_L": (135, 0)}, (2, 6, 0), 16),
}


def play_clip(armature, targets, frames):
    """Rest, then the clip's rotations at the middle frame, then rest again."""
    pose = armature.pose.bones
    for bone in pose:
        bone.rotation_mode = "QUATERNION"
    half = frames // 2
    for frame, amount in ((0, 0.0), (half, 1.0), (frames, 0.0)):
        for name, (x, y) in targets.items():
            bone = pose[name]
            # Bone axes match the mod's model axes (X left, Y up along the bone, Z forward).
            rotation = Quaternion((1, 0, 0), math.radians(x * amount)) @ Quaternion((0, 1, 0), math.radians(y * amount))
            bone.rotation_quaternion = rotation
            bone.keyframe_insert("rotation_quaternion", frame=frame)


def camera(name, position, target, scale):
    data = bpy.data.cameras.new(name)
    data.type = "ORTHO"
    data.ortho_scale = scale
    cam = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(cam)
    cam.location = position
    direction = Vector(target) - Vector(position)
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    return cam


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    parser.add_argument("--slim", action="store_true")
    parser.add_argument("--skin")
    parser.add_argument("--clip", default="bend", choices=sorted(CLIPS))
    parser.add_argument("--frames", type=int, default=48)
    parser.add_argument("--step", type=int, default=2)
    parser.add_argument("--size", type=int, default=320)
    args = parser.parse_args(argv)

    murim_biped.clear_scene()
    scene = bpy.context.scene
    armature = murim_biped.build(slim=args.slim)
    image = bpy.data.images.load(os.path.abspath(args.skin)) if args.skin else test_skin(args.slim)
    material = skin_material(image)
    for obj in scene.objects:
        if obj.type == "MESH":
            obj.data.materials.append(material)
    targets, (cx, cy, cz), scale = CLIPS[args.clip]
    if targets:
        play_clip(armature, targets, args.frames)
    else:
        args.frames = 0

    world = bpy.data.worlds.new("world")
    world.color = (0.35, 0.38, 0.42)
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.35, 0.38, 0.42, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 1.2
    scene.world = world
    sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
    sun.data.energy = 2.5
    sun.rotation_euler = (math.radians(50), 0, math.radians(-30))
    scene.collection.objects.link(sun)

    center = murim_biped.blender(cx, cy, cz)
    cams = [
        camera("front", murim_biped.blender(cx, cy, cz + 60), center, scale),
        camera("side", murim_biped.blender(cx + 60, cy, cz), center, scale),
        camera("three_quarter", murim_biped.blender(cx + 42, cy + 6, cz + 42), center, scale),
    ]
    engines = {item.identifier for item in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items}
    scene.render.engine = "BLENDER_EEVEE" if "BLENDER_EEVEE" in engines else "BLENDER_EEVEE_NEXT"
    scene.render.resolution_x = args.size
    scene.render.resolution_y = int(args.size * 1.25)
    scene.render.image_settings.file_format = "PNG"
    if hasattr(scene, "eevee"):
        scene.eevee.taa_render_samples = 8

    out = os.path.abspath(args.out)
    views = os.path.join(out, "views")
    os.makedirs(views, exist_ok=True)
    count = 0
    for frame in range(0, args.frames + 1, args.step):
        scene.frame_set(frame)
        strips = []
        for cam in cams:
            scene.camera = cam
            path = os.path.join(views, f"{cam.name}.png")
            scene.render.filepath = path
            bpy.ops.render.render(write_still=True)
            shot = bpy.data.images.load(path, check_existing=False)
            strips.append(np.array(shot.pixels[:], dtype=np.float32).reshape(shot.size[1], shot.size[0], 4))
            bpy.data.images.remove(shot)
        joined = np.concatenate(strips, axis=1)
        frame_image = bpy.data.images.new("frame", joined.shape[1], joined.shape[0], alpha=True)
        frame_image.pixels.foreach_set(joined.ravel())
        frame_image.filepath_raw = os.path.join(out, f"frame_{count:03d}.png")
        frame_image.file_format = "PNG"
        frame_image.save()
        bpy.data.images.remove(frame_image)
        count += 1
    print("MURIM rendered", count, "frames to", out)


main()
