package com.murimblock.world.raven;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The endless castle of the Raven Halls (docs/combat/styles/raven-art.md): every chunk of the dimension, at every level
 * (one each {@link #LEVEL} blocks, from the black water at the bottom to the top), is a piece of the same palace with no
 * end. Long covered galleries run along whole rows and columns of chunks out of sight; great red columns rise through
 * every level; between them floors, pavilions upright or hung upside down from the level above, stairs from one level
 * to the next, floors turned on their side like walls, lanterns hanging on chains; and voids, to see the levels go on
 * above and below into the dark. Each piece is drawn from the chunk and level alone (the same everywhere it is made),
 * and stays inside its own chunk.
 */
public class RavenCastleFeature extends Feature<NoneFeatureConfiguration> {
    /** The levels: the first floor's height, the height between floors, the last. */
    static final int BASE = 10, LEVEL = 14, TOP = 370;
    /** The black water at the bottom. */
    static final int WATER = 3;

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState FLOOR = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    static final BlockState TATAMI = Blocks.SPRUCE_PLANKS.defaultBlockState();
    static final BlockState EDGE_X = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    static final BlockState EDGE_Z = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    static final BlockState RED = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
    static final BlockState RED_BEAM = Blocks.MANGROVE_PLANKS.defaultBlockState();
    static final BlockState RAIL = Blocks.MANGROVE_FENCE.defaultBlockState();
    static final BlockState PAPER = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    static final BlockState FRAME = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    static final BlockState TILE = Blocks.DEEPSLATE_TILES.defaultBlockState();
    static final BlockState TILE_SLAB = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
    static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    static final BlockState HANGING = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);

    public RavenCastleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public RavenCastleFeature() {
        this(NoneFeatureConfiguration.CODEC);
    }

    /** A number from a few others, the same each time. */
    static int hash(int a, int b, int c, int salt) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + c * 0x165667B19E3779F9L + salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) (h >>> 33) & 0x7FFFFFFF;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos o = context.origin();
        Piece p = new Piece(level, o.getX() & ~15, o.getZ() & ~15);
        p.water();
        for (int k = 0; BASE + k * LEVEL <= TOP; k++) p.level(k, BASE + k * LEVEL);
        if (hash(p.cx, 0, p.cz, 7) % 6 == 0) p.column();
        return true;
    }

    /** One chunk's share of the castle. */
    static final class Piece {
        final WorldGenLevel level;
        final int x0, z0, cx, cz;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        Piece(WorldGenLevel level, int x0, int z0) {
            this.level = level;
            this.x0 = x0;
            this.z0 = z0;
            this.cx = x0 >> 4;
            this.cz = z0 >> 4;
        }

        void set(int x, int y, int z, BlockState state) {
            if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y > TOP + LEVEL) return;
            level.setBlock(at.set(x0 + x, y, z0 + z), state, 2);
        }

        /** Swapped axes: along x when `alongX`, else along z (u runs along, w across). */
        void set(boolean alongX, int u, int y, int w, BlockState state) {
            if (alongX) set(u, y, w, state);
            else set(w, y, u, state);
        }

        void water() {
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                set(x, 0, z, Blocks.DEEPSLATE.defaultBlockState());
                for (int y = 1; y <= WATER; y++) set(x, y, z, Blocks.WATER.defaultBlockState());
            }
        }

        /** A great red column through every level, a band of gold at each floor. */
        void column() {
            int c = 6 + hash(cx, 1, cz, 8) % 3;
            for (int y = WATER + 1; y <= TOP + LEVEL; y++) {
                boolean band = Math.floorMod(y - BASE, LEVEL) == 0;
                for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) set(c + dx, y, c + dz, band ? GOLD : RED);
            }
        }

        void level(int k, int y) {
            // The galleries: a row (or column) of chunks has one at this level, from end to end.
            if (hash(0, k, cz, 1) % 6 == 0) gallery(true, y, hash(0, k, cz, 11) % 3 != 0);
            if (hash(cx, k, 0, 2) % 7 == 0) gallery(false, y, hash(cx, k, 0, 12) % 3 != 0);
            switch (hash(cx, k, cz, 3) % 14) {
                case 0, 1, 2, 3, 4, 5, 6 -> { }
                case 7 -> platform(y);
                case 8 -> pavilion(y, false);
                case 9 -> pavilion(y + LEVEL - 1, true);
                case 10 -> stairs(y, hash(cx, k, cz, 4) % 4);
                case 11 -> sideways(y, hash(cx, k, cz, 5) % 2 == 0);
                default -> lanterns(y, k);
            }
        }

        /** A gallery four wide across the whole chunk, railed, lanterns on its posts; covered when `roofed`. */
        void gallery(boolean alongX, int y, boolean roofed) {
            for (int u = 0; u < 16; u++) {
                for (int w = 6; w <= 9; w++) set(alongX, u, y, w, w == 6 || w == 9 ? (alongX ? EDGE_X : EDGE_Z) : FLOOR);
                boolean post = u % 4 == 0;
                for (int w : new int[]{5, 10}) {
                    set(alongX, u, y, w, alongX ? EDGE_X : EDGE_Z);
                    if (roofed && post) for (int h = 1; h <= 4; h++) set(alongX, u, y + h, w, RED);
                    else set(alongX, u, y + 1, w, RAIL);
                    if (!roofed && u % 8 == 0) set(alongX, u, y + 2, w, LANTERN);
                }
                // A red beam under it every four blocks.
                if (post) for (int w = 5; w <= 10; w++) set(alongX, u, y - 1, w, RED_BEAM);
                if (roofed) {
                    for (int w = 4; w <= 11; w++) set(alongX, u, y + 5, w, w == 4 || w == 11 ? TILE_SLAB : TILE);
                    for (int w = 6; w <= 9; w++) set(alongX, u, y + 6, w, TILE_SLAB);
                    if (post) for (int w = 5; w <= 10; w++) set(alongX, u, y + 4, w, w == 5 || w == 10 ? RED : RED_BEAM);
                    if (u % 8 == 2) set(alongX, u, y + 4, 7, HANGING);
                }
            }
        }

        /** A floor with posts that go down into the dark and a lantern on each. */
        void platform(int y) {
            int a = 2 + hash(cx, y, cz, 13) % 2, b = 13 - hash(cx, y, cz, 14) % 2;
            for (int x = a; x <= b; x++) for (int z = a; z <= b; z++) {
                boolean border = x == a || x == b || z == a || z == b;
                set(x, y, z, border ? (x == a || x == b ? EDGE_Z : EDGE_X) : (x % 3 == 0 ? FLOOR : TATAMI));
                if (border) set(x, y + 1, z, RAIL);
            }
            for (int[] c : new int[][]{{a, a}, {a, b}, {b, a}, {b, b}}) {
                for (int h = -6; h <= 1; h++) set(c[0], y + h, c[1], RED);
                set(c[0], y + 2, c[1], LANTERN);
            }
        }

        /**
         * A pavilion: red posts, walls of paper in dark frames, a roof of tiles in three steps with gold on top, a
         * lantern inside. `inverted`: hung from the floor above, upside down (its floor at `y`, all of it below).
         */
        void pavilion(int y, boolean inverted) {
            int s = inverted ? -1 : 1;
            int a = 3, b = 12;
            for (int x = a; x <= b; x++) for (int z = a; z <= b; z++) set(x, y, z, x == a || x == b || z == a || z == b ? FRAME : TATAMI);
            for (int x = a; x <= b; x++) for (int z = a; z <= b; z++) {
                boolean wall = x == a || x == b || z == a || z == b;
                if (!wall) continue;
                boolean post = (x == a || x == b) && (z == a || z == b || z == 7 || z == 8) || (z == a || z == b) && (x == 7 || x == 8);
                boolean door = (z == b && (x == 7 || x == 8)) && !inverted || (z == a && (x == 7 || x == 8)) && inverted;
                for (int h = 1; h <= 4; h++) {
                    BlockState state = post ? RED : door && h <= 3 ? AIR : h == 1 || h == 4 ? FRAME : PAPER;
                    set(x, y + s * h, z, state);
                }
            }
            // The roof: three steps of tiles, eaves out past the walls.
            for (int step = 0; step < 3; step++) {
                int lo = a - 1 + step * 2, hi = b + 1 - step * 2;
                for (int x = lo; x <= hi; x++) for (int z = lo; z <= hi; z++) {
                    boolean rim = x == lo || x == hi || z == lo || z == hi;
                    if (!rim && step < 2) continue;
                    set(x, y + s * (5 + step), z, rim ? eave(x, z, lo, hi, inverted) : TILE);
                }
            }
            set(7, y + s * 8, 7, GOLD);
            set(8, y + s * 8, 8, GOLD);
            set(7, y + s * 3, 7, inverted ? LANTERN : HANGING);
        }

        static BlockState eave(int x, int z, int lo, int hi, boolean inverted) {
            Direction facing = x == lo ? Direction.EAST : x == hi ? Direction.WEST : z == lo ? Direction.SOUTH : Direction.NORTH;
            return Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing)
                    .setValue(StairBlock.HALF, inverted ? Half.TOP : Half.BOTTOM);
        }

        /** Stairs three wide from this level up to the next, in one of four directions, red rails on both sides. */
        void stairs(int y, int dir) {
            boolean alongX = dir < 2, up = dir % 2 == 0;
            Direction facing = alongX ? (up ? Direction.EAST : Direction.WEST) : (up ? Direction.SOUTH : Direction.NORTH);
            BlockState step = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
            for (int i = 0; i < LEVEL && i < 16; i++) {
                int u = up ? i + 1 : 14 - i;
                if (u < 0 || u > 15) continue;
                for (int w = 6; w <= 8; w++) {
                    set(alongX, u, y + i, w, step);
                    set(alongX, u, y + i - 1, w, FLOOR);
                }
                set(alongX, u, y + i + 1, 5, RAIL);
                set(alongX, u, y + i + 1, 9, RAIL);
                set(alongX, u, y + i, 5, RED_BEAM);
                set(alongX, u, y + i, 9, RED_BEAM);
                if (i % 4 == 0) set(alongX, u, y + i + 2, 5, LANTERN);
            }
        }

        /**
         * A floor turned on its side, standing like a wall: tatami framed in dark wood, a doorway of paper in it, rails
         * and lanterns sticking out of it sideways, as if the room had been turned over.
         */
        void sideways(int y, boolean alongX) {
            int w = 3;
            for (int u = 1; u <= 14; u++) for (int h = 0; h < LEVEL - 1; h++) {
                boolean border = u == 1 || u == 14 || h == 0 || h == LEVEL - 2;
                boolean doorway = u >= 6 && u <= 9 && h >= 3 && h <= 8;
                set(alongX, u, y + h, w, doorway ? (u == 6 || u == 9 || h == 3 || h == 8 ? FRAME : PAPER) : border ? FRAME : u % 3 == 0 ? FLOOR : TATAMI);
                // The rail stands out of the "floor" sideways, along its edge.
                if (border && (u == 1 || u == 14) && h % 2 == 0) set(alongX, u, y + h, w + 1, RAIL);
            }
            for (int h : new int[]{4, 8}) {
                set(alongX, 4, y + h, w + 1, RAIL);
                set(alongX, 4, y + h, w + 2, LANTERN);
                set(alongX, 11, y + h, w + 1, RAIL);
                set(alongX, 11, y + h, w + 2, LANTERN);
            }
        }

        /** Lanterns hanging on long chains from the floor above. */
        void lanterns(int y, int k) {
            for (int i = 0; i < 5; i++) {
                int x = 2 + hash(cx, k, cz, 20 + i) % 12, z = 2 + hash(cx, k, cz, 30 + i) % 12;
                int length = 2 + hash(cx, k, cz, 40 + i) % 9;
                int top = y + LEVEL - 1;
                for (int h = 0; h < length; h++) set(x, top - h, z, CHAIN);
                set(x, top - length, z, HANGING);
            }
        }
    }
}
