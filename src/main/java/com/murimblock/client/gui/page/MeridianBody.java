package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.CultivationData;
import com.murimblock.client.gui.data.CultivationData.Acupoint;
import com.murimblock.client.gui.data.CultivationData.Meridian;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The cubic body of the Meridians view: a Minecraft player seen from the front, every acupoint as a square, and
 * the chosen meridian's points joined by a vein in pixel steps, with Qi flowing along it once open.
 * Everything is placed in screen pixels from the camera, so squares stay sharp at any zoom.
 */
final class MeridianBody {
    /** Body parts in body units: x, y, width, height. */
    private static final int[][] PARTS = {
            {40, 0, 80, 80}, {40, 80, 80, 120}, {0, 80, 40, 120}, {120, 80, 40, 120}, {40, 200, 40, 120}, {80, 200, 40, 120}};
    /** Darker pixels: hair, eyes and belt. */
    private static final int[][] DETAILS = {{40, 0, 80, 20}, {55, 40, 10, 5}, {95, 40, 10, 5}, {40, 185, 80, 15}};
    private static final int SKIN = 0xFF0C1F33;
    private static final int EDGE = 0xFF2D77A3;
    private static final int DARK = 0xFF16324D;
    private static final int GRID = 0x1463CFFF;

    private MeridianBody() {
    }

    static void render(GuiGraphics g, Font font, Rect vp, MeridianCamera.View cam, int selected, long millis) {
        g.enableScissor(vp.x() + 1, vp.y() + 1, vp.right() - 1, vp.bottom() - 1);
        for (int[] p : PARTS) {
            int x1 = sx(vp, cam, p[0]);
            int y1 = sy(vp, cam, p[1]);
            int x2 = sx(vp, cam, p[0] + p[2]);
            int y2 = sy(vp, cam, p[1] + p[3]);
            g.fill(x1, y1, x2, y2, SKIN);
            grid(g, vp, cam, p);
            g.renderOutline(x1, y1, x2 - x1, y2 - y1, EDGE);
        }
        for (int[] d : DETAILS) {
            g.fill(sx(vp, cam, d[0]), sy(vp, cam, d[1]), sx(vp, cam, d[0] + d[2]), sy(vp, cam, d[1] + d[3]), DARK);
        }

        float pulse = 0.65f + 0.35f * (float) Math.sin(millis / 400.0);
        int dantian = SystemTheme.withAlpha(SystemTheme.CYAN, Math.round(255 * pulse));
        int dx = CultivationData.DANTIAN_X;
        int dy = CultivationData.DANTIAN_Y;
        g.fill(sx(vp, cam, dx - 5), sy(vp, cam, dy - 5), sx(vp, cam, dx + 5), sy(vp, cam, dy + 5), dantian);

        List<Meridian> meridians = CultivationData.MERIDIANS;
        // Every other meridian's vein, faint: the body reads as a web of channels.
        for (int i = 0; i < meridians.size(); i++) {
            if (i != selected) {
                int faint = CultivationData.isOpen(i) ? SystemTheme.withAlpha(SystemTheme.CYAN, 0x38) : SystemTheme.withAlpha(SystemTheme.CYAN, 0x16);
                path(g, vp, cam, meridians.get(i).points(), faint, false, 0);
            }
        }
        for (int i = 0; i < meridians.size(); i++) {
            if (i == selected) {
                continue;
            }
            int color = CultivationData.isOpen(i) ? SystemTheme.withAlpha(SystemTheme.CYAN, 0x80) : SystemTheme.withAlpha(SystemTheme.CYAN, 0x30);
            for (Acupoint a : meridians.get(i).points()) {
                square(g, sx(vp, cam, a.x()), sy(vp, cam, a.y()), 3, color, 0);
            }
        }

        Meridian chosen = meridians.get(selected);
        boolean open = CultivationData.isOpen(selected);
        vein(g, vp, cam, chosen.points(), open, millis);
        for (Acupoint a : chosen.points()) {
            int x = sx(vp, cam, a.x());
            int y = sy(vp, cam, a.y());
            if (open) {
                square(g, x, y, 7, SystemTheme.withAlpha(SystemTheme.CYAN, 0x40), 0);
            }
            square(g, x, y, 5, open ? SystemTheme.CYAN : SKIN, open ? SystemTheme.TEXT : 0xFF9FD8F5);
            boolean left = a.x() > 140;
            int tx = left ? x - 6 - font.width(a.code()) : x + 6;
            g.drawString(font, a.code(), tx, y - 4, SystemTheme.TEXT, true);
        }
        g.disableScissor();
    }

