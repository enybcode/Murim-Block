package com.murimblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MurimblockKeyMappings {
    public static final String CATEGORY = "key.categories." + Murimblock.MOD_ID;
    public static final KeyMapping CHARGE_QI = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".charge_qi",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY
    );
    public static final KeyMapping COMBAT_MODE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".combat_mode",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            CATEGORY
    );
    public static final KeyMapping OPEN_PROFILE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".open_profile",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
    );

    /** Executes a knocked-down player (V carries them: the combat mode key, next to a downed player). */
    public static final KeyMapping EXECUTE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".execute",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            CATEGORY
    );

    /** Tenth slot of the combat bar (Minecraft has nine hotbar keys; the first nine slots use them). */
    public static final KeyMapping COMBAT_SLOT_10 = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".combat_slot_10",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_0,
            CATEGORY
    );

    private MurimblockKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHARGE_QI);
        event.register(COMBAT_MODE);
        event.register(OPEN_PROFILE);
        event.register(EXECUTE);
        event.register(COMBAT_SLOT_10);
    }
}
