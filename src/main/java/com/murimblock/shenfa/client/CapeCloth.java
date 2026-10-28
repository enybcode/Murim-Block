package com.murimblock.shenfa.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.Cloth;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The player's cape as cloth ({@link Cloth}) instead of vanilla's stiff board: its top edge is pinned across the top
 * of the back, it hangs and swings with the body's motion, and it stays off the back and the legs. Drawn with the
 * player's cape texture, both faces. {@code /skeletal cape test} shows a plain test cape on players without one.
 */
public final class CapeCloth {
    private static final int COLUMNS = 6, ROWS = 10;
    /** The cape is 10 by 16 pixels, hung 2.2 pixels behind the chest's middle. */
    private static final float WIDTH = 10, HEIGHT = 16, BACK = 2.25f;
    private static final Map<AbstractClientPlayer, State> STATES = new WeakHashMap<>();
    private static boolean enabled = true;
    private static ResourceLocation testCape;
    private static boolean useTestCape;

    private CapeCloth() {
    }

    private static final class State {
        final Cloth cloth = new Cloth(COLUMNS, ROWS, WIDTH / 16, HEIGHT / 16);
        long lastNanos;
        Vector3f lastPin;
    }

    public static void enable(boolean on) {
        enabled = on;
    }

    /** Dev: players without a cape wear a plain test cape (an original texture made here). */
    public static void testCape(boolean on) {
        useTestCape = on;
    }

    /** The cape texture {@code player} shows, or null when it shows none (vanilla's rules: no elytra, shown, visible). */
    static ResourceLocation texture(AbstractClientPlayer player) {
        if (player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return null;
        }
        ResourceLocation cape = player.getSkin().capeTexture();
        return cape != null ? cape : useTestCape ? testTexture() : null;
    }

    /** Whether the cape is drawn as cloth here (vanilla's cape layer then stays out). */
    static boolean drawn(AbstractClientPlayer player) {
        return enabled && texture(player) != null;
    }

    /**
     * Simulates and draws {@code player}'s cape.
     *
     * @param model  the body's model space to view space (blocks; the camera-relative world)
     * @param joints the pose's joints in model space (pixels)
     */
    static void render(AbstractClientPlayer player, Matrix4f model, Matrix4f[] joints, MultiBufferSource buffers,
                       int light) {
        ResourceLocation texture = texture(player);
        if (!enabled || texture == null) {
            return;
        }
        State state = STATES.computeIfAbsent(player, key -> new State());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Matrix4f chest = joints[Biped.ARMATURE.get(Biped.CHEST).index()];
        Matrix4f hips = joints[Biped.ARMATURE.get(Biped.ROOT).index()];
        Vector3f[] top = new Vector3f[COLUMNS];
        for (int c = 0; c < COLUMNS; c++) {
            float x = WIDTH / 2 - WIDTH * c / (COLUMNS - 1);
            top[c] = world(model, chest, new Vector3f(x, 6, -BACK), camera);
        }
        Vector3f down = world(model, chest, new Vector3f(0, -6, -BACK), camera).sub(world(model, chest,
                new Vector3f(0, 6, -BACK), camera));
        long now = System.nanoTime();
        float seconds = state.lastNanos == 0 ? 0 : Math.min(0.1f, (now - state.lastNanos) / 1e9f);
        state.lastNanos = now;
        if (state.lastPin != null && state.lastPin.distance(top[0]) > 4) {
            state.cloth.reset();
        }
        state.lastPin = new Vector3f(top[0]);
        Cloth.Plane back = plane(model, chest, camera, -BACK + 0.6f);
        Cloth.Plane legs = plane(model, hips, camera, legsBack(joints, hips));
        state.cloth.update(top, down, seconds, back, legs);
        draw(state.cloth, texture, camera, buffers, light);
    }

    /**
     * For checks: the bottom middle of {@code player}'s cape from the middle of its top edge (blocks, world axes), or
     * null when it wears none.
     */
    public static Vector3f hang(AbstractClientPlayer player) {
        State state = STATES.get(player);
        if (state == null) {
            return null;
        }
        Cloth cloth = state.cloth;
        int middle = cloth.columns() / 2;
        return new Vector3f(cloth.point(middle, cloth.rows() - 1)).sub(cloth.point(middle, 0));
    }

