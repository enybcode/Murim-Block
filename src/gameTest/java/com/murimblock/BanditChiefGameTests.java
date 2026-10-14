package com.murimblock;

import com.murimblock.integration.epicfight.BanditChiefPatch;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.BanditChiefEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Bandit Chief, the mini-boss: he comes with his great axe, held in both hands (a greatsword for Epic Fight); he fights
 * with his own attacks; each of them (the cleave, the sweep, the leaping slam) can be parried; the slam's shockwave hurts
 * whoever is near and unguarded, and spares a guard that parried it.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BanditChiefGameTests {
    private BanditChiefGameTests() { }

    private static BanditChiefEntity chief(GameTestHelper helper, BlockPos at) {
        BanditChiefEntity chief = MurimEntities.BANDIT_CHIEF.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(chief != null, "No chief spawned");
        return chief;
    }

    @GameTest(template = "foundation_arena", batch = "chief_axe")
    public static void theChiefComesWithHisGreatAxeInBothHands(GameTestHelper helper) {
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 2));
        ItemStack held = chief.getItemBySlot(EquipmentSlot.MAINHAND);
        helper.assertTrue(held.is(MurimItems.CHIEF_GREAT_AXE.get()), "The chief does not hold his axe: " + held);
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(held);
        helper.assertTrue(capability.getWeaponCategory() == CapabilityItem.WeaponCategories.GREATSWORD,
                "Epic Fight does not take the axe for a two-handed weapon: " + capability.getWeaponCategory());
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(chief, LivingEntityPatch.class);
        helper.assertTrue(patch instanceof BanditChiefPatch, "No Epic Fight patch on the chief");
        helper.assertTrue(capability.getStyle(patch) == CapabilityItem.Styles.TWO_HAND, "The axe is not held in both hands: " + capability.getStyle(patch));
        helper.assertTrue(chief.getMaxHealth() == 120, "A mini-boss should have 120 health, not " + chief.getMaxHealth());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(chief.getAttributeValue(Attributes.ATTACK_DAMAGE) - 8) < 0.01,
                    "The chief should hit for 8 with his axe, not " + chief.getAttributeValue(Attributes.ATTACK_DAMAGE));
            chief.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "chief_attack", timeoutTicks = 300)
    public static void theChiefFightsWithHisOwnAttacks(GameTestHelper helper) {
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        target.setHealth(300);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        boolean[] swung = {false};
        helper.onEachTick(() -> {
            chief.setTarget(target);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(chief, LivingEntityPatch.class);
            AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main != null && main.getRealAnimation() != null && main.getRealAnimation().get() instanceof AttackAnimation) {
                var a = main.getRealAnimation();
                helper.assertTrue(a == MurimAnimations.CHIEF_CLEAVE || a == MurimAnimations.CHIEF_SWEEP || a == MurimAnimations.CHIEF_SLAM,
                        "The chief swung something else than his attacks: " + a);
                swung[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(swung[0], "The chief has not swung his axe yet");
            helper.assertTrue(target.getHealth() < 300, "The chief's axe has not struck yet");
            chief.discard();
            target.discard();
        });
    }

    @GameTest(template = "foundation_arena", batch = "chief_parry_cleave", timeoutTicks = 160)
    public static void theCleaveCanBeParried(GameTestHelper helper) {
        parry(helper, MurimAnimations.CHIEF_CLEAVE, 2.4);
    }

    @GameTest(template = "foundation_arena", batch = "chief_parry_sweep", timeoutTicks = 160)
    public static void theSweepCanBeParried(GameTestHelper helper) {
        parry(helper, MurimAnimations.CHIEF_SWEEP, 2.4);
    }

    @GameTest(template = "foundation_arena", batch = "chief_parry_slam", timeoutTicks = 160)
    public static void theSlamCanBeParried(GameTestHelper helper) {
        parry(helper, MurimAnimations.CHIEF_SLAM, 3.6);
    }

    /**
     * The chief strikes a player {@code distance} blocks in front of him twice: into a perfect parry (no damage, the
     * shockwave of a slam spared too, the chief off balance), then unguarded (it hurts: the blow does reach).
     */
    private static void parry(GameTestHelper helper, AnimationManager.AnimationAccessor<? extends StaticAnimation> attack, double distance) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 1));
        PlayerPatch<?> guard = EpicFightBridge.patch(player);
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 3));
        chief.setNoAi(true);
        BanditChiefPatch patch = EpicFightCapabilities.getEntityPatch(chief, BanditChiefPatch.class);
        boolean[] staggered = {false}, parried = {false};
        int[] tick = {0};
        helper.onEachTick(() -> {
            int t = tick[0]++;
            // Face to face, whatever the last blow did.
            if (t == 0 || t == 60) {
                player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                player.setHealth(player.getMaxHealth());
                player.invulnerableTime = 0;
                chief.moveTo(player.getX(), player.getY(), player.getZ() + distance, 180, 0);
                chief.setYHeadRot(180);
                chief.yBodyRot = 180;
            }
            if (t >= 2 && t < 50) {
                // The guard goes up two ticks before the blow, whenever it comes: a perfect parry.
                guard.startSkillHolding(MurimSkills.MURIM_GUARD.get());
                guard.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 2);
                staggered[0] |= patch.getEntityState().hurt();
                parried[0] |= MurimGuardSkill.parriedThisTick(player);
            }
            if (t == 5 || t == 61) patch.playAnimationSynchronized(attack, 0);
            if (t == 50) {
                helper.assertTrue(parried[0], "The " + attack + " was not parried (did it miss?)");
                helper.assertTrue(player.getHealth() == player.getMaxHealth(), "The " + attack + " went through a perfect parry: " + player.getHealth());
                helper.assertTrue(staggered[0], "Parrying the " + attack + " did not throw the chief off balance");
                guard.resetHolding();
            }
            if (t == 110) {
                helper.assertTrue(player.getHealth() < player.getMaxHealth(), "Unguarded, the " + attack + " did not reach the player");
                chief.discard();
                cleanup(player);
                helper.succeed();
            }
        });
    }

    @GameTest(template = "foundation_arena", batch = "chief_shockwave")
    public static void theShockwaveHurtsWhoeverIsNearUnlessTheyParry(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 1));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 3));
        chief.setNoAi(true);
        // The landing point is 1.6 ahead of him: the player stands right there, facing him.
        chief.moveTo(player.getX(), player.getY(), player.getZ() + 2.4, 180, 0);
        helper.runAfterDelay(2, () -> {
            chief.shockwave(helper.getLevel());
            helper.assertTrue(player.getHealth() < player.getMaxHealth(), "The shockwave did not hurt an unguarded player");
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;
            PlayerPatch<?> guard = EpicFightBridge.patch(player);
            guard.startSkillHolding(MurimSkills.MURIM_GUARD.get());
            guard.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 2);
            chief.shockwave(helper.getLevel());
            helper.assertTrue(player.getHealth() == player.getMaxHealth(), "The shockwave went through a perfect parry: " + player.getHealth());
            guard.resetHolding();
            chief.discard();
            cleanup(player);
            helper.succeed();
        });
    }
}
