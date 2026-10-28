"""Builds a mob of tools/blender/mobs/: paints its texture, exports its model, films it from every side.

blender -b --factory-startup --python tools/blender/mob.py -- --mob disciple
    [--texture out.png] [--gltf out.gltf] [--blend out.blend] [--renders dir] [--size 480]

The texture (and <name>_glow.png for glowing parts) is what the mod draws; --renders writes close views from the
front, back, sides and three quarters, the head close up, and a pose, for Enzo to judge before the model goes in game.
"""

import argparse
import importlib
import math
import os
import sys

import bpy
from mathutils import Quaternion, Vector

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "mobs"))
import murim_biped  # noqa: E402
import murim_mob  # noqa: E402

# A fighting pose for the renders: joint rotations in degrees about X then Y.
POSE = {
    "Arm_R": (-75, 10), "Hand_R": (-30, 0), "Arm_L": (-25, -10), "Hand_L": (-70, 0),
    "Chest": (8, 15), "Head": (-12, -10), "Thigh_L": (-25, 0), "Leg_L": (30, 0), "Thigh_R": (15, 0),
}


def material(image, glow):
    mat = bpy.data.materials.new("mob")
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    bsdf = nodes.get("Principled BSDF")
    texture = nodes.new("ShaderNodeTexImage")
    texture.image = image
    texture.interpolation = "Closest"
    links.new(texture.outputs["Color"], bsdf.inputs["Base Color"])
    links.new(texture.outputs["Alpha"], bsdf.inputs["Alpha"])
    bsdf.inputs["Roughness"].default_value = 1.0
    if glow is not None:
        light = nodes.new("ShaderNodeTexImage")
        light.image = glow
        light.interpolation = "Closest"
        links.new(light.outputs["Color"], bsdf.inputs["Emission Color"])
        bsdf.inputs["Emission Strength"].default_value = float(os.environ.get("MURIM_GLOW", "1.6"))
    if hasattr(mat, "surface_render_method"):
        mat.surface_render_method = "DITHERED"
    mat.use_backface_culling = False
    return mat


def camera(name, position, target, scale):
    data = bpy.data.cameras.new(name)
    data.type = "ORTHO"
    data.ortho_scale = scale
    cam = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(cam)
    cam.location = position
    cam.rotation_euler = (Vector(target) - Vector(position)).to_track_quat("-Z", "Y").to_euler()
    return cam


def light(name, kind, energy, rotation, colour):
    lamp = bpy.data.objects.new(name, bpy.data.lights.new(name, kind))
    lamp.data.energy = energy
    lamp.data.color = colour
    lamp.rotation_euler = [math.radians(a) for a in rotation]
    bpy.context.scene.collection.objects.link(lamp)


def pose(armature, rotations):
    for bone in armature.pose.bones:
        bone.rotation_mode = "QUATERNION"
        x, y = rotations.get(bone.name, (0, 0))
        bone.rotation_quaternion = Quaternion((1, 0, 0), math.radians(x)) @ Quaternion((0, 1, 0), math.radians(y))


def render(armature, out, size):
    scene = bpy.context.scene
    world = bpy.data.worlds.new("world")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.42, 0.45, 0.5, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.9
    scene.world = world
    light("key", "SUN", 3.2, (55, 0, -35), (1.0, 0.95, 0.86))
    light("rim", "SUN", 1.4, (60, 0, 150), (0.75, 0.85, 1.0))
    engines = {item.identifier for item in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items}
    scene.render.engine = "BLENDER_EEVEE" if "BLENDER_EEVEE" in engines else "BLENDER_EEVEE_NEXT"
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    if hasattr(scene, "eevee"):
        scene.eevee.taa_render_samples = 16
    b = murim_biped.blender
    body, face = b(0, 17, 0), b(0, 28, 1)
    views = [
        # name, camera position (model), target, ortho scale (pixels), aspect (height / width)
        ("front", b(0, 17, 80), body, 40, 1.25),
        ("back", b(0, 17, -80), body, 40, 1.25),
        ("right", b(-80, 17, 0), body, 40, 1.25),
        ("left", b(80, 17, 0), body, 40, 1.25),
        ("three_quarter", b(52, 30, 58), body, 40, 1.25),
        ("three_quarter_back", b(-56, 28, -54), body, 40, 1.25),
        ("head", b(30, 32, 50), face, 16, 1.0),
    ]
    os.makedirs(out, exist_ok=True)
    for name, position, target, scale, aspect in views:
        scene.camera = camera(name, position, target, scale)
        scene.render.resolution_x = size
        scene.render.resolution_y = int(size * aspect)
        scene.render.filepath = os.path.join(out, f"{name}.png")
        bpy.ops.render.render(write_still=True)
    pose(armature, POSE)
    scene.camera = camera("pose", b(46, 24, 64), body, 42)
    scene.render.resolution_x = scene.render.resolution_y = size
    scene.render.filepath = os.path.join(out, "pose.png")
    bpy.ops.render.render(write_still=True)
    pose(armature, {})
    print("MURIM rendered views to", out)


