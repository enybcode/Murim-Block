"""Finds the frames where a held item passes through the body, for a mob's armed clips.

blender -b --factory-startup --python tools/blender/clip_check.py -- --mob disciple --clips sword_strike,axe_strike

For each frame, points along the item held in the right hand (murim_items, from just past the hand to the tip) are
tested against the body's boxes (head, chest, belly, thighs, shins, the left arm) as each joint moves them; prints
the frames and the parts hit, and a total per clip.
"""

import argparse
import importlib
import os
import sys

import bpy

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "mobs"))
import murim_biped  # noqa: E402
import murim_clips  # noqa: E402
import murim_items  # noqa: E402
import murim_mob  # noqa: E402


def hits(armature, item):
    return murim_items.body_hits(armature, item in ("axe", "tool"))


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--mob", required=True)
    parser.add_argument("--clips", required=True)
    args = parser.parse_args(argv)
    mob = importlib.import_module(args.mob)
    parts = mob.parts()
    height = murim_mob.pack(parts, mob.TEXTURE_WIDTH)
    murim_biped.clear_scene()
    armature = murim_mob.build(parts, mob.TEXTURE_WIDTH, height, args.mob)
    for clip in args.clips.split(","):
        keys = murim_clips.SETS[args.mob][clip]
        murim_clips.make_action(armature, clip, keys)
        item = clip.split("_")[0]
        bad = []
        for frame in range(keys[-1][0] + 1):
            bpy.context.scene.frame_set(frame)
            found = hits(armature, item)
            if found:
                bad.append(f"{frame}:{'+'.join(sorted(found))}")
        print("MURIM", clip, len(bad), " ".join(bad))


main()
