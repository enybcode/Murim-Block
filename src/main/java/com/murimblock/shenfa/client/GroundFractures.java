package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.registry.MurimBlocks;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The ground fracturing (shenfa.GroundFracture, or a clip's {@code fracture} event), drawn the way V1's spiritual
 * pressure drew it with Epic Fight's ground slam: every block of the ground's top within the radius is lifted out of the
 * world for a while (a {@code fractured_ground} block, solid but drawn by nobody, stands in for it) and drawn tilted
 * away from the centre, the more the farther out, the outer half lifted, with a little jitter; the wave spreads from the
 * centre, each block popping up as it passes, then all settle back flat and the real blocks return. The plant or snow
 * on a block goes with it. Client side only: the server's world never changes.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GroundFractures {
    /** How fast the wave runs out (ticks per block), how long a block takes to rise and to settle (ticks). */
    private static final float WAVE = 0.8f;
    private static final int RISE = 4, SETTLE = 10;
    /** Flags for the client's own changes: redraw, no neighbour updates, no drops. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<BlockPos, Shard> SHARDS = new LinkedHashMap<>();
    private static long now;

    private GroundFractures() {
    }

    /** One block of broken ground. */
    private static final class Shard {
        final BlockPos pos;
        final BlockState ground;
        final BlockState cover;
        final Quaternionf tilt;
        final float lift, pop;
        final long start;
        long end;
        boolean out;

        Shard(BlockPos pos, BlockState ground, BlockState cover, Quaternionf tilt, float lift, float pop, long start,
                long end) {
            this.pos = pos;
            this.ground = ground;
            this.cover = cover;
            this.tilt = tilt;
            this.lift = lift;
            this.pop = pop;
            this.start = start;
            this.end = end;
        }
    }

    public static void start(CombatPayloads.Fracture payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            start(mc.level, new Vec3(payload.x(), payload.y(), payload.z()), payload.radius(), payload.seed());
        }
    }

    /** Fractures the ground round {@code center} (feet on the ground) out to {@code radius} blocks. */
    public static void start(Level level, Vec3 center, float radius, int seed) {
        if (!level.isClientSide || radius <= 0) {
            return;
        }
        radius = Math.max(0.5f, radius);
        RandomSource random = RandomSource.create(seed);
        int base = Mth.floor(center.y - 0.5);
        int reach = Mth.ceil(radius);
        BlockPos middle = BlockPos.containing(center.x, base, center.z);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double x = middle.getX() + dx + 0.5 - center.x, z = middle.getZ() + dz + 0.5 - center.z;
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius) {
                    continue;
                }
                BlockPos pos = top(level, middle.offset(dx, 0, dz));
                if (pos == null) {
                    continue;
                }
                float out = (float) (distance / radius);
                long start = now + (long) (distance * WAVE);
                long end = start + 40 + random.nextInt(Math.max(1, (int) (radius * 20)));
                Shard old = SHARDS.get(pos);
                if (old != null) {
                    // Broken again before it mended: it stays broken longer.
                    old.end = Math.max(old.end, end);
                    continue;
                }
                // Tilted away from the centre (its outer edge up), more the farther out, with a little jitter.
                Vector3f axis = distance < 0.3 ? new Vector3f(random.nextFloat() - 0.5f, 0, random.nextFloat() - 0.5f)
                        : new Vector3f((float) -z, 0, (float) x);
                if (axis.lengthSquared() < 1e-6) {
                    axis.set(1, 0, 0);
                }
                axis.normalize();
                Quaternionf tilt = new Quaternionf().rotateAxis((float) Math.toRadians(out * 22 + random.nextFloat()
                        * 12 - 6), axis)
                        .rotateX((float) Math.toRadians(random.nextFloat() * 15 - 7.5))
                        .rotateY((float) Math.toRadians(random.nextFloat() * 40 - 20))
                        .rotateZ((float) Math.toRadians(random.nextFloat() * 15 - 7.5));
                float lift = Math.max(0, out - 0.4f) * 0.6f;
                BlockState ground = level.getBlockState(pos), above = level.getBlockState(pos.above());
                BlockState cover = covers(level, pos.above(), above) ? above : null;
                SHARDS.put(pos.immutable(), new Shard(pos.immutable(), ground, cover, tilt, lift, 0.35f * out * out,
                        start, end));
            }
        }
        BlockState under = level.getBlockState(middle);
        if (!under.isAir()) {
            level.playLocalSound(center.x, center.y, center.z, under.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                    1, 0.6f, false);
        }
        if (radius >= 1.5f) {
            level.playLocalSound(center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    Math.min(1, radius / 6), 0.55f, false);
        }
    }

    /** The ground's top in this column, a block above or below {@code at} too: a full block with room above it. */
    private static BlockPos top(Level level, BlockPos at) {
        for (int dy : new int[] {1, 0, -1}) {
            BlockPos pos = at.above(dy);
            if (breaks(level, pos) && !full(level, pos.above())) {
                return pos;
            }
        }
        return null;
    }

    /** Whether a block can fracture: full and plain (no block entity), drawn as a model. */
    private static boolean breaks(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return full(level, pos) && !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL
                && state.getFluidState().isEmpty() && !state.is(MurimBlocks.FRACTURED_GROUND.get());
    }

    private static boolean full(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return Block.isFaceFull(state.getCollisionShape(level, pos, CollisionContext.empty()), Direction.DOWN);
    }

    /** Whether what lies on the block goes with it: a plant, snow, a carpet (no collision, no block entity). */
    private static boolean covers(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL
                && state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        now++;
        for (Iterator<Shard> it = SHARDS.values().iterator(); it.hasNext(); ) {
            Shard shard = it.next();
            if (!shard.out && now >= shard.start) {
                lift(mc, shard);
            }
            if (now >= shard.end) {
                put(mc, shard);
                it.remove();
            }
        }
    }

    /** The wave reaches the block: the real one leaves the world, dust and chips burst from it. */
    private static void lift(Minecraft mc, Shard shard) {
        shard.out = true;
        var level = mc.level;
        if (!level.getBlockState(shard.pos).equals(shard.ground)) {
            // Changed since (mined, built over): it does not break.
            shard.end = shard.start;
            return;
        }
        level.setBlock(shard.pos, MurimBlocks.FRACTURED_GROUND.get().defaultBlockState(), FLAGS);
        if (shard.cover != null && level.getBlockState(shard.pos.above()).equals(shard.cover)) {
            level.setBlock(shard.pos.above(), Blocks.AIR.defaultBlockState(), FLAGS);
        }
        if (mc.options.particles().get() != net.minecraft.client.ParticleStatus.MINIMAL) {
            RandomSource random = level.random;
            for (int i = 0; i < 3; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, shard.ground),
                        shard.pos.getX() + random.nextFloat(), shard.pos.getY() + 1.05, shard.pos.getZ() + random.nextFloat(),
                        (random.nextFloat() - 0.5) * 0.2, 0.15 + random.nextFloat() * 0.2, (random.nextFloat() - 0.5) * 0.2);
            }
        }
    }

    /** The block mends: the real one comes back, if nothing else took its place meanwhile. */
    private static void put(Minecraft mc, Shard shard) {
        if (!shard.out) {
            return;
        }
        var level = mc.level;
        if (level.getBlockState(shard.pos).is(MurimBlocks.FRACTURED_GROUND.get())) {
            level.setBlock(shard.pos, shard.ground, FLAGS);
            if (shard.cover != null && level.getBlockState(shard.pos.above()).isAir()) {
                level.setBlock(shard.pos.above(), shard.cover, FLAGS);
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SHARDS.clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || SHARDS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blocks = mc.getBlockRenderer();
        PoseStack poseStack = event.getPoseStack();
        Set<RenderType> used = new HashSet<>();
        float time = now + partialTick;
        for (Shard shard : SHARDS.values()) {
            if (!shard.out || !mc.level.getBlockState(shard.pos).is(MurimBlocks.FRACTURED_GROUND.get())) {
                continue;
            }
            float age = time - shard.start;
            // Rises fast with a pop, holds, settles flat before the real block returns.
            float k = Mth.clamp(age / RISE, 0, 1) * Mth.clamp((shard.end - time) / SETTLE, 0, 1);
            k = k * k * (3 - 2 * k);
            float pop = age < 8 ? shard.pop * Mth.sin(age / 8 * Mth.PI) : 0;
            poseStack.pushPose();
            poseStack.translate(shard.pos.getX() - camera.x + 0.5, shard.pos.getY() - camera.y + 0.5 + shard.lift * k
                    + pop, shard.pos.getZ() - camera.z + 0.5);
            poseStack.mulPose(new Quaternionf().slerp(shard.tilt, k));
            poseStack.translate(-0.5, -0.5, -0.5);
            draw(mc, blocks, buffers, poseStack, shard.ground, shard.pos, shard.pos.above(), used);
            if (shard.cover != null) {
                poseStack.translate(0, 1, 0);
                draw(mc, blocks, buffers, poseStack, shard.cover, shard.pos.above(), shard.pos.above(), used);
            }
            poseStack.popPose();
        }
        for (RenderType type : used) {
            buffers.endBatch(type);
        }
    }

    /**
     * One block's model, tinted as the world would tint it at {@code pos} and lit by the open air above the ground
     * (out of the ground, its sides catch the light: lit from inside the ground they would be black).
     */
    private static void draw(Minecraft mc, BlockRenderDispatcher blocks, MultiBufferSource buffers, PoseStack poseStack,
            BlockState state, BlockPos pos, BlockPos air, Set<RenderType> used) {
        BakedModel model = blocks.getBlockModel(state);
        int tint = mc.getBlockColors().getColor(state, mc.level, pos, 0);
        float r = tint == -1 ? 1 : (tint >> 16 & 255) / 255f, g = tint == -1 ? 1 : (tint >> 8 & 255) / 255f,
                b = tint == -1 ? 1 : (tint & 255) / 255f;
        int light = LevelRenderer.getLightColor(mc.level, air);
        for (RenderType chunkType : model.getRenderTypes(state, RandomSource.create(state.getSeed(pos)),
                ModelData.EMPTY)) {
            RenderType type = RenderTypeHelper.getEntityRenderType(chunkType, false);
            used.add(type);
            blocks.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(type), state, model, r, g, b,
                    light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, chunkType);
        }
    }

    /** For checks: how many blocks are broken out of the ground now. */
    public static int broken() {
        return (int) SHARDS.values().stream().filter(shard -> shard.out).count();
    }

    /** For checks: whether any block is still waiting to break or mend. */
    public static boolean busy() {
        return !SHARDS.isEmpty();
    }
}
