package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.WaterBallEntity;
import net.minecraft.world.effect.MobEffects;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client check of Murim's guard (gradlew -PgameTests runParryTour): in a fresh flat world a
 * guarding player (a server player on a mock connection) takes a zombie's hit on a perfect then a normal parry, then
 * real arrows, then the crab man's water balls, while the local player films from the side. Frame sequences go to build/parry-tour-run/screenshots,
 * the report to parry-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ParryTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile ServerPlayer guard;
    private static volatile Zombie zombie;
    private static volatile CrabManEntity crabMan;
    private static volatile float lowest = Float.MAX_VALUE;
    private static double baseX, baseY, baseZ;
    private static final List<String> report = new ArrayList<>();

    private ParryTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.parryTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-parry", GameType.SPECTATOR, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-parry", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            server.execute(() -> { if (guard != null && !guard.isRemoved()) guard.doTick(); });
            step(mc, server);
        } catch (Exception e) {
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Parry tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        record(mc, "01-perfect-parry", 36, 70, 1);
        record(mc, "02-normal-parry", 96, 130, 1);
        record(mc, "03-arrow-perfect", 156, 186, 1);
        record(mc, "04-arrow-normal", 216, 246, 1);
        record(mc, "05-water-ball-perfect", 268, 300, 1);
        record(mc, "06-water-ball-normal", 348, 382, 1);
        // The lowest health during the normal parry scene (the player regenerates afterwards).
        if (ticks > 350 && ticks <= 382) server.execute(() -> lowest = Math.min(lowest, guard.getHealth()));
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = self(server);
                    baseX = Math.floor(self.getX()) + 0.5;
                    baseY = self.getY();
                    baseZ = Math.floor(self.getZ()) + 0.5;
                    guard = spawnGuard(server, level);
                    guard.moveTo(baseX, baseY, baseZ, -90, 0);       // faces +X
                    zombie = EntityType.ZOMBIE.create(level);
                    zombie.moveTo(baseX + 1.6, baseY, baseZ, 90, 0); // faces the guard
                    zombie.setNoAi(true);
                    // A helmet: no burning in the sun on camera.
                    zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    zombie.setPersistenceRequired();
                    level.addFreshEntity(zombie);
                    // The camera, from the side.
                    self.teleportTo(level, baseX + 0.8, baseY + 0.1, baseZ - 3.0, 0, 10);
                });
            }
            // 1. Perfect parry: the guard goes up 4 ticks before the hit.
            case 40 -> server.execute(() -> raise(0));
            case 44 -> server.execute(() -> {
                guard.hurt(guard.damageSources().mobAttack(zombie), 6);
                LivingEntityPatch<?> z = EpicFightCapabilities.getEntityPatch(zombie, LivingEntityPatch.class);
                check(guard.getHealth() == guard.getMaxHealth(), "perfect parry: no damage (health " + guard.getHealth() + ")");
                check(z != null && z.getEntityState().hurt(), "perfect parry: the zombie is thrown off balance");
            });
            case 80 -> server.execute(() -> lower());
            // 2. Normal parry: the guard has been up for 2 seconds.
            case 60 + 20 + 1 -> server.execute(() -> raise(0));
            case 124 -> server.execute(() -> {
                guard.hurt(guard.damageSources().mobAttack(zombie), 6);
                check(Math.abs(guard.getMaxHealth() - guard.getHealth() - 3) < 0.01F, "normal parry: half the damage (health " + guard.getHealth() + ")");
            });
            case 140 -> server.execute(() -> { lower(); guard.setHealth(guard.getMaxHealth()); zombie.discard(); });
            // 3. A real arrow on a perfect parry: shot from 6 blocks, the guard goes up as it flies.
            case 160 -> server.execute(() -> shoot());
            case 161 -> server.execute(() -> raise(0));
            case 185 -> server.execute(() -> check(guard.getHealth() == guard.getMaxHealth(), "perfect parry: the arrow does no damage (health " + guard.getHealth() + ")"));
            case 200 -> server.execute(() -> lower());
            // 4. A real arrow on a normal parry.
            case 201 -> server.execute(() -> raise(0));
            case 240 -> server.execute(() -> shoot());
            case 252 -> server.execute(() -> {
                float taken = guard.getMaxHealth() - guard.getHealth();
                check(taken > 0 && taken < 4.5F, "normal parry: the arrow does half its damage (took " + taken + ")");
            });
            // 5-6. The crab man's water ball, on a perfect then a normal parry.
            case 258 -> server.execute(() -> {
                lower();
                guard.setHealth(guard.getMaxHealth());
                guard.removeAllEffects();
                crabMan = MurimEntities.CRAB_MAN.get().create(guard.level());
                crabMan.moveTo(guard.getX() + 6, guard.getY(), guard.getZ(), 90, 0);
                crabMan.setNoAi(true);
                crabMan.yBodyRot = crabMan.yHeadRot = 90;
                guard.level().addFreshEntity(crabMan);
            });
            // Wider, so the crab man and the guard are both in the picture.
            case 262 -> server.execute(() -> self(server).teleportTo(server.overworld(), baseX + 3, baseY + 0.4, baseZ - 5.5, 0, 6));
            case 270 -> server.execute(() -> { throwBall(); raise(0); });
            case 300 -> server.execute(() -> {
                check(guard.getHealth() == guard.getMaxHealth() && !guard.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                        "perfect parry: the water ball does nothing (health " + guard.getHealth() + ", slowed " + guard.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) + ")");
                lower();
            });
            case 305 -> server.execute(() -> raise(0));
            case 350 -> server.execute(() -> throwBall());
            case 382 -> server.execute(() -> {
                float taken = guard.getMaxHealth() - lowest;
                check(Math.abs(taken - 2) < 0.01F && !guard.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                        "normal parry: the water ball does half its damage, no slowness (took " + taken + ")");
            });
            case 395 -> {
                server.execute(() -> { server.getPlayerList().remove(guard); if (crabMan != null) crabMan.discard(); });
                finish(mc);
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the guarding player

    private static void raise(int ticksAgo) {
        PlayerPatch<?> patch = EpicFightBridge.patch(guard);
        check(patch.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "the Murim guard is in the guard slot");
        patch.startSkillHolding(MurimSkills.MURIM_GUARD.get());
        patch.getSkill(SkillSlots.GUARD).getDataManager().setDataSync(EpicFightSkillDataKeys.LAST_ACTIVE, guard.tickCount - ticksAgo);
    }

    private static void lower() {
        EpicFightBridge.patch(guard).resetHolding();
    }

    /** A real arrow, 6 blocks in front of the guard, flying at their chest. */
    private static void shoot() {
        Arrow arrow = new Arrow(guard.level(), guard.getX() + 6, guard.getY() + 1.3, guard.getZ(), new ItemStack(Items.ARROW), null);
        arrow.shoot(-1, 0.02, 0, 1.6F, 0);
        guard.level().addFreshEntity(arrow);
    }

    /** The crab man's big water ball, thrown at the guard. */
    private static void throwBall() {
        WaterBallEntity ball = WaterBallEntity.charging(crabMan, 1.0F, 0);
        guard.level().addFreshEntity(ball);
        ball.launch(guard, WaterBallEntity.SPEED, 0, 0);
    }

    private static ServerPlayer spawnGuard(MinecraftServer server, ServerLevel level) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "Guard"), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        CombatService.setCombatMode(player, true);
        EpicFightCombatDefaults.enforce(player);
        return player;
    }

    // ---------------------------------------------------------------- helpers

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) {
            Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(String.format("%s/frame-%03d.png", name, (ticks - from) / every));
            Files.createDirectories(file.getParent());
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(file);
            }
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Parry tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("parry-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the parry tour report", e);
        }
        LOGGER.info("Parry tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
