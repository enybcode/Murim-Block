package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.world.MurimBiomes;
import com.murimblock.world.map.MurimBiomeSource;
import com.murimblock.world.map.MurimMap;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * The Murim map with the real noise (docs/biomes/murim-map.md): the "normal" world preset puts it on the overworld,
 * and over the whole map (a sample every 100 blocks, two seeds) every land block of the bubble has a Murim biome of
 * its own region, the ring is deep ocean and the world beyond is vanilla. It also draws the map: murim-map-<seed>.png
 * in the GameTest server's folder (build/game-test-run).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MurimMapGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HALF = MurimMap.RADIUS + MurimMap.RING + 2_000, STEP = 100;

    private MurimMapGameTests() { }

    private static final Map<MurimMap.Region, Set<ResourceKey<Biome>>> REGION_BIOMES = Map.of(
            MurimMap.Region.CENTER, Set.of(MurimBiomes.CLOUD_SEA_PEAKS, MurimBiomes.PEACH_BLOSSOM_VALLEY),
            MurimMap.Region.NORTH, Set.of(MurimBiomes.KUNLUN_GLACIERS, MurimBiomes.TIANSHAN_SNOWFIELDS),
            MurimMap.Region.WEST, Set.of(MurimBiomes.HEXI_FRONTIER_DESERT, MurimBiomes.LOESS_PLATEAU),
            MurimMap.Region.SOUTH, Set.of(MurimBiomes.JADE_KARST_FOREST, MurimBiomes.SHU_BAMBOO_SEA, MurimBiomes.CAMPHOR_HILLS),
            MurimMap.Region.EAST, Set.of(MurimBiomes.JIANGNAN_WATERLANDS, MurimBiomes.CLOUD_SEA_PEAKS, MurimBiomes.CAMPHOR_HILLS));

    private static final Map<ResourceKey<Biome>, Integer> COLOURS = Map.of(
            MurimBiomes.CLOUD_SEA_PEAKS, 0xB8BEC4, MurimBiomes.PEACH_BLOSSOM_VALLEY, 0xE8A0B8,
            MurimBiomes.KUNLUN_GLACIERS, 0x9ED8F0, MurimBiomes.TIANSHAN_SNOWFIELDS, 0xF2F6FA,
            MurimBiomes.HEXI_FRONTIER_DESERT, 0xE2C27A, MurimBiomes.LOESS_PLATEAU, 0xB8904A,
            MurimBiomes.JADE_KARST_FOREST, 0x2E7A3A, MurimBiomes.SHU_BAMBOO_SEA, 0x7EC24A,
            MurimBiomes.JIANGNAN_WATERLANDS, 0x4AA89A, MurimBiomes.CAMPHOR_HILLS, 0x3F6E35);

    @GameTest(template = "foundation_arena", batch = "murim_map_preset")
    public static void theNormalWorldPresetPutsTheMurimMapOnTheOverworld(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var preset = access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value();
        LevelStem overworld = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD);
        helper.assertTrue(overworld != null && overworld.generator().getBiomeSource() instanceof MurimBiomeSource,
                "The normal preset's overworld has no Murim map: " + (overworld == null ? null : overworld.generator().getBiomeSource()));
        helper.assertTrue(overworld.generator().getBiomeSource().possibleBiomes().stream().anyMatch(b -> b.is(MurimBiomes.KUNLUN_GLACIERS)),
                "The Murim biomes are not among the overworld's");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "murim_map_layout", timeoutTicks = 2000)
    public static void everyRegionHasItsOwnBiomesAndTheRingIsSea(GameTestHelper helper) {
        for (long seed : new long[]{1L, 8_675_309L}) survey(helper, seed);
        helper.succeed();
    }

    /**
     * The north's ranges: the ground's height over the whole north (a point every 250 blocks), with the real terrain
     * noise. Immense (the highest well above vanilla's usual peaks) without ever reaching the world's ceiling.
     */
    @GameTest(template = "foundation_arena", batch = "murim_map_heights", timeoutTicks = 2000)
    public static void theNorthRisesIntoImmenseRangesUnderTheCeiling(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        RandomState state = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), 1L);
        var vanilla = MultiNoiseBiomeSource.createFromPreset(access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var biomes = access.registryOrThrow(Registries.BIOME);
        var generator = new NoiseBasedChunkGenerator(new MurimBiomeSource(vanilla,
                HolderSet.direct(MurimBiomes.ALL.stream().map(biomes::getHolderOrThrow).toList())), settings);
        var heightAccessor = net.minecraft.world.level.LevelHeightAccessor.create(-64, 384);
        int highest = 0, count = 0, above200 = 0;
        long sum = 0;
        for (int x = -19_000; x <= 19_000; x += 250) for (int z = -19_000; z <= 19_000; z += 250) {
            if (MurimMap.region(x, z) != MurimMap.Region.NORTH) continue;
            int h = generator.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, state);
            highest = Math.max(highest, h);
            sum += h;
            count++;
            if (h > 200) above200++;
        }
        LOGGER.info("Murim north: {} points, mean height {}, highest {}, {} above 200", count, sum / Math.max(1, count), highest, above200);
        helper.assertTrue(highest < 316, "The north's ranges reach the world's ceiling: " + highest);
        helper.assertTrue(highest > 230, "The north's ranges are not immense: the highest point is at " + highest);
        helper.succeed();
    }

    private static void survey(GameTestHelper helper, long seed) {
        var access = helper.getLevel().registryAccess();
        var biomes = access.registryOrThrow(Registries.BIOME);
        RandomState state = RandomState.create(access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD).value(),
                access.lookupOrThrow(Registries.NOISE), seed);
        var vanilla = MultiNoiseBiomeSource.createFromPreset(access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var source = new MurimBiomeSource(vanilla, HolderSet.direct(MurimBiomes.ALL.stream().map(biomes::getHolderOrThrow).toList()));
        int size = 2 * HALF / STEP;
        BufferedImage map = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Map<String, Integer> tally = new TreeMap<>();
        Map<MurimMap.Region, Integer> murimByRegion = new HashMap<>();
        int wrong = 0, ringLand = 0, insideVanillaLand = 0;
        String firstWrong = null;
        for (int px = 0; px < size; px++) for (int pz = 0; pz < size; pz++) {
            int x = -HALF + px * STEP, z = -HALF + pz * STEP;
            // High above the ground: the surface's biome (lower down, vanilla's caves under the mountains).
            Holder<Biome> b = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(200), QuartPos.fromBlock(z), state.sampler());
            ResourceKey<Biome> key = b.unwrapKey().orElseThrow();
            tally.merge(key.location().getPath(), 1, Integer::sum);
            boolean sea = b.is(BiomeTags.IS_OCEAN);
            boolean murim = b.is(MurimBiomes.MURIM_TAG) || MurimBiomes.ALL.contains(key);
            double reach = MurimMap.reach(x, z);
            if (murim) {
                MurimMap.Region region = MurimMap.region(x, z);
                murimByRegion.merge(region, 1, Integer::sum);
                if (!REGION_BIOMES.get(region).contains(key)) {
                    wrong++;
                    if (firstWrong == null) firstWrong = key.location() + " in the " + region + " at " + x + ", " + z;
                }
            } else if (reach < MurimMap.RADIUS - MurimMap.EDGE - 200 && !sea && !b.is(BiomeTags.IS_BEACH) && !b.is(BiomeTags.IS_RIVER)
                    && !b.is(net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES) && !b.is(net.minecraft.world.level.biome.Biomes.LUSH_CAVES)
                    && !b.is(net.minecraft.world.level.biome.Biomes.DEEP_DARK)) {
                insideVanillaLand++;
            }
            if (reach > MurimMap.RADIUS + MurimMap.EDGE + 200 && reach < MurimMap.RADIUS + MurimMap.RING - MurimMap.EDGE - 200 && !sea) ringLand++;
            int colour = COLOURS.getOrDefault(key, sea ? (b.is(BiomeTags.IS_DEEP_OCEAN) ? 0x1A3A6A : 0x2E5C9A) : 0x6E7A68);
            map.setRGB(px, pz, colour);
        }
        try {
            Path out = helper.getLevel().getServer().getServerDirectory().resolve("murim-map-" + seed + ".png");
            ImageIO.write(map, "png", out.toFile());
            LOGGER.info("Murim map {}: {} ({} samples of {} blocks) {}", seed, out.toAbsolutePath(), size * size, STEP, tally);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        helper.assertTrue(wrong == 0, "A Murim biome outside its region (" + wrong + "), first: " + firstWrong);
        helper.assertTrue(ringLand == 0, "Land in the ocean ring: " + ringLand + " samples");
        // Inside the bubble only Murim land (vanilla keeps the caves, its rivers become Murim: none of its land biomes).
        helper.assertTrue(insideVanillaLand == 0, "Vanilla land inside the bubble: " + insideVanillaLand + " samples " + tally);
        // The five regions hold about the same land: each between a seventh and a quarter of it.
        int land = murimByRegion.values().stream().mapToInt(Integer::intValue).sum();
        LOGGER.info("Murim map {}: layout turned {} degrees, quarters {}, land by region {}", seed, Math.round(Math.toDegrees(MurimMap.layout().turn())),
                java.util.Arrays.toString(MurimMap.layout().quarters()), murimByRegion);
        for (MurimMap.Region r : MurimMap.Region.values()) {
            int n = murimByRegion.getOrDefault(r, 0);
            helper.assertTrue(n > land / 7 && n < land / 4, "The " + r + " holds " + n + " of " + land + " samples of land: " + murimByRegion);
        }
        for (ResourceKey<Biome> b : MurimBiomes.ALL)
            helper.assertTrue(tally.getOrDefault(b.location().getPath(), 0) > 50, "Hardly any " + b.location() + " on the map: " + tally);
    }
}
