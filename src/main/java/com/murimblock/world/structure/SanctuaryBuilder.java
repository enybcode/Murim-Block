package com.murimblock.world.structure;

import com.murimblock.world.MurimTrees;
import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds the Cloud Cliff Sanctuary (docs/structures): a Taoist sect seat on its own pale granite spire, about 70 blocks
 * high, reached by a plank road spiralling round the rock, crowned by a paved court with the main hall (vermilion
 * columns, green glazed roof), dormitory, library, bell and drum towers, a paifang gate and an incense fire whose smoke
 * column marks the place from afar. Built once per world by {@link SanctuaryPlacement}.
 * Coordinates: the spire stands on (cx, ground, cz); +Z is the south, where the gate and the court open.
 */
public final class SanctuaryBuilder {
    public static final int HEIGHT = 70;
    /** Distance from the spire axis at which the ground is measured to choose the road side. */
    public static final int FOOT_RADIUS = 22;
    /** Plank road: blocks of climb per block walked, and where it starts (angle in radians, 0 = east). */
    static final double ROAD_PITCH = 0.3;

    private final ServerLevel level;
    private final RandomSource random;
    private final int cx, cz, ground, top;
    /** Angle (radians, 0 = east, pi/2 = south) where the plank road starts: the open, low side of the spire. */
    private final double roadStart;

    // Palette.
    private static final BlockState GRANITE = MurimBlocks.PALE_GRANITE.get().defaultBlockState();
    private static final BlockState PAVING = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final BlockState BORDER = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState WALL = Blocks.CALCITE.defaultBlockState();
    private static final BlockState BEAM = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState PLANK = Blocks.SPRUCE_PLANKS.defaultBlockState();

    public SanctuaryBuilder(ServerLevel level, int cx, int ground, int cz, RandomSource random) {
        this(level, cx, ground, cz, random, Math.PI / 2);
    }

    public SanctuaryBuilder(ServerLevel level, int cx, int ground, int cz, RandomSource random, double roadStart) {
        this.roadStart = roadStart;
        this.level = level;
        this.random = random;
        this.cx = cx;
        this.cz = cz;
        this.ground = ground;
        this.top = ground + HEIGHT;
    }

    public int top() { return top; }

    /** Where players arrive: the foot of the plank road, on its little stone landing. */
    public BlockPos foot() {
        double r = radiusAt(ground + 1) + 4;
        return new BlockPos((int) Math.floor(cx + Math.cos(roadStart) * r), ground + 1, (int) Math.floor(cz + Math.sin(roadStart) * r));
    }

    public void build() {
        spire();
        road();
        summit();
    }

