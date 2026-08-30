package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.qi.QiFormat;
import com.murimblock.qi.QiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiDebugOverlay {
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 16;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private QiDebugOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        String text = buildText(QiService.getQi(player), QiService.getQiMax(player));
        Font font = minecraft.font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = getRightAlignedX(guiGraphics.guiWidth(), font.width(text));
        int y = getBottomAlignedY(guiGraphics.guiHeight(), font.lineHeight);

        guiGraphics.drawString(font, text, x, y, TEXT_COLOR, true);
    }

    static String buildText(double qi, double qiMax) {
        return "Qi : " + QiFormat.format(qi) + " / " + QiFormat.format(qiMax);
    }

    static int getRightAlignedX(int screenWidth, int textWidth) {
        return Math.max(0, screenWidth - textWidth - RIGHT_MARGIN);
    }

    static int getBottomAlignedY(int screenHeight, int lineHeight) {
        return Math.max(0, screenHeight - lineHeight - BOTTOM_MARGIN);
    }
}
