package com.murimblock.martial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Iceberg (15/10/2026, Enzo's request), the Sanguine Art's last technique: an arena of blood crystal, 45 blocks
 * across, rises out of the ground under the player (Enzo chose the arena over the first mountain): a round floor 3 blocks
 * up with a glowing seal of blood on it, tiers stepping up round it to a crenellated wall with eight great spires, and an
 * outer slope down to the ground. Whoever stands where it rises is carried up onto it. In it the player has Strength and
 * Regeneration, and a blood crystal grows back every 3 seconds (a dorsal crystal in the
 * form, a point of the gauge outside it). After 60 seconds it crumbles away from the top down.
 * <p>
 * It only fills air (and grass, flowers, snow: what a block may replace), never breaks a block. Each of its blocks
 * removes itself by a scheduled tick (saved with the chunk), so nothing is left behind even if the server stops.
 */
public final class BloodIceberg {
    /**
     * Half its width (45 blocks across); the floor's height over the player's feet and its radius; the wall's height over
     * the floor; the spires' over the wall; the highest it reaches; how deep it fills down a slope.
     */
    public static final int RADIUS = 22, FLOOR = 3, ARENA = 16, WALL = 6, SPIRE = 6, TOP = FLOOR + WALL + SPIRE, DEPTH = 12;
    /** It rises after the palms strike the ground, in a second and a half; it stands 60 s; it crumbles over a few seconds. */
    public static final int RISE_DELAY = 6, RISE_TICKS = 30, LIFE_TICKS = 1200;
    /** On it: the effects refreshed every second; a blood crystal every 3 seconds. */
    public static final int EFFECT_EVERY = 20, CRYSTAL_EVERY = 60;

    record Spot(BlockPos pos, boolean vein) {
    }

    static final class Peak {
        final ResourceKey<Level> level;
        final BlockPos centre;
        final List<Spot> spots;
        final long start, ends;
        final int maxY;
        int placed;

        Peak(ResourceKey<Level> level, BlockPos centre, List<Spot> spots, long start) {
            this.level = level; this.centre = centre; this.spots = spots; this.start = start;
            ends = start + RISE_TICKS + LIFE_TICKS;
            maxY = spots.isEmpty() ? centre.getY() : spots.getLast().pos().getY();
        }
    }

    private static final Map<UUID, Peak> PEAKS = new HashMap<>();

    private BloodIceberg() {
    }

    /** The arena, column by column (RADIUS * 2 square): heights over the player's feet (-1 outside), and which tops glow. */
    public record Shape(int[][] heights, boolean[][] seal) {
    }

    /**
     * The arena: the floor (FLOOR high) out to ARENA; two tiers (+2, +4); the wall (+WALL), crenellated, with eight
     * spires (+SPIRE at their tip) turned by the seed; the outer slope down to the ground. On the floor, the seal: two
     * rings and eight spokes between them toward the spires, and the middle.
     */
    public static Shape shape(long seed) {
        int size = RADIUS * 2;
        int[][] h = new int[size][size];
        boolean[][] seal = new boolean[size][size];
        double turn = new Random(seed).nextDouble() * Math.PI / 4, step = Math.PI / 4;
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
            double x = i - RADIUS + .5, z = j - RADIUS + .5, d = Math.hypot(x, z), a = Math.atan2(z, x);
            // How far round from the nearest spire's line (as an arc, in blocks at this distance).
            double off = ((a - turn) % step + step) % step, arc = Math.min(off, step - off) * d;
            int v;
            if (d > RADIUS + .5) v = -1;
            else if (d <= ARENA) {
                v = FLOOR;
                seal[i][j] = Math.abs(d - 6) < .55 || Math.abs(d - 12) < .55 || d > 6 && d < 12 && arc < .5 || d < 1.5;
            } else if (d <= ARENA + 1.5) v = FLOOR + 2;
            else if (d <= ARENA + 3) v = FLOOR + 4;
            else if (d <= RADIUS - 1) {
                v = FLOOR + WALL + ((int) Math.floor((a + Math.PI) / (Math.PI / 20)) % 2 == 0 ? 1 : 0);
                if (arc < 1.8) { v = Math.max(v, FLOOR + WALL + (int) Math.round(SPIRE * (1 - arc / 1.8))); seal[i][j] = arc < .9; }
            } else v = Math.max(1, (int) Math.round(FLOOR + WALL - (d - (RADIUS - 1)) * 4));
            h[i][j] = v;
        }
        return new Shape(h, seal);
    }

    /** The technique: the arena is laid out under the player; it starts to rise once the palms strike the ground. */
    public static boolean raise(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos feet = p.blockPosition();
        long now = level.getGameTime(), seed = now * 31 + p.getUUID().getLeastSignificantBits();
        Shape shape = shape(seed);
        int[][] h = shape.heights();
        Random random = new Random(seed ^ 0x5EED);
        List<Spot> spots = new ArrayList<>();
        for (int i = 0; i < h.length; i++) for (int j = 0; j < h.length; j++) {
            if (h[i][j] < 0) continue;
            int x = feet.getX() + i - RADIUS, z = feet.getZ() + j - RADIUS, top = feet.getY() + h[i][j] - 1;
            // From its top down to the ground (or DEPTH under the feet, down a cliff).
            for (int y = top; y >= feet.getY() - DEPTH; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!fillable(level.getBlockState(pos))) break;
                spots.add(new Spot(pos, y == top ? shape.seal()[i][j] : random.nextFloat() < .08F));
            }
        }
        spots.sort(Comparator.comparingInt(s -> s.pos().getY()));
        PEAKS.put(p.getUUID(), new Peak(level.dimension(), feet, spots, now + RISE_DELAY));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, .5F);
        return true;
    }

    static boolean fillable(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    @Nullable static Peak peak(ServerPlayer p) { return PEAKS.get(p.getUUID()); }

    /** Every tick, for each player: their arena rising, then its gifts while they are in it. */
    static void tick(ServerPlayer p) {
        Peak k = PEAKS.get(p.getUUID());
        if (k == null) return;
        long now = p.level().getGameTime();
        if (now >= k.ends + 60) { PEAKS.remove(p.getUUID()); return; }
        if (!p.level().dimension().equals(k.level)) return;
        ServerLevel level = p.serverLevel();
        if (now >= k.start && k.placed < k.spots.size()) rise(level, k, now, p);
        if (now >= k.start + RISE_TICKS && now < k.ends && on(p, k)) {
            long t = now - k.start - RISE_TICKS;
            if (t % EFFECT_EVERY == 0) {
                // No particles: the Sanguine Art shows none.
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_EVERY + 40, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_EVERY + 40, 0, true, false, true));
            }
            if (t > 0 && t % CRYSTAL_EVERY == 0) SanguineArt.icebergCrystal(p);
        }
    }

    /** This tick's layers, from the ground up; whoever stands where the crystal grows is carried up on it. */
    private static void rise(ServerLevel level, Peak k, long now, ServerPlayer owner) {
        int lowest = k.spots.getFirst().pos().getY();
        double f = Math.min(1, (now - k.start + 1) / (double) RISE_TICKS);
        int upTo = (int) Math.floor(lowest + (k.maxY - lowest + 1) * f);
        var block = SanguineBlocks.BLOOD_CRYSTAL.get();
        var vein = SanguineBlocks.BLOOD_CRYSTAL_VEIN.get();
        Random random = new Random(now);
        while (k.placed < k.spots.size() && k.spots.get(k.placed).pos().getY() <= upTo) {
            Spot s = k.spots.get(k.placed++);
            if (!fillable(level.getBlockState(s.pos()))) continue;
            var b = s.vein() ? vein : block;
            // Shown to clients without updating the neighbours: thousands of blocks in a second and a half.
            level.setBlock(s.pos(), b.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            // It crumbles away from the top down once its 60 s are up.
            level.scheduleTick(s.pos(), b, (int) (k.ends - now) + (k.maxY - s.pos().getY()) * 3 + random.nextInt(8));
        }
        AABB area = new AABB(k.centre).inflate(RADIUS + 1, DEPTH + TOP + 4, RADIUS + 1);
        lift(level, owner);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != owner)) lift(level, e);
        if ((now - k.start) % 4 == 0) {
            level.playSound(null, k.centre.getX() + .5, k.centre.getY(), k.centre.getZ() + .5, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.8F, (float) (.45 + f * .4));
            level.playSound(null, k.centre.getX() + .5, k.centre.getY(), k.centre.getZ() + .5, SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.4F, .5F);
        }
        if (k.placed >= k.spots.size())
            level.playSound(null, k.centre.getX() + .5, k.maxY, k.centre.getZ() + .5, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2F, .5F);
    }

    /** Carried up: someone the crystal grew into stands on its top. */
    private static void lift(ServerLevel level, LivingEntity e) {
        BlockPos at = e.blockPosition(), up = at;
        while (level.getBlockState(up).getBlock() instanceof BloodIcebergBlock) up = up.above();
        if (up.equals(at)) return;
        e.teleportTo(e.getX(), up.getY(), e.getZ());
        e.resetFallDistance();
        e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
    }

    /** Whether the player is in (or on, or at the foot of) their arena. */
    static boolean on(ServerPlayer p, Peak k) {
        double dx = Math.abs(p.getX() - (k.centre.getX() + .5)), dz = Math.abs(p.getZ() - (k.centre.getZ() + .5));
        return dx <= RADIUS + .5 && dz <= RADIUS + .5 && p.getY() >= k.centre.getY() - DEPTH - 2 && p.getY() <= k.maxY + 8;
    }

    static void forget(@Nullable UUID id) {
        if (id == null) PEAKS.clear(); else PEAKS.remove(id);
    }
}
