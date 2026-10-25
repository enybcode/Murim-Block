package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;

/** The three columns of the Profile page: player model, identity, Qi panel. */
public record ProfileLayout(Rect model, Rect identity, Rect qi) {
    public static final int MODEL_WIDTH = 120;
    public static final int QI_WIDTH = 150;
    public static final int GAP = 14;

    public static ProfileLayout of(Rect area) {
        Rect model = new Rect(area.x(), area.y(), MODEL_WIDTH, area.height());
        Rect qi = new Rect(area.right() - QI_WIDTH, area.y(), QI_WIDTH, area.height());
        int identityX = model.right() + GAP;
        Rect identity = new Rect(identityX, area.y(), Math.max(0, qi.x() - GAP - identityX), area.height());
        return new ProfileLayout(model, identity, qi);
    }
}
