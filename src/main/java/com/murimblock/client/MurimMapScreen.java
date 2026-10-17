package com.murimblock.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.network.MurimMapPayloads;
import com.murimblock.world.map.MurimMap;
import com.murimblock.world.map.MurimMapView;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Murim map (docs/biomes/murim-map.md): the bubble seen from above, its coast and its ocean, each region in its
 * colour with its name in the middle of its land, and the player as a red seal. The server draws the regions from the world's layout
 * (MurimMapView) and sends them when the map is opened (the M key, or the button on the Info page); the seed never
 * leaves the server. North (-z) is up.
 */
public class MurimMapScreen extends Screen {
    private static final Style MANUSCRIPT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "murim_map");
    private static final int FRAME = 0xFF1A1714, PAPER = 0xFF2B2721, LINE = 0xFF4A443B, INK = 0xFFEDE3CC, MUTED = 0xFFB3A68A,
            GOLD = 0xFFC9A45C, GOLD_DARK = 0xFF7A5E2C, SEAL = 0xFFC24A36;
    /** Each cell value's colour (ABGR is made from these RGB): the five regions, the ocean, the vanilla world. */
    static final int[] COLOURS = {0xE3A6A9, 0xE6ECF0, 0xD9B56C, 0x6FA45A, 0x5C9CC9, 0x1D2D4B, 0x2B3029};
    private static final String[] NAMES = {"center", "north", "west", "south", "east"};

    /** The map last sent by the server: null until it comes; empty cells when the world has no Murim lands. */
    @Nullable private static byte[] cells;
    @Nullable private static int[] hearts;
    private static boolean uploaded;
    /** Where each region's name goes: the middle of its land on the map (cells), worked out with the texture. */
    private static final double[][] LABELS = new double[5][];

    public MurimMapScreen() {
        super(Component.translatable("gui.murimblock.map.title"));
    }

    /** Opens the map and asks the server for it (again: the world may have changed). */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        cells = null;
        hearts = null;
        mc.setScreen(new MurimMapScreen());
        PacketDistributor.sendToServer(new MurimMapPayloads.Request());
    }

    /** From the server: the regions. Turned into a small texture, a pixel a cell. */
    public static void receive(MurimMapPayloads.View view) {
        cells = view.cells();
        hearts = view.hearts();
        uploaded = false;
    }

    private static void upload() {
        if (uploaded || cells == null || cells.length != MurimMapView.SIZE * MurimMapView.SIZE) return;
        int n = MurimMapView.SIZE;
        NativeImage img = new NativeImage(n, n, false);
        for (int row = 0; row < n; row++) for (int col = 0; col < n; col++) {
            int v = cells[row * n + col];
            int rgb = COLOURS[Math.clamp(v, 0, COLOURS.length - 1)];
            boolean land = v <= 4;
            // The coast inked dark; a border between two regions a little darker; the ocean's waves every few cells.
            boolean coast = false, border = false;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int r = row + d[0], c = col + d[1];
                if (r < 0 || c < 0 || r >= n || c >= n) continue;
                int w = cells[r * n + c];
                if (land && w > 4) coast = true;
                else if (land && w != v && (d[0] > 0 || d[1] > 0)) border = true;
            }
            if (coast) rgb = 0x1A1714;
            else if (border) rgb = shade(rgb, 0.62);
            else if (v == MurimMapView.OCEAN && (row * 7 + col * 3) % 23 == 0) rgb = 0x2C4470;
            img.setPixelRGBA(col, row, 0xFF000000 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
        }
        // Each region's name in the middle of its land (the centre of its cells).
        for (int r = 0; r < LABELS.length; r++) {
            double sumCol = 0, sumRow = 0;
            int count = 0;
            for (int i = 0; i < cells.length; i++) if (cells[i] == r) { sumCol += i % n; sumRow += i / n; count++; }
            LABELS[r] = count == 0 ? null : new double[]{(sumCol / count + 0.5) / n, (sumRow / count + 0.5) / n};
        }
        Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(img));
        uploaded = true;
    }

    static int shade(int rgb, double k) {
        int r = (int) ((rgb >> 16 & 0xFF) * k), g = (int) ((rgb >> 8 & 0xFF) * k), b = (int) ((rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MurimblockKeyMappings.MURIM_MAP.matches(keyCode, scanCode) || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Component text(Component c) {
        return Component.literal(c.getString()).withStyle(MANUSCRIPT);
    }

    private void rect(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + h, colour);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int colour) {
        rect(g, x, y, w, 1, colour);
        rect(g, x, y + h - 1, w, 1, colour);
        rect(g, x, y, 1, h, colour);
        rect(g, x + w - 1, y, 1, h, colour);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null) { onClose(); return; }
        upload();
        // The panel: the map square on the left, the legend on the right.
        int legendW = Math.min(118, Math.max(84, width / 5));
        int map = Math.max(80, Math.min(height - 44, width - legendW - 34));
        int panelW = map + legendW + 22, panelH = map + 30;
        int x0 = (width - panelW) / 2, y0 = (height - panelH) / 2;
        rect(g, x0 - 2, y0 - 2, panelW + 4, panelH + 4, FRAME);
        rect(g, x0, y0, panelW, panelH, PAPER);
        outline(g, x0, y0, panelW, panelH, GOLD_DARK);
        Component title = text(Component.literal(getTitle().getString().toUpperCase(java.util.Locale.ROOT)));
        g.drawString(font, title, x0 + (panelW - font.width(title)) / 2, y0 + 6, GOLD, false);
        rect(g, x0 + 8, y0 + 10, (panelW - font.width(title)) / 2 - 14, 1, GOLD_DARK);
        rect(g, x0 + (panelW + font.width(title)) / 2 + 6, y0 + 10, (panelW - font.width(title)) / 2 - 14, 1, GOLD_DARK);
        int mx = x0 + 8, my = y0 + 20;
        rect(g, mx - 1, my - 1, map + 2, map + 2, GOLD_DARK);

        Component hovered = null;
        if (cells == null) {
            rect(g, mx, my, map, map, 0xFF1D2D4B);
            Component wait = text(Component.translatable("gui.murimblock.map.loading"));
            g.drawString(font, wait, mx + (map - font.width(wait)) / 2, my + map / 2 - 4, MUTED, false);
        } else if (cells.length == 0) {
            rect(g, mx, my, map, map, 0xFF2B3029);
            Component none = text(Component.translatable("gui.murimblock.map.none"));
            g.drawWordWrap(font, none, mx + 8, my + map / 2 - 10, map - 16, MUTED);
        } else {
            g.blit(TEXTURE, mx, my, map, map, 0, 0, MurimMapView.SIZE, MurimMapView.SIZE, MurimMapView.SIZE, MurimMapView.SIZE);
            // North, up.
            Component north = text(Component.translatable("gui.murimblock.map.north"));
            rect(g, mx + map / 2 - font.width(north) / 2 - 2, my + 2, font.width(north) + 4, 10, 0xB0000000);
            g.drawString(font, north, mx + map / 2 - font.width(north) / 2, my + 3, INK, false);
            // Each region's name at its heart.
            // Short, and half size where the screen's pixels allow it, so the five names stay apart; one that would
            // cover another moves down.
            float scale = minecraft.getWindow().getGuiScale() >= 3 ? 0.5F : 1.0F;
            java.util.List<int[]> placed = new java.util.ArrayList<>();
            for (int r = 0; r < NAMES.length; r++) {
                if (LABELS[r] == null) continue;
                Component name = text(Component.translatable("region.murimblock." + NAMES[r] + ".short"));
                int w = Math.round(font.width(name) * scale), h = Math.round(11 * scale);
                int px = Mth.clamp(mx + (int) (LABELS[r][0] * map) - w / 2, mx + 2, mx + map - w - 2);
                int py = my + (int) (LABELS[r][1] * map) - h / 2;
                for (boolean moved = true; moved; ) {
                    moved = false;
                    for (int[] o : placed) if (px < o[0] + o[2] + 2 && o[0] < px + w + 2 && py < o[1] + o[3] && o[1] < py + h) { py = o[1] + o[3] + 1; moved = true; }
                }
                placed.add(new int[]{px, py, w, h});
                rect(g, px - 2, py - 1, w + 4, h + 1, 0xA0000000);
                g.pose().pushPose();
                g.pose().translate(px, py + scale, 0);
                g.pose().scale(scale, scale, 1);
                g.drawString(font, name, 0, 1, INK, false);
                g.pose().popPose();
            }
            // The player: a red seal, blinking, and a tick where they look.
            double px = minecraft.player.getX(), pz = minecraft.player.getZ();
            int sx = Mth.clamp(toScreen(px, mx, map), mx + 2, mx + map - 3), sy = Mth.clamp(toScreen(pz, my, map), my + 2, my + map - 3);
            float yaw = minecraft.player.getYRot() * Mth.DEG_TO_RAD;
            int tx = sx + Math.round(-Mth.sin(yaw) * 6), ty = sy + Math.round(Mth.cos(yaw) * 6);
            for (int i = 1; i <= 5; i++) rect(g, sx + (tx - sx) * i / 5, sy + (ty - sy) * i / 5, 1, 1, INK);
            boolean on = (minecraft.level.getGameTime() / 8) % 2 == 0;
            rect(g, sx - 2, sy - 2, 5, 5, FRAME);
            rect(g, sx - 1, sy - 1, 3, 3, on ? SEAL : 0xFFE8735E);
            if (mouseX >= mx && mouseY >= my && mouseX < mx + map && mouseY < my + map) {
                double bx = toBlocks(mouseX - mx, map), bz = toBlocks(mouseY - my, map);
                hovered = Component.translatable("gui.murimblock.map.at", where(bx, bz), Math.round(bx / 100) * 100, Math.round(bz / 100) * 100);
            }
        }

        // The legend: the five regions, then where the player is.
        int lx = mx + map + 10, ly = my;
        g.drawString(font, text(Component.translatable("gui.murimblock.map.regions")), lx, ly, MUTED, false);
        rect(g, lx, ly + 10, 18, 1, GOLD);
        ly += 16;
        for (int r = 0; r < NAMES.length; r++) {
            rect(g, lx, ly, 9, 9, FRAME);
            rect(g, lx + 1, ly + 1, 7, 7, 0xFF000000 | COLOURS[r]);
            g.drawWordWrap(font, text(Component.translatable("region.murimblock." + NAMES[r])), lx + 13, ly + 1, legendW - 14, INK);
            ly += font.wordWrapHeight(text(Component.translatable("region.murimblock." + NAMES[r])), legendW - 14) + 5;
        }
        ly += 4;
        rect(g, lx, ly, legendW - 4, 1, LINE);
        ly += 6;
        g.drawString(font, text(Component.translatable("gui.murimblock.map.you")), lx, ly, MUTED, false);
        ly += 11;
        g.drawWordWrap(font, text(where(minecraft.player.getX(), minecraft.player.getZ())), lx, ly, legendW - 4, GOLD);
        ly += font.wordWrapHeight(text(where(minecraft.player.getX(), minecraft.player.getZ())), legendW - 4) + 2;
        g.drawString(font, text(Component.literal((int) minecraft.player.getX() + ", " + (int) minecraft.player.getZ())), lx, ly, MUTED, false);

        if (hovered != null) g.renderTooltip(font, hovered, mouseX, mouseY);
    }

    /** What lies at this block: its region, the ocean, or the world beyond (from the grid). */
    private static Component where(double x, double z) {
        if (cells == null || cells.length == 0) return Component.translatable("gui.murimblock.map.unknown");
        int n = MurimMapView.SIZE;
        int col = (int) Math.floor((x + MurimMapView.REACH) / MurimMapView.CELL), row = (int) Math.floor((z + MurimMapView.REACH) / MurimMapView.CELL);
        if (col < 0 || row < 0 || col >= n || row >= n) return Component.translatable("gui.murimblock.map.beyond");
        int v = cells[row * n + col];
        if (v <= 4) return Component.translatable("region.murimblock." + NAMES[v]);
        return Component.translatable(v == MurimMapView.OCEAN ? "gui.murimblock.map.ocean" : "gui.murimblock.map.beyond");
    }

    private static int toScreen(double block, int origin, int size) {
        return origin + (int) Math.floor((block + MurimMapView.REACH) / (2.0 * MurimMapView.REACH) * size);
    }

    private static double toBlocks(int pixel, int size) {
        return (pixel + 0.5) / size * 2.0 * MurimMapView.REACH - MurimMapView.REACH;
    }

    /** For tests: the map as the client holds it. */
    public static boolean ready() {
        return cells != null;
    }

    static {
        // Region names follow the regions' order; a new region would need its name here.
        if (NAMES.length != MurimMap.Region.values().length) throw new IllegalStateException("MurimMapScreen: region names");
    }
}
