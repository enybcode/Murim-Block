"""The Murimblock biped for Blender: skeleton, skinned body and outer layer, both arm widths.

Shared by the build, export and render scripts. Must match com.murimblock.anim.Biped (the mod checks joint
names and parents when it loads the exported file). The joint layout follows Epic Fight's biped (same 20 joints,
names and parents); the mesh is ours, built here.

Model space (the mod's): pixels, Y up, facing +Z, left side on +X. Blender is Z up and the body faces -Y, so a
model point (x, y, z) is the Blender point (x, -z, y).

Every bone points straight up with no roll, so in the exported glTF each joint's rest rotation is the identity
and poses written in the mod (rotate the forearm about X...) mean the same thing as in Blender.

Limbs are rigid: the upper part follows Arm or Thigh, the lower part Hand or Leg. At the elbow and the knee a
hinge (flat at rest) joins them: its outer edge follows the Elbow or Knee helper joint, which turns half as far as
the forearm or shin, so the outer corner of a bent limb is filled with the texture row at the joint.
"""

import bmesh
import bpy

SKIN_SIZE = 64

# name, parent, model position (x, y, z) at rest. Parents before children.
JOINTS = [
    ("Root", None, (0, 12, 0)),
    ("Torso", "Root", (0, 13, 0)),
    ("Chest", "Torso", (0, 18, 0)),
    ("Head", "Chest", (0, 24, 0)),
]
for _side, _x in (("L", 1), ("R", -1)):
    JOINTS += [
        (f"Thigh_{_side}", "Root", (2 * _x, 12, 0)),
        (f"Leg_{_side}", f"Thigh_{_side}", (2 * _x, 6, 0)),
        (f"Knee_{_side}", f"Thigh_{_side}", (2 * _x, 6, 0)),
        (f"Shoulder_{_side}", "Chest", (0, 24, 0)),
        # Vanilla's arm pivot; Epic Fight puts it at the arm's middle (x ±6).
        (f"Arm_{_side}", f"Shoulder_{_side}", (5 * _x, 22, 0)),
        (f"Hand_{_side}", f"Arm_{_side}", (6 * _x, 18, 0)),
        (f"Tool_{_side}", f"Hand_{_side}", (6 * _x, 13, 0)),
        (f"Elbow_{_side}", f"Arm_{_side}", (6 * _x, 18, 0)),
    ]

# Helper joint, the joint it follows at half the angle.
HELPERS = [("Elbow_L", "Hand_L"), ("Elbow_R", "Hand_R"), ("Knee_L", "Leg_L"), ("Knee_R", "Leg_R")]


def blender(x, y, z):
    """A model point as a Blender point."""
    return (x, -z, y)


def trunk_weights(y):
    """The body twists smoothly between the torso and the chest."""
    if y <= 12:
        return {"Torso": 1.0}
    if y >= 24:
        return {"Chest": 1.0}
    chest = {14: 0.25, 16: 0.25, 18: 0.5, 20: 0.75, 22: 0.75}[y]
    return {"Torso": 1 - chest, "Chest": chest}


