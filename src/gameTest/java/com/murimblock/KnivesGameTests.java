package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Bishou and the Flying Knife are Epic Fight daggers: in the hand they fight with Epic Fight's own dagger moveset
 * (three quick strikes, the dash and the air slash), two of them with its dual daggers' four strikes. Their blows are
 * parried as any other (ParryEverythingGameTests.everyKnifeBlowCanBeParried).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KnivesGameTests {
    private KnivesGameTests() { }

    @GameTest(template = "foundation_arena", batch = "knives")
    public static void bothKnivesFightWithEpicFightsDaggerMoveset(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            EpicFightCombatDefaults.enforce(p);
            PlayerPatch<?> patch = EpicFightBridge.patch(p);
            for (var knife : List.of(MurimItems.BISHOU.get(), MurimItems.FLYING_KNIFE.get())) {
                ItemStack stack = new ItemStack(knife);
                helper.assertTrue(EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory() == CapabilityItem.WeaponCategories.DAGGER,
                        "Epic Fight does not take " + stack + " for a dagger");
                p.setItemInHand(InteractionHand.MAIN_HAND, stack);
                p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                List<?> chain = patch.getPrimaryItemCapability().getAutoAttackMotion(patch);
                helper.assertTrue(chain.equals(List.of(Animations.DAGGER_AUTO1, Animations.DAGGER_AUTO2, Animations.DAGGER_AUTO3,
                        Animations.DAGGER_DASH, Animations.DAGGER_AIR_SLASH)), "Not Epic Fight's dagger strikes for " + stack + ": " + chain);
                // A knife in each hand: Epic Fight's dual daggers.
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(knife));
                chain = patch.getPrimaryItemCapability().getAutoAttackMotion(patch);
                helper.assertTrue(chain.contains(Animations.DAGGER_DUAL_AUTO1) && chain.contains(Animations.DAGGER_DUAL_AUTO4),
                        "Two knives are not dual daggers for " + stack + ": " + chain);
            }
            helper.succeed();
        } finally {
            cleanup(p);
        }
    }
}
