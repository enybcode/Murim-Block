package com.murimblock.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/** A martial guide read (item/MartialGuideItem): its pages open in the book screen. */
public final class GuideBookClient {
    private GuideBookClient() {
    }

    public static void open(List<Component> pages) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }
}
