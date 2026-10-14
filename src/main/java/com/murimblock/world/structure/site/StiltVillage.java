package com.murimblock.world.structure.site;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Jade River Stilt Village (jade karst forest): a Zhuang hamlet of ganlan houses on stilts round a stone-flagged
 * square with a roofed well; a granary on stilts reached by a ladder, the village's sacred banyan with its altar, rice
 * paddies, and a gate on the south road. Every path joins a door, a ladder, the altar or the gate to the square.
 */
final class StiltVillage extends Site {
    private static final BlockState STILT = log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y);
    private static final BlockState PLANK = s(Blocks.SPRUCE_PLANKS);
    private static final BlockState FENCE = s(Blocks.SPRUCE_FENCE);

    @Override public int reach() { return 26; }

    @Override public List<Part> parts() { return List.of(new Part(-21, -21, 21, 26, 0, 24)); }

    @Override public int probe() { return 18; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 9, false); }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, -21, -21, 21, 21, g, g + 28, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        // The square: old stone flags, a lantern post at each corner.
        pave(c, -6, -6, 6, 6, g, s(Blocks.STONE_BRICKS), s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), s(Blocks.CRACKED_STONE_BRICKS), s(Blocks.ANDESITE));
        c.carried(-6, -6, 6, 6, g);
        for (int x : new int[]{-6, 6}) for (int z : new int[]{-6, 6}) lanternPost(c, x, g + 1, z, 2, FENCE);
        well(c, g);
        // Houses round the square, their stairs coming down onto it.
        house(c.frame(0, -14, Rotation.NONE), g, 0);
        house(c.frame(-14, 0, Rotation.COUNTERCLOCKWISE_90), g, 1);
        house(c.frame(14, 0, Rotation.CLOCKWISE_90), g, 2);
        granary(c.frame(-14, -14, Rotation.NONE), g);
        sacredBanyan(c, 14, -14, g);
        // Paths: granary ladder and banyan altar to the square, square to the south gate.
        path(c, -14, -9, -14, -8, g);
        path(c, -14, -8, -6, -8, g);
        path(c, -6, -8, -6, -7, g);
        path(c, 6, -8, 14, -8, g);
        path(c, 6, -8, 6, -7, g);
        // The road out through the gate, on solid ground whatever the terrain does beyond the village.
        prepare(c, -2, 22, 2, 26, g, g + 8, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        path(c, -1, 7, 1, 26, g);
        gate(c, g);
        paddies(c, -20, 8, -8, 20, g);
        paddies(c, 8, 8, 20, 20, g);
    }

    private static void path(Canvas c, int x0, int z0, int x1, int z1, int g) {
        pave(c, x0, z0, x1, z1, g, s(Blocks.DIRT_PATH), s(Blocks.DIRT_PATH), s(Blocks.GRAVEL), s(Blocks.COBBLESTONE));
        c.carried(x0, z0, x1, z1, g);
    }

    /** A stone-ringed well under a little tiled roof, in the middle of the square. */
    private static void well(Canvas c, int g) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            boolean centre = x == 0 && z == 0;
            for (int y = g - 4; y <= g; y++) c.set(x, y, z, centre && y > g - 4 ? s(Blocks.WATER) : s(Blocks.MOSSY_STONE_BRICKS));
            if (!centre) { c.set(x, g + 1, z, s(Blocks.MOSSY_STONE_BRICKS)); c.carried(x, z, x, z, g + 1); }
        }
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) { c.set(x, g + 2, z, FENCE); c.set(x, g + 3, z, FENCE); }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) c.set(x, g + 4, z, x == 0 && z == 0 ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.DEEPSLATE_TILE_SLAB));
        c.set(0, g + 3, 0, s(Blocks.CHAIN));
        c.set(0, g + 2, 0, hangingLantern());
    }

    /**
     * A ganlan house (frame: front +z): 9 x 7 on 3 blocks of stilts with a fenced pen under it, a balcony under the
     * deep front eave, a grey tiled gable roof, and stairs down the front.
     */
    private static void house(Canvas h, int g, int variant) {
        int f = g + 4;
        for (int x : new int[]{-4, 0, 4}) for (int z : new int[]{-3, 0, 3, 5}) {
            h.set(x, g, z, h.pick(x, g, z, s(Blocks.COBBLESTONE), s(Blocks.MOSSY_COBBLESTONE)));
            h.carried(x, z, x, z, g);
            for (int y = g + 1; y <= g + 3; y++) h.set(x, y, z, STILT);
        }
        for (int z : new int[]{-3, 0, 3, 5}) for (int x = -3; x <= 3; x++) if (x != 0) h.set(x, g + 3, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
        // The pen under the house: fenced at the back and sides, firewood, hay and barrels.
        for (int x = -3; x <= 3; x++) if (x != 0) h.set(x, g + 1, -3, FENCE);
        for (int z = -2; z <= 2; z++) if (z != 0) { h.set(-4, g + 1, z, FENCE); h.set(4, g + 1, z, FENCE); }
        h.set(-3, g + 1, -2, s(Blocks.HAY_BLOCK));
        h.set(-2, g + 1, -2, s(Blocks.HAY_BLOCK));
        h.set(-3, g + 2, -2, s(Blocks.HAY_BLOCK));
        h.set(3, g + 1, -2, s(Blocks.BARREL));
        h.set(2, g + 1, -2, s(Blocks.COMPOSTER));
        for (int x = 1; x <= 3; x++) h.set(x, g + 1, 1, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        // Floor and balcony.
        h.fill(-4, f, -3, 4, f, 5, PLANK);
        // Walls.
        for (int y = f + 1; y <= f + 3; y++) {
            for (int x = -4; x <= 4; x++) { h.set(x, y, -3, PLANK); h.set(x, y, 3, PLANK); }
            for (int z = -2; z <= 2; z++) { h.set(-4, y, z, PLANK); h.set(4, y, z, PLANK); }
            for (int[] p : new int[][]{{-4, -3}, {4, -3}, {-4, 3}, {4, 3}, {0, -3}, {-4, 0}, {4, 0}}) h.set(p[0], y, p[1], STILT);
        }
        for (int x : new int[]{-2, 2}) { h.set(x, f + 2, 3, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH)); h.set(x, f + 2, -3, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH)); }
        for (int z : new int[]{-1, 1}) { h.set(-4, f + 2, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.WEST)); h.set(4, f + 2, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.EAST)); }
        door(h, 0, f + 1, 3, Blocks.SPRUCE_DOOR, Direction.NORTH);
        // Balcony: posts carrying the eave, a railing open in the middle for the stairs, lanterns.
        for (int x : new int[]{-4, 4}) {
            for (int y = f + 1; y <= f + 3; y++) h.set(x, y, 5, STILT);
            h.set(x, f + 1, 4, FENCE);
        }
        for (int x = -3; x <= 3; x++) if (Math.abs(x) > 1) h.set(x, f + 1, 5, FENCE);
        h.set(-3, f + 2, 5, s(Blocks.LANTERN));
        h.set(3, f + 2, 5, s(Blocks.LANTERN));
        h.kit().gableRoof(-4, -3, 4, 5, f + 4, RoofTiles.GREY, PLANK, PLANK);
        h.room(-3, f + 1, -2, 3, f + 3, 2);
        h.room(-3, f + 1, 4, 3, f + 3, 5);
        // Stairs down the front onto the square, on a solid stringer.
        for (int i = 0; i < 4; i++) for (int x = -1; x <= 1; x++) {
            int y = f - i, z = 6 + i;
            h.set(x, y, z, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, Half.BOTTOM));
            for (int yy = g + 1; yy < y; yy++) h.set(x, yy, z, PLANK);
        }
        furnish(h, f, variant);
    }

    private static void furnish(Canvas h, int f, int variant) {
        int y = f + 1;
        Block bed = variant == 0 ? Blocks.RED_BED : variant == 1 ? Blocks.WHITE_BED : Blocks.LIGHT_BLUE_BED;
        bed(h, -3, y, -1, Direction.NORTH, bed);
        if (variant != 1) bed(h, -2, y, -1, Direction.NORTH, bed);
        for (int x = -1; x <= 1; x++) for (int z = 0; z <= 1; z++) h.set(x, y, z, s(h.chance(x, y, z, 0.5) ? Blocks.BROWN_CARPET : Blocks.RED_CARPET));
        table(h, 2, y, 0, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        h.set(2, y, 1, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, Half.BOTTOM));
        h.set(3, y, 0, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, Half.BOTTOM));
        h.set(3, y, -2, s(Blocks.BARREL));
        // Herbalists, weavers, cooks.
        h.set(2, y, -2, s(variant == 0 ? Blocks.BREWING_STAND : variant == 1 ? Blocks.LOOM : Blocks.SMOKER));
        h.set(-1, y, -2, s(variant == 0 ? Blocks.POTTED_FERN : variant == 1 ? Blocks.CRAFTING_TABLE : Blocks.WATER_CAULDRON));
        h.set(-3, y, 2, s(Blocks.POTTED_FERN));
        h.set(3, y, 2, s(variant == 1 ? Blocks.POTTED_BAMBOO : Blocks.FLOWER_POT));
        h.set(0, f + 3, 0, hangingLantern());
        // The household: one villager, whose trade goes with the workstation of the house (and its bed is here).
        VillagerProfession trade = variant == 0 ? VillagerProfession.CLERIC : variant == 1 ? VillagerProfession.SHEPHERD : VillagerProfession.BUTCHER;
        String title = variant == 0 ? "herbalist" : variant == 1 ? "weaver" : "cook";
        h.spawn(0, y, -1, Direction.SOUTH, EntityType.VILLAGER, villager -> {
            villager.setVillagerData(villager.getVillagerData().setType(VillagerType.JUNGLE).setProfession(trade).setLevel(1));
            villager.setVillagerXp(1); // keeps the trade even if the workstation is moved
            villager.setCustomName(Component.translatable("villager.murimblock.jade_river." + title));
        });
    }

    /** The households of the village: one villager per house. */
    static final int VILLAGERS = 3;

    /** The granary: a closed 5 x 5 store on stilts, under a pyramid roof, reached by a ladder to its porch. */
    private static void granary(Canvas h, int g) {
        int f = g + 4;
        for (int x : new int[]{-2, 2}) for (int z : new int[]{-2, 2}) {
            h.set(x, g, z, s(Blocks.COBBLESTONE));
            h.carried(x, z, x, z, g);
            for (int y = g + 1; y <= g + 3; y++) h.set(x, y, z, STILT);
        }
        h.set(0, g, 3, s(Blocks.COBBLESTONE));
        h.carried(0, 3, 0, 3, g);
        for (int y = g + 1; y <= g + 3; y++) h.set(0, y, 3, STILT);
        for (int i = -1; i <= 1; i++) {
            h.set(i, g + 3, -2, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            h.set(i, g + 3, 2, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            h.set(-2, g + 3, i, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            h.set(2, g + 3, i, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        }
        h.fill(-2, f, -2, 2, f, 2, PLANK);
        h.fill(-1, f, 3, 1, f, 3, PLANK);
        for (int y = f + 1; y <= f + 3; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;
            h.set(x, y, z, Math.abs(x) == 2 && Math.abs(z) == 2 ? STILT : PLANK);
        }
        door(h, 0, f + 1, 2, Blocks.SPRUCE_DOOR, Direction.NORTH);
        h.set(0, f + 2, -2, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH));
        h.set(-1, f + 1, -1, s(Blocks.HAY_BLOCK));
        h.set(1, f + 1, -1, s(Blocks.HAY_BLOCK));
        h.set(-1, f + 2, -1, s(Blocks.HAY_BLOCK));
        h.set(-1, f + 1, 1, s(Blocks.BARREL));
        h.set(1, f + 1, 1, s(Blocks.BARREL));
        h.set(0, f + 3, 0, hangingLantern());
        h.kit().roof(-2, -2, 2, 2, f + 4, RoofTiles.GREY, true);
        h.room(-1, f + 1, -1, 1, f + 3, 1);
        ladder(h, 0, g + 1, f, 4, Direction.SOUTH);
    }

    /** The sacred banyan of the village, roots hanging from its crown, with a stone altar and candles before it. */
    private static void sacredBanyan(Canvas c, int x, int z, int g) {
        BlockState leaves = leaves(MurimBlocks.BANYAN.leaves().get());
        tree(c, x, g + 1, z, log(MurimBlocks.BANYAN.log().get(), Direction.Axis.Y), leaves, 6, 4.6, 0.55);
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) c.set(x + d[0], g + 1, z + d[1], log(MurimBlocks.BANYAN.log().get(), Direction.Axis.Y));
        // Aerial roots hanging from the underside of the crown.
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int r2 = dx * dx + dz * dz;
            if (r2 < 6 || r2 > 14 || !c.chance(x + dx, g, z + dz, 0.4)) continue;
            int under = g + 2;
            while (under < g + 12 && !inCrown(c, x, g + 7, z, 4.6, 0.55, x + dx, under + 1, z + dz)) under++;
            if (under >= g + 12) continue;
            int length = 2 + (int) (c.rand(x + dx, g + 1, z + dz) * 3);
            for (int i = 0; i < length && under - i > g + 2; i++) c.set(x + dx, under - i, z + dz, s(MurimBlocks.BANYAN_ROOTS.get()));
        }
        // Altar on the side of the square.
        c.set(x, g + 1, z + 5, s(Blocks.CHISELED_STONE_BRICKS));
        c.set(x, g + 2, z + 5, litCandles(3));
        c.set(x - 1, g + 1, z + 5, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, Half.TOP));
        c.set(x + 1, g + 1, z + 5, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, Half.TOP));
        c.carried(x - 1, z + 5, x + 1, z + 5, g);
    }

    /** The south gate: two posts, a lintel with a little tiled roof, lanterns and red banners. */
    private static void gate(Canvas c, int g) {
        for (int x : new int[]{-2, 2}) {
            c.set(x, g, 19, s(Blocks.COBBLESTONE));
            for (int y = g + 1; y <= g + 4; y++) c.set(x, y, 19, STILT);
            wallBanner(c, x, g + 3, 20, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        }
        for (int x = -3; x <= 3; x++) c.set(x, g + 5, 19, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
        c.kit().gableRoof(-2, 19, 2, 19, g + 6, RoofTiles.GREY, PLANK, PLANK);
        c.set(-1, g + 4, 19, hangingLantern());
        c.set(1, g + 4, 19, hangingLantern());
    }

    /** Rice paddies: four plots between grass bunds, rows of rice in the mud between rows of water. */
    private static void paddies(Canvas c, int x0, int z0, int x1, int z1, int g) {
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            if (!c.inside(x, z)) continue;
            boolean bund = x == x0 || x == x1 || z == z0 || z == z1 || x == mx || z == mz;
            if (bund) continue;
            c.set(x, g - 1, z, s(Blocks.MUD));
            if ((z - z0) % 2 == 0) {
                c.set(x, g, z, s(Blocks.MUD));
                if (c.chance(x, g, z, 0.85)) tallPlant(c, x, g + 1, z, MurimBlocks.WILD_RICE.get());
            } else {
                c.set(x, g, z, s(Blocks.WATER));
            }
        }
    }
}
