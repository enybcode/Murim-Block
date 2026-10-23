package com.murimblock.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * The island of an inner world (mockup validated by Enzo on 23 Oct 2026, detailed on 24 Oct): 64 blocks across, a flat
 * 32-block arena in the middle for boss fights (taiji at the center, spokes, lantern posts around), the arrival point
 * to the south under a gate, an empty 8x8 zone to the north kept for a future NPC, a pond with lily pads and reeds,
 * pines, cherry trees, a bamboo grove, flowers, a rocky underside with hanging roots, dripstone and spore blossoms,
 * and floating islets. The same island for everyone for now. Offsets from the center: +x east, +z south.
 */
public final class InnerIsland {
    /** Raise when the layout changes: islands built with an older version are rebuilt on the next visit. */
    public static final int VERSION = 2;
    public static final int SURFACE_Y = 100;
    public static final int RADIUS = 32;
    public static final int ARENA_RADIUS = 16;
    /** Where the player appears, relative to the island center (south of the arena). */
    public static final int ARRIVAL_Z = 24;
    /** The NPC zone, relative to the island center: x -4..3, z -28..-21. */
    public static final int NPC_X = -4;
    public static final int NPC_Z = -28;
    public static final int NPC_SIZE = 8;
    /** Half-size of the box cleared before a rebuild, floating islets included. */
    static final int CLEAR_RADIUS = 52;
    private static final int CLEAR_BELOW = 40;
    private static final int CLEAR_ABOVE = 30;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private static final double TAIJI_RADIUS = 6.6;
    private static final int GATE_Z = 20;
    private static final int POND_X = 20;
    private static final int POND_Z = 12;
    private static final int[][] PINES = {{-22, -20, 9}, {-26, -6, 8}, {27, -9, 10}, {-12, 27, 7}, {-28, 8, 7}};
    private static final int[][] CHERRIES = {{-20, 16}, {25, 1}};
    private static final int[] BAMBOO = {18, -21};
    private static final int[][] AZALEAS = {{-9, -28}, {29, 10}, {10, 28}, {-29, -14}};
    private static final int[][] BOULDERS = {{-26, 18}, {12, -27}, {28, 16}, {-30, 0}};
    /** Floating islets: x, y above the surface, z, radius, decor (0 bare, 1 pine, 2 azalea, 3 flowers). */
    private static final int[][] ISLETS = {{-42, 8, -34, 3, 1}, {44, 12, -28, 2, 3}, {40, -4, 34, 3, 2},
            {-38, 14, 30, 2, 3}, {6, 18, -46, 2, 0}, {-48, 0, 4, 2, 2}};

    private final ServerLevel level;
    private final BlockPos center;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private InnerIsland(ServerLevel level, BlockPos center) {
        this.level = level;
        this.center = center;
    }

    public static void build(ServerLevel level, BlockPos center, boolean clearFirst) {
        InnerIsland island = new InnerIsland(level, center);
        if (clearFirst) {
            island.clear();
        }
        island.ground();
        island.arena();
        island.paths();
        island.npcZone();
        island.arrival();
        island.gate();
        island.pond();
        for (int[] p : PINES) {
            island.pine(p[0], SURFACE_Y + 1, p[1], p[2]);
        }
        for (int[] c : CHERRIES) {
            island.cherry(c[0], c[1]);
        }
        island.bamboo(BAMBOO[0], BAMBOO[1], 3);
        for (int[] a : AZALEAS) {
            island.azalea(a[0], SURFACE_Y + 1, a[1]);
        }
        for (int[] b : BOULDERS) {
            island.boulder(b[0], b[1]);
        }
        island.flora();
        for (int[] i : ISLETS) {
            island.islet(i[0], SURFACE_Y + i[1], i[2], i[3], i[4]);
        }
    }

    // ---- Shape ----

    /** The edge wobbles a little so the island does not look turned on a lathe. */
    static double edge(double angle) {
        return RADIUS + 1.2 * Math.sin(angle * 3 + 0.7) + 0.8 * Math.sin(angle * 7 + 2.1);
    }

