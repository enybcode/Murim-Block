package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The crater the Bandit Warlord's fall leaves in a bandit camp (Enzo, 16/10/2026: "it changes the bandit village into a
 * crater, for PvP"; docs/structures/bandit-camp.md): for good, the camp's arena from then on.
 * <ul>
 *   <li>a bowl {@link #RADIUS} blocks round the plaza's centre and {@link #DEPTH} deep, everything that stood in it gone;
 *   its floor scorched rock at the heart (blackstone, basalt, a few embers of magma), stone and gravel, then earth up the
 *   slopes;</li>
 *   <li>a rim of thrown earth and rubble, two or three blocks high, {@link #RIM} wide;</li>
 *   <li>round it the camp in ruins, the nearer the more broken, the high parts first (roofs blown off, walls down); the
 *   palisade burst outward, logs thrown flat beyond it; rubble everywhere.</li>
 * </ul>
 * Nothing that holds things is ever broken (the chief's chest in the longhall, the loot barrels), nor anything under the
 * camp's ground outside the bowl. The pattern comes from the camp's place: the same crater for everyone. Carved over
 * several ticks ({@link #BUDGET} blocks a tick): no freeze, the warlord's landing covers it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class WarlordCrater {
    public static final int RADIUS = 24, DEPTH = 10, RIM = 4, HEIGHT = 30;
    /** The ruins reach the palisade and its ditch. */
    static final int RUINS = BanditCamp.R + 3;
    /** Block changes a tick. */
    static final int BUDGET = 6000;

    private record Change(BlockPos pos, BlockState state) { }
    private record Job(ServerLevel level, Deque<Change> changes) { }

    private static final List<Job> JOBS = new ArrayList<>();

    private WarlordCrater() { }

    /** The bowl's floor at this distance from the centre, for a camp whose ground is {@code g}. */
    public static int floor(int g, double d) {
        return (int) Math.round(g - DEPTH * (1 - (d / RADIUS) * (d / RADIUS)));
    }

    /** Opens the crater of the camp whose centre is {@code centre} (its plaza's ground at {@code g}). */
    public static void open(ServerLevel level, BlockPos centre, int g) {
        JOBS.add(new Job(level, plan(level, centre.getX(), centre.getZ(), g)));
    }

    /** Opens it now, whole (tests). */
    public static void openNow(ServerLevel level, BlockPos centre, int g) {
        for (Change c : plan(level, centre.getX(), centre.getZ(), g)) level.setBlock(c.pos, c.state, 2);
    }

    /** Every change the crater makes, in order: the bowl first, then its rim, the ruins, the thrown logs and rubble. */
    private static Deque<Change> plan(ServerLevel level, int cx, int cz, int g) {
        Deque<Change> out = new ArrayDeque<>();
        RandomSource r = RandomSource.create(BanditCampCount.camp(cx, cz) * 31 + 0x5EED);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -RUINS; x <= RUINS; x++) for (int z = -RUINS; z <= RUINS; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > RUINS) continue;
            int wx = cx + x, wz = cz + z;
            if (d < RADIUS) {
                // The bowl: everything above its floor gone; the floor darker and more broken the deeper.
                // Never through the world's floor (a shallow world): the bowl stops above it.
                int h = Math.max(floor(g, d), level.getMinBuildHeight() + 4);
                for (int y = h + 1; y <= g + HEIGHT; y++) clear(level, p.set(wx, y, wz), out, air);
                double deep = 1 - d / RADIUS;
                for (int y = h - 3; y <= h; y++) {
                    BlockState s = y < h ? Blocks.STONE.defaultBlockState()
                            : deep > 0.75 ? pick(r, 0.08, Blocks.MAGMA_BLOCK, 0.5, Blocks.BLACKSTONE, Blocks.BASALT)
                            : deep > 0.4 ? pick(r, 0.5, Blocks.STONE, 0.6, Blocks.GRAVEL, Blocks.COBBLESTONE)
                            : pick(r, 0.5, Blocks.COARSE_DIRT, 0.6, Blocks.DIRT, Blocks.GRAVEL);
                    if (keeps(level, p.set(wx, y, wz))) continue;
                    out.add(new Change(p.immutable(), s));
                }
            } else if (d < RADIUS + RIM) {
                // The rim: thrown earth and rubble raised two or three blocks; little left standing on it.
                int up = (int) Math.round(3 * (1 - (d - RADIUS) / RIM) + r.nextDouble() * 1.2);
                for (int y = g + 1; y <= g + up; y++) {
                    if (keeps(level, p.set(wx, y, wz))) continue;
                    out.add(new Change(p.immutable(), pick(r, 0.5, Blocks.DIRT, 0.6, Blocks.COBBLESTONE, Blocks.GRAVEL)));
                }
                for (int y = g + up + 1; y <= g + HEIGHT; y++) if (r.nextDouble() < 0.85) clear(level, p.set(wx, y, wz), out, air);
            } else {
                // The ruins: the nearer, the more blown away, the high parts first.
                double blast = Math.max(0.35, 1 - (d - RADIUS - RIM) / (RUINS - RADIUS - RIM));
                for (int y = g + 1; y <= g + HEIGHT; y++) {
                    double high = (y - g) / 12.0;
                    if (r.nextDouble() < blast * (0.55 + high)) clear(level, p.set(wx, y, wz), out, air);
                }
            }
        }
        // The palisade burst outward: logs thrown flat beyond it.
        for (int k = 0; k < 40; k++) {
            double a = r.nextDouble() * Math.PI * 2, d = BanditCamp.R + 3 + r.nextDouble() * 9;
            int x = cx + (int) Math.round(Math.sin(a) * d), z = cz + (int) Math.round(Math.cos(a) * d), len = 3 + r.nextInt(4);
            boolean along = r.nextBoolean();
            BlockState log = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along ? Direction.Axis.X : Direction.Axis.Z);
            for (int i = 0; i < len; i++) {
                int lx = along ? x + i : x, lz = along ? z : z + i;
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                if (Math.abs(top - (g + 1)) <= 3) out.add(new Change(new BlockPos(lx, top, lz), log));
            }
        }
        // Rubble thrown across the rim and the ruins.
        for (int k = 0; k < 70; k++) {
            double a = r.nextDouble() * Math.PI * 2, d = RADIUS + r.nextDouble() * 20;
            int x = cx + (int) Math.round(Math.sin(a) * d), z = cz + (int) Math.round(Math.cos(a) * d);
            out.add(new Change(new BlockPos(x, g + 1 + (d < RADIUS + RIM ? 3 : 0), z), pick(r, 0.6, Blocks.COBBLESTONE, 1, Blocks.DIRT, Blocks.DIRT)));
        }
        return out;
    }

    private static void clear(ServerLevel level, BlockPos p, Deque<Change> out, BlockState air) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || keeps(level, p)) return;
        out.add(new Change(p.immutable(), s.getFluidState().isEmpty() ? air : s.getFluidState().createLegacyBlock()));
    }

    /** What the crater never breaks: whatever holds things (chests, barrels), and what nothing can break (bedrock). */
    static boolean keeps(ServerLevel level, BlockPos p) {
        return level.getBlockEntity(p) instanceof Container || level.getBlockState(p).getDestroySpeed(level, p) < 0;
    }

    private static BlockState pick(RandomSource r, double first, net.minecraft.world.level.block.Block a, double second,
                                   net.minecraft.world.level.block.Block b, net.minecraft.world.level.block.Block c) {
        return (r.nextDouble() < first ? a : r.nextDouble() < second ? b : c).defaultBlockState();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) return;
        Job job = JOBS.get(0);
        for (int n = 0; n < BUDGET && !job.changes.isEmpty(); n++) {
            Change c = job.changes.poll();
            if (job.level.isLoaded(c.pos) && !keeps(job.level, c.pos)) job.level.setBlock(c.pos, c.state, 2);
        }
        if (job.changes.isEmpty()) JOBS.remove(0);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JOBS.clear();
    }

    /** Tests: whether a crater is still being carved. */
    public static boolean carving() {
        return !JOBS.isEmpty();
    }
}
