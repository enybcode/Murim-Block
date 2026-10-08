package com.murimblock.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicFightGuiAdapterTest {
    @Test
    void routesOnlyNativeEpicFightScreens() {
        assertTrue(EpicFightGuiAdapter.isNativeScreen("yesman.epicfight.client.gui.screen.SkillEditScreen"));
        assertTrue(EpicFightGuiAdapter.isNativeScreen("yesman.epicfight.client.gui.screen.EmoteWheelScreen"));
        assertTrue(EpicFightGuiAdapter.isNativeScreen("yesman.epicfight.client.gui.datapack.screen.DatapackEditScreen"));
        assertFalse(EpicFightGuiAdapter.isNativeScreen("net.minecraft.client.gui.screens.OptionsScreen"));
        assertFalse(EpicFightGuiAdapter.isNativeScreen("com.otheraddon.SkillScreen"));
        assertFalse(EpicFightGuiAdapter.isNativeScreen("yesman.epicfight.client.gui.widgets.EmoteWheelTab"));
    }
}
