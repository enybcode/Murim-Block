package com.murimblock.cultivation.inner;

import static com.murimblock.cultivation.inner.InnerIsland.GROUND;
import static com.murimblock.cultivation.inner.InnerIsland.noise;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.BuildKit;
import com.murimblock.world.structure.kit.RoofTiles;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

/**
 * What makes each martial art's inner world its own (InnerTheme, docs/cultivation/inner-worlds.md): the island's ground
 * and stone, its trees once the realm grows them, its water, its floating stones, and its landmarks. Landmarks keep clear
 * of the meditation platform, the steles round it, the Heart Demon's place in front (north) and the old soul's.
 */
final class InnerWorlds {
    private InnerWorlds() {
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState leaves(MurimBlocks.Wood wood) {
        return wood.leaves().get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A place a landmark may take: off the platform and its steles, the demon's and the old soul's places, on the island. */
    static boolean free(int x, int z, int radius) {
        double d = Math.hypot(x, z);
        if (d < 6 || d > radius - 1) return false;
        if (Math.abs(x) <= 2 && z <= -3 && z >= -8) return false;
        if (Math.hypot(x - radius * 0.3, z - radius * 0.65) < 2.5) return false;
        return Math.hypot(x - radius * 0.5, z - radius * 0.35) >= 3;
    }

    /** A point at this angle (radians, 0 = east, π/2 = south) and share of the radius, at least six blocks out. */
    static int[] at(double angle, double share, int radius) {
        double r = Math.max(6, radius * share);
        return new int[]{(int) Math.round(Math.cos(angle) * r), (int) Math.round(Math.sin(angle) * r)};
    }

    // ---------------------------------------------------------------- the ground

    /** Smooth patches (0..1) a few blocks across, rather than a speckle of single blocks. */
    static double patch(int x, int z, int salt) {
        int cell = 4, gx = Math.floorDiv(x, cell), gz = Math.floorDiv(z, cell);
        double fx = (x - gx * cell) / (double) cell, fz = (z - gz * cell) / (double) cell;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = noise(gx, gz, salt), b = noise(gx + 1, gz, salt), c = noise(gx, gz + 1, salt), e = noise(gx + 1, gz + 1, salt);
        double v = (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + e * fx) * fz;
        return Math.max(0, Math.min(1, (v - 0.5) * 1.9 + 0.5));
    }

    /** The island's top block here (d: the distance from the middle). */
    static BlockState top(InnerTheme t, int x, int z, double d, int radius) {
        double n = patch(x, z, 3);
        return switch (t) {
            case PLAIN, AZURE_CLOUD, TRAVELLER_STAFF -> n < 0.15 ? s(Blocks.MOSS_BLOCK) : s(Blocks.GRASS_BLOCK);
            case LONG_FIST -> d < radius * 0.6 ? s(Blocks.STONE_BRICKS) : n < 0.3 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRASS_BLOCK);
            case GOLDEN_BELL -> d < radius * 0.7 ? (vein(x, z) ? s(Blocks.RAW_GOLD_BLOCK) : s(Blocks.SMOOTH_STONE)) : s(Blocks.GRASS_BLOCK);
            case CELESTIAL_PEAK -> n < 0.2 ? s(Blocks.SNOW_BLOCK) : s(Blocks.GRASS_BLOCK);
            case THUNDER_SABER -> n < 0.3 ? s(Blocks.POLISHED_BLACKSTONE) : n < 0.45 ? s(Blocks.BASALT) : s(Blocks.BLACKSTONE);
            case SEVEN_STARS -> d < radius * 0.45 ? s(Blocks.POLISHED_DEEPSLATE) : n < 0.25 ? s(Blocks.MOSS_BLOCK) : s(Blocks.GRASS_BLOCK);
            case MOUNTAIN_SHATTERING -> vein(x, z) && d > 4 && noise(x, z, 94) < 0.6 ? s(Blocks.MAGMA_BLOCK) : n < 0.3 ? s(Blocks.GRAVEL) : n < 0.5 ? s(Blocks.COARSE_DIRT) : s(Blocks.STONE);
            case OVERLORD_SPEAR -> n < 0.35 ? s(MurimBlocks.RED_LOESS.get()) : n < 0.6 ? s(Blocks.COARSE_DIRT) : s(Blocks.ROOTED_DIRT);
            case SANGUINE -> n < 0.3 ? s(Blocks.NETHER_WART_BLOCK) : s(Blocks.RED_TERRACOTTA);
            case DEVOURING_DEMON -> n < 0.3 ? s(Blocks.SCULK) : n < 0.5 ? s(Blocks.DEEPSLATE) : s(Blocks.BLACKSTONE);
            case MAJIN_FIST -> d < radius * 0.75 ? (vein(x, z) ? s(Blocks.RED_NETHER_BRICKS) : n < 0.3 ? s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) : s(Blocks.POLISHED_BLACKSTONE_BRICKS)) : s(Blocks.BLACKSTONE);
        };
    }

