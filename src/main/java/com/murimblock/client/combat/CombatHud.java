package com.murimblock.client.combat;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.combat.CombatState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * What combat mode shows: the Qi a dodge, a guard or a technique spends, as a bar above the hotbar, and a short white
 * flash when a heavy blow lands on the player or breaks its guard.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    private static int flash;

    private CombatHud() {
    }

    /** A flash over the screen for a few ticks (a heavy blow taken). */
    public static void flash(int ticks) {
        flash = Math.max(flash, ticks);
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
                "combat"), CombatHud::draw);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (flash > 0) {
            flash--;
        }
    }

    private static void draw(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        if (flash > 0) {
            int alpha = Math.min(110, flash * 40);
            graphics.fill(0, 0, width, height, alpha << 24 | 0xFFFFFF);
        }
        CombatState local = CombatClient.local();
        if (!local.mode || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        var qi = ClientCultivation.get();
        int barWidth = 81, x = width / 2 - 91, y = height - 32 - 10 - (mc.player.isCreative() ? 0 : 10);
        float share = qi.maxQi() <= 0 ? 0 : (float) Math.min(1, qi.qi() / qi.maxQi());
        graphics.fill(x, y, x + barWidth, y + 4, 0xA0101820);
        graphics.fill(x + 1, y + 1, x + 1 + Math.round((barWidth - 2) * share), y + 3, 0xFF4FC3F7);
        Component label = Component.translatable("hud.murimblock.qi", (int) qi.qi(), qi.maxQi());
        graphics.drawString(mc.font, label, x, y - 9, 0xFFB3E5FC, true);
        if (LockOn.target() != null) {
            graphics.drawString(mc.font, Component.translatable("hud.murimblock.locked", LockOn.target().getDisplayName()),
                    width / 2 + 10, y - 9, 0xFFFF8A80, true);
        }
    }
}
