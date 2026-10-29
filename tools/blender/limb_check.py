"""Finds the frames where an arm (or the item in the right hand) passes into the body, for every clip of a mob.

blender -b --factory-startup --python tools/blender/limb_check.py -- --mob disciple [--set disciple] [--clips a,b]
    [--declip disciple,sables]

Tests the mob's arms against its body as murim_declip does (pairs that meet at rest, and the shoulder's own fold, left
out); armed clips also test the held item (murim_items.body_hits). With --declip, the clips are first corrected as
export_clips.py --declip corrects them, for those bodies. Prints, per clip, the frames and the parts that meet.
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
import murim_declip  # noqa: E402
import murim_items  # noqa: E402
import murim_mob  # noqa: E402


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--mob", required=True)
    parser.add_argument("--set", help="a clip set of murim_clips (default: the mob's own CLIPS)")
    parser.add_argument("--clips")
    parser.add_argument("--declip", help="correct the clips first, for these bodies, as a,b")
    args = parser.parse_args(argv)
    mob = importlib.import_module(args.mob)
    parts = mob.parts()
    height = murim_mob.pack(parts, mob.TEXTURE_WIDTH)
    murim_biped.clear_scene()
    armature = murim_mob.build(parts, mob.TEXTURE_WIDTH, height, args.mob, joints=getattr(mob, "JOINTS", None),
                               helpers=getattr(mob, "HELPERS", None))
    clips = murim_clips.SETS[args.set] if args.set else mob.CLIPS
    body = murim_declip.Body(args.mob, parts)
    murim_declip.settle_rest(armature, [body])
    if args.declip:
        murim_clips.DECLIP = murim_declip.bodies(args.declip.split(","))
        murim_declip.settle_rest(armature, murim_clips.DECLIP)
    names = args.clips.split(",") if args.clips else list(clips)
    total = 0
    for clip in names:
        keys = clips[clip]
        murim_clips.make_action(armature, clip, keys)
        item = murim_clips.ARMED.get(clip) if args.set == "disciple" else None
        bad = []
        for frame in range(keys[-1][0] + 1):
            bpy.context.scene.frame_set(frame)
            found = {f"{a}>{b}" for a, b in body.meets(armature.pose.bones)}
            if item:
                found |= {f"{item}>{b}" for b in murim_items.body_hits(armature, item in ("axe", "tool"))}
            if found:
                bad.append(f"{frame}:{'+'.join(sorted(found))}")
        total += len(bad)
        print("MURIM", clip, len(bad), " ".join(bad[:12]) + (" ..." if len(bad) > 12 else ""))
    print("MURIM total", args.mob, total, "frames over", len(names), "clips")


main()
