package com.murimblock.world.structure;

import com.murimblock.mob.MurimEntities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

/**
 * The Crab King's lair, the Root Well (docs/structures/crab-king-lair.md, mock-up C chosen by Enzo on 15/10/2026): a
 * round cavern 88 blocks across under the Jiangnan Waterlands, its ceiling 42 blocks over the mud at the centre (more
 * than 25 over all the fighting floor: the king is a giant and strikes all round him), open to the swamp through a
 * shaft hung with mangrove roots, vines on its walls to climb back up, a pool four blocks deep where it lands (one can
 * jump down). A war junk lies broken in two against the west wall, its stern half sunk in the mud, its bow standing up
 * out of it. Nests of shells and bones round the floor, glowing lichen on the walls; lit bright (froglights in the
 * walls, lanterns, unseen light) so the fight is easy to see and nothing else spawns there. The king waits beside the pool with a few armoured crabs.
 * <p>
 * Built by steps, a slice of the cavern a step ({@link #buildSome}), so the server never freezes: carve, then the ship,
 * the dressing, the dwellers. Everything comes from the world seed, so a restarted build is identical.
 */
public final class CrabLairBuilder {
    /** The cavern, about its centre on the floor: horizontal radius, the ellipsoid's centre and vertical radius above the floor. */
    public static final int RADIUS = 44, CENTRE_Y = 8, RISE = 34;
    /** Depth of the floor under the surface; the shaft's radius; the pool's radius and depth under it. */
    public static final int DEPTH = 46, SHAFT = 8, POOL = 7, POOL_DEPTH = 4;
    /** Where the shaft and its pool are (blocks from the centre: z). */
    public static final int SHAFT_Z = 4;
    /** Where the king waits (from the centre). */
    public static final int KING_X = 9, KING_Z = 12;

    private final ServerLevel level;
    private final int x0, floor, z0, surface;
    private final RandomSource random;
    private ArrayDeque<Runnable> steps;
    private final List<Mob> dwellers = new ArrayList<>();

    /** The lair under (x, z), the floor DEPTH blocks under `surface`. */
    public CrabLairBuilder(ServerLevel level, int x, int surface, int z, long seed) {
        this.level = level;
        this.x0 = x;
        this.z0 = z;
        this.surface = surface;
        this.floor = Math.max(level.getMinBuildHeight() + 10, surface - DEPTH);
        this.random = RandomSource.create(seed);
    }

    public int floorY() { return floor; }
    public BlockPos centre() { return new BlockPos(x0, floor, z0); }
    public List<Mob> dwellers() { return dwellers; }

    /** Runs the next steps for about {@code budgetNanos}; true while there is more to do. */
    public boolean buildSome(long budgetNanos) {
        if (steps == null) steps = plan();
        long end = System.nanoTime() + budgetNanos;
        Runnable step;
        while ((step = steps.poll()) != null) {
            step.run();
            if (System.nanoTime() >= end) break;
        }
        return !steps.isEmpty();
    }

    /** Builds it all at once (tests). */
    public void buildAll() {
        while (buildSome(Long.MAX_VALUE / 4)) { }
    }

    private ArrayDeque<Runnable> plan() {
        ArrayDeque<Runnable> plan = new ArrayDeque<>();
        for (int x = -RADIUS - 4; x <= RADIUS + 4; x++) { int sx = x; plan.add(() -> carveSlice(sx)); }
        plan.add(this::shaft);
        plan.add(this::pool);
        plan.add(this::junk);
        plan.add(this::dress);
        plan.add(this::light);
        plan.add(this::dwell);
        return plan;
    }

    // ---------------------------------------------------------------- the shape

    /** A smooth noise in about [-1, 1], for rough rock. */
    static double n3(double x, double y, double z) {
        return Math.sin(x * 0.31 + z * 0.17) * 0.5 + Math.sin(y * 0.43 - x * 0.12) * 0.3 + Math.cos(z * 0.27 + y * 0.21) * 0.4;
    }

    /** Whether (dx, dy, dz) from the centre on the floor lies inside the cavern (dy 0 is the floor). */
    public static boolean inside(int dx, int dy, int dz) {
        if (dy < 1) return false;
        double d = (dx / (double) RADIUS) * (dx / (double) RADIUS) + ((dy - CENTRE_Y) / (double) RISE) * ((dy - CENTRE_Y) / (double) RISE)
                + (dz / (double) RADIUS) * (dz / (double) RADIUS);
        return d < 1 + 0.1 * n3(dx, dy, dz);
    }

