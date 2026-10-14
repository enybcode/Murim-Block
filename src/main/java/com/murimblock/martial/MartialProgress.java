package com.murimblock.martial;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * How a student progresses in an art (docs/combat/styles/azure-cloud-sword.md, "Débloquer les compétences" and
 * "Maîtrise et évolutions"):
 * <ul>
 *   <li><b>quests</b>, one after the other: each has steps done in order, each step a counter brought to its goal;
 *   finishing the last step unlocks the quest's technique. A counter only moves while its step is the current one;</li>
 *   <li><b>mastery</b>, 0 to 100 per technique (and per passive), raised by real hits on enemies; the style's mastery
 *   is their average, and at set levels a technique evolves.</li>
 * </ul>
 * The rules are pure functions on {@link MartialData} (tested on their own); the server side tells the player.
 */
public final class MartialProgress {
    /** How many strikes landed raise the passive's mastery by one. */
    public static final int STRIKES_PER_STANCE_MASTERY = 8;

    private MartialProgress() {
    }

    // ---------------------------------------------------------------- quests

    /** The quest a student is on in an art: the first whose technique is still locked, if the previous ones are done. */
    @Nullable public static MartialArts.Quest currentQuest(MartialData data, MartialArts.Art art) {
        if (!data.knowsArt(art.id())) return null;
        for (MartialArts.Quest q : art.quests()) if (!data.hasTechnique(q.unlocks())) return q;
        return null;
    }

    /** The step of a quest being worked on: the first not at its goal. */
    @Nullable public static MartialArts.QuestStep currentStep(MartialData data, MartialArts.Quest quest) {
        for (MartialArts.QuestStep s : quest.steps()) if (data.progress(s.key()) < s.goal()) return s;
        return null;
    }

    /** What a counter's advance did: the new data, and the technique it unlocked (or null). */
    public record Advance(MartialData data, boolean moved, @Nullable MartialArts.Technique unlocked) {
    }

    /**
     * Moves a quest counter of an art by {@code amount}, if it is the current step of the current quest (capped at the
     * step's goal). When the quest's last step reaches its goal, its technique is unlocked.
     */
    public static Advance advance(MartialData data, MartialArts.Art art, String key, int amount) {
        MartialArts.Quest quest = currentQuest(data, art);
        MartialArts.QuestStep step = quest == null ? null : currentStep(data, quest);
        if (step == null || !step.key().equals(key) || amount <= 0) return new Advance(data, false, null);
        MartialData d = data.withProgress(key, Math.min(step.goal(), data.progress(key) + amount));
        if (currentStep(d, quest) != null) return new Advance(d, true, null);
        MartialArts.Technique technique = MartialArts.technique(quest.unlocks());
        return new Advance(technique == null ? d : d.unlock(technique), true, technique);
    }

    /** Moves a quest counter for a player, telling them how far they are, and what they unlocked. */
    public static void count(ServerPlayer player, String key, int amount) {
        MartialData data = MartialService.data(player);
        for (MartialArts.Art art : MartialArts.arts()) {
            Advance a = advance(data, art, key, amount);
            if (!a.moved()) continue;
            MartialService.set(player, a.data());
            if (a.unlocked() != null) {
                player.sendSystemMessage(Component.translatable("quest.murimblock.complete", Component.translatable(art.questFor(a.unlocked().id()).translationKey()),
                        Component.translatable(a.unlocked().translationKey())));
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            } else {
                MartialArts.QuestStep step = stepOf(art, key);
                if (step != null) player.displayClientMessage(Component.translatable("quest.murimblock.step." + key, a.data().progress(key), step.goal()), true);
            }
            return;
        }
    }

    @Nullable private static MartialArts.QuestStep stepOf(MartialArts.Art art, String key) {
        for (MartialArts.Quest q : art.quests()) for (MartialArts.QuestStep s : q.steps()) if (s.key().equals(key)) return s;
        return null;
    }

    // ---------------------------------------------------------------- mastery

    /** The evolutions an art reaches between two style masteries (for the message), in order. */
    public static List<MartialArts.Evolution> evolutionsReached(MartialArts.Art art, MartialData before, MartialData after) {
        int was = before.styleMastery(art), now = after.styleMastery(art);
        return art.evolutions().stream()
                .filter(e -> after.hasTechnique(e.technique()) && (was < e.styleMastery() || !before.hasTechnique(e.technique())) && now >= e.styleMastery())
                .toList();
    }

    /** Raises the mastery of a technique or a passive of an art, announcing the evolutions the style reaches. */
    public static void addMastery(ServerPlayer player, MartialArts.Art art, ResourceLocation id, int amount) {
        MartialData before = MartialService.data(player);
        if (amount <= 0 || before.mastery(id) >= MartialData.MAX_MASTERY) return;
        MartialData after = before.withMastery(id, before.mastery(id) + amount);
        MartialService.set(player, after);
        for (MartialArts.Evolution e : evolutionsReached(art, before, after)) {
            MartialArts.Technique t = MartialArts.technique(e.technique());
            player.sendSystemMessage(Component.translatable("evolution.murimblock.reached",
                    t == null ? Component.literal(e.technique().toString()) : Component.translatable(t.translationKey()), Component.translatable(e.nameKey())));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    /** A strike of the passive landed: every few of them, its mastery goes up by one. */
    public static void strikeLanded(ServerPlayer player, MartialArts.Art art) {
        MartialData data = MartialService.data(player);
        int hits = data.progress(MartialArts.STANCE_HITS) + 1;
        if (hits >= STRIKES_PER_STANCE_MASTERY) {
            MartialService.set(player, data.withProgress(MartialArts.STANCE_HITS, 0));
            addMastery(player, art, art.passive().id(), 1);
        } else {
            MartialService.set(player, data.withProgress(MartialArts.STANCE_HITS, hits));
        }
    }
}
