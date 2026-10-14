package com.murimblock.client.gui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Compact manuscript menu: integer coordinates, reflowed rather than shrunk.
 * Thin title band, bookmark tabs under it, then a paper page holding two cards per tab.
 */
final class MurimProfileLayout {
    static final int MAX_WIDTH = 320;
    static final int MAX_HEIGHT = 200;
    static final int MARGIN = 8;
    /** Top of the paper page; the active bookmark tab joins it. */
    static final int PAGE_TOP = 40;

    enum Page {
        PROFILE("profile"), TECHNIQUES("techniques"), CULTIVATION("cultivation"), INFOS("infos");
        final String id;
        Page(String id) { this.id = id; }
        boolean showsPlayer() { return this == PROFILE; }
    }
    enum Field {
        NAME, REALM_LABEL, REALM, STAGE_LABEL, STAGE, PASSIVE_LABEL, PASSIVE, QI_LABEL, QI_VALUE, STATUS,
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
    final Box page;
    final Box content;
    final Box player;
    final Box profileQiFill;
    final Box cultivationQiFill;
    final Box stagePips;
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
        if (width < 280 || height < 190) throw new IllegalArgumentException("Viewport is smaller than Minecraft's supported GUI");
        this.width = width;
        this.height = height;
        title = new Box(40, 7, width - 80, 10);
        close = new Box(width - 18, 5, 13, 13);
        for (Page target : Page.values()) {
            int x = 8 + target.ordinal() * (width - 16) / 4;
            int end = 8 + (target.ordinal() + 1) * (width - 16) / 4;
            tabs.put(target, new Box(x + 1, 26, end - x - 2, 14));
            text.put(target, new EnumMap<>(Field.class));
        }
        page = new Box(4, PAGE_TOP, width - 8, height - PAGE_TOP - 4);
        content = new Box(10, PAGE_TOP + 6, width - 20, height - PAGE_TOP - 14);
        int cx = content.x(), cy = content.y(), cw = content.width(), ch = content.height();

        // Profile: framed portrait, then name, realm, stage and Qi.
        player = new Box(cx, cy, cw * 36 / 100, ch);
        Box profile = new Box(player.right() + 8, cy, content.right() - player.right() - 8, ch);
        panels.put(Page.PROFILE, List.of(player, profile));
        int px = profile.x(), pw = profile.width();
        put(Page.PROFILE, Field.NAME, px, cy + 2, pw, 18);
        put(Page.PROFILE, Field.REALM_LABEL, px, cy + 26, pw, 9);
        put(Page.PROFILE, Field.REALM, px, cy + 37, pw, 18);
        put(Page.PROFILE, Field.STAGE_LABEL, px, cy + 58, 40, 9);
        put(Page.PROFILE, Field.STAGE, px + 44, cy + 58, pw - 44, 9);
        // The one passive in use: the main style's.
        put(Page.PROFILE, Field.PASSIVE_LABEL, px, cy + 72, pw, 9);
        put(Page.PROFILE, Field.PASSIVE, px, cy + 84, pw, 9);
        put(Page.PROFILE, Field.QI_LABEL, px, cy + 98, pw, 9);
        profileQiFill = new Box(px + 1, cy + 111, pw - 2, 6);
        put(Page.PROFILE, Field.QI_VALUE, px, cy + 121, pw, 9);

        // Techniques: library list and detail card under a header.
        int libraryWidth = cw * 40 / 100;
        Box library = new Box(cx, cy + 22, libraryWidth - 4, ch - 22);
        Box details = new Box(cx + libraryWidth + 4, cy + 22, cw - libraryWidth - 4, ch - 22);
        panels.put(Page.TECHNIQUES, List.of(library, details));
        put(Page.TECHNIQUES, Field.HEADER, cx, cy, cw, 18);
        put(Page.TECHNIQUES, Field.LIBRARY_LABEL, library.x() + 6, library.y() + 6, library.width() - 12, 9);
        put(Page.TECHNIQUES, Field.DETAILS_LABEL, details.x() + 6, details.y() + 6, details.width() - 12, 9);
        put(Page.TECHNIQUES, Field.EMPTY_LIBRARY, library.x() + 6, library.y() + library.height() / 2 - 4, library.width() - 12, 20);
        put(Page.TECHNIQUES, Field.EMPTY_DETAILS, details.x() + 6, details.y() + details.height() / 2 - 4, details.width() - 12, 20);

