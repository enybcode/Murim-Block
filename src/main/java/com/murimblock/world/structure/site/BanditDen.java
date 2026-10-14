package com.murimblock.world.structure.site;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.slab;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Yellow River Bandit Den (loess plateau): a sunken courtyard (dikengyuan) dug 7 blocks into the plateau, 13 x 13, with
 * vaulted cave rooms (yaodong) in its four walls behind lattice facades: the chief's cave, the feast hall, two
 * dormitories, the armoury, a jail and the kitchen. A ramp in a cut leads down from a gate on the plateau, a tiled eave
 * and a low parapet run round the rim, and a lookout stands on the plateau. From afar: only a square hole in the plain.
 */
final class BanditDen extends Site {
    static final int DEPTH = 7;
    private static final BlockState LOESS = s(MurimBlocks.LOESS.get());
    private static final BlockState BRICK = s(Blocks.MUD_BRICKS);
    private static final BlockState FLOOR = s(Blocks.PACKED_MUD);
    private static final BlockState GRINDSTONE = Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR);

    enum Room { CHIEF, HALL, DORM, ARMOURY, JAIL, KITCHEN }

    @Override public int reach() { return 21; }

    @Override public List<Part> parts() { return List.of(new Part(-17, -17, 17, 21, 0, 12)); }

    @Override public int probe() { return 14; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 8, false); }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0], p = g - DEPTH;
        // The plateau the den is dug into: its own surface kept, solid loess under it.
        prepareNatural(c, -17, -17, 17, 17, g, g + 20, LOESS, LOESS);
        c.fill(-17, p - 2, -17, 17, g - 1, 17, LOESS);
        courtyard(c, g, p);
        cave(c.frame(-3, -7, Rotation.NONE), p, Room.CHIEF);
        cave(c.frame(3, -7, Rotation.NONE), p, Room.HALL);
        cave(c.frame(-7, -3, Rotation.COUNTERCLOCKWISE_90), p, Room.ARMOURY);
        cave(c.frame(-7, 3, Rotation.COUNTERCLOCKWISE_90), p, Room.JAIL);
        cave(c.frame(7, -3, Rotation.CLOCKWISE_90), p, Room.DORM);
        cave(c.frame(7, 3, Rotation.CLOCKWISE_90), p, Room.DORM);
        cave(c.frame(-3, 7, Rotation.CLOCKWISE_180), p, Room.KITCHEN);
        ramp(c, g, p);
        rim(c, g);
        lookout(c, g, 12, -12);
        // Solid ground before the gate, whatever the gullies of the plateau do there.
        prepareNatural(c, 0, 18, 6, 21, g, g + 8, LOESS, LOESS);
        for (int x = 2; x <= 4; x++) for (int z = 17; z <= 21; z++) c.set(x, g, z, c.pick(x, g, z, s(Blocks.COARSE_DIRT), s(Blocks.DIRT_PATH), LOESS));
        c.carried(2, 17, 4, 21, g);
    }

    private static void courtyard(Canvas c, int g, int p) {
        c.fill(-6, p + 1, -6, 6, g + 1, 6, AIR);
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            boolean walk = Math.abs(x) == 6 || Math.abs(z) == 6;
            c.set(x, p, z, walk ? s(Blocks.BRICKS) : c.pick(x, p, z, FLOOR, FLOOR, s(Blocks.COARSE_DIRT)));
        }
        c.carried(-6, -6, 6, 6, p);
        // A brick skirt at the foot of the walls.
        for (int i = -6; i <= 6; i++) for (int[] w : new int[][]{{i, -7}, {i, 7}, {-7, i}, {7, i}}) c.set(w[0], p + 1, w[1], BRICK);
        // The jujube tree in the middle, on its patch of grass.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) c.set(x, p, z, s(Blocks.GRASS_BLOCK));
        tree(c, 0, p + 1, 0, log(MurimBlocks.JUJUBE.log().get(), Direction.Axis.Y), leaves(MurimBlocks.JUJUBE.leaves().get()), 3, 2.6, 0.8);
        // The well, the millstone, maize drying, a trough, lanterns.
        for (int y = p - 3; y <= p; y++) c.set(4, y, -4, s(Blocks.WATER));
        c.set(4, p - 4, -4, LOESS);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) {
            c.set(4 + dx, p, -4 + dz, s(Blocks.COBBLESTONE));
            c.set(4 + dx, p + 1, -4 + dz, s(Blocks.MOSSY_COBBLESTONE_WALL));
        }
        c.set(-4, p + 1, -4, GRINDSTONE);
        c.set(-4, p + 1, -3, s(Blocks.HAY_BLOCK));
        c.set(-3, p + 1, -4, s(Blocks.HAY_BLOCK));
        c.set(3, p + 1, 4, s(Blocks.HAY_BLOCK));
        c.set(4, p + 1, 4, s(Blocks.HAY_BLOCK));
        c.set(4, p + 2, 4, s(Blocks.HAY_BLOCK));
        c.set(-4, p + 1, 4, s(Blocks.WATER_CAULDRON));
        for (int x : new int[]{-5, 5}) for (int z : new int[]{-5, 5}) if (!(x == 5 && z == -5)) lanternPost(c, x, p + 1, z, 2, s(Blocks.DARK_OAK_FENCE));
    }

    /**
     * A cave room (frame: its facade on the courtyard wall at z = 0, looking +z): 3 wide, 8 deep, vaulted, behind a
     * facade of mud bricks and lattice with the door in the middle and red couplets either side.
     */
    private static void cave(Canvas h, int p, Room room) {
        for (int z = -8; z <= 0; z++) {
            for (int x = -1; x <= 1; x++) {
                h.set(x, p, z, room == Room.JAIL ? s(Blocks.COARSE_DIRT) : FLOOR);
                for (int y = p + 1; y <= p + 3; y++) h.set(x, y, z, AIR);
            }
            h.set(0, p + 4, z, AIR);
            h.set(-1, p + 4, z, stair(Blocks.MUD_BRICK_STAIRS, Direction.WEST, Half.TOP));
            h.set(1, p + 4, z, stair(Blocks.MUD_BRICK_STAIRS, Direction.EAST, Half.TOP));
        }
        h.room(-1, p + 1, -8, 1, p + 4, -1);
        // Facade.
        if (room == Room.JAIL) {
            for (int x = -1; x <= 1; x++) for (int y = p + 1; y <= p + 3; y++) h.set(x, y, 0, s(Blocks.IRON_BARS));
            h.set(0, p + 4, 0, s(Blocks.IRON_BARS));
        } else {
            for (int x : new int[]{-1, 1}) {
                h.set(x, p + 1, 0, BRICK);
                h.set(x, p + 2, 0, BRICK);
                h.set(x, p + 3, 0, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            }
            if (room == Room.CHIEF) {
                // The chief's cave opens in a tall arch, without a door: the Bandit Chief stands 2.5 blocks high.
                for (int y = p + 1; y <= p + 3; y++) h.set(0, y, 0, AIR);
            } else {
                door(h, 0, p + 1, 0, Blocks.SPRUCE_DOOR, Direction.NORTH);
                h.set(0, p + 3, 0, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            }
            h.set(0, p + 4, 0, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
        }
        wallBanner(h, -2, p + 3, 1, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        wallBanner(h, 2, p + 3, 1, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        h.set(0, p + 4, -4, hangingLantern());
        int y = p + 1;
        switch (room) {
            case CHIEF -> {
                kang(h, p);
                bed(h, 0, p + 2, -7, Direction.NORTH, Blocks.RED_BED);
                h.set(-1, p + 2, -7, s(Blocks.DECORATED_POT));
                h.set(1, p + 2, -7, s(Blocks.BARREL));
                h.set(0, y, -5, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
                h.set(-1, y, -5, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                h.set(1, y, -5, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                for (int x = -1; x <= 1; x++) for (int z = -4; z <= -2; z++) h.set(x, y, z, s(Blocks.ORANGE_CARPET));
                h.set(-1, y, -2, s(Blocks.BLACK_CARPET));
                h.set(1, y, -4, s(Blocks.BLACK_CARPET));
                wallBanner(h, 0, p + 4, -8, Blocks.BLACK_WALL_BANNER, Direction.SOUTH);
            }
            case HALL -> {
                for (int z = -7; z <= -2; z++) {
                    table(h, 0, y, z, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
                    h.set(-1, y, z, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
                    h.set(1, y, z, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, Half.BOTTOM));
                }
                h.set(0, y, -8, s(Blocks.BARREL));
                h.set(-1, y, -8, s(Blocks.BARREL));
                h.set(1, y, -8, s(Blocks.SMOKER));
                h.set(0, y + 1, -8, litCandles(3));
            }
            case DORM -> {
                kang(h, p);
                for (int x = -1; x <= 1; x++) bed(h, x, p + 2, -7, Direction.NORTH, Blocks.BROWN_BED);
                h.set(-1, y, -5, s(Blocks.BARREL));
                h.set(1, y, -5, s(Blocks.BARREL));
                h.set(1, y, -4, s(Blocks.CRAFTING_TABLE));
            }
            case ARMOURY -> {
                h.set(-1, y, -8, s(Blocks.SMITHING_TABLE));
                h.set(0, y, -8, s(Blocks.BLAST_FURNACE));
                h.set(1, y, -8, s(Blocks.ANVIL));
                h.set(-1, y, -6, GRINDSTONE);
                h.set(1, y, -6, s(Blocks.BARREL));
                h.set(1, y, -5, s(Blocks.BARREL));
                h.set(-1, y, -4, s(Blocks.FLETCHING_TABLE));
            }
            case JAIL -> {
                for (int z = -8; z <= -5; z++) for (int x = -1; x <= 1; x++) if (h.chance(x, y, z, 0.5)) h.set(x, y, z, s(Blocks.HAY_BLOCK));
                for (int x : new int[]{-1, 1}) {
                    h.set(x, p + 3, -3, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y));
                    h.set(x, p + 2, -3, s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y));
                }
                h.set(0, y, -2, s(Blocks.CAULDRON));
            }
            case KITCHEN -> {
                h.set(-1, y, -8, s(Blocks.SMOKER));
                h.set(0, y, -8, s(Blocks.FURNACE));
                h.set(1, y, -8, s(Blocks.WATER_CAULDRON));
                h.set(-1, y, -7, s(Blocks.BARREL));
                h.set(1, y, -6, s(Blocks.CRAFTING_TABLE));
                h.set(-1, y, -5, s(Blocks.HAY_BLOCK));
                h.set(1, y, -4, s(Blocks.BARREL));
            }
        }
    }

    /** The kang: the heated brick sleeping platform across the back of a cave. */
    private static void kang(Canvas h, int p) {
        for (int x = -1; x <= 1; x++) for (int z = -8; z <= -7; z++) h.set(x, p + 1, z, s(Blocks.BRICKS));
    }

    /** The way in: a gate on the plateau, a ramp down a cut that becomes a tunnel, a gateway into the courtyard. */
    private static void ramp(Canvas c, int g, int p) {
        // Gateway in the south wall (x 2..4), vaulted like a cave mouth.
        for (int x = 2; x <= 4; x++) for (int y = p + 1; y <= p + 3; y++) c.set(x, y, 7, AIR);
        c.set(3, p + 4, 7, AIR);
        c.set(2, p + 4, 7, stair(Blocks.MUD_BRICK_STAIRS, Direction.WEST, Half.TOP));
        c.set(4, p + 4, 7, stair(Blocks.MUD_BRICK_STAIRS, Direction.EAST, Half.TOP));
        for (int x = 2; x <= 4; x++) c.set(x, p, 7, s(Blocks.BRICKS));
        // The ramp: seven steps up to the plateau, roofed by the earth where it is deep, open in a cut where not.
        for (int i = 0; i < DEPTH; i++) {
            int z = 8 + i, y = p + 1 + i;
            for (int x = 2; x <= 4; x++) {
                c.set(x, y, z, stair(Blocks.MUD_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
                for (int yy = y + 1; yy <= y + 3; yy++) c.set(x, yy, z, AIR);
            }
            if (y + 4 < g) {
                c.set(3, y + 4, z, AIR);
                c.set(2, y + 4, z, stair(Blocks.MUD_BRICK_STAIRS, Direction.WEST, Half.TOP));
                c.set(4, y + 4, z, stair(Blocks.MUD_BRICK_STAIRS, Direction.EAST, Half.TOP));
                c.room(2, y + 1, z, 4, y + 3, z);
            } else {
                for (int x = 2; x <= 4; x++) for (int yy = y + 4; yy <= g + 1; yy++) c.set(x, yy, z, AIR);
            }
        }
        c.set(3, p + 5, 8, hangingLantern());
        // Open cut walls get a parapet; the gate stands at the top of the ramp.
        for (int z = 10; z <= 15; z++) { c.set(1, g + 1, z, s(Blocks.MUD_BRICK_WALL)); c.set(5, g + 1, z, s(Blocks.MUD_BRICK_WALL)); }
        for (int x = 2; x <= 4; x++) { c.set(x, g, 15, s(Blocks.BRICKS)); c.set(x, g, 16, s(Blocks.BRICKS)); }
        c.carried(2, 15, 4, 16, g);
        for (int x : new int[]{1, 5}) {
            for (int y = g + 1; y <= g + 4; y++) c.set(x, y, 16, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
            wallBanner(c, x, g + 3, 17, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        }
        for (int x = 0; x <= 6; x++) c.set(x, g + 5, 16, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X));
        c.kit().gableRoof(1, 16, 5, 16, g + 6, RoofTiles.GREY, s(Blocks.DARK_OAK_PLANKS), s(Blocks.DARK_OAK_PLANKS));
        c.set(2, g + 4, 16, hangingLantern());
        c.set(4, g + 4, 16, hangingLantern());
    }

    /** Round the rim: a tiled eave over the courtyard and a low parapet on the plateau. */
    private static void rim(Canvas c, int g) {
        for (int i = -6; i <= 6; i++) {
            c.set(i, g, -6, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
            c.set(i, g, 6, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, Half.BOTTOM));
            c.set(-6, g, i, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
            c.set(6, g, i, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
        }
        for (int i = -7; i <= 7; i++) for (int[] w : new int[][]{{i, -7}, {i, 7}, {-7, i}, {7, i}}) c.set(w[0], g + 1, w[1], s(Blocks.MUD_BRICK_WALL));
    }

    /** A lookout on the plateau: a platform on four posts, a railing, a ladder and a lantern. */
    private static void lookout(Canvas c, int g, int x, int z) {
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) {
            c.carried(x + dx, z + dz, x + dx, z + dz, g);
            for (int y = g + 1; y <= g + 4; y++) c.set(x + dx, y, z + dz, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            c.set(x + dx, g + 5, z + dz, s(Blocks.DARK_OAK_PLANKS));
            if (dx != 0 || dz != 0) c.set(x + dx, g + 6, z + dz, s(Blocks.DARK_OAK_FENCE));
        }
        c.set(x, g + 6, z + 1, AIR);
        c.set(x, g + 4, z + 1, s(Blocks.DARK_OAK_PLANKS));
        ladder(c, x, g + 1, g + 5, z + 2, Direction.SOUTH);
        c.set(x, g + 1, z + 1, s(Blocks.DARK_OAK_PLANKS));
        for (int y = g + 2; y <= g + 3; y++) c.set(x, y, z + 1, s(Blocks.DARK_OAK_PLANKS));
        c.set(x - 1, g + 7, z - 1, s(Blocks.LANTERN));
    }
}
