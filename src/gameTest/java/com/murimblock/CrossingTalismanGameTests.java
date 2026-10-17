package com.murimblock;

import com.murimblock.dialogue.DialogueService;
import com.murimblock.item.CrossingTalismanItem;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.GoldenDragonSageDialogue;
import com.murimblock.mob.GoldenDragonSageEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Golden Dragon in human form talks and makes crossing talismans for 32 gold yuanbao; a talisman carries its
 * bearer to its world, remembers where they stood, carries them back, and crumbles (Enzo, 17/10/2026).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrossingTalismanGameTests {
    private CrossingTalismanGameTests() { }

    private static String lastKey(ServerPlayer p) {
        var page = DialogueService.last(p);
        return page != null && page.line().getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(page);
    }

    @GameTest(template = "foundation_arena", batch = "sage_dialogue")
    public static void theSageTalksAndSellsATalismanForThirtyTwoYuanbao(GameTestHelper helper) {
        GoldenDragonSageEntity sage = MurimEntities.GOLDEN_DRAGON_SAGE.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        ServerPlayer p = player(helper);
        try {
            p.moveTo(sage.getX() + 2, sage.getY(), sage.getZ());
            p.getInventory().clearContent();
            sage.interact(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(lastKey(p).endsWith("golden_dragon_sage.first"), "No first words: " + lastKey(p));
            for (String step : new String[]{"who", "dragon", "back", "path", "more_path", "back", "worlds", "make", "nether"}) {
                DialogueService.choose(p, sage.getId(), step);
            }
            helper.assertTrue(lastKey(p).endsWith("offer.nether"), "Not the Nether offer: " + lastKey(p));
            DialogueService.choose(p, sage.getId(), "pay_nether");
            helper.assertTrue(lastKey(p).endsWith("poor") && p.getInventory().countItem(MurimItems.NETHER_TALISMAN.get()) == 0, "Made without being paid");
            // Paid: 32 yuanbao (in two stacks) go, the talisman comes.
            p.getInventory().add(new ItemStack(MurimItems.GOLD_YUANBAO.get(), 20));
            p.getInventory().add(new ItemStack(MurimItems.GOLD_YUANBAO.get(), 20));
            for (String step : new String[]{"back", "worlds", "make", "nether", "pay_nether"}) DialogueService.choose(p, sage.getId(), step);
            helper.assertTrue(lastKey(p).endsWith("made.nether"), "Not made: " + lastKey(p));
            helper.assertTrue(p.getInventory().countItem(MurimItems.NETHER_TALISMAN.get()) == 1, "No talisman given");
            helper.assertTrue(p.getInventory().countItem(MurimItems.GOLD_YUANBAO.get()) == 40 - GoldenDragonSageDialogue.PRICE,
                    "Wrong price taken: " + p.getInventory().countItem(MurimItems.GOLD_YUANBAO.get()) + " left");
        } finally {
            sage.discard();
            cleanup(p);
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "crossing_talisman", timeoutTicks = 200)
    public static void aTalismanCrossesOverAndBackThenCrumbles(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            Vec3 start = p.position();
            ItemStack stack = new ItemStack(MurimItems.NETHER_TALISMAN.get(), 2);
            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
            CrossingTalismanItem talisman = MurimItems.NETHER_TALISMAN.get();
            talisman.finishUsingItem(stack, p.level(), p);
            helper.assertTrue(p.level().dimension() == Level.NETHER, "Not carried to the Nether: " + p.level().dimension());
            helper.assertTrue(p.level().getBlockState(p.blockPosition()).isAir() && !p.level().getBlockState(p.blockPosition().below()).isAir(),
                    "Set down in a wall or over the void at " + p.blockPosition());
            // One of the two went along and remembers the way back; the other stays as it was.
            ItemStack bound = null;
            for (ItemStack s : p.getInventory().items) if (s.is(talisman) && CrossingTalismanItem.bound(s)) bound = s;
            helper.assertTrue(bound != null && bound.getCount() == 1 && stack.getCount() == 1 && !CrossingTalismanItem.bound(stack), "The bound talisman is not one of the stack");
            talisman.finishUsingItem(bound, p.level(), p);
            helper.assertTrue(p.level().dimension() == Level.OVERWORLD && p.position().distanceTo(start) < 0.01, "Not carried back to the start: " + p.position() + " in " + p.level().dimension());
            helper.assertTrue(bound.isEmpty(), "The talisman did not crumble");
            // In its own world, an unused Murim talisman does nothing.
            ItemStack home = new ItemStack(MurimItems.MURIM_TALISMAN.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, home);
            helper.assertTrue(!home.use(p.level(), p, InteractionHand.MAIN_HAND).getResult().consumesAction() && p.level().dimension() == Level.OVERWORLD, "Used in its own world");
        } finally {
            cleanup(p);
        }
        helper.succeed();
    }
}