    /** Whether (dx, dz) lies in the shaft. */
    static boolean inShaft(int dx, int dz) {
        return Math.hypot(dx, dz - SHAFT_Z) < SHAFT;
    }

    private BlockState rock(int dx, int dy, int dz) {
        double k = n3(dx * 1.7, dy * 1.3, dz * 1.9);
        return k > 0.75 ? Blocks.MOSS_BLOCK.defaultBlockState() : k > -0.25 ? Blocks.DEEPSLATE.defaultBlockState()
                : k > -0.65 ? Blocks.TUFF.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    }

    private void set(int dx, int dy, int dz, BlockState state) {
        level.setBlock(new BlockPos(x0 + dx, floor + dy, z0 + dz), state, 2 | 16);
    }

    private BlockState get(int dx, int dy, int dz) {
        return level.getBlockState(new BlockPos(x0 + dx, floor + dy, z0 + dz));
    }

    /** One slice (x) of the cavern: air inside, a shell of rock two blocks thick round it, a floor of mud. */
    private void carveSlice(int dx) {
        int top = CENTRE_Y + RISE + 4;
        for (int dz = -RADIUS - 4; dz <= RADIUS + 4; dz++) for (int dy = -3; dy <= top; dy++) {
            if (floor + dy >= surface - 1) break;   // never above the swamp's own ground
            if (inside(dx, dy, dz)) {
                set(dx, dy, dz, Blocks.AIR.defaultBlockState());
                continue;
            }
            boolean near = false;
            for (int[] o : NEAR) if (inside(dx + o[0], dy + o[1], dz + o[2])) { near = true; break; }
            if (!near) continue;
            if (dy <= 0) {
                // The floor: mud, darker in hollows, puddles; firm ground under it.
                double k = n3(dx * 0.6, 3, dz * 0.6);
                set(dx, dy, dz, dy < 0 ? Blocks.PACKED_MUD.defaultBlockState()
                        : k > 0.6 ? Blocks.WATER.defaultBlockState() : k < -0.4 ? Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState() : Blocks.MUD.defaultBlockState());
            } else set(dx, dy, dz, rock(dx, dy, dz));
        }
    }

