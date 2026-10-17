package com.murimblock;

import com.murimblock.alchemy.AlchemyItems;
import com.murimblock.alchemy.AlchemyKnowledge;
import com.murimblock.alchemy.AlchemyPillItem;
import com.murimblock.alchemy.AlchemyRecipes;
import com.murimblock.alchemy.PillFurnaceBlockEntity;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiService;
import com.murimblock.world.plant.HerbItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The pill furnace (docs/cultivation/alchemy.md): spring water, then the herbs in their order; a mixture one does not
 * know never starts; fed and held in each band, it makes its pills with their purity; a blaze ruins it in a residue
 * without breaking anything; the manual teaches the common recipes, a scroll a rare one; a purifying brew draws
 * toxicity off.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlchemyGameTests {
    private AlchemyGameTests() { }

    private static final BlockPos FURNACE = new BlockPos(4, 1, 4);

    private static PillFurnaceBlockEntity furnace(GameTestHelper helper) {
        helper.setBlock(FURNACE, AlchemyItems.FURNACE.get());
        return (PillFurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(FURNACE));
    }

    private static ItemStack herb(String id, int purity) {
        return HerbItem.of(AlchemyRecipes.item(id).get(), 1, "mature", purity);
    }

    /** The Qi Gathering Pill's herbs in, after the water. */
    private static void fill(PillFurnaceBlockEntity f, ServerPlayer p, int purity) {
        f.pourWater(p);
        f.addHerb(p, herb("lotus_seed_pod", purity));
        f.addHerb(p, herb("sea_buckthorn_berries", purity));
        f.addHerb(p, herb("chrysanthemum_flower", purity));
    }

    /** Holds the heat at `heat` for `ticks` ticks. */
    private static void hold(GameTestHelper helper, PillFurnaceBlockEntity f, int heat, int ticks) {
        for (int i = 0; i < ticks; i++) {
            f.setHeatForTests(heat);
            f.tickForTests(helper.getLevel());
        }
    }

    private static List<ItemStack> dropsNear(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FURNACE)).inflate(3)).stream().map(ItemEntity::getItem).toList();
    }

    private static void clearDrops(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FURNACE)).inflate(4)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_order")
    public static void waterFirstThenTheHerbsInTheirOrder(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        PillFurnaceBlockEntity f = furnace(helper);
        helper.assertTrue(f.addHerb(p, herb("lotus_seed_pod", 70)) != null, "A herb went in before the water");
        f.pourWater(p);
        helper.assertTrue(f.addHerb(p, herb("chrysanthemum_flower", 70)) != null, "The harmonizer went in first");
        helper.assertTrue(f.addHerb(p, herb("lotus_seed_pod", 70)) == null, "The principal did not go in");
        helper.assertTrue(f.herbs().size() == 1, "Herbs in: " + f.herbs().size());
        cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_unknown")
    public static void anUnknownMixtureNeverStarts(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        AlchemyKnowledge.forgetAllForTests(p);
        PillFurnaceBlockEntity f = furnace(helper);
        fill(f, p, 70);
        f.addFuel(new ItemStack(Items.CHARCOAL));
        hold(helper, f, 650, 40);
        helper.assertTrue(f.phase() == PillFurnaceBlockEntity.PREPARE, "It started on a recipe its brewer does not know: phase " + f.phase());
        cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_brew", timeoutTicks = 200)
    public static void heldInEveryBandItMakesItsPills(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        AlchemyKnowledge.forgetAllForTests(p);
        AlchemyKnowledge.learn(p, AlchemyRecipes.byId("qi_gathering_pill"));
        QiService.setQiMax(p, 200);
        QiService.setQi(p, 200);
        p.moveTo(helper.absolutePos(FURNACE).getCenter().add(1.5, 0, 0));
        PillFurnaceBlockEntity f = furnace(helper);
        clearDrops(helper);
        fill(f, p, 90);
        f.addFuel(new ItemStack(Items.CHARCOAL));
        AlchemyRecipes.Recipe r = AlchemyRecipes.byId("qi_gathering_pill");
        hold(helper, f, 650, r.extract().hold() + 2);
        helper.assertTrue(f.phase() == PillFurnaceBlockEntity.STABILIZE, "Extraction not done: phase " + f.phase());
        hold(helper, f, 440, r.stabilize().hold() + 1);
        helper.assertTrue(f.phase() == PillFurnaceBlockEntity.REFINE, "Stabilization not done: phase " + f.phase());
        // Refining without Qi infused holds nothing.
        hold(helper, f, 550, 20);
        helper.assertTrue(f.heldForTests() == 0, "Refining went on without Qi");
        f.setInfusing(p, true);
        double qiBefore = QiService.getQi(p);
        hold(helper, f, 550, r.refine().hold() + 1);
        helper.assertTrue(QiService.getQi(p) < qiBefore, "The refining drew no Qi");
        helper.assertTrue(f.phase() == PillFurnaceBlockEntity.PREPARE && f.herbs().isEmpty(), "The furnace did not finish and empty");
        hold(helper, f, 400, 25);
        List<ItemStack> drops = dropsNear(helper);
        ItemStack pills = drops.stream().filter(s -> s.is(r.resultItem())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(pills.getCount() >= 2, "No Qi Gathering Pills came out: " + drops);
        helper.assertTrue(AlchemyPillItem.purity(pills) >= 75, "Pure herbs and a steady hand made pills of purity " + AlchemyPillItem.purity(pills));
        clearDrops(helper);
        cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_blast")
    public static void aBlazeRuinsItInAResidueAndBreaksNothing(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        AlchemyKnowledge.learn(p, AlchemyRecipes.byId("qi_gathering_pill"));
        helper.setBlock(FURNACE.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(FURNACE.east(), net.minecraft.world.level.block.Blocks.STONE);
        PillFurnaceBlockEntity f = furnace(helper);
        clearDrops(helper);
        fill(f, p, 70);
        f.addFuel(new ItemStack(Items.CHARCOAL));
        hold(helper, f, 650, 10);
        hold(helper, f, PillFurnaceBlockEntity.BLAST + 10, 4);
        helper.assertTrue(f.herbs().isEmpty() && f.phase() == PillFurnaceBlockEntity.PREPARE, "The blaze did not end the work");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(FURNACE)).is(AlchemyItems.FURNACE.get()), "The blast broke the furnace");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(FURNACE.below())).is(net.minecraft.world.level.block.Blocks.STONE)
                && helper.getLevel().getBlockState(helper.absolutePos(FURNACE.east())).is(net.minecraft.world.level.block.Blocks.STONE), "The blast broke the stone round it");
        hold(helper, f, 300, 4);
        helper.assertTrue(dropsNear(helper).stream().anyMatch(s -> s.is(AlchemyItems.PILL_RESIDUE.get())), "No residue: " + dropsNear(helper));
        clearDrops(helper);
        cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_books")
    public static void theManualTeachesTheCommonOnesAndAScrollARareOne(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        AlchemyKnowledge.forgetAllForTests(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlchemyItems.MANUAL.get()));
        p.getMainHandItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(AlchemyKnowledge.knows(p, AlchemyRecipes.byId("qi_gathering_pill")) && AlchemyKnowledge.knows(p, AlchemyRecipes.byId("purifying_brew")), "The manual taught nothing");
        helper.assertTrue(!AlchemyKnowledge.knows(p, AlchemyRecipes.byId("martial_elixir")), "The manual taught a rare recipe");
        helper.assertTrue(!p.getMainHandItem().isEmpty(), "The manual was used up");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlchemyItems.MARTIAL_ELIXIR_SCROLL.get()));
        p.getMainHandItem().use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
        helper.assertTrue(AlchemyKnowledge.knows(p, AlchemyRecipes.byId("martial_elixir")), "The scroll taught nothing");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "The scroll was not used up");
        cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "alchemy_brew_purify")
    public static void aPurifyingBrewDrawsToxicityOff(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        long now = p.level().getGameTime();
        CultivationService.setJournal(p, CultivationService.journal(p).withToxicity(60, now));
        ((AlchemyPillItem) AlchemyItems.PURIFYING_BREW.get()).take(p, 80);
        double after = CultivationService.journal(p).toxicityAt(now);
        helper.assertTrue(after < 40, "Toxicity after the brew: " + after);
        cleanup(p);
        helper.succeed();
    }
}
