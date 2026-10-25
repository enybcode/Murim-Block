package com.murimblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.cultivation.CultivationPayloads;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Murimblock key bindings and what they open. */
public final class MurimKeys {
    public static final String CATEGORY = "key.categories.murimblock";
    public static final KeyMapping OPEN_SYSTEM = new KeyMapping("key.murimblock.open_system",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping MEDITATE = new KeyMapping("key.murimblock.meditate",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    private MurimKeys() {
    }

    /** NeoForge sends each event to its own bus (mod or game) from the event type. */
    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private static boolean sneakWasDown;

        private Events() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SYSTEM);
            event.register(MEDITATE);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_SYSTEM.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    mc.setScreen(new SystemScreen(SystemPage.PROFILE));
                }
            }
            // While meditating the key is a breath of the QTE (read by BreathingHud), and sneaking stands up.
            boolean meditating = ClientCultivation.get().meditating();
            while (MEDITATE.consumeClick()) {
                if (mc.screen == null && mc.player != null && !meditating) {
                    PacketDistributor.sendToServer(new CultivationPayloads.ToggleMeditation());
                }
            }
            boolean sneak = mc.options.keyShift.isDown();
            if (meditating && sneak && !sneakWasDown && mc.screen == null) {
                PacketDistributor.sendToServer(new CultivationPayloads.ToggleMeditation());
            }
            sneakWasDown = sneak;
        }
    }
}
