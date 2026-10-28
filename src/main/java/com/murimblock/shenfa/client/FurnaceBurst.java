package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Creeper;
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
 * The Fourneau d'alchimie bursting into its own pieces of bronze (storyboard: docs/mobs/boards/furnace_burst.png):
 * when a swollen creeper leaves the client's world without dying, it breaks into the lid, the knob, twelve thick
 * shards of its walls, the handles, the seal and the legs, which fly out spinning, fall, bounce with a clang, slide and
 * smoke, then fade after 5 s; bronze shards, embers and steam burst with them. Client side only: no collision with
 * anyone, no loot.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FurnaceBurst {
    private static final int LIFE = 100, FADE = 30;
    private static final float PIXEL = 1 / 16f, GRAVITY = 0.04f;
    /** The vessel's middle in model pixels: pieces fly away from it. */
    private static final Vector3f CENTRE = new Vector3f(0, 17, 0);
    /** The pieces, built in place on the vessel (tools/blender/mobs/furnace_debris.py). */
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
            "mobs/furnace_debris");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
            "textures/entity/furnace_debris.png");
    private static final List<Piece> PIECES = new ArrayList<>();
    /** The model's parts sorted into pieces, built once the model is loaded. */
    private static Map<String, List<SkinnedMesh>> shapes;
    private static SkinnedModel shapesOf;

    private FurnaceBurst() {
    }

    private static final class Piece {
        final List<SkinnedMesh> meshes;
        final Vector3f centre, min, max, scale;
        final Vector3f thin;
        Vec3 pos, previous, velocity;
        final Quaternionf orientation, previousOrientation = new Quaternionf();
        Vector3f spin;
        float spinSpeed;
        int age, bounces;
        boolean resting;

        Piece(List<SkinnedMesh> meshes, Vector3f centre, Vector3f min, Vector3f max, Vector3f scale) {
            this.meshes = meshes;
            this.centre = centre;
            this.min = min;
            this.max = max;
            this.scale = scale;
            Vector3f size = new Vector3f(max).sub(min).mul(scale);
            thin = size.x <= size.y && size.x <= size.z ? new Vector3f(1, 0, 0)
                    : size.y <= size.z ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
            orientation = new Quaternionf();
        }
    }

    /** A Fourneau leaving the world swollen (and not dying) has burst. */
    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Creeper creeper && creeper.deathTime == 0
                && creeper.getSwelling(1) > 0.6f && SkeletalMobs.look(creeper) == SkeletalMobs.FOURNEAU) {
            burst(creeper);
        }
    }

    /** Breaks {@code creeper}'s look into pieces where it stands. */
    public static void burst(Creeper creeper) {
        SkinnedModel model = SkeletalModels.get(MODEL);
        if (model == null || !(creeper.level() instanceof ClientLevel level)) {
            return;
        }
        Map<String, List<SkinnedMesh>> pieces = shapes(model);
        RandomSource random = creeper.getRandom();
        float scale = creeper.getScale();
        // A little swollen, as at the end of its fuse.
        Vector3f swollen = new Vector3f(1.15f, 1.05f, 1.15f).mul(scale);
        Quaternionf yaw = Axis.YP.rotationDegrees(-creeper.yBodyRot);
        Vec3 at = creeper.position();
        for (Map.Entry<String, List<SkinnedMesh>> entry : pieces.entrySet()) {
            Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
            for (SkinnedMesh mesh : entry.getValue()) {
                float[] p = mesh.positions();
                for (int i : mesh.indices()) {
                    min.min(new Vector3f(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]));
                    max.max(new Vector3f(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]));
                }
            }
            Vector3f centre = new Vector3f(min).add(max).mul(0.5f);
            Piece piece = new Piece(entry.getValue(), centre, new Vector3f(min).sub(centre),
                    new Vector3f(max).sub(centre), swollen);
            Vector3f offset = yaw.transform(new Vector3f(centre).mul(swollen).mul(PIXEL));
            piece.pos = piece.previous = at.add(offset.x, offset.y, offset.z);
            Vector3f out = yaw.transform(new Vector3f(centre).sub(CENTRE).mul(1, 0, 1));
            if (out.lengthSquared() < 1e-4f) {
                out.set(random.nextFloat() - 0.5f, 0, random.nextFloat() - 0.5f);
            }
            out.normalize();
            String name = entry.getKey();
            float speed = 0.07f + random.nextFloat() * 0.07f;
            float up = name.equals("knob") ? 0.6f : name.equals("lid") ? 0.5f
                    : name.startsWith("leg") ? 0.16f + random.nextFloat() * 0.08f : 0.24f + random.nextFloat() * 0.16f;
            piece.velocity = new Vec3(out.x * speed, up, out.z * speed);
            piece.orientation.set(yaw);
            piece.spin = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f,
                    random.nextFloat() - 0.5f).normalize();
            piece.spinSpeed = (12 + random.nextFloat() * 18) * Mth.DEG_TO_RAD;
            PIECES.add(piece);
        }
        sparks(level, creeper, at.add(0, 1.1 * scale, 0), random);
    }

    /** The bronze shards, embers and steam bursting with the pieces (fewer with fewer particles). */
    private static void sparks(ClientLevel level, Creeper creeper, Vec3 at, RandomSource random) {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        int share = status == ParticleStatus.ALL ? 1 : status == ParticleStatus.DECREASED ? 2 : 4;
        BlockParticleOption bronze = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CUT_COPPER.defaultBlockState());
        BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RAW_GOLD_BLOCK.defaultBlockState());
        for (int i = 0; i < 24 / share; i++) {
            level.addParticle(i % 3 == 0 ? gold : bronze, at.x, at.y, at.z, (random.nextFloat() - 0.5f) * 1.2f,
                    0.3f + random.nextFloat() * 0.6f, (random.nextFloat() - 0.5f) * 1.2f);
        }
        for (int i = 0; i < 20 / share; i++) {
            level.addParticle(ParticleTypes.LAVA, at.x, at.y, at.z, 0, 0, 0);
        }
        for (int i = 0; i < 10 / share; i++) {
            level.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, (random.nextFloat() - 0.5f) * 0.4f,
                    random.nextFloat() * 0.3f, (random.nextFloat() - 0.5f) * 0.4f);
        }
        for (int i = 0; i < 18 / share; i++) {
            level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, (random.nextFloat() - 0.5f) * 0.5f,
                    0.05f + random.nextFloat() * 0.2f, (random.nextFloat() - 0.5f) * 0.5f);
        }
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 1.9f, false);
    }

    /** The debris model's parts sorted into pieces: a part named "<piece>__<part>" belongs to that piece. */
    private static Map<String, List<SkinnedMesh>> shapes(SkinnedModel model) {
        if (shapes != null && shapesOf == model) {
            return shapes;
        }
        Map<String, List<SkinnedMesh>> pieces = new LinkedHashMap<>();
        for (SkinnedMesh mesh : model.parts()) {
            int cut = mesh.name().indexOf("__");
            pieces.computeIfAbsent(cut < 0 ? mesh.name() : mesh.name().substring(0, cut), key -> new ArrayList<>())
                    .add(mesh);
        }
        shapes = pieces;
        shapesOf = model;
        return pieces;
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
        for (Iterator<Piece> it = PIECES.iterator(); it.hasNext(); ) {
            Piece piece = it.next();
            if (++piece.age > LIFE) {
                it.remove();
                continue;
            }
            move(mc.level, piece);
        }
    }

    private static void move(ClientLevel level, Piece piece) {
        piece.previous = piece.pos;
        piece.previousOrientation.set(piece.orientation);
        if (!piece.resting) {
            piece.velocity = piece.velocity.add(0, -GRAVITY, 0).scale(0.98);
            piece.pos = piece.pos.add(piece.velocity);
            piece.orientation.premul(new Quaternionf().rotateAxis(piece.spinSpeed, piece.spin));
        }
        // Lands on whatever block is under its lowest corner.
        float low = lowest(piece);
        BlockPos below = BlockPos.containing(piece.pos.x, piece.pos.y + low, piece.pos.z);
        VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (!shape.isEmpty()) {
            double top = below.getY() + shape.max(Direction.Axis.Y);
            if (piece.pos.y + low < top) {
                piece.pos = new Vec3(piece.pos.x, top - low, piece.pos.z);
                if (piece.velocity.y < -0.1 && piece.bounces < 2) {
                    if (piece.bounces++ == 0 && piece.meshes.size() > 1) {
                        level.playLocalSound(piece.pos.x, piece.pos.y, piece.pos.z, SoundEvents.ANVIL_LAND,
                                SoundSource.HOSTILE, 0.08f, 1.7f + level.random.nextFloat() * 0.5f, false);
                    }
                    piece.velocity = new Vec3(piece.velocity.x * 0.55, -piece.velocity.y * 0.32,
                            piece.velocity.z * 0.55);
                    piece.spinSpeed *= 0.45f;
                } else {
                    // Slides to a stop and settles flat on its thinnest side.
                    piece.velocity = new Vec3(piece.velocity.x * 0.7, 0, piece.velocity.z * 0.7);
                    piece.spinSpeed = 0;
                    Vector3f thin = piece.orientation.transform(new Vector3f(piece.thin));
                    Vector3f up = new Vector3f(0, Math.signum(thin.y) == 0 ? 1 : Math.signum(thin.y), 0);
                    Quaternionf settle = new Quaternionf().rotationTo(thin, up);
                    piece.orientation.premul(new Quaternionf().slerp(settle, 0.35f));
                    piece.resting = piece.velocity.lengthSqr() < 1e-4 && Math.abs(thin.y) > 0.995f;
                }
            }
        }
        if (piece.resting && piece.age < 80 && level.random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, piece.pos.x, piece.pos.y + 0.05, piece.pos.z, 0, 0.03, 0);
        }
    }

    /** How far below its centre the piece's lowest corner is now (blocks, negative). */
    private static float lowest(Piece piece) {
        float low = 0;
        for (int corner = 0; corner < 8; corner++) {
            Vector3f c = new Vector3f((corner & 1) == 0 ? piece.min.x : piece.max.x,
                    (corner & 2) == 0 ? piece.min.y : piece.max.y, (corner & 4) == 0 ? piece.min.z : piece.max.z);
            low = Math.min(low, piece.orientation.transform(c.mul(piece.scale).mul(PIXEL)).y);
        }
        return low;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || PIECES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SkinnedModel model = SkeletalModels.get(MODEL);
        if (mc.level == null || model == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(type);
        Matrix4f[] rest = new Matrix4f[model.armature().joints().size()];
        for (int i = 0; i < rest.length; i++) {
            rest[i] = new Matrix4f();
        }
        PoseStack poseStack = event.getPoseStack();
        for (Piece piece : PIECES) {
            float age = piece.age + partialTick;
            int alpha = (int) (255 * Mth.clamp((LIFE - age) / FADE, 0, 1));
            Vec3 at = piece.previous.lerp(piece.pos, partialTick).subtract(camera);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(new Quaternionf(piece.previousOrientation).slerp(piece.orientation, partialTick));
            poseStack.scale(piece.scale.x * PIXEL, piece.scale.y * PIXEL, piece.scale.z * PIXEL);
            poseStack.translate(-piece.centre.x, -piece.centre.y, -piece.centre.z);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(piece.pos.x, piece.pos.y + 0.2,
                    piece.pos.z));
            for (SkinnedMesh mesh : piece.meshes) {
                SkeletalPlayerRenderer.draw(mesh, rest, poseStack.last(), consumer, alpha << 24 | 0xFFFFFF, light,
                        OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }

    /** For checks: how many pieces are flying or lying about. */
    public static int pieces() {
        return PIECES.size();
    }
}