    /**
     * How far behind the hips the legs' plane stands (pixels, along the hips' z): just behind the backs of the thighs,
     * further back when a leg swings behind the body (a swimming kick, a stride), so the legs never poke through.
     */
    private static float legsBack(Matrix4f[] joints, Matrix4f hips) {
        Matrix4f toHips = new Matrix4f(hips).invert();
        float back = -2.4f;
        for (String side : new String[] {"L", "R"}) {
            Matrix4f thigh = joints[Biped.ARMATURE.get("Thigh_" + side).index()];
            Matrix4f shin = joints[Biped.ARMATURE.get("Leg_" + side).index()];
            // The back of the thigh's middle and of the knee.
            back = Math.min(back, toHips.transformPosition(thigh.transformPosition(new Vector3f(0, -3, -2))).z - 0.4f);
            back = Math.min(back, toHips.transformPosition(shin.transformPosition(new Vector3f(0, 0, -2))).z - 0.4f);
        }
        return Math.max(back, -8);
    }

    /** A plane behind a joint: the cloth stays at least on its far side (toward the body's back). */
    private static Cloth.Plane plane(Matrix4f model, Matrix4f joint, Vec3 camera, float z) {
        Vector3f point = world(model, joint, new Vector3f(0, 0, z), camera);
        Vector3f normal = world(model, joint, new Vector3f(0, 0, z - 1), camera).sub(point).normalize();
        return new Cloth.Plane(point, normal, 0.005f);
    }

    private static Vector3f world(Matrix4f model, Matrix4f joint, Vector3f pixels, Vec3 camera) {
        Vector3f at = joint.transformPosition(pixels).mul(1 / 16f);
        model.transformPosition(at);
        return at.add((float) camera.x, (float) camera.y, (float) camera.z);
    }

    private static void draw(Cloth cloth, ResourceLocation texture, Vec3 camera, MultiBufferSource buffers, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = new PoseStack().last();
        Vector3f normal = new Vector3f();
        for (int r = 0; r + 1 < cloth.rows(); r++) {
            for (int c = 0; c + 1 < cloth.columns(); c++) {
                Vector3f a = rel(cloth.point(c, r), camera), b = rel(cloth.point(c + 1, r), camera),
                        d = rel(cloth.point(c + 1, r + 1), camera), e = rel(cloth.point(c, r + 1), camera);
                new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a), normal).normalize();
                // The outside of the cape (seen from behind the body), then the inside.
                vertex(consumer, pose, a, u(c, false), v(r), light, normal);
                vertex(consumer, pose, e, u(c, false), v(r + 1), light, normal);
                vertex(consumer, pose, d, u(c + 1, false), v(r + 1), light, normal);
                vertex(consumer, pose, b, u(c + 1, false), v(r), light, normal);
                normal.negate();
                vertex(consumer, pose, b, u(c + 1, true), v(r), light, normal);
                vertex(consumer, pose, d, u(c + 1, true), v(r + 1), light, normal);
                vertex(consumer, pose, e, u(c, true), v(r + 1), light, normal);
                vertex(consumer, pose, a, u(c, true), v(r), light, normal);
            }
        }
    }

    private static Vector3f rel(org.joml.Vector3fc point, Vec3 camera) {
        return new Vector3f(point).sub((float) camera.x, (float) camera.y, (float) camera.z);
    }

    /** The cape texture's faces: the outside at x 1 to 11, the inside at 12 to 22 (of 64), y 1 to 17 (of 32). */
    private static float u(int column, boolean inside) {
        float across = (float) column / (COLUMNS - 1);
        return inside ? (12 + 10 * across) / 64 : (11 - 10 * across) / 64;
    }

    private static float v(int row) {
        return (1 + 16f * row / (ROWS - 1)) / 32;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, float u, float v, int light,
                               Vector3f normal) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, normal.x, normal.y, normal.z);
    }

    /** A plain cape drawn here: deep red, a gold border. */
    private static ResourceLocation testTexture() {
        if (testCape == null) {
            NativeImage image = new NativeImage(64, 32, true);
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 32; y++) {
                    boolean border = x == 1 || x == 10 || x == 12 || x == 21 || y == 1 || y == 16;
                    // NativeImage colors are ABGR.
                    image.setPixelRGBA(x, y, border ? 0xFF2FB4E8 : 0xFF1C1C8C);
                }
            }
            testCape = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "dynamic/test_cape");
            Minecraft.getInstance().getTextureManager().register(testCape, new DynamicTexture(image));
        }
        return testCape;
    }
}
