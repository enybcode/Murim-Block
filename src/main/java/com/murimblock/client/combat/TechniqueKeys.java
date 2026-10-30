package com.murimblock.client.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The keys of the active art's six techniques (1 to 6) and its ultimate (R), rebindable in Minecraft's Controls under
 * Murimblock. They act only in combat mode: there they share the number keys with the hotbar, which then stops changing
 * slot. Controls still marks the shared keys in red (NeoForge flags any two bindings on one key); it is harmless.
 * Nothing is cast yet (techniques wait for their design); the HUD and the System menu
 * show the keys as bound.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TechniqueKeys {
    /** In game and in combat mode only; shares keys with everything else. */
    public static final IKeyConflictContext IN_COMBAT = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return KeyConflictContext.IN_GAME.isActive() && CombatClient.local().mode;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };
    public static final int COUNT = 6;
    public static final KeyMapping[] TECHNIQUES = new KeyMapping[COUNT];
    public static final KeyMapping ULTIMATE = new KeyMapping("key.murimblock.ultimate", IN_COMBAT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, MurimKeys.CATEGORY);

    static {
        for (int i = 0; i < COUNT; i++) {
            TECHNIQUES[i] = new KeyMapping("key.murimblock.technique_" + (i + 1), IN_COMBAT, InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_1 + i, MurimKeys.CATEGORY);
        }
    }

    private TechniqueKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : TECHNIQUES) {
            event.register(key);
        }
        event.register(ULTIMATE);
    }

    /** In combat mode the number keys belong to the techniques: the hotbar's clicks are dropped before they act. */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !CombatClient.local().mode) {
            return;
        }
        for (KeyMapping hotbar : mc.options.keyHotbarSlots) {
            for (KeyMapping technique : TECHNIQUES) {
                if (hotbar.getKey().equals(technique.getKey())) {
                    while (hotbar.consumeClick()) {
                        // Dropped: the technique key takes it.
                    }
                }
            }
        }
        for (KeyMapping technique : TECHNIQUES) {
            while (technique.consumeClick()) {
                // Nothing to cast yet.
            }
        }
        while (ULTIMATE.consumeClick()) {
            // Nothing to cast yet.
        }
    }

    /** The short name of technique {@code slot}'s key (0 to 5), as shown on the bar and in the menu. */
    public static String label(int slot) {
        return slot < 0 || slot >= COUNT ? "?" : shortName(TECHNIQUES[slot]);
    }

    public static String ultimateLabel() {
        return shortName(ULTIMATE);
    }

    private static String shortName(KeyMapping key) {
        if (key.isUnbound()) {
            return "-";
        }
        // The number row reads as its digit whatever the layout (an AZERTY 1 key is "&").
        InputConstants.Key bound = key.getKey();
        if (bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() >= GLFW.GLFW_KEY_0 && bound.getValue() <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + bound.getValue() - GLFW.GLFW_KEY_0));
        }
        String name = key.getTranslatedKeyMessage().getString();
        return name.length() <= 3 ? name.toUpperCase(java.util.Locale.ROOT) : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }
}
