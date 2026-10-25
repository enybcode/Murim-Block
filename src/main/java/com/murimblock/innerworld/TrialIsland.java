package com.murimblock.innerworld;

import static com.murimblock.innerworld.InnerIsland.ARENA_RADIUS;
import static com.murimblock.innerworld.InnerIsland.ARRIVAL_Z;
import static com.murimblock.innerworld.InnerIsland.GATE_Z;
import static com.murimblock.innerworld.InnerIsland.NPC_SIZE;
import static com.murimblock.innerworld.InnerIsland.NPC_X;
import static com.murimblock.innerworld.InnerIsland.NPC_Z;
import static com.murimblock.innerworld.InnerIsland.RADIUS;
import static com.murimblock.innerworld.InnerIsland.SURFACE_Y;
import static com.murimblock.innerworld.InnerIsland.hash;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The island of the mortal realm's trial (mockup "Graveyard Inner World", Enzo 25 Oct 2026): the inner island's
 * footprint and arena turned into a graveyard at night. Blackstone arena with a bone taiji split by crying obsidian
 * cracks, soul lantern posts, rows of graves around it (zombies climb out of them), dead trees, a broken iron fence,
 * a dark gate, a stone mausoleum on the north zone and a black pool. Built in the trial dimension at the same
 * coordinates as the player's own island. Offsets from the center: +x east, +z south.
 */
public final class TrialIsland {
    /** Raise when the layout changes: trial islands built with an older version are rebuilt on the next trial. */
    public static final int VERSION = 1;
    /** Where the big zombie of the last wave appears: inside the mausoleum. */
    public static final BlockPos WARDEN = new BlockPos(0, SURFACE_Y + 1, NPC_Z + 5);
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final double TAIJI_RADIUS = 6.6;
    private static final int POND_X = 20;
    private static final int POND_Z = 12;
    private static final int[][] TREES = {{-22, -20, 9}, {-26, -6, 8}, {27, -9, 9}, {-12, 27, 7}, {-28, 8, 7},
            {-20, 16, 7}, {25, 1, 8}, {17, -22, 7}};
    /** Floating islets: x, y above the surface, z, radius, decor (0 headstone, 1 dead tree). */
    private static final int[][] ISLETS = {{-42, 8, -34, 3, 1}, {44, 12, -28, 2, 0}, {40, -4, 34, 3, 1},
            {-38, 14, 30, 2, 0}, {6, 18, -46, 2, 0}, {-48, 0, 4, 2, 1}};
    /** Where zombies climb out: the mound in front of each grave, offsets from the center at surface level. */
    public static final List<BlockPos> GRAVES = graves();

    private final ServerLevel level;
    private final BlockPos center;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private TrialIsland(ServerLevel level, BlockPos center) {
        this.level = level;
        this.center = center;
    }

    public static void build(ServerLevel level, BlockPos center, boolean clearFirst) {
        TrialIsland island = new TrialIsland(level, center);
        if (clearFirst) {
            island.clear();
        }
        island.ground();
        island.arena();
        island.paths();
        island.mausoleum();
        island.gate();
        island.pond();
        for (int[] t : TREES) {
            island.deadTree(t[0], t[1], t[2]);
        }
        for (BlockPos g : GRAVES) {
            island.grave(g.getX(), g.getZ() - 1);
        }
        island.fence();
        for (int[] i : ISLETS) {
            island.islet(i[0], SURFACE_Y + i[1], i[2], i[3], i[4]);
        }
    }

