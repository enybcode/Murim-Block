package com.murimblock.client.gui.data;

import com.murimblock.client.MurimKeys;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Quests, the System log and the guide. Quests and log are the mockup's placeholders until quests exist. */
public final class InfoData {
    public enum Group { DAILY, MAIN }

    public record Goal(String id, int done, int total) {
        public Component label() {
            return Component.translatable("murimblock.quest.goal." + id);
        }

        public boolean complete() {
            return done >= total;
        }
    }

    public record Quest(String id, Group group, String giver, String timer, List<Goal> goals, boolean warning) {
        public Component name() {
            return Component.translatable("murimblock.quest." + id);
        }

        public Component description() {
            return Component.translatable("murimblock.quest." + id + ".desc");
        }

        public Component reward() {
            return Component.translatable("murimblock.quest." + id + ".reward");
        }

        public Component warningText() {
            return Component.translatable("murimblock.quest." + id + ".warning");
        }

        public Component giverName() {
            return Component.translatable("murimblock.giver." + giver);
        }

        public int completedGoals() {
            return (int) goals.stream().filter(Goal::complete).count();
        }
    }

    public record LogEntry(String kind, Component text, String time) {
        public Component kindName() {
            return Component.translatable("gui.murimblock.log.kind." + kind);
        }
    }

    /** A control: its key and what it does. Only the System key is real for now. */
    public record Control(Component key, Component action) {
    }

    public static final List<Quest> QUESTS = List.of(
            new Quest("daily_training", Group.DAILY, "system", "14:22:05", List.of(
                    new Goal("defeat_enemies", 32, 50), new Goal("meditate_minutes", 10, 10), new Goal("travel_blocks", 1200, 5000)), true),
            new Quest("first_step", Group.MAIN, "old_master", "", List.of(
                    new Goal("reach_stage_3", 3, 3), new Goal("open_lung", 1, 1), new Goal("return_to_master", 0, 1)), false),
            new Quest("bandits_on_the_road", Group.MAIN, "village_elder", "", List.of(
                    new Goal("defeat_bandits", 4, 10)), false),
            new Quest("heart_demon", Group.MAIN, "system", "", List.of(
                    new Goal("fill_dantian", 620, 1000), new Goal("open_4_meridians", 4, 4)), true));

    public static List<LogEntry> log() {
        return List.of(
                entry("technique", "gui.murimblock.log.learned", "2 min", Component.translatable("murimblock.technique.waterfall_descent")),
                entry("quest", "gui.murimblock.log.goal_done", "9 min", Component.translatable("murimblock.quest.goal.meditate_minutes")),
                entry("meridian", "gui.murimblock.log.meridian_open", "40 min", Component.translatable("murimblock.meridian.spleen"),
                        Component.translatable("murimblock.meridian.spleen.effect")),
                entry("realm", "gui.murimblock.log.stage", "1 h", Component.translatable("murimblock.cultivation_realm.qi_condensation"), 3),
                entry("combat", "gui.murimblock.log.defeated", "1 h", Component.translatable("murimblock.mob.mountain_bandit"), 5),
                entry("art", "gui.murimblock.log.active_art", "2 h", MartialArtsData.active().name()),
                entry("warning", "gui.murimblock.log.hostile", "2 h"),
                entry("quest", "gui.murimblock.log.new_quest", "3 h", Component.translatable("murimblock.quest.bandits_on_the_road")));
    }

    private static LogEntry entry(String kind, String key, String time, Object... args) {
        return new LogEntry(kind, Component.translatable(key, args), time);
    }

    public static List<Control> controls() {
        return List.of(
                new Control(MurimKeys.OPEN_SYSTEM.getTranslatedKeyMessage(), Component.translatable("key.murimblock.open_system")),
                control("C", "gui.murimblock.guide.key.meditate"),
                control("1-4", "gui.murimblock.guide.key.techniques"),
                control("R", "gui.murimblock.guide.key.ultimate"),
                control("G", "gui.murimblock.guide.key.sense_qi"),
                control("V", "gui.murimblock.guide.key.guard"));
    }

    private static Control control(String key, String action) {
        return new Control(Component.literal(key), Component.translatable(action));
    }

    public static final List<String> TOPICS = List.of("qi", "realms", "meridians", "martial_arts", "trials");

    private InfoData() {
    }
}