    /** A stable pseudo-random number in [0, 1) for a spot, different for each salt. */
    private static double hash(int x, int z, int salt) {
        long h = x * 73856093L ^ z * 19349663L ^ salt * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return Math.floorMod(h, 10_000) / 10_000.0;
    }

    /** Smooth patches across the island, roughly -1.5 to 1.5. */
    private static double patches(int x, int z) {
        return Math.sin(x * 0.21 + z * 0.13) * Math.cos(z * 0.17 - x * 0.07) + 0.5 * Math.sin((x + z) * 0.37);
    }

    private static double pondShape(int x, int z) {
        double dx = x - POND_X;
        double dz = z - POND_Z;
        return dx * dx / 25.0 + dz * dz / 12.25;
    }

    /** Spots kept clear of trees, rocks and plants: arena and its posts, paths, gate, arrival, NPC zone, pond. */
    private static boolean reserved(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        return d <= ARENA_RADIUS + 4
                || Math.abs(x) <= 2 && (z > ARENA_RADIUS && z < ARRIVAL_Z || z < -ARENA_RADIUS && z >= NPC_Z + NPC_SIZE)
                || Math.abs(x) <= 5 && Math.abs(z - GATE_Z) <= 2
                || x * x + (z - ARRIVAL_Z) * (z - ARRIVAL_Z) <= 20
                || x >= NPC_X - 2 && x <= NPC_X + NPC_SIZE + 1 && z >= NPC_Z - 2 && z <= NPC_Z + NPC_SIZE + 1
                || pondShape(x, z) <= 1.7;
    }