def parts(slim):
    """Every box of the player. Limbs: hinge height, upper, lower and helper joints, the side the hinge opens on."""
    arm_w = 3 if slim else 4
    arm_y = 11.5 if slim else 12
    head = {"kind": "box", "levels": [], "weights": lambda y: {"Head": 1.0}}
    trunk = {"kind": "box", "levels": [14, 16, 18, 20, 22], "weights": trunk_weights}

    def arm(side):
        return {"kind": "limb", "hinge": 18, "upper": f"Arm_{side}", "lower": f"Hand_{side}",
                "helper": f"Elbow_{side}", "opens": "back"}

    def leg(side):
        return {"kind": "limb", "hinge": 6, "upper": f"Thigh_{side}", "lower": f"Leg_{side}",
                "helper": f"Knee_{side}", "opens": "front"}

    boxes = [
        # name, outer layer, model min corner, size, skin offset, inflation, shape
        ("head", False, (-4, 24, -4), (8, 8, 8), (0, 0), 0, head),
        ("body", False, (-4, 12, -2), (8, 12, 4), (16, 16), 0, trunk),
        ("arm_r", False, (-4 - arm_w, arm_y, -2), (arm_w, 12, 4), (40, 16), 0, arm("R")),
        ("arm_l", False, (4, arm_y, -2), (arm_w, 12, 4), (32, 48), 0, arm("L")),
        # The vanilla leg pivots sit at x ±1.9, so the legs overlap by 0.2 in the middle.
        ("leg_r", False, (-3.9, 0, -2), (4, 12, 4), (0, 16), 0, leg("R")),
        ("leg_l", False, (-0.1, 0, -2), (4, 12, 4), (16, 48), 0, leg("L")),
        ("hat", True, (-4, 24, -4), (8, 8, 8), (32, 0), 0.5, head),
        ("jacket", True, (-4, 12, -2), (8, 12, 4), (16, 32), 0.25, trunk),
        ("sleeve_r", True, (-4 - arm_w, arm_y, -2), (arm_w, 12, 4), (40, 32), 0.25, arm("R")),
        ("sleeve_l", True, (4, arm_y, -2), (arm_w, 12, 4), (48, 48), 0.25, arm("L")),
        ("pants_r", True, (-3.9, 0, -2), (4, 12, 4), (0, 32), 0.25, leg("R")),
        ("pants_l", True, (-0.1, 0, -2), (4, 12, 4), (0, 48), 0.25, leg("L")),
    ]
    return boxes


