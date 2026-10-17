package com.murimblock.mob;

import com.murimblock.item.MurimItems;
import com.murimblock.network.ScrollShopPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Golden Dragon's wares, shown on a hanging scroll (client/ScrollShopScreen), Enzo, 17/10/2026: a guide to every
 * martial art that can be won, priced by its difficulty, and the crossing talismans, 32 gold yuanbao each. The arts no
 * one in the Murim keeps yet (the Sanguine Art, the Thunder Saber) are on the scroll but not for sale. The price is checked and taken
 * here, on the server; only a scroll the player has open, from a seller still near, sells.
 */
public final class GoldenDragonShop {
    /** A ware: what is given, its price in gold yuanbao, its difficulty (ink drops, 0: none shown), whether it is sold. */
    public record Ware(Supplier<ItemStack> item, Component name, int price, int difficulty, boolean forSale) { }

    /** How near the seller must still be when a ware is bought. */
    public static final double REACH = 8;

    /** A guide's line, under its art's name (the book shows it is a guide). */
    private static Ware guide(Supplier<? extends com.murimblock.item.MartialGuideItem> item, int difficulty, int price) {
        return new Ware(() -> new ItemStack(item.get()), Component.translatable("art.murimblock." + item.get().art().getPath()), price, difficulty, true);
    }

    /** An art no one keeps yet: on the scroll under its name, with its manual's icon, not for sale. */
    private static Ware unwritten(Supplier<? extends net.minecraft.world.item.Item> manual) {
        String art = ((com.murimblock.martial.MartialManualItem) manual.get()).art().getPath();
        return new Ware(() -> new ItemStack(manual.get()), Component.translatable("art.murimblock." + art), 0, 0, false);
    }

    private static Ware talisman(Supplier<? extends net.minecraft.world.item.Item> item) {
        return new Ware(() -> new ItemStack(item.get()), Component.translatable(item.get().getDescriptionId()), GoldenDragonSageDialogue.PRICE, 0, true);
    }

    /** The scroll's lines, in order (built on first use: the items are registered by then). */
    private static List<Ware> wares;

    public static List<Ware> wares() {
        if (wares == null) {
            wares = List.of(
                    guide(MurimItems.LONG_FIST_GUIDE, 1, 3),
                    guide(MurimItems.GOLDEN_BELL_GUIDE, 1, 3),
                    guide(MurimItems.TRAVELLER_STAFF_GUIDE, 1, 3),
                    guide(MurimItems.CELESTIAL_PEAK_SPEAR_GUIDE, 2, 6),
                    guide(MurimItems.AZURE_CLOUD_SWORD_GUIDE, 3, 10),
                    guide(MurimItems.MOUNTAIN_SHATTERING_FIST_GUIDE, 4, 16),
                    guide(MurimItems.SEVEN_STARS_SWORD_GUIDE, 4, 20),
                    guide(MurimItems.MAJIN_FIST_GUIDE, 4, 20),
                    unwritten(MurimItems.SANGUINE_ART_SCROLL),
                    unwritten(com.murimblock.martial.ThunderSaberItems.MANUAL),
                    talisman(MurimItems.NETHER_TALISMAN),
                    talisman(MurimItems.END_TALISMAN),
                    talisman(MurimItems.MURIM_TALISMAN));
        }
        return wares;
    }

    /** The scroll open for each player: the seller's entity id. */
    private static final Map<UUID, Integer> OPEN = new HashMap<>();

    private GoldenDragonShop() {
    }

    /** Unrolls the seller's scroll before the player. */
    public static void open(ServerPlayer player, Entity seller) {
        OPEN.put(player.getUUID(), seller.getId());
        List<ScrollShopPayloads.Ware> lines = new ArrayList<>();
        for (Ware w : wares()) lines.add(new ScrollShopPayloads.Ware(w.item().get(), w.name(), w.price(), w.difficulty(), w.forSale()));
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new ScrollShopPayloads.Open(seller.getId(), lines));
    }

    /** What came of a purchase (the tests read it). */
    public enum Result { SOLD, POOR, REFUSED }

    /** The player bought the ware at this line of the scroll they have open. */
    public static Result buy(ServerPlayer player, int sellerId, int index) {
        Integer open = OPEN.get(player.getUUID());
        if (open == null || open != sellerId || index < 0 || index >= wares().size()) return Result.REFUSED;
        Entity seller = player.level().getEntity(sellerId);
        if (!(seller instanceof GoldenDragonSageEntity) || !seller.isAlive() || seller.distanceTo(player) > REACH) return Result.REFUSED;
        Ware ware = wares().get(index);
        if (!ware.forSale()) return Result.REFUSED;
        var inventory = player.getInventory();
        boolean free = player.getAbilities().instabuild;
        if (!free && inventory.countItem(MurimItems.GOLD_YUANBAO.get()) < ware.price()) {
            player.displayClientMessage(Component.translatable("message.murimblock.sage_shop.poor", ware.price()), true);
            player.level().playSound(null, seller.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
            return Result.POOR;
        }
        if (!free) {
            int left = ware.price();
            for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
                ItemStack s = inventory.getItem(slot);
                if (!s.is(MurimItems.GOLD_YUANBAO.get())) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        ItemStack bought = ware.item().get();
        if (!inventory.add(bought)) player.drop(bought, false);
        player.level().playSound(null, seller.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.murimblock.sage_shop.sold", ware.name()), true);
        return Result.SOLD;
    }

    public static void close(ServerPlayer player) {
        OPEN.remove(player.getUUID());
    }
}
