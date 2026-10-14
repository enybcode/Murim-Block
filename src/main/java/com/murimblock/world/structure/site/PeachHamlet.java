package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Peach Spring Hamlet (peach blossom valley), after Tao Yuanming's hidden village: rammed earth houses under thatch
 * round a green with a well, the unpainted hall of the elders, a little shrine to the god of the soil, fields fenced
 * with bamboo (crops on either side of an irrigation channel, a strip of rice), peach trees shedding petals and a
 * bamboo screen behind. Nothing marks rank.
 * <p>And the blight has come (Enzo's request, 14/10): the crab men's disease has reached the hamlet. Three crab men
 * roam it, each with its two armored crabs (one on the green, one in the fields, one in a house); grey growths like
 * dead coral spread on the earthen walls, dead corals sprout on the ground, crops lie trampled, mud spreads on the
 * green, a door is torn off, cobwebs hang in the corners. Nobody is left.
 */
final class PeachHamlet extends Site {
    /** Thatch: bamboo mosaic for the slopes (golden, woven), hay for the ridge. */
    static final RoofTiles THATCH = new RoofTiles(Blocks.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_MOSAIC_SLAB, Blocks.HAY_BLOCK);
    private static final BlockState EARTH = s(Blocks.PACKED_MUD);
    private static final BlockState POST = log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Y);
    private static final int[][] PEACHES = {{-9, -9}, {9, -9}, {-17, 7}, {17, 7}, {-12, -17}, {12, -17}};

    @Override public int reach() { return 20; }

    @Override public List<Part> parts() { return List.of(new Part(-20, -20, 20, 20, 0, 16)); }

    @Override public int probe() { return 17; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 8, false); }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, -20, -20, 20, 20, g, g + 22, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        // The green, of trodden earth, with the well in the middle.
        pave(c, -5, -5, 5, 5, g, s(Blocks.COARSE_DIRT), s(Blocks.DIRT_PATH), s(Blocks.DIRT_PATH), s(Blocks.PACKED_MUD));
        c.carried(-5, -5, 5, 5, g);
        well(c, g);
        house(c.frame(0, -13, Rotation.NONE), g, 5, 0);
        house(c.frame(-13, -2, Rotation.COUNTERCLOCKWISE_90), g, 3, 1);
        house(c.frame(13, -2, Rotation.CLOCKWISE_90), g, 3, 2);
        house(c.frame(-12, 12, Rotation.CLOCKWISE_180), g, 3, 3);
        shrine(c.frame(12, 12, Rotation.CLOCKWISE_180), g);
        // Paths from every door, the shrine and the field gate to the green.
        path(c, 0, -10, 0, -6, g);
        path(c, -10, -2, -6, -2, g);
        path(c, 6, -2, 10, -2, g);
        path(c, -12, 6, -12, 9, g);
        path(c, -12, 6, -6, 6, g);
        path(c, 12, 6, 12, 9, g);
        path(c, 6, 6, 12, 6, g);
        path(c, 0, 6, 0, 6, g);
        fields(c, g);
        for (int[] t : PEACHES) peach(c, t[0], t[1], g);
        // A bamboo screen along the back of the hamlet.
        for (int x = -18; x <= 18; x++) for (int z = -19; z <= -18; z++) {
            if (nearPeach(x, z, 3.5) || !c.chance(x, g, z, 0.6)) continue;
            bamboo(c, x, z, g);
        }
        for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) lanternPost(c, p[0], g + 1, p[1], 1, s(Blocks.OAK_FENCE));
        blight(c, g);
    }

    /** Whether the blight has rotted the grass here: about a third of the hamlet, in blotches 3 to 6 blocks across. */
    private static boolean blotch(Canvas c, int x, int z, int g) {
        int bx = Math.floorDiv(x, 4), bz = Math.floorDiv(z, 4);
        double coarse = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
            double wx = dx == 0 ? 1 - Math.floorMod(x, 4) / 4.0 : Math.floorMod(x, 4) / 4.0;
            double wz = dz == 0 ? 1 - Math.floorMod(z, 4) / 4.0 : Math.floorMod(z, 4) / 4.0;
            coarse += wx * wz * c.rand(bx + dx, g - 9, bz + dz);
        }
        return coarse + 0.25 * (c.rand(x, g - 8, z) - 0.5) > 0.6;
    }

    /** Dead corals that grow on dry land here: the blight's sprouts. */
    private static final Block[] SPROUTS = {Blocks.DEAD_BRAIN_CORAL, Blocks.DEAD_TUBE_CORAL, Blocks.DEAD_FIRE_CORAL_FAN, Blocks.DEAD_HORN_CORAL_FAN, Blocks.DEAD_BUBBLE_CORAL};

    /**
     * The blight over the open ground: mud on the green, dead corals sprouting from the grass and the paths, crops
     * trampled; and the crab men who brought it (the third waits in a house, see house).
     */
    private static void blight(Canvas c, int g) {
        for (int x = -19; x <= 19; x++) for (int z = -19; z <= 19; z++) {
            if (!c.inside(x, z)) continue;
            BlockState ground = c.get(x, g, z), above = c.get(x, g + 1, z);
            boolean green = Math.abs(x) <= 5 && Math.abs(z) <= 5 && Math.max(Math.abs(x), Math.abs(z)) >= 2;
            if (green && above.isAir() && c.chance(x, g - 3, z, 0.3)) { c.set(x, g, z, s(Blocks.MUD)); continue; }
            // The grass rotting in patches: blotches that grow together (a coarse noise, then a fine one).
            if (ground.is(Blocks.GRASS_BLOCK) && blotch(c, x, z, g)) {
                c.set(x, g, z, c.pick(x, g - 4, z, s(Blocks.PODZOL), s(Blocks.MUD), s(Blocks.COARSE_DIRT), s(Blocks.PODZOL)));
                if (!above.isAir() && !above.is(Blocks.PINK_PETALS)) continue;
                c.set(x, g + 1, z, AIR);
                ground = c.get(x, g, z);
                above = AIR;
            }
            if (above.isAir() && (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT_PATH) || ground.is(Blocks.COARSE_DIRT)
                    || ground.is(Blocks.PODZOL) || ground.is(Blocks.MUD)) && c.chance(x, g + 5, z, 0.12)) {
                Block sprout = SPROUTS[(int) (c.rand(x, g + 6, z) * SPROUTS.length)];
                c.set(x, g + 1, z, sprout.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
            } else if (ground.is(Blocks.FARMLAND) && !above.isAir() && c.chance(x, g + 7, z, 0.35)) {
                c.set(x, g + 1, z, AIR);
                c.set(x, g, z, s(Blocks.COARSE_DIRT));
            }
        }
        c.spawn(3, g + 1, 3, Direction.SOUTH, MurimEntities.CRAB_MAN.get(), crabMan -> { });
        c.spawn(4, g + 1, 16, Direction.NORTH, MurimEntities.CRAB_MAN.get(), crabMan -> { });
    }

    private static boolean nearPeach(int x, int z, double r) {
        for (int[] t : PEACHES) if (Math.hypot(x - t[0], z - t[1]) < r) return true;
        return false;
    }

    private static void path(Canvas c, int x0, int z0, int x1, int z1, int g) {
        pave(c, x0, z0, x1, z1, g, s(Blocks.DIRT_PATH), s(Blocks.DIRT_PATH), s(Blocks.COARSE_DIRT), s(Blocks.GRAVEL));
        c.carried(x0, z0, x1, z1, g);
    }

    private static void well(Canvas c, int g) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean centre = dx == 0 && dz == 0;
            for (int y = g - 4; y <= g; y++) c.set(dx, y, dz, centre && y > g - 4 ? s(Blocks.WATER) : s(Blocks.MOSSY_COBBLESTONE));
            if (!centre) c.set(dx, g + 1, dz, s(Blocks.MOSSY_COBBLESTONE_WALL));
        }
        for (int dx : new int[]{-1, 1}) c.set(dx, g + 2, 0, s(Blocks.OAK_FENCE));
        for (int dx = -1; dx <= 1; dx++) c.set(dx, g + 3, 0, log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.X));
        c.set(0, g + 2, 0, s(Blocks.CHAIN));
    }

    /**
     * A rammed earth house (frame: front +z): a stone footing course, packed earth walls with timber corners,
     * lattice windows, a thatched gable roof; {@code w} is its half-width (3 for a home, 5 for the elders' hall).
     */
    private static void house(Canvas h, int g, int w, int kind) {
        int z0 = -2, z1 = 2;
        for (int x = -w; x <= w; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = Math.abs(x) == w || z == z0 || z == z1;
            h.set(x, g, z, edge ? s(Blocks.COBBLESTONE) : EARTH);
            for (int y = g + 1; y <= g + 3; y++) {
                if (!edge) { h.set(x, y, z, AIR); continue; }
                boolean corner = Math.abs(x) == w && (z == z0 || z == z1);
                // The blight's grey growths spreading on the earth.
                BlockState wall = h.chance(x, y + 11, z, 0.3) ? h.pick(x, y + 12, z, s(Blocks.DEAD_BRAIN_CORAL_BLOCK), s(Blocks.DEAD_TUBE_CORAL_BLOCK)) : EARTH;
                h.set(x, y, z, corner ? POST : y == g + 1 ? h.pick(x, y, z, s(Blocks.COBBLESTONE), s(Blocks.MOSSY_COBBLESTONE)) : wall);
            }
        }
        h.carried(-w, z0, w, z1, g);
        for (int x = -w + 2; x <= w - 2; x += 2) if (x != 0) {
            h.set(x, g + 2, z1, lattice(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH));
            h.set(x, g + 2, z0, lattice(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH));
        }
        h.set(-w, g + 2, 0, lattice(Blocks.SPRUCE_TRAPDOOR, Direction.WEST));
        h.set(w, g + 2, 0, lattice(Blocks.SPRUCE_TRAPDOOR, Direction.EAST));
        door(h, 0, g + 1, z1, Blocks.OAK_DOOR, Direction.NORTH);
        h.kit().gableRoof(-w, z0, w, z1, g + 4, THATCH, s(Blocks.OAK_PLANKS), EARTH);
        h.room(-w + 1, g + 1, z0 + 1, w - 1, g + 3, z1 - 1);
        h.set(0, g + 3, 0, hangingLantern());
        int y = g + 1;
        if (kind == 0) {
            // The elders' hall: a long table and benches, an old scroll on its stand, candles.
            for (int x = -3; x <= 3; x++) {
                if (x == 0) continue;
                table(h, x, y, 0, Blocks.OAK_FENCE, Blocks.OAK_PRESSURE_PLATE);
                h.set(x, y, -1, stair(Blocks.OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
            }
            h.set(0, y, -1, s(Blocks.LECTERN));
            h.set(-4, y, -1, s(Blocks.BARREL));
            h.set(4, y, -1, litCandles(3));
            h.set(4, y, 1, s(Blocks.POTTED_FLOWERING_AZALEA));
            h.set(-4, y, 1, s(Blocks.COMPOSTER));
        } else {
            Block bed = kind == 1 ? Blocks.PINK_BED : kind == 2 ? Blocks.WHITE_BED : Blocks.YELLOW_BED;
            bed(h, -2, y, 0, Direction.NORTH, bed);
            h.set(-1, y, -1, s(Blocks.BARREL));
            h.set(2, y, -1, s(kind == 1 ? Blocks.LOOM : kind == 2 ? Blocks.SMOKER : Blocks.CRAFTING_TABLE));
            h.set(1, y, -1, s(kind == 3 ? Blocks.COMPOSTER : Blocks.FURNACE));
            h.set(2, y, 1, s(Blocks.POTTED_OAK_SAPLING));
            h.set(-2, y, 1, s(Blocks.WHITE_CARPET));
        }
        // Abandoned: cobwebs in the corners under the roof, one door torn off, a crab man waiting in the last house.
        h.set(-w + 1, g + 3, z0 + 1, s(Blocks.COBWEB));
        if (kind != 0) h.set(w - 1, g + 3, z1 - 1, s(Blocks.COBWEB));
        if (kind == 2) {
            h.set(0, g + 1, z1, AIR);
            h.set(0, g + 2, z1, AIR);
        }
        if (kind == 3) h.spawn(1, y, 0, Direction.SOUTH, MurimEntities.CRAB_MAN.get(), crabMan -> { });
    }

    /** The shrine to the god of the soil (frame: front +z): a stone niche under a tiny tiled roof, with offerings. */
    private static void shrine(Canvas h, int g) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            h.set(x, g, z, s(Blocks.STONE_BRICKS));
            h.set(x, g + 1, z, s(Blocks.STONE_BRICKS));
            h.footing(x, g - 1, z, s(Blocks.STONE_BRICKS));
        }
        h.carried(-1, -1, 1, 1, g);
        for (int y = g + 2; y <= g + 3; y++) {
            for (int x = -1; x <= 1; x++) h.set(x, y, -1, s(Blocks.STONE_BRICKS));
            h.set(-1, y, 0, s(Blocks.STONE_BRICKS));
            h.set(1, y, 0, s(Blocks.STONE_BRICKS));
            h.set(0, y, 0, AIR);
            for (int x = -1; x <= 1; x++) h.set(x, y, 1, x == 0 ? AIR : s(Blocks.STONE_BRICK_WALL));
        }
        h.set(0, g + 2, 0, s(Blocks.DECORATED_POT));
        h.set(0, g + 2, 1, litCandles(2));
        h.set(0, g + 1, 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        // Its roof: a little tiled pyramid.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
            Direction in = Math.abs(z) >= Math.abs(x) ? (z < 0 ? Direction.SOUTH : Direction.NORTH) : (x < 0 ? Direction.EAST : Direction.WEST);
            h.set(x, g + 4, z, rim ? stair(Blocks.DEEPSLATE_TILE_STAIRS, in, Half.BOTTOM) : s(Blocks.DEEPSLATE_TILES));
            if (!rim) h.set(x, g + 5, z, x == 0 && z == 0 ? s(Blocks.DEEPSLATE_TILES) : stair(Blocks.DEEPSLATE_TILE_STAIRS, in, Half.BOTTOM));
        }
        h.set(0, g + 6, 0, s(Blocks.DEEPSLATE_TILE_SLAB));
        h.room(0, g + 2, 0, 0, g + 3, 0);
    }

    /**
     * The fields, fenced with bamboo and entered by a gate from the green: two strips of crops either side of an
     * irrigation channel, and a strip of rice in mud and water at the far end.
     */
    private static void fields(Canvas c, int g) {
        int x0 = -6, x1 = 9, z0 = 7, z1 = 20;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean fence = x == x0 || x == x1 || z == z0 || z == z1;
            if (fence) {
                c.set(x, g + 1, z, x == 0 && z == z0 ? Blocks.BAMBOO_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH) : s(Blocks.BAMBOO_FENCE));
                continue;
            }
            if (z == 12) { c.set(x, g, z, s(Blocks.WATER)); c.set(x, g - 1, z, s(Blocks.MUD)); continue; }
            if (z >= 17) {
                c.set(x, g - 1, z, s(Blocks.MUD));
                if (z % 2 == 1) c.set(x, g, z, s(Blocks.WATER));
                else { c.set(x, g, z, s(Blocks.MUD)); if (c.chance(x, g, z, 0.85)) tallPlant(c, x, g + 1, z, MurimBlocks.WILD_RICE.get()); }
                continue;
            }
            if (z == 8 || z == 16) { c.set(x, g, z, s(Blocks.DIRT_PATH)); continue; }
            c.set(x, g, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            Block crop = x < -1 ? Blocks.WHEAT : x < 3 ? Blocks.CARROTS : x < 6 ? Blocks.POTATOES : Blocks.BEETROOTS;
            int age = crop == Blocks.BEETROOTS ? 1 + (int) (c.rand(x, g + 1, z) * 3) : 2 + (int) (c.rand(x, g + 1, z) * 6);
            c.set(x, g + 1, z, crop == Blocks.BEETROOTS ? crop.defaultBlockState().setValue(net.minecraft.world.level.block.BeetrootBlock.AGE, age)
                    : crop.defaultBlockState().setValue(CropBlock.AGE, age));
        }
        // The channel runs under the fence at both ends, closed by stone.
        c.set(x0, g, 12, s(Blocks.STONE));
        c.set(x1, g, 12, s(Blocks.STONE));
        c.carried(x0 + 1, z0 + 1, x1 - 1, 11, g);
    }

    /** A peach tree in flower, petals fallen on the grass round it. */
    private static void peach(Canvas c, int x, int z, int g) {
        tree(c, x, g + 1, z, log(MurimBlocks.PEACH.log().get(), Direction.Axis.Y), leaves(MurimBlocks.PEACH.leaves().get()), 3, 2.2, 0.75);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if ((dx == 0 && dz == 0) || !c.chance(x + dx, g + 1, z + dz, 0.45)) continue;
            if (!c.inside(x + dx, z + dz) || !c.get(x + dx, g, z + dz).is(Blocks.GRASS_BLOCK) || !c.get(x + dx, g + 1, z + dz).isAir()) continue;
            c.set(x + dx, g + 1, z + dz, Blocks.PINK_PETALS.defaultBlockState()
                    .setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (c.rand(x + dx, g + 2, z + dz) * 4))
                    .setValue(PinkPetalsBlock.FACING, c.pick(x + dx, g + 3, z + dz, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)));
        }
    }

    private static void bamboo(Canvas c, int x, int z, int g) {
        int h = 6 + (int) (c.rand(x, g + 1, z) * 5);
        for (int i = 1; i <= h; i++) {
            BambooLeaves leaves = i >= h - 1 ? BambooLeaves.LARGE : i == h - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
            c.set(x, g + i, z, Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, leaves));
        }
    }
}
