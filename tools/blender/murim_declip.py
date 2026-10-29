"""Keeps arms out of the body: tests an arm's parts against the body's as the joints move them, and moves the shoulder
the least it takes to clear them, frame by frame (export_clips.py --declip, limb_check.py).

A body is a mob's parts (tools/blender/mobs): the arm's parts (bound to Arm_*, Hand_*, or a limb whose upper joint is an
arm) are filled with points; the body's parts (head, chest, belly, the trunk, thighs and shins) are boxes, tested a
little inside their faces. Pairs that already meet at rest (a sleeve over the shoulder) are left out.
"""

import bpy
from mathutils import Vector

import murim_biped

BODY = {"Head", "Chest", "Torso", "Thigh_L", "Thigh_R", "Leg_L", "Leg_R"}
MARGIN = 0.75
# Points this close to a shoulder's pivot (pixels) fold into the chest with the joint itself: left out.
SHOULDER = 4.0
STEPS = 3
# The shoulder's moves tried, in degrees (out from the body, forward), the smallest first.
TRIES = sorted(((out, fwd, bend) for out in (0, 4, 8, 12, 16, 20, 25, 30) for fwd in (0, -6, -12, -18, 6)
                for bend in (0, 8, -8, 16, -16, 24) if out or fwd or bend),
               key=lambda t: t[0] + abs(t[1]) * 0.8 + abs(t[2]) * 0.6)
# Frames on each side a correction eases in and out over, so the arm does not jump.
EASE = 2


def arm_joint(joint):
    return joint.startswith(("Arm_", "Hand_", "Tool_", "Elbow_"))


def split(part):
    """The part as rigid boxes: (joint, low corner, high corner) in the model's rest space (pixels)."""
    (x, y, z), (w, h, d) = part.corner, part.size
    low, high = (x, y, z), (x + w, y + h, z + d)
    if isinstance(part.bind, tuple):
        _, upper, lower, _, hinge, _ = part.bind
        boxes = []
        if high[1] > hinge:
            boxes.append((upper, (x, max(y, hinge), z), high))
        if y < hinge:
            boxes.append((lower, low, (x + w, min(y + h, hinge), z + d)))
        return boxes
    if part.bind == "trunk":
        # The trunk's weights move from the belly to the chest about y 18.
        boxes = []
        if high[1] > 18:
            boxes.append(("Chest", (x, max(y, 18), z), high))
        if y < 18:
            boxes.append(("Torso", low, (x + w, min(y + h, 18), z + d)))
        return boxes
    return [(part.bind, low, high)]


def lattice(low, high):
    """Points filling a box, a little inside its faces."""
    points = []
    span = [max(hi - lo - 2 * 0.3, 0) for lo, hi in zip(low, high)]
    counts = [STEPS, max(STEPS, int(span[1] / 2) + 1), STEPS]
    for i in range(counts[0]):
        for j in range(counts[1]):
            for k in range(counts[2]):
                points.append(tuple(lo + 0.3 + s * (n / (c - 1) if c > 1 else 0.5)
                                    for lo, s, n, c in zip(low, span, (i, j, k), counts)))
    return points


class Body:
    """One mob's arms (points by joint, with their side) and body boxes."""

    def __init__(self, name, parts):
        self.name, self.arms, self.boxes, self.rest = name, [], [], set()
        for part in parts:
            if part.cloth:
                continue
            for joint, low, high in split(part):
                if arm_joint(joint) or isinstance(part.bind, tuple) and part.bind[1].startswith("Arm_"):
                    self.arms.append((part.name, joint, joint[-1], [Vector(murim_biped.blender(*p))
                                                                    for p in lattice(low, high)]))
                elif joint in BODY:
                    self.boxes.append((part.name, joint, low, high))

    def meets(self, pose, sides="LR"):
        """The (arm part, body part) pairs that meet in the current pose, but those that meet at rest."""
        found = set()
        moved = {}
        for name, joint, side, points in self.arms:
            if side in sides:
                bone = pose[joint]
                m = bone.matrix @ bone.bone.matrix_local.inverted()
                moved[name, joint] = [m @ p for p in points]
        for bname, bjoint, low, high in self.boxes:
            bone = pose[bjoint]
            back = bone.bone.matrix_local @ bone.matrix.inverted()
            for (aname, _), points in moved.items():
                if (aname, bname) in found:
                    continue
                for p in points:
                    r = back @ p
                    if all(lo + MARGIN < v < hi - MARGIN for v, lo, hi in zip((r.x, r.z, -r.y), low, high)):
                        found.add((aname, bname))
                        break
        return found - self.rest


def bodies(names):
    """The bodies of these mobs (tools/blender/mobs), all on the skeleton the clips are keyed on."""
    import importlib
    return [Body(name, importlib.import_module(name).parts()) for name in names]


def settle_rest(armature, found_bodies):
    """Notes the pairs that meet at rest (the pose cleared), to leave them out. Points right at a shoulder are
    left out too: the joint itself always folds a little into the chest.
    """
    pose = armature.pose.bones
    for body in found_bodies:
        for i, (name, joint, side, points) in enumerate(body.arms):
            if joint.startswith("Arm_"):
                pivot = pose[joint].bone.head_local
                body.arms[i] = (name, joint, side, [p for p in points if (p - pivot).length > SHOULDER])
    for bone in pose:
        bone.rotation_mode = "QUATERNION"
        bone.rotation_quaternion = (1, 0, 0, 0)
        bone.location = (0, 0, 0)
    bpy.context.view_layer.update()
    for body in found_bodies:
        body.rest = set()
        body.rest = body.meets(pose)


