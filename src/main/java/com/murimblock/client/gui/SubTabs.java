package com.murimblock.client.gui;

import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** A row of tabs inside a page, left aligned, the chosen one underlined. */
public final class SubTabs {
    public static final int GAP = 4;
    public static final int PADDING = 10;

    private SubTabs() {
    }

    public static void add(SystemScreen screen, SystemLayout.Rect row, List<Component> labels, int selected, IntConsumer onSelect) {
        int x = row.x();
        for (int i = 0; i < labels.size(); i++) {
            int index = i;
            Component label = labels.get(i);
            int width = SystemTheme.spacedWidth(Minecraft.getInstance().font, label.getString(), 1) + 2 * PADDING;
            screen.addPageWidget(new SystemButton(x, row.y(), width, row.height(), label, SystemButton.Style.SUB_TAB,
                    () -> onSelect.accept(index)).selected(i == selected));
            x += width + GAP;
        }
    }
}
