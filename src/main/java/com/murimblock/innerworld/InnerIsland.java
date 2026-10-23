package com.murimblock.innerworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The island of an inner world (mockup validated by Enzo on 23 Oct 2026): 64 blocks across, a flat 32-block arena in
 * the middle for boss fights, the arrival point to the south, an empty 8x8 zone to the north kept for a future NPC,
 * a pond, pines and a few floating stones. The same island for everyone for now. Offsets: +x east, +z south.
 */
public final class InnerIsland {
    /** Raise when the layout changes: islands built with an older version are rebuilt on the next visit. */
    public static final int VERSION = 1;
    public static final int SURFACE_Y = 100;
    public static final int RADIUS = 32;
    public static final int ARENA_RADIUS = 16;
    /** Where the player appears, relative to the island center (south of the arena). */
    public static final int ARRIVAL_Z = 24;
    /** The NPC zone, relative to the island center: x -4..3, z -28..-21. */
    public static final int NPC_X = -4;
    public static final int NPC_Z = -28;
    public static final int NPC_SIZE = 8;
    /** Half-size of the box cleared before a rebuild, floating stones included. */
    static final int CLEAR_RADIUS = 52;
    private static final int CLEAR_BELOW = 40;
    private static final int CLEAR_ABOVE = 30;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private static final int[][] PINES = {{-22, -20}, {-25, -6}, {-23, 16}, {-16, 23}, {22, -19}, {27, -6}, {12, 25}};
    /** Floating stones: x, y above the surface, z, radius. */
    private static final int[][] STONES = {{-42, 8, -34, 3}, {44, 12, -28, 2}, {40, -4, 34, 3}};

    private InnerIsland() {
    }

    public static void build(ServerLevel level, BlockPos center, boolean clearFirst) {
        if (clearFirst) {
            clear(level, center);
        }
        ground(level, center);
        arena(level, center);
        paths(level, center);
        npcZone(level, center);
        arrival(level, center);
        pond(level, center);
        for (int[] p : PINES) {
            pine(level, center.offset(p[0], SURFACE_Y - center.getY() + 1, p[1]), 6 + Math.floorMod(p[0] * 7 + p[1], 3));
        }
        for (int[] s : STONES) {
            floatingStone(level, center.offset(s[0], SURFACE_Y - center.getY() + s[1], s[2]), s[3]);
        }
        flora(level, center);
    }

