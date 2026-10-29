package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Skinning;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Esprit de l'Archer's death (storyboard: docs/mobs/boards/esprit_death.png): struck, it rises (its clip,
 * {@code archer/spirit_death}) while its mist comes undone from the bottom up in snowflakes ({@link #cut}); its heart
 * bursts in frost, then its armour, empty, falls from where it floated: the hood, the pauldron, the lamellae, the belt,
 * the bracer, the quiver and the bow, which ring once on the ground and fade after 5 s. Client side only: the loot is
 * vanilla's.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SpiritRemains {
    /** The death tick the heart bursts on, and the one the armour is let go on. */
    public static final int BURST = 12, RELEASE = 14;
    private static final int LIFE = 100, FADE = 30;
    private static final float PIXEL = 1 / 16f, GRAVITY = 0.04f;
    private static final List<Piece> PIECES = new ArrayList<>();
    private static final Set<Mob> RELEASED = Collections.newSetFromMap(new WeakHashMap<>());

    private SpiritRemains() {
    }

    private static final class Piece {
        /** The piece as it hung at the release, in model pixels around its centre (or null: an item). */
        final float[] positions, normals, uvs;
        final ItemStack item;
        final float scale;
        /** Its box around the centre, in blocks. */
        final Vector3f min, max, thin;
        final boolean cloth;
        Vec3 pos, previous, velocity;
        final Quaternionf orientation = new Quaternionf(), previousOrientation = new Quaternionf();
        Vector3f spin;
        float spinSpeed;
        int age, bounces;
        boolean resting;

        Piece(float[] positions, float[] normals, float[] uvs, ItemStack item, float scale, Vector3f min,
              Vector3f max, boolean cloth) {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.item = item;
            this.scale = scale;
            this.min = min;
            this.max = max;
            this.cloth = cloth;
            Vector3f size = new Vector3f(max).sub(min);
            thin = size.x <= size.y && size.x <= size.z ? new Vector3f(1, 0, 0)
                    : size.y <= size.z ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
        }
    }

    /** The armour piece a part of the Esprit's model belongs to (tools/blender/mobs/esprit.py), or null. */
    static String piece(String part) {
        if (part.startsWith("hood_") || part.startsWith("icicle_brow") || part.equals("mantle")) {
            return "hood";
        }
        if (part.startsWith("pauldron") || part.startsWith("icicle_pauldron")) {
            return "pauldron";
        }
        if (part.equals("belt") || part.equals("buckle")) {
            return "belt";
        }
        if (part.equals("quiver") || part.startsWith("fletch")) {
            return "quiver";
        }
        return part.startsWith("lamella") || part.startsWith("bracer") ? part : null;
    }

    /** Whether a part of the Esprit's model is its mist, which comes undone as it dies. */
    static boolean mist(String part) {
        return part.startsWith("mist_") || part.startsWith("tail_");
    }

    /** How high the dying Esprit's mist has come undone (model pixels): below everything until it starts to. */
    static float cut(Mob mob, float partialTick) {
        return mob.deathTime == 0 ? -Float.MAX_VALUE : (mob.deathTime + partialTick - 3) * 4.5f - 4;
    }

    /** Each tick of the Esprit's death: the crack of the blow, the flakes where the mist comes undone, the burst. */
    static void dying(Mob mob) {
        Level level = mob.level();
        RandomSource random = mob.getRandom();
        int time = mob.deathTime;
        float scale = mob.getScale();
        if (time == 1) {
            level.playLocalSound(mob.getX(), mob.getEyeY(), mob.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE,
                    0.35f, 1.5f, false);
        }
        if (time >= 3 && time < BURST + 1) {
            double y = mob.getY() + Math.max(0, cut(mob, 0)) * PIXEL * scale + 0.1;
            for (int i = 0; i < share(3); i++) {
                level.addParticle(ParticleTypes.SNOWFLAKE, mob.getX() + (random.nextFloat() - 0.5f) * 0.5, y,
                        mob.getZ() + (random.nextFloat() - 0.5f) * 0.5, (random.nextFloat() - 0.5f) * 0.06,
                        0.06 + random.nextFloat() * 0.06, (random.nextFloat() - 0.5f) * 0.06);
            }
        }
        if (time == BURST) {
            // The heart bursts in frost.
            double y = mob.getY() + 1.25 * scale;
            for (int i = 0; i < share(20); i++) {
                Vec3 out = new Vec3(random.nextFloat() - 0.5, random.nextFloat() - 0.3, random.nextFloat() - 0.5)
                        .normalize().scale(0.12 + random.nextFloat() * 0.08);
                level.addParticle(ParticleTypes.SNOWFLAKE, mob.getX(), y, mob.getZ(), out.x, out.y, out.z);
            }
            level.playLocalSound(mob.getX(), y, mob.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE,
                    0.7f, 1.4f, false);
        }
    }

    private static int share(int count) {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.ALL ? count : status == ParticleStatus.DECREASED ? count / 2 : count / 4;
    }

    /** Whether the dying {@code mob} has let its armour go (and is drawn no more). */
    static boolean released(Mob mob) {
        return RELEASED.contains(mob);
    }

    /**
     * Lets the dying Esprit's armour go, each piece as it hangs in its last pose ({@code skin}, {@code joints}) on the
     * mob turned by {@code bodyYaw} and grown by {@code scale}.
     */
    static void release(Mob mob, SkinnedModel model, Matrix4f[] skin, Matrix4f[] joints, float scale, float bodyYaw,
                        float partialTick) {
        RELEASED.add(mob);
        RandomSource random = mob.getRandom();
        Quaternionf yaw = Axis.YP.rotationDegrees(-bodyYaw);
        Vec3 at = mob.getPosition(partialTick);
        Map<String, List<SkinnedMesh>> pieces = new LinkedHashMap<>();
        for (SkinnedMesh mesh : model.parts()) {
            String name = piece(mesh.name());
            if (name != null) {
                pieces.computeIfAbsent(name, key -> new ArrayList<>()).add(mesh);
            }
        }
        for (Map.Entry<String, List<SkinnedMesh>> entry : pieces.entrySet()) {
            Piece piece = bake(entry.getValue(), skin, scale, entry.getKey().equals("hood"));
            Vector3f centre = centre(entry.getValue(), skin);
            Vector3f offset = yaw.transform(new Vector3f(centre).mul(scale * PIXEL));
            launch(piece, at.add(offset.x, offset.y, offset.z), yaw, random);
        }
        ItemStack bow = mob.getMainHandItem();
        if (!bow.isEmpty()) {
            Vector3f hand = joints[model.armature().get("Tool_R").index()].getTranslation(new Vector3f());
            Vector3f offset = yaw.transform(hand.mul(scale * PIXEL));
            Piece piece = new Piece(null, null, null, bow.copy(), scale, new Vector3f(-0.3f, -0.3f, -0.03f).mul(scale),
                    new Vector3f(0.3f, 0.3f, 0.03f).mul(scale), false);
            launch(piece, at.add(offset.x, offset.y, offset.z), new Quaternionf(yaw).mul(Axis.YP.rotationDegrees(90)),
                    random);
        }
    }

    private static void launch(Piece piece, Vec3 at, Quaternionf orientation, RandomSource random) {
        piece.pos = piece.previous = at;
        // They drop from where they floated, barely pushed apart.
        piece.velocity = new Vec3((random.nextFloat() - 0.5f) * 0.08, 0.02 + random.nextFloat() * 0.05,
                (random.nextFloat() - 0.5f) * 0.08);
        piece.orientation.set(orientation);
        piece.previousOrientation.set(orientation);
        piece.spin = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f)
                .normalize();
        piece.spinSpeed = (piece.cloth ? 3 : 5 + random.nextFloat() * 8) * Mth.DEG_TO_RAD;
        PIECES.add(piece);
    }

    private static Vector3f centre(List<SkinnedMesh> meshes, Matrix4f[] skin) {
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
        for (SkinnedMesh mesh : meshes) {
            float[] p = skinned(mesh, skin)[0];
            for (int i = 0; i < mesh.vertexCount(); i++) {
                min.min(new Vector3f(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]));
                max.max(new Vector3f(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]));
            }
        }
        return min.add(max).mul(0.5f);
    }

    private static float[][] skinned(SkinnedMesh mesh, Matrix4f[] skin) {
        float[] positions = new float[mesh.vertexCount() * 3], normals = new float[mesh.vertexCount() * 3];
        Skinning.skin(mesh, skin, positions, normals);
        return new float[][] {positions, normals};
    }

    /** The meshes' triangles as they hang now, around their centre, joined into one piece. */
    private static Piece bake(List<SkinnedMesh> meshes, Matrix4f[] skin, float scale, boolean cloth) {
        Vector3f centre = centre(meshes, skin);
        int count = 0;
        for (SkinnedMesh mesh : meshes) {
            count += mesh.indices().length;
        }
        float[] positions = new float[count * 3], normals = new float[count * 3], uvs = new float[count * 2];
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
        int n = 0;
        for (SkinnedMesh mesh : meshes) {
            float[][] p = skinned(mesh, skin);
            for (int v : mesh.indices()) {
                for (int axis = 0; axis < 3; axis++) {
                    positions[n * 3 + axis] = p[0][v * 3 + axis] - centre.get(axis);
                    normals[n * 3 + axis] = p[1][v * 3 + axis];
                }
                uvs[n * 2] = mesh.uvs()[v * 2];
                uvs[n * 2 + 1] = mesh.uvs()[v * 2 + 1];
                Vector3f at = new Vector3f(positions[n * 3], positions[n * 3 + 1], positions[n * 3 + 2]);
                min.min(at);
                max.max(at);
                n++;
            }
        }
        return new Piece(positions, normals, uvs, null, scale, min.mul(scale * PIXEL), max.mul(scale * PIXEL), cloth);
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
            // The empty hood drifts down slower than the metal.
            piece.velocity = piece.velocity.add(0, piece.cloth ? -GRAVITY * 0.5 : -GRAVITY, 0)
                    .scale(piece.cloth ? 0.92 : 0.98);
            piece.pos = piece.pos.add(piece.velocity);
            piece.orientation.premul(new Quaternionf().rotateAxis(piece.spinSpeed, piece.spin));
        }
        float low = lowest(piece);
        BlockPos below = BlockPos.containing(piece.pos.x, piece.pos.y + low, piece.pos.z);
        VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
        if (!shape.isEmpty()) {
            double top = below.getY() + shape.max(Direction.Axis.Y);
            if (piece.pos.y + low < top) {
                piece.pos = new Vec3(piece.pos.x, top - low, piece.pos.z);
                if (piece.velocity.y < -0.1 && piece.bounces < 1 && !piece.cloth) {
                    piece.bounces++;
                    level.playLocalSound(piece.pos.x, piece.pos.y, piece.pos.z, SoundEvents.ANVIL_LAND,
                            SoundSource.HOSTILE, 0.05f, 2.2f + level.random.nextFloat() * 0.4f, false);
                    piece.velocity = new Vec3(piece.velocity.x * 0.5, -piece.velocity.y * 0.3, piece.velocity.z * 0.5);
                    piece.spinSpeed *= 0.4f;
                } else {
                    // Settles flat on its thinnest side.
                    piece.velocity = new Vec3(piece.velocity.x * 0.6, 0, piece.velocity.z * 0.6);
                    piece.spinSpeed = 0;
                    Vector3f thin = piece.orientation.transform(new Vector3f(piece.thin));
                    Vector3f up = new Vector3f(0, Math.signum(thin.y) == 0 ? 1 : Math.signum(thin.y), 0);
                    Quaternionf settle = new Quaternionf().rotationTo(thin, up);
                    piece.orientation.premul(new Quaternionf().slerp(settle, 0.35f));
                    piece.resting = piece.velocity.lengthSqr() < 1e-4 && Math.abs(thin.y) > 0.995f;
                }
            }
        }
        // The last wisp of mist rising from the hood.
        if (piece.cloth && piece.resting && piece.age > 20 && piece.age < 50 && level.random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, piece.pos.x, piece.pos.y + 0.1, piece.pos.z, 0, 0.05, 0);
        }
    }

    /** How far below its centre the piece's lowest corner is now (blocks, negative). */
    private static float lowest(Piece piece) {
        float low = 0;
        for (int corner = 0; corner < 8; corner++) {
            Vector3f c = new Vector3f((corner & 1) == 0 ? piece.min.x : piece.max.x,
                    (corner & 2) == 0 ? piece.min.y : piece.max.y, (corner & 4) == 0 ? piece.min.z : piece.max.z);
            low = Math.min(low, piece.orientation.transform(c).y);
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
        RenderType type = RenderType.entityTranslucent(SkeletalMobs.ESPRIT.texture());
        PoseStack poseStack = event.getPoseStack();
        for (Piece piece : PIECES) {
            float fade = Mth.clamp((LIFE - piece.age - partialTick) / FADE, 0, 1);
            Vec3 at = piece.previous.lerp(piece.pos, partialTick).subtract(camera);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(piece.pos.x, piece.pos.y + 0.2,
                    piece.pos.z));
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(new Quaternionf(piece.previousOrientation).slerp(piece.orientation, partialTick));
            if (piece.item != null) {
                // An item cannot fade: it shrinks away instead.
                poseStack.scale(piece.scale * fade, piece.scale * fade, piece.scale * fade);
                mc.getItemRenderer().renderStatic(piece.item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                        poseStack, buffers, mc.level, 0);
            } else {
                poseStack.scale(piece.scale * PIXEL, piece.scale * PIXEL, piece.scale * PIXEL);
                draw(piece, poseStack.last(), buffers.getBuffer(type), (int) (255 * fade) << 24 | 0xFFFFFF, light);
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void draw(Piece piece, PoseStack.Pose matrix, VertexConsumer consumer, int color, int light) {
        float[] p = piece.positions, n = piece.normals, uv = piece.uvs;
        for (int t = 0; t < p.length / 3; t += 3) {
            // Entity render types draw quads: each triangle is sent as a quad with its last corner twice.
            for (int corner = 0; corner < 4; corner++) {
                int v = t + Math.min(corner, 2);
                consumer.addVertex(matrix, p[v * 3], p[v * 3 + 1], p[v * 3 + 2])
                        .setColor(color)
                        .setUv(uv[v * 2], uv[v * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(matrix, n[v * 3], n[v * 3 + 1], n[v * 3 + 2]);
            }
        }
    }

    /** For checks: how many pieces are falling or lying about. */
    public static int pieces() {
        return PIECES.size();
    }
}
