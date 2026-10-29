package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.combat.SealFighting;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * The Sceau de jade vivant's marks (storyboard: docs/mobs/boards/seal.png). Its blow slams it down in dust and leaves
 * its seal stamped red on the ground for 3 s: the carved underside of its model, printed where it fell. As it dies it
 * cracks and trembles, then bursts at tick {@link #BURST} in a spray of jade; the smaller seals vanilla makes of it
 * there roll over and right themselves ({@link #fromBurst}). Client side only.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SealStamp {
    /** The death tick it bursts on; it is no longer drawn from then. */
    public static final int BURST = 17;
    private static final int LIFE = 60, FADE = 20, BURST_MEMORY = 30;
    private static final Vector3f CINNABAR = new Vector3f(0.70f, 0.15f, 0.16f);
    private static final List<Mark> MARKS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    /** The underside's four corners on the model (x, z, pixels) and on its texture (u, v), found once it is loaded. */
    private static float[][] underside;

    private SealStamp() {
    }

    private static final class Mark {
        final Vec3 at;
        final float scale, yaw;
        int age;

        Mark(Vec3 at, float scale, float yaw) {
            this.at = at;
            this.scale = scale;
            this.yaw = yaw;
        }
    }

    private record Burst(Vec3 at, long time) {
    }

    /** Its blow lands: a dry knock, dust ringing out, the ground shaking near it, the red mark. */
    static void slam(Slime seal) {
        Level level = seal.level();
        RandomSource random = seal.getRandom();
        float size = seal.getSize() / 4f;
        level.playLocalSound(seal.getX(), seal.getY(), seal.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE,
                1.2f, 0.55f, false);
        level.playLocalSound(seal.getX(), seal.getY(), seal.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE,
                0.15f, 0.5f, false);
        BlockState ground = level.getBlockState(seal.blockPosition().below());
        if (!ground.isAir()) {
            BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            int count = share(28);
            for (int i = 0; i < count; i++) {
                float a = Mth.TWO_PI * i / count, r = 1.0f * size + random.nextFloat() * 0.3f;
                level.addParticle(dust, seal.getX() + Mth.cos(a) * r, seal.getY() + 0.1, seal.getZ() + Mth.sin(a) * r,
                        Mth.cos(a) * 0.25, 0.12, Mth.sin(a) * 0.25);
            }
        }
        for (int i = 0; i < share(6); i++) {
            level.addParticle(SealFighting.JADE, seal.getX(), seal.getY() + 0.3, seal.getZ(),
                    (random.nextFloat() - 0.5f) * 0.4, 0.2 + random.nextFloat() * 0.2, (random.nextFloat() - 0.5f) * 0.4);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.distanceTo(seal) < 6 * size + 2) {
            CameraShake.start(0.7f * size, 7);
        }
        MARKS.add(new Mark(seal.position(), size, seal.yBodyRot));
    }

    /** Each tick of its death: cracking sounds and chips, then the burst. */
    static void dying(Slime seal) {
        Level level = seal.level();
        RandomSource random = seal.getRandom();
        float size = seal.getSize() / 4f;
        double y = seal.getY() + 0.9 * size;
        if (seal.deathTime < BURST && seal.deathTime % 4 == 1) {
            level.playLocalSound(seal.getX(), y, seal.getZ(), SoundEvents.DEEPSLATE_TILES_HIT, SoundSource.HOSTILE,
                    0.6f, 0.7f + seal.deathTime * 0.04f, false);
            level.addParticle(SealFighting.JADE, seal.getX() + (random.nextFloat() - 0.5f) * size * 1.6, y,
                    seal.getZ() + (random.nextFloat() - 0.5f) * size * 1.6, 0, 0.08, 0);
        }
        if (seal.deathTime == BURST) {
            for (int i = 0; i < share((int) (16 + 36 * size)); i++) {
                level.addParticle(SealFighting.JADE, seal.getX() + (random.nextFloat() - 0.5f) * size * 1.6,
                        seal.getY() + random.nextFloat() * size * 1.8, seal.getZ() + (random.nextFloat() - 0.5f) * size * 1.6,
                        (random.nextFloat() - 0.5f) * 0.5, 0.15 + random.nextFloat() * 0.3, (random.nextFloat() - 0.5f) * 0.5);
            }
            for (int i = 0; i < share(10); i++) {
                level.addParticle(new DustParticleOptions(CINNABAR, 1.2f), seal.getX(), y, seal.getZ(),
                        (random.nextFloat() - 0.5f) * 0.3, 0.1, (random.nextFloat() - 0.5f) * 0.3);
            }
            level.playLocalSound(seal.getX(), y, seal.getZ(), SoundEvents.DEEPSLATE_TILES_BREAK, SoundSource.HOSTILE,
                    1.2f, 0.7f, false);
            level.playLocalSound(seal.getX(), y, seal.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE,
                    0.8f, 0.6f, false);
            BURSTS.add(new Burst(seal.position(), level.getGameTime()));
        }
    }

    /** Whether a seal just appeared where a bigger one burst: one of its pieces, which rolls over and stands up. */
    static boolean fromBurst(Slime seal) {
        long now = seal.level().getGameTime();
        return BURSTS.stream().anyMatch(burst -> now - burst.time <= BURST_MEMORY
                && burst.at.distanceToSqr(seal.position()) < 9);
    }

    private static int share(int count) {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.ALL ? count : status == ParticleStatus.DECREASED ? count / 2 : count / 4;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MARKS.clear();
            BURSTS.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        for (Iterator<Mark> it = MARKS.iterator(); it.hasNext(); ) {
            if (++it.next().age > LIFE) {
                it.remove();
            }
        }
        long now = mc.level.getGameTime();
        BURSTS.removeIf(burst -> now - burst.time > BURST_MEMORY || now < burst.time);
    }

    /** The underside of its block: the corners of the faces looking down, with their place on the texture. */
    private static float[][] underside(SkinnedModel model) {
        if (underside == null) {
            for (SkinnedMesh mesh : model.parts()) {
                if (!mesh.name().equals("block")) {
                    continue;
                }
                List<float[]> corners = new ArrayList<>();
                for (int i = 0; i < mesh.vertexCount(); i++) {
                    if (mesh.normals()[i * 3 + 1] > -0.9f) {
                        continue;
                    }
                    float x = mesh.positions()[i * 3], z = mesh.positions()[i * 3 + 2];
                    if (corners.stream().noneMatch(c -> Math.abs(c[0] - x) < 0.01f && Math.abs(c[1] - z) < 0.01f)) {
                        corners.add(new float[] {x, z, mesh.uvs()[i * 2], mesh.uvs()[i * 2 + 1]});
                    }
                }
                if (corners.size() == 4) {
                    // Round the square: corners in turn about its middle.
                    corners.sort(java.util.Comparator.comparingDouble(c -> Math.atan2(c[1], c[0])));
                    underside = corners.toArray(new float[0][]);
                }
            }
        }
        return underside;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || MARKS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SkinnedModel model = SkeletalModels.get(SkeletalMobs.SCEAU.model());
        float[][] corners = model == null ? null : underside(model);
        if (mc.level == null || corners == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(SkeletalMobs.SCEAU.texture());
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack poseStack = event.getPoseStack();
        for (Mark mark : MARKS) {
            float age = mark.age + partialTick;
            int alpha = (int) (230 * Mth.clamp((LIFE - age) / FADE, 0, 1));
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(mark.at.x, mark.at.y + 0.1, mark.at.z));
            Vec3 at = mark.at.subtract(camera);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y + 0.012, at.z);
            // Turned and sized as the seal was, its pixels to blocks.
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-mark.yaw));
            float scale = mark.scale / 16;
            poseStack.scale(scale, 1, scale);
            PoseStack.Pose pose = poseStack.last();
            for (int i = 0; i < 4; i++) {
                float[] c = corners[i];
                consumer.addVertex(pose, c[0], 0, c[1]).setColor(alpha << 24 | 0xFFFFFF).setUv(c[2], c[3])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
            }
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }

    /** For checks: how many marks are stamped on the ground. */
    public static int marks() {
        return MARKS.size();
    }
}