class BoxBuilder:
    """Writes the faces of one skin box into a mesh, UV mapped like a vanilla cube, with a weight per vertex."""

    def __init__(self, name, corner, size, tex, inflate):
        self.x0, self.y0, self.z0 = corner
        self.w, self.h, self.d = size
        self.u, self.v = tex
        g = inflate
        self.X0, self.X1 = self.x0 - g, self.x0 + self.w + g
        self.Z0, self.Z1 = self.z0 - g, self.z0 + self.d + g
        self.Ybot, self.Ytop = self.y0 - g, self.y0 + self.h + g
        self.mesh = bpy.data.meshes.new(name)
        self.obj = bpy.data.objects.new(name, self.mesh)
        self.groups = {joint: self.obj.vertex_groups.new(name=joint).index for joint, *_ in JOINTS}
        self.bm = bmesh.new()
        self.uv_layer = self.bm.loops.layers.uv.new("UVMap")
        self.deform = self.bm.verts.layers.deform.verify()
        # The normal each face should have; zero-area hinge faces cannot work theirs out.
        self.normals = []

    def place_y(self, y):
        # Inflation pushes the end caps out; inner rings stay where the joint is.
        if y == self.y0:
            return self.Ybot
        if y == self.y0 + self.h:
            return self.Ytop
        return y

    def v_of(self, y):
        """Down the side faces: the top of the box is at v + d, the bottom at v + d + h."""
        return self.v + self.d + (self.y0 + self.h - y)

    # Where each face's texture sits, as functions of the model coordinate running across the face.
    def u_front(self, x):
        return self.u + self.d + (x - self.X0) / (self.X1 - self.X0) * self.w

    def u_back(self, x):
        return self.u + 2 * self.d + self.w + (self.X1 - x) / (self.X1 - self.X0) * self.w

    def u_right(self, z):
        return self.u + (z - self.Z0) / (self.Z1 - self.Z0) * self.d

    def u_left(self, z):
        return self.u + self.d + self.w + (self.Z1 - z) / (self.Z1 - self.Z0) * self.d

    def face(self, corners, normal):
        """corners: (model point, uv in skin pixels, {joint: weight}), counter-clockwise seen from outside."""
        verts = []
        for point, _uv, weights in corners:
            vert = self.bm.verts.new(blender(*point))
            for joint, weight in weights.items():
                vert[self.deform][self.groups[joint]] = weight
            verts.append(vert)
        face = self.bm.faces.new(verts)
        for loop, (_point, (pu, pv), _weights) in zip(face.loops, corners):
            loop[self.uv_layer].uv = (pu / SKIN_SIZE, 1 - pv / SKIN_SIZE)
        self.normals.append(blender(*normal))

    def sides(self, lo, hi, weights_lo, weights_hi):
        """The four side faces between two heights."""
        X0, X1, Z0, Z1 = self.X0, self.X1, self.Z0, self.Z1
        ylo, yhi, vlo, vhi = self.place_y(lo), self.place_y(hi), self.v_of(lo), self.v_of(hi)
        lo_w, hi_w = weights_lo, weights_hi
        self.face([((X0, ylo, Z1), (self.u_front(X0), vlo), lo_w), ((X1, ylo, Z1), (self.u_front(X1), vlo), lo_w),
                   ((X1, yhi, Z1), (self.u_front(X1), vhi), hi_w), ((X0, yhi, Z1), (self.u_front(X0), vhi), hi_w)],
                  (0, 0, 1))
        self.face([((X1, ylo, Z0), (self.u_back(X1), vlo), lo_w), ((X0, ylo, Z0), (self.u_back(X0), vlo), lo_w),
                   ((X0, yhi, Z0), (self.u_back(X0), vhi), hi_w), ((X1, yhi, Z0), (self.u_back(X1), vhi), hi_w)],
                  (0, 0, -1))
        self.face([((X0, ylo, Z0), (self.u_right(Z0), vlo), lo_w), ((X0, ylo, Z1), (self.u_right(Z1), vlo), lo_w),
                   ((X0, yhi, Z1), (self.u_right(Z1), vhi), hi_w), ((X0, yhi, Z0), (self.u_right(Z0), vhi), hi_w)],
                  (-1, 0, 0))
        self.face([((X1, ylo, Z1), (self.u_left(Z1), vlo), lo_w), ((X1, ylo, Z0), (self.u_left(Z0), vlo), lo_w),
                   ((X1, yhi, Z0), (self.u_left(Z0), vhi), hi_w), ((X1, yhi, Z1), (self.u_left(Z1), vhi), hi_w)],
                  (1, 0, 0))

    def top(self, weights):
        X0, X1, Z0, Z1, Y, u, v, w, d = self.X0, self.X1, self.Z0, self.Z1, self.Ytop, self.u, self.v, self.w, self.d
        # u from -X to +X, v from the back (v) to the front (v + d).
        self.face([((X0, Y, Z1), (u + d, v + d), weights), ((X1, Y, Z1), (u + d + w, v + d), weights),
                   ((X1, Y, Z0), (u + d + w, v), weights), ((X0, Y, Z0), (u + d, v), weights)], (0, 1, 0))

    def bottom(self, weights):
        X0, X1, Z0, Z1, Y, u, v, w, d = self.X0, self.X1, self.Z0, self.Z1, self.Ybot, self.u, self.v, self.w, self.d
        # u from -X to +X, v from the front (v) to the back (v + d). To check against vanilla in step 3.
        self.face([((X0, Y, Z0), (u + d + w, v + d), weights), ((X1, Y, Z0), (u + d + 2 * w, v + d), weights),
                   ((X1, Y, Z1), (u + d + 2 * w, v), weights), ((X0, Y, Z1), (u + d + w, v), weights)], (0, -1, 0))

    def hinge(self, y, upper, lower, helper, opens):
        """
        Flat at rest at height y: on the face the joint opens on (back for an elbow, front for a knee), a strip
        from the upper part's edge to the helper's edge to the lower part's edge; on each side, two triangles
        from the joint's axis to the same three points. All textured with the texel row just below the joint.
        """
        X0, X1 = self.X0, self.X1
        up, mid, low = {upper: 1.0}, {helper: 1.0}, {lower: 1.0}
        row = self.v_of(y) + 0.5
        axis_z = self.z0 + self.d / 2
        if opens == "back":
            z, u_of, normal = self.Z0, self.u_back, (0, 0, -1)
            # Bottom to top, counter-clockwise from behind: +X then -X.
            strip = [(X1, low), (X0, low), (X0, mid), (X1, mid)], [(X1, mid), (X0, mid), (X0, up), (X1, up)]
        else:
            z, u_of, normal = self.Z1, self.u_front, (0, 0, 1)
            strip = [(X0, low), (X1, low), (X1, mid), (X0, mid)], [(X0, mid), (X1, mid), (X1, up), (X0, up)]
        for quad in strip:
            self.face([((x, y, z), (u_of(x), row), weights) for x, weights in quad], normal)
        for x, u_side, side_normal in ((X0, self.u_right, (-1, 0, 0)), (X1, self.u_left, (1, 0, 0))):
            center = ((x, y, axis_z), (u_side(axis_z), row), up)
            outer = [((x, y, z), (u_side(z), row), weights) for weights in (up, mid, low)]
            # Seen from that side: counter-clockwise when the hinge opens behind on the -X side, else reversed.
            forward = (opens == "back") == (x == X0)
            for a, b in ((outer[0], outer[1]), (outer[1], outer[2])):
                self.face([center, a, b] if forward else [center, b, a], side_normal)

    def finish(self, armature):
        self.bm.to_mesh(self.mesh)
        self.bm.free()
        loop_normals = []
        for poly, normal in zip(self.mesh.polygons, self.normals):
            poly.use_smooth = False
            loop_normals += [normal] * poly.loop_total
        if hasattr(self.mesh, "normals_split_custom_set"):
            self.mesh.normals_split_custom_set(loop_normals)
        self.obj.parent = armature
        modifier = self.obj.modifiers.new("Armature", "ARMATURE")
        modifier.object = armature
        return self.obj


