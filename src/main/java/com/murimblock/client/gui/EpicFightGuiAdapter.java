package com.murimblock.client.gui;

import com.murimblock.Murimblock;
import com.murimblock.client.EpicFightControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.config.ClientConfig;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EpicFightGuiAdapter {
    private EpicFightGuiAdapter() { }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        EpicFightControls.configure();
        ClientConfig.showTargetIndicator = false;
        ClientConfig.healthBarVisibility = ClientConfig.HealthBarVisibility.NONE;
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        var minecraft = Minecraft.getInstance();
        var screen = event.getNewScreen();
        if (screen == null || !isNativeScreen(screen.getClass().getName())) return;
        if (minecraft.player == null) {
            event.setNewScreen(new OptionsScreen(minecraft.screen, minecraft.options));
        } else if (screen instanceof SkillEditScreen || screen instanceof SkillBookScreen) {
            event.setNewScreen(MurimProfileScreen.techniques());
        } else {
            event.setNewScreen(MurimProfileScreen.combatSettings());
        }
    }

    public static boolean isNativeScreen(String className) {
        return className.startsWith("yesman.epicfight.client.gui.screen.")
                || className.startsWith("yesman.epicfight.client.gui.datapack.");
    }
}
