package com.murimblock.client.animation;

import com.murimblock.Murimblock;
import com.zigythebird.playeranim.neoforge.event.PlayerAnimationRegisterEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatPreviewClientEvents {
    private CombatPreviewClientEvents() { }

    @SubscribeEvent
    public static void register(PlayerAnimationRegisterEvent event) {
        event.getAnimManager().addAnimLayer(800, new PalPreviewController(event.getClientPlayer()));
    }
}
