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
    /** Meditate: a tap sits down in the lotus (or gets up); held while seated, it opens the inner world (or leaves it). */
    public static final KeyMapping MEDITATE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".charge_qi",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
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

    /** Qi Sense, held: read the auras around (from Qi Condensation). */
    public static final KeyMapping QI_SENSE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".qi_sense",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY
    );

    /** The aura's pressure: released, or held in (from Foundation Establishment). */
    public static final KeyMapping AURA_PRESSURE = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".aura_pressure",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY
    );

    /** The Murim map: the bubble and its regions, where the player stands. */
    public static final KeyMapping MURIM_MAP = new KeyMapping(
            "key." + Murimblock.MOD_ID + ".murim_map",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
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
        event.register(MEDITATE);
        event.register(QI_SENSE);
        event.register(COMBAT_MODE);
        event.register(OPEN_PROFILE);
        event.register(EXECUTE);
        event.register(COMBAT_SLOT_10);
        event.register(MURIM_MAP);
        event.register(AURA_PRESSURE);
    }
}