    /** Graves on a loose grid in the ring between the arena and the shore, clear of paths, pond, trees and mausoleum. */
    private static List<BlockPos> graves() {
        List<BlockPos> spots = new ArrayList<>();
        for (int gz = -29; gz <= 28; gz += 4) {
            for (int gx = -29; gx <= 28; gx += 3) {
                int x = gx + (int) Math.round((hash(gx, gz, 21) - 0.5) * 2);
                int z = gz + (int) Math.round((hash(gx, gz, 22) - 0.5) * 1.2);
                double d = Math.sqrt(x * x + z * z);
                double far = Math.sqrt(x * x + (z + 2) * (z + 2));
                if (d < ARENA_RADIUS + 4 || d > RADIUS - 4.5 || far > RADIUS - 3 || Math.abs(x) <= 3
                        || pondShape(x, z) <= 2.2 || pondShape(x, z + 2) <= 2.2 || nearMausoleum(x, z) || nearMausoleum(x, z + 2)
                        || nearTree(x, z) || hash(gx, gz, 23) < 0.18) {
                    continue;
                }
                spots.add(new BlockPos(x, SURFACE_Y, z + 1));
            }
        }
        return List.copyOf(spots);
    }

    private static boolean nearMausoleum(int x, int z) {
        return x >= NPC_X - 2 && x <= NPC_X + NPC_SIZE + 1 && z >= NPC_Z - 2 && z <= NPC_Z + NPC_SIZE + 1;
    }

    private static boolean nearTree(int x, int z) {
        for (int[] t : TREES) {
            if (Math.abs(x - t[0]) <= 2 && Math.abs(z - t[1]) <= 3) {
                return true;
            }
        }
        return false;
    }

    private static double pondShape(int x, int z) {
        double dx = x - POND_X;
        double dz = z - POND_Z;
        return dx * dx / 22.0 + dz * dz / 11.0;
    }

