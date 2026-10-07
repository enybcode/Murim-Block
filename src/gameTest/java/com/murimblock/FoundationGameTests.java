package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeService;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Runs against a disposable server world and is excluded from the shipped mod. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class FoundationGameTests {
    @SubscribeEvent
    public static void template(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getServer() instanceof GameTestServer)) return;
        CompoundTag nbt = new CompoundTag();
        ListTag size = new ListTag();
        for (int i = 0; i < 3; i++) size.add(IntTag.valueOf(5));
        nbt.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        nbt.put("palette", palette);
        nbt.put("blocks", new ListTag());
        nbt.put("entities", new ListTag());
        level.getStructureManager().getOrCreate(ResourceLocation.parse("murimblock:foundation_arena"))
                .load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
    }

    @GameTest(template = "foundation_arena", batch = "foundation", timeoutTicks = 40)
    public static void swordAttackAppliesOneImmediateHitAndNoDelayedHit(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.setNoGravity(true);
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingIncomingDamageEvent> observer = event -> {
            if (event.getEntity() == target && event.getSource().getEntity() == player) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            float before = target.getHealth();
            player.attack(target);
            float after = target.getHealth();
            helper.assertTrue(after < before, "The default sword hit was deferred or canceled");
            helper.assertTrue(contacts.get() == 1, "One attack generated more than one damage contact");
            helper.runAfterDelay(5, () -> {
                try {
                    helper.assertTrue(target.getHealth() == after && contacts.get() == 1,
                            "A second delayed hit followed the default attack");
                    helper.assertTrue(QiService.getQi(player) == 100, "Sword attacks spent Qi");
                    helper.succeed();
                } finally {
                    NeoForge.EVENT_BUS.unregister(observer);
                    cleanup(player);
                }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void incomingMeleeDamageDoesNotSpendQiOrGrantSwordProtection(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
            attacker.setNoAi(true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health - 6, "The mode flag intercepted default damage");
            helper.assertTrue(QiService.getQi(player) == 100, "Incoming damage spent Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void itemUseKeepsDefaultSwordAndOffhandShieldBehavior(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            var result = player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.PASS && !player.isUsingItem(), "Sword use was intercepted");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            player.gameMode.useItem(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND);
            helper.assertTrue(player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND,
                    "The offhand shield could not be used");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void modeToggleDoesNotChangeMovementQiOrCultivation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            var cultivation = CultivationService.getCultivation(player);
            CombatService.setCombatMode(player, false);
            CombatService.setCombatMode(player, true);
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Mode toggle slowed the player");
            helper.assertTrue(QiService.getQi(player) == 100 && QiService.getQiMax(player) == 100, "Mode toggle changed Qi");
            helper.assertTrue(CultivationService.getCultivation(player).equals(cultivation), "Mode toggle changed cultivation");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void qiChargingStillLocksAndRestoresMovement(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH);
            helper.assertTrue(QiChargeService.startCharging(player), "Qi charging could not start with a sword equipped");
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0
                    && player.getAttributeValue(Attributes.JUMP_STRENGTH) == 0, "Qi charging did not lock movement");
            QiChargeService.stopCharging(player);
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed
                    && player.getAttributeValue(Attributes.JUMP_STRENGTH) == jump, "Charging left movement locked");
            helper.assertTrue(QiService.getQi(player) == 100, "Starting/stopping charge spent Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void modeAndChargeResetOnLogoutAndClone(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        ServerPlayer clone = player(helper);
        try {
            QiChargeService.startCharging(clone);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, original, true));
            helper.assertFalse(CombatService.isInCombatMode(clone), "Respawn inherited the temporary mode");
            helper.assertFalse(QiChargeService.isCharging(clone), "Respawn inherited charging");
            QiChargeService.startCharging(original);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(original));
            helper.assertFalse(CombatService.isInCombatMode(original), "Logout kept the temporary mode");
            helper.assertFalse(QiChargeService.isCharging(original), "Logout kept charging");
            helper.succeed();
        } finally {
            cleanup(original);
            cleanup(clone);
        }
    }

    @GameTest(template = "foundation_arena", batch = "foundation")
    public static void playerDataRoundTripsQiAndCultivationWithoutTemporaryMode(GameTestHelper helper) {
        ServerPlayer original = player(helper);
        ServerPlayer restored = player(helper);
        try {
            QiService.setQiMax(original, 150);
            QiService.setQi(original, 73);
            CultivationService.setCultivation(original, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
            var cultivation = CultivationService.getCultivation(original);
            CompoundTag saved = new CompoundTag();
            original.saveWithoutId(saved);
            helper.assertFalse(saved.toString().contains("murimblock:player_combat"), "The temporary mode was persisted");
            CombatService.setCombatMode(restored, false);
            // Keep the connected mock player's identity separate from the saved player.
            saved.remove("UUID");
            restored.load(saved);
            helper.assertTrue(QiService.getQi(restored) == 73 && QiService.getQiMax(restored) == 150,
                    "Qi values did not survive save/load");
            helper.assertTrue(CultivationService.getCultivation(restored).equals(cultivation), "Cultivation did not survive save/load");
            helper.assertFalse(CombatService.isInCombatMode(restored), "Saved data enabled the temporary mode");
            helper.succeed();
        } finally {
            cleanup(original);
            cleanup(restored);
        }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "foundation-test"), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.setNoGravity(true);
        player.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        // Damage must run after Minecraft's login protection has elapsed.
        for (int i = 0; i <= 60; i++) player.tick();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.tick();
        CombatService.setCombatMode(player, true);
        QiService.setQiMax(player, 100);
        QiService.setQi(player, 100);
        return player;
    }

    private static void cleanup(ServerPlayer player) {
        QiChargeService.stopCharging(player);
        player.server.getPlayerList().remove(player);
    }
}
