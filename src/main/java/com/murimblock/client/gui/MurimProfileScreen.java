package com.murimblock.client.gui;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.client.CombatBarClient;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.TechniqueIcons;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
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
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.config.ClientConfig;

import static com.murimblock.client.gui.MurimProfileLayout.*;
import static com.murimblock.client.gui.MurimProfileLayout.Field.*;

/**
 * Compact "clear manuscript" menu: ink title band with a red seal, bookmark tabs, a paper page and
 * soft cards. Same information and controls as before; everything is drawn with integer fills.
 */
public final class MurimProfileScreen extends Screen {
    private static final int INK = 0xFF1F2420;
    private static final int INK_SOFT = 0xFF343B35;
    private static final int PAPER = 0xFFF3EBD4;
    private static final int CARD = 0xFFEAE0C4;
    private static final int PAPER_LINE = 0xFFD2C29C;
    private static final int MUTED = 0xFF7A6A4C;
    private static final int TAB_TEXT = 0xFFCFC3A2;
    private static final int GOLD = 0xFFC9A74B;
    private static final int GOLD_DARK = 0xFF8E7230;
    private static final int SEAL = 0xFFA8322D;
    private static final int BLUE = 0xFF238DD4;
    private static final int BLUE_LIGHT = 0xFF73C2F2;
    private static final int BLUE_DARK = 0xFF15598A;
    private static final int TRACK = 0xFFDCD1B4;
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
        renderFrame(g);
        renderTabs(g);
        if (page.showsPlayer()) {
            Box box = layout.player;
            card(g, box);
            fill(g, new Box(box.x() + 8, box.bottom() - 12, box.width() - 16, 1), PAPER_LINE);
            fill(g, new Box(box.x() + 14, box.bottom() - 10, box.width() - 28, 1), PAPER_LINE);
            int size = Math.min((box.height() - 18) / 2, (box.width() - 12) * 3 / 5);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + box.x() + 3, top + box.y() + 3,
                    left + box.right() - 3, top + box.bottom() - 6, size, 0.0625F, mouseX, mouseY, player);
        } else if (settingsMode) {
            card(g, new Box(layout.settingsStatus.x() - 6, layout.content.y(), layout.content.right() - layout.settingsStatus.x() + 6, layout.content.height()));
        } else {
            for (Box panel : layout.panels(page)) card(g, panel);
        }
        CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);
        double qi = MurimblockApi.qi().getQi(player);
        double qiMax = MurimblockApi.qi().getQiMax(player);
        switch (page) {
            case PROFILE -> renderProfile(g, player, qi, qiMax, cultivation);
            case TECHNIQUES -> renderTechniques(g);
            case CULTIVATION -> renderCultivation(g, player, cultivation, qi, qiMax);
            case INFOS -> renderInfos(g, qiMax);
        }
    }

    private void renderFrame(GuiGraphics g) {
        fill(g, new Box(0, 0, layout.width, layout.height), INK);
        outline(g, new Box(2, 2, layout.width - 4, layout.height - 4), GOLD_DARK);
        fill(g, layout.page, PAPER);
        fill(g, new Box(layout.page.x(), layout.page.y(), layout.page.width(), 1), PAPER_LINE);
        for (int x : new int[]{1, layout.width - 6}) {
            for (int y : new int[]{1, layout.height - 6}) {
                fill(g, new Box(x, y, 5, 5), GOLD);
                fill(g, new Box(x + 2, y + 2, 1, 1), INK);
            }
        }
        // Title flanked by gold rules, a red seal on the left.
        Component styledTitle = Component.literal(title.getString()).withStyle(GUI_TEXT_STYLE);
        int titleWidth = Math.min(font.width(styledTitle), layout.title.width());
        int titleLeft = (layout.width - titleWidth) / 2;
        text(g, title, layout.title, GOLD, true, 1);
        int ruleY = layout.title.y() + 4;
        if (titleLeft - 14 > 30) {
            fill(g, new Box(24, ruleY, titleLeft - 32, 1), GOLD_DARK);
            fill(g, new Box(titleLeft - 8, ruleY - 1, 3, 3), GOLD);
            int rightStart = titleLeft + titleWidth + 8;
            fill(g, new Box(rightStart, ruleY - 1, 3, 3), GOLD);
            fill(g, new Box(rightStart + 6, ruleY, layout.close.x() - rightStart - 12, 1), GOLD_DARK);
        }
        seal(g, 9, 5);
        boolean closeHovered = layout.close.contains(pointerX, pointerY);
        fill(g, layout.close, closeHovered ? SEAL : INK_SOFT);
        outline(g, layout.close, closeHovered ? GOLD : GOLD_DARK);
        text(g, Component.literal("x"), new Box(layout.close.x(), layout.close.y() + 2, layout.close.width(), 9), PAPER, true, 1);
    }
    /** Small square chop: red block, paper frame and a stylised 王 (three bars and a stem), not a cross. */
    private void seal(GuiGraphics g, int x, int y) {
        fill(g, new Box(x, y, 13, 13), SEAL);
        outline(g, new Box(x + 1, y + 1, 11, 11), PAPER);
        for (int bar : new int[]{3, 6, 9}) fill(g, new Box(x + 3, y + bar, 7, 1), PAPER);
        fill(g, new Box(x + 6, y + 3, 1, 7), PAPER);
    }
    private void renderTabs(GuiGraphics g) {
        for (Page tab : Page.values()) {
            Box box = layout.tab(tab);
            boolean active = tab == page;
            boolean hovered = !active && box.contains(pointerX, pointerY);
            if (active) {
                // The active bookmark merges into the page below it.
                fill(g, new Box(box.x(), box.y(), box.width(), box.height() + 1), PAPER);
                fill(g, new Box(box.x(), box.y(), box.width(), 2), GOLD);
            } else {
                fill(g, box, hovered ? 0xFF434B44 : INK_SOFT);
                fill(g, new Box(box.x(), box.y(), box.width(), 1), hovered ? GOLD : GOLD_DARK);
            }
            text(g, label("tab." + tab.id), layout.tabLabel(tab), active ? INK : hovered ? PAPER : TAB_TEXT, true, 1);
        }
    }

    static int computeFilledWidth(double qi, double qiMax, int pixelWidth) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0 || pixelWidth <= 0) return 0;
        return (int) Math.round(pixelWidth * Math.clamp(qi / qiMax, 0, 1));
    }
    private void renderProfile(GuiGraphics g, Player player, double qi, double qiMax, CultivationSnapshot cultivation) {
        text(g, NAME, player.getName(), 2);
        section(g, REALM_LABEL, label("realm_label"));
        text(g, REALM, realm(cultivation), 2);
        text(g, layout.text(page, STAGE_LABEL), label("stage"), MUTED);
        text(g, STAGE, label("stage_unknown"));
        section(g, QI_LABEL, label("qi"));
        qiBar(g, layout.profileQiFill, qi, qiMax);
        text(g, QI_VALUE, qiValue(qi, qiMax));
    }
    private void renderTechniques(GuiGraphics g) {
        text(g, HEADER, label("techniques.title"), 2);
        section(g, LIBRARY_LABEL, label("techniques.library"));
        section(g, DETAILS_LABEL, label("techniques.details"));
        techniqueRows.clear();
        barSlots.clear();
        MartialData data = minecraft == null || minecraft.player == null ? MartialData.EMPTY : MartialService.data(minecraft.player);
        if (data.arts().isEmpty()) {
            text(g, label("techniques.empty"), layout.text(page, EMPTY_LIBRARY), MUTED, true, 1);
            text(g, label("techniques.nothing_selected"), layout.text(page, EMPTY_DETAILS), MUTED, true, 1);
            return;
        }
        // Library: each known art, then its techniques (locked ones greyed); click one to see it.
        Box library = layout.panels(page).get(0), details = layout.panels(page).get(1);
        int x = library.x() + 6, w = library.width() - 12, y = library.y() + 20;
        for (MartialArts.Art art : MartialArts.arts()) {
            if (!data.knowsArt(art.id())) continue;
            text(g, new Box(x, y, w, 9), Component.translatable(art.translationKey()), GOLD_DARK);
            y += 12;
            for (MartialArts.Technique technique : art.techniques()) {
                Box row = new Box(x, y, w, 11);
                boolean unlocked = data.hasTechnique(technique.id());
                if (technique.id().equals(selectedTechnique)) fill(g, row, TRACK);
                else if (row.contains(pointerX, pointerY)) fill(g, row, PAPER_LINE);
                Component name = Component.translatable(technique.translationKey());
                text(g, new Box(x + 3, y + 1, w - 3, 9), unlocked ? name : label("techniques.locked", name), unlocked ? INK : MUTED);
                techniqueRows.add(Map.entry(row, technique.id()));
                y += 12;
            }
            y += 4;
        }
        // Details: the selected technique, then the combat bar.
        int dx = details.x() + 6, dw = details.width() - 12, dy = details.y() + 20;
        MartialArts.Technique selected = selectedTechnique == null ? null : MartialArts.technique(selectedTechnique);
        if (selected == null) {
            text(g, label("techniques.select"), new Box(dx, dy, dw, 20), MUTED, false, 1);
        } else {
            boolean unlocked = data.hasTechnique(selected.id());
            // The technique's icon, large, beside its name and figures.
            Box icon = new Box(dx, dy, 32, 32);
            outline(g, new Box(icon.x() - 1, icon.y() - 1, 34, 34), GOLD_DARK);
            TechniqueIcons.draw(g, selected, left + icon.x(), top + icon.y(), 32);
            dx += 38;
            dw -= 38;
            text(g, new Box(dx, dy, dw, 9), Component.translatable(selected.translationKey()), INK);
            text(g, new Box(dx, dy + 12, dw, 9), label("techniques.cost", QiFormat.format(selected.qiCost()), QiFormat.format(selected.cooldownTicks() / 20.0)), MUTED);
            text(g, new Box(dx, dy + 24, dw, 9), unlocked ? label("techniques.mastery", data.mastery(selected.id())) : label("techniques.status_locked"), unlocked ? MUTED : SEAL);
        }
        int barY = details.bottom() - 34;
        section(g, new Box(dx, barY - 14, dw, 9), label("techniques.bar"));
        int slot = Math.min(16, (dw - 9 * 2) / MartialData.BAR_SLOTS);
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            Box box = new Box(dx + i * (slot + 2), barY, slot, slot);
            fill(g, box, box.contains(pointerX, pointerY) ? PAPER_LINE : CARD);
            outline(g, box, GOLD_DARK);
            MartialArts.Technique placed = data.slot(i).map(MartialArts::technique).orElse(null);
            if (placed != null) {
                TechniqueIcons.draw(g, placed, left + box.x() + 1, top + box.y() + 1, slot - 2);
                if (box.contains(pointerX, pointerY)) hoveredText = Component.translatable(placed.translationKey());
            }
            String key = minecraft == null ? "" : CombatBarClient.slotKey(minecraft, i).getTranslatedKeyMessage().getString();
            text(g, Component.literal(key.length() > 1 ? key.substring(0, 1) : key), new Box(box.x(), box.bottom() + 1, slot, 9), MUTED, true, 1);
            barSlots.add(box);
        }
        text(g, label("techniques.bar_hint"), new Box(dx, details.bottom() - 10, dw, 9), MUTED, false, 1);
    }

    /** Selects a technique on the Techniques page, as a click on its row would (tests and tours). */
    public void selectForTests(ResourceLocation technique) {
        selectedTechnique = technique;
    }

    /** Clickable rows and bar slots of the Techniques page, as last drawn. */
    private final List<Map.Entry<Box, ResourceLocation>> techniqueRows = new ArrayList<>();
    private final List<Box> barSlots = new ArrayList<>();
    @Nullable private ResourceLocation selectedTechnique;

    private static String initials(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("[ -]")) if (!word.isEmpty() && out.length() < 2) out.append(Character.toUpperCase(word.charAt(0)));
        return out.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.TECHNIQUES && minecraft != null && minecraft.player != null) {
            int mx = (int) mouseX - left, my = (int) mouseY - top;
            for (Map.Entry<Box, ResourceLocation> row : techniqueRows) {
                if (row.getKey().contains(mx, my)) {
                    selectedTechnique = row.getValue();
                    return true;
                }
            }
            for (int i = 0; i < barSlots.size(); i++) {
                if (!barSlots.get(i).contains(mx, my)) continue;
                MartialData data = MartialService.data(minecraft.player);
                if (button == 1) {
                    PacketDistributor.sendToServer(new TechniquePayloads.Assign(i, Optional.empty()));
                } else if (selectedTechnique != null && data.hasTechnique(selectedTechnique)) {
                    PacketDistributor.sendToServer(new TechniquePayloads.Assign(i, Optional.of(selectedTechnique)));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void renderCultivation(GuiGraphics g, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        section(g, REALM_LABEL, label("cultivation.current"));
        text(g, REALM, realm(cultivation), 2);
        text(g, STAGE, label("cultivation.stage", stage(cultivation)));
        stagePips(g, cultivation.stageId());
        section(g, NEXT_LABEL, label("cultivation.next_stage"));
        var next = MurimblockApi.cultivation().getNextCultivation(player);
        text(g, NEXT_REALM, next.map(MurimProfileScreen::realm).orElseGet(() -> label("cultivation.complete")));
        next.ifPresent(value -> text(g, layout.text(page, NEXT_STAGE), stage(value), MUTED));
        text(g, layout.text(page, REQUIRED_QI), CultivationService.getRequiredQiMaxForNextStage(player)
                .<Component>map(value -> label("cultivation.required", QiFormat.format(value))).orElseGet(() -> label("cultivation.no_requirement")), MUTED);
        section(g, QI_LABEL, label("qi_reserve"));
        qiBar(g, layout.cultivationQiFill, qi, qiMax);
        text(g, QI_VALUE, qiValue(qi, qiMax));
        section(g, BREAKTHROUGH_LABEL, label("cultivation.breakthrough"));
        boolean complete = next.isEmpty();
        boolean ready = !complete && MurimblockApi.cultivation().canAttemptBreakthrough(player);
        Box pill = layout.text(page, BREAKTHROUGH);
        if (complete) {
            fill(g, pill, INK);
        } else if (ready) {
            fill(g, pill, GOLD);
            outline(g, pill, GOLD_DARK);
        } else {
            outline(g, pill, PAPER_LINE);
        }
        text(g, label(complete ? "cultivation.state.complete" : ready ? "cultivation.state.ready" : "cultivation.state.not_ready"),
                new Box(pill.x() + 2, pill.y() + 5, pill.width() - 4, 9), complete ? PAPER : ready ? INK : MUTED, true, 1);
    }
    /** Early, Middle, Late, Peak as four marks; reached stages are gold. */
    private void stagePips(GuiGraphics g, String stageId) {
        int reached = CultivationStage.bySerializedName(stageId).map(Enum::ordinal).orElse(-1);
        for (int i = 0; i < CultivationStage.values().length; i++) {
            Box pip = new Box(layout.stagePips.x() + i * 8, layout.stagePips.y(), 5, 5);
            if (i <= reached) {
                fill(g, pip, GOLD);
                outline(g, pip, GOLD_DARK);
            } else outline(g, pip, PAPER_LINE);
        }
    }
    private void renderInfos(GuiGraphics g, double qiMax) {
        if (settingsMode) {
            text(g, label("settings.title"), layout.settingsHeader, INK, false, 1);
            fill(g, new Box(layout.settingsHeader.x(), layout.settingsHeader.y() + 11, 18, 1), GOLD);
            Box header = new Box(layout.settingsStatus.x(), layout.content.y() + 6, layout.settingsStatus.width(), 9);
            section(g, header, label("infos.player_status"));
            text(g, label("status_placeholder"), layout.settingsStatus, INK, false, 1);
        } else {
            section(g, CONTROLS_LABEL, label("infos.controls"));
            section(g, INFO_LABEL, label("infos.player_status"));
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
        setting(0, "camera", () -> label("settings.camera." + ClientConfig.tpsType.name().toLowerCase(Locale.ROOT)),
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
                if (isHoveredOrFocused()) fill(g, box, CARD);
                fill(g, new Box(box.x(), box.bottom() - 1, box.width(), 1), PAPER_LINE);
                int valueWidth = Math.min(62, box.width() / 3);
                Box labelBox = new Box(box.x() + 4, box.y() + (box.height() - 9) / 2, box.width() - valueWidth - 10, 9);
                text(g, label("settings." + name), labelBox, INK, false, 1);
                Box valueBox = new Box(box.right() - valueWidth, labelBox.y(), valueWidth - 2, 9);
                if (enabled != null) {
                    boolean on = enabled.getAsBoolean();
                    Box check = new Box(valueBox.x(), box.y() + (box.height() - 9) / 2, 9, 9);
                    fill(g, check, on ? GOLD : PAPER);
                    outline(g, check, isHoveredOrFocused() ? GOLD_DARK : INK);
                    if (on) fill(g, new Box(check.x() + 3, check.y() + 3, 3, 3), INK);
                    valueBox = new Box(valueBox.x() + 13, valueBox.y(), valueBox.width() - 13, 9);
                    text(g, value.get(), valueBox, on ? INK : MUTED, false, 1);
                } else {
                    outline(g, new Box(valueBox.x() - 2, valueBox.y() - 3, valueBox.width() + 4, 15), isHoveredOrFocused() ? GOLD_DARK : PAPER_LINE);
                    text(g, value.get(), valueBox, INK, true, 1);
                }
            }
        });
    }
    private Button button(Box box, Supplier<Component> message, Runnable action, boolean painted) {
        return addRenderableWidget(new ManuscriptButton(box, message, action, painted));
    }
    /** Keyboard key cap with a one-pixel bottom edge. */
    private void key(GuiGraphics g, Field field, KeyMapping mapping) {
        Box box = layout.text(page, field);
        fill(g, box, PAPER);
        outline(g, box, INK);
        fill(g, new Box(box.x() + 1, box.bottom() - 3, box.width() - 2, 2), PAPER_LINE);
        text(g, mapping.getTranslatedKeyMessage(), new Box(box.x() + 2, box.y() + 4, box.width() - 4, 9), INK, true, 1);
    }
    private static Component realm(CultivationSnapshot value) { return label("realm." + value.realmId()); }
    private static Component stage(CultivationSnapshot value) { return label("stage." + value.stageId()); }
    /** Whole points read better in a menu; the exact value remains available in commands. */
    static Component qiValue(double qi, double max) {
        long current = Double.isFinite(qi) ? (long) Math.floor(Math.max(0, qi)) : 0;
        long capacity = Double.isFinite(max) ? (long) Math.floor(Math.max(0, max)) : 0;
        return Component.literal(current + " / " + capacity);
    }
    private void qiBar(GuiGraphics g, Box box, double qi, double max) {
        outline(g, new Box(box.x() - 1, box.y() - 1, box.width() + 2, box.height() + 2), INK);
        fill(g, box, TRACK);
        int filled = computeFilledWidth(qi, max, box.width());
        if (filled > 0) {
            fill(g, new Box(box.x(), box.y(), filled, box.height()), BLUE);
            fill(g, new Box(box.x(), box.y(), filled, 1), BLUE_LIGHT);
            fill(g, new Box(box.x(), box.bottom() - 1, filled, 1), BLUE_DARK);
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int x = box.x() + box.width() * quarter / 4;
            fill(g, new Box(x, box.y(), 1, box.height()), x < box.x() + filled ? BLUE_DARK : PAPER_LINE);
        }
    }
    /** Rounded-looking card: soft fill, paper-line border, cut corners. */
    private void card(GuiGraphics g, Box box) {
        fill(g, new Box(box.x() + 1, box.y(), box.width() - 2, box.height()), CARD);
        fill(g, new Box(box.x(), box.y() + 1, 1, box.height() - 2), CARD);
        fill(g, new Box(box.right() - 1, box.y() + 1, 1, box.height() - 2), CARD);
        fill(g, new Box(box.x() + 1, box.y(), box.width() - 2, 1), PAPER_LINE);
        fill(g, new Box(box.x() + 1, box.bottom() - 1, box.width() - 2, 1), PAPER_LINE);
        fill(g, new Box(box.x(), box.y() + 1, 1, box.height() - 2), PAPER_LINE);
        fill(g, new Box(box.right() - 1, box.y() + 1, 1, box.height() - 2), PAPER_LINE);
    }
    /** Small-caps section label in muted ink with a short gold underline. */
    private void section(GuiGraphics g, Field field, Component value) { section(g, layout.text(page, field), value); }
    private void section(GuiGraphics g, Box box, Component value) {
        text(g, Component.literal(value.getString().toUpperCase(Locale.ROOT)), box, MUTED, false, 1);
        fill(g, new Box(box.x(), box.bottom() + 1, Math.min(18, box.width()), 1), GOLD);
    }
    private void fill(GuiGraphics g, Box box, int color) { g.fill(left + box.x(), top + box.y(), left + box.right(), top + box.bottom(), color); }
    private void outline(GuiGraphics g, Box box, int color) {
        fill(g, new Box(box.x(), box.y(), box.width(), 1), color);
        fill(g, new Box(box.x(), box.bottom() - 1, box.width(), 1), color);
        fill(g, new Box(box.x(), box.y(), 1, box.height()), color);
        fill(g, new Box(box.right() - 1, box.y(), 1, box.height()), color);
    }
    private void text(GuiGraphics g, Field field, Component value) { text(g, field, value, 1); }
    private void text(GuiGraphics g, Field field, Component value, int preferredScale) {
        text(g, value, layout.text(page, field), INK, false, preferredScale);
    }
    private void text(GuiGraphics g, Box box, Component value, int color) { text(g, value, box, color, false, 1); }
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
                boolean hot = isHoveredOrFocused();
                fill(g, box, hot ? INK_SOFT : INK);
                outline(g, box, hot ? GOLD : GOLD_DARK);
                text(g, getMessage(), new Box(box.x() + 4, box.y() + (box.height() - 8) / 2, box.width() - 8, 9), hot ? GOLD : PAPER, true, 1);
            }
        }
    }
}