    private static final int[][] NEAR = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {2, 0, 0}, {-2, 0, 0}, {0, 2, 0}, {0, -2, 0}, {0, 0, 2}, {0, 0, -2}};

    /** The shaft up to the swamp, its walls in rock, vines to climb, roots hanging down; a ring of roots and mud above. */
    private void shaft() {
        int ceiling = CENTRE_Y + RISE;
        for (int dx = -SHAFT - 2; dx <= SHAFT + 2; dx++) for (int dz = SHAFT_Z - SHAFT - 2; dz <= SHAFT_Z + SHAFT + 2; dz++) {
            double d = Math.hypot(dx, dz - SHAFT_Z);
            for (int dy = ceiling - 12; floor + dy <= surface + 1; dy++) {
                if (inside(dx, dy, dz)) continue;
                if (d < SHAFT) set(dx, dy, dz, Blocks.AIR.defaultBlockState());
                else if (d < SHAFT + 2 && floor + dy < surface) set(dx, dy, dz, rock(dx, dy, dz));
            }
            // The rim on the swamp: mud and mangrove roots.
            if (d >= SHAFT && d < SHAFT + 3) {
                set(dx, surface - floor - 1, dz, random.nextFloat() < 0.5 ? Blocks.MUD.defaultBlockState() : Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState());
                if (random.nextFloat() < 0.3) set(dx, surface - floor, dz, Blocks.MANGROVE_ROOTS.defaultBlockState());
            }
        }
        // Vines on the shaft's walls, all the way down to the cavern's ceiling (the way back up).
        for (int k = 0; k < 64; k++) {
            double a = k * Math.PI * 2 / 64;
            int dx = (int) Math.round(Math.cos(a) * (SHAFT - 1)), dz = SHAFT_Z + (int) Math.round(Math.sin(a) * (SHAFT - 1));
            Direction toWall = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? (Math.cos(a) > 0 ? Direction.EAST : Direction.WEST) : (Math.sin(a) > 0 ? Direction.SOUTH : Direction.NORTH);
            if (k % 3 != 0) continue;
            for (int dy = surface - floor - 1; dy > ceiling - 14; dy--) {
                if (!get(dx, dy, dz).isAir() || get(dx + toWall.getStepX(), dy, dz + toWall.getStepZ()).isAir()) continue;
                set(dx, dy, dz, Blocks.VINE.defaultBlockState().setValue(VineBlock.PROPERTY_BY_DIRECTION.get(toWall), true));
            }
        }
        // Mangrove roots hanging down through the shaft into the cavern.
        for (int k = 0; k < 26; k++) {
            double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * (SHAFT - 3);
            int dx = (int) Math.round(Math.cos(a) * r), dz = SHAFT_Z + (int) Math.round(Math.sin(a) * r);
            int len = 10 + random.nextInt(22), start = surface - floor - 1;
            for (int i = 0; i < len; i++) {
                int dy = start - i;
                if (dy < 20) break;   // never in the king's way
                if (!get(dx, dy, dz).isAir()) continue;
                set(dx, dy, dz, i == len - 1 ? Blocks.HANGING_ROOTS.defaultBlockState() : Blocks.MANGROVE_ROOTS.defaultBlockState());
            }
        }
    }

    /** The pool under the shaft, deep enough to land in from the swamp. */
    private void pool() {
        for (int dx = -POOL - 1; dx <= POOL + 1; dx++) for (int dz = SHAFT_Z - POOL - 1; dz <= SHAFT_Z + POOL + 1; dz++) {
            double d = Math.hypot(dx, dz - SHAFT_Z);
            if (d >= POOL + 1) continue;
            if (d >= POOL) { set(dx, 0, dz, Blocks.MUD.defaultBlockState()); continue; }
            for (int dy = -POOL_DEPTH; dy <= 0; dy++) set(dx, dy, dz, Blocks.WATER.defaultBlockState());
            set(dx, -POOL_DEPTH - 1, dz, Blocks.MUD.defaultBlockState());
            for (int dy = -POOL_DEPTH - 2; dy < 0; dy++) for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                if (Math.hypot(dx + o[0], dz + o[1] - SHAFT_Z) >= POOL && get(dx + o[0], dy, dz + o[1]).isAir())
                    set(dx + o[0], dy, dz + o[1], Blocks.PACKED_MUD.defaultBlockState());
        }
    }

    // ---------------------------------------------------------------- the junk

    /**
     * The war junk broken in two (as mock-up C): built in its own axes (u along, stern to bow; v across; h up), its stern
     * half heeled and sunk in the mud against the west wall, its bow half tipped up out of the mud about the break.
     */
    private void junk() {
        int len = 50, half = len / 2, width = 7, broken = 6;
        double yaw = Math.toRadians(30), roll = Math.toRadians(16), pitch = Math.toRadians(-4), rise = Math.toRadians(64);
        int ox = -24, oz = -10, oy = -2;
        List<int[]> parts = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState(), strake = Blocks.MANGROVE_PLANKS.defaultBlockState(),
                deck = Blocks.SPRUCE_PLANKS.defaultBlockState(), white = Blocks.CALCITE.defaultBlockState(),
                post = Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState(), tile = Blocks.DEEPSLATE_TILES.defaultBlockState(),
                mast = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y),
                sail = Blocks.WHITE_WOOL.defaultBlockState(), batten = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
                lantern = Blocks.LANTERN.defaultBlockState();
        java.util.function.BiConsumer<int[], BlockState> put = (p, s) -> { parts.add(p); states.add(s); };
        for (int u = -half; u <= half; u++) {
            double t = u / (double) half;
            int w = Math.max(1, (int) Math.round(width * (1 - Math.pow(Math.max(0, t), 2) * 0.75 - Math.pow(Math.max(0, -t), 4) * 0.25)));
            int deckH = (int) Math.round(8 + Math.max(0, -t - 0.55) * 14 + Math.max(0, t - 0.7) * 8);
            for (int v = -w; v <= w; v++) {
                int bottom = (int) Math.round(Math.pow(Math.abs(v) / (double) w, 2) * 5);
                put.accept(new int[] {u, v, bottom}, hull);
                if (Math.abs(v) == w) for (int h = bottom; h <= deckH + 1; h++) put.accept(new int[] {u, v, h}, h == deckH - 1 ? strake : hull);
                if ((u + v) % 7 != 0) put.accept(new int[] {u, v, deckH}, deck);
            }
            if (Math.abs(u) == half) for (int v = -w; v <= w; v++) for (int h = 0; h <= deckH; h++) put.accept(new int[] {u, v, h}, hull);
        }
        for (int u = -half; u <= -half + 9; u++) for (int v = -6; v <= 6; v++) for (int h = 1; h <= 5; h++) {
            int deckH = (int) Math.round(8 + Math.max(0, -u / (double) half - 0.55) * 14);
            if (u == -half + 9 || Math.abs(v) == 6) put.accept(new int[] {u, v, deckH + h}, Math.abs(v) == 6 && u % 3 == 0 ? post : white);
            if (h == 5) put.accept(new int[] {u, v, deckH + h + 1}, tile);
        }
        for (int[] m : new int[][] {{-half / 4, 24, 8}, {half / 4, 19, 6}, {(int) (half * 0.7), 12, 4}}) {
            for (int k = 8; k <= m[1]; k++) put.accept(new int[] {m[0], 0, k}, mast);
            for (int k = 11; k <= m[1] - 1; k++) for (int s = -m[2]; s <= m[2]; s++) {
                if (random.nextFloat() < 0.18F + (k < 14 ? 0.25F : 0)) continue;   // torn
                put.accept(new int[] {m[0] + 1, s, k}, k % 3 == 0 ? batten : sail);
            }
        }
        for (int u = -half + 12; u <= half - 6; u += 8) for (int v : new int[] {-(width - 1), width - 1}) put.accept(new int[] {u, v, 10}, lantern);
        for (int i = 0; i < parts.size(); i++) {
            int[] p = parts.get(i);
            double u = p[0], v = p[1], h = p[2];
            if (u > broken) {
                double du = u - broken;
                double nu = broken + du * Math.cos(rise) - h * Math.sin(rise) + 2, nh = du * Math.sin(rise) + h * Math.cos(rise) - 6;
                u = nu;
                h = nh;
            } else if (u > broken - 2 && random.nextFloat() < 0.5F) continue;   // the splintered break
            double vv = v * Math.cos(roll) - h * Math.sin(roll), hh = v * Math.sin(roll) + h * Math.cos(roll);
            double uu = u * Math.cos(pitch) + hh * Math.sin(pitch);
            hh = -u * Math.sin(pitch) + hh * Math.cos(pitch);
            int wx = (int) Math.round(ox + uu * Math.cos(yaw) - vv * Math.sin(yaw)), wz = (int) Math.round(oz + uu * Math.sin(yaw) + vv * Math.cos(yaw));
            int wy = (int) Math.round(oy + hh);
            if (wy <= 0 || !inside(wx, wy, wz)) continue;
            BlockState here = get(wx, wy, wz);
            if (!here.isAir() && states.get(i) == deck) continue;
            set(wx, wy, wz, states.get(i));
            if (wy <= 2 && states.get(i) == hull) for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                if (get(wx + o[0], 1, wz + o[1]).isAir() && random.nextFloat() < 0.6F) set(wx + o[0], 1, wz + o[1], Blocks.MUD.defaultBlockState());
        }
    }

    // ---------------------------------------------------------------- dressing, light, dwellers

    /** Nests round the floor, stalactites high up only, glowing lichen on the walls. */
    private void dress() {
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI * 2 / 10;
            int cx = (int) Math.round(Math.cos(a) * 34), cz = (int) Math.round(Math.sin(a) * 34);
            for (int i = 0; i < 16; i++) {
                int dx = cx + random.nextInt(5) - 2, dz = cz + random.nextInt(5) - 2, dy = 1 + (i > 12 ? 1 : 0);
                if (!inside(dx, dy, dz) || !get(dx, dy, dz).isAir()) continue;
                float f = random.nextFloat();
                set(dx, dy, dz, f < 0.35F ? Blocks.BONE_BLOCK.defaultBlockState() : f < 0.7F ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.BROWN_TERRACOTTA.defaultBlockState());
            }
        }
        // Stalactites, only where they hang more than 22 blocks over the floor (and not in the shaft).
        for (int k = 0; k < 90; k++) {
            int dx = random.nextInt(RADIUS * 2) - RADIUS, dz = random.nextInt(RADIUS * 2) - RADIUS;
            if (inShaft(dx, dz)) continue;
            int dy = CENTRE_Y + RISE + 2;
            while (dy > 1 && !inside(dx, dy, dz)) dy--;
            if (dy < 28) continue;
            int length = 2 + random.nextInt(5);
            for (int i = 0; i < length && dy - i > 22; i++) {
                boolean tip = i == length - 1 || dy - i - 1 <= 22;
                set(dx, dy - i, dz, tip ? Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                        .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP) : Blocks.DRIPSTONE_BLOCK.defaultBlockState());
                if (tip) break;
            }
        }
        // Glowing lichen on the walls.
        for (int k = 0; k < 900; k++) {
            int dx = random.nextInt(RADIUS * 2 + 1) - RADIUS, dz = random.nextInt(RADIUS * 2 + 1) - RADIUS, dy = 2 + random.nextInt(CENTRE_Y + RISE - 2);
            if (!inside(dx, dy, dz) || !get(dx, dy, dz).isAir()) continue;
            for (Direction d : Direction.values()) {
                if (d == Direction.UP || d == Direction.DOWN) continue;
                if (!inside(dx + d.getStepX(), dy, dz + d.getStepZ()) && get(dx + d.getStepX(), dy, dz + d.getStepZ()).isSolid()) {
                    set(dx, dy, dz, Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(d), true));
                    break;
                }
            }
        }
    }

    /**
     * Lit bright enough to fight in (Enzo: the fight must be easy to see) and so no other monster spawns: ochre
     * froglights set in the walls on two rings, lanterns on chains hanging from the ceiling, shroomlights in the nests,
     * and unseen light blocks over the floor filling the rest.
     */
    private void light() {
        for (int ring = 0; ring < 2; ring++) {
            int dy = ring == 0 ? 6 : 17, count = 36;
            for (int k = 0; k < count; k++) {
                double a = (k + ring * 0.5) * Math.PI * 2 / count;
                // From the middle outwards to the first wall block at this height: set a froglight in it.
                for (int d = 10; d <= RADIUS + 3; d++) {
                    int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
                    if (inside(dx, dy, dz)) continue;
                    set(dx, dy, dz, Blocks.OCHRE_FROGLIGHT.defaultBlockState());
                    break;
                }
            }
        }
        // Lanterns on chains from the ceiling, over the fighting floor (never lower than 20 blocks over it).
        for (int k = 0; k < 14; k++) {
            double a = k * Math.PI * 2 / 14, d = k % 2 == 0 ? 16 : 28;
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (inShaft(dx, dz)) continue;
            int dy = CENTRE_Y + RISE + 2;
            while (dy > 1 && !inside(dx, dy, dz)) dy--;
            int chain = Math.max(1, Math.min(6, dy - 22));
            for (int i = 0; i < chain; i++) set(dx, dy - i, dz, Blocks.CHAIN.defaultBlockState());
            set(dx, dy - chain, dz, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        // Shroomlights in the nests.
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI * 2 / 10;
            int dx = (int) Math.round(Math.cos(a) * 34), dz = (int) Math.round(Math.sin(a) * 34);
            if (inside(dx, 1, dz)) set(dx, 1, dz, Blocks.SHROOMLIGHT.defaultBlockState());
        }
        // Unseen light over all the floor.
        for (int dx = -RADIUS; dx <= RADIUS; dx += 4) for (int dz = -RADIUS; dz <= RADIUS; dz += 4)
            for (int dy : new int[] {2, 10, 22}) if (inside(dx, dy, dz) && get(dx, dy, dz).isAir())
                set(dx, dy, dz, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
    }

    /** The Crab King beside the pool, facing the shaft, and his crabs. */
    private void dwell() {
        Mob king = spawn(MurimEntities.CRAB_KING.get(), KING_X, KING_Z, 200);
        if (king != null) king.setPersistenceRequired();
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.4;
            spawn(MurimEntities.ARMORED_CRAB.get(), KING_X + (int) Math.round(Math.cos(a) * 5), KING_Z + (int) Math.round(Math.sin(a) * 5), random.nextFloat() * 360);
        }
    }

    private Mob spawn(net.minecraft.world.entity.EntityType<? extends Mob> type, int dx, int dz, float yaw) {
        Mob mob = type.create(level);
        if (mob == null) return null;
        mob.moveTo(x0 + dx + 0.5, floor + 1, z0 + dz + 0.5, yaw, 0);
        mob.setYHeadRot(yaw);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        dwellers.add(mob);
        return mob;
    }
}
