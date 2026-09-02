package com.murimblock.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.qi.QiFormat;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
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
    static final int VANILLA_PROGRESS_SCALE = 183;
    static final int QI_COLOR = colorFromQiParticle();
    private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace(
            "hud/experience_bar_background"
    );
    private static final ResourceLocation QI_BAR_PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath(
            Murimblock.MOD_ID,
            "hud/qi_bar_progress"
    );
    private static final int TEXT_COLOR = QI_COLOR;

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
        return (int) (computeQiRatio(qi, qiMax) * VANILLA_PROGRESS_SCALE);
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

        if (player.getXpNeededForNextLevel() <= 0) {
            return;
        }

        double qi = QiService.getQi(player);
        double qiMax = QiService.getQiMax(player);
        int x = guiGraphics.guiWidth() / 2 - 91;
        int y = guiGraphics.guiHeight() - BAR_Y_OFFSET;
        int filledWidth = computeFilledWidth(qi, qiMax);

        RenderSystem.enableBlend();
        guiGraphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_SPRITE, x, y, BAR_WIDTH, BAR_HEIGHT);
        if (filledWidth > 0) {
            guiGraphics.blitSprite(QI_BAR_PROGRESS_SPRITE, BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y, filledWidth, BAR_HEIGHT);
        }
        RenderSystem.disableBlend();

        Font font = minecraft.font;
        String text = buildText(qi, qiMax);
        int textX = (guiGraphics.guiWidth() - font.width(text)) / 2;
        int textY = y - TEXT_Y_OFFSET;
        guiGraphics.drawString(font, text, textX, textY, TEXT_COLOR, true);
    }

    private static int colorFromQiParticle() {
        int red = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.x * 255.0F);
        int green = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.y * 255.0F);
        int blue = Math.round(QiChargeVisuals.QI_PARTICLE_COLOR.z * 255.0F);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
