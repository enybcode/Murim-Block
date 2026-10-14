package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MountainBanditPatch;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
import yesman.epicfight.api.animation.LivingMotions;
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

    @GameTest(template = "foundation_arena", batch = "bandit_idle")
    public static void aBanditStandsInItsOwnIdle(GameTestHelper helper) {
        MountainBanditEntity bandit = bandit(helper, new BlockPos(2, 1, 2));
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(bandit, LivingEntityPatch.class);
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            helper.assertTrue(idle == MurimAnimations.BANDIT_IDLE, "With its dao, the bandit should stand in its own idle, not " + idle);
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

    /*
     * Every cut of the bandit can be parried: a player in front of it, the Murim guard raised just in time, takes none of
     * the cut and throws the bandit off balance. The same cut, unguarded, does hurt: it reaches the player.
     */
    @GameTest(template = "foundation_arena", batch = "bandit_parry_level", timeoutTicks = 120)
    public static void theLevelCutCanBeParried(GameTestHelper helper) {
        parryCut(helper, MurimAnimations.DRIFTING_CUT);
    }

    @GameTest(template = "foundation_arena", batch = "bandit_parry_backhand", timeoutTicks = 120)
    public static void theBackhandCanBeParried(GameTestHelper helper) {
        parryCut(helper, MurimAnimations.RETURNING_CUT);
    }

    @GameTest(template = "foundation_arena", batch = "bandit_parry_falling", timeoutTicks = 120)
    public static void theFallingCutCanBeParried(GameTestHelper helper) {
        parryCut(helper, MurimAnimations.FALLING_RAIN);
    }

    /**
     * The bandit cuts a player 1.6 blocks in front of it twice: into a perfect parry (no damage, the bandit off
     * balance), then unguarded (it hurts: the cut does reach). The parry first: the falling cut knocks an unguarded
     * player down, and nothing hits a player lying on the ground.
     */
    private static void parryCut(GameTestHelper helper, AnimationManager.AnimationAccessor<? extends StaticAnimation> cut) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 1));
        PlayerPatch<?> guard = EpicFightBridge.patch(player);
        MountainBanditEntity bandit = bandit(helper, new BlockPos(2, 1, 2));
        bandit.setNoAi(true);
        MountainBanditPatch banditPatch = EpicFightCapabilities.getEntityPatch(bandit, MountainBanditPatch.class);
        boolean[] staggered = {false}, parried = {false};
        int[] tick = {0};
        helper.onEachTick(() -> {
            int t = tick[0]++;
            // Face to face, 1.6 blocks apart, whatever the last cut did.
            if (t == 0 || t == 40) {
                player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                bandit.moveTo(player.getX(), player.getY(), player.getZ() + 1.6, 180, 0);
                bandit.setYHeadRot(180);
                bandit.yBodyRot = 180;
            }
            if (t >= 2 && t < 34) {
                // The guard goes up two ticks before the hit, whenever it comes: a perfect parry.
                guard.startSkillHolding(MurimSkills.MURIM_GUARD.get());
                guard.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 2);
                staggered[0] |= banditPatch.getEntityState().hurt();
                parried[0] |= MurimGuardSkill.parriedThisTick(player);
            }
            if (t == 5 || t == 41) banditPatch.playAnimationSynchronized(cut, 0);
            if (t == 34) {
                helper.assertTrue(parried[0], "The " + cut + " was not parried (did it miss?)");
                helper.assertTrue(player.getHealth() == player.getMaxHealth(), "The " + cut + " went through a perfect parry: " + player.getHealth());
                helper.assertTrue(staggered[0], "Parrying the " + cut + " did not throw the bandit off balance");
                guard.resetHolding();
            }
            if (t == 70) {
                helper.assertTrue(player.getHealth() < player.getMaxHealth(), "Unguarded, the " + cut + " did not reach the player");
                bandit.discard();
                cleanup(player);
                helper.succeed();
            }
        });
    }
}
