package com.murimblock.world.structure.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * Shared tools for the Murim structures built in code (docs/structures): block placement in world coordinates or
 * relative to the structure's origin (x and z only; y is always absolute), stair and slab states, hanging lanterns and
 * the Chinese roof. Each structure keeps its own layout and palette; the kit only does what they all need.
 */
public final class BuildKit {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Where the kit's blocks go: a live level, or a structure piece being generated (with its own frame). */
    public interface Sink {
        void set(int x, int y, int z, BlockState state);
    }

    private final ServerLevel level;
    private final Sink sink;
    private final int originX, originZ;

    public BuildKit(ServerLevel level, int originX, int originZ) {
        this.level = level;
        this.sink = (x, y, z, state) -> level.setBlock(new BlockPos(x, y, z), state, FLAGS);
        this.originX = originX;
        this.originZ = originZ;
    }

    /** A kit writing to a sink in its own coordinates (origin 0, 0). */
    public BuildKit(Sink sink) {
        this.level = null;
        this.sink = sink;
        this.originX = 0;
        this.originZ = 0;
    }

    /** The live level, or null for a kit writing to a sink. */
    public ServerLevel level() { return level; }

    // ---------------------------------------------------------------- placing blocks

    /** Sets a block in world coordinates, without neighbour updates (structures are placed whole). */
    public void set(int x, int y, int z, BlockState state) {
        sink.set(x, y, z, state);
    }

    /** Sets a block relative to the origin. */
    public void setRel(int dx, int y, int dz, BlockState state) {
        set(originX + dx, y, originZ + dz, state);
    }

