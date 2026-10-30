package com.murimblock.client.family;

import com.murimblock.client.gui.PixelArt;
import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.family.Family;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * The Destiny window (board {@code docs/gui/boards/families.png}): a seal spins through the houses and lands on the one
 * the server drew; its name, rarity, seat and inheritance show; ACCEPT keeps it, REROLL spends the free reroll (that
 * second draw is final). Escape closes it without choosing: it opens again at the next login.
 */
public final class DestinyScreen extends Screen {
    private static final int WIDTH = 260, HEIGHT = 210;
    /** Ticks the seal spins before landing. */
    private static final int SPIN = 34;

    private Rect panel;
    private int age;
    private int seenDraws;
    private SystemButton accept;
    private SystemButton reroll;

    public DestinyScreen() {
        super(Component.translatable("family.murimblock.destiny"));
    }

    @Override
    protected void init() {
        panel = new Rect((width - WIDTH) / 2, (height - HEIGHT) / 2, WIDTH, HEIGHT);
        seenDraws = ClientFamily.draws();
        int y = panel.bottom() - 26;
        accept = addRenderableWidget(new SystemButton(panel.x() + 30, y, 90, 15, Component.translatable("family.murimblock.accept"),
                SystemButton.Style.ACTION, () -> {
                    if (ClientFamily.freeReroll()) {
                        ClientFamily.choose(false);
                    }
                    onClose();
                }));
        reroll = addRenderableWidget(new SystemButton(panel.right() - 120, y, 90, 15, rerollLabel(),
                SystemButton.Style.ACTION, () -> {
                    if (ClientFamily.freeReroll()) {
                        ClientFamily.choose(true);
                    }
                }));
    }

    private static Component rerollLabel() {
        return ClientFamily.freeReroll() ? Component.translatable("family.murimblock.reroll", 1)
                : Component.translatable("family.murimblock.final");
    }

    @Override
    public void tick() {
        if (ClientFamily.draws() != seenDraws) {
            // A new draw came in (the reroll): spin again.
            seenDraws = ClientFamily.draws();
            age = 0;
        }
        age++;
        boolean landed = age >= SPIN;
        accept.active = landed;
        reroll.active = landed && ClientFamily.freeReroll();
        reroll.setMessage(rerollLabel());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // The blur first, then the window and its buttons on top (Screen.render would blur them too).
        renderBackground(g, mouseX, mouseY, partialTick);
        SystemTheme.window(g, panel);
        SystemTheme.titleTag(g, font, title, panel.centerX(), panel.y());
        Component sub = Component.translatable("family.murimblock.destiny.sub");
        SystemTheme.spaced(g, font, sub.getString(), panel.centerX() - SystemTheme.spacedWidth(font, sub.getString(), 1) / 2,
                panel.y() + 14, SystemTheme.TEXT_MUTED, 1);
        Family drawn = ClientFamily.family();
        if (drawn == null) {
            widgets(g, mouseX, mouseY, partialTick);
            return;
        }
        int cx = panel.centerX(), cy = panel.y() + 62;
        if (age < SPIN) {
            // The seal spins through the houses, slowing down.
            Family[] all = Family.values();
            int step = (int) (Math.pow(age / (float) SPIN, 2) * 14) + age;
            Family shown = all[Math.floorMod(step * 7 + drawn.ordinal(), all.length)];
            crest(g, cx - 54, cy, 15, all[Math.floorMod(shown.ordinal() - 1, all.length)], false);
            crest(g, cx + 54, cy, 15, all[Math.floorMod(shown.ordinal() + 1, all.length)], false);
            crest(g, cx, cy, 25, shown, true);
            widgets(g, mouseX, mouseY, partialTick);
            return;
        }
        crest(g, cx, cy, 27, drawn, true);
        if (drawn.rarity == Family.Rarity.LEGENDARY) {
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8 + age * 0.02;
                for (int d = 31; d < (k % 2 == 0 ? 44 : 38); d++) {
                    PixelArt.px(g, cx + (int) Math.round(Math.cos(a) * d), cy + (int) Math.round(Math.sin(a) * d),
                            k % 2 == 0 ? drawn.rarity.line : 0x80FFD36B);
                }
            }
        }
        int y = cy + 34;
        Component name = Component.translatable(drawn.nameKey());
        g.pose().pushPose();
        g.pose().translate(cx - font.width(name), y, 0);
        g.pose().scale(2, 2, 1);
        g.drawString(font, name, 0, 0, SystemTheme.TEXT, true);
        g.pose().popPose();
        y += 20;
        String rarity = Component.translatable(drawn.rarity.labelKey()).getString().toUpperCase(java.util.Locale.ROOT);
        int tw = SystemTheme.spacedWidth(font, rarity, 1) + 12;
        PixelArt.frame(g, cx - tw / 2, y, tw, 13, 1, drawn.rarity.line, 0xFF0D2238, 0);
        SystemTheme.spaced(g, font, rarity, cx - tw / 2 + 6, y + 3, drawn.rarity.text, 1);
        y += 18;
        Rect box = new Rect(panel.x() + 20, y, panel.width() - 40, 38);
        PixelArt.frame(g, box.x(), box.y(), box.width(), box.height(), 2, drawn.rarity.line, 0xE60D2238, 0);
        MutableComponent inheritance = Component.empty();
        for (Component line : ClientFamily.inheritance(drawn)) {
            if (!inheritance.getSiblings().isEmpty()) {
                inheritance.append(" · ");
            }
            inheritance.append(line);
        }
        g.drawString(font, Component.translatable("family.murimblock.inheritance_label"), box.x() + 7, box.y() + 5,
                drawn.rarity.text, false);
        List<FormattedCharSequence> lines = font.split(inheritance, box.width() - 14);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.drawString(font, lines.get(i), box.x() + 7, box.y() + 16 + i * 10, 0xFFCFEEFF, false);
        }
        widgets(g, mouseX, mouseY, partialTick);
    }

    private void widgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        for (net.minecraft.client.gui.components.Renderable r : renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
    }

    /** A house's crest: a diamond seal in its rarity's colours with the surname's characters. */
    private void crest(GuiGraphics g, int cx, int cy, int r, Family house, boolean bright) {
        PixelArt.diamond(g, cx, cy, r, PixelArt.OUTLINE);
        PixelArt.diamond(g, cx, cy, r - 1, bright ? house.rarity.line : PixelArt.SLATE);
        PixelArt.diamond(g, cx, cy, r - 2, PixelArt.OUTLINE);
        PixelArt.diamond(g, cx, cy, r - 3, bright ? 0xFF14243A : 0xFF0D1824);
        String chars = house.characters;
        float scale = Mth.clamp((r * 1.1f) / Math.max(1, font.width(chars)), 1, 3);
        g.pose().pushPose();
        g.pose().translate(cx - font.width(chars) * scale / 2f, cy - 4 * scale, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, chars, 0, 0, bright ? house.rarity.text : 0xFF6B8AA3, false);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