    // ---------------------------------------------------------------- helpers

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void setRel(int dx, int y, int dz, BlockState state) { set(cx + dx, y, cz + dz, state); }
    private void fillRel(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) setRel(x, y, z, state);
    }
    private static BlockState stair(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }
    private static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    /** Spire radius at a height: a little wider towards the summit, so the top holds the whole court and its buildings. */
    double radiusAt(int y) {
        double t = Math.max(0, Math.min(1, (double) (y - ground) / HEIGHT));
        return 14 + 6 * t;
    }

    // ---------------------------------------------------------------- the spire

    private void spire() {
        // From below the lowest ground of the footprint, so it never floats on a slope.
        int bottom = ground;
        for (int dx = -21; dx <= 21; dx += 3) for (int dz = -21; dz <= 21; dz += 3)
            bottom = Math.min(bottom, level.getHeight(Heightmap.Types.OCEAN_FLOOR, cx + dx, cz + dz) - 1);
        int[] jointX = new int[HEIGHT + 64], jointZ = new int[HEIGHT + 64];
        for (int s = 0; s < jointX.length; s += 7) {
            int jx = random.nextInt(3) - 1, jz = random.nextInt(3) - 1;
            for (int k = s; k < Math.min(s + 7, jointX.length); k++) { jointX[k] = jx; jointZ[k] = jz; }
        }
        for (int y = bottom - 4; y <= top; y++) {
            double r = radiusAt(y);
            int k = Math.max(0, y - bottom);
            int ri = (int) Math.ceil(r) + 2;
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double d = Math.hypot(dx - jointX[k] * 0.6, dz - jointZ[k] * 0.6);
                double rough = (Math.floorMod(dx * 7 + dz * 13 + y / 7 * 5, 9) - 4) * 0.12;
                if (d > r + rough) continue;
                boolean skin = d > r + rough - 1.3;
                BlockState state = GRANITE;
                if (skin && Math.floorMod(y, 7) == 0 && random.nextFloat() < 0.5f) state = Blocks.DIORITE.defaultBlockState(); // joints
                if (skin && random.nextFloat() < 0.04f) state = Blocks.MOSS_BLOCK.defaultBlockState();
                setRel(dx, y, dz, state);
            }
        }
        // Cliff pines out of the flanks.
        var pine = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOptional(MurimTrees.feature("cliff_pine"));
        pine.ifPresent(feature -> {
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                int y = ground + 10 + random.nextInt(HEIGHT - 20);
                double r = radiusAt(y);
                feature.place(level, level.getChunkSource().getGenerator(), random,
                        new BlockPos((int) (cx + Math.cos(a) * (r - 1)), y, (int) (cz + Math.sin(a) * (r - 1))));
            }
        });
    }

    // ---------------------------------------------------------------- the plank road

    /**
     * Total angle the road turns through: about the natural spiral at {@link #ROAD_PITCH}, adjusted so that it always
     * arrives at the south (pi/2), in line with the paifang gate.
     */
    private double roadTurn() {
        double a = 0, y = ground + 1;
        while (y < top) { a += 1.0 / (radiusAt((int) y) + 1.2); y += ROAD_PITCH; }
        double miss = Math.IEEEremainder(Math.PI / 2 - (roadStart + a), Math.PI * 2);
        return a + miss;
    }

    /** Length of the gangway between the rim of the court and the end of the road. */
    static final int GANGWAY = 4;

    /** How far out from the rock the road runs: against it, then swinging out over the last quarter turn. */
    private static double roadOffset(double frac, double turn) {
        double left = (1 - frac) * turn; // radians still to go
        double t = Math.max(0, Math.min(1, 1 - left / (Math.PI / 2)));
        return 1.2 + GANGWAY * t * t * (3 - 2 * t);
    }

    private void road() {
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        BlockState strut = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        double turn = roadTurn();
        double a = roadStart;
        int lastY = ground + 1;
        double pavilionA = 0, pavilionR = 0;
        int pavilionY = Integer.MIN_VALUE;
        int step = 0;
        while (true) {
            double frac = Math.min(1, (a - roadStart) / turn);
            double y = ground + 1 + frac * (HEIGHT - 1);
            int iy = (int) Math.floor(y);
            double rock = radiusAt(iy);
            double off = roadOffset(frac, turn);
            double r = rock + off;
            Direction travel = Direction.fromYRot(Math.toDegrees(a) + 180); // tangent, roughly
            for (double w = 0; w <= 1.6; w += 0.4) { // two blocks wide
                int x = (int) Math.floor(cx + Math.cos(a) * (r + w)), z = (int) Math.floor(cz + Math.sin(a) * (r + w));
                BlockState tread = iy > lastY ? stair(Blocks.SPRUCE_STAIRS, travel, Half.BOTTOM) : PLANK;
                set(x, iy, z, tread);
                for (int h = 1; h <= 3; h++) set(x, iy + h, z, Blocks.AIR.defaultBlockState());
                // Beam struck into the rock under every few planks.
                if (Math.floorMod(x + z, 4) == 0) set(x, iy - 1, z, fence);
            }
            // Away from the rock, the road rests on log struts struck into it, with a rail on the inner side too.
            if (off > 1.7) {
                if (step % 3 == 0) for (double d = rock - 1; d <= r + 1.6; d += 0.5)
                    set((int) Math.floor(cx + Math.cos(a) * d), iy - 1, (int) Math.floor(cz + Math.sin(a) * d), strut);
                set((int) Math.floor(cx + Math.cos(a) * (r - 0.6)), iy + 1, (int) Math.floor(cz + Math.sin(a) * (r - 0.6)), fence);
            }
            // Rail on the outer edge.
            int ox = (int) Math.floor(cx + Math.cos(a) * (r + 2.4)), oz = (int) Math.floor(cz + Math.sin(a) * (r + 2.4));
            set(ox, iy + 1, oz, fence);
            if (pavilionY == Integer.MIN_VALUE && y > ground + HEIGHT * 0.5) { pavilionA = a; pavilionR = r; pavilionY = iy; }
            lastY = iy;
            if (frac >= 1) break;
            a += 1.0 / r;          // one block of arc
            step++;
        }
        // After the whole road, so its later turns never cut through the pavilion roof.
        restPavilion(pavilionA, pavilionR, pavilionY);
        // Foot landing, last so nothing of the road (rail, steps) stands on it: a stone apron, a stele, lanterns.
        BlockPos foot = foot();
        // Open sky over the landing (it may stand against a slope), then no tree or plant around it.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, foot.getX() + dx, foot.getZ() + dz);
            for (int h = ground + 1; h <= surface + 1; h++) level.setBlock(new BlockPos(foot.getX() + dx, h, foot.getZ() + dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // Clear the trees and plants of the biome around the landing: players must arrive in the open.
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int h = 1; h <= 14; h++) {
            BlockPos p = new BlockPos(foot.getX() + dx, ground + h, foot.getZ() + dz);
            BlockState s = level.getBlockState(p);
            if (s.is(net.minecraft.tags.BlockTags.LOGS) || s.is(net.minecraft.tags.BlockTags.LEAVES) || (s.canBeReplaced() && s.getFluidState().isEmpty()))
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            set(foot.getX() + dx, ground, foot.getZ() + dz, BORDER);
            for (int h = 1; h <= 4; h++) set(foot.getX() + dx, ground + h, foot.getZ() + dz, Blocks.AIR.defaultBlockState());
        }
        set(foot.getX() + 2, ground + 1, foot.getZ() + 2, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(foot.getX() + 2, ground + 2, foot.getZ() + 2, Blocks.LANTERN.defaultBlockState());
        set(foot.getX() - 2, ground + 1, foot.getZ() + 2, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(foot.getX() - 2, ground + 2, foot.getZ() + 2, Blocks.LANTERN.defaultBlockState());
    }

    /**
     * Mid-way rest pavilion standing out from the road on beams, reached by a short bridge: four red posts, a small
     * pyramid roof clear of the road, benches.
     */
    private void restPavilion(double a, double r, int y) {
        double ux = Math.cos(a), uz = Math.sin(a), tx = -uz, tz = ux; // outwards, along the road
        double centre = r + 6.5;
        // Bridge from the road's outer edge to the pavilion floor, rails on both sides.
        for (double d = r + 1.6; d <= centre - 2; d += 0.4) for (int w = -2; w <= 2; w++) {
            int x = (int) Math.floor(cx + ux * d + tx * w), z = (int) Math.floor(cz + uz * d + tz * w);
            boolean rail = Math.abs(w) == 2;
            if (!rail) {
                set(x, y, z, PLANK);
                set(x, y - 1, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                for (int h = 1; h <= 3; h++) set(x, y + h, z, Blocks.AIR.defaultBlockState());
            } else if (d > r + 2.8) set(x, y + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        int px = (int) Math.floor(cx + ux * centre), pz = (int) Math.floor(cz + uz * centre);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            set(px + dx, y, pz + dz, PLANK);
            set(px + dx, y - 1, pz + dz, Blocks.SPRUCE_FENCE.defaultBlockState());
            for (int h = 1; h <= 5; h++) set(px + dx, y + h, pz + dz, Blocks.AIR.defaultBlockState());
        }
        BlockState post = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
        for (int dx : new int[]{-2, 2}) for (int dz : new int[]{-2, 2}) for (int h = 1; h <= 3; h++) set(px + dx, y + h, pz + dz, post);
        roof(px - 2, pz - 2, px + 2, pz + 2, y + 4, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES, true);
        set(px, y + 3, pz, Blocks.CHAIN.defaultBlockState());
        set(px, y + 2, pz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(px - 1, y + 1, pz + 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        set(px + 1, y + 1, pz + 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
    }

    // ---------------------------------------------------------------- the summit

    private void summit() {
        int t = top;
        // Clear and pave the court; a stone brick border and a low balustrade at the rim.
        int reach = (int) Math.ceil(radiusAt(t)) + 1;
        for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
            if (Math.hypot(dx, dz) > radiusAt(t) + 0.5) continue;
            for (int h = 1; h <= 30; h++) setRel(dx, t + h, dz, Blocks.AIR.defaultBlockState());
            boolean rim = Math.hypot(dx, dz) > radiusAt(t) - 1.2;
            setRel(dx, t, dz, rim ? BORDER : ((dx + dz) % 7 == 0 ? Blocks.STONE_BRICKS.defaultBlockState() : PAVING));
            if (rim) setRel(dx, t + 1, dz, Blocks.STONE_BRICK_WALL.defaultBlockState());
        }
        mainHall(t);
        sideHall(-15, -9, -3, 5, t, false); // dormitory (west)
        sideHall(9, 15, -3, 5, t, true);    // library (east)
        tower(-12, 10, t, true);            // bell tower (south-west)
        tower(12, 10, t, false);            // drum tower (south-east)
        paifang(t);
        incense(t);
        // Two pines in the court corners.
        var pine = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOptional(MurimTrees.feature("huangshan_pine"));
        pine.ifPresent(feature -> {
            feature.place(level, level.getChunkSource().getGenerator(), random, new BlockPos(cx - 6, t + 1, cz + 10));
            feature.place(level, level.getChunkSource().getGenerator(), random, new BlockPos(cx + 6, t + 1, cz + 9));
        });
        gangway(t);
    }

    /**
     * The entrance: the plank road ends at the south, a few blocks out from the rock, and a gangway (3 wide, rails on
     * both sides, beams under it) crosses to a gap in the balustrade between two lantern posts, in line with the
     * paifang's central passage.
     */
    private void gangway(int t) {
        int rim = (int) Math.floor(radiusAt(t) + 0.5);          // last paved row of the court
        int far = (int) Math.floor(radiusAt(t) + 1.2 + GANGWAY); // where the road runs
        for (int x = -1; x <= 1; x++) for (int z = rim - 1; z <= far; z++) {
            setRel(x, t, z, z <= rim - 1 ? PAVING : PLANK);
            if (z > rim && x != 0) setRel(x, t - 1, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            for (int h = 1; h <= 3; h++) setRel(x, t + h, z, Blocks.AIR.defaultBlockState());
        }
        for (int x : new int[]{-2, 2}) {
            for (int z = rim + 1; z < far; z++) setRel(x, t + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
            // Lantern posts at the gap in the balustrade.
            setRel(x, t + 1, rim, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            setRel(x, t + 2, rim, Blocks.LANTERN.defaultBlockState());
        }
        // The road ends here: close its far end over the void.
        for (int z = far; z <= far + 2; z++) setRel(-2, t + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
    }

    /** Main hall, 5 bays (21 x 9), facing the court on the south: platform, vermilion columns, lattice front, green roof. */
    private void mainHall(int t) {
        int x0 = -10, x1 = 10, z0 = -14, z1 = -6;
        fillRel(x0 - 1, t + 1, z0 - 1, x1 + 1, t + 2, z1 + 1, PAVING);
        for (int x = x0 - 1; x <= x1 + 1; x++) { setRel(x, t + 3, z1 + 1, Blocks.STONE_BRICK_WALL.defaultBlockState()); }
        for (int x = -3; x <= 3; x++) { // front steps on the axis
            setRel(x, t + 3, z1 + 1, Blocks.AIR.defaultBlockState());
            setRel(x, t + 2, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            setRel(x, t + 1, z1 + 3, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        int fy = t + 3, storey = 6;
        BlockState column = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int y = fy; y < fy + storey; y++) {
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (!edge) { setRel(x, y, z, Blocks.AIR.defaultBlockState()); continue; }
                boolean col = (Math.floorMod(x - x0, 4) == 0 && (z == z0 || z == z1)) || ((x == x0 || x == x1) && Math.floorMod(z - z0, 4) == 0);
                if (col) { setRel(x, y, z, column); continue; }
                if (z == z1) { // front: open door bays in the middle, lattice in the others
                    boolean door = Math.abs(x) <= 1 && y < fy + 4;
                    setRel(x, y, z, door ? Blocks.AIR.defaultBlockState()
                            : y >= fy + 1 && y <= fy + 4 ? Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true)
                            : BEAM);
                } else setRel(x, y, z, WALL);
            }
        }
        for (int x = x0; x <= x1; x++) { setRel(x, fy + storey, z0, BEAM); setRel(x, fy + storey, z1, BEAM); }
        for (int z = z0; z <= z1; z++) { setRel(x0, fy + storey, z, BEAM); setRel(x1, fy + storey, z, BEAM); }
        // Inside: red carpet aisle, altar with a statue of the founder, meditation cushions, lanterns.
        for (int z = z0 + 1; z < z1; z++) setRel(0, fy, z, Blocks.RED_CARPET.defaultBlockState());
        fillRel(-2, fy, z0 + 1, 2, fy, z0 + 2, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        setRel(0, fy + 1, z0 + 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        setRel(0, fy + 2, z0 + 1, Blocks.GOLD_BLOCK.defaultBlockState());
        setRel(-2, fy + 1, z0 + 2, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        setRel(2, fy + 1, z0 + 2, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        for (int x : new int[]{-6, -3, 3, 6}) setRel(x, fy, z0 + 4, Blocks.YELLOW_CARPET.defaultBlockState());
        for (int x : new int[]{-6, 6}) {
            setRel(x, fy + storey - 1, -10, Blocks.CHAIN.defaultBlockState());
            setRel(x, fy + storey - 2, -10, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // Hanging lanterns under the front eave.
        for (int x : new int[]{-8, -4, 4, 8}) setRel(x, fy + storey - 1, z1 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        roofRel(x0, z0, x1, z1, fy + storey + 1, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE, false);
    }

    /** A 3-bay side hall (dormitory or library) along the z axis, opening onto the court. */
    private void sideHall(int x0, int x1, int z0, int z1, int t, boolean library) {
        fillRel(x0 - 1, t + 1, z0 - 1, x1 + 1, t + 1, z1 + 1, PAVING);
        int fy = t + 2, storey = 4;
        int door = library ? x0 : x1; // the side facing the court
        for (int y = fy; y < fy + storey; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { setRel(x, y, z, Blocks.AIR.defaultBlockState()); continue; }
            boolean col = (x == x0 || x == x1) && (z == z0 || z == z1 || Math.floorMod(z - z0, 4) == 0);
            if (col) setRel(x, y, z, Blocks.DARK_OAK_LOG.defaultBlockState());
            else if (x == door && Math.abs(z - (z0 + z1) / 2) <= 0 && y < fy + 2) setRel(x, y, z, Blocks.AIR.defaultBlockState());
            else if (x == door && y == fy + 2) setRel(x, y, z, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, library ? Direction.WEST : Direction.EAST).setValue(TrapDoorBlock.OPEN, true));
            else setRel(x, y, z, WALL);
        }
        for (int z = z0 + 1; z < z1; z++) {
            int inner = library ? x1 - 1 : x0 + 1;
            if (library) { setRel(inner, fy, z, Blocks.BOOKSHELF.defaultBlockState()); setRel(inner, fy + 1, z, Blocks.BOOKSHELF.defaultBlockState()); }
            else if (z % 2 == 0) setRel(inner, fy, z, Blocks.WHITE_CARPET.defaultBlockState());
        }
        if (library) setRel(x1 - 2, fy, (z0 + z1) / 2, Blocks.LECTERN.defaultBlockState());
        roofRel(x0, z0, x1, z1, fy + storey, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES, false);
    }

    /** Two-storey bell or drum tower, 5 x 5, pyramid roof in green tiles. */
    private void tower(int x, int z, int t, boolean bell) {
        for (int y = t + 1; y <= t + 7; y++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
            if (y <= t + 3) setRel(x + dx, y, z + dz, corner ? Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState() : edge ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.AIR.defaultBlockState());
            else if (y == t + 4) setRel(x + dx, y, z + dz, BEAM);
            else setRel(x + dx, y, z + dz, corner ? Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        setRel(x, t + 1, z + 2, Blocks.AIR.defaultBlockState()); setRel(x, t + 2, z + 2, Blocks.AIR.defaultBlockState());
        if (bell) {
            setRel(x, t + 7, z, BEAM);
            setRel(x, t + 6, z, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        } else {
            setRel(x, t + 5, z, Blocks.BARREL.defaultBlockState());
            setRel(x, t + 6, z, Blocks.NOTE_BLOCK.defaultBlockState());
        }
        roofRel(x - 2, z - 2, x + 2, z + 2, t + 8, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE, true);
    }

    /** Paifang at the south rim: three openings, red posts, dark lintels, small green roofs, hanging lanterns. */
    private void paifang(int t) {
        int z = (int) Math.floor(radiusAt(t)) - 3;
        BlockState post = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState();
        for (int x : new int[]{-6, -2, 2, 6}) {
            for (int y = t + 1; y <= t + 7; y++) setRel(x, y, z, post);
            setRel(x, t + 1, z - 1, BORDER); setRel(x, t + 1, z + 1, BORDER);
        }
        fillRel(-7, t + 6, z, 7, t + 6, z, BEAM);
        fillRel(-3, t + 8, z, 3, t + 8, z, BEAM);
        for (int x = -7; x <= 7; x++) {
            setRel(x, t + 7, z - 1, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
            setRel(x, t + 7, z + 1, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            setRel(x, t + 7, z, Blocks.DARK_PRISMARINE.defaultBlockState());
        }
        for (int x = -4; x <= 4; x++) {
            setRel(x, t + 9, z - 1, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
            setRel(x, t + 9, z + 1, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            setRel(x, t + 9, z, Blocks.DARK_PRISMARINE.defaultBlockState());
        }
        for (int x : new int[]{-8, 8}) setRel(x, t + 7, z, stair(Blocks.PRISMARINE_BRICK_STAIRS, x < 0 ? Direction.EAST : Direction.WEST, Half.BOTTOM));
        for (int x : new int[]{-4, 0, 4}) setRel(x, t + 5, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** Incense fire in the court: a campfire on a hay bale (its smoke rises very high), in a stone basin. */
    private void incense(int t) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) setRel(dx, t + 1, 2 + dz, BORDER);
        setRel(0, t + 1, 2, Blocks.HAY_BLOCK.defaultBlockState());
        setRel(0, t + 2, 2, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
        for (int[] c : new int[][]{{-1, 1}, {1, 1}, {-1, 3}, {1, 3}}) setRel(c[0], t + 2, c[1], Blocks.IRON_BARS.defaultBlockState());
    }

    // ---------------------------------------------------------------- roofs

    private void roofRel(int x0, int z0, int x1, int z1, int y, Block stairs, Block slabs, Block ridge, boolean pyramid) {
        roof(cx + x0, cz + z0, cx + x1, cz + z1, y, stairs, slabs, ridge, pyramid);
    }

    /**
     * Chinese roof over a rectangle (world coordinates): eaves one block out as slabs, then stair courses climbing
     * inward (a hip roof, or a pyramid on a square), flying corners lifted one block, a solid ridge with raised ends.
     */
    private void roof(int x0, int z0, int x1, int z1, int y, Block stairs, Block slabs, Block ridge, boolean pyramid) {
        x0 -= 2; z0 -= 2; x1 += 2; z1 += 2;
        // Eave course: bottom slabs (the flat lower curve of the roof) round a solid ceiling, so no gap shows under it.
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
            set(x, y, z, x == x0 || x == x1 || z == z0 || z == z1 ? slab(slabs, SlabType.BOTTOM) : ridge.defaultBlockState());
        // Flying eaves at the four corners.
        set(x0, y + 1, z0, stair(stairs, Direction.SOUTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
        set(x1, y + 1, z0, stair(stairs, Direction.SOUTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
        set(x0, y + 1, z1, stair(stairs, Direction.NORTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
        set(x1, y + 1, z1, stair(stairs, Direction.NORTH, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
        int ax0 = x0 + 1, az0 = z0 + 1, ax1 = x1 - 1, az1 = z1 - 1;
        int yy = y + 1;
        while (ax1 - ax0 >= (pyramid ? 0 : 1) && az1 - az0 >= 0) {
            for (int x = ax0; x <= ax1; x++) for (int z = az0; z <= az1; z++) {
                boolean n = z == az0, s = z == az1, w = x == ax0, e = x == ax1;
                if (!(n || s || w || e)) { set(x, yy, z, ridge.defaultBlockState()); continue; }
                Direction facing = n ? Direction.SOUTH : s ? Direction.NORTH : w ? Direction.EAST : Direction.WEST;
                BlockState st = stair(stairs, facing, Half.BOTTOM);
                if ((n || s) && (w || e)) st = st.setValue(StairBlock.SHAPE, (n == w) ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT);
                if (ax1 - ax0 <= 1 || az1 - az0 <= 1) st = ridge.defaultBlockState(); // the ridge line itself
                set(x, yy, z, st);
            }
            ax0++; az0++; ax1--; az1--; yy++;
        }
        // Ridge ornaments: raised ends on the long axis.
        if (!pyramid) {
            boolean alongX = (x1 - x0) >= (z1 - z0);
            int midZ = (z0 + z1) / 2, midX = (x0 + x1) / 2;
            int ry = yy - 1;
            if (alongX) {
                int inset = (z1 - z0) / 2;
                set(x0 + inset, ry + 1, midZ, ridge.defaultBlockState());
                set(x1 - inset, ry + 1, midZ, ridge.defaultBlockState());
            } else {
                int inset = (x1 - x0) / 2;
                set(midX, ry + 1, z0 + inset, ridge.defaultBlockState());
                set(midX, ry + 1, z1 - inset, ridge.defaultBlockState());
            }
        } else {
            set((x0 + x1) / 2, yy, (z0 + z1) / 2, ridge.defaultBlockState());
            set((x0 + x1) / 2, yy + 1, (z0 + z1) / 2, Blocks.LIGHTNING_ROD.defaultBlockState());
        }
    }
}
