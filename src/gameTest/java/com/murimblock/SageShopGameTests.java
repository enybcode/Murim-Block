package com.murimblock;

import com.murimblock.item.MartialGuideItem;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.mob.GoldenDragonSageEntity;
import com.murimblock.mob.GoldenDragonShop;
import com.murimblock.mob.MurimEntities;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Golden Dragon's scroll of wares (Enzo, 17/10/2026): a guide to every martial art, priced by its difficulty, and
 * the crossing talismans; sold only from a scroll the player has open, near him, for its price in gold yuanbao.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SageShopGameTests {
    private SageShopGameTests() { }

    private static int index(Item item) {
        for (int i = 0; i < GoldenDragonShop.wares().size(); i++) if (GoldenDragonShop.wares().get(i).item().get().is(item)) return i;
        return -1;
    }

    @GameTest(template = "foundation_arena", batch = "sage_shop")
    public static void theScrollListsEveryArtAndEveryTalisman(GameTestHelper helper) {
        Set<ResourceLocation> guided = new HashSet<>();
        int previous = 0;
        for (GoldenDragonShop.Ware ware : GoldenDragonShop.wares()) {
            if (ware.item().get().getItem() instanceof MartialGuideItem guide) {
                guided.add(guide.art());
                helper.assertTrue(ware.forSale() && ware.price() >= previous && ware.difficulty() >= 1, "Guides not priced by difficulty: " + guide.art());
                previous = ware.price();
            }
        }
        for (MartialArts.Art art : MartialArts.arts()) {
            if (!art.id().equals(MartialArts.SANGUINE_ART)) helper.assertTrue(guided.contains(art.id()), "No guide to " + art.id());
        }
        // The Sanguine Art is on the scroll, not for sale: no one keeps it yet.
        helper.assertTrue(GoldenDragonShop.wares().stream().anyMatch(w -> !w.forSale() && w.item().get().is(MurimItems.SANGUINE_ART_SCROLL.get())), "No Sanguine line");
        for (var talisman : List.of(MurimItems.NETHER_TALISMAN, MurimItems.END_TALISMAN, MurimItems.MURIM_TALISMAN)) {
            int i = index(talisman.get());
            helper.assertTrue(i >= 0 && GoldenDragonShop.wares().get(i).price() == 32, "Talisman missing or mispriced: " + talisman.getId());
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "sage_shop")
    public static void aGuideIsSoldOnlyFromAnOpenScrollNearTheSeller(GameTestHelper helper) {
        GoldenDragonSageEntity sage = MurimEntities.GOLDEN_DRAGON_SAGE.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        ServerPlayer p = player(helper);
        try {
            p.moveTo(sage.getX() + 2, sage.getY(), sage.getZ());
            p.getInventory().clearContent();
            p.getInventory().add(new ItemStack(MurimItems.GOLD_YUANBAO.get(), 64));
            int spear = index(MurimItems.CELESTIAL_PEAK_SPEAR_GUIDE.get());
            // No scroll open: nothing sold.
            helper.assertTrue(GoldenDragonShop.buy(p, sage.getId(), spear) == GoldenDragonShop.Result.REFUSED, "Sold without the scroll open");
            GoldenDragonShop.open(p, sage);
            int sanguine = index(MurimItems.SANGUINE_ART_SCROLL.get());
            helper.assertTrue(GoldenDragonShop.buy(p, sage.getId(), sanguine) == GoldenDragonShop.Result.REFUSED
                    && p.getInventory().countItem(MurimItems.SANGUINE_ART_SCROLL.get()) == 0, "The Sanguine line sold");
            helper.assertTrue(GoldenDragonShop.buy(p, sage.getId(), 99) == GoldenDragonShop.Result.REFUSED, "A line past the end sold");
            helper.assertTrue(GoldenDragonShop.buy(p, sage.getId(), spear) == GoldenDragonShop.Result.SOLD, "Not sold");
            int price = GoldenDragonShop.wares().get(spear).price();
            helper.assertTrue(p.getInventory().countItem(MurimItems.CELESTIAL_PEAK_SPEAR_GUIDE.get()) == 1
                    && p.getInventory().countItem(MurimItems.GOLD_YUANBAO.get()) == 64 - price, "Wrong sale");
            // Walked away: no more sales.
            p.moveTo(sage.getX() + 20, sage.getY(), sage.getZ());
            helper.assertTrue(GoldenDragonShop.buy(p, sage.getId(), spear) == GoldenDragonShop.Result.REFUSED, "Sold from afar");
            // A guide read in the Overworld writes the way (none to find in a test world) and is kept.
            ItemStack guide = new ItemStack(MurimItems.CELESTIAL_PEAK_SPEAR_GUIDE.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, guide);
            helper.assertTrue(guide.use(p.level(), p, InteractionHand.MAIN_HAND).getResult().consumesAction() && !guide.isEmpty(), "Guide not read, or used up");
            helper.assertTrue(((MartialGuideItem) guide.getItem()).pages().size() == MartialGuideItem.PAGES, "Pages missing");
        } finally {
            sage.discard();
            GoldenDragonShop.close(p);
            cleanup(p);
        }
        helper.succeed();
    }
}
