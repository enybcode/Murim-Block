package com.murimblock.world.structure.site;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A Murim site (docs/structures): its parts (one structure piece each; the terrain is shaped under each part at its
 * level), how it chooses its levels from the terrain, and how it builds a part. Local coordinates: x to the right,
 * z towards the front (the entrance side, turned towards the lowest ground), y absolute.
 */
public abstract class Site {
    /** One piece: a local rectangle (all its blocks inside), standing on {@code levels[level]}, {@code height} blocks tall. */
    public record Part(int x0, int z0, int x1, int z1, int level, int height) { }

    /** What a site can learn of the terrain before it is generated (local coordinates). */
    public interface Probe {
        /** Top solid block of the column (the bottom under water). */
        int ground(int x, int z);
        /** Water over the ground there. */
        boolean wet(int x, int z);
        int seaLevel();
    }

    /** Distance from the origin to the farthest block of the site, in blocks (any rotation). */
    public abstract int reach();

    public abstract List<Part> parts();

    /** Distance from the origin of the terrain samples (the four used to turn the site are among them). */
    public int probe() { return reach() * 3 / 4; }

    /** The levels the parts stand on (the first is the main ground level), or null if this place does not suit. */
    public abstract int[] levels(Probe probe);

    public abstract void build(Canvas c, int part, int[] levels);

    // ---------------------------------------------------------------- choosing a place

    /** Diagnostics (site tour): places refused for water, and for too steep ground. */
    public static final AtomicInteger WET = new AtomicInteger(), STEEP = new AtomicInteger();

    /**
     * A flat site: 3 x 3 samples over a square of half-side {@code half}; refused if the ground varies more than
     * {@code maxSpread} or (unless {@code water}) if any is under water. The level is the median ground.
     */
    protected static int[] flat(Probe p, int half, int maxSpread, boolean water) {
        int[] h = new int[9];
        int i = 0;
        for (int x = -half; x <= half; x += half) for (int z = -half; z <= half; z += half) {
            if (!water && p.wet(x, z)) { WET.incrementAndGet(); return null; }
            h[i++] = p.ground(x, z);
        }
        Arrays.sort(h);
        if (h[8] - h[0] > maxSpread) { STEEP.incrementAndGet(); return null; }
        return new int[]{h[4]};
    }

    // ---------------------------------------------------------------- ground

    /**
     * Clears a rectangle above {@code g} up to {@code clearTop}, lays {@code top} at {@code g} and fills under it with
     * {@code fill} down to natural ground, so the site neither floats nor sits in a hole.
     */
    protected static void prepare(Canvas c, int x0, int z0, int x1, int z1, int g, int clearTop, BlockState top, BlockState fill) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!c.inside(x, z)) continue;
            for (int y = g + 1; y <= clearTop; y++) if (!c.get(x, y, z).isAir()) c.set(x, y, z, AIR);
            c.set(x, g, z, top);
            c.footing(x, g - 1, z, fill);
        }
    }

    /** Like {@link #prepare}, but keeps the natural surface at {@code g} where there is one (its grass, its patches). */
    protected static void prepareNatural(Canvas c, int x0, int z0, int x1, int z1, int g, int clearTop, BlockState fallback, BlockState fill) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!c.inside(x, z)) continue;
            for (int y = g + 1; y <= clearTop; y++) if (!c.get(x, y, z).isAir()) c.set(x, y, z, AIR);
            if (!Canvas.ground(c.get(x, g, z))) c.set(x, g, z, fallback);
            c.footing(x, g - 1, z, fill);
        }
    }

    /** Lays a floor or pavement at y, each block picked by position among {@code choices}. */
    protected static void pave(Canvas c, int x0, int z0, int x1, int z1, int y, BlockState... choices) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) c.set(x, y, z, c.pick(x, y, z, choices));
    }

    // ---------------------------------------------------------------- block states

    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();

    protected static BlockState s(Block b) { return b.defaultBlockState(); }

    protected static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    protected static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** An open trapdoor flat against a window opening: the lattice of Chinese windows. */
    protected static BlockState lattice(Block trapdoor, Direction facing) {
        return trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.OPEN, true);
    }

    protected static BlockState litCandles(int n) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, n).setValue(CandleBlock.LIT, true);
    }

    // ---------------------------------------------------------------- furniture

    protected static void door(Canvas c, int x, int y, int z, Block door, Direction facing) {
        BlockState d = door.defaultBlockState().setValue(DoorBlock.FACING, facing);
        c.set(x, y, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        c.set(x, y + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A bed with its foot at (x, y, z) and its head one block towards {@code toHead}. */
    protected static void bed(Canvas c, int x, int y, int z, Direction toHead, Block bed) {
        BlockState b = bed.defaultBlockState().setValue(BedBlock.FACING, toHead);
        c.set(x, y, z, b.setValue(BedBlock.PART, BedPart.FOOT));
        c.set(x + toHead.getStepX(), y, z + toHead.getStepZ(), b.setValue(BedBlock.PART, BedPart.HEAD));
    }

    /** Two-block plant (lower and upper halves). */
    protected static void tallPlant(Canvas c, int x, int y, int z, Block plant) {
        c.set(x, y, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        c.set(x, y + 1, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A small table: a fence post with a plate on top. */
    protected static void table(Canvas c, int x, int y, int z, Block fence, Block plate) {
        c.set(x, y, z, s(fence));
        c.set(x, y + 1, z, s(plate));
    }

    /** A ladder on the face of the block behind it (the ladder looks towards {@code facing}). */
    protected static void ladder(Canvas c, int x, int y0, int y1, int z, Direction facing) {
        for (int y = y0; y <= y1; y++) c.set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
    }

    protected static void wallBanner(Canvas c, int x, int y, int z, Block banner, Direction facing) {
        c.set(x, y, z, banner.defaultBlockState().setValue(WallBannerBlock.FACING, facing));
    }

    /** A lantern on a post (fence or wall) {@code h} blocks high. */
    protected static void lanternPost(Canvas c, int x, int y, int z, int h, BlockState post) {
        for (int i = 0; i < h; i++) c.set(x, y + i, z, post);
        c.set(x, y + h, z, s(Blocks.LANTERN));
    }

    /**
     * A tree drawn by hand (persistent leaves, so it never decays): a trunk, then a crown of radius {@code r} centred
     * {@code trunk} blocks up, flattened by {@code squash} (under 1) and frayed at its rim.
     */
    protected static void tree(Canvas c, int x, int y, int z, BlockState log, BlockState leaves, int trunk, double r, double squash) {
        int R = (int) Math.ceil(r);
        int cy = y + trunk;
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) for (int dy = -R; dy <= R; dy++)
            if (inCrown(c, x, cy, z, r, squash, x + dx, cy + dy, z + dz)) c.set(x + dx, cy + dy, z + dz, leaves);
        for (int i = 0; i < trunk + 1; i++) c.set(x, y + i, z, log);
    }

    /** True if (x, y, z) holds a leaf of the crown {@link #tree} draws round (cx, cy, cz). */
    protected static boolean inCrown(Canvas c, int cx, int cy, int cz, double r, double squash, int x, int y, int z) {
        double dy = (y - cy) / squash;
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz) + dy * dy);
        return d <= r - 0.9 * c.rand(x, y, z);
    }
}
