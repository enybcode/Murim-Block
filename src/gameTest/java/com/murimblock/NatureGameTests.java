package com.murimblock;

import com.murimblock.world.MurimBiomes;
import com.murimblock.world.MurimTrees;
import com.murimblock.world.block.MurimBlocks;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import terrablender.api.RegionType;
import terrablender.api.Regions;

/**
 * The Murim biomes' content: registries and data load, every tree and rock feature really generates, saplings grow,
 * plants hold on their own soil. Absolute coordinates above the void at x 0..40: one batch per test.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NatureGameTests {
    private NatureGameTests() { }

    @GameTest(template = "foundation_arena", batch = "nature")
    public static void theMurimBiomesLoadWithTheirTags(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> murim = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "murim_biomes"));
        // Seven of the first slice, the two snows of the north and the camphor hills (14/10/2026); where they grow is the
        // Murim map's.
        helper.assertTrue(MurimBiomes.ALL.size() == 10, "Expected ten Murim biomes");
        for (ResourceKey<Biome> key : MurimBiomes.ALL) {
            var holder = biomes.getHolder(key);
            helper.assertTrue(holder.isPresent(), "Biome not loaded: " + key.location());
            helper.assertTrue(holder.get().is(murim), "Biome missing from #murimblock:murim_biomes: " + key.location());
        }
        // No TerraBlender region any more: outside the Murim map, the world is vanilla.
        helper.assertTrue(Regions.get(RegionType.OVERWORLD).stream().noneMatch(r -> r.getName().getNamespace().equals(Murimblock.MOD_ID)),
                "A Murim TerraBlender region still mixes Murim biomes into the vanilla world");
        // Every Murim block has an item, a loot table and is in the nature tab order.
        long blocks = BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals(Murimblock.MOD_ID)).count();
        helper.assertTrue(blocks == MurimBlocks.creativeOrder().size(), "Nature tab lists " + MurimBlocks.creativeOrder().size() + " of " + blocks + " blocks");
        for (var block : MurimBlocks.creativeOrder()) {
            helper.assertTrue(block.get().asItem() != Items.AIR, "No item for " + block.getId());
            var loot = helper.getLevel().getServer().reloadableRegistries().getLootTable(block.get().getLootTable());
            helper.assertTrue(loot != LootTable.EMPTY, "No loot table for " + block.getId());
        }
        helper.succeed();
    }

    /** Clears a 24 x 24 area at y 70 around (x, 0.5) and lays a floor of `soil`. */
    private static BlockPos platform(GameTestHelper helper, int x, Block soil) {
        ServerLevel level = helper.getLevel();
        level.getChunk(x >> 4, 0);
        for (int cx = (x - 12) >> 4; cx <= (x + 12) >> 4; cx++) for (int cz = -1; cz <= 0; cz++) level.setChunkForced(cx, cz, true);
        for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
            level.setBlockAndUpdate(new BlockPos(x + dx, 69, dz), soil.defaultBlockState());
            for (int y = 70; y < 140; y++) {
                BlockPos p = new BlockPos(x + dx, y, dz);
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        return new BlockPos(x, 70, 0);
    }

    private static void clear(GameTestHelper helper, int x) {
        ServerLevel level = helper.getLevel();
        for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) for (int y = 64; y < 140; y++) {
            BlockPos p = new BlockPos(x + dx, y, dz);
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(x - 14, 60, -14, x + 14, 150, 14))) e.discard();
    }

    private static int count(GameTestHelper helper, int x, Predicate<BlockState> what) {
        int n = 0;
        for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) for (int y = 64; y < 140; y++) {
            if (what.test(helper.getLevel().getBlockState(new BlockPos(x + dx, y, dz)))) n++;
        }
        return n;
    }

    private static boolean place(GameTestHelper helper, String feature, BlockPos at) {
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(MurimTrees.feature(feature));
        return configured.place(level, level.getChunkSource().getGenerator(), RandomSource.create(feature.hashCode()), at);
    }

    @GameTest(template = "foundation_arena", batch = "nature_trees", timeoutTicks = 200)
    public static void everyMurimTreeGrowsItsOwnLogAndLeaves(GameTestHelper helper) {
        StringBuilder failures = new StringBuilder();
        int x = 2000;
        try {
            for (MurimBlocks.Wood wood : MurimBlocks.WOODS) {
                Block soil = wood == MurimBlocks.DESERT_POPLAR || wood == MurimBlocks.JUJUBE ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK;
                BlockPos at = platform(helper, x, soil);
                boolean placed = false;
                // A tree can refuse a spot (random height, room): try a few seeds.
                for (int attempt = 0; attempt < 6 && !placed; attempt++) {
                    ServerLevel level = helper.getLevel();
                    placed = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(MurimTrees.feature(wood.name()))
                            .place(level, level.getChunkSource().getGenerator(), RandomSource.create(attempt * 31L + wood.name().hashCode()), at);
                }
                int logs = count(helper, x, s -> s.is(wood.log().get()));
                int leaves = count(helper, x, s -> s.is(wood.leaves().get()));
                if (!placed || logs < 3 || leaves < 6) failures.append(wood.name()).append(" (placed ").append(placed).append(", logs ").append(logs).append(", leaves ").append(leaves).append(") ");
                if (wood == MurimBlocks.WILLOW && count(helper, x, s -> s.is(MurimBlocks.WILLOW_STRANDS.get())) == 0) failures.append("willow without hanging strands ");
                clear(helper, x);
            }
            helper.assertTrue(failures.isEmpty(), "Trees that did not generate: " + failures);
            helper.succeed();
        } finally { clear(helper, x); }
    }

    @GameTest(template = "foundation_arena", batch = "nature_saplings", timeoutTicks = 200)
    public static void saplingsGrowIntoTheirTree(GameTestHelper helper) {
        StringBuilder failures = new StringBuilder();
        int x = 2100;
        try {
            for (MurimBlocks.Wood wood : List.of(MurimBlocks.OSMANTHUS, MurimBlocks.PEACH, MurimBlocks.WILLOW, MurimBlocks.DESERT_POPLAR)) {
                Block soil = wood == MurimBlocks.DESERT_POPLAR ? Blocks.SAND : Blocks.GRASS_BLOCK;
                BlockPos at = platform(helper, x, soil);
                ServerLevel level = helper.getLevel();
                helper.assertTrue(wood.sapling().get().defaultBlockState().canSurvive(level, at), wood.name() + " sapling cannot root in " + soil);
                level.setBlockAndUpdate(at, wood.sapling().get().defaultBlockState());
                for (int i = 0; i < 12 && level.getBlockState(at).getBlock() instanceof SaplingBlock sapling; i++) {
                    sapling.advanceTree(level, at, level.getBlockState(at), RandomSource.create(i));
                }
                if (!level.getBlockState(at).is(wood.log().get())) failures.append(wood.name()).append(' ');
                clear(helper, x);
            }
            helper.assertTrue(failures.isEmpty(), "Saplings that did not grow: " + failures);
            helper.succeed();
        } finally { clear(helper, x); }
    }

    @GameTest(template = "foundation_arena", batch = "nature_rocks", timeoutTicks = 200)
    public static void karstTowersSpiresPillarsAndYardangsRise(GameTestHelper helper) {
        StringBuilder failures = new StringBuilder();
        int x = 2200;
        record Rock(String feature, Block rock, int minBlocks, int minHeight) { }
        try {
            for (Rock rock : List.of(new Rock("karst_tower", MurimBlocks.KARST_LIMESTONE.get(), 400, 20),
                    new Rock("granite_spire", MurimBlocks.PALE_GRANITE.get(), 120, 20),
                    new Rock("sandstone_pillar", MurimBlocks.PILLAR_SANDSTONE.get(), 200, 25),
                    new Rock("yardang", Blocks.SMOOTH_SANDSTONE, 40, 3))) {
                BlockPos at = platform(helper, x, Blocks.GRASS_BLOCK);
                boolean placed = place(helper, rock.feature(), at.offset(rock.feature().equals("yardang") ? 10 : 0, 0, 0));
                int blocks = count(helper, x, s -> s.is(rock.rock()));
                int top = 0;
                for (int y = 139; y > 70 && top == 0; y--) {
                    for (int dx = -12; dx <= 12 && top == 0; dx++) for (int dz = -12; dz <= 12; dz++) {
                        if (helper.getLevel().getBlockState(new BlockPos(x + dx, y, dz)).is(rock.rock())) { top = y; break; }
                    }
                }
                if (!placed || blocks < rock.minBlocks() || top - 70 < rock.minHeight()) {
                    failures.append(rock.feature()).append(" (blocks ").append(blocks).append(", height ").append(top - 70).append(") ");
                }
                clear(helper, x);
            }
            helper.assertTrue(failures.isEmpty(), "Rock features too small or missing: " + failures);
            helper.succeed();
        } finally { clear(helper, x); }
    }

    @GameTest(template = "foundation_arena", batch = "nature_groves", timeoutTicks = 200)
    public static void bambooGrovesCliffPinesAndPondsGenerate(GameTestHelper helper) {
        int x = 2300;
        try {
            BlockPos at = platform(helper, x, Blocks.GRASS_BLOCK);
            place(helper, "moso_bamboo_grove", at);
            int culms = count(helper, x, s -> s.is(MurimBlocks.MOSO_BAMBOO.get()));
            clear(helper, x);
            // A sheer wall for the cliff pine.
            at = platform(helper, x, Blocks.GRASS_BLOCK);
            for (int y = 70; y < 100; y++) for (int dz = -12; dz <= 12; dz++) helper.getLevel().setBlockAndUpdate(new BlockPos(x - 2, y, dz), MurimBlocks.PALE_GRANITE.get().defaultBlockState());
            boolean pine = place(helper, "cliff_pine", new BlockPos(x - 2, 80, 0));
            int pineLogs = count(helper, x, s -> s.is(MurimBlocks.HUANGSHAN_PINE.log().get()));
            clear(helper, x);
            at = platform(helper, x, Blocks.GRASS_BLOCK);
            for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) {
                helper.getLevel().setBlockAndUpdate(new BlockPos(x + dx, 68, dz), Blocks.DIRT.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(new BlockPos(x + dx, 67, dz), Blocks.DIRT.defaultBlockState());
            }
            place(helper, "jiangnan_pond", at);
            int water = count(helper, x, s -> s.is(Blocks.WATER));
            helper.assertTrue(culms >= 30, "Moso bamboo grove too small: " + culms + " culm blocks");
            helper.assertTrue(pine && pineLogs >= 3, "Cliff pine did not grow from the wall (" + pineLogs + " logs)");
            helper.assertTrue(water >= 20, "Jiangnan pond holds only " + water + " water blocks");
            helper.succeed();
        } finally { clear(helper, x); }
    }

    @GameTest(template = "foundation_arena", batch = "nature_plants")
    public static void plantsHoldOnlyOnTheirOwnSoil(GameTestHelper helper) {
        int x = 2400;
        try {
            ServerLevel level = helper.getLevel();
            BlockPos at = platform(helper, x, Blocks.GRASS_BLOCK);
            record Check(Block plant, Block soil, boolean holds) { }
            StringBuilder failures = new StringBuilder();
            for (Check c : List.of(new Check(MurimBlocks.KARST_FERN.get(), Blocks.GRASS_BLOCK, true),
                    new Check(MurimBlocks.CISTANCHE.get(), Blocks.SAND, true), new Check(MurimBlocks.SAXAUL.get(), MurimBlocks.GOBI_GRAVEL.get(), true),
                    new Check(MurimBlocks.FEATHER_GRASS.get(), MurimBlocks.LOESS.get(), true), new Check(MurimBlocks.KARST_FERN.get(), Blocks.SAND, false),
                    new Check(MurimBlocks.CHRYSANTHEMUM.get(), Blocks.GRASS_BLOCK, true), new Check(MurimBlocks.WILD_RICE.get(), Blocks.MUD, true),
                    new Check(MurimBlocks.TAMARISK.get(), Blocks.RED_SAND, true), new Check(MurimBlocks.ARROW_BAMBOO.get(), Blocks.STONE, false))) {
                level.setBlockAndUpdate(at.below(), c.soil().defaultBlockState());
                boolean holds = c.plant().defaultBlockState().canSurvive(level, at);
                if (holds != c.holds()) failures.append(BuiltInRegistries.BLOCK.getKey(c.plant())).append(" on ").append(BuiltInRegistries.BLOCK.getKey(c.soil())).append(' ');
            }
            // Lotus floats on water only; strands hang only under something.
            level.setBlockAndUpdate(at.below(), Blocks.WATER.defaultBlockState());
            if (!MurimBlocks.LOTUS_BLOSSOM.get().defaultBlockState().canSurvive(level, at)) failures.append("lotus on water ");
            level.setBlockAndUpdate(at.above(), MurimBlocks.WILLOW.leaves().get().defaultBlockState());
            if (!MurimBlocks.WILLOW_STRANDS.get().defaultBlockState().canSurvive(level, at)) failures.append("willow strands under leaves ");
            level.setBlockAndUpdate(at.above(), Blocks.AIR.defaultBlockState());
            if (MurimBlocks.WILLOW_STRANDS.get().defaultBlockState().canSurvive(level, at)) failures.append("willow strands in mid-air ");
            helper.assertTrue(failures.isEmpty(), "Wrong soil rules: " + failures);
            helper.assertTrue(MurimBlocks.WILLOW.log().get().defaultBlockState().is(BlockTags.LOGS), "Murim logs are not #minecraft:logs (leaves would decay)");
            helper.succeed();
        } finally { clear(helper, x); }
    }
}
