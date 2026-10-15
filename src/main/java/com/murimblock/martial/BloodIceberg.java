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
 * Blood Iceberg (15/10/2026, Enzo's request), the Sanguine Art's last technique: a mountain of blood crystal, 30 blocks
 * across, rises out of the ground under the player and lifts them to its peak, an iceberg bristling with spires. On it
 * the player has Strength and Regeneration, and a blood crystal grows back every 3 seconds (a dorsal crystal in the
 * form, a point of the gauge outside it). After 60 seconds it crumbles away from the top down.
 * <p>
 * It only fills air (and grass, flowers, snow: what a block may replace), never breaks a block. Each of its blocks
 * removes itself by a scheduled tick (saved with the chunk), so nothing is left behind even if the server stops.
 */
public final class BloodIceberg {
    /** Half its width (30 blocks across), its peak's height over the player's feet, how deep it fills down a slope. */
    public static final int RADIUS = 15, PEAK = 9, DEPTH = 12;
    /** It rises after the palms strike the ground, in a second; it stands 60 s; it crumbles over a few seconds. */
    public static final int RISE_DELAY = 6, RISE_TICKS = 20, LIFE_TICKS = 1200;
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

    /**
     * The mountain's heights over the player's feet, column by column (30 x 30, -1 outside it): a broad cone to the peak
     * (PEAK) over the middle, rough at its flanks, and spires of crystal standing out of it, none on the peak itself.
     */
    public static int[][] heights(long seed) {
        Random random = new Random(seed);
        int size = RADIUS * 2;
        double[][] h = new double[size][size];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
            double x = i - RADIUS + .5, z = j - RADIUS + .5;
            // A rounded square, so it fills most of the 30 x 30.
            double d = Math.cbrt(Math.pow(Math.abs(x), 3) + Math.pow(Math.abs(z), 3)) / RADIUS;
            h[i][j] = d > 1 ? -1 : Math.max(1, PEAK * Math.pow(1 - d, 1.15) + random.nextDouble() * 1.6);
        }
        // The middle stays the peak, flat enough to stand on.
        for (int i = RADIUS - 1; i <= RADIUS; i++) for (int j = RADIUS - 1; j <= RADIUS; j++) h[i][j] = PEAK;
        for (int s = 0; s < 12; s++) {
            double angle = random.nextDouble() * Math.PI * 2, reach = 4 + random.nextDouble() * 8.5;
            double cx = RADIUS + Math.cos(angle) * reach, cz = RADIUS + Math.sin(angle) * reach;
            double rise = 3 + random.nextDouble() * 4.5;
            for (int i = (int) cx - 2; i <= (int) cx + 2; i++) for (int j = (int) cz - 2; j <= (int) cz + 2; j++) {
                if (i < 0 || j < 0 || i >= size || j >= size || h[i][j] < 0) continue;
                double dist = Math.hypot(i + .5 - cx, j + .5 - cz);
                if (dist < 1.8) h[i][j] = Math.max(h[i][j], h[(int) cx][(int) cz] + rise * (1 - dist / 1.8));
            }
        }
        int[][] out = new int[size][size];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) out[i][j] = h[i][j] < 0 ? -1 : (int) Math.round(h[i][j]);
        return out;
    }

    /** The technique: the mountain is laid out under the player; it starts to rise once the palms strike the ground. */
    public static boolean raise(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos feet = p.blockPosition();
        long now = level.getGameTime(), seed = now * 31 + p.getUUID().getLeastSignificantBits();
        int[][] h = heights(seed);
        Random random = new Random(seed ^ 0x5EED);
        List<Spot> spots = new ArrayList<>();
        for (int i = 0; i < h.length; i++) for (int j = 0; j < h.length; j++) {
            if (h[i][j] < 0) continue;
            int x = feet.getX() + i - RADIUS, z = feet.getZ() + j - RADIUS, top = feet.getY() + h[i][j] - 1;
            // From its top down to the ground (or DEPTH under the feet, down a cliff).
            for (int y = top; y >= feet.getY() - DEPTH; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!fillable(level.getBlockState(pos))) break;
                spots.add(new Spot(pos, random.nextFloat() < .12F || y == top && h[i][j] > PEAK));
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

    /** Every tick, for each player: their mountain rising, then its gifts while they stand on it. */
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
            level.setBlock(s.pos(), b.defaultBlockState(), 3);
            // It crumbles away from the top down once its 60 s are up.
            level.scheduleTick(s.pos(), b, (int) (k.ends - now) + (k.maxY - s.pos().getY()) * 3 + random.nextInt(8));
        }
        AABB area = new AABB(k.centre).inflate(RADIUS + 1, DEPTH + PEAK + 12, RADIUS + 1);
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

    /** Whether the player is on (or in the air over, or at the foot of) their mountain. */
    static boolean on(ServerPlayer p, Peak k) {
        double dx = Math.abs(p.getX() - (k.centre.getX() + .5)), dz = Math.abs(p.getZ() - (k.centre.getZ() + .5));
        return dx <= RADIUS + .5 && dz <= RADIUS + .5 && p.getY() >= k.centre.getY() - DEPTH - 2 && p.getY() <= k.maxY + 8;
    }

    static void forget(@Nullable UUID id) {
        if (id == null) PEAKS.clear(); else PEAKS.remove(id);
    }
}
