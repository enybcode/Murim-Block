package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.raven.RavenBlocks;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * The Raven Tower (docs/structures/raven-tower.md): the Crow Master's hermitage on the high peaks (the Sea of Clouds
 * peaks, the jade karst, the Wulingyuan pillars), a slender tower of four storeys on a plinth of dark stone, red posts
 * and paper walls, a skirt of black tiles round every storey, a pyramid roof with a gold finial. Inside, ladders from
 * floor to floor: below, the master's room (his books, his tea, his writing desk), then a storeroom and a sleeping floor;
 * at the top an open gallery of perches, black feathers and a nest where his ravens come and go.
 */
final class RavenTower extends Site {
    /** The tower's half width (its walls at ±HALF), the height of a storey, the storeys. */
    static final int HALF = 4, STOREY = 5, STOREYS = 4, PLINTH = 7;
    private static final BlockState STONE = s(Blocks.DEEPSLATE_BRICKS), STONE_DARK = s(Blocks.POLISHED_BLACKSTONE_BRICKS), STONE_TOP = s(Blocks.POLISHED_DEEPSLATE);
    private static final BlockState RED = log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y), BEAM = s(Blocks.MANGROVE_PLANKS);
    private static final BlockState FLOOR = s(Blocks.DARK_OAK_PLANKS), FRAME = s(Blocks.DARK_OAK_PLANKS), PAPER = s(Blocks.WHITE_TERRACOTTA);
    private static final BlockState GOLD = s(Blocks.GOLD_BLOCK);

    @Override public int reach() { return 12; }

    @Override public List<Part> parts() { return List.of(new Part(-10, -10, 10, 12, 0, STOREYS * STOREY + 12)); }

    @Override public int probe() { return 8; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 6, false); }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepareNatural(c, -10, -10, 10, 12, g, g + STOREYS * STOREY + 10, s(Blocks.COARSE_DIRT), s(Blocks.STONE));
        plinth(c, g);
        int base = g + 2;
        for (int k = 0; k < STOREYS; k++) storey(c, base + k * STOREY, k);
        int top = base + STOREYS * STOREY;
        c.kit().roof(-HALF, -HALF, HALF, HALF, top, RoofTiles.GREY, true);
        // The finial: gold over the lightning rod's place.
        c.set(0, top + 6, 0, GOLD);
        // Ladders on the north-west inner wall, a hatch through each floor.
        for (int k = 0; k < STOREYS; k++) {
            int y0 = base + k * STOREY + 1, y1 = base + (k + 1) * STOREY;
            if (k < STOREYS - 1) {
                ladder(c, -HALF + 1, y0, y1, -HALF + 1, Direction.SOUTH);
                c.set(-HALF + 1, y1, -HALF + 1, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
            }
        }
        rooms(c, base);
        // What the checks hold it to: the plinth and the ground floor carried, every storey under a roof.
        c.carried(-PLINTH, -PLINTH, PLINTH, PLINTH, g);
        c.carried(-PLINTH + 1, -PLINTH + 1, PLINTH - 1, PLINTH - 1, g + 1);
        c.carried(-HALF, -HALF, HALF, HALF, base);
        for (int k = 0; k < STOREYS; k++) c.room(-HALF + 2, base + k * STOREY + 1, -HALF + 2, HALF - 1, base + (k + 1) * STOREY - 1, HALF - 1);
        // The master himself, in his room below.
        c.spawn(1, base + 1, 1, Direction.SOUTH, MurimEntities.CROW_MASTER.get(), master -> master.setHome(new net.minecraft.core.BlockPos(c.wx(1, 1), base + 1, c.wz(1, 1))));
    }

    /** The plinth: two steps of dark stone, stairs up to the door on the south. */
    private static void plinth(Canvas c, int g) {
        for (int x = -PLINTH; x <= PLINTH; x++) for (int z = -PLINTH; z <= PLINTH; z++) {
            boolean rim = Math.abs(x) == PLINTH || Math.abs(z) == PLINTH;
            c.set(x, g, z, rim ? STONE_DARK : STONE);
            if (Math.abs(x) <= PLINTH - 1 && Math.abs(z) <= PLINTH - 1) c.set(x, g + 1, z, Math.abs(x) == PLINTH - 1 || Math.abs(z) == PLINTH - 1 ? STONE_DARK : STONE_TOP);
        }
        for (int x = -1; x <= 1; x++) {
            c.set(x, g + 1, PLINTH, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            c.set(x, g + 2, HALF + 1, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        for (int x = -1; x <= 1; x++) for (int z = PLINTH + 1; z <= 12; z++) c.set(x, g, z, c.pick(x, g, z, s(Blocks.COBBLED_DEEPSLATE), STONE, s(Blocks.GRAVEL)));
        // Two lantern posts by the steps.
        lanternPost(c, -3, g + 2, PLINTH - 1, 2, s(Blocks.DARK_OAK_FENCE));
        lanternPost(c, 3, g + 2, PLINTH - 1, 2, s(Blocks.DARK_OAK_FENCE));
    }

    /** A storey from its floor at `f`: red posts at the corners and mid-walls, paper in dark frames, a skirt of tiles above. */
    private static void storey(Canvas c, int f, int k) {
        boolean top = k == STOREYS - 1;
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) {
            c.set(x, f, z, Math.abs(x) == HALF || Math.abs(z) == HALF ? BEAM : FLOOR);
            for (int y = f + 1; y < f + STOREY; y++) c.set(x, y, z, AIR);
        }
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) {
            boolean edge = Math.abs(x) == HALF || Math.abs(z) == HALF;
            if (!edge) continue;
            boolean post = (Math.abs(x) == HALF && (Math.abs(z) == HALF || z == 0)) || (Math.abs(z) == HALF && x == 0);
            for (int h = 1; h < STOREY; h++) {
                int y = f + h;
                BlockState wall;
                if (post) wall = RED;
                else if (top) wall = h == 1 ? s(Blocks.DARK_OAK_FENCE) : h == STOREY - 1 ? FRAME : AIR;
                else if (h == 1 || h == STOREY - 1) wall = FRAME;
                else if (h == 2 && Math.abs(x) == 2 || h == 2 && Math.abs(z) == 2) wall = s(Blocks.DARK_OAK_TRAPDOOR);
                else wall = PAPER;
                c.set(x, y, z, wall);
            }
        }
        // The door on the south of the ground storey.
        if (k == 0) {
            door(c, -1, f + 1, HALF, Blocks.DARK_OAK_DOOR, Direction.SOUTH);
            c.set(0, f + 1, HALF, RED);
            c.set(1, f + 1, HALF, FRAME);
            for (int x = -1; x <= 1; x += 2) c.set(x == -1 ? -2 : 1, f + 2, HALF, PAPER);
        }
        skirt(c, f + STOREY, top);
        c.set(0, f + STOREY - 1, 0, hangingLantern());
    }

    /** A skirt of black tiles round the storey's top, one block out, its corners lifted. */
    private static void skirt(Canvas c, int y, boolean last) {
        int r = HALF + 1;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (Math.abs(x) < r && Math.abs(z) < r) continue;
            c.set(x, y, z, s(Blocks.DEEPSLATE_TILE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
            c.set(sx * r, y + 1, sz * r, s(Blocks.DEEPSLATE_TILE_STAIRS).setValue(StairBlock.FACING, sz < 0 ? Direction.SOUTH : Direction.NORTH));
        if (last) return;
        for (int x = -HALF; x <= HALF; x++) for (int z = -HALF; z <= HALF; z++) if (Math.abs(x) == HALF || Math.abs(z) == HALF) c.set(x, y, z, BEAM);
    }

    /** What each floor holds. */
    private static void rooms(Canvas c, int base) {
        // The master's room: a writing desk, books, tea.
        int f = base + 1;
        c.set(3, f, -3, s(Blocks.BOOKSHELF));
        c.set(3, f + 1, -3, s(Blocks.BOOKSHELF));
        c.set(2, f, -3, s(Blocks.BOOKSHELF));
        c.set(3, f, -2, s(Blocks.LECTERN));
        table(c, -2, f, 2, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        c.set(-2, f + 1, 2, s(Blocks.FLOWER_POT));
        c.set(-3, f, 2, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        c.set(3, f, 3, s(Blocks.BARREL));
        c.set(-3, f, -1, RavenBlocks.RAVEN_PERCH.get().defaultBlockState());
        // The storeroom.
        f = base + STOREY + 1;
        for (int x = 1; x <= 2; x++) c.set(x, f, -3, s(Blocks.BARREL));
        // The cache: a barrel holding the sites' loot (SiteLoot).
        c.stocked(3, f, -3, s(Blocks.BARREL), SiteLoot.CACHE);
        c.set(3, f, 3, s(Blocks.CHEST));
        c.set(2, f, 3, s(Blocks.BARREL));
        // The sleeping floor.
        f = base + 2 * STOREY + 1;
        bed(c, 2, f, 2, Direction.NORTH, Blocks.BLACK_BED);
        c.set(-2, f, 3, s(Blocks.CANDLE));
        // The gallery of perches, feathers and a nest.
        f = base + 3 * STOREY + 1;
        for (int[] p : new int[][]{{-3, 3}, {3, 3}, {3, -3}, {0, 2}}) c.set(p[0], f, p[1], RavenBlocks.RAVEN_PERCH.get().defaultBlockState());
        for (int x = 1; x <= 2; x++) for (int z = -2; z <= -1; z++) c.set(x, f, z, x == 1 && z == -2 ? s(Blocks.BLACK_WOOL) : s(Blocks.DARK_OAK_SLAB));
        for (int[] p : new int[][]{{-1, 0}, {2, 1}, {-2, -2}}) c.set(p[0], f, p[1], s(Blocks.BLACK_CARPET));
    }
}
