package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;

/** The Cultivation page: a sub-tab row, then either the Realm or the Meridians columns. */
public record CultivationLayout(Rect subTabs, Rect body) {
    public static final int SUB_TAB_HEIGHT = 16;
    public static final int BODY_TOP = 22;

    public static final int LADDER_WIDTH = 150;
    public static final int TRIAL_WIDTH = 150;
    public static final int REALM_GAP = 12;

    public static final int LIST_WIDTH = 122;
    public static final int DETAIL_WIDTH = 150;
    public static final int MERIDIAN_GAP = 10;

    public static CultivationLayout of(Rect area) {
        return new CultivationLayout(new Rect(area.x(), area.y(), area.width(), SUB_TAB_HEIGHT),
                area.inset(0, BODY_TOP, 0, 0));
    }

    /** Ladder of realms, dantian, breakthrough trial. */
    public Rect[] realmColumns() {
        return columns(LADDER_WIDTH, TRIAL_WIDTH, REALM_GAP);
    }

    /** Meridian list, body viewport, chosen meridian. */
    public Rect[] meridianColumns() {
        return columns(LIST_WIDTH, DETAIL_WIDTH, MERIDIAN_GAP);
    }

    private Rect[] columns(int left, int right, int gap) {
        Rect a = new Rect(body.x(), body.y(), left, body.height());
        Rect c = new Rect(body.right() - right, body.y(), right, body.height());
        int bx = a.right() + gap;
        Rect b = new Rect(bx, body.y(), Math.max(0, c.x() - gap - bx), body.height());
        return new Rect[] {a, b, c};
    }
}