def hits(armature, found_bodies, side):
    pose = armature.pose.bones
    return sum(len(body.meets(pose, side)) for body in found_bodies)


def declip(armature, last, found_bodies, sides="LR"):
    """
    Moves each shoulder of the current action out (or forward, or back) and bends its elbow the least it takes for
    its arm to clear the body on every frame (the fewest parts met where it cannot clear them all), eased over the
    frames around; keys the shoulder and elbow on every frame. Returns {side: (frames corrected, frames still meeting)}.
    """
    import murim_clips
    scene = bpy.context.scene
    pose = armature.pose.bones
    report = {}
    for side in sides:
        bone, hand = pose[f"Arm_{side}"], pose[f"Hand_{side}"]
        sign = 1 if side == "L" else -1
        original, bent, needed, left = {}, {}, {}, 0
        for frame in range(last + 1):
            scene.frame_set(frame)
            original[frame] = bone.rotation_quaternion.copy()
            bent[frame] = hand.rotation_quaternion.copy()
            first = hits(armature, found_bodies, side)
            if not first:
                continue
            best, fewest = (0, 0, 0), first
            for out, fwd, bend in TRIES:
                bone.rotation_quaternion = murim_clips.rotation((fwd, 0, sign * out)) @ original[frame]
                hand.rotation_quaternion = murim_clips.rotation((bend, 0, 0)) @ bent[frame]
                bpy.context.view_layer.update()
                count = hits(armature, found_bodies, side)
                if count < fewest:
                    best, fewest = (out, fwd, bend), count
                    if not count:
                        break
            if fewest:
                left += 1
            if best != (0, 0, 0):
                needed[frame] = best
        if not needed:
            report[side] = (0, left)
            continue
        # Each frame takes the largest correction around it, eased by distance, so the arm glides out and back.
        moves = {}
        for frame in range(last + 1):
            out = fwd = bend = 0.0
            for other, (o, f, b) in needed.items():
                gap = abs(other - frame)
                if gap <= EASE:
                    weight = 1 - gap / (EASE + 1)
                    if o * weight > out:
                        out = o * weight
                    if abs(f) * weight > abs(fwd):
                        fwd = f * weight
                    if abs(b) * weight > abs(bend):
                        bend = b * weight
            moves[frame] = (out, fwd, bend)
        for frame in range(last + 1):
            out, fwd, bend = moves[frame]
            bone.rotation_quaternion = murim_clips.rotation((fwd, 0, sign * out)) @ original[frame]
            bone.keyframe_insert("rotation_quaternion", frame=frame)
            hand.rotation_quaternion = murim_clips.rotation((bend, 0, 0)) @ bent[frame]
            hand.keyframe_insert("rotation_quaternion", frame=frame)
        after = 0
        for frame in range(last + 1):
            scene.frame_set(frame)
            after += 1 if hits(armature, found_bodies, side) else 0
        report[side] = (len(needed), after)
    return report


# The grip's moves tried for a two-handed hold, in degrees (forward, out), the smallest first.
# The left elbow bends the way it bent (None), or out from the body, out and down, or down (armature space).
POLES = (None, (1, 0, 0), (1, 0, -1), (0, 0, -1), (1, 1, 0))
GRIP_TRIES = sorted(((fwd, out, slide, pole) for fwd in (0, -6, -12, -18, -24, -32, 10) for out in (0, 6, 12, 20)
                     for slide in (0, 0.05, -0.05, 0.1) for pole in range(len(POLES)) if fwd or out or slide or pole),
                    key=lambda t: abs(t[0]) + t[1] + abs(t[2]) * 60 + t[3] * 3)


def declip_grip(armature, found_bodies, resolve):
    """
    For a two-handed hold, this frame: if an arm is in the body, turns the right shoulder (the haft with it) forward or
    out and slides the left hand along the haft the least it takes, the left hand put back on the haft by
    `resolve(slide, pole)` each time, the elbow bent out or down if that clears it; keeps the fewest parts met.
    """
    import murim_clips
    pose = armature.pose.bones
    first = hits(armature, found_bodies, "LR")
    if not first:
        return
    names = ("Arm_R", "Arm_L", "Hand_L")
    saved = {name: pose[name].rotation_quaternion.copy() for name in names}
    best, fewest = None, first
    for fwd, out, slide, pole in GRIP_TRIES:
        for name in names:
            pose[name].rotation_quaternion = saved[name]
        pose["Arm_R"].rotation_quaternion = murim_clips.rotation((fwd, 0, -out)) @ saved["Arm_R"]
        bpy.context.view_layer.update()
        resolve(slide, POLES[pole])
        count = hits(armature, found_bodies, "LR")
        if count < fewest:
            best, fewest = {name: pose[name].rotation_quaternion.copy() for name in names}, count
            if not count:
                break
    for name in names:
        pose[name].rotation_quaternion = (best or saved)[name]
    bpy.context.view_layer.update()
