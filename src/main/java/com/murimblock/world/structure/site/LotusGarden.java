package com.murimblock.world.structure.site;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Lotus Moon Garden (Jiangnan waterlands), after the scholars' gardens of Suzhou: a white wall capped with dark tiles
 * and pierced with lattice windows, entered by a moon gate; inside, a lotus pond crossed by a zigzag bridge, a
 * waterside pavilion, a covered gallery along the west wall, Taihu rockeries, a willow, bamboo and osmanthus, and the
 * hall of the "zither and sword" at the north. The paths join the gate, the bridge, the gallery, the pavilion and both
 * doors of the hall.
 */
final class LotusGarden extends Site {
    private static final BlockState WALL = s(Blocks.CALCITE);
    private static final BlockState CAP = s(Blocks.DEEPSLATE_TILE_WALL);
    private static final BlockState FRAME = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
    private static final BlockState[] MOSAIC = {s(Blocks.POLISHED_ANDESITE), s(Blocks.ANDESITE), s(Blocks.POLISHED_ANDESITE), s(Blocks.STONE)};
    private static final BlockState[] ROCK = {s(Blocks.CALCITE), s(Blocks.DIORITE), s(Blocks.TUFF), s(Blocks.CALCITE)};

    @Override public int reach() { return 20; }

    @Override public List<Part> parts() { return List.of(new Part(-17, -15, 17, 20, 0, 16)); }

    @Override public int probe() { return 13; }

    @Override public int[] levels(Probe p) {
        int[] h = flat(p, probe(), 10, true);
        if (h == null || h[0] < p.seaLevel() - 8) return null;
        return new int[]{Math.max(p.seaLevel(), h[0])};
    }

    /** The pond: an oval with a wavering shore, centre (3, 0). */
    static boolean pond(int x, int z) {
        double dx = (x - 3) / 8.0, dz = z / 5.0;
        double a = Math.atan2(dz, dx);
        return dx * dx + dz * dz <= Math.pow(1 + 0.1 * Math.sin(3 * a) + 0.06 * Math.cos(5 * a), 2);
    }

    /** The zigzag bridge, as segments from the south bank to the hall steps. */
    private static final int[][] BRIDGE = {{0, 5, 0, 2}, {0, 2, 4, 2}, {4, 2, 4, -2}, {4, -2, 0, -2}, {0, -2, 0, -6}};

