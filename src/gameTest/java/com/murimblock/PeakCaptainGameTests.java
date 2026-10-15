package com.murimblock;

import com.murimblock.integration.epicfight.PeakCaptainPatch;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.PeakCaptainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Captain of the Peak Guard: he wears the Peak Guard's four pieces and holds the Crimson Dragon Spear (a spear for
 * Epic Fight), none of it dropped as worn equipment; neutral, he attacks no one first, struck he fights back, calls the
 * soldiers about him and shows his boss bar; his loot leaves one piece of the armour every time, the spear now and then.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PeakCaptainGameTests {
    private PeakCaptainGameTests() { }

    private static PeakCaptainEntity captain(GameTestHelper helper, BlockPos at) {
        PeakCaptainEntity captain = MurimEntities.PEAK_CAPTAIN.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(captain != null, "No captain spawned");
        return captain;
    }

    @GameTest(template = "foundation_arena", batch = "captain_gear")
    public static void heWearsHisArmourAndHoldsHisSpear(GameTestHelper helper) {
        PeakCaptainEntity captain = captain(helper, new BlockPos(2, 1, 2));
        // Worn equipment counts in his attributes from his first tick.
        helper.runAfterDelay(3, () -> check(helper, captain));
    }

    private static void check(GameTestHelper helper, PeakCaptainEntity captain) {
        try {
            helper.assertTrue(EpicFightCapabilities.getEntityPatch(captain, PeakCaptainPatch.class) != null, "No Epic Fight patch on the captain");
            helper.assertTrue(captain.getItemBySlot(EquipmentSlot.HEAD).is(MurimItems.PEAK_GUARD_HELM.get()), "No helm: " + captain.getItemBySlot(EquipmentSlot.HEAD));
            helper.assertTrue(captain.getItemBySlot(EquipmentSlot.CHEST).is(MurimItems.PEAK_GUARD_LAMELLAR.get()), "No lamellar");
            helper.assertTrue(captain.getItemBySlot(EquipmentSlot.LEGS).is(MurimItems.PEAK_GUARD_TASSETS.get()), "No tassets");
            helper.assertTrue(captain.getItemBySlot(EquipmentSlot.FEET).is(MurimItems.PEAK_GUARD_GREAVES.get()), "No greaves");
            helper.assertTrue(captain.getMainHandItem().is(MurimItems.CRIMSON_DRAGON_SPEAR.get())
                    && EpicFightCapabilities.getItemStackCapability(captain.getMainHandItem()).getWeaponCategory() == CapabilityItem.WeaponCategories.SPEAR,
                    "He does not hold the Crimson Dragon Spear as a spear: " + captain.getMainHandItem());
            // The armour's 20 points, on top of none of his own; his spear stronger than the soldiers' (5).
            helper.assertTrue(captain.getAttributeValue(Attributes.ARMOR) == 20, "His armour gives " + captain.getAttributeValue(Attributes.ARMOR));
            helper.assertTrue(captain.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 6, "His spear strikes for " + captain.getAttributeValue(Attributes.ATTACK_DAMAGE));
            helper.assertTrue(captain.isPersistenceRequired(), "The captain could despawn");
            for (var piece : java.util.List.of(MurimItems.PEAK_GUARD_HELM.get(), MurimItems.PEAK_GUARD_LAMELLAR.get(), MurimItems.PEAK_GUARD_TASSETS.get(), MurimItems.PEAK_GUARD_GREAVES.get()))
                helper.assertTrue(piece instanceof ArmorItem, piece + " is not armour a player can wear");
        } finally {
            captain.discard();
        }
        helper.succeed();
    }

    /** Beside a player he attacks no one; struck, he turns on the player, calls a soldier, and shows his boss bar. */
    @GameTest(template = "foundation_arena", batch = "captain_anger", timeoutTicks = 120)
    public static void heIsNeutralUntilStruck(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        PeakCaptainEntity captain = captain(helper, new BlockPos(1, 1, 3));
        var guard = MurimEntities.ALLIANCE_SPEAR_GUARD.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 4)), MobSpawnType.COMMAND);
        // Mock players are not tracked: he is shown to the player by hand, as the sensei (AzureSenseiGameTests).
        captain.startSeenByPlayer(player);
        helper.assertTrue(captain.showsBossBarTo(player), "The player does not see his boss bar");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(captain.getTarget() == null && !captain.isAngry(), "The captain attacked first: " + captain.getTarget());
            boolean hurt = captain.hurt(player.damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(hurt && captain.getLastHurtByMob() == player, "The blow did not land: hurt " + hurt + ", by " + captain.getLastHurtByMob());
        });
        helper.runAfterDelay(45, () -> helper.succeedWhen(() -> {
            helper.assertTrue(captain.getTarget() == player, "The struck captain does not fight back: target " + captain.getTarget() + ", by " + captain.getLastHurtByMob() + ", angry " + captain.getRemainingPersistentAngerTime() + ", player mode " + player.gameMode.getGameModeForPlayer());
            helper.assertTrue(captain.isAngryAt(player) && captain.getRemainingPersistentAngerTime() > 0, "He is not angry at the player");
            helper.assertTrue(guard.getTarget() == player, "The spear guard did not come to his help");
            captain.discard();
            guard.discard();
            cleanup(player);
        }));
    }

    /** Killed by a player: one armour piece every time (each of the four in turn), the spear about 15 times in 100. */
    @GameTest(template = "foundation_arena", batch = "captain_loot")
    public static void heLeavesAPieceOfArmourEveryTimeAndHisSpearNowAndThen(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        PeakCaptainEntity captain = captain(helper, new BlockPos(2, 1, 2));
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "entities/peak_captain")));
        var pieces = java.util.Set.of(MurimItems.PEAK_GUARD_HELM.get(), MurimItems.PEAK_GUARD_LAMELLAR.get(),
                MurimItems.PEAK_GUARD_TASSETS.get(), MurimItems.PEAK_GUARD_GREAVES.get());
        LootParams byPlayer = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, captain)
                .withParameter(LootContextParams.ORIGIN, captain.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().playerAttack(player))
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .create(LootContextParamSets.ENTITY);
        int rolls = 400, spears = 0;
        var seen = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (int i = 0; i < rolls; i++) {
            var items = table.getRandomItems(byPlayer);
            long armour = items.stream().filter(s -> pieces.contains(s.getItem())).peek(s -> seen.add(s.getItem())).count();
            helper.assertTrue(armour == 1, "He left " + armour + " pieces of armour");
            spears += (int) items.stream().filter(s -> s.is(MurimItems.CRIMSON_DRAGON_SPEAR.get())).count();
        }
        captain.discard();
        cleanup(player);
        // 60 of 400 expected.
        helper.assertTrue(spears >= 35 && spears <= 90, "He left his spear " + spears + " times in " + rolls + ", not about 15 in 100");
        helper.assertTrue(seen.size() == 4, "Not every piece of his armour came: " + seen);
        helper.succeed();
    }
}
