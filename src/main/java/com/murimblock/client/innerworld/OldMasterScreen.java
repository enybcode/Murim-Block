package com.murimblock.client.innerworld;

import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.CultivationData;
import com.murimblock.innerworld.OldMaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/**
 * Talking with the Old Master: a System panel at the bottom of the screen, his words written letter by letter, then
 * numbered answers (click or keys 1-9). Client side only: he says nothing the client does not already know.
 */
public final class OldMasterScreen extends Screen {
    private enum Topic {
        GREET, WHO, PLACE, GROW, TRIAL
    }

    private static final Topic[] QUESTIONS = {Topic.WHO, Topic.PLACE, Topic.GROW, Topic.TRIAL};
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 112;
    private static final int PAD = 12;
    private static final int LINE = 11;
    private static final float LETTERS_PER_TICK = 1.6f;
    /** Walking farther than this ends the talk. */
    private static final double RANGE = 8;

    private final OldMaster master;
    private Topic topic = Topic.GREET;
    private Component words = Component.empty();
    private float shown;
    private Rect panel;

    public OldMasterScreen(OldMaster master) {
        super(master.getName());
        this.master = master;
    }

    @Override
    protected void init() {
        int w = Math.min(PANEL_WIDTH, width - 32);
        panel = new Rect((width - w) / 2, height - PANEL_HEIGHT - 28, w, PANEL_HEIGHT);
        say(topic);
    }

    private void say(Topic next) {
        topic = next;
        words = next == Topic.GROW ? Component.translatable("murimblock.old_master.grow", advice())
                : Component.translatable("murimblock.old_master." + key(next));
        shown = 0;
    }

    /** The most useful piece of advice for where the player stands now. */
    private static Component advice() {
        if (CultivationData.trialReady()) {
            return Component.translatable("murimblock.old_master.advice.trial");
        }
        if (CultivationData.qi() >= CultivationData.qiToFill()) {
            return Component.translatable("murimblock.old_master.advice.advance");
        }
        if (CultivationData.openMeridians() < CultivationData.meridiansForTrial()) {
            return Component.translatable("murimblock.old_master.advice.meridians",
                    CultivationData.openMeridians(), CultivationData.meridiansForTrial());
        }
        return Component.translatable("murimblock.old_master.advice.breathe");
    }

    /** Every question but the one just answered. */
    private List<Topic> questions() {
        return Arrays.stream(QUESTIONS).filter(q -> q != topic).toList();
    }

    /** The questions, then farewell. */
    private List<Component> answers() {
        List<Component> answers = new ArrayList<>();
        for (Topic q : questions()) {
            answers.add(Component.translatable("murimblock.old_master.ask." + key(q)));
        }
        answers.add(Component.translatable("murimblock.old_master.ask.farewell"));
        return answers;
    }

    private void choose(int index) {
        List<Topic> questions = questions();
        if (index < questions.size()) {
            say(questions.get(index));
        } else if (index == questions.size()) {
            onClose();
        }
    }

    private static String key(Topic topic) {
        return topic.name().toLowerCase(Locale.ROOT);
    }

    private boolean writing() {
        return shown < words.getString().length();
    }

    @Override
    public void tick() {
        if (!master.isAlive() || minecraft.player == null || minecraft.player.distanceTo(master) > RANGE) {
            onClose();
            return;
        }
        shown += LETTERS_PER_TICK;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        SystemTheme.window(g, panel);
        SystemTheme.titleTag(g, font, title, panel.x() + 60, panel.y());
        int x = panel.x() + PAD;
        int y = panel.y() + PAD + 4;
        String all = words.getString();
        String part = all.substring(0, Math.min(all.length(), (int) shown));
        for (FormattedCharSequence line : font.split(Component.literal(part), panel.width() - 2 * PAD)) {
            g.drawString(font, line, x, y, SystemTheme.TEXT, false);
            y += LINE;
        }
        if (writing()) {
            return;
        }
        List<Component> answers = answers();
        int top = panel.bottom() - PAD - answers.size() * LINE + 2;
        for (int i = 0; i < answers.size(); i++) {
            int ay = top + i * LINE;
            boolean hovered = hovered(i, answers.size(), mouseX, mouseY);
            Component text = Component.literal((i + 1) + ". ").append(answers.get(i));
            if (hovered) {
                g.fill(x - 3, ay - 2, panel.right() - PAD + 3, ay + LINE - 2, SystemTheme.CYAN_FILL);
                SystemTheme.glowText(g, font, text, x, ay, SystemTheme.TEXT);
            } else {
                g.drawString(font, text, x, ay, SystemTheme.CYAN, false);
            }
        }
    }

    private boolean hovered(int i, int count, double mouseX, double mouseY) {
        int ay = panel.bottom() - PAD - count * LINE + 2 + i * LINE;
        return mouseX >= panel.x() + PAD - 3 && mouseX < panel.right() - PAD + 3 && mouseY >= ay - 2 && mouseY < ay + LINE - 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (writing()) {
            shown = words.getString().length();
            return true;
        }
        int count = answers().size();
        for (int i = 0; i < count; i++) {
            if (hovered(i, count, mouseX, mouseY)) {
                choose(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            if (writing()) {
                shown = words.getString().length();
            } else {
                choose(key - GLFW.GLFW_KEY_1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE && writing()) {
            shown = words.getString().length();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
