package com.murimblock.client.hud;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.qi.QiFormat;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatQiHud {
    static final int BAR_WIDTH = 182;
    static final int BAR_HEIGHT = 5;
    static final int BAR_Y_OFFSET = 29;
    static final int TEXT_Y_OFFSET = 12;
    static final int QI_COLOR = colorFromQiParticle();
    private static final int BACKGROUND_COLOR = 0xAA10151C;
    private static final int BORDER_DARK_COLOR = 0xFF05080C;
    private static final int BORDER_LIGHT_COLOR = 0xFF1B3857;
    private static final int TEXT_COLOR = 0xEAF6FF;

    private CombatQiHud() {
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!shouldReplaceExperienceHud()) {
            return;
        }

        if (VanillaGuiLayers.EXPERIENCE_BAR.equals(event.getName())) {
            render(event.getGuiGraphics());
            event.setCanceled(true);
            return;
        }

        if (VanillaGuiLayers.EXPERIENCE_LEVEL.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    static double computeQiRatio(double qi, double qiMax) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0.0) {
            return 0.0;
        }
        return Math.clamp(qi / qiMax, 0.0, 1.0);
    }

    static int computeFilledWidth(double qi, double qiMax) {
        return (int) Math.round(BAR_WIDTH * computeQiRatio(qi, qiMax));
    }

    static String buildText(double qi, double qiMax) {
        return "Qi " + QiFormat.format(qi) + " / " + QiFormat.format(qiMax);
    }

    private static boolean shouldReplaceExperienceHud() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null
                && minecraft.gameMode != null
                && player.jumpableVehicle() == null
                && minecraft.gameMode.hasExperience()
                && MurimblockApi.combat().isInCombatMode(player);
    }

    private static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        double qi = QiService.getQi(player);
        double qiMax = QiService.getQiMax(player);
        int x = guiGraphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() - BAR_Y_OFFSET;
        int filledWidth = computeFilledWidth(qi, qiMax);

        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_DARK_COLOR);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND_COLOR);
        if (filledWidth > 0) {
            guiGraphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, QI_COLOR);
            guiGraphics.fill(x, y, x + filledWidth, y + 1, highlightColor());
        }
        guiGraphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, BORDER_LIGHT_COLOR);

        Font font = minecraft.font;
        String text = buildText(qi, qiMax);
        int textX = (guiGraphics.guiWidth() - font.width(text)) / 2;
        int textY = y - TEXT_Y_OFFSET;
        guiGraphics.drawString(font, text, textX, textY, TEXT_COLOR, true);
    }

    private static int highlightColor() {
        int red = Math.min(255, Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.x * 255.0F) + 45);
        int green = Math.min(255, Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.y * 255.0F) + 35);
        int blue = Math.min(255, Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.z * 255.0F));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int colorFromQiParticle() {
        int red = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.x * 255.0F);
        int green = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.y * 255.0F);
        int blue = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.z * 255.0F);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
