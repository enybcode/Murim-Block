package com.murimblock.combat;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.connection.ConnectionType;
import com.murimblock.qi.QiService;
import com.murimblock.qi.charge.QiChargeService;
import com.murimblock.combat.preview.CombatPreviewService;
import com.murimblock.combat.preview.PreviewDefinition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Dev-only server tests. These classes are not included in the distributable mod jar. */
@GameTestHolder("murimblock")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "murimblock")
public final class SwordGuardGameTests {
    @SubscribeEvent
    public static void template(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(level.getServer() instanceof GameTestServer)) return;
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
        level.getStructureManager().getOrCreate(ResourceLocation.parse("murimblock:guard_arena"))
                .load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void frontHitCostsQiButNotHealth(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health, "Frontal sword guard did not prevent actual health loss");
            helper.assertTrue(QiService.getQi(player) == 76, "Guard did not spend exactly 24 Qi");
            helper.assertTrue(MeleeCombatService.getData(player).action() == MeleeData.Action.GUARD_IMPACT,
                    "Successful interception did not synchronize its action");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void rearHitIsNotBlocked(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, false);
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health - 6, "Sword guard granted rear protection");
            helper.assertTrue(QiService.getQi(player) == 100, "Rear hit spent guard Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void insufficientQiBreaksGuardAndDoesNotCancelDamage(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            QiService.setQi(player, 10);
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health - 6, "Unaffordable guard canceled incoming damage");
            helper.assertTrue(QiService.getQi(player) == 0, "Broken guard did not exhaust its remaining Qi");
            MeleeCombatService.requestGuard(player, false);
            MeleeCombatService.requestGuard(player, true);
            helper.assertTrue(MeleeCombatService.getData(player).action() == MeleeData.Action.GUARD_BREAK,
                    "Release/repress bypassed guard-break recovery");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void releasingGuardRestoresMovementAndDamage(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            double speed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            MeleeCombatService.requestGuard(player, true);
            helper.assertTrue(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    - speed * 0.35) < 1.0E-6, "Guard movement multiplier is wrong");
            MeleeCombatService.requestGuard(player, false);
            helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == speed,
                    "Guard release left a movement penalty");
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 4);
            helper.assertTrue(player.getHealth() == health - 4, "Released guard still blocked a hit");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void weaponSwapHasNoOneTickProtectionWindow(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 4);
            helper.assertTrue(player.getHealth() == health - 4, "An axe inherited stale sword protection before the next tick");
            helper.assertFalse(MeleeCombatService.isBusy(player), "Stale sword state locked a different weapon family");
            MeleeCombatService.tick(player.server);
            helper.assertTrue(MeleeCombatService.getData(player).equals(MeleeData.initial()), "Weapon swap left a stale action");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void offhandItemsAndCombatModeOffPreserveVanilla(GameTestHelper helper) {
        var player = player(helper);
        try {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            MeleeCombatService.requestGuard(player, true);
            helper.assertFalse(MeleeCombatService.isBusy(player), "Sword guard replaced the offhand item");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            MeleeCombatService.requestGuard(player, true);
            CombatService.setCombatMode(player, false);
            helper.assertTrue(MeleeCombatService.getData(player).equals(MeleeData.initial()), "Combat Mode off kept the guard");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void guardDoesNotStopEnvironmentalDamage(GameTestHelper helper) {
        var player = player(helper);
        try {
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().fall(), 4);
            helper.assertTrue(player.getHealth() == health - 4, "Sword guard canceled fall damage");
            helper.assertTrue(QiService.getQi(player) == 100, "Environmental damage spent guard Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void guardAndQiChargingAreMutuallyExclusive(GameTestHelper helper) {
        var player = player(helper);
        try {
            QiChargeService.startCharging(player);
            MeleeCombatService.requestGuard(player, true);
            helper.assertFalse(QiChargeService.isCharging(player), "Starting guard did not stop Qi charging");
            helper.assertFalse(QiChargeService.startCharging(player), "Qi charging started while guarding");
            MeleeCombatService.requestGuard(player, false);
            helper.assertTrue(QiChargeService.startCharging(player), "Release did not restore Qi charging");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void attackingDropsGuardWithoutDeferringOrDuplicatingTheHit(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie target = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            float targetHealth = target.getHealth();
            player.attack(target);
            helper.assertTrue(target.getHealth() < targetHealth, "Sword attack was deferred instead of using the current vanilla hit");
            helper.assertTrue(MeleeCombatService.getData(player).action() == MeleeData.Action.SWING,
                    "Attack did not suspend its held guard");
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(target), 4);
            helper.assertTrue(player.getHealth() == health - 4, "Attacking simultaneously benefited from a held guard");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard", timeoutTicks = 60)
    public static void lostReleasePacketExpiresOnTheServer(GameTestHelper helper) {
        var player = player(helper);
        MeleeCombatService.requestGuard(player, true);
        helper.runAfterDelay(MeleeCombatService.GUARD_TIMEOUT_TICKS + 1, () -> {
            try {
                helper.assertTrue(MeleeCombatService.getData(player).equals(MeleeData.initial()),
                        "Guard stayed active without client input refreshes");
                helper.succeed();
            } finally { cleanup(player); }
        });
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void releaseDuringImpactStopsProtectionButKeepsRecovery(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            MeleeCombatService.requestGuard(player, false);
            helper.assertTrue(MeleeCombatService.getData(player).locksAttack(player.level().getGameTime()),
                    "Release erased impact recovery");
            player.hurt(player.damageSources().mobAttack(attacker), 4);
            helper.assertTrue(player.getHealth() == health - 4, "Released impact still protected health");
            helper.assertTrue(QiService.getQi(player) == 76, "Released impact spent Qi again");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void repeatedBlocksEachPayTheirOwnCost(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            player.hurt(player.damageSources().mobAttack(attacker), 6);
            helper.assertTrue(player.getHealth() == health, "Held guard failed during impact recovery");
            helper.assertTrue(QiService.getQi(player) == 52, "Repeated blocks were free or double charged");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void guardDoesNotCancelProjectiles(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            var arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, player.serverLevel());
            MeleeCombatService.requestGuard(player, true);
            float health = player.getHealth();
            player.hurt(player.damageSources().arrow(arrow, attacker), 4);
            helper.assertTrue(player.getHealth() == health - 4, "Sword guard intercepted an unsupported projectile");
            helper.assertTrue(QiService.getQi(player) == 100, "Projectile damage spent sword guard Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void identicalSwordInAnotherSlotCannotReuseOldGuard(GameTestHelper helper) {
        var player = player(helper);
        try {
            Zombie attacker = attacker(helper, true);
            MeleeCombatService.requestGuard(player, true);
            player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
            player.getInventory().selected = 1;
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(attacker), 4);
            helper.assertTrue(player.getHealth() == health - 4, "A same-item slot change reused the previous guard");
            helper.assertTrue(QiService.getQi(player) == 100, "Invalid weapon binding spent Qi");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard", timeoutTicks = 60)
    public static void previewFinishesWithoutChangingHealthQiOrMovement(GameTestHelper helper) {
        var player = player(helper);
        // Unlike a zombie, this stationary melee target cannot lose health to daylight.
        var target = helper.spawn(EntityType.HUSK, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.setNoGravity(true);
        float health = player.getHealth();
        float targetHealth = target.getHealth();
        double speed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(CombatPreviewService.start(player), "Preview did not start with a sword");
        helper.assertFalse(CombatPreviewService.start(player), "Preview restarted while already playing");
        helper.runAfterDelay(PreviewDefinition.DURATION_TICKS + 1, () -> {
            try {
                helper.assertFalse(CombatPreviewService.getData(player).playing(), "Preview did not expire");
                helper.assertTrue(player.getHealth() == health, "Preview changed player health");
                helper.assertTrue(target.getHealth() == targetHealth, "Preview changed target health");
                helper.assertTrue(QiService.getQi(player) == 100, "Preview changed Qi");
                helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "Preview damaged the weapon");
                helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == speed,
                        "Preview changed movement");
                helper.succeed();
            } finally { cleanup(player); }
        });
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void previewDoesNotBlockOrDuplicateLegacyAttack(GameTestHelper helper) {
        var player = player(helper);
        var hits = new java.util.concurrent.atomic.AtomicInteger();
        Zombie target = attacker(helper, true);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> counter =
                event -> { if (event.getEntity() == target) hits.incrementAndGet(); };
        NeoForge.EVENT_BUS.addListener(counter);
        try {
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not start");
            float before = target.getHealth();
            player.attack(target);
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Attack left preview playing");
            helper.assertTrue(target.getHealth() < before && hits.get() == 1, "Vanilla sword attack was lost or duplicated");
            helper.assertTrue(MeleeCombatService.getData(player).action() == MeleeData.Action.SWING, "Legacy swing was lost");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(counter);
            cleanup(player);
        }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void previewRefusesOtherWeaponsOffhandAndBusyActions(GameTestHelper helper) {
        var player = player(helper);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            helper.assertFalse(CombatPreviewService.start(player), "Axe used sword preview");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
            helper.assertFalse(CombatPreviewService.start(player), "Offhand item allowed preview");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            QiChargeService.startCharging(player);
            helper.assertFalse(CombatPreviewService.start(player), "Preview interrupted charging");
            helper.assertTrue(QiChargeService.isCharging(player), "Refused preview changed charging");
            QiChargeService.stopCharging(player);
            MeleeCombatService.requestGuard(player, true);
            helper.assertFalse(CombatPreviewService.start(player), "Preview interrupted guard");
            helper.assertTrue(MeleeCombatService.getData(player).isGuarding(), "Refused preview changed guard");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void guardAndChargeInterruptPreviewButKeepTheirBehavior(GameTestHelper helper) {
        var player = player(helper);
        try {
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not start");
            MeleeCombatService.requestGuard(player, true);
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Guard left preview playing");
            helper.assertTrue(MeleeCombatService.getData(player).isGuarding(), "Preview blocked guard");
            MeleeCombatService.requestGuard(player, false);
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not restart after guard release");
            helper.assertTrue(QiChargeService.startCharging(player), "Preview blocked Qi charging");
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Charging left preview playing");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void slotSwapAndItemUseStopPreviewWithoutLockingPlayer(GameTestHelper helper) {
        var player = player(helper);
        try {
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not start");
            player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
            player.getInventory().selected = 1;
            CombatPreviewService.tick(player);
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Same-sword slot swap reused preview");
            helper.assertFalse(MeleeCombatService.isBusy(player), "Preview locked legacy combat");
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not start again");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
            player.startUsingItem(InteractionHand.OFF_HAND);
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Item use did not cancel preview");
            helper.assertTrue(player.isUsingItem(), "Preview canceled item use");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard", timeoutTicks = 60)
    public static void lateTrackerKeepsAgeInsteadOfRestartingPreview(GameTestHelper helper) {
        var player = player(helper);
        helper.assertTrue(CombatPreviewService.start(player), "Preview did not start");
        long started = CombatPreviewService.getData(player).startedAt();
        helper.runAfterDelay(9, () -> {
            ServerPlayer observer = null;
            try {
                observer = player(helper);
                NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(observer, player));
                var state = CombatPreviewService.getData(player);
                helper.assertTrue(state.playing() && state.startedAt() == started && state.sampledAge() >= 9,
                        "Late tracker snapshot restarted or lost preview time");
                helper.succeed();
            } finally {
                if (observer != null) cleanup(observer);
                cleanup(player);
            }
        });
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void lifecycleEventsCancelPreviewAndDoNotPersistIt(GameTestHelper helper) {
        var player = player(helper);
        try {
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not start");
            CompoundTag saved = new CompoundTag();
            player.saveWithoutId(saved);
            helper.assertFalse(saved.toString().contains("combat_preview"), "Preview leaked into saved data");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, net.minecraft.world.level.Level.OVERWORLD,
                    net.minecraft.world.level.Level.NETHER));
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Dimension transition kept preview");
            helper.assertTrue(CombatPreviewService.start(player), "Preview did not restart");
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertFalse(CombatPreviewService.getData(player).playing(), "Death kept preview");
            helper.succeed();
        } finally { cleanup(player); }
    }

    @GameTest(template = "guard_arena", batch = "sword_guard")
    public static void previewIsClearedOnCloneAndLogout(GameTestHelper helper) {
        var original = player(helper);
        var clone = player(helper);
        try {
            helper.assertTrue(CombatPreviewService.start(original) && CombatPreviewService.start(clone), "Preview did not start");
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, original, true));
            helper.assertFalse(CombatPreviewService.getData(original).playing(), "Original player kept preview after clone");
            helper.assertFalse(CombatPreviewService.getData(clone).playing(), "New player inherited preview on respawn");
            helper.assertTrue(CombatPreviewService.start(clone), "New player could not preview after respawn");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(clone));
            helper.assertFalse(CombatPreviewService.getData(clone).playing(), "Logout kept preview");
            helper.succeed();
        } finally {
            cleanup(original);
            cleanup(clone);
        }
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "guard-test-player"), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // The vanilla mock helper skips NeoForge channel negotiation before firing login hooks.
        NetworkRegistry.configureMockConnection(connection);
        player.server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.setNoGravity(true);
        player.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        // Exercise damage only after vanilla's sixty-tick login protection has elapsed.
        for (int i = 0; i <= 60; i++) player.tick();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        CombatService.setCombatMode(player, true);
        QiService.setQiMax(player, 100);
        QiService.setQi(player, 100);
        return player;
    }

    private static Zombie attacker(GameTestHelper helper, boolean front) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, front ? 3 : 1));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        return zombie;
    }

    private static void cleanup(ServerPlayer player) {
        MeleeCombatService.clear(player);
        QiChargeService.stopCharging(player);
        player.server.getPlayerList().remove(player);
    }
}