    /** One faint line per skin pixel, only when the zoom leaves room for it. */
    private static void grid(GuiGraphics g, Rect vp, MeridianCamera.View cam, int[] p) {
        if (cam.scale() * 10 < 5) {
            return;
        }
        int y1 = sy(vp, cam, p[1]);
        int y2 = sy(vp, cam, p[1] + p[3]);
        for (int u = p[0] + 10; u < p[0] + p[2]; u += 10) {
            int x = sx(vp, cam, u);
            g.fill(x, y1, x + 1, y2, GRID);
        }
        int x1 = sx(vp, cam, p[0]);
        int x2 = sx(vp, cam, p[0] + p[2]);
        for (int u = p[1] + 10; u < p[1] + p[3]; u += 10) {
            int y = sy(vp, cam, u);
            g.fill(x1, y, x2, y + 1, GRID);
        }
    }

    /** From each point to the next: down (or up) first, then across, like pixels. */
    private static void vein(GuiGraphics g, Rect vp, MeridianCamera.View cam, List<Acupoint> points, boolean open, long millis) {
        int base = open ? SystemTheme.withAlpha(SystemTheme.CYAN, 0x99) : 0x599FD8F5;
        path(g, vp, cam, points, base, open, (int) (millis / 60 % 8));
    }

    private static void path(GuiGraphics g, Rect vp, MeridianCamera.View cam, List<Acupoint> points, int base, boolean open, int flow) {
        int walked = 0;
        for (int i = 1; i < points.size(); i++) {
            int x0 = sx(vp, cam, points.get(i - 1).x());
            int y0 = sy(vp, cam, points.get(i - 1).y());
            int x1 = sx(vp, cam, points.get(i).x());
            int y1 = sy(vp, cam, points.get(i).y());
            walked = line(g, x0, y0, x0, y1, base, open, flow, walked);
            walked = line(g, x0, y1, x1, y1, base, open, flow, walked);
        }
    }

    /** A straight one-pixel line, with bright dots travelling along it when Qi flows. */
    private static int line(GuiGraphics g, int x0, int y0, int x1, int y1, int base, boolean flowing, int flow, int walked) {
        int length = Math.abs(x1 - x0) + Math.abs(y1 - y0);
        int stepX = Integer.signum(x1 - x0);
        int stepY = Integer.signum(y1 - y0);
        g.fill(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1, base);
        if (flowing) {
            for (int d = 0; d <= length; d++) {
                if (Math.floorMod(walked + d - flow, 8) < 2) {
                    int x = x0 + stepX * d;
                    int y = y0 + stepY * d;
                    g.fill(x, y, x + 1, y + 1, SystemTheme.TEXT);
                }
            }
        }
        return walked + length;
    }

    private static void square(GuiGraphics g, int cx, int cy, int size, int fill, int edge) {
        int x = cx - size / 2;
        int y = cy - size / 2;
        g.fill(x, y, x + size, y + size, fill);
        if (edge != 0) {
            g.renderOutline(x, y, size, size, edge);
        }
    }

    private static int sx(Rect vp, MeridianCamera.View cam, float unitX) {
        return Math.round(vp.x() + vp.width() / 2f + (unitX - cam.x()) * cam.scale());
    }

    private static int sy(Rect vp, MeridianCamera.View cam, float unitY) {
        return Math.round(vp.y() + vp.height() / 2f + (unitY - cam.y()) * cam.scale());
    }
}
