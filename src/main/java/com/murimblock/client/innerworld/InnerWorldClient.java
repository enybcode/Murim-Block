package com.murimblock.client.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.innerworld.InnerWorldEvents;
import com.murimblock.registry.MurimEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of the inner world: the body's renderer, the travel request, and no pick-block copies inside. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldClient {
    private InnerWorldClient() {
    }

    public static boolean inside() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && InnerWorld.isInnerWorld(mc.level);
    }

    public static void travel() {
        PacketDistributor.sendToServer(new InnerWorldEvents.Travel());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MEDITATING_BODY.get(), MeditatingBodyRenderer::new);
    }

    /** Creative pick-block would hand out a copy of the island's blocks. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isPickBlock() && inside()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
