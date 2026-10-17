package com.murimblock.mob;

import com.murimblock.dialogue.DialogueService;
import com.murimblock.network.DialoguePayloads.Option;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the Golden Dragon in human form says (docs/mobs/golden-dragon.md), on the dialogue pages of DialogueService.
 * He greets, tells a little of who he is and of the place, gives a word on the path, speaks of the other worlds and
 * unrolls his scroll of wares (GoldenDragonShop): guides to the martial arts and crossing talismans to the Nether, the
 * End or the Murim and back, 32 gold yuanbao each (Enzo, 17/10/2026).
 */
public final class GoldenDragonSageDialogue {
    /** Remembered with the player (their saved data): they have spoken to him once. */
    static final String MET = "murimblock:met_golden_dragon_sage";
    /** What a talisman costs, in gold yuanbao. */
    public static final int PRICE = 32;

    private GoldenDragonSageDialogue() {
    }

    private static Component line(String key, Object... args) {
        return Component.translatable("dialogue.murimblock.golden_dragon_sage." + key, args);
    }

    private static Option answer(String id) {
        return DialogueService.option(id, Component.translatable("dialogue.murimblock.golden_dragon_sage.answer." + id));
    }

    private static void show(ServerPlayer player, GoldenDragonSageEntity sage, Component line, Option... options) {
        DialogueService.show(player, sage, line, List.of(options));
    }

    private static Option[] menu() {
        return new Option[]{answer("who"), answer("place"), answer("path"), answer("worlds"), answer("wares"), answer("farewell")};
    }

    /** The player spoke to him: the first words, or one of his greetings. */
    public static void greet(ServerPlayer player, GoldenDragonSageEntity sage) {
        if (!player.getPersistentData().getBoolean(MET)) {
            player.getPersistentData().putBoolean(MET, true);
            show(player, sage, line("first"), menu());
            return;
        }
        // A greeting for the day: the same all day, another tomorrow.
        int day = (int) Math.floorMod(player.level().getDayTime() / 24000L, 3L);
        show(player, sage, line("greet." + day), menu());
    }

    /** The answer chosen: the next page, or his scroll of wares unrolled. */
    public static void choose(ServerPlayer player, GoldenDragonSageEntity sage, String option) {
        switch (option) {
            case "who" -> show(player, sage, line("who"), answer("dragon"), answer("back"));
            case "dragon" -> show(player, sage, line("dragon"), answer("back"));
            case "place" -> show(player, sage, line("place"), answer("back"));
            case "path" -> show(player, sage, line("path"), answer("more_path"), answer("back"));
            case "more_path" -> show(player, sage, line("more_path"), answer("back"));
            case "worlds" -> show(player, sage, line("worlds"), answer("make"), answer("back"));
            case "make" -> show(player, sage, line("make", PRICE), answer("wares"), answer("back"));
            case "wares" -> GoldenDragonShop.open(player, sage);
            case "back" -> show(player, sage, line("menu"), menu());
            case "farewell" -> show(player, sage, line("farewell"));
            default -> { }
        }
    }
}
