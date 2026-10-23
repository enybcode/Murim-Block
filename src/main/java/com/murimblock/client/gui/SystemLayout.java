package com.murimblock.client.gui;

/**
 * Pure layout math of the System window, kept free of Minecraft classes so it can be unit tested.
 * Sizes are in GUI pixels; the window follows the validated mockups (1060 x 620 at half scale).
 */
public final class SystemLayout {
    public static final int MAX_WIDTH = 530;
    public static final int MAX_HEIGHT = 310;
    /** Room kept around the window, and above it for the title tag that sits on the top border. */
    public static final int MARGIN = 8;
    public static final int TITLE_ROOM = 8;

    public static final int TAB_HEIGHT = 20;
    public static final int TAB_PADDING = 10;
    public static final int TAB_GAP = 5;
    public static final int TAB_TOP = 16;

    public static final int CONTENT_SIDE = 16;
    public static final int CONTENT_BOTTOM = 12;
    public static final int CONTENT_GAP = 10;

    private SystemLayout() {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public int centerX() {
            return x + width / 2;
        }

        public Rect inset(int left, int top, int right, int bottom) {
            return new Rect(x + left, y + top, Math.max(0, width - left - right), Math.max(0, height - top - bottom));
        }
    }

    /** Share of the real screen the window takes when there is room, across and down. */
    public static final float SCREEN_SHARE_X = 0.7f;
    public static final float SCREEN_SHARE_Y = 0.75f;
    /** Share it may take on a small screen, to stay readable. */
    public static final float SMALL_SCREEN_SHARE = 0.95f;

    /**
     * Real pixels per design pixel. The System ignores Minecraft's GUI scale and is drawn whole, always at a whole
     * factor when one fits, because the pixel font turns uneven (blurry) at fractional ones: about 70 % of the
     * screen when that allows x2 or more (x2 at 1080p, x4 at 4K); on smaller screens the largest whole factor that
     * still fits (x2 at 720p, x1 at 480p); only a screen too small for x1 gets a fraction.
     */
    public static float displayFactor(int screenPixelsX, int screenPixelsY) {
        float comfortable = fit(screenPixelsX, screenPixelsY, SCREEN_SHARE_X, SCREEN_SHARE_Y);
        if (comfortable >= 2) {
            return (float) Math.floor(comfortable);
        }
        float most = fit(screenPixelsX, screenPixelsY, SMALL_SCREEN_SHARE, SMALL_SCREEN_SHARE);
        if (most >= 1) {
            return (float) Math.floor(most);
        }
        return Math.max(0.1f, most);
    }

    private static float fit(int screenPixelsX, int screenPixelsY, float shareX, float shareY) {
        float fitX = screenPixelsX * shareX / (MAX_WIDTH + 2 * MARGIN);
        float fitY = screenPixelsY * shareY / (MAX_HEIGHT + 2 * MARGIN + TITLE_ROOM);
        return Math.min(fitX, fitY);
    }

    /** The window, centred, shrunk to fit small screens or large GUI scales. */
    public static Rect window(int screenWidth, int screenHeight) {
        int width = Math.max(0, Math.min(MAX_WIDTH, screenWidth - 2 * MARGIN));
        int height = Math.max(0, Math.min(MAX_HEIGHT, screenHeight - 2 * MARGIN - TITLE_ROOM));
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height + TITLE_ROOM) / 2;
        return new Rect(x, y, width, height);
    }

    /** Left edge of each tab of a centred row, from the width of each label. */
    public static int[] tabRow(int centerX, int[] labelWidths) {
        int[] xs = new int[labelWidths.length];
        int total = 0;
        for (int i = 0; i < labelWidths.length; i++) {
            total += tabWidth(labelWidths[i]) + (i > 0 ? TAB_GAP : 0);
        }
        int x = centerX - total / 2;
        for (int i = 0; i < labelWidths.length; i++) {
            xs[i] = x;
            x += tabWidth(labelWidths[i]) + TAB_GAP;
        }
        return xs;
    }

    public static int tabWidth(int labelWidth) {
        return labelWidth + 2 * TAB_PADDING;
    }

    /** Where a page draws: below the main tabs, inside the border. */
    public static Rect content(Rect window) {
        int top = TAB_TOP + TAB_HEIGHT + CONTENT_GAP;
        return window.inset(CONTENT_SIDE, top, CONTENT_SIDE, CONTENT_BOTTOM);
    }
}
