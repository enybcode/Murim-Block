package com.murimblock.client.gui;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

final class MurimProfileLayout {
    static final Box TITLE = new Box(122, 9, 76, 9);
    static final Box CONTENT = new Box(8, 27, 304, 137);
    static final Box PLAYER = new Box(17, 34, 106, 126);
    static final Box PROFILE_QI_FILL = new Box(239, 97, 59, 5);
    static final Box CULTIVATION_QI_FRAME = new Box(205, 53, 94, 10);

    enum Page {
        PROFILE("profile", new Box(5, 171, 74, 31), new Box(12, 176, 18, 23)),
        TECHNIQUES("techniques", new Box(81, 171, 76, 31), new Box(90, 177, 17, 21)),
        CULTIVATION("cultivation", new Box(160, 171, 75, 31), new Box(169, 177, 19, 20)),
        INFOS("infos", new Box(240, 171, 74, 31), new Box(249, 176, 13, 23));

        final String id;
        final Box tab;
        final Box icon;

        Page(String id, Box tab, Box icon) {
            this.id = id;
            this.tab = tab;
            this.icon = icon;
        }

        boolean showsPlayer() {
            return this == PROFILE;
        }

        Box text(Field field) {
            Box box = TEXT.get(this).get(field);
            if (box == null) {
                throw new IllegalArgumentException("No " + field + " area on " + this);
            }
            return box;
        }

        Box tabLabel() {
            return new Box(tab.x() + 4, 190, tab.width() - 8, 9);
        }

        Box tabIcon() {
            int width = Math.round(icon.width() * 11.0F / icon.height());
            int x = Math.round(tab.x() + tab.width() / 2.0F - width / 2.0F);
            return new Box(x, 175, width, 11);
        }
    }

    enum Field {
        NAME, REALM_LABEL, REALM, STAGE_LABEL, STAGE, QI_LABEL, QI_VALUE, COMBAT, STATUS,
        HEADER, LIBRARY_LABEL, EMPTY_LIBRARY, DETAILS_LABEL, EMPTY_DETAILS,
        NEXT_LABEL, NEXT_REALM, NEXT_STAGE, REQUIRED_QI, BREAKTHROUGH_LABEL, BREAKTHROUGH,
        CONTROLS_LABEL, CONTROL_PROFILE, CONTROL_QI, CONTROL_COMBAT,
        KEY_PROFILE, KEY_QI, KEY_COMBAT, INFO_LABEL
    }

    record Box(int x, int y, int width, int height) {
        Box {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("GUI areas must have positive dimensions");
            }
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

    static final Map<Page, List<Box>> PANELS = Map.of(
            Page.PROFILE, List.of(new Box(131, 28, 176, 48), new Box(131, 79, 90, 35),
                    new Box(226, 79, 81, 35), new Box(131, 117, 176, 45)),
            Page.TECHNIQUES, List.of(new Box(9, 28, 302, 26), new Box(9, 59, 112, 103), new Box(126, 59, 185, 103)),
            Page.CULTIVATION, List.of(new Box(9, 28, 179, 134), new Box(194, 28, 117, 65), new Box(194, 98, 117, 64)),
            Page.INFOS, List.of(new Box(9, 28, 176, 134), new Box(191, 28, 120, 134)));

    static final Map<Page, Map<Field, Box>> TEXT = Map.of(
            Page.PROFILE, Map.ofEntries(
                    entry(Field.NAME, new Box(140, 34, 120, 10)),
                    entry(Field.REALM, new Box(140, 46, 122, 19)),
                    entry(Field.STAGE_LABEL, new Box(165, 84, 49, 9)),
                    entry(Field.STAGE, new Box(165, 98, 49, 10)),
                    entry(Field.QI_LABEL, new Box(233, 84, 72, 9)),
                    entry(Field.QI_VALUE, new Box(234, 105, 70, 9)),
                    entry(Field.COMBAT, new Box(139, 125, 132, 10)),
                    entry(Field.STATUS, new Box(139, 141, 132, 12))),
            Page.TECHNIQUES, Map.ofEntries(
                    entry(Field.HEADER, new Box(17, 37, 286, 10)),
                    entry(Field.LIBRARY_LABEL, new Box(17, 68, 96, 10)),
                    entry(Field.EMPTY_LIBRARY, new Box(17, 94, 96, 36)),
                    entry(Field.DETAILS_LABEL, new Box(134, 68, 169, 10)),
                    entry(Field.EMPTY_DETAILS, new Box(134, 119, 169, 9))),
            Page.CULTIVATION, Map.ofEntries(
                    entry(Field.REALM_LABEL, new Box(17, 37, 163, 9)),
                    entry(Field.REALM, new Box(17, 50, 163, 10)),
                    entry(Field.STAGE, new Box(17, 63, 163, 10)),
                    entry(Field.NEXT_LABEL, new Box(17, 88, 163, 9)),
                    entry(Field.NEXT_REALM, new Box(17, 101, 163, 11)),
                    entry(Field.NEXT_STAGE, new Box(17, 114, 163, 18)),
                    entry(Field.REQUIRED_QI, new Box(17, 143, 163, 10)),
                    entry(Field.QI_LABEL, new Box(202, 38, 101, 10)),
                    entry(Field.QI_VALUE, new Box(202, 73, 101, 10)),
                    entry(Field.BREAKTHROUGH_LABEL, new Box(202, 108, 101, 10)),
                    entry(Field.BREAKTHROUGH, new Box(202, 124, 101, 28))),
            Page.INFOS, Map.ofEntries(
                    entry(Field.CONTROLS_LABEL, new Box(17, 37, 160, 10)),
                    entry(Field.CONTROL_PROFILE, new Box(17, 69, 126, 10)),
                    entry(Field.CONTROL_QI, new Box(17, 96, 126, 10)),
                    entry(Field.CONTROL_COMBAT, new Box(17, 123, 126, 10)),
                    entry(Field.KEY_PROFILE, new Box(154, 69, 17, 9)),
                    entry(Field.KEY_QI, new Box(154, 96, 17, 9)),
                    entry(Field.KEY_COMBAT, new Box(154, 123, 17, 9)),
                    entry(Field.INFO_LABEL, new Box(199, 37, 104, 10)),
                    entry(Field.STATUS, new Box(199, 69, 104, 20)),
                    entry(Field.COMBAT, new Box(199, 95, 104, 24)),
                    entry(Field.QI_VALUE, new Box(199, 135, 104, 10))));

    static List<Box> decorations(Page page) {
        return switch (page) {
            case PROFILE -> List.of(PLAYER, new Box(266, 32, 37, 38), new Box(136, 85, 26, 27),
                    PROFILE_QI_FILL, new Box(274, 143, 30, 11));
            case TECHNIQUES -> List.of(new Box(209, 91, 18, 22));
            case CULTIVATION -> List.of(CULTIVATION_QI_FRAME);
            case INFOS -> List.of();
        };
    }

    private MurimProfileLayout() {
    }
}
