package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The locator pendants' needles: the vanilla compass "angle", aimed at the pendant's target, picks one of each
 * pendant's 32 frames.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class LocatorPendantClient {
    private LocatorPendantClient() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MurimItems.pendants().forEach(pendant -> ItemProperties.register(pendant.get(),
                ResourceLocation.withDefaultNamespace("angle"), new CompassItemPropertyFunction((level, stack, entity) -> {
                    var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    return tracker == null ? null : tracker.target().orElse(null);
                }))));
    }
}