    /** Fills a box relative to the origin (corners in any order). */
    public void fillRel(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) setRel(x, y, z, state);
    }

    // ---------------------------------------------------------------- block states

    public static BlockState stair(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    public static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    /** A lantern hanging from the block above. */
    public static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    // ---------------------------------------------------------------- roofs

    /** {@link #roof} relative to the origin. */
    public void roofRel(int x0, int z0, int x1, int z1, int y, RoofTiles tiles, boolean pyramid) {
        roof(originX + x0, originZ + z0, originX + x1, originZ + z1, y, tiles, pyramid);
    }

    /**
     * Chinese roof over a rectangle of walls (world coordinates), its eaves course at {@code y}: eaves two blocks out as
     * slabs round a solid ceiling (so no gap shows under the roof), flying corners lifted one block, then stair courses
     * climbing inward (a hip roof, or a pyramid on a square) to a solid ridge with raised ends, or a finial on a pyramid.
     */
    public void roof(int x0, int z0, int x1, int z1, int y, RoofTiles tiles, boolean pyramid) {
        Block stairs = tiles.stairs(), slabs = tiles.slab(), ridge = tiles.ridge();
        x0 -= 2; z0 -= 2; x1 += 2; z1 += 2;
        // Eave course: bottom slabs (the flat lower curve of the roof) round a solid ceiling, so no gap shows under it.
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            set(x, y, z, x == x0 || x == x1 || z == z0 || z == z1 ? slab(slabs, SlabType.BOTTOM) : ridge.defaultBlockState());
        // Flying eaves at the four corners.
        set(x0, y + 1, z0, stair(stairs, Direction.SOUTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
        set(x1, y + 1, z0, stair(stairs, Direction.SOUTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
        set(x0, y + 1, z1, stair(stairs, Direction.NORTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
        set(x1, y + 1, z1, stair(stairs, Direction.NORTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
        int ax0 = x0 + 1, az0 = z0 + 1, ax1 = x1 - 1, az1 = z1 - 1;
        int yy = y + 1;
        while (ax1 - ax0 >= (pyramid ? 0 : 1) && az1 - az0 >= 0) {
            for (int x = ax0; x <= ax1; x++) for (int z = az0; z <= az1; z++) {
                boolean n = z == az0, s = z == az1, w = x == ax0, e = x == ax1;
                if (!(n || s || w || e)) { set(x, yy, z, ridge.defaultBlockState()); continue; }
                Direction facing = n ? Direction.SOUTH : s ? Direction.NORTH : w ? Direction.EAST : Direction.WEST;
                BlockState st = stair(stairs, facing, Half.BOTTOM);
                if ((n || s) && (w || e)) st = st.setValue(StairBlock.SHAPE, (n == w) ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT);
                if (ax1 - ax0 <= 1 || az1 - az0 <= 1) st = ridge.defaultBlockState(); // the ridge line itself
                set(x, yy, z, st);
            }
            ax0++; az0++; ax1--; az1--; yy++;
        }
        // Ridge ornaments: raised ends on the long axis.
        if (!pyramid) {
            boolean alongX = (x1 - x0) >= (z1 - z0);
            int midZ = (z0 + z1) / 2, midX = (x0 + x1) / 2;
            int ry = yy - 1;
            if (alongX) {
                int inset = (z1 - z0) / 2;
                set(x0 + inset, ry + 1, midZ, ridge.defaultBlockState());
                set(x1 - inset, ry + 1, midZ, ridge.defaultBlockState());
            } else {
                int inset = (x1 - x0) / 2;
                set(midX, ry + 1, z0 + inset, ridge.defaultBlockState());
                set(midX, ry + 1, z1 - inset, ridge.defaultBlockState());
            }
        } else {
            set((x0 + x1) / 2, yy, (z0 + z1) / 2, ridge.defaultBlockState());
            set((x0 + x1) / 2, yy + 1, (z0 + z1) / 2, Blocks.LIGHTNING_ROD.defaultBlockState());
        }
    }

    /**
     * Two-slope roof of a house over walls x0..x1, z0..z1 (ridge along x), its lowest course at {@code y}: stair courses
     * one block out all round, a solid ceiling at {@code y} over the walls (so nothing shows under the roof), the gable
     * ends walled with {@code gable} up to the ridge, and a ridge of full tiles whose ends rise one block. An odd number
     * of rows (z1 - z0 odd) gives a single ridge row.
     */
    public void gableRoof(int x0, int z0, int x1, int z1, int y, RoofTiles tiles, BlockState ceiling, BlockState gable) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) set(x, y, z, ceiling);
        int a = z0 - 1, b = z1 + 1, k = 0;
        while (a + k <= b - k) {
            int n = a + k, s = b - k, h = y + k;
            boolean ridgeRow = s - n <= 1;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (ridgeRow) {
                    set(x, h, n, tiles.ridge().defaultBlockState());
                    set(x, h, s, tiles.ridge().defaultBlockState());
                } else {
                    set(x, h, n, stair(tiles.stairs(), Direction.SOUTH, Half.BOTTOM));
                    set(x, h, s, stair(tiles.stairs(), Direction.NORTH, Half.BOTTOM));
                }
            }
            // Gable ends: the wall triangle under this course.
            if (k > 0) for (int z = n + 1; z < s; z++) { set(x0, h, z, gable); set(x1, h, z, gable); }
            if (ridgeRow) {
                for (int z = n; z <= s; z++) { set(x0 - 1, h + 1, z, tiles.ridge().defaultBlockState()); set(x1 + 1, h + 1, z, tiles.ridge().defaultBlockState()); }
                break;
            }
            k++;
        }
    }

    /**
     * Single-slope roof (a lean-to) over walls x0..x1, z0..z1, high at the back (z0) and low at the front: eaves one block
     * out at the front and sides, a solid ceiling at {@code y}, the side and back walls closed with {@code gable} under it.
     */
    public void shedRoof(int x0, int z0, int x1, int z1, int y, RoofTiles tiles, BlockState ceiling, BlockState gable) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) set(x, y, z, ceiling);
        for (int i = 0, z = z1 + 1; z >= z0; i++, z--) {
            int h = y + i;
            for (int x = x0 - 1; x <= x1 + 1; x++) set(x, h, z, stair(tiles.stairs(), Direction.NORTH, Half.BOTTOM));
            if (z <= z1) for (int yy = y + 1; yy < h; yy++) { set(x0, yy, z, gable); set(x1, yy, z, gable); }
            if (z == z0) for (int x = x0 + 1; x < x1; x++) for (int yy = y + 1; yy < h; yy++) set(x, yy, z, gable);
        }
    }
}
