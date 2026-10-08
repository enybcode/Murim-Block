package com.murimblock.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import yesman.epicfight.client.input.EpicFightKeyMappings;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EpicFightControls {
    private EpicFightControls() { }

    private static Set<KeyMapping> disabled() {
        return Set.of(EpicFightKeyMappings.SWITCH_MODE, EpicFightKeyMappings.SKILL_EDIT,
                EpicFightKeyMappings.OPEN_CONFIG_SCREEN, EpicFightKeyMappings.OPEN_EMOTE_WHEEL,
                EpicFightKeyMappings.SWITCH_VANILLA_MODEL_DEBUGGING);
    }

    public static boolean hidden(KeyMapping mapping) {
        return disabled().contains(mapping) || mapping == EpicFightKeyMappings.WEAPON_INNATE_SKILL_TOOLTIP;
    }

    public static void configure() {
        Minecraft minecraft = Minecraft.getInstance();
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping.getCategory().startsWith("key.epicfight.")) mapping.category = MurimblockKeyMappings.CATEGORY;
        }
        // Reset All must not restore native shortcuts for screens/mode controls replaced by Murimblock.
        for (KeyMapping mapping : disabled()) {
            mapping.defaultKey = InputConstants.UNKNOWN;
            mapping.setKey(InputConstants.UNKNOWN);
            mapping.setDown(false);
            while (mapping.consumeClick()) { }
        }
        KeyMapping.resetMapping();
    }

    @SubscribeEvent
    public static void beforeControls(ScreenEvent.Init.Pre event) {
        if (event.getScreen() instanceof KeyBindsScreen) configure();
    }

    @SubscribeEvent
    public static void afterControls(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof KeyBindsScreen)) return;
        for (var listener : event.getListenersList()) {
            if (listener instanceof KeyBindsList list) {
                // Remove only presentation rows, not registered mappings or saved options.
                list.children().removeIf(entry -> entry instanceof KeyBindsList.KeyEntry key && hidden(key.key));
            }
        }
    }
}
