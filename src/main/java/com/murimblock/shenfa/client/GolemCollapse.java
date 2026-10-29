package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import com.murimblock.shenfa.Skinning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Gardien-talisman's death ({@code docs/mobs/boards/golem.png}): at tick 6 of it the talisman is torn off its face
 * and flutters up and away, slowly falling, while the statue goes on to its knees; when it is gone (tick 20) it falls
 * apart where it lay, each of its stone parts (head, helmet, chest, arms, fists, chains, legs...) tumbling down in the
 * pose it died in, with dust and the sound of breaking stone; the pieces lie a few seconds, then fade. Client side only.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GolemCollapse {
    private static final int LIFE = 100, FADE = 30, TALISMAN_LIFE = 90;
    private static final float PIXEL = 1 / 16f, GRAVITY = 0.04f;
    private static final List<Piece> PIECES = new ArrayList<>();
    /** The guardians whose talisman has flown already. */
    private static final Set<IronGolem> TORN = Collections.newSetFromMap(new WeakHashMap<>());

    private GolemCollapse() {
    }

    private static final class Piece {
        final SkinnedMesh mesh;
        final Matrix4f[] skin;
        final Vector3f centre, min, max;
        final float scale;
        final boolean paper;
        final int life;
        Vec3 pos, previous, velocity;
        final Quaternionf orientation = new Quaternionf(), previousOrientation = new Quaternionf();
        Vector3f spin;
        float spinSpeed;
        int age;
        boolean resting;

        Piece(SkinnedMesh mesh, Matrix4f[] skin, Vector3f min, Vector3f max, float scale, boolean paper) {
            this.mesh = mesh;
            this.skin = skin;
            this.min = min;
            this.max = max;
            this.centre = new Vector3f(min).add(max).mul(0.5f);
            this.scale = scale;
            this.paper = paper;
            this.life = paper ? TALISMAN_LIFE : LIFE;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PIECES.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof IronGolem golem && golem.deathTime >= GolemTalisman.TORN && !TORN.contains(golem)
                    && SkeletalMobs.look(golem) == SkeletalMobs.GOLEM) {
                TORN.add(golem);
                breakUp(golem, true);
            }
        }
        for (Iterator<Piece> it = PIECES.iterator(); it.hasNext(); ) {
            Piece piece = it.next();
            if (++piece.age > piece.life) {
                it.remove();
                continue;
            }
            move(mc.level, piece);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof IronGolem golem && golem.isDeadOrDying()
                && SkeletalMobs.look(golem) == SkeletalMobs.GOLEM) {
            breakUp(golem, false);
        }
    }

    /**
     * Breaks the guardian's look into its parts, as posed now: the talisman alone ({@code talisman}), or all the rest.
     */
    private static void breakUp(IronGolem golem, boolean talisman) {
        SkinnedModel model = SkeletalModels.get(SkeletalMobs.GOLEM.model());
        if (model == null || !(golem.level() instanceof ClientLevel level)) {
            return;
        }
        Pose pose = SkeletalMobs.pose(golem, 1, 0, 0);
        Matrix4f[] skin = pose.toSkinMatrices(model.armature());
        RandomSource random = golem.getRandom();
        float scale = golem.getScale();
        Quaternionf yaw = Axis.YP.rotationDegrees(-golem.yBodyRot);
        Vec3 at = golem.position();
        Vector3f middle = new Vector3f(0, 12, 0);
        for (SkinnedMesh mesh : model.parts()) {
            if (mesh.armor() || mesh.name().startsWith("talisman") != talisman) {
                continue;
            }
            float[] positions = new float[mesh.vertexCount() * 3], normals = new float[positions.length];
            Skinning.skin(mesh, skin, positions, normals);
            Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
            for (int i = 0; i < positions.length; i += 3) {
                Vector3f p = new Vector3f(positions[i], positions[i + 1], positions[i + 2]);
                min.min(p);
                max.max(p);
            }
            if (max.x - min.x < 0.01f && max.y - min.y < 0.01f) {
                // Hidden (scaled to nothing): nothing to break.
                continue;
            }
            Piece piece = new Piece(mesh, skin, min, max, scale, talisman);
            Vector3f offset = yaw.transform(new Vector3f(piece.centre).mul(scale * PIXEL));
            piece.pos = piece.previous = at.add(offset.x, offset.y, offset.z);
            piece.orientation.set(yaw);
            piece.previousOrientation.set(yaw);
            Vector3f out = yaw.transform(new Vector3f(piece.centre).sub(middle).mul(1, 0, 1));
            if (out.lengthSquared() < 1e-4f) {
                out.set(random.nextFloat() - 0.5f, 0, random.nextFloat() - 0.5f);
            }
            out.normalize();
            if (talisman) {
                // Torn off: it flutters up and away from the face.
                Vector3f ahead = yaw.transform(new Vector3f(0, 0, 1));
                piece.velocity = new Vec3(ahead.x * 0.06 + (random.nextFloat() - 0.5) * 0.04, 0.16,
                        ahead.z * 0.06 + (random.nextFloat() - 0.5) * 0.04);
                piece.spin = new Vector3f(0.3f, 1, 0.2f).normalize();
                piece.spinSpeed = 9 * Mth.DEG_TO_RAD;
            } else {
                // The stone gives way: the parts slump outward and tumble down.
                float speed = 0.05f + random.nextFloat() * 0.08f;
                piece.velocity = new Vec3(out.x * speed, 0.05 + random.nextFloat() * 0.12, out.z * speed);
                piece.spin = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f,
                        random.nextFloat() - 0.5f).normalize();
                piece.spinSpeed = (4 + random.nextFloat() * 10) * Mth.DEG_TO_RAD;
            }
            PIECES.add(piece);
        }
        if (talisman) {
            level.playLocalSound(at.x, at.y + 2.5, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1, 0.7f, false);
            return;
        }
        level.playLocalSound(at.x, at.y + 1, at.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f, false);
        level.playLocalSound(at.x, at.y + 1, at.z, SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.HOSTILE, 1, 0.5f, false);
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        for (int i = 0; i < 40; i++) {
            level.addParticle(dust, at.x + (random.nextFloat() - 0.5) * 1.6, at.y + random.nextFloat() * 1.6,
                    at.z + (random.nextFloat() - 0.5) * 1.6, 0, 0.1, 0);
        }
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (random.nextFloat() - 0.5) * 1.4, at.y + 0.3,
                    at.z + (random.nextFloat() - 0.5) * 1.4, 0, 0.02, 0);
        }
    }

    private static void move(ClientLevel level, Piece piece) {
        piece.previous = piece.pos;
        piece.previousOrientation.set(piece.orientation);
        if (piece.resting) {
            return;
        }
        if (piece.paper) {
            // Paper: barely falls, drifts, sways.
            piece.velocity = piece.velocity.add(0, -0.006, 0).scale(0.94);
            piece.velocity = piece.velocity.add(Mth.sin(piece.age * 0.3f) * 0.006, 0, Mth.cos(piece.age * 0.23f) * 0.006);
        } else {
            piece.velocity = piece.velocity.add(0, -GRAVITY, 0).scale(0.98);
        }
        piece.pos = piece.pos.add(piece.velocity);
        piece.orientation.premul(new Quaternionf().rotateAxis(piece.spinSpeed, piece.spin));
        float low = lowest(piece);
        BlockPos below = BlockPos.containing(piece.pos.x, piece.pos.y + low, piece.pos.z);
        VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (!shape.isEmpty()) {
            double top = below.getY() + shape.max(Direction.Axis.Y);
            if (piece.pos.y + low < top) {
                piece.pos = new Vec3(piece.pos.x, top - low, piece.pos.z);
                if (!piece.paper && piece.velocity.y < -0.12) {
                    piece.velocity = new Vec3(piece.velocity.x * 0.5, -piece.velocity.y * 0.25, piece.velocity.z * 0.5);
                    piece.spinSpeed *= 0.4f;
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                            piece.pos.x, piece.pos.y, piece.pos.z, 0, 0.05, 0);
                } else {
                    piece.velocity = Vec3.ZERO;
                    piece.spinSpeed = 0;
                    piece.resting = true;
                }
            }
        }
    }

    /** How far below its centre the piece's lowest corner is now (blocks, negative). */
    private static float lowest(Piece piece) {
        float low = 0;
        for (int corner = 0; corner < 8; corner++) {
            Vector3f c = new Vector3f((corner & 1) == 0 ? piece.min.x : piece.max.x,
                    (corner & 2) == 0 ? piece.min.y : piece.max.y, (corner & 4) == 0 ? piece.min.z : piece.max.z)
                    .sub(piece.centre);
            low = Math.min(low, piece.orientation.transform(c.mul(piece.scale * PIXEL)).y);
        }
        return low;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PIECES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(SkeletalMobs.GOLEM.texture());
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack poseStack = event.getPoseStack();
        for (Piece piece : PIECES) {
            float age = piece.age + partialTick;
            int alpha = (int) (255 * Mth.clamp((piece.life - age) / FADE, 0, 1));
            Vec3 at = piece.previous.lerp(piece.pos, partialTick).subtract(camera);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(new Quaternionf(piece.previousOrientation).slerp(piece.orientation, partialTick));
            poseStack.scale(piece.scale * PIXEL, piece.scale * PIXEL, piece.scale * PIXEL);
            poseStack.translate(-piece.centre.x, -piece.centre.y, -piece.centre.z);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(piece.pos.x, piece.pos.y + 0.2,
                    piece.pos.z));
            SkeletalPlayerRenderer.draw(piece.mesh, piece.skin, poseStack.last(), consumer, alpha << 24 | 0xFFFFFF,
                    light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }

    /** For checks: how many pieces of a guardian (and its talisman) are falling or lying about. */
    public static int pieces() {
        return PIECES.size();
    }

    /** For checks: whether its talisman is among them. */
    public static boolean talismanFlying() {
        return PIECES.stream().anyMatch(piece -> piece.paper);
    }
}
