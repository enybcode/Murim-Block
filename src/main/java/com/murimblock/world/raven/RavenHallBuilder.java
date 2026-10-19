package com.murimblock.world.raven;

import static com.murimblock.world.raven.RavenCastleFeature.EDGE_X;
import static com.murimblock.world.raven.RavenCastleFeature.EDGE_Z;
import static com.murimblock.world.raven.RavenCastleFeature.FLOOR;
import static com.murimblock.world.raven.RavenCastleFeature.FRAME;
import static com.murimblock.world.raven.RavenCastleFeature.GOLD;
import static com.murimblock.world.raven.RavenCastleFeature.HANGING;
import static com.murimblock.world.raven.RavenCastleFeature.LANTERN;
import static com.murimblock.world.raven.RavenCastleFeature.PAPER;
import static com.murimblock.world.raven.RavenCastleFeature.RAIL;
import static com.murimblock.world.raven.RavenCastleFeature.RED;
import static com.murimblock.world.raven.RavenCastleFeature.RED_BEAM;
import static com.murimblock.world.raven.RavenCastleFeature.TATAMI;
import static com.murimblock.world.raven.RavenCastleFeature.TILE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a player's hall in the Raven Halls (RavenHalls) and the closed rooms where the swallowed are kept, in the
 * style of the endless castle round them (RavenCastleFeature).
 * <ul>
 *   <li>The hall: a space cleared in the castle, a terrace of dark wood 45 blocks across hanging in it on red posts, railed,
 *   lanterns on its rail, the player's ground to build on; on it a pavilion 21 by 21, its floor of tatami round the
 *   raven's seal (a black circle ringed with gold), walls of paper in dark frames between red pillars, a roof of tiles in
 *   three steps with gold on its top, lanterns hanging inside, black feathers (carpets) and a nest in a corner; four doors
 *   of shadow (RavenDoorBlock), south the way out, west, east and north the ways to the player's perches.</li>
 *   <li>A closed room: 13 by 13, 7 high, walls of paper between red posts, a floor of tatami, two lanterns, no door.</li>
 * </ul>
 */
