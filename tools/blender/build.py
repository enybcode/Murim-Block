"""Builds the biped and saves it as a .blend (to animate) and/or exports it as glTF (for the mod).

blender -b --factory-startup --python tools/blender/build.py -- [--slim] [--blend out.blend] [--gltf out.gltf]
"""

import argparse
import os
import sys

import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import murim_biped  # noqa: E402


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--slim", action="store_true", help="slim (Alex) arms")
    parser.add_argument("--blend", help="save the scene here")
    parser.add_argument("--gltf", help="export glTF (.gltf + .bin) here")
    args = parser.parse_args(argv)

    murim_biped.clear_scene()
    murim_biped.build(slim=args.slim)
    if args.blend:
        os.makedirs(os.path.dirname(os.path.abspath(args.blend)), exist_ok=True)
        bpy.ops.wm.save_as_mainfile(filepath=os.path.abspath(args.blend))
        print("MURIM saved", args.blend)
    if args.gltf:
        export(args.gltf)


def export(path):
    path = os.path.abspath(path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format="GLTF_SEPARATE",
        export_yup=True,
        export_apply=False,
        export_texcoords=True,
        export_normals=True,
        export_materials="NONE",
        export_skins=True,
        export_all_influences=False,
        export_def_bones=False,
        export_animations=True,
        export_extras=True,
    )
    print("MURIM exported", path)


main()
