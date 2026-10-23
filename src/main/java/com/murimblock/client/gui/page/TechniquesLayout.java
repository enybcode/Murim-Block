package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;

/** The three columns of the Techniques page: arts, techniques of the active art, chosen technique. */
public record TechniquesLayout(Rect arts, Rect techniques, Rect detail) {
    public static final int ARTS_WIDTH = 168;
    public static final int TECHNIQUES_WIDTH = 140;
    public static final int GAP = 12;

    public static TechniquesLayout of(Rect area) {
        Rect arts = new Rect(area.x(), area.y(), ARTS_WIDTH, area.height());
        Rect techniques = new Rect(arts.right() + GAP, area.y(), TECHNIQUES_WIDTH, area.height());
        int detailX = techniques.right() + GAP;
        Rect detail = new Rect(detailX, area.y(), Math.max(0, area.right() - detailX), area.height());
        return new TechniquesLayout(arts, techniques, detail);
    }
}
