package com.murimblock;

import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * The Mountain Bandit: it spawns with its Bandit Dao in hand, a sword for Epic Fight; its outfit is kept in its save; and
 * it fights with the dao through Murim's own cuts, played by Epic Fight (no vanilla punch, no generic mob swing).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MountainBanditGameTests {
    private MountainBanditGameTests() { }

    private static MountainBanditEntity bandit(GameTestHelper helper, BlockPos at) {
        MountainBanditEntity bandit = MurimEntities.MOUNTAIN_BANDIT.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(bandit != null, "No bandit spawned");
        return bandit;
    }

    @GameTest(template = "foundation_arena", batch = "bandit_dao")
    public static void aBanditSpawnsWithItsDaoASwordForEpicFight(GameTestHelper helper) {
        MountainBanditEntity bandit = bandit(helper, new BlockPos(2, 1, 2));
        ItemStack held = bandit.getItemBySlot(EquipmentSlot.MAINHAND);
        helper.assertTrue(held.is(MurimItems.BANDIT_DAO.get()), "The bandit does not hold its dao: " + held);
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(held);
        helper.assertTrue(capability.getWeaponCategory() == CapabilityItem.WeaponCategories.SWORD,
                "Epic Fight does not take the dao for a sword: " + capability.getWeaponCategory());
        helper.assertTrue(EpicFightCapabilities.getEntityPatch(bandit, LivingEntityPatch.class) != null, "No Epic Fight patch on the bandit");
        // Its own attack is nothing; the dao gives its 4 (held equipment counts from the next tick).
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(bandit.getAttributeValue(Attributes.ATTACK_DAMAGE) - 4) < 0.01,
                    "A bandit should hit for 4 with its dao, not " + bandit.getAttributeValue(Attributes.ATTACK_DAMAGE));
            bandit.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "bandit_outfit")
    public static void aBanditKeepsItsOutfitInItsSave(GameTestHelper helper) {
        MountainBanditEntity bandit = bandit(helper, new BlockPos(2, 1, 2));
        bandit.setVariant(4);
        CompoundTag saved = new CompoundTag();
        bandit.save(saved);
        MountainBanditEntity loaded = MurimEntities.MOUNTAIN_BANDIT.get().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(loaded.getVariant() == 4, "The outfit was not kept: " + loaded.getVariant());
        // Every outfit has its skin.
        for (String outfit : MountainBanditEntity.OUTFITS)
            helper.assertTrue(MountainBanditGameTests.class.getResource("/assets/murimblock/textures/entity/mountain_bandit/" + outfit + ".png") != null,
                    "No skin for the outfit " + outfit);
        bandit.discard();
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "bandit_attack", timeoutTicks = 200)
    public static void aBanditCutsWithItsDao(GameTestHelper helper) {
        MountainBanditEntity bandit = bandit(helper, new BlockPos(2, 1, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        // A helmet: no burning in the sun.
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        bandit.setTarget(target);
        boolean[] cut = {false};
        helper.onEachTick(() -> {
            bandit.setTarget(target);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(bandit, LivingEntityPatch.class);
            AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main != null && main.getRealAnimation() != null && main.getRealAnimation().get() instanceof AttackAnimation) {
                var a = main.getRealAnimation();
                // Murim's own cuts, edge first, not the engine's generic mob swings.
                helper.assertTrue(a == MurimAnimations.DRIFTING_CUT || a == MurimAnimations.RETURNING_CUT || a == MurimAnimations.FALLING_RAIN,
                        "The bandit swung something else than its cuts: " + a);
                cut[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(cut[0], "The bandit has not swung its dao yet");
            helper.assertTrue(target.getHealth() < 200, "The bandit's dao has not cut yet");
            bandit.discard();
            target.discard();
        });
    }
}
