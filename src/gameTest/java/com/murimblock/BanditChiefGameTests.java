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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
        // Kept whatever player another test left behind (a monster despawns when the nearest player is far away).
        chief.setPersistenceRequired();
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

    @GameTest(template = "foundation_arena", batch = "chief_idle")
    public static void theChiefStandsInHisOwnIdle(GameTestHelper helper) {
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 2));
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(chief, LivingEntityPatch.class);
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            helper.assertTrue(idle == MurimAnimations.CHIEF_IDLE, "With his axe, the chief should stand in his own idle, not " + idle);
            chief.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "chief_chain", timeoutTicks = 120)
    public static void anAttackStartsAtOnceAfterTheLastOne(GameTestHelper helper) {
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 2));
        chief.setNoAi(true);
        BanditChiefPatch patch = EpicFightCapabilities.getEntityPatch(chief, BanditChiefPatch.class);
        helper.runAfterDelay(2, () -> patch.playAnimationSynchronized(MurimAnimations.CHIEF_CLEAVE, 0));
        // The cleave is over (1.15 s): the sweep must start the tick it is asked for.
        helper.runAfterDelay(32, () -> patch.playAnimationSynchronized(MurimAnimations.CHIEF_SWEEP, 0));
        helper.runAfterDelay(34, () -> {
            var playing = patch.getAnimator().getPlayerFor(null).getRealAnimation();
            helper.assertTrue(playing == MurimAnimations.CHIEF_SWEEP, "The sweep did not start after the cleave: " + playing);
            chief.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "chief_attack", timeoutTicks = 300)
    public static void theChiefFightsWithHisOwnAttacks(GameTestHelper helper) {
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        target.setNoAi(true);
        target.setPersistenceRequired();
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

    // At 3.6 blocks from the player the chief starts at relative Z=5.1, outside foundation_arena's loaded chunks.
    @GameTest(template = "spear_arena", batch = "chief_parry_slam", timeoutTicks = 160)
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
                helper.assertTrue(helper.getBounds().contains(chief.position()),
                        "The chief starts outside the loaded test arena: " + chief.position() + " outside " + helper.getBounds());
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

    /** His armour, the four pieces together: 17 armour and 4 toughness, between iron (15, 0) and diamond (20, 8). */
    @GameTest(template = "foundation_arena", batch = "chief_armour")
    public static void hisArmourIsBetweenIronAndDiamond(GameTestHelper helper) {
        int armour = 0;
        float toughness = 0;
        for (var piece : java.util.List.of(MurimItems.CHIEF_BROW_BAND, MurimItems.CHIEF_LAMELLAR_VEST, MurimItems.CHIEF_TASSETS, MurimItems.CHIEF_FUR_BOOTS)) {
            armour += piece.get().getDefense();
            toughness += piece.get().getToughness();
        }
        helper.assertTrue(armour == 17, "The chief's armour should give 17, not " + armour);
        helper.assertTrue(Math.abs(toughness - 4) < 0.01, "The chief's armour should give toughness 4, not " + toughness);
        helper.assertTrue(MurimItems.CHIEF_LAMELLAR_VEST.get().getEquipmentSlot() == EquipmentSlot.CHEST, "The vest is not worn on the chest");
        helper.succeed();
    }

    /**
     * The Long Fist, the most widespread art: the chief drops its scroll about three times in ten (killed by a player),
     * and the sites' caches hold it four times in ten, with what a village weaponsmith's chest holds.
     */
    @GameTest(template = "foundation_arena", batch = "chief_long_fist_drop")
    public static void theLongFistScrollComesFromTheChiefAndTheCaches(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 2));
        var tables = helper.getLevel().getServer().reloadableRegistries();
        LootTable chiefTable = tables.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "entities/bandit_chief")));
        LootParams byPlayer = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, chief)
                .withParameter(LootContextParams.ORIGIN, chief.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().playerAttack(player))
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .create(LootContextParamSets.ENTITY);
        int rolls = 400, fromChief = 0, fromCache = 0, otherLoot = 0;
        for (int i = 0; i < rolls; i++)
            fromChief += (int) chiefTable.getRandomItems(byPlayer).stream().filter(s -> s.is(MurimItems.LONG_FIST_SCROLL.get())).count();
        LootTable cache = tables.getLootTable(com.murimblock.world.structure.site.SiteLoot.CACHE);
        LootParams opened = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, chief.position())
                .create(LootContextParamSets.CHEST);
        for (int i = 0; i < rolls; i++) {
            var items = cache.getRandomItems(opened);
            fromCache += (int) items.stream().filter(s -> s.is(MurimItems.LONG_FIST_SCROLL.get())).count();
            otherLoot += (int) items.stream().filter(s -> !s.is(MurimItems.LONG_FIST_SCROLL.get())).count();
        }
        chief.discard();
        cleanup(player);
        // 120 and 160 of 400 expected.
        helper.assertTrue(fromChief >= 85 && fromChief <= 160, "The chief dropped the scroll " + fromChief + " times in " + rolls + ", not about three in ten");
        helper.assertTrue(fromCache >= 120 && fromCache <= 200, "The caches held the scroll " + fromCache + " times in " + rolls + ", not about four in ten");
        helper.assertTrue(otherLoot > rolls, "The caches hold too little of a weaponsmith's loot: " + otherLoot + " items in " + rolls);
        helper.succeed();
    }

    /**
     * Killed by a player, the chief drops one piece of his armour about one time in five (never two at once), any of
     * the four; killed by anything else, none.
     */
    @GameTest(template = "foundation_arena", batch = "chief_armour_drop")
    public static void heDropsAPieceOfHisArmourNowAndThen(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BanditChiefEntity chief = chief(helper, new BlockPos(2, 1, 2));
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "entities/bandit_chief")));
        var pieces = java.util.Set.of(MurimItems.CHIEF_BROW_BAND.get(), MurimItems.CHIEF_LAMELLAR_VEST.get(),
                MurimItems.CHIEF_TASSETS.get(), MurimItems.CHIEF_FUR_BOOTS.get());
        int rolls = 400, dropped = 0;
        var seen = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (boolean byPlayer : new boolean[] {true, false}) {
            LootParams.Builder params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, chief)
                    .withParameter(LootContextParams.ORIGIN, chief.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, byPlayer ? helper.getLevel().damageSources().playerAttack(player)
                            : helper.getLevel().damageSources().generic());
            if (byPlayer) params.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withParameter(LootContextParams.ATTACKING_ENTITY, player);
            LootParams built = params.create(LootContextParamSets.ENTITY);
            for (int i = 0; i < rolls; i++) {
                long armour = table.getRandomItems(built).stream().filter(stack -> pieces.contains(stack.getItem())).peek(stack -> seen.add(stack.getItem())).count();
                helper.assertTrue(armour <= 1, "The chief dropped " + armour + " pieces of his armour at once");
                if (!byPlayer) helper.assertTrue(armour == 0, "The chief dropped his armour without a player killing him");
                dropped += byPlayer ? (int) armour : 0;
            }
        }
        // One time in five: 80 of 400 expected, well within 50..115.
        helper.assertTrue(dropped >= 50 && dropped <= 115, "The chief dropped his armour " + dropped + " times in " + rolls + ", not about one in five");
        helper.assertTrue(seen.size() == 4, "Not every piece of his armour dropped: " + seen);
        chief.discard();
        cleanup(player);
        helper.succeed();
    }
}
