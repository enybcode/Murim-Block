package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.martial.DevouringDemon;
import com.murimblock.martial.KaguneState;
import com.murimblock.network.KaguneEruptPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * The kagune bursting out of the ground under a foe, on every client (docs/combat/styles/devouring-demon-art.md): the
 * server says where, at the cast (KaguneEruptPayload), and the clients draw it coming up there on the blow's tick
 * (DevouringDemon.THORNS_HIT_TICK, BURROW_HIT_TICK), from the kagune's own chains (KaguneModel.drawChain):
 * <ul>
 *   <li><b>Impaling Thorns</b>: four tentacle-thorns shoot up out of the ground round the point, leaning out, two blocks
 *   high; they hold, then sink back;</li>
 *   <li><b>Burrow</b>: a centipede bursts up under each foe, rears three blocks high, arches over and comes down, then
 *   sinks back into the ground.</li>
 * </ul>
 * The ground breaks where they come out (the blocks' own particles), at that moment only.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneEruptions {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/kagune_forms.png");
    private static final String[] THORNS = {"upper_right_0", "lower_left_0", "upper_left_0", "lower_right_0"};
    /** How long a thorn comes up for, holds, and sinks back (s). */
    private static final float RISE = 0.08F, HOLD = 0.45F, SINK = 0.25F;
    private static final float BURROW_RISE = 0.12F, BURROW_HOLD = 0.6F, BURROW_SINK = 0.3F;

    private record Eruption(int attacker, int strike, List<Vec3> points, long since, boolean[] broke) { }
    private static final List<Eruption> ERUPTIONS = new ArrayList<>();

    private KaguneEruptions() { }

    public static void erupt(KaguneEruptPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i + 2 < p.points().size(); i += 3) points.add(new Vec3(p.points().get(i), p.points().get(i + 1), p.points().get(i + 2)));
        ERUPTIONS.add(new Eruption(p.attacker(), p.strike(), points, level.getGameTime(), new boolean[points.size()]));
    }

    /** How far out of the ground (0 to 1) at time t (s since the cast). */
    private static float out(float t, float at, float rise, float hold, float sink) {
        if (t < at) return 0;
        if (t < at + rise) return Mth.clamp((t - at) / rise, 0, 1);
        if (t < at + rise + hold) return 1;
        return 1 - Mth.clamp((t - at - rise - hold) / sink, 0, 1);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ERUPTIONS.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) { ERUPTIONS.clear(); return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = level.getGameTime();
        ERUPTIONS.removeIf(e -> now - e.since() > 60);
        Vec3 camera = event.getCamera().getPosition();
        var stack = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var type = RenderType.entityCutoutNoCull(TEXTURE);
        var out = buffers.getBuffer(type);
        var model = KaguneModel.get();
        for (Eruption e : ERUPTIONS) {
            float t = (now - e.since() + partial) / 20F;
            boolean burrow = e.strike() == KaguneState.STRIKE_BURROW;
            float at = (burrow ? DevouringDemon.BURROW_HIT_TICK : DevouringDemon.THORNS_HIT_TICK) / 20F - (burrow ? BURROW_RISE : RISE);
            var striker = level.getEntity(e.attacker());
            for (int n = 0; n < e.points().size(); n++) {
                Vec3 p = e.points().get(n);
                float up = burrow ? out(t, at, BURROW_RISE, BURROW_HOLD, BURROW_SINK) : out(t, at, RISE, HOLD, SINK);
                if (up > 0 && !e.broke()[n]) { e.broke()[n] = true; breakGround(level, p, burrow); }
                if (up <= 0) continue;
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(p.x, p.y + 1, p.z));
                // Toward the striker, on the ground: a burrowing centipede comes down that way.
                Vec3 toward = striker == null ? new Vec3(0, 0, 1) : striker.position().subtract(p).multiply(1, 0, 1);
                toward = toward.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : toward.normalize();
                if (burrow) {
                    String root = (n & 1) == 0 ? "centipede_right_0" : "centipede_left_0";
                    int joints = model.chainLength(model.bone(root));
                    float length = joints * 4.5F / 16, high = 3.2F;
                    // Up out of the ground, then arched over and down (toward where it came from).
                    float over = Mth.clamp((t - at - BURROW_RISE) / 0.18F, 0, 1);
                    float arc = (float) Math.toDegrees(2.6F * ease(over)) / Math.max(1, joints - 1);
                    Vector3f dir = new Vector3f((float) toward.x * 0.15F, 1, (float) toward.z * 0.15F).normalize();
                    // A chain bends toward its "top" in these mirrored frames (as KaguneStrikes found): toward the striker.
                    Vector3f top = new Vector3f((float) toward.x, 0, (float) toward.z);
                    chain(stack, camera, p, length, high * up, model, root, "idle_demon", t, dir, top, 6, t * 20, arc, out, light);
                } else {
                    for (int k = 0; k < THORNS.length; k++) {
                        double a = Math.PI / 2 * k + 0.4 + n;
                        Vec3 at2 = p.add(Math.cos(a) * 0.32, 0, Math.sin(a) * 0.32);
                        // Leaning out from the point, a little each its own way.
                        Vector3f dir = new Vector3f((float) Math.cos(a) * 0.32F, 1, (float) Math.sin(a) * 0.32F).normalize();
                        Vector3f top = new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a));
                        int joints = model.chainLength(model.bone(THORNS[k]));
                        float length = joints * 3.4F / 16, high = 2.0F + 0.3F * (k % 2);
                        chain(stack, camera, at2, length, high * out(t, at + 0.02F * k, RISE, HOLD - 0.02F * k, SINK), model, THORNS[k], "idle_hunger", t,
                                dir, top, 4, t * 30 + k, 0, out, light);
                    }
                }
            }
        }
        buffers.endBatch(type);
    }

    /** One chain standing out of the ground at {@code at}: its first joint under the ground, {@code visible} blocks of it out. */
    private static void chain(com.mojang.blaze3d.vertex.PoseStack stack, Vec3 camera, Vec3 at, float length, float visible, KaguneModel model,
                              String root, String animation, float t, Vector3f dir, Vector3f top, float curl, float phase, float arc,
                              com.mojang.blaze3d.vertex.VertexConsumer out, int light) {
        if (visible <= 0.01F) return;
        stack.pushPose();
        stack.translate(at.x - camera.x, at.y - camera.y - (length - visible), at.z - camera.z);
        stack.scale(-1 / 16F, 1 / 16F, 1 / 16F);
        // The stack here is the world's axes (scaled to pixels, x mirrored): the directions are given in the world's, as
        // KaguneModel.aim reads them in its base's space.
        model.drawChain(model.bone(root), animation, t, dir, top, curl, phase, arc, 1, stack, out, light);
        stack.popPose();
    }

    /** The ground breaks where the kagune comes out: the blocks' own particles, thrown up. */
    private static void breakGround(net.minecraft.client.multiplayer.ClientLevel level, Vec3 p, boolean burrow) {
        var below = level.getBlockState(BlockPos.containing(p.x, p.y - 0.5, p.z));
        if (below.isAir()) return;
        var particle = new BlockParticleOption(ParticleTypes.BLOCK, below);
        var random = level.random;
        for (int i = 0; i < (burrow ? 40 : 24); i++) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * (burrow ? 1.0 : 0.6);
            level.addParticle(particle, p.x + Math.cos(a) * r, p.y + 0.05, p.z + Math.sin(a) * r,
                    Math.cos(a) * 0.15, 0.25 + random.nextDouble() * 0.35, Math.sin(a) * 0.15);
        }
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
