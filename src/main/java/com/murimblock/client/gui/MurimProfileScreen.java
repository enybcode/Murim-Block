package com.murimblock.client.gui;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.client.CombatBarClient;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.client.TechniqueIcons;
import com.murimblock.client.hud.HudSize;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialManualItem;
import com.murimblock.martial.MartialProgress;
import com.murimblock.martial.MartialService;
import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.config.ClientConfig;

import static com.murimblock.client.gui.MurimProfileLayout.*;
import static com.murimblock.client.gui.MurimProfileLayout.Field.*;

/**
 * The Murim menu, "rice paper, ink night": a dark aged paper scroll held by two wooden rods, light ink, hanging red
 * ribbon tabs, gold rules and red seals. Same information and controls as before; everything is drawn with integer
 * fills. The Techniques page is a library: the manuals first (an unknown one shows nothing), then the open manual's
 * techniques, each shown only once unlocked ("???" with its quest, or its secret, until then).
 */
public final class MurimProfileScreen extends Screen {
    /** The scroll's back and the darkest marks. */
    private static final int FRAME = 0xFF1A1714;
    /** The paper, its cards, its fibres and rules. */
    private static final int PAPER = 0xFF2B2721;
    private static final int CARD = 0xFF332E27;
    private static final int FIBRE = 0xFF2F2A24;
    private static final int PAPER_LINE = 0xFF4A443B;
    private static final int TRACK = 0xFF221F1A;
    private static final int SELECTED = 0xFF3A342C;
    /** The ink: light on the dark paper. */
    private static final int INK = 0xFFEDE3CC;
    private static final int INK_SOFT = 0xFFCFC3A8;
    private static final int MUTED = 0xFFB3A68A;
    private static final int GOLD = 0xFFC9A45C;
    private static final int GOLD_LIGHT = 0xFFF2D9A0;
    private static final int GOLD_DARK = 0xFF7A5E2C;
    private static final int SEAL = 0xFFC24A36;
    private static final int SEAL_TEXT = 0xFFF4E9D2;
    private static final int TAB = 0xFF3A342C;
    private static final int BLUE = 0xFF5C9BCC;
    private static final int BLUE_LIGHT = 0xFFB8DBF4;
    private static final int AZURE_TEXT = 0xFF9CC9EC;
    private static final int GREEN = 0xFF8FCB9B;
    private static final int WOOD = 0xFF4A3320;
    private static final int WOOD_LIGHT = 0xFF6B4A2E;
    private static final int WOOD_DARK = 0xFF2E1F13;
    private static final int COVER = 0xFF2F5E52;
    private static final int COVER_DARK = 0xFF1F3F37;
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
    public static MurimProfileScreen cultivation() {
        MurimProfileScreen screen = new MurimProfileScreen();
        screen.page = Page.CULTIVATION;
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
        g.fill(0, 0, width, height, 0x9E000000);
        renderFrame(g);
        renderTabs(g);
        if (page.showsPlayer()) {
            Box box = layout.player;
            // The player in a brush circle (enso), left open at the top right.
            int size = Math.min((box.height() - 18) / 2, (box.width() - 12) * 3 / 5);
            int radius = Math.min(box.width(), box.height()) / 2 - 3;
            enso(g, box.x() + box.width() / 2, box.y() + box.height() / 2, radius);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + box.x() + 3, top + box.y() + 3,
                    left + box.right() - 3, top + box.bottom() - 6, size, 0.0625F, mouseX, mouseY, player);
        } else if (settingsMode) {
            card(g, new Box(layout.settingsStatus.x() - 6, layout.content.y(), layout.content.right() - layout.settingsStatus.x() + 6, layout.content.height()));
        } else if (page != Page.TECHNIQUES) {
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
        fill(g, new Box(0, 0, layout.width, layout.height), FRAME);
        fill(g, layout.page, PAPER);
        // The paper's fibres, then its edge darkened.
        for (int x = layout.page.x() + 3; x < layout.page.right(); x += 7) fill(g, new Box(x, layout.page.y(), 1, layout.page.height()), FIBRE);
        outline(g, layout.page, TRACK);
        // The two wooden rods holding the scroll, gold-capped.
        rod(g, -5);
        rod(g, layout.height);
        // Title between gold rules, a red seal on the left.
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
        // Close: a red seal.
        boolean closeHovered = layout.close.contains(pointerX, pointerY);
        fill(g, layout.close, closeHovered ? 0xFFD9614B : SEAL);
        outline(g, layout.close, closeHovered ? GOLD_LIGHT : 0xFF8E3324);
        text(g, Component.literal("x"), new Box(layout.close.x(), layout.close.y() + 2, layout.close.width(), 9), SEAL_TEXT, true, 1);
    }
    /** A wooden rod across the scroll at `y` (5 px), its ends capped in gold. */
    private void rod(GuiGraphics g, int y) {
        fill(g, new Box(-3, y, layout.width + 6, 5), WOOD);
        fill(g, new Box(-3, y, layout.width + 6, 1), WOOD_LIGHT);
        fill(g, new Box(-3, y + 4, layout.width + 6, 1), WOOD_DARK);
        for (int x : new int[]{-7, layout.width + 3}) {
            fill(g, new Box(x, y - 1, 4, 7), GOLD);
            fill(g, new Box(x, y + 5, 4, 1), GOLD_DARK);
        }
    }
    /** Small square chop: red block, light frame and a stylised 王 (three bars and a stem). */
    private void seal(GuiGraphics g, int x, int y) {
        fill(g, new Box(x, y, 13, 13), SEAL);
        outline(g, new Box(x + 1, y + 1, 11, 11), SEAL_TEXT);
        for (int bar : new int[]{3, 6, 9}) fill(g, new Box(x + 3, y + bar, 7, 1), SEAL_TEXT);
        fill(g, new Box(x + 6, y + 3, 1, 7), SEAL_TEXT);
    }
    /**
     * A brush circle: a ring two or three pixels thick, thicker where the brush pressed (left), thinning to an open
     * end at the top right, a drop of ink beside it.
     */
    private void enso(GuiGraphics g, int cx, int cy, int radius) {
        if (radius < 8) return;
        for (int dy = -radius - 2; dy <= radius + 2; dy++) {
            for (int dx = -radius - 2; dx <= radius + 2; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                // Angle from the top, clockwise: the stroke runs from 40 degrees round to 345.
                double a = Math.toDegrees(Math.atan2(dx, -dy));
                if (a < 0) a += 360;
                if (a < 40 || a > 345) continue;
                double press = 1.0 + 1.4 * Math.sin(Math.toRadians((a - 40) * 180 / 305));
                if (Math.abs(d - radius) <= press / 2 + 0.25) fill(g, new Box(cx + dx, cy + dy, 1, 1), (a > 300 ? INK_SOFT : INK) & 0xD8FFFFFF);
            }
        }
        fill(g, new Box(cx + radius * 7 / 10 + 2, cy - radius * 7 / 10 - 3, 2, 2), INK_SOFT);
    }
    private void renderTabs(GuiGraphics g) {
        for (Page tab : Page.values()) {
            Box box = layout.tab(tab);
            boolean active = tab == page;
            boolean hovered = !active && box.contains(pointerX, pointerY);
            if (active) {
                // A red ribbon hanging into the page, its end cut in a V.
                fill(g, new Box(box.x(), box.y() - 2, box.width(), box.height() + 6), SEAL);
                int mid = box.x() + box.width() / 2;
                for (int i = 0; i < 3; i++) fill(g, new Box(mid - 3 + i, box.bottom() + 3 - i, 6 - i * 2, 1), PAPER);
                fill(g, new Box(box.x(), box.y() - 2, box.width(), 1), GOLD);
            } else {
                fill(g, box, hovered ? 0xFF4A443B : TAB);
                fill(g, new Box(box.x(), box.y(), box.width(), 1), hovered ? GOLD : GOLD_DARK);
            }
            text(g, label("tab." + tab.id), layout.tabLabel(tab), active ? SEAL_TEXT : hovered ? INK : INK_SOFT, true, 1);
        }
    }