    private static void clear(ServerLevel level, BlockPos center) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = SURFACE_Y - CLEAR_BELOW; y <= SURFACE_Y + CLEAR_ABOVE; y++) {
                    pos.set(center.getX() + x, y, center.getZ() + z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, FLAGS);
                    }
                }
            }
        }
    }

    /** The edge wobbles a little so the island does not look turned on a lathe. */
    static double edge(double angle) {
        return RADIUS + 1.2 * Math.sin(angle * 3 + 0.7) + 0.8 * Math.sin(angle * 7 + 2.1);
    }

    /** Whether this offset is on the island's top. */
    public static boolean onIsland(int x, int z) {
        return Math.sqrt(x * x + z * z) <= edge(Math.atan2(z, x));
    }

    private static void ground(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -RADIUS - 3; x <= RADIUS + 3; x++) {
            for (int z = -RADIUS - 3; z <= RADIUS + 3; z++) {
                double d = Math.sqrt(x * x + z * z);
                double edge = edge(Math.atan2(z, x));
                if (d > edge) {
                    continue;
                }
                // An upside-down cone of rock under the grass, deepest under the arena.
                int depth = 3 + (int) (22 * Math.pow(1 - d / edge, 0.8)) + Math.floorMod(x * 31 + z * 17, 3);
                for (int y = SURFACE_Y - depth; y <= SURFACE_Y; y++) {
                    BlockState state = y == SURFACE_Y ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : y >= SURFACE_Y - 3 ? Blocks.DIRT.defaultBlockState()
                            : Math.floorMod(x * 13 + y * 7 + z * 5, 9) == 0 ? Blocks.ANDESITE.defaultBlockState()
                            : Blocks.STONE.defaultBlockState();
                    level.setBlock(pos.set(center.getX() + x, y, center.getZ() + z), state, FLAGS);
                }
            }
        }
    }

    private static void arena(ServerLevel level, BlockPos center) {
        for (int x = -ARENA_RADIUS - 1; x <= ARENA_RADIUS + 1; x++) {
            for (int z = -ARENA_RADIUS - 1; z <= ARENA_RADIUS + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > ARENA_RADIUS + 0.5) {
                    continue;
                }
                BlockState state = d > ARENA_RADIUS - 1 ? Blocks.STONE_BRICKS.defaultBlockState()
                        : d < 2.5 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_ANDESITE.defaultBlockState();
                top(level, center, x, z, state);
            }
        }
    }

    /** Dirt paths from the arrival point to the arena, and from the arena to the NPC zone. */
    private static void paths(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = ARENA_RADIUS + 1; z < ARRIVAL_Z - 2; z++) {
                top(level, center, x, z, Blocks.DIRT_PATH.defaultBlockState());
            }
            for (int z = NPC_Z + NPC_SIZE; z < -ARENA_RADIUS; z++) {
                top(level, center, x, z, Blocks.DIRT_PATH.defaultBlockState());
            }
        }
    }

    /** An empty paved square kept for a future NPC: only the floor, nothing else yet. */
    private static void npcZone(ServerLevel level, BlockPos center) {
        for (int x = 0; x < NPC_SIZE; x++) {
            for (int z = 0; z < NPC_SIZE; z++) {
                boolean border = x == 0 || z == 0 || x == NPC_SIZE - 1 || z == NPC_SIZE - 1;
                top(level, center, NPC_X + x, NPC_Z + z, border ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
    }

    private static void arrival(ServerLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x * x + z * z > 5) {
                    continue;
                }
                top(level, center, x, ARRIVAL_Z + z, x == 0 && z == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
    }

    private static void pond(ServerLevel level, BlockPos center) {
        int px = 20;
        int pz = 12;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 5; z++) {
                double e = (x * x) / 25.0 + (z * z) / 12.25;
                if (e > 1) {
                    continue;
                }
                int wx = center.getX() + px + x;
                int wz = center.getZ() + pz + z;
                level.setBlock(pos.set(wx, SURFACE_Y - 2, wz), Blocks.CLAY.defaultBlockState(), FLAGS);
                level.setBlock(pos.set(wx, SURFACE_Y - 1, wz), e < 0.5 ? Blocks.WATER.defaultBlockState()
                        : Blocks.SAND.defaultBlockState(), FLAGS);
                level.setBlock(pos.set(wx, SURFACE_Y, wz), Blocks.WATER.defaultBlockState(), FLAGS);
            }
        }
    }

    private static void pine(ServerLevel level, BlockPos base, int height) {
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.set(base.getX(), base.getY() + y, base.getZ()), log, FLAGS);
        }
        // Rings of leaves narrowing to a tip, from two blocks above the ground.
        for (int y = 2; y <= height + 1; y++) {
            int r = y >= height ? (y == height + 1 ? 0 : 1) : (height - y) % 2 == 0 ? 2 : 1;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if ((x == 0 && z == 0 && y < height) || (Math.abs(x) == r && Math.abs(z) == r && r > 0)) {
                        continue;
                    }
                    pos.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, leaves, FLAGS);
                    }
                }
            }
        }
    }

    private static void floatingStone(ServerLevel level, BlockPos c, int r) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r - 1; y <= 0; y++) {
                    double d = Math.sqrt(x * x + z * z) + Math.max(0, -y - 1) * 0.8;
                    if (d > r + 0.3) {
                        continue;
                    }
                    BlockState state = y == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    level.setBlock(pos.set(c.getX() + x, c.getY() + y, c.getZ() + z), state, FLAGS);
                }
            }
        }
    }

    /** Grass tufts and flowers on the grass, never in the arena, on paths or in the special zones. */
    private static void flora(ServerLevel level, BlockPos center) {
        RandomSource random = RandomSource.create(center.asLong());
        BlockState[] plants = {Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(),
                Blocks.FERN.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()};
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 160; i++) {
            int x = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            pos.set(center.getX() + x, SURFACE_Y, center.getZ() + z);
            if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), plants[random.nextInt(plants.length)], FLAGS);
            }
        }
    }

    private static void top(ServerLevel level, BlockPos center, int x, int z, BlockState state) {
        level.setBlock(new BlockPos(center.getX() + x, SURFACE_Y, center.getZ() + z), state, FLAGS);
    }
}