def film(armature, clips, out, size):
    """Each clip from three quarters, a frame per tick, into <out>/<clip>/frame_NNN.png."""
    import murim_clips
    scene = bpy.context.scene
    world = bpy.data.worlds.new("world")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.42, 0.45, 0.5, 1)
    scene.world = world
    light("key", "SUN", 3.2, (55, 0, -35), (1.0, 0.95, 0.86))
    light("rim", "SUN", 1.4, (60, 0, 150), (0.75, 0.85, 1.0))
    engines = {item.identifier for item in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items}
    scene.render.engine = "BLENDER_EEVEE" if "BLENDER_EEVEE" in engines else "BLENDER_EEVEE_NEXT"
    scene.render.resolution_x = scene.render.resolution_y = size
    b = murim_biped.blender
    scene.camera = camera("film", b(46, 22, 52), b(0, 14, 2), 46)
    for clip in clips:
        set_name, name = clip.split("/")
        keys = murim_clips.SETS[set_name][name]
        murim_clips.make_action(armature, name, keys)
        folder = os.path.join(out, name)
        os.makedirs(folder, exist_ok=True)
        for frame in range(0, keys[-1][0] + 1):
            scene.frame_set(frame)
            scene.render.filepath = os.path.join(folder, f"frame_{frame:03d}.png")
            bpy.ops.render.render(write_still=True)
    print("MURIM filmed", clips)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--mob", required=True)
    parser.add_argument("--texture")
    parser.add_argument("--gltf")
    parser.add_argument("--blend")
    parser.add_argument("--renders")
    parser.add_argument("--size", type=int, default=480)
    parser.add_argument("--film", help="clips of murim_clips.py as <set>/<clip>,... filmed frame by frame into --renders")
    args = parser.parse_args(argv)

    mob = importlib.import_module(args.mob)
    parts = mob.parts()
    width = mob.TEXTURE_WIDTH
    height = murim_mob.pack(parts, width)
    murim_biped.clear_scene()
    texture = murim_mob.paint_texture(parts, width, height)
    glow = murim_mob.paint_texture(parts, width, height, emissive=True)
    texture_path = args.texture or os.path.join(HERE, "out", f"{args.mob}.png")
    glow_path = texture_path[:-4] + "_glow.png"
    image = murim_mob.save_png(texture, texture_path, args.mob)
    glow_image = murim_mob.save_png(glow, glow_path, args.mob + "_glow") if glow[..., 3].any() else None
    print("MURIM texture", width, "x", height, "->", texture_path)
    armature = murim_mob.build(parts, width, height, args.mob)
    if args.gltf:
        path = os.path.abspath(args.gltf)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        bpy.ops.export_scene.gltf(filepath=path, export_format="GLTF_SEPARATE", export_yup=True, export_apply=False,
                                  export_texcoords=True, export_normals=True, export_materials="NONE",
                                  export_skins=True, export_all_influences=False, export_def_bones=False,
                                  export_animations=False, export_extras=True)
        print("MURIM exported", path)
        spec = murim_mob.cloth_spec(parts, width, height)
        if spec["cloth"]:
            import json
            with open(path[:-len(".gltf")] + ".json", "w", encoding="utf-8") as out:
                json.dump(spec, out, indent=1)
            print("MURIM cloth", [piece["name"] for piece in spec["cloth"]])
    mat = material(image, glow_image)
    for obj in bpy.context.scene.objects:
        if obj.type == "MESH":
            obj.data.materials.append(mat)
    if args.blend:
        os.makedirs(os.path.dirname(os.path.abspath(args.blend)), exist_ok=True)
        bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(args.blend))
    if args.renders and args.film:
        film(armature, args.film.split(","), os.path.abspath(args.renders), args.size)
    elif args.renders:
        render(armature, os.path.abspath(args.renders), args.size)


main()