    static int computeFilledWidth(double qi, double qiMax, int pixelWidth) {
        if (!Double.isFinite(qi) || !Double.isFinite(qiMax) || qiMax <= 0 || pixelWidth <= 0) return 0;
        return (int) Math.round(pixelWidth * Math.clamp(qi / qiMax, 0, 1));
    }
    private void renderProfile(GuiGraphics g, Player player, double qi, double qiMax, CultivationSnapshot cultivation) {
        text(g, NAME, player.getName(), 2);
        section(g, REALM_LABEL, label("realm_label"));
        Box realm = layout.text(page, REALM);
        fill(g, new Box(realm.x() - 4, realm.y(), 2, realm.height()), GOLD);
        text(g, realm(cultivation), realm, GOLD_LIGHT, false, 2);
        text(g, layout.text(page, STAGE_LABEL), label("stage"), MUTED);
        text(g, STAGE, label("stage_unknown"));
        // The passive in use: only one at a time, the main style's; what it does on hover.
        section(g, PASSIVE_LABEL, label("passive"));
        MartialArts.Passive passive = MartialService.data(player).passive().orElse(null);
        Box passiveBox = layout.text(page, PASSIVE);
        if (passive == null) {
            text(g, label("passive_none"), passiveBox, MUTED, false, 1);
        } else {
            text(g, Component.translatable(passive.translationKey()), passiveBox, AZURE_TEXT, false, 1);
            if (passiveBox.contains(pointerX, pointerY)) hoveredText = Component.translatable(passive.translationKey() + ".desc");
        }
        section(g, QI_LABEL, label("qi"));
        qiBar(g, layout.profileQiFill, qi, qiMax);
        text(g, QI_VALUE, qiValue(qi, qiMax));
    }

    // ---------------------------------------------------------------- Techniques: the library, then a manual

    /** The manual open on the Techniques page; none: the library of manuals. */
    @Nullable private ResourceLocation openArt;
    /** Clickable manual covers, the "Library" button, technique tiles, bar slots and M1 slots, as last drawn. */
    private final List<Map.Entry<Box, ResourceLocation>> covers = new ArrayList<>();
    @Nullable private Box backButton;
    /** On the passive of an art that is not the main style: the button that makes it so, and the art. */
    @Nullable private Map.Entry<Box, ResourceLocation> mainButton;
    private final List<Map.Entry<Box, ResourceLocation>> techniqueRows = new ArrayList<>();
    private final List<Box> barSlots = new ArrayList<>();
    private final List<Box> strikeSlots = new ArrayList<>();
    @Nullable private ResourceLocation selectedTechnique;
    private static final Map<ResourceLocation, ItemStack> MANUALS = new HashMap<>();

    private void renderTechniques(GuiGraphics g) {
        covers.clear();
        techniqueRows.clear();
        barSlots.clear();
        strikeSlots.clear();
        backButton = null;
        mainButton = null;
        MartialData data = minecraft == null || minecraft.player == null ? MartialData.EMPTY : MartialService.data(minecraft.player);
        MartialArts.Art art = openArt == null ? null : MartialArts.art(openArt);
        if (art == null || !data.knowsArt(art.id())) {
            openArt = null;
            renderLibrary(g, data);
        } else {
            renderManual(g, data, art);
        }
    }

