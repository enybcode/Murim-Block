package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent.RenderFog;
import net.minecraft.client.renderer.FogRenderer;

/**
 * Mist of the Murim biomes, as in shan shui painting: the far plane of the terrain fog comes closer in the misty
 * ones, so ranges fade one behind the other. Thickest among the Huangshan peaks (sea of clouds), lighter in the karst,
 * the bamboo sea, the water country and the peach valley; none on the loess and in the desert. Eased in and out
 * over a few seconds when walking between biomes. A biome's own fog colour is in its JSON.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MurimMistClient {
    private static float current;
    private static float target;

    private MurimMistClient() { }

    /** Fraction of the normal fog distance kept in each biome (1 = no mist). */
    static float mistFor(ResourceKey<Biome> biome) {
        if (biome == MurimBiomes.CLOUD_SEA_PEAKS) return 0.38F;
        if (biome == MurimBiomes.JADE_KARST_FOREST) return 0.55F;
        if (biome == MurimBiomes.SHU_BAMBOO_SEA) return 0.5F;
        if (biome == MurimBiomes.JIANGNAN_WATERLANDS) return 0.65F;
        if (biome == MurimBiomes.PEACH_BLOSSOM_VALLEY) return 0.7F;
        // The stone forest: the far pillars fade into a blue haze, rank behind rank.
        if (biome == MurimBiomes.WULINGYUAN_PILLARS) return 0.62F;
        return 1.0F;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { current = target = 0; return; }
        target = mc.level.getBiome(mc.gameRenderer.getMainCamera().getBlockPosition()).unwrapKey()
                .map(key -> 1 - mistFor(key)).orElse(0F);
        // About three seconds to settle.
        current += Mth.clamp(target - current, -0.02F, 0.02F);
    }

    @SubscribeEvent
    public static void fog(RenderFog event) {
        if (current <= 0.001F || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) return;
        float far = event.getFarPlaneDistance() * (1 - current);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.12F));
        event.setCanceled(true);
    }

    /** For the visual tour: settle the mist at once after a teleport. */
    public static void settleNow() {
        current = target;
    }
}
