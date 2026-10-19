package com.murimblock.world.raven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Builds a player's hall in the Raven Halls (RavenHalls) and the closed rooms where the swallowed are kept.
 * <ul>
 *   <li>The hall: a terrace of polished deepslate 45 blocks across, the player's ground to build on; on it a hall 21 by
 *   21, its floor of deepslate tiles with a ring of polished blackstone round a black circle, walls of deepslate bricks
 *   between blackstone pillars, a dark oak beam, a roof of blackstone slabs, lanterns hanging from it, black feathers
 *   (carpets) here and there, a nest of dark oak in a corner; four doors of shadow (RavenDoorBlock), south the way out,
 *   west, east and north the ways to the player's perches.</li>
 *   <li>A closed room: 13 by 13, 7 high, deepslate bricks, two lanterns, no door.</li>
 * </ul>
 */
public final class RavenHallBuilder {
    /** The hall's half width inside, its walls' height. */
    static final int HALF = 10, HEIGHT = 8, TERRACE = 22;
    static final int ROOM_HALF = 6, ROOM_HEIGHT = 7;

    private RavenHallBuilder() {
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    /** The hall round `c` (the middle of its floor, at the height one stands). */
    public static void hall(ServerLevel level, BlockPos c) {
        BlockState terrace = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), tiles = Blocks.DEEPSLATE_TILES.defaultBlockState(),
                ring = Blocks.POLISHED_BLACKSTONE.defaultBlockState(), heart = Blocks.BLACK_CONCRETE.defaultBlockState(),
                wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState(), pillar = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                beam = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X),
                roof = Blocks.BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        // The terrace, the player's ground.
        for (int x = -TERRACE; x <= TERRACE; x++) for (int z = -TERRACE; z <= TERRACE; z++) {
            set(level, c.offset(x, -1, z), terrace);
            set(level, c.offset(x, -2, z), Blocks.DEEPSLATE.defaultBlockState());
        }
        // The floor: tiles, a ring, a black heart.
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) {
            double d = Math.sqrt(x * x + z * z);
            set(level, c.offset(x, -1, z), d < 2.5 ? heart : d < 3.6 ? ring : tiles);
        }
        // The walls between pillars, a beam on top, the roof.
        for (int x = -HALF - 1; x <= HALF + 1; x++) for (int z = -HALF - 1; z <= HALF + 1; z++) {
            boolean edge = Math.abs(x) == HALF + 1 || Math.abs(z) == HALF + 1;
            for (int y = 0; y < HEIGHT; y++) {
                if (!edge) { set(level, c.offset(x, y, z), Blocks.AIR.defaultBlockState()); continue; }
                boolean column = (Math.abs(x) == HALF + 1 && Math.floorMod(z, 5) == 0) || (Math.abs(z) == HALF + 1 && Math.floorMod(x, 5) == 0)
                        || (Math.abs(x) == HALF + 1 && Math.abs(z) == HALF + 1);
                set(level, c.offset(x, y, z), column ? pillar : wall);
            }
            set(level, c.offset(x, HEIGHT, z), edge ? beam : roof);
        }
        // Lanterns hanging from the roof.
        for (int x = -HALF + 2; x <= HALF - 2; x += 4) for (int z = -HALF + 2; z <= HALF - 2; z += 4)
            set(level, c.offset(x, HEIGHT - 1, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Black feathers fallen on the floor.
        int[][] feathers = {{-7, 5}, {-3, 8}, {4, 6}, {7, -2}, {-6, -6}, {2, -8}, {8, 8}, {-8, 1}};
        for (int[] f : feathers) set(level, c.offset(f[0], 0, f[1]), Blocks.BLACK_CARPET.defaultBlockState());
        // The nest in the north-east corner.
        for (int x = 6; x <= 8; x++) for (int z = -8; z <= -6; z++)
            set(level, c.offset(x, 0, z), x == 7 && z == -7 ? Blocks.BLACK_WOOL.defaultBlockState()
                    : Blocks.DARK_OAK_SLAB.defaultBlockState());
        // The doors: south the way out, then west, east and north to the perches.
        door(level, c.offset(0, 0, HALF + 1), Direction.Axis.X, 0);
        door(level, c.offset(-HALF - 1, 0, 0), Direction.Axis.Z, 1);
        door(level, c.offset(HALF + 1, 0, 0), Direction.Axis.Z, 2);
        door(level, c.offset(0, 0, -HALF - 1), Direction.Axis.X, 3);
    }

    /** A door three wide and three high in a wall, framed in gilded blackstone, a lantern over it. */
    static void door(ServerLevel level, BlockPos foot, Direction.Axis along, int index) {
        BlockState frame = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState shadow = RavenBlocks.RAVEN_DOOR.get().defaultBlockState().setValue(RavenDoorBlock.INDEX, index);
        for (int k = -2; k <= 2; k++) for (int y = 0; y <= 4; y++) {
            BlockPos p = along == Direction.Axis.X ? foot.offset(k, y, 0) : foot.offset(0, y, k);
            boolean inside = Math.abs(k) <= 1 && y <= 2;
            set(level, p, inside ? shadow : frame);
        }
    }

    /** A closed room round `c` (the middle of its floor). */
    public static void room(ServerLevel level, BlockPos c) {
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState(), floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        for (int x = -ROOM_HALF - 1; x <= ROOM_HALF + 1; x++) for (int z = -ROOM_HALF - 1; z <= ROOM_HALF + 1; z++) for (int y = -1; y <= ROOM_HEIGHT; y++) {
            boolean shell = y == -1 || y == ROOM_HEIGHT || Math.abs(x) == ROOM_HALF + 1 || Math.abs(z) == ROOM_HALF + 1;
            set(level, c.offset(x, y, z), shell ? (y == -1 ? floor : wall) : Blocks.AIR.defaultBlockState());
        }
        for (int x : new int[]{-3, 3}) set(level, c.offset(x, ROOM_HEIGHT - 1, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }
}
