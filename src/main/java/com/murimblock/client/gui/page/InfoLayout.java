package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;

/** The Info page: a sub-tab row, then Quests (list, detail), the log, or Guide (controls, topics, topic). */
public record InfoLayout(Rect subTabs, Rect body) {
    public static final int SUB_TAB_HEIGHT = 16;
    public static final int BODY_TOP = 22;
    public static final int GAP = 12;

    public static final int QUEST_LIST_WIDTH = 150;
    public static final int CONTROLS_WIDTH = 150;
    public static final int TOPICS_WIDTH = 100;

    public static InfoLayout of(Rect area) {
        return new InfoLayout(new Rect(area.x(), area.y(), area.width(), SUB_TAB_HEIGHT), area.inset(0, BODY_TOP, 0, 0));
    }

    public Rect[] questColumns() {
        Rect list = new Rect(body.x(), body.y(), QUEST_LIST_WIDTH, body.height());
        int dx = list.right() + GAP;
        return new Rect[] {list, new Rect(dx, body.y(), Math.max(0, body.right() - dx), body.height())};
    }

    public Rect[] guideColumns() {
        Rect controls = new Rect(body.x(), body.y(), CONTROLS_WIDTH, body.height());
        Rect topics = new Rect(controls.right() + GAP, body.y(), TOPICS_WIDTH, body.height());
        int dx = topics.right() + GAP;
        return new Rect[] {controls, topics, new Rect(dx, body.y(), Math.max(0, body.right() - dx), body.height())};
    }
}