public final class RavenHallBuilder {
    /** The hall's half width inside, its walls' height, the terrace's half width. */
    static final int HALF = 10, HEIGHT = 8, TERRACE = 22;
    static final int ROOM_HALF = 6, ROOM_HEIGHT = 7;
    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private RavenHallBuilder() {
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    /** The hall round `c` (the middle of its floor, at the height one stands). */
    public static void hall(ServerLevel level, BlockPos c) {
        // The space cleared in the castle, the castle all round past it.
        for (int x = -TERRACE - 2; x <= TERRACE + 2; x++) for (int z = -TERRACE - 2; z <= TERRACE + 2; z++)
            for (int y = -3; y <= HEIGHT + 7; y++) set(level, c.offset(x, y, z), AIR);
        // The terrace: dark wood, a frame of logs, a red rail with lanterns, red posts down into the dark.
        for (int x = -TERRACE; x <= TERRACE; x++) for (int z = -TERRACE; z <= TERRACE; z++) {
            boolean edgeX = Math.abs(x) == TERRACE, edgeZ = Math.abs(z) == TERRACE;
            set(level, c.offset(x, -1, z), edgeX ? EDGE_Z : edgeZ ? EDGE_X : FLOOR);
            if (edgeX || edgeZ) {
                boolean post = Math.floorMod(x, 6) == 4 && edgeZ || Math.floorMod(z, 6) == 4 && edgeX || edgeX && edgeZ;
                set(level, c.offset(x, 0, z), post ? RED : RAIL);
                if (post) {
                    set(level, c.offset(x, 1, z), LANTERN);
                    for (int y = -2; y >= -12; y--) set(level, c.offset(x, y, z), RED);
                }
            }
            if (Math.floorMod(x, 6) == 4 && !edgeZ) set(level, c.offset(x, -2, z), RED_BEAM);
        }
        // The floor: tatami round the raven's seal.
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) {
            double d = Math.sqrt(x * x + z * z);
            set(level, c.offset(x, -1, z), d < 2.6 ? Blocks.BLACK_CONCRETE.defaultBlockState() : d < 3.4 ? GOLD
                    : Math.floorMod(x, 3) == 0 ? FLOOR : TATAMI);
        }
        // The walls: paper in dark frames between red pillars.
        for (int x = -HALF - 1; x <= HALF + 1; x++) for (int z = -HALF - 1; z <= HALF + 1; z++) {
            boolean edge = Math.abs(x) == HALF + 1 || Math.abs(z) == HALF + 1;
            if (!edge) continue;
            boolean column = (Math.abs(x) == HALF + 1 && Math.floorMod(z, 5) == 0) || (Math.abs(z) == HALF + 1 && Math.floorMod(x, 5) == 0)
                    || (Math.abs(x) == HALF + 1 && Math.abs(z) == HALF + 1);
            for (int y = 0; y < HEIGHT; y++) set(level, c.offset(x, y, z), column ? RED : y == 0 || y == 4 || y == HEIGHT - 1 ? FRAME : PAPER);
            set(level, c.offset(x, HEIGHT, z), RED_BEAM);
        }
        // The roof: a ceiling of dark wood, then three steps of tiles with eaves, gold on top.
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) set(level, c.offset(x, HEIGHT, z), FLOOR);
        for (int step = 0; step < 4; step++) {
            int r = HALF + 2 - step * 3;
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                boolean rim = Math.abs(x) == r || Math.abs(z) == r;
                if (!rim && step < 3) continue;
                Direction facing = x == -r ? Direction.EAST : x == r ? Direction.WEST : z == -r ? Direction.SOUTH : Direction.NORTH;
                set(level, c.offset(x, HEIGHT + 1 + step, z), rim ? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing) : TILE);
            }
        }
        set(level, c.offset(0, HEIGHT + 5, 0), GOLD);
        // Lanterns hanging from the ceiling.
        for (int x = -HALF + 2; x <= HALF - 2; x += 4) for (int z = -HALF + 2; z <= HALF - 2; z += 4)
            set(level, c.offset(x, HEIGHT - 1, z), HANGING);
        // Black feathers fallen on the floor.
        int[][] feathers = {{-7, 5}, {-3, 8}, {4, 6}, {7, -2}, {-6, -6}, {2, -8}, {8, 8}, {-8, 1}};
        for (int[] f : feathers) set(level, c.offset(f[0], 0, f[1]), Blocks.BLACK_CARPET.defaultBlockState());
        // The nest in the north-east corner.
        for (int x = 6; x <= 8; x++) for (int z = -8; z <= -6; z++)
            set(level, c.offset(x, 0, z), x == 7 && z == -7 ? Blocks.BLACK_WOOL.defaultBlockState() : Blocks.DARK_OAK_SLAB.defaultBlockState());
        // The doors: south the way out, then west, east and north to the perches.
        door(level, c.offset(0, 0, HALF + 1), Direction.Axis.X, 0);
        door(level, c.offset(-HALF - 1, 0, 0), Direction.Axis.Z, 1);
        door(level, c.offset(HALF + 1, 0, 0), Direction.Axis.Z, 2);
        door(level, c.offset(0, 0, -HALF - 1), Direction.Axis.X, 3);
    }

    /** A door three wide and three high in a wall, framed in red and gilded blackstone. */
    static void door(ServerLevel level, BlockPos foot, Direction.Axis along, int index) {
        BlockState frame = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState shadow = RavenBlocks.RAVEN_DOOR.get().defaultBlockState().setValue(RavenDoorBlock.INDEX, index);
        for (int k = -2; k <= 2; k++) for (int y = 0; y <= 4; y++) {
            BlockPos p = along == Direction.Axis.X ? foot.offset(k, y, 0) : foot.offset(0, y, k);
            boolean inside = Math.abs(k) <= 1 && y <= 2;
            set(level, p, inside ? shadow : Math.abs(k) == 2 ? RED : frame);
        }
    }

    /** A closed room round `c` (the middle of its floor): paper between red posts, tatami, no way out. */
    public static void room(ServerLevel level, BlockPos c) {
        for (int x = -ROOM_HALF - 1; x <= ROOM_HALF + 1; x++) for (int z = -ROOM_HALF - 1; z <= ROOM_HALF + 1; z++) for (int y = -1; y <= ROOM_HEIGHT; y++) {
            boolean wallX = Math.abs(x) == ROOM_HALF + 1, wallZ = Math.abs(z) == ROOM_HALF + 1;
            BlockState state;
            if (y == -1) state = Math.floorMod(x, 3) == 0 ? FLOOR : TATAMI;
            else if (y == ROOM_HEIGHT) state = FLOOR;
            else if (wallX || wallZ) {
                boolean post = wallX && Math.floorMod(z, 4) == 0 || wallZ && Math.floorMod(x, 4) == 0 || wallX && wallZ;
                state = post ? RED : y == 0 || y == 3 || y == ROOM_HEIGHT - 1 ? FRAME : PAPER;
            } else state = AIR;
            set(level, c.offset(x, y, z), state);
        }
        for (int x : new int[]{-3, 3}) set(level, c.offset(x, ROOM_HEIGHT - 1, 0), HANGING);
    }
}