    private void clear() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = SURFACE_Y - CLEAR_BELOW; y <= SURFACE_Y + CLEAR_ABOVE; y++) {
                    at(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, FLAGS);
                    }
                }
            }
        }
    }

    /** Grass with moss patches on top; dirt, then banded rock below in an upside-down cone; roots and stone hanging under. */
    private void ground() {
        for (int x = -RADIUS - 3; x <= RADIUS + 3; x++) {
            for (int z = -RADIUS - 3; z <= RADIUS + 3; z++) {
                double d = Math.sqrt(x * x + z * z);
                double edge = edge(Math.atan2(z, x));
                if (d > edge) {
                    continue;
                }
                boolean rim = d > edge - 1.5;
                int depth = 3 + (int) (22 * Math.pow(1 - d / edge, 0.8)) + (int) (hash(x, z, 1) * 3);
                int bottom = SURFACE_Y - depth;
                for (int y = bottom; y <= SURFACE_Y; y++) {
                    set(x, y, z, groundBlock(x, y, z, rim));
                }
                underside(x, bottom, z, d, edge);
            }
        }
    }

    private static BlockState groundBlock(int x, int y, int z, boolean rim) {
        if (y == SURFACE_Y) {
            return patches(x, z) > 0.95 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (y >= SURFACE_Y - 3) {
            return rim && hash(x, z, y) < 0.25 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        double h = hash(x, z, y);
        if (rim && h < 0.3) {
            return h < 0.15 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
        }
        // Rock in bands, as cut by a blade.
        return switch (Math.floorMod(y + (int) (patches(x, z) * 2), 9)) {
            case 0 -> Blocks.TUFF.defaultBlockState();
            case 4, 5 -> Blocks.ANDESITE.defaultBlockState();
            case 7 -> Blocks.DEEPSLATE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    private void underside(int x, int bottom, int z, double d, double edge) {
        double h = hash(x, z, 7);
        if (d > edge - 3) {
            if (h < 0.4) {
                set(x, bottom - 1, z, Blocks.HANGING_ROOTS.defaultBlockState());
            }
        } else if (h < 0.07) {
            set(x, bottom, z, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
            dripstone(x, bottom - 1, z, 1 + (int) (hash(x, z, 8) * 4));
        } else if (h < 0.085) {
            set(x, bottom - 1, z, Blocks.SPORE_BLOSSOM.defaultBlockState());
        }
    }

    /** Pointed dripstone hanging down from y, this many blocks long. */
    private void dripstone(int x, int y, int z, int length) {
        for (int i = 0; i < length; i++) {
            DripstoneThickness thickness = i == length - 1 ? DripstoneThickness.TIP
                    : i == length - 2 ? DripstoneThickness.FRUSTUM : i == 0 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            set(x, y - i, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                    .setValue(PointedDripstoneBlock.THICKNESS, thickness));
        }
    }

    // ---- Arena, paths and places ----

    /** Polished stone with a taiji at the center, eight spokes, a ring, a rim; lantern posts just outside. */
    private void arena() {
        for (int x = -ARENA_RADIUS - 1; x <= ARENA_RADIUS + 1; x++) {
            for (int z = -ARENA_RADIUS - 1; z <= ARENA_RADIUS + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= ARENA_RADIUS + 0.5) {
                    set(x, SURFACE_Y, z, arenaBlock(x, z, d));
                }
            }
        }
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI / 8 + k * Math.PI / 4;
            int x = (int) Math.round(Math.cos(angle) * (ARENA_RADIUS + 2.5));
            int z = (int) Math.round(Math.sin(angle) * (ARENA_RADIUS + 2.5));
            lanternPost(x, z);
        }
    }

    private static BlockState arenaBlock(int x, int z, double d) {
        if (d > ARENA_RADIUS - 1) {
            return Math.floorMod((int) Math.round(Math.atan2(z, x) * 16 / Math.PI), 4) == 0
                    ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (d <= TAIJI_RADIUS) {
            return taiji(x, z, TAIJI_RADIUS) ? Blocks.POLISHED_DIORITE.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (d <= TAIJI_RADIUS + 1 || d > 11 && d <= 12) {
            return Blocks.TUFF_BRICKS.defaultBlockState();
        }
        double angle = Math.atan2(z, x);
        double toSpoke = Math.abs(Math.IEEEremainder(angle, Math.PI / 4)) * d;
        if (toSpoke < 0.6 && d < ARENA_RADIUS - 2) {
            return Blocks.POLISHED_TUFF.defaultBlockState();
        }
        return hash(x, z, 3) < 0.06 ? Blocks.ANDESITE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
    }

    /** Light side of a taiji of this radius at offset (x, z): two halves, two swirls, two dots. */
    private static boolean taiji(int x, int z, double r) {
        double half = r / 2;
        double north = Math.hypot(x, z + half);
        double south = Math.hypot(x, z - half);
        if (north < r / 6 + 0.3) {
            return false;
        }
        if (south < r / 6 + 0.3) {
            return true;
        }
        if (north < half) {
            return true;
        }
        if (south < half) {
            return false;
        }
        return x > 0;
    }

    private void lanternPost(int x, int z) {
        set(x, SURFACE_Y, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(x, SURFACE_Y + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(x, SURFACE_Y + 2, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(x, SURFACE_Y + 3, z, Blocks.LANTERN.defaultBlockState());
    }

    /** Stone paths from the arrival point to the arena and from the arena to the NPC zone, edged with gravel. */
    private void paths() {
        for (int z = -ARENA_RADIUS - 5; z <= ARRIVAL_Z - 2; z++) {
            if (Math.abs(z) <= ARENA_RADIUS && z > NPC_Z + NPC_SIZE - 1) {
                continue;
            }
            for (int x = -2; x <= 2; x++) {
                double h = hash(x, z, 5);
                if (Math.abs(x) == 2) {
                    if (h < 0.45) {
                        set(x, SURFACE_Y, z, h < 0.25 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
                    }
                } else {
                    set(x, SURFACE_Y, z, h < 0.2 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                            : h < 0.3 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
        }
    }

    /** An empty paved square kept for a future NPC, lanterns at its corners; nothing else yet. */
    private void npcZone() {
        for (int x = 0; x < NPC_SIZE; x++) {
            for (int z = 0; z < NPC_SIZE; z++) {
                boolean border = x == 0 || z == 0 || x == NPC_SIZE - 1 || z == NPC_SIZE - 1;
                set(NPC_X + x, SURFACE_Y, NPC_Z + z, border ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
        }
        lanternPost(NPC_X - 1, NPC_Z - 1);
        lanternPost(NPC_X + NPC_SIZE, NPC_Z - 1);
        lanternPost(NPC_X - 1, NPC_Z + NPC_SIZE);
        lanternPost(NPC_X + NPC_SIZE, NPC_Z + NPC_SIZE);
    }

    private void arrival() {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x * x + z * z <= 5) {
                    set(x, SURFACE_Y, ARRIVAL_Z + z, x == 0 && z == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                            : Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
        }
        lanternPost(-3, ARRIVAL_Z + 2);
        lanternPost(3, ARRIVAL_Z + 2);
    }

    /** A red gate over the south path: two pillars, a beam, a tiled roof with raised ends, two hanging lanterns. */
    private void gate() {
        BlockState pillar = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
        BlockState beam = Blocks.MANGROVE_PLANKS.defaultBlockState();
        for (int x : new int[] {-3, 3}) {
            set(x, SURFACE_Y, GATE_Z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            for (int y = 1; y <= 4; y++) {
                set(x, SURFACE_Y + y, GATE_Z, pillar);
            }
        }
        for (int x = -4; x <= 4; x++) {
            set(x, SURFACE_Y + 5, GATE_Z, beam);
            for (int z = GATE_Z - 1; z <= GATE_Z + 1; z++) {
                set(x, SURFACE_Y + 6, z, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
            }
        }
        for (int z = GATE_Z - 1; z <= GATE_Z + 1; z++) {
            set(-5, SURFACE_Y + 6, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            set(5, SURFACE_Y + 6, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        }
        set(-1, SURFACE_Y + 4, GATE_Z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(1, SURFACE_Y + 4, GATE_Z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** Clear water over sand and clay, seagrass in the deep middle, lily pads, reeds and stones on the shore. */
    private void pond() {
        for (int x = POND_X - 7; x <= POND_X + 7; x++) {
            for (int z = POND_Z - 6; z <= POND_Z + 6; z++) {
                double e = pondShape(x, z);
                double h = hash(x, z, 11);
                if (e <= 1) {
                    boolean deep = e < 0.5;
                    set(x, SURFACE_Y - 2, z, Blocks.CLAY.defaultBlockState());
                    set(x, SURFACE_Y - 1, z, !deep ? Blocks.SAND.defaultBlockState()
                            : h < 0.4 ? Blocks.SEAGRASS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                    set(x, SURFACE_Y, z, Blocks.WATER.defaultBlockState());
                    if (h > 0.88) {
                        set(x, SURFACE_Y + 1, z, Blocks.LILY_PAD.defaultBlockState());
                    }
                } else if (e <= 1.5) {
                    set(x, SURFACE_Y, z, h < 0.2 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.SAND.defaultBlockState());
                    if (h > 0.7) {
                        int height = h > 0.85 ? 3 : 2;
                        for (int y = 1; y <= height; y++) {
                            set(x, SURFACE_Y + y, z, Blocks.SUGAR_CANE.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    // ---- Trees, rocks and plants ----

    private void pine(int x, int base, int z, int height) {
        BlockState leaves = leaves(Blocks.SPRUCE_LEAVES);
        for (int y = 0; y < height; y++) {
            set(x, base + y, z, Blocks.SPRUCE_LOG.defaultBlockState());
        }
        // Tiers of leaves, wide and narrow in turn, shrinking to a tip.
        for (int y = 2; y <= height + 1; y++) {
            double t = (double) (y - 2) / (height - 1);
            double r = Math.max(0.5, 3 * (1 - t) + 0.4) * ((height - y) % 2 == 0 ? 1 : 0.6);
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (Math.hypot(dx, dz) <= r + 0.2) {
                        setIfAir(x + dx, base + y, z + dz, leaves);
                    }
                }
            }
        }
        setIfAir(x, base + height + 2, z, leaves);
        groundCover(x, z, 2, Blocks.PODZOL.defaultBlockState(), 0.6);
    }

    /** A cherry tree: a trunk forking in two, pink canopies, petals on the grass below. */
    private void cherry(int x, int z) {
        int base = SURFACE_Y + 1;
        BlockState log = Blocks.CHERRY_LOG.defaultBlockState();
        for (int y = 0; y < 4; y++) {
            set(x, base + y, z, log);
        }
        set(x + 1, base + 4, z, log);
        set(x + 2, base + 5, z, log);
        set(x - 1, base + 4, z + 1, log);
        set(x - 1, base + 5, z + 1, log);
        BlockState leaves = leaves(Blocks.CHERRY_LEAVES);
        blob(x + 2, base + 7, z, 3.2, leaves);
        blob(x - 2, base + 6, z + 1, 2.6, leaves);
        blob(x, base + 8, z - 1, 2.4, leaves);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double h = hash(x + dx, z + dz, 13);
                if (Math.hypot(dx, dz) <= 4.5 && h < 0.45 && grassAt(x + dx, z + dz)) {
                    set(x + dx, base, z + dz, Blocks.PINK_PETALS.defaultBlockState()
                            .setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (h * 8.8))
                            .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (h * 100) % 4)));
                }
            }
        }
    }

    /** A flattened ball of leaves, ragged at its edge. */
    private void blob(int cx, int cy, int cz, double r, BlockState leaves) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 2.5 + dz * dz);
                    if (d <= r - 0.6 || d <= r && hash(cx + dx, cz + dz, cy + dy) < 0.6) {
                        setIfAir(cx + dx, cy + dy, cz + dz, leaves);
                    }
                }
            }
        }
    }

    /** Full-grown bamboo (it no longer grows), thick and thin stalks of varied height, leafy tops. */
    private void bamboo(int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double h = hash(cx + dx, cz + dz, 17);
                if (Math.hypot(dx, dz) > r + 0.3 || h > 0.5 || !grassAt(cx + dx, cz + dz)) {
                    continue;
                }
                int height = 7 + (int) (h * 12);
                BlockState stalk = Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, h < 0.25 ? 1 : 0)
                        .setValue(BambooStalkBlock.STAGE, 1);
                for (int y = 1; y <= height; y++) {
                    BambooLeaves foliage = y >= height - 1 ? BambooLeaves.LARGE : y >= height - 3 ? BambooLeaves.SMALL : BambooLeaves.NONE;
                    set(cx + dx, SURFACE_Y + y, cz + dz, stalk.setValue(BambooStalkBlock.LEAVES, foliage));
                }
            }
        }
        groundCover(cx, cz, r + 1, Blocks.COARSE_DIRT.defaultBlockState(), 0.25);
    }

    private void azalea(int x, int base, int z) {
        BlockState plain = leaves(Blocks.AZALEA_LEAVES);
        BlockState flowering = leaves(Blocks.FLOWERING_AZALEA_LEAVES);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 2 + dz * dz);
                    double h = hash(x + dx, z + dz, base + dy);
                    if (d <= 1.6 || d <= 2.3 && h < 0.5) {
                        setIfAir(x + dx, base + dy, z + dz, h < 0.4 ? flowering : plain);
                    }
                }
            }
        }
    }

    private void boulder(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double h = hash(x + dx, z + dz, 19);
                BlockState rock = h < 0.4 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : h < 0.7 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState();
                if (Math.abs(dx) + Math.abs(dz) <= 1 || h < 0.4) {
                    set(x + dx, SURFACE_Y + 1, z + dz, rock);
                }
                if (dx == 0 && dz == 0 || Math.abs(dx) + Math.abs(dz) == 1 && h < 0.3) {
                    set(x + dx, SURFACE_Y + 2, z + dz, h < 0.5 ? Blocks.MOSS_BLOCK.defaultBlockState() : rock);
                }
            }
        }
    }

    /** Grass, ferns and flowers in patches, each patch with its own flowers; rare tall flowers. */
    private void flora() {
        BlockState[][] patchFlowers = {
                {Blocks.DANDELION.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()},
                {Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState()},
                {Blocks.ALLIUM.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()},
                {Blocks.POPPY.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()}};
        Block[] tallFlowers = {Blocks.PEONY, Blocks.LILAC, Blocks.ROSE_BUSH};
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                if (reserved(x, z) || !grassAt(x, z)) {
                    continue;
                }
                double h = hash(x, z, 23);
                double p = patches(x, z);
                if (h < 0.2) {
                    set(x, SURFACE_Y + 1, z, Blocks.SHORT_GRASS.defaultBlockState());
                } else if (h < 0.26) {
                    tall(x, z, Blocks.TALL_GRASS);
                } else if (h < 0.29) {
                    set(x, SURFACE_Y + 1, z, Blocks.FERN.defaultBlockState());
                } else if (h < 0.3) {
                    tall(x, z, Blocks.LARGE_FERN);
                } else if (h < 0.38 && p > 0.3) {
                    BlockState[] set = patchFlowers[Math.floorMod((int) Math.floor(Math.atan2(z, x) * 2 / Math.PI + 4), 4)];
                    set(x, SURFACE_Y + 1, z, set[(int) (hash(x, z, 29) * set.length)]);
                } else if (h < 0.386) {
                    tall(x, z, tallFlowers[(int) (hash(x, z, 31) * tallFlowers.length)]);
                }
            }
        }
    }

    private void tall(int x, int z, Block plant) {
        if (!level.getBlockState(at(x, SURFACE_Y + 2, z)).isAir()) {
            return;
        }
        set(x, SURFACE_Y + 1, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, SURFACE_Y + 2, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A floating islet: rock tapering down, moss or grass on top, a little life on it, dripstone under. */
    private void islet(int cx, int cy, int cz, int r, int decor) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int bottom = 1;
                for (int dy = -r - 1; dy <= 0; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz) + Math.max(0, -dy - 1) * 0.8;
                    if (d > r + 0.3) {
                        continue;
                    }
                    bottom = Math.min(bottom, dy);
                    BlockState state = dy < 0 ? (hash(cx + dx, cz + dz, dy) < 0.3 ? Blocks.ANDESITE.defaultBlockState()
                            : Blocks.STONE.defaultBlockState())
                            : decor == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                    set(cx + dx, cy + dy, cz + dz, state);
                }
                if (bottom <= 0 && hash(cx + dx, cz + dz, 37) < 0.3) {
                    set(cx + dx, cy + bottom - 1, cz + dz, Blocks.HANGING_ROOTS.defaultBlockState());
                }
            }
        }
        if (decor == 0) {
            set(cx, cy - r - 2, cz, Blocks.SPORE_BLOSSOM.defaultBlockState());
        } else {
            dripstone(cx, cy - r - 2, cz, r);
        }
        switch (decor) {
            case 1 -> pine(cx, cy + 1, cz, 5);
            case 2 -> azalea(cx, cy + 1, cz);
            case 3 -> {
                set(cx, cy + 1, cz, Blocks.LILY_OF_THE_VALLEY.defaultBlockState());
                set(cx + 1, cy + 1, cz, Blocks.SHORT_GRASS.defaultBlockState());
                set(cx - 1, cy + 1, cz - 1, Blocks.CORNFLOWER.defaultBlockState());
            }
            default -> {
            }
        }
    }

    // ---- Helpers ----

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private void groundCover(int x, int z, int r, BlockState cover, double chance) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.hypot(dx, dz) <= r && hash(x + dx, z + dz, 41) < chance && grassAt(x + dx, z + dz)) {
                    set(x + dx, SURFACE_Y, z + dz, cover);
                }
            }
        }
    }

    private boolean grassAt(int x, int z) {
        return level.getBlockState(at(x, SURFACE_Y, z)).is(Blocks.GRASS_BLOCK)
                && level.getBlockState(at(x, SURFACE_Y + 1, z)).isAir();
    }

    private BlockPos at(int x, int y, int z) {
        return pos.set(center.getX() + x, y, center.getZ() + z);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(at(x, y, z), state, FLAGS);
    }

    private void setIfAir(int x, int y, int z, BlockState state) {
        if (level.getBlockState(at(x, y, z)).isAir()) {
            level.setBlock(pos, state, FLAGS);
        }
    }
}
