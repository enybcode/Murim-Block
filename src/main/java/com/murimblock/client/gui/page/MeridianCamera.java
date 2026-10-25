package com.murimblock.client.gui.page;

/**
 * Where the meridian view looks: a point of the body (in body units) shown at the centre of the viewport, and a
 * zoom. It frames the chosen meridian's acupoints and glides there instead of jumping.
 */
public final class MeridianCamera {
    public static final int PADDING = 40;
    public static final int MIN_SPAN = 120;
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2f;
    /** Share of the remaining distance covered per second. */
    private static final float SPEED = 6f;

    private float x;
    private float y;
    private float scale;
    private boolean placed;

    public record View(float x, float y, float scale) {
    }

    /** The view that fits the box [minX, maxX] x [minY, maxY] of body units into a viewport. */
    public static View fit(int minX, int minY, int maxX, int maxY, int viewportWidth, int viewportHeight) {
        float w = Math.max(maxX - minX + 2 * PADDING, MIN_SPAN);
        float h = Math.max(maxY - minY + 2 * PADDING, MIN_SPAN);
        float scale = Math.min(viewportWidth / w, viewportHeight / h);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        return new View((minX + maxX) / 2f, (minY + maxY) / 2f, scale);
    }

    /** Moves toward the target; the first call places the camera there at once. */
    public View follow(View target, float seconds) {
        if (!placed) {
            x = target.x();
            y = target.y();
            scale = target.scale();
            placed = true;
        } else {
            float k = Math.min(1f, Math.max(0f, seconds) * SPEED);
            x += (target.x() - x) * k;
            y += (target.y() - y) * k;
            scale += (target.scale() - scale) * k;
        }
        return new View(x, y, scale);
    }
}
