package com.murimblock.client.hud;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import yesman.epicfight.skill.modules.ChargeableSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EpicFightHud {
    private static final Set<String> LAYERS = Set.of("stamina_bar", "skills", "weapon_innate", "charging_bar");
    private EpicFightHud() { }

    public static boolean replaces(ResourceLocation name) {
        return name.getNamespace().equals("epicfight") && LAYERS.contains(name.getPath());
    }

    public static int fillWidth(double current, double maximum, int width) {
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || maximum <= 0 || width <= 0) return 0;
        return (int) Math.round(Math.clamp(current / maximum, 0, 1) * width);
    }

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (!replaces(event.getName())) return;
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getName().getPath().equals("stamina_bar") || minecraft.player == null
                || minecraft.options.hideGui || minecraft.screen != null || !minecraft.player.isAlive()
                || minecraft.player.isSpectator()) return;
        PlayerPatch<?> patch = EpicFightBridge.patch(minecraft.player);
        if (patch == null || !patch.isEpicFightMode()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int center = graphics.guiWidth() / 2;
        int x = Math.max(5, center - 158);
        int y = graphics.guiHeight() - 15;
        if (patch.isHoldingAny() && patch.getHoldingSkill() instanceof ChargeableSkill charge) {
            meter(graphics, x, y - 8, 54, patch.getSkillChargingTicks(), charge.getMaxChargingTicks(), 0xFFF3EEE0);
        }
    }

    private static void meter(GuiGraphics graphics, int x, int y, int width, double value, double maximum, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xFF171D1B);
        graphics.fill(x, y, x + width, y + 3, 0xFF44483D);
        int fill = fillWidth(value, maximum, width);
        if (fill > 0) graphics.fill(x, y, x + fill, y + 3, color);
    }
}