    /** Thin winding veins across the ground. */
    private static boolean vein(int x, int z) {
        double v = Math.sin(x * 0.55 + Math.sin(z * 0.3) * 2) + Math.cos(z * 0.45 + Math.sin(x * 0.25) * 2);
        return Math.abs(v) < 0.12;
    }

    /** The two blocks under the top. */
    static BlockState soil(InnerTheme t) {
        return switch (t) {
            case THUNDER_SABER, MAJIN_FIST -> s(Blocks.BLACKSTONE);
            case MOUNTAIN_SHATTERING -> s(Blocks.STONE);
            case OVERLORD_SPEAR -> s(MurimBlocks.RED_LOESS.get());
            case SANGUINE -> s(Blocks.RED_TERRACOTTA);
            case DEVOURING_DEMON, SEVEN_STARS -> s(Blocks.DEEPSLATE);
            default -> s(Blocks.DIRT);
        };
    }

    /** The rock the island tapers down in (two of them, mixed). */
    static BlockState rock(InnerTheme t, boolean other) {
        return switch (t) {
            case PLAIN, AZURE_CLOUD, TRAVELLER_STAFF -> other ? s(MurimBlocks.MOSSY_KARST_LIMESTONE.get()) : s(MurimBlocks.KARST_LIMESTONE.get());
            case LONG_FIST, GOLDEN_BELL -> other ? s(Blocks.STONE) : s(MurimBlocks.PALE_GRANITE.get());
            case CELESTIAL_PEAK -> other ? s(Blocks.STONE) : s(MurimBlocks.KUNLUN_ROCK.get());
            case THUNDER_SABER -> other ? s(Blocks.AMETHYST_BLOCK) : s(MurimBlocks.KUNLUN_ROCK.get());
            case SEVEN_STARS -> other ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.DEEPSLATE);
            case MOUNTAIN_SHATTERING -> other ? s(Blocks.TUFF) : s(Blocks.STONE);
            case OVERLORD_SPEAR -> other ? s(Blocks.RED_SANDSTONE) : s(Blocks.TERRACOTTA);
            case SANGUINE -> other ? s(Blocks.REDSTONE_BLOCK) : s(Blocks.BLACKSTONE);
            case DEVOURING_DEMON, MAJIN_FIST -> other ? s(Blocks.CRYING_OBSIDIAN) : s(Blocks.BLACKSTONE);
        };
    }

    /** Whether the ground here is eaten away (the Devouring Demon's island, gnawed at its edges). */
    static boolean eaten(InnerTheme t, int x, int z, double d, int radius) {
        return t == InnerTheme.DEVOURING_DEMON && d > radius * 0.72 && noise(x, z, 41) < 0.4;
    }

    /** Whether grass may sprout here. */
    static boolean grassy(InnerTheme t) {
        return t == InnerTheme.PLAIN || t == InnerTheme.AZURE_CLOUD || t == InnerTheme.TRAVELLER_STAFF || t == InnerTheme.LONG_FIST
                || t == InnerTheme.CELESTIAL_PEAK || t == InnerTheme.GOLDEN_BELL || t == InnerTheme.SEVEN_STARS;
    }

    // ---------------------------------------------------------------- water, trees, floating stones

    /** Whether the pond is this theme's (the others have their own water or none). */
    static boolean pond(InnerTheme t) {
        return switch (t) {
            case PLAIN, AZURE_CLOUD, LONG_FIST, GOLDEN_BELL, CELESTIAL_PEAK, TRAVELLER_STAFF -> true;
            default -> false;
        };
    }

    /** The trees the island grows from Qi Condensation, in its own kind. */
    static void trees(BuildKit kit, InnerTheme t, int radius) {
        double[] angles = {0.0, 4.25, 5.6};
        for (double a : angles) {
            int x = (int) Math.round(Math.cos(a) * (radius - 2.5)), z = (int) Math.round(Math.sin(a) * (radius - 2.5));
            int h = 4 + (int) (noise(x, z, 6) * 3);
            switch (t) {
                case LONG_FIST -> round(kit, x, z, s(MurimBlocks.JUJUBE.log().get()), leaves(MurimBlocks.JUJUBE), h);
                case GOLDEN_BELL -> round(kit, x, z, s(MurimBlocks.OSMANTHUS.log().get()), leaves(MurimBlocks.OSMANTHUS), h);
                case CELESTIAL_PEAK -> cone(kit, x, z, s(MurimBlocks.TIANSHAN_SPRUCE.log().get()), leaves(MurimBlocks.TIANSHAN_SPRUCE), h + 3);
                case TRAVELLER_STAFF -> round(kit, x, z, s(MurimBlocks.WILLOW.log().get()), leaves(MurimBlocks.WILLOW), h);
                case SEVEN_STARS -> round(kit, x, z, s(MurimBlocks.DOVE_TREE.log().get()), leaves(MurimBlocks.DOVE_TREE), h);
                case THUNDER_SABER, OVERLORD_SPEAR -> dead(kit, x, z, s(t == InnerTheme.THUNDER_SABER ? Blocks.BASALT : Blocks.DARK_OAK_LOG), h);
                case SANGUINE -> round(kit, x, z, s(Blocks.CRIMSON_STEM), s(Blocks.NETHER_WART_BLOCK), h);
                case MOUNTAIN_SHATTERING, DEVOURING_DEMON, MAJIN_FIST -> { }
                default -> { }
            }
        }
    }

    private static void round(BuildKit kit, int x, int z, BlockState log, BlockState leaves, int h) {
        for (int y = 1; y <= h; y++) kit.setRel(x, GROUND + y, z, log);
        for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
            double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
            if (d > 2.4 || dx == 0 && dz == 0 && dy <= 0) continue;
            if (d > 2.0 && noise(x + dx, z + dz, 60 + dy) < 0.4) continue;
            kit.setRel(x + dx, GROUND + h + dy, z + dz, leaves);
        }
    }

    private static void cone(BuildKit kit, int x, int z, BlockState log, BlockState leaves, int h) {
        for (int y = 1; y <= h; y++) kit.setRel(x, GROUND + y, z, log);
        for (int y = 3; y <= h + 1; y++) {
            int r = Math.max(0, (h + 1 - y) / 2 - ((y & 1) == 0 ? 0 : 1));
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && y <= h)) kit.setRel(x + dx, GROUND + y, z + dz, leaves);
        }
        kit.setRel(x, GROUND + h + 1, z, leaves);
    }

    private static void dead(BuildKit kit, int x, int z, BlockState log, int h) {
        for (int y = 1; y <= h; y++) kit.setRel(x, GROUND + y, z, log);
        kit.setRel(x + 1, GROUND + h - 1, z, log);
        kit.setRel(x + 2, GROUND + h, z, log);
        kit.setRel(x - 1, GROUND + h - 2, z + 1, log);
    }

    /** The floating stones' two blocks (body, top). */
    static BlockState[] floating(InnerTheme t) {
        return switch (t) {
            case PLAIN, AZURE_CLOUD, TRAVELLER_STAFF -> new BlockState[]{s(MurimBlocks.KARST_LIMESTONE.get()), s(Blocks.MOSS_BLOCK)};
            case LONG_FIST, CELESTIAL_PEAK -> new BlockState[]{s(MurimBlocks.PALE_GRANITE.get()), s(Blocks.SNOW_BLOCK)};
            case GOLDEN_BELL -> new BlockState[]{s(MurimBlocks.PALE_GRANITE.get()), s(Blocks.GOLD_BLOCK)};
            case THUNDER_SABER -> new BlockState[]{s(Blocks.BLACKSTONE), s(Blocks.AMETHYST_BLOCK)};
            case SEVEN_STARS -> new BlockState[]{s(Blocks.DEEPSLATE), s(Blocks.SEA_LANTERN)};
            case MOUNTAIN_SHATTERING -> new BlockState[]{s(Blocks.STONE), s(Blocks.MAGMA_BLOCK)};
            case OVERLORD_SPEAR -> new BlockState[]{s(Blocks.TERRACOTTA), s(MurimBlocks.RED_LOESS.get())};
            case SANGUINE -> new BlockState[]{s(Blocks.BLACKSTONE), s(Blocks.REDSTONE_BLOCK)};
            case DEVOURING_DEMON -> new BlockState[]{s(Blocks.BLACKSTONE), s(Blocks.SCULK)};
            case MAJIN_FIST -> new BlockState[]{s(Blocks.BLACKSTONE), s(Blocks.RED_NETHER_BRICKS)};
        };
    }

    // ---------------------------------------------------------------- the landmarks

    static void landmarks(BuildKit kit, InnerTheme t, int radius) {
        switch (t) {
            case AZURE_CLOUD -> swords(kit, radius);
            case LONG_FIST -> yard(kit, radius);
            case GOLDEN_BELL -> bellTower(kit, radius);
            case CELESTIAL_PEAK -> peaks(kit, radius);
            case TRAVELLER_STAFF -> road(kit, radius);
            case THUNDER_SABER -> storm(kit, radius);
            case SEVEN_STARS -> dipper(kit, radius);
            case MOUNTAIN_SHATTERING -> shattered(kit, radius);
            case OVERLORD_SPEAR -> battlefield(kit, radius);
            case SANGUINE -> blood(kit, radius);
            case DEVOURING_DEMON -> devoured(kit, radius);
            case MAJIN_FIST -> arena(kit, radius);
            default -> { }
        }
    }

    /** Swords planted in the rock, blade down, their hilt up. */
    private static void swords(BuildKit kit, int radius) {
        for (int i = 0; i < 6; i++) {
            int[] p = at(2.4 + i * 0.32, 0.72, radius);
            if (!free(p[0], p[1], radius)) continue;
            kit.setRel(p[0], GROUND + 1, p[1], s(Blocks.IRON_BARS));
            kit.setRel(p[0], GROUND + 2, p[1], s(Blocks.IRON_BARS));
            kit.setRel(p[0], GROUND + 3, p[1], s(Blocks.LIGHTNING_ROD));
        }
    }

    /** The Long Fist's yard: weight stones and red banners round it (its dummies are drawn by InnerIsland). */
    private static void yard(BuildKit kit, int radius) {
        for (double a : new double[]{0.6, 1.1}) {
            int[] p = at(a, 0.6, radius);
            if (!free(p[0], p[1], radius)) continue;
            kit.setRel(p[0], GROUND + 1, p[1], s(Blocks.POLISHED_ANDESITE));
            kit.setRel(p[0] + 1, GROUND + 1, p[1], s(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.X));
            kit.setRel(p[0] + 2, GROUND + 1, p[1], s(Blocks.POLISHED_ANDESITE));
        }
        for (double a : new double[]{-0.6, 1.9, 3.8, 5.2}) banner(kit, at(a, 0.85, radius), radius, s(Blocks.RED_WOOL));
    }

    private static void banner(BuildKit kit, int[] p, int radius, BlockState cloth) {
        if (!free(p[0], p[1], radius)) return;
        for (int y = 1; y <= 5; y++) kit.setRel(p[0], GROUND + y, p[1], s(Blocks.SPRUCE_FENCE));
        kit.setRel(p[0] + 1, GROUND + 5, p[1], cloth);
        kit.setRel(p[0] + 1, GROUND + 4, p[1], cloth);
        kit.setRel(p[0] + 1, GROUND + 3, p[1], cloth);
    }

    /** The bell tower: four red pillars, a grey tiled roof, the great bronze bell hanging under it. */
    private static void bellTower(BuildKit kit, int radius) {
        int[] c = at(3.75, 0.62, radius);
        int x = c[0], z = c[1];
        kit.fillRel(x - 2, GROUND, z - 2, x + 2, GROUND, z + 2, s(Blocks.POLISHED_ANDESITE));
        for (int[] p : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}})
            for (int y = 1; y <= 5; y++) kit.setRel(x + p[0], GROUND + y, z + p[1], s(Blocks.STRIPPED_CRIMSON_STEM));
        kit.fillRel(x - 2, GROUND + 6, z - 2, x + 2, GROUND + 6, z + 2, s(Blocks.DARK_OAK_PLANKS));
        kit.roofRel(x - 2, z - 2, x + 2, z + 2, GROUND + 7, RoofTiles.GREY, true);
        kit.setRel(x, GROUND + 5, z, s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        // Golden lanterns at its foot.
        kit.setRel(x - 3, GROUND + 1, z, s(Blocks.LANTERN));
        kit.setRel(x + 3, GROUND + 1, z, s(Blocks.LANTERN));
    }

    /** Three peaks rising beyond the north edge, snow on their heads; spear racks and the Alliance's blue banners. */
    private static void peaks(BuildKit kit, int radius) {
        int[][] peaks = {{-radius / 2 - 3, -(radius + 3), 4, 14}, {0, -(radius + 6), 5, 20}, {radius / 2 + 3, -(radius + 3), 4, 14}};
        for (int[] pk : peaks) {
            int base = pk[2] + radius / 8, h = pk[3] + radius / 3;
            for (int y = -8; y <= h; y++) {
                double r = y < 0 ? base * (1 + y / 10.0) : base * (1 - y / (double) h);
                for (int dx = (int) -Math.ceil(r); dx <= Math.ceil(r); dx++) for (int dz = (int) -Math.ceil(r); dz <= Math.ceil(r); dz++) {
                    double d = Math.hypot(dx, dz) + noise(pk[0] + dx, pk[1] + dz, 70 + y) * 0.8;
                    if (d > r) continue;
                    BlockState b = y > h * 0.65 ? s(Blocks.SNOW_BLOCK) : noise(dx, dz + y, 71) < 0.3 ? s(MurimBlocks.PALE_GRANITE.get()) : s(MurimBlocks.KUNLUN_ROCK.get());
                    kit.setRel(pk[0] + dx, GROUND + y, pk[1] + dz, b);
                }
            }
        }
        for (double a : new double[]{0.5, 2.6}) {
            int[] p = at(a, 0.62, radius);
            if (!free(p[0], p[1], radius) || !free(p[0] + 2, p[1], radius)) continue;
            for (int i = 0; i < 3; i++) {
                kit.setRel(p[0] + i, GROUND + 1, p[1], s(Blocks.SPRUCE_FENCE));
                kit.setRel(p[0] + i, GROUND + 2, p[1], s(Blocks.SPRUCE_FENCE));
                kit.setRel(p[0] + i, GROUND + 3, p[1], s(Blocks.LIGHTNING_ROD));
            }
        }
        for (double a : new double[]{1.6, 4.0, 5.4}) banner(kit, at(a, 0.85, radius), radius, s(Blocks.BLUE_WOOL));
    }

    /** A winding road from the platform south to the edge, lanterns along it, an old tree and a wayside shrine. */
    private static void road(BuildKit kit, int radius) {
        for (int z = 4; z <= radius; z++) {
            int x = (int) Math.round(Math.sin(z * 0.45) * 2.2);
            for (int w = -1; w <= 0; w++) {
                kit.setRel(x + w, GROUND, z, s(Blocks.DIRT_PATH));
                kit.setRel(x + w, GROUND + 1, z, s(Blocks.AIR));
            }
            if (z % 4 == 2 && z > 6) {
                kit.setRel(x + 2, GROUND + 1, z, s(Blocks.SPRUCE_FENCE));
                kit.setRel(x + 2, GROUND + 2, z, s(Blocks.LANTERN));
            }
        }
        int[] tree = at(2.3, 0.62, radius);
        if (free(tree[0], tree[1], radius)) round(kit, tree[0], tree[1], s(MurimBlocks.CAMPHOR.log().get()), leaves(MurimBlocks.CAMPHOR), 6);
        int[] shrine = at(5.2, 0.62, radius);
        if (free(shrine[0], shrine[1], radius)) {
            int x = shrine[0], z = shrine[1];
            kit.fillRel(x - 1, GROUND + 1, z - 1, x + 1, GROUND + 1, z + 1, s(Blocks.STONE_BRICKS));
            kit.setRel(x, GROUND + 2, z, s(Blocks.CHISELED_STONE_BRICKS));
            kit.setRel(x, GROUND + 3, z, s(Blocks.LANTERN));
            kit.fillRel(x - 1, GROUND + 4, z - 1, x + 1, GROUND + 4, z + 1, s(Blocks.DARK_OAK_SLAB));
        }
    }

    /** Iron rods for the lightning, war drums, purple crystals breaking out of the black stone. */
    private static void storm(BuildKit kit, int radius) {
        for (int i = 0; i < 4; i++) {
            int[] p = at(0.4 + i * 1.6, 0.75, radius);
            if (!free(p[0], p[1], radius)) continue;
            for (int y = 1; y <= 3; y++) kit.setRel(p[0], GROUND + y, p[1], s(Blocks.IRON_BLOCK));
            kit.setRel(p[0], GROUND + 4, p[1], s(Blocks.LIGHTNING_ROD));
        }
        for (double a : new double[]{2.2, 0.95}) {
            int[] p = at(a, 0.55, radius);
            if (!free(p[0], p[1], radius)) continue;
            kit.setRel(p[0], GROUND + 1, p[1], s(Blocks.DARK_OAK_FENCE));
            kit.setRel(p[0], GROUND + 2, p[1], s(Blocks.BARREL).setValue(BarrelBlock.FACING, Direction.UP));
            kit.setRel(p[0], GROUND + 3, p[1], s(Blocks.RED_CARPET));
        }
        for (int i = 0; i < 7; i++) {
            int[] p = at(i * 0.9 + 0.2, 0.45 + noise(i, 3, 72) * 0.4, radius);
            if (!free(p[0], p[1], radius)) continue;
            kit.setRel(p[0], GROUND + 1, p[1], s(Blocks.AMETHYST_BLOCK));
            kit.setRel(p[0], GROUND + 2, p[1], s(Blocks.AMETHYST_CLUSTER));
        }
    }

    /** Seven pillars of light in the shape of the Dipper, and a still pool that mirrors the stars. */
    private static void dipper(BuildKit kit, int radius) {
        double[][] stars = {{-7, -3}, {-4.5, -4}, {-2, -3.2}, {0.5, -1.5}, {1, 1.8}, {4.6, 2.2}, {4.8, -1}};
        double scale = Math.max(1.2, radius / 7.0);
        for (int i = 0; i < stars.length; i++) {
            int x = (int) Math.round(stars[i][0] * scale + radius * 0.15), z = (int) Math.round(stars[i][1] * scale - radius * 0.2);
            if (!free(x, z, radius)) continue;
            int h = 3 + (int) (noise(i, 7, 73) * 3);
            for (int y = 1; y <= h; y++) kit.setRel(x, GROUND + y, z, s(Blocks.QUARTZ_PILLAR));
            kit.setRel(x, GROUND + h + 1, z, s(Blocks.SEA_LANTERN));
            kit.setRel(x, GROUND + h + 2, z, s(Blocks.END_ROD));
        }
        int[] c = at(2.6, 0.55, radius);
        int pr = 1 + radius / 6;
        for (int dx = -pr - 1; dx <= pr + 1; dx++) for (int dz = -pr - 1; dz <= pr + 1; dz++) {
            double d = Math.hypot(dx, dz);
            if (d > pr + 1.2 || !free(c[0] + dx, c[1] + dz, radius + 2) && Math.hypot(c[0] + dx, c[1] + dz) < 6) continue;
            kit.setRel(c[0] + dx, GROUND, c[1] + dz, d <= pr ? s(Blocks.WATER) : s(Blocks.POLISHED_DEEPSLATE));
            kit.setRel(c[0] + dx, GROUND + 1, c[1] + dz, s(Blocks.AIR));
            if (d <= pr) kit.setRel(c[0] + dx, GROUND - 1, c[1] + dz, s(Blocks.DEEPSLATE));
        }
    }

    /** A ring of broken ground round the platform, and great blocks of the shattered mountain floating round. */
    private static void shattered(BuildKit kit, int radius) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            double d = Math.hypot(x, z);
            if (d < 4.5 || d > 6.5 || noise(x, z, 74) < 0.3) continue;
            kit.setRel(x, GROUND, z, noise(x, z, 75) < 0.08 ? s(Blocks.MAGMA_BLOCK) : s(Blocks.GRAVEL));
            kit.setRel(x, GROUND + 1, z, s(Blocks.AIR));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.4;
            int dist = radius + 3 + (int) (noise(i, 1, 76) * 6), size = 2 + (int) (noise(i, 2, 77) * 2);
            int x = (int) Math.round(Math.cos(a) * dist), z = (int) Math.round(Math.sin(a) * dist), y = GROUND - 2 + (int) (noise(i, 3, 78) * 12);
            for (int dx = -size; dx <= size; dx++) for (int dy = -size; dy <= size; dy++) for (int dz = -size; dz <= size; dz++) {
                if (Math.abs(dx) + Math.abs(dy) * 1.2 + Math.abs(dz) > size * 1.8 + noise(dx, dz + dy, 79)) continue;
                kit.setRel(x + dx, y + dy, z + dz, noise(x + dx, z + dz + dy, 80) < 0.06 ? s(Blocks.MAGMA_BLOCK) : noise(dx, dy + dz, 81) < 0.4 ? s(Blocks.TUFF) : s(Blocks.STONE));
            }
        }
        // Broken stumps of the mountain on the island.
        for (int i = 0; i < 5; i++) {
            int[] p = at(i * 1.25 + 0.3, 0.75, radius);
            if (!free(p[0], p[1], radius)) continue;
            int h = 2 + (int) (noise(i, 4, 82) * 4);
            for (int y = 1; y <= h; y++) kit.setRel(p[0], GROUND + y, p[1], s(y == h ? Blocks.COBBLESTONE : Blocks.STONE));
        }
    }

    /** Broken spears everywhere, fallen standards, the conqueror's war tent with its fire. */
    private static void battlefield(BuildKit kit, int radius) {
        for (int i = 0; i < 14; i++) {
            int[] p = at(i * 0.45 + noise(i, 0, 83), 0.4 + noise(i, 1, 84) * 0.5, radius);
            if (!free(p[0], p[1], radius)) continue;
            int h = noise(i, 2, 85) < 0.5 ? 3 : 2;
            for (int y = 1; y <= h; y++) kit.setRel(p[0], GROUND + y, p[1], s(Blocks.DARK_OAK_FENCE));
            if (h == 3) kit.setRel(p[0], GROUND + 4, p[1], s(Blocks.LIGHTNING_ROD));
        }
        for (double a : new double[]{0.7, 2.2}) {
            int[] p = at(a, 0.68, radius);
            if (!free(p[0], p[1], radius) || !free(p[0] + 3, p[1], radius)) continue;
            for (int i = 0; i < 3; i++) kit.setRel(p[0] + i, GROUND + 1, p[1], s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            kit.setRel(p[0] + 1, GROUND + 1, p[1] + 1, s(Blocks.RED_CARPET));
            kit.setRel(p[0] + 2, GROUND + 1, p[1] + 1, s(Blocks.RED_CARPET));
        }
        int[] c = at(3.9, 0.62, radius);
        int x = c[0], z = c[1];
        for (int dz = -2; dz <= 2; dz++) for (int h = 0; h <= 3; h++) {
            kit.setRel(x - 3 + h, GROUND + 1 + h, z + dz, s(Blocks.RED_WOOL));
            kit.setRel(x + 3 - h, GROUND + 1 + h, z + dz, s(Blocks.RED_WOOL));
        }
        kit.setRel(x, GROUND + 4, z - 3, s(Blocks.DARK_OAK_FENCE));
        kit.setRel(x, GROUND + 5, z - 3, s(Blocks.RED_WOOL));
        kit.setRel(x, GROUND + 1, z + 4, s(Blocks.CAMPFIRE));
    }

    /** A lake of blood and spires of blood crystal. */
    private static void blood(BuildKit kit, int radius) {
        int[] c = at(0.7, 0.5, radius);
        int pr = 2 + radius / 6;
        for (int dx = -pr; dx <= pr; dx++) for (int dz = -pr; dz <= pr; dz++) {
            double d = Math.hypot(dx, dz) + noise(dx, dz, 86);
            int x = c[0] + dx, z = c[1] + dz;
            if (d > pr || Math.hypot(x, z) < 5 || Math.hypot(x, z) > radius - 1) continue;
            kit.setRel(x, GROUND, z, s(Blocks.RED_STAINED_GLASS));
            kit.setRel(x, GROUND - 1, z, s(Blocks.RED_CONCRETE));
            kit.setRel(x, GROUND + 1, z, s(Blocks.AIR));
        }
        for (int i = 0; i < 6; i++) {
            int[] p = at(2.0 + i * 0.75, 0.55 + noise(i, 5, 87) * 0.3, radius);
            if (!free(p[0], p[1], radius)) continue;
            int h = 4 + (int) (noise(i, 6, 88) * 4);
            for (int y = 1; y <= h; y++) {
                kit.setRel(p[0], GROUND + y, p[1], y < h / 2 ? s(Blocks.REDSTONE_BLOCK) : s(Blocks.RED_STAINED_GLASS));
                if (y <= h / 3) for (Direction dir : Direction.Plane.HORIZONTAL)
                    kit.setRel(p[0] + dir.getStepX(), GROUND + y, p[1] + dir.getStepZ(), s(Blocks.RED_STAINED_GLASS));
            }
        }
    }

    /** Arches of ribs over the island, debris of the eaten edges falling away, the red eye in the north sky. */
    private static void devoured(BuildKit kit, int radius) {
        int span = Math.max(6, radius - 2);
        for (int zi = -1; zi <= 1; zi++) {
            int z = zi * Math.max(3, radius / 3) + 2;
            for (double a = 0.15; a < Math.PI - 0.1; a += 0.04) {
                int x = (int) Math.round(Math.cos(a) * span), y = GROUND + (int) Math.round(Math.sin(a) * span * 0.9);
                kit.setRel(x, y, z, s(Blocks.BONE_BLOCK));
                kit.setRel(x, y, z + 1, s(Blocks.BONE_BLOCK));
            }
        }
        for (int i = 0; i < 10; i++) {
            double a = i * 0.63;
            int dist = radius + 1 + (int) (noise(i, 0, 89) * 5);
            int x = (int) Math.round(Math.cos(a) * dist), z = (int) Math.round(Math.sin(a) * dist), y = GROUND - 3 - (int) (noise(i, 1, 90) * 10);
            kit.setRel(x, y, z, s(Blocks.BLACKSTONE));
            kit.setRel(x + 1, y, z, s(Blocks.SCULK));
            kit.setRel(x, y - 1, z, s(Blocks.BLACKSTONE));
        }
        // The eye, watching from the north, high up.
        int ez = -(radius + 16), ey = GROUND + 20;
        BlockState glow = s(Blocks.LIGHT).setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
        for (int dx = -12; dx <= 12; dx++) for (int dy = -7; dy <= 7; dy++) {
            double lens = Math.pow(dx / 12.0, 2) + Math.pow(dy / 7.0, 2);
            if (lens > 1) continue;
            double d = Math.hypot(dx, dy);
            BlockState b = lens > 0.78 ? s(Blocks.BLACK_CONCRETE) : d <= 1.6 ? s(Blocks.BLACK_CONCRETE) : d <= 5 ? s(Blocks.REDSTONE_BLOCK) : s(Blocks.WHITE_CONCRETE);
            if (dx == -2 && dy == 3 || dx == -3 && dy == 3) b = s(Blocks.SEA_LANTERN);
            kit.setRel(dx, ey + dy, ez, b);
            if ((dx + dy) % 3 == 0) kit.setRel(dx, ey + dy, ez + 2, glow);
        }
    }

    /** A low ring wall of black stone with gaps, chains hanging from the void, braziers of dark flames. */
    private static void arena(BuildKit kit, int radius) {
        double ring = Math.max(6.5, radius * 0.8);
        for (double a = 0; a < Math.PI * 2; a += 0.05) {
            if (Math.sin(a * 6) > 0.7) continue;
            int x = (int) Math.round(Math.cos(a) * ring), z = (int) Math.round(Math.sin(a) * ring);
            if (Math.abs(x) <= 2 && z < 0) continue;
            kit.setRel(x, GROUND + 1, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            if (noise(x, z, 91) < 0.5) kit.setRel(x, GROUND + 2, z, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
        }
        for (int i = 0; i < 6; i++) {
            int[] p = at(i * 1.05 + 0.5, 0.55, radius);
            int top = GROUND + 14 + (int) (noise(i, 0, 92) * 4), bottom = GROUND + 4 + (int) (noise(i, 1, 93) * 4);
            for (int y = bottom; y < top; y++) kit.setRel(p[0], y, p[1], s(Blocks.CHAIN));
            kit.setRel(p[0], top, p[1], s(Blocks.BLACKSTONE));
        }
        for (double a : new double[]{0.8, 2.35, 3.9, 5.5}) {
            int[] p = at(a, 0.62, radius);
            if (!free(p[0], p[1], radius)) continue;
            kit.setRel(p[0], GROUND + 1, p[1], s(Blocks.POLISHED_BLACKSTONE));
            kit.setRel(p[0], GROUND + 2, p[1], s(Blocks.SOUL_CAMPFIRE).setValue(CampfireBlock.LIT, true));
        }
    }

    /** A lantern hanging state, for the themes' lamps. */
    static BlockState hangingLantern() {
        return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
    }
}