    static boolean onBridge(int x, int z) {
        for (int[] s : BRIDGE)
            if (x >= Math.min(s[0], s[2]) && x <= Math.max(s[0], s[2]) && z >= Math.min(s[1], s[3]) && z <= Math.max(s[1], s[3])) return true;
        return false;
    }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, -16, -14, 16, 14, g, g + 20, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        // A landing before the moon gate: solid ground, or a stone jetty where the garden stands in water.
        prepare(c, -2, 15, 2, 20, g, g + 8, s(Blocks.GRASS_BLOCK), s(Blocks.STONE_BRICKS));
        line(c, -1, 15, 1, 20, g);
        walls(c, g);
        water(c, g);
        paths(c, g);
        hall(c, g);
        pavilion(c, g);
        gallery(c.frame(-14, 0, Rotation.COUNTERCLOCKWISE_90), g);
        rockery(c, -9, 11, g, 2.2, 4);
        rockery(c, 12, -11, g, 1.8, 3);
        rockery(c, -4, 5, g, 1.4, 2);
        willow(c, -7, -3, g);
        tree(c, -10, g + 1, 2, log(MurimBlocks.OSMANTHUS.log().get(), Direction.Axis.Y), leaves(MurimBlocks.OSMANTHUS.leaves().get()), 2, 1.8, 0.9);
        tree(c, 8, g + 1, 12, log(MurimBlocks.OSMANTHUS.log().get(), Direction.Axis.Y), leaves(MurimBlocks.OSMANTHUS.leaves().get()), 2, 1.8, 0.9);
        for (int x = 11; x <= 15; x++) for (int z = 10; z <= 13; z++) if (c.chance(x, g, z, 0.55)) bamboo(c, x, z, g);
        for (int[] p : new int[][]{{-2, 12}, {2, 12}, {13, 5}, {13, -5}, {-11, -10}}) lanternPost(c, p[0], g + 1, p[1], 1, s(Blocks.STONE_BRICK_WALL));
    }

    /** White wall, tile-capped, with lattice windows, and the moon gate in the middle of the south side. */
    private static void walls(Canvas c, int g) {
        for (int x = -16; x <= 16; x++) for (int z = -14; z <= 14; z++) {
            if (Math.abs(x) != 16 && Math.abs(z) != 14) continue;
            c.set(x, g, z, s(Blocks.STONE_BRICKS));
            c.footing(x, g - 1, z, s(Blocks.STONE_BRICKS));
            c.carried(x, z, x, z, g);
            boolean gate = z == 14 && Math.abs(x) <= 4;
            int top = gate ? g + 7 : g + 5;
            for (int y = g + 1; y <= top; y++) c.set(x, y, z, WALL);
            c.set(x, top + 1, z, CAP);
            // Lattice windows every six blocks along the wall (not at corners or by the gate).
            int along = Math.abs(x) == 16 ? z : x;
            boolean corner = Math.abs(x) == 16 && Math.abs(z) == 14;
            if (!corner && !gate && Math.floorMod(along, 6) == 3) {
                Direction out = Math.abs(x) == 16 ? (x > 0 ? Direction.EAST : Direction.WEST) : (z > 0 ? Direction.SOUTH : Direction.NORTH);
                c.set(x, g + 2, z, lattice(Blocks.DARK_OAK_TRAPDOOR, out));
                c.set(x, g + 3, z, lattice(Blocks.DARK_OAK_TRAPDOOR, out));
            }
        }
        // The moon gate: a round opening ringed with grey stone.
        for (int x = -4; x <= 4; x++) for (int y = g + 1; y <= g + 7; y++) {
            double d2 = x * x + (y - (g + 3.5)) * (y - (g + 3.5));
            if (d2 <= 7.3) c.set(x, y, 14, AIR);
            else if (d2 <= 13.5) c.set(x, y, 14, s(Blocks.POLISHED_ANDESITE));
        }
    }

    /** The pond, two deep over mud, its shore lined with stones, lotus on it. */
    private static void water(Canvas c, int g) {
        for (int x = -6; x <= 12; x++) for (int z = -6; z <= 6; z++) {
            if (!pond(x, z)) continue;
            c.set(x, g - 3, z, s(Blocks.MUD));
            c.set(x, g - 2, z, s(Blocks.WATER));
            c.set(x, g - 1, z, s(Blocks.WATER));
            c.set(x, g, z, AIR);
            if (!onBridge(x, z) && c.chance(x, g, z, 0.3))
                c.set(x, g, z, s(c.chance(x, g + 1, z, 0.25) ? MurimBlocks.LOTUS_BLOSSOM.get() : MurimBlocks.LOTUS_PAD.get()));
        }
        for (int x = -7; x <= 13; x++) for (int z = -7; z <= 7; z++) {
            if (pond(x, z)) continue;
            boolean shore = pond(x + 1, z) || pond(x - 1, z) || pond(x, z + 1) || pond(x, z - 1);
            if (shore && !onBridge(x, z)) c.set(x, g, z, c.pick(x, g, z, s(Blocks.MOSSY_COBBLESTONE), s(Blocks.COBBLESTONE), s(Blocks.TUFF)));
            if (shore) { c.set(x, g - 1, z, s(Blocks.STONE)); c.set(x, g - 2, z, s(Blocks.STONE)); }
        }
    }

    private static void paths(Canvas c, int g) {
        line(c, 0, 14, 0, 6, g);      // moon gate to the bridge
        line(c, -12, 8, 0, 8, g);     // to the gallery
        line(c, 0, 9, 14, 9, g);      // east, along the bamboo
        line(c, 14, -9, 14, 9, g);    // up the east side, past the pavilion
        line(c, 9, -9, 14, -9, g);    // to the hall's east door
        line(c, -12, -9, -9, -9, g);  // gallery to the hall's west door
        line(c, 13, 0, 14, 0, g);     // into the pavilion
        // The zigzag bridge: a stone deck flush with the banks, on piers, over the water.
        for (int[] s : BRIDGE) for (int x = Math.min(s[0], s[2]); x <= Math.max(s[0], s[2]); x++)
            for (int z = Math.min(s[1], s[3]); z <= Math.max(s[1], s[3]); z++) {
                if (!pond(x, z)) { c.set(x, g, z, c.pick(x, g, z, MOSAIC)); c.carried(x, z, x, z, g); continue; }
                c.set(x, g, z, s(Blocks.STONE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
                if ((x + z) % 3 == 0 || x == s[0] && z == s[1]) for (int y = g - 3; y < g; y++) c.set(x, y, z, s(Blocks.STONE_BRICKS));
            }
    }

    private static void line(Canvas c, int x0, int z0, int x1, int z1, int g) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
            if (pond(x, z)) continue;
            c.set(x, g, z, c.pick(x, g, z, MOSAIC));
            c.carried(x, z, x, z, g);
        }
    }

    /** The hall at the north: white walls in a dark frame, lattice front, grey hipped roof; a study inside. */
    private static void hall(Canvas c, int g) {
        int x0 = -7, x1 = 7, z0 = -12, z1 = -8, f = g + 1;
        c.fill(x0 - 1, f, z0 - 1, x1 + 1, f, z1 + 1, s(Blocks.STONE_BRICKS));
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        for (int x = -1; x <= 1; x++) c.set(x, g + 1, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int y = f + 1; y <= f + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean post = (x == x0 || x == x1 || x % 3 == 0) && (z == z0 || z == z1) || (x == x0 || x == x1) && z == (z0 + z1) / 2 - 1;
            if (post) c.set(x, y, z, FRAME);
            else if (z == z1 && y >= f + 1 && y <= f + 3) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            else c.set(x, y, z, WALL);
        }
        door(c, 0, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        door(c, x0, f + 1, -9, Blocks.DARK_OAK_DOOR, Direction.EAST);
        door(c, x1, f + 1, -9, Blocks.DARK_OAK_DOOR, Direction.WEST);
        c.set(x0 - 1, f, -9, s(Blocks.STONE_BRICKS));
        c.set(x1 + 1, f, -9, s(Blocks.STONE_BRICKS));
        c.set(x0 - 2, g + 1, -9, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, Half.BOTTOM));
        c.set(x1 + 2, g + 1, -9, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, Half.BOTTOM));
        c.kit().roof(x0, z0, x1, z1, f + 5, RoofTiles.GREY, false);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 4, z1 - 1);
        int y = f + 1;
        // A scholar-swordsman's study: books, a zither (note block) on its table, tea, a sword grindstone, scrolls.
        for (int x = -6; x <= -3; x++) { c.set(x, y, z0 + 1, s(Blocks.BOOKSHELF)); c.set(x, y + 1, z0 + 1, s(Blocks.BOOKSHELF)); }
        c.set(-2, y, z0 + 1, s(Blocks.LECTERN));
        c.set(1, y, -10, s(Blocks.NOTE_BLOCK));
        c.set(0, y, -10, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        table(c, 3, y, -10, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        c.set(3, y, -9, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        c.set(4, y, -10, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        c.set(6, y, z0 + 1, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR));
        c.set(5, y, z0 + 1, s(Blocks.POTTED_BAMBOO));
        c.set(6, y, -10, s(Blocks.POTTED_AZALEA));
        wallBanner(c, 2, y + 2, z0 + 1, Blocks.WHITE_WALL_BANNER, Direction.SOUTH);
        wallBanner(c, 4, y + 2, z0 + 1, Blocks.LIGHT_GRAY_WALL_BANNER, Direction.SOUTH);
        for (int x : new int[]{-4, 0, 4}) c.set(x, f + 4, -10, hangingLantern());
    }

    /** The waterside pavilion, half over the pond: four posts, benches, a pyramid roof. */
    private static void pavilion(Canvas c, int g) {
        int x0 = 9, x1 = 13, z0 = -2, z1 = 2;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            c.set(x, g, z, s(Blocks.STONE_BRICKS));
            c.footing(x, g - 1, z, s(Blocks.STONE_BRICKS));
            for (int y = g + 1; y <= g + 3; y++) c.set(x, y, z, AIR);
        }
        c.carried(x0, z0, x1, z1, g);
        for (int x : new int[]{x0, x1}) for (int z : new int[]{z0, z1}) for (int y = g + 1; y <= g + 3; y++) c.set(x, y, z, FRAME);
        // Benches round the sides, open to the east path.
        for (int z = z0 + 1; z <= z1 - 1; z++) c.set(x0, g + 1, z, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            c.set(x, g + 1, z0, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
            c.set(x, g + 1, z1, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        c.kit().roof(x0, z0, x1, z1, g + 4, RoofTiles.GREY, true);
        c.set(11, g + 3, 0, hangingLantern());
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 3, z1 - 1);
    }

    /** The covered gallery along the west wall (frame: front +z to the garden), its lean-to roof on the wall. */
    private static void gallery(Canvas h, int g) {
        int x0 = -12, x1 = 12;
        for (int x = x0; x <= x1; x++) for (int z = -1; z <= 1; z++) {
            h.set(x, g, z, s(Blocks.STONE_BRICKS));
            h.carried(x, z, x, z, g);
            for (int y = g + 1; y <= g + 3; y++) h.set(x, y, z, AIR);
        }
        for (int x = x0; x <= x1; x++) {
            boolean post = Math.floorMod(x, 3) == 0;
            if (post) for (int y = g + 1; y <= g + 3; y++) h.set(x, y, 1, FRAME);
            else if (x != -8 && x != 9 && x != 8 && Math.abs(x) != x1 - 1) h.set(x, g + 1, 1, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        h.kit().shedRoof(x0, -1, x1, 1, g + 4, RoofTiles.GREY, s(Blocks.DARK_OAK_PLANKS), WALL);
        for (int x = -9; x <= 9; x += 6) h.set(x, g + 3, 0, hangingLantern());
        h.room(x0, g + 1, -1, x1, g + 3, 1);
    }

    /** A Taihu rockery: pale, pitted stones heaped round (x, z). */
    private static void rockery(Canvas c, int x, int z, int g, double r, int height) {
        int R = (int) Math.ceil(r);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.hypot(dx, dz);
            if (d > r) continue;
            int h = (int) Math.round(height * (1 - d / (r + 0.6)) + c.rand(x + dx, g, z + dz) * 1.5);
            for (int y = 1; y <= h; y++) {
                if (y > 1 && c.chance(x + dx, g + y, z + dz, 0.18)) continue; // the holes of Taihu stone
                c.set(x + dx, g + y, z + dz, c.pick(x + dx, g + y, z + dz, ROCK));
            }
        }
    }

    /** A weeping willow on the bank, its strands hanging over the water. */
    private static void willow(Canvas c, int x, int z, int g) {
        double r = 2.8, squash = 0.6;
        int cy = g + 5;
        tree(c, x, g + 1, z, log(MurimBlocks.WILLOW.log().get(), Direction.Axis.Y), leaves(MurimBlocks.WILLOW.leaves().get()), 4, r, squash);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz < 4 || !c.chance(x + dx, g, z + dz, 0.6)) continue;
            int under = g + 1;
            while (under < g + 9 && !inCrown(c, x, cy, z, r, squash, x + dx, under + 1, z + dz)) under++;
            if (under >= g + 9) continue;
            int length = 2 + (int) (c.rand(x + dx, g + 2, z + dz) * 3);
            for (int i = 0; i < length && under - i > g; i++) c.set(x + dx, under - i, z + dz, s(MurimBlocks.WILLOW_STRANDS.get()));
        }
    }

    private static void bamboo(Canvas c, int x, int z, int g) {
        c.set(x, g, z, s(Blocks.PODZOL));
        int h = 5 + (int) (c.rand(x, g + 1, z) * 5);
        for (int i = 1; i <= h; i++) {
            BambooLeaves leaves = i >= h - 1 ? BambooLeaves.LARGE : i == h - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
            c.set(x, g + i, z, Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, leaves));
        }
    }
}
