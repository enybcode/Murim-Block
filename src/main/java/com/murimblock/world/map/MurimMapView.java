package com.murimblock.world.map;

/**
 * The Murim world seen from above, for the players' map (client/MurimMapScreen): a grid of {@link #SIZE} x {@link #SIZE}
 * cells over the bubble and its ocean, each the region there (its {@link MurimMap.Region} ordinal), the ocean or the
 * vanilla world beyond; and the heart of each region, where its name is written. Made on the server from the world's
 * layout, so the clients never learn the seed. Drawn once per layout.
 */
public final class MurimMapView {
    /** Cells on a side, and how far the grid reaches from the origin (the bubble, its ocean and a little more). */
    public static final int SIZE = 160, REACH = MurimMap.RADIUS + MurimMap.RING + 1_000;
    /** Blocks on a cell's side. */
    public static final double CELL = 2.0 * REACH / SIZE;
    /** Cell values beyond the regions' ordinals (0 to 4). */
    public static final byte OCEAN = 5, OUTSIDE = 6;
    /** How far out the outer regions' names go: a share of the radius. */
    public static final double LABEL_SHARE = 0.6;

    private static long madeFor = Long.MIN_VALUE;
    private static byte[] cells;
    private static int[] hearts;

    private MurimMapView() {
    }

    /** The grid, row by row from the north-west corner (-x, -z), for the layout in use. */
    public static synchronized byte[] cells() {
        make();
        return cells;
    }

    /** Each region's heart as block x, z (by ordinal): the origin for the centre, then the outer ones. */
    public static synchronized int[] hearts() {
        make();
        return hearts;
    }

    private static void make() {
        MurimMap.Layout layout = MurimMap.layout();
        if (cells != null && madeFor == layout.seed()) return;
        byte[] grid = new byte[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) for (int col = 0; col < SIZE; col++) {
            int x = (int) Math.round(-REACH + (col + 0.5) * CELL), z = (int) Math.round(-REACH + (row + 0.5) * CELL);
            double reach = MurimMap.reach(x, z);
            grid[row * SIZE + col] = reach < MurimMap.RADIUS ? (byte) MurimMap.region(x, z).ordinal()
                    : reach < MurimMap.RADIUS + MurimMap.RING ? OCEAN : OUTSIDE;
        }
        int[] at = new int[MurimMap.Region.values().length * 2];
        for (MurimMap.Region region : MurimMap.Region.values()) {
            int[] heart = MurimMap.heart(region, LABEL_SHARE);
            at[region.ordinal() * 2] = heart[0];
            at[region.ordinal() * 2 + 1] = heart[1];
        }
        cells = grid;
        hearts = at;
        madeFor = layout.seed();
    }
}
