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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Iceberg (15/10/2026, Enzo's request), the Sanguine Art's last technique: a temple arena of blood, 45 blocks
 * across, rises out of the ground under the player. In the "blood temple" style Enzo chose (red lacquered stone and
 * gold): a round floor of polished stone 3 blocks up with a gold seal inlaid in it, a channel of blood running round it,
 * two tiers of bricks, a crenellated wall with lanterns set in it, eight spires of gold crystal crowned with lanterns,
 * and between the spires eight falls of blood pouring from the wall's top down the tiers into the channel; an outer slope
 * of rough stone down to the ground. Whoever stands where it rises is carried up onto it. In it the player has Strength
 * and Regeneration, and a blood crystal grows back every 3 seconds (a dorsal crystal in the form, a point of the gauge
 * outside it). After 60 seconds the blood runs dry and the arena crumbles away from the top down.
 * <p>
 * It only fills air (and grass, flowers, snow: what a block may replace), never breaks a block. Each of its blocks, and
 * each of its springs of blood, removes itself by a scheduled tick (saved with the chunk), so nothing is left behind even
 * if the server stops; the flowing blood dries up once its springs are gone.
 * <p>
 * Its stone cannot be broken (by hand, explosion or piston), nothing can be built or poured in it while it stands, and
 * it collapses at once if its caster dies (15/10/2026, Enzo's rules).
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
    /** The blood's springs run dry 10 s before the arena starts to crumble, so the thick blood has drained away by then. */
    static final int DRY_BEFORE = 200;

    /** What a column is: the floor, the channel of blood, a tier, the wall, a spire, a fall of blood, the outer slope. */
    public enum Part { FLOOR, CHANNEL, TIER, WALL, SPIRE, FALL, SLOPE }

    /** What a block of the arena is made of: the blood temple's six stones, or a spring of blood. */
    enum Kind { ROCK, BRICKS, POLISHED, SEAL, GOLD, LANTERN, BLOOD }

    record Spot(BlockPos pos, Kind kind) {
    }

    static final class Peak {
        final ResourceKey<Level> level;
        final BlockPos centre;
        final List<Spot> spots, springs;
        final long start, ends;
        final int maxY;
        int placed;
        boolean flowing;
        /** Collapsing (its caster died): how many of its blocks, from the top, have been taken away. */
        boolean collapsing;
        int removed;

        Peak(ResourceKey<Level> level, BlockPos centre, List<Spot> spots, List<Spot> springs, long start) {
            this.level = level; this.centre = centre; this.spots = spots; this.springs = springs; this.start = start;
            ends = start + RISE_TICKS + LIFE_TICKS;
            maxY = spots.isEmpty() ? centre.getY() : spots.getLast().pos().getY();
        }
    }

    private static final Map<UUID, Peak> PEAKS = new HashMap<>();

    private BloodIceberg() {
    }

    /**
     * The arena, column by column (RADIUS * 2 square): heights over the player's feet (-1 outside), what each column is,
     * which floor tops are the seal, which wall columns hold a lantern.
     */
    public record Shape(int[][] heights, Part[][] parts, boolean[][] seal, boolean[][] lantern) {
    }

    /**
     * The arena: the floor (FLOOR high) out to the channel, one lower, at its rim; two tiers (+2, +4); the wall (+WALL),
     * crenellated, with eight spires (+SPIRE at their tip) turned by the seed, a notch for a fall of blood midway between
     * each two; the outer slope down to the ground. On the floor, the seal: two rings and eight spokes between them
     * toward the spires, and the middle.
     */
    public static Shape shape(long seed) {
        int size = RADIUS * 2;
        int[][] h = new int[size][size];
        Part[][] parts = new Part[size][size];
        boolean[][] seal = new boolean[size][size], lantern = new boolean[size][size];
        double turn = new Random(seed).nextDouble() * Math.PI / 4, step = Math.PI / 4;
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
            double x = i - RADIUS + .5, z = j - RADIUS + .5, d = Math.hypot(x, z), a = Math.atan2(z, x);
            // How far round from the nearest spire's line, and from the midway line between two (arcs, in blocks here).
            double off = ((a - turn) % step + step) % step, arc = Math.min(off, step - off) * d, mid = Math.abs(off - step / 2) * d;
            int v; Part part;
            if (d > RADIUS + .5) { v = -1; part = null; }
            else if (d <= ARENA - 1.2) {
                v = FLOOR; part = Part.FLOOR;
                seal[i][j] = Math.abs(d - 6) < .55 || Math.abs(d - 12) < .55 || d > 6 && d < 12 && arc < .5 || d < 1.5;
            } else if (d <= ARENA) { v = FLOOR - 1; part = Part.CHANNEL; }
            else if (d <= ARENA + 1.5) { v = FLOOR + 2; part = Part.TIER; }
            else if (d <= ARENA + 3) { v = FLOOR + 4; part = Part.TIER; }
            else if (d <= RADIUS - 1) {
                v = FLOOR + WALL + ((int) Math.floor((a + Math.PI) / (Math.PI / 20)) % 2 == 0 ? 1 : 0); part = Part.WALL;
                if (arc < 1.8) { v = Math.max(v, FLOOR + WALL + (int) Math.round(SPIRE * (1 - arc / 1.8))); part = Part.SPIRE; }
                else if (mid < .6 && d <= ARENA + 4.2) { v = FLOOR + WALL - 1; part = Part.FALL; }
                else if (d <= ARENA + 4 && (int) Math.floor((a + Math.PI) / (Math.PI / 24)) % 3 == 0) lantern[i][j] = true;
            } else { v = Math.max(1, (int) Math.round(FLOOR + WALL - (d - (RADIUS - 1)) * 4)); part = Part.SLOPE; }
            h[i][j] = v; parts[i][j] = part;
        }
        return new Shape(h, parts, seal, lantern);
    }

    /** What the block of a column is, `up` blocks over the player's feet (its top is `top`). */
    static Kind kind(Shape shape, int i, int j, int up, int top) {
        if (up < 0) return Kind.ROCK;
        return switch (shape.parts()[i][j]) {
            case FLOOR -> up == top ? (shape.seal()[i][j] ? Kind.SEAL : Kind.POLISHED) : Kind.ROCK;
            case CHANNEL, TIER -> Kind.BRICKS;
            case WALL -> up == top ? Kind.POLISHED : shape.lantern()[i][j] && up == FLOOR + 4 ? Kind.LANTERN : Kind.BRICKS;
            case FALL -> up == top ? Kind.POLISHED : Kind.BRICKS;
            case SPIRE -> up == top ? Kind.LANTERN : up >= FLOOR + WALL - 1 ? Kind.GOLD : Kind.BRICKS;
            case SLOPE -> Kind.ROCK;
        };
    }

    static Block block(Kind kind) {
        return switch (kind) {
            case ROCK -> SanguineBlocks.TEMPLE_ROCK.get();
            case BRICKS -> SanguineBlocks.TEMPLE_BRICKS.get();
            case POLISHED -> SanguineBlocks.TEMPLE_POLISHED.get();
            case SEAL -> SanguineBlocks.TEMPLE_SEAL.get();
            case GOLD -> SanguineBlocks.TEMPLE_GOLD.get();
            case LANTERN -> SanguineBlocks.TEMPLE_LANTERN.get();
            case BLOOD -> SanguineBlocks.BLOOD.get();
        };
    }

    /** The technique: the arena is laid out under the player; it starts to rise once the palms strike the ground. */
    public static boolean raise(LivingEntity p) {
        ServerLevel level = (ServerLevel)p.level();
        BlockPos feet = p.blockPosition();
        long now = level.getGameTime(), seed = now * 31 + p.getUUID().getLeastSignificantBits();
        Shape shape = shape(seed);
        int[][] h = shape.heights();
        List<Spot> spots = new ArrayList<>(), springs = new ArrayList<>();
        for (int i = 0; i < h.length; i++) for (int j = 0; j < h.length; j++) {
            if (h[i][j] < 0) continue;
            int x = feet.getX() + i - RADIUS, z = feet.getZ() + j - RADIUS, top = h[i][j] - 1;
            // From its top down to the ground (or DEPTH under the feet, down a cliff).
            for (int up = top; up >= -DEPTH; up--) {
                BlockPos pos = new BlockPos(x, feet.getY() + up, z);
                if (!fillable(level.getBlockState(pos))) break;
                spots.add(new Spot(pos, kind(shape, i, j, up, top)));
            }
            // Blood: the channel full of it, a spring in each notch of the wall to pour down the tiers.
            Part part = shape.parts()[i][j];
            if (part == Part.CHANNEL || part == Part.FALL) springs.add(new Spot(new BlockPos(x, feet.getY() + top + 1, z), Kind.BLOOD));
        }
        spots.sort(Comparator.comparingInt(s -> s.pos().getY()));
        PEAKS.put(p.getUUID(), new Peak(level.dimension(), feet, spots, springs, now + RISE_DELAY));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, .5F);
        return true;
    }

    static boolean fillable(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    @Nullable static Peak peak(ServerPlayer p) { return PEAKS.get(p.getUUID()); }

    /** Every tick, for each player: their arena rising, then its gifts while they are in it. */
    public static void tick(LivingEntity p) {
        Peak k = PEAKS.get(p.getUUID());
        if (k == null || k.collapsing) return;
        long now = p.level().getGameTime();
        if (now >= k.ends + 60) { PEAKS.remove(p.getUUID()); return; }
        if (!p.level().dimension().equals(k.level)) return;
        ServerLevel level = (ServerLevel)p.level();
        if (now >= k.start && k.placed < k.spots.size()) rise(level, k, now, p);
        if (!k.flowing && k.placed >= k.spots.size()) flow(level, k, now);
        if (now >= k.start + RISE_TICKS && now < k.ends && on(p, k)) {
            long t = now - k.start - RISE_TICKS;
            if (t % EFFECT_EVERY == 0) {
                // No particles: the Sanguine Art shows none.
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_EVERY + 40, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_EVERY + 40, 0, true, false, true));
            }
            if (t > 0 && t % CRYSTAL_EVERY == 0) {
                if (p instanceof ServerPlayer player) SanguineArt.icebergCrystal(player);
                else if (p instanceof com.murimblock.mob.ScarletGuardianEntity guardian) guardian.replenishCrystal();
            }
        }
    }

    /** This tick's layers, from the ground up; whoever stands where the stone grows is carried up on it. */
    private static void rise(ServerLevel level, Peak k, long now, LivingEntity owner) {
        int lowest = k.spots.getFirst().pos().getY();
        double f = Math.min(1, (now - k.start + 1) / (double) RISE_TICKS);
        int upTo = (int) Math.floor(lowest + (k.maxY - lowest + 1) * f);
        Random random = new Random(now);
        while (k.placed < k.spots.size() && k.spots.get(k.placed).pos().getY() <= upTo) {
            Spot s = k.spots.get(k.placed++);
            if (!fillable(level.getBlockState(s.pos()))) continue;
            Block b = block(s.kind());
            // Shown to clients without updating the neighbours: thousands of blocks in a second and a half.
            level.setBlock(s.pos(), b.defaultBlockState(), Block.UPDATE_CLIENTS);
            // It crumbles away from the top down once its 60 s are up.
            level.scheduleTick(s.pos(), b, (int) (k.ends - now) + (k.maxY - s.pos().getY()) * 3 + random.nextInt(8));
        }
        AABB area = new AABB(k.centre).inflate(RADIUS + 1, DEPTH + TOP + 4, RADIUS + 1);
        lift(level, owner);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != owner)) lift(level, e);
        if ((now - k.start) % 4 == 0) {
            level.playSound(null, k.centre.getX() + .5, k.centre.getY(), k.centre.getZ() + .5, SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.8F, (float) (.45 + f * .4));
            level.playSound(null, k.centre.getX() + .5, k.centre.getY(), k.centre.getZ() + .5, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.4F, .5F);
        }
        if (k.placed >= k.spots.size())
            level.playSound(null, k.centre.getX() + .5, k.maxY, k.centre.getZ() + .5, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, .5F);
    }

    /** Once it stands, the blood: the channel fills and the falls start to pour; the springs run dry before it crumbles. */
    private static void flow(ServerLevel level, Peak k, long now) {
        k.flowing = true;
        Block blood = block(Kind.BLOOD);
        int dry = (int) Math.max(20, k.ends - DRY_BEFORE - now);
        for (Spot s : k.springs) {
            if (!level.getBlockState(s.pos()).isAir()) continue;
            level.setBlock(s.pos(), blood.defaultBlockState(), Block.UPDATE_ALL);
            level.scheduleTick(s.pos(), blood, dry);
        }
        level.playSound(null, k.centre.getX() + .5, k.centre.getY() + FLOOR, k.centre.getZ() + .5, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.6F, .6F);
    }

    /** Carried up: someone the stone grew into stands on its top. */
    private static void lift(ServerLevel level, LivingEntity e) {
        BlockPos at = e.blockPosition(), up = at;
        while (level.getBlockState(up).getBlock() instanceof BloodIcebergBlock) up = up.above();
        if (up.equals(at)) return;
        e.teleportTo(e.getX(), up.getY(), e.getZ());
        e.resetFallDistance();
        e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
    }

    /** Whether a position is in an arena that stands (its footprint, from under the ground to over its spires). */
    public static boolean inArena(Level level, BlockPos pos) {
        long now = level.getGameTime();
        for (Peak k : PEAKS.values()) {
            if (k.collapsing || now >= k.ends || !k.level.equals(level.dimension())) continue;
            if (Math.abs(pos.getX() - k.centre.getX()) <= RADIUS + 1 && Math.abs(pos.getZ() - k.centre.getZ()) <= RADIUS + 1
                    && pos.getY() >= k.centre.getY() - DEPTH - 1 && pos.getY() <= k.maxY + 4) return true;
        }
        return false;
    }

    /** Its caster died: the arena collapses at once (BloodIceberg.collapseTick takes it down, top first, in half a second). */
    public static void collapse(LivingEntity p) {
        Peak k = PEAKS.get(p.getUUID());
        if (k == null || k.collapsing) return;
        k.collapsing = true;
        ServerLevel level = p.level().getServer().getLevel(k.level);
        if (level != null) level.playSound(null, k.centre.getX() + .5, k.centre.getY() + FLOOR, k.centre.getZ() + .5,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, .6F);
    }

    /** Each server tick: collapsing arenas come down, their blood first, then their stone from the top, in 10 ticks. */
    public static void collapseTick(net.minecraft.server.MinecraftServer server) {
        for (var it = PEAKS.values().iterator(); it.hasNext(); ) {
            Peak k = it.next();
            if (!k.collapsing) continue;
            ServerLevel level = server.getLevel(k.level);
            if (level == null) { it.remove(); continue; }
            if (k.removed == 0) for (Spot s : k.springs) if (level.getBlockState(s.pos()).is(block(Kind.BLOOD))) level.setBlock(s.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            int batch = Math.max(1, k.placed / 10 + 1);
            for (int n = 0; n < batch && k.removed < k.placed; n++) {
                Spot s = k.spots.get(k.placed - 1 - k.removed++);
                if (level.getBlockState(s.pos()).getBlock() instanceof BloodIcebergBlock)
                    level.setBlock(s.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            if (k.removed >= k.placed) it.remove();
        }
    }

    /** Whether the player is in (or on, or at the foot of) their arena. */
    static boolean on(LivingEntity p, Peak k) {
        double dx = Math.abs(p.getX() - (k.centre.getX() + .5)), dz = Math.abs(p.getZ() - (k.centre.getZ() + .5));
        return dx <= RADIUS + .5 && dz <= RADIUS + .5 && p.getY() >= k.centre.getY() - DEPTH - 2 && p.getY() <= k.maxY + 8;
    }

    static void forget(@Nullable UUID id) {
        if (id == null) PEAKS.clear(); else PEAKS.remove(id);
    }
}