    /** The manual item of an art, for its cover (empty if it has none). */
    private static ItemStack manual(ResourceLocation art) {
        return MANUALS.computeIfAbsent(art, id -> BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof MartialManualItem m && m.art().equals(id)).findFirst()
                .map(ItemStack::new).orElse(ItemStack.EMPTY));
    }

    /** How much of an art this player can read: its passive and the techniques unlocked, of all of them. */
    private static Component revealed(MartialData data, MartialArts.Art art) {
        int shown = 1;
        for (MartialArts.Technique t : art.techniques()) if (data.hasTechnique(t.id())) shown++;
        return label("techniques.revealed", shown, 1 + art.techniques().size());
    }

    /** The library: one cover per martial art on a wooden shelf; an art not learned is a dark "???" cover. */
    private void renderLibrary(GuiGraphics g, MartialData data) {
        text(g, HEADER, label("techniques.title"), 2);
        Box content = layout.content;
        text(g, label("techniques.library_hint"), new Box(content.x(), content.y() + 22, content.width(), 18), MUTED, false, 1);
        List<MartialArts.Art> arts = new ArrayList<>();
        MartialArts.arts().forEach(arts::add);
        int coverWidth = 72, coverHeight = 92, gap = 16;
        int shelfY = content.bottom() - 6;
        int total = arts.size() * coverWidth + Math.max(0, arts.size() - 1) * gap;
        int x = content.x() + Math.max(0, (content.width() - total) / 2);
        int y = Math.max(content.y() + 42, shelfY - coverHeight);
        int coverH = shelfY - y;
        fill(g, new Box(content.x(), shelfY, content.width(), 3), WOOD);
        fill(g, new Box(content.x(), shelfY, content.width(), 1), WOOD_LIGHT);
        fill(g, new Box(content.x(), shelfY + 3, content.width(), 1), WOOD_DARK);
        if (arts.isEmpty()) {
            text(g, label("techniques.empty"), new Box(content.x(), y + coverH / 2, content.width(), 9), MUTED, true, 1);
            return;
        }
        for (MartialArts.Art art : arts) {
            Box cover = new Box(x, y, coverWidth, coverH);
            boolean known = data.knowsArt(art.id());
            boolean hot = known && cover.contains(pointerX, pointerY);
            if (known) {
                fill(g, cover, hot ? 0xFF367064 : COVER);
                fill(g, new Box(cover.x() + 4, cover.bottom() - 6, cover.width() - 4, 6), COVER_DARK);
                fill(g, new Box(cover.x(), cover.y(), 4, cover.height()), SEAL);
                outline(g, cover, hot ? GOLD_LIGHT : GOLD);
                ItemStack stack = manual(art.id());
                if (!stack.isEmpty()) {
                    g.pose().pushPose();
                    g.pose().translate(left + cover.x() + 2 + (cover.width() - 32) / 2.0F, top + cover.y() + 6, 0);
                    g.pose().scale(2, 2, 1);
                    g.renderItem(stack, 0, 0);
                    g.pose().popPose();
                }
                text(g, Component.translatable(art.translationKey()), new Box(cover.x() + 6, cover.y() + 42, cover.width() - 9, 18), GOLD_LIGHT, true, 1);
                text(g, revealed(data, art), new Box(cover.x() + 6, cover.y() + 61, cover.width() - 9, 9), INK_SOFT, true, 1);
                Box open = new Box(cover.x() + 4 + (cover.width() - 4 - 34) / 2, cover.bottom() - 17, 34, 11);
                fill(g, open, SEAL);
                text(g, label("techniques.open"), new Box(open.x(), open.y() + 1, open.width(), 9), SEAL_TEXT, true, 1);
                covers.add(Map.entry(cover, art.id()));
            } else {
                fill(g, cover, 0xFF26231E);
                dashed(g, cover, PAPER_LINE);
                text(g, Component.literal("???"), new Box(cover.x(), cover.y() + coverH / 2 - 10, cover.width(), 9), MUTED, true, 1);
                text(g, label("techniques.not_found"), new Box(cover.x(), cover.y() + coverH / 2 + 2, cover.width(), 9), PAPER_LINE, true, 1);
            }
            x += coverWidth + gap;
        }
    }

    /**
     * An open manual: back to the library, the manual's name; its passive and techniques as tiles (one not unlocked is
     * "???", with a gold lock for a quest or a red seal for a secret); the selected one's page below, with the combat bar
     * (or, for the passive, its four M1).
     */
    private void renderManual(GuiGraphics g, MartialData data, MartialArts.Art art) {
        Box content = layout.content;
        backButton = new Box(content.x(), content.y(), 48, 14);
        boolean backHot = backButton.contains(pointerX, pointerY);
        fill(g, backButton, backHot ? SELECTED : PAPER);
        outline(g, backButton, backHot ? INK : MUTED);
        text(g, Component.literal("< ").append(label("techniques.back")), new Box(backButton.x(), backButton.y() + 3, backButton.width(), 9), INK, true, 1);
        ItemStack stack = manual(art.id());
        int nameX = backButton.right() + 6;
        if (!stack.isEmpty()) {
            g.renderItem(stack, left + nameX, top + content.y() - 1);
            nameX += 20;
        }
        text(g, new Box(nameX, content.y() + 3, content.right() - nameX - 78, 9), Component.translatable(art.translationKey()), GOLD_LIGHT);
        text(g, new Box(content.right() - 76, content.y() + 3, 76, 9), revealed(data, art), MUTED);

        // The tiles: the passive, then each technique (a secret only as "???" until found).
        List<ResourceLocation> entries = new ArrayList<>();
        entries.add(art.passive().id());
        for (MartialArts.Technique t : art.techniques()) entries.add(t.id());
        if (selectedTechnique == null || !entries.contains(selectedTechnique)) selectedTechnique = art.passive().id();
        int tx = content.x(), ty = content.y() + 20;
        for (ResourceLocation id : entries) {
            MartialArts.Technique technique = MartialArts.technique(id);
            boolean shown = technique == null || data.hasTechnique(id);
            boolean selected = id.equals(selectedTechnique);
            Box tile = new Box(tx, selected ? ty - 2 : ty, 20, 20);
            fill(g, tile, shown ? TRACK : 0xFF221F1A);
            if (shown) {
                if (technique == null) TechniqueIcons.draw(g, id, Component.translatable(art.passive().translationKey()), left + tile.x() + 2, top + tile.y() + 2, 16, 0xFF2F6E9E);
                else TechniqueIcons.draw(g, technique, left + tile.x() + 2, top + tile.y() + 2, 16);
            } else {
                text(g, Component.literal("?"), new Box(tile.x(), tile.y() + 6, tile.width(), 9), MUTED, true, 1);
            }
            if (selected) {
                outline(g, tile, SEAL);
                outline(g, new Box(tile.x() - 1, tile.y() - 1, tile.width() + 2, tile.height() + 2), SEAL);
            } else if (shown) {
                outline(g, tile, tile.contains(pointerX, pointerY) ? INK : MUTED);
            } else {
                dashed(g, tile, tile.contains(pointerX, pointerY) ? MUTED : PAPER_LINE);
            }
            if (!shown && technique != null) {
                // A secret: a red seal; a technique a quest unlocks: a gold lock.
                Box badge = new Box(tile.right() - 4, tile.bottom() - 4, 6, 6);
                fill(g, badge, technique.secret() ? SEAL : GOLD);
                if (!technique.secret()) fill(g, new Box(badge.x() + 2, badge.y() + 2, 2, 2), FRAME);
            }
            if (tile.contains(pointerX, pointerY)) hoveredText = shown ? (technique == null ? Component.translatable(art.passive().translationKey())
                    : techniqueName(data, art, technique)) : Component.literal("???");
            techniqueRows.add(Map.entry(tile, id));
            tx += 26;
        }
        int ruleY = ty + 24;
        for (int x = content.x(); x < content.right(); x += 4) fill(g, new Box(x, ruleY, 2, 1), PAPER_LINE);

        // The selected one's page: left its name, figures and text; right the combat bar (or the M1).
        int pageY = ruleY + 5;
        if (content.bottom() - pageY < 40) return;
        int leftWidth = content.width() * 58 / 100 - 6;
        Box info = new Box(content.x(), pageY, leftWidth, content.bottom() - pageY);
        Box side = new Box(info.right() + 10, pageY, content.right() - info.right() - 10, content.bottom() - pageY);
        fill(g, new Box(info.right() + 4, pageY, 1, side.height()), PAPER_LINE);
        MartialArts.Art passiveArt = MartialArts.artOfPassive(selectedTechnique);
        if (passiveArt != null) {
            renderPassive(g, data, passiveArt, info, side);
            return;
        }
        MartialArts.Technique selected = MartialArts.technique(selectedTechnique);
        if (selected == null) return;
        int y = info.y();
        if (data.hasTechnique(selected.id())) {
            text(g, new Box(info.x(), y, info.width(), 9), techniqueName(data, art, selected), GOLD_LIGHT);
            text(g, new Box(info.x(), y + 11, info.width(), 9), selected.secret() ? label("techniques.secret_found")
                    : label("techniques.cost", QiFormat.format(selected.qiCost()), QiFormat.format(selected.cooldownTicks() / 20.0)), AZURE_TEXT);
            if (!selected.secret()) {
                int mastery = data.mastery(selected.id());
                text(g, new Box(info.x(), y + 22, info.width(), 9), label("techniques.mastery", mastery), MUTED);
                fill(g, new Box(info.x(), y + 32, info.width(), 2), TRACK);
                int reached = info.width() * Math.clamp(mastery, 0, 100) / 100;
                if (reached > 0) fill(g, new Box(info.x(), y + 32, reached, 2), GOLD);
            }
            int aboutY = y + 38;
            if (info.bottom() - aboutY >= 9) about(g, Component.translatable(selected.translationKey() + ".desc"), new Box(info.x(), aboutY, info.width(), info.bottom() - aboutY));
        } else {
            text(g, new Box(info.x(), y, info.width(), 9), Component.literal("???"), MUTED);
            text(g, new Box(info.x(), y + 11, info.width(), 9), label(selected.secret() ? "techniques.secret_tag" : "techniques.locked_tag"), SEAL);
            Component why = selected.secret() ? label("techniques.secret_hint") : questStatus(data, art, selected);
            text(g, why, new Box(info.x(), y + 24, info.width(), info.bottom() - y - 24), INK, false, 1);
        }
        renderBar(g, data, side);
    }

    /** The combat bar, two rows of five slots, its keys and its hint. */
    private void renderBar(GuiGraphics g, MartialData data, Box side) {
        section(g, new Box(side.x(), side.y(), side.width(), 9), label("techniques.bar"));
        int slot = 16, step = 20;
        int bx = side.x(), by = side.y() + 14;
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            Box box = new Box(bx + (i % 5) * step, by + (i / 5) * (slot + 11), slot, slot);
            fill(g, box, box.contains(pointerX, pointerY) ? SELECTED : TRACK);
            outline(g, box, PAPER_LINE);
            MartialArts.Technique placed = data.slot(i).map(MartialArts::technique).orElse(null);
            if (placed != null) {
                TechniqueIcons.draw(g, placed, left + box.x() + 1, top + box.y() + 1, slot - 2);
                if (box.contains(pointerX, pointerY)) hoveredText = Component.translatable(placed.translationKey());
            }
            String key = minecraft == null ? "" : CombatBarClient.slotKey(minecraft, i).getTranslatedKeyMessage().getString();
            text(g, Component.literal(key.length() > 1 ? key.substring(0, 1) : key), new Box(box.x(), box.bottom() + 1, slot, 9), MUTED, true, 1);
            barSlots.add(box);
        }
        int hintY = by + 2 * (slot + 11);
        if (side.bottom() - hintY >= 9) text(g, label("techniques.bar_hint"), new Box(side.x(), hintY, side.width(), side.bottom() - hintY), MUTED, false, 1);
    }

    /** A technique's name: its evolution's once the style has reached it. */
    private static Component techniqueName(MartialData data, MartialArts.Art art, MartialArts.Technique technique) {
        MartialArts.Evolution evolution = art.evolutionOf(technique.id());
        if (evolution != null && data.evolved(art, technique.id())) return Component.translatable(evolution.nameKey());
        return Component.translatable(technique.translationKey());
    }

    /** Why a technique is locked: its quest and how far the current step is, or the quest still to come. */
    private static Component questStatus(MartialData data, @Nullable MartialArts.Art art, MartialArts.Technique technique) {
        MartialArts.Quest quest = art == null ? null : art.questFor(technique.id());
        if (quest == null) return label("techniques.status_locked");
        if (MartialProgress.currentQuest(data, art) != quest) return label("techniques.quest_later", Component.translatable(quest.translationKey()));
        MartialArts.QuestStep step = MartialProgress.currentStep(data, quest);
        if (step == null) return Component.translatable(quest.translationKey());
        return label("techniques.quest", Component.translatable(quest.translationKey()),
                Component.translatable("quest.murimblock.step." + step.key(), data.progress(step.key()), step.goal()));
    }

    /**
     * A passive: on the left whether it is the main style's, its mastery and the style's, what it does; on the right
     * its four M1 slots (click: the next strike that fits, right-click: the previous).
     */
    private void renderPassive(GuiGraphics g, MartialData data, MartialArts.Art art, Box info, Box side) {
        MartialArts.Passive passive = art.passive();
        boolean main = data.mainArt().filter(art.id()::equals).isPresent();
        text(g, new Box(info.x(), info.y(), info.width(), 9), Component.translatable(passive.translationKey()), GOLD_LIGHT);
        if (main) {
            text(g, new Box(info.x(), info.y() + 11, info.width(), 9), label("techniques.passive_main"), AZURE_TEXT);
        } else {
            // Not the main style: a seal button makes it the main style (its passive, M1 and guard).
            Box button = new Box(info.x(), info.y() + 10, Math.min(info.width(), 96), 11);
            boolean hot = button.contains(pointerX, pointerY);
            fill(g, button, hot ? 0xFFC0473A : SEAL);
            text(g, label("techniques.make_main"), new Box(button.x(), button.y() + 1, button.width(), 9), SEAL_TEXT, true, 1);
            if (hot) hoveredText = label("techniques.passive_inactive");
            mainButton = Map.entry(button, art.id());
        }
        text(g, new Box(info.x(), info.y() + 22, info.width(), 9), label("techniques.style_mastery", data.mastery(passive.id()), data.styleMastery(art)), MUTED);
        int aboutY = info.y() + 35;
        if (info.bottom() - aboutY >= 9) about(g, Component.translatable(passive.translationKey() + ".desc"), new Box(info.x(), aboutY, info.width(), info.bottom() - aboutY));

        section(g, new Box(side.x(), side.y(), side.width(), 9), label("techniques.strikes"));
        if (passive.strikes().isEmpty()) {
            // A passive that keeps Epic Fight's strikes as they are (bare hands for the Long Fist; the weapon's own for an
            // art that has its text): nothing to choose.
            String own = "techniques.strikes_fixed." + art.id().getPath();
            text(g, label(net.minecraft.client.resources.language.I18n.exists("gui.murimblock." + own) ? own : "techniques.strikes_bare_hands"),
                    new Box(side.x(), side.y() + 14, side.width(), side.bottom() - side.y() - 14), INK, false, 1);
            return;
        }
        List<ResourceLocation> strikes = data.strikes(passive);
        int gap = 3, sw = Math.max(20, (side.width() - gap) / 2);
        for (int i = 0; i < MartialArts.Passive.M1_SLOTS; i++) {
            Box box = new Box(side.x() + (i % 2) * (sw + gap), side.y() + 14 + (i / 2) * 21, sw, 18);
            MartialArts.Strike strike = passive.strike(strikes.get(i));
            fill(g, box, main && box.contains(pointerX, pointerY) ? SELECTED : TRACK);
            outline(g, box, PAPER_LINE);
            if (strike != null) {
                int kind = switch (strike.kind()) { case LAUNCHER -> BLUE; case FINISHER -> SEAL; default -> GOLD; };
                fill(g, new Box(box.x() + 1, box.y() + 1, box.width() - 2, 2), kind);
                text(g, Component.literal((i + 1) + " ").append(Component.translatable(strike.translationKey())), new Box(box.x() + 2, box.y() + 6, box.width() - 4, 9), INK, true, 1);
                if (box.contains(pointerX, pointerY)) hoveredText = Component.literal("M" + (i + 1) + ": ").append(Component.translatable(strike.translationKey()))
                        .append(" - ").append(Component.translatable(strike.translationKey() + ".desc"));
            }
            if (main) strikeSlots.add(box);
        }
        int hintY = side.y() + 14 + 2 * 21;
        if (side.bottom() - hintY >= 9) text(g, label("techniques.strikes_hint"), new Box(side.x(), hintY, side.width(), side.bottom() - hintY), MUTED, false, 1);
    }

    /** What a technique or passive does, as far as the room allows; the whole text on hover. */
    private void about(GuiGraphics g, Component description, Box box) {
        text(g, description, box, INK_SOFT, false, 1);
        if (box.contains(pointerX, pointerY)) hoveredText = description;
    }

    /** The strike to place in an M1 slot after (or before) the one there: the next of the passive's that keeps the chain valid. */
    @Nullable static ResourceLocation cycleStrike(MartialData data, MartialArts.Passive passive, int slot, boolean forward) {
        List<MartialArts.Strike> all = passive.strikes();
        if (all.isEmpty()) return null;
        ResourceLocation current = data.strikes(passive).get(slot);
        int at = 0;
        for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(current)) at = i;
        for (int k = 1; k < all.size(); k++) {
            ResourceLocation candidate = all.get(Math.floorMod(at + (forward ? k : -k), all.size())).id();
            if (data.withStrike(slot, candidate) != data) return candidate;
        }
        return null;
    }

    /** Opens the manual of a technique (or passive) and selects it, as clicks on its cover and tile would (tests and tours). */
    public void selectForTests(ResourceLocation technique) {
        selectedTechnique = technique;
        MartialArts.Technique t = MartialArts.technique(technique);
        MartialArts.Art art = t != null ? MartialArts.art(t.art()) : MartialArts.artOfPassive(technique);
        openArt = art == null ? null : art.id();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.TECHNIQUES && minecraft != null && minecraft.player != null) {
            int mx = (int) mouseX - left, my = (int) mouseY - top;
            for (Map.Entry<Box, ResourceLocation> cover : covers) {
                if (cover.getKey().contains(mx, my)) {
                    openArt = cover.getValue();
                    selectedTechnique = null;
                    return true;
                }
            }
            if (backButton != null && backButton.contains(mx, my)) {
                openArt = null;
                return true;
            }
            if (mainButton != null && mainButton.getKey().contains(mx, my)) {
                PacketDistributor.sendToServer(new TechniquePayloads.SetMainArt(mainButton.getValue()));
                return true;
            }
            for (Map.Entry<Box, ResourceLocation> row : techniqueRows) {
                if (row.getKey().contains(mx, my)) {
                    selectedTechnique = row.getValue();
                    return true;
                }
            }
            for (int i = 0; i < strikeSlots.size(); i++) {
                if (!strikeSlots.get(i).contains(mx, my)) continue;
                MartialData data = MartialService.data(minecraft.player);
                MartialArts.Passive passive = data.passive().orElse(null);
                ResourceLocation next = passive == null ? null : cycleStrike(data, passive, i, button != 1);
                if (next != null) PacketDistributor.sendToServer(new TechniquePayloads.SetStrike(i, next));
                return true;
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

    // ---------------------------------------------------------------- Cultivation and Info

    private void renderCultivation(GuiGraphics g, Player player, CultivationSnapshot cultivation, double qi, double qiMax) {
        section(g, REALM_LABEL, label("cultivation.current"));
        text(g, realm(cultivation), layout.text(page, REALM), GOLD_LIGHT, false, 2);
        text(g, STAGE, label("cultivation.stage", stage(cultivation)));
        stagePips(g, cultivation.stageId());
        section(g, NEXT_LABEL, label("cultivation.next_stage"));
        var next = MurimblockApi.cultivation().getNextCultivation(player);
        text(g, NEXT_REALM, next.map(MurimProfileScreen::realm).orElseGet(() -> label("cultivation.complete")));
        next.ifPresent(value -> text(g, layout.text(page, NEXT_STAGE), stage(value), MUTED));
        Box required = layout.text(page, REQUIRED_QI);
        var requiredQi = CultivationService.getRequiredQiMaxForNextStage(player);
        text(g, required, requiredQi.<Component>map(value -> label("cultivation.required", QiFormat.format(value))).orElseGet(() -> label("cultivation.no_requirement")), MUTED);
        // Qi Max on its way to the next threshold, from where this stage began.
        var data = CultivationService.getCultivation(player);
        if (requiredQi.isPresent()) {
            double from = com.murimblock.cultivation.CultivationRules.floorOf(data), to = requiredQi.get();
            Box bar = new Box(required.x() + 1, required.y() + 13, required.width() - 2, 4);
            fill(g, new Box(bar.x() - 1, bar.y() - 1, bar.width() + 2, bar.height() + 2), TRACK);
            int reached = (int) Math.round(bar.width() * Math.clamp((qiMax - from) / Math.max(1, to - from), 0, 1));
            if (reached > 0) fill(g, new Box(bar.x(), bar.y(), reached, bar.height()), GOLD);
            text(g, new Box(required.x(), bar.y() + 7, required.width(), 9), label("cultivation.qi_max", QiFormat.format(Math.floor(qiMax)), QiFormat.format(to)), INK_SOFT);
        }
        section(g, QI_LABEL, label("qi_reserve"));
        qiBar(g, layout.cultivationQiFill, qi, qiMax);
        Box value = layout.text(page, QI_VALUE);
        text(g, QI_VALUE, qiValue(qi, qiMax));
        // Where one stands (how much meditation condenses here), and what is left of the day's condensation.
        var aura = com.murimblock.cultivation.MeditationService.aura(player);
        var journal = CultivationService.journal(player);
        long now = player.level().getGameTime();
        var place = aura.placeKind();
        text(g, new Box(value.x(), value.y() + 11, value.width(), 9), label("cultivation.place", label("cultivation.place." + place.key()),
                QiFormat.format(place.condensation)), MUTED);
        int left = Math.max(0, com.murimblock.cultivation.CultivationRules.DAILY_FULL_TICKS - journal.condensedOn(now));
        text(g, new Box(value.x(), value.y() + 21, value.width(), 9), left > 0 ? label("cultivation.today", (left + 1199) / 1200) : label("cultivation.tired"), MUTED);
        section(g, BREAKTHROUGH_LABEL, label("cultivation.breakthrough"));
        boolean complete = next.isEmpty();
        boolean bottleneck = !complete && com.murimblock.cultivation.CultivationRules.atBottleneck(data, qiMax);
        Box pill = layout.text(page, BREAKTHROUGH);
        if (complete) {
            fill(g, pill, FRAME);
            outline(g, pill, GOLD_DARK);
        } else if (bottleneck) {
            fill(g, pill, SEAL);
            outline(g, pill, GOLD);
        } else {
            outline(g, pill, PAPER_LINE);
        }
        Component state = complete ? label("cultivation.state.complete") : bottleneck ? label("cultivation.state.demon")
                : com.murimblock.cultivation.CultivationRules.nextIsMajor(data) ? label("cultivation.state.gate") : label("cultivation.state.minor");
        text(g, state, new Box(pill.x() + 2, pill.y() + 5, pill.width() - 4, 9), complete ? GOLD_LIGHT : bottleneck ? SEAL_TEXT : MUTED, true, 1);
        // Under it: the Heart Demon's strength at the gate, the toxicity, a pill working, the failed attempts, the realm's gifts.
        int y = pill.bottom() + 3;
        double toxicity = journal.toxicityAt(now);
        if (bottleneck) {
            double strength = com.murimblock.cultivation.CultivationRules.demonStrength(new com.murimblock.cultivation.CultivationRules.Preparation(
                    journal.breakingPillActive(now), place, journal.failures(), toxicity));
            text(g, new Box(pill.x(), y, pill.width(), 9), label("cultivation.demon_strength", Math.round(strength * 100)), SEAL);
            y += 10;
        }
        text(g, new Box(pill.x(), y, pill.width(), 9), journal.breakingPillActive(now)
                ? label("cultivation.toxicity_pill", Math.round(toxicity), (journal.breakingPillUntil() - now + 1199) / 1200)
                : label("cultivation.toxicity", Math.round(toxicity)), toxicity > com.murimblock.cultivation.CultivationRules.TOXICITY_HEAVY ? SEAL : MUTED);
        y += 10;
        if (journal.failures() > 0) {
            text(g, new Box(pill.x(), y, pill.width(), 9), label("cultivation.failures", journal.failures()), MUTED);
            y += 10;
        }
        var bonuses = com.murimblock.cultivation.CultivationRules.bonuses(data);
        if (bonuses.health() > 0 || bonuses.armor() > 0) {
            text(g, new Box(pill.x(), y, pill.width(), 9), label("cultivation.bonuses", Math.round(bonuses.health() / 2), Math.round(bonuses.armor())), INK_SOFT);
            text(g, new Box(pill.x(), y + 10, pill.width(), 9), label("cultivation.bonuses_strength",
                    Math.round(bonuses.damage() * 100), Math.round(bonuses.fist())), INK_SOFT);
        }
    }
    /** Early, Middle, Late, Peak as four marks; reached stages are red seals. */
    private void stagePips(GuiGraphics g, String stageId) {
        int reached = CultivationStage.bySerializedName(stageId).map(Enum::ordinal).orElse(-1);
        for (int i = 0; i < CultivationStage.values().length; i++) {
            Box pip = new Box(layout.stagePips.x() + i * 8, layout.stagePips.y(), 5, 5);
            if (i <= reached) {
                fill(g, pip, SEAL);
                outline(g, pip, GOLD);
            } else outline(g, pip, PAPER_LINE);
        }
    }
    private void renderInfos(GuiGraphics g, double qiMax) {
        if (settingsMode) {
            text(g, label("settings.title"), layout.settingsHeader, GOLD_LIGHT, false, 1);
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
            key(g, KEY_QI, MurimblockKeyMappings.MEDITATE);
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
        // Murim's own setting, saved in its client config rather than Epic Fight's.
        setting(6, "hud_size", () -> label("settings.hud_size." + HudSize.current().id()), () -> HudSize.set(HudSize.current().next()), null);
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
                for (int x = box.x(); x < box.right(); x += 4) fill(g, new Box(x, box.bottom() - 1, 2, 1), PAPER_LINE);
                int valueWidth = Math.min(62, box.width() / 3);
                Box labelBox = new Box(box.x() + 4, box.y() + (box.height() - 9) / 2, box.width() - valueWidth - 10, 9);
                text(g, label("settings." + name), labelBox, INK, false, 1);
                Box valueBox = new Box(box.right() - valueWidth, labelBox.y(), valueWidth - 2, 9);
                if (enabled != null) {
                    boolean on = enabled.getAsBoolean();
                    Box check = new Box(valueBox.x(), box.y() + (box.height() - 9) / 2, 9, 9);
                    fill(g, check, on ? SEAL : TRACK);
                    outline(g, check, isHoveredOrFocused() ? GOLD_LIGHT : on ? GOLD : MUTED);
                    if (on) fill(g, new Box(check.x() + 3, check.y() + 3, 3, 3), SEAL_TEXT);
                    valueBox = new Box(valueBox.x() + 13, valueBox.y(), valueBox.width() - 13, 9);
                    text(g, value.get(), valueBox, on ? AZURE_TEXT : MUTED, false, 1);
                } else {
                    outline(g, new Box(valueBox.x() - 2, valueBox.y() - 3, valueBox.width() + 4, 15), isHoveredOrFocused() ? GOLD : BLUE);
                    text(g, value.get(), valueBox, AZURE_TEXT, true, 1);
                }
            }
        });
    }
    private Button button(Box box, Supplier<Component> message, Runnable action, boolean painted) {
        return addRenderableWidget(new ManuscriptButton(box, message, action, painted));
    }
    /** A key as a small red seal. */
    private void key(GuiGraphics g, Field field, KeyMapping mapping) {
        Box box = layout.text(page, field);
        fill(g, box, SEAL);
        outline(g, box, 0xFF8E3324);
        fill(g, new Box(box.x() + 1, box.bottom() - 2, box.width() - 2, 1), 0xFF8E3324);
        text(g, mapping.getTranslatedKeyMessage(), new Box(box.x() + 2, box.y() + 4, box.width() - 4, 9), SEAL_TEXT, true, 1);
    }
    private static Component realm(CultivationSnapshot value) { return label("realm." + value.realmId()); }
    private static Component stage(CultivationSnapshot value) { return label("stage." + value.stageId()); }
    /** Whole points read better in a menu; the exact value remains available in commands. */
    static Component qiValue(double qi, double max) {
        long current = Double.isFinite(qi) ? (long) Math.floor(Math.max(0, qi)) : 0;
        long capacity = Double.isFinite(max) ? (long) Math.floor(Math.max(0, max)) : 0;
        return Component.literal(current + " / " + capacity);
    }
    /** Qi as a wash of azure ink on a dark track. */
    private void qiBar(GuiGraphics g, Box box, double qi, double max) {
        fill(g, new Box(box.x() - 1, box.y() - 1, box.width() + 2, box.height() + 2), TRACK);
        int filled = computeFilledWidth(qi, max, box.width());
        if (filled > 0) {
            fill(g, new Box(box.x(), box.y(), filled, box.height()), BLUE);
            fill(g, new Box(box.x(), box.y(), filled, 1), BLUE_LIGHT);
            fill(g, new Box(box.x() + filled - 1, box.y(), 1, box.height()), BLUE_LIGHT);
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int x = box.x() + box.width() * quarter / 4;
            fill(g, new Box(x, box.bottom() - 1, 1, 1), PAPER_LINE);
        }
    }
    /** A card on the paper: a slightly lighter sheet with cut corners. */
    private void card(GuiGraphics g, Box box) {
        fill(g, new Box(box.x() + 1, box.y(), box.width() - 2, box.height()), CARD);
        fill(g, new Box(box.x(), box.y() + 1, 1, box.height() - 2), CARD);
        fill(g, new Box(box.right() - 1, box.y() + 1, 1, box.height() - 2), CARD);
        fill(g, new Box(box.x() + 1, box.y(), box.width() - 2, 1), PAPER_LINE);
        fill(g, new Box(box.x() + 1, box.bottom() - 1, box.width() - 2, 1), PAPER_LINE);
        fill(g, new Box(box.x(), box.y() + 1, 1, box.height() - 2), PAPER_LINE);
        fill(g, new Box(box.right() - 1, box.y() + 1, 1, box.height() - 2), PAPER_LINE);
    }
    /** A dashed frame (two pixels drawn, two left). */
    private void dashed(GuiGraphics g, Box box, int color) {
        for (int x = box.x(); x < box.right(); x += 4) {
            fill(g, new Box(x, box.y(), Math.min(2, box.right() - x), 1), color);
            fill(g, new Box(x, box.bottom() - 1, Math.min(2, box.right() - x), 1), color);
        }
        for (int y = box.y(); y < box.bottom(); y += 4) {
            fill(g, new Box(box.x(), y, 1, Math.min(2, box.bottom() - y)), color);
            fill(g, new Box(box.right() - 1, y, 1, Math.min(2, box.bottom() - y)), color);
        }
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
        if (truncated) {
            // Fill every line the box holds; only the last one ends in "..." (the whole text shows on hover).
            List<net.minecraft.network.chat.FormattedText> parts = font.getSplitter().splitLines(styled.getString(), box.width() / scale, GUI_TEXT_STYLE);
            List<FormattedCharSequence> kept = new java.util.ArrayList<>();
            for (int i = 0; i < maxLines - 1; i++) kept.add(net.minecraft.locale.Language.getInstance().getVisualOrder(parts.get(i)));
            StringBuilder rest = new StringBuilder();
            for (int i = maxLines - 1; i < parts.size(); i++) rest.append(i > maxLines - 1 ? " " : "").append(parts.get(i).getString().strip());
            kept.add(fit(rest.toString(), box.width() / scale).getVisualOrderText());
            lines = kept;
        }
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
                fill(g, box, hot ? 0xFFD9614B : SEAL);
                outline(g, box, hot ? GOLD_LIGHT : GOLD);
                text(g, getMessage(), new Box(box.x() + 4, box.y() + (box.height() - 8) / 2, box.width() - 8, 9), SEAL_TEXT, true, 1);
            }
        }
    }
}
