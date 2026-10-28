"""Builds the biped, keys a clip set from murim_clips.py and exports its animations as glTF (skeleton only, no mesh).

blender -b --factory-startup --python tools/blender/export_clips.py -- --set test --gltf out.gltf [--blend out.blend]

A mob with a skeleton of its own (tools/blender/mobs/<mob>.py with JOINTS) gives its clips as CLIPS:
--set spider --mob spider builds its skeleton instead of the biped.
"""

import argparse
import os
import sys

import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import murim_biped  # noqa: E402
import murim_clips  # noqa: E402


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--set", required=True)
    parser.add_argument("--mob", help="a mob of tools/blender/mobs with its own JOINTS and CLIPS")
    parser.add_argument("--gltf", required=True)
    parser.add_argument("--blend", help="also save the scene with the actions here, to edit them")
    args = parser.parse_args(argv)

    murim_biped.clear_scene()
    if args.mob:
        sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "mobs"))
        import importlib
        import murim_mob
        mob = importlib.import_module(args.mob)
        murim_clips.SETS[args.set] = mob.CLIPS
        armature = murim_mob.build([], 16, 16, args.set, joints=mob.JOINTS, helpers=mob.HELPERS)
    else:
        armature = murim_biped.build()
    lengths = murim_clips.build_set(armature, args.set)
    scene = bpy.context.scene
    scene.frame_start = 0
    scene.frame_end = max(lengths.values())
    if args.blend:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(args.blend))

    bpy.ops.object.select_all(action="DESELECT")
    armature.select_set(True)
    bpy.context.view_layer.objects.active = armature
    path = os.path.abspath(args.gltf)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format="GLTF_SEPARATE",
        use_selection=True,
        export_yup=True,
        export_skins=False,
        export_def_bones=False,
        export_animations=True,
        export_animation_mode="ACTIONS",
        export_force_sampling=True,
        export_frame_step=1,
        export_anim_slide_to_zero=True,
        export_optimize_animation_size=False,
        export_extras=False,
    )
    print("MURIM exported", ", ".join(f"{name} ({frames} frames)" for name, frames in lengths.items()), "to", path)


main()
