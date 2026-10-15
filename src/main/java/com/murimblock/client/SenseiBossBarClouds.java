package com.murimblock.client;

import com.murimblock.Murimblock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * Little clouds around Sensei Yun's boss bar: a cloud on each end of the bar, half behind it, and a small one on each
 * side of his name. Pixel art in the Azure Cloud's colours (white, sky-blue shade, azure outline), drawn a pixel of the
 * screen's GUI scale at a time; the bar itself stays Minecraft's own, drawn over them. His bar is known by its name,
 * the sensei's translation key (a renamed sensei keeps it: the bar is named when he spawns).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SenseiBossBarClouds {
    /** The sensei's bar: its name's translation key. */
    static final String NAME_KEY = "entity.murimblock.azure_sensei";
    /** The bar's width in GUI pixels (Minecraft's). */
    private static final int BAR_WIDTH = 182;

    /** A cloud on the bar's end (the left one; the right one mirrored). O outline, W white, L its shaded underside. */
    static final String[] END_CLOUD = {
            ".......OOOO.........",
            ".....OOWWWWO..OOO...",
            "....OWWWWWWWOOWWWO..",
            "..OOWWWWWWWWWWWWWWO.",
            ".OWWWWWWWWWWWWWWWWWO",
            "OWWWWWWWWWWWWWWWWWLO",
            "OWLLWWWWWWWWWWWLLLLO",
            ".OOLLLLLLLLLLLLLLOO.",
            "...OOOOOOOOOOOOOO...",
    };
    /** A small puff beside his name. */
    static final String[] PUFF = {
            "...OOO....",
            "..OWWWOOO.",
            ".OWWWWWWWO",
            "OWWWWWWWLO",
            ".OOLLLLOO.",
            "..OOOOOO..",
    };
    private static final int OUTLINE = 0xFF1F5FAF, WHITE = 0xFFF4F8FF, SHADE = 0xFFA9CDF2;

    private SenseiBossBarClouds() { }

    /** Whether this boss bar is the sensei's. */
    static boolean isSensei(LerpingBossEvent bar) {
        return bar.getName().getContents() instanceof TranslatableContents t && NAME_KEY.equals(t.getKey());
    }

    /** Before Minecraft draws the sensei's bar: the clouds, so the bar's ends lie over them. */
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!isSensei(event.getBossEvent())) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = event.getX(), y = event.getY();
        // The bar is 5 pixels high: the end clouds centred on it, their inner edge 6 pixels under its ends.
        draw(g, END_CLOUD, x - END_CLOUD[0].length() + 6, y - 2, false);
        draw(g, END_CLOUD, x + BAR_WIDTH - 6, y - 2, true);
        // The name sits 9 pixels above the bar, centred: the puffs well clear of it.
        draw(g, PUFF, x + 22, y - 10, false);
        draw(g, PUFF, x + BAR_WIDTH - 22 - PUFF[0].length(), y - 10, true);
    }

    private static void draw(GuiGraphics g, String[] sprite, int x, int y, boolean mirrored) {
        int w = sprite[0].length();
        for (int row = 0; row < sprite.length; row++) {
            for (int col = 0; col < w; col++) {
                int colour = switch (sprite[row].charAt(mirrored ? w - 1 - col : col)) {
                    case 'O' -> OUTLINE;
                    case 'W' -> WHITE;
                    case 'L' -> SHADE;
                    default -> 0;
                };
                if (colour != 0) g.fill(x + col, y + row, x + col + 1, y + row + 1, colour);
            }
        }
    }
}
