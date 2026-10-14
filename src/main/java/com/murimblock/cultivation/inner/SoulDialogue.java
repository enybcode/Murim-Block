package com.murimblock.cultivation.inner;

import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationJournal;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.dialogue.DialogueService;
import com.murimblock.network.DialoguePayloads.Option;
import com.murimblock.qi.QiFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the remnant soul of the old master says (docs/cultivation/qi-cultivation.md). He greets the cultivator, tells who
 * he is and where they are, gives the day's missions, advises them on their path, and, when their Qi presses against
 * the gate of the next realm, offers them the trial: to face themselves, their Heart Demon, with all their strength.
 * Each page is a line and the answers the player may give (DialogueService).
 */
public final class SoulDialogue {
    /** Remembered with the player (their saved data): they have met the old master. */
    static final String MET = "murimblock:met_remnant_soul";

    private SoulDialogue() {
    }

    private static Component line(String key, Object... args) {
        return Component.translatable("dialogue.murimblock.soul." + key, args);
    }

    private static Option answer(String id) {
        return DialogueService.option(id, Component.translatable("dialogue.murimblock.soul.answer." + id));
    }

    private static void show(ServerPlayer player, RemnantSoulEntity soul, Component line, Option... options) {
        DialogueService.show(player, soul, line, List.of(options));
    }

    /** The player spoke to the soul: the first words of a conversation. */
    public static void greet(ServerPlayer player, RemnantSoulEntity soul) {
        if (!InnerWorldService.isInside(player) || !InnerWorldService.session(player).active()) return;
        if (InnerWorldService.trialUnderway(player)) {
            show(player, soul, line("trial.now"));
            return;
        }
        InnerMissions.Mission running = InnerMissions.running(player);
        if (running != null) {
            show(player, soul, line("busy." + running.id), answer("resume"), answer("give_up"));
            return;
        }
        if (!player.getPersistentData().getBoolean(MET)) {
            player.getPersistentData().putBoolean(MET, true);
            show(player, soul, line("first"), answer("who"), answer("where"));
            return;
        }
        if (atGate(player)) {
            show(player, soul, line("gate", realm(player)), answer("beyond"), answer("ready"), answer("not_yet"));
            return;
        }
        CultivationData data = CultivationService.getCultivation(player);
        show(player, soul, line("greet", Component.literal(data.stage().displayName()), realm(player)), menu());
    }

    private static Option[] menu() {
        return new Option[]{answer("train"), answer("advice"), answer("who"), answer("farewell")};
    }

    private static boolean atGate(ServerPlayer player) {
        return InnerWorldService.session(player).breakthrough();
    }

    private static Component realm(ServerPlayer player) {
        return Component.literal(CultivationService.getRealm(player).displayName());
    }

    private static Component nextRealm(ServerPlayer player) {
        return Component.literal(CultivationService.getRealm(player).next().map(CultivationRealm::displayName).orElse("?"));
    }

    /** The answer chosen: the next page, or what it sets going. */
    public static void choose(ServerPlayer player, RemnantSoulEntity soul, String option) {
        if (!InnerWorldService.isInside(player) || !InnerWorldService.session(player).active()) return;
        boolean gate = atGate(player);
        switch (option) {
            case "who" -> show(player, soul, line("who"), answer("past"), answer("back"));
            case "past" -> show(player, soul, line("past"), answer("back"));
            case "where" -> show(player, soul, line("where"), answer("who"), answer("back"));
            case "back" -> {
                if (gate) show(player, soul, line("gate.again", realm(player)), answer("beyond"), answer("ready"), answer("not_yet"));
                else show(player, soul, line("menu"), menu());
            }
            case "farewell" -> show(player, soul, line("farewell"));
            case "train" -> train(player, soul);
            case "storm", "sparring" -> {
                if (gate || InnerMissions.running(player) != null) return;
                InnerMissions.Mission mission = option.equals("storm") ? InnerMissions.Mission.STORM : InnerMissions.Mission.SPARRING;
                if (CultivationService.journal(player).missionDone(mission.id, player.level().getGameTime())) return;
                show(player, soul, line("begin." + mission.id));
                InnerMissions.start(player, mission);
            }
            case "later" -> show(player, soul, line("menu"), menu());
            case "advice" -> show(player, soul, advice(player), answer("more_advice"), answer("back"));
            case "more_advice" -> show(player, soul, line("advice.roads"), answer("back"));
            case "resume" -> show(player, soul, line("resume"));
            case "give_up" -> {
                InnerMissions.abandon(player);
                show(player, soul, line("given_up"), answer("back"));
            }
            // At the gate: the trial.
            case "beyond" -> show(player, soul, line("beyond", nextRealm(player)), answer("myself"), answer("not_yet"));
            case "myself" -> show(player, soul, line("myself"), answer("prepare"), answer("ready"), answer("not_yet"));
            case "prepare" -> show(player, soul, line("prepare", Math.round(InnerWorldService.session(player).strength() * 100)),
                    answer("ready"), answer("not_yet"));
            case "ready" -> show(player, soul, line("warn"), answer("summon"), answer("not_yet"));
            case "summon" -> {
                if (!gate || InnerWorldService.trialUnderway(player)) return;
                show(player, soul, line("trial.begin"));
                InnerWorldService.beginTrial(player);
            }
            case "not_yet" -> show(player, soul, line("not_yet"));
            default -> { }
        }
    }

    /** The day's next mission, or none left today (and none at the gate: the Qi can grow no more in this realm). */
    private static void train(ServerPlayer player, RemnantSoulEntity soul) {
        if (atGate(player)) {
            show(player, soul, line("train.gate"), answer("back"));
            return;
        }
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        for (InnerMissions.Mission mission : InnerMissions.Mission.values()) {
            if (journal.missionDone(mission.id, now)) continue;
            double reward = CultivationRules.missionReward(CultivationService.getRealm(player), mission.share);
            show(player, soul, line("train." + mission.id, QiFormat.format(reward)), answer(mission.id), answer("later"));
            return;
        }
        show(player, soul, line("train.none"), answer("back"));
    }

    /** One piece of advice, the one this cultivator needs most: their pills, the place their body sits, or the roads up. */
    private static Component advice(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (CultivationService.journal(player).toxicityAt(now) > CultivationRules.TOXICITY_HEAVY) return line("advice.toxic");
        List<String> keys = new ArrayList<>();
        // Where the body sits, then how near the next realm is.
        if (InnerWorldService.bodyPlace(player) == CultivationRules.Place.ORDINARY) keys.add("advice.place");
        if (CultivationRules.nextIsMajor(CultivationService.getCultivation(player))) keys.add("advice.gate");
        if (keys.isEmpty()) keys.add("advice.roads");
        return line(keys.get(0));
    }
}
