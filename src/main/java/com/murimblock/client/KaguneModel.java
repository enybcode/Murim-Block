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

    private void drawBone(int i, Animation animation, float t, float bendYaw, float bendPitch, PoseStack stack, VertexConsumer out, int light, int overlay) {
        var bone = bones.get(i);
        float[] l = local(i, animation, t);
        // Shrunk out of sight (a hidden form): nothing under it is drawn.
        if (Math.abs(l[7]) < 0.02F && Math.abs(l[8]) < 0.02F) return;
        stack.pushPose();
        apply(l, stack);
        if (chainSide[i] != 0 && (bendYaw != 0 || bendPitch != 0)) {
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(bendYaw * chainSide[i]));
            stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(bendPitch));
        }
        for (var cube : bone.cubes()) drawCube(cube, stack, out, light, overlay);
        for (int child : bone.children()) drawBone(child, animation, t, bendYaw, bendPitch, stack, out, light, overlay);
        stack.popPose();
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
