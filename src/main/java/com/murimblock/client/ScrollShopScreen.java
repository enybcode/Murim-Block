package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import com.murimblock.network.ScrollShopPayloads;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Golden Dragon's scroll of wares (mob/GoldenDragonShop), after the mock-up Enzo approved on 17/10/2026: a hanging
 * scroll that unrolls from its top roller, red brocade round aged paper with mountains in mist; the title, then a line
 * per ware (its icon, its name, its difficulty in ink drops, its price in gold yuanbao, a round red seal to buy it),
 * the purse at the foot. The mouse wheel scrolls the wares; a click on a seal buys (the server checks the price).
 * Painted by scripts/animation/ScrollShopArt.java at twice the GUI's density; laid out here in GUI pixels.
 */
public final class ScrollShopScreen extends Screen {
    private static final Style TEXT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));
    private static final ResourceLocation SCROLL = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/sage_scroll.png");
    private static final ResourceLocation PARTS = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/sage_scroll_parts.png");
    private static final int SCROLL_W = 592, SCROLL_H = 502, PARTS_W = 644, PARTS_H = 128;
    /** The mounting, the paper inside it (GUI pixels), the rollers' overhang. */
    private static final int MW = 296, MH = 251, PX = 13, PY = 12, PW = 270, PH = 227, ROLLER_W = 322, ROLLER_H = 13;
    /** The wares' lines: where they start on the paper, their height, how many show at once. */
    private static final int ROWS_Y = 96, ROW = 19, VISIBLE = 6;
    private static final int INK = 0xFF24140A, TITLE = 0xFF2A1608, MUTED = 0xFF8A6A3A, PRICE = 0xFF5A3408, PURSE = 0xFF4A2A10;
    /** How long the scroll takes to unroll, in ticks. */
    private static final float UNROLL = 12;

    private final ScrollShopPayloads.Open shop;
    private int ticks;
    private int first;
    private int hovered = -1;
    private boolean hoveredSeal;

    private ScrollShopScreen(ScrollShopPayloads.Open shop) {
        super(Component.translatable("gui.murimblock.sage_shop.title"));
        this.shop = shop;
    }

    /** The scroll from the server: unrolled at once (in place of the conversation). */
    public static void open(ScrollShopPayloads.Open shop) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.setScreen(new ScrollShopScreen(shop));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.0F));
        });
    }

    /** The scroll on screen, if open (for the tours). */
    public static ScrollShopPayloads.Open current() {
        return Minecraft.getInstance().screen instanceof ScrollShopScreen screen ? screen.shop : null;
    }

    private static Component styled(Component text) {
        return text.copy().withStyle(TEXT);
    }

    @Override public boolean isPauseScreen() {
        return false;
    }

    @Override public void tick() {
        ticks++;
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0x60000000, 0xA0000000);
    }

    // ---------------------------------------------------------------- layout

    /** The scale that fits the whole scroll (rollers included) on the screen. */
    private float scale() {
        return Math.min(1.0F, Math.min((height - 8) / (float) (MH + 2 * ROLLER_H), (width - 8) / (float) ROLLER_W));
    }

    private float left() {
        return (width - MW * scale()) / 2;
    }

    private float top() {
        return (height - MH * scale()) / 2;
    }

    private int purse() {
        return minecraft == null || minecraft.player == null ? 0 : minecraft.player.getInventory().countItem(MurimItems.GOLD_YUANBAO.get());
    }

    private List<ScrollShopPayloads.Ware> wares() {
        return shop.wares();
    }

    private int maxFirst() {
        return Math.max(0, wares().size() - VISIBLE);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        float s = scale();
        float mx = (mouseX - left()) / s, my = (mouseY - top()) / s;
        float unrolled = Mth.clamp((ticks + partial) / UNROLL, 0, 1);
        unrolled = 1 - (1 - unrolled) * (1 - unrolled) * (1 - unrolled);
        int shown = Math.max(1, Math.round(MH * unrolled));

        var pose = g.pose();
        pose.pushPose();
        pose.translate(left(), top(), 0);
        pose.scale(s, s, 1);
        // The mounting, as far as it has unrolled.
        g.blit(SCROLL, 0, 0, MW, shown, 0, 0, SCROLL_W, shown * 2, SCROLL_W, SCROLL_H);
        hovered = -1;
        hoveredSeal = false;
        if (unrolled >= 1) contents(g, mx, my);
        // The tassels hang from the top roller; the rollers last, over the paper's ends.
        int rx = (MW - ROLLER_W) / 2;
        g.blit(PARTS, rx + 6, 2, 10, 25, 0, 30, 20, 50, PARTS_W, PARTS_H);
        g.blit(PARTS, rx + ROLLER_W - 16, 2, 10, 25, 0, 30, 20, 50, PARTS_W, PARTS_H);
        g.blit(PARTS, rx, -11, ROLLER_W, ROLLER_H, 0, 0, 644, 26, PARTS_W, PARTS_H);
        g.blit(PARTS, rx, shown - 2, ROLLER_W, ROLLER_H, 0, 0, 644, 26, PARTS_W, PARTS_H);
        pose.popPose();

        // The hovered ware's tooltip, at the mouse (outside the scaled scroll).
        if (hovered >= 0 && !hoveredSeal) g.renderTooltip(font, wares().get(hovered).icon(), mouseX, mouseY);
    }

    /** The title, the wares, the purse: once the scroll is open. */
    private void contents(GuiGraphics g, float mx, float my) {
        int x0 = PX, y0 = PY, cx = PX + PW / 2;
        var pose = g.pose();
        // The title, larger, and the line under it.
        Component title = styled(getTitle());
        pose.pushPose();
        pose.translate(cx, y0 + 9, 0);
        pose.scale(1.5F, 1.5F, 1);
        g.drawString(font, title, -font.width(title) / 2, 0, TITLE, false);
        pose.popPose();
        Component motto = styled(Component.translatable("gui.murimblock.sage_shop.motto"));
        g.drawString(font, motto, cx - font.width(motto) / 2, y0 + 26, MUTED, false);

        int purse = purse();
        first = Mth.clamp(first, 0, maxFirst());
        for (int line = 0; line < VISIBLE && first + line < wares().size(); line++) {
            int index = first + line;
            ScrollShopPayloads.Ware ware = wares().get(index);
            int ry = y0 + ROWS_Y + line * ROW;
            boolean over = mx >= x0 + 4 && mx < x0 + PW - 4 && my >= ry && my < ry + ROW;
            boolean onSeal = over && mx >= x0 + 238 && mx < x0 + 258;
            if (over) {
                hovered = index;
                hoveredSeal = onSeal;
                g.blit(PARTS, cx - 125, ry - 1, 250, 20, 0, 88, 500, 40, PARTS_W, PARTS_H);
            }
            g.blit(PARTS, x0 + 13, ry + 16, 244, 3, 0, 84, 488, 6, PARTS_W, PARTS_H);
            // The icon (faded when not for sale) and the name, shrunk to fit its column.
            g.renderItem(ware.icon(), x0 + 10, ry);
            if (!ware.forSale()) g.fill(x0 + 10, ry, x0 + 26, ry + 16, 0xA8EADAB0);
            Component name = styled(ware.name());
            int room = 134, w = font.width(name);
            pose.pushPose();
            pose.translate(x0 + 32, ry + 5, 0);
            if (w > room) pose.scale(room / (float) w, room / (float) w, 1);
            g.drawString(font, name, 0, 0, ware.forSale() ? INK : MUTED, false);
            pose.popPose();
            // The difficulty, in ink drops.
            if (ware.difficulty() > 0 && ware.forSale()) for (int k = 0; k < 4; k++)
                g.blit(PARTS, x0 + 168 + k * 7, ry + 4, 7, 7, k < ware.difficulty() ? 30 : 50, 30, 14, 14, PARTS_W, PARTS_H);
            if (!ware.forSale()) {
                Component later = styled(Component.translatable("gui.murimblock.sage_shop.not_yet"));
                g.drawString(font, later, x0 + PW - 10 - font.width(later), ry + 5, MUTED, false);
                continue;
            }
            // The price, and the seal to buy: bright under the mouse, grey when the purse is too light.
            g.blit(PARTS, x0 + 202, ry + 4, 12, 9, 70, 30, 24, 18, PARTS_W, PARTS_H);
            g.drawString(font, styled(Component.literal(String.valueOf(ware.price()))), x0 + 216, ry + 5, PRICE, false);
            int seal = purse < ware.price() && !creative() ? 180 : onSeal ? 140 : 100;
            g.blit(PARTS, x0 + 240, ry, 16, 16, seal, 30, 32, 32, PARTS_W, PARTS_H);
        }
        // The purse, and a word when more wares wait below.
        int fy = y0 + PH - 14;
        g.blit(PARTS, x0 + 12, fy - 1, 12, 9, 70, 30, 24, 18, PARTS_W, PARTS_H);
        g.drawString(font, styled(Component.translatable("gui.murimblock.sage_shop.purse", purse)), x0 + 28, fy, PURSE, false);
        if (maxFirst() > 0) {
            Component more = styled(Component.translatable(first < maxFirst() ? "gui.murimblock.sage_shop.more" : "gui.murimblock.sage_shop.top"));
            g.drawString(font, more, x0 + PW - 12 - font.width(more), fy, MUTED, false);
        }
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    // ---------------------------------------------------------------- buying

    private void buy(int index) {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        PacketDistributor.sendToServer(new ScrollShopPayloads.Buy(shop.sellerId(), index));
    }

    /** For the tours: buys as a click on the seal would. */
    public void buyForTests(int index) {
        buy(index);
    }

    /** For the tours: the scroll unrolled at once. */
    public void unrollForTests() {
        ticks = (int) UNROLL;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0 && hoveredSeal && wares().get(hovered).forSale()) {
            buy(hovered);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int before = first;
        first = Mth.clamp(first - (int) Math.signum(scrollY), 0, maxFirst());
        if (first != before && minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.3F));
        return true;
    }
}
