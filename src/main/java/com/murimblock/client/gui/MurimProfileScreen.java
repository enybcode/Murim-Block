package com.murimblock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class MurimProfileScreen extends Screen {
    static final int PANEL_WIDTH = 320;
    static final int PANEL_HEIGHT = 214;
    static final int QI_FILL_WIDTH = 59;
    private static final int INK = 0xFF2A1F12;
    private static final int MUTED_INK = 0xFF62533F;
    private static final int GOLD = 0xFFE4C263;
    private static final ResourceLocation QI_FILL = texture("murim_qi_fill_v4");
    private static final Style GUI_TEXT_STYLE = Style.EMPTY.withFont(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));

    private enum Page {
        PROFILE("profile", 5, 74),
        TECHNIQUES("techniques", 81, 76),
        CULTIVATION("cultivation", 160, 75),
        INFOS("infos", 240, 74);

        private final String id;
        private final int x;
        private final int width;
        private final ResourceLocation background;

        Page(String id, int x, int width) {
            this.id = id;
            this.x = x;
            this.width = width;
            this.background = texture("murim_" + id + "_v4");
        }
    }

    private Page page = Page.PROFILE;
    private int left;
    private int top;
    private Component hoveredText;

    public MurimProfileScreen() {
        super(Component.translatable("gui.murimblock.title"));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/" + name + ".png");
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        for (Page target : Page.values()) {
            addRenderableWidget(new ArtButton(left + target.x, top + 171, target.width, 31,
                    Component.translatable("gui.murimblock.tab." + target.id), button -> page = target));
        }
        ArtButton close = new ArtButton(left + 302, top + 8, 14, 14,
                Component.translatable("gui.murimblock.close"), button -> onClose());
        close.setTooltip(Tooltip.create(close.getMessage()));
        addRenderableWidget(close);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MurimblockKeyMappings.OPEN_PROFILE.matches(keyCode, scanCode)
                || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.player == null) {
            onClose();
            return;
        }
        hoveredText = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredText != null) {
            graphics.renderTooltip(font, hoveredText, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        graphics.fill(0, 0, width, height, 0x88000000);
        RenderSystem.enableBlend();
        graphics.blit(page.background, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        double qi = MurimblockApi.qi().getQi(player);
        double qiMax = MurimblockApi.qi().getQiMax(player);
        int filled = computeFilledWidth(qi, qiMax);
        if (filled > 0) {
            graphics.blit(QI_FILL, left + 239, top + 97, 0, 0, filled, 5, QI_FILL_WIDTH, 5);
        }
        RenderSystem.disableBlend();
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                left + 17, top + 34, left + 123, top + 160, 52, 0.0625F, mouseX, mouseY, player);
        renderLabels(graphics, player, qi, qiMax, mouseX, mouseY);
    }

    static int computeFilledWidth(double qi, double qiMax) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0) {
            return 0;
        }
        return (int) Math.round(QI_FILL_WIDTH * Math.clamp(qi / qiMax, 0, 1));
    }

    private void renderLabels(GuiGraphics graphics, Player player, double qi, double qiMax, int mouseX, int mouseY) {
        CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);
        Component realm = Component.translatable("gui.murimblock.realm." + cultivation.realmId());
        Component stage = Component.translatable("gui.murimblock.stage." + cultivation.stageId());
        Component combat = Component.translatable(MurimblockApi.combat().isInCombatMode(player)
                ? "gui.murimblock.combat.on" : "gui.murimblock.combat.off");
        centered(graphics, title, 160, 9, GOLD, 76, mouseX, mouseY);
        fitted(graphics, player.getName(), 140, 35, 161, INK, mouseX, mouseY);
        fitted(graphics, realm, 140, 49, 161, INK, mouseX, mouseY);
        fitted(graphics, Component.translatable("gui.murimblock.stage"), 165, 86, 54, INK, mouseX, mouseY);
        fitted(graphics, stage, 165, 101, 54, INK, mouseX, mouseY);
        centered(graphics, Component.literal("QI"), 269, 86, INK, 72, mouseX, mouseY);
        centered(graphics, Component.literal(QiFormat.format(qi) + " / " + QiFormat.format(qiMax)),
                269, 105, INK, 72, mouseX, mouseY);
        switch (page) {
            case PROFILE -> {
                fitted(graphics, combat, 137, 126, 169, INK, mouseX, mouseY);
                fitted(graphics, Component.literal(cultivation.realmStatus()), 137, 139, 169, MUTED_INK, mouseX, mouseY);
                fitted(graphics, breakthroughLabel(player), 137, 152, 169, MUTED_INK, mouseX, mouseY);
            }
            case TECHNIQUES -> fitted(graphics, Component.translatable("gui.murimblock.techniques.empty"),
                    137, 139, 169, MUTED_INK, mouseX, mouseY);
            case CULTIVATION -> {
                Component next = MurimblockApi.cultivation().getNextCultivation(player)
                        .<Component>map(value -> Component.translatable("gui.murimblock.cultivation.next",
                                Component.translatable("gui.murimblock.realm." + value.realmId()),
                                Component.translatable("gui.murimblock.stage." + value.stageId())))
                        .orElseGet(() -> Component.translatable("gui.murimblock.cultivation.complete"));
                fitted(graphics, next, 137, 126, 169, INK, mouseX, mouseY);
                CultivationService.getRequiredQiMaxForNextStage(player).ifPresent(required -> fitted(graphics,
                        Component.translatable("gui.murimblock.cultivation.required", QiFormat.format(required)),
                        137, 139, 169, MUTED_INK, mouseX, mouseY));
                fitted(graphics, breakthroughLabel(player), 137, 152, 169, MUTED_INK, mouseX, mouseY);
            }
            case INFOS -> {
                fitted(graphics, Component.literal(cultivation.realmStatus()), 137, 126, 169, INK, mouseX, mouseY);
                fitted(graphics, combat, 137, 139, 169, MUTED_INK, mouseX, mouseY);
                fitted(graphics, Component.translatable("gui.murimblock.qi_capacity", QiFormat.format(qiMax)),
                        137, 152, 169, MUTED_INK, mouseX, mouseY);
            }
        }
        for (Page tab : Page.values()) {
            centered(graphics, Component.translatable("gui.murimblock.tab." + tab.id),
                    tab.x + tab.width / 2, 192, INK, tab.width - 8, mouseX, mouseY);
        }
    }

    private Component breakthroughLabel(Player player) {
        if (MurimblockApi.cultivation().getNextCultivation(player).isEmpty()) {
            return Component.translatable("gui.murimblock.cultivation.complete");
        }
        return Component.translatable(MurimblockApi.cultivation().canAttemptBreakthrough(player)
                ? "gui.murimblock.cultivation.ready" : "gui.murimblock.cultivation.not_ready");
    }

    private void centered(GuiGraphics graphics, Component text, int centerX, int y, int color, int maxWidth, int mouseX, int mouseY) {
        Component fitted = fit(text.getString(), maxWidth);
        fitted(graphics, text, centerX - font.width(fitted) / 2, y, maxWidth, color, mouseX, mouseY);
    }

    private void fitted(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, int mouseX, int mouseY) {
        String value = text.getString();
        Component styled = Component.literal(value).withStyle(GUI_TEXT_STYLE);
        graphics.drawString(font, fit(value, maxWidth), left + x, top + y, color, false);
        if (font.width(styled) > maxWidth && mouseX >= left + x && mouseX < left + x + maxWidth
                && mouseY >= top + y && mouseY < top + y + font.lineHeight) {
            hoveredText = styled;
        }
    }

    private Component fit(String value, int maxWidth) {
        Component styled = Component.literal(value).withStyle(GUI_TEXT_STYLE);
        if (font.width(styled) <= maxWidth) {
            return styled;
        }
        int ellipsisWidth = font.width(Component.literal("...").withStyle(GUI_TEXT_STYLE));
        String shortened = font.getSplitter().plainHeadByWidth(value,
                Math.max(0, maxWidth - ellipsisWidth), GUI_TEXT_STYLE);
        return Component.literal(shortened + "...").withStyle(GUI_TEXT_STYLE);
    }

    private static final class ArtButton extends Button {
        private ArtButton(int x, int y, int width, int height, Component title, OnPress action) {
            super(x, y, width, height, title, action, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused()) {
                graphics.fill(getX() + 2, getY() + getHeight() - 2, getX() + getWidth() - 2, getY() + getHeight() - 1, GOLD);
            }
        }
    }
}
