package com.murimblock.world;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils.Continentalness;
import terrablender.api.ParameterUtils.Depth;
import terrablender.api.ParameterUtils.Erosion;
import terrablender.api.ParameterUtils.Humidity;
import terrablender.api.ParameterUtils.ParameterPointListBuilder;
import terrablender.api.ParameterUtils.Temperature;
import terrablender.api.ParameterUtils.Weirdness;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

/**
 * TerraBlender region of the Murim lands: every temperate and warm land climate in it belongs to one of the seven
 * biomes, so they come in large coherent stretches instead of small patches; cold climates, oceans, beaches and
 * river valleys stay vanilla. Each biome takes the slots whose vanilla terrain matches its real topography
 * (Minecraft terrain comes from shared noise, not from the biome), and the three large biomes take the most room:
 * <ul>
 * <li>karst (large): hot and humid land, every relief (towers are a feature);</li>
 * <li>frontier desert (large): hot and dry land;</li>
 * <li>loess (large): temperate to warm dry land, plateaus and uplands;</li>
 * <li>peach valley (medium): temperate, middle humidity, valleys and hills;</li>
 * <li>Shu bamboo (medium): temperate to warm, wet hills (erosion 1 and 2);</li>
 * <li>Jiangnan (medium): temperate to warm, wet flat inland lowlands (erosion 3 to 6), not the sea coast;</li>
 * <li>cloud-sea peaks (medium): cool inland mountains, and the most mountainous temperate wet slots (erosion 0).</li>
 * </ul>
 * No two Murim biomes share a slot.
 */
public final class MurimRegion extends Region {
    /** Out of the overworld regions; vanilla's is 10 by TerraBlender's default config. */
    public static final int WEIGHT = 7;

    private static final Weirdness[] NO_RIVER = {Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.HIGH_SLICE_NORMAL_ASCENDING,
            Weirdness.PEAK_NORMAL, Weirdness.HIGH_SLICE_NORMAL_DESCENDING, Weirdness.MID_SLICE_NORMAL_DESCENDING,
            Weirdness.LOW_SLICE_NORMAL_DESCENDING, Weirdness.LOW_SLICE_VARIANT_ASCENDING, Weirdness.MID_SLICE_VARIANT_ASCENDING,
            Weirdness.HIGH_SLICE_VARIANT_ASCENDING, Weirdness.PEAK_VARIANT, Weirdness.HIGH_SLICE_VARIANT_DESCENDING,
            Weirdness.MID_SLICE_VARIANT_DESCENDING};

    public MurimRegion(ResourceLocation name) {
        super(name, RegionType.OVERWORLD, WEIGHT);
    }

    @Override public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();
        // Large.
        add(builder, MurimBiomes.JADE_KARST_FOREST, land().temperature(Temperature.HOT)
                .humidity(Humidity.NEUTRAL, Humidity.WET, Humidity.HUMID).erosion(Erosion.FULL_RANGE));
        add(builder, MurimBiomes.HEXI_FRONTIER_DESERT, land().temperature(Temperature.HOT)
                .humidity(Humidity.ARID, Humidity.DRY).erosion(Erosion.FULL_RANGE));
        add(builder, MurimBiomes.LOESS_PLATEAU, land().temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.ARID, Humidity.DRY).erosion(Erosion.FULL_RANGE));
        // Medium.
        add(builder, MurimBiomes.PEACH_BLOSSOM_VALLEY, land().temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.NEUTRAL).erosion(Erosion.span(Erosion.EROSION_1, Erosion.EROSION_6)));
        add(builder, MurimBiomes.SHU_BAMBOO_SEA, land().temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.WET, Humidity.HUMID).erosion(Erosion.span(Erosion.EROSION_1, Erosion.EROSION_2)));
        // Low inland plains, not the sea coast: the water country is ponds and canals, not open sea.
        add(builder, MurimBiomes.JIANGNAN_WATERLANDS, new ParameterPointListBuilder()
                .continentalness(Continentalness.span(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)).weirdness(NO_RIVER)
                .temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.WET, Humidity.HUMID).erosion(Erosion.span(Erosion.EROSION_3, Erosion.EROSION_6)));
        // Cool mountains only inland, where the vanilla relief really rises; cool coasts stay vanilla.
        add(builder, MurimBiomes.CLOUD_SEA_PEAKS, inland().temperature(Temperature.COOL)
                .humidity(Humidity.FULL_RANGE).erosion(Erosion.span(Erosion.EROSION_0, Erosion.EROSION_2)));
        add(builder, MurimBiomes.CLOUD_SEA_PEAKS, land().temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.NEUTRAL, Humidity.WET, Humidity.HUMID).erosion(Erosion.EROSION_0));
        builder.build().forEach(mapper);
    }

    /** All land from the coast inland, river valleys excluded. */
    private static ParameterPointListBuilder land() {
        return new ParameterPointListBuilder()
                .continentalness(Continentalness.span(Continentalness.COAST, Continentalness.FAR_INLAND))
                .weirdness(NO_RIVER);
    }

    /** Mid and far inland, river valleys excluded. */
    private static ParameterPointListBuilder inland() {
        return new ParameterPointListBuilder()
                .continentalness(Continentalness.span(Continentalness.MID_INLAND, Continentalness.FAR_INLAND))
                .weirdness(NO_RIVER);
    }

    private static void add(VanillaParameterOverlayBuilder builder, ResourceKey<Biome> biome, ParameterPointListBuilder points) {
        points.depth(Depth.SURFACE, Depth.FLOOR).build().forEach(point -> builder.add(point, biome));
    }
}
