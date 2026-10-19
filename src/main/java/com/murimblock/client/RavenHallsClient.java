package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.world.raven.RavenHalls;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The air of the Raven Halls: a dark mist, near, that swallows the castle's levels and galleries a few dozen blocks
 * off, so that they seem to go on for ever in every direction, their lanterns the last thing seen.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenHallsClient {
    private RavenHallsClient() {
    }

    private static boolean inHalls() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == RavenHalls.HALLS;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inHalls()) return;
        event.setNearPlaneDistance(12);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 110));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        if (!inHalls()) return;
        event.setRed(0.035F);
        event.setGreen(0.025F);
        event.setBlue(0.06F);
    }
}
