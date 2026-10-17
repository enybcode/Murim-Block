package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Misty Bamboo Daoist Temple (Shu bamboo sea), after Qingcheng: three terraces climbing the slope behind stone
 * retaining walls. The mountain gate below; the middle court with its taiji pavement, censer and two side halls
 * (scriptures, elixirs); a covered stairway up to the Hall of the Three Pure Ones. Dark timber frames filled with white,
 * grey tiled roofs with lifted corners. One axis joins the gate, the court, the stairway and the hall. Sensei Yun, master
 * of the Azure Cloud Sect, keeps the hall (docs/mobs/azure-sensei.md); the Golden Bell monk keeps the court
 * (docs/mobs/golden-bell-monk.md).
 */
final class BambooTemple extends Site {
    private static final BlockState FRAME = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
    private static final BlockState WHITE = s(Blocks.CALCITE);
    private static final BlockState PAVING = s(Blocks.POLISHED_ANDESITE);
    private static final BlockState[] RETAINING = {s(Blocks.STONE_BRICKS), s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), s(Blocks.CRACKED_STONE_BRICKS)};

    @Override public int reach() { return 28; }

    @Override public List<Part> parts() {
        return List.of(new Part(-8, 14, 8, 28, 0, 14), new Part(-12, -12, 12, 13, 1, 20), new Part(-11, -27, 11, -13, 2, 16));
    }

    @Override public int[] levels(Probe p) {
        int[][] front = {{0, 21}, {-6, 18}, {6, 18}}, mid = {{0, 0}, {-10, 0}, {10, 0}}, back = {{0, -20}, {-9, -20}, {9, -20}};
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        int[] f = new int[3], m = new int[3], b = new int[3];
        for (int i = 0; i < 3; i++) {
            for (int[][] set : new int[][][]{front, mid, back}) if (p.wet(set[i][0], set[i][1])) return null;
            f[i] = p.ground(front[i][0], front[i][1]);
            m[i] = p.ground(mid[i][0], mid[i][1]);
            b[i] = p.ground(back[i][0], back[i][1]);
            min = Math.min(min, Math.min(f[i], Math.min(m[i], b[i])));
            max = Math.max(max, Math.max(f[i], Math.max(m[i], b[i])));
        }
        if (max - min > 20) return null;
        int l0 = median(f);
        int l1 = Math.max(l0 + 2, Math.min(l0 + 7, median(m)));
        int l2 = Math.max(l1 + 2, Math.min(l1 + 7, median(b)));
        return new int[]{l0, l1, l2};
    }

    private static int median(int[] v) {
        int[] s = v.clone();
        Arrays.sort(s);
        return s[1];
    }

    @Override public void build(Canvas c, int part, int[] lv) {
        switch (part) {
            case 0 -> gateTerrace(c, lv[0]);
            case 1 -> middleCourt(c, lv[0], lv[1], lv[2]);
            default -> upperTerrace(c, lv[2]);
        }
    }

    /** A terrace: cleared above, paved, and carried on stone retaining walls down to the ground. */
    private static void terrace(Canvas c, int x0, int z0, int x1, int z1, int level, int clear, int cutFrom) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!c.inside(x, z)) continue;
            for (int y = level + 1; y <= level + clear; y++) if (!c.get(x, y, z).isAir()) c.set(x, y, z, AIR);
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            c.set(x, level, z, edge ? s(Blocks.STONE_BRICKS) : PAVING);
            for (int y = level - 1, n = 0; n < 48 && !Canvas.ground(c.get(x, y, z)); y--, n++) c.set(x, y, z, c.pick(x, y, z, RETAINING));
        }
        // The floor must be carried everywhere but where the stairs from below are cut into it (|x| <= 1, z >= cutFrom).
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (Math.abs(x) > 1 || z < cutFrom) c.carried(x, z, x, z, level);
    }

    /** A low stone balustrade along a terrace edge, from (x0, z0) to (x1, z1), leaving out |x| <= gap on the axis. */
    private static void balustrade(Canvas c, int x0, int z0, int x1, int z1, int y, int gap) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (Math.abs(x) > gap || z0 != z1) c.set(x, y, z, s(Blocks.STONE_BRICK_WALL));
    }

    // ---------------------------------------------------------------- the mountain gate

    private static void gateTerrace(Canvas c, int l) {
        terrace(c, -8, 14, 8, 23, l, 20, Integer.MAX_VALUE);
        int x0 = -4, x1 = 4, z0 = 16, z1 = 19;
        for (int y = l + 1; y <= l + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean passage = Math.abs(x) <= 1;
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1 || Math.abs(x) == 2;
            if (passage || !edge) { c.set(x, y, z, AIR); continue; }
            boolean post = (x == x0 || x == x1 || Math.abs(x) == 2) && (z == z0 || z == z1);
            if (post) c.set(x, y, z, FRAME);
            else if (y == l + 2 && (z == z0 || z == z1)) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, z == z1 ? Direction.SOUTH : Direction.NORTH));
            else c.set(x, y, z, y == l + 4 ? s(Blocks.STRIPPED_DARK_OAK_WOOD) : WHITE);
        }
        c.kit().roof(x0, z0, x1, z1, l + 5, RoofTiles.GREY, false);
        c.room(-1, l + 1, z0, 1, l + 4, z1);
        // The bays: a bell on the west, a drum on the east.
        c.set(-3, l + 4, 17, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        c.set(3, l + 1, 17, s(Blocks.NOTE_BLOCK));
        c.stocked(3, l + 1, 18, s(Blocks.BARREL), SiteLoot.CACHE);
        c.set(-3, l + 1, 18, litCandles(2));
        for (int x : new int[]{-1, 1}) c.set(x, l + 4, 17, hangingLantern());
        // Lanterns either side of the way in.
        for (int x : new int[]{-3, 3}) lanternPost(c, x, l + 1, 22, 2, s(Blocks.STONE_BRICK_WALL));
        c.set(0, l, 21, s(Blocks.CHISELED_STONE_BRICKS));
        // The way up from the forest: a flagged path on solid ground before the gate.
        prepare(c, -3, 24, 3, 28, l, l + 8, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        pave(c, -1, 24, 1, 28, l, s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), PAVING);
        c.carried(-1, 24, 1, 28, l);
    }

    // ---------------------------------------------------------------- the middle court

    private static void middleCourt(Canvas c, int l0, int l1, int l2) {
        terrace(c, -12, -12, 12, 13, l1, 22, 7);
        balustrade(c, -12, 13, 12, 13, l1 + 1, 1);
        balustrade(c, -12, -12, -12, 12, l1 + 1, 0);
        balustrade(c, 12, -12, 12, 12, l1 + 1, 0);
        // Stairs down to the gate terrace, cut into the front of the court.
        for (int i = 0; i < l1 - l0; i++) for (int x = -1; x <= 1; x++) {
            int z = 13 - i, y = l0 + 1 + i;
            c.set(x, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            for (int yy = y + 1; yy <= l1 + 1; yy++) c.set(x, yy, z, AIR);
        }
        taiji(c, 0, 1, l1);
        // The censer on the axis, its smoke rising before the stairway.
        c.set(0, l1 + 1, -4, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
        c.set(0, l1 + 2, -4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false));
        for (int x : new int[]{-1, 1}) c.set(x, l1 + 1, -4, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
        // The Golden Bell monk keeps the court, by the taiji on the scriptures' side, facing the stairway to the hall.
        c.spawn(-4, l1 + 1, 7, Direction.NORTH, MurimEntities.GOLDEN_BELL_MONK.get(), monk -> { });
        sideHall(c.frame(-9, 1, Rotation.COUNTERCLOCKWISE_90), l1, true);
        sideHall(c.frame(9, 1, Rotation.CLOCKWISE_90), l1, false);
        for (int x : new int[]{-5, 5}) for (int z : new int[]{-10, 11}) lanternPost(c, x, l1 + 1, z, 2, s(Blocks.STONE_BRICK_WALL));
        // Bamboo in the corners of the court.
        for (int[] q : new int[][]{{-10, -10}, {10, -10}, {-10, 11}, {10, 11}}) bamboo(c, q[0], q[1], l1);
        stairway(c, l1, l2);
    }

    /** The taiji (yin and yang) laid in black and white stone, ringed with andesite. */
    private static void taiji(Canvas c, int cx, int cz, int y) {
        double r = 4.5;
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            double d = Math.hypot(dx, dz);
            if (d > r + 0.8) continue;
            BlockState s;
            if (d > r) s = s(Blocks.POLISHED_ANDESITE);
            else {
                boolean white = dx > 0;
                if (Math.hypot(dx, dz + r / 2) <= r / 2) white = true;
                if (Math.hypot(dx, dz - r / 2) <= r / 2) white = false;
                if (Math.hypot(dx, dz + r / 2) <= 0.8) white = false;
                if (Math.hypot(dx, dz - r / 2) <= 0.8) white = true;
                s = white ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.POLISHED_BLACKSTONE);
            }
            c.set(cx + dx, y, cz + dz, s);
        }
    }

    /** A side hall of the court (frame: front +z towards the court): scriptures on one side, elixirs on the other. */
    private static void sideHall(Canvas h, int l, boolean scriptures) {
        int x0 = -4, x1 = 4, z0 = -2, z1 = 2, f = l + 1;
        h.fill(x0 - 1, f, z0 - 1, x1 + 1, f, z1 + 1, s(Blocks.STONE_BRICKS));
        h.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        h.set(0, l + 1, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int y = f + 1; y <= f + 3; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { h.set(x, y, z, AIR); continue; }
            boolean post = (x == x0 || x == x1 || x % 2 == 0) && (z == z0 || z == z1) || (x == x0 || x == x1) && z == 0;
            if (post) h.set(x, y, z, FRAME);
            else if (y == f + 2) h.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, z == z1 ? Direction.SOUTH : z == z0 ? Direction.NORTH : x == x0 ? Direction.WEST : Direction.EAST));
            else h.set(x, y, z, WHITE);
        }
        door(h, 0, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        h.kit().gableRoof(x0, z0, x1, z1, f + 4, RoofTiles.GREY, s(Blocks.DARK_OAK_PLANKS), WHITE);
        h.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 3, z1 - 1);
        int y = f + 1;
        if (scriptures) {
            for (int x = x0 + 1; x <= x1 - 1; x++) if (x != 0) h.set(x, y, z0 + 1, s(Blocks.CHISELED_BOOKSHELF));
            h.set(0, y, z0 + 1, s(Blocks.LECTERN));
            h.set(x0 + 1, y, z1 - 1, s(Blocks.POTTED_BAMBOO));
            h.set(x1 - 1, y, z1 - 1, litCandles(3));
        } else {
            h.set(-2, y, z0 + 1, s(Blocks.BREWING_STAND));
            h.set(-1, y, z0 + 1, s(Blocks.WATER_CAULDRON));
            h.set(0, y, z0 + 1, s(Blocks.BLAST_FURNACE));
            h.set(1, y, z0 + 1, s(Blocks.BREWING_STAND));
            h.set(2, y, z0 + 1, s(Blocks.BARREL));
            h.set(x0 + 1, y, z1 - 1, s(Blocks.POTTED_FERN));
            h.set(x1 - 1, y, z1 - 1, s(Blocks.DECORATED_POT));
        }
        h.set(-2, f + 3, 0, hangingLantern());
        h.set(2, f + 3, 0, hangingLantern());
    }

    /** The covered stairway from the court up to the upper terrace, on a solid stone stringer. */
    private static void stairway(Canvas c, int l1, int l2) {
        int rise = l2 - l1;
        for (int i = 0; i < 7; i++) {
            int z = -6 - i;
            int y = i < rise ? l1 + 1 + i : l2; // the step, or the landing at the top
            for (int x = -2; x <= 2; x++) {
                for (int yy = l1; yy < y; yy++) c.set(x, yy, z, c.pick(x, yy, z, RETAINING));
                boolean step = Math.abs(x) <= 1 && i < rise;
                c.set(x, y, z, step ? stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM) : s(Blocks.STONE_BRICKS));
                for (int yy = y + 1; yy <= y + 4; yy++) c.set(x, yy, z, AIR);
            }
            for (int x : new int[]{-2, 2}) {
                if (i % 2 == 0) for (int yy = y + 1; yy <= y + 4; yy++) c.set(x, yy, z, FRAME);
                else c.set(x, y + 1, z, s(Blocks.DARK_OAK_FENCE));
            }
            // Its roof: a ridge on the axis, tiles sloping to eaves on both sides.
            int r = y + 5;
            for (int x = -2; x <= 2; x++) c.set(x, r, z, s(Blocks.DEEPSLATE_TILES));
            c.set(-3, r, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
            c.set(3, r, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
            c.set(-1, r + 1, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
            c.set(1, r + 1, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
            c.set(0, r + 1, z, s(Blocks.DEEPSLATE_TILES));
            c.room(-1, y + 1, z, 1, y + 4, z);
            if (i % 3 == 1) c.set(0, y + 4, z, hangingLantern());
        }
    }

    /** A clump of bamboo (vanilla stalks) on a patch of grass. */
    private static void bamboo(Canvas c, int x, int z, int l) {
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
            if (!c.chance(x + dx, l, z + dz, 0.75)) continue;
            c.set(x + dx, l, z + dz, s(Blocks.GRASS_BLOCK));
            int h = 5 + (int) (c.rand(x + dx, l + 1, z + dz) * 4);
            for (int i = 1; i <= h; i++) {
                BambooLeaves leaves = i >= h - 1 ? BambooLeaves.LARGE : i == h - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
                c.set(x + dx, l + i, z + dz, Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, leaves));
            }
        }
    }

    // ---------------------------------------------------------------- the hall of the Three Pure Ones

    private static void upperTerrace(Canvas c, int l) {
        terrace(c, -11, -27, 11, -13, l, 22, Integer.MAX_VALUE);
        balustrade(c, -11, -13, 11, -13, l + 1, 2);
        balustrade(c, -11, -27, -11, -14, l + 1, 0);
        balustrade(c, 11, -27, 11, -14, l + 1, 0);
        balustrade(c, -10, -27, 10, -27, l + 1, -1);
        int x0 = -7, x1 = 7, z0 = -24, z1 = -18, f = l + 1;
        c.fill(x0 - 1, f, z0 - 1, x1 + 1, f, z1 + 1, s(Blocks.STONE_BRICKS));
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        for (int x = -2; x <= 2; x++) c.set(x, f, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int y = f + 1; y <= f + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean post = (x == x0 || x == x1 || x % 3 == 0) && (z == z0 || z == z1) || (x == x0 || x == x1) && (z - z0) % 3 == 0;
            if (post) c.set(x, y, z, FRAME);
            else if (z == z1 && y >= f + 2 && y <= f + 3) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            else if ((x == x0 || x == x1) && y == f + 2) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, x == x0 ? Direction.WEST : Direction.EAST));
            else c.set(x, y, z, y == f + 4 ? s(Blocks.STRIPPED_DARK_OAK_WOOD) : WHITE);
        }
        door(c, 0, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        door(c, -1, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        door(c, 1, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        c.kit().roof(x0, z0, x1, z1, f + 5, RoofTiles.GREY, false);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 4, z1 - 1);
        // The altar of the Three Pure Ones: three hangings, incense, candles, cushions before it.
        int y = f + 1;
        for (int x = -4; x <= 4; x++) c.set(x, y, z0 + 1, s(Blocks.POLISHED_GRANITE));
        Block[] hangings = {Blocks.BLUE_WALL_BANNER, Blocks.YELLOW_WALL_BANNER, Blocks.RED_WALL_BANNER};
        for (int i = 0; i < 3; i++) {
            wallBanner(c, -3 + 3 * i, y + 3, z0 + 1, hangings[i], Direction.SOUTH);
            c.set(-3 + 3 * i, y + 1, z0 + 1, s(Blocks.DECORATED_POT));
        }
        for (int x : new int[]{-2, -1, 1, 2}) c.set(x, y + 1, z0 + 1, litCandles(1 + Math.abs(x)));
        for (int x = -4; x <= 4; x += 2) for (int z = z0 + 3; z <= z1 - 2; z += 2) c.set(x, y, z, s(Blocks.YELLOW_CARPET));
        for (int x : new int[]{-4, 0, 4}) c.set(x, f + 4, -21, hangingLantern());
        // Sensei Yun, between the altar and the cushions, facing the doors of his hall.
        c.spawn(0, y, z0 + 2, Direction.SOUTH, MurimEntities.AZURE_SENSEI.get(), sensei -> { });
        for (int x : new int[]{-4, 4}) lanternPost(c, x, l + 1, -15, 2, s(Blocks.STONE_BRICK_WALL));
        bamboo(c, -10, -15, l);
        bamboo(c, 9, -15, l);
    }
}
