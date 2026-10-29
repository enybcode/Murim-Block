package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The Bête des Cinq Venins' death (storyboard: docs/mobs/boards/venins_death.png): on the Fileuse's death clip its
 * venom sac swells and pulses ({@link #grow}) as it rolls onto its back, then bursts at tick {@link #BURST} in a spray
 * of venom and spreads into a smoking green puddle that stays 5 s. The puddle drawn here is the look of the poison
 * cloud the server leaves there ({@code SpiderFighting}). Client side only.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class VenomBurst {
    /** The death tick the sac bursts on. */
    public static final int BURST = 10;
    private static final int LIFE = 100, SPREAD = 10, FADE = 30, SIDES = 14;
    private static final float RADIUS = 1.2f;
    private static final List<Puddle> PUDDLES = new ArrayList<>();
    /** Where on the Bête's texture its venom is (the sac's middle), found once its model is loaded. */
    private static float[] venomUv;

    private VenomBurst() {
    }

    private static final class Puddle {
        final Vec3 at;
        final float scale;
        final float turn;
        int age;

        Puddle(Vec3 at, float scale, float turn) {
            this.at = at;
            this.scale = scale;
            this.turn = turn;
        }
    }

    /** How much the dying Bête's sac has swollen (1 when not dying), pulsing as it nears the burst; 0 once burst. */
    static float grow(Mob mob, float partialTick) {
        if (mob.deathTime == 0) {
            return 1;
        }
        float time = mob.deathTime + partialTick;
        if (time >= BURST) {
            return 0;
        }
        return 1 + 0.3f * time / BURST + 0.06f * Mth.sin(time * 2.2f);
    }

    /** Each tick of the Bête's death: its hiss as it is struck, the burst and the puddle. */
    static void dying(Mob mob) {
        Level level = mob.level();
        float scale = mob.getScale() * 0.7f;
        if (mob.deathTime == 1) {
            level.playLocalSound(mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE,
                    0.3f, 1.6f, false);
        }
        if (mob.deathTime == BURST) {
            RandomSource random = mob.getRandom();
            double y = mob.getY() + 0.45 * scale;
            for (int i = 0; i < share(24); i++) {
                level.addParticle(ParticleTypes.ITEM_SLIME, mob.getX(), y, mob.getZ(), (random.nextFloat() - 0.5f) * 0.5,
                        0.25 + random.nextFloat() * 0.35, (random.nextFloat() - 0.5f) * 0.5);
            }
            level.playLocalSound(mob.getX(), y, mob.getZ(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 0.9f,
                    0.8f, false);
            PUDDLES.add(new Puddle(new Vec3(mob.getX(), mob.getY(), mob.getZ()), 1, random.nextFloat() * Mth.TWO_PI));
        }
    }

    private static int share(int count) {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.ALL ? count : status == ParticleStatus.DECREASED ? count / 2 : count / 4;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PUDDLES.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        for (Iterator<Puddle> it = PUDDLES.iterator(); it.hasNext(); ) {
            Puddle puddle = it.next();
            if (++puddle.age > LIFE) {
                it.remove();
                continue;
            }
            // It bubbles and smokes green.
            if (mc.level.random.nextInt(3) == 0) {
                float a = mc.level.random.nextFloat() * Mth.TWO_PI, r = mc.level.random.nextFloat() * RADIUS * 0.8f;
                mc.level.addParticle(puddle.age % 2 == 0 ? ParticleTypes.ITEM_SLIME : ParticleTypes.SNEEZE,
                        puddle.at.x + Mth.cos(a) * r, puddle.at.y + 0.05, puddle.at.z + Mth.sin(a) * r, 0, 0.02, 0);
            }
        }
    }

    /** The venom's place on the Bête's texture: the middle of its sac's faces. */
    private static float[] venomUv(SkinnedModel model) {
        if (venomUv == null) {
            for (SkinnedMesh mesh : model.parts()) {
                if (mesh.name().equals("sac")) {
                    float u = 0, v = 0;
                    for (int i = 0; i < mesh.vertexCount(); i++) {
                        u += mesh.uvs()[i * 2];
                        v += mesh.uvs()[i * 2 + 1];
                    }
                    venomUv = new float[] {u / mesh.vertexCount(), v / mesh.vertexCount()};
                }
            }
        }
        return venomUv;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PUDDLES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SkinnedModel model = SkeletalModels.get(SkeletalMobs.VENINS.model());
        float[] uv = model == null ? null : venomUv(model);
        if (mc.level == null || uv == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(SkeletalMobs.VENINS.texture());
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack poseStack = event.getPoseStack();
        for (Puddle puddle : PUDDLES) {
            float age = puddle.age + partialTick;
            float radius = RADIUS * puddle.scale * Mth.clamp(age / SPREAD, 0.2f, 1);
            int alpha = (int) (200 * Mth.clamp((LIFE - age) / FADE, 0, 1));
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(puddle.at.x, puddle.at.y + 0.1,
                    puddle.at.z));
            Vec3 at = puddle.at.subtract(camera);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y + 0.015, at.z);
            PoseStack.Pose pose = poseStack.last();
            // A ragged disc: each side's edge pulled in or out a little, so it does not read as a circle.
            for (int i = 0; i < SIDES; i++) {
                float a0 = puddle.turn + Mth.TWO_PI * i / SIDES, a1 = puddle.turn + Mth.TWO_PI * (i + 1) / SIDES;
                float r0 = radius * (0.8f + 0.25f * Mth.sin(i * 2.7f)), r1 = radius * (0.8f + 0.25f * Mth.sin((i + 1) * 2.7f));
                vertex(consumer, pose, 0, 0, uv, alpha, light);
                vertex(consumer, pose, Mth.cos(a1) * r1, Mth.sin(a1) * r1, uv, alpha, light);
                vertex(consumer, pose, Mth.cos(a0) * r0, Mth.sin(a0) * r0, uv, alpha, light);
                vertex(consumer, pose, Mth.cos(a0) * r0, Mth.sin(a0) * r0, uv, alpha, light);
            }
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float[] uv, int alpha,
                               int light) {
        consumer.addVertex(pose, x, 0, z).setColor(alpha << 24 | 0xFFFFFF).setUv(uv[0], uv[1])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    /** For checks: how many puddles lie about. */
    public static int puddles() {
        return PUDDLES.size();
    }
}
