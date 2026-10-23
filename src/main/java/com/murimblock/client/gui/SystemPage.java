package com.murimblock.client.gui;

import net.minecraft.network.chat.Component;

/** The four pages of the System, in tab order. */
public enum SystemPage {
    PROFILE("profile"),
    TECHNIQUES("techniques"),
    CULTIVATION("cultivation"),
    INFO("info");

    private final String id;

    SystemPage(String id) {
        this.id = id;
    }

    public Component tab() {
        return Component.translatable("gui.murimblock.tab." + id);
    }

    /** The word on the title tag of the window, such as STATUS for the profile. */
    public Component title() {
        return Component.translatable("gui.murimblock.title." + id);
    }
}
