package com.murimblock.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiFormat;
import java.util.ArrayList;
import java.util.List;
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
    static final int PANEL_WIDTH = 320;
    static final int PANEL_HEIGHT = 214;
    static final int QI_FILL_WIDTH = 59;
    private static final int INK = 0xFF2A1F12;
    private static final int MUTED_INK = 0xFF62533F;
    private static final int GOLD = 0xFFE4C263;
    private static final ResourceLocation ART = texture("murim_profile_v4");
    private static final ResourceLocation QI_FILL = texture("murim_qi_fill_v4");
    private static final Style GUI_TEXT_STYLE = Style.EMPTY.withFont(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));

    private Page page = Page.PROFILE;
    private int left;
    private int top;
    private int pointerX;
    private int pointerY;
    private Component hoveredText;
    private boolean settingsMode;

    public MurimProfileScreen() {
        super(label("title"));
    }

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

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/" + name + ".png");
    }

    private static Component label(String suffix, Object... arguments) {
        return Component.translatable("gui.murimblock." + suffix, arguments);
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        for (Page target : Page.values()) {
            Box tab = target.tab;
            addRenderableWidget(new ArtButton(left + tab.x(), top + tab.y(), tab.width(), tab.height(),
                    label("tab." + target.id), button -> {
                        page = target;
                        settingsMode = false;
                        rebuildWidgets();
                    }));
        }
        ArtButton close = new ArtButton(left + 302, top + 8, 14, 14, label("close"), button -> onClose());
        close.setTooltip(Tooltip.create(close.getMessage()));
        addRenderableWidget(close);
        if (page == Page.INFOS) initInfoButtons();
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
        pointerX = mouseX - left;
        pointerY = mouseY - top;
        hoveredText = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredText != null) {
            graphics.renderTooltip(font, font.split(hoveredText, Math.min(220, width - 20)), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        graphics.fill(0, 0, width, height, 0x88000000);
        ResourceLocation background = texture("murim_" + page.id + "_v4");
        RenderSystem.enableBlend();
        graphics.blit(background, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        if (page.showsPlayer()) {
            quietPaper(graphics, new Box(139, 32, 123, 33));
        } else {
            quietPaper(graphics, CONTENT);
            PANELS.get(page).forEach(panel -> paper(graphics, panel));
        }
        renderTabs(graphics, background);
        RenderSystem.disableBlend();
        if (page.showsPlayer()) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    left + PLAYER.x(), top + PLAYER.y(), left + PLAYER.right(), top + PLAYER.bottom(),
                    52, 0.0625F, mouseX, mouseY, player);
        }
        text(graphics, title, TITLE, GOLD, true);
        CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);
        double qi = MurimblockApi.qi().getQi(player);
        double qiMax = MurimblockApi.qi().getQiMax(player);
        switch (page) {
            case PROFILE -> renderProfile(graphics, player, cultivation, qi, qiMax);
            case TECHNIQUES -> renderTechniques(graphics);
            case CULTIVATION -> renderCultivation(graphics, player, cultivation, qi, qiMax);
            case INFOS -> renderInfos(graphics, player, cultivation, qiMax);
        }
    }

    static int computeFilledWidth(double qi, double qiMax) {
        return computeFilledWidth(qi, qiMax, QI_FILL_WIDTH);
    }

    static int computeFilledWidth(double qi, double qiMax, int pixelWidth) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0 || pixelWidth <= 0) {
            return 0;
        }
        return (int) Math.round(pixelWidth * Math.clamp(qi / qiMax, 0, 1));
    }

    private void renderProfile(GuiGraphics graphics, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        text(graphics, NAME, player.getName(), INK);
        text(graphics, REALM, realm(cultivation), INK);
        text(graphics, STAGE_LABEL, label("stage"), INK);
        text(graphics, STAGE, stage(cultivation), INK);
        centered(graphics, QI_LABEL, Component.literal("Qi"), INK);
        renderQiFill(graphics, PROFILE_QI_FILL, qi, qiMax);
        centered(graphics, QI_VALUE, qiValue(qi, qiMax), INK);
        text(graphics, COMBAT, combat(player), INK);
        text(graphics, STATUS, Component.literal(cultivation.realmStatus()), MUTED_INK);
    }

    private void renderTechniques(GuiGraphics graphics) {
        text(graphics, HEADER, label("techniques.title"), INK);
        text(graphics, LIBRARY_LABEL, label("techniques.library"), INK);
        divider(graphics, 17, 81, 96);
        text(graphics, EMPTY_LIBRARY, label("techniques.empty"), MUTED_INK);
        text(graphics, DETAILS_LABEL, label("techniques.details"), INK);
        divider(graphics, 134, 81, 169);
        RenderSystem.enableBlend();
        icon(graphics, Page.TECHNIQUES, 218, 91, 22);
        RenderSystem.disableBlend();
        centered(graphics, EMPTY_DETAILS, label("techniques.nothing_selected"), MUTED_INK);
    }

    private void initInfoButtons() {
        if (!settingsMode) {
            inkButton(new Box(17, 146, 160, 12), () -> label("settings.title"), () -> {
                settingsMode = true;
                rebuildWidgets();
            });
            return;
        }
        setting(62, "camera", () -> label("settings.camera." + ClientConfig.tpsType.name().toLowerCase(java.util.Locale.ROOT)), () -> ClientConfig.tpsType = ClientConfig.tpsType.nextEnum());
        setting(78, "auto_perspective", () -> state(ClientConfig.autoPerspectiveSwithing), () -> ClientConfig.autoPerspectiveSwithing = !ClientConfig.autoPerspectiveSwithing);
        setting(94, "first_person_motion", () -> state(ClientConfig.enableFirstPersonCameraMove), () -> ClientConfig.enableFirstPersonCameraMove = !ClientConfig.enableFirstPersonCameraMove);
        setting(110, "first_person_model", () -> state(ClientConfig.enableAnimatedFirstPersonModel), () -> ClientConfig.enableAnimatedFirstPersonModel = !ClientConfig.enableAnimatedFirstPersonModel);
        setting(126, "lock_on", () -> state(ClientConfig.lockOnSnapping), () -> ClientConfig.lockOnSnapping = !ClientConfig.lockOnSnapping);
        setting(142, "blood", () -> state(ClientConfig.bloodEffects), () -> ClientConfig.bloodEffects = !ClientConfig.bloodEffects);
        inkButton(new Box(199, 145, 104, 12), () -> label("settings.keybinds"), () -> minecraft.setScreen(new ControlsScreen(this, minecraft.options)));
    }

    private static Component state(boolean enabled) {
        return label(enabled ? "settings.on" : "settings.off");
    }

    private void setting(int y, String name, Supplier<Component> value, Runnable action) {
        inkButton(new Box(17, y, 160, 12), () -> label("settings.option", label("settings." + name), value.get()), () -> {
            action.run();
            List<Runnable> save = new ArrayList<>();
            ClientConfig.checkUnsaved(save, new ArrayList<>());
            save.forEach(Runnable::run);
        });
    }

    private Button inkButton(Box box, Supplier<Component> title, Runnable action) {
        return addRenderableWidget(new InkButton(box, title, action));
    }

    private void renderCultivation(GuiGraphics graphics, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        text(graphics, REALM_LABEL, label("cultivation.current"), MUTED_INK);
        text(graphics, REALM, realm(cultivation), INK);
        text(graphics, STAGE, label("cultivation.stage", stage(cultivation)), INK);
        divider(graphics, 17, 79, 163);
        text(graphics, NEXT_LABEL, label("cultivation.next_stage"), MUTED_INK);
        var next = MurimblockApi.cultivation().getNextCultivation(player);
        if (next.isPresent()) {
            text(graphics, NEXT_REALM, realm(next.get()), INK);
            text(graphics, NEXT_STAGE, stage(next.get()), INK);
        } else {
            text(graphics, NEXT_REALM, label("cultivation.complete"), INK);
        }
        Component required = CultivationService.getRequiredQiMaxForNextStage(player)
                .<Component>map(value -> label("cultivation.required", QiFormat.format(value)))
                .orElseGet(() -> label("cultivation.no_requirement"));
        text(graphics, REQUIRED_QI, required, INK);
        centered(graphics, QI_LABEL, label("qi_reserve"), INK);
        RenderSystem.enableBlend();
        Box frame = CULTIVATION_QI_FRAME;
        frame(graphics, ART, new Box(233, 95, 74, 10), frame, 3);
        Box fill = new Box(frame.x() + 6, frame.y() + 2, frame.width() - 12, 5);
        graphics.fill(left + fill.x(), top + fill.y(), left + fill.right(), top + fill.bottom(), 0xFF1F211B);
        renderQiFill(graphics, fill, qi, qiMax);
        RenderSystem.disableBlend();
        centered(graphics, QI_VALUE, qiValue(qi, qiMax), INK);
        centered(graphics, BREAKTHROUGH_LABEL, label("cultivation.breakthrough"), INK);
        Component state = next.isEmpty() ? label("cultivation.state.complete")
                : label(MurimblockApi.cultivation().canAttemptBreakthrough(player)
                        ? "cultivation.state.ready" : "cultivation.state.not_ready");
        centered(graphics, BREAKTHROUGH, state, MUTED_INK);
    }

    private void renderInfos(GuiGraphics graphics, Player player, CultivationSnapshot cultivation, double qiMax) {
        text(graphics, CONTROLS_LABEL, label(settingsMode ? "settings.title" : "infos.controls"), INK);
        divider(graphics, 17, 51, 160);
        if (!settingsMode) {
            text(graphics, CONTROL_PROFILE, label("tab.profile"), INK);
            text(graphics, CONTROL_QI, label("infos.charge_qi"), INK);
            text(graphics, CONTROL_COMBAT, label("infos.combat_mode"), INK);
            key(graphics, KEY_PROFILE, MurimblockKeyMappings.OPEN_PROFILE);
            key(graphics, KEY_QI, MurimblockKeyMappings.CHARGE_QI);
            key(graphics, KEY_COMBAT, MurimblockKeyMappings.COMBAT_MODE);
        }
        text(graphics, INFO_LABEL, label("infos.player_status"), INK);
        divider(graphics, 199, 51, 104);
        text(graphics, STATUS, Component.literal(cultivation.realmStatus()), MUTED_INK);
        text(graphics, COMBAT, combat(player), INK);
        if (!settingsMode) text(graphics, QI_VALUE, label("infos.qi_max", QiFormat.format(qiMax)), INK);
    }

    private static Component realm(CultivationSnapshot cultivation) {
        return label("realm." + cultivation.realmId());
    }

    private static Component stage(CultivationSnapshot cultivation) {
        return label("stage." + cultivation.stageId());
    }

    private static Component combat(Player player) {
        return label(MurimblockApi.combat().isInCombatMode(player) ? "combat.on" : "combat.off");
    }

    private static Component qiValue(double qi, double qiMax) {
        return Component.literal(QiFormat.format(qi) + " / " + QiFormat.format(qiMax));
    }

    private void key(GuiGraphics graphics, Field field, KeyMapping mapping) {
        Box box = page.text(field);
        RenderSystem.enableBlend();
        paper(graphics, new Box(box.x() - 4, box.y() - 5, box.width() + 8, 18));
        RenderSystem.disableBlend();
        centered(graphics, field, mapping.getTranslatedKeyMessage(), INK);
    }

    private void renderQiFill(GuiGraphics graphics, Box target, double qi, double qiMax) {
        int filled = computeFilledWidth(qi, qiMax, target.width());
        if (filled <= 0) {
            return;
        }
        int sourceWidth = Math.max(1, computeFilledWidth(qi, qiMax));
        graphics.blit(QI_FILL, left + target.x(), top + target.y(), filled, target.height(),
                0, 0, sourceWidth, 5, QI_FILL_WIDTH, 5);
    }

    private void renderTabs(GuiGraphics graphics, ResourceLocation background) {
        for (Page tab : Page.values()) {
            Box box = tab.tab;
            graphics.fill(left + box.x(), top + box.y(), left + box.right(), top + box.bottom(),
                    tab == page ? 0xFFE8C879 : 0xFFECDFC4);
            frame(graphics, background, box, box, 4);
            blit(graphics, ART, tab.icon, tab.tabIcon());
            text(graphics, label("tab." + tab.id), tab.tabLabel(), INK, true);
        }
    }

    private void quietPaper(GuiGraphics graphics, Box target) {
        blit(graphics, ART, new Box(169, 32, 9, 6), target);
    }

    private void paper(GuiGraphics graphics, Box target) {
        quietPaper(graphics, target);
        frame(graphics, ART, new Box(131, 28, 176, 48), target, 4);
    }

    // Reuse the approved bitmap borders while reserving a clear centre for text.
    private void frame(GuiGraphics graphics, ResourceLocation texture, Box source, Box target, int border) {
        int[] sx = {source.x(), source.x() + border, source.right() - border};
        int[] sy = {source.y(), source.y() + border, source.bottom() - border};
        int[] sw = {border, source.width() - border * 2, border};
        int[] sh = {border, source.height() - border * 2, border};
        int[] dx = {target.x(), target.x() + border, target.right() - border};
        int[] dy = {target.y(), target.y() + border, target.bottom() - border};
        int[] dw = {border, target.width() - border * 2, border};
        int[] dh = {border, target.height() - border * 2, border};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row != 1 || column != 1) {
                    blit(graphics, texture, new Box(sx[column], sy[row], sw[column], sh[row]),
                            new Box(dx[column], dy[row], dw[column], dh[row]));
                }
            }
        }
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, Box source, Box target) {
        graphics.blit(texture, left + target.x(), top + target.y(), target.width(), target.height(),
                source.x(), source.y(), source.width(), source.height(), PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void icon(GuiGraphics graphics, Page icon, int centerX, int y, int height) {
        int width = Math.round(icon.icon.width() * (float) height / icon.icon.height());
        blit(graphics, ART, icon.icon, new Box(Math.round(centerX - width / 2.0F), y, width, height));
    }

    private void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(left + x, top + y, left + x + width, top + y + 1, 0xFF9E895D);
        int center = x + width / 2;
        graphics.fill(left + center, top + y - 1, left + center + 3, top + y + 2, 0xFF9E895D);
    }

    private void text(GuiGraphics graphics, Field field, Component value, int color) {
        text(graphics, value, page.text(field), color, false);
    }

    private void centered(GuiGraphics graphics, Field field, Component value, int color) {
        text(graphics, value, page.text(field), color, true);
    }

    private void text(GuiGraphics graphics, Component value, Box box, int color, boolean centered) {
        Component styled = Component.literal(value.getString()).withStyle(GUI_TEXT_STYLE);
        List<FormattedCharSequence> lines = font.split(styled, box.width());
        int maxLines = Math.max(1, box.height() / font.lineHeight);
        boolean truncated = lines.size() > maxLines;
        if (truncated) {
            lines = List.of(fit(styled.getString(), box.width()).getVisualOrderText());
        }
        graphics.enableScissor(left + box.x(), top + box.y(), left + box.right(), top + box.bottom());
        try {
            for (int line = 0; line < lines.size(); line++) {
                FormattedCharSequence text = lines.get(line);
                int x = centered ? box.x() + (box.width() - font.width(text)) / 2 : box.x();
                graphics.drawString(font, text, left + x, top + box.y() + line * font.lineHeight, color, false);
            }
        } finally {
            graphics.disableScissor();
        }
        if (truncated && box.contains(pointerX, pointerY)) {
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

    private static class ArtButton extends Button {
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

    private final class InkButton extends ArtButton {
        private final Box box;
        private final Supplier<Component> title;

        private InkButton(Box box, Supplier<Component> title, Runnable action) {
            super(left + box.x(), top + box.y(), box.width(), box.height(), title.get(), button -> action.run());
            this.box = box;
            this.title = title;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            setMessage(title.get());
            RenderSystem.enableBlend();
            quietPaper(graphics, box);
            RenderSystem.disableBlend();
            int edge = isHoveredOrFocused() ? 0xFFB69342 : 0xFF9E895D;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + 1, edge);
            graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), edge);
            graphics.fill(getX(), getY(), getX() + 1, getY() + getHeight(), edge);
            graphics.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), edge);
            text(graphics, getMessage(), new Box(box.x() + 4, box.y() + 1, box.width() - 8, 9), active ? INK : MUTED_INK, false);
        }
    }
}
