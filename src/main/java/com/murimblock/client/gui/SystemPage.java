package com.murimblock.client.gui;

import com.murimblock.client.gui.page.CultivationPage;
import com.murimblock.client.gui.page.InfoPage;
import com.murimblock.client.gui.page.ProfilePage;
import com.murimblock.client.gui.page.TechniquesPage;
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

    public SystemPageView createView() {
        return switch (this) {
            case PROFILE -> new ProfilePage();
            case TECHNIQUES -> new TechniquesPage();
            case CULTIVATION -> new CultivationPage(false);
            case INFO -> new InfoPage(InfoPage.QUESTS);
        };
    }

    public Component tab() {
        return Component.translatable("gui.murimblock.tab." + id);
    }

    /** The word on the title tag of the window, such as STATUS for the profile. */
    public Component title() {
        return Component.translatable("gui.murimblock.title." + id);
    }
}
