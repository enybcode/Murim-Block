package com.murimblock.client.world;

import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiome;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The Murim map's mist (docs/world/map.md, step 5): in the damp or high zones, the terrain fog closes in to a share of
 * its normal distance, so peaks and forests fade one behind the other. The share eases from zone to zone over a few
 * seconds instead of jumping at the border.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MurimFog {
    /** The share of the normal fog distance kept in each misty biome; the others keep all of it. */
    private static final Map<MurimBiome, Float> MIST = new EnumMap<>(MurimBiome.class);
    /** How much of the gap to the wanted share is closed each tick (about 4 s to settle). */
    private static final float EASE = 0.02F;

    static {
        MIST.put(MurimBiome.SACRED_MOUNTAIN, 0.38F);
        MIST.put(MurimBiome.STONE_PEAKS, 0.5F);
        MIST.put(MurimBiome.BAMBOO_MOUNTAINS, 0.55F);
        MIST.put(MurimBiome.WESTERN_GORGES, 0.55F);
        MIST.put(MurimBiome.UNNAM_JUNGLE, 0.6F);
        MIST.put(MurimBiome.FROZEN_TUNDRA, 0.6F);
        MIST.put(MurimBiome.WESTERN_SNOW_PEAKS, 0.65F);
        MIST.put(MurimBiome.HUBEI_WATERLANDS, 0.7F);
        MIST.put(MurimBiome.RIVER_DELTA, 0.7F);
        MIST.put(MurimBiome.SUBTROPICAL_FOREST, 0.75F);
        MIST.put(MurimBiome.STONE_FOREST, 0.75F);
        MIST.put(MurimBiome.COLD_DESERT, 0.8F);
    }

    private static float share = 1;

    private MurimFog() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            share = 1;
            return;
        }
        String id = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                .filter(k -> k.location().getNamespace().equals(Murimblock.MOD_ID))
                .map(k -> k.location().getPath()).orElse("");
        float wanted = 1;
        for (Map.Entry<MurimBiome, Float> e : MIST.entrySet()) {
            if (e.getKey().key.equals(id)) {
                wanted = e.getValue();
            }
        }
        share += (wanted - share) * EASE;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (share > 0.995F || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        event.setNearPlaneDistance(event.getNearPlaneDistance() * share);
        event.setFarPlaneDistance(event.getFarPlaneDistance() * share);
        event.setCanceled(true);
    }
}
