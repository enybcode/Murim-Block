package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.network.DialoguePayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * A conversation with a character (DialogueService): a manuscript panel low on the screen, the speaker's name on a red
 * seal, their words written out a few letters at a time, then the answers, numbered (a click or the number key). The
 * world goes on behind it. A last page, with no answers, closes on a click.
 */
public final class DialogueScreen extends Screen {
    private static final Style TEXT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));
    private static final int PAPER = 0xF02B2721, LINE = 0xFF4A443B, GOLD = 0xFFC9A45C, GOLD_LIGHT = 0xFFF2D9A0;
    private static final int INK = 0xFFEDE3CC, MUTED = 0xFFB3A68A, SEAL = 0xFFC24A36, SEAL_TEXT = 0xFFF4E9D2, HOVER = 0xFF3A342C;
    /** Letters written per tick. */
    private static final float SPEED = 1.6F;
    private static final int ROW = 13;

    private final DialoguePayloads.Page page;
    private final String words;
    private float written;
    private boolean answered;
    private final List<int[]> optionBoxes = new ArrayList<>();

    private DialogueScreen(DialoguePayloads.Page page) {
        super(page.speaker());
        this.page = page;
        this.words = page.line().getString();
    }

    /** A page from the server: shown at once (replacing the last one). */
    public static void open(DialoguePayloads.Page page) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new DialogueScreen(page)));
    }

    /** The page on screen, if a conversation is open (for the tours). */
    public static DialoguePayloads.Page current() {
        return Minecraft.getInstance().screen instanceof DialogueScreen screen ? screen.page : null;
    }

    private static Component styled(String text) {
        return Component.literal(text).withStyle(TEXT);
    }

    private boolean done() {
        return written >= words.length();
    }

    @Override
    public void tick() {
        int before = (int) written;
        written = Math.min(words.length(), written + SPEED);
        // A soft brush sound now and then while the words come.
        if ((int) written / 12 != before / 12 && !done() && minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.6F, 0.25F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // The world stays in view: only a shade rising from the bottom, behind the panel.
        g.fillGradient(0, height - 150, width, height, 0x00000000, 0x90000000);
    }

    // ---------------------------------------------------------------- layout

    private int panelWidth() {
        return Math.min(width - 32, 420);
    }

    private List<FormattedCharSequence> lines(String text) {
        return font.split(styled(text), panelWidth() - 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int w = panelWidth();
        List<FormattedCharSequence> all = lines(words);
        List<DialoguePayloads.Option> options = page.options();
        int optionRows = done() ? Math.max(1, options.size()) : 0;
        int h = 12 + all.size() * 10 + (optionRows > 0 ? 6 + optionRows * ROW : 0) + 4;
        int x = (width - w) / 2, y = height - h - 14;
        // The paper, a gold rule, the speaker's seal.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
        g.fill(x, y, x + w, y + h, PAPER);
        g.fill(x + 6, y + 3, x + w - 6, y + 4, LINE);
        Component name = page.speaker().copy().withStyle(TEXT);
        int nameW = font.width(name) + 14;
        g.fill(x + 10, y - 10, x + 10 + nameW, y + 3, SEAL);
        g.fill(x + 10, y + 3, x + 10 + nameW, y + 4, 0xFF7A2A1E);
        g.drawString(font, name, x + 17, y - 7, SEAL_TEXT, false);
        // The words so far.
        float shown = Math.min(words.length(), written + (done() ? 0 : partial * SPEED));
        List<FormattedCharSequence> visible = lines(words.substring(0, Mth.floor(shown)));
        int ty = y + 12;
        for (FormattedCharSequence line : visible) {
            g.drawString(font, line, x + 12, ty, INK, false);
            ty += 10;
        }
        // The answers, once the words are all written.
        optionBoxes.clear();
        if (!done()) return;
        int oy = y + 12 + all.size() * 10 + 6;
        g.fill(x + 12, oy - 3, x + w - 12, oy - 2, LINE);
        if (options.isEmpty()) {
            g.drawString(font, Component.translatable("gui.murimblock.dialogue.close").withStyle(TEXT), x + 14, oy + 2, MUTED, false);
            return;
        }
        for (int i = 0; i < options.size(); i++) {
            int[] box = {x + 8, oy + i * ROW, w - 16, ROW - 1};
            optionBoxes.add(box);
            boolean hover = mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3];
            if (hover) g.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], HOVER);
            g.drawString(font, styled((i + 1) + "."), box[0] + 6, box[1] + 2, GOLD, false);
            g.drawString(font, options.get(i).label().copy().withStyle(TEXT), box[0] + 20, box[1] + 2, hover ? GOLD_LIGHT : INK, false);
        }
    }

    // ---------------------------------------------------------------- answering

    private void answer(int index) {
        if (answered || index < 0 || index >= page.options().size()) return;
        answered = true;
        if (minecraft != null) minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        PacketDistributor.sendToServer(new DialoguePayloads.Choose(page.speakerId(), page.options().get(index).id()));
    }

    /** For the tours: the words all written at once. */
    public void showAllForTests() {
        written = words.length();
    }

    /** For the tours: answers as a click would. */
    public void answerForTests(String id) {
        written = words.length();
        for (int i = 0; i < page.options().size(); i++) if (page.options().get(i).id().equals(id)) answer(i);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!done()) {
            written = words.length();
            return true;
        }
        if (page.options().isEmpty()) {
            onClose();
            return true;
        }
        for (int i = 0; i < optionBoxes.size(); i++) {
            int[] box = optionBoxes.get(i);
            if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
                answer(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            if (!done()) written = words.length();
            else answer(key - GLFW.GLFW_KEY_1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!done()) written = words.length();
            else if (page.options().isEmpty()) onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
