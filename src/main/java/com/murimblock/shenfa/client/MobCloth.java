package com.murimblock.shenfa.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.Cloth;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A mob look's cloth ({@link Cloth}): the pieces its model leaves to be simulated (a rope's loose ends, a robe's
 * panels), listed with where they hang and their faces in the look's texture in {@code skeletal/<model>.json}
 * (written by {@code tools/blender/mob.py}). Each hangs from a joint, swings with the body and stays off the body and
 * the legs, on the side it hangs on; each mob has its own, sized to it (a baby's are half as long).
 */
public final class MobCloth {
    /**
     * A piece: the joint it hangs from, the middle of its top edge (in the joint's space), its size, side, faces; how
     * low it reaches (model pixels) and how much further out than the innermost pieces it hangs.
     */
    private record Piece(String name, String joint, Vector3f pin, float width, float length, int side, int[] front,
                         int[] back, float lowest, float outward) {
    }

    private record Spec(int width, int height, List<Piece> pieces) {
    }

    private static final class Hanging {
        final Cloth cloth;
        long lastNanos;
        Vector3f lastPin;

        Hanging(Piece piece, float scale) {
            int columns = Math.max(2, Math.min(6, Math.round(piece.width) + 1));
            int rows = Math.max(3, Math.min(10, Math.round(piece.length) + 1));
            cloth = new Cloth(columns, rows, piece.width / 16 * scale, piece.length / 16 * scale);
        }
    }

    /** Clear of the legs and of what the model wears on them (a robe's skirts stand half a pixel out). */
    private static final float LEGS_LEAST = 3.0f, LEGS_MARGIN = 1.0f;
    /** How far out (pixels) the innermost pieces hang: those hung further out keep their lead over them. */
    private static final float INNERMOST = 2.6f;
    private static final Map<ResourceLocation, Optional<Spec>> SPECS = new HashMap<>();
    private static final Map<Mob, Map<String, Hanging>> HANGING = new WeakHashMap<>();

    private MobCloth() {
    }

    /** Forgets the specs (the resource packs were reloaded). */
    static void clear() {
        SPECS.clear();
    }

    /**
     * Simulates and draws {@code mob}'s cloth.
     *
     * @param model  the body's model space to view space (blocks; the camera-relative world)
     * @param joints the pose's joints in model space (pixels)
     * @param scale  the mob's size (a baby's is half)
     */
    static void render(Mob mob, SkeletalMobs.Look look, Matrix4f model, Matrix4f[] joints, float scale,
                       MultiBufferSource buffers, int light, int overlay) {
        // The look's skeleton: the biped's, or a stretched one of its own (the Moine du Vide).
        Armature armature = SkeletalMobs.armature(look);
        Spec spec = SPECS.computeIfAbsent(look.model(), key -> load(key, armature)).orElse(null);
        if (spec == null || spec.pieces.isEmpty()) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Matrix4f hips = joints[armature.get(Biped.ROOT).index()];
        Matrix4f torso = joints[armature.get(Biped.TORSO).index()];
        Map<String, Hanging> hanging = HANGING.computeIfAbsent(mob, key -> new HashMap<>());
        VertexConsumer consumer = buffers.getBuffer(SkeletalMobs.translucent(look)
                ? RenderType.entityTranslucent(look.texture()) : RenderType.entityCutoutNoCull(look.texture()));
        long now = System.nanoTime();
        for (Piece piece : spec.pieces) {
            Hanging state = hanging.computeIfAbsent(piece.name, key -> new Hanging(piece, scale));
            Matrix4f joint = joints[armature.get(piece.joint).index()];
            Cloth cloth = state.cloth;
            Vector3f[] top = new Vector3f[cloth.columns()];
            for (int c = 0; c < top.length; c++) {
                float x = piece.width / 2 - piece.width * c / (top.length - 1);
                top[c] = world(model, joint, new Vector3f(piece.pin).add(x, 0, 0), camera);
            }
            Vector3f down = world(model, joint, new Vector3f(piece.pin).add(0, -6, 0), camera).sub(world(model, joint,
                    piece.pin, camera));
            float seconds = state.lastNanos == 0 ? 0 : Math.min(0.1f, (now - state.lastNanos) / 1e9f);
            state.lastNanos = now;
            if (state.lastPin != null && state.lastPin.distance(top[0]) > 4) {
                cloth.reset();
            }
            state.lastPin = new Vector3f(top[0]);
            // Off the body (its front or back, a little out) and off the legs on that side.
            Cloth.Plane body = plane(model, torso, camera, piece.side * 2.3f, piece.side);
            // The pieces of a side share their legs' plane, reaching as low as the lowest of them; one hung further out
            // (a rope end over a panel) stays that much further out.
            float lowest = (float) spec.pieces.stream().filter(other -> other.side == piece.side)
                    .mapToDouble(Piece::lowest).min().orElse(piece.lowest);
            Cloth.Plane legs = plane(model, hips, camera, legsEdge(armature, joints, hips, piece.side, lowest)
                    + piece.side * piece.outward, piece.side);
            cloth.update(top, down, seconds, body, legs);
            draw(cloth, piece, spec, camera, consumer, light, overlay);
        }
    }

    /**
     * Where a plane before ({@code side} 1) or behind (-1) the legs stands (pixels, along the hips' z): past the parts
     * of the thighs a piece reaching down to {@code lowest} (model pixels) can touch, further when a leg swings that way.
     */
    private static float legsEdge(Armature armature, Matrix4f[] joints, Matrix4f hips, int side, float lowest) {
        Matrix4f toHips = new Matrix4f(hips).invert();
        float edge = LEGS_LEAST;
        for (String leg : new String[] {"L", "R"}) {
            Matrix4f thigh = joints[armature.get("Thigh_" + leg).index()];
            Matrix4f shin = joints[armature.get("Leg_" + leg).index()];
            // The thigh's middle (9 px up at rest) and the knee (6 px), for a piece hanging that low.
            if (lowest < 9) {
                Vector3f thighSide = thigh.transformPosition(new Vector3f(0, -3, 2 * side));
                edge = Math.max(edge, side * toHips.transformPosition(thighSide).z + LEGS_MARGIN);
            }
            if (lowest < 6) {
                Vector3f kneeSide = shin.transformPosition(new Vector3f(0, 0, 2 * side));
                edge = Math.max(edge, side * toHips.transformPosition(kneeSide).z + LEGS_MARGIN);
            }
        }
        return side * Math.min(edge, 8);
    }

    /** A plane at {@code z} along a joint's z, the cloth kept on its {@code side} (1: in front, -1: behind). */
    private static Cloth.Plane plane(Matrix4f model, Matrix4f joint, Vec3 camera, float z, int side) {
        Vector3f point = world(model, joint, new Vector3f(0, 0, z), camera);
        Vector3f normal = world(model, joint, new Vector3f(0, 0, z + side), camera).sub(point).normalize();
        return new Cloth.Plane(point, normal, 0.005f);
    }

    private static Vector3f world(Matrix4f model, Matrix4f joint, Vector3fc pixels, Vec3 camera) {
        Vector3f at = joint.transformPosition(new Vector3f(pixels)).mul(1 / 16f);
        model.transformPosition(at);
        return at.add((float) camera.x, (float) camera.y, (float) camera.z);
    }

    /**
     * Both faces of a piece: the one facing forward (+Z at rest) with the texture's front face, the other with its back
     * face. Columns run from the body's left (+X) to its right, as the top edge is laid out.
     */
    private static void draw(Cloth cloth, Piece piece, Spec spec, Vec3 camera, VertexConsumer consumer, int light,
                             int overlay) {
        PoseStack.Pose pose = new PoseStack().last();
        Vector3f normal = new Vector3f();
        int columns = cloth.columns(), rows = cloth.rows();
        for (int r = 0; r + 1 < rows; r++) {
            for (int c = 0; c + 1 < columns; c++) {
                Vector3f a = rel(cloth.point(c, r), camera), b = rel(cloth.point(c + 1, r), camera),
                        d = rel(cloth.point(c + 1, r + 1), camera), e = rel(cloth.point(c, r + 1), camera);
                // Columns run from the body's left, rows down: this side faces forward (the front face), then back.
                new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a), normal).normalize();
                vertex(consumer, pose, a, u(piece.front, spec, c, columns, true), v(piece.front, spec, r, rows), light,
                        overlay, normal);
                vertex(consumer, pose, e, u(piece.front, spec, c, columns, true), v(piece.front, spec, r + 1, rows), light,
                        overlay, normal);
                vertex(consumer, pose, d, u(piece.front, spec, c + 1, columns, true), v(piece.front, spec, r + 1, rows),
                        light, overlay, normal);
                vertex(consumer, pose, b, u(piece.front, spec, c + 1, columns, true), v(piece.front, spec, r, rows), light,
                        overlay, normal);
                normal.negate();
                vertex(consumer, pose, b, u(piece.back, spec, c + 1, columns, false), v(piece.back, spec, r, rows),
                        light, overlay, normal);
                vertex(consumer, pose, d, u(piece.back, spec, c + 1, columns, false), v(piece.back, spec, r + 1, rows),
                        light, overlay, normal);
                vertex(consumer, pose, e, u(piece.back, spec, c, columns, false), v(piece.back, spec, r + 1, rows),
                        light, overlay, normal);
                vertex(consumer, pose, a, u(piece.back, spec, c, columns, false), v(piece.back, spec, r, rows), light,
                        overlay, normal);
            }
        }
    }

    /** Across a face {u, v, width, height} (texels): the front face runs from the body's right, the back from its left. */
    private static float u(int[] face, Spec spec, int column, int columns, boolean front) {
        float across = (float) column / (columns - 1);
        return (face[0] + face[2] * (front ? 1 - across : across)) / spec.width;
    }

    private static float v(int[] face, Spec spec, int row, int rows) {
        return (face[1] + face[3] * (float) row / (rows - 1)) / spec.height;
    }

    private static Vector3f rel(Vector3fc point, Vec3 camera) {
        return new Vector3f(point).sub((float) camera.x, (float) camera.y, (float) camera.z);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, float u, float v, int light,
                               int overlay, Vector3f normal) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static Optional<Spec> load(ResourceLocation model, Armature armature) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "skeletal/" + model.getPath() + ".json");
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray size = json.getAsJsonArray("texture");
            List<Piece> pieces = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("cloth")) {
                JsonObject piece = element.getAsJsonObject();
                JsonArray pin = piece.getAsJsonArray("pin");
                String joint = piece.get("joint").getAsString();
                // The pin is written in model space: the joint's own space is the one its matrices move.
                Vector3f at = new Vector3f(pin.get(0).getAsFloat(), pin.get(1).getAsFloat(), pin.get(2).getAsFloat());
                Vector3f local = armature.inverseBind(armature.get(joint).index())
                        .transformPosition(new Vector3f(at));
                float length = piece.get("length").getAsFloat();
                int side = "back".equals(piece.get("side").getAsString()) ? -1 : 1;
                pieces.add(new Piece(piece.get("name").getAsString(), joint, local, piece.get("width").getAsFloat(),
                        length, side, ints(piece.getAsJsonArray("front")), ints(piece.getAsJsonArray("back")),
                        at.y - length, Math.max(0, side * at.z - INNERMOST)));
            }
            return Optional.of(new Spec(size.get(0).getAsInt(), size.get(1).getAsInt(), List.copyOf(pieces)));
        } catch (IOException | RuntimeException e) {
            Murimblock.LOGGER.error("Mob cloth {} could not be read: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    private static int[] ints(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }

    /** Dev: how far in front of {@code mob}'s feet (pixels) the bottom middle of a piece hangs, or NaN. */
    public static float ahead(Mob mob, String piece) {
        Map<String, Hanging> hanging = HANGING.get(mob);
        Hanging state = hanging == null ? null : hanging.get(piece);
        if (state == null) {
            return Float.NaN;
        }
        Cloth cloth = state.cloth;
        Vector3fc bottom = cloth.point(cloth.columns() / 2, cloth.rows() - 1);
        double yaw = Math.toRadians(mob.yBodyRot);
        return (float) (((bottom.x() - mob.getX()) * -Math.sin(yaw) + (bottom.z() - mob.getZ()) * Math.cos(yaw)) * 16);
    }

    /** For checks: the bottom middle of {@code mob}'s piece from the middle of its top edge (blocks), or null. */
    public static Vector3f hang(Mob mob, String piece) {
        Map<String, Hanging> hanging = HANGING.get(mob);
        Hanging state = hanging == null ? null : hanging.get(piece);
        if (state == null) {
            return null;
        }
        Cloth cloth = state.cloth;
        int middle = cloth.columns() / 2;
        return new Vector3f(cloth.point(middle, cloth.rows() - 1)).sub(cloth.point(middle, 0));
    }
}
