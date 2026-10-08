package com.murimblock.client.gui;

import com.murimblock.Murimblock;
import com.murimblock.client.EpicFightControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SkillEditScreen;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.item.SkillBookItem;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EpicFightGuiAdapter {
    private static InteractionHand pendingHand;
    private static long pendingTick = -1;
    private EpicFightGuiAdapter() { }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        EpicFightControls.configure();
        ClientConfig.showTargetIndicator = false;
        ClientConfig.healthBarVisibility = ClientConfig.HealthBarVisibility.NONE;
        pendingHand = null;
        pendingTick = -1;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBookUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide && event.getItemStack().getItem() instanceof SkillBookItem) {
            pendingHand = event.getHand();
            pendingTick = event.getLevel().getGameTime();
        }
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        var minecraft = Minecraft.getInstance();
        var screen = event.getNewScreen();
        if (screen == null || !isNativeScreen(screen.getClass().getName())) return;
        if (minecraft.player == null) {
            event.setNewScreen(new OptionsScreen(minecraft.screen, minecraft.options));
        } else if (screen instanceof SkillEditScreen) {
            event.setNewScreen(MurimProfileScreen.techniques(null, null));
        } else if (screen instanceof SkillBookScreen) {
            var player = Minecraft.getInstance().player;
            var hand = player != null && pendingTick == player.level().getGameTime() ? pendingHand : null;
            var skill = hand == null ? null : SkillBookItem.getContainSkill(player.getItemInHand(hand))
                    .map(holder -> holder.getKey().location()).orElse(null);
            event.setNewScreen(MurimProfileScreen.techniques(skill, hand));
            pendingHand = null;
            pendingTick = -1;
        } else {
            event.setNewScreen(MurimProfileScreen.combatSettings());
        }
    }

    public static boolean isNativeScreen(String className) {
        return className.startsWith("yesman.epicfight.client.gui.screen.")
                || className.startsWith("yesman.epicfight.client.gui.datapack.");
    }
}
