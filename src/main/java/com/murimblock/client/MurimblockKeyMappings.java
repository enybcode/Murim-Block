package com.murimblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MurimblockKeyMappings {
    public static final String CATEGORY = "key.categories." + Murimblock.MOD_ID;
    public static final KeyMapping CHARGE_QI = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".charge_qi",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY
    );

    private MurimblockKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHARGE_QI);
    }
}
