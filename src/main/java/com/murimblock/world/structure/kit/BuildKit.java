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

    private final ServerLevel level;
    private final int originX, originZ;

    public BuildKit(ServerLevel level, int originX, int originZ) {
        this.level = level;
        this.originX = originX;
        this.originZ = originZ;
    }

    public ServerLevel level() { return level; }

    // ---------------------------------------------------------------- placing blocks

    /** Sets a block in world coordinates, without neighbour updates (structures are placed whole). */
    public void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, FLAGS);
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
}