    private void clear() {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -InnerIsland.CLEAR_RADIUS; x <= InnerIsland.CLEAR_RADIUS; x++) {
            for (int z = -InnerIsland.CLEAR_RADIUS; z <= InnerIsland.CLEAR_RADIUS; z++) {
                for (int y = SURFACE_Y - InnerIsland.CLEAR_BELOW; y <= SURFACE_Y + InnerIsland.CLEAR_ABOVE; y++) {
                    at(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, FLAGS);
                    }
                }
            }
        }
    }

    // ---- Ground ----

    /** Dead grass, podzol and coarse dirt on top; dirt, then dark rock in an upside-down cone; roots hanging under. */
    private void ground() {
        for (int x = -RADIUS - 3; x <= RADIUS + 3; x++) {
            for (int z = -RADIUS - 3; z <= RADIUS + 3; z++) {
                double d = Math.sqrt(x * x + z * z);
                double edge = InnerIsland.edge(Math.atan2(z, x));
                if (d > edge) {
                    continue;
                }
                int depth = 3 + (int) (22 * Math.pow(1 - d / edge, 0.8)) + (int) (hash(x, z, 1) * 3);
                int bottom = SURFACE_Y - depth;
                for (int y = bottom; y <= SURFACE_Y; y++) {
                    set(x, y, z, groundBlock(x, y, z, d > edge - 1.5));
                }
                if (d > edge - 3 && hash(x, z, 7) < 0.4) {
                    set(x, bottom - 1, z, Blocks.HANGING_ROOTS.defaultBlockState());
                }
            }
        }
    }

    private static BlockState groundBlock(int x, int y, int z, boolean rim) {
        if (y == SURFACE_Y) {
            double h = hash(x, z, 31);
            return h < 0.42 ? Blocks.PODZOL.defaultBlockState() : h < 0.68 ? Blocks.COARSE_DIRT.defaultBlockState()
                    : h < 0.9 ? Blocks.GRASS_BLOCK.defaultBlockState() : h < 0.96 ? Blocks.ROOTED_DIRT.defaultBlockState()
                    : Blocks.SOUL_SOIL.defaultBlockState();
        }
        if (y >= SURFACE_Y - 3) {
            return rim && hash(x, z, y) < 0.3 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        double h = hash(x, z, y);
        if (rim && h < 0.3) {
            return h < 0.15 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        return switch (Math.floorMod(y + x / 5, 7)) {
            case 0 -> Blocks.TUFF.defaultBlockState();
            case 3, 4 -> Blocks.DEEPSLATE.defaultBlockState();
            default -> Blocks.STONE.defaultBlockState();
        };
    }

    // ---- Arena ----

    /** Polished blackstone, deepslate spokes, a bone and blackstone taiji, crying obsidian cracks, soul lanterns. */
    private void arena() {
        for (int x = -ARENA_RADIUS - 1; x <= ARENA_RADIUS + 1; x++) {
            for (int z = -ARENA_RADIUS - 1; z <= ARENA_RADIUS + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= ARENA_RADIUS + 0.5) {
                    set(x, SURFACE_Y, z, arenaBlock(x, z, d));
                }
            }
        }
        // Six cracks wander out from the center: the seal of the trial.
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3 + hash(k, 0, 41) * 0.5;
            double length = 9 + hash(k, 1, 41) * 4;
            for (double r = 1.5; r < length; r += 0.7) {
                angle += (hash(k, (int) (r * 10), 42) - 0.5) * 0.35;
                set((int) Math.round(Math.cos(angle) * r), SURFACE_Y, (int) Math.round(Math.sin(angle) * r),
                        Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }
        }
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI / 8 + k * Math.PI / 4;
            soulPost((int) Math.round(Math.cos(angle) * (ARENA_RADIUS + 2.5)),
                    (int) Math.round(Math.sin(angle) * (ARENA_RADIUS + 2.5)));
        }
    }

    private static BlockState arenaBlock(int x, int z, double d) {
        double h = hash(x, z, 3);
        if (d > ARENA_RADIUS - 1) {
            return h < 0.3 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : h < 0.5 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (d <= TAIJI_RADIUS) {
            return InnerIsland.taiji(x, z, TAIJI_RADIUS) ? Blocks.BONE_BLOCK.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (d <= TAIJI_RADIUS + 1 || d > 11 && d <= 12) {
            return h < 0.25 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        double toSpoke = Math.abs(Math.IEEEremainder(Math.atan2(z, x), Math.PI / 4)) * d;
        if (toSpoke < 0.6 && d < ARENA_RADIUS - 2) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        return h < 0.08 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }

    private void soulPost(int x, int z) {
        set(x, SURFACE_Y, z, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        set(x, SURFACE_Y + 1, z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        set(x, SURFACE_Y + 2, z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        set(x, SURFACE_Y + 3, z, Blocks.SOUL_LANTERN.defaultBlockState());
    }

    // ---- Paths and places ----

    /** Gravel and cobblestone paths from the arrival point to the arena and on to the mausoleum; the arrival circle. */
    private void paths() {
        for (int z = -ARENA_RADIUS - 1; z >= NPC_Z + NPC_SIZE; z--) {
            pathRow(z);
        }
        for (int z = ARENA_RADIUS + 1; z <= ARRIVAL_Z - 2; z++) {
            pathRow(z);
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = ARRIVAL_Z - 3; z <= ARRIVAL_Z + 3; z++) {
                if (x * x + (z - ARRIVAL_Z) * (z - ARRIVAL_Z) <= 9) {
                    set(x, SURFACE_Y, z, hash(x, z, 51) < 0.35 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : Blocks.STONE_BRICKS.defaultBlockState());
                }
            }
        }
        soulPost(-3, ARRIVAL_Z + 3);
        soulPost(3, ARRIVAL_Z + 3);
    }

    private void pathRow(int z) {
        for (int x = -1; x <= 1; x++) {
            double h = hash(x, z, 52);
            set(x, SURFACE_Y, z, h < 0.45 ? Blocks.GRAVEL.defaultBlockState() : h < 0.75 ? Blocks.COBBLESTONE.defaultBlockState()
                    : Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        }
    }

    /** A small stone mausoleum on the north zone: three walls, two front pillars, a tiled roof, a candle altar. */
    private void mausoleum() {
        int x0 = NPC_X;
        int x1 = NPC_X + NPC_SIZE - 1;
        int z0 = NPC_Z;
        int z1 = NPC_Z + NPC_SIZE - 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                set(x, SURFACE_Y, z, Blocks.STONE_BRICKS.defaultBlockState());
                boolean wall = x == x0 || x == x1 || z == z0;
                boolean pillar = z == z1 && (x == x0 || x == x1);
                for (int y = 1; y <= 4; y++) {
                    if (wall || pillar) {
                        double h = hash(x, z * 7 + y, 61);
                        set(x, SURFACE_Y + y, z, h < 0.25 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                                : h < 0.4 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                    } else {
                        set(x, SURFACE_Y + y, z, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                set(x, SURFACE_Y + 5, z, Blocks.DEEPSLATE_TILES.defaultBlockState());
            }
            set(x, SURFACE_Y + 6, (z0 + z1) / 2, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        }
        for (int x = -2; x <= 1; x++) {
            set(x, SURFACE_Y + 1, z0 + 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
            if (x != -1) {
                set(x, SURFACE_Y + 2, z0 + 1, Blocks.CANDLE.defaultBlockState()
                        .setValue(CandleBlock.CANDLES, 1 + (x + 2) % 3).setValue(CandleBlock.LIT, true));
            }
        }
        set(-1, SURFACE_Y + 2, z0 + 1, Blocks.SKELETON_SKULL.defaultBlockState());
    }

    /** The gate over the south path, in dark wood, with two hanging soul lanterns and cobwebs. */
    private void gate() {
        for (int y = 1; y <= 5; y++) {
            set(-3, SURFACE_Y + y, GATE_Z, Blocks.DARK_OAK_LOG.defaultBlockState());
            set(3, SURFACE_Y + y, GATE_Z, Blocks.DARK_OAK_LOG.defaultBlockState());
        }
        for (int x = -4; x <= 4; x++) {
            set(x, SURFACE_Y + 6, GATE_Z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        for (int x = -5; x <= 5; x++) {
            set(x, SURFACE_Y + 7, GATE_Z, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        }
        set(-1, SURFACE_Y + 5, GATE_Z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(1, SURFACE_Y + 5, GATE_Z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(-2, SURFACE_Y + 5, GATE_Z, Blocks.COBWEB.defaultBlockState());
        set(2, SURFACE_Y + 5, GATE_Z, Blocks.COBWEB.defaultBlockState());
    }

    /** A still black pool in a mud basin, mossy stones on its rim. */
    private void pond() {
        for (int x = POND_X - 6; x <= POND_X + 6; x++) {
            for (int z = POND_Z - 5; z <= POND_Z + 5; z++) {
                double s = pondShape(x, z);
                if (s <= 1) {
                    set(x, SURFACE_Y, z, Blocks.WATER.defaultBlockState());
                    set(x, SURFACE_Y - 1, z, s < 0.5 ? Blocks.WATER.defaultBlockState() : Blocks.MUD.defaultBlockState());
                    set(x, SURFACE_Y - 2, z, Blocks.MUD.defaultBlockState());
                } else if (s <= 1.6 && hash(x, z, 71) < 0.35) {
                    set(x, SURFACE_Y + 1, z, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    // ---- Graves, trees, fence, islets ----

    /** A headstone at (x, z) and a mound south of it, sometimes a lit candle at its foot. */
    private void grave(int x, int z) {
        double h = hash(x, z, 81);
        if (h < 0.3) {
            set(x, SURFACE_Y + 1, z, Blocks.ANDESITE_WALL.defaultBlockState());
            if (h < 0.15) {
                set(x, SURFACE_Y + 2, z, Blocks.ANDESITE_WALL.defaultBlockState());
            }
        } else if (h < 0.55) {
            set(x, SURFACE_Y + 1, z, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        } else if (h < 0.75) {
            set(x, SURFACE_Y + 1, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        } else if (h < 0.9) {
            set(x, SURFACE_Y + 1, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
        } else {
            set(x, SURFACE_Y + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        set(x, SURFACE_Y, z + 1, Blocks.COARSE_DIRT.defaultBlockState());
        set(x, SURFACE_Y, z + 2, Blocks.ROOTED_DIRT.defaultBlockState());
        if (hash(x, z, 82) < 0.35) {
            set(x, SURFACE_Y + 1, z + 2, Blocks.CANDLE.defaultBlockState()
                    .setValue(CandleBlock.CANDLES, 1 + (int) (hash(x, z, 83) * 3)).setValue(CandleBlock.LIT, true));
        }
    }

    /** A bare, crooked dark-oak trunk with a few branches near the top. */
    private void deadTree(int x, int z, int height) {
        int px = x;
        int pz = z;
        for (int y = 1; y <= height; y++) {
            set(px, SURFACE_Y + y, pz, Blocks.DARK_OAK_LOG.defaultBlockState());
            double h = hash(x, z * 31 + y, 91);
            if (y > 3 && y < height - 1 && h < 0.3) {
                px += h < 0.15 ? 1 : -1;
                set(px, SURFACE_Y + y, pz, Blocks.DARK_OAK_LOG.defaultBlockState());
            }
            if (y >= height - 3 && hash(x, z * 17 + y, 92) < 0.65) {
                boolean alongX = hash(x, z + y, 93) < 0.5;
                int dir = hash(x + y, z, 94) < 0.5 ? 1 : -1;
                BlockState branch = Blocks.DARK_OAK_LOG.defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
                for (int i = 1; i <= 2; i++) {
                    set(px + (alongX ? dir * i : 0), SURFACE_Y + y + (i == 2 ? 1 : 0), pz + (alongX ? 0 : dir * i), branch);
                }
            }
        }
    }

    /** A broken ring of iron bars near the shore, open at the paths, with deepslate posts. */
    private void fence() {
        int previousX = Integer.MIN_VALUE;
        int previousZ = Integer.MIN_VALUE;
        for (int step = 0; step < 720; step++) {
            double angle = step * Math.PI / 360;
            double r = InnerIsland.edge(angle) - 2;
            int x = (int) Math.round(Math.cos(angle) * r);
            int z = (int) Math.round(Math.sin(angle) * r);
            if (x == previousX && z == previousZ) {
                continue;
            }
            previousX = x;
            previousZ = z;
            if (Math.abs(x) <= 3 || hash(x, z, 101) < 0.22 || pondShape(x, z) <= 1.6) {
                continue;
            }
            boolean post = Math.floorMod(x * 3 + z * 5, 7) == 0;
            set(x, SURFACE_Y + 1, z, post ? Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState());
            if (post) {
                set(x, SURFACE_Y + 2, z, Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState());
            }
        }
    }

    private void islet(int cx, int cy, int cz, int r, int decor) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > r + 0.3) {
                    continue;
                }
                set(cx + x, cy, cz + z, Blocks.PODZOL.defaultBlockState());
                int depth = 1 + (int) Math.round((r + 0.5 - d) * 1.6);
                for (int y = 1; y <= depth; y++) {
                    set(cx + x, cy - y, cz + z, y == 1 ? Blocks.DIRT.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                }
            }
        }
        if (decor == 1) {
            for (int y = 1; y <= 4; y++) {
                set(cx, cy + y, cz, Blocks.DARK_OAK_LOG.defaultBlockState());
            }
            set(cx + 1, cy + 4, cz, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        } else {
            set(cx, cy + 1, cz, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        }
    }

    private BlockPos at(int x, int y, int z) {
        return pos.set(center.getX() + x, y, center.getZ() + z);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(at(x, y, z), state, FLAGS);
    }
}
