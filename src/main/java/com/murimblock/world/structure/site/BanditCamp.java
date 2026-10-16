package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.map.MurimMap;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.slab;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * A great bandit camp (docs/structures/bandit-camp.md, from the mock-up scripts/concepts/bandit-camp.js): a round
 * palisade 85 blocks across behind a ditch, the gate to the south between two towers behind spiked barricades, four
 * watchtowers; inside, the chief's longhall to the north with its empty seat and the war drum before it, the bonfire
 * plaza, two bunkhouses, ten tents, the loot store, three cages, a horse pen and a training ring. Thirty Mountain Bandits
 * hold it, each at his post.
 * <p>One in every region of the Murim map, dressed for its land (the region of its centre): logs and thatch in the
 * centre, dark wood under snow in the north, rammed earth in the west, bamboo in the south, tiles and a moat in the east.
 * The boss who will come out of the longhall once the thirty are dead is not here yet: the seat waits for him.
 */
final class BanditCamp extends Site {
    /** The palisade's radius, half the gate's width. */
    static final int R = 42, GATE = 3;
    /** How many bandits hold the camp. */
    static final int BANDITS = 30;
    /** Crimson Scarf: the lieutenants' outfit (MountainBanditEntity.OUTFITS). */
    private static final int CRIMSON = 1;

    /** A region's dress: the palisade, roofs, tents, ground, and the outfits its bandits wear. */
    record Theme(BlockState wall, RoofTiles roof, RoofTiles bunkRoof, Block tentA, Block tentB, BlockState top, BlockState[] floor,
                 boolean snow, boolean moat, int... outfits) { }

    private static final RoofTiles SPRUCE = new RoofTiles(Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_PLANKS);
    private static final RoofTiles MUD = new RoofTiles(Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_SLAB, Blocks.MUD_BRICKS);
    private static final RoofTiles BAMBOO = new RoofTiles(Blocks.BAMBOO_STAIRS, Blocks.BAMBOO_SLAB, Blocks.BAMBOO_PLANKS);

    static Theme theme(MurimMap.Region region) {
        BlockState dirt = s(Blocks.COARSE_DIRT), path = s(Blocks.DIRT_PATH), mud = s(Blocks.PACKED_MUD);
        return switch (region) {
            case NORTH -> new Theme(log(Blocks.DARK_OAK_LOG, Direction.Axis.Y), SPRUCE, SPRUCE, Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL,
                    s(Blocks.SNOW_BLOCK), new BlockState[]{s(Blocks.SNOW_BLOCK), dirt, s(Blocks.PODZOL)}, true, false, 2, 2, 0);
            case WEST -> new Theme(s(Blocks.PACKED_MUD), PeachHamlet.THATCH, MUD, Blocks.BROWN_WOOL, Blocks.ORANGE_TERRACOTTA,
                    s(Blocks.SAND), new BlockState[]{s(Blocks.SAND), dirt, mud}, false, false, 3, 3, 0);
            case SOUTH -> new Theme(s(Blocks.BAMBOO_BLOCK), PeachHamlet.THATCH, BAMBOO, Blocks.BROWN_WOOL, Blocks.GREEN_WOOL,
                    s(Blocks.GRASS_BLOCK), new BlockState[]{dirt, s(Blocks.MOSS_BLOCK), path}, false, false, 0, 1, 2);
            case EAST -> new Theme(log(Blocks.SPRUCE_LOG, Direction.Axis.Y), RoofTiles.GREY, RoofTiles.GREY, Blocks.BROWN_WOOL, Blocks.BROWN_TERRACOTTA,
                    s(Blocks.GRASS_BLOCK), new BlockState[]{dirt, mud, path}, false, true, 0, 1, 2);
            default -> new Theme(log(Blocks.SPRUCE_LOG, Direction.Axis.Y), PeachHamlet.THATCH, SPRUCE, Blocks.BROWN_WOOL, Blocks.BROWN_TERRACOTTA,
                    s(Blocks.GRASS_BLOCK), new BlockState[]{dirt, path, mud}, false, false, 0, 1, 2);
        };
    }

