package com.murimblock.world.structure.site;

import com.murimblock.world.structure.kit.BuildKit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Where a site is drawn: local coordinates (x right, z towards the front, y absolute) turned by the site's rotation and
 * moved to its origin, and clipped to the chunk being generated, so one piece can be built chunk by chunk with the same
 * result. Randomness is a hash of the local position (never a sequence), so every chunk sees the same choices whatever
 * it reads from the world. Optionally records what the checks must verify (rooms to be roofed, footings to be carried).
 */
public final class Canvas implements BuildKit.Sink {
    private final WorldGenLevel level;
    private final BoundingBox clip;
    private final int ox, oz;
    private final Rotation rotation;
    private final long seed;
    private final List<Check> checks;
    /** Live levels only (tests): positions whose shape must be recomputed once the build is done. */
    private final List<BlockPos> shapes;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private BuildKit kit;

    /** A box in world coordinates that a check covers: a room that must be roofed, or a footing that must be carried. */
    public record Check(boolean room, BoundingBox box) { }

    public Canvas(WorldGenLevel level, BoundingBox clip, int ox, int oz, Rotation rotation, long seed, List<Check> checks) {
        this(level, clip, ox, oz, rotation, seed, checks, level instanceof ServerLevel ? new ArrayList<>() : null);
    }

    private Canvas(WorldGenLevel level, BoundingBox clip, int ox, int oz, Rotation rotation, long seed, List<Check> checks, List<BlockPos> shapes) {
        this.level = level;
        this.clip = clip;
        this.ox = ox;
        this.oz = oz;
        this.rotation = rotation;
        this.seed = seed;
        this.checks = checks;
        this.shapes = shapes;
    }

    private static final BoundingBox NOWHERE = new BoundingBox(Integer.MIN_VALUE, 0, Integer.MIN_VALUE, Integer.MIN_VALUE, 0, Integer.MIN_VALUE);

    /** A canvas that touches no block: for its coordinates, and to record a site's checks. */
    public static Canvas dry(int ox, int oz, Rotation rotation, long seed, List<Check> checks) {
        return new Canvas(null, NOWHERE, ox, oz, rotation, seed, checks, null);
    }

    public static Canvas dry(int ox, int oz, Rotation rotation) {
        return dry(ox, oz, rotation, 0, null);
    }

    /** A sub-frame: its origin at local (dx, dz), turned by {@code turn} (its front, local +z, then faces that way). */
    public Canvas frame(int dx, int dz, Rotation turn) {
        return new Canvas(level, clip, wx(dx, dz), wz(dx, dz), rotation.getRotated(turn), mix(seed + dx * 31L + dz * 1013L), checks, shapes);
    }

    public BuildKit kit() {
        if (kit == null) kit = new BuildKit(this);
        return kit;
    }

    public WorldGenLevel level() { return level; }

    // ---------------------------------------------------------------- coordinates

    public int wx(int x, int z) {
        return ox + switch (rotation) {
            case NONE -> x;
            case CLOCKWISE_90 -> -z;
            case CLOCKWISE_180 -> -x;
            case COUNTERCLOCKWISE_90 -> z;
        };
    }

    public int wz(int x, int z) {
        return oz + switch (rotation) {
            case NONE -> z;
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> -z;
            case COUNTERCLOCKWISE_90 -> -x;
        };
    }

    /** True if this local column is in the chunk being generated (so it can be read and written). */
    public boolean inside(int x, int z) {
        if (clip == null) return true;
        int X = wx(x, z), Z = wz(x, z);
        return X >= clip.minX() && X <= clip.maxX() && Z >= clip.minZ() && Z <= clip.maxZ();
    }

    // ---------------------------------------------------------------- blocks

    @Override public void set(int x, int y, int z, BlockState state) {
        if (!inside(x, z)) return;
        BlockState s = rotation == Rotation.NONE ? state : state.rotate(rotation);
        pos.set(wx(x, z), y, wz(x, z));
        level.setBlock(pos, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        // Fences, walls, panes and stairs join their neighbours once everything is placed.
        Block b = s.getBlock();
        if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock) {
            if (shapes != null) shapes.add(pos.immutable());
            else level.getChunk(pos).markPosForPostprocessing(pos);
        }
    }

    public BlockState get(int x, int y, int z) {
        return level.getBlockState(pos.set(wx(x, z), y, wz(x, z)));
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) set(x, y, z, state);
    }

    /** Natural ground a footing can rest on: solid, not a plant, a tree or a liquid. */
    public static boolean ground(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && s.isSolid() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS);
    }

    /** Fills from {@code top} downwards with {@code state} until natural ground (through air, water, plants). */
    public void footing(int x, int top, int z, BlockState state) {
        if (!inside(x, z)) return;
        for (int y = top, n = 0; n < 64 && y > level.getMinBuildHeight(); y--, n++) {
            if (ground(get(x, y, z))) return;
            set(x, y, z, state);
        }
    }

    /** Live levels (tests): joins fences, walls and stairs to their neighbours, as world generation does. */
    public void finish() {
        if (shapes == null) return;
        for (BlockPos p : shapes) {
            BlockState s = level.getBlockState(p);
            BlockState joined = Block.updateFromNeighbourShapes(s, level, p);
            if (joined != s) level.setBlock(p, joined, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        shapes.clear();
    }

    // ---------------------------------------------------------------- randomness

    /** A number in [0, 1) fixed by the position (and the site): the same in every chunk. */
    public double rand(int x, int y, int z) {
        long h = mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L));
        return (h >>> 11) * 0x1.0p-53;
    }

    public boolean chance(int x, int y, int z, double p) {
        return rand(x, y, z) < p;
    }

    @SafeVarargs
    public final <T> T pick(int x, int y, int z, T... choices) {
        return choices[(int) (rand(x, y, z) * choices.length)];
    }

    private static long mix(long h) {
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    // ---------------------------------------------------------------- checks

    /** Records an interior (inclusive local box) that must have a roof over every column. */
    public void room(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (checks != null) checks.add(new Check(true, box(x0, y0, z0, x1, y1, z1)));
    }

    /** Records a layer (local rectangle at y) that must be solid and stand on something solid. */
    public void carried(int x0, int z0, int x1, int z1, int y) {
        if (checks != null) checks.add(new Check(false, box(x0, y, z0, x1, y, z1)));
    }

    public BoundingBox box(int x0, int y0, int z0, int x1, int y1, int z1) {
        int ax = wx(x0, z0), az = wz(x0, z0), bx = wx(x1, z1), bz = wz(x1, z1);
        return new BoundingBox(Math.min(ax, bx), Math.min(y0, y1), Math.min(az, bz), Math.max(ax, bx), Math.max(y0, y1), Math.max(az, bz));
    }
}
