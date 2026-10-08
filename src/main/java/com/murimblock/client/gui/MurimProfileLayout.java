package com.murimblock.client.gui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Integer coordinates: reflow content instead of shrinking a textured menu. */
final class MurimProfileLayout {
    static final int MAX_WIDTH = 480;
    static final int MAX_HEIGHT = 300;
    static final int MARGIN = 8;

    enum Page {
        PROFILE("profile"), TECHNIQUES("techniques"), CULTIVATION("cultivation"), INFOS("infos");
        final String id;
        Page(String id) { this.id = id; }
        boolean showsPlayer() { return this == PROFILE; }
    }
    enum Field {
        NAME, REALM_LABEL, REALM, STAGE_LABEL, STAGE, QI_LABEL, QI_VALUE, STATUS,
        HEADER, LIBRARY_LABEL, EMPTY_LIBRARY, DETAILS_LABEL, EMPTY_DETAILS,
        NEXT_LABEL, NEXT_REALM, NEXT_STAGE, REQUIRED_QI, BREAKTHROUGH_LABEL, BREAKTHROUGH,
        CONTROLS_LABEL, CONTROL_PROFILE, CONTROL_QI, CONTROL_COMBAT,
        KEY_PROFILE, KEY_QI, KEY_COMBAT, INFO_LABEL
    }
    record Box(int x, int y, int width, int height) {
        Box {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("GUI areas must have positive dimensions");
        }
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(int px, int py) { return px >= x && px < right() && py >= y && py < bottom(); }
        boolean contains(Box other) {
            return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
        }
        boolean intersects(Box other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    final int width;
    final int height;
    final Box title;
    final Box close;
    final Box content;
    final Box player;
    final Box profileQiFill;
    final Box cultivationQiFill;
    final Box infoSettings;
    final Box settingsHeader;
    final Box settingsStatus;
    final Box keybinds;
    final List<Box> settingRows;
    private final Map<Page, Box> tabs = new EnumMap<>(Page.class);
    private final Map<Page, List<Box>> panels = new EnumMap<>(Page.class);
    private final Map<Page, Map<Field, Box>> text = new EnumMap<>(Page.class);

    static MurimProfileLayout forViewport(int width, int height) {
        return new MurimProfileLayout(Math.min(MAX_WIDTH, width - MARGIN * 2), Math.min(MAX_HEIGHT, height - MARGIN * 2));
    }

    MurimProfileLayout(int width, int height) {
        if (width < 280 || height < 200) throw new IllegalArgumentException("Viewport is smaller than Minecraft's supported GUI");
        this.width = width;
        this.height = height;
        title = new Box(36, 8, width - 72, 18);
        close = new Box(width - 27, 8, 18, 18);
        int tabY = height - 31;
        content = new Box(12, 39, width - 24, tabY - 45);
        for (Page page : Page.values()) {
            int x = 8 + page.ordinal() * (width - 16) / 4;
            int end = 8 + (page.ordinal() + 1) * (width - 16) / 4;
            tabs.put(page, new Box(x, tabY, end - x, 23));
            text.put(page, new EnumMap<>(Field.class));
        }
        int profileSplit = content.x() + content.width() * 37 / 100;
        player = new Box(content.x(), content.y(), profileSplit - content.x() - 8, content.height());
        Box profile = new Box(profileSplit + 4, content.y(), content.right() - profileSplit - 4, content.height());
        panels.put(Page.PROFILE, List.of(player, profile));
        int row = Math.max(17, content.height() / 7);
        put(Page.PROFILE, Field.NAME, profile.x(), profile.y() + 5, profile.width(), 18);
        put(Page.PROFILE, Field.REALM_LABEL, profile.x(), profile.y() + row + 10, 36, 10);
        put(Page.PROFILE, Field.REALM, profile.x() + 40, profile.y() + row + 10, profile.width() - 40, 20);
        put(Page.PROFILE, Field.STAGE_LABEL, profile.x(), profile.y() + row * 3, 36, 10);
        put(Page.PROFILE, Field.STAGE, profile.x() + 40, profile.y() + row * 3, profile.width() - 40, 10);
        put(Page.PROFILE, Field.QI_LABEL, profile.x(), profile.y() + row * 4 + 5, profile.width(), 10);
        profileQiFill = new Box(profile.x() + 2, profile.y() + row * 5, profile.width() - 4, 6);
        put(Page.PROFILE, Field.QI_VALUE, profile.x(), profileQiFill.bottom() + 6, profile.width(), 10);

        int libraryWidth = content.width() * 35 / 100;
        Box library = new Box(content.x(), content.y(), libraryWidth - 8, content.height());
        Box details = new Box(content.x() + libraryWidth + 8, content.y(), content.width() - libraryWidth - 8, content.height());
        panels.put(Page.TECHNIQUES, List.of(library, details));
        put(Page.TECHNIQUES, Field.HEADER, library.x(), content.y(), content.width(), 18);
        put(Page.TECHNIQUES, Field.LIBRARY_LABEL, library.x(), content.y() + 28, library.width(), 10);
        put(Page.TECHNIQUES, Field.DETAILS_LABEL, details.x(), content.y() + 28, details.width(), 10);
        put(Page.TECHNIQUES, Field.EMPTY_LIBRARY, library.x(), content.y() + content.height() / 2, library.width(), 30);
        put(Page.TECHNIQUES, Field.EMPTY_DETAILS, details.x(), content.y() + content.height() / 2, details.width(), 30);

        Box progress = new Box(content.x(), content.y(), content.width() / 2 - 10, content.height());
        Box reserve = new Box(content.x() + content.width() / 2 + 10, content.y(), content.width() - content.width() / 2 - 10, content.height());
        panels.put(Page.CULTIVATION, List.of(progress, reserve));
        int step = Math.max(16, (content.height() - 20) / 8);
        put(Page.CULTIVATION, Field.REALM_LABEL, progress.x(), progress.y(), progress.width(), 10);
        put(Page.CULTIVATION, Field.REALM, progress.x(), progress.y() + step, progress.width(), 18);
        put(Page.CULTIVATION, Field.STAGE, progress.x(), progress.y() + step * 2 + 3, progress.width(), 10);
        put(Page.CULTIVATION, Field.NEXT_LABEL, progress.x(), progress.y() + step * 4, progress.width(), 10);
        put(Page.CULTIVATION, Field.NEXT_REALM, progress.x(), progress.y() + step * 5, progress.width(), 18);
        put(Page.CULTIVATION, Field.NEXT_STAGE, progress.x(), progress.y() + step * 6 + 3, progress.width(), 10);
        put(Page.CULTIVATION, Field.REQUIRED_QI, progress.x(), progress.bottom() - 20, progress.width(), 20);
        put(Page.CULTIVATION, Field.QI_LABEL, reserve.x(), reserve.y() + 16, reserve.width(), 10);
        cultivationQiFill = new Box(reserve.x() + 2, reserve.y() + 40, reserve.width() - 4, 6);
        put(Page.CULTIVATION, Field.QI_VALUE, reserve.x(), reserve.y() + 55, reserve.width(), 10);
        put(Page.CULTIVATION, Field.BREAKTHROUGH_LABEL, reserve.x(), reserve.y() + content.height() / 2 + 10, reserve.width(), 10);
        put(Page.CULTIVATION, Field.BREAKTHROUGH, reserve.x(), reserve.y() + content.height() / 2 + 28, reserve.width(), 30);

        int controlsWidth = content.width() * 54 / 100;
        Box controls = new Box(content.x(), content.y(), controlsWidth - 10, content.height());
        Box status = new Box(content.x() + controlsWidth + 8, content.y(), content.width() - controlsWidth - 8, content.height());
        panels.put(Page.INFOS, List.of(controls, status));
        put(Page.INFOS, Field.CONTROLS_LABEL, controls.x(), controls.y(), controls.width(), 18);
        put(Page.INFOS, Field.INFO_LABEL, status.x(), status.y(), status.width(), 18);
        for (int i = 0; i < 3; i++) {
            int y = controls.y() + 38 + i * Math.max(24, (content.height() - 70) / 3);
            put(Page.INFOS, new Field[]{Field.CONTROL_PROFILE, Field.CONTROL_QI, Field.CONTROL_COMBAT}[i],
                    controls.x(), y + 3, controls.width() - 40, 18);
            put(Page.INFOS, new Field[]{Field.KEY_PROFILE, Field.KEY_QI, Field.KEY_COMBAT}[i], controls.right() - 31, y, 29, 22);
        }
        infoSettings = new Box(controls.x(), controls.bottom() - 24, controls.width(), 22);
        put(Page.INFOS, Field.STATUS, status.x(), status.y() + 38, status.width(), 20);
        put(Page.INFOS, Field.QI_VALUE, status.x(), status.y() + 70, status.width(), 20);
        int settingWidth = content.width() * 63 / 100;
        settingsHeader = new Box(content.x(), content.y(), settingWidth - 10, 18);
        settingsStatus = new Box(content.x() + settingWidth + 8, content.y() + 38, content.width() - settingWidth - 8, 20);
        keybinds = new Box(settingsStatus.x(), content.bottom() - 24, settingsStatus.width(), 22);
        int settingStep = (content.height() - 28) / 6;
        settingRows = java.util.stream.IntStream.range(0, 6).mapToObj(i ->
                new Box(content.x(), content.y() + 28 + i * settingStep, settingWidth - 10, settingStep - 2)).toList();
    }
    Box tab(Page page) { return tabs.get(page); }
    Box tabLabel(Page page) {
        Box box = tab(page);
        return new Box(box.x() + 5, box.y() + 7, box.width() - 10, 10);
    }
    Box text(Page page, Field field) {
        Box box = text.get(page).get(field);
        if (box == null) throw new IllegalArgumentException("No " + field + " area on " + page);
        return box;
    }
    Map<Field, Box> fields(Page page) { return Map.copyOf(text.get(page)); }
    List<Box> panels(Page page) { return panels.get(page); }
    private void put(Page page, Field field, int x, int y, int width, int height) {
        text.get(page).put(field, new Box(x, y, width, height));
    }
}
