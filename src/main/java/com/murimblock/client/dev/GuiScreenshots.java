package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Dev tool, off unless the {@code murimblock.guiShots} system property is set (run {@code gradlew runGuiShots}):
 * from the title screen, opens each System page at GUI scale 3, saves a screenshot, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GuiScreenshots {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.guiShots");
    /** Frames to wait on a page before the shot, so layout and blur have settled. */
    private static final int SETTLE_FRAMES = 20;
    private static final int GUI_SCALE = 3;

    private static int page = -1;
    private static int frames;

    private GuiScreenshots() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED || page >= SystemPage.values().length) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (page < 0) {
            if (!(mc.screen instanceof TitleScreen)) {
                return;
            }
            mc.options.guiScale().set(GUI_SCALE);
            mc.resizeDisplay();
            open(mc, 0);
            return;
        }
        if (++frames < SETTLE_FRAMES) {
            return;
        }
        SystemPage current = SystemPage.values()[page];
        String name = "murim_" + current.name().toLowerCase(java.util.Locale.ROOT) + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("GUI shot: {}", message.getString()));
        if (page + 1 < SystemPage.values().length) {
            open(mc, page + 1);
        } else {
            // The game draws a few more frames while stopping: no second shot of the last page.
            page = SystemPage.values().length;
            mc.stop();
        }
    }

    private static void open(Minecraft mc, int index) {
        page = index;
        frames = 0;
        mc.setScreen(new SystemScreen(SystemPage.values()[index]));
    }
}