def build_part(armature, name, corner, size, tex, inflate, shape):
    box = BoxBuilder(name, corner, size, tex, inflate)
    y0, h = corner[1], size[1]
    if shape["kind"] == "box":
        weights = shape["weights"]
        levels = sorted({y0, y0 + h, *shape["levels"]})
        for lo, hi in zip(levels, levels[1:]):
            box.sides(lo, hi, weights(lo), weights(hi))
        box.top(weights(y0 + h))
        box.bottom(weights(y0))
    else:
        hinge, upper, lower = shape["hinge"], {shape["upper"]: 1.0}, {shape["lower"]: 1.0}
        # Two rigid blocks open at the joint, then the hinge between them.
        box.sides(hinge, y0 + h, upper, upper)
        box.top(upper)
        box.sides(y0, hinge, lower, lower)
        box.bottom(lower)
        box.hinge(hinge, shape["upper"], shape["lower"], shape["helper"], shape["opens"])
    return box.finish(armature)


def build(slim=False, collection=None):
    """Builds the armature and the 12 skin boxes in the current scene; returns the armature object."""
    collection = collection or bpy.context.scene.collection
    data = bpy.data.armatures.new("biped_slim" if slim else "biped")
    armature = bpy.data.objects.new(data.name, data)
    collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    bpy.ops.object.mode_set(mode="EDIT")
    bones = {}
    for name, parent, position in JOINTS:
        bone = data.edit_bones.new(name)
        bone.head = blender(*position)
        # Straight up, no roll: an identity rest rotation in glTF.
        bone.tail = blender(position[0], position[1] + 2, position[2])
        bone.roll = 0
        if parent:
            bone.parent = bones[parent]
            bone.use_connect = False
        bones[name] = bone
    bpy.ops.object.mode_set(mode="OBJECT")
    # In Blender too the helpers follow at half the angle, so what Enzo animates is what the mod draws.
    for helper, follows in HELPERS:
        constraint = armature.pose.bones[helper].constraints.new("COPY_ROTATION")
        constraint.target = armature
        constraint.subtarget = follows
        constraint.owner_space = "LOCAL"
        constraint.target_space = "LOCAL"
        constraint.influence = 0.5
    for name, outer, corner, size, tex, inflate, shape in parts(slim):
        obj = build_part(armature, name, corner, size, tex, inflate, shape)
        obj["outer"] = outer
        collection.objects.link(obj)
    return armature


def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
