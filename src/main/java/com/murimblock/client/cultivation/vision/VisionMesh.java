package com.murimblock.client.cultivation.vision;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The outlines the spiritual vision draws: every edge of every block face open to the air (or to plants), within the
 * realm's reach of the eye. The scan runs a slice of the sphere per tick, then the finished mesh replaces the shown
 * one, so a broken or placed block shows a few seconds later without a hitch.
 */
final class VisionMesh {
    /** Height scanned above and below the eye, at most: deep caves and the sky are left out. */
    private static final int MAX_HALF_HEIGHT = 20;
    private static final int REST_TICKS = 40;

    /** A finished mesh: edges as x1 y1 z1 x2 y2 z2 from the origin, nearest first, and the open faces. */
    record Mesh(BlockPos origin, int reach, float[] edges, float[] distances, int[] faces) {
        static final Mesh EMPTY = new Mesh(BlockPos.ZERO, 0, new float[0], new float[0], new int[0]);

        int edgeCount() {
            return distances.length;
        }
    }

    private static Mesh shown = Mesh.EMPTY;
    // The scan in progress.
    private static BlockPos origin;
    private static int reach;
    private static int nextX;
    private static int rest;
    private static final LongOpenHashSet KEYS = new LongOpenHashSet();
    private static final FloatArrayList EDGES = new FloatArrayList();
    private static final IntArrayList FACES = new IntArrayList();

    private VisionMesh() {
    }

    static Mesh shown() {
        return shown;
    }

    static void clear() {
        shown = Mesh.EMPTY;
        origin = null;
        rest = 0;
    }

    /** One tick of the scan around the eye at {@code eye} for this reach (0 clears the mesh). */
    static void tick(Level level, BlockPos eye, int newReach) {
        if (newReach <= 0) {
            clear();
            return;
        }
        boolean moved = shown.reach() != newReach || shown.edgeCount() > 0 && !shown.origin().closerThan(eye, 1.5);
        if (origin == null) {
            if (rest-- > 0 && !moved) {
                return;
            }
            begin(eye, newReach);
        }
        // The first mesh is built quickly; later ones take their time.
        int slices = shown.reach() != reach ? 8 : 1;
        for (int i = 0; i < slices && origin != null; i++) {
            scanSlice(level);
        }
    }

    private static void begin(BlockPos eye, int newReach) {
        origin = eye.immutable();
        reach = newReach;
        nextX = -reach;
        KEYS.clear();
        EDGES.clear();
        FACES.clear();
    }

    private static void scanSlice(Level level) {
        int half = Math.min(reach, MAX_HALF_HEIGHT);
        int r2 = reach * reach;
        int x = nextX;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (int z = -reach; z <= reach; z++) {
            for (int y = -half; y <= half; y++) {
                if (x * x + y * y + z * z > r2) {
                    continue;
                }
                pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                if (!seen(level, pos)) {
                    continue;
                }
                for (Direction d : Direction.values()) {
                    side.setWithOffset(pos, d);
                    if (!seen(level, side)) {
                        addFace(x, y, z, d);
                    }
                }
            }
        }
        if (++nextX > reach) {
            finish();
        }
    }

    /** Whether the vision outlines this block: anything solid to walk on, and liquids. Air and plants are not. */
    private static boolean seen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty());
    }

    private static void addFace(int x, int y, int z, Direction d) {
        FACES.add(packFace(x, y, z, d.ordinal()));
        int[][] c = corners(x, y, z, d);
        for (int i = 0; i < 4; i++) {
            int[] a = c[i];
            int[] b = c[(i + 1) % 4];
            long ka = packPoint(a);
            long kb = packPoint(b);
            if (KEYS.add(Math.min(ka, kb) << 24 | Math.max(ka, kb))) {
                EDGES.add(a[0]);
                EDGES.add(a[1]);
                EDGES.add(a[2]);
                EDGES.add(b[0]);
                EDGES.add(b[1]);
                EDGES.add(b[2]);
            }
        }
    }

    /** The four corners of a block face, in order around it, from the scan origin. */
    static int[][] corners(int x, int y, int z, Direction d) {
        return switch (d) {
            case EAST -> new int[][] {{x + 1, y, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x + 1, y, z + 1}};
            case WEST -> new int[][] {{x, y, z}, {x, y + 1, z}, {x, y + 1, z + 1}, {x, y, z + 1}};
            case UP -> new int[][] {{x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case DOWN -> new int[][] {{x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}};
            case SOUTH -> new int[][] {{x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}};
            case NORTH -> new int[][] {{x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}};
        };
    }

    private static long packPoint(int[] p) {
        return (long) (p[0] + 128) << 16 | (long) (p[1] + 128) << 8 | (p[2] + 128);
    }

    static int packFace(int x, int y, int z, int direction) {
        return (x + 128) << 20 | (y + 128) << 12 | (z + 128) << 4 | direction;
    }

    static int faceX(int face) {
        return (face >>> 20 & 0xFF) - 128;
    }

    static int faceY(int face) {
        return (face >>> 12 & 0xFF) - 128;
    }

    static int faceZ(int face) {
        return (face >>> 4 & 0xFF) - 128;
    }

    static Direction faceDirection(int face) {
        return Direction.values()[face & 0xF];
    }

    /** Sorts the edges nearest the eye first (the reveal wave and the reach cut stop at the first one too far). */
    private static void finish() {
        int count = EDGES.size() / 6;
        float[] raw = EDGES.toFloatArray();
        long[] order = new long[count];
        for (int i = 0; i < count; i++) {
            int o = i * 6;
            // The eye is half a block into the origin block.
            float mx = (raw[o] + raw[o + 3]) / 2 - 0.5f;
            float my = (raw[o + 1] + raw[o + 4]) / 2 - 0.5f;
            float mz = (raw[o + 2] + raw[o + 5]) / 2 - 0.5f;
            order[i] = (long) Float.floatToIntBits((float) Math.sqrt(mx * mx + my * my + mz * mz)) << 32 | i;
        }
        Arrays.sort(order);
        float[] edges = new float[count * 6];
        float[] distances = new float[count];
        for (int i = 0; i < count; i++) {
            int from = (int) order[i] * 6;
            System.arraycopy(raw, from, edges, i * 6, 6);
            distances[i] = Float.intBitsToFloat((int) (order[i] >>> 32));
        }
        shown = new Mesh(origin, reach, edges, distances, FACES.toIntArray());
        origin = null;
        rest = REST_TICKS;
        KEYS.clear();
        EDGES.clear();
        FACES.clear();
    }
}