        // Cultivation: progression card and Qi reserve / breakthrough card.
        Box progress = new Box(cx, cy, cw / 2 - 4, ch);
        Box reserve = new Box(cx + cw / 2 + 4, cy, cw - cw / 2 - 4, ch);
        panels.put(Page.CULTIVATION, List.of(progress, reserve));
        int gx = progress.x() + 6, gw = progress.width() - 12;
        put(Page.CULTIVATION, Field.REALM_LABEL, gx, cy + 6, gw, 9);
        put(Page.CULTIVATION, Field.REALM, gx, cy + 18, gw, 18);
        put(Page.CULTIVATION, Field.STAGE, gx, cy + 38, gw, 9);
        stagePips = new Box(gx, cy + 50, 4 * 8, 5);
        put(Page.CULTIVATION, Field.NEXT_LABEL, gx, cy + 66, gw, 9);
        put(Page.CULTIVATION, Field.NEXT_REALM, gx, cy + 78, gw, 9);
        put(Page.CULTIVATION, Field.NEXT_STAGE, gx, cy + 89, gw, 9);
        put(Page.CULTIVATION, Field.REQUIRED_QI, gx, cy + 106, gw, 18);
        int rx = reserve.x() + 6, rw = reserve.width() - 12;
        put(Page.CULTIVATION, Field.QI_LABEL, rx, cy + 6, rw, 9);
        cultivationQiFill = new Box(rx + 1, cy + 22, rw - 2, 6);
        put(Page.CULTIVATION, Field.QI_VALUE, rx, cy + 33, rw, 9);
        put(Page.CULTIVATION, Field.BREAKTHROUGH_LABEL, rx, cy + 66, rw, 9);
        put(Page.CULTIVATION, Field.BREAKTHROUGH, rx, cy + 80, rw, 18);

        // Info: controls with key caps, player status, Combat Settings button.
        int controlsWidth = cw * 58 / 100;
        Box controls = new Box(cx, cy, controlsWidth - 4, ch);
        Box status = new Box(cx + controlsWidth + 4, cy, cw - controlsWidth - 4, ch);
        panels.put(Page.INFOS, List.of(controls, status));
        put(Page.INFOS, Field.CONTROLS_LABEL, controls.x() + 6, cy + 6, controls.width() - 12, 9);
        put(Page.INFOS, Field.INFO_LABEL, status.x() + 6, cy + 6, status.width() - 12, 9);
        Field[] controlLabels = {Field.CONTROL_PROFILE, Field.CONTROL_QI, Field.CONTROL_COMBAT};
        Field[] keys = {Field.KEY_PROFILE, Field.KEY_QI, Field.KEY_COMBAT};
        for (int i = 0; i < 3; i++) {
            int y = cy + 24 + i * 22;
            put(Page.INFOS, controlLabels[i], controls.x() + 6, y + 5, controls.width() - 52, 9);
            put(Page.INFOS, keys[i], controls.right() - 42, y, 36, 17);
        }
        infoSettings = new Box(controls.x() + 6, controls.bottom() - 22, controls.width() - 12, 16);
        put(Page.INFOS, Field.STATUS, status.x() + 6, cy + 26, status.width() - 12, 9);
        put(Page.INFOS, Field.QI_VALUE, status.x() + 6, cy + 42, status.width() - 12, 9);

        // Combat Settings (inside Info): seven rows and a Keybinds button.
        int settingWidth = cw * 62 / 100;
        settingsHeader = new Box(cx, cy, settingWidth - 6, 14);
        // Status column text and Keybinds sit 6 px inside the card drawn around them.
        settingsStatus = new Box(cx + settingWidth + 12, cy + 22, cw - settingWidth - 18, 9);
        keybinds = new Box(settingsStatus.x(), content.bottom() - 22, settingsStatus.width(), 16);
        int settingStep = (ch - 18) / 7;
        settingRows = IntStream.range(0, 7).mapToObj(i ->
                new Box(cx, cy + 18 + i * settingStep, settingWidth - 6, settingStep - 2)).toList();
    }
    Box tab(Page page) { return tabs.get(page); }
    Box tabLabel(Page page) {
        Box box = tab(page);
        return new Box(box.x() + 2, box.y() + 3, box.width() - 4, 9);
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
