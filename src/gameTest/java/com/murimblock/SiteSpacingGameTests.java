package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.world.MurimBiomes;
import com.murimblock.world.map.MurimBiomeSource;
import com.murimblock.world.structure.site.SiteSpacing;
import com.murimblock.world.structure.site.SiteStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * At least SiteSpacing.GAP blocks between any two Murim sites (Enzo, 16/10/2026). Over a square of the Murim map, with
 * the real biomes: every place a site's grid offers, kept if its biome suits it, it is allowed there and SiteSpacing
 * lets it be (the terrain can only refuse more); then no two kept places come within the gap, edge to edge, and every
 * kind of site still has places.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiteSpacingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HALF = 12_000;

    private SiteSpacingGameTests() { }

    private record Place(String set, int x, int z, int reach) { }

    @GameTest(template = "foundation_arena", batch = "site_spacing", timeoutTicks = 2000)
    public static void noTwoSitesComeWithinTheGap(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var biomes = access.registryOrThrow(Registries.BIOME);
        long seed = 1L;
        RandomState state = RandomState.create(access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD).value(),
                access.lookupOrThrow(Registries.NOISE), seed);
        var vanilla = MultiNoiseBiomeSource.createFromPreset(access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var source = new MurimBiomeSource(vanilla, HolderSet.direct(MurimBiomes.ALL.stream().map(biomes::getHolderOrThrow).toList()));
        int sea = 63;
        List<Place> kept = new ArrayList<>();
        Map<String, Integer> offered = new TreeMap<>(), crowded = new TreeMap<>(), keptBySet = new TreeMap<>();
        for (var entry : access.registryOrThrow(Registries.STRUCTURE_SET).entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(Murimblock.MOD_ID)) continue;
            if (!(entry.getValue().placement() instanceof RandomSpreadStructurePlacement spread)) continue;
            String name = entry.getKey().location().getPath();
            int spacing = spread.spacing();
            for (var choice : entry.getValue().structures()) {
                if (!(choice.structure().value() instanceof SiteStructure site)) continue;
                int reach = site.plan().site().reach();
                for (int rx = Math.floorDiv(-HALF >> 4, spacing); rx <= Math.floorDiv(HALF >> 4, spacing); rx++)
                    for (int rz = Math.floorDiv(-HALF >> 4, spacing); rz <= Math.floorDiv(HALF >> 4, spacing); rz++) {
                        ChunkPos c = spread.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                        int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
                        if (Math.abs(x) > HALF || Math.abs(z) > HALF) continue;
                        if (!site.plan().site().allowedAt(x, z)) continue;
                        var biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sea), QuartPos.fromBlock(z), state.sampler());
                        if (!site.biomes().contains(biome)) continue;
                        offered.merge(name, 1, Integer::sum);
                        if (SiteSpacing.crowded(access, seed, source, state.sampler(), sea, c, reach, entry.getKey())) {
                            crowded.merge(name, 1, Integer::sum);
                            continue;
                        }
                        kept.add(new Place(name, x, z, reach));
                        keptBySet.merge(name, 1, Integer::sum);
                    }
            }
        }
        LOGGER.info("Site spacing over {} x {}: offered {}, stepped aside {}, kept {}", 2 * HALF, 2 * HALF, offered, crowded, keptBySet);
        String closest = null;
        double worst = Double.MAX_VALUE;
        for (int i = 0; i < kept.size(); i++) for (int j = i + 1; j < kept.size(); j++) {
            Place a = kept.get(i), b = kept.get(j);
            double gap = Math.hypot(a.x - b.x, a.z - b.z) - a.reach - b.reach;
            if (gap < worst) { worst = gap; closest = a + " and " + b; }
        }
        LOGGER.info("Site spacing: the closest two kept sites are {} blocks apart, edge to edge: {}", Math.round(worst), closest);
        helper.assertTrue(worst >= SiteSpacing.GAP, "Two sites within " + SiteSpacing.GAP + " blocks: " + closest + " (" + Math.round(worst) + ")");
        for (String set : offered.keySet())
            helper.assertTrue(keptBySet.getOrDefault(set, 0) > 0, "Every place of " + set + " stepped aside: " + offered + " / " + keptBySet);
        helper.succeed();
    }
}
