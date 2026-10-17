package com.murimblock.mob;

import com.murimblock.dialogue.DialogueService;
import com.murimblock.item.MurimItems;
import com.murimblock.network.DialoguePayloads.Option;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What the Golden Dragon in human form says (docs/mobs/golden-dragon.md), on the dialogue pages of DialogueService.
 * He greets, tells a little of who he is and of the place, gives a word on the path, speaks of the other worlds and
 * makes, for those who can pay, a crossing talisman to one of them and back: the Nether, the End or the Murim, 32 gold
 * yuanbao each (Enzo, 17/10/2026).
 */
public final class GoldenDragonSageDialogue {
    /** Remembered with the player (their saved data): they have spoken to him once. */
    static final String MET = "murimblock:met_golden_dragon_sage";
    /** What a talisman costs, in gold yuanbao. */
    public static final int PRICE = 32;
    /** The talismans he makes, by answer id. */
    private static final List<String> WORLDS = List.of("nether", "end", "murim");

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
        return new Option[]{answer("who"), answer("place"), answer("path"), answer("worlds"), answer("farewell")};
    }

    static Supplier<Item> talisman(String world) {
        return switch (world) {
            case "nether" -> MurimItems.NETHER_TALISMAN::get;
            case "end" -> MurimItems.END_TALISMAN::get;
            default -> MurimItems.MURIM_TALISMAN::get;
        };
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

    /** The answer chosen: the next page, or the talisman made. */
    public static void choose(ServerPlayer player, GoldenDragonSageEntity sage, String option) {
        switch (option) {
            case "who" -> show(player, sage, line("who"), answer("dragon"), answer("back"));
            case "dragon" -> show(player, sage, line("dragon"), answer("back"));
            case "place" -> show(player, sage, line("place"), answer("back"));
            case "path" -> show(player, sage, line("path"), answer("more_path"), answer("back"));
            case "more_path" -> show(player, sage, line("more_path"), answer("back"));
            case "worlds" -> show(player, sage, line("worlds"), answer("make"), answer("back"));
            case "make" -> show(player, sage, line("make", PRICE), answer("nether"), answer("end"), answer("murim"), answer("back"));
            case "nether", "end", "murim" -> show(player, sage, line("offer." + option, PRICE), answer("pay_" + option), answer("make"));
            case "pay_nether", "pay_end", "pay_murim" -> pay(player, sage, option.substring(4));
            case "back" -> show(player, sage, line("menu"), menu());
            case "farewell" -> show(player, sage, line("farewell"));
            default -> { }
        }
    }

    /** Takes the price (checked again here, on the server) and gives the talisman. */
    private static void pay(ServerPlayer player, GoldenDragonSageEntity sage, String world) {
        if (!WORLDS.contains(world)) return;
        var inventory = player.getInventory();
        if (!player.getAbilities().instabuild && inventory.countItem(MurimItems.GOLD_YUANBAO.get()) < PRICE) {
            show(player, sage, line("poor", PRICE), answer("back"));
            return;
        }
        if (!player.getAbilities().instabuild) {
            int left = PRICE;
            for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
                ItemStack s = inventory.getItem(slot);
                if (!s.is(MurimItems.GOLD_YUANBAO.get())) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        ItemStack made = new ItemStack(talisman(world).get());
        if (!inventory.add(made)) player.drop(made, false);
        player.level().playSound(null, sage.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        show(player, sage, line("made." + world), answer("back"));
    }
}
