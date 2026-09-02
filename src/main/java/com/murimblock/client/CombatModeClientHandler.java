package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.network.CombatModeTogglePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatModeClientHandler {
    private CombatModeClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (MurimblockKeyMappings.COMBAT_MODE.consumeClick()) {
            PacketDistributor.sendToServer(CombatModeTogglePayload.INSTANCE);
        }
    }
}