    private static final BlockState PLANKS = s(Blocks.SPRUCE_PLANKS);
    private static final BlockState BEAM = log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y);
    private static final BlockState STONE = s(Blocks.COBBLESTONE);

    @Override public int reach() { return 60; }

    @Override public List<Part> parts() { return List.of(new Part(-47, -47, 47, 60, 0, 24)); }

    @Override public int probe() { return 34; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 10, false); }

    /** Anywhere in the Murim bubble: every region has its camps. */
    @Override public boolean allowedAt(int x, int z) {
        return MurimMap.inBubble(x, z);
    }

    /** Big, and allowed in every biome: it steps aside for any other site, so it never lands on one. */
    @Override public boolean keepsClearOfOtherSites() { return true; }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        Theme t = theme(MurimMap.region(c.wx(0, 0), c.wz(0, 0)));
        prepare(c, -47, -47, 47, 47, g, g + 24, t.top(), s(Blocks.DIRT));
        prepare(c, -12, 48, 12, 60, g, g + 10, t.top(), s(Blocks.DIRT));
        ground(c, g, t);
        palisade(c, g, t);
        gate(c, g, t);
        for (int[] w : new int[][]{{-24, -30}, {24, -30}, {-30, 22}, {30, 22}}) tower(c, w[0], w[1], g, 10, t, Direction.fromDelta(-Integer.signum(w[0]), 0, 0));
        longhall(c, g, t);
        plaza(c, g);
        bunkhouse(c, g, t, -1);
        bunkhouse(c, g, t, 1);
        int[][] tents = {{-19, -10}, {-12, -12}, {12, -12}, {19, -10}, {-20, 12}, {-14, 17}, {12, 16}, {20, 12}, {-8, 25}, {8, 25}};
        for (int i = 0; i < tents.length; i++) tent(c, tents[i][0], tents[i][1], g, i % 2 == 0 ? t.tentA() : t.tentB());
        loot(c, g);
        for (int[] k : new int[][]{{-23, 21}, {-17, 26}, {-11, 31}}) cage(c, k[0], k[1], g);
        pen(c, g);
        trainingRing(c, g);
        bandits(c, g, t);
    }

    // ---------------------------------------------------------------- the ground, the palisade, the gate

    /** Trodden ground inside, the way from the gate to the hall and across the plaza; the road out. */
    private static void ground(Canvas c, int g, Theme t) {
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.hypot(x, z);
            if (d > R - 1) continue;
            BlockState b = Math.abs(x) <= 2 || (Math.abs(z) <= 1 && d < 34) ? s(Blocks.DIRT_PATH) : c.pick(x, g, z, t.top(), t.top(), t.floor()[0], t.floor()[1], t.floor()[2]);
            c.set(x, g, z, b);
        }
        c.carried(-30, -30, 30, 30, g);
        for (int z = R + 1; z <= 60; z++) for (int x = -2; x <= 2; x++) c.set(x, g, z, c.pick(x, g, z, s(Blocks.DIRT_PATH), s(Blocks.COARSE_DIRT), s(Blocks.GRAVEL)));
        c.carried(-2, R + 5, 2, 60, g);
    }

    /** The palisade: a ring of logs (or rammed earth, bamboo) six and seven high, sharpened; a ditch outside it. */
    private static void palisade(Canvas c, int g, Theme t) {
        for (int x = -R - 4; x <= R + 4; x++) for (int z = -R - 4; z <= R + 4; z++) {
            double d = Math.hypot(x, z);
            boolean road = Math.abs(x) <= GATE && z > 0;
            if (d >= R - 0.5 && d < R + 0.5 && !road) {
                int h = (x + z & 1) == 0 ? 7 : 6;
                c.footing(x, g, z, t.wall());
                c.carried(x, z, x, z, g);
                for (int y = g; y <= g + h; y++) c.set(x, y, z, t.wall());
                if (t.snow()) c.set(x, g + h + 1, z, s(Blocks.SNOW));
            } else if (d >= R + 1.5 && d < R + 3.5) {
                // The ditch, two deep; the road crosses it on planks; in the east, water in it.
                if (Math.abs(x) <= 2 && z > 0) { c.set(x, g, z, PLANKS); c.set(x, g - 1, z, t.moat() ? s(Blocks.WATER) : AIR); continue; }
                c.set(x, g, z, AIR);
                c.set(x, g - 1, z, t.moat() ? s(Blocks.WATER) : AIR);
            }
        }
    }

    /** The gate between two towers, its leaves swung open inward, a lintel; spiked barricades before it. */
    private static void gate(Canvas c, int g, Theme t) {
        for (int s : new int[]{-1, 1}) {
            tower(c, s * 6, R, g, 8, t, Direction.NORTH);
            for (int y = g + 1; y <= g + 5; y++) for (int z = R - 5; z <= R - 1; z++) c.set(s * GATE, y, z, PLANKS);
        }
        for (int x = -GATE; x <= GATE; x++) c.set(x, g + 9, R, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        for (int x = -GATE + 1; x <= GATE - 1; x++) c.set(x, g + 8, R, s(Blocks.RED_WOOL));
        // Chevaux de frise: a log laid across, spikes of fence above it.
        for (int s : new int[]{-1, 1}) for (int i = 0; i < 5; i++) {
            int x = s * (6 + i);
            c.set(x, g + 1, R + 7, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
            if (i % 2 == 0) c.set(x, g + 2, R + 7, s(Blocks.SPRUCE_FENCE));
        }
    }

    /**
     * A square tower five across: walls of the palisade's stuff {@code h} high, a door towards the camp, a ladder inside
     * up through a plank floor railed with fences, posts at its corners and a pyramid roof over it.
     */
    private static void tower(Canvas c, int cx, int cz, int g, int h, Theme t, Direction door) {
        int top = g + h;
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2, corner = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
            c.footing(x, g, z, STONE);
            c.carried(x, z, x, z, g);
            c.set(x, g, z, PLANKS);
            for (int y = g + 1; y <= top; y++) c.set(x, y, z, edge ? (corner ? BEAM : t.wall()) : AIR);
            c.set(x, top + 1, z, PLANKS);
            if (corner) for (int y = top + 2; y <= top + 4; y++) c.set(x, y, z, BEAM);
            else if (edge) c.set(x, top + 2, z, s(Blocks.SPRUCE_FENCE));
            else for (int y = top + 2; y <= top + 4; y++) c.set(x, y, z, AIR);
        }
        c.kit().roof(cx - 2, cz - 2, cx + 2, cz + 2, top + 5, t.roof(), true);
        Direction back = door.getOpposite();
        ladder(c, cx + back.getStepX(), g + 1, top + 1, cz + back.getStepZ(), door);
        door(c, cx + door.getStepX() * 2, g + 1, cz + door.getStepZ() * 2, Blocks.SPRUCE_DOOR, door.getOpposite());
    }

    // ---------------------------------------------------------------- the buildings

    /**
     * The chief's longhall: a log house on a cobbled footing, a gable roof, its doorway south under a red hanging; the
     * chief's seat on a dais between two red hangings (empty: it waits for the boss), the cache, tables, lanterns; the
     * great war drum before it.
     */
    private static void longhall(Canvas c, int g, Theme t) {
        int x0 = -13, x1 = 13, z0 = -35, z1 = -25;
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) { c.footing(x, g - 1, z, STONE); c.set(x, g, z, STONE); }
        c.fill(x0 + 1, g, z0 + 1, x1 - 1, g, z1 - 1, PLANKS);
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, g);
        BlockState wall = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        for (int y = g + 1; y <= g + 5; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            boolean post = (x == x0 || x == x1) && (z == z0 || z == z1) || (z == z0 || z == z1) && Math.abs(x) % 6 == 0;
            c.set(x, y, z, !edge ? AIR : post ? BEAM : y == g + 3 && z == z1 && Math.abs(x) % 6 == 3 ? Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH).setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true) : wall);
        }
        for (int x = -1; x <= 1; x++) for (int y = g + 1; y <= g + 3; y++) c.set(x, y, z1, AIR);
        c.kit().gableRoof(x0, z0, x1, z1, g + 6, t.roof(), PLANKS, wall);
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 5, z1 - 1);
        for (int x = -2; x <= 2; x++) c.set(x, g + 4, z1 + 1, s(Blocks.RED_WOOL));
        // The dais and the empty seat between red hangings.
        for (int x = -3; x <= 3; x++) c.set(x, g + 1, z0 + 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        c.set(0, g + 1, z0 + 1, PLANKS);
        c.set(0, g + 2, z0 + 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int x : new int[]{-2, 2}) wallBanner(c, x, g + 3, z0 + 1, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        c.stocked(x0 + 1, g + 1, z0 + 1, s(Blocks.BARREL), SiteLoot.CACHE);
        c.stocked(x1 - 1, g + 1, z0 + 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), SiteLoot.BANDIT_CAMP);
        for (int x : new int[]{-8, 8}) table(c, x, g + 1, -30, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        for (int x : new int[]{-8, 0, 8}) c.set(x, g + 5, -30, s(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        // The war drum: a great red drum on a dark frame, before the hall. It will sound for the boss.
        for (int x = 7; x <= 10; x++) for (int z = -22; z <= -20; z++) c.set(x, g + 1, z, x == 7 || x == 10 ? s(Blocks.DARK_OAK_FENCE) : AIR);
        for (int x = 8; x <= 9; x++) for (int z = -22; z <= -20; z++) for (int y = g + 1; y <= g + 2; y++) c.set(x, y, z, z == -21 ? s(Blocks.RED_WOOL) : s(Blocks.WHITE_WOOL));
    }

    /** The bonfire plaza: a campfire in a ring of stone, benches of logs round it, a cauldron on a fire for the cooking. */
    private static void plaza(Canvas c, int g) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) c.set(x, g + 1, z, x == 0 && z == 0 ? s(Blocks.CAMPFIRE) : s(Blocks.COBBLESTONE_WALL));
        for (int i = -4; i <= 4; i++) {
            c.set(i, g + 1, -7, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            c.set(i, g + 1, 7, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            c.set(-7, g + 1, i, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            c.set(7, g + 1, i, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        }
        c.set(10, g + 1, -2, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true));
        c.set(10, g + 2, -2, s(Blocks.WATER_CAULDRON));
        for (int x : new int[]{9, 11}) for (int y = g + 1; y <= g + 3; y++) c.set(x, y, -2, s(Blocks.SPRUCE_FENCE));
        c.set(10, g + 3, -2, s(Blocks.SPRUCE_FENCE));
    }

    /** A bunkhouse along a side wall ({@code side}: -1 west, 1 east): log walls, a hip roof, two rows of beds. */
    private static void bunkhouse(Canvas c, int g, Theme t, int side) {
        int x0 = Math.min(27 * side, 34 * side), x1 = Math.max(27 * side, 34 * side), z0 = -9, z1 = 9, inner = side < 0 ? x1 : x0;
        c.fill(x0, g, z0, x1, g, z1, PLANKS);
        c.carried(x0, z0, x1, z1, g);
        BlockState wall = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        for (int y = g + 1; y <= g + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            c.set(x, y, z, edge ? ((x == x0 || x == x1) && (z == z0 || z == z1) ? BEAM : wall) : AIR);
        }
        door(c, inner, g + 1, 0, Blocks.SPRUCE_DOOR, side < 0 ? Direction.WEST : Direction.EAST);
        c.kit().roof(x0, z0, x1, z1, g + 5, t.bunkRoof(), false);
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 4, z1 - 1);
        int wallX = side < 0 ? x0 + 1 : x1 - 1, toWall = side < 0 ? -1 : 1;
        for (int z = z0 + 1; z <= z1 - 1; z += 2) if (Math.abs(z) > 1) bed(c, wallX - toWall, g + 1, z, toWall < 0 ? Direction.WEST : Direction.EAST, Blocks.RED_BED);
        c.set(inner + toWall, g + 1, z1 - 1, s(Blocks.BARREL));
        c.set(side < 0 ? x1 - 1 : x0 + 1, g + 4, 0, s(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    /** A hide tent: an A of wool over seven blocks on two poles, a red rag on the front one, its ends open. */
    private static void tent(Canvas c, int cx, int cz, int g, Block hide) {
        for (int k = 0; k <= 2; k++) for (int z = cz - 3; z <= cz + 3; z++) {
            c.set(cx - k, g + 3 - k, z, s(hide));
            c.set(cx + k, g + 3 - k, z, s(hide));
        }
        for (int z : new int[]{cz - 4, cz + 4}) for (int y = g + 1; y <= g + 3; y++) c.set(cx, y, z, s(Blocks.SPRUCE_FENCE));
        c.set(cx, g + 4, cz + 4, s(Blocks.RED_WOOL));
        c.set(cx, g + 1, cz - 2, s(Blocks.BROWN_CARPET));
        c.set(cx, g + 1, cz, s(Blocks.BROWN_CARPET));
    }

    /** The loot store: barrels and chests of plunder, bales of silk, a stolen cart. */
    private static void loot(Canvas c, int g) {
        int[][] barrels = {{18, -26}, {19, -26}, {20, -26}, {18, -25}, {21, -25}, {22, -26}};
        for (int[] b : barrels) c.set(b[0], g + 1, b[1], s(Blocks.BARREL));
        c.set(19, g + 2, -26, s(Blocks.BARREL));
        c.stocked(21, g + 1, -26, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), SiteLoot.BANDIT_CAMP);
        c.stocked(23, g + 1, -26, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), SiteLoot.BANDIT_CAMP);
        for (int x = 18; x <= 22; x++) for (int z = -22; z <= -20; z++) c.set(x, g + 1, z, x == 18 || x == 22 ? log(Blocks.SPRUCE_LOG, Direction.Axis.Z) : PLANKS);
        for (int x = 19; x <= 21; x++) c.set(x, g + 2, -21, s(Blocks.WHITE_WOOL));
        c.set(20, g + 2, -20, s(Blocks.RED_WOOL));
    }

    /** A cage of iron bars under a plank roof, its gate on the south. */
    private static void cage(Canvas c, int cx, int cz, int g) {
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
            c.set(x, g, z, s(Blocks.COARSE_DIRT));
            for (int y = g + 1; y <= g + 3; y++) c.set(x, y, z, edge ? (Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2 ? BEAM : s(Blocks.IRON_BARS)) : AIR);
            c.set(x, g + 4, z, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        c.set(cx, g + 1, cz + 2, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH));
        c.set(cx, g + 2, cz + 2, AIR);
        c.set(cx - 1, g + 1, cz - 1, s(Blocks.HAY_BLOCK));
    }

    /** The horse pen: a rail fence, hay and a water trough. */
    private static void pen(Canvas c, int g) {
        int x0 = -28, x1 = -18, z0 = -26, z1 = -18;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (edge) c.set(x, g + 1, z, x == -23 && z == z1 ? Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH) : s(Blocks.SPRUCE_FENCE));
        }
        c.set(-26, g + 1, -24, s(Blocks.HAY_BLOCK));
        c.set(-25, g + 1, -24, s(Blocks.HAY_BLOCK));
        c.set(-26, g + 2, -24, s(Blocks.HAY_BLOCK));
        c.set(-20, g + 1, -24, s(Blocks.WATER_CAULDRON));
    }

    /** The training ring: a ring of logs on the ground, two straw dummies. */
    private static void trainingRing(Canvas c, int g) {
        int cx = 21, cz = 25;
        for (int x = cx - 6; x <= cx + 6; x++) for (int z = cz - 6; z <= cz + 6; z++) {
            double d = Math.hypot(x - cx, z - cz);
            if (d >= 5.5 && d < 6.5 && !(x == cx && z < cz)) c.set(x, g + 1, z, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        for (int[] d : new int[][]{{cx - 3, cz + 3}, {cx + 3, cz + 3}}) {
            c.set(d[0], g + 1, d[1], s(Blocks.SPRUCE_FENCE));
            c.set(d[0], g + 2, d[1], s(Blocks.HAY_BLOCK));
            c.set(d[0], g + 3, d[1], s(Blocks.CARVED_PUMPKIN));
        }
    }

    // ---------------------------------------------------------------- the thirty

    /** Where each of the thirty stands: [x, y above the ground, z, lieutenant?]. */
    static final int[][] POSTS = {
            // The lieutenants: two at the hall's door, two at the fire.
            {-4, 1, -22, 1}, {4, 1, -22, 1}, {-9, 1, 3, 1}, {9, 1, -4, 1},
            // Four archers on the watchtowers' floors; two on the gate towers, two at the gate.
            {-24, 12, -30, 0}, {24, 12, -30, 0}, {-30, 12, 22, 0}, {30, 12, 22, 0},
            {-6, 10, R, 0}, {6, 10, R, 0}, {-2, 1, 38, 0}, {2, 1, 38, 0},
            // Six round the fire.
            {-5, 1, -4, 0}, {4, 1, -5, 0}, {-5, 1, 3, 0}, {5, 1, 3, 0}, {0, 1, -5, 0}, {0, 1, 5, 0},
            // Four sparring in the ring; two at the loot; two by the cages.
            {19, 1, 24, 0}, {23, 1, 24, 0}, {21, 1, 22, 0}, {21, 1, 28, 0},
            {16, 1, -23, 0}, {25, 1, -22, 0}, {-20, 1, 21, 0}, {-14, 1, 26, 0},
            // Four sentries: by the bunkhouses' doors and the pen.
            {-25, 1, 0, 0}, {25, 1, 0, 0}, {-23, 1, -15, 0}, {23, 1, -15, 0}};

    private static void bandits(Canvas c, int g, Theme t) {
        for (int[] p : POSTS) {
            int outfit = p[3] == 1 ? CRIMSON : c.pick(p[0], g, p[2], toIntegers(t.outfits()));
            Direction facing = p[2] > 30 ? Direction.SOUTH : Direction.fromDelta(Integer.signum(-p[0]) == 0 ? 0 : -Integer.signum(p[0]), 0, 0);
            if (facing == null) facing = Direction.SOUTH;
            c.spawn(p[0], g + p[1], p[2], facing, MurimEntities.MOUNTAIN_BANDIT.get(), bandit -> bandit.setVariant(outfit));
        }
    }

    private static Integer[] toIntegers(int[] a) {
        Integer[] r = new Integer[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[i];
        return r;
    }
}
