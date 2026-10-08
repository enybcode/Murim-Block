package com.murimblock.client.gui;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.config.ClientConfig;

import static com.murimblock.client.gui.MurimProfileLayout.*;
import static com.murimblock.client.gui.MurimProfileLayout.Field.*;

public final class MurimProfileScreen extends Screen {
    private static final int INK = 0xFF202522;
    private static final int PAPER = 0xFFF0E7CE;
    private static final int PREVIEW = 0xFFD9D8C8;
    private static final int GOLD = 0xFFC8A64A;
    private static final int BLUE = 0xFF238DD4;
    private static final Style GUI_TEXT_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));
    private Page page = Page.PROFILE;
    private MurimProfileLayout layout;
    private int left;
    private int top;
    private int pointerX;
    private int pointerY;
    private Component hoveredText;
    private boolean settingsMode;

    public MurimProfileScreen() { super(label("title")); }
    public static MurimProfileScreen techniques() {
        MurimProfileScreen screen = new MurimProfileScreen();
        screen.page = Page.TECHNIQUES;
        return screen;
    }
    public static MurimProfileScreen combatSettings() {
        MurimProfileScreen screen = new MurimProfileScreen();
        screen.page = Page.INFOS;
        screen.settingsMode = true;
        return screen;
    }
    private static Component label(String suffix, Object... arguments) {
        return Component.translatable("gui.murimblock." + suffix, arguments);
    }
    @Override protected void init() {
        layout = MurimProfileLayout.forViewport(width, height);
        left = (width - layout.width) / 2;
        top = (height - layout.height) / 2;
        for (Page target : Page.values()) {
            button(layout.tab(target), () -> label("tab." + target.id), () -> {
                page = target;
                settingsMode = false;
                rebuildWidgets();
            }, false);
        }
        Button close = button(layout.close, () -> label("close"), this::onClose, false);
        close.setTooltip(Tooltip.create(close.getMessage()));
        if (page == Page.INFOS) initInfoButtons();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MurimblockKeyMappings.OPEN_PROFILE.matches(keyCode, scanCode) || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.player == null) { onClose(); return; }
        pointerX = mouseX - left;
        pointerY = mouseY - top;
        hoveredText = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredText != null) graphics.renderTooltip(font, font.split(hoveredText, Math.min(220, width - 20)), mouseX, mouseY);
    }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Player player = minecraft.player;
        if (player == null) return;
        g.fill(0, 0, width, height, 0x88000000);
        fill(g, new Box(0, 0, layout.width, layout.height), INK);
        outline(g, new Box(3, 3, layout.width - 6, layout.height - 6), GOLD);
        fill(g, new Box(8, 33, layout.width - 16, layout.height - 70), PAPER);
        for (int x : new int[]{4, layout.width - 10}) {
            for (int y : new int[]{4, layout.height - 10}) {
                fill(g, new Box(x, y, 6, 6), GOLD);
                fill(g, new Box(x + 2, y + 2, 2, 2), INK);
            }
        }
        text(g, title, layout.title, PAPER, true, 2);
        if (layout.width >= 400) { cloud(g, 18, 17); cloud(g, layout.width - 64, 17); }
        fill(g, layout.close, PAPER);
        text(g, Component.literal("X"), new Box(layout.close.x(), layout.close.y() + 4, layout.close.width(), 10), INK, true, 1);
        for (Page tab : Page.values()) {
            Box box = layout.tab(tab);
            fill(g, box, tab == page ? INK : PAPER);
            outline(g, box, INK);
            if (tab == page) fill(g, new Box(box.x() + 2, box.bottom() - 3, box.width() - 4, 2), GOLD);
            text(g, label("tab." + tab.id), layout.tabLabel(tab), tab == page ? PAPER : INK, true, 1);
        }
        if (page.showsPlayer()) {
            fill(g, layout.player, PREVIEW);
            outline(g, layout.player, INK);
            Box box = layout.player;
            int size = Math.min((box.height() - 14) / 2, (box.width() - 12) * 3 / 5);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + box.x() + 3, top + box.y() + 3,
                    left + box.right() - 3, top + box.bottom() - 3, size, 0.0625F, mouseX, mouseY, player);
        } else {
            int split = settingsMode ? layout.settingsStatus.x() - 9 : layout.panels(page).get(1).x() - 9;
            int offset = page == Page.TECHNIQUES ? 25 : 0;
            fill(g, new Box(split, layout.content.y() + offset, 1, layout.content.height() - offset), INK);
        }
        CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);
        double qi = MurimblockApi.qi().getQi(player);
        double qiMax = MurimblockApi.qi().getQiMax(player);
        switch (page) {
            case PROFILE -> renderProfile(g, player, cultivation, qi, qiMax);
            case TECHNIQUES -> renderTechniques(g);
            case CULTIVATION -> renderCultivation(g, player, cultivation, qi, qiMax);
            case INFOS -> renderInfos(g, qiMax);
        }
    }

    static int computeFilledWidth(double qi, double qiMax, int pixelWidth) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0 || pixelWidth <= 0) return 0;
        return (int) Math.round(pixelWidth * Math.clamp(qi / qiMax, 0, 1));
    }
    private void renderProfile(GuiGraphics g, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        text(g, NAME, player.getName(), 2);
        text(g, REALM_LABEL, label("realm_label"));
        text(g, REALM, realm(cultivation));
        text(g, STAGE_LABEL, label("stage"));
        text(g, STAGE, label("stage_unknown"));
        text(g, QI_LABEL, label("qi"));
        qiBar(g, layout.profileQiFill, qi, qiMax);
        text(g, QI_VALUE, qiValue(qi, qiMax));
    }
    private void renderTechniques(GuiGraphics g) {
        text(g, HEADER, label("techniques.title"), 2);
        text(g, LIBRARY_LABEL, label("techniques.library"));
        text(g, DETAILS_LABEL, label("techniques.details"));
        rule(g, layout.text(page, LIBRARY_LABEL));
        rule(g, layout.text(page, DETAILS_LABEL));
        text(g, label("techniques.empty"), layout.text(page, EMPTY_LIBRARY), INK, true, 1);
        text(g, label("techniques.nothing_selected"), layout.text(page, EMPTY_DETAILS), INK, true, 1);
    }
    private void renderCultivation(GuiGraphics g, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        text(g, REALM_LABEL, label("cultivation.current"));
        text(g, REALM, realm(cultivation));
        text(g, STAGE, label("cultivation.stage", stage(cultivation)));
        text(g, NEXT_LABEL, label("cultivation.next_stage"));
        var next = MurimblockApi.cultivation().getNextCultivation(player);
        text(g, NEXT_REALM, next.map(MurimProfileScreen::realm).orElseGet(() -> label("cultivation.complete")));
        next.ifPresent(value -> text(g, NEXT_STAGE, stage(value)));
        text(g, REQUIRED_QI, CultivationService.getRequiredQiMaxForNextStage(player)
                .<Component>map(value -> label("cultivation.required", QiFormat.format(value))).orElseGet(() -> label("cultivation.no_requirement")));
        text(g, QI_LABEL, label("qi_reserve"));
        qiBar(g, layout.cultivationQiFill, qi, qiMax);
        text(g, QI_VALUE, qiValue(qi, qiMax));
        text(g, BREAKTHROUGH_LABEL, label("cultivation.breakthrough"));
        text(g, BREAKTHROUGH, label(next.isEmpty() ? "cultivation.state.complete" : MurimblockApi.cultivation().canAttemptBreakthrough(player)
                ? "cultivation.state.ready" : "cultivation.state.not_ready"));
    }
    private void renderInfos(GuiGraphics g, double qiMax) {
        if (settingsMode) {
            text(g, label("settings.title"), layout.settingsHeader, INK, false, 2);
            Box header = new Box(layout.settingsStatus.x(), layout.content.y(), layout.settingsStatus.width(), 18);
            text(g, label("infos.player_status"), header, INK, false, 1);
            rule(g, header);
            text(g, label("status_placeholder"), layout.settingsStatus, INK, false, 1);
        } else {
            text(g, CONTROLS_LABEL, label("infos.controls"), 2);
            text(g, INFO_LABEL, label("infos.player_status"));
            rule(g, layout.text(page, CONTROLS_LABEL));
            rule(g, layout.text(page, INFO_LABEL));
            text(g, CONTROL_PROFILE, label("tab.profile"));
            text(g, CONTROL_QI, label("infos.charge_qi"));
            text(g, CONTROL_COMBAT, label("infos.combat_mode"));
            key(g, KEY_PROFILE, MurimblockKeyMappings.OPEN_PROFILE);
            key(g, KEY_QI, MurimblockKeyMappings.CHARGE_QI);
            key(g, KEY_COMBAT, MurimblockKeyMappings.COMBAT_MODE);
            text(g, STATUS, label("status_placeholder"));
            text(g, QI_VALUE, label("infos.qi_max", QiFormat.format(qiMax)));
        }
    }
    private void initInfoButtons() {
        if (!settingsMode) {
            button(layout.infoSettings, () -> label("settings.title"), () -> { settingsMode = true; rebuildWidgets(); }, true);
            return;
        }
        setting(0, "camera", () -> label("settings.camera." + ClientConfig.tpsType.name().toLowerCase(java.util.Locale.ROOT)),
                () -> ClientConfig.tpsType = ClientConfig.tpsType.nextEnum(), null);
        toggle(1, "auto_perspective", () -> ClientConfig.autoPerspectiveSwithing, () -> ClientConfig.autoPerspectiveSwithing = !ClientConfig.autoPerspectiveSwithing);
        toggle(2, "first_person_motion", () -> ClientConfig.enableFirstPersonCameraMove, () -> ClientConfig.enableFirstPersonCameraMove = !ClientConfig.enableFirstPersonCameraMove);
        toggle(3, "first_person_model", () -> ClientConfig.enableAnimatedFirstPersonModel, () -> ClientConfig.enableAnimatedFirstPersonModel = !ClientConfig.enableAnimatedFirstPersonModel);
        toggle(4, "lock_on", () -> ClientConfig.lockOnSnapping, () -> ClientConfig.lockOnSnapping = !ClientConfig.lockOnSnapping);
        toggle(5, "blood", () -> ClientConfig.bloodEffects, () -> ClientConfig.bloodEffects = !ClientConfig.bloodEffects);
        button(layout.keybinds, () -> label("settings.keybinds"), () -> minecraft.setScreen(new ControlsScreen(this, minecraft.options)), true);
    }
    private void toggle(int row, String name, BooleanSupplier enabled, Runnable action) {
        setting(row, name, () -> label(enabled.getAsBoolean() ? "settings.on" : "settings.off"), action, enabled);
    }
    private void setting(int row, String name, Supplier<Component> value, Runnable action, BooleanSupplier enabled) {
        Box box = layout.settingRows.get(row);
        addRenderableWidget(new ManuscriptButton(box, () -> label("settings.option", label("settings." + name), value.get()), () -> {
            action.run();
            List<Runnable> save = new ArrayList<>();
            ClientConfig.checkUnsaved(save, new ArrayList<>());
            save.forEach(Runnable::run);
        }, false) {
            @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                setMessage(message.get());
                int valueWidth = Math.min(62, box.width() / 3);
                Box labelBox = new Box(box.x(), box.y() + (box.height() - 9) / 2, box.width() - valueWidth - 6, 10);
                text(g, label("settings." + name), labelBox, INK, false, 1);
                Box valueBox = new Box(box.right() - valueWidth, labelBox.y(), valueWidth, 10);
                if (enabled != null) {
                    Box check = new Box(valueBox.x(), box.y() + (box.height() - 11) / 2, 11, 11);
                    outline(g, check, isHoveredOrFocused() ? GOLD : INK);
                    if (enabled.getAsBoolean()) text(g, Component.literal("X"), new Box(check.x(), check.y() + 1, check.width(), 10), INK, true, 1);
                    valueBox = new Box(valueBox.x() + 15, valueBox.y(), valueBox.width() - 15, 10);
                } else {
                    outline(g, new Box(valueBox.x() - 2, valueBox.y() - 3, valueBox.width() + 2, 16), isHoveredOrFocused() ? GOLD : INK);
                }
                text(g, value.get(), valueBox, INK, true, 1);
            }
        });
    }
    private Button button(Box box, Supplier<Component> message, Runnable action, boolean painted) {
        return addRenderableWidget(new ManuscriptButton(box, message, action, painted));
    }
    private void key(GuiGraphics g, Field field, KeyMapping mapping) {
        Box box = layout.text(page, field);
        outline(g, box, INK);
        text(g, mapping.getTranslatedKeyMessage(), new Box(box.x() + 3, box.y() + 6, box.width() - 6, 10), INK, true, 1);
    }
    private static Component realm(CultivationSnapshot value) { return label("realm." + value.realmId()); }
    private static Component stage(CultivationSnapshot value) { return label("stage." + value.stageId()); }
    private static Component qiValue(double qi, double max) { return Component.literal(QiFormat.format(qi) + " / " + QiFormat.format(max)); }
    private void qiBar(GuiGraphics g, Box box, double qi, double max) {
        outline(g, new Box(box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4), INK);
        fill(g, box, PREVIEW);
        int filled = computeFilledWidth(qi, max, box.width());
        if (filled > 0) fill(g, new Box(box.x(), box.y(), filled, box.height()), BLUE);
    }
    private void fill(GuiGraphics g, Box box, int color) { g.fill(left + box.x(), top + box.y(), left + box.right(), top + box.bottom(), color); }
    private void outline(GuiGraphics g, Box box, int color) {
        fill(g, new Box(box.x(), box.y(), box.width(), 1), color);
        fill(g, new Box(box.x(), box.bottom() - 1, box.width(), 1), color);
        fill(g, new Box(box.x(), box.y(), 1, box.height()), color);
        fill(g, new Box(box.right() - 1, box.y(), 1, box.height()), color);
    }
    private void rule(GuiGraphics g, Box header) { fill(g, new Box(header.x(), header.bottom() + 4, header.width(), 1), GOLD); }
    private void cloud(GuiGraphics g, int x, int y) {
        fill(g, new Box(x, y + 4, 28, 3), GOLD);
        fill(g, new Box(x + 6, y + 1, 16, 3), GOLD);
        fill(g, new Box(x + 10, y - 2, 8, 3), GOLD);
        fill(g, new Box(x + 13, y + 1, 3, 3), INK);
    }
    private void text(GuiGraphics g, Field field, Component value) { text(g, field, value, 1); }
    private void text(GuiGraphics g, Field field, Component value, int preferredScale) {
        text(g, value, layout.text(page, field), INK, false, preferredScale);
    }
    private void text(GuiGraphics g, Component value, Box box, int color, boolean centered, int preferredScale) {
        Component styled = Component.literal(value.getString()).withStyle(GUI_TEXT_STYLE);
        int scale = preferredScale == 2 && font.width(styled) * 2 <= box.width() && box.height() >= 18 ? 2 : 1;
        List<FormattedCharSequence> lines = font.split(styled, box.width() / scale);
        int maxLines = Math.max(1, box.height() / (font.lineHeight * scale));
        boolean truncated = lines.size() > maxLines;
        if (truncated) lines = List.of(fit(styled.getString(), box.width() / scale).getVisualOrderText());
        g.enableScissor(left + box.x(), top + box.y(), left + box.right(), top + box.bottom());
        g.pose().pushPose();
        try {
            g.pose().translate(left + box.x(), top + box.y(), 0);
            g.pose().scale(scale, scale, 1);
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                FormattedCharSequence line = lines.get(i);
                int x = centered ? (box.width() / scale - font.width(line)) / 2 : 0;
                g.drawString(font, line, x, i * font.lineHeight, color, false);
            }
        } finally { g.pose().popPose(); g.disableScissor(); }
        if (truncated && box.contains(pointerX, pointerY)) hoveredText = styled;
    }
    private Component fit(String value, int maxWidth) {
        Component styled = Component.literal(value).withStyle(GUI_TEXT_STYLE);
        if (font.width(styled) <= maxWidth) return styled;
        int ellipsis = font.width(Component.literal("...").withStyle(GUI_TEXT_STYLE));
        String head = font.getSplitter().plainHeadByWidth(value, Math.max(0, maxWidth - ellipsis), GUI_TEXT_STYLE);
        return Component.literal(head + "...").withStyle(GUI_TEXT_STYLE);
    }
    private class ManuscriptButton extends Button {
        final Box box;
        final Supplier<Component> message;
        private final boolean painted;
        ManuscriptButton(Box box, Supplier<Component> message, Runnable action, boolean painted) {
            super(left + box.x(), top + box.y(), box.width(), box.height(), message.get(), button -> action.run(), DEFAULT_NARRATION);
            this.box = box;
            this.message = message;
            this.painted = painted;
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            setMessage(message.get());
            if (painted) {
                fill(g, box, INK);
                outline(g, box, isHoveredOrFocused() ? GOLD : INK);
                text(g, getMessage(), new Box(box.x() + 5, box.y() + 7, box.width() - 10, 10), PAPER, true, 1);
            } else if (isHoveredOrFocused()) outline(g, box, GOLD);
        }
    }
}
