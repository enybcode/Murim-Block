package com.murimblock.client.hud;

import com.murimblock.Murimblock;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Draws the bottom HUD at the chosen {@link HudSize}: each of its layers (the hotbar or combat bar, the Qi or
 * experience bar, hearts, armour, hunger, air, the held item's name and the action bar) is scaled around the bottom
 * centre of the screen, so they keep their places relative to one another.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class HudScale {
    private static final Set<ResourceLocation> BOTTOM = Set.of(
            VanillaGuiLayers.HOTBAR, VanillaGuiLayers.JUMP_METER, VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP, VanillaGuiLayers.OVERLAY_MESSAGE
    );
    /** Whether the current layer's pose was pushed, to pop it once whether the layer was drawn or cancelled. */
    private static boolean pushed;

    private HudScale() {
    }

    /** HUD pixels per GUI pixel for the size in use; 1 at Normal. */
    public static float factor() {
        Minecraft mc = Minecraft.getInstance();
        int gui = (int) mc.getWindow().getGuiScale();
        return (float) HudSize.current().pixels(gui, mc.getWindow().getWidth()) / gui;
    }

    /** The chat's margin above the bottom of the screen, scaled with the HUD (40 GUI pixels at Normal). */
    public static int chatMargin(int margin) {
        return Math.round(margin * factor());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (pushed || !BOTTOM.contains(event.getName())) return;
        float scale = factor();
        if (scale == 1.0F) return;
        GuiGraphics g = event.getGuiGraphics();
        // x -> x * scale + (1 - scale) * centre, the shift rounded to whole screen pixels so the pixels stay even.
        float gui = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float dx = Math.round(g.guiWidth() / 2.0F * (1 - scale) * gui) / gui;
        float dy = Math.round(g.guiHeight() * (1 - scale) * gui) / gui;
        g.pose().pushPose();
        g.pose().translate(dx, dy, 0);
        g.pose().scale(scale, scale, 1);
        pushed = true;
    }

    /** A cancelled layer (the hotbar our combat bar replaces) gets no Post event: its pose is popped here. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLayerCancelled(RenderGuiLayerEvent.Pre event) {
        if (event.isCanceled()) pop(event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        pop(event.getGuiGraphics());
    }

    private static void pop(GuiGraphics g) {
        if (!pushed) return;
        g.pose().popPose();
        pushed = false;
    }
}
