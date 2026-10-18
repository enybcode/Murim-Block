package com.murimblock.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The kagune's two forms as exported from Blockbench (scripts/animation/kagune-export.cjs, kagune/kagune_forms.json):
 * its bones (local position, rotation, scale), its cubes and three animations sampled 20 times a second. Draws a bone's
 * subtree at a time, posed at a moment of one animation (lerped between samples), in Blockbench's units (pixels, the
 * player's feet at the origin, the face toward -z). Loaded once, on first use.
 */
public final class KaguneModel {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "kagune/kagune_forms.json");
    private static KaguneModel loaded;

    record Bone(String name, int parent, float[] pos, float[] rot, float[] scale, List<Integer> children, List<Cube> cubes) { }
    record Cube(float[] pos, float[] rot, float[] from, float[] to, Map<String, float[]> faces) { }
    record Animation(float length, boolean loop, int fps, Map<Integer, float[][]> tracks) { }

    final List<Bone> bones = new ArrayList<>();
    /** Which bones are joints of a tentacle or a centipede (they bend with the tail's physics), and on which side (+1 right, -1 left). */
    private int[] chainSide = new int[0];
    final Map<String, Animation> animations = new HashMap<>();
    private final Map<String, Integer> byName = new HashMap<>();

    public static KaguneModel get() {
        if (loaded == null) loaded = load();
        return loaded;
    }

    private static KaguneModel load() {
        var model = new KaguneModel();
        try (var in = Minecraft.getInstance().getResourceManager().open(FILE)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : json.getAsJsonArray("bones")) {
                var b = e.getAsJsonObject();
                model.byName.put(b.get("name").getAsString(), model.bones.size());
                model.bones.add(new Bone(b.get("name").getAsString(), b.get("parent").getAsInt(), floats(b.getAsJsonArray("pos")),
                        floats(b.getAsJsonArray("rot")), floats(b.getAsJsonArray("scale")), new ArrayList<>(), new ArrayList<>()));
            }
            model.chainSide = new int[model.bones.size()];
            var chain = java.util.regex.Pattern.compile("^(upper|lower|centipede)_(left|right)_\\d+$");
            for (int i = 0; i < model.bones.size(); i++) {
                var m = chain.matcher(model.bones.get(i).name());
                if (m.matches()) model.chainSide[i] = m.group(2).equals("left") ? -1 : 1;
            }
            for (int i = 0; i < model.bones.size(); i++) {
                int parent = model.bones.get(i).parent();
                if (parent >= 0) model.bones.get(parent).children().add(i);
            }
            for (var e : json.getAsJsonArray("cubes")) {
                var c = e.getAsJsonObject();
                int bone = c.get("bone").getAsInt();
                if (bone < 0) continue;
                Map<String, float[]> faces = new HashMap<>();
                for (var f : c.getAsJsonObject("faces").entrySet()) faces.put(f.getKey(), floats(f.getValue().getAsJsonArray()));
                model.bones.get(bone).cubes().add(new Cube(floats(c.getAsJsonArray("pos")), floats(c.getAsJsonArray("rot")),
                        floats(c.getAsJsonArray("from")), floats(c.getAsJsonArray("to")), faces));
            }
            for (var e : json.getAsJsonObject("animations").entrySet()) {
                var a = e.getValue().getAsJsonObject();
                Map<Integer, float[][]> tracks = new HashMap<>();
                for (var t : a.getAsJsonObject("tracks").entrySet()) {
                    JsonArray frames = t.getValue().getAsJsonArray();
                    float[][] samples = new float[frames.size()][];
                    for (int i = 0; i < samples.length; i++) samples[i] = floats(frames.get(i).getAsJsonArray());
                    tracks.put(Integer.parseInt(t.getKey()), samples);
                }
                model.animations.put(e.getKey(), new Animation(a.get("length").getAsFloat(), a.get("loop").getAsBoolean(), a.get("fps").getAsInt(), tracks));
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Could not load the kagune model {}", FILE, ex);
        }
        return model;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
        return out;
    }

    public int bone(String name) {
        return byName.getOrDefault(name, -1);
    }

    public float length(String animation) {
        var a = animations.get(animation);
        return a == null ? 0 : a.length();
    }

    /** Bone i's local transform at time t (s) of the animation: pos (3), rotation (4, x y z w), scale (3). */
    private float[] local(int i, Animation animation, float t) {
        var bone = bones.get(i);
        float[][] track = animation == null ? null : animation.tracks().get(i);
        if (track == null) return concat(bone.pos(), bone.rot(), bone.scale());
        float clock = animation.loop() ? t % animation.length() : Math.min(t, animation.length());
        float frame = clock * animation.fps();
        int a = Math.min((int) frame, track.length - 1), b = Math.min(a + 1, track.length - 1);
        float f = frame - (int) frame;
        float[] x = track[a], y = track[b], out = new float[10];
        for (int k = 0; k < 10; k++) out[k] = x[k] + (y[k] - x[k]) * f;
        // The rotation, as a proper unit quaternion between the two samples.
        var q = new Quaternionf(x[3], x[4], x[5], x[6]).slerp(new Quaternionf(y[3], y[4], y[5], y[6]), f);
        out[3] = q.x; out[4] = q.y; out[5] = q.z; out[6] = q.w;
        return out;
    }

    private static float[] concat(float[] p, float[] r, float[] s) {
        return new float[]{p[0], p[1], p[2], r[0], r[1], r[2], r[3], s[0], s[1], s[2]};
    }

    /** Draws bone i and everything under it, posed at time t of the animation, from its parent's frame. */
    public void draw(int i, String animation, float t, PoseStack stack, VertexConsumer out, int light, int overlay) {
        draw(i, animation, t, 0, 0, stack, out, light, overlay);
    }

    /** As above, every tentacle and centipede joint bent a little more (degrees per joint: yaw, pitch), as a tail drags. */
    public void draw(int i, String animation, float t, float bendYaw, float bendPitch, PoseStack stack, VertexConsumer out, int light, int overlay) {
        if (i < 0) return;
        drawBone(i, animations.get(animation), t, bendYaw, bendPitch, stack, out, light, overlay);
    }

    /** Applies the transforms of bone i's parents (from the root down), posed at time t, so bone i can be drawn there. */
    public void toParentOf(int i, String animation, float t, PoseStack stack) {
        var a = animations.get(animation);
        List<Integer> chain = new ArrayList<>();
        for (int p = bones.get(i).parent(); p >= 0; p = bones.get(p).parent()) chain.add(0, p);
        for (int p : chain) apply(local(p, a, t), stack);
    }

    /** As {@link #toParentOf}, with every parent at rest (for a bone carried by a joint of the player instead). */
    public void toParentAtRest(int i, PoseStack stack) {
        List<Integer> chain = new ArrayList<>();
        for (int p = bones.get(i).parent(); p >= 0; p = bones.get(p).parent()) chain.add(0, p);
        for (int p : chain) apply(local(p, null, 0), stack);
    }

    private static void apply(float[] l, PoseStack stack) {
        stack.translate(l[0], l[1], l[2]);
        stack.mulPose(new Quaternionf(l[3], l[4], l[5], l[6]));
        stack.scale(l[7], l[8], l[9]);
    }

    /** An extra turn (degrees about X) at the first joint of some chains, to lift them off the ground (keyed by bone). */
    private final Map<Integer, Float> lift = new HashMap<>();

    private void drawBone(int i, Animation animation, float t, float bendYaw, float bendPitch, PoseStack stack, VertexConsumer out, int light, int overlay) {
        var bone = bones.get(i);
        float[] l = posed(i, animation, t);
        // Shrunk out of sight (a hidden form): nothing under it is drawn.
        if (Math.abs(l[7]) < 0.02F && Math.abs(l[8]) < 0.02F) return;
        stack.pushPose();
        apply(l, stack);
        bendAt(i, bendYaw, bendPitch, stack.last().pose());
        float stretch = stretchOf(i);
        if (stretch != 1) { stack.pushPose(); stack.scale(1, 1, stretch); }
        for (var cube : bone.cubes()) drawCube(cube, stack, out, light, overlay);
        if (stretch != 1) stack.popPose();
        for (int child : bone.children()) drawBone(child, animation, t, bendYaw, bendPitch, stack, out, light, overlay);
        stack.popPose();
    }

    /** The tail's bend at a chain joint, and the lift at a chain's first joint. */
    private void bendAt(int i, float bendYaw, float bendPitch, org.joml.Matrix4f m) {
        if (chainSide[i] != 0 && (bendYaw != 0 || bendPitch != 0)) {
            m.rotate((float) Math.toRadians(bendYaw * chainSide[i]), 0, 1, 0);
            m.rotate((float) Math.toRadians(bendPitch), 1, 0, 0);
        }
        Float up = lift.get(i);
        if (up != null && up != 0) m.rotate((float) Math.toRadians(up), 1, 0, 0);
    }

    private static void apply(float[] l, org.joml.Matrix4f m) {
        m.translate(l[0], l[1], l[2]).rotate(new Quaternionf(l[3], l[4], l[5], l[6])).scale(l[7], l[8], l[9]);
    }

    /** The first joints of the tentacles and the centipedes (a chain joint whose parent is not one). */
    private List<Integer> chainRoots;

    List<Integer> chainRoots() {
        if (chainRoots == null) {
            chainRoots = new ArrayList<>();
            for (int i = 0; i < bones.size(); i++) {
                int p = bones.get(i).parent();
                if (chainSide[i] != 0 && (p < 0 || chainSide[p] == 0)) chainRoots.add(i);
            }
        }
        return chainRoots;
    }

    /** The lowest point (y, in the base's space) of bone i's joints and everything under it, posed and bent. */
    private float lowest(int i, Animation animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f parent) {
        float[] l = posed(i, animation, t);
        if (Math.abs(l[7]) < 0.02F && Math.abs(l[8]) < 0.02F) return Float.MAX_VALUE;
        var m = new org.joml.Matrix4f(parent);
        apply(l, m);
        bendAt(i, bendYaw, bendPitch, m);
        float low = m.m31();
        for (int child : bones.get(i).children()) low = Math.min(low, lowest(child, animation, t, bendYaw, bendPitch, m));
        return low;
    }

    /** Bone i's parent frame, from bone from's parent down (posed and bent), from a base matrix. */
    private org.joml.Matrix4f frameAbove(int from, int i, Animation animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f base) {
        List<Integer> chain = new ArrayList<>();
        int stop = bones.get(from).parent();
        for (int p = bones.get(i).parent(); p >= 0 && p != stop; p = bones.get(p).parent()) chain.add(0, p);
        var m = new org.joml.Matrix4f(base);
        for (int p : chain) { apply(posed(p, animation, t), m); bendAt(p, bendYaw, bendPitch, m); }
        return m;
    }

    /**
     * Keeps every chain under bone {@code from} above the ground (Enzo, 18/10: "the kagune must not go into the ground"):
     * with {@code base} mapping the model into a space where the ground is at y = 0 (the player's feet), each chain that
     * would dip under {@code clearance} is turned up at its first joint, by the least angle, either way, that lifts it
     * clear (or, failing that, the one that lifts it most).
     */
    public void keepAboveGround(int from, String animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f base, float clearance) {
        var a = animations.get(animation);
        for (int root : chainRoots()) {
            if (!under(root, from)) continue;
            lift.remove(root);
            var frame = frameAbove(from, root, a, t, bendYaw, bendPitch, base);
            if (lowest(root, a, t, bendYaw, bendPitch, frame) >= clearance) continue;
            float best = 0, bestLow = -Float.MAX_VALUE;
            search:
            for (float step = 3; step <= 90; step += 3) {
                for (float sign : new float[]{1, -1}) {
                    lift.put(root, step * sign);
                    float low = lowest(root, a, t, bendYaw, bendPitch, frame);
                    if (low >= clearance) { best = step * sign; break search; }
                    if (low > bestLow) { bestLow = low; best = step * sign; }
                }
            }
            lift.put(root, best);
        }
    }

    // ---------------------------------------------------------------- strikes (KaguneStrikes)

    /** A chain taken over by a strike: how much (0 to 1), its first joint's turn, and the wave along it (degrees, radians). */
    private record Aim(float weight, Quaternionf root, float curl, float phase, float arc, float stretch) { }
    private final Map<Integer, Aim> aims = new HashMap<>();
    /** For each chain joint, its chain's first joint and how far along it is (-1 off the chains). */
    private int[] chainRootOf, chainIndex;

    private void indexChains() {
        chainRootOf = new int[bones.size()];
        chainIndex = new int[bones.size()];
        java.util.Arrays.fill(chainRootOf, -1);
        for (int root : chainRoots()) {
            int k = 0;
            for (int i = root; i >= 0; ) {
                chainRootOf[i] = root;
                chainIndex[i] = k++;
                int next = -1;
                for (int c : bones.get(i).children()) if (chainSide[c] != 0) next = c;
                i = next;
            }
        }
    }

    /** No chain taken over (each frame starts so). */
    public void clearAims() {
        aims.clear();
    }

    /**
     * A strike takes over a chain: its first joint turned to point the chain along {@code dir} ({@code up} as its top),
     * both in the base's space (KaguneOverlay: the player's model space), by {@code weight} over the animation's own pose;
     * the joints along it bent by a wave of {@code curl} degrees, travelling with {@code phase}.
     */
    public void aim(int root, float weight, Vector3f dir, Vector3f up, float curl, float phase,
                    int from, String animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f base) {
        aim(root, weight, dir, up, curl, phase, 0, 1, from, animation, t, bendYaw, bendPitch, base);
    }

    /**
     * As above, every joint after the first also bent down by {@code arc} degrees (the chain arches over, a cobra striking),
     * and the chain drawn {@code stretch} times its length (its joints spread, its segments drawn longer): coiled under 1,
     * reaching out over it.
     */
    public void aim(int root, float weight, Vector3f dir, Vector3f up, float curl, float phase, float arc, float stretch,
                    int from, String animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f base) {
        if (root < 0 || weight <= 0) return;
        if (chainRootOf == null) indexChains();
        var frame = frameAbove(from, root, animations.get(animation), t, bendYaw, bendPitch, base);
        var inv = new org.joml.Matrix3f(frame).invert();
        Vector3f d = inv.transform(new Vector3f(dir)).normalize(), u = inv.transform(new Vector3f(up)).normalize();
        // The chain runs along its joints' +z: z = d, y = the top, x to make a frame.
        Vector3f x = new Vector3f(u).cross(d);
        if (x.lengthSquared() < 1e-6) x.set(1, 0, 0); else x.normalize();
        // The frames carry Blockbench's mirror (x by -1): there the cross product turns the top over; turned back.
        if (inv.determinant() < 0) x.negate();
        Vector3f y = new Vector3f(d).cross(x).normalize();
        var q = new Quaternionf().setFromNormalized(new org.joml.Matrix3f(x, y, d));
        aims.put(root, new Aim(Math.min(1, weight), q, curl, phase, arc, stretch));
    }

    /** How many joints a chain has (from its first), for a strike to spread an arc over it. */
    public int chainLength(int root) {
        if (root < 0) return 0;
        if (chainRootOf == null) indexChains();
        int n = 0;
        for (int i = 0; i < bones.size(); i++) if (chainRootOf[i] == root) n++;
        return n;
    }

    /** How many joints of the chain from {@code root} (past its first {@code skip}) are inside a box of the base's space (a check). */
    public int jointsInside(int from, int root, String animation, float t, float bendYaw, float bendPitch, org.joml.Matrix4f base,
                            float x0, float y0, float z0, float x1, float y1, float z1, int skip) {
        var a = animations.get(animation);
        var m = frameAbove(from, root, a, t, bendYaw, bendPitch, base);
        int n = 0, k = 0;
        for (int i = root; i >= 0; k++) {
            apply(posed(i, a, t), m);
            bendAt(i, bendYaw, bendPitch, m);
            Vector3f at = m.transformPosition(new Vector3f());
            if (k >= skip && at.x > x0 && at.x < x1 && at.y > y0 && at.y < y1 && at.z > z0 && at.z < z1) n++;
            int next = -1;
            for (int c : bones.get(i).children()) if (chainSide[c] != 0) next = c;
            i = next;
        }
        return n;
    }

    /** How much longer a chain joint's own segment is drawn now (1 off a strike). */
    private float stretchOf(int i) {
        if (aims.isEmpty() || chainRootOf == null || chainRootOf[i] < 0) return 1;
        Aim aim = aims.get(chainRootOf[i]);
        return aim == null ? 1 : 1 + (aim.stretch() - 1) * aim.weight();
    }

    /** Bone i's local transform now: the animation's, or a strike's where it takes the chain over. */
    private float[] posed(int i, Animation animation, float t) {
        float[] l = local(i, animation, t);
        if (aims.isEmpty() || chainRootOf == null || chainRootOf[i] < 0) return l;
        Aim aim = aims.get(chainRootOf[i]);
        if (aim == null) return l;
        Quaternionf target = i == chainRootOf[i] ? aim.root()
                : new Quaternionf().rotationX((float) Math.toRadians(aim.arc() + aim.curl() * Math.sin(aim.phase() - chainIndex[i] * 0.55)));
        var q = new Quaternionf(l[3], l[4], l[5], l[6]).slerp(target, aim.weight());
        l[3] = q.x; l[4] = q.y; l[5] = q.z; l[6] = q.w;
        if (i != chainRootOf[i]) l[2] *= 1 + (aim.stretch() - 1) * aim.weight();
        return l;
    }

    private boolean under(int bone, int from) {
        for (int p = bone; p >= 0; p = bones.get(p).parent()) if (p == from) return true;
        return false;
    }

    private static final float ATLAS = 64;

    private static void drawCube(Cube c, PoseStack stack, VertexConsumer out, int light, int overlay) {
        stack.pushPose();
        stack.translate(c.pos()[0], c.pos()[1], c.pos()[2]);
        stack.mulPose(new Quaternionf(c.rot()[0], c.rot()[1], c.rot()[2], c.rot()[3]));
        var pose = stack.last();
        float x0 = c.from()[0], y0 = c.from()[1], z0 = c.from()[2], x1 = c.to()[0], y1 = c.to()[1], z1 = c.to()[2];
        // Each face from outside, its corners as Blockbench lays its uv: top-left, top-right, bottom-right, bottom-left.
        face(c, "north", pose, out, light, overlay, 0, 0, -1, new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}});
        face(c, "south", pose, out, light, overlay, 0, 0, 1, new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}});
        face(c, "east", pose, out, light, overlay, 1, 0, 0, new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}});
        face(c, "west", pose, out, light, overlay, -1, 0, 0, new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}});
        face(c, "up", pose, out, light, overlay, 0, 1, 0, new float[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}});
        face(c, "down", pose, out, light, overlay, 0, -1, 0, new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}});
        stack.popPose();
    }

    private static void face(Cube c, String name, PoseStack.Pose pose, VertexConsumer out, int light, int overlay,
                             float nx, float ny, float nz, float[][] corners) {
        float[] uv = c.faces().get(name);
        if (uv == null) return;
        float u0 = uv[0] / ATLAS, v0 = uv[1] / ATLAS, u1 = uv[2] / ATLAS, v1 = uv[3] / ATLAS;
        float[][] uvs = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        var normal = pose.transformNormal(nx, ny, nz, new Vector3f());
        for (int k = 0; k < 4; k++) {
            out.addVertex(pose, corners[k][0], corners[k][1], corners[k][2]).setColor(-1).setUv(uvs[k][0], uvs[k][1])
                    .setOverlay(overlay).setLight(light).setNormal(normal.x, normal.y, normal.z);
        }
    }
}
