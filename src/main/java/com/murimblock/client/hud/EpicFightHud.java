package com.murimblock.client.hud;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
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
        if (patch.getStamina() < patch.getMaxStamina()) {
            meter(graphics, x, y, 60, patch.getStamina(), patch.getMaxStamina(), 0xFFE4C263);
        }
        if (patch.isHoldingAny() && patch.getHoldingSkill() instanceof ChargeableSkill charge) {
            meter(graphics, x, y - 8, 54, patch.getSkillChargingTicks(), charge.getMaxChargingTicks(), 0xFFF3EEE0);
        }
        int index = 0;
        for (SkillContainer container : patch.getPlayerSkills().listSkillContainers().toList()) {
            if (container.isEmpty() || !container.getSkill().shouldDraw(container)) continue;
            boolean innate = container.getSlot() == SkillSlots.WEAPON_INNATE;
            boolean active = container.isActivated() || container.getMaxResource() > 0
                    && container.getResource() < container.getMaxResource();
            if (!innate && !active || index >= 3) continue;
            int sx = center + 99 + index++ * 19;
            graphics.fill(sx, y - 12, sx + 17, y + 4, 0xDD161B19);
            graphics.fill(sx, y - 12, sx + 17, y - 11, 0xFFD5B75F);
            String name = Component.translatable(container.getSkill().getTranslationKey()).getString();
            String initials = name.length() > 2 ? name.substring(0, 2) : name;
            graphics.drawString(minecraft.font, initials, sx + 2, y - 9, 0xFFF2E9D0, false);
            meter(graphics, sx + 2, y + 1, 13, container.getResource(), container.getMaxResource(),
                    container.isDisabled() ? 0xFF827B6D : 0xFFE4C263);
        }
    }

    private static void meter(GuiGraphics graphics, int x, int y, int width, double value, double maximum, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xFF171D1B);
        graphics.fill(x, y, x + width, y + 3, 0xFF44483D);
        int fill = fillWidth(value, maximum, width);
        if (fill > 0) graphics.fill(x, y, x + fill, y + 3, color);
    }
}
